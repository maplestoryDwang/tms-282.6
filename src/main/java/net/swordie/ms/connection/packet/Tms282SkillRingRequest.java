package net.swordie.ms.connection.packet;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** Native CP0498: 234CB50 -> Encode4(slot), 455240(rule). No tick or item serial.
 * The client echoes its displayed rule; every field must match server authority before charging.
 */
public record Tms282SkillRingRequest(int slot, Rule rule) {
    public static final int OPCODE = 0x0498;
    public static final int BODY_SIZE = 36;

    public record Rule(int stoneItemId, int useCount, int prop, long meso,
                       int ringItemId, int ringFromLevel, int ringToLevel) {
        public Rule {
            if (stoneItemId / 10000 != 253 || ringItemId / 10000 != 111 || useCount <= 0
                    || prop <= 0 || prop > 100 || meso < 0 || ringFromLevel <= 0
                    || ringToLevel != ringFromLevel + 1 || ringToLevel > 255) {
                throw new IllegalArgumentException("無效的戒指研磨規則。");
            }
        }
        /** Native455970/456870 count2539002 as the2539001 family, preserving each consumed stack's identity. */
        public boolean acceptsStone(int itemId) {
            return itemId==stoneItemId || (stoneItemId==2539001 && itemId==2539002);
        }
    }

    public Tms282SkillRingRequest {
        if (slot <= 0 || slot > Short.MAX_VALUE || rule == null)
            throw new IllegalArgumentException("請先將戒指放入裝備欄。");
    }

    public static Tms282SkillRingRequest parse(byte[] body) {
        if (body == null || body.length != BODY_SIZE)
            throw new IllegalArgumentException("CP0498 requires exactly 36 bytes");
        ByteBuffer in = ByteBuffer.wrap(body).order(ByteOrder.LITTLE_ENDIAN);
        return new Tms282SkillRingRequest(in.getInt(), new Rule(in.getInt(), in.getInt(), in.getInt(),
                in.getLong(), in.getInt(), in.getInt(), in.getInt()));
    }
}
