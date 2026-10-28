package net.swordie.ms.world.tms282.bossrun;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.social.Party.Party;
import net.swordie.ms.constants.BossConstants;
import net.swordie.ms.constants.BossHpConstants;
import net.swordie.ms.enums.BossPartyType;
import net.swordie.ms.scripts.ScriptManagerImpl;

/**
 * Entry dialogs for boss NPC scripts (Python calls these instead of {@code sm.startBossUI} / {@code sm.startBoss}):
 * <pre>
 * from net.swordie.ms.world.tms282.bossrun import BossRunScripts
 * BossRunScripts.startBossUI(sm, chr, "Zakum")   # instead of sm.startBossUI("Zakum")
 * BossRunScripts.startBoss(sm, chr)              # instead of sm.startBoss()
 * </pre>
 * The party leader chooses formal (counted, rewards) or practice (TMS273 practiceMode: no clear log, no count, no
 * rewards), sees the account's remaining clears, or claims stored rewards. Off the 282 profile, with the feature
 * off, or for a non-leader, it calls the original method directly.
 */
public final class BossRunScripts {
    private static final int FORMAL = 0;
    private static final int PRACTICE = 1;
    private static final int CLAIM = 2;

    private BossRunScripts() {
    }

    static boolean dialogFor(Char chr) {
        BossRunService s = BossRunService.get();
        if (!s.active()) return false;
        Party p = chr.getParty();
        return p == null || p.isLeader(chr);
    }

    public static void startBossUI(ScriptManagerImpl sm, Char chr, String bossName) {
        if (!dialogFor(chr)) {
            sm.startBossUI(bossName);
            return;
        }
        String party = partyCapText(chr, bossName);
        if (party != null) {
            sm.sendSayOkay(party);
            return;
        }
        Integer mode = ask(sm, chr, bossName);
        if (mode == null) return;
        sm.startBossUI(bossName);
    }

    public static void startBoss(ScriptManagerImpl sm, Char chr) {
        if (!dialogFor(chr)) {
            sm.startBoss();
            return;
        }
        BossRunService s = BossRunService.get();
        // Chosen already at the Boss UI step (instance stamped): do not ask twice.
        if (chr.getInstance() != null && chr.getInstance().hasProperty(BossRunService.PROP_PRACTICE)) {
            sm.startBoss();
            return;
        }
        BossPartyType bpt = BossHpConstants.getCurrentBossParty(chr);
        if (bpt != null && s.armedPractice(BossRunService.leaderIdOf(chr)) != null) {
            s.practiceOf(chr.getInstance(), BossRunService.leaderIdOf(chr));
            sm.startBoss();
            return;
        }
        Integer mode = ask(sm, chr, bpt == null ? null : bpt.getBossName());
        if (mode == null) return;
        if (mode == FORMAL && bpt != null) {
            String blocked = formalBlock(chr, bpt);
            if (blocked != null) {
                s.disarm(BossRunService.leaderIdOf(chr));
                sm.sendSayOkay(blocked + "\r\n可以改用#b練習模式#k挑戰（不計次數、無獎勵）。");
                return;
            }
        }
        s.practiceOf(chr.getInstance(), BossRunService.leaderIdOf(chr));
        sm.startBoss();
    }

    /** Menu; returns FORMAL / PRACTICE after arming, or null when the script should stop. */
    static Integer ask(ScriptManagerImpl sm, Char chr, String bossName) {
        BossRunService s = BossRunService.get();
        int account = BossRunService.accountIdOf(chr);
        StringBuilder text = new StringBuilder("#e<首領挑戰>#n\r\n");
        if (bossName != null) {
            String usage = s.usageText(account, bossName);
            if (!usage.isEmpty()) {
                text.append(s.config().limitMode() == BossRunConfig.LimitMode.ACCOUNT ? "本帳號剩餘通關次數：\r\n" : "通關紀錄（依角色次數限制）：\r\n");
                text.append("#b").append(usage).append("#k");
            }
        }
        int pending = s.pendingCount(chr.getId());
        text.append("\r\n#L0#正式挑戰（計入次數、可獲得獎勵）#l\r\n");
        text.append("#L1#練習模式（不計次數、不記錄、無獎勵）#l\r\n");
        text.append("#L2#領取暫存首領獎勵").append(pending > 0 ? "（" + pending + " 項）" : "").append("#l");
        int sel = sm.sendNext(text.toString());
        int leader = BossRunService.leaderIdOf(chr);
        switch (sel) {
            case FORMAL -> {
                s.arm(leader, false);
                return FORMAL;
            }
            case PRACTICE -> {
                s.arm(leader, true);
                chr.chatMessage(net.swordie.ms.enums.ChatType.SystemNotice, "已選擇練習模式：本次挑戰不計次數、不記錄、不發放獎勵。");
                return PRACTICE;
            }
            case CLAIM -> {
                sm.sendSayOkay(claim(chr));
                return null;
            }
            default -> {
                return null;
            }
        }
    }

