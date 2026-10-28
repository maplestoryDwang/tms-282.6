package net.swordie.ms.connection.packet;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * CP0191 USER_ABILITY_MASS_UP_REQUEST (265 InHeader 0x18A): the AP auto-assign button [verified: sender 5EDF840,
 * packet built at 5EDFA13; same sequence as 265]:
 * <pre>
 * u32 tick
 * u32 n
 * n x (Buf8 stat flag (u64, as CP0190), u32 amount)
 * </pre>
 * The request lock is set after sending (5E04DA0(ctx, 1)). 346A070 appends nothing for 0x191.
 */
public record Tms282AbilityMassUpRequest(int tick, List<Entry> entries) {
    public static final int OPCODE = 0x0191;
    /** More entries than stats is not a client request. */
    public static final int MAX_ENTRIES = 16;

    public record Entry(long stat, int amount) {
        @Override
        public String toString() {
            return String.format("0x%X x%d", stat, amount);
        }
    }

    public Tms282AbilityMassUpRequest {
        entries = List.copyOf(entries);
    }

    /** Accepts the full plaintext packet, including the opcode; must end after the last entry. */
    public static Tms282AbilityMassUpRequest parse(byte[] packet) {
        Tms282FieldRequestReader in = Tms282FieldRequestReader.open(packet, OPCODE, "CP0191");
        int tick = in.i32();
        int n = in.i32();
        if (n < 0 || n > MAX_ENTRIES) {
            throw new IllegalArgumentException("CP0191 entry count " + n);
        }
        List<Entry> entries = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            long stat = ByteBuffer.wrap(in.bytes(8)).order(ByteOrder.LITTLE_ENDIAN).getLong();
            entries.add(new Entry(stat, in.i32()));
        }
        in.end();
        return new Tms282AbilityMassUpRequest(tick, entries);
    }

    /** Sum of the amounts (long: the client sends u32). */
    public long totalAmount() {
        return entries.stream().mapToLong(e -> Integer.toUnsignedLong(e.amount())).sum();
    }

    @Override
    public String toString() {
        return "CP0191 ability mass up " + entries.stream().map(Entry::toString).collect(Collectors.joining(", ", "[", "]"));
    }
}
