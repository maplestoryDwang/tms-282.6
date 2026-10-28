package net.swordie.ms.connection.packet;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** CP0085: name plus three opaque context fields. This is not PIN authentication. */
public record Tms282CharacterNameRequest(String name, int contextInt, int contextShort,
                                         int contextByte, byte[] nameBytes) {
    public Tms282CharacterNameRequest {
        Objects.requireNonNull(name, "name");
        nameBytes = nameBytes.clone();
        if (nameBytes.length > 0xFFFF || contextShort < 0 || contextShort > 0xFFFF
                || contextByte < 0 || contextByte > 0xFF) {
            throw new IllegalArgumentException("Character-name field outside its wire range");
        }
    }

    @Override
    public byte[] nameBytes() {
        return nameBytes.clone();
    }

    /** Accepts the full plaintext packet, including opcode; preserves name bytes for LP0017 echo. */
    public static Tms282CharacterNameRequest parse(byte[] packet, Charset charset) {
        Objects.requireNonNull(packet, "packet");
        Objects.requireNonNull(charset, "charset");
        if (packet.length < 11) {
            throw new IllegalArgumentException("Truncated CP0085");
        }
        ByteBuffer in = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
        if (Short.toUnsignedInt(in.getShort()) != 0x0085) {
            throw new IllegalArgumentException("Wrong character-name request opcode");
        }
        int length = Short.toUnsignedInt(in.getShort());
        if (in.remaining() != length + 7) {
            throw new IllegalArgumentException("CP0085 name length or trailing context mismatch");
        }
        byte[] bytes = new byte[length];
        in.get(bytes);
        String name;
        try {
            name = charset.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException e) {
            throw new IllegalArgumentException("Invalid character-name encoding", e);
        }
        return new Tms282CharacterNameRequest(name, in.getInt(), Short.toUnsignedInt(in.getShort()),
                Byte.toUnsignedInt(in.get()), bytes);
    }

    /** Compatibility entry point for offline UTF-8 fixtures. Live callers must supply their wire charset. */
    public static boolean isValidName(String name) {
        return isValidName(name, StandardCharsets.UTF_8);
    }

    /** Keep the 4..13-byte name limit inside the native 15-byte name field, without splitting Han characters. */
    public static boolean isValidName(String name, Charset charset) {
        Objects.requireNonNull(charset, "charset");
        if (name == null || name.isEmpty() || !name.codePoints().allMatch(c -> c >= '0' && c <= '9'
                || c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z'
                || Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN)) return false;
        try {
            int bytes = charset.newEncoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).encode(CharBuffer.wrap(name)).remaining();
            return bytes >= 4 && bytes <= 13;
        } catch (CharacterCodingException e) {
            return false;
        }
    }
}
