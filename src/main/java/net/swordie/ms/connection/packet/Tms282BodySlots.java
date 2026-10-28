package net.swordie.ms.connection.packet;

/**
 * Worn-equipment positions that differ between the 265 server (BodyPart) and the TMS 282.3 client.
 * <p>
 * The 282 CharacterData keeps worn equips in a fixed array of 39 body slots (92B9B1: pos - 1 &lt;= 0x26, entries of
 * 16 bytes at (pos + 4) * 16), so the android (265 BodyPart.Android 53) and its heart (MechanicalHeart 54) cannot be
 * there under their 265 numbers. LIVE 2026-09-27: the client equips the android at -32 and the heart at -33 (CP0141
 * "3->-32 item=1662198", "2->-33 item=1672069", character 37); the earlier 27 / 28 guess was wrong.
 * <p>
 * Positions are 265 {@code bagIndex} values: positive for worn items in SetField, negative in inventory operations
 * and CP0141. Everything else maps to itself.
 */
public final class Tms282BodySlots {
    public static final int ANDROID_265 = 53;
    public static final int HEART_265 = 54;
    public static final int ANDROID_282 = 32;
    public static final int HEART_282 = 33;

    private Tms282BodySlots() {
    }

    /** A worn 265 body part (positive, as SetField's worn list sends it) as the 282 body slot. */
    public static int wornToClient(int bodyPart) {
        return bodyPart == ANDROID_265 ? ANDROID_282 : bodyPart == HEART_265 ? HEART_282 : bodyPart;
    }

    /** A 282 body slot (positive) as the 265 body part. */
    public static int wornToServer(int bodySlot) {
        return bodySlot == ANDROID_282 ? ANDROID_265 : bodySlot == HEART_282 ? HEART_265 : bodySlot;
    }

    /**
     * An inventory-operation / CP0141 position of the equip inventories: negative = worn (mapped), positive = a bag
     * slot (unchanged: bag slots 32 / 33 are ordinary slots).
     */
    public static short toClient(short pos) {
        return pos < 0 ? (short) -wornToClient(-pos) : pos;
    }

    public static short toServer(short pos) {
        return pos < 0 ? (short) -wornToServer(-pos) : pos;
    }

    // ---- 282 item -> body-slot table (equips README WI-2; client MapleStory.exe RVA 0x79E970) -------------------------

    /** 282 non-cash worn slots of the accessories whose number differs from the 265 BodyPart. */
    public static final int MEDAL = 21, SHOULDER = 28, POCKET = 31, BADGE = 34, EMBLEM = 35, PENDANT_2 = 36, ASTRA = 37;
    /** 282 pet equip (cash 14 / 24 / 26) and pet name-tag ring (cash 23 / 25 / 27) slots as the server stores them. */
    public static final int[] PET_WEAR = {114, 124, 126};
    public static final int[] PET_NAME_TAG = {123, 125, 127};
    /** The pet name-tag ring outside the 182xxxx range (79F00C: {@code cmp r13d, 0x1B7F74}). */
    public static final int PET_NAME_TAG_1802100 = 1802100;
    /** Cash items are worn at the non-cash slot + 100 (CP0141 / SetField cash block). */
    public static final int CASH_OFFSET = 100;

    private static final int[] NONE = {};

