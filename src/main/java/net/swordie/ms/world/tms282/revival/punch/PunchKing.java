package net.swordie.ms.world.tms282.revival.punch;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.world.tms282.activity.ActivityRewards;
import net.swordie.ms.world.tms282.revival.RevivalEvents;
import net.swordie.ms.world.tms282.revival.hub.HubDefinition;
import net.swordie.ms.world.tms282.revival.hub.ProgressStore;
import net.swordie.ms.world.tms282.revival.hub.SqlProgressStore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 世界最強拳擊王 (revival 3): results of event/revival_punch_king.js. The score is the damage dealt to the machine
 * chain 9834315..9834320 in 125 s; the weekly best is kept in revival_progress (event "punch", mission "best", count =
 * damage in millions, GREATEST upsert), the first run of a day gives a tier reward once per account (receipt
 * {@code rv_punch:<date>:run}).
 */
public final class PunchKing {
    public static final String EVENT = "punch";
    public static final int[] MACHINES = {9834315, 9834316, 9834317, 9834318, 9834319, 9834320};
    public static final int LOBBY = 993222000, READY = 993222100, GAME = 993222200, EXIT = 993222001;
    public static final long UNIT = 1_000_000L;

    /**
     * Built-in tier rewards by machines broken (index = broken 0..6); the live values come from revival-rewards.json
     * "punch.tiers" ({@link #rewards}).
     */
    public static final java.util.List<java.util.List<HubDefinition.Reward>> DEFAULT_TIERS = java.util.List.of(
            java.util.List.of(rw(2023999, 1)),
            java.util.List.of(rw(2023999, 2)),
            java.util.List.of(rw(2023999, 2), rw(2450180, 1)),
            java.util.List.of(rw(2023999, 3), rw(2450180, 1)),
            java.util.List.of(rw(2023999, 3), rw(2450180, 2)),
            java.util.List.of(rw(2023999, 4), rw(2450180, 2)),
            java.util.List.of(rw(2023999, 5), rw(2450180, 3)));

    private static HubDefinition.Reward rw(int id, int q) {
        return new HubDefinition.Reward(id, q);
    }

    /**
     * 番茄節 (夢中的拉契爾恩, revival 5): the 273 event_punch_king.js maps and tomato chain 9834335..9834340; the daily tier
     * reward is 夜之碎片 (receipt {@code rv_lacheln:<date>:tomato}), the weekly best is kept under event "tomato".
     */
    public static final String TOMATO = "tomato";
    public static final int[] TOMATO_MACHINES = {9834335, 9834336, 9834337, 9834338, 9834339, 9834340};
    public static final int TOMATO_READY = 993238100, TOMATO_GAME = 993238200, TOMATO_EXIT = 993238001;
    static final int NIGHT_SHARD = 4310343;
    /** Built-in 夜之碎片 by tomatoes broken 0..6; live values: revival-rewards.json "punch.tomatoShards". */
    public static final java.util.List<Integer> DEFAULT_TOMATO_SHARDS = java.util.List.of(10, 15, 20, 30, 40, 50, 70);

    private static volatile ProgressStore store = new SqlProgressStore();

    private PunchKing() {
    }

    public static void install(ProgressStore s) {
        store = s == null ? new SqlProgressStore() : s;
    }

    public static int tier(int broken) {
        return Math.max(0, Math.min(net.swordie.ms.world.tms282.revival.rewards.RevivalRewards.PUNCH_TIERS - 1, broken));
    }

    /** {item, quantity} rows of the tier, from the current rewards file. */
    public static int[][] rewards(int broken) {
        var list = net.swordie.ms.world.tms282.revival.rewards.RevivalRewardsSource.current().punchTiers().get(tier(broken));
        int[][] out = new int[list.size()][];
        for (int i = 0; i < list.size(); i++) out[i] = new int[]{list.get(i).itemId(), list.get(i).quantity()};
        return out;
    }

    public static int tomatoShards(int broken) {
        return net.swordie.ms.world.tms282.revival.rewards.RevivalRewardsSource.current().tomatoShards().get(tier(broken));
    }

    public static String weekKey(LocalDate day) {
        return HubDefinition.Period.WEEKLY.key(day);
    }

    /** Converts the run to the stored score (millions, int range). */
    public static int units(double damage) {
        if (!(damage > 0)) return 0;
        return (int) Math.min(Integer.MAX_VALUE, (long) (damage / UNIT));
    }

    public interface Granter {
        ActivityRewards.Result grant(String receiptKey, int[] ids, int[] qty);
    }

