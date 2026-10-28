package net.swordie.ms.constants;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.enums.UnionStatType;
import net.swordie.ms.util.Position;

import java.util.ArrayList;
import java.util.List;

public class UnionConstants {

    public static boolean isEligibleForUnion(Char chr) {
        return chr.getLevel() > 60 && chr.getJob() % 100 >= 1;
    }

    public static double getUnionMultiplier(short level) {
        double multiplier = 0.5; // default for 60~99
        if (level >= 100 && level <= 139) {
            multiplier = 0.4;
        } else if (level >= 140 && level <= 179) {
            multiplier = 0.7;
        } else if (level >= 180 && level <= 199) {
            multiplier = 0.8;
        } else if (level >= 200 && level <= 209) {
            multiplier = 1.0;
        } else if (level >= 210 && level <= 219) {
            multiplier = 1.1;
        } else if (level >= 220 && level <= 229) {
            multiplier = 1.15;
        } else if (level >= 230 && level <= 239) {
            multiplier = 1.2;
        } else if (level >= 240 && level <= 250) {
            multiplier = 1.25;
        }
        return multiplier;
    }

    public static UnionChucMultiplier getUnionChucMultiplier(int chuc) {
        // values for chuc 0~59
        double firstMulti = 0.10;
        double secondMulti = 15.0;
        double thirdMulti = 750.0;
        double fourthMulti = 0.0;
        if (chuc >= 60 && chuc <= 119) {
            firstMulti = 0.11;
            secondMulti = 16.5;
            thirdMulti = 825.0;
            fourthMulti = 1250.0;
        } else if (chuc >= 120 && chuc <= 179) {
            firstMulti = 0.12;
            secondMulti = 18.0;
            thirdMulti = 900.0;
            fourthMulti = 2500.0;
        } else if (chuc >= 180 && chuc <= 229) {
            firstMulti = 0.13;
            secondMulti = 19.5;
            thirdMulti = 975.0;
            fourthMulti = 3750.0;
        } else if (chuc >= 230 && chuc <= 259) {
            firstMulti = 0.14;
            secondMulti = 21.0;
            thirdMulti = 1050.0;
            fourthMulti = 5000.0;
        } else if (chuc >= 260 && chuc <= 289) {
            firstMulti = 0.15;
            secondMulti = 22.5;
            thirdMulti = 1125.0;
            fourthMulti = 6250.0;
        } else if (chuc >= 290 && chuc <= 319) {
            firstMulti = 0.16;
            secondMulti = 24.0;
            thirdMulti = 1200.0;
            fourthMulti = 7500.0;
        } else if (chuc >= 320 && chuc <= 349) {
            firstMulti = 0.17;
            secondMulti = 25.5;
            thirdMulti = 1275.0;
            fourthMulti = 8750.0;
        } else if (chuc >= 350) {
            firstMulti = 0.18;
            secondMulti = 27.0;
            thirdMulti = 1350.0;
            fourthMulti = 10000.0;
        }
        return new UnionChucMultiplier(firstMulti, secondMulti, thirdMulti, fourthMulti);
    }

    public static class UnionChucMultiplier {
        public double firstMulti, secondMulti, thirdMulti, fourthMulti;

        public UnionChucMultiplier(double firstMulti, double secondMulti, double thirdMulti, double fourthMulti) {
            this.firstMulti = firstMulti;
            this.secondMulti = secondMulti;
            this.thirdMulti = thirdMulti;
            this.fourthMulti = fourthMulti;
        }
    }

    public static final int UNION_WIDTH = 22;
    public static final int UNION_HEIGHT = 20;
    public static final int[][] UnionTable = new int[UNION_HEIGHT][UNION_WIDTH];

    public static final int[] unionLevel = new int[]{
            101, 102, 103, 104, 105,
            201, 202, 203, 204, 205,
            301, 302, 303, 304, 305,
            401, 402, 403, 404, 405
    };

    public static final List<Integer> l0 = List.of(
            116, 117, 118, 119, 120,
            139, 140, 141, 142,
            162, 163, 164,
            185, 186,
            208
    );


