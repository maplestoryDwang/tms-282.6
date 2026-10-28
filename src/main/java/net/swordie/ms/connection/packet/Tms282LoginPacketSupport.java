package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Shared encoding and validation for the TMS 282.3 login packet factories
 * ({@link Tms282AuthPackets}, {@link Tms282WorldPackets}, {@link Tms282CharacterPackets},
 * {@link Tms282LoginUiPackets}, {@link Tms282LoginSystemPackets}).
 * <p>
 * Verified TMS 282.3 login layouts. Numeric opcodes deliberately bypass older OutHeader mappings.
 * Deliberately absent (unknown-lp-2823.md):
 * <ul>
 * <li>LP000D (3CA3640): only u8 flag, i32 n, n x str are native; the rest of the handler jumps into the
 * Themida VM (.themida has RawSize 0), so the full layout cannot be read. Only {@code [u8 0]} is proven harmless.</li>
 * <li>LP004B (3CAEA20): the whole handler runs inside the Themida VM; no readable field at all.</li>
 * <li>LP002D (3CACA00): the handler is a bare {@code ret} and reads nothing, so a builder has no purpose.</li>
 * </ul>
 * <p>
 * String encoding: every builder writes strings through {@link #str(OutPacket, String, Charset)}. Builders
 * with a {@code Charset} parameter use the caller's charset; builders without one use
 * {@link #DEFAULT_CHARSET}. {@link OutPacket#encodeString(String)} is deliberately not used: it silently
 * drops strings over 32767 bytes and switches to a per-char encoding when the text contains certain
 * Latin-1 symbols.
 */
final class Tms282LoginPacketSupport {
    /**
     * Charset of builders without a {@code Charset} parameter: UTF-8, the charset
     * {@link OutPacket#encodeString(String)} uses, so the captured golden packets stay byte-identical.
     */
    static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    private Tms282LoginPacketSupport() {
    }

    /**
     * A TMS 282.3 codec packet with its raw wire opcode: the same bytes as {@code new OutPacket(op)}, but marked
     * {@link OutPacket#forProfile built for} 282.3 so the fail-closed OutboundGuard of the 282 profile lets it
     * through (the research probe writes frames itself and never consults the guard).
     */
    static OutPacket packet(int op) {
        return OutPacket.forProfile(net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3, op);
    }

    /** Client DecodeStr (F8D3D0): u16 byte length, then the bytes in the caller's charset. */
    static void str(OutPacket out, String s, Charset charset) {
        byte[] bytes = Objects.requireNonNull(s, "string").getBytes(Objects.requireNonNull(charset, "charset"));
        if (bytes.length > 0xFFFF) {
            throw new IllegalArgumentException("String too long");
        }
        out.encodeShort(bytes.length);
        out.encodeArr(bytes);
    }

    static void checkByte(int value, String what) {
        if (value < 0 || value > 255) {
            throw new IllegalArgumentException(what + " must fit one byte");
        }
    }

    static void checkWorldId(int worldId) {
        // The world-list reader sign-extends its byte; negative values identify the terminator.
        if (worldId < 0 || worldId > 127) {
            throw new IllegalArgumentException("World ID must be between 0 and 127");
        }
    }
}
