package net.swordie.ms.connection.packet.tms282report;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Ordered field collector filled by a {@link ClientReportParser}, plus the byte ranges the parser
 * marked as secret. Secret ranges survive a parser failure ({@link #withoutFields()}), so the hex
 * fallback and the stored raw bytes never show them.
 */
public final class ReportFields {
    /** Value shown instead of a secret field. */
    public static final String SECRET_VALUE = "(已遮蔽)";
    private final List<Tms282ClientReport.Field> fields = new ArrayList<>();
    private final List<Tms282ClientReport.Redaction> secrets = new ArrayList<>();

    public ReportFields add(String name, Object value) {
        fields.add(new Tms282ClientReport.Field(name, String.valueOf(value)));
        return this;
    }

    /**
     * Hex of {@code data}; at most {@link Tms282ClientReport#MAX_RAW_BYTES} bytes are rendered and a longer
     * value ends with the total byte count.
     */
    public ReportFields hex(String name, byte[] data) {
        if (data.length == 0) {
            return add(name, "(空)");
        }
        if (data.length <= Tms282ClientReport.MAX_RAW_BYTES) {
            return add(name, ReportReader.hex(data));
        }
        return add(name, ReportReader.hex(Arrays.copyOf(data, Tms282ClientReport.MAX_RAW_BYTES))
                + "…(共 " + data.length + " bytes，已截斷)");
    }

    /**
     * Marks {@code length} bytes at packet offset {@code offset} (opcode = 0) as secret: the field shows
     * {@link #SECRET_VALUE} and the report's raw bytes are zeroed there. Mark before reading the bytes, so a
     * truncated packet is still masked when the parser then fails.
     */
    public ReportFields secret(String name, int offset, int length) {
        secrets.add(new Tms282ClientReport.Redaction(offset, length));
        return add(name, SECRET_VALUE);
    }

    /** Same secret ranges, no fields: the starting point of the hex fallback after a parser failure. */
    ReportFields withoutFields() {
        ReportFields copy = new ReportFields();
        copy.secrets.addAll(secrets);
        return copy;
    }

    List<Tms282ClientReport.Field> build() {
        return List.copyOf(fields);
    }

    List<Tms282ClientReport.Redaction> secrets() {
        return List.copyOf(secrets);
    }
}
