package net.swordie.ms.connection.packet.tms282report;

/**
 * Fallback: body length and hex. Used for opcodes without a parser, and (via {@link #failed})
 * when a registered parser throws, keeping the failing parser's name and its error.
 */
public final class HexReportParser implements ClientReportParser {
    public static final HexReportParser INSTANCE = new HexReportParser("未知回報", null);
    public static final String ERROR_FIELD = "解析錯誤";
    public static final String BODY_FIELD = "內容";
    private final String name;
    private final String error;

    private HexReportParser(String name, String error) {
        this.name = name;
        this.error = error;
    }

    /** Hex fallback for a parser that threw; the report keeps that parser's name. */
    public static HexReportParser failed(ClientReportParser parser, RuntimeException error) {
        return new HexReportParser(parser.name(), error.getClass().getSimpleName() + ": " + error.getMessage());
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public void parse(ReportReader in, ReportFields out) {
        if (error != null) {
            out.add(ERROR_FIELD, error);
        }
        out.add("長度", in.remaining());
        out.hex(BODY_FIELD, in.rest());
    }
}
