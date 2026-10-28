package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.packet.Tms282FieldPackets;
import net.swordie.ms.connection.packet.Tms282NpcMoveRequest;
import net.swordie.ms.connection.packet.Tms282NpcPackets;
import net.swordie.ms.connection.packet.Tms282UserMoveRequest;
import net.swordie.ms.connection.packet.Tms282UserRemotePackets;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.codec.MovePathCodec;
import net.swordie.ms.connection.protocol.codec.Tms282MovePathCodec;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.handlers.user.UserHandler;
import net.swordie.ms.handlers.user.UserMoveReplies;
import net.swordie.ms.life.Life;
import net.swordie.ms.life.movement.Movement;
import net.swordie.ms.life.movement.MovementInfo;
import net.swordie.ms.life.npc.Npc;
import net.swordie.ms.util.Position;
import net.swordie.ms.world.field.Field;

import java.util.function.Supplier;

/**
 * TMS 282.3 in-field movement (phase 2), channel socket, after SetField ({@link Tms282Guards#IN_FIELD}, dropped
 * before):
 * <ul>
 *     <li>CP0106 USER_MOVE ({@link Tms282UserMoveRequest}) runs the 265 move logic
 *     {@link UserHandler#applyUserMove} with 282 replies: LP046D REMOTE_MOVE to the rest of the field, LP04CC
 *     TELEPORT when the user fell out of the field;</li>
 *     <li>CP0545 NPC_MOVE ({@link Tms282NpcMoveRequest}, sent by the NPC's controller) moves the NPC as 265
 *     NpcHandler.handleNpcMove does and broadcasts LP0731 NPC_MOVE to the field.</li>
 * </ul>
 * Paths use the profile's {@link MovePathCodec} (the 282 codec until the profile registers one). A request that
 * does not parse is logged and dropped, not fatal: movement is continuous and a single unknown element type must
 * not end the session; the next CP0106 carries the position again. A failure of the 265 server-side logic is logged
 * the same way.
 */
final class Tms282MovementHandlers {
    private final Supplier<MovePathCodec> paths;
    private final UserMoveReplies replies;

    Tms282MovementHandlers() {
        this(Tms282MovementHandlers::profilePaths);
    }

    Tms282MovementHandlers(Supplier<MovePathCodec> paths) {
        this.paths = paths;
        this.replies = new UserMoveReplies() {
            @Override
            public OutPacket remoteMove(Char chr, MovementInfo path) {
                return Tms282UserRemotePackets.move(chr.getId(), path, paths.get());
            }

            @Override
            public OutPacket teleport(Char chr, Position position) {
                return Tms282FieldPackets.teleport(chr.getId(), position);
            }
        };
    }

    /** The active profile's path codec, the 282 one before the profile registers it. */
    static MovePathCodec profilePaths() {
        return Protocol.active().codecs().find(MovePathCodec.class).orElse(Tms282MovePathCodec.INSTANCE);
    }

    void register(Tms282Routes routes) {
        routes.channel(Tms282UserMoveRequest.OPCODE, InHeader.USER_MOVE).when(Tms282Guards.IN_FIELD)
                .handle("user move -> LP046D to the field", this::userMove);
        routes.channel(Tms282NpcMoveRequest.OPCODE, InHeader.NPC_MOVE).when(Tms282Guards.IN_FIELD)
                .handle("NPC move -> LP0731 to the field", this::npcMove);
    }

    UserMoveReplies replies() {
        return replies;
    }

    private void userMove(Tms282LoginState session, int op, byte[] packet) {
        Char chr = session.fieldCharacter();
        if (chr.getField() == null) {
            return;
        }
        Tms282UserMoveRequest request;
        try {
            request = Tms282UserMoveRequest.parse(packet, paths.get());
        } catch (IllegalArgumentException e) {
            System.out.printf("[TMS282] CHANNEL 0106 len=%d dropped: %s%n", packet.length, e.getMessage());
            return;
        }
        try {
            UserHandler.applyUserMove(chr, request.path(), replies);
        } catch (RuntimeException e) {
            serverSideFailure("0106", chr, e);
        }
    }

    /** A 265 server-side failure after a valid request: logged, the session stays (265 ChannelHandler logs too). */
    static void serverSideFailure(String op, Char chr, RuntimeException e) {
        System.out.printf("[TMS282] CHANNEL %s of character %d: server-side 265 logic failed: %s%n", op, chr.getId(), e);
    }

    private void npcMove(Tms282LoginState session, int op, byte[] packet) {
        Char chr = session.fieldCharacter();
        Field field = chr.getField();
        if (field == null) {
            return;
        }
        Tms282NpcMoveRequest request;
        try {
            request = Tms282NpcMoveRequest.parse(packet, paths.get());
        } catch (IllegalArgumentException e) {
            System.out.printf("[TMS282] CHANNEL 0545 len=%d dropped: %s%n", packet.length, e.getMessage());
            return;
        }
        try {
            OutPacket move = moveNpc(field.getLifeByObjectID(request.objectId()), request, paths.get());
            if (move != null) {
                field.broadcast(move);
            }
        } catch (RuntimeException e) {
            serverSideFailure("0545", chr, e);
        }
    }

    /**
     * 265 NpcHandler.handleNpcMove on the 282 request: only a moving NPC is updated (position, velocity, foothold
     * and action of each element) and answered; its LP0731 carries the path (an empty one at the NPC's position when
     * the controller sent none, so a moving NPC's reader always finds one).
     *
     * @return the LP0731 for the field, or null when {@code life} is not a moving NPC
     */
    static OutPacket moveNpc(Life life, Tms282NpcMoveRequest request, MovePathCodec codec) {
        if (!(life instanceof Npc npc) || !npc.isMove()) {
            return null;
        }
        MovementInfo path = request.path();
        if (path == null) {
            path = new MovementInfo(npc.getPosition(), npc.getVPosition());
        } else {
            for (Movement m : path.getMovements()) {
                m.applyTo(npc);
            }
        }
        return Tms282NpcPackets.move(request.objectId(), request.oneTimeAction(), request.chatIdx(), request.duration(),
                path, codec);
    }
}
