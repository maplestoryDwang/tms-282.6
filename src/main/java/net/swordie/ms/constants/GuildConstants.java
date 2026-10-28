package net.swordie.ms.constants;

public class GuildConstants {
    public static final int MAX_DAY_CONTRIBUTION = 5000;

    public static final int CONTRIBUTION_PER_QUEST = 1;

    public static final int SP_PER_GUILD_LEVEL = 2;

    public static final double GGP_PER_CONTRIBUTION = 0.3;

    public static final double IGP_PER_CONTRIBUTION = 0.7;

    public static final int GUILD_BBS_RECORDS_PER_PAGE = 10;

    public static final int GGP_FOR_SKILL_RESET = 50000;

    public static final int MAX_GUILD_LV = 25;

    public static final int MAX_GUILD_MEMBERS = 200;

    public static final int QR_GUILD_QUESTID = 26015;

    public static final String QR_GUILD_SYNTAX = "guild=%d;name=%s;time_leave=%d;";

    public static final int[] EXP_TABLE = {
            0,
            15000,
            45000,
            75000,
            105000,
            135000,
            165000,
            195000,
            225000,
            255000,
            285000,
            315000,
            345000,
            375000,
            405000,
            435000,
            465000,
            495000,
            525000,
            555000,
            585000,
            615000,
            645000,
            675000,
            705000,
            0
    };

    public static int getExpRequiredForNextGuildLevel(int curLevel) {
        if (curLevel >= 25 || curLevel < 0) {
            return 0;
        }
        return EXP_TABLE[curLevel];
    }

    public static boolean isGuildSkill(int skillID) {
        int prefix = skillID / 10000;
        if (prefix == 8000) {
            prefix = skillID / 100;
        }
        return prefix == 9100;
    }

    public static boolean isGuildContentSkill(int skillID) {
        return (skillID >= 91000007 && skillID <= 91000038) || (skillID >= 91001016 && skillID <= 91001036);
    }

    public static boolean isGuildNoblesseSkill(int skillID) {
        return skillID >= 91001022 && skillID <= 91001025;
    }

    public static int getIGPCostFromSkill(int skillID, int skillLevel) {
        switch (skillID) {
            case ON_MY_WAY:
            case I_SUMMON_THEE:
            case RISE_MINIONS:
            case SHARENIAN_DEMON_MOUNT:
                return 2000 - 200 * skillLevel;
            case BOSS_SLAYERS:
            case UNDETERRED:
            case FOR_THE_GUILD:
            case HARD_HITTER:
                return 1000 - 50 * skillLevel;
            default:
                return 0;
        }
    }

    // Normal Guild Skills:
    public static final int SPOTTING_SMALL_CHANGE = 91000011;
    public static final int MERCHANDISING = 91000008;
    public static final int ENHANCEMENT_MASTERY = 91000012;
    public static final int ITEM_SALVATION = 91000013;
    public static final int UPGRADE_SALVATION = 91000014;

    public static final int BANNER_OF_PLENTY_I = 91000009;
    public static final int BANNER_OF_PLENTY_II = 91000031;
    public static final int BANNER_OF_PLENTY_III = 91000032;
    public static final int ARCANE_FORCE = 91000035;

    public static final int UNITED_FRONT = 91000026;
    public static final int GUILD_ON_FIRE_I = 91000028;
    public static final int GUILD_ON_FIRE_II = 91000029;
    public static final int GUILD_ON_FIRE_III = 91000030;

    public static final int AMIST_THE_STARS = 91000038;
    public static final int TEAM_PLAYERS = 91000033;
    public static final int RISE_MINIONS = 91001020;
    public static final int WELL_ROUNDED = 91000034;
    public static final int SHARENIAN_DEMON_MOUNT = 91001036;

    public static final int GUILD_EXPERTISE = 91000027;
    public static final int ON_MY_WAY = 91001016;
    public static final int I_SUMMON_THEE = 91001017;
    public static final int FEARLESS = 91000015;

    // Noblessse Guild Skills:
    public static final int BOSS_SLAYERS = 91001022;
    public static final int UNDETERRED = 91001023;
    public static final int FOR_THE_GUILD = 91001024;
    public static final int HARD_HITTER = 91001025;
}
