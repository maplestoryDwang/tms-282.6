package net.swordie.ms.connection.packet;

import java.util.List;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.packet.Tms282SkillRingRequest.Rule;

/** LP0AC8 -> 231CAC0(reason17) ->455110(kind0) ->456660(count, rules).
 * Runtime sends catalogs only when the separately deployed skillring feature is enabled.
 */
public final class Tms282SkillRingPackets {
    public static final int OPCODE = 0x0AC8;
    private Tms282SkillRingPackets() { }

    public static OutPacket grindstoneTable(List<Rule> rules) {
        List<Rule> snapshot = List.copyOf(rules);
        OutPacket out = Tms282LoginPacketSupport.packet(OPCODE);
        out.encodeInt(17);
        out.encodeInt(0);
        out.encodeInt(snapshot.size());
        for (Rule row : snapshot) {
            out.encodeInt(row.stoneItemId());
            out.encodeInt(row.useCount());
            out.encodeInt(row.prop());
            out.encodeLong(row.meso());
            out.encodeInt(row.ringItemId());
            out.encodeInt(row.ringFromLevel());
            out.encodeInt(row.ringToLevel());
        }
        return out;
    }

    /** 231CAC0 routes 0/1 through event key0x1A; ring callback234C850 maps 0 to its success animation.
     * Inventory/money packets must be sent before this result, after durable completion.
     */
    public static OutPacket result(boolean success) {
        OutPacket out = Tms282LoginPacketSupport.packet(OPCODE);
        out.encodeInt(success ? 0 : 1);
        return out;
    }

    /** Native reason3 routes an error notification and clears the request lock (231CB2C). */
    public static OutPacket rejected() {
        OutPacket out = Tms282LoginPacketSupport.packet(OPCODE);
        out.encodeInt(3);
        return out;
    }
}
