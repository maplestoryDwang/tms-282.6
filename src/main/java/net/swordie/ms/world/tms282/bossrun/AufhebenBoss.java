package net.swordie.ms.world.tms282.bossrun;

import net.swordie.ms.Server;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.social.Party.Party;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.scripts.ScriptManagerImpl;
import net.swordie.ms.util.DataPrinter;
import net.swordie.ms.world.field.Field;
import net.swordie.ms.world.field.Instance;
import net.swordie.ms.world.field.Portal;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.TimeUnit;

/**
 * 奧芙赫班 (Aufheben) boss, ported from TMS273 {@code event/boss_aufheben.js} + {@code expand/aufheben_accept.js}:
 * party 1-6, Lv100-300, 5 entries per character per day (log key 100936), 20 revives, 30 minutes, five phases
 * 9601838..9601842 in 876000200/400/600/800/1000, exit 876000001.
 * <p>
 * The TMS273 direction fields 876000100/300/500/700/900 ({@code enter_Auf_Scene}) have no field script in this
 * server, so phases warp straight to the fight fields. Mobs are spawned here (the fields have no life) and a death
 * observer on each phase mob drives the next phase, so no BossHelper change is needed.
 */
public final class AufhebenBoss {
    public static final String BOSS_KEY = "AUFHEBEN";
    public static final String DISPLAY = "奧芙赫班";
    public static final int EXIT_FIELD = 876000001;
    public static final int[] PHASE_FIELDS = {876000200, 876000400, 876000600, 876000800, 876001000};
    public static final int[] PHASE_MOBS = {9601838, 9601839, 9601840, 9601841, 9601842};
    /** Spawn points: each fight field's "sp" portal (282 Map.wz). */
    static final int[][] FALLBACK_SPAWN = {{109, 277}, {-15, 299}, {-75, 292}, {-136, -189}, {-15, 299}};
    public static final int MIN_LEVEL = 100;
    public static final int MAX_LEVEL = 300;
    public static final int MIN_PLAYERS = 1;
    public static final int MAX_PLAYERS = 6;
    public static final int REVIVES = 20;
    public static final int TIME_SECONDS = 30 * 60;
    static final long SPAWN_DELAY_MS = 5000;
    static final long NEXT_PHASE_DELAY_MS = 6000;
    static final String PROP_PHASE = "aufheben.phase";
    static final String PROP_CLEARED = "aufheben.cleared";

    private AufhebenBoss() {
    }

    /** Fields and mobs must exist for the active profile. */
    public static boolean available() {
        return BossRunMaps.exists(EXIT_FIELD) && BossRunMaps.allExist(PHASE_FIELDS);
    }

    public static int phaseOf(int fieldId) {
        for (int i = 0; i < PHASE_FIELDS.length; i++) if (PHASE_FIELDS[i] == fieldId) return i;
        return -1;
    }

    static int dailyLimit() {
        return BossRunService.get().ledger().limits().aufhebenDaily();
    }

    static int logKey() {
        return BossRunService.get().ledger().limits().aufhebenLogKey();
    }

    public static int entriesUsed(int charId) {
        BossRunService s = BossRunService.get();
        return s.store().pqCount(charId, logKey(), s.now().toLocalDate());
    }

    /** null = the leader's party may enter; otherwise the Traditional Chinese reason. */
    public static String checkEntry(Char leader) {
        if (!available()) return "奧芙赫班的戰場目前無法使用。";
        Party party = leader.getParty();
        if (party == null) return "這裡很危險，必須組隊進入。\r\n#b(1 人以上的隊伍即可申請。)#k";
        if (!party.isLeader(leader)) return "請讓你的組隊長和我對話吧！";
        List<Char> online = new ArrayList<>(party.getOnlineChars());
        if (online.size() != party.getMembers().size()) return "所有隊員都必須在線。";
        for (Char c : online) {
            if (c.getField() != leader.getField()) return "好像有隊員在其他地方，請把隊員都召集到這裡！";
        }
        if (online.size() < MIN_PLAYERS || online.size() > MAX_PLAYERS) {
            return "你需要有一個 #r" + MIN_PLAYERS + "~" + MAX_PLAYERS + "#k 人的隊伍。";
        }
        for (Char c : online) {
            if (c.getLevel() < MIN_LEVEL || c.getLevel() > MAX_LEVEL) {
                return "隊員 #r#e" + c.getName() + "#n#k 的等級需要在 " + MIN_LEVEL + "~" + MAX_LEVEL + " 之間！";
            }
            if (entriesUsed(c.getId()) >= dailyLimit()) {
                return "隊員 #r#e" + c.getName() + "#n#k 今日的挑戰次數（" + dailyLimit() + " 次）已經用完了。";
            }
        }
        return null;
    }

    /** Creates the party instance, counts one entry per member, starts phase 1. */
    public static String enter(ScriptManagerImpl sm, Char leader) {
        String reason = checkEntry(leader);
        if (reason != null) return reason;
        BossRunService s = BossRunService.get();
        List<Char> members = new ArrayList<>(leader.getParty().getOnlineChars());
        sm.warpInstanceIn(leader, PHASE_FIELDS[0], true);
        Instance inst = leader.getInstance();
        if (inst == null) return "發生未知錯誤，請重試。";
        inst.setForcedReturn(EXIT_FIELD);
        inst.setForcedReturnPortalId(-1);
        inst.setTimeout(TIME_SECONDS, true);
        inst.setProperty(PROP_PHASE, 0);
        inst.setProperty(BossRunService.PROP_START, System.currentTimeMillis());
        inst.setProperty(BossRunService.PROP_PRACTICE, false);
        for (Char c : members) {
            s.store().pqAdd(c.getId(), logKey(), s.now().toLocalDate(), 1);
            c.setDeathCount(REVIVES);
            c.chatMessage(net.swordie.ms.enums.ChatType.SystemNotice, "奧芙赫班：限時 30 分鐘，可復活 " + REVIVES
                    + " 次。今日已使用 " + entriesUsed(c.getId()) + "/" + dailyLimit() + " 次。");
        }
        schedule(() -> spawnPhase(inst, 0), SPAWN_DELAY_MS);
        return null;
    }

