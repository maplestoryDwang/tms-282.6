package net.swordie.ms.client.character.skills;

import net.swordie.ms.connection.OutPacket;

public class TemporarySkill {

    private byte type;
    private int skillID;
    private byte SLV;
    private int continuousTime;
    private int continuationSkillID;
    private int linkSkillID;
    private boolean processKeyAtDisable;

    public TemporarySkill(byte type, int skillID, byte slv, int continuousTime, int continuationSkillID, int linkSkillID, boolean processKeyAtDisable) {
        this.type = type;
        this.skillID = skillID;
        this.SLV = slv;
        this.continuousTime = continuousTime;
        this.continuationSkillID = continuationSkillID;
        this.linkSkillID = linkSkillID;
        this.processKeyAtDisable = processKeyAtDisable;
    }

    public TemporarySkill(int skillID) {
        this.type = 0;
        this.skillID = skillID;
        this.SLV = 1;
        this.continuousTime = 0;
        this.continuationSkillID = 0;
        this.linkSkillID = 0;
        this.processKeyAtDisable = false;
    }

    public void encode(OutPacket outPacket) {
        outPacket.encodeByte(getType());
        outPacket.encodeInt(getSkillID());
        outPacket.encodeByte(getSLV());
        outPacket.encodeInt(getContinuousTime());
        outPacket.encodeInt(getContinuationSkillID());
        outPacket.encodeInt(getLinkSkillID());
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
        outPacket.encodeByte(isProcessKeyAtDisable());
        outPacket.encodeByte(true);
    }

    public byte getType() {
        return type;
    }

    public void setType(byte type) {
        this.type = type;
    }

    public int getSkillID() {
        return skillID;
    }

    public void setSkillID(int skillID) {
        this.skillID = skillID;
    }

    public byte getSLV() {
        return SLV;
    }

    public void setSLV(byte SLV) {
        this.SLV = SLV;
    }

    public int getContinuousTime() {
        return continuousTime;
    }

    public void setContinuousTime(int continuousTime) {
        this.continuousTime = continuousTime;
    }

    public int getContinuationSkillID() {
        return continuationSkillID;
    }

    public void setContinuationSkillID(int continuationSkillID) {
        this.continuationSkillID = continuationSkillID;
    }

    public int getLinkSkillID() {
        return linkSkillID;
    }

    public void setLinkSkillID(int linkSkillID) {
        this.linkSkillID = linkSkillID;
    }

    public boolean isProcessKeyAtDisable() {
        return processKeyAtDisable;
    }

    public void setProcessKeyAtDisable(boolean processKeyAtDisable) {
        this.processKeyAtDisable = processKeyAtDisable;
    }
}
