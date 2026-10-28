package net.swordie.ms.connection.packet;

/** Strict native CP0580/0581/0582 layouts. No success is inferred from the completed byte. */
public final class Tms282RuneRequests {
    private Tms282RuneRequests() { }
    public record Use(int index, int runeType) { }
    public record Direction(int index, int direction) { }
    public record Finish(boolean completed, boolean compact, int tokenA, int tokenB,
                         int[] directions, int valueA, int valueB, int valueC, int tick, int flag) {
        public Finish { directions = directions.clone(); }
        @Override public int[] directions() { return directions.clone(); }
    }
    public static Use use(byte[] packet) {
        var in = Tms282FieldRequestReader.open(packet, 0x580, "rune use");
        var result = new Use(in.i32(), in.i32()); in.end(); return result;
    }
    public static Direction direction(byte[] packet) {
        var in = Tms282FieldRequestReader.open(packet, 0x582, "rune direction");
        var result = new Direction(in.i32(), in.i32()); in.end();
        if (result.index < 0 || result.index > 3 || result.direction < 0 || result.direction > 3)
            throw new IllegalArgumentException("Invalid rune direction/index");
        return result;
    }
    public static Finish finish(byte[] packet) {
        var in = Tms282FieldRequestReader.open(packet, 0x581, "rune completion");
        int completed = bool(in);
        if (completed == 0) {
            int length = in.u16();
            if (length > 1024) throw new IllegalArgumentException("Rune cancel text too long");
            in.bytes(length); // Native cancel text is not used as authority or logged.
            int a = in.i32(), b = in.i32(); in.end();
            return new Finish(false, false, a, b, new int[0], 0, 0, 0, 0, 0);
        }
        bool(in); // Native puzzle-mode flag, not proof of a correct answer.
        int a = in.i32(), b = in.i32();
        boolean compact = in.remaining() == 17;
        int[] directions = compact ? new int[0] : new int[4];
        for (int i = 0; i < directions.length; i++) {
            directions[i] = in.i32();
            if (directions[i] < 0 || directions[i] > 3)
                throw new IllegalArgumentException("Invalid completed rune direction");
        }
        var result = new Finish(true, compact, a, b, directions, in.i32(), in.i32(), in.i32(), in.i32(), bool(in));
        in.end(); return result;
    }
    private static int bool(Tms282FieldRequestReader in) {
        int value = in.u8();
        if (value > 1) throw new IllegalArgumentException("Invalid rune boolean");
        return value;
    }
}
