package net.swordie.ms.world.tms282.newbie;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Persistence of the newbie flow ({@link JdbcNewbieStore}: tables {@code newbie_flow} and {@code newbie_gift_log}).
 * Every change is a compare-and-set in the database, so two dialogs, two channels or two processes can never move
 * one character twice: {@link #advance} only from the expected step, {@link #claimGift} / {@link #markJobDone} only
 * while their column is still NULL.
 */
public interface NewbieStore {

    enum Claim {
        /** This call claimed the gift (row updated and logged in one transaction). */
        CLAIMED,
        /** Already claimed before (or by a concurrent call). */
        ALREADY,
        /** The user's {@code maxClaimsPerUser} is used up; nothing was claimed. */
        LIMIT,
        /** The character has no newbie_flow row. */
        NO_ROW
    }

    /** CREATE TABLE IF NOT EXISTS (the only DDL the server's account may run). */
    void ensureSchema() throws SQLException;

    /** Inserts the row of a new character (no-op when it exists). True when a row was inserted. */
    boolean create(int characterId, int accountId, int userId, String loginJob, String firstStepId) throws SQLException;

    Optional<NewbieProgress> load(int characterId) throws SQLException;

    /** Moves the progress from {@code fromStep} to {@code toStep}; false when it was not at {@code fromStep}. */
    boolean advance(int characterId, String fromStep, String toStep) throws SQLException;

    /** Marks the flow completed, only from {@code fromStep}; false otherwise. */
    boolean complete(int characterId, String fromStep) throws SQLException;

    /**
     * Claims the starter gift once: in one transaction {@code gift_claimed_at} goes from NULL to now and one
     * {@code newbie_gift_log} row (UNIQUE character_id) records what is given. {@code maxClaimsPerUser > 0} also
     * limits the claims of one login user (the user's rows are locked for the count).
     */
    Claim claimGift(int characterId, String configVersion, String branch, String items, int maxClaimsPerUser) throws SQLException;

    /** Sets {@code job_done_at} (and the branch) once; false when it was already set or there is no row. */
    boolean markJobDone(int characterId, String branch) throws SQLException;

    /** Atomically record Zero completion and its original Lv30/60 reward-center entries. Lv100 has no reward. */
    boolean markZeroJobDone(int characterId, String characterName) throws SQLException;

    /** Reopen a previously skipped Zero first-job step, retaining all job/gift receipts. */
    boolean reopenZeroPrologue(int characterId, String firstJobStep) throws SQLException;

    /** Admin view: the newest rows (with the character name when known), at most {@code limit}. */
    List<Row> list(String query, int limit) throws SQLException;

    /** One admin row. */
    record Row(NewbieProgress progress, String name, int level, int job, long posMap) {
    }
}