    public static final List<Integer> l1 = List.of(
            121, 122, 123, 124, 125,
            143, 144, 145, 146,
            165, 166, 167,
            187, 188,
            209
    );

    public static final List<Integer> l2 = List.of(
            126,
            147, 148,
            168, 169, 170,
            189, 190, 191, 192,
            210, 211, 212, 213, 214
    );

    public static final List<Integer> l3 = List.of(
            232, 233, 234, 235, 236,
            255, 256, 257, 258,
            278, 279, 280,
            301, 302,
            324
    );

    public static final List<Integer> l4 = List.of(
            231,
            253, 254,
            275, 276, 277,
            297, 298, 299, 300,
            319, 320, 321, 322, 323
    );

    public static final List<Integer> l5 = List.of(
            230,
            251, 252,
            272, 273, 274,
            293, 294, 295, 296,
            314, 315, 316, 317, 318
    );

    public static final List<Integer> l6 = List.of(
            225, 226, 227, 228, 229,
            247, 248, 249, 250,
            269, 270, 271,
            291, 292,
            313
    );

    public static final List<Integer> l7 = List.of(
            115,
            137, 138,
            158, 160, 161,
            181, 182, 183, 184,
            203, 204, 205, 206, 207
    );

    public static final List<Integer> lAbnormalStatusResistance = List.of(
            1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
            24, 25, 26, 27, 28, 29, 30, 31, 32,
            47, 48, 49, 50, 51, 52, 53, 54,
            70, 71, 72, 73, 74, 75, 76,
            93, 94, 95, 96, 97, 98
    );

    public static final List<Integer> lBonusExp = List.of(
            11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
            33, 34, 35, 36, 37, 38, 39, 40, 41,
            55, 56, 57, 58, 59, 60, 61, 62,
            77, 78, 79, 80, 81, 82, 83,
            99, 100, 101, 102, 103, 104
    );

    public static final List<Integer> lCriticalRate = List.of(
            21,
            42, 43,
            63, 64, 65,
            84, 85, 86, 87,
            105, 106, 107, 108, 109,
            127, 128, 129, 130, 131,
            149, 150, 151, 152, 153,
            171, 172, 173, 174, 175,
            193, 194, 195, 196, 197,
            215, 216, 217, 218, 219
    );

    public static final List<Integer> lBossDamage = List.of(
            237, 238, 239, 240, 241,
            259, 260, 261, 262, 263,
            281, 282, 283, 284, 285,
            303, 304, 305, 306, 307,
            325, 326, 327, 328, 329,
            347, 348, 349, 350, 351,
            370, 371, 372, 373,
            393, 394, 395,
            416, 417,
            439
    );

    public static final List<Integer> lKnockbackResistance = List.of(
            341, 342, 343, 344, 345, 346,
            363, 364, 365, 366, 367, 368, 369,
            385, 386, 387, 388, 389, 390, 391, 392,
            407, 408, 409, 410, 411, 412, 413, 414, 415,
            429, 430, 431, 432, 433, 434, 435, 436, 437, 438
    );

    public static final List<Integer> lBuffDuration = List.of(
            335, 336, 337, 338, 339, 340,
            356, 357, 358, 359, 360, 361, 362,
            377, 378, 379, 380, 381, 382, 383, 384,
            398, 399, 400, 401, 402, 403, 404, 405, 406,
            419, 420, 421, 422, 423, 424, 425, 426, 427, 428
    );

    public static final List<Integer> lIgnoreDef = List.of(
            220, 221, 222, 223, 224,
            242, 243, 244, 245, 246,
            264, 265, 266, 267, 268,
            286, 287, 288, 289, 290,
            308, 309, 310, 311, 312,
            330, 331, 332, 333, 334,
            352, 353, 354, 355,
            374, 375, 376,
            396, 397,
            418
    );

