package net.swordie.ms.world.tms282.dojo;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.util.DataPrinter;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

/**
 * Weekly Mu Lung Dojo ranking: records each character's best run per week (stage desc, time asc), ranks them
 * overall and per job line, and settles finished weeks into {@code zsf_rank_reward} rows claimed through
 * {@link DojoRewardService}. Settlement is lazy: the first ranking access after the Monday 00:00 rollover
 * ({@link WeekKeys}) settles every finished, unsettled week; {@code dojo_week_settlement} guarantees once-only.
 */
public final class DojoRankService {
    private static final DojoRankService INSTANCE = new DojoRankService();

    private volatile DojoRewardConfig config;
    private volatile int checkedWeek;

    private DojoRankService() {
    }

    public static DojoRankService get() {
        return INSTANCE;
    }

    public DojoRewardConfig config() {
        DojoRewardConfig c = config;
        if (c == null) {
            synchronized (this) {
                if (config == null) config = DojoRewardConfig.load();
                c = config;
            }
        }
        return c;
    }

    public synchronized void reloadConfig() {
        config = DojoRewardConfig.load();
    }

    /** Records a cleared floor; only improves the character's record of the current week. */
    public void recordClear(Char chr, int stage, int timeSec) {
        if (chr == null || stage <= 0 || stage > DojoFloorService.get().finalFloor()) return;
        try {
            ensureSettled();
            DojoRecord r = new DojoRecord(chr.getId(), WeekKeys.current(), stage, Math.max(0, timeSec),
                    chr.getJob(), chr.getLevel(), chr.getName());
            DojoRecordStore.submit(r);
        } catch (SQLException | RuntimeException e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
        }
    }

    /** Ranked records of a week (overall order, with job-line positions). */
    public List<DojoRankEntry> ranking(int weekKey) throws SQLException {
        List<DojoRecord> records = DojoRecordStore.week(weekKey);
        List<DojoRankEntry> result = new ArrayList<>(records.size());
        Map<Integer, Integer> jobPositions = new HashMap<>();
        int rank = 0;
        for (DojoRecord r : records) {
            int jobRank = jobPositions.merge(r.jobLine(), 1, Integer::sum);
            result.add(new DojoRankEntry(r, ++rank, jobRank));
        }
        return result;
    }

    public List<DojoRankEntry> currentRanking() throws SQLException {
        ensureSettled();
        return ranking(WeekKeys.current());
    }

    public List<DojoRankEntry> previousRanking() throws SQLException {
        ensureSettled();
        return ranking(WeekKeys.previous(WeekKeys.current()));
    }

    public Optional<DojoRankEntry> find(List<DojoRankEntry> ranking, int charId) {
        return ranking.stream().filter(e -> e.record().charId() == charId).findFirst();
    }

    /** Settles every finished week that has not been settled yet. Cheap after the first call of a week. */
    public void ensureSettled() throws SQLException {
        int current = WeekKeys.current();
        if (checkedWeek == current) return;
        synchronized (this) {
            if (checkedWeek == current) return;
            for (int week : DojoRecordStore.unsettledWeeks(current)) {
                settle(week);
            }
            checkedWeek = current;
        }
    }

    /**
     * Settles one finished week.
     *
     * @return inserted reward rows, or -1 if the week was already settled / not finished yet
     */
    public synchronized int settle(int weekKey) throws SQLException {
        if (weekKey >= WeekKeys.current()) return -1;
        DojoRewardConfig cfg = config();
        List<int[]> rows = new ArrayList<>();
        for (DojoRankEntry e : ranking(weekKey)) {
            for (int code : cfg.codesFor(e)) {
                rows.add(new int[]{e.record().charId(), code});
            }
        }
        Timestamp time = Timestamp.valueOf(WeekKeys.date(weekKey).atStartOfDay());
        int inserted = DojoRecordStore.settle(weekKey, rows, time);
        if (inserted >= 0) {
            DataPrinter.send(DataPrinter.SCRIPTS, String.format("道場週排行結算完成：週 %d，發放 %d 筆獎勵。", weekKey, inserted), true);
        }
        return inserted;
    }

    /** Admin entry: settles last week now (no-op if already settled). Returns a Traditional Chinese summary. */
    public String settlePreviousWeek() {
        int week = WeekKeys.previous(WeekKeys.current());
        try {
            int inserted = settle(week);
            ensureSettled();
            return inserted < 0
                    ? String.format("上週（%s 起）道場獎勵已結算過，未重複發放。", WeekKeys.date(week))
                    : String.format("上週（%s 起）道場獎勵結算完成，共建立 %d 筆獎勵。", WeekKeys.date(week), inserted);
        } catch (SQLException | RuntimeException e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
            return "道場獎勵結算失敗，請查看伺服器日誌。";
        }
    }

    public boolean isSettled(int weekKey) throws SQLException {
        return DojoRecordStore.isSettled(weekKey);
    }
}
