package net.swordie.ms.world.tms282.bossrun;

import net.swordie.ms.Server;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.social.Party.Party;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;
import net.swordie.ms.constants.BossConstants;
import net.swordie.ms.constants.BossHpConstants;
import net.swordie.ms.enums.BossPartyType;
import net.swordie.ms.enums.ChatType;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.util.DataPrinter;
import net.swordie.ms.world.field.Field;
import net.swordie.ms.world.field.Instance;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

/**
 * TMS 282.3 boss runs (TMS273 party_boss port): per-account clear limits, practice mode, clear log, clear rewards
 * (BOSS_REWARD_APPLY), clear broadcast with damage shares.
 * <p>
 * Hooks (all static, all 282-profile only and fail-safe: an exception never changes the legacy result):
 * <ul>
 *   <li>{@link #exhaustedOverride(Char, BossPartyType)} from {@code Char.hasBossPartyAttempt} (entry gate).</li>
 *   <li>{@link #onLegacyAttempt(Char, BossPartyType)} from {@code Char.addPartyboss} (called for every party member on
 *       every compatible boss kill by BossHelper / ScriptManagerImpl.addPartyBoss): records the member once per run.</li>
 *   <li>{@link #onBossCleared(Field, Mob, Party)}: optional explicit clear point for boss death handlers; the same run
 *       de-duplication applies, so calling both never counts twice.</li>
 *   <li>{@link #track(Mob)}: optional damage tracking from spawnBossMob; a 1 s sweeper over live instances covers
 *       bosses spawned elsewhere.</li>
 * </ul>
 * Counting: one run = one instance x boss key; a character is recorded at most once per run; an account at most
 * once per run (distinct run ids). Limit mode ACCOUNT: only this ledger gates entry (legacy partyboss rows are still
 * written for the client but ignored). Mode CHARACTER: legacy BossPartyType gating; the ledger only logs.
 */
public final class BossRunService {
    public static final String PROP_PRACTICE = "bossrun.practice";
    public static final String PROP_START = "bossrun.start";
    static final long FINALIZE_DELAY_MS = 1500;
    static final long RUN_KEEP_MS = 10 * 60 * 1000L;
    static final long ARM_TTL_MS = 15 * 60 * 1000L;

    /** Where items and messages go (a Char in the server; a fake in the checks). */
    public interface Delivery {
        boolean canHold(int itemId, int quantity);

        void give(int itemId, int quantity);

        void message(String text);
    }

    public interface Scheduler {
        void schedule(Runnable task, long delayMs);
    }

    public record MemberInfo(int charId, int accountId, String name, Delivery delivery) {
    }

    /** What identifies and describes one run. */
    public record RunSpec(String runKey, String bossKey, String displayName, int orderId, int difficulty, int fieldId,
                          int mobId, int leaderId, String leaderName, long startMillis, List<String> damageScopes,
                          BooleanSupplier practice, boolean accountLimited) {
    }

    record Armed(boolean practice, long expiresAt) {
    }

    static final class Run {
        final RunSpec spec;
        final Map<Integer, MemberInfo> members = new LinkedHashMap<>();
        boolean scheduled;
        boolean finalized;
        boolean practice;
        long runId;
        long clearMillis;
        LocalDateTime clearedAt;
        Map<Integer, Integer> shares = Map.of();

        Run(RunSpec spec) {
            this.spec = spec;
        }
    }

    private static volatile BossRunService instance;

    private final BossRunConfig config;
    private final BossRunStore store;
    private final BossRunLedger ledger;
    private final BossRewardTable rewards;
    private final Supplier<LocalDateTime> clock;
    private final Supplier<Long> millis;
    private final RandomGenerator random;
    private final Scheduler scheduler;
    private final Consumer<String> broadcaster;
    private final boolean requireTms282;
    private final BossRunDamage damage = new BossRunDamage();
    private final Map<Integer, Armed> armed = new ConcurrentHashMap<>();
    private final Map<String, Run> runs = new ConcurrentHashMap<>();

    public BossRunService(BossRunConfig config, BossRunStore store, BossRunLimits limits, BossRewardTable rewards,
                          Supplier<LocalDateTime> clock, Supplier<Long> millis, RandomGenerator random,
                          Scheduler scheduler, Consumer<String> broadcaster, boolean requireTms282) {
        this.config = config;
        this.store = store;
        this.ledger = new BossRunLedger(store, limits, config.weekResetDay());
        this.rewards = rewards;
        this.clock = clock;
        this.millis = millis;
        this.random = random;
        this.scheduler = scheduler;
        this.broadcaster = broadcaster;
        this.requireTms282 = requireTms282;
    }

