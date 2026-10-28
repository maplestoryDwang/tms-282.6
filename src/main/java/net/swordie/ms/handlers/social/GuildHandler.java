package net.swordie.ms.handlers.social;

import net.swordie.ms.Server;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.temp.TemporaryStatBase;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.client.social.Alliance.Alliance;
import net.swordie.ms.client.social.Alliance.AllianceResult;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.SkillStat;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.client.social.Guild.Guild;
import net.swordie.ms.client.social.Guild.GuildMember;
import net.swordie.ms.client.social.Guild.GuildRequestor;
import net.swordie.ms.client.social.Guild.GuildSkill;
import net.swordie.ms.client.social.Guild.BBSRecord;
import net.swordie.ms.client.social.Guild.BBSReply;
import net.swordie.ms.client.social.Guild.GuildBBSPacket;
import net.swordie.ms.connection.packet.UserRemote;
import net.swordie.ms.enums.TSIndex;
import net.swordie.ms.enums.social.Guild.GuildBBSType;
import net.swordie.ms.client.social.Guild.GuildResult;
import net.swordie.ms.enums.social.Guild.GuildType;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.FieldPacket;
import net.swordie.ms.connection.packet.WvsContext;
import net.swordie.ms.constants.GuildConstants;
import net.swordie.ms.enums.social.Alliance.AllianceType;
import net.swordie.ms.enums.UIType;
import net.swordie.ms.handlers.Handler;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.util.DataPrinter;
import net.swordie.ms.util.FileTime;
import net.swordie.ms.util.Util;
import net.swordie.ms.world.World;
import net.swordie.ms.world.field.Field;

import java.util.*;
import java.util.stream.Collectors;

import static net.swordie.ms.client.character.skills.SkillStat.*;
import static net.swordie.ms.client.character.skills.SkillStat.time;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;
import static net.swordie.ms.constants.GuildConstants.*;

public class GuildHandler {

