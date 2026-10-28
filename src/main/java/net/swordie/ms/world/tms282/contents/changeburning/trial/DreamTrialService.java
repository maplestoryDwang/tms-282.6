package net.swordie.ms.world.tms282.contents.changeburning.trial;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.protocol.ProtocolConfig;
import net.swordie.ms.world.tms282.contents.common.HotJsonFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

/**
 * 變身燃燒：露希妲 夢幻的試煉 (dreamtrial-1005, docs/dreamtrial-1005/夢幻的試煉.md): the designated 變身燃燒 character
 * fights a dream boss alone, with the forced transformation r = 80004228 (only the Lucid's own ability).
 * <ul>
 *     <li>entry ({@link #checkEntry} / {@link #enter}): feature flag, event period, designated, solo, Lucid level,
 *     every stage field exists, daily limit (counted on entry, tms282_bossrun_pqlog);</li>
 *     <li>a solo instance on the first stage, the instance clock ({@code Instance.setTimeout} = CLOCK LP030F, the
 *     proven boss timer) and the revive count; then the forced transformation;</li>
 *     <li>stages from {@link DreamTrialConfig}: mobs with configured HP (drops off), the optional boss HP tag
 *     (FIELD_EFFECT MobHPTag) and Lucid butterflies; all clear mobs dead -> next stage or clear;</li>
 *     <li>clear: the LucidBoss mission quest (first clear; the client then runs the season reward q1029xx), dust into
 *     the 夢幻的裝備 store, configured items; exit after {@code exitDelaySeconds};</li>
 *     <li>fail / exit: any field entry outside the trial's stage fields (timeout, revives used up, give up,
 *     relogin) ends the session and removes the forced transformation ({@link #onFieldEnter}).</li>
 * </ul>
 * The LucidBoss window (UIChangeBurning.img/LucidBoss) is not driven: its reward buttons (btnScript) send a packet
 * nobody has decoded, so the window only shows what the quest states show (mission complete ticks).
 * Off unless {@code tms282.lucid.dreamTrial=true} (system property or config/server.properties; GM {@code @dreamtrial on}).
 */
public final class DreamTrialService {
    public static final String PROPERTY = "tms282.lucid.dreamTrial";
    public static final Path CONFIG = Path.of("data/tms282-changeburning/dreamtrial.json");

    /** Side effects on the server (Command boundary; the checks use a recording implementation). */
    public interface Runtime {
        boolean tms282();

        boolean eventActive();

        boolean designated(Char chr);

        int lucidLevel(Char chr);

        /** null when the character may enter alone; else the reason (party with others, already in an instance). */
        String soloBlock(Char chr);

        boolean fieldExists(int fieldId);

        int used(Char chr, int logKey);

        void count(Char chr, int logKey);

        void reset(Char chr, int logKey);

        /** Solo instance on {@code fieldId}; returns the instance handle or null. */
        Object enter(Char chr, int fieldId, int exitField, int timeLimitSeconds, int revives);

        /** The character is still inside {@code instance}. */
        boolean inside(Char chr, Object instance);

        int fieldId(Char chr);

        void warp(Char chr, int fieldId);

        /** Leaves the instance to {@code fieldId} (also used outside an instance). */
        void leave(Char chr, int fieldId);

        boolean forceTransform(Char chr);

        boolean endForced(Char chr);

        boolean forced(Char chr);

        void schedule(Runnable r, long delayMs);

        /** Clears the stage field of the instance (mobs, drops off). */
        void prepare(Object instance, int fieldId);

        /** Spawns one mob with {@code hp}; {@code onDeath} gets the template id once when it dies. False = not spawned. */
        boolean spawn(Object instance, int fieldId, DreamTrialConfig.MobSpawn spawn, long hp, boolean hpTag, IntConsumer onDeath);

        /** Removes every mob of the stage field (GM clear). */
        void removeMobs(Object instance, int fieldId);

        void lucid(Object instance, int fieldId, int phase, int mode, boolean on);

        boolean questDone(Char chr, int questId);

        void completeQuest(Char chr, int questId);

        /** null = given; else why not (off / failed). */
        String addDust(Char chr, long dust);

