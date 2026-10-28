package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.protocol.codec.LegacyPacketTranslator;
import net.swordie.ms.handlers.header.OutHeader;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * TMS 282.3 {@link LegacyPacketTranslator}: a composite keyed by the 265 {@link OutHeader} a finished packet was
 * built from. Features contribute their rules through a {@link Contributor} (registered in
 * {@link Tms282FieldEncoders#register}); a header can have only one rule. A rule returns the 282 packet or empty
 * to decline; a rule that throws (malformed 265 body) declines too, so the 265 packet goes on to the guard and is
 * dropped as before.
 * <p>
 * Contributors: {@link Tms282SystemMessageTranslators} (CHAT_MSG, EXCL_REQUEST, SET_IN_GAME_DIRECTION_MODE,
 * MESSAGE records).
 */
public final class Tms282LegacyPackets implements LegacyPacketTranslator {

    /** Adds rules to the builder (one feature's 265 headers). */
    @FunctionalInterface
    public interface Contributor {
        void contribute(Builder builder);
    }

    private final Map<OutHeader, Function<OutPacket, Optional<OutPacket>>> rules;

    private Tms282LegacyPackets(Map<OutHeader, Function<OutPacket, Optional<OutPacket>>> rules) {
        this.rules = Collections.unmodifiableMap(new EnumMap<>(rules));
    }

    public static Builder builder(Charset charset) {
        return new Builder(charset);
    }

    /** The 265 headers with a rule. */
    public Set<OutHeader> headers() {
        return rules.keySet();
    }

    @Override
    public Optional<OutPacket> translate(OutPacket legacy) {
        Function<OutPacket, Optional<OutPacket>> rule = rules.get(legacy.getOrigin());
        if (rule == null) {
            return Optional.empty();
        }
        try {
            return Objects.requireNonNull(rule.apply(legacy), "rule result");
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    @Override
    public void observe(OutPacket sent) {
        Tms282UnlockScope.observe(sent);
    }

    public static final class Builder {
        private final Charset charset;
        private final Map<OutHeader, Function<OutPacket, Optional<OutPacket>>> rules = new EnumMap<>(OutHeader.class);

        private Builder(Charset charset) {
            this.charset = Objects.requireNonNull(charset, "charset");
        }

        /** The 282 string charset (character-name charset) for rules that re-encode 265 UTF-8 strings. */
        public Charset charset() {
            return charset;
        }

        /** @throws IllegalStateException when {@code header} already has a rule */
        public Builder on(OutHeader header, Function<OutPacket, Optional<OutPacket>> rule) {
            Objects.requireNonNull(header, "header");
            Objects.requireNonNull(rule, "rule");
            if (rules.putIfAbsent(header, rule) != null) {
                throw new IllegalStateException("Legacy translator registered twice for " + header);
            }
            return this;
        }

        public Builder with(Contributor contributor) {
            contributor.contribute(this);
            return this;
        }

        public Tms282LegacyPackets build() {
            return new Tms282LegacyPackets(rules);
        }
    }

    /**
     * Little-endian reader over the body of a 265 packet (after its 2-byte opcode). Strings are UTF-8 like
     * {@link OutPacket#encodeString(String)}. Reading past the end, or {@link #end()} with bytes left, throws
     * {@link IllegalArgumentException}.
     */
    public static final class Body {
        private final ByteBuffer buf;

        private Body(byte[] body) {
            buf = ByteBuffer.wrap(body).order(ByteOrder.LITTLE_ENDIAN);
        }

        public static Body of(OutPacket legacy) {
            byte[] data = legacy.getData();
            return new Body(Arrays.copyOfRange(data, 2, data.length));
        }

        public int u8() {
            return read(() -> buf.get() & 0xFF);
        }

        public int u16() {
            return read(() -> buf.getShort() & 0xFFFF);
        }

        public int i32() {
            return read(buf::getInt);
        }

        public long i64() {
            return read(buf::getLong);
        }

        public String str() {
            byte[] b = bytes(u16());
            return new String(b, StandardCharsets.UTF_8);
        }

        public byte[] bytes(int n) {
            if (n < 0 || n > buf.remaining()) {
                throw new IllegalArgumentException("265 body truncated: need " + n + ", have " + buf.remaining());
            }
            byte[] b = new byte[n];
            buf.get(b);
            return b;
        }

        public byte[] rest() {
            return bytes(buf.remaining());
        }

        public int remaining() {
            return buf.remaining();
        }

        /** @throws IllegalArgumentException when bytes are left (the rule does not know this body) */
        public void end() {
            if (buf.hasRemaining()) {
                throw new IllegalArgumentException("265 body has " + buf.remaining() + " unread bytes");
            }
        }

        private <T> T read(java.util.function.Supplier<T> r) {
            try {
                return r.get();
            } catch (BufferUnderflowException e) {
                throw new IllegalArgumentException("265 body truncated", e);
            }
        }
    }
}
