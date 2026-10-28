package net.swordie.ms.client.character.quest.reward;

import net.swordie.ms.client.Account;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.enums.Stat;
import net.swordie.ms.loaders.DatSerializable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Created on 3/2/2018.
 */
public class QuestTraitReward implements QuestReward {

    private int stat;
    private int amount;

    public QuestTraitReward(int stat, int amount) {
        this.stat = stat;
        this.amount = amount;
    }

    public QuestTraitReward() {

    }

    public int getStat() {
        return stat;
    }

    public void setStat(int stat) {
        this.stat = stat;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    @Override
    public void giveReward(Char chr) {
        chr.addStatAndSendPacket(Stat.getByVal(getStat()), getAmount());
    }

    @Override
    public void giveReward(Account account, String questName) {
        account.getCurrentChr().addStatAndSendPacket(Stat.getByVal(getStat()), getAmount());
    }

    @Override
    public void write(DataOutputStream dos) throws IOException {
        dos.writeInt(getStat());
        dos.writeInt(getAmount());
    }

    @Override
    public DatSerializable load(DataInputStream dis) throws IOException {
        return new QuestTraitReward(dis.readInt(), dis.readInt());
    }
}
