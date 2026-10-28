package net.swordie.ms.world.online;

import net.swordie.ms.Server;
import net.swordie.ms.ServerConfig;
import net.swordie.ms.client.Client;
import net.swordie.ms.client.User;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.api.launcher.OnlineAccounts;
import net.swordie.ms.enums.ChatType;
import net.swordie.ms.handlers.Timer;
import net.swordie.ms.util.Position;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/**
 * 累積上線獎勵（leftovers-1005 任務 7）：每分鐘檢查一次線上的角色，每個登入帳號（users.id）最多算 1 分鐘，
 * 累積到設定的分鐘數就發樂豆點（users.nxPrepaid），並在遊戲內通知。
 * <ul>
 *   <li>只算真正在遊戲裡的時間：連線還在、角色在頻道的地圖上（Client.IN_FIELD）；商城／拍賣場、角色選擇、登入畫面都不算。</li>
 *   <li>同一帳號同時有兩個角色在線也只算 1 分鐘；進度存在 online_reward_day（每帳號每天一列），重新登入、換頻道、
 *       伺服器重開都接續。日期以伺服器時區（{@link ServerConfig#ZONE}）的 00:00 換日。</li>
 *   <li>掛機：角色連續 afkMinutes 分鐘沒有移動（同一張地圖、同一個座標）就暫停累計，動了之後從下一分鐘恢復；
 *       afkMinutes = 0 關閉判斷。換地圖、傳送、移動都算「有動作」。</li>
 *   <li>發放走 {@link JdbcOnlineRewardLedger}：進度、錢包、收據同一筆交易；成交後才更新記憶體中的 User 與通知。</li>
 * </ul>
 * 設定：{@code data/tms282-online-reward/online-reward.json}（{@link OnlineRewardSettings} 熱載入），見 docs/online-reward.md。
 */
public final class OnlineRewardService {
    static final long PERIOD_MS = 60_000L;
    private static volatile boolean started;
    private static final OnlineRewardService STANDARD = new OnlineRewardService(OnlineRewardSettings.standard(),
            JdbcOnlineRewardLedger.standard(), System::currentTimeMillis, OnlineAccounts.live());

    /** 一個帳號最後一次「有動作」的位置與時間。 */
    record Activity(int fieldId, int x, int y, long movedAt) {
    }

    /** 這一輪要計入的帳號：用哪個角色（通知、AFK 判斷）。 */
    public record Candidate(int userId, Char chr, int fieldId, int x, int y) {
    }

    /** 一個帳號這一輪的處理結果（測試與記錄用）。 */
    public enum Outcome { COUNTED, GRANTED, SAME_MINUTE, AFK, DISABLED, FAILED }

    private final OnlineRewardSettings settings;
    private final OnlineRewardLedger ledger;
    private final LongSupplier clock;
    private final OnlineAccounts accounts;
    private final Map<Integer, Activity> activity = new ConcurrentHashMap<>();

    public OnlineRewardService(OnlineRewardSettings settings, OnlineRewardLedger ledger, LongSupplier clock, OnlineAccounts accounts) {
        this.settings = settings;
        this.ledger = ledger;
        this.clock = clock;
        this.accounts = accounts;
    }

    public static OnlineRewardService standard() {
        return STANDARD;
    }

    /** Server.initTimers：每分鐘一次（eventTimer）。 */
    public static synchronized void start(Timer timer) {
        if (started || timer == null) {
            return;
        }
        started = true;
        timer.addFixedRateEvent(STANDARD::safeTick, PERIOD_MS, PERIOD_MS, TimeUnit.MILLISECONDS, true);
    }

    private void safeTick() {
        try {
            tick(eligible(Server.get().getChars().values()));
        } catch (Throwable t) {
            System.out.println("[OnlineReward] tick failed: " + t);
        }
    }

    /** 線上、在地圖上、不是機器人的角色，每個帳號取一個。 */
    public static List<Candidate> eligible(Collection<Char> chars) {
        Map<Integer, Candidate> byUser = new LinkedHashMap<>();
        for (Char chr : chars) {
            if (chr == null || chr.isBot() || !chr.isOnline()) {
                continue;
            }
            Client c = chr.getClient();
            User u = chr.getUser();
            if (c == null || u == null || u.getId() <= 0 || c.getChr() != chr || c.getCh() == null || !c.getCh().isActive()
                    || c.getCurrentState() != Client.IN_FIELD || c.getNextState() != Client.NONE || chr.getField() == null) {
                continue;
            }
            Position p = chr.getPosition();
            byUser.putIfAbsent(u.getId(), new Candidate(u.getId(), chr, chr.getField().getId(), p == null ? 0 : p.getX(), p == null ? 0 : p.getY()));
        }
        return new ArrayList<>(byUser.values());
    }

