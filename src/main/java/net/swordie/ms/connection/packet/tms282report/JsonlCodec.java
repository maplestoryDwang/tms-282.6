package net.swordie.ms.connection.packet.tms282report;

import java.util.Objects;
import java.util.function.Function;

/**
 * Strategy that turns one value into one JSON line and back, shared by {@link JsonlFileSink} and
 * {@link JsonlFileSource}. Client reports use {@link ClientReportJson#CODEC}, packet traces
 * {@link PacketTraceJson#CODEC}.
 * <p>
 * {@link #toJson} must never emit a line break (Jackson escapes them inside strings). {@link #fromJson}
 * throws {@link IllegalArgumentException} for a malformed or invalid line; the source skips and counts it.
 */
public interface JsonlCodec<T> {
    String toJson(T value);

    T fromJson(String line);

    static <T> JsonlCodec<T> of(Function<T, String> writer, Function<String, T> reader) {
        Objects.requireNonNull(writer, "writer");
        Objects.requireNonNull(reader, "reader");
        return new JsonlCodec<>() {
            @Override
            public String toJson(T value) {
                return writer.apply(value);
            }

            @Override
            public T fromJson(String line) {
                return reader.apply(line);
            }
        };
    }
}
