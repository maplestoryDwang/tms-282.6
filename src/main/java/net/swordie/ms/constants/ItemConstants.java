package net.swordie.ms.constants;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.*;
import net.swordie.ms.client.character.skills.SkillStat;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.client.social.Guild.GuildSkill;
import net.swordie.ms.enums.*;
import net.swordie.ms.life.drop.DropInfo;
import net.swordie.ms.life.pet.PetSkill;
import net.swordie.ms.loaders.EffectData;
import net.swordie.ms.loaders.ItemData;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.loaders.StringData;
import net.swordie.ms.loaders.containerclasses.ItemInfo;
import net.swordie.ms.util.Util;

import java.util.*;
import java.util.stream.Collectors;

import static net.swordie.ms.constants.SkillConstants.*;
import static net.swordie.ms.enums.InvType.EQUIP;
import static net.swordie.ms.enums.ItemGrade.*;

public class ItemConstants {

    public static final int CURRENCY = 4000313;

    public static final int EQUIPMENT_MAXLEVEL = 30;
    public static final int INC_RAND_LEVELUP_MAX = 2;
    public static final int EMPTY_SOCKET_ID = 3;
    public static final short INACTIVE_SOCKET = 0;
    public static final short MIN_LEVEL_FOR_SOUL_SOCKET = 75;
    public static final int SOUL_ENCHANTER_BASE_ID = 2590000;
    public static final int SOUL_ITEM_BASE_ID = 2591000;
    public static final int MAX_SOUL_CAPACITY = 1000;
    public static final int MOB_CARD_BASE_ID = 2380000;
    public static final int FAMILIAR_PREFIX = 996;
    public static final int SPELL_TRACE_ID = 4001832;
    public static final int RAND_CHAOS_MAX = 5;
    public static final int INCREDIBLE_RAND_CHAOS_MAX = 10;
    public static final int CASH_ITEM_AVAILABLE_DAYS = 30;

    public static final byte MAX_SKIN = 13; // 6, 7, 8 crash

    public static final int MIN_HAIR = 30000;
    public static final int MAX_HAIR = 49999;
    public static final int MIN_HAIR_2 = 60000;
    public static final int MAX_HAIR_2 = 69999;

    public static final int MIN_FACE = 20000;
    public static final int MAX_FACE = 29999;
    public static final int MIN_FACE_2 = 50000;
    public static final int MAX_FACE_2 = 59999;

    public static final int SPECIAL_BEAUTY_COUPON = 2430182;
    public static final int REWARD_COIN = 4310150;
    public static final int HYPER_TELEPORT_ROCK = 5040004;
    public static final int PET_VAC = 99999999;
    public static final int SURPRISE_STYLE_BOX = 5222006;
    //Chances are out of 1k (10* typical 100 values so we don't have to use doubles)
    public static final int THIRD_LINE_CHANCE = 10;
    public static final int DEFAULT_PRIME_LINE_2_CHANCE = 10; // 12.5 -> 1% ra line thứ 2 PRIME
    public static final int DEFAULT_PRIME_LINE_3_CHANCE = 0; // 10% -> 0% ra line thứ 3 PRIME
    public static final int BOOSTED_PRIME_LINE_2_CHANCE = 50; // 22% - 5% ra line thứ 2 PRIME - Black Cube/ White Bonus Cube
    public static final int BOOSTED_PRIME_LINE_3_CHANCE = 10; // 10% - 1% ra line thứ 3 PRIME - Black Cube/ White Bonus Cube
    public static final int MIRACLE_CUBE = 5062000;
    public static final int PREMIUM_MIRACLE_CUBE = 5062001;
    public static final int SUPER_MIRACLE_CUBE = 5062002;
    public static final int REVOLUTIONARY_MIRACLE_CUBE = 5062003;
    public static final int ENLIGHTENING_MIRACLE_CUBE = 5062005;
    public static final int PLATINUM_MIRACLE_CUBE = 5062006;
    public static final int RED_CUBE = 5062009;
    public static final int BLACK_CUBE = 5062010;
    public static final int MEMORY_CUBE = 5062090;
    public static final int VIOLET_CUBE_PRIME_LINE_CHANCE = 100;
    public static final int VIOLET_CUBE = 5062024;
    public static final int VIOLET_CUBE_FRAGMENT = 2434125;
    public static final int BONUS_POTENTIAL_CUBE = 5062500;
    //If you plan to implement, you must store the grade to the item and change Equip#getBaseGrade and similar methods behave appropriately
    //This is because you can select a non-current tier line as the primary line, thus making the current methods believe the item is actually a tier lower!
    //By storing the tier in the Equip object you can allow for the list of potentials to be completely independent
    public static final int SPECIAL_BONUS_POTENTIAL_CUBE = 5062501;

    public static final int SYSTEM_DEFAULT_CUBE_INDICATOR = -1; //Cause default int value is 0 and we want to differentiate when it is intentional to not have a cube source

    public static final int MYSTICAL_CUBE = 2710028; // tradeBlock
    public static final List<Integer> MYSTICAL_CUBES = Arrays.asList(
            2711011, 2711027,                               // USER_CONSUME_CUBE_ITEM_USE_REQUEST (TODO)
            2710028, 2710033, 2710036, 2710037              // USER_CASH_CUBE_ITEM_USE_REQUEST
    );

    public static final int BONUS_MYSTICAL_CUBE = 2730007; // tradeBlock
    public static final List<Integer> BONUS_MYSTICAL_CUBES = Arrays.asList(
            2730000, 2730002, 2730004, 2730006, 2730013,    // USER_BONUS_MYSTICAL_ITEM_USE_REQUEST (TODO)
            2730007, 2730008, 2730009, 2730010, 2730017     // USER_CASH_CUBE_ITEM_USE_REQUEST
    );

    public static final int HARD_CUBE = 2710034; // tradeBlock
    public static final List<Integer> HARD_CUBES = Arrays.asList(
            2711012, 2711023, 2711026,                      // USER_CONSUME_CUBE_ITEM_USE_REQUEST (TODO)
            2710029, 2710032, 2710034, 2710038, 2710044     // USER_CASH_CUBE_ITEM_USE_REQUEST
    );

    public static final int SOLID_CUBE = 2710035; // tradeBlock
    public static final List<Integer> SOLID_CUBES = Arrays.asList(
            2711021, 2711028,                               // USER_CONSUME_CUBE_ITEM_USE_REQUEST (TODO)
            2710030, 2710035, 2710039, 2710040, 2710042     // USER_CASH_CUBE_ITEM_USE_REQUEST
    );

    public static final int GLOWING_CUBE = 5062028;
    public static final int BONUS_GLOWING_CUBE = 5062505;
    public static final int KARMA_BONUS_GLOWING_CUBE = 2710061;
    public static final int BRIGHT_CUBE = 5062029;
    public static final int BONUS_BRIGHT_CUBE_1 = 5062503;
    public static final int BONUS_BRIGHT_CUBE_2 = 5062506;

    public static final List<Integer> BRIGHT_CUBES = Arrays.asList(2711024, 2711025, 2710054, 2710055); // USER_BONUS_BRIGHT_CUBE_ITEM_USE_REQUEST (TODO)
    public static final List<Integer> BONUS_BRIGHT_CUBES = Arrays.asList(2730011, 2730012, 2730018, 2730019); // USER_BONUS_BRIGHT_CUBE_ITEM_USE_REQUEST (TODO)

    //Chances are out of 1k (10* typical 100 values so we don't have to use doubles)
    public static final int[] MYSTICAL_CUBES_TIER_UP_RATES = new int[]{10, 0, 0}; //1%, 0%, 0%
    public static final int[] HARD_CUBES_TIER_UP_RATES = new int[]{120, 40, 0}; //12%, 4%, 0%
    public static final int[] GLOWING_CUBES_TIER_UP_RATES = new int[]{100, 50, 25}; //10%, 5%, 2.5%
    public static final int[] BRIGHT_CUBES_TIER_UP_RATES = new int[]{150, 70, 40}; //1.5%, 0.5%, 0%
    public static final int[] SOLID_CUBE_TIER_UP_RATES = new int[]{120, 80, 25}; //12%, 8%, 5%
    public static final int[] MIRACLE_CUBE_TIER_UP_RATES = new int[]{60, 20, 0}; //15%, 6%, 2.5%
    public static final int[] PREMIUM_MIRACLE_CUBE_TIER_UP_RATES = new int[]{120, 40, 0}; //15%, 6%, 2.5%
    public static final int[] SUPER_MIRACLE_CUBE_TIER_UP_RATES = new int[]{150, 80, 25}; //15%, 6%, 2.5%
    public static final int[] REVOLUTIONARY_MIRACLE_CUBE_TIER_UP_RATES = new int[]{150, 100, 45}; //15%, 6%, 2.5%
    public static final int[] ENLIGHTENING_MIRACLE_CUBE_TIER_UP_RATES = new int[]{150, 120, 65}; //15%, 6%, 2.5%
    public static final int[] PLATINUM_MIRACLE_CUBE_TIER_UP_RATES = new int[]{150, 150, 85}; //15%, 6%, 2.5%
    public static final int[] RED_CUBE_TIER_UP_RATES = new int[]{150, 60, 25}; //15%, 6%, 2.5%
    public static final int[] BLACK_CUBE_TIER_UP_RATES = new int[]{150, 120, 50}; //15%, 12%, 5%
    public static final int[] VIOLET_CUBE_TIER_UP_RATES = new int[]{30, 12, 5}; //3%, 1.2%, 0.5%
    public static final int[] MEMORY_CUBE_TIER_UP_RATES = new int[]{15, 6, 2}; //1.5%, 0.6%, 0.2%
    public static final int[] BONUS_OCCULT_CUBE_TIER_UP_RATES = new int[]{20, 0, 0}; //2%, 0%, 0%
    public static final int[] BONUS_MYSTICAL_CUBE_TIER_UP_RATES = new int[]{20, 0, 0}; //2%, 0%, 0%
    public static final int[] BONUS_GLOWING_CUBE_TIER_UP_RATES = new int[]{100, 75, 25}; //2%, 0%, 0%
    public static final int[] BONUS_POT_CUBE_TIER_UP_RATE = new int[]{200, 100, 50}; //20%, 10%, 5%
    public static final int[] BONUS_BRIGHT_CUBE_TIER_UP_RATES = new int[]{200, 150, 50}; //20%, 15%, 7.5%
    public static final int[] DEFAULT_CUBE_TIER_UP_RATES = new int[]{100, 50, 25}; //10%, 5%, 2.5%

    public static final int NEBILITE_BASE_ID = 3060000;
    public static final int[] HORNTAIL_NECKLACE = {
            1122000, // Horntail Necklace
            1122076, // Chaos Horntail Necklace
            1122151, // Chaos Horntail Necklace (+2)
            1122249, // Dream Horntail Necklace
            1122278, // Mystic Horntail Necklace
    };
    public static final int[] EXP_2X_COUPON = {
            5680484,
            5680342,
            5680275,
            5211122,
            5211121,
            5211120,
            5211049,
            5211048,
            5211047,
            5211046,
            5211000
    };
    public static final short MAX_HAMMER_SLOTS = 2;
    public static final int[] NON_KMS_BOSS_SETS = {
            127, // Amaterasu
            128, // Oyamatsumi
            129, // Ame-no-Uzume
            130, // Tsukuyomi
            131, // Susano-o
            315, // Cracked Gollux
            316, // Solid Gollux
            317, // Reinforced Gollux
            318, // Superior Gollux
            328, // Sweetwater
    };
    public static final int[] NON_KMS_BOSS_ITEMS = {
            1032224, // Sweetwater Earrings
            1022211, // Sweetwater Monocle
            1012438, // Sweetwater Tattoo
            1152160, // Sweetwater Shoulder
            1132247, // Sweetwater Belt
            1122269, // Sweetwater Pendant
    };
    // Flames
    public static final double[] WEAPON_FLAME_MULTIPLIER = {1.0, 2.2, 3.65, 5.35, 7.3, 8.8, 10.25};
    public static final double[] WEAPON_FLAME_MULTIPLIER_BOSS_WEAPON = {1.0, 1.0, 3.0, 4.4, 6.05, 8.0, 10.25}; // Boss weapons do not ever roll stat level 1/2.
    public static final short EQUIP_FLAME_LEVEL_DIVIDER = 40;
    public static final short EQUIP_FLAME_LEVEL_DIVIDER_EXTENDED = 20;
    public static final int[] EXCEPTIONAL_EX_ALLOWED = {
            1152155, // Scarlet Shoulder
            1113015, // Secret Ring
    };
    // Self-made drops per mob
    public static final List<Integer> equipDropLv10To40 = List.of(
            1212001,
            1212086,
            1222001,
            1232001,
            1242087,
            1252001,
            1252047,
            1262000,
            1262022,
            1312000,
            1312170,
            1322032,
            1322220,
            1322122,
            1332102,
            1332244,
            1362106,
            1382228,
            1402216,
            1412077,
            1442077,
            1442144,
            1442239,
            1452002,
            1452037,
            1462092,
            1462210,
            1472000,
            1472044,
            1472232,
            1482014,
            1482185,
            1492063,
            1522028,
            1522110,
            1532045,
            1532115,
            1212027,
            1252049,
            1302006,
            1322002,
            1312080,
            1322008,
            1332033,
            1432008,
            1442007,
            1482002,
            1522002,
            1532024,
            1542001,
            1552021,
            1212002,
            1222002,
            1242002,
            1262001,
            1312005,
            1322056,
            1322027,
            1312013,
            1322124,
            1332009,
            1342013,
            1382136,
            1402077,
            1432002,
            1442121,
            1452092,
            1462077,
            1482064,
            1522032,
            1002002,
            1002010,
            1002017,
            1002074,
            1002075,
            1002122,
            1002123,
            1002124,
            1002125,
            1002380,
            1002757,
            1002011,
            1002060,
            1002061,
            1002062,
            1002063,
            1002072,
            1002073,
            1002117,
            1002127,
            1002584,
            1002585,
            1002023,
            1002034,
            1002035,
            1002036,
            1002037,
            1002038,
            1002161,
            1002162,
            1002163,
            1002164,
            1002165,
            1002171,
            1002172,
            1002175,
            1002454,
            1040008,
            1040031,
            1040033,
            1041014,
            1041007,
            1041064,
            1040012,
            1040044,
            1040043,
            1041003,
            1041019,
            1040021,
            1040057,
            1040060,
            1041048,
            1041054,
            1052095,
            1050005,
            1052101,
            1050011,
            1050166,
            1051010,
            1052244,
            1060008,
            1060021,
            1060022,
            1060023,
            1061031,
            1062164,
            1061171,
            1060010,
            1060029,
            1060033,
            1061016,
            1060016,
            1060044,
            1061044,
            1072015,
            1072066,
            1072019,
            1072075,
            1072082
    );

    public static final List<Integer> equipDropLv50To70 = List.of(
            1222077,
            1212028,
            1232077,
            1302009,
            1302016,
            1322024,
            1332021,
            1362007,
            1382019,
            1522006,
            1532006,
            1552003,
            1242003,
            1242088,
            1312019,
            1322017,
            1332003,
            1402013,
            1432004,
            1472018,
            1542024,
            1522008,
            1472037,
            1452030,
            1002025,
            1002185,
            1002028,
            1002215,
            1002029,
            1002246,
            1002247,
            1002267,
            1040080,
            1040000,
            1040084,
            1040085,
            1040089,
            1040094,
            1041080,
            1041089,
            1040093,
            1040100,
            1041096,
            1052113,
            1050052,
            1050058,
            1051043,
            1060000,
            1060068,
            1061083,
            1061191,
            1060078,
            1060083,
            1061079,
            1061076,
            1061088,
            1072107,
            1072117,
            1072118,
            1072592,
            1072590,
            1072593,
            1072125,
            1072122,
            1072130,
            1072140,
            1082025,
            1082062,
            1082064,
            1082073,
            1082074,
            1082076
    );

    public static final List<Integer> equipDropLv80To100 = List.of(
            1212005,
            1222005,
            1232005,
            1242005,
            1302042,
            1322026,
            1322039,
            1322091,
            1332037,
            1332129,
            1342005,
            1402023,
            1412016,
            1422018,
            1432023,
            1442030,
            1442034,
            1452009,
            1472029,
            1472026,
            1482019,
            1522009,
            1212006,
            1242006,
            1262005,
            1322028,
            1362076,
            1382203,
            1442189,
            1452012,
            1462010,
            1462013,
            1532010,
            1232007,
            1242007,
            1462021,
            1462018,
            1462022,
            1522011,
            1532011,
            1002030,
            1002095,
            1002094,
            1002252,
            1002285,
            1002284,
            1002289,
            1002288,
            1002637,
            1003138,
            1002271,
            1002274,
            1002272,
            1002275,
            1002278,
            1002277,
            1002276,
            1002330,
            1002328,
            1002338,
            1002340,
            1002323,
            1002324,
            1002326,
            1002364,
            1002363,
            1002366,
            1002402,
            1002528,
            1002532,
            1002643,
            1040102,
            1040104,
            1040107,
            1040105,
            1041100,
            1041103,
            1040110,
            1040109,
            1040108,
            1041107,
            1041106,
            1040113,
            1040115,
            1041121,
            1050064,
            1052333,
            1052398,
            1050078,
            1050083,
            1051080,
            1051079,
            1052125,
            1050088,
            1050091,
            1051082,
            1051084,
            1051085,
            1052128,
            1060090,
            1060092,
            1060095,
            1061096,
            1061098,
            1061101,
            1061102,
            1060097,
            1060099,
            1061106,
            1061104,
            1072154,
            1072156,
            1072158,
            1072161,
            1072163,
            1072164,
            1072167,
            1072174,
            1072179,
            1072185,
            1072212,
            1072192,
            1072195,
            1072198,
            1072205,
            1072203,
            1072206,
            1072209,
            1082095,
            1082097,
            1082105,
            1082103,
            1082107,
            1082106,
            1082108,
            1082222,
            1082117,
            1082114,
            1082109,
            1082120,
            1082127,
            1082130,
            1082128,
            1082129,
            1082134,
            1082133
    );

    public static final List<Integer> equipDropLv100To150 = List.of(
            1212008,
            1252008,
            1302056,
            1312030,
            1312172,
            1322045,
            1332032,
            1332051,
            1332052,
            1342008,
            1362013,
            1382060,
            1402035,
            1402037,
            1412021,
            1432186,
            1442044,
            1442068,
            1452019,
            1452020,
            1452021,
            1462015,
            1462016,
            1462017,
            1472053,
            1472234,
            1522012,
            1532012,
            1542009,
            1552113,
            1212010,
            1222019,
            1232010,
            1242010,
            1242019,
            1312031,
            1322052,
            1332050,
            1332049,
            1342042,
            1442045,
            1442139,
            1462039,
            1452044,
            1212011,
            1222011,
            1232011,
            1242011,
            1252011,
            1302081,
            1312037,
            1322060,
            1332074,
            1332075,
            1342011,
            1382057,
            1412033,
            1472068,
            1002379,
            1002383,
            1002398,
            1002401,
            1002406,
            1002408,
            1002551,
            1002550,
            1002649,
            1002773,
            1003660,
            1002776,
            1002793,
            1002794,
            1003690,
            1004214,
            1040120,
            1040121,
            1050096,
            1050097,
            1050098,
            1050099,
            1050106,
            1050107,
            1050108,
            1051090,
            1051091,
            1051092,
            1051093,
            1051105,
            1051106,
            1051107,
            1052071,
            1052072,
            1052134,
            1052075,
            1052155,
            1052157,
            1052158,
            1052159,
            1060109,
            1060110,
            1060111,
            1061121,
            1061122,
            1061123,
            1072213,
            1072214,
            1072215,
            1072216,
            1072222,
            1072226,
            1072227,
            1072229,
            1072269,
            1072355,
            1072356,
            1072357,
            1072358,
            1072359,
            1082135,
            1082136,
            1082137,
            1082138,
            1082141,
            1082151,
            1082153,
            1082158,
            1082159,
            1082160,
            1082163,
            1082234,
            1082235,
            1082236,
            1082237,
            1082238,
            1082566
    );
    private static final Set<DropInfo> EMPTY_DROPS = Collections.emptySet();
    public static final Map<Integer, Set<DropInfo>> consumableDropsPerLevel = new HashMap<>();
    public static final Map<Integer, Set<DropInfo>> globalDropsPerLevel = new HashMap<>();
    public static final Map<Integer, Set<DropInfo>> eliteDrop = new HashMap<>();
    public static final Map<Integer, Set<DropInfo>> eliteChannelDrop = new HashMap<>();
    public static final Map<Integer, Set<DropInfo>> arcaneSymbolDrop = new HashMap<>();

    public static final Map<Integer, Integer> nebuliteRanks = new HashMap<>(); // REMOVED

    private static final Integer[] soulPotList = new Integer[]{
            32001, 32002, 32003, 32004, 32005, 32006, 32011, 32012, // flat
            32041, 32042, 32043, 32044, 32045, 32046, 32051, 32052}; // rate
    private static final int[] TUC_IGNORE_ITEMS = {
            1113231, // Master Ring SS
            1114301, // Reboot Vengeful Ring
            1114302, // Synergy Ring
            1114303, // Cosmos Ring
            1114304, // Reboot Cosmos Ring
            1114305, // Chaos Ring
    };
    // Spell tracing
    private static final int BASE_ST_COST = 10;
    private static final int INNOCENCE_ST_COST = 12_000;
    private static final int ARK_INNOCENCE_ST_COST = 24_000;
    private static final int CLEAN_SLATE_ST_COST = 3_000;
    //CUBING CONSTANTS BELOW
    public static int GLOBAL_TIER_UP_MULTIPLIER = 1; //For DMT uses? 2 = 2x, 3 = 3x, etc etc

    public static int[] CASH_DROP_2X_COUPON = {5360000, 5360042, 5360055, 5360056, 5360057};

    static {
        initConsumableDrops();
        initCustomGlobalDrops();
        initEliteDrop();
        initEliteChannelDrop();
        initArcaneSymbolDrop();
        //initNebulites();
    }

    private static void initConsumableDrops() {
        consumableDropsPerLevel.put(0, Util.makeSet(
                new DropInfo(2000000, 200), // Red Potion
                new DropInfo(2000003, 200)  // Blue Potion
        ));
        consumableDropsPerLevel.put(20, Util.makeSet(
                new DropInfo(2000002, 200), // White Potion
                new DropInfo(2000006, 200)  // Mana Elixir
        ));
        consumableDropsPerLevel.put(40, Util.makeSet(
                new DropInfo(2022000, 200), // Pure Water
                new DropInfo(2022003, 200)  // Unagi
        ));
        consumableDropsPerLevel.put(60, Util.makeSet(
                new DropInfo(2001001, 200), // Ice Cream Pop
                new DropInfo(2001002, 200)  // Very Special Sundae
        ));
        consumableDropsPerLevel.put(80, Util.makeSet(
                new DropInfo(2001001, 200), // Ice Cream Pop
                new DropInfo(2001002, 200)  // Very Special Sundae
        ));
        consumableDropsPerLevel.put(100, Util.makeSet(
                new DropInfo(2020012, 200), // Melting Cheese
                new DropInfo(2020014, 200) // Sunrise Dew
        ));
        consumableDropsPerLevel.put(120, Util.makeSet(
                new DropInfo(2020012, 200), // Melting Cheese
                new DropInfo(2020014, 200) // Sunrise Dew
        ));
        consumableDropsPerLevel.put(140, Util.makeSet(
                new DropInfo(2020013, 100), // Reindeer Milk
                new DropInfo(2020015, 100), // Sunrise Dew
                new DropInfo(2050004, 10)   // All Cure
        ));
    }

    private static void initCustomGlobalDrops() {
        Set<DropInfo> dropInfos_10 = new HashSet<>();
        for (int itemID : ItemConstants.equipDropLv10To40) {
            dropInfos_10.add(new DropInfo(itemID, 3));
        }
        globalDropsPerLevel.put(10, dropInfos_10);

        Set<DropInfo> dropInfos_50 = new HashSet<>();
        for (int itemID : ItemConstants.equipDropLv50To70) {
            dropInfos_50.add(new DropInfo(itemID, 7));
        }
        globalDropsPerLevel.put(50, dropInfos_50);

        Set<DropInfo> dropInfos_80 = new HashSet<>();
        for (int itemID : ItemConstants.equipDropLv80To100) {
            dropInfos_80.add(new DropInfo(itemID, 3));
        }
        globalDropsPerLevel.put(80, dropInfos_80);

        Set<DropInfo> dropInfos_100 = new HashSet<>();
        for (int itemID : ItemConstants.equipDropLv100To150) {
            dropInfos_100.add(new DropInfo(itemID, 3));
        }
        globalDropsPerLevel.put(100, dropInfos_100);
    }

    private static void initEliteDrop() {
        eliteDrop.put(0, Util.makeSet(
                new DropInfo(ItemConstants.SPELL_TRACE_ID, 5000, 10, 50),
                new DropInfo(2433103, 1000, 1, 5), // 1,000 Honor EXP
                new DropInfo(ItemConstants.MYSTICAL_CUBE, 1000, 1, 3),
                new DropInfo(2048716, 100, 1, 3) // Powerful Rebirth Flame
        ));
    }

    private static void initEliteChannelDrop() {
        eliteChannelDrop.put(0, Util.makeSet(new DropInfo(ItemConstants.SPECIAL_BEAUTY_COUPON, 25, 1, 1)));
    }

    private static void initArcaneSymbolDrop() {
        arcaneSymbolDrop.put(0, Util.makeSet(new DropInfo(1712001, 1))); // Arcane Symbol: Vanishing Journey
        arcaneSymbolDrop.put(1, Util.makeSet(new DropInfo(1712002, 1))); // Arcane Symbol: Chu Chu Island
        arcaneSymbolDrop.put(2, Util.makeSet(new DropInfo(1712003, 1))); // Arcane Symbol: Lachelein
        arcaneSymbolDrop.put(3, Util.makeSet(new DropInfo(1712004, 1))); // Arcane Symbol: Arcana
        arcaneSymbolDrop.put(4, Util.makeSet(new DropInfo(1712005, 1))); // Arcane Symbol: Morass
        arcaneSymbolDrop.put(5, Util.makeSet(new DropInfo(1712006, 1))); // Arcane Symbol: Esfera
    }

