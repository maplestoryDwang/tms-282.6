package net.swordie.ms.connection.packet;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** Native5E23F40..5E243F3: CP02A3, i32 operation followed by zero/one/two i32 arguments. */
public record Tms282VMatrixRequest(int operation, int argument, int value) {
    public static final int OPCODE = 0x02A3;

    public static Tms282VMatrixRequest parse(byte[] packet) {
        if (packet == null || packet.length < 6) throw new IllegalArgumentException("Truncated CP02A3");
        var in = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
        if (Short.toUnsignedInt(in.getShort()) != OPCODE) throw new IllegalArgumentException("Expected CP02A3");
        int operation = in.getInt();
        int length = switch (operation) { case 0, 3, 4, 5 -> 10; case 1, 2 -> 14; case 6 -> 6;
            default -> throw new IllegalArgumentException("Unknown VMatrix operation"); };
        if (packet.length != length) throw new IllegalArgumentException("Invalid CP02A3 body length");
        int argument = length >= 10 ? in.getInt() : 0;
        int value = length == 14 ? in.getInt() : 0;
        if (length >= 10 && argument <= 0 || length == 14 && value <= 0)
            throw new IllegalArgumentException("VMatrix arguments must be positive");
        return new Tms282VMatrixRequest(operation, argument, value);
    }
}
