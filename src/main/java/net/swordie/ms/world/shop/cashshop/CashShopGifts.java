package net.swordie.ms.world.shop.cashshop;

import net.swordie.ms.Server;
import net.swordie.ms.client.Account;
import net.swordie.ms.client.Client;
import net.swordie.ms.client.User;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.constants.GameConstants;
import net.swordie.ms.enums.CashErrorType;
import net.swordie.ms.handlers.CashShopHandler;
import net.swordie.ms.loaders.CashShopData;
import net.swordie.ms.loaders.Etc.Commodity.CommodityInfo;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Cash shop gifts: the sender pays at once, the gift is stored in {@code cs_gift} and put into the receiver
 * account's cash locker the next time one of its characters enters the cash shop ({@link #deliverPending}); an
 * online receiver is told at once. Schema: sql/tms282/20260928-cashshop.sql (created at runtime if absent).
 */
public final class CashShopGifts {
    private CashShopGifts() {
    }

    static final String DDL = "CREATE TABLE IF NOT EXISTS cs_gift ("
            + "id BIGINT NOT NULL AUTO_INCREMENT,"
            + "sender_user_id INT NOT NULL,"
            + "sender_account_id INT NOT NULL,"
            + "sender_char_id INT NOT NULL,"
            + "sender_name VARCHAR(32) NOT NULL,"
            + "receiver_account_id INT NOT NULL,"
            + "receiver_char_id INT NOT NULL,"
            + "receiver_name VARCHAR(32) NOT NULL,"
            + "commodity_sn INT NOT NULL,"
            + "item_id INT NOT NULL,"
            + "count INT NOT NULL,"
            + "period INT NOT NULL DEFAULT 0,"
            + "price INT NOT NULL DEFAULT 0,"
            + "message VARCHAR(256) NOT NULL DEFAULT '',"
            + "received TINYINT NOT NULL DEFAULT 0,"
            + "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
            + "received_at TIMESTAMP NULL DEFAULT NULL,"
            + "PRIMARY KEY (id),"
            + "KEY idx_cs_gift_receiver (receiver_account_id, received)"
            + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
    private static final Set<String> COLUMNS = Set.of("id", "sender_user_id", "sender_account_id", "sender_char_id",
            "sender_name", "receiver_account_id", "receiver_char_id", "receiver_name", "commodity_sn", "item_id",
            "count", "period", "price", "message", "received", "received_at");
    private static volatile boolean ready;

