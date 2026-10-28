package net.swordie.ms.client.jobs.adventurer.warrior;

import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.ExtendSP;
import net.swordie.ms.client.character.info.HitInfo;
import net.swordie.ms.client.character.items.BodyPart;
import net.swordie.ms.client.character.items.Equip;
import net.swordie.ms.client.character.skills.*;
import net.swordie.ms.client.character.skills.info.*;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.client.social.Party.Party;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.*;
import net.swordie.ms.constants.ItemConstants;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.constants.SkillConstants;
import net.swordie.ms.enums.ForceAtomEnum;
import net.swordie.ms.handlers.GlobalTimerManager;
import net.swordie.ms.life.AffectedArea;
import net.swordie.ms.life.Life;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.life.mob.MobStat;
import net.swordie.ms.life.mob.MobTemporaryStat;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.util.Position;
import net.swordie.ms.util.Rect;
import net.swordie.ms.util.Util;
import net.swordie.ms.world.field.Field;

import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

import static net.swordie.ms.client.character.skills.SkillStat.*;
import static net.swordie.ms.client.character.skills.SkillStat.mob;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;

public class Paladin extends Warrior {

    public static final int FINAL_ATTACK = 1200002;
    public static final int WEAPON_BOOSTER = 1201004;
    public static final int CLOSE_COMBAT = 1201013;
    public static final int VESSEL_OF_LIGHT = 1200014;

    public static final int SHIELD_MASTERY = 1210001;
    public static final int DIVINE_SHIELD = 1210016;

    public static final int HP_RECOVERY = 1211010;
    public static final int COMBAT_ORDERS = 1211011;
    public static final int PARASHOCK_GUARD = 1211014;
    public static final int DIVINE_CHARGE = 1211018;

    public static final int GREATER_VESSEL_OF_LIGHT = 1220010;
    public static final int DIVINE_JUDGEMENT = 1220022;
    public static final int DIVINE_JUDGEMENT_DEBUFF = 1221023;
    public static final int BLAST = 1221009;
    public static final int NOBLE_DEMAND = 1211013;
    public static final int HEAVENS_HAMMER = 1221011;
    public static final int DIVINE_BLESSING = 1221015;
    public static final int DIVINE_MARK = 1221019;
    public static final int MAPLE_WARRIOR = 1221000;
    public static final int GUARDIAN = 1221016;
    public static final int MAGIC_CRASH = 1221014;
    public static final int HEROS_WILL = 1221012;

    //Hyper Skills
    public static final int EPIC_ADVENTURE = 1221053; //Lv200
    public static final int SACROSANCTITY = 1221054; //Lv150
    public static final int SMITE_SHIELD = 1221052; //Lv170

    // V Skills
    public static final int HAMMERS_OF_THE_RIGHTEOUS = 400011052;
    public static final int GRAND_GUARDIAN = 400011072;
    public static final int HAMMERS_OF_THE_RIGHTEOUS_2 = 400011053;
    public static final int DIVINE_ECHO = 400011003;
    public static final int DIVINE_ECHO_MIMIC = 400011021; // skillId given to the user that mimics
    public static final int MIGHTY_MJOLNIR = 400011131;
    public static final int MIGHTY_MJOLNIR_EXPLOSION = 400011132;

    // HEXA Skills
    public static final int SACRED_BASTION = 1241500;
    public static final int SACRED_BASTION_TILE = 1241501;
    public static final int SACRED_BASTION_EXTRA = 1241502;
    public static final int SACRED_BASTION_AFTER_ATTACK = 1241503;
    public static final int HEXA_FINAL_ATTACK = 1240008;
    public static final int HEXA_BLAST = 1241000;
    public static final int HEXA_DIVINE_JUDGEMENT = 1240001;
    public static final int HEXA_DIVINE_JUDGEMENT_DEBUFF = 1241002;
    public static final int HEXA_DIVINE_CHARGE = 1241003;
    public static final int HEXA_DIVINE_MARK = 1241004;
    public static final int FALLING_JUSTICE = 1241005;
    public static final int FALLING_JUSTICE_2 = 1241006;
    public static final int HEXA_HEAVENS_HAMMER = 1241007;
    public static final int RISING_JUSTICE = 1241009;

    // HEXA Boosts
    public static final int HEXA_DIVINE_ECHO = 500061000;
    public static final int HEXA_DIVINE_ECHO_MIMIC = 500061001; // skillId given to the user that mimics

    private long lastDivineShieldHit = Long.MIN_VALUE;
    private int lastCharge = 0;
    private int divShieldAmount = 0;
    private ScheduledFuture divineEchoTimer;
    private SpecialStack specialStack;

    public Paladin(Char chr) {
        super(chr);
    }

    @Override
    public boolean isHandlerOfJob(short id) {
        return JobConstants.isPaladin(id);
    }

    @Override
    public void update(long now) {
        super.update(now);
        if (chr.hasSkill(DIVINE_JUDGEMENT)) {
            SpecialStack.prune(chr, this.specialStack, now);
        }
    }

