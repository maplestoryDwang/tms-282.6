package net.swordie.ms.connection.packet;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** TMS 282.3 CP0078 file report. Parsing and comparison only; sends no acknowledgement. */
public record Tms282FileReport(int metric1, int metric2, int reserved, int tableCrc,
                               Map<String, Integer> fileCrcs) {
    private static final int HEADER_LENGTH = 38;
    private static final int COUNT_BIAS = 0x029F3D35;
    private static final int CRC_BIAS = 0x2497D08D;

    public Tms282FileReport {
        fileCrcs = Map.copyOf(fileCrcs);
    }

    /** Accepts the complete plaintext packet, including its two-byte opcode. */
    public static Tms282FileReport parse(byte[] packet) {
        Objects.requireNonNull(packet, "packet");
        require(packet.length >= HEADER_LENGTH, "Truncated file-report header");
        ByteBuffer in = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
        require(Short.toUnsignedInt(in.getShort()) == 0x0078, "Wrong file-report opcode");
        // magic2 0x68860C00 = 282.3, 0x68860C01 = 282.4, 0x68860C02 = 282.5, 0x68860C03 = 282.6
        // (research/tms2824, tms2825); the manifest check below still compares every file CRC with the trusted reference
        int magic1 = in.getInt();
        int magic2 = in.getInt();
        require(magic1 == 0x4E8EE75F && (magic2 >= 0x68860C00 && magic2 <= 0x68860C03)
                && in.getInt() == 0x0003AC54, "Wrong TMS 282.x file-report magic");
        require(in.getInt() == 1, "Unsupported file-report type");
        int metric1 = in.getInt();
        int metric2 = in.getInt();
        int reserved = in.getInt();
        require(reserved == 0, "Unsupported file-report reserved field");
        int tableCrc = in.getInt();
        long count = Integer.toUnsignedLong(in.getInt()) - COUNT_BIAS;
        // Each entry requires a u16 length, at least one UTF-8 byte, and one int.
        require(count >= 0 && count <= in.remaining() / 7, "Invalid file-report count");
        Map<String, Integer> files = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            require(in.remaining() >= 2, "Truncated filename length");
            int length = Short.toUnsignedInt(in.getShort());
            require(length > 0 && length <= in.remaining() - 4, "Invalid filename length");
            byte[] nameBytes = new byte[length];
            in.get(nameBytes);
            for (int j = 0; j < nameBytes.length; j++) {
                int value = Byte.toUnsignedInt(nameBytes[j]);
                nameBytes[j] = (byte) ((value >>> 1) | (value << 7));
            }
            String name;
            try {
                name = StandardCharsets.UTF_8.newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT)
                        .decode(ByteBuffer.wrap(nameBytes)).toString();
            } catch (CharacterCodingException e) {
                throw new IllegalArgumentException("Invalid UTF-8 filename", e);
            }
            require(name.indexOf('\0') < 0, "NUL in filename");
            // Java int subtraction intentionally retains the original 32-bit CRC bits.
            int crc = in.getInt() - CRC_BIAS;
            require(files.putIfAbsent(name, crc) == null, "Duplicate filename in file report");
        }
        require(!in.hasRemaining(), "Trailing file-report data");
        return new Tms282FileReport(metric1, metric2, reserved, tableCrc, files);
    }

    /** The reference must come from an independently trusted report, never the current client. */
    public void validateAgainst(Tms282FileReport trustedReference) {
        Objects.requireNonNull(trustedReference, "trustedReference");
        require(tableCrc == trustedReference.tableCrc, "File-report table CRC mismatch");
        require(fileCrcs.equals(trustedReference.fileCrcs), "File-report manifest mismatch");
    }

    private static void require(boolean valid, String message) {
        if (!valid) {
            throw new IllegalArgumentException(message);
        }
    }
}