    public static final List<Integer> lCriticalDamage = List.of(
            0,
            22, 23,
            44, 45, 46,
            66, 67, 68, 69,
            88, 89, 90, 91, 92,
            110, 111, 112, 113, 114,
            132, 133, 134, 135, 136,
            154, 155, 156, 157, 158,
            176, 177, 178, 179, 180,
            198, 199, 200, 201, 202
    );

    //Job ID Find AoB and ref ida with String v204 with Name.
    public static final int UNION_MAPLE_M = 10010900;

    public static final int UNION_LAB_SERVER = 10010900;

    public static final int UNION_ENHANCE_LAB_SERVER = 10010900;

    //QR
    public static final int UNION_LEVEL_QR_KEY = 18771;

    public static final String UNION_LEVEL_QR_DATA = "rank=%d";
    //500630 presetNo
    //500013 preset

    //Grid Skill
    public static final int UNION_STR_SKILL = 71004000;
    public static final int UNION_DEX_SKILL = 71004001;
    public static final int UNION_INT_SKILL = 71004002;
    public static final int UNION_LUK_SKILL = 71004003;
    public static final int UNION_ATT_SKILL = 71004004;
    public static final int UNION_MATT_SKILL = 71004005;
    public static final int UNION_MAX_HP_SKILL = 71004006;
    public static final int UNION_MAX_MP_SKILL = 71004007;
    public static final int UNION_CRITICAL_DAMAGE_SKILL = 71004008;
    public static final int UNION_ABNORMAL_STATUS_RESISTANCE_SKILL = 71004009;
    public static final int UNION_OBTAINED_EXP_SKILL = 71004010;
    public static final int UNION_CRITICAL_RATE_SKILL = 71004011;
    public static final int UNION_BOSS_DAMAGE_SKILL = 71004012;
    public static final int UNION_KNOCKBACK_RESISTANCE_SKILL = 71004013;
    public static final int UNION_BUFF_DURATION_SKILL = 71004014;
    public static final int UNION_IGNORE_DEFENSE_SKILL = 71004015;
    public static final int LAB_SERVER_UNION_BLOCK_SKILL = 71009002;
    public static final int ENHANCED_LAB_SERVER_UNION_BLOCK_SKILL = 71009003;

    //Char Skill
    //region 10/20/40/80/100 STR
    public static final int EXPLORER_WARRIOR_HERO = 71000011;
    public static final int EXPLORER_WARRIOR_PALADIN = 71000012;
    public static final int NOVA_WARRIOR_KAISER = 71000611;
    public static final int EXPLORER_PIRATE_BUCCANEER_1 = 71000051;
    public static final int EXPLORER_PIRATE_BUCCANEER_2 = 71000058;
    public static final int EXPLORER_PIRATE_CANNONEER = 71000151;
    public static final int CYGNUS_PIRATE_THUNDER_BREAKER = 71000151;
    public static final int HIGH_FLORA_PIRATE_ARK = 71000951;
    //endregion

    //region 10/20/40/80/100 DEX
    public static final int EXPLORER_BOWMAN_BOW_MASTER = 71000031;
    public static final int CYGNUS_BOWMAN_WIND_ARCHER = 71000131;
    public static final int NOVA_PIRATE_ANGELIC_BUSTER = 71000651;
    //endregion

    //region 10/20/40/80/100 INT
    public static final int EXPLORER_MAGICIAN_ARCH_MAGE_IL = 71000022;
    public static final int EXPLORER_MAGICIAN_BISHOP = 71000023;
    public static final int RESISTANCE_MAGICIAN_BATTLE_MAGE = 71000321;
    public static final int HERO_MAGICIAN_LUMINOUS = 71000271;
    public static final int CYGNUS_MAGICIAN_BLAZE_WIZARD = 71000121;
    public static final int NATURAL_GENIUS_KINESIS = 71000921;
    public static final int VERDANT_FLORA_MAGICIAN_ILLIUM = 71000922;
    //endregion

