package net.swordie.ms.client.character.skills.info;

public class ExtraSkillInfo {

    public int skillId;
    public int delay;
    public int manual;

    public ExtraSkillInfo() {
    }

    public int getSkillId() {
        return skillId;
    }

    public void setSkillId(int skillId) {
        this.skillId = skillId;
    }

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public int getManual() {
        return manual;
    }

    public void setManual(int manual) {
        this.manual = manual;
    }
}
