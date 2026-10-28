package net.swordie.ms.client.social.Guild;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.enums.social.Guild.GuildType;
import net.swordie.ms.util.FileTime;

import java.util.List;

import static net.swordie.ms.enums.social.Guild.GuildType.*;

/**
 * Created on 3/21/2018.
 */
public class GuildResult {

    private GuildType type;
    private Guild guild;
    private GuildMember member;
    private GuildRequestor requestor;
    private boolean bool;
    private boolean showBox;
    private Char chr;
    private int value1;
    private int value2;
    private String stringArg;
    private GuildSkill skill;
    private List<Integer> list;

    private GuildResult(GuildType type) {
        this.type = type;
    }

    public static GuildResult response_GuildLoad_Success(Guild guild) {
        GuildResult gri = new GuildResult(Response_GuildLoad_Success);
        gri.guild = guild;
        return gri;
    }

    public static GuildResult response_GradeSetName_Success(Guild guild, int gradeChanged) {
        GuildResult gri = new GuildResult(Response_GradeNameSet_Success);
        gri.guild = guild;
        gri.value1 = gradeChanged;
        return gri;
    }

    public static GuildResult response_GradeSetPermission_Success(Guild guild, int gradeChanged) {
        GuildResult gri = new GuildResult(Response_GradePermissionSet_Success);
        gri.guild = guild;
        gri.value1 = gradeChanged;
        return gri;
    }

    public static GuildResult response_GuildCreate_Success(Guild guild) {
        GuildResult gri = new GuildResult(Response_GuildCreate_Success);
        gri.guild = guild;
        return gri;
    }

    public static GuildResult response_GuildFind_Success(Guild guild) {
        GuildResult gri = new GuildResult(Response_GuildFind_Success);
        gri.guild = guild;
        return gri;
    }

    public static GuildResult request_GuildInvite(Char chr) {
        GuildResult gri = new GuildResult(Request_GuildInvite);
        gri.chr = chr;
        return gri;
    }

    public static GuildResult request_GuildInvite_New(Char chr) {
        GuildResult gri = new GuildResult(Response_GuildJoin_Success_New);
        gri.chr = chr;
        return gri;
    }

    public static GuildResult response_GuildInvite_Success(Guild guild, GuildMember member) {
        GuildResult gri = new GuildResult(Response_GuildInvite_Success);
        gri.guild = guild;
        gri.member = member;
        return gri;
    }

    public static GuildResult response_GuildLeave_Success(Guild guild, int leaverID, String leaverName) {
        GuildResult gri = new GuildResult(Response_GuildLeave_Success);
        gri.guild = guild;
        gri.value1 = leaverID;
        gri.stringArg = leaverName;
        return gri;
    }

    public static GuildResult response_GuildKick_Success(Guild guild, int leaverID, String leaverName) {
        GuildResult gri = new GuildResult(Response_GuildKick_Success);
        gri.guild = guild;
        gri.value1 = leaverID;
        gri.stringArg = leaverName;
        return gri;
    }

    public static GuildResult response_MarkSet_Success(Guild guild, Char chr) {
        GuildResult gri = new GuildResult(Response_MarkSet_Success);
        gri.guild = guild;
        gri.chr = chr;
        return gri;
    }

    public static GuildResult response_GradeSetMember_Success(Guild guild, GuildMember member, int intArg) {
        GuildResult gri = new GuildResult(Response_GradeMemberSet_Success);
        gri.guild = guild;
        gri.value1 = intArg;
        gri.member = member;
        return gri;
    }

    public static GuildResult response_ChangeLevelOrJob_Success(Guild guild, GuildMember member) {
        GuildResult gri = new GuildResult(Response_ChangeLevelOrJob_Success);
        gri.guild = guild;
        gri.member = member;
        return gri;
    }

    public static GuildResult response_GuildNotify_LoginOrLogout(Guild guild, GuildMember member, boolean online, boolean showBox) {
        GuildResult gri = new GuildResult(Response_GuildNotify_LoginOrLogout);
        gri.guild = guild;
        gri.member = member;
        gri.bool = online;
        gri.showBox = showBox;
        return gri;
    }

