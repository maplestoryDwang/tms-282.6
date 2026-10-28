package net.swordie.ms.connection.api.launcher.daily;

import net.swordie.ms.client.daily.DailyGiftItemInfo;
import net.swordie.ms.constants.DailyGiftConstants;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * The 28 daily gift rewards as the launcher shows them: {@code data/protocol/tms282.3/daily-gift.tsv} (relative to the
 * working directory, {@code D:\TMS\server\265} for the server and the CLI alike), the file the TMS 282 game reads
 * through {@link DailyGiftConstants#rewards()}. Read here directly because the CLI runs without
 * {@code -Dprotocol.version}, where {@code rewards()} would answer the GMS table; the same parsing rules as
 * {@code DailyGiftConstants.parse}, plus the {@code name_282} column, and the expiry the game grants
 * ({@link DailyGiftConstants#periodDays}). Shared by the process, polled for changes every two seconds; admin saves
 * publish immediately. API views capture {@link #snapshot()} once for matching rows/version. A failed reload keeps
 * the last valid table. In the server process it is compared once with the game's ({@link #checkAgainstGameOnce}).
 */
public final class DailyRewardTable {
    public static final Path DEFAULT_FILE = Path.of("data", "protocol", "tms282.3", "daily-gift.tsv");
    public static final int DAYS = 28;

    /** One day's reward; {@code periodDays} 0 = permanent. */
    public record Reward(int day, int itemId, int count, int periodDays, String name) {
        /** Wire order: day, itemId, count, periodDays, name. */
        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("day", day);
            m.put("itemId", itemId);
            m.put("count", count);
            m.put("periodDays", periodDays);
            m.put("name", name);
            return m;
        }
    }

    /** Rows and version captured together, without holding the catalog monitor across API/database reads. */
    public record Snapshot(List<Reward> rewards, String version) {
        public Snapshot { rewards = List.copyOf(rewards); Objects.requireNonNull(version); }
        public Reward next(int claimed) { return claimed >= 0 && claimed < rewards.size() ? rewards.get(claimed) : null; }
    }

    private static final DailyRewardTable STANDARD = new DailyRewardTable(DEFAULT_FILE);

    private final Path file;
    private volatile Snapshot loaded;
    private final AtomicBoolean checked = new AtomicBoolean();
    private long checkedAt;
    private String fileKey;
    private String problem = "";

    public DailyRewardTable(Path file) {
        this.file = Objects.requireNonNull(file, "file");
    }

    /** The working directory's table, shared by the process. */
    public static DailyRewardTable standard() {
        return STANDARD;
    }

    /** Day 1..28 in order; IllegalStateException when the file cannot be read or is malformed. */
    public List<Reward> rewards() {
        return load().rewards();
    }

    public Snapshot snapshot() { return load(); }

    /** First 16 hex digits of the SHA-256 of the parsed table (changes only when a reward changes). */
    public String version() {
        return load().version();
    }

    /** The reward after {@code claimed} claims (index {@code claimed}), or null when the month is complete. */
    public Reward next(int claimed) {
        List<Reward> r = rewards();
        return claimed >= 0 && claimed < r.size() ? r.get(claimed) : null;
    }

    private synchronized Snapshot load() {
        long now = System.currentTimeMillis();
        if (loaded != null && now - checkedAt < 2000 && now >= checkedAt) return loaded;
        checkedAt = now;
        try {
            var attrs = Files.readAttributes(file, java.nio.file.attribute.BasicFileAttributes.class);
            String key = attrs.lastModifiedTime().toMillis() + ":" + attrs.size();
            if (loaded != null && key.equals(fileKey)) return loaded;
            if (attrs.size() > 128 * 1024) throw new IllegalArgumentException("daily gift file too large");
            List<Reward> rewards = parse(Files.readAllLines(file, StandardCharsets.UTF_8));
            loaded = new Snapshot(rewards, version(rewards));
            fileKey = key;
            problem = "";
        } catch (IOException | RuntimeException e) {
            problem = e.getMessage();
            if (loaded == null) throw new IllegalStateException("daily gift rewards unreadable: " + file, e);
            System.out.println("[DailyRewards] keeping last valid table: " + e.getClass().getSimpleName());
        }
        return loaded;
    }

    public List<DailyGiftItemInfo> gameRewards() {
        return rewards().stream().map(r -> new DailyGiftItemInfo(r.day(), r.itemId(), r.count(), 0, r.periodDays())).toList();
    }

    public synchronized String problem() { load(); return problem; }

    /** Administrator edits use a version check and an atomic file replacement; backup keeps the previous table. */
    public synchronized String save(List<Reward> proposed, String expectedVersion) throws IOException {
        checkedAt = 0;
        if (!version().equals(expectedVersion)) return "conflict";
        StringBuilder text = new StringBuilder("day\titem_id\tcount\tperiod_days\tname_282\n");
        for (Reward r : proposed) text.append(r.day()).append('\t').append(r.itemId()).append('\t').append(r.count())
                .append('\t').append(r.periodDays()).append('\t').append(r.name()).append('\n');
        List<Reward> validated = parse(text.toString().lines().toList());
        Path parent = file.toAbsolutePath().getParent();
        Files.createDirectories(parent);
        Path tmp = Files.createTempFile(parent, "daily-gift-", ".tmp");
        try {
            Files.writeString(tmp, text.toString(), StandardCharsets.UTF_8);
            Files.copy(file, file.resolveSibling(file.getFileName() + ".bak"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            Files.move(tmp, file, java.nio.file.StandardCopyOption.ATOMIC_MOVE, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            loaded = new Snapshot(validated, version(validated));
            fileKey = null;
            checkedAt = 0;
            problem = "";
            return "ok";
        } finally { Files.deleteIfExists(tmp); }
    }

    /** {@code day item_id count period_days name_282}; the header row and blank rows are skipped; exactly days 1..28. */
    public static List<Reward> parse(List<String> rows) {
        List<Reward> out = new ArrayList<>();
        for (String raw : rows) {
            String row = out.isEmpty() && raw.startsWith("\uFEFF") ? raw.substring(1) : raw;
            if (row.isBlank() || row.startsWith("day\t")) {
                continue;
            }
            String[] c = row.split("\t", -1);
            if (c.length != 5) {
                throw new IllegalArgumentException("malformed daily gift row " + (out.size() + 1));
            }
            int day = Integer.parseInt(c[0].trim()), item = Integer.parseInt(c[1].trim());
            int count = Integer.parseInt(c[2].trim()), period = Integer.parseInt(c[3].trim());
            if (day != out.size() + 1 || item <= 0 || count <= 0 || period < 0) {
                throw new IllegalArgumentException("invalid daily gift row " + (out.size() + 1));
            }
            if (item < 1_000_000 || item > 9_999_999 || count > 30_000 || period > 3650 || c[4].isBlank()
                    || c[4].length() > 180 || c[4].chars().anyMatch(ch -> ch < 32))
                throw new IllegalArgumentException("invalid daily gift values at day " + day);
            if (item / 1_000_000 == 1 && count != 1)
                throw new IllegalArgumentException("daily gift equip count must be 1 at day " + day);
            int granted = period;
            out.add(new Reward(day, item, count, granted, c[4].strip()));
        }
        if (out.size() != DAYS) {
            throw new IllegalArgumentException("daily gift needs " + DAYS + " days, got " + out.size());
        }
        return List.copyOf(out);
    }

    static String version(List<Reward> rewards) {
        StringBuilder sb = new StringBuilder();
        for (Reward r : rewards) {
            sb.append(r.day()).append('\t').append(r.itemId()).append('\t').append(r.count()).append('\t')
                    .append(r.periodDays()).append('\t').append(r.name()).append('\n');
        }
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(h, 0, 8);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Same days, items, counts and granted expiry as {@code game} (the table {@code DailyGift.claim} grants from). */
    public boolean matches(List<DailyGiftItemInfo> game) {
        List<Reward> mine = rewards();
        if (game == null || game.size() != mine.size()) {
            return false;
        }
        for (int i = 0; i < mine.size(); i++) {
            DailyGiftItemInfo g = game.get(i);
            Reward r = mine.get(i);
            if (g.getId() != r.day() || g.getItemId() != r.itemId() || g.getQuantity() != r.count()
                    || DailyGiftConstants.periodDays(g) != r.periodDays()) {
                return false;
            }
        }
        return true;
    }

    /** Server process: compares with the game's table once and logs one line when they differ. */
    public void checkAgainstGameOnce(Supplier<List<DailyGiftItemInfo>> game) {
        if (!checked.compareAndSet(false, true)) {
            return;
        }
        try {
            if (!matches(game.get())) {
                System.out.println("[DailyRewards] " + file + " differs from the game's daily gift table; the launcher shows the file");
            }
        } catch (RuntimeException e) {
            System.out.println("[DailyRewards] table check failed: " + e.getClass().getSimpleName());
        }
    }
}
