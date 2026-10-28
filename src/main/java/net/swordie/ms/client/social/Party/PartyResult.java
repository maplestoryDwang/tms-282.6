package net.swordie.ms.client.social.Party;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.Encodable;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.enums.social.Party.PartyType;

public class PartyResult implements Encodable {

    private PartyType type;
    private Party party;
    private PartyMember member;
    private Char chr;
    private int arg1;
    private boolean bool, bool2;
    private String str;

    public PartyResult(PartyType type) {
        this.type = type;
    }

    public static PartyResult request_Invite(Party party, Char chr) {
        PartyResult partyResult = new PartyResult(PartyType.Request_Invite);
        partyResult.party = party;
        partyResult.chr = chr;
        return partyResult;
    }

    public static PartyResult request_InviteIntrusion(Party party, Char chr) {
        PartyResult partyResult = new PartyResult(PartyType.Request_InviteIntrusion);
        partyResult.party = party;
        partyResult.chr = chr;
        return partyResult;
    }

    public static PartyResult request_Apply(Party party, PartyMember member) {
        PartyResult partyResult = new PartyResult(PartyType.Request_Apply);
        partyResult.party = party;
        partyResult.member = member;
        return partyResult;
    }

    public static PartyResult load(Party party) {
        PartyResult partyResult = new PartyResult(PartyType.Response_Load_Success);
        partyResult.party = party;
        return partyResult;
    }

    public static PartyResult create(Party party) {
        PartyResult partyResult = new PartyResult(PartyType.Response_Create_Success);
        partyResult.party = party;
        return partyResult;
    }

    public static PartyResult response_Leave_Success(Party party, PartyMember kickedMember, boolean partyStillExists, boolean expelled) {
        PartyResult partyResult = new PartyResult(PartyType.Response_Leave_Success);
        partyResult.party = party;
        partyResult.member = kickedMember;
        partyResult.bool = partyStillExists;
        partyResult.bool2 = expelled;
        return partyResult;
    }

    public static PartyResult response_Join_Success(Party party, String joinerName) {
        PartyResult partyResult = new PartyResult(PartyType.Response_Join_Success);
        partyResult.party = party;
        partyResult.str = joinerName;
        return partyResult;
    }

    public static PartyResult response_Join_Success_ForClient(Party party) {
        PartyResult partyResult = new PartyResult(PartyType.Response_Join_Success_ForClient);
        partyResult.party = party;
        return partyResult;
    }

    public static PartyResult response_Invite_Success(int arg1, String name) {
        PartyResult partyResult = new PartyResult(PartyType.Response_Invite_Success);
        partyResult.arg1 = arg1;
        partyResult.str = name;
        return partyResult;
    }

    public static PartyResult response_ChangeLeader_Success(PartyMember leader, int newLeaderID) {
        PartyResult partyResult = new PartyResult(PartyType.Response_ChangeLeader_Success);
        partyResult.member = leader;
        partyResult.arg1 = newLeaderID;
        return partyResult;
    }

    public static PartyResult response_UpdateDataMember(Char chr, PartyMember member, int type) {
        PartyResult partyResult = new PartyResult(PartyType.Response_UpdateDataMember);
        partyResult.chr = chr;
        partyResult.member = member;
        partyResult.arg1 = type;
        return partyResult;
    }

    public static PartyResult response_SetAppliable(boolean publicParty) {
        PartyResult partyResult = new PartyResult(PartyType.Response_SetAppliable);
        partyResult.bool = publicParty;
        return partyResult;
    }

    public static PartyResult response_Setting_Success(String newName, boolean publicParty, int settingType) {
        PartyResult partyResult = new PartyResult(PartyType.Response_Setting_Success);
        partyResult.bool = publicParty;
        partyResult.str = newName;
        partyResult.arg1 = settingType;
        return partyResult;
    }

    public static PartyResult response_Apply_Success(String name) {
        PartyResult partyResult = new PartyResult(PartyType.Response_Apply_Success);
        partyResult.str = name;
        return partyResult;
    }

    public static PartyResult msg(PartyType type) {
        return new PartyResult(type);
    }

