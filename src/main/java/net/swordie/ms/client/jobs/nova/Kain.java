package net.swordie.ms.client.jobs.nova;

import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.CharacterStat;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.client.character.skills.ExtraSkill;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.Skill;
import net.swordie.ms.client.character.skills.SkillStat;
import net.swordie.ms.client.character.skills.info.AttackInfo;
import net.swordie.ms.client.character.skills.info.MobAttackInfo;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.client.character.skills.info.SkillUseInfo;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.client.jobs.Job;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.UserLocal;
import net.swordie.ms.connection.packet.WvsContext;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.enums.Stat;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.loaders.ItemData;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.util.Rect;

import java.util.*;

import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;

public class Kain extends Job {

    // TODO [4] Handle Grappling Wire local&remote effect
    // TODO [5] Handle Link Skill & Union (If needs handling)

    // Thanatos Descent Rect
    public static final Rect thanatosRect = new Rect(-200, -250, 200, -30);
    public static final Rect remainIncenseRect = new Rect(-700, -400, 700, 450);

    // Beginner Job
    public static final int PRIOR_PREPARATION = 60030241;

    // 1st Job
    public static final int HITMAN = 63000007; // Passive
    public static final int COVERT = 63000008; // Passive
    public static final int STRIKE_ARROW = 63001000;
    public static final int STRIKE_ARROW_EXTRA_HIT = 63001001;
    public static final int SHADOW_STEP_HORIZONTAL = 63001002;
    public static final int SHADOW_STEP_VERTICAL_UP = 63001003; // Up
    public static final int GRAPPLING_WIRE = 63001004;
    public static final int SHADOW_STEP_VERTICAL_DOWN = 63001005; // Down
    public static final int SHADOW_SWIFT = 63001006; // Flash Jump
    public static final int STRIKE_ARROW_POSSESS = 63001100; // Possess Skill


    // 2nd Job
    public static final int STRIKE_ARROW_2_PASSIVE = 63100002; // Upgrade on StrikeArrow
    public static final int BREATH_SHOOTER_MASTERY = 63100011; // Passive Mastery
    public static final int PHYSICAL_TRAINING = 63100012; // Passive
    public static final int STRIKE_ARROW_POSSESS_PASSIVE = 63100100; // Upgrade on StrikeArrowPossess? (disable=1)
    public static final int SCATTERING_SHOT_POSSESS_PASSIVE = 63101001; // Upgrade on ScatteringShotPossess? (disable=1)
    public static final int POSSESSION = 63101001; // Kain Energy/Gauge Skill
    public static final int STRIKE_ARROW_2 = 63101003; // Upgrade on StrikeArrow. Added Sequential Attack after StrikeArrow
    public static final int SCATTERING_SHOT = 63101004; // TODO [2] Handle Stack Inc Request
    public static final int DRAGON_FANG = 63101005; // Buff that shows 3 Orbs around character (So new CTS, most likely)
    public static final int DRAGON_FANG_SA = 63101006; // Second Atom!
    public static final int BREATH_SHOOTER_BOOSTER = 63101010; // Booster Buff
    public static final int STRIKE_ARROW_2_POSSESS = 63101100; // Possess Skill
    public static final int SCATTERING_SHOT_POSSESS = 63101104; // Possess Skill & Second Atom


