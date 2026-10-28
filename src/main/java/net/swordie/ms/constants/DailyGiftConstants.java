package net.swordie.ms.constants;

import net.swordie.ms.client.daily.DailyGiftItemInfo;

import java.util.ArrayList;
import java.util.List;

import static net.swordie.ms.constants.ItemConstants.*;

public class DailyGiftConstants {

    public static List<DailyGiftItemInfo> items;

    public static final int MONSTER_COUNT_QR = 501385;

    public static final int REQ_MONSTER = 300;

    /** GMS 265 (and official GMS/TMS) daily gift level. */
    public static final int REQ_LEVEL = 33;

    /**
     * TMS 282 default (dailyui-1005). The level goes to the client in every daily gift entry (282 entry +0x2C,
     * read at 133F686); the main menu disables 每日禮物 (item 0x1F) when the character level is below it
     * (0x2878870), and the client's own claim check 0x1F204B0 refuses it with SID_MSG_DAILYGIFT_LEVEL_LIMIT_WZ2.
     * The server never enforced the level (the panel 每日簽到 and the launcher claim at any level), so 33 only hid
     * the native window from low characters. Override with {@code -Dtms282.dailyGift.reqLevel=N} (1..300).
     */
    public static final int TMS282_REQ_LEVEL = 10;

    /** Level advertised to the client (and the launcher) by the active protocol. */
    public static int reqLevel() {
        if (net.swordie.ms.connection.protocol.Protocol.active().version()
                != net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) {
            return REQ_LEVEL;
        }
        return tms282ReqLevel(System.getProperty("tms282.dailyGift.reqLevel"));
    }

    /** The 282 level from the system property value; missing, malformed or out of 1..300 gives the default. */
    static int tms282ReqLevel(String property) {
        if (property == null || property.isBlank()) {
            return TMS282_REQ_LEVEL;
        }
        try {
            int level = Integer.parseInt(property.trim());
            return level >= 1 && level <= 300 ? level : TMS282_REQ_LEVEL;
        } catch (NumberFormatException e) {
            return TMS282_REQ_LEVEL;
        }
    }

    private static void initItems() {
        items = new ArrayList<>();
        items.add(new DailyGiftItemInfo(1, HYPER_TELEPORT_ROCK, 1, 0)); // Hyper Teleport Rock
        items.add(new DailyGiftItemInfo(2, 2450064, 2, 0)); // 2x EXP Coupon (30 min) x2
        items.add(new DailyGiftItemInfo(3, 1122017, 1, 0)); // Pendant of the Spirit
        items.add(new DailyGiftItemInfo(4, MYSTICAL_CUBE, 10, 0)); // Mystical Cube x 10
        items.add(new DailyGiftItemInfo(5, SPELL_TRACE_ID, 500, 0)); // Spell Trace x 500
        items.add(new DailyGiftItemInfo(6, 2432970, 1, 0)); // Special Medal of Honor
        items.add(new DailyGiftItemInfo(7, 5680410, 1, 0)); // 5000 Maple Points
        items.add(new DailyGiftItemInfo(8, MYSTICAL_CUBE, 50, 0)); // Mystical Cube x 50
        items.add(new DailyGiftItemInfo(9, 2450064, 2, 0)); // 2x EXP Coupon (30 min) x2
        items.add(new DailyGiftItemInfo(10, HARD_CUBE, 20, 0)); // Hard Cube x 20
        items.add(new DailyGiftItemInfo(11, 2048716, 1, 0)); // Powerful Rebirth Flame
        items.add(new DailyGiftItemInfo(12, 2000005, 100, 0)); // Power Elixir x 100
        items.add(new DailyGiftItemInfo(13, 2350000, 1, 0)); // Character Slot Expansion Coupon
        items.add(new DailyGiftItemInfo(14, MYSTICAL_CUBE, 50, 0)); // Mystical Cube x 50
        items.add(new DailyGiftItemInfo(15, HYPER_TELEPORT_ROCK, 1, 0)); // Hyper Teleport Rock
        items.add(new DailyGiftItemInfo(16, 2450064, 2, 0)); // 2x EXP Coupon (30 min) x2
        items.add(new DailyGiftItemInfo(17, 5205007, 1, 0)); // 20K Maple Point Coupon
        items.add(new DailyGiftItemInfo(18, 5062800, 1, 0)); // Miracle Circulator
        items.add(new DailyGiftItemInfo(19, SPELL_TRACE_ID, 500, 0)); // Spell Trace x 500
        items.add(new DailyGiftItemInfo(20, 2430909, 1, 0)); // Trait Boost Potion
        items.add(new DailyGiftItemInfo(21, 5205011, 1, 0)); // 50K Maple Point Coupon
        items.add(new DailyGiftItemInfo(22, 2023072, 1, 0)); // 2x Drop Coupon
        items.add(new DailyGiftItemInfo(23, 1113227, 1, 0)); // Reboot Ring
        items.add(new DailyGiftItemInfo(24, 2028333, 5, 0)); // Mysterious Meso Pouch x 5
        items.add(new DailyGiftItemInfo(25, MYSTICAL_CUBE, 50, 0)); // Mystical Cube x 50
        items.add(new DailyGiftItemInfo(26, 2049705, 1, 0)); // Epic Potential Scroll 50%
        items.add(new DailyGiftItemInfo(27, 2028333, 20, 0)); // Mysterious Meso Pouch x 20
        items.add(new DailyGiftItemInfo(28, BONUS_MYSTICAL_CUBE, 30, 0)); // Bonus Mystical Cube x 30
    }

