package net.swordie.ms.client.character.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.jobs.Job;
import net.swordie.ms.client.jobs.legend.Evan;
import net.swordie.ms.client.jobs.adventurer.archer.Pathfinder;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.scripts.ScriptManagerImpl;
import net.swordie.ms.scripts.ScriptType;

/** 282 progression after the numeric level service. First-job choices stay in the newbie dialog;
 * only an unambiguous, level-eligible next stage may run, once. Never replay base level-up AP/SP/HP. */
public final class Tms282JobAdvance {
    private static final ThreadLocal<Boolean> SKIP_BASE = ThreadLocal.withInitial(() -> false);
    static final String BRANCH_SCRIPT = "expand/menu/更多功能/補轉職";
    private Tms282JobAdvance() { }
    public static boolean skipBase() { return SKIP_BASE.get(); }

    /** Read-only predicate for an overdue, unambiguous ordinary stage. First-job/Explorer choices stay separate. */
    public static boolean hasDueStage(Char chr) {
        if (chr == null || !enabled()) return false;
        Step step = nextStep(chr);
        return step != null && step.level() <= chr.getLevel();
    }

    /** Common login/channel-migration job initialization. Resume missed linear stages after the old intro hook;
     * an unfinished newbie flow still owns first-job choices/rewards. Use the existing save-gate lock order. */
    public static void initializeAfterMigrate(Char chr) {
        if (!enabled()) {
            chr.getJobHandler().handleInitAfterMigrate(chr);
            return;
        }
        EconomyLocks.packet(chr, () -> {
            chr.getJobHandler().handleInitAfterMigrate(chr);
            if (!net.swordie.ms.world.tms282.newbie.NewbieFlowService.get().replacesEntryScript(chr)) catchUp(chr);
            return null;
        });
    }

    public static void afterLevelUp(Char chr, int fromLevel, int toLevel) {
        if (chr == null || toLevel <= fromLevel || !enabled()) return;
        advance(chr, Math.min(toLevel, chr.getLevel()));
        promptBranchChoice(chr);
    }

    /** Explorer second jobs need the player's choice: open the panel's 轉職 dialog instead of guessing. The script
     * runs on its own thread (same lock order as the panel). Skipped while another dialog is open or the newbie flow
     * owns the character; the next level-up or the panel asks again. */
    public static boolean promptBranchChoice(Char chr) {
        if (chr == null || !enabled() || !hasBranchChoice(chr)
                || net.swordie.ms.world.tms282.newbie.NewbieFlowService.get().replacesEntryScript(chr)) return false;
        ScriptManagerImpl sm = chr.getScriptManager();
        if (sm == null) return false;
        synchronized (sm) {
            ScriptType last = sm.getLastActiveScriptType();
            if (last != null && last != ScriptType.None && sm.isActive(last)) return false;
            return sm.startScript(chr, 0, BRANCH_SCRIPT, ScriptType.Content);
        }
    }

    /** The branch table of {@link #chooseSecondJob}; Dual Blade (400 / sub job 1) and Cannoneer (501) are linear. */
    static boolean hasBranchChoice(Char chr) {
        if (chr.getLevel() < 30) return false;
        return switch (chr.getJob()) {
            case 100, 200, 300, 500 -> true;
            case 400 -> chr.getSubJob() != 1;
            default -> false;
        };
    }

    /** Also repairs missed second jobs and Dual Blade's 20/30/45 stages; branch choices are never guessed. */
    public static String catchUp(Char chr) {
        if (chr == null || !enabled()) return null;
        // Script threads can grant quest/medal rewards. Acquire the packet/save lock order before Char.
        String[] changed = {null};
        EconomyLocks.packet(chr, () -> {
            short before = chr.getJob();
            advanceLocked(chr, chr.getLevel());
            changed[0] = before == chr.getJob() ? null : before + " -> " + chr.getJob();
            return null;
        });
        return changed[0];
    }