    @Handler(op = InHeader.GUILD_REQUEST)
    public static void handleGuildRequest(Char chr, InPacket inPacket) {
        if(net.swordie.ms.client.social.Guild.Tms282GuildRuntime.enabled()) { chr.dispose();return; }
        byte type = inPacket.decodeByte();
        GuildType guildType = GuildType.getTypeByVal(type);
        if (guildType == null) {
            System.out.printf("Unknown guild request %d%n", type);
            return;
        }
        // Do not trust UI visibility as authorization; forged legacy packets reach this handler too.
        Guild actorGuild = chr.getGuild();
        int permission = switch (guildType) {
            case Request_GuildInvite -> 1;
            case Request_Greeting_Edit -> 2;
            case Request_GradeMemberSet -> 4;
            case Request_MarkSet -> 8;
            case Request_GuildKick -> 16;
            default -> 0;
        };
        boolean masterOnly = switch (guildType) {
            case Request_GradeNameSet, Request_GradePermissionSet, Request_GuildChangeMaster,
                 Request_JoinSetting, Request_GuildRemove -> true;
            default -> false;
        };
        if (permission != 0 && (actorGuild == null || !actorGuild.hasPermission(chr.getId(), permission))
                || masterOnly && (actorGuild == null || !actorGuild.isGuildMaster(chr))) {
            chr.dispose();
            return;
        }
        Guild guild;
        int nGuildID;
        String sGuildName = "";
        int nCharID;
        String sCharName = "";
        System.out.println("Guild Type: " + type + " - " + guildType);
        switch (guildType) {
            //region Guild:Load
            case Request_GuildLoad:
            case Request_GuildLoad_New:
                guild = chr.getGuild();
                if (guild == null) {
                    chr.chatPopup("發生未知錯誤。");
                    chr.dispose();
                    return;
                }
                chr.write(WvsContext.guildResult(GuildResult.response_GuildLoad_Success(guild)));
                break;
            case Response_GuildAcceptRequest:
                nGuildID = inPacket.decodeInt();
                guild = chr.getClient().getWorld().getGuildByID(nGuildID);
                if (guild == null) {
                    chr.chatPopup("發生未知錯誤。");
                    chr.dispose();
                    return;
                }
                if (chr.getGuild() != null) {
                    chr.chatPopup("發生未知錯誤。");
                    chr.dispose();
                    return;
                }
                if (guild.getMembers().size() >= guild.getMaxMembers()) {
                    chr.write(WvsContext.guildResult(GuildResult.msg(GuildType.Response_GuildInvite_AlreadyFull)));
                    return;
                }
                if (!guild.consumeInvitation(chr.getId(), System.currentTimeMillis())) {
                    chr.dispose();
                    return;
                }
                chr.setGuild(guild);
                chr.updateCharacterGuildToSQL();
                guild.addMember(chr);

                chr.write(WvsContext.guildResult(GuildResult.response_GuildInvite_Success(guild, guild.getMemberByCharID(chr.getId()))));
                guild.broadcast(WvsContext.guildResult(GuildResult.response_GuildInvite_Success(guild, guild.getMemberByCharID(chr.getId()))), chr);
                chr.write(WvsContext.guildResult(GuildResult.response_GuildLoad_Success(guild)));
                break;
            case Request_Greeting_Edit:
                String notice = inPacket.decodeString();
                guild = chr.getGuild();
                if (guild == null) {
                    chr.chatPopup("發生未知錯誤。");
                    chr.dispose();
                    return;
                }
                if (notice.length() > 255 || notice.indexOf('\0') >= 0) { chr.dispose(); return; }
                try {
                    guild.saveNotice(notice);
                } catch (java.sql.SQLException failure) {
                    chr.dispose();
                    return;
                }
                guild.broadcast(WvsContext.guildResult(GuildResult.response_NoticeSet_Success(guild, chr, notice)));
                break;
            case Request_GuildFindByGuildID:
                nGuildID = inPacket.decodeInt();
                guild = chr.getClient().getWorld().getGuildByID(nGuildID);
                if (guild == null) {
                    chr.chatPopup("發生未知錯誤。");
                    chr.dispose();
                    return;
                }
                chr.write(WvsContext.guildResult(GuildResult.response_GuildFind_Success(guild)));
                break;
            //endregion
            //region Guild:Create
            case Request_GuildNameCheck:
                sGuildName = inPacket.decodeString();
                if (chr.getGuild() != null || chr.getGuildID() != 0 || chr.getMoney() < 100000000
                        || sGuildName.isBlank() || sGuildName.indexOf('\0') >= 0) {
                    chr.dispose();
                    return;
                }
                if (Guild.checkName(sGuildName)) {
                    chr.write(WvsContext.guildResult(GuildResult.msg(GuildType.Response_GuildNameCheck_AlreadyUsed)));
                    return;
                }
                if (sGuildName.length() > 13 || sGuildName.matches(".*\\s.*")) {
                    chr.chatPopup("公會名稱不符合社區規範。");
                    return;
                }
                guild = new Guild();
                guild.setName(sGuildName);
                guild.setLevel(1);
                guild.setLeaderID(chr.getId());
                guild.updateGuildToSQL();
                chr.getWorld().addGuild(guild);

                chr.setGuild(guild); //Set Guild ID for character
                chr.updateCharacterGuildToSQL();

                guild = chr.getGuild(); //Need to set this or u r a Guild Master but NPC dont trust u. LOL
                guild.addMember(chr);
                guild.setWorldID(chr.getWorld().getWorldId());
                chr.write(WvsContext.guildResult(GuildResult.response_GuildCreate_Success(guild)));
                chr.deductMoney(100000000);
                break;
            //endregion
            //region Guild:Search
            case Request_GuildSearch:
                int mode = inPacket.decodeShort();
                String text = inPacket.decodeString();
                int option = inPacket.decodeShort();
                chr.write(WvsContext.guildSearchResult(chr, chr.getWorld().getGuilds().values(), mode, text, option));
                break;
            //endregion
            //region Guild:Invite
            case Request_GuildInvite:
                Char responseInvite = chr.getClient().getChannelInstance().getCharByName(inPacket.decodeString());
                if (responseInvite == null) {
                    chr.write(WvsContext.guildResult(GuildResult.msg(GuildType.Response_GuildInvite_UnknowUser)));
                    return;
                }
                if (responseInvite.getGuild() != null) {
                    chr.write(WvsContext.guildResult(GuildResult.msg(GuildType.Response_GuildInvite_AlreadyJoined)));
                    return;
                }
                if (responseInvite.getClient().getChannel() != chr.getClient().getChannel()) {
                    chr.write(WvsContext.guildResult(GuildResult.msg(GuildType.Response_GuildInvite_NotSameChannel)));
                    return;
                }
                for (Char tempChar : responseInvite.getAccount().getCharacters()) {
                    if (tempChar != null) {
                        if (tempChar.getGuild() != null && tempChar.getGuild().getId() == chr.getGuild().getId()) {
                            chr.chatPopup("該賬號已有角色在你的公會中。");
                            return;
                        }
                    }
                }
                if (!chr.getGuild().invite(chr.getId(), responseInvite.getId(), System.currentTimeMillis())) {
                    chr.dispose();
                    return;
                }
                responseInvite.write(WvsContext.guildResult(GuildResult.request_GuildInvite_New(chr)));
                break;
            //endregion
            //region Guild:Leave
            case Request_GuildLeave:
                nCharID = inPacket.decodeInt();
                sCharName = inPacket.decodeString();
                guild = chr.getGuild();
                if (guild != null && guild.isGuildMaster(chr)) { chr.dispose(); return; }
                if (chr.getId() != nCharID) {
                    chr.chatPopup("發生未知錯誤。");
                    chr.dispose();
                    return;
                }
                if (guild == null) {
                    // Temp Fix
                    if (chr.getGuildID() != 0) {
                        guild = Server.get().getWorld().getGuildByID(chr.getGuildID());
                        if (guild == null) {
                            chr.write(WvsContext.guildResult(GuildResult.msg(GuildType.Response_GuildKick_NotJoined)));
                            return;
                        }
                    } else {
                        chr.write(WvsContext.guildResult(GuildResult.response_GuildLeave_Success(guild, nCharID, sCharName)));
                        return;
                    }
                }
                chr.setGuildID(0);
                chr.setGuild(null);
                chr.updateCharacterGuildToSQL();
                guild.removeMember(chr.getId());
                chr.write(WvsContext.guildResult(GuildResult.response_GuildLeave_Success(guild, nCharID, sCharName)));
                guild.broadcast(WvsContext.guildResult(GuildResult.response_GuildLeave_Success(guild, nCharID, sCharName)));
                break;
            //endregion
            //region Guild:Kick
            case Request_GuildKick:
                nCharID = inPacket.decodeInt();
                sCharName = inPacket.decodeString();
                GuildMember kickedMember = actorGuild.getMemberByCharID(nCharID);
                GuildMember kickingMember = actorGuild.getMemberByCharID(chr.getId());
                if (kickedMember == null || kickingMember == null || nCharID == actorGuild.getLeaderID()
                        || kickedMember.getRank() <= kickingMember.getRank()) { chr.dispose(); return; }
                Char responseKick = Server.get().getWorld().getCharById(nCharID);
                guild = chr.getGuild();
                if (guild == null) {
                    // Temp Fix
                    if (chr.getGuildID() != 0) {
                        guild = Server.get().getWorld().getGuildByID(chr.getGuildID());
                        if (guild == null) {
                            chr.write(WvsContext.guildResult(GuildResult.msg(GuildType.Response_GuildKick_NotJoined)));
                            return;
                        }
                    } else {
                        chr.write(WvsContext.guildResult(GuildResult.msg(GuildType.Response_GuildKick_NotJoined)));
                        return;
                    }
                }
                if (responseKick == null) {
                    responseKick = Char.getCharDataByID(nCharID);
                    if (responseKick == null) {
                        chr.chatPopup("發生未知錯誤。");
                        chr.dispose();
                        return;
                    }
                    if (responseKick.getId() != nCharID) {
                        chr.chatPopup("發生未知錯誤。");
                        chr.dispose();
                        return;
                    }
                    responseKick.removeCharacterGuildToSQL();
                    // guild.removeMember(responseKick);
                    GuildMember.deleteGuildMemberFromSQL(nCharID);
                    guild.setMembers(GuildMember.getGuildMembersFromSQLByGuildID(guild.getId()));
                    guild.updateGuildToSQL();
                    guild.broadcast(WvsContext.guildResult(GuildResult.response_GuildKick_Success(guild, nCharID, sCharName)));
                    return;
                } else {
                    if (responseKick.getId() != nCharID) {
                        chr.chatPopup("發生未知錯誤。");
                        chr.dispose();
                        return;
                    }
                    responseKick.setGuild(null);
                    responseKick.updateCharacterGuildToSQL();
                    guild.removeMember(nCharID);
                    responseKick.write(WvsContext.guildResult(GuildResult.response_GuildKick_Success(guild, nCharID, sCharName)));
                    guild.broadcast(WvsContext.guildResult(GuildResult.response_GuildKick_Success(guild, nCharID, sCharName)));
                }
                break;
            //endregion
            //region Guild:Grade
            case Request_GradeNameSet:
                guild = chr.getGuild();
                if (guild == null) {
                    return;
                }
                String grade1Name = inPacket.decodeString();
                String grade2Name = inPacket.decodeString();
                String grade3Name = inPacket.decodeString();
                String grade4Name = inPacket.decodeString();
                String grade5Name = inPacket.decodeString();
                int gradeNameChanged = 0;
                if (!grade1Name.equals(guild.getGrade1())) {
                    guild.setGrade1(grade1Name);
                    gradeNameChanged += 1;
                }
                if (!grade2Name.equals(guild.getGrade2())) {
                    guild.setGrade2(grade2Name);
                    gradeNameChanged += 2;
                }
                if (!grade3Name.equals(guild.getGrade3())) {
                    guild.setGrade3(grade3Name);
                    gradeNameChanged += 3;
                }
                if (!grade4Name.equals(guild.getGrade4())) {
                    guild.setGrade4(grade4Name);
                    gradeNameChanged += 4;
                }
                if (!grade5Name.equals(guild.getGrade5())) {
                    guild.setGrade5(grade5Name);
                    gradeNameChanged += 5;
                }
                if (gradeNameChanged != 0) {
                    guild.updateGradeNameToSQL();
                    guild.broadcast(WvsContext.guildResult(GuildResult.response_GradeSetName_Success(guild, gradeNameChanged)));
                }
                break;
            case Request_GradePermissionSet:
                guild = chr.getGuild();
                if (guild == null) {
                    return;
                }
                inPacket.decodeInt(); // Guild Master
                int grade2 = inPacket.decodeInt();
                int grade3 = inPacket.decodeInt();
                int grade4 = inPacket.decodeInt();
                int grade5 = inPacket.decodeInt();
                int gradePermissionChanged = 0;
                if (grade2 != guild.getGrade2Permission()) {
                    guild.setGrade2Permission(grade2);
                    gradePermissionChanged += 2;
                }
                if (grade3 != guild.getGrade3Permission()) {
                    guild.setGrade3Permission(grade3);
                    gradePermissionChanged += 3;
                }
                if (grade4 != guild.getGrade4Permission()) {
                    guild.setGrade4Permission(grade4);
                    gradePermissionChanged += 4;
                }
                if (grade5 != guild.getGrade5Permission()) {
                    guild.setGrade5Permission(grade5);
                    gradePermissionChanged += 5;
                }
                if (gradePermissionChanged != 0) {
                    guild.updateGradeNameToSQL();
                    guild.broadcast(WvsContext.guildResult(GuildResult.response_GradeSetPermission_Success(guild, gradePermissionChanged)));
                }
                break;
            case Request_GradeMemberSet:
                int charID = inPacket.decodeInt();
                int rankUp = inPacket.decodeByte();
                guild = chr.getGuild();
                if (guild == null) {
                    chr.chatPopup("發生未知錯誤。");
                    chr.dispose();
                    return;
                }
                GuildMember guildMember = guild.getMemberByCharID(charID);
                if (guildMember == null) {
                    chr.chatPopup("發生未知錯誤。");
                    chr.dispose();
                    return;
                }
                GuildMember rankingMember = guild.getMemberByCharID(chr.getId());
                if (rankingMember == null || charID == guild.getLeaderID() || charID == chr.getId()
                        || rankUp < 2 || rankUp > 5 || rankUp <= rankingMember.getRank()
                        || guildMember.getRank() <= rankingMember.getRank()) { chr.dispose(); return; }
                guildMember.setRank(rankUp);
                guildMember.updateGuildMemberToSQL();
                guild.broadcast(WvsContext.guildResult(GuildResult.response_GradeSetMember_Success(guild, guildMember, rankUp)));
                break;
            case Request_MarkSet:
                guild = chr.getGuild();
                if (guild == null) {
                    chr.chatPopup("發生未知錯誤。");
                    chr.dispose();
                    return;
                }
                if (guild.getLevel() < 2 && guild.getGgp() >= 150000) {
                    guild.setGgp(guild.getGgp() - 150000);
                } else {
                    chr.chatMessage("[通知] GP 不足 150,000，或你的公會尚未達到 10 級。");
                    chr.dispose();
                    return;
                }
                byte isCustomImage = inPacket.decodeByte();
                if (isCustomImage == 0) {
                    guild.setMarkBg(inPacket.decodeShort());
                    guild.setMarkBgColor(inPacket.decodeByte());
                    guild.setMark(inPacket.decodeShort());
                    guild.setMarkColor(inPacket.decodeByte());
                    guild.updateGuildToSQL();
                    guild.setCustomEmblem(null);
                    guild.updateCustomEmblemToSQL(null);
                    guild.broadcast(WvsContext.guildResult(GuildResult.response_MarkSet_Success(guild, chr)));
                    guild.broadcast(WvsContext.guildResult(GuildResult.response_GuildLoad_Success(guild)));
                    for (Char gmChr : guild.getChars()) {
                        gmChr.getField().broadcast(UserRemote.guildMarkChanged(gmChr.getId(), guild), gmChr);
                    }
                } else {
                    if (guild.getLevel() < 10 && guild.getGgp() >= 150000) {
                        guild.setGgp(guild.getGgp() - 150000);
                    } else {
                        chr.chatMessage("[通知] GP 不足 150,000，或你的公會尚未達到 10 級。");
                        chr.dispose();
                        return;
                    }
                    int m = inPacket.decodeInt();
                    byte[] imgdata = new byte[m];
                    for (int n = 0; n < m; n++) {
                        imgdata[n] = inPacket.decodeByte();
                    }
                    guild.setMarkBg(0);
                    guild.setMarkBgColor(0);
                    guild.setMark(0);
                    guild.setMarkColor(0);
                    guild.updateGuildToSQL();
                    guild.setCustomEmblem(imgdata);
                    guild.updateCustomEmblemToSQL(imgdata);
                    guild.broadcast(WvsContext.guildResult(GuildResult.response_MarkSet_Success(guild, chr)));
                    guild.broadcast(WvsContext.guildResult(GuildResult.response_GuildLoad_Success(guild)));
                    for (Char gmChr : guild.getChars()) {
                        gmChr.getField().broadcast(UserRemote.guildMarkChanged(gmChr.getId(), guild), gmChr);
                    }
                }
                break;
            //endregion
            case Request_JoinSetting:
                chr.dispose();
                break;
            //region Guild:Skill
            case Request_BattleSkillOpen:
                guild = chr.getGuild();
                if (guild == null) {
                    chr.chatPopup("發生未知錯誤。");
                    chr.dispose();
                    return;
                }
                chr.write(WvsContext.guildResult(GuildResult.response_BattleSkillOpen_Success(guild)));
                break;
            case Request_SkillLevelSetUp:
                guild = chr.getGuild();
                if (guild == null) {
                    chr.chatPopup("發生未知錯誤。");
                    chr.dispose();
                    return;
                }
                int skillID = inPacket.decodeInt();
                boolean up = inPacket.decodeByte() != 0;
                if (up) {
                    //Check if skillID was memory edit.
                    if (!GuildConstants.isGuildContentSkill(skillID) && !GuildConstants.isGuildNoblesseSkill(skillID)) {
                        DataPrinter.send(DataPrinter.AUTOBAN_WARNING, String.format("Character %d tried to add an invalid guild skill (%d)", chr.getId(), skillID));
                        chr.write(WvsContext.guildResult(GuildResult.msg(GuildType.Response_SkillUse_Error)));
                        return;
                    }
                    //Get Skill Point Used.
                    int spentSp = guild.getSpentSp();
                    System.out.println(spentSp);
                    //Check if Skill is Guild Content Skill
                    if (GuildConstants.isGuildContentSkill(skillID)) {
                        //Check if Skill Point Use >= Skill Point Guild Have
                        if (spentSp >= guild.getLevel() * 2) {
                            DataPrinter.send(DataPrinter.AUTOBAN_WARNING, String.format("Character %d tried to add a guild skill without enough sp (spent %d, level %d).", chr.getId(), spentSp, guild.getLevel()));
                            chr.chatMessage("發生未知錯誤。");
                            chr.dispose();
                            return;
                        }
                        //Check Battle Sp Guild Have (= Guild Level + 4) -Battle SP use <= 0
                    } else if (guild.getBattleSp() - guild.getSpentBattleSp() <= 0) { // Noblesse
                        DataPrinter.send(DataPrinter.AUTOBAN_WARNING, String.format("Character %d tried to add a guild battle skill without enough sp (spent %d).", chr.getId(), guild.getSpentSp()));
                        chr.chatMessage("發生未知錯誤。");
                        chr.dispose();
                        return;
                    }
                    //Get Skill Info
                    SkillInfo skillInfo = SkillData.getSkillInfoById(skillID);
                    //Check if Skill Point Used < ??? Skill request Skill Point Used
                    if (spentSp < skillInfo.getReqTierPoint()) {
                        DataPrinter.send(DataPrinter.AUTOBAN_WARNING, String.format("Character %d tried to add a guild skill without enough sp spent (spent %d, needed %d).", chr.getId(), spentSp, skillInfo.getReqTierPoint()));
                        chr.chatMessage("發生未知錯誤。");
                        chr.dispose();
                        return;
                    }
                    for (Map.Entry<Integer, Integer> entry : skillInfo.getReqSkills().entrySet()) {
                        int reqSkillID = entry.getKey();
                        int reqSlv = entry.getValue();
                        GuildSkill requestGuildSkill = guild.getSkillById(reqSkillID);
                        if (requestGuildSkill.getLevel() < reqSlv) {
                            DataPrinter.send(DataPrinter.AUTOBAN_WARNING, String.format("Character %d tried to add a guild skill without having the required skill first (req id %d, needed %d, has %d).", chr.getId(), reqSkillID, reqSlv, requestGuildSkill == null ? 0 : requestGuildSkill.getLevel()));
                            chr.chatMessage("發生未知錯誤。");
                            chr.dispose();
                            return;
                        }
                    }
                    GuildSkill guildSkill = guild.getSkillById(skillID);
                    if (guildSkill == null) {
                        guildSkill = new GuildSkill();
                        guildSkill.setExpireDate(FileTime.MAX_TIME());
                        guildSkill.setBuyCharacterName(chr.getName());
                        guildSkill.setExtendCharacterName(chr.getName());
                        guildSkill.setSkillID(skillID);
                        guild.addGuildSkill(guildSkill);
                    }
                    if (guildSkill.getLevel() >= skillInfo.getMaxLevel()) {
                        chr.chatMessage("技能已達到最高等級。");
                        chr.dispose();
                        return;
                    }
                    guildSkill.setLevel((short) (guildSkill.getLevel() + 1));
                    guild.broadcast(WvsContext.guildResult(GuildResult.response_SkillSet_Success(guild, guildSkill, chr.getId())));
                    guildSkill.updateGuildSkillToSQL(guild.getId());
                    guild.addToBaseStatCache(skillID);
                    chr.dispose();
                } else {
                    GuildSkill guildSkill = guild.getSkillById(skillID);
                    if (guildSkill == null || guildSkill.getLevel() == 0) {
                        DataPrinter.send(DataPrinter.AUTOBAN_WARNING, String.format("Character %d tried to decrement a guild skill without that skill existing (id %d).", chr.getId(), skillID));
                        chr.chatMessage("發生未知錯誤。");
                        chr.dispose();
                        return;
                    }
                    if (guild.getGgp() < GuildConstants.GGP_FOR_SKILL_RESET) {
                        DataPrinter.send(DataPrinter.AUTOBAN_WARNING, String.format("Character %d tried to decrement a guild skill without having enough GGP (needed %d, has %d).", chr.getId(), GuildConstants.GGP_FOR_SKILL_RESET, guild.getGgp()));
                        chr.chatMessage("發生未知錯誤。");
                        chr.dispose();
                        return;
                    }
                    guild.setGgp(guild.getGgp() - GuildConstants.GGP_FOR_SKILL_RESET);
                    guildSkill.setLevel((short) (guildSkill.getLevel() - 1));
                    guild.broadcast(WvsContext.guildResult(GuildResult.response_SkillSet_Success(guild, guildSkill, chr.getId())));
                    guild.broadcast(WvsContext.guildResult(GuildResult.response_GGPSet_Success(guild)));
                    chr.write(WvsContext.incGPMessage(-GuildConstants.GGP_FOR_SKILL_RESET));
                    guildSkill.updateGuildSkillToSQL(guild.getId());
                    guild.addToBaseStatCache(skillID);
                    chr.dispose();
                }
                break;
            case Request_UseActiveSkill:
                guild = chr.getGuild();
                if (guild == null) {
                    chr.chatPopup("發生未知錯誤。");
                    chr.dispose();
                    return;
                }
                guildMember = guild.getMemberByCharID(chr.getId());
                if (guildMember == null) {
                    chr.chatPopup("發生未知錯誤。");
                    chr.dispose();
                    return;
                }
                skillID = inPacket.decodeInt();
                long usableTime = chr.getSkillCoolTimes().getOrDefault(skillID, Long.MIN_VALUE);
                if (usableTime > System.currentTimeMillis()) {
                    chr.chatMessage("該技能仍在冷卻中。");
                    return;
                }
                GuildSkill gs = guild.getSkillById(skillID);
                if (gs == null || gs.getLevel() == 0) {
                    DataPrinter.send(DataPrinter.AUTOBAN_WARNING, String.format("Character %d tried to use a guild skill without having it (id %d).", chr.getId(), skillID));
                    chr.chatMessage("發生未知錯誤。");
                    chr.dispose();
                    return;
                }
                int igpCost = GuildConstants.getIGPCostFromSkill(gs.getSkillID(), gs.getLevel());
                if (igpCost > guild.getMemberByCharID(chr.getId()).getIGP()) {
                    chr.chatPopup("發生未知錯誤。");
                    chr.dispose();
                    return;
                }
                guildMember.setIGP(guildMember.getIGP() - igpCost);
                guild.broadcast(WvsContext.guildResult(GuildResult.response_IGPSet_Success(guild, guildMember)));
                SkillInfo si = SkillData.getSkillInfoById(skillID);
                Option o = new Option();
                TemporaryStatManager tsm = chr.getTemporaryStatManager();
                switch (skillID) {
                    case RISE_MINIONS:
                        for (GuildMember gm : guild.getMembers()) {
                            Char member = Server.get().getWorld().getCharById(gm.getCharID());
                            if (member != null) {
                                if (member.getHP() > 0) {
                                    continue;
                                }
                                member.healHPMP();
                                member.chatMessage("[公會技能] 你已通過 " + chr.getName() + " 發動的公會技能恢復了 HP 和 MP。");
                            }
                        }
                        break;
                    case ON_MY_WAY:
                    case I_SUMMON_THEE:
                        charID = inPacket.decodeInt();
                        guildMember = guild.getMemberByCharID(charID);
                        if (guildMember == null) {
                            chr.chatMessage("不能對未知角色使用該技能。");
                            return;
                        }
                        Char targetChar = Server.get().getWorld().getCharById(guildMember.getCharID());
                        if (targetChar == null) {
                            chr.chatMessage("不能對未知角色使用該技能。");
                            return;
                        }
                        if (targetChar.getInstance() != null || chr.getInstance() != null) {
                            chr.chatMessage("無法傳送到角色 " + targetChar.getName() + " 所在地圖，因爲對方或你正在隱藏地圖中！");
                            return;
                        }
                        if (skillID == I_SUMMON_THEE) {
                            Field field = chr.getField();
                            if (field == null || !field.isTown()) {
                                chr.chatMessage("爲了 " + targetChar.getName() + " 的安全，你必須在城鎮地圖中使用。");
                                return;
                            }
                            targetChar.changeChannelAndWarp(chr.getClient().getChannel(), field.getId());
                        } else {
                            Field field = targetChar.getField();
                            if (field == null || !field.isTown()) {
                                chr.chatMessage(targetChar.getName() + " 必須在城鎮地圖中，才能安全傳送你。");
                                return;
                            }
                            chr.changeChannelAndWarp(targetChar.getClient().getChannel(), field.getId());
                        }
                        break;
                    case BOSS_SLAYERS:
                        o.nReason = skillID;
                        o.nValue = si.getValue(indieBDR, gs.getLevel());
                        o.tTerm = si.getValue(time, gs.getLevel());
                        tsm.sendStat(IndieBDR, o);
                        break;
                    case UNDETERRED:
                        o.nReason = skillID;
                        o.nValue = si.getValue(indieIgnoreMobpdpR, gs.getLevel());
                        o.tTerm = si.getValue(time, gs.getLevel());
                        tsm.sendStat(IndieIgnoreMobpdpR, o);
                        break;
                    case FOR_THE_GUILD:
                        o.nReason = skillID;
                        o.nValue = si.getValue(indieDamR, gs.getLevel());
                        o.tTerm = si.getValue(time, gs.getLevel());
                        tsm.sendStat(IndieDamR, o);
                        break;
                    case HARD_HITTER:
                        o.nReason = skillID;
                        o.nValue = si.getValue(indieCD, gs.getLevel());
                        o.tTerm = si.getValue(time, gs.getLevel());
                        tsm.sendStat(IndieCD, o);
                        break;
                    case SHARENIAN_DEMON_MOUNT:
                        TemporaryStatBase tsb = tsm.getTSBByTSIndex(TSIndex.RideVehicle);
                        if (tsm.hasStat(RideVehicle)) {
                            tsm.removeStat(RideVehicle);
                        }
                        tsb.setNOption(si.getVehicleId());
                        tsb.setROption(skillID);
                        tsm.sendStat(RideVehicle, tsb.getOption());
                        break;
                }
                chr.getSkillCoolTimes().put(skillID, System.currentTimeMillis() + 1000L * si.getValue(SkillStat.cooltime, gs.getLevel()));
                chr.write(WvsContext.incGPMessage(-igpCost));
                break;
            case Request_GuildChangeMaster:
                charID = inPacket.decodeInt();
                guild = chr.getGuild();
                if (guild == null) {
                    chr.chatPopup("發生未知錯誤。");
                    chr.dispose();
                    return;
                }
                GuildMember newLeader = guild.getMemberByCharID(charID);
                GuildMember oldLeader = guild.getMemberByCharID(chr.getId());
                if (newLeader == null || oldLeader == null || charID == chr.getId() || !newLeader.isOnline()) {
                    chr.chatPopup("發生未知錯誤。");
                    chr.dispose();
                    return;
                }
                oldLeader.setRank(newLeader.getRank());
                newLeader.setRank(1);
                guild.setLeaderID(charID);
                guild.setLeader(newLeader);
                guild.updateGuildToSQL();
                guild.broadcast(WvsContext.guildResult(GuildResult.response_ChangeGuildMaster_Success(guild, chr.getId(), charID)));
                break;
            case Request_GuildRequest_Success:
                acceptGuildRequest(chr, chr.getGuildID());
                break;
            default:
                System.out.printf("Unhandled guild request %s%n", guildType);
                break;

        }
    }

