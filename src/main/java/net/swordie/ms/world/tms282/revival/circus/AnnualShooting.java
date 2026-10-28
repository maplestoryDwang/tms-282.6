package net.swordie.ms.world.tms282.revival.circus;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.world.tms282.activity.ActivityRewards;
import net.swordie.ms.world.tms282.revival.RevivalEvents;

import java.time.LocalDate;

/**
 * 星夜馬戲團 動態視力修煉場 (event/revival_annual_shooting.js): the first run of a day pays a score tier once per
 * account (receipt {@code rv_circus:<date>:shoot}); the coins are the circus hub currencies.
 */
public final class AnnualShooting {
    public static final int FIELD = 867168300, EXIT = 867168000, NPC = 9402009;
    public static final int FANTASY = 4310331, DREAM = 4310297;
    /** Built-in tiers, best first; live values: revival-rewards.json "shooting.tiers". */
    public static final java.util.List<net.swordie.ms.world.tms282.revival.rewards.RevivalRewards.ShootingTier> DEFAULT_TIERS =
            java.util.List.of(st(35, 60, 5), st(25, 40, 0), st(10, 20, 0), st(0, 5, 0));

    private static net.swordie.ms.world.tms282.revival.rewards.RevivalRewards.ShootingTier st(int min, int fantasy, int dream) {
        return new net.swordie.ms.world.tms282.revival.rewards.RevivalRewards.ShootingTier(min, fantasy, dream);
    }

    private AnnualShooting() {
    }

    /** {min score, fantasy, dream} of the score, from the current rewards file. */
    public static int[] tier(int score) {
        var tiers = net.swordie.ms.world.tms282.revival.rewards.RevivalRewardsSource.current().shootingTiers();
        var pick = tiers.get(tiers.size() - 1);
        for (var t : tiers) if (score >= t.minScore()) { pick = t; break; }
        return new int[]{pick.minScore(), pick.fantasy(), pick.dream()};
    }

    public interface Granter {
        ActivityRewards.Result grant(String receiptKey, int[] ids, int[] qty);
    }

    public static String record(Granter granter, int score, LocalDate day) {
        int s = Math.max(0, score);
        int[] t = tier(s);
        int[] ids = t[2] > 0 ? new int[]{FANTASY, DREAM} : new int[]{FANTASY};
        int[] qty = t[2] > 0 ? new int[]{t[1], t[2]} : new int[]{t[1]};
        StringBuilder out = new StringBuilder("【動態視力修煉場】命中蟠猴標靶 ").append(s).append(" 個。");
        ActivityRewards.Result r = granter.grant("rv_circus:" + day + ":shoot", ids, qty);
        out.append("\r\n").append(switch (r) {
            case GRANTED -> "獲得幻想硬幣 " + t[1] + " 個" + (t[2] > 0 ? "、夢想硬幣 " + t[2] + " 個" : "") + "（今日挑戰獎勵）。";
            case ALREADY -> "本帳號今天已領取過修煉場獎勵。";
            case FULL -> "背包空間不足，今天的獎勵沒有發放，整理後再挑戰一次即可領取。";
            default -> "今天的獎勵暫時無法發放。";
        });
        return out.toString();
    }

    public static String record(Char chr, int score) {
        return record((k, ids, q) -> ActivityRewards.grantOnce(chr, k, ids, q), score, RevivalEvents.today());
    }
}