        boolean canHold(Char chr, List<DreamTrialConfig.Item> items);

        void give(Char chr, List<DreamTrialConfig.Item> items);

        void message(Char chr, String text);

        /** Registers the Lucid / Vellum extra ids of the config (empty when off). */
        void registerMechanics(DreamTrialConfig config, boolean on);
    }

    /** One running trial of one character. */
    static final class Session {
        final int charId;
        volatile Char chr;
        final DreamTrialConfig.Trial trial;
        final long startMillis = System.currentTimeMillis();
        volatile Object instance;
        int stage = -1;
        final Set<Integer> remaining = new HashSet<>();
        boolean cleared;
        boolean ended;

        Session(Char chr, DreamTrialConfig.Trial trial) {
            this.charId = chr.getId();
            this.chr = chr;
            this.trial = trial;
        }
    }

    private static volatile DreamTrialService instance;
    private static volatile Boolean enabled;

    private final Supplier<DreamTrialConfig> config;
    private final Runtime rt;
    private final Map<Integer, Session> sessions = new ConcurrentHashMap<>();
    private final Map<Integer, Integer> origins = new ConcurrentHashMap<>();
    private volatile DreamTrialConfig registered;

    public DreamTrialService(Supplier<DreamTrialConfig> config, Runtime runtime) {
        this.config = Objects.requireNonNull(config);
        this.rt = Objects.requireNonNull(runtime);
    }

    private static final HotJsonFile<DreamTrialConfig> FILE = new HotJsonFile<>(CONFIG, DreamTrialConfig::parse,
            DreamTrialConfig::empty, 2000);

    public static DreamTrialService get() {
        DreamTrialService s = instance;
        if (s == null) {
            synchronized (DreamTrialService.class) {
                s = instance;
                if (s == null) {
                    s = new DreamTrialService(FILE::get, new DreamTrialServerRuntime());
                    instance = s;
                }
            }
        }
        return s;
    }

    public static void install(DreamTrialService service) {
        instance = service;
    }

    public static HotJsonFile<DreamTrialConfig> file() {
        return FILE;
    }

    public static boolean enabled() {
        Boolean e = enabled;
        if (e == null) {
            String sys = System.getProperty(PROPERTY);
            if (sys != null) {
                e = Boolean.parseBoolean(sys.trim());
            } else {
                Properties p = new Properties();
                if (Files.isRegularFile(ProtocolConfig.CONFIG_FILE)) {
                    try (InputStream in = Files.newInputStream(ProtocolConfig.CONFIG_FILE)) {
                        p.load(in);
                    } catch (Exception ignored) {
                        // unreadable config: off
                    }
                }
                e = Boolean.parseBoolean(p.getProperty(PROPERTY, "false").trim());
            }
            enabled = e;
        }
        return e;
    }

    public static void setEnabled(Boolean value) {
        enabled = value;
    }

    /** The current definitions (hot reloaded) and the Lucid / Vellum id registration that goes with them. */
    public DreamTrialConfig config() {
        DreamTrialConfig c = config.get();
        boolean on = enabled();
        if (on ? registered != c : registered != null) {
            try {
                rt.registerMechanics(c, on);
            } catch (RuntimeException | LinkageError e) {
                System.out.println("[DreamTrial] mechanics registration failed: " + e);
            }
            registered = on ? c : null;
        }
        return c;
    }

    // ---- entry ---------------------------------------------------------------------------------------------------

    public int used(Char chr, DreamTrialConfig.Trial t) {
        return rt.used(chr, t.logKey());
    }

