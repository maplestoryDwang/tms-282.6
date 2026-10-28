package net.swordie.ms.connection.packet;

import java.util.HexFormat;

/**
 * Character-interaction requests of TMS 282.3 and their 265 bodies (UserHandler.handleUserEmotion /
 * handleUserPortableChairSitRequest). Senders from research/tms2823/daily-trace/senders.py.
 */
public final class Tms282InteractionRequests {
    private Tms282InteractionRequests() {
    }

    /** Native 5EE2460: CP0126 wings 5010087..89, CP0127 tails 5010090..92; zero clears. */
    public record KaiserAppearance(int itemId, boolean tail) {
        public static final int WING_OPCODE = 0x0126;
        public static final int TAIL_OPCODE = 0x0127;

        public static KaiserAppearance parse(byte[] packet, int opcode) {
            if (opcode != WING_OPCODE && opcode != TAIL_OPCODE) {
                throw new IllegalArgumentException("Not a Kaiser appearance opcode");
            }
            var in = Tms282FieldRequestReader.open(packet, opcode, "Kaiser appearance");
            int item = in.i32();
            in.end();
            int first = opcode == WING_OPCODE ? 5010087 : 5010090;
            if (item != 0 && (item < first || item > first + 2)) {
                throw new IllegalArgumentException("Invalid Kaiser appearance item " + item);
            }
            return new KaiserAppearance(item, opcode == TAIL_OPCODE);
        }

        public byte[] toGms265Body() {
            return new Gms265BodyWriter().i32(itemId).bytes();
        }
    }

    /** CP07A9: cached quick-move list version, u16 at senders 4E5371C / 4E54D78. */
    public record QuickMove(int cachedVersion) {
        public static final int OPCODE = 0x07A9;

        public static QuickMove parse(byte[] packet) {
            Tms282FieldRequestReader in = Tms282FieldRequestReader.open(packet, OPCODE, "CP07A9");
            QuickMove request = new QuickMove(in.i16() & 0xffff);
            in.end();
            return request;
        }

        public byte[] toGms265Body() {
            return new Gms265BodyWriter().u16(cachedVersion).bytes();
        }
    }

    /**
     * CP011A USER_EMOTION {@code u32 emotion, u32 duration, u8 byItemOption} [證實: senders 5EDDCA0 / 5EE1EA0 encode
     * 4 4 1; the same as the 265 decode].
     */
    public record Emotion(int emotion, int duration, int byItemOption) {
        public static final int OPCODE = 0x011A;

        public static Emotion parse(byte[] packet) {
            Tms282FieldRequestReader in = Tms282FieldRequestReader.open(packet, OPCODE, "CP011A");
            Emotion e = new Emotion(in.i32(), in.i32(), in.u8());
            in.end();
            return e;
        }

        public byte[] toGms265Body() {
            return new Gms265BodyWriter().i32(emotion).i32(duration).u8(byItemOption).bytes();
        }
    }

    /**
     * CP0107 USER_SIT_REQUEST {@code u16 fieldSeatId} (-1 = stand up) [證實: senders 57BF160 / 5E0B1A0 / 5E1CD50 encode
     * 2; the same as the 265 decode].
     */
    public record Sit(int fieldSeatId) {
        public static final int OPCODE = 0x0107;

        public static Sit parse(byte[] packet) {
            Tms282FieldRequestReader in = Tms282FieldRequestReader.open(packet, OPCODE, "CP0107");
            Sit s = new Sit(in.i16());
            in.end();
            return s;
        }

        public byte[] toGms265Body() {
            return new Gms265BodyWriter().u16(fieldSeatId).bytes();
        }
    }

    /**
     * CP0108 USER_PORTABLE_CHAIR_SIT_REQUEST [證實: sender 5E1B9B0, COutPacket(0x108) at 5E1CA53]:
     * <pre>
     * u32 fieldId (field vtable), u32 itemId, u32 slot, u8 inBag,
     * u32 extra        282 only ([rbp+0x5D0], meaning unknown; not in 265)
     * u32 x, u32 y     user position
     * ...              chair-type data written by the chair object (58643B0 -> vtable+0x48: text, meso, ...)
     * [8 bytes]        only for items 3020000..3029999 (Buf 8 from the item's data)
     * </pre>
     * The 265 body is the same without {@code extra}; the chair-type data is passed on unchanged.
     */
    public record PortableChairSit(int fieldId, int itemId, int slot, int inBag, int extra, int x, int y,
                                   byte[] chairData) {
        public static final int OPCODE = 0x0108;

        public PortableChairSit {
            chairData = chairData.clone();
        }

        @Override
        public byte[] chairData() {
            return chairData.clone();
        }

        public static PortableChairSit parse(byte[] packet) {
            Tms282FieldRequestReader in = Tms282FieldRequestReader.open(packet, OPCODE, "CP0108");
            return new PortableChairSit(in.i32(), in.i32(), in.i32(), in.u8(), in.i32(), in.i32(), in.i32(), in.rest());
        }

        public byte[] toGms265Body() {
            byte[] head = new Gms265BodyWriter().i32(fieldId).i32(itemId).i32(slot).u8(inBag).i32(x).i32(y).bytes();
            byte[] body = new byte[head.length + chairData.length];
            System.arraycopy(head, 0, body, 0, head.length);
            System.arraycopy(chairData, 0, body, head.length, chairData.length);
            return body;
        }

        @Override
        public String toString() {
            return String.format("CP0108 chair %d slot %d bag %d field %d extra %d at (%d,%d) data[%d]=%s", itemId, slot,
                    inBag, fieldId, extra, x, y, chairData.length, HexFormat.of().formatHex(chairData));
        }
    }
}
