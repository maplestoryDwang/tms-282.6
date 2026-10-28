package net.swordie.ms.enums;

/**
 * Created on 6/7/2018.
 */
public enum LinkedSkillResultType {
    SetSonOfLinkedSkillResult_Success(0),
    SetSonOfLinkedSkillResult_Fail_ParentAlreadyExist(1),
    SetSonOfLinkedSkillResult_Fail_Unknown(2),
    SetSonOfLinkedSkillResult_Fail_MaxCount(3),
    SetSonOfLinkedSkillResult_Fail_DBRequestFail(4),

    SetLinkedSkillResult_YouCannotActivateAnyMoreSkills(2),
    SetLinkedSkillResult_YouHaveAlreadyActivatedThisSkill(3),
    SetLinkedSkillResult_YouHaveAlreadyTransferredThisSkillToday(4),
    SetLinkedSkillResult_UnableToActivateLinkSkill(5),
    SetLinkedSkillResult_InvalidRequest(6),
    SetLinkedSkillResult_Error(5),

    ;

    private final int val;

    LinkedSkillResultType(int val) {
        this.val = val;
    }

    public int getVal() {
        return val;
    }
}
