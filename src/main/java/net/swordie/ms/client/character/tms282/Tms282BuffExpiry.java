package net.swordie.ms.client.character.tms282;

import net.swordie.ms.connection.OutPacket;

import java.util.Objects;
import java.util.OptionalLong;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

/**
 * Sends TEMPORARY_STAT_RESET when a character's buffs run out: one timer per {@link Tms282BuffBook}, re-armed for the
 * next end time after every expiry or new buff. {@link #cancel()} when the character leaves. Built with the
 * character's id, it also hands the other users' packets (LP0490 / LP048F, {@link Tms282SkillService#expire(
 * Tms282BuffBook, int)}) to {@link Sink#broadcast}.
 */
public final class Tms282BuffExpiry {
    private static final ScheduledExecutorService TIMERS = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "tms282-buff-expiry");
        t.setDaemon(true);
        return t;
    });

    /** Where the reset packets go (the session's send), and where the log lines go. */
    public interface Sink {
        void send(OutPacket packet) throws Exception;

        void log(String line);

        /** A packet for the other users of the character's field (nothing by default). */
        default void broadcast(OutPacket packet) throws Exception {
        }
    }

    private final Tms282SkillService service;
    private final Tms282BuffBook book;
    private final Sink sink;
    private final LongSupplier clock;
    private final int characterId;
    private ScheduledFuture<?> pending;
    private boolean cancelled;

    public Tms282BuffExpiry(Tms282SkillService service, Tms282BuffBook book, Sink sink, LongSupplier clock) {
        this(service, book, sink, clock, 0);
    }

    /** {@code characterId} above 0: the other users of the field are told too ({@link Sink#broadcast}). */
    public Tms282BuffExpiry(Tms282SkillService service, Tms282BuffBook book, Sink sink, LongSupplier clock,
                            int characterId) {
        this.characterId = characterId;
        this.service = Objects.requireNonNull(service, "service");
        this.book = Objects.requireNonNull(book, "book");
        this.sink = Objects.requireNonNull(sink, "sink");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /** Arms the timer for the book's earliest buff end, replacing an armed one (nothing when no buff is active). */
    public synchronized void arm() {
        if (cancelled) {
            return;
        }
        OptionalLong at = book.nextExpiry();
        if (pending != null) {
            pending.cancel(false);
            pending = null;
        }
        if (at.isEmpty()) {
            return;
        }
        long delay = Math.max(0, at.getAsLong() - clock.getAsLong());
        pending = TIMERS.schedule(this::fire, delay, TimeUnit.MILLISECONDS);
    }

    public synchronized void cancel() {
        cancelled = true;
        if (pending != null) {
            pending.cancel(false);
            pending = null;
        }
    }

    private void fire() {
        synchronized (this) {
            if (cancelled) {
                return;
            }
            pending = null;
        }
        try {
            Tms282SkillService.Outcome outcome = service.expire(book, characterId);
            for (OutPacket p : outcome.packets()) {
                sink.send(p);
            }
            for (OutPacket p : outcome.fieldPackets()) {
                sink.broadcast(p);
            }
            if (outcome.accepted()) {
                sink.log(outcome.log());
            }
        } catch (Exception e) {
            sink.log("buff expiry failed (timer stopped until the next buff): " + e);
            return;
        }
        arm();
    }
}