    private static void initNebulites() {
        nebuliteRanks.put(3060000, NebuliteType.Rank_D.getVal()); // [D] Nebulite (STR)
        nebuliteRanks.put(3060001, NebuliteType.Rank_D.getVal()); // [D] Nebulite (STR)
        nebuliteRanks.put(3060002, NebuliteType.Rank_D.getVal()); // [D] Nebulite (STR)
        nebuliteRanks.put(3060010, NebuliteType.Rank_D.getVal()); // [D] Nebulite (DEX)
        nebuliteRanks.put(3060011, NebuliteType.Rank_D.getVal()); // [D] Nebulite (DEX)
        nebuliteRanks.put(3060012, NebuliteType.Rank_D.getVal()); // [D] Nebulite (DEX)
        nebuliteRanks.put(3060020, NebuliteType.Rank_D.getVal()); // [D] Nebulite (INT)
        nebuliteRanks.put(3060021, NebuliteType.Rank_D.getVal()); // [D] Nebulite (INT)
        nebuliteRanks.put(3060022, NebuliteType.Rank_D.getVal()); // [D] Nebulite (INT)
        nebuliteRanks.put(3060030, NebuliteType.Rank_D.getVal()); // [D] Nebulite (LUK)
        nebuliteRanks.put(3060031, NebuliteType.Rank_D.getVal()); // [D] Nebulite (LUK)
        nebuliteRanks.put(3060032, NebuliteType.Rank_D.getVal()); // [D] Nebulite (LUK)
        nebuliteRanks.put(3060040, NebuliteType.Rank_D.getVal()); // [D] Nebulite (Max HP)
        nebuliteRanks.put(3060041, NebuliteType.Rank_D.getVal()); // [D] Nebulite (Max HP)
        nebuliteRanks.put(3060042, NebuliteType.Rank_D.getVal()); // [D] Nebulite (Max HP)
        nebuliteRanks.put(3060050, NebuliteType.Rank_D.getVal()); // [D] Nebulite (Max MP)
        nebuliteRanks.put(3060051, NebuliteType.Rank_D.getVal()); // [D] Nebulite (Max MP)
        nebuliteRanks.put(3060052, NebuliteType.Rank_D.getVal()); // [D] Nebulite (Max MP)
        //nebuliteRanks.put(3060060, NebuliteType.Rank_D.getVal()); // [D] Nebulite (DEX)
        //nebuliteRanks.put(3060061, NebuliteType.Rank_D.getVal()); // [D] Nebulite (DEX)
        //nebuliteRanks.put(3060070, NebuliteType.Rank_D.getVal()); // [D] Nebulite (DEX)
        //nebuliteRanks.put(3060071, NebuliteType.Rank_D.getVal()); // [D] Nebulite (DEX)
        nebuliteRanks.put(3060080, NebuliteType.Rank_D.getVal()); // [D] Nebulite (Speed)
        nebuliteRanks.put(3060081, NebuliteType.Rank_D.getVal()); // [D] Nebulite (Speed)
        nebuliteRanks.put(3060090, NebuliteType.Rank_D.getVal()); // [D] Nebulite (Jump)
        nebuliteRanks.put(3060091, NebuliteType.Rank_D.getVal()); // [D] Nebulite (Jump)
        nebuliteRanks.put(3060100, NebuliteType.Rank_D.getVal()); // [D] Nebulite (Weapon ATT)
        nebuliteRanks.put(3060110, NebuliteType.Rank_D.getVal()); // [D] Nebulite (Magic ATT)
        nebuliteRanks.put(3060120, NebuliteType.Rank_D.getVal()); // [D] Nebulite (Weapon Defense)
        nebuliteRanks.put(3060121, NebuliteType.Rank_D.getVal()); // [D] Nebulite (Weapon Defense)
        nebuliteRanks.put(3060122, NebuliteType.Rank_D.getVal()); // [D] Nebulite (Weapon Defense)
        nebuliteRanks.put(3060130, NebuliteType.Rank_D.getVal()); // [D] Nebulite (Magic Defense)
        nebuliteRanks.put(3060131, NebuliteType.Rank_D.getVal()); // [D] Nebulite (Magic Defense)
        nebuliteRanks.put(3060132, NebuliteType.Rank_D.getVal()); // [D] Nebulite (Magic Defense)
        nebuliteRanks.put(3060140, NebuliteType.Rank_D.getVal()); // [D] Nebulite (Face)
        nebuliteRanks.put(3060150, NebuliteType.Rank_D.getVal()); // [D] Nebulite (Face)
        nebuliteRanks.put(3060160, NebuliteType.Rank_D.getVal()); // [D] Nebulite (Face)
        nebuliteRanks.put(3060170, NebuliteType.Rank_D.getVal()); // [D] Nebulite (Face)
        nebuliteRanks.put(3060180, NebuliteType.Rank_D.getVal()); // [D] Nebulite (Face)
        nebuliteRanks.put(3061000, NebuliteType.Rank_C.getVal()); // [C] Nebulite (STR)
        nebuliteRanks.put(3061001, NebuliteType.Rank_C.getVal()); // [C] Nebulite (STR)
        nebuliteRanks.put(3061010, NebuliteType.Rank_C.getVal()); // [C] Nebulite (DEX)
        nebuliteRanks.put(3061011, NebuliteType.Rank_C.getVal()); // [C] Nebulite (DEX)
        nebuliteRanks.put(3061020, NebuliteType.Rank_C.getVal()); // [C] Nebulite (INT)
        nebuliteRanks.put(3061021, NebuliteType.Rank_C.getVal()); // [C] Nebulite (INT)
        nebuliteRanks.put(3061030, NebuliteType.Rank_C.getVal()); // [C] Nebulite (LUK)
        nebuliteRanks.put(3061031, NebuliteType.Rank_C.getVal()); // [C] Nebulite (LUK)
        nebuliteRanks.put(3061040, NebuliteType.Rank_C.getVal()); // [C] Nebulite (Max HP)
        nebuliteRanks.put(3061041, NebuliteType.Rank_C.getVal()); // [C] Nebulite (Max HP)
        nebuliteRanks.put(3061042, NebuliteType.Rank_C.getVal()); // [C] Nebulite (Max HP)
        nebuliteRanks.put(3061050, NebuliteType.Rank_C.getVal()); // [C] Nebulite (Max MP)
        nebuliteRanks.put(3061051, NebuliteType.Rank_C.getVal()); // [C] Nebulite (Max MP)
        nebuliteRanks.put(3061052, NebuliteType.Rank_C.getVal()); // [C] Nebulite (Max MP)
        //nebuliteRanks.put(3061060, NebuliteType.Rank_C.getVal()); // [C] Nebulite (DEX)
        //nebuliteRanks.put(3061061, NebuliteType.Rank_C.getVal()); // [C] Nebulite (DEX)
        //nebuliteRanks.put(3061070, NebuliteType.Rank_C.getVal()); // [C] Nebulite (DEX)
        //nebuliteRanks.put(3061071, NebuliteType.Rank_C.getVal()); // [C] Nebulite (DEX)
        nebuliteRanks.put(3061080, NebuliteType.Rank_C.getVal()); // [C] Nebulite (Speed)
        nebuliteRanks.put(3061081, NebuliteType.Rank_C.getVal()); // [C] Nebulite (Speed)
        nebuliteRanks.put(3061090, NebuliteType.Rank_C.getVal()); // [C] Nebulite (Jump)
        nebuliteRanks.put(3061091, NebuliteType.Rank_C.getVal()); // [C] Nebulite (Jump)
        nebuliteRanks.put(3061100, NebuliteType.Rank_C.getVal()); // [C] Nebulite (Weapon ATT)
        nebuliteRanks.put(3061110, NebuliteType.Rank_C.getVal()); // [C] Nebulite (Magic ATT)
        nebuliteRanks.put(3061120, NebuliteType.Rank_C.getVal()); // [C] Nebulite (Weapon Defense)
        nebuliteRanks.put(3061121, NebuliteType.Rank_C.getVal()); // [C] Nebulite (Weapon Defense)
        nebuliteRanks.put(3061122, NebuliteType.Rank_C.getVal()); // [C] Nebulite (Weapon Defense)
        nebuliteRanks.put(3061123, NebuliteType.Rank_C.getVal()); // [C] Nebulite (Weapon Defense)
        nebuliteRanks.put(3061124, NebuliteType.Rank_C.getVal()); // [C] Nebulite (Weapon Defense)
        nebuliteRanks.put(3061125, NebuliteType.Rank_C.getVal()); // [C] Nebulite (Weapon Defense)
        nebuliteRanks.put(3061130, NebuliteType.Rank_C.getVal()); // [C] Nebulite (Magic Defense)
        nebuliteRanks.put(3061131, NebuliteType.Rank_C.getVal()); // [C] Nebulite (Magic Defense)
        nebuliteRanks.put(3061132, NebuliteType.Rank_C.getVal()); // [C] Nebulite (Magic Defense)
        nebuliteRanks.put(3061133, NebuliteType.Rank_C.getVal()); // [C] Nebulite (Magic Defense)
        nebuliteRanks.put(3061134, NebuliteType.Rank_C.getVal()); // [C] Nebulite (Magic Defense)
        nebuliteRanks.put(3061135, NebuliteType.Rank_C.getVal()); // [C] Nebulite (Magic Defense)
        nebuliteRanks.put(3061140, NebuliteType.Rank_C.getVal()); // [C] Nebulite (STR %)
        nebuliteRanks.put(3061150, NebuliteType.Rank_C.getVal()); // [C] Nebulite (DEX %)
        nebuliteRanks.put(3061160, NebuliteType.Rank_C.getVal()); // [C] Nebulite (INT %)
        nebuliteRanks.put(3061170, NebuliteType.Rank_C.getVal()); // [C] Nebulite (LUK %)
        nebuliteRanks.put(3061180, NebuliteType.Rank_C.getVal()); // [C] Nebulite (Max HP %)
        nebuliteRanks.put(3061190, NebuliteType.Rank_C.getVal()); // [C] Nebulite (Max MP %)
        nebuliteRanks.put(3061200, NebuliteType.Rank_C.getVal()); // [C] Nebulite (DEX %)
        nebuliteRanks.put(3061210, NebuliteType.Rank_C.getVal()); // [C] Nebulite (DEX %)
        nebuliteRanks.put(3061220, NebuliteType.Rank_C.getVal()); // [C] Nebulite (Weapon ATT %)
        nebuliteRanks.put(3061230, NebuliteType.Rank_C.getVal()); // [C] Nebulite (Magic ATT %)
        nebuliteRanks.put(3061240, NebuliteType.Rank_C.getVal()); // [C] Nebulite (Weapon Defense %)
        nebuliteRanks.put(3061250, NebuliteType.Rank_C.getVal()); // [C] Nebulite (Magic Defense %)
        nebuliteRanks.put(3061260, NebuliteType.Rank_C.getVal()); // [C] Nebulite (Crit Rate %)
        nebuliteRanks.put(3061270, NebuliteType.Rank_C.getVal()); // [C] Nebulite (All Damage %)
        nebuliteRanks.put(3061271, NebuliteType.Rank_C.getVal()); // [C] Nebulite (All Damage %)
        nebuliteRanks.put(3061280, NebuliteType.Rank_C.getVal()); // [C] Nebulite (All Stat)
        nebuliteRanks.put(3061290, NebuliteType.Rank_C.getVal()); // [C] Nebulite (Recovery HP)
        nebuliteRanks.put(3061291, NebuliteType.Rank_C.getVal()); // [C] Nebulite (Recovery HP)
        nebuliteRanks.put(3061292, NebuliteType.Rank_C.getVal()); // [C] Nebulite (Recovery HP)
        nebuliteRanks.put(3061293, NebuliteType.Rank_C.getVal()); // [C] Nebulite (Recovery HP)
        nebuliteRanks.put(3061294, NebuliteType.Rank_C.getVal()); // [C] Nebulite (Recovery HP)
        nebuliteRanks.put(3061295, NebuliteType.Rank_C.getVal()); // [C] Nebulite (Recovery HP)
        nebuliteRanks.put(3061300, NebuliteType.Rank_C.getVal()); // [C] Nebulite (Recovery MP)
        nebuliteRanks.put(3061301, NebuliteType.Rank_C.getVal()); // [C] Nebulite (Recovery MP)
        nebuliteRanks.put(3061302, NebuliteType.Rank_C.getVal()); // [C] Nebulite (Recovery MP)
        nebuliteRanks.put(3061303, NebuliteType.Rank_C.getVal()); // [C] Nebulite (Recovery MP)
        nebuliteRanks.put(3061304, NebuliteType.Rank_C.getVal()); // [C] Nebulite (Recovery MP)
        nebuliteRanks.put(3061305, NebuliteType.Rank_C.getVal()); // [C] Nebulite (Recovery MP)
        nebuliteRanks.put(3061310, NebuliteType.Rank_C.getVal()); // [C] Nebulite (Recovery HP)
        nebuliteRanks.put(3061311, NebuliteType.Rank_C.getVal()); // [C] Nebulite (Recovery HP)
        nebuliteRanks.put(3061312, NebuliteType.Rank_C.getVal()); // [C] Nebulite (Recovery HP)
        nebuliteRanks.put(3061313, NebuliteType.Rank_C.getVal()); // [C] Nebulite (Recovery HP)
        nebuliteRanks.put(3061314, NebuliteType.Rank_C.getVal()); // [C] Nebulite (Recovery HP)
        nebuliteRanks.put(3061315, NebuliteType.Rank_C.getVal()); // [C] Nebulite (Recovery HP)
        nebuliteRanks.put(3061320, NebuliteType.Rank_C.getVal()); // [C] Nebulite (Recovery MP)
        nebuliteRanks.put(3061321, NebuliteType.Rank_C.getVal()); // [C] Nebulite (Recovery MP)
        nebuliteRanks.put(3061322, NebuliteType.Rank_C.getVal()); // [C] Nebulite (Recovery MP)
        nebuliteRanks.put(3061323, NebuliteType.Rank_C.getVal()); // [C] Nebulite (Recovery MP)
        nebuliteRanks.put(3061324, NebuliteType.Rank_C.getVal()); // [C] Nebulite (Recovery MP)
        nebuliteRanks.put(3061325, NebuliteType.Rank_C.getVal()); // [C] Nebulite (Recovery MP)
        nebuliteRanks.put(3061330, NebuliteType.Rank_C.getVal()); // [C] Nebulite (Abnormal Status)
        nebuliteRanks.put(3061331, NebuliteType.Rank_C.getVal()); // [C] Nebulite (Abnormal Status)
        nebuliteRanks.put(3061332, NebuliteType.Rank_C.getVal()); // [C] Nebulite (Abnormal Status)
        nebuliteRanks.put(3061333, NebuliteType.Rank_C.getVal()); // [C] Nebulite (Abnormal Status)
        nebuliteRanks.put(3061334, NebuliteType.Rank_C.getVal()); // [C] Nebulite (Abnormal Status)
        nebuliteRanks.put(3061335, NebuliteType.Rank_C.getVal()); // [C] Nebulite (Abnormal Status)
        nebuliteRanks.put(3061340, NebuliteType.Rank_C.getVal()); // [C] Nebulite (Abnormal Status)
        nebuliteRanks.put(3061341, NebuliteType.Rank_C.getVal()); // [C] Nebulite (Abnormal Status)
        nebuliteRanks.put(3061350, NebuliteType.Rank_C.getVal()); // [C] Nebulite (Abnormal Status)
        nebuliteRanks.put(3061351, NebuliteType.Rank_C.getVal()); // [C] Nebulite (Abnormal Status)
        nebuliteRanks.put(3061360, NebuliteType.Rank_C.getVal()); // [C] Nebulite (Abnormal Status)
        nebuliteRanks.put(3061361, NebuliteType.Rank_C.getVal()); // [C] Nebulite (Abnormal Status)
        nebuliteRanks.put(3061362, NebuliteType.Rank_C.getVal()); // [C] Nebulite (Abnormal Status)
        nebuliteRanks.put(3061370, NebuliteType.Rank_C.getVal()); // [C] Nebulite (Abnormal Status)
        nebuliteRanks.put(3061371, NebuliteType.Rank_C.getVal()); // [C] Nebulite (Abnormal Status)
        nebuliteRanks.put(3061380, NebuliteType.Rank_C.getVal()); // [C] Nebulite (Abnormal Status)
        nebuliteRanks.put(3061381, NebuliteType.Rank_C.getVal()); // [C] Nebulite (Abnormal Status)
        nebuliteRanks.put(3061390, NebuliteType.Rank_C.getVal()); // [C] Nebulite (Ignore monster's defense)
        nebuliteRanks.put(3062000, NebuliteType.Rank_B.getVal()); // [B] Nebulite (STR)
        nebuliteRanks.put(3062001, NebuliteType.Rank_B.getVal()); // [B] Nebulite (STR)
        nebuliteRanks.put(3062010, NebuliteType.Rank_B.getVal()); // [B] Nebulite (DEX)
        nebuliteRanks.put(3062011, NebuliteType.Rank_B.getVal()); // [B] Nebulite (DEX)
        nebuliteRanks.put(3062020, NebuliteType.Rank_B.getVal()); // [B] Nebulite (INT)
        nebuliteRanks.put(3062021, NebuliteType.Rank_B.getVal()); // [B] Nebulite (INT)
        nebuliteRanks.put(3062030, NebuliteType.Rank_B.getVal()); // [B] Nebulite (LUK)
        nebuliteRanks.put(3062031, NebuliteType.Rank_B.getVal()); // [B] Nebulite (LUK)
        nebuliteRanks.put(3062040, NebuliteType.Rank_B.getVal()); // [B] Nebulite (Max HP)
        nebuliteRanks.put(3062041, NebuliteType.Rank_B.getVal()); // [B] Nebulite (Max HP)
        nebuliteRanks.put(3062042, NebuliteType.Rank_B.getVal()); // [B] Nebulite (Max HP)
        nebuliteRanks.put(3062050, NebuliteType.Rank_B.getVal()); // [B] Nebulite (Max MP)
        nebuliteRanks.put(3062051, NebuliteType.Rank_B.getVal()); // [B] Nebulite (Max MP)
        nebuliteRanks.put(3062052, NebuliteType.Rank_B.getVal()); // [B] Nebulite (Max MP)
        //nebuliteRanks.put(3062060, NebuliteType.Rank_B.getVal()); // [B] Nebulite (DEX)
        //nebuliteRanks.put(3062061, NebuliteType.Rank_B.getVal()); // [B] Nebulite (DEX)
        nebuliteRanks.put(3062062, NebuliteType.Rank_B.getVal()); // [B] Nebulite (DEX)
        nebuliteRanks.put(3062070, NebuliteType.Rank_B.getVal()); // [B] Nebulite (DEX)
        nebuliteRanks.put(3062071, NebuliteType.Rank_B.getVal()); // [B] Nebulite (DEX)
        nebuliteRanks.put(3062072, NebuliteType.Rank_B.getVal()); // [B] Nebulite (DEX)
        nebuliteRanks.put(3062080, NebuliteType.Rank_B.getVal()); // [B] Nebulite (Speed)
        nebuliteRanks.put(3062081, NebuliteType.Rank_B.getVal()); // [B] Nebulite (Speed)
        nebuliteRanks.put(3062090, NebuliteType.Rank_B.getVal()); // [B] Nebulite (Jump)
        nebuliteRanks.put(3062091, NebuliteType.Rank_B.getVal()); // [B] Nebulite (Jump)
        nebuliteRanks.put(3062100, NebuliteType.Rank_B.getVal()); // [B] Nebulite (Weapon ATT)
        nebuliteRanks.put(3062101, NebuliteType.Rank_B.getVal()); // [B] Nebulite (Weapon ATT)
        nebuliteRanks.put(3062110, NebuliteType.Rank_B.getVal()); // [B] Nebulite (Magic ATT)
        nebuliteRanks.put(3062111, NebuliteType.Rank_B.getVal()); // [B] Nebulite (Magic ATT)
        nebuliteRanks.put(3062120, NebuliteType.Rank_B.getVal()); // [B] Nebulite (Weapon Defense)
        nebuliteRanks.put(3062121, NebuliteType.Rank_B.getVal()); // [B] Nebulite (Weapon Defense)
        nebuliteRanks.put(3062122, NebuliteType.Rank_B.getVal()); // [B] Nebulite (Weapon Defense)
        nebuliteRanks.put(3062130, NebuliteType.Rank_B.getVal()); // [B] Nebulite (Magic Defense)
        nebuliteRanks.put(3062131, NebuliteType.Rank_B.getVal()); // [B] Nebulite (Magic Defense)
        nebuliteRanks.put(3062132, NebuliteType.Rank_B.getVal()); // [B] Nebulite (Magic Defense)
        nebuliteRanks.put(3062140, NebuliteType.Rank_B.getVal()); // [B] Nebulite (STR %)
        nebuliteRanks.put(3062150, NebuliteType.Rank_B.getVal()); // [B] Nebulite (DEX %)
        nebuliteRanks.put(3062160, NebuliteType.Rank_B.getVal()); // [B] Nebulite (INT %)
        nebuliteRanks.put(3062170, NebuliteType.Rank_B.getVal()); // [B] Nebulite (LUK %)
        nebuliteRanks.put(3062180, NebuliteType.Rank_B.getVal()); // [B] Nebulite (Max HP %)
        nebuliteRanks.put(3062190, NebuliteType.Rank_B.getVal()); // [B] Nebulite (Max MP %)
        nebuliteRanks.put(3062200, NebuliteType.Rank_B.getVal()); // [B] Nebulite (DEX %)
        nebuliteRanks.put(3062210, NebuliteType.Rank_B.getVal()); // [B] Nebulite (DEX %)
        nebuliteRanks.put(3062220, NebuliteType.Rank_B.getVal()); // [B] Nebulite (Weapon ATT %)
        nebuliteRanks.put(3062230, NebuliteType.Rank_B.getVal()); // [B] Nebulite (Magic ATT %)
        nebuliteRanks.put(3062240, NebuliteType.Rank_B.getVal()); // [B] Nebulite (Weapon Defense %)
        nebuliteRanks.put(3062250, NebuliteType.Rank_B.getVal()); // [B] Nebulite (Magic Defense %)
        nebuliteRanks.put(3062260, NebuliteType.Rank_B.getVal()); // [B] Nebulite (Crit Rate %)
        nebuliteRanks.put(3062261, NebuliteType.Rank_B.getVal()); // [B] Nebulite (Crit Rate %)
        nebuliteRanks.put(3062270, NebuliteType.Rank_B.getVal()); // [B] Nebulite (All Damage %)
        nebuliteRanks.put(3062271, NebuliteType.Rank_B.getVal()); // [B] Nebulite (All Damage %)
        nebuliteRanks.put(3062280, NebuliteType.Rank_B.getVal()); // [B] Nebulite (All Stat %)
        nebuliteRanks.put(3062281, NebuliteType.Rank_B.getVal()); // [B] Nebulite (All Stat %)
        nebuliteRanks.put(3062290, NebuliteType.Rank_B.getVal()); // [B] Nebulite (Recovery HP)
        nebuliteRanks.put(3062291, NebuliteType.Rank_B.getVal()); // [B] Nebulite (Recovery HP)
        nebuliteRanks.put(3062292, NebuliteType.Rank_B.getVal()); // [B] Nebulite (Recovery HP)
        nebuliteRanks.put(3062293, NebuliteType.Rank_B.getVal()); // [B] Nebulite (Recovery HP)
        nebuliteRanks.put(3062294, NebuliteType.Rank_B.getVal()); // [B] Nebulite (Recovery HP)
        nebuliteRanks.put(3062295, NebuliteType.Rank_B.getVal()); // [B] Nebulite (Recovery HP)
        nebuliteRanks.put(3062300, NebuliteType.Rank_B.getVal()); // [B] Nebulite (Recovery MP)
        nebuliteRanks.put(3062301, NebuliteType.Rank_B.getVal()); // [B] Nebulite (Recovery MP)
        nebuliteRanks.put(3062302, NebuliteType.Rank_B.getVal()); // [B] Nebulite (Recovery MP)
        nebuliteRanks.put(3062303, NebuliteType.Rank_B.getVal()); // [B] Nebulite (Recovery MP)
        nebuliteRanks.put(3062304, NebuliteType.Rank_B.getVal()); // [B] Nebulite (Recovery MP)
        nebuliteRanks.put(3062305, NebuliteType.Rank_B.getVal()); // [B] Nebulite (Recovery MP)
        nebuliteRanks.put(3062310, NebuliteType.Rank_B.getVal()); // [B] Nebulite (Ignore monster's defense)
        nebuliteRanks.put(3062320, NebuliteType.Rank_B.getVal()); // [B] Nebulite (Damage defense)
        nebuliteRanks.put(3062321, NebuliteType.Rank_B.getVal()); // [B] Nebulite (Damage defense)
        nebuliteRanks.put(3062322, NebuliteType.Rank_B.getVal()); // [B] Nebulite (Damage defense)
        nebuliteRanks.put(3062323, NebuliteType.Rank_B.getVal()); // [B] Nebulite (Damage defense)
        nebuliteRanks.put(3062324, NebuliteType.Rank_B.getVal()); // [B] Nebulite (Damage defense)
        nebuliteRanks.put(3062325, NebuliteType.Rank_B.getVal()); // [B] Nebulite (Damage defense)
        nebuliteRanks.put(3062330, NebuliteType.Rank_B.getVal()); // [B] Nebulite (Damage defense)
        nebuliteRanks.put(3062331, NebuliteType.Rank_B.getVal()); // [B] Nebulite (Damage defense)
        nebuliteRanks.put(3062332, NebuliteType.Rank_B.getVal()); // [B] Nebulite (Damage defense)
        nebuliteRanks.put(3062333, NebuliteType.Rank_B.getVal()); // [B] Nebulite (Damage defense)
        nebuliteRanks.put(3062334, NebuliteType.Rank_B.getVal()); // [B] Nebulite (Damage defense)
        nebuliteRanks.put(3062335, NebuliteType.Rank_B.getVal()); // [B] Nebulite (Damage defense)
        nebuliteRanks.put(3062340, NebuliteType.Rank_B.getVal()); // [B] Nebulite (Damage defense)
        nebuliteRanks.put(3062341, NebuliteType.Rank_B.getVal()); // [B] Nebulite (Damage defense)
        nebuliteRanks.put(3062342, NebuliteType.Rank_B.getVal()); // [B] Nebulite (Damage defense)
        nebuliteRanks.put(3062343, NebuliteType.Rank_B.getVal()); // [B] Nebulite (Damage defense)
        nebuliteRanks.put(3062344, NebuliteType.Rank_B.getVal()); // [B] Nebulite (Damage defense)
        nebuliteRanks.put(3062345, NebuliteType.Rank_B.getVal()); // [B] Nebulite (Damage defense)
        nebuliteRanks.put(3062350, NebuliteType.Rank_B.getVal()); // [B] Nebulite (Invincibility)
        nebuliteRanks.put(3062360, NebuliteType.Rank_B.getVal()); // [B] Nebulite (Abnormal Status Time)
        nebuliteRanks.put(3062370, NebuliteType.Rank_B.getVal()); // [B] Nebulite (Recovery HP)
        nebuliteRanks.put(3062371, NebuliteType.Rank_B.getVal()); // [B] Nebulite (Recovery HP)
        nebuliteRanks.put(3062372, NebuliteType.Rank_B.getVal()); // [B] Nebulite (Recovery HP)
        nebuliteRanks.put(3062373, NebuliteType.Rank_B.getVal()); // [B] Nebulite (Recovery HP)
        nebuliteRanks.put(3062374, NebuliteType.Rank_B.getVal()); // [B] Nebulite (Recovery HP)
        nebuliteRanks.put(3062375, NebuliteType.Rank_B.getVal()); // [B] Nebulite (Recovery HP)
        nebuliteRanks.put(3062380, NebuliteType.Rank_B.getVal()); // [B] Nebulite (Recovery MP)
        nebuliteRanks.put(3062381, NebuliteType.Rank_B.getVal()); // [B] Nebulite (Recovery MP)
        nebuliteRanks.put(3062382, NebuliteType.Rank_B.getVal()); // [B] Nebulite (Recovery MP)
        nebuliteRanks.put(3062383, NebuliteType.Rank_B.getVal()); // [B] Nebulite (Recovery MP)
        nebuliteRanks.put(3062384, NebuliteType.Rank_B.getVal()); // [B] Nebulite (Recovery MP)
        nebuliteRanks.put(3062385, NebuliteType.Rank_B.getVal()); // [B] Nebulite (Recovery MP)
        nebuliteRanks.put(3063000, NebuliteType.Rank_A.getVal()); // [A] Nebulite (STR)
        nebuliteRanks.put(3063001, NebuliteType.Rank_A.getVal()); // [A] Nebulite (STR)
        nebuliteRanks.put(3063010, NebuliteType.Rank_A.getVal()); // [A] Nebulite (DEX)
        nebuliteRanks.put(3063011, NebuliteType.Rank_A.getVal()); // [A] Nebulite (DEX)
        nebuliteRanks.put(3063020, NebuliteType.Rank_A.getVal()); // [A] Nebulite (INT)
        nebuliteRanks.put(3063021, NebuliteType.Rank_A.getVal()); // [A] Nebulite (INT)
        nebuliteRanks.put(3063030, NebuliteType.Rank_A.getVal()); // [A] Nebulite (LUK)
        nebuliteRanks.put(3063031, NebuliteType.Rank_A.getVal()); // [A] Nebulite (LUK)
        nebuliteRanks.put(3063040, NebuliteType.Rank_A.getVal()); // [A] Nebulite (Max HP)
        nebuliteRanks.put(3063041, NebuliteType.Rank_A.getVal()); // [A] Nebulite (Max HP)
        nebuliteRanks.put(3063042, NebuliteType.Rank_A.getVal()); // [A] Nebulite (Max HP)
        nebuliteRanks.put(3063050, NebuliteType.Rank_A.getVal()); // [A] Nebulite (Max MP)
        nebuliteRanks.put(3063051, NebuliteType.Rank_A.getVal()); // [A] Nebulite (Max MP)
        nebuliteRanks.put(3063052, NebuliteType.Rank_A.getVal()); // [A] Nebulite (Max MP)
        //nebuliteRanks.put(3063060, NebuliteType.Rank_A.getVal()); // [A] Nebulite (DEX)
        //nebuliteRanks.put(3063061, NebuliteType.Rank_A.getVal()); // [A] Nebulite (DEX)
        //nebuliteRanks.put(3063062, NebuliteType.Rank_A.getVal()); // [A] Nebulite (DEX)
        //nebuliteRanks.put(3063063, NebuliteType.Rank_A.getVal()); // [A] Nebulite (DEX)
        //nebuliteRanks.put(3063070, NebuliteType.Rank_A.getVal()); // [A] Nebulite (DEX)
        //nebuliteRanks.put(3063071, NebuliteType.Rank_A.getVal()); // [A] Nebulite (DEX)
        //nebuliteRanks.put(3063072, NebuliteType.Rank_A.getVal()); // [A] Nebulite (DEX)
        //nebuliteRanks.put(3063073, NebuliteType.Rank_A.getVal()); // [A] Nebulite (DEX)
        nebuliteRanks.put(3063080, NebuliteType.Rank_A.getVal()); // [A] Nebulite (Speed)
        nebuliteRanks.put(3063081, NebuliteType.Rank_A.getVal()); // [A] Nebulite (Speed)
        nebuliteRanks.put(3063090, NebuliteType.Rank_A.getVal()); // [A] Nebulite (Jump)
        nebuliteRanks.put(3063091, NebuliteType.Rank_A.getVal()); // [A] Nebulite (Jump)
        nebuliteRanks.put(3063100, NebuliteType.Rank_A.getVal()); // [A] Nebulite (Weapon ATT)
        nebuliteRanks.put(3063101, NebuliteType.Rank_A.getVal()); // [A] Nebulite (Weapon ATT)
        nebuliteRanks.put(3063110, NebuliteType.Rank_A.getVal()); // [A] Nebulite (Magic ATT)
        nebuliteRanks.put(3063111, NebuliteType.Rank_A.getVal()); // [A] Nebulite (Magic ATT)
        nebuliteRanks.put(3063120, NebuliteType.Rank_A.getVal()); // [A] Nebulite (Weapon Defense)
        nebuliteRanks.put(3063121, NebuliteType.Rank_A.getVal()); // [A] Nebulite (Weapon Defense)
        nebuliteRanks.put(3063122, NebuliteType.Rank_A.getVal()); // [A] Nebulite (Weapon Defense)
        nebuliteRanks.put(3063130, NebuliteType.Rank_A.getVal()); // [A] Nebulite (Magic Defense)
        nebuliteRanks.put(3063131, NebuliteType.Rank_A.getVal()); // [A] Nebulite (Magic Defense)
        nebuliteRanks.put(3063132, NebuliteType.Rank_A.getVal()); // [A] Nebulite (Magic Defense)
        nebuliteRanks.put(3063140, NebuliteType.Rank_A.getVal()); // [A] Nebulite (STR %)
        nebuliteRanks.put(3063141, NebuliteType.Rank_A.getVal()); // [A] Nebulite (STR %)
        nebuliteRanks.put(3063150, NebuliteType.Rank_A.getVal()); // [A] Nebulite (DEX %)
        nebuliteRanks.put(3063151, NebuliteType.Rank_A.getVal()); // [A] Nebulite (DEX %)
        nebuliteRanks.put(3063160, NebuliteType.Rank_A.getVal()); // [A] Nebulite (INT %)
        nebuliteRanks.put(3063161, NebuliteType.Rank_A.getVal()); // [A] Nebulite (INT %)
        nebuliteRanks.put(3063170, NebuliteType.Rank_A.getVal()); // [A] Nebulite (LUK %)
        nebuliteRanks.put(3063171, NebuliteType.Rank_A.getVal()); // [A] Nebulite (LUK %)
        nebuliteRanks.put(3063180, NebuliteType.Rank_A.getVal()); // [A] Nebulite (Max HP %)
        nebuliteRanks.put(3063181, NebuliteType.Rank_A.getVal()); // [A] Nebulite (Max HP %)
        nebuliteRanks.put(3063190, NebuliteType.Rank_A.getVal()); // [A] Nebulite (Max MP %)
        nebuliteRanks.put(3063191, NebuliteType.Rank_A.getVal()); // [A] Nebulite (Max MP %)
        //nebuliteRanks.put(3063200, NebuliteType.Rank_A.getVal()); // [A] Nebulite (DEX %)
        //nebuliteRanks.put(3063201, NebuliteType.Rank_A.getVal()); // [A] Nebulite (DEX %)
        //nebuliteRanks.put(3063210, NebuliteType.Rank_A.getVal()); // [A] Nebulite (DEX %)
        //nebuliteRanks.put(3063211, NebuliteType.Rank_A.getVal()); // [A] Nebulite (DEX %)
        nebuliteRanks.put(3063220, NebuliteType.Rank_A.getVal()); // [A] Nebulite (Weapon ATT %)
        nebuliteRanks.put(3063221, NebuliteType.Rank_A.getVal()); // [A] Nebulite (Weapon ATT %)
        nebuliteRanks.put(3063230, NebuliteType.Rank_A.getVal()); // [A] Nebulite (Magic ATT %)
        nebuliteRanks.put(3063231, NebuliteType.Rank_A.getVal()); // [A] Nebulite (Magic ATT %)
        nebuliteRanks.put(3063240, NebuliteType.Rank_A.getVal()); // [A] Nebulite (Weapon Defense %)
        nebuliteRanks.put(3063241, NebuliteType.Rank_A.getVal()); // [A] Nebulite (Weapon Defense %)
        nebuliteRanks.put(3063250, NebuliteType.Rank_A.getVal()); // [A] Nebulite (Magic Defense %)
        nebuliteRanks.put(3063251, NebuliteType.Rank_A.getVal()); // [A] Nebulite (Magic Defense %)
        nebuliteRanks.put(3063260, NebuliteType.Rank_A.getVal()); // [A] Nebulite (Crit Rate %)
        nebuliteRanks.put(3063261, NebuliteType.Rank_A.getVal()); // [A] Nebulite (Crit Rate %)
        nebuliteRanks.put(3063270, NebuliteType.Rank_A.getVal()); // [A] Nebulite (All Damage %)
        nebuliteRanks.put(3063271, NebuliteType.Rank_A.getVal()); // [A] Nebulite (All Damage %)
        nebuliteRanks.put(3063280, NebuliteType.Rank_A.getVal()); // [A] Nebulite (All Stat %)
        nebuliteRanks.put(3063281, NebuliteType.Rank_A.getVal()); // [A] Nebulite (All Stat %)
        nebuliteRanks.put(3063290, NebuliteType.Rank_A.getVal()); // [A] Nebulite (All Skill Level)
        nebuliteRanks.put(3063300, NebuliteType.Rank_A.getVal()); // [A] Nebulite (Ignore monster's defense)
        nebuliteRanks.put(3063310, NebuliteType.Rank_A.getVal()); // [A] Nebulite (Damage defense %)
        nebuliteRanks.put(3063320, NebuliteType.Rank_A.getVal()); // [A] Nebulite (Invincibility)
        nebuliteRanks.put(3063330, NebuliteType.Rank_A.getVal()); // [A] Nebulite (Invincibility)
        nebuliteRanks.put(3063340, NebuliteType.Rank_A.getVal()); // [A] Nebulite (Recovery Rate)
        nebuliteRanks.put(3063341, NebuliteType.Rank_A.getVal()); // [A] Nebulite (Recovery Rate)
        nebuliteRanks.put(3063350, NebuliteType.Rank_A.getVal()); // [A] Nebulite (Boss Damage)
        nebuliteRanks.put(3063351, NebuliteType.Rank_A.getVal()); // [A] Nebulite (Boss Damage)
        nebuliteRanks.put(3063360, NebuliteType.Rank_A.getVal()); // [A] Nebulite (Auto Steal)
        nebuliteRanks.put(3063361, NebuliteType.Rank_A.getVal()); // [A] Nebulite (Auto Steal)
        nebuliteRanks.put(3063370, NebuliteType.Rank_A.getVal()); // [A] Nebulite (Skill)
        nebuliteRanks.put(3063380, NebuliteType.Rank_A.getVal()); // [A] Nebulite (Skill)
        nebuliteRanks.put(3063390, NebuliteType.Rank_A.getVal()); // [A] Nebulite (Skill)
        nebuliteRanks.put(3063400, NebuliteType.Rank_A.getVal()); // [A] Nebulite (Skill)
        nebuliteRanks.put(3064000, NebuliteType.Rank_S.getVal()); // [S] Nebulite (STR)
        nebuliteRanks.put(3064001, NebuliteType.Rank_S.getVal()); // [S] Nebulite (STR)
        nebuliteRanks.put(3064002, NebuliteType.Rank_S.getVal()); // [S] Nebulite (STR)
        nebuliteRanks.put(3064010, NebuliteType.Rank_S.getVal()); // [S] Nebulite (DEX)
        nebuliteRanks.put(3064011, NebuliteType.Rank_S.getVal()); // [S] Nebulite (DEX)
        nebuliteRanks.put(3064012, NebuliteType.Rank_S.getVal()); // [S] Nebulite (DEX)
        nebuliteRanks.put(3064020, NebuliteType.Rank_S.getVal()); // [S] Nebulite (INT)
        nebuliteRanks.put(3064021, NebuliteType.Rank_S.getVal()); // [S] Nebulite (INT)
        nebuliteRanks.put(3064022, NebuliteType.Rank_S.getVal()); // [S] Nebulite (INT)
        nebuliteRanks.put(3064030, NebuliteType.Rank_S.getVal()); // [S] Nebulite (LUK)
        nebuliteRanks.put(3064031, NebuliteType.Rank_S.getVal()); // [S] Nebulite (LUK)
        nebuliteRanks.put(3064032, NebuliteType.Rank_S.getVal()); // [S] Nebulite (LUK)
        nebuliteRanks.put(3064040, NebuliteType.Rank_S.getVal()); // [S] Nebulite (Max HP)
        nebuliteRanks.put(3064041, NebuliteType.Rank_S.getVal()); // [S] Nebulite (Max HP)
        nebuliteRanks.put(3064042, NebuliteType.Rank_S.getVal()); // [S] Nebulite (Max HP)
        nebuliteRanks.put(3064050, NebuliteType.Rank_S.getVal()); // [S] Nebulite (Max MP)
        nebuliteRanks.put(3064051, NebuliteType.Rank_S.getVal()); // [S] Nebulite (Max MP)
        nebuliteRanks.put(3064052, NebuliteType.Rank_S.getVal()); // [S] Nebulite (Max MP)
        //nebuliteRanks.put(3064060, NebuliteType.Rank_S.getVal()); // [S] Nebulite (DEX)
        //nebuliteRanks.put(3064061, NebuliteType.Rank_S.getVal()); // [S] Nebulite (DEX)
        //nebuliteRanks.put(3064062, NebuliteType.Rank_S.getVal()); // [S] Nebulite (DEX)
        //nebuliteRanks.put(3064070, NebuliteType.Rank_S.getVal()); // [S] Nebulite (DEX)
        //nebuliteRanks.put(3064071, NebuliteType.Rank_S.getVal()); // [S] Nebulite (DEX)
        //nebuliteRanks.put(3064072, NebuliteType.Rank_S.getVal()); // [S] Nebulite (DEX)
        nebuliteRanks.put(3064080, NebuliteType.Rank_S.getVal()); // [S] Nebulite (Speed)
        nebuliteRanks.put(3064081, NebuliteType.Rank_S.getVal()); // [S] Nebulite (Speed)
        nebuliteRanks.put(3064090, NebuliteType.Rank_S.getVal()); // [S] Nebulite (Jump)
        nebuliteRanks.put(3064091, NebuliteType.Rank_S.getVal()); // [S] Nebulite (Jump)
        nebuliteRanks.put(3064100, NebuliteType.Rank_S.getVal()); // [S] Nebulite (Weapon ATT)
        nebuliteRanks.put(3064101, NebuliteType.Rank_S.getVal()); // [S] Nebulite (Weapon ATT)
        nebuliteRanks.put(3064110, NebuliteType.Rank_S.getVal()); // [S] Nebulite (Magic ATT)
        nebuliteRanks.put(3064111, NebuliteType.Rank_S.getVal()); // [S] Nebulite (Magic ATT)
        nebuliteRanks.put(3064120, NebuliteType.Rank_S.getVal()); // [S] Nebulite (Weapon Defense)
        nebuliteRanks.put(3064121, NebuliteType.Rank_S.getVal()); // [S] Nebulite (Weapon Defense)
        nebuliteRanks.put(3064122, NebuliteType.Rank_S.getVal()); // [S] Nebulite (Weapon Defense)
        nebuliteRanks.put(3064123, NebuliteType.Rank_S.getVal()); // [S] Nebulite (Weapon Defense)
        nebuliteRanks.put(3064124, NebuliteType.Rank_S.getVal()); // [S] Nebulite (Weapon Defense)
        nebuliteRanks.put(3064130, NebuliteType.Rank_S.getVal()); // [S] Nebulite (Magic Defense)
        nebuliteRanks.put(3064131, NebuliteType.Rank_S.getVal()); // [S] Nebulite (Magic Defense)
        nebuliteRanks.put(3064132, NebuliteType.Rank_S.getVal()); // [S] Nebulite (Magic Defense)
        nebuliteRanks.put(3064133, NebuliteType.Rank_S.getVal()); // [S] Nebulite (Magic Defense)
        nebuliteRanks.put(3064134, NebuliteType.Rank_S.getVal()); // [S] Nebulite (Magic Defense)
        nebuliteRanks.put(3064140, NebuliteType.Rank_S.getVal()); // [S] Nebulite (STR %)
        nebuliteRanks.put(3064141, NebuliteType.Rank_S.getVal()); // [S] Nebulite (STR %)
        nebuliteRanks.put(3064150, NebuliteType.Rank_S.getVal()); // [S] Nebulite (DEX %)
        nebuliteRanks.put(3064151, NebuliteType.Rank_S.getVal()); // [S] Nebulite (DEX %)
        nebuliteRanks.put(3064160, NebuliteType.Rank_S.getVal()); // [S] Nebulite (INT %)
        nebuliteRanks.put(3064161, NebuliteType.Rank_S.getVal()); // [S] Nebulite (INT %)
        nebuliteRanks.put(3064170, NebuliteType.Rank_S.getVal()); // [S] Nebulite (LUK %)
        nebuliteRanks.put(3064171, NebuliteType.Rank_S.getVal()); // [S] Nebulite (LUK %)
        nebuliteRanks.put(3064180, NebuliteType.Rank_S.getVal()); // [S] Nebulite (Max HP %)
        nebuliteRanks.put(3064181, NebuliteType.Rank_S.getVal()); // [S] Nebulite (Max HP %)
        nebuliteRanks.put(3064190, NebuliteType.Rank_S.getVal()); // [S] Nebulite (Max MP %)
        nebuliteRanks.put(3064191, NebuliteType.Rank_S.getVal()); // [S] Nebulite (Max MP %)
        //nebuliteRanks.put(3064200, NebuliteType.Rank_S.getVal()); // [S] Nebulite (DEX %)
        //nebuliteRanks.put(3064201, NebuliteType.Rank_S.getVal()); // [S] Nebulite (DEX %)
        //nebuliteRanks.put(3064210, NebuliteType.Rank_S.getVal()); // [S] Nebulite (DEX %)
        //nebuliteRanks.put(3064211, NebuliteType.Rank_S.getVal()); // [S] Nebulite (DEX %)
        nebuliteRanks.put(3064220, NebuliteType.Rank_S.getVal()); // [S] Nebulite (Weapon ATT %)
        nebuliteRanks.put(3064221, NebuliteType.Rank_S.getVal()); // [S] Nebulite (Weapon ATT %)
        nebuliteRanks.put(3064230, NebuliteType.Rank_S.getVal()); // [S] Nebulite (Magic ATT %)
        nebuliteRanks.put(3064231, NebuliteType.Rank_S.getVal()); // [S] Nebulite (Magic ATT %)
        nebuliteRanks.put(3064240, NebuliteType.Rank_S.getVal()); // [S] Nebulite (Weapon Defense %)
        nebuliteRanks.put(3064241, NebuliteType.Rank_S.getVal()); // [S] Nebulite (Weapon Defense %)
        nebuliteRanks.put(3064250, NebuliteType.Rank_S.getVal()); // [S] Nebulite (Magic Defense %)
        nebuliteRanks.put(3064251, NebuliteType.Rank_S.getVal()); // [S] Nebulite (Magic Defense %)
        nebuliteRanks.put(3064260, NebuliteType.Rank_S.getVal()); // [S] Nebulite (Crit Rate %)
        nebuliteRanks.put(3064261, NebuliteType.Rank_S.getVal()); // [S] Nebulite (Crit Rate %)
        nebuliteRanks.put(3064290, NebuliteType.Rank_S.getVal()); // [S] Nebulite (All Damage %)
        nebuliteRanks.put(3064291, NebuliteType.Rank_S.getVal()); // [S] Nebulite (All Damage %)
        nebuliteRanks.put(3064300, NebuliteType.Rank_S.getVal()); // [S] Nebulite (All Stat)
        nebuliteRanks.put(3064301, NebuliteType.Rank_S.getVal()); // [S] Nebulite (All Stat)
        nebuliteRanks.put(3064302, NebuliteType.Rank_S.getVal()); // [S] Nebulite (All Stat)
        nebuliteRanks.put(3064310, NebuliteType.Rank_S.getVal()); // [S] Nebulite (All Stat %)
        nebuliteRanks.put(3064311, NebuliteType.Rank_S.getVal()); // [S] Nebulite (All Stat %)
        nebuliteRanks.put(3064320, NebuliteType.Rank_S.getVal()); // [S] Nebulite (All Skill Level)
        nebuliteRanks.put(3064330, NebuliteType.Rank_S.getVal()); // [S] Nebulite (Elemental Resistance)
        nebuliteRanks.put(3064331, NebuliteType.Rank_S.getVal()); // [S] Nebulite (Elemental Resistance)
        nebuliteRanks.put(3064340, NebuliteType.Rank_S.getVal()); // [S] Nebulite (Abnormal Status Resistance)
        nebuliteRanks.put(3064341, NebuliteType.Rank_S.getVal()); // [S] Nebulite (Abnormal Status Resistance)
        nebuliteRanks.put(3064350, NebuliteType.Rank_S.getVal()); // [S] Nebulite (Ignore monster's defense)
        nebuliteRanks.put(3064360, NebuliteType.Rank_S.getVal()); // [S] Nebulite (Damage defense %)
        nebuliteRanks.put(3064370, NebuliteType.Rank_S.getVal()); // [S] Nebulite (Invincibility)
        nebuliteRanks.put(3064380, NebuliteType.Rank_S.getVal()); // [S] Nebulite (Invincibility)
        nebuliteRanks.put(3064390, NebuliteType.Rank_S.getVal()); // [S] Nebulite (Reflect Damage)
        nebuliteRanks.put(3064391, NebuliteType.Rank_S.getVal()); // [S] Nebulite (Reflect Damage)
        nebuliteRanks.put(3064392, NebuliteType.Rank_S.getVal()); // [S] Nebulite (Reflect Damage)
        nebuliteRanks.put(3064393, NebuliteType.Rank_S.getVal()); // [S] Nebulite (Reflect Damage)
        nebuliteRanks.put(3064400, NebuliteType.Rank_S.getVal()); // [S] Nebulite (Skill MP Reduce)
        nebuliteRanks.put(3064401, NebuliteType.Rank_S.getVal()); // [S] Nebulite (Skill MP Reduce)
        nebuliteRanks.put(3064410, NebuliteType.Rank_S.getVal()); // [S] Nebulite (Recovery Rate)
        nebuliteRanks.put(3064420, NebuliteType.Rank_S.getVal()); // [S] Nebulite (Reduce Cooltime)
        nebuliteRanks.put(3064421, NebuliteType.Rank_S.getVal()); // [S] Nebulite (Reduce Cooltime)
        nebuliteRanks.put(3064430, NebuliteType.Rank_S.getVal()); // [S] Nebulite (Boss Damage)
        nebuliteRanks.put(3064431, NebuliteType.Rank_S.getVal()); // [S] Nebulite (Boss Damage)
        nebuliteRanks.put(3064440, NebuliteType.Rank_S.getVal()); // [S] Nebulite (Meso Prop)
        nebuliteRanks.put(3064441, NebuliteType.Rank_S.getVal()); // [S] Nebulite (Meso Prop)
        nebuliteRanks.put(3064442, NebuliteType.Rank_S.getVal()); // [S] Nebulite (Meso Prop)
        nebuliteRanks.put(3064450, NebuliteType.Rank_S.getVal()); // [S] Nebulite (Reward Prop)
        nebuliteRanks.put(3064451, NebuliteType.Rank_S.getVal()); // [S] Nebulite (Reward Prop)
        nebuliteRanks.put(3064452, NebuliteType.Rank_S.getVal()); // [S] Nebulite (Reward Prop)
        nebuliteRanks.put(3064460, NebuliteType.Rank_S.getVal()); // [S] Nebulite (Auto Steal)
        nebuliteRanks.put(3064461, NebuliteType.Rank_S.getVal()); // [S] Nebulite (Auto Steal)
        nebuliteRanks.put(3064470, NebuliteType.Rank_S.getVal()); // [S] Nebulite (Skill)
        nebuliteRanks.put(3064480, NebuliteType.Rank_S.getVal()); // [S] Nebulite (Skill)
        nebuliteRanks.put(3064490, NebuliteType.Rank_S.getVal()); // [S] Nebulite (Skill)
    }