    /** Account limit check for every party member (formal entry). null = allowed. */
    static String formalBlock(Char chr, BossPartyType bpt) {
        BossRunService s = BossRunService.get();
        if (s.config().limitMode() != BossRunConfig.LimitMode.ACCOUNT) return null;
        Party p = chr.getParty();
        Iterable<Char> members = p == null ? java.util.List.of(chr) : p.getOnlineChars();
        for (Char c : members) {
            BossRunLedger.Verdict v = s.ledger().verdict(BossRunService.accountIdOf(c), bpt.name(), s.now());
            if (v != BossRunLedger.Verdict.OK) {
                return "隊員 #e#r" + c.getName() + "#k#n 的帳號" + BossRunLedger.verdictText(v) + "（" + BossRunNames.of(bpt) + "）。";
            }
        }
        return null;
    }

    /** Limbo / Malitia: at most 3 players (the lowest cap of the boss's difficulties on this profile). */
    static String partyCapText(Char chr, String bossName) {
        Party p = chr.getParty();
        if (p == null) return null;
        int cap = Integer.MAX_VALUE;
        for (BossPartyType b : BossPartyType.values()) {
            if (b.getBossName().equalsIgnoreCase(bossName)) cap = Math.min(cap, partyCap(b));
        }
        if (p.getMembers().size() > cap) {
            return "此首領最多可由 " + cap + " 人組隊挑戰，請調整隊伍人數。";
        }
        return null;
    }

    public static int partyCap(BossPartyType b) {
        int cap = BossConstants.getBossPartyLimit(b);
        return switch (b) {
            case LIMBO_NORMAL, LIMBO_HARD, MALITIA_NORMAL, MALITIA_EXTREME -> Math.min(cap, 3);
            default -> cap;
        };
    }

    public static String claim(Char chr) {
        return BossRunService.get().claimPending(chr.getId(), new BossRunService.CharDelivery(chr));
    }

    static final int GOLLUX_HEAD_FIELD = 863010600;
    static final int GOLLUX_HEART_FIELD = 863010700;
    static final int[] GOLLUX_HEADS = {9390600, 9390601, 9390602};

    /** npc/giantBossPortal_npc.py (9330630 in 培羅德的頭): open once no head phase is alive. */
    public static void golluxHeadPortal(ScriptManagerImpl sm, Char chr) {
        boolean tms282;
        try {
            tms282 = net.swordie.ms.connection.protocol.Protocol.active().version()
                    == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3;
        } catch (RuntimeException e) {
            tms282 = false;
        }
        var field = chr.getField();
        var inst = chr.getInstance();
        if (!tms282 || field == null || field.getId() != GOLLUX_HEAD_FIELD || inst == null
                || !BossRunMaps.exists(GOLLUX_HEART_FIELD)) {
            sm.sendSayOkay("目前無法通過。");
            return;
        }
        if (!golluxHeadDefeated(field.isGollux_head(), field.getMobs().stream().map(m -> m.getTemplateId()).toList())) {
            sm.sendSayOkay("培羅德的頭部仍在活動中，現在無法通過。");
            return;
        }
        if (!sm.sendAskYesNo("要前往#b培羅德的心臟#k嗎？\r\n（同一張地圖內的隊員會一起移動。）")) return;
        for (Char c : new java.util.ArrayList<>(field.getChars())) {
            if (c.getInstance() == inst) c.warp(GOLLUX_HEART_FIELD);
        }
    }

    /** The head field was set up (Gollux.init) and no head phase mob is left. */
    public static boolean golluxHeadDefeated(boolean headInitialised, java.util.Collection<Integer> mobIds) {
        if (!headInitialised) return false;
        for (int id : mobIds) {
            for (int head : GOLLUX_HEADS) if (id == head) return false;
        }
        return true;
    }

    /** npc/aufheben_accept.py */
    public static void aufhebenAccept(ScriptManagerImpl sm, Char chr) {
        if (!AufhebenBoss.available()) {
            sm.sendSayOkay("奧芙赫班的戰場目前無法使用。");
            return;
        }
        int used = AufhebenBoss.entriesUsed(chr.getId());
        int sel = sm.sendNext("#e<首領：奧芙赫班>#n\r\n\r\n#b#h0##k，偉大的勇士，你準備好迎戰奧芙赫班了嗎？\r\n"
                + "（" + AufhebenBoss.MIN_PLAYERS + "~" + AufhebenBoss.MAX_PLAYERS + " 人，等級 " + AufhebenBoss.MIN_LEVEL + "~"
                + AufhebenBoss.MAX_LEVEL + "，每日 " + AufhebenBoss.dailyLimit() + " 次，今日已使用 " + used + " 次）\r\n"
                + "#L0#申請入場 <首領：奧芙赫班>#l\r\n#L2#領取暫存首領獎勵#l");
        if (sel == 2) {
            sm.sendSayOkay(claim(chr));
            return;
        }
        if (sel != 0) return;
        if (chr.getParty() == null) {
            if (sm.sendAskYesNo("那個地方很危險，你必須先組隊才能進去。要建立隊伍以入場嗎？\r\n#b（點擊同意時將建立隊伍。）")) {
                sm.createSoloPartyForBossIfNeeded();
            }
            if (chr.getParty() == null) {
                sm.sendSayOkay("這裡很危險，必須組隊進入。\r\n#b(1 人以上的隊伍即可申請。)#k");
                return;
            }
        }
        String reason = AufhebenBoss.enter(sm, chr);
        if (reason != null) sm.sendSayOkay(reason);
    }
}