    @Handler(op = InHeader.GUILD_RESULT)
    public static void handleGuildResult(Char chr, InPacket inPacket) {
        if(net.swordie.ms.client.social.Guild.Tms282GuildRuntime.enabled()) { chr.dispose();return; }
        byte type = inPacket.decodeByte();
        GuildType guildType = GuildType.getTypeByVal(type);
        Char invited;
        if (guildType == null) {
            System.out.printf("Unknown guild result %d%n", type);
            return;
        }
        switch (guildType) {
            case Response_GuildInvite_Rejected:
                String invitedName = inPacket.decodeString();
                invited = chr.getClient().getChannelInstance().getCharByName(invitedName);
                if (invited != null) {
                    invited.write(WvsContext.guildResult(GuildResult.response_GuildInvite_Rejected(chr.getName())));
                }
                break;
        }
    }

    @Handler(op = InHeader.GUILD_JOIN_REQUEST)
    public static void handleGuildJoinRequest(Char chr, InPacket inPacket) {
        if(net.swordie.ms.client.social.Guild.Tms282GuildRuntime.enabled()) { chr.dispose();return; }
        int nGuildID = inPacket.decodeInt();
        acceptGuildRequest(chr, nGuildID);
    }

    private static void acceptGuildRequest(Char chr, int nGuildID) {
        Guild guild;
        if (!chr.isOnline()) {
            chr.chatPopup("發生未知錯誤。");
            chr.dispose();
            return;
        }
        guild = chr.getClient().getWorld().getGuildByID(nGuildID);
        if (guild == null) {
            chr.chatPopup("發生未知錯誤。");
            chr.dispose();
            return;
        }
        for (Char tempChar : chr.getAccount().getCharacters()) {
            if (tempChar != null) {
                if (tempChar.getGuild() != null && tempChar.getGuild().getId() == guild.getId()) {
                    chr.chatPopup("該賬號已有角色在你的公會中。");
                    chr.write(FieldPacket.closeUI(UIType.UI_GUILDBOARD));
                    return;
                }
            }
        }
        if (guild.getMembers().size() == guild.getMaxMembers()) {
            chr.chatPopup("該公會成員人數已達上限。");
            chr.dispose();
            return;
        }
        String sTimeLeave = chr.getQRValueByKey(GuildConstants.QR_GUILD_QUESTID, "time_leave");
        if (sTimeLeave != null && !sTimeLeave.isEmpty() && !sTimeLeave.equals("null")) {
            if (Long.parseLong(sTimeLeave) + 60 * 1000 > System.currentTimeMillis()) {
                chr.write(WvsContext.guildResult(GuildResult.msg(GuildType.Response_GuildRequest_LimitTime)));
                return;
            }
        }
        chr.createQuestWithQRValue(GuildConstants.QR_GUILD_QUESTID, String.format(GuildConstants.QR_GUILD_SYNTAX, guild.getId(), guild.getName(), null));
        GuildRequestor guildRequestor = new GuildRequestor(chr, guild);
        guild.addRequestors(guildRequestor);
        guild.broadcast(WvsContext.guildResult(GuildResult.response_GuildRequest_Success(chr, guild, guildRequestor)));
        chr.chatPopup("你已向公會 " + guild.getName() + " 發送加入申請。");
    }

