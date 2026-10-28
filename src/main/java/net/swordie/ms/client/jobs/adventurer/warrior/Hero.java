package net.swordie.ms.client.jobs.adventurer.warrior;

import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.ExtendSP;
import net.swordie.ms.client.character.info.HitInfo;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.Skill;
import net.swordie.ms.client.character.skills.info.AttackInfo;
import net.swordie.ms.client.character.skills.info.MobAttackInfo;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.client.character.skills.info.SkillUseInfo;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.*;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.enums.AssistType;
import net.swordie.ms.enums.MoveAbility;
import net.swordie.ms.life.AffectedArea;
import net.swordie.ms.life.Life;
import net.swordie.ms.life.Summon;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.life.mob.MobStat;
import net.swordie.ms.life.mob.MobTemporaryStat;
import net.swordie.ms.life.mob.skill.BurnedInfo;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.scripts.ScriptManagerImpl;
import net.swordie.ms.util.Position;
import net.swordie.ms.util.Rect;
import net.swordie.ms.util.Util;
import net.swordie.ms.world.field.Field;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;

import static net.swordie.ms.client.character.skills.SkillStat.*;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;

public class Hero extends Warrior {

    public static final int WEAPON_BOOSTER = 1101004;
    public static final int COMBO_ATTACK = 1101013;
    public static final int SPIRIT_BLADE = 1101006;

    public static final int FINAL_ATTACK = 1100002;
    public static final int FLASH_BLADE = 1101014;

    public static final int COMBO_SYNERGY = 1110013;
    public static final int SELF_RECOVERY = 1110000;
    public static final int BEAM_BLADE = 1111016;

    public static final int COMBO_FURY = 1101012;
    public static final int COMBO_FURY_DOWN = 1100012;
    public static final int SCARRING_SWORD = 1111003;
    public static final int ADVANCED_FINAL_ATTACK = 1120013;
    public static final int ADVANCED_COMBO_REINFORCE = 1120043;
    public static final int MAPLE_WARRIOR = 1121000;
    public static final int PUNCTURE = 1121015;
    public static final int MAGIC_CRASH = 1121016;
    public static final int HEROS_WILL = 1121011;
    public static final int ADVANCED_COMBO = 1120003;

    //Hyper Skills
    public static final int RISING_RAGE = 1121052;
    public static final int EPIC_ADVENTURE = 1121053;
    public static final int CRY_VALHALLA = 1121054; //Lv150
    public static final int CRY_VALHALLA_S = 1121055; //Lv150

    // V Skills
    public static final int BURNING_SOUL_BLADE = 400011001;
    public static final int BURNING_SOUL_BLADE_STATIONARY = 400011002;
    public static final int WORLDREAVER = 400011027;
    public static final int COMBO_INSTINCT = 400011073;
    public static final int COMBO_INSTINCT_SLASH_STRAIGHT = 400011076;
    public static final int COMBO_INSTINCT_SLASH_DOWN = 400011075;
    public static final int COMBO_INSTINCT_SLASH_UP = 400011074;
    public static final int SWORD_ILLUSION = 400011124;
    public static final int SWORD_ILLUSION_2 = 400011125;
    public static final int SWORD_ILLUSION_3 = 400011126;

    // HEXA Skills
    public static final int HEXA_RISING_RAGE = 1141002;
    public static final int HEXA_BEAM_BLADE = 1141003;
    public static final int RENDING_EDGE = 1140005;
    public static final int RENDING_EDGE_ = 1140010;
    public static final int HEXA_CRY_VALHALLA = 1141006;
    public static final int HEXA_CRY_VALHALLA_S = 1141007; //Lv150
    public static final int HEXA_PUNCTURE = 1141008;
    public static final int HEXA_FINAL_ATTACK = 1140009;

    private static final List<Integer> comboInstinctAttack = Arrays.asList(
            COMBO_INSTINCT_SLASH_DOWN,
            COMBO_INSTINCT_SLASH_UP,
            COMBO_INSTINCT_SLASH_STRAIGHT
    );

    private long lastSelfRecovery = 0L;
    private long flashBlade = 0L;
    private int cryValhallaCount = 0;
    private int rendingEdgeCount = 0;

    public Hero(Char chr) {
        super(chr);
    }

