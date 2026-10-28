package net.swordie.ms.connection.packet.tms282report;

import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Error-log files the client uploads after connecting (346AF80, unknown-misc-cp-2823.md): CP0095
 * *_ms_report.ini and CP0096 *_ms_callstack.ini (captured), CP0097 msexcr.ini and CP0094 msmig.ini
 * (static layout only). Wire form: a length prefix ({@link LengthPrefix}), then that many bytes of
 * text without a NUL; CP0094 is the only one with a u16 prefix.
 * <p>
 * The captured text is '|'-separated: CP0095 starts with {@code ELog|<level>|}; CP0095/0096 then carry
 * a header of KEY|VALUE pairs (VERSION|28203|DATETIME|...|Socket|127.0.0.1:8484) followed by free-form
 * content (error codes, call stacks). Header pairs become one field each; pairing stops at the
 * first token that is not a word-like key, and everything after it is one {@code 內容} field.
 * The msmig.ini / msexcr.ini text format is unknown; text without '|' pairs becomes one {@code 內容}.
 */
public final class ELogReportParser implements ClientReportParser {
    /** Width of the length field in front of the text. */
    public enum LengthPrefix {
        /** EncodeBuffer(4) length (CP0095, CP0096, CP0097). */
        I32 {
            @Override
            int read(ReportReader in) {
                return in.i32();
            }
        },
        /** Encode2 length (CP0094, 34792D0). */
        U16 {
            @Override
            int read(ReportReader in) {
                return in.u16();
            }
        };

        abstract int read(ReportReader in);
    }

    private static final Pattern KEY = Pattern.compile("[A-Za-z][A-Za-z0-9_]*");
    private final String name;
    private final LengthPrefix prefix;

    public ELogReportParser(String name) {
        this(name, LengthPrefix.I32);
    }

    public ELogReportParser(String name, LengthPrefix prefix) {
        this.name = name;
        this.prefix = Objects.requireNonNull(prefix, "prefix");
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public void parse(ReportReader in, ReportFields out) {
        int length = prefix.read(in);
        String text = in.multilineText(in.bytes(length));
        out.add("長度", length);
        String[] tokens = text.split("\\|", -1);
        int index = 0;
        if (tokens.length >= 2 && tokens[0].equals("ELog")) {
            out.add("等級", tokens[1]);
            index = 2;
        }
        while (index + 1 < tokens.length && KEY.matcher(tokens[index]).matches()) {
            out.add(tokens[index], tokens[index + 1]);
            index += 2;
        }
        String body = String.join("|", Arrays.copyOfRange(tokens, index, tokens.length)).strip();
        while (body.endsWith("|") || body.endsWith("⏎")) {
            body = body.substring(0, body.length() - 1).strip();
        }
        if (!body.isEmpty()) {
            out.add("內容", body);
        }
    }
}
