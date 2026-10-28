package net.swordie.ms.connection.packet;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.union.UnionBoard;
import net.swordie.ms.client.character.union.UnionMember;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.handlers.header.OutHeader;

import java.util.Set;

public class UnionPacket {

    public static OutPacket unionResult(int coin,
                                        int rank,
                                        Set<Char> eligibleChars,
                                        Set<UnionMember> activeMembers,
                                        UnionMember mobileMember,
                                        UnionMember labMember,
                                        UnionMember labEnhancedMember,
                                        UnionMember unkMember) {
        OutPacket outPacket = new OutPacket(OutHeader.UNION_RESULT);

        outPacket.encodeInt(coin);
        outPacket.encodeByte(0);

        // CUnion::Decode
        outPacket.encodeInt(rank);
        outPacket.encodeInt(eligibleChars.size());
        for (Char chr : eligibleChars) {
            chr.createUnionMember().encode(outPacket, true);
        }
        outPacket.encodeInt(activeMembers.size());
        for (UnionMember unionMember : activeMembers) {
            unionMember.encode(outPacket);
        }
        outPacket.encodeByte(mobileMember != null);
        if (mobileMember != null) {
            mobileMember.encode(outPacket);
        }
        outPacket.encodeByte(labMember != null);
        if (labMember != null) {
            labMember.encode(outPacket);
        }
        outPacket.encodeByte(labEnhancedMember != null);
        if (labEnhancedMember != null) {
            labEnhancedMember.encode(outPacket);
        }
        outPacket.encodeByte(unkMember != null);
        if (unkMember != null) {
            unkMember.encode(outPacket);
        }
        for (int i = 0; i < 5; i++) {
            outPacket.encodeByte(0);
        }

        return outPacket;
    }


    public static OutPacket unionAssignResult(int rank,
                                              Set<Char> eligibleChars,
                                              UnionBoard activeBoard,
                                              UnionMember mobileMember,
                                              UnionMember labMember,
                                              UnionMember labEnhancedMember,
                                              UnionMember unkMember) {
        OutPacket outPacket = new OutPacket(OutHeader.UNION_ASSIGN_RESULT);

        outPacket.encodeInt(rank);

        outPacket.encodeInt(eligibleChars.size());
        for (Char chr : eligibleChars) {
            chr.createUnionMember().encode(outPacket, true);
        }
        outPacket.encodeInt(activeBoard.getActiveMembers().size());
        for (UnionMember unionMember : activeBoard.getActiveMembers()) {
            unionMember.encode(outPacket);
        }
        outPacket.encodeByte(mobileMember != null);
        if (mobileMember != null) {
            mobileMember.encode(outPacket);
        }
        outPacket.encodeByte(labMember != null);
        if (labMember != null) {
            labMember.encode(outPacket);
        }
        outPacket.encodeByte(labEnhancedMember != null);
        if (labEnhancedMember != null) {
            labEnhancedMember.encode(outPacket);
        }
        outPacket.encodeByte(unkMember != null);
        if (unkMember != null) {
            unkMember.encode(outPacket);
        }
        for (int i = 0; i < 5; i++) {
            outPacket.encodeByte(0);
        }

        return outPacket;
    }

    public static OutPacket unionAritfactChampionResult(Set<Char> eligibleChars) {
        OutPacket outPacket = new OutPacket(OutHeader.UNION_ARTIFACT_CHAMPION_REGISTER);

        outPacket.encodeByte(true);
        for (int i = 0; i < 8; i++) {
            outPacket.encodeInt(i);
        }
        outPacket.encodeInt(eligibleChars.size());
        for (Char chr : eligibleChars) {
            chr.createUnionMember().encode(outPacket);
        }
        outPacket.encodeInt(0);
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 8; j++) {
                outPacket.encodeInt(j);
            }
        }
        outPacket.encodeInt(0);

        return outPacket;
    }

    public static OutPacket unionCoin(int coin) {
        OutPacket outPacket = new OutPacket(OutHeader.UNION_COIN);

        outPacket.encodeInt(0); // Unknown
        outPacket.encodeInt(coin);

        return outPacket;
    }

    public static OutPacket setUnionRaidScore(long score) {
        OutPacket outPacket = new OutPacket(OutHeader.UNION_RAID_SCORE);

        outPacket.encodeLong(score);

        return outPacket;
    }

    public static OutPacket setUnionRaidCoinNum(int coin, boolean set) {
        OutPacket outPacket = new OutPacket(OutHeader.UNION_RAID_COIN);

        outPacket.encodeInt(coin);
        outPacket.encodeByte(set);

        return outPacket;
    }

    public static OutPacket showUnionRaidHpUI(int template, long curHP, long maxHP, int template2, long curHP2, long maxHP2) {
        OutPacket outPacket = new OutPacket(OutHeader.UNION_RAID_HP);

        outPacket.encodeInt(template);
        outPacket.encodeLong(curHP);
        outPacket.encodeLong(maxHP);
        outPacket.encodeInt(template2);
        outPacket.encodeLong(curHP2);
        outPacket.encodeLong(maxHP2);

        return outPacket;
    }
}