    public static int getGenderFromId(int itemID) {
        int result;

        if (itemID / 1000000 != 1 && getItemPrefix(itemID) != 254 || getItemPrefix(itemID) == 119 || getItemPrefix(itemID) == 168)
            return 2;
        switch (itemID / 1000 % 10) {
            case 0:
                result = 0;
                break;
            case 1:
                result = 1;
                break;
            default:
                return 2;
        }
        return result;
    }

    public static int getBodyPartFromItem(int itemID, int gender) {
        List<Integer> arr = getBodyPartArrayFromItem(itemID, gender);
        return !arr.isEmpty() ? arr.getFirst() : 0;
    }

    /**
     * The 265 {@link BodyPart} of an item in every profile. Enhancement rules (scroll / upgrade tables) are keyed on
     * the 265 BodyPart names, which the 282 slot numbers (e.g. shoulder 28 = MechanicalHeart_OLD) would misname.
     */
    public static int getLegacyBodyPartFromItem(int itemID, int gender) {
        List<Integer> arr = getLegacyBodyPartArrayFromItem(itemID, gender);
        return !arr.isEmpty() ? arr.getFirst() : 0;
    }

    /**
     * Server {@code bagIndex} candidates of an item, primary first. 265: the 265 BodyPart layout (unchanged). 282: the
     * client's table ({@link net.swordie.ms.connection.packet.Tms282BodySlots#storageSlots}, equips README WI-2).
     */
    public static List<Integer> getBodyPartArrayFromItem(int itemID, int genderArg) {
        if (net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) {
            int gender = getGenderFromId(itemID);
            int prefix = getItemPrefix(itemID);
            if (prefix != EquipPrefix.Emblem.getVal() && prefix != EquipPrefix.Bit.getVal()
                    && gender != 2 && genderArg != 2 && gender != genderArg) {
                return new ArrayList<>();
            }
            Equip template = itemID / 1000000 == 1 ? ItemData.getEquipById(itemID) : null;
            return net.swordie.ms.connection.packet.Tms282BodySlots.storageSlots(itemID, template != null && template.isCash());
        }
        return getLegacyBodyPartArrayFromItem(itemID, genderArg);
    }

