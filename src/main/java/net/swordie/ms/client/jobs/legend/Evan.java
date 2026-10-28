package net.swordie.ms.client.jobs.legend;

import net.swordie.ms.ServerConfig;
import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.CharacterStat;
import net.swordie.ms.client.character.info.HitInfo;
import net.swordie.ms.client.character.skills.ForceAtom;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.Skill;
import net.swordie.ms.client.character.skills.info.*;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TemporaryStatBase;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.client.jobs.Job;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.FieldPacket;
import net.swordie.ms.connection.packet.UserLocal;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.constants.SkillConstants;
import net.swordie.ms.enums.*;
import net.swordie.ms.life.*;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.life.mob.MobStat;
import net.swordie.ms.life.mob.MobTemporaryStat;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.scripts.ScriptManagerImpl;
import net.swordie.ms.util.Position;
import net.swordie.ms.util.Rect;
import net.swordie.ms.util.Util;
import net.swordie.ms.world.field.Field;

import java.util.EnumMap;
import java.util.List;

import static net.swordie.ms.client.character.skills.SkillStat.*;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;

/**
 * Created on 12/14/2017.
 */
public class Evan extends Job {

    public static final int INHERITED_WILL = 20010194;
    public static final int BACK_TO_NATURE = 20011293;

    public static final int MAGIC_GUARD = 22001012; // Buff

    public static final int MAGIC_BOOSTER = 22111020; // Buff
    public static final int ELEMENTAL_DECREASE = 22141016; // Buff
    public static final int PARTNERS = 22110016;

    public static final int MAGIC_DEBRIS = 22141017;

    public static final int MAPLE_WARRIOR_EVAN = 22171068; // Buff
    public static final int BLESSING_OF_THE_ONYX = 22171073;
    public static final int DRAGON_MASTER = 22171080; // Mount
    public static final int DRAGON_MASTER_ATTACK = 22171083; // Add-on
    public static final int SUMMON_ONYX_DRAGON = 22171081; // Summon
    public static final int HEROIC_MEMORIES_EVAN = 22171082;
    public static final int ENHANCED_MAGIC_DEBRIS = 22170070;
    public static final int HEROS_WILL_EVAN = 22171004;
    public static final int DRAGON_FURY = 22170074;

    //Returns
    public static final int RETURN_FLASH = 22110013; // Return after Wind Skills (Mob Debuff)
    public static final int RETURN_DIVE = 22140013; // Return Dive (Buff)
    public static final int RETURN_FLAME = 22170064; // Return Flame (Flame  AoE)
    public static final int RETURN_FLAME_TILE = 22170093; // Return Flames Tile

    //Evan Attacks
    public static final int MANA_BURST_I = 22001010;
    public static final int MANA_BURST_II = 22110010;
    public static final int MANA_BURST_III = 22140010;
    public static final int MANA_BURST_IV_1 = 22170060;
    public static final int MANA_BURST_IV_2 = 22170061;
    public static final int WIND_CIRCLE = 22111011;
    public static final int THUNDER_CIRCLE = 22141011;
    public static final int EARTH_CIRCLE = 22171062;
    public static final int DARK_FOG = 22171095;

    //Final Attack
    public static final int DRAGON_SPARK = 22000015;
    public static final int ADV_DRAGON_SPARK = 22110021;

    public static final int SPEEDY_DRAGON_FLASH = 22170084;
    public static final int SPEEDY_DRAGON_DIVE = 22170087;
    public static final int SPEEDY_DRAGON_BREATH = 22170090;

    // V Skills
    public static final int ELEMENTAL_BARRAGE = 400021012;
    public static final int DRAGON_SLAM = 400021046;
    public static final int WYRMKINGS_BREATH = 400020046;
    public static final int LUDICROUS_SPEED = 400020051;
    public static final int ELEMENTAL_RADIANCE = 400021073;
    public static final int SPIRAL_OF_MANA = 400021095;

    // HEXA Skills
    public static final int HEXA_DRAGON_SPARK = 22200016;

