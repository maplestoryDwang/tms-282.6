package net.swordie.ms.client.jobs.anima;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.CharacterStat;
import net.swordie.ms.client.character.info.HitInfo;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.client.character.quest.Quest;
import net.swordie.ms.client.character.skills.*;
import net.swordie.ms.client.character.skills.info.*;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.client.jobs.Job;
import net.swordie.ms.client.jobs.JobManager;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.UserLocal;
import net.swordie.ms.connection.packet.UserPacket;
import net.swordie.ms.connection.packet.WvsContext;
import net.swordie.ms.constants.FieldConstants;
import net.swordie.ms.constants.GameConstants;
import net.swordie.ms.constants.ItemConstants;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.constants.QuestConstants;
import net.swordie.ms.enums.AssistType;
import net.swordie.ms.enums.LeaveType;
import net.swordie.ms.enums.MoveAbility;
import net.swordie.ms.life.AffectedArea;
import net.swordie.ms.life.Summon;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.life.mob.MobStat;
import net.swordie.ms.life.mob.MobTemporaryStat;
import net.swordie.ms.loaders.ItemData;
import net.swordie.ms.loaders.QuestData;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.scripts.ScriptManagerImpl;
import net.swordie.ms.util.Position;
import net.swordie.ms.util.Rect;
import net.swordie.ms.util.Util;
import net.swordie.ms.world.field.Field;

import java.util.*;

import static net.swordie.ms.client.character.skills.SkillStat.*;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;
import static net.swordie.ms.enums.InvType.EQUIPPED;

public class Ren extends Job {

    // Beginner Skills (Anima Warrior)
    public static final int RETURN_TO_SERPENT_HEARTWATERS = 160021074;
    public static final int SPIRIT_AFFINITY = 160020000; // Passive
    public static final int GROUNDED_BODY = 160020001; // Passive
    public static final int EXCLUSIVE_SPELL = 160021005; // MaxLevelBuff nOption 4
    public static final int SHAPESHIFT = 160021075;

    // 1st Job
    public static final int PLUM_BLOSSOM_SWORD_SLICE = 164001000;

    // 2nd Job
    public static final int BLOSSOMING_BLADE = 161101005; // Booster

    // 3rd Job
    public static final int SECOND_PLUM_BLOSSOM_SWORD_RAINING_BLOSSOMS = 161110001; // REGISTER_EXTRA_SKILL
    public static final int SECOND_PLUM_BLOSSOM_SWORD_RAINING_BLOSSOMS_ATOM = 161111002;
    public static final int FIRST_IMUGI_SPIRIT_SWORD_SERPENT_BLESSING = 161110005;
    // Contained stat IndieAsrR (29)
    // Contained stat IndieCheckTimeByClient (68)
    public static final int FIRST_IMUGI_SPIRIT_SWORD_BURROWING_EARTH = 161111007; // invisible
    public static final int FIRST_IMUGI_SPIRIT_SWORD_BURROWING_EARTH_EFF = 161111008; // invisible
    public static final int FIRST_IMUGI_SPIRIT_SWORD_BURROWING_EARTH_2 = 161111011;

    // 4th Job
    public static final int SECOND_IMUGI_SPIRIT_SWORD_SERPENT_FANG = 161120006; // Final Attack
    public static final int FINAL_IMUGI_SPIRIT_SWORD_RAVENOUS_SPIRIT = 161120003;
    public static final int FINAL_IMUGI_SPIRIT_SWORD_RAVENOUS_SPIRIT_EFF = 161121004;
    public static final int FINAL_IMUGI_SPIRIT_SWORD_YEARS_UNCOUNTED = 161121043;
    public static final int FINAL_IMUGI_SPIRIT_SWORD_YEARS_UNCOUNTED_DEBUFF = 161121044;
    public static final int HEXA_SECOND_IMUGI_SPIRIT_SWORD_SERPENT_FANG = 161140010; // Final Attack
    public static final int ANIMA_WARRIOR = 161121009;
    public static final int ANIMA_HERO_WILL = 161121010;

    public static final int PLUM_BLOSSOM_SWORD_STORM = 161121000; // KeyDownMoving nOption = 1 hasMovingAffectingStat 17
    // RenPlumSwordForm3Shoot nOption = 2 ~ 25, 26, 27, 28 hasMovingAffectingStat 0 | SHOOT_OBJECT_CREATED
    // RenPlumSwordForm3Stack nOption = 1
    public static final int PLUM_BLOSSOM_SWORD_UNBOWED_BLADE = 161121008; // IndieNotDamaged nValue = 1 (3 giây)

