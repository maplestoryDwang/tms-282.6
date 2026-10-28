package net.swordie.ms.connection.api.launcher.gift;

import net.swordie.ms.connection.api.launcher.ServerMemory;
import net.swordie.ms.connection.api.launcher.account.CharacterList;
import net.swordie.ms.connection.api.launcher.account.CharacterReader;
import net.swordie.ms.connection.api.launcher.account.CharacterView;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.constants.ItemConstants;
import net.swordie.ms.loaders.ItemData;
import net.swordie.ms.loaders.StringData;
import net.swordie.ms.world.tms282.items.Tms282ItemAvailability;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.LongSupplier;

/**
 * Gift codes (P7): creating (admin console), redeeming (launcher), listing / disabling / history / queue.
 * <p>
 * <b>Redeem</b> is one InnoDB transaction: the code row is locked ({@code SELECT ... FOR UPDATE} by its HMAC), so
 * every redemption of one code is serialised; then the period, enabled flag, key, world and level are checked, the
 * account's uses counted, a redemption row inserted (UNIQUE(code_id, user_id, seq) as a backstop), used_count raised
 * with a guard ({@code used_count < max_uses}), one launcher_reward_queue row per item written, commit. A duplicate
 * request, a concurrent one or a crash in between can therefore never give more than max_uses / per_user_limit.
 * <p>
 * Answers: {@code alreadyRedeemed} only when this account has used up its own uses of an otherwise valid code;
 * every other refusal about the code is {@code invalidOrExpired} (no enumeration of codes, limits or periods).
 * Failed guesses are limited per account ({@link #MAX_FAILURES} per hour) on top of the per-IP bucket.
 */
public final class GiftCodeService {
    public static final int MAX_ITEMS = 10;
    public static final int MAX_FAILURES = 10;
    public static final long FAILURE_WINDOW_MS = 3_600_000L;
    private static final GiftCodeService STANDARD = new GiftCodeService(System::currentTimeMillis);

    private final LongSupplier clock;
    private final Map<Integer, Deque<Long>> failures = new HashMap<>();

    public GiftCodeService(LongSupplier clock) {
        this.clock = clock;
    }

    public static GiftCodeService standard() {
        return STANDARD;
    }