    // ------------------------------------------------------------------ singleton

    public static BossRunService get() {
        BossRunService s = instance;
        if (s == null) {
            synchronized (BossRunService.class) {
                if (instance == null) instance = createDefault();
                s = instance;
            }
        }
        return s;
    }

    /** Checks install their own service (memory store, fixed clock). */
    public static void install(BossRunService service) {
        instance = service;
    }

    private static BossRunService createDefault() {
        boolean noDb = Boolean.getBoolean("tms282.tests.rejectRealDatabase");
        ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "tms282-bossrun");
            t.setDaemon(true);
            return t;
        });
        BossRewardTable table;
        try {
            table = BossRewardTable.load(BossRewardTable.defaultPath());
        } catch (RuntimeException e) {
            BossRunService.logError(e);
            table = new BossRewardTable(Map.of());
        }
        BossRunLimits limits;
        try {
            limits = BossRunLimits.load(BossRunLimits.defaultPath());
        } catch (RuntimeException e) {
            BossRunService.logError(e);
            limits = new BossRunLimits(new BossRunLimits.Limit(1, 4), Map.of(), 100936, 5);
        }
        BossRunService s = new BossRunService(BossRunConfig.load(), noDb ? new MemoryBossRunStore() : new JdbcBossRunStore(),
                limits, table, LocalDateTime::now, System::currentTimeMillis, RandomGenerator.getDefault(),
                (task, delay) -> timer.schedule(guard(task), delay, TimeUnit.MILLISECONDS),
                BossRunService::broadcastWorld, true);
        if (!noDb && s.active()) {
            timer.scheduleAtFixedRate(guard(s::sweep), 1000, 1000, TimeUnit.MILLISECONDS);
        }
        return s;
    }

    static void logError(Throwable t) {
        DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, t instanceof Exception ex ? ex : new RuntimeException(t));
    }

    private static Runnable guard(Runnable r) {
        return () -> {
            try {
                r.run();
            } catch (RuntimeException | LinkageError e) {
                BossRunService.logError(e);
            }
        };
    }

    // ------------------------------------------------------------------ accessors

    public BossRunConfig config() {
        return config;
    }

    public BossRunStore store() {
        return store;
    }

    public BossRunLedger ledger() {
        return ledger;
    }

    public BossRewardTable rewards() {
        return rewards;
    }

    public BossRunDamage damage() {
        return damage;
    }

    public LocalDateTime now() {
        return clock.get();
    }

    public boolean active() {
        if (!config.enabled()) return false;
        if (!requireTms282) return true;
        try {
            return Protocol.active().version() == ProtocolVersion.TMS_282_3;
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    private static BossRunService activeOrNull() {
        try {
            BossRunService s = get();
            return s.active() ? s : null;
        } catch (RuntimeException | LinkageError e) {
            BossRunService.logError(e);
            return null;
        }
    }

    // ------------------------------------------------------------------ practice arming

    /** The entry dialog's choice for the party led by {@code leaderId}: practice or formal, valid 15 minutes. */
    public void arm(int leaderId, boolean practice) {
        armed.put(leaderId, new Armed(practice, millis.get() + ARM_TTL_MS));
    }

    public void disarm(int leaderId) {
        armed.remove(leaderId);
    }

    /** null when nothing (or an expired choice) is armed. */
    public Boolean armedPractice(int leaderId) {
        Armed a = armed.get(leaderId);
        if (a == null) return null;
        if (a.expiresAt() < millis.get()) {
            armed.remove(leaderId, a);
            return null;
        }
        return a.practice();
    }

    public boolean isArmedPractice(int leaderId) {
        return Boolean.TRUE.equals(armedPractice(leaderId));
    }

    /** Instance mode, stamped on first read so it cannot change during the fight. */
    public boolean practiceOf(Instance inst, int leaderId) {
        if (inst != null) {
            synchronized (inst) {
                Object v = inst.getProperty(PROP_PRACTICE);
                if (v instanceof Boolean b) return b;
                boolean p = isArmedPractice(leaderId);
                inst.setProperty(PROP_PRACTICE, p);
                return p;
            }
        }
        return isArmedPractice(leaderId);
    }

    // ------------------------------------------------------------------ core (testable without Char)

    /** Records one member for a run; the first member schedules the run's settlement. */
    public void noteClear(RunSpec spec, MemberInfo member) {
        Run run = runs.computeIfAbsent(spec.runKey(), k -> new Run(spec));
        boolean late;
        synchronized (run) {
            if (run.members.containsKey(member.charId())) return;
            run.members.put(member.charId(), member);
            late = run.finalized;
            if (!late && !run.scheduled) {
                run.scheduled = true;
                scheduler.schedule(() -> finalizeRun(run), FINALIZE_DELAY_MS);
            }
        }
        if (late) {
            settleMember(run, member);
        }
    }

    void finalizeRun(Run run) {
        List<MemberInfo> members;
        synchronized (run) {
            if (run.finalized) return;
            run.finalized = true;
            members = new ArrayList<>(run.members.values());
        }
        RunSpec spec = run.spec;
        run.practice = safePractice(spec);
        run.clearedAt = clock.get();
        run.clearMillis = Math.max(0, millis.get() - spec.startMillis());
        run.shares = BossRunDamage.sharesPermille(damage.totals(spec.damageScopes()),
                members.stream().map(MemberInfo::charId).toList());
        if (!run.practice) {
            run.runId = store.insertRun(new BossRunStore.RunRow(0, spec.bossKey(), spec.orderId(), spec.difficulty(),
                    spec.fieldId(), spec.mobId(), spec.leaderId(), spec.leaderName(), members.size(), run.clearMillis,
                    false, run.clearedAt, membersText(run, members)));
        }
        for (MemberInfo m : members) settleMember(run, m);
        if (config.broadcastEnabled()) {
            for (String line : broadcastLines(run, members)) {
                try {
                    broadcaster.accept(line);
                } catch (RuntimeException e) {
                    BossRunService.logError(e);
                }
            }
        }
        scheduler.schedule(() -> {
            runs.remove(spec.runKey(), run);
            damage.forget(spec.damageScopes());
        }, RUN_KEEP_MS);
    }

    private static boolean safePractice(RunSpec spec) {
        try {
            return spec.practice() != null && spec.practice().getAsBoolean();
        } catch (RuntimeException e) {
            return false;
        }
    }

    private void settleMember(Run run, MemberInfo m) {
        RunSpec spec = run.spec;
        if (run.practice) {
            m.delivery().message("練習模式：本次通關不記錄、不計入挑戰次數、不發放獎勵。");
            return;
        }
        boolean counted = true;
        String note = "";
        if (spec.accountLimited() && config.limitMode() == BossRunConfig.LimitMode.ACCOUNT) {
            BossRunLedger.Usage u = ledger.usage(m.accountId(), spec.bossKey(), run.clearedAt, run.runId);
            if (u.verdict() != BossRunLedger.Verdict.OK) {
                counted = false;
                note = "capped " + u.verdict().name().toLowerCase();
            }
        }
        int permille = run.shares.getOrDefault(m.charId(), 0);
        store.insertClear(new BossRunStore.ClearRow(run.runId, m.accountId(), m.charId(), m.name(), spec.bossKey(),
                run.clearedAt, run.clearMillis, permille, counted, note));
        if (!counted) {
            m.delivery().message("本帳號「" + spec.displayName() + "」" + BossRunLedger.verdictText(
                    ledger.usage(m.accountId(), spec.bossKey(), run.clearedAt, run.runId).verdict())
                    + "，本次通關只記錄、不計次數、不發放獎勵。");
            return;
        }
        if (config.rewardsEnabled()) giveRewards(spec, m);
    }

    void giveRewards(RunSpec spec, MemberInfo m) {
        List<BossRewardTable.Grant> grants = rewards.roll(spec.bossKey(), random);
        if (grants.isEmpty()) return;
        StringBuilder basic = new StringBuilder();
        StringBuilder adv = new StringBuilder();
        boolean stored = false;
        for (BossRewardTable.Grant g : grants) {
            boolean given = false;
            try {
                if (m.delivery().canHold(g.itemId(), g.quantity())) {
                    m.delivery().give(g.itemId(), g.quantity());
                    given = true;
                }
            } catch (RuntimeException e) {
                BossRunService.logError(e);
            }
            if (!given) {
                store.addPending(m.charId(), g.itemId(), g.quantity(), "bossrun:" + spec.bossKey());
                stored = true;
            }
            StringBuilder sb = g.advanced() ? adv : basic;
            if (sb.length() > 0) sb.append("、");
            sb.append(g.name()).append(" x").append(g.quantity());
        }
        StringBuilder text = new StringBuilder("擊破 ").append(spec.displayName()).append(" 的獎勵：");
        if (basic.length() > 0) text.append("基本獎勵 ").append(basic);
        if (adv.length() > 0) text.append(basic.length() > 0 ? "；" : "").append("進階獎勵 ").append(adv);
        m.delivery().message(text.toString());
        if (stored) {
            m.delivery().message("背包空間不足，部分首領獎勵已暫存。請在首領入口 NPC 選擇「領取暫存首領獎勵」，或輸入 @bossreward 領取。");
        }
        DataPrinter.send(DataPrinter.ITEM, String.format("首領獎勵：%s(%d) %s %s%s", m.name(), m.charId(), spec.bossKey(),
                grants, stored ? " (部分暫存)" : ""), true);
    }

    /** Delivers stored rewards that fit; each pending row is claimed at most once. */
    public String claimPending(int charId, Delivery d) {
        List<BossRunStore.PendingRow> rows = store.pending(charId);
        if (rows.isEmpty()) return "沒有暫存的首領獎勵。";
        int got = 0;
        boolean left = false;
        for (BossRunStore.PendingRow r : rows) {
            if (!d.canHold(r.itemId(), r.quantity())) {
                left = true;
                continue;
            }
            if (!store.deletePending(r.id())) continue;
            d.give(r.itemId(), r.quantity());
            got++;
        }
        if (got == 0) return "背包空間不足，請整理背包後再領取。";
        return "已領取暫存的首領獎勵 " + got + " 項。" + (left ? "\r\n仍有部分因背包空間不足而保留，請整理後再來。" : "");
    }

    public int pendingCount(int charId) {
        return store.pending(charId).size();
    }

    static String membersText(Run run, List<MemberInfo> members) {
        StringBuilder sb = new StringBuilder();
        for (MemberInfo m : sorted(run, members)) {
            if (sb.length() > 0) sb.append(',');
            sb.append(m.name()).append(':').append(pct(run.shares.getOrDefault(m.charId(), 0)));
        }
        return sb.toString();
    }

    private static List<MemberInfo> sorted(Run run, List<MemberInfo> members) {
        List<MemberInfo> list = new ArrayList<>(members);
        list.sort((a, b) -> Integer.compare(run.shares.getOrDefault(b.charId(), 0), run.shares.getOrDefault(a.charId(), 0)));
        return list;
    }

    static String pct(int permille) {
        return (permille / 10) + "." + (permille % 10) + "%";
    }

    public static String clock(long ms) {
        long s = Math.max(0, ms) / 1000;
        return String.format("%02d:%02d", s / 60, s % 60);
    }

    List<String> broadcastLines(Run run, List<MemberInfo> members) {
        RunSpec spec = run.spec;
        List<String> out = new ArrayList<>();
        out.add("[首領擊破] " + spec.leaderName() + " 的隊伍擊破了 " + spec.displayName()
                + (run.practice ? "（練習模式）" : "") + "！通關時間 " + clock(run.clearMillis));
        boolean any = run.shares.values().stream().anyMatch(v -> v > 0);
        StringBuilder sb = new StringBuilder("傷害比例：");
        if (!any) {
            sb.append("無傷害資料");
        } else {
            int i = 0;
            for (MemberInfo m : sorted(run, members)) {
                if (i++ > 0) sb.append("、");
                sb.append(m.name()).append(' ').append(pct(run.shares.getOrDefault(m.charId(), 0)));
            }
        }
        out.add(sb.toString());
        return out;
    }

    // ------------------------------------------------------------------ Char-side hooks

    /**
     * Entry gate for {@code Char.hasBossPartyAttempt}: null = keep the legacy (per-character) answer;
     * TRUE = no attempt left; FALSE = may enter (practice armed, or account has entries left).
     */
    public static Boolean exhaustedOverride(Char chr, BossPartyType bpt) {
        BossRunService s = activeOrNull();
        if (s == null || chr == null || bpt == null) return null;
        try {
            if (s.isArmedPractice(leaderIdOf(chr))) return Boolean.FALSE;
            if (s.config.limitMode() != BossRunConfig.LimitMode.ACCOUNT) return null;
            return s.ledger.verdict(accountIdOf(chr), bpt.name(), s.now()) != BossRunLedger.Verdict.OK;
        } catch (RuntimeException | LinkageError e) {
            BossRunService.logError(e);
            return null;
        }
    }

    /**
     * Clear hook for {@code Char.addPartyboss}: records the member for the run; false = practice run, so the legacy
     * per-character counter must not be written either.
     */
    public static boolean onLegacyAttempt(Char chr, BossPartyType bpt) {
        BossRunService s = activeOrNull();
        if (s == null || chr == null || bpt == null) return true;
        try {
            Instance inst = chr.getInstance();
            if (inst == null || !inInstance(inst, chr)) return true; // not in the fight: legacy only
            RunSpec spec = s.specFor(inst, chr.getField(), bpt, 0, chr);
            s.noteClear(spec, memberOf(chr));
            return !safePractice(spec);
        } catch (RuntimeException | LinkageError e) {
            BossRunService.logError(e);
            return true;
        }
    }

    /**
     * Explicit "boss cleared" point for boss death handlers (BossHelper.completeCompatibleBoss etc.). Records every
     * character of {@code party} standing in the field (all characters in the field when party is null).
     */
    public static void onBossCleared(Field field, Mob bossMob, Party party) {
        BossRunService s = activeOrNull();
        if (s == null || field == null) return;
        try {
            List<Char> present = new ArrayList<>();
            for (Char c : new ArrayList<>(field.getChars())) {
                if (party == null || party.hasPartyMember(c.getId())) present.add(c);
            }
            if (present.isEmpty()) return;
            Char any = present.getFirst();
            BossPartyType bpt = BossHpConstants.getCurrentBossParty(any);
            if (bpt == null) return;
            Instance inst = any.getInstance();
            if (bossMob != null) s.snapshot(scopeOf(inst, field), bossMob);
            RunSpec spec = s.specFor(inst, field, bpt, bossMob == null ? 0 : bossMob.getTemplateId(), any);
            for (Char c : present) s.noteClear(spec, memberOf(c));
        } catch (RuntimeException | LinkageError e) {
            BossRunService.logError(e);
        }
    }

    /** Damage snapshots every 500 ms while the mob lives (call from spawnBossMob next to UrusRewardService.track). */
    public static void track(Mob mob) {
        BossRunService s = activeOrNull();
        if (s == null || mob == null) return;
        Field field = mob.getField();
        String scope = "f" + System.identityHashCode(field);
        s.scheduleTrack(scope, mob);
    }

    private void scheduleTrack(String scope, Mob mob) {
        scheduler.schedule(() -> {
            snapshot(scope, mob);
            if (mob.getHp() > 0 && mob.getField() != null && mob.getField().getMobs().contains(mob)) scheduleTrack(scope, mob);
        }, 500);
    }

    void snapshot(String scope, Mob mob) {
        try {
            Map<Integer, Long> m = new HashMap<>();
            for (Map.Entry<Char, Long> e : new HashMap<>(mob.getDamageDone()).entrySet()) {
                if (e.getKey() != null && e.getValue() != null) m.merge(e.getKey().getId(), e.getValue(), Long::sum);
            }
            damage.record(scope, mob.getObjectId(), m);
        } catch (ConcurrentModificationException ignored) {
            // next reading
        }
    }

    /** 1 s: damage of boss mobs in live instances, fight start stamp, practice stamp. */
    void sweep() {
        List<Instance> list;
        try {
            list = new ArrayList<>(Server.get().getInstances());
        } catch (ConcurrentModificationException e) {
            return;
        }
        Set<String> live = new HashSet<>();
        for (Instance inst : list) {
            if (inst == null) continue;
            String scope = "i" + inst.getId();
            live.add(scope);
            boolean boss = false;
            for (Field f : new ArrayList<>(inst.getFields().values())) {
                for (Mob m : new ArrayList<>(f.getMobs())) {
                    if (m.isBoss() && m.getHp() > 0) {
                        boss = true;
                        snapshot(scope, m);
                    }
                }
            }
            if (boss) {
                synchronized (inst) {
                    if (!inst.hasProperty(PROP_START)) inst.setProperty(PROP_START, millis.get());
                }
                Party p = inst.getParty();
                practiceOf(inst, p != null ? p.getPartyLeaderID() : inst.getChr() != null ? inst.getChr().getId() : 0);
            }
        }
        damage.retainInstanceScopes(live);
    }

    RunSpec specFor(Instance inst, Field field, BossPartyType bpt, int mobId, Char who) {
        int leaderId = leaderIdOf(who);
        String leaderName = leaderNameOf(who);
        String key = (inst != null ? "i" + inst.getId() : "f" + System.identityHashCode(field)) + ":" + bpt.name();
        List<String> scopes = new ArrayList<>();
        if (inst != null) {
            scopes.add("i" + inst.getId());
            for (Field f : inst.getFields().values()) scopes.add("f" + System.identityHashCode(f));
        } else if (field != null) {
            scopes.add("f" + System.identityHashCode(field));
        }
        long start = startOf(inst, bpt);
        return new RunSpec(key, bpt.name(), BossRunNames.of(bpt), bpt.getOrderId(), bpt.getDifficulty().getVal(),
                field == null ? 0 : field.getId(), mobId, leaderId, leaderName, start, scopes,
                () -> practiceOf(inst, leaderId), true);
    }

    private long startOf(Instance inst, BossPartyType bpt) {
        long now = millis.get();
        if (inst == null) return now;
        Object v = inst.getProperty(PROP_START);
        if (v instanceof Long l) return l;
        int remaining = inst.getRemainingTime();
        int total = BossConstants.getBossFightingTime(bpt);
        if (remaining > 0 && total > remaining) return now - (total - remaining) * 1000L;
        return now;
    }

    static String scopeOf(Instance inst, Field field) {
        return inst != null ? "i" + inst.getId() : "f" + System.identityHashCode(field);
    }

    static boolean inInstance(Instance inst, Char chr) {
        if (!inst.getChars().containsKey(chr.getId())) return false;
        Field f = chr.getField();
        return f != null && inst.getFields().get(f.getId()) == f;
    }

    public static int accountIdOf(Char chr) {
        return chr.getUser() != null ? chr.getUser().getId() : chr.getAccId();
    }

    public static int leaderIdOf(Char chr) {
        Party p = chr.getParty();
        return p != null ? p.getPartyLeaderID() : chr.getId();
    }

    static String leaderNameOf(Char chr) {
        Party p = chr.getParty();
        if (p != null && p.getPartyLeader() != null && p.getPartyLeader().getCharName() != null) {
            return p.getPartyLeader().getCharName();
        }
        return chr.getName();
    }

    public static MemberInfo memberOf(Char chr) {
        return new MemberInfo(chr.getId(), accountIdOf(chr), chr.getName(), new CharDelivery(chr));
    }

    /** Char as a {@link Delivery}; inventory writes under ECONOMY_MONITOR -> Char, the 282 lock order. */
    public record CharDelivery(Char chr) implements Delivery {
        @Override
        public boolean canHold(int itemId, int quantity) {
            return chr.getClient() != null && chr.canHold(itemId, quantity);
        }

        @Override
        public void give(int itemId, int quantity) {
            // 2026-09-29: Char before ECONOMY_MONITOR deadlocked with the Tms282Routes packet wrapper.
            synchronized (net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime.ECONOMY_MONITOR) {
                synchronized (chr) {
                    chr.addItemToInventory(itemId, quantity);
                }
            }
        }

        @Override
        public void message(String text) {
            if (chr.getClient() != null) chr.chatMessage(ChatType.SystemNotice, text);
        }
    }

    static void broadcastWorld(String line) {
        Collection<Char> all;
        try {
            all = new ArrayList<>(Server.get().getWorld().getChars());
        } catch (RuntimeException e) {
            return;
        }
        for (Char c : all) {
            if (c != null && c.getClient() != null) c.chatMessage(ChatType.SystemNotice, line);
        }
    }

    // ------------------------------------------------------------------ status text

    /** One line per difficulty of the boss (by English BossPartyType name): today / week left. */
    public String usageText(int accountId, String bossName) {
        StringBuilder sb = new StringBuilder();
        for (BossPartyType b : BossPartyType.values()) {
            if (!b.getBossName().equalsIgnoreCase(bossName)) continue;
            BossRunLedger.Usage u = ledger.usage(accountId, b.name(), now(), 0);
            sb.append(BossRunNames.of(b)).append("：").append(limitText(u)).append("\r\n");
        }
        return sb.toString();
    }

    public static String limitText(BossRunLedger.Usage u) {
        String day = u.limit().daily() <= 0 ? "今日不限" : "今日 " + u.dailyLeft() + "/" + u.limit().daily();
        String week = u.limit().weekly() <= 0 ? "本週不限" : "本週 " + u.weeklyLeft() + "/" + u.limit().weekly();
        return day + "、" + week;
    }

    static Duration ttl() {
        return Duration.ofMillis(ARM_TTL_MS);
    }
}