    /** Apply a player's explicit second-job selection with the same one-time SP policy as automatic steps. */
    public static boolean chooseSecondJob(Char chr, int from, int to) {
        if (chr == null || !enabled()) return false;
        boolean[] advanced = {false};
        EconomyLocks.packet(chr, () -> {
            if (chr.getJob() != from || chr.getLevel() < 30) return null;
            boolean allowed = switch (from) {
                case 100 -> to == 110 || to == 120 || to == 130;
                case 200 -> to == 210 || to == 220 || to == 230;
                case 300 -> to == 310 || to == 320;
                case 400 -> chr.getSubJob() != 1 && (to == 410 || to == 420);
                case 500 -> to == 510 || to == 520;
                case 501 -> to == 530;
                default -> false;
            };
            if (!allowed) return null;
            applyMissingStep(chr, new Step(from, to, 30));
            advanced[0] = chr.getJob() == to;
            if (advanced[0]) net.swordie.ms.client.jobs.StarterWeapons.grantFor(chr);
            return null;
        });
        return advanced[0];
    }

    /** Compatibility for the pre-282-choice panel script only; do not introduce the new policy into GMS. */
    public static String catchUpLegacy(Char chr) {
        if (chr == null || enabled()) return null;
        synchronized (chr) {
            short before = chr.getJob();
            if (Tms282DemonJobAdvance.handles(before)) {
                Tms282DemonJobAdvance.advance(chr, chr.getLevel());
            } else {
                if (JobConstants.getJobLevel(chr.getJob()) == 2 && chr.getLevel() >= 60) legacyLevel(chr,60);
                if (JobConstants.getJobLevel(chr.getJob()) == 3 && chr.getLevel() >= 100) legacyLevel(chr,100);
            }
            return before == chr.getJob() ? null : before + " -> " + chr.getJob();
        }
    }

    private static void legacyLevel(Char chr, int level) {
        if (JobConstants.isZero(chr.getJob()) || chr.getJobHandler() == null) return;
        boolean previous = SKIP_BASE.get();
        SKIP_BASE.set(true);
        try { chr.getJobHandler().handleLevelUp((short) level); }
        catch (RuntimeException e) {
            System.out.println("[TMS282] legacy job advance failed character=" + chr.getId() + " level=" + level + ": " + e);
        }
        finally { SKIP_BASE.set(previous); }
    }

    private static boolean enabled() { return ScriptManagerImpl.GAME_SCRIPTS_ENABLED && Protocol.active().version() == ProtocolVersion.TMS_282_3; }

    record Step(short from, short to, int level) {
        Step(int from, int to, int level) { this((short) from, (short) to, level); }
    }

    static Step nextStep(Char chr) {
        short job = chr.getJob();
        if (JobConstants.isZero(job) || JobConstants.isSpecialEventJob(job)) return null;
        // First-job setup (including single-line beginners) is owned by NewbieFlow's durable receipt.
        if (JobConstants.isBeginnerJob(job)) return null;
        if (job == 400 && chr.getSubJob() == 1) return new Step(400,430,20);
        if (JobConstants.isDualBlade(job)) {
            return switch (job) {
                case 430 -> new Step(430,431,30);
                case 431 -> new Step(431,432,45);
                case 432 -> new Step(432,433,60);
                case 433 -> new Step(433,434,100);
                default -> null;
            };
        }
        // 2216/2218 belong to the old ten-stage model, not the current four-stage skill books.
        if (JobConstants.isEvan(job)) {
            return switch (job) {
                case 2210 -> new Step(2210,2212,30);
                case 2212 -> new Step(2212,2214,60);
                case 2214 -> new Step(2214,2217,100);
                default -> null;
            };
        }
        Short next = JobConstants.getNextJobForAdvancement(job, chr.getSubJob());
        if (next == null || JobConstants.JobEnum.getJobById(next) == null) return null;
        int level = switch (JobConstants.getJobLevel(job)) { case 1 -> 30; case 2 -> 60; case 3 -> 100; default -> 0; };
        return level == 0 ? null : new Step(job, next, level);
    }

    private static void advance(Char chr, int limit) {
        EconomyLocks.packet(chr, () -> { advanceLocked(chr,limit); return null; });
    }

