package net.swordie.ms.handlers.social;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.social.Friend.Friend;
import net.swordie.ms.client.social.Party.Party;
import net.swordie.ms.client.social.Party.PartyInvites;
import net.swordie.ms.client.social.Party.PartyMember;
import net.swordie.ms.client.social.Party.PartyResult;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.UserLocal;
import net.swordie.ms.connection.packet.UserRemote;
import net.swordie.ms.connection.packet.WvsContext;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.enums.social.Party.PartyType;
import net.swordie.ms.handlers.Handler;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.world.field.Field;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static net.swordie.ms.enums.ChatType.SystemNotice;

public class PartyHandler {

    @Handler(op = InHeader.PARTY_INVITABLE_SET)
    public static void handlePartyInvitableSet(Char chr, InPacket inPacket) {
        boolean isInvitable = inPacket.decodeByte() != 0;
        chr.setPartyInvitable(isInvitable);
    }

    @Handler(op = InHeader.PARTY_REQUEST)
    public static void handlePartyRequest(Char chr, InPacket inPacket) {
        byte type = inPacket.decodeByte();
        PartyType partyType = PartyType.getByVal(type);
        if (partyType == null) {
            System.out.printf("Unknown party request type %d.%n", type);
            chr.dispose();
            return;
        }
        Party party = chr.getParty();
        switch (partyType) {
            case Request_Create -> {
                String partyName = inPacket.decodeString();
                byte isPrivateParty = inPacket.decodeByte();
                int settingType = inPacket.decodeInt();

                if (party == null) {
                    if (JobConstants.isBeginnerJob(chr.getJob())) {
                        chr.chatPopup("你無法創建隊伍。");
                        chr.dispose();
                        return;
                    }
                    party = Party.createNewParty(isPrivateParty, partyName, chr.getWorld());
                    party.setPartyAcquisitionPermission(Party.PartyAcquisitionPermission.getByVal(settingType));
                    party.addPartyMember(chr);
                    chr.write(WvsContext.partyResult(PartyResult.create(party)));
                } else if (party.getPartyLeader().getCharID() == chr.getId() && !party.isEmpty()) {
                    party.setPartyAcquisitionPermission(Party.PartyAcquisitionPermission.getByVal(settingType));
                    chr.write(WvsContext.partyResult(PartyResult.create(party)));
                } else {
                    chr.write(WvsContext.partyResult(PartyResult.msg(PartyType.Response_Create_AlreadyJoined)));
                }
            }
            case Request_Leave -> {
                if (party != null) {
                    if (party.getMembers().size() == 1) {
                        party.disband();
                    } else {
                        party.leave(chr.getId());
                    }
                } else {
                    chr.write(WvsContext.partyResult(PartyResult.response_Leave_Success(null, null, false, false)));
                }
            }
            case Request_Kick -> {
                int expelID = inPacket.decodeInt();
                if (party == null) {
                    chr.write(WvsContext.partyResult(PartyResult.response_Leave_Success(null, null, false, false)));
                } else if (!party.isLeader(chr)) { // multiplayer-0930: only the leader may kick
                    chr.chatMessage("只有隊長可以將隊員移出隊伍。");
                } else if (expelID == chr.getId() || !party.hasPartyMember(expelID)) {
                    chr.chatMessage("找不到要移出的隊員。");
                } else {
                    party.expel(expelID);
                }
            }
            case Request_ChangeLeader -> {
                int newLeaderID = inPacket.decodeInt();
                // multiplayer-0930: party / leader checks (was an NPE without a party, and any member could take it)
                if (party == null) {
                    chr.chatMessage("你目前沒有隊伍。");
                    break;
                }
                if (!party.isLeader(chr)) {
                    chr.chatMessage("只有隊長可以轉移隊長。");
                    break;
                }
                if (newLeaderID == chr.getId()) {
                    break;
                }
                PartyMember newLeader = null;
                for (PartyMember partyMember : party.getOnlineMembers()) {
                    if (partyMember != null) {
                        if (partyMember.getCharID() == newLeaderID) {
                            newLeader = partyMember;
                            break;
                        }
                    }
                }
                // multiplayer-0930: the refusal goes to the requester only, as a chat line (282 has no translation for
                // these PARTY_RESULT types; type 58 even collides with ChangeLeader_Success)
                if (newLeader == null) {
                    chr.chatMessage("找不到該隊員，或對方不在線上。");
                    break;
                }
                if (newLeader.getFieldID() != chr.getFieldID()) {
                    chr.chatMessage("只能將隊長轉移給同一張地圖的隊員。");
                    break;
                }
                if (newLeader.getChannel() != chr.getClient().getChannel()) {
                    chr.chatMessage("只能將隊長轉移給同一個頻道的隊員。");
                    break;
                }
                party.setPartyLeaderID(newLeaderID);
                party.broadcast(UserLocal.chatMsg(SystemNotice, String.format("%s 已成爲新的隊長。", newLeader.getCharName())));
                party.updateFull();
            }
            case Request_Apply -> {
                int partyID = inPacket.decodeInt();
                if (party != null) {
                    chr.write(WvsContext.partyResult(PartyResult.msg(PartyType.Response_Join_AlreadyJoined)));
                    return;
                }
                if (chr.getInstance() != null) {
                    chr.chatPopup("你在隱藏地圖中，無法同意邀請。");
                    return;
                }
                party = chr.getWorld().getPartyByPartyID(partyID);
                if (party == null) {
                    chr.write(WvsContext.partyResult(PartyResult.msg(PartyType.Response_UnexpectedError)));
                    return;
                }
                if (party.isFull()) {
                    chr.write(WvsContext.partyResult(PartyResult.msg(PartyType.Response_Join_AlreadyFull)));
                    return;
                }
                PartyMember leader = party.getPartyLeader();
                Char leaderChr = leader == null ? null : chr.getWorld().getCharById(leader.getCharID());
                if (leaderChr != null) {
                    if (isTms282()) {
                        // multiplayer-0930: 265 sends the application as PARTY_RESULT type 8, which 282 shows as an
                        // invitation to the leader's own party (accepting it answered "already in a party"); the 282
                        // application reader is not traced -> the leader is told to invite the applicant.
                        leaderChr.chatMessage(SystemNotice, String.format("%s 申請加入你的隊伍，請使用邀請功能邀請他。", chr.getName()));
                        chr.chatMessage(SystemNotice, "已向隊長送出入隊申請，請等待隊長邀請。");
                    } else {
                        leaderChr.write(WvsContext.partyResult(PartyResult.request_Apply(party, leader)));
                    }
                }
            }
            case Request_Invite -> {
                String invitedName = inPacket.decodeString();
                int unk = inPacket.decodeInt();
                Char invited = chr.getWorld().getCharByName(invitedName);
                if (invited == chr) {
                    break;
                }
                if (invited == null) {
                    chr.write(WvsContext.partyResult(PartyResult.msg(PartyType.Response_InAnotherChannelOrBlockedUser)));
                    return;
                }
                if (net.swordie.ms.client.character.social.tms282.SocialGuards.refusePartyInvite(chr, invited)) { // social: block list
                    chr.write(WvsContext.partyResult(PartyResult.msg(PartyType.Response_InAnotherChannelOrBlockedUser)));
                    return;
                }
                if (party == null) {
                    party = Party.createNewParty((byte) 1, "Let's Party", chr.getWorld());
                    party.addPartyMember(chr);
                    chr.write(WvsContext.partyResult(PartyResult.create(party)));
                }
                //Character UnCheck invite Party Setting.
                PartyMember inviter = party.getPartyLeader();
                if (invited.getInstance() != null) {
                    chr.chatPopup("對方在隱藏地圖中，無法邀請。");
                } else if (!invited.isPartyInvitable()) {
                    chr.chatMessage(String.format("%s 當前拒絕所有隊伍邀請。", invitedName));
                } else if (invited.getParty() == null) {
                    PartyInvites.offer(invited.getId(), party.getId(), inviter != null ? inviter.getCharID() : chr.getId());
                    invited.write(WvsContext.partyResult(PartyResult.request_Apply(party, inviter)));
                    chr.chatMessage(SystemNotice, String.format("你已向 %s 發送隊伍邀請。", invitedName));
                } else {
                    chr.chatPopup("對方已經在隊伍中。");
                }
            }
            case Request_Setting -> {
                String newPartyName = inPacket.decodeString();
                boolean isPrivateParty = inPacket.decodeByte() == 1;
                int settingType = inPacket.decodeInt();
                if (party == null) {
                    return;
                }
                party.setAppliable(isPrivateParty);
                party.setName(newPartyName);
                party.setPartyAcquisitionPermission(Party.PartyAcquisitionPermission.getByVal(settingType));
                chr.write(WvsContext.partyResult(PartyResult.response_Setting_Success(party.getName(), !isPrivateParty, settingType)));
                party.updateFull();
            }
            default -> System.out.printf("Unhandled party request type %s | %s%n", partyType, type);
        }
        chr.dispose();
    }

