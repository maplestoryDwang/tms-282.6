package net.swordie.ms.client.jobs.anima;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.SecondAtom;
import net.swordie.ms.client.jobs.Job;
import net.swordie.ms.constants.JobConstants;

public class Lara extends Job {
    public static final int WIND_SWING_FLY_SKILLID = 80003059;

    // Link Skill
    public static final int NATURE_FRIEND_ORIGIN = 160010001; // Link Skill
    public static final int NATURE_FRIEND_LINKED = 80003058; // Link Skill
    public static final int NATURE_FRIEND_LINKED_BUFF = 80003070; // Link Skill Buff

    // 0th Job
    public static final int SPIRIT_AFFINITY = 160010000;

    // 1st Job
    // Passive
    public static final int MOUNTAIN_KID_PASSIVE = 162000003;
    public static final int RESPONSE = 162000006;
    // Active
    public static final int ESSENCE_SPRINKLE = 162001000;
    public static final int JUMP_FOR_JOY = 162001001; // Flash Jump
    public static final int LEAP_UP = 162001002; // Up Jump
    public static final int MOUNTAIN_KID_ACTIVE = 162001004;
    public static final int PEERLESS_MOUNTAIN = 162001005; // ON/OFF (CTS Val: 686)


    // 2nd Job
    // Passive
    public static final int ERUPTION_HEAVING_RIVER_PASSIVE = 162100002;
    public static final int ERUPTION_WHIRLWIND_PASSIVE = 162100005;
    public static final int ERUPTION_SUNRISE_WELL_PASSIVE = 162100008;
    public static final int WAND_MASTERY = 162100014;
    public static final int FORTUNE_FITNESS = 162100015;
    // Active
    public static final int DRAGON_VEIN_READING = 162101000; // (CTS Val: 684)
    public static final int DRAGON_VEIN_ERUPTION = 162101001;
    public static final int ERUPTION_HEAVING_RIVER_ACTIVE_1 = 162101003; // Summon
    public static final int ERUPTION_HEAVING_RIVER_ACTIVE_2 = 162101004;
    public static final int ERUPTION_WHIRLWIND_ACTIVE_1 = 162101006; // Summon
    public static final int ERUPTION_WHIRLWIND_ACTIVE_2 = 162101007;
    public static final int ERUPTION_SUNRISE_WELL_ACTIVE_1 = 162101009; // Normal Attack
    public static final int ERUPTION_SUNRISE_WELL_ACTIVE_2 = 162101010; // Affected Area (creates second Atoms)
    public static final int ERUPTION_SUNRISE_WELL_ACTIVE_3 = 162101011; // SecondAtom
    public static final int MOUNTAIN_SEEDS = 162101012; // Summon
    public static final int WAND_BOOSTER = 162101013;


    // 3rd Job
    // Passive
    public static final int DRAGON_VEIN_ECHO = 162110007;
    public static final int IMMACULATE_THOUGHT = 162110008;
    public static final int GEOMANCY = 162110009;
    // Active
    public static final int MANIFESTATION_WIND_SWING_ACTIVE_1 = 162111000; // Affected Area
    public static final int MANIFESTATION_WIND_SWING_ACTIVE_2 = 162111001; // Buff Info
    public static final int MANIFESTATION_WHERE_THE_RIVER_COURSES_ACTIVE_1 = 162111002; // SecondAtom
    public static final int MANIFESTATION_WHERE_THE_RIVER_COURSES_ACTIVE_2 = 162111010; // Contains Counter Number
    public static final int MANIFESTATION_SUNLIGHT_FILLED_GROUND_ACTIVE_1 = 162111003; // Affected Area
    public static final int MANIFESTATION_SUNLIGHT_FILLED_GROUND_ACTIVE_2 = 162111004; // Buff Info
    public static final int WAKEUP_CALL = 162111005;
    public static final int DRAGON_VEIN_TRACES = 162111006;