    public static final int RIOTOUS_HEART = 161121005; // Shoot Object
    public static final int HEARTS_UNITED = 161121014; // Shoot Object

    public static final int SUBLIMATION = 161121041; // Sublimation nValue = 3
    public static final int PLUM_BRANCH_SNARE = 161121042; // Plum Branch Snare

    // V Skills
    public static final int THOUSAND_BLOSSOM_FLURRY = 400011147;
    public static final int THOUSAND_BLOSSOM_FLURRY_EX = 400011148; // RenPlumSwordEx
    public static final int THOUSAND_BLOSSOM_FLURRY_ATOM = 400011149; // RenPlumSwordEx
    public static final int SOUL_IMMEASURABLE = 400011150; // Soul Immeasurable
    public static final int SOUL_IMMEASURABLE_ATT = 400011151; // Soul Immeasurable
    public static final int SOUL_IMMEASURABLE_SPE = 400011152; // Soul Immeasurable
    public static final int DANCING_ANNIHILATION = 400011153;
    public static final int FINAL_IMUGI_SPIRIT_SWORD_BLADE_OF_THE_UNBOUND_HEART = 400011154;
    public static final int FINAL_IMUGI_SPIRIT_SWORD_BLADE_OF_THE_UNBOUND_HEART_ATT = 400011157; // Skill Activation

    // HEXA skills
    public static final int HEXA_FINAL_IMUGI_SPIRIT_SWORD_YEARS_UNCOUNTED = 161141004;
    public static final int HEXA_FINAL_IMUGI_SPIRIT_SWORD_YEARS_UNCOUNTED_DEBUFF = 161141012;
    public static final int HEXA_SECOND_PLUM_BLOSSOM_SWORD_RAINING_BLOSSOMS = 161140007; // REGISTER_EXTRA_SKILL
    public static final int HEXA_SECOND_PLUM_BLOSSOM_SWORD_RAINING_BLOSSOMS_ATOM = 161141008;
    public static final int HEXA_RIOTOUS_HEART = 161141009; // Shoot Object
    public static final int HEXA_HEARTS_UNITED = 161141013; // Shoot Object
    // Sixth job (HEXA) — H1 origin / H2 ascent
    public static final int HEARTBOUND_VERSE = 161141506;
    public static final int HEARTBOUND_VERSE_FALLING_FLOWER = 161141502;
    public static final int HEARTBOUND_VERSE_CLIMBING_SERPENT = 161141503;

    private static final int[] addedSkills = new int[]{
            RETURN_TO_SERPENT_HEARTWATERS,
            SPIRIT_AFFINITY,
            SHAPESHIFT,
            EXCLUSIVE_SPELL,
            GROUNDED_BODY,
    };
    public int soulImmeasuableStack = 0;