    private static List<Integer> getLegacyBodyPartArrayFromItem(int itemID, int genderArg) {
        int gender = getGenderFromId(itemID);
        EquipPrefix prefix = EquipPrefix.getByVal(getItemPrefix(itemID));
        List<Integer> bodyPartList = new ArrayList<>();
        if (prefix != EquipPrefix.Emblem && prefix != EquipPrefix.Bit &&
                gender != 2 && genderArg != 2 && gender != genderArg) {
            return bodyPartList;
        }
        if (prefix != null) {
            switch (prefix) {
                case Hat:
                    bodyPartList.add(BodyPart.Hat.getVal());
                    bodyPartList.add(BodyPart.EvanHat.getVal());
                    bodyPartList.add(BodyPart.APHat.getVal());
                    bodyPartList.add(BodyPart.DUHat.getVal());
                    bodyPartList.add(BodyPart.ZeroHat.getVal());
                    bodyPartList.add(BodyPart.CHat.getVal());
                    break;
                case FaceAccessory:
                    bodyPartList.add(BodyPart.FaceAccessory.getVal());
                    bodyPartList.add(BodyPart.APFaceAccessory.getVal());
                    bodyPartList.add(BodyPart.DUFaceAccessory.getVal());
                    bodyPartList.add(BodyPart.ZeroFaceAccessory.getVal());
                    bodyPartList.add(BodyPart.CFaceAccessory.getVal());
                    break;
                case EyeAccessory:
                    bodyPartList.add(BodyPart.EyeAccessory.getVal());
                    bodyPartList.add(BodyPart.ZeroEyeAccessory.getVal());
                    bodyPartList.add(BodyPart.CEyeAccessory.getVal());
                    break;
                case Earrings:
                    bodyPartList.add(BodyPart.Earrings.getVal());
                    bodyPartList.add(BodyPart.ZeroEarrings.getVal());
                    bodyPartList.add(BodyPart.CEarrings.getVal());
                    break;
                case Top:
                case Overall:
                    bodyPartList.add(BodyPart.Top.getVal());
                    bodyPartList.add(BodyPart.APTop.getVal());
                    bodyPartList.add(BodyPart.DUTop.getVal());
                    bodyPartList.add(BodyPart.ZeroTop.getVal());
                    bodyPartList.add(BodyPart.CTop.getVal());
                    break;
                case Bottom:
                    bodyPartList.add(BodyPart.Bottom.getVal());
                    bodyPartList.add(BodyPart.APBottom.getVal());
                    bodyPartList.add(BodyPart.ZeroBottom.getVal());
                    bodyPartList.add(BodyPart.CBottom.getVal());
                    break;
                case Shoes:
                    bodyPartList.add(BodyPart.Shoes.getVal());
                    bodyPartList.add(BodyPart.APShoes.getVal());
                    bodyPartList.add(BodyPart.ZeroShoes.getVal());
                    bodyPartList.add(BodyPart.CShoes.getVal());
                    break;
                case Gloves:
                    bodyPartList.add(BodyPart.Gloves.getVal());
                    bodyPartList.add(BodyPart.APGloves.getVal());
                    bodyPartList.add(BodyPart.DUGloves.getVal());
                    bodyPartList.add(BodyPart.ZeroGloves.getVal());
                    bodyPartList.add(BodyPart.CGloves.getVal());
                    break;
                case Shield:
                case SecondaryWeapon:
                case Lapis:
                    bodyPartList.add(BodyPart.Shield.getVal());
                    break;
                case Katara:
                case Lazuli:
                case HoyoungFan:
                    bodyPartList.add(BodyPart.Weapon.getVal());
                    break;
                case Cape:
                    bodyPartList.add(BodyPart.Cape.getVal());
                    bodyPartList.add(BodyPart.APCape.getVal());
                    bodyPartList.add(BodyPart.DUCape.getVal());
                    bodyPartList.add(BodyPart.ZeroCape.getVal());
                    bodyPartList.add(BodyPart.CCape.getVal());
                    break;
                case Ring:
                    bodyPartList.add(BodyPart.Ring1.getVal());
                    bodyPartList.add(BodyPart.Ring2.getVal());
                    bodyPartList.add(BodyPart.Ring3.getVal());
                    bodyPartList.add(BodyPart.Ring4.getVal());
                    bodyPartList.add(BodyPart.CRing1.getVal());
                    bodyPartList.add(BodyPart.CRing2.getVal());
                    bodyPartList.add(BodyPart.CRing3.getVal());
                    bodyPartList.add(BodyPart.CRing4.getVal());
                    bodyPartList.add(BodyPart.ZeroRing1.getVal());
                    bodyPartList.add(BodyPart.ZeroRing2.getVal());
                    break;
                case Pendant:
                    bodyPartList.add(BodyPart.Pendant.getVal());
                    bodyPartList.add(BodyPart.ExtendedPendant.getVal());
                    break;
                case Belt:
                    bodyPartList.add(BodyPart.Belt.getVal());
                    break;
                case Medal:
                    bodyPartList.add(BodyPart.Medal.getVal());
                    break;
                case Shoulder:
                    bodyPartList.add(BodyPart.Shoulder.getVal());
                    break;
                case PocketItem:
                    bodyPartList.add(BodyPart.PocketItem.getVal());
                    break;
                case MonsterBook:
                    bodyPartList.add(BodyPart.MonsterBook.getVal());
                    break;
                case Badge:
                    bodyPartList.add(BodyPart.Badge.getVal());
                    break;
                case Emblem:
                    bodyPartList.add(BodyPart.Emblem.getVal());
                    break;
                case Totem:
                    bodyPartList.add(BodyPart.Totem1.getVal());
                    bodyPartList.add(BodyPart.Totem2.getVal());
                    bodyPartList.add(BodyPart.Totem3.getVal());
                    break;
                case MachineEngine:
                    bodyPartList.add(BodyPart.MachineEngine.getVal());
                    break;
                case MachineArm:
                    bodyPartList.add(BodyPart.MachineArm.getVal());
                    break;
                case MachineLeg:
                    bodyPartList.add(BodyPart.MachineLeg.getVal());
                    break;
                case MachineFrame:
                    bodyPartList.add(BodyPart.MachineFrame.getVal());
                    break;
                case MachineTransistor:
                    bodyPartList.add(BodyPart.MachineTransistor.getVal());
                    break;
                case Android:
                    bodyPartList.add(BodyPart.Android.getVal());
                    break;
                case MechanicalHeart:
                    bodyPartList.add(BodyPart.MechanicalHeart.getVal());
                    break;
                case Bit:
                    for (int id = BodyPart.BitsBase.getVal(); id <= BodyPart.BitsEnd.getVal(); id++) {
                        bodyPartList.add(id);
                    }
                    break;
                case CWeapon:
                    bodyPartList.add(BodyPart.CWeapon.getVal());
                    break;
                case PetWear:
                    bodyPartList.add(BodyPart.PetWear1.getVal());
                    bodyPartList.add(BodyPart.PetWear2.getVal());
                    bodyPartList.add(BodyPart.PetWear3.getVal());
                    break;
                case TamingMob:
                    bodyPartList.add(BodyPart.TamingMob.getVal());
                    break;
                case Saddle:
                    bodyPartList.add(BodyPart.Saddle.getVal());
                    break;
                case EvanHat:
                    bodyPartList.add(BodyPart.EvanHat.getVal());
                    break;
                case EvanPendant:
                    bodyPartList.add(BodyPart.EvanPendant.getVal());
                    break;
                case EvanWing:
                    bodyPartList.add(BodyPart.EvanWing.getVal());
                    break;
                case EvanShoes:
                    bodyPartList.add(BodyPart.EvanShoes.getVal());
                    break;
                case Symbol:
                    if (net.swordie.ms.connection.protocol.Protocol.active().version()
                            == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) {
                        // 282 client slots (79E970): arcane 1600..1605, authentic 1700..1705, grand 1750..1752
                        int base = itemID / 1000 == 1712 ? BodyPart.ArcBase.getVal()
                                : itemID / 1000 == 1713 ? BodyPart.AUSBase.getVal()
                                : itemID / 1000 == 1714 ? GRAND_AUTHENTIC_SYMBOL_BASE : 0;
                        int count = itemID / 1000 == 1714 ? 3 : 6;
                        for (int i = 0; base > 0 && i < count; i++) {
                            bodyPartList.add(base + i);
                        }
                        break;
                    }
                    if (itemID >= 1712000 && itemID <= 1712006) {
                        bodyPartList.add(BodyPart.ArcBase.getVal());
                        for (int i = 1; i <= 5; i++) {
                            bodyPartList.add(BodyPart.ArcBase.getVal() + i);
                        }
                        bodyPartList.add(BodyPart.ArcEnd.getVal());
                    } else if (itemID >= 1713000 && itemID <= 1713006) {
                        bodyPartList.add(BodyPart.AUSBase.getVal());
                        for (int i = 1; i <= 5; i++) {
                            bodyPartList.add(BodyPart.AUSBase.getVal() + i);
                        }
                        bodyPartList.add(BodyPart.AUSEnd.getVal());
                    }
                    break;
                default:
                    if (ItemConstants.isLongOrBigSword(itemID) || ItemConstants.isWeapon(itemID)) {
                        bodyPartList.add(BodyPart.Weapon.getVal());
                        if (ItemConstants.isFan(itemID)) {
                            bodyPartList.add(BodyPart.HakuFan.getVal());
                        } else {
                            bodyPartList.add(BodyPart.ZeroWeapon.getVal());
                        }
                    } else {
                        if (itemID != 0) {
                            System.out.println("Unknown type? id = " + itemID);
                        }
                    }
                    break;
            }
        } else {
            if (itemID != 0) {
                System.out.println("Unknown type? id = " + itemID);
            }
        }
        return bodyPartList;

    }

    public static int getItemPrefix(int itemID) {
        return itemID / 10000;
    }

    public static boolean isLongOrBigSword(int itemID) {
        return getItemPrefix(itemID) == EquipPrefix.Lapis.getVal() || getItemPrefix(itemID) == EquipPrefix.Lazuli.getVal();
    }

    public static boolean isLongSword(int itemID) {
        return getItemPrefix(itemID) == EquipPrefix.Lazuli.getVal();
    }

    public static boolean isBigSword(int itemID) {
        return getItemPrefix(itemID) == EquipPrefix.Lapis.getVal();
    }

    private static boolean isFan(int itemID) {
        return getItemPrefix(itemID) == EquipPrefix.Fan.getVal();
    }

    public static WeaponType getWeaponType(int itemID) {
        if (itemID / 1000000 != 1) {
            return WeaponType.None;
        }
        return WeaponType.getByVal(itemID / 1000 % 1000);
    }

    public static int getWeaponTypeVal(int itemID) {
        int mid3 = (itemID / 1000) % 1000;
        int wt;

        if (mid3 == WeaponType.Bladecaster.getVal()
                || mid3 == WeaponType.Whispershot.getVal()
                || mid3 == WeaponType.RenSword.getVal()
                || mid3 == WeaponType.Fist.getVal()
                || mid3 == WeaponType.Chakram.getVal()) {
            wt = mid3;
        } else {
            wt = getItemPrefix(itemID) % 100;
        }
        return wt;
    }

    public static boolean isThrowingItem(int itemID) {
        return isThrowingStar(itemID) || isBullet(itemID);// || isBowArrow(itemID);
    }

    public static boolean isThrowingStar(int itemID) {
        return getItemPrefix(itemID) == 207;
    }

    public static boolean isBullet(int itemID) {
        return getItemPrefix(itemID) == 233;
    }

    public static boolean isBowArrow(int itemID) {
        return itemID / 1000 == 2060;
    }

    public static boolean isFamiliar(int itemID) {
        return getItemPrefix(itemID) == 287;
    }

    public static boolean isNebuliteSocketCreator(int itemID) {
        return itemID == 2930000;
    }

    public static boolean isEnhancementScroll(int scrollID) {
        return scrollID / 100 == 20493;
    }

    public static boolean isHat(int itemID) {
        return getItemPrefix(itemID) == EquipPrefix.Hat.getVal();
    }

    public static boolean isCashWeapon(int itemID) {
        return getItemPrefix(itemID) == 170;
    }

    public static boolean isWeapon(int itemID) {
        return itemID >= 1210000 && itemID < 1600000 || itemID / 10000 == 170;
    }

    public static boolean isSubWeapon(int itemID) {
        return getItemPrefix(itemID) == 135;
    }

    /** 282 Astra secondary (172xxxx, islot "Si"): worn in 10 or 37 (client 79E970 / AF7660). */
    public static boolean isAstraSecondary(int itemID) {
        return getItemPrefix(itemID) == EquipPrefix.AstraSecondary.getVal();
    }

    public static boolean isSecondary(int itemID) {
        return getItemPrefix(itemID) == EquipPrefix.SecondaryWeapon.getVal();
    }

    public static boolean isShield(int itemID) {
        return getItemPrefix(itemID) == EquipPrefix.Shield.getVal();
    }

    public static boolean isAccessory(int itemID) {
        return (itemID >= 1010000 && itemID < 1040000) || (itemID >= 1122000 && itemID < 1153000) ||
                (itemID >= 1112000 && itemID < 1113000) || (itemID >= 1670000 && itemID < 1680000);
    }

    public static boolean isFaceAccessory(int itemID) {
        return getItemPrefix(itemID) == EquipPrefix.FaceAccessory.getVal();
    }

    public static boolean isEyeAccessory(int itemID) {
        return getItemPrefix(itemID) == EquipPrefix.EyeAccessory.getVal();
    }

    public static boolean isEarrings(int itemID) {
        return getItemPrefix(itemID) == EquipPrefix.Earrings.getVal();
    }

    public static boolean isTop(int itemID) {
        return getItemPrefix(itemID) == EquipPrefix.Top.getVal();
    }

    public static boolean isOverall(int itemID) {
        return getItemPrefix(itemID) == EquipPrefix.Overall.getVal();
    }

    public static boolean isBottom(int itemID) {
        return getItemPrefix(itemID) == EquipPrefix.Bottom.getVal();
    }

    public static boolean isShoe(int itemID) {
        return getItemPrefix(itemID) == EquipPrefix.Shoes.getVal();
    }

    public static boolean isGlove(int itemID) {
        return getItemPrefix(itemID) == EquipPrefix.Gloves.getVal();
    }

    public static boolean isCape(int itemID) {
        return getItemPrefix(itemID) == EquipPrefix.Cape.getVal();
    }

    public static boolean isArmor(int itemID) {
        return !isAccessory(itemID) && !isWeapon(itemID) && !isBadge(itemID) && !isMechanicalHeart(itemID);
    }

    public static boolean isRing(int itemID) {
        return getItemPrefix(itemID) == EquipPrefix.Ring.getVal();
    }

    public static boolean isPendant(int itemID) {
        return getItemPrefix(itemID) == EquipPrefix.Pendant.getVal();
    }

    public static boolean isBelt(int itemID) {
        return getItemPrefix(itemID) == EquipPrefix.Belt.getVal();
    }

    public static boolean isMedal(int itemID) {
        return getItemPrefix(itemID) == EquipPrefix.Medal.getVal();
    }

    public static boolean isShoulder(int itemID) {
        return getItemPrefix(itemID) == EquipPrefix.Shoulder.getVal();
    }

    public static boolean isPocketItem(int itemID) {
        return getItemPrefix(itemID) == EquipPrefix.PocketItem.getVal();
    }

    public static boolean isMonsterBook(int itemID) {
        return getItemPrefix(itemID) == EquipPrefix.MonsterBook.getVal();
    }

    public static boolean isBadge(int itemID) {
        return getItemPrefix(itemID) == EquipPrefix.Badge.getVal();
    }

    public static boolean isEmblem(int itemID) {
        return getItemPrefix(itemID) == EquipPrefix.Emblem.getVal();
    }

    public static boolean isTotem(int itemID) {
        return getItemPrefix(itemID) == EquipPrefix.Totem.getVal();
    }

    public static boolean isAndroid(int itemID) {
        return getItemPrefix(itemID) == EquipPrefix.Android.getVal();
    }

    public static boolean isMechanicalHeart(int itemID) {
        return getItemPrefix(itemID) == EquipPrefix.MechanicalHeart.getVal();
    }

    public static boolean isSymbol(int itemID) {
        return isArcaneSymbol(itemID) || isSacredSymbol(itemID);
    }

    /** 282 grand authentic symbol (1714xxx) equip slots 1750..1752 (client 79E970). */
    public static final int GRAND_AUTHENTIC_SYMBOL_BASE = 1750;
    public static final int GRAND_AUTHENTIC_SYMBOL_END = 1753;

    public static boolean isArcaneSymbol(int itemID) {
        return itemID / 1000 == 1712;
    }

    public static boolean isSacredSymbol(int itemID) {
        return itemID / 1000 == 1713 || itemID / 1000 == 1714;
    }

    public static boolean isNodeStone(int itemID) {
        return itemID == 2435719 || itemID == 2435902 || itemID == 2436078 || itemID == 2439869 || itemID == 2436324;
    }

    public static boolean isRebirthFlame(int itemId) {
        // TMS 282: every item the 282 data marks as a flame (info/exGrade: 2536xxx, 2610402/3 too).
        if (net.swordie.ms.client.character.items.Tms282EnhanceItems.active()
                && net.swordie.ms.client.character.items.Tms282EnhanceItems.isFlame(itemId)) {
            return true;
        }
        return itemId >= 2048700 && itemId < 2048800;
    }

    public static boolean isNebulite(int itemId) {
        return getItemPrefix(itemId) == 306;
    }

    public static boolean canEquipTypeHavePotential(int itemid) {
        return isRing(itemid) ||
                isPendant(itemid) ||
                isWeapon(itemid) ||
                isBelt(itemid) ||
                isHat(itemid) ||
                isFaceAccessory(itemid) ||
                isEyeAccessory(itemid) ||
                isOverall(itemid) ||
                isTop(itemid) ||
                isBottom(itemid) ||
                isShoe(itemid) ||
                isEarrings(itemid) ||
                isShoulder(itemid) ||
                isGlove(itemid) ||
                isEmblem(itemid) ||
                isBadge(itemid) ||
                isShield(itemid) ||
                isCape(itemid) ||
                isMechanicalHeart(itemid);
    }

    public static boolean canEquipHavePotential(Equip equip) {
        return !equip.isCash() &&
                canEquipTypeHavePotential(equip.getItemId()) &&
                !equip.isNoPotential() &&
                ((ItemData.getEquipById(equip.getItemId()) != null && ItemData.getEquipById(equip.getItemId()).getTuc() >= 1) || isTucIgnoreItem(equip.getItemId()));
    }

    public static boolean canEquipHaveFlame(Equip equip) {
        return !equip.isCash() && (isPendant(equip.getItemId()) ||
                (isWeapon(equip.getItemId()) && !isSecondary((equip.getItemId())) && !isShield((equip.getItemId()))) ||
                isBelt(equip.getItemId()) ||
                isHat(equip.getItemId()) ||
                isFaceAccessory(equip.getItemId()) ||
                isEyeAccessory(equip.getItemId()) ||
                isOverall(equip.getItemId()) ||
                isTop(equip.getItemId()) ||
                isBottom(equip.getItemId()) ||
                isShoe(equip.getItemId()) ||
                isEarrings(equip.getItemId()) ||
                Collections.singletonList(EXCEPTIONAL_EX_ALLOWED).contains(equip.getItemId()) ||
                isGlove(equip.getItemId()) ||
                isCape(equip.getItemId()) ||
                isPocketItem(equip.getItemId()));
    }

    public static boolean canEquipGoldHammer(Equip equip) {
        Equip defaultEquip = ItemData.getEquipById(equip.getItemId());
        return defaultEquip != null && !(Collections.singletonList(HORNTAIL_NECKLACE).contains(equip.getItemId()) || equip.getIuc() >= defaultEquip.getIUCMax() || defaultEquip.getTuc() <= 0); // No upgrade slots by default
    }

    public static boolean isGoldHammer(Item item) {
        return getItemPrefix(item.getItemId()) == 247;
    }

    /**
     * Gets potential tier for a line.
     * Accounts prime lines too.
     *
     * @param line             Potential line.
     * @param grade            Our current potential grade.
     * @param additionalPrimes How many extra prime lines this item has;
     */
    public static ItemGrade getLineTier(int line, ItemGrade grade, int additionalPrimes) {
        if (line == 0) {
            return grade;
        }
        if (additionalPrimes >= line) { //If cube has > 3 lines, still supports
            return grade;
        } else {
            return getOneTierLower(grade.getVal());
        }
    }

    /**
     * Gets prime line count based on cubeId
     * Used to manage the position of lines on the item throughout the cube method chain
     *
     * @param cubeId used to grab which rate will be used;
     */
    public static int getAdditionalPrimeCountForCube(int cubeId) {
        int addedPrimeCount = 0;
        switch (cubeId) {
            case VIOLET_CUBE: //Max of 5 additional since default has 1 prime
                addedPrimeCount = 2;
                break;
            case BONUS_BRIGHT_CUBE_1:
            case BONUS_BRIGHT_CUBE_2:
                if (Util.succeedProp(BOOSTED_PRIME_LINE_2_CHANCE, 1000)) { //Dependent Probability for 2 additional primes
                    addedPrimeCount++;
                    if (Util.succeedProp(BOOSTED_PRIME_LINE_3_CHANCE, 1000)) {
                        addedPrimeCount++;
                    }
                }
                break;
            default:
                if (Util.succeedProp(DEFAULT_PRIME_LINE_2_CHANCE, 1000)) {
                    addedPrimeCount++;
                    if (Util.succeedProp(DEFAULT_PRIME_LINE_3_CHANCE, 1000)) {
                        addedPrimeCount++;
                    }
                }
                break;
        }

        return addedPrimeCount;
    }

    /**
     * Determines whether a nebulite can be mounted on an equip.
     *
     * @param equip      Equip item.
     * @param nebuliteId The nebulite to mount on the equip.
     */
    public static boolean nebuliteFitsEquip(Equip equip, int nebuliteId) {
        Map<ScrollStat, Integer> vals = ItemData.getItemInfoByID(nebuliteId).getScrollStats();
        if (vals.isEmpty()) {
            return false;
        }
        int[] wrongNebutites = {3060060, 3060061, 3060070, 3060071, 3061060, 3061061, 3061070, 3061071, 3062060,
                3063061, 3063062, 3063063, 3063070, 3063071, 3063072, 3063073, 3063200, 3063201, 3063210, 3063211,
                3064060, 3064061, 3064062, 3064070, 3064071, 3064072, 3064200, 3064201, 3064210, 3064211};
        for (int wrongNebutite : wrongNebutites) {
            if (wrongNebutite == nebuliteId) {
                return false;
            }
        }
        ItemOptionType type = ItemOptionType.getByVal(vals.getOrDefault(ScrollStat.optionType, 0));
        int equipId = equip.getItemId();
        return switch (type) {
            case AnyEquip -> true;
            case Weapon -> isWeapon(equipId) || isShield(equipId);
            case AnyExceptWeapon -> !isWeapon(equipId) && !isShield(equipId);
            case Armor ->
                    isBelt(equipId) || isHat(equipId) || isOverall(equipId) || isTop(equipId) || isBottom(equipId) || isShoe(equipId) || isCape(equipId);
            case Accessory ->
                    isRing(equipId) || isPendant(equipId) || isFaceAccessory(equipId) || isEyeAccessory(equipId) || isEarrings(equipId) || isShoulder(equipId);
            case Hat -> isHat(equipId);
            case Top -> isTop(equipId) || isOverall(equipId);
            case Bottom -> isBottom(equipId) || isOverall(equipId);
            case Glove -> isGlove(equipId);
            case Shoes -> isShoe(equipId);
            default -> false;
        };
    }

    public static List<ItemOption> getOptionsByEquip(Equip equip, boolean bonus, int line, int cubeId, int additionalPrimes) {
        int id = equip.getItemId();
        Collection<ItemOption> data = ItemData.getFilteredItemOptions();
        ItemGrade grade = getLineTier(line, getGradeByVal(bonus ? equip.getBonusGrade() : equip.getBaseGrade()), additionalPrimes);
        // need a list, as we take a random item from it later on
        List<ItemOption> res = data.stream().filter(
                io -> io.getOptionType() == ItemOptionType.AnyEquip.getVal() &&
                        io.hasMatchingGrade(grade.getVal()) && io.isBonus() == bonus
        ).collect(Collectors.toList());

        if (isShield(id) || isSecondary(id)) {
            res.addAll(data.stream().filter(
                    io -> (io.getOptionType() == ItemOptionType.Armor.getVal() || io.getOptionType() == ItemOptionType.Weapon.getVal())
                            && io.hasMatchingGrade(grade.getVal())
                            && io.isBonus() == bonus
                            && io.getId() != 42060 //Armor's Crit Damage (Secondary Specific Filter)
            ).collect(Collectors.toList()));
        } else if (isWeapon(id)) {
            res.addAll(data.stream().filter(
                    io -> io.getOptionType() == ItemOptionType.Weapon.getVal()
                            && io.hasMatchingGrade(grade.getVal())
                            && io.isBonus() == bonus
            ).collect(Collectors.toList()));
        } else if (isEmblem(id)) {
            res.addAll(data.stream().filter(
                    io -> io.getOptionType() == ItemOptionType.Weapon.getVal()
                            && io.hasMatchingGrade(grade.getVal())
                            && io.isBonus() == bonus
                            && !io.getString().contains("Boss") //(Emblem Specific Filter)
            ).collect(Collectors.toList()));
        } else {
            res.addAll(data.stream().filter(
                    io -> io.getOptionType() == ItemOptionType.AnyExceptWeapon.getVal()
                            && io.hasMatchingGrade(grade.getVal())
                            && io.isBonus() == bonus
            ).collect(Collectors.toList()));

            if (isRing(id) || isPendant(id) || isFaceAccessory(id) || isEyeAccessory(id) || isEarrings(id)) {
                res.addAll(data.stream().filter(
                        io -> io.getOptionType() == ItemOptionType.Accessory.getVal()
                                && io.hasMatchingGrade(grade.getVal())
                                && io.isBonus() == bonus
                ).collect(Collectors.toList()));
            } else {
                if (isHat(id)) {
                    res.addAll(data.stream().filter(
                            io -> io.getOptionType() == ItemOptionType.Hat.getVal()
                                    && io.hasMatchingGrade(grade.getVal())
                                    && io.isBonus() == bonus
                    ).collect(Collectors.toList()));
                }
                if (isTop(id) || isOverall(id)) {
                    res.addAll(data.stream().filter(
                            io -> io.getOptionType() == ItemOptionType.Top.getVal()
                                    && io.hasMatchingGrade(grade.getVal())
                                    && io.isBonus() == bonus
                    ).collect(Collectors.toList()));
                }
                if (isBottom(id)) {
                    res.addAll(data.stream().filter(
                            io -> io.getOptionType() == ItemOptionType.Bottom.getVal()
                                    && io.hasMatchingGrade(grade.getVal())
                                    && io.isBonus() == bonus
                    ).collect(Collectors.toList()));
                }
                if (isShoe(id)) {
                    res.addAll(data.stream().filter(
                            io -> io.getOptionType() == ItemOptionType.Shoes.getVal()
                                    && io.hasMatchingGrade(grade.getVal())
                                    && io.isBonus() == bonus
                    ).collect(Collectors.toList()));
                }
                if (isGlove(id)) {
                    if (grade == HiddenUnique || grade == Unique || grade == HiddenLegendary || grade == Legendary || grade == UniqueBonusHidden || grade == LegendaryBonusHidden) {
                        res.addAll(data.stream().filter(io ->
                                (io.getOptionType() == ItemOptionType.Glove.getVal() || io.getOptionType() == ItemOptionType.Armor.getVal())
                                        && io.hasMatchingGrade(grade.getVal()) && io.isBonus() == bonus
                                        && io.getId() != 42060 //Bonus - Armor's 1% Crit Damage (Glove Specific Filter)
                        ).collect(Collectors.toList()));
                        if (!SOLID_CUBES.contains(cubeId) && !HARD_CUBES.contains(cubeId) && (cubeId != SYSTEM_DEFAULT_CUBE_INDICATOR)) {
                            res = res.stream().filter(
                                            io -> !io.getString().contains("Auto Steal")) //(Glove Specific Filter)
                                    .collect(Collectors.toList());
                        }
                    } else {
                        res.addAll(data.stream().filter(
                                io -> io.getOptionType() == ItemOptionType.Glove.getVal()
                                        && io.hasMatchingGrade(grade.getVal())
                                        && io.isBonus() == bonus
                        ).collect(Collectors.toList()));
                    }
                } else if (isArmor(id) || isShoulder(id) || isBelt(id)) {
                    res.addAll(data.stream().filter(
                            io -> io.getOptionType() == ItemOptionType.Armor.getVal()
                                    && io.hasMatchingGrade(grade.getVal())
                                    && io.isBonus() == bonus
                    ).collect(Collectors.toList()));
                }
            }
        }
        return res.stream().filter(io -> io.getReqLevel() <= equip.getrLevel() + equip.getiIncReq()).collect(Collectors.toList());
    }

    public static List<Integer> getWeightedOptionsByEquip(Equip equip, boolean bonus, int line, int cubeId, int additionalPrimes) {
        List<Integer> res = new ArrayList<>();
        List<ItemOption> data = getOptionsByEquip(equip, bonus, line, cubeId, additionalPrimes);
        for (ItemOption io : data) {
            for (int i = 0; i < io.getWeight(); i++) {
                res.add(io.getId());
            }
        }
        return res;
    }

    public static int getRandomOption(Equip equip, boolean bonus, int line, int cubeId, int additionalPrimes) {
        List<Integer> data = getWeightedOptionsByEquip(equip, bonus, line, cubeId, additionalPrimes);
        return data.get(Util.getRandom(data.size()));
    }

