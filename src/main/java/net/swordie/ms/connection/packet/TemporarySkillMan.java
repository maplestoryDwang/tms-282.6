package net.swordie.ms.connection.packet;

import net.swordie.ms.client.character.skills.TemporarySkill;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.handlers.header.OutHeader;

import java.util.List;

public class TemporarySkillMan {

    public static OutPacket setTemporarySkillSet(int skillID, int index) {
        OutPacket outPacket = new OutPacket(OutHeader.SET_TEMPORARY_SKILL_SET);

        outPacket.encodeInt(index);
        outPacket.encodeInt(index);
        outPacket.encodeByte(0);
        outPacket.encodeInt(0);
        outPacket.encodeInt(1);
        TemporarySkill ts = new TemporarySkill(skillID);
        ts.encode(outPacket);

        return outPacket;
    }

    public static OutPacket setTemporarySkillSet(int index, List<TemporarySkill> tsList) {
        OutPacket outPacket = new OutPacket(OutHeader.SET_TEMPORARY_SKILL_SET);

        outPacket.encodeInt(index);
        if (index > 0) {
            outPacket.encodeInt(index);
            outPacket.encodeByte(0);
            outPacket.encodeInt(0);
            outPacket.encodeInt(tsList.size());
            for (TemporarySkill ts : tsList) {
                ts.encode(outPacket);
            }
        }

        return outPacket;
    }

    public static OutPacket usedTemporarySkill(int skillID, int coolTime) {
        OutPacket outPacket = new OutPacket(OutHeader.USED_TEMPORARY_SKILL);

        outPacket.encodeInt(skillID); // nSkillID
        outPacket.encodeInt(coolTime); // tCoolTime

        return outPacket;
    }

    public static OutPacket validateTemporarySkill(int index, TemporarySkill ts, boolean isEncode) {
        OutPacket outPacket = new OutPacket(OutHeader.VALIDATE_TEMPORARY_SKILL);

        outPacket.encodeInt(index);
        if (index > 0) {
            outPacket.encodeByte(isEncode);
            if (isEncode) {
                ts.encode(outPacket);
                outPacket.encodeByte(true);
            }
        }

        return outPacket;
    }

    public static OutPacket validateTemporarySkillSet(int index, List<TemporarySkill> tsList) {
        OutPacket outPacket = new OutPacket(OutHeader.VALIDATE_TEMPORARY_SKILL);

        outPacket.encodeInt(index);
        if (index > 0) {
            outPacket.encodeInt(index);
            outPacket.encodeInt(tsList.size());
            for (TemporarySkill ts : tsList) {
                ts.encode(outPacket);
            }
        }

        return outPacket;
    }

    public static OutPacket resetCoolTimeSkillSet(int index) {
        OutPacket outPacket = new OutPacket(OutHeader.RESET_COOLTIME_SKILL_SET);

        outPacket.encodeInt(index);

        return outPacket;
    }
}
