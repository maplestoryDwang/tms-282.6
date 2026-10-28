package net.swordie.ms.world.tms282.bossrun;

import net.swordie.ms.enums.BossPartyDifficultyType;
import net.swordie.ms.enums.BossPartyType;

/** Traditional Chinese boss / difficulty labels for player text. */
public final class BossRunNames {
    private BossRunNames() {
    }

    public static String boss(int orderId, String fallback) {
        return switch (orderId) {
            case 0 -> "巴洛古";
            case 1 -> "殘暴炎魔";
            case 2 -> "闇黑龍王";
            case 3 -> "希拉";
            case 4 -> "比艾樂";
            case 5 -> "斑斑";
            case 6 -> "血腥皇后";
            case 7 -> "貝倫";
            case 8 -> "凡雷恩";
            case 9 -> "阿卡伊農";
            case 10 -> "梅格耐斯";
            case 11 -> "皮卡啾";
            case 12 -> "西格諾斯";
            case 13 -> "史烏";
            case 14 -> "烏勒斯";
            case 15 -> "戴米安";
            case 16 -> "培羅德";
            case 17 -> "森蘭丸";
            case 18 -> "濃姬";
            case 19 -> "露希妲";
            case 21 -> "卡翁";
            case 22 -> "拉圖斯";
            case 23 -> "威爾";
            case 24 -> "真希拉";
            case 25 -> "黑魔法師";
            case 26 -> "戴斯克";
            case 27 -> "頓凱爾";
            case 28 -> "受選的賽蓮";
            case 29 -> "守護天使綠水靈";
            case 30 -> "監視者卡洛斯";
            case 31 -> "咖凌";
            case 32 -> "極限怪物公園";
            case 33 -> "林波";
            case 34 -> "巴爾德里克斯";
            case 35 -> "最初的對抗者";
            case 36 -> "凱伊";
            case 37 -> "燦爛的兇星";
            case 38 -> "尤比太";
            case 107 -> "明智光秀";
            case 110 -> "瑪麗西亞";
            default -> fallback;
        };
    }

    public static String difficulty(BossPartyDifficultyType d) {
        if (d == null) return "";
        return switch (d) {
            case Easy -> "簡單";
            case Normal -> "普通";
            case Hard -> "困難";
            case Chaos -> "混沌";
            case Extreme -> "極限";
            default -> d.getName();
        };
    }

    public static String of(BossPartyType bpt) {
        return boss(bpt.getOrderId(), bpt.getBossName()) + "[" + difficulty(bpt.getDifficulty()) + "]";
    }
}