    // 3rd Job
    public static final int STRIKE_ARROW_3_PASSIVE = 63110001; // Upgrade on StrikeArrow2
    public static final int DEATH_BLESSING = 63110011; // Passive Skill that places MobStat on Mobs when attacking with Possessed Skills
    public static final int NATURAL_BORN_INSTINCT = 63110014; // Passive
    public static final int GRINDING = 63110015;
    public static final int SHAFT_BREAK_PASSIVE = 63110103; // Upgrade? (disable=1)
    public static final int STRIKE_ARROW_3 = 63111002; // Upgrade on StrikeArrow2. Added Sequential Attack after StrikeArrow2
    public static final int SHAFT_BREAK = 63111003; // Shoot Obj
    public static final int SHAFT_BREAK_VACUUM = 63111004; // Shoot Obj
    public static final int SHAFT_BREAK_EXPLOSION = 63111005; // Shoot Obj
    public static final int TEARING_KNIFE = 63111007; // Execution Skill
    public static final int PHANTOM_BLADE = 63111008; // Execution Skill
    public static final int REMAIN_INCENSE = 63111009; // ON/OFF buff
    public static final int REMAIN_INCENSE_WRECKAGE = 63111010; // Wreckage
    public static final int DEATH_BLESSING_ATTACK = 63111012;
    public static final int DEATH_BLESSING_BUFF = 63111013; // skillList = placeMobDebuff | skillList2 = explodeMobDebuff
    public static final int SHAFT_BREAK_POSSESS = 63111103;
    public static final int SHAFT_BREAK_VACUUM_POSSESS = 63111104;
    public static final int SHAFT_BREAK_EXPLOSION_POSSESS = 63111105;
    public static final int SHAFT_BREAK_AFTER_EFFECT_POSSESS = 63111106;


    // 4th Job
    public static final int POSSESSION_2 = 63120000; // Upgrade on Possession
    public static final int ADVANCED_DEATH_BLESSING = 63120001; // Upgrade on Death Blessing
    public static final int GRINDING_2 = 63120011; // Upgrade on Grinding
    public static final int DOGMA = 63120012; // Passive
    public static final int BREATH_SHOOTER_EXPERT = 63120013; // Passive
    public static final int ADAPT_TO_DEATH = 63120014; // Passive
    public static final int FALLING_DUST_PASSIVE = 63120102; // Upgrade? (disable=1)
    public static final int FALLING_DUST = 63121002; // Attack
    public static final int CHAIN_SICKLE = 63121004; // Execution
    public static final int CHAIN_SICKLE_2 = 63121005; // Execution | Additional Attack after ChainSickle
    public static final int POISON_NEEDLE = 63121006; // Execution | Inflicts BurnedInfo | KeyDown
    public static final int POISON_NEEDLE_2 = 63121007; // Execution | Inflicts BurnedInfo
    public static final int DRAGON_SCALE = 63121008; // iFrames | KeyDown
    public static final int NOVA_WARRIOR = 63121009;
    public static final int NOVA_HERO_WILL = 63121010;
    public static final int FALLING_DUST_POSSESS = 63121102; // Possess
    public static final int FALLING_DUST_2_POSSESS = 63121103;


    // Hyper Skills
    public static final int DRAGON_SCALE_EXTRA_HEALING = 63120039;

    public static final int SNEAKY_SNIPING_POSSES_PASSIVE = 63120140; // Upgrade? (disable=1)

    public static final int SNEAKY_SNIPING = 63121040; // Keydown!
    public static final int SNEAKY_SNIPING_2 = 63121041; // Keydown!
    public static final int CHASING_SHOT = 63121042;
    public static final int INCARNATION = 63121044; // Buff
    public static final int SNEAKY_SNIPING_POSSESS = 63121140;
    public static final int SNEAKY_SNIPING_2_POSSESS = 63121141;

    // V skills
    public static final int DRAGON_BURST = 400031061; // Possess | Keydown!
    public static final int THANATOS_DESCENT = 400031062; // Buff
    public static final int THANATOS_DESCENT_2 = 400031063; // Second Atom
    public static final int THANATOS_DESCENT_3 = 400031064; // Screen Attack
    public static final int FATAL_BLITZ = 400031065; // Execution
    public static final int GRIP_OF_AGONY = 400031066; // Second Atom


    public static final int[] possessionSkills = new int[] {
            STRIKE_ARROW_POSSESS,
            STRIKE_ARROW_2_POSSESS,
            SCATTERING_SHOT_POSSESS,
            SHAFT_BREAK_POSSESS,
            FALLING_DUST_POSSESS,

            DRAGON_BURST,
    };

