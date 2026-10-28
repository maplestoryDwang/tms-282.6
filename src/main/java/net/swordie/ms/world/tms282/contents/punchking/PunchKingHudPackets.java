package net.swordie.ms.world.tms282.contents.punchking;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.protocol.ProtocolVersion;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Set;

/**
 * The 282 packets of the punch-king field class (fieldType 236), from static analysis of MapleStory.exe 282.4
 * (research/tms2823/content-1005/feasibility.md section 1). Neither packet is live-verified.
 * <ul>
 *     <li>CP0685 "field ready": the field class (ctor 39CFEB0, vtable 865DFE0) loads the HUD from the map's
 *     {@code info/MainUI_UOL} in Init 39D01E0 and, on its first Update (39D03F0 -> 782A80), sends
 *     {@code COutPacket(0x685); Encode4(0)} once. No exclusive lock is taken, so no reply is required.</li>
 *     <li>LP0833 HUD update: field OnPacket 39D0420 -> component field+0x960 -> 782BB0, {@code Decode4 sub} and the
 *     jump table at 782CE4. The read order per sub is certain ({@link #ARITY}); the meaning is not:
 *     sub 0 state (3 refreshes the HUD, 5 calls 3744CC0), sub 2 score (field+0x97C, drawn in 39D3F60), sub 3
 *     (mob id, value) number popup over a mob, sub 4 seconds = field clock set and start (3489560 / 34898E0) are
 *     "medium" confidence; subs 1, 5..8 are "low".</li>
 * </ul>
 * The HUD only exists in a fieldType-236 map ({@link #FIELDS}); 993222200 世界最強拳擊王 uses
 * {@code UI/UIWindow5.img/worldBest_PunchKing}.
 */
public final class PunchKingHudPackets {
    public static final int FIELD_READY_REQUEST = 0x0685;
    public static final int HUD = 0x0833;

    /** Number of i32 after {@code sub}, by sub (282.4 jump table 782CE4). */
    static final int[] ARITY = {1, 1, 1, 2, 1, 1, 3, 4, 11};

    public static final int SUB_STATE = 0;
    public static final int SUB_SCORE = 2;
    public static final int SUB_MOB_VALUE = 3;
    public static final int SUB_CLOCK = 4;

    /** Every fieldType-236 map of the 282 Map.wz (data282-import/snap/282-map.tsv). */
    public static final Set<Integer> FIELDS = Set.of(555003300, 867154200, 867158200, 867161100, 867161300, 867164900,
            867169200, 941000200, 993111400, 993111700, 993176600, 993176700, 993177300, 993184300, 993186200, 993189300,
            993191600, 993192800, 993197200, 993200200, 993222200, 993225300, 993229100, 993234600, 993234900, 993238200,
            993244200, 993246400, 993247200, 993248200, 993255200, 993255900, 993256601, 993263600, 993268200, 993277200,
            993299600);

    private PunchKingHudPackets() {
    }

    public static boolean isHudField(int fieldId) {
        return FIELDS.contains(fieldId);
    }

    /** Number of i32 arguments sub {@code sub} reads, or -1 for a sub the client does not handle. */
    public static int arity(int sub) {
        return sub >= 0 && sub < ARITY.length ? ARITY[sub] : -1;
    }

    /** LP0833: i32 sub, then exactly {@link #arity(int)} i32. */
    public static OutPacket hud(int sub, int... args) {
        int n = arity(sub);
        if (n < 0) {
            throw new IllegalArgumentException("LP0833: sub " + sub + " is not handled by the client (0..8)");
        }
        if (args == null || args.length != n) {
            throw new IllegalArgumentException("LP0833: sub " + sub + " reads " + n + " int(s), got "
                    + (args == null ? 0 : args.length));
        }
        OutPacket out = OutPacket.forProfile(ProtocolVersion.TMS_282_3, HUD);
        out.encodeInt(sub);
        for (int a : args) {
            out.encodeInt(a);
        }
        return out;
    }

    public static OutPacket state(int state) {
        return hud(SUB_STATE, state);
    }

    public static OutPacket score(int score) {
        return hud(SUB_SCORE, score);
    }

    public static OutPacket mobValue(int mobId, int value) {
        return hud(SUB_MOB_VALUE, mobId, value);
    }

    public static OutPacket clock(int seconds) {
        if (seconds < 0) {
            throw new IllegalArgumentException("LP0833 clock: negative seconds " + seconds);
        }
        return hud(SUB_CLOCK, seconds);
    }

    /** CP0685 body: i32 (0 in every sender found); a trailing u32 socket tail is tolerated. */
    public record FieldReadyRequest(int value) {
        public static FieldReadyRequest parse(byte[] packet) {
            if (packet == null || packet.length < 2) {
                throw new IllegalArgumentException("CP0685: no opcode");
            }
            int op = (packet[0] & 0xFF) | (packet[1] & 0xFF) << 8;
            if (op != FIELD_READY_REQUEST) {
                throw new IllegalArgumentException(String.format("CP0685: opcode %04X", op));
            }
            int body = packet.length - 2;
            if (body != 4 && body != 8) {
                throw new IllegalArgumentException("CP0685: body of " + body + " bytes");
            }
            return new FieldReadyRequest(ByteBuffer.wrap(packet, 2, 4).order(ByteOrder.LITTLE_ENDIAN).getInt());
        }
    }
}