    /** Per 1000. Cubes with a row in data/resources/tms282/enhance/cube-grade.tsv (273 Grade.json, per 10000) use it;
     * the arrays below are this fork's own rates and stay the fallback. */
    public static int getTierUpChance(int id, ItemGrade grade) {
        int tableRate = net.swordie.ms.loaders.enhance.CubeRateTable.tierUp(
                net.swordie.ms.loaders.enhance.CubeRateTable.effectiveCube(id),
                net.swordie.ms.loaders.enhance.CubeRateTable.gradeIndex(grade));
        if (tableRate >= 0) {
            // rate.option.grade.up (GameSettings): 100 = unchanged; capped at 1000 (per-1000 scale)
            return net.swordie.ms.GameSettings.scaleTierUpChance((int) Math.round(tableRate / 10.0) * GLOBAL_TIER_UP_MULTIPLIER, 1000);
        }
        int[] rateArray;
        if (id == ItemConstants.MIRACLE_CUBE) {
            rateArray = ItemConstants.MIRACLE_CUBE_TIER_UP_RATES;
        } else if (id == ItemConstants.PREMIUM_MIRACLE_CUBE) {
            rateArray = ItemConstants.PREMIUM_MIRACLE_CUBE_TIER_UP_RATES;
        } else if (id == ItemConstants.SUPER_MIRACLE_CUBE) {
            rateArray = ItemConstants.SUPER_MIRACLE_CUBE_TIER_UP_RATES;
        } else if (id == ItemConstants.REVOLUTIONARY_MIRACLE_CUBE) {
            rateArray = ItemConstants.REVOLUTIONARY_MIRACLE_CUBE_TIER_UP_RATES;
        } else if (id == ItemConstants.ENLIGHTENING_MIRACLE_CUBE) {
            rateArray = ItemConstants.ENLIGHTENING_MIRACLE_CUBE_TIER_UP_RATES;
        } else if (id == ItemConstants.PLATINUM_MIRACLE_CUBE) {
            rateArray = ItemConstants.PLATINUM_MIRACLE_CUBE_TIER_UP_RATES;
        } else if (id == ItemConstants.RED_CUBE) {
            rateArray = ItemConstants.RED_CUBE_TIER_UP_RATES;
        } else if (id == ItemConstants.BLACK_CUBE) {
            rateArray = ItemConstants.BLACK_CUBE_TIER_UP_RATES;
        } else if (id == ItemConstants.VIOLET_CUBE) {
            rateArray = ItemConstants.VIOLET_CUBE_TIER_UP_RATES;
        } else if (id == ItemConstants.MEMORY_CUBE) {
            rateArray = ItemConstants.MEMORY_CUBE_TIER_UP_RATES;
        }

        else if (MYSTICAL_CUBES.contains(id)) {
            rateArray = ItemConstants.MYSTICAL_CUBES_TIER_UP_RATES;
        }
        else if (HARD_CUBES.contains(id)) {
            rateArray = ItemConstants.HARD_CUBES_TIER_UP_RATES;
        }
        else if (SOLID_CUBES.contains(id)) {
            rateArray = ItemConstants.SOLID_CUBE_TIER_UP_RATES;
        }
        else if (id == ItemConstants.GLOWING_CUBE) {
            rateArray = ItemConstants.GLOWING_CUBES_TIER_UP_RATES;
        }
        else if (BRIGHT_CUBES.contains(id) || id == ItemConstants.BRIGHT_CUBE) {
            rateArray = ItemConstants.BRIGHT_CUBES_TIER_UP_RATES;
        }
        else if (BONUS_MYSTICAL_CUBES.contains(id)) {
            rateArray = ItemConstants.BONUS_MYSTICAL_CUBE_TIER_UP_RATES;
        }
        else if (id == ItemConstants.BONUS_GLOWING_CUBE
                || id == ItemConstants.KARMA_BONUS_GLOWING_CUBE) {
            rateArray = ItemConstants.BONUS_GLOWING_CUBE_TIER_UP_RATES;
        }
        else if (BONUS_BRIGHT_CUBES.contains(id)
                || id == ItemConstants.BONUS_BRIGHT_CUBE_1
                || id == ItemConstants.BONUS_BRIGHT_CUBE_2) {
            rateArray = ItemConstants.BONUS_BRIGHT_CUBE_TIER_UP_RATES;
        }

        else if (id == ItemConstants.BONUS_POTENTIAL_CUBE || id == ItemConstants.SPECIAL_BONUS_POTENTIAL_CUBE) {
            rateArray = ItemConstants.BONUS_POT_CUBE_TIER_UP_RATE;
        } else {
            rateArray = ItemConstants.DEFAULT_CUBE_TIER_UP_RATES;
        }

        int chance = switch (grade) {
            case Rare, HiddenRare, RareBonusHidden -> rateArray[0] * GLOBAL_TIER_UP_MULTIPLIER;
            case Epic, HiddenEpic, EpicBonusHidden -> rateArray[1] * GLOBAL_TIER_UP_MULTIPLIER;
            case Unique, HiddenUnique, UniqueBonusHidden -> rateArray[2] * GLOBAL_TIER_UP_MULTIPLIER;
            default -> 0;
        };
        // rate.option.grade.up (GameSettings): 100 = unchanged; capped at 1000 (per-1000 scale)
        return net.swordie.ms.GameSettings.scaleTierUpChance(chance, 1000);
    }


    public static boolean isEquip(int id) {
        return id / 1000000 == 1;
    }

    public static boolean isClaw(int id) {
        return getItemPrefix(id) == 147;
    }

    public static boolean isBow(int id) {
        return getItemPrefix(id) == 145;
    }

    public static boolean isXBow(int id) {
        return getItemPrefix(id) == 146;
    }

    public static boolean isGun(int id) {
        return getItemPrefix(id) == 149;
    }

    public static boolean isXBowArrow(int id) {
        return id / 1000 == 2061;
    }

    public static InvType getInvTypeByItemID(int itemID) {
        if (isEquip(itemID)) {
            return EQUIP;
        } else {
            ItemInfo ii = ItemData.getItemInfoByID(itemID);
            if (ii == null) {
                return null;
            }
            return ii.getInvType();
        }
    }

    public static Set<Integer> getRechargeablesList() {
        Set<Integer> itemList = new HashSet<>();
        // all throwing stars
        for (int i = 2070000; i <= 2070016; i++) {
            if (i != 2070014) {
                itemList.add(i);
            }
        }
        itemList.add(2070018);
        itemList.add(2070023);
        itemList.add(2070024);
        itemList.add(2070026);
        // all bullets
        for (int i = 2330000; i <= 2330006; i++) {
            itemList.add(i);
        }
        itemList.add(2330008);
        itemList.add(2330016);
        itemList.add(2331000);
        itemList.add(2332000);
        return itemList;
    }

    public static boolean isRechargable(int itemId) {
        return isThrowingStar(itemId) || isBullet(itemId);
    }

    public static int getDamageSkinIDByItemID(int itemID) {
        int result = -1;
        switch (itemID) {
            case 2632268:
                result = 1426;
                break;
            case 2431965:
                result = 0;
                break;
            case 2438159:
                result = 0;
                break;
            case 2431966:
                result = 1;
                break;
            case 2432084:
                result = 1;
                break;
            case 2438160:
                result = 1;
                break;
            case 2431967:
                result = 2;
                break;
            case 2438161:
                result = 2;
                break;
            case 2432131:
                result = 3;
                break;
            case 2438162:
                result = 3;
                break;
            case 2432153:
                result = 4;
                break;
            case 2432638:
                result = 4;
                break;
            case 2436688:
                result = 4;
                break;
            case 2438163:
                result = 4;
                break;
            case 2432154:
                result = 5;
                break;
            case 2432637:
                result = 5;
                break;
            case 2435045:
                result = 5;
                break;
            case 2438164:
                result = 5;
                break;
            case 2432207:
                result = 6;
                break;
            case 2438165:
                result = 6;
                break;
            case 2432354:
                result = 7;
                break;
            case 2435023:
                result = 7;
                break;
            case 2438157:
                result = 7;
                break;
            case 2438166:
                result = 7;
                break;
            case 2432355:
                result = 8;
                break;
            case 2438167:
            case 2632882:
                result = 8;
                break;
            case 2432465:
                result = 9;
                break;
            case 2438168:
                result = 9;
                break;
            case 2432479:
                result = 10;
                break;
            case 2438169:
                result = 10;
                break;
            case 2432526:
                result = 11;
                break;
            case 2432639:
                result = 11;
                break;
            case 2438170:
                result = 11;
                break;
            case 2432532:
                result = 12;
                break;
            case 2435478:
                result = 12;
                break;
            case 2438171:
                result = 12;
                break;
            case 2432592:
            case 2633215:
                result = 13;
                break;
            case 2432640:
                result = 14;
                break;
            case 2438461:
                result = 14;
                break;
            case 2432710:
                result = 15;
                break;
            case 2433919:
                result = 15;
                break;
            case 2435170:
                result = 15;
                break;
            case 2432836:
                result = 16;
                break;
            case 2432973:
                result = 17;
                break;
            case 2435834:
                result = 17;
                break;
            case 2433063:
                result = 18;
                break;
            case 2438177:
                result = 18;
                break;
            case 2433456:
                result = 19;
                break;
            case 2433178:
                result = 20;
                break;
            case 2435169:
                result = 20;
                break;
            case 2631138:
                result = 20;
                break;
            case 2433631:
                result = 22;
                break;
            case 2433655:
                result = 22;
                break;
            case 2438180:
                result = 22;
                break;
            case 2433715:
                result = 23;
                break;
            case 2435024:
                result = 23;
                break;
            case 2433804:
            case 2633216:
                result = 24;
                break;
            case 2435168:
                result = 24;
                break;
            case 2433913:
                result = 26;
                break;
            case 2435025:
                result = 26;
                break;
            case 2433980:
                result = 27;
                break;
            case 2435026:
                result = 27;
                break;
            case 2437527:
                result = 27;
                break;
            case 2433981:
                result = 28;
                break;
            case 2438421:
                result = 28;
                break;
            case 2433990:
                result = 29;
                break;
            case 2434248:
                result = 34;
                break;
            case 2435027:
                result = 34;
                break;
            case 2434273:
                result = 35;
                break;
            case 2435028:
                result = 35;
                break;
            case 2434274:
                result = 36;
                break;
            case 2435029:
                result = 36;
                break;
            case 2435490:
                result = 36;
                break;
            case 2434289:
                result = 37;
                break;
            case 2434390:
                result = 38;
                break;
            case 2436099:
                result = 38;
                break;
            case 2434391:
                result = 39;
                break;
            case 2436034:
                result = 39;
                break;
            case 2434528:
                result = 41;
                break;
            case 2434529:
                result = 42;
                break;
            case 2434530:
                result = 43;
                break;
            case 2434546:
                result = 44;
                break;
            case 2436531:
                result = 44;
                break;
            case 2434574:
                result = 45;
                break;
            case 2434575:
                result = 46;
                break;
            case 2435171:
                result = 46;
                break;
            case 2434542:
                result = 47;
                break;
            case 2434654:
                result = 48;
                break;
            case 2435325:
                result = 48;
                break;
            case 2434655:
                result = 49;
                break;
            case 2435326:
                result = 49;
                break;
            case 2434661:
                result = 50;
                break;
            case 2437878:
                result = 50;
                break;
            case 2631137:
                result = 50;
                break;
            case 2434710:
                result = 51;
                break;
            case 2434734:
                result = 52;
                break;
            case 2434824:
                result = 53;
                break;
            case 2434950:
                result = 74;
                break;
            case 2435837:
                result = 74;
                break;
            case 2434951:
                result = 75;
                break;
            case 2435030:
                result = 76;
                break;
            case 2435043:
                result = 77;
                break;
            case 2436041:
                result = 77;
                break;
            case 2435044:
                result = 78;
                break;
            case 2436042:
                result = 78;
                break;
            case 2435046:
                result = 79;
                break;
            case 2436097:
                result = 79;
                break;
            case 2435047:
                result = 80;
                break;
            case 2435140:
                result = 81;
                break;
            case 2435836:
                result = 81;
                break;
            case 2435141:
                result = 82;
                break;
            case 2437244:
                result = 82;
                break;
            case 2436096:
                result = 83;
                break;
            case 2435162:
                result = 84;
                break;
            case 2435724:
                result = 84;
                break;
            case 2435157:
                result = 85;
                break;
            case 2436098:
                result = 85;
                break;
            case 2435158:
                result = 86;
                break;
            case 2435835:
                result = 86;
                break;
            case 2435159:
                result = 87;
                break;
            case 2436687:
                result = 160;
                break;
            case 2435160:
                result = 88;
                break;
            case 2436044:
                result = 88;
                break;
            case 2435161:
                result = 89;
                break;
            case 2436043:
                result = 89;
                break;
            case 2435182:
                result = 90;
                break;
            case 2435725:
                result = 90;
                break;
            case 2435166:
                result = 91;
                break;
            case 2435850:
                result = 91;
                break;
            case 2435184:
                result = 92;
                break;
            case 2435222:
                result = 93;
                break;
            case 2436530:
                result = 93;
                break;
            case 2435293:
                result = 94;
                break;
            case 2435313:
                result = 95;
                break;
            case 2435331:
                result = 96;
                break;
            case 2435473:
                result = 96;
                break;
            case 2435332:
                result = 97;
                break;
            case 2435333:
                result = 98;
                break;
            case 2435849:
                result = 98;
                break;
            case 2435334:
                result = 99;
                break;
            case 2435474:
                result = 99;
                break;
            case 2435316:
                result = 100;
                break;
            case 2435408:
                result = 101;
                break;
            case 2435427:
                result = 102;
                break;
            case 2435428:
                result = 103;
                break;
            case 2435429:
                result = 104;
                break;
            case 2435456:
                result = 105;
                break;
            case 2435493:
                result = 106;
                break;
            case 2435424:
                result = 109;
                break;
            case 2435425:
                result = 110;
                break;
            case 2435431:
                result = 111;
                break;
            case 2435430:
                result = 112;
                break;
            case 2435432:
                result = 113;
                break;
            case 2435433:
                result = 114;
                break;
            case 2435516:
                result = 115;
                break;
            case 2435521:
                result = 116;
                break;
            case 2435522:
                result = 117;
                break;
            case 2435523:
                result = 118;
                break;
            case 2435524:
                result = 119;
                break;
            case 2436561:
                result = 119;
                break;
            case 2435538:
                result = 120;
                break;
            case 2435832:
                result = 121;
                break;
            case 2435833:
                result = 122;
                break;
            case 2435839:
                result = 123;
                break;
            case 2435840:
                result = 124;
                break;
            case 2435841:
                result = 125;
                break;
            case 2435972:
                result = 127;
                break;
            case 2436023:
                result = 128;
                break;
            case 2436024:
                result = 129;
                break;
            case 2436026:
                result = 130;
                break;
            case 2436027:
                result = 131;
                break;
            case 2436028:
                result = 132;
                break;
            case 2436029:
                result = 133;
                break;
            case 2436045:
                result = 134;
                break;
            case 2436085:
                result = 135;
                break;
            case 2436083:
                result = 136;
                break;
            case 2436084:
                result = 137;
                break;
            case 2436103:
                result = 138;
                break;
            case 2436131:
                result = 139;
                break;
            case 2436140:
                result = 140;
                break;
            case 2436206:
                result = 141;
                break;
            case 2436182:
                result = 142;
                break;
            case 2436212:
                result = 143;
                break;
            case 2437851:
                result = 143;
                break;
            case 2436215:
                result = 144;
                break;
            case 2436268:
                result = 145;
                break;
            case 2436258:
                result = 146;
                break;
            case 2436259:
                result = 147;
                break;
            case 2436400:
                result = 148;
                break;
            case 2436437:
                result = 149;
                break;
            case 2436511:
                result = 150;
                break;
            case 2436528:
                result = 152;
                break;
            case 2436529:
                result = 153;
                break;
            case 2436522:
                result = 154;
                break;
            case 2436553:
                result = 155;
                break;
            case 2437697:
                result = 155;
                break;
            case 2436560:
                result = 156;
                break;
            case 2436578:
                result = 157;
                break;
            case 2437767:
                result = 157;
                break;
            case 2436611:
                result = 158;
                break;
            case 2436612:
                result = 159;
                break;
            case 2436596:
                result = 160;
                break;
            case 2436679:
                result = 161;
                break;
            case 2436680:
                result = 162;
                break;
            case 2436681:
                result = 163;
                break;
            case 2436682:
                result = 164;
                break;
            case 2436683:
                result = 165;
                break;
            case 2436684:
                result = 166;
                break;
            case 2436785:
                result = 167;
                break;
            case 2436810:
                result = 168;
                break;
            case 2436951:
                result = 169;
                break;
            case 2436952:
                result = 170;
                break;
            case 2436953:
                result = 171;
                break;
            case 2437022:
                result = 172;
                break;
            case 2437023:
                result = 173;
                break;
            case 2437024:
                result = 174;
                break;
            case 2437009:
                result = 175;
                break;
            case 2437164:
                result = 176;
                break;
            case 2438925:
                result = 176;
                break;
            case 2437238:
                result = 177;
                break;
            case 2437243:
                result = 178;
                break;
            case 2437239:
                result = 179;
                break;
            case 2437495:
                result = 180;
                break;
            case 2437496:
                result = 181;
                break;
            case 2437498:
                result = 182;
                break;
            case 2437515:
                result = 183;
                break;
            case 2437482:
                result = 184;
                break;
            case 2438926:
                result = 184;
                break;
            case 2437691:
                result = 185;
                break;
            case 2437716:
                result = 186;
                break;
            case 2437735:
                result = 187;
                break;
            case 2437736:
                result = 188;
                break;
            case 2437854:
                result = 189;
                break;
            case 2437877:
                result = 190;
                break;
            case 2438143:
                result = 191;
                break;
            case 2438146:
                result = 191;
                break;
            case 2438144:
                result = 192;
                break;
            case 2438147:
                result = 192;
                break;
            case 2438352:
                result = 193;
                break;
            case 2438353:
                result = 193;
                break;
            case 2438924:
                result = 193;
                break;
            case 2438378:
                result = 194;
                break;
            case 2438379:
                result = 195;
                break;
            case 2438413:
                result = 196;
                break;
            case 2438414:
                result = 196;
                break;
            case 2438415:
                result = 197;
                break;
            case 2438416:
                result = 197;
                break;
            case 2438417:
                result = 198;
                break;
            case 2438418:
                result = 198;
                break;
            case 2438419:
                result = 199;
                break;
            case 2438420:
                result = 199;
                break;
            case 2438460:
                result = 200;
                break;
            case 2438485:
                result = 200;
                break;
            case 2438491:
                result = 201;
                break;
            case 2438492:
                result = 201;
                break;
            case 2438529:
                result = 202;
                break;
            case 2438530:
                result = 202;
                break;
            case 2438637:
                result = 203;
                break;
            case 2438672:
                result = 204;
                break;
            case 2438676:
                result = 205;
                break;
            case 2438713:
                result = 205;
                break;
            case 2438880:
                result = 206;
                break;
            case 2438881:
                result = 206;
                break;
            case 2438884:
                result = 207;
                break;
            case 2438885:
                result = 207;
                break;
            case 2438871:
                result = 208;
                break;
            case 2438872:
                result = 208;
                break;
            case 2439256:
                result = 209;
                break;
            case 2439298:
                result = 209;
                break;
            case 2439264:
                result = 210;
                break;
            case 2439336:
                result = 210;
                break;
            case 2439265:
                result = 211;
                break;
            case 2439337:
                result = 211;
                break;
            case 2439277:
                result = 212;
                break;
            case 2439338:
                result = 212;
                break;
            case 2439381:
                result = 213;
                break;
            case 2439392:
                result = 214;
                break;
            case 2439393:
                result = 214;
                break;
            case 2439394:
                result = 215;
                break;
            case 2439395:
                result = 215;
                break;
            case 2439407:
                result = 216;
                break;
            case 2439408:
                result = 216;
                break;
            case 2439572:
                result = 217;
                break;
            case 2439616:
                result = 218;
                break;
            case 2439617:
                result = 218;
                break;
            case 2439652:
                result = 219;
                break;
            case 2439665:
                result = 219;
                break;
            case 2439683:
                result = 221;
                break;
            case 2439684:
                result = 221;
                break;
            case 2439685:
                result = 222;
                break;
            case 2439686:
                result = 222;
                break;
            case 2439768:
                result = 223;
                break;
            case 2439769:
                result = 223;
                break;
            case 2439925:
                result = 224;
                break;
            case 2439926:
                result = 224;
                break;
            case 2439927:
                result = 225;
                break;
            case 2439928:
                result = 225;
                break;
            case 2630104:
                result = 225;
                break;
            case 2630132:
                result = 227;
                break;
            case 2630137:
                result = 227;
                break;
            case 2630010:
                result = 228;
                break;
            case 2603222:
                result = 228;
                break;
            case 2630178:
                result = 229;
                break;
            case 2630179:
                result = 229;
                break;
            case 2630213:
                result = 230;
                break;
            case 2630214:
                result = 230;
                break;
            case 2630380:
                result = 231;
                break;
            case 2630381:
                result = 231;
                break;
            case 2630235:
                result = 232;
                break;
            case 2630236:
                result = 232;
                break;
            case 2630224:
                result = 233;
                break;
            case 2630225:
                result = 233;
                break;
            case 2630766:
                result = 233;
                break;
            case 2630262:
                result = 234;
                break;
            case 2630263:
                result = 234;
                break;
            case 2630264:
                result = 235;
                break;
            case 2630265:
                result = 235;
                break;
            case 2630266:
                result = 236;
                break;
            case 2630267:
                result = 236;
                break;
            case 2630384:
                result = 237;
                break;
            case 2630385:
                result = 237;
                break;
            case 2630400:
                result = 238;
                break;
            case 2630421:
                result = 239;
                break;
            case 2630434:
                result = 239;
                break;
            case 2630435:
                result = 240;
                break;
            case 2630436:
                result = 240;
                break;
            case 2630477:
                result = 241;
                break;
            case 2630478:
                result = 241;
                break;
            case 2630479:
                result = 242;
                break;
            case 2630480:
                result = 242;
                break;
            case 2630481:
                result = 243;
                break;
            case 2630482:
                result = 243;
                break;
            case 2630483:
                result = 244;
                break;
            case 2630484:
                result = 244;
                break;
            case 2630485:
                result = 245;
                break;
            case 2630486:
                result = 245;
                break;
            case 2630552:
                result = 246;
                break;
            case 2630553:
                result = 246;
                break;
            case 2630554:
                result = 247;
                break;
            case 2630555:
                result = 247;
                break;
            case 2630556:
                result = 248;
                break;
            case 2630557:
                result = 248;
                break;
            case 2630558:
                result = 249;
                break;
            case 2630559:
                result = 249;
                break;
            case 2630560:
                result = 250;
                break;
            case 2630561:
                result = 250;
                break;
            case 2630652:
                result = 251;
                break;
            case 2630653:
                result = 251;
                break;
            case 2630743:
                result = 252;
                break;
            case 2630744:
                result = 252;
                break;
            case 2630745:
                result = 253;
                break;
            case 2630746:
                result = 253;
                break;
            case 2630747:
                result = 254;
                break;
            case 2630748:
                result = 254;
                break;
            case 2630749:
                result = 255;
                break;
            case 2630750:
                result = 255;
                break;
            case 2630751:
                result = 256;
                break;
            case 2630752:
                result = 256;
                break;
            case 2630753:
                result = 257;
                break;
            case 2630754:
                result = 257;
                break;
            case 2630780:
                result = 258;
                break;
            case 2630804:
                result = 258;
                break;
            case 2630969:
                result = 259;
                break;
            case 2630970:
                result = 259;
                break;
            case 2631090:
                result = 260;
                break;
            case 2631094:
                result = 260;
                break;
            case 2631091:
                result = 261;
                break;
            case 2631095:
                result = 261;
                break;
            case 2631097:
                result = 262;
                break;
            case 2631098:
                result = 262;
                break;
            case 2631134:
                result = 263;
                break;
            case 2631135:
                result = 263;
                break;
            case 2631189:
                result = 264;
                break;
            case 2631183:
                result = 265;
                break;
            case 2631184:
                result = 265;
                break;
            case 2631401:
                result = 266;
                break;
            case 2631402:
                result = 266;
                break;
            case 2631451:
                result = 267;
                break;
            case 2631452:
                result = 267;
                break;
            case 2631471:
                result = 268;
                break;
            case 2631472:
                result = 268;
                break;
            case 2631491:
                result = 269;
                break;
            case 2631492:
                result = 269;
                break;
            case 2631610:
                result = 270;
                break;
            case 2631611:
                result = 270;
                break;
            case 3130001:
                result = 271;
                break;
            case 2631797:
                result = 273;
                break;
            case 2631798:
                result = 273;
                break;
            case 2631814:
                result = 274;
                break;
            case 2631815:
                result = 274;
                break;
            case 2631884:
                result = 275;
                break;
            case 2631885:
                result = 275;
                break;
            case 2631892:
                result = 276;
                break;
            case 2631893:
                result = 276;
                break;
            case 2632123:
                result = 277;
                break;
            case 2632124:
                result = 277;
                break;
            case 2632281:
                result = 278;
                break;
            case 2632282:
                result = 279;
                break;
            case 2632287:
                result = 279;
                break;
            case 2632288:
                result = 279;
                break;
            case 2632348:
                result = 280;
                break;
            case 2632350:
                result = 280;
                break;
            case 2632429:
            case 2632430:
                result = 281;
                break;
            case 2632452:
            case 2632544:
                result = 282;
                break;
            case 2632497:
            case 2632498:
                result = 283;
                break;
            case 2632711:
            case 2632712:
                result = 284;
                break;
            case 2632815:
            case 2632816:
                result = 286;
                break;
            case 2632888:
                result = 287;
                break;
            case 2632975:
            case 2632976:
                result = 288;
                break;
            case 2633045:
            case 2633046:
                result = 289;
                break;
            case 2633047:
            case 2633048:
                result = 290;
                break;
            case 2633073:
            case 2633074:
                result = 291;
                break;
            case 2633218:
            case 2633219:
                result = 292;
                break;
            case 2633220:
            case 2633221:
                result = 293;
                break;
            case 2439396:
                result = 1010;
                break;
            case 2439397:
                result = 1010;
                break;
            case 2439398:
                result = 1017;
                break;
            case 2439399:
                result = 1017;
                break;
            case 2630268:
                result = 1030;
                break;
            case 2630269:
                result = 1030;
                break;
            case 2438148:
                result = 1287;
                break;
            case 2438149:
                result = 1287;
                break;
            case 2439681:
                result = 1290;
                break;
            case 2439682:
                result = 1290;
                break;
            case 2438150:
                result = 1302;
                break;
            case 2438151:
                result = 1302;
                break;
            case 2439400:
                result = 1322;
                break;
            case 2439401:
                result = 1322;
                break;
            case 2630516:
                result = 1343;
                break;
            case 2630517:
                result = 1343;
                break;
            case 2634176:
            case 2634177:
                result = 304;
                break;
            case 2634250:
            case 2634251:
                result = 305;
                break;
            case 2634267:
            case 2634268:
                result = 307;
                break;
            case 2634276:
            case 2634277:
                result = 308;
                break;
            case 2634279:
            case 2634280:
                result = 309;
                break;
            default:
                result = EffectData.getDamageSkinIDById(itemID);
        }
        return result;
    }

    public static boolean isMasteryBook(int itemId) {
        return getItemPrefix(itemId) == 229;
    }

    public static boolean isPet(int itemId) {
        return getItemPrefix(itemId) == 500;
    }

    public static boolean isSoulEnchanter(int itemID) {
        return itemID / 1000 == 2590;
    }

    public static boolean isSoul(int itemID) {
        return itemID / 1000 == 2591;
    }

