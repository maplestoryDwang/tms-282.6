package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.enums.ButterFlyType;
import net.swordie.ms.handlers.header.OutHeader;
import net.swordie.ms.life.mob.skill.ButterFly;
import net.swordie.ms.life.mob.skill.FairyDust;
import net.swordie.ms.life.mob.skill.MobSkillID;
import net.swordie.ms.util.Position;
import net.swordie.ms.util.Util;

import java.util.List;

public class LucidPacket {

    public static OutPacket butterFlyInit(List<ButterFly> butterflies) {
        OutPacket outPacket = new OutPacket(OutHeader.LUCID_BUTTERFLY_INIT);

        outPacket.encodeInt(0);
        outPacket.encodeInt(butterflies.size());
        for (ButterFly butterFly : butterflies) {
            outPacket.encodeInt(butterFly.getType());
            outPacket.encodePositionInt(butterFly.getPos());
        }

        return outPacket;
    }

    public static OutPacket butterflyAction(ButterFlyType mode, int typeId, Position pos, int count, int startDelay) {
        OutPacket outPacket = new OutPacket(OutHeader.LUCID_BUTTERFLY_ACTION);

        outPacket.encodeInt(mode.getVal());
        switch (mode) {
            case Add:
                outPacket.encodeInt(0); // unknown
                outPacket.encodeInt(typeId);
                outPacket.encodePositionInt(pos);
                break;
            case Move:
                outPacket.encodePositionInt(pos);
                break;
            case Attack:
                outPacket.encodeInt(count);
                outPacket.encodeInt(startDelay);
                break;
            case Erase:
                // Empty
                break;
        }

        return outPacket;
    }

    public static OutPacket butterflyAttack(int count, List<Integer> chars) {
        OutPacket outPacket = new OutPacket(OutHeader.LUCID_BUTTERFLY_ACTION);

        outPacket.encodeInt(2);
        outPacket.encodeInt(count);
        outPacket.encodeInt(chars.size());
        for (int i : chars) {
            outPacket.encodeInt(i);
        }

        return outPacket;
    }

    public static OutPacket createDragon(int phase, int posX, int posY, int createPosX, int createPosY, boolean isLeft) {
        OutPacket outPacket = new OutPacket(OutHeader.LUCID_DRAGON_INIT);

        outPacket.encodeInt(phase);
        outPacket.encodeInt(posX);
        outPacket.encodeInt(posY);
        outPacket.encodeInt(createPosX);
        outPacket.encodeInt(createPosY);
        outPacket.encodeByte(isLeft);

        return outPacket;
    }

    public static OutPacket doSkill(int slv) {
        return doSkill(slv, 0, null, null, 0, null, 0);
    }

    public static OutPacket doSkill(int slv,
                                    int pattern, Position pos,
                                    List<FairyDust> fairyDust,
                                    int startDelay, List<Integer> intervals,
                                    int charID) {
        OutPacket outPacket = new OutPacket(OutHeader.LUCID_DO_SKILL);

        outPacket.encodeInt(MobSkillID.Lucid.getVal());
        outPacket.encodeInt(slv);
        switch (slv) {
            case 1:
            case 2:
            case 3:
                // Flower Trap
                outPacket.encodeInt(pattern);
                outPacket.encodePositionInt(pos);
                outPacket.encodeByte(Util.succeedProp(50));
                break;
            case 4:
            case 10:
                // Fairy Dust:
                outPacket.encodeInt(fairyDust.size());
                for (FairyDust fd : fairyDust) {
                    outPacket.encodeInt(fd.getScale());
                    outPacket.encodeInt(fd.getCreateDelay());
                    outPacket.encodeInt(fd.getMoveSpeed());
                    outPacket.encodeInt(fd.getAngle());
                }
                break;
            case 5:
                // Laser Rain
                outPacket.encodeInt(startDelay);
                outPacket.encodeInt(intervals.size());
                for (int interval : intervals) {
                    outPacket.encodeInt(interval);
                }
                break;
            case 6:
            case 11:
                // Forced Teleport
                outPacket.encodeInt(charID);
                break;
            case 8:
                // Rush
                outPacket.encodeInt(0); // Etc/BossLucid.img/RushLucid/path%d/info
                break;
        }

        return outPacket;
    }

    public static OutPacket stainedGlassOnOff(boolean enable, List<String> tags) {
        OutPacket outPacket = new OutPacket(OutHeader.LUCID_STAINED_GLASS_ON_OFF);

        outPacket.encodeByte(enable);
        outPacket.encodeInt(tags.size());
        for (String name : tags) {
            outPacket.encodeString(name);
        }

        return outPacket;
    }

    public static OutPacket stainedGlassBreak(List<String> tags) {
        OutPacket outPacket = new OutPacket(OutHeader.LUCID_STAINED_GLASS_BREAK);

        outPacket.encodeInt(tags.size());
        for (String name : tags) {
            outPacket.encodeString(name);
        }

        return outPacket;
    }

    public static OutPacket statueStateChange(boolean showStatue, int gauge, boolean showEffect) {
        OutPacket outPacket = new OutPacket(OutHeader.LUCID_STATUE_STATE_CHANGE);

        outPacket.encodeInt(showStatue ? 1 : 0);
        if (showStatue) {
            outPacket.encodeByte(showEffect);
        } else {
            outPacket.encodeInt(gauge); // 0~3
            outPacket.encodeByte(showEffect);
        }

        return outPacket;
    }

    public static OutPacket setFlyingMode(boolean state) {
        OutPacket outPacket = new OutPacket(OutHeader.LUCID_SET_FLYING_MODE);

        outPacket.encodeByte(state);

        return outPacket;
    }

    public static OutPacket welcomeBarrage(int type) {
        return welcomeBarrage(type, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    public static OutPacket welcomeBarrage(int type, int angleRate, int speed) {
        return welcomeBarrage(type, 0, speed, angleRate, 0, 0, 0, 0, 0);
    }

    public static OutPacket welcomeBarrage(int type, int angleRate, int speed, int interval, int shotCount) {
        return welcomeBarrage(type, 0, speed, angleRate, interval, shotCount, 0, 0, 0);
    }

    public static OutPacket welcomeBarrage(int type,
                                           int angle, int speed,
                                           int angleRate, int interval, int shotCount,
                                           int angleDiff, int bulletAngleRate, int bulletSpeedRate) {
        OutPacket outPacket = new OutPacket(OutHeader.LUCID_WELCOME_BARRAGE);

        outPacket.encodeInt(type);
        switch (type) {
            case 0:
                // Shoot
                outPacket.encodeInt(angle);
                outPacket.encodeInt(speed);
                break;
            case 1:
            case 2:
                break;
            case 3:
                // Bi Direction Shoot
                outPacket.encodeInt(angleRate);
                outPacket.encodeInt(speed);
                outPacket.encodeInt(interval);
                outPacket.encodeInt(shotCount);
                break;
            case 4:
            case 5:
                // Spiral Shoot
                outPacket.encodeInt(angle);
                outPacket.encodeInt(angleRate);
                outPacket.encodeInt(angleDiff);
                outPacket.encodeInt(speed);
                outPacket.encodeInt(interval);
                outPacket.encodeInt(shotCount);
                outPacket.encodeInt(bulletAngleRate);
                outPacket.encodeInt(bulletSpeedRate);
                break;
        }

        return outPacket;
    }

    public static OutPacket setLastPhaseTypeForHardMode(int type) {
        OutPacket outPacket = new OutPacket(OutHeader.LUCID_LAST_PHASE_HARD_MODE);

        outPacket.encodeInt(type);

        return outPacket;
    }
}
