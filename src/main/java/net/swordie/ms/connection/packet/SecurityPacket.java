package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.handlers.header.OutHeader;

public class SecurityPacket {

    public static OutPacket VerifyTimePulseNext(int NextType, boolean action) {
        OutPacket outPacket = new OutPacket(OutHeader.REQUEST_STATUS_CHECK);

        outPacket.encodeInt(NextType);
        outPacket.encodeByte(action);

        return outPacket;
    }

    public static OutPacket sendMigrateSecurity0() {
        OutPacket outPacket = new OutPacket(OutHeader.MIGRATE_SECURITY_CHECK_0);

        outPacket.encodeInt(0);
        outPacket.encodeShort(0);

        return outPacket;
    }

    public static OutPacket sendMigrateSecurity1() {
        OutPacket outPacket = new OutPacket(OutHeader.MIGRATE_SECURITY_CHECK_1);

        outPacket.encodeInt(0);

        return outPacket;
    }

    public static OutPacket sendMigrateSecurity2() {
        OutPacket outPacket = new OutPacket(OutHeader.MIGRATE_SECURITY_CHECK_2);

        outPacket.encodeInt(0);

        return outPacket;
    }

    public static OutPacket sendMigrateSecurity3() {
        OutPacket outPacket = new OutPacket(OutHeader.MIGRATE_SECURITY_CHECK_3);

        outPacket.encodeInt(0);

        return outPacket;
    }

    public static OutPacket sendGameKick(String reason) {
        OutPacket outPacket = new OutPacket(OutHeader.GAME_KICK);

        outPacket.encodeString(reason); // You have been kicked by Nexon Game Security for suspicious game or client behavior.

        return outPacket;
    }
}
