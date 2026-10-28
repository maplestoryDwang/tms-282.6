package net.swordie.ms.world.tms282.audit;

import net.swordie.ms.client.character.Char;

/**
 * Per-thread "why" label for the core money / inventory hooks in Char.
 * <p>
 * The core hooks (Char.addMoneyInternal, Char.addInventoryItem, Char.consumeInventoryItem) know <em>what</em>
 * changed but not <em>why</em>. Call sites tag the change cheaply:
 * <ul>
 *     <li>{@link #mark(Char, String, long)} - one-liner, valid for the same character on the same thread for
 *     {@value #MARK_WINDOW_MS} ms (packet handlers finish well inside that). Newer marks overwrite older ones.</li>
 *     <li>{@link #scope(Char, String, long)} - try-with-resources; valid until closed, no expiry.</li>
 * </ul>
 * Nothing is allocated on the hot path when no mark is set; a mark is a 4-slot per-thread array.
 */
public final class AuditContext {

    public static final long MARK_WINDOW_MS = 150L;
    private static final long MARK_WINDOW_NANOS = MARK_WINDOW_MS * 1_000_000L;
    private static final int SLOTS = 4;

    static final class Mark {
        int charId;
        String reason;
        long parentId;
        long at;
        boolean pinned;
    }

    private static final ThreadLocal<Mark[]> MARKS = ThreadLocal.withInitial(() -> {
        Mark[] marks = new Mark[SLOTS];
        for (int i = 0; i < SLOTS; i++) {
            marks[i] = new Mark();
        }
        return marks;
    });

    private AuditContext() {
    }

    /** Tags the next money/inventory changes of {@code chr} on this thread with {@code reason}. */
    public static void mark(Char chr, String reason) {
        mark(chr, reason, 0L);
    }

    /** @param parentId counterparty: other character id (trade), npc/shop id, trunk id, drop owner, ... */
    public static void mark(Char chr, String reason, long parentId) {
        if (chr == null || !AuditWriter.enabled()) {
            return;
        }
        set(chr.getId(), reason, parentId, false);
    }

    /** Scoped label, e.g. {@code try (var a = AuditContext.scope(chr, "TRADE", other.getId())) { ... }}. */
    public static Scope scope(Char chr, String reason, long parentId) {
        if (chr == null || !AuditWriter.enabled()) {
            return Scope.NOOP;
        }
        Mark m = set(chr.getId(), reason, parentId, true);
        return new Scope(m, chr.getId());
    }

    public static void clear(Char chr) {
        if (chr == null) {
            return;
        }
        for (Mark m : MARKS.get()) {
            if (m.charId == chr.getId()) {
                m.reason = null;
                m.pinned = false;
            }
        }
    }

    private static Mark set(int charId, String reason, long parentId, boolean pinned) {
        Mark[] marks = MARKS.get();
        Mark target = null;
        Mark oldest = marks[0];
        for (Mark m : marks) {
            if (m.charId == charId && m.reason != null) {
                target = m;
                break;
            }
            if (m.reason == null) {
                if (target == null) {
                    target = m;
                }
            } else if (m.at < oldest.at && !m.pinned) {
                oldest = m;
            }
        }
        if (target == null) {
            target = oldest;
        }
        target.charId = charId;
        target.reason = reason;
        target.parentId = parentId;
        target.pinned = pinned;
        target.at = System.nanoTime();
        return target;
    }

    /** Returns the active mark for {@code charId} on this thread, or null. */
    static Mark current(int charId) {
        Mark[] marks = MARKS.get();
        long now = System.nanoTime();
        for (Mark m : marks) {
            if (m.charId == charId && m.reason != null) {
                if (m.pinned || now - m.at <= MARK_WINDOW_NANOS) {
                    return m;
                }
                m.reason = null; // expired
            }
        }
        return null;
    }

    public static final class Scope implements AutoCloseable {
        static final Scope NOOP = new Scope(null, 0);
        private final Mark mark;
        private final int charId;

        private Scope(Mark mark, int charId) {
            this.mark = mark;
            this.charId = charId;
        }

        @Override
        public void close() {
            if (mark != null && mark.charId == charId && mark.pinned) {
                mark.reason = null;
                mark.pinned = false;
            }
        }
    }
}
