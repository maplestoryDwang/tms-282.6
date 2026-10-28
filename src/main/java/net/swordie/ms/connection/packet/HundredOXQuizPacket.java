package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.handlers.header.OutHeader;
import net.swordie.ms.util.Position;

public class HundredOXQuizPacket {

    public static OutPacket footHold(Position pos) {
        OutPacket outPacket = new OutPacket(OutHeader.FIELD_HUNDREDOXQUIZ_FOOT_HOLD);

        outPacket.encodePosition(pos);

        return outPacket;
    }

    public static OutPacket questions(int totalQuestionLeft, String question) {
        OutPacket outPacket = new OutPacket(OutHeader.FIELD_HUNDREDOXQUIZ_QUESTIONS);

        outPacket.encodeInt(totalQuestionLeft);
        outPacket.encodeInt(20 - totalQuestionLeft + 1);
        outPacket.encodeString(question);

        return outPacket;
    }

    public static OutPacket explan(int questionID, String explain) {
        OutPacket outPacket = new OutPacket(OutHeader.FIELD_HUNDREDOXQUIZ_EXPLAN);

        outPacket.encodeInt(questionID);
        outPacket.encodeString(explain);

        return outPacket;
    }

    public static OutPacket countEffect() {
        return new OutPacket(OutHeader.FIELD_HUNDREDOXQUIZ_COUNT_EFFECT);
    }

    public static OutPacket moveToPortal(int portalID) {
        OutPacket outPacket = new OutPacket(OutHeader.FIELD_HUNDREDOXQUIZ_OX_QUIZ_MOVE_TO_PORTAL);

        outPacket.encodeByte(portalID); // 11-19

        return outPacket;
    }

    public static OutPacket answerResult(boolean isSuccess) {
        OutPacket outPacket = new OutPacket(OutHeader.FIELD_HUNDREDOXQUIZ_ANSWER_RESULT);

        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
        outPacket.encodeInt(isSuccess ? 1 : 0);

        return outPacket;
    }
}
