package net.swordie.ms.connection.packet;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * CP0245 Monster Collection exploration request [opcode 推定, layout 證實 from the only sender 58AE070]:
 * {@code i32 a (the caller's argument), i32 b, i32 c} (the pair returned by the UI item's vtable +0x30), then
 * 346A070; the sender sets its wait flag (+0xA984) like a UI request answered by LP01D7 MONSTER_COLLECTION_RESULT,
 * whose handler 58AF770 sits next to it. Read as 265 MONSTER_COLLECTION_EXPLORE_REQ {@code region, session, group}
 * (same three i32); the ranges are checked so a different meaning is refused instead of starting an exploration.
 */
public record Tms282CollectionExploreRequest(int region, int session, int group) {
    public static final int OPCODE = 0x0245;

    public static Tms282CollectionExploreRequest parse(byte[] packet) {
        Tms282FieldRequestReader in = Tms282FieldRequestReader.open(packet, OPCODE, "collection exploration");
        int region = in.i32();
        int session = in.i32();
        int group = in.i32();
        in.end();
        if (region < 0 || region > 9999 || session < 0 || session > 99 || group < 0 || group > 99) {
            throw new IllegalArgumentException("collection exploration out of range: " + region + "/" + session + "/" + group);
        }
        return new Tms282CollectionExploreRequest(region, session, group);
    }

    /** The 265 body (no opcode): region, session, group. */
    public byte[] toGms265Body() {
        return ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN).putInt(region).putInt(session).putInt(group).array();
    }

    @Override
    public String toString() {
        return String.format("CP%04X collection exploration region=%d session=%d group=%d", OPCODE, region, session, group);
    }
}
