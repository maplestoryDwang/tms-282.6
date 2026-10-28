package net.swordie.ms.client.jobs.adventurer.pirate;

import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.info.HitInfo;
import net.swordie.ms.client.character.skills.PartyBooster;
import net.swordie.ms.client.character.skills.*;
import net.swordie.ms.client.character.skills.info.*;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.*;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.constants.QuestConstants;
import net.swordie.ms.constants.SkillConstants;
import net.swordie.ms.enums.TSIndex;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.life.mob.MobStat;
import net.swordie.ms.life.mob.MobTemporaryStat;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.util.Util;
import net.swordie.ms.util.container.Tuple;

import java.util.*;

import static net.swordie.ms.client.character.skills.SkillStat.*;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;

public class Buccaneer extends Pirate {

    public static final int TORNADO_UPPERCUT = 5101012; //Special Attack
    public static final int CORKSCREW_BLOW = 5101004;
    public static final int PERSEVERANCE = 5100013;
    public static final int SEA_SERPENT = 5101017;
    public static final int SEA_SERPENT_BURST = 5101019;

    public static final int GREATER_SEA_SERPENT_I = 5110016;
    public static final int SERPENT_ASSAULT = 5110018;
    public static final int SERPENT_ASSAULT_BURST = 5111019;
    public static final int SERPENT_ASSAULT_RAGE = 5121027;
    public static final int GREATER_SEA_SERPENT_I_BURST = 5111021;
    public static final int GREATER_SEA_SERPENT_II = 5120022;
    public static final int GREATER_SEA_SERPENT_II_BURST = 5121023;
    public static final int GREATE_SEA_SERPENT_III = 5111023;
    public static final int SERPENT_SCALE = 5111017;
    public static final int ROLL_OF_THE_DICE = 5111007; //Buff
    public static final int TURNING_KICK = 5111002;
    public static final int STATIC_THUMPER = 5111012; //Special Attack
    public static final int GROGGY_MASTERY = 5110000;
    public static final int SPIRAL_ASSAULT = 5111009;

    public static final int AGGRESSIVE_STANCE = 5120028; //Passive
    public static final int OCTOPUNCH = 5121007; //Special Attack
    public static final int NAUTILUS_STRIKE = 5121013;
    public static final int NAUTILUS_STRIKE_FA = 5120021; // Final Attack
    public static final int HOOK_BOMBER = 5121016; //Special Attack
    public static final int CROSSBONES = 5121015; //Buff
    public static final int SPEED_INFUSION = 5121009; //Buff
    public static final int TIME_LEAP = 5121010; //Special Move / Buff
    public static final int DEFENSIVE_STANCE = 5120011;
    public static final int ROLL_OF_THE_DICE_DD = 5120012;
    public static final int ROLL_OF_THE_DICE_ADDITION = 5120044;
    public static final int ROLL_OF_THE_DICE_SAVING_GRACE = 5120043;
    public static final int ROLL_OF_THE_DICE_ENHANCE = 5120045;
    public static final int HEROS_WILL = 5121008;
    public static final int SEA_SERPENT_RAGE = 5121025;

    public static final int SERPENT_SPIRIT = 5121052;
    public static final int EPIC_ADVENTURER = 5121053;
    public static final int STIMULATING_CONVERSATION = 5121054;
    public static final int SERPENT_SPIRIT_BUFF = 5121055;

    // V Skills
    public static final int LIGHTING_FORM = 400051002;
    public static final int LIGHTING_FORM_ORB = 400051003;
    public static final int LORD_OF_THE_DEEP = 400051015;
    public static final int SERPENT_VORTEX = 400051042;
    public static final int HOWLING_FIST = 400051070;
    public static final int HOWLING_FIST_LEVIATHAN_ATTACK = 400051071;

