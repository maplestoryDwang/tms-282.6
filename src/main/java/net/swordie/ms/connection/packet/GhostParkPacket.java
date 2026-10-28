package net.swordie.ms.connection.packet;

import net.swordie.ms.client.character.runestones.RuneStone;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.handlers.header.OutHeader;
import net.swordie.ms.world.field.HomingBullet;

import java.util.List;

public class GhostParkPacket {

    public static OutPacket runeUseAck(RuneStone runeStone) {
        OutPacket outPacket = new OutPacket(OutHeader.GHOST_PARK_RUNE_USE_ACK);

        outPacket.encodeInt(runeStone.getObjectId());

        return outPacket;
    }

    public static OutPacket runeAppear(List<RuneStone> runeStones) {
        OutPacket outPacket = new OutPacket(OutHeader.GHOST_PARK_RUNE_APPEAR);

        outPacket.encodeInt(runeStones.size());
        for (RuneStone runeStone : runeStones) {
            outPacket.encodeInt(runeStone.getRuneType().getVal());
            outPacket.encodePositionInt(runeStone.getPosition());
        }

        return outPacket;
    }

    public static OutPacket runeDisappear(RuneStone runeStone) {
        OutPacket outPacket = new OutPacket(OutHeader.GHOST_PARK_RUNE_DISAPPEAR);

        outPacket.encodeInt(runeStone.getObjectId());

        return outPacket;
    }

    public static OutPacket killedMobBonusEXPRateInfo(int EXPRate, int Count) {
        OutPacket outPacket = new OutPacket(OutHeader.GHOST_PARK_KILLED_MOB_BONUS_EXP_RATE_INFO);

        outPacket.encodeInt(EXPRate);
        outPacket.encodeInt(Count);

        return outPacket;
    }

    public static OutPacket curseLevelEXPRate(List<Integer> expBonuses, int curseExpRate) {
        OutPacket outPacket = new OutPacket(OutHeader.GHOST_PARK_CURSE_LEVEL_EXP_RATE);

        for (Integer expBonus : expBonuses) {
            outPacket.encodeInt(expBonus);
        }
        outPacket.encodeInt(curseExpRate);

        return outPacket;
    }

    public static OutPacket homingBulletCreate(List<HomingBullet> bullets) {
        OutPacket outPacket = new OutPacket(OutHeader.GHOST_PARK_HOMING_BULLET_CREATE);

        outPacket.encodeInt(bullets.size()); // nBulletCount
        for (HomingBullet homingBullet : bullets) {
            homingBullet.encode(outPacket);
        }

        return outPacket;
    }
}
