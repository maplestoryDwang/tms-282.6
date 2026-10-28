package net.swordie.ms.client.character.quest.requirement;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.enums.Stat;
import net.swordie.ms.loaders.DatSerializable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Created on 3/2/2018.
 */
public class QuestStartMinStatRequirement implements QuestStartRequirement {

    private Stat stat;
    private short reqAmount;

    public QuestStartMinStatRequirement(Stat stat, short reqAmount) {
        this.reqAmount = reqAmount;
        this.stat = stat;
    }

    public QuestStartMinStatRequirement() {

    }

    public short getReqAmount() {
        return reqAmount;
    }

    public void setReqAmount(short reqAmount) {
        this.reqAmount = reqAmount;
    }

    public Stat getStat() {
        return stat;
    }

    @Override
    public boolean hasRequirements(Char chr) {
        return chr.getStat(getStat()) >= getReqAmount();
    }

    @Override
    public void write(DataOutputStream dos) throws IOException {
        dos.writeInt(stat.getVal());
        dos.writeShort(getReqAmount());
    }

    @Override
    public DatSerializable load(DataInputStream dis) throws IOException {
        return new QuestStartMinStatRequirement(Stat.getByVal(dis.readInt()), dis.readShort());
    }
}
