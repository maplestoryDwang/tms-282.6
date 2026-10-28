package net.swordie.ms.connection.packet;

import net.swordie.ms.client.jobs.Job;
import net.swordie.ms.connection.OutPacket;

/** CP02F3 mirror-spider sequence start/end. Native 4565880/4566360 writes four i32s, no trailer. */
public record Tms282SummonSequenceRequest(int objectId, int phase, int skillId, int level) {
    public static final int OPCODE = 0x02F3, REMOTE = 0x0688;

    public static Tms282SummonSequenceRequest parse(byte[] packet) {
        var r = Tms282FieldRequestReader.open(packet, OPCODE, "CP02F3");
        var result = new Tms282SummonSequenceRequest(r.i32(), r.i32(), r.i32(), r.i32());
        r.end();
        if (result.objectId <= 0 || result.phase < 1 || result.phase > 2 || result.level < 1 || result.level > 30
                || result.skillId != Job.TRUE_ARACHNID_REFLECTION_SUMMON)
            throw new IllegalArgumentException("Unsupported summon sequence");
        return result;
    }

    /** 452AE30 passes phase1 to 4565880 and phase2 to 4566360 with this exact skill/level pair. */
    public OutPacket remote(int ownerId) {
        var out = Tms282LoginPacketSupport.packet(REMOTE);
        out.encodeInt(ownerId); out.encodeInt(objectId); out.encodeInt(phase); out.encodeInt(skillId); out.encodeInt(level);
        return out;
    }
}
