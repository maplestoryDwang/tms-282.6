package net.swordie.ms;

import net.swordie.ms.client.User;
import net.swordie.ms.client.character.BroadcastMsg;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.packet.WvsContext;
import net.swordie.ms.connection.protocol.ProtocolConfig;
import net.swordie.ms.enums.AccountType;
import net.swordie.ms.life.Life;
import net.swordie.ms.life.npc.Npc;
import net.swordie.ms.loaders.NpcData;
import net.swordie.ms.util.Position;
import net.swordie.ms.world.field.Field;
import net.swordie.ms.world.field.Foothold;

import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

/**
 * Game-play toggles from {@code config/server.properties} (keys ported from the TMS 279 config.toml and the MetaMS 273
 * game0/login properties). Every key defaults to the behaviour the server had before the key existed, so an absent
 * key changes nothing.
 * <p>
 * Hot reload: every getter checks the file's modification time at most once per {@link #RELOAD_CHECK_MS} and
 * re-reads it when it changed; {@link #load()} forces a re-read. JVM {@code -Dkey=value} overrides win over the file.
 * The file may be UTF-8 (Chinese text written directly) or ISO-8859-1 with {@code \\uXXXX} escapes.
 */
public final class GameSettings {
    static final long RELOAD_CHECK_MS = 3_000;

    public static final String DEFAULT_MAINTENANCE_MESSAGE = "伺服器維護中，目前僅開放管理員登入。";
    public static final String DEFAULT_FIELD_LEVEL_MESSAGE = "等級不足 %d，無法進入此地圖。";

    /** One {@code game.custom-npc.N=map,npc,x,y,facing} entry; facing 0 = left, 1 = right (as TMS 279 {@code f}). */
    public record CustomNpc(int mapId, int npcId, int x, int y, int facing) {
    }

    /** Immutable view of the parsed keys; replaced as a whole on reload. */
    record Snapshot(
            // [rate]
            NavigableMap<Integer, Integer> expByLevel,
            int dropBoss, int dropNormal, int bonusMesoPercent, int bonusDropPercent,
            int familiarExp, int optionGradeUpPercent, int optionGradeSamePercent,
            // meso
            int mesoPickupMax,
            // field
            boolean fieldCheckLevel, String fieldLevelMessage,
            // login
            boolean maintenance, String maintenanceMessage, int maintenanceCode, int maintenanceMinAccountType,
            Map<Integer, String> worldMessages, String loginBackground,
            // slide
            String slide, boolean slideChat,
            // features
            boolean trade, boolean mesoDrop, boolean rune, boolean auction, boolean cashShop,
            // npcs
            Set<Integer> disabledNpcs, Map<Integer, Set<Integer>> disabledNpcsByMap, List<CustomNpc> customNpcs) {
    }

    private static Path configFile = ProtocolConfig.CONFIG_FILE;
    private static volatile Snapshot current;
    private static volatile long lastModified = Long.MIN_VALUE;
    private static volatile long lastCheck;

    private GameSettings() {
    }

    /** Tests only. */
    static void useConfigFile(Path file) {
        configFile = file;
        load();
    }

    // ---------------------------------------------------------------------------------------------------- loading

    /** Re-reads the file now. */
    public static synchronized void load() {
        Properties p = new Properties();
        long modified = Long.MIN_VALUE;
        try {
            if (Files.isRegularFile(configFile)) {
                modified = Files.getLastModifiedTime(configFile).toMillis();
                p.load(new StringReader(decode(Files.readAllBytes(configFile))));
            }
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("[GameSettings] cannot read " + configFile + ": " + e.getMessage());
        }
        current = parse(p);
        lastModified = modified;
        lastCheck = System.currentTimeMillis();
    }

    private static Snapshot s() {
        Snapshot snap = current;
        long now = System.currentTimeMillis();
        if (snap == null) {
            load();
            return current;
        }
        if (now - lastCheck >= RELOAD_CHECK_MS) {
            lastCheck = now;
            try {
                long m = Files.isRegularFile(configFile) ? Files.getLastModifiedTime(configFile).toMillis() : Long.MIN_VALUE;
                if (m != lastModified) {
                    load();
                    System.out.println("[GameSettings] reloaded " + configFile);
                }
            } catch (IOException ignored) {
                // keep the current snapshot
            }
        }
        return current;
    }

    private static String decode(byte[] bytes) {
        try {
            return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException e) {
            return new String(bytes, StandardCharsets.ISO_8859_1);
        }
    }

