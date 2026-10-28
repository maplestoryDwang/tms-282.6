package net.swordie.ms.connection.packet.tms282report;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * For reports whose exact layout is not established but which visibly embed DecodeStr
 * strings (CP0307 / CP0309 hardware, CP00AE pre-disconnect report).
 * <p>
 * Scans the body for {@code u16 length + printable ASCII} runs of at least
 * {@value #MINIMUM_LENGTH} characters. Each string becomes a {@code 字串@offset} field and each
 * gap a {@code hex@offset} field, so the whole body is shown and nothing is guessed beyond
 * "this looks like a string". Offsets count from the start of the packet (opcode = 0).
 */
public final class EmbeddedStringReportParser implements ClientReportParser {
    static final int MINIMUM_LENGTH = 3;
    private final String name;

    public EmbeddedStringReportParser(String name) {
        this.name = name;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public void parse(ReportReader in, ReportFields out) {
        int base = in.position();
        byte[] body = in.rest();
        int gapStart = 0;
        int i = 0;
        while (i + 2 <= body.length) {
            int length = Byte.toUnsignedInt(body[i]) | Byte.toUnsignedInt(body[i + 1]) << 8;
            if (length >= MINIMUM_LENGTH && i + 2 + length <= body.length && printable(body, i + 2, length)) {
                gap(out, body, base, gapStart, i);
                out.add(String.format("字串@0x%04X", base + i),
                        new String(body, i + 2, length, StandardCharsets.US_ASCII));
                i += 2 + length;
                gapStart = i;
            } else {
                i++;
            }
        }
        gap(out, body, base, gapStart, body.length);
    }

    private static void gap(ReportFields out, byte[] body, int base, int from, int to) {
        if (to > from) {
            out.hex(String.format("hex@0x%04X", base + from), Arrays.copyOfRange(body, from, to));
        }
    }

    private static boolean printable(byte[] data, int from, int length) {
        for (int i = from; i < from + length; i++) {
            if (data[i] < 0x20 || data[i] > 0x7E) {
                return false;
            }
        }
        return true;
    }
}
