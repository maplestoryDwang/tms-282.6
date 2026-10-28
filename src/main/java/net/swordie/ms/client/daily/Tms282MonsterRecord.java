package net.swordie.ms.client.daily;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.constants.DailyGiftConstants;

/**
 * TMS 282 daily gift monster count, quest record {@link DailyGiftConstants#MONSTER_COUNT_QR} 501385
 * ({@code date=yyyyMMdd;count=N;}) - dailyui-1005.
 * <p>
 * The 282.4 client reads it in 0x8B7E90 through 0x8BC6D0 from the CharacterData+0xD5B map ("date" must be today
 * (0x13808B0), else the count is 0). That map is filled by MESSAGE type 1 QuestRecord (5ED27F0 case 1 -> 5EEF5D0 ->
 * 5EF25C0 -> 8C29C0 -> 8BE740); MESSAGE type 14 QuestRecordEx (5EF3AA0) writes +0xD73 instead. The 265 code updated
 * the count with QuestRecordEx, so on 282 the client kept the login value (the field-entry replay sends type 1) and
 * the daily gift window and its own claim check (0x1F204B0: count >= entry +0x4C 300) never saw a kill until relogin.
 * Here every change is a QuestRecord (type 1, state Started) carrying the whole value, and a record from an earlier
 * day restarts at 0 for today.
 */
public final class Tms282MonsterRecord {
    private Tms282MonsterRecord() {
    }

    /** Login: today's record exists (created or restarted at 0 when it is missing or from another day). */
    static void ensureToday(Char chr, String today) {
        if (!today.equals(dateOf(chr))) {
            put(chr, today, 0);
        }
    }

    /** One counted kill (the caller checked the level range). Nothing is sent once today's count is at the cap. */
    static void kill(Char chr, String today) {
        int count = today.equals(dateOf(chr)) ? countOf(chr) : 0;
        if (today.equals(dateOf(chr)) && count >= DailyGiftConstants.REQ_MONSTER) {
            return;
        }
        put(chr, today, Math.min(count + 1, DailyGiftConstants.REQ_MONSTER));
    }

    static String value(String date, int count) {
        return String.format("date=%s;count=%d;", date, count);
    }

    private static void put(Char chr, String date, int count) {
        chr.createQuestWithQRValue(DailyGiftConstants.MONSTER_COUNT_QR, value(date, count), false);
    }

    private static String dateOf(Char chr) {
        return chr.hasQuest(DailyGiftConstants.MONSTER_COUNT_QR)
                ? chr.getQRValueByKey(DailyGiftConstants.MONSTER_COUNT_QR, "date") : null;
    }

    private static int countOf(Char chr) {
        try {
            String v = chr.getQRValueByKey(DailyGiftConstants.MONSTER_COUNT_QR, "count");
            return v == null ? 0 : Math.max(0, Integer.parseInt(v.trim()));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