    public static int getSoulOptionFromSoul(int itemId) {
        String name = StringData.getItemStringById(itemId);
        boolean soul_1 = name.contains("Spirit of Rock") || name.contains("Prison Guard Ani") || name.contains("Xerxes") || name.contains("Black Slime");
        boolean soul_2 = name.contains("Dragon Rider") || name.contains("Rex");
        boolean soul_3 = name.contains("Mu Gong") || name.contains("Pianus");
        boolean soul_4 = name.contains("Balrog") || name.contains("Tutu") || name.contains("Nene");
        boolean soul_5 = name.contains("Zakum") || name.contains("Gold Dragon") || name.contains("Red Tiger");
        boolean soul_6 = name.contains("Von Leon") || name.contains("Hilla");
        boolean soul_7 = name.contains("Arkarium") || name.contains("Mad Mage") || name.contains("Rampant Cyborg") || name.contains("Vicious Hunter") || name.contains("Bad Brawler");
        boolean soul_8 = name.contains("Pink Bean") || name.contains("Black Knight") || name.contains("Pierre") || name.contains("Von Bon");
        boolean soul_9 = name.contains("Cygnus") || name.contains("Magnus") || name.contains("Murgoth") || name.contains("Crimson Queen") || name.contains("Vellum") || name.contains("Lotus") || name.contains("Damien") || name.contains("Lucid");
        if (name.startsWith("Beefy") || name.startsWith("Swift") || name.startsWith("Clever") || name.startsWith("Fortuitous")) { // +incSTR/+incDEX/+incINT/+incLUK
            if (soul_1) {
                return name.startsWith("Beefy") ? 101 : (name.startsWith("Swift") ? 102 : (name.startsWith("Clever") ? 103 : 104)); // +3
            } else if (soul_2) {
                return name.startsWith("Beefy") ? 105 : (name.startsWith("Swift") ? 106 : (name.startsWith("Clever") ? 107 : 108)); // +5
            } else if (soul_3) {
                return name.startsWith("Beefy") ? 109 : (name.startsWith("Swift") ? 110 : (name.startsWith("Clever") ? 111 : 112)); // +7
            } else if (soul_4) {
                return name.startsWith("Beefy") ? 113 : (name.startsWith("Swift") ? 114 : (name.startsWith("Clever") ? 115 : 116)); // +10
            } else if (soul_5) {
                return name.startsWith("Beefy") ? 163 : (name.startsWith("Swift") ? 164 : (name.startsWith("Clever") ? 165 : 166)); // +12
            } else if (soul_6) {
                return name.startsWith("Beefy") ? 117 : (name.startsWith("Swift") ? 118 : (name.startsWith("Clever") ? 119 : 120)); // +15
            } else if (soul_7) {
                return name.startsWith("Beefy") ? 185 : (name.startsWith("Swift") ? 186 : (name.startsWith("Clever") ? 187 : 188)); // +18
            } else if (soul_8) {
                return name.startsWith("Beefy") ? 121 : (name.startsWith("Swift") ? 122 : (name.startsWith("Clever") ? 123 : 124)); // +20
            } else if (soul_9) {
                return name.startsWith("Beefy") ? 167 : (name.startsWith("Swift") ? 168 : (name.startsWith("Clever") ? 169 : 170)); // +24
            }
        } else if (name.startsWith("Hearty") || name.startsWith("Ample")) { // +incMHP/+incMMP
            if (soul_1) {
                return name.startsWith("Hearty") ? 131 : 132; // +150
            } else if (soul_2) {
                return name.startsWith("Hearty") ? 133 : 134; // +200
            } else if (soul_3) {
                return name.startsWith("Hearty") ? 135 : 136; // +300
            } else if (soul_4) {
                return name.startsWith("Hearty") ? 137 : 0; // +400
            } else if (soul_5) {
                return name.startsWith("Hearty") ? 175 : 0; // +500
            } else if (soul_6) {
                return name.startsWith("Hearty") ? 138 : 0; // +600
            } else if (soul_7) {
                return name.startsWith("Hearty") ? 189 : 0; // +700
            } else if (soul_8) {
                return name.startsWith("Hearty") ? 139 : 0; // +800
            } else if (soul_9) {
                return name.startsWith("Hearty") ? 177 : 0; // +960
            }
        } else if (name.startsWith("Flashy")) { // All Stats: +#incSTR
            if (soul_1) {
                return 201; // +2
            } else if (soul_2) {
                return 202; // +3
            } else if (soul_3) {
                return 203; // +5
            } else if (soul_4) {
                return 204; // +7
            } else if (soul_5) {
                return 210; // +8
            } else if (soul_6) {
                return 205; // +10
            } else if (soul_8) {
                return 40081; // +12
            } else if (soul_9) {
                return 70057; // +15
            }
        } else if (name.startsWith("Potent") || name.startsWith("Radiant")) { // +#incPAD/+#incMAD
            if (soul_5) {
                return name.startsWith("Potent") ? 2011 : 2012; // +3
            } else if (soul_6 || soul_7) {
                return name.startsWith("Potent") ? 153 : 154; // +4
            } else if (soul_8) {
                return name.startsWith("Potent") ? 155 : 156; // +5
            } else if (soul_9) {
                return name.startsWith("Potent") ? 171 : 172; // +6
            }
        } else if (name.startsWith("Sharp")) {
            return 42292;
        } else if (name.startsWith("Destructive")) {
            return 60009;
        }
        return Util.getRandomFromCollection(soulPotList);
    }

    public static int getSoulSkillFromSoulID(int soulID) {
        switch (soulID) {
            case 1: //Balrog's Soul Marble
                return 80001267;
            case 2: //Horntail's Soul Marble
                return 0;
            case 3: //Rex's Soul Marble
                return 80001799;
            case 4: //Pink Bean's Soul Marble
                return 80001269;
            case 5: //Dragon Rider's Soul Marble
                return 80001801;
            case 6: //Von Leon Soul Marble
                return 80001268;
            case 7: //Zakum's Soul Marble
                return 80001274;
            case 8: //Spirit of Rock's Soul Marble
                return 80001804;
            case 9: //Mu Gong Panda's Soul Marble
                return 80001805;
            case 10: //Ani's Soul Marble
                return 80001800;
            case 11: //Beefy Spirit of Rock Soul
            case 12: //Swift Spirit of Rock Soul
            case 13: //Clever Spirit of Rock Soul
            case 14: //Fortuitous Spirit of Rock Soul
            case 15: //Hearty Spirit of Rock Soul
            case 16: //Ample Spirit of Rock Soul
            case 17: //Flashy Spirit of Rock Soul
                return 80001804;
            case 18: //Beefy Prison Guard Ani Soul
            case 19: //Swift Prison Guard Ani Soul
            case 20: //Clever Prison Guard Ani Soul
            case 21: //Fortuitous  Prison Guard Ani Soul
            case 22: //Hearty Prison Guard Ani Soul
            case 23: //Ample Prison Guard Ani Soul
            case 24: //Flashy Prison Guard Ani Soul
                return 80001800;
            case 25: //Beefy Dragon Rider Soul
            case 26: //Swift Dragon Rider Soul
            case 27: //Clever Dragon Rider Soul
            case 28: //Fortuitous Dragon Rider Soul
            case 29: //Hearty Dragon Rider Soul
            case 30: //Ample Dragon Rider Soul
            case 31: //Flashy Dragon Rider Soul
                return 80001801;
            case 32: //Beefy Rex Soul
            case 33: //Swift Rex Soul
            case 34: //Clever Rex Soul
            case 35: //Fortuitous Rex Soul
            case 36: //Hearty Rex Soul
            case 37: //Ample Rex Soul
            case 38: //Flashy Rex Soul
                return 80001799;
            case 39: //Beefy Mu Gong Soul
            case 40: //Swift Mu Gong Soul
            case 41: //Clever Mu Gong Soul
            case 42: //Fortuitous Mu Gong Soul
            case 43: //Hearty Mu Gong Soul
            case 44: //Ample Mu Gong Soul
            case 45: //Flashy Mu Gong Soul
                return 80001805;
            case 46: //Beefy Balrog Soul
            case 47: //Swift Balrog Soul
            case 48: //Clever Balrog Soul
            case 49: //Fortuitous  Balrog Soul
            case 50: //Flashy Balrog Soul
            case 51: //Powerful Balrog Soul
            case 52: //Radiant Balrog Soul
            case 53: //Hearty Balrog Soul
            case 54: //Sharp Balrog Soul
            case 55: //Destructive Balrog Soul
                return 80001267;
            case 56: //Beefy Pink Bean Soul
            case 57: //Swift Pink Bean Soul
            case 58: //Clever Pink Bean Soul
            case 59: //Fortuitous Pink Bean Soul
            case 60: //Flashy Pink Bean Soul
            case 61: //Potent Pink Bean Soul
            case 62: //Radiant Pink Bean Soul
            case 63: //Hearty Pink Bean Soul
            case 64: //Sharp Pink Bean Soul
            case 65: //Destructive Pink Bean Soul
                return 80001269;
            case 66: //Beefy Von Leon Soul
            case 67: //Swift Von Leon Soul
            case 68: //Clever Von Leon Soul
            case 69: //Fortuitous  Von Leon Soul
            case 70: //Flashy Von Leon Soul
            case 71: //Potent Von Leon Soul
            case 72: //Radiant Von Leon Soul
            case 73: //Hearty Von Leon Soul
            case 74: //Sharp Von Leon Soul
            case 75: //Destructive Von Leon Soul
                return 80001268;
            case 76: //Beefy Cygnus Soul
            case 77: //Swift Cygnus Soul
            case 78: //Clever Cygnus Soul
            case 79: //Fortuitous Cygnus Soul
            case 80: //Flashy Cygnus Soul
            case 81: //Potent Cygnus Soul
            case 82: //Radiant Cygnus Soul
            case 83: //Hearty Cygnus Soul
                return EMPRESS_OFF_LAMES;
            case 86: //Magnificent Balrog Soul
                return 80001797;
            case 87: //Magnificent Von Leon Soul
                return 80001802;
            case 88: //Magnificent Pink Bean Soul
                return MORE_THAN_A_CUTIE;
            case 89: //Magnificent Cygnus Soul
                return EMPRESS_OF_FURY;
            case 90: //Beefy Spirit of Rock Soul
            case 91: //Swift Spirit of Rock Soul
            case 92: //Clever Spirit of Rock Soul
            case 93: //Fortuitous Spirit of Rock Soul
            case 94: //Hearty Spirit of Rock Soul
            case 95: //Ample Spirit of Rock Soul
            case 96: //Flashy Spirit of Rock Soul
                return 80001804;
            case 97: //Beefy Prison Guard Ani Soul
            case 98: //Swift Prison Guard Ani Soul
            case 99: //Clever Prison Guard Ani Soul
            case 100: //Fortuitous  Prison Guard Ani Soul
            case 101: //Hearty Prison Guard Ani Soul
            case 102: //Ample Prison Guard Ani Soul
            case 103: //Flashy Prison Guard Ani Soul
                return 80001800;
            case 104: //Beefy Dragon Rider Soul
            case 105: //Swift Dragon Rider Soul
            case 106: //Clever Dragon Rider Soul
            case 107: //Fortuitous  Dragon Rider Soul
            case 108: //Hearty Dragon Rider Soul
            case 109: //Ample Dragon Rider Soul
            case 110: //Flashy Dragon Rider Soul
                return 80001801;
            case 111: //Beefy Rex Soul
            case 112: //Swift Rex Soul
            case 113: //Clever Rex Soul
            case 114: //Fortuitous  Rex Soul
            case 115: //Hearty Rex Soul
            case 116: //Ample Rex Soul
            case 117: //Flashy Rex Soul
                return 80001800;
            case 118: //Beefy Mu Gong Soul
            case 119: //Swift Mu Gong Soul
            case 120: //Clever Mu Gong Soul
            case 121: //Fortuitous Mu Gong Soul
            case 122: //Hearty Mu Gong Soul
            case 123: //Ample Mu Gong Soul
            case 124: //Flashy Mu Gong Soul
                return 80001805;
            case 125: //Beefy Balrog Soul
            case 126: //Swift Balrog Soul
            case 127: //Clever Balrog Soul
            case 128: //Fortuitous Balrog Soul
            case 129: //Flashy Balrog Soul
            case 130: //Powerful Balrog Soul
            case 131: //Radiant Balrog Soul
            case 132: //Hearty Balrog Soul
                return 80001267;
            case 133: //Beefy Von Leon Soul
            case 134: //Swift Von Leon Soul
            case 135: //Clever Von Leon Soul
            case 136: //Fortuitous Von Leon Soul
            case 137: //Flashy Von Leon Soul
            case 138: //Potent Von Leon Soul
            case 139: //Radiant Von Leon Soul
            case 140: //Hearty Von Leon Soul
                return 80001268;
            case 141: //Beefy Pink Bean Soul
            case 142: //Swift Pink Bean Soul
            case 143: //Clever Pink Bean Soul
            case 144: //Fortuitous Pink Bean Soul
            case 145: //Flashy Pink Bean Soul
            case 146: //Potent Pink Bean Soul
            case 147: //Radiant Pink Bean Soul
            case 148: //Hearty Pink Bean Soul
                return 80001269;
            case 149: //Beefy Xerxes Soul
            case 150: //Swift Xerxes Soul
            case 151: //Clever Xerxes Soul
            case 152: //Fortuitous Xerxes Soul
            case 153: //Hearty Xerxes Soul
            case 154: //Ample Xerxes Soul
            case 155: //Flashy Xerxes Soul
                return 80001273;
            case 156: //Beefy Zakum Soul
            case 157: //Swift Zakum Soul
            case 158: //Clever Zakum Soul
            case 159: //Fortuitous Zakum Soul
            case 160: //Flashy Zakum Soul
            case 161: //Potent Zakum Soul
            case 162: //Radiant Zakum Soul
            case 163: //Hearty Zakum Soul
            case 164: //Magnificent Zakum Soul
                return 80001274;
            case 165: //Beefy Xerxes Soul
            case 166: //Swift Xerxes Soul
            case 167: //Clever Xerxes Soul
            case 168: //Fortuitous Xerxes Soul
            case 169: //Hearty Xerxes Soul
            case 170: //Ample Xerxes Soul
            case 171: //Flashy Xerxes Soul
                return 80001273;
            case 172: //Beefy Zakum Soul
            case 173: //Swift Zakum Soul
            case 174: //Clever Zakum Soul
            case 175: //Fortuitous Zakum Soul
            case 176: //Flashy Zakum Soul
            case 177: //Potent Zakum Soul
            case 178: //Radiant Zakum Soul
            case 179: //Hearty Zakum Soul
                return 80001274;
            case 180: //Beefy Cygnus Soul
            case 181: //Swift Cygnus Soul
            case 182: //Clever Cygnus Soul
            case 183: //Fortuitous Cygnus Soul
            case 184: //Flashy Cygnus Soul
            case 185: //Potent Cygnus Soul
            case 186: //Radiant Cygnus Soul
            case 187: //Hearty Cygnus Soul
                return EMPRESS_OFF_LAMES;
            case 188: //Beefy Ephenia Soul
            case 189: //Swift Ephenia Soul
            case 190: //Clever Ephenia Soul
            case 191: //Fortuitous Ephenia Soul
            case 192: //Hearty Ephenia Soul
            case 193: //Ample Ephenia Soul
            case 194: //Flashy Ephenia Soul
                return A_QUEENLY_FRAGRANCE;
            case 195: //Beefy Arkarium Soul
            case 196: //Swift Arkarium Soul
            case 197: //Clever Arkarium Soul
            case 198: //Fortuitous Arkarium Soul
            case 199: //Flashy Arkarium Soul
            case 200: //Potent Arkarium Soul
            case 201: //Radiant Arkarium Soul
            case 202: //Hearty Arkarium Soul
                return 80001281;
            case 203: //Magnificent Arkarium Soul
                return 80001282;
            case 204: //Beefy Ephenia Soul
            case 205: //Swift Ephenia Soul
            case 206: //Clever Ephenia Soul
            case 207: //Fortuitous Ephenia Soul
            case 208: //Hearty Ephenia Soul
            case 209: //Ample Ephenia Soul
            case 210: //Flashy Ephenia Soul
                return A_QUEENLY_FRAGRANCE;
            case 211: //Beefy Arkarium Soul
            case 212: //Swift Arkarium Soul
            case 213: //Clever Arkarium Soul
            case 214: //Fortuitous Arkarium Soul
            case 215: //Flashy Arkarium Soul
            case 216: //Potent Arkarium Soul
            case 217: //Radiant Arkarium Soul
            case 218: //Hearty Arkarium Soul
                return 80001281;
            case 219: //Beefy Pianus Soul
            case 220: //Swift Pianus Soul
            case 221: //Clever Pianus Soul
            case 222: //Fortuitous Pianus Soul
            case 223: //Hearty Pianus Soul
            case 224: //Ample Pianus Soul
            case 225: //Flashy Pianus Soul
                return 80001321;
            case 226: //Beefy Hilla Soul
            case 227: //Swift Hilla Soul
            case 228: //Clever Hilla Soul
            case 229: //Fortuitous Hilla Soul
            case 230: //Flashy Hilla Soul
            case 231: //Potent Hilla Soul
            case 232: //Radiant Hilla Soul
            case 233: //Hearty Hilla Soul
                return HILLA_FURY;
            case 234: //Magnificent Hilla Soul
                return HILLA_THUNDER;
            case 235: //Beefy Pianus Soul
            case 236: //Swift Pianus Soul
            case 237: //Clever Pianus Soul
            case 238: //Fortuitous Pianus Soul
            case 239: //Hearty Pianus Soul
            case 240: //Ample Pianus Soul
            case 241: //Flashy Pianus Soul
                return 80001321;
            case 242: //Beefy Hilla Soul
            case 243: //Swift Hilla Soul
            case 244: //Clever Hilla Soul
            case 245: //Fortuitous Hilla Soul
            case 246: //Flashy Hilla Soul
            case 247: //Potent Hilla Soul
            case 248: //Radiant Hilla Soul
            case 249: //Hearty Hilla Soul
                return HILLA_FURY;
            case 250: //Beefy Black Slime Soul
            case 251: //Swift Black Slime Soul
            case 252: //Clever Black Slime Soul
            case 253: //FortuitousBlack Slime Soul
            case 254: //Hearty Black Slime Soul
            case 255: //Ample Black Slime Soul
            case 256: //Flashy Black Slime Soul
                return 80001339;
            case 257: //Beefy Magnus Soul
            case 258: //Swift Magnus Soul
            case 259: //Clever Magnus Soul
            case 260: //Fortuitous Magnus Soul
            case 261: //Flashy Magnus Soul
            case 262: //Potent Magnus Soul
            case 263: //Radiant Magnus Soul
            case 264: //Hearty Magnus Soul
                return 80001340;
            case 265: //Magnificent Magnus Soul
                return WRATH_OF_MAGNUS;
            case 266: //Beefy Black Slime Soul
            case 267: //Swift Black Slime Soul
            case 268: //Clever Black Slime Soul
            case 269: //Fortuitous Black Slime Soul
            case 270: //Hearty Black Slime Soul
            case 271: //Ample Black Slime Soul
            case 272: //Flashy Black Slime Soul
                return 80001339;
            case 273: //Beefy Magnus Soul
            case 274: //Swift Magnus Soul
            case 275: //Clever Magnus Soul
            case 276: //Fortuitous Magnus Soul
            case 277: //Flashy Magnus Soul
            case 278: //Potent Magnus Soul
            case 279: //Radiant Magnus Soul
            case 280: //Hearty Magnus Soul
                return 80001340;
            case 289: //Beefy Murgoth Soul
            case 290: //Swift Murgoth Soul
            case 291: //Clever Murgoth Soul
            case 292: //Fortuitous Murgoth Soul
            case 293: //Flashy Murgoth Soul
            case 294: //Potent Murgoth Soul
            case 295: //Radiant Murgoth Soul
            case 296: //Hearty Murgoth Soul
                return MURGOTH_STRANGE_COMPANY;
            case 297: //Magnificent Murgoth Soul
                return MURGOTH_SUSPICIOUS_COMPANY;
            case 298: //Beefy Black Knight Soul
            case 299: //Swift Black Knight Soul
            case 300: //Clever Black Knight Soul
            case 301: //Fortuitous Black Knight Soul
            case 302: //Flashy Black Knight Soul
            case 303: //Potent Black Knight Soul
            case 304: //Radiant Black Knight Soul
            case 305: //Hearty Black Knight Soul
                return BLACK_KNIGHT;
            case 306: //Magnificent Black Knight Soul
                return PITCH_BLACK_KNIGHT;
            case 307: //Beefy Mad Mage Soul
            case 308: //Swift Mad Mage Soul
            case 309: //Clever Mad Mage Soul
            case 310: //Fortuitous Mad Mage Soul
            case 311: //Flashy Mad Mage Soul
            case 312: //Potent Mad Mage Soul
            case 313: //Radiant Mad Mage Soul
            case 314: //Hearty Mad Mage Soul
                return MAD_MAGE;
            case 315: //Magnificent Mad Mage Soul
                return COMPLETELY_MAD_MAGE;
            case 316: //Beefy Rampant Cyborg Soul
            case 317: //Swift Rampant Cyborg Soul
            case 318: //Clever Rampant Cyborg Soul
            case 319: //Fortuitous Rampant Cyborg Soul
            case 320: //Flashy Rampant Cyborg Soul
            case 321: //Potent Rampant Cyborg Soul
            case 322: //Radiant Rampant Cyborg Soul
            case 323: //Hearty Rampant Cyborg Soul
                return RAMPANT_CYBORG;
            case 324: //Magnificent Rampant Cyborg Soul
                return FULLY_RAMPANT_CYBORG;
            case 325: //Beefy Vicious Hunter Soul
            case 326: //Swift Vicious Hunter Soul
            case 327: //Clever Vicious Hunter Soul
            case 328: //Fortuitous Vicious Hunter Soul
            case 329: //Flashy Vicious Hunter Soul
            case 330: //Potent Vicious Hunter Soul
            case 331: //Radiant Vicious Hunter Soul
            case 332: //Hearty Vicious Hunter Soul
                return VICIOUS_HUNTER;
            case 333: //Magnificent Vicious Hunter Soul
                return UTTERLY_VICIOUS_HUNTER;
            case 334: //Beefy Bad Brawler Soul
            case 335: //Swift Bad Brawler Soul
            case 336: //Clever Bad Brawler Soul
            case 337: //Fortuitous Bad Brawler Soul
            case 338: //Flashy Bad Brawler Soul
            case 339: //Potent Bad Brawler Soul
            case 340: //Radiant Bad Brawler Soul
            case 341: //Hearty Bad Brawler Soul
                return BAD_BRAWLER;
            case 342: //Magnificent Bad Brawler Soul
                return REAL_BAD_BRAWLER;
            case 343: //Beefy Black Knight Soul
            case 344: //Swift Black Knight Soul
            case 345: //Clever Black Knight Soul
            case 346: //Fortuitous Black Knight Soul
            case 347: //Flashy Black Knight Soul
            case 348: //Potent Black Knight Soul
            case 349: //Radiant Black Knight Soul
            case 350: //Hearty Black Knight Soul
                return BLACK_KNIGHT;
            case 351: //Beefy Mad Mage Soul
            case 352: //Swift Mad Mage Soul
            case 353: //Clever Mad Mage Soul
            case 354: //Fortuitous Mad Mage Soul
            case 355: //Flashy Mad Mage Soul
            case 356: //Potent Mad Mage Soul
            case 357: //Radiant Mad Mage Soul
            case 358: //Hearty Mad Mage Soul
                return MAD_MAGE;
            case 359: //Beefy Rampant Cyborg Soul
            case 360: //Swift Rampant Cyborg Soul
            case 361: //Clever Rampant Cyborg Soul
            case 362: //Fortuitous Rampant Cyborg Soul
            case 363: //Flashy Rampant Cyborg Soul
            case 364: //Potent Rampant Cyborg Soul
            case 365: //Radiant Rampant Cyborg Soul
            case 366: //Hearty Rampant Cyborg Soul
                return RAMPANT_CYBORG;
            case 367: //Beefy Vicious Hunter Soul
            case 368: //Swift Vicious Hunter Soul
            case 369: //Clever Vicious Hunter Soul
            case 370: //Fortuitous Vicious Hunter Soul
            case 371: //Flashy Vicious Hunter Soul
            case 372: //Potent Vicious Hunter Soul
            case 373: //Radiant Vicious Hunter Soul
            case 374: //Hearty Vicious Hunter Soul
                return VICIOUS_HUNTER;
            case 375: //Beefy Bad Brawler Soul
            case 376: //Swift Bad Brawler Soul
            case 377: //Clever Bad Brawler Soul
            case 378: //Fortuitous Bad Brawler Soul
            case 379: //Flashy Bad Brawler Soul
            case 380: //Potent Bad Brawler Soul
            case 381: //Radiant Bad Brawler Soul
            case 382: //Hearty Bad Brawler Soul
                return BAD_BRAWLER;
            case 384: //Beefy Pierre Soul
            case 385: //Swift Pierre Soul
            case 386: //Clever Pierre Soul
            case 387: //Fortuitous Pierre Soul
            case 388: //Flashy Pierre Soul
            case 389: //Potent Pierre Soul
            case 390: //Radiant Pierre Soul
            case 391: //Hearty Pierre Soul
                return Util.getRandom(80001682, 80001683);
            case 392: //Magnificent Pierre Soul
                return Util.getRandom(80001680, 80001681);
            case 393: //Beefy Von Bon Soul
            case 394: //Swift Von Bon Soul
            case 395: //Clever Von Bon Soul
            case 396: //Fortuitous Von Bon Soul
            case 397: //Flashy Von Bon Soul
            case 398: //Potent Von Bon Soul
            case 399: //Radiant Von Bon Soul
            case 400: //Hearty Von Bon Soul
                return 80001684;
            case 401: //Magnificent Von Bon Soul
                return CHICKEN_RISING;
            case 402: //Beefy Crimson Queen Soul
            case 403: //Swift Crimson Queen Soul
            case 404: //Clever Crimson Queen Soul
            case 405: //Fortuitous Crimson Queen Soul
            case 406: //Flashy Crimson Queen Soul
            case 407: //Potent Crimson Queen Soul
            case 408: //Radiant Crimson Queen Soul
            case 409: //Hearty Crimson Queen Soul
                return Util.getRandom(80001686, 80001689);
            case 410: //Magnificent Crimson Queen Soul
                return Util.getRandom(LONG_LIVE_THE_QUEEN, LONG_LIVE_THE_QUEEN_3);
            case 411: //Beefy Vellum Soul
            case 412: //Swift Vellum Soul
            case 413: //Clever Vellum Soul
            case 414: //Fortuitous Vellum Soul
            case 415: //Flashy Vellum Soul
            case 416: //Potent Vellum Soul
            case 417: //Radiant Vellum Soul
            case 418: //Hearty Vellum Soul
                return 80001694;
            case 419: //Magnificent Vellum Soul
                return JR_VELLUM;
            case 420: //Beefy Lotus Soul
            case 421: //Swift Lotus Soul
            case 422: //Clever Lotus Soul
            case 423: //Fortuitous  Lotus Soul
            case 424: //Flashy Lotus Soul
            case 425: //Potent Lotus Soul
            case 426: //Radiant Lotus Soul
            case 427: //Hearty Lotus Soul
                return LOTUS_STRIKE;
            case 428: //Magnificent Lotus Soul
                return LOTUS_ENRAGED;
            case 429: //Beefy Pierre Soul
            case 430: //Swift Pierre Soul
            case 431: //Clever Pierre Soul
            case 432: //Fortuitous Pierre Soul
            case 433: //Flashy Pierre Soul
            case 434: //Potent Pierre Soul
            case 435: //Radiant Pierre Soul
            case 436: //Hearty Pierre Soul
                return Util.getRandom(80001682, 80001683);
            case 437: //Beefy Von Bon Soul
            case 438: //Swift Von Bon Soul
            case 439: //Clever Von Bon Soul
            case 440: //Fortuitous Von Bon Soul
            case 441: //Flashy Von Bon Soul
            case 442: //Potent Von Bon Soul
            case 443: //Radiant Von Bon Soul
            case 444: //Hearty Von Bon Soul
                return 80001684;
            case 445: //Beefy Crimson Queen Soul
            case 446: //Swift Crimson Queen Soul
            case 447: //Swift Crimson Queen Soul
            case 448: //Fortuitous Crimson Queen Soul
            case 449: //Flashy Crimson Queen Soul
            case 450: //Potent Crimson Queen Soul
            case 451: //Radiant Crimson Queen Soul
            case 452: //Hearty Crimson Queen Soul
                return Util.getRandom(80001686, 80001689);
            case 453: //Beefy Vellum Soul
            case 454: //Swift Vellum Soul
            case 455: //Clever Vellum Soul
            case 456: //Fortuitous Vellum Soul
            case 457: //Flashy Vellum Soul
            case 458: //Potent Vellum Soul
            case 459: //Radiant Vellum Soul
            case 460: //Hearty Vellum Soul
                return 80001694;
            case 461: //Beefy Lotus Soul
            case 462: //Swift Lotus Soul
            case 463: //Clever Lotus Soul
            case 464: //Fortuitous  Lotus Soul
            case 465: //Flashy Lotus Soul
            case 466: //Potent Lotus Soul
            case 467: //Radiant Lotus Soul
            case 468: //Hearty Lotus Soul
                return LOTUS_STRIKE;
            case 469: //Beefy Gold Dragon Soul
            case 470: //Swift Gold Dragon Soul
            case 471: //Clever Gold Dragon Soul
            case 472: //Fortuitous Gold Dragon Soul
            case 473: //Flashy Gold Dragon Soul
            case 474: //Potent Gold Dragon Soul
            case 475: //Radiant Gold Dragon Soul
            case 476: //Hearty Gold Dragon Soul
                return 80011173;
            case 477: //Beefy Red Tiger Soul
            case 478: //Swift Red Tiger Soul
            case 479: //Clever Red Tiger Soul
            case 480: //Fortuitous Red Tiger Soul
            case 481: //Flashy Red Tiger Soul
            case 482: //Potent Red Tiger Soul
            case 483: //Radiant Red Tiger Soul
            case 484: //Hearty Red Tiger Soul
            case 485: //Magnificent Gold Dragon Soul
            case 486: //Magnificent Red Tiger Soul
                return 80011174;
            case 487: //Magnificent Pink Bean Soul
                return MORE_THAN_A_CUTIE;
            case 488: //??? ??? ??
            case 489: //??? ??? ??
            case 490: //??? ??? ??
            case 491: //??? ??? ??
            case 492: //??? ??? ??
            case 493: //??? ??? ??
            case 494: //??? ??? ??
            case 495: //??? ??? ??
            case 496: //??? ??? ??
            case 501: //??? ????PC? ??
            case 502: //??? ????PC? ??
            case 503: //??? ????PC? ??
            case 504: //??? ????PC? ??
            case 505: //??? ????PC? ??
            case 506: //??? ????PC? ??
            case 507: //??? ????PC? ??
            case 508: //??? ????PC? ??
            case 509: //??? ????PC???
                break;
            case 510: //Beefy Ursus Soul
            case 511: //Swift Ursus Soul
            case 512: //Clever Ursus Soul
            case 513: //Fortuitous Ursus Soul
            case 514: //Flashy Ursus Soul
            case 515: //Potent Ursus Soul
            case 516: //Radiant Ursus Soul
            case 517: //Hearty Ursus Soul
                return MIGHTY_ROAR;
            case 518: //Magnificent Ursus Soul
                return FEROCIOUS_ROAR;
            case 519: //Beefy Ursus Soul
            case 520: //Swift Ursus Soul
            case 521: //Clever Ursus Soul
            case 522: //Fortuitous Ursus Soul
            case 523: //Flashy Ursus Soul
            case 524: //Potent Ursus Soul
            case 525: //Radiant Ursus Soul
            case 526: //Hearty Ursus Soul
                return MIGHTY_ROAR;
            case 529: //Beefy Pink Mong Soul
            case 530: //Swift Pink Mong Soul
            case 531: //Clever Pink Mong Soul
            case 532: //Fortuitous Pink Mong Soul
            case 533: //Hearty Pink Mong Soul
            case 534: //Ample Pink Mong Soul
            case 535: //Flashy Pink Mong Soul
                return HAPPY_NEW_WEEK;
            case 545: //Beefy Tutu Soul
            case 546: //Swift Tutu Soul
            case 547: //Clever Tutu Soul
            case 548: //Fortuitous Tutu Soul
            case 549: //Flashy Tutu Soul
            case 550: //Potent Tutu Soul
            case 551: //Radiant Tutu Soul
            case 552: //Hearty Tutu Soul
            case 553: //Magnificent Tutu Soul
                return 80011338;
            case 554: //Beefy Nene Soul
            case 555: //Swift Nene Soul
            case 556: //Clever Nene Soul
            case 557: //Fortuitous Nene Soul
            case 558: //Flashy Nene Soul
            case 559: //Potent Nene Soul
            case 560: //Radiant Nene Soul
            case 561: //Hearty Nene Soul
            case 562: //Magnificent Nene Soul
            case 563: //Nene's Soul Orb
                return 80011337;
            case 564: //Tutu's Soul Orb
                return 80011338;
            case 565: //Beefy Damien Soul
            case 566: //Swift Damien Soul
            case 567: //Clever Damien Soul
            case 568: //Fortuitous Damien Soul
            case 569: //Flashy Damien Soul
            case 570: //Potent Damien Soul
            case 571: //Radiant Damien Soul
            case 572: //Hearty Damien Soul
                return 80001984;
            case 573: //Magnificent Damien Soul
                return 80001985;
            case 574: //Beefy Damien Soul
            case 575: //Swift Damien Soul
            case 576: //Clever Damien Soul
            case 577: //Fortuitous Damien Soul
            case 578: //Flashy Damien Soul
            case 579: //Potent Damien Soul
            case 580: //Radiant Damien Soul
            case 581: //Hearty Damien Soul
                return 80001984;
            case 582: //Magnificent Damien Soul
                return 80001985;
            case 583: //Beefy Lucid Soul
            case 584: //Swift Lucid Soul
            case 585: //Clever Lucid Soul
            case 586: //Fortuitous Lucid Soul
            case 587: //Flashy Lucid Soul
            case 588: //Potent Lucid Soul
            case 589: //Radiant Lucid Soul
            case 590: //Hearty Lucid Soul
                return 80002230;
            case 591: //Magnificent Lucid Soul
                return 80002231;
        }
        return 0;
    }