    private int prevSkill = 0;
    private Dragon dragon;
    private final net.swordie.ms.client.character.tms282.Tms282DragonMaster dragonMasterRuntime = new net.swordie.ms.client.character.tms282.Tms282DragonMaster();
    public net.swordie.ms.client.character.tms282.Tms282DragonMaster getDragonMasterRuntime() { return dragonMasterRuntime; }

    private final int[] addedSkills = new int[]{
            INHERITED_WILL,
            BACK_TO_NATURE,};

    public Evan(Char chr) {
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

    public void spawnDragon() {
        Dragon dragon = getDragon();
        if (dragon == null) {
            return;
        }
        dragon.resetToPlayer(chr.getPosition());
        if (getDragon() != null) {
            chr.getField().spawnLife(getDragon(), null);
        }
    }

    public int getEvanSkill(int skillID) {
        switch (skillID) {
            case MANA_BURST_I:
            case MANA_BURST_II:
            case MANA_BURST_III:
            case MANA_BURST_IV_1:
            case MANA_BURST_IV_2:
            case WIND_CIRCLE:
            case THUNDER_CIRCLE:
            case EARTH_CIRCLE:
            case DARK_FOG:
                return 1;

        }
        return skillID;
    }

    @Override
    public boolean isHandlerOfJob(short id) {
        return JobConstants.isEvan(id);
    }

    // Attack related methods ------------------------------------------------------------------------------------------
    @Override
    public void handleDebuffOnMob(Client c, Mob mob, SkillInfo si, int skillID, int slv, long damage) {
        Char chr = c.getChr();
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
        handleDragonSkillEffect(attackInfo, skillID);
        if (hasHitMobs) {
            // Partners
            if (getEvanSkill(skillID) != 1) {
                givePartnersBuff(skillID);
            }
            if (chr.hasSkill(MAGIC_DEBRIS)) {
                createWreckage(attackInfo, skillID);
            }
            if (skillID == ELEMENTAL_BARRAGE || skillID >= 400021013 && skillID <= 400021015) {
                Option o1 = new Option();
                Option o2 = new Option();
                si = SkillData.getSkillInfoById(ELEMENTAL_BARRAGE);
                int yValue = si.getValue(y, slv);
                int RealValue = skillID == 400021015 ?
                        yValue * 4 :
                        skillID == 400021014 ?
                        yValue * 3 :
                        skillID == 400021013 ?
                        yValue * 2 : yValue;
                if (skillID == ELEMENTAL_BARRAGE) {
                    o1.nReason = 400021015;
                    o1.nValue = si.getValue(cr, slv);
                    o1.tTerm = 10;
                    tsm.sendStat(IndieCrR, o1);
                    o2.nReason = 400021015;
                    o2.nValue = RealValue;
                    o2.tTerm = 10;
                    tsm.sendStat(IndieDamR, o2);
                }
            }
        }
        switch (attackInfo.skillId) {
            case DRAGON_MASTER_ATTACK:
                if (!net.swordie.ms.connection.packet.Tms282RemoteBuffs.active() && !tsm.hasStat(CharacterTemporaryStat.NewFlying)) {
                    tsm.removeStatsBySkill(Evan.DRAGON_MASTER);
                }
                break;
        }
    }

    public void givePartnersBuff(int skillID) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        SkillInfo si = SkillData.getSkillInfoById(PARTNERS);
        Option o1 = new Option();
        Option o2 = new Option();
        if (tsm.getOptByCTSAndSkill(Stance, PARTNERS) == null) {
            EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
            prevSkill = skillID;
            o1.nReason = PARTNERS;
            o1.nValue = si.getValue(indieDamR, 1);
            o1.tTerm = 3;
            newStats.put(IndieDamR, o1);
            o2.nOption = si.getValue(stanceProp, 1);
            o2.rOption = PARTNERS;
            o2.tOption = 3;
            newStats.put(Stance, o2);
            tsm.sendStat(newStats);
        }
    }