    //region 10/20/40/80/100 LUK
    public static final int EXPLORER_THIEF_SHADOWER = 71000042;
    public static final int EXPLORER_THIEF_DUAL_BLADE = 71000043;
    public static final int CYGNUS_THIEF_NIGHT_WALKER = 71000141;
    public static final int NOVA_THIEF_CADENA = 71000641;
    //endregion

    //region 5/10/20/40/50 STR, DEX, LUK
    public static final int RESISTANCE_HYBRID_XENON = 71000361;
    //endregion

    //region 2/3/4/5/6 %HP
    public static final int EXPLORER_WARRIOR_DARK_KNIGHT = 71000013;
    //endregion

    //region 250/500/1000/2000/2500 HP
    public static final int CYGNUS_DAWN_WARRIOR = 71000111;
    public static final int MIHILE_KNIGHT_OF_LIGHT = 71000511;
    //endregion

    //region 70% proc heal HP rate.
    public static final int HERO_WARRIOR_ARAN = 71000211;
    //endregion

    //region 2/3/4/5/6 %MP
    public static final int EXPLORER_MAGICIAN_ARCH_MAGE_FP = 71000021;
    //endregion

    //region 2/4/6/8/10 70% proc heal rate
    public static final int HERO_MAGICIAN_EVAN = 71000221;
    //endregion

    //region 4/6/8/10/12 %EXP
    public static final int CHILD_OF_THE_GODDESS_ZERO = 71000711;
    //endregion

    //region 5/10/15/20/25 %Buff Duration
    public static final int RESISTANCE_PIRATE_MECHANIC = 71000351;
    //endregion

    //region 2/3/4/5/6 %Cooldown Reduction
    public static final int HERO_BOWMAN_MERCEDES = 71000231;
    //endregion

    //region 1/2/3/4/5 Status Resistance
    public static final int RESISTANCE_WARRIOR_DEMON_SLAYER = 71000311;
    //endregion

    //region 1/2/3/4/5 %Meso Drop
    public static final int HERO_THIEF_PHANTOM = 71000241;
    //endregion

    //region 4/6/8/10/12 %Summon Duration
    public static final int EXPLORER_PIRATE_CORSAIR_1 = 71000052;
    public static final int EXPLORER_PIRATE_CORSAIR_2 = 71000059;
    //endregion

    //region 1/2/3/4/5 %Critical Rate
    public static final int EXPLORER_BOWMAN_MARKSMAN = 71000032;
    public static final int EXPLORER_THIEF_NIGHT_LORD = 71000041;
    //endregion

    //region 1/2/3/5/6 %Critical Damage
    public static final int PIRATE_HERO_SHADE = 71000251;
    public static final int SENGOKU_WARRIOR_HAYATO = 71000411;
    //endregion

    //region 1/2/3/5/6 %Ignore Enemy Defense
    public static final int RESISTANCE_WARRIOR_BLASTER = 71000371;
    public static final int SONG_OF_SPIRITS = 71000821;
    //endregion

    //region 1/2/3/5/6 %Boss Damage
    public static final int RESISTANCE_WARRIOR_DEMON_AVENGER = 71000312;
    public static final int SENGOKU_WARRIOR_KANNA = 71000421;
    //endregion

    //region 4/8/12/16/20 20% Chance increase Damage
    public static final int RESISTANCE_BOWMAN_WILD_HUNTER = 71000331;
    //endregion

    //region Set Legion
    public static final int WARRIORS_FIRST_STEP = 71001100; //3 B-Rank (or higher) Cards
    public static final int WARRIORS_GROWTH = 71001101; //3 A-Rank (or higher) Cards
    public static final int WARRIORS_FEAT = 71001102; //3 S-Rank (or higher) Cards
    public static final int COMPLETED_WARRIOR = 71001103; //3 SS-Rank Cards
    //endregion

    static {
        initLegionTable();
    }

    public static void initLegionTable() {
        int num = 0;
        for (int i = 0; i < UNION_HEIGHT; i++) {
            for (int j = 0; j < UNION_WIDTH; j++) {
                UnionTable[i][j] = num;
                num++;
            }
        }
    }

