package net.swordie.ms.world.tms282.newbie;

import net.swordie.ms.client.character.Char;

import java.sql.SQLException;
import java.util.List;

/**
 * What the newbie flow changes in the game world (ports for the checks; {@link WorldNewbieGame} is the server's).
 * Every change runs under the packet lock order of step 6 - save gate(chr) -> ECONOMY_MONITOR -> chr - and checks
 * again under it that the character is still the online one of its client, so a logout save can never run between
 * the database claim and the grant, and nothing is ever given to a character object that was already saved for the
 * last time.
 */
public interface NewbieGame {

    enum Gift {GRANTED, ALREADY, LIMIT, FULL, OFFLINE, BLOCKED, NO_ROW}

    enum Job {ADVANCED, ALREADY, OFFLINE, FAILED}
    enum Points {GRANTED, ALREADY, FAILED}

    /** One line of the gift; {@code periodDays > 0} gives the item an expiry. */
    record Grant(int itemId, int quantity, int periodDays) {
    }

    /** The database claim, run under the locks right before the grant. */
    @FunctionalInterface
    interface GiftClaim {
        NewbieStore.Claim claim() throws SQLException;
    }

    /** The database receipt of the first job, run under the locks after the advancement was saved. */
    @FunctionalInterface
    interface JobClaim {
        boolean claim() throws SQLException;
    }

    /** The character is the online, in-field character of its client. */
    boolean online(Char chr);

    /**
     * Under the locks: online, economic access, every item grantable and the whole gift fits ({@code canHold} of all
     * lines together); then {@code claim}; only a {@link NewbieStore.Claim#CLAIMED} grants meso and items through
     * the game's own primitives ({@code addMoney}, {@code addItemToInventory}; zlog_inventory by the core hook).
     */
    Gift grantGift(Char chr, long meso, List<Grant> items, GiftClaim claim);

    /**
     * Under the locks: online, still {@code fromJob}, then the 282 level service up to
     * {@code levelTo} (its automatic advancement included), the job quests, ScriptManager.jobAdvance (job, skills,
     * SP) and, with {@code autoAssignAp}, resetAP onto the job's main stat. When the level-up already advanced the
     * job (Evan at 10), only the claim stays. Save the character before recording the receipt, including retries
     * whose memory already has {@code toJob} after a failed save.
     */
    Job advanceJob(Char chr, int fromJob, int toJob, List<Integer> quests, int levelTo, boolean autoAssignAp, JobClaim claim);

    /** Zero's prologue owns its SP/appearance; save its bootstrap before the atomic reward/job receipt. */
    Job completeZeroPrologue(Char chr, JobClaim claim);

    /** Saves the character soon (the online save path, off the dialog thread). */
    void saveSoon(Char chr);

    /** Independently retried after the item gift; one durable grant per login user across every world/character. */
    Points grantWelcomePoints(Char chr, int amount);
}
