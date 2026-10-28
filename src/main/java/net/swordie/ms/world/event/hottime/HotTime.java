package net.swordie.ms.world.event.hottime;

/**
 * HotTime 目前生效中的倍率。未生效時皆為 1.0。
 * 由 {@link HotTimeScheduler} 每分鐘更新；Mob 經驗／掉寶／楓幣計算時乘上即可，不會改動 RateSettings。
 */
public final class HotTime {

    private static volatile double expMultiplier = 1.0;
    private static volatile double dropMultiplier = 1.0;
    private static volatile double mesoMultiplier = 1.0;

    private HotTime() {
    }

    public static double currentExpMultiplier() {
        return expMultiplier;
    }

    public static double dropMultiplier() {
        return dropMultiplier;
    }

    public static double mesoMultiplier() {
        return mesoMultiplier;
    }

    public static boolean isActive() {
        return expMultiplier != 1.0 || dropMultiplier != 1.0 || mesoMultiplier != 1.0;
    }

    static void set(double exp, double drop, double meso) {
        expMultiplier = sanitize(exp);
        dropMultiplier = sanitize(drop);
        mesoMultiplier = sanitize(meso);
    }

    private static double sanitize(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v) || v <= 0) {
            return 1.0;
        }
        return Math.min(v, 100.0);
    }
}
