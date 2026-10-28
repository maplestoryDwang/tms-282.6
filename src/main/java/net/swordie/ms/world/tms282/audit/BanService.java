package net.swordie.ms.world.tms282.audit;

import net.swordie.ms.connection.hikariCP.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * IP bans ({@code block_ip}) and typed, timed account bans ({@code account_block}).
 * <p>
 * Lookups ({@link #isIpBanned}, {@link #activeBan}) are served from an in-memory cache - O(1), no DB access, safe
 * on any thread. The cache is loaded once (synchronously, on the first lookup after the DB pool is up - that
 * first lookup happens on the login thread, never a game thread) and then refreshed every
 * {@code tms282.ban.refreshSec} seconds (default 60) on the audit thread so rows added by a web panel are picked up.
 * All writes update the cache immediately and persist asynchronously on the audit thread.
 * <p>
 * IP rules: an exact address ({@code 1.2.3.4}) or a prefix ending in {@code *} ({@code 1.2.3.*}, {@code 1.2.*}).
 */
public final class BanService {

    private static final long REFRESH_SEC = Math.max(10L, Long.getLong("tms282.ban.refreshSec", 60L));

    private static volatile Set<String> exactIps = ConcurrentHashMap.newKeySet();
    private static final CopyOnWriteArrayList<String> prefixIps = new CopyOnWriteArrayList<>();
    private static volatile Map<Integer, List<AccountBlock>> blocks = new ConcurrentHashMap<>();
    private static final AtomicBoolean refreshScheduled = new AtomicBoolean();
    private static volatile boolean loaded;

    private BanService() {
    }

    // ------------------------------------------------------------- lookups

    /** True if {@code ip} matches an exact or prefix ({@code a.b.c.*}) entry in {@code block_ip}. */
    public static boolean isIpBanned(String ip) {
        ensureLoaded();
        String n = normalizeIp(ip);
        if (n.isEmpty()) {
            return false;
        }
        if (exactIps.contains(n)) {
            return true;
        }
        for (String prefix : prefixIps) {
            if (n.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /** The active login-blocking ban with the latest end (permanent wins), if any. {@code accountId = users.id}. */
    public static Optional<AccountBlock> activeBan(int accountId) {
        ensureLoaded();
        List<AccountBlock> list = blocks.get(accountId);
        if (list == null || list.isEmpty()) {
            return Optional.empty();
        }
        long now = System.currentTimeMillis();
        AccountBlock best = null;
        for (AccountBlock b : list) {
            if (!b.blocksLogin() || !b.isActive(now)) {
                continue;
            }
            if (best == null || b.isPermanent() || (!best.isPermanent() && b.endTime() > best.endTime())) {
                best = b;
            }
        }
        return Optional.ofNullable(best);
    }

    /** Active restriction of a specific type (e.g. {@link BanType#CHAT} mute, {@link BanType#TRADE}). */
    public static Optional<AccountBlock> activeBan(int accountId, BanType type) {
        ensureLoaded();
        List<AccountBlock> list = blocks.get(accountId);
        if (list == null) {
            return Optional.empty();
        }
        long now = System.currentTimeMillis();
        for (AccountBlock b : list) {
            if (b.type() == type.getVal() && b.isActive(now)) {
                return Optional.of(b);
            }
        }
        return Optional.empty();
    }

    /** Convenience for login hooks: IP ban or login-blocking account ban. */
    public static boolean isLoginBlocked(int accountId, String ip) {
        return isIpBanned(ip) || activeBan(accountId).isPresent();
    }

    /** Human readable reason for a login refusal, or null when not blocked. */
    public static String loginBlockMessage(int accountId, String ip) {
        if (isIpBanned(ip)) {
            return "此 IP 已被封鎖。";
        }
        Optional<AccountBlock> ban = activeBan(accountId);
        if (ban.isEmpty()) {
            return null;
        }
        AccountBlock b = ban.get();
        long mins = b.remainingMinutes(System.currentTimeMillis());
        return "賬號已被封鎖（" + b.banType().getLabel() + "）"
                + (mins < 0 ? "，永久。" : "，剩餘 " + mins + " 分鐘。")
                + (b.reason() != null ? " 原因: " + b.reason() : "");
    }

    // -------------------------------------------------------------- writes

    public static CompletableFuture<Boolean> banIp(String ip, String reason) {
        String n = normalizeIp(ip);
        if (n.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }
        addIpToCache(n);
        return runDb(con -> {
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO `block_ip` (`created_at`, `ip`, `reason`) VALUES (?, ?, ?)")) {
                ps.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
                ps.setString(2, n);
                ps.setString(3, cut(reason));
                return ps.executeUpdate() > 0;
            }
        });
    }

    public static CompletableFuture<Boolean> unbanIp(String ip) {
        String n = normalizeIp(ip);
        exactIps.remove(n);
        prefixIps.remove(n.endsWith("*") ? n.substring(0, n.length() - 1) : n);
        return runDb(con -> {
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM `block_ip` WHERE `ip` = ?")) {
                ps.setString(1, n);
                return ps.executeUpdate() > 0;
            }
        });
    }

    /**
     * Adds an {@code account_block} row.
     *
     * @param hours {@code <= 0} = permanent ({@code end_time NULL})
     */
    public static CompletableFuture<Boolean> tempBan(int accountId, BanType type, long hours, String reason) {
        long now = System.currentTimeMillis();
        long end = hours <= 0 ? 0L : now + TimeUnit.HOURS.toMillis(hours);
        AccountBlock block = new AccountBlock(-1, accountId, type.getVal(), now, end, cut(reason));
        blocks.computeIfAbsent(accountId, k -> new CopyOnWriteArrayList<>()).add(block);
        return runDb(con -> {
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO `account_block` (`created_at`, `account_id`, `type`, `end_time`, `reason`) VALUES (?, ?, ?, ?, ?)")) {
                ps.setTimestamp(1, new Timestamp(now));
                ps.setInt(2, accountId);
                ps.setInt(3, type.getVal());
                if (end > 0) {
                    ps.setTimestamp(4, new Timestamp(end));
                } else {
                    ps.setNull(4, Types.TIMESTAMP);
                }
                ps.setString(5, cut(reason));
                return ps.executeUpdate() > 0;
            }
        });
    }

    /** Ends every active block of {@code accountId} (optionally only of {@code type}) by setting end_time = now. */
    public static CompletableFuture<Integer> liftBans(int accountId, BanType typeOrNull) {
        List<AccountBlock> list = blocks.get(accountId);
        if (list != null) {
            list.removeIf(b -> typeOrNull == null || b.type() == typeOrNull.getVal());
        }
        CompletableFuture<Integer> f = new CompletableFuture<>();
        AuditWriter.executor().execute(() -> {
            if (!dbReady()) {
                f.complete(-1);
                return;
            }
            String sql = "UPDATE `account_block` SET `end_time` = ? WHERE `account_id` = ? "
                    + "AND (`end_time` IS NULL OR `end_time` > ?)" + (typeOrNull != null ? " AND `type` = ?" : "");
            try (Connection con = DatabaseManager.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
                Timestamp ts = new Timestamp(System.currentTimeMillis());
                ps.setTimestamp(1, ts);
                ps.setInt(2, accountId);
                ps.setTimestamp(3, ts);
                if (typeOrNull != null) {
                    ps.setInt(4, typeOrNull.getVal());
                }
                f.complete(ps.executeUpdate());
            } catch (SQLException e) {
                System.err.println("[BAN] liftBans failed: " + e.getMessage());
                f.complete(-1);
            }
        });
        return f;
    }

    /** users.id of a character by name, looked up on the audit thread (online characters are resolved by callers). */
    public static CompletableFuture<Integer> findAccountIdByCharName(String name) {
        CompletableFuture<Integer> f = new CompletableFuture<>();
        AuditWriter.executor().execute(() -> {
            if (!dbReady()) {
                f.complete(0);
                return;
            }
            String sql = "SELECT a.userid FROM characterstats cs JOIN characters c ON c.id = cs.characterid "
                    + "JOIN accounts a ON a.id = c.accid WHERE cs.name = ? LIMIT 1";
            try (Connection con = DatabaseManager.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) {
                    f.complete(rs.next() ? rs.getInt(1) : 0);
                }
            } catch (SQLException e) {
                System.err.println("[BAN] account lookup failed: " + e.getMessage());
                f.complete(0);
            }
        });
        return f;
    }

    /** Snapshot of the cached active blocks of an account (for GM info). */
    public static List<AccountBlock> blocksOf(int accountId) {
        List<AccountBlock> list = blocks.get(accountId);
        return list == null ? Collections.emptyList() : new ArrayList<>(list);
    }

    /** Forces a cache reload on the audit thread. */
    public static void refreshAsync() {
        AuditWriter.executor().execute(BanService::reload);
    }

    // ------------------------------------------------------------ internals

    private interface DbWork {
        boolean run(Connection con) throws SQLException;
    }

    private static CompletableFuture<Boolean> runDb(DbWork work) {
        CompletableFuture<Boolean> f = new CompletableFuture<>();
        AuditWriter.executor().execute(() -> {
            if (!dbReady()) {
                f.complete(false);
                return;
            }
            try (Connection con = DatabaseManager.getConnection()) {
                f.complete(work.run(con));
            } catch (SQLException e) {
                System.err.println("[BAN] db write failed: " + e.getMessage());
                f.complete(false);
            }
        });
        return f;
    }

    private static boolean dbReady() {
        return DatabaseManager.isInitialized() && AuditWriter.ensureSchema();
    }

    private static void ensureLoaded() {
        if (!loaded && DatabaseManager.isInitialized()) {
            synchronized (BanService.class) {
                if (!loaded) {
                    reload();
                }
            }
        }
        if (refreshScheduled.compareAndSet(false, true)) {
            AuditWriter.executor().scheduleWithFixedDelay(BanService::reload, REFRESH_SEC, REFRESH_SEC, TimeUnit.SECONDS);
        }
    }

    private static synchronized void reload() {
        if (!dbReady()) {
            return;
        }
        Set<String> newExact = ConcurrentHashMap.newKeySet();
        List<String> newPrefix = new ArrayList<>();
        Map<Integer, List<AccountBlock>> newBlocks = new ConcurrentHashMap<>();
        try (Connection con = DatabaseManager.getConnection(); Statement st = con.createStatement()) {
            try (ResultSet rs = st.executeQuery("SELECT `ip` FROM `block_ip`")) {
                while (rs.next()) {
                    String n = normalizeIp(rs.getString(1));
                    if (n.endsWith("*")) {
                        newPrefix.add(n.substring(0, n.length() - 1));
                    } else if (!n.isEmpty()) {
                        newExact.add(n);
                    }
                }
            }
            try (ResultSet rs = st.executeQuery("SELECT `id`, `account_id`, `type`, `created_at`, `end_time`, `reason` "
                    + "FROM `account_block` WHERE `end_time` IS NULL OR `end_time` > NOW(3)")) {
                while (rs.next()) {
                    Timestamp created = rs.getTimestamp(4);
                    Timestamp end = rs.getTimestamp(5);
                    AccountBlock b = new AccountBlock(rs.getInt(1), rs.getInt(2), rs.getInt(3),
                            created != null ? created.getTime() : 0L, end != null ? end.getTime() : 0L, rs.getString(6));
                    newBlocks.computeIfAbsent(b.accountId(), k -> new CopyOnWriteArrayList<>()).add(b);
                }
            }
        } catch (SQLException e) {
            System.err.println("[BAN] cache reload failed: " + e.getMessage());
            return;
        }
        // keep bans issued in the last 2 minutes that may not have been persisted when the SELECT ran
        long recent = System.currentTimeMillis() - 120_000L;
        for (List<AccountBlock> list : blocks.values()) {
            for (AccountBlock b : list) {
                if (b.id() < 0 && b.createdAt() > recent) {
                    newBlocks.computeIfAbsent(b.accountId(), k -> new CopyOnWriteArrayList<>()).add(b);
                }
            }
        }
        exactIps = newExact;
        prefixIps.clear();
        prefixIps.addAll(newPrefix);
        blocks = newBlocks;
        loaded = true;
    }

    private static void addIpToCache(String n) {
        if (n.endsWith("*")) {
            prefixIps.addIfAbsent(n.substring(0, n.length() - 1));
        } else {
            exactIps.add(n);
        }
    }

    /** Strips a leading '/', a trailing ':port' (IPv4 only) and whitespace. */
    static String normalizeIp(String ip) {
        if (ip == null) {
            return "";
        }
        String n = ip.trim();
        if (n.startsWith("/")) {
            n = n.substring(1);
        }
        int colon = n.indexOf(':');
        if (colon > 0 && n.indexOf(':', colon + 1) < 0) {
            n = n.substring(0, colon);
        }
        return n;
    }

    private static String cut(String s) {
        return s == null ? null : (s.length() > 1000 ? s.substring(0, 1000) : s);
    }
}
