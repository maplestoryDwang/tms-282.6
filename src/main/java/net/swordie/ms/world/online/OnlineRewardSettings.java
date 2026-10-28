package net.swordie.ms.world.online;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;
import java.util.function.LongSupplier;

/**
 * 熱載入 {@code data/tms282-online-reward/online-reward.json}（與 revival-rewards.json、daily-gift.tsv 相同規則）：
 * 最多每 2 秒看一次檔案的修改時間與大小；檔案不存在 = 內建預設值；檔案壞掉（JSON 錯、數值不合理、超過 64 KB）
 * = 沿用上一份成功讀到的設定並記錄一次，等檔案再改。
 */
public final class OnlineRewardSettings {
    public static final Path DEFAULT_FILE = Path.of("data", "tms282-online-reward", "online-reward.json");
    static final long CHECK_MS = 2_000;
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final OnlineRewardSettings STANDARD = new OnlineRewardSettings(DEFAULT_FILE, System::currentTimeMillis);

    private final Path file;
    private final LongSupplier clock;
    private volatile OnlineRewardConfig current = OnlineRewardConfig.defaults();
    private long checkedAt = Long.MIN_VALUE;
    private String fileKey = "";
    private String problem = "";

    public OnlineRewardSettings(Path file, LongSupplier clock) {
        this.file = Objects.requireNonNull(file, "file");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /** 伺服器工作目錄下的設定檔。 */
    public static OnlineRewardSettings standard() {
        return STANDARD;
    }

    public Path file() {
        return file;
    }

    /** 目前有效的設定。 */
    public synchronized OnlineRewardConfig get() {
        long now = clock.getAsLong();
        if (checkedAt != Long.MIN_VALUE && now - checkedAt < CHECK_MS && now >= checkedAt) {
            return current;
        }
        checkedAt = now;
        String key;
        try {
            BasicFileAttributes a = Files.readAttributes(file, BasicFileAttributes.class);
            key = a.lastModifiedTime().toMillis() + ":" + a.size();
            if (key.equals(fileKey)) {
                return current;
            }
            if (a.size() > 64 * 1024) {
                throw new IllegalArgumentException("file larger than 64 KB");
            }
        } catch (NoSuchFileException e) {
            if (!fileKey.equals("missing")) {
                current = OnlineRewardConfig.defaults();
                fileKey = "missing";
                problem = "";
            }
            return current;
        } catch (IOException | RuntimeException e) {
            return keep(e);
        }
        try {
            String text = Files.readString(file);
            if (!text.isEmpty() && text.charAt(0) == '﻿') {
                text = text.substring(1);
            }
            OnlineRewardConfig next = OnlineRewardConfig.parse(JSON.readTree(text));
            current = next;
            fileKey = key;
            problem = "";
            System.out.printf("[OnlineReward] settings loaded: enabled=%s every %d min -> %d pts, daily cap %d, afk %d min%n",
                    next.enabled(), next.intervalMinutes(), next.pointsPerInterval(), next.dailyCapPoints(), next.afkMinutes());
        } catch (IOException | RuntimeException e) {
            fileKey = key; // do not re-read the same broken file every 2 s; a new edit changes the key
            return keep(e);
        }
        return current;
    }

    /** 最近一次讀檔的問題（空字串 = 沒問題）。 */
    public synchronized String problem() {
        get();
        return problem;
    }

    /** 下一次 {@link #get()} 一定重新檢查檔案（測試、管理工具改檔後用）。 */
    public synchronized void invalidate() {
        checkedAt = Long.MIN_VALUE;
    }

    private OnlineRewardConfig keep(Exception e) {
        String p = e.getClass().getSimpleName() + ": " + e.getMessage();
        if (!p.equals(problem)) {
            System.out.println("[OnlineReward] " + file + " not used, keeping the previous settings: " + p);
        }
        problem = p;
        return current;
    }
}
