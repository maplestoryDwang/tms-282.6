package net.swordie.ms.world.tms282.contents.dimensiontower;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.enums.ChatType;
import net.swordie.ms.world.field.Field;
import net.swordie.ms.world.field.Portal;
import net.swordie.ms.world.tms282.contents.common.AccountLedger;
import net.swordie.ms.world.tms282.contents.common.HotJsonFile;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * 次元之塔 (content-1005 feature 3): a timed climb of the 282 maps 993267000 (Stage 1) and 993267001 (Stage 2).
 * <ul>
 *     <li>{@link #enter} (NPC 9063722 可疑的次元門 / quest 102399): season running, level, then a new run and a warp to
 *     the Stage 1 start;</li>
 *     <li>{@link #stage1Clear} (collision portal {@code psh_stage1Clear}, script gotoStage2): the run's Stage 1 split,
 *     medal 1143396 once per character, warp to Stage 2;</li>
 *     <li>{@link #stage2Clear} (collision portals {@code psh_record_8/9}, script pt_toTirnog): total time stored in
 *     {@link TowerRecords} (best kept), medal 1143449 once per character, warp to the goal platform
 *     ({@code ph_tirnog_clear}, NPC 漢斯 / 排行佈告欄);</li>
 *     <li>{@link #ranking} / {@link #claimRankReward} (NPC 9063723 排行佈告欄): top 10, own rank; after the season,
 *     the rank tier's rewards once per character.</li>
 * </ul>
 * A clear only counts in its map, above the portal's height (the portal scripts can only be reached by climbing),
 * and for a run started by {@link #enter}; the run lives in memory (a restart or a relog ends it).
 */
public final class DimensionTowerService {
    public static final int STAGE1 = 993267000, STAGE2 = 993267001, CAMPUS = 993267500;
    /** Highest y of the Stage 1 clear portal area (-9394) and of the Stage 2 record portals (-12071 / -12051), with slack. */
    public static final int STAGE1_GOAL_Y = -8800, STAGE2_GOAL_Y = -11500;
    public static final String GOAL_PORTAL = "ph_tirnog_clear";
    /** account_questex.questid for the medal / rank-reward receipts (the season quest 102399). */
    public static final int LEDGER_QUEST = 102399;
    public static final Path FILE = Path.of("data/tms282-dimensiontower/tower.json");

    public enum Result {OK, NO_SEASON, LEVEL, NOT_RUNNING, WRONG_PLACE, FAILED}

    /** Moves a character (the server warps; tests record). */
    public interface Mover {
        void move(Char chr, int fieldId, String portalName);
    }

    public record Run(String season, long startedAt, long stage1Millis) {
    }

    private static volatile DimensionTowerService instance;
    private final Supplier<TowerSeason.Config> config;
    private final Supplier<AccountLedger> ledger;
    private final TowerRecords records;
    private final Supplier<LocalDateTime> clock;
    private final LongSupplier millis;
    private final Mover mover;
    private final Map<Integer, Run> runs = new ConcurrentHashMap<>();

    public DimensionTowerService(Supplier<TowerSeason.Config> config, Supplier<AccountLedger> ledger, TowerRecords records,
                                 Supplier<LocalDateTime> clock, LongSupplier millis, Mover mover) {
        this.config = Objects.requireNonNull(config);
        this.ledger = Objects.requireNonNull(ledger);
        this.records = Objects.requireNonNull(records);
        this.clock = Objects.requireNonNull(clock);
        this.millis = Objects.requireNonNull(millis);
        this.mover = Objects.requireNonNull(mover);
    }

    public static DimensionTowerService get() {
        DimensionTowerService s = instance;
        if (s == null) {
            synchronized (DimensionTowerService.class) {
                s = instance;
                if (s == null) {
                    HotJsonFile<TowerSeason.Config> file = new HotJsonFile<>(FILE, TowerSeason.Config::parse,
                            TowerSeason.Config::disabled, 2000);
                    s = new DimensionTowerService(file::get, AccountLedger::get, new TowerRecords.Sql(),
                            () -> LocalDateTime.now(TowerSeason.ZONE), System::currentTimeMillis, DimensionTowerService::warp);
                    instance = s;
                }
            }
        }
        return s;
    }

    public static void install(DimensionTowerService service) {
        instance = service;
    }

    static void warp(Char chr, int fieldId, String portalName) {
        Field field = chr.getOrCreateFieldByCurrentInstanceType(fieldId);
        if (field == null) {
            return;
        }
        Portal portal = portalName == null ? null : field.getPortalByName(portalName);
        chr.warp(field, portal == null ? field.getDefaultPortal() : portal);
    }

    /** The season to show (running, or in its claim window), or null. */
    public TowerSeason season() {
        return config.get().current(clock.get());
    }

    public boolean running() {
        TowerSeason s = season();
        return s != null && s.running(clock.get());
    }

    public Run run(Char chr) {
        return chr == null ? null : runs.get(chr.getId());
    }

    public static String format(long ms) {
        long s = ms / 1000;
        return String.format("%d:%02d.%02d", s / 60, s % 60, (ms % 1000) / 10);
    }

    public Result enter(Char chr) {
        TowerSeason s = season();
        if (chr == null) {
            return Result.FAILED;
        }
        if (s == null || !s.running(clock.get())) {
            return Result.NO_SEASON;
        }
        if (chr.getLevel() < s.minLevel()) {
            return Result.LEVEL;
        }
        runs.put(chr.getId(), new Run(s.id(), millis.getAsLong(), -1));
        mover.move(chr, STAGE1, null);
        chr.chatMessage(ChatType.SystemNotice, "次元之塔計時開始！爬到最上面就能前往 Stage 2。");
        return Result.OK;
    }

    private static boolean at(Char chr, int fieldId, int maxY) {
        return chr.getField() != null && chr.getField().getId() == fieldId && chr.getPosition() != null
                && chr.getPosition().getY() <= maxY;
    }

    public Result stage1Clear(Char chr) {
        if (chr == null || !at(chr, STAGE1, STAGE1_GOAL_Y)) {
            return Result.WRONG_PLACE;
        }
        TowerSeason s = season();
        if (s == null || !s.running(clock.get())) {
            runs.remove(chr.getId());
            chr.chatMessage(ChatType.SystemNotice, "次元之塔活動已經結束。");
            mover.move(chr, CAMPUS, null);
            return Result.NO_SEASON;
        }
        medal(chr, s, s.stage1Medal(), "m1");
        Run r = runs.get(chr.getId());
        Result result;
        if (r == null || !r.season().equals(s.id()) || r.stage1Millis() >= 0) {
            runs.remove(chr.getId());
            chr.chatMessage(ChatType.SystemNotice, "Stage 1 通過！這次沒有計時（請從可疑的次元門進入才會計時）。");
            result = Result.NOT_RUNNING;
        } else {
            long split = millis.getAsLong() - r.startedAt();
            runs.put(chr.getId(), new Run(r.season(), r.startedAt(), split));
            chr.chatMessage(ChatType.SystemNotice, "Stage 1 通過！用時 " + format(split) + "，繼續挑戰 Stage 2。");
            result = Result.OK;
        }
        mover.move(chr, STAGE2, null);
        return result;
    }

    public Result stage2Clear(Char chr) {
        if (chr == null || !at(chr, STAGE2, STAGE2_GOAL_Y)) {
            return Result.WRONG_PLACE;
        }
        Run r = runs.remove(chr.getId());
        TowerSeason s = season();
        if (s == null || !s.running(clock.get())) {
            chr.chatMessage(ChatType.SystemNotice, "次元之塔活動已經結束。");
            mover.move(chr, CAMPUS, null);
            return Result.NO_SEASON;
        }
        medal(chr, s, s.stage2Medal(), "m2");
        if (r == null || !r.season().equals(s.id()) || r.stage1Millis() < 0) {
            chr.chatMessage(ChatType.SystemNotice, "Stage 2 通過！這次沒有計時（請從可疑的次元門進入才會計時）。");
            mover.move(chr, STAGE2, GOAL_PORTAL);
            return Result.NOT_RUNNING;
        }
        long total = millis.getAsLong() - r.startedAt();
        TowerRecords.Entry best;
        try {
            best = records.record(s.id(), chr.getId(), chr.getAccount() == null ? 0 : chr.getAccount().getId(),
                    chr.getName(), total, r.stage1Millis(), millis.getAsLong());
        } catch (Exception e) {
            System.out.println("[DimensionTower] record failed character=" + chr.getId() + " error=" + e);
            chr.chatMessage(ChatType.SystemNotice, "Stage 2 通過！用時 " + format(total) + "（紀錄暫時無法儲存，請通知管理員）。");
            mover.move(chr, STAGE2, GOAL_PORTAL);
            return Result.FAILED;
        }
        boolean newBest = best != null && best.bestMillis() == total;
        chr.chatMessage(ChatType.SystemNotice, "Stage 2 通過！總用時 " + format(total)
                + (newBest ? "（個人最佳紀錄）" : "，個人最佳 " + (best == null ? "-" : format(best.bestMillis()))) + "。");
        mover.move(chr, STAGE2, GOAL_PORTAL);
        return Result.OK;
    }

    private void medal(Char chr, TowerSeason s, int medal, String tag) {
        if (medal <= 0) {
            return;
        }
        AccountLedger.Outcome o = ledger.get().claim(chr, LEDGER_QUEST, s.id() + ":" + tag + ":" + chr.getId(),
                "dimension tower medal", List.of(new AccountLedger.Reward(medal, 1)), null, null);
        if (o.result() == AccountLedger.Result.FULL) {
            chr.chatMessage(ChatType.SystemNotice, "裝備欄已滿，勳章沒有發放；整理後再通過一次即可領取。");
        }
    }

    /** Lines for the ranking board: own best / rank, then the top 10. */
    public List<String> ranking(Char chr) {
        List<String> out = new ArrayList<>();
        TowerSeason s = season();
        if (s == null) {
            out.add("目前沒有次元之塔的排行。");
            return out;
        }
        try {
            TowerRecords.Entry me = chr == null ? null : records.find(s.id(), chr.getId());
            out.add(me == null ? "你在這一季還沒有完成紀錄。"
                    : "你的最佳紀錄：" + format(me.bestMillis()) + "（第 " + records.rank(s.id(), chr.getId()) + " 名，完成 "
                    + me.clears() + " 次）");
            List<TowerRecords.Entry> top = records.top(s.id(), 10);
            if (top.isEmpty()) {
                out.add("目前還沒有人完成。");
            }
            for (int i = 0; i < top.size(); i++) {
                out.add((i + 1) + ". " + top.get(i).name() + "  " + format(top.get(i).bestMillis()));
            }
        } catch (Exception e) {
            out.add("排行暫時無法讀取。");
        }
        return out;
    }

    public enum ClaimResult {GRANTED, ALREADY, NOT_YET, CLOSED, NO_RANK, FULL, FAILED}

    public ClaimResult claimRankReward(Char chr) {
        TowerSeason s = season();
        LocalDateTime now = clock.get();
        if (chr == null || s == null) {
            return ClaimResult.CLOSED;
        }
        if (s.running(now)) {
            return ClaimResult.NOT_YET;
        }
        if (!s.claimable(now)) {
            return ClaimResult.CLOSED;
        }
        int rank;
        try {
            rank = records.rank(s.id(), chr.getId());
        } catch (Exception e) {
            return ClaimResult.FAILED;
        }
        TowerSeason.Tier tier = s.tierOf(rank);
        if (tier == null || tier.rewards().isEmpty()) {
            return ClaimResult.NO_RANK;
        }
        AccountLedger.Outcome o = ledger.get().claim(chr, LEDGER_QUEST, s.id() + ":rank:" + chr.getId(),
                "dimension tower rank=" + rank, tier.rewards(), null, null);
        return switch (o.result()) {
            case GRANTED -> ClaimResult.GRANTED;
            case ALREADY -> ClaimResult.ALREADY;
            case FULL -> ClaimResult.FULL;
            default -> ClaimResult.FAILED;
        };
    }

    /** Ends the in-memory run (leaving the tower through an NPC). */
    public void abandon(Char chr) {
        if (chr != null) {
            runs.remove(chr.getId());
        }
    }

    public void leave(Char chr) {
        abandon(chr);
        mover.move(chr, CAMPUS, null);
    }
}
