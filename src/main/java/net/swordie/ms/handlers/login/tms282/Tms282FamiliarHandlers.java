package net.swordie.ms.handlers.login.tms282;
import net.swordie.ms.client.jobs.sengoku.Tms282KannaOniController;
import net.swordie.ms.connection.packet.Tms282ActorRpcRequest;
import net.swordie.ms.handlers.header.InHeader;

/** UserCommon RPC envelope. Each feature checks its actor before decoding class-local method arguments. */
final class Tms282FamiliarHandlers {
    private final java.nio.charset.Charset charset;
    Tms282FamiliarHandlers() { this(Tms282LoginSettings.defaults().nameCharset()); }
    Tms282FamiliarHandlers(java.nio.charset.Charset charset) { this.charset=java.util.Objects.requireNonNull(charset); }
    void register(Tms282Routes routes) {
        routes.channel(0x027D,InHeader.CP027D).when(Tms282Guards.IN_FIELD)
                .handle("native UserCommon actor RPC -> owned Oni / familiar",(s,op,p)->handle(s,p));
    }
    private void handle(Tms282LoginState session, byte[] packet) {
        final Tms282ActorRpcRequest request;
        try {
            request = Tms282ActorRpcRequest.parse(packet);
        } catch (IllegalArgumentException malformed) {
            System.out.println("[TMS282] Rejected UserCommon RPC envelope: " + malformed.getMessage());
            return;
        }
        if (request.opcode() != 0x027D) return;
        for (var frame : request.frames()) {
            Tms282KannaOniController.handleFrame(session.fieldCharacter(), frame.actorId(),
                    frame.methodIndex(), frame.arguments());
        }
        // Keep the familiar batch intact; it classifies each actor before decoding class-local methods.
        Tms282FamiliarWindow.handle(session, packet, charset);
    }
}