    @Override
    public void encode(OutPacket outPacket) {
        outPacket.encodeByte(type.getVal());
        switch (type) {
            case Request_Invite:
            case Request_InviteIntrusion:
                outPacket.encodeInt(party.getId());
                outPacket.encodeInt(party.getPartyLeaderID());
                outPacket.encodeString(chr.getName());
                outPacket.encodeInt(chr.getLevel());
                outPacket.encodeInt(chr.getJob());
                outPacket.encodeInt(chr.getSubJob());
                break;
            case Request_Apply:
                outPacket.encodeInt(party.getId());
                outPacket.encodeInt(party.getPartyLeaderID());
                outPacket.encodeString(member.getCharName());
                outPacket.encodeInt(member.getLevel());
                outPacket.encodeInt(member.getJob());
                outPacket.encodeInt(member.getSubJob());
                break;
            case Response_Load_Success:
                outPacket.encodeByte(party != null);
                if (party != null) {
                    outPacket.encode(party);
                } else {
                    outPacket.encodeByte(0); // unk
                }
                break;
            case Response_Create_Success:
                outPacket.encodeInt(party.getId());
                outPacket.encodeByte(0); // unk
                outPacket.encodeInt(999999999);
                outPacket.encodeInt(999999999);
                outPacket.encodeInt(0);
                outPacket.encodeShort(0);
                outPacket.encodeShort(0);
                PartyMember pm = party.getPartyLeader();
                int charID = pm != null ? pm.getCharID() : 0;
                outPacket.encodeInt(charID);
                if (charID != 0) {
                    outPacket.encodeInt(charID);
                    outPacket.encodeString(pm.getCharName());
                    outPacket.encodeInt(pm.getJob());
                    outPacket.encodeInt(pm.getSubJob());
                    outPacket.encodeInt(pm.getLevel());
                    outPacket.encodeInt(party.getWorld().getWorldId());
                    outPacket.encodeInt(pm.isOnline() ? 1 : 0);
                    outPacket.encodeInt(pm.isOnline() ? pm.getChannel() - 1 : -2); // -1 for cash shop
                    outPacket.encodeByte(0);
                    outPacket.encodeByte(0);
                    outPacket.encodeInt(pm.getArcaneForce());
                    outPacket.encodeInt(pm.getSacredForce());
                    outPacket.encodeInt(pm.getDojangRank());
                    outPacket.encodeLong(pm.getDojangRankTime());
                    outPacket.encodeArr(pm.getPackedAvatar());
                }
                outPacket.encodeShort(30);
                outPacket.encodeString(party.getName(), 30);
                outPacket.encodeByte(party.isAppliable());
                outPacket.encodeInt(party.getPartyAcquisitionPermission() != null ? party.getPartyAcquisitionPermission().getVal() : 1);
                outPacket.encodeInt(-1); // BossPartyRecruiment
                outPacket.encodeInt(party.getPartyLeaderID());
                break;
            case Response_Leave_Success:
                if (party != null) {
                    outPacket.encodeInt(member.getCharID());
                    outPacket.encodeByte(bool); // bPartyExists
                    if (bool) {
                        outPacket.encodeByte(bool2); // bExpelled
                        outPacket.encodeString(member.getCharName());
                        party.encode(outPacket);
                    }
                } else {
                    outPacket.encodeInt(0);
                    outPacket.encodeByte(false); // bPartyExists
                }
                break;
            case Response_Join_Success:
                outPacket.encodeString(str); // sJoinerName
                outPacket.encodeByte(0);
                outPacket.encodeInt(0);
                party.encode(outPacket);
                break;
            case Response_Join_Success_ForClient: // 29
                outPacket.encodeByte(1);
                outPacket.encodeInt(party.getId());
                outPacket.encodeInt(0);
                break;
            case Response_Invite_Success:
                outPacket.encodeInt(arg1);
                outPacket.encodeString(str);
                break;
            case Response_ChangeLeader_Success:
                outPacket.encodeInt(member.getCharID());
                outPacket.encodeByte(arg1); // nReason
                break;
            case Response_UpdateDataMember:
                outPacket.encodeByte(1);
                outPacket.encodeInt(chr.getId());
                outPacket.encodeInt(arg1);
                member.encodeVal(outPacket, arg1);
                break;
            case Response_SetAppliable:
                outPacket.encodeByte(bool);
                break;
            case Response_Setting_Success:
                outPacket.encodeShort(30);
                outPacket.encodeString(str, 30);
                outPacket.encodeByte(bool);
                outPacket.encodeInt(arg1);
                break;
            case Response_Apply_Success:
                outPacket.encodeString(str);
                break;
        }
    }
}
