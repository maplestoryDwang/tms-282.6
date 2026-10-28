package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.CharacterStat;

import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/**
 * Writes the HP/MP that CP0193 recovery changed (the research probe's hpMpDirty / flushHpMp), at most once per
 * interval: {@link #markDirty} schedules one write for {@code lastWrite + interval} on the session timer, the write
 * clears the flag, and a failed write keeps it and retries one interval later.
 * <p>
 * Disconnect differs from the probe on purpose: the probe wrote hp/mp once more when its socket closed, the server
 * already saves the whole character there (265 {@code Client.finalizeSession} -> {@code Char.saveToSQL}, which
 * writes characterstats with the same in-memory hp/mp). {@link #stop} therefore only cancels the pending write, so
 * the row is not written twice and a late timer write can never land after that save. A write also checks that the
 * socket's client still holds this character ({@code finalizeSession} clears it before saving).
 */
final class Tms282HpMpSaveTimer {
    private final Tms282LoginState session;
    private final Char chr;
    private final Tms282FieldStore store;
    private final long intervalNanos;
    private final LongSupplier clock;

    private boolean dirty;
    private boolean stopped;
    private long lastWrite;
    private ScheduledFuture<?> pending;
    private int writes;

    Tms282HpMpSaveTimer(Tms282LoginState session, Char chr, Tms282FieldStore store, long intervalNanos, LongSupplier clock) {
        this.session = Objects.requireNonNull(session, "session");
        this.chr = Objects.requireNonNull(chr, "chr");
        this.store = Objects.requireNonNull(store, "store");
        if (intervalNanos <= 0) {
            throw new IllegalArgumentException("interval must be positive");
        }
        this.intervalNanos = intervalNanos;
        this.clock = Objects.requireNonNull(clock, "clock");
        this.lastWrite = clock.getAsLong();
    }

    /** CP0193 changed hp or mp: one write is due one interval after the last one. */
    synchronized void markDirty() {
        dirty = true;
        if (pending == null && !stopped) {
            long wait = Math.max(0, lastWrite + intervalNanos - clock.getAsLong());
            pending = session.schedule(this::write, wait, TimeUnit.NANOSECONDS);
        }
    }

    private void write() {
        synchronized (this) {
            pending = null;
            if (stopped || !dirty) {
                return;
            }
            dirty = false;
            lastWrite = clock.getAsLong();
        }
        CharacterStat stat = chr.getAvatarData().getCharacterStat();
        if (session.client().getChr() != chr) {
            System.out.println("[TMS282] SKIPPED hp/mp save character=" + chr.getId() + ": no longer the client's character");
            return;
        }
        try {
            store.saveHpMp(chr);
            synchronized (this) {
                writes++;
            }
            System.out.println("[TMS282] SAVED hp/mp character=" + chr.getId() + " hp=" + stat.getHp() + " mp=" + stat.getMp());
        } catch (RuntimeException e) {
            System.out.println("[TMS282] FAILED hp/mp save character=" + chr.getId() + ": " + e.getMessage());
            markDirty();
        }
    }

    /** Socket closed: the pending write is cancelled; the 265 finalizeSession save writes the final hp/mp. */
    void stop() {
        boolean wasDirty;
        synchronized (this) {
            if (stopped) {
                return;
            }
            stopped = true;
            wasDirty = dirty;
            dirty = false;
            if (pending != null) {
                pending.cancel(false);
                pending = null;
            }
        }
        if (wasDirty) {
            System.out.println("[TMS282] hp/mp of character " + chr.getId()
                    + " left to the 265 finalizeSession character save (no separate disconnect write)");
        }
    }

    synchronized boolean dirty() {
        return dirty;
    }

    synchronized boolean writePending() {
        return pending != null;
    }

    synchronized int writes() {
        return writes;
    }
}
