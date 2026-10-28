package net.swordie.ms.connection.packet;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * CP0351 CHECK_BOSS_PARTY_BY_SCRIPT [verified; 265 0x341, shift +0x10 between two anchors, senders 2CD31F0 and
 * 30C9F50 write {@code i32, i32, i32}]: {@code i32 orderId, i32 difficulty, i32} - the 265 layout.
 */
public record Tms282CheckBossPartyRequest(int orderId, int difficulty, int extra) {
    public static final int OPCODE = 0x0351;

    public static Tms282CheckBossPartyRequest parse(byte[] packet) {
        Tms282FieldRequestReader in = Tms282FieldRequestReader.open(packet, OPCODE, "check boss party request");
        Tms282CheckBossPartyRequest r = new Tms282CheckBossPartyRequest(in.i32(), in.i32(), in.i32());
        in.end();
        return r;
    }

    /** The 265 CHECK_BOSS_PARTY_BY_SCRIPT body (no opcode). */
    public byte[] toGms265Body() {
        return ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN).putInt(orderId)
                .putInt(Tms282BossUiRequest.legacyDifficulty(orderId, difficulty)).putInt(extra)
                .array();
    }
}
