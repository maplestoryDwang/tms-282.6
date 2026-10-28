package net.swordie.ms.client.jobs.anima;

import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.CharacterStat;
import net.swordie.ms.client.character.info.HitInfo;
import net.swordie.ms.client.character.items.BodyPart;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.client.character.skills.ExtraSkill;
import net.swordie.ms.client.character.skills.ForceAtom;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.Skill;
import net.swordie.ms.client.character.skills.SkillStat;
import net.swordie.ms.client.character.skills.info.*;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.client.jobs.Job;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.Effect;
import net.swordie.ms.connection.packet.Summoned;
import net.swordie.ms.connection.packet.UserLocal;
import net.swordie.ms.connection.packet.UserPacket;
import net.swordie.ms.connection.packet.WvsContext;
import net.swordie.ms.constants.FieldConstants;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.constants.QuestConstants;
import net.swordie.ms.enums.AssistType;
import net.swordie.ms.enums.ForceAtomEnum;
import net.swordie.ms.enums.MoveAbility;
import net.swordie.ms.life.Life;
import net.swordie.ms.life.Summon;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.life.mob.MobStat;
import net.swordie.ms.life.mob.MobTemporaryStat;
import net.swordie.ms.loaders.ItemData;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.scripts.ScriptManagerImpl;
import net.swordie.ms.util.Position;
import net.swordie.ms.util.Rect;
import net.swordie.ms.util.Util;
import net.swordie.ms.world.field.Field;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;
import static net.swordie.ms.enums.InvType.EQUIPPED;

public class HoYoung extends Job {

    // Beginner Skills (Anima Thief)
    public static final int RETURN_TO_CHEONGWOON = 160001074; // TODO: Not handled.
    public static final int SPIRIT_AFFINITY = 160000000;
    public static final int SHAPESHIFT = 160001075;
    public static final int FIEND_SEAL = 160000076;

    // 1st Job
    public static final int TALISMAN_ENERGY = 164000010;
    public static final int HUMANITY_AS_YOU_WILL_FAN = 164001000; //Humanity
    public static final int TALISMAN_EVIL_SEALING_GOURD = 164001001; //Talisman
    public static final int TALISMAN_EVIL_SEALING_GOURD_EFFECT = 164001002; //Talisman

    public static final int GRACEFUL_FLIGHT = 164001003; // FJ
    public static final int LIGHT_STEPS = 164000011; // (passive)
    public static final int SHROUDING_MIST = 164000012; // (passive)
    public static final int NIMBUS_CLOUD = 164001004;

    // 2nd Job
    public static final int GROUND_SHATTERING_WAVE = 164101000; //Earth
    public static final int GROUND_SHATTERING_WAVE_1 = 164100000;
    public static final int GROUND_SHATTERING_WAVE_2 = 164101001;
    public static final int GROUND_SHATTERING_WAVE_3 = 164101002;
    public static final int CLONE = 164101003; //Talisman
    public static final int CLONE_1 = 164101004; //Talisman
    public static final int RITUAL_FAN_ACCELERATION = 164101005;
    public static final int RITUAL_FAN_MASTERY = 164100010;
    public static final int OUT_OF_SIGHT = 164100006;
    public static final int OUT_OF_SIGHT_1 = 164101006;
    public static final int THIRD_EYE = 164100011;
    public static final int HEAVENLY_BODY = 164100012;
    public static final int FORTUNE_FITNESS = 164100013;

    // 3rd Job
    public static final int SCROLL_ENERGY = 164110014;
    public static final int IRON_FAN_GALE = 164111000;
    public static final int IRON_FAN_GALE_1 = 164110000;
    public static final int IRON_FAN_GALE_2 = 164111001;
    public static final int IRON_FAN_GALE_3 = 164111002;
    public static final int IRON_FAN_GALE_4 = 164111009;
    public static final int IRON_FAN_GALE_5 = 164111010;
    public static final int IRON_FAN_GALE_6 = 164111011;
    public static final int STONE_TREMOR = 164111003; //Earth
    public static final int STONE_TREMOR_1 = 164110003;
    public static final int STONE_TREMOR_2 = 164111004;
    public static final int STONE_TREMOR_3 = 164111005;
    public static final int STONE_TREMOR_4 = 164111006;
    public static final int SEEKING_GHOST_FLAME = 164111007; //Talisman
    public static final int DEGENERATION = 164111008; //Scroll
    public static final int ATTAINMENT = 164110010;
    public static final int ASURA = 164110011;
    public static final int DIAMOND_BODY = 164110012;
    public static final int BALANCED_BREATH = 164110013;

