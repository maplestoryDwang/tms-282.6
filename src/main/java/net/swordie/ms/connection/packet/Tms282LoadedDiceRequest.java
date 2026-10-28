package net.swordie.ms.connection.packet;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** Native58843D5: selected i32 face and opaque transport; an owned paid phase supplies authority. */
public record Tms282LoadedDiceRequest(int choice) {
    public static final int OPCODE = 0x03D5;

    public Tms282LoadedDiceRequest {
        if (choice < 1 || choice > 6) throw new IllegalArgumentException("Unsupported native dice face");
    }

    public static Tms282LoadedDiceRequest parse(byte[] wire) {
        if (wire == null || wire.length != 10) throw new IllegalArgumentException("Invalid CP03D5 length");
        ByteBuffer in = ByteBuffer.wrap(wire).order(ByteOrder.LITTLE_ENDIAN);
        if (Short.toUnsignedInt(in.getShort()) != OPCODE) throw new IllegalArgumentException("Invalid CP03D5 opcode");
        int choice = in.getInt();
        in.getInt(); // Transport is never a die face or permission.
        return new Tms282LoadedDiceRequest(choice);
    }
}
