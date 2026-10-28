package net.swordie.ms.client.jobs;

import net.swordie.ms.ServerConfig;
import net.swordie.ms.client.Account;
import net.swordie.ms.client.Client;
import net.swordie.ms.client.LinkSkill;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime;
import net.swordie.ms.client.character.tms282.Tms282VMatrixSpecialEngine;
import net.swordie.ms.client.character.CharacterStat;
import net.swordie.ms.client.character.ExtraTMSSystem;
import net.swordie.ms.client.character.achievement.AchievementHandler;
import net.swordie.ms.client.character.info.HitInfo;
import net.swordie.ms.client.character.items.BodyPart;
import net.swordie.ms.client.character.items.Equip;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.client.character.runestones.RuneStone;
import net.swordie.ms.client.character.skills.*;
import net.swordie.ms.client.character.skills.info.*;
import net.swordie.ms.client.character.skills.matrix.MatrixCore;
import net.swordie.ms.client.character.skills.matrix.MatrixSlot;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TemporaryStatBase;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.client.jobs.adventurer.Beginner;
import net.swordie.ms.client.jobs.adventurer.magician.Bishop;
import net.swordie.ms.client.jobs.adventurer.magician.FirePoison;
import net.swordie.ms.client.jobs.adventurer.magician.IceLightning;
import net.swordie.ms.client.jobs.adventurer.thief.DualBlade;
import net.swordie.ms.client.jobs.adventurer.thief.Shadower;
import net.swordie.ms.client.jobs.adventurer.warrior.DarkKnight;
import net.swordie.ms.client.jobs.adventurer.warrior.Paladin;
import net.swordie.ms.client.jobs.adventurer.warrior.Warrior;
import net.swordie.ms.client.jobs.cygnus.*;
import net.swordie.ms.client.jobs.flora.Illium;
import net.swordie.ms.client.jobs.legend.Phantom;
import net.swordie.ms.client.jobs.resistance.BattleMage;
import net.swordie.ms.client.jobs.resistance.Xenon;
import net.swordie.ms.client.social.Guild.GuildSkill;
import net.swordie.ms.client.social.Party.Party;
import net.swordie.ms.client.social.Party.PartyMember;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.*;
import net.swordie.ms.constants.*;
import net.swordie.ms.enums.*;
import net.swordie.ms.handlers.GlobalTimerManager;
import net.swordie.ms.handlers.user.SpecialHPBossHandler;
import net.swordie.ms.life.AffectedArea;
import net.swordie.ms.life.Life;
import net.swordie.ms.life.RandomPortal;
import net.swordie.ms.life.Summon;
import net.swordie.ms.life.drop.Drop;
import net.swordie.ms.life.mob.*;
import net.swordie.ms.life.mob.skill.BurnedInfo;
import net.swordie.ms.life.mob.skill.MobSkillID;
import net.swordie.ms.life.pet.Pet;
import net.swordie.ms.loaders.Etc.SetItemInfo.SetItemInfoData;
import net.swordie.ms.loaders.Etc.VCore.VCore;
import net.swordie.ms.loaders.Etc.VCore.VCoreData;
import net.swordie.ms.loaders.FieldData;
import net.swordie.ms.loaders.ItemData;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.loaders.StringData;
import net.swordie.ms.loaders.containerclasses.PetInfo;
import net.swordie.ms.scripts.ExpandMenuHelper;
import net.swordie.ms.scripts.ScriptType;
import net.swordie.ms.util.*;
import net.swordie.ms.util.container.Tuple;
import net.swordie.ms.world.boss.BossHelper;
import net.swordie.ms.world.field.Field;
import net.swordie.ms.world.field.Foothold;

import java.util.*;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static net.swordie.ms.client.character.skills.SkillStat.*;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;
import static net.swordie.ms.client.jobs.resistance.Mechanic.MULTIPURPOSE_BOT_MFL;
import static net.swordie.ms.constants.SkillConstants.*;
import static net.swordie.ms.enums.InvType.CONSUME;
import static net.swordie.ms.enums.InvType.EQUIPPED;

public abstract class Job {

    private static final int NEW_CHARACTER_WHITE_POTION = 2000002;
    private static final int NEW_CHARACTER_MANA_POTION = 2000006;
    private static final int NEW_CHARACTER_PERMANENT_POWER_ELIXIR = 2001505;
    private static final int NEW_CHARACTER_MENU_CONSUMABLE = 2432022;
    private static final int NEW_CHARACTER_EQUIPMENT_GIFT_BOX = 2433509;

    protected Char chr;
    protected Client c;

    public static final int HYPER_STAT_ARCANE_FORCE = 80000421;

    public static final int DECENT_HASTE = 8000;
    public static final int DECENT_MYSTIC_DOOR = 8001;
    public static final int DECENT_SHARP_EYES = 8002;
    public static final int DECENT_HYPER_BODY = 8003;
    public static final int DECENT_COMBAT_ORDERS = 8004;
    public static final int DECENT_ADVANCED_BLESSING = 8005;
    public static final int DECENT_SPEED_INFUSION = 8006;

    public static final int MONOLITH = 80011261;
    public static final int FURY_TOTEM = 80011824;

    /** Ownership only; callers retain instance, cooldown and summon checks. TMS1202193 grants MONOLITH. */
    public static boolean ownsSpawnTotem(int skillId, net.swordie.ms.connection.protocol.ProtocolVersion version,
                                        java.util.function.IntPredicate ownsItem) {
        return (skillId == MONOLITH || skillId == FURY_TOTEM)
                && (ownsItem.test(1202236) || ownsItem.test(1202267)
                || (version == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3
                && skillId == MONOLITH && ownsItem.test(1202193)));
    }
    public static final int ELEMENTAL_SYLPH = 80001518;
    public static final int FLAME_SYLPH = 80001519;
    public static final int THUNDER_SYLPH = 80001520;
    public static final int ICE_SYLPH = 80001521;
    public static final int EARTH_SYLPH = 80001522;
    public static final int DARK_SYLPH = 80001523;
    public static final int HOLY_SYLPH = 80001524;
    public static final int SALAMANDER_SYLPH = 80001525;
    public static final int ELECTRON_SYLPH = 80001526;
    public static final int UNDINE_SYLPH = 80001527;
    public static final int GNOME_SYLPH = 80001528;
    public static final int DEVIL_SYLPH = 80001529;
    public static final int ANGEL_SYLPH = 80001530;
    public static final int ELEMENTAL_SYLPH_2 = 80001715;
    public static final int FLAME_SYLPH_2 = 80001716;
    public static final int THUNDER_SYLPH_2 = 80001717;
    public static final int ICE_SYLPH_2 = 80001718;
    public static final int EARTH_SYLPH_2 = 80001719;
    public static final int DARK_SYLPH_2 = 80001720;
    public static final int HOLY_SYLPH_2 = 80001721;
    public static final int SALAMANDER_SYLPH_2 = 80001722;
    public static final int ELECTRON_SYLPH_2 = 80001723;
    public static final int UNDINE_SYLPH_2 = 80001724;
    public static final int GNOME_SYLPH_2 = 80001725;
    public static final int DEVIL_SYLPH_2 = 80001726;
    public static final int ANGEL_SYLPH_2 = 80001727;
    public static final int WHITE_ANGELIC_BLESSING = 80000155;
    public static final int WHITE_ANGELIC_BLESSING_2 = 80001154;
    public static final int LIGHTNING_GOD_RING = 80001262;
    public static final int LIGHTNING_GOD_RING_2 = 80011178;
    public static final int GUARD_RING = 80011149;
    public static final int SUN_RING = 80010067;
    public static final int RAIN_RING = 80010068;
    public static final int RAINBOW_RING = 80010069;
    public static final int SNOW_RING = 80010070;
    public static final int LIGHTNING_RING = 80010071;
    public static final int WIND_RING = 80010072;
    public static final int REBOOT = 80000186;
    public static final int REBOOT2 = 80000187;
    public static final int MAPLERUNNER_DASH = 80001965;
    public static final int IRON_SPIRIT = 80001654;
    public static final int TANADIAN_RUIN = 80002632;
    public static final int AEONIAN_RISE = 80002633;

    public static final int MYSTICAL_POWER_OF_THE_HAT = 80003566;

    // General buffs
    public static final int BOSS_SLAYERS = 80003600;
    public static final int UNDETERRED = 80003601;
    public static final int FOR_THE_GUILD = 80003602;
    public static final int HARD_HITTER = 80003603;
    public static final int HEAVEN_DOOR = 80003604;
    public static final int BOSS_PRACTICE_MODE_FOG_FOREST_TRAINING_GROUNDS_EXCLUSIVE = 80003605;
    public static final int WEAPON_TEMPERING = 80003606;
    public static final int ADVANCED_WEAPON_TEMPERING = 80003607;

    // Star Dust Event Skills
    public static final int STARLIGHT_EXPLOSION = 80000257;
    public static final int STELLAR_STAFF = 80002300;
    public static final int UFO_RAID = 80002301;

    // Link Skills:
    public static final int CLOSE_CALLS = 20050286;
    public static final int CLOSE_CALLS_LINK = 80000169;
    public static final int KNIGHT_WATCH = 50001214;
    public static final int KNIGHT_WATCH_LINK = 80001140;
    public static final int TERMS_CONDITIONS = 60011219;
    public static final int TERMS_CONDITIONS_LINK = 80001155;
    public static final int TIDE_OF_BATTLE = 150000017;
    public static final int TIDE_OF_BATTLE_LINK = 80000268;
    public static final int SOLUS = 150010241;
    public static final int SOLUS_LINK = 80000514;
    public static final int INVICIBLE_BELIEF_HERO = 252;
    public static final int INVICIBLE_BELIEF_PALADIN = 253;
    public static final int INVICIBLE_BELIEF_DARK_KNIGHT = 254;
    public static final int INVICIBLE_BELIEF_LINK = 80002758;
    public static final int EMPIRICAL_KNOWLEDGE_FP = 255;
    public static final int EMPIRICAL_KNOWLEDGE_IL = 256;
    public static final int EMPIRICAL_KNOWLEDGE_BIS = 257;
    public static final int EMPIRICAL_KNOWLEDGE_LINK = 80002762;
    public static final int THIEF_CUNNING_NL = 261;
    public static final int THIEF_CUNNING_SHADOWER = 262;
    public static final int THIEF_CUNNING_DB = 263;
    public static final int THIEF_CUNNING_LINK = 80002770;
    public static final int QI_CULTIVATION = 170000241;
    public static final int QI_CULTIVATION_LINK = 80011964;
    public static final int GROUNDED_BODY = 160020001;
    public static final int GROUNDED_BODY_LINK = 80003877;

    // Special Node Skills:
    public static final int FRENZIED_STRENGTH_I = 400007000;
    public static final int FRENZIED_STRENGTH_II = 400007001;
    public static final int FRENZIED_STRENGTH_III = 400007002;
    public static final int KEEN_ATTACk_I = 400007003;
    public static final int KEEN_STRIKE_I = 400007004;
    public static final int DEFENSE_SMASH_I = 400007005;
    public static final int RUNE_BLESSED = 400007006;
    public static final int RUNE_EXP = 400007007;
    public static final int BOSS_SLAYER = 400007008;
    public static final int FRENZIED_STRENGTH_V = 400007009;
    public static final int FATAL_STRIKE_I = 400007010;
    public static final int CHARACTER_BUILDING = 400007011;

    // Common V Skills
    public static final int ROPE_LIFT = 400001000;
    public static final int DECENT_MYSTIC_DOOR_V = 400001001;
    public static final int DECENT_SHARP_EYES_V = 400001002;
    public static final int DECENT_HYPER_BODY_V = 400001003;
    public static final int DECENT_COMBAT_ORDERS_V = 400001004;
    public static final int DECENT_ADV_BLESSING_V = 400001005;
    public static final int DECENT_SPEED_INFUSION_V = 400001006;
    public static final int DECENT_HOLY_SYMBOL_V = 400001020;
    public static final int ERDA_NOVA = 400001008;
    public static final int WILL_OF_ERDA = 400001009;
    public static final int ERDA_SHOWER = 400001036;
    public static final int ERDA_FOUNTAIN = 400001064;

    // First Branch V Skills
    public static final int WEAPON_AURA_ATTACK = 400010000;
    public static final int WEAPON_AURA = 400011000;
    public static final int MANA_OVERLOAD = 400021000;
    public static final int GUIDED_ARROW = 400031000;
    public static final int GUIDED_ARROW_ATOM = 400031001;
    public static final int VENOM_BURST = 400041000;
    public static final int VENOM_BURST_ATTACK = 400041030;
    public static final int LOADED_DICE = 400051000;
    public static final int LUCKY_DICE = 400051001;

    // Second Branch V Skills
    public static final int IMPENETRABLE_SKIN = 400011066;
    public static final int ETHEREAL_FORM = 400021060;
    public static final int VICIOUS_SHOT = 400031023;
    public static final int LAST_RESORT = 400041032;
    public static final int OVERDRIVE = 400051033;

    // Race V Skills
    public static final int FREUDS_WISDOM = 400001024;
    public static final int FREUDS_WISDOM_1 = 400001025;
    public static final int FREUDS_WISDOM_2 = 400001026;
    public static final int FREUDS_WISDOM_3 = 400001027;
    public static final int FREUDS_WISDOM_4 = 400001028;
    public static final int FREUDS_WISDOM_5 = 400001029;
    public static final int FREUDS_WISDOM_6 = 400001030;

    public static final int RESISTANCE_INFANTRY_1 = 400001019;
    public static final int RESISTANCE_INFANTRY_2 = 400001022;

    public static final int SENGOKU_FORCE_ASSEMBLE = 400001031;
    public static final int SENGOKU_FORCE_TAKEDA = 400001035;
    public static final int SENGOKU_FORCE_AYAME = 400001034;
    public static final int SENGOKU_FORCE_HARUAKI = 400001033;
    public static final int SENGOKU_FORCE_UESUGI = 400001032;

    public static final int MIGHT_OF_THE_NOVA = 400001014;
    public static final int MIGHT_OF_THE_NOVA_BUFF = 400001015;

    public static final int CONVERSION_OVERDRIVE = 400001037;
    public static final int CONVERSION_OVERDRIVE_ATTACK = 400001038;

    public static final int TRUE_ARACHNID_REFLECTION = 400001039;
    public static final int TRUE_ARACHNID_REFLECTION_SUMMON = 400001040; // summon
    public static final int TRUE_ARACHNID_REFLECTION_2 = 400001041;

    public static final int MAPLE_WORLD_GODDESS_BLESSING = 400001042;
    public static final int EMPRESS_CYGNUS_BLESSING = 400001043;
    public static final int TRANSCENDENT_CYGNUS_BLESSING = 400001044;
    public static final int GRANDIS_GODDESS_BLESSING = 400001046;
    public static final int GRANDIS_GODDESS_BLESSING_KAISER = 400001047;
    public static final int GRANDIS_GODDESS_BLESSING_FLORA = 400001048;
    public static final int GRANDIS_GODDESS_BLESSING_AMINA = 400001049;

    public static final int OTHERWORLD_GODDESS_BLESSING = 400001050;
    public static final int BLESSING_OF_RECOVERY = 400001051;
    public static final int AEGIS_BLESSING = 400001053;
    public static final int BLESSING_OF_FORTITUDE = 400001054;
    public static final int OTHERWORLDLY_VOID = 400001055;
    public static final int LOTUS_FLOWER = 400001061;

    public static final int ORIGIN_SKILL = 80003365;
    public static final int SOL_JANUS_ACTIVATION = 500001000;
    public static final int SOL_JANUS_DUSK = 500001001;
    public static final int SOL_JANUS_DAWN = 500001002;
    public static final int SOL_JANUS_DAWN_2 = 500001003;
    public static final int SOL_JANUS_DAWN_3 = 500001004;

    public static final int[] REMOVE_ON_STOP = new int[]{
            MAPLERUNNER_DASH
    };

    public static final int[] REMOVE_ON_WARP = new int[]{
            MAPLERUNNER_DASH,};

    public int weaponType;

    private ScheduledFuture<?> bless5thTimer;
    private ScheduledFuture<?> invincibleBeliefTimer;
    private int solusStack = 0;
    private int empiricalKnowledgeStack = 0;
    private int HeroWarriorRecoveryStack = 0;
    private long lastHeroWarriorRecovery = 0L;
    boolean lotusFlowerTrigged = false;

    public Job(Char chr) {
        this.chr = chr;
        this.c = chr.getClient();
    }