    public static void printTetrisSlot(List<Integer> tetrisList) {
        for (int data : tetrisList) {
            System.out.println("[Tetris Slot] " + data);
            //System.out.println("tetrisList.add(" + data + ");");
        }
    }

    public static Position getPositionOfSlot(int slot) {
        for (int i = 0; i < UNION_HEIGHT; i++) {
            for (int j = 0; j < UNION_WIDTH; j++) {
                if (UnionTable[i][j] == slot) {
                    return new Position(j, i);
                }
            }
        }
        return new Position();
    }

    public static int handleHorizontalFlip(int originPositionX, int x) {
        if (x == originPositionX) {
            return x;
        } else if (x > originPositionX) {
            return originPositionX - (x - originPositionX);
        } else {
            return originPositionX + (originPositionX - x);
        }
    }

    public static int handleVerticalFlip(int originPositionY, int y) {
        if (y == originPositionY) {
            return y;
        } else if (y > originPositionY) {
            return originPositionY - (y - originPositionY);
        } else {
            return originPositionY + (originPositionY - y);
        }
    }

    public static int getTetrisPartSlot(int bonusX, int bonusY, int slot, int angle, int flip) {
        Position originPosition = getPositionOfSlot(slot);
        if (angle > 0 && (bonusX != 0 || bonusY != 0)) {
            for (int i = 0; i < angle / 90; i++) {
                bonusY = bonusY ^ bonusX;
                bonusX = bonusY ^ bonusX;
                bonusY = -(bonusY ^ bonusX);
            }
        }
        int newX = originPosition.getX() + bonusX;
        int newY = originPosition.getY() + bonusY;
        if (flip == 1) {
            newX = handleHorizontalFlip(originPosition.getX(), newX);
        } else if (flip == 2) {
            newY = handleVerticalFlip(originPosition.getY(), newY);
        } else if (flip == 3) {
            newY = handleVerticalFlip(originPosition.getY(), newY);
            newX = handleHorizontalFlip(originPosition.getX(), newX);
        }
        if (newX >= 0 && newX <= UNION_WIDTH - 1 && newY >= 0 && newY <= UNION_HEIGHT - 1) {
            return UnionTable[newY][newX];
        }

        return -1;
    }

