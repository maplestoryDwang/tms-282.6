package net.swordie.ms.handlers.user;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.Macro;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.handlers.Handler;
import net.swordie.ms.handlers.header.InHeader;

import java.util.ArrayList;
import java.util.List;

public class MacroHandler {

    @Handler(op = InHeader.USER_MACRO_SYS_DATA_MODIFIED)
    public static void handleUserMacroSysDataModified(Char chr, InPacket inPacket) {
        //Todo: Verify Skill for anti skill inject
        byte size = inPacket.decodeByte();
        List<Macro> macros = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            Macro macro = new Macro();
            macro.setName(inPacket.decodeString());
            macro.setMuted(inPacket.decodeByte() != 0);
            for (int j = 0; j < 3; j++) {
                macro.setSkillAtPos(j, inPacket.decodeInt());
            }
            macros.add(macro);
        }
        for (Macro macro : chr.getMacros()) {
            macro.deleteFromSQL();
        }
        chr.getMacros().clear();
        chr.getMacros().addAll(macros); // don't set macros directly, as a new row will be made in the DB
    }

    @Handler(op = InHeader.USER_ANTI_MACRO_QUESTION_RESULT)
    public static void handleUserAntiMacroQuestionResult(Char chr, InPacket inPacket) {
        short length = inPacket.decodeShort();
        if (length > 0) {
            String answer = inPacket.decodeString(length);
            boolean ok = answer.length() >= 6 && answer.equalsIgnoreCase(chr.getLieDetectorAnswer());
            net.swordie.ms.world.tms282.audit.AuditLog.antiMacro(chr, 0, 0, chr.getLieDetectorAnswer(), answer, ok, ok ? null : "WRONG_ANSWER"); // audit

            if (answer.length() < 6 || !answer.equalsIgnoreCase(chr.getLieDetectorAnswer())) {
                chr.failedLieDetector();
            } else {
                chr.passedLieDetector();
            }
        } else {
            net.swordie.ms.world.tms282.audit.AuditLog.antiMacro(chr, 0, 0, chr.getLieDetectorAnswer(), null, false, "EMPTY_ANSWER"); // audit
            chr.failedLieDetector();
            chr.dispose();
        }
    }

    @Handler(op = InHeader.USER_ANTI_MACRO_REFRESH_REQUEST)
    public static void handleUserAntiMacroRefreshResult(Char chr, InPacket inPacket) {
        // attempting to refresh while there's no LD
        if (chr.getLieDetectorAnswer().length() > 0) {
            chr.sendLieDetector(true);
        }
    }
}
