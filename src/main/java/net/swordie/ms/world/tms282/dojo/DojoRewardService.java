package net.swordie.ms.world.tms282.dojo;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.util.DataPrinter;

import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/** Lists and claims settled dojo weekly rewards ({@code zsf_rank_reward} rows of type 'dojang'). */
public final class DojoRewardService {
    private static final DojoRewardService INSTANCE = new DojoRewardService();

    /** One claimable row as shown in the claim menu. */
    public record Claim(int id, int code, LocalDate period, String menuText, String label, List<DojoRewardConfig.RewardItem> items) {
    }

    public record Result(boolean success, String message) {
    }

    private DojoRewardService() {
    }

    public static DojoRewardService get() {
        return INSTANCE;
    }

    public List<Claim> claims(Char chr) {
        List<Claim> result = new ArrayList<>();
        try {
            DojoRankService.get().ensureSettled();
            DojoRewardConfig cfg = DojoRankService.get().config();
            for (long[] row : DojoRecordStore.unclaimed(chr.getId())) {
                int code = (int) row[1];
                DojoRewardConfig.Reward reward = cfg.reward(code);
                LocalDate period = Instant.ofEpochMilli(row[2]).atZone(ZoneId.systemDefault()).toLocalDate();
                if (reward == null) {
                    result.add(new Claim((int) row[0], code, period, "未知獎勵代碼 " + code, "", List.of()));
                } else {
                    result.add(new Claim((int) row[0], code, period, reward.menuText(), reward.label(), reward.items()));
                }
            }
        } catch (SQLException | RuntimeException e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
        }
        return result;
    }

    public Result claim(Char chr, int rowId) {
        Claim claim = claims(chr).stream().filter(c -> c.id() == rowId).findFirst().orElse(null);
        if (claim == null) return new Result(false, "此獎勵不存在或已領取。");
        if (claim.items().isEmpty()) return new Result(false, "此獎勵設定已失效，請聯繫管理員。");
        synchronized (net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime.ECONOMY_MONITOR) { synchronized (chr) { // multiplayer-0930 lock order
            for (DojoRewardConfig.RewardItem item : claim.items()) {
                if (!chr.canHold(item.id(), item.count())) {
                    return new Result(false, "背包欄位空間不足，請檢查。");
                }
            }
            try {
                if (!DojoRecordStore.markClaimed(rowId, chr.getId())) {
                    return new Result(false, "此獎勵不存在或已領取。");
                }
            } catch (SQLException e) {
                DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
                return new Result(false, "發生未知錯誤，請重試。");
            }
            StringBuilder text = new StringBuilder("領取成功\r\n");
            try {
                for (DojoRewardConfig.RewardItem item : claim.items()) {
                    chr.addItemToInventory(item.id(), item.count());
                    text.append("#v").append(item.id()).append("# x ").append(item.count()).append(" \r\n");
                }
            } catch (RuntimeException e) {
                DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
                return new Result(false, "發放獎勵時發生錯誤，請聯繫管理員。");
            }
            String period = String.format("%02d-%02d", claim.period().getMonthValue(), claim.period().getDayOfMonth());
            chr.getScriptManager().broadcastWorldChat(
                    String.format("玩家<%s>領取了<%s>期 %s的獎勵", chr.getName(), period, claim.label()));
            return new Result(true, text.toString());
        } }
    }
}
