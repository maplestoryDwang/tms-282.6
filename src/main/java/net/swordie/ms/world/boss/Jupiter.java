package net.swordie.ms.world.boss;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;

/** Verified 282 map routes. Native Luppiter combat and the official boss intro remain unsupported. */
public final class Jupiter {
    public static final int LOBBY = 410013709;
    public static final int NORMAL_WAIT = 410032100;
    public static final int HARD_WAIT = 410032200;
    public static final int NORMAL_INTRO = 410027000;
    public static final int HARD_INTRO = 410028000;
    public static final int NORMAL_FIGHT = 410027100;
    public static final int HARD_FIGHT = 410028100;
    public static final int STORY_INTRO = 410028400;
    public static final int STORY_FIGHT = 410028410;

    private Jupiter() { }

    private static boolean enabled() {
        return Protocol.active().version() == ProtocolVersion.TMS_282_3;
    }

    public static int fightForIntro(int id) {
        return switch (id) {
            case NORMAL_INTRO -> NORMAL_FIGHT;
            case HARD_INTRO -> HARD_FIGHT;
            case STORY_INTRO -> STORY_FIGHT;
            default -> 0;
        };
    }

    public static boolean isArena(int id) {
        return fightForIntro(id) != 0 || id == NORMAL_FIGHT || id == HARD_FIGHT
                || id == STORY_FIGHT || id == 410027200 || id == 410028200;
    }

    public static void openUI(Char chr) {
        if (enabled() && chr.getFieldID() == LOBBY) chr.getScriptManager().startBossUI("Jupiter");
    }

    public static void enter(Char chr) {
        if (!enabled()) return;
        int id = chr.getFieldID();
        if (id == LOBBY) openUI(chr);
        else if (id == NORMAL_WAIT || id == HARD_WAIT) chr.getScriptManager().startBoss();
    }

    public static void intro(Char chr) {
        if (!enabled()) return;
        int source = chr.getFieldID();
        int fight = fightForIntro(source);
        if (fight == 0 || (source != STORY_INTRO && chr.getInstance() == null)) return;
        // The extracted Direction47/geardrak videos show story companions, not a verified boss intro.
        // Do not play unrelated footage, wait on a fabricated duration, or treat scenery loops as intros.
        chr.chatMessage("尤比太原始進場演出尚未重建；目前僅開放場景，戰鬥機制未接通。");
        chr.warp(fight);
    }

    public static void combat(Char chr) {
        if (!enabled()) return;
        int id = chr.getFieldID();
        if (id != NORMAL_FIGHT && id != HARD_FIGHT && id != STORY_FIGHT) return;
        chr.chatMessage("尤比太原生戰鬥尚未支援；不會產生通關或獎勵，可由出口離開。");
    }

    public static void exit(Char chr) {
        if (!enabled()) return;
        int id = chr.getFieldID();
        if (!isArena(id) && id != NORMAL_WAIT && id != HARD_WAIT) return;
        if (chr.getInstance() != null) chr.getScriptManager().exitBoss();
        else chr.warp(LOBBY);
    }
}
