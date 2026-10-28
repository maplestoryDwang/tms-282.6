package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;

import java.util.Map;

/**
 * 282-only Battle Mage packets. LP05F2 (CUserLocal 592EB70) is the Dark Lightning mark object of the local user
 * (class 82C4F0, skills 32111016 / 32110020 / 32141005): u8 0 clears it (592F740). u8 1 is followed by two mob
 * object-id lists (i32 n, n x i32; i32 m, m x i32), then 82D110: i32 a, a x (i32 mob, i32 elapsed ms, i32 value) and
 * i32 b, b x (i32 mob, i32, i32 elapsed ms, i32 value). The read layout is traced; the meaning is 推定: first id
 * list + first timed list = the marked mobs (value = triggers left), the second pair (VI marks) stays empty.
 * Not live-tested.
 */
public final class Tms282BattleMagePackets {
    public static final int DARK_LIGHTNING_MARK = 0x05F2;

    private Tms282BattleMagePackets() { }

    /** {@code marks}: mob object id -> {applied at (ms), triggers left}. */
    public static OutPacket darkLightningMarks(Map<Integer, long[]> marks, long now) {
        OutPacket out = Tms282LoginPacketSupport.packet(DARK_LIGHTNING_MARK);
        if (marks == null || marks.isEmpty()) {
            out.encodeByte(0);
            return out;
        }
        out.encodeByte(1);
        out.encodeInt(marks.size());
        for (int id : marks.keySet()) {
            out.encodeInt(id);
        }
        out.encodeInt(0);
        out.encodeInt(marks.size());
        for (Map.Entry<Integer, long[]> e : marks.entrySet()) {
            out.encodeInt(e.getKey());
            out.encodeInt((int) Math.max(0, now - e.getValue()[0]));
            out.encodeInt((int) e.getValue()[1]);
        }
        out.encodeInt(0);
        return out;
    }
}
