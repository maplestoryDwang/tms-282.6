package net.swordie.ms.client.character.tms282;

import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;

/**
 * One character's active temporary stats and skill cooldowns, as far as the TMS 282.3 skill service sent them.
 * Same replacement rules as 265 {@code TemporaryStatManager.sendStat}: a standard stat holds one option (a new one
 * replaces it); an indie stat holds one option per source skill ({@code nReason}), a new option from the same skill
 * replaces the old one. Thread-safe (the session thread applies, a timer thread expires).
 */
public final class Tms282BuffBook {
    private net.swordie.ms.client.character.Char owner;
    private long revision;
    private final Map<CharacterTemporaryStat, List<Option>> active = new EnumMap<>(CharacterTemporaryStat.class);
    private final Map<Integer, Long> cooldownUntil = new HashMap<>();
    private int tanjiroDashUses;
    private long tanjiroDashLastUse;
    private int tanjiroDashAttacks;
    private long tanjiroDashAttackExpiry;

    /** One 282 socket's book owns its mirrored TSM options and their expiry. */
    public void bind(net.swordie.ms.client.character.Char chr) {
        java.util.Objects.requireNonNull(chr);
        net.swordie.ms.client.character.Char previous;
        Change change;
        synchronized (this) {
            if (owner == chr) return;
            previous = owner;
            owner = chr;
            change = change();
        }
        if (previous != null) previous.getTemporaryStatManager().releaseBook(this, change.revision());
        chr.getTemporaryStatManager().bindBook(this, change.revision());
        sync(change);
    }

    /** Socket migration keeps timed TSM buffs; its normal update owns their remaining lifetime. */
    public void release() {
        net.swordie.ms.client.character.Char previous;
        long version;
        synchronized (this) { previous = owner; owner = null; version = ++revision; }
        if (previous != null) previous.getTemporaryStatManager().releaseBook(this, version);
    }

    /** A legacy TSM removal already emitted its reset; forget exactly the options it removed. */
    public void discardStats(Map<CharacterTemporaryStat, List<Option>> removed) {
        removeWhere((cts, option) -> removed.getOrDefault(cts, List.of()).stream()
                .anyMatch(old -> old == option)); // A late legacy removal must not cancel a refreshed source.
    }

    private record Change(net.swordie.ms.client.character.Char owner, long revision,
                          Map<CharacterTemporaryStat, List<Option>> stats) { }

    /** Caller holds only the book monitor here; TSM mutation/callbacks always happen after it is released. */
    private Change change() { return new Change(owner, ++revision, snapshot()); }

    private void sync(Change change) {
        if (change != null && change.owner() != null)
            change.owner().getTemporaryStatManager().syncBookStats(this, change.revision(), change.stats());
    }

    /** Claim one of the dash's consecutive uses; the first use owns the cooldown. */
    public synchronized int claimTanjiroDash(long now, int maxUses, long continuationMillis, long cooldownMillis) {
        final int skillId = 121001008;
        if (maxUses <= 0 || continuationMillis <= 0 || cooldownMillis <= 0) return 0;
        if (!coolingDown(skillId, now)) {
            tanjiroDashUses = 1;
            tanjiroDashLastUse = now;
            startCooldown(skillId, now + cooldownMillis);
            tanjiroDashAttacks = 1;
            tanjiroDashAttackExpiry = now + continuationMillis;
            return 1;
        }
        if (tanjiroDashUses == 0 || tanjiroDashUses >= maxUses || now < tanjiroDashLastUse
                || now - tanjiroDashLastUse >= continuationMillis) return 0;
        tanjiroDashLastUse = now;
        tanjiroDashAttacks++;
        tanjiroDashAttackExpiry = now + continuationMillis;
        return ++tanjiroDashUses;
    }

    /** One attack can consume each paid CP04B4; duplicates and stale attacks are rejected. */
    public synchronized boolean consumeTanjiroDashAttack(long now) {
        if (tanjiroDashAttacks <= 0 || now < tanjiroDashLastUse || now >= tanjiroDashAttackExpiry) return false;
        tanjiroDashAttacks--;
        return true;
    }

    /** What {@link #expire} removed. {@code remainingIndie}: options still active for each removed indie stat. */
    public record Expired(Map<CharacterTemporaryStat, List<Option>> removed,
                          Map<CharacterTemporaryStat, List<Option>> remainingIndie, int remainingKinds) {
        public boolean isEmpty() {
            return removed.isEmpty();
        }
    }

