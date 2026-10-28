package net.swordie.ms.world.event.hottime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.swordie.ms.ServerConfig;
import net.swordie.ms.ServerConstants;
import net.swordie.ms.handlers.Timer;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 讀取 data/resources/tms282/events/hottime.json，每分鐘檢查一次：
 * <ul>
 *     <li>hottime：時段開始/結束時全服公告 + 滑動公告，開始時發獎，並更新 {@link HotTime} 倍率。</li>
 *     <li>notice：固定時間點或間隔的全服公告（移植自 MetaMS 273 server_tip / event_schedule / server_date_notice）。</li>
 * </ul>
 * 時區使用 {@link ServerConfig#ZONE}。
 */
public final class HotTimeScheduler {

    public static final Path CONFIG_PATH = Path.of(ServerConstants.RESOURCES_DIR, "tms282", "events", "hottime.json");

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Config {
        public List<HotTimeEntry> entries = new ArrayList<>();

        public Config() {
        }
    }

    private static volatile List<HotTimeEntry> entries = List.of();
    private static final Set<String> activeHotTimes = ConcurrentHashMap.newKeySet();
    private static final Map<String, String> firedNotices = new ConcurrentHashMap<>();
    private static final Map<String, Long> nextIntervalDue = new ConcurrentHashMap<>();
    private static volatile boolean started;

    private HotTimeScheduler() {
    }

    public static synchronized void load() {
        if (!Files.isRegularFile(CONFIG_PATH)) {
            entries = List.of();
            System.out.println("[HotTime] 找不到 " + CONFIG_PATH + "，HotTime 未啟用。");
            return;
        }
        try (InputStream in = Files.newInputStream(CONFIG_PATH)) {
            Config cfg = new ObjectMapper().readValue(in, Config.class);
            List<HotTimeEntry> list = new ArrayList<>();
            if (cfg != null && cfg.entries != null) {
                for (HotTimeEntry e : cfg.entries) {
                    if (e == null) {
                        continue;
                    }
                    try {
                        if (e.isHotTime()) {
                            HotTimeEntry.parseTime(e.start);
                            HotTimeEntry.parseTime(e.end);
                        } else if (e.times != null) {
                            for (String t : e.times) {
                                HotTimeEntry.parseTime(t);
                            }
                        }
                        list.add(e);
                    } catch (Exception ex) {
                        System.err.printf("[HotTime] 項目 %s 時間格式錯誤，已略過: %s%n", e.key(), ex.getMessage());
                    }
                }
            }
            entries = List.copyOf(list);
            long enabled = list.stream().filter(x -> x.enabled).count();
            System.out.printf("[HotTime] 已載入 %d 筆排程（啟用 %d 筆），時區 %s%n", list.size(), enabled, ServerConfig.ZONE);
        } catch (Exception ex) {
            System.err.println("[HotTime] 讀取 hottime.json 失敗: " + ex);
            entries = List.of();
        }
    }

    /** 重新讀取設定；目前生效中的 hottime 會在下一次 tick 依新設定重新判斷。 */
    public static void reload() {
        load();
        nextIntervalDue.clear();
        tick();
    }

    /** 由 Server.initTimers 呼叫，對齊到下一個整分鐘後每分鐘執行一次。 */
    public static synchronized void start(Timer timer) {
        if (started || timer == null) {
            return;
        }
        started = true;
        load();
        long now = System.currentTimeMillis();
        long delay = 60_000L - (now % 60_000L) + 1_000L;
        timer.addFixedRateEvent(HotTimeScheduler::safeTick, delay, 60_000L, TimeUnit.MILLISECONDS, true);
    }

    private static void safeTick() {
        try {
            tick();
        } catch (Throwable t) {
            System.err.println("[HotTime] tick 發生錯誤: " + t);
        }
    }

    static synchronized void tick() {
        if (net.swordie.ms.Server.get().getWorld() == null) {
            return;
        }
        LocalDateTime now = ZonedDateTime.now(ServerConfig.ZONE).toLocalDateTime();
        double exp = 1.0, drop = 1.0, meso = 1.0;
        Set<String> nowActive = new HashSet<>();

        for (HotTimeEntry e : entries) {
            if (!e.enabled) {
                continue;
            }
            if (e.isHotTime()) {
                LocalDateTime occurrence = activeOccurrenceStart(e, now);
                if (occurrence == null) {
                    continue;
                }
                String key = e.key();
                nowActive.add(key);
                exp = Math.max(exp, e.expRate);
                drop = Math.max(drop, e.dropRate);
                meso = Math.max(meso, e.mesoRate);
                if (activeHotTimes.add(key)) {
                    onStart(e, now, occurrence);
                }
            } else if (e.isNotice()) {
                handleNotice(e, now);
            }
        }
        HotTime.set(exp, drop, meso);

        for (String key : new ArrayList<>(activeHotTimes)) {
            if (!nowActive.contains(key)) {
                activeHotTimes.remove(key);
                HotTimeEntry e = find(key);
                onEnd(e, key);
            }
        }
    }

    /** 若 e 在 now 生效，回傳本次時段的開始時間；否則 null。支援跨午夜 (end <= start)。 */
    static LocalDateTime activeOccurrenceStart(HotTimeEntry e, LocalDateTime now) {
        LocalTime s = HotTimeEntry.parseTime(e.start);
        LocalTime en = HotTimeEntry.parseTime(e.end);
        if (s == null || en == null) {
            return null;
        }
        LocalDate today = now.toLocalDate();
        LocalTime t = now.toLocalTime();
        if (en.isAfter(s)) {
            if (e.matchesDay(today.getDayOfWeek()) && !t.isBefore(s) && t.isBefore(en)) {
                return today.atTime(s);
            }
            return null;
        }
        // 跨午夜
        if (e.matchesDay(today.getDayOfWeek()) && !t.isBefore(s)) {
            return today.atTime(s);
        }
        LocalDate yesterday = today.minusDays(1);
        if (e.matchesDay(yesterday.getDayOfWeek()) && t.isBefore(en)) {
            return yesterday.atTime(s);
        }
        return null;
    }

    private static HotTimeEntry find(String key) {
        for (HotTimeEntry e : entries) {
            if (e.key().equals(key)) {
                return e;
            }
        }
        return null;
    }

    private static void onStart(HotTimeEntry e, LocalDateTime now, LocalDateTime occurrence) {
        System.out.printf("[HotTime] %s 開始 (EXP x%.2f / 掉寶 x%.2f / 楓幣 x%.2f)%n", e.key(), e.expRate, e.dropRate, e.mesoRate);
        HotTimeBroadcaster.worldNotice(e.message);
        if (e.slide != null && !e.slide.isEmpty()) {
            HotTimeBroadcaster.slide(e.slide, true);
        }
        if (e.rewardItem > 0) {
            long late = Duration.between(occurrence, now).toMinutes();
            if (late <= Math.max(0, e.rewardGraceMinutes)) {
                int n = HotTimeBroadcaster.giveReward(e);
                System.out.printf("[HotTime] %s 已發放 %d x%d 給 %d 名線上角色%n", e.key(), e.rewardItem, Math.max(1, e.rewardCount), n);
            } else {
                System.out.printf("[HotTime] %s 於開始後 %d 分鐘才偵測到（可能為重啟），略過發獎%n", e.key(), late);
            }
        }
    }

    private static void onEnd(HotTimeEntry e, String key) {
        System.out.printf("[HotTime] %s 結束%n", key);
        if (e == null) {
            return;
        }
        HotTimeBroadcaster.worldNotice(e.endMessage);
        if (e.endSlide != null) {
            HotTimeBroadcaster.slide(e.endSlide, !e.endSlide.isEmpty());
        }
    }

    private static void handleNotice(HotTimeEntry e, LocalDateTime now) {
        if (e.messages == null || e.messages.isEmpty() || !e.matchesDay(now.getDayOfWeek())) {
            return;
        }
        String key = e.key();
        if (e.times != null && !e.times.isEmpty()) {
            LocalTime t = now.toLocalTime();
            for (String ts : e.times) {
                LocalTime target = HotTimeEntry.parseTime(ts);
                if (target == null) {
                    continue;
                }
                // 允許 2 分鐘內補發（timer 延遲時），同一天同一時間點只發一次
                LocalTime windowEnd = target.plusMinutes(2);
                boolean wraps = windowEnd.isBefore(target);
                if (!t.isBefore(target) && (wraps || t.isBefore(windowEnd))) {
                    String firedKey = key + "@" + ts;
                    String stamp = now.toLocalDate().toString();
                    if (!stamp.equals(firedNotices.get(firedKey))) {
                        firedNotices.put(firedKey, stamp);
                        fireNotice(e);
                    }
                }
            }
        }
        if (e.intervalMinutes > 0) {
            long nowMs = System.currentTimeMillis();
            Long due = nextIntervalDue.get(key);
            if (due == null) {
                nextIntervalDue.put(key, nowMs + nextDelayMs(e));
            } else if (nowMs >= due) {
                fireNotice(e);
                nextIntervalDue.put(key, nowMs + nextDelayMs(e));
            }
        }
    }

    private static long nextDelayMs(HotTimeEntry e) {
        int minutes = e.random ? ThreadLocalRandom.current().nextInt(1, e.intervalMinutes + 1) : e.intervalMinutes;
        return minutes * 60_000L - 5_000L; // 扣 5 秒避免因 tick 抖動多等一輪
    }

    private static void fireNotice(HotTimeEntry e) {
        List<String> msgs = e.messages;
        String msg = msgs.size() == 1 ? msgs.get(0) : msgs.get(ThreadLocalRandom.current().nextInt(msgs.size()));
        String prefix = e.prefix == null ? "" : e.prefix;
        HotTimeBroadcaster.send(e.style, prefix + msg, e);
    }

    public static List<HotTimeEntry> getEntries() {
        return entries;
    }
}