    public void update(long now) {
        super.update(now);
        if (chr.hasSkill(FLASH_BLADE) && (this.flashBlade == 0 || now - this.flashBlade >= 4000L)) {
            increaseFlashBlade(now);
        }
    }

    @Override
    public boolean isHandlerOfJob(short id) {
        return JobConstants.isHero(id);
    }

    private void spawnCryValhallas() {
        if (chr.hasSkill(CRY_VALHALLA)) {
            TemporaryStatManager tsm = chr.getTemporaryStatManager();
            int skillID = chr.hasSkill(HEXA_CRY_VALHALLA) ? HEXA_CRY_VALHALLA : CRY_VALHALLA;
            if (tsm.hasStatBySkillId(skillID)) {
                int slv = chr.getSkillLevel(skillID);
                int summonID = skillID == HEXA_CRY_VALHALLA ? HEXA_CRY_VALHALLA_S : CRY_VALHALLA_S;
                SkillInfo si = SkillData.getSkillInfoById(summonID);
                if (si == null) {
                    return;
                }
                int max = si.getValue(w, slv);
                int maxCount = si.getValue(u2, slv);
                if (cryValhallaCount >= maxCount) {
                    return;
                }
                for (int i = 0; i < max; i++) {
                    Summon summon = Summon.getSummonBy(chr, summonID, slv);
                    summon.setMoveAbility(MoveAbility.Stop);
                    chr.getField().spawnAddSummon(summon);
                }
                cryValhallaCount++;
            }
        }
    }