    public static GuildResult response_CommitmentMemberSet_Success(Guild guild, GuildMember member) {
        GuildResult gri = new GuildResult(Response_CommitmentMemberSet_Success);
        gri.guild = guild;
        gri.member = member;
        return gri;
    }

    public static GuildResult response_GGPSet_Success(Guild guild) {
        GuildResult gri = new GuildResult(Response_GGPSet_Success);
        gri.guild = guild;
        return gri;
    }

    public static GuildResult response_IGPSet_Success(Guild guild, GuildMember member) {
        GuildResult gri = new GuildResult(Response_IGPSet_Success);
        gri.guild = guild;
        gri.member = member;
        return gri;
    }

    public static GuildResult response_PointIncrease_Success(Guild guild) {
        GuildResult gr = new GuildResult(Response_PointIncrease_Success);
        gr.guild = guild;
        return gr;
    }

    public static GuildResult response_SkillSet_Success(Guild guild, GuildSkill skill, int buyCharID) {
        GuildResult gr = new GuildResult(Response_SkillSet_Success);
        gr.guild = guild;
        gr.skill = skill;
        gr.value1 = buyCharID;
        return gr;
    }

    public static GuildResult request_BattleSkillOpen(Guild guild) {
        GuildResult gr = new GuildResult(Request_BattleSkillOpen);
        gr.guild = guild;
        return gr;
    }

    public static GuildResult response_Rank_Reflash(Guild guild) {
        GuildResult gr = new GuildResult(Response_Rank_Reflash);
        gr.guild = guild;
        return gr;
    }

    public static GuildResult response_MaxMemberIncrease_Success(Guild guild) {
        GuildResult gr = new GuildResult(Response_MaxMemberIncrease_Success);
        gr.guild = guild;
        return gr;
    }

    public static GuildResult response_GuildInvite_Rejected(String rejectedName) {
        GuildResult gr = new GuildResult(Response_GuildInvite_Rejected);
        gr.stringArg = rejectedName;
        return gr;
    }

    public static GuildResult response_GuideInvite_DisableInvited(String blockName) {
        GuildResult gr = new GuildResult(Response_GuildInvite_DisableInvited);
        gr.stringArg = blockName;
        return gr;
    }

    public static GuildResult response_GuideInvite_HaveAnotherInvited(String invitedName) {
        GuildResult gr = new GuildResult(Response_GuildInvite_HaveAnotherInvited);
        gr.stringArg = invitedName;
        return gr;
    }

    public static GuildResult response_GuildInvite_Unknown_1(int guidID) {
        GuildResult gr = new GuildResult(Response_GuildInvite_Unknown_1);
        gr.value1 = guidID;
        return gr;
    }

    public static GuildResult response_GuildRequest_Success(Char chr, Guild guild, GuildRequestor requestor) {
        GuildResult gr = new GuildResult(Response_GuildRequest_Success);
        gr.chr = chr;
        gr.guild = guild;
        gr.requestor = requestor;
        return gr;
    }

    public static GuildResult response_GuildRequest_Cancelled(Char chr, Guild guild) {
        GuildResult gr = new GuildResult(Response_GuildRequest_Cancelled);
        gr.chr = chr;
        gr.guild = guild;
        return gr;
    }

    public static GuildResult response_BattleSkillOpen_Success(Guild guild) {
        GuildResult gr = new GuildResult(Response_BattleSkillOpen_Success);
        gr.guild = guild;
        return gr;
    }

    public static GuildResult response_ChangeGuildMaster_Success(Guild guild, int oldLeader, int newLeader) {
        GuildResult gr = new GuildResult(Response_ChangeGuildMaster_Success);
        gr.guild = guild;
        gr.value1 = oldLeader;
        gr.value2 = newLeader;
        return gr;
    }

    public static GuildResult response_NoticeSet_Success(Guild guild, Char chr, String newNotice) {
        GuildResult gr = new GuildResult(Response_NoticeSet_Success);
        gr.guild = guild;
        gr.chr = chr;
        gr.stringArg = newNotice;
        return gr;
    }

