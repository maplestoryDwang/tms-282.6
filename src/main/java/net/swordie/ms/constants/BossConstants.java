package net.swordie.ms.constants;

import net.swordie.ms.enums.BossPartyType;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.util.Position;
import net.swordie.ms.util.Rect;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BossConstants {

    public static boolean isBossSet(int itemID) {
        switch (itemID) {
            case 99999999: // Todo thêm tất cả Item ID của Boss Set vào
                return true;
        }
        return false;
    }

    public static int getBossFightingTime(BossPartyType boss) {
        if (net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3
                && (boss == BossPartyType.RANMARU_NORMAL || boss == BossPartyType.RANMARU_HARD)) return 60 * 60;
        return switch (boss) {
            case BALROG -> BALROG_TIME;
            case ZAKUM_EASY -> EASY_ZAKUM_TIME;
            case ZAKUM_NORMAL, ZAKUM_CHAOS -> CHAOS_ZAKUM_TIME;
            case HORNTAIL_EASY -> EASY_HORNTAIL_TIME;
            case HORNTAIL_NORMAL, HORNTAIL_CHAOS -> CHAOS_HORNTAIL_TIME;
            case HILLA_NORMAL -> NORMAL_HILLA_TIME;
            case HILLA_HARD -> EASY_HILLA_TIME;
            case MAGNUS_EASY, MAGNUS_NORMAL, MAGNUS_HARD -> MAGNUS_TIME;
            case PIERRE_NORMAL, PIERRE_CHAOS, VON_BON_NORMAL, VON_BON_CHAOS,
                    QUEEN_NORMAL, QUEEN_CHAOS, VELLUM_NORMAL, VELLUM_CHAOS -> ROOT_ABYSS_TIME;
            case VON_LEON_EASY, VON_LEON_NORMAL, VON_LEON_HARD -> VON_LEON_TIME;
            case ARKARIUM_EASY, ARKARIUM_NORMAL -> ARKARIUM_TIME;
            case PINK_BEAN_NORMAL, PINK_BEAN_CHAOS -> PINK_BEAN_TIME;
            case CYGNUS_EASY, CYGNUS_NORMAL -> CYGNUS_TIME;
            case LOTUS_NORMAL, LOTUS_HARD, LOTUS_EXTREME -> LOTUS_TIME;
            case DAMIEN_NORMAL, DAMIEN_HARD -> DEMIAN_TIME;
            case GOLLUX -> GOLLUX_TIME;
            case RANMARU_NORMAL, RANMARU_HARD -> RANMARU_TIME;
            case LUCID_EASY, LUCID_NORMAL, LUCID_HARD -> LUCID_TIME;
            case WILL_EASY, WILL_NORMAL, WILL_HARD -> WILL_TIME;
            case VERUS_HILLA_NORMAL, VERUS_HILLA_HARD -> VERUS_HILLA_TIME;
            case GLOOM_NORMAL, GLOOM_CHAOS, DARKNELL_NORMAL, DARKNELL_HARD,
                    BLACK_MAGE_HARD, BLACK_MAGE_EXTREME -> 30 * 60;
            case PRINCESS_NO, OMNI_CLN_NORMAL, AKECHI_MITSUHIDE,
                    CHOSEN_SEREN_NORMAL, CHOSEN_SEREN_HARD, CHOSEN_SEREN_EXTREME,
                    KALOS_EASY, KALOS_NORMAL, KALOS_HARD, KALOS_EXTREME,
                    KALING_EASY, KALING_NORMAL, KALING_HARD, KALING_EXTREME,
                    MONSTER_PARK_EXTREME, LIMBO_NORMAL, LIMBO_HARD,
                    BALDRIX_NORMAL, BALDRIX_HARD,
                    FIRST_ADVERSARY_EASY, FIRST_ADVERSARY_NORMAL, FIRST_ADVERSARY_HARD, FIRST_ADVERSARY_EXTREME ->
                    30 * 60;
            case URSUS -> URSUS_TIME;
            case MALITIA_NORMAL, MALITIA_EXTREME -> 30 * 60;
            default -> BOSS_WAITING_TIME;
        };
    }

    /** Runtime entry can be a cutscene field; legacy map tables remain profile-independent. */
    public static int getBossEntryFieldId(BossPartyType boss) {
        if (net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) {
            switch (boss) {
                case WILL_EASY, WILL_NORMAL: return 450008700;
                case WILL_HARD: return 450008100;
                case LIMBO_NORMAL: return 410009000;
                case LIMBO_HARD: return 410011000;
                case BALDRIX_NORMAL: return 410014000;
                case BALDRIX_HARD: return 410015100;
                case FIRST_ADVERSARY_EASY: return 410020000;
                case FIRST_ADVERSARY_NORMAL: return 410021000;
                case FIRST_ADVERSARY_HARD: return 410022000;
                case FIRST_ADVERSARY_EXTREME: return 410023000;
                case LOTUS_EXTREME: return 350068000;
                case CHOSEN_SEREN_NORMAL: return 410002100;
                case CHOSEN_SEREN_HARD: return 410002000;
                case CHOSEN_SEREN_EXTREME: return 410002200;
                case KALOS_NORMAL: return 410006000;
                case KALOS_EASY: return 410006200;
                case KALOS_HARD: return 410006300;
                case KALOS_EXTREME: return 410006400;
                case KALING_EASY: return 410008000;
                case KALING_NORMAL: return 410007100;
                case KALING_HARD: return 410008300;
                case KALING_EXTREME: return 410008600;
                case RANMARU_NORMAL: return 807300110;
                case RANMARU_HARD: return 807300210;
                case OMNI_CLN_NORMAL: return 221030910;
                case PRINCESS_NO: return 811000100;
                case MALITIA_NORMAL: return 875030000;
                case MALITIA_EXTREME: return 875031000;
                default: break;
            }
        }
        return getBossFightingFieldId(boss);
    }

    public static int getBossLobbyFieldId(BossPartyType boss) {
        if (net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) {
            switch (boss) {
                case LIMBO_NORMAL, LIMBO_HARD: return 410007601;
                case BALDRIX_NORMAL, BALDRIX_HARD: return 410013608;
                case FIRST_ADVERSARY_EASY, FIRST_ADVERSARY_NORMAL,
                        FIRST_ADVERSARY_HARD, FIRST_ADVERSARY_EXTREME: return 410007019;
                case RANMARU_NORMAL, RANMARU_HARD: return 807300100;
                case PRINCESS_NO: return 811000999;
                case MALITIA_NORMAL, MALITIA_EXTREME: return 875033004;
                case KAI_NORMAL, KAI_HARD: return 993277350;
                case YMIR_NORMAL, YMIR_HARD: return 410007670;
                case JUPITER_NORMAL, JUPITER_HARD: return 410013709;
                default: break;
            }
        }
        return getBossWaitingFieldId(boss);
    }

    public static int getBossFightingFieldId(BossPartyType boss) {
        return switch (boss) {
            case BALROG -> BALROG_EASY_BATTLE_MAP;
            case ZAKUM_EASY -> ZAKUM_EASY_ALTAR;
            case ZAKUM_NORMAL -> ZAKUM_NORMAL_ALTAR;
            case ZAKUM_CHAOS -> ZAKUM_CHAOS_ALTAR;
            case HORNTAIL_EASY -> 240060000;
            case HORNTAIL_NORMAL -> 240060002;
            case HORNTAIL_CHAOS -> 240060001;
            case HILLA_NORMAL -> 262030300;
            case HILLA_HARD -> 262031300;
            case MAGNUS_EASY -> 401060300;
            case MAGNUS_NORMAL -> 401060100;
            case MAGNUS_HARD -> 401060200;
            case PIERRE_NORMAL -> 105200210;
            case PIERRE_CHAOS -> 105200610;
            case VON_BON_NORMAL -> 105200510;
            case VON_BON_CHAOS -> 105200110;
            case QUEEN_NORMAL -> 105200310;
            case QUEEN_CHAOS -> 105200710;
            case VELLUM_NORMAL -> 105200410;
            case VELLUM_CHAOS -> 105200810;
            case VON_LEON_EASY, VON_LEON_NORMAL, VON_LEON_HARD -> 211070100;
            case ARKARIUM_EASY -> 272020200;
            case ARKARIUM_NORMAL -> 272020210;
            case PINK_BEAN_NORMAL -> 270050100;
            case PINK_BEAN_CHAOS -> 270051100;
            case CYGNUS_EASY -> 271040100;
            case CYGNUS_NORMAL -> 271041100;
            case LOTUS_NORMAL -> 350060700;
            case LOTUS_HARD, LOTUS_EXTREME -> 350060400;
            case PRINCESS_NO -> PRINCESS_NO_FIGHT_MAP;
            case OMNI_CLN_NORMAL -> OMNI_CLN_FIGHT_MAP;
            case CHOSEN_SEREN_NORMAL -> SEREN_NORMAL_FIGHT_MAP;
            case CHOSEN_SEREN_HARD -> SEREN_HARD_FIGHT_MAP;
            case CHOSEN_SEREN_EXTREME -> SEREN_EXTREME_FIGHT_MAP;
            case KALOS_EASY -> KALOS_EASY_FIGHT_MAP;
            case KALOS_NORMAL -> KALOS_NORMAL_FIGHT_MAP;
            case KALOS_HARD -> KALOS_HARD_FIGHT_MAP;
            case KALOS_EXTREME -> KALOS_EXTREME_FIGHT_MAP;
            case KALING_EASY, KALING_NORMAL, KALING_HARD, KALING_EXTREME -> KALING_FIGHT_MAP;
            case MONSTER_PARK_EXTREME -> MONSTER_PARK_EXTREME_FIGHT_MAP;
            case LIMBO_NORMAL, LIMBO_HARD -> LIMBO_FIGHT_MAP;
            case BALDRIX_NORMAL, BALDRIX_HARD -> BALDRIX_FIGHT_MAP;
            case FIRST_ADVERSARY_EASY, FIRST_ADVERSARY_NORMAL, FIRST_ADVERSARY_HARD, FIRST_ADVERSARY_EXTREME ->
                    FIRST_ADVERSARY_FIGHT_MAP;
            case AKECHI_MITSUHIDE -> AKECHI_FIGHT_MAP;
            case BLACK_MAGE_EXTREME -> BLACK_MAGE_FIGHT_MAP;
            case DAMIEN_NORMAL -> 350160200;
            case DAMIEN_HARD -> 350160100;
            case GOLLUX -> GOLLUX_FIRST_MAP;
            case RANMARU_NORMAL -> 211041700;
            case RANMARU_HARD -> 211042000;
            case LUCID_EASY, LUCID_NORMAL, LUCID_HARD -> 450004150;
            case WILL_EASY, WILL_NORMAL -> 450008750;
            case WILL_HARD -> 450008150;
            case VERUS_HILLA_NORMAL, VERUS_HILLA_HARD -> VERUS_HILLA_FIGHT_MAP;
            case GLOOM_NORMAL -> 450009400;
            case GLOOM_CHAOS -> 450009450;
            case DARKNELL_NORMAL -> 450012210;
            case DARKNELL_HARD -> 450012600;
            case BLACK_MAGE_HARD -> 450013100;
            case PAPULATUS_EASY -> 220080100;
            case PAPULATUS_NORMAL -> 220080200;
            case PAPULATUS_CHAOS -> 220080300;
            case SLIME_NORMAL -> 160040000;
            case SLIME_HARD -> 160020000;
            case URSUS -> URSUS_FIGHT_MAP;
            default -> getBossWaitingFieldId(boss);
        };
    }

    public static int getBossWaitingFieldId(BossPartyType boss) {
        return switch (boss) {
            case BALROG -> BALROG_ENTRY_MAP;
            case ZAKUM_EASY -> ZAKUM_EASY_ENTRANCE;
            case ZAKUM_NORMAL -> ZAKUM_NORMAL_ENTRANCE;
            case ZAKUM_CHAOS -> ZAKUM_CHAOS_ENTRANCE;
            case MAGNUS_EASY -> MAGNUS_EASY_ENTRANCE_MAP;
            case MAGNUS_NORMAL -> MAGNUS_NORMAL_ENTRANCE_MAP;
            case MAGNUS_HARD -> MAGNUS_HARD_ENTRANCE_MAP;
            case HILLA_NORMAL, HILLA_HARD -> HILLA_ENTRANCE_MAP;
            case OMNI_CLN_NORMAL -> OMNI_CLN_ENTRANCE_MAP;
            case PAPULATUS_EASY, PAPULATUS_NORMAL, PAPULATUS_CHAOS -> PAPULATUS_ENTRANCE_MAP;
            case PIERRE_NORMAL, PIERRE_CHAOS, VON_BON_NORMAL, VON_BON_CHAOS, QUEEN_NORMAL, QUEEN_CHAOS, VELLUM_NORMAL, VELLUM_CHAOS ->
                    ROOT_ABYSS_ENTRACE_MAP;
            case VON_LEON_EASY, VON_LEON_NORMAL, VON_LEON_HARD -> VON_LEON_ENTRACE_MAP;
            case HORNTAIL_EASY, HORNTAIL_NORMAL, HORNTAIL_CHAOS -> HORNTAIL_ENTRANCE_MAP;
            case ARKARIUM_EASY, ARKARIUM_NORMAL -> ARKARIUM_ENTRACE_MAP;
            case PINK_BEAN_NORMAL, PINK_BEAN_CHAOS -> PINK_BEAN_ENTRACE_MAP;
            case CYGNUS_EASY, CYGNUS_NORMAL -> CYGNUS_ENTRACE_MAP;
            case LOTUS_NORMAL, LOTUS_HARD, LOTUS_EXTREME -> LOTUS_ENTRANCE_MAP;
            case CHOSEN_SEREN_NORMAL, CHOSEN_SEREN_HARD, CHOSEN_SEREN_EXTREME -> SEREN_ENTRANCE_MAP;
            case KALOS_EASY, KALOS_NORMAL, KALOS_HARD, KALOS_EXTREME -> KALOS_ENTRANCE_MAP;
            case KALING_EASY, KALING_NORMAL, KALING_HARD, KALING_EXTREME -> KALING_ENTRANCE_MAP;
            case MONSTER_PARK_EXTREME -> MONSTER_PARK_EXTREME_ENTRANCE_MAP;
            case LIMBO_NORMAL, LIMBO_HARD -> LIMBO_ENTRANCE_MAP;
            case BALDRIX_NORMAL, BALDRIX_HARD -> BALDRIX_ENTRANCE_MAP;
            case FIRST_ADVERSARY_EASY, FIRST_ADVERSARY_NORMAL, FIRST_ADVERSARY_HARD, FIRST_ADVERSARY_EXTREME ->
                    FIRST_ADVERSARY_ENTRANCE_MAP;
            case AKECHI_MITSUHIDE -> AKECHI_ENTRANCE_MAP;
            case BLACK_MAGE_EXTREME -> BLACK_MAGE_ENTRANCE_MAP;
            case DAMIEN_NORMAL, DAMIEN_HARD -> DEMIAN_ENTRACE_MAP;
            case LUCID_EASY, LUCID_NORMAL, LUCID_HARD -> LUCID_ENTRACE_MAP;
            case URSUS -> URSUS_ENTRANCE_MAP;
            case GOLLUX -> GOLLUX_ENTRACE_MAP;
            case GLOOM_NORMAL, GLOOM_CHAOS -> GLOOM_ENTRACE_MAP;
            case VERUS_HILLA_NORMAL, VERUS_HILLA_HARD -> VERUS_HILLA_ENTRANCE_MAP;
            case DARKNELL_NORMAL, DARKNELL_HARD -> DARKNELL_ENTRANCE_MAP;
            case BLACK_MAGE_HARD -> BLACK_MAGE_ENTRANCE_MAP;
            case RANMARU_NORMAL, RANMARU_HARD -> RANMARU_ENTRACE_MAP;
            case PRINCESS_NO -> PRINCESS_NO_ENTRACE_MAP;
            case WILL_EASY, WILL_NORMAL, WILL_HARD -> WILL_ENTRACE_MAP;
            case SLIME_NORMAL, SLIME_HARD -> SLIME_ENTRANCE_MAP;
            default -> 0;
        };
    }

    public static int[] getRequiredEntryItems(BossPartyType boss) {
        return switch (boss) {
            case ZAKUM_EASY -> new int[]{ZAKUM_EASY_SPAWN_ITEM};
            case ZAKUM_NORMAL, ZAKUM_CHAOS -> new int[]{ZAKUM_CHAOS_SPAWN_ITEM};
            case PIERRE_NORMAL, PIERRE_CHAOS,
                    VON_BON_NORMAL, VON_BON_CHAOS,
                    QUEEN_NORMAL, QUEEN_CHAOS,
                    VELLUM_NORMAL, VELLUM_CHAOS -> new int[]{ROOT_ABYSS_ENTRY_KEY};
            default -> new int[0];
        };
    }

    /**
     * 僅開放已驗證具備召喚路徑的挑戰。地圖存在並不代表該版本能完成挑戰，
     * 因此不能把“有地圖”作爲 Boss UI 的准入條件。
     */
    public static boolean isBossChallengeSupported(BossPartyType boss) {
        return switch (boss) {
            case BALROG,
                    ZAKUM_EASY, ZAKUM_NORMAL, ZAKUM_CHAOS,
                    HORNTAIL_EASY, HORNTAIL_NORMAL, HORNTAIL_CHAOS,
                    HILLA_NORMAL, HILLA_HARD,
                    PIERRE_NORMAL, PIERRE_CHAOS,
                    VON_BON_NORMAL, VON_BON_CHAOS,
                    QUEEN_NORMAL, QUEEN_CHAOS,
                    VELLUM_NORMAL, VELLUM_CHAOS,
                    VON_LEON_EASY, VON_LEON_NORMAL, VON_LEON_HARD,
                    ARKARIUM_EASY, ARKARIUM_NORMAL,
                    MAGNUS_EASY, MAGNUS_NORMAL, MAGNUS_HARD,
                    PINK_BEAN_NORMAL, PINK_BEAN_CHAOS,
                    CYGNUS_EASY, CYGNUS_NORMAL,
                    LOTUS_NORMAL, LOTUS_HARD,
                    URSUS,
                    DAMIEN_NORMAL, DAMIEN_HARD,
                    GOLLUX,
                    RANMARU_NORMAL, RANMARU_HARD,
                    LUCID_EASY, LUCID_NORMAL, LUCID_HARD,
                    PAPULATUS_EASY, PAPULATUS_NORMAL, PAPULATUS_CHAOS,
                    WILL_EASY, WILL_NORMAL, WILL_HARD,
                    VERUS_HILLA_NORMAL, VERUS_HILLA_HARD,
                    BLACK_MAGE_HARD,
                    GLOOM_NORMAL, GLOOM_CHAOS,
                    DARKNELL_NORMAL, DARKNELL_HARD,
                    SLIME_NORMAL, SLIME_HARD,
                    LOTUS_EXTREME,
                    PRINCESS_NO, OMNI_CLN_NORMAL,
                    BLACK_MAGE_EXTREME,
                    CHOSEN_SEREN_NORMAL, CHOSEN_SEREN_HARD, CHOSEN_SEREN_EXTREME,
                    KALOS_EASY, KALOS_NORMAL, KALOS_HARD, KALOS_EXTREME,
                    KALING_EASY, KALING_NORMAL, KALING_HARD, KALING_EXTREME,
                    MONSTER_PARK_EXTREME,
                    LIMBO_NORMAL, LIMBO_HARD,
                    BALDRIX_NORMAL, BALDRIX_HARD,
                    FIRST_ADVERSARY_EASY, FIRST_ADVERSARY_NORMAL, FIRST_ADVERSARY_HARD, FIRST_ADVERSARY_EXTREME,
                    AKECHI_MITSUHIDE -> true;
            // Malitia (TMS273 boss_malitia_*): offered only when its lobby and both first fields exist for the
            // active profile (282 data has them; the 265 data does not, so the 265 profile keeps it closed).
            case MALITIA_NORMAL, MALITIA_EXTREME ->
                    net.swordie.ms.world.tms282.bossrun.BossRunMaps.malitiaAvailable();
            default -> false;
        };
    }

    public static boolean hasConfiguredBossMaps(BossPartyType boss) {
        if (net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) {
            // Removed client maps and the Xerxes-map placeholder cannot be advertised as an entry.
            if (boss == BossPartyType.AKECHI_MITSUHIDE || boss == BossPartyType.MONSTER_PARK_EXTREME) return false;
            boolean supported = isBossChallengeSupported(boss)
                    || boss == BossPartyType.MALITIA_NORMAL || boss == BossPartyType.MALITIA_EXTREME;
            int lobby = getBossLobbyFieldId(boss), entry = getBossEntryFieldId(boss);
            var availability = net.swordie.ms.connection.protocol.codec.FieldAvailability.active();
            return supported && lobby > 0 && entry > 0 && availability.isAvailable(lobby) && availability.isAvailable(entry);
        }
        return isBossChallengeSupported(boss)
                && getBossWaitingFieldId(boss) > 0
                && getBossFightingFieldId(boss) > 0;
    }

    public static int getBossPartyLimit(BossPartyType boss) {
        if (net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) {
            return switch (boss) {
                case KAI_NORMAL, KAI_HARD -> 1;
                case MALITIA_NORMAL, MALITIA_EXTREME, LIMBO_NORMAL, LIMBO_HARD -> 3;
                default -> Integer.MAX_VALUE;
            };
        }
        return Integer.MAX_VALUE;
    }

    public static int getBossPartyBuffSkill(BossPartyType bossPartyType) {
        return switch (bossPartyType) {
            case BALROG -> 80001535;
            case ZAKUM_EASY, ZAKUM_NORMAL, ZAKUM_CHAOS -> 80001536;
            case HORNTAIL_EASY, HORNTAIL_NORMAL, HORNTAIL_CHAOS -> 80001537;
            case HILLA_NORMAL, HILLA_HARD -> 80001538;
            case VON_LEON_EASY, VON_LEON_NORMAL, VON_LEON_HARD -> 80001539;
            case ARKARIUM_EASY, ARKARIUM_NORMAL -> 80001540;
            case MAGNUS_EASY, MAGNUS_NORMAL, MAGNUS_HARD -> 80001541;
            case PINK_BEAN_NORMAL, PINK_BEAN_CHAOS -> 80001542;
            case CYGNUS_EASY, CYGNUS_NORMAL -> 80001543;
            case PIERRE_NORMAL, PIERRE_CHAOS -> 80001544;
            case VON_BON_NORMAL, VON_BON_CHAOS -> 80001545;
            case QUEEN_NORMAL, QUEEN_CHAOS -> 80001546;
            case VELLUM_NORMAL, VELLUM_CHAOS -> 80001547;
            case LOTUS_NORMAL, LOTUS_HARD, LOTUS_EXTREME -> 80001634;
            case CHOSEN_SEREN_NORMAL, CHOSEN_SEREN_HARD, CHOSEN_SEREN_EXTREME -> 80002643;
            case KALOS_EASY, KALOS_NORMAL, KALOS_HARD, KALOS_EXTREME -> 80002643;
            case KALING_EASY, KALING_NORMAL, KALING_HARD, KALING_EXTREME -> 80002643;
            case LIMBO_NORMAL, LIMBO_HARD -> 80002643;
            case BALDRIX_NORMAL, BALDRIX_HARD -> 80002643;
            case FIRST_ADVERSARY_EASY, FIRST_ADVERSARY_NORMAL, FIRST_ADVERSARY_HARD, FIRST_ADVERSARY_EXTREME -> 80002643;
            case MONSTER_PARK_EXTREME -> 80002643;
            case DAMIEN_NORMAL, DAMIEN_HARD -> 80001979;
            case LUCID_EASY, LUCID_NORMAL, LUCID_HARD -> 80002232;
            case OMNI_CLN_NORMAL -> 80002264;
            case PAPULATUS_EASY, PAPULATUS_NORMAL, PAPULATUS_CHAOS -> 80002337;
            case WILL_EASY, WILL_NORMAL, WILL_HARD -> 80002419;
            case VERUS_HILLA_NORMAL, VERUS_HILLA_HARD -> 80002642;
            case BLACK_MAGE_HARD, BLACK_MAGE_EXTREME -> 80002643;
            case GLOOM_NORMAL, GLOOM_CHAOS -> 80002645;
            case DARKNELL_NORMAL, DARKNELL_HARD -> 80002646;
            case GOLLUX -> 80011124;
            case RANMARU_NORMAL, RANMARU_HARD -> 80011125;
            case PRINCESS_NO -> 80011126;
            case AKECHI_MITSUHIDE -> 80012058;
            default -> 0;
        };
    }

    public static final int BOSS_WAITING_TIME = 5 * 60; // 5 minutes
    // INFERNO WOLF ----------------------------------------------------------------------------------------------------
    public static final int INFERNO_WOLF = 9101078;
    // BALROG ----------------------------------------------------------------------------------------------------------
    public static final int BALROG_ENTRY_MAP = 105100100; // lobby map
    public static final short BALROG_TIME = 20 * 60; // 20 min
    public static final short BALROG_SPAWN_X = 412;
    public static final short BALROG_SPAWN_Y = 258;
    public static final int BALROG_EASY_DMGSINK = 8830010; // has the hp bar, dmg gets splashed to this from all the below mobs
    public static final int BALROG_EASY_BODY = 8830007;
    public static final int BALROG_EASY_LARM = 8830008;
    public static final int BALROG_EASY_RARM = 8830009;
    public static final int BALROG_EASY_BATTLE_MAP = 105100400;
    public static final int BALROG_EASY_WIN_MAP = 105100401;
    public static final int BALROG_HARD_DMGSINK = 8830003;
    public static final int BALROG_HARD_BODY = 8830000;
    public static final int BALROG_HARD_LARM = 8830001;
    public static final int BALROG_HARD_RARM = 8830002;
    public static final int BALROG_HARD_BATTLE_MAP = 105100300;
    public static final int BALROG_HARD_WIN_MAP = 105100301;
    // ZAKUM -----------------------------------------------------------------------------------------------------------
    public static final int ZAKUM_JQ_MAP_1 = 280020000;
    public static final int ZAKUM_JQ_MAP_2 = 280020001;
    public static final int EASY_ZAKUM_TIME = 30 * 60; // 20 min timer
    public static final int CHAOS_ZAKUM_TIME = 30 * 60; // 30 min timer
    public static final int ZAKUM_TIME_BASE = 15 * 60; // 15 min * difficulty (15 easy, 30 normal, 45 chaos)
    public static final int ZAKUM_SPAWN_X = -14;
    public static final int ZAKUM_SPAWN_Y = 86;
    public static final int ZAKUM_EASY_SPAWN_ITEM = 4001796; // Eye of fire chunk
    public static final int ZAKUM_EASY_ENTRANCE = 211042402;
    public static final int ZAKUM_EASY_ALTAR = 280030200;
    public static final int ZAKUM_EASY_BODY = 8800020;
    public static final int ZAKUM_EASY_ARM = 8800023;
    public static final int ZAKUM_NORMAL_ENTRANCE = 211042400;
    public static final int ZAKUM_NORMAL_ALTAR = 280030100;
    public static final int ZAKUM_NORMAL_BODY = 8800000;
    public static final int ZAKUM_NORMAL_ARM = 8800003;
    public static final int ZAKUM_CHAOS_SPAWN_ITEM = 4001017; // Eye of fire
    public static final int ZAKUM_CHAOS_ENTRANCE = 211042500;
    public static final int ZAKUM_CHAOS_ALTAR = 280030000;
    public static final int ZAKUM_CHAOS_BODY = 8800100;
    public static final int ZAKUM_CHAOS_ARM = 8800103;
    // HORNTAIL --------------------------------------------------------------------------------------------------------
    public static final int HORNTAIL_ENTRANCE_MAP = 240050400;
    public static final int EASY_HORNTAIL_TIME = 75 * 60; // 1 hr, 15 min timer
    public static final int CHAOS_HORNTAIL_TIME = 150 * 60; // 2 hrs, 30 min timer
    // URSUS --------------------------------------------------------------------------------------------------------
    public static final int URSUS_ENTRANCE_MAP = 970072200;
    public static final int URSUS_TIME = 30 * 60; // 30 min
    public static final int URSUS_FIGHT_MAP = 970072000;
    // LOTUS -----------------------------------------------------------------------------------------------------------
    public static final int LOTUS_ENTRANCE_MAP = 350060300;
    public static final int LOTUS_TIME = 30 * 60; // 30 min timer
    public final static int LOTUS_CORE = 8950000;
    public static final long[][] LOTUS_HP_PHASE_DIFFICULTY = {
            {400000000000L, 1700000000000L},
            {400000000000L, 7000000000000L},
            {710000000000L, 24000000000000L}
    };
    public static final int LOTUS_SUMMON_RED = 8950103;
    public static final int LOTUS_SUMMON_BLUE = 8950104;
    public static final int LOTUS_SUMMON_YELLOW = 8950105;
    public static final int LOTUS_SUMMON_CHAOS = 8950107;
    public static final int LOTUS_SUMMON_RED_HARD = 8950003;
    public static final int LOTUS_SUMMON_BLUE_HARD = 8950004;
    public static final int LOTUS_SUMMON_YELLOW_HARD = 8950005;
    public static final int LOTUS_SUMMON_CHAOS_HARD = 8950007;
    public static final int LOTUS_BOUNCING_BALL_DURATION = 20000;
    public static final int LOTUS_BLUE_ATOM_AMOUNT = 3; // max amount of Atoms spawning attempts in 1 call
    public static final int LOTUS_BLUE_ATOM_PROP = 30; // % chance of actually spawning in
    public static final int LOTUS_BLUE_ATOM_DAMAGE = 25; // % of Max HP
    public static final int LOTUS_YELLOW_ATOM_AMOUNT = 3; // max amount of Atoms spawning attempts in 1 call
    public static final int LOTUS_YELLOW_ATOM_PROP = 25; // % chance of actually spawning in
    public static final int LOTUS_YELLOW_ATOM_DAMAGE = 50; // % of Max HP
    public static final int LOTUS_PURPLE_ATOM_AMOUNT = 3; // max amount of Atoms spawning attempts in 1 call
    public static final int LOTUS_PURPLE_ATOM_PROP = 20; // % chance of actually spawning in
    public static final int LOTUS_PURPLE_ATOM_DAMAGE = 100; // % of Max HP
    public static final int LOTUS_ROBOT_ATOM_AMOUNT = 2; // max amount of Atoms spawning attempts in 1 call
    public static final int LOTUS_ROBOT_ATOM_PROP = 15; // % chance of actually spawning in
    public static final int LOTUS_ROBOT_ATOM_DAMAGE = 100; // % of Max HP
    public static final int LOTUS_CRUSHER_ATOM_AMOUNT = 1; // max amount of Atoms spawning attempts in 1 call
    public static final int LOTUS_CRUSHER_ATOM_PROP = 5; // % chance of actually spawning in
    public static final int LOTUS_CRUSHER_ATOM_DAMAGE = 100; // % of Max HP
    // MAGNUS ----------------------------------------------------------------------------------------------------------
    public static final int MAGNUS_EASY_ENTRANCE_MAP = 401060400;
    public static final int MAGNUS_NORMAL_ENTRANCE_MAP = 401060400;
    public static final int MAGNUS_HARD_ENTRANCE_MAP = 401060400;
    public static final int MAGNUS_TIME = 20 * 60; // 20 minutes
    public static final int MAGNUS_DEATHCOUNT = 20; // 20 death count
    public static final int MAGNUS_OBSTACLE_ATOM_VELOCITY = 5; // Velocity at which the Obstacle Atoms fall down.
    public static final int MAGNUS_GREEN_ATOM_EXECUTION_DELAY = 1000; // in ms. Delay between method executions
    public static final int MAGNUS_GREEN_ATOM_AMOUNT = 4; // max amount of Atoms spawning attempts in 1 call
    public static final int MAGNUS_GREEN_ATOM_PROP = 35; // % chance of actually spawning in
    public static final int MAGNUS_GREEN_ATOM_DAMAGE = 25; // % of Max HP
    public static final int MAGNUS_BLUE_ATOM_EXECUTION_DELAY = 750; // in ms. Delay between method executions
    public static final int MAGNUS_BLUE_ATOM_AMOUNT = 4; // max amount of Atoms spawning attempts in 1 call
    public static final int MAGNUS_BLUE_ATOM_PROP = 30; // % chance of actually spawning in
    public static final int MAGNUS_BLUE_ATOM_DAMAGE = 50; // % of Max HP
    public static final int MAGNUS_PURPLE_ATOM_EXECUTION_DELAY = 2000; // in ms. Delay between method executions
    public static final int MAGNUS_PURPLE_ATOM_AMOUNT = 3; // max amount of Atoms spawning attempts in 1 call
    public static final int MAGNUS_PURPLE_ATOM_PROP = 25; // % chance of actually spawning in
    public static final int MAGNUS_PURPLE_ATOM_DAMAGE = 100; // % of Max HP
    // ROOT ABYSS ------------------------------------------------------------------------------------------------------
    public static final int ROOT_ABYSS_ENTRACE_MAP = 105200000;
    public static final int ROOT_ABYSS_ENTRY_KEY = 4033611;
    public static final int ROOT_ABYSS_TIME = 30 * 60; // 30 min timer
    // OMNI-CLN --------------------------------------------------------------------------------------------------------
    public static final int OMNI_CLN_ENTRANCE_MAP = 221030900;
    // PAPULATUS -------------------------------------------------------------------------------------------------------
    public static final int PAPULATUS_ENTRANCE_MAP = 220080000;
    // HILLA -----------------------------------------------------------------------------------------------------------
    public static final int HILLA_ENTRANCE_MAP = 262030000;
    public static final int EASY_HILLA_TIME = 30 * 60; // 1 hr, 15 min timer
    public static final int NORMAL_HILLA_TIME = 150 * 60; // 2 hrs, 30 min timer
    // VERUS HILLA -----------------------------------------------------------------------------------------------------
    public static final int VERUS_HILLA_ENTRANCE_MAP = 940500100;
    public static final int VERUS_HILLA_FIGHT_MAP = 450010500;
    public static final int VERUS_HILLA_TIME = 30 * 60;
    // VON LEON --------------------------------------------------------------------------------------------------------
    public static final int VON_LEON_ENTRACE_MAP = 211070000;
    public static final int VON_LEON_TIME = 30 * 60; // 30 min timer
    // CYGNUS ----------------------------------------------------------------------------------------------------------
    public static final int CYGNUS_ENTRACE_MAP = 271030600;
    public static final int CYGNUS_TIME = 30 * 60; // 30 min timer
    // ARKARIUM --------------------------------------------------------------------------------------------------------
    public static final int ARKARIUM_ENTRACE_MAP = 272020110;
    public static final int ARKARIUM_TIME = 30 * 60; // 30 min timer
    // PINK BEAN -------------------------------------------------------------------------------------------------------
    public static final int PINK_BEAN_ENTRACE_MAP = 270050000;
    public static final int PINK_BEAN_TIME = 30 * 60; // 30 min timer
    // DEMIAN ----------------------------------------------------------------------------------------------------------
    public static final int BRAND_OF_SACRIFICE = 80001974; // Skill ID
    public static final long DEMIAN_NORMAL_PHASE_1_HP = 840000000000L;
    public static final long DEMIAN_HARD_PHASE_1_HP = 25200000000000L;
    public static final long DEMIAN_NORMAL_PHASE_2_HP = 360000000000L;
    public static final long DEMIAN_HARD_PHASE_2_HP = 10800000000000L;
    public static final int DEMIAN_NORMAL_PHASE_1_TEMPLATE_ID = 8880110;
    public static final int DEMIAN_NORMAL_PHASE_2_TEMPLATE_ID = 8880111;
    public static final int DEMIAN_HARD_PHASE_1_TEMPLATE_ID = 8880100;
    public static final int DEMIAN_HARD_PHASE_2_TEMPLATE_ID = 8880101;
    // DEMIAN - Sword --------------------------------------------------------------------------------------------------
    public static final int DEMIAN_ENTRACE_MAP = 105300303;
    public static final int DEMIAN_SWORD_VELOCITY = 30; // default velocity
    public static final int DEMIAN_SWORD_TARGETING_VELOCITY = 60; // default velocity when targeting
    public static final int DEMIAN_MAX_STIGMA = 7; // max stigma
    public static final int DEMIAN_MAX_CORRUPTION = 7; // max corruption
    public static final int DEMIAN_PASSIVE_STIGMA_TIME = 30 * 1000; // Every 30 seconds, users are hit with +1 stigma
    public static final int DEMIAN_STIGMA_INCINERATE_OBJECT_RESPAWN_TIME = 20 * 1000; // Stigma Pillar spawns every 20seconds
    public static final int DEMIAN_STIGMA_INCINERATE_OBJECT_DURATION_TIME = 10 * 1000; // Stigma Pillar lasts 10 seconds
    public static final int DEMIAN_TIME = 30 * 60; // 30 min timer
    // GOLLUX ----------------------------------------------------------------------------------------------------------
    public static final int GOLLUX_ENTRACE_MAP = 863010000;
    public static final int GOLLUX_TIME = 30 * 60; // 30 min timer
    public static final int[][] GOLLUX_HP_MULTIPLIERS = {{1, 60, 300, 500}, {1, 10, 150, 3000}, {1, 10, 300, 6000}};
    public static final int GOLLUX_FIRST_MAP = 863010100;
    public static final int GOLLUX_RIGHT_SHOULDER = 863010330;
    public static final int GOLLUX_LEFT_SHOULDER = 863010430;
    public static final int GOLLUX_ABDOMEN = 863010240;
    public static final int[] GOLLUX_RIGHT_HAND_SKILLS = new int[]{3, 5, 6, 8, 10};
    public static final int[] GOLLUX_LEFT_HAND_SKILLS = new int[]{2, 4, 7, 9, 11};
    public static final int GOLLUX_BREATH_ATTACK = 1;
    public static final int GOLLUX_DROP_STONE_CHANCE = 25;
    public static final int GOLLUX_REWARD_MAP = 863010700;
    // GlOOM -----------------------------------------------------------------------------------------------------------
    public static final int GLOOM_ENTRACE_MAP = 450009301;
    // VON BON ---------------------------------------------------------------------------------------------------------
    public static final String[] VON_BON_PORTAL_NAMES =
            {"Pt01", "Pt02", "Pt04gate", "Pt05gate", "Pt06gate",
                    "Pt08gate", "Pt09gate", "Pt03gate", "Pt07gate"};
    // VELLUM ----------------------------------------------------------------------------------------------------------
    public static final int VELLUM_ATOM_EXECUTION_DELAY = 3000; // in ms. Delay between method executions
    // RANMARU ---------------------------------------------------------------------------------------------------------
    public static final int RANMARU_ENTRACE_MAP = 211041700;
    public static final int RANMARU_TIME = 30 * 60;
    // PRINCESS NO -----------------------------------------------------------------------------------------------------
    public static final int PRINCESS_NO_ENTRACE_MAP = 811000008;
    public static final int PRINCESS_NO_FIGHT_MAP = 811000033;
    public static final int PRINCESS_NO_TIME = 30 * 60;
    // OMNI-CLN --------------------------------------------------------------------------------------------------------
    public static final int OMNI_CLN_FIGHT_MAP = 941000200;
    // CHOSEN SEREN ----------------------------------------------------------------------------------------------------
    public static final int SEREN_ENTRANCE_MAP = 410000670;
    public static final int SEREN_NORMAL_FIGHT_MAP = 410002160;
    public static final int SEREN_HARD_FIGHT_MAP = 410002060;
    public static final int SEREN_EXTREME_FIGHT_MAP = 410002080;
    public static final int SEREN_TIME = 30 * 60;
    // KALOS -----------------------------------------------------------------------------------------------------------
    public static final int KALOS_ENTRANCE_MAP = 410005005;
    public static final int KALOS_EASY_FIGHT_MAP = 410006260;
    public static final int KALOS_NORMAL_FIGHT_MAP = 410006060;
    public static final int KALOS_HARD_FIGHT_MAP = 410006360;
    public static final int KALOS_EXTREME_FIGHT_MAP = 410006460;
    public static final int KALOS_TIME = 30 * 60;
    // KALING ----------------------------------------------------------------------------------------------------------
    public static final int KALING_ENTRANCE_MAP = 410007025;
    public static final int KALING_FIGHT_MAP = 410007160;
    public static final int KALING_TIME = 30 * 60;
    // MONSTER PARK EXTREME --------------------------------------------------------------------------------------------
    public static final int MONSTER_PARK_EXTREME_ENTRANCE_MAP = 951000000;
    public static final int MONSTER_PARK_EXTREME_FIGHT_MAP = 920012300;
    // LIMBO -----------------------------------------------------------------------------------------------------------
    public static final int LIMBO_ENTRANCE_MAP = 402090011;
    public static final int LIMBO_FIGHT_MAP = 410007601;
    // BALDRIX ---------------------------------------------------------------------------------------------------------
    public static final int BALDRIX_ENTRANCE_MAP = 875000000;
    public static final int BALDRIX_FIGHT_MAP = 875001000;
    // FIRST ADVERSARY -------------------------------------------------------------------------------------------------
    public static final int FIRST_ADVERSARY_ENTRANCE_MAP = 954109000;
    public static final int FIRST_ADVERSARY_FIGHT_MAP = 954109500;
    // AKECHI MITSUHIDE ------------------------------------------------------------------------------------------------
    public static final int AKECHI_ENTRANCE_MAP = 807100000;
    public static final int AKECHI_FIGHT_MAP = 874004002;
    public static final int AKECHI_TIME = 30 * 60;
    public static final int BLACK_MAGE_FIGHT_MAP = 450013100;
    // LUCID -----------------------------------------------------------------------------------------------------------
    public static final int LUCID_ENTRACE_MAP = 450004000;
    public static final int LUCID_TIME = 30 * 60;
    public static final List<String> STAINED_GLASS = Arrays.asList("Bblue1", "Bblue2", "Bblue3", "Bred1", "Bred2",
            "Bred3", "Mred2", "Mred3", "Myellow1", "Myellow2", "Myellow3");
    // WILL ------------------------------------------------------------------------------------------------------------
    public static final int WILL_ENTRACE_MAP = 450007240;
    public static final int WILL_TIME = 30 * 60; // 20 minutes
    // JULIETA ---------------------------------------------------------------------------------------------------------
    public static final int JULIETA_ENTRACE_MAP = 867236000;
    // DARKNELL ---------------------------------------------------------------------------------------------------------
    public static final int DARKNELL_ENTRANCE_MAP = 450012200;
    // GUARDIAN ANGEL SLIME --------------------------------------------------------------------------------------------
    public static final int SLIME_ENTRANCE_MAP = 160000000;
    // BLACK MAGE ------------------------------------------------------------------------------------------------------
    public static final int BLACK_MAGE_ENTRANCE_MAP = 450012500;

    private static final Map<Integer, Integer> FIGHT_FIELD_TO_ENTRANCE = new HashMap<>();

    static {
        for (BossPartyType boss : BossPartyType.values()) {
            int fight = getBossFightingFieldId(boss);
            int entrance = getBossWaitingFieldId(boss);
            if (fight > 0 && entrance > 0) {
                FIGHT_FIELD_TO_ENTRANCE.putIfAbsent(fight, entrance);
            }
        }
        putFightEntrance(450004200, LUCID_ENTRACE_MAP);
        putFightEntrance(450004250, LUCID_ENTRACE_MAP);
        putFightEntrance(450004300, LUCID_ENTRACE_MAP);
        putFightEntrance(450004450, LUCID_ENTRACE_MAP);
        putFightEntrance(450004500, LUCID_ENTRACE_MAP);
        putFightEntrance(450004550, LUCID_ENTRACE_MAP);
        putFightEntrance(450004750, LUCID_ENTRACE_MAP);
        putFightEntrance(450004800, LUCID_ENTRACE_MAP);
        putFightEntrance(450004850, LUCID_ENTRACE_MAP);
        putFightEntrance(350160240, DEMIAN_ENTRACE_MAP);
        putFightEntrance(350160140, DEMIAN_ENTRACE_MAP);
        putFightEntrance(350060500, LOTUS_ENTRANCE_MAP);
        putFightEntrance(350060600, LOTUS_ENTRANCE_MAP);
        putFightEntrance(350060800, LOTUS_ENTRANCE_MAP);
        putFightEntrance(350060900, LOTUS_ENTRANCE_MAP);
        putFightEntrance(450010500, VERUS_HILLA_ENTRANCE_MAP);
        putFightEntrance(450013100, BLACK_MAGE_ENTRANCE_MAP);
        putFightEntrance(450013300, BLACK_MAGE_ENTRANCE_MAP);
        putFightEntrance(450013500, BLACK_MAGE_ENTRANCE_MAP);
        putFightEntrance(450013700, BLACK_MAGE_ENTRANCE_MAP);
        putFightEntrance(211042000, RANMARU_ENTRACE_MAP);
        putFightEntrance(807300110, RANMARU_ENTRACE_MAP);
        putFightEntrance(807300210, RANMARU_ENTRACE_MAP);
        for (int fieldId : new int[]{
                450008100, 450008200, 450008250, 450008300, 450008350, 450008400,
                450008700, 450008800, 450008850, 450008900, 450008950
        }) {
            FIGHT_FIELD_TO_ENTRANCE.putIfAbsent(fieldId, WILL_ENTRACE_MAP);
        }
        for (int fieldId : new int[]{
                863010100, 863010200, 863010210, 863010220, 863010230, 863010240,
                863010300, 863010310, 863010320, 863010400, 863010410, 863010420,
                863010600, 863010700
        }) {
            FIGHT_FIELD_TO_ENTRANCE.putIfAbsent(fieldId, GOLLUX_ENTRACE_MAP);
        }
    }

    private static void putFightEntrance(int fightFieldId, int entranceFieldId) {
        FIGHT_FIELD_TO_ENTRANCE.putIfAbsent(fightFieldId, entranceFieldId);
    }

    /**
     * 無副本實例時，將首領戰鬥/階段地圖重定向到對應等待入口。
     */
    public static int getBossEntranceMapForFightField(int fieldId) {
        if (net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) {
            // These were placeholders on public towns/PQ maps, not boss arenas.
            if (fieldId == 875001000 || fieldId == 954109500 || fieldId == 410007601
                    || fieldId == 211041700 || fieldId == 211042000 || fieldId == 941000200
                    || fieldId == 811000033) return 0;
            if (fieldId == BALROG_HARD_BATTLE_MAP) return BALROG_ENTRY_MAP;
            if (fieldId == 221030910) return OMNI_CLN_ENTRANCE_MAP;
            if (fieldId >= 410002000 && fieldId <= 410002280 && fieldId % 20 == 0) return SEREN_ENTRANCE_MAP;
            if (net.swordie.ms.world.boss.Kalos.baseForField(fieldId) != 0) return KALOS_ENTRANCE_MAP;
            if (net.swordie.ms.world.boss.Kaling.isField(fieldId)) return KALING_ENTRANCE_MAP;
            if (fieldId >= 811000100 && fieldId <= 811000500 && fieldId % 100 == 0) return 811000999;
            if ((fieldId >= 410009000 && fieldId <= 410009800
                    || fieldId >= 410011000 && fieldId <= 410011800) && fieldId % 100 == 0) return 410007601;
            if (fieldId >= 410014000 && fieldId <= 410016100 && fieldId % 100 == 0) return 410013608;
            if (fieldId >= 410020000 && fieldId <= 410023600 && fieldId % 100 == 0 && fieldId % 1000 <= 600) return 410007019;
            if (fieldId >= 350068000 && fieldId <= 350068600 && fieldId % 100 == 0) return LOTUS_ENTRANCE_MAP;
            if (net.swordie.ms.world.boss.Malitia.isField(fieldId)) return 875033004;
            if (fieldId == 807300110 || fieldId == 807300120 || fieldId == 807300210 || fieldId == 807300220) return 807300100;
        }
        return FIGHT_FIELD_TO_ENTRANCE.getOrDefault(fieldId, 0);
    }

    public static Rect getMobSkillRect(Mob mob, int skillLevel) {
        Rect rect = new Rect();
        boolean isZakumVerticalArmSkill = isZakumArm(mob.getTemplateId()) && (skillLevel == 25 || skillLevel == 26 || skillLevel == 33 || skillLevel == 34 || skillLevel == 35 || skillLevel == 36);
        boolean isZakumHorizontalArmSkill = isZakumArm(mob.getTemplateId()) && skillLevel == 27;
        switch (mob.getTemplateId()) {
            case 8800023: //Easy Zakum Arm 1
            case 8800003: //Normal Zakum Arm 1
            case 8800103: //Chaos Zakum Arm 1
            {
                if (isZakumVerticalArmSkill) {
                    rect = new Rect(-160, 50, -30, 86);
                } else if (isZakumHorizontalArmSkill) {
                    rect = new Rect(-514, -234, -260, -198);
                }
                break;
            }
            case 8800024: //Easy Zakum Arm 2
            case 8800004: //Normal Zakum Arm 2
            case 8800104: //Chaos Zakum Arm 2
            {
                if (isZakumVerticalArmSkill) {
                    rect = new Rect(-290, 50, -160, 86);
                } else if (isZakumHorizontalArmSkill) {
                    rect = new Rect(-549, -149, -312, -113);
                }
                break;
            }
            case 8800025: //Easy Zakum Arm 3
            case 8800005: //Normal Zakum Arm 3
            case 8800105: //Chaos Zakum Arm 3
            {
                if (isZakumVerticalArmSkill) {
                    rect = new Rect(-420, 50, -290, 86);
                } else if (isZakumHorizontalArmSkill) {
                    rect = new Rect(-549, -55, -308, -19);
                }
                break;
            }

            case 8800026: //Easy Zakum Arm 4
            case 8800006: //Normal Zakum Arm 4
            case 8800106: //Chaos Zakum Arm 4
            {
                if (isZakumVerticalArmSkill) {
                    rect = new Rect(-550, 50, -420, 86);
                }
                break;
            }
            case 8800027: //Easy Zakum Arm 5
            case 8800007: //Normal Zakum Arm 5
            case 8800107: //Chaos Zakum Arm 5
            {
                if (isZakumVerticalArmSkill) {
                    rect = new Rect(-10, 50, 90, 86);
                } else if (isZakumHorizontalArmSkill) {
                    rect = new Rect(300, -237, 554, -201);
                }
                break;
            }
            case 8800028: //Easy Zakum Arm 6
            case 8800008: //Normal Zakum Arm 6
            case 8800108: //Chaos Zakum Arm 6
            {
                if (isZakumVerticalArmSkill) {
                    rect = new Rect(125, 50, 255, 86);
                } else if (isZakumHorizontalArmSkill) {
                    rect = new Rect(334, -149, 589, -113);
                }
                break;
            }
            case 8800029: //Easy Zakum Arm 7
            case 8800009: //Normal Zakum Arm 7
            case 8800109: //Chaos Zakum Arm 7
            {
                if (isZakumVerticalArmSkill) {
                    rect = new Rect(260, 50, 390, 86);
                } else if (isZakumHorizontalArmSkill) {
                    rect = new Rect(313, -55, 567, -19);
                }
                break;
            }
            case 8800030: //Easy Zakum Arm 8
            case 8800010: //Normal Zakum Arm 8
            case 8800110: //Chaos Zakum Arm 8
            {
                if (isZakumVerticalArmSkill) {
                    rect = new Rect(395, 50, 525, 86);
                }
                break;
            }
            default:
                rect = new Rect(0, 0, 0, 0);
                break;
        }
        return rect;
    }

    public static boolean isZakumArm(int mobID) {
        //Easy Zakum Arm
        if (mobID >= 8800023 && mobID <= 8800030) {
            return true;
        }
        //Normal Zakum Arm
        if (mobID >= 8800003 && mobID <= 8800010) {
            return true;
        }
        //Chaos Zakum Arm
        else return mobID >= 8800103 && mobID <= 8800110;
    }

    public static boolean isZakumMob(int mobID) {
        switch (mobID) {
            //Easy
            case 8800020:
            case 8800021:
            case 8800022:

            case 8800023:
            case 8800024:
            case 8800025:
            case 8800026:

            case 8800027:
            case 8800028:
            case 8800029:
            case 8800030:
                //Normal
            case 8800000:
            case 8800001:
            case 8800002:

            case 8800003:
            case 8800004:
            case 8800005:
            case 8800006:
            case 8800007:
            case 8800008:
            case 8800009:
            case 8800010:
                //Chaos
            case 8800100:
            case 8800101:
            case 8800102:

            case 8800103:
            case 8800104:
            case 8800105:
            case 8800106:
            case 8800107:
            case 8800108:
            case 8800109:
            case 8800110:
                return true;
            default:
                return false;
        }
    }

    public static boolean isVellum(int mobID) {
        return mobID == 8930100 || mobID == 8930000 || extraVellum.contains(mobID);
    }

    /**
     * dreamtrial-1005: extra templates that use the Vellum mechanics (burrow / breath positions of the 105200410
     * layout), e.g. 夢幻的貝倫 9834718 in the trial copy 993290800. Registered by DreamTrialService from its JSON only
     * while tms282.lucid.dreamTrial is on.
     */
    private static volatile java.util.Set<Integer> extraVellum = java.util.Set.of();

    public static void setExtraVellum(java.util.Set<Integer> ids) {
        extraVellum = java.util.Set.copyOf(ids);
    }

    public static List<Position> vellumStonePositionList = List.of(
            new Position(810, 443),
            new Position(-2190, 443),
            new Position(-1690, 443),
            new Position(560, 443),
            new Position(-190, 443),
            new Position(-690, 443),
            new Position(-1940, 443),
            new Position(1310, 443),
            new Position(-1190, 443),
            new Position(1060, 443),
            new Position(-940, 443),
            new Position(-1440, 443),
            new Position(1560, 443),
            new Position(-440, 443)
    );
}
