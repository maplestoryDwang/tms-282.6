package net.swordie.ms.world.event;

import net.swordie.ms.ServerConstants;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Objects;
import java.util.function.LongSupplier;

/**
 * The in-game event list (UIEventList) of {@code data/resources/tms282/events/events.tw.json}, falling back to the
 * 265 {@code events.json} (EventListLoader.loadPreferTw). Unlike the old {@code EventConstants.events} (read once at
 * class initialisation) the file is re-read when its modification time or size changed, checked at most every
 * {@value #CHECK_MS} ms, so a data-only fix of the list needs no restart (event-fixes-1005). A file that fails to
 * load keeps the previous list.
 */
public final class EventListRepository {
    static final long CHECK_MS = 5_000;
    private static volatile EventListRepository standard;

    private final Path tw;
    private final Path fallback;
    private final LongSupplier clock;
    private long lastCheck = Long.MIN_VALUE;
    private String lastKey;
    private List<EventListData> events = List.of();

    public EventListRepository(Path tw, Path fallback, LongSupplier clock) {
        this.tw = Objects.requireNonNull(tw, "tw");
        this.fallback = fallback;
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /** The server's list: RESOURCES_DIR/tms282/events/events.tw.json, else RESOURCES_DIR/events.json. */
    public static EventListRepository standard() {
        EventListRepository r = standard;
        if (r == null) {
            synchronized (EventListRepository.class) {
                if (standard == null) {
                    standard = new EventListRepository(
                            Path.of(ServerConstants.RESOURCES_DIR, "tms282", "events", "events.tw.json"),
                            Path.of(ServerConstants.RESOURCES_DIR, "events.json"), System::currentTimeMillis);
                }
                r = standard;
            }
        }
        return r;
    }

    /** The current list (immutable). */
    public synchronized List<EventListData> current() {
        refresh(clock.getAsLong());
        return events;
    }

    /** The entry whose {@code unkVal} (the client's map key, LP 0x0175 event +0x14) is {@code key}; null if none. */
    public EventListData byKey(int key) {
        for (EventListData e : current()) {
            if (e != null && e.unkVal == key) {
                return e;
            }
        }
        return null;
    }

    private void refresh(long now) {
        if (lastCheck != Long.MIN_VALUE && now - lastCheck < CHECK_MS && now >= lastCheck) {
            return;
        }
        lastCheck = now;
        String key = stamp(tw) + "|" + (fallback == null ? "-" : stamp(fallback));
        if (key.equals(lastKey)) {
            return;
        }
        try {
            List<EventListData> loaded = Files.isRegularFile(tw) || fallback == null
                    ? EventListLoader.loadFromFile(tw) : EventListLoader.loadPreferTw(tw, fallback);
            events = List.copyOf(loaded.stream().filter(Objects::nonNull).toList());
            lastKey = key;
            System.out.printf("[EventList] loaded %d events from %s%n", events.size(), Files.isRegularFile(tw) ? tw : fallback);
        } catch (RuntimeException e) {
            lastKey = key; // do not retry a broken file every call; a new edit changes the key
            System.out.println("[EventList] " + tw + " could not be read (" + e.getClass().getSimpleName()
                    + "): keeping the previous " + events.size() + " events");
        }
    }

    private static String stamp(Path p) {
        try {
            BasicFileAttributes a = Files.readAttributes(p, BasicFileAttributes.class);
            return a.lastModifiedTime().toMillis() + ":" + a.size();
        } catch (IOException e) {
            return "missing";
        }
    }
}