    /**
     * The 282 client's body slots of an item, in 79E970's order (client numbers: android 32, heart 33, no cash offset).
     * The first entry is the primary slot. Empty when the 282 client has no slot for the item (e.g. 117 monster book).
     */
    public static int[] clientSlots(int itemId) {
        switch (itemId / 1000) { // symbols (default case of 79E970)
            case 1712: return range(1600, 6);
            case 1713: return range(1700, 6);
            case 1714: return range(1750, 3);
            default: break;
        }
        int prefix = itemId / 10000;
        switch (prefix) {
            case 100: return new int[]{1, 1200, 1300, 1501};
            case 101: return new int[]{2, 1202, 1302, 1502};
            case 102: return new int[]{3, 1208, 1500, 1305};
            case 103: return new int[]{4, 1209, 1503, 1306};
            case 104: case 105: return new int[]{5, 1203, 1307, 1505};
            case 106: return new int[]{6, 1204, 1308, 1508};
            case 107: return new int[]{7, 1205, 1309, 1509};
            case 108: return new int[]{8, 1206, 1304, 1506};
            case 109: case 134: case 135: case 156: return new int[]{10};
            // Astra secondary: 37 when the item is in the client's runtime set (AF7660), else 10. The set's content is
            // not static; the server places it next to the ordinary secondary (needs a CP0141 capture to confirm).
            case 172: return new int[]{10, ASTRA};
            case 110: return new int[]{9, 1201, 1301, 1504};
            case 111: return new int[]{12, 13, 15, 16, 1510, 1511, 1210, 1211, 1212, 1213};
            case 112: return new int[]{17, PENDANT_2};
            case 113: return new int[]{22};
            case 114: return new int[]{MEDAL};
            case 115: return new int[]{SHOULDER};
            case 116: return new int[]{POCKET};
            case 118: return new int[]{BADGE};
            case 119: return new int[]{EMBLEM};
            case 120: return new int[]{5000, 5001, 5002};
            case 161: return new int[]{1100};
            case 162: return new int[]{1101};
            case 163: return new int[]{1102};
            case 164: return new int[]{1103};
            case 165: return new int[]{1104};
            case 166: return new int[]{ANDROID_282};
            case 167: return new int[]{HEART_282};
            case 168: return range(1400, 25);
            case 178: return new int[]{5250};
            case 180: return itemId == PET_NAME_TAG_1802100 ? new int[]{23, 25, 27} : new int[]{14, 24, 26};
            case 182: return new int[]{23, 25, 27};
            case 184: return new int[]{5100};
            case 185: return new int[]{5102};
            case 186: return new int[]{5103};
            case 187: return new int[]{5104};
            case 188: return new int[]{5101};
            case 189: return new int[]{5105};
            case 190: return new int[]{18};
            case 191: return new int[]{19};
            case 192: return new int[]{20};
            case 194: return new int[]{1000};
            case 195: return new int[]{1001};
            case 196: return new int[]{1002};
            case 197: return new int[]{1003};
            default: break;
        }
        // weapons (79F860 / isWeapon): 11, then the Zero weapon slot
        if (itemId >= 1210000 && itemId < 1600000 || prefix == 170) {
            return new int[]{11, 1507};
        }
        return NONE;
    }

    /**
     * The server {@code bagIndex} values of an item under 282: {@link #clientSlots} with the android / heart stored as
     * the 265 BodyPart (53 / 54, {@link #wornToServer}) and, for cash items (and all pet equips / name tags, which the
     * client only wears in the cash area), slots below 100 moved to the cash area (+100).
     */
    public static java.util.List<Integer> storageSlots(int itemId, boolean cash) {
        int prefix = itemId / 10000;
        boolean cashArea = cash || prefix == 180 || prefix == 182;
        java.util.List<Integer> result = new java.util.ArrayList<>();
        for (int slot : clientSlots(itemId)) {
            int stored = wornToServer(slot);
            result.add(cashArea && slot < CASH_OFFSET ? slot + CASH_OFFSET : stored);
        }
        return result;
    }

    /**
     * The AvatarLook equip-list slot byte of an item (282 reader 0901B97: slots 1..39 are kept, anything else ignored);
     * 0 when the item has no such slot.
     */
    public static int avatarSlot(int itemId) {
        int[] slots = clientSlots(itemId);
        return slots.length > 0 && slots[0] >= 1 && slots[0] <= 39 ? slots[0] : 0;
    }

    /** Pet index (0..2) of a 282 pet equip slot 114 / 124 / 126 (sign ignored), else -1. */
    public static int petWearIndex(int slot) {
        return indexOf(PET_WEAR, Math.abs(slot));
    }

    /** Pet index (0..2) of a 282 pet name-tag ring slot 123 / 125 / 127 (sign ignored), else -1. */
    public static int petNameTagIndex(int slot) {
        return indexOf(PET_NAME_TAG, Math.abs(slot));
    }

    /** A pet name-tag ring (1802100, 182xxxx): worn in 123 / 125 / 127. */
    public static boolean isPetNameTag(int itemId) {
        return itemId == PET_NAME_TAG_1802100 || itemId / 10000 == 182;
    }

    private static int indexOf(int[] values, int value) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] == value) {
                return i;
            }
        }
        return -1;
    }

    private static int[] range(int base, int count) {
        int[] result = new int[count];
        for (int i = 0; i < count; i++) {
            result[i] = base + i;
        }
        return result;
    }
}
