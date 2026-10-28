package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;

/** Captured movement effects and area expiry; these requests never authorize damage or teleportation. */
public final class Tms282VisualSkillRequests {
    private Tms282VisualSkillRequests() { }

    public record MovementEffect(int skillId, int reportedLevel, boolean local, int[] positions) {
        public MovementEffect { positions = positions.clone(); }
        @Override public int[] positions() { return positions.clone(); }
        public static MovementEffect parse(byte[] packet) {
            var in = Tms282FieldRequestReader.open(packet, 0x0134, "CP0134");
            int skill = in.i32(), level = in.u8(), local = in.u8();
            if (level == 0 || local > 1) throw new IllegalArgumentException("Invalid effect level/flag");
            int count;
            if (skill == 400001000) { // Rope Lift: 5A16291/29D are checksums, not coordinates.
                in.i32(); in.i32(); count = 3;
            } else if (skill == 152001004) {
                count = 4;
            } else throw new IllegalArgumentException("Untraced movement-effect skill " + skill);
            int[] positions = new int[count];
            for (int i=0; i<count; i++) {
                positions[i] = in.i32();
                if (positions[i]<Short.MIN_VALUE || positions[i]>Short.MAX_VALUE)
                    throw new IllegalArgumentException("Movement effect outside native field coordinates");
            }
            in.end(); // CP0134 has no transport trailer, including the rope variant.
            return new MovementEffect(skill, level, local != 0, positions);
        }
        public OutPacket effect(int owner, int characterLevel, int level, boolean remote) {
            OutPacket out = Tms282LoginPacketSupport.packet(remote ? 0x048E : 0x04CA);
            if (remote) out.encodeInt(owner);
            out.encodeByte(1); out.encodeInt(skillId); out.encodeInt(characterLevel); out.encodeInt(level);
            for (int value : positions) out.encodeInt(value);
            return out;
        }
    }

    public record AreaEnd(int skillId) {
        public static AreaEnd parse(byte[] packet) {
            var in = Tms282FieldRequestReader.open(packet, 0x0271, "CP0271");
            int skill = in.i32(); in.end();
            if (skill <= 0) throw new IllegalArgumentException("Invalid area skill");
            return new AreaEnd(skill);
        }
    }

    public record MovingPrepare(int skillId, short mask, int speed) {
        public static MovingPrepare parse(byte[] packet) {
            var in = Tms282FieldRequestReader.open(packet, 0x0114, "CP0114");
            var request = new MovingPrepare(in.i32(), in.i16(), in.u8()); in.end();
            if (request.skillId < 0) throw new IllegalArgumentException("Invalid moving skill");
            return request;
        }
    }
}