    public Ren(Char chr) {
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

    @Override
    public boolean isHandlerOfJob(short id) {
        return JobConstants.isRen(id);
    }

    // Attack related methods ------------------------------------------------------------------------------------------
    @Override
    public void handleDebuffOnMob(Client c, Mob mob, SkillInfo si, int skillID, int slv, long damage) {
        Char chr = c.getChr();
        Option o1 = new Option();
        MobTemporaryStat mts = mob.getTemporaryStat();
        switch (skillID) {
            case PLUM_BRANCH_SNARE:
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    o1.nOption = 1;
                    o1.rOption = skillID;
                    o1.tOption = si.getValue(time, slv);
                    mts.addStatOptions(mob, MobStat.Stun, o1);
                }
                break;
            case FINAL_IMUGI_SPIRIT_SWORD_YEARS_UNCOUNTED:
            case HEXA_FINAL_IMUGI_SPIRIT_SWORD_YEARS_UNCOUNTED:
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    List<Option> options = new ArrayList<>();
                    o1.nOption = 1;
                    o1.rOption = skillID;
                    o1.tOption = 5;
                    Option ox = new Option();
                    ox.nOption = 1;
                    ox.cOption = chr.getId();
                    ox.rOption = skillID;
                    ox.mOption = skillID + 1;
                    ox.xOption = mob.getPosition().getX();
                    ox.yOption = mob.getPosition().getY();
                    ox.zOption = mob.getPosition().getY() + 10;
                    ox.tOption = 5000; // 5 giây
                    ox.uOption = 10;
                    options.add(ox);
                    o1.extraOpts = new ArrayList<>(options);
                    mts.addStatOptions(mob, MobStat.NewBurnedInfo, o1);
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
        if (hasHitMobs) {
            if (skillID != SECOND_PLUM_BLOSSOM_SWORD_RAINING_BLOSSOMS_ATOM
                    && skillID != HEXA_SECOND_PLUM_BLOSSOM_SWORD_RAINING_BLOSSOMS_ATOM
                    && skillID != THOUSAND_BLOSSOM_FLURRY_EX
                    && skillID != THOUSAND_BLOSSOM_FLURRY_ATOM
                    && skillID != SOUL_IMMEASURABLE_ATT
                    && skillID != SOUL_IMMEASURABLE_SPE) {
                serpentBlessing();
                rainingBlossoms();
                thousandBlossomsFlurry(attackInfo);
                soulImmeasuable(attackInfo);
            }
        }
        Option o1 = new Option();
        chr.write(WvsContext.renImugiSpiritSwordRequest(skillID));
        switch (attackInfo.skillId) {
            case PLUM_BLOSSOM_SWORD_STORM:
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(SkillStat.time, slv);
                o1.xOption = 1;
                tsm.sendStat(KeyDownMoving, o1);
                break;
            case PLUM_BLOSSOM_SWORD_UNBOWED_BLADE:
                o1.nValue = 1;
                o1.nReason = skillID;
                o1.tOption = 3;
                tsm.sendStat(IndieNotDamaged, o1);
                break;
            case RIOTOUS_HEART:
            case HEXA_RIOTOUS_HEART:
                chr.setSkillCooldown(skillID, 10000);
                break;
            case HEARTS_UNITED:
            case HEXA_HEARTS_UNITED:
                chr.addSkillCooldown(skillID, 30000);
                break;
        }
    }

    private void soulImmeasuable(AttackInfo attackInfo) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        int skillID = SOUL_IMMEASURABLE;
        if (tsm.hasStatBySkillId(skillID)) {
            int slv = chr.getSkillLevel(skillID);
            SkillInfo si = SkillData.getSkillInfoById(skillID);
            int limit = si.getValue(u2, slv);
            boolean tms282 = net.swordie.ms.connection.packet.Tms282RemoteBuffs.active();
            if (this.soulImmeasuableStack < limit) {
                Option o2 = new Option();
                this.soulImmeasuableStack += attackInfo.mobCount;
                if (tms282) this.soulImmeasuableStack = Math.min(limit, this.soulImmeasuableStack);
                o2.nOption = this.soulImmeasuableStack;
                o2.rOption = skillID;
                o2.tOption = si.getValue(time, slv);
                tsm.sendStat(SpiritAwakeningStack, o2);
                chr.write(UserLocal.userBonusAttackRequest(SOUL_IMMEASURABLE_ATT));
                if (this.soulImmeasuableStack == (tms282 ? limit : 90)) {
                    chr.write(UserLocal.userBonusAttackRequest(SOUL_IMMEASURABLE_SPE));
                }
            }
        }
    }

