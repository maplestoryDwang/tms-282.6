package net.swordie.ms.connection.packet;

import net.swordie.ms.client.character.tms282.Tms282VMatrixState;
import net.swordie.ms.connection.OutPacket;
import java.util.TreeMap;
import java.util.TreeSet;

/** LP020D full core snapshot. The decoder clears the whole map; never send a partial projection. */
public final class Tms282VMatrixPackets {
    private Tms282VMatrixPackets() { }

    public static OutPacket update(Tms282VMatrixState state, boolean includeRetainedSet) {
        var out = Tms282LoginPacketSupport.packet(0x020D);
        out.encodeInt(state.cores().size());
        for (var core : new TreeMap<>(state.cores()).values()) {
            out.encodeInt(core.id());
            out.encodeInt(core.id());
            out.encodeInt(core.level());
            out.encodeByte(core.selected());
            out.encodeLong(core.expiresAt());
        }
        // The set's full UI meaning is still under investigation; round-trip stored state, never infer its IDs.
        out.encodeByte(includeRetainedSet);
        if (includeRetainedSet) {
            out.encodeInt(state.retainedSet().size());
            for (int id : new TreeSet<>(state.retainedSet())) out.encodeInt(id);
        }
        out.encodeByte(0); // no optional notification i32
        return out;
    }
}
