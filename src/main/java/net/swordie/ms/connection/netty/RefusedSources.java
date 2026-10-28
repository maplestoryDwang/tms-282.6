package net.swordie.ms.connection.netty;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Recently refused sources of the game ports (net.allow, net.maxPerIp, shutting down), for the admin console:
 * per source IP the number of refusals, the last time, reason and port. Memory only, at most {@value #CAPACITY}
 * addresses (the least recently refused one is dropped first), so a scan cannot grow it. Recorded by
 * {@link GameSourceGate} before its log throttle, so the counts include the refusals the log folds together.
 */
public final class RefusedSources {
    public static final int CAPACITY = 64;
    private static final RefusedSources STANDARD = new RefusedSources(CAPACITY);

    /** One source address. {@code reason}: the GameSourceGate verdict name (NOT_ALLOWED, TOO_MANY, ...). */
    public record Entry(String ip, long count, long firstAt, long lastAt, String reason, String kind, int port) {
    }

    private final int capacity;
    private final LinkedHashMap<String, Entry> entries = new LinkedHashMap<>(16, 0.75f, true);
    private final AtomicLong total = new AtomicLong();

    public RefusedSources(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity");
        }
        this.capacity = capacity;
    }

    public static RefusedSources standard() {
        return STANDARD;
    }

    public void record(String ip, String reason, String kind, int port, long now) {
        String key = ip == null ? "" : ip;
        total.incrementAndGet();
        synchronized (entries) {
            Entry e = entries.get(key);
            entries.put(key, e == null ? new Entry(key, 1, now, now, reason, kind, port)
                    : new Entry(key, e.count() + 1, e.firstAt(), now, reason, kind, port));
            if (entries.size() > capacity) {
                // access order: the first entry is the least recently refused one
                String eldest = entries.keySet().iterator().next();
                entries.remove(eldest);
            }
        }
    }

    /** Most recently refused first. */
    public List<Entry> snapshot() {
        List<Entry> out;
        synchronized (entries) {
            out = new ArrayList<>(entries.values());
        }
        out.sort(Comparator.comparingLong(Entry::lastAt).reversed());
        return out;
    }

    /** Refusals since start (all addresses, also the dropped ones). */
    public long total() {
        return total.get();
    }

    public void clear() {
        synchronized (entries) {
            entries.clear();
        }
    }

    /** Copy for checks. */
    Map<String, Entry> view() {
        synchronized (entries) {
            return new LinkedHashMap<>(entries);
        }
    }
}