    /** null = {@code chr} may enter {@code t}; otherwise the reason (Traditional Chinese). */
    public String checkEntry(Char chr, DreamTrialConfig.Trial t) {
        DreamTrialConfig c = config();
        if (!enabled() || !rt.tms282()) return "夢幻的試煉目前沒有開放。";
        if (t == null) return "找不到這個試煉。";
        if (!t.enabled()) return t.name() + "：" + t.closedText();
        if (!rt.eventActive()) return "變身燃燒活動已經結束了。";
        if (!rt.designated(chr)) return "只有這個帳號指定的變身燃燒角色可以挑戰夢幻的試煉。";
        if (sessions.containsKey(chr.getId())) return "你已經在試煉中了。";
        String solo = rt.soloBlock(chr);
        if (solo != null) return solo;
        int lv = rt.lucidLevel(chr);
        if (lv < c.minLucidLevel()) return "露希妲等級需要 Lv." + c.minLucidLevel() + " 以上（目前 Lv." + lv + "）。";
        for (int f : t.stageFields()) {
            if (!rt.fieldExists(f)) return t.name() + " 的戰場（" + f + "）目前無法使用。";
        }
        if (!rt.fieldExists(t.exitField())) return t.name() + " 的出口（" + t.exitField() + "）目前無法使用。";
        int limit = c.dailyLimitOf(t);
        if (used(chr, t) >= limit) return t.name() + " 今天的挑戰次數（" + limit + " 次）已經用完了。";
        return null;
    }

    /** Enters {@code key}; null on success, else the reason. */
    public String enter(Char chr, String key) {
        DreamTrialConfig c = config();
        DreamTrialConfig.Trial t = c.trial(key);
        String reason = checkEntry(chr, t);
        if (reason != null) return reason;
        Session s = new Session(chr, t);
        if (sessions.putIfAbsent(chr.getId(), s) != null) return "你已經在試煉中了。";
        int from = rt.fieldId(chr);
        if (c.trialOfLobby(from) == null && c.trialOfField(from) == null) origins.put(chr.getId(), from);
        DreamTrialConfig.Stage first = t.stages().getFirst();
        Object inst;
        try {
            inst = rt.enter(chr, first.field(), t.exitField(), c.timeLimitSeconds(), c.revives());
        } catch (RuntimeException e) {
            System.out.println("[DreamTrial] enter failed for character " + chr.getId() + ": " + e);
            inst = null;
        }
        if (inst == null) {
            sessions.remove(chr.getId(), s);
            return "發生未知錯誤，請稍後再試。";
        }
        s.instance = inst;
        rt.count(chr, t.logKey());
        if (!rt.forceTransform(chr)) {
            System.out.println("[DreamTrial] forced transformation refused for character " + chr.getId());
            sessions.remove(chr.getId(), s);
            rt.leave(chr, t.exitField());
            return "無法變身成露希妲，已離開試煉（次數已使用）。";
        }
        rt.message(chr, "[夢幻的試煉] " + t.name() + "：限時 " + c.timeLimitSeconds() / 60 + " 分鐘，可復活 " + c.revives()
                + " 次。今日 " + used(chr, t) + "/" + c.dailyLimitOf(t) + " 次。只會套用露希妲自身的能力值，不會掉落道具。");
        System.out.printf("[DreamTrial] character %d entered %s%n", chr.getId(), t.key());
        rt.schedule(() -> spawnStage(s, 0), c.spawnDelayMs());
        return null;
    }

    // ---- stages --------------------------------------------------------------------------------------------------

    void spawnStage(Session s, int idx) {
        DreamTrialConfig c = config();
        synchronized (s) {
            if (s.ended || s.cleared || s.stage >= idx) return;
            s.stage = idx;
            s.remaining.clear();
            s.remaining.addAll(s.trial.stages().get(idx).clearMobs());
        }
        DreamTrialConfig.Stage st = s.trial.stages().get(idx);
        rt.prepare(s.instance, st.field());
        int lv = lucidLevelOf(s);
        for (DreamTrialConfig.MobSpawn m : st.mobs()) {
            long hp = m.hp().hp(lv, c.hpMultiplier());
            boolean tag = c.hpTag() && st.clearMobs().contains(m.id());
            if (!rt.spawn(s.instance, st.field(), m, hp, tag, id -> onMobDead(s, idx, id))) {
                System.out.printf("[DreamTrial] %s stage %d: mob %d not spawned%n", s.trial.key(), idx, m.id());
                synchronized (s) {
                    s.remaining.remove(m.id());
                }
            }
        }
        if (st.lucidPhase() > 0) rt.lucid(s.instance, st.field(), st.lucidPhase(), st.lucidMode(), true);
        boolean empty;
        synchronized (s) {
            empty = s.remaining.isEmpty();
        }
        if (empty) { // nothing could spawn (data missing): never a free clear, the run is aborted
            System.out.printf("[DreamTrial] %s stage %d: no clear mob spawned, run aborted for character %d%n", s.trial.key(), idx, s.charId);
            Char chr = charOf(s);
            if (chr != null) {
                rt.message(chr, "[夢幻的試煉] 戰場的首領無法出現，試煉中止。請回報 GM。");
                rt.leave(chr, s.trial.exitField());
            }
        }
    }

