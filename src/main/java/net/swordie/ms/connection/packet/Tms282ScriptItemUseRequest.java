package net.swordie.ms.connection.packet;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * CP014E USER_SCRIPT_ITEM_USE_REQUEST [verified opcode; 265 0x149] (senders 1B374A0, 5E10790; ingame-cp-map-2823
 * "序列一致（exact）"): {@code i32 tick, i16 slot, i32 itemId, i32 quantity} - the 265 layout. Used by script
 * consumables such as the starter-pack control panel 2432022.
 */
public record Tms282ScriptItemUseRequest(int tick, short slot, int itemId, int quantity) {
    public static final int OPCODE = 0x014E;
    public static final int LENGTH = 2 + 14;

    public static Tms282ScriptItemUseRequest parse(byte[] packet) {
        Tms282FieldRequestReader in = Tms282FieldRequestReader.open(packet, OPCODE, "script item use request");
        int tick = in.i32();
        short slot = in.i16();
        int itemId = in.i32();
        int quantity = in.i32();
        in.end();
        return new Tms282ScriptItemUseRequest(tick, slot, itemId, quantity);
    }

    /** The 265 USER_SCRIPT_ITEM_USE_REQUEST body (no opcode), byte for byte the same fields. */
    public byte[] toGms265Body() {
        return ByteBuffer.allocate(14).order(ByteOrder.LITTLE_ENDIAN).putInt(tick).putShort(slot).putInt(itemId)
                .putInt(quantity).array();
    }
}
