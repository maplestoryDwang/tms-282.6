package net.swordie.ms.enums.social.Guild;

import java.util.Arrays;

/**
 * Created on 3/21/2018.
 */
public enum GuildType {
    Request_GuildLoad(0),
    Response_GuildAcceptRequest(1),
    Request_GuildFindByGuildID(2),
    Request_GuildNameInput(3),
    Request_GuildNameCheck(4),
    Request_GuildCreateAgree(5),
    Request_GuildCreate(6),

    Request_GuildInvite(7),
    Request_GuildJoin(8),
    Request_GuildJoinDirect(8),
    Request_GuildUpdateJoinState(10),
    Request_GuildLeave(11),
    Request_GuildKick(12),
    Request_GuildRemove(13),
    Request_IncreaseMaxMember(14),

    Request_ChangeLevel(15),
    Request_ChangeJob(16),
    Request_GuildSetName(17),
    Request_GradeNameSet(18), // v214
    Request_GradePermissionSet(19), // v214
    Request_GradeMemberSet(21), // v214
    Request_MarkSet(22), // v214
    Request_NoticeSet(-1),
    Request_MarkInput(-1),

    Request_Greeting_Edit(23), // v214
    Request_JoinSetting(24), // v214

    Request_Setting_Advertise(25), // v214

    Request_QuestWaitingCheck1(-1), // v214
    Request_QuestWaitingCheck2(-1), // v214
    Request_QuestWaitingInsert(-1), // v214
    Request_QuestWaitingCancel(-1), // v214
    Request_QuestRemoveCompleteGuild(-1), // v214

    Request_HonorEXPIncrease(28),
    Request_ContributionIncrease(29),
    Request_GGPDecrease(30),
    Request_IGPDecrease(31),

    Request_QuestTimeSet(32),
    Request_GuildRankingShow(33),

    Request_SkillSet(34),
    Request_SkillLevelSetUp(37), // v214
    Request_GuildBattleSkillReset(-1), // v214
    Request_UseActiveSkill(40), // v214
    Request_UseADGuildSkill(41), // v214
    Request_SkillExtend(42), // v214
    Request_GuildChangeMaster(43), // v214
    Request_GuildSkillUse_FromMember(44), // v214

    Request_GGPSet(42),
    Request_IGPSet(-1),

    Request_BattleSkillOpen(44),
    Request_GuildSearch(46),

    Request_GuildCreateNew_Block(47),
    Request_AllianceCreateNew_Block(48),

    Request_Guild_CheckIn(50),

    Request_GuildRequest_Success(25), // v214

    Request_GuildLoad_New(54),

    Request_GuildBSS_Load(57),

    // Responses ------------------------------

    Response_GuildLoad_Success(59),
    Response_GuildFind_Success(60),
    Response_GuildNameCheck_Available(61),
    Response_GuildNameCheck_AlreadyUsed(62),
    Response_GuildNameCheck_Unknown(63),
    Response_GuildCreateAgree_Reply(64),
    Response_GuildCreateAgree_Unknown(65),

    Response_GuildCreate_Success(67),
    Response_GuildCreate_AlreadyJoined(68),
    Response_GuildCreate_AlreadyExits(69),
    Response_GuildCreate_Beginner(70),
    Response_GuildCreate_Disagree(71),
    Response_GuildCreate_NotFullParty(72),
    Response_GuildCreate_Unknown(73),

    Response_GuildInvite_Success(74),
    Response_GuildInvite_AlreadyJoined(75),         //Text: Char name already joined the guild.
    Response_GuildInvite_AlreadyJoinedToUser(76),         //Text: Char name already joined the guild.
    Response_GuildInvite_AlreadyFull(77),           //Text: The guild you are trying to join has already reached the max number of users.
    Response_GuildInvite_UnknowUser(79),            //Text: The guild you are trying to join has already reached the max number of requests.
    Response_GuildInvite_NotSameChannel(80),        //Text: The character cannot be found in the current channel.
    Response_GuildInvite_Unknown_1(81),             //Text: Cannot find the character that requested to join
    Response_GuildInvite_Unknown_2(82),             //Text: The guild request has not been accepted, due to unknown reason.

    Response_GuildRequest_Success(83), // v214
    //Response_GuildRequest_Cancelled(84), // v214

