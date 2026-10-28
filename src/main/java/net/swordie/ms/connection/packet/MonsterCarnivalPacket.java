package net.swordie.ms.connection.packet;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.MonsterCarnivalRanking;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.handlers.header.OutHeader;

import java.util.List;
import java.util.Set;

public class MonsterCarnivalPacket {

    // 2 x 2 : 980000501 - 980000601
    // 1 x 1 : 980001501 - 980001601
    public static OutPacket enter(boolean isMyTeamBlue, int cp, int personalTotalCP, int myTeamCScore, int enemyCScore) {
        OutPacket outPacket = new OutPacket(OutHeader.MONSTER_CARNIVAL_CARNIVAL_ENTER);

        outPacket.encodeByte(isMyTeamBlue);
        outPacket.encodeInt(cp);
        outPacket.encodeInt(personalTotalCP);
        outPacket.encodeInt(myTeamCScore);
        outPacket.encodeInt(enemyCScore);
        for (int i = 0; i < 22; i++) {
            outPacket.encodeByte(i);
        }

        return outPacket;
    }

    public static OutPacket personalCP(int cp, int totalCP) {
        OutPacket outPacket = new OutPacket(OutHeader.MONSTER_CARNIVAL_PERSONAL_CP);

        outPacket.encodeInt(cp);
        outPacket.encodeInt(totalCP);

        return outPacket;
    }

    public static OutPacket teamCScore(int totalRed, int totalBlue) {
        OutPacket outPacket = new OutPacket(OutHeader.MONSTER_CARNIVAL_TEAM_CSCORE);

        outPacket.encodeInt(totalRed);
        outPacket.encodeInt(totalBlue);

        return outPacket;
    }

    public static OutPacket spellCooltime(int skill1, int skill2, int skill3, int skill4) {
        OutPacket outPacket = new OutPacket(OutHeader.MONSTER_CARNIVAL_SPELL_COOLTIME);

        outPacket.encodeInt(skill1);
        outPacket.encodeInt(skill2);
        outPacket.encodeInt(skill3);
        outPacket.encodeInt(skill4);

        return outPacket;
    }

    public static OutPacket requestResult_Success(int max, int cur) {
        OutPacket outPacket = new OutPacket(OutHeader.MONSTER_CARNIVAL_RESULT_SUCCESS);

        outPacket.encodeInt(max);
        outPacket.encodeInt(cur);

        return outPacket;
    }

    public static OutPacket requestResult_failed(int max, int cur) {
        OutPacket outPacket = new OutPacket(OutHeader.MONSTER_CARNIVAL_RESULT_FAIL);

        outPacket.encodeInt(max);
        outPacket.encodeInt(cur);

        return outPacket;
    }

    public static OutPacket processForDeath(boolean isBlue, String name, boolean isNoneLeft) {
        OutPacket outPacket = new OutPacket(OutHeader.MONSTER_CARNIVAL_DEATH);

        outPacket.encodeByte(isBlue);
        // true = Maple Blue
        // fasle = Maple Red
        outPacket.encodeString(name);
        outPacket.encodeByte(isNoneLeft);
        // false = [%s]'s [%s] can no longer fight, and did not lose any CP because there is none left.
        // true = [%s]'s [%s] can no longer fight, and lost %d CP.

        return outPacket;
    }

    public static OutPacket showMemberOutMsg(boolean isPartyBoss, boolean isBlue, String newLeaderName) {
        OutPacket outPacket = new OutPacket(OutHeader.MONSTER_CARNIVAL_MEMBER_OUT);

        outPacket.encodeByte(isPartyBoss ? 7 : 0);
        // 7 = Since the leader of the Team [%s] quit the Monster Carnival%2C [%s] has been appointed as the new leader of the team.
        // else = [%s] of Team [%s] has quit the Monster Carnival.
        outPacket.encodeByte(isBlue);
        // true = Maple Blue
        // fasle = Maple Red
        outPacket.encodeString(newLeaderName);

        return outPacket;
    }

    public static OutPacket showGameResult(int type) {
        OutPacket outPacket = new OutPacket(OutHeader.MONSTER_CARNIVAL_SHOW_GAME_RESULT);

        outPacket.encodeByte(type);
        // 9 = You have won the Monster Carnival. Please wait as you'll be transported out of here shortly.
        // 10 = Unfortunately%2C you have lost the Monster Carnival. Please wait as you'll be transported out of here shortly.
        // 11 = Despite the Overtime%2C the carnival ended in a draw. Please wait as you'll be transported out of here shortly.
        // 12 = Monster Carnival has ended abruptly due to the opposing team leaving the game too early. Please wait as you'll be transported out of here shortly.

        return outPacket;
    }

    public static OutPacket updateRankInfo(Set<Char> chars) {
        OutPacket outPacket = new OutPacket(OutHeader.MONSTER_CARNIVAL_UPDATE_RANK_INFO);

        outPacket.encodeShort(chars.size());
        for (Char chr : chars) {
            if (chr.getMonsterCarnivalRanking() != null) {
                chr.getMonsterCarnivalRanking().encode(outPacket);
            }
        }

        return outPacket;
    }
}
