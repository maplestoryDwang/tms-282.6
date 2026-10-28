package net.swordie.ms.connection.packet;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HexFormat;

/**
 * CP0131 USER_TRUNK_REQUEST (storage) [opcode verified: COutPacket(0x131) in the CTrunkDlg senders], u8 type (265
 * {@code TrunkType} request values) and a body per type, from the static senders:
 * <pre>
 *  4 get      (4627A20): u8 invType, u8 index, i32 position   265: u8, u8, i16 position
 *  5 find all (462ADB0 / 462B2E0): Buf4 x3                     265 reads nothing
 *  6 put      (4627C40): i16 slot, i32 itemId, i32 quantity    265: i16, i32, i16 quantity
 *  8 sort     (4627700 / 462B960): -
 *  9 money    (462B9E0 / 462BD60): i64 (negative = store)     as 265
 * 10 close    (4626060, 462CDB0): -
 *  3 check    (462D13F): kept as sent (265 has no case for it)
 * </pre>
 * {@link #gms265Body} is the body 265 {@code UserTrunkHandler.handleUserTrunkRequest} reads (type included); types
 * without a static sender (7 auto store) are passed on unchanged.
 */
public record Tms282TrunkRequest(int type, byte[] gms265Body, String describe) {
    public static final int OPCODE = 0x0131;

    public Tms282TrunkRequest {
        gms265Body = gms265Body.clone();
    }

    @Override
    public byte[] gms265Body() {
        return gms265Body.clone();
    }

    /** The full plaintext packet, including the opcode. */
    public static Tms282TrunkRequest parse(byte[] packet) {
        Tms282FieldRequestReader in = Tms282FieldRequestReader.open(packet, OPCODE, "CP0131");
        int type = in.u8();
        switch (type) {
            case 4 -> {
                int inv = in.u8();
                int index = in.u8();
                int position = in.i32();
                in.end();
                return new Tms282TrunkRequest(type, le(5).put((byte) type).put((byte) inv).put((byte) index)
                        .putShort(clampShort(position)).array(),
                        String.format("CP0131 trunk get inv=%d index=%d pos=%d", inv, index, position));
            }
            case 5 -> {
                byte[] filters = in.bytes(12);
                in.end();
                return new Tms282TrunkRequest(type, new byte[]{(byte) type},
                        "CP0131 trunk find all " + HexFormat.of().formatHex(filters));
            }
            case 6 -> {
                short slot = in.i16();
                int itemId = in.i32();
                int quantity = in.i32();
                in.end();
                return new Tms282TrunkRequest(type, le(9).put((byte) type).putShort(slot).putInt(itemId)
                        .putShort(clampShort(quantity)).array(),
                        String.format("CP0131 trunk put slot=%d item=%d quantity=%d", slot, itemId, quantity));
            }
            case 8, 10 -> {
                in.end();
                return new Tms282TrunkRequest(type, new byte[]{(byte) type}, "CP0131 trunk " + (type == 8 ? "sort" : "close"));
            }
            case 9 -> {
                long money = ByteBuffer.wrap(in.bytes(8)).order(ByteOrder.LITTLE_ENDIAN).getLong();
                in.end();
                return new Tms282TrunkRequest(type, le(9).put((byte) type).putLong(money).array(),
                        "CP0131 trunk money " + money);
            }
            default -> {
                byte[] rest = in.rest();
                byte[] body = new byte[1 + rest.length];
                body[0] = (byte) type;
                System.arraycopy(rest, 0, body, 1, rest.length);
                return new Tms282TrunkRequest(type, body, "CP0131 trunk type " + type + " " + HexFormat.of().formatHex(rest));
            }
        }
    }

    private static short clampShort(int v) {
        if (v < 0 || v > Short.MAX_VALUE) {
            throw new IllegalArgumentException("value out of the 265 i16 range: " + v);
        }
        return (short) v;
    }

    private static ByteBuffer le(int n) {
        return ByteBuffer.allocate(n).order(ByteOrder.LITTLE_ENDIAN);
    }

    @Override
    public String toString() {
        return describe;
    }
}
