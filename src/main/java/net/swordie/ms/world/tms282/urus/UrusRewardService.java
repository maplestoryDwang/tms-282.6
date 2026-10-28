package net.swordie.ms.world.tms282.urus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.swordie.ms.ServerConstants;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.enums.ChatType;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.util.DataPrinter;
import net.swordie.ms.util.Randomizer;
import net.swordie.ms.world.field.Field;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Urus (烏勒斯, 8881000) clear reward: Urus coins for every participant, scaled by damage rank
 * ({@code data/resources/tms282/urus/urus-rewards.json}).
 * <p>
 * Mob keeps per-player damage in {@code Mob#getDamageDone()} but clears it before notifying observers of the death,
 * and the kill path (Mob/BossHelper) is outside this package. So {@link #track(Mob)} (called from
 * {@code ScriptManagerImpl.spawnBossMob}) snapshots the damage map every {@value #SNAPSHOT_MS} ms while the boss lives
 * and registers an observer that pays out on death (hp &lt;= 0) from the last snapshot; at most the final
 * {@value #SNAPSHOT_MS} ms of damage is missed for ranking. Removal without a kill (timeout, reset) pays nothing.
 * Coins that do not fit the bag are stored in {@code urus_reward_pending} and claimed at NPC 艾霞 (9070101).
 */
public final class UrusRewardService {
    static final long SNAPSHOT_MS = 500;

    private record Grant(boolean each, List<Integer> items) {
    }

    private static final ScheduledExecutorService TIMER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "urus-damage-snapshot");
        t.setDaemon(true);
        return t;
    });
    private static volatile UrusRewardService instance;
    private static volatile boolean ready;

    private final Set<Integer> bossIds;
    private final int[] rankAmounts;
    private final int minAmount;
    private final List<Grant> grants;

    private UrusRewardService(Set<Integer> bossIds, int[] rankAmounts, int minAmount, List<Grant> grants) {
        this.bossIds = Set.copyOf(bossIds);
        this.rankAmounts = rankAmounts;
        this.minAmount = minAmount;
        this.grants = List.copyOf(grants);
    }

    public static UrusRewardService get() {
        UrusRewardService s = instance;
        if (s == null) {
            synchronized (UrusRewardService.class) {
                if (instance == null) instance = load();
                s = instance;
            }
        }
        return s;
    }

    private static UrusRewardService load() {
        Path path = Path.of(ServerConstants.RESOURCES_DIR, "tms282", "urus", "urus-rewards.json");
        try {
            JsonNode root = new ObjectMapper().readTree(path.toFile());
            Set<Integer> ids = new HashSet<>();
            root.path("bossMobIds").forEach(n -> ids.add(n.asInt()));
            List<Integer> amounts = new ArrayList<>();
            root.path("rankAmounts").forEach(n -> amounts.add(n.asInt()));
            List<Grant> grants = new ArrayList<>();
            for (JsonNode g : root.path("grants")) {
                List<Integer> items = new ArrayList<>();
                g.path("items").forEach(n -> items.add(n.asInt()));
                if (!items.isEmpty()) grants.add(new Grant(!"randomOne".equals(g.path("mode").asText("each")), List.copyOf(items)));
            }
            return new UrusRewardService(ids, amounts.stream().mapToInt(Integer::intValue).toArray(),
                    root.path("minAmount").asInt(3), grants);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load " + path, e);
        }
    }

    public boolean isUrusBoss(int templateId) {
        return bossIds.contains(templateId);
    }

    public int amountForRank(int rank) {
        return rank >= 1 && rank <= rankAmounts.length ? Math.max(minAmount, rankAmounts[rank - 1]) : minAmount;
    }

    /** Starts damage snapshots and the death observer for a freshly spawned Urus. */
    public void track(Mob mob) {
        if (mob == null || !isUrusBoss(mob.getTemplateId())) return;
        Map<Integer, Long> snapshot = new ConcurrentHashMap<>();
        ScheduledFuture<?>[] task = new ScheduledFuture<?>[1];
        task[0] = TIMER.scheduleAtFixedRate(() -> {
            try {
                for (Map.Entry<Char, Long> e : new HashMap<>(mob.getDamageDone()).entrySet()) {
                    if (e.getKey() != null && e.getValue() != null) snapshot.merge(e.getKey().getId(), e.getValue(), Math::max);
                }
            } catch (ConcurrentModificationException ignored) {
                // next tick retries
            } catch (RuntimeException e) {
                DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
            }
        }, SNAPSHOT_MS, SNAPSHOT_MS, TimeUnit.MILLISECONDS);
        Field field = mob.getField();
        mob.addObserver(new Observer() {
            private boolean done;

            @Override
            public synchronized void update(Observable o, Object arg) {
                if (done) return;
                done = true;
                task[0].cancel(false);
                o.deleteObserver(this);
                if (mob.getHp() <= 0) reward(field, snapshot);
            }
        });
    }

    private void reward(Field field, Map<Integer, Long> damage) {
        if (field == null) return;
        List<Char> players = new ArrayList<>(field.getChars());
        players.sort(Comparator.comparingLong((Char c) -> damage.getOrDefault(c.getId(), 0L)).reversed());
        int rank = 0;
        for (Char chr : players) {
            rank++;
            int amount = damage.getOrDefault(chr.getId(), 0L) > 0 ? amountForRank(rank) : minAmount;
            try {
                give(chr, rank, amount);
            } catch (RuntimeException e) {
                DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
            }
        }
    }

    private void give(Char chr, int rank, int amount) {
        Map<Integer, Integer> items = new LinkedHashMap<>();
        for (Grant g : grants) {
            if (g.each()) g.items().forEach(id -> items.merge(id, amount, Integer::sum));
            else items.merge(g.items().get(Randomizer.nextInt(g.items().size())), amount, Integer::sum);
        }
        boolean stored = false;
        // 2026-09-29: ECONOMY_MONITOR before Char (282 lock order, see BossRunService.CharDelivery).
        synchronized (net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime.ECONOMY_MONITOR) {
        synchronized (chr) {
            for (Map.Entry<Integer, Integer> e : items.entrySet()) {
                if (chr.canHold(e.getKey(), e.getValue())) {
                    chr.addItemToInventory(e.getKey(), e.getValue());
                } else {
                    storePending(chr.getId(), e.getKey(), e.getValue());
                    stored = true;
                }
            }
        }
        }
        chr.chatMessage(ChatType.SystemNotice, String.format("擊敗烏勒斯！傷害排名第 %d 名，獲得烏勒斯硬幣各 %d 個。", rank, amount));
        if (stored) {
            chr.chatMessage(ChatType.SystemNotice, "背包欄位空間不足，部分烏勒斯硬幣已暫存，請整理背包後找艾霞領取。");
        }
        DataPrinter.send(DataPrinter.ITEM, String.format("烏勒斯獎勵：%s(%d) 排名 %d 數量 %d %s%s。",
                chr.getName(), chr.getId(), rank, amount, items, stored ? " (部分暫存)" : ""), true);
    }

    // Pending storage -------------------------------------------------------------------------------------------

    private static synchronized void ensure() throws SQLException {
        if (ready) return;
        try (Connection c = DatabaseManager.getConnection(); Statement s = c.createStatement()) {
            s.executeUpdate("CREATE TABLE IF NOT EXISTS urus_reward_pending (char_id INT NOT NULL, item_id INT NOT NULL,"
                    + " quantity INT NOT NULL, PRIMARY KEY(char_id, item_id)) ENGINE=InnoDB");
        }
        ready = true;
    }

    private static void storePending(int charId, int itemId, int quantity) {
        try {
            ensure();
            try (Connection c = DatabaseManager.getConnection(); PreparedStatement p = c.prepareStatement(
                    "INSERT INTO urus_reward_pending(char_id, item_id, quantity) VALUES(?,?,?)"
                            + " ON DUPLICATE KEY UPDATE quantity=quantity+VALUES(quantity)")) {
                p.setInt(1, charId);
                p.setInt(2, itemId);
                p.setInt(3, quantity);
                p.executeUpdate();
            }
        } catch (SQLException e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
        }
    }

    public int pendingCount(Char chr) {
        try {
            ensure();
            try (Connection c = DatabaseManager.getConnection(); PreparedStatement p = c.prepareStatement(
                    "SELECT COALESCE(SUM(quantity),0) FROM urus_reward_pending WHERE char_id=?")) {
                p.setInt(1, chr.getId());
                try (ResultSet r = p.executeQuery()) {
                    return r.next() ? r.getInt(1) : 0;
                }
            }
        } catch (SQLException e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
            return 0;
        }
    }

    /** Delivers stored coins that fit into the bag; returns a Traditional Chinese summary. */
    public String claimPending(Char chr) {
        int delivered = 0;
        boolean left = false;
        synchronized (net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime.ECONOMY_MONITOR) {
        synchronized (chr) {
            try {
                ensure();
                try (Connection c = DatabaseManager.getConnection()) {
                    Map<Integer, Integer> rows = new LinkedHashMap<>();
                    try (PreparedStatement p = c.prepareStatement(
                            "SELECT item_id, quantity FROM urus_reward_pending WHERE char_id=?")) {
                        p.setInt(1, chr.getId());
                        try (ResultSet r = p.executeQuery()) {
                            while (r.next()) rows.put(r.getInt(1), r.getInt(2));
                        }
                    }
                    for (Map.Entry<Integer, Integer> e : rows.entrySet()) {
                        if (!chr.canHold(e.getKey(), e.getValue())) {
                            left = true;
                            continue;
                        }
                        try (PreparedStatement p = c.prepareStatement(
                                "DELETE FROM urus_reward_pending WHERE char_id=? AND item_id=? AND quantity=?")) {
                            p.setInt(1, chr.getId());
                            p.setInt(2, e.getKey());
                            p.setInt(3, e.getValue());
                            if (p.executeUpdate() != 1) continue;
                        }
                        chr.addItemToInventory(e.getKey(), e.getValue());
                        delivered += e.getValue();
                    }
                }
            } catch (SQLException e) {
                DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
                return "發生未知錯誤，請重試。";
            }
        }
        }
        if (delivered == 0) return left ? "背包欄位空間不足，請檢查。" : "沒有暫存的烏勒斯硬幣。";
        return "已領取暫存的烏勒斯硬幣 " + delivered + " 個。" + (left ? "\r\n仍有部分因背包空間不足而保留，請整理後再來。" : "");
    }
}