    public static List<Integer> getTetrisSlot(short jobID, int level, int slot, int angle) {
        List<Integer> result = new ArrayList<>();
        int flip = 0;
        if (angle >= 3000) {
            flip = 3;
            angle -= 3000;
        } else if (angle >= 2000) {
            flip = 2;
            angle -= 2000;
        } else if (angle >= 1000) {
            flip = 1;
            angle -= 1000;
        }
        if (JobConstants.isXenon(jobID)) {
            if (level >= 60) {
                result.add(getTetrisPartSlot(0, 0, slot, angle, flip));
            }
            if (level >= 100) {
                result.add(getTetrisPartSlot(1, 0, slot, angle, flip));
            }
            if (level >= 140) {
                result.add(getTetrisPartSlot(-1, 0, slot, angle, flip));
            }
            if (level >= 200) {
                result.add(getTetrisPartSlot(1, 1, slot, angle, flip));
            }
            if (level >= 250) {
                result.add(getTetrisPartSlot(-1, -1, slot, angle, flip));
            }
        } else if (JobConstants.isZero(jobID)) {
            if (level >= 130) {
                result.add(getTetrisPartSlot(0, 0, slot, angle, flip));
            }
            if (level >= 160) {
                result.add(getTetrisPartSlot(1, 0, slot, angle, flip));
            }
            if (level >= 180) {
                result.add(getTetrisPartSlot(0, 1, slot, angle, flip));
            }
            if (level >= 200) {
                result.add(getTetrisPartSlot(1, 1, slot, angle, flip));
            }
            if (level >= 250) {
                result.add(getTetrisPartSlot(1 + 1, 0, slot, angle, flip));
            }
        } else if (JobConstants.isWarriorEquipJob(jobID)) {
            if (level >= 60) {
                result.add(getTetrisPartSlot(0, 0, slot, angle, flip));
            }
            if (level >= 100) {
                result.add(getTetrisPartSlot(1, 0, slot, angle, flip));
            }
            if (level >= 140) {
                result.add(getTetrisPartSlot(0, 1, slot, angle, flip));
            }
            if (level >= 200) {
                result.add(getTetrisPartSlot(1, 1, slot, angle, flip));
            }
            if (level >= 250) {
                result.add(getTetrisPartSlot(1 + 1, 0, slot, angle, flip));
            }
        } else if (JobConstants.isMageEquipJob(jobID)) {
            //Special Point
            if (level >= 250) {
                result.add(getTetrisPartSlot(0, 0, slot, angle, flip));
                result.add(getTetrisPartSlot(1, 0, slot, angle, flip));
                result.add(getTetrisPartSlot(-1, 0, slot, angle, flip));
                result.add(getTetrisPartSlot(0, 1, slot, angle, flip));
                result.add(getTetrisPartSlot(0, -1, slot, angle, flip));
            } else if (level >= 200) {
                result.add(getTetrisPartSlot(0, 0, slot, angle, flip));
                result.add(getTetrisPartSlot(0, -1, slot, angle, flip));
                result.add(getTetrisPartSlot(-1, -1, slot, angle, flip));
                result.add(getTetrisPartSlot(1, -1, slot, angle, flip));
            } else if (level >= 140) {
                result.add(getTetrisPartSlot(0, 0, slot, angle, flip));
                result.add(getTetrisPartSlot(1, 0, slot, angle, flip));
                result.add(getTetrisPartSlot(-1, 0, slot, angle, flip));
            } else if (level >= 100) {
                result.add(getTetrisPartSlot(0, 0, slot, angle, flip));
                result.add(getTetrisPartSlot(1, 0, slot, angle, flip));
            } else if (level >= 60) {
                result.add(getTetrisPartSlot(1 + 1, 0, slot, angle, flip));
            }
        } else if (JobConstants.isArcherEquipJob(jobID)) {
            if (level >= 60) {
                result.add(getTetrisPartSlot(0, 0, slot, angle, flip));
            }
            if (level >= 100) {
                result.add(getTetrisPartSlot(1, 0, slot, angle, flip));
            }
            if (level >= 140) {
                result.add(getTetrisPartSlot(-1, 0, slot, angle, flip));
            }
            if (level >= 200) {
                result.add(getTetrisPartSlot(1 + 1, 0, slot, angle, flip));
            }
            if (level >= 250) {
                result.add(getTetrisPartSlot(-1 - 1, 0, slot, angle, flip));
            }
        } else if (JobConstants.isThiefEquipJob(jobID)) {
            if (level >= 60) {
                result.add(getTetrisPartSlot(0, 0, slot, angle, flip));
            }
            if (level >= 100) {
                result.add(getTetrisPartSlot(1, 0, slot, angle, flip));
            }
            if (level >= 140) {
                result.add(getTetrisPartSlot(-1, 0, slot, angle, flip));
            }
            if (level >= 200) {
                result.add(getTetrisPartSlot(1, 1, slot, angle, flip));
            }
            if (level >= 250) {
                result.add(getTetrisPartSlot(1, -1, slot, angle, flip));
            }
        } else if (JobConstants.isPirateEquipJob(jobID)) {
            if (level >= 60) {
                result.add(getTetrisPartSlot(0, 0, slot, angle, flip));
            }
            if (level >= 100) {
                result.add(getTetrisPartSlot(1, 0, slot, angle, flip));
            }
            if (level >= 140) {
                result.add(getTetrisPartSlot(0, 1, slot, angle, flip));
            }
            if (level >= 200) {
                result.add(getTetrisPartSlot(1, -1, slot, angle, flip));
            }
            if (level >= 250) {
                result.add(getTetrisPartSlot(1, -2, slot, angle, flip));
            }
        }
        result.removeIf(i -> i == -1);

        return result;
    }

