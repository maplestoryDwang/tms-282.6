package net.swordie.ms.connection.protocol;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.packet.Login;

import java.util.List;

/** The handshake LoginAcceptor/ChannelAcceptor sent before the protocol layer existed, unchanged. */
public final class Gms265HandshakeStrategy implements HandshakeStrategy {
    public static final Gms265HandshakeStrategy INSTANCE = new Gms265HandshakeStrategy();

    private Gms265HandshakeStrategy() {
    }

    @Override
    public OutPacket hello(SessionKind kind, byte[] recvIv, byte[] sendIv) {
        return Login.sendConnect(recvIv, sendIv, kind == SessionKind.CHANNEL);
    }

    @Override
    public List<OutPacket> afterHello(SessionKind kind) {
        if (kind == SessionKind.LOGIN) {
            return List.of(
                    Login.sendSecurityPacket(),
                    Login.sendServerValues(),
                    Login.sendServerEnvironment(),
                    Login.sendWzCheckList(),
                    Login.sendSecurityCodePacket(),
                    Login.sendSecurityCodePacket_Attach());
        }
        return List.of(
                Login.sendServerValues(),        // 69
                Login.sendServerEnvironment(),   // 70
                Login.sendWzCheckList(),         // 61
                Login.sendFileCheckList(),
                Login.sendSecurityCodePacket(),  // 45
                Login.sendSecurityCodePacket_Attach()); // 48
    }

    @Override
    public boolean pingOnConnect(SessionKind kind) {
        return true;
    }
}
