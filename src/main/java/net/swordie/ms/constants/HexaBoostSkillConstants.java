package net.swordie.ms.constants;

import net.swordie.ms.client.jobs.Jianghu.MoXuan;
import net.swordie.ms.client.jobs.adventurer.archer.BowMaster;
import net.swordie.ms.client.jobs.adventurer.magician.Bishop;
import net.swordie.ms.client.jobs.adventurer.pirate.Cannoneer;
import net.swordie.ms.client.jobs.adventurer.thief.Shadower;
import net.swordie.ms.client.jobs.adventurer.warrior.Paladin;
import net.swordie.ms.client.jobs.anima.HoYoung;
import net.swordie.ms.client.jobs.cygnus.Mihile;
import net.swordie.ms.client.jobs.cygnus.NightWalker;
import net.swordie.ms.client.jobs.cygnus.ThunderBreaker;
import net.swordie.ms.client.jobs.cygnus.WindArcher;
import net.swordie.ms.client.jobs.flora.Adele;
import net.swordie.ms.client.jobs.flora.Ark;
import net.swordie.ms.client.jobs.legend.Mercedes;
import net.swordie.ms.client.jobs.resistance.Xenon;
import net.swordie.ms.client.jobs.resistance.demon.DemonAvenger;
import net.swordie.ms.client.jobs.sengoku.Kanna;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Maps hexa matrix boost skills (50006xxxx) to their base V skills.
 * Skill parameters (damage, MP, cooldown, etc.) are read from SkillData / GUI overrides,
 * not hardcoded here.
 */
public final class HexaBoostSkillConstants {

    private static final Map<Integer, Integer> HEXA_TO_BASE = new LinkedHashMap<>();
    private static final Map<Integer, Integer> BOOST_CORE_TO_SKILL = new LinkedHashMap<>();
    private static final Map<Integer, Integer> BASE_TO_BOOST_CORE = new LinkedHashMap<>();

    static {
        register(Paladin.HEXA_DIVINE_ECHO, Paladin.DIVINE_ECHO);
        register(Paladin.HEXA_DIVINE_ECHO_MIMIC, Paladin.DIVINE_ECHO_MIMIC);

        register(Bishop.HEXA_BENEDICTION, Bishop.BENEDICTION);

        register(NightWalker.HEXA_GREATER_DARK_SERVANT, NightWalker.GREATER_DARK_SERVANT);
        register(NightWalker.HEXA_SHADOW_BITE, NightWalker.SHADOW_BITE);

        register(Mihile.HEXA_SHIELD_OF_LIGHT, Mihile.SHIELD_OF_LIGHT);
        register(Mihile.HEXA_LIGHT_OF_COURAGE_BUFF, Mihile.LIGHT_OF_COURAGE_BUFF);
        register(Mihile.HEXA_LIGHT_OF_COURAGE_SI, Mihile.LIGHT_OF_COURAGE_SI);

        register(Ark.HEXA_INFINITY_SPELL, Ark.INFINITY_SPELL);

        register(HoYoung.HEXA_CLONE_RAMPAGE, HoYoung.CLONE_RAMPAGE);
        register(HoYoung.HEXA_CLONE_RAMPAGE_ATOM, HoYoung.CLONE_RAMPAGE_ATOM);

        register(BowMaster.HEXA_SILHOUETTE_MIRAGE, BowMaster.SILHOUETTE_MIRAGE);
        register(BowMaster.HEXA_SILHOUETTE_MIRAGE_ATTACK, BowMaster.SILHOUETTE_MIRAGE_ATTACK);

        register(Shadower.HEXA_SHADOW_ASSAULT_5, Shadower.SHADOW_ASSAULT);
        register(Shadower.HEXA_SHADOW_ASSAULT_6, Shadower.SHADOW_ASSAULT);

        register(Cannoneer.HEXA_POOLMAKER, Cannoneer.POOLMAKER);
        register(Cannoneer.HEXA_POOLMAKER_ATTACK, Cannoneer.POOLMAKER_ATTACK);
        register(Cannoneer.HEXA_POOLMAKER_AA, Cannoneer.POOLMAKER_AA);
        register(Cannoneer.HEXA_POOLMAKER_CHEST_BUFF, Cannoneer.POOLMAKER_CHEST_BUFF);

        register(WindArcher.HEXA_GALE_BARRIER, WindArcher.GALE_BARRIER);
        register(WindArcher.HEXA_GALE_BARRIER_ATOM, WindArcher.GALE_BARRIER_ATOM);

        register(ThunderBreaker.HEXA_LIGHTNING_CASCADE, ThunderBreaker.LIGHTNING_CASCADE);
        register(ThunderBreaker.HEXA_LIGHTNING_CASCADE_ATTACK, ThunderBreaker.LIGHTNING_CASCADE_ATTACK);

        register(Mercedes.HEXA_SPIRIT_OF_ELLUEL, Mercedes.SPIRIT_OF_ELLUEL);
        register(Mercedes.HEXA_SPIRIT_OF_ELLUEL_1, Mercedes.SPIRIT_OF_ELLUEL_1);
        register(Mercedes.HEXA_SPIRIT_OF_ELLUEL_2, Mercedes.SPIRIT_OF_ELLUEL_2);

        register(DemonAvenger.HEXA_REVENANT, DemonAvenger.REVENANT);
        register(DemonAvenger.HEXA_THORN_OF_FURY_1, DemonAvenger.THORN_OF_FURY_1);
        register(DemonAvenger.HEXA_THORN_OF_FURY_2, DemonAvenger.THORN_OF_FURY_2);
        register(DemonAvenger.HEXA_THORN_OF_FURY_3, DemonAvenger.THORN_OF_FURY_3);
        register(DemonAvenger.HEXA_REVENANT_END, DemonAvenger.REVENANT_END);

        register(Xenon.CORE_OVERLOAD_BOOST_SKILL, Xenon.CORE_OVERLOAD_ATTACK);

        register(Adele.HEXA_LEGACY_RESTORATION, Adele.LEGACY_RESTORATION);

        register(Kanna.HEXA_SPIRITS_DOMAIN, Kanna.SPIRITS_DOMAIN);

        register(MoXuan.HEXA_SOUL_ART_THE_OPENED_GATE, MoXuan.SOUL_ART_THE_OPENED_GATE);
        registerBoostCore(MoXuan.HEXA_BOOST_OPENED_GATE_CORE, MoXuan.HEXA_SOUL_ART_THE_OPENED_GATE, MoXuan.SOUL_ART_THE_OPENED_GATE);
    }

