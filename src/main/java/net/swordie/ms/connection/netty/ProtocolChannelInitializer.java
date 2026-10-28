package net.swordie.ms.connection.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import net.swordie.ms.client.Client;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.crypto.MapleCrypto;
import net.swordie.ms.connection.protocol.HandshakeStrategy;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolConfig;
import net.swordie.ms.connection.protocol.ProtocolProfile;
import net.swordie.ms.connection.protocol.SessionKind;
import net.swordie.ms.client.User;
import net.swordie.ms.connection.protocol.capture.CaptureInboundHandler;
import net.swordie.ms.connection.protocol.capture.CaptureOutboundHandler;
import net.swordie.ms.connection.protocol.capture.PacketCapture;
import net.swordie.ms.connection.protocol.capture.PacketCaptureFactory;
import net.swordie.ms.connection.protocol.capture.SendSecretScan;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static net.swordie.ms.connection.netty.NettyClient.CLIENT_KEY;

/**
 * Session setup shared by LoginAcceptor and ChannelAcceptor (template method; the protocol-specific steps
 * come from the profile's {@link HandshakeStrategy}):
 * <ol>
 *     <li>{@link GameSourceGate} (net.allow): a refused socket is closed before anything else happens;</li>
 *     <li>pipeline: PacketDecoder, [CaptureInboundHandler when {@code capture.recv}], PacketEncoder,
 *     [CaptureOutboundHandler when {@code capture.send}], ChannelHandler (the taps see plaintext: after
 *     decryption, before encryption);</li>
 *     <li>the plaintext hello, written while the channel has no client attribute (the encoder passes it through);</li>
 *     <li>client and crypto attributes;</li>
 *     <li>the strategy's unprompted packets, encrypted, and the optional first ping;</li>
 *     <li>{@link HandshakeStrategy#sessionStarted} (no-op for GMS 265; TMS 282.3 attaches its session state).</li>
 * </ol>
 * With the GMS 265 profile and capture off this is exactly what both acceptors did inline before.
 * With {@code capture.send} the channel also carries {@link PacketCapture#KEY}, so NettyClient can report packets
 * the OutboundGuard drops.
 */
public final class ProtocolChannelInitializer extends ChannelInitializer<SocketChannel> {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SessionKind kind;
    private final ProtocolProfile profile;
    private final ProtocolConfig config;
    private final PacketCaptureFactory captures;

    public ProtocolChannelInitializer(SessionKind kind) {
        this(kind, Protocol.active(), Protocol.config());
    }

    public ProtocolChannelInitializer(SessionKind kind, ProtocolProfile profile, ProtocolConfig config) {
        this(kind, profile, PacketCaptureFactory.standard(config, profile));
    }

    /** Checks pass their own capture factory (trace sink, writer). */
    public ProtocolChannelInitializer(SessionKind kind, ProtocolProfile profile, PacketCaptureFactory captures) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.profile = Objects.requireNonNull(profile, "profile");
        this.captures = Objects.requireNonNull(captures, "captures");
        this.config = captures.config();
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        // step 2: source allowlist (net.allow) before anything is added to the pipeline or written
        if (!GameSourceGate.standard().admit(ch, kind)) {
            ch.close();
            return;
        }
        byte[] siv = new byte[4];
        RANDOM.nextBytes(siv);
        byte[] riv = new byte[4];
        RANDOM.nextBytes(riv);
        initSession(ch, riv, siv);
        if (kind == SessionKind.LOGIN) {
            // step 2: a login socket that sends nothing for net.login.idleSec (default 60) is closed
            int idle = LoginIdleTimeout.configuredSeconds(net.swordie.ms.util.HotProperties.server());
            if (idle > 0) {
                ch.pipeline().addFirst("loginIdle", new LoginIdleTimeout(idle));
            }
        }
    }

    /**
     * Sets up one socket with explicit IVs (checks use an EmbeddedChannel).
     *
     * @param riv the server's receive IV (decoder)
     * @param siv the server's send IV (encoder)
     */
    public Client initSession(Channel ch, byte[] riv, byte[] siv) {
        ch.pipeline().addLast(new PacketDecoder(riv));
        PacketCapture capture = captures.open(kind, SendSecretScan.secrets(() -> accountSecrets(ch)));
        if (capture != null && config.captureRecv()) {
            ch.pipeline().addLast(new CaptureInboundHandler(capture));
        }
        ch.pipeline().addLast(new PacketEncoder(siv, profile.cipherPolicy().forSession(kind)));
        if (capture != null && config.captureSend()) {
            ch.pipeline().addLast(new CaptureOutboundHandler(capture, CLIENT_KEY));
            ch.attr(PacketCapture.KEY).set(capture);
        }
        ch.pipeline().addLast(new ChannelHandler(true));
        Client c = new Client(ch);
        HandshakeStrategy handshake = profile.handshake();
        writeHello(c, handshake.hello(kind, riv, siv));
        ch.attr(CLIENT_KEY).set(c);
        ch.attr(Client.CRYPTO_KEY).set(new MapleCrypto());
        for (OutPacket packet : handshake.afterHello(kind)) {
            c.write(packet);
        }
        if (handshake.pingOnConnect(kind)) {
            c.sendPing();
        }
        handshake.sessionStarted(c, kind);
        return c;
    }

    /** Password and SPW/PIC of the socket's account as stored (searched in sent packets by the capture). */
    private static List<String> accountSecrets(Channel ch) {
        NettyClient client = ch.attr(CLIENT_KEY).get();
        User user = client instanceof Client c ? c.getUser() : null;
        List<String> secrets = new ArrayList<>(2);
        if (user != null) {
            secrets.add(user.getPassword());
            secrets.add(user.getPic());
        }
        return secrets;
    }

    /**
     * The hello is a length-prefixed plaintext blob, not an opcode packet, so it bypasses the OutboundGuard;
     * the rest matches NettyClient.write (the client attribute is not set yet, so the encoder writes it raw).
     */
    private static void writeHello(Client c, OutPacket hello) {
        c.getCh().writeAndFlush(hello.getData());
        NettyClient.debug(c, hello);
    }
}
