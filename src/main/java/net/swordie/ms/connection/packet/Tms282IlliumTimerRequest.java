package net.swordie.ms.connection.packet;

/** 5900BC0 crystal timer: fixed group key, unsigned client-clock hint and transport trailer. */
public record Tms282IlliumTimerRequest(boolean start, int clientTick) {
    public static final int START_OPCODE = 0x0409;
    public static final int PULSE_OPCODE = 0x0408;
    public static final int KEY = 152100010;

    public static Tms282IlliumTimerRequest parse(byte[] packet) {
        if (packet == null || packet.length < 2) throw new IllegalArgumentException("Truncated crystal timer");
        int op = (packet[0] & 255) | (packet[1] & 255) << 8;
        if (op != START_OPCODE && op != PULSE_OPCODE) throw new IllegalArgumentException("Wrong crystal timer opcode");
        var r = Tms282FieldRequestReader.open(packet, op, "Illium crystal timer");
        int key = r.i32(), tick = r.i32(), trailer = r.i32();
        r.end();
        if (key != KEY || trailer != -1) throw new IllegalArgumentException("Unsupported crystal timer key/trailer");
        return new Tms282IlliumTimerRequest(op == START_OPCODE, tick);
    }
}