    @Handler(op = InHeader.GUILD_JOIN_CANCEL_REQUEST)
    public static void handleGuildJoinCancelRequest(Char chr, InPacket inPacket) {
        if(net.swordie.ms.client.social.Guild.Tms282GuildRuntime.enabled()) { chr.dispose();return; }
        if (!chr.isOnline()) {
            chr.chatPopup("發生未知錯誤。");
            chr.dispose();
            return;
        }
        GuildRequestor guildRequestor = GuildRequestor.getGuildRequestorFromSQLByCharID(chr.getId());
        if (guildRequestor == null) {
            chr.chatPopup("發生未知錯誤。");
            chr.dispose();
            return;
        }
        String sGuildID = chr.getQRValueByKey(GuildConstants.QR_GUILD_QUESTID, "guild");
        if (sGuildID == null && !sGuildID.isEmpty()) {
            chr.chatPopup("發生未知錯誤。");
            chr.dispose();
            return;
        }
        int nGuildID = Integer.parseInt(sGuildID);
        Guild guild = chr.getClient().getWorld().getGuildByID(nGuildID);
        if (guild == null) {
            chr.chatPopup("發生未知錯誤。");
            chr.dispose();
            return;
        }
        chr.createQuestWithQRValue(GuildConstants.QR_GUILD_QUESTID, String.format(GuildConstants.QR_GUILD_SYNTAX, null, "", System.currentTimeMillis()));
        guild.removeRequestors(guildRequestor);
        guild.broadcast(WvsContext.guildResult(GuildResult.response_GuildRequest_Cancelled(chr, guild)), chr);
        chr.write(WvsContext.guildResult(GuildResult.response_GuildRequest_Cancelled(chr, guild)));
    }