    private static void advanceLocked(Char chr, int limit) {
        if (Tms282DemonJobAdvance.handles(chr.getJob())) {
            short before = chr.getJob();
            Tms282DemonJobAdvance.advance(chr, limit);
            if (chr.getJob() != before) net.swordie.ms.client.jobs.StarterWeapons.grantFor(chr);
            return;
        }
        for (int count = 0; count < 5; count++) { // longest ordinary sequence: Dual Blade 400 -> 434
            Step step = nextStep(chr);
            if (step == null || step.level() > limit) return;
            try {
                if (hasSafeLevelHandler(step)) {
                    Job handler = chr.getJobHandler();
                    if (handler == null) return;
                    boolean previous = SKIP_BASE.get();
                    SKIP_BASE.set(true);
                    try { handler.handleLevelUp((short) step.level()); }
                    finally { SKIP_BASE.set(previous); }
                } else {
                    applyMissingStep(chr, step);
                }
            } catch (RuntimeException e) {
                System.out.println("[TMS282] job advance failed character=" + chr.getId() + " " + step + ": " + e);
                return;
            }
            if (chr.getJob() != step.to()) return;
            net.swordie.ms.client.jobs.StarterWeapons.grantFor(chr);
            System.out.println("[TMS282] JOB ADVANCE character=" + chr.getId() + " level " + step.level()
                    + ": " + step.from() + " -> " + step.to());
        }
    }

    /** Existing handlers whose advancement part does not recompute historical level-up SP. */
    private static boolean hasSafeLevelHandler(Step s) {
        short job = s.from();
        if (JobConstants.isHayato(job) || JobConstants.isKain(job)) return true;
        if (JobConstants.isDualBlade(job)) return s.level() >= 45;
        return s.level() >= 60 && (job >= 100 && job < 600 || JobConstants.isCygnusKnight(job)
                || JobConstants.isAdele(job) || JobConstants.isPhantom(job) || JobConstants.isKaiser(job)
                || JobConstants.isRen(job) || JobConstants.isLynn(job));
    }

    private static void applyMissingStep(Char chr, Step step) {
        ScriptManagerImpl sm = chr.getScriptManager();
        sm.setJob(step.to());
        if (chr.getJob() != step.to()) return;
        // Do not call sm.jobAdvance: its late-advancement formula includes level-up SP already granted.
        int previousSp = JobConstants.isEvan(step.from()) ? (step.level() == 60 ? 1 : 2) : 5;
        sm.addSPJobAdv(step.from(), previousSp);
        sm.addSPJobAdv(step.to(), 3);
        for (int quest : quests(step.to())) if (!chr.hasQuestCompleted(quest)) sm.completeQuestNoRewards(quest);
        for (int item : rewards(step.to())) if (!chr.hasItem(item) && chr.canHold(item)) sm.giveItem(item);
        if (step.to() == 330 && chr.getSkillLevel(Pathfinder.ANCIENT_CURSE) < 2)
            chr.addSkill(Pathfinder.ANCIENT_CURSE, 2, 4);
        if (chr.getJobHandler() instanceof Evan evan) evan.spawnDragon();
    }

