package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.protocol.codec.Tms282MovePathCodec;
import net.swordie.ms.life.movement.MovementInfo;

import java.nio.charset.Charset;

/**
 * TMS 282.3 pet requests rewritten as the bodies of the unchanged 265 PetHandler (ingame-cp-map-2823.tsv; the 265
 * reads are the prefix of the 282 send sequences, the 282 extras are dropped):
 * <ul>
 *     <li>CP019B USER_ACTIVATE_PET_REQUEST [證實 exact, 5EE1730]: {@code i32 tick, i16 slot};</li>
 *     <li>CP014C USER_PET_FOOD_ITEM_USE_REQUEST [證實 exact]: {@code i32 tick, i16 slot, i32 item};</li>
 *     <li>CP02D6 PET_MOVE [推定: no static sender, 265 0x2C8 + the +0xE of 0x2C9..0x2F5]: {@code i32 idx, u8,
 *     CMovePath} (265 layout, 282 path elements);</li>
 *     <li>CP02D7 PET_ACTION [證實 exact, 41A6020]: {@code i32 idx, i32 tick, u8, u8, ?str};</li>
 *     <li>CP02D8 PET_INTERACTION_REQUEST [證實 prefix, 41A0510]: {@code i32 idx, u8, u8 command} + one 282 u8;</li>
 *     <li>CP02D9 PET_DROP_PICK_UP_REQUEST [證實 prefix, 41A2930]: {@code i32 idx, u8 fieldKey, i32 tick, i32,
 *     i16 x, i16 y, i32 dropId, i32} + 282 extras;</li>
 *     <li>CP02DA PET_STAT_CHANGE_ITEM_USE_REQUEST [證實 prefix, 5E0DDE0]: {@code u8, i32 tick, i16 slot, i32 item}
 *     + one 282 i32;</li>
 *     <li>CP02DD PET_OPEN_SHOP [證實 exact, 41AB4E0]: {@code i32 idx}.</li>
 * </ul>
 */
public final class Tms282PetRequests {
    public static final int ACTIVATE = 0x019B;
    public static final int FOOD = 0x014C;
    public static final int MOVE = 0x02D6;
    public static final int ACTION = 0x02D7;
    public static final int INTERACTION = 0x02D8;
    public static final int PICK_UP = 0x02D9;
    public static final int STAT_CHANGE_ITEM = 0x02DA;
    public static final int OPEN_SHOP = 0x02DD;

    private Tms282PetRequests() {
    }

    /** The 265 body for {@code packet} (282 opcode + body); IllegalArgumentException when it does not parse. */
    public static byte[] toGms265Body(int opcode, byte[] packet, Charset charset) {
        Tms282FieldRequestReader r = Tms282FieldRequestReader.open(packet, opcode, String.format("CP%04X pet", opcode));
        Gms265BodyWriter w = new Gms265BodyWriter();
        switch (opcode) {
            case ACTIVATE -> {
                w.i32(r.i32()).u16(r.i16());
                r.end();
            }
            case FOOD -> {
                w.i32(r.i32()).u16(r.i16()).i32(r.i32());
                r.end();
            }
            case MOVE -> {
                w.i32(r.i32()).u8(r.u8());
                MovementInfo path = r.path(Tms282MovePathCodec.INSTANCE);
                OutPacket p = new OutPacket();
                path.encode(p);
                w.raw(p.getData());
            }
            case ACTION -> {
                w.i32(r.i32()).i32(r.i32()).u8(r.u8()).u8(r.u8());
                w.str(r.remaining() >= 2 ? str(r, charset) : "");
            }
            case INTERACTION -> w.i32(r.i32()).u8(r.u8()).u8(r.u8());
            case PICK_UP -> w.i32(r.i32()).u8(r.u8()).i32(r.i32()).i32(r.i32()).u16(r.i16()).u16(r.i16())
                    .i32(r.i32()).i32(r.i32());
            case STAT_CHANGE_ITEM -> w.u8(r.u8()).i32(r.i32()).u16(r.i16()).i32(r.i32());
            case OPEN_SHOP -> {
                w.i32(r.i32());
                r.end();
            }
            default -> throw new IllegalArgumentException(String.format("CP%04X is not a pet request", opcode));
        }
        return w.bytes();
    }

    /** CP02D6 PET_MOVE [推定]: {@code i32 idx, u8, CMovePath} with the 282 path elements. */
    public record Move(int idx, int flag, MovementInfo path) {
        public static Move parse(byte[] packet) {
            Tms282FieldRequestReader r = Tms282FieldRequestReader.open(packet, MOVE, "CP02D6 pet move");
            int idx = r.i32();
            int flag = r.u8();
            return new Move(idx, flag, r.path(Tms282MovePathCodec.INSTANCE));
        }
    }

    static String str(Tms282FieldRequestReader r, Charset charset) {
        int n = r.u16();
        return new String(r.bytes(n), charset);
    }
}
