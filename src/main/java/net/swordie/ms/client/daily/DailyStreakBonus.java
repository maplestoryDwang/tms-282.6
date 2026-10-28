package net.swordie.ms.client.daily;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.swordie.ms.connection.api.launcher.gift.GiftSchema;
import net.swordie.ms.connection.hikariCP.DatabaseManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Streak bonus on top of the 28-day daily gift (2026-10-08): when an account's daily-gift claims of this month reach N
 * consecutive days (3 / 7 / 14 by default, {@code bonuses} in {@code data/launcher/checkin.json}, re-read on change),
 * the bonus items are queued once per month and threshold for the claiming character (launcher_reward_queue, source
 * "streak"). The streak counts the claim days of dailygift_log, so it starts again on the 1st with the gift itself.
 * Called after a claim in the launcher ({@link DailyGiftLauncherClaim}) and in the game ({@link DailyGift#claim}).
 */
public final class DailyStreakBonus {
    public static final Path CONFIG = Path.of("data", "launcher", "checkin.json");
    static final String DDL = "CREATE TABLE IF NOT EXISTS daily_streak_bonus ("
            + "account_id INT NOT NULL,"
            + "month CHAR(7) NOT NULL,"
            + "days INT NOT NULL,"
            + "day DATE NOT NULL,"
            + "character_id INT NOT NULL,"
            + "PRIMARY KEY (account_id, month, days)"
            + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";

    public record Item(int itemId, int quantity, String name) {
        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("itemId", itemId);
            m.put("quantity", quantity);
            m.put("name", name);
            return m;
        }
    }

    public record Bonus(int days, List<Item> items) {
        public Map<String, Object> toMap() {
            return Map.of("days", days, "items", items.stream().map(Item::toMap).toList());
        }
    }

    public record Granted(int days, List<Item> items) {
    }

    /** An account's streak this month and the bonuses already given. */
    public record Status(int streak, boolean claimedToday, List<Integer> given, List<Bonus> bonuses) {
        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("streak", streak);
            m.put("claimedToday", claimedToday);
            m.put("given", given);
            m.put("bonuses", bonuses.stream().map(Bonus::toMap).toList());
            return m;
        }
    }

    static final List<Bonus> DEFAULTS = List.of(
            new Bonus(3, List.of(new Item(5062010, 3, "黑色方塊"))),
            new Bonus(7, List.of(new Item(5062500, 5, "附加方塊"))),
            new Bonus(14, List.of(new Item(5062006, 3, "白金奇幻方塊"), new Item(2450042, 5, "經驗值2倍券(1小時)"))));

    private static volatile boolean ready;
    private static volatile List<Bonus> config = DEFAULTS;
    private static volatile long stamp = -1;

    private DailyStreakBonus() {
    }

    static void ensure() throws SQLException {
        if (ready) return;
        synchronized (DailyStreakBonus.class) {
            if (ready) return;
            try (Connection c = DatabaseManager.getConnection(); Statement s = c.createStatement()) {
                s.executeUpdate(DDL);
            }
            ready = true;
        }
    }

    /** The bonuses of checkin.json (re-read when it changes; a broken file keeps the last good list). */
    public static List<Bonus> bonuses() {
        try {
            long st = Files.isRegularFile(CONFIG) ? Files.getLastModifiedTime(CONFIG).toMillis() : 0;
            if (st != stamp) {
                boolean first = stamp == -1;
                config = st == 0 ? DEFAULTS : parse(new ObjectMapper().readTree(Files.readAllBytes(CONFIG)));
                stamp = st;
                if (!first) {
                    // the file changed while the server runs: have the icons of its items ready for the launcher
                    List<Integer> ids = new ArrayList<>();
                    for (Bonus b : config) {
                        for (Item i : b.items()) {
                            ids.add(i.itemId());
                        }
                    }
                    net.swordie.ms.connection.api.launcher.daily.ItemIconService.standard().ensureAsync(ids);
                }
            }
        } catch (IOException | RuntimeException e) {
            System.out.println("[StreakBonus] " + CONFIG + " not used: " + e.getMessage());
            stamp = -2;
        }
        return config;
    }

    static List<Bonus> parse(JsonNode root) {
        List<Bonus> out = new ArrayList<>();
        for (JsonNode b : root.path("bonuses")) {
            int days = b.path("days").asInt(0);
            if (days < 2 || days > 28) throw new IllegalArgumentException("bonus days " + days);
            List<Item> items = new ArrayList<>();
            for (JsonNode i : b.path("items")) {
                int id = i.path("itemId").asInt(0);
                int qty = i.path("quantity").asInt(1);
                if (id < 1_000_000 || id > 9_999_999 || qty < 1 || qty > 9999) throw new IllegalArgumentException("item " + id + " x" + qty);
                items.add(new Item(id, qty, i.path("name").asText("").strip()));
            }
            if (items.isEmpty() || items.size() > 10) throw new IllegalArgumentException("bonus " + days + ": 1..10 items");
            out.add(new Bonus(days, List.copyOf(items)));
        }
        out.sort((a, b) -> Integer.compare(a.days(), b.days()));
        return List.copyOf(out);
    }

    static LocalDate today() {
        return LocalDate.now(ZoneId.systemDefault());
    }

    /** Consecutive days in {@code days} (days of the month) ending at {@code end}. */
    static int streak(TreeSet<Integer> days, int end) {
        int n = 0;
        for (int d = end; d >= 1 && days.contains(d); d--) n++;
        return n;
    }

    static TreeSet<Integer> claimDays(Connection c, int accountId, YearMonth month) {
        TreeSet<Integer> out = new TreeSet<>();
        try {
            for (LocalDate d : DailyGiftLogStore.claimDates(c, accountId, month)) out.add(d.getDayOfMonth());
        } catch (SQLException e) {
            // no dailygift_log: no streak (the bonus needs the dated claims)
        }
        return out;
    }

    static List<Integer> given(Connection c, int accountId, YearMonth month) throws SQLException {
        List<Integer> out = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement("SELECT days FROM daily_streak_bonus WHERE account_id = ? AND month = ? ORDER BY days")) {
            ps.setInt(1, accountId);
            ps.setString(2, month.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(rs.getInt(1));
            }
        }
        return out;
    }

    public static Status status(Connection c, int accountId) throws SQLException {
        ensure();
        LocalDate today = today();
        YearMonth month = YearMonth.from(today);
        TreeSet<Integer> days = claimDays(c, accountId, month);
        boolean claimed = days.contains(today.getDayOfMonth());
        return new Status(streak(days, claimed ? today.getDayOfMonth() : today.getDayOfMonth() - 1), claimed, given(c, accountId, month), bonuses());
    }

    /** After today's claim (today counts even if the log row was not written): queue the bonuses this streak reaches. */
    public static List<Granted> afterClaim(int userId, int accountId, int characterId) throws SQLException {
        ensure();
        GiftSchema.ensure();
        List<Bonus> cfg = bonuses();
        if (cfg.isEmpty()) return List.of();
        LocalDate today = today();
        YearMonth month = YearMonth.from(today);
        List<Granted> out = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection()) {
            TreeSet<Integer> days = claimDays(c, accountId, month);
            days.add(today.getDayOfMonth());
            int streak = streak(days, today.getDayOfMonth());
            List<Integer> given = given(c, accountId, month);
            c.setAutoCommit(false);
            try {
                for (Bonus b : cfg) {
                    if (streak < b.days() || given.contains(b.days())) continue;
                    try (PreparedStatement ps = c.prepareStatement("INSERT IGNORE INTO daily_streak_bonus (account_id, month, days, day, character_id) VALUES (?,?,?,?,?)")) {
                        ps.setInt(1, accountId);
                        ps.setString(2, month.toString());
                        ps.setInt(3, b.days());
                        ps.setDate(4, Date.valueOf(today));
                        ps.setInt(5, characterId);
                        if (ps.executeUpdate() != 1) continue; // given by a concurrent claim
                    }
                    for (Item i : b.items()) {
                        DailyGiftLauncherClaim.queue(c, userId, characterId, i.itemId(), i.quantity(), 0, "streak", b.days());
                    }
                    out.add(new Granted(b.days(), b.items()));
                }
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
        if (!out.isEmpty()) {
            System.out.printf("[StreakBonus] account %d streak bonus %s -> character %d%n", accountId, out.stream().map(Granted::days).toList(), characterId);
        }
        return out;
    }
}
