package net.swordie.ms.connection.packet;

/** CP0764, 1FD9970: three flags, plus u16 elapsed seconds when stopping. */
public record Tms282BattleRecordRequest(int on, int fresh, int clear, int seconds) {
    public static final int OPCODE = 0x0764;
    public static Tms282BattleRecordRequest parse(byte[] packet) {
        var in = Tms282FieldRequestReader.open(packet, OPCODE, "CP0764");
        int on=in.u8(), fresh=in.u8(), clear=in.u8();
        if (on>1 || fresh>1 || clear>1) throw new IllegalArgumentException("Invalid battle record flag");
        int seconds=on==0 ? in.u16() : 0;
        in.end();
        return new Tms282BattleRecordRequest(on,fresh,clear,seconds);
    }
    public byte[] toGms265Body() { return new byte[]{(byte)on,(byte)fresh,(byte)clear}; }
}
