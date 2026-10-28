package net.swordie.ms.world.migration;

import java.util.Objects;

/**
 * The identity a client presents when it enters a channel (MIGRATE_IN), as decoded by a {@link MigrateInDecoder}.
 *
 * @param worldId     world the client says it comes from (265: ignored; 282: must match the ticket)
 * @param userId      user (login account) ID the client says it is
 * @param characterId character to enter with
 * @param machineId   16-byte machine ID (logs only)
 */
public record MigrateInRequest(int worldId, int userId, int characterId, byte[] machineId) {
    public MigrateInRequest {
        machineId = Objects.requireNonNull(machineId, "machineId").clone();
    }

    @Override
    public byte[] machineId() {
        return machineId.clone();
    }

    @Override
    public String toString() {
        return "MigrateInRequest[world=" + worldId + ", user=" + userId + ", character=" + characterId + "]";
    }
}
