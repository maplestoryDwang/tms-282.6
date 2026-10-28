package net.swordie.ms.world.tms282.revival.hub;

import net.swordie.ms.world.tms282.activity.ActivityRewards;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Mission progress of the revival hubs. Kill counters live in memory per character and are written to the
 * {@link ProgressStore} on acceptance, every {@link #SAVE_EVERY} kills, at the target and at the claim. A claim is an
 * account receipt ({@code ActivityRewards.grantOnce}, receipt before inventory), so one account claims a mission once
 * per period whichever character finished it.
 */
public final class MissionService {
    public static final int SAVE_EVERY = 25;

    /** What the service needs from a character. */
    public interface Actor {
        int charId();

        int level();

        void notice(String text);

        /** For solo missions. */
        default boolean inParty() {
            return false;
        }

        ActivityRewards.Result grant(String receiptKey, int[] itemIds, int[] quantities);
    }

    public enum AcceptResult {ACCEPTED, ALREADY, CLOSED, LEVEL, NOT_COUNTED, CLAIMED, FAILED}

    public enum ClaimResult {GRANTED, ACCOUNT_ALREADY, ALREADY, NOT_READY, NOT_ACCEPTED, LEVEL, FULL, CLOSED, FAILED}

    public record Status(boolean accepted, int count, int target, boolean claimed) {
        public boolean ready() {
            return accepted && count >= target;
        }
    }

    private static final class State {
        final Map<String, Map<String, ProgressStore.Row>> rows = new HashMap<>();
    }

    private final ProgressStore store;
    private final Supplier<LocalDate> today;
    private final Function<String, Optional<HubDefinition>> openHub;
    private final Supplier<Collection<HubDefinition>> openHubs;
    private final Map<Integer, State> states = new ConcurrentHashMap<>();

    public MissionService(ProgressStore store, Supplier<LocalDate> today, Function<String, Optional<HubDefinition>> openHub,
                          Supplier<Collection<HubDefinition>> openHubs) {
        this.store = store;
        this.today = today;
        this.openHub = openHub;
        this.openHubs = openHubs;
    }

    private State state(int charId) {
        return states.computeIfAbsent(charId, k -> new State());
    }

    /** Rows of the event, loaded once per character; null when the store failed (then nothing is counted). */
    private Map<String, ProgressStore.Row> rows(State s, int charId, String event) {
        Map<String, ProgressStore.Row> r = s.rows.get(event);
        if (r != null) return r;
        try {
            r = new HashMap<>(store.load(charId, event));
        } catch (Exception e) {
            System.out.println("[Revival] progress load failed character=" + charId + " event=" + event + " " + e.getClass().getSimpleName());
            return null;
        }
        s.rows.put(event, r);
        return r;
    }

    private boolean persist(int charId, String event, HubDefinition.Mission m, String period, ProgressStore.Row row) {
        try {
            store.save(charId, event, m.id(), period, row.count(), row.claimed());
            return true;
        } catch (Exception e) {
            System.out.println("[Revival] progress save failed character=" + charId + " event=" + event + " mission=" + m.id()
                    + " " + e.getClass().getSimpleName());
            return false;
        }
    }

    public Status status(Actor a, String event, String missionId) {
        HubDefinition hub = openHub.apply(event).orElse(null);
        HubDefinition.Mission m = hub == null ? null : hub.mission(missionId).orElse(null);
        if (m == null) return new Status(false, 0, 0, false);
        if (!m.counted()) {
            State s = state(a.charId());
            synchronized (s) {
                Map<String, ProgressStore.Row> rows = rows(s, a.charId(), event);
                ProgressStore.Row row = rows == null ? null : rows.get(ProgressStore.key(m.id(), m.period().key(today.get())));
                return new Status(true, a.level(), m.target(), row != null && row.claimed());
            }
        }
        State s = state(a.charId());
        synchronized (s) {
            Map<String, ProgressStore.Row> rows = rows(s, a.charId(), event);
            ProgressStore.Row row = rows == null ? null : rows.get(ProgressStore.key(m.id(), m.period().key(today.get())));
            return row == null ? new Status(false, 0, m.target(), false) : new Status(true, Math.min(row.count(), m.target()), m.target(), row.claimed());
        }
    }

    public AcceptResult accept(Actor a, String event, String missionId) {
        HubDefinition hub = openHub.apply(event).orElse(null);
        HubDefinition.Mission m = hub == null ? null : hub.mission(missionId).orElse(null);
        if (m == null) return AcceptResult.CLOSED;
        if (!m.counted()) return AcceptResult.NOT_COUNTED;
        if (a.level() < m.minLevel()) return AcceptResult.LEVEL;
        State s = state(a.charId());
        synchronized (s) {
            Map<String, ProgressStore.Row> rows = rows(s, a.charId(), event);
            if (rows == null) return AcceptResult.FAILED;
            String period = m.period().key(today.get());
            ProgressStore.Row row = rows.get(ProgressStore.key(m.id(), period));
            if (row != null) return row.claimed() ? AcceptResult.CLAIMED : AcceptResult.ALREADY;
            ProgressStore.Row fresh = new ProgressStore.Row(0, false);
            if (!persist(a.charId(), event, m, period, fresh)) return AcceptResult.FAILED;
            rows.put(ProgressStore.key(m.id(), period), fresh);
            return AcceptResult.ACCEPTED;
        }
    }

    /** Mob kill hook (Char.handleMobKillForQuest): counts every accepted, unclaimed, unfinished mission it matches. */
    public void onKill(Actor a, int mobId, int mobLevel) {
        Collection<HubDefinition> hubs = openHubs.get();
        if (hubs.isEmpty()) return;
        // Memory only: rows come from preload (field entry), accept or an NPC page. Never a query on the kill path.
        State s = states.get(a.charId());
        if (s == null) return;
        LocalDate day = today.get();
        synchronized (s) {
            for (HubDefinition hub : hubs) {
                Map<String, ProgressStore.Row> rows = s.rows.get(hub.eventKey());
                if (rows == null || rows.isEmpty()) continue;
                for (HubDefinition.Mission m : hub.missions()) {
                    if (!m.counted() || !m.counts(a.level(), mobId, mobLevel) || m.solo() && a.inParty()) continue;
                    String period = m.period().key(day);
                    String key = ProgressStore.key(m.id(), period);
                    ProgressStore.Row row = rows.get(key);
                    if (row == null || row.claimed() || row.count() >= m.target()) continue;
                    ProgressStore.Row next = new ProgressStore.Row(row.count() + 1, false);
                    rows.put(key, next);
                    if (next.count() == m.target()) {
                        persist(a.charId(), hub.eventKey(), m, period, next);
                        a.notice("[" + hub.title() + "] 「" + m.title() + "」已完成，請找活動 NPC 領取獎勵。");
                    } else if (next.count() % SAVE_EVERY == 0) {
                        persist(a.charId(), hub.eventKey(), m, period, next);
                        if (next.count() % noticeStep(m.target()) == 0)
                            a.notice("[" + hub.title() + "] 「" + m.title() + "」 " + next.count() + " / " + m.target());
                    }
                }
            }
        }
    }

    /** Progress notices about every quarter of the target, on a save boundary. */
    static int noticeStep(int target) {
        int quarter = (target / 4 + SAVE_EVERY - 1) / SAVE_EVERY * SAVE_EVERY;
        return Math.max(SAVE_EVERY, quarter);
    }

    public ClaimResult claim(Actor a, String event, String missionId) {
        HubDefinition hub = openHub.apply(event).orElse(null);
        HubDefinition.Mission m = hub == null ? null : hub.mission(missionId).orElse(null);
        if (m == null) return ClaimResult.CLOSED;
        State s = state(a.charId());
        synchronized (s) {
            Map<String, ProgressStore.Row> rows = rows(s, a.charId(), event);
            if (rows == null) return ClaimResult.FAILED;
            LocalDate day = today.get();
            String period = m.period().key(day);
            String key = ProgressStore.key(m.id(), period);
            ProgressStore.Row row = rows.get(key);
            if (m.counted()) {
                if (row == null) return ClaimResult.NOT_ACCEPTED;
                if (row.claimed()) return ClaimResult.ALREADY;
                if (row.count() < m.target()) return ClaimResult.NOT_READY;
            } else {
                if (row != null && row.claimed()) return ClaimResult.ALREADY;
                if (a.level() < m.target()) return ClaimResult.LEVEL;
            }
            int[] ids = m.rewards().stream().mapToInt(HubDefinition.Reward::itemId).toArray();
            int[] qty = m.rewards().stream().mapToInt(HubDefinition.Reward::quantity).toArray();
            ActivityRewards.Result r = a.grant(hub.receiptKey(m, day), ids, qty);
            ClaimResult out = switch (r) {
                case GRANTED -> ClaimResult.GRANTED;
                case ALREADY -> ClaimResult.ACCOUNT_ALREADY;
                case FULL -> ClaimResult.FULL;
                default -> ClaimResult.FAILED;
            };
            if (out == ClaimResult.GRANTED || out == ClaimResult.ACCOUNT_ALREADY) {
                ProgressStore.Row done = new ProgressStore.Row(row == null ? m.target() : row.count(), true);
                rows.put(key, done);
                persist(a.charId(), event, m, period, done);
            }
            return out;
        }
    }

    /** Loads the rows of every open hub for a character that has none in memory yet (field entry, off the game thread). */
    public void preload(int charId) {
        Collection<HubDefinition> hubs = openHubs.get();
        if (hubs.isEmpty()) return;
        State s = state(charId);
        synchronized (s) {
            for (HubDefinition hub : hubs) rows(s, charId, hub.eventKey());
        }
    }

    public boolean isLoaded(int charId, String event) {
        State s = states.get(charId);
        if (s == null) return false;
        synchronized (s) {
            return s.rows.containsKey(event);
        }
    }

    /** Drops the cached rows of a character (logout); unsaved kills since the last save are lost (at most SAVE_EVERY - 1). */
    public void forget(int charId) {
        states.remove(charId);
    }
}
