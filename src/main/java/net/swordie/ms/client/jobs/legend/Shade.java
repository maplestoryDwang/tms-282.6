package net.swordie.ms.client.jobs.legend;

import it.unimi.dsi.fastutil.ints.Int2LongMap;
import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.info.HitInfo;
import net.swordie.ms.client.character.skills.ForceAtom;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.Skill;
import net.swordie.ms.client.character.skills.info.AttackInfo;
import net.swordie.ms.client.character.skills.info.ForceAtomInfo;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.client.character.skills.info.SkillUseInfo;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.client.jobs.Job;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.*;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.enums.*;
import net.swordie.ms.life.AffectedArea;
import net.swordie.ms.life.Summon;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.life.mob.MobStat;
import net.swordie.ms.life.mob.MobTemporaryStat;
import net.swordie.ms.life.mob.skill.BurnedInfo;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.loaders.StringData;
import net.swordie.ms.scripts.ScriptManagerImpl;
import net.swordie.ms.util.Position;
import net.swordie.ms.util.Rect;
import net.swordie.ms.util.Util;
import net.swordie.ms.world.field.Field;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static net.swordie.ms.client.character.skills.SkillStat.*;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;

public class Shade extends Job {

    public static final int SPIRIT_BOND_I = 20050285;
    public static final int FLASH_FIST = 25001002;
    public static final int FOX_TROT = 20051284;

    public static final int SPIRIT_AFFINITY = 20050074;

    public static final int SWIFT_STRIKE = 25001000;
    public static final int VULPES_LEAP = 25001204;

    public static final int FOX_SPIRITS = 25101009; //Buff (ON/OFF)
    public static final int FOX_SPIRITS_INIT = 25100009;
    public static final int FOX_SPIRITS_ATOM = 25100010;
    public static final int FOX_SPIRITS_ATOM_2 = 25120115; //Upgrade
    public static final int GROUND_POUND_FIRST = 25101000; //Special Attack (Slow Debuff)
    public static final int GROUND_POUND_SECOND = 25100001; //Special Attack (Slow Debuff)

    public static final int SUMMON_OTHER_SPIRIT_KNOCKBACK = 25111211;

    public static final int SUMMON_OTHER_SPIRIT = 25111209; //Passive Buff (Icon)
    public static final int SPIRIT_TRAP = 25111206; //Tile
    public static final int WEAKEN = 25110210; //Passive Debuff

    public static final int SPIRIT_WARD = 25121209; //Special Buff
    public static final int MAPLE_WARRIOR_SH = 25121108; //Buff
    public static final int BOMB_PUNCH = 25121000;
    public static final int BOMB_PUNCH_FINAL = 25120003; //Special Attack (Stun Debuff)
    public static final int DEATH_MARK = 25121006; //Special Attack (Mark Debuff)
    public static final int SOUL_SPLITTER = 25121007; //Special Attack (Split)
    public static final int FIRE_FOX_SPIRIT_MASTERY = 25120110;
    public static final int HEROS_WILL_SH = 25121211;
    public static final int SPIRIT_FRENZY = 25111005;

    public static final int HEROIC_MEMORIES_SH = 25121132;
    public static final int SPIRIT_BOND_MAX = 25121131;
    public static final int SPIRIT_BOND_MAX_2 = 25121133;
    public static final int SPIRIT_INCARNATION = 25121030;

    public static final int SPIRIT_FLOW = 400051010;
    public static final int SPIRITGATE_SUMMONER = 400051022;
    public static final int SPIRITGATE_SUMMONS = 400051023;
    public static final int TRUE_SPIRIT_CLAW = 400051043;
    public static final int SMASHING_MULTIPUNCH_KEYDOWN = 400051078;
    public static final int SMASHING_MULTIPUNCH_FINALE = 400051079;

    private final int[] addedSkills = new int[]{
            FLASH_FIST,
            SPIRIT_AFFINITY,
            SWIFT_STRIKE,
            FOX_TROT,
            SPIRIT_BOND_I,
            25000003,};

    private long spiritWardTimer;
    private long spiritFlowAttackTime = 0;

