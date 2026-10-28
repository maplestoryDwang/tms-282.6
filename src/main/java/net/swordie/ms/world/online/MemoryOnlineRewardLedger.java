package net.swordie.ms.world.online;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 測試用：與 {@link JdbcOnlineRewardLedger} 相同的規則，資料放在記憶體；錢包也在這裡（userId → 點數）。 */
public final class MemoryOnlineRewardLedger implements OnlineRewardLedger {
    private record Key(int userId, LocalDate day) {
    }

    private record Row(int minutes, int points, long lastTickAt) {
    }

    /** 一筆收據：與 online_reward_log 的欄位相同。 */
    public record Receipt(int userId, LocalDate day, int amount, int pointsAfter, int minutes) {
    }

    private final Map<Key, Row> rows = new HashMap<>();
    private final Map<Integer, Long> wallets = new HashMap<>();
    private final List<Receipt> receipts = new ArrayList<>();
    /** 測試注入：發放時錢包寫入失敗（模擬 SQLException → 整筆回滾）。 */
    public volatile boolean failWalletWrite;

    public synchronized void setBalance(int userId, long balance) {
        wallets.put(userId, balance);
    }

    public synchronized long balance(int userId) {
        return wallets.getOrDefault(userId, 0L);
    }

    public synchronized List<Receipt> receipts() {
        return List.copyOf(receipts);
    }

    @Override
    public synchronized Credit credit(int userId, LocalDate day, long nowMs, OnlineRewardConfig config) throws java.sql.SQLException {
        Key key = new Key(userId, day);
        Row row = rows.getOrDefault(key, new Row(0, 0, 0));
        if (row.lastTickAt() > 0 && nowMs - row.lastTickAt() < MIN_TICK_GAP_MS) {
            return new Credit(new Progress(userId, day, row.minutes(), row.points()), false, 0, -1);
        }
        int minutes = row.minutes() + 1;
        int owed = Math.max(0, config.pointsEarnedBy(minutes) - row.points());
        long after = -1;
        if (owed > 0) {
            if (failWalletWrite) {
                throw new java.sql.SQLException("injected wallet failure");
            }
            long before = balance(userId);
            after = before + owed;
            if (after > Integer.MAX_VALUE) {
                throw new java.sql.SQLException("wallet limit");
            }
            wallets.put(userId, after);
            receipts.add(new Receipt(userId, day, owed, row.points() + owed, minutes));
        }
        Row next = new Row(minutes, row.points() + owed, nowMs);
        rows.put(key, next);
        return new Credit(new Progress(userId, day, next.minutes(), next.points()), true, owed, after);
    }

    @Override
    public synchronized Progress read(int userId, LocalDate day) {
        Row row = rows.get(new Key(userId, day));
        return row == null ? Progress.empty(userId, day) : new Progress(userId, day, row.minutes(), row.points());
    }
}