    // HEXA Skills
    public static final int HEXA_SEA_SERPENT = 5140004;
    public static final int HEXA_SEA_SERPENT_RAGE = 5141006;
    public static final int HEXA_SEA_SERPENT_BURST = 5141008;
    public static final int HEXA_OCTOPUNCH = 5141000;
    public static final int SUPER_OCTOPUNCH = 5141002; // SuperFistEnrageStack
    public static final int HEXA_NAUTILUS_STRIKE = 5141009;
    public static final int HEXA_NAUTILUS_STRIKE_FA = 5140010; // Final Attack
    public static final int HEXA_SERPENT_SCALE = 5141011;
    public static final int HEXA_SERPENT_SCALE_BURST = 5141013;
    public static final int HEXA_SERPENT_SCALE_RAGE = 5141014;
    public static final int HEXA_HOOK_BOMBER = 5141015;

    private long lastPerseverance = 0L;
    private long lastVStackBuff = 0L;
    private long NextSeaSerpentAttackTime = 0L;

    private SpecialStack specialStack;

    public Buccaneer(Char chr) {
        super(chr);
    }

    @Override
    public void update(long now) {
        super.update(now);
        if (chr.hasSkill(SEA_SERPENT_RAGE)) {
            SpecialStack.prune(chr, this.specialStack, now);
        }
    }

    @Override
    public boolean isHandlerOfJob(short id) {
        return JobConstants.isBuccaneer(id);
    }

    private void powerUnity() {
        // 282 Serpent Spirit is a learned critical-damage passive plus the normal SkillUse visual, not a stack buff.
        if (net.swordie.ms.connection.packet.Tms282RemoteBuffs.active()) return;
        if (!chr.hasSkill(SERPENT_SPIRIT)) {
            return;
        }
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o1 = new Option();
        Skill skill = chr.getSkill(SERPENT_SPIRIT);
        int slv = skill.getCurrentLevel();
        SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
        int amount = 1;
        if (tsm.hasStat(UnityOfPower)) {
            amount = tsm.getOption(UnityOfPower).nOption;
            if (amount < 4) {
                amount++;
            }
        }
        o1.nOption = Math.min(amount, 3);
        o1.rOption = SERPENT_SPIRIT_BUFF;
        o1.tOption = si.getValue(time, slv);
        tsm.sendStat(UnityOfPower, o1);
    }

    // Attack related methods ------------------------------------------------------------------------------------------
    @Override
    public void handleDebuffOnMob(Client c, Mob mob, SkillInfo si, int skillID, int slv, long damage) {
        Char chr = c.getChr();
        Option o1 = new Option();
        MobTemporaryStat mts = mob.getTemporaryStat();
        if (skillID != 0) {
            applyStunMasteryOnMob(mob);
        }
        switch (skillID) {
            case TURNING_KICK:
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    if (!mob.isBoss()) {
                        o1.nOption = 1;
                        o1.rOption = skillID;
                        o1.tOption = si.getValue(time, slv);
                        mts.addStatOptions(mob, MobStat.Stun, o1);
                    }
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
            handlePerseverance(now);
            handleLordOfTheDeep(attackInfo);
            handleSerpentStone(attackInfo, false, now);
            handleAggressiveStance(attackInfo);
            handleSerpentRage(attackInfo, now);
            handleSeaSerpent(attackInfo, now);
            handleDefensiveStance(attackInfo);
        }
        switch (skillID) {
            case SERPENT_VORTEX:
                updateVSkillStackBuff(chr, Math.max(tsm.getOption(CannonShooter_BFCannonBall).nOption - 1, 0));
                break;
        }
    }

    private void handlePerseverance(long now) {
        if (chr.hasSkill(PERSEVERANCE)) {
            SkillInfo siP = SkillData.getSkillInfoById(PERSEVERANCE);
            int slvP = chr.getSkillLevel(PERSEVERANCE);
            if (lastPerseverance <= System.currentTimeMillis() - siP.getValue(w, slvP)) {
                if (chr.getHP() < chr.getMaxHP()) {
                    chr.heal(siP.getValue(x, slvP));
                }
                if (chr.getMP() < chr.getMaxMP()) {
                    chr.healMP(siP.getValue(x, slvP));
                }
                lastPerseverance = now;
            }
        }
    }