    // 4th Job
    public static final int CONSUMING_FLAMES = 164121000; // Heaven
    public static final int CONSUMING_FLAMES_1 = 164120000;
    public static final int CONSUMING_FLAMES_2 = 164121001;
    public static final int CONSUMING_FLAMES_3 = 164121002;
    public static final int CONSUMING_FLAMES_4 = 164121014;
    public static final int GOLD_BANDED_CUDGEL = 164121003; // Humanity
    public static final int GOLD_BANDED_CUDGEL_1 = 164121004; // Humanity
    public static final int THOUSAND_TON_STONE = 164121005;
    public static final int WARP_GATE = 164121006;
    public static final int WARP_GATE_1 = 164121011; // Talisman
    public static final int WARP_GATE_2 = 164121012; // Talisman
    public static final int STAR_VORTEX = 164121008; // Scroll
    public static final int STAR_VORTEX_1 = 164121015; // Scroll
    public static final int BUTTERFLY_DREAM = 164121007; // Scroll
    public static final int BUTTERFLY_DREAM_ATOM = 164120007; // Scroll
    public static final int ANIMA_WARRIOR = 164121009;
    public static final int ANIMA_HERO_WILL = 164121010;
    public static final int ADVANCED_RITUAL_FAN_MASTERY = 164120010;
    public static final int ENLIGHTENMENT = 164120011;
    public static final int DRAGONS_EYE = 164120012;

    public static final int MASTER_ELIXIR = 164121041;
    public static final int DREAM_GARDEN = 164121042; // iFrame
    public static final int MASTER_CLONE_TRANSFORMATION = 164121043; // Scroll Attr
    public static final int MASTER_CLONE_TRANSFORMATION_MOB_DEBUFF = 164121044; // Applies Debuff on Mob  Bind

    // V Skills
    public static final int CLONE_RAMPAGE = 400041048;
    public static final int CLONE_RAMPAGE_ATOM = 400041049;
    public static final int SAGE_TIGER_OF_SONGYU = 400041050;
    public static final int SAGE_TIGER_OF_SONGYU_ATTACK = 400041051;
    public static final int SAGE_WRATH_OF_GODS = 400041052;
    public static final int SAGE_WRATH_OF_GODS_ATTACK = 400041053;
    public static final int THREE_PATHS_APPARITION_BUFF = 400041063; // Buff
    public static final int THREE_PATHS_APPARITION_ATTACK_1 = 400041064; // Attack | Heaven
    public static final int THREE_PATHS_APPARITION_ATTACK_2 = 400041065; // Attack | Earth
    public static final int THREE_PATHS_APPARITION_ATTACK_3 = 400041066; // Attack | Human
    public static final int THREE_PATHS_APPARITION_COOLTIMES = 400041067; // Cooltimes | x passive | y active
    public static final int THREE_PATHS_APPARITION_PROC = 400041068; // Skill Effect when proccing

    // HEXA Boosts
    public static final int HEXA_CLONE_RAMPAGE = 500061014;
    public static final int HEXA_CLONE_RAMPAGE_ATOM = 500061015; // TODO

    // Gauge
    private static final int MAX_TALISMAN_ENERGY = 100;
    private static final int MAX_SCROLL_ENERGY = 900;
    private static final int BASE_TALISMAN_ENERGY_TO_ADD = 10;
    private static final int COMBO_TALISMAN_ENERGY_TO_ADD = 5;
    private static final int BASE_SCROLL_ENERGY_TO_ADD = 15;

    private int comboCount = 1;
    private int sageTigerSongyuCount = 0;
    private int threePathsAttributeMask = 0;
    private Seals lastSealUsed;

    private enum Seals {
        HEAVEN,
        EARTH,
        HUMANITY
    }

    // Degeneration
    private static final Integer[] GROUNDED_MORPH = new Integer[]{
            2400500,
            2400501,
            2400502
    };

    private static final Integer[] FLYING_MORPH = new Integer[]{
            2400503
    };

    // Evil-Sealing Gourd
    private boolean isUsingGourdToss = false; // scuffed
    private final Stack<Mob> swallowedMobs = new Stack<>();

    private static final int[] addedSkills = new int[]{
            RETURN_TO_CHEONGWOON,
            SPIRIT_AFFINITY,
            SHAPESHIFT,
            FIEND_SEAL,
            NIMBUS_CLOUD,
    };

    public HoYoung(Char chr) {
        super(chr);
        if (chr.getId() != 0 && isHandlerOfJob(chr.getJob())) {
            for (int id : addedSkills) {
                if (!chr.hasSkill(id)) {
                    Skill skill = SkillData.getSkillDeepCopyById(id);
                    if (skill != null) {
                        skill.setCurrentLevel(skill.getMasterLevel());
                        chr.addSkill(skill);
                    }
                }
            }
        }
    }