    private void rainingBlossoms() {
        if (chr.hasSkill(SECOND_PLUM_BLOSSOM_SWORD_RAINING_BLOSSOMS)) {

            int originalSkillID = chr.hasSkill(HEXA_SECOND_PLUM_BLOSSOM_SWORD_RAINING_BLOSSOMS) ?
                    HEXA_SECOND_PLUM_BLOSSOM_SWORD_RAINING_BLOSSOMS
                    : SECOND_PLUM_BLOSSOM_SWORD_RAINING_BLOSSOMS;

            int atomSkillID = chr.hasSkill(originalSkillID) ?
                    HEXA_SECOND_PLUM_BLOSSOM_SWORD_RAINING_BLOSSOMS_ATOM
                    : SECOND_PLUM_BLOSSOM_SWORD_RAINING_BLOSSOMS_ATOM;

            var si = SkillData.getSkillInfoById(atomSkillID);
            if (Util.succeedProp(35)) {
                int bulletCount = si.getValue(SkillStat.bulletCount, chr.getSkillLevel(originalSkillID));
                List<SecondAtom> secondAtoms = new LinkedList<>();
                Rect rect = chr.getRectAround(new Rect(-500, -500, 500, 500));
                if (!chr.isLeft()) rect = rect.horizontalFlipAround(chr.getPosition().getX());
                var sai = si.getSecondAtomInfos().get(0);
                if (sai == null) return;
                final var pos = chr.getPosition();
                final var mobs = chr.getField().getMobsInRect(rect);
                final int dataIndex = atomSkillID == SECOND_PLUM_BLOSSOM_SWORD_RAINING_BLOSSOMS_ATOM
                        ? (Util.succeedProp(50) ? 105 : 117) : (Util.succeedProp(50) ? 108 : 118);
                final long start = System.currentTimeMillis();
                for (int i = 0; i < bulletCount; i++) {
                    var mob = Util.getRandomFromCollection(mobs);
                    if (mob == null) continue;
                    SecondAtom fa = new SecondAtom(sai, chr.getNewSecondAtomKey(), chr.getId(), mob != null ? mob.getObjectId() : 0, i,
                            si.getSkillId(), pos, start);
                    fa.setDataIndex(dataIndex);
                    fa.setCustomrotate(360);
                    fa.setCustoms(sai.getCustoms());
                    fa.setUnk(2);
                    secondAtoms.add(fa);
                }
                if (!secondAtoms.isEmpty()) {
                    chr.createSecondAtom(secondAtoms);
                    Option o1 = new Option();
                    o1.nValue = 1;
                    o1.nReason = originalSkillID;
                    o1.tTerm = 2;
                    chr.getTemporaryStatManager().sendStat(IndieBuffIcon, o1);
                }
            }
        }
    }