    @Override
    public void handleKeyDownSkillCost(int skillId) {
        super.handleKeyDownSkillCost(skillId);
        switch (skillId) {
            case GRAND_GUARDIAN:
                decreaseHPByGrandGuardian();
                break;
        }
    }

    public void increaseGrandGuardianState() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (tsm.hasStat(GrandCross)) {
            Option o = tsm.getOption(GrandCross);
            o.nOption = 2;
            tsm.updateStat(GrandCross, o);
        }
    }

    public void decreaseHPByGrandGuardian() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (tsm.hasStat(GrandCross)) {
            SkillInfo si = SkillData.getSkillInfoById(GRAND_GUARDIAN);
            int slv = chr.getSkillLevel(GRAND_GUARDIAN);
            int hpConsumed = (int) (((double) chr.getMaxHP() * 1.5) / 100F);
            //TODO (int) ((double) (chr.getMaxHP() * si.getValue(t, slv)) / 100F);
            chr.heal(-hpConsumed);
        }
    }

    public static void doParashockGaurd(Char chr, int slv) {
        EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
        Option o1 = new Option();
        Option o2 = new Option();
        int skillID = PARASHOCK_GUARD;
        SkillInfo si = SkillData.getSkillInfoById(skillID);
        o1.nValue = si.getValue(indiePad, slv);
        o1.nReason = skillID;
        newStats.put(IndieDamR, o1);
        o2.nOption = 1;
        o2.rOption = skillID;
        o2.xOption = chr.getId();
        o2.yOption = 0;
        newStats.put(KnightsAura, o2);
        chr.getTemporaryStatManager().sendStat(newStats);
    }

    // Attack related methods ------------------------------------------------------------------------------------------
    @Override
    public void handleDebuffOnMob(Client c, Mob mob, SkillInfo si, int skillID, int slv, long damage) {
        Char chr = c.getChr();
        Option o1 = new Option();
        MobTemporaryStat mts = mob.getTemporaryStat();
        switch (skillID) {
            case CLOSE_COMBAT:
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    if (Util.succeedProp(si.getValue(prop, slv))) {
                        o1.nOption = si.getValue(x, slv);
                        o1.rOption = skillID;
                        o1.tOption = si.getValue(time, slv);
                        mts.addStatOptions(mob, MobStat.Speed, o1);
                    }
                }
                break;
            case DIVINE_CHARGE:
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    if (Util.succeedProp(si.getValue(prop, slv))) {
                        o1.nOption = 1;
                        o1.rOption = skillID;
                        o1.tOption = si.getValue(time, slv);
                        mts.addStatOptions(mob, MobStat.Seal, o1);
                    }
                }
                break;
            case SMITE_SHIELD:
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    o1.nOption = 1;
                    o1.rOption = skillID;
                    o1.tOption = si.getValue(time, slv);
                    mts.addStatOptions(mob, MobStat.Smite, o1);
                }
                break;
        }
        if (!JobConstants.isPhantom(chr.getJob())) {
            super.handleDebuffOnMob(c, mob, si, skillID, slv, damage);
        }
    }

    @Override
    public void handleAttack(Client c, AttackInfo attackInfo, SkillInfo si, long now) {
        Char chr = c.getChr();
        if (!JobConstants.isPhantom(chr.getJob())) {
            super.handleAttack(c, attackInfo, si, now);
        }
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        int skillID = attackInfo.skillId;
        boolean hasHitMobs = !attackInfo.mobAttackInfo.isEmpty();
        if (hasHitMobs) {
            if (si.getElemAttr().contains("h")) {
                giveChargeBuff(skillID, tsm);
                giveFallingRisingJustice(attackInfo, now);
            }
            if (tsm.hasStat(SacredBastion) && !chr.hasSkillOnCooldown(SACRED_BASTION_EXTRA)
                    && skillID != SACRED_BASTION && skillID != SACRED_BASTION_TILE
                    && skillID != SACRED_BASTION_EXTRA && skillID != SACRED_BASTION_AFTER_ATTACK) {
                AffectedArea aa = chr.getField().getAffectedAreaBySkillID(chr.getId(), SACRED_BASTION_TILE);
                if (aa != null) {
                    ExtraSkill extraSkill = new ExtraSkill(SACRED_BASTION_EXTRA, aa.getPosition());
                    extraSkill.Value = 1;
                    extraSkill.Delay = 60;
                    chr.write(UserLocal.registerExtraSkill(SACRED_BASTION_EXTRA, List.of(extraSkill)));
                    chr.addSkillCooldown(SACRED_BASTION_EXTRA, 500);
                }
            }
        }
        switch (skillID) {
            case BLAST:
            case HEXA_BLAST:
                if (hasHitMobs) {
                    doDivineJudgement(attackInfo, now);
                }
                break;
        }
    }

    private void giveFallingRisingJustice(AttackInfo attackInfo, long now) {
        if (!chr.hasSkill(FALLING_JUSTICE)) {
            return;
        }
        if (!chr.hasSkillOnCooldown(FALLING_JUSTICE)) {
            Mob boss = null;
            for (MobAttackInfo mai : attackInfo.mobAttackInfo) {
                Mob mob = (Mob) chr.getField().getLifeByObjectID(mai.mobId);
                if (mob == null || mob.getHp() <= 0) {
                    continue;
                }
                if (mob.isBoss()) {
                    boss = mob;
                    break;
                } else {
                    boss = mob;
                }
            }
            if (boss != null) {
                chr.write(UserLocal.userBonusAttackRequest(FALLING_JUSTICE, boss.getObjectId()));
                boss.getField().broadcast(MobPool.specialEffectBySkill(boss, FALLING_JUSTICE, chr.getId(), 0));
                chr.addSkillCooldown(FALLING_JUSTICE, 30000);
                if (chr.hasSkill(RISING_JUSTICE)) {
                    giveRisingJustice(attackInfo, now);
                }
            }
        }
    }

    private void giveRisingJustice(AttackInfo attackInfo, long now) {
        var skillId = RISING_JUSTICE;
        if (!chr.hasSkill(skillId)) {
            return;
        }
        var slv = chr.getSkillLevel(skillId);
        var si = SkillData.getSkillInfoById(skillId);
        if (si == null || si.getSecondAtomInfos().size() <= 0) {
            return;
        }
        List<SecondAtom> secondAtoms = new LinkedList<>();
        int max = si.getValue(SkillStat.bulletCount, slv);
        final var mobs = chr.getField().getMobs(attackInfo.mobAttackInfo);
        if (mobs.isEmpty()) return;
        int key = 0;
        for (int i = 0; i < max; i++) {
            var sai = Util.getRandomFromCollection(si.getSecondAtomInfos().values());
            var mob = Util.getRandomFromCollection(mobs);
            if (sai == null || mob == null) continue;
            final int randX = chr.getPosition().getX() + Util.getRandom(-200, 200);
            final int randY = chr.getPosition().getY() - Util.getRandom(100, 200);
            final var pos = new Position(randX, randY);
            SecondAtom fa = new SecondAtom(sai, chr.getNewSecondAtomKey(), chr.getId(), mob != null ? mob.getObjectId() : 0, key, si.getSkillId(), pos, now);
            fa.setCustoms(sai.getCustoms());
            fa.setDataIndex(Util.getRandomFromCollection(sai.getCustoms().values()));
            secondAtoms.add(fa);
            key++;
        }
        chr.createSecondAtom(secondAtoms);
    }

    private void doDivineJudgement(AttackInfo attackInfo, long now) {
        if (!chr.hasSkill(DIVINE_JUDGEMENT)) {
            return;
        }
        Field field = chr.getField();
        int originalSkillID = chr.hasSkill(HEXA_DIVINE_JUDGEMENT) ? HEXA_DIVINE_JUDGEMENT : DIVINE_JUDGEMENT;
        int skillID = originalSkillID == HEXA_DIVINE_JUDGEMENT ? HEXA_DIVINE_JUDGEMENT_DEBUFF : DIVINE_JUDGEMENT_DEBUFF;
        int slv = chr.getSkillLevel(originalSkillID);
        SkillInfo si = SkillData.getSkillInfoById(originalSkillID);
        int duration = si.getValue(SkillStat.time, slv) * 1000;
        int maxStack = si.getValue(SkillStat.x, slv);
        if (this.specialStack == null || this.specialStack.originalSkillID != originalSkillID
                || this.specialStack.skillID != skillID || this.specialStack.duration != duration
                || this.specialStack.maxStacks != maxStack) {
            this.specialStack = new SpecialStack(originalSkillID, skillID, 0, duration, maxStack);
        }
        for (MobAttackInfo mai : attackInfo.mobAttackInfo) {
            Mob mob = (Mob) field.getLifeByObjectID(mai.mobId);
            if (mob == null || mob.getHp() <= 0) {
                continue;
            }
            int oid = mob.getObjectId();
            if (!this.specialStack.targets.containsKey(oid)) {
                SpecialStack.SpecialStackInfo ssi = new SpecialStack.SpecialStackInfo(now, duration);
                ssi.objectId = oid;
                this.specialStack.targets.put(oid, ssi);
            }
        }
        List<Integer> explosionTargets = new ArrayList<>();

        // iterate with iterator so we can remove safely
        var it = this.specialStack.targets.int2ObjectEntrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            int objectId = entry.getIntKey();
            SpecialStack.SpecialStackInfo info = entry.getValue();

            var life = field.getLifeByObjectID(objectId);
            if (!(life instanceof Mob mob) || mob.getHp() <= 0) {
                it.remove();
                continue;
            }

            if (now >= info.end) { // timeleft <= 0
                it.remove();
                continue;
            }

            int stack = info.stack + 1;
            if (stack >= maxStack) {
                explosionTargets.add(objectId);
                info.stack = 0;
            } else {
                info.stack = stack;
            }
        }

        if (!this.specialStack.targets.isEmpty()) {
            chr.write(UserLocal.setMonsterDebuffMark(this.specialStack));
        }
        if (!explosionTargets.isEmpty()) {
            chr.write(UserLocal.attackMonsterDebuffMark(skillID, attackInfo.skillId, maxStack, explosionTargets));
        }
    }

    private void giveChargeBuff(int skillId, TemporaryStatManager tsm) {
        Option o = new Option();
        int skillID = chr.hasSkill(GREATER_VESSEL_OF_LIGHT) ? GREATER_VESSEL_OF_LIGHT : VESSEL_OF_LIGHT;
        int slv = chr.getSkillLevel(skillID);
        SkillInfo si = SkillData.getSkillInfoById(skillID);
        int stack = 1;
        int maxStack = si.getValue(z, slv);
        if (tsm.hasStat(ElementalCharge)) {
            stack = tsm.getOption(ElementalCharge).xOption;
            if (lastCharge == skillId) {
                return;
            }
            if (stack < maxStack) {
                stack++;
            } else {
                return;
            }
        }
        lastCharge = skillId;
        o.nOption = maxStack;
        o.rOption = skillID;
        o.tOption = (10 * si.getValue(time, slv));
        o.xOption = stack;
        o.yOption = stack * si.getValue(y, slv);
        o.uOption = stack * si.getValue(u, slv); // Status Resistance + 2%
        o.wOption = stack * si.getValue(w, slv); // Damage Taken + 2%
        tsm.sendStat(ElementalCharge, o);
        if (chr.hasSkill(HAMMERS_OF_THE_RIGHTEOUS)) {
            Option o1 = new Option();
            o1.nOption = stack;
            o1.rOption = HAMMERS_OF_THE_RIGHTEOUS;
            tsm.sendStat(BlessedHammer, o1);
        }
    }

    @Override
    public int getFinalAttackSkill(int faSkill) {
        if (faSkill == FINAL_ATTACK) {
            if (chr.hasSkill(HEXA_FINAL_ATTACK)) return HEXA_FINAL_ATTACK;
        }
        return super.getFinalAttackSkill(faSkill);
    }

    // Skill related methods -------------------------------------------------------------------------------------------
    @Override
    public void handleSkill(Client c, InPacket inPacket, SkillUseInfo skillUseInfo) {
        if (!JobConstants.isPhantom(chr.getJob())) {
            super.handleSkill(c, inPacket, skillUseInfo);
        }
        Char chr = c.getChr();
        SkillInfo si = skillUseInfo.skillInfo;
        int slv = skillUseInfo.slv;
        int skillID = skillUseInfo.skillID;
        EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o1 = new Option();
        Option o2 = new Option();
        var isDivineEchoMimicSkill = SkillConstants.isDivineEchoMimicSkills(skillID);
        if (isDivineEchoMimicSkill) {
            si = SkillData.getSkillInfoById(skillID);
        }
        if (tsm.hasStat(PairingUser) && isDivineEchoMimicSkill) {
            var mimic = getDivineEchoMimic();
            if (mimic != null && mimic != chr) {
                giveDivineEchoLinkedChrBuffs(mimic, mimic.getTemporaryStatManager(), skillID, slv);
            }
        }
        Field field = chr.getField();
        switch (skillID) {
            case COMBAT_ORDERS:
                o1.nOption = si.getValue(x, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(CombatOrders, o1);
                break;
            case DIVINE_SHIELD:
                o1.nOption = si.getValue(x, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                newStats.put(BlessingArmor, o1);
                o2.nOption = si.getValue(epad, slv);
                o2.rOption = skillID;
                o2.tOption = si.getValue(time, slv);
                newStats.put(BlessingArmorIncPAD, o2);
                tsm.sendStat(newStats);
                break;
            case PARASHOCK_GUARD:
                if (tsm.hasStatBySkillId(skillID)) {
                    tsm.removeStatsBySkill(skillID);
                } else {
                    o1.nValue = si.getValue(indiePad, slv);
                    o1.nReason = skillID;
                    newStats.put(IndieDamR, o1);
                    o2.nOption = 10;
                    o2.rOption = skillID;
                    o2.xOption = chr.getId();
                    o2.yOption = 1;
                    newStats.put(KnightsAura, o2);
                    tsm.sendStat(newStats);
                }
                break;
            case DIVINE_BLESSING:
                o1.nValue = si.getValue(indiePMdR, slv);
                o1.nReason = skillID;
                o1.tTerm = si.getValue(time, slv);
                tsm.sendStat(IndiePMdR, o1);
                break;
            case EPIC_ADVENTURE:
                o1.nValue = si.getValue(indieDamR, slv);
                o1.nReason = skillID;
                o1.tTerm = si.getValue(time, slv);
                tsm.sendStat(IndieDamR, o1);
                break;
            case SACROSANCTITY:
                o1.nValue = 1;
                o1.nReason = skillID;
                o1.tTerm = si.getValue(time, slv);
                newStats.put(IndieNotDamaged, o1);
                o2.nValue = 1;
                o2.nReason = skillID;
                o2.tTerm = si.getValue(time, slv);
                newStats.put(IndieIgnorePCounter, o2);
                tsm.sendStat(newStats);
                break;
            case HP_RECOVERY:
                hpRecovery();
                break;
            case NOBLE_DEMAND:
                Rect rect = chr.getPosition().getRectAround(si.getRects().getFirst());
                if (!chr.isLeft()) {
                    rect = rect.moveRight();
                }
                o1.nOption = si.getValue(x, slv);
                o1.rOption = skillID;
                o1.tOption = 80;
                o2.nValue = si.getValue(x, slv);
                o2.nReason = skillID;
                o2.tTerm = 80;
                for (Life life : chr.getField().getLifesInRect(rect)) {
                    if (life instanceof Mob mob && mob.getHp() > 0) {
                        MobTemporaryStat mts = mob.getTemporaryStat();
                        EnumMap<MobStat, Option> map = new EnumMap<>(MobStat.class);
                        if (Util.succeedProp(si.getValue(prop, slv))) {
                            map.put(MobStat.PAD, o1.deepCopy());
                            map.put(MobStat.MAD, o1.deepCopy());
                            map.put(MobStat.IndiePDR, o2.deepCopy());
                            map.put(MobStat.IndieMDR, o2.deepCopy());
                            mts.addStatOptions(mob, map);
                        }
                    }
                }
                break;
            case GUARDIAN:
                chr.heal(chr.getMaxHP());

                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(NotDamaged, o1);

                Party party = chr.getParty();
                if (party != null) {
                    rect = chr.getPosition().getRectAround(si.getRects().getFirst());
                    if (!chr.isLeft()) {
                        rect = rect.moveRight();
                    }
                    List<Char> eligblePartyCharList = field.getPartyCharSameFieldInRect(chr, rect).stream().
                            filter(pmChr -> pmChr.getId() != chr.getId() && pmChr.getHP() <= 0).toList();

                    if (!eligblePartyCharList.isEmpty()) {
                        Char randomPartyChr = Util.getRandomFromCollection(eligblePartyCharList);
                        if (randomPartyChr != null) {
                            TemporaryStatManager partyTSM = randomPartyChr.getTemporaryStatManager();
                            randomPartyChr.heal(randomPartyChr.getMaxHP());
                            partyTSM.sendStat(NotDamaged, o1);
                            randomPartyChr.write(UserPacket.effect(Effect.skillAffected(skillID, (byte) 1, 0)));
                            randomPartyChr.getField().broadcast(UserRemote.effect(randomPartyChr.getId(), Effect.skillAffected(skillID, (byte) 1, 0)), randomPartyChr);
                        }
                    }
                }

                break;
            case MAGIC_CRASH: {
                rect = chr.getRectAround(new Rect(-500, -250, 500, 250));
                if (!chr.isLeft()) {
                    rect = rect.moveRight();
                }
                int count = 0;
                final List<Mob> mobs = chr.getField().getMobsInRect(rect);
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                o1.cOption = chr.getId();
                for (Mob mob : mobs) {
                    if (mob != null && mob.getHp() > 0) {
                        if (count >= 10) {
                            break;
                        }
                        MobTemporaryStat mts = mob.getTemporaryStat();
                        if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                            mts.removeBuffs(mob);
                            mts.addStatOptions(mob, MobStat.MagicCrash, o1.deepCopy());
                            count += 1;
                        }
                    }
                }
                break;
            }
            case HEROS_WILL:
                tsm.removeAllDebuffs();
                break;
            case DIVINE_ECHO:
            case HEXA_DIVINE_ECHO: {
                if (!tsm.hasStat(PairingUser)) {
                    var mimic = getRandomEligableDivineEchoMimic();
                    o1.nOption = mimic.getId();
                    o1.rOption = skillID;
                    o1.tOption = si.getValue(time, slv);
                    o1.xOption = chr.getId(); // from ChrId
                    tsm.sendStat(PairingUser, o1);
                    var interval = 500; // ms
                    var executes = o1.tOption / interval;
                    if (divineEchoTimer != null) {
                        divineEchoTimer.cancel(false);
                    }
                    ScheduledFuture<?> sf = chr.getTimer().addFixedRateEvent(this::doDivineEcho, 0, interval, executes);
                    this.divineEchoTimer = sf;
                    GlobalTimerManager.addCharTimer(chr.getId(), sf);
                }
                break;
            }
            case HAMMERS_OF_THE_RIGHTEOUS_2:
                if (tsm.hasStat(BlessedHammerActive)) {
                    tsm.removeStatsBySkill(skillID);
                } else {
                    int charges = tsm.getOption(ElementalCharge).mOption;
                    si = SkillData.getSkillInfoById(HAMMERS_OF_THE_RIGHTEOUS);
                    slv = chr.getSkillLevel(HAMMERS_OF_THE_RIGHTEOUS);
                    o1.nOption = charges;
                    o1.rOption = skillID;
                    o1.tOption = si.getValue(v, slv);
                    tsm.sendStat(BlessedHammerActive, o1);
                    chr.setSkillCooldown(HAMMERS_OF_THE_RIGHTEOUS, chr.getSkillLevel(HAMMERS_OF_THE_RIGHTEOUS));
                }
                break;
            case MIGHTY_MJOLNIR: // ForceAtom Use Skill
                inPacket.decodeByte();
                int targetsListSize = inPacket.decodeByte();
                List<Integer> targetList = new ArrayList<>();
                for (int i = 0; i < targetsListSize; i++) {
                    targetList.add(inPacket.decodeInt());
                }
                if (!targetList.isEmpty()) {
                    createMightMjolnirForceAtom(targetList);
                    decreaseMightyMjolnir();
                }
                break;
            case SACRED_BASTION_TILE:
                slv = chr.getSkillLevel(SACRED_BASTION);
                AffectedArea aa = AffectedArea.getPassiveAA(chr, SACRED_BASTION_TILE, slv);
                aa.setPosition(chr.getPosition());
                aa.setRect(aa.getRectAround(si.getFirstRect()));
                aa.setDelay((short) 3);
                aa.setDuration(si.getValue(time, slv) * 1000);
                chr.getField().spawnAffectedArea(aa);
                if (!tsm.hasStat(SacredBastion)) {
                    o1.nOption = 1;
                    o1.rOption = SACRED_BASTION;
                    o1.tOption = si.getValue(time, slv);
                    tsm.sendStat(SacredBastion, o1);
                }
                break;
        }
    }

    public void createMightMjolnirForceAtom(List<Integer> targetList) {
        if (!chr.hasSkill(MIGHTY_MJOLNIR) || targetList.isEmpty()) {
            return;
        }

        var fae = ForceAtomEnum.MIGHTY_MJOLNIR;
        Mob mob = (Mob) chr.getField().getLifeByObjectID(targetList.getFirst());
        List<ForceAtomInfo> faiList = new ArrayList<>();
        for (int i = 0; i < targetList.size(); i++) {
            var fai = new ForceAtomInfo(chr.getNewForceAtomKey(), fae.getInc(), 28, 79,
                    250, 0, Util.getCurrentTime(), 0, 0,
                    new Position());
            faiList.add(fai);
        }
        ForceAtom fa = new ForceAtom(chr.getId(), fae, targetList, MIGHTY_MJOLNIR, faiList);
        fa.setForcedTargetPosition(mob.getPosition());
        chr.createForceAtom(fa);
    }

    public void decreaseMightyMjolnir() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (!chr.hasSkill(MIGHTY_MJOLNIR) || !tsm.hasStat(CannonShooter_BFCannonBall)) {
            return;
        }
        var count = tsm.getOptByCTSAndSkill(CannonShooter_BFCannonBall, MIGHTY_MJOLNIR).nOption;
        if (count > 0) {
            count--;
        }
        updateVSkillStackBuff(chr, count);
    }

    public void increaseMightyMjolnir() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        int count = 1;
        count = tsm.getOption(CannonShooter_BFCannonBall).nOption;
        if (count < 2) {
            count++;
        }
        updateVSkillStackBuff(chr, count);
    }

    private void doDivineEcho() {
        var tsm = chr.getTemporaryStatManager();
        if (!tsm.hasStat(PairingUser)) {
            if (divineEchoTimer != null) {
                divineEchoTimer.cancel(false);
            }
            removeDivineEchoMimic(); // remove by searching from other chars in field
            return;
        }
        var party = chr.getParty();
        if (party == null) {
            return;
        }
        // if does not have echo partner
        // search
        // update tsm

        // if echo partner left rect
        // remove skills
        // update tsm
        // mark paladin as divine echo - free

        var mimic = getDivineEchoMimic();

        // Divine Echo does not have a mimic -> search for possible mimic
        if (mimic == null) {
            mimic = getRandomEligableDivineEchoMimic();

            if (mimic == null || mimic == chr) { // No Eligible mimic found.
                return;
            }
        }

        var skillIDMinic = chr.hasSkill(HEXA_DIVINE_ECHO) ? HEXA_DIVINE_ECHO_MIMIC : DIVINE_ECHO_MIMIC;
        var mimicTsm = mimic.getTemporaryStatManager();
        var mimicPos = mimic.getPosition();
        var rect = chr.getRectAround(SkillData.getSkillInfoById(DIVINE_ECHO).getLastRect());

        // Mimic inside Rect -> give Divine Echo if doesn't have yet
        if (rect.hasPositionInside(mimicPos)) {
            // If selected mimic does not yet have Divine Echo update
            if (!mimicTsm.hasStat(PairingUser)) {
                var opt = new Option();
                opt.nOption = mimic.getId();
                opt.rOption = skillIDMinic;
                opt.xOption = chr.getId();
                mimicTsm.sendStat(PairingUser, opt);
                var optPala = tsm.getOption(PairingUser);
                optPala.nOption = mimic.getId();
                tsm.updateStat(PairingUser, optPala);
            }
            // Mimic outside Rect -> remove Divine Echo
        } else {
            mimicTsm.removeStat(PairingUser);
            var optPala = tsm.getOption(PairingUser);
            optPala.nOption = chr.getId();
            tsm.updateStat(PairingUser, optPala);
        }
    }

    private Char getRandomEligableDivineEchoMimic() {
        if (chr.getParty() == null) {
            return chr;
        }

        var si = SkillData.getSkillInfoById(DIVINE_ECHO);
        Rect rect2 = chr.getRectAround(si.getLastRect());
        List<Char> chrList = chr.getParty().getPartyMembersInSameField(chr).stream()
                .filter(pChr -> rect2.hasPositionInside(pChr.getPosition())                     // Inside rect
                        && pChr != chr                                                          // not paladin self
                        && !JobConstants.isPaladin(pChr.getJob())                               // not a paladin class
                        && !pChr.getTemporaryStatManager().hasStat(PairingUser)) // is not currently affected by another Divine Echo
                .collect(Collectors.toList());
        if (chrList.size() <= 0) {
            return chr;
        }

        return Util.getRandomFromCollection(chrList);
    }

    private Char getDivineEchoMimic() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (tsm.hasStat(PairingUser) && tsm.getOption(PairingUser).nOption != chr.getId()) {
            return chr.getField().getCharByID(tsm.getOption(PairingUser).nOption);
        }
        return null;
    }

    private void giveDivineEchoLinkedChrBuffs(Char mimic, TemporaryStatManager tsm, int skillID, int slv) {
        if (mimic == null || mimic == chr) {
            return;
        }
        var effect = Effect.skillUse(skillID, mimic.getLevel(), chr.getSkillLevel(skillID), 0);
        mimic.write(UserPacket.effect(effect));
        mimic.getField().broadcast(UserRemote.effect(mimic.getId(), effect), mimic);
        handleSkill(mimic.getClient(), null, new SkillUseInfo(SkillData.getSkillInfoById(skillID), slv));
    }

    public static void removeDivineEchoLinkedBuffs(Char linkedChr) {
        TemporaryStatManager linkedTSM = linkedChr.getTemporaryStatManager();
        Map<Integer, CharacterTemporaryStat> linkedSkills = new HashMap<>() {{
            put(WEAPON_BOOSTER, IndieBooster);
            put(DIVINE_BLESSING, IndiePMdR);
            put(SACROSANCTITY, NotDamaged);
        }};
        for (Map.Entry<Integer, CharacterTemporaryStat> entry : linkedSkills.entrySet()) {
            int skillId = entry.getKey();
            CharacterTemporaryStat cts = entry.getValue();
            if (linkedTSM.hasStatBySkillId(skillId) && linkedTSM.getOption(cts).xOption != linkedChr.getId()) {
                linkedTSM.removeStatsBySkill(skillId);
            }
        }
    }

    private void removeDivineEchoMimic() {
        var field = chr.getField();
        for (var fChr : field.getChars()) {
            var tsm = fChr.getTemporaryStatManager();
            if (tsm != null && tsm.hasStat(PairingUser)) {
                var opt = tsm.getOption(PairingUser);
                if (opt != null && opt.xOption == chr.getId()) {
                    tsm.removeStat(PairingUser);
                }
            }
        }
    }

    public void hpRecovery() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o = new Option();
        if (chr.hasSkill(HP_RECOVERY)) {
            Skill skill = chr.getSkill(HP_RECOVERY);
            int slv = skill.getCurrentLevel();
            SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
            int recovery = si.getValue(x, slv);
            int amount = 10;

            if (tsm.hasStat(Restoration)) {
                amount = tsm.getOption(Restoration).nOption;
                if (amount < 300) {
                    amount = amount + 10;
                }
            }

            o.nOption = amount;
            o.rOption = skill.getSkillId();
            o.tOption = si.getValue(time, slv);
            int heal = Math.max((recovery + 10) - amount, 10);
            chr.heal((int) (chr.getMaxHP() / ((double) 100 / heal)));
            tsm.sendStat(Restoration, o);
        }
    }

    @Override
    public int alterCooldownSkill(int skillId) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (skillId == HAMMERS_OF_THE_RIGHTEOUS) {
            if (tsm.hasStat(BlessedHammer) && !tsm.hasStat(BlessedHammerActive)) {
                return 0;
            }
        }
        return super.alterCooldownSkill(skillId);
    }

    // Hit related methods ---------------------------------------------------------------------------------------------
    @Override
    public void handleHit(Client c, InPacket inPacket, HitInfo hitInfo) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        int skillID = DIVINE_SHIELD;
        if (chr.hasSkill(skillID)) {
            Skill skill = chr.getSkill(skillID);
            SkillInfo si = SkillData.getSkillInfoById(skillID);
            int slv = skill.getCurrentLevel();
            int shieldprop = 50;//      si.getValue(SkillStat.prop, slv);       //TODO should be prop in WzFiles, but it's actually 0
            Option o1 = new Option();
            Option o2 = new Option();
            int divShieldCoolDown = si.getValue(cooltime, slv);
            if (tsm.hasStat(BlessingArmor)) {
                if (divShieldAmount < 10) {
                    divShieldAmount++;
                } else {
                    resetDivineShield();
                    divShieldAmount = 0;
                }
            } else {
                final long now = System.currentTimeMillis();
                if (lastDivineShieldHit + (divShieldCoolDown * 1000L) < now) {
                    if (Util.succeedProp(shieldprop)) {
                        EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
                        lastDivineShieldHit = now;
                        o1.nOption = 1;
                        o1.rOption = skillID;
                        o1.tOption = si.getValue(time, slv);
                        newStats.put(BlessingArmor, o1);
                        o2.nOption = si.getValue(epad, slv);
                        o2.rOption = skillID;
                        o2.tOption = si.getValue(time, slv);
                        newStats.put(PAD, o2);
                        tsm.sendStat(newStats);
                        divShieldAmount = 0;
                    }
                }
            }
        }
        skillID = SHIELD_MASTERY;
        if (chr.hasSkill(skillID)) { //If Wearing a Shield
            Equip equip = (Equip) chr.getEquippedItemByBodyPart(BodyPart.Shield);
            if (equip != null && ItemConstants.isShield(equip.getItemId()) && hitInfo.hpDamage == 0 && hitInfo.mpDamage == 0) {
                // Guarded
                int mobID = hitInfo.mobID;
                Mob mob = (Mob) chr.getField().getLifeByObjectID(mobID);
                if (mob != null) {
                    Option o = new Option();
                    Skill skill = chr.getSkill(skillID);
                    int slv = skill.getCurrentLevel();
                    SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
                    int proc = si.getValue(subProp, slv);
                    MobTemporaryStat mts = mob.getTemporaryStat();
                    if (!mts.hasCurrentMobStatBySkillId(skill.getSkillId())) {
                        if (Util.succeedProp(proc) && !mob.isBoss()) {
                            o.nOption = 1;
                            o.rOption = skill.getSkillId();
                            o.tOption = 3;  // Value isn't given
                            mts.addStatOptions(mob, MobStat.Stun, o);
                        }
                    }
                }
            }
        }
        super.handleHit(c, inPacket, hitInfo);
    }

    private void resetDivineShield() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        tsm.removeStat(BlessingArmor);
        tsm.removeStat(PAD);
    }

    @Override
    public void handleLevelUp(short level) {
        super.handleLevelUp(level);
        // hacks to bypass the quest glitch (accept but no packet)
        ExtendSP esp = chr.getAvatarData().getCharacterStat().getExtendSP();
        if (level == 30) {
            chr.completeQuest(1410);
        } else if (level == 60) {
            esp.setSpToJobLevel(3, 5);
            chr.completeQuest(1430);
        } else if (level == 100) {
            chr.completeQuest(1450);
        }
    }

    @Override
    public void handleCancelTimer(Char chr) {
        if (divineEchoTimer != null) {
            divineEchoTimer.cancel(true);
        }
        super.handleCancelTimer(chr);
    }

    @Override
    public void handleKeyDownSkill(Char chr, SkillInfo si, InPacket inPacket) {
        super.handleKeyDownSkill(chr, si, inPacket);
        int skillId = si.getSkillId();
        int slv = chr.getSkillLevel(skillId);

        EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o1 = new Option();
        Option o2 = new Option();
        Option o3 = new Option();
        switch (skillId) {
            case GRAND_GUARDIAN:
                o1.nOption = 1;
                o1.rOption = skillId;
                o1.tOption = si.getValue(time, slv);
                newStats.put(GrandCross, o1);
                o2.nValue = -(si.getValue(x, slv));
                o2.nReason = skillId;
                o2.tTerm = si.getValue(time, slv);
                newStats.put(IndieAllHitDamR, o2);
                o3.nValue = 1;
                o3.nReason = skillId;
                o3.tTerm = si.getValue(time, slv);
                newStats.put(IndieAntiMagicShell, o3);
                tsm.sendStat(newStats);
                break;
        }
    }

    @Override
    public void handleSkillRemove(Char chr, int skillID) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        switch (skillID) {
            case GRAND_GUARDIAN:
                tsm.removeStatsBySkill(skillID);
                break;
        }
    }

    @Override
    public void handleRemoveCTS(CharacterTemporaryStat cts,  List<Option> options) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (cts == BlessingArmor) {
            SkillInfo si = SkillData.getSkillInfoById(DIVINE_SHIELD);
            int slv = chr.getSkillLevel(DIVINE_SHIELD);
            chr.addSkillCoolTime(DIVINE_SHIELD, Util.getCurrentTimeLong() + (si.getValue(cooltime, slv) * 1000L));
        } else if (cts == ElementalCharge) {
            tsm.removeStat(BlessedHammer);
            tsm.removeStat(BlessedHammerActive);
        } else if (cts == PairingUser) {
            var mimic = chr.getField().getCharByID(options.getFirst().nOption);
            if (mimic != null && mimic.getId() != chr.getId()) {
                mimic.getTemporaryStatManager().removeStat(PairingUser);
            }
            if (divineEchoTimer != null) {
                divineEchoTimer.cancel(false);
            }
        }
        super.handleRemoveCTS(cts, options);
    }

    @Override
    public int handleSetCoolDownSkill(int skillId) {
        switch (skillId) {
            case HEXA_DIVINE_MARK -> {
                int skillID = DIVINE_MARK;
                int slv = chr.getSkillLevel(skillID);
                chr.setSkillCooldown(skillID, slv);
                return 1;
            }
            case HEXA_HEAVENS_HAMMER -> {
                int skillID = HEAVENS_HAMMER;
                int slv = chr.getSkillLevel(skillID);
                chr.setSkillCooldown(skillID, slv);
                return 1;
            }
        }
        return super.handleSetCoolDownSkill(skillId);
    }
}
