package net.swordie.ms.world.tms282.activity;

import net.swordie.ms.Server;
import net.swordie.ms.client.Account;
import net.swordie.ms.client.Client;
import net.swordie.ms.client.User;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.client.character.tms282.EconomyLocks;
import net.swordie.ms.client.character.tms282.Tms282CashExpansionService;
import net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime;
import net.swordie.ms.connection.api.launcher.OnlineAccounts;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;
import net.swordie.ms.constants.ItemConstants;
import net.swordie.ms.lifecycle.OnlineCharacterSaver;
import net.swordie.ms.loaders.ItemData;
import net.swordie.ms.util.FileTime;
import net.swordie.ms.world.tms282.items.Tms282ItemAvailability;

import java.sql.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Daily activity receipts precede ordinary inventory grants, as with newbie gifts. A crash between receipt and
 * inventory saves needs manual receipt review; this is not an atomic inventory transaction or automatic recovery. */
public final class ActivityRewards {
    public static final int RECEIPT_QUEST = 99_999_998;
    private static final int PARK_QUEST = 18805;
    private static final DateTimeFormatter PARK_DATE = DateTimeFormatter.ofPattern("yy/MM/dd");
    public enum Result {GRANTED, ALREADY, FULL, UNAVAILABLE, OFFLINE, FAILED}
    public enum AdmissionStatus {ADMITTED, LIMIT, INSUFFICIENT, OFFLINE, FAILED}
    public record Admission(AdmissionStatus status, int count, int charged, int balance) {}
    private ActivityRewards() {}

    /** The same "really in field on this channel" test the receipts use (revival exchange). */
    public static boolean isOnline(Char chr) {
        return online(chr);
    }

    static boolean online(Char chr) {
        if (chr == null || !chr.isOnline() || chr.getId() <= 0 || chr.getAccount() == null || chr.getAccount().getId() <= 0
                || chr.getUser() == null || chr.getUser().getId() <= 0) return false;
        Client client = chr.getClient();
        return client != null && client.getChr() == chr && Server.get().getChars().get(chr.getId()) == chr
                && client.getCh() != null && client.getCh().isActive()
                && client.getCurrentState() == Client.IN_FIELD && client.getNextState() == Client.NONE;
    }

    /** A legacy non-InnoDB table or wrong primary key cannot support the receipt/fee guarantees. Never ALTER it. */
    static void schema(boolean wallet) throws SQLException {
        try (Connection c = DatabaseManager.getConnection(); Statement s = c.createStatement()) {
            s.executeUpdate("CREATE TABLE IF NOT EXISTS account_questex (accountid INT NOT NULL, questid INT NOT NULL, `key` VARCHAR(64) NOT NULL, `value` TEXT, PRIMARY KEY(accountid,questid,`key`)) ENGINE=InnoDB");
            for (String table : wallet ? List.of("account_questex", "users") : List.of("account_questex")) {
                try (PreparedStatement p = c.prepareStatement("SELECT ENGINE FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=?")) {
                    p.setString(1, table);
                    try (ResultSet r = p.executeQuery()) {
                        if (!r.next() || !"InnoDB".equalsIgnoreCase(r.getString(1))) throw new SQLException(table + " must be InnoDB");
                    }
                }
            }
            try (PreparedStatement p = c.prepareStatement("SELECT COLUMN_NAME,SUB_PART FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='account_questex' AND INDEX_NAME='PRIMARY' ORDER BY SEQ_IN_INDEX"); ResultSet r = p.executeQuery()) {
                List<String> columns = new ArrayList<>();
                while (r.next()) {
                    if (r.getObject(2) != null) throw new SQLException("Activity receipt primary key cannot use a prefix");
                    columns.add(r.getString(1).toLowerCase(Locale.ROOT));
                }
                if (!columns.equals(List.of("accountid", "questid", "key"))) throw new SQLException("account_questex primary key must be accountid,questid,key");
            }
            try (PreparedStatement p = c.prepareStatement("SELECT COLUMN_NAME,DATA_TYPE,CHARACTER_MAXIMUM_LENGTH FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='account_questex' AND COLUMN_NAME IN ('key','value')"); ResultSet r = p.executeQuery()) {
                int valid = 0;
                while (r.next()) {
                    String name = r.getString(1), type = r.getString(2).toLowerCase(Locale.ROOT);
                    long size = r.getLong(3);
                    if (name.equalsIgnoreCase("key") && type.equals("varchar") && size >= 64
                            || name.equalsIgnoreCase("value") && Set.of("varchar", "text", "mediumtext", "longtext").contains(type) && size >= 1024) valid++;
                }
                if (valid != 2) throw new SQLException("Activity record columns cannot hold date keys and complete receipts");
            }
        }
    }