    public void handleAttack(Client c, AttackInfo attackInfo, SkillInfo si, long now) {
        Char chr = c.getChr();
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        int skillID = attackInfo.skillId;
        boolean hasHitMobs = !attackInfo.mobAttackInfo.isEmpty();
        int slv = attackInfo.slv;
        Option o = new Option();
        if (si != null) {
            // Aran Sliding Request
            int fallingSpeed = 30;
            int fallingTime = SkillConstants.getFallingTime(skillID);
            if (fallingTime != -1) {
                chr.write(UserLocal.setSlowDown(fallingSpeed, fallingTime));
            }
            // Final Attack Request
            handleFinalAttackSkill(chr, attackInfo, skillID, si);
            // Extra Skill Request
            handleExtraSkill(si, skillID, attackInfo);
            if (hasHitMobs) {
                if (tsm.hasStat(GuidedArrow) && !SkillConstants.isForceAtomSkill(skillID)) {
                    guideGuidedArrowForceAtom(attackInfo);
                }
                if (chr.getLevel() >= 200 && !Tms282VMatrixRuntime.enabled()) {
                    chr.getSpecialNodeSkill().activate();
                }
                if ((chr.hasSkill(UnionConstants.HERO_WARRIOR_ARAN) || chr.hasSkill(UnionConstants.HERO_MAGICIAN_EVAN))) {
                    if (lastHeroWarriorRecovery <= now - 10000L && Util.succeedProp(HeroWarriorRecoveryStack == 0 ? 70 : 35)) {
                        int skillIDRecovery = chr.hasSkill(UnionConstants.HERO_WARRIOR_ARAN) ? UnionConstants.HERO_WARRIOR_ARAN : UnionConstants.HERO_MAGICIAN_EVAN;
                        SkillInfo siRecovery = SkillData.getSkillInfoById(skillIDRecovery);
                        int hp = (int) (siRecovery.getValue(onHitHpRecoveryR, chr.getSkillLevel(skillIDRecovery)) * chr.getMaxHP() / 100.0D);
                        if (HeroWarriorRecoveryStack == 0) {
                            HeroWarriorRecoveryStack = 1;
                        } else {
                            HeroWarriorRecoveryStack = 0;
                            hp *= 2;
                        }
                        chr.heal(hp);
                        lastHeroWarriorRecovery = now;
                    }
                }
                if (chr.hasSkill(UnionConstants.RESISTANCE_BOWMAN_WILD_HUNTER)) {
                    if (Util.succeedProp(20)) {
                        int skillIDLegionWH = UnionConstants.RESISTANCE_BOWMAN_WILD_HUNTER;
                        SkillInfo skillInfo = SkillData.getSkillInfoById(skillIDLegionWH);
                        Option o1 = new Option();
                        int slvLegionWH = chr.getSkillLevel(skillIDLegionWH);
                        o1.nValue = skillInfo.getValue(y, slvLegionWH);
                        o1.nReason = skillIDLegionWH;
                        o1.tTerm = 10;
                        tsm.sendStat(IndieDamR, o1);
                    }
                }
                if (chr.hasSkill(THIEF_CUNNING_NL)
                        || chr.hasSkill(THIEF_CUNNING_SHADOWER)
                        || chr.hasSkill(THIEF_CUNNING_DB)
                        || chr.hasSkill(THIEF_CUNNING_LINK)) {
                    int thiefCunningID = 0;
                    int SLV = chr.getSkillLevel(THIEF_CUNNING_LINK);
                    if (chr.hasSkill(THIEF_CUNNING_NL) || chr.hasSkill(THIEF_CUNNING_LINK)) {
                        thiefCunningID = chr.hasSkill(THIEF_CUNNING_NL) ? THIEF_CUNNING_NL : THIEF_CUNNING_LINK;
                        if (!chr.hasSkill(THIEF_CUNNING_LINK)) {
                            SLV = chr.getSkillLevel(THIEF_CUNNING_NL);
                        }
                    } else if (chr.hasSkill(THIEF_CUNNING_SHADOWER) || chr.hasSkill(THIEF_CUNNING_LINK)) {
                        thiefCunningID = chr.hasSkill(THIEF_CUNNING_SHADOWER) ? THIEF_CUNNING_SHADOWER : THIEF_CUNNING_LINK;
                        if (!chr.hasSkill(THIEF_CUNNING_LINK)) {
                            SLV = chr.getSkillLevel(THIEF_CUNNING_SHADOWER);
                        }
                    } else if (chr.hasSkill(THIEF_CUNNING_DB) || chr.hasSkill(THIEF_CUNNING_LINK)) {
                        thiefCunningID = chr.hasSkill(THIEF_CUNNING_DB) ? THIEF_CUNNING_DB : THIEF_CUNNING_LINK;
                        if (!chr.hasSkill(THIEF_CUNNING_LINK)) {
                            SLV = chr.getSkillLevel(THIEF_CUNNING_DB);
                        }
                    }
                    if (!chr.hasSkillOnCooldown(thiefCunningID)) {
                        Option o1 = new Option();
                        SkillInfo thiefCunningSI = SkillData.getSkillInfoById(thiefCunningID);
                        o1.nValue = thiefCunningSI.getValue(indieDamR, SLV);
                        o1.nReason = THIEF_CUNNING_LINK;
                        o1.tTerm = thiefCunningSI.getValue(time, SLV);
                        tsm.sendStat(IndieDamR, o1);
                        chr.addSkillCooldown(thiefCunningID, thiefCunningSI.getValue(cooltime, SLV) * 1000);
                    }
                }
                handleSolJanusDusk();
                switch (skillID) {
                    case RuneStone.LIBERATE_THE_DESTRUCTIVE_RUNE -> {
                        // Attack of the Rune
                        AffectedArea aa = AffectedArea.getAffectedArea(chr, attackInfo);
                        aa.setMobOrigin((byte) 0);
                        aa.setPosition(chr.getPosition());
                        aa.setRect(aa.getPosition().getRectAround(si.getRects().getFirst()));
                        chr.getField().spawnAffectedArea(aa);
                        // Buff of the Rune
                        int additionalLinkSkillTime = 0;
                        if (chr.hasSkill(20010294) || chr.hasSkill(80000369)) {
                            additionalLinkSkillTime = 10 + 20 * (chr.hasSkill(20010294) ? chr.getSkillLevel(20010294) : chr.getSkillLevel(80000369));
                        }
                        si = SkillData.getSkillInfoById(RuneStone.LIBERATE_THE_DESTRUCTIVE_RUNE_BUFF); //Buff Info
                        slv = 1;
                        o.nReason = RuneStone.LIBERATE_THE_DESTRUCTIVE_RUNE_BUFF;
                        o.nValue = si.getValue(indieDamR, slv); //50% DamR
                        o.tTerm = si.getValue(time, slv) + additionalLinkSkillTime;
                        tsm.sendStat(IndieDamR, o);
                    }
                    case ERDA_SHOWER -> chr.reduceSkillCoolTime(skillID, 1000L * attackInfo.mobAttackInfo.size());
                    case TRUE_ARACHNID_REFLECTION -> {
                        Summon summon = Summon.getSummonByAndSetStat(c.getChr(), TRUE_ARACHNID_REFLECTION_SUMMON, slv);
                        summon.setMoveAction((byte) 4);
                        summon.setAssistType(AssistType.ExplosionAttack);
                        summon.setMoveAbility(MoveAbility.Stop);
                        summon.setFlyMob(true);
                        chr.getField().spawnSummon(summon);
                    }
                    case MIGHT_OF_THE_NOVA -> {
                        if (tsm.hasStatBySkillId(Bishop.HEAVENS_DOOR) || tsm.hasStatBySkillId(Bishop.HEXA_HEAVENS_DOOR)) {
                            chr.chatMessage("天堂之門效果持續中，無法使用。");
                            break;
                        }
                        si = SkillData.getSkillInfoById(MIGHT_OF_THE_NOVA_BUFF);
                        o.nOption = 1;
                        o.rOption = MIGHT_OF_THE_NOVA_BUFF;
                        o.tOption = si.getValue(time, slv);
                        tsm.sendStat(ReviveOnce, o);
                    }
                    case ORIGIN_SKILL -> {
                        for (MobAttackInfo mai : attackInfo.mobAttackInfo) {
                            Mob mob = (Mob) chr.getField().getLifeByObjectID(mai.mobId);
                            if (mob == null || mob.getHp() <= 0) {
                                continue;
                            }
                            MobTemporaryStat mts = mob.getTemporaryStat();
                            if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                                EnumMap<MobStat, Option> map = new EnumMap<>(MobStat.class);
                                Option o1 = new Option();
                                Option o2 = new Option();
                                o1.nOption = 1;
                                o1.rOption = skillID;
                                o1.tOption = 20;
                                o1.cOption = chr.getId();
                                map.put(MobStat.Freeze, o1);
                                o2.nOption = 10;
                                o2.rOption = skillID;
                                o2.tOption = 20;
                                o2.xOption = 22;
                                map.put(MobStat.OriginDebuff, o2);
                                mts.addStatOptions(mob, map);
                            }
                        }
                        if (chr.getParty() != null) {
                            for (Char other : chr.getParty().getPartyMembersInSameField(chr)) {
                                other.write(UserLocal.showHexaSkillEff(chr));
                            }
                        }
                    }
                }
                if (JobConstants.isFirePoison(chr.getJob())) {
                    handleDamageCalc(si, slv, attackInfo);
                }
            }
        }
        handleSixthJobOriginAscentSkill(chr, tsm, skillID, si, slv);
    }

    private void handleDamageCalc(SkillInfo si, int slv, AttackInfo attackInfo) {
        var dmgCalc = chr.getDamageCalc();
        var list = dmgCalc.getDamages();
        if (list.size() >= 50) {
            int idx = 0;
            long max = Long.MIN_VALUE;
            for (int i = 0, n = list.size(); i < n; i++) {
                long v = list.get(i);
                if (v > max) {
                    max = v;
                    idx = i;
                }
            }
            list.remove(idx);
        }
        int dmgMul = si != null ? si.getValue(SkillStat.damage, slv) : 100;
        if (dmgMul <= 0) dmgMul = 100;
        long min = Long.MAX_VALUE;
        long[] arr = attackInfo.mobAttackInfo.getFirst().damages;
        for (long d : arr) {
            if (d < min) {
                min = d;
            }
        }
        long damage = (min * 100L) / dmgMul;
        list.add(damage);
    }

    private void handleFinalAttackSkill(Char chr, AttackInfo attackInfo, int skillID, SkillInfo si) {
        final List<Integer> fas = si.getFinalAttacks();
        final int faCount = fas.size();
        if (faCount == 0) {
            final int faSkill = resolveFallbackFinalAttack(chr, skillID);
            if (faSkill == 0) {
                return;
            }
            int proc = getFinalAttackProc(faSkill);
            if (proc == 0) {
                proc = 30;
            }
            if (Util.succeedProp(proc)) {
                final int[] mobsHit = toMobIdArray(attackInfo);
                chr.write(FieldPacket.finalAttackRequest(weaponType, skillID, faSkill, mobsHit));
            }
            return;
        }
        if (faCount == 1) {
            int faSkill = fas.getFirst();
            faSkill = resolveFinalAttackSkillOnce(faSkill);
            final int proc = getFinalAttackProc(faSkill);
            if (proc != 0 && !Util.succeedProp(proc)) {
                return;
            }
            final int[] mobsHit = toMobIdArray(attackInfo);
            chr.write(FieldPacket.finalAttackRequest(weaponType, skillID, faSkill, mobsHit));
            return;
        }
        final int[] toSend = new int[faCount];
        int sendCount = 0;
        for (int i = 0; i < faCount; i++) {
            int faSkill = fas.get(i);
            faSkill = resolveFinalAttackSkillOnce(faSkill);
            if (!chr.hasSkill(faSkill)) {
                continue;
            }
            final int proc = getFinalAttackProc(faSkill);
            if (proc != 0 && !Util.succeedProp(proc)) {
                continue;
            }
            toSend[sendCount++] = faSkill;
        }
        if (sendCount == 0) {
            return;
        }
        final int[] mobsHit = toMobIdArray(attackInfo);
        for (int i = 0; i < sendCount; i++) {
            chr.write(FieldPacket.finalAttackRequest(weaponType, skillID, toSend[i], mobsHit));
        }
    }

    private int resolveFinalAttackSkillOnce(int faSkill) {
        int mapped = getFinalAttackSkill(faSkill);
        return mapped != 0 ? mapped : faSkill;
    }

    private int resolveFallbackFinalAttack(Char chr, int skillID) {
        if (JobConstants.isFirePoison(chr.getJob()) && skillID != 0 && chr.hasSkill(FirePoison.METEOR_SHOWER)) {
            return chr.hasSkill(FirePoison.HEXA_METEOR_SHOWER) ? FirePoison.HEXA_METEOR_SHOWER_FA : FirePoison.METEOR_SHOWER_FA;
        } else if (JobConstants.isIceLightning(chr.getJob()) && skillID != 0 && chr.hasSkill(IceLightning.BLIZZARD)) {
            return chr.hasSkill(IceLightning.HEXA_BLIZZARD) ? IceLightning.HEXA_BLIZZARD_FA : IceLightning.BLIZZARD_FA;
        } else if (JobConstants.isBishop(chr.getJob()) && skillID != 0 && chr.hasSkill(Bishop.HEXA_GENESIS)) {
            return Bishop.HEXA_GENESIS_FA;
        } else if (JobConstants.isBattleMage(chr.getJob()) && skillID != 0 && chr.hasSkill(BattleMage.DARK_GENESIS)) {
            return chr.hasSkill(BattleMage.HEXA_DARK_GENESIS) ? BattleMage.HEXA_DARK_GENESIS_FA : BattleMage.DARK_GENESIS_FA;
        }
        return 0;
    }

    private int[] toMobIdArray(AttackInfo attackInfo) {
        var list = attackInfo.mobAttackInfo;
        int[] arr = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            arr[i] = list.get(i).mobId;
        }
        return arr;
    }

    public void handleExtraSkill(SkillInfo si, int skillID, AttackInfo attackInfo) {
        if (skillID == Xenon.HEXA_BEAM_DANCE_EXTRA
                || skillID == DualBlade.KARMA_BLADE_EXTRA
                || skillID == DawnWarrior.FLARE_SLASH_SUN
                || skillID == DawnWarrior.EQUINOX_POWER_EXTRA) {
            return; // Special Handled in Xenon, Dual Blade, Dawn Warrior (Spammable)
        }
        if (!si.getExtraSkillInfo().isEmpty()) {
            List<ExtraSkill> extraSkills = new ArrayList<>();
            Field field = chr.getField();
            for (var entry : si.getExtraSkillInfo().entrySet()) {
                var idx = entry.getKey();
                var extraSkillID = entry.getValue().getSkillId();
                var delay = entry.getValue().getDelay();
                ExtraSkill extraSkill = new ExtraSkill(extraSkillID, chr.getPosition());
                if (skillID == FirePoison.CREEPING_TOXIN || skillID == FirePoison.HEXA_CREEPING_TOXIN) {
                    extraSkill.SkillID = skillID + 1;
                    extraSkill.TriggerSkillID = FirePoison.FLAME_HAZE;
                    extraSkill.Delay = 250;
                    AffectedArea aa = field.getAffectedAreaBySkillID(chr.getId(), skillID);
                    extraSkill.TargetOID = aa != null ? aa.getObjectId() : 0;
                    extraSkill.Value = 1;
                } else if (skillID == Shadower.SLASH_SHADOW_FORMATION) {
                    extraSkill.Delay = delay;
                    extraSkill.Value = extraSkillID == Shadower.SLASH_SHADOW_FORMATION_4 ? 8 : 1;
                } else {
                    if (entry.getValue().getManual() != 0) {
                        if (!chr.hasQuest(QuestConstants.SKILL_COMMAND_LOCK_ARK)
                                || !"1".equals(chr.getQRValueByKey(QuestConstants.SKILL_COMMAND_LOCK_ARK,
                                String.valueOf(skillID)))) {
                            continue;
                        }
                    }
                    extraSkill.Delay = delay;
                    extraSkill.Value = 1;
                    if (extraSkill.SkillID == Paladin.MIGHTY_MJOLNIR_EXPLOSION) {
                        Mob mob = (Mob) chr.getField().getLifeByObjectID(attackInfo.mobAttackInfo.getFirst().mobId);
                        if (mob != null && mob.getHp() >= 0) {
                            extraSkill.Position = mob.getPosition();
                        }
                    }
                }
                extraSkills.add(extraSkill);
            }
            extraSkills.sort(Comparator.comparingInt(ExtraSkill::getDelay).reversed());
            chr.write(UserLocal.registerExtraSkill(skillID, extraSkills));
        }
    }

    private void handleSolJanusDusk() {
        if (!chr.hasSkill(SOL_JANUS_ACTIVATION)) return;
        String qr = chr.getQRValueByKey(QuestConstants.SKILL_COMMAND_LOCK_ARAN, "500001000");
        if (qr != null && !chr.hasSkillOnCooldown(SOL_JANUS_DUSK) && qr.equals("500001001")) {
            SkillInfo si = SkillData.getSkillInfoById(SOL_JANUS_DUSK);
            int slv = chr.getSkillLevel(SOL_JANUS_ACTIVATION);
            int bulletCount = si.getValue(SkillStat.bulletCount, slv);
            List<SecondAtom> secondAtoms = new LinkedList<>();
            Rect rect = chr.getRectAround(new Rect(-500, -500, 500, 500));
            if (!chr.isLeft()) {
                rect = rect.horizontalFlipAround(chr.getPosition().getX());
            }
            List<Mob> mobs = chr.getField().getMobsInRect(rect);
            var sai = si.getSecondAtomInfos().get(0);
            if (sai == null) return;
            final long start = System.currentTimeMillis();
            for (int i = 0; i < bulletCount; i++) {
                var mob = Util.getRandomFromCollection(mobs);
                if (mob == null) continue;
                SecondAtom fa = new SecondAtom(sai, chr.getNewSecondAtomKey(), chr.getId(), mob != null ? mob.getObjectId() : 0, i,
                        si.getSkillId(), mob.getPosition(), start);
                secondAtoms.add(fa);
            }
            if (!secondAtoms.isEmpty()) {
                chr.createSecondAtom(secondAtoms);
                chr.heal(-(si.getValue(hpRCon, slv) * chr.getMaxHP() / 100));
                chr.addSkillCooldown(SOL_JANUS_DUSK, si.getValue(cooltime, slv) * 1000);
            }
        }
    }

    protected void handleSixthJobOriginAscentSkill(Char chr, TemporaryStatManager tsm, int skillID, SkillInfo si, int slv) {
        if (!SixthJobSkillConstants.isRecognizedOriginSkill(skillID, si)
                && !SixthJobSkillConstants.isRecognizedAscentSkill(skillID, si)) {
            return;
        }
        if (tsm.hasStatBySkillId(skillID)) {
            return;
        }
        int iframeDuration = si != null ? si.getValue(time, slv) : 0;
        if (iframeDuration <= 0 && si != null) {
            iframeDuration = si.getValue(ndTime, slv);
        }
        if (iframeDuration <= 0) {
            iframeDuration = 30;
        }
        Option o = new Option();
        o.nValue = 1;
        o.nReason = skillID;
        o.tTerm = iframeDuration;
        o.isInMillis = true;
        tsm.sendStat(IndieNotDamaged, o);
    }

    public void handleDebuffOnMob(Client c, Mob mob, SkillInfo si, int skillID, int slv, long damage) {
        Char chr = c.getChr();

        handleSpecialLinkSkill(mob);

        MobTemporaryStat mts = mob.getTemporaryStat();
        switch (skillID) {
            case RuneStone.LIBERATE_THE_DESTRUCTIVE_RUNE: {
                BurnedInfo bi = BurnedInfo.createBurnInfo(chr, skillID, slv, damage);
                mts.createAndAddBurnedInfo(mob, bi, skillID);
                break;
            }
            case ERDA_NOVA: {
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    Option o1 = new Option();
                    o1.nOption = 1;
                    o1.rOption = skillID;
                    o1.tOption = 10;
                    mts.addStatOptions(mob, MobStat.Stun, o1);
                }
                break;
            }
        }
    }

    private void handleSpecialLinkSkill(Mob mob) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (chr.hasSkill(EMPIRICAL_KNOWLEDGE_FP)
                || chr.hasSkill(EMPIRICAL_KNOWLEDGE_IL)
                || chr.hasSkill(EMPIRICAL_KNOWLEDGE_BIS)
                || chr.hasSkill(EMPIRICAL_KNOWLEDGE_LINK)) {
            int empiricalKnowledgeID = 0;
            int SLV = chr.getSkillLevel(EMPIRICAL_KNOWLEDGE_LINK);
            if (chr.hasSkill(EMPIRICAL_KNOWLEDGE_FP) || chr.hasSkill(EMPIRICAL_KNOWLEDGE_LINK)) {
                empiricalKnowledgeID = chr.hasSkill(EMPIRICAL_KNOWLEDGE_FP) ? EMPIRICAL_KNOWLEDGE_FP : EMPIRICAL_KNOWLEDGE_LINK;
                if (!chr.hasSkill(EMPIRICAL_KNOWLEDGE_LINK)) {
                    SLV = chr.getSkillLevel(EMPIRICAL_KNOWLEDGE_FP);
                }
            } else if (chr.hasSkill(EMPIRICAL_KNOWLEDGE_IL) || chr.hasSkill(EMPIRICAL_KNOWLEDGE_LINK)) {
                empiricalKnowledgeID = chr.hasSkill(EMPIRICAL_KNOWLEDGE_IL) ? EMPIRICAL_KNOWLEDGE_IL : EMPIRICAL_KNOWLEDGE_LINK;
                if (!chr.hasSkill(EMPIRICAL_KNOWLEDGE_LINK)) {
                    SLV = chr.getSkillLevel(EMPIRICAL_KNOWLEDGE_IL);
                }
            } else if (chr.hasSkill(EMPIRICAL_KNOWLEDGE_BIS) || chr.hasSkill(EMPIRICAL_KNOWLEDGE_LINK)) {
                empiricalKnowledgeID = chr.hasSkill(EMPIRICAL_KNOWLEDGE_BIS) ? EMPIRICAL_KNOWLEDGE_BIS : EMPIRICAL_KNOWLEDGE_LINK;
                if (!chr.hasSkill(EMPIRICAL_KNOWLEDGE_LINK)) {
                    SLV = chr.getSkillLevel(EMPIRICAL_KNOWLEDGE_IL);
                }
            }
            SkillInfo empiricalKnowledgeSI = SkillData.getSkillInfoById(empiricalKnowledgeID);
            if (Util.succeedProp(empiricalKnowledgeSI.getValue(prop, SLV))) {
                if (empiricalKnowledgeStack == 0 || empiricalKnowledgeStack < empiricalKnowledgeSI.getValue(x, SLV)) {
                    ++empiricalKnowledgeStack;
                    Option o = new Option();
                    o.nOption = empiricalKnowledgeStack;
                    o.rOption = EMPIRICAL_KNOWLEDGE_LINK;
                    o.tOption = empiricalKnowledgeSI.getValue(time, SLV);
                    o.xOption = mob.getObjectId();
                    tsm.sendStat(NoviceMagicianLink, o);
                }
            }
        }
        if (chr.hasSkill(QI_CULTIVATION) || chr.hasSkill(QI_CULTIVATION_LINK)) {
            int moXuanLinkSkillID = chr.hasSkill(QI_CULTIVATION) ? QI_CULTIVATION : QI_CULTIVATION_LINK;
            if (mob.isBoss() && !chr.hasSkillOnCooldown(moXuanLinkSkillID)) {
                int SLV = chr.getSkillLevel(moXuanLinkSkillID);
                SkillInfo moXuanLinkSkillSI = SkillData.getSkillInfoById(moXuanLinkSkillID);
                int inc = moXuanLinkSkillSI.getValue(y, SLV);
                int max = moXuanLinkSkillSI.getValue(x, SLV);
                Option o = new Option();
                o.nOption = tsm.hasStat(MukhyunLinkSkill) ? Math.max((int) tsm.getTotalNOptionOfStat(MukhyunLinkSkill) + inc, max) : inc;
                if (net.swordie.ms.connection.protocol.Protocol.active().version()
                        == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) {
                    // 282 common/x is the cap, common/y the per-hit increment; max skipped straight to cap then overflowed.
                    o.nOption = Math.min((int) tsm.getTotalNOptionOfStat(MukhyunLinkSkill) + inc, max);
                }
                o.rOption = moXuanLinkSkillID;
                o.tOption = moXuanLinkSkillSI.getValue(time, SLV);
                tsm.sendStat(MukhyunLinkSkill, o);
                chr.addSkillCooldown(moXuanLinkSkillID, moXuanLinkSkillSI.getValue(cooltime, SLV) * 1000);
            }
        }
    }

    public void handleSkill(Client c, InPacket inPacket, SkillUseInfo skillUseInfo) {
        Char chr = c.getChr();
        Field field = chr.getField();
        SkillInfo si = skillUseInfo.skillInfo;
        int slv = skillUseInfo.slv;
        int skillID = skillUseInfo.skillID;
        Summon summon;
        if (inPacket != null && net.swordie.ms.client.character.tms282.Tms282InstallSummons.handles(skillID)) {
            net.swordie.ms.client.character.tms282.Tms282InstallSummons.spawn(chr, skillID, slv);
        }
        if (inPacket != null && si != null && net.swordie.ms.client.character.tms282.Tms282InstallSummons.returnSkill(skillID)) {
            // live 10-07: Illium 回歸魔法屏障 150001021 spent MP and the 600 s cooldown without moving the character
            Field town = chr.getOrCreateFieldByCurrentInstanceType(si.getValue(SkillStat.x, slv));
            if (town != null) {
                chr.warp(town);
            }
        }
        if (inPacket != null) {
            TemporaryStatManager tsm = chr.getTemporaryStatManager();
            EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
            Option o1 = new Option();
            Option o2 = new Option();
            Option o3 = new Option();
            Option o4 = new Option();
            Option o5 = new Option();
            Option o6 = new Option();
            applyHpCost(skillID);
            applyNdTime(chr, si, slv);
            int noviceSkill = SkillConstants.getNoviceSkillFromRace(skillID);
            if (noviceSkill == 1085 || noviceSkill == 1087 || noviceSkill == 1090 || noviceSkill == 1179) {
                summon = Summon.getSummonByAndSetStat(c.getChr(), skillID, slv);
                summon.setMoveAction((byte) 4);
                summon.setAssistType(AssistType.Heal);
                summon.setFlyMob(true);
                field.spawnSummon(summon);
            } else if (noviceSkill == 1026) { // soaring
                if (field.isFly()) {
                    Option option = new Option();
                    option.nOption = 1;
                    option.rOption = skillID;
                    tsm.sendStat(Flying, option);
                }
            } else if (si.getVehicleId() > 0) {
                TemporaryStatBase tsb = tsm.getTSBByTSIndex(TSIndex.RideVehicle);
                if (tsm.hasStat(RideVehicle)) {
                    tsm.removeStat(RideVehicle);
                }
                tsb.setNOption(si.getVehicleId());
                tsb.setROption(skillID);
                tsm.sendStat(RideVehicle, tsb.getOption());
            } else if (net.swordie.ms.client.character.tms282.Tms282RidingVehicles.vehicleOf(skillID) > 0) {
                // 282: the skill data has no vehicleID (RidingSkillInfo); using the same riding skill again dismounts
                TemporaryStatBase tsb = tsm.getTSBByTSIndex(TSIndex.RideVehicle);
                boolean sameMount = tsm.hasStat(RideVehicle) && tsb.getROption() == skillID;
                if (tsm.hasStat(RideVehicle)) {
                    tsm.removeStat(RideVehicle);
                    String landed = net.swordie.ms.client.character.tms282.Tms282LucidFlight.land(chr, "dismount");
                    if (landed != null) {
                        System.out.println(landed);
                    }
                }
                if (!sameMount) {
                    tsb.setNOption(net.swordie.ms.client.character.tms282.Tms282RidingVehicles.vehicleOf(skillID));
                    tsb.setROption(skillID);
                    tsm.sendStat(RideVehicle, tsb.getOption());
                }
            } else if (SkillConstants.isSoulSummonSkill(skillID)) {
                summon = Summon.getSummonByAndSetStat(c.getChr(), skillID, slv);
                if (summon != null) {
                    summon.setAttackActive(true);
                    field.spawnSummon(summon);
                }
                if (skillID == LONG_LIVE_THE_QUEEN) {
                    // TODO: HP Absorbed from Damage: % HP
                } else if (skillID == LONG_LIVE_THE_QUEEN_1) {
                    o1.nReason = skillID;
                    o1.tTerm = si.getValue(time, slv);
                    o1.nValue = -1;
                    tsm.sendStat(IndieBooster, o1);
                } else if (skillID == LONG_LIVE_THE_QUEEN_2) {
                    o1.nReason = skillID;
                    o1.tTerm = si.getValue(time, slv);
                    o1.nValue = si.getValue(indiePad, slv);
                    newStats.put(IndiePAD, o1);
                    o2.nReason = skillID;
                    o2.tTerm = si.getValue(time, slv);
                    o2.nValue = si.getValue(indieMad, slv);
                    newStats.put(IndieMAD, o2);
                    tsm.sendStat(newStats);
                } else if (skillID == LONG_LIVE_THE_QUEEN_3) {
                    o1.nReason = skillID;
                    o1.tTerm = si.getValue(time, slv);
                    o1.nValue = si.getValue(indiePdd, slv);
                    newStats.put(IndiePDD, o1);
                    o3.nReason = skillID;
                    o3.tTerm = si.getValue(time, slv);
                    o3.nValue = si.getValue(indieMhpR, slv);
                    newStats.put(IndieMHPR, o3);
                    tsm.sendStat(newStats);
                }
            } else if (JobConstants.JobEnum.getJobById(chr.getJob()).getBeginnerJobId() * 10000 + DECENT_HASTE == skillID) {
                o1.nOption = si.getValue(speed, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                newStats.put(Speed, o1);
                o2.nOption = si.getValue(jump, slv);
                o2.rOption = skillID;
                o2.tOption = si.getValue(time, slv);
                newStats.put(Jump, o2);
                tsm.sendStat(newStats);
            } else if (JobConstants.JobEnum.getJobById(chr.getJob()).getBeginnerJobId() * 10000 + DECENT_MYSTIC_DOOR == skillID) {
                Field townField = FieldData.getFieldById(chr.getField().getReturnMap());
                int x = townField.getPortalByName("tp").getX();
                int y = townField.getPortalByName("tp").getY();
                Position townPosition = new Position(x, y); // Grabs the Portal Co-ordinates for the TownPortalPoint
                int duration = si.getValue(time, slv);
                if (chr.getTownPortal() != null) {
                    TownPortal townPortal = chr.getTownPortal();
                    townPortal.despawnTownPortal();
                }
                TownPortal townPortal = new TownPortal(chr, townPosition, chr.getPosition(), chr.getField().getReturnMap(), chr.getFieldID(), skillID, duration);
                townPortal.spawnTownPortal();
                chr.dispose();
            } else if (JobConstants.JobEnum.getJobById(chr.getJob()).getBeginnerJobId() * 10000 + DECENT_SHARP_EYES == skillID) {
                int cr = si.getValue(x, slv);
                int crDmg = si.getValue(y, slv);
                o1.nOption = (cr << 8) + crDmg;
                if (net.swordie.ms.connection.protocol.Protocol.active().version() == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) {
                    o1.xOption = cr;
                    o1.yOption = crDmg;
                }
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(SharpEyes, o1);
            } else if (JobConstants.JobEnum.getJobById(chr.getJob()).getBeginnerJobId() * 10000 + DECENT_HYPER_BODY == skillID) {
                o1.nOption = si.getValue(x, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                newStats.put(MaxHP, o1);
                o2.nOption = si.getValue(y, slv);
                o2.rOption = skillID;
                o2.tOption = si.getValue(time, slv);
                newStats.put(MaxMP, o2);
                tsm.sendStat(newStats);
            } else if (JobConstants.JobEnum.getJobById(chr.getJob()).getBeginnerJobId() * 10000 + DECENT_COMBAT_ORDERS == skillID) {
                o1.nOption = si.getValue(x, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(CombatOrders, o1);
            } else if (JobConstants.JobEnum.getJobById(chr.getJob()).getBeginnerJobId() * 10000 + DECENT_ADVANCED_BLESSING == skillID) {
                o1.nValue = si.getValue(indieMhp, slv);
                o1.nReason = skillID;
                o1.tTerm = si.getValue(time, slv);
                newStats.put(IndieMHP, o1);
                o2.nValue = si.getValue(indieMhp, slv);
                o2.nReason = skillID;
                o2.tTerm = si.getValue(time, slv);
                newStats.put(IndieMMP, o2);
                o3.nOption = 1;
                o3.rOption = skillID;
                o3.tOption = si.getValue(time, slv);
                newStats.put(UsefulAdvancedBless, o3);
                tsm.sendStat(newStats);
            } else if (JobConstants.JobEnum.getJobById(chr.getJob()).getBeginnerJobId() * 10000 + DECENT_SPEED_INFUSION == skillID) {
                PartyBooster pb = (PartyBooster) tsm.getTSBByTSIndex(TSIndex.PartyBooster);
                pb.setNOption(si.getValue(x, slv));
                pb.setROption(skillID);
                pb.setCurrentTime(Util.getCurrentTime());
                pb.setStartTime(System.currentTimeMillis());
                pb.setExpireTerm(si.getValue(time, slv));
                tsm.sendStat(PartyBooster, pb.getOption());
            } else {
                switch (skillID) {
                    case net.swordie.ms.client.character.items.Tms282EquipmentEffects.FIRESTARTER:
                        net.swordie.ms.client.character.items.Tms282EquipmentEffects.firestart(chr);
                        break;
                    case net.swordie.ms.client.character.items.Tms282EquipmentEffects.ASCETIC:
                        net.swordie.ms.client.character.items.Tms282EquipmentEffects.ascetic(chr);
                        break;
                    case net.swordie.ms.client.character.items.Tms282EquipmentEffects.LUCID_NIGHTMARE:
                        net.swordie.ms.client.character.items.Tms282EquipmentEffects.lucidNightmare(chr);
                        break;
                    case net.swordie.ms.client.character.items.Tms282MugongSoul.SKILL:
                        // TMS SkillService atomically consumes soul charge and applies this buff;
                        // the optional legacy effects adapter must not apply it a second time.
                        break;
                    case MONOLITH, FURY_TOTEM:
                        if (ownsSpawnTotem(skillID, net.swordie.ms.connection.protocol.Protocol.active().version(), chr::hasItem)) {
                            if (chr.getInstance() != null) {
                                break;
                            }
                            Summon currentSummon = field.getSummonBySkillId(chr, skillID);
                            if (currentSummon != null) {
                                field.setTotem(new Tuple<>(false, chr.getUser().getVipGrade()));
                            }
                            summon = Summon.getSummonByAndSetStat(chr, skillID, slv);
                            summon.setMoveAbility(MoveAbility.Stop);
                            if (skillID == MONOLITH && net.swordie.ms.connection.protocol.Protocol.active().version()
                                    == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) {
                                summon.setAssistType(AssistType.None);
                                summon.setAttackActive(false); // WZ has stand/die only; this is a spawn-rate totem.
                            }
                            field.spawnSummon(summon);
                            field.setTotem(new Tuple<>(true, chr.getUser().getVipGrade()));
                            //String msg = "";
                            //msg += String.format("#fs15#Active Totem For %s \r\n", chr.getUser().getVipGrade() == 0 ? "Normal Player" : "MVP User");
                            //msg += String.format("#fs12#Increase Total Mob: #r#e%d#n#k To #r#e%d#n#k \r\n", field.getFixedMobCapacity(), field.getMobCapacity());
                            //msg += String.format("Increase Mobs Per Wave: #r#e%d#n#k To #r#e%d#n#k \r\n", field.getMobGens().size(), field.getMobGens().size() + field.getBonusMobWaves());
                            //msg += String.format("Decrease Spawn Time: #r#e%d ms#n#k To #r#e%d ms#n#k \r\n", FieldConstants.BASE_MOB_RESPAWN_RATE, field.getRespawnRate());
                            //chr.sendPopupSay(msg);
                        } else {
                            chr.chatMessage("沒有圖騰，無法使用這個技能。");
                        }
                        break;
                    case WHITE_ANGELIC_BLESSING, WHITE_ANGELIC_BLESSING_2, LIGHTNING_GOD_RING, LIGHTNING_GOD_RING_2, GUARD_RING, SUN_RING, RAIN_RING, RAINBOW_RING, SNOW_RING, LIGHTNING_RING, WIND_RING:
                        summon = Summon.getSummonByAndSetStat(chr, skillID, slv);
                        summon.setMoveAction((byte) 4);
                        summon.setAssistType(AssistType.Heal);
                        summon.setFlyMob(true);
                        field.spawnSummon(summon);
                        break;
                    case ELEMENTAL_SYLPH, FLAME_SYLPH, THUNDER_SYLPH, ICE_SYLPH, EARTH_SYLPH, DARK_SYLPH, HOLY_SYLPH, SALAMANDER_SYLPH, ELECTRON_SYLPH, UNDINE_SYLPH, GNOME_SYLPH, DEVIL_SYLPH, ANGEL_SYLPH, ELEMENTAL_SYLPH_2, FLAME_SYLPH_2, THUNDER_SYLPH_2, ICE_SYLPH_2, EARTH_SYLPH_2, DARK_SYLPH_2, HOLY_SYLPH_2, SALAMANDER_SYLPH_2, ELECTRON_SYLPH_2, UNDINE_SYLPH_2, GNOME_SYLPH_2, DEVIL_SYLPH_2, ANGEL_SYLPH_2:
                        summon = Summon.getSummonByAndSetStat(chr, skillID, slv);
                        field.spawnSummon(summon);
                        break;
                    case CLOSE_CALLS, CLOSE_CALLS_LINK:
                        if (!tsm.hasStatBySkillId(skillID)) {
                            o1.nOption = 1;
                            o1.rOption = skillID;
                            tsm.sendStat(PreReviveOnce, o1);
                        }
                        break;
                    case KNIGHT_WATCH:
                        if (!tsm.hasStatBySkillId(skillID)) {
                            o1.nReason = skillID;
                            o1.nValue = si.getValue(indieDamR, slv);
                            o1.tTerm = si.getValue(time, slv);
                            newStats.put(IndieDamR, o1);
                            o2.nOption = 1;
                            o2.rOption = skillID;
                            o2.tOption = si.getValue(time, slv);
                            newStats.put(Barrier, o2);
                            tsm.sendStat(newStats);
                        }
                        break;
                    case KNIGHT_WATCH_LINK:
                        if (!tsm.hasStatBySkillId(skillID)) {
                            o1.nValue = si.getValue(indieStance, slv);
                            o1.nReason = skillID;
                            o1.tTerm = si.getValue(time, slv);
                            tsm.sendStat(IndieStance, o1);
                        }
                        break;
                    case TERMS_CONDITIONS:
                    case TERMS_CONDITIONS_LINK:
                        if (!tsm.hasStatBySkillId(skillID)) {
                            o1.nValue = si.getValue(indieDamR, slv);
                            o1.nReason = skillID;
                            o1.tTerm = si.getValue(time, slv);
                            tsm.sendStat(IndieDamR, o1);
                        }
                        break;
                    case IRON_SPIRIT:
                        if (!tsm.hasStatBySkillId(skillID)) {
                            o1.nOption = 100;
                            o1.rOption = skillID;
                            o1.tOption = si.getValue(time, slv);
                            //tsm.sendStat(IronSpirit, o1);
                        }
                        break;
                    case TANADIAN_RUIN:
                        if (net.swordie.ms.connection.protocol.Protocol.active().version()
                                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) break;
                        if (!tsm.hasStatBySkillId(skillID)) {
                            o1.nOption = 15;
                            o1.rOption = skillID;
                            tsm.sendStat(BlackMageWeaponDestruction, o1);
                        }
                        break;
                    case AEONIAN_RISE:
                        // Original282 Service applies/cancels this state and authorizes the early-cancel burst.
                        if (net.swordie.ms.connection.protocol.Protocol.active().version()
                                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) break;
                        if (!tsm.hasStatBySkillId(skillID)) {
                            o1.nReason = skillID;
                            o1.nValue = 1;
                            o1.tTerm = si.getValue(time, slv);
                            newStats.put(IndieNotDamaged, o1);
                            o2.nOption = 1;
                            o2.rOption = skillID;
                            o2.tOption = si.getValue(time, slv);
                            newStats.put(NotDamaged, o2);
                            o3.nOption = 1;
                            o3.rOption = skillID;
                            o3.tOption = si.getValue(time, slv);
                            newStats.put(BlackMageWeaponCreation, o3);
                            tsm.sendStat(newStats);
                        } else {
                            tsm.removeStatsBySkill(skillID);
                        }
                        break;
                    // Hero's Echo:
                    case 1005:
                    case 10001005:
                    case 10001215:
                    case 20011005:
                    case 20021005:
                    case 20031005:
                    case 20041005:
                    case 20051005:
                    case 30001005:
                    case 30011005:
                    case 30021005:
                    case 40011005:
                    case 40021005:
                    case 50001005:
                    case 50001215:
                    case 140001005:
                    case 170001005:
                    case 170011005:
                        if (!tsm.hasStatBySkillId(skillID)) {
                            o1.nValue = si.getValue(x, slv);
                            o1.nReason = skillID;
                            o1.tTerm = si.getValue(time, slv);
                            tsm.sendStat(IndieDamR, o1);
                        }
                        break;
                    case MYSTICAL_POWER_OF_THE_HAT:
                        if (!tsm.hasStatBySkillId(skillID)) {
                            o1.nValue = 15;
                            o1.nReason = skillID;
                            o1.tTerm = si.getValue(time, slv);
                            tsm.sendStat(IndieNBDR, o1);
                        }
                        break;
                    case MAPLERUNNER_DASH:
                        o1.nReason = o2.nReason = skillID;
                        o1.tTerm = o2.tTerm = si.getValue(time, slv);
                        o1.nValue = si.getValue(indieForceJump, slv);
                        newStats.put(IndieForceJump, o1);
                        o2.nValue = si.getValue(indieForceSpeed, slv);
                        newStats.put(IndieForceSpeed, o2);
                        tsm.sendStat(newStats);
                        break;
                    case Beginner.NIMBLE_FEET:
                        o1.nOption = 5 + 5 * slv;
                        o1.rOption = skillID;
                        o1.tOption = 4 * slv;
                        tsm.sendStat(Speed, o1);
                        chr.addSkillCooldown(skillID, 60000);
                        break;
                    case Beginner.RECOVERY:
                        o1.rOption = skillID;
                        o1.tOption = 30;
                        tsm.sendStat(Restoration, o1);
                        recoveryInterval();
                        chr.addSkillCooldown(skillID, 600000);
                        break;
                    case A_QUEENLY_FRAGRANCE:
                        o1.nReason = skillID;
                        o1.tTerm = si.getValue(time, slv);
                        o1.nValue = si.getValue(indieMhpR, slv);
                        newStats.put(IndieMHPR, o1);
                        o2.nReason = skillID;
                        o2.tTerm = si.getValue(time, slv);
                        o2.nValue = si.getValue(indieMmpR, slv);
                        newStats.put(IndieMMPR, o2);
                        tsm.sendStat(newStats);
                        break;
                    case HAPPY_NEW_WEEK:
                        o1.nReason = skillID;
                        o1.tTerm = si.getValue(time, slv);
                        o1.nValue = si.getValue(indieMhp, slv);
                        newStats.put(IndieMHP, o1);
                        o2.nReason = skillID;
                        o2.tTerm = si.getValue(time, slv);
                        o2.nValue = si.getValue(indieMmp, slv);
                        newStats.put(IndieMMP, o2);
                        o3.nReason = skillID;
                        o3.tTerm = si.getValue(time, slv);
                        o3.nValue = si.getValue(indieAllStat, slv);
                        newStats.put(IndieAllStat, o3);
                        tsm.sendStat(newStats);
                        break;
                    case DECENT_MYSTIC_DOOR:
                    case DECENT_MYSTIC_DOOR_V:
                        Field townField = FieldData.getFieldById(chr.getField().getReturnMap());
                        Position townPosition = new Position(townField.getPortalByName("tp").getX(), townField.getPortalByName("tp").getY());
                        int duration = si.getValue(time, slv);
                        if (chr.getTownPortal() != null) {
                            TownPortal townPortal = chr.getTownPortal();
                            townPortal.despawnTownPortal();
                        }
                        TownPortal townPortal = new TownPortal(chr, townPosition, chr.getPosition(), chr.getField().getReturnMap(), chr.getFieldID(), skillID, duration);
                        townPortal.spawnTownPortal();
                        chr.dispose();
                        break;
                    case DECENT_SHARP_EYES_V:
                        // Short nOption is split in  2 bytes,  first one = CritDmg  second one = Crit%
                        int cr = si.getValue(x, slv);
                        int crDmg = si.getValue(y, slv);
                        o1.nOption = (cr << 8) + crDmg;
                        o1.rOption = skillID;
                        o1.tOption = si.getValue(time, slv);
                        o1.xOption = cr;
                        o1.yOption = crDmg;
                        tsm.sendStat(SharpEyes, o1);
                        break;
                    case DECENT_HYPER_BODY_V:
                        o1.nOption = si.getValue(x, slv);
                        o1.rOption = skillID;
                        o1.tOption = si.getValue(time, slv);
                        newStats.put(MaxHP, o1);
                        o2.nOption = si.getValue(y, slv);
                        o2.rOption = skillID;
                        o2.tOption = si.getValue(time, slv);
                        newStats.put(MaxMP, o2);
                        tsm.sendStat(newStats);
                        break;
                    case DECENT_COMBAT_ORDERS_V:
                        o1.nOption = 1;
                        o1.rOption = skillID;
                        o1.tOption = si.getValue(time, slv);
                        tsm.sendStat(CombatOrders, o1);
                        break;
                    case DECENT_ADV_BLESSING_V:
                        o1.nOption = si.getValue(z, slv);
                        o1.rOption = skillID;
                        o1.tOption = si.getValue(time, slv);
                        newStats.put(PDD, o1);
                        o3.nValue = si.getValue(x, slv);
                        o3.nReason = skillID;
                        o3.tTerm = si.getValue(time, slv);
                        newStats.put(IndiePAD, o3);
                        newStats.put(IndieMAD, o3.deepCopy());
                        o4.nValue = si.getValue(indieMhp, slv);
                        o4.nReason = skillID;
                        o4.tTerm = si.getValue(time, slv);
                        newStats.put(IndieMHP, o4);
                        newStats.put(IndieMMP, o4.deepCopy());
                        tsm.sendStat(newStats);
                        break;
                    case DECENT_SPEED_INFUSION_V:
                        o1.nReason = skillID;
                        o1.nValue = -1;
                        o1.tTerm = si.getValue(time, slv);
                        tsm.sendStat(IndieBooster, o1);
                        break;
                    case DECENT_HOLY_SYMBOL_V:
                        o1.nOption = si.getValue(x, slv);
                        o1.rOption = skillID;
                        o1.tOption = si.getValue(time, slv);
                        newStats.put(HolySymbol, o1);
                        o2.nOption = si.getValue(v, slv);
                        o2.rOption = skillID;
                        o2.tOption = si.getValue(time, slv);
                        newStats.put(DropRate, o2);
                        tsm.sendStat(newStats);
                        break;
                    case LOADED_DICE:
                        int chooseNumber = inPacket.decodeByte();
                        Option o = new Option();
                        o.nOption = chooseNumber;
                        o.rOption = skillID;
                        tsm.sendStat(LoadedDice, o);

                        if (!JobConstants.isCannoneer(chr.getJob()) && !JobConstants.isBuccaneer(chr.getJob()) && !JobConstants.isCorsair(chr.getJob())) {
                            si = SkillData.getSkillInfoById(400051001);
                            chr.removeBaseStatByOption();
                            Effect eff = Effect.avatarOriented("Skill/40005.img/skill/400051001/affected/" + chooseNumber);
                            chr.write(UserPacket.effect(eff));
                            chr.getField().broadcast(UserRemote.effect(chr.getId(), eff), chr);
                            if (chooseNumber < 2) {
                                return;
                            }
                            chr.addBaseStatByDiceNumber(chooseNumber, si, slv);
                            o1.nOption = chooseNumber;
                            o1.rOption = 400051001;
                            o1.tOption = 180;
                            tsm.throwDice(chooseNumber);
                            tsm.sendStat(Dice, o1);
                        }
                        break;
                    case MANA_OVERLOAD:
                        if (tsm.hasStat(Wizard_OverloadMana)) {
                            tsm.removeStatsBySkill(MANA_OVERLOAD);
                        } else {
                            o1.nOption = 10;
                            o1.rOption = skillID;
                            newStats.put(Wizard_OverloadMana, o1);
                            o2.nReason = skillID;
                            o2.nValue = si.getValue(z, slv);
                            newStats.put(IndiePMdR, o2);
                            tsm.sendStat(newStats);
                        }
                        break;
                    case GUIDED_ARROW:
                        int faKey = chr.getNewForceAtomKey();
                        o1.nOption = si.getValue(z, slv);
                        o1.rOption = skillID;
                        o1.tOption = si.getValue(time, slv);
                        o1.xOption = faKey;
                        tsm.sendStat(GuidedArrow, o1);

                        ForceAtomEnum fae = ForceAtomEnum.GUIDED_ARROW;
                        ForceAtomInfo forceAtomInfo = new ForceAtomInfo(faKey, fae.getInc(), 41, 3,
                                90, 840, Util.getCurrentTime(), 0, 0,
                                new Position());
                        ForceAtom fa = new ForceAtom(false, 0, chr.getId(), fae,
                                true, 0, skillID, forceAtomInfo, si.getFirstRect(), 0, 300,
                                new Position(), skillID, new Position(), 0);
                        fa.setMaxRecreationCount(si.getValue(z, slv));
                        chr.createForceAtom(fa);
                        break;
                    case VENOM_BURST:
                        Rect rect = chr.getPosition().getRectAround(si.getRects().get(0));
                        if (!chr.isLeft()) {
                            rect = rect.moveRight();
                        }
                        int i = 1;
                        int max = si.getValue(y, slv);
                        BurnedInfo bi = BurnedInfo.createBurnInfo(chr, skillID, slv, 1);
                        for (Life life : chr.getField().getLifesInRect(rect)) {
                            if (i == max) {
                                break;
                            }
                            if (life instanceof Mob mob && mob.getHp() > 0) {
                                MobTemporaryStat mts = mob.getTemporaryStat();
                                mts.createAndAddBurnedInfo(mob, bi, skillID);
                                i++;
                            }
                        }
                        break;
                    case ETHEREAL_FORM:
                        o1.nOption = si.getValue(x, slv);
                        o1.rOption = skillID;
                        o1.tOption = si.getValue(time, slv);
                        o1.xOption = si.getValue(s, slv); // RGB
                        o1.yOption = si.getValue(y, slv);
                        tsm.sendStat(EtherealForm, o1);
                        break;
                    case WILL_OF_ERDA:
                        o1.nOption = 100;
                        o1.rOption = skillID;
                        o1.tOption = 3;
                        tsm.sendStat(AsrR, o1);
                        tsm.removeAllDebuffs();
                        break;
                    case IMPENETRABLE_SKIN:
                        o1.nOption = 1;
                        o1.rOption = skillID;
                        o1.tOption = si.getValue(time, slv);
                        o1.xOption = 0; // stack
                        newStats.put(HitStackDamR, o1);
                        o2.nReason = skillID;
                        o2.nValue = si.getValue(indieAsrR, slv);
                        o2.tTerm = si.getValue(time, slv);
                        newStats.put(IndieAsrR, o2);
                        o3.nReason = skillID;
                        o3.nValue = 1;
                        o3.tTerm = si.getValue(time, slv);
                        newStats.put(IndieApplySuperStance, o3);
                        tsm.sendStat(newStats);
                        break;
                    case WEAPON_AURA:
                        o1.nOption = si.getValue(z, slv);
                        o1.rOption = skillID;
                        o1.tOption = si.getValue(time, slv);
                        newStats.put(Warrior_AuraWeapon, o1);
                        o2.nValue = si.getValue(indieIgnoreMobpdpR, slv);
                        o2.nReason = skillID;
                        o2.tTerm = si.getValue(time, slv);
                        newStats.put(IndieIgnoreMobpdpR, o2);
                        o3.nValue = si.getValue(indiePMdR, slv);
                        o3.nReason = skillID;
                        o3.tTerm = si.getValue(time, slv);
                        newStats.put(IndiePMdR, o3);
                        tsm.sendStat(newStats);
                        if (net.swordie.ms.connection.packet.Tms282RemoteBuffs.active()) break;
                        o4.nOption = tsm.hasStat(Warrior_AuraWeaponStack) ? (int) (tsm.getTotalNOptionOfStat(Warrior_AuraWeaponStack) - 1) : 1;
                        o4.rOption = WEAPON_AURA;
                        tsm.sendStat(Warrior_AuraWeaponStack, o4);
                        break;
                    case LAST_RESORT:
                        int nOption = tsm.hasStat(ReadyToDie) ? tsm.getOption(ReadyToDie).nOption : 0;
                        long remainingTime = tsm.getRemainingTime(ReadyToDie, LAST_RESORT);
                        tsm.removeStatsBySkill(LAST_RESORT);
                        switch (nOption) {
                            case 0:
                                o1.nOption = 1;
                                o1.rOption = skillID;
                                o1.tOption = si.getValue(time, slv);
                                newStats.put(ReadyToDie, o1);
                                o2.nValue = si.getValue(x, slv);
                                o2.nReason = skillID;
                                o2.tTerm = si.getValue(time, slv);
                                newStats.put(IndieEVARReduceR, o2);
                                o3.nValue = si.getValue(z, slv);
                                o3.nReason = skillID;
                                o3.tTerm = si.getValue(time, slv);
                                newStats.put(IndieAllHitDamR, o3);
                                o4.nReason = skillID;
                                o4.nValue = si.getValue(y, slv);
                                o4.tTerm = si.getValue(time, slv);
                                newStats.put(IndiePMdR, o4);
                                tsm.sendStat(newStats);
                                break;
                            case 1:
                                o1.nOption = 2;
                                o1.rOption = skillID;
                                o1.tOption = (int) ((remainingTime) / 2);
                                o1.setInMillis(true);
                                newStats.put(ReadyToDie, o1);
                                o2.nValue = si.getValue(w, slv);
                                o2.nReason = skillID;
                                o2.tTerm = (int) ((remainingTime) / 2);
                                o2.setInMillis(true);
                                newStats.put(IndieEVARReduceR, o2);
                                o3.nValue = si.getValue(s, slv);
                                o3.nReason = skillID;
                                o3.tTerm = (int) ((remainingTime) / 2);
                                o3.setInMillis(true);
                                newStats.put(IndieAllHitDamR, o3);
                                o4.nReason = skillID;
                                o4.nValue = si.getValue(q, slv);
                                o4.tTerm = (int) ((remainingTime) / 2);
                                o4.setInMillis(true);
                                newStats.put(IndiePMdR, o4);
                                tsm.sendStat(newStats);
                                break;
                        }
                        break;
                    case VICIOUS_SHOT:
                        o1.nOption = si.getValue(x, slv);
                        o1.rOption = skillID;
                        o1.tOption = si.getValue(time, slv);
                        tsm.sendStat(Oblivion, o1);
                        break;
                    case OVERDRIVE:
                        o1.nOption = si.getValue(x, slv);
                        o1.rOption = skillID;
                        o1.tOption = si.getValue(time, slv);
                        tsm.sendStat(ConvertAD, o1);
                        break;
                    case RESISTANCE_INFANTRY_1:
                    case RESISTANCE_INFANTRY_2:
                    case ERDA_FOUNTAIN:
                        summon = Summon.getSummonByAndSetStat(chr, skillID, slv);
                        field = chr.getField();
                        summon.setMoveAbility(MoveAbility.Stop);
                        field.spawnSummon(summon);
                        break;
                    //Fall through intended for all Freuds Wisdom Skill cases
                    case FREUDS_WISDOM_6:
                        o1.nOption = 1;
                        o1.rOption = skillID;
                        o1.tOption = si.getValue(time, slv);
                        newStats.put(NotDamaged, o1);
                    case FREUDS_WISDOM_5:
                        o2.nReason = skillID;
                        o2.nValue = si.getValue(indieBDR, slv);
                        o2.tTerm = si.getValue(time, slv);
                        newStats.put(IndieBDR, o2);
                    case FREUDS_WISDOM_4:
                        o3.nReason = skillID;
                        o3.nValue = si.getValue(indiePad, slv);
                        o3.tTerm = si.getValue(time, slv);
                        newStats.put(IndiePAD, o3);
                        newStats.put(IndieMAD, o3);
                    case FREUDS_WISDOM_3:
                        o4.nReason = skillID;
                        o4.nValue = si.getValue(indieAllStat, slv);
                        o4.tTerm = si.getValue(time, slv);
                        newStats.put(IndieAllStat, o4);
                    case FREUDS_WISDOM_2:
                        o5.nReason = skillID;
                        o5.nValue = si.getValue(indieStance, slv);
                        o5.tTerm = si.getValue(time, slv);
                        newStats.put(IndieStance, o5);
                    case FREUDS_WISDOM_1:
                        o6.nOption = tsm.hasStat(FreudBlessing) ? tsm.getOption(FreudBlessing).nOption + 1 : 1;
                        o6.rOption = skillID;
                        o6.tOption = si.getValue(time, slv);
                        newStats.put(FreudBlessing, o6);
                        tsm.sendStat(newStats);

                        int cooldown = 25000; // in ms
                        if (skillID == FREUDS_WISDOM_6) {
                            cooldown = 240000;
                        }
                        chr.addSkillCoolTime(FREUDS_WISDOM, Util.getCurrentTimeLong() + cooldown); // value isn't included in SkillId
                        for (int skillId : chr.getSkillCoolTimes().keySet()) {
                            si = SkillData.getSkillInfoById(skillId);
                            if (si != null) {
                                chr.reduceSkillCoolTime(skillId, (long) (chr.getRemainingCoolTime(skillId) * 0.1F));
                            }
                        }
                        break;
                    case SENGOKU_FORCE_ASSEMBLE:
                        summonSengokuForces();
                        break;
                    case CONVERSION_OVERDRIVE:
                        o1.nOption = 1;
                        o1.rOption = skillID;
                        o1.tOption = si.getValue(time, slv);
                        tsm.sendStat(LPMagicCircuitFullDrive, o1);
                        break;
                    case MAPLE_WORLD_GODDESS_BLESSING:
                        o1.nReason = skillID;
                        o1.tTerm = si.getValue(time, slv);
                        o1.nValue = si.getValue(indieDamR, slv);
                        newStats.put(IndieDamR, o1);
                        o2.nReason = skillID;
                        o2.tTerm = si.getValue(time, slv);
                        o2.nValue = si.getValue(x, slv);
                        newStats.put(IndieAllStat, o2);
                        tsm.sendStat(newStats);
                        break;
                    case EMPRESS_CYGNUS_BLESSING: {
                        o1.nOption = 1;
                        o1.rOption = skillID;
                        o1.tOption = si.getValue(time, slv);
                        o1.startTime = System.currentTimeMillis();
                        newStats.put(FifthGoddessBless, o1);
                        o2.nReason = skillID;
                        o2.tTerm = si.getValue(time, slv);
                        o2.nValue = si.getValue(q, slv);
                        newStats.put(IndieDamR, o2);
                        tsm.sendStat(newStats);

                        if (this.bless5thTimer != null) {
                            this.bless5thTimer.cancel(false);
                            tsm.removeStatsBySkill(EMPRESS_CYGNUS_BLESSING);
                            tsm.removeStatsBySkill(TRANSCENDENT_CYGNUS_BLESSING);
                        }
                        ScheduledFuture<?> sf = chr.getTimer().addFixedRateEvent(() -> setBless5thTimer(skillID), 1000, 1500, TimeUnit.MILLISECONDS, false);
                        this.bless5thTimer = sf;
                        GlobalTimerManager.addCharTimer(chr.getId(), sf);
                        break;
                    }
                    case TRANSCENDENT_CYGNUS_BLESSING: {
                        o1.nOption = 1;
                        o1.rOption = skillID;
                        o1.tOption = si.getValue(time, slv);
                        o1.startTime = System.currentTimeMillis();
                        newStats.put(FifthGoddessBless, o1);
                        o2.nReason = skillID;
                        o2.tTerm = si.getValue(time, slv);
                        o2.nValue = si.getValue(q, slv);
                        newStats.put(IndieDamR, o2);
                        o3.nReason = skillID;
                        o3.tTerm = si.getValue(time, slv);
                        o3.nValue = si.getValue(z, slv);
                        newStats.put(IndieDamReduceR, o3);
                        tsm.sendStat(newStats);

                        if (this.bless5thTimer != null) {
                            this.bless5thTimer.cancel(false);
                            tsm.removeStatsBySkill(EMPRESS_CYGNUS_BLESSING);
                            tsm.removeStatsBySkill(TRANSCENDENT_CYGNUS_BLESSING);
                        }
                        ScheduledFuture<?> sf = chr.getTimer().addFixedRateEvent(() -> setBless5thTimer(skillID), 1000, 1500, TimeUnit.MILLISECONDS, false);
                        this.bless5thTimer = sf;
                        GlobalTimerManager.addCharTimer(chr.getId(), sf);
                        break;
                    }
                    case GRANDIS_GODDESS_BLESSING:
                        int grandisSkillID = 0;
                        if (JobConstants.isKaiser(chr.getJob())) {
                            grandisSkillID = GRANDIS_GODDESS_BLESSING_KAISER;
                            SkillInfo grandisSkillInfo = SkillData.getSkillInfoById(grandisSkillID);
                            o1.nOption = 1;
                            o1.rOption = grandisSkillID;
                            o1.tOption = si.getValue(time, slv);
                            o1.zOption = si.getValue(y, slv);
                            o1.startTime = System.currentTimeMillis();
                            // Skills have a #x% chance to bypass cooldown as many as #y time(s).
                            newStats.put(FifthGoddessBless, o1);
                            o2.nReason = grandisSkillID;
                            o2.tTerm = si.getValue(time, slv);
                            o2.nValue = grandisSkillInfo.getValue(indieDamR, slv);
                            newStats.put(IndieDamR, o2);
                            o3.nReason = grandisSkillID;
                            o3.tTerm = si.getValue(time, slv);
                            o3.nValue = grandisSkillInfo.getValue(indieStance, slv);
                            newStats.put(IndieStance, o3);
                            o4.nOption = si.getValue(v, slv);
                            o4.rOption = grandisSkillID;
                            o4.tOption = si.getValue(time, slv);
                            newStats.put(DamR, o4);
                            tsm.sendStat(newStats);
                            chr.write(UserPacket.effect(Effect.skillUse(grandisSkillID, chr.getLevel(), slv)));
                            chr.getField().broadcast(UserRemote.effect(chr.getId(), Effect.skillUse(grandisSkillID, chr.getLevel(), slv)), chr);
                        } else if (JobConstants.isFlora(chr.getJob())) {
                            grandisSkillID = GRANDIS_GODDESS_BLESSING_FLORA;
                            SkillInfo grandisSkillInfo = SkillData.getSkillInfoById(grandisSkillID);
                            o1.nOption = 1;
                            o1.rOption = grandisSkillID;
                            o1.tOption = si.getValue(time, slv);
                            o1.startTime = System.currentTimeMillis();
                            newStats.put(FifthGoddessBless, o1);
                            o2.nReason = grandisSkillID;
                            o2.tTerm = si.getValue(time, slv);
                            o2.nValue = grandisSkillInfo.getValue(indiePad, slv);
                            newStats.put(IndiePAD, o2);
                            o3.nReason = grandisSkillID;
                            o3.tTerm = si.getValue(time, slv);
                            o3.nValue = grandisSkillInfo.getValue(indieMad, slv);
                            newStats.put(IndieMAD, o3);
                            tsm.sendStat(newStats);
                            chr.write(UserPacket.effect(Effect.skillUse(grandisSkillID, chr.getLevel(), slv)));
                            chr.getField().broadcast(UserRemote.effect(chr.getId(), Effect.skillUse(grandisSkillID, chr.getLevel(), slv)), chr);
                        } else if (JobConstants.isHoYoung(chr.getJob())) {
                            grandisSkillID = GRANDIS_GODDESS_BLESSING_AMINA;
                            SkillInfo grandisSkillInfo = SkillData.getSkillInfoById(grandisSkillID);
                            o1.nOption = si.getValue(x, slv);
                            o1.rOption = grandisSkillID;
                            o1.tOption = si.getValue(time, slv);
                            o1.startTime = System.currentTimeMillis();
                            newStats.put(FifthGoddessBless, o1);
                            o2.nReason = grandisSkillID;
                            o2.tTerm = si.getValue(time, slv);
                            o2.nValue = grandisSkillInfo.getValue(indieDamR, slv);
                            newStats.put(IndieDamR, o2);
                            tsm.sendStat(newStats);
                            chr.write(UserPacket.effect(Effect.skillUse(grandisSkillID, chr.getLevel(), slv)));
                            chr.getField().broadcast(UserRemote.effect(chr.getId(), Effect.skillUse(grandisSkillID, chr.getLevel(), slv)), chr);
                        }
                        break;
                    case OTHERWORLD_GODDESS_BLESSING:
                        o1.nOption = 1;
                        o1.rOption = skillID;
                        o1.tOption = si.getValue(time, slv);
                        o1.startTime = System.currentTimeMillis();
                        newStats.put(FifthGoddessBless, o1);
                        o2.nReason = skillID;
                        o2.tTerm = si.getValue(time, slv);
                        o2.nValue = si.getValue(indiePMdR, slv);
                        newStats.put(IndiePMdR, o2);
                        tsm.sendStat(newStats);
                        break;
                    case LOTUS_FLOWER:
                        if (tsm.hasStatBySkillId(Bishop.HEXA_HEAVENS_DOOR) || tsm.hasStatBySkillId(HEAVEN_DOOR)) {
                            chr.chatMessage("天堂之門效果持續中，無法使用。");
                            break;
                        }
                        summon = Summon.getSummonByAndSetStat(c.getChr(), skillID, slv);
                        summon.setMoveAction((byte) 4);
                        summon.setMoveAbility(MoveAbility.Stop);
                        summon.setAssistType(AssistType.Attack);
                        summon.setFlyMob(false);
                        field.spawnSummon(summon);
                        lotusFlowerTrigged = false;
                        break;
                    case UFO_RAID:
                        summon = Summon.getSummonByAndSetStat(c.getChr(), skillID, slv);
                        summon.setMoveAction((byte) 4);
                        summon.setAssistType(AssistType.Attack);
                        summon.setMoveAbility(MoveAbility.Stop);
                        summon.setFlyMob(false);
                        field.spawnSummon(summon);
                        break;
                    case SOL_JANUS_DAWN:
                    case SOL_JANUS_DAWN_2:
                    case SOL_JANUS_DAWN_3: {
                        if (!chr.hasSkill(SOL_JANUS_ACTIVATION)) {
                            break;
                        }
                        int maxCount = slv / 10;
                        int nowCount = 0;
                        Summon firstJanus = null;
                        for (Summon s : field.getSummonsByChar(chr)) {
                            int id = s.getSkillID();
                            if (id != SOL_JANUS_DAWN
                                    && id != SOL_JANUS_DAWN_2
                                    && id != SOL_JANUS_DAWN_3) {
                                continue;
                            }
                            if (firstJanus == null) {
                                firstJanus = s;
                            }
                            nowCount++;
                        }
                        if (nowCount == maxCount + 1 && firstJanus != null) {
                            field.removeLife(firstJanus);
                        }
                        int summonID;
                        if (nowCount == 1) {
                            summonID = SOL_JANUS_DAWN;
                        } else if (nowCount == 2) {
                            summonID = SOL_JANUS_DAWN_2;
                        } else {
                            summonID = SOL_JANUS_DAWN_3;
                        }
                        summon = Summon.getSummonByAndSetStat(chr, summonID, slv);
                        summon.setMoveAction((byte) 4);
                        summon.setAssistType(AssistType.Attack);
                        summon.setMoveAbility(MoveAbility.Stop);
                        summon.setFlyMob(false);
                        field.spawnAddSummon(summon);
                        nowCount++;
                        if (nowCount >= maxCount) {
                            chr.addSkillCooldown(skillID, 60000);
                        }
                        break;
                    }
                    case BOSS_SLAYERS:
                        o1.nReason = skillID;
                        o1.tTerm = si.getValue(time, slv);
                        o1.nValue = si.getValue(indieBDR, slv);
                        tsm.sendStat(IndieBDR, o1);
                        break;
                    case UNDETERRED:
                        o1.nReason = skillID;
                        o1.tTerm = si.getValue(time, slv);
                        o1.nValue = si.getValue(indieIgnoreMobpdpR, slv);
                        tsm.sendStat(IndieIgnoreMobpdpR, o1);
                        break;
                    case FOR_THE_GUILD:
                        o1.nReason = skillID;
                        o1.tTerm = si.getValue(time, slv);
                        o1.nValue = si.getValue(indieDamR, slv);
                        tsm.sendStat(IndieDamR, o1);
                        break;
                    case HARD_HITTER:
                    case WEAPON_TEMPERING:
                    case ADVANCED_WEAPON_TEMPERING:
                        o1.nReason = skillID;
                        o1.tTerm = si.getValue(time, slv);
                        o1.nValue = si.getValue(indieCD, slv);
                        tsm.sendStat(IndieCD, o1);
                        break;
                    case BOSS_PRACTICE_MODE_FOG_FOREST_TRAINING_GROUNDS_EXCLUSIVE:
                        o1.nReason = skillID;
                        o1.tTerm = si.getValue(time, slv);
                        o1.nValue = si.getValue(x, slv);
                        newStats.put(IndiePAD, o1);
                        newStats.put(IndieMAD, o1.deepCopy());
                        tsm.sendStat(newStats);
                        break;
                }
            }
            handleSixthJobOriginAscentSkill(chr, tsm, skillID, si, slv);
        }
    }

    private void applyNdTime(Char chr, SkillInfo si, int slv) {
        // TMS Saitama arms the same ndTime + grace once on its accepted visible action, never on follow-ups.
        if (si.getSkillId() == 122001008
                && net.swordie.ms.client.jobs.special.SaitamaActionProtection.of(chr) != null) return;
        if (si.getValue(ndTime, slv) > 0) {
            TemporaryStatManager tsm = chr.getTemporaryStatManager();
            if (!tsm.hasStatBySkillId(si.getSkillId())) {
                Option o = new Option();
                o.nReason = si.getSkillId();
                o.nValue = 1;
                o.tTerm = si.getValue(ndTime, slv) + 1000;
                o.isInMillis = true;
                tsm.sendStat(IndieNotDamaged, o);
            }
        }
    }

    public int handleSetCoolDownSkill(int skillId) {
        return 0;
    }

    public int alterCooldownSkill(int skillId) {
        Skill skill = chr.getSkill(skillId);
        if (skill == null) {
            return -1;
        }
        SkillInfo si = SkillData.getSkillInfoById(skillId);
        int slv = skill.getCurrentLevel();
        int cdInSec = si.getValue(SkillStat.cooltime, slv);
        int cdInMillis = cdInSec > 0 ? cdInSec * 1000 : si.getValue(SkillStat.cooltimeMS, slv);
        int cooldownReductionR = chr.getHyperPsdSkillsCooltimeR().getOrDefault(skillId, 0);
        if (cooldownReductionR > 0) {
            return (int) (cdInMillis - ((double) (cdInMillis * cooldownReductionR) / 100));
        }
        return -1;
    }

    /**
     * Gets called when Character receives a debuff from a Mob Skill
     */
    public void handleMobDebuffSkill(Char chr) {

    }

    /**
     * Used for Classes that have timers, to cancel the timer after changing
     * channel
     */
    public void handleCancelTimer(Char chr) {
        if (this.bless5thTimer != null) {
            this.bless5thTimer.cancel(true);
        }
    }

    public void handleShootObject(Char chr, ShootObjectSkillInfo sosi) {
        var skillId = sosi.getSkillId();
        var slv = sosi.getSlv();
        chr.setSkillCooldown(skillId, slv);
    }

    public void applyHpCost(int skillID) {
        if (skillID == 0 || !chr.hasSkill(skillID)) {
            return;
        }
        Skill skill = chr.getSkill(SkillConstants.getActualSkillIDfromSkillID(skillID));
        int slv = skill.getCurrentLevel();
        SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
        int hpRCost = si.getValue(hpRCon, slv);
        int hpCost = si.getValue(hpCon, slv);
        if (hpRCost > 0) {
            int skillcost = (int) (chr.getMaxHP() / ((double) 100 / hpRCost));
            if (chr.getHP() > skillcost) {
                chr.heal(-skillcost);
            } else {
                chr.heal(-(chr.getHP() - 1));
            }
        } else if (hpCost > 0) {
            if (chr.getHP() > hpCost) {
                chr.heal(-hpCost);
            } else {
                chr.heal(-(chr.getHP() - 1));
            }
        }
    }

    public void setBless5thTimer(int skillID) {
        int slv = chr.getSkillLevel(skillID);
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        SkillInfo si = SkillData.getSkillInfoById(skillID);
        Option o1 = new Option();
        chr.heal(chr.getMaxHP() * si.getValue(x, slv) / 100);
        int damageInc = 0;
        int term = tsm.hasStatBySkillId(skillID) ? (int) tsm.getRemainingTime(FifthGoddessBless, skillID) / 1000 : si.getValue(time, slv);
        Option currentOption = tsm.getOptByCTSAndSkill(IndieDamR, skillID);
        if (currentOption != null) {
            damageInc = currentOption.nValue;
        }
        if (damageInc + si.getValue(damage, slv) >= si.getValue(w, slv)) {
            damageInc = si.getValue(w, slv);
        } else {
            damageInc += si.getValue(damage, slv);
        }
        o1.nReason = skillID;
        o1.nValue = damageInc;
        o1.tTerm = term;
        tsm.sendStat(IndieDamR, o1);

        if (damageInc >= si.getValue(w, slv)) {
            if (this.bless5thTimer != null) {
                this.bless5thTimer.cancel(false);
            }
        }
    }

    public void recoveryInterval() {
        if (chr.hasSkill(Beginner.RECOVERY)) {
            Skill skill = chr.getSkill(Beginner.RECOVERY);
            TemporaryStatManager tsm = chr.getTemporaryStatManager();
            int slv = skill.getCurrentLevel();
            if (tsm.hasStat(Restoration)) {
                chr.heal(24 * slv / 3);
                chr.getTimer().addEvent(this::recoveryInterval, 10, TimeUnit.SECONDS);
            }
        }
    }

    /**
     * Handles the initial part of a hit, the initial packet processing.
     *
     * @param inPacket The packet to be processed
     */
    public void handleUserHit(Client c, InPacket inPacket) {
        if (net.swordie.ms.client.character.tms282.Tms282DefensiveSkills.blocksDamage(chr, System.currentTimeMillis())) return;
        if (net.swordie.ms.client.character.items.Tms282SpecialWeaponEffects.blocksDamage(chr, System.currentTimeMillis())) return;
        if (net.swordie.ms.client.jobs.special.SaitamaActionProtection.blocksDamage(chr, System.currentTimeMillis())) return;
        if (chr.isInvincible() || chr.getClient() == null) {
            return;
        }
        HitInfo hitInfo = new HitInfo();
        inPacket.decodeInt(); // Unknown
        hitInfo.damagedTime = inPacket.decodeInt();
        hitInfo.attackIdx = inPacket.decodeByte(); // -1 attack idx = body (touch) attack
        hitInfo.type = inPacket.decodeByte();
        hitInfo.elemAttr = inPacket.decodeByte();
        hitInfo.hpDamage = inPacket.decodeInt();
        inPacket.decodeByte(); // Hardcoded 0 crit
        inPacket.decodeByte(); // Hardcoded 0
        inPacket.decodeInt(); // Hardcoded 0
        boolean knockBack;
        if (hitInfo.type <= AttackIndex.Counter.getVal()) {
            hitInfo.obstacle = inPacket.decodeShort();
        } else {
            hitInfo.mobID = inPacket.decodeInt();
            hitInfo.templateID = inPacket.decodeInt();
            hitInfo.mobIdForMissCheck = inPacket.decodeInt();
            hitInfo.isLeft = inPacket.decodeByte() != 0;
            hitInfo.blockSkillId = inPacket.decodeInt();
            hitInfo.reducedDamage = inPacket.decodeInt();
            hitInfo.reflect = inPacket.decodeByte();
            hitInfo.guard = inPacket.decodeByte();
            if (hitInfo.guard == 2) {
                knockBack = true;
            }
            if (hitInfo.guard == 2 || hitInfo.reducedDamage > 0) {
                hitInfo.powerGuard = inPacket.decodeByte() != 0; // && nReflect > 0
                hitInfo.reflectMobID = inPacket.decodeInt();
                hitInfo.hitAction = inPacket.decodeByte();
                hitInfo.hitPos = inPacket.decodePosition();
                hitInfo.userHitPos = inPacket.decodePosition();
                if (hitInfo.powerGuard) {
                    hitInfo.reflectDamage = inPacket.decodeInt();
                }
            }
            hitInfo.stance = inPacket.decodeByte();
            hitInfo.stanceSkillID = inPacket.decodeInt();
            hitInfo.cancelSkillID = inPacket.decodeInt();
            hitInfo.reductionSkillID = inPacket.decodeInt();
        }
        inPacket.decodeByte(); // Hardcoded 0

        if (hitInfo.hpDamage == 0) {
            chr.write(UserLocal.dodgeSkillReady());
            return;
        }
        Mob mob = chr.getField().getMobByObjectId(hitInfo.mobID);
        if (mob != null && mob.getHp() <= 0 && hitInfo.hpDamage != 0) {
            chr.write(UserLocal.dodgeSkillReady());
            return;
        }
        handleHit(c, inPacket, hitInfo);
        handleHitDebuff(c, inPacket, hitInfo);
        handleHit(c, hitInfo);
    }

    /**
     * The final part of the hit process. Assumes the correct info (wrt buffs
     * for example) is already in <code>hitInfo</code>.
     *
     * @param c       The client
     * @param hitInfo The completed hitInfo
     */
    public void handleHit(Client c, HitInfo hitInfo) {
        Char chr = c.getChr();
        if (net.swordie.ms.client.character.tms282.Tms282DefensiveSkills.blocksDamage(chr, System.currentTimeMillis())) return;
        if (net.swordie.ms.client.character.items.Tms282SpecialWeaponEffects.blocksDamage(chr, System.currentTimeMillis())) return;
        if (net.swordie.ms.client.jobs.special.SaitamaActionProtection.blocksDamage(chr, System.currentTimeMillis())) return;
        int damageTaken = 0;
        if (chr.getGuild() != null) {
            GuildSkill gs = chr.getGuild().getSkillById(GuildConstants.UNITED_FRONT);
            SkillInfo si = SkillData.getSkillInfoById(GuildConstants.UNITED_FRONT);
            if (gs != null && si != null) {
                damageTaken = si.getValue(damAbsorbShieldR, gs.getLevel());
            }
        }
        hitInfo.hpDamage *= ((100.0D - damageTaken) / 100.0D); // Guild Skills
        hitInfo.hpDamage = (Math.max(0, hitInfo.hpDamage)); // to prevent -1 (dodges) healing the player.
        if (chr.is1HitKOSkill(hitInfo.hpDamage)) {
            return;
        }
        int curHP = chr.getStat(Stat.hp);
        final int beforeHitHp = curHP;
        int newHP = curHP - hitInfo.hpDamage;
        
        curHP = Math.max(newHP, 0);
        Map<Stat, Object> stats = new HashMap<>();
        chr.setStat(Stat.hp, curHP);
        stats.put(Stat.hp, curHP);
        if (!JobConstants.isNoManaJob(chr.getJob())) {
            int curMP = chr.getStat(Stat.mp);
            int newMP = curMP - hitInfo.mpDamage;
            curMP = Math.max(newMP, 0);
            chr.setStat(Stat.mp, curMP);
            stats.put(Stat.mp, curMP);
        }
        chr.sendStatsPacket(stats);
        //chr.getField().broadcastPacket(UserRemote.hit(chr, hitInfo), chr);
        if (beforeHitHp > 0 && curHP < beforeHitHp && hitInfo.hpDamage > 0 && Tms282VMatrixRuntime.managed(chr)) {
            Tms282VMatrixRuntime.event(chr, Tms282VMatrixSpecialEngine.Type.HIT, 1, 0);
        }
        if (chr.getParty() != null) {
            for (Char partyChar : chr.getParty().getOnlineChars()) {
                if (!partyChar.equals(chr)) {
                    partyChar.write(UserRemote.receiveHP(chr));
                }
            }
        }
        if (curHP <= 0) {
            var saitamaProtection = net.swordie.ms.client.jobs.special.SaitamaActionProtection.of(chr);
            if (saitamaProtection != null) saitamaProtection.clear(chr);
            TemporaryStatManager tsm = chr.getTemporaryStatManager();
            if (tsm.getOptByCTSAndSkill(ReviveOnce, Bishop.HEAVENS_DOOR) != null
                    || tsm.getOptByCTSAndSkill(ReviveOnce, Bishop.HEXA_HEAVENS_DOOR) != null) {
                Bishop.reviveByHeavensDoor(chr);
            } else if (JobConstants.isDarkKnight(chr.getJob()) && chr.getJobHandler() instanceof DarkKnight darkKnight
                    && chr.hasSkill(DarkKnight.FINAL_PACT_INFO)) {
                darkKnight.reviveByFinalPact(chr);
            } else if (tsm.getOptByCTSAndSkill(ReviveOnce, NightWalker.DARKNESS_ASCENDING) != null) {
                NightWalker.reviveByDarknessAscending(chr);
            } else if (tsm.getOptByCTSAndSkill(FlareTrick, BlazeWizard.PHOENIX_RUN) != null) {
                BlazeWizard.reviveByPhoenixRun(chr);
            } else if (tsm.getOptByCTSAndSkill(ReviveOnce, Zero.REWIND) != null) {
                Zero.reviveByRewind(chr);
            } else if (tsm.getOptByCTSAndSkill(ReviveOnce, Phantom.FINAL_FEINT) != null) {
                Phantom.reviveByFinalFeint(chr);
            } else if (!hasAndInsideLotusFlower(chr)) {
                for (Summon summon : chr.getField().getSummonsByChar(chr)) {
                    chr.getField().removeSummon(summon.getSkillID(), summon.getSlv());
                    tsm.removeStatsBySkill(summon.getSkillID());
                }
                int antiExpLostR = 0;
                if (chr.getGuild() != null) {
                    GuildSkill guildSkill = chr.getGuild().getSkillById(GuildConstants.FEARLESS);
                    SkillInfo skillInfo = SkillData.getSkillInfoById(GuildConstants.FEARLESS);
                    if (guildSkill != null && skillInfo != null) {
                        antiExpLostR = skillInfo.getValue(SkillStat.expLossReduceR, guildSkill.getLevel());
                    }
                }
                if (chr.hasItem(5130000)) {
                    chr.getScriptManager().startScript(chr, 0, "cash_5130000", ScriptType.Item);
                } else {
                    long exp = chr.getAvatarData().getCharacterStat().getExp();
                    long expLost = exp * (10 - antiExpLostR) / 100; //10 is 10% exp lost when death.
                    long newExp = exp - expLost;
                    if (newExp < 0) {
                        newExp = 0;
                    }
                    chr.getAvatarData().getCharacterStat().setExp(newExp);
                    stats.put(Stat.exp, newExp);
                }
                chr.sendStatsPacket(stats);
                chr.openUIOnDead();
            }
        } else {
            if ((invincibleBeliefTimer == null || invincibleBeliefTimer.isDone()) && !chr.getField().isTown()) {
                if (chr.hasSkill(INVICIBLE_BELIEF_HERO) // Invincible Belief (Hero)
                        || chr.hasSkill(INVICIBLE_BELIEF_PALADIN) // Invincible Belief (Paladin)
                        || chr.hasSkill(INVICIBLE_BELIEF_DARK_KNIGHT)  // Invincible Belief (Dark Knight)
                        || chr.hasSkill(INVICIBLE_BELIEF_LINK)) {
                    int invincibleBeliefID = 0;
                    int SLV = chr.getSkillLevel(INVICIBLE_BELIEF_LINK);
                    if (chr.hasSkill(INVICIBLE_BELIEF_HERO) || chr.hasSkill(INVICIBLE_BELIEF_LINK)) {
                        invincibleBeliefID = chr.hasSkill(INVICIBLE_BELIEF_HERO) ? INVICIBLE_BELIEF_HERO : INVICIBLE_BELIEF_LINK;
                        if (!chr.hasSkill(INVICIBLE_BELIEF_LINK)) {
                            SLV = chr.getSkillLevel(INVICIBLE_BELIEF_HERO);
                        }
                    } else if (chr.hasSkill(INVICIBLE_BELIEF_PALADIN) || chr.hasSkill(INVICIBLE_BELIEF_LINK)) {
                        invincibleBeliefID = chr.hasSkill(INVICIBLE_BELIEF_PALADIN) ? INVICIBLE_BELIEF_PALADIN : INVICIBLE_BELIEF_LINK;
                        if (!chr.hasSkill(INVICIBLE_BELIEF_LINK)) {
                            SLV = chr.getSkillLevel(INVICIBLE_BELIEF_PALADIN);
                        }
                    } else if (chr.hasSkill(INVICIBLE_BELIEF_DARK_KNIGHT) || chr.hasSkill(INVICIBLE_BELIEF_LINK)) {
                        invincibleBeliefID = chr.hasSkill(INVICIBLE_BELIEF_DARK_KNIGHT) ? INVICIBLE_BELIEF_DARK_KNIGHT : INVICIBLE_BELIEF_LINK;
                        if (!chr.hasSkill(INVICIBLE_BELIEF_LINK)) {
                            SLV = chr.getSkillLevel(INVICIBLE_BELIEF_DARK_KNIGHT);
                        }
                    }
                    if (!chr.hasSkillOnCooldown(invincibleBeliefID)) {
                        SkillInfo skillInfo = SkillData.getSkillInfoById(invincibleBeliefID);
                        if (((double) curHP / chr.getMaxHP() * 100) <= skillInfo.getValue(x, SLV)) {
                            int finalSLV = SLV;
                            this.invincibleBeliefTimer = chr.getTimer().addFixedRateEvent(() -> chr.heal(skillInfo.getValue(y, finalSLV) * chr.getMaxHP() / 100), 0L, 1000L, skillInfo.getValue(time, SLV));
                            chr.addSkillCooldown(invincibleBeliefID, skillInfo.getValue(cooltime, SLV));
                        }
                    }
                }
            }
        }
    }

    private boolean hasAndInsideLotusFlower(Char chr) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        int skillID = LOTUS_FLOWER;
        if (chr.hasSkill(skillID) && tsm.hasStatBySkillId(skillID) && !lotusFlowerTrigged) {
            var lotusFlower = chr.getField().getSummonBySkillId(chr, skillID);
            var pos = lotusFlower.getPosition();
            SkillInfo si = SkillData.getSkillInfoById(skillID);
            int slv = chr.getSkillLevel(skillID);
            var rect = pos.getRectAround(si.getFirstRect());
            if (rect.hasPositionInside(chr.getPosition())) {
                Option o = new Option();
                o.nValue = 1;
                o.nReason = skillID;
                o.tTerm = si.getValue(x, slv);
                o.setInMillis(true);
                tsm.sendStat(IndieNotDamaged, o);
                // Invincible for 3.5 sec. upon revival.
                chr.healHPMP();
                chr.addSkillCooldown(skillID, si.getValue(w, slv));
                lotusFlowerTrigged = true;
                chr.getField().broadcast(Summoned.lotusFlower(lotusFlower));
                return true;
            }
        }
        return false;
    }

    /**
     * Handles the 'middle' part of hit processing, namely the job-specific
     * stuff like Magic Guard, and puts this info in <code>hitInfo</code>.
     *
     * @param c        The client
     * @param inPacket packet to be processed
     * @param hitInfo  The hit info that should be altered if necessary
     */
    public void handleHit(Client c, InPacket inPacket, HitInfo hitInfo) {
        Char chr = c.getChr();
        boolean phantom282 = net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3
                && chr.getJobHandler() instanceof Phantom;
        // Stolen job effects run before this method; the common hit rules run once on Phantom itself.
        if (phantom282 && chr.getJobHandler() != this) {
            return;
        }
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o1 = new Option();

        // If no job specific skills already nullified the dmg taken
        if (hitInfo.hpDamage > 0) {

            // Mages - Ethereal Form
            if (chr.hasSkill(ETHEREAL_FORM) && tsm.hasStat(EtherealForm)) {
                int mpDmg = tsm.getOption(EtherealForm).nOption;
                int hpDmg = tsm.getOption(EtherealForm).yOption;
                int remainingMP = chr.getMP() - mpDmg;
                if (chr.getMP() > 0) {
                    hitInfo.mpDamage = remainingMP < 0 ? chr.getMP() : mpDmg;
                    hitInfo.hpDamage = remainingMP < 0 ? (hpDmg - chr.getMP() > 0 ? hpDmg - chr.getMP() : 0) : 0;
                } else {
                    hitInfo.hpDamage = hpDmg;
                }
            }

            // Wind Archer - Gale Barrier
            if (chr.hasSkill(WindArcher.GALE_BARRIER) && tsm.hasStat(WindBreakerStormGuard)
                    && hitInfo.hpDamage > 0 && chr.getJobHandler() instanceof WindArcher windArcher) {
                double hpDmgR = (((double) hitInfo.hpDamage) / chr.getMaxHP()) * 100;
                windArcher.diminishGaleBarrier((int) hpDmgR);
                hitInfo.hpDamage = 0;
            }

            // General - Damage Reduce
            long totalDmgReduceR = chr.getTotalStat(BaseStat.dmgReduce);
            if (totalDmgReduceR > 0) {
                hitInfo.hpDamage -= (int) ((hitInfo.hpDamage * totalDmgReduceR / 100D) > hitInfo.hpDamage ? hitInfo.hpDamage : (hitInfo.hpDamage * totalDmgReduceR / 100D));
            }

            // Mihile - Shield Of Light
            else if (tsm.hasStat(Michael_RhoAias)) {
                // Hack Checks for Party members being in the shield rect.
                Char mihileChr = chr.getField().getCharByID(tsm.getOption(Michael_RhoAias).xOption);
                if (mihileChr != null && mihileChr.getJobHandler() instanceof Mihile mihile) {
                    mihile.hitShieldOfLight();
                }
            }

            // Warrior V - Impenetrable Skin
            if (tsm.hasStatBySkillId(IMPENETRABLE_SKIN) && chr.hasSkill(IMPENETRABLE_SKIN)) {
                EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
                Skill skill = chr.getSkill(IMPENETRABLE_SKIN);
                SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
                int slv = skill.getCurrentLevel();
                int count = tsm.getOption(HitStackDamR).xOption;
                if (count < si.getValue(y, slv)) {
                    count++;
                    o1.nOption = 1;
                    o1.rOption = skill.getSkillId();
                    o1.tOption = (int) (tsm.getRemainingTime(HitStackDamR, IMPENETRABLE_SKIN));
                    o1.xOption = count;
                    o1.setInMillis(true);
                    tsm.sendStat(HitStackDamR, o1);
                }
            }

            // Mihile - Soul Link
            else if (tsm.hasStat(MichaelSoulLink) && chr.getId() != tsm.getOption(MichaelSoulLink).cOption) {
                Party party = chr.getParty();

                PartyMember mihileInParty = party.getPartyMemberByID(tsm.getOption(MichaelSoulLink).cOption);
                if (mihileInParty != null) {
                    Char mihileChr = mihileInParty.getChr();
                    if (mihileChr != null) {
                        Skill skill = mihileChr.getSkill(Mihile.SOUL_LINK);
                        SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
                        int slv = skill.getCurrentLevel();

                        int hpDmg = hitInfo.hpDamage;
                        int mihileDmgTaken = (int) (hpDmg * ((double) si.getValue(q, slv) / 100));

                        hitInfo.hpDamage = hitInfo.hpDamage - mihileDmgTaken;
                        mihileChr.damage(mihileDmgTaken);
                    }
                } else {
                    tsm.removeStatsBySkill(Mihile.SOUL_LINK);
                    tsm.removeStatsBySkill(Mihile.ROYAL_GUARD);
                    tsm.removeStatsBySkill(Mihile.ENDURING_SPIRIT);
                }
            }

            // Paladin - Parashock Guard
            else if (tsm.hasStat(KnightsAura) && chr.getId() != tsm.getOption(KnightsAura).nOption) {
                Party party = chr.getParty();

                PartyMember paladinInParty = party.getPartyMemberByID(tsm.getOption(KnightsAura).nOption);
                if (paladinInParty != null) {
                    Char paladinChr = paladinInParty.getChr();
                    if (paladinChr != null) {
                        Skill skill = paladinChr.getSkill(Paladin.PARASHOCK_GUARD);
                        SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
                        int slv = skill.getCurrentLevel();

                        int dmgReductionR = si.getValue(y, slv);
                        int dmgReduceAmount = (int) (hitInfo.hpDamage * ((double) dmgReductionR / 100));
                        hitInfo.hpDamage = hitInfo.hpDamage - dmgReduceAmount;
                    }
                }
            }

            // Mechanic - Multipurpose Bot M-FL
            else if (chr.getField().getSummonBySkillId(chr, MULTIPURPOSE_BOT_MFL) != null) {
                if (hitInfo.hpDamage >= (chr.getMaxHP() * 0.9D)) {
                    hitInfo.hpDamage = 0;
                    chr.write(UserPacket.effect(Effect.skillSpecial(MULTIPURPOSE_BOT_MFL)));
                    chr.getField().broadcast(UserRemote.effect(chr.getId(), Effect.skillSpecial(MULTIPURPOSE_BOT_MFL)), chr);
                }
            }

            // Magic Guard
            int magicGuardPercent = chr.getTotalStat(BaseStat.magicGuard);
            if (phantom282 && tsm.hasStat(MagicGuard)) {
                magicGuardPercent = Math.max(magicGuardPercent, tsm.getOption(MagicGuard).nOption);
            }
            if (magicGuardPercent > 0) {
                int dmgPerc = magicGuardPercent;
                int dmg = hitInfo.hpDamage;
                int mpDmg = (int) (dmg * (dmgPerc / 100D));
                mpDmg = chr.getStat(Stat.mp) - mpDmg < 0 ? chr.getStat(Stat.mp) : mpDmg;
                hitInfo.hpDamage = dmg - mpDmg;
                hitInfo.mpDamage = mpDmg;
            }

            if ((chr.hasSkill(GROUNDED_BODY) || chr.hasSkill(GROUNDED_BODY_LINK)) && tsm.hasStat(AMLinkSkill)) {
                int hpDec = tsm.getOption(AMLinkSkill).nOption;
                hitInfo.hpDamage -= hitInfo.hpDamage * hpDec / 100;
            }
        }
    }

    public void handleHitDebuff(Client c, InPacket inPacket, HitInfo hitInfo) {
        Char chr = c.getChr();
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o = new Option();
        if (hitInfo.hpDamage != 0) {
            switch (hitInfo.templateID) {
                case 8880100:
                case 8880110:
                    if (hitInfo.type == 2 && !tsm.hasStat(Lapidification)) {
                        o.nOption = 10;
                        o.rOption = MobSkillID.Lapidification.getVal();
                        o.slv = 14;
                        o.tOption = 8;
                        tsm.sendSetStatFromMobSkillPacket(Lapidification, o);
                        chr.write(UserPacket.effect(Effect.mobSkillHit(o.rOption, o.slv)));
                        chr.getField().broadcast(UserRemote.effect(chr.getId(), Effect.mobSkillHit(o.rOption, o.slv)), chr);
                    }
                    break;
                case 8880101:
                case 8880111:
                    if (hitInfo.type == 3 && !tsm.hasStat(Lapidification)) {
                        o.nOption = 10;
                        o.rOption = MobSkillID.Lapidification.getVal();
                        o.slv = 14;
                        o.tOption = 8;
                        tsm.sendSetStatFromMobSkillPacket(Lapidification, o);
                        chr.write(UserPacket.effect(Effect.mobSkillHit(o.rOption, o.slv)));
                        chr.getField().broadcast(UserRemote.effect(chr.getId(), Effect.mobSkillHit(o.rOption, o.slv)), chr);
                    }
                    break;
                case 9309201:
                    if (hitInfo.type == 0 && !tsm.hasStat(Poison)) {
                        o.nOption = 100;
                        o.rOption = MobSkillID.Poison.getVal();
                        o.slv = 32;
                        o.tOption = 5;
                        tsm.sendSetStatFromMobSkillPacket(Poison, o);
                        chr.write(UserPacket.effect(Effect.mobSkillHit(o.rOption, o.slv)));
                        chr.getField().broadcast(UserRemote.effect(chr.getId(), Effect.mobSkillHit(o.rOption, o.slv)), chr);
                    }
                    break;
            }
            CrimsonQueen.applyHitDebuff(chr, hitInfo.templateID, tsm, hitInfo.blockSkillId, hitInfo.type);
            Pierre.applyHitDebuff(chr, hitInfo.templateID, tsm, hitInfo.blockSkillId, hitInfo.type);
        }
    }

    private void reviveByCloseCall(Char chr, int beforeHitHp) {
        int skillID = CLOSE_CALLS_LINK;
        if (chr.hasSkill(CLOSE_CALLS)) {
            skillID = CLOSE_CALLS;
        }
        if (Util.succeedProp(chr.getSkillLevel(skillID) * 5)) {
            chr.heal(beforeHitHp, true);
            chr.chatMessage(ChatType.Tip, "你通過技能 " + StringData.getSkillStringById(skillID).getName() + " 避免了一次危險攻擊。");
            chr.write(UserPacket.effect(Effect.skillUse(skillID, chr.getLevel(), chr.getSkillLevel(skillID))));
            chr.getField().broadcast(UserRemote.effect(chr.getId(), Effect.skillUse(skillID, chr.getLevel(), chr.getSkillLevel(skillID))), chr);
        } else {
            chr.openUIOnDead();
        }
    }

    public abstract boolean isHandlerOfJob(short id);

    public int getFinalAttackSkill(int faSkill) {
        return 0;
    }

    public int getFinalAttackProc(int faSkill) {
        return SkillData.getSkillInfoById(faSkill) != null ? SkillData.getSkillInfoById(faSkill).getValue(prop, chr.getSkillLevel(faSkill)) : 0;
    }

    /**
     * Called when a player is right-clicking a buff, requesting for it to be disabled.
     */
    public void handleSkillRemove(Char chr, int skillID) {

    }

    public void handleAddCTS(CharacterTemporaryStat cts, List<Option> options) {
        for (Option addOpt : options) {
            if (cts == CombatOrders) {
                chr.setCombatOrders(addOpt.nOption);
            }
        }
    }

    public void handleRemoveCTS(CharacterTemporaryStat cts, List<Option> options) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        for (Option removeOpt : options) {
            if (removeOpt == null) {
                continue;
            }
            int skillId = cts.isIndie() ? removeOpt.nReason : removeOpt.rOption;
            if (Arrays.asList(isAuraCTS).contains(cts)) {
                AffectedArea aura = chr.getField().getAffectedAreas().stream().filter(
                        aa -> aa.getCharID() == chr.getId()
                                && aa.getSkillID() == skillId).findFirst().orElse(null);
                if (aura != null && aura.getCharID() == chr.getId()) {
                    chr.getField().removeLife(aura);
                }
            }
            if (cts == CombatOrders) {
                chr.setCombatOrders(0);
            } else if (cts == PairingUser) {
                for (var mimicSkillId : new int[] {
                        Paladin.WEAPON_BOOSTER,
                        Paladin.DIVINE_BLESSING,
                        Paladin.SACROSANCTITY,
                        Paladin.HP_RECOVERY}) {
                    tsm.removeStatsBySkill(mimicSkillId);
                }
            }  else if (cts == Dice) {
                chr.removeBaseStatByOption();
            } else if (cts == FifthAdvWarriorShield) {
                chr.write(UserLocal.userBonusAttackRequest(Warrior.BLITZ_SHIELD_ATTACK));
            } else if (cts == CrystalChargeBuffIcon && chr.getJobHandler() instanceof Illium illium) {
                illium.changeCrystalCharge(0);
                illium.resetCrystalBattery();
            } else if (cts == FreudBlessing && removeOpt.nOption != 6) {
                SkillInfo si = SkillData.getSkillInfoById(FREUDS_WISDOM);
                int slv = chr.getSkillLevel(FREUDS_WISDOM);
                chr.addSkillCoolTime(FREUDS_WISDOM, Util.getCurrentTimeLong() + (si.getValue(y, slv) * 1000L));
            } else if (cts == ConvertAD && removeOpt.nOption > 0) {
                chr.getTimer().addEvent(this::setOverdriveCooldown, 50, TimeUnit.MILLISECONDS);
            } else if (cts == GuidedArrow) {
                chr.removeForceAtomByKey(removeOpt.xOption);
            } else if (cts == LPBattleMode) {
                this.solusStack = 0;
            } else if (cts == NoviceMagicianLink) {
                this.empiricalKnowledgeStack = 0;
            } else if (cts == Frenzy) {
                for (AffectedArea aa : chr.getField().getAffectedAreas()) {
                    if (aa.getCharID() == chr.getId()) {
                        chr.getField().removeLife(aa);
                    }
                }
            }
        }
    }

    public void handleLevelUp(short level) {
        if (net.swordie.ms.client.character.tms282.Tms282JobAdvance.skipBase()) {
            return; // 282: Tms282LevelService already granted AP / SP / HP; only the job overrides' advancement runs
        }
        if (JobConstants.isSpecialEventJob(chr.getJob())) {
            // Event jobs learn explicit fixed-level skills; generic SP, max-all and V rewards do not apply.
            Map<Stat, Object> eventStats = new HashMap<>();
            if (level > 10) {
                chr.addStat(Stat.ap, 5);
                eventStats.put(Stat.ap, (short) chr.getStat(Stat.ap));
            }
            int[] gain = GameConstants.getHpMpPerLevel(chr.getJob());
            chr.addStatAndSendPacket(Stat.mhp, gain[0]);
            if (!JobConstants.isNoManaJob(chr.getJob())) chr.addStatAndSendPacket(Stat.mmp, gain[1]);
            chr.sendStatsPacket(eventStats);
            chr.heal(chr.getMaxHP());
            chr.healMP(chr.getMaxMP());
            net.swordie.ms.client.jobs.special.SpecialJobProgression.learnAvailableSkills(chr, true);
            autoAP();
            return;
        }
        Map<Stat, Object> stats = new HashMap<>();
        if (level > 10) {
            chr.addStat(Stat.ap, 5);
            stats.put(Stat.ap, (short) chr.getStat(Stat.ap));
        }
        if (level >= 50) {
            chr.addHonorExp(700 + ((chr.getLevel() - 50) / 10) * 100);
        }
        int sp = 0;
        short job = chr.getJob();
        if (level == 140) {
            for (Skill skill : SkillData.getSkillsByJob(job)) {
                int skillID = skill.getSkillId();
                Skill currentSkill = chr.getSkill(skillID);
                if (currentSkill != null
                        && currentSkill.getRootId() == chr.getJob()
                        && !SkillConstants.isHyperSkill(job, skillID)
                        && !SkillConstants.isHyperStat(skillID)) {
                    sp += skill.getMaxLevel() - currentSkill.getCurrentLevel();
                }
            }
            List<Skill> updateSkills = new ArrayList<>();
            for (Skill skill : chr.getSkills()) {
                if (job - skill.getRootId() >= 1
                        && SkillConstants.isMatching(skill.getRootId(), job)
                        && skill.getCurrentLevel() != skill.getMaxLevel()
                        && !JobConstants.isBeastTamer(job)
                        && !JobConstants.isZero(job)) {
                    skill.setCurrentLevel(skill.getMaxLevel());
                    chr.addSkill(skill);
                    updateSkills.add(skill);
                }
            }
            chr.write(WvsContext.changeSkillRecordResult(updateSkills, true, false, false));
            chr.initHyperStats();
        } else {
            sp = SkillConstants.getBaseSpByLevel(level);
            if ((level % 10) % 3 == 0 && level > 100) {
                sp *= 2; // double sp on levels ending in 3/6/9
            }
        }
        if (!JobConstants.isBeastTamer(job) && !JobConstants.isZero(job)) {
            chr.addSpToJobByCurrentLevel(sp);
            stats.put(Stat.sp, chr.getAvatarData().getCharacterStat().getExtendSP());
        }

        // Link Skills:
        byte linkSkillLevel = JobConstants.isZero(job) ? (byte) chr.getSkillLevel(Zero.RHINNES_BLESSING_BOOST) : (byte) SkillConstants.getLinkSkillLevelByCharLevel(level, job);
        int linkSkillID = SkillConstants.getLinkSkillByJob(job);
        int originalOfLinkSkillID = SkillConstants.getOriginalOfLinkedSkill(linkSkillID);
        if (originalOfLinkSkillID != 0 && linkSkillLevel > 0) {
            Skill skill = chr.getSkill(originalOfLinkSkillID, true);
            if (skill.getCurrentLevel() != linkSkillLevel) {
                if (!chr.hasSkill(originalOfLinkSkillID)) {
                    skill.setCurrentLevel(1);
                    chr.addSkill(skill);
                } else {
                    skill.setCurrentLevel(linkSkillLevel);
                }
                chr.write(WvsContext.changeSkillRecordResult(skill));
            }
        }

        int[] hpMpToAdd = GameConstants.getHpMpPerLevel(chr.getJob());
        int hp = hpMpToAdd[0];
        int mp = hpMpToAdd[1];
        chr.addStatAndSendPacket(Stat.mhp, hp);
        if (!JobConstants.isNoManaJob(chr.getJob())) {
            chr.addStatAndSendPacket(Stat.mmp, mp);
        }

        chr.sendStatsPacket(stats);
        chr.heal(chr.getMaxHP());
        chr.healMP(chr.getMaxMP());
        Account acc = chr.getAccount();
        switch (level) {
            case 10 -> {
                String message = "#b[指南] 一轉轉職#k\r\n\r\n";
                message += "你已達到 10 級，可以進行 #b[一轉轉職]#k！\r\n\r\n";
                message += "完成任務 #r[轉職]#k 以解鎖你的一轉職業！\r\n";
                chr.write(UserLocal.addPopupSay(9010000, 6000, message, "FarmSE.img/boxResult"));
            }
            case 20 -> {
                String message;
                if (job == JobConstants.JobEnum.THIEF.getJobId() && chr.getSubJob() == 1) {
                    message = "#b[指南] 1.5 轉轉職#k\r\n\r\n";
                    message += "你已達到 20 級，可以進行 #b[1.5 轉轉職]#k！\r\n\r\n";
                    message += "完成任務 #r[轉職]#k 以解鎖你的 1.5 轉職業！\r\n";
                    chr.write(UserLocal.addPopupSay(9010000, 6000, message, "FarmSE.img/boxResult"));
                }
                message = "#b[指南] 升級系統#k\r\n\r\n";
                message += "你已達到 20 級，現在可以使用 #b[卷軸強化]#k！\r\n\r\n";
                message += "請從任務圖標中接受任務 #b你瞭解卷軸強化嗎？#k！\r\n";
                chr.write(UserLocal.addPopupSay(9010000, 6000, message, "FarmSE.img/boxResult"));
            }
            case 30 -> {
                chr.sendLevelRewardToChar(30);
                String message = "#b[指南] 二轉轉職#k\r\n\r\n";
                message += "你已達到 30 級，可以進行 #b[二轉轉職]#k！\r\n\r\n";
                message += "完成任務 #r[轉職]#k 以解鎖你的二轉職業！\r\n";
                chr.write(UserLocal.addPopupSay(9010000, 6000, message, "FarmSE.img/boxResult"));

                message = "#b[指南] 能力值#k\r\n\r\n";
                message += "你已達到 30 級，現在可以解鎖 #b[內在能力]#k！\r\n\r\n";
                message += "請從任務圖示接受 #b內在能力#k 的開啟任務！\r\n";
                chr.write(UserLocal.addPopupSay(9010000, 6000, message, "FarmSE.img/boxResult"));
            }
            case 31 -> {
                String message = "#b[指南] 性向#k\r\n\r\n";
                message += "從 30 級開始，你可以解鎖 #b[性向]#k！\r\n\r\n";
                message += "打開 #b性向視窗（預設快捷鍵：B）#k，查看你的 #b[性向]#k！\r\n";
                chr.write(UserLocal.addPopupSay(9010000, 6000, message, "FarmSE.img/boxResult"));
            }
            case 60 -> {
                chr.sendLevelRewardToChar(60);
            }
            case 70 -> {
                if (linkSkillID != 0 && linkSkillLevel > 0) {
                    acc.addLinkSkill(chr, linkSkillID, FileTime.MIN_TIME());
                    acc.getLinkSkills().forEach(LinkSkill::updateLinkSkillToSQL);
                }
            }
            case 100 -> {
                String message = "#b[指南] 四轉轉職#k\r\n\r\n";
                message += "你已達到 100 級，可以進行 #b[四轉轉職]#k！\r\n\r\n";
                message += "完成任務 #r[轉職]#k 以解鎖你的四轉職業！\r\n";
                chr.write(UserLocal.addPopupSay(9010000, 6000, message, "FarmSE.img/boxResult"));
            }
            case 110 -> { // Link skills can now be tradeable through character in the account;
                if (JobConstants.isZero(job) && linkSkillID != 0 && linkSkillLevel > 0) {
                    acc.addLinkSkill(chr, linkSkillID, FileTime.MIN_TIME());
                    acc.getLinkSkills().forEach(LinkSkill::updateLinkSkillToSQL);
                }
            }
            case 120 -> {
                int id = SkillConstants.getLinkSkillByJob(job);
                if (id != 0 && linkSkillLevel > 1) {
                    LinkSkill linkSkill = acc.getLinkSkill(chr.getId(), id);
                    if (linkSkill != null) {
                        linkSkill.setLevel(SkillConstants.getLinkSkillLevelByCharLevel(level, job));
                    }
                    acc.getLinkSkills().forEach(LinkSkill::updateLinkSkillToSQL);
                }
            }
            case 130 -> {
                if (JobConstants.isZero(job)) {
                    String message = "#b[神之子] 神之子連結技能#k\r\n\r\n";
                    message += "#b[時之祝福]#k 提升了 1 級，帳號的連結技能 #b[時之祝福]#k 也提升了 1 級！\r\n";
                    chr.write(UserLocal.addPopupSay(9010000, 6000, message, "FarmSE.img/boxResult"));
                    int id = SkillConstants.getLinkSkillByJob(job);
                    LinkSkill linkSkill = acc.getLinkSkillBySkillID(id);
                    if (linkSkill != null & linkSkill.getLevel() < 2) {
                        linkSkill.setLevel(2);
                    }
                    acc.getLinkSkills().forEach(LinkSkill::updateLinkSkillToSQL);
                }
            }
            case 140 -> {
                String message = "#b[指南] 極限屬性#k\r\n\r\n";
                message += "你已達到 140 級，解鎖了 #b[極限屬性]#k 的力量！\r\n\r\n";
                message += "打開 #b角色能力值視窗（預設快捷鍵：S）#k，試試你的 #b[極限屬性]#k！\r\n";
                chr.write(UserLocal.addPopupSay(9010000, 6000, message, "FarmSE.img/boxResult"));
                String message2 = "#b[指南] 超技能#k\r\n\r\n";
                message2 += "你已達到 140 級，解鎖了 #b[超技能]#k 的力量！\r\n\r\n";
                message2 += "打開 #b技能視窗（預設快捷鍵：K）#k，試試你的 #b[超技能]#k！\r\n";
                chr.write(UserLocal.addPopupSay(9010000, 6000, message2, "FarmSE.img/boxResult"));
                if (JobConstants.isZero(job)) {
                    message = "#b[神之子] 神之子連結技能#k\r\n\r\n";
                    message += "#b[時之祝福]#k 提升了 1 級，帳號的連結技能 #b[時之祝福]#k 也提升了 1 級！\r\n";
                    chr.write(UserLocal.addPopupSay(9010000, 6000, message, "FarmSE.img/boxResult"));
                    int id = SkillConstants.getLinkSkillByJob(job);
                    LinkSkill linkSkill = acc.getLinkSkillBySkillID(id);
                    if (linkSkill != null && linkSkill.getLevel() < 3) {
                        linkSkill.setLevel(3);
                    }
                    acc.getLinkSkills().forEach(LinkSkill::updateLinkSkillToSQL);
                }
                chr.sendLevelRewardToChar(140);
            }
            case 150 -> {
                if (JobConstants.isZero(job)) {
                    String message = "#b[神之子] 神之子連結技能#k\r\n\r\n";
                    message += "#b[時之祝福]#k 提升了 1 級，帳號的連結技能 #b[時之祝福]#k 也提升了 1 級！\r\n";
                    chr.write(UserLocal.addPopupSay(9010000, 6000, message, "FarmSE.img/boxResult"));
                    int id = SkillConstants.getLinkSkillByJob(job);
                    LinkSkill linkSkill = acc.getLinkSkill(chr.getId(), id);
                    if (linkSkill != null && linkSkill.getLevel() < 4) {
                        linkSkill.setLevel(4);
                    }
                    acc.getLinkSkills().forEach(LinkSkill::updateLinkSkillToSQL);
                }
            }
            case 160 -> {
                if (JobConstants.isZero(job)) {
                    String message = "#b[神之子] 神之子連結技能#k\r\n\r\n";
                    message += "#b[時之祝福]#k 提升了 1 級，帳號的連結技能 #b[時之祝福]#k 也提升了 1 級！\r\n";
                    chr.write(UserLocal.addPopupSay(9010000, 6000, message, "FarmSE.img/boxResult"));
                    int id = SkillConstants.getLinkSkillByJob(job);
                    LinkSkill linkSkill = acc.getLinkSkill(chr.getId(), id);
                    if (linkSkill != null && linkSkill.getLevel() < 5) {
                        linkSkill.setLevel(5);
                    }
                    acc.getLinkSkills().forEach(LinkSkill::updateLinkSkillToSQL);
                }
            }
            case 200 -> {
                chr.sendLevelRewardToChar(200);
                String message = "#b[指南] 五轉轉職#k\r\n\r\n";
                message += "你已達到 200 級，可以進行 #b[五轉轉職]#k！\r\n\r\n";
                message += "完成任務 #r[轉職] 五轉：艾爾達的呼喚#k 以解鎖你的五轉職業！\r\n";
                chr.write(UserLocal.addPopupSay(9010000, 6000, message, "FarmSE.img/boxResult"));
                if (chr.getMatrixSlot().size() < MatrixConstants.MAX_NODE_SLOTS) {
                    for (int i = chr.getMatrixSlot().size(); i < MatrixConstants.MAX_NODE_SLOTS; i++) {
                        chr.getMatrixSlot().add(new MatrixSlot(chr.getId(), i));
                    }
                }
                GiveVSkills();
            }
            case 210 -> {
                int id = SkillConstants.getLinkSkillByJob(job);
                if (id != 0 && linkSkillLevel > 2) {
                    LinkSkill linkSkill = acc.getLinkSkill(chr.getId(), id);
                    if (linkSkill != null) {
                        if (!JobConstants.isZero(job)) {
                            linkSkill.setLevel(SkillConstants.getLinkSkillLevelByCharLevel(level, job));
                        }
                    }
                    acc.getLinkSkills().forEach(LinkSkill::updateLinkSkillToSQL);
                }
            }
            case 250 -> chr.sendLevelRewardToChar(250);
            case 275 -> chr.sendLevelRewardToChar(275);
            case 300 -> chr.sendLevelRewardToChar(300);
        }
        if (level == 50 || level == 100 || level == 150 || level == 200 || level == 210 || level == 220 || level == 230 || level == 240 || level == 250 || level == 255 || level == 260 || level == 265 || level == 270 || level == 275 || level == 300) {
            AchievementHandler.handleLevelUp(chr, level);
        }
        autoAP();
    }

    private void autoAP() {
        if (chr.hasQuest(25995)) {
            String string = chr.getQRValueByKey(25995, "instantap");
            if (string == null || string.equals("0")) {
                return;
            }
            short job = chr.getJob();
            int remaining = chr.getStat(Stat.ap);
            if (JobConstants.isDemonAvenger(job)) {
                int hp  = chr.getStat(Stat.mhp);
                int str = chr.getStat(Stat.str);
                int apToHp  = addUpToCap(hp, remaining, GameConstants.MAX_HP_MP);
                if (apToHp > 0) {
                    int addHp = apToHp * 15;
                    hp += addHp;
                    remaining -= apToHp;
                    chr.setStatAndSendPacket(Stat.mhp, hp);
                    chr.chatMessage("自動分配： HP +" + addHp);
                }
                if (remaining > 0) {
                    int addStr = addUpToCap(str, remaining);
                    if (addStr > 0) {
                        str += addStr; remaining -= addStr;
                        chr.setStatAndSendPacket(Stat.str, str);
                        chr.chatMessage("自動分配： STR +" + addStr);
                    }
                }
                chr.setStatAndSendPacket(Stat.ap, remaining);
            } else if (JobConstants.isXenon(job)) {
                int curStr = chr.getStat(Stat.str);
                int curDex = chr.getStat(Stat.dex);
                int curLuk = chr.getStat(Stat.luk);
                if (curStr >= 330 && curDex >= 330 && curLuk >= 330) {
                    Stat[] order = new Stat[] { Stat.str, Stat.dex, Stat.luk };
                    java.util.Arrays.sort(order, (a, b) -> {
                        int va = chr.getStat(a);
                        int vb = chr.getStat(b);
                        if (va != vb) return Integer.compare(vb, va);
                        if (a == Stat.str) return -1;
                        if (b == Stat.str) return 1;
                        if (a == Stat.dex) return -1;
                        if (b == Stat.dex) return 1;
                        return 0;
                    });
                    for (Stat s : order) {
                        if (remaining <= 0) break;
                        int cur = chr.getStat(s);
                        int add = addUpToCap(cur, remaining);
                        if (add <= 0) continue;
                        int nv = cur + add;
                        remaining -= add;
                        chr.setStatAndSendPacket(s, nv);
                        chr.chatMessage("自動分配： " + s.name().toUpperCase() + " +" + add);
                    }
                    chr.setStatAndSendPacket(Stat.ap, remaining);
                }
            } else if (JobConstants.isCadena(job) || JobConstants.isDualBlade(job)) {
                int luk = chr.getStat(Stat.luk);
                int str = chr.getStat(Stat.str);
                int dex = chr.getStat(Stat.dex);
                int addLuk = addUpToCap(luk, remaining);
                if (addLuk > 0) {
                    luk += addLuk; remaining -= addLuk;
                    chr.setStatAndSendPacket(Stat.luk, luk);
                    chr.chatMessage("自動分配： LUK +" + addLuk);
                }
                if (remaining > 0) {
                    Stat first = (dex >= str) ? Stat.dex : Stat.str; // tie -> DEX
                    int firstVal = (first == Stat.dex) ? dex : str;
                    int addFirst = addUpToCap(firstVal, remaining);
                    if (addFirst > 0) {
                        firstVal += addFirst; remaining -= addFirst;
                        chr.setStatAndSendPacket(first, firstVal);
                        chr.chatMessage("自動分配： " + (first == Stat.dex ? "DEX" : "STR") + " +" + addFirst);
                        if (first == Stat.dex) dex = firstVal; else str = firstVal;
                    }
                }
                chr.setStatAndSendPacket(Stat.ap, remaining);
            } else if (JobConstants.isWarriorEquipJob(job)) {
                int str = chr.getStat(Stat.str);
                int dex = chr.getStat(Stat.dex);
                int addStr = addUpToCap(str, remaining);
                if (addStr > 0) {
                    str += addStr; remaining -= addStr;
                    chr.setStatAndSendPacket(Stat.str, str);
                    chr.chatMessage("自動分配： STR +" + addStr);
                }
                if (remaining > 0) {
                    int addDex = addUpToCap(dex, remaining);
                    if (addDex > 0) {
                        dex += addDex; remaining -= addDex;
                        chr.setStatAndSendPacket(Stat.dex, dex);
                        chr.chatMessage("自動分配： DEX +" + addDex);
                    }
                }
                chr.setStatAndSendPacket(Stat.ap, remaining);
            } else if (JobConstants.isMageEquipJob(job)) {
                int inte = chr.getStat(Stat.inte);
                int luk  = chr.getStat(Stat.luk);
                int addInt = addUpToCap(inte, remaining);
                if (addInt > 0) {
                    inte += addInt; remaining -= addInt;
                    chr.setStatAndSendPacket(Stat.inte, inte);
                    chr.chatMessage("自動分配： INT +" + addInt);
                }
                if (remaining > 0) {
                    int addLuk = addUpToCap(luk, remaining);
                    if (addLuk > 0) {
                        luk += addLuk; remaining -= addLuk;
                        chr.setStatAndSendPacket(Stat.luk, luk);
                        chr.chatMessage("自動分配： LUK +" + addLuk);
                    }
                }
                chr.setStatAndSendPacket(Stat.ap, remaining);
            } else if (JobConstants.isArcherEquipJob(job)) {
                int dex = chr.getStat(Stat.dex);
                int str = chr.getStat(Stat.str);
                int addDex = addUpToCap(dex, remaining);
                if (addDex > 0) {
                    dex += addDex; remaining -= addDex;
                    chr.setStatAndSendPacket(Stat.dex, dex);
                    chr.chatMessage("自動分配： DEX +" + addDex);
                }
                if (remaining > 0) {
                    int addStr = addUpToCap(str, remaining);
                    if (addStr > 0) {
                        str += addStr; remaining -= addStr;
                        chr.setStatAndSendPacket(Stat.str, str);
                        chr.chatMessage("自動分配： STR +" + addStr);
                    }
                }
                chr.setStatAndSendPacket(Stat.ap, remaining);
            } else if (JobConstants.isThiefEquipJob(job)) {
                int luk = chr.getStat(Stat.luk);
                int dex = chr.getStat(Stat.dex);
                int addLuk = addUpToCap(luk, remaining);
                if (addLuk > 0) {
                    luk += addLuk; remaining -= addLuk;
                    chr.setStatAndSendPacket(Stat.luk, luk);
                    chr.chatMessage("自動分配： LUK +" + addLuk);
                }
                if (remaining > 0) {
                    int addDex = addUpToCap(dex, remaining);
                    if (addDex > 0) {
                        dex += addDex; remaining -= addDex;
                        chr.setStatAndSendPacket(Stat.dex, dex);
                        chr.chatMessage("自動分配： DEX +" + addDex);
                    }
                }
                chr.setStatAndSendPacket(Stat.ap, remaining);
            } else if (JobConstants.isPirateEquipJob(job)) {
                // Corsair: main DEX, sec STR
                // Buccaneer & Cannoneer: main STR, sec DEX
                final boolean isDexMainStat = JobConstants.isAngelicBuster(job)  || JobConstants.isCorsair(job)
                                                || JobConstants.isMechanic(job) || JobConstants.isMoXuan(job);

                Stat main = isDexMainStat ? Stat.dex : Stat.str;
                Stat sec  = isDexMainStat ? Stat.str : Stat.dex;
                int mainVal = chr.getStat(main);
                int secVal  = chr.getStat(sec);
                int addMain = addUpToCap(mainVal, remaining);
                if (addMain > 0) {
                    mainVal += addMain; remaining -= addMain;
                    chr.setStatAndSendPacket(main, mainVal);
                    chr.chatMessage("自動分配： " + main.name() + " +" + addMain);
                }
                if (remaining > 0) {
                    int addSec = addUpToCap(secVal, remaining);
                    if (addSec > 0) {
                        secVal += addSec; remaining -= addSec;
                        chr.setStatAndSendPacket(sec, secVal);
                        chr.chatMessage("自動分配： " + sec.name() + " +" + addSec);
                    }
                }
                chr.setStatAndSendPacket(Stat.ap, remaining);
            }
        }
    }

    private int addUpToCap(int cur, int add) {
        if (add <= 0) return 0;
        int room = 32767 - cur;
        if (room <= 0) return 0;
        return Math.min(add, room);
    }

    private int addUpToCap(int cur, int add, int cap) {
        if (add <= 0) return 0;
        int room = cap - cur;
        if (room <= 0) return 0;
        return Math.min(add, room);
    }

    public void giveHyperArcaneForceBuff() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o = new Option();
        int value = 0;
        if (chr.hasSkill(HYPER_STAT_ARCANE_FORCE)) {
            int slv = chr.getSkillLevel(HYPER_STAT_ARCANE_FORCE);
            value += slv <= 10 ? slv * 5 : 50 + (slv - 10) * 10;
        }
        if (chr.getGuild() != null) {
            GuildSkill gs = chr.getGuild().getSkillById(GuildConstants.ARCANE_FORCE);
            SkillInfo si = SkillData.getSkillInfoById(GuildConstants.ARCANE_FORCE);
            if (gs != null && si != null) {
                value += si.getValue(arcX, gs.getLevel());
            }
        }
        if (value != 0) {
            tsm.sendStat(IndieArc, o);
        }
    }

    public void setCharCreationStats(Char chr) {
        CharacterStat characterStat = chr.getAvatarData().getCharacterStat();
        characterStat.setLevel(1);
        characterStat.setStr(4);
        characterStat.setDex(4);
        characterStat.setInt(4);
        characterStat.setLuk(4);
        characterStat.setHp(50);
        characterStat.setMaxHp(50);
        if (!JobConstants.isNoManaJob(chr.getJob())) {
            characterStat.setMp(5);
            characterStat.setMaxMp(5);
        }
    }

    private void addItemToNewCharacter(Char chr, int itemId, int quantity, InvType invType) {
        Item item = ItemData.getItemDeepCopy(itemId);
        if (item == null) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, String.format("Unable to find new character item %d", itemId));
            return;
        }
        item.setQuantity(quantity);
        item.setInvType(invType);
        chr.addItemToInventoryToNewCharacter(item);
        item.setInventoryID(chr.getInventoryByType(invType).getId());
        item.setCharID(chr.getId());
        item.saveToSQL();
    }

    private void addItemToNewCharacter(Char chr, int itemId, int quantity) {
        addItemToNewCharacter(chr, itemId, quantity, CONSUME);
    }

    public static void giveSecondaryWeaponToNewCharacter(Char chr, int itemId) {
        Item secondary = ItemData.getItemDeepCopy(itemId);
        if (secondary == null) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, String.format("Unable to find new character secondary weapon %d", itemId));
            return;
        }
        chr.addItemToInventoryToNewCharacter(EQUIPPED, secondary, true);
        secondary.setInventoryID(chr.getInventoryByType(EQUIPPED).getId());
        secondary.setCharID(chr.getId());
        secondary.setInvType(EQUIPPED);
        secondary.setBagIndex(BodyPart.Shield.getVal());
        secondary.saveToSQL();
        chr.getAvatarData().getAvatarLook().getHairEquips().add(secondary.getItemId());
        chr.getAvatarData().getAvatarLook().updateAvatarLookToSQL();
    }

    public void addItemToNewCharacter(Char chr) {
        //Get AvatarLook Item and Add item to "Equipped" Inventory
        try {
            for (int i : chr.getAvatarData().getAvatarLook().getHairEquips()) {
                Equip equip = ItemData.getEquipDeepCopyFromID(i, false);
                if (equip != null && equip.getItemId() >= 1000000) {
                    equip.setBagIndex(ItemConstants.getBodyPartFromItem(equip.getItemId(), chr.getAvatarData().getAvatarLook().getGender()));
                    chr.addItemToInventoryToNewCharacter(EQUIPPED, equip, true);
                    equip.setInventoryID(chr.getInventoryByType(EQUIPPED).getId());
                    equip.setCharID(chr.getId());
                    equip.saveToSQL();
                }
            }
            // TMS 282 has no monster-book body slot (79E970: prefix 117 has none; equips README WI-2)
            if (net.swordie.ms.connection.protocol.Protocol.active().version()
                    != net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) {
                Equip codex = ItemData.getEquipDeepCopyFromID(1172000, false);
                chr.addItemToInventoryToNewCharacter(EQUIPPED, codex, true);
                codex.setInventoryID(chr.getInventoryByType(EQUIPPED).getId());
                codex.setCharID(chr.getId());
                codex.setInvType(EQUIPPED);
                codex.setBagIndex(BodyPart.MonsterBook.getVal());
                codex.saveToSQL();
            }

            addItemToNewCharacter(chr, NEW_CHARACTER_WHITE_POTION, 100);
            addItemToNewCharacter(chr, NEW_CHARACTER_MANA_POTION, 100);
            addItemToNewCharacter(chr, NEW_CHARACTER_PERMANENT_POWER_ELIXIR, 1);
            addItemToNewCharacter(chr, NEW_CHARACTER_MENU_CONSUMABLE, 1);
            addItemToNewCharacter(chr, NEW_CHARACTER_EQUIPMENT_GIFT_BOX, 1);
        } catch (Exception e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
        }
    }

    public void givePetPassiveBuffs(boolean activated) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
        if (!activated) {
            if (!tsm.getPetPassiveSkills().isEmpty()) {
                tsm.getPetPassiveSkills().forEach(tsm::removeStatsBySkill);
            }
        }
        tsm.getPetPassiveSkills().clear();
        if (net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) {
            // SetItemInfo 1127: level = number of different set pets out (Frieren collab README)
            net.swordie.ms.world.tms282.collab.frieren.MiniFrierenPassive.apply(chr);
        }
        for (Pet pet : chr.getPets()) {
            if (pet == null || pet.getItem() == null) {
                continue;
            }
            int itemID = pet.getItem().getItemId();
            PetInfo petInfo = ItemData.getPetInfoByID(itemID);
            if (petInfo != null && petInfo.getSetItemID() != 0) {
                if (net.swordie.ms.world.tms282.collab.frieren.MiniFrierenPassive.handles(petInfo.getSetItemID())) {
                    continue; // handled above by piece count
                }
                Tuple<Integer, Integer> entry = SetItemInfoData.getSkill(petInfo.getSetItemID(), itemID);
                if (entry != null) {
                    int skillID = entry.getLeft();
                    int effectIdx = entry.getRight();
                    SkillInfo si = SkillData.getPetPassiveSkillInfoById(skillID);
                    Option o1 = new Option();
                    Option o2 = new Option();
                    if (petInfo.getSetItemID() == 149) {
                        o1.nValue = effectIdx == 3 ? 7 : effectIdx == 2 ? 5 : 3;
                        o1.nReason = 80000025 + effectIdx;
                        newStats.put(IndiePAD, o1);
                        o2.nValue = effectIdx == 3 ? 7 : effectIdx == 2 ? 5 : 3;
                        o2.nReason = 80000025 + effectIdx;
                        newStats.put(IndieMAD, o2);
                        tsm.addPetPassiveSkills(80000025 + effectIdx);
                        tsm.sendStat(newStats);
                    } else if (petInfo.getSetItemID() == 300) {
                        o1.nValue = effectIdx == 3 ? 7 : effectIdx == 2 ? 5 : 3;
                        o1.nReason = 80000110 + effectIdx;
                        newStats.put(IndiePAD, o1);
                        o2.nValue = effectIdx == 3 ? 7 : effectIdx == 2 ? 5 : 3;
                        o2.nReason = 80000110 + effectIdx;
                        newStats.put(IndieMAD, o2);
                        tsm.addPetPassiveSkills(80000110 + effectIdx);
                        tsm.sendStat(newStats);
                    } else {
                        if (si != null) {
                            o1.nValue = si.getValue(padX, 1);
                            o1.nReason = si.getSkillId();
                            newStats.put(IndiePAD, o1);
                            o2.nValue = si.getValue(madX, 1);
                            o2.nReason = si.getSkillId();
                            newStats.put(IndieMAD, o2);
                            tsm.addPetPassiveSkills(si.getSkillId());
                            tsm.sendStat(newStats);
                        }
                    }
                }
            }
        }
    }

    public void handleTideOfBattle() {
        int skillID = chr.hasSkill(TIDE_OF_BATTLE_LINK) ? TIDE_OF_BATTLE_LINK : TIDE_OF_BATTLE;
        Skill skill = chr.getSkill(skillID);
        if (chr.hasSkillOnCooldown(skillID)) {
            return;
        }
        chr.addSkillCooldown(skillID, 2 * 1000);
        if (skill == null) {
            return;
        }
        EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
        SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
        int slv = skill.getCurrentLevel();
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o1 = new Option();
        Option o2 = new Option();
        int stack = 1;
        if (tsm.hasStat(CharacterTemporaryStat.LefMageLinkSkill)) {
            stack = tsm.getOption(CharacterTemporaryStat.LefMageLinkSkill).nOption;
            if (stack < 6) {
                stack++;
            }
        }
        o1.nReason = skillID;
        o1.nOption = stack; //Stacks
        o1.tOption = si.getValue(SkillStat.time, slv);
        newStats.put(LefMageLinkSkill, o1);
        o2.nReason = skillID;
        o2.nValue = stack * slv;
        o2.tTerm = si.getValue(SkillStat.time, slv);
        newStats.put(IndieDamR, o2);
        tsm.sendStat(newStats);
    }

    public void handleJobAdvance() {
    }

    /**
     * Handles ForceAtom Collision, recreates the force atom automatically if the curRecreationCount is below maxRecreationCount
     */
    public void handleForceAtomCollision(int faKey, int skillId, int mobObjId, Position position, InPacket inPacket) {
        ForceAtom forceAtom = chr.getForceAtomByKey(faKey);
        if (forceAtom == null) {
            return;
        }
        if (forceAtom.getCurRecreationCount(faKey) < forceAtom.getMaxRecreationCount(faKey) && Util.succeedProp(forceAtom.getRecreationChance(faKey))) {
            chr.recreateforceAtom(faKey, forceAtom.recreate(faKey, chr, mobObjId, position));
        } else {
            chr.removeForceAtomByKey(faKey);
        }
    }

    private void guideGuidedArrowForceAtom(AttackInfo attackInfo) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Field field = chr.getField();
        int faKey = tsm.getOption(GuidedArrow).xOption;
        Rect rect = chr.getRectAround(new Rect(-350, -150, 100, 50));
        if (!chr.isLeft()) {
            rect = rect.horizontalFlipAround(chr.getPosition().getX());
        }
        Mob mob = (Mob) attackInfo.mobAttackInfo.stream().map(mai -> field.getLifeByObjectID(mai.mobId)).filter(Objects::nonNull).findFirst().orElse(null);
        if (mob == null || mob.getHp() <= 0) {
            if (field.getMobsInRect(rect).size() <= 0) {
                return;
            }
            mob = Util.getRandomFromCollection(field.getMobsInRect(rect));
        }
        chr.getField().broadcast(FieldPacket.guideForceAtom(chr.getId(), faKey, mob.getObjectId()));
    }

    private void setOverdriveCooldown() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o1 = new Option();
        SkillInfo si = SkillData.getSkillInfoById(OVERDRIVE);
        int slv = chr.getSkillLevel(OVERDRIVE);
        o1.nOption = -si.getValue(y, slv);
        o1.rOption = OVERDRIVE;
        o1.tOption = si.getValue(cooltime, slv) - si.getValue(time, slv);
        tsm.sendStat(ConvertAD, o1);
    }

    public void handleGuidedForceAtomCollision(int faKey, int skillId, Position position) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        ForceAtom forceAtom = chr.getForceAtomByKey(faKey);
        switch (skillId) {
            case GUIDED_ARROW:
                if (forceAtom.getCurRecreationCount(faKey) < forceAtom.getMaxRecreationCount(faKey)) {
                    forceAtom.incrementCurRecreationCount(faKey);
                } else {
                    tsm.removeStatsBySkill(skillId);
                    chr.removeForceAtomByKey(faKey);
                }
                break;
        }
    }

    private void summonSengokuForces() {
        if (!chr.hasSkill(SENGOKU_FORCE_ASSEMBLE) || !JobConstants.isSengoku(chr.getJob())) {
            return;
        }
        List<Integer> summonList = new ArrayList<Integer>() {{
            add(SENGOKU_FORCE_UESUGI);
            add(SENGOKU_FORCE_AYAME);
            add(SENGOKU_FORCE_HARUAKI);
            add(SENGOKU_FORCE_TAKEDA);
        }};

        Skill skill = chr.getSkill(SENGOKU_FORCE_ASSEMBLE);
        SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
        int slv = skill.getCurrentLevel();
        Field field = chr.getField();
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o = new Option();
        o.tTerm = si.getValue(time, slv);

        for (int i = 0; i < 2; i++) {
            int summonId = Util.getRandomFromCollection(summonList);
            summonList.removeIf(x -> x.equals(summonId));
            Summon summon = Summon.getSummonByAndSetStat(chr, summonId, slv);
            summon.setMoveAbility(MoveAbility.FixVMove);
            summon.setAssistType(AssistType.TeleportToMobs);
            field.spawnSummon(summon);
            o.nReason = summonId;

            switch (summonId) {
                case SENGOKU_FORCE_UESUGI: // Ied
                    o.nValue = si.getValue(indieIgnoreMobpdpR, slv);
                    tsm.sendStat(IndieIgnoreMobpdpR, o);
                    break;
                case SENGOKU_FORCE_AYAME: // crDmg
                    o.nValue = si.getValue(indieIgnoreMobpdpR, slv);
                    tsm.sendStat(IndieCD, o);
                    break;
                case SENGOKU_FORCE_HARUAKI: // dmg reduce
                    o.nValue = si.getValue(indieDamReduceR, slv);
                    tsm.sendStat(IndieDamReduceR, o);
                    break;
                case SENGOKU_FORCE_TAKEDA: // flat att/matt
                    o.nValue = si.getValue(indiePad, slv);
                    tsm.sendStat(IndieMAD, o);
                    tsm.sendStat(IndiePAD, o.deepCopy());
                    break;
            }
        }

    }

    public void bonusConversionOverdriveAttack() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (!JobConstants.isFlora(chr.getJob()) || !chr.hasSkill(CONVERSION_OVERDRIVE) || !tsm.hasStat(LPMagicCircuitFullDrive) || chr.hasSkillOnCooldown(CONVERSION_OVERDRIVE_ATTACK)) {
            return;
        }

        chr.write(UserLocal.userBonusAttackRequest(CONVERSION_OVERDRIVE_ATTACK));
    }

    public void handleKeyDownSkill(Char chr, SkillInfo si, InPacket inPacket) {
        if (chr != null) {
            int skillID = si.getSkillId();
            if (SkillConstants.isCooltimeOnStartSkill(skillID)) {
                chr.setSkillCooldown(skillID, chr.getSkillLevel(skillID));
            }
        }
    }

    public void handleCancelKeyDownSkill(Char chr, int skillID) {
        if (chr != null) {
            if (!SkillConstants.isCooltimeOnStartSkill(skillID)) {
                chr.setSkillCooldown(skillID, chr.getSkillLevel(skillID));
            }
        }
    }

    public void updateVSkillStackBuff(Char chr, int count) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o = new Option();
        o.nOption = count;
        tsm.sendStat(CannonShooter_BFCannonBall, o);
        chr.write(WvsContext.updateSkillStackRequestResult(-2, (byte) 1));
    }

    public void handleInitAfterMigrate(Char chr) {
        if (chr.getLevel() < 30 && !chr.hasQuest(25995)) {
            chr.createQuestWithQRValue(25995, "instantap=1");
        }
        Item weapon = chr.getEquippedInventory().getFirstItemByBodyPart(BodyPart.Weapon);
        this.weaponType = weapon == null ? 0 : ItemConstants.getWeaponTypeVal(weapon.getItemId());
        if (chr.hasSkill(GROUNDED_BODY) || chr.hasSkill(GROUNDED_BODY_LINK)) {
            int skillID = chr.hasSkill(GROUNDED_BODY) ? GROUNDED_BODY : GROUNDED_BODY_LINK;
            Option opt = new Option();
            opt.nOption = SkillData.getSkillInfoById(skillID).getValue(x, chr.getSkillLevel(skillID));
            opt.rOption = skillID;
            chr.getTemporaryStatManager().sendStat(AMLinkSkill, opt);
        }
        giveHyperArcaneForceBuff();
        givePetPassiveBuffs(true);
        chr.initSoulMP();
    }

    public void handleInitAfterField() {
        var sm = chr.getScriptManager();
        var tsm = chr.getTemporaryStatManager();
        if (JobConstants.isAngelicBuster(chr.getJob())) {
            chr.write(UserLocal.setDressChanged(true, true));
        }
        if (chr.getFieldID() != EventConstants.TERA_BLINK_FIELD) {
            sm.closeUI(1561);
        } else {
            sm.openUI(1561);
        }
        boolean newbieDialogue = net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3
                && net.swordie.ms.world.tms282.newbie.NewbieFlowService.get().replacesEntryScript(chr);
        if (chr.getFieldID() == FieldConstants.START_MAP && !newbieDialogue) {
            sm.lockUI();
        }
        if (FieldConstants.swimMaps.contains(chr.getFieldID())) {
            chr.write(FieldPacket.momentAreaOffAll(List.of("swim01")));
        }
        for (int skill : Job.REMOVE_ON_WARP) {
            if (tsm.hasStatBySkillId(skill)) {
                tsm.removeStatsBySkill(skill);
            }
        }
        if (tsm.hasStat(Flying) && !chr.getField().isFly()) {
            tsm.removeStat(Flying);
        }

        // Lynn packet here
        chr.write(ExtraTMSSystem.initField(true, chr, JobConstants.isLynn(chr.getJob()) ? 2 : 1));

        List<Integer> ascents = new ArrayList<>();
        for (Skill skill : chr.getSkills()) {
            int skillID = skill.getSkillId();
            SkillInfo si = SkillData.getSkillInfoById(skillID);
            if (si != null && si.isAscentSkill() && !si.isInvisible()) {
                ascents.add(skillID);
                break;
            }
        }
        chr.write(WvsContext.updateAscentSkillStackRequest(ascents));
    }

    public void handleKeyDownSkillCost(int skillId) {
    }

    public void update(long now) {
    }

    public void GiveVSkills() {
        for (int VSkill : MatrixConstants.GetVSkillsToGiveUponReachingV((int)chr.getJob())) {
            for (VCoreData data : VCore.getSkillNodes()) {
                if (data != null && data.getConnectSkills().getFirst() == VSkill) {
                    MatrixCore core = new MatrixCore(chr.getId(), data.getCoreID(), data.getConnectSkills().getFirst(), 0, 0);
                    core.saveToSQL();
                    chr.getMatrixCore().add(core);
                    chr.write(WvsContext.updateVMatrix(chr, true, MatrixUpdateType.Update.getVal(), 0));
                }
            }
        }
    }

    public boolean handleSecondAtomRemoveRequest(int objectId) {
        // Only used for extra shit that needs to happen upon Removing SecondAtom.
        // DOES NOT DO THE REMOVAL LOGIC.
        return true;
    }

    public void handleSecondAtomCollisionRequest(List<SecondAtomCollision> collisions) {

    }

    /**
     * Handled when a mob is hit
     */
    public void handleMobDamaged(Mob mob, long damage) {
    }

    /**
     * Handled when a mob is killed
     */
    public void handleMobKilled(Mob mob, int skillID) {
        var field = mob.getField();

        SpecialHPBossHandler.onLackingGroupHPMobHandle(chr, skillID);

        BossHelper.handleMobKilled(mob, chr);
        ExpandMenuHelper.onMobKilled(chr, mob);
        if (net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) {
            // equip info/addition/mobdie: HP / MP recovery on kill (equips README WI-6)
            net.swordie.ms.client.character.items.Tms282EquipAdditions.onMobKilled(chr);
            net.swordie.ms.world.tms282.collab.frieren.FrierenCollab.get().onMobKilled(chr, mob);
            net.swordie.ms.world.tms282.contents.changeburning.LucidDream.onMobKilled(chr, mob); // 變身燃燒 Lucid exp / LucidBoss (lucidprog)
            net.swordie.ms.world.tms282.contents.academy.AcademyService.get().onMobKilled(chr, mob); // 楓之谷學院 course kills (memory, content-1005)
        }

        // Combo Counter per Kill
        final int newCombo = chr.getComboCounter() + 1;
        chr.write(UserLocal.comboCounter(StylishKillType.COMBO, newCombo, mob.getObjectId()));
        chr.setComboCounter(newCombo);
        chr.comboKillResetTimer();
        AchievementHandler.handleComboKill(chr, newCombo);
        if (Tms282VMatrixRuntime.managed(chr)) {
            // Mob.damage invokes this only for an actual alive -> dead transition.
            Tms282VMatrixRuntime.event(chr, Tms282VMatrixSpecialEngine.Type.KILL, 1, mob.getObjectId());
            Tms282VMatrixRuntime.event(chr, Tms282VMatrixSpecialEngine.Type.COMBO_KILL, 1, mob.getObjectId());
        }

        // Exp Orb spawning from Mob every 50 combos
        if (newCombo % 50 == 0) {
            Item item;
            if (newCombo >= GameConstants.COMBO_KILL_REWARD_YELLOW) {
                item = ItemData.getItemDeepCopy(GameConstants.YELLOW_EXP_ORB_ID); // Yellow Exp Orb
            } else if (newCombo >= GameConstants.COMBO_KILL_REWARD_RED) {
                item = ItemData.getItemDeepCopy(GameConstants.RED_EXP_ORB_ID); // Red Exp Orb
            } else if (newCombo >= GameConstants.COMBO_KILL_REWARD_PURPLE) {
                item = ItemData.getItemDeepCopy(GameConstants.PURPLE_EXP_ORB_ID); // Purple Exp Orb
            } else {
                item = ItemData.getItemDeepCopy(GameConstants.BLUE_EXP_ORB_ID); // Blue Exp Orb
            }
            Drop drop = new Drop(-1, item);
            drop.setMobExp(mob.getForcedMobStat().getExp() * ServerConfig.EXP_RATE);
            var dropPos = mob.getPosition().deepCopy();
            chr.getField().drop(drop, dropPos);
        }

        // Events
        // Random portal spawn
        if (EventConstants.RANDOM_PORTAL_EVENT && field.getFieldType() == FieldType.DEFAULT) {
            if (field.isChannelField() && chr.getNextRandomPortalTime() <= System.currentTimeMillis()
                    && Util.succeedProp(EventConstants.RANDOM_PORTAL_SPAWN_CHANCE, 1000)) {
                chr.setNextRandomPortalTime(System.currentTimeMillis() + EventConstants.RANDOM_PORTAL_COOLTIME); // 50% chance for inferno/yellow portal
                List<Foothold> listOfFootHolds = new ArrayList<>(field.getNonWallFootholds());
                Foothold foothold = Util.getRandomFromCollection(listOfFootHolds);
                Position position = foothold.getRandomPosition();
                List<RandomPortal.Type> randomPortalTypeList = new ArrayList<>();
                //randomPortalTypeList.add(RandomPortal.Type.TreasureCatch);
                randomPortalTypeList.add(RandomPortal.Type.PolloFritto);
                randomPortalTypeList.add(RandomPortal.Type.Inferno);
                RandomPortal.Type portalType = Util.getRandomFromCollection(randomPortalTypeList);
                RandomPortal randomPortal = new RandomPortal(portalType, position, chr.getId());
                field.addLife(randomPortal);
                chr.getField().broadcast(WvsContext.randomPortalNotice(randomPortal, chr.getFieldID()));
                chr.getField().broadcast(RandomPortalPool.created(randomPortal));
            }
        }

        switch (skillID) {
            case ERDA_FOUNTAIN -> {
                var summon = field.getSummonBySkillId(chr, skillID);
                if (summon != null) {
                    summon.incMobDefeated(1);
                    if (summon.getMobDefeated() >= 12) {
                        summon.setMobDefeated(0);
                        field.broadcast(Summoned.erdaFountain(summon, 0, true));
                    } else {
                        field.broadcast(Summoned.erdaFountain(summon, summon.getMobDefeated(), false));
                    }
                }
            }
        }
    }
}