    static Snapshot parse(Properties p) {
        return new Snapshot(
                parseLevelMap(get(p, "rate.exp.levels", "")),
                intKey(p, "rate.drop.boss", 0, 0, RateSettings.MAX),
                intKey(p, "rate.drop.normal", 0, 0, RateSettings.MAX),
                intKey(p, "rate.bonus.meso", 100, 0, 100_000),
                intKey(p, "rate.bonus.drop", 100, 0, 100_000),
                intKey(p, "rate.familiar.exp", 1, 0, RateSettings.MAX),
                intKey(p, "rate.option.grade.up", 100, 0, 100_000),
                intKey(p, "rate.option.grade.same", 100, 0, 100_000),
                intKey(p, "meso.pickup.max", 0, 0, Integer.MAX_VALUE),
                bool(p, "field.check.level", false),
                get(p, "field.check.level.message", DEFAULT_FIELD_LEVEL_MESSAGE),
                bool(p, "login.maintenance", false),
                get(p, "login.maintenance.message", DEFAULT_MAINTENANCE_MESSAGE),
                intKey(p, "login.maintenance.code", 4, 1, 255),
                intKey(p, "login.maintenance.minAccountType", AccountType.GameMaster.getVal(), 0, 4),
                worldMessages(p),
                get(p, "login.background", "").trim(),
                get(p, "game.slide", ""),
                bool(p, "game.slide.chat", true),
                bool(p, "feature.trade", true),
                bool(p, "feature.meso.drop", true),
                bool(p, "feature.rune", true),
                bool(p, "feature.auction", true),
                bool(p, "feature.cashshop", true),
                disabledNpcs(get(p, "world.disabled-npcs", "")),
                disabledNpcsByMap(get(p, "world.disabled-npcs", "")),
                customNpcs(p));
    }

    private static String get(Properties p, String key, String def) {
        String v = System.getProperty(key, p.getProperty(key));
        return v == null ? def : v;
    }

    private static boolean bool(Properties p, String key, boolean def) {
        String v = get(p, key, null);
        if (v == null || v.isBlank()) return def;
        v = v.trim();
        if (v.equalsIgnoreCase("true") || v.equals("1") || v.equalsIgnoreCase("yes") || v.equalsIgnoreCase("on")) return true;
        if (v.equalsIgnoreCase("false") || v.equals("0") || v.equalsIgnoreCase("no") || v.equalsIgnoreCase("off")) return false;
        System.err.println("[GameSettings] " + key + "=" + v + " is not a boolean, kept " + def);
        return def;
    }

    private static int intKey(Properties p, String key, int def, int min, int max) {
        String v = get(p, key, null);
        if (v == null || v.isBlank()) return def;
        try {
            int n = Integer.parseInt(v.trim());
            if (n >= min && n <= max) return n;
            System.err.println("[GameSettings] " + key + "=" + v + " out of range " + min + ".." + max + ", kept " + def);
        } catch (NumberFormatException e) {
            System.err.println("[GameSettings] " + key + "=" + v + " is not a number, kept " + def);
        }
        return def;
    }

    /** {@code 1:100,101:500,211:100} or MetaMS {@code {0:1,50:2}}; key = lowest level of the band. */
    static NavigableMap<Integer, Integer> parseLevelMap(String raw) {
        TreeMap<Integer, Integer> out = new TreeMap<>();
        String s = raw.trim();
        if (s.startsWith("{")) s = s.substring(1);
        if (s.endsWith("}")) s = s.substring(0, s.length() - 1);
        for (String part : s.split("[,;]")) {
            part = part.trim();
            if (part.isEmpty()) continue;
            String[] kv = part.split("[:=]", 2);
            try {
                int level = Integer.parseInt(kv[0].trim());
                int rate = Integer.parseInt(kv[1].trim());
                if (level < 0 || !RateSettings.valid(rate)) throw new NumberFormatException();
                out.put(level, rate);
            } catch (RuntimeException e) {
                System.err.println("[GameSettings] rate.exp.levels entry \"" + part + "\" ignored (use level:rate, rate "
                        + RateSettings.MIN + ".." + RateSettings.MAX + ")");
            }
        }
        return Collections.unmodifiableNavigableMap(out);
    }

