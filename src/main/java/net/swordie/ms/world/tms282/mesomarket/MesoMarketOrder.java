package net.swordie.ms.world.tms282.mesomarket;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * One row of tms282_meso_market. price = meso per Maple Point, count = Maple Points.
 * escrowMeso: meso still locked by an open buy order. pendingMeso / pendingPoints: settled proceeds or refunds
 * waiting to be claimed by the owning character (works while the owner is offline).
 */
public record MesoMarketOrder(long id, Timestamp createdAt, int worldId, int userId, int characterId, boolean buy,
                              long price, long count, long tradeCount, long receivedCount, long escrowMeso,
                              long pendingMeso, long pendingPoints, String state) {

    public static final String OPEN = "OPEN", FILLED = "FILLED", CANCELLED = "CANCELLED";

    public long remaining() {
        return count - tradeCount;
    }

    public boolean open() {
        return OPEN.equals(state);
    }

    static MesoMarketOrder read(ResultSet r) throws SQLException {
        return new MesoMarketOrder(r.getLong("id"), r.getTimestamp("created_at"), r.getInt("world_id"),
                r.getInt("user_id"), r.getInt("character_id"), r.getBoolean("is_buy"), r.getLong("price"),
                r.getLong("count"), r.getLong("trade_count"), r.getLong("received_count"), r.getLong("escrow_meso"),
                r.getLong("pending_meso"), r.getLong("pending_points"), r.getString("state"));
    }
}
