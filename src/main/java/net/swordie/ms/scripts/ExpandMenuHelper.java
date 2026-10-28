package net.swordie.ms.scripts;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.packet.UserLocal;
import net.swordie.ms.enums.ChatType;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.Server;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;

/**
 * Hooks for expand/menu scripts: daily flags, wild bosses, bounty tracking.
 */
public final class ExpandMenuHelper {

    public static final int MENU_DAILY_QUEST = 8800020;
    public static final int MENU_WILD_BOSS_QUEST = 8800026;
    public static final int MENU_HEXA_QUEST = 8800027;
    public static final int MENU_FAMILIAR_QUEST = 8800028;

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private static final Set<Integer> BOUNTY_BOSSES = Set.of(
            8800000, 8800001, 8800002, 8800003, 8800100, 8800101, 8800102,
            8810018, 8810022, 8810122, 8820001, 8820212
    );

    private record WildBossInfo(String name, int respawnMinutes) {
    }

    private static final Map<Integer, WildBossInfo> WILD_BOSSES = Map.ofEntries(
            Map.entry(2220000, new WildBossInfo("紅蝸牛王", 60)),
            Map.entry(3220000, new WildBossInfo("樹妖王", 60)),
            Map.entry(6130101, new WildBossInfo("蘑菇王", 60)),
            Map.entry(6300005, new WildBossInfo("殭屍蘑菇王", 60)),
            Map.entry(8130100, new WildBossInfo("蝙蝠怪", 60)),
            Map.entry(9400205, new WildBossInfo("大海獸", 120)),
            Map.entry(9400121, new WildBossInfo("天鷹", 120)),
            Map.entry(8180000, new WildBossInfo("火焰龍", 120)),
            Map.entry(8180001, new WildBossInfo("天鷹", 120)),
            Map.entry(8220001, new WildBossInfo("雷卡", 120)),
            Map.entry(8220000, new WildBossInfo("雷卡", 120))
    );

    private ExpandMenuHelper() {
    }

    public static void onMobKilled(Char chr, Mob mob) {
        if (chr == null || mob == null || !mob.isBoss()) {
            return;
        }
        int templateId = mob.getTemplateId();
        if (BOUNTY_BOSSES.contains(templateId)) {
            markDailyFlag(chr, MENU_DAILY_QUEST, "killed");
        }
        WildBossInfo info = WILD_BOSSES.get(templateId);
        if (info != null) {
            long respawnAt = System.currentTimeMillis() + info.respawnMinutes * 60_000L;
            chr.setQRValueByKey(MENU_WILD_BOSS_QUEST, "dead_" + templateId, String.valueOf(respawnAt));
            chr.setQRValueByKey(MENU_WILD_BOSS_QUEST, "killer_" + templateId, chr.getName());
            markDailyFlag(chr, MENU_DAILY_QUEST, "killed");
            Server.get().broadcastForWorld(UserLocal.chatMsg(ChatType.Notice2,
                    "[野外BOSS] " + info.name + " 被 " + chr.getName() + " 擊殺了！"));
        }
    }

    public static boolean isDailyFlagSet(Char chr, int questId, String key) {
        String today = LocalDate.now().format(DAY_FMT);
        String dateVal = chr.getQRValueByKey(questId, key + "_date");
        if (dateVal == null || !dateVal.equals(today)) {
            return false;
        }
        String val = chr.getQRValueByKey(questId, key);
        return val != null && val.equals("1");
    }

    public static void markDailyFlag(Char chr, int questId, String key) {
        String today = LocalDate.now().format(DAY_FMT);
        if (!chr.hasQuest(questId)) {
            chr.createQuestWithQRValue(questId, key + "_date=" + today);
        }
        chr.setQRValueByKey(questId, key + "_date", today);
        chr.setQRValueByKey(questId, key, "1");
    }

    public static int getPlatformarStar(Char chr, int floor) {
        if (floor < 1 || floor > 40) {
            return 0;
        }
        int quest = switch (floor) {
            case 31, 32, 33, 34, 35 -> 18847 + floor;
            case 36, 37, 38, 39, 40 -> 18891 + floor;
            default -> 18838 + floor;
        };
        String val = chr.getQRValueByKey(quest, "cs");
        if (val == null || val.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static long getWildBossRespawnAt(Char chr, int mobTemplateId) {
        String val = chr.getQRValueByKey(MENU_WILD_BOSS_QUEST, "dead_" + mobTemplateId);
        if (val == null || val.isEmpty()) {
            return 0;
        }
        try {
            return Long.parseLong(val);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static String getWildBossKiller(Char chr, int mobTemplateId) {
        String val = chr.getQRValueByKey(MENU_WILD_BOSS_QUEST, "killer_" + mobTemplateId);
        return val == null ? "" : val;
    }
}
