package net.swordie.ms.world.tms282.audit;

/**
 * One {@code account_block} row. {@code accountId} is {@code users.id} (the login account).
 *
 * @param endTime epoch millis, or 0 for a permanent block ({@code end_time IS NULL})
 */
public record AccountBlock(int id, int accountId, int type, long createdAt, long endTime, String reason) {

    public boolean isPermanent() {
        return endTime <= 0;
    }

    public boolean isActive(long now) {
        return isPermanent() || endTime > now;
    }

    public BanType banType() {
        BanType t = BanType.getByVal(type);
        return t != null ? t : BanType.GENERAL;
    }

    public boolean blocksLogin() {
        return banType().blocksLogin();
    }

    /** Remaining minutes, or -1 for permanent. */
    public long remainingMinutes(long now) {
        return isPermanent() ? -1 : Math.max(0, (endTime - now) / 60_000L);
    }
}
