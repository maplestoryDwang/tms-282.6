package net.swordie.ms.world.tms282.contents.common;

import net.swordie.ms.Server;
import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.client.character.tms282.EconomyLocks;
import net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;
import net.swordie.ms.constants.ItemConstants;
import net.swordie.ms.lifecycle.OnlineCharacterSaver;
import net.swordie.ms.loaders.ItemData;
import net.swordie.ms.util.FileTime;
import net.swordie.ms.world.tms282.items.Tms282ItemAvailability;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Account-wide receipts for event rewards (content-1005: 菇菇通行證, 楓之谷學院, 次元之塔), stored as rows of
 * {@code account_questex(accountid, questid, key, value)} like the Codex {@code ActivityRewards} receipts.
 * <p>
 * {@link #claim} follows the same order as {@code ActivityRewards.grant}: inside {@link EconomyLocks#packet} and the
 * account monitor it re-reads the receipts, runs the caller's guard on them (e.g. "at most 5 this week"), prepares
 * every reward (282-grantable, permanent), checks the bag, inserts the receipt with {@code INSERT IGNORE} (a second
 * claim of the same key is ALREADY), adds the items and saves the character at once. A crash between the receipt and
 * the save leaves a receipt without items (to be reviewed by hand from the receipt text), never items without a
 * receipt.
 */
public final class AccountLedger {
    /** Receipt key: lower-case letters, digits and {@code _ : . -}, at most 64 characters (account_questex.key). */
    public static final String KEY_PATTERN = "[a-z0-9_:.\\-]{1,64}";

    public enum Result {GRANTED, ALREADY, DENIED, FULL, UNAVAILABLE, OFFLINE, FAILED}

    public record Reward(int itemId, int quantity) {
        public Reward {
            if (itemId <= 0 || quantity <= 0 || quantity > 30_000) {
                throw new IllegalArgumentException("bad reward " + itemId + " x" + quantity);
            }
        }
    }

    /** Result plus the guard's reason for DENIED (null otherwise). */
    public record Outcome(Result result, String reason) {
        public boolean granted() {
            return result == Result.GRANTED;
        }
    }

    /** Receipt storage (SQL in the server, a map in tests). */
    public interface Store {
        /** Every receipt of {@code accountId}/{@code questId} whose key starts with {@code prefix}. */
        Map<String, String> read(int accountId, int questId, String prefix) throws SQLException;

        /** False when the key already exists. */
        boolean insert(int accountId, int questId, String key, String value) throws SQLException;
    }

    /** Character-side effects (the real server, or a recording stub in tests). */
    public interface Runtime {
        boolean online(Char chr);

        void locked(Char chr, Runnable body);

        boolean save(Char chr);

        /** A prepared permanent item, or null when it cannot be granted on this server. */
        Item prepare(int itemId, int quantity);

        boolean canHold(Char chr, List<Item> items);

        void add(Char chr, Item item);
    }

    private static volatile AccountLedger instance;
    private final Store store;
    private final Runtime runtime;

    public AccountLedger(Store store, Runtime runtime) {
        this.store = Objects.requireNonNull(store, "store");
        this.runtime = Objects.requireNonNull(runtime, "runtime");
    }

    public static AccountLedger get() {
        AccountLedger l = instance;
        if (l == null) {
            synchronized (AccountLedger.class) {
                l = instance;
                if (l == null) {
                    l = new AccountLedger(new SqlStore(), new ServerRuntime());
                    instance = l;
                }
            }
        }
        return l;
    }

    /** Tests: replace the shared ledger (null restores the server one on the next {@link #get}). */
    public static void install(AccountLedger ledger) {
        instance = ledger;
    }

    public static void checkKey(String key) {
        if (key == null || !key.matches(KEY_PATTERN)) {
            throw new IllegalArgumentException("bad receipt key " + key);
        }
    }

    /** Receipts of the character's account with keys starting with {@code prefix}; empty when offline or on error. */
    public Map<String, String> receipts(Char chr, int questId, String prefix) {
        if (chr == null || chr.getAccount() == null) {
            return Map.of();
        }
        try {
            return store.read(chr.getAccount().getId(), questId, prefix == null ? "" : prefix);
        } catch (SQLException | RuntimeException e) {
            System.out.println("[AccountLedger] read failed quest=" + questId + " character=" + chr.getId() + " error="
                    + e.getClass().getSimpleName());
            return Map.of();
        }
    }

    /**
     * Claims receipt {@code key} of {@code questId} once per account and grants {@code rewards} (may be empty: a pure
     * record). {@code guard} sees the account's receipts starting with {@code guardPrefix} (read inside the lock) and
     * returns null to allow or a reason to refuse.
     */
    public Outcome claim(Char chr, int questId, String key, String detail, List<Reward> rewards, String guardPrefix,
                         Function<Map<String, String>, String> guard) {
        checkKey(key);
        List<Reward> list = List.copyOf(rewards == null ? List.of() : rewards);
        if (list.size() > 32) {
            throw new IllegalArgumentException("too many rewards");
        }
        if (chr == null || chr.getAccount() == null) {
            return new Outcome(Result.OFFLINE, null);
        }
        Outcome[] out = {new Outcome(Result.FAILED, null)};
        runtime.locked(chr, () -> {
            boolean online;
            try {
                online = runtime.online(chr);
            } catch (RuntimeException e) {
                online = false;
            }
            if (!online) {
                out[0] = new Outcome(Result.OFFLINE, null);
                return;
            }
            synchronized (chr.getAccount()) {
                int account = chr.getAccount().getId();
                try {
                    if (store.read(account, questId, key).containsKey(key)) {
                        out[0] = new Outcome(Result.ALREADY, null);
                        return;
                    }
                    Map<String, String> existing = guardPrefix == null ? Map.of() : store.read(account, questId, guardPrefix);
                    String reason = guard == null ? null : guard.apply(existing);
                    if (reason != null) {
                        out[0] = new Outcome(Result.DENIED, reason);
                        return;
                    }
                    List<Item> items = new ArrayList<>();
                    for (Reward r : list) {
                        Item item = runtime.prepare(r.itemId(), r.quantity());
                        if (item == null) {
                            out[0] = new Outcome(Result.UNAVAILABLE, String.valueOf(r.itemId()));
                            return;
                        }
                        if (ItemConstants.isEquip(r.itemId())) {
                            for (int i = 0; i < r.quantity(); i++) {
                                Item copy = i == 0 ? item : item.deepCopy();
                                copy.setQuantity(1);
                                items.add(copy);
                            }
                        } else {
                            items.add(item);
                        }
                    }
                    if (!items.isEmpty() && !runtime.canHold(chr, items)) {
                        out[0] = new Outcome(Result.FULL, null);
                        return;
                    }
                    StringBuilder receipt = new StringBuilder("character=").append(chr.getId()).append(";at=")
                            .append(java.time.Instant.now()).append(";rewards=");
                    for (int i = 0; i < list.size(); i++) {
                        receipt.append(i == 0 ? "" : ",").append(list.get(i).itemId()).append('x').append(list.get(i).quantity());
                    }
                    if (detail != null && !detail.isEmpty()) {
                        receipt.append(';').append(detail);
                    }
                    if (!store.insert(account, questId, key, receipt.toString())) {
                        out[0] = new Outcome(Result.ALREADY, null);
                        return;
                    }
                    for (Item item : items) {
                        runtime.add(chr, item);
                    }
                    out[0] = new Outcome(runtime.save(chr) ? Result.GRANTED : Result.FAILED, null);
                    System.out.println("[AccountLedger] quest=" + questId + " key=" + key + " character=" + chr.getId()
                            + " account=" + account + " result=" + out[0].result());
                } catch (SQLException | RuntimeException e) {
                    System.out.println("[AccountLedger] claim failed quest=" + questId + " key=" + key + " character="
                            + chr.getId() + " error=" + e.getClass().getSimpleName() + ": " + e.getMessage());
                    out[0] = new Outcome(Result.FAILED, null);
                }
            }
        });
        return out[0];
    }

    // ------------------------------------------------------------------------------------------------ server side

    /** account_questex, the table of ActivityRewards (created the same way when missing). */
    public static final class SqlStore implements Store {
        private volatile boolean ready;

        private void schema(Connection c) throws SQLException {
            if (ready) {
                return;
            }
            try (Statement s = c.createStatement()) {
                s.executeUpdate("CREATE TABLE IF NOT EXISTS account_questex (accountid INT NOT NULL, questid INT NOT NULL, "
                        + "`key` VARCHAR(64) NOT NULL, `value` TEXT, PRIMARY KEY(accountid,questid,`key`)) ENGINE=InnoDB");
            }
            ready = true;
        }

        @Override
        public Map<String, String> read(int accountId, int questId, String prefix) throws SQLException {
            Map<String, String> out = new LinkedHashMap<>();
            try (Connection c = DatabaseManager.getConnection()) {
                schema(c);
                try (PreparedStatement p = c.prepareStatement(
                        "SELECT `key`,`value` FROM account_questex WHERE accountid=? AND questid=? AND `key` LIKE ? ESCAPE '!' ORDER BY `key`")) {
                    p.setInt(1, accountId);
                    p.setInt(2, questId);
                    p.setString(3, prefix.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%");
                    try (ResultSet r = p.executeQuery()) {
                        while (r.next()) {
                            out.put(r.getString(1), r.getString(2));
                        }
                    }
                }
            }
            return out;
        }

        @Override
        public boolean insert(int accountId, int questId, String key, String value) throws SQLException {
            try (Connection c = DatabaseManager.getConnection()) {
                schema(c);
                if (!c.getAutoCommit()) {
                    throw new SQLException("receipt requires autocommit");
                }
                try (PreparedStatement p = c.prepareStatement(
                        "INSERT IGNORE INTO account_questex(accountid,questid,`key`,`value`) VALUES(?,?,?,?)")) {
                    p.setInt(1, accountId);
                    p.setInt(2, questId);
                    p.setString(3, key);
                    p.setString(4, value);
                    return p.executeUpdate() == 1;
                }
            }
        }
    }

    /** The live server: 282 profile, character in field, economy locks, immediate save. */
    public static final class ServerRuntime implements Runtime {
        @Override
        public boolean online(Char chr) {
            if (Protocol.active().version() != ProtocolVersion.TMS_282_3 || chr == null || !chr.isOnline()
                    || chr.getId() <= 0 || chr.getAccount() == null || chr.getAccount().getId() <= 0) {
                return false;
            }
            Client client = chr.getClient();
            if (client == null || client.getChr() != chr || Server.get().getChars().get(chr.getId()) != chr
                    || client.getCh() == null || !client.getCh().isActive()
                    || client.getCurrentState() != Client.IN_FIELD || client.getNextState() != Client.NONE) {
                return false;
            }
            Tms282VMatrixRuntime.requireEconomicAccess(chr);
            return true;
        }

        @Override
        public void locked(Char chr, Runnable body) {
            EconomyLocks.packet(chr, () -> {
                body.run();
                return null;
            });
        }

        @Override
        public boolean save(Char chr) {
            return OnlineCharacterSaver.save(chr) == OnlineCharacterSaver.Result.SAVED;
        }

        @Override
        public Item prepare(int itemId, int quantity) {
            if (!Tms282ItemAvailability.isGrantable(itemId)) {
                return null;
            }
            Item item = ItemData.getItemDeepCopy(itemId);
            if (item == null) {
                return null;
            }
            item.setQuantity(ItemConstants.isEquip(itemId) ? 1 : quantity);
            item.setDateExpire(FileTime.MAX_TIME());
            return item;
        }

        @Override
        public boolean canHold(Char chr, List<Item> items) {
            return chr.canHold(items);
        }

        @Override
        public void add(Char chr, Item item) {
            chr.addItemToInventory(item);
        }
    }
}