    private void createMagicDebrisForceAtom() {
        Field field = chr.getField();
        SkillInfo si = SkillData.getSkillInfoById(getDebrisSkill());
        Rect rect = chr.getPosition().getRectAround(si.getFirstRect());
        if (!chr.isLeft()) {
            rect = rect.horizontalFlipAround(chr.getPosition().getX());
        }
        List<Mob> lifes = field.getMobsInRect(rect);
        if (lifes.size() <= 0) {
            return;
        }
        for (Wreckage wreckage : field.getWreckageByChrId(chr.getId())) {
            if (wreckage.getField() != chr.getField()) {
                continue;
            }
            Life life = Util.getRandomFromCollection(lifes);
            int mobID = (life).getObjectId();
            ForceAtomEnum fae = ForceAtomEnum.WRECKAGE;
            if (chr.hasSkill(ENHANCED_MAGIC_DEBRIS)) {
                fae = ForceAtomEnum.ADV_WRECKAGE;
            }
            ForceAtomInfo forceAtomInfo = new ForceAtomInfo(chr.getNewForceAtomKey(), fae.getInc(), 15, 10,
                    0, 200, Util.getCurrentTime(), 1, 0,
                    wreckage.getPosition());
            chr.createForceAtom(new ForceAtom(false, 0, chr.getId(), fae,
                    true, mobID, getDebrisSkill(), forceAtomInfo, new Rect(), 0, 300,
                    life.getPosition(), getDebrisSkill(), life.getPosition(), 0));
        }
        chr.write(FieldPacket.delWreckage(chr, field.getWreckageByChrId(chr.getId())));
    }

    private void createWreckage(AttackInfo attackInfo, int skillID) {
        if (SkillConstants.isEvanFusionSkill(skillID) && (skillID != MAGIC_DEBRIS && skillID != ENHANCED_MAGIC_DEBRIS)) {
            if (chr.getField().getWreckage().size() < getMaxDebris()) {
                for (MobAttackInfo mai : attackInfo.mobAttackInfo) {
                    Mob mob = (Mob) chr.getField().getLifeByObjectID(mai.mobId);
                    if (mob == null || mob.getHp() <= 0) {
                        continue;
                    }
                    final Position pos = mob.getPosition();
                    Wreckage wreckage = Wreckage.getWreckageBy(chr, getDebrisSkill(), pos, 30000, 1);
                    chr.getField().spawnWreckage(chr, wreckage);
                }
            }
        }
    }

    private int getMaxDebris() {
        Skill skill = null;
        if (chr.hasSkill(MAGIC_DEBRIS)) {
            skill = chr.getSkill(MAGIC_DEBRIS);
        }
        if (chr.hasSkill(ENHANCED_MAGIC_DEBRIS)) {
            skill = chr.getSkill(ENHANCED_MAGIC_DEBRIS);
        }
        if (skill == null) {
            return 0;
        }
        SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
        int slv = skill.getCurrentLevel();
        return si.getValue(x, slv);
    }

    private int getDebrisSkill() {
        int skill = 0;
        if (chr.hasSkill(MAGIC_DEBRIS)) {
            skill = MAGIC_DEBRIS;
        }
        if (chr.hasSkill(ENHANCED_MAGIC_DEBRIS)) {
            skill = ENHANCED_MAGIC_DEBRIS;
        }
        return skill;
    }

    private void handleDragonSkillEffect(AttackInfo attackInfo, int skillID) {
        // Hacky Way:
        switch (skillID) {
            case 22110013:
                attackInfo.attackActionType = 2;
                break;
            case 22110023:
                attackInfo.attackActionType = 3;
                break;
            case 22110025:
                attackInfo.attackActionType = 4;
                break;
            case 22140023:
                attackInfo.attackActionType = 5;
                break;
            case 22140022:
                attackInfo.attackActionType = 6;
                break;
            case 22170067:
                attackInfo.attackActionType = 8;
                break;
            case 22171063:
                attackInfo.attackActionType = 9;
                break;
            case 22170066:
                attackInfo.attackActionType = 11;
                break;
        }
    }