    /** {@code world.message.<id>=text}. */
    private static Map<Integer, String> worldMessages(Properties p) {
        Map<Integer, String> out = new HashMap<>();
        Set<String> keys = new HashSet<>(p.stringPropertyNames());
        keys.addAll(System.getProperties().stringPropertyNames());
        for (String key : keys) {
            if (!key.startsWith("world.message.")) continue;
            try {
                out.put(Integer.parseInt(key.substring("world.message.".length()).trim()), get(p, key, ""));
            } catch (NumberFormatException e) {
                System.err.println("[GameSettings] " + key + " ignored (world id must be a number)");
            }
        }
        return Collections.unmodifiableMap(out);
    }

    /** Plain {@code npcId} entries apply to every map. */
    private static Set<Integer> disabledNpcs(String raw) {
        Set<Integer> out = new HashSet<>();
        for (String part : raw.split("[,;\\s]+")) {
            part = part.trim();
            if (part.isEmpty() || part.contains(":")) continue;
            try {
                out.add(Integer.parseInt(part));
            } catch (NumberFormatException e) {
                System.err.println("[GameSettings] world.disabled-npcs entry \"" + part + "\" ignored");
            }
        }
        return Collections.unmodifiableSet(out);
    }

    /** {@code mapId:npcId} entries apply to that map only (TMS 279 {@code [world.disabled-npcs] map = [npc]}). */
    private static Map<Integer, Set<Integer>> disabledNpcsByMap(String raw) {
        Map<Integer, Set<Integer>> out = new HashMap<>();
        for (String part : raw.split("[,;\\s]+")) {
            part = part.trim();
            if (!part.contains(":")) continue;
            String[] kv = part.split(":", 2);
            try {
                out.computeIfAbsent(Integer.parseInt(kv[0].trim()), k -> new HashSet<>()).add(Integer.parseInt(kv[1].trim()));
            } catch (NumberFormatException e) {
                System.err.println("[GameSettings] world.disabled-npcs entry \"" + part + "\" ignored");
            }
        }
        return Collections.unmodifiableMap(out);
    }

    /** {@code game.custom-npc.<n>=mapId,npcId,x,y[,facing]}, n = any label; sorted by key. */
    private static List<CustomNpc> customNpcs(Properties p) {
        Set<String> keys = new java.util.TreeSet<>(p.stringPropertyNames());
        keys.addAll(System.getProperties().stringPropertyNames());
        List<CustomNpc> out = new ArrayList<>();
        for (String key : keys) {
            if (!key.startsWith("game.custom-npc.")) continue;
            String v = get(p, key, "").trim();
            if (v.isEmpty()) continue;
            String[] f = v.split("\\s*,\\s*");
            try {
                if (f.length < 4 || f.length > 5) throw new NumberFormatException();
                int facing = f.length == 5 ? Integer.parseInt(f[4]) : 0;
                out.add(new CustomNpc(Integer.parseInt(f[0]), Integer.parseInt(f[1]), Integer.parseInt(f[2]),
                        Integer.parseInt(f[3]), facing));
            } catch (NumberFormatException e) {
                System.err.println("[GameSettings] " + key + "=" + v + " ignored (use mapId,npcId,x,y[,facing])");
            }
        }
        return Collections.unmodifiableList(out);
    }

    // ---------------------------------------------------------------------------------------------------- rates

    /**
     * Mob EXP multiplier for a character level: the {@code rate.exp.levels} band whose lowest level is the largest one
     * not above {@code level}; without such a band, the old three keys (rate.exp.1-100 / 101-210 / 211).
     */
    public static int expRate(int level) {
        Map.Entry<Integer, Integer> band = s().expByLevel().floorEntry(level);
        if (band != null) return band.getValue();
        if (level >= 1 && level <= 100) return ServerConfig.EXP_RATE_BELOW_100;
        if (level > 100 && level <= 210) return ServerConfig.EXP_RATE_101_210;
        return ServerConfig.EXP_RATE;
    }

    /** {@code rate.drop.boss} / {@code rate.drop.normal}; 0 (default) = {@code rate.drop}. */
    public static int dropRate(boolean boss) {
        Snapshot snap = s();
        int v = boss ? snap.dropBoss() : snap.dropNormal();
        return v > 0 ? v : ServerConfig.DROP_RATE;
    }

    /** Scales the character's own meso-rate bonus (buffs, skills, equipment, VIP) by {@code rate.bonus.meso} percent. */
    public static int bonusMeso(int characterBonus) {
        int pct = s().bonusMesoPercent();
        return pct == 100 ? characterBonus : (int) Math.min(Integer.MAX_VALUE, (long) characterBonus * pct / 100);
    }

