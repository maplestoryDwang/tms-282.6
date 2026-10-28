package net.swordie.ms.connection.packet;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * CP012B USER_SELECT_NPC [verified; 265 0x128] (senders 40C6E20, 586DBE0): {@code u32 npcObjectId, i16 x, i16 y,
 * i32 (282 only, -1 from both senders)}. 265 reads the first three.
 */
public record Tms282SelectNpcRequest(int objectId, short x, short y, int extra) {
    public static final int OPCODE = 0x012B;

    public static Tms282SelectNpcRequest parse(byte[] packet) {
        Tms282FieldRequestReader in = Tms282FieldRequestReader.open(packet, OPCODE, "select NPC request");
        int objectId = in.i32();
        short x = in.i16();
        short y = in.i16();
        int extra = in.i32();
        in.end();
        return new Tms282SelectNpcRequest(objectId, x, y, extra);
    }

    /** The 265 USER_SELECT_NPC body (no opcode): {@code i32 objectId, i16 x, i16 y}. */
    public byte[] toGms265Body() {
        return ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putInt(objectId).putShort(x).putShort(y).array();
    }
}
