package net.swordie.ms.world.tms282.bossrun;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Persistence of the boss-run ledger (Repository). {@link JdbcBossRunStore} is the server store (tables created at
 * runtime with CREATE TABLE IF NOT EXISTS, same DDL as {@code sql/tms282/20260929-bossrun.sql});
 * {@link MemoryBossRunStore} is used by the checks ({@code -Dtms282.tests.rejectRealDatabase=true}).
 */
public interface BossRunStore {

    /** One party clear. members = "name:percent,..." in damage order. */
    record RunRow(long runId, String bossKey, int orderId, int difficulty, int fieldId, int mobId, int leaderId,
                  String leaderName, int partySize, long clearMillis, boolean practice, LocalDateTime clearedAt,
                  String members) {
    }

    /** One member of a clear. counted=false: logged only (practice never logs; capped members log uncounted). */
    record ClearRow(long runId, int accountId, int charId, String charName, String bossKey, LocalDateTime clearedAt,
                    long clearMillis, int damagePermille, boolean counted, String note) {
    }

    record PendingRow(long id, int charId, int itemId, int quantity, String source) {
    }

    record CharRef(int charId, int accountId, String name) {
    }

    long insertRun(RunRow run);

    void insertClear(ClearRow clear);

    /** Counted clears of an account for one boss key at or after {@code since}. */
    List<ClearRow> countedClears(int accountId, String bossKey, LocalDateTime since);

    /** Counted clears of an account (all bosses) at or after {@code since}. */
    List<ClearRow> countedClears(int accountId, LocalDateTime since);

    /** Latest clear rows of an account, newest first. */
    List<ClearRow> recentClears(int accountId, int limit);

    /** Marks counted clears uncounted (the log stays); bossKey null = every boss. Returns rows changed. */
    int resetCounted(int accountId, String bossKey);

    void addPending(int charId, int itemId, int quantity, String source);

    List<PendingRow> pending(int charId);

    /** Deletes one pending row; true when this call removed it (claim is exactly once). */
    boolean deletePending(long id);

    int pqCount(int charId, int logKey, LocalDate day);

    void pqAdd(int charId, int logKey, LocalDate day, int amount);

    int pqReset(int charId, int logKey);

    /** Online or offline character by name; null when unknown. */
    CharRef findChar(String name);

    /** Legacy per-character counters (partyboss table) of a character; orderId &lt; 0 = all. */
    int resetLegacyPartyBoss(int charId, int orderId, int difficulty);
}