    private int lucidLevelOf(Session s) {
        Char chr = s.chr;
        return chr == null ? 1 : rt.lucidLevel(chr);
    }

    private static Char charOf(Session s) {
        return s.chr;
    }

    void onMobDead(Session s, int idx, int templateId) {
        synchronized (s) {
            if (s.ended || s.cleared || s.stage != idx || !s.remaining.remove(templateId) || !s.remaining.isEmpty()) {
                return;
            }
        }
        stageCleared(s, idx);
    }

    private void stageCleared(Session s, int idx) {
        DreamTrialConfig c = config();
        DreamTrialConfig.Stage st = s.trial.stages().get(idx);
        if (st.lucidPhase() > 0) rt.lucid(s.instance, st.field(), st.lucidPhase(), st.lucidMode(), false);
        rt.removeMobs(s.instance, st.field()); // arms / summons left behind
        Char chr = charOf(s);
        if (idx + 1 < s.trial.stages().size()) {
            int next = s.trial.stages().get(idx + 1).field();
            if (chr != null) rt.message(chr, "[夢幻的試煉] 第 " + (idx + 1) + " 階段完成，即將進入下一階段。");
            rt.schedule(() -> {
                Char c2 = charOf(s);
                if (s.ended || c2 == null || !rt.inside(c2, s.instance)) return;
                rt.warp(c2, next);
                rt.schedule(() -> spawnStage(s, idx + 1), c.spawnDelayMs());
            }, c.stageDelayMs());
            return;
        }
        clear(s);
    }

    void clear(Session s) {
        DreamTrialConfig c = config();
        synchronized (s) {
            if (s.cleared || s.ended) return;
            s.cleared = true;
        }
        Char chr = charOf(s);
        DreamTrialConfig.Trial t = s.trial;
        long secs = (System.currentTimeMillis() - s.startMillis) / 1000;
        System.out.printf("[DreamTrial] character %d cleared %s in %d s%n", s.charId, t.key(), secs);
        if (chr == null) return;
        StringBuilder msg = new StringBuilder("[夢幻的試煉] 恭喜通關「" + t.name() + "」（" + secs / 60 + " 分 " + secs % 60 + " 秒）！");
        if (t.quest() > 0 && !rt.questDone(chr, t.quest())) {
            rt.completeQuest(chr, t.quest());
            msg.append(" 首領任務完成。");
        }
        if (t.reward().dust() > 0) {
            String why = rt.addDust(chr, t.reward().dust());
            msg.append(why == null ? " 夢幻粉末 +" + t.reward().dust() + "。" : " 夢幻粉末沒有發放（" + why + "）。");
        }
        if (!t.reward().items().isEmpty()) {
            if (rt.canHold(chr, t.reward().items())) {
                rt.give(chr, t.reward().items());
                msg.append(" 已獲得獎勵道具。");
            } else {
                msg.append(" 背包空間不足，獎勵道具沒有發放。");
                System.out.printf("[DreamTrial] character %d %s reward items not given (inventory full)%n", s.charId, t.key());
            }
        }
        rt.message(chr, msg.toString());
        rt.message(chr, "[夢幻的試煉] " + c.exitDelaySeconds() + " 秒後回到入口，也可以和大門／夢幻的光環對話離開。");
        rt.schedule(() -> {
            Char c2 = charOf(s);
            if (!s.ended && c2 != null && rt.inside(c2, s.instance)) rt.leave(c2, t.exitField());
        }, c.exitDelaySeconds() * 1000L);
    }

