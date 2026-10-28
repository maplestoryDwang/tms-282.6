package net.swordie.ms.client.character.social.tms282;

import net.swordie.ms.client.character.items.Item;

/**
 * Pair-ring families of TMS 282.3 (Character.wz 1112xxx), classified by item id from the 282 String.wz names
 * (research/tms2823/data282-import/snap/282-string.tsv). TMS reused several GMS friendship/wedding ids (1112803,
 * 1112805..1112809: chat / name-tag rings in TMS) and 1112000 / 1112018..1112022 are not pair rings.
 */
public enum RingKind {
    /** 戀人戒指 family: 1112001..1112016, 1112023..1112026. */
    COUPLE,
    /** 友情戒指 family: 1112800..1112802, 1112810..1112812, 1112816, 1112817, 1112820, 1112822, 1112823, 1112827..1112839. */
    FRIENDSHIP,
    /** 結婚戒指: 1112300..1112319, 1112804. */
    MARRIAGE;

    /** The family of {@code itemId}, or null when it is not a pair ring. */
    public static RingKind of(int itemId) {
        if ((itemId >= 1112001 && itemId <= 1112016) || (itemId >= 1112023 && itemId <= 1112026)) {
            return COUPLE;
        }
        if ((itemId >= 1112300 && itemId <= 1112319) || itemId == 1112804) {
            return MARRIAGE;
        }
        if ((itemId >= 1112800 && itemId <= 1112802) || (itemId >= 1112810 && itemId <= 1112812)
                || itemId == 1112816 || itemId == 1112817 || itemId == 1112820 || itemId == 1112822
                || itemId == 1112823 || (itemId >= 1112827 && itemId <= 1112839)) {
            return FRIENDSHIP;
        }
        return null;
    }

    public static boolean isPairRing(int itemId) {
        return of(itemId) != null;
    }

    /**
     * The serial a record stores for {@code ring}: its cash serial number, or its item database id for a ring that
     * has none (rings given by scripts / GM commands). The same rule is used when matching worn rings.
     */
    public static long serialOf(Item ring) {
        long sn = ring.getCashItemSerialNumber();
        return sn != 0 ? sn : ring.getId();
    }

    /** Traditional Chinese name for player messages. */
    public String displayName() {
        return switch (this) {
            case COUPLE -> "戀人戒指";
            case FRIENDSHIP -> "友情戒指";
            case MARRIAGE -> "結婚戒指";
        };
    }
}
