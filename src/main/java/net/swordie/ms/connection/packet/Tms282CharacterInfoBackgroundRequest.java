package net.swordie.ms.connection.packet;

/** CP0124: selected catalogue row ID (2D8D03C/2D8D0FE), rather than its list index. */
public record Tms282CharacterInfoBackgroundRequest(int id) {
    public static final int OPCODE = 0x0124;

    public static Tms282CharacterInfoBackgroundRequest parse(byte[] packet) {
        var in = Tms282FieldRequestReader.open(packet, OPCODE, "character info background");
        int id = in.i32();
        in.end();
        return new Tms282CharacterInfoBackgroundRequest(id);
    }
}
