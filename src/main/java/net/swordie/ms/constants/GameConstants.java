package net.swordie.ms.constants;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.console.config.ServerRuntimeConfig;
import net.swordie.ms.client.character.items.Equip;
import net.swordie.ms.client.character.items.IntensePowerCrystalData;
import net.swordie.ms.connection.packet.QuickMoveInfo;
import net.swordie.ms.enums.*;
import net.swordie.ms.loaders.ItemData;
import net.swordie.ms.util.FileTime;
import net.swordie.ms.util.Util;
import net.swordie.ms.util.container.Triple;
import net.swordie.ms.world.event.DailyGiftItemInfo;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class GameConstants {
    public static int CHANNELS_PER_WORLD = ServerRuntimeConfig.intValue("channel.count", 10);
    public static final int BUFFED_CHANNELS = 5;
    public static final long MAX_MONEY = 499_999_999_999L;
    public static final short DAMAGE_SKIN_MAX_SIZE = 35;
    public static final int MAX_PET_AMOUNT = 3;
    public static final int MAX_HP_MP = 500_000;
    public static final long DAMAGE_CAP = 500_000_000_000L;
    public static final int MAX_TRAIT_EXP = 93596;
    public static final int BEGINNER_SP_MAX_LV = 7;
    public static final int RESISTANCE_SP_MAX_LV = 10;
    public static final int QUICKSLOT_LENGTH = 32;
    public static final Integer[] QuickSlot_basic = {42, 82, 71, 73, 29, 83, 79, 81, 2, 3, 4, 5, 16, 17, 18, 19, 6, 7, 8, 9, 20, 30, 31, 32, 10, 11, 33, 34, 37, 38, 49, 50};
    public static final Integer[] QuickSlot_adv = {16, 17, 18, 19, 30, 31, 32, 33, 2, 3, 4, 5, 29, 56, 44, 45, 6, 7, 8, 9, 46, 22, 23, 36, 10, 11, 37, 38, 24, 25, 49, 50};

    public static final boolean IS_OPEN_UI_MAPLE_WORLD_REWARD_POINT = false;

    // Stats
    public static final int HP_PER_LEVEL = 50;
    public static final int MP_PER_LEVEL = 37;
    public static final double STR_HP_MULT = 1.5;
    public static final double STR_MP_MULT = 0.75;
    public static final double INT_HP_MULT = 0.75;
    public static final double INT_MP_MULT = 1.5;

    //Shop
    public static final int GENERAL_SHOP = 9201060;

    //Inventory
    public static final int MAX_CHARACTER_SLOTS = 58;

    // Familiar
    public static final short FAMILIAR_ORB_VITALITY = 300;
    public static final int FAMILIAR_BADGE_SLOTS = 8;

    //Buffed mobs
    public static final int BUFFED_MOB_HP_MULTIPLIER = 500;
    public static final int BUFFED_MOB_SCALE = 175; //Default scale is 100
    public static final int BUFFED_MOB_DAMAGE_MULTIPLIER = 15;
    public static final int BUFFED_MOB_DEFENSE_MULTIPLIER = 20;
    public static final int BUFFED_MOB_EXP_MULTIPLIER = 250;

    // Drop
    public static final int DROP_HEIGHT = 100; // was 20
    public static final int DROP_DIFF = 50;
    public static final int DROP_REMAIN_ON_GROUND_TIME = 120_000; // 2 minutes
    public static final int DROP_REMOVE_OWNERSHIP_TIME = 30_000; // 30 sec
    public static final int MIN_MONEY_MULT = 6;
    public static final int MAX_MONEY_MULT = 9;
    public static final int MAX_DROP_CHANCE = 10000;

    // Combo Kill
    public static final int COMBO_KILL_RESET_TIMER = 5; // 5 sec
    public static final int COMBO_KILL_REWARD_BLUE = 50; // Combo kills
    public static final int COMBO_KILL_REWARD_PURPLE = 350; // Combo kills
    public static final int COMBO_KILL_REWARD_RED = 750; // Combo kills
    public static final int COMBO_KILL_REWARD_YELLOW = 2000; // Combo kills

    // Multi Kill
    public static final float MULTI_KILL_BONUS_EXP_MULTIPLIER = 0.01f; // Multi Kill Bonus Exp given  =  mobEXP * (( multi Kill Amount - 2 ) * 5) * BONUS_EXP_FOR_MULTI_KILL

    // Inner Ability
    public static final int CHAR_POT_BASE_ID = 70000000;
    public static final int CHAR_POT_END_ID = 70000062;
    public static final int BASE_CHAR_RARE_POT_UP_RATE = 5; // 5%
    public static final int BASE_CHAR_RARE_POT_DOWN_RATE = 0; // 0%
    public static final int BASE_CHAR_EPIC_POT_UP_RATE = 3; // 3%
    public static final int BASE_CHAR_EPIC_POT_DOWN_RATE = 3; // 3%
    public static final int BASE_CHAR_UNIQUE_POT_UP_RATE = 2; // 2%
    public static final int BASE_CHAR_UNIQUE_POT_DOWN_RATE = 15; // 15%
    public static final int BASE_CHAR_LEGENDARY_POT_UP_RATE = 0; // 0%
    public static final int BASE_CHAR_LEGENDARY_POT_DOWN_RATE = 50; // 50%
    public static final int CHAR_POT_RESET_COST = 100;
    public static final int CHAR_POT_GRADE_LOCK_COST = 10000;
    public static final int CHAR_POT_LOCK_1_COST = 3000;
    public static final int CHAR_POT_LOCK_2_COST = 5000;

    // Nebulite
    public static final int NEBULITE_CLASS_DD_UP_RATE = 30; // 30%
    public static final int NEBULITE_CLASS_CD_UP_RATE = 10; // 20%
    public static final int NEBULITE_CLASS_CD_DOWN_RATE = 20; // 20%
    public static final int NEBULITE_CLASS_CC_UP_RATE = 25; // 20%
    public static final int NEBULITE_CLASS_CC_DOWN_RATE = 50; // 10%
    public static final int NEBULITE_CLASS_BC_UP_RATE = 10; // 5%
    public static final int NEBULITE_CLASS_BC_DOWN_RATE = 20; // 5%
    public static final int NEBULITE_CLASS_BB_UP_RATE = 15; // 5%
    public static final int NEBULITE_CLASS_BB_DOWN_RATE = 30; // 30%
    public static final int NEBULITE_CLASS_AB_UP_RATE = Util.getRandom(1, 10); // 1-2%
    public static final int NEBULITE_CLASS_AB_DOWN_RATE = 20; // 1-2%
    public static final int NEBULITE_CLASS_AA_UP_RATE = Util.getRandom(10, 20); // 1-2%
    public static final int NEBULITE_CLASS_AA_DOWN_RATE = 30; // 70%

    // Potential Chance on Drop Equips
    public static final int RANDOM_EQUIP_UNIQUE_CHANCE = 1; // out of a 100
    public static final int RANDOM_EQUIP_EPIC_CHANCE = 3; // out of a 100
    public static final int RANDOM_EQUIP_RARE_CHANCE = 8; // out of a 100

    // Exp Orb
    public static final int BLUE_EXP_ORB_ID = 2023484;
    public static final int PURPLE_EXP_ORB_ID = 2023494;
    public static final int RED_EXP_ORB_ID = 2023495;
    public static final int YELLOW_EXP_ORB_ID = 2023669;
    public static final double BLUE_EXP_ORB_MULT = 2;
    public static final double PURPLE_EXP_ORB_MULT = 3.5;
    public static final double RED_EXP_ORB_MULT = 5;
    public static final double YELLOW_EXP_ORB_MULT = 7.5;

    // Soul Orb
    public static final int MOB_SOUL = 4001536;

    // Mob
    public static final int MOB_SKILL_CHANCE = 20;
    public static final int MOB_ATTACK_CHANCE = 50;
    public static final int NX_DROP_CHANCE = 3;

    // Elite mob
    public static final int ELITE_MOB_SKILL_COUNT = 2;
    public static final int ELITE_MOB_RESPAWN_TIME = 300; // seconds
    public static final int ELITE_MOB_SPAWN_CHANCE = 100; // out of a 1000
    public static final int ELITE_MOB_DARK_NOTIFICATION = 10;
    public static final int ELITE_BOSS_REQUIRED_KILLS = 10;
    public static final String ELITE_BOSS_BGM = "Bgm45/Anthem For Heroes";
    public static final String ELITE_BOSS_BONUS = "Bgm23/MPBonusMap";
    public static final long ELITE_BOSS_HP_RATE = 2000; // multiplier for boss' hp compared to the mobs on the map
    public static final int ELITE_BOSS_DAMAGE_MULTIPLIER = 50;
    public static final int ELITE_BOSS_DEFENSE_MULTIPLIER = 100;
    public static final int ELITE_BOSS_SCALE = 200; //Default scale is 100
    public static final int RARE_TREASURE_CHEST = 2433834; // Floating

    public static boolean isEliteBossTemplate(int tid) {
        switch (tid) {
            case 9303130: case 9303131: case 9303132: case 9303133: case 9303134:
            case 9303135: case 9303136: case 9303137: case 9303138: case 9303139:
                return true;
            default:
                return false;
        }
    }

    public static final int DARKNESS_RUNE_NUMBER_OF_ELITE_MOBS_SPAWNED = 3; // number of elites spawned when activating Rune of Darkness

    // Party
    public static final int MAX_PARTY_MOB_LEVEL_DIFF = 5; // x levels lower than mob level
    public static final int MAX_PARTY_CHR_LEVEL_DIFF = 5; // x levels lower than mob level

    // Hyper stat
    public static final long HYPER_STAT_RESET_COST = 10000000;
    public static final long HYPER_STAT_CHANGE_PRESET_COST = 2000000;
    public static final long HYPER_SKILL_RESET_COST = 100000;

    // Cash Shop
    public static final int MAX_CS_ITEMS_PER_PAGE = 12;
    public static final int MAX_LOCKER_SIZE = 158;

    // Commerci Republic
    public static final int COMMERCI_TRADE_ENTRANCE_MAP = 865000001;
    public static final int COMMERCI_TRADE_MAP_1 = 865000100;
    public static final int COMMERCI_TRADE_MAP_2 = 865000200;
    public static final int COMMERCI_TRADE_MAP_3 = 865000300;
    public static final int COMMERCI_TRADE_MAP_4 = 865000400;
    public static final int COMMERCI_TRADE_MAP_5 = 865000501;
    public static final int COMMERCI_TRADE_MAP_6 = 865000900;

    // START OF Party Quests
    public static final long PARTY_QUEST_GLOBAL_EXP = 300000; // The minimum amount of Exp given from a PQ.
    // Dojo
    public static final int DOJO_DUMMY_DURATION = 10; // Dummy will stay alive for [] minutes, after which it will be removed.
    public static final int DOJO_SPAWM_BOSS_DELAY = 3; // Spawn delay, in seconds, per boss on the Dojo Floors
    // Monster Park
    public static final byte MAX_MONSTER_PARK_RUNS = 7; // Max Monster Park runs per character
    public static final int MONSTER_PARK_EXP_QUEST = 99999; // Quest where the Exp for MP runs gets stored.
    public static final int MONSTER_PARK_ENTRANCE_CHECK_QUEST = 99997; // Quest where the Number of MP runs are stored
    public static final int MONSTER_PARK_TIME = 10 * 60; // 10minutes
    //Boss QR Values
    public static final int EASY_HORNTAIL_QUEST = 99996; // Quest where the Spawn state of horntail's heads is stored
    // Moon Bunny PQ
    public static final int MOON_BUNNY_ENTRANCE_MAP = 933000000;
    public static final int MOON_BUNNY_STAGE = 910010000;
    public static final int RICE_CAKE = 4001101;
    public static final int PRIMROSE_SEED = 4001453;
    public static final int MOON_BUNNY = 9300061;
    public static final int MOON_REACTOR = 9101000;
    public static final int MOON_BUNNY_EXIT = 933009000;
    // Firt Time Together PQ
    public static final int FIRST_TIME_TOGETHER_ENTRANCE_MAP = 933010000;
    public static final int FIRST_TIME_TOGETHER_STAGE_1 = 933011000;
    public static final int FIRST_TIME_TOGETHER_STAGE_2 = 933012000;
    public static final int FIRST_TIME_TOGETHER_STAGE_3 = 933013000;
    public static final int FIRST_TIME_TOGETHER_STAGE_4 = 933014000;
    public static final int FIRST_TIME_TOGETHER_EXIT = 933019000;
    // Evolving PQ
    public static final int EVOLVING_TIME = 15 * 60;
    public static final int EVOLVING_ENTRANCE_MAP = 957000000;
    public static final int EVOLVING_LINK_MAP_1 = 957010000;
    public static final int EVOLVING_LINK_MAP_2 = 957011000;
    public static final int EVOLVING_LINK_MAP_3 = 957012000;
    public static final int EVOLVING_LINK_MAP_4 = 957013000;
    public static final int EVOLVING_LINK_MAP_5 = 957014000;
    public static final int EVOLVING_LINK_MAP_6 = 957015000;
    public static final int EVOLVING_LINK_MAP_7 = 957016000;
    public static final int EVOLVING_LINK_MAP_8 = 957017000;
    public static final int EVOLVING_LINK_MAP_9 = 957018000;
    public static final int EVOLVING_CENTRAL_CONTROL_MAP = 957019000;
    // Tangyoon Cooking PQ
    public static final int TANGYOON_COOKING_ENTRANCE_MAP = 912080000;
    public static final int TANGYOON_COOKING_MAIN_STAGE = 912080100;
    // Dimensional Invasion PQ
    public static final int DIMENSIONAL_INVASION_ENTRANCE_MAP = 940020000;
    public static final int DIMENSIONAL_INVASION_MAIN_STAGE = 940021000;
    // Nett Pyramid PQ
    public static final int NETT_PYRAMID_TIME = 60 * 60;
    public static final int NETT_PYRAMID_ENTRANCE_MAP = 926010000;
    public static final int NETT_PYRAMID_MAIN_MAP = 926010100;
    public static final int NETT_PYRAMID_REWARD_MAP = 926010001;
    public static final int NETT_PYRAMID_EXIT_MAP = 926020001;
    // Xerxes In Chryse PQ
    public static final int XERXES_CHRYSE_TIME = 20 * 60;
    public static final int XERXES_CHRYSE_ENTRANCE_MAP = 920012000;
    public static final int XERXES_CHRYSE_FRONT_FIELD = 920012100;
    public static final int XERXES_CHRYSE_TOWER = 920012200;
    public static final int XERXES_CHRYSE_MAP_BOSS = 920012300;
    public static final int XERXES_CHRYSE_MAP_REWARD = 920012400;
    public static final int XERXES_CHRYSE_EXIT = 920012600;
    // Escape PQ
    public static final int ESCAPE_TIME = 20 * 60;
    public static final int ESCAPE_ENTRANCE_MAP = 921160000;
    public static final int ESCAPE_STAGE_FIRST_STAGE = 921160100;
    // Lord Pirate PQ
    public static final int LORD_PIRATE_TIME = 24 * 60;
    public static final int LORD_PIRATE_ENTRANCE_MAP = 251010404;
    public static final int LORD_PIRATE_FIRST_STAGE = 925100000;
    public static final int LORD_PIRATE_EXIT = 925100700;
    // Escape PQ
    public static final int DRAGON_RIDER_TIME = 20 * 60;
    public static final int DRAGON_RIDER_FIRST_STAGE = 240080100;
    // Kenta in Danger
    public static final int KENTA_IN_DANGER_TIME = 20 * 60;
    public static final int KENTA_IN_DANGER_ENTRANCE_MAP = 923040000;
    public static final int KENTA_IN_DANGER_FIRST_STAGE = 923040100;
    // Romeo & Juliet
    public static final int ROMEO_JULIET_TIME = 20 * 60;
    public static final int ROMEO_ENTRANCE_MAP = 261000011;
    public static final int ROMEO_EXIT_MAP = 926100700;
    public static final int ROMEO_ENTRANCE_FIRST_STAGE = 926100000;
    public static final int JULIET_ENTRANCE_MAP = 261000021;
    public static final int JULIET_EXIT_MAP = 926110700;
    public static final int JULIET_ENTRANCE_FIRST_STAGE = 926110000;
    // Alien Visitor PQ
    public static final int ALIEN_VISITOR_ENTRANCE_MAP = 861000000;
    public static final int ALIEN_VISITOR_START_MAP = 861000050;
    public static final int ALIEN_VISITOR_EXT_MAP = 861000001;
    public static final int ALIEN_VISITOR_TIME = 5 * 60;
    // Chu Chu PQ
    public static final int HUNGRY_MUTO_ENTRANCE_MAP = 450002023;
    public static final int HUNGRY_MUTO_NORMAL_STAGE = 921170050;
    public static final int HUNGRY_MUTO_HARD_STAGE = 921170100;
    public static final int HUNGRY_MUTO_EXIT_MAP = 450002024;
    // END OF Party Quests
    public static final int EASY_HILLA_QUEST = 99995; //Quest where the state of hilla portals is stored
    public static final int ARKARIUM_QUEST = 99994; //Quest wehre difficulty of arkarium is stored
    public static final int EASY_VON_LEON_QUEST = 99991;
    public static final int BOSS_QUEST = 99990; //checksomething
    // Custom Quest QR Values
    public static final int EVAN_INTRO = 99992; // Used for portal script "evanRoom1" so script doesn't loop
    public static final int EVAN_INTRO2 = 99993; // Used for portal script "DragonEggNotice" so script doesn't loop
    // Trading
    public static final int MAX_TRADE_ITEMS = 9;
    // Faming
    public static final int MIN_LEVEL_TO_FAME = 15;
    public static final int FAME_COOLDOWN = 24; // in hours
    // Monster Collection
    public static final int MOBS_PER_PAGE = 25;
    public static final int MOBS_PER_GROUP = 5;
    // Skills
    public static final int TIME_LEAP_QR_KEY = 99996; // Quest where personal Time Leap CDs get stored
    //Merchant
    public static final int MAX_MERCHANT_VISITORS = 6;
    public static final int MAX_MERCHANT_SLOTS = 16;
    //Inventory
    public static final int MAX_INVENTORY_SLOTS = 128;
    public static final int MAX_CASH_INVENTORY_SLOTS = 256;
    public static final int SILENT_CRUSADE_BOSS_COOLDOWN = 240; // minutes between silent crusade attempts (240 min = 4 hours)
    //Trunk
    public static final byte DEFAULT_TRUNK_SIZE = 56;
    //Matrix Core
    public static final int SKILL_CORE_CHANCE = 10;
    public static final int BOOST_CORE_CHANCE = 89;
    public static final int SPECIAL_CORE_CHANCE = 1;
    public static final int JOB_CORE_CHANCE = 80;

    public static final short MAX_LEVEL = 300;
    // per equip
    // per equip
    // Starforce
    private static final int[][] STARFORCE_LEVELS = {
            {Integer.MAX_VALUE, -1}, // per equip
            {137, 30},
            {127, 20},
            {117, 10},
            {107, 8},
            {95, 5},
    };
    private static final int[][] STARFORCE_LEVELS_SUPERIOR = {
            {Integer.MAX_VALUE, 15},
            {137, 15},
            {127, 10},
            {117, 8},
            {107, 5},
            {95, 3},
    };
    // Time
    public static final long ONE_DAY = 86400000;
    public static final long ONE_WEEK = 604800000;

    public static long[] itemExp = new long[31];
    public static long[] charExp = new long[300];
    public static final int[] petExp = {0, 1, 3, 6, 14, 31, 60, 108, 181, 287, 434, 632, 891, 1224, 1642, 2161,
            2793, 3557, 4467, 5542, 6801, 8263, 9950, 11882, 14084, 16578, 19391, 22547, 26074, 30000};

    public static Int2ObjectMap<IntensePowerCrystalData> intensePowerCrystal;
    private static int[][] enchantSuccessRatesSuperior = new int[15][2];
    private static List<QuickMoveInfo> quickMoveInfos;
    public static List<DailyGiftItemInfo> dailyItems;

    public static class MonsterParkInfo {
        public int type;
        public int index;
        public String name;
        public String desc;
        public int minLevel;
        public int maxLevel;
        public int reqQuest;
        public int mapID;

        public MonsterParkInfo(int type, int index, String name, String desc, int minLevel, int maxLevel, int reqQuest, int mapID) {
            this.type = type;
            this.index = index;
            this.name = name;
            this.desc = desc;
            this.minLevel = minLevel;
            this.maxLevel = maxLevel;
            this.reqQuest = reqQuest;
            this.mapID = mapID;
        }

        public int getType() {
            return type;
        }

        public int getIndex() {
            return index;
        }

        public String getName() {
            return name;
        }

        public String getDesc() {
            return desc;
        }

        public int getMinLevel() {
            return minLevel;
        }

        public int getMaxLevel() {
            return maxLevel;
        }

        public int getReqQuest() {
            return reqQuest;
        }
    }

    public static List<MonsterParkInfo> monsterParkInfos;

    public static List<MonsterParkInfo> getMonsterParkInfos(int type) {
        List<MonsterParkInfo> result = new LinkedList<>();
        for (MonsterParkInfo entry : monsterParkInfos) {
            if (entry.type == type) {
                result.add(entry);
            }
        }
        return result;
    }

    public static MonsterParkInfo getMonsterPark(int type, int index) {
        for (MonsterParkInfo entry : monsterParkInfos) {
            if (entry.type == type && entry.index == index) {
                return entry;
            }
        }
        return null;
    }

    public static final int[][] BUFF_GROUPS = {
            { 80003600,80003601,80003602,80003603,80003604,80003605,80003606,80003607 },
            { -2024172,-2024173,-2024174,-2024175,-2024176,-2024177,-2024178,-2024179,-2024180,-2024181,-2024182 },
            { -2024193,-2024187,-2024186,-2024194,-2024195,-2024188,-2024183,-2024184,-2024185,-2024189,-2024190,-2024191,-2024192,-2024237 }
    };

    public static final int[][] ALIAS_PAIRS = {
            { -2024195, -2024194 },
            { -2024194, -2024195 },
            { -2024187, -2024186 },
            { -2024186, -2024187 },
            {  80003606, 80003607 },
            {  80003607, 80003606 },
    };

    public static final int[][] QSTATE_ROWS = {
            { -2024237, 200, 503417 } // key, val1, val2
    };
    public static final String QSTATE_K = "qState";
    public static final String QSTATE_V = "1";

    static {
        try {
            initCharExp();
            initItemExp();
            initEnchantRates();
            initEnchantRatesSuperior();
            initQuickMove();
            initMonsterPark();
            initDailyGiftItems();
            initCrystals();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static final long PARTY_QUEST_EXP_FORMULA(Char chr) {
        return PARTY_QUEST_GLOBAL_EXP * (1 + (6 * 10 / chr.getParty().getAvgPartyLevel()));
    } // Exp formula for giving Exp from Party Quests

    private static void initQuickMove() {
        quickMoveInfos = new ArrayList<>();
        quickMoveInfos.add(new QuickMoveInfo(QuickMoveType.MonsterPark, 105, "前往#fcFFFFFFFF#怪物公園#fcFFB7BFC5#" ,"quick_monsterPark", "怪物公園", "105 級以上的角色才能前往怪物公園。", 9071003, 0, false, FileTime.MIN_TIME(), FileTime.MAX_TIME()));
        quickMoveInfos.add(new QuickMoveInfo(QuickMoveType.Ardentmill, 10, "前往專業技術村 #fcFFFFFFFF#梅斯特鎮", "quick_MeisterVille", "梅斯特鎮", "", 9031019, 0, false, FileTime.MIN_TIME(), FileTime.MAX_TIME()));
        quickMoveInfos.add(new QuickMoveInfo(QuickMoveType.LegionCoinShop, 0, "在#fcFFFFFFFF#聯盟硬幣商店#fcFFB7BFC5#購買各種道具", "union_CoinShop", "聯盟硬幣商店", "", 9010111, 0, false, FileTime.MIN_TIME(), FileTime.MAX_TIME()));
        quickMoveInfos.add(new QuickMoveInfo(QuickMoveType.CrystalShop, 0, "出售#fcFFFFFFFF#強烈的力量結晶#fcFFB7BFC5#或購買各種#fcFFFFFFFF#祕藥#fcFFB7BFC5#", "quick_Collecter", "結晶與祕藥", "", 9001212, 0, false, FileTime.MIN_TIME(), FileTime.MAX_TIME()));
        quickMoveInfos.add(new QuickMoveInfo(QuickMoveType.AuctionHouse, 0, "使用#fcFFFFFFFF#拍賣場#fcFFB7BFC5#", "quick_auctionMove", "拍賣場", "", 9030300, 0, false, FileTime.MIN_TIME(), FileTime.MAX_TIME()));
        quickMoveInfos.add(new QuickMoveInfo(QuickMoveType.MesoMarket, 61, "交換#fcFFFFFFFF#楓點#fcFFB7BFC5#與#fcFFFFFFFF#楓幣#fcFFB7BFC5#", "mesoMarketMove", "楓幣商城", "", 9062008, 0, false, FileTime.MIN_TIME(), FileTime.MAX_TIME()));
        quickMoveInfos.add(new QuickMoveInfo(QuickMoveType.DimensionalPortal, 50, "選擇並進入#fcFFFFFFFF#次元圖書館#fcFFB7BFC5#或#fcFFFFFFFF#組隊任務#fcFFB7BFC5#", "DivisionMirror", "次元之鏡", "", 9010022, 0, false, FileTime.MIN_TIME(), FileTime.MAX_TIME()));
        quickMoveInfos.add(new QuickMoveInfo(QuickMoveType.SymbolExpressPass, 0, "使用休菲凱曼的#fcFFFFFFFF#符文快速通關#fcFFB7BFC5#", "quickpath_symbol", "符文快速通關", "完成 #qn34120# 的角色可以使用符文快速通關", 3003146, 34120, false, FileTime.MIN_TIME(), FileTime.MAX_TIME()));
        quickMoveInfos.add(new QuickMoveInfo(QuickMoveType.MapleAdministrator, 10, "與#fcFFFFFFFF#楓之谷管理者#fcFFB7BFC5#對話", "quick_adminNPC", "楓之谷管理者", "", 9010000, 0, false, FileTime.MIN_TIME(), FileTime.MAX_TIME()));
        quickMoveInfos.add(new QuickMoveInfo(QuickMoveType.StorageRoom, 10, "使用#fcFFFFFFFF#倉庫#fcFFB7BFC5#", "quick_trunk", "倉庫", "", 9070110, 0, false, FileTime.MIN_TIME(), FileTime.MAX_TIME()));
        //quickMoveInfos.add(new QuickMoveInfo(0, 9072303, QuickMoveType.Announcer, 1, "Job Advancement", false, FileTime.MIN_TIME();, FileTime.MAX_TIME()));
        //quickMoveInfos.add(new QuickMoveInfo(0, 9201324, QuickMoveType.Waitress, 1, "Custom Options", false, FileTime.MIN_TIME();, FileTime.MAX_TIME()));
    }

    private static void initMonsterPark() {
        monsterParkInfos = new LinkedList<>();
        monsterParkInfos.add(new MonsterParkInfo(1, 0,"自動警備區域", "Magatia", 105, 114, 0, 953020000));
        monsterParkInfos.add(new MonsterParkInfo(1, 1, "苔蘚森林", "EilnForest", 115, 124, 0, 953030000));
        monsterParkInfos.add(new MonsterParkInfo(1, 2, "海賊團祕密基地", "WhiteHerb", 125, 134, 0, 953060000));
        monsterParkInfos.add(new MonsterParkInfo(1, 3, "異界的戰場", "helisium", 135, 144, 0, 953070000));
        monsterParkInfos.add(new MonsterParkInfo(1, 4, "偏僻森林危險區域", "Leafre", 140, 149, 0, 953080000));
        monsterParkInfos.add(new MonsterParkInfo(1, 5, "隱藏的遺跡", "GoldTempleTH", 150, 159, 0, 953090000));
        monsterParkInfos.add(new MonsterParkInfo(1, 6, "廢棄的都市", "neoCity", 160, 169, 0, 954000000));
        monsterParkInfos.add(new MonsterParkInfo(1, 7, "死亡森林", "ElNathDungeon", 170, 179, 0, 954010000));
        monsterParkInfos.add(new MonsterParkInfo(1, 8, "龍之巢穴", "Leafre", 180, 189, 0, 954030000));
        monsterParkInfos.add(new MonsterParkInfo(1, 9, "騎士團要塞", "destructionTown", 190, 199, 0, 954050000));

        monsterParkInfos.add(new MonsterParkInfo(2, 0, "幽靈峽谷", "destructionPerion", 200, 209, 0, 954060000));
        monsterParkInfos.add(new MonsterParkInfo(2, 1, "消滅的旅途", "Road of Vanishing", 200, 209, 34120, 954070000));
        monsterParkInfos.add(new MonsterParkInfo(2, 2, "啾啾艾爾蘭", "ChewChew", 210, 219, 34218, 954080000));
        monsterParkInfos.add(new MonsterParkInfo(2, 3, "夢之都拉契爾恩", "Lacheln", 220, 229, 34331, 954090000));
        monsterParkInfos.add(new MonsterParkInfo(2, 4, "神祕森林阿爾卡娜", "Arcana", 230, 239, 34478, 954100000));
        monsterParkInfos.add(new MonsterParkInfo(2, 5, "魔菈斯", "Morass", 235, 245, 34269, 954101000));
        monsterParkInfos.add(new MonsterParkInfo(2, 6, "艾斯佩拉", "esfera", 240, 250, 37871, 954102000));
        monsterParkInfos.add(new MonsterParkInfo(2, 7, "賽拉斯", "Sellas", 245, 250, 37921, 954103000));
        monsterParkInfos.add(new MonsterParkInfo(2, 8, "月之橋", "moonBridge", 250, 255, 35632, 954104000));
        monsterParkInfos.add(new MonsterParkInfo(2, 9, "苦痛迷宮", "TheLabyrinthOfSuffering", 255, 260, 36772, 954105000));
        monsterParkInfos.add(new MonsterParkInfo(2, 10, "利曼", "Limen", 260, 265, 35815, 954106000)); // 10-9

        monsterParkInfos.add(new MonsterParkInfo(3, 0, "賽爾尼溫", "Cernium", 260, 265, 39921, 954107000));
        monsterParkInfos.add(new MonsterParkInfo(3, 1, "阿爾克斯", "Arcs", 260, 270, 38120, 954108000));
        monsterParkInfos.add(new MonsterParkInfo(3, 2, "奧迪溫", "odium", 260, 270, 38319, 954109000));
    }

    public static List<QuickMoveInfo> getQuickMoveInfos() {
        return quickMoveInfos;
    }

    private static void initCharExp() {
        charExp[1] = 15;
        charExp[2] = 34;
        charExp[3] = 57;
        charExp[4] = 92;
        charExp[5] = 135;
        charExp[6] = 372;
        charExp[7] = 560;
        charExp[8] = 840;
        charExp[9] = 1242;
        getExpByLvl(charExp, 10, 14, (double) 1);
        getExpByLvl(charExp, 15, 29, (double) 1.2);
        getExpByLvl(charExp, 30, 34, (double) 1);
        getExpByLvl(charExp, 35, 39, (double) 1.2);
        getExpByLvl(charExp, 40, 59, (double) 1.08);
        getExpByLvl(charExp, 60, 64, (double) 1);
        getExpByLvl(charExp, 65, 74, (double) 1.075);
        getExpByLvl(charExp, 75, 89, (double) 1.07);
        getExpByLvl(charExp, 90, 99, (double) 1.065);
        getExpByLvl(charExp, 100, 104, (double) 1);
        getExpByLvl(charExp, 105, 139, (double) 1.065);
        getExpByLvl(charExp, 140, 169, (double) 1.0625);
        charExp[170] = 138750435L;
        getExpByLvl(charExp, 171, 199, (double) 1.05);
        charExp[200] = 2207026470L;
        getExpByLvl(charExp, 201, 209, (double) 1.12);
        getExpByLvl(charExp, 210, 210, (double) 1.3);
        getExpByLvl(charExp, 211, 214, (double) 1.11);
        getExpByLvl(charExp, 215, 215, (double) 1.3);
        getExpByLvl(charExp, 216, 219, (double) 1.09);
        getExpByLvl(charExp, 220, 220, (double) 1.3);
        getExpByLvl(charExp, 221, 224, (double) 1.07);
        getExpByLvl(charExp, 225, 225, (double) 1.3);
        getExpByLvl(charExp, 226, 229, (double) 1.07);
        getExpByLvl(charExp, 230, 230, (double) 1.3);
        getExpByLvl(charExp, 231, 234, (double) 1.03);
        getExpByLvl(charExp, 235, 235, (double) 1.3);
        getExpByLvl(charExp, 236, 239, (double) 1.03);
        getExpByLvl(charExp, 240, 240, (double) 1.3);
        getExpByLvl(charExp, 241, 244, (double) 1.03);
        getExpByLvl(charExp, 245, 245, (double) 1.3);
        getExpByLvl(charExp, 246, 249, (double) 1.03);
        getExpByLvl(charExp, 250, 250, (double) 1.5);
        getExpByLvl(charExp, 251, 259, (double) 1.03);
        getExpByLvl(charExp, 260, 260, (double) 3);
        getExpByLvl(charExp, 261, 264, (double) 1.01);
        getExpByLvl(charExp, 265, 265, (double) 1.3);
        getExpByLvl(charExp, 266, 269, (double) 1.01);
        getExpByLvl(charExp, 270, 270, (double) 2.22);
        getExpByLvl(charExp, 271, 274, (double) 1.01);
        getExpByLvl(charExp, 275, 275, (double) 2.02);
        getExpByLvl(charExp, 276, 279, (double) 1.1);
        getExpByLvl(charExp, 280, 280, (double) 2.02);
        getExpByLvl(charExp, 281, 284, (double) 1.1);
        getExpByLvl(charExp, 285, 285, (double) 2.02);
        getExpByLvl(charExp, 286, 289, (double) 1.1);
        getExpByLvl(charExp, 290, 290, (double) 2.02);
        getExpByLvl(charExp, 291, 294, (double) 1.1);
        getExpByLvl(charExp, 295, 295, (double) 2.02);
        getExpByLvl(charExp, 296, 298, (double) 1.1);
        getExpByLvl(charExp, 299, 299, (double) 1.5);
    }

    public static long getExpByLvl(long[] exp, int minLvl, int maxLvl, double multiplier) {
        float unk = Float.intBitsToFloat(0x3F800000); // 1.0f
        if (exp == null || minLvl < 1 || maxLvl >= exp.length || minLvl > maxLvl) {
            return 0L;
        }
        exp[minLvl] = (long) (exp[minLvl - 1] * multiplier * unk);
        long last = exp[minLvl];
        for (int i = minLvl + 1; i <= maxLvl; i++) {
            last = (long) (exp[i - 1] * multiplier);
            exp[i] = last;
        }
        return last;
    }

    private static void initItemExp() {
        itemExp[1] = 15;
        itemExp[2] = 32;
        itemExp[3] = 57;
        itemExp[4] = 92;
        itemExp[5] = 135;
        itemExp[6] = 372;
        itemExp[7] = 560;
        itemExp[8] = 840;
        itemExp[9] = 1242;
        for (int i = 10; i <= 14; i++) {
            itemExp[i] = itemExp[i - 1];
        }
        for (int i = 15; i <= 30; i++) {
            itemExp[i] = (long) (itemExp[i - 1] * 1.2);
        }
    }

    public static int getMaxStars(Equip equip) {
        int level = equip.getrLevel() + equip.getiIncReq();
        // data/resources/tms282/enhance/starforce-caps.tsv first; the built-in arrays when absent.
        int[][] caps = net.swordie.ms.loaders.enhance.StarForceTable.caps(equip.isSuperiorEqp());
        int stars = Arrays.stream(caps != null ? caps : equip.isSuperiorEqp() ? STARFORCE_LEVELS_SUPERIOR : STARFORCE_LEVELS)
                .filter(lv -> level <= lv[0]).findFirst().get()[1];
        return stars != -1 ? stars : ItemConstants.getItemStarLimit(equip.getItemId());
    }

    public enum EnchantResult {
        SUCCESS,
        MAINTAIN,
        DESTROY
    }

    public static final int RATE_SCALE = 10_000;
    public static int[][] enchantRates;

    private static void initEnchantRates() {
        enchantRates = new int[30][2];
        setRate(0,  9500, 0);
        setRate(1,  9000, 0);
        setRate(2,  8500, 0);
        setRate(3,  8500, 0);
        setRate(4,  8000, 0);

        setRate(5,  7500, 0);
        setRate(6,  7000, 0);
        setRate(7,  6500, 0);
        setRate(8,  6000, 0);
        setRate(9,  5500, 0);

        setRate(10, 4500, 0);
        setRate(11, 3500, 0);
        setRate(12, 3000, 0);
        setRate(13, 3000, 0);
        setRate(14, 3000, 0);

        setRate(15, 3000,  210);
        setRate(16, 3000,  210);
        setRate(17, 1500,  680);
        setRate(18, 1500,  680);
        setRate(19, 1500,  850);
        setRate(20, 3000, 1050);
        setRate(21, 1500, 1275);
        setRate(22, 1500, 1700);
        setRate(23, 1000, 1800);
        setRate(24, 1000, 1800);
        setRate(25, 1000, 1800);
        setRate(26,  700, 1860);
        setRate(27,  500, 1900);
        setRate(28,  300, 1940);
        setRate(29,  100, 1980);
        // TMS 279 hyper_upgrade.toml rows (starforce.tsv) replace the built-in rates star by star.
        try {
            for (int star = 0; star < enchantRates.length; star++) {
                var row = net.swordie.ms.loaders.enhance.StarForceTable.row(star, false);
                if (row != null) setRate(star, row.success(), row.destroy());
            }
        } catch (Throwable t) {
            System.err.println("[enhance] star force table ignored: " + t);
        }
    }

    private static void setRate(int star, int success, int destroy) {
        if (success < 0 || destroy < 0 || success + destroy > RATE_SCALE) {
            throw new IllegalArgumentException("Bad rate at star " + star);
        }
        enchantRates[star][0] = success;
        enchantRates[star][1] = destroy;
    }

    public static EnchantResult rollEnchant(int star,
                                            int vipGrade,
                                            boolean extraChanceFromMiniGame,
                                            boolean safeGuard,
                                            boolean safeGuardFromUI,
                                            boolean canSafeguard) {
        int successProp = enchantRates[star][0];
        int destroyProp = enchantRates[star][1];

        // VIP / minigame -> success
        if (vipGrade > 0) {
            successProp = (int) Math.round(successProp * (1.0 + 0.025 * vipGrade));
        }
        if (extraChanceFromMiniGame) {
            successProp = (int) Math.round(successProp * 1.045);
        }

        // safeguard -> destroy = 0
        if ((safeGuard || safeGuardFromUI) && canSafeguard) {
            destroyProp = 0;
        }

        // clamp để tổng <= 100%
        if (successProp > RATE_SCALE - destroyProp) {
            successProp = RATE_SCALE - destroyProp;
        }

        int r = ThreadLocalRandom.current().nextInt(RATE_SCALE);
        if (r < successProp) {
            return EnchantResult.SUCCESS;
        }
        if (r < successProp + destroyProp) {
            return EnchantResult.DESTROY;
        }
        return EnchantResult.MAINTAIN;
    }

    private static void initEnchantRatesSuperior() {
        enchantSuccessRatesSuperior = new int[][]{
                {500, 0},
                {500, 0},
                {450, 0},
                {400, 0},
                {400, 0},

                {400, 18},
                {400, 30},
                {400, 42},
                {400, 60},
                {370, 95},

                {350, 130},
                {350, 162},
                {30, 485},
                {20, 490},
                {10, 500},
        };
        // This array keeps its per-1000 scale; starforce.tsv is per 10000.
        try {
            for (int star = 0; star < enchantSuccessRatesSuperior.length; star++) {
                var row = net.swordie.ms.loaders.enhance.StarForceTable.row(star, true);
                if (row != null) enchantSuccessRatesSuperior[star] = new int[]{row.success() / 10, row.destroy() / 10};
            }
        } catch (Throwable t) {
            System.err.println("[enhance] superior star force table ignored: " + t);
        }
    }

    /** Whether a failed attempt from {@code star} should drop a star (279 {@code down}). Not applied by the handler yet. */
    public static boolean isStarDownOnFailure(int star, boolean superior) {
        var row = net.swordie.ms.loaders.enhance.StarForceTable.row(star, superior);
        return row != null && row.down();
    }

    private static void initDailyGiftItems() {
        dailyItems = new ArrayList<>();
        dailyItems.add(new DailyGiftItemInfo(1, 5040004, 1, 0)); // Hyper Teleport Rock
        dailyItems.add(new DailyGiftItemInfo(2, 2450064, 2, 0)); // 2x EXP Coupon (30 min) x2
        dailyItems.add(new DailyGiftItemInfo(3, 1122017, 1, 0)); // Pendant of the Spirit
        dailyItems.add(new DailyGiftItemInfo(4, 2710000, 10, 0)); // Occult Cube x 10
        dailyItems.add(new DailyGiftItemInfo(5, 4001832, 500, 0)); // Spell Trace x 500
        dailyItems.add(new DailyGiftItemInfo(6, 2434288, 1, 0)); // Special Medal of Honor
        dailyItems.add(new DailyGiftItemInfo(7, 2432299, 1, 0)); // 500 Reward Points
        dailyItems.add(new DailyGiftItemInfo(8, 2048400, 1, 0)); // Mastery Book Synergizer
        dailyItems.add(new DailyGiftItemInfo(9, 2450064, 2, 0)); // 2x EXP Coupon (30 min) x2
        dailyItems.add(new DailyGiftItemInfo(10, 2434665, 1, 0)); // Shao Spirit Totem Coupon
        dailyItems.add(new DailyGiftItemInfo(11, 2434020, 1, 0)); // Powerful Rebirth Flame
        dailyItems.add(new DailyGiftItemInfo(12, 2434270, 1, 0)); // Selective 8 Slot Coupon
        dailyItems.add(new DailyGiftItemInfo(13, 2350000, 1, 0)); // Character Slot Expansion Coupon
        dailyItems.add(new DailyGiftItemInfo(14, 5062009, 1, 0)); // Red Cube
        dailyItems.add(new DailyGiftItemInfo(15, 5040004, 1, 0)); // Hyper Teleport Rock
        dailyItems.add(new DailyGiftItemInfo(16, 2450064, 2, 0)); // 2x EXP Coupon (30 min) x2
        dailyItems.add(new DailyGiftItemInfo(17, 1122017, 1, 0)); // Pendant of the Spirit
        dailyItems.add(new DailyGiftItemInfo(18, 5062800, 1, 0)); // Miracle Circulator
        dailyItems.add(new DailyGiftItemInfo(19, 2434665, 1, 0)); // Shao Spirit Totem Coupon
        dailyItems.add(new DailyGiftItemInfo(20, 2434921, 1, 0)); // Trait Boost Potion
        dailyItems.add(new DailyGiftItemInfo(21, 2434964, 1, 0)); // 1000 Reward Points
        dailyItems.add(new DailyGiftItemInfo(22, 2023072, 1, 0)); // 2x Drop Coupon
        dailyItems.add(new DailyGiftItemInfo(23, 2434270, 1, 0)); // Selective 8 Slot Coupon
        dailyItems.add(new DailyGiftItemInfo(24, 2028048, 5, 0)); // Mysterious Meso Pouch x 5
        dailyItems.add(new DailyGiftItemInfo(25, 2048717, 1, 0)); // Eternal Flame of Rebirth
        dailyItems.add(new DailyGiftItemInfo(26, 5680382, 1, 0)); // Epic Potential Scroll 50
        dailyItems.add(new DailyGiftItemInfo(27, 2028048, 20, 0)); // Mysterious Meso Pouch x 20
        dailyItems.add(new DailyGiftItemInfo(28, 5062010, 1, 0)); // Black Cube
    }

    public static long getEnchantmentMesoCost(int level, int star, boolean superior) {
        var row = net.swordie.ms.loaders.enhance.StarForceTable.row(star, superior);
        if (row != null && row.mesoCost() >= 0) {
            return row.mesoCost();
        }
        double cost;

        if (superior) {
            cost = 1000 + Math.pow(level, 3.56);
            return Math.round(cost / 100) * 100;
        }

        if (star < 10) {
            cost = 1000 + Math.pow(level, 3) * (star + 1) / 25.0;
        } else if (star == 10) {
            cost = 1000 + Math.pow(level, 3) * Math.pow(11, 2.7) / 400.0;
        } else if (star == 11) {
            cost = 1000 + Math.pow(level, 3) * Math.pow(12, 2.7) / 220.0;
        } else if (star == 12) {
            cost = 1000 + Math.pow(level, 3) * Math.pow(13, 2.7) / 150.0;
        } else if (star == 13) {
            cost = 1000 + Math.pow(level, 3) * Math.pow(14, 2.7) / 110.0;
        } else if (star == 14) {
            cost = 1000 + Math.pow(level, 3) * Math.pow(15, 2.7) / 75.0;
        } else if (star == 15 || star == 16) {
            cost = 1000 + Math.pow(level, 3) * Math.pow(star + 1, 2.7) / 200.0;
        } else if (star == 17) {
            cost = 1000 + Math.pow(level, 3) * Math.pow(18, 2.7) / 150.0;
        } else if (star == 18) {
            cost = 1000 + Math.pow(level, 3) * Math.pow(19, 2.7) / 70.0;
        } else if (star == 19) {
            cost = 1000 + Math.pow(level, 3) * Math.pow(20, 2.7) / 45.0;
        } else if (star == 20) {
            cost = 1000 + Math.pow(level, 3) * Math.pow(21, 2.7) / 200.0;
        } else if (star == 21) {
            cost = 1000 + Math.pow(level, 3) * Math.pow(22, 2.7) / 125.0;
        } else {
            cost = 1000 + Math.pow(level, 3) * Math.pow(star + 1, 2.7) / 200.0;
        }

        return Math.round(cost / 100) * 100;
    }

    public static int getEnchantmentSuccessRate(Equip equip) {
        if (equip.getDropStreak() >= net.swordie.ms.loaders.enhance.StarForceTable.chanceTimeDropStreak(2)) {
            return 1000;
        }
        int chuc = equip.getChuc();
        if (chuc < 0 || chuc > 24) {
            return 0;
        } else if (equip.isSuperiorEqp()) {
            return enchantSuccessRatesSuperior[chuc][0];
        } else {
            return enchantRates[chuc][0];
        }
    }

    public static int getEnchantmentDestroyRate(Equip equip) {
        if (equip.getDropStreak() >= net.swordie.ms.loaders.enhance.StarForceTable.chanceTimeDropStreak(2)) {
            return 0;
        }
        int chuc = equip.getChuc();
        if (chuc < 0 || chuc > 30) {
            return 0;
        } else if (equip.isSuperiorEqp()) {
            return enchantSuccessRatesSuperior[chuc][1];
        } else {
            return enchantRates[chuc][1];
        }
    }

    public static int getStatForSuperiorEnhancement(int reqLevel, short chuc) {
        if (chuc == 0) {
            return reqLevel < 110 ? 2 : reqLevel < 149 ? 9 : 19;
        } else if (chuc == 1) {
            return reqLevel < 110 ? 3 : reqLevel < 149 ? 10 : 20;
        } else if (chuc == 2) {
            return reqLevel < 110 ? 5 : reqLevel < 149 ? 12 : 22;
        } else if (chuc == 3) {
            return reqLevel < 149 ? 15 : 25;
        } else if (chuc == 4) {
            return reqLevel < 149 ? 19 : 29;
        }
        return 0;
    }

    public static int getAttForSuperiorEnhancement(int reqLevel, short chuc) {
        if (chuc == 5) {
            return reqLevel < 150 ? 5 : 9;
        } else if (chuc == 6) {
            return reqLevel < 150 ? 6 : 10;
        } else if (chuc == 7) {
            return reqLevel < 150 ? 7 : 11;
        } else {
            return chuc == 8 ? 12 : chuc == 9 ? 13 : chuc == 10 ? 15 : chuc == 11 ? 17 : chuc == 12 ? 19 : chuc == 13 ? 21 : chuc == 14 ? 23 : 0;
        }
    }

    // ugliest function in swordie
    public static int getEquipStatBoost(Equip equip, EnchantStat es, short chuc) {
        int stat = 0;
        // hp/mp
        if (es == EnchantStat.MHP || es == EnchantStat.MMP) {
            stat += chuc <= 2 ? 5 : chuc <= 4 ? 10 : chuc <= 6 ? 15 : chuc <= 8 ? 20 : chuc <= 14 ? 25 : 0;
        }
        int reqLevel = equip.getrLevel() + equip.getiIncReq();
        // all stat
        if (es == EnchantStat.STR || es == EnchantStat.DEX || es == EnchantStat.INT || es == EnchantStat.LUK) {
            if (chuc <= 4) {
                stat += 2;
            } else if (chuc <= 14) {
                stat += 3;
            } else if (chuc <= 21) {
                stat += reqLevel <= 137 ? 7 : reqLevel <= 149 ? 9 : reqLevel <= 159 ? 11 : reqLevel <= 199 ? 13 : 15;
            }
        }
        // att for all equips
        if ((es == EnchantStat.PAD || es == EnchantStat.MAD) && chuc >= 15) {
            if (chuc == 15) {
                stat += reqLevel <= 137 ? 6 : reqLevel <= 149 ? 7 : reqLevel <= 159 ? 8 : reqLevel <= 199 ? 9 : 12;
            } else if (chuc == 16) {
                stat += reqLevel <= 137 ? 7 : reqLevel <= 149 ? 8 : reqLevel <= 159 ? 9 : reqLevel <= 199 ? 9 : 13;
            } else if (chuc == 17) {
                stat += reqLevel <= 137 ? 7 : reqLevel <= 149 ? 8 : reqLevel <= 159 ? 9 : reqLevel <= 199 ? 10 : 14;
            } else if (chuc == 18) {
                stat += reqLevel <= 137 ? 8 : reqLevel <= 149 ? 9 : reqLevel <= 159 ? 10 : reqLevel <= 199 ? 11 : 14;
            } else if (chuc == 19) {
                stat += reqLevel <= 137 ? 9 : reqLevel <= 149 ? 10 : reqLevel <= 159 ? 11 : reqLevel <= 199 ? 12 : 15;
            } else if (chuc == 20) {
                stat += reqLevel <= 149 ? 11 : reqLevel <= 159 ? 12 : reqLevel <= 199 ? 13 : 16;
            } else if (chuc == 21) {
                stat += reqLevel <= 149 ? 12 : reqLevel <= 159 ? 13 : reqLevel <= 199 ? 14 : 17;
            } else if (chuc == 22) {
                stat += reqLevel <= 149 ? 17 : reqLevel <= 159 ? 18 : reqLevel <= 199 ? 19 : 21;
            } else if (chuc == 23) {
                stat += reqLevel <= 149 ? 19 : reqLevel <= 159 ? 20 : reqLevel <= 199 ? 21 : 23;
            } else if (chuc == 24) {
                stat += reqLevel <= 149 ? 21 : reqLevel <= 159 ? 22 : reqLevel <= 199 ? 23 : 25;
            } else if (chuc == 25) {
                stat += reqLevel <= 149 ? 23 : reqLevel <= 159 ? 24 : reqLevel <= 199 ? 25 : 27;
            } else if (chuc == 26) {
                stat += reqLevel <= 149 ? 25 : reqLevel <= 159 ? 26 : reqLevel <= 199 ? 27 : 29;
            } else if (chuc == 27) {
                stat += reqLevel <= 149 ? 27 : reqLevel <= 159 ? 28 : reqLevel <= 199 ? 29 : 31;
            } else if (chuc == 28) {
                stat += reqLevel <= 149 ? 29 : reqLevel <= 159 ? 30 : reqLevel <= 199 ? 31 : 33;
            } else if (chuc == 29) {
                stat += reqLevel <= 149 ? 31 : reqLevel <= 159 ? 32 : reqLevel <= 199 ? 33 : 35;
            } else if (chuc == 30) {
                stat += reqLevel <= 149 ? 33 : reqLevel <= 159 ? 34 : reqLevel <= 199 ? 35 : 37;
            }
        }
        // att gains for weapons
        if (ItemConstants.isWeapon(equip.getItemId()) && !ItemConstants.isSecondary(equip.getItemId())) {
            if (chuc <= 14) {
                if (es == EnchantStat.PAD) {
                    stat += equip.getiPad() * 0.02;
                } else if (es == EnchantStat.MAD) {
                    stat += equip.getiMad() * 0.02;
                }
            } else if (es == EnchantStat.PAD || es == EnchantStat.MAD) {
                switch (chuc) {
                    case 22:
                        stat += 13;
                        break;
                    case 23:
                        stat += 12;
                        break;
                    case 24:
                        stat += 11;
                        break;
                    case 25:
                        stat += 10;
                        break;
                    case 27:
                        stat += 9;
                        break;
                    case 28:
                        stat += 8;
                        break;
                    case 29:
                        stat += 7;
                        break;
                }
                if (reqLevel == 200 && chuc == 15) {
                    stat += 1;
                }
            }
        }
        // att gain for gloves, enhancements 4/6/8/10 and 12-14
        if (ItemConstants.isGlove(equip.getItemId()) && (es == EnchantStat.PAD || es == EnchantStat.MAD)) {
            if ((chuc <= 10 && chuc % 2 == 0) || (chuc >= 12 && chuc <= 14)) {
                stat += 1;
            }
        }
        // speed/jump for shoes
        if (ItemConstants.isShoe(equip.getItemId()) && (es == EnchantStat.SPEED || es == EnchantStat.JUMP) && chuc <= 4) {
            stat += 1;
        }
        return stat;
    }

    public static int getEnchantmentValByChuc(Equip equip, EnchantStat es, short chuc, int curAmount) {
        if (equip.isCash() || (ItemData.getEquipById(equip.getItemId()) != null && ItemData.getEquipById(equip.getItemId()).getTuc() <= 0 && !ItemConstants.isTucIgnoreItem(equip.getItemId()))) {
            return 0;
        }
        if (es == EnchantStat.PDD) {
            return (int) (equip.getiPDD() * (ItemConstants.isOverall(equip.getItemId()) ? 0.10 : 0.05));
        }
        if (es == EnchantStat.MDD) {
            return (int) (equip.getiMDD() * (ItemConstants.isOverall(equip.getItemId()) ? 0.10 : 0.05));
        }
        if (!equip.isSuperiorEqp()) {
            return getEquipStatBoost(equip, es, chuc);
        } else {
            if (es == EnchantStat.STR || es == EnchantStat.DEX || es == EnchantStat.INT || es == EnchantStat.LUK) {
                return getStatForSuperiorEnhancement(equip.getrLevel() + equip.getiIncReq(), chuc);
            }
            if (es == EnchantStat.PAD || es == EnchantStat.MAD) {
                return getAttForSuperiorEnhancement(equip.getrLevel() + equip.getiIncReq(), chuc);
            }
        }
        return 0;
    }

    public static BaseStat getMainBaseStatForJob(short job) {
        if (JobConstants.isBuccaneer(job) || JobConstants.isAdventurerPirate(job)
                || JobConstants.isPinkBean(job) || JobConstants.isYeti(job) || JobConstants.isDawnWarrior(job)
                || JobConstants.isKaiser(job) || JobConstants.isZero(job) || JobConstants.isDemon(job)
                || JobConstants.isDemonSlayer(job) || JobConstants.isAran(job) || JobConstants.isCannoneer(job)
                || JobConstants.isDarkKnight(job) || JobConstants.isHero(job) || JobConstants.isPaladin(job)
                || JobConstants.isBlaster(job) || JobConstants.isHayato(job) || JobConstants.isMihile(job)
                || JobConstants.isShade(job) || JobConstants.isThunderBreaker(job) || JobConstants.isAdventurerWarrior(job)
                || JobConstants.isArk(job) || JobConstants.isAdele(job) || JobConstants.isRen(job)
                || JobConstants.isTanjiro(job) || JobConstants.isSaitama(job)) {
            return BaseStat.str;
        } else if (JobConstants.isJett(job) || JobConstants.isCorsair(job) || JobConstants.isWildHunter(job)
                || JobConstants.isMercedes(job) || JobConstants.isAngelicBuster(job) || JobConstants.isWindArcher(job)
                || JobConstants.isAdventurerArcher(job) || JobConstants.isPathFinder(job) || JobConstants.isKain(job)
                || JobConstants.isMoXuan(job)) {
            return BaseStat.dex;
        } else if (JobConstants.isBeastTamer(job) || JobConstants.isBlazeWizard(job) || JobConstants.isBishop(job)
                || JobConstants.isEvan(job) || JobConstants.isIceLightning(job) || JobConstants.isFirePoison(job)
                || JobConstants.isAdventurerMage(job) || JobConstants.isKanna(job) || JobConstants.isKinesis(job)
                || JobConstants.isLuminous(job) || JobConstants.isBattleMage(job) || JobConstants.isIllium(job)
                || JobConstants.isLara(job) || JobConstants.isLynn(job) || JobConstants.isSiaAstelle(job)) {
            return BaseStat.inte;
        } else if (JobConstants.isAdventurerThief(job) || JobConstants.isNightLord(job) || JobConstants.isShadower(job)
                || JobConstants.isPhantom(job) || JobConstants.isNightWalker(job) || JobConstants.isDualBlade(job)
                || JobConstants.isCadena(job) || JobConstants.isHoYoung(job) || JobConstants.isKhali(job)) {
            return BaseStat.luk;
        } else if (JobConstants.isDemonAvenger(job)) {
            return BaseStat.mhp;
        }
        return null;
    }

    public static ItemJob getItemJobByJob(int jobArg) {
        short job = (short) jobArg;
        if (JobConstants.isPinkBean(job) || JobConstants.isDawnWarrior(job) || JobConstants.isKaiser(job)
                || JobConstants.isZero(job) || JobConstants.isDemon(job) || JobConstants.isDemonSlayer(job)
                || JobConstants.isAran(job) || JobConstants.isDarkKnight(job) || JobConstants.isHero(job)
                || JobConstants.isPaladin(job) || JobConstants.isBlaster(job) || JobConstants.isHayato(job)
                || JobConstants.isMihile(job) || JobConstants.isAdventurerWarrior(job) || JobConstants.isAdele(job)
                || JobConstants.isRen(job) || JobConstants.isTanjiro(job)) {
            return ItemJob.WARRIOR;
        }
        if (JobConstants.isWildHunter(job) || JobConstants.isMercedes(job) || JobConstants.isWindArcher(job) ||
                JobConstants.isAdventurerArcher(job) || JobConstants.isPathFinder(job) || JobConstants.isKain(job)) {
            return ItemJob.BOWMAN;
        }
        if (JobConstants.isBeastTamer(job) || JobConstants.isBlazeWizard(job) || JobConstants.isBishop(job)
                || JobConstants.isEvan(job) || JobConstants.isIceLightning(job) || JobConstants.isFirePoison(job)
                || JobConstants.isAdventurerMage(job) || JobConstants.isKanna(job) || JobConstants.isBattleMage(job)
                || JobConstants.isKinesis(job) || JobConstants.isLuminous(job) || JobConstants.isIllium(job)
                || JobConstants.isLara(job) || JobConstants.isLynn(job) || JobConstants.isSiaAstelle(job)) {
            return ItemJob.MAGICIAN;
        }
        if (JobConstants.isAdventurerThief(job) || JobConstants.isNightLord(job) || JobConstants.isShadower(job)
                || JobConstants.isPhantom(job) || JobConstants.isNightWalker(job) || JobConstants.isDualBlade(job)
                || JobConstants.isCadena(job) || JobConstants.isHoYoung(job) || JobConstants.isKhali(job)) {
            return ItemJob.THIEF;
        }
        if (JobConstants.isBuccaneer(job) || JobConstants.isAdventurerPirate(job) || JobConstants.isCannoneer(job)
                || JobConstants.isShade(job) || JobConstants.isThunderBreaker(job) || JobConstants.isCorsair(job)
                || JobConstants.isAngelicBuster(job) || JobConstants.isJett(job) || JobConstants.isArk(job)
                || JobConstants.isXenon(job) || JobConstants.isMechanic(job) || JobConstants.isMoXuan(job)
                || JobConstants.isYeti(job) || JobConstants.isSaitama(job)) {
            return ItemJob.PIRATE;
        } else {
            return ItemJob.BEGINNER;
        }
    }

    public static BaseStat getSecStatByMainStat(BaseStat mainStat) {
        if (mainStat == null) {
            return null;
        }
        return switch (mainStat) {
            case str -> BaseStat.dex;
            case dex -> BaseStat.str;
            case inte -> BaseStat.luk;
            case luk -> BaseStat.dex;
            default -> null;
        };
    }

    public static double getExpOrbExpModifierById(int itemID) {
        return switch (itemID) {
            case BLUE_EXP_ORB_ID -> BLUE_EXP_ORB_MULT;
            case PURPLE_EXP_ORB_ID -> PURPLE_EXP_ORB_MULT;
            case RED_EXP_ORB_ID -> RED_EXP_ORB_MULT;
            case YELLOW_EXP_ORB_ID -> YELLOW_EXP_ORB_MULT;
            default -> 0;
        };
    }

    /**
     * Gets a list of possible elite stats by mob level.
     *
     * @param level the level of the mob
     * @return list of Triples, each triple indicating the level (left), extra hp rate (mid) and the extra exp/meso drop rate (right).
     */
    public static List<Triple<Integer, Double, Double>> getEliteInfoByMobLevel(int level) {
        List<Triple<Integer, Double, Double>> list = new ArrayList<>();
        if (level < 10) {
            list.add(new Triple<>(0, 21D, 10.5));
        } else if (level >= 10 && level < 20) {
            list.add(new Triple<>(1, 29D, 14.5));
        } else if (level >= 20 && level < 30) {
            list.add(new Triple<>(2, 38D, 19D));
        } else if (level >= 30 && level < 40) {
            list.add(new Triple<>(3, 47D, 21D));
        } else if (level >= 40 && level < 50) {
            list.add(new Triple<>(4, 60D, 23D));
        } else if (level >= 50 && level < 60) {
            list.add(new Triple<>(5, 70D, 25D));
        } else if (level >= 60 && level < 70) {
            list.add(new Triple<>(6, 80D, 28D));
        } else if (level >= 70 && level < 80) {
            list.add(new Triple<>(7, 100D, 31D));
        } else if (level >= 80 && level < 90) {
            list.add(new Triple<>(8, 120D, 35D));
        } else if (level >= 90 && level < 100) {
            list.add(new Triple<>(9, 150D, 40D));
        } else if (level >= 100 && level < 110) {
            list.add(new Triple<>(10, 150D, 38D));
        } else if (level >= 110 && level < 120) {
            list.add(new Triple<>(11, 180D, 40D));
        } else if (level >= 120 && level < 130) {
            list.add(new Triple<>(12, 200D, 42D));
        } else if (level >= 130 && level < 140) {
            list.add(new Triple<>(13, 240D, 44D));
        } else if (level >= 140 && level < 150) {
            list.add(new Triple<>(14, 280D, 46D));
        } else if (level >= 150 && level < 160) {
            list.add(new Triple<>(15, 320D, 48D));
        } else if (level >= 160 && level < 170) {
            list.add(new Triple<>(16, 360D, 50D));
        } else if (level >= 170 && level < 180) {
            list.add(new Triple<>(17, 400D, 52D));
        } else if (level >= 180 && level < 190) {
            list.add(new Triple<>(18, 450D, 55D));
        } else if (level >= 190 && level < 200) {
            list.add(new Triple<>(19, 500D, 57D));
        } else if (level >= 200 && level < 210) {
            list.add(new Triple<>(20, 500D, 58D));
        } else if (level >= 210 && level < 220) {
            list.add(new Triple<>(21, 500D, 60D));
            list.add(new Triple<>(22, 500D, 61D));
            list.add(new Triple<>(23, 500D, 62D));
            list.add(new Triple<>(24, 500D, 63D));
        } else { // > 220
            list.add(new Triple<>(25, 600D, 65D));
            list.add(new Triple<>(26, 600D, 66D));
            list.add(new Triple<>(27, 600D, 67D));
        }
        return list;
    }

    public static double getPartyExpRateByAttackersAndLeechers(int attackers, int leechers) {
        if (leechers == 1) {
            return 0; // Just 1 attacker
        }
        if (attackers >= 3) {
            return switch (leechers) {
                case 6 -> 1.95;
                case 5 -> 1.5;
                case 4 -> 1.1;
                default -> 0.75;
            };
        } else {
            return switch (leechers) {
                case 6 -> 1.65 + attackers * 0.1;
                case 5 -> 1.2 + attackers * 0.1;
                case 4 -> 0.8 + attackers * 0.1;
                case 3 -> 0.4 + attackers * 0.1;
                default -> 0.15 + attackers * 0.1;
            };
        }
    }

    public static long applyTax(long money) {
        // 5% global tax starting from v180ish
        return Math.round(money * 0.95);
    }

    // -1 if not in Maplerunner
    // 0 for lobby
    // N (1-50) for Maplerunner stages
    public static int getMaplerunnerField(int fieldId) {
        // Forest of Tenacity prefix
        if (fieldId / 1000 != 993001) {
            return -1;
        }
        return fieldId % 1000 / 10;
    }

    public static int[] getHpMpPerLevel(short job) {
        var mainStat = GameConstants.getMainBaseStatForJob(job);
        int hp = HP_PER_LEVEL;
        int mp = MP_PER_LEVEL;
        if (mainStat == BaseStat.str) {
            hp *= STR_HP_MULT;
            mp *= STR_MP_MULT;
        } else if (mainStat == BaseStat.inte) {
            hp *= INT_HP_MULT;
            mp *= INT_MP_MULT;
        }
        return new int[]{hp, mp};
    }

    public static boolean isValidName(String name) {
        return name.length() >= 4 && name.length() <= 13 && Util.isDigitLetterString(name);
    }

    public static boolean isValidEmotion(int emotion) {
        return emotion >= 0 && emotion <= 10;
    }

    public static boolean isFreeMarketField(int id) {
        // room 1~22
        return id > 910000000 && id < 910001000;
    }

    public static ReactorType getReactorType(int reactorId) {
        int prefix = reactorId / 100000;
        return switch (prefix) {
            case 1 -> ReactorType.HERB;
            case 2 -> ReactorType.VEIN;
            default -> null;
        };
    }

    public static int getDemonAvengerMinHP(int job, int level) {
        return switch (job) {
            case 3001 -> 50;
            case 3101 -> 250;
            case 3121, 3122 -> 550;
            default -> 0;
        };
    }

    public static String formatDamageCompact(long dmg) {
        if (dmg < 1_000L) return Long.toString(dmg);

        long val;
        char s;
        if (dmg >= 100_000_000_000L) { val = dmg / 100_000_000_000L; s = 'T'; }
        else if (dmg >= 1_000_000_000L) { val = dmg / 1_000_000_000L; s = 'B'; }
        else if (dmg >= 1_000_000L) { val = dmg / 1_000_000L; s = 'M'; }
        else { val = dmg / 1_000L; s = 'K'; }

        return Long.toString(val) + s;
    }

    public static int percentInt(long damage, long maxHp) {
        if (maxHp <= 0 || damage <= 0) return 0;
        long pct;
        if (damage > Long.MAX_VALUE / 100L) {
            pct = (damage / maxHp) * 100L;
        } else {
            pct = (damage * 100L) / maxHp;
        }
        if (pct < 0) return 0;
        if (pct > 100) return 100;
        return (int) pct;
    }

    public static int getCoreValue(int coreID) {
        return switch (coreID) {
            // Changes Evolution System's Monster Level + Level
            case 3600000 -> 3;
            case 3600001 -> 4;
            case 3600002 -> 5;
            case 3600003 -> 7;
            case 3600004 -> 9;
            case 3600005 -> 10;
            // Increases Evolution System Monster's HP. + HP %
            case 3600100 -> 20;
            case 3600101 -> 50;
            case 3600102 -> 400;
            case 3600103 -> 900;
            case 3600104 -> 1900;
            // Increases the number of Monsters that show up in the Evolution System. + MobGen %
            case 3600200 -> 20;
            case 3600201 -> 30;
            case 3600202 -> 70;
            case 3600203 -> 100;
            case 3600204 -> // (Unique Core) Monster Population +50%
                    50;
            // Grants extra EXP for Party Hunting. + Exp %
            case 3602000 -> 5;
            case 3602001 -> 10;
            case 3602002 -> 15;
            case 3602003 -> // (Unique Core) Party EXP +5%
                    5;
            // Increases item drop rate. + DropRate %
            case 3603000 -> 5;
            case 3603001 -> 10;
            case 3603002 -> 15;
            case 3603003 -> 20;
            case 3603004 -> 25;
            case 3603005 -> 30;
            case 3603006 -> // (Unique Core) Drop Rate +20%
                    20;
            default -> 0;
        };
    }

    public static int getTraitExpByLevel(int level) {
        return switch (level) {
            case 0 -> 20;
            case 1 -> 26;
            case 2 -> 34;
            case 3 -> 44;
            case 4 -> 57;
            case 5 -> 74;
            case 6 -> 96;
            case 7 -> 125;
            case 8 -> 163;
            case 9 -> 212;
            case 10 -> 233;
            case 11 -> 256;
            case 12 -> 282;
            case 13 -> 310;
            case 14 -> 341;
            case 15 -> 375;
            case 16 -> 413;
            case 17 -> 454;
            case 18 -> 499;
            case 19 -> 549;
            case 20 -> 565;
            case 21 -> 582;
            case 22 -> 599;
            case 23 -> 617;
            case 24 -> 636;
            case 25 -> 655;
            case 26 -> 675;
            case 27 -> 695;
            case 28 -> 716;
            case 29 -> 737;
            case 30 -> 748;
            case 31 -> 759;
            case 32 -> 770;
            case 33 -> 782;
            case 34 -> 794;
            case 35 -> 806;
            case 36 -> 818;
            case 37 -> 830;
            case 38 -> 842;
            case 39 -> 855;
            case 40 -> 868;
            case 41 -> 881;
            case 42 -> 894;
            case 43 -> 907;
            case 44 -> 921;
            case 45 -> 935;
            case 46 -> 949;
            case 47 -> 963;
            case 48 -> 977;
            case 49 -> 992;
            case 50 -> 1007;
            case 51 -> 1022;
            case 52 -> 1037;
            case 53 -> 1053;
            case 54 -> 1069;
            case 55 -> 1085;
            case 56 -> 1101;
            case 57 -> 1118;
            case 58 -> 1135;
            case 59 -> 1152;
            case 60 -> 1169;
            case 61 -> 1187;
            case 62 -> 1205;
            case 63 -> 1223;
            case 64 -> 1241;
            case 65 -> 1260;
            case 66 -> 1279;
            case 67 -> 1298;
            case 68 -> 1317;
            case 69 -> 1337;
            case 70 -> 1341;
            case 71 -> 1345;
            case 72 -> 1349;
            case 73 -> 1353;
            case 74 -> 1357;
            case 75 -> 1361;
            case 76 -> 1365;
            case 77 -> 1369;
            case 78 -> 1373;
            case 79 -> 1377;
            case 80 -> 1381;
            case 81 -> 1385;
            case 82 -> 1389;
            case 83 -> 1393;
            case 84 -> 1397;
            case 85 -> 1401;
            case 86 -> 1405;
            case 87 -> 1409;
            case 88 -> 1413;
            case 89 -> 1417;
            case 90 -> 1421;
            case 91 -> 1425;
            case 92 -> 1429;
            case 93 -> 1433;
            case 94 -> 1437;
            case 95 -> 1441;
            case 96 -> 1445;
            case 97 -> 1449;
            case 98 -> 1453;
            case 99 -> 1457;
            default -> 0;
        };
    }

    public static int getHpMpByTraitLevel(int level) {
        if (level >= 5 && level <= 9) {
            return 100;
        } else if (level >= 10 && level <= 14) {
            return 200;
        } else if (level >= 15 && level <= 19) {
            return 300;
        } else if (level >= 20 && level <= 24) {
            return 400;
        } else if (level >= 25 && level <= 29) {
            return 500;
        } else if (level >= 30 && level <= 34) {
            return 600;
        } else if (level >= 35 && level <= 39) {
            return 800;
        } else if (level >= 40 && level <= 44) {
            return 800;
        } else if (level >= 45 && level <= 49) {
            return 900;
        } else if (level >= 50 && level <= 54) {
            return 1000;
        } else if (level >= 55 && level <= 59) {
            return 1100;
        } else if (level >= 60 && level <= 64) {
            return 1200;
        } else if (level >= 65 && level <= 69) {
            return 1300;
        } else if (level >= 70 && level <= 74) {
            return 1400;
        } else if (level >= 75 && level <= 79) {
            return 1500;
        } else if (level >= 80 && level <= 84) {
            return 1600;
        } else if (level >= 85 && level <= 89) {
            return 1700;
        } else if (level >= 90 && level <= 94) {
            return 1800;
        } else if (level >= 95 && level <= 99) {
            return 1900;
        } else if (level == 100) {
            return 2000;
        }
        return 0;
    }

    public static int getBuffDurationByTraitLevel(int level) {
        //Lazy find recipe
        if (level >= 10 && level <= 19) {
            return 1;
        } else if (level >= 20 && level <= 29) {
            return 2;
        } else if (level >= 30 && level <= 39) {
            return 3;
        } else if (level >= 40 && level <= 49) {
            return 4;
        } else if (level >= 50 && level <= 59) {
            return 5;
        } else if (level >= 60 && level <= 69) {
            return 6;
        } else if (level >= 70 && level <= 79) {
            return 7;
        } else if (level >= 80 && level <= 89) {
            return 8;
        } else if (level >= 90 && level <= 99) {
            return 9;
        } else if (level == 100) {
            return 10;
        }
        return 0;
    }

    public static int getScrollSuccessRateByTraitLevel(int level) {
        if (level >= 5 && level <= 14) {
            return 1;
        } else if (level >= 15 && level <= 24) {
            return 2;
        } else if (level >= 25 && level <= 34) {
            return 3;
        } else if (level >= 35 && level <= 44) {
            return 4;
        } else if (level >= 45 && level <= 54) {
            return 5;
        } else if (level >= 55 && level <= 64) {
            return 6;
        } else if (level >= 65 && level <= 74) {
            return 7;
        } else if (level >= 75 && level <= 84) {
            return 8;
        } else if (level >= 85 && level <= 94) {
            return 9;
        } else if (level >= 95 && level <= 100) {
            return 10;
        }
        return 0;
    }

    public static boolean isMuLungDojoMob(int templateID) {
        // 282 dojo floors (incl. stage 80+ bosses): 9305600..9305728
        return templateID >= 9305600 && templateID <= 9305728;
    }

    public static boolean isRedLeafHigh(int templateID) {
        return (templateID >= 9410199 && templateID <= 9410219 && templateID != 9410217) || templateID == 9410248;
    }

    public static boolean isVanishingJourney(int mobID) {
        return switch (mobID) {
            case 8641000, 8641001, 8641002, 8641003, 8641004, 8641005, 8641006, 8641007, 8641008, 8641009, 8641012, 8641013, 8641014, 8641015 ->
                    true;
            default -> false;
        };
    }

    public static boolean isChuChuIsLand(int mobID) {
        return switch (mobID) {
            case 8642000, 8642001, 8642002, 8642003, 8642004, 8642005, 8642006, 8642007, 8642008, 8642009, 8642010, 8642011, 8642012, 8642013, 8642014, 8642015, 8642016, 8642017, 8642018, 8642019, 8642021, 8642022 ->
                    true;
            default -> false;
        };
    }

    public static boolean isLachelein(int mobID) {
        return switch (mobID) {
            case 8643000, 8643001, 8643002, 8643003, 8643004, 8643005, 8643006, 8643007, 8643008, 8643009, 8643010, 8643011, 8643012, 8643014, 8643015, 8643016 ->
                    true;
            default -> false;
        };
    }

    public static boolean isArcana(int mobID) {
        return switch (mobID) {
            case 8644000, 8644001, 8644002, 8644003, 8644004, 8644005, 8644006, 8644007, 8644008, 8644009, 8644010, 8644011, 8644012, 8644014, 8644015, 8644016, 8644017, 8644018, 8644019 ->
                    true;
            default -> false;
        };
    }

    public static boolean isMorass(int mobID) {
        return switch (mobID) {
            case 8644400, 8644401, 8644402, 8644403, 8644404, 8644405, 8644406, 8644407, 8644408, 8644409, 8644410, 8644411, 8644412, 8644415, 8644421, 8644422, 8644423, 8644424, 8644428, 8644429, 8644430, 8644431, 8644432, 8644433, 8644434, 8644437, 8644438, 8644439, 8644440, 8644441, 8644442, 8644443, 8644444, 8644445, 8644446, 8644447, 8645103 ->
                    true;
            default -> false;
        };
    }

    public static boolean isEsfera(int mobID) {
        return switch (mobID) {
            case 8644500, 8644501, 8644502, 8644503, 8644504, 8644505, 8644506, 8644507, 8644508, 8644509, 8644510, 8644520, 8644521, 8644522, 8644523, 8644524 ->
                    true;
            default -> false;
        };
    }

    public static int getStarForceMultiplier(int perc, int diff) {
        if (perc <= 9) {
            return -100;
        } else if (perc <= 29) {
            return -90;
        } else if (perc <= 49) {
            return -70;
        } else if (perc <= 69) {
            return -50;
        } else if (perc <= 99) {
            return -30;
        } else if (perc == 100) {
            return 0;
        }
        return Math.min(20, diff);
    }

    public static int getArcaneForceMultiplier(int perc) {
        if (perc <= 9) {
            return -90;
        } else if (perc <= 29) {
            return -70;
        } else if (perc <= 49) {
            return -40;
        } else if (perc <= 69) {
            return -30;
        } else if (perc <= 99) {
            return -20;
        } else if (perc <= 109) {
            return 0;
        } else if (perc <= 129) {
            return 10;
        } else if (perc <= 149) {
            return 30;
        }
        return 50;
    }

    public static double getDamageBonusFromLevelDifference(int charLevel, int mobLevel) {
        double mult = 0;
        int diff = charLevel - mobLevel;
        if (diff >= 0) {
            diff = Math.min(diff, 5); // max 5 * 2% extra
            mult = 10 + (diff * 2);
        } else if (diff < 0 && diff >= -5) {
            // can do calc based on diff, but needs some rounding, so just to a switch
            switch (diff) {
                case -1:
                    mult = 1.08 * 0.98;
                    break;
                case -2:
                    mult = 1.06 * 0.95;
                    break;
                case -3:
                    mult = 1.04 * 0.93;
                    break;
                case -4:
                    mult = 1.02 * 0.9;
                    break;
                case -5:
                    mult = 0.88;
                    break;
            }
            mult -= 1;
            mult *= 100;
        } else {
            // diff < 5, max out at 40 diff (=100% damage reduction)
            diff = Math.max(-40, diff);
            mult = Math.round(-15 - (-2.5 * (diff + 6)));
        }
        return mult / 100;
    }

    public static long getDreamBreakerHP(int stage) {
        if (stage < 10) {
            return 220000000L;
        } else if (stage < 20) {
            return 500000000L;
        } else if (stage < 30) {
            return 1200000000L;
        } else if (stage < 40) {
            return 2300000000L;
        } else if (stage < 50) {
            return 5400000000L;
        } else if (stage < 60) {
            return 9750000000L;
        } else if (stage < 70) {
            return 15250000000L;
        } else if (stage < 80) {
            return 24700000000L;
        } else if (stage < 90) {
            return 36000000000L;
        } else if (stage == 90) {
            return 50000000000L;
        } else if (stage < 100) {
            return 87000000000L;
        } else if (stage == 100) {
            return 135000000000L;
        } else if (stage < 110) {
            return 335000000000L;
        } else if (stage < 120) {
            return 373000000000L;
        } else if (stage < 130) {
            return 403000000000L;
        } else if (stage < 140) {
            return 435000000000L;
        } else if (stage < 150) {
            return 469000000000L;
        } else if (stage < 160) {
            return 503000000000L;
        } else if (stage < 170) {
            return 533000000000L;
        } else if (stage < 180) {
            return 569000000000L;
        } else if (stage < 190) {
            return 603000000000L;
        } else if (stage < 200) {
            return 635000000000L;
        } else if (stage == 200) {
            return 669000000000L;
        } else if (stage == 201) {
            return 4700000000000L;
        } else {
            return Math.max(1L, 4700000000000L * (stage - 201));
        }
    }

    public static BossPartyType getPartyBossByTemplateID(int templateID) {
        return switch (templateID) {
            case 8500022 -> BossPartyType.PAPULATUS_CHAOS;    //	Boss Name:	Papulatus (Chaos)	|	HP:	504000000000

            case 8644650 -> BossPartyType.GLOOM_NORMAL;    //	Boss Name:	Giant Monster Gloom	|	HP:	1337

            case 8800102 -> BossPartyType.ZAKUM_CHAOS;    //	Boss Name:	Chaos Zakum	|	HP:	2100000000

            case 8810122 -> BossPartyType.HORNTAIL_CHAOS;    //	Boss Name:	Chaos Horntail	|	HP:	10000000000

            case 8840006 -> BossPartyType.VON_LEON_HARD;    //	Boss Name:	Von Leon (Hard)	|	HP:	2090000000

            case 8880000 -> BossPartyType.MAGNUS_HARD;    //	Boss Name:	Magnus (Hard)	|	HP:	120000000000

            case 8880111 -> BossPartyType.DAMIEN_NORMAL;    //	Boss Name:	Damien (Normal)	|	HP:	120000000000
            case 8880101 -> BossPartyType.DAMIEN_HARD;    //	Boss Name:	Damien (Hard)	|	HP:	120000000000

            case 8880177 -> BossPartyType.LUCID_EASY;    //	Boss Name:	Final Music Box (Normal)	|	HP:	360000000
            case 8880167 -> BossPartyType.LUCID_NORMAL;    //	Boss Name:	Final Music Box (Hard)	|	HP:	360000000
            case 8880156 -> BossPartyType.LUCID_HARD;    //	Boss Name:	Final Music Box (Hard)	|	HP:	360000000

            case 8880302 -> BossPartyType.WILL_NORMAL;    //	Boss Name:	Will (Normal)	|	HP:	1337
            case 8880342 -> BossPartyType.WILL_HARD;    //	Boss Name:	Will (Hard)	|	HP:	1337

            case 8880410 -> BossPartyType.VERUS_HILLA_HARD;    //	Boss Name:	Verus Hilla (Hard)	|	HP:	1337

            case 8880504 -> BossPartyType.BLACK_MAGE_HARD;    //	Boss Name:	Black Mage	|	HP:	1337

            case 8900003 -> BossPartyType.PIERRE_CHAOS;    //	Boss Name:	Chaos Pierre	|	HP:	1000

            case 8910000 -> BossPartyType.VON_BON_CHAOS;    //	Boss Name:	Chaos Von Bon	|	HP:	100000000000

            case 8920006 -> BossPartyType.QUEEN_CHAOS;    //	Boss Name:	Chaos Crimson Queen	|	HP:	1000

            case 8930000 -> BossPartyType.VELLUM_CHAOS;    //	Boss Name:	Chaos Vellum	|	HP:	200000000000

            case 8950002 -> BossPartyType.LOTUS_NORMAL;    //	Boss Name:	Lotus (Normal)	|	HP:	1337
            case 8950102 -> BossPartyType.LOTUS_HARD;    //	Boss Name:	Lotus (Hard)	|	HP:	1337

            default -> null;
        };
    }

    public static int getBossGuildContribution(int BossTemplateID) {
        return switch (BossTemplateID) {
            case 8500002 -> 100;    //	Boss Name:	Papulatus (Easy)	|	HP:	100000000
            case 8500012 -> 500;    //	Boss Name:	Papulatus (Normal)	|	HP:	16600000000
            case 8500022 -> 1250;    //	Boss Name:	Papulatus (Chaos)	|	HP:	504000000000

            case 8800022 -> 100;    //	Boss Name:	Crumbling Zakum	|	HP:	2200000
            case 8800002 -> 100;    //	Boss Name:	Zakum (Normal)	|	HP:	7000000
            case 8800102 -> 1000;    //	Boss Name:	Chaos Zakum	|	HP:	2100000000

            case 8810214 -> 150;    //	Boss Name:	Easy Horntail	|	HP:	817600000
            case 8810018 -> 150;    //	Boss Name:	Horntail (Normal)	|	HP:	2090000000
            case 8810122 -> 250;    //	Boss Name:	Chaos Horntail	|	HP:	10000000000

            case 8820001 -> 250;    //	Boss Name:	Pink Bean	|	HP:	2100000000
            case 8820212 -> 1000;    //	Boss Name:	Chaos Pink Bean	|	HP:	2100000000

            case 8840000 -> 150;    //	Boss Name:	Von Leon (Easy)	|	HP:	550000000
            case 8840007 -> 250;    //	Boss Name:	Von Leon (Normal)	|	HP:	700000000
            case 8840014 -> 500;    //	Boss Name:	Von Leon (Hard)	|	HP:	2090000000

            case 8850111 -> 500;    //	Boss Name:	Cygnus (Easy)	|	HP:	10500000000
            case 8850011 -> 1000;    //	Boss Name:	Cygnus (Normal)	|	HP:	63000000000

            case 8860005 -> 200;    //	Boss Name:	Arkarium (Easy)	|	HP:	2100000000
            case 8860000 -> 500;    //	Boss Name:	Arkarium (Normal)	|	HP:	2100000000

            case 8870000 -> 150;    //	Boss Name:	Hilla	|	HP:	315000000
            case 8870100 -> 1000;    //	Boss Name:	Silver-Haired Hilla	|	HP:	16800000000

            case 8880010 -> 100;    //	Boss Name:	Magnus (Easy)	|	HP:	400000000
            case 8880002 -> 500;    //	Boss Name:	Magnus (Normal)	|	HP:	6000000000
            case 8880000 -> 1250;    //	Boss Name:	Magnus (Hard)	|	HP:	120000000000

            case 8880111 -> 1500;    //	Boss Name:	Damien (Normal)	|	HP:	120000000000
            case 8880101 -> 1500;    //	Boss Name:	Damien (Hard)	|	HP:	120000000000

            case 8880177 -> 1500;    //	Boss Name:	Final Music Box (Easy)	|	HP:	360000000
            case 8880167 -> 1500;    //	Boss Name:	Final Music Box (Normal)	|	HP:	360000000
            case 8880156 -> 1500;    //	Boss Name:	Final Music Box (Hard)	|	HP:	360000000

            case 8880342 -> 1500;    //	Boss Name:	Will (Normal)	|	HP:	1337
            case 8880302 -> 1500;    //	Boss Name:	Will (Hard)	|	HP:	1337

            case 8880405 -> 1500;    //	Boss Name:	Verus Hilla (Normal)	|	HP:	1337
            case 8880410 -> 1500;    //	Boss Name:	Verus Hilla (Hard)	|	HP:	1337

            case 8880504 -> 2000;    //	Boss Name:	Black Mage	|	HP:	1337

            case 8900103 -> 150;    //	Boss Name:	Pierre's Treasure Chest	|	HP:	1000
            case 8900003 -> 1000;    //	Boss Name:	Chaos Pierre	|	HP:	1000

            case 8910100 -> 150;    //	Boss Name:	Von Bon	|	HP:	315000000
            case 8910000 -> 1000;    //	Boss Name:	Chaos Von Bon	|	HP:	100000000000

            case 8920106 -> 150;    //	Boss Name:	Crimson Queen's Treasure Chest	|	HP:	1000
            case 8920006 -> 1000;    //	Boss Name:	Chaos Crimson Queen	|	HP:	1000

            case 8930100 -> 150;    //	Boss Name:	Vellum	|	HP:	550000000
            case 8930000 -> 1000;    //	Boss Name:	Chaos Vellum	|	HP:	200000000000

            case 8950002 -> 1500;    //	Boss Name:	Lotus (Normal)	|	HP:	1337
            case 8950102 -> 1500;    //	Boss Name:	Lotus (Hard)	|	HP:	1337

            case 9421581 -> 100;    //	Boss Name:	Mori Ranmaru (Normal)	|	HP:	1000000000
            case 9421589 -> 500;    //	Boss Name:	Mori Ranmaru (Hard)	|	HP:	2100000000
            default -> 1;
        };
    }

    public static boolean isIntensePowerCrystal(int itemId) {
        return itemId == 4001886 // Intense Power Crystal (Daily)
                || itemId == 4001928 // Intense Power Crystal (Weekly)
                || itemId == 4001929 // Intense Power Crystal (Monthly)
                ;
    }

    public static void initCrystals() {
        intensePowerCrystal = new Int2ObjectOpenHashMap<>();
        intensePowerCrystal.put(9210000, new IntensePowerCrystalData(8800022, 9210000, 200000L)); // Crumbling Zakum (8800022 | 9210000 | 200,000 mesos)
        intensePowerCrystal.put(9210001, new IntensePowerCrystalData(8800002, 9210001, 612500L)); // Zakum (8800002 | 9210001 | 612,500 mesos)
        intensePowerCrystal.put(9210002, new IntensePowerCrystalData(8800102, 9210002, 16200000L)); // Chaos Zakum (8800102 | 9210002 | 16,200,000 mesos)
        intensePowerCrystal.put(9210003, new IntensePowerCrystalData(8880010, 9210003, 722000L)); // Magnus (8880010 | 9210003 | 722,000 mesos)
        intensePowerCrystal.put(9210004, new IntensePowerCrystalData(8880002, 9210004, 2592000L)); // Magnus (8880002 | 9210004 | 2,592,000 mesos)
        intensePowerCrystal.put(9210005, new IntensePowerCrystalData(8880000, 9210005, 19012500L)); // Magnus (8880000 | 9210005 | 19,012,500 mesos)
        intensePowerCrystal.put(9210006, new IntensePowerCrystalData(8870000, 9210006, 800000L)); // Hilla (8870000 | 9210006 | 800,000 mesos)
        intensePowerCrystal.put(9210007, new IntensePowerCrystalData(8870100, 9210007, 11250000L)); // Silver-Haired Hilla (8870100 | 9210007 | 11,250,000 mesos)
        intensePowerCrystal.put(9210008, new IntensePowerCrystalData(8880200, 9210008, 1250000L)); // OMNI-CLN (8880200 | 9210008 | 1,250,000 mesos)
        intensePowerCrystal.put(9210009, new IntensePowerCrystalData(8900100, 9210009, 968000L)); // Pierre (8900100 | 9210009 | 968,000 mesos)
        intensePowerCrystal.put(9210010, new IntensePowerCrystalData(8900000, 9210010, 16200000L)); // Chaos Pierre (8900000 | 9210010 | 16,200,000 mesos)
        intensePowerCrystal.put(9210011, new IntensePowerCrystalData(8910100, 9210011, 968000L)); // Von Bon (8910100 | 9210011 | 968,000 mesos)
        intensePowerCrystal.put(9210012, new IntensePowerCrystalData(8910000, 9210012, 16200000L)); // Chaos Von Bon (8910000 | 9210012 | 16,200,000 mesos)
        intensePowerCrystal.put(9210013, new IntensePowerCrystalData(8920100, 9210013, 968000L)); // Crimson Queen (8920100 | 9210013 | 968,000 mesos)
        intensePowerCrystal.put(9210014, new IntensePowerCrystalData(8920000, 9210014, 16200000L)); // Chaos Crimson Queen (8920000 | 9210014 | 16,200,000 mesos)
        intensePowerCrystal.put(9210015, new IntensePowerCrystalData(8930100, 9210015, 968000L)); // Vellum (8930100 | 9210015 | 968,000 mesos)
        intensePowerCrystal.put(9210016, new IntensePowerCrystalData(8930000, 9210016, 21012500L)); // Chaos Vellum (8930000 | 9210016 | 21,012,500 mesos)
        intensePowerCrystal.put(9210017, new IntensePowerCrystalData(8840007, 9210017, 1058000L)); // Von Leon (8840007 | 9210017 | 1,058,000 mesos)
        intensePowerCrystal.put(9210018, new IntensePowerCrystalData(8840000, 9210018, 1458000L)); // Von Leon (8840000 | 9210018 | 1,458,000 mesos)
        intensePowerCrystal.put(9210019, new IntensePowerCrystalData(8840014, 9210019, 2450000L)); // Von Leon (8840014 | 9210019 | 2,450,000 mesos)
        intensePowerCrystal.put(9210020, new IntensePowerCrystalData(8810214, 9210020, 882000L)); // Easy Horntail (8810214 | 9210020 | 882,000 mesos)
        intensePowerCrystal.put(9210021, new IntensePowerCrystalData(8810018, 9210021, 1012500L)); // Horntail (8810018 | 9210021 | 1,012,500 mesos)
        intensePowerCrystal.put(9210022, new IntensePowerCrystalData(8810122, 9210022, 1352000L)); // Chaos Horntail (8810122 | 9210022 | 1,352,000 mesos)
        intensePowerCrystal.put(9210023, new IntensePowerCrystalData(8860005, 9210023, 1152000L)); // Arkarium (8860005 | 9210023 | 1,152,000 mesos)
        intensePowerCrystal.put(9210024, new IntensePowerCrystalData(8860000, 9210024, 2520500L)); // Arkarium (8860000 | 9210024 | 2,520,500 mesos)
        intensePowerCrystal.put(9210025, new IntensePowerCrystalData(8820001, 9210025, 1404500L)); // Pink Bean (8820001 | 9210025 | 1,404,500 mesos)
        intensePowerCrystal.put(9210026, new IntensePowerCrystalData(8820212, 9210026, 12800000L)); // Chaos Pink Bean (8820212 | 9210026 | 12,800,000 mesos)
        intensePowerCrystal.put(9210027, new IntensePowerCrystalData(8850111, 9210027, 9112500L)); // Cygnus (8850111 | 9210027 | 9,112,500 mesos)
        intensePowerCrystal.put(9210028, new IntensePowerCrystalData(8850011, 9210028, 14450000L)); // Cygnus (8850011 | 9210028 | 14,450,000 mesos)
        intensePowerCrystal.put(9210029, new IntensePowerCrystalData(8950101, 9210029, 32512500L)); // Lotus (8950101 | 9210029 | 32,512,500 mesos)
        intensePowerCrystal.put(9210030, new IntensePowerCrystalData(8950001, 9210030, 74112500L)); // Lotus (8950001 | 9210030 | 74,112,500 mesos)
        intensePowerCrystal.put(9210031, new IntensePowerCrystalData(8880110, 9210031, 33800000L)); // Normal Damien (8880110 | 9210031 | 33,800,000 mesos)
        intensePowerCrystal.put(9210032, new IntensePowerCrystalData(8880100, 9210032, 70312500L)); // Damien (8880100 | 9210032 | 70,312,500 mesos)
        intensePowerCrystal.put(9210033, new IntensePowerCrystalData(8880140, 9210033, 40612500L)); // Dreaming Lucid (8880140 | 9210033 | 40,612,500 mesos)
        intensePowerCrystal.put(9210034, new IntensePowerCrystalData(8880141, 9210034, 80000000L)); // Dreaming Lucid (8880141 | 9210034 | 80,000,000 mesos)
        intensePowerCrystal.put(9210035, new IntensePowerCrystalData(8500002, 9210035, 684500L)); // Papulatus (8500002 | 9210035 | 684,500 mesos)
        intensePowerCrystal.put(9210036, new IntensePowerCrystalData(8500012, 9210036, 2664500L)); // Papulatus (8500012 | 9210036 | 2,664,500 mesos)
        intensePowerCrystal.put(9210037, new IntensePowerCrystalData(8500022, 9210037, 26450000L)); // Papulatus (8500022 | 9210037 | 26,450,000 mesos)
        intensePowerCrystal.put(9210038, new IntensePowerCrystalData(8880340, 9210038, 46512500L)); // Will (8880340 | 9210038 | 46,512,500 mesos)
        intensePowerCrystal.put(9210039, new IntensePowerCrystalData(8880300, 9210039, 88200000L)); // Will (8880300 | 9210039 | 88,200,000 mesos)
        intensePowerCrystal.put(9210040, new IntensePowerCrystalData(8880410, 9210040, 110450000L)); // Verus Hilla (8880410 | 9210040 | 110,450,000 mesos)
        intensePowerCrystal.put(9210041, new IntensePowerCrystalData(8880502, 9210041, 500000000L)); // Black Mage (8880502 | 9210041 | 500,000,000 mesos)
        intensePowerCrystal.put(9210042, new IntensePowerCrystalData(8644650, 9210042, 49612500L)); // Giant Monster Gloom (8644650 | 9210042 | 49,612,500 mesos)
        intensePowerCrystal.put(9210043, new IntensePowerCrystalData(8645009, 9210043, 52812500L)); // Guard Captain Darknell (8645009 | 9210043 | 52,812,500 mesos)
        intensePowerCrystal.put(9210044, new IntensePowerCrystalData(9400300, 9210044, 722000L)); // Yakuza Boss (9400300 | 9210044 | 722,000 mesos)
        intensePowerCrystal.put(9210045, new IntensePowerCrystalData(9601327, 9210045, 882000L)); // Gigatoad (9601327 | 9210045 | 882,000 mesos)
        intensePowerCrystal.put(9210046, new IntensePowerCrystalData(8644655, 9210046, 92450000L)); // Giant Monster Gloom (8644655 | 9210046 | 92,450,000 mesos)
        intensePowerCrystal.put(9210047, new IntensePowerCrystalData(8645066, 9210047, 96800000L)); // Guard Captain Darknell (8645066 | 9210047 | 96,800,000 mesos)
        intensePowerCrystal.put(9210048, new IntensePowerCrystalData(8880142, 9210048, 35112500L)); // Dreaming Lucid (8880142 | 9210048 | 35,112,500 mesos)
        intensePowerCrystal.put(9210049, new IntensePowerCrystalData(9601328, 9210049, 1153000L)); // Frenzied Gigatoad (9601328 | 9210049 | 1,153,000 mesos)
        intensePowerCrystal.put(9210050, new IntensePowerCrystalData(8880600, 9210050, 151250000L)); // Chosen Seren (8880600 | 9210050 | 151,250,000 mesos)
        intensePowerCrystal.put(9210051, new IntensePowerCrystalData(8880711, 9210051, 90312500L)); // Guardian Angel Slime (8880711 | 9210051 | 90,312,500 mesos)
        intensePowerCrystal.put(9210052, new IntensePowerCrystalData(8880700, 9210052, 34322000L)); // Guardian Angel Slime (8880700 | 9210052 | 34,322,000 mesos)
        intensePowerCrystal.put(9210053, new IntensePowerCrystalData(8880360, 9210053, 38255000L)); // Will (8880360 | 9210053 | 38,255,000 mesos)
        intensePowerCrystal.put(9210054, new IntensePowerCrystalData(8880630, 9210054, 133687500L)); // Chosen Seren (8880630 | 9210054 | 133,687,500 mesos)
        intensePowerCrystal.put(9210055, new IntensePowerCrystalData(8880450, 9210055, 89520000L)); // Verus Hilla (8880450 | 9210055 | 89,520,000 mesos)
        intensePowerCrystal.put(9210056, new IntensePowerCrystalData(8880802, 9210056, 200000000L)); // Kalos the Guardian (8880802 | 9210056 | 200,000,000 mesos)
        intensePowerCrystal.put(9210057, new IntensePowerCrystalData(8880532, 9210057, 3600000000L)); // Black Mage (8880532 | 9210057 | 3,600,000,000 mesos)
        intensePowerCrystal.put(9210058, new IntensePowerCrystalData(8880650, 9210058, 847000000L)); // Chosen Seren (8880650 | 9210058 | 847,000,000 mesos)
        intensePowerCrystal.put(9210059, new IntensePowerCrystalData(8880845, 9210059, 301300000L)); // Peace Blooms Again (8880845 | 9210059 | 301,300,000 mesos)
        intensePowerCrystal.put(9210060, new IntensePowerCrystalData(8880802, 9210060, 187500000L)); // Kalos the Guardian (8880802 | 9210060 | 187,500,000 mesos)
        intensePowerCrystal.put(9210061, new IntensePowerCrystalData(8880802, 9210061, 520000000L)); // Kalos the Guardian (8880802 | 9210061 | 520,000,000 mesos)
        intensePowerCrystal.put(9210062, new IntensePowerCrystalData(8880802, 9210062, 1040000000L)); // Kalos the Guardian (8880802 | 9210062 | 1,040,000,000 mesos)
        intensePowerCrystal.put(9210063, new IntensePowerCrystalData(8880845, 9210063, 206250000L)); // Peace Blooms Again (8880845 | 9210063 | 206,250,000 mesos)
        intensePowerCrystal.put(9210064, new IntensePowerCrystalData(8880845, 9210064, 598000000L)); // Peace Blooms Again (8880845 | 9210064 | 598,000,000 mesos)
        intensePowerCrystal.put(9210065, new IntensePowerCrystalData(8880845, 9210065, 1205200000L)); // Peace Blooms Again (8880845 | 9210065 | 1,205,200,000 mesos)
        intensePowerCrystal.put(9210066, new IntensePowerCrystalData(8881200, 9210066, 279500000L)); // Lotus (8881200 | 9210066 | 279,500,000 mesos)
        intensePowerCrystal.put(9210067, new IntensePowerCrystalData(8881320, 9210067, 420000000L)); // Limbo Enlightened by Dark Truth (8881320 | 9210067 | 420,000,000 mesos)
        intensePowerCrystal.put(9210068, new IntensePowerCrystalData(8881320, 9210068, 749000000L)); // Limbo Enlightened by Dark Truth (8881320 | 9210068 | 749,000,000 mesos)
        intensePowerCrystal.put(9210069, new IntensePowerCrystalData(8881601, 9210069, 560000000L)); // Baldrix (8881601 | 9210069 | 560,000,000 mesos)
        intensePowerCrystal.put(9210070, new IntensePowerCrystalData(8881601, 9210070, 840000000L)); // Baldrix (8881601 | 9210070 | 840,000,000 mesos)
        intensePowerCrystal.put(9210071, new IntensePowerCrystalData(8881710, 9210071, 197000000L)); // First Adversary (8881710 | 9210071 | 197,000,000 mesos)
        intensePowerCrystal.put(9210072, new IntensePowerCrystalData(8881710, 9210072, 273000000L)); // First Adversary (8881710 | 9210072 | 273,000,000 mesos)
        intensePowerCrystal.put(9210073, new IntensePowerCrystalData(8881710, 9210073, 588000000L)); // First Adversary (8881710 | 9210073 | 588,000,000 mesos)
        intensePowerCrystal.put(9210074, new IntensePowerCrystalData(8881710, 9210074, 1176000000L)); // First Adversary (8881710 | 9210074 | 1,176,000,000 mesos)
        intensePowerCrystal.put(9601188, new IntensePowerCrystalData(9421589, 9601188, 840500L)); // Mori Ranmaru (9421589 | 9601188 | 840,500 mesos)
        intensePowerCrystal.put(9601189, new IntensePowerCrystalData(9421589, 9601189, 2664500L)); // Mori Ranmaru (9421589 | 9601189 | 2,664,500 mesos)
        intensePowerCrystal.put(9601190, new IntensePowerCrystalData(9450022, 9601190, 16200000L)); // Princess No (9450022 | 9601190 | 16,200,000 mesos)
        intensePowerCrystal.put(9601635, new IntensePowerCrystalData(9601623, 9601635, 28800000L)); // Akechi Mitsuhide (9601623 | 9601635 | 28,800,000 mesos)
    }

    // Nguồn: https://maplestory.fandom.com/wiki/Intense_Power_Crystal#KMS
    public static long getBossRewardPrice(int BossTemplateID) {
        for (var entry : intensePowerCrystal.int2ObjectEntrySet()) {
            if (entry.getValue().getNamingMonster() == BossTemplateID) {
                return entry.getValue().getMeso();
            }
        }
        return 1;
    }

    public static int getBossRewardID(int BossTemplateID) {
        for (var entry : intensePowerCrystal.int2ObjectEntrySet()) {
            if (entry.getValue().getRealMonster() == BossTemplateID) {
                return entry.getValue().getNamingMonster();
            }
        }
        return 0;
    }
}