    public Shade(Char chr) {
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

    public static void reviveBySummonOtherSpirit(Char chr) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        chr.heal(chr.getMaxHP(), true);
        tsm.removeStatsBySkill(SUMMON_OTHER_SPIRIT);
        chr.chatMessage("你被 " + StringData.getSkillStringById(SUMMON_OTHER_SPIRIT).getName() + " 技能復活。");
        chr.write(UserPacket.effect(Effect.skillSpecial(SUMMON_OTHER_SPIRIT)));
        chr.getField().broadcast(UserRemote.effect(chr.getId(), Effect.skillSpecial(SUMMON_OTHER_SPIRIT)), chr);
        chr.write(UserPacket.effect(Effect.skillUse(25111211, chr.getLevel(), (byte) 1)));
        chr.getField().broadcast(UserRemote.effect(chr.getId(), Effect.skillUse(25111211, chr.getLevel(), (byte) 1)), chr);
    }

    @Override
    public boolean isHandlerOfJob(short id) {
        return JobConstants.isShade(id);
    }

    // Attack related methods ------------------------------------------------------------------------------------------
    @Override
    public void handleDebuffOnMob(Client c, Mob mob, SkillInfo si, int skillID, int slv, long damage) {
        Char chr = c.getChr();
        applyWeakenOnMob(mob, slv);
        Option o1 = new Option();
        Field field = chr.getField();
        MobTemporaryStat mts = mob.getTemporaryStat();
        switch (skillID) {
            case GROUND_POUND_FIRST:
            case GROUND_POUND_SECOND:
                if (!mts.hasCurrentMobStatBySkillId(GROUND_POUND_FIRST)) {
                    if (!mob.isBoss()) {
                        o1.nOption = -si.getValue(y, slv);
                        o1.rOption = GROUND_POUND_FIRST;
                        o1.tOption = si.getValue(time, slv);
                        mts.addStatOptions(mob, MobStat.Speed, o1);
                    }
                }
                break;
            case BOMB_PUNCH_FINAL:
                if (!mts.hasCurrentMobStatBySkillId(BOMB_PUNCH)) {
                    SkillInfo bpi = SkillData.getSkillInfoById(BOMB_PUNCH);
                    byte bombPunchslv = (byte) chr.getSkill(BOMB_PUNCH).getCurrentLevel();
                    if (Util.succeedProp(bpi.getValue(prop, bombPunchslv)) && !mob.isBoss()) {
                        o1.nOption = 1;
                        o1.rOption = BOMB_PUNCH;
                        o1.tOption = 2;
                        mts.addStatOptions(mob, MobStat.Stun, o1);
                    }
                }
                break;
            case DEATH_MARK:
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    if (!mob.isBoss()) {
                        int healrate = si.getValue(x, slv);
                        chr.heal((int) (chr.getMaxHP() / ((double) 100 / healrate)));
                        BurnedInfo bi = BurnedInfo.createBurnInfo(chr, DEATH_MARK, slv, damage);
                        o1.nOption = 1;
                        o1.rOption = skillID;
                        o1.tOption = si.getValue(dotTime, slv);
                        mts.addStatOptions(mob, MobStat.DebuffHealing, o1);
                        mts.createAndAddBurnedInfo(mob, bi, DEATH_MARK);
                    }
                }
                break;
            case SOUL_SPLITTER:
                //Split Duration & Timer on when to removeLife
                int duration = si.getValue(time, slv);
                if (!mob.isSplit()) {
                    mob.soulSplitMob(chr, mob, duration, skillID);
                }
                break;
            case SPIRITGATE_SUMMONS:
                si = SkillData.getSkillInfoById(SPIRITGATE_SUMMONER);
                slv = (byte) chr.getSkillLevel(SPIRITGATE_SUMMONER);
                o1.rOption = skillID;
                o1.tOption = si.getValue(s2, slv);
                int stack = 0;
                if (mts.hasCurrentMobStat(MobStat.SpiritGate)) {
                    stack = mts.getCurrentOptionsByMobStat(MobStat.SpiritGate).nOption;
                }
                if (stack < si.getValue(q2, slv)) {
                    stack++;
                }
                o1.nOption = stack * si.getValue(v2, slv);
                o1.xOption = stack * si.getValue(q, slv);
                mts.addStatOptions(mob, MobStat.SpiritGate, o1);
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
            if (chr.hasSkill(SPIRIT_BOND_I) && chr.getHP() < chr.getMaxHP()) {
                chr.heal((int) (chr.getMaxHP() / ((double) 100)));
            }
            if (skillID != FOX_SPIRITS_ATOM && skillID != FOX_SPIRITS_ATOM_2) {
                if (Util.succeedProp(10)) {
                    createFoxSpiritForceAtom(skillID);
                }
            }
        }