    public static Result grant(Char chr, String activityKey, int[] itemIds, int[] quantities) {
        if (activityKey == null || !activityKey.matches("[a-z][a-z0-9_]{0,31}"))
            throw new IllegalArgumentException("Invalid activity reward request");
        // The date is read inside the account lock, as before (a claim waiting across midnight counts for the new day).
        return grantKeyed(chr, activityKey, () -> activityKey + ":" + LocalDate.now(), itemIds, quantities);
    }

    /** Receipt keys: lower-case activity, then ':'-separated claim parts (e.g. {@code rv_punch:2026-10-05:rank1}). */
    public static boolean validReceiptKey(String key) {
        return key != null && key.length() <= 64 && key.matches("[a-z][a-z0-9_]{0,31}(:[A-Za-z0-9_.-]{1,24}){1,3}");
    }

    /**
     * {@link #grant} with a caller-chosen account receipt key instead of the daily one: one claim per key and account,
     * the receipt is written (autocommit INSERT IGNORE) before the items reach the inventory.
     */
    public static Result grantOnce(Char chr, String receiptKey, int[] itemIds, int[] quantities) {
        if (!validReceiptKey(receiptKey)) throw new IllegalArgumentException("Invalid activity reward request");
        return grantKeyed(chr, receiptKey, () -> receiptKey, itemIds, quantities);
    }