    /** 一輪：每個帳號計入 1 分鐘（掛機的除外）。回傳每個帳號的結果。 */
    public Map<Integer, Outcome> tick(List<Candidate> candidates) {
        Map<Integer, Outcome> out = new LinkedHashMap<>();
        OnlineRewardConfig config = settings.get();
        long now = clock.getAsLong();
        Set<Integer> present = new java.util.HashSet<>();
        for (Candidate c : candidates) {
            present.add(c.userId());
        }
        activity.keySet().retainAll(present); // logged out: the AFK clock restarts on the next login
        if (!config.enabled()) {
            for (Candidate c : candidates) {
                out.put(c.userId(), Outcome.DISABLED);
            }
            return out;
        }
        LocalDate day = LocalDate.ofInstant(java.time.Instant.ofEpochMilli(now), ServerConfig.ZONE);
        for (Candidate c : candidates) {
            if (afk(c, config, now)) {
                out.put(c.userId(), Outcome.AFK);
                continue;
            }
            out.put(c.userId(), credit(c, day, now, config));
        }
        return out;
    }

    boolean afk(Candidate c, OnlineRewardConfig config, long now) {
        Activity last = activity.get(c.userId());
        if (last == null || last.fieldId() != c.fieldId() || last.x() != c.x() || last.y() != c.y()) {
            activity.put(c.userId(), new Activity(c.fieldId(), c.x(), c.y(), now));
            return false;
        }
        return config.afkMinutes() > 0 && now - last.movedAt() >= config.afkMinutes() * 60_000L;
    }

    private Outcome credit(Candidate c, LocalDate day, long now, OnlineRewardConfig config) {
        User user = c.chr() == null ? null : c.chr().getUser();
        List<User> copies = copies(c.userId(), user);
        try {
            OnlineRewardLedger.Credit r = locked(copies, 0, () -> {
                OnlineRewardLedger.Credit credit = ledger.credit(c.userId(), day, now, config);
                if (credit.granted() > 0) {
                    for (User copy : copies) {
                        copy.setNxPrepaid((int) credit.balanceAfter());
                    }
                }
                return credit;
            });
            if (!r.counted()) {
                return Outcome.SAME_MINUTE;
            }
            if (r.granted() > 0) {
                notice(c.chr(), config.noticeFor(r.granted(), r.progress().minutes(), r.progress().points()));
                return Outcome.GRANTED;
            }
            return Outcome.COUNTED;
        } catch (SQLException | RuntimeException e) {
            System.out.println("[OnlineReward] user=" + c.userId() + " not credited (nothing granted): " + e);
            return Outcome.FAILED;
        }
    }

    private static void notice(Char chr, String text) {
        if (chr == null) {
            return;
        }
        try {
            chr.chatMessage(ChatType.SystemNotice, text);
        } catch (RuntimeException e) {
            System.out.println("[OnlineReward] notice not sent: " + e);
        }
    }

    /** 當日進度（啟動器、腳本查詢用）。 */
    public OnlineRewardLedger.Progress progress(int userId) throws SQLException {
        LocalDate day = LocalDate.ofInstant(java.time.Instant.ofEpochMilli(clock.getAsLong()), ServerConfig.ZONE);
        return ledger.read(userId, day);
    }

    public OnlineRewardConfig config() {
        return settings.get();
    }

    private List<User> copies(int userId, User user) {
        Set<User> unique = Collections.newSetFromMap(new IdentityHashMap<>());
        unique.addAll(accounts.copies(userId));
        if (user != null) {
            unique.add(user);
        }
        List<User> list = new ArrayList<>(unique);
        list.sort(Comparator.comparingInt(System::identityHashCode)); // same order as NewbiePoints / AccountSecretWriter
        return list;
    }

    @FunctionalInterface
    private interface Work {
        OnlineRewardLedger.Credit run() throws SQLException;
    }

    private static OnlineRewardLedger.Credit locked(List<User> copies, int i, Work work) throws SQLException {
        if (i == copies.size()) {
            return work.run();
        }
        synchronized (copies.get(i)) {
            return locked(copies, i + 1, work);
        }
    }
}
