package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.character.tms282.Tms282MyHomeRuntime;
import net.swordie.ms.connection.packet.Tms282ActorRpcRequest;
import net.swordie.ms.handlers.header.InHeader;

/** CField actor transport. UserCommon/familiar CP027D belongs to a different native network. */
final class Tms282ActorRpcRouter {
    private Tms282ActorRpcRouter() { }

    static void register(Tms282Routes routes) {
        routes.channel(0x06D2, InHeader.CP06D2).when(Tms282Guards.IN_FIELD)
                .handle("native field actor RPC -> owned private housing",(session,op,packet)->handle(session,packet));
    }

    static void handle(Tms282LoginState session, byte[] packet) {
        final Tms282ActorRpcRequest request;
        try {
            request = Tms282ActorRpcRequest.parse(packet);
        } catch (IllegalArgumentException malformed) {
            System.out.println("[TMS282] Rejected actor RPC envelope: " + malformed.getMessage());
            return;
        }
        if (request.opcode() != 0x06D2 || request.mode() != 1) return;
        for (var frame : request.frames()) {
            Tms282MyHomeRuntime.handleFrame(session.fieldCharacter(), request.opcode(), frame);
        }
    }
}
