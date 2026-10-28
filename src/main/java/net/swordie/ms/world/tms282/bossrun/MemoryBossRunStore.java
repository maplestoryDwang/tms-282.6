package net.swordie.ms.world.tms282.bossrun;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/** In-memory {@link BossRunStore} for the checks and for runs with the real database refused. */
public final class MemoryBossRunStore implements BossRunStore {
    private final AtomicLong ids = new AtomicLong();
    private final List<RunRow> runs = new ArrayList<>();
    private final List<ClearRow> clears = new ArrayList<>();
    private final List<PendingRow> pending = new ArrayList<>();
    private final Map<String, Integer> pq = new HashMap<>();
    private final Map<String, CharRef> chars = new HashMap<>();
    private final List<String> legacyResets = new ArrayList<>();

    public synchronized void addChar(CharRef ref) {
        chars.put(ref.name().toLowerCase(), ref);
    }

    public synchronized List<RunRow> runs() {
        return List.copyOf(runs);
    }

    public synchronized List<ClearRow> clears() {
        return List.copyOf(clears);
    }

    public synchronized List<String> legacyResets() {
        return List.copyOf(legacyResets);
    }

    @Override
    public synchronized long insertRun(RunRow run) {
        long id = ids.incrementAndGet();
        runs.add(new RunRow(id, run.bossKey(), run.orderId(), run.difficulty(), run.fieldId(), run.mobId(), run.leaderId(),
                run.leaderName(), run.partySize(), run.clearMillis(), run.practice(), run.clearedAt(), run.members()));
        return id;
    }

    @Override
    public synchronized void insertClear(ClearRow clear) {
        clears.add(clear);
    }

    @Override
    public synchronized List<ClearRow> countedClears(int accountId, String bossKey, LocalDateTime since) {
        List<ClearRow> out = new ArrayList<>();
        for (ClearRow c : clears) {
            if (c.counted() && c.accountId() == accountId && c.bossKey().equals(bossKey) && !c.clearedAt().isBefore(since)) {
                out.add(c);
            }
        }
        return out;
    }

    @Override
    public synchronized List<ClearRow> countedClears(int accountId, LocalDateTime since) {
        List<ClearRow> out = new ArrayList<>();
        for (ClearRow c : clears) {
            if (c.counted() && c.accountId() == accountId && !c.clearedAt().isBefore(since)) out.add(c);
        }
        return out;
    }

    @Override
    public synchronized List<ClearRow> recentClears(int accountId, int limit) {
        List<ClearRow> out = new ArrayList<>();
        for (ClearRow c : clears) if (c.accountId() == accountId) out.add(c);
        out.sort(Comparator.comparing(ClearRow::clearedAt).reversed());
        return out.size() > limit ? List.copyOf(out.subList(0, limit)) : out;
    }

    @Override
    public synchronized int resetCounted(int accountId, String bossKey) {
        int n = 0;
        for (int i = 0; i < clears.size(); i++) {
            ClearRow c = clears.get(i);
            if (c.counted() && c.accountId() == accountId && (bossKey == null || c.bossKey().equals(bossKey))) {
                clears.set(i, new ClearRow(c.runId(), c.accountId(), c.charId(), c.charName(), c.bossKey(), c.clearedAt(),
                        c.clearMillis(), c.damagePermille(), false, "GM reset"));
                n++;
            }
        }
        return n;
    }

    @Override
    public synchronized void addPending(int charId, int itemId, int quantity, String source) {
        pending.add(new PendingRow(ids.incrementAndGet(), charId, itemId, quantity, source));
    }

    @Override
    public synchronized List<PendingRow> pending(int charId) {
        return pending.stream().filter(p -> p.charId() == charId).toList();
    }

    @Override
    public synchronized boolean deletePending(long id) {
        return pending.removeIf(p -> p.id() == id);
    }

    private static String pqKey(int charId, int logKey, LocalDate day) {
        return charId + ":" + logKey + ":" + day;
    }

    @Override
    public synchronized int pqCount(int charId, int logKey, LocalDate day) {
        return pq.getOrDefault(pqKey(charId, logKey, day), 0);
    }

    @Override
    public synchronized void pqAdd(int charId, int logKey, LocalDate day, int amount) {
        pq.merge(pqKey(charId, logKey, day), amount, Integer::sum);
    }

    @Override
    public synchronized int pqReset(int charId, int logKey) {
        String prefix = charId + ":" + logKey + ":";
        int n = 0;
        for (var it = pq.keySet().iterator(); it.hasNext(); ) {
            if (it.next().startsWith(prefix)) {
                it.remove();
                n++;
            }
        }
        return n;
    }

    @Override
    public synchronized CharRef findChar(String name) {
        return name == null ? null : chars.get(name.toLowerCase());
    }

    @Override
    public synchronized int resetLegacyPartyBoss(int charId, int orderId, int difficulty) {
        legacyResets.add(charId + ":" + orderId + ":" + difficulty);
        return 0;
    }
}
