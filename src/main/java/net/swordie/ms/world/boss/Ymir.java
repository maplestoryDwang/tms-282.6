package net.swordie.ms.world.boss;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;

/** TMS282 Ymir guards. Native patternSys 1091 and the cinematic are not implemented. */
public final class Ymir {
    public static final int LOBBY = 410007670;
    private Ymir() { }

    private static boolean enabled() {
        return Protocol.active().version() == ProtocolVersion.TMS_282_3;
    }

    public static boolean isChallengeField(int id) {
        return (id >= 410025000 && id <= 410025300 && id % 100 == 0)
                || (id >= 410026000 && id <= 410026300 && id % 100 == 0);
    }

    public static void enter(Char chr) {
        if (!enabled() || chr.getFieldID() != LOBBY) return;
        chr.getScriptManager().sendSayOkay("燦爛的凶星的原版開場動畫與戰鬥機制尚未完成移植，目前無法開始挑戰。");
    }

    public static void enterField(Char chr) {
        if (!enabled() || !isChallengeField(chr.getFieldID())) return;
        // These intro maps are blank director stages, not automatic Spine movies.
        // Do not strand entrants, synthesize an intro timer, or charge a fake clear.
        chr.chatMessage("燦爛的凶星的原版開場動畫與戰鬥機制尚未完成移植，返回入口。");
        exit(chr);
    }

    public static void exit(Char chr) {
        if (!enabled() || !isChallengeField(chr.getFieldID())) return;
        if (chr.getInstance() != null) chr.getScriptManager().warpInstanceOut(chr, LOBBY);
        else chr.warp(LOBBY);
    }
}
