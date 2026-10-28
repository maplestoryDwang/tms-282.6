package net.swordie.ms.client.character.skills;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.life.Summon;
import net.swordie.ms.util.Position;

import java.util.ArrayList;
import java.util.List;

public class ShootObjectSkillInfo {
    private Summon summonOwner;
    private int charID;
    private int skillId;
    private int slv;
    private Position position;
    private int action;
    private int actionSpeed;
    private List<ShootObject> shootObjects = new ArrayList<>();
    private int projectileItemId;
    private int projectileItemPosition;
    private byte unknownBool;
    private boolean encodeExtra;

    public int extraEncodeInt1;
    public int extraEncodeInt2;
    public int extraEncodeInt3;
    public int extraEncodeInt4;
    public int extraEncodeInt5;
    public int extraEncodeInt6;
    public byte extraEncodeByte1;
    public int extraEncodeInt7;
    public byte extraEncodeByte2;
    public long extraEncodeLong1;
    public int extraEncodeInt8;
    public int extraEncodeInt9;

    public ShootObjectSkillInfo(int charID) {
        this.charID = charID;
    }

    public Summon getSummonOwner() {
        return summonOwner;
    }

    public void setSummonOwner(Summon summonOwner) {
        this.summonOwner = summonOwner;
    }

    public int getCharID() {
        return charID;
    }

    public void setChr(int charID) {
        this.charID = charID;
    }

    public int getSkillId() {
        return skillId;
    }

    public void setSkillId(int skillId) {
        this.skillId = skillId;
    }

    public int getSlv() {
        return slv;
    }

    public void setSlv(int slv) {
        this.slv = slv;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public int getAction() {
        return action;
    }

    public void setAction(int action) {
        this.action = action;
    }

    public int getActionSpeed() {
        return actionSpeed;
    }

    public void setActionSpeed(int actionSpeed) {
        this.actionSpeed = actionSpeed;
    }

    public List<ShootObject> getShootObjects() {
        return shootObjects;
    }

    public void setShootObjects(List<ShootObject> shootObjects) {
        this.shootObjects = shootObjects;
    }

    public int getProjectileItemId() {
        return projectileItemId;
    }

    public void setProjectileItemId(int projectileItemId) {
        this.projectileItemId = projectileItemId;
    }

    public int getProjectileItemPosition() {
        return projectileItemPosition;
    }

    public void setProjectileItemPosition(int projectileItemPosition) {
        this.projectileItemPosition = projectileItemPosition;
    }

    public boolean isEncodeExtra() {
        return encodeExtra;
    }

    public void setEncodeExtra(boolean encodeExtra) {
        this.encodeExtra = encodeExtra;
    }

    public byte getUnknownBool() {
        return unknownBool;
    }

    public void setUnknownBool(byte unknownBool) {
        this.unknownBool = unknownBool;
    }
}