    /** Creates the table if absent and checks that an existing one has our columns (fails closed otherwise). */
    static synchronized void ensure() throws SQLException {
        if (ready) return;
        try (Connection c = DatabaseManager.getConnection(); Statement s = c.createStatement()) {
            s.executeUpdate(DDL);
            Set<String> present = new HashSet<>();
            try (ResultSet r = c.getMetaData().getColumns(c.getCatalog(), null, "cs_gift", null)) {
                while (r.next()) present.add(r.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
            }
            if (!present.containsAll(COLUMNS)) {
                throw new SQLException("cs_gift exists with another schema: " + present);
            }
        }
        ready = true;
    }

    /** The receiver as stored in the database. */
    public record Receiver(int charId, int accountId, int userId, int worldId, String name, int gender) {
    }

    public record Outcome(boolean done, CashErrorType reason, String receiverName, int itemId, int count) {
        static Outcome fail(CashErrorType reason) {
            return new Outcome(false, reason, null, 0, 0);
        }
    }

    public static Receiver findReceiver(String name) throws SQLException {
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement p = c.prepareStatement(
                "SELECT c.id, c.accid, s.name, s.gender, a.userid, a.worldid FROM characterstats s "
                        + "JOIN characters c ON c.id = s.characterid JOIN accounts a ON a.id = c.accid "
                        + "WHERE s.name = ? LIMIT 1")) {
            p.setString(1, name);
            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) return null;
                return new Receiver(r.getInt(1), r.getInt(2), r.getInt(5), r.getInt(6), r.getString(3), r.getInt(4));
            }
        }
    }

    /**
     * Buys {@code sn} with the sender's prepaid cash for the character named {@code receiverName}. Everything is checked
     * before the debit; the gift row is written right after it (a failed write refunds).
     */
    public static Outcome gift(Char sender, int sn, String receiverName, String message) {
        User user = sender.getUser();
        Account account = sender.getAccount();
        if (receiverName == null || receiverName.isBlank() || receiverName.length() > 15) {
            return Outcome.fail(CashErrorType.CannotFindCharacterGift);
        }
        if (message == null) message = "";
        if (message.length() > 100) message = message.substring(0, 100);
        CommodityInfo ci = CashShopData.getCommodityBySN(sn);
        if (ci == null || CashShopCatalogRules.isBlocked(sn) || ci.getOnSale() == 0
                || !ci.isSaleActive(LocalDateTime.now()) || ci.isOnlyMileage()
                || CashVipPoints.isVipCommodity(sn)) {
            return Outcome.fail(CashErrorType.ThatItemIsNotBeingSold);
        }
        if (!net.swordie.ms.loaders.Tms282CashPackages.components(ci.getItemId()).isEmpty()) {
            return Outcome.fail(CashErrorType.PleaseTryAgain); // package gifts: kind 0x27/0x23 path, not ported
        }
        int cost = new CashShopItem(ci).getNewPrice();
        if (cost <= 0) return Outcome.fail(CashErrorType.ThatItemIsNotBeingSold);
        try {
            CashShopHandler.prepareCashDelivery(ci, List.of(), 1, account, sender);
        } catch (IllegalArgumentException | ArithmeticException e) {
            return Outcome.fail(CashErrorType.ItemChanged_PleaseRefresh);
        }
        Receiver receiver;
        try {
            ensure();
            receiver = findReceiver(receiverName);
        } catch (SQLException e) {
            System.out.println("[CashShop] gift unavailable: " + e.getMessage());
            return Outcome.fail(CashErrorType.PleaseTryAgain);
        }
        if (receiver == null || receiver.worldId() != account.getWorldId()) {
            return Outcome.fail(CashErrorType.CannotFindCharacterGift);
        }
        if (receiver.accountId() == account.getId() || receiver.userId() == user.getId()) {
            return Outcome.fail(CashErrorType.CannotGiftToOwnAccount);
        }
        if (ci.getGender() == 1 && receiver.gender() != 1) {
            // Only the explicit female-only value is enforced: an absent Gender node loads as 0 too.
            return Outcome.fail(CashErrorType.GenderRestriction);
        }
        int period = CashShopPurchaseCounts.period(ci, java.time.LocalDate.now());
        synchronized (net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime.ECONOMY_MONITOR) {
            synchronized (sender) {
                synchronized (user) {
                    net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime.requireEconomicAccess(sender);
                    if (user.getNxPrepaid() < cost) return Outcome.fail(CashErrorType.NotEnoughCash);
                    try {
                        if (!CashShopPurchaseCounts.reserve(user.getId(), ci, 1, period)) {
                            return Outcome.fail(CashErrorType.ExceededThePurchaseLimitForThisItem);
                        }
                    } catch (SQLException e) {
                        return Outcome.fail(CashErrorType.PleaseTryAgain);
                    }
                    try {
                        user.deductNXPrepaid(cost);
                    } catch (RuntimeException e) {
                        cancelCount(user, ci, period);
                        System.out.println("[CashShop] gift debit failed: " + e.getMessage());
                        return Outcome.fail(CashErrorType.NotEnoughCash);
                    }
                    try {
                        insert(user, account, sender, receiver, ci, cost, message);
                    } catch (SQLException e) {
                        System.out.printf("[CashShop] gift row failed after debit (refunding %d): %s%n", cost, e.getMessage());
                        try {
                            user.addNXPrepaid(cost);
                        } catch (RuntimeException refund) {
                            System.out.printf("[CashShop] REFUND FAILED user=%d amount=%d: %s%n", user.getId(), cost,
                                    refund.getMessage());
                        }
                        cancelCount(user, ci, period);
                        return Outcome.fail(CashErrorType.PleaseTryAgain);
                    }
                }
            }
        }
        System.out.printf("[CashShop] gift sn=%d item=%d x%d price=%d from %s(%d) to %s(%d)%n", sn, ci.getItemId(),
                ci.getCount(), cost, sender.getName(), sender.getId(), receiver.name(), receiver.charId());
        notifyReceiver(receiver, sender.getName());
        return new Outcome(true, null, receiver.name(), ci.getItemId(), Math.max(1, ci.getCount()));
    }

    private static void cancelCount(User user, CommodityInfo ci, int period) {
        try {
            CashShopPurchaseCounts.cancel(user.getId(), ci, 1, period);
        } catch (SQLException e) {
            System.out.println("[CashShop] gift count reservation could not be cancelled: " + e.getMessage());
        }
    }

    private static void insert(User user, Account account, Char sender, Receiver receiver, CommodityInfo ci, int cost,
                               String message) throws SQLException {
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement p = c.prepareStatement(
                "INSERT INTO cs_gift (sender_user_id, sender_account_id, sender_char_id, sender_name, receiver_account_id, "
                        + "receiver_char_id, receiver_name, commodity_sn, item_id, count, period, price, message) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
            int i = 1;
            p.setInt(i++, user.getId());
            p.setInt(i++, account.getId());
            p.setInt(i++, sender.getId());
            p.setString(i++, sender.getName());
            p.setInt(i++, receiver.accountId());
            p.setInt(i++, receiver.charId());
            p.setString(i++, receiver.name());
            p.setInt(i++, ci.getSN());
            p.setInt(i++, ci.getItemId());
            p.setInt(i++, Math.max(1, ci.getCount()));
            p.setInt(i++, ci.getPeriod());
            p.setInt(i++, cost);
            p.setString(i, message);
            p.executeUpdate();
        }
    }

    private static void notifyReceiver(Receiver receiver, String senderName) {
        Char online = Server.get().getChars().get(receiver.charId());
        if (online == null) {
            for (Char chr : Server.get().getChars().values()) {
                if (chr.getAccount() != null && chr.getAccount().getId() == receiver.accountId()) {
                    online = chr;
                    break;
                }
            }
        }
        if (online == null) return;
        try {
            Client client = online.getClient();
            if (client != null && client.getCurrentState() == Client.IN_CASH_SHOP) {
                // Not delivered from this (the sender's) thread: the receiver's own requests use its locker. It arrives
                // on the receiver's next shop entry; no field chat packet is sent into the shop stage.
                System.out.printf("[CashShop] gift receiver %d is in the cash shop; delivery on next entry%n",
                        online.getId());
            } else {
                online.chatMessage("收到「" + senderName + "」送來的禮物！請到購物商城的保管箱領取。");
            }
        } catch (RuntimeException e) {
            System.out.println("[CashShop] gift notification failed: " + e.getMessage());
        }
    }

    private record Pending(long id, int sn, int itemId, int count, int period, String sender, String message) {
    }

    /**
     * Moves every undelivered gift of {@code chr}'s account into its cash locker (as a purchase would: CashItemInfo
     * row, trunk, item row). Gifts that do not fit stay pending. Returns the number delivered. Sends nothing: the
     * caller (the cash shop entry, in the shop stage) sends the locker afterwards.
     */
    public static int deliverPending(Char chr) {
        Account account = chr.getAccount();
        if (account == null || account.getTrunk() == null) return 0;
        List<Pending> pending = new ArrayList<>();
        try {
            ensure();
            try (Connection c = DatabaseManager.getConnection(); PreparedStatement p = c.prepareStatement(
                    "SELECT id, commodity_sn, item_id, count, period, sender_name, message FROM cs_gift "
                            + "WHERE receiver_account_id = ? AND received = 0 ORDER BY id")) {
                p.setInt(1, account.getId());
                try (ResultSet r = p.executeQuery()) {
                    while (r.next()) {
                        pending.add(new Pending(r.getLong(1), r.getInt(2), r.getInt(3), r.getInt(4), r.getInt(5),
                                r.getString(6), r.getString(7)));
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("[CashShop] pending gifts unavailable: " + e.getMessage());
            return 0;
        }
        int delivered = 0;
        synchronized (account.getTrunk()) {
            for (Pending gift : pending) {
                CommodityInfo ci = new CommodityInfo();
                ci.setSN(gift.sn());
                ci.setItemId(gift.itemId());
                ci.setCount(gift.count());
                ci.setPeriod(gift.period());
                List<CashItemInfo> items;
                try {
                    items = CashShopHandler.prepareCashDelivery(ci, List.of(), 1, account, chr);
                } catch (IllegalArgumentException | ArithmeticException e) {
                    System.out.printf("[CashShop] gift %d (item %d) cannot be built: %s%n", gift.id(), gift.itemId(),
                            e.getMessage());
                    continue;
                }
                if (account.getTrunk().getLocker().size() + items.size() > GameConstants.MAX_LOCKER_SIZE) {
                    // Stays pending; no chat line here: the caller is in the cash shop stage.
                    System.out.printf("[CashShop] locker of account %d full; gift %d stays pending%n", account.getId(), gift.id());
                    break;
                }
                if (!claim(gift.id())) continue;
                for (CashItemInfo cii : items) {
                    cii.setBuyCharacterID(gift.sender());
                    cii.updateCashItemInfoToSQL();
                    account.getTrunk().addCashItem(cii);
                    cii.getItem().setTrunkID(account.getTrunk().getId());
                    cii.getItem().saveToSQL();
                }
                delivered++;
                System.out.printf("[CashShop] gift %d delivered to account %d (item %d from %s)%n", gift.id(),
                        account.getId(), gift.itemId(), gift.sender());
            }
        }
        return delivered;
    }

    /** Number of undelivered gifts for an account (login notice). */
    public static int pendingCount(int accountId) {
        try {
            ensure();
            try (Connection c = DatabaseManager.getConnection(); PreparedStatement p = c.prepareStatement(
                    "SELECT COUNT(*) FROM cs_gift WHERE receiver_account_id = ? AND received = 0")) {
                p.setInt(1, accountId);
                try (ResultSet r = p.executeQuery()) {
                    return r.next() ? r.getInt(1) : 0;
                }
            }
        } catch (SQLException e) {
            return 0;
        }
    }

    /** Login hook (for the login path owner): tells the character about gifts waiting in the cash shop. */
    public static void notifyOnLogin(Char chr) {
        if (chr == null || chr.getAccount() == null) return;
        int n = pendingCount(chr.getAccount().getId());
        if (n > 0) chr.chatMessage("您有 " + n + " 份禮物尚未領取，請到購物商城的保管箱領取。");
    }

    private static boolean claim(long id) {
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement p = c.prepareStatement(
                "UPDATE cs_gift SET received = 1, received_at = CURRENT_TIMESTAMP WHERE id = ? AND received = 0")) {
            p.setLong(1, id);
            return p.executeUpdate() == 1;
        } catch (SQLException e) {
            System.out.println("[CashShop] gift claim failed: " + e.getMessage());
            return false;
        }
    }
}
