package net.swordie.ms.constants;

import net.swordie.ms.client.jobs.Jianghu.Lynn;
import net.swordie.ms.client.jobs.Jianghu.MoXuan;
import net.swordie.ms.client.jobs.Zero;
import net.swordie.ms.client.jobs.adventurer.Kinesis;
import net.swordie.ms.client.jobs.adventurer.archer.BowMaster;
import net.swordie.ms.client.jobs.adventurer.archer.Marksman;
import net.swordie.ms.client.jobs.adventurer.archer.Pathfinder;
import net.swordie.ms.client.jobs.adventurer.magician.Bishop;
import net.swordie.ms.client.jobs.adventurer.magician.FirePoison;
import net.swordie.ms.client.jobs.adventurer.magician.IceLightning;
import net.swordie.ms.client.jobs.adventurer.pirate.Buccaneer;
import net.swordie.ms.client.jobs.adventurer.pirate.Cannoneer;
import net.swordie.ms.client.jobs.adventurer.pirate.Corsair;
import net.swordie.ms.client.jobs.adventurer.thief.DualBlade;
import net.swordie.ms.client.jobs.adventurer.thief.NightLord;
import net.swordie.ms.client.jobs.adventurer.thief.Shadower;
import net.swordie.ms.client.jobs.adventurer.warrior.DarkKnight;
import net.swordie.ms.client.jobs.adventurer.warrior.Hero;
import net.swordie.ms.client.jobs.adventurer.warrior.Paladin;
import net.swordie.ms.client.jobs.anima.HoYoung;
import net.swordie.ms.client.jobs.anima.Lara;
import net.swordie.ms.client.jobs.anima.Ren;
import net.swordie.ms.client.jobs.cygnus.*;
import net.swordie.ms.client.jobs.flora.Adele;
import net.swordie.ms.client.jobs.flora.Ark;
import net.swordie.ms.client.jobs.flora.Illium;
import net.swordie.ms.client.jobs.flora.Khali;
import net.swordie.ms.client.jobs.legend.*;
import net.swordie.ms.client.jobs.nova.AngelicBuster;
import net.swordie.ms.client.jobs.nova.Cadena;
import net.swordie.ms.client.jobs.nova.Kain;
import net.swordie.ms.client.jobs.nova.Kaiser;
import net.swordie.ms.client.jobs.resistance.*;
import net.swordie.ms.client.jobs.resistance.demon.DemonAvenger;
import net.swordie.ms.client.jobs.resistance.demon.DemonSlayer;
import net.swordie.ms.client.jobs.sengoku.Hayato;
import net.swordie.ms.client.jobs.sengoku.Kanna;
import net.swordie.ms.client.jobs.shine.SiaAstelle;
import net.swordie.ms.loaders.Etc.VCore.VCore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MatrixConstants {
    // Etc.wz/VMatrixOption.img
    public static final int SLOT_MAX = 500;
    public static final int EQUIP_SLOT_MIN = 4;
    public static final int EQUIP_SLOT_MAX = 15;
    public static final int SPECIAL_SLOT_MAX = 1;
    public static final int EXTEND_LEVEL = 5;
    public static final int EXTEND_AF = 0;
    public static final int GRADE_MAX = 25;
    public static final int TOTAL_GRADE_MAX = 50;
    public static final int CRAFT_SKILL_CORE_COST = 140;
    public static final int CRAFT_ENCHANT_CORE_COST = 70;
    public static final int CRAFT_SPECIAL_CORE_COST = 250;
    public static final int CRAFT_GEMSTONE_COST = 35;
    public static final int MATRIX_POINT_RESET_MESO = 1000000;
    public static final int EQUIP_SLOT_ENHANCE_MAX = 5;
    public static final int MATRIX_SHARDS = 1477;
    // slotExpansionMeso can be helpful

    public static final int MAX_NODE_SLOTS = 30;
    public static final int[] REQ_LV_BY_MATRIX_SLOT_POS = new int[] {
            200, // 0
            200, // 1
            200, // 2
            200, // 3
            200, // 4

            200, // 5
            200, // 6
            200, // 7

            200, // 8
            200, // 9
            200, // 10
            200, // 11

            205, // 12
            210, // 13
            210, // 14

            215, // 15
            220, // 16
            220, // 17
            225, // 18

            230, // 19
            230, // 20
            235, // 21
            240, // 22
            240, // 23
            245, // 24

            250, // 25
            250, // 26
            255, // 27
            260, // 28
            260  // 29
    };

    public static final int GIANT_ROBOT_SG_88 = 35121003;

    public static boolean isMatrixSkillBoost(int skillID) {
        return skillID / 1000 == 400004;
    }

    public static boolean isMatrixJoblessSkill(int skillID) {
        return skillID <= 400001057 && skillID >= 400001000;
    }

    public static int getIncShard(int coreID, int skillLevel) {
        int shard = 0;
        if (VCore.isSkillNode(coreID)) {
            shard = VCore.getEnforceOption(VCore.SKILL).get(skillLevel).getExtract();
        } else if (VCore.isBoostNode(coreID)) {
            shard = VCore.getEnforceOption(VCore.BOOST).get(skillLevel).getExtract();
        } else if (VCore.isSpecialNode(coreID)) {
            shard = VCore.getEnforceOption(VCore.SPECIAL).get(skillLevel).getExtract();
        } else if (VCore.isExpNode(coreID)) {
            shard = VCore.getEnforceOption(VCore.EXP).get(skillLevel).getExtract();
        }
        return shard;
    }

    public static int getSlotsByLevel(int level) {
        int slotByLevel = level / 5 - 40;
        return Math.min(slotByLevel, EQUIP_SLOT_MAX);
    }

    private static Map<Short, List<Integer>> VSkillsToGiveUponReachingV = new HashMap<>(){{
        put(JobConstants.JobEnum.HERO.getJobId(), new ArrayList<>(){{
            add(Hero.BURNING_SOUL_BLADE);
            add(Hero.WORLDREAVER);
            add(Hero.COMBO_INSTINCT);
            add(Hero.SWORD_ILLUSION);
        }});

        put(JobConstants.JobEnum.PALADIN.getJobId(), new ArrayList<>(){{
            add(Paladin.DIVINE_ECHO);
            add(Paladin.HAMMERS_OF_THE_RIGHTEOUS);
            add(Paladin.GRAND_GUARDIAN);
            add(Paladin.MIGHTY_MJOLNIR);
        }});

        put(JobConstants.JobEnum.DARKKNIGHT.getJobId(), new ArrayList<>(){{
            add(DarkKnight.SPEAR_OF_DARKNESS);
            add(DarkKnight.RADIANT_EVIL);
            add(DarkKnight.CALAMITOUS_CYCLONE);
            add(DarkKnight.DARKNESS_AURA);
        }});


        put(JobConstants.JobEnum.BOWMASTER.getJobId(), new ArrayList<>(){{
            add(BowMaster.STORM_OF_ARROWS);
            add(BowMaster.INHUMAN_SPEED);
            add(BowMaster.QUIVER_BARRAGE);
            add(BowMaster.SILHOUETTE_MIRAGE);
        }});

        put(JobConstants.JobEnum.MARKSMAN.getJobId(), new ArrayList<>(){{
            add(Marksman.PERFECT_SHOT);
            add(Marksman.SPLIT_SHOT);
            add(Marksman.SURGE_BOLT);
            add(Marksman.REPEATING_CROSSBOW_CARTRIDGE);
        }});

        put(JobConstants.JobEnum.PATHFINDER_4.getJobId(), new ArrayList<>(){{
            add(Pathfinder.NOVA_BLAST);
            add(Pathfinder.RAVEN_TEMPEST);
            add(Pathfinder.OBSIDIAN_BARRIER_NONE);
            add(Pathfinder.RELIC_UNBOUND);
        }});

        put(JobConstants.JobEnum.BISHOP.getJobId(), new ArrayList<>(){{
            add(Bishop.BENEDICTION);
            add(Bishop.ANGEL_OF_BALANCE_BENEVOLENCE);
            add(Bishop.PEACEMAKER_TRAVEL);
            add(Bishop.DIVINE_PUNIHSMENT);
        }});

        put(JobConstants.JobEnum.FP_ARCHMAGE.getJobId(), new ArrayList<>(){{
            add(FirePoison.DOT_PUNISHER);
            add(FirePoison.POISON_NOVA);
            add(FirePoison.ELEMENTAL_FURY);
            add(FirePoison.POISON_CHAIN_1);
        }});

        put(JobConstants.JobEnum.IL_ARCHMAGE.getJobId(), new ArrayList<>(){{
            add(IceLightning.ICE_AGE);
            add(IceLightning.BOLT_BARRAGE);
            add(IceLightning.SPIRIT_OF_SNOW);
            add(IceLightning.JUPITER_THUNDER);
        }});

        put(JobConstants.JobEnum.NIGHTLORD.getJobId(), new ArrayList<>(){{
            add(NightLord.THROWING_STAR_BARRAGE);
            add(NightLord.SHURRIKANE);
            add(NightLord.DARK_LORDS_OMEN);
            add(NightLord.THROW_BLASTING_BUFF);
        }});

        put(JobConstants.JobEnum.SHADOWER.getJobId(), new ArrayList<>(){{
            add(Shadower.SHADOW_ASSAULT);
            add(Shadower.TRICKBLADE);
            add(Shadower.SONIC_BLOW);
            add(Shadower.SLASH_SHADOW_FORMATION);
        }});

        put(JobConstants.JobEnum.BLADE_MASTER.getJobId(), new ArrayList<>(){{
            add(DualBlade.BLADE_TEMPEST);
            add(DualBlade.BLADES_OF_DESTINY);
            add(DualBlade.BLADE_TORNADO);
            add(DualBlade.HAUNTED_EDGE);
        }});

        put(JobConstants.JobEnum.BUCCANEER.getJobId(), new ArrayList<>(){{
            add(Buccaneer.LIGHTING_FORM);
            add(Buccaneer.LORD_OF_THE_DEEP);
            add(Buccaneer.SERPENT_VORTEX);
            add(Buccaneer.HOWLING_FIST);
        }});

        put(JobConstants.JobEnum.CORSAIR.getJobId(), new ArrayList<>(){{
            add(Corsair.BULLET_BARRAGE);
            add(Corsair.TARGET_LOCK);
            add(Corsair.NAUTILUS_ASSAULT);
            add(Corsair.DEATH_TRIGGER);
        }});

        put(JobConstants.JobEnum.CANNON_MASTER.getJobId(), new ArrayList<>(){{
            add(Cannoneer.BIG_HUGE_GIGANTIC_ROCKET);
            add(Cannoneer.NUCLEAR_OPTION);
            add(Cannoneer.SPECIAL_MONKEY_SIDEKICK);
            add(Cannoneer.POOLMAKER);
        }});

        put(JobConstants.JobEnum.DAWNWARRIOR4.getJobId(), new ArrayList<>(){{
            add(DawnWarrior.RIFT_OF_DAMNATION);
            add(DawnWarrior.SOUL_ECLIPSE );
            add(DawnWarrior.FLARE_SLASH_SUN);
            add(DawnWarrior.COSMOS);
        }});

        put(JobConstants.JobEnum.BLAZEWIZARD4.getJobId(), new ArrayList<>(){{
            add(BlazeWizard.ORBITAL_INFERNO);
            add(BlazeWizard.SAVAGE_FLAME);
            add(BlazeWizard.INFERNO_SPHERE);
            add(BlazeWizard.SALAMANDER_MISCHIEF);
        }});

        put(JobConstants.JobEnum.WINDARCHER4.getJobId(), new ArrayList<>(){{
            add(WindArcher.HOWLING_GALE);
            add(WindArcher.MERCILESS_WINDS);
            add(WindArcher.GALE_BARRIER);
            add(WindArcher.VORTEX_SPHERE);
        }});

        put(JobConstants.JobEnum.NIGHTWALKER4.getJobId(), new ArrayList<>(){{
            add(NightWalker.SHADOW_SPEAR);
            add(NightWalker.GREATER_DARK_SERVANT);
            add(NightWalker.SHADOW_BITE);
            add(NightWalker.RAPID_THROW);
        }});

        put(JobConstants.JobEnum.THUNDERBREAKER4.getJobId(), new ArrayList<>(){{
            add(ThunderBreaker.LIGHTNING_CASCADE);
            add(ThunderBreaker.SHARK_TORPEDO);
            add(ThunderBreaker.TRIDENT_STRIKE);
            add(ThunderBreaker.LIGHTNING_SPEAR_MULTISTRIKE_SKILL);
        }});

        put(JobConstants.JobEnum.ARAN4.getJobId(), new ArrayList<>(){{
            add(Aran.MAHAS_FURY_BUFF);
            add(Aran.MAHAS_CARNAGE);
            add(Aran.FENRIR_CRASH);
            add(Aran.BLIZZARD_TEMPEST);
        }});

        put(JobConstants.JobEnum.EVAN4.getJobId(), new ArrayList<>(){{
            add(Evan.ELEMENTAL_BARRAGE);
            add(Evan.DRAGON_SLAM);
            add(Evan.ELEMENTAL_RADIANCE);
            add(Evan.SPIRAL_OF_MANA);
        }});

        put(JobConstants.JobEnum.MERCEDES4.getJobId(), new ArrayList<>(){{
            add(Mercedes.SPIRIT_OF_ELLUEL);
            add(Mercedes.SYLVIDIAS_FLIGHT);
            add(Mercedes.IRKILLAS_WRATH);
            add(Mercedes.ROYAL_KNIGHTS);
        }});

        put(JobConstants.JobEnum.PHANTOM4.getJobId(), new ArrayList<>(){{
            add(Phantom.LUCK_OF_THE_DRAW);
            add(Phantom.ACE_IN_THE_HOLE);
            add(Phantom.PHANTOMS_MARK);
            add(Phantom.RIFT_BREAK_TELEPORT);
        }});

        put(JobConstants.JobEnum.SHADE4.getJobId(), new ArrayList<>(){{
            add(Shade.SPIRIT_FLOW);
            add(Shade.SPIRITGATE_SUMMONER);
            add(Shade.TRUE_SPIRIT_CLAW);
            add(Shade.SMASHING_MULTIPUNCH_KEYDOWN);
        }});

        put(JobConstants.JobEnum.LUMINOUS4.getJobId(), new ArrayList<>(){{
            add(Luminous.GATE_OF_LIGHT);
            add(Luminous.AETHER_CONDUIT_L);
            add(Luminous.BAPTISM_OF_LIGHT_AND_DARKNESS);
            add(Luminous.LIBERATION_ORB);
        }});

        put(JobConstants.JobEnum.DEMON_SLAYER4.getJobId(), new ArrayList<>(){{
            add(DemonSlayer.DEMON_AWAKENING);
            add(DemonSlayer.SPIRIT_OF_RAGE);
            add(DemonSlayer.ORTHRUS);
            add(DemonSlayer.DEMON_BANE);
        }});

        put(JobConstants.JobEnum.DEMON_AVENGER4.getJobId(), new ArrayList<>(){{
            add(DemonAvenger.DEMONIC_FRENZY);
            add(DemonAvenger.DEMONIC_BLAST_HOLDDOWN);
            add(DemonAvenger.DIMENSIONAL_SWORD_SUMMON);
            add(DemonAvenger.REVENANT);
        }});

        put(JobConstants.JobEnum.BATTLE_MAGE_4.getJobId(), new ArrayList<>(){{
            add(BattleMage.AURA_SCYTHE);
            add(BattleMage.ALTAR_OF_ANNIHILATION);
            add(BattleMage.GRIM_HARVEST);
            add(BattleMage.ABYSSAL_LIGHTNING);
        }});

        put(JobConstants.JobEnum.WILD_HUNTER_4.getJobId(), new ArrayList<>(){{
            add(WildHunter.JAGUAR_STORM);
            add(WildHunter.PRIMAL_FURY);
            add(WildHunter.PRIMAL_GRENADE);
            add(WildHunter.WILD_ARROW_BLAST_TYPE_X);
        }});

        put(JobConstants.JobEnum.MECHANIC_4.getJobId(), new ArrayList<>(){{
            add(Mechanic.MULTIPURPOSE_BOT_MFL);
            add(Mechanic.MOBILE_MISSILE_BATTERY);
            add(Mechanic.FULL_METAL_BARRAGE);
            add(Mechanic.MECHA_CARRIER);
        }});

        put(JobConstants.JobEnum.XENON4.getJobId(), new ArrayList<>(){{
            add(Xenon.OMEGA_BLASTER);
            add(Xenon.CORE_OVERLOAD_BUFF);
            add(Xenon.HYPOGRAM_FIELD_FUSION);
            add(Xenon.PHOTON_RAY);
        }});

        put(JobConstants.JobEnum.BLASTER_4.getJobId(), new ArrayList<>(){{
            add(Blaster.ROCKET_PUNCH);
            add(Blaster.GATLING_PUNCH);
            add(Blaster.BULLET_BLAST);
            add(Blaster.AFTERIMAGE_SHOCK_1);
        }});

        put(JobConstants.JobEnum.HAYATO4.getJobId(), new ArrayList<>(){{
            add(Hayato.BATTOUJUTSU_ZANKOU);
            add(Hayato.IAIJUTSU_PHANTOM_BLADE);
            add(Hayato.BATTOUJUTSU_ULTIMATE_WILL);
            add(Hayato.INSTANT_SLICE);
        }});

        put(JobConstants.JobEnum.KANNA4.getJobId(), new ArrayList<>(){{
            add(Kanna.YUKI_MUSUME_SHOUKAN);
            add(Kanna.SPIRITS_DOMAIN);
            add(Kanna.LIBERATED_SPIRIT_CIRCLE_SMALL);
            add(Kanna.GHOST_YAKSHA_GREAT_ONI_LORD_LEGION);
        }});

        put(JobConstants.JobEnum.MIHILE4.getJobId(), new ArrayList<>(){{
            add(Mihile.SHIELD_OF_LIGHT);
            add(Mihile.SWORD_OF_LIGHT);
            add(Mihile.RADIANT_SOUL);
            add(Mihile.LIGHT_OF_COURAGE_BUFF);
        }});

        put(JobConstants.JobEnum.KAISER4.getJobId(), new ArrayList<>(){{
            add(Kaiser.NOVA_GUARDIANS);
            add(Kaiser.BLADEFALL_ATTACK);
            add(Kaiser.DRACO_SURGE_ATTACK);
            add(Kaiser.DRAGON_BLAZE);
        }});

        put(JobConstants.JobEnum.KAIN_4.getJobId(), new ArrayList<>(){{
            add(Kain.DRAGON_BURST);
            add(Kain.FATAL_BLITZ);
            add(Kain.THANATOS_DESCENT);
            add(Kain.GRIP_OF_AGONY);
        }});

        put(JobConstants.JobEnum.CADENA_4.getJobId(), new ArrayList<>(){{
            add(Cadena.APOCALYPSE_CANNON_SUMMON);
            add(Cadena.CHAIN_ARTS_VOID_STRIKE_BUFF);
            add(Cadena.CHAIN_ARTS_MAELSTROM);
            add(Cadena.MUSCLE_MEMORY_FINALE);
        }});

        put(JobConstants.JobEnum.ANGELIC_BUSTER4.getJobId(), new ArrayList<>(){{
            add(AngelicBuster.SPARKLE_BURST);
            add(AngelicBuster.SUPER_STAR_SPOTLIGHT);
            add(AngelicBuster.MIGHTY_MASCOT);
            add(AngelicBuster.TRINITY_FUSION);
        }});

        put(JobConstants.JobEnum.ZERO4.getJobId(), new ArrayList<>(){{
            add(Zero.CHRONO_BREAK);
            add(Zero.TWIN_BLADES_OF_TIME_START);
            add(Zero.SHADOW_FLASH_ALPHA_TILE);
            add(Zero.EGO_WEAPON_ALPHA);
        }});

        put(JobConstants.JobEnum.KINESIS_4.getJobId(), new ArrayList<>(){{
            add(Kinesis.PSYCHIC_TORNADO);
            add(Kinesis.MIND_OVER_MATTER);
            add(Kinesis.ULTIMATE_PSYCHIC_SHOCKWAVE);
            add(Kinesis.LAW_OF_GRAVITY);
        }});

        put(JobConstants.JobEnum.ADELE_4.getJobId(), new ArrayList<>(){{
            add(Adele.RUIN);
            add(Adele.INFINITY_BLADE);
            add(Adele.LEGACY_RESTORATION);
            add(Adele.STORM);
        }});

        put(JobConstants.JobEnum.ILLIUM_4.getJobId(), new ArrayList<>(){{
            add(Illium.CRYSTAL_IGNITION);
            add(Illium.TEMPLAR_KNIGHT);
            add(Illium.CRYSTALLINE_SPIRIT);
            add(Illium.CRYSTAL_GATE);
        }});

        put(JobConstants.JobEnum.ARK_4.getJobId(), new ArrayList<>(){{
            add(Ark.INFINITY_SPELL);
            add(Ark.ABYSSAL_RECALL);
            add(Ark.NIGHTMARES_ESCAPE);
            add(Ark.ENDLESSLY_STARVING_BEAST);
        }});

        put(JobConstants.JobEnum.LARA_4.getJobId(), new ArrayList<>(){{
            add(Lara.BIG_STRETCH);
            add(Lara.LANDS_CONNECTION);
            add(Lara.SURGING_ESSENCE);
            add(Lara.WINDING_MOUNTAIN_RIDGE_1);
        }});

        put(JobConstants.JobEnum.HOYOUNG_4.getJobId(), new ArrayList<>(){{
            add(HoYoung.CLONE_RAMPAGE);
            add(HoYoung.SAGE_TIGER_OF_SONGYU);
            add(HoYoung.SAGE_WRATH_OF_GODS);
            add(HoYoung.THREE_PATHS_APPARITION_BUFF);
        }});

        put(JobConstants.JobEnum.KHALI_4.getJobId(), new ArrayList<>(){{
            add(Khali.HEX_PANDEMONIUM);
            add(Khali.VOID_BURST);
            add(Khali.ARTS_ASTRA);
            add(Khali.RESONATE_ULTIMATUM);
        }});

        put(JobConstants.JobEnum.LYNN_4.getJobId(), new ArrayList<>(){{
            add(Lynn.BEAST_RAGE);
            add(Lynn.BEAK_STRIKE);
            add(Lynn.FOCUS_AWAKEN);
            add(Lynn.NATURE_GRACE);
        }});

        put(JobConstants.JobEnum.MOXUAN_4.getJobId(), new ArrayList<>(){{
            add(MoXuan.SOUL_ART_BENEATH_HEAVEN);
            add(MoXuan.SOUL_ART_THE_CONQUERED_SELF);
            add(MoXuan.SOUL_ART_THE_OPENED_GATE);
            add(MoXuan.DIVINE_ART_CRASHING_EARTH);
        }});

        put(JobConstants.JobEnum.SIA_4.getJobId(), new ArrayList<>(){{
            add(SiaAstelle.SHINE);
            add(SiaAstelle.STELLAR_XI_SIRIUS);
            add(SiaAstelle.STELLAR_XII_SADALSUUD);
            add(SiaAstelle.SAVIOR_CIRCLE);
        }});

        put(JobConstants.JobEnum.REN_4.getJobId(), new ArrayList<>(){{
            add(Ren.THOUSAND_BLOSSOM_FLURRY);
            add(Ren.SOUL_IMMEASURABLE);
            add(Ren.DANCING_ANNIHILATION);
            add(Ren.FINAL_IMUGI_SPIRIT_SWORD_BLADE_OF_THE_UNBOUND_HEART_ATT);
        }});
    }};

    public static List<Integer> GetVSkillsToGiveUponReachingV(int jobId) {
        for (var entry : VSkillsToGiveUponReachingV.entrySet()) {
            if (entry.getKey() == jobId) {
                return entry.getValue();
            }
        }
        return new ArrayList<>();
    }
}
