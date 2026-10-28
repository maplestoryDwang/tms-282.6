package net.swordie.ms.world.tms282.contents.changeburning.trial;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.scripts.ScriptManagerImpl;

import java.util.List;

/**
 * NPC dialogs of 夢幻的試煉 (Python calls these):
 * <pre>
 * from net.swordie.ms.world.tms282.contents.changeburning.trial import DreamTrialScripts
 * DreamTrialScripts.menu(sm, chr)        # npc_9063907 露希妲「夢幻的試煉」, NPC 9063911 夢幻的光環
 * DreamTrialScripts.gate(sm, chr, "vellum") # NPC 9063912 第四個封印之門
 * DreamTrialScripts.door(sm, chr)        # NPC 9063913 大門 (inside the fight fields)
 * DreamTrialScripts.lobbyExit(chr)       # lobby exit portals (portal scripts: chr binding only)
 * </pre>
 * Every function returns false when the feature is off, so the scripts keep their old text then.
 */
public final class DreamTrialScripts {
    private DreamTrialScripts() {
    }

    private static DreamTrialService svc() {
        return DreamTrialService.get();
    }

    /** Inside a running trial: leave (cleared) or give up. True when handled. */
    static boolean runningMenu(ScriptManagerImpl sm, Char chr) {
        DreamTrialService s = svc();
        if (!s.inTrial(chr)) return false;
        DreamTrialConfig.Trial t = s.trialOf(chr);
        if (s.clearedNow(chr)) {
            if (sm.sendAskYesNo("恭喜通關「" + t.name() + "」！\r\n要回到入口嗎？")) s.leave(chr);
        } else if (sm.sendAskYesNo("「" + t.name() + "」還沒有結束。\r\n#r要放棄挑戰並離開嗎？（已使用的次數不會返還）#k")) {
            s.leave(chr);
        }
        return true;
    }

    /** The trial list: status, today's count, entry. */
    public static boolean menu(ScriptManagerImpl sm, Char chr) {
        if (!DreamTrialService.enabled()) return false;
        if (runningMenu(sm, chr)) return true;
        DreamTrialService s = svc();
        DreamTrialConfig c = s.config();
        List<DreamTrialConfig.Trial> trials = c.trials();
        StringBuilder text = new StringBuilder("#e<夢幻的試煉>#n\r\n用#b露希妲#k單人挑戰夢中的首領。\r\n"
                + "試煉中會強制變身，只套用露希妲自身的能力值；不計入每週首領次數，不會掉落道具。\r\n"
                + "限時 " + c.timeLimitSeconds() / 60 + " 分鐘，可復活 " + c.revives() + " 次。\r\n");
        for (int i = 0; i < trials.size(); i++) {
            DreamTrialConfig.Trial t = trials.get(i);
            text.append("\r\n#L").append(i).append('#').append(t.name());
            if (!t.enabled()) {
                text.append("（").append(t.closedText()).append("）");
            } else {
                boolean done = t.quest() > 0 && chr.hasQuestCompleted(t.quest());
                text.append("（今日 ").append(s.used(chr, t)).append('/').append(c.dailyLimitOf(t)).append(done ? "，已完成" : "").append("）");
            }
            text.append("#l");
        }
        if (c.trialOfLobby(chr.getField() == null ? 0 : chr.getField().getId()) != null) {
            text.append("\r\n#L99#離開這裡#l");
        }
        int sel = sm.sendNext(text.toString());
        if (sel == 99) {
            s.leaveLobby(chr);
            return true;
        }
        if (sel < 0 || sel >= trials.size()) return true;
        DreamTrialConfig.Trial t = trials.get(sel);
        String reason = s.checkEntry(chr, t);
        if (reason != null) {
            sm.sendSayOkay(reason);
            return true;
        }
        if (!sm.sendAskYesNo("要進入「" + t.name() + "」嗎？\r\n#r進入時會使用 1 次今日挑戰次數。#k")) return true;
        reason = s.enter(chr, t.key());
        if (reason != null) sm.sendSayOkay(reason);
        return true;
    }

    /** A gate NPC of one trial's lobby (第四個封印之門 -> vellum). */
    public static boolean gate(ScriptManagerImpl sm, Char chr, String key) {
        if (!DreamTrialService.enabled()) return false;
        if (runningMenu(sm, chr)) return true;
        DreamTrialService s = svc();
        DreamTrialConfig.Trial t = s.config().trial(key);
        String reason = s.checkEntry(chr, t);
        if (reason != null) {
            sm.sendSayOkay(reason);
            return true;
        }
        if (sm.sendAskYesNo("封印之門的另一邊是「" + t.name() + "」。\r\n要進入嗎？#r（使用 1 次今日挑戰次數）#k")) {
            reason = s.enter(chr, t.key());
            if (reason != null) sm.sendSayOkay(reason);
        }
        return true;
    }

    /** 大門 inside the fight fields. */
    public static boolean door(ScriptManagerImpl sm, Char chr) {
        if (!DreamTrialService.enabled()) return false;
        if (runningMenu(sm, chr)) return true;
        sm.sendSayOkay("（目前無法通過。）");
        return true;
    }

    /** Lobby exit portals: back to where the character came from. */
    public static boolean lobbyExit(Char chr) {
        if (!DreamTrialService.enabled()) return false;
        svc().leaveLobby(chr);
        return true;
    }
}
