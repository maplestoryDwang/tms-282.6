package net.swordie.ms.connection.packet;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * CP035D special exit (senders 287F540, 4A3FA60, 4A3FEE0; static sequence i32, i32). Live 2026-10-07: the 離開 button
 * of 燃燒衝刺班「帽子裡的世界」993236800 sent {@code 0E 00 00 00 5F 01 00 00} twice and nothing answered. The body is the
 * 265 USER_SPECIAL_EXIT (845) body: i32 type (14 = Tera Blink), i32 value.
 */
public record Tms282SpecialExitRequest(int type, int value) {
    public static final int OPCODE = 0x035D;

    public static Tms282SpecialExitRequest parse(byte[] packet) {
        var in = Tms282FieldRequestReader.open(packet, OPCODE, "special exit request");
        int type = in.i32();
        int value = in.i32();
        in.end();
        return new Tms282SpecialExitRequest(type, value);
    }

    /** The 265 body (no opcode) for UserHandler.handleUserSpecialExit. */
    public byte[] toGms265Body() {
        return ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putInt(type).putInt(value).array();
    }
}
