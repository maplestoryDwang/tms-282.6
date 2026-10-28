package net.swordie.ms.connection.packet;

/** 5900BC0: stack-unit key and client tick. Tick is an unsigned clock hint, never an aether amount. */
public record Tms282AdeleTimerRequest(boolean start, int clientTick) {
    public static final int PULSE_OPCODE = 0x0407;
    public static final int START_OPCODE = 0x0409;
    public static final int SKILL = 151100017;

    public static Tms282AdeleTimerRequest parse(byte[] packet) {
        if (packet == null || packet.length < 2) throw new IllegalArgumentException("Truncated aether timer");
        int op = (packet[0] & 255) | (packet[1] & 255) << 8;
        if (op != START_OPCODE && op != PULSE_OPCODE) throw new IllegalArgumentException("Wrong aether timer opcode");
        var r = Tms282FieldRequestReader.open(packet, op, "Adele stack-unit timer");
        int skill = r.i32();
        int tick = r.i32();
        int trailer = r.i32();
        r.end();
        if (skill != SKILL || trailer != -1) throw new IllegalArgumentException("Unsupported aether timer source/trailer");
        return new Tms282AdeleTimerRequest(op == START_OPCODE, tick);
    }
}