    public record Item(int itemId, int quantity) {
        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("itemId", itemId);
            m.put("name", itemName(itemId));
            m.put("quantity", quantity);
            return m;
        }
    }

    public static String itemName(int itemId) {
        try {
            String n = StringData.getItemStringById(itemId);
            return n == null || n.isBlank() ? "?" : n;
        } catch (RuntimeException e) {
            return "?";
        }
    }

    /** Null when {@code item} may be put into a code, otherwise the problem key. */
    public static String itemProblem(int itemId, int quantity) {
        if (itemId <= 0 || (ItemData.getItemInfoByID(itemId) == null && ItemData.getItemDeepCopy(itemId) == null)) {
            return "itemId";
        }
        if (!Tms282ItemAvailability.isGrantable(itemId)) {
            return "itemNotGrantable";
        }
        boolean unique = ItemConstants.isEquip(itemId) || ItemConstants.isPet(itemId);
        if (quantity < 1 || quantity > (unique ? 1 : 1000)) {
            return "quantity";
        }
        return null;
    }

    // ------------------------------------------------------------------ create

    public record Spec(String title, String note, List<Item> items, int maxUses, int perUserLimit, int worldId, int minLevel,
                       Long startsAt, Long expiresAt, int count) {
    }

    public record Created(List<Integer> ids, List<String> codes, String batchId) {
        @Override
        public String toString() {
            return "Created[ids=" + ids + ", batch=" + batchId + "]"; // never the codes
        }
    }

    /** Null when {@code s} is acceptable, otherwise the problem key. */
    public static String validate(Spec s, long now) {
        if (s.title() == null || s.title().isBlank() || s.title().strip().length() > 64) return "title";
        if (s.note() != null && s.note().length() > 200) return "note";
        if (s.items() == null || s.items().isEmpty() || s.items().size() > MAX_ITEMS) return "items";
        for (Item i : s.items()) {
            String p = itemProblem(i.itemId(), i.quantity());
            if (p != null) return p;
        }
        if (s.maxUses() < 1 || s.maxUses() > 100_000) return "maxUses";
        if (s.perUserLimit() < 1 || s.perUserLimit() > 100 || s.perUserLimit() > s.maxUses()) return "perUserLimit";
        if (s.worldId() < -1 || s.worldId() > 255) return "worldId";
        if (s.minLevel() < 0 || s.minLevel() > 300) return "minLevel";
        if (s.expiresAt() != null && s.expiresAt() <= now) return "expiresAt";
        if (s.startsAt() != null && s.expiresAt() != null && s.startsAt() >= s.expiresAt()) return "startsAt";
        if (s.count() < 1 || s.count() > 200) return "count";
        return null;
    }

    public Created create(Spec s, GiftCodeKey key) throws SQLException {
        String problem = validate(s, clock.getAsLong());
        if (problem != null) throw new IllegalArgumentException(problem);
        GiftSchema.ensure();
        String batch = Long.toString(clock.getAsLong(), 36) + "-" + GiftCodeFormat.newCode().substring(0, 4);
        List<Integer> ids = new ArrayList<>();
        List<String> codes = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection()) {
            c.setAutoCommit(false);
            try {
                for (int n = 0; n < s.count(); n++) {
                    String code;
                    int id;
                    int attempts = 0;
                    while (true) {
                        code = GiftCodeFormat.newCode();
                        try (PreparedStatement ps = c.prepareStatement("INSERT INTO gift_codes (code_hash, key_id, title, note, batch_id, max_uses, "
                                + "per_user_limit, world_id, min_level, starts_at, expires_at, enabled, created_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,1,?)",
                                Statement.RETURN_GENERATED_KEYS)) {
                            ps.setBytes(1, key.hash(code));
                            ps.setString(2, key.keyId());
                            ps.setString(3, s.title().strip());
                            ps.setString(4, s.note() == null ? "" : s.note().strip());
                            ps.setString(5, batch);
                            ps.setInt(6, s.maxUses());
                            ps.setInt(7, s.perUserLimit());
                            ps.setInt(8, s.worldId());
                            ps.setInt(9, s.minLevel());
                            ps.setTimestamp(10, ts(s.startsAt()));
                            ps.setTimestamp(11, ts(s.expiresAt()));
                            ps.setTimestamp(12, new Timestamp(clock.getAsLong()));
                            ps.executeUpdate();
                            try (ResultSet k = ps.getGeneratedKeys()) {
                                k.next();
                                id = k.getInt(1);
                            }
                            break;
                        } catch (SQLException e) {
                            if (e.getErrorCode() == 1062 && ++attempts < 5) continue; // 80-bit collision: draw again
                            throw e;
                        }
                    }
                    int slot = 0;
                    for (Item i : s.items()) {
                        try (PreparedStatement ps = c.prepareStatement("INSERT INTO gift_code_items (code_id, slot, item_id, quantity) VALUES (?,?,?,?)")) {
                            ps.setInt(1, id);
                            ps.setInt(2, slot++);
                            ps.setInt(3, i.itemId());
                            ps.setInt(4, i.quantity());
                            ps.executeUpdate();
                        }
                    }
                    ids.add(id);
                    codes.add(GiftCodeFormat.display(code));
                }
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
        System.out.printf("[GiftCode] created %d code(s) batch %s ids %s title \"%s\" maxUses %d perUser %d world %d minLevel %d%n",
                ids.size(), batch, ids, s.title().strip(), s.maxUses(), s.perUserLimit(), s.worldId(), s.minLevel());
        return new Created(List.copyOf(ids), List.copyOf(codes), batch);
    }

    // ------------------------------------------------------------------ redeem

    public enum Status {REDEEMED, INVALID_OR_EXPIRED, ALREADY_REDEEMED, NO_RECEIVER, RATE_LIMITED}

    public record Redemption(Status status, long redemptionId, String title, List<Item> items, CharacterView receiver, long retryAfterMs) {
    }

    /**
     * Redeems {@code input} for the session's account {@code userId} onto its character {@code characterId}.
     * {@code memory} supplies the live level of an online character.
     */
    public Redemption redeem(int userId, int characterId, String input, String sourceIp, GiftCodeKey key, ServerMemory memory)
            throws SQLException {
        long retry = retryAfter(userId);
        if (retry > 0) return new Redemption(Status.RATE_LIMITED, 0, null, List.of(), null, retry);
        GiftSchema.ensure();
        CharacterView receiver;
        try (Connection c = DatabaseManager.getConnection()) {
            CharacterList list = new CharacterReader().read(c, userId, memory);
            receiver = list.characters().stream().filter(v -> v.id() == characterId && !v.deleting()).findFirst().orElse(null);
        }
        if (receiver == null) return new Redemption(Status.NO_RECEIVER, 0, null, List.of(), null, 0);
        String code = GiftCodeFormat.normalize(input);
        if (code == null) return fail(userId);
        byte[] hash = key.hash(code);
        try (Connection c = DatabaseManager.getConnection()) {
            c.setAutoCommit(false);
            try {
                int codeId, maxUses, used, perUser, world, minLevel;
                String title, keyId;
                boolean enabled;
                Timestamp starts, expires;
                try (PreparedStatement ps = c.prepareStatement("SELECT id, key_id, title, max_uses, used_count, per_user_limit, world_id, min_level, "
                        + "starts_at, expires_at, enabled FROM gift_codes WHERE code_hash = ? FOR UPDATE")) {
                    ps.setBytes(1, hash);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            c.rollback();
                            return fail(userId);
                        }
                        codeId = rs.getInt("id");
                        keyId = rs.getString("key_id");
                        title = rs.getString("title");
                        maxUses = rs.getInt("max_uses");
                        used = rs.getInt("used_count");
                        perUser = rs.getInt("per_user_limit");
                        world = rs.getInt("world_id");
                        minLevel = rs.getInt("min_level");
                        starts = rs.getTimestamp("starts_at");
                        expires = rs.getTimestamp("expires_at");
                        enabled = rs.getBoolean("enabled");
                    }
                }
                long now = clock.getAsLong();
                if (!enabled || !key.keyId().equals(keyId) || (starts != null && starts.getTime() > now)
                        || (expires != null && expires.getTime() <= now)) {
                    c.rollback();
                    return fail(userId);
                }
                int mine;
                try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM gift_code_redemptions WHERE code_id = ? AND user_id = ?")) {
                    ps.setInt(1, codeId);
                    ps.setInt(2, userId);
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        mine = rs.getInt(1);
                    }
                }
                if (mine >= perUser) {
                    c.rollback();
                    // not a guess: this account knows the code already
                    return new Redemption(Status.ALREADY_REDEEMED, 0, title, List.of(), receiver, 0);
                }
                if (used >= maxUses || (world >= 0 && receiver.world() != world) || receiver.level() < minLevel) {
                    c.rollback();
                    return fail(userId);
                }
                List<Item> items = new ArrayList<>();
                try (PreparedStatement ps = c.prepareStatement("SELECT item_id, quantity FROM gift_code_items WHERE code_id = ? ORDER BY slot")) {
                    ps.setInt(1, codeId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) items.add(new Item(rs.getInt(1), rs.getInt(2)));
                    }
                }
                if (items.isEmpty()) {
                    c.rollback();
                    return fail(userId);
                }
                long redemptionId;
                try (PreparedStatement ps = c.prepareStatement("INSERT INTO gift_code_redemptions (code_id, user_id, seq, character_id, world_id, "
                        + "source_ip, redeemed_at) VALUES (?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, codeId);
                    ps.setInt(2, userId);
                    ps.setInt(3, mine + 1);
                    ps.setInt(4, receiver.id());
                    ps.setInt(5, receiver.world());
                    ps.setString(6, sourceIp == null ? "" : sourceIp.length() > 45 ? sourceIp.substring(0, 45) : sourceIp);
                    ps.setTimestamp(7, new Timestamp(now));
                    ps.executeUpdate();
                    try (ResultSet k = ps.getGeneratedKeys()) {
                        k.next();
                        redemptionId = k.getLong(1);
                    }
                }
                try (PreparedStatement ps = c.prepareStatement("UPDATE gift_codes SET used_count = used_count + 1 WHERE id = ? AND used_count < max_uses AND enabled = 1")) {
                    ps.setInt(1, codeId);
                    if (ps.executeUpdate() != 1) {
                        c.rollback();
                        return fail(userId);
                    }
                }
                for (Item i : items) {
                    try (PreparedStatement ps = c.prepareStatement("INSERT INTO launcher_reward_queue (user_id, character_id, item_id, quantity, source, "
                            + "source_id, created_at) VALUES (?,?,?,?,'gift',?,?)")) {
                        ps.setInt(1, userId);
                        ps.setInt(2, receiver.id());
                        ps.setInt(3, i.itemId());
                        ps.setInt(4, i.quantity());
                        ps.setLong(5, redemptionId);
                        ps.setTimestamp(6, new Timestamp(now));
                        ps.executeUpdate();
                    }
                }
                c.commit();
                System.out.printf("[GiftCode] redeemed code %d (\"%s\") by account %d for character %d, redemption %d, %d item line(s) queued%n",
                        codeId, title, userId, receiver.id(), redemptionId, items.size());
                return new Redemption(Status.REDEEMED, redemptionId, title, List.copyOf(items), receiver, 0);
            } catch (SQLException e) {
                c.rollback();
                if (e.getErrorCode() == 1062) {
                    // UNIQUE(code_id, user_id, seq): a parallel request of the same account won
                    return new Redemption(Status.ALREADY_REDEEMED, 0, null, List.of(), receiver, 0);
                }
                throw e;
            } catch (RuntimeException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    private Redemption fail(int userId) {
        synchronized (failures) {
            failures.computeIfAbsent(userId, k -> new ArrayDeque<>()).addLast(clock.getAsLong());
        }
        return new Redemption(Status.INVALID_OR_EXPIRED, 0, null, List.of(), null, 0);
    }

    /** > 0 while the account has {@link #MAX_FAILURES} failed redemptions within the last hour. */
    long retryAfter(int userId) {
        long now = clock.getAsLong();
        synchronized (failures) {
            Deque<Long> d = failures.get(userId);
            if (d == null) return 0;
            while (!d.isEmpty() && now - d.peekFirst() >= FAILURE_WINDOW_MS) d.pollFirst();
            if (d.isEmpty()) {
                failures.remove(userId);
                return 0;
            }
            return d.size() >= MAX_FAILURES ? FAILURE_WINDOW_MS - (now - d.peekFirst()) : 0;
        }
    }

    /** Admin console (account unlock) / tests. */
    public void clearFailures(int userId) {
        synchronized (failures) {
            failures.remove(userId);
        }
    }

    // ------------------------------------------------------------------ history / admin views

    /** The account's latest redemptions (never the code): when, title, character, items, delivered. */
    public List<Map<String, Object>> history(Connection c, int userId, int limit) throws SQLException {
        if (!tablesExist(c)) return List.of();
        List<Map<String, Object>> out = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement("SELECT r.id, r.redeemed_at, r.character_id, g.title, cs.name AS chr_name "
                + "FROM gift_code_redemptions r JOIN gift_codes g ON g.id = r.code_id "
                + "LEFT JOIN characters ch ON ch.id = r.character_id LEFT JOIN avatardata ad ON ad.id = ch.avatardata LEFT JOIN characterstats cs ON cs.id = ad.characterstat "
                + "WHERE r.user_id = ? ORDER BY r.id DESC LIMIT ?")) {
            ps.setInt(1, userId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("redeemedAt", rs.getTimestamp("redeemed_at").getTime());
                    m.put("title", rs.getString("title"));
                    m.put("characterId", rs.getInt("character_id"));
                    m.put("characterName", rs.getString("chr_name") == null ? "" : rs.getString("chr_name"));
                    m.put("redemptionId", rs.getLong("id"));
                    out.add(m);
                }
            }
        }
        for (Map<String, Object> m : out) {
            long rid = (Long) m.remove("redemptionId");
            List<Map<String, Object>> items = new ArrayList<>();
            boolean delivered = true;
            try (PreparedStatement ps = c.prepareStatement("SELECT item_id, quantity, delivered_at FROM launcher_reward_queue WHERE source = 'gift' AND source_id = ? ORDER BY id")) {
                ps.setLong(1, rid);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        items.add(new Item(rs.getInt(1), rs.getInt(2)).toMap());
                        if (rs.getTimestamp(3) == null) delivered = false;
                    }
                }
            }
            m.put("items", items);
            m.put("delivered", delivered);
        }
        return out;
    }

    public static boolean tablesExist(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME IN "
                + "('gift_codes', 'gift_code_items', 'gift_code_redemptions', 'launcher_reward_queue')")) {
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) == 4;
            }
        }
    }

    public static List<Item> items(Connection c, int codeId) throws SQLException {
        List<Item> items = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement("SELECT item_id, quantity FROM gift_code_items WHERE code_id = ? ORDER BY slot")) {
            ps.setInt(1, codeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) items.add(new Item(rs.getInt(1), rs.getInt(2)));
            }
        }
        return items;
    }

    /** Admin list (newest first). {@code state}: active, disabled, scheduled, expired, exhausted, otherKey. */
    public List<Map<String, Object>> list(Connection c, GiftCodeKey key, int limit, boolean includeDisabled) throws SQLException {
        List<Map<String, Object>> out = new ArrayList<>();
        long now = clock.getAsLong();
        try (PreparedStatement ps = c.prepareStatement("SELECT * FROM gift_codes " + (includeDisabled ? "" : "WHERE enabled = 1 ")
                + "ORDER BY id DESC LIMIT ?")) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", rs.getInt("id"));
                    m.put("title", rs.getString("title"));
                    m.put("note", rs.getString("note"));
                    m.put("batchId", rs.getString("batch_id"));
                    m.put("maxUses", rs.getInt("max_uses"));
                    m.put("usedCount", rs.getInt("used_count"));
                    m.put("perUserLimit", rs.getInt("per_user_limit"));
                    m.put("worldId", rs.getInt("world_id"));
                    m.put("minLevel", rs.getInt("min_level"));
                    Timestamp st = rs.getTimestamp("starts_at"), ex = rs.getTimestamp("expires_at");
                    m.put("startsAt", st == null ? null : st.getTime());
                    m.put("expiresAt", ex == null ? null : ex.getTime());
                    m.put("enabled", rs.getBoolean("enabled"));
                    m.put("createdAt", rs.getTimestamp("created_at").getTime());
                    Timestamp dis = rs.getTimestamp("disabled_at");
                    m.put("disabledAt", dis == null ? null : dis.getTime());
                    boolean keyOk = key.keyId().equals(rs.getString("key_id"));
                    String state = !rs.getBoolean("enabled") ? "disabled" : !keyOk ? "otherKey"
                            : st != null && st.getTime() > now ? "scheduled" : ex != null && ex.getTime() <= now ? "expired"
                            : rs.getInt("used_count") >= rs.getInt("max_uses") ? "exhausted" : "active";
                    m.put("state", state);
                    out.add(m);
                }
            }
        }
        for (Map<String, Object> m : out) {
            m.put("items", items(c, (Integer) m.get("id")).stream().map(Item::toMap).toList());
        }
        return out;
    }

    /** Disables a code (redemptions already queued are still delivered). False when there is no such enabled code. */
    public boolean disable(Connection c, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE gift_codes SET enabled = 0, disabled_at = ? WHERE id = ? AND enabled = 1")) {
            ps.setTimestamp(1, new Timestamp(clock.getAsLong()));
            ps.setInt(2, id);
            boolean done = ps.executeUpdate() == 1;
            if (done) System.out.printf("[GiftCode] code %d disabled%n", id);
            return done;
        }
    }

    public static Optional<Integer> codeExists(Connection c, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT id FROM gift_codes WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(rs.getInt(1)) : Optional.empty();
            }
        }
    }

    /** Redemptions, newest first; optionally of one code. */
    public List<Map<String, Object>> redemptions(Connection c, Integer codeId, int limit) throws SQLException {
        List<Map<String, Object>> out = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement("SELECT r.*, g.title, u.name AS user_name, cs.name AS chr_name, "
                + "(SELECT COUNT(*) FROM launcher_reward_queue q WHERE q.source = 'gift' AND q.source_id = r.id AND q.delivered_at IS NULL) AS pending "
                + "FROM gift_code_redemptions r JOIN gift_codes g ON g.id = r.code_id LEFT JOIN users u ON u.id = r.user_id "
                + "LEFT JOIN characters ch ON ch.id = r.character_id LEFT JOIN avatardata ad ON ad.id = ch.avatardata LEFT JOIN characterstats cs ON cs.id = ad.characterstat "
                + (codeId == null ? "" : "WHERE r.code_id = ? ") + "ORDER BY r.id DESC LIMIT ?")) {
            int i = 1;
            if (codeId != null) ps.setInt(i++, codeId);
            ps.setInt(i, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", rs.getLong("id"));
                    m.put("codeId", rs.getInt("code_id"));
                    m.put("title", rs.getString("title"));
                    m.put("userId", rs.getInt("user_id"));
                    m.put("userName", rs.getString("user_name") == null ? "" : rs.getString("user_name"));
                    m.put("characterId", rs.getInt("character_id"));
                    m.put("characterName", rs.getString("chr_name") == null ? "" : rs.getString("chr_name"));
                    m.put("worldId", rs.getInt("world_id"));
                    m.put("sourceIp", rs.getString("source_ip"));
                    m.put("redeemedAt", rs.getTimestamp("redeemed_at").getTime());
                    m.put("delivered", rs.getInt("pending") == 0);
                    out.add(m);
                }
            }
        }
        return out;
    }

    /** The reward queue, oldest pending first (or the latest delivered too). */
    public List<Map<String, Object>> queue(Connection c, boolean includeDelivered, int limit) throws SQLException {
        List<Map<String, Object>> out = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement("SELECT q.*, u.name AS user_name, cs.name AS chr_name FROM launcher_reward_queue q "
                + "LEFT JOIN users u ON u.id = q.user_id LEFT JOIN characters ch ON ch.id = q.character_id LEFT JOIN avatardata ad ON ad.id = ch.avatardata LEFT JOIN characterstats cs ON cs.id = ad.characterstat "
                + (includeDelivered ? "ORDER BY q.delivered_at IS NULL DESC, q.id DESC " : "WHERE q.delivered_at IS NULL ORDER BY q.id ") + "LIMIT ?")) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", rs.getLong("id"));
                    m.put("userId", rs.getInt("user_id"));
                    m.put("userName", rs.getString("user_name") == null ? "" : rs.getString("user_name"));
                    m.put("characterId", rs.getInt("character_id"));
                    m.put("characterName", rs.getString("chr_name") == null ? "" : rs.getString("chr_name"));
                    m.put("itemId", rs.getInt("item_id"));
                    m.put("itemName", itemName(rs.getInt("item_id")));
                    m.put("quantity", rs.getInt("quantity"));
                    m.put("source", rs.getString("source"));
                    m.put("sourceId", rs.getLong("source_id"));
                    m.put("createdAt", rs.getTimestamp("created_at").getTime());
                    Timestamp d = rs.getTimestamp("delivered_at");
                    m.put("deliveredAt", d == null ? null : d.getTime());
                    out.add(m);
                }
            }
        }
        return out;
    }

    private static Timestamp ts(Long epochMs) {
        return epochMs == null ? null : Timestamp.from(Instant.ofEpochMilli(epochMs));
    }

    /** For the admin action: the working directory's key, or the IOException text. */
    public static GiftCodeKey key() throws IOException {
        return GiftCodeKey.standard();
    }
}