    // 4th Job
    // Passive
    public static final int ABSORPTION_RIVER_PUDDLE_DOUSE_PASSIVE = 162120002;
    public static final int ABSORPTION_FIERCE_WIND_PASSIVE = 162120005;
    public static final int ABSORPTION_SUNLIT_GRAIN_PASSIVE = 162120008;
    public static final int NATURE_MASTER = 162120011;
    public static final int BEST_FRIEND = 162120020;
    public static final int ADVANCED_WAND_MASTERY = 162120025;
    public static final int IN_DEPTH_GEOMANCY = 162120026;
    public static final int INSIGHT = 162120027;
    public static final int SOB = 162120028;
    // Active
    public static final int DRAGON_VEIN_ABSORPTION = 162121000;
    public static final int DRAGON_VEIN_CONVERSION = 162121001;
    public static final int ABSORPTION_RIVER_PUDDLE_DOUSE_ACTIVE_1 = 162121003;
    public static final int ABSORPTION_RIVER_PUDDLE_DOUSE_ACTIVE_2 = 162121004;
    public static final int ABSORPTION_FIERCE_WIND_ACTIVE_1 = 162121006;
    public static final int ABSORPTION_FIERCE_WIND_ACTIVE_2 = 162121007;
    public static final int ABSORPTION_SUNLIT_GRAIN_ACTIVE_1 = 162121009;
    public static final int ABSORPTION_SUNLIT_GRAIN_ACTIVE_2 = 162121010; // Second Atom & Called through extraSkillinfo
    public static final int ERUPTION_HEAVING_RIVER_UPGRADE_4_ACTIVE_1 = 162121012;
    public static final int ERUPTION_HEAVING_RIVER_UPGRADE_4_ACTIVE_2 = 162121013;
    public static final int ERUPTION_HEAVING_RIVER_UPGRADE_4_ACTIVE_3 = 162121014;
    public static final int ERUPTION_WHIRLWIND_UPGRADE_4_ACTIVE_1 = 162121015;
    public static final int ERUPTION_WHIRLWIND_UPGRADE_4_ACTIVE_2 = 162121016;
    public static final int ERUPTION_SUNRISE_WELL_UPGRADE_4_ACTIVE_1 = 162121017;
    public static final int ERUPTION_SUNRISE_WELL_UPGRADE_4_ACTIVE_2 = 162121018;
    public static final int ERUPTION_SUNRISE_WELL_UPGRADE_4_ACTIVE_3 = 162121019; // SecondAtom
    public static final int ESSENCE_SPRINKLE_UPGRADE_4 = 162121021;
    public static final int MOUNTAIN_EMBRACE = 162121022;
    public static final int ANIMA_WARRIOR = 162121023;
    public static final int ANIMA_HERO_WILL = 162121024;


    // Hypers
    // Passive
    public static final int ERUPTION_ABSORPTION_GUARD_BREAK = 162120031;
    public static final int ERUPTION_ABSORPTION_GUARD_REINFORCE = 162120032;
    public static final int ERUPTION_ABSORPTION_GUARD_BOSS_RUSH = 162120033;
    public static final int ESSENCE_SPRINKLE_BOSS_RUSH = 162120034;
    public static final int WAKEUP_CALL_COOLDOWN_CUTTER = 162120035; // Should automatically be handled
    public static final int MOUNTAIN_SEEDS_REINFORCE = 162120036;
    public static final int DRAGON_VEIN_ECHO_ENHANCE = 162120037;
    public static final int MOUNTAIN_EMBRACE_EXTRA_SHIELD = 162120038;
    public static final int DRAGON_VEIN_ENHANCE = 162120039;
    // Active
    public static final int VINE_COIL = 162121041; // 140
    public static final int UNCONSTRAINED_DRAGON_VEIN = 162121042; // 160
    public static final int ARBOR_AWAY_1 = 162121043; // 190
    public static final int ARBOR_AWAY_2 = 162121044; // 190

    // V skills
    public static final int BIG_STRETCH = 400021122;
    public static final int LANDS_CONNECTION = 400021123;
    public static final int LANDS_CONNECTION_PASSIVE_1 = 400021124;
    public static final int LANDS_CONNECTION_PASSIVE_2 = 400021125;
    public static final int LANDS_CONNECTION_PASSIVE_3 = 400021126;
    public static final int LANDS_CONNECTION_PASSIVE_4 = 400021127;
    public static final int LANDS_CONNECTION_PASSIVE_5 = 400021128;
    public static final int SURGING_ESSENCE = 400021129;
    public static final int WINDING_MOUNTAIN_RIDGE_1 = 400021130;
    public static final int WINDING_MOUNTAIN_RIDGE_2 = 400021131;

    public Lara(Char chr) {
        super(chr);
    }

    @Override
    public boolean isHandlerOfJob(short id) {
        return JobConstants.isLara(id);
    }

    public void secondAtomCommandRequest(SecondAtom sa) {

    }
}