    @Handler(op = InHeader.PARTY_RESULT)
    public static void handlePartyResult(Char chr, InPacket inPacket) {
        byte type = inPacket.decodeByte();
        PartyType partyType = PartyType.getByVal(type);
        if (partyType == null) {
            System.out.printf("Unknown party request result type %d%n", type);
            chr.dispose();
            return;
        }
        switch (partyType) {
            case Response_Invite_Success -> {
                int resultType = inPacket.decodeInt();
                int charID = inPacket.decodeInt();
                Char inviter = chr.getWorld().getCharById(charID);
                Party party = inviter == null ? null : inviter.getParty();
                if (party == null) {
                    PartyInvites.clear(chr.getId());
                    if (resultType == 5) {
                        chr.chatMessage("隊伍邀請已失效。");
                    }
                    break;
                }
                if (resultType == 0) {
                    chr.chatMessage(String.format("你被邀請加入 %s 的隊伍。", inviter.getName()));
                } else if (resultType == 4) {
                    PartyInvites.consume(chr.getId(), party.getId());
                    Char leaderChr = party.getPartyLeader() == null ? null : chr.getWorld().getCharById(party.getPartyLeader().getCharID());
                    if (leaderChr != null) {
                        leaderChr.chatPopup(String.format("%s 拒絕加入你的隊伍。", chr.getName()));
                    }
                } else if (resultType == 5) {
                    // multiplayer-0930: only with a pending invitation of this party (any client could join any party)
                    if (!PartyInvites.consume(chr.getId(), party.getId())) {
                        chr.chatMessage("隊伍邀請已失效。");
                        break;
                    }
                    if (chr.getParty() != null) {
                        chr.chatMessage("你已經在隊伍中。");
                        break;
                    }
                    if (party.isFull()) {
                        chr.chatMessage("隊伍人數已滿。");
                        break;
                    }
                    party.addPartyMember(chr); // broadcasts Join_Success itself (it was sent twice)
                    if (chr.getParty() == party) {
                        party.broadcast(UserRemote.receiveHP(chr));
                        for (Char member : party.getOnlineChars()) {
                            if (member != chr) {
                                chr.write(UserRemote.receiveHP(member));
                            }
                        }
                    }
                } else if (resultType >= 1 && resultType <= 3) {
                    // automatic refusals of the 282 client (2FD559D..2FD5593: busy / blocked / option)
                    PartyInvites.consume(chr.getId(), party.getId());
                    inviter.chatMessage(String.format("%s 目前無法接受隊伍邀請。", chr.getName()));
                }
            }
            /*case Response_Invite_AlreadyInvited -> {
                if (party.getPartyLeader() == null || party.getPartyLeader().getChr() == null) {
                    return;
                }
                party.getPartyLeader().getChr().chatPopup(String.format("你已邀請 '%s' 加入你的隊伍。", chr.getName()));
            }
            case Response_Invite_Rejected -> {
                if (party.getPartyLeader() == null || party.getPartyLeader().getChr() == null) {
                    return;
                }
                party.getPartyLeader().getChr().chatPopup(String.format("%s 拒絕加入你的隊伍。", chr.getName()));
            }
            case Response_Apply_Accepted -> {
                if (chr.getParty() != null) {
                    chr.write(WvsContext.partyResult(PartyResult.msg(PartyType.Response_Join_AlreadyJoined)));
                    return;
                }
                if (party.isFull()) {
                    chr.write(WvsContext.partyResult(PartyResult.msg(PartyType.Response_Join_AlreadyFull)));
                    return;
                }
                party.addPartyMember(chr);
                party.broadcast(WvsContext.partyResult(PartyResult.response_Join_Success(party, chr.getName())));
                party.broadcast(UserRemote.receiveHP(chr));
            }
            case Response_Apply_AlreadyAppliedByApplier -> party.broadcast(WvsContext.broadcastMsg(BroadcastMsg.popUpMessage(
                    String.format("你被邀請加入 %s 的隊伍。", chr.getName()))));
            case Response_Apply_Rejected -> party.broadcast(WvsContext.broadcastMsg(BroadcastMsg.popUpMessage(
                    String.format("%s 拒絕加入你的隊伍。", chr.getName()))));
            case Response_Apply_Success -> {
                if (party != null && party.getPartyLeader() != null) {
                    party.broadcast(WvsContext.partyResult(
                            PartyResult.response_Apply_Success(party.getPartyLeader().getCharName())));
                }
            }*/
            default -> System.out.printf("Unhandled party request result type %s | %s%n", partyType, type);
        }
        chr.dispose();
    }

