package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.handlers.header.OutHeader;

public class SideScrollFlyShootingPacket {

    public static OutPacket gameInit(int maxHeart, int initHeart) {
        OutPacket outPacket = new OutPacket(OutHeader.FIELD_SIDESCROLLFYSHOOTING_GAME_INIT);

        outPacket.encodeInt(maxHeart);
        outPacket.encodeInt(initHeart);

        return outPacket;
    }

    public static OutPacket stageReady(int curStage, int stageType, int stageGameTime) {
        OutPacket outPacket = new OutPacket(OutHeader.FIELD_SIDESCROLLFYSHOOTING_STAGE_READY);

        outPacket.encodeInt(curStage);
        outPacket.encodeInt(stageType);
        outPacket.encodeInt(stageGameTime);
        outPacket.encodeByte(false);

        return outPacket;
    }

    public static OutPacket setMaxHeartBy2() {
        return new OutPacket(OutHeader.FIELD_SIDESCROLLFYSHOOTING_SET_MAX_HEART);
    }

    public static OutPacket setMaxHeartBy3andPlaySound() {
        return new OutPacket(OutHeader.FIELD_SIDESCROLLFYSHOOTING_SET_MAX_HEART_PLAY_SOUND);
    }

    public static OutPacket stageFail() {
        return new OutPacket(OutHeader.FIELD_SIDESCROLLFYSHOOTING_STAGE_FAIL);
    }

    public static OutPacket userInfo(int charID, int heart, int upgrade, int lethalAttack) {
        OutPacket outPacket = new OutPacket(OutHeader.FIELD_SIDESCROLLFYSHOOTING_GAME_INIT);

        outPacket.encodeInt(charID);
        outPacket.encodeInt(heart);
        outPacket.encodeInt(upgrade);
        outPacket.encodeInt(0);
        outPacket.encodeInt(lethalAttack);

        return outPacket;
    }

    public static OutPacket userLethalAttack(int charID, int skillID) {
        OutPacket outPacket = new OutPacket(OutHeader.FIELD_SIDESCROLLFYSHOOTING_GAME_INIT);

        outPacket.encodeInt(charID);
        outPacket.encodeInt(skillID);

        return outPacket;
    }
}
