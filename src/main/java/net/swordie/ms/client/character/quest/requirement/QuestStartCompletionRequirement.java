package net.swordie.ms.client.character.quest.requirement;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.loaders.DatSerializable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class QuestStartCompletionRequirement implements QuestStartRequirement {

    private int questID;
    private byte questStatus;

    public QuestStartCompletionRequirement() {
    }

    public QuestStartCompletionRequirement(int questID, byte questStatus) {
        this.questID = questID;
        this.questStatus = questStatus;
    }

    public int getQuestID() {
        return questID;
    }

    public void setQuestID(int questID) {
        this.questID = questID;
    }

    public byte getQuestStatus() {
        return questStatus;
    }

    public void setQuestStatus(byte questStatus) {
        this.questStatus = questStatus;
    }

    @Override
    public boolean hasRequirements(Char chr) {
        if (net.swordie.ms.connection.protocol.Protocol.active().version() == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) {
            return switch (getQuestStatus()) {
                case 0 -> !chr.hasQuestInProgress(getQuestID()) && !chr.hasQuestCompleted(getQuestID());
                case 1 -> chr.hasQuestInProgress(getQuestID());
                case 2 -> chr.hasQuestCompleted(getQuestID());
                default -> false;
            };
        }
        switch (getQuestStatus()) {
//            case 0: // Not started
//                return !chr.hasQuestInProgress(getQuestID()) && !chr.hasQuestCompleted(getQuestID());
//            case 1: // In progress
//                return chr.hasQuestInProgress(getQuestID());
            case 0: // Completed
                return chr.hasQuestCompleted(getQuestID());
            default:
                System.out.printf("Unknown quest status %d%n", getQuestStatus());
                return true;
        }
    }

    @Override
    public void write(DataOutputStream dos) throws IOException {
        dos.writeInt(getQuestID());
        dos.writeByte(getQuestStatus());
    }

    @Override
    public DatSerializable load(DataInputStream dis) throws IOException {
        QuestStartCompletionRequirement qscr = new QuestStartCompletionRequirement();
        qscr.setQuestID(dis.readInt());
        qscr.setQuestStatus(dis.readByte());
        return qscr;
    }
}