    public static boolean isSoulLowTier(int itemID) {
        return switch (itemID) {
            case 2591007, 2591010, 2591011, 2591012, 2591013, 2591014, 2591015, 2591016, 2591089, 2591090, 2591091, 2591092, 2591093, 2591094, 2591095, 2591017, 2591018, 2591019, 2591020, 2591021, 2591022, 2591023, 2591096, 2591097, 2591098, 2591099, 2591100, 2591101, 2591102, 2591008, 2591038, 2591039, 2591040, 2591041, 2591042, 2591043, 2591044, 2591117, 2591118, 2591119, 2591120, 2591121, 2591122, 2591123, 2591004, 2591024, 2591025, 2591026, 2591027, 2591028, 2591029, 2591030, 2591103, 2591104, 2591105, 2591106, 2591107, 2591108, 2591109, 2591002, 2591031, 2591032, 2591033, 2591034, 2591035, 2591036, 2591037, 2591110, 2591111, 2591112, 2591113, 2591114, 2591115, 2591116, 2591218, 2591219, 2591220, 2591221, 2591222, 2591223, 2591224, 2591234, 2591235, 2591236, 2591237, 2591238, 2591239, 2591240, 2591187, 2591188, 2591189, 2591190, 2591191, 2591192, 2591193, 2591203, 2591204, 2591205, 2591206, 2591207, 2591208, 2591209, 2591148, 2591149, 2591150, 2591151, 2591152, 2591153, 2591154, 2591164, 2591165, 2591166, 2591167, 2591168, 2591169, 2591170, 2591155, 2591156, 2591157, 2591158, 2591159, 2591160, 2591161, 2591162, 2591163, 2591171, 2591172, 2591173, 2591174, 2591175, 2591176, 2591177, 2591178, 2591000, 2591045, 2591046, 2591047, 2591048, 2591049, 2591050, 2591051, 2591052, 2591053, 2591054, 2591085, 2591124, 2591125, 2591126, 2591127, 2591128, 2591129, 2591130, 2591131, 2591468, 2591469, 2591470, 2591471, 2591472, 2591473, 2591474, 2591475, 2591484, 2591476, 2591477, 2591478, 2591479, 2591480, 2591481, 2591482, 2591483, 2591485 ->
                    true;
            default -> false;
        };
    }

    public static List<Integer> getDecentPotentialSkillID(int[] potentialBase) {
        List<Integer> result = new ArrayList<>();
        for (int i : potentialBase) {
            switch (i) {
                case 31001 -> result.add(8000);
                case 31002 -> result.add(8001);
                case 31003 -> result.add(8002);
                case 31004 -> result.add(8003);
                case 41005 -> result.add(8004);
                case 41006 -> result.add(8005);
                case 41007 -> result.add(8006);
            }
        }
        return result;
    }

    public static Integer getDecentNebuliteSkillID(Integer nebuliteID) {
        return switch (nebuliteID) {
            case 3063370 -> 8000;
            case 3063380 -> 8001;
            case 3063390 -> 8002;
            case 3063400 -> 8003;
            case 3064470 -> 8004;
            case 3064480 -> 8005;
            case 3064490 -> 8006;
            default -> 0;
        };
    }

    public static boolean isMobCard(int itemID) {
        return getItemPrefix(itemID) == 238;
    }

    public static boolean isCollisionLootItem(int itemID) {
        switch (itemID) {
            case 2023484: // Blue
            case 2023494: // Purple
            case 2023495: // Red
            case 2023669: // Gold
                return true;
            default:
                return false;
        }
    }

    public static boolean isUpgradable(int itemID) {
        BodyPart bodyPart = BodyPart.getByVal(getLegacyBodyPartFromItem(itemID, 0));
        if (bodyPart == null || getItemPrefix(itemID) == EquipPrefix.SecondaryWeapon.getVal()) {
            return false;
        }
        return switch (bodyPart) {
            case Ring1, Ring2, Ring3, Ring4, Pendant, ExtendedPendant, Weapon, Belt, Hat, FaceAccessory, EyeAccessory, Top, Bottom, Shoes, Earrings, Shoulder, Gloves, Badge, Shield, Cape, MechanicalHeart ->
                    true;
            default -> false;
        };
    }

    public static List<ScrollUpgradeInfo> getScrollUpgradeInfosForFullSlotsEquip(Char chr, Equip equip) {
        List<ScrollUpgradeInfo> scrolls = new ArrayList<>();
        if (equip.hasUsedSlots()) {
            int guildBonus = 0;
            if (chr.getGuild() != null) {
                GuildSkill guildSkill = chr.getGuild().getSkillById(GuildConstants.ENHANCEMENT_MASTERY);
                SkillInfo skillInfo = SkillData.getSkillInfoById(GuildConstants.ENHANCEMENT_MASTERY);
                if (guildSkill != null && skillInfo != null) {
                    guildBonus = skillInfo.getValue(SkillStat.itemUpgradeBonusR, guildSkill.getLevel());
                    chr.chatMessage("[公會技能] 依公會技能效果，成功機率額外增加 " + guildBonus + "%。");
                }
            }

            ScrollUpgradeInfo inno = new ScrollUpgradeInfo(4, "回真卷軸100%", SpellTraceScrollType.Innocence, 1, new TreeMap<>(), INNOCENCE_ST_COST, 100);
            ScrollUpgradeInfo css = new ScrollUpgradeInfo(5, "純白的卷軸100%", SpellTraceScrollType.CleanSlate, 0, new TreeMap<>(), CLEAN_SLATE_ST_COST, 100);
            ScrollUpgradeInfo ark = new ScrollUpgradeInfo(6, "亞克回真卷軸100%", SpellTraceScrollType.Innocence, 4, new TreeMap<>(), ARK_INNOCENCE_ST_COST, 100);
            scrolls.add(inno);
            scrolls.add(css);
            scrolls.add(ark);
        }
        return scrolls;
    }

    private static int getJobMaskFromChar(short job) {
        if (JobConstants.isXenon(job)) return RequiredJob.Xenon.getVal(); // 0x18
        if (JobConstants.isWarriorEquipJob(job)) return RequiredJob.Warrior.getVal(); // 0x1
        if (JobConstants.isMageEquipJob(job)) return RequiredJob.Magician.getVal(); // 0x2
        if (JobConstants.isArcherEquipJob(job)) return RequiredJob.Bowman.getVal(); // 0x4
        if (JobConstants.isThiefEquipJob(job)) return RequiredJob.Thief.getVal(); // 0x8
        if (JobConstants.isPirateEquipJob(job)) return RequiredJob.Pirate.getVal(); // 0x10
        return RequiredJob.AnyJob.getVal(); // 0
    }

    private static int getEffectiveReqJobMask(short chrJob, int equipRJob) {
        // Shared/AnyJob -> theo nhân vật đang scroll
        if (equipRJob == 0 || equipRJob == RequiredJob.AnyJob.getVal()) {
            return getJobMaskFromChar(chrJob);
        }
        return equipRJob;
    }

    private static final EnchantStat ALL_STATS = null; // marker

    private static String statShort(EnchantStat s) {
        if (s == ALL_STATS) return "全屬性";
        return switch (s) {
            case STR -> "STR";
            case DEX -> "DEX";
            case INT -> "INT";
            case LUK -> "LUK";
            case MHP -> "HP";
            default -> "";
        };
    }


    /**
     * Weapon/Gloves: mỗi option chỉ là PAD+{STR/DEX/LUK/HP} hoặc MAD+INT
     * - Warrior: STR hoặc HP
     * - Xenon: STR/DEX/LUK
     * - Pirate: STR và (tuỳ server) DEX
     * - Shared/AnyJob: STR/DEX/INT/LUK (+HP nếu muốn)
     */
    private static List<EnchantStat> getWeaponGloveMainOptions(int reqMask) {
        List<EnchantStat> list = new ArrayList<>(4);

        if ((reqMask & RequiredJob.Xenon.getVal()) > 0) {
            list.add(EnchantStat.STR);
            list.add(EnchantStat.DEX);
            list.add(EnchantStat.LUK);
            return list;
        }
        if ((reqMask & RequiredJob.Warrior.getVal()) > 0) {
            list.add(EnchantStat.STR);
            list.add(EnchantStat.MHP);
            return list;
        }
        if ((reqMask & RequiredJob.Magician.getVal()) > 0) {
            list.add(EnchantStat.INT);
            return list;
        }
        if ((reqMask & RequiredJob.Bowman.getVal()) > 0) {
            list.add(EnchantStat.DEX);
            return list;
        }
        if ((reqMask & RequiredJob.Thief.getVal()) > 0) {
            list.add(EnchantStat.LUK);
            return list;
        }
        if ((reqMask & RequiredJob.Pirate.getVal()) > 0) {
            list.add(EnchantStat.STR);
            list.add(EnchantStat.DEX); // nếu bạn không muốn pirate DEX thì xoá dòng này
            return list;
        }

        // AnyJob/Shared fallback
        list.add(EnchantStat.STR);
        list.add(EnchantStat.DEX);
        list.add(EnchantStat.INT);
        list.add(EnchantStat.LUK);
        // list.add(EnchantStat.MHP); // bật nếu AnyJob weapon/glove có HP option
        return list;
    }

    /**
     * Armor/Accessory: stat options tách riêng (STR / DEX / INT / LUK), Xenon: STR/DEX/LUK
     */
    private static List<EnchantStat> getArmorAccessoryMainOptions(int reqMask) {
        List<EnchantStat> list = new ArrayList<>(5);

        // Xenon: tách riêng
        if ((reqMask & RequiredJob.Xenon.getVal()) > 0) {
            list.add(EnchantStat.STR);
            list.add(EnchantStat.DEX);
            list.add(EnchantStat.LUK);
        } else if ((reqMask & RequiredJob.Warrior.getVal()) > 0) {
            list.add(EnchantStat.STR);
        } else if ((reqMask & RequiredJob.Magician.getVal()) > 0) {
            list.add(EnchantStat.INT);
        } else if ((reqMask & RequiredJob.Bowman.getVal()) > 0) {
            list.add(EnchantStat.DEX);
        } else if ((reqMask & RequiredJob.Thief.getVal()) > 0) {
            list.add(EnchantStat.LUK);
        } else if ((reqMask & RequiredJob.Pirate.getVal()) > 0) {
            list.add(EnchantStat.STR);
            list.add(EnchantStat.DEX); // nếu muốn pirate DEX
        } else {
            list.add(EnchantStat.STR);
            list.add(EnchantStat.DEX);
            list.add(EnchantStat.INT);
            list.add(EnchantStat.LUK);
        }

        list.add(ALL_STATS);

        return list;
    }


    public static List<ScrollUpgradeInfo> getScrollUpgradeInfosByEquip(Char chr, Equip equip, boolean fevertime) {
        BodyPart bp = BodyPart.getByVal(ItemConstants.getLegacyBodyPartFromItem(equip.getItemId(), 0));
        List<ScrollUpgradeInfo> scrolls = new ArrayList<>();

        int rLevel = equip.getrLevel() + equip.getiIncReq();
        int rJob = equip.getrJob();
        short job = chr.getJob();

        int reqMask = getEffectiveReqJobMask(job, rJob);

        int plusFromLevel;
        int[] chances;
        int[] attStats = new int[0];
        int[] stat;
        int[] armorHp = new int[]{5, 20, 30, 70, 120};
        int[] armorDef = new int[]{1, 2, 4, 7, 10};
        boolean armor = false;

        // Guild Skill Discount
        int guildBonus = 0;
        if (chr.getGuild() != null) {
            GuildSkill guildSkill = chr.getGuild().getSkillById(GuildConstants.ENHANCEMENT_MASTERY);
            SkillInfo skillInfo = SkillData.getSkillInfoById(GuildConstants.ENHANCEMENT_MASTERY);
            if (guildSkill != null && skillInfo != null) {
                guildBonus = skillInfo.getValue(SkillStat.itemUpgradeBonusR, guildSkill.getLevel());
                chr.chatMessage("[公會技能] 依公會技能效果，成功機率額外增加 " + guildBonus + "%。");
            }
        }

        // Configure by body part
        if (bp == BodyPart.Weapon) {
            plusFromLevel = rLevel >= 120 ? 2 : rLevel >= 60 ? 1 : 0;
            chances = new int[]{100, 70, 30, 15};

            if (fevertime) {
                chances[1] = Math.min(chances[1] + 25, 100);
                chances[2] = Math.min(chances[2] + 15, 100);
                chances[3] = Math.min(chances[3] + 10, 100);
            }
            if (guildBonus != 0) {
                chances[1] = Math.min(chances[1] + guildBonus, 100);
                chances[2] = Math.min(chances[2] + guildBonus, 100);
                chances[3] = Math.min(chances[3] + guildBonus, 100);
            }

            attStats = new int[]{1, 2, 3, 5, 7, 9};
            stat = new int[]{0, 0, 1, 2, 3, 4};
        } else if (bp == BodyPart.Gloves) {
            plusFromLevel = rLevel <= 70 ? 0 : 1;
            chances = new int[]{100, 70, 30};

            if (fevertime) {
                chances[1] = Math.min(chances[1] + 25, 100);
                chances[2] = Math.min(chances[2] + 15, 100);
            }
            if (guildBonus != 0) {
                chances[1] = Math.min(chances[1] + guildBonus, 100);
                chances[2] = Math.min(chances[2] + guildBonus, 100);
            }

            attStats = new int[]{0, 1, 2, 3};
            stat = new int[]{3, 0, 0, 0};
        } else if (ItemConstants.isAccessory(equip.getItemId())) {
            plusFromLevel = rLevel >= 120 ? 2 : rLevel >= 60 ? 1 : 0;
            chances = new int[]{100, 70, 30};

            if (fevertime) {
                chances[1] = Math.min(chances[1] + 25, 100);
                chances[2] = Math.min(chances[2] + 15, 100);
            }
            if (guildBonus != 0) {
                chances[1] = Math.min(chances[1] + guildBonus, 100);
                chances[2] = Math.min(chances[2] + guildBonus, 100);
            }

            stat = new int[]{1, 1, 2, 3, 5};
        } else {
            armor = true;
            plusFromLevel = rLevel >= 120 ? 2 : rLevel >= 60 ? 1 : 0;
            chances = new int[]{100, 70, 30};

            if (fevertime) {
                chances[1] = Math.min(chances[1] + 25, 100);
                chances[2] = Math.min(chances[2] + 15, 100);
            }
            if (guildBonus != 0) {
                chances[1] = Math.min(chances[1] + guildBonus, 100);
                chances[2] = Math.min(chances[2] + guildBonus, 100);
            }

            stat = new int[]{1, 2, 3, 5, 7};
        }

        // Build scroll options (tách theo main stat, không gom nhiều stat vào 1 scroll)
        List<EnchantStat> mainOptions =
                (bp == BodyPart.Weapon || bp == BodyPart.Gloves)
                        ? getWeaponGloveMainOptions(reqMask)
                        : getArmorAccessoryMainOptions(reqMask);

        for (EnchantStat main : mainOptions) {
            boolean isMageMain = (main == EnchantStat.INT);

            for (int iconID = 0; iconID < chances.length; iconID++) {
                int tier = iconID + plusFromLevel;

                // safety
                if (tier < 0) continue;
                if (attStats.length > 0 && tier >= attStats.length) continue;
                if (tier >= stat.length) continue;
                if (armor && tier >= armorHp.length) continue;
                if (armor && tier >= armorDef.length) continue;

                TreeMap<EnchantStat, Integer> stats = new TreeMap<>();

                // Weapon/Gloves: luôn có 1 loại ATT
                if (bp == BodyPart.Weapon || bp == BodyPart.Gloves) {
                    if (isMageMain) stats.put(EnchantStat.MAD, attStats[tier]);
                    else            stats.put(EnchantStat.PAD, attStats[tier]);
                }

                // Main stat: CHỈ 1 trong STR/DEX/INT/LUK/HP
                int v = (main == EnchantStat.MHP) ? (stat[tier] * 50) : stat[tier];
                if (v != 0) {
                    if (main == ALL_STATS) {
                        stats.put(EnchantStat.STR, v);
                        stats.put(EnchantStat.DEX, v);
                        stats.put(EnchantStat.INT, v);
                        stats.put(EnchantStat.LUK, v);
                    } else {
                        stats.put(main, v);
                    }
                }

                // Armor base bonuses (giữ như code cũ, áp lên từng scroll)
                if (armor) {
                    stats.put(EnchantStat.PDD, armorDef[tier] + stats.getOrDefault(EnchantStat.PDD, 0));
                    stats.put(EnchantStat.MDD, armorDef[tier] + stats.getOrDefault(EnchantStat.MDD, 0));
                    stats.put(EnchantStat.MHP, armorHp[tier] + stats.getOrDefault(EnchantStat.MHP, 0));
                }

                String title = chances[iconID] + "% ";
                if (bp == BodyPart.Weapon || bp == BodyPart.Gloves) {
                    title += (isMageMain ? "魔力卷軸" : "攻擊力卷軸");
                    title += " (" + statShort(main) + ")";
                } else {
                    title += statShort(main) + " 卷軸";
                }

                ScrollUpgradeInfo sui = new ScrollUpgradeInfo(
                        iconID,
                        title,
                        SpellTraceScrollType.Normal,
                        0,
                        stats,
                        BASE_ST_COST + rLevel * (tier + 1),
                        chances[iconID]
                );
                scrolls.add(sui);
            }
        }

        if (equip.getTuc() == 0) {
            scrolls.clear();
        }

        if (equip.hasUsedSlots()) {
            ScrollUpgradeInfo inno = new ScrollUpgradeInfo(4, "回真卷軸100%", SpellTraceScrollType.Innocence, 1, new TreeMap<>(), INNOCENCE_ST_COST, 100);
            ScrollUpgradeInfo css = new ScrollUpgradeInfo(5, "純白的卷軸100%", SpellTraceScrollType.CleanSlate, 0, new TreeMap<>(), CLEAN_SLATE_ST_COST, 100);
            ScrollUpgradeInfo ark = new ScrollUpgradeInfo(6, "亞克回真卷軸100%", SpellTraceScrollType.Innocence, 4, new TreeMap<>(), ARK_INNOCENCE_ST_COST, 100);
            scrolls.add(inno);
            scrolls.add(css);
            scrolls.add(ark);
        }

        return scrolls;
    }

    // is_tuc_ignore_item(int itemID)
    public static boolean isTucIgnoreItem(int itemID) {
        return (isSecondary(itemID) || isEmblem(itemID) || isDragonEssence(itemID) || isDemonShield(itemID) || isSoulShield(itemID) || Collections.singletonList(TUC_IGNORE_ITEMS).contains(itemID));
    }

    public static boolean isDemonShield(int itemID) {
        return switch (itemID) {
            case 1099000, 1099001, 1099002, 1099003, 1099004, 1099005, 1099006, 1099007, 1099008, 1099009, 1099010, 1099011, 1099012, 1099013, 1099014, 1099015 ->
                    true;
            default -> false;
        };
    }

    public static boolean isDragonEssence(int itemID) {
        return switch (itemID) {
            case 1352500, 1352501, 1352502, 1352503, 1352504, 1352505, 1352506, 1352507, 1352508 -> true;
            default -> false;
        };
    }

    public static boolean isSoulShield(int itemID) {
        return switch (itemID) {
            case 1098000, 1098001, 1098002, 1098003, 1098004, 1098005, 1098006, 1098007, 1098008 -> true;
            default -> false;
        };
    }

    public static PetSkill getPetSkillFromID(int itemID) {
        return switch (itemID) {
            case 5190000, 5191000, 5191003 -> PetSkill.ITEM_PICKUP;
            case 5190001, 5191001 -> PetSkill.AUTO_HP;
            case 5190002, 5191002 -> PetSkill.EXPANDED_AUTO_MOVE;
            case 5190003 -> PetSkill.AUTO_MOVE;
            case 5190004 -> PetSkill.EXPIRED_PICKUP;
            case 5190005 -> PetSkill.IGNORE_ITEM;
            case 5190006 -> PetSkill.AUTO_MP;
            case 5190007 -> PetSkill.RECALL;
            case 5190008 -> PetSkill.AUTO_SPEAKING;
            case 5190009 -> PetSkill.AUTO_ALL_CURE;
            case 5190010 -> PetSkill.AUTO_BUFF;
            case 5190011 -> PetSkill.AUTO_FEED;
            case 5190012, 5190014 -> PetSkill.FATTEN_UP;
            case 5190013 -> PetSkill.PET_SHOP;
            default -> null;
        };
    }

    // Gets the hardcoded starforce capacities Nexon introduced for equips above level 137.
    // The cap for stars is in GetHyperUpgradeCapacity (E8 ? ? ? ? 0F B6 CB 83 C4 0C, follow `call`),
    // therefore it needs to be manually implemented on the server side.
    // Nexon's decision was very poor, but will require client edits to revert.
    static int getItemStarLimit(int itemID) {
        switch (itemID) {
            case 1072870: // Sweetwater Shoes
            case 1082556: // Sweetwater Gloves
            case 1102623: // Sweetwater Cape
            case 1132247: // Sweetwater Belt
                return 15;
            case 1182060: // Ghost Ship Exorcist
            case 1182273: // Sengoku Hakase Badge
                return 22;
        }
        return 30;
    }

    public static int getEquippedSummonSkillItem(int itemID, short job) {
        switch (itemID) {
            case 1112585:// Angelic Blessing
                return (SkillConstants.getNoviceSkillRoot(job) * 10000) + 1085;
            case 1112586:// Dark Angelic Blessing
                return (SkillConstants.getNoviceSkillRoot(job) * 10000) + 1087;
            case 1112594:// Snowdrop Angelic Blessing
                return (SkillConstants.getNoviceSkillRoot(job) * 10000) + 1090;
            case 1112663:// White Angelic Blessing
                return (SkillConstants.getNoviceSkillRoot(job) * 10000) + 1179;
            case 1112735:// White Angelic Blessing 2
                return 80001154;
            case 1113020:// Lightning God Ring
                return 80001262;
            case 1113173:// Lightning God Ring 2
                return 80011178;
            // Heaven Rings
            case 1112932:// Guard Ring
                return 80011149;
            case 1114232:// Sun Ring
                return 80010067;
            case 1114233:// Rain Ring
                return 80010068;
            case 1114234:// Rainbow Ring
                return 80010069;
            case 1114235:// Snow Ring
                return 80010070;
            case 1114236:// Lightning Ring
                return 80010071;
            case 1114237:// Wind Ring
                return 80010072;
        }
        return 0;
    }

    public static boolean isRecipeOpenItem(int itemID) {
        return itemID / 10000 == 251;
    }

    public static boolean isChair(int itemID) {
        return itemID / 10000 == 301 || itemID / 1000 == 5204;
    }

