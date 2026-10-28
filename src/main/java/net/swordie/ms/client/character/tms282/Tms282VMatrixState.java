package net.swordie.ms.client.character.tms282;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Separate282 authority: does not mutate/reinterpret legacy265 matrix inventory rows. */
public record Tms282VMatrixState(long points, long revision, boolean migrationPending,
                                Map<Integer, Core> cores, Set<Integer> retainedSet) {
    public static final long DEFAULT_EXPIRY = 150842304000000000L; // nativeRVA8205878
    public static final int MAX_POINTS = 9999; //282 String/Consume.img/2435719 desc_leftalign

    public record Core(int id, int level, boolean selected, long expiresAt, int protectedLevel) {
        public Core(int id, int level, boolean selected, long expiresAt) { this(id, level, selected, expiresAt, 0); }
        public Core {
            if (id <= 0 || level < 0 || level > 60 || expiresAt < 0 || protectedLevel < 0 || protectedLevel > level)
                throw new IllegalArgumentException("Invalid282 core record");
        }
    }

    public Tms282VMatrixState {
        if (points < 0 || points > MAX_POINTS || revision < 0) throw new IllegalArgumentException("Invalid282 matrix state");
        cores = Map.copyOf(cores);
        retainedSet = Set.copyOf(retainedSet);
        for (var e : cores.entrySet()) if (e.getKey() != e.getValue().id())
            throw new IllegalArgumentException("Core map key mismatch");
        if (retainedSet.stream().anyMatch(id -> id <= 0)) throw new IllegalArgumentException("Invalid retained identifier");
        if (cores.values().stream().filter(Core::selected).count() > 1)
            throw new IllegalArgumentException("More than one special core selected");
    }

    public static Tms282VMatrixState empty(long validatedPoints) {
        return new Tms282VMatrixState(validatedPoints, 0, false, Map.of(), Set.of());
    }

    public TreeMap<Integer, Core> mutableCores() { return new TreeMap<>(cores); }
    public static long fileTime(long epochMillis) { return Math.multiplyExact(Math.addExact(epochMillis, 11644473600000L), 10000L); }
}
