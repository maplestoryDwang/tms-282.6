package net.swordie.ms.client.character.potential;

import net.swordie.ms.constants.GameConstants;
import net.swordie.ms.util.Util;

public class CharacterPotentialValueHolder {
    private byte key;
    private int skillID;
    private byte slv;
    private byte grade;

    public CharacterPotentialValueHolder() {
    }

    public CharacterPotentialValueHolder(byte key, int skillID, byte slv, byte grade) {
        this.key = key;
        this.skillID = skillID;
        this.slv = slv;
        this.grade = grade;
    }

    public byte getKey() {
        return key;
    }

    public void setKey(byte key) {
        this.key = key;
    }

    public int getSkillID() {
        return skillID;
    }

    public void setSkillID(int skillID) {
        this.skillID = skillID;
    }

    public byte getSlv() {
        return slv;
    }

    public void setSlv(byte slv) {
        this.slv = slv;
    }

    public byte getGrade() {
        return grade;
    }

    public void setGrade(byte grade) {
        this.grade = grade;
    }
}
