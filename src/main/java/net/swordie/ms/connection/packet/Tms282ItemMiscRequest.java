package net.swordie.ms.connection.packet;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Small item requests whose 282 layout equals the 265 one (the body is handed to the 265 handler unchanged after a
 * strict length check):
 * <ul>
 *     <li>CP0182 USER_LOCK_SORT_ITEM_REQUEST [verified: sender 5E09100 {@code i32 tick, i32 invType, i32 pos, u8 lock}];</li>
 *     <li>CP015A USER_CHAR_SLOT_INC_ITEM_USE_REQUEST [verified: sender 5E1F960 {@code u16 slot, i32 itemId}];</li>
 *     <li>CP0128 USER_UPGRADE_TOMB_EFFECT [verified: sender 5792AF0 {@code i32 itemId, i32 x, i32 y}]. A second
 *     sender (53282C0) writes a single u8 under the same opcode; that form is not the tomb effect and is refused.</li>
 * </ul>
 */
public record Tms282ItemMiscRequest(int opcode, byte[] body) {
    public static final int LOCK_SORT = 0x0182;
    public static final int CHAR_SLOT_INC = 0x015A;
    public static final int TOMB_EFFECT = 0x0128;

    public Tms282ItemMiscRequest {
        body = body.clone();
    }

    @Override
    public byte[] body() {
        return body.clone();
    }

    public static Tms282ItemMiscRequest parseLockSort(byte[] packet) {
        return exact(packet, LOCK_SORT, "CP0182", 13);
    }

    public static Tms282ItemMiscRequest parseCharSlotInc(byte[] packet) {
        return exact(packet, CHAR_SLOT_INC, "CP015A", 6);
    }

    public static Tms282ItemMiscRequest parseTombEffect(byte[] packet) {
        return exact(packet, TOMB_EFFECT, "CP0128", 12);
    }

    private static Tms282ItemMiscRequest exact(byte[] packet, int opcode, String name, int length) {
        Tms282FieldRequestReader in = Tms282FieldRequestReader.open(packet, opcode, name);
        byte[] body = in.rest();
        if (body.length != length) {
            throw new IllegalArgumentException(name + " body is " + body.length + " bytes, expected " + length);
        }
        return new Tms282ItemMiscRequest(opcode, body);
    }

    /** Little-endian i32 at {@code offset} of the body. */
    public int i32(int offset) {
        return ByteBuffer.wrap(body, offset, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    @Override
    public String toString() {
        return String.format("CP%04X %s", opcode, java.util.HexFormat.of().formatHex(body));
    }
}
