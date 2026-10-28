package net.swordie.ms.connection.packet;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

/**
 * CP0193 USER_CHANGE_STAT_REQUEST (265 InHeader 0x18C), sender 5EDFFC0: {@code u32 tick, u16 hp, u16 mp, u8 option}
 * (encode sequence {@code 4 2 2 1}) [verified: static sender and 52 captured samples, each 11 bytes].
 * Evidence: packet-audit/infield-capture-1790158495223.md 2.4. The client sends it every 10 s per stat (HP 10, MP 3
 * for a new character; separate packets with the other amount 0 until the two timers line up) and changes
 * nothing itself: HP/MP only move when the server answers with LP009D STAT_CHANGED ({@link Tms282StatPackets}).
 * Natural HP/MP recovery [presumed, strong evidence]. option was 0 in every sample; its meaning is unknown.
 * <p>
 * 265 reads {@code tick, i32 mask, i32, n x i16, u8}; that layout does not fit 282.
 */
public record Tms282StatRecoveryRequest(int tick, int hp, int mp, int option) {
    public static final int OPCODE = 0x0193;
    public static final int LENGTH = 2 + 4 + 2 + 2 + 1;

    public Tms282StatRecoveryRequest {
        if (hp < 0 || hp > 0xFFFF || mp < 0 || mp > 0xFFFF) {
            throw new IllegalArgumentException("CP0193 amounts are u16");
        }
        Tms282LoginPacketSupport.checkByte(option, "CP0193 option");
    }

    /** Accepts the full plaintext packet, including the opcode. Exactly 11 bytes. */
    public static Tms282StatRecoveryRequest parse(byte[] packet) {
        Objects.requireNonNull(packet, "packet");
        ByteBuffer in = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
        try {
            if (Short.toUnsignedInt(in.getShort()) != OPCODE) {
                throw new IllegalArgumentException("Wrong CP0193 opcode");
            }
            var request = new Tms282StatRecoveryRequest(in.getInt(), Short.toUnsignedInt(in.getShort()),
                    Short.toUnsignedInt(in.getShort()), Byte.toUnsignedInt(in.get()));
            if (in.hasRemaining()) {
                throw new IllegalArgumentException("CP0193 has " + in.remaining() + " trailing bytes");
            }
            return request;
        } catch (BufferUnderflowException e) {
            throw new IllegalArgumentException("Truncated CP0193", e);
        }
    }

    /**
     * The HP/MP after applying this recovery: current + amount, capped at the maximum (a maximum below 1 is treated
     * as 1, a current value below 0 as 0, and a current value already above the maximum is kept). Pure arithmetic;
     * the caller supplies the character's values.
     */
    public Recovered applyTo(int currentHp, int maxHp, int currentMp, int maxMp) {
        return new Recovered(cap(currentHp, hp, maxHp), cap(currentMp, mp, maxMp));
    }

    public record Recovered(int hp, int mp) {
    }

    private static int cap(int current, int amount, int max) {
        long base = Math.max(0, current);
        long limit = Math.max(1, max);
        // Never lowers a value that is already above the maximum (recovery only adds).
        return (int) Math.max(base, Math.min(base + amount, limit));
    }
}