    /** Scales the character's own drop-rate bonus by {@code rate.bonus.drop} percent. */
    public static int bonusDrop(int characterBonus) {
        int pct = s().bonusDropPercent();
        return pct == 100 ? characterBonus : (int) Math.min(Integer.MAX_VALUE, (long) characterBonus * pct / 100);
    }

    /** {@code rate.familiar.exp} (TMS 279 rate.familiar). Not applied yet: the server has no familiar EXP gain. */
    public static int familiarExpRate() {
        return s().familiarExp();
    }

    /** {@code rate.option.grade.up}: percent applied to a cube's tier-up chance (100 = unchanged). */
    public static int optionGradeUpPercent() {
        return s().optionGradeUpPercent();
    }

    /** {@code rate.option.grade.same}: percent for same-grade (premium) option rolls (100 = unchanged). */
    public static int optionGradeSamePercent() {
        return s().optionGradeSamePercent();
    }

    /** Tier-up chance scaled by {@code rate.option.grade.up}, capped at {@code cap} (the caller's 100%). */
    public static int scaleTierUpChance(int chance, int cap) {
        int pct = optionGradeUpPercent();
        if (pct == 100) return chance;
        return (int) Math.min(cap, (long) chance * pct / 100);
    }

    /** {@code meso.pickup.max}: largest meso amount of one mob / field drop; 0 = unlimited. */
    public static int capMesoDrop(int money) {
        int max = s().mesoPickupMax();
        return max > 0 && money > max ? max : money;
    }

    // ---------------------------------------------------------------------------------------------------- login

    public static boolean maintenance() {
        return s().maintenance();
    }

    public static String maintenanceMessage() {
        net.swordie.ms.lifecycle.MaintenanceCountdown c = net.swordie.ms.lifecycle.MaintenanceCountdown.standard();
        if (!s().maintenance() && c.active()) {
            return "伺服器即將維修（約 " + Math.max(1, (c.remainingSec() + 59) / 60) + " 分鐘後），暫停登入";
        }
        return s().maintenanceMessage();
    }

    /** LP0000 failure code used for the maintenance refusal (default 4, the code the wrong-password path uses). */
    public static int maintenanceCode() {
        return s().maintenanceCode();
    }

    /** True when {@code login.maintenance} is on and the user's account type is below the minimum. */
    /**
     * A new password login (CP0074): refused under {@link #refusedByMaintenance} and while a maintenance countdown runs
     * (MaintenanceCountdown; GMs at login.maintenance.minAccountType still pass). Players already online (relogin,
     * channel change) are not affected by the countdown.
     */
    public static boolean refusedNewLogin(User user) {
        if (refusedByMaintenance(user)) return true;
        if (!net.swordie.ms.lifecycle.MaintenanceCountdown.standard().active()) return false;
        return user == null || user.getAccountType() == null
                || user.getAccountType().getVal() < s().maintenanceMinAccountType();
    }

    public static boolean refusedByMaintenance(User user) {
        Snapshot snap = s();
        if (!snap.maintenance()) return false;
        return user == null || user.getAccountType() == null
                || user.getAccountType().getVal() < snap.maintenanceMinAccountType();
    }

    /** {@code world.message.<id>}; empty when not configured (the old behaviour). */
    public static String worldMessage(int worldId) {
        return s().worldMessages().getOrDefault(worldId, "");
    }

    /** {@code login.background}; empty = do not send LP007D (the old behaviour). */
    public static String loginBackground() {
        return s().loginBackground();
    }

    // ---------------------------------------------------------------------------------------------------- features

    public static boolean tradeEnabled() {
        return s().trade();
    }

    public static boolean mesoDropEnabled() {
        return s().mesoDrop();
    }

    public static boolean runeEnabled() {
        return s().rune();
    }

    public static boolean auctionEnabled() {
        return s().auction();
    }

    public static boolean cashShopEnabled() {
        return s().cashShop();
    }

    public static final String TRADE_DISABLED = "交易功能目前已關閉。";
    public static final String MESO_DROP_DISABLED = "丟棄楓幣功能目前已關閉。";
    public static final String RUNE_DISABLED = "輪符功能目前已關閉。";
    public static final String AUCTION_DISABLED = "拍賣場目前已關閉。";
    public static final String CASH_SHOP_DISABLED = "購物商城目前已關閉。";

    // ---------------------------------------------------------------------------------------------------- field

    private static boolean isGm(Char chr) {
        User u = chr.getUser();
        return u != null && u.getAccountType() != null && u.getAccountType().getVal() >= AccountType.GameMaster.getVal();
    }

