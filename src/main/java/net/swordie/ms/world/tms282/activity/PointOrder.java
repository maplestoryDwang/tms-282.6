package net.swordie.ms.world.tms282.activity;

import net.swordie.ms.client.User;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.client.character.tms282.EconomyLocks;
import net.swordie.ms.client.character.tms282.Tms282CashExpansionService;
import net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;
import net.swordie.ms.constants.ItemConstants;
import net.swordie.ms.lifecycle.OnlineCharacterSaver;
import net.swordie.ms.loaders.ItemData;
import net.swordie.ms.util.FileTime;
import net.swordie.ms.world.tms282.items.Tms282ItemAvailability;
import net.swordie.ms.world.tms282.activity.PointRaffle.Result;
import net.swordie.ms.world.tms282.activity.PointRaffle.Status;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * A paid raffle draw or gift pack, built by a script ({@code stage.pointOrder(wallet, key, cost)}), then
 * {@link #purchase()}d once:
 * <ol>
 *   <li>every prize is prepared and must be grantable in 282, and the bag must hold all of them ({@code canHold}),
 *       otherwise nothing is charged (UNAVAILABLE / FULL);</li>
 *   <li>one InnoDB transaction: lock the users row, lock every wallet involved (sorted by name), check the balance,
 *       check and count the purchase limits (account_event), charge, credit the bonuses, write the receipt
 *       (account_questex quest {@link PointRaffle#RECEIPT_QUEST});</li>
 *   <li>after the commit: the prepared items go into the bag and the character is saved.</li>
 * </ol>
 * Scripts write their own progress (pity counters, draw totals, pools, broadcasts) only after GRANTED.
 */
public final class PointOrder {
    /** Default end of a permanent account_event row (same as check_log.addEventValue(…, -1)). */
    static final String PERMANENT_RESET = "2035-01-01 00:00:00";

    private final Char chr;
    private final String walletId, key;
    private final int cost;
    private final List<Item> items = new ArrayList<>();
    private final List<String> lines = new ArrayList<>();
    private final Map<String, Integer> credits = new LinkedHashMap<>();
    private final List<Limit> limits = new ArrayList<>();
    private boolean unavailable, used;

    private record Limit(String key, int add, int max, int resetDays) {}
    private record Charge(Status status, long balance, String receipt) {}

    PointOrder(Char chr, String walletId, String key, int cost) {
        if (PointWallets.of(walletId) == null) throw new IllegalArgumentException("Unknown wallet " + walletId);
        if (key == null || !key.matches("[a-z][a-z0-9_]{0,23}") || cost <= 0 || cost > 10_000_000)
            throw new IllegalArgumentException("Invalid point order");
        this.chr = chr; this.walletId = walletId; this.key = key; this.cost = cost;
    }

    /** Script entry: {@code stage.pointOrder("maple", "dragon_slayer", 450)}. */
    public static PointOrder of(Char chr, String walletId, String key, int cost) {
        return new PointOrder(chr, walletId, key, cost);
    }

    private void line(int itemId, int quantity) {
        if (lines.size() >= PointRaffle.MAX_LINES) throw new IllegalArgumentException("Too many prize lines");
        lines.add(itemId + "x" + quantity);
    }

    private static Item template(int itemId) {
        return Tms282ItemAvailability.isGrantable(itemId) ? ItemData.getItemDeepCopy(itemId) : null;
    }

    /** Permanent prize; an equip becomes one item per copy, anything else one stack. */
    public PointOrder item(int itemId, int quantity) {
        return add(itemId, quantity, FileTime::MAX_TIME);
    }

    /** Prize that expires {@code minutes} after the purchase (月卡 boxes). */
    public PointOrder timedItem(int itemId, int quantity, int minutes) {
        if (minutes <= 0 || minutes > 3_650 * 24 * 60) throw new IllegalArgumentException("Invalid item period");
        LocalDateTime end = LocalDateTime.now().plusMinutes(minutes);
        return add(itemId, quantity, () -> FileTime.fromDate(end));
    }

    private PointOrder add(int itemId, int quantity, java.util.function.Supplier<FileTime> expire) {
        if (quantity <= 0 || quantity > 30_000) throw new IllegalArgumentException("Invalid prize quantity");
        line(itemId, quantity);
        Item t = template(itemId);
        if (t == null) { unavailable = true; return this; }
        if (ItemConstants.isEquip(itemId)) {
            if (quantity > 256) throw new IllegalArgumentException("Invalid equip quantity");
            for (int i = 0; i < quantity; i++) {
                Item e = i == 0 ? t : t.deepCopy();
                e.setQuantity(1); e.setDateExpire(expire.get()); items.add(e);
            }
        } else {
            t.setQuantity(quantity); t.setDateExpire(expire.get()); items.add(t);
        }
        return this;
    }

    /** A prize the script already made and adjusted (random stats, stars); granted as-is. */
    public PointOrder prepared(Item item) {
        if (item == null) { unavailable = true; return this; }
        line(item.getItemId(), Math.max(1, item.getQuantity()));
        if (!Tms282ItemAvailability.isGrantable(item.getItemId()) || item.getQuantity() <= 0 || item.getQuantity() > 30_000) {
            unavailable = true; return this;
        }
        if (item.getDateExpire() == null) item.setDateExpire(FileTime.MAX_TIME());
        items.add(item);
        return this;
    }

    /** Extra wallet credit in the same transaction (gift-pack bonus, jackpot). */
    public PointOrder credit(String wallet, int amount) {
        if (PointWallets.of(wallet) == null || amount <= 0) throw new IllegalArgumentException("Invalid credit");
        credits.merge(wallet, amount, Math::addExact);
        return this;
    }

    /**
     * account_event counter checked and written in the transaction (check_log semantics): the purchase is LIMITED when
     * the current unexpired value plus {@code add} would exceed {@code max}. {@code resetDays} -1 = permanent,
     * 0 = end of today, n = end of the day n days from now (only used when a new row is written).
     */
    public PointOrder limit(String eventKey, int add, int max, int resetDays) {
        if (eventKey == null || eventKey.isEmpty() || eventKey.length() > 255 || add <= 0 || max < add || resetDays < -1 || resetDays > 3650)
            throw new IllegalArgumentException("Invalid purchase limit");
        limits.add(new Limit(eventKey, add, max, resetDays));
        return this;
    }

    /** account_event counter written in the transaction without a cap. */
    public PointOrder count(String eventKey, int add, int resetDays) {
        return limit(eventKey, add, Integer.MAX_VALUE, resetDays);
    }

    public int itemCount() { return items.size(); }

    public Result purchase() {
        if (used) throw new IllegalStateException("Point order already purchased");
        used = true;
        if (items.isEmpty() && credits.isEmpty() && !unavailable) throw new IllegalArgumentException("Empty point order");
        if (chr == null || Protocol.active().version() != ProtocolVersion.TMS_282_3) return Result.of(Status.OFFLINE);
        Result[] result = {Result.of(Status.FAILED)};
        EconomyLocks.packet(chr, () -> {
            if (!ActivityRewards.online(chr) || chr.getUser() == null) {result[0] = Result.of(Status.OFFLINE); return null;}
            synchronized (chr.getAccount()) {
                try {
                    Tms282VMatrixRuntime.requireEconomicAccess(chr);
                    if (Tms282CashExpansionService.isBalanceUncertain(chr.getUser())) return null;
                    if (unavailable) {result[0] = Result.of(Status.UNAVAILABLE); return null;}
                    if (!chr.canHold(items)) {result[0] = Result.of(Status.FULL); return null;}
                    List<User> copies = ActivityRewards.walletCopies(chr);
                    Charge charge = ActivityRewards.locked(copies, 0, () -> charge(copies));
                    if (charge.status() != Status.GRANTED) {
                        result[0] = new Result(charge.status(), 0, (int) Math.min(Integer.MAX_VALUE, charge.balance()), null, false);
                        return null;
                    }
                    for (Item item : items) chr.addItemToInventory(item);
                    boolean saved = items.isEmpty() || OnlineCharacterSaver.save(chr) == OnlineCharacterSaver.Result.SAVED;
                    if (!saved) System.out.println("[PointOrder] granted but save not confirmed receipt=" + charge.receipt());
                    result[0] = new Result(Status.GRANTED, cost, (int) Math.min(Integer.MAX_VALUE, charge.balance()), charge.receipt(), saved);
                } catch (SQLException | RuntimeException e) {
                    System.out.println("[PointOrder] purchase failed order=" + key + " wallet=" + walletId + " character=" + chr.getId()
                            + " error=" + e.getClass().getSimpleName());
                }
            }
            return null;
        });
        return result[0];
    }

    private void schema() throws SQLException {
        ActivityRewards.schema(true);
        Set<String> tables = new LinkedHashSet<>();
        tables.addAll(PointWallets.of(walletId).tables());
        for (String w : credits.keySet()) tables.addAll(PointWallets.of(w).tables());
        if (!limits.isEmpty()) tables.add("account_event");
        tables.remove("users"); tables.remove("account_questex");
        if (tables.isEmpty()) return;
        try (Connection c = DatabaseManager.getConnection()) {
            for (String table : tables) {
                try (PreparedStatement p = c.prepareStatement("SELECT ENGINE FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=?")) {
                    p.setString(1, table);
                    try (ResultSet r = p.executeQuery()) {
                        if (!r.next() || !"InnoDB".equalsIgnoreCase(r.getString(1))) throw new SQLException(table + " must be InnoDB");
                    }
                }
            }
        }
    }

    private Charge charge(List<User> copies) throws SQLException {
        schema();
        PointWallet debit = PointWallets.of(walletId);
        // delta per wallet, sorted by name: the lock order for every order of this user
        TreeMap<String, Long> deltas = new TreeMap<>();
        deltas.put(walletId, (long) -cost);
        credits.forEach((w, a) -> deltas.merge(w, (long) a, Long::sum));
        try (Connection c = DatabaseManager.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement p = c.prepareStatement("SELECT id FROM users WHERE id=? FOR UPDATE")) {
                    p.setInt(1, chr.getUser().getId());
                    try (ResultSet r = p.executeQuery()) { if (!r.next()) throw new SQLException("Missing wallet user"); }
                }
                Map<String, Long> before = new TreeMap<>();
                for (String w : deltas.keySet()) before.put(w, PointWallets.of(w).balance(c, chr, true));
                long balance = before.get(walletId);
                if (balance < cost) {
                    c.rollback();
                    debit.sync(copies, balance);
                    return new Charge(Status.INSUFFICIENT, balance, null);
                }
                StringBuilder events = new StringBuilder();
                for (Limit limit : limits) {
                    if (!countLimit(c, limit)) {
                        c.rollback();
                        return new Charge(Status.LIMITED, balance, null);
                    }
                    events.append(events.length() == 0 ? "" : ",").append(limit.key()).append('+').append(limit.add());
                }
                String receiptKey = PointRaffle.receiptKey(key, chr.getId());
                for (var e : deltas.entrySet()) {
                    if (e.getValue() == 0) continue;
                    long delta = e.getValue();
                    if (delta < Integer.MIN_VALUE || delta > Integer.MAX_VALUE) throw new SQLException("Wallet delta out of range");
                    PointWallets.of(e.getKey()).apply(c, chr, before.get(e.getKey()), (int) delta, receiptKey);
                }
                StringBuilder receipt = new StringBuilder("character=").append(chr.getId()).append(";wallet=").append(walletId)
                        .append(";at=").append(Instant.now()).append(";cost=").append(cost).append(";before=").append(balance)
                        .append(";rewards=").append(String.join(",", lines));
                if (!credits.isEmpty()) {
                    receipt.append(";credits=");
                    int i = 0;
                    for (var e : credits.entrySet()) receipt.append(i++ == 0 ? "" : ",").append(e.getKey()).append('+').append(e.getValue());
                }
                if (events.length() > 0) receipt.append(";events=").append(events);
                try (PreparedStatement p = c.prepareStatement(
                        "INSERT INTO account_questex(accountid,questid,`key`,`value`) VALUES(?,?,?,?)")) {
                    p.setInt(1, chr.getAccount().getId()); p.setInt(2, PointRaffle.RECEIPT_QUEST); p.setString(3, receiptKey); p.setString(4, receipt.toString());
                    if (p.executeUpdate() != 1) throw new SQLException("Point order receipt was not saved");
                }
                c.commit();
                for (var e : deltas.entrySet()) PointWallets.of(e.getKey()).sync(copies, before.get(e.getKey()) + e.getValue());
                return new Charge(Status.GRANTED, balance + deltas.get(walletId), receiptKey);
            } catch (SQLException | RuntimeException e) {c.rollback(); throw e;}
        }
    }

    static String resetTime(int resetDays) {
        if (resetDays == -1) return PERMANENT_RESET;
        LocalDateTime end = LocalDate.now().plusDays(resetDays).atTime(LocalTime.of(23, 59, 59));
        return Timestamp.valueOf(end).toString().substring(0, 19);
    }

    /** True when counted; false when the limit is reached. */
    private boolean countLimit(Connection c, Limit limit) throws SQLException {
        int accountId = chr.getAccount().getId();
        boolean exists = false, valid = false;
        long value = 0;
        try (PreparedStatement p = c.prepareStatement(
                "SELECT `value`, (reset_time IS NOT NULL AND reset_time >= NOW()) FROM account_event WHERE account_id=? AND `key`=? FOR UPDATE")) {
            p.setInt(1, accountId); p.setString(2, limit.key());
            try (ResultSet r = p.executeQuery()) {
                if (r.next()) { exists = true; value = r.getLong(1); valid = r.getBoolean(2); }
            }
        }
        long current = valid ? value : 0;
        if (current + limit.add() > limit.max()) return false;
        if (valid) {
            try (PreparedStatement p = c.prepareStatement("UPDATE account_event SET `value`=`value`+? WHERE account_id=? AND `key`=?")) {
                p.setInt(1, limit.add()); p.setInt(2, accountId); p.setString(3, limit.key());
                if (p.executeUpdate() != 1) throw new SQLException("Purchase limit was not saved");
            }
        } else if (exists) {
            try (PreparedStatement p = c.prepareStatement("UPDATE account_event SET `value`=?, reset_time=? WHERE account_id=? AND `key`=?")) {
                p.setInt(1, limit.add()); p.setString(2, resetTime(limit.resetDays())); p.setInt(3, accountId); p.setString(4, limit.key());
                if (p.executeUpdate() != 1) throw new SQLException("Purchase limit was not saved");
            }
        } else {
            try (PreparedStatement p = c.prepareStatement("INSERT INTO account_event(account_id, `key`, `value`, reset_time) VALUES (?, ?, ?, ?)")) {
                p.setInt(1, accountId); p.setString(2, limit.key()); p.setInt(3, limit.add()); p.setString(4, resetTime(limit.resetDays()));
                if (p.executeUpdate() != 1) throw new SQLException("Purchase limit was not saved");
            }
        }
        return true;
    }
}
