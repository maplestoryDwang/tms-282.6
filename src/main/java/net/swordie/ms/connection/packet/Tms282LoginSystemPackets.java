package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.util.FileTime;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static net.swordie.ms.connection.packet.Tms282LoginPacketSupport.str;

/**
 * TMS 282.3 session plumbing without a login-screen UI of its own: the plaintext login and channel hellos,
 * LP0020 ping, LP0064 server time,
 * LP005A file-check completion, LP0B8D game-log initialization, LP0034 app flag, LP0037 trigger,
 * LP0B87 int list, LP0018 default key layout (global table, applied later by KeyConfig), LP0050
 * identity blob (echoed by CP00A4) and LP0069 WebAM log line (file write only).
 * See {@link Tms282LoginPacketSupport} for the opcodes deliberately left out.
 */
public final class Tms282LoginSystemPackets {
    private Tms282LoginSystemPackets() {
    }

    /** Hello version field (346C60E: the low 15 bits must be 0x11A; bit 15 set makes a channel socket throw). */
    public static final int HELLO_VERSION = 282;
    /** 346BD82: locale must be 6 (TMS), otherwise the client throws 0x22000007. */
    public static final int HELLO_LOCALE = 6;
    /** Last hello byte, stored in sock+0xC: 5 on the verified login hello, 4 for a channel (265 ChannelAcceptor). */
    public static final int SERVER_TYPE_LOGIN = 5;
    public static final int SERVER_TYPE_CHANNEL = 4;
    /** Login-only patch string (B1, "3:1" -> minor 3) and the two version ints B2/B3 (282 * 100 + 3). */
    // 282.4 (2026-09-29, research/tms2824/PORT.md): the client accepts minor <= 4 (346C6A4) and requires build 28204
    // in both hello ints (28203 -> error 0x22000007). Opcodes and layouts are unchanged from 282.3.
    // 282.5 (2026-10-08, research/tms2825): the same three values again (minor 5, build 28205); which one is sent is
    // config: tms282.clientBuild, see Tms282ClientBuild.
    private static final String LOGIN_PATCH = Tms282ClientBuild.patch();
    private static final int LOGIN_MINOR_VERSION = Tms282ClientBuild.build();

    /**
     * Verified 54-byte login hello (sessions/1790036161981/hello.bin), byte-identical to
     * Login.sendConnect(recvIv, sendIv, false) for 282/locale 6: login-only prefix A1..A6
     * (u16 282, str "3:1", recvIv, sendIv, u8 6, u8 0), the shared core, login-only B1..B6
     * (str "3:1", i32 28203, i32 28203, i32 0, u8 1, u8 0), then u8 serverType 5.
     * <p>
     * Like every hello this is PLAINTEXT: write {@code getData()} (u16 body length + body) to the socket
     * as-is, before any encrypted packet. Never pass it to an encrypting send; {@code getHeader()} is the
     * length, not an opcode.
     */
    public static OutPacket loginHello(byte[] recvIv, byte[] sendIv) {
        OutPacket body = new OutPacket();
        body.encodeShort(HELLO_VERSION);
        str(body, LOGIN_PATCH, StandardCharsets.US_ASCII);
        body.encodeArr(iv(recvIv, "recvIv"));
        body.encodeArr(iv(sendIv, "sendIv"));
        body.encodeByte(HELLO_LOCALE);
        body.encodeByte(0);
        encodeHelloCore(body, recvIv, sendIv);
        str(body, LOGIN_PATCH, StandardCharsets.US_ASCII);
        body.encodeInt(LOGIN_MINOR_VERSION);
        body.encodeInt(LOGIN_MINOR_VERSION);
        body.encodeInt(0);
        body.encodeByte(1);
        body.encodeByte(0);
        body.encodeByte(SERVER_TYPE_LOGIN);
        return lengthPrefixed(body);
    }

    /**
     * Channel-socket hello (channel-enter-2823.md 1.2, parser 346B624..346BC68 with [sock+0x48]==0): 18 bytes,
     * {@code 10 00 | 1A 01 | 1A 01 00 00 | recvIv | sendIv | 06 | 04}. The channel parser skips every login-only
     * field, so the body is only the shared core plus serverType. IV1 = the server's receive IV (client send IV,
     * sock+0xD8), IV2 = the server's send IV (sock+0xDC); the session then uses the same MapleCrypto(282) framing
     * as the login socket. serverType 4 is presumed (265 value; 4 calls 15C09E0 in the VM). Plaintext, see
     * {@link #loginHello}.
     */
    public static OutPacket channelHello(byte[] recvIv, byte[] sendIv) {
        OutPacket body = new OutPacket();
        encodeHelloCore(body, recvIv, sendIv);
        body.encodeByte(SERVER_TYPE_CHANNEL);
        return lengthPrefixed(body);
    }

    /** Fields #1..#5 both parsers read (346B8B2..346BA0A): u16 version, u32 version, IV1, IV2, u8 locale. */
    private static void encodeHelloCore(OutPacket body, byte[] recvIv, byte[] sendIv) {
        body.encodeShort(HELLO_VERSION);
        body.encodeInt(HELLO_VERSION); // #2, unused by readable code; the verified login hello sends 1A 01 00 00.
        body.encodeArr(iv(recvIv, "recvIv"));
        body.encodeArr(iv(sendIv, "sendIv"));
        body.encodeByte(HELLO_LOCALE);
    }

    private static OutPacket lengthPrefixed(OutPacket body) {
        OutPacket out = new OutPacket(body.getLength());
        out.encodeArr(body.getData());
        return out;
    }

    private static byte[] iv(byte[] iv, String what) {
        if (Objects.requireNonNull(iv, what).length != 4) {
            throw new IllegalArgumentException(what + " must be 4 bytes");
        }
        return iv;
    }