    /**
     * {@code field.check.level}: refuses a warp into a field whose {@code lvLimit} is above the character's level
     * (GMs pass). Sends the notice itself. Always true while the key is off.
     */
    public static boolean canEnterField(Char chr, Field toField) {
        Snapshot snap = s();
        if (!snap.fieldCheckLevel() || chr == null || toField == null || isGm(chr)) return true;
        int req = toField.getLvLimit();
        if (req > 0 && chr.getLevel() < req) {
            String msg;
            try {
                msg = String.format(snap.fieldLevelMessage(), req);
            } catch (java.util.IllegalFormatException e) {
                msg = snap.fieldLevelMessage();
            }
            chr.chatMessage(msg);
            return false;
        }
        return true;
    }

    /** {@code game.slide}: the slide notice text; empty when not configured (the launcher's news shows it too). */
    public static String slide() {
        return s().slide();
    }

    /** {@code game.slide}: slide notice (and, with {@code game.slide.chat}, a chat line) on every channel entry. */
    public static void onChannelEnter(Char chr) {
        Snapshot snap = s();
        if (chr == null || snap.slide().isBlank()) return;
        try {
            chr.write(WvsContext.broadcastMsg(BroadcastMsg.slideNotice(snap.slide(), true)));
            if (snap.slideChat()) {
                chr.chatMessage(snap.slide());
            }
        } catch (RuntimeException e) {
            System.err.println("[GameSettings] slide notice for " + chr.getName() + " not sent: " + e);
        }
    }

    /** True when {@code world.disabled-npcs} hides this NPC on this map. */
    public static boolean npcDisabled(int mapId, int npcId) {
        Snapshot snap = s();
        if (snap.disabledNpcs().contains(npcId)) return true;
        Set<Integer> onMap = snap.disabledNpcsByMap().get(mapId);
        return onMap != null && onMap.contains(npcId);
    }

    public static List<CustomNpc> customNpcs() {
        return s().customNpcs();
    }

    /**
     * Applied by {@code FieldData.getFieldCopyById} to every new field instance: removes the disabled NPCs, adds the
     * custom ones and drops the initial rune when {@code feature.rune=false}. No-op with the default keys.
     */
    public static void applyToNewField(Field field) {
        if (field == null) return;
        Snapshot snap = s();
        if (!snap.rune()) {
            field.setRuneStone(null);
        }
        if (!snap.disabledNpcs().isEmpty() || !snap.disabledNpcsByMap().isEmpty()) {
            List<Integer> remove = new ArrayList<>();
            for (Life l : field.getLifes().values()) {
                if (l instanceof Npc && npcDisabled(field.getId(), l.getTemplateId())) remove.add(l.getObjectId());
            }
            for (int id : remove) field.removeLife(id, false, false);
            field.getOriginalNpcs().removeIf(n -> npcDisabled(field.getId(), n.getTemplateId()));
        }
        for (CustomNpc c : snap.customNpcs()) {
            if (c.mapId() != field.getId()) continue;
            Npc npc = NpcData.getNpcDeepCopyById(c.npcId());
            if (npc == null) {
                System.err.println("[GameSettings] custom NPC " + c.npcId() + " not in server data (map " + c.mapId() + ")");
                continue;
            }
            npc.setPosition(new Position(c.x(), c.y()));
            npc.setCy(c.y());
            npc.setRx0(c.x() - 50);
            npc.setRx1(c.x() + 50);
            Foothold fh = field.findFootHoldBelow(new Position(c.x(), c.y() - 2));
            npc.setFh(fh == null ? 0 : fh.getId());
            npc.setFlip(c.facing() == 1);
            field.addLife(npc);
            field.getOriginalNpcs().add(npc);
        }
    }

    /** Human-readable summary for logs / admin commands. */
    public static String summary() {
        Snapshot s = s();
        return "等級經驗表=" + (s.expByLevel().isEmpty() ? "(使用三段倍率)" : s.expByLevel())
                + " 王怪掉寶=" + dropRate(true) + " 一般掉寶=" + dropRate(false)
                + " 楓幣加成%=" + s.bonusMesoPercent() + " 掉寶加成%=" + s.bonusDropPercent()
                + " 楓幣上限=" + s.mesoPickupMax() + " 維護=" + s.maintenance()
                + " 交易=" + s.trade() + " 丟楓幣=" + s.mesoDrop() + " 輪符=" + s.rune()
                + " 拍賣=" + s.auction() + " 商城=" + s.cashShop();
    }
}
