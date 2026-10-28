package net.swordie.ms.world.tms282.contents.changeburning;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * 變身燃燒：露希妲 (TMS 282.3 event 2026-09-09 .. 2026-12-01, research/tms2823/change-burning-lucid/README.md).
 * One designated character per account (NPC 9063907, npc_9063907.py) gets 80004200 「CHANGE：露希妲」. Casting it
 * makes the client's content component send CP04E1 {@code 5, 1}; the server then sets or removes the CTS
 * RPEventStat (bit 978, r = 80004200) and tells manager 5 to start / end (LP02C7 sub 2).
 * <p>
 * lucidskill 09-29 (research change-burning-lucid/SKILLS-FIX.md): CP04E1 action 1 is answered with LP064C
 * {@code 5, 1, 0} (else the client never sends it again and the skill cannot end the transformation); the Lucid
 * skills 800042xx of the Lucid level are granted as skill records; the transformation switches the key preset to
 * the event preset 3 (QR 100972 no=3, Lucid layout built from the active preset, LP0B16) and back.
 * <p>
 * Not done yet (phase B/C of the research plan): 惡夢叢林 (993294500 / 993295500) and its tickets, Lucid exp from the
 * nightmare mobs, the weekly / season quests, 夢幻的裝備 / 結晶, the Lucid stat tables and the 19 RPEventStat ints.
 * State: {@code data/tms282-changeburning/designated.properties} (account id = character id); Lucid level / exp are
 * the character's QR 600384.
 */
public final class ChangeBurning {
    public static final int CHANGE_SKILL = 80004200;
    public static final int QR_LUCID = 600384;
    public static final int QR_PASS = 102930;
    public static final int MIN_LEVEL = 260;
    public static final int MAX_LUCID_LEVEL = 100;
    public static final ZoneId ZONE = ZoneId.of("Asia/Taipei");
    public static final LocalDateTime EVENT_END = LocalDateTime.of(2026, 12, 1, 23, 59, 59);

    /** QR 100972 (QuestConstants.KEY_BINDINGS_PRESET): {@code no} = active key preset, read by the client (3AEC760). */
    public static final int QR_KEY_PRESET = 100972;
    /** The 4th LP0B16 preset; 3AF8E30 lets Lucid skills 80004201..80004236 be bound only here. */
    public static final int EVENT_KEY_PRESET = 3;
    /** QR 100972 key keeping the preset that was active before the transformation. */
    static final String PREV_PRESET_KEY = "cbPrev";
    static final int SKILL_KEY_TYPE = 1;
    static final int MACRO_KEY_TYPE = 8;
    static final int KEY_COUNT = 89;

    /**
     * Lucid skills {id, reqLev} from Skill.wz 800042.img (reqLev = Lucid level, QR 600384 lv). 80004228 does not
     * exist; 80004237..80004239 (disable / fixLevel item skills) are not granted.
     */
    static final int[][] LUCID_SKILLS = {
            {80004201, 1}, {80004202, 1}, {80004203, 1}, {80004204, 1}, {80004205, 10}, {80004206, 1},
            {80004207, 1}, {80004208, 1}, {80004209, 1}, {80004210, 1}, {80004211, 1}, {80004212, 1},
            {80004213, 1}, {80004214, 1}, {80004215, 1}, {80004216, 1}, {80004217, 45}, {80004218, 45},
            {80004219, 20}, {80004220, 20}, {80004221, 20}, {80004222, 20}, {80004223, 20}, {80004224, 20},
            {80004225, 1}, {80004226, 1}, {80004227, 1}, {80004229, 10}, {80004230, 10}, {80004231, 10},
            {80004232, 10}, {80004233, 10}, {80004234, 1}, {80004235, 1}, {80004236, 1},
    };
    /** Castable Lucid skills in the order they take the keys freed from the character's own skills. */
    static final int[] LUCID_KEY_ORDER = {
            80004202, 80004206, 80004212, 80004213, 80004214, 80004205, 80004207, 80004210, 80004215, 80004216,
            80004219, 80004217, 80004218, 80004201, 80004225, 80004234, 80004235,
    };
    /** Keys (FuncKeyMapped index) used when fewer own skill keys were freed: Shift Ctrl Alt, A S D F, Z X C, Q W E R, 1..0. */
    static final int[] FALLBACK_KEYS = {42, 29, 56, 30, 31, 32, 33, 44, 45, 46, 16, 17, 18, 19, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};

