package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.Tms282MiniRoomRequest;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.handlers.social.RoomHandler;

import java.nio.charset.Charset;
import java.util.Objects;

/**
 * CP01F0 MINI_ROOM of TMS 282.3 on the channel socket after LP02EF ({@code when(IN_FIELD)}): the trading-room requests
 * ({@link Tms282MiniRoomRequest}) are rewritten to the 265 body and handed to the unchanged 265
 * {@link RoomHandler#handleMiniRoom}; its replies go out in 282 form through the profile codec
 * {@link net.swordie.ms.connection.protocol.codec.MiniRoomPacketEncoder} (Tms282MiniRoomPackets, LP097F). The 265
 * handler starts with {@code chr.dispose()} (EXCL_REQUEST), which the request-scoped unlock
 * ({@link net.swordie.ms.connection.packet.Tms282UnlockScope}) turns into one LP009D at the end of the route.
 * Types without a 265 counterpart (personal shop, hired merchant, mini games) are logged only.
 */
final class Tms282MiniRoomHandlers {
    private final Charset charset;

    Tms282MiniRoomHandlers(Charset charset) {
        this.charset = Objects.requireNonNull(charset, "charset");
    }

    void register(Tms282Routes routes) {
        routes.channel(Tms282MiniRoomRequest.OPCODE, InHeader.MINI_ROOM).when(Tms282Guards.IN_FIELD)
                .handle("mini room (trade) -> 265 RoomHandler (LP097F replies)", this::miniRoom);
    }

    private void miniRoom(Tms282LoginState session, int op, byte[] packet) {
        Tms282MiniRoomRequest request;
        try {
            request = Tms282MiniRoomRequest.parse(packet, charset);
        } catch (IllegalArgumentException e) {
            Tms282SocialHandlers.rejected(op, packet, e);
            return;
        }
        Char chr = session.fieldCharacter();
        byte[] body = request.toGms265Body();
        System.out.printf("[TMS282] CP01F0 mini room %s%s%n", request, body == null ? " (not ported)" : "");
        if (body != null) {
            Tms282SocialHandlers.guarded(chr, op, () -> RoomHandler.handleMiniRoom(chr, new InPacket(body)));
        }
    }
}