    @Handler(op = InHeader.GUILD_JOIN_ACCEPT)
    public static void handleGuildJoinAccept(Char chr, InPacket inPacket) {
        if(net.swordie.ms.client.social.Guild.Tms282GuildRuntime.enabled()) { chr.dispose();return; }
        if (!chr.isOnline()) {
            chr.chatPopup("發生未知錯誤。");
            chr.dispose();
            return;
        }
        Guild guild = chr.getGuild();
        if (guild == null) {
            chr.chatPopup("發生未知錯誤。");
            chr.dispose();
            return;
        }
        Char requestor = null;
        GuildRequestor guildRequestor = null;
        byte nTotalRequestor = inPacket.decodeByte();
        for (int nRequestorIndex = 0; nRequestorIndex < nTotalRequestor; nRequestorIndex++) {
            int nRequestorID = inPacket.decodeInt();
            requestor = chr.getClient().getWorld().getCharById(nRequestorID);
            if (requestor == null) { //When Character Offline or Delete? Just for Sure.
                //TODO: Print smth?
                continue;
            }
            if (!requestor.isOnline()) {
                continue;
            }
            if (requestor.getGuild() != null) {
                continue;
            }
            guildRequestor = GuildRequestor.getGuildRequestorFromSQLByCharID(nRequestorID);
            if (guildRequestor == null) {
                //TODO: Print smth?
                continue;
            }
            if (guild.getMembers().size() >= guild.getMaxMembers()) {
                requestor.chatPopup("該公會成員人數已達上限。");
                requestor.dispose();
                return;
            }
            chr.createQuestWithQRValue(GuildConstants.QR_GUILD_QUESTID, String.format(GuildConstants.QR_GUILD_SYNTAX, null, "", null));
            guild.removeRequestors(guildRequestor);

            requestor.setGuild(guild);
            requestor.updateCharacterGuildToSQL();

            guild.addMember(requestor);
            requestor.write(WvsContext.guildResult(GuildResult.response_GuildInvite_Success(guild, guild.getMemberByCharID(requestor.getId()))));
            guild.broadcast(WvsContext.guildResult(GuildResult.response_GuildInvite_Success(guild, guild.getMemberByCharID(requestor.getId()))));
            requestor.write(WvsContext.guildResult(GuildResult.response_GuildLoad_Success(guild)));
            guild.broadcast(WvsContext.guildResult(GuildResult.response_GuildLoad_Success(guild)));
        }
    }

