package net.swordie.ms.connection.packet;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** CP01E1 native empty sender body + SendPacket's captured i32 -1 transport tail. */
public final class Tms282ZeroAssistRequest {
    public static final int OPCODE = 0x01E1;
    private Tms282ZeroAssistRequest() { }
    public static void validate(byte[] packet) {
        if (packet == null || packet.length != 6) throw new IllegalArgumentException("CP01E1 requires empty body and four-byte transport tail");
        ByteBuffer b = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
        if (Short.toUnsignedInt(b.getShort()) != OPCODE || b.getInt() != -1)
            throw new IllegalArgumentException("CP01E1 opcode/transport tail differs from native captured layout");
    }
}
