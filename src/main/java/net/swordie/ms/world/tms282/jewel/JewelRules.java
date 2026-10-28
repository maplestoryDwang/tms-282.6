package net.swordie.ms.world.tms282.jewel;

import java.util.Map;
import java.util.function.IntUnaryOperator;

/**
 * Pure jewel rules (no DB, no Char) so they can be checked offline. {@code roll.applyAsInt(bound)} must return a
 * uniform value in [0, bound).
 */
public final class JewelRules {
    private JewelRules() { }

    public enum EngraveOutcome { UPGRADE, DOWNGRADE, DESTROY }

    public record Engrave(EngraveOutcome outcome, JewelState after) { }

    public record Stats(int allStat, int engraveBonus) { }

    public static JewelConfig.EngraveStep nextEngrave(JewelConfig cfg, JewelState s) {
        if (s.engraveLevel() >= cfg.maxEngrave()) return null;
        return cfg.engrave().get(s.engraveLevel() + 1);
    }

    public static JewelConfig.AwakenStep nextAwaken(JewelConfig cfg, JewelState s) {
        if (s.awakenLevel() >= cfg.maxAwaken()) return null;
        return cfg.awaken().get(s.awakenLevel() + 1);
    }

    public static Engrave engrave(JewelConfig cfg, JewelState s, IntUnaryOperator roll) {
        JewelConfig.EngraveStep step = nextEngrave(cfg, s);
        if (step == null) throw new IllegalStateException("engrave already max");
        int r = roll.applyAsInt(100);
        if (r < step.upgrade()) return new Engrave(EngraveOutcome.UPGRADE, s.withEngrave(s.engraveLevel() + 1));
        if (r < step.upgrade() + step.downgrade())
            return new Engrave(EngraveOutcome.DOWNGRADE, s.withEngrave(Math.max(0, s.engraveLevel() - 1)));
        return new Engrave(EngraveOutcome.DESTROY, s.destroyed());
    }

    public static JewelState awaken(JewelConfig cfg, JewelState s) {
        if (nextAwaken(cfg, s) == null) throw new IllegalStateException("awaken already max");
        return s.withAwaken(s.awakenLevel() + 1);
    }

    /** Rolls a polish grade (25/50/75/100) for awaken slot {@code slot} with that level's weights. */
    public static int rollPolish(JewelConfig cfg, int slot, IntUnaryOperator roll) {
        JewelConfig.AwakenStep step = cfg.awaken().get(slot);
        int r = roll.applyAsInt(step.totalWeight());
        int acc = 0;
        int last = 0;
        for (Map.Entry<Integer, Integer> e : step.polishWeights().entrySet()) {
            acc += e.getValue();
            last = e.getKey();
            if (r < acc) return e.getKey();
        }
        return last;
    }

    /**
     * ASSUMPTION (not confirmed against the 282 client): effective all-stat = maxStat(tier) * sum(polish grades)
     * / (100 * maxAwaken); engrave bonus = engraveLevel * engraveBonus(tier). Not applied to character stats.
     */
    public static Stats stats(JewelConfig cfg, JewelState s) {
        JewelConfig.Tier t = cfg.tier(s.tier());
        long sum = 0;
        for (int i = 1; i <= s.awakenLevel(); i++) sum += s.polishAt(i);
        int allStat = (int) (t.maxStat() * sum / (100L * cfg.maxAwaken()));
        return new Stats(allStat, s.engraveLevel() * t.engraveBonus());
    }
}