    @Handler(op = InHeader.GUILD_JOIN_REJECT)
    public static void handleGuildJoinReject(Char chr, InPacket inPacket) {
        if(net.swordie.ms.client.social.Guild.Tms282GuildRuntime.enabled()) { chr.dispose();return; }
        if (!chr.isOnline()) {
            chr.chatPopup("發生未知錯誤。");
            chr.dispose();
            return;
        }
        Guild guild = chr.getGuild();
        if (guild == null) {
            chr.chatPopup("發生未知錯誤。");
            chr.dispose();
            return;
        }
        Char requestor = null;
        GuildRequestor guildRequestor = null;
        byte nTotalRequestor = inPacket.decodeByte();
        for (int nRequestorIndex = 0; nRequestorIndex < nTotalRequestor; nRequestorIndex++) {
            int nRequestorID = inPacket.decodeInt();
            requestor = chr.getClient().getWorld().getCharById(nRequestorID);
            if (requestor == null) { //When Character Offline or Delete? Just for Sure.
                //TODO: Print smth?
                continue;
            }
            if (!requestor.isOnline()) {
                continue;
            }
            guildRequestor = GuildRequestor.getGuildRequestorFromSQLByCharID(nRequestorID);
            if (guildRequestor == null) {
                //TODO: Print smth?
                continue;
            }
            chr.createQuestWithQRValue(GuildConstants.QR_GUILD_QUESTID, String.format(GuildConstants.QR_GUILD_SYNTAX, null, "", System.currentTimeMillis()));
            guild.removeRequestors(guildRequestor);
            guild.broadcast(WvsContext.guildResult(GuildResult.response_GuildRequest_Cancelled(requestor, guild)));
            requestor.write(WvsContext.guildResult(GuildResult.response_GuildRequest_Cancelled(requestor, guild)));
        }
    }

