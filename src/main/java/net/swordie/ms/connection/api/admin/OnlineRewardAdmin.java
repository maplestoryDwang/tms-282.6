package net.swordie.ms.connection.api.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.swordie.ms.ServerConfig;
import net.swordie.ms.connection.api.launcher.LauncherResult;
import net.swordie.ms.connection.api.launcher.OnlineAccounts;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.world.online.OnlineRewardConfig;
import net.swordie.ms.world.online.OnlineRewardSettings;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 累積上線獎勵 admin actions (fixes10):
 * <ul>
 *     <li>{@code onlinereward.get} (read-only): the settings in use (data\tms282-online-reward\online-reward.json, hot
 *     reloaded; no file = the built-in defaults), the file's state, one day's per-account minutes and points
 *     (online_reward_day + users.name), totals, and the newest receipts (online_reward_log).</li>
 *     <li>{@code onlinereward.save} (audited): new settings written atomically (temp file in the same folder, then
 *     ATOMIC_MOVE; the old file kept as {@code .bak}); refused when the values are out of range
 *     ({@link OnlineRewardConfig}) or the file changed since {@code baseVersion} (conflict). The server's hot reload
 *     picks it up at once ({@link OnlineRewardSettings#invalidate()}).</li>
 * </ul>
 */
final class OnlineRewardAdmin {
    static final Path FILE = OnlineRewardSettings.DEFAULT_FILE;
    static final int MAX_ROWS = 500;
    private static final ObjectMapper JSON = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private static final Map<Path, OnlineRewardSettings> OTHER_ROOTS = new ConcurrentHashMap<>();

    private OnlineRewardAdmin() {
    }

    /** The server's own settings object when the console's root is the working directory (always on a server). */
    static OnlineRewardSettings settings(AdminContext ctx) {
        Path file = ctx.env().resolve(FILE.toString()).toAbsolutePath().normalize();
        OnlineRewardSettings standard = OnlineRewardSettings.standard();
        if (standard.file().toAbsolutePath().normalize().equals(file)) {
            return standard;
        }
        return OTHER_ROOTS.computeIfAbsent(file, f -> new OnlineRewardSettings(f, System::currentTimeMillis));
    }

    static Map<String, Object> config(OnlineRewardConfig c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("enabled", c.enabled());
        m.put("intervalMinutes", c.intervalMinutes());
        m.put("pointsPerInterval", c.pointsPerInterval());
        m.put("dailyCapPoints", c.dailyCapPoints());
        m.put("afkMinutes", c.afkMinutes());
        m.put("notice", c.notice());
        return m;
    }

    /** SHA-256 of the file's bytes, "missing" when there is none (the save's conflict check). */
    static String version(Path file) throws IOException {
        try {
            byte[] b = Files.readAllBytes(file);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b));
        } catch (NoSuchFileException e) {
            return "missing";
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    static Map<String, Object> state(OnlineRewardSettings s) throws IOException {
        Map<String, Object> m = new LinkedHashMap<>();
        OnlineRewardConfig c = s.get();
        String problem = s.problem();
        boolean exists = Files.isRegularFile(s.file());
        m.put("config", config(c));
        m.put("defaults", config(OnlineRewardConfig.defaults()));
        m.put("state", !exists ? "default" : problem.isEmpty() ? "file" : "invalid");
        m.put("problem", problem);
        m.put("file", s.file().toAbsolutePath().normalize().toString());
        m.put("version", version(s.file()));
        return m;
    }

    // ------------------------------------------------------------------ onlinereward.get

    static final class Get implements AdminAction {
        @Override
        public String name() {
            return "onlinereward.get";
        }

        @Override
        public boolean mutating() {
            return false;
        }

        @Override
        public LauncherResult execute(AdminContext ctx, JsonNode request) throws Exception {
            LocalDate today = LocalDate.now(ServerConfig.ZONE);
            LocalDate day = today;
            String d = AdminParams.text(request, "day").strip();
            if (!d.isEmpty()) {
                try {
                    day = LocalDate.parse(d);
                } catch (DateTimeParseException e) {
                    return LauncherResult.fail(AdminCodes.INVALID_INPUT).with("problem", "onlinereward.day");
                }
            }
            String query = AdminParams.text(request, "query").strip();
            if (query.length() > 32) {
                return LauncherResult.fail(AdminCodes.INVALID_INPUT).with("problem", "onlinereward.query");
            }
            Long limit = AdminParams.number(request, "limit", 1, MAX_ROWS);
            Long logLimit = AdminParams.number(request, "logLimit", 1, MAX_ROWS);
            LauncherResult r = LauncherResult.ok(AdminCodes.OK);
            state(settings(ctx)).forEach(r::with);
            r.with("day", day.toString()).with("today", today.toString()).with("zone", ServerConfig.ZONE.getId());
            List<Map<String, Object>> accounts = new ArrayList<>();
            List<Map<String, Object>> log = new ArrayList<>();
            long[] totals = new long[3];
            boolean tables = true;
            try (Connection c = DatabaseManager.getConnection()) {
                accounts(c, day, query, limit == null ? 200 : limit.intValue(), accounts, totals);
                log(c, query, logLimit == null ? 100 : logLimit.intValue(), log);
            } catch (SQLException e) {
                if (e.getErrorCode() != 1146) {
                    throw e;
                }
                tables = false; // the server never ticked: the tables are created on first use
                accounts.clear();
                log.clear();
            }
            Map<String, Object> t = new LinkedHashMap<>();
            t.put("accounts", totals[0]);
            t.put("minutes", totals[1]);
            t.put("points", totals[2]);
            return r.with("tables", tables).with("totals", t).with("accounts", accounts).with("log", log);
        }

        /** One day's rows (points, then minutes, highest first); totals over the whole day, not only the shown rows. */
        static void accounts(Connection c, LocalDate day, String query, int limit, List<Map<String, Object>> out, long[] totals) throws SQLException {
            try (PreparedStatement p = c.prepareStatement("SELECT COUNT(*), COALESCE(SUM(minutes),0), COALESCE(SUM(points),0) FROM online_reward_day WHERE day = ?")) {
                p.setDate(1, Date.valueOf(day));
                try (ResultSet rs = p.executeQuery()) {
                    if (rs.next()) {
                        totals[0] = rs.getLong(1);
                        totals[1] = rs.getLong(2);
                        totals[2] = rs.getLong(3);
                    }
                }
            }
            String filter = filter(query);
            try (PreparedStatement p = c.prepareStatement("SELECT d.user_id, u.name, d.minutes, d.points, d.updated_at FROM online_reward_day d "
                    + "LEFT JOIN users u ON u.id = d.user_id WHERE d.day = ?" + filter + " ORDER BY d.points DESC, d.minutes DESC, d.user_id LIMIT ?")) {
                int i = 1;
                p.setDate(i++, Date.valueOf(day));
                i = bind(p, i, query);
                p.setInt(i, limit);
                OnlineAccounts online = OnlineAccounts.live();
                try (ResultSet rs = p.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> m = new LinkedHashMap<>();
                        int userId = rs.getInt(1);
                        m.put("userId", userId);
                        m.put("name", rs.getString(2) == null ? "" : rs.getString(2));
                        m.put("minutes", rs.getInt(3));
                        m.put("points", rs.getInt(4));
                        m.put("updatedAt", ms(rs.getTimestamp(5)));
                        boolean on;
                        try {
                            on = online.online(userId);
                        } catch (RuntimeException e) {
                            on = false;
                        }
                        m.put("online", on);
                        out.add(m);
                    }
                }
            }
        }

        static void log(Connection c, String query, int limit, List<Map<String, Object>> out) throws SQLException {
            try (PreparedStatement p = c.prepareStatement("SELECT l.id, l.user_id, u.name, l.day, l.amount, l.points_after, l.minutes, l.balance_after, l.granted_at "
                    + "FROM online_reward_log l LEFT JOIN users u ON u.id = l.user_id WHERE 1=1" + filter(query).replace("d.user_id", "l.user_id")
                    + " ORDER BY l.id DESC LIMIT ?")) {
                int i = bind(p, 1, query);
                p.setInt(i, limit);
                try (ResultSet rs = p.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("id", rs.getLong(1));
                        m.put("userId", rs.getInt(2));
                        m.put("name", rs.getString(3) == null ? "" : rs.getString(3));
                        m.put("day", rs.getDate(4).toLocalDate().toString());
                        m.put("amount", rs.getInt(5));
                        m.put("pointsAfter", rs.getInt(6));
                        m.put("minutes", rs.getInt(7));
                        m.put("balanceAfter", rs.getLong(8));
                        m.put("grantedAt", ms(rs.getTimestamp(9)));
                        out.add(m);
                    }
                }
            }
        }

        /** "" = everyone; digits = that user id or a name containing them; otherwise a name containing the text. */
        static String filter(String query) {
            if (query.isEmpty()) {
                return "";
            }
            return query.matches("\\d{1,10}") ? " AND (d.user_id = ? OR u.name LIKE ?)" : " AND u.name LIKE ?";
        }

        static int bind(PreparedStatement p, int i, String query) throws SQLException {
            if (query.isEmpty()) {
                return i;
            }
            String like = "%" + query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
            if (query.matches("\\d{1,10}")) {
                p.setLong(i++, Long.parseLong(query));
            }
            p.setString(i++, like);
            return i;
        }

        static long ms(Timestamp t) {
            return t == null ? 0 : t.getTime();
        }
    }

    // ------------------------------------------------------------------ onlinereward.save

    static final class Save implements AdminAction {
        @Override
        public String name() {
            return "onlinereward.save";
        }

        @Override
        public boolean mutating() {
            return true;
        }

        @Override
        public String auditSummary(JsonNode request) {
            JsonNode c = request == null ? null : request.get("config");
            if (c == null || !c.isObject()) {
                return "online reward settings";
            }
            return "online reward enabled=" + c.path("enabled").asText() + " interval=" + c.path("intervalMinutes").asText() + " points="
                    + c.path("pointsPerInterval").asText() + " cap=" + c.path("dailyCapPoints").asText() + " afk=" + c.path("afkMinutes").asText();
        }

        @Override
        public LauncherResult execute(AdminContext ctx, JsonNode request) throws Exception {
            JsonNode in = request == null ? null : request.get("config");
            if (in == null || !in.isObject()) {
                return LauncherResult.fail(AdminCodes.INVALID_INPUT).with("problem", "onlinereward.config");
            }
            OnlineRewardConfig next;
            try {
                next = OnlineRewardConfig.parse(in);
            } catch (IllegalArgumentException e) {
                return LauncherResult.fail(AdminCodes.REJECTED).with("problem", e.getMessage());
            }
            OnlineRewardSettings s = settings(ctx);
            Path file = s.file().toAbsolutePath().normalize();
            synchronized (OnlineRewardAdmin.class) {
                String base = AdminParams.text(request, "baseVersion");
                if (!version(file).equals(base)) {
                    return LauncherResult.fail(AdminCodes.CONFLICT).with("version", version(file));
                }
                // keep keys the host added by hand (comments); replace the six settings
                ObjectNode out = JSON.createObjectNode();
                try {
                    String text = Files.readString(file, StandardCharsets.UTF_8);
                    if (!text.isEmpty() && text.charAt(0) == '﻿') {
                        text = text.substring(1);
                    }
                    JsonNode old = JSON.readTree(text);
                    if (old != null && old.isObject()) {
                        out.setAll((ObjectNode) old);
                    }
                } catch (NoSuchFileException e) {
                    // first save: a new file
                } catch (IOException e) {
                    // a broken file is replaced as a whole (it was not in use anyway)
                }
                config(next).forEach((k, v) -> out.set(k, JSON.valueToTree(v)));
                byte[] bytes = (JSON.writeValueAsString(out) + "\n").getBytes(StandardCharsets.UTF_8);
                Path parent = file.getParent();
                Files.createDirectories(parent);
                Path tmp = Files.createTempFile(parent, "online-reward-", ".tmp");
                try {
                    Files.write(tmp, bytes);
                    if (Files.isRegularFile(file)) {
                        Files.copy(file, file.resolveSibling(file.getFileName() + ".bak"), StandardCopyOption.REPLACE_EXISTING);
                    }
                    Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } finally {
                    Files.deleteIfExists(tmp);
                }
            }
            s.invalidate();
            LauncherResult r = LauncherResult.ok(AdminCodes.OK);
            state(s).forEach(r::with);
            System.out.println("[OnlineReward] settings saved from the admin console: " + config(next));
            return r;
        }
    }
}