    public static int getLevel(List<Integer> tetrisSlot, UnionStatType type) {
        int level = 0;
        for (var slot : tetrisSlot) {
            switch (type) {
                case Union_StatPosition0 -> {
                    if (l0.contains(slot)) {
                        level++;
                    }
                }
                case Union_StatPosition1 -> {
                    if (l1.contains(slot)) {
                        level++;
                    }
                }
                case Union_StatPosition2 -> {
                    if (l2.contains(slot)) {
                        level++;
                    }
                }
                case Union_StatPosition3 -> {
                    if (l3.contains(slot)) {
                        level++;
                    }
                }
                case Union_StatPosition4 -> {
                    if (l4.contains(slot)) {
                        level++;
                    }
                }
                case Union_StatPosition5 -> {
                    if (l5.contains(slot)) {
                        level++;
                    }
                }
                case Union_StatPosition6 -> {
                    if (l6.contains(slot)) {
                        level++;
                    }
                }
                case Union_StatPosition7 -> {
                    if (l7.contains(slot)) {
                        level++;
                    }
                }
                case Union_AbnormalStatusResistance -> {
                    if (lAbnormalStatusResistance.contains(slot)) {
                        level++;
                    }
                }
                case Union_BonusExp -> {
                    if (lBonusExp.contains(slot)) {
                        level++;
                    }
                }
                case Union_CriticalRate -> {
                    if (lCriticalRate.contains(slot)) {
                        level++;
                    }
                }
                case Union_BossDamage -> {
                    if (lBossDamage.contains(slot)) {
                        level++;
                    }
                }
                case Union_KnockbackResistance -> {
                    if (lKnockbackResistance.contains(slot)) {
                        level++;
                    }
                }
                case Union_BuffDuration -> {
                    if (lBuffDuration.contains(slot)) {
                        level++;
                    }
                }
                case Union_IgnoreDef -> {
                    if (lIgnoreDef.contains(slot)) {
                        level++;
                    }
                }
                case Union_CriticalDamage -> {
                    if (lCriticalDamage.contains(slot)) {
                        level++;
                    }
                }
            }
        }
        return level;
    }

    public static boolean isUnionSkill(int skillID) {
        return skillID / 100000 == 710;
    }

