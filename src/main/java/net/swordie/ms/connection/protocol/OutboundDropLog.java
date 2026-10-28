package net.swordie.ms.connection.protocol;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.util.DataPrinter;

import java.util.concurrent.atomic.AtomicLong;

/** Console and file trace of packets an {@link OutboundGuard} refused. */
public final class OutboundDropLog {
    private static final String FILE = "Packets/outbound-guard.txt";
    private static final AtomicLong DROPPED = new AtomicLong();
    /** (origin, first body byte) pairs whose bytes were already sampled: one hex sample per sub-type. */
    private static final java.util.Set<String> SAMPLED = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private static final int SAMPLE_BYTES = 64;

    private OutboundDropLog() {
    }

    static void record(OutPacket packet, String target, String reason) {
        long n = DROPPED.incrementAndGet();
        String origin = packet.getOrigin() != null ? packet.getOrigin().name() : "raw";
        String line = String.format("[OutboundGuard] #%d dropped %s op=0x%04X len=%d to %s: %s",
                n, origin, packet.getHeader() & 0xFFFF, packet.getLength(), target, reason);
        System.out.println(line);
        try {
            DataPrinter.send(FILE, line, false);
        } catch (RuntimeException ignored) {
        }
        sample(packet, origin);
    }

    /**
     * The first time a (packet, first body byte) pair is dropped, its leading bytes go to the log: the sub-type
     * (MESSAGE / EFFECT type byte) and body a 282 translator must be written for. Bounded to one line per sub-type.
     */
    private static void sample(OutPacket packet, String origin) {
        try {
            byte[] data = packet.getData();
            if (data == null || data.length < 2) {
                return;
            }
            String key = origin + ":" + (data.length > 2 ? (data[2] & 0xFF) : -1);
            if (SAMPLED.size() > 512 || !SAMPLED.add(key)) {
                return;
            }
            int n = Math.min(data.length, SAMPLE_BYTES + 2);
            String line = String.format("[OutboundGuard] sample %s sub=%s len=%d body=%s%s", origin,
                    data.length > 2 ? String.format("0x%02X", data[2] & 0xFF) : "-", data.length,
                    java.util.HexFormat.of().formatHex(data, 2, n), n < data.length ? "..." : "");
            System.out.println(line);
            DataPrinter.send(FILE, line, false);
        } catch (RuntimeException ignored) {
            // diagnostics only
        }
    }

    /** Packets dropped since start (diagnostics and checks). */
    public static long droppedCount() {
        return DROPPED.get();
    }
}