    /** Existing job-skip receipts, without executing UI dialogs or quest rewards. */
    private static int[] quests(int to) {
        return switch (to) {
            case 430 -> new int[]{2622,2623}; case 431 -> new int[]{2637,2638};
            case 530 -> new int[]{1427,1428}; case 330 -> new int[]{35939};
            case 1110 -> new int[]{20870,20871}; case 1210 -> new int[]{20870,20872};
            case 1310 -> new int[]{20870,20873}; case 1410 -> new int[]{20870,20874};
            case 1510 -> new int[]{20870,20875};
            case 2110 -> new int[]{21200,21201,21202}; case 2111 -> new int[]{21300,21301,21302,21303};
            case 2112 -> new int[]{21400,21401};
            case 2310 -> new int[]{24010,24011}; case 2311 -> new int[]{24012}; case 2312 -> new int[]{24013};
            case 2410 -> new int[]{25100,25101};
            case 2510 -> new int[]{38028,38029,38030}; case 2511 -> new int[]{38074,38075,38076};
            case 2512 -> new int[]{38072,38073};
            case 2710 -> new int[]{25510}; case 2711 -> new int[]{25511}; case 2712 -> new int[]{25512};
            case 3210 -> new int[]{23020,23023}; case 3211 -> new int[]{23030,23033};
            case 3212 -> new int[]{23040,23043,23046,23049,23052};
            case 3310 -> new int[]{23021,23024}; case 3311 -> new int[]{23031,23034};
            case 3312 -> new int[]{23041,23044,23047,23050,23053};
            case 3510 -> new int[]{23022,23025}; case 3511 -> new int[]{23032,23035};
            case 3512 -> new int[]{23042,23045,23048,23051,23054};
            case 3610 -> new int[]{23610,23611}; case 3611 -> new int[]{23612,23613,23614,23615};
            case 3612 -> new int[]{23616};
            case 3710 -> new int[]{23161,23162}; case 3711 -> new int[]{23163,23164};
            case 3712 -> new int[]{23165,23166,23167,23168};
            case 4211 -> new int[]{57459}; case 4212 -> new int[]{57460};
            case 5110 -> new int[]{20806,20807,20808,20809,20810}; case 5111 -> new int[]{20320,20321};
            case 5112 -> new int[]{20411,20412}; case 6110 -> new int[]{25710};
            case 6410 -> new int[]{34657}; case 6411 -> new int[]{34658}; case 6412 -> new int[]{34659};
            case 6510 -> new int[]{25825}; case 6511 -> new int[]{25826}; case 6512 -> new int[]{25827};
            case 14210 -> new int[]{22770}; case 14211 -> new int[]{22800}; case 14212 -> new int[]{22850};
            case 15110 -> new int[]{39651,39652};
            case 15210 -> new int[]{34817}; case 15211 -> new int[]{34831,34832,34833,34834};
            case 15212 -> new int[]{34842};
            case 15510 -> new int[]{34902}; case 15511 -> new int[]{34903}; case 15512 -> new int[]{34904};
            default -> new int[0];
        };
    }

    /** Missing milestone rewards go into free inventory space, never replacing equipped items. */
    private static int[] rewards(int to) {
        return switch (to) {
            case 1110,1210,1310,1410,1510 -> new int[]{1142067};
            case 2110 -> new int[]{1142130}; case 2111 -> new int[]{1142131}; case 2112 -> new int[]{1142132};
            case 2310 -> new int[]{1142337}; case 2311 -> new int[]{1142338}; case 2312 -> new int[]{1142339};
            case 2710 -> new int[]{1142480,2430874}; case 2711 -> new int[]{1142481}; case 2712 -> new int[]{1142482};
            case 3210,3310,3510,3710 -> new int[]{1142243};
            case 3211,3311,3511,3711 -> new int[]{1142244};
            case 3212,3312,3512,3712 -> new int[]{1142245};
            case 3610 -> new int[]{1142576}; case 3611 -> new int[]{1142577}; case 3612 -> new int[]{1142578};
            case 5110 -> new int[]{1302038,1142400,1098001}; case 5111 -> new int[]{1142401,1098002};
            case 5112 -> new int[]{1142402,1098003};
            case 6410 -> new int[]{1143054}; case 6411 -> new int[]{1143055}; case 6412 -> new int[]{1143056};
            case 6510 -> new int[]{1142496,1352602}; case 6511 -> new int[]{1142497,1352603};
            case 6512 -> new int[]{1142498,1352604};
            case 14210 -> new int[]{1142864}; case 14211 -> new int[]{1142865}; case 14212 -> new int[]{1142866};
            case 15210 -> new int[]{1143079}; case 15211 -> new int[]{1143080}; case 15212 -> new int[]{1143081};
            case 15510 -> new int[]{1143099}; case 15511 -> new int[]{1143100}; case 15512 -> new int[]{1143101};
            default -> new int[0];
        };
    }
}
