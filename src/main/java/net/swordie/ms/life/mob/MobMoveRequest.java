package net.swordie.ms.life.mob;

import net.swordie.ms.life.movement.MovementInfo;

import java.util.Objects;

/**
 * A decoded MOB_MOVE request (the controller's client moved a mob), independent of the wire layout. Produced by a
 * {@link MobMoveRequestDecoder}; {@link net.swordie.ms.handlers.life.MobHandler#moveMob} runs the 265 game logic
 * on it (position, skills, controller ack, broadcast).
 *
 * @param objectId   mob object id
 * @param moveId     the client's move id, echoed in MOB_CONTROL_ACK
 * @param attackInfo action / skill / target fields ({@code actionAndDirMask}, {@code action >> 1}, flip, skill id and
 *                   level from the long target info, ball targets, area attack times, the rectangle block)
 * @param action     raw action byte (action << 1 | flip)
 * @param path       the move path, decoded by the profile's {@link net.swordie.ms.connection.protocol.codec.MovePathCodec}
 */
public record MobMoveRequest(int objectId, short moveId, MobSkillAttackInfo attackInfo, byte action, MovementInfo path) {
    public MobMoveRequest {
        Objects.requireNonNull(attackInfo, "attackInfo");
        Objects.requireNonNull(path, "path");
    }

    /** Bit 0x10 of actionAndDirMask: the sender asks to take control of the mob. */
    public boolean changeController() {
        return (attackInfo.actionAndDirMask & 0x10) == 16;
    }
}