    static {
        initItems();
    }

    private static final class Tms282 {
        static final List<DailyGiftItemInfo> ITEMS = load();

        static List<DailyGiftItemInfo> load() {
            try {
                return parse(java.nio.file.Files.readAllLines(java.nio.file.Path.of(
                        net.swordie.ms.ServerConstants.DIR, "data/protocol/tms282.3/daily-gift.tsv")));
            } catch (java.io.IOException e) {
                throw new IllegalStateException("Cannot read TMS 282 daily gift rewards", e);
            }
        }
    }

    /** 28 rows {@code day, item_id, count, period_days, name}; days must be 1..28 in order. */
    static List<DailyGiftItemInfo> parse(Iterable<String> rows) {
        List<DailyGiftItemInfo> result = new ArrayList<>();
        for (String row : rows) {
            if (row.isBlank() || row.startsWith("day	")) continue;
            String[] c = row.split("	", -1);
            if (c.length != 5) throw new IllegalArgumentException("Malformed daily gift row: " + row);
            int day = Integer.parseInt(c[0]), item = Integer.parseInt(c[1]);
            int count = Integer.parseInt(c[2]), period = Integer.parseInt(c[3]);
            if (day != result.size() + 1 || item <= 0 || count <= 0 || period < 0)
                throw new IllegalArgumentException("Invalid daily gift row: " + row);
            result.add(new DailyGiftItemInfo(day, item, count, 0, period));
        }
        if (result.size() != 28) throw new IllegalArgumentException("Daily gift needs 28 days, got " + result.size());
        return List.copyOf(result);
    }

    /** The reward table for the active protocol: TMS 282 uses the 279 package list, GMS keeps its own. */
    public static List<DailyGiftItemInfo> rewards() {
        return net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3
                ? net.swordie.ms.connection.api.launcher.daily.DailyRewardTable.standard().gameRewards() : items;
    }

    /** Legacy GMS rewards carry their expiry here; table rows carry it in periodDays. */
    public static int periodDays(DailyGiftItemInfo info) {
        if (net.swordie.ms.connection.protocol.Protocol.active().version() == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3)
            return info.getPeriodDays();
        if (info.getPeriodDays() > 0) return info.getPeriodDays();
        return switch (info.getItemId()) {
            case HYPER_TELEPORT_ROCK -> 7;
            case 1122017 -> 3;
            case 1113227 -> 15;
            default -> 0;
        };
    }
}
