import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.*;
import net.swordie.ms.ServerConstants;
import net.swordie.ms.client.Client;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.crypto.MapleCrypto;
import net.swordie.ms.connection.crypto.Tms282ServerCipher;
import net.swordie.ms.connection.netty.ProtocolChannelInitializer;
import net.swordie.ms.connection.packet.Tms282LoginSystemPackets;
import net.swordie.ms.connection.protocol.*;
import net.swordie.ms.handlers.login.tms282.*;
import net.swordie.ms.world.migration.MigrationRegistry;

// Run from server/265. The phase-1 282 profile's socket setup through the real ProtocolChannelInitializer
// (EmbeddedChannel, fixed IVs): channel hello 18 B / login hello 54 B, no 265 initialization packet, the 282
// session state attached per socket kind, server frames decryptable with Tms282ServerCipher (channel: byte +
// sendIV[0]; login: AES), and a client AES frame decoded and dispatched to the 282 channel overlay.
class Tms282ChannelHandshakeCheck {
    static int checks;
    static final Path DATA = Path.of("data", "protocol", "tms282.3");
    static final byte[] RIV = {0x11, 0x22, 0x33, 0x44};
    static final byte[] SIV = {(byte) 0x91, 0x52, 0x63, 0x74};

    public static void main(String[] args) {
        MapleCrypto.initialize(ServerConstants.version);
        Tms2823Profile profile = Tms282Profiles.create(DATA,
                new Tms282LoginContext(Tms282LoginSettings.defaults(), Tms282WorldCatalog.standard(6), MigrationRegistry::new));
        ProtocolConfig config = new ProtocolConfig(ProtocolVersion.TMS_282_3, Path.of("data", "protocol"), false, Path.of("logs", "capture"));
        Protocol.withProfile(profile, () -> {
            channelSocket(profile, config);
            loginSocket(profile, config);
        });
        System.out.println("PASS " + checks + " checks; 282 channel hello 18 B + byte+IV[0] frames, login hello 54 B + AES frames,"
                + " session kinds, client AES frame dispatched to the channel overlay");
        System.exit(0);
    }

    static void channelSocket(Tms2823Profile profile, ProtocolConfig config) {
        EmbeddedChannel ch = new EmbeddedChannel();
        Client c = new ProtocolChannelInitializer(SessionKind.CHANNEL, profile, config).initSession(ch, RIV.clone(), SIV.clone());
        byte[] hello = bytes(ch.readOutbound());
        check(hello.length == 18 && Arrays.equals(hello, Tms282LoginSystemPackets.channelHello(RIV, SIV).getData()),
                "channel hello is the 18-byte 282 channel hello");
        check(ch.readOutbound() == null, "no 265 initialization packet and no ping after the channel hello");
        check(Tms282LoginState.of(c).isChannel() && !Tms282LoginState.of(c).awaitingPong(), "channel session state attached (no channel ping by default)");

        byte[] iv = SIV.clone();
        for (OutPacket p : List.of(Tms282LoginSystemPackets.ping(), Tms282LoginSystemPackets.serverTime())) {
            byte[] plain = p.getData();
            c.write(p);
            byte[] frame = bytes(ch.readOutbound());
            byte[] header = Arrays.copyOf(frame, 4);
            byte[] body = Arrays.copyOfRange(frame, 4, frame.length);
            check(Arrays.equals(header, MapleCrypto.getFrameHeader(plain.length, iv)), "channel frame header from the send IV");
            check(Arrays.equals(Tms282ServerCipher.decryptChannel(body, iv), plain), "Tms282ServerCipher.decryptChannel recovers LP"
                    + String.format("%04X", p.getHeader()));
            check(body[0] == (byte) (plain[0] + iv[0]), "channel body is byte + sendIV[0]");
            iv = MapleCrypto.getNewIv(iv);
        }

        // Client -> server stays AES on the channel socket: a malformed CP007E is decoded and makes the 282
        // channel overlay end the session (probe: "Malformed CP007E; disconnecting").
        byte[] cp007e = {0x7E, 0x00, 1, 2, 3};
        ch.writeInbound(io.netty.buffer.Unpooled.wrappedBuffer(clientFrame(cp007e, RIV.clone())));
        ch.runPendingTasks();
        check(!ch.isOpen(), "client AES frame decoded, routed to CP007E, malformed body closes the channel socket");
    }

    static void loginSocket(Tms2823Profile profile, ProtocolConfig config) {
        EmbeddedChannel ch = new EmbeddedChannel();
        Client c = new ProtocolChannelInitializer(SessionKind.LOGIN, profile, config).initSession(ch, RIV.clone(), SIV.clone());
        byte[] hello = bytes(ch.readOutbound());
        check(hello.length == 54 && Arrays.equals(hello, Tms282LoginSystemPackets.loginHello(RIV, SIV).getData()),
                "login hello is the 54-byte 282 login hello");
        check(ch.readOutbound() == null, "no 265 initialization packet on the login socket");
        check(!Tms282LoginState.of(c).isChannel(), "login session state attached");
        OutPacket p = Tms282LoginSystemPackets.serverTime();
        byte[] plain = p.getData();
        c.write(p);
        byte[] frame = bytes(ch.readOutbound());
        byte[] body = Arrays.copyOfRange(frame, 4, frame.length);
        check(Arrays.equals(new MapleCrypto(282).crypt(body, SIV.clone(), false), plain), "login frame body is the AES stream");
        ch.close();
        check(!ch.isOpen(), "closed");
    }

    /** A client frame as the 282 client sends it (LoginProtocolCheck.send): header from the server's receive IV, AES body. */
    static byte[] clientFrame(byte[] plain, byte[] iv) {
        int sequence = ((iv[2] & 255) | ((iv[3] & 255) << 8)) ^ ServerConstants.version;
        ByteBuffer out = ByteBuffer.allocate(4 + plain.length).order(ByteOrder.LITTLE_ENDIAN);
        out.putShort((short) sequence).putShort((short) (sequence ^ plain.length));
        out.put(new MapleCrypto(282).crypt(plain.clone(), iv));
        return out.array();
    }

    static byte[] bytes(Object o) {
        if (o instanceof byte[] b) return b;
        if (o instanceof ByteBuf buf) {
            byte[] b = new byte[buf.readableBytes()];
            buf.readBytes(b);
            buf.release();
            return b;
        }
        throw new AssertionError("unexpected outbound " + o);
    }

    static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
        checks++;
    }
}
