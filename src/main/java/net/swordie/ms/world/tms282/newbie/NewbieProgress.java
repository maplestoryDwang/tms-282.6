package net.swordie.ms.world.tms282.newbie;

import java.time.Instant;

/**
 * One {@code newbie_flow} row: the per-character progress of the newbie flow (the character's "custom key"; kept in
 * the server's own table instead of a quest record, so nothing about it reaches the 282 client).
 *
 * @param stepId        the step that runs next ("" = the first one)
 * @param branch        the equipment set chosen by the first job ({@code warrior}, ..., "" before / without one)
 * @param giftClaimedAt set once, by the claim transaction ({@link NewbieStore#claimGift})
 * @param jobDoneAt     set once, when the flow advanced the job ({@link NewbieStore#markJobDone})
 * @param completedAt   set when the last step finished; a completed flow never opens again
 */
public record NewbieProgress(int characterId, int accountId, int userId, String loginJob, String stepId, String branch,
                             Instant giftClaimedAt, Instant jobDoneAt, Instant completedAt, Instant createdAt,
                             Instant updatedAt) {

    public boolean giftClaimed() {
        return giftClaimedAt != null;
    }

    public boolean jobDone() {
        return jobDoneAt != null;
    }

    public boolean completed() {
        return completedAt != null;
    }
}