    public static boolean isTextChair(int itemID) {
        return itemID / 1000 == 3014;
    }

    public static boolean isTowerChair(int itemID) {
        return itemID / 1000 == 3017;
    }

    public static boolean isMesoChair(int itemID) {
        return itemID == 3015650 || itemID == 3015651 || itemID == 3015440 || itemID == 3015897;
    }

    public static Set<DropInfo> getConsumableMobDrops(int level) {
        int bucket = Math.min(100, (level / 20) * 20);
        Set<DropInfo> set = consumableDropsPerLevel.get(bucket);
        return set != null ? set : EMPTY_DROPS;
    }

    public static Set<DropInfo> getGlobalCustomMobDrops(int level) {
        if (level >= 10 && level < 49) {
            return globalDropsPerLevel.getOrDefault(10, EMPTY_DROPS);
        } else if (level >= 50 && level < 79) {
            return globalDropsPerLevel.getOrDefault(50, EMPTY_DROPS);
        } else if (level >= 80 && level < 99) {
            return globalDropsPerLevel.getOrDefault(80, EMPTY_DROPS);
        } else if (level >= 100 && level <= 150) {
            return globalDropsPerLevel.getOrDefault(100, EMPTY_DROPS);
        }
        return EMPTY_DROPS;
    }

    public static Set<DropInfo> getEliteMobDrops() {
        return eliteDrop.getOrDefault(0, EMPTY_DROPS);
    }

    public static Set<DropInfo> getEliteChannelDrops() {
        return eliteChannelDrop.getOrDefault(0, EMPTY_DROPS);
    }

    public static boolean isMiuMiuMerchant(int itemID) {
        return itemID == 5450000 || itemID == 5450003 || itemID == 5450004 || itemID == 5450006 || itemID == 5450007 || itemID == 5450012 || itemID == 5450013;
    }

    public static boolean isPortableStorage(int itemID) {
        return itemID == 5450009 || itemID == 5450008 || itemID == 5450005;
    }

    public static boolean is2XDropCoupon(int itemId) {
        return itemId <= 5360057 && itemId >= 5360000;
    }

    public static boolean isFusionAnvil(int itemId) {
        return itemId / 100 == 50624;
    }

    public static boolean isRewardCashItem(int itemId) {
        return getItemPrefix(itemId) == 553;
    }

    public static boolean isAvatarMegaphoneItem(int itemId) {
        return getItemPrefix(itemId) == 539;
    }

    public static boolean isPetSkillItem(int itemId) {
        return getItemPrefix(itemId) == 519;
    }

    public static boolean isCube(int itemId) {
        return itemId / 100 == 50620;
    }

    public static boolean isSpecialCube(int itemId) {
        return itemId / 100 == 50621;
    }

    public static boolean isBonusCube(int itemId) {
        return itemId / 100 == 50625;
    }

    public static boolean isNebuliteDiffuser(int itemId) {
        return itemId == 5750001;
    }

    public static boolean isAlienCube(int itemId) {
        return itemId == 5750000;
    }

    public static boolean isMegaphoneItem(int itemId) {
        return itemId == 5070000 || itemId == 5071000;
    }

    public static boolean isSpecialMegaphoneItem(int itemId) {
        return itemId == 5072000 || itemId == 5072001 || itemId == 5076000 || itemId == 5076100 || itemId == 5077000;
    }

    public static boolean isChangeStatItem(int itemId) {
        return getItemPrefix(itemId) == 200
                || getItemPrefix(itemId) == 201
                || getItemPrefix(itemId) == 202
                || getItemPrefix(itemId) == 205
                || getItemPrefix(itemId) == 221
                || getItemPrefix(itemId) == 236
                || getItemPrefix(itemId) == 238
                || getItemPrefix(itemId) == 245
                || getItemPrefix(itemId) == 290
                || getItemPrefix(itemId) == 280;
    }

    public static boolean isScriptRunConsumeItem(int itemId) {
        return getItemPrefix(itemId) == 243;
    }

    public static boolean isScritpRunCashItem(int itemId) {
        return getItemPrefix(itemId) == 568;
    }

    public static boolean isAPResetItem(int itemId) {
        return itemId / 1000 == 2501 || itemId == 5050100;
    }

    public static boolean isAPChangeItem(int itemId) {
        return itemId == 2502000 || itemId == 5050000;
    }

    public static boolean isSkillResetItem(int itemId) {
        return itemId / 1000 == 2500 || itemId == 5051001;
    }

    public static boolean isCirculatorItem(int itemId) {
        return itemId / 1000 == 2702 || itemId == 5062800 || itemId == 5062801;
    }

    public static boolean isJukeBoxItem(int itemId) {
        return getItemPrefix(itemId) == 510;
    }

    public static boolean isChangeMapEffectItem(int itemId) {
        return getItemPrefix(itemId) == 512;
    }

    public static boolean isMesoBagItem(int itemId) {
        return itemId == 5200000 || itemId == 5200001 || itemId == 5200002;
    }

    public static boolean isMaplePointChipItem(int itemId) {
        return itemId == 5200009 || itemId == 5200010;
    }

    public static boolean isRandomMesoBagItem(int itemId) {
        return itemId / 1000 == 5202;
    }

    public static boolean isChalkboardtem(int itemId) {
        return getItemPrefix(itemId) == 537;
    }

    public static boolean isViciousHammerItem(int itemId) {
        return getItemPrefix(itemId) == 557;
    }

    public static boolean isItemTagItem(int itemId) {
        return itemId == 5060000;
    }

    public static boolean isItemGuardItem(int itemId) {
        return itemId == 5060001 || itemId == 5061000 || itemId == 5061001 || itemId == 5061002 || itemId == 5061003;
    }

    public static boolean isIncCharacterSlotItem(int itemId) { // Cash Shop BUY
        return itemId == 5430000;
    }

    public static boolean isKarmaItem(int itemId) {
        return itemId == 5520000 || itemId == 5520001 || itemId == 2720000 || itemId == 2720001;
    }

    public static boolean isLockedCashConsumeItem(int itemId) {
        return itemId >= 5060002 && itemId <= 5060047 // Incubator
                || itemId >= 5431000 && itemId <= 5433000 // Create Character instantly
                || getItemPrefix(itemId) == 550 // Extend item expiration
                ;
    }

    public static boolean isPendantSlotItem(int itemId) { // Cash Shop BUY
        return getItemPrefix(itemId) == 555;
    }

    public static boolean isTitleItem(int itemId) { // Cash Shop BUY
        return getItemPrefix(itemId) == 370;
    }

    public static boolean isRateModifiedCashItem(int itemId) {
        return itemId / 1000 == 5211 || getItemPrefix(itemId) == 536;
    }

    public static boolean isUpgradeAssistScroll(int itemId) {
        return itemId / 1000 == 5063 || itemId / 1000 == 5064 || itemId / 1000 == 5068;
    }

    public static boolean isPetNameTag(int itemId) {
        return itemId / 10000 == 517;
    }

    public static boolean isPetFood(int itemId) {
        return itemId / 10000 == 524;
    }

    public static boolean isBonusPotentialScroll(int itemId) {
        // TMS 282: bonus potential scrolls named by the 282 data (2048339, 204973x, 204977x, 20499xx, 26441xx).
        if (net.swordie.ms.client.character.items.Tms282EnhanceItems.active()
                && net.swordie.ms.client.character.items.Tms282EnhanceItems.isBonusPotentialScroll(itemId)) {
            return true;
        }
        return (itemId >= 2048305 && itemId <= 2048316 && itemId != 2048312)
                || (itemId >= 2048325 && itemId <= 2048329)
                || itemId == 2048331 || itemId == 2048334;
    }

    public static boolean isAdditionalSlotExtendItem(int itemId) {
        return itemId / 100 == 20482 || itemId >= 2048300 && itemId <= 2048304;
    }

    public static boolean isBridleItem(int itemId) {
        return itemId / 10000 == 227;
    }

    public static boolean isSpecialConsumeEffectItem(int itemId) {
        return itemId / 1000 == 5281;
    }

    public static boolean isMaplePointsCouponItem(int itemId) {
        return itemId / 1000 == 5205;
    }

    public static boolean isPortableChair(int itemId) {
        return itemId / 10000 == 301;
    }

    public static double getBonusExpByItem(int itemID) {
        switch (itemID) {
            case 1112613:
                return 0.02D;
            case 1142198:
                return 0.05D;
            case 1142199:
                return 0.06D;
            case 1142200:
                return 0.07D;
            case 1142201:
                return 0.08D;
            case 1142202:
            case 1142992:
                return 0.1D;
            case 1142978:
                return 0.2D;
            case 1112724:
            case 1112728:
            case 1112741:
            case 1122017:
            case 1122155:
            case 1122156:
            case 1122172:
            case 1122207:
            case 1122215:
            case 1122271:
            case 1122307:
            case 1122313:
            case 1122316:
            case 1122323:
            case 1122324:
                return 0.3D;
            case 1122108:
            case 1122158:
                return 0.15D;
        }
        return 0.0D;
    }

    public static boolean isCoreItem(int itemId) {
        return itemId / 10000 == 360;
    }

    public static boolean isAliciaRingBox(int itemId) {
        return itemId >= 2028263 && itemId <= 2028272;
    }

    public static Integer[] AliciaRingBox_2ndRank = {
            1113098, 1113113, 1113114, 1113115, 1113116, 1113099, 1113122, 1113108, 1113104, 1113125, 1113128, 1113121,
            1113118, 1113109, 1113110, 1113111, 1113112, 1113101, 1113105, 1113100, 1113103, 1113127, 1113119,
            1113102, 1113123, 1113124, 1113106, 1113107, 1113126, 1113117, 1113120, 2432468, 2432503, 2023380
    };

    public static Integer[] AliciaRingBox_6thRank = {
            2432468, 2432503, 4001832
    };

    public static boolean isSealedBox(int itemId) {
        return itemId == 2028154 || itemId == 2028155 || itemId == 2028156 || (itemId >= 2028161 && itemId <= 2028165);
    }

    public static Integer[] sealedBox_Hat = {
            1003154, 1003155, 1003156, 1003157, 1003158, 1003290, 1003291, 1003292, 1003293, 1003294, 1003780, 1003781,
            1003863, 1003947, 1004214, 1004215, 1004216, 1004217, 1004218, 1004234, 1004235, 1004236, 1004237, 1004238,
            1004404, 1004492
    };

    public static Integer[] sealedBox_Overall_Top_Bottom = {
            1050253, 1051309, 1052299, 1052300, 1052301, 1052302, 1052303, 1052384, 1052385, 1052386, 1052387, 1052388,
            1052588, 1052589, 1052612, 1052784, 1052785, 1052786, 1052787, 1052788, 1052804, 1052805, 1052806, 1052807,
            1052808, 1052893, 1052929
    };

    public static Integer[] sealedBox_Shoes_Gloves = {
            1082285, 1082286, 1082287, 1082288, 1082289, 1082338, 1082339, 1082340, 1082341, 1082342, 1082593, 1082594,
            1082595, 1082596, 1082597, 1082613, 1082614, 1082615, 1082616, 1082617, 1082647, 1072471, 1072472, 1072473,
            1072474, 1072475, 1072554, 1072555, 1072556, 1072557, 1072558, 1072776, 1072952, 1072953, 1072954, 1072955,
            1072956, 1072972, 1072973, 1072974, 1072975, 1072976, 1073057
    };

    public static Integer[] sealedBox_Weapon = {
            1212015, 1212016, 1212035, 1212066, 1212073, 1212092, 1212095, 1212098, 1212102, 1212116, 1212117, 1222015,
            1222016, 1222035, 1222061, 1222068, 1222086, 1222089, 1222092, 1222096, 1222110, 1222111, 1232015, 1232016,
            1232035, 1232060, 1232065, 1232086, 1232089, 1232092, 1232096, 1232110, 1232111, 1242015, 1242016, 1242035,
            1242047, 1242051, 1242065, 1242092, 1242095, 1242099, 1242103, 1242117, 1242118, 1252016, 1252017, 1252020,
            1252064, 1252083, 1252087, 1252092, 1252095, 1252097, 1262012, 1262013, 1262014, 1262027, 1262037, 1302149,
            1302175, 1302193, 1302213, 1302277, 1302280, 1302292, 1302301, 1302304, 1302312, 1302316, 1302334, 1302336,
            1312095, 1312096, 1312099, 1312112, 1312155, 1312158, 1312168, 1312176, 1312179, 1312182, 1312186, 1312200,
            1312201, 1322135, 1322136, 1322139, 1322151, 1322205, 1322208, 1322218, 1322228, 1322230, 1322233, 1322237,
            1322251, 1322253, 1332126, 1332152, 1332170, 1332187, 1332188, 1332227, 1332230, 1332241, 1332251, 1332254,
            1332257, 1332261, 1332275, 1332277, 1362020, 1362021, 1362061, 1362092, 1362095, 1362112, 1362115, 1362118,
            1362122, 1362136, 1362137, 1372080, 1372102, 1372119, 1372132, 1372179, 1372182, 1372191, 1372198, 1372201,
            1372204, 1372208, 1372223, 1372225, 1382102, 1382126, 1382145, 1382159, 1382211, 1382214, 1382225, 1382236,
            1382239, 1382242, 1382246, 1382260, 1382263, 1432084, 1432101, 1432119, 1432133, 1432169, 1432172, 1432181,
            1432191, 1432194, 1432197, 1432201, 1432215, 1432216, 1442113, 1442138, 1442156, 1442171, 1442225, 1442228,
            1442237, 1442245, 1442248, 1442251, 1442255, 1442269, 1442270, 1472118, 1472143, 1472161, 1472175, 1472216,
            1472219, 1472229, 1472238, 1472241, 1472244, 1472248, 1472262, 1472263, 1482080, 1482104, 1482122, 1482136,
            1482170, 1482173, 1482182, 1482193, 1482196, 1482199, 1482203, 1482217, 1482218, 1492081, 1492103, 1492122,
            1492136, 1492181, 1492184, 1492193, 1492202, 1492205, 1492209, 1492213, 1492232, 1492233, 1522019, 1522022,
            1522056, 1522067, 1522096, 1522099, 1522115, 1522118, 1522121, 1522125, 1522139, 1522140, 1532019, 1532039,
            1532060, 1532071, 1532100, 1532121, 1532124, 1532127, 1532131, 1532145, 1532146, 1542014, 1542040, 1542080,
            1542098, 1542102, 1542107, 1552014, 1552040, 1552080, 1552099, 1552103, 1552109, 1582012, 1582013, 1582014,
            1582021
    };

    public static Integer[] sealedBox_Hat_Chaos = {
            1003154, 1003155, 1003156, 1003157, 1003158, 1003172, 1003173, 1003174, 1003175, 1003176, 1003290, 1003291,
            1003292, 1003293, 1003294, 1003621, 1003622, 1003780, 1003781, 1003797, 1003798, 1003799, 1003800, 1003801,
            1003841, 1003863, 1003928, 1003929, 1003930, 1003947, 1004075, 1004214, 1004215, 1004216, 1004217, 1004218,
            1004234, 1004235, 1004236, 1004237, 1004238, 1004404, 1004492, 1004637
    };

    public static Integer[] sealedBox_Overall_Top_Bottom_Chaos = {
            1042254, 1042255, 1042256, 1042257, 1042258, 1050253, 1051309, 1052299, 1052300, 1052301, 1052302, 1052303,
            1052314, 1052315, 1052316, 1052317, 1052318, 1052384, 1052385, 1052386, 1052387, 1052388, 1052526, 1052527,
            1052588, 1052589, 1052608, 1052609, 1052612, 1052640, 1052641, 1052642, 1052784, 1052785, 1052786, 1052787,
            1052788, 1052804, 1052805, 1052806, 1052807, 1052808, 1052893, 1052929, 1062165, 1062166, 1062167, 1062168,
            1062169
    };

    public static Integer[] sealedBox_Shoes_Gloves_Chaos = {
            1082285, 1082286, 1082287, 1082288, 1082289, 1082295, 1082296, 1082297, 1082298, 1082299, 1082338, 1082339,
            1082340, 1082341, 1082342, 1082522, 1082537, 1082543, 1082544, 1082545, 1082546, 1082547, 1082593, 1082594,
            1082595, 1082596, 1082597, 1082613, 1082614, 1082615, 1082616, 1082617, 1082647, 1072471, 1072472, 1072473,
            1072474, 1072475, 1072485, 1072486, 1072487, 1072488, 1072489, 1072554, 1072555, 1072556, 1072557, 1072558,
            1072743, 1072744, 1072745, 1072746, 1072747, 1072776, 1072815, 1072845, 1072952, 1072953, 1072954, 1072955,
            1072956, 1072972, 1072973, 1072974, 1072975, 1072976, 1073057
    };

    public static Integer[] sealedBox_Weapon_Chaos = {
            1212014, 1212015, 1212016, 1212035, 1212044, 1212063, 1212065, 1212066, 1212073, 1212074, 1212075, 1212076,
            1212077, 1212092, 1212093, 1212095, 1212098, 1212102, 1212116, 1212117, 1222014, 1222015, 1222016, 1222035,
            1222044, 1222058, 1222060, 1222061, 1222068, 1222069, 1222070, 1222071, 1222072, 1222086, 1222087, 1222089,
            1222092, 1222096, 1222110, 1222111, 1232014, 1232015, 1232016, 1232035, 1232041, 1232057, 1232058, 1232060,
            1232065, 1232066, 1232067, 1232068, 1232071, 1232086, 1232087, 1232089, 1232092, 1232096, 1232110, 1232111,
            1242014, 1242015, 1242016, 1242035, 1242042, 1242044, 1242047, 1242051, 1242060, 1242061, 1242062, 1242063,
            1242065, 1242071, 1242072, 1242073, 1242076, 1242092, 1242093, 1242095, 1242099, 1242103, 1242117, 1242118,
            1252014, 1252015, 1252016, 1252017, 1252020, 1252031, 1252058, 1252060, 1252064, 1252083, 1252087, 1252092,
            1252095, 1252097, 1262012, 1262013, 1262014, 1262015, 1262016, 1262027, 1262035, 1262037, 1302149, 1302152,
            1302175, 1302193, 1302213, 1302248, 1302275, 1302276, 1302277, 1302280, 1302281, 1302282, 1302283, 1302285,
            1302292, 1302301, 1302302, 1302304, 1302312, 1302316, 1302334, 1302336, 1312065, 1312095, 1312096, 1312099,
            1312112, 1312135, 1312153, 1312154, 1312155, 1312158, 1312159, 1312160, 1312161, 1312162, 1312168, 1312176,
            1312177, 1312179, 1312182, 1312186, 1312200, 1312201, 1322096, 1322135, 1322136, 1322139, 1322151, 1322181,
            1322203, 1322204, 1322205, 1322208, 1322210, 1322211, 1322212, 1322213, 1322218, 1322228, 1322229, 1322230,
            1322233, 1322237, 1322251, 1322253, 1332126, 1332130, 1332152, 1332170, 1332187, 1332188, 1332205, 1332225,
            1332226, 1332227, 1332230, 1332232, 1332233, 1332234, 1332235, 1332241, 1332251, 1332252, 1332254, 1332257,
            1332261, 1332275, 1332277, 1362019, 1362020, 1362021, 1362061, 1362074, 1362090, 1362091, 1362092, 1362095,
            1362096, 1362097, 1362098, 1362099, 1362112, 1362113, 1362115, 1362118, 1362122, 1362136, 1362137, 1372080,
            1372084, 1372102, 1372119, 1372132, 1372161, 1372177, 1372178, 1372179, 1372182, 1372183, 1372184, 1372185,
            1372186, 1372191, 1372198, 1372199, 1372201, 1372204, 1372208, 1372223, 1372225, 1382102, 1382104, 1382126,
            1382145, 1382159, 1382192, 1382208, 1382209, 1382211, 1382214, 1382217, 1382218, 1382219, 1382220, 1382225,
            1382234, 1382235, 1382236, 1382237, 1382239, 1382242, 1382246, 1382260, 1382263, 1432084, 1432086, 1432101,
            1432119, 1432133, 1432150, 1432167, 1432168, 1432169, 1432172, 1432173, 1432174, 1432175, 1432176, 1432181,
            1432191, 1432192, 1432194, 1432197, 1432201, 1432215, 1432216, 1442113, 1442116, 1442138, 1442156, 1442171,
            1442202, 1442223, 1442224, 1442225, 1442228, 1442229, 1442230, 1442231, 1442232, 1442237, 1442245, 1442246,
            1442248, 1442251, 1442255, 1442269, 1442270, 1472118, 1472122, 1472143, 1472161, 1472175, 1472197, 1472214,
            1472215, 1472216, 1472219, 1472220, 1472221, 1472222, 1472223, 1472229, 1472238, 1472239, 1472241, 1472244,
            1472248, 1472262, 1472263, 1482080, 1482084, 1482104, 1482122, 1482136, 1482151, 1482168, 1482169, 1482170,
            1482173, 1482174, 1482175, 1482176, 1482177, 1482182, 1482193, 1482194, 1482196, 1482199, 1482203, 1482217,
            1482218, 1492081, 1492085, 1492103, 1492122, 1492136, 1492162, 1492179, 1492180, 1492181, 1492184, 1492185,
            1492186, 1492187, 1492188, 1492193, 1492202, 1492203, 1492205, 1492209, 1492213, 1492232, 1492233, 1522018,
            1522019, 1522022, 1522056, 1522067, 1522078, 1522094, 1522095, 1522096, 1522099, 1522100, 1522101, 1522102,
            1522103, 1522115, 1522116, 1522118, 1522121, 1522125, 1522139, 1522140, 1532018, 1532019, 1532039, 1532060,
            1532071, 1532081, 1532098, 1532099, 1532100, 1532103, 1532104, 1532105, 1532106, 1532121, 1532122, 1532124,
            1532127, 1532131, 1532145, 1532146, 1542014, 1542040, 1542063, 1542071, 1542075, 1542077, 1542078, 1542079,
            1542080, 1542081, 1542098, 1542102, 1542107, 1552014, 1552040, 1552063, 1552071, 1552075, 1552077, 1552078,
            1552079, 1552080, 1552081, 1552099, 1552103, 1552109, 1582012, 1582013, 1582014, 1582015, 1582016, 1582021,
            1582022
    };

    public static boolean isMesoPouch(int itemId) {
        return itemId == 2028048 || itemId == 2028330 || itemId == 2028333 || itemId == 2028335 || itemId == 2028349;
    }

    public static long getMesoCubingCost(int level) {
        double value = 0;
        if (level >= 31 && level <= 70) {
            value = 0.5;
        } else if (level <= 120) {
            value = 2.5;
        } else {
            value = 20;
        }
        return (long) (level * level * value);
    }

    public static boolean isCleanStateScroll(int scrollID) {
        return (scrollID / 100) == 20490 && scrollID != 2049023;
    }

    public static boolean isInnocenceScroll(int scrollID) {
        // TMS 282: any scroll whose 282 info has reset / arcReset (2049622/25/27/28/30 too).
        if (net.swordie.ms.client.character.items.Tms282EnhanceItems.active()
                && net.swordie.ms.client.character.items.Tms282EnhanceItems.resetKind(scrollID)
                != net.swordie.ms.client.character.items.Tms282EnhanceItems.ResetKind.NONE) {
            return true;
        }
        return scrollID >= 2049600 && scrollID <= 2049618;
    }

    public static final Integer[] SSB_Common = {
            // hat
            1005430, 1005431, 1005432, 1005433, 1005434, 1005435, 1005436, 1005437, 1005438, 1005439,
            // longcoat
            1053403, 1053405, 1053406, 1053399, 1053400,
    };

    public static final Integer[] SSB_Rare = {
            // hat
            1005440, 1005441, 1005442, 1005443, 1005444, 1005445, 1005446, 1005447, 1005448, 1005449,
            // longcoat
            1053423, 1053424, 1053434, 1053435, 1053435, 1053437,
            // coat
            1042312, 1042313,
            // pants
            1062214,
            // cape
            1103232, 1103225, 1103226, 1103216, 1103245, 1103246, 1103248, 1103248, 1103252,
            // ring
            1115176, 1115177, 1115175, 1115163, 1115164, 1115165, 1115166, 1115167,
            // pet
            5002036, 5002037, 5002038, 5002117, 5002118, 5002119, 5002130, 5002131, 5002132, 5002137, 5002138, 5002139, 5000982, 5000983, 5000984, 5000985, 5000986, 5000836
    };

    public static Integer getSSB_Common_Reward() {
        return Util.getRandomFromCollection(SSB_Common);
    }

    public static List<Integer> getSSB_Common_Rewards() {
        return Arrays.stream(SSB_Common).toList();
    }

    public static Integer getSSB_Rare_Reward() {
        return Util.getRandomFromCollection(SSB_Rare);
    }

    public static boolean isJokerSetItem(int itemID) {
        return itemID == 1003719 || itemID == 1003720 || itemID == 1003721 || itemID == 1003722;
    }

    public static boolean isGuildRewardBox(int itemId) {
        return itemId >= 2028342 && itemId <= 2028346 || itemId == 2028338;
    }

    public static Integer[] guildReward_TierS = {
            5680408, 5530623, 5530625, 5530626,
            5062801, 5680478, 5530557, 5530246,
            5530341, 2435669, 2435696, 1
    };

    public static Integer[] guildReward_TierA = {
            5680407, 5530556, 5530557, 5530005,
            5530008, 5530246, 5530341, 5530342,
            5530622, 5062024, 5062500, 5680406,
            5680330
    };

    public static Integer[] guildReward_TierB = {
            5530005, 5530008, 5530246, 5530341,
            5530342, 5530622, 5062024, 5062500,
            5680406, 5680330
    };

    public static Integer[] guildReward_TierC = {
            2023604, 2023605, 2023128, 2049122,
            2049507, 2048201, 2049607, 2049334,
            5530004, 5530006, 5530666, 5680405,
    };

    public static Integer[] guildReward_TierD_F = {
            2000002, 2000006, 2000045, 2012018,
            2022282, 2022283, 2023125, 2023126,
            2023127, 5530538, 5530445,
            SPELL_TRACE_ID
    };


    public static int getNewYearBox() {
        if (Util.succeedProp(20)) {
            int itemID = Util.getRandom(1004881, 1006139);
            if (ItemData.getItemDeepCopy(itemID) != null) {
                return itemID;
            } else {
                return getNewYearBox();
            }
        } else if (Util.succeedProp(20)) {
            int itemID = Util.getRandom(1053120, 1053941);
            if (ItemData.getItemDeepCopy(itemID) != null) {
                return itemID;
            } else {
                return getNewYearBox();
            }
        } else if (Util.succeedProp(20)) {
            int itemID = Util.getRandom(1073185, 1073691);
            if (ItemData.getItemDeepCopy(itemID) != null) {
                return itemID;
            } else {
                return getNewYearBox();
            }
        } else if (Util.succeedProp(20)) {
            int itemID = Util.getRandom(1082717, 1082775);
            if (ItemData.getItemDeepCopy(itemID) != null) {
                return itemID;
            } else {
                return getNewYearBox();
            }
        } else if (Util.succeedProp(20)) {
            int itemID = Util.getRandom(1102980, 1103548);
            if (ItemData.getItemDeepCopy(itemID) != null) {
                return itemID;
            } else {
                return getNewYearBox();
            }
        } else {
            return getNewYearBox();
        }
    }

    public static boolean isTwoHanded(int itemId) {
        switch (Objects.requireNonNull(getWeaponType(itemId))) {
            case TwoHandedSword:
            case TwoHandedAxe:
            case TwoHandedMace:
            case Spear:
            case Polearm:
            case BigSword:
            case LongSword:
            case ArmCannon:
            case Bow:
            case Crossbow:
            case DualBowgun:
            case Claw:
            case Gun:
            case Knuckle:
            case HandCannon:
            case AncientBow: {
                return true;
            }
            default: {
                return false;
            }
        }
    }
}

