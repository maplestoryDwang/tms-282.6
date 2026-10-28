package net.swordie.ms.connection.packet;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.handlers.header.OutHeader;

public class HundredBingoPacket {

    public static OutPacket enterGame(int round, int charID) {
        OutPacket outPacket = new OutPacket(OutHeader.FIELD_BINGO_ENTER_GAME);

        outPacket.encodeInt(round);
        outPacket.encodeInt(1);
        outPacket.encodeInt(6);
        outPacket.encodeString("嘿，等一下喔！");
        outPacket.encodeString("數字出現時就點擊它！");
        outPacket.encodeString("連成一條線時請按下賓果按鈕。");
        outPacket.encodeString("我喊賓！你喊果！賓！果！賓！果！");
        outPacket.encodeString("喜歡賓果的就大聲喊出來吧！");
        outPacket.encodeString("遊戲即將開始！");
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
        outPacket.encodeInt(1);
        outPacket.encodeInt(charID);

        return outPacket;
    }

    public static OutPacket hostNumber(int number) {
        OutPacket outPacket = new OutPacket(OutHeader.FIELD_BINGO_HOST_NUMBER);

        outPacket.encodeInt(number);
        outPacket.encodeInt(125);

        return outPacket;
    }

    public static OutPacket hostNumberReady() {
        return new OutPacket(OutHeader.FIELD_BINGO_HOST_NUMBER_READY);
    }

    public static OutPacket addRank(Char chr, int number, int round, int rank) {
        OutPacket outPacket = new OutPacket(OutHeader.FIELD_BINGO_ADD_RANK);

        outPacket.encodeInt(number);
        outPacket.encodeInt(chr.getId());
        outPacket.encodeString(chr.getName());
        outPacket.encodeInt(round);
        outPacket.encodeInt(rank);

        return outPacket;
    }

    public static OutPacket removeRank(int rank) {
        OutPacket outPacket = new OutPacket(OutHeader.FIELD_BINGO_REMOVE_RANK);

        outPacket.encodeInt(rank);

        return outPacket;
    }

    public static OutPacket finishRank(int round, int rank) {
        OutPacket outPacket = new OutPacket(OutHeader.FIELD_BINGO_FINISH_RANK);

        outPacket.encodeInt(round);
        outPacket.encodeInt(rank);

        return outPacket;
    }

    public static OutPacket checkNumberAck(int pos, int number) {
        OutPacket outPacket = new OutPacket(OutHeader.FIELD_BINGO_CHECK_NUMBER_ACK);

        outPacket.encodeInt(pos);
        outPacket.encodeInt(number);
        outPacket.encodeInt(0);
        int size = 0;
        outPacket.encodeInt(size);
        for (int i = 0; i < size; i++) {
            outPacket.encodeInt(0);
        }

        return outPacket;
    }

    public static OutPacket gameState(int gameState, int round) {
        OutPacket outPacket = new OutPacket(OutHeader.FIELD_BINGO_GAME_STATE);

        outPacket.encodeInt(gameState);
        outPacket.encodeInt(round);

        return outPacket;
    }
}
