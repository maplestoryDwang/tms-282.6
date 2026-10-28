package net.swordie.ms.lifecycle;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * The last {@value #CAPACITY} periodic character saves (character, duration, success), newest first, for the admin
 * console. Memory only; recorded by {@link PeriodicSaveService} on its own thread after each save.
 */
public final class SaveHistory {
    public static final int CAPACITY = 50;
    private static final SaveHistory STANDARD = new SaveHistory(CAPACITY);

    public record Entry(long at, int characterId, String name, long millis, boolean ok) {
    }

    private final int capacity;
    private final Deque<Entry> entries = new ArrayDeque<>();

    public SaveHistory(int capacity) {
        this.capacity = Math.max(1, capacity);
    }

    public static SaveHistory standard() {
        return STANDARD;
    }

    public synchronized void record(int characterId, String name, long millis, boolean ok, long at) {
        entries.addFirst(new Entry(at, characterId, name == null ? "" : name, millis, ok));
        while (entries.size() > capacity) {
            entries.removeLast();
        }
    }

    /** Newest first. */
    public synchronized List<Entry> recent() {
        return new ArrayList<>(entries);
    }
}