    @Handler(op = InHeader.ALLIANCE_REQUEST)
    public static void handleAllianceRequest(Char chr, InPacket inPacket) {
        if(net.swordie.ms.client.social.Guild.Tms282GuildRuntime.enabled()) { chr.dispose();return; }
        byte type = inPacket.decodeByte();
        AllianceType at = AllianceType.getByVal(type);
        if (at == null) {
            System.out.printf("Unknown alliance request %d%n", type);
            return;
        }
        Guild guild = chr.getGuild();
        Alliance alliance = guild == null ? null : guild.getAlliance();
        Char other;
        Guild otherGuild;
        World world = chr.getClient().getWorld();
        GuildMember member = guild == null ? null : chr.getGuild().getMemberByCharID(chr.getId());
        GuildMember otherMember;
        if (!chr.isGuildMaster()) {
            return;
        }
        switch (at) {
            case Req_Withdraw:
                if (member.getAllianceRank() == 1) {
                    if (alliance.getGuilds().size() <= 1) {
                        alliance.broadcast(WvsContext.allianceResult(AllianceResult.withdraw(alliance, guild, false)));
                        alliance.removeGuild(guild);
                        //DatabaseManager.deleteFromDB(alliance);
                    } else {
                        alliance.getGuilds().removeIf(x -> x.getId() == guild.getId());
                        Guild newLeadingGuild = Util.getRandomFromCollection(alliance.getGuilds());
                        otherMember = newLeadingGuild.getGuildLeader();
                        otherMember.setAllianceRank(1);
                        alliance.broadcast(WvsContext.allianceResult(AllianceResult.changeMaster(alliance, member, otherMember)));
                        alliance.broadcast(WvsContext.allianceResult(AllianceResult.withdraw(alliance, guild, false)));
                        alliance.removeGuild(guild);
                    }
                } else {
                    alliance.broadcast(WvsContext.allianceResult(AllianceResult.withdraw(alliance, guild, false)));
                    alliance.removeGuild(guild);
                }
                break;
            case Req_Invite:
                String guildName = inPacket.decodeString();
                otherGuild = world.getGuildByName(guildName);
                if (otherGuild != null) {
                    other = world.getCharById(otherGuild.getLeaderID());
                    if (other != null) {
                        if (other.getGuild().getAlliance() == null) {
                            other.write(WvsContext.allianceResult(AllianceResult.inviteGuild(alliance, member)));
                        } else {
                            other.write(WvsContext.allianceResult(AllianceResult.msg(AllianceType.Res_InviteGuild_AlreadyInvited)));
                        }
                    } else {
                        chr.write(WvsContext.allianceResult(AllianceResult.msg(AllianceType.Res_Invite_Failed)));
                    }
                } else {
                    chr.write(WvsContext.allianceResult(AllianceResult.msg(AllianceType.Res_Invite_Failed)));
                }
                break;
            case Req_Load:
                chr.write(WvsContext.allianceResult(AllianceResult.loadDone(alliance)));
                chr.write(WvsContext.allianceResult(AllianceResult.loadGuildDone(alliance)));
                break;
            case Req_ChangeMaster:
                other = world.getCharById(inPacket.decodeInt());
                if (other != null) {
                    otherMember = other.getGuild().getMemberByCharID(other.getId());
                    member.setAllianceRank(2);
                    otherMember.setAllianceRank(1);
                    alliance.broadcast(WvsContext.allianceResult(AllianceResult.changeMaster(alliance, member, otherMember)));
                } else {
                    chr.chatMessage("該角色不在線。");
                }
                break;
            case Req_Kick:
                int otherID = inPacket.decodeInt();
                int kickedGuildID = inPacket.decodeInt();
                otherGuild = alliance.getGuildByID(kickedGuildID);
                if (otherGuild != null) {
                    alliance.broadcast(WvsContext.allianceResult(AllianceResult.withdraw(alliance, otherGuild, true)));
                    alliance.removeGuild(otherGuild);
                }
                break;
            case Req_SetGradeName:
                for (int i = 0; i < 5; i++) {
                    String gradeName = inPacket.decodeString();
                    if (gradeName.length() >= 4 && gradeName.length() <= 10) {
                        alliance.getGradeNames().set(i, gradeName);
                    }
                }
                alliance.broadcast(WvsContext.allianceResult(AllianceResult.setGradeName(alliance)));
                break;
            default:
                System.out.printf("Unhandled alliance request type %s%n", at.getVal());
        }
    }

