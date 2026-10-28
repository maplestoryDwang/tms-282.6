package net.swordie.ms.world.tms282.mesomarket;

import net.swordie.ms.connection.hikariCP.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Persistence and matching for the meso market. Tables are created at runtime with CREATE TABLE IF NOT EXISTS
 * (copy in sql/tms282/20260928-mesomarket-jewel.sql). All mutating calls run inside one JDBC transaction and are
 * additionally serialized by {@link MesoMarketService#LOCK}.
 */
public final class MesoMarketStore {
    private MesoMarketStore() { }

    public static final String ORDER_TABLE = "tms282_meso_market";
    public static final String TRADE_TABLE = "tms282_meso_market_trade";

    static final String CREATE_ORDERS = "CREATE TABLE IF NOT EXISTS " + ORDER_TABLE + " ("
            + "id BIGINT NOT NULL AUTO_INCREMENT,"
            + "created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),"
            + "updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),"
            + "world_id INT NOT NULL DEFAULT 0,"
            + "user_id INT NOT NULL,"
            + "character_id INT NOT NULL,"
            + "is_buy TINYINT(1) NOT NULL DEFAULT 0,"
            + "price BIGINT NOT NULL,"
            + "count BIGINT NOT NULL,"
            + "trade_count BIGINT NOT NULL DEFAULT 0,"
            + "received_count BIGINT NOT NULL DEFAULT 0,"
            + "escrow_meso BIGINT NOT NULL DEFAULT 0,"
            + "pending_meso BIGINT NOT NULL DEFAULT 0,"
            + "pending_points BIGINT NOT NULL DEFAULT 0,"
            + "state VARCHAR(16) NOT NULL DEFAULT 'OPEN',"
            + "PRIMARY KEY (id),"
            + "KEY idx_book (world_id,state,is_buy,price,id),"
            + "KEY idx_user_day (user_id,created_at),"
            + "KEY idx_char (character_id)"
            + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

    static final String CREATE_TRADES = "CREATE TABLE IF NOT EXISTS " + TRADE_TABLE + " ("
            + "id BIGINT NOT NULL AUTO_INCREMENT,"
            + "created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),"
            + "world_id INT NOT NULL DEFAULT 0,"
            + "buy_order_id BIGINT NOT NULL,"
            + "sell_order_id BIGINT NOT NULL,"
            + "price BIGINT NOT NULL,"
            + "count BIGINT NOT NULL,"
            + "gross_meso BIGINT NOT NULL,"
            + "tax_meso BIGINT NOT NULL,"
            + "PRIMARY KEY (id),"
            + "KEY idx_buy (buy_order_id),"
            + "KEY idx_sell (sell_order_id)"
            + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

    private static volatile boolean ready;

    static synchronized void ensure() throws SQLException {
        if (ready) return;
        try (Connection c = DatabaseManager.getConnection(); Statement s = c.createStatement()) {
            s.executeUpdate(CREATE_ORDERS);
            s.executeUpdate(CREATE_TRADES);
        }
        ready = true;
    }

    /** One execution between a resting (maker) order and the incoming (taker) order. */
    public record Fill(long takerId, long makerId, int makerCharacterId, boolean makerIsBuy, long price, long count,
                       long grossMeso, long taxMeso) { }

    /** Amounts atomically moved out of pending columns. */
    public record Claim(long meso, long points, List<Long> orderIds, boolean mesoDeferred, boolean pointsDeferred) {
        public boolean empty() {
            return meso == 0 && points == 0;
        }
    }

    public record PriceLevel(long price, long count, int orders) { }

    static int countCreatedToday(int userId) throws SQLException {
        ensure();
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement p = c.prepareStatement(
                "SELECT COUNT(*) FROM " + ORDER_TABLE + " WHERE user_id=? AND created_at>=CURDATE()")) {
            p.setInt(1, userId);
            try (ResultSet r = p.executeQuery()) { return r.next() ? r.getInt(1) : 0; }
        }
    }

    static int countOpen(int userId) throws SQLException {
        ensure();
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement p = c.prepareStatement(
                "SELECT COUNT(*) FROM " + ORDER_TABLE + " WHERE user_id=? AND state='OPEN'")) {
            p.setInt(1, userId);
            try (ResultSet r = p.executeQuery()) { return r.next() ? r.getInt(1) : 0; }
        }
    }

    /**
     * Inserts the new order (escrow already taken by the caller) and matches it against the book in one transaction.
     * Returns the created order id followed by its fills. On any exception nothing is written.
     */
    static PlaceResult placeAndMatch(MesoMarketConfig cfg, int worldId, int userId, int characterId, boolean buy,
                                     long price, long count) throws SQLException {
        ensure();
        try (Connection c = DatabaseManager.getConnection()) {
            c.setAutoCommit(false);
            try {
                long id;
                try (PreparedStatement p = c.prepareStatement("INSERT INTO " + ORDER_TABLE
                        + " (world_id,user_id,character_id,is_buy,price,count,escrow_meso,state) VALUES (?,?,?,?,?,?,?,'OPEN')",
                        Statement.RETURN_GENERATED_KEYS)) {
                    p.setInt(1, worldId); p.setInt(2, userId); p.setInt(3, characterId); p.setBoolean(4, buy);
                    p.setLong(5, price); p.setLong(6, count); p.setLong(7, buy ? Math.multiplyExact(price, count) : 0);
                    p.executeUpdate();
                    try (ResultSet k = p.getGeneratedKeys()) {
                        if (!k.next()) throw new SQLException("no generated order id");
                        id = k.getLong(1);
                    }
                }
                List<Fill> fills = match(c, cfg, id);
                c.commit();
                return new PlaceResult(id, fills);
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }

    public record PlaceResult(long orderId, List<Fill> fills) {
        public long filled() {
            return fills.stream().mapToLong(Fill::count).sum();
        }
    }

    private static MesoMarketOrder lock(Connection c, long id) throws SQLException {
        try (PreparedStatement p = c.prepareStatement("SELECT * FROM " + ORDER_TABLE + " WHERE id=? FOR UPDATE")) {
            p.setLong(1, id);
            try (ResultSet r = p.executeQuery()) { return r.next() ? MesoMarketOrder.read(r) : null; }
        }
    }

    /** Price-time priority; trades at the resting order's price; self-trades (same login account) are skipped. */
    static List<Fill> match(Connection c, MesoMarketConfig cfg, long takerId) throws SQLException {
        List<Fill> fills = new ArrayList<>();
        MesoMarketOrder taker = lock(c, takerId);
        while (taker != null && taker.open() && taker.remaining() > 0) {
            String sql = "SELECT * FROM " + ORDER_TABLE + " WHERE world_id=? AND state='OPEN' AND is_buy=? AND user_id<>? AND "
                    + (taker.buy() ? "price<=? ORDER BY price ASC, id ASC" : "price>=? ORDER BY price DESC, id ASC")
                    + " LIMIT 1 FOR UPDATE";
            MesoMarketOrder maker;
            try (PreparedStatement p = c.prepareStatement(sql)) {
                p.setInt(1, taker.worldId()); p.setBoolean(2, !taker.buy()); p.setInt(3, taker.userId());
                p.setLong(4, taker.price());
                try (ResultSet r = p.executeQuery()) { maker = r.next() ? MesoMarketOrder.read(r) : null; }
            }
            if (maker == null) break;
            long qty = Math.min(taker.remaining(), maker.remaining());
            long tradePrice = maker.price();
            long gross = Math.multiplyExact(tradePrice, qty);
            long tax = cfg.tax(gross);
            MesoMarketOrder buyer = taker.buy() ? taker : maker;
            MesoMarketOrder seller = taker.buy() ? maker : taker;
            long buyerLocked = Math.multiplyExact(buyer.price(), qty);
            long buyerRefund = buyerLocked - gross; // price improvement returned to the buyer
            try (PreparedStatement p = c.prepareStatement("UPDATE " + ORDER_TABLE
                    + " SET trade_count=trade_count+?, pending_points=pending_points+?, escrow_meso=escrow_meso-?,"
                    + " pending_meso=pending_meso+?, state=IF(trade_count>=count,'FILLED',state) WHERE id=?")) {
                p.setLong(1, qty); p.setLong(2, qty); p.setLong(3, buyerLocked); p.setLong(4, buyerRefund);
                p.setLong(5, buyer.id());
                p.executeUpdate();
            }
            try (PreparedStatement p = c.prepareStatement("UPDATE " + ORDER_TABLE
                    + " SET trade_count=trade_count+?, pending_meso=pending_meso+?,"
                    + " state=IF(trade_count>=count,'FILLED',state) WHERE id=?")) {
                p.setLong(1, qty); p.setLong(2, gross - tax); p.setLong(3, seller.id());
                p.executeUpdate();
            }
            try (PreparedStatement p = c.prepareStatement("INSERT INTO " + TRADE_TABLE
                    + " (world_id,buy_order_id,sell_order_id,price,count,gross_meso,tax_meso) VALUES (?,?,?,?,?,?,?)")) {
                p.setInt(1, taker.worldId()); p.setLong(2, buyer.id()); p.setLong(3, seller.id());
                p.setLong(4, tradePrice); p.setLong(5, qty); p.setLong(6, gross); p.setLong(7, tax);
                p.executeUpdate();
            }
            fills.add(new Fill(taker.id(), maker.id(), maker.characterId(), maker.buy(), tradePrice, qty, gross, tax));
            taker = lock(c, takerId);
        }
        return fills;
    }

    /** Cancels an open order owned by the character; the remaining escrow becomes claimable. */
    static MesoMarketOrder cancel(int characterId, long orderId) throws SQLException {
        ensure();
        try (Connection c = DatabaseManager.getConnection()) {
            c.setAutoCommit(false);
            try {
                MesoMarketOrder o = lock(c, orderId);
                if (o == null || o.characterId() != characterId || !o.open()) {
                    c.rollback();
                    return null;
                }
                try (PreparedStatement p = c.prepareStatement("UPDATE " + ORDER_TABLE
                        + " SET pending_meso=pending_meso+escrow_meso, escrow_meso=0,"
                        + " pending_points=pending_points+IF(is_buy=0,count-trade_count,0), state='CANCELLED' WHERE id=?")) {
                    p.setLong(1, orderId);
                    p.executeUpdate();
                }
                MesoMarketOrder after = lock(c, orderId);
                c.commit();
                return after;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }

    /**
     * Moves every pending amount of the character's orders out of the table. Meso is only claimed when all of it fits
     * {@code mesoRoom}; points only when they fit {@code pointRoom}; otherwise that part stays pending.
     */
    static Claim claim(int characterId, long mesoRoom, long pointRoom) throws SQLException {
        ensure();
        try (Connection c = DatabaseManager.getConnection()) {
            c.setAutoCommit(false);
            try {
                long meso = 0, points = 0;
                List<Long> ids = new ArrayList<>();
                try (PreparedStatement p = c.prepareStatement("SELECT id,pending_meso,pending_points FROM " + ORDER_TABLE
                        + " WHERE character_id=? AND (pending_meso>0 OR pending_points>0) FOR UPDATE")) {
                    p.setInt(1, characterId);
                    try (ResultSet r = p.executeQuery()) {
                        while (r.next()) {
                            ids.add(r.getLong(1));
                            meso = Math.addExact(meso, r.getLong(2));
                            points = Math.addExact(points, r.getLong(3));
                        }
                    }
                }
                boolean takeMeso = meso > 0 && meso <= mesoRoom;
                boolean takePoints = points > 0 && points <= pointRoom;
                if (!takeMeso && !takePoints) {
                    c.rollback();
                    return new Claim(0, 0, ids, meso > 0, points > 0);
                }
                try (PreparedStatement p = c.prepareStatement("UPDATE " + ORDER_TABLE + " SET "
                        + (takeMeso ? "pending_meso=0," : "")
                        + (takePoints ? "received_count=received_count+pending_points, pending_points=0," : "")
                        + " updated_at=CURRENT_TIMESTAMP(3) WHERE character_id=? AND (pending_meso>0 OR pending_points>0)")) {
                    p.setInt(1, characterId);
                    p.executeUpdate();
                }
                c.commit();
                return new Claim(takeMeso ? meso : 0, takePoints ? points : 0, ids,
                        meso > 0 && !takeMeso, points > 0 && !takePoints);
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }

    /** Puts amounts back into pending after a failed credit (compensation for {@link #claim}). */
    static void restorePending(long orderId, long meso, long points) throws SQLException {
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement p = c.prepareStatement("UPDATE " + ORDER_TABLE
                + " SET pending_meso=pending_meso+?, pending_points=pending_points+?,"
                + " received_count=GREATEST(0,received_count-?) WHERE id=?")) {
            p.setLong(1, meso); p.setLong(2, points); p.setLong(3, points); p.setLong(4, orderId);
            if (p.executeUpdate() != 1) throw new SQLException("meso market order " + orderId + " vanished");
        }
    }

    static List<PriceLevel> book(int worldId, boolean buySide, int depth) throws SQLException {
        ensure();
        List<PriceLevel> out = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement p = c.prepareStatement(
                "SELECT price, SUM(count-trade_count), COUNT(*) FROM " + ORDER_TABLE
                        + " WHERE world_id=? AND state='OPEN' AND is_buy=? GROUP BY price ORDER BY price "
                        + (buySide ? "DESC" : "ASC") + " LIMIT ?")) {
            p.setInt(1, worldId); p.setBoolean(2, buySide); p.setInt(3, Math.max(1, depth));
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) out.add(new PriceLevel(r.getLong(1), r.getLong(2), r.getInt(3)));
            }
        }
        return out;
    }

    /** Open orders plus any order that still has something to claim. */
    static List<MesoMarketOrder> ordersOf(int characterId) throws SQLException {
        ensure();
        List<MesoMarketOrder> out = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement p = c.prepareStatement(
                "SELECT * FROM " + ORDER_TABLE + " WHERE character_id=? AND (state='OPEN' OR pending_meso>0 OR pending_points>0)"
                        + " ORDER BY id DESC LIMIT 50")) {
            p.setInt(1, characterId);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) out.add(MesoMarketOrder.read(r));
            }
        }
        return out;
    }

    static long lastTradePrice(int worldId) throws SQLException {
        ensure();
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement p = c.prepareStatement(
                "SELECT price FROM " + TRADE_TABLE + " WHERE world_id=? ORDER BY id DESC LIMIT 1")) {
            p.setInt(1, worldId);
            try (ResultSet r = p.executeQuery()) { return r.next() ? r.getLong(1) : 0; }
        }
    }
}
