package net.swordie.ms.connection.api.launcher.release;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

/**
 * Concurrent launcher downloads: at most {@code perIp} per source address and {@code total} in all (standard: 1 and 4,
 * docs/launcher-api-update.md section 8). Only transfers that are sending count; a {@link Slot} is given back exactly
 * once by {@link Slot#close()} (finished, client gone, error), so a broken connection never leaks a slot.
 */
public final class DownloadSlots {
    public static final int PER_IP = 1;
    public static final int TOTAL = 4;

    private final int perIp;
    private final int total;
    private final LongSupplier clock;
    private final Map<String, Integer> byIp = new HashMap<>();
    private final Set<Slot> active = new LinkedHashSet<>();
    private final AtomicLong refused = new AtomicLong();
    private final AtomicLong served = new AtomicLong();

    public DownloadSlots(int perIp, int total, LongSupplier clock) {
        if (perIp < 1 || total < perIp) {
            throw new IllegalArgumentException("perIp " + perIp + " total " + total);
        }
        this.perIp = perIp;
        this.total = total;
        this.clock = clock;
    }

    private static final DownloadSlots SHARED = new DownloadSlots(PER_IP, TOTAL, System::currentTimeMillis);

    /** The server's slots (HttpAuthApi's download endpoint; the admin console lists them). */
    public static DownloadSlots shared() {
        return SHARED;
    }

    private static final DownloadSlots CLIENT = new DownloadSlots(PER_IP, TOTAL, System::currentTimeMillis);

    /** 1.2.3: the client file endpoint's own slots (1 per address, 4 in all), so a launcher exe download is not blocked by a game file one. */
    public static DownloadSlots client() {
        return CLIENT;
    }

    /** A slot for {@code ip}, or null when that address or the server is at its limit. */
    public synchronized Slot tryAcquire(String ip, String file, long bytes) {
        String key = ip == null ? "" : ip;
        int mine = byIp.getOrDefault(key, 0);
        if (mine >= perIp || active.size() >= total) {
            refused.incrementAndGet();
            return null;
        }
        byIp.put(key, mine + 1);
        Slot s = new Slot(key, file, bytes, clock.getAsLong());
        active.add(s);
        served.incrementAndGet();
        return s;
    }

    private synchronized void release(Slot s) {
        if (!active.remove(s)) {
            return;
        }
        int n = byIp.getOrDefault(s.ip, 0) - 1;
        if (n <= 0) {
            byIp.remove(s.ip);
        } else {
            byIp.put(s.ip, n);
        }
    }

    public synchronized int active() {
        return active.size();
    }

    public synchronized int activeFor(String ip) {
        return byIp.getOrDefault(ip == null ? "" : ip, 0);
    }

    public long refused() {
        return refused.get();
    }

    /** Transfers started since start (admin console). */
    public long started() {
        return served.get();
    }

    /** The running transfers for the admin console: ip, file, sent, bytes, startedAt. */
    public synchronized List<Map<String, Object>> list() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Slot s : active) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("ip", s.ip);
            m.put("file", s.file);
            m.put("sent", s.sent.get());
            m.put("bytes", s.bytes);
            m.put("startedAt", s.startedAt);
            out.add(m);
        }
        return out;
    }

    public int perIp() {
        return perIp;
    }

    public int total() {
        return total;
    }

    /** One running transfer. */
    public final class Slot implements AutoCloseable {
        private final String ip;
        private final String file;
        private final long bytes;
        private final long startedAt;
        private final AtomicLong sent = new AtomicLong();
        private final AtomicBoolean closed = new AtomicBoolean();

        private Slot(String ip, String file, long bytes, long startedAt) {
            this.ip = ip;
            this.file = file;
            this.bytes = bytes;
            this.startedAt = startedAt;
        }

        public void sent(long n) {
            sent.addAndGet(n);
        }

        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
                release(this);
            }
        }
    }
}