    public static int getUnionCardLevel(short jobID, int level) {
        if (JobConstants.isZero(jobID)) {
            if (level >= 250) {
                return 5;
            } else if (level >= 200) {
                return 4;
            } else if (level >= 180) {
                return 3;
            } else if (level >= 160) {
                return 2;
            } else if (level >= 130) {
                return 1;
            } else {
                return 0;
            }
        } else {
            if (level >= 250) {
                return 5;
            } else if (level >= 200) {
                return 4;
            } else if (level >= 140) {
                return 3;
            } else if (level >= 100) {
                return 2;
            } else if (level >= 60) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    public static int getUnionJobSkill(short jobID) {
        if (JobConstants.isHero(jobID)) {
            return EXPLORER_WARRIOR_HERO;
        } else if (JobConstants.isPaladin(jobID)) {
            return EXPLORER_WARRIOR_PALADIN;
        } else if (JobConstants.isKaiser(jobID)) {
            return NOVA_WARRIOR_KAISER;
        } else if (JobConstants.isBuccaneer(jobID)) {
            return EXPLORER_PIRATE_BUCCANEER_1;
        } else if (JobConstants.isCannoneer(jobID)) {
            return EXPLORER_PIRATE_CANNONEER;
        } else if (JobConstants.isThunderBreaker(jobID)) {
            return CYGNUS_PIRATE_THUNDER_BREAKER;
        } else if (JobConstants.isArk(jobID)) {
            return HIGH_FLORA_PIRATE_ARK;
        } else if (JobConstants.isBowMaster(jobID)) {
            return EXPLORER_BOWMAN_BOW_MASTER;
        } else if (JobConstants.isWindArcher(jobID)) {
            return CYGNUS_BOWMAN_WIND_ARCHER;
        } else if (JobConstants.isAngelicBuster(jobID)) {
            return NOVA_PIRATE_ANGELIC_BUSTER;
        } else if (JobConstants.isIceLightning(jobID)) {
            return EXPLORER_MAGICIAN_ARCH_MAGE_IL;
        } else if (JobConstants.isBishop(jobID)) {
            return EXPLORER_MAGICIAN_BISHOP;
        } else if (JobConstants.isBattleMage(jobID)) {
            return RESISTANCE_MAGICIAN_BATTLE_MAGE;
        } else if (JobConstants.isLuminous(jobID)) {
            return HERO_MAGICIAN_LUMINOUS;
        } else if (JobConstants.isBlazeWizard(jobID)) {
            return CYGNUS_MAGICIAN_BLAZE_WIZARD;
        } else if (JobConstants.isKinesis(jobID)) {
            return NATURAL_GENIUS_KINESIS;
        } else if (JobConstants.isIllium(jobID)) {
            return VERDANT_FLORA_MAGICIAN_ILLIUM;
        } else if (JobConstants.isShadower(jobID)) {
            return EXPLORER_THIEF_SHADOWER;
        } else if (JobConstants.isDualBlade(jobID)) {
            return EXPLORER_THIEF_DUAL_BLADE;
        } else if (JobConstants.isNightWalker(jobID)) {
            return CYGNUS_THIEF_NIGHT_WALKER;
        } else if (JobConstants.isCadena(jobID)) {
            return NOVA_THIEF_CADENA;
        } else if (JobConstants.isXenon(jobID)) {
            return RESISTANCE_HYBRID_XENON;
        } else if (JobConstants.isDarkKnight(jobID)) {
            return EXPLORER_WARRIOR_DARK_KNIGHT;
        } else if (JobConstants.isDawnWarrior(jobID)) {
            return CYGNUS_DAWN_WARRIOR;
        } else if (JobConstants.isMihile(jobID)) {
            return MIHILE_KNIGHT_OF_LIGHT;
        } else if (JobConstants.isAran(jobID)) {
            return HERO_WARRIOR_ARAN;
        } else if (JobConstants.isFirePoison(jobID)) {
            return EXPLORER_MAGICIAN_ARCH_MAGE_FP;
        } else if (JobConstants.isEvan(jobID)) {
            return HERO_MAGICIAN_EVAN;
        } else if (JobConstants.isZero(jobID)) {
            return CHILD_OF_THE_GODDESS_ZERO;
        } else if (JobConstants.isMechanic(jobID)) {
            return RESISTANCE_PIRATE_MECHANIC;
        } else if (JobConstants.isMercedes(jobID)) {
            return HERO_BOWMAN_MERCEDES;
        } else if (JobConstants.isDemonSlayer(jobID)) {
            return RESISTANCE_WARRIOR_DEMON_SLAYER;
        } else if (JobConstants.isPhantom(jobID)) {
            return HERO_THIEF_PHANTOM;
        } else if (JobConstants.isCorsair(jobID)) {
            return 0; //IDK
        } else if (JobConstants.isMarksman(jobID)) {
            return EXPLORER_BOWMAN_MARKSMAN;
        } else if (JobConstants.isNightLord(jobID)) {
            return EXPLORER_THIEF_NIGHT_LORD;
        } else if (JobConstants.isShade(jobID)) {
            return PIRATE_HERO_SHADE;
        } else if (JobConstants.isHayato(jobID)) {
            return SENGOKU_WARRIOR_HAYATO;
        } else if (JobConstants.isBlaster(jobID)) {
            return RESISTANCE_WARRIOR_BLASTER;
        } else if (JobConstants.isBeastTamer(jobID)) { //Nah
            return SONG_OF_SPIRITS;
        } else if (JobConstants.isDemonAvenger(jobID)) {
            return RESISTANCE_WARRIOR_DEMON_AVENGER;
        } else if (JobConstants.isKanna(jobID)) {
            return SENGOKU_WARRIOR_KANNA;
        } else {
            return 0;
        }
    }
}
