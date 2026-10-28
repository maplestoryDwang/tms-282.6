package net.swordie.ms.world.kit;

import com.fasterxml.jackson.databind.JsonNode;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.avatar.AvatarLook;
import net.swordie.ms.client.character.items.Equip;
import net.swordie.ms.client.character.items.EquipFlame;
import net.swordie.ms.client.character.items.Inventory;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.client.character.items.ItemOption;
import net.swordie.ms.client.character.commands.AdminCommands;
import net.swordie.ms.connection.packet.UserRemote;
import net.swordie.ms.connection.packet.WvsContext;
import net.swordie.ms.constants.GameConstants;
import net.swordie.ms.constants.ItemConstants;
import net.swordie.ms.constants.JobNameCatalog;
import net.swordie.ms.enums.AvatarModifiedMask;
import net.swordie.ms.enums.EquipBaseStat;
import net.swordie.ms.enums.InvType;
import net.swordie.ms.enums.InventoryOperation;
import net.swordie.ms.enums.Stat;
import net.swordie.ms.loaders.ItemData;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * "Character kit": copies the look and gear of an official character (a MapleKit / Nexon Open API character JSON) onto
 * an online character of this server - level, job (when the class name is known), skills, hair / face / skin, every
 * equip with its star force, scroll stats, flames and both potentials, and the worn cash items.
 * <p>
 * {@link #build} maps the source to ids: item, hair, face and skin names through the 282 client's own name table
 * ({@code data/protocol/tms282.3/eqp-names-zh.tsv}, dumped from String/Eqp.img); potentials by their text with the
 * numbers removed inside the grade's id range, the candidate whose value equals the text's number first. What has no
 * server data (puzzle pieces, the jewel) is listed as skipped. {@link #apply} then does what the one-off script
 * content/kit_import.py did (2026-10-07, 28/28 equips on the first live run): each piece on its own, a failure is
 * reported and does not stop the rest.
 */
public final class CharacterKit {
    public static final Path NAMES = Path.of("data", "protocol", "tms282.3", "eqp-names-zh.tsv");
    private static final Map<String, Integer> GRADE = Map.of("特殊", 1, "稀有", 2, "罕見", 3, "傳說", 4);
    private static final Pattern STRIP = Pattern.compile("[\\d\\s+\\-.：:秒]");
    private static final Pattern NUMBER = Pattern.compile("\\d+(?:\\.\\d+)?");
    private static final Map<String, String> SCROLL = Map.ofEntries(Map.entry("iStr", "str"), Map.entry("iDex", "dex"), Map.entry("iInt", "int"),
            Map.entry("iLuk", "luk"), Map.entry("iMaxHP", "max_hp"), Map.entry("iMaxMP", "max_mp"), Map.entry("iPAD", "attack_power"),
            Map.entry("iMAD", "magic_power"), Map.entry("iPDD", "armor"), Map.entry("iSpeed", "speed"), Map.entry("iJump", "jump"));
    private static final Map<String, String> FLAME = Map.ofEntries(Map.entry("STR", "str"), Map.entry("DEX", "dex"), Map.entry("INT", "int"),
            Map.entry("LUK", "luk"), Map.entry("HP", "max_hp"), Map.entry("MP", "max_mp"), Map.entry("PAD", "attack_power"), Map.entry("MAD", "magic_power"),
            Map.entry("PDD", "armor"), Map.entry("Speed", "speed"), Map.entry("Jump", "jump"), Map.entry("AllStatR", "all_stat"),
            Map.entry("BossDamageR", "boss_damage"), Map.entry("Damage", "damage"), Map.entry("ReduceReqLevel", "equipment_level_decrease"));
    private static volatile Map<String, List<Integer>> names;

    private CharacterKit() {
    }

    /** One equip of the kit, ids resolved. */
    public record Piece(int id, String name, String slot, int stars, int cuc, int[] pot, int[] bonus, Map<String, Integer> scroll,
                        Map<String, Integer> flame) {
    }

    /** {@code gender}: the source character's (0 male, 1 female, -1 unknown); hair and face ids are gendered. */
    public record Kit(String source, int level, int job, int hair, int face, int skin, List<Piece> equips, List<int[]> cash, List<String> cashNames,
                      List<String> skipped, int gender) {
    }

    /** What {@link #apply} did. */
    public record Report(List<String> done, List<String> failed) {
    }

    // ------------------------------------------------------------------ name table

    static Map<String, List<Integer>> names() throws IOException {
        Map<String, List<Integer>> n = names;
        if (n == null) {
            n = new HashMap<>();
            for (String line : Files.readAllLines(NAMES, StandardCharsets.UTF_8)) {
                String[] p = line.split("\t", 3);
                if (p.length == 3 && p[1].matches("\\d{1,9}")) {
                    n.computeIfAbsent(p[2].strip(), k -> new ArrayList<>()).add(Integer.parseInt(p[1]));
                }
            }
            names = n;
        }
        return n;
    }

    private static int num(JsonNode n) {
        if (n == null || n.isNull()) {
            return 0;
        }
        if (n.isNumber()) {
            return n.asInt();
        }
        try {
            return (int) Double.parseDouble(n.asText("0").trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static Map<String, Integer> stats(JsonNode o, Map<String, String> keys) {
        Map<String, Integer> out = new LinkedHashMap<>();
        if (o != null && o.isObject()) {
            keys.forEach((to, from) -> {
                int v = num(o.get(from));
                if (v != 0) {
                    out.put(to, v);
                }
            });
        }
        return out;
    }

    /** The id of a named equip: the one whose base stats match the source's when the name has several. */
    static int pick(String name, JsonNode base) throws IOException {
        List<Equip> found = new ArrayList<>();
        for (int id : names().getOrDefault(name.strip(), List.of())) {
            Equip e = id / 1000000 == 1 ? ItemData.getEquipById(id) : null;
            if (e != null) {
                found.add(e);
            }
        }
        if (found.isEmpty()) {
            return 0;
        }
        if (found.size() > 1 && base != null && base.isObject()) {
            List<Equip> same = found.stream().filter(e -> e.getiLuk() == num(base.get("luk")) && e.getiStr() == num(base.get("str"))
                    && e.getiDex() == num(base.get("dex")) && e.getiInt() == num(base.get("int"))).toList();
            if (!same.isEmpty()) {
                found = new ArrayList<>(same);
            }
        }
        found.sort((x, y) -> Integer.compare(y.getTuc(), x.getTuc())); // ties: the variant with more upgrade slots
        return found.get(0).getItemId();
    }

    /** The potential option of a text line ("LUK +13%"); 0 when nothing matches. */
    static int option(String text, String grade, boolean bonus) {
        Integer g = grade == null ? null : GRADE.get(grade);
        if (text == null || text.isBlank() || g == null) {
            return 0;
        }
        String norm = STRIP.matcher(text).replaceAll("");
        Double want = null;
        Matcher m = NUMBER.matcher(text);
        while (m.find()) {
            want = Double.parseDouble(m.group());
        }
        int best = 0;
        double bestValue = -1;
        for (ItemOption o : ItemData.itemOptions.values()) {
            int id = o.getId();
            if (id / 10000 != g || ((id / 1000) % 10 == 2) != bonus) {
                continue;
            }
            String s;
            try {
                s = o.getString(25);
            } catch (RuntimeException e) {
                continue;
            }
            if (s == null || !STRIP.matcher(s).replaceAll("").equals(norm)) {
                continue;
            }
            double value = 0;
            try {
                for (double v : o.getStatValuesByLevel(25).values()) {
                    value = Math.max(value, v);
                }
            } catch (RuntimeException e) {
                // no stat values: a misc option
            }
            if (want != null && value == want) {
                return id;
            }
            if (value > bestValue) {
                bestValue = value;
                best = id;
            }
        }
        return best;
    }

    /** A fourth-job id whose line name is {@code className}, or 0. */
    static int jobOf(String className) {
        int best = 0;
        for (int id : JobNameCatalog.knownJobIds()) {
            if (className.equals(JobNameCatalog.lineName(id)) && id % 10 == 2 && id > best) {
                best = id;
            }
        }
        return best;
    }

    // ------------------------------------------------------------------ build

    public static Kit build(JsonNode source) throws IOException {
        JsonNode basic = source.path("basic");
        List<Piece> equips = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        for (JsonNode it : source.path("itemEquipment").path("item_equipment")) {
            String name = it.path("item_name").asText("");
            int id = pick(name, it.get("item_base_option"));
            if (id == 0) {
                skipped.add(it.path("item_equipment_slot").asText("") + " " + name);
                continue;
            }
            int[] pot = new int[3], bonus = new int[3];
            for (int i = 0; i < 3; i++) {
                pot[i] = option(it.path("potential_option_" + (i + 1)).asText(null), it.path("potential_option_grade").asText(null), false);
                bonus[i] = option(it.path("additional_potential_option_" + (i + 1)).asText(null), it.path("additional_potential_option_grade").asText(null), true);
            }
            equips.add(new Piece(id, name, it.path("item_equipment_slot").asText(""), num(it.get("starforce")), num(it.get("scroll_upgrade")), pot, bonus,
                    stats(it.get("item_etc_option"), SCROLL), stats(it.get("item_add_option"), FLAME)));
        }
        List<int[]> cash = new ArrayList<>();
        List<String> cashNames = new ArrayList<>();
        JsonNode cashRoot = source.path("cashItemEquipment");
        int preset = Math.max(1, Math.min(3, cashRoot.path("preset_no").asInt(1)));
        JsonNode worn = cashRoot.path("cash_item_equipment_preset_" + preset);
        if (!worn.isArray() || worn.isEmpty()) {
            worn = cashRoot.path("cash_item_equipment_base");
        }
        for (JsonNode it : worn) {
            String name = it.path("cash_item_name").asText("");
            int id = pick(name, null);
            if (id == 0) {
                skipped.add("時裝 " + name);
            } else {
                cash.add(new int[]{id});
                cashNames.add(name);
            }
        }
        JsonNode beauty = source.path("beautyEquipment");
        int hair = first(beauty.path("character_hair").path("hair_name").asText(""), 30000, 70000);
        int face = faceOf(beauty.path("character_face").path("face_name").asText(""), beauty.path("character_face").path("base_color").asText(""));
        int skinItem = first(beauty.path("character_skin").path("skin_name").asText(""), 12000, 13000);
        return new Kit(basic.path("character_name").asText(""), Math.max(1, Math.min(300, basic.path("character_level").asInt(1))),
                jobOf(basic.path("character_class").asText("")), hair, face, skinItem == 0 ? -1 : skinItem - 12000, equips, cash, cashNames, skipped,
                genderOf(basic.path("character_gender").asText("")));
    }

    /** 男 0, 女 1, otherwise -1. */
    static int genderOf(String text) {
        return switch (text.strip()) {
            case "男" -> 0;
            case "女" -> 1;
            default -> -1;
        };
    }

    /** Lens colours in id order (hundreds digit); the mixed colour of the source is not reproduced. */
    private static final List<String> FACE_COLORS = List.of("黑色", "藍色", "紅色", "綠色", "褐色", "翡翠色", "紫色", "紫水晶色", "白色");

    /** Every colour of a face shares one name: take the id whose hundreds digit is the source's base colour. */
    private static int faceOf(String name, String color) throws IOException {
        List<Integer> ids = names().getOrDefault(name.strip(), List.of()).stream().filter(i -> i >= 20000 && i < 60000).sorted().toList();
        if (ids.isEmpty()) {
            return 0;
        }
        int index = FACE_COLORS.indexOf(color.strip());
        for (int id : ids) {
            if (index >= 0 && (id / 100) % 10 == index) {
                return id;
            }
        }
        return ids.get(0);
    }

    private static int first(String name, int min, int max) throws IOException {
        for (int id : names().getOrDefault(name.strip(), List.of())) {
            if (id >= min && id < max) {
                return id;
            }
        }
        return 0;
    }

    // ------------------------------------------------------------------ apply

    static Equip make(Piece p) throws ReflectiveOperationException {
        Equip equip = ItemData.getEquipDeepCopyFromID(p.id(), false);
        if (equip == null) {
            throw new IllegalStateException("沒有裝備資料");
        }
        p.scroll().forEach((k, v) -> equip.addStat(EquipBaseStat.valueOf(k), v));
        if (p.cuc() > 0) {
            equip.setCuc(p.cuc());
            equip.setTuc((short) 0);
        }
        EquipFlame flame = equip.getFlameStat();
        for (var e : p.flame().entrySet()) {
            EquipFlame.class.getMethod("set" + e.getKey(), int.class).invoke(flame, e.getValue());
        }
        int stars = Math.min(p.stars(), GameConstants.getMaxStars(equip));
        if (stars > 0) {
            equip.setChuc((short) stars, true);
        }
        List<Integer> options = equip.getOptions();
        while (options.size() < 7) {
            options.add(0);
        }
        for (int i = 0; i < 3; i++) {
            if (p.pot()[i] != 0) {
                equip.setOptionBase(i, p.pot()[i]);
            }
            if (p.bonus()[i] != 0) {
                equip.setOptionBonus(i, p.bonus()[i]);
            }
        }
        return equip;
    }

    private static int putOn(Char chr, Equip equip, Set<Integer> used) {
        int gender = chr.getAvatarData().getAvatarLook().getGender();
        int slot = 0;
        List<Integer> slots = ItemConstants.getBodyPartArrayFromItem(equip.getItemId(), gender);
        for (int s : slots) {
            if (!used.contains(s)) {
                slot = s;
                break;
            }
        }
        if (slot == 0) {
            throw new IllegalStateException(slots.isEmpty() ? "這個角色的性別不能穿" : "沒有空的欄位");
        }
        used.add(slot);
        Inventory equipped = chr.getEquippedInventory();
        Item old = equipped.getItemBySlot(slot);
        if (old != null) {
            Inventory target = old.isCash() ? chr.getDecorationInventory() : chr.getEquipInventory();
            if (target.getEmptySlots() > 0) {
                old.setInventoryID(target.getId());
                old.setInvType(old.isCash() ? InvType.DECORATION : InvType.EQUIP);
                chr.unequip(old, target.getFirstOpenSlot());
                old.updateToChar(chr);
            } else {
                throw new IllegalStateException("背包滿了，原本那件換不下來");
            }
        }
        equip.setInventoryID(equipped.getId());
        equip.setInvType(InvType.EQUIPPED);
        chr.equip(equip, slot);
        equip.updateToChar(chr);
        return slot;
    }

    /** Applies the kit to an online character. Pieces that fail are reported, the rest go on. */
    public static Report apply(Char chr, Kit kit, boolean levelAndJob) {
        List<String> done = new ArrayList<>(), failed = new ArrayList<>();
        if (levelAndJob) {
            try {
                if (kit.job() != 0 && chr.getJob() != kit.job()) {
                    AdminCommands.Job.execute(chr, new String[]{"job", String.valueOf(kit.job())});
                }
                if (chr.getLevel() < kit.level()) {
                    AdminCommands.Level.execute(chr, new String[]{"level", String.valueOf(kit.level())});
                }
                chr.maxSkills();
                done.add((kit.job() == 0 ? "職業不變" : "職業 " + kit.job()) + "、等級 " + Math.max(chr.getLevel(), kit.level()) + "、技能全滿");
            } catch (RuntimeException e) {
                failed.add("職業／等級：" + e);
            }
        }
        try {
            AvatarLook look = chr.getAvatarData().getAvatarLook();
            if (kit.skin() >= 0) {
                look.setSkin(kit.skin());
                chr.setStatAndSendPacket(Stat.skin, kit.skin());
            }
            if (kit.face() != 0) {
                look.setFace(kit.face());
                chr.setStatAndSendPacket(Stat.face, kit.face());
            }
            if (kit.hair() != 0) {
                look.setHair(kit.hair());
                chr.setStatAndSendPacket(Stat.hair, kit.hair());
            }
            if (chr.getField() != null) {
                chr.getField().broadcast(UserRemote.avatarModified(chr, AvatarModifiedMask.AvatarLook.getVal(), (byte) 0), chr);
            }
            done.add("髮型 " + kit.hair() + "、臉型 " + kit.face() + "、膚色 " + kit.skin());
            // 2026-10-08: a male character with the female source's hair is drawn bald on the 282 select screen and quick
            // login (the game itself draws it). The gender is not changed here: the target's other equips follow it.
            if (kit.gender() >= 0 && kit.gender() != chr.getAvatarData().getCharacterStat().getGender()) {
                failed.add("性別不同：官方角色是" + (kit.gender() == 1 ? "女性" : "男性") + "，這隻是" + (kit.gender() == 1 ? "男性" : "女性")
                        + "。髮型與臉型在選角畫面和快速登入不會顯示（進遊戲才看得到）；請用同性別的角色複製");
            }
        } catch (RuntimeException e) {
            failed.add("外觀：" + e);
        }
        Set<Integer> used = new HashSet<>();
        int ok = 0;
        for (Piece p : kit.equips()) {
            try {
                putOn(chr, make(p), used);
                ok++;
            } catch (Exception e) {
                failed.add(p.name() + "（" + p.id() + "）：" + (e.getCause() != null ? e.getCause() : e).getMessage());
            }
        }
        done.add("一般裝備 " + ok + "／" + kit.equips().size() + " 件");
        ok = 0;
        for (int i = 0; i < kit.cash().size(); i++) {
            int id = kit.cash().get(i)[0];
            try {
                Equip equip = ItemData.getEquipDeepCopyFromID(id, false);
                if (equip == null) {
                    throw new IllegalStateException("沒有裝備資料");
                }
                try {
                    putOn(chr, equip, used);
                } catch (IllegalStateException wear) {
                    // cannot be worn (other gender, no slot): hand it over in the cash-equip bag instead
                    if (chr.getDecorationInventory().getEmptySlots() <= 0) {
                        throw wear;
                    }
                    chr.addItemToInventory(InvType.DECORATION, equip, false, false);
                    chr.write(WvsContext.inventoryOperation(true, false, InventoryOperation.Add, (short) equip.getBagIndex(), (byte) -1, 0, equip));
                    failed.add("時裝 " + kit.cashNames().get(i) + "（" + id + "）穿不上（" + wear.getMessage() + "），已放進時裝欄");
                    continue;
                }
                ok++;
            } catch (Exception e) {
                failed.add("時裝 " + kit.cashNames().get(i) + "（" + id + "）：" + e.getMessage());
            }
        }
        done.add("時裝 " + ok + "／" + kit.cash().size() + " 件");
        System.out.println("[KitImport] character=" + chr.getId() + " source=" + kit.source() + " done=" + done + " failed=" + failed.size());
        return new Report(done, failed);
    }
}
