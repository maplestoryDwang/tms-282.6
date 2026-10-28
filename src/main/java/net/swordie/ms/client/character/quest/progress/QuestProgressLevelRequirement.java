package net.swordie.ms.client.character.quest.progress;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.loaders.DatSerializable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class QuestProgressLevelRequirement extends QuestProgressRequirement {

    private int level;

    public void saveToSQL(int ownerid, long questID, boolean isAccount) {
        if (getId() == 0) {
            String query = "INSERT INTO " + (isAccount ? "`questprogressrequirements_acc`" : "`questprogressrequirements`") + " (" +
                    "`orderNum`, " +
                    (isAccount ? "`accid`, " : "`charid`, ") +
                    "`progresstype`, " +
                    "`questid`, " +
                    "`requiredcount` " +
                    ") VALUES (" +
                    String.format("%d, ", getOrder()) +
                    String.format("%d, ", ownerid) +
                    String.format("'%s', ", "level") +
                    String.format("%d, ", questID) +
                    String.format("%d ", getLevel()) +
                    ");";
            long id = DatabaseManager.executeStatementReturnID(query);
            setId(id);
        }
    }

    public QuestProgressLevelRequirement() {
    }

    public QuestProgressLevelRequirement(int level) {
        this.level = level;
    }

    @Override
    public boolean isComplete(Char chr) {
        return chr.getLevel() >= getLevel();
    }

    @Override
    public QuestProgressRequirement deepCopy() {
        QuestProgressLevelRequirement qplr = new QuestProgressLevelRequirement();
        qplr.setLevel(getLevel());
        qplr.setOrder(getOrder());
        return qplr;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    @Override
    public void write(DataOutputStream dos) throws IOException {
        dos.writeInt(getLevel());
    }

    @Override
    public DatSerializable load(DataInputStream dis) throws IOException {
        return new QuestProgressLevelRequirement(dis.readInt());
    }


}
