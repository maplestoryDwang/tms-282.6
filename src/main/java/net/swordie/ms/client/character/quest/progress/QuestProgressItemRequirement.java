package net.swordie.ms.client.character.quest.progress;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.loaders.DatSerializable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class QuestProgressItemRequirement extends QuestProgressRequirement {

    private int itemID;
    private int requiredCount;
    private int currentCount;

    public void saveToSQL(int ownerid, long questID, boolean isAccount) {
        if (getId() == 0) {
            String query = "INSERT INTO " + (isAccount ? "`questprogressrequirements_acc`" : "`questprogressrequirements`") + " (" +
                    "`orderNum`, " +
                    (isAccount ? "`accid`, " : "`charid`, ") +
                    "`progresstype`, " +
                    "`questid`, " +
                    "`unitid`, " +
                    "`requiredcount` " +
                    ") VALUES (" +
                    String.format("%d, ", getOrder()) +
                    String.format("%d, ", ownerid) +
                    String.format("'%s', ", "item") +
                    String.format("%d, ", questID) +
                    String.format("%d, ", getItemID()) +
                    String.format("%d ", getRequiredCount()) +
                    ");";
            long id = DatabaseManager.executeStatementReturnID(query);
            setId(id);
        }
    }

    public QuestProgressItemRequirement() {
    }

    public int getItemID() {
        return itemID;
    }

    public void setItemID(int itemID) {
        this.itemID = itemID;
    }

    public int getRequiredCount() {
        return requiredCount;
    }

    public void setRequiredCount(int requiredCount) {
        this.requiredCount = requiredCount;
    }

    public int getCurrentCount() {
        return currentCount;
    }

    public void setCurrentCount(int currentCount) {
        this.currentCount = currentCount;
    }

    @Override
    public boolean isComplete(Char chr) {
        return chr.hasItemCount(getItemID(), getRequiredCount());
    }

    @Override
    public QuestProgressRequirement deepCopy() {
        QuestProgressItemRequirement qpir = new QuestProgressItemRequirement();
        qpir.setItemID(getItemID());
        qpir.setRequiredCount(getRequiredCount());
        qpir.setCurrentCount(getCurrentCount());
        qpir.setOrder(getOrder());
        return qpir;
    }

    @Override
    public void write(DataOutputStream dos) throws IOException {
        dos.writeInt(getItemID());
        dos.writeInt(getRequiredCount());
        dos.writeInt(getCurrentCount());
        dos.writeInt(getOrder());
    }

    @Override
    public DatSerializable load(DataInputStream dis) throws IOException {
        QuestProgressItemRequirement qpir = new QuestProgressItemRequirement();
        qpir.setItemID(dis.readInt());
        qpir.setRequiredCount(dis.readInt());
        qpir.setCurrentCount(dis.readInt());
        qpir.setOrder(dis.readInt());
        return qpir;
    }
}
