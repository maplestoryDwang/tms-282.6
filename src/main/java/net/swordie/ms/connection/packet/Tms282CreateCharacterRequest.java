package net.swordie.ms.connection.packet;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.util.Objects;

/**
 * CP008E: the 265 creation fields, followed by a 282 client-integrity tail
 * (seven hash strings, an int-length opaque blob, two hash strings). The tail is
 * checked for structure only; its values are not interpreted.
 * Layout from real-client capture session 1790059975193, recv-070 (392 bytes).
 */
public record Tms282CreateCharacterRequest(String name, int keySettingType, int eventNewCharSaleJob,
                                           int race, short subJob, byte gender, byte skin, int[] items) {
    private static final int LEADING_HASHES = 7, TRAILING_HASHES = 2, MAX_ITEMS = 16, MAX_BLOB = 0x400;

    public Tms282CreateCharacterRequest {
        Objects.requireNonNull(name, "name");
        items = items.clone();
    }

    @Override
    public int[] items() {
        return items.clone();
    }

    /** Accepts the full plaintext packet, including opcode. */
    public static Tms282CreateCharacterRequest parse(byte[] packet, Charset charset) {
        Objects.requireNonNull(packet, "packet");
        Objects.requireNonNull(charset, "charset");
        ByteBuffer in = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
        try {
            if (Short.toUnsignedInt(in.getShort()) != 0x008E) {
                throw new IllegalArgumentException("Wrong create-character request opcode");
            }
            String name = decode(readBytes(in, Short.toUnsignedInt(in.getShort())), charset);
            int keySettingType = in.getInt();
            int eventNewCharSaleJob = in.getInt();
            int race = in.getInt();
            short subJob = in.getShort();
            byte gender = in.get();
            byte skin = in.get();
            int count = Byte.toUnsignedInt(in.get());
            if (count == 0 || count > MAX_ITEMS) {
                throw new IllegalArgumentException("Invalid create-character item count");
            }
            int[] items = new int[count];
            for (int i = 0; i < count; i++) {
                items[i] = in.getInt();
            }
            for (int i = 0; i < LEADING_HASHES; i++) {
                readBytes(in, Short.toUnsignedInt(in.getShort()));
            }
            int blob = in.getInt();
            if (blob < 0 || blob > MAX_BLOB) {
                throw new IllegalArgumentException("Invalid create-character integrity blob length");
            }
            readBytes(in, blob);
            for (int i = 0; i < TRAILING_HASHES; i++) {
                readBytes(in, Short.toUnsignedInt(in.getShort()));
            }
            if (in.hasRemaining()) {
                throw new IllegalArgumentException("Unexpected bytes after CP008E integrity tail");
            }
            return new Tms282CreateCharacterRequest(name, keySettingType, eventNewCharSaleJob, race, subJob,
                    gender, skin, items);
        } catch (BufferUnderflowException e) {
            throw new IllegalArgumentException("Truncated CP008E", e);
        }
    }

    private static byte[] readBytes(ByteBuffer in, int length) {
        if (length > in.remaining()) {
            throw new BufferUnderflowException();
        }
        byte[] bytes = new byte[length];
        in.get(bytes);
        return bytes;
    }

    private static String decode(byte[] bytes, Charset charset) {
        try {
            return charset.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException e) {
            throw new IllegalArgumentException("Invalid character-name encoding", e);
        }
    }
}