    private HexaBoostSkillConstants() {
    }

    private static void register(int hexaSkillId, int baseSkillId) {
        HEXA_TO_BASE.put(hexaSkillId, baseSkillId);
    }

    private static void registerBoostCore(int boostCoreId, int boostSkillId, int baseSkillId) {
        BOOST_CORE_TO_SKILL.put(boostCoreId, boostSkillId);
        BASE_TO_BOOST_CORE.put(baseSkillId, boostCoreId);
        register(boostSkillId, baseSkillId);
    }

    public static int getBoostCoreId(int baseSkillId) {
        return BASE_TO_BOOST_CORE.getOrDefault(baseSkillId, 0);
    }

    public static int getBoostSkillId(int boostCoreId) {
        return BOOST_CORE_TO_SKILL.getOrDefault(boostCoreId, 0);
    }

    /**
     * Returns the hexa boost level for a base V skill, from matrix core level or granted boost skill level.
     */
    public static int getHexaBoostLevel(net.swordie.ms.client.character.Char chr, int baseSkillId) {
        int coreId = getBoostCoreId(baseSkillId);
        if (coreId > 0) {
            int coreLevel = chr.getHexaSkillLevel(coreId);
            if (coreLevel > 0) {
                return coreLevel;
            }
        }
        int boostSkillId = getBoostSkillId(getBoostCoreId(baseSkillId));
        if (boostSkillId > 0 && chr.hasSkill(boostSkillId)) {
            return chr.getSkillLevel(boostSkillId);
        }
        return 0;
    }

    public static boolean isHexaBoostSkill(int skillId) {
        return skillId / 10000 == 50006 || HEXA_TO_BASE.containsKey(skillId);
    }

    public static int getBaseSkillId(int skillId) {
        return HEXA_TO_BASE.getOrDefault(skillId, skillId);
    }

    public static Map<Integer, Integer> allMappings() {
        return Collections.unmodifiableMap(HEXA_TO_BASE);
    }
}