    private static Result grantKeyed(Char chr, String activityKey, java.util.function.Supplier<String> receiptKey,
                                     int[] itemIds, int[] quantities) {
        if (itemIds == null || quantities == null
                || itemIds.length == 0 || itemIds.length > 32 || itemIds.length != quantities.length)
            throw new IllegalArgumentException("Invalid activity reward request");
        int[] ids = itemIds.clone(), counts = quantities.clone();
        for (int count : counts) if (count <= 0 || count > 30_000) throw new IllegalArgumentException("Invalid activity reward quantity");
        if (chr == null || Protocol.active().version() != ProtocolVersion.TMS_282_3) return Result.OFFLINE;
        Result[] result = {Result.FAILED};
        EconomyLocks.packet(chr, () -> {
            if (!online(chr)) {result[0] = Result.OFFLINE; return null;}
            synchronized (chr.getAccount()) {
                try {
                    Tms282VMatrixRuntime.requireEconomicAccess(chr);
                    schema(false);
                    String key = receiptKey.get();
                    try (Connection c = DatabaseManager.getConnection(); PreparedStatement p = c.prepareStatement(
                            "SELECT `value` FROM account_questex WHERE accountid=? AND questid=? AND `key`=?")) {
                        p.setInt(1, chr.getAccount().getId()); p.setInt(2, RECEIPT_QUEST); p.setString(3, key);
                        try (ResultSet r = p.executeQuery()) {if (r.next()) {result[0] = Result.ALREADY; return null;}}
                    }
                    List<Item> prepared = new ArrayList<>();
                    for (int line = 0; line < ids.length; line++) {
                        if (!Tms282ItemAvailability.isGrantable(ids[line])) {result[0] = Result.UNAVAILABLE; return null;}
                        Item template = ItemData.getItemDeepCopy(ids[line]);
                        if (template == null) {result[0] = Result.UNAVAILABLE; return null;}
                        if (ItemConstants.isEquip(ids[line])) {
                            if (counts[line] > 256) {result[0] = Result.FULL; return null;}
                            for (int i = 0; i < counts[line]; i++) {
                                Item equip = template.deepCopy(); equip.setQuantity(1); equip.setDateExpire(FileTime.MAX_TIME()); prepared.add(equip);
                            }
                        } else {
                            template.setQuantity(counts[line]); template.setDateExpire(FileTime.MAX_TIME()); prepared.add(template);
                        }
                    }
                    if (!chr.canHold(prepared)) {result[0] = Result.FULL; return null;}
                    StringBuilder receipt = new StringBuilder("character=").append(chr.getId()).append(";at=").append(Instant.now()).append(";rewards=");
                    for (int i = 0; i < ids.length; i++) receipt.append(i == 0 ? "" : ",").append(ids[i]).append('x').append(counts[i]);
                    try (Connection c = DatabaseManager.getConnection(); PreparedStatement p = c.prepareStatement(
                            "INSERT IGNORE INTO account_questex(accountid,questid,`key`,`value`) VALUES(?,?,?,?)")) {
                        if (!c.getAutoCommit()) throw new SQLException("Activity receipt requires autocommit");
                        p.setInt(1, chr.getAccount().getId()); p.setInt(2, RECEIPT_QUEST); p.setString(3, key); p.setString(4, receipt.toString());
                        int affected = p.executeUpdate();
                        if (affected == 0) {
                            try (PreparedStatement check = c.prepareStatement("SELECT `value` FROM account_questex WHERE accountid=? AND questid=? AND `key`=?")) {
                                check.setInt(1, chr.getAccount().getId()); check.setInt(2, RECEIPT_QUEST); check.setString(3, key);
                                try (ResultSet r = check.executeQuery()) {
                                    if (!r.next()) throw new SQLException("Activity receipt was ignored without an existing claim");
                                }
                            }
                            result[0] = Result.ALREADY; return null;
                        }
                        if (affected != 1) throw new SQLException("Activity receipt was not saved");
                    }
                    for (Item item : prepared) chr.addItemToInventory(item);
                    result[0] = OnlineCharacterSaver.save(chr) == OnlineCharacterSaver.Result.SAVED ? Result.GRANTED : Result.FAILED;
                } catch (SQLException | RuntimeException e) {
                    System.out.println("[ActivityRewards] grant failed activity=" + activityKey + " character=" + chr.getId() + " error=" + e.getClass().getSimpleName());
                }
            }
            return null;
        });
        return result[0];
    }

    interface WalletWork<T> {T run() throws SQLException;}
    /** Every live copy of the user, in a stable order, so wallet writers lock them consistently. */
    static List<User> walletCopies(Char chr) {
        Set<User> unique = Collections.newSetFromMap(new IdentityHashMap<>());
        unique.addAll(OnlineAccounts.live().copies(chr.getUser().getId())); unique.add(chr.getUser());
        List<User> copies = new ArrayList<>(unique); copies.sort(Comparator.comparingInt(System::identityHashCode));
        return copies;
    }
    static <T> T locked(List<User> copies, int index, WalletWork<T> work) throws SQLException {
        if (index == copies.size()) return work.run();
        synchronized (copies.get(index)) {return locked(copies, index + 1, work);}
    }

    /** Call after the real warp succeeded. Wallet plus daily count are one transaction; warp plus commit ACK are
     * not. An unknown commit outcome may consume the fee/count although event initialization rolls back the warp. */
    public static Admission admitMonsterPark(Char chr) {
        Admission failed = new Admission(AdmissionStatus.FAILED, 0, 0, 0);
        if (chr == null || Protocol.active().version() != ProtocolVersion.TMS_282_3) return new Admission(AdmissionStatus.OFFLINE, 0, 0, 0);
        Admission[] result = {failed};
        EconomyLocks.packet(chr, () -> {
            if (!online(chr) || chr.getUser() == null) {result[0] = new Admission(AdmissionStatus.OFFLINE, 0, 0, 0); return null;}
            synchronized (chr.getAccount()) {
                try {
                    Tms282VMatrixRuntime.requireEconomicAccess(chr);
                    if (Tms282CashExpansionService.isBalanceUncertain(chr.getUser())) return null;
                    List<User> copies = walletCopies(chr);
                    result[0] = locked(copies, 0, () -> admit(chr, copies));
                } catch (SQLException | RuntimeException e) {
                    System.out.println("[ActivityRewards] park admission failed character=" + chr.getId() + " error=" + e.getClass().getSimpleName());
                }
            }
            return null;
        });
        return result[0];
    }

