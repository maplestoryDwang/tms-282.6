package net.swordie.ms.world.boss;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;

/** Verified TMS282 season-boss entry routes; native Chrono combat/video remain unsupported. */
public final class Kai {
    public static final int LOBBY = 993277350;
    public static final int NORMAL_WAIT = 993277400;
    public static final int HARD_WAIT = 993278000;
    public static final int NORMAL_FIGHT = 993277600;
    public static final int HARD_FIGHT = 993278200;
    public static final String ORIGINAL_VIDEO = "Effect/Direction45.img/Chrono/2";

    private Kai() { }

    private static boolean enabled() {
        return Protocol.active().version() == ProtocolVersion.TMS_282_3;
    }

    public static void openUI(Char chr) {
        if (!enabled()) return;
        // BossParty.img/36: maxMemberCount=1 and onlySoloPlay=1 in both difficulties.
        if (chr.getParty() != null && chr.getParty().getMembers().size() > 1) {
            chr.chatMessage("凱伊僅能單人挑戰，請先離開多人隊伍。");
            return;
        }
        chr.getScriptManager().startBossUI("Kai");
    }

    public static void enter(Char chr) {
        if (!enabled()) return;
        int id = chr.getFieldID();
        if (id == LOBBY) {
            openUI(chr);
        } else if (id == NORMAL_WAIT || id == HARD_WAIT) {
            chr.getScriptManager().startBoss();
        }
    }

    public static void enterField(Char chr) {
        if (!enabled() || (chr.getFieldID() != NORMAL_FIGHT && chr.getFieldID() != HARD_FIGHT)) return;
        playOriginalIntro(chr);
        chr.chatMessage("凱伊原始影片播放仍待實機驗證，戰鬥機制尚未接通；可由大門退出。");
    }

    /** Experimental native-asset test: Screen packet is known, VIDEO_PROPERTY playback is not live verified. */
    public static void playOriginalIntro(Char chr) {
        if (!enabled() || chr.getInstance() == null
                || (chr.getFieldID() != NORMAL_FIGHT && chr.getFieldID() != HARD_FIGHT)) return;
        var field = chr.getField();
        var instance = chr.getInstance();
        synchronized (field) {
            if (chr.getField() != field || chr.getInstance() != instance
                    || field.hasProperty("KaiOriginalIntroRequested")) return;
            // The original MCV is 7200 ms. No guessed delay unlocks combat or fabricates completion.
            // Use its parent UOL with the established Screen transport, not PlayMovieClip's AVI API.
            chr.getScriptManager().screen(ORIGINAL_VIDEO);
            field.setProperty("KaiOriginalIntroRequested", true);
        }
    }

    public static void exit(Char chr) {
        if (!enabled()) return;
        int id = chr.getFieldID();
        if (id == LOBBY || id == NORMAL_WAIT || id == HARD_WAIT
                || id == NORMAL_FIGHT || id == HARD_FIGHT
                || id == 993277800 || id == 993278400) {
            // Releases the boss instance and uses its recorded return map, unlike a plain warp.
            chr.getScriptManager().exitBoss();
        }
    }
}