    public static final int[] executionSkills = new int[] {
            TEARING_KNIFE,
            PHANTOM_BLADE,
            CHAIN_SICKLE,
            POISON_NEEDLE_2,

            SNEAKY_SNIPING_POSSESS,
            SNEAKY_SNIPING_2_POSSESS,
            FATAL_BLITZ,
    };

    public static final int[] whisperShotSkills = new int[]{
            STRIKE_ARROW,
            STRIKE_ARROW_2,
            SCATTERING_SHOT,
            STRIKE_ARROW_3,
            SHAFT_BREAK,
            FALLING_DUST,
    };

    public static final int[] addedSkills = new int[] {
            PRIOR_PREPARATION
    };

    private Map<Integer, Integer> kainStackSkills = new HashMap<>(); // <SkillId, CurStack>
    private Map<Integer, Integer> deathBlessingStacks = new HashMap<>(); // <MobObjectId, Stack>
    private int advDeathBlessingKillCount = 0;

    private static boolean contains(int[] skills, int skillID) {
        for (int skill : skills) {
            if (skill == skillID) {
                return true;
            }
        }
        return false;
    }

    private static List<Integer> getHitMobIds(AttackInfo attackInfo) {
        List<Integer> mobIds = new ArrayList<>();
        for (MobAttackInfo mai : attackInfo.mobAttackInfo) {
            mobIds.add(mai.mobId);
        }
        return mobIds;
    }

    private boolean isPossessionAttack(int skillID) {
        return contains(possessionSkills, skillID);
    }

    private boolean isExecutionAttack(int skillID) {
        return contains(executionSkills, skillID);
    }

    private boolean isWhispershotAttack(int skillID) {
        return contains(whisperShotSkills, skillID);
    }

    private int getPossessionSkillId() {
        return chr.hasSkill(POSSESSION_2) ? POSSESSION_2 : POSSESSION;
    }

    private void updatePossessionGauge(int delta) {
        if (chr == null || !isHandlerOfJob(chr.getJob())) {
            return;
        }
        SkillInfo si = SkillData.getSkillInfoById(getPossessionSkillId());
        int slv = Math.max(1, chr.getSkillLevel(getPossessionSkillId()));
        int max = 300;
        if (si != null) {
            int v = si.getValue(SkillStat.v, slv);
            if (v > 0) {
                max = v * 100 + 99;
            }
        }
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o = tsm.getOption(NADragonGauge);
        o.nOption = Math.max(0, Math.min(max, o.nOption + delta));
        o.rOption = 6003;
        tsm.sendStat(NADragonGauge, o);
        chr.write(WvsContext.updateSkillStackRequestResult(getPossessionSkillId(), (byte) Math.min(127, o.nOption)));
    }

    private void registerExtraAttack(int triggerSkillID, int extraSkillID, List<Integer> mobIds, int delay) {
        if (mobIds.isEmpty()) {
            return;
        }
        ExtraSkill extraSkill = new ExtraSkill(extraSkillID, chr.getPosition());
        extraSkill.FaceLeft = chr.isLeft() ? 1 : 0;
        extraSkill.Delay = delay;
        extraSkill.Value = 1;
        extraSkill.MobOIDs.addAll(mobIds);
        chr.write(UserLocal.registerExtraSkill(triggerSkillID, Collections.singletonList(extraSkill)));
    }

    private void handleWhispershotExtra(AttackInfo attackInfo) {
        int skillID = attackInfo.skillId;
        int extraSkillID = switch (skillID) {
            case STRIKE_ARROW, STRIKE_ARROW_2, STRIKE_ARROW_3 -> STRIKE_ARROW_EXTRA_HIT;
            default -> 0;
        };
        if (extraSkillID != 0) {
            registerExtraAttack(skillID, extraSkillID, getHitMobIds(attackInfo), 0);
        }
    }

