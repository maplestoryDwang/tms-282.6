package net.swordie.ms.connection.packet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.util.Rect;

/** Adele-only LP0599 -> native58E3040 -> CP0137. LP057F is a different random-area attack. */
public final class Tms282AdeleShardPackets {
    public static final int REQUEST = 0x0599, REPLY = 0x0137;
    public static final int BASE = 151121041, HEXA = 151141017, MAX_AREAS = 7;
    private Tms282AdeleShardPackets() { }

    public static boolean handles(int skill) { return skill == BASE || skill == HEXA; }

    /** Immutable geometry. key and requestId are echoed unchanged by the native sender. */
    public record Area(int key, int left, int top, int right, int bottom, boolean hitMob) {
        public Area {
            if (key <= 0 || left > right || top > bottom)
                throw new IllegalArgumentException("Invalid Adele shard area");
        }
        public Rect rect() { return new Rect(left, top, right, bottom); }
    }

    public record Reply(int skillId, int requestId, List<Area> areas) {
        public Reply { areas = List.copyOf(areas); }
        public static Reply parse(byte[] packet) {
            var r = Tms282FieldRequestReader.open(packet, REPLY, "CP0137 Adele shards");
            int skill = r.i32(), request = r.i32(), count = r.i32();
            if (!handles(skill) || count < 1 || count > MAX_AREAS || r.remaining() != count * 23 + 4)
                throw new IllegalArgumentException("Invalid Adele shard source/count/length");
            var result = new ArrayList<Area>(count);
            var keys = new HashSet<Integer>();
            for (int i = 0; i < count; i++) {
                int key = r.i32();
                // 58E3123 selects both current Adele IDs and replaces terrain slices with one original rectangle.
                if (r.u16() != 1 || !keys.add(key)) throw new IllegalArgumentException("Invalid Adele shard rectangle count/key");
                int left = r.i32(), top = r.i32(), right = r.i32(), bottom = r.i32(), flag = r.u8();
                if (flag > 1) throw new IllegalArgumentException("Invalid Adele shard flag");
                result.add(new Area(key, left, top, right, bottom, flag != 0));
            }
            r.i32(); // Existing346A070 transport trailer.
            r.end();
            return new Reply(skill, request, result);
        }
    }

    public static OutPacket request(int skill, int requestId, List<Area> areas) {
        var snapshot = List.copyOf(areas);
        if (!handles(skill) || snapshot.isEmpty() || snapshot.size() > MAX_AREAS
                || snapshot.stream().map(Area::key).distinct().count() != snapshot.size())
            throw new IllegalArgumentException("Invalid Adele shard request");
        var out = Tms282LoginPacketSupport.packet(REQUEST);
        out.encodeInt(skill);
        out.encodeInt(requestId);
        out.encodeInt(snapshot.size());
        for (var area : snapshot) {
            out.encodeInt(area.key());
            area.rect().encode(out);
            out.encodeByte(area.hitMob());
        }
        return out;
    }
}
