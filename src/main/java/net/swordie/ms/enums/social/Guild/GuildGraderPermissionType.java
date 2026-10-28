package net.swordie.ms.enums.social.Guild;

import java.util.Arrays;

public enum GuildGraderPermissionType {
    None(0),
    InviteMembers(1),
    EditGreeting(2),
    SetMemberRank(4),
    EditEmblem(8),
    KickMember(16),
    ManageBulletinBoard(32),
    AcceptNewMembers(64),
    ManageGuildSkills(0),
    UseGuildSkills(0),
    ManageCulvertEntry(0),
    Everything(-1),
    ;

    private final int val;

    GuildGraderPermissionType(int val) {
        this.val = val;
    }

    public static GuildGraderPermissionType getByValue(byte val) {
        return Arrays.stream(values()).filter(gbt -> gbt.getVal() == val).findAny().orElse(null);
    }

    public int getVal() {
        return val;
    }
}
