package net.swordie.ms.life.movement;

import net.swordie.ms.util.Position;

/**
 * Created on 1/2/2018.
 * These classes + children/parents are basically the same as Mushy, credits to @MaxCloud.
 */
public abstract class MovementBase implements Movement {
    protected byte command;
    protected byte moveAction;
    protected byte forcedStop;
    protected byte stat;

    protected short fh;
    protected short footStart;
    protected short elapse;
    protected short unk;

    protected Position position;
    protected Position vPosition;
    protected Position offset;

    protected short MPA;
    protected short Param1;
    protected short Param2;
    protected short Param3;
    protected short Param4;
    protected short Param5;
    protected short Param6;
    protected int idk;

    @Override
    public byte getCommand() {
        return command;
    }

    @Override
    public byte getMoveAction() {
        return moveAction;
    }

    @Override
    public byte getForcedStop() {
        return forcedStop;
    }

    @Override
    public byte getStat() {
        return stat;
    }

    @Override
    public short getFh() {
        return fh;
    }

    @Override
    public short getFootStart() {
        return footStart;
    }

    public short getElapse() {
        return elapse;
    }

    @Override
    public Position getPosition() {
        return position;
    }

    @Override
    public Position getVPosition() {
        return vPosition;
    }

    @Override
    public Position getOffset() {
        return offset;
    }

    @Override
    public short getDuration() {
        return elapse;
    }

    public short getMPA() {
        return MPA;
    }

    public short getParam1() {
        return Param1;
    }

    public short getParam2() {
        return Param2;
    }

    public short getParam3() {
        return Param3;
    }

    public short getParam4() {
        return Param4;
    }

    public short getParam5() {
        return Param5;
    }

    public short getParam6() {
        return Param6;
    }

    public int getIdk() {
        return idk;
    }

    public void setIdk(int idk) {
        this.idk = idk;
    }

    public void setUnk(short unk) {
        this.unk = unk;
    }

    public short getUnk() {
        return unk;
    }
}