    private static volatile ChangeBurning instance;

    private final Path file;
    private final Properties designated = new Properties();

    ChangeBurning(Path file) {
        this.file = file.toAbsolutePath().normalize();
        if (Files.isRegularFile(this.file)) {
            try (InputStream in = Files.newInputStream(this.file)) {
                designated.load(in);
            } catch (IOException e) {
                System.err.println("[ChangeBurning] unreadable " + this.file + ": " + e);
            }
        }
    }

    public static ChangeBurning get() {
        ChangeBurning i = instance;
        if (i == null) {
            synchronized (ChangeBurning.class) {
                i = instance;
                if (i == null) {
                    i = new ChangeBurning(Path.of(System.getProperty("tms282.changeBurning.file",
                            "data/tms282-changeburning/designated.properties")));
                    instance = i;
                }
            }
        }
        return i;
    }

    public boolean eventActive() {
        return !LocalDateTime.now(ZONE).isAfter(EVENT_END);
    }

    private static boolean tms282() {
        return Protocol.active().version() == ProtocolVersion.TMS_282_3;
    }

    /** The designated character of the account, 0 when none. */
    public synchronized int designatedCharacter(int accountId) {
        String v = designated.getProperty(String.valueOf(accountId));
        try {
            return v == null ? 0 : Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public boolean isDesignated(Char chr) {
        return chr != null && chr.getId() > 0 && designatedCharacter(chr.getAccId()) == chr.getId();
    }

    /** Designates {@code chr} (replacing the account's previous choice); null on success, else the reason. */
    public String designate(Char chr) {
        if (!eventActive()) {
            return "變身燃燒活動已經結束了。";
        }
        if (chr.getLevel() < MIN_LEVEL) {
            return "需要 Lv." + MIN_LEVEL + " 以上的角色才能指定為變身燃燒角色。";
        }
        int previous;
        synchronized (this) {
            previous = designatedCharacter(chr.getAccId());
            designated.setProperty(String.valueOf(chr.getAccId()), String.valueOf(chr.getId()));
            save();
        }
        if (previous > 0 && previous != chr.getId()) {
            releaseOnline(previous);
        }
        setUp(chr);
        chr.getClient().write(ChangeBurningPackets.contentsCreate());
        chr.getClient().write(ChangeBurningPackets.userContentsCreate());
        System.out.printf("[ChangeBurning] account %d designated character %d%n", chr.getAccId(), chr.getId());
        return null;
    }

    private synchronized void save() {
        try {
            Files.createDirectories(file.getParent());
            Path temp = Files.createTempFile(file.getParent(), "designated-", ".tmp");
            try {
                try (OutputStream out = Files.newOutputStream(temp)) {
                    designated.store(out, "TMS282 變身燃燒 designated character per account");
                }
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(temp);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** CHANGE skill, the Lucid skills of the current Lucid level, Lucid QR records and the exp table. */
    private void setUp(Char chr) {
        if (chr.getSkill(CHANGE_SKILL) == null) {
            chr.addSkill(CHANGE_SKILL, 1, 1);
        }
        if (chr.getQuestById(QR_LUCID) == null) {
            chr.createQuestWithQRValue(QR_LUCID, "lv=1;exp=0");
        } else {
            chr.write(net.swordie.ms.connection.packet.WvsContext.questRecordExMessage(chr.getQuestById(QR_LUCID)));
        }
        grantLucidSkills(chr);
        if (chr.getQuestById(QR_PASS) == null) {
            chr.createQuestWithQRValue(QR_PASS, "passCnt=0;ui=max;start=1");
        } else if (chr.getQRValueByKey(QR_PASS, "start") == null) {
            // lucidprog: QR 102930 start=1 lets the client auto-start the weekly / season missions (item5 report)
            chr.setQRValueByKey(QR_PASS, "start", "1");
        } else {
            chr.write(net.swordie.ms.connection.packet.WvsContext.questRecordExMessage(chr.getQuestById(QR_PASS)));
        }
        // lucidprog: QR 102883 (UI 剩餘食夢者 = eaterMaxCnt - eaterKillCnt; season target of quest 102942 = 220,000)
        if (chr.getQuestById(LucidDream.QR_COUNTS) == null) {
            chr.createQuestWithQRValue(LucidDream.QR_COUNTS, "eaterMaxCnt=220000;eaterKillCnt=0;bossCnt=0");
        } else {
            chr.write(net.swordie.ms.connection.packet.WvsContext.questRecordExMessage(chr.getQuestById(LucidDream.QR_COUNTS)));
        }
        // bossmech1-0930: LP02CC = stat table 80004228 (level -> StatBlock, 自訂 LucidAbility.levelTable) + the same exp
        // table 4 (traced CLEAN, lucid-re-0930/ability/out/java-lp02cc.bin): rpStatViewer "基本" is no longer 0
        chr.getClient().write(LucidAbility.rpTables(LucidAbility.levelTable(), expTable()));
        try {
            net.swordie.ms.world.tms282.contents.changeburning.dream.DreamService.get().sendAll(chr); // content-1005: 露希妲物品欄 (off by default)
        } catch (RuntimeException | LinkageError e) {
            System.out.println("[ChangeBurning] dream bag send failed for character " + chr.getId() + ": " + e);
        }
    }

    /**
     * lucidprog: the account designated another character; if the old one is online it is untransformed and loses
     * manager 5 / the user component (LP02CA / LP064E; a remove of an absent content is a no-op: 1826730 / 1826950
     * return when the FNV lookup misses, static).
     */
    private void releaseOnline(int characterId) {
        try {
            Char old = net.swordie.ms.Server.get().getChars().get(characterId);
            if (old == null || old.getClient() == null) {
                return;
            }
            if (isTransformed(old)) {
                toggle(old);
            }
            old.getClient().write(ChangeBurningPackets.userContentsRemove());
            old.getClient().write(ChangeBurningPackets.contentsRemove());
            old.chatMessage("[變身燃燒] 帳號已改為指定其他角色，這個角色的變身燃燒已結束。");
        } catch (RuntimeException e) {
            System.out.println("[ChangeBurning] release of character " + characterId + " failed: " + e);
        }
    }

    /** Lucid level from QR 600384 {@code lv} (1 when missing or malformed, capped at 100). */
    public static int lucidLevel(Char chr) {
        if (chr.getQuestById(QR_LUCID) == null) {
            return 1;
        }
        try {
            String v = chr.getQRValueByKey(QR_LUCID, "lv");
            return v == null ? 1 : Math.max(1, Math.min(MAX_LUCID_LEVEL, Integer.parseInt(v.trim())));
        } catch (RuntimeException e) {
            return 1;
        }
    }

    /**
     * Skill records of the Lucid skills whose reqLev is reached (Skill.wz: invisibleEvenSkillLevel, so the official
     * server grants them as normal records; the client only lets them be used while RPEventStat r = 80004200/80004228,
     * C8A5B0). Missing ones are added and sent in one CHANGE_SKILL_RECORD_RESULT. Returns how many were added.
     */
    public static int grantLucidSkills(Char chr) {
        int lv = lucidLevel(chr);
        List<net.swordie.ms.client.character.skills.Skill> added = new ArrayList<>();
        for (int[] s : LUCID_SKILLS) {
            if (s[1] > lv) {
                continue;
            }
            net.swordie.ms.client.character.skills.Skill have = chr.getSkill(s[0]);
            if (have != null && have.getCurrentLevel() > 0) {
                continue;
            }
            net.swordie.ms.client.character.skills.Skill skill = net.swordie.ms.loaders.SkillData.getSkillDeepCopyById(s[0]);
            if (skill == null) {
                continue; // not in the server's skill data (Char.addSkill would throw on its format string)
            }
            skill.setCurrentLevel(1);
            skill.setMasterLevel(1);
            chr.addSkill(skill);
            added.add(skill);
        }
        if (!added.isEmpty()) {
            chr.write(net.swordie.ms.connection.packet.WvsContext.changeSkillRecordResult(added, true, false, false));
            System.out.printf("[ChangeBurning] character %d got %d Lucid skill(s) (Lucid Lv.%d)%n", chr.getId(), added.size(), lv);
        }
        return added.size();
    }

    // ---- key preset (QR 100972 no, LP0B16 preset 3) ---------------------------------------------------------------

    static int parsePreset(String v, int fallback) {
        try {
            int p = v == null ? fallback : Integer.parseInt(v.trim());
            return p >= 0 && p <= EVENT_KEY_PRESET ? p : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static int activePreset(Char chr) {
        return chr.getQuestById(QR_KEY_PRESET) == null ? 0 : parsePreset(chr.getQRValueByKey(QR_KEY_PRESET, "no"), 0);
    }

    private static void setPresetQr(Char chr, String key, int value) {
        if (chr.getQuestById(QR_KEY_PRESET) == null) {
            chr.createQuestWithQRValue(QR_KEY_PRESET, "no=0;unionkey=1");
        }
        chr.setQRValueByKey(QR_KEY_PRESET, key, String.valueOf(value));
    }

    static boolean isLucidSkill(int skillId) {
        return skillId > CHANGE_SKILL && skillId <= 80004236;
    }

    /**
     * Preset 3 for the transformation: the bindings of {@code source} without the character's own skills and macros
     * (80004200 is kept), and the owned Lucid skills of {@link #LUCID_KEY_ORDER} on the freed keys (ascending), then
     * on free {@link #FALLBACK_KEYS}. Result: index -> {type, value}.
     */
    static java.util.TreeMap<Integer, int[]> lucidLayout(List<net.swordie.ms.client.character.keys.Keymapping> source,
                                                         java.util.function.IntPredicate owned) {
        java.util.TreeMap<Integer, int[]> layout = new java.util.TreeMap<>();
        List<Integer> freed = new ArrayList<>();
        boolean change = false;
        if (source != null) {
            for (net.swordie.ms.client.character.keys.Keymapping km : source) {
                int idx = km.getIndex(), type = km.getType(), val = km.getVal();
                if (type == 0) {
                    continue;
                }
                if (type == MACRO_KEY_TYPE || type == SKILL_KEY_TYPE && val != CHANGE_SKILL) {
                    if (idx < KEY_COUNT) {
                        freed.add(idx);
                    }
                    continue;
                }
                change |= type == SKILL_KEY_TYPE;
                layout.put(idx, new int[]{type, val});
            }
        }
        java.util.Collections.sort(freed);
        java.util.ArrayDeque<Integer> keys = new java.util.ArrayDeque<>(freed);
        for (int k : FALLBACK_KEYS) {
            if (!freed.contains(k) && !layout.containsKey(k)) {
                keys.add(k);
            }
        }
        for (int skill : LUCID_KEY_ORDER) {
            if (owned.test(skill) && !keys.isEmpty()) {
                layout.put(keys.poll(), new int[]{SKILL_KEY_TYPE, skill});
            }
        }
        if (!change && owned.test(CHANGE_SKILL) && !keys.isEmpty()) {
            layout.put(keys.poll(), new int[]{SKILL_KEY_TYPE, CHANGE_SKILL});
        }
        return layout;
    }

    /**
     * Makes sure preset 3 holds a Lucid layout (a preset 3 that already binds a Lucid skill is the player's own and
     * is kept), writes it to the funckeymap/keymaps tables and re-sends LP0B16. Returns true when it was rebuilt.
     */
    private boolean prepareLucidPreset(Char chr, int sourcePreset) {
        java.util.Map<Integer, net.swordie.ms.client.character.keys.FuncKeyMap> maps = chr.getFuncKeyMaps();
        if (maps == null) {
            maps = new java.util.LinkedHashMap<>();
            chr.setFuncKeyMaps(maps);
        }
        net.swordie.ms.client.character.keys.FuncKeyMap event = maps.get(EVENT_KEY_PRESET);
        if (event != null && event.getKeymap() != null && event.getKeymap().stream()
                .anyMatch(km -> km.getType() == SKILL_KEY_TYPE && isLucidSkill(km.getVal()))) {
            return false;
        }
        net.swordie.ms.client.character.keys.Tms282KeyMapService.prepareForInit(chr); // presets 0..2 explicit
        net.swordie.ms.client.character.keys.FuncKeyMap source = maps.get(sourcePreset);
        java.util.TreeMap<Integer, int[]> layout = lucidLayout(source == null ? null : source.getKeymap(), id -> {
            net.swordie.ms.client.character.skills.Skill s = chr.getSkill(id);
            return s != null && s.getCurrentLevel() > 0;
        });
        if (event == null) {
            event = new net.swordie.ms.client.character.keys.FuncKeyMap();
            event.setCharID(chr.getId());
            event.setPreset(EVENT_KEY_PRESET);
            maps.put(EVENT_KEY_PRESET, event);
        } else if (event.getKeymap() == null) {
            event.setKeymap(new ArrayList<>());
        }
        for (net.swordie.ms.client.character.keys.Keymapping km : event.getKeymap()) {
            if (!layout.containsKey(km.getIndex())) { // keep the row (and its ID), clear the binding
                km.setType((byte) 0);
                km.setVal(0);
            }
        }
        for (java.util.Map.Entry<Integer, int[]> e : layout.entrySet()) {
            event.putKeyBinding(chr.getId(), e.getKey(), (byte) e.getValue()[0], e.getValue()[1]);
        }
        try {
            net.swordie.ms.client.character.keys.Tms282KeyMapService.save(new net.swordie.ms.client.character.keys
                    .Tms282KeyMapService.Applied(chr.getId(), event, false, event.getKeymap()));
        } catch (RuntimeException e) {
            System.out.println("[ChangeBurning] preset 3 not saved for character " + chr.getId() + ": " + e
                    + " (kept for this session)");
        }
        chr.getClient().write(net.swordie.ms.connection.packet.Tms282KeyPackets.funcKeyMappedInit(maps,
                chr.getQuickslotKeys()));
        System.out.printf("[ChangeBurning] character %d preset 3 = Lucid layout from preset %d (%d keys); LP0B16 sent%n",
                chr.getId(), sourcePreset, layout.size());
        return true;
    }

    /** Transformation start: preset 3 active (QR 100972 no=3), the previous preset kept in {@link #PREV_PRESET_KEY}. */
    private void useLucidKeys(Char chr) {
        int current = activePreset(chr);
        int previous = current == EVENT_KEY_PRESET
                ? parsePreset(chr.getQRValueByKey(QR_KEY_PRESET, PREV_PRESET_KEY), 0) : current;
        if (previous == EVENT_KEY_PRESET) {
            previous = 0;
        }
        prepareLucidPreset(chr, previous);
        if (current != EVENT_KEY_PRESET) {
            setPresetQr(chr, PREV_PRESET_KEY, previous);
            setPresetQr(chr, "no", EVENT_KEY_PRESET);
        }
    }

    /** Transformation end (or a login after it): back to the preset saved by {@link #useLucidKeys}. */
    private static boolean restoreKeys(Char chr) {
        if (chr.getQuestById(QR_KEY_PRESET) == null || activePreset(chr) != EVENT_KEY_PRESET) {
            return false;
        }
        String prev = chr.getQRValueByKey(QR_KEY_PRESET, PREV_PRESET_KEY);
        if (prev == null) {
            return false; // preset 3 was not chosen by the transformation
        }
        int p = parsePreset(prev, 0);
        setPresetQr(chr, "no", p == EVENT_KEY_PRESET ? 0 : p);
        return true;
    }

    /** Level -> exp to the next level (policy: 1000 + 50 lv^2; the official table is sent by the official server). */
    public static List<int[]> expTable() {
        List<int[]> rows = new ArrayList<>();
        for (int lv = 1; lv < MAX_LUCID_LEVEL; lv++) {
            rows.add(new int[]{lv, demandExp(lv)});
        }
        return rows;
    }

    /** Exp from {@code lv} to {@code lv + 1} (the {@link #expTable} policy; used by {@link LucidDream}). */
    public static int demandExp(int lv) {
        return 1000 + 50 * lv * lv;
    }

    /** First field entry of a session (Tms282FieldEntry): the transformation does not survive a relogin. */
    public void onLogin(Char chr) {
        if (!tms282()) {
            return;
        }
        try {
            if (restoreKeys(chr)) {
                System.out.printf("[ChangeBurning] character %d key preset restored after a transformed logout%n", chr.getId());
            }
        } catch (RuntimeException e) {
            System.out.println("[ChangeBurning] key preset restore failed for character " + chr.getId() + ": " + e);
        }
        if (!eventActive() || !isDesignated(chr)) {
            return;
        }
        try {
            setUp(chr);
        } catch (RuntimeException e) {
            System.out.println("[ChangeBurning] login setup failed for character " + chr.getId() + ": " + e);
        }
    }

    /** Every field entry after SetField (Field.addChar): manager 5 and the user component (both idempotent). */
    public void onFieldEnter(Char chr) {
        if (tms282()) {
            // lucidfly-0930: 異想之夢 has isFieldMovingLimit (the client refuses portals while flying, 5A8B920); a
            // server-side warp ends the flight so the new field starts on the ground with no NewFlying left.
            landLucidFlight(chr, "field entry");
            try {
                // dreamtrial-1005: leaving the trial fields ends the forced form / the trial session
                net.swordie.ms.world.tms282.contents.changeburning.trial.DreamTrialService.get().onFieldEnter(chr);
            } catch (RuntimeException | LinkageError e) {
                System.out.println("[ChangeBurning] dream trial field hook failed for character " + chr.getId() + ": " + e);
            }
        }
        if (!tms282() || !isDesignated(chr) || chr.getClient() == null) {
            return;
        }
        if (!eventActive()) {
            // lucidprog: the event ended during the session -> end the transformation, remove the contents (no-op
            // when the client never had them)
            try {
                if (isTransformed(chr)) {
                    toggle(chr);
                }
            } catch (RuntimeException e) {
                System.out.println("[ChangeBurning] end-of-event untransform failed for character " + chr.getId() + ": " + e);
            }
            chr.getClient().write(ChangeBurningPackets.userContentsRemove());
            chr.getClient().write(ChangeBurningPackets.contentsRemove());
            return;
        }
        chr.getClient().write(ChangeBurningPackets.contentsCreate());
        chr.getClient().write(ChangeBurningPackets.userContentsCreate());
    }

    /** bossmech1-0930: RPEventStat ints from the Lucid level (LucidAbility, 自訂 table); all 0 when that fails. */
    private static int[] abilityInts(Char chr) {
        try {
            return LucidAbility.rpEventStatInts(chr);
        } catch (RuntimeException e) {
            System.out.println("[ChangeBurning] Lucid ability failed for character " + chr.getId() + ": " + e);
            return null;
        }
    }

    /**
     * bossmech1-0930: re-sends RPEventStat with fresh ints while transformed (Lucid level-up; later 夢幻的裝備 changes).
     * The client reads the 19 ints only from the CTS (14FC274), so a level-up needs this to change the damage.
     */
    public void refreshAbility(Char chr) {
        if (!tms282() || chr == null || chr.getClient() == null || !isTransformed(chr)) {
            return;
        }
        Option o = new Option();
        o.nOption = 1;
        o.rOption = transformReason(chr) == FORCED_REASON ? FORCED_REASON : CHANGE_SKILL; // dreamtrial-1005: keep 80004228
        o.tOption = 0;
        o.rpStats = abilityInts(chr);
        chr.getTemporaryStatManager().sendStat(CharacterTemporaryStat.RPEventStat, o);
    }

    /** lucidfly-0930: ends the 異想之夢 flight (NewFlying r=80004201) if the character's 282 buff book holds it. */
    private static void landLucidFlight(Char chr, String why) {
        try {
            String log = net.swordie.ms.client.character.tms282.Tms282LucidFlight.land(chr, why);
            if (log != null) {
                System.out.println(log);
            }
        } catch (RuntimeException | LinkageError e) {
            System.out.println("[ChangeBurning] Lucid flight landing failed for character " + chr.getId() + ": " + e);
        }
    }

    public boolean isTransformed(Char chr) {
        return chr.getTemporaryStatManager().hasStat(CharacterTemporaryStat.RPEventStat);
    }

    // ---- dreamtrial-1005: forced transformation r = 80004228 (夢幻的試煉) ---------------------------------------------

    /**
     * RPEventStat reason of the forced transformation (research lucid-re-0930/ability §1, 【證實】 static): with r =
     * 80004228 the client uses ATK x1 (C8A870 multiplies by 100 only for 80004200) and compares the explicit LEVEL
     * with the mob level (7B7B99), i.e. only the Lucid's own ability counts. Not a skill (Skill.wz has no 80004228).
     */
    public static final int FORCED_REASON = LucidAbility.JOB_CODE;

    /** rOption of the current RPEventStat, 0 when not transformed. */
    public int transformReason(Char chr) {
        if (chr == null || !isTransformed(chr)) {
            return 0;
        }
        Option o = chr.getTemporaryStatManager().getOption(CharacterTemporaryStat.RPEventStat);
        return o == null ? 0 : o.rOption;
    }

    /** The RPEventStat option of a transformation with {@code reason} (the 19 ability ints of this Lucid). */
    Option transformOption(Char chr, int reason) {
        Option o = new Option();
        o.nOption = 1;
        o.rOption = reason;
        o.tOption = 0;
        o.rpStats = abilityInts(chr);
        return o;
    }

    /**
     * Forces r = 80004228 (dream trial entry). A normal 80004200 transformation is replaced. Returns false (nothing
     * sent) when the character is not the designated one, the event is over or the profile is not 282.
     */
    public boolean forceTransform(Char chr) {
        if (chr == null || chr.getClient() == null || !tms282() || !eventActive() || !isDesignated(chr)) {
            return false;
        }
        if (transformReason(chr) == FORCED_REASON) {
            refreshAbility(chr);
            return true;
        }
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (isTransformed(chr)) {
            landLucidFlight(chr, "forced transform");
            tsm.removeStat(CharacterTemporaryStat.RPEventStat);
        }
        try {
            grantLucidSkills(chr);
            useLucidKeys(chr);
        } catch (RuntimeException e) {
            System.out.println("[ChangeBurning] Lucid skills/keys failed for character " + chr.getId() + ": " + e);
        }
        tsm.sendStat(CharacterTemporaryStat.RPEventStat, transformOption(chr, FORCED_REASON));
        chr.getClient().write(ChangeBurningPackets.transformUi(1));
        System.out.printf("[ChangeBurning] character %d forced transformation (RPEventStat r=%d)%n", chr.getId(), FORCED_REASON);
        return true;
    }

    /** Ends a forced (80004228) transformation; a normal 80004200 one is left alone. Returns true when ended. */
    public boolean endForcedTransform(Char chr) {
        if (chr == null || chr.getClient() == null || transformReason(chr) != FORCED_REASON) {
            return false;
        }
        toggle(chr);
        return true;
    }

    /** CP04E1 from the content component; false when it is not content 5. */
    public boolean onRequest(Char chr, ChangeBurningPackets.Request request) {
        if (request.type() != ChangeBurningPackets.TYPE) {
            return false;
        }
        switch (request.action()) {
            case 1 -> {
                // Answer first (LP064C sub 1 act 0): the client picks the in/out animation from the CTS state it has
                // now, and only this answer lets its request state return to 0 (then it sends action 2). Without it the
                // CHANGE skill never sends CP04E1 again, so the transformation could not be ended with the skill.
                chr.getClient().write(ChangeBurningPackets.requestResult(0));
                if (transformReason(chr) == FORCED_REASON) { // dreamtrial-1005: the trial keeps the forced form
                    chr.chatMessage("夢幻的試煉進行中，無法解除露希妲變身。");
                } else {
                    toggle(chr);
                }
            }
            case 2 -> { } // request finished on the client (animation end / 1 s tick after LP064C act 0) or cancelled
            // lucidprog: 6 = sent by manager slot9 19BB100 after every CTS update carrying bit 978 (RPEventStat), a
            // notification of the state the server itself just set -> nothing to answer; 5 = key-config window button
            // 12000 (4B59CFA); the bindings themselves arrive as CP020F and are saved there -> no answer either
            // (research change-burning-lucid §3.6: no reader of a reply exists in 19BB150 / 19BB100).
            case 5 -> System.out.printf("[ChangeBurning] CP04E1 action 5 (event key config applied) from character %d%n",
                    chr.getId());
            case 6 -> { }
            default -> System.out.printf("[ChangeBurning] CP04E1 action %d from character %d (not handled)%n",
                    request.action(), chr.getId());
        }
        return true;
    }

    public void toggle(Char chr) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (isTransformed(chr)) {
            landLucidFlight(chr, "untransform");
            tsm.removeStat(CharacterTemporaryStat.RPEventStat);
            chr.getClient().write(ChangeBurningPackets.transformUi(0));
            try {
                restoreKeys(chr);
            } catch (RuntimeException e) {
                System.out.println("[ChangeBurning] key preset restore failed for character " + chr.getId() + ": " + e);
            }
            System.out.printf("[ChangeBurning] character %d back to normal%n", chr.getId());
            return;
        }
        if (!eventActive() || !isDesignated(chr)) {
            chr.chatMessage("只有指定的變身燃燒角色可以變身成露希妲（請找露希妲 NPC）。");
            return;
        }
        Option o = new Option();
        o.nOption = 1;
        o.rOption = CHANGE_SKILL;
        o.tOption = 0;
        o.rpStats = abilityInts(chr); // bossmech1-0930: the 19 RPEventStat ints (all 0 before = no damage)
        try {
            grantLucidSkills(chr);
            useLucidKeys(chr);
        } catch (RuntimeException e) {
            System.out.println("[ChangeBurning] Lucid skills/keys failed for character " + chr.getId() + ": " + e);
        }
        tsm.sendStat(CharacterTemporaryStat.RPEventStat, o);
        chr.getClient().write(ChangeBurningPackets.transformUi(1));
        System.out.printf("[ChangeBurning] character %d transformed (RPEventStat r=%d)%n", chr.getId(), CHANGE_SKILL);
    }
}