    private void handleSerpentStone(AttackInfo attackInfo, boolean isAuto, long now) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        int skillID = chr.hasSkill(HEXA_SERPENT_SCALE) ? HEXA_SERPENT_SCALE : SERPENT_SCALE;
        SkillInfo si = SkillData.getSkillInfoById(SEA_SERPENT);
        if (si == null) return;
        final int usedSkillId = attackInfo.skillId;
        final boolean inList1 = si.getSkillList1().contains(usedSkillId); // burst
        final boolean inList2 = si.getSkillList2().contains(usedSkillId); // rage
        if (!inList1 && !inList2) return;
        if (tsm.getOptByCTSAndSkill(IndieBuffIcon, skillID) != null || isAuto) {
            if (inList1) {
                sendSeaSerpentExtra(SERPENT_ASSAULT, getSerpentStoneBurst(), 240);
            } else {
                handleSerpentRageAssault(attackInfo, getSerpentStoneRage(), now);
            }
            tsm.removeStatsBySkill(skillID);
        }
        if (tsm.hasStat(SuperFistEnrageStack) && inList2) {
            chr.write(UserLocal.userBonusAttackRequest(SUPER_OCTOPUNCH));
        }
    }

    private void handleSerpentRageAssault(AttackInfo attackInfo, int extraSkillID, long now) {
        SkillInfo si = SkillData.getSkillInfoById(extraSkillID);
        if (si == null || si.getSecondAtomInfos().size() <= 0) {
            return;
        }
        List<SecondAtom> secondAtoms = new LinkedList<>();
        final var mobs = chr.getField().getMobs(attackInfo.mobAttackInfo);
        if (mobs.isEmpty()) {
            return;
        }
        var sai = si.getSecondAtomInfos().get(0);
        if (sai == null) {
            return;
        }
        var mob = Util.getRandomFromCollection(mobs);
        final var pos = chr.getPosition();
        SecondAtom fa = new SecondAtom(sai, chr.getNewSecondAtomKey(), chr.getId(), mob != null ? mob.getObjectId() : 0,
                1, si.getSkillId(), pos, now);
        secondAtoms.add(fa);
        chr.createSecondAtom(secondAtoms);

        Option o = new Option();
        o.nOption = 1;
        o.rOption = SUPER_OCTOPUNCH;
        o.tOption = 10;
        chr.getTemporaryStatManager().sendStat(SuperFistEnrageStack, o);
    }

    private int getSerpentStoneBurst() {
        if (chr.hasSkill(HEXA_SERPENT_SCALE)) {
            return HEXA_SERPENT_SCALE_BURST;
        }
        return SERPENT_ASSAULT_BURST;
    }

    private int getSerpentStoneRage() {
        if (chr.hasSkill(HEXA_SERPENT_SCALE)) {
            return HEXA_SERPENT_SCALE_RAGE;
        }
        return SERPENT_ASSAULT_RAGE;
    }

    private void handleLordOfTheDeep(AttackInfo attackInfo) {
        if (!chr.hasSkill(LORD_OF_THE_DEEP) || attackInfo.skillId != LORD_OF_THE_DEEP) {
            return;
        }
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        int skillID = LORD_OF_THE_DEEP;
        int slv = chr.getSkillLevel(skillID);
        SkillInfo si = SkillData.getSkillInfoById(skillID);
        if (si != null && tsm.hasStat(SerpentScrew)) {
            int count = 0;
            for (MobAttackInfo mobAttackInfo : attackInfo.mobAttackInfo) {
                Mob mob = chr.getField().getMobByObjectId(mobAttackInfo.mobId);
                if (mob == null || mob.getHp() <= 0) {
                    continue;
                }
                if (mob.isBoss()) {
                    count += 3;
                } else {
                    count += 1;
                }
            }
            count /= 3;
            Option o1 = tsm.getOption(SerpentScrew);
            o1.nOption -= (int) count;
            if (o1.nOption <= 0) {
                o1.nOption = 100;
                chr.setSkillCooldown(skillID, slv);
                chr.applyMpCon(si, slv, false);
            }
            tsm.sendStat(SerpentScrew, o1);
        }
    }

    private void handleSerpentRage(AttackInfo attackInfo, long now) {
        if (attackInfo.skillId == SEA_SERPENT_RAGE || attackInfo.skillId == HEXA_SEA_SERPENT_RAGE) {
            TemporaryStatManager tsm = chr.getTemporaryStatManager();
            int duration = 30000;
            if (this.specialStack == null
                    || this.specialStack.originalSkillID != SEA_SERPENT_RAGE
                    || this.specialStack.duration != duration) {
                this.specialStack = new SpecialStack(SEA_SERPENT_RAGE, SEA_SERPENT_RAGE, 0, duration, 1);
            }
            for (MobAttackInfo mai : attackInfo.mobAttackInfo) {
                Mob mob = (Mob) chr.getField().getLifeByObjectID(mai.mobId);
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
            if (!this.specialStack.targets.isEmpty()) {
                chr.write(UserLocal.setMobAdvMark(this.specialStack));
                if (tsm.hasStat(UnityOfPower)) {
                    powerUnity();
                }
            }
        }
    }

    private void handleSeaSerpent(AttackInfo attackInfo, long now) {
        if (!chr.hasSkill(SEA_SERPENT)) return;
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (!tsm.hasStat(SeaSerpent)) return;
        SkillInfo si = SkillData.getSkillInfoById(SEA_SERPENT);
        if (si == null) return;
        final int usedSkillId = attackInfo.skillId;
        final boolean inList1 = si.getSkillList1().contains(usedSkillId); // burst
        final boolean inList2 = si.getSkillList2().contains(usedSkillId); // rage
        if (!inList1 && !inList2) return;
        if (NextSeaSerpentAttackTime < 0L || NextSeaSerpentAttackTime > now) return;
        if (inList1) {
            ExtraSkillInfo extraSkillInfo = pickHighestBurst(si);
            if (extraSkillInfo == null) {
                return;
            }
            sendSeaSerpentExtra(SEA_SERPENT, extraSkillInfo.getSkillId(), extraSkillInfo.getDelay());
            setSeaSerpentSkillCoolTime(extraSkillInfo);
            applySerpentStone(attackInfo, now);
            return;
        }
        ExtraSkillInfo extraSkillInfo = pickHighestRage(si);
        if (extraSkillInfo == null) {
            return;
        }
        sendSeaSerpentExtra(SEA_SERPENT, extraSkillInfo.getSkillId(), extraSkillInfo.getDelay());
        setSeaSerpentSkillCoolTime(extraSkillInfo);
        applySerpentStone(attackInfo, now);
    }

    private void sendSeaSerpentExtra(int skillID, int extraSkillId, int delay) {
        ExtraSkill es = new ExtraSkill(extraSkillId, chr.getPosition());
        es.Value = 1;
        es.FaceLeft = chr.isLeft() ? 1 : 0;
        es.Delay = delay;
        chr.write(UserLocal.registerExtraSkill(skillID, Collections.singletonList(es)));
    }

    private ExtraSkillInfo pickHighestBurst(SkillInfo si) {
        return pickHighestLearnedFromMap(si.getExtraSkillInfo(), true);
    }

    private ExtraSkillInfo pickHighestRage(SkillInfo si) {
        return pickHighestLearnedFromMap(si.getExtraSkillInfo(), false);
    }

    private ExtraSkillInfo pickHighestLearnedFromMap(Map<Integer, ExtraSkillInfo> m, boolean burst) {
        if (m == null || m.isEmpty()) {
            return null;
        }
        int bestSkillId = 0;
        ExtraSkillInfo best = null;
        for (int k = 0; k <= 4; k++) {
            ExtraSkillInfo t = m.get(k);
            if (t == null) continue;
            int skillId = t.getSkillId();
            if (burst) {
                if (!isBurstSkill(skillId)) continue;
            } else {
                if (!isRageSkill(skillId)) continue;
            }
            if (chr.getSkillLevel(SkillConstants.getActualSkillIDfromSkillID(skillId)) <= 0) {
                continue;
            }
            if (skillId > bestSkillId) {
                bestSkillId = skillId;
                best = t;
            }
        }
        return best;
    }

    private boolean isBurstSkill(int skillId) {
        switch (skillId) {
            case SEA_SERPENT_BURST:
            case GREATER_SEA_SERPENT_I_BURST:
            case GREATER_SEA_SERPENT_II_BURST:
            case HEXA_SEA_SERPENT_BURST:
                return true;
        }
        return false;
    }

    private boolean isRageSkill(int skillId) {
        switch (skillId) {
            case SEA_SERPENT_RAGE:
            case HEXA_SEA_SERPENT_RAGE:
                return true;
        }
        return false;
    }

    private void setSeaSerpentSkillCoolTime(ExtraSkillInfo extraSkillInfo) {
        long next = System.currentTimeMillis();
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        final boolean halfCdBuff = tsm.hasStat(Stimulate);
        int extraSkillId = extraSkillInfo.getSkillId();
        switch (extraSkillId) {
            case SEA_SERPENT_BURST: // Sea Serpent Burst
            {
                int ct = 11; // 5100018
                next += (long) ct / (halfCdBuff ? 2 : 1);
                break;
            }
            case GREATER_SEA_SERPENT_I_BURST: // Sea Serpent Burst II
            {
                int z = 9; // 5110016
                next += (long) z * 500L * (halfCdBuff ? 1 : 2);
                break;
            }
            case GREATER_SEA_SERPENT_II_BURST: // Sea Serpent Burst II
            {
                int v = 4; // 5120024
                next += (long) v * 500L * (halfCdBuff ? 1 : 2);
                break;
            }
            case SEA_SERPENT_RAGE: // Sea Serpent's Rage
            {
                int z = 7; // 5120029
                next += (long) z * 500L * (halfCdBuff ? 1 : 2);
                break;
            }
            case HEXA_SEA_SERPENT_RAGE: // HEXA Sea Serpent's Rage
            {
                int v = 4; // 5140004
                next += (long) v * 500L * (halfCdBuff ? 1 : 2);
                break;
            }
            case HEXA_SEA_SERPENT_BURST: // HEXA Sea Serpent Burst
            {
                int v = 6; // 5140004
                next += (long) v * 500L * (halfCdBuff ? 1 : 2);
                break;
            }
        }
        this.NextSeaSerpentAttackTime = next;
    }

    private void applySerpentStone(AttackInfo attackInfo, long now) {
        if (!chr.hasSkill(SERPENT_SCALE)) {
            return;
        }
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o = tsm.getOption(SerpentStone);
        o.nOption = Math.min(5, o.nOption + 1);
        tsm.sendStat(SerpentStone, o);
        if (o.nOption >= 5
                && chr.hasQuest(QuestConstants.SKILL_COMMAND_LOCK_ARK)
                && "1".equals(chr.getQRValueByKey(
                QuestConstants.SKILL_COMMAND_LOCK_ARK,
                String.valueOf(SERPENT_SCALE)))) {
            handleSerpentStone(attackInfo, true, now);
        }
    }

    private void handleAggressiveStance(AttackInfo attackInfo) {
        if (!chr.hasSkill(AGGRESSIVE_STANCE)) {
            return;
        }
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        int skillID = AGGRESSIVE_STANCE;
        int slv = chr.getSkillLevel(skillID);
        SkillInfo si = SkillData.getSkillInfoById(skillID);
        if (si != null && si.getSkillList1().contains(attackInfo.skillId) && !chr.hasSkillOnCooldown(skillID)) {
            Option o1 = new Option();
            o1.nValue = si.getValue(indieDamR, slv);
            o1.nReason = skillID;
            o1.tTerm = si.getValue(time, slv);
            tsm.sendStat(IndieDamR, o1);
            chr.setSkillCooldown(skillID, slv);
        }
    }

    private void handleDefensiveStance(AttackInfo attackInfo) {
        if (!chr.hasSkill(DEFENSIVE_STANCE)) {
            return;
        }
        int skillID = DEFENSIVE_STANCE;
        int slv = chr.getSkillLevel(skillID);
        SkillInfo si = SkillData.getSkillInfoById(skillID);
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (si != null && si.getSkillList1().contains(attackInfo.skillId) && !chr.hasSkillOnCooldown(skillID)) {
            EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
            Option o1 = new Option();
            Option o2 = new Option();
            o1.nValue = -si.getValue(y, slv);
            o1.nReason = skillID;
            o1.tTerm = si.getValue(x, slv);
            newStats.put(IndieAllHitDamR, o1);
            o2.nValue = 1;
            o2.nReason = skillID;
            o2.tTerm = si.getValue(x, slv);
            newStats.put(IndieCheckTimeByClient, o2);
            tsm.sendStat(newStats);
            chr.setSkillCooldown(skillID, slv);
        }
    }

    private void applyStunMasteryOnMob(Mob mob) {
        Option o1 = new Option();
        SkillInfo si = SkillData.getSkillInfoById(GROGGY_MASTERY);
        MobTemporaryStat mts = mob.getTemporaryStat();
        if (!mts.hasCurrentMobStatBySkillId(GROGGY_MASTERY)) {
            if (!mob.isBoss()) {
                o1.nOption = 1;
                o1.rOption = GROGGY_MASTERY;
                o1.tOption = 3;
                mts.addStatOptions(mob, MobStat.Stun, o1);
            }
        }
    }

    public void incVSkillStackBuff() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        int count = 1;
        count = tsm.getOption(CannonShooter_BFCannonBall).nOption;
        if (count < 6) {
            count++;
            updateVSkillStackBuff(chr, count);
        }
    }

    @Override
    public int getFinalAttackSkill(int faSkill) {
        if (faSkill == NAUTILUS_STRIKE_FA) {
            if (chr.hasSkill(HEXA_NAUTILUS_STRIKE)) return HEXA_NAUTILUS_STRIKE_FA;
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
        switch (skillID) {
            case SEA_SERPENT:
                if (tsm.hasStatBySkillId(skillID)) {
                    tsm.removeStatsBySkill(skillID);
                    break;
                }
                o1.nOption = 1;
                o1.rOption = chr.hasSkill(HEXA_SEA_SERPENT) ? HEXA_SEA_SERPENT : skillID;
                tsm.sendStat(SeaSerpent, o1);
                break;
            case SERPENT_SCALE:
            case HEXA_SERPENT_SCALE:
                o1.nOption = 0;
                o1.rOption = skillID;
                newStats.put(SerpentStone, o1);
                o2.nValue = 1;
                o2.nReason = skillID;
                o2.tTerm = si.getValue(time, slv);
                newStats.put(IndieBuffIcon, o2);
                tsm.sendStat(newStats);
                break;
            case SERPENT_SPIRIT:
                powerUnity();
                break;
            case TIME_LEAP:
                if (!tsm.hasStat(ViperTimeLeap)) {
                    o1.nOption = 1;
                    o1.rOption = skillID;
                    o1.tOption = si.getValue(time, slv);
                    tsm.sendStat(ViperTimeLeap, o1);
                    for (int skillId : chr.getSkillCoolTimes().keySet()) {
                        si = SkillData.getSkillInfoById(skillId);
                        if (si != null && si.getHyper() == 0 && !si.isNotCooltimeReset()) {
                            chr.resetSkillCoolTime(skillId);
                        }
                    }
                }
                return;
            case SPEED_INFUSION:
                PartyBooster pb = (PartyBooster) tsm.getTSBByTSIndex(TSIndex.PartyBooster);
                pb.setNOption(si.getValue(x, slv));
                pb.setROption(skillID);
                pb.setCurrentTime(Util.getCurrentTime());
                pb.setStartTime(System.currentTimeMillis());
                pb.setExpireTerm(si.getValue(time, slv));
                tsm.sendStat(PartyBooster, pb.getOption());
                break;
            case ROLL_OF_THE_DICE: {
                int upbound = 6;
                if (chr.hasSkill(ROLL_OF_THE_DICE_DD) && chr.hasSkill(ROLL_OF_THE_DICE_ADDITION)) {
                    upbound = 7;
                }
                int diceThrow1 = new Random().nextInt(upbound) + 1;

                if (chr.hasSkill(ROLL_OF_THE_DICE_ENHANCE) && Util.succeedProp(40)) {
                    diceThrow1 = new Random().nextInt(4) + 4;
                }

                chr.write(UserPacket.effect(Effect.skillAffectedSelect(skillID, slv, diceThrow1, false)));
                chr.getField().broadcast(UserRemote.effect(chr.getId(), Effect.skillAffectedSelect(skillID, slv, diceThrow1, false)), chr);

                if (diceThrow1 < 2) {
                    chr.reduceSkillCoolTime(skillID, (1000L * si.getValue(cooltime, slv)) / 2);
                    return;
                }

                o1.nOption = diceThrow1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);

                tsm.throwDice(diceThrow1);
                tsm.sendStat(Dice, o1);
                break;
            }
            case ROLL_OF_THE_DICE_DD:
            {
                chr.removeBaseStatByOption();
                boolean isCharged = tsm.getViperEnergyCharge() > 0;
                int upbound = 6;
                if (chr.hasSkill(ROLL_OF_THE_DICE_DD) && chr.hasSkill(5120044)) {
                    upbound = 7;
                }

                int random = new Random().nextInt(upbound) + 1;
                int randomDD = new Random().nextInt(upbound) + 1;
                Option o = tsm.getOption(LoadedDice);
                if (o != null && tsm.hasStat(LoadedDice)) {
                    //Background
                    Effect eff1 = Effect.avatarOriented("Skill/40005.img/skill/400051000/affected1");
                    chr.write(UserPacket.effect(eff1));
                    chr.getField().broadcast(UserRemote.effect(chr.getId(), eff1), chr);
                    //Top Dice
                    Effect eff2 = Effect.avatarOriented("Skill/40005.img/skill/400051000/affected/" + o.nOption);
                    chr.write(UserPacket.effect(eff2));
                    chr.getField().broadcast(UserRemote.effect(chr.getId(), eff2), chr);
                    Effect eff3 = Effect.avatarOriented("Skill/40005.img/skill/400051000/specialAffected0/" + random);
                    chr.write(UserPacket.effect(eff3));
                    chr.getField().broadcast(UserRemote.effect(chr.getId(), eff3), chr);
                    Effect eff4 = Effect.avatarOriented("Skill/40005.img/skill/400051000/specialAffected/" + randomDD);
                    chr.write(UserPacket.effect(eff4));
                    chr.getField().broadcast(UserRemote.effect(chr.getId(), eff4), chr);
                } else {
                    chr.write(UserPacket.effect(Effect.skillAffectedSelect(skillID, slv, random, false)));
                    chr.write(UserPacket.effect(Effect.skillAffectedSelect(skillID, slv, randomDD, true)));
                    chr.getField().broadcast(UserRemote.effect(chr.getId(), Effect.skillAffectedSelect(skillID, slv, random, false)), chr);
                    chr.getField().broadcast(UserRemote.effect(chr.getId(), Effect.skillAffectedSelect(skillID, slv, randomDD, true)), chr);
                }
                if (random < 2 && randomDD < 2) {
                    return;
                }
                chr.addBaseStatByDiceNumber(random, si, slv);
                chr.addBaseStatByDiceNumber(randomDD, si, slv);
                if (o != null) {
                    chr.addBaseStatByDiceNumber(o.nOption, si, slv);
                }
                o1.nOption = (random * 10) + randomDD; // if rolled: 3 and 5, the DoubleDown nOption = 35
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.throwDice(random, randomDD);

                tsm.sendStat(Dice, o1);
                break;
            }
            case CROSSBONES:
                o2.nReason = skillID;
                o2.nValue = si.getValue(indiePadR, slv);
                o2.tTerm = si.getValue(time, slv);
                tsm.sendStat(IndiePADR, o2);
                break;
            //Hyper
            case EPIC_ADVENTURER:
                o1.nValue = si.getValue(indieDamR, slv);
                o1.nReason = skillID;
                o1.tTerm = si.getValue(time, slv);
                tsm.sendStat(IndieDamR, o1);
                break;
            case STIMULATING_CONVERSATION:
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                newStats.put(Stimulate, o1);
                o2.nValue = si.getValue(indieDamR, slv);
                o2.nReason = skillID;
                o2.tTerm = si.getValue(time, slv);
                newStats.put(IndieDamR, o2);
                tsm.sendStat(newStats);
                var effect = Effect.skillAffected(skillID, slv, 0);
                chr.write(UserPacket.effect(effect));
                chr.getField().broadcast(UserRemote.effect(chr.getId(), effect), chr);
                break;
            case LIGHTING_FORM:
                o1.nOption = si.getValue(w, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(TransformOverMan, o1);
                break;
            case LORD_OF_THE_DEEP:
                o1.nOption = si.getValue(v, slv);
                o1.rOption = skillID;
                tsm.sendStat(SerpentScrew, o1);
                break;
            case HEROS_WILL:
                tsm.removeAllDebuffs();
                break;
        }
    }

    // Hit related methods ---------------------------------------------------------------------------------------------
    @Override
    public void handleHit(Client c, InPacket inPacket, HitInfo hitInfo) {
        if (chr.hasSkill(DEFENSIVE_STANCE)) {
            applyPirateRevenge();
        }
        super.handleHit(c, inPacket, hitInfo);
    }

    private void applyPirateRevenge() {
        Skill skill = getPirateRevenge();
        if (skill == null) {
            return;
        }
        SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
        int slv = skill.getCurrentLevel();
        Option o1 = new Option();
        Option o2 = new Option();
        int prop = si.getValue(SkillStat.prop, slv);
        if (Util.succeedProp(prop)) {
            EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
            TemporaryStatManager tsm = chr.getTemporaryStatManager();
            o1.nValue = 1;
            o1.nReason = skill.getSkillId();
            o1.tTerm = si.getValue(x, slv);
            newStats.put(IndieCheckTimeByClient, o1);
            o2.nValue = -si.getValue(y, slv);
            o2.nReason = skill.getSkillId();
            o2.tTerm = si.getValue(x, slv);
            newStats.put(IndieAllHitDamR, o2);
            tsm.sendStat(newStats);
        }
    }

    private Skill getPirateRevenge() {
        Skill skill = null;
        if (chr.hasSkill(DEFENSIVE_STANCE)) {
            skill = chr.getSkill(DEFENSIVE_STANCE);
        }
        return skill;
    }

    @Override
    public void handleShootObject(Char chr, ShootObjectSkillInfo sosi) {
        var skillId = sosi.getSkillId();
        var slv = sosi.getSlv();
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        switch (skillId) {
            case LIGHTING_FORM_ORB:
                Option o = tsm.getOption(TransformOverMan);
                o.nOption = o.nOption - 1;
                if (o.nOption == 0) {
                    o.nOption = -1;
                }
                tsm.updateStat(TransformOverMan, o);
                break;
        }
        super.handleShootObject(chr, sosi);
    }

    @Override
    public void handleSkillRemove(Char chr, int skillId) {
        var tsm = chr.getTemporaryStatManager();
        switch (skillId) {
            case HOWLING_FIST:
                if (!tsm.hasStatBySkillId(HOWLING_FIST_LEVIATHAN_ATTACK)) {
                    var o = new Option();
                    o.nValue = 1;
                    o.nReason = HOWLING_FIST_LEVIATHAN_ATTACK;
                    o.tTerm = 3;
                    tsm.sendStat(IndieNotDamaged, o);
                }
                break;
            case SEA_SERPENT:
            case HEXA_SEA_SERPENT:
                tsm.removeStat(SeaSerpent);
                break;
        }
        super.handleSkillRemove(chr, skillId);
    }

    @Override
    public int handleSetCoolDownSkill(int skillId) {
        switch (skillId) {
            case HEXA_SEA_SERPENT -> {
                int skillID = SEA_SERPENT;
                int slv = chr.getSkillLevel(skillID);
                chr.setSkillCooldown(skillID, slv);
                return 1;
            }
            case HEXA_NAUTILUS_STRIKE -> {
                int skillID = NAUTILUS_STRIKE;
                int slv = chr.getSkillLevel(skillID);
                chr.setSkillCooldown(skillID, slv);
                return 1;
            }
        }
        return super.handleSetCoolDownSkill(skillId);
    }

    @Override
    public void handleRemoveCTS(CharacterTemporaryStat cts, List<Option> options) {
        if (cts == SuperFistEnrageStack) {
            chr.write(UserLocal.userBonusAttackRequest(5141003));
        }
        super.handleRemoveCTS(cts, options);
    }
}
