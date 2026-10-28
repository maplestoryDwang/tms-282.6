package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;

/** CP03CF / LP041E: physical-object motion, not an attack. Native 43291E0/432F500 -> 56A1EA0. */
public record Tms282ProjectileMotionRequest(int objectId, int x, int y, int type, int targetX, int targetY,
                                            int option, int option2) {
    public static final int OPCODE = 0x03CF, REMOTE = 0x041E;

    public static Tms282ProjectileMotionRequest parse(byte[] packet) {
        var r = Tms282FieldRequestReader.open(packet, OPCODE, "CP03CF");
        var result = new Tms282ProjectileMotionRequest(r.i32(), r.i32(), r.i32(), r.i32(), r.i32(), r.i32(),
                r.i32(), r.i32());
        r.i32(); // 346A070 transport trailer
        r.end();
        if (result.objectId <= 0 || result.type < 1 || result.type > 2 || result.option < 0 || result.option > 60_000
                || result.option2 != 0)
            throw new IllegalArgumentException("Unsupported physical-object motion");
        for (int coordinate : new int[]{result.x, result.y, result.targetX, result.targetY})
            if (coordinate < Short.MIN_VALUE || coordinate > Short.MAX_VALUE)
                throw new IllegalArgumentException("Physical-object coordinate outside field range");
        return result;
    }

    /** CUserPool strips the owner's i32 before calling 56A1EA0 with these eight i32s. */
    public OutPacket remote(int ownerId) {
        var out = Tms282LoginPacketSupport.packet(REMOTE);
        out.encodeInt(ownerId);
        out.encodeInt(objectId); out.encodeInt(x); out.encodeInt(y); out.encodeInt(type);
        out.encodeInt(targetX); out.encodeInt(targetY); out.encodeInt(option); out.encodeInt(option2);
        return out;
    }
}