    private void thousandBlossomsFlurry(AttackInfo attackInfo) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (tsm.hasStat(RenPlumSwordEx)) {
            int count = 0;
            List<Integer> mobList = new ArrayList<>();
            for (MobAttackInfo mai : attackInfo.mobAttackInfo) {
                Mob mob = (Mob) chr.getField().getLifeByObjectID(mai.mobId);
                if (mob == null || mob.getHp() <= 0) {
                    continue;
                }
                mobList.add(mob.getObjectId());
            }
            chr.write(UserLocal.userBonusAttackRequest(THOUSAND_BLOSSOM_FLURRY_EX, mobList, attackInfo.skillId));
            if (chr.getField().getSummonBySkillId(chr, THOUSAND_BLOSSOM_FLURRY) != null) {
                Option o1 = tsm.getOption(RenPlumSwordEx);
                o1.nOption += 1;
                count = o1.nOption;
                o1.rOption = THOUSAND_BLOSSOM_FLURRY_EX;
                o1.yOption = Util.succeedProp(10) ? 2 : Util.succeedProp(30) ? 1 : 0;
                tsm.sendStat(RenPlumSwordEx, o1);
            }
            if (count == 30 || count == 70 || count == 125) {
                int bulletCount = count - 10;
                SkillInfo si = SkillData.getSkillInfoById(THOUSAND_BLOSSOM_FLURRY_ATOM);
                List<SecondAtom> secondAtoms = new LinkedList<>();
                Rect rect = chr.getRectAround(new Rect(-1000, -1000, 1000, 1000));
                if (!chr.isLeft()) rect = rect.horizontalFlipAround(chr.getPosition().getX());
                final var mobs = chr.getField().getMobsInRect(rect);
                if (mobs.isEmpty()) return;
                var sai = si.getSecondAtomInfos().getOrDefault(0, null);
                if (sai != null) {
                    final int randX = chr.getPosition().getX() + Util.getRandom(-200, 200);
                    final int randY = chr.getPosition().getY() - Util.getRandom(100, 200);
                    final var pos = new Position(randX, randY);
                    final long start = System.currentTimeMillis();
                    for (int i = 0; i < bulletCount; i++) {
                        var mob = Util.getRandomFromCollection(mobs);
                        if (mob == null) continue;
                        SecondAtom fa = new SecondAtom(sai, chr.getNewSecondAtomKey(), chr.getId(), mob != null ? mob.getObjectId() : 0,
                                i, si.getSkillId(), pos, start);
                        secondAtoms.add(fa);
                    }
                    chr.createSecondAtom(secondAtoms);
                }
            }
        }
    }

    private void serpentBlessing() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (chr.hasSkill(FIRST_IMUGI_SPIRIT_SWORD_SERPENT_BLESSING) && Util.succeedProp(35)
                && !tsm.hasStatBySkillId(FIRST_IMUGI_SPIRIT_SWORD_SERPENT_BLESSING)) {
            EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
            Option o1 = new Option();
            Option o2 = new Option();
            o1.nValue = 100;
            o1.nReason = FIRST_IMUGI_SPIRIT_SWORD_SERPENT_BLESSING;
            o1.tTerm = 4;
            newStats.put(IndieAsrR, o1);
            o2.nValue = 1;
            o2.nReason = FIRST_IMUGI_SPIRIT_SWORD_SERPENT_BLESSING;
            o2.tTerm = 4;
            newStats.put(IndieCheckTimeByClient, o2);
            tsm.sendStat(newStats);
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
        Field field = chr.getField();
        EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Summon summon;
        switch (skillID) {
            case RETURN_TO_SERPENT_HEARTWATERS:
                Field toField = chr.getOrCreateFieldByCurrentInstanceType(si.getValue(x, slv));
                chr.warp(toField);
                break;
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
            case EXCLUSIVE_SPELL:
                o1.nOption = si.getValue(SkillStat.x, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(SkillStat.time, slv);
                tsm.sendStat(MaxLevelBuff, o1);
                break;
            case BLOSSOMING_BLADE:
                o1.nOption = si.getValue(SkillStat.x, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(SkillStat.time, slv);
                tsm.sendStat(Booster, o1);
                break;
            case SUBLIMATION:
                o1.nOption = si.getValue(SkillStat.w, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(SkillStat.time, slv);
                tsm.sendStat(Sublimation, o1);
                break;
            case RIOTOUS_HEART:
            case HEXA_RIOTOUS_HEART:
                o1.nOption = tsm.hasStat(RenPlumSwordForm3Stack) ? (int) (tsm.getTotalNOptionOfStat(RenPlumSwordForm3Stack) - 1) : 0;
                o1.rOption = skillID;
                tsm.sendStat(RenPlumSwordForm3Stack, o1);
                o2.nOption = si.getValue(x, slv);
                o2.rOption = skillID;
                tsm.sendStat(RenPlumSwordForm3Shoot, o2);
                break;
            case ANIMA_HERO_WILL:
                tsm.removeAllDebuffs();
                break;
            case THOUSAND_BLOSSOM_FLURRY:
                summon = Summon.getSummonByAndSetStat(chr, skillID, slv);
                summon.setMoveAbility(MoveAbility.Stop);
                summon.setAssistType(AssistType.None);
                field.spawnSummon(summon);
                AffectedArea aa = AffectedArea.getPassiveAA(chr, skillID, slv);
                aa.setMobOrigin((byte) 0);
                aa.setPosition(chr.getPosition());
                aa.setRect(aa.getPosition().getRectAround(si.getRects().getFirst()));
                aa.setDelay((short) 8);
                chr.getField().spawnAffectedArea(aa);
                o1.nOption = 0;
                o1.rOption = THOUSAND_BLOSSOM_FLURRY_EX;
                o1.tOption = si.getValue(SkillStat.time, slv);
                o1.startTime = System.currentTimeMillis();
                tsm.sendStat(RenPlumSwordEx, o1);
                break;
            case SOUL_IMMEASURABLE:
                this.soulImmeasuableStack = 0;
                o1.nOption = 2;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                newStats.put(SpiritAwakening, o1);
                o2.nOption = 0;
                o2.rOption = skillID;
                o2.tOption = si.getValue(time, slv);
                newStats.put(SpiritAwakeningStack, o2);
                tsm.sendStat(newStats);
                break;
            case FINAL_IMUGI_SPIRIT_SWORD_BLADE_OF_THE_UNBOUND_HEART: {
                List<SecondAtom> secondAtoms = new LinkedList<>();
                Rect rect = chr.getRectAround(new Rect(-800, -800, 800, 800));
                if (!chr.isLeft()) rect = rect.horizontalFlipAround(chr.getPosition().getX());
                final var mob = Util.getRandomFromCollection(chr.getField().getMobsInRect(rect));
                if (mob == null) {
                    break;
                }
                int key = 0;
                final long start = System.currentTimeMillis();
                for (SkillInfo.SecondAtomInfo sai : si.getSecondAtomInfos().values()) {
                    final var pos = chr.getPosition().add(sai.getPos());
                    SecondAtom fa = new SecondAtom(sai, chr.getNewSecondAtomKey(), chr.getId(), mob != null ? mob.getObjectId() : 0, key,
                            skillID, pos, start);
                    fa.setDataIndex(107);
                    fa.setSpecialAtom(true);
                    secondAtoms.add(fa);
                    key++;
                }
                chr.createSecondAtom(secondAtoms);
                break;
            }
            case HEARTBOUND_VERSE_FALLING_FLOWER:
                summon = Summon.getSummonByAndSetStat(chr, skillID, slv);
                summon.setMoveAbility(MoveAbility.Stop);
                summon.setAssistType(AssistType.ExplosionAttack);
                summon.setLeaveType(LeaveType.ASCENT);
                summon.setMoveAction((byte) 4);
                field.spawnSummon(summon);
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(ndTime, slv);
                o1.isInMillis = true;
                tsm.sendStat(Holding, o1);
                break;
            case HEARTBOUND_VERSE_CLIMBING_SERPENT:
                tsm.removeStatsBySkill(HEARTBOUND_VERSE_FALLING_FLOWER);
                break;
        }
    }

    @Override
    public void handleHit(Client c, InPacket inPacket, HitInfo hitInfo) {
        super.handleHit(c, inPacket, hitInfo);
    }

    @Override
    public void setCharCreationStats(Char chr) {
        super.setCharCreationStats(chr);
        CharacterStat cs = chr.getAvatarData().getCharacterStat();
        cs.setPosMap(FieldConstants.HOME_MAP);
        cs.setJob(JobConstants.JobEnum.REN_1.getJobId());
        cs.setLevel(10);
        cs.setStr(45);
        cs.setDex(4);
        cs.setInt(4);
        cs.setLuk(4);
        cs.setHp(1000);
        cs.setMaxHp(1000);
        if (!JobConstants.isNoManaJob(chr.getJob())) {
            cs.setMp(500);
            cs.setMaxMp(500);
        }
        cs.getExtendSP().addSpToJobLevel(1, 5);
    }

    @Override
    public void addItemToNewCharacter(Char chr) {
        super.addItemToNewCharacter(chr);
        applyNewCharacterTutorialSkip(chr);
    }

    /**
     * Match handleInitAfterMigrate before the first SET_FIELD so the client receives level-30 REN_2 data.
     */
    private void applyNewCharacterTutorialSkip(Char chr) {
        CharacterStat cs = chr.getAvatarData().getCharacterStat();
        short ren1 = JobConstants.JobEnum.REN_1.getJobId();
        short ren2 = JobConstants.JobEnum.REN_2.getJobId();
        boolean tms282 = net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3;
        if (tms282 && (chr.getJob() != ren1 || cs.getLevel() >= 30)) return;
        int[] hpMpPerLevel = GameConstants.getHpMpPerLevel(ren2);
        int hp = cs.getMaxHp();
        int mp = cs.getMaxMp();
        short ap = cs.getAp();
        for (int level = cs.getLevel() + 1; level <= 30; level++) {
            hp += hpMpPerLevel[0];
            mp += hpMpPerLevel[1];
            ap += 5;
            if (tms282 && level > 10) cs.getExtendSP().addSpToJobLevel(1,
                    net.swordie.ms.client.character.tms282.Tms282LevelService.baseSpForLevel(level));
        }
        cs.setLevel(30);
        cs.setJob(ren2);
        cs.setExp(0);
        cs.setAp(ap);
        cs.setMaxHp(hp);
        cs.setHp(hp);
        cs.setMaxMp(mp);
        cs.setMp(mp);
        cs.getExtendSP().addSpToJobLevel(1, 5);
        cs.getExtendSP().addSpToJobLevel(2, 3);

        chr.getAvatarData().getAvatarLook().setJob(ren2);
        chr.setJobHandler(JobManager.getJobById(ren2, chr));
        SkillData.getSkillsByJob(ren1).forEach(skill -> chr.addSkill(skill, true));
        SkillData.getSkillsByJob(ren2).forEach(skill -> chr.addSkill(skill, true));

        for (int qid = 36821; qid <= 36839; qid++) {
            Quest quest = QuestData.createQuestFromId(qid, chr.getId());
            quest.completeQuest();
            chr.getQuests().put(quest.getQRKey(), quest);
        }
        Quest.saveToSQL(chr.getQuests(), chr.getId());

        equipStartingItem(chr, 1215002);
        equipStartingItem(chr, 1354041);

        cs.updateCharacterStatToSQL();
        cs.getExtendSP().updateExtendSPToSQL();
        chr.getAvatarData().getAvatarLook().updateAvatarLookToSQL();
    }

    private void equipStartingItem(Char chr, int itemId) {
        Item equip = ItemData.getItemDeepCopy(itemId);
        if (equip == null) {
            return;
        }
        int gender = chr.getAvatarData().getAvatarLook().getGender();
        int bodyPart = ItemConstants.getBodyPartFromItem(itemId, gender);
        Item oldEquip = chr.getEquippedInventory().getItemBySlot((short) bodyPart);
        if (oldEquip != null) {
            chr.getEquippedInventory().removeItem(oldEquip);
        }
        chr.addItemToInventoryToNewCharacter(EQUIPPED, equip, true);
        equip.setInventoryID(chr.getInventoryByType(EQUIPPED).getId());
        equip.setCharID(chr.getId());
        equip.setInvType(EQUIPPED);
        equip.setBagIndex(bodyPart);
        equip.saveToSQL();
        if (itemId >= 1000000) {
            chr.getAvatarData().getAvatarLook().getHairEquips().removeIf(id ->
                    ItemConstants.getBodyPartFromItem(id, gender) == bodyPart);
            chr.getAvatarData().getAvatarLook().getHairEquips().add(itemId);
        }
    }

    @Override
    public void handleLevelUp(short level) {
        super.handleLevelUp(level);
        var sm = chr.getScriptManager();
        if (level == 60) {
            final var jobID = chr.getJob();
            sm.setJob((short) (jobID + 1));
            sm.completeQuestNoRewards(36840);
            sm.addSPJobAdv(jobID, 5);
            sm.addSPJobAdv((short) (jobID + 1), 3);
        } else if (level == 100) {
            final var jobID = chr.getJob();
            sm.setJob((short) (jobID + 1));
            sm.completeQuestNoRewards(36844);
            sm.addSPJobAdv(jobID, 5);
            sm.addSPJobAdv((short) (jobID + 1), 3);
        }
    }

    @Override
    public int getFinalAttackSkill(int faSkill) {
        if (faSkill == SECOND_IMUGI_SPIRIT_SWORD_SERPENT_FANG) {
            if (chr.hasSkill(HEXA_SECOND_IMUGI_SPIRIT_SWORD_SERPENT_FANG)) return HEXA_SECOND_IMUGI_SPIRIT_SWORD_SERPENT_FANG;
        }
        return super.getFinalAttackSkill(faSkill);
    }

    @Override
    public int handleSetCoolDownSkill(int skillId) {
        switch (skillId) {
            case FIRST_IMUGI_SPIRIT_SWORD_BURROWING_EARTH_EFF,
                 FINAL_IMUGI_SPIRIT_SWORD_BLADE_OF_THE_UNBOUND_HEART -> {
                chr.addSkillCooldown(FIRST_IMUGI_SPIRIT_SWORD_BURROWING_EARTH_EFF, 20000);
                return 1;
            }
        }
        return super.handleSetCoolDownSkill(skillId);
    }

    @Override
    public void handleInitAfterMigrate(Char chr) {
        if (chr.getLevel() < 30) {
            ScriptManagerImpl sm = chr.getScriptManager();
            boolean tms282 = net.swordie.ms.connection.protocol.Protocol.active().version()
                    == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3;
            if (tms282) sm.setJob(JobConstants.JobEnum.REN_2.getJobId());
            sm.levelUntil(30);
            if (!tms282) sm.setJob(JobConstants.JobEnum.REN_2.getJobId());
            for (int qid = 36821; qid <= 36839; qid++) {
                sm.completeQuestNoRewards(qid);
            }
            sm.addSPJobAdv(JobConstants.JobEnum.REN_1.getJobId(), 5);
            sm.addSPJobAdv(JobConstants.JobEnum.REN_2.getJobId(), 3);
            sm.giveAndEquip(1215002);
            sm.giveAndEquip(1354041);
            sm.warp(FieldConstants.HOME_MAP);
        }
        int restoredSp = restoreTms282TutorialSp(chr);
        if (restoredSp > 0) {
            try { chr.getAvatarData().getCharacterStat().getExtendSP().updateExtendSPToSQL(); }
            catch (RuntimeException failure) {
                System.out.println("[TMS282] Ren tutorial SP save failed; retained for disconnect save: " + failure);
            }
            chr.write(net.swordie.ms.connection.packet.Tms282SkillPackets.spChanged(false, chr.getJob(),
                    chr.getAvatarData().getCharacterStat()));
            System.out.println("[TMS282] restored Ren tutorial SP character=" + chr.getId() + " added=" + restoredSp);
        }
        super.handleInitAfterMigrate(chr);
        incrementRiotousHeart();
    }

    /** The old creation skip granted only bonuses. Free SP + spent visible levels is invariant under skillUp/reset. */
    private int restoreTms282TutorialSp(Char chr) {
        if (net.swordie.ms.connection.protocol.Protocol.active().version()
                != net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3
                || !JobConstants.isRen(chr.getJob()) || JobConstants.getJobLevel(chr.getJob()) < 2
                || chr.getLevel() < 30 || !chr.hasQuestCompleted(36839)) return 0;
        var sp = chr.getAvatarData().getCharacterStat().getExtendSP();
        if (sp == null || sp.getSpSet().stream().noneMatch(set -> set.getJobLevel() == 1)) return 0;
        int budget = 10; // Existing creation5 and the second-job bonus5; retain the server's rules.
        for (int level = 11; level <= 30; level++)
            budget += net.swordie.ms.client.character.tms282.Tms282LevelService.baseSpForLevel(level);
        long earned = sp.getSpByJobLevel((byte) 1);
        if (earned < 0) return 0;
        for (int skillId : new int[]{161001000, 161001001, 161001003, 161000004, 161000005}) {
            SkillInfo info = SkillData.getSkillInfoById(skillId);
            if (info == null || info.isInvisible() || info.getFixLevel() > 0) return 0;
            earned += Math.max(0, chr.getSkillLevel(skillId));
        }
        int missing = (int) Math.max(0, budget - earned);
        if (missing > 0) sp.addSpToJobLevel(1, missing);
        return missing;
    }

    @Override
    public void handleShootObject(Char chr, ShootObjectSkillInfo sosi) {
        var skillId = sosi.getSkillId();
        var slv = sosi.getSlv();
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (skillId == RIOTOUS_HEART || skillId == HEXA_RIOTOUS_HEART) {
            Option o = new Option();
            byte val = tsm.hasStat(RenPlumSwordForm3Shoot) ? (byte) (tsm.getTotalNOptionOfStat(RenPlumSwordForm3Shoot) - 1) : 0;
            o.nOption = Math.max(0, val);
            o.rOption = skillId;
            if (o.nOption <= 0) {
                tsm.removeStat(RenPlumSwordForm3Shoot);
            } else {
                tsm.sendStat(RenPlumSwordForm3Shoot, o);
            }
        } else if (skillId == HEARTS_UNITED || skillId == HEXA_HEARTS_UNITED) {
            if (tsm.hasStat(RenPlumSwordForm3Shoot)) {
                incrementRiotousHeart();
                tsm.removeStat(RenPlumSwordForm3Shoot);
            }
        }
        super.handleShootObject(chr, sosi);
    }

    public void incrementRiotousHeart() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o = new Option();
        if (chr.hasSkill(HEXA_RIOTOUS_HEART)) {
            o.nOption = 2;
            o.rOption = HEXA_RIOTOUS_HEART;
            tsm.sendStat(RenPlumSwordForm3Stack, o);
            chr.write(WvsContext.updateSkillStackRequestResult(HEXA_RIOTOUS_HEART, (byte) 1));
        } else if (chr.hasSkill(RIOTOUS_HEART)) {
            o.nOption = 2;
            o.rOption = RIOTOUS_HEART;
            tsm.sendStat(RenPlumSwordForm3Stack, o);
            chr.write(WvsContext.updateSkillStackRequestResult(RIOTOUS_HEART, (byte) 1));
        }
    }
}
