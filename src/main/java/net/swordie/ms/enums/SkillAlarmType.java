package net.swordie.ms.enums;

import java.util.Arrays;

public enum SkillAlarmType {
    NOT_DEFINED(-1),
    ADD(0),
    UPDATE_STATE(1),
    REMOVE(2),
    CHANGE_POSITION(3),
    NEW_TYPE(4);
    int type;

    SkillAlarmType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public static SkillAlarmType getFromType(int type) {
        return Arrays.stream(SkillAlarmType.values())
                .filter(skillAlertType -> skillAlertType.getType() == type)
                .findFirst().orElse(SkillAlarmType.NOT_DEFINED);
    }
}
