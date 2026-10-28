package net.swordie.ms.world.migration;

import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.Tms282MigrateInRequest;

/**
 * Decodes a MIGRATE_IN body into a {@link MigrateInRequest} (strategy: one layout per protocol). The opcode has
 * already been read by the dispatcher; entering the field is {@link MigrationService}'s job.
 */
@FunctionalInterface
public interface MigrateInDecoder {
    /** @throws IllegalArgumentException when the body does not have this layout */
    MigrateInRequest decode(InPacket in);

    /** GMS 265: int world, int user, int character, 16-byte machine ID (the rest is left unread). */
    MigrateInDecoder GMS_265 = in -> {
        int worldId = in.decodeInt();
        int userId = in.decodeInt();
        int characterId = in.decodeInt();
        byte[] machineId = in.decodeArr(16);
        return new MigrateInRequest(worldId, userId, characterId, machineId);
    };

    /**
     * TMS 282.3 CP007E (Tms282MigrateInRequest, captured live): the whole packet is parsed and the body is consumed.
     */
    MigrateInDecoder TMS_282_3 = in -> {
        Tms282MigrateInRequest request = Tms282MigrateInRequest.parse(in.getData());
        int unread = in.getUnreadAmount();
        if (unread > 0) {
            in.decodeArr(unread);
        }
        return new MigrateInRequest(request.worldId(), request.userId(), request.characterId(), request.machineId());
    };
}