    /**
     * Records {@code stats} and returns what TEMPORARY_STAT_SET must carry for them: a standard stat with its one
     * option, an indie stat with all of its options (the client replaces the stat's whole list).
     */
    public Map<CharacterTemporaryStat, List<Option>> apply(List<Tms282BuffResolver.ResolvedStat> stats) {
        Map<CharacterTemporaryStat, List<Option>> changed = new EnumMap<>(CharacterTemporaryStat.class);
        Change change;
        synchronized (this) {
            for (Tms282BuffResolver.ResolvedStat s : stats) {
                CharacterTemporaryStat cts = s.stat();
                if (cts.isIndie()) {
                    List<Option> list = active.computeIfAbsent(cts, k -> new ArrayList<>());
                    list.removeIf(o -> o.nReason == s.option().nReason);
                    list.add(s.option());
                    changed.put(cts, List.copyOf(list));
                } else {
                    active.put(cts, new ArrayList<>(List.of(s.option())));
                    changed.put(cts, List.of(s.option()));
                }
            }
            change = changed.isEmpty() ? null : change();
        }
        sync(change);
        return changed;
    }

    /** Removes every option whose time is up at {@code nowMillis}. */
    public Expired expire(long nowMillis) {
        return removeWhere((cts, o) -> endMillis(cts, o) <= nowMillis);
    }

    /**
     * CP04B7 skill cancel: removes every option given by {@code skillId} (standard stats: {@code rOption}, indie
     * stats: {@code nReason}, as {@link SkillStatBuffResolver} writes them).
     */
    public Expired cancelSkill(int skillId) {
        return removeWhere((cts, o) -> (cts.isIndie() ? o.nReason : o.rOption) == skillId);
    }

    private Expired removeWhere(java.util.function.BiPredicate<CharacterTemporaryStat, Option> gone0) {
        Map<CharacterTemporaryStat, List<Option>> removed = new EnumMap<>(CharacterTemporaryStat.class);
        Map<CharacterTemporaryStat, List<Option>> remaining = new EnumMap<>(CharacterTemporaryStat.class);
        Change change;
        Expired result;
        synchronized (this) {
            for (Iterator<Map.Entry<CharacterTemporaryStat, List<Option>>> it = active.entrySet().iterator(); it.hasNext(); ) {
                var e = it.next();
                List<Option> gone = new ArrayList<>();
                e.getValue().removeIf(o -> {
                    boolean done = gone0.test(e.getKey(), o);
                    if (done) gone.add(o);
                    return done;
                });
                if (gone.isEmpty()) continue;
                removed.put(e.getKey(), List.copyOf(gone));
                if (e.getKey().isIndie()) remaining.put(e.getKey(), List.copyOf(e.getValue()));
                if (e.getValue().isEmpty()) it.remove();
            }
            result = new Expired(removed, remaining, active.size());
            change = removed.isEmpty() ? null : change();
        }
        sync(change);
        return result;
    }

    /** Earliest end time of an active option. */
    public synchronized OptionalLong nextExpiry() {
        return active.entrySet().stream()
                .flatMap(e -> e.getValue().stream().map(o -> endMillis(e.getKey(), o)))
                .mapToLong(Long::longValue).filter(end -> end != Long.MAX_VALUE).min();
    }

    /** Number of active stat kinds (265 {@code currentStats.size()}, TEMPORARY_STAT_SET/RESET curSize). */
    public synchronized int size() {
        return active.size();
    }

    public synchronized Map<CharacterTemporaryStat, List<Option>> snapshot() {
        Map<CharacterTemporaryStat, List<Option>> copy = new EnumMap<>(CharacterTemporaryStat.class);
        active.forEach((k, v) -> copy.put(k, List.copyOf(v)));
        return copy;
    }

    /** Packet lists must retain legacy TSM sources sharing a CTS with this book's options. */
    public Map<CharacterTemporaryStat, List<Option>> serverSnapshot() {
        net.swordie.ms.client.character.Char chr;
        synchronized (this) {
            chr = owner;
            if (chr == null) return snapshot();
        }
        return chr.getTemporaryStatManager().getCurrentStats();
    }

    public synchronized boolean coolingDown(int skillId, long nowMillis) {
        Long until = cooldownUntil.get(skillId);
        return until != null && until > nowMillis;
    }

    public synchronized long cooldownRemainingMillis(int skillId, long nowMillis) {
        Long until = cooldownUntil.get(skillId);
        return until == null ? 0 : Math.max(0, until - nowMillis);
    }

    public synchronized void startCooldown(int skillId, long untilMillis) {
        cooldownUntil.put(skillId, untilMillis);
    }

    public synchronized boolean tryStartCooldown(int skillId, long nowMillis, long durationMillis) {
        if (coolingDown(skillId, nowMillis)) return false;
        startCooldown(skillId, nowMillis + durationMillis);
        return true;
    }

    private static long endMillis(CharacterTemporaryStat cts, Option o) {
        long term = cts.isIndie() ? o.tTerm : o.tOption;
        return term == 0 ? Long.MAX_VALUE : o.startTime + term;
    }
}