    /** LP0020 keep-alive ping; no payload (captured in every probe session, e.g. 1790062939853/send-006). */
    public static OutPacket ping() {
        return Tms282LoginPacketSupport.packet(0x0020);
    }

    public static OutPacket serverTime() {
        OutPacket out = Tms282LoginPacketSupport.packet(0x0064);
        // This reader compares GetLocalTime -> SystemTimeToFileTime, without UTC normalization.
        out.encodeFT(FileTime.fromEpochMillis(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(8)));
        return out;
    }

    /**
     * LP0045 [u8 0]: the reply the client waits for after the pre-login CP00BC (u32). Captured probe reply
     * (research/tms2823/responses/00bc.bin = 45 00 00, e.g. session 1790134047977 send-007-0045); without it
     * the client disconnects before CP0074 (formal-1790175816237, 2026-09-23). Semantics of the byte unknown.
     */
    public static OutPacket preLoginAck0045() {
        OutPacket out = Tms282LoginPacketSupport.packet(0x0045);
        out.encodeByte(0);
        return out;
    }

    public static OutPacket gameLogUnavailable() {
        OutPacket out = Tms282LoginPacketSupport.packet(0x0B8D);
        out.encodeInt(-1); // Native no-init branch at RVA 0xF3C57F.
        return out;
    }

    /** Official TMS 282.3 reply to a verified CP0078 report; no payload. */
    public static OutPacket fileCheckComplete() {
        return Tms282LoginPacketSupport.packet(0x005A);
    }

    /** LP0034 (inline), low confidence: u8 -> app+0x23C8 = (value != 0); no reader found. */
    public static OutPacket appFlag23C8(boolean flag) {
        OutPacket out = Tms282LoginPacketSupport.packet(0x0034);
        out.encodeByte(flag);
        return out;
    }

    /** LP0037 (3CAC690), low confidence: no payload; calls 5D575C0 -> 1652D30(obj, 2). */
    public static OutPacket trigger0037() {
        return Tms282LoginPacketSupport.packet(0x0037);
    }

    /**
     * LP0B87 (3C9AC50), low confidence: i32 unused, i32 flag -> CLogin+0x5D0, i32 n (<= 1,000,000)
     * + n x i32 -> CLogin+0x5D8. No reader of the stored values found.
     */
    public static OutPacket loginIntList(boolean flag, List<Integer> values) {
        Objects.requireNonNull(values, "values");
        if (values.size() > 1_000_000) {
            throw new IllegalArgumentException("Client rejects more than 1,000,000 entries");
        }
        OutPacket out = Tms282LoginPacketSupport.packet(0x0B87);
        out.encodeInt(0);
        out.encodeInt(flag ? 1 : 0);
        out.encodeInt(values.size());
        for (int value : values) {
            out.encodeInt(value);
        }
        return out;
    }

    /**
     * LP0018 (3CA8690), optional: four key strings (table 867ABA0) and one quick-slot string. The client
     * copies them into the global default tables 9EE8AA0/9EE8C60 at once; no UI, no reply, any stage.
     * Every string is ASCII, so the byte length equals the checked character length.
     */
    public static OutPacket defaultKeyLayout(Tms282DefaultKeyLayout layout) {
        Objects.requireNonNull(layout, "layout");
        OutPacket out = Tms282LoginPacketSupport.packet(0x0018);
        for (String keys : layout.keyStrings()) {
            str(out, keys, StandardCharsets.US_ASCII);
        }
        str(out, layout.quickSlotString(), StandardCharsets.US_ASCII);
        return out;
    }

    /**
     * LP0050 (3477C90 -> reader 34797D0): i32 n (<= 1,000,000), n bytes, u8 persist. persist != 0 stores
     * the bytes in the registry value maple_ur_tw, otherwise in memory (9EE5198). The client answers at
     * once with CP00A4 echoing the same bytes, so the blob works as an opaque device/session marker.
     * Not needed: the local flow never sends it.
     */
    public static OutPacket identityBlob(byte[] blob, boolean persist) {
        Objects.requireNonNull(blob, "blob");
        if (blob.length > 1_000_000) {
            throw new IllegalArgumentException("Client rejects more than 1,000,000 bytes");
        }
        OutPacket out = Tms282LoginPacketSupport.packet(0x0050);
        out.encodeInt(blob.length);
        out.encodeArr(blob);
        out.encodeByte(persist);
        return out;
    }

    /**
     * LP0069 (5CA3B00, reader 873AC0), documentation only - do not send. The client only appends
     * "ExamID : examId[ | Error : error]" to &lt;client dir&gt;\WebAM.txt; no UI, no login state, no reply.
     * Layout: i32, i32, str examId, str error, i32, i32 n, n x {buf4, i32, i32, i32, u8}, buf4; this
     * builder sends n = 0.
     * <p>
     * 265's opcode 0x69 is USER_GET_TIME_RESULT ({@code Login.sendLoginTime}, one 8-byte FILETIME). That
     * layout is incompatible: the 282 client would read the FILETIME as the two i32 and then DecodeStr
     * past the end of the packet. The 282 flow must never reuse it; 282 server time is LP0064
     * ({@link #serverTime()}).
     */
    public static OutPacket webAmExamLog(String examId, String error, Charset charset) {
        OutPacket out = Tms282LoginPacketSupport.packet(0x0069);
        out.encodeInt(0);
        out.encodeInt(0);
        str(out, examId, charset);
        str(out, error, charset);   // empty -> no " | Error : " part
        out.encodeInt(0);
        out.encodeInt(0);           // n = 0 entries
        out.encodeInt(0);           // trailing buf4
        return out;
    }
}