    private boolean hasHumanity() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        return tsm.hasStat(AnimaThiefTaoistType) && tsm.getOption(AnimaThiefTaoistType).yOption == 0;
    }

    private boolean hasEarth() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        return tsm.hasStat(AnimaThiefTaoistType) && tsm.getOption(AnimaThiefTaoistType).xOption == 0;
    }

    private boolean hasHeaven() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        return tsm.hasStat(AnimaThiefTaoistType) && tsm.getOption(AnimaThiefTaoistType).nOption == 0;
    }

    public int getTalismanEnergy() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        return tsm.getOption(CharacterTemporaryStat.AnimaThiefTaoistGauge).nOption;
    }

    public int getScrollEnergy() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        return tsm.getOption(CharacterTemporaryStat.AnimaThiefTaoistGauge).xOption;
    }

    private void createButterflyForceAtom() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (!tsm.hasStat(CharacterTemporaryStat.TriflingWhimOnOff)) {
            return;
        }
        int fImpact = new Random().nextInt(10) + 35;
        ForceAtomEnum fae = null;
        for (int i = 0; i < 5; i++) {
            switch (i) {
                case 0 -> fae = ForceAtomEnum.BUTTERYFLY_1;
                case 1 -> fae = ForceAtomEnum.BUTTERYFLY_2;
                case 2 -> fae = ForceAtomEnum.BUTTERYFLY_3;
                case 3 -> fae = ForceAtomEnum.BUTTERYFLY_4;
                case 4 -> fae = ForceAtomEnum.BUTTERYFLY_5;
            }
            ForceAtomInfo fai = new ForceAtomInfo(chr.getNewForceAtomKey(), fae.getInc(), fImpact, 1,
                    100 + (i * 30), 0, Util.getCurrentTime(), 0, 0,
                    new Position(30, 0));

            ForceAtom fa = new ForceAtom(false, 0, chr.getId(), fae,
                    true, 0, BUTTERFLY_DREAM_ATOM, fai, new Rect(), 0, 0,
                    new Position(), 0, chr.getPosition(), 0);

            chr.createForceAtom(fa);
            chr.addSkillCooldown(BUTTERFLY_DREAM, 2 * 1000);
        }
    }

    public void createCloneForceAtom(AttackInfo attackInfo) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (!tsm.hasStat(CharacterTemporaryStat.AnimaThiefCloneAttack)) {
            return;
        }
        SkillInfo si = SkillData.getSkillInfoById(CLONE);
        int slv = chr.getSkillLevel(CLONE);
        if (si == null || slv <= 0) {
            return;
        }
        ForceAtomEnum fae = ForceAtomEnum.CLONE_ATOM;
        List<Mob> mobs = new ArrayList<>(3);
        int maxTargets = Math.max(1, si.getValue(SkillStat.v2, slv));
        for (MobAttackInfo mai : attackInfo.mobAttackInfo) {
            if (mobs.size() >= maxTargets) {
                break;
            }
            Life life = chr.getField().getLifeByObjectID(mai.mobId);
            if (life instanceof Mob mob) {
                if (mob.getHp() > 0) {
                    mobs.add(mob);
                }
            }
        }
        int i = 0;
        for (Mob mob : mobs) {
            int fImpact = new Random().nextInt(10) + 35;
            ForceAtomInfo fai = new ForceAtomInfo(chr.getNewForceAtomKey(), fae.getInc(), fImpact, 1,
                    100 + (i * 30), 0, Util.getCurrentTime(), 1, 0,
                    new Position(30, 0));
            ForceAtom fa = new ForceAtom(false, 0, chr.getId(), fae,
                    true, mob.getObjectId(), CLONE_1, fai, new Rect(), 0, 0,
                    new Position(), 0, chr.getPosition(), 0);
            chr.createForceAtom(fa);
            chr.addSkillCooldown(CLONE_1, si.getValue(SkillStat.t, slv) * 1000);
            i++;
        }
    }

    public void createCloneRForceAtom() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (!tsm.hasStat(CharacterTemporaryStat.AnimaThiefFifthCloneAttack)) {
            return;
        }
        ForceAtomEnum fae = ForceAtomEnum.CLONE_R_ATOM;
        for (int i = 0; i < 3; i++) {
            int fImpact = new Random().nextInt(10) + 35;
            ForceAtomInfo fai = new ForceAtomInfo(chr.getNewForceAtomKey(), fae.getInc(), fImpact, 1,
                    100 + (i * 30), 0, Util.getCurrentTime(), 1, 0,
                    new Position(30, 0));
            ForceAtom fa = new ForceAtom(false, 0, chr.getId(), fae,
                    true, 0, CLONE_RAMPAGE_ATOM, fai, new Rect(), 0, 0,
                    new Position(), 0, chr.getPosition(), 0);
            chr.createForceAtom(fa);
            chr.addSkillCooldown(CLONE_RAMPAGE_ATOM, 2 * 1000);
        }
    }

    private boolean isHumanitySkill(int skillId) {
        return switch (skillId) {
            case HUMANITY_AS_YOU_WILL_FAN, GOLD_BANDED_CUDGEL, GOLD_BANDED_CUDGEL_1,
                    THREE_PATHS_APPARITION_ATTACK_3 -> true;
            default -> false;
        };
    }

    private boolean isEarthSkill(int skillId) {
        return switch (skillId) {
            case STONE_TREMOR, STONE_TREMOR_1, STONE_TREMOR_2, STONE_TREMOR_3, STONE_TREMOR_4,
                    GROUND_SHATTERING_WAVE, GROUND_SHATTERING_WAVE_1, GROUND_SHATTERING_WAVE_2, GROUND_SHATTERING_WAVE_3,
                    THREE_PATHS_APPARITION_ATTACK_2 -> true;
            default -> false;
        };
    }

    private boolean isHeavenSkill(int skill) {
        return switch (skill) {
            case CONSUMING_FLAMES, CONSUMING_FLAMES_1, CONSUMING_FLAMES_2, CONSUMING_FLAMES_3, CONSUMING_FLAMES_4,
                    IRON_FAN_GALE, IRON_FAN_GALE_1, IRON_FAN_GALE_2, IRON_FAN_GALE_3,
                    IRON_FAN_GALE_4, IRON_FAN_GALE_5, IRON_FAN_GALE_6,
                    THREE_PATHS_APPARITION_ATTACK_1 -> true;
            default -> false;
        };
    }

    private boolean isAttributeSkill(int skillId) {
        return isHumanitySkill(skillId) || isEarthSkill(skillId) || isHeavenSkill(skillId);
    }

    private int getAttributeMask(int skillId) {
        if (isHeavenSkill(skillId)) {
            return 1;
        }
        if (isEarthSkill(skillId)) {
            return 2;
        }
        if (isHumanitySkill(skillId)) {
            return 4;
        }
        return 0;
    }

    private int getThreePathsAttackSkill(int usedAttributeMask) {
        if ((usedAttributeMask & 1) == 0) {
            return THREE_PATHS_APPARITION_ATTACK_1;
        }
        if ((usedAttributeMask & 2) == 0) {
            return THREE_PATHS_APPARITION_ATTACK_2;
        }
        return THREE_PATHS_APPARITION_ATTACK_3;
    }

    private void handleBalancedBreathRecovery() {
        Skill skill = chr.getSkill(BALANCED_BREATH);
        if (skill == null) {
            return;
        }
        SkillInfo si = SkillData.getSkillInfoById(BALANCED_BREATH);
        int slv = skill.getCurrentLevel();
        if (si == null || slv <= 0) {
            return;
        }
        chr.heal(chr.getHPPerc(si.getValue(SkillStat.x, slv)));
        chr.healMP(chr.getMPPerc(si.getValue(SkillStat.y, slv)));
    }

    private void handleThreePathsApparition(int skillId, AttackInfo attackInfo) {
        if (!chr.hasSkill(THREE_PATHS_APPARITION_BUFF) || !chr.getTemporaryStatManager().hasStat(AnimaThiefIllusion)) {
            return;
        }
        int attr = getAttributeMask(skillId);
        if (attr == 0 || chr.hasSkillOnCooldown(THREE_PATHS_APPARITION_COOLTIMES)) {
            return;
        }
        threePathsAttributeMask |= attr;
        int extraSkillId = getThreePathsAttackSkill(threePathsAttributeMask);
        ExtraSkill extraSkill = new ExtraSkill(extraSkillId, chr.getPosition());
        extraSkill.FaceLeft = attackInfo.left ? 1 : 0;
        extraSkill.Value = 1;
        if (!attackInfo.mobAttackInfo.isEmpty()) {
            extraSkill.MobOIDs.add(attackInfo.mobAttackInfo.get(0).mobId);
        }
        chr.write(UserLocal.registerExtraSkill(THREE_PATHS_APPARITION_BUFF, Collections.singletonList(extraSkill)));
        SkillInfo si = SkillData.getSkillInfoById(THREE_PATHS_APPARITION_COOLTIMES);
        int slv = chr.getSkillLevel(THREE_PATHS_APPARITION_BUFF);
        int cooldown = si == null ? 5000 : Math.max(1000, si.getValue(SkillStat.q, slv) * 1000);
        chr.addSkillCooldown(THREE_PATHS_APPARITION_COOLTIMES, cooldown);
        if (threePathsAttributeMask == 7) {
            threePathsAttributeMask = 0;
        }
    }

    private boolean isTalismanSkill(int skillId) {
        return switch (skillId) {
            case TALISMAN_EVIL_SEALING_GOURD, TALISMAN_EVIL_SEALING_GOURD_EFFECT,
                    CLONE, CLONE_1, SEEKING_GHOST_FLAME, WARP_GATE, WARP_GATE_1, WARP_GATE_2 -> true;
            default -> false;
        };
    }

    private boolean isScrollSkill(int skillId) {
        return switch (skillId) {
            case DEGENERATION, BUTTERFLY_DREAM, BUTTERFLY_DREAM_ATOM, STAR_VORTEX, STAR_VORTEX_1,
                    MASTER_CLONE_TRANSFORMATION, MASTER_CLONE_TRANSFORMATION_MOB_DEBUFF -> true;
            default -> false;
        };
    }

    public void handleSeals(boolean humanity, boolean earth, boolean heaven) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o1 = new Option();

        o1.nOption = heaven ? 0 : 1; // Blue
        o1.xOption = earth ? 0 : 1; // Yellow
        o1.yOption = humanity ? 0 : 1; //Purple
        o1.rOption = 16400; //JobID?

        tsm.sendStat(AnimaThiefTaoistType, o1);
    }

    public void restoreMasterExilir() {
        int skillID = MASTER_ELIXIR;
        int slv = chr.getSkillLevel(skillID);
        SkillInfo si = SkillData.getSkillInfoById(skillID);
        if (si == null) {
            return;
        }
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (!tsm.hasStat(MiracleDrug)) {
            return;
        }
        int talismanEnergy = si.getValue(SkillStat.x, slv);
        int scrollEnergy = si.getValue(SkillStat.y, slv);
        Option o1 = tsm.getOption(AnimaThiefTaoistGauge);
        if (talismanEnergy >= MAX_TALISMAN_ENERGY) {
            talismanEnergy = MAX_TALISMAN_ENERGY;
        }
        if (scrollEnergy >= MAX_SCROLL_ENERGY) {
            scrollEnergy = MAX_SCROLL_ENERGY;
        }
        o1.nOption += talismanEnergy;
        o1.xOption += scrollEnergy;
        o1.rOption = 16400; //JobID?
        tsm.sendStat(AnimaThiefTaoistGauge, o1);
        chr.write(WvsContext.updateSkillStackRequestResult(skillID, (byte) o1.nOption));
    }

    public void handleSpellGauge(int talismanEnergy, int scrollEnergy) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o1 = new Option();
        if (talismanEnergy >= MAX_TALISMAN_ENERGY) {
            talismanEnergy = MAX_TALISMAN_ENERGY;
        }
        if (scrollEnergy >= MAX_SCROLL_ENERGY) {
            scrollEnergy = MAX_SCROLL_ENERGY;
        }
        o1.nOption = talismanEnergy;
        o1.xOption = scrollEnergy;
        o1.rOption = 16400; //JobID?
        tsm.sendStat(AnimaThiefTaoistGauge, o1);
    }

    public void handleTalismanGauge(int skillId) {
        int talismanEnergyToAdd = BASE_TALISMAN_ENERGY_TO_ADD;
        int scrollEnergyToAdd = BASE_SCROLL_ENERGY_TO_ADD;

        if (!hasHumanity() && !hasHeaven() && !hasEarth()) {
            comboCount = 1;
            lastSealUsed = null;
            handleSeals(true, true, true);
        }

        Seals currentSealUsed = null;
        if (isHumanitySkill(skillId) && hasHumanity()) {
            currentSealUsed = Seals.HUMANITY;
        } else if (isEarthSkill(skillId) && hasEarth()) {
            currentSealUsed = Seals.EARTH;
        } else if (isHeavenSkill(skillId) && hasHeaven()) {
            currentSealUsed = Seals.HEAVEN;
        }

        if (currentSealUsed != null && lastSealUsed != null) {
            if (lastSealUsed == currentSealUsed) {
                comboCount = 1;
            } else {
                comboCount++;
                if (comboCount >= 3) {
                    comboCount = 3;
                }
            }
        } else {
            comboCount = 1;
        }

        if (isHumanitySkill(skillId)) {
            lastSealUsed = hasHumanity() ? Seals.HUMANITY : null;
            handleSeals(false, hasEarth(), hasHeaven());
        } else if (isEarthSkill(skillId)) {
            lastSealUsed = hasEarth() ? Seals.EARTH : null;
            handleSeals(hasHumanity(), false, hasHeaven());
        } else if (isHeavenSkill(skillId)) {
            lastSealUsed = hasHeaven() ? Seals.HEAVEN : null;
            handleSeals(hasHumanity(), hasEarth(), false);
        }

        if (!hasHumanity() && !hasEarth() && !hasHeaven()) {
            handleBalancedBreathRecovery();
        }

        talismanEnergyToAdd += COMBO_TALISMAN_ENERGY_TO_ADD * (comboCount - 1);

        handleSpellGauge(getTalismanEnergy() + talismanEnergyToAdd, getScrollEnergy() + scrollEnergyToAdd);

        if (chr.getLevel() < 30) {
            if (!hasHumanity()) {
                comboCount = 1;
                lastSealUsed = null;
                handleSeals(true, true, true);
            }
        } else if (chr.getLevel() < 60) {
            if (!hasHumanity() && !hasEarth()) {
                comboCount = 1;
                lastSealUsed = null;
                handleSeals(true, true, true);
            }
        } else {
            if (!hasHumanity() && !hasHeaven() && !hasEarth()) {
                comboCount = 1;
                lastSealUsed = null;
                handleSeals(true, true, true);
            }
        }
    }

    @Override
    public boolean isHandlerOfJob(short id) {
        return JobConstants.isHoYoung(id);
    }

    // Attack related methods ------------------------------------------------------------------------------------------
    @Override
    public void handleDebuffOnMob(Client c, Mob mob, SkillInfo si, int skillID, int slv, long damage) {
        Char chr = c.getChr();
        Option o1 = new Option();
        MobTemporaryStat mts = mob.getTemporaryStat();
        switch (skillID) {
            case HUMANITY_AS_YOU_WILL_FAN:
                // TODO
                break;
            case TALISMAN_EVIL_SEALING_GOURD:
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    o1.rOption = skillID;
                    o1.nOption = slv;
                    o1.tOption = 23;
                    o1.xOption = chr.getId();
                    if (!mob.isBoss() && ((mob.getLevel() - chr.getLevel()) <= 11)) { // TODO: stationary mobs?
                        mts.addStatOptions(mob, MobStat.MobLock, o1);
                        swallowedMobs.push(mob);
                    }
                    swallowMobForceAtom(mob);
                }
                break;
            case DEGENERATION:
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    int morphId = Util.getRandomFromCollection(mob.getFlySpeed() > 0 ? FLYING_MORPH : GROUNDED_MORPH);
                    o1.rOption = skillID;
                    o1.nOption = morphId;
                    o1.tOption = 60000;
                    mts.addStatOptions(mob, MobStat.Morph, o1);
                }
                break;
        }
        super.handleDebuffOnMob(c, mob, si, skillID, slv, damage);
    }

    @Override
    public void handleAttack(Client c, AttackInfo attackInfo, SkillInfo si, long now) {
        Char chr = c.getChr();
        super.handleAttack(c, attackInfo, si, now);
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        int skillID = attackInfo.skillId;
        boolean hasHitMobs = !attackInfo.mobAttackInfo.isEmpty();
        int slv = attackInfo.slv;
        Option o1 = new Option();
        switch (attackInfo.skillId) {
            case TALISMAN_EVIL_SEALING_GOURD_EFFECT:
                this.isUsingGourdToss = true;
                break;
            case GROUND_SHATTERING_WAVE:
                o1.nValue = si.getValue(SkillStat.x, slv);
                o1.nReason = OUT_OF_SIGHT;
                o1.tTerm = 2;
                tsm.sendStat(DarkSight, o1);
        }
        handleSpellGauge(
                isTalismanSkill(skillID) ? 0 : getTalismanEnergy(),
                isScrollSkill(skillID) ? 0 : getScrollEnergy()
        );
        if (hasHitMobs) {
            if (isAttributeSkill(skillID)) {
                handleTalismanGauge(skillID);
                handleThreePathsApparition(skillID, attackInfo);
            }
            if (!chr.hasSkillOnCooldown(SAGE_WRATH_OF_GODS_ATTACK)) {
                doSageGodAssist();
            }
            if (!chr.hasSkillOnCooldown(SAGE_TIGER_OF_SONGYU_ATTACK)) {
                doSageTigerSongyu();
            }
            if (!chr.hasSkillOnCooldown(BUTTERFLY_DREAM)) {
                createButterflyForceAtom();
            }
            if (!chr.hasSkillOnCooldown(CLONE_RAMPAGE_ATOM)) {
                createCloneRForceAtom();
            }
            if (!chr.hasSkillOnCooldown(CLONE_1)) {
                createCloneForceAtom(attackInfo);
            }
        }
    }

    @Override
    public void handleSkill(Client c, InPacket inPacket, SkillUseInfo skillUseInfo) {
        super.handleSkill(c, inPacket, skillUseInfo);
        Char chr = c.getChr();
        SkillInfo si = skillUseInfo.skillInfo;
        int slv = skillUseInfo.slv;
        int skillID = skillUseInfo.skillID;
        Option o1 = new Option();
        Option o2 = new Option();
        int scrollEnergyToAdd = 0;
        if (chr.getJob() > 16410 && chr.getJob() <= 16412) {
            scrollEnergyToAdd = isTalismanSkill(skillID) ? 200 : 15;
        }
        Field field = chr.getField();
        EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Summon summon;

        switch (skillID) {
            case SHAPESHIFT:
                boolean enable;
                int qid = QuestConstants.SHAPESHIFT_QR;
                if (chr.getQRValueByKey(qid, "sw") != null && chr.getQRValueByKey(qid, "sw").equalsIgnoreCase("0")) {
                    chr.setQRValueByKey(qid, "sw", "1");
                    enable = true;
                } else {
                    chr.createQuestWithQRValue(qid, "sw=0");
                    enable = false;
                }
                chr.getField().broadcast(UserPacket.shapeShiftResult(chr.getId(), enable));
                chr.addSkillCooldown(SHAPESHIFT, 10000);
                break;
            case RITUAL_FAN_ACCELERATION:
                o1.nValue = si.getValue(SkillStat.x, slv);
                o1.nReason = skillID;
                o1.tTerm = si.getValue(SkillStat.time, slv);
                tsm.sendStat(IndieBooster, o1);
                break;
            case ANIMA_HERO_WILL:
                tsm.removeAllDebuffs();
                break;
            case ANIMA_WARRIOR:
                o1.nReason = skillID;
                o1.nValue = si.getValue(SkillStat.x, slv);
                o1.tTerm = si.getValue(SkillStat.time, slv);
                tsm.sendStat(IndieStatR, o1);
                break;
            case CLONE:
                o1.rOption = CLONE;
                o1.nOption = 1;
                o1.yOption = CLONE;
                o1.tOption = si.getValue(SkillStat.time, slv);
                tsm.sendStat(AnimaThiefCloneAttack, o1);
                break;
            case SEEKING_GHOST_FLAME:
                summon = Summon.getSummonByAndSetStat(chr, skillID, slv);
                summon.setFlyMob(true);
                summon.setMoveAction((byte) 4);
                summon.setAssistType(AssistType.Summon);
                summon.setMoveAbility(MoveAbility.Fly);
                field.spawnSummon(summon);
                break;
            case STAR_VORTEX:
                summon = Summon.getSummonByAndSetStat(chr, skillID, slv);
                summon.setMoveAbility(MoveAbility.Stop);
                field.spawnSummon(summon);
                break;
            case STAR_VORTEX_1:
                List<Char> chrList = new ArrayList<>();
                chr.getField().getSummons().stream().filter(s -> s.getSkillID() == STAR_VORTEX && s.getOwnerId() == chr.getId()).forEach(chr.getField()::removeLife);
                int percentHealed = 5;
                if (chr.getParty() != null) {
                    chrList.addAll(chr.getParty().getPartyMembersInSameField(chr));
                } else {
                    chr.heal((int) ((chr.getMaxHP() * percentHealed) / 100D));
                    chr.healMP((int) ((chr.getMaxMP() * percentHealed) / 100D));
                }
                chrList.add(chr);
                for (Char pChr : chrList) {
                    pChr.heal((int) ((pChr.getMaxHP() * percentHealed) / 100D));
                    pChr.healMP((int) ((pChr.getMaxMP() * percentHealed) / 100D));
                }
                break;
            case WARP_GATE:
                chr.getField().getSummons().stream().filter(s -> s.getSkillID() == WARP_GATE && s.getOwnerId() == chr.getId()).forEach(chr.getField()::removeLife);
                chr.getField().getSummons().stream().filter(s -> s.getSkillID() == WARP_GATE_1 && s.getOwnerId() == chr.getId()).forEach(chr.getField()::removeLife);
                summon = Summon.getSummonByAndSetStat(chr, skillID, slv);
                summon.setMoveAbility(MoveAbility.Stop);
                field.spawnSummon(summon);
                break;
            case WARP_GATE_1:
                summon = Summon.getSummonByAndSetStat(chr, skillID, slv);
                summon.setMoveAbility(MoveAbility.Stop);
                field.spawnSummon(summon);
                break;
            case BUTTERFLY_DREAM:
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(SkillStat.time, slv);
                newStats.put(TriflingWhimOnOff, o1);
                o2.nValue = si.getValue(SkillStat.indiePMdR, slv);
                o2.nReason = skillID;
                o2.tTerm = si.getValue(SkillStat.time, slv);
                newStats.put(IndiePMdR, o2);
                tsm.sendStat(newStats);
                break;
            case MASTER_ELIXIR:
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(SkillStat.time, slv);
                newStats.put(MiracleDrug, o1);
                o2.nOption = 10;
                o2.rOption = skillID;
                o2.tOption = si.getValue(SkillStat.time, slv);
                newStats.put(IndiePMdR, o2);
                tsm.sendStat(newStats);
                break;
            case CLONE_RAMPAGE:
            case HEXA_CLONE_RAMPAGE:
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.yOption = CLONE;
                o1.tOption = si.getValue(SkillStat.time, slv);
                tsm.sendStat(AnimaThiefFifthCloneAttack, o1);
                break;
            case SAGE_WRATH_OF_GODS:
                summon = Summon.getSummonByAndSetStat(chr, skillID, slv);
                summon.setMoveAction((byte) 4);
                summon.setAssistType(AssistType.Summon);
                summon.setMoveAbility(MoveAbility.Smart);
                field.spawnSummon(summon);
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(SkillStat.time, slv);
                newStats.put(AnimaThiefMetaphysics, o1);
                o2.nValue = si.getValue(SkillStat.indieDamR, slv);
                o2.nReason = skillID;
                o2.tTerm = si.getValue(SkillStat.time, slv);
                newStats.put(IndieDamR, o2);
                tsm.sendStat(newStats);
                break;
            case SAGE_TIGER_OF_SONGYU:
                summon = Summon.getSummonByAndSetStatWithTime(chr, skillID, slv, System.currentTimeMillis(), 57);
                summon.setAssistType(AssistType.Attack);
                summon.setMoveAbility(MoveAbility.Stop);
                field.spawnSummon(summon);
                this.sageTigerSongyuCount = 0;
                break;
            case THREE_PATHS_APPARITION_BUFF:
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(SkillStat.time, slv);
                tsm.sendStat(AnimaThiefIllusion, o1);
                this.threePathsAttributeMask = 0;
                break;
        }
        handleSpellGauge(
                isTalismanSkill(skillID) ? 0 : getTalismanEnergy(),
                isScrollSkill(skillID) ? 0 : isTalismanSkill(skillID) ? getScrollEnergy() + scrollEnergyToAdd : getScrollEnergy()
        );
    }

    @Override
    public void handleHit(Client c, InPacket inPacket, HitInfo hitInfo) {

        super.handleHit(c, inPacket, hitInfo);
    }

    @Override
    public void setCharCreationStats(Char chr) {
        super.setCharCreationStats(chr);
        CharacterStat cs = chr.getAvatarData().getCharacterStat();
        cs.setPosMap(FieldConstants.HOME_MAP);//cs.setPosMap(FieldConstants.HENESYS_ID);
        cs.setJob(JobConstants.JobEnum.HOYOUNG_1.getJobId());
        cs.setLevel(10);
        cs.setStr(4);
        cs.setDex(4);
        cs.setInt(4);
        cs.setLuk(45);
        cs.setHp(1000);
        cs.setMaxHp(1000);
        cs.setMp(500);
        cs.setMaxMp(500);
        cs.getExtendSP().addSpToJobLevel(1, 5);
    }

    private void swallowMobForceAtom(Mob mob) {
        ForceAtomEnum fae = ForceAtomEnum.SWALLOW_MOB;
        int mobObjId;
        Rect rect = new Rect(-405, -50, 405, 50);
        final ForceAtomInfo fai = new ForceAtomInfo(2, fae.getInc(), 5, 30,
                0, 0, Util.getCurrentTime(), 0, 0, new Position(0, 0));

        List<Mob> mobs = chr.getField().getMobsInRect(mob.getRectAround(rect));
        if (mobs.isEmpty()) {
            mobObjId = mob.getObjectId();
        } else {
            mobObjId = Util.getRandomFromCollection(mobs).getObjectId();
        }

        ArrayList<Integer> targetList = new ArrayList<>();
        targetList.add(chr.getId());

        final ForceAtom fa = new ForceAtom(true, chr.getId(), mobObjId, fae,
                true, targetList, TALISMAN_EVIL_SEALING_GOURD, new ArrayList<>(Collections.singleton(fai)),
                rect, 0, 0, new Position(), 0, new Position(), 0);
        chr.createForceAtom(fa);
    }

    private void doSageGodAssist() {
        if (!chr.hasSkill(SAGE_WRATH_OF_GODS)) {
            return;
        }
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (tsm.hasStat(CharacterTemporaryStat.AnimaThiefMetaphysics)) {
            Option o = new Option();
            int stack = tsm.getOption(CharacterTemporaryStat.AnimaThiefMetaphysics).xOption;
            if (stack < 12) {
                stack++;
            }
            if (stack == 12) {
                int randomInt = new Random().nextInt(2) + 10;
                chr.getField().broadcast(Summoned.assistAttackRequest(chr.getField().getSummonBySkillId(chr, SAGE_WRATH_OF_GODS), randomInt));
                stack = 0;
            }
            o.xOption = stack;
            tsm.sendStat(AnimaThiefMetaphysics, o);
            chr.addSkillCooldown(SAGE_WRATH_OF_GODS_ATTACK, 1500);
        }
    }

    private void doSageTigerSongyu() {
        if (!chr.hasSkill(SAGE_TIGER_OF_SONGYU)) {
            return;
        }
        Summon summon = chr.getField().getSummonBySkillId(chr, SAGE_TIGER_OF_SONGYU);
        if (summon != null) {
            if (this.sageTigerSongyuCount < 2) {
                this.sageTigerSongyuCount++;
            }
            if (this.sageTigerSongyuCount == 2) {
                chr.getField().broadcast(Summoned.specialAssistSkill(summon, SAGE_TIGER_OF_SONGYU_ATTACK));
                this.sageTigerSongyuCount = 0;
            }
            chr.addSkillCooldown(SAGE_TIGER_OF_SONGYU_ATTACK, 3000);
        }
    }

    @Override
    public void handleMobDamaged(Mob mob, long damage) {
        if (!swallowedMobs.empty() && chr.hasSkill(TALISMAN_EVIL_SEALING_GOURD) && isUsingGourdToss) {
            chr.write(UserPacket.effect(Effect.showTalismanSwallowEffect(TALISMAN_EVIL_SEALING_GOURD_EFFECT, chr.getLevel(),
                    chr.getSkillLevel(TALISMAN_EVIL_SEALING_GOURD), mob.getObjectId(), mob.getTemplateId())));

            MobTemporaryStat mts = swallowedMobs.pop().getTemporaryStat();
            mts.removeMobStat(mob, MobStat.MobLock);
            if (swallowedMobs.empty()) isUsingGourdToss = false;
        }
    }

    @Override
    public void handleJobAdvance() {
        ScriptManagerImpl sm = chr.getScriptManager();
        if (chr.getJob() == JobConstants.JobEnum.HOYOUNG_1.getJobId()) {
            if (chr.getLevel() < 30) {
                sm.sendSayOkay("#e這個職業需要達到#r30#k等以上才能轉職");
                return;
            }
            if (sm.sendAskYesNo("#e要跳過轉職任務嗎？")) {
                if (sm.getEmptyInventorySlots(1) < 1) {
                    sm.sendSayOkay("#e請在裝備欄空出更多空間。");
                    return;
                }
                sm.jobAdvance(JobConstants.JobEnum.HOYOUNG_2.getJobId());
            }
        } else if (chr.getJob() == JobConstants.JobEnum.HOYOUNG_2.getJobId()) {
            if (chr.getLevel() < 60) {
                sm.sendSayOkay("#e這個職業需要達到#r60#k等以上才能轉職");
                return;
            }
            if (sm.sendAskYesNo("#e要跳過轉職任務嗎？")) {
                if (sm.getEmptyInventorySlots(1) < 1) {
                    sm.sendSayOkay("#e請在裝備欄空出更多空間。");
                    return;
                }
                sm.jobAdvance(JobConstants.JobEnum.HOYOUNG_3.getJobId());
            }
        } else if (chr.getJob() == JobConstants.JobEnum.HOYOUNG_3.getJobId()) {
            if (chr.getLevel() < 100) {
                sm.sendSayOkay("#e這個職業需要達到#r100#k等以上才能轉職");
                return;
            }
            if (sm.sendAskYesNo("#e要跳過轉職任務嗎？")) {
                if (sm.getEmptyInventorySlots(1) < 1) {
                    sm.sendSayOkay("#e請在裝備欄空出更多空間。");
                    return;
                }
                sm.jobAdvance(JobConstants.JobEnum.HOYOUNG_4.getJobId());
            }
        } else {
            sm.sendSayOkay("#e目前的狀態無法轉職。");
        }
    }
}