    private void handleDeathBlessing(AttackInfo attackInfo) {
        List<Integer> mobIds = getHitMobIds(attackInfo);
        int skillID = attackInfo.skillId;
        if (isPossessionAttack(skillID)) {
            for (int mobId : mobIds) {
                deathBlessingStacks.merge(mobId, 1, (oldValue, one) -> Math.min(15, oldValue + one));
            }
            return;
        }
        if (!isExecutionAttack(skillID)) {
            return;
        }
        List<Integer> triggered = new ArrayList<>();
        for (int mobId : mobIds) {
            int stack = deathBlessingStacks.getOrDefault(mobId, 0);
            if (stack <= 0) {
                continue;
            }
            triggered.add(mobId);
            if (stack == 1) {
                deathBlessingStacks.remove(mobId);
            } else {
                deathBlessingStacks.put(mobId, stack - 1);
            }
        }
        if (!triggered.isEmpty()) {
            chr.write(UserLocal.userBonusAttackRequest(DEATH_BLESSING_ATTACK, triggered));
            TemporaryStatManager tsm = chr.getTemporaryStatManager();
            Option o = new Option();
            o.nOption = chr.hasSkill(ADVANCED_DEATH_BLESSING) ? 2 : 1;
            o.rOption = DEATH_BLESSING_BUFF;
            o.tOption = 5;
            tsm.sendStat(NADeathBlessing, o);
            updatePossessionGauge(chr.hasSkill(ADVANCED_DEATH_BLESSING) ? 20 : 10);
        }
    }

