package net.swordie.ms.connection.packet;

/** Native5900BC0 key/tick request for Xenon's Surplus Supply; tick is not an energy value. */
public record Tms282XenonSupplyTimerRequest(boolean start, int clientTick) {
    public static final int START_OPCODE = 0x0409, PULSE_OPCODE = 0x0407, SKILL = 30020232;

    public static Tms282XenonSupplyTimerRequest parse(byte[] packet) {
        if (packet == null || packet.length < 2) throw new IllegalArgumentException("Truncated Xenon supply timer");
        int opcode = (packet[0] & 255) | (packet[1] & 255) << 8;
        if (opcode != START_OPCODE && opcode != PULSE_OPCODE) throw new IllegalArgumentException("Wrong supply timer opcode");
        var in = Tms282FieldRequestReader.open(packet, opcode, "Xenon Surplus Supply timer");
        int skill = in.i32(), tick = in.i32(), trailer = in.i32(); in.end();
        if (skill != SKILL || trailer != -1) throw new IllegalArgumentException("Unsupported supply timer key/trailer");
        return new Tms282XenonSupplyTimerRequest(opcode == START_OPCODE, tick);
    }
}