    private static boolean isTms282() {
        return net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3;
    }

    @Handler(op = InHeader.PARTY_MEMBER_CANDIDATE_REQUEST)
    public static void handlePartyMemberCandidateRequest(Char chr, InPacket inPacket) {
        Field field = chr.getField();
        chr.write(WvsContext.partyMemberCandidateResult(field.getChars().stream()
                .filter(ch -> ch.isPartyInvitable() && !ch.equals(chr) && ch.getParty() == null)
                .collect(Collectors.toSet())));
    }

    @Handler(op = InHeader.PARTY_CANDIDATE_REQUEST)
    public static void handlePartyCandidateRequest(Char chr, InPacket inPacket) {
        //Disable Party Search until find problem get -38.
        Party party = chr.getParty();
        Set<Friend> friends = chr.getFriends();
        Field field = chr.getField();
        if (party != null) {
            //chr.write(WvsContext.partyCandidateResult(new HashSet<>()));
        } else {
            Set<Party> parties = new HashSet<>();
            for (Char ch : field.getChars()) {
                if (ch.getParty() != null && ch.getParty().isLeader(ch) && !ch.getParty().isAppliable()) {
                    parties.add(ch.getParty());
                }
            }
            //chr.write(WvsContext.partyCandidateResult(parties));
        }
    }

}