    public void increaseFlashBlade(long now) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o = new Option();
        var val = tsm.hasStat(FlashSlashStack) ? (int) (tsm.getTotalNOptionOfStat(FlashSlashStack) + 1) : 1;
        o.nOption = Math.min(2, val);
        o.rOption = FLASH_BLADE;
        tsm.sendStat(FlashSlashStack, o);
        flashBlade = now;
    }

    private void decreaseFlashBlade() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o = new Option();
        var val = tsm.hasStat(FlashSlashStack) ? (int) (tsm.getTotalNOptionOfStat(FlashSlashStack) - 1) : 0;
        o.nOption = Math.max(0, val);
        o.rOption = FLASH_BLADE;
        tsm.sendStat(FlashSlashStack, o);
    }

    public void increaseBeamBlade() {
        int skillID = HEXA_BEAM_BLADE;
        if (chr.hasSkill(skillID)) {
            TemporaryStatManager tsm = chr.getTemporaryStatManager();
            Option o = new Option();
            var val = tsm.hasStat(HeroAuraBladeStack) ? (int) (tsm.getTotalNOptionOfStat(HeroAuraBladeStack) + 1) : 1;
            o.nOption = Math.min(2, val);
            o.rOption = skillID;
            tsm.sendStat(HeroAuraBladeStack, o);
            chr.write(WvsContext.updateSkillStackRequestResult(skillID, (byte) o.nOption));
        }
    }

    private void decreaseBeamBlade() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o = new Option();
        var val = tsm.hasStat(HeroAuraBladeStack) ? (int) (tsm.getTotalNOptionOfStat(HeroAuraBladeStack) - 1) : 0;
        o.nOption = Math.max(0, val);
        o.rOption = HEXA_BEAM_BLADE;
        tsm.sendStat(HeroAuraBladeStack, o);
    }

    private void addCombo(int inc) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        int currentCount = getComboCount();
        int maxCombo = getMaxCombo();
        if (currentCount > 0 && currentCount < maxCombo) {
            int comboInc = inc;
            if (chr.hasSkill(ADVANCED_COMBO) && !tsm.hasStatBySkillId(COMBO_INSTINCT)) {
                int slv = chr.getSkillLevel(ADVANCED_COMBO);
                SkillInfo si = SkillData.getSkillInfoById(ADVANCED_COMBO);
                if (slv > 0 && Util.succeedProp(si.getValue(prop, slv))) {
                    comboInc = 2;
                }
            }
            Option o = new Option();
            o.nOption = Math.min(maxCombo, currentCount + comboInc);
            o.rOption = COMBO_ATTACK;
            tsm.sendStat(ComboCounter, o);
        }
    }

    private void removeCombo(int count) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        int currentCount = getComboCount();
        Option o = new Option();
        if (currentCount > count + 1) {
            o.nOption = currentCount - count;
        } else {
            o.nOption = 1;
        }
        o.rOption = COMBO_ATTACK;
        tsm.sendStat(ComboCounter, o);
    }

    private int getComboProp() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Skill skill = null;
        if (chr.hasSkill(COMBO_SYNERGY)) {
            skill = chr.getSkill(COMBO_SYNERGY);
        } else if (chr.hasSkill(COMBO_ATTACK)) {
            skill = chr.getSkill(COMBO_ATTACK);
        }
        if (skill == null) {
            return 0;
        }
        int proc = SkillData.getSkillInfoById(skill.getSkillId()).getValue(prop, skill.getCurrentLevel());
        if (tsm.hasStatBySkillId(COMBO_INSTINCT)) {
            proc -= 50;
        }
        return Math.max(proc, 0);
    }

    public int getComboCount() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (tsm.hasStat(ComboCounter)) {
            return tsm.getOption(ComboCounter).nOption;
        }
        return -1;
    }

    private int getMaxCombo() {
        if (chr.hasSkill(ADVANCED_COMBO)) {
            return 11;
        } else {
            return 6;
        }
    }

    public Skill getComboAttackSkill() {
        Skill skill = null;
        if (chr.hasSkill(ADVANCED_COMBO)) {
            skill = chr.getSkill(ADVANCED_COMBO);
        } else if (chr.hasSkill(COMBO_SYNERGY)) {
            skill = chr.getSkill(COMBO_SYNERGY);
        } else if (chr.hasSkill(COMBO_ATTACK)) {
            skill = chr.getSkill(COMBO_ATTACK);
        }

        return skill;
    }

    // Attack related methods ------------------------------------------------------------------------------------------
    @Override
    public void handleDebuffOnMob(Client c, Mob mob, SkillInfo si, int skillID, int slv, long damage) {
        Char chr = c.getChr();
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o1 = new Option();
        Option o2 = new Option();
        BurnedInfo bi;
        MobTemporaryStat mts = mob.getTemporaryStat();
        EnumMap<MobStat, Option> map = new EnumMap<>(MobStat.class);
        if (tsm.hasStat(CrusaderPanic)) {
            if (!mts.hasCurrentMobStatBySkillId(SCARRING_SWORD)) {
                int slvv = chr.getSkillLevel(SCARRING_SWORD);
                SkillInfo ssi = SkillData.getSkillInfoById(SCARRING_SWORD);
                o1.nOption = -ssi.getValue(w, slvv);
                o1.rOption = SCARRING_SWORD;
                o1.tOption = 20;
                map.put(MobStat.PAD, o1);
                o2.nOption = -ssi.getValue(v, slvv);
                o2.rOption = SCARRING_SWORD;
                o2.tOption = 20;
                map.put(MobStat.ACC, o2);
                mts.addStatOptions(mob, map);
            }
        }
        switch (skillID) {
            case COMBO_FURY:
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    if (Util.succeedProp(si.getValue(prop, slv)) && !mob.isBoss()) {
                        o1.nOption = 1;
                        o1.rOption = skillID;
                        o1.tOption = si.getValue(time, slv);
                        mts.addStatOptions(mob, MobStat.Stun, o1);
                    }
                    addCombo(1);
                }
                break;
            case PUNCTURE:
            case HEXA_PUNCTURE:
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    bi = BurnedInfo.createBurnInfo(chr, skillID, slv, damage);
                    o1.nOption = si.getValue(y, slv);
                    o1.rOption = skillID;
                    o1.tOption = si.getValue(time, slv);
                    mts.addStatOptions(mob, MobStat.TotalDamParty, o1);
                    if (Util.succeedProp(si.getValue(prop, slv))) {
                        mts.createAndAddBurnedInfo(mob, bi, skillID);
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
        int slv = attackInfo.slv;
        if (chr.hasSkill(SELF_RECOVERY)) {
            SkillInfo siSR = SkillData.getSkillInfoById(SELF_RECOVERY);
            int slvSR = chr.getSkillLevel(SELF_RECOVERY);
            if (now - lastSelfRecovery >= siSR.getValue(hcHp, slvSR)) {
                if (chr.getHP() < chr.getMaxHP()) {
                    chr.heal(siSR.getValue(hp, slvSR));
                }
                if (chr.getMP() < chr.getMaxMP()) {
                    chr.healMP(siSR.getValue(mp, slvSR));
                }
                lastSelfRecovery = now;
            }
        }
        if (hasHitMobs) {
            //Combo
             if (!isComboIgnoreSkill(skillID)){
                int comboProp = getComboProp();
                if (Util.succeedProp(comboProp)) {
                    addCombo(1);
                }
            }
            // Instinctual Combo
            if (tsm.hasStatBySkillId(COMBO_INSTINCT) && (skillID == 1121008 || skillID == 1120017)) { // Raging Blow
                for (int comboInstinctSkillId : comboInstinctAttack) {
                    chr.write(UserLocal.userBonusAttackRequest(comboInstinctSkillId));
                }
            }
            if (skillID != RENDING_EDGE) {
                doRendingEdge(attackInfo);
            }
            if (skillID != CRY_VALHALLA_S && skillID != HEXA_CRY_VALHALLA_S) {
                spawnCryValhallas();
            }
        }
        Option o1 = new Option();
        Option o2 = new Option();
        switch (skillID) {
            case COMBO_FURY:
            case PUNCTURE:
            case HEXA_PUNCTURE:
                removeCombo(1);
                break;
            case FLASH_BLADE:
                decreaseFlashBlade();
                break;
            case COMBO_FURY_DOWN:
                if (hasHitMobs) {
                    int mobId = attackInfo.mobAttackInfo.get(0).mobId;
                    Life mob = chr.getField().getLifeByObjectID(mobId);
                    if (mob instanceof Mob) {
                        chr.getField().broadcast(UserRemote.effect(chr.getId(),
                                Effect.showHookEffect(skillID, chr.getLevel(), 1, 0, attackInfo.left, mobId, mob.getX(), mob.getY())));
                    }
                }
                break;
            case WORLDREAVER:
                for (MobAttackInfo mai : attackInfo.mobAttackInfo) {
                    Mob mob = (Mob) chr.getField().getLifeByObjectID(mai.mobId);
                    long totalDmg = Arrays.stream(mai.damages).sum();
                    if (mob == null || mob.getHp() <= 0 || totalDmg < mob.getHp()) {
                        continue;
                    }
                    chr.getField().broadcast(MobPool.specialSelectedEffectBySkill(mob, skillID, chr.getId()));

                }
                removeCombo(6);
                if (!tsm.hasStatBySkillId(skillID)) {
                    o1.nValue = 6 * 10; // 6 combo orbs * 10% FD
                    o1.nReason = skillID;
                    o1.tTerm = 5;
                    tsm.sendStat(IndieDamR, o1);
                    o2.nValue = 1;
                    o2.nReason = skillID;
                    o2.tTerm = 5;
                    tsm.sendStat(IndieNotDamaged, o2);
                }
                break;
            case HEXA_BEAM_BLADE:
                if (!tsm.hasStat(HeroAuraBladeAdv)) {
                    o1.nOption = 1;
                    o1.rOption = skillID;
                    o1.tOption = 2;
                    tsm.sendStat(HeroAuraBladeAdv, o1);
                }
                decreaseBeamBlade();
                break;
        }
    }

    private void doRendingEdge(AttackInfo attackInfo) {
        final int skillID = RENDING_EDGE;
        if (!chr.hasSkill(skillID) || attackInfo.mobCount <= 0) return;

        SkillInfo si = SkillData.getSkillInfoById(skillID);
        if (si == null) return;

        int slv = chr.getSkillLevel(skillID);
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Field field = chr.getField();

        int max = tsm.hasStat(HeroComboInstinct) ? 10 : 5;

        var existing = field.getAffectedAreasBySkillID(skillID, chr.getId());
        int curCount = existing.size();
        if (curCount >= max) return;

        int prev = this.rendingEdgeCount;
        int next = prev + attackInfo.mobCount;

        if (next >= 32) {
            this.rendingEdgeCount = 0;
            return;
        }
        this.rendingEdgeCount = next;

        // mốc hợp lệ: 5..30 => tier 1..6
        int prevTier = Math.min(prev / 5, 6);
        int nextTier = Math.min(next / 5, 6);

        int crossed = nextTier - prevTier;
        if (crossed <= 0) return;

        // tìm position: ưu tiên boss, nếu không có boss lấy mob HP cao nhất
        Position bossPosition = null;
        long bestHp = -1;
        boolean foundBoss = false;

        var mobList = attackInfo.mobAttackInfo;
        if (mobList == null || mobList.isEmpty()) return;

        for (MobAttackInfo mai : mobList) {
            Life life = field.getLifeByObjectID(mai.mobId);
            if (!(life instanceof Mob mob)) continue;

            long hp = mob.getHp();
            if (hp <= 0) continue;

            boolean isBoss = mob.isBoss();
            if (isBoss) {
                if (!foundBoss || hp > bestHp) {
                    foundBoss = true;
                    bestHp = hp;
                    bossPosition = mob.getPosition();
                }
            } else if (!foundBoss && hp > bestHp) {
                bestHp = hp;
                bossPosition = mob.getPosition();
            }
        }
        if (bossPosition == null) return;

        // spawn liền nhiều cái = số mốc vượt, nhưng không vượt max
        int canSpawn = max - curCount;
        int spawnCount = Math.min(crossed, canSpawn);

        var rect = si.getRects().getFirst();
        for (int i = 0; i < spawnCount; i++) {
            Position pos = new Position(
                    bossPosition.getX(),
                    bossPosition.getY() - Util.getRandom(10, 100)
            );

            AffectedArea aa = AffectedArea.getAffectedArea(chr, skillID, slv);
            aa.setPosition(pos);
            aa.setRect(pos.getRectAround(rect));
            aa.setDuration(1500);
            field.spawnAffectedArea(aa);
        }
    }

    @Override
    public int getFinalAttackSkill(int faSkill) {
        if (faSkill == FINAL_ATTACK) {
            if (chr.hasSkill(HEXA_FINAL_ATTACK)) return HEXA_FINAL_ATTACK;
            else if (chr.hasSkill(ADVANCED_FINAL_ATTACK)) return ADVANCED_FINAL_ATTACK;
        }
        return super.getFinalAttackSkill(faSkill);
    }

    public boolean isComboIgnoreSkill(int skillID) {
        return skillID == SCARRING_SWORD
                || skillID == COMBO_FURY
                || skillID == COMBO_FURY_DOWN
                || skillID == PUNCTURE
                || skillID == COMBO_INSTINCT_SLASH_DOWN
                || skillID == COMBO_INSTINCT_SLASH_UP
                || skillID == COMBO_INSTINCT_SLASH_STRAIGHT;
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
        Option o3 = new Option();
        Option o4 = new Option();
        Field field = chr.getField();
        switch (skillID) {
            case SPIRIT_BLADE:
                o1.nReason = skillID;
                //o1.nValue = si.getValue(indiePad, slv);
                o1.nValue = 30 + slv; // Skill-log
                o1.tTerm = si.getValue(time, slv);
                newStats.put(IndiePAD, o1);
                o2.nOption = si.getValue(y, slv);
                o2.rOption = skillID;
                o2.tOption = si.getValue(time, slv);
                newStats.put(PowerGuard, o2);
                tsm.sendStat(newStats);
                break;
            case COMBO_ATTACK:
                o1.nOption = 1;
                o1.rOption = skillID;
                tsm.sendStat(ComboCounter, o1);
                break;
            case SCARRING_SWORD:
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(CrusaderPanic, o1);
                break;
            case EPIC_ADVENTURE:
                o1.nReason = skillID;
                o1.nValue = si.getValue(indieDamR, slv);
                o1.tTerm = si.getValue(time, slv);
                tsm.sendStat(IndieDamR, o1);
                break;
            case CRY_VALHALLA:
            case HEXA_CRY_VALHALLA:
                this.cryValhallaCount = 0;
                o1.nReason = skillID;
                o1.nValue = si.getValue(indieCr, slv);
                o1.tTerm = si.getValue(time, slv);
                newStats.put(IndieCrR, o1);
                o2.nOption = si.getValue(x, slv);
                o2.rOption = skillID;
                o2.tOption = si.getValue(time, slv);
                newStats.put(AsrR, o2);
                newStats.put(TerR, o2.deepCopy());
                o3.nOption = 100;
                o3.rOption = skillID;
                o3.tOption = si.getValue(time, slv);
                newStats.put(Stance, o3);
                o4.nReason = skillID;
                o4.nValue = si.getValue(indiePad, slv);
                o4.tTerm = si.getValue(time, slv);
                newStats.put(IndiePAD, o4);
                tsm.sendStat(newStats);
                addCombo(getMaxCombo());
                break;
            case BURNING_SOUL_BLADE:
            case BURNING_SOUL_BLADE_STATIONARY:
                Summon existingSummon = null;
                int remainingTime = 0;
                long startTime = 0;
                for (Summon summon : field.getSummons()) {
                    if ((summon.getSkillID() == BURNING_SOUL_BLADE || summon.getSkillID() == BURNING_SOUL_BLADE_STATIONARY) && summon.getOwnerId() == chr.getId()) {
                        existingSummon = summon;
                        Option existingOption = tsm.getOptByCTSAndSkill(IndieEmpty, summon.getSkillID());
                        if (existingOption != null) {
                            startTime = existingOption.startTime;
                            long timeCount = (Util.getCurrentTimeLong() - existingOption.startTime) / 1000;
                            remainingTime = si.getValue(time, slv) - (int) timeCount;
                        }
                        break;
                    }
                }
                if (existingSummon != null) {
                    tsm.removeStatsBySkill(existingSummon.getSkillID());
                    field.removeLife(existingSummon);
                }
                if (existingSummon == null || remainingTime <= 0) {
                    remainingTime = 0;
                    startTime = 0;
                }
                int newSkillId = (existingSummon != null && existingSummon.getSkillID() == BURNING_SOUL_BLADE)
                        ? BURNING_SOUL_BLADE_STATIONARY
                        : BURNING_SOUL_BLADE;
                Summon newSummon = remainingTime == 0
                        ? Summon.getSummonByAndSetStat(chr, newSkillId, slv)
                        : Summon.getSummonByAndSetStatWithTime(chr, newSkillId, slv, startTime, remainingTime);
                newSummon.setFlyMob(newSkillId == BURNING_SOUL_BLADE);
                newSummon.setMoveAbility(newSkillId == BURNING_SOUL_BLADE ? MoveAbility.Walk : MoveAbility.Stop);
                newSummon.setAssistType(AssistType.Attack);
                newSummon.setAttackActive(true);
                field.spawnSummon(newSummon);
                break;
            case COMBO_INSTINCT:
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(HeroComboInstinct, o1);
                break;
            case MAGIC_CRASH: {
                var rect = chr.getRectAround(new Rect(-500, -250, 500, 250));
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
        }
    }

    // Hit related methods ---------------------------------------------------------------------------------------------
    @Override
    public void handleHit(Client c, InPacket inPacket, HitInfo hitInfo) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (chr.hasSkill(COMBO_SYNERGY)) {
            int comboprop = 30;
            if (Util.succeedProp(comboprop)) {
                addCombo(1);
            }
        }
        super.handleHit(c, inPacket, hitInfo);
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
    public void handleRemoveCTS(CharacterTemporaryStat cts,  List<Option> options) {
        if (chr.hasSkill(COMBO_SYNERGY)) {
            SkillInfo csi = SkillData.getSkillInfoById(COMBO_SYNERGY);
            int slv = csi.getCurrentLevel();
            int comboprop = csi.getValue(subProp, slv);
            if (Util.succeedProp(comboprop)) {
                addCombo(1);
            }
        }
        super.handleRemoveCTS(cts, options);
    }

    @Override
    public int handleSetCoolDownSkill(int skillId) {
        switch (skillId) {
            case HEXA_RISING_RAGE -> {
                int skillID = RISING_RAGE;
                int slv = chr.getSkillLevel(skillID);
                chr.setSkillCooldown(skillID, slv);
                return 1;
            }
            case HEXA_CRY_VALHALLA -> {
                int skillID = CRY_VALHALLA;
                int slv = chr.getSkillLevel(skillID);
                chr.setSkillCooldown(skillID, slv);
                return 1;
            }
        }
        return super.handleSetCoolDownSkill(skillId);
    }
}
