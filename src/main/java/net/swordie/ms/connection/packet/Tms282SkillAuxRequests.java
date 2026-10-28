package net.swordie.ms.connection.packet;

/** Strict, whole-frame readers for captured auxiliary skill requests. */
public final class Tms282SkillAuxRequests {
    private Tms282SkillAuxRequests() { }

    public record MovingPrepare(int skillId) {
        public static MovingPrepare parse(byte[] packet) {
            var r = Tms282FieldRequestReader.open(packet, 0x04BA, "CP04BA");
            int skill = r.i32(); r.i32(); r.end(); // transport trailer, not mask/action speed
            if (skill <= 0) throw new IllegalArgumentException("Invalid moving-shoot skill");
            return new MovingPrepare(skill);
        }
    }
    public record BulletReload(int skillId, int flag) {
        public static BulletReload parse(byte[] packet) {
            var r = Tms282FieldRequestReader.open(packet, 0x040B, "CP040B");
            int skill = r.i32(), flag = r.u8(); r.i32(); r.end();
            if (flag > 1) throw new IllegalArgumentException("Invalid reload flag");
            return new BulletReload(skill, flag);
        }
    }
    public record SummonAction(int objectId, int action) {
        public static SummonAction parse(byte[] packet) {
            var r = Tms282FieldRequestReader.open(packet, 0x02F0, "CP02F0");
            var request = new SummonAction(r.i32(), r.u8()); r.end();
            return request;
        }
    }
    public record SummonReaction(int objectId, int tick, int summonSkill, int attackSkill, int triggerSkill, int flag) {
        public static SummonReaction parse(byte[] packet) {
            var r = Tms282FieldRequestReader.open(packet, 0x02F2, "CP02F2");
            var request = new SummonReaction(r.i32(), r.i32(), r.i32(), r.i32(), r.i32(), r.u8());
            // Native 346A070 does not append a trailer to 02F2 (all live captures are 23 bytes).
            r.end();
            if (request.flag > 1) throw new IllegalArgumentException("Invalid reaction flag");
            return request;
        }
    }
}
