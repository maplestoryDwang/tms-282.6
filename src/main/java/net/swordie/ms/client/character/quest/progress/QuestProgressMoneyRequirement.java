package net.swordie.ms.client.character.quest.progress;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.loaders.DatSerializable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class QuestProgressMoneyRequirement extends QuestProgressRequirement {

    private int money;

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
                    String.format("'%s', ", "money") +
                    String.format("%d, ", questID) +
                    "NULL, " +
                    String.format("%d ", getMoney()) +
                    ");";
            long id = DatabaseManager.executeStatementReturnID(query);
            setId(id);
        }
    }

    public QuestProgressMoneyRequirement() {
    }

    public QuestProgressMoneyRequirement(int money) {
        this.money = money;
    }

    @Override
    public boolean isComplete(Char chr) {
        return chr.getMoney() >= getMoney();
    }

    @Override
    public QuestProgressRequirement deepCopy() {
        QuestProgressMoneyRequirement qpmr = new QuestProgressMoneyRequirement();
        qpmr.setMoney(getMoney());
        qpmr.setOrder(getOrder());
        return qpmr;
    }

    public int getMoney() {
        return money;
    }

    public void setMoney(int money) {
        this.money = money;
    }

    @Override
    public void write(DataOutputStream dos) throws IOException {
        dos.writeInt(getMoney());
    }

    @Override
    public DatSerializable load(DataInputStream dis) throws IOException {
        return new QuestProgressMoneyRequirement(dis.readInt());
    }

    public void addMoney(int money) {
        setMoney(getMoney() + money);
    }
}
