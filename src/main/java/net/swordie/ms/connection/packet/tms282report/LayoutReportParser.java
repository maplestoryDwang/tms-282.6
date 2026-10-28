package net.swordie.ms.connection.packet.tms282report;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Declarative parser for fixed report layouts, built step by step in wire order:
 * <pre>{@code
 * LayoutReportParser.named("載入時間").u8("旗標1").u8("旗標2").i32("時間1").build();
 * }</pre>
 * Steps follow the client's Encode calls documented in login-cp-layouts-2823.md or observed in
 * captures; an unknown tail is left to {@link ClientReportParsers}' trailing hex field.
 */
public final class LayoutReportParser implements ClientReportParser {
    /** One wire step. Custom steps cover conditional layouts. */
    @FunctionalInterface
    public interface Step {
        void read(ReportReader in, ReportFields out);
    }

    private final String name;
    private final List<Step> steps;

    private LayoutReportParser(String name, List<Step> steps) {
        this.name = name;
        this.steps = List.copyOf(steps);
    }

    public static Builder named(String name) {
        return new Builder(name);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public void parse(ReportReader in, ReportFields out) {
        for (Step step : steps) {
            step.read(in, out);
        }
    }

    public static final class Builder {
        private final String name;
        private final List<Step> steps = new ArrayList<>();

        private Builder(String name) {
            this.name = Objects.requireNonNull(name, "name");
        }

        public Builder u8(String field) {
            return step((in, out) -> out.add(field, in.u8()));
        }

        public Builder u16(String field) {
            return step((in, out) -> out.add(field, in.u16()));
        }

        public Builder i32(String field) {
            return step((in, out) -> out.add(field, in.i32()));
        }

        public Builder i64(String field) {
            return step((in, out) -> out.add(field, in.i64()));
        }

        public Builder str(String field) {
            return step((in, out) -> out.add(field, in.str()));
        }

        /**
         * DecodeStr whose content is secret: the field shows {@link ReportFields#SECRET_VALUE} and the string
         * bytes (not the u16 length) are zeroed in the stored raw packet.
         */
        public Builder secretStr(String field) {
            return step((in, out) -> {
                int length = in.u16();
                out.secret(field, in.position(), Math.min(length, in.remaining()));
                in.bytes(length);
            });
        }

        /** Fixed-size secret bytes, masked like {@link #secretStr}. */
        public Builder secretBytes(String field, int length) {
            return step((in, out) -> {
                out.secret(field, in.position(), Math.min(length, in.remaining()));
                in.bytes(length);
            });
        }

        public Builder rotatedStr(String field, int rotateRightBits) {
            return step((in, out) -> out.add(field, in.rotatedStr(rotateRightBits)));
        }

        /** i32 length followed by that many opaque bytes, shown as hex. */
        public Builder sizedBlob(String field) {
            return step((in, out) -> {
                int length = in.i32();
                out.add(field + "長度", length);
                out.hex(field, in.bytes(length));
            });
        }

        /** Fixed-size opaque bytes, shown as hex. */
        public Builder bytes(String field, int length) {
            return step((in, out) -> out.hex(field, in.bytes(length)));
        }

        /** Win32 SYSTEMTIME (8 × u16). */
        public Builder systemTime(String field) {
            return step((in, out) -> {
                int year = in.u16(), month = in.u16();
                in.u16(); // wDayOfWeek
                int day = in.u16(), hour = in.u16(), minute = in.u16(), second = in.u16(), millis = in.u16();
                out.add(field, String.format("%04d-%02d-%02d %02d:%02d:%02d.%03d", year, month, day, hour, minute, second, millis));
            });
        }

        /** in_addr as stored by getsockname (network byte order in memory). */
        public Builder ipv4(String field) {
            return step((in, out) -> {
                int value = in.i32();
                out.add(field, (value & 255) + "." + (value >>> 8 & 255) + "." + (value >>> 16 & 255) + "." + (value >>> 24 & 255));
            });
        }

        /** Whatever is left, as hex (for tails whose meaning is not established). */
        public Builder rest(String field) {
            return step((in, out) -> out.hex(field, in.rest()));
        }

        public Builder step(Step step) {
            steps.add(Objects.requireNonNull(step, "step"));
            return this;
        }

        public LayoutReportParser build() {
            return new LayoutReportParser(name, steps);
        }
    }
}