    /** Records the run and grants the daily reward; returns the player-facing summary. */
    public static String record(int charId, Granter granter, int broken, double damage, LocalDate day) {
        int units = units(damage);
        StringBuilder t = new StringBuilder("【世界最強拳擊王】擊破 ").append(Math.max(0, broken)).append(" 台機器，總傷害約 ")
                .append(String.format("%,d", units)).append(" 百萬。");
        try {
            ProgressStore.Row old = store.load(charId, EVENT).get(ProgressStore.key("best", weekKey(day)));
            store.save(charId, EVENT, "best", weekKey(day), units, false);
            if (old == null || units > old.count()) t.append("\r\n刷新本週最佳紀錄！");
        } catch (Exception e) {
            t.append("\r\n（紀錄暫時無法儲存）");
        }
        int[][] r = rewards(broken);
        int[] ids = new int[r.length], qty = new int[r.length];
        for (int i = 0; i < r.length; i++) {
            ids[i] = r[i][0];
            qty[i] = r[i][1];
        }
        ActivityRewards.Result g = granter.grant("rv_" + EVENT + ":" + day + ":run", ids, qty);
        t.append("\r\n").append(switch (g) {
            case GRANTED -> "已發放今日挑戰獎勵（第 " + tier(broken) + " 階）。";
            case ALREADY -> "本帳號今天已領取過挑戰獎勵，紀錄仍會列入排行。";
            case FULL -> "背包空間不足，今天的獎勵沒有發放，整理後再挑戰一次即可領取。";
            default -> "今天的獎勵暫時無法發放。";
        });
        return t.toString();
    }

    public static String record(Char chr, int broken, double damage) {
        return record(chr.getId(), (k, ids, q) -> ActivityRewards.grantOnce(chr, k, ids, q), broken, damage, RevivalEvents.today());
    }

    /** The tomato run: weekly best under "tomato", 夜之碎片 by machines broken, once a day per account. */
    public static String recordTomato(int charId, Granter granter, int broken, double damage, LocalDate day) {
        int units = units(damage);
        int shards = tomatoShards(broken);
        StringBuilder t = new StringBuilder("【番茄節】擊破 ").append(Math.max(0, broken)).append(" 顆番茄果實，總傷害約 ")
                .append(String.format("%,d", units)).append(" 百萬。");
        try {
            store.save(charId, TOMATO, "best", weekKey(day), units, false);
        } catch (Exception e) {
            t.append("\r\n（紀錄暫時無法儲存）");
        }
        ActivityRewards.Result g = granter.grant("rv_lacheln:" + day + ":tomato", new int[]{NIGHT_SHARD}, new int[]{shards});
        t.append("\r\n").append(switch (g) {
            case GRANTED -> "獲得夜之碎片 " + shards + " 個（今日番茄節獎勵）。";
            case ALREADY -> "本帳號今天已領取過番茄節獎勵。";
            case FULL -> "背包空間不足，今天的獎勵沒有發放，整理後再挑戰一次即可領取。";
            default -> "今天的獎勵暫時無法發放。";
        });
        return t.toString();
    }

    public static String recordTomato(Char chr, int broken, double damage) {
        return recordTomato(chr.getId(), (k, ids, q) -> ActivityRewards.grantOnce(chr, k, ids, q), broken, damage, RevivalEvents.today());
    }

    public record Entry(String name, int units) {
    }

    /** Weekly top list from revival_progress (names from characterstats.characterid). */
    public static List<Entry> weeklyTop(int limit, LocalDate day) {
        List<Entry> out = new ArrayList<>();
        String sql = "SELECT p.count, (SELECT cs.name FROM characterstats cs WHERE cs.characterid = p.charid LIMIT 1) "
                + "FROM revival_progress p WHERE p.event=? AND p.mission='best' AND p.period=? ORDER BY p.count DESC LIMIT ?";
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, EVENT);
            p.setString(2, weekKey(day));
            p.setInt(3, Math.max(1, Math.min(20, limit)));
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) out.add(new Entry(r.getString(2) == null ? "?" : r.getString(2), r.getInt(1)));
            }
        } catch (Exception e) {
            System.out.println("[Revival] punch ranking " + e.getClass().getSimpleName());
        }
        return out;
    }

    public static int myBest(int charId, LocalDate day) {
        try {
            ProgressStore.Row r = store.load(charId, EVENT).get(ProgressStore.key("best", weekKey(day)));
            return r == null ? 0 : r.count();
        } catch (Exception e) {
            return -1;
        }
    }
}
