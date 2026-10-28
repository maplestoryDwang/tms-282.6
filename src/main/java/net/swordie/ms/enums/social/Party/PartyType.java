package net.swordie.ms.enums.social.Party;

import java.util.Arrays;

public enum PartyType {

    Response_UnexpectedError(-1),
    Request_Load(0),
    Request_Create(1),
    Request_Leave(3),
    Request_Invite(4),
    Request_InviteIntrusion(5),
    Request_Kick(6),
    Request_ChangeLeader(7),
    Request_Apply(8),
    Request_SetAppliable(9),
    Request_ClearIntrusion(10),
    Request_Create_Group(11),
    Request_Join_Group(12),
    Request_Setting(13),

    Response_Load_Success(29), // v263

    //Return: SystemNotice: "You have created a new party."
    Response_Create_Success(30), // v263

    //Return: SystemNotice: "Already have joined a party."
    Response_Create_AlreadyJoined(31), // v263

    //Return: SystemNotice: "A beginner can't create a party."
    Response_Create_IsBeginner(32), // v263

    //Return: SystemNotice: "You have been expelled from the party."
    //Return: SystemNotice: "You have left the party."
    //Return: SystemNotice: "'%s' have been expelled from the party."
    //Return: SystemNotice: "'%s' have left the party."
    //Return: SystemNotice: "You have quit as the leader of the party. The party has been disbanded."
    //Return: SystemNotice: "You have left the party since the party leader quit."
    Response_Leave_Success(33), // v263

    //Return: SystemNotice: "You have yet to join a party."
    Response_Leave_NotJoined(34), // v263

    //Return: SystemNotice: "'%s' has joined the party."
    //Return: SystemNotice: "You have joined the party."
    Response_Join_Success(35), // v263

    //Return: SystemNotice: "You have joined the party."
    Response_Join_Success_ForClient(36), // v263
    //Return: SystemNotice: "Already have joined a party."
    Response_Join_AlreadyJoined(37), // v263
    //Return: SystemNotice: "The party you're trying to join is already in full capacity."
    Response_Join_AlreadyFull(38), // v263

    //Return: SystemNotice: "Due to the party leader disconnecting from the game%2C %s has been assigned as the new leader."
    //Return: SystemNotice: "%s has become the leader of the party.."
    Response_Join_OverDesiredSize(32), //?
    //Nexon Delete
    Response_Join_UnknownUser(33),
    //Nexon Delete
    Response_Join_Unknown(34),
    //Nexon Delete
    Response_Join_FieldLimit(35),
    Response_JoinIntrusion_Success(36),
    Response_JoinIntrusion_UnknownParty(37),

    //Return: BroadcastMsg: "You have invited '%s' to your party."
    //Return: SystemNotice: "Already have joined a party."
    Response_Invite_Success(46), // v263

    Response_Invite_BlockedUser(47),
    Response_Invite_AlreadyInvited(48), // v263
    Response_Invite_AlreadyInvitedByInviter(49),
    Response_Invite_Rejected(50), // v263
    Response_Invite_Accepted(51),
    //Response_Invite_Accepted(40),
    Response_Invite_FieldLimit(52),

    Response_InviteIntrusion_Success(45),
    Response_InviteIntrusion_BlockedUser(46),
    Response_InviteIntrusion_AlreadyInvited(47),
    Response_InviteIntrusion_AlreadyInvitedByInviter(49),
    Response_InviteIntrusion_Rejected(49),
    Response_InviteIntrusion_Accepted(50),

    Response_Kick_Success(51),
    //Return: SystemNotice: "Cannot kick another user in this map"
    Response_Kick_FieldLimit(52),
    Response_Kick_Unknown(53),
    //Return: SystemNotice: "The kick function is unavailable at this time."
    Response_Kick_TimeLimit(54),

    Response_ChangeLeader_Success(58), // v263
    //Return: SystemNotice: "This can only be given to a party member within the vicinity."
    Response_ChangeLeader_NotSameField(56),
    //Return: SystemNotice: "Unable to hand over the leadership post; No party member is currently within the vicinity of the party leader."
    Response_ChangeLeader_NoMemberInSameField(57),
    //Return: SystemNotice: "You may only change with the party member that's on the same channel."
    Response_ChangeLeader_NotSameChannel(58),
    //Response_ChangeLeader_Unknown(54),

    //Return: SystemNotice: "As a GM%2C you're forbidden from creating a party."
    Response_AdminCannotCreate(60),
    //Response_AdminCannotInvite(56),
    //Return: SystemNotice: "You cannot join the same party as someone from a different world. Clear the Main Quest first."
    Response_InAnotherWorld(61),
    //Return: SystemNotice: "Unable to find the requested character in this channel."
    Response_InAnotherChannelOrBlockedUser(58),

    Response_UpdateDataMember(66),

    //Encode: Byte
    //Return: SystemNotice: "The party leader has changed the party join request acceptance setting."
    Response_SetAppliable(69),
    //Return: SystemNotice: "Party settings could not be changed. Please try again later."
    Response_SetAppliableFailed(70),
    //Encode: Int, String, Byte
    Response_SuccessToSelectPQReward(71),
    Response_FailToSelectPQReward(72),
    Response_ReceivePQReward(73),
    Response_FailToRequestPQReward(74),

    //Return: SystemNotice: "Cannot be done in the current map."
    Response_CanNotInThisField(75),

    //Encode: String
    //Return: SystemNotice: "You've requested to join %s's party."
    Response_Apply_Success(76), // v263
    Response_Apply_UnknownParty(77),
    Response_Apply_BlockedUser(78),
    Response_Apply_AlreadyApplied(79),
    Response_Apply_AlreadyAppliedByApplier(80), // v263
    Response_Apply_AlreadyFull(81),
    Response_Apply_Rejected(82), // v263
    Response_Apply_Accepted(83), // v263

    //Encode: String, Int, Int, Int, Int, Int
    Response_FoundPossibleMember(84),

    //Encode: String, Int, Int, Int, Int, Int
    Response_FoundPossibleParty(85),

    Response_Setting_Success(69), // v263

    //Encode: Int, Int, Int
    Response_Load_StarGrade_Success(81),

    //Encode: Int, Int, Int, Int
    Response_Load_StarGrade_Success2(82),
    Response_Member_Rename(83),

    //Encode: Int, Buffer, Buffer, Byte, Byte, String
    PartyInfo_TownPortalChanged(90), // Updated
    PartyInfo_OpenGate(91),
    ;

    private final byte val;

    PartyType(int val) {
        this.val = (byte) val;
    }

    public static PartyType getByVal(byte type) {
        return Arrays.stream(values()).filter(i -> i.getVal() == type).findFirst().orElse(null);
    }

    public byte getVal() {
        return val;
    }
}