    @Override
    public int getFinalAttackSkill(int faSkill) {
        if (faSkill == DRAGON_SPARK) {
            if (chr.hasSkill(HEXA_DRAGON_SPARK)) return HEXA_DRAGON_SPARK;
            if (chr.hasSkill(ADV_DRAGON_SPARK)) return ADV_DRAGON_SPARK;
        }
        return super.getFinalAttackSkill(faSkill);
    }

    public Dragon getDragon() {
        if (dragon == null && chr.getJob() != JobConstants.JobEnum.EVAN_NOOB.getJobId()) {
            dragon = new Dragon(chr);
        }
        return dragon;
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
        switch (skillID) {
            case MAGIC_GUARD:
                o1.nOption = si.getValue(x, slv);
                o1.rOption = skillID;
                tsm.sendStat(MagicGuard, o1);
                break;
            case RETURN_DIVE:
                o1.nReason = skillID;
                o1.nValue = si.getValue(x, slv);
                o1.tTerm = si.getValue(time, slv);
                tsm.sendStat(IndieBooster, o1);
                break;
            case ELEMENTAL_DECREASE:
                o1.nOption = si.getValue(x, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(ElementalReset, o1);
                break;
            case BLESSING_OF_THE_ONYX:
                o1.nOption = si.getValue(emad, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                newStats.put(EMAD, o1);
                o2.nOption = si.getValue(epdd, slv);
                o2.rOption = skillID;
                o2.tOption = si.getValue(time, slv);
                newStats.put(EPDD, o2);
                tsm.sendStat(newStats);
                break;
            case HEROIC_MEMORIES_EVAN:
                o1.nReason = skillID;
                o1.nValue = si.getValue(indieDamR, slv);
                o1.tTerm = si.getValue(time, slv);
                tsm.sendStat(IndieDamR, o1);
                break;
            case SUMMON_ONYX_DRAGON:
                Summon summon = Summon.getSummonByAndSetStat(c.getChr(), skillID, slv);
                summon.setFlyMob(true);
                summon.setMoveAbility(MoveAbility.Stop);
                chr.getField().spawnSummon(summon);
                break;
            case DRAGON_MASTER:
                if (net.swordie.ms.client.character.tms282.Tms282DragonMaster.begin(chr, slv, System.currentTimeMillis())) break;
                TemporaryStatBase tsb = tsm.getTSBByTSIndex(TSIndex.RideVehicleExpire);
                tsb.setNOption(1939007);
                tsb.setROption(skillID);
                tsb.setExpireTerm(10);
                tsm.sendStat(RideVehicleExpire, tsb.getOption());
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = 1;
                tsm.sendStat(NotDamaged, o1);
                o2.nOption = 1;
                o2.rOption = skillID;
                o2.tOption = 10;
                tsm.sendStat(NewFlying, o2);
                break;
            case BACK_TO_NATURE:
                Field toField = chr.getOrCreateFieldByCurrentInstanceType(si.getValue(x, slv));
                chr.warp(toField);
                break;
            case RETURN_FLAME:
                SkillInfo rft = SkillData.getSkillInfoById(RETURN_FLAME_TILE);
                AffectedArea aa = AffectedArea.getPassiveAA(chr, RETURN_FLAME_TILE, slv);
                aa.setMobOrigin((byte) 0);
                aa.setPosition(chr.getPosition());
                aa.setRect(aa.getPosition().getRectAround(rft.getRects().get(0)));
                chr.getField().spawnAffectedArea(aa);
                break;
            case RETURN_FLASH:
                SkillInfo rflash = SkillData.getSkillInfoById(RETURN_FLASH);
                Rect rect = new Rect( //Skill itself doesn't give a Rect
                        new Position(
                                chr.getPosition().deepCopy().getX() - 300,
                                chr.getPosition().deepCopy().getY() - 300),
                        new Position(
                                chr.getPosition().deepCopy().getX() + 300,
                                chr.getPosition().deepCopy().getY() + 300)
                );
                o1.nOption = rflash.getValue(x, slv);
                o1.rOption = skillID;
                o1.tOption = rflash.getValue(time, slv);
                for (Life life : chr.getField().getLifesInRect(rect)) {
                    if (life instanceof Mob mob) {
                        if (mob == null || mob.getHp() <= 0) {
                            continue;
                        }
                        if (mob.getHp() > 0) {
                            MobTemporaryStat mts = mob.getTemporaryStat();
                            mts.addStatOptions(mob, MobStat.TotalDamParty, o1.deepCopy());
                        }
                    }
                }
                break;
            case MAGIC_DEBRIS:
            case ENHANCED_MAGIC_DEBRIS:
                createMagicDebrisForceAtom();
                break;
            case HEROS_WILL_EVAN:
                tsm.removeAllDebuffs();
                break;
        }
    }

    // Hit related methods ---------------------------------------------------------------------------------------------
    @Override
    public void handleHit(Client c, InPacket inPacket, HitInfo hitInfo) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (tsm.hasStat(MagicGuard)) {
            Skill skill = chr.getSkill(MAGIC_GUARD);
            SkillInfo si = SkillData.getSkillInfoById(MAGIC_GUARD);
            int dmgPerc = si.getValue(x, skill.getCurrentLevel());
            int dmg = hitInfo.hpDamage;
            int mpDmg = (int) (dmg * (dmgPerc / 100D));
            mpDmg = chr.getStat(Stat.mp) - mpDmg < 0 ? chr.getStat(Stat.mp) : mpDmg;
            hitInfo.hpDamage = dmg - mpDmg;
            hitInfo.mpDamage = mpDmg;
        }
        super.handleHit(c, inPacket, hitInfo);
    }

    @Override
    public void setCharCreationStats(Char chr) {
        super.setCharCreationStats(chr);
        CharacterStat cs = chr.getAvatarData().getCharacterStat();
        cs.setPosMap(JobConstants.EVAN_CREATION_MAP);
    }

    @Override
    public void handleLevelUp(short level) {
        super.handleLevelUp(level);
        switch (level) {
            case 10:
                chr.setJob(2210);
                chr.setStatAndSendPacket(Stat.job, 2210);
                chr.addSpToSpecificJob((short) 2210, 3);
                spawnDragon();
                break;
            case 20: {
                String message = "主人，你真的很會戰鬥耶！";
                chr.write(UserLocal.addPopupSay(1013208, 6000, message, "FarmSE.img/boxResult"));
                break;
            }
            case 30:
                chr.setJob(2212);
                chr.setStatAndSendPacket(Stat.job, 2212);
                chr.addSpToSpecificJob((short) 2210, 2);
                chr.addSpToSpecificJob((short) 2212, 3);
                spawnDragon();
                break;
            case 50: {
                String message = "如果我專心攻擊一個區域，你攻擊另一個區域，" +
                        "就能打倒更多敵人。不過遇到更強的敵人時，我們一定要一起集中攻擊！";
                chr.write(UserLocal.addPopupSay(1013208, 6000, message, "FarmSE.img/boxResult"));
                break;
            }
            case 60:
                chr.setJob(2214);
                chr.setStatAndSendPacket(Stat.job, 2214);
                chr.addSpToSpecificJob((short) 2212, 1);
                chr.addSpToSpecificJob((short) 2214, 3);
                spawnDragon();
                break;
            case 70: {
                String message = "主人！閃雷攻擊可以同時打中五個不同的地方！";
                chr.write(UserLocal.addPopupSay(1013208, 3000, message, "FarmSE.img/boxResult"));
                String message2 = "對付體型大的敵人時，它的傷害比我其他任何技能都高喔。";
                chr.write(UserLocal.addPopupSay(1013208, 3000, message2, "FarmSE.img/boxResult"));
                break;
            }
            case 90: {
                String message = "寶貝龍，你知道嗎？聽說用龍皮做成的武器和防具，" +
                        "最近可以賣到很高的價錢呢……";
                chr.write(UserLocal.addPopupSay(0, 3000, message, "FarmSE.img/boxResult"));
                String message2 = "等、等一下，主、主人，你這是什麼意思……";
                chr.write(UserLocal.addPopupSay(1013208, 3000, message2, "FarmSE.img/boxResult"));
                String message3 = "哈！被我騙到了吧。";
                chr.write(UserLocal.addPopupSay(0, 3000, message3, "FarmSE.img/boxResult"));
                String message4 = "喂！不要這樣嚇我啦！";
                chr.write(UserLocal.addPopupSay(1013208, 3000, message4, "FarmSE.img/boxResult"));
                String message5 = "嘿嘿。不管你發生什麼事，我都絕對不會丟下你的！";
                chr.write(UserLocal.addPopupSay(0, 3000, message5, "FarmSE.img/boxResult"));
                String message6 = ".....";
                chr.write(UserLocal.addPopupSay(1013208, 3000, message6, "FarmSE.img/boxResult"));
                break;
            }
            case 100:
                chr.setJob(2217);
                chr.setStatAndSendPacket(Stat.job, 2218);
                chr.addSpToSpecificJob((short) 2214, 2);
                chr.addSpToSpecificJob((short) 2218, 3);
                spawnDragon();
                break;
            case 200: {
                String message = "200 級……主人，太厲害了！我真的覺得自己變強了！";
                chr.write(UserLocal.addPopupSay(1013208, 6000, message, "FarmSE.img/boxResult"));
                break;
            }
        }
    }

    @Override
    public void handleJobAdvance() {
        ScriptManagerImpl sm = chr.getScriptManager();
        if (chr.getJob() == JobConstants.JobEnum.EVAN1.getJobId()) {
            if (chr.getLevel() < 30) {
                sm.sendSayOkay("#e這個職業需要達到#r30#k等以上才能轉職");
                return;
            }
            if (sm.sendAskYesNo("#e要跳過轉職任務嗎？")) {
                sm.jobAdvance(JobConstants.JobEnum.EVAN2.getJobId());
            }
        } else if (chr.getJob() == JobConstants.JobEnum.EVAN2.getJobId()) {
            if (chr.getLevel() < 60) {
                sm.sendSayOkay("#e這個職業需要達到#r60#k等以上才能轉職");
                return;
            }
            if (sm.sendAskYesNo("#e要跳過轉職任務嗎？")) {
                if (sm.getEmptyInventorySlots(1) < 1) {
                    sm.sendSayOkay("#e請在裝備欄空出更多空間。");
                    return;
                }
                sm.jobAdvance(JobConstants.JobEnum.EVAN3.getJobId());
                sm.giveItem(1142156);
            }
        } else if (chr.getJob() == JobConstants.JobEnum.EVAN3.getJobId()) {
            if (chr.getLevel() < 100) {
                sm.sendSayOkay("#e這個職業需要達到#r100#k等以上才能轉職");
                return;
            }
            if (sm.sendAskYesNo("#e要跳過轉職任務嗎？")) {
                if (sm.getEmptyInventorySlots(1) < 1) {
                    sm.sendSayOkay("#e請在裝備欄空出更多空間。");
                    return;
                }
                sm.jobAdvance(JobConstants.JobEnum.EVAN4.getJobId());
                sm.giveItem(1142157);
            }
        } else {
            sm.sendSayOkay("#e目前的狀態無法轉職。");
        }
    }

    @Override
    public void handleRemoveCTS(CharacterTemporaryStat cts, List<Option> options) {
        if (cts == RideVehicleExpire) {
            spawnDragon();
        }
        super.handleRemoveCTS(cts, options);
    }

    @Override
    public int handleSetCoolDownSkill(int skillId) {
        switch (skillId) {
            case 22140022 -> { // Dragon Dive
                chr.setSkillCooldown(22140022, chr.getSkillLevel(22141012));
                return 1;
            }
            case 22110023 -> { // Dragon Flash
                chr.setSkillCooldown(22110023, chr.getSkillLevel(22111012));
                return 1;
            }
            case 22110022 -> { // Dragon Flash
                chr.setSkillCooldown(22110022, chr.getSkillLevel(22111012));
                return 1;
            }
        }
        return super.handleSetCoolDownSkill(skillId);
    }
}
