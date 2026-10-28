package net.swordie.ms.connection.packet;

/** Native Aran periodic Combo poll; client time is a transport hint, never server credit. */
public record Tms282AranTimerRequest(int skillId, int clientTick) {
    public static Tms282AranTimerRequest parse(byte[] packet) {
        if (packet == null || packet.length < 2) throw new IllegalArgumentException("Truncated Aran timer");
        int opcode = (packet[0] & 255) | (packet[1] & 255) << 8;
        if (opcode != 0x0407 && opcode != 0x0408) throw new IllegalArgumentException("Wrong Aran timer opcode");
        var reader = Tms282FieldRequestReader.open(packet, opcode, "Aran periodic Combo timer");
        int skill = reader.i32(), tick = reader.i32(), trailer = reader.i32();
        reader.end();
        if (skill != 21100019 || trailer != -1) throw new IllegalArgumentException("Unsupported Aran timer source/trailer");
        return new Tms282AranTimerRequest(skill, tick);
    }
}