    // ---- exit ----------------------------------------------------------------------------------------------------

    /** Every field entry (ChangeBurning.onFieldEnter, 282): outside the session's stage fields the session ends. */
    public void onFieldEnter(Char chr) {
        if (chr == null) return;
        onFieldEnter(chr, rt.fieldId(chr));
    }

    void onFieldEnter(Char chr, int fieldId) {
        Session s = sessions.get(chr.getId());
        if (s != null) {
            s.chr = chr;
            if (s.instance == null) return; // the entry warp itself (enter() sets the instance after it)
            if (s.trial.hasStageField(fieldId) && rt.inside(chr, s.instance)) return;
            end(s, chr);
            return;
        }
        // no session (relogin, GM warp, server restart): a forced form never survives outside the trial fields
        if (rt.forced(chr) && config().trialOfField(fieldId) == null) {
            rt.endForced(chr);
        }
    }

    private void end(Session s, Char chr) {
        boolean cleared;
        synchronized (s) {
            if (s.ended) return;
            s.ended = true;
            cleared = s.cleared;
        }
        sessions.remove(s.charId, s);
        DreamTrialConfig.Stage st = s.stage >= 0 ? s.trial.stages().get(s.stage) : null;
        if (st != null && st.lucidPhase() > 0) rt.lucid(s.instance, st.field(), st.lucidPhase(), st.lucidMode(), false);
        rt.endForced(chr);
        if (!cleared) rt.message(chr, "[夢幻的試煉] 挑戰失敗（離開戰場、超過時間或復活次數用完）。");
        System.out.printf("[DreamTrial] character %d left %s (%s)%n", s.charId, s.trial.key(), cleared ? "cleared" : "failed");
    }

    /** Gives up / leaves the running trial (NPC 大門 / 夢幻的光環). False when not in a trial. */
    public boolean leave(Char chr) {
        Session s = sessions.get(chr.getId());
        if (s == null) return false;
        rt.leave(chr, s.trial.exitField());
        return true;
    }

    /** From a trial lobby back to where the character came from (or the configured town). */
    public void leaveLobby(Char chr) {
        Integer back = origins.remove(chr.getId());
        int to = back != null && rt.fieldExists(back) ? back : config().townField();
        rt.leave(chr, to);
    }

    public boolean inTrial(Char chr) {
        return chr != null && sessions.containsKey(chr.getId());
    }

    public boolean clearedNow(Char chr) {
        Session s = chr == null ? null : sessions.get(chr.getId());
        return s != null && s.cleared;
    }

    public DreamTrialConfig.Trial trialOf(Char chr) {
        Session s = chr == null ? null : sessions.get(chr.getId());
        return s == null ? null : s.trial;
    }

    /** GM: the current stage is cleared at once (its mobs are removed). */
    public String gmClearStage(Char chr) {
        Session s = sessions.get(chr.getId());
        if (s == null) return "不在試煉中。";
        int idx;
        synchronized (s) {
            if (s.ended || s.cleared || s.stage < 0) return "目前沒有可以結束的階段。";
            idx = s.stage;
            s.remaining.clear();
        }
        stageCleared(s, idx);
        return "已結束第 " + (idx + 1) + " 階段。";
    }

    /** GM: resets today's count of {@code key}. */
    public String gmReset(Char chr, String key) {
        DreamTrialConfig.Trial t = config().trial(key);
        if (t == null) return "找不到試煉 " + key;
        rt.reset(chr, t.logKey());
        return t.name() + " 的今日次數已重設。";
    }

    /** HP of every stage mob of {@code t} for a Lucid of level {@code lv} (GM listing, checks). */
    public static String hpTable(DreamTrialConfig c, DreamTrialConfig.Trial t, int lv) {
        StringBuilder sb = new StringBuilder(t.name()).append(" (Lucid Lv.").append(lv).append(")：");
        for (DreamTrialConfig.Stage st : t.stages()) {
            for (DreamTrialConfig.MobSpawn m : st.mobs()) {
                sb.append(m.id()).append('=').append(m.hp().hp(lv, c.hpMultiplier())).append(' ');
            }
        }
        return sb.toString().trim();
    }
}