    Response_GuildRequest_OnlyApplyTo5Guilds(87), // v214
    Response_GuildRequest_LimitTime(88), // v214

    Response_GuildRequest_Cancelled(90), // v214

    Response_GuildLeave_Success(91), // v214
    Response_GuildLeave_NotJoined(92), // v214
    Response_GuildLeave_Unknown(93), // v214

    Response_GuildKick_Success(94), // v214
    Response_GuildKick_NotJoined(95), // v214
    Response_GuildKick_Unknown(96),  // v214

    Response_GuildRemove_Success(97),
    Response_GuildRemove_NotExist(98),
    Response_GuildRemove_Unknown(99),
    Response_GuildRemoveRequest_Success(100),

    Response_GuildInvite_DisableInvited(101),        //Text: Char name is currently not accepting guild invite message.
    Response_GuildInvite_HaveAnotherInvited(102),    //Text: 'Char name' is taking care of another inviation.

    Response_GuildInvite_Rejected(103),              //Text: Char name has denied your guild invitation.
    // 104 - > 105 encodeString
    Response_GuildJoin_Success_New(106),
    // 107 - 111
    Response_AdminCannotCreate(-1),
    Response_AdminCannotInvite(-1),

    Response_MaxMemberIncrease_Success(116), // removed?
    Response_MaxMemberIncrease_Unknown(117),

    Response_ChangeMemberName(116),
    Response_ChangeRequestUserName(117),

    Response_ChangeLevelOrJob_Success(118), // v214
    Response_GuildNotify_LoginOrLogout(119), // v214

    Response_GradeNameSet_Success(120), // v214

    Response_GradePermissionSet_Success(122), // v214

    Response_GradeMemberSet_Success(126), // v214
    Response_GradeMemberSet_Unknown(127),            //Text: The guild request has noot been accepted, due to unknown reason.

    Response_CommitmentMemberSet_Success(128), // v214
    Response_CommitmentMemberSet_Unknown(129),

    Response_MarkSet_Success(130),
    Response_MarkSet_Unknown(131),                  //Text: Unable to create the Guild Emblem. Creation Requirement: Guild level 2 or higher 150,000 GP

    Response_NoticeSet_Success(137), // v214

    Response_QuestInsert(132),
    Response_NoticeQuestWaitingOrder(133),
    Response_SetGuildCanEnterQuest(134),

    Response_PointIncrease_Success(147), // v214

    Response_ShowGuildRanking(148), // Removed

    Response_GGPSet_Success(149), // v214

    Response_IGPSet_Success(150), // Removed

    Response_GuildQuest_NotEnoughUser(151),
    Response_GuildQuest_RegisterDisconnected(152),
    Response_GuildQuest_NoticeOrder(154), //TODO: encodeByte(????) + encodeInt(Type) (type == 0 | 1 | 2)

    Response_Authkey_Update(154), // Removed

    Response_SkillSet_Success(155),
    Response_SkillSet_Extend_Unknown(156),
    Response_SkillSet_LevelSet_Unknown(157),
    Response_SKillSet_BattleSkillReset(158),

    Response_SkillUse_Success(159),
    Response_SkillUse_Error(160),

    Response_ChangeName_Done(161), //TODO: encodeInt(guildID) + encodeString(????) + encodeString(?????)
    Response_ChangeName_Unknown(162),
    Response_GuildChangeMaster_Success(163),// TODO: encodeInt + encodeInt + encodeInt + encodeByte + if (??) => encodeInt
    Response_GuildChangeMaster_Unknown(164),

    Response_BlockedBehaviorCreate(165),
    Response_BlockedBehaviorJoin(166),
    Response_BattleSkillOpen_Success(167),
    Response_GetData(-1),
    Response_Rank_Reflash(169), // SAI
    Response_GuildFind_Error(170),

    Response_ChangeGuildMaster_Success(168), // v214

    Response_ChangeMaster_Pinkbean(171),

    Response_Unknown(-1),

    ;

    private final byte val;

    GuildType(int val) {
        this.val = (byte) val;
    }

    public static GuildType getTypeByVal(byte val) {
        return Arrays.stream(values()).filter(grt -> grt.getVal() == val).findAny().orElse(null);
    }

    public byte getVal() {
        return val;
    }
}
