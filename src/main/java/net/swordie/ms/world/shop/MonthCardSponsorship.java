package net.swordie.ms.world.shop;

import net.swordie.ms.util.HotProperties;

import java.util.Locale;

/**
 * 月卡與累積贊助（累充，hypay.payReward）的計算規則（leftovers-1005 任務 6）。
 * <p>
 * {@code config/server.properties} 的 {@code sponsor.monthCard.accumulate}（熱載入 3 秒，{@code -D} 優先）：
 * <ul>
 *   <li>{@code purchase}（預設，保守）：辦理月卡時，以「購買價格」計入累充一次；之後 30 天每天領取的月卡獎勵
 *       一律不再計入（{@link #dailyClaimCredit} 永遠是 0）。</li>
 *   <li>{@code none}：月卡完全不計入累充（購買與每日領取都不算）。</li>
 * </ul>
 * 其他值（打錯字）一律當作預設 {@code purchase} 並記錄一次。沒有「每日領取也計入」的選項：那會讓同一筆錢被算 30 次。
 * 腳本：{@code Java.type('net.swordie.ms.world.shop.MonthCardSponsorship').purchaseCredit(價格)}。
 */
public final class MonthCardSponsorship {
    public static final String KEY = "sponsor.monthCard.accumulate";

    public enum Mode { PURCHASE, NONE }

    private static volatile String warned = "";

    private MonthCardSponsorship() {
    }

    public static Mode mode() {
        return mode(HotProperties.server().raw(KEY));
    }

    static Mode mode(String raw) {
        if (raw == null || raw.isBlank()) {
            return Mode.PURCHASE;
        }
        String v = raw.trim().toLowerCase(Locale.ROOT);
        switch (v) {
            case "purchase":
            case "once":
                return Mode.PURCHASE;
            case "none":
            case "off":
            case "false":
                return Mode.NONE;
            default:
                if (!v.equals(warned)) {
                    warned = v;
                    System.out.println("[MonthCard] " + KEY + "=" + raw.trim() + " is not purchase/none; using purchase");
                }
                return Mode.PURCHASE;
        }
    }

    /** 辦理月卡時要加進累充的金額：purchase = 價格（一次），none = 0。價格 <= 0 一律 0。 */
    public static int purchaseCredit(int price) {
        return purchaseCredit(mode(), price);
    }

    public static int purchaseCredit(Mode mode, int price) {
        return mode == Mode.PURCHASE && price > 0 ? price : 0;
    }

    /** 每日領取月卡獎勵時要加進累充的金額：任何模式都是 0（月卡只在購買時計入一次）。 */
    public static int dailyClaimCredit(int amount) {
        return 0;
    }
}
