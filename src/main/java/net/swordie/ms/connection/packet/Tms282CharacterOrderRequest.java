package net.swordie.ms.connection.packet;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashSet;

/** CP00C7, native 217AE00: user id, int count, ordered character ids; no reply is read. */
public record Tms282CharacterOrderRequest(int userId, int[] characterIds) {
    public static final int OPCODE = 0x00C7;

    public Tms282CharacterOrderRequest {
        characterIds = characterIds.clone();
        var unique = new HashSet<Integer>();
        for (int id : characterIds) {
            if (id <= 0 || !unique.add(id)) throw new IllegalArgumentException("Invalid or duplicate character id");
        }
    }

    @Override public int[] characterIds() { return characterIds.clone(); }

    public static Tms282CharacterOrderRequest parse(byte[] packet) {
        if (packet == null || packet.length < 10) throw new IllegalArgumentException("Truncated CP00C7");
        var in = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
        if (Short.toUnsignedInt(in.getShort()) != OPCODE) throw new IllegalArgumentException("Wrong CP00C7 opcode");
        int userId = in.getInt(), count = in.getInt();
        if (count < 0 || count > 255 || in.remaining() != count * 4)
            throw new IllegalArgumentException("CP00C7 count does not match packet length");
        int[] ids = new int[count];
        for (int i = 0; i < count; i++) ids[i] = in.getInt();
        return new Tms282CharacterOrderRequest(userId, ids);
    }
}
