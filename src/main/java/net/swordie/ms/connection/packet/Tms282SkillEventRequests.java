package net.swordie.ms.connection.packet;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * Skill-event phase (2026-09-29) client requests, parsed strictly (research/tms2823/skill-events/README.md).
 * <ul>
 *     <li>CP019D USER_CHECK_TEMPORARY_STAT_DURATION_REQUEST, sender 5FE82C0: the client asks
 *     SecondaryStat 1503A20 for the stats whose local duration has run out, and while that 152-byte CTS mask
 *     (38 ints, bit k = int[k >> 5] & 1 << (k & 31), the LP009E/LP009F mask layout) is not empty it sends
 *     {@code Buf152 mask} + the 346A070 tail (live: i32 -1) at most every 200 ms ([ctx+0xB4]). Nothing clears a
 *     pending flag: the client repeats until a LP009F removes the stats (live 09-29: 12247 requests, all bit 50
 *     IndieNotDamaged).</li>
 *     <li>CP0527 MOB_LIFTING_END (推定: 265 enum order MOB_EXPLOSION_START 0x526 -> MOB_LIFTING_END ->
 *     MOB_UPDATE_FIXED_MOVE_DIR -> MOB_CREATE_FIRE_WALK 0x529), sender 3EA0870: a CMob resets its local "lift"
 *     state ([mob+0x230..0x270], armed by 3EA0130 with rise / hold / fall times) and, when it was active, sends
 *     {@code i32 mobObjId} ([mob+0x558]). No reply; the 265 handler reads another body (u8, i16, u8).</li>
 * </ul>
 */
public final class Tms282SkillEventRequests {
    public static final int CHECK_DURATION = 0x019D;
    public static final int MOB_LIFTING_END = 0x0527;
    public static final int MASK_BYTES = Tms282CtsTable.MASK_INTS * 4;

    private Tms282SkillEventRequests() {
    }

    private static ByteBuffer body(byte[] packet, int op, int length) {
        if (packet == null || packet.length < 2) {
            throw new IllegalArgumentException("packet without opcode");
        }
        int got = (packet[0] & 0xFF) | (packet[1] & 0xFF) << 8;
        if (got != op) {
            throw new IllegalArgumentException(String.format("opcode CP%04X, expected CP%04X", got, op));
        }
        if (packet.length - 2 != length) {
            throw new IllegalArgumentException("body " + (packet.length - 2) + " bytes, expected " + length);
        }
        return ByteBuffer.wrap(packet, 2, length).order(ByteOrder.LITTLE_ENDIAN);
    }

    /** CP019D: the expired-stat mask and the 4-byte tail. */
    public record CheckDuration(int[] mask, int tail) {
        public CheckDuration {
            if (mask.length != Tms282CtsTable.MASK_INTS) {
                throw new IllegalArgumentException("mask has " + mask.length + " ints");
            }
            mask = mask.clone();
        }

        public static CheckDuration parse(byte[] packet) {
            ByteBuffer in = body(packet, CHECK_DURATION, MASK_BYTES + 4);
            int[] mask = new int[Tms282CtsTable.MASK_INTS];
            for (int i = 0; i < mask.length; i++) {
                mask[i] = in.getInt();
            }
            return new CheckDuration(mask, in.getInt());
        }

        @Override
        public int[] mask() {
            return mask.clone();
        }

        /** Set 282 CTS bits, ascending. */
        public List<Integer> bits() {
            List<Integer> bits = new ArrayList<>();
            for (int k = 0; k <= Tms282CtsTable.MAX_BIT; k++) {
                if ((mask[k >> 5] & (1 << (k & 31))) != 0) {
                    bits.add(k);
                }
            }
            return bits;
        }
    }

    /** CP0527: the mob whose lift state ended. */
    public record MobLiftingEnd(int mobObjId) {
        public static MobLiftingEnd parse(byte[] packet) {
            return new MobLiftingEnd(body(packet, MOB_LIFTING_END, 4).getInt());
        }
    }
}