    static void schedule(Runnable r, long delayMs) {
        Server.get().getEventTimer().addEvent(() -> {
            try {
                r.run();
            } catch (RuntimeException | LinkageError e) {
                BossRunService.logError(e);
            }
        }, delayMs, TimeUnit.MILLISECONDS);
    }

    static boolean alive(Instance inst) {
        return inst != null && Server.get().getInstances().contains(inst) && !inst.getChars().isEmpty();
    }

    static void spawnPhase(Instance inst, int phase) {
        if (!alive(inst)) return;
        Field field = inst.getField(PHASE_FIELDS[phase]);
        if (field == null) return;
        field.removeMobs();
        int x = FALLBACK_SPAWN[phase][0];
        int y = FALLBACK_SPAWN[phase][1];
        Portal sp = field.getPortalByName("sp");
        if (sp != null) {
            x = sp.getX();
            y = sp.getY();
        }
        Mob mob = field.spawnMob(PHASE_MOBS[phase], x, y, false);
        if (mob == null) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, new IllegalStateException("Aufheben mob " + PHASE_MOBS[phase] + " missing"));
            return;
        }
        BossRunService.track(mob);
        mob.addObserver(new Observer() {
            private boolean done;

            @Override
            public synchronized void update(Observable o, Object arg) {
                if (done) return;
                if (mob.getHp() > 0) return;
                done = true;
                o.deleteObserver(this);
                onPhaseCleared(inst, field, mob, phase);
            }
        });
    }

    static void onPhaseCleared(Instance inst, Field field, Mob mob, int phase) {
        synchronized (inst) {
            Object cleared = inst.getProperty(PROP_CLEARED);
            if (cleared instanceof Integer c && c >= phase) return;
            inst.setProperty(PROP_CLEARED, phase);
        }
        if (phase == PHASE_FIELDS.length - 1) {
            recordClear(inst, field, mob);
            for (Char c : new ArrayList<>(field.getChars())) {
                c.chatMessage(net.swordie.ms.enums.ChatType.SystemNotice, "奧芙赫班已被擊敗！與守門人對話即可離開。");
            }
            return;
        }
        for (Char c : new ArrayList<>(field.getChars())) {
            c.chatMessage(net.swordie.ms.enums.ChatType.SystemNotice, "第 " + (phase + 1) + " 階段完成，即將進入下一階段。");
        }
        schedule(() -> advance(inst, phase + 1), NEXT_PHASE_DELAY_MS);
    }

    static void advance(Instance inst, int next) {
        if (!alive(inst)) return;
        synchronized (inst) {
            Object cur = inst.getProperty(PROP_PHASE);
            if (cur instanceof Integer p && p >= next) return;
            inst.setProperty(PROP_PHASE, next);
        }
        for (Char c : new ArrayList<>(inst.getChars().values())) {
            if (c.getInstance() == inst && phaseOf(c.getFieldID()) >= 0) c.warp(PHASE_FIELDS[next]);
        }
        schedule(() -> spawnPhase(inst, next), SPAWN_DELAY_MS);
    }

    static void recordClear(Instance inst, Field field, Mob mob) {
        BossRunService s = BossRunService.get();
        if (!s.active()) return;
        List<Char> present = new ArrayList<>(field.getChars());
        if (present.isEmpty()) return;
        Char lead = present.getFirst();
        List<String> scopes = new ArrayList<>();
        scopes.add("i" + inst.getId());
        for (Field f : inst.getFields().values()) scopes.add("f" + System.identityHashCode(f));
        Object start = inst.getProperty(BossRunService.PROP_START);
        BossRunService.RunSpec spec = new BossRunService.RunSpec("i" + inst.getId() + ":" + BOSS_KEY, BOSS_KEY, DISPLAY,
                -1, 0, field.getId(), mob.getTemplateId(), BossRunService.leaderIdOf(lead), BossRunService.leaderNameOf(lead),
                start instanceof Long l ? l : System.currentTimeMillis(), scopes, () -> false, false);
        for (Char c : present) s.noteClear(spec, BossRunService.memberOf(c));
    }

    /** Gate NPC 9401553 in the fight fields: next phase / exit once the current phase is cleared. */
    public static void onGateNpc(ScriptManagerImpl sm, Char chr) {
        Instance inst = chr.getInstance();
        int phase = phaseOf(chr.getFieldID());
        if (inst == null || phase < 0) {
            sm.sendSayOkay("目前無法通過。");
            return;
        }
        Object cleared = inst.getProperty(PROP_CLEARED);
        boolean done = cleared instanceof Integer c && c >= phase;
        if (!done) {
            if (sm.sendAskYesNo("奧芙赫班還沒有被擊敗。\r\n要放棄挑戰並離開嗎？")) {
                chr.setDeathCount(-1);
                sm.warpInstanceOut(chr, EXIT_FIELD);
            }
            return;
        }
        if (phase == PHASE_FIELDS.length - 1) {
            if (sm.sendAskYesNo("恭喜擊敗奧芙赫班！要離開這裡嗎？")) {
                chr.setDeathCount(-1);
                sm.warpInstanceOut(chr, EXIT_FIELD);
            }
            return;
        }
        advance(inst, phase + 1);
    }
}