    private void handleRemainIncense(AttackInfo attackInfo) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (!tsm.hasStat(NARemainIncense) || attackInfo.skillId == REMAIN_INCENSE_WRECKAGE) {
            return;
        }
        SkillInfo si = SkillData.getSkillInfoById(REMAIN_INCENSE);
        if (si != null && si.getSkillList2().contains(attackInfo.skillId)) {
            chr.write(UserLocal.userBonusAttackRequest(REMAIN_INCENSE_WRECKAGE, getHitMobIds(attackInfo)));
        }
    }

    private void handleThanatosDescent(AttackInfo attackInfo) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (!tsm.hasStat(NAThanatosDescent) || attackInfo.skillId == THANATOS_DESCENT_2 || chr.hasSkillOnCooldown(THANATOS_DESCENT_2)) {
            return;
        }
        List<Integer> mobIds = getHitMobIds(attackInfo);
        if (!mobIds.isEmpty()) {
            chr.write(UserLocal.userBonusAttackRequest(THANATOS_DESCENT_2, mobIds));
            chr.addSkillCooldown(THANATOS_DESCENT_2, 1000);
        }
    }

    public Kain(Char chr) {
        super(chr);
        if (isHandlerOfJob(chr.getJob())) {
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

    @Override
    public boolean isHandlerOfJob(short id) {
        return JobConstants.isKain(id);
    }

    @Override
    public void handleAttack(Client c, AttackInfo attackInfo, SkillInfo si, long now) {
        super.handleAttack(c, attackInfo, si, now);
        if (attackInfo.mobAttackInfo.isEmpty()) {
            return;
        }
        int skillID = attackInfo.skillId;
        if (isWhispershotAttack(skillID)) {
            handleWhispershotExtra(attackInfo);
        }
        if (isPossessionAttack(skillID)) {
            updatePossessionGauge(-100);
        } else {
            updatePossessionGauge(20);
        }
        handleDeathBlessing(attackInfo);
        handleRemainIncense(attackInfo);
        handleThanatosDescent(attackInfo);
    }

    @Override
    public void handleSkill(Client c, InPacket inPacket, SkillUseInfo skillUseInfo) {
        super.handleSkill(c, inPacket, skillUseInfo);
        SkillInfo si = skillUseInfo.skillInfo;
        int slv = skillUseInfo.slv;
        int skillID = skillUseInfo.skillID;
        if (si == null) {
            return;
        }
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o1 = new Option();
        Option o2 = new Option();
        switch (skillID) {
            case POSSESSION:
            case POSSESSION_2:
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = Math.max(1, si.getValue(SkillStat.time, slv));
                tsm.sendStat(NADragonEnchant, o1);
                break;
            case BREATH_SHOOTER_BOOSTER:
                o1.nOption = si.getValue(SkillStat.x, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(SkillStat.time, slv);
                tsm.sendStat(Booster, o1);
                break;
            case DRAGON_FANG:
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.xOption = si.getValue(SkillStat.x, slv);
                o1.yOption = si.getValue(SkillStat.y, slv);
                tsm.sendStat(NABrutalPang, o1);
                break;
            case SHADOW_STEP_HORIZONTAL:
            case SHADOW_STEP_VERTICAL_UP:
            case SHADOW_STEP_VERTICAL_DOWN:
                o1.nOption = slv;
                o1.rOption = SHADOW_STEP_HORIZONTAL;
                o1.tOption = Math.max(1, si.getValue(SkillStat.subTime, slv) / 1000);
                tsm.sendStat(DarkSight, o1);
                break;
            case REMAIN_INCENSE:
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = 2100000000;
                tsm.sendStat(NARemainIncense, o1);
                break;
            case DRAGON_SCALE:
                o1.nValue = 1;
                o1.nReason = skillID;
                o1.tTerm = Math.max(1, si.getValue(SkillStat.y, slv) / 1000);
                tsm.sendStat(IndieNotDamaged, o1);
                break;
            case INCARNATION:
                o1.nValue = si.getValue(SkillStat.indieDamR, slv);
                o1.nReason = skillID;
                o1.tTerm = si.getValue(SkillStat.time, slv);
                tsm.sendStat(IndieDamR, o1);
                o2.nValue = si.getValue(SkillStat.indieStance, slv);
                o2.nReason = skillID;
                o2.tTerm = si.getValue(SkillStat.time, slv);
                tsm.sendStat(IndieStance, o2);
                break;
            case THANATOS_DESCENT:
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(SkillStat.time, slv);
                o1.xOption = si.getValue(SkillStat.x, slv);
                tsm.sendStat(NAThanatosDescent, o1);
                o2.nValue = si.getValue(SkillStat.indieDamR, slv);
                o2.nReason = skillID;
                o2.tTerm = si.getValue(SkillStat.time, slv);
                tsm.sendStat(IndieDamR, o2);
                break;
        }
    }

    @Override
    public void setCharCreationStats(Char chr) {
        super.setCharCreationStats(chr);
        CharacterStat cs = chr.getAvatarData().getCharacterStat();
        cs.setPosMap(410000401);
        cs.setLevel(10);
        chr.setJob(JobConstants.JobEnum.KAIN_1.getJobId());
        chr.addSpToSpecificJob((short) JobConstants.JobEnum.KAIN_1.getJobId(), 5);
        cs.setStr(5);
        cs.setDex(5);
        cs.setInt(5);
        cs.setLuk(5);
        cs.setAp(4 + cs.getLevel() * 5);
        cs.setHp(450);
        cs.setMaxHp(450);
        cs.setMp(300);
        cs.setMaxMp(300);
    }

    @Override
    public void handleLevelUp(short level) {
        super.handleLevelUp(level);
        if (level == 10) {
            chr.setJob(JobConstants.JobEnum.KAIN_1.getJobId());
            chr.addSpToSpecificJob((short) JobConstants.JobEnum.KAIN_1.getJobId(), 5);
            chr.addStatAndSendPacket(Stat.ap, 5);
        } else if (level == 30) {
            chr.setJob(JobConstants.JobEnum.KAIN_2.getJobId());
            chr.addSpToSpecificJob((short) JobConstants.JobEnum.KAIN_2.getJobId(), 4);
            chr.addStatAndSendPacket(Stat.ap, 4);
        } else if (level == 60) {
            chr.setJob(JobConstants.JobEnum.KAIN_3.getJobId());
            chr.addSpToSpecificJob((short) JobConstants.JobEnum.KAIN_3.getJobId(), 4);
            chr.addStatAndSendPacket(Stat.ap, 5);
        } else if (level == 100) {
            chr.setJob(JobConstants.JobEnum.KAIN_4.getJobId());
            chr.addSpToSpecificJob((short) JobConstants.JobEnum.KAIN_4.getJobId(), 3);
            chr.addStatAndSendPacket(Stat.ap, 5);
        }
    }
}
