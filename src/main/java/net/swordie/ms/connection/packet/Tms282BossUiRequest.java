package net.swordie.ms.connection.packet;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * CP02CB BOSS_UI_REQUEST [verified; 265 0x2BC]: the boss entry window's "enter" button. Sender 5F9C190 (next to the
 * LP00D7 BOSS_UI reader 5F9C950) writes {@code sub_52E9A0 = i32 orderId, u8 practice, u8, i32 difficulty, u8, u8,
 * u8} - the sequence 265 ScriptHandler.handleBossUIRequest decodes (ingame-cp-map-2823 0x02CB, exact).
 */
public record Tms282BossUiRequest(int orderId, int practice, int flag, int difficulty, int b1, int b2, int b3) {
    public static final int OPCODE = 0x02CB;

    public static Tms282BossUiRequest parse(byte[] packet) {
        Tms282FieldRequestReader in = Tms282FieldRequestReader.open(packet, OPCODE, "boss UI request");
        Tms282BossUiRequest r = new Tms282BossUiRequest(in.i32(), in.u8(), in.u8(), in.i32(), in.u8(), in.u8(), in.u8());
        in.end();
        return r;
    }

    // Original TMS282 BossParty.img uses Chaos(3) where the legacy server calls these Hard(2).
    public static int legacyDifficulty(int orderId, int difficulty) {
        return (orderId == 29 || orderId == 30) && difficulty == 3 ? 2 : difficulty;
    }

    public static int nativeDifficulty(int orderId, int difficulty) {
        return (orderId == 29 || orderId == 30) && difficulty == 2 ? 3 : difficulty;
    }

    /** The 265 BOSS_UI_REQUEST body (no opcode), with native difficulty IDs normalized. */
    public byte[] toGms265Body() {
        return ByteBuffer.allocate(13).order(ByteOrder.LITTLE_ENDIAN).putInt(orderId).put((byte) practice)
                .put((byte) flag).putInt(legacyDifficulty(orderId, difficulty))
                .put((byte) b1).put((byte) b2).put((byte) b3).array();
    }
}
