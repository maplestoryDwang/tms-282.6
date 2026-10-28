package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.handlers.header.OutHeader;

import java.util.Optional;

/**
 * Skill-system 265 packets whose body the 282 client reads as is (or after a fixed reorder), re-stamped with the
 * 282 opcode just before the fail-closed guard ({@link Tms282LegacyPackets}). Each rule was traced against the
 * client with the 265 builder's own output (skill2-trace/README.md; tools/Tms282SkillSystemCheck.java):
 * <ul>
 *     <li>STAT_CHANGED -> LP009D (5EEA1A0): the 265 per-stat widths equal 282 for mask bits 0..18 and the six
 *     trait EXPs (20..25), job (u16 job, u16 subJob) and both SP forms included. Two differences: 282 reads money
 *     (bit 18) after the whole block (5EEA449), so a 265 body with money and a trait is reordered; 282 does not
 *     read fatigue (bit 19) and the higher bits, and 265 sign-extends the mask for eventPoints (bit 31), so those
 *     bodies are declined (dropped by the guard as before). A body with money, a trait and SP is declined too (the
 *     SP width is not in the body).</li>
 *     <li>CHANGE_SKILL_RECORD_RESULT -> LP00A2 (5FE5D20): same body (u8 excl, u8 show, i16 n, n x (skill, level,
 *     master, FILETIME), u8 sn). Job advancement, quest / NPC skill grants and every 265 skill change reach the
 *     skill window through this.</li>
 *     <li>SUMMONED_REMOVED -> LP066F (45AB780 + 4521FD0): same body (i32 owner, i32 summoned, u8 leaveType).</li>
 *     <li>極限屬性 (hyperstat-282 research): HYPER_STATS_PRESET -> LP0269 (5E7F610: u8 preset, u8 action, action != 0:
 *     3 x (i32 n, n x (i32 index, i32 skillId, i32 level))), RECEIVE_HYPER_STAT_RESET_RESULT -> LP01FA (5F29D60) and
 *     HYPER_SKILL_RESET_RESULT -> LP014E (5F29C90), both u8 excl, i32 characterId, u8 success. Same bodies.</li>
 * </ul>
 * SUMMONED_CREATED differs and has its own codec ({@link Tms282SummonedPackets}).
 */
public final class Tms282SkillLegacyTranslators implements Tms282LegacyPackets.Contributor {
    public static final Tms282SkillLegacyTranslators INSTANCE = new Tms282SkillLegacyTranslators();

    public static final int STAT_CHANGED = Tms282StatPackets.STAT_CHANGED;
    public static final int CHANGE_SKILL_RECORD_RESULT = Tms282SkillPackets.CHANGE_SKILL_RECORD_RESULT;
    public static final int HYPER_STATS_PRESET = 0x0269;
    public static final int HYPER_STAT_RESET_RESULT = 0x01FA;
    public static final int HYPER_SKILL_RESET_RESULT = 0x014E;

    /** Mask bits whose 265 width is the 282 width: 0..2, 4..18 and the trait EXPs 20..25. */
    static final long SAME_WIDTH_BITS = 0x0007FFF7L | 0x03F00000L;
    static final long MONEY = 1L << 18;
    static final long SP = 1L << 15;
    static final long TRAITS = 0x03F00000L;

    private Tms282SkillLegacyTranslators() {
    }

    @Override
    public void contribute(Tms282LegacyPackets.Builder builder) {
        builder.on(OutHeader.STAT_CHANGED, Tms282SkillLegacyTranslators::statChanged)
                .on(OutHeader.CHANGE_SKILL_RECORD_RESULT, p -> copy(p, CHANGE_SKILL_RECORD_RESULT))
                .on(OutHeader.SUMMONED_REMOVED, p -> copy(p, Tms282SummonedPackets.SUMMONED_REMOVED))
                .on(OutHeader.HYPER_STATS_PRESET, p -> copy(p, HYPER_STATS_PRESET))
                .on(OutHeader.RECEIVE_HYPER_STAT_RESET_RESULT, p -> copy(p, HYPER_STAT_RESET_RESULT))
                .on(OutHeader.HYPER_SKILL_RESET_RESULT, p -> copy(p, HYPER_SKILL_RESET_RESULT));
    }

    /** The 265 body unchanged under the 282 opcode. */
    static Optional<OutPacket> copy(OutPacket legacy, int op) {
        byte[] body = Tms282LegacyPackets.Body.of(legacy).rest();
        OutPacket out = Tms282LoginPacketSupport.packet(op);
        out.encodeArr(body);
        return Optional.of(out);
    }

    static Optional<OutPacket> statChanged(OutPacket legacy) {
        Tms282LegacyPackets.Body body = Tms282LegacyPackets.Body.of(legacy);
        int excl = body.u8();
        int second = body.u8();
        int third = body.u8();
        long mask = body.i64();
        if ((mask & ~SAME_WIDTH_BITS) != 0) {
            return Optional.empty();
        }
        byte[] stats = body.rest();
        OutPacket out = Tms282LoginPacketSupport.packet(STAT_CHANGED);
        out.encodeByte(excl);
        out.encodeByte(second);
        out.encodeByte(third);
        out.encodeLong(mask);
        if ((mask & MONEY) == 0 || (mask & TRAITS) == 0) {
            out.encodeArr(stats);
            return Optional.of(out);
        }
        if ((mask & SP) != 0) {
            return Optional.empty();
        }
        // 265: ... money(8) traits(4 each) tail; 282: ... traits money tail
        int moneyAt = fixedWidthBefore(mask, 18);
        int traitBytes = Long.bitCount(mask & TRAITS) * 4;
        if (stats.length < moneyAt + 8 + traitBytes) {
            return Optional.empty();
        }
        out.encodeArr(java.util.Arrays.copyOfRange(stats, 0, moneyAt));
        out.encodeArr(java.util.Arrays.copyOfRange(stats, moneyAt + 8, moneyAt + 8 + traitBytes));
        out.encodeArr(java.util.Arrays.copyOfRange(stats, moneyAt, moneyAt + 8));
        out.encodeArr(java.util.Arrays.copyOfRange(stats, moneyAt + 8 + traitBytes, stats.length));
        return Optional.of(out);
    }

    /** Bytes the 265 builder writes for the mask bits below {@code bit} (SP excluded by the caller). */
    static int fixedWidthBefore(long mask, int bit) {
        int[] width = {5, 4, 4, 0, 4, 4, 2, 2, 2, 2, 4, 4, 4, 4, 2, 0, 8, 4};
        int n = 0;
        for (int b = 0; b < bit; b++) {
            if ((mask & (1L << b)) != 0) {
                n += width[b];
            }
        }
        return n;
    }
}
