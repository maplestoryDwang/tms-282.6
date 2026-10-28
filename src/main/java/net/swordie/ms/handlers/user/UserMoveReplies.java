package net.swordie.ms.handlers.user;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.packet.FieldPacket;
import net.swordie.ms.connection.packet.UserRemote;
import net.swordie.ms.life.movement.MovementInfo;
import net.swordie.ms.util.Position;

/**
 * Packets the shared user-move logic ({@link UserHandler#applyUserMove}) sends (strategy): the move broadcast to
 * the other users of the field and the teleport back into the field. {@link #GMS265} is the 265 behaviour; the TMS
 * 282.3 overlay passes its own builders (LP046D / LP04CC).
 */
public interface UserMoveReplies {

    /** Broadcast of {@code chr}'s path to the other users of its field. */
    OutPacket remoteMove(Char chr, MovementInfo path);

    /** Moves {@code chr} to {@code position} (used when it fell out of the field). */
    OutPacket teleport(Char chr, Position position);

    /** 265: {@link UserRemote#move} and {@link FieldPacket#teleport}. */
    UserMoveReplies GMS265 = new UserMoveReplies() {
        @Override
        public OutPacket remoteMove(Char chr, MovementInfo path) {
            return UserRemote.move(chr, path);
        }

        @Override
        public OutPacket teleport(Char chr, Position position) {
            return FieldPacket.teleport(position, chr);
        }
    };
}