    private static Admission admit(Char chr, List<User> copies) throws SQLException {
        schema(true);
        try (Connection c = DatabaseManager.getConnection()) {
            c.setAutoCommit(false);
            try {
                int balance;
                try (PreparedStatement p = c.prepareStatement("SELECT maplePoints FROM users WHERE id=? FOR UPDATE")) {
                    p.setInt(1, chr.getUser().getId());
                    try (ResultSet r = p.executeQuery()) {
                        if (!r.next()) throw new SQLException("Missing activity wallet");
                        long amount = r.getLong(1);
                        if (r.wasNull() || amount < 0 || amount > Integer.MAX_VALUE) throw new SQLException("Invalid activity wallet");
                        balance = (int) amount;
                    }
                }
                Map<String, String> records = new HashMap<>();
                try (PreparedStatement p = c.prepareStatement("SELECT `key`,`value` FROM account_questex WHERE accountid=? AND questid=? AND `key` IN ('count','date') FOR UPDATE")) {
                    p.setInt(1, chr.getAccount().getId()); p.setInt(2, PARK_QUEST);
                    try (ResultSet r = p.executeQuery()) {while (r.next()) records.put(r.getString(1), r.getString(2));}
                }
                String today = LocalDate.now().format(PARK_DATE); // after any wallet/record-lock wait at midnight
                boolean newDay = !today.equals(records.get("date"));
                int count = 0;
                if (!newDay) {
                    try {count = Integer.parseInt(records.getOrDefault("count", "0"));}
                    catch (NumberFormatException e) {throw new SQLException("Invalid activity count", e);}
                    if (count < 0) throw new SQLException("Invalid activity count");
                }
                int fee = count >= 2 ? 20 : 0;
                if (count >= 7 || balance < fee) {
                    c.rollback(); for (User copy : copies) copy.setMaplePoints(balance);
                    return new Admission(count >= 7 ? AdmissionStatus.LIMIT : AdmissionStatus.INSUFFICIENT, count, 0, balance);
                }
                if (fee > 0) {
                    try (PreparedStatement p = c.prepareStatement("UPDATE users SET maplePoints=? WHERE id=?")) {
                        p.setInt(1, balance - fee); p.setInt(2, chr.getUser().getId());
                        if (p.executeUpdate() != 1) throw new SQLException("Activity fee was not saved");
                    }
                }
                Map<String, String> updates = new LinkedHashMap<>();
                updates.put("count", String.valueOf(count + 1)); updates.put("date", today);
                if (newDay) {updates.put("tryCount", "0"); updates.put("clear", "0"); updates.put("Td", today);}
                String tuples = String.join(",", Collections.nCopies(updates.size(), "(?,?,?,?)"));
                try (PreparedStatement p = c.prepareStatement("INSERT INTO account_questex(accountid,questid,`key`,`value`) VALUES" + tuples + " ON DUPLICATE KEY UPDATE `value`=VALUES(`value`)")) {
                    int index = 1;
                    for (var entry : updates.entrySet()) {
                        p.setInt(index++, chr.getAccount().getId()); p.setInt(index++, PARK_QUEST); p.setString(index++, entry.getKey()); p.setString(index++, entry.getValue());
                    }
                    if (p.executeUpdate() <= 0) throw new SQLException("Activity count was not saved");
                }
                c.commit(); balance -= fee;
                for (User copy : copies) copy.setMaplePoints(balance);
                return new Admission(AdmissionStatus.ADMITTED, count + 1, fee, balance);
            } catch (SQLException | RuntimeException e) {c.rollback(); throw e;}
        }
    }
}
