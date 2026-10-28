package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.packet.Tms282MobApplyCtrlRequest;
import net.swordie.ms.connection.packet.Tms282MobMoveRequest;
import net.swordie.ms.connection.protocol.codec.MovePathCodec;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.handlers.life.MobHandler;
import net.swordie.ms.life.Life;
import net.swordie.ms.util.DataPrinter;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Mob control packets of TMS 282.3 on the channel socket, after LP02EF ({@code when(IN_FIELD)}):
 * <ul>
 *     <li>CP0516 MOB_MOVE [verified opcode, static layout]: {@link Tms282MobMoveRequest} (path through the profile's
 *     {@link MovePathCodec}), then the 265 game logic {@link MobHandler#moveMob}: position and move action, mob
 *     skills, controller change, LP06AB MOB_CONTROL_ACK to the controller and LP06A0 MOB_MOVE to the rest of the
 *     field;</li>
 *     <li>CP0517 MOB_APPLY_CTRL [presumed]: {@link Tms282MobApplyCtrlRequest}, then {@link MobHandler#applyMobCtrl}.</li>
 * </ul>
 * The replies are built by the active profile's {@link net.swordie.ms.connection.protocol.codec.MobPacketEncoder}
 * (Tms282MobPackets). A malformed request is logged and dropped. A failure inside the 265 game logic is logged and
 * the packet dropped (the client keeps its mobs), as {@link Tms282FieldEntry} does for the 265 placement. CP0516
 * arrives several times a second per controlled mob, so only the first {@link #LOGGED_MOVES} moves, every
 * {@link #LOG_EVERY}th one and every move in the fallback layout are logged.
 */
final class Tms282MobHandlers {
    static final int LOGGED_MOVES = 5;
    static final int LOG_EVERY = 500;

    private final MovePathCodec paths;
    private final AtomicLong moves = new AtomicLong();

    Tms282MobHandlers(MovePathCodec paths) {
        this.paths = Objects.requireNonNull(paths, "paths");
    }

    void register(Tms282Routes routes) {
        routes.channel(Tms282MobMoveRequest.OPCODE, InHeader.MOB_MOVE).when(Tms282Guards.IN_FIELD)
                .handle("mob move -> LP06AB ack + LP06A0 to the field (265 MobHandler.moveMob)", this::onMobMove);
        routes.channel(Tms282MobApplyCtrlRequest.OPCODE, InHeader.MOB_APPLY_CTRL).when(Tms282Guards.IN_FIELD)
                .handle("mob apply control (265 MobHandler.applyMobCtrl)", this::onApplyCtrl);
    }

    // ---- CP0516 ----------------------------------------------------------------------------------------------

    private void onMobMove(Tms282LoginState session, int op, byte[] packet) {
        Tms282MobMoveRequest request;
        try {
            request = Tms282MobMoveRequest.parse(packet, paths);
        } catch (IllegalArgumentException e) {
            System.out.printf("[TMS282] REJECTED CP0516 len=%d: %s%n", packet.length, e.getMessage());
            return;
        }
        Char chr = session.fieldCharacter();
        if (chr.getField() == null) {
            System.out.println("[TMS282] CP0516 mob=" + request.objectId() + " dropped: character " + chr.getId()
                    + " has no server-side field");
            return;
        }
        try {
            MobHandler.moveMob(chr, request.toRequest());
        } catch (RuntimeException e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT_SKILL, e);
            System.out.println("[TMS282] FAILED CP0516 mob=" + request.objectId() + " character=" + chr.getId() + ": " + e);
            return;
        }
        long n = moves.incrementAndGet();
        if (n <= LOGGED_MOVES || n % LOG_EVERY == 0 || request.hasConditional()) {
            Life mob = chr.getField().getLifeByObjectID(request.objectId());
            System.out.println("[TMS282] CP0516 #" + n + " mob=" + request.objectId() + " moveId=" + request.moveId()
                    + " elements=" + request.path().getMovements().size()
                    + (request.hasConditional() ? " (layout with the conditional string)" : "")
                    + (mob == null ? " unknown mob -> LP0698" : " pos=" + mob.getPosition() + " controller="
                    + mob.getControllerID() + (mob.getControllerID() == chr.getId() ? " -> LP06AB + LP06A0" : "")));
        }
    }

    // ---- CP0517 ----------------------------------------------------------------------------------------------

    private void onApplyCtrl(Tms282LoginState session, int op, byte[] packet) {
        Tms282MobApplyCtrlRequest request;
        try {
            request = Tms282MobApplyCtrlRequest.parse(packet);
        } catch (IllegalArgumentException e) {
            System.out.printf("[TMS282] REJECTED CP0517 len=%d: %s%n", packet.length, e.getMessage());
            return;
        }
        Char chr = session.fieldCharacter();
        if (chr.getField() == null) {
            return;
        }
        try {
            MobHandler.applyMobCtrl(chr, request.objectId());
        } catch (RuntimeException e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT_SKILL, e);
            System.out.println("[TMS282] FAILED CP0517 mob=" + request.objectId() + " character=" + chr.getId() + ": " + e);
        }
    }
}
