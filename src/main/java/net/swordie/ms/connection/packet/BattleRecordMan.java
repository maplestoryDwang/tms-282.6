package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.handlers.header.OutHeader;
import net.swordie.ms.life.mob.skill.BurnedInfo;
import net.swordie.ms.util.Util;

public class BattleRecordMan {

    public static OutPacket serverOnCalcRequestResult(boolean on) {
        OutPacket outPacket = new OutPacket(OutHeader.SERVER_ON_CALC_REQUEST_RESULT);

        outPacket.encodeByte(true);

        return outPacket;
    }

    public static OutPacket dotDamageInfo(BurnedInfo burnedInfo, int count) {
        OutPacket outPacket = new OutPacket(OutHeader.DOT_DAMAGE_INFO.getValue());

        outPacket.encodeLong(burnedInfo.getDamage());
        outPacket.encodeInt(count);

        outPacket.encodeByte(burnedInfo.getDotTickDamR() > 0);
        if (burnedInfo.getDotTickDamR() > 0) {
            outPacket.encodeInt(burnedInfo.getDotTickDamR());
        }
        outPacket.encodeInt(burnedInfo.getSkillId());

        return outPacket;
    }

    public static OutPacket killDamageInfo(int skillID, long damage) {
        OutPacket outPacket = new OutPacket(OutHeader.KILL_DAMAGE_INFO);

        outPacket.encodeInt(skillID);
        outPacket.encodeLong(damage);
        outPacket.encodeInt(0);

        return outPacket;
    }

    public static OutPacket battleDamageInfo(int mobIdx, int atkIdx, int skillID, long damageBonus) {
        OutPacket outPacket = new OutPacket(OutHeader.BATTLE_DAMAGE_INFO);

        outPacket.encodeInt(mobIdx);
        outPacket.encodeInt(atkIdx);
        outPacket.encodeInt(skillID);
        outPacket.encodeLong(damageBonus);
        outPacket.encodeInt(0);

        return outPacket;
    }

    public static OutPacket skillDamageLog(int time) {
        OutPacket outPacket = new OutPacket(OutHeader.SKILL_DAMAGE_LOG);

        outPacket.encodeInt(time); // (v2->m_DamageInfo.m_tEndTime - v2->m_DamageInfo.m_tStartTime) / 1000

        return outPacket;
    }

    public static OutPacket enemiesDefeatedUpdate() {
        return new OutPacket(OutHeader.ENEMINES_DEFEATED_UPDATE);
    }
}
