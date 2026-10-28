package net.swordie.ms.constants;

import net.swordie.ms.util.Rect;

import java.util.List;

public class FieldConstants {

    public static final int START_MAP = 993166000;
    public static final int HOME_MAP = 100000000; // Henesys
    public static final int TRAINING_MAP_1 = 910000001; // Free Market Room 1
    public static final int TRAINING_MAP_6 = 910000006; // Free Market Room 6
    public static final int TRAINING_MAP_12 = 910000012; // Free Market Room 12

    public static final int NO_MAP_ID = 999999999;
    public static final int HENESYS_ID = 100000000;
    public static final int VIDEO_FIELD = 931050990; // Used for Effects and/or Videos
    public static final int ARDENTMILL = 910001000;
    public static final int FOREST_OF_TENACITY = 993001000;
    public static final int MAPLE_GALAXY = 993016000;
    public static final int PLANET_GLACIUS = 993014000;
    public static final int SUB_ZERO_HUNT = 993014200;
    public static final int DEFAULT_FIELD_MOB_CAPACITY = 25;
    public static final double DEFAULT_FIELD_MOB_RATE_BY_MOBGEN_COUNT = 1.5;
    public static final int BASE_MOB_RESPAWN_RATE = 5000; // In milliseconds
    public static final double KISHIN_MOB_MULTIPLIER = 1.7;
    public static final double KISHIN_MOB_RATE_MULTIPLIER = 1.7;
    public static final double TOTEM_MOB_MULTIPLIER_VIP = 2.0;
    public static final double TOTEM_MOB_MULTIPLIER_NORMAL = 1.5;

    public static final Rect MOB_CHECK_RECT = new Rect(-100, -100, 100, 100);

    // Rune
    public static final int RUNE_RESPAWN_TIME = 5; // minutes
    public static final int RUNE_COOLDOWN_TIME = 4; // minutes
    public static final int THUNDER_RUNE_ATTACK_DELAY = 4; // seconds
    public static final int[] BLOCKED_RUNE_MAPS = { // map ids where runes will not be spawned
            //689013000,  // pink zakum battle map
    };

    public static final int EVENT_HALL = 820000000;

    public static final int MC_BATTLE_FIELD_LOBBY_1 = 980000500;
    public static final int MC_BATTLE_FIELD_BLUE_1 = 980000501;
    public static final int MC_BATTLE_FIELD_RED_1 = 980000601;

    public static final int MC_BATTLE_FIELD_LOBBY_2 = 980000600; // solo
    public static final int MC_BATTLE_FIELD_BLUE_2 = 980001501;
    public static final int MC_BATTLE_FIELD_RED_2 = 980001601;

    // BurningField
    public static final int BURNING_FIELD_MAX_LEVEL = 10; //Maximum Burning Field Level
    public static final int BURNING_FIELD_LEVEL_ON_START = 1; //Starts Burning Maps at BurningLevel 10
    public static final int BURNING_FIELD_TIMER = 10; // minutes
    public static final int BURNING_FIELD_MIN_MOB_LEVEL = 0; //Minimum Mob Level for the Field to become a Burning Field
    public static final int BURNING_FIELD_BONUS_EXP_MULTIPLIER_PER_LEVEL = 10; // multiplied by the BurningField Level  =  Bonus Exp% given

    public static final List<Integer> BOSS_BATTLE_MAP = List.of(
            280030200, //Easy Zakum
            280030100, //Normal Zakum
            280030000, //Chaos Zakum

            272020200, //Easy Arkarium
            272020210, //Normal Arkarium

//            271041100, //Easy Cygnus
//            271040100, //Normal Cygnus

            807300110, //Easy Ranmaru
            807300210, //Normal Ranmaru

            401060300, //Easy Magnus
            401060200, //Normal Magnus
            401060100, //Hard Magnus
            401060100, //Hard Magnus

            105200310, //Normal Queen
            105200710, //Chaos Queen

            105200210, //Normal Pierre
            105200610, //Chaos Pierre

            105200110, //Normal Von Bon
            105200510, //Chaos Von Bon

            105200410, //Normal Vellum
            105200810, //Chaos Vellum

            211070100, //Easy Von Leon
            211070102, //Normal Von Leon
            211070104, //Chaos Von Leon

            350060700, //Normal Lotus
            350060800, //Normal Lotus
            350060900, //Normal Lotus
            350060400, //Hard Lotus
            350060500,//Hard Lotus
            350060600 //Hard Lotus
    );
    // Npc
    public static final int[] BLOCKED_SPAWNED_NPCS = {
            9072200, // Clara
            9072201, // Jimmy-Jack
            1022101, // Rooney
            9001058, // Rice Cake Mommy Moon Bunny
            9201151, // Donald
            9020000, // Lakelis
            2120028, // Halloween Kaleido-Spinner
            2001007, // Christmas Tree
            9000154, // Christmas Tree
            2120025, // Spin Man
            9000291, // Manager Mush
            9001040, // Manager Mush
            9001042, // Manager Mush
            9001043, // Manager Mush
            9001044, // Manager Mush
            9001045, // Manager Mush
            9400025, // Lil' Murgoth
            2101014, // Cesar
            2042010, // Spiegelmann
            9000006, // Vikoon
            9000047, // Fairytale Jack
            9000048, // Fairytale Pinnochio
            9000049, // Fairytale Crackers
            9000453, // Zakum Crush
            2091010, // Mu Lung Ranking
            2159018, // Von
            2159019, // Iceman
            9201391, // Temporal Wormhole
            9201476, // Maple Forest Ranger
            9310601, // Fortune Teller of Henesys
            9310602, // Fortune Teller of Edelsten
            9310603, // Fortune Teller of The Far Past
            9310604, // Fortune Teller of Rien
            9000383, // Yeti
            9000384, // Pepe
            9201142, // Witch Lady
            9010010, // Cassandra
            9201595, // Spiegelmann
            9001127, // Spiegelmann
            9001128, // Spiegelette
            9201029, // Grandma Benson
            9330601, // Mr. Bebe
            9330602, // Mr. Bebe LED
            9200001, // Mad Bunny
            9000131, // Magical Mortar Bunny
            9000132, // Wee Moon Bunny
            9001047, // Brawns
            9201290, // Mr. Lee
            9201356, // Game Machine
            9201357, // Reward Machine
            9001120, // Gamsper
            9200000, // Cody
            9001121, // Katherine
            9001121, // Katherine
            9000021, // Gaga
            9000133, // Homecoming Wonky
            2041017, // Ace of Hearts
            9000017, // Coco

            // Red Leaf High:
            9330189, // Schrodinger
            9201269, // Boss Kumi
    };

    public static List<Integer> swimMaps = List.of(
            450002011,
            450002012,
            450002013,
            450002014,
            450002015,
            450002021,
            450002200,
            450002201,
            450002301,
            921170050,
            921170100,
            993000868,
            993000869,
            993000870,
            993000871,
            993000872,
            993000873,
            993000874,
            993000875,
            993000877
    );

    public static boolean isVonbonField(int fieldID) {
        return fieldID >= 105200110 && fieldID < 105200120 || fieldID >= 105200510 && fieldID < 105200520;
    }
}
