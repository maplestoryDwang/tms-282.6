package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * TMS 282.3 LP009D STAT_CHANGED (CWvsContext handler 5EEA1A0; GW_CharacterStat::DecodeChangeStat 8DA760).
 * Evidence: packet-audit/ingame-layouts-282.md section 5 and 5.1, and setfield_trace.py --entry 5EEA1A0
 * (expected/infield-stat-hpmp.tsv and infield-stat-wide.tsv) [verified]:
 * <pre>
 * u8   exclRequest   5EEA1EA  non-zero calls 5E04DA0(ctx, 0): clears the client's "request pending" lock
 * u8   0             5EEA211  (265 sends false; meaning unknown)
 * u8   1             5EEA223  (265 sends 1; stored in ctx+4D78)
 * Buf8 mask          8DA798   u64
 * the masked stats in code order (8DA760): level i32, str/dex/int/luk u16, hp i32, mhp i32, mp i32, mmp i32,
 *                    ap u16, exp i64, pop i32, ...
 * i64  money         5EEA449  only for mask bit 18, and only AFTER the whole DecodeChangeStat block
 * u8   0            5EEA599  no charmOld
 * u8   0            5EEA5C1  no hp/mp recovery numbers
 * </pre>
 * Porting rules from 5.1: the mask is built as a {@code long} (265 builds an {@code int}; bit 31 would sign-extend
 * into bits 32..63 and make the client read bit 32 and bit 34 fields); money moves behind the block; bit 19
 * (fatigue) is never read by 282 and must not be set. This factory only offers {@link Stat} values whose width
 * does not depend on other state (no skin/job/SP/day-limit/character-card/282-only bits), so a mask can never
 * name a field the builder does not write.
 * <p>
 * exclRequest: every reply to a request the client locked on (item use, AP/SP, ...) must set it, or the client stays
 * "busy". The CP0193 recovery reply sets it too. Native 5E04DA0 also updates ctx+2490/+24A4 timestamps and
 * calls 60874D0/6087940; structural decode checks do not establish that repeated unlocks are semantically harmless.
 */
public final class Tms282StatPackets {
    public static final int STAT_CHANGED = 0x009D;

    private Tms282StatPackets() {
    }

    /** Stats with a fixed width, in the client's read order. {@code width} is in bytes. */
    public enum Stat {
        FACE(1, 4),
        HAIR(2, 4),
        LEVEL(4, 4),
        STR(6, 2),
        DEX(7, 2),
        INT(8, 2),
        LUK(9, 2),
        HP(10, 4),
        MAX_HP(11, 4),
        MP(12, 4),
        MAX_MP(13, 4),
        AP(14, 2),
        EXP(16, 8),
        POP(17, 4),
        /** Read after the whole DecodeChangeStat block (5EEA449), not at its bit position. */
        MONEY(18, 8);

        private final int bit;
        private final int width;

        Stat(int bit, int width) {
            this.bit = bit;
            this.width = width;
        }

        public long mask() {
            return 1L << bit;
        }

        public int bit() {
            return bit;
        }

        public int width() {
            return width;
        }
    }

    /** LP009D with HP and MP only (mask 0x1400). */
    public static OutPacket hpMpChanged(boolean exclRequest, int hp, int mp) {
        Map<Stat, Long> stats = new EnumMap<>(Stat.class);
        stats.put(Stat.HP, (long) hp);
        stats.put(Stat.MP, (long) mp);
        return statChanged(exclRequest, stats);
    }

    /** LP009D with no stat (mask 0): only releases the request lock when {@code exclRequest} is set. */
    public static OutPacket unlock() {
        return statChanged(true, Map.of());
    }

    /** LP009D for the given stats; values are range-checked against each stat's width. */
    public static OutPacket statChanged(boolean exclRequest, Map<Stat, Long> stats) {
        return statChanged(exclRequest, stats, 0, 0);
    }

    /**
     * As above with the HP / MP recovery numbers the client shows over the character (5EEA5C1 updateCovery, then
     * 5EEA5CD / 5EEA5D7 i32 hp, i32 mp - ingame-layouts-282.md 5 #8/#9); both 0 = none (placeholder-audit-0929 #4).
     */
    public static OutPacket statChanged(boolean exclRequest, Map<Stat, Long> stats, int hpRecovery, int mpRecovery) {
        Objects.requireNonNull(stats, "stats");
        EnumMap<Stat, Long> sorted = stats.isEmpty() ? new EnumMap<>(Stat.class) : new EnumMap<>(stats);
        long mask = 0L;
        for (Map.Entry<Stat, Long> e : sorted.entrySet()) {
            Objects.requireNonNull(e.getValue(), e.getKey() + " value");
            checkRange(e.getKey(), e.getValue());
            mask |= e.getKey().mask();
        }
        OutPacket out = Tms282LoginPacketSupport.packet(STAT_CHANGED);
        out.encodeByte(exclRequest ? 1 : 0);
        out.encodeByte(0);
        out.encodeByte(1);
        out.encodeLong(mask);
        for (Map.Entry<Stat, Long> e : sorted.entrySet()) {
            if (e.getKey() != Stat.MONEY) {
                write(out, e.getKey().width(), e.getValue());
            }
        }
        Long money = sorted.get(Stat.MONEY);
        if (money != null) {
            out.encodeLong(money);
        }
        out.encodeByte(0); // no charmOld
        boolean recovery = hpRecovery > 0 || mpRecovery > 0;
        out.encodeByte(recovery ? 1 : 0);
        if (recovery) {
            out.encodeInt(Math.max(0, hpRecovery));
            out.encodeInt(Math.max(0, mpRecovery));
        }
        return out;
    }

    private static void write(OutPacket out, int width, long value) {
        switch (width) {
            case 2 -> out.encodeShort((short) value);
            // encodeInt(long): OutPacket.encodeInt(int) silently drops the value -88888.
            case 4 -> out.encodeInt(value);
            case 8 -> out.encodeLong(value);
            default -> throw new IllegalStateException("width " + width);
        }
    }

    private static void checkRange(Stat stat, long value) {
        boolean ok = switch (stat.width()) {
            case 2 -> value >= Short.MIN_VALUE && value <= 0xFFFF;
            case 4 -> value >= Integer.MIN_VALUE && value <= 0xFFFFFFFFL;
            default -> true;
        };
        if (!ok) {
            throw new IllegalArgumentException(stat + " value " + value + " does not fit " + stat.width() + " bytes");
        }
    }
}