    public static GuildResult msg(GuildType type) {
        return new GuildResult(type);
    }

    public void encode(OutPacket outPacket) {
        outPacket.encodeByte(type.getVal());
        switch (type) {
            case Response_GuildLoad_Success:
                outPacket.encodeInt(50000); //??
                outPacket.encodeByte(guild != null);
                if (guild != null) {
                    outPacket.encodeInt(0);
                    outPacket.encode(guild);
                    outPacket.encodeInt(0);
                }
                break;
            case Response_GuildCreate_Success:
                outPacket.encode(guild);
                break;
            case Response_GuildJoin_Success_New:
                outPacket.encodeInt(chr.getGuild().getId());
                outPacket.encodeString(chr.getGuild().getName());
                outPacket.encodeInt(chr.getId());
                outPacket.encodeString(chr.getName());
                outPacket.encodeInt(chr.getLevel());
                outPacket.encodeInt(chr.getJob());
                outPacket.encodeInt(chr.getSubJob());
                break;
            case Response_GuildRequest_Success:
                outPacket.encodeInt(guild.getId());
                outPacket.encodeInt(chr.getId());
                requestor.encode(outPacket);
                break;
            case Response_GuildFind_Success:
                outPacket.encodeInt(guild.getId());
                outPacket.encodeString("");
                outPacket.encode(guild);
                outPacket.encodeShort(0);
                outPacket.encodeShort(0);
                outPacket.encodeShort(0);
                break;
            case Request_GuildInvite:
                outPacket.encodeInt(chr.getGuild().getId());
                outPacket.encodeString(chr.getName());
                outPacket.encodeInt(chr.getLevel());
                outPacket.encodeInt(chr.getJob());
                outPacket.encodeInt(chr.getSubJob());
                break;
            case Response_GuildInvite_Success:
                outPacket.encodeInt(guild.getId());
                outPacket.encodeInt(member.getCharID());
                outPacket.encode(member);
                break;
            case Response_GuildInvite_Rejected:
                outPacket.encodeString(stringArg);
                break;
            case Response_GuildKick_Success:
            case Response_GuildLeave_Success:
                if (guild != null) {
                    outPacket.encodeInt(guild.getId());
                } else {
                    outPacket.encodeInt(0);
                }
                outPacket.encodeInt(value1); // expelledID
                outPacket.encodeString(stringArg); // expelledName
                break;
            case Response_GradeNameSet_Success:
                outPacket.encodeInt(guild.getId());
                outPacket.encodeInt(value1);
                outPacket.encodeString(guild.getGrade1());
                outPacket.encodeString(guild.getGrade2());
                outPacket.encodeString(guild.getGrade3());
                outPacket.encodeString(guild.getGrade4());
                outPacket.encodeString(guild.getGrade5());
                break;
            case Response_GradePermissionSet_Success:
                outPacket.encodeInt(guild.getId());
                outPacket.encodeInt(value1);
                outPacket.encodeInt(guild.getGrade1Permission());
                outPacket.encodeInt(guild.getGrade2Permission());
                outPacket.encodeInt(guild.getGrade3Permission());
                outPacket.encodeInt(guild.getGrade4Permission());
                outPacket.encodeInt(guild.getGrade5Permission());
                break;
            case Response_GradeMemberSet_Success:
                outPacket.encodeInt(guild.getId());
                outPacket.encodeInt(member.getCharID());
                outPacket.encodeByte(value1);
                break;
            case Response_NoticeSet_Success:
                outPacket.encodeInt(guild.getId());
                outPacket.encodeInt(chr.getId());
                outPacket.encodeString(stringArg);
                break;
            case Response_MarkSet_Success:
                outPacket.encodeInt(guild.getId());
                outPacket.encodeInt(chr.getId());
                byte[] customEmblem = guild.getCustomEmblem();
                outPacket.encodeByte((customEmblem != null && customEmblem.length > 0) ? 1 : 0);
                if (customEmblem != null && customEmblem.length > 0) {
                    outPacket.encodeShort(0);
                    outPacket.encodeByte(0);
                    outPacket.encodeShort(0);
                    outPacket.encodeByte(0);
                    outPacket.encodeInt(customEmblem.length);
                    for (byte b : customEmblem) {
                        outPacket.encodeByte(b);
                    }
                    outPacket.encodeInt(0);
                } else {
                    outPacket.encodeShort(guild.getMarkBg());
                    outPacket.encodeByte(guild.getMarkBgColor());
                    outPacket.encodeShort(guild.getMark());
                    outPacket.encodeByte(guild.getMarkColor());
                    outPacket.encodeInt(0);
                    outPacket.encodeInt(0);
                }
                break;
            case Response_ChangeLevelOrJob_Success:
                outPacket.encodeInt(guild.getId());
                outPacket.encodeInt(member.getCharID());
                outPacket.encodeInt(member.getLevel());
                outPacket.encodeInt(member.getJob());
                break;
            case Response_GuildNotify_LoginOrLogout:
                outPacket.encodeInt(guild.getId());
                outPacket.encodeInt(member.getCharID());
                outPacket.encodeByte(bool);
                if (!bool) {
                    outPacket.encodeFT(FileTime.currentTime());
                }
                outPacket.encodeByte(showBox);
                break;
            case Response_MaxMemberIncrease_Success:
                outPacket.encodeInt(guild.getId());
                outPacket.encodeInt(guild.getMaxMembers());
                break;
            case Response_CommitmentMemberSet_Success:
                outPacket.encodeInt(guild.getId());
                outPacket.encodeInt(member.getCharID());
                outPacket.encodeInt(member.getIGP());
                outPacket.encodeInt(member.getDayContribution());
                outPacket.encodeFT(member.getContributionIncTime());
                break;
            case Response_GGPSet_Success:
                outPacket.encodeInt(guild.getId());
                outPacket.encodeInt(guild.getGgp());
                break;
            case Response_IGPSet_Success:
                outPacket.encodeInt(guild.getId());
                outPacket.encodeInt(member.getCharID());
                outPacket.encodeInt(member.getIGP());
                break;
            case Response_PointIncrease_Success:
                outPacket.encodeInt(guild.getId());
                outPacket.encodeInt(guild.getHonorEXP());
                outPacket.encodeInt(guild.getLevel());
                outPacket.encodeInt(guild.getGgp());
                outPacket.encodeInt(0);
                break;
            case Response_GuildInvite_Unknown_1:
                outPacket.encodeInt(value1);
                break;
            case Response_GuildInvite_DisableInvited:
                outPacket.encodeString(stringArg);
                break;
            case Response_GuildInvite_HaveAnotherInvited:
                outPacket.encodeString(stringArg);
                break;
            case Response_SkillUse_Error:
                outPacket.encodeInt(0);
                break;
            case Response_SKillSet_BattleSkillReset:
                outPacket.encodeInt(guild.getId());
                outPacket.encodeInt(0);
                break;
            case Response_SkillSet_Success:
                outPacket.encodeInt(guild.getId());
                outPacket.encodeInt(skill.getSkillID());
                outPacket.encodeInt(value1); // nBuyCharacterID
                outPacket.encode(skill);
                break;
            case Response_BattleSkillOpen_Success:
                outPacket.encodeInt(guild.getBattleSp());
                break;
            case Response_Rank_Reflash:
                outPacket.encodeInt(guild.getRank());
                break;
            case Response_SkillSet_LevelSet_Unknown:
                outPacket.encodeByte(false);
                break;
            case Response_GuildRequest_Cancelled:
                outPacket.encodeInt(chr.getId());
                outPacket.encodeInt(guild.getId());
                break;
            case Response_ChangeGuildMaster_Success:
                outPacket.encodeInt(guild.getId());
                outPacket.encodeInt(value1); // oldLeader
                outPacket.encodeInt(value2); // newLeader
                outPacket.encodeByte(false);
                boolean bool = guild.getAllianceID() != 0;
                outPacket.encodeByte(bool);
                if (bool) {
                    outPacket.encodeInt(guild.getAllianceID());
                }
                break;
        }
    }

}
