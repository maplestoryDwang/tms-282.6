package net.swordie.ms.connection.packet.tms282report;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Little-endian cursor over one client packet body (after the opcode), as the client's
 * Decode1/2/4/8 and DecodeStr (u16 length + bytes) read it. Underflow throws
 * {@link IllegalArgumentException}, which {@link ClientReportParsers} turns into a hex fallback.
 */
public final class ReportReader {
    private final ByteBuffer in;
    private final Charset charset;

    ReportReader(byte[] packet, int offset, Charset charset) {
        this.in = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
        this.in.position(offset);
        this.charset = Objects.requireNonNull(charset, "charset");
    }

    public int position() {
        return in.position();
    }

    public int remaining() {
        return in.remaining();
    }

    public int u8() {
        need(1);
        return Byte.toUnsignedInt(in.get());
    }

    public int u16() {
        need(2);
        return Short.toUnsignedInt(in.getShort());
    }

    public int i32() {
        need(4);
        return in.getInt();
    }

    public long i64() {
        need(8);
        return in.getLong();
    }

    public byte[] bytes(int length) {
        if (length < 0) {
            throw new IllegalArgumentException("Negative length " + length);
        }
        need(length);
        byte[] out = new byte[length];
        in.get(out);
        return out;
    }

    public byte[] rest() {
        return bytes(in.remaining());
    }

    /** DecodeStr: u16 length, then bytes in the configured charset (see {@link #text(byte[])}). */
    public String str() {
        return text(bytes(u16()));
    }

    /** DecodeStr whose bytes were obfuscated by the client with a left rotation; undone by rotating right. */
    public String rotatedStr(int rotateRightBits) {
        return text(rotateRight(bytes(u16()), rotateRightBits));
    }

    /**
     * Strict decode in the configured charset. When that fails or yields control characters the
     * charset is uncertain, so the value is decoded as lenient UTF-8 and the hex is listed as well.
     */
    public String text(byte[] data) {
        String strict = strictText(data);
        return strict != null && strict.codePoints().noneMatch(Character::isISOControl) ? strict : withHex(data);
    }

    /**
     * Multi-line text (ELog): control characters are expected, so only an undecodable byte
     * sequence falls back to UTF-8 + hex. Line breaks are shown as " ⏎ " to keep one line per field.
     */
    public String multilineText(byte[] data) {
        String strict = strictText(data);
        return strict == null ? withHex(data) : strict.replace("\r\n", " ⏎ ").replace("\n", " ⏎ ").replace("\r", " ⏎ ");
    }

    private String strictText(byte[] data) {
        try {
            return charset.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(data)).toString();
        } catch (CharacterCodingException e) {
            return null;
        }
    }

    private static String withHex(byte[] data) {
        return new String(data, StandardCharsets.UTF_8) + " [hex " + hex(data) + "]";
    }

    public static String hex(byte[] data) {
        return HexFormat.of().withUpperCase().formatHex(data);
    }

    static byte[] rotateRight(byte[] data, int bits) {
        byte[] out = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            int value = Byte.toUnsignedInt(data[i]);
            out[i] = (byte) ((value >>> bits) | (value << (8 - bits)));
        }
        return out;
    }

    private void need(int length) {
        if (in.remaining() < length) {
            throw new IllegalArgumentException("Truncated at offset " + in.position() + ": need " + length
                    + " byte(s), " + in.remaining() + " left");
        }
    }
}
