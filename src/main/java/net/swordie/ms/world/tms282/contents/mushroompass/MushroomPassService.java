package net.swordie.ms.world.tms282.contents.mushroompass;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.enums.ChatType;
import net.swordie.ms.world.tms282.contents.common.AccountLedger;
import net.swordie.ms.world.tms282.contents.common.HotJsonFile;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 究極菇菇通行證 with the client's own pass window (content-1005 feature 2).
 * <ul>
 *     <li>{@link #open}: LP065D season table, the claimed marks of this account as QR-ex "b&lt;idx&gt;" of the
 *     season's {@code claimQuest}, then LP04D9 OPEN_UI 120018. Called from NPC 茱妮 9401434.</li>
 *     <li>{@link #claim}: CP04FC. Basic rewards only (this server sells no premium / hyper pass): season running,
 *     index in range, character level, then {@link AccountLedger#claim} (one claim per account per season and
 *     index, receipt {@code <season>:b<idx>} of {@link #LEDGER_QUEST}), then the QR-ex mark.</li>
 * </ul>
 * Rewards, levels and periods live in {@code data/tms282-mushroompass/season.json} (re-read 2 s after a change).
 */
public final class MushroomPassService {
    /** account_questex.questid of the claim receipts (68404 does not exist in the 282 Quest.wz). */
    public static final int LEDGER_QUEST = 68404;
    public static final Path FILE = Path.of("data/tms282-mushroompass/season.json");

    public enum ClaimResult {GRANTED, ALREADY, NO_SEASON, BAD_INDEX, LEVEL, PREMIUM, FULL, UNAVAILABLE, OFFLINE, FAILED}

    private static volatile MushroomPassService instance;
    private final Supplier<MushroomPassSeason.Config> config;
    private final Supplier<AccountLedger> ledger;
    private final Supplier<LocalDateTime> clock;

    public MushroomPassService(Supplier<MushroomPassSeason.Config> config, Supplier<AccountLedger> ledger,
                               Supplier<LocalDateTime> clock) {
        this.config = Objects.requireNonNull(config, "config");
        this.ledger = Objects.requireNonNull(ledger, "ledger");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public static MushroomPassService get() {
        MushroomPassService s = instance;
        if (s == null) {
            synchronized (MushroomPassService.class) {
                s = instance;
                if (s == null) {
                    HotJsonFile<MushroomPassSeason.Config> file = new HotJsonFile<>(FILE, MushroomPassSeason.Config::parse,
                            MushroomPassSeason.Config::disabled, 2000);
                    s = new MushroomPassService(file::get, AccountLedger::get,
                            () -> LocalDateTime.now(MushroomPassSeason.ZONE));
                    instance = s;
                }
            }
        }
        return s;
    }

    public static void install(MushroomPassService service) {
        instance = service;
    }

    public MushroomPassSeason activeSeason() {
        return config.get().active(clock.get());
    }

    /** Opens the window; false (with a chat line) when no season runs. */
    public boolean open(Char chr) {
        MushroomPassSeason season = activeSeason();
        if (chr == null) {
            return false;
        }
        if (season == null) {
            chr.chatMessage(ChatType.SystemNotice, "目前沒有進行中的菇菇通行證。");
            return false;
        }
        syncMarks(chr, season);
        chr.write(MushroomPassPackets.seasons(config.get().seasons()));
        chr.write(MushroomPassPackets.openWindow());
        return true;
    }

    /** Writes QR-ex "b&lt;idx&gt;=1" for every index this account already claimed in {@code season}. */
    void syncMarks(Char chr, MushroomPassSeason season) {
        String prefix = season.id() + ":b";
        Map<String, String> claimed = ledger.get().receipts(chr, LEDGER_QUEST, prefix);
        for (String key : claimed.keySet()) {
            int idx;
            try {
                idx = Integer.parseInt(key.substring(prefix.length()));
            } catch (NumberFormatException e) {
                continue;
            }
            mark(chr, season, idx);
        }
    }

    private static void mark(Char chr, MushroomPassSeason season, int idx) {
        if (!"1".equals(chr.getQRValueByKey(season.claimQuest(), "b" + idx))) {
            chr.setQRValueByKey(season.claimQuest(), "b" + idx, "1");
        }
    }

    public ClaimResult claim(Char chr, MushroomPassPackets.ClaimRequest request) {
        ClaimResult r = decide(chr, request);
        String text = switch (r) {
            case GRANTED -> "菇菇通行證獎勵已發放。";
            case ALREADY -> "這個獎勵本帳號已經領取過了。";
            case NO_SEASON -> "目前沒有進行中的菇菇通行證。";
            case BAD_INDEX -> null;
            case LEVEL -> "等級不足，還不能領取這個獎勵。";
            case PREMIUM -> "本伺服器沒有販售高級通行證，只能領取一般獎勵。";
            case FULL -> "背包空間不足，請整理後再領取。";
            case UNAVAILABLE -> "這個獎勵目前無法發放，請通知管理員。";
            case OFFLINE, FAILED -> "目前無法領取，請稍後再試。";
        };
        if (chr != null && text != null) {
            chr.chatMessage(ChatType.SystemNotice, text);
        }
        return r;
    }

    private ClaimResult decide(Char chr, MushroomPassPackets.ClaimRequest request) {
        if (chr == null) {
            return ClaimResult.OFFLINE;
        }
        MushroomPassSeason season = activeSeason();
        if (season == null) {
            return ClaimResult.NO_SEASON;
        }
        int idx = request.index();
        if (idx < 0 || idx >= season.levels().size()) {
            return ClaimResult.BAD_INDEX;
        }
        MushroomPassSeason.Level level = season.levels().get(idx);
        if (!request.basic()) {
            return request.premium() ? ClaimResult.PREMIUM : ClaimResult.BAD_INDEX;
        }
        if (chr.getLevel() < level.level()) {
            return ClaimResult.LEVEL;
        }
        if (level.basic().isEmpty()) {
            return ClaimResult.BAD_INDEX;
        }
        AccountLedger.Outcome o = ledger.get().claim(chr, LEDGER_QUEST, season.id() + ":b" + idx,
                "mushroompass level=" + level.level(), level.basic(), null, null);
        ClaimResult r = switch (o.result()) {
            case GRANTED -> ClaimResult.GRANTED;
            case ALREADY -> ClaimResult.ALREADY;
            case FULL -> ClaimResult.FULL;
            case UNAVAILABLE -> ClaimResult.UNAVAILABLE;
            case OFFLINE -> ClaimResult.OFFLINE;
            case DENIED, FAILED -> ClaimResult.FAILED;
        };
        if (r == ClaimResult.GRANTED || r == ClaimResult.ALREADY) {
            mark(chr, season, idx);
        }
        return r;
    }
}
