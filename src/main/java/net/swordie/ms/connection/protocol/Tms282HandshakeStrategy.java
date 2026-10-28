package net.swordie.ms.connection.protocol;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.packet.Tms282LoginSystemPackets;

import java.util.List;

/**
 * TMS 282.3: login hello 54 B, channel hello 18 B (channel-enter-2823.md 1.2), nothing else unprompted.
 * The official 6-packet pre-login initialization is a reply to CP0071, so it belongs to the login handler
 * overlay (phase 1), not to the connect handshake. No 265 initialization packet is ever sent, and no ping
 * on connect (the probe pings on its own timer, the channel ping is unproven).
 */
public final class Tms282HandshakeStrategy implements HandshakeStrategy {
    /** Phase-0 strategy: nothing happens when a session starts. */
    public static final Tms282HandshakeStrategy INSTANCE = new Tms282HandshakeStrategy((c, kind) -> { });

    private final SessionStartListener onSessionStart;

    /**
     * @param onSessionStart told about every new socket after its hello (the 282 login overlay attaches its
     *                       session state and keep-alive there)
     */
    public Tms282HandshakeStrategy(SessionStartListener onSessionStart) {
        this.onSessionStart = java.util.Objects.requireNonNull(onSessionStart, "onSessionStart");
    }

    @Override
    public void sessionStarted(net.swordie.ms.client.Client client, SessionKind kind) {
        onSessionStart.sessionStarted(client, kind);
    }

    @Override
    public OutPacket hello(SessionKind kind, byte[] recvIv, byte[] sendIv) {
        return kind == SessionKind.CHANNEL
                ? Tms282LoginSystemPackets.channelHello(recvIv, sendIv)
                : Tms282LoginSystemPackets.loginHello(recvIv, sendIv);
    }

    @Override
    public List<OutPacket> afterHello(SessionKind kind) {
        return List.of();
    }

    @Override
    public boolean pingOnConnect(SessionKind kind) {
        return false;
    }
}