    @Handler(op = InHeader.GUILD_BBS)
    public static void handleGuildBBS(Char chr, InPacket inPacket) {
        if(net.swordie.ms.client.social.Guild.Tms282GuildRuntime.enabled()) { chr.dispose();return; }
        Guild guild = chr.getGuild();
        if (guild == null) {
            chr.chatPopup("發生未知錯誤。");
            chr.dispose();
            return;
        }
        if (true) {
            chr.dispose();
            return;
        }
        byte nType = inPacket.decodeByte();
        GuildBBSType type = GuildBBSType.getByValue(nType);
        if (type == null) {
            System.out.printf("Unknown guild bbs type %s%n", nType);
            return;
        }
        switch (type) {
            case Request_RecordCreate:
                BBSRecord record = null;
                boolean bEdit = inPacket.decodeByte() != 0;
                if (bEdit) {
                    record = guild.getRecordByID(inPacket.decodeInt());
                    if (record == null || record.getCreatorID() != chr.getId()) {
                        chr.chatPopup("錯誤：未找到。");
                        chr.dispose();
                        return;
                    }
                }
                inPacket.decodeInt();
                //String sSubject = inPacket.decodeString();
                String sContent = inPacket.decodeString();
                //int nIcon = inPacket.decodeInt();
                if (bEdit) {
                    record.setSubject("");
                    record.setMsg(sContent);
                    record.setIcon(0);
                    record.setGuildID(guild.getId());
                    //Edit Record Just for Notice.
                    if (record.getIdForBbs() == 0) {
                        chr.write(WvsContext.guildBBSResult(GuildBBSPacket.response_RecordLoad(record)));
                        record.updateBBSRecordToSQL();
                        return;
                    }
                } else {
                    record = new BBSRecord(chr.getId(), "", sContent, FileTime.currentTime(), 0);
                    record.setGuildID(guild.getId());
                    record.updateBBSRecordToSQL();
                }
                if (bEdit) {
                    guild.setBbsNotice(record);
                    guild.updateGuildToSQL();
                } else {
                    guild.addBbsRecord(record);
                }
                record.updateBBSRecordToSQL();
                chr.write(WvsContext.guildBBSResult(GuildBBSPacket.response_RecordLoad(record)));
                break;
            case Request_RecordDelete:
                record = guild.getRecordByID(inPacket.decodeInt());
                if (record == null) {
                    return;
                }
                if (chr.getId() == record.getCreatorID()) {
                    guild.removeRecord(record);
                    record.deleteBBSRecordFromSQL();

                    if (record.getIdForBbs() == 0) {
                        guild.updateGuildToSQL();
                    }
                } else {
                    chr.chatPopup("錯誤：未找到。");
                }
                break;
            case Request_PagesLoad:
                int nPage = inPacket.decodeInt();
                List<BBSRecord> records = guild.getBbsRecords().stream().sorted(Comparator.comparingInt(BBSRecord::getIdForBbs)).collect(Collectors.toList());
                if (nPage != 0 && nPage * GuildConstants.GUILD_BBS_RECORDS_PER_PAGE >= records.size()) {
                    chr.chatMessage("沒有更多公會留言可以顯示。");
                    return;
                }
                int nStart = nPage * GuildConstants.GUILD_BBS_RECORDS_PER_PAGE;
                int nEnd = Math.min(nStart + GuildConstants.GUILD_BBS_RECORDS_PER_PAGE, records.size());
                List<BBSRecord> pageRecords = records.subList(nStart, nEnd);
                chr.write(WvsContext.guildBBSResult(GuildBBSPacket.response_PagesLoad(guild.getBbsNotice(), records.size(), pageRecords)));
                break;
            case Request_RecordLoad:
                record = guild.getRecordByID(inPacket.decodeInt());
                if (record == null) {
                    return;
                }
                chr.write(WvsContext.guildBBSResult(GuildBBSPacket.response_RecordLoad(record)));
                break;
            case Request_ReplyCreate:
                int recordID = inPacket.decodeInt();
                record = guild.getRecordByID(recordID);
                String sReply = inPacket.decodeString();
                if (record == null) {
                    return;
                }
                BBSReply replyCreate = new BBSReply(chr.getId(), FileTime.currentTime(), sReply);
                replyCreate.setRecordID(recordID);
                record.addReply(replyCreate);
                replyCreate.updateBBSReplyToSQL();
                chr.write(WvsContext.guildBBSResult(GuildBBSPacket.response_RecordLoad(record)));
                break;
            case Request_ReplyDelete:
                recordID = inPacket.decodeInt();
                record = guild.getRecordByID(recordID);
                int nReplyID = inPacket.decodeInt();
                if (record == null) {
                    return;
                }
                BBSReply replyDelete = record.getReplyById(nReplyID);
                record.removeReply(replyDelete);
                replyDelete.deleteBBSReplyFromSQL();
                chr.write(WvsContext.guildBBSResult(GuildBBSPacket.response_RecordLoad(record)));
                break;
            default:
                System.out.printf("Unhandled guild bbs type %s%n", type);
        }
    }

}