        if (skillID != FOX_SPIRITS_ATOM
                && skillID != FOX_SPIRITS_ATOM_2
                && skillID != SPIRITGATE_SUMMONS
                && skillID != SPIRIT_TRAP
                && tsm.hasStat(TempSecondaryStat)) {
            doSpiritFlowBonusAttack(now);
        }

        switch (skillID) {
            case SPIRIT_INCARNATION:
                Option o = new Option();
                o.nOption = 1;
                o.rOption = skillID;
                o.tOption = si.getValue(time, slv);
                tsm.sendStat(NotDamaged, o);
                chr.dispose();
                break;
        }
    }

    private void createFoxSpiritForceAtom(int skillID) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (tsm.hasStat(EunwolFoxSpirit)) {
            SkillInfo si = SkillData.getSkillInfoById(FOX_SPIRITS);
            Rect rect = chr.getPosition().getRectAround(si.getFirstRect());
            List<Mob> mobs = chr.getField().getMobsInRect(rect);
            if (mobs.size() <= 0) {
                return;
            }
            Mob mob = Util.getRandomFromCollection(mobs);
            int mobID = mob.getObjectId();
            int recreationCount = 2;
            int atomid = FOX_SPIRITS_ATOM;
            ForceAtomEnum fae = ForceAtomEnum.RABBIT_ORB;

            if (chr.hasSkill(FIRE_FOX_SPIRIT_MASTERY)) {
                atomid = FOX_SPIRITS_ATOM_2;
                fae = ForceAtomEnum.FLAMING_RABBIT_ORB;
                recreationCount = 3;
            }
            ForceAtomInfo forceAtomInfo = new ForceAtomInfo(chr.getNewForceAtomKey(), fae.getInc(), 15, 7,
                    305, 400, Util.getCurrentTime(), 1, 0,
                    new Position(chr.isLeft() ? 0 : -50, -50));
            ForceAtom fa = new ForceAtom(false, 0, chr.getId(), fae,
                    true, mobID, atomid, forceAtomInfo, new Rect(), 0, 300,
                    mob.getPosition(), atomid, mob.getPosition(), 0);
            fa.setMaxRecreationCount(recreationCount);
            chr.createForceAtom(fa);
        }
    }

    public void applyWeakenOnMob(Mob mob, int slv) {
        if (chr.hasSkill(WEAKEN)) {
            Option o1 = new Option();
            Option o2 = new Option();
            Option o3 = new Option();
            SkillInfo si = SkillData.getSkillInfoById(WEAKEN);
            if (Util.succeedProp(si.getValue(prop, slv))) {
                if (!mob.isBoss()) {
                    MobTemporaryStat mts = mob.getTemporaryStat();
                    EnumMap<MobStat, Option> map = new EnumMap<>(MobStat.class);
                    o1.nOption = si.getValue(x, slv);
                    o1.rOption = WEAKEN;
                    o1.tOption = si.getValue(time, slv);
                    map.put(MobStat.Weakness, o1);
                    o2.nOption = si.getValue(y, slv);
                    o2.rOption = WEAKEN;
                    o2.tOption = si.getValue(time, slv);
                    map.put(MobStat.ACC, o2);
                    o3.nOption = si.getValue(z, slv);
                    o3.rOption = WEAKEN;
                    o3.tOption = si.getValue(time, slv);
                    map.put(MobStat.EVA, o3);
                    mts.addStatOptions(mob, map);
                }
            }
        }
    }

    public void doSpiritFlowBonusAttack(long now) {
        SkillInfo si = SkillData.getSkillInfoById(SPIRIT_FLOW);
        if (now - spiritFlowAttackTime >= 2000) {
            for (Map.Entry<Map<Integer, Integer>, Integer> randomSkills : si.getRandomSkills().entrySet()) {
                if (Util.succeedProp(randomSkills.getValue())) {
                    for (Map.Entry<Integer, Integer> randomSkillList : randomSkills.getKey().entrySet()) {
                        //Hyper Skill and Spirit Frenzy
                        if (randomSkillList.getKey() == 25121055 || randomSkillList.getKey() == 25111012) {
                            int skillID = randomSkillList.getKey();
                            byte slv = (byte) (skillID == 25121055 ? chr.getSkillLevel(25121030) : chr.getSkillLevel(25111005));
                            for (var currentArea : chr.getField().getAffectedAreas()) {
                                if (currentArea.getSkillID() == skillID && currentArea.getCharID() == chr.getId()) {
                                    currentArea.broadcastLeavePacket();
                                }
                            }
                            SkillInfo si2 = SkillData.getSkillInfoById(skillID);
                            AffectedArea aa = AffectedArea.getPassiveAA(chr, skillID, slv);
                            int duration = 3000;
                            aa.setDuration(duration);
                            aa.setPosition(chr.getPosition());
                            aa.setSkillID(skillID);
                            aa.setSlv(slv);
                            aa.setRect(aa.getPosition().getRectAround(si2.getRects().get(0)));
                            chr.getField().spawnAffectedArea(aa);
                            break;
                            //chr.getField().broadcastPacket(UserPacket.createSpiritFlow(chr, skillID, duration)); //this is affect area so don't need this
                        } else {
                            if (chr.hasSkillOnCooldown(randomSkillList.getKey())) {
                                continue;
                            }
                            chr.write(UserLocal.bonusAttackDelayRequest(randomSkills.getKey()));
                            break;
                        }
                    }
                    spiritFlowAttackTime = now;
                }
            }
        }
    }


    public void summonSpiritgateSummons(Position position) {
        if (!chr.hasSkill(SPIRITGATE_SUMMONER)) {
            return;
        }
        Field field = chr.getField();
        Skill skill = chr.getSkill(SPIRITGATE_SUMMONER);
        int slv = skill.getCurrentLevel();

        if (field.getSummons().stream().anyMatch(s -> s.getSkillID() == SPIRITGATE_SUMMONER && s.getOwnerId() == chr.getId())) {
            for (int i = 0; i < 2; i++) {
                Summon summon = Summon.getSummonByAndSetStat(chr, SPIRITGATE_SUMMONS, slv);
                summon.setMoveAbility(MoveAbility.FlyRandomAroundPos);
                summon.setAssistType(AssistType.Attack);
                summon.setPosition(position);
                field.spawnAddSummon(summon);
            }
        }
    }

    // Skill related methods -------------------------------------------------------------------------------------------
    @Override
    public void handleSkill(Client c, InPacket inPacket, SkillUseInfo skillUseInfo) {
        super.handleSkill(c, inPacket, skillUseInfo);
        Char chr = c.getChr();
        SkillInfo si = skillUseInfo.skillInfo;
        int slv = skillUseInfo.slv;
        int skillID = skillUseInfo.skillID;
        EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o1 = new Option();
        Option o2 = new Option();
        Option o3 = new Option();
        Option o4 = new Option();
        Option o5 = new Option();
        switch (skillID) {
            case FOX_SPIRITS:
                o1.nOption = 1;
                o1.rOption = skillID;
                tsm.sendStat(EunwolFoxSpirit, o1);
                break;
            case SUMMON_OTHER_SPIRIT:
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(ReviveOnce, o1);
                break;
            case SPIRIT_WARD:
                o1.nOption = 3;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(SpiritGuard, o1);
                this.spiritWardTimer = System.currentTimeMillis() + (si.getValue(time, slv) * 1000L);
                break;
            case HEROIC_MEMORIES_SH:
                o1.nReason = skillID;
                o1.nValue = si.getValue(indieDamR, slv);
                o1.tTerm = si.getValue(time, slv);
                tsm.sendStat(IndieDamR, o1);
                break;
            case SPIRIT_BOND_MAX:
                o1.nReason = skillID;
                o1.nValue = si.getValue(indieDamR, slv);
                o1.tTerm = si.getValue(time, slv);
                newStats.put(IndieDamR, o1);
                o2.nReason = skillID;
                o2.nValue = si.getValue(indiePad, slv);
                o2.tTerm = si.getValue(time, slv);
                newStats.put(IndiePAD, o2);
                o3.nReason = skillID;
                o3.nValue = si.getValue(indieBDR, slv);
                o3.tTerm = si.getValue(time, slv);
                newStats.put(IndieBDR, o3);
                o4.nReason = skillID;
                o4.nValue = -1; //Booster
                o4.tTerm = si.getValue(time, slv);
                newStats.put(IndieBooster, o4);
                o5.nReason = skillID;
                o5.nValue = si.getValue(indieIgnoreMobpdpR, slv);
                o5.tTerm = si.getValue(time, slv);
                newStats.put(IndieIgnoreMobpdpR, o5);
                tsm.sendStat(newStats);
                break;
            case SPIRIT_BOND_MAX_2:
                List<Summon> summons = chr.getField().getSummonsByChar(chr);
                for (var s : summons) {
                    if (s != null && s.getSkillID() == skillID) {
                        chr.write(Summoned.removed(s, LeaveType.ANIMATION));
                        tsm.removeStatsBySkill(s.getSkillID());
                    }
                }
                o1.nReason = skillID;
                o1.nValue = si.getValue(indieDamR, slv);
                o1.tTerm = si.getValue(time, slv);
                newStats.put(IndieDamR, o1);
                o2.nReason = skillID;
                o2.nValue = si.getValue(indiePad, slv);
                o2.tTerm = si.getValue(time, slv);
                newStats.put(IndiePAD, o2);
                o3.nReason = skillID;
                o3.nValue = si.getValue(indieBDR, slv);
                o3.tTerm = si.getValue(time, slv);
                newStats.put(IndieBDR, o3);
                o4.nReason = skillID;
                o4.nValue = -1; //Booster
                o4.tTerm = si.getValue(time, slv);
                newStats.put(IndieBooster, o4);
                o5.nReason = skillID;
                o5.nValue = si.getValue(indieIgnoreMobpdpR, slv);
                o5.tTerm = si.getValue(time, slv);
                newStats.put(IndieIgnoreMobpdpR, o5);
                tsm.sendStat(newStats);

                Field field = chr.getField();
                Summon summon = Summon.getSummonByAndSetStatWithTime(c.getChr(), skillID, slv, Util.getCurrentTimeLong(), si.getValue(time, slv));
                summon.setMoveAbility(MoveAbility.Walk);
                summon.setEnterType(EnterType.NoAnimation);
                summon.setAssistType(AssistType.AttackManual);
                summon.setAttackActive(true);
                field.spawnSummon(summon);
                chr.addSkillCoolTime(skillID, System.currentTimeMillis() + 120000);
                chr.write(UserLocal.skillCooltimeSetM(skillID, 120000));
                break;
            case SPIRIT_FLOW:
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(TempSecondaryStat, o1);

                for (Int2LongMap.Entry e : chr.getSkillCoolTimes().int2LongEntrySet()) {
                    int cdSkillId = e.getIntKey();
                    long val      = e.getLongValue();

                    SkillInfo skillInfo = SkillData.getSkillInfoById(cdSkillId);
                    if (skillInfo == null || cdSkillId == skillID) {
                        continue;
                    }
                    if (JobConstants.isShade((short) skillInfo.getRootId())) {
                        chr.reduceSkillCoolTime(cdSkillId, val);
                    }
                }
                break;
            case SPIRITGATE_SUMMONER:
                summon = Summon.getSummonByAndSetStat(chr, skillID, slv);
                summon.setMoveAbility(MoveAbility.Stop);
                summon.setAssistType(AssistType.AttackManual);
                chr.getField().spawnSummon(summon);
                break;
            case SPIRIT_TRAP:
                SkillInfo fci = SkillData.getSkillInfoById(skillID);
                AffectedArea aa = AffectedArea.getPassiveAA(chr, skillID, slv);
                aa.setMobOrigin((byte) 0);
                aa.setPosition(chr.getPosition());
                aa.setRect(aa.getPosition().getRectAround(fci.getRects().get(0)));
                aa.setDelay((short) 4);
                chr.getField().spawnAffectedArea(aa);
                break;
            case FIRE_FOX_SPIRIT_MASTERY, FOX_SPIRITS_INIT:
                createFoxSpiritForceAtom(skillID);
                break;
            case HEROS_WILL_SH:
                tsm.removeAllDebuffs();
                break;
        }
    }

    // Hit related methods ---------------------------------------------------------------------------------------------
    @Override
    public void handleHit(Client c, InPacket inPacket, HitInfo hitInfo) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (tsm.hasStat(SpiritGuard) && hitInfo.hpDamage > 0) {
            deductSpiritWard();
            hitInfo.hpDamage = 0;
            hitInfo.mpDamage = 0;
        }
        super.handleHit(c, inPacket, hitInfo);
    }

    public void doSpiritWard() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (tsm.hasStat(SpiritGuard)) {
            return;
        }
        SkillInfo si = SkillData.getSkillInfoById(25121209);
        int slv = chr.getSkillLevel(25121209);
        Option o1 = new Option();
        o1.nOption = 3;
        o1.rOption = 25121209;
        o1.tOption = si.getValue(time, slv);
        tsm.sendStat(SpiritGuard, o1);
        spiritWardTimer = System.currentTimeMillis() + (si.getValue(time, slv) * 1000L);
    }

    public void deductSpiritWard() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (!chr.hasSkill(SPIRIT_WARD)) {
            return;
        }
        Skill skill = chr.getSkill(SPIRIT_WARD);
        Option o = new Option();
        if (tsm.hasStat(SpiritGuard)) {
            int spiritWardCount = tsm.getOption(SpiritGuard).nOption;

            if (spiritWardCount > 0) {
                spiritWardCount--;
            }

            if (spiritWardCount <= 0) {
                tsm.removeStatsBySkill(skill.getSkillId());
            } else {
                o.setInMillis(true);
                o.nOption = spiritWardCount;
                o.rOption = skill.getSkillId();
                o.tOption = (int) (spiritWardTimer - System.currentTimeMillis());
                tsm.sendStat(SpiritGuard, o);
            }
        }
    }

    @Override
    public void handleMobDebuffSkill(Char chr) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (tsm.hasStat(SpiritGuard)) {
            tsm.removeAllDebuffs();
            deductSpiritWard();
        }
        super.handleMobDebuffSkill(chr);
    }

    @Override
    public void setCharCreationStats(Char chr) {
        super.setCharCreationStats(chr);
        chr.getAvatarData().getCharacterStat().setPosMap(JobConstants.SHADE_CREATION_MAP);
    }

    @Override
    public void handleJobAdvance() {
        ScriptManagerImpl sm = chr.getScriptManager();
        if (chr.getJob() == JobConstants.JobEnum.SHADE1.getJobId()) {
            if (chr.getLevel() < 30) {
                sm.sendSayOkay("#e這個職業需要達到#r30#k等以上才能轉職");
                return;
            }
            if (sm.sendAskYesNo("#e要跳過轉職任務嗎？")) {
                sm.jobAdvance(JobConstants.JobEnum.SHADE2.getJobId());
                sm.completeQuestNoRewards(38028);
                sm.completeQuestNoRewards(38029);
                sm.completeQuestNoRewards(38030);
            }
        } else if (chr.getJob() == JobConstants.JobEnum.SHADE2.getJobId()) {
            if (chr.getLevel() < 60) {
                sm.sendSayOkay("#e這個職業需要達到#r60#k等以上才能轉職");
                return;
            }
            if (sm.sendAskYesNo("#e要跳過轉職任務嗎？")) {
                sm.jobAdvance(JobConstants.JobEnum.SHADE3.getJobId());
                sm.completeQuestNoRewards(38074);
                sm.completeQuestNoRewards(38075);
                sm.completeQuestNoRewards(38076);
            }
        } else if (chr.getJob() == JobConstants.JobEnum.SHADE3.getJobId()) {
            if (chr.getLevel() < 100) {
                sm.sendSayOkay("#e這個職業需要達到#r100#k等以上才能轉職");
                return;
            }
            if (sm.sendAskYesNo("#e要跳過轉職任務嗎？")) {
                sm.jobAdvance(JobConstants.JobEnum.SHADE4.getJobId());
                sm.completeQuestNoRewards(38072);
                sm.completeQuestNoRewards(38073);
            }
        } else {
            sm.sendSayOkay("#e目前的狀態無法轉職。");
        }
    }

    @Override
    public int handleSetCoolDownSkill(int skillId) {
        switch (skillId) {
            case SPIRIT_BOND_MAX_2 -> { // Spirit Bond Max
                int skillID = SPIRIT_BOND_MAX;
                int slv = chr.getSkillLevel(skillID);
                chr.setSkillCooldown(skillID, slv);
                return 1;
            }
            case SUMMON_OTHER_SPIRIT -> {
                chr.reduceSkillCoolTime(SUMMON_OTHER_SPIRIT, 1800000);
                return 1;
            }
        }
        return super.handleSetCoolDownSkill(skillId);
    }
}
