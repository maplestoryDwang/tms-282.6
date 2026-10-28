package net.swordie.ms.life;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.Skill;
import net.swordie.ms.client.character.skills.SkillStat;
import net.swordie.ms.client.character.skills.info.AttackInfo;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.client.jobs.Zero;
import net.swordie.ms.client.jobs.adventurer.archer.BowMaster;
import net.swordie.ms.client.jobs.adventurer.archer.Pathfinder;
import net.swordie.ms.client.jobs.adventurer.magician.Bishop;
import net.swordie.ms.client.jobs.adventurer.magician.FirePoison;
import net.swordie.ms.client.jobs.adventurer.magician.IceLightning;
import net.swordie.ms.client.jobs.adventurer.pirate.Pirate;
import net.swordie.ms.client.jobs.adventurer.thief.NightLord;
import net.swordie.ms.client.jobs.adventurer.thief.Shadower;
import net.swordie.ms.client.jobs.adventurer.thief.Thief;
import net.swordie.ms.client.jobs.cygnus.BlazeWizard;
import net.swordie.ms.client.jobs.legend.Aran;
import net.swordie.ms.client.jobs.legend.Shade;
import net.swordie.ms.client.jobs.resistance.BattleMage;
import net.swordie.ms.client.jobs.resistance.Xenon;
import net.swordie.ms.client.jobs.sengoku.Kanna;
import net.swordie.ms.connection.packet.FieldPacket;
import net.swordie.ms.constants.SkillConstants;
import net.swordie.ms.enums.BaseStat;
import net.swordie.ms.enums.Stat;
import net.swordie.ms.handlers.GlobalTimerManager;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.life.mob.MobStat;
import net.swordie.ms.life.mob.MobTemporaryStat;
import net.swordie.ms.life.mob.boss.demian.sword.DemianFlyingSword;
import net.swordie.ms.life.mob.boss.demian.sword.DemianFlyingSwordPath;
import net.swordie.ms.life.mob.skill.BurnedInfo;
import net.swordie.ms.life.mob.skill.MobSkillStat;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.loaders.containerclasses.MobSkillInfo;
import net.swordie.ms.util.Position;
import net.swordie.ms.util.Rect;
import net.swordie.ms.world.field.Field;

import java.util.*;
import java.util.concurrent.ScheduledFuture;

import static net.swordie.ms.client.character.skills.SkillStat.*;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;

public class AffectedArea extends Life {

    private Char owner;
    private int charLevel;
    private Rect rect;
    private int skillID;
    private int force;
    private int option;
    private int elemAttr;
    private int linkingSkillID;
    private int slv;
    private byte mobOrigin;
    private short delay;
    private boolean flip;
    private int duration;
    private boolean removeSkill;
    private Mob mob;
    private int mobLvl;
    private int mobOwnerID;
    private boolean hitMob;
    private ScheduledFuture<?> aaTimer;
    private long damage;
    private Set<AffectedAreaSpeacial> affectedAreaSpeacials;

    public AffectedArea(int templateId) {
        super(templateId);
    }

    public static AffectedArea getMobAA(Mob mob, int skill, int slv, MobSkillInfo msi) {
        AffectedArea aa = new AffectedArea(0);

        aa.setMobOrigin((byte) 1);
        aa.setMob(mob);
        aa.setMobLvl(mob.getLevel());
        aa.setSkillID(skill);
        aa.setSlv(slv);
        aa.setDuration(msi.getSkillStatIntValue(MobSkillStat.time) * 1000);
        aa.setPosition(mob.getPosition());
        aa.setRect(mob.getPosition().getRectAround(new Rect(msi.getLt(), msi.getRb())));

        return aa;
    }

    public static AffectedArea getAreaForce(Mob mob, short skill, short slv, int duration) {
        AffectedArea aa = new AffectedArea(0);

        aa.setMobOrigin((byte) 1);
        aa.setMob(mob);
        aa.setMobLvl(mob.getLevel());
        aa.setSkillID(skill);
        aa.setSlv(slv);
        aa.setDuration(duration);
        aa.setPosition(mob.getPosition());

        return aa;
    }

    public static AffectedArea getMobAAWithPosition(Mob mob, int skill, int slv, Rect rect, int duration) {
        AffectedArea aa = new AffectedArea(0);

        aa.setMobOrigin((byte) 1);
        aa.setMob(mob);
        aa.setMobLvl(mob.getLevel());
        aa.setSkillID(skill);
        aa.setSlv((byte) slv);
        aa.setDuration(duration);
        aa.setRect(rect);

        return aa;
    }

    public static AffectedArea getAffectedArea(Char chr, AttackInfo attackInfo) {
        AffectedArea aa = new AffectedArea(-1);

        aa.setSkillID(attackInfo.skillId);
        aa.setSlv(attackInfo.slv);
        aa.setElemAttr(attackInfo.elemAttr);
        aa.setForce(attackInfo.force);
        aa.setOption(attackInfo.option);
        aa.setOwner(chr);
        aa.setCharLevel(chr.getLevel());
        aa.setPosition(chr.getPosition());

        return aa;
    }

    public static AffectedArea getAffectedArea(Char chr, int skillID, int slv, int force, int option) {
        AffectedArea aa = new AffectedArea(-1);

        aa.setSkillID(skillID);
        aa.setSlv(slv);
        aa.setOwner(chr);
        aa.setCharLevel(chr.getLevel());
        aa.setForce(force);
        aa.setOption(option);
        aa.setPosition(chr.getPosition());

        return aa;
    }

    public static AffectedArea getAffectedArea(Char chr, int skillID, int slv) {
        AffectedArea aa = new AffectedArea(-1);

        aa.setSkillID(skillID);
        aa.setSlv(slv);
        aa.setOwner(chr);
        aa.setCharLevel(chr.getLevel());
        aa.setPosition(chr.getPosition());

        return aa;
    }

    public static AffectedArea getPassiveAA(Char chr, int skillID, int slv) {
        AffectedArea aa = new AffectedArea(-1);

        aa.setOwner(chr);
        aa.setCharLevel(chr.getLevel());
        aa.setSkillID(skillID);
        aa.setSlv(slv);
        aa.setRemoveSkill(true);
        aa.setPosition(chr.getPosition());

        return aa;
    }

    public Rect getRect() {
        return rect;
    }

    public void setRect(Rect rect) {
        this.rect = rect;
    }

    public int getCharID() {
        return owner != null ? owner.getId() : 0;
    }

    public Char getOwner() {
        return owner;
    }

    public void setOwner(Char owner) {
        this.owner = owner;
    }

    public int getSkillID() {
        return skillID;
    }

    public void setSkillID(int skillID) {
        this.skillID = skillID;
    }

    public int getForce() {
        return force;
    }

    public void setForce(int force) {
        this.force = force;
    }

    public int getOption() {
        return option;
    }

    public void setOption(int option) {
        this.option = option;
    }

    public int getElemAttr() {
        return elemAttr;
    }

    public void setElemAttr(int elemAttr) {
        this.elemAttr = elemAttr;
    }

    public int getLinkingSkillID() {
        return linkingSkillID;
    }

    public void setLinkingSkillID(int linkingSkillID) {
        this.linkingSkillID = linkingSkillID;
    }

    public int getSlv() {
        return slv;
    }

    public void setSlv(int slv) {
        this.slv = slv;
    }

    public byte getMobOrigin() {
        return mobOrigin;
    }

    public void setMobOrigin(byte mobOrigin) {
        this.mobOrigin = mobOrigin;
    }

    public Mob getMob() {
        return mob;
    }

    public void setMob(Mob mob) {
        this.mob = mob;
    }

    public short getDelay() {
        return delay;
    }

    public void setDelay(short delay) {
        this.delay = delay;
    }

    public boolean isFlip() {
        return flip;
    }

    public void setFlip(boolean flip) {
        this.flip = flip;
    }

    public boolean getRemoveSkill() {
        return removeSkill;
    }

    public void setRemoveSkill(boolean removeSkill) {
        this.removeSkill = removeSkill;
    }

    public boolean hasHitMob() {
        return hitMob;
    }

    public void setHitMob(boolean hasHitMob) {
        this.hitMob = hasHitMob;
    }

    public void setDamage(long damage) {
        this.damage = damage;
    }

    public void handleMobOutside(Mob mob) {
        MobTemporaryStat mts = mob.getTemporaryStat();
        switch (skillID) {
            case FirePoison.CREEPING_TOXIN:
            case FirePoison.HEXA_CREEPING_TOXIN:
            case FirePoison.POISON_MIST:
            case FirePoison.HEXA_POISON_MIST:
            case BowMaster.FLAME_SURGE:
                if (mts.hasBurnFromOwner(getCharID(), skillID)) {
                    mts.removeBurnedInfo(mob, getCharID(), skillID);
                }
                break;
            case Shade.SPIRIT_TRAP:
            case Aran.FINAL_CHARGE:
                if (mts.hasCurrentMobStatBySkillId(skillID)) {
                    mts.removeMobStat(mob, MobStat.Freeze);
                }
                break;
            case NightLord.FRAILTY_CURSE:
                if (mts.hasCurrentMobStatBySkillId(skillID)) {
                    mts.removeMobStat(mob, MobStat.Speed);
                    mts.removeMobStat(mob, MobStat.PAD);
                    mts.removeMobStat(mob, MobStat.PDR);
                    mts.removeMobStat(mob, MobStat.MAD);
                    mts.removeMobStat(mob, MobStat.MDR);
                }
                break;
            case Zero.TIME_DISTORTION:
                if (mts.hasCurrentMobStatBySkillId(skillID)) {
                    mts.removeMobStat(mob, MobStat.Freeze);
                    mts.removeMobStat(mob, MobStat.TotalDamParty);
                }
                break;
            case Pirate.PIRATES_BANNER:
            case BattleMage.WEAKENING_AURA:
            case BattleMage.AURA_SCYTHE:
                if (mts.hasCurrentMobStatBySkillId(skillID)) {
                    mts.removeMobStat(mob, MobStat.PDR);
                    mts.removeMobStat(mob, MobStat.MDR);
                }
                break;
            case Shadower.SMOKE_SCREEN:
                if (mts.hasCurrentMobStatBySkillId(skillID)) {
                    mts.removeMobStat(mob, MobStat.HitCriDamR);
                }
                break;
            case IceLightning.ICE_AGE_TILE:
                if (mts.hasCurrentMobStatBySkillId(skillID)) {
                    mts.removeMobStat(mob, MobStat.Speed);
                }
                break;
        }
    }

    public void handleMobInside(Mob mob) {
        if (getOwner() == null) {
            return;
        }
        Char chr = getField().getCharByID(getCharID());
        if (chr == null) {
            return;
        }
        int skillID = getSkillID();
        Skill skill = chr.getSkill(getSkillID());
        int slv = getSlv();
        SkillInfo si = SkillData.getSkillInfoById(skillID);
        MobTemporaryStat mts = mob.getTemporaryStat();
        EnumMap<MobStat, Option> map = new EnumMap<>(MobStat.class);
        Option o = new Option();
        Option o1 = new Option();
        switch (skillID) {
            case FirePoison.CREEPING_TOXIN:
            case FirePoison.HEXA_CREEPING_TOXIN:
            case FirePoison.POISON_MIST:
            case FirePoison.HEXA_POISON_MIST:
            case BowMaster.FLAME_SURGE:
                BurnedInfo bi = BurnedInfo.createBurnInfo(chr, skillID, slv, damage);
                mts.createAndAddBurnedInfo(mob, bi, skillID);
                break;
            case Shade.SPIRIT_TRAP:
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    o.nOption = 1;
                    o.rOption = skillID;
                    o.tOption = si.getValue(time, slv);
                    mts.addStatOptions(mob, MobStat.Freeze, o);
                }
                break;
            case NightLord.FRAILTY_CURSE:
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    if (!mob.isBoss() || chr.hasSkill(NightLord.FRAILTY_CURSE_BOSS_RUSH)) {
                        o.nOption = si.getValue(SkillStat.y, slv) - chr.getSkillStatValue(s, NightLord.FRAILTY_CURSE_SLOW); // already negative in SI
                        o.rOption = skillID;
                        o.tOption = si.getValue(time, slv);
                        map.put(MobStat.Speed, o);
                        o1.nOption = -si.getValue(SkillStat.w, slv) - chr.getSkillStatValue(v, NightLord.FRAILTY_CURSE_ENHANCE);
                        o1.rOption = skillID;
                        o1.tOption = si.getValue(time, slv);
                        map.put(MobStat.PAD, o1);
                        map.put(MobStat.PDR, o1.deepCopy());
                        map.put(MobStat.MAD, o1.deepCopy());
                        map.put(MobStat.MDR, o1.deepCopy());
                        mts.addStatOptions(mob, map);
                    }
                }
                break;
            case Zero.TIME_DISTORTION:
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    mts.removeBuffs(mob);
                    o.nOption = 1;
                    o.rOption = skillID;
                    o.tOption = 5;
                    map.put(MobStat.Freeze, o);
                    o1.nOption = si.getValue(SkillStat.x, slv);
                    o1.rOption = skillID;
                    o1.tOption = 5;
                    map.put(MobStat.TotalDamParty, o1);
                    mts.addStatOptions(mob, map);
                }
                break;
            case Aran.FINAL_CHARGE:
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    o.nOption = 1;
                    o.rOption = skillID;
                    o.tOption = 3;
                    mts.addStatOptions(mob, MobStat.Freeze, o);
                }
                break;
            case Pirate.PIRATES_BANNER:
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    o.nOption = -si.getValue(z, slv);
                    o.rOption = skillID;
                    o.tOption = 5;
                    map.put(MobStat.PDR, o);
                    map.put(MobStat.MDR, o.deepCopy());
                    mts.addStatOptions(mob, map);
                }
                break;
            case BattleMage.WEAKENING_AURA:
            case BattleMage.AURA_SCYTHE:
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    si = SkillData.getSkillInfoById(BattleMage.WEAKENING_AURA);
                    slv = getOwner().getSkillLevel(BattleMage.WEAKENING_AURA);
                    o.nOption = -si.getValue(SkillStat.x, slv);
                    o.rOption = si.getSkillId();
                    o.tOption = si.getValue(time, slv);
                    map.put(MobStat.PDR, o);
                    map.put(MobStat.MDR, o.deepCopy());
                    mts.addStatOptions(mob, map);
                }
                break;
            case Shadower.SMOKE_SCREEN:
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    o.nOption = si.getValue(SkillStat.x, slv);
                    o.rOption = skillID;
                    o.tOption = 7;
                    mts.addStatOptions(mob, MobStat.HitCriDamR, o);
                }
                break;
            case IceLightning.ICE_AGE_TILE: {
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    int counter = 1;
                    if (mts.hasCurrentMobStat(MobStat.Speed)) {
                        counter = mts.getCurrentOptionsByMobStat(MobStat.Speed).mOption;
                        if (counter < 5) {
                            counter++;
                        }
                    }
                    o1.nOption = 20;
                    o1.rOption = skillID;
                    o1.tOption = 15; //No Duration given
                    o1.mOption = counter;
                    mts.addStatOptions(mob, MobStat.Speed, o1);
                }
                break;
            }
        }
    }

    public void handleCharOutside(Char chr) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        switch (skillID) {
            case Zero.TIME_DISTORTION:
                if (tsm.getOptByCTSAndSkill(IndieBooster, skillID) != null) {
                    tsm.removeIndieStat(IndieBooster, tsm.getOptByCTSAndSkill(IndieBooster, skillID));
                }
                break;
            case BlazeWizard.BURNING_CONDUIT:
                if (tsm.getOptByCTSAndSkill(IndieDamR, BlazeWizard.BURNING_CONDUIT_BUFF) != null) {
                    tsm.removeIndieStat(IndieDamR, tsm.getOptByCTSAndSkill(IndieDamR, BlazeWizard.BURNING_CONDUIT_BUFF));
                    tsm.removeIndieStat(IndieBooster, tsm.getOptByCTSAndSkill(IndieBooster, BlazeWizard.BURNING_CONDUIT_BUFF));
                }
                break;
            case Shadower.SMOKE_SCREEN:
                if (tsm.getOptByCTSAndSkill(PVPDamage, skillID) != null) {
                    tsm.removeStat(PVPDamage);
                }
                break;
            case Xenon.HYPOGRAM_FIELD_FUSION:
                if (tsm.getOptByCTSAndSkill(IndieDamR, skillID) != null) {
                    tsm.removeIndieStat(IndieDamR, tsm.getOptByCTSAndSkill(IndieDamR, skillID));
                }
                break;
            case Xenon.HYPOGRAM_FIELD_SUPPORT:
            case Xenon.HEXA_HYPOGRAM_FIELD_SUPPORT:
                if (tsm.getOptByCTSAndSkill(IndieMHPR, skillID) != null) {
                    tsm.removeIndieStat(IndieMHPR, tsm.getOptByCTSAndSkill(IndieMHPR, skillID));
                }
                break;
            case Kanna.BELLFLOWER_BARRIER:
                if (tsm.getOptByCTSAndSkill(IndieDamR, skillID) != null) {
                    tsm.removeIndieStat(IndieDamR, tsm.getOptByCTSAndSkill(IndieDamR, skillID));
                    tsm.removeStat(BossDamageRate);
                }
                break;
            case Kanna.BLOSSOM_BARRIER:
                if (tsm.getOptByCTSAndSkill(DamageReduce, skillID) != null) {
                    tsm.removeStat(DamageReduce);
                    tsm.removeStat(AsrR);
                    tsm.removeStat(TerR);
                }
                break;
            case Pirate.PIRATES_BANNER:
                if (tsm.getOptByCTSAndSkill(IndieStatR, skillID) != null) {
                    tsm.removeIndieStat(IndieStatR, tsm.getOptByCTSAndSkill(IndieStatR, skillID));
                }
                break;
            case Kanna.SPIRITS_DOMAIN:
                if (tsm.getOptByCTSAndSkill(IndiePMdR, skillID) != null) {
                    tsm.removeIndieStat(IndiePMdR, tsm.getOptByCTSAndSkill(IndiePMdR, skillID));
                    tsm.removeIndieStat(IndieAsrR, tsm.getOptByCTSAndSkill(IndieAsrR, skillID));
                    tsm.removeIndieStat(IndieBooster, tsm.getOptByCTSAndSkill(IndieBooster, skillID));
                }
                break;
            case Pathfinder.OBSIDIAN_BARRIER_BURST:
            case Pathfinder.OBSIDIAN_BARRIER_TORRENT:
                if (tsm.getOptByCTSAndSkill(DamageReduce, skillID) != null) {
                    tsm.removeStat(DamageReduce);
                }
                break;
            // Auras
            case BattleMage.HASTY_AURA:
                if (tsm.getOptByCTSAndSkill(BMageAuraYellow, skillID) != null) {
                    tsm.removeIndieStat(IndieSpeed, tsm.getOptByCTSAndSkill(IndieSpeed, skillID));
                    tsm.removeIndieStat(IndieBooster, tsm.getOptByCTSAndSkill(IndieBooster, skillID));
                    tsm.removeStat(BMageAuraYellow);
                }
                break;
            case BattleMage.DRAINING_AURA:
                if (tsm.getOptByCTSAndSkill(BMageAuraDrain, skillID) != null) {
                    tsm.removeIndieStat(IndieDrainHP, tsm.getOptByCTSAndSkill(IndieDrainHP, skillID));
                    tsm.removeStat(BMageAuraDrain);
                }
                break;
            case BattleMage.BLUE_AURA:
                if (tsm.getOptByCTSAndSkill(BMageAuraBlue, skillID) != null) {
                    tsm.removeIndieStat(IndieAsrR, tsm.getOptByCTSAndSkill(IndieAsrR, skillID));
                    tsm.removeIndieStat(IndieTerR, tsm.getOptByCTSAndSkill(IndieTerR, skillID));
                    tsm.removeStat(DamageReduce);
                    tsm.removeStat(BMageAuraBlue);
                }
                break;
            case BattleMage.DARK_AURA:
                if (tsm.getOptByCTSAndSkill(BMageAuraDark, skillID) != null) {
                    tsm.removeIndieStat(IndieDamR, tsm.getOptByCTSAndSkill(IndieDamR, skillID));
                    tsm.removeStat(BMageAuraDark);
                }
                break;
            case Bishop.BENEDICTION:
            case Bishop.HEXA_BENEDICTION:
                if (tsm.getOptByCTSAndSkill(IndiePMdR, skillID) == null) {
                    tsm.removeIndieStat(IndiePMdR, tsm.getOptByCTSAndSkill(IndiePMdR, skillID));
                    tsm.removeIndieStat(IndieDamR, tsm.getOptByCTSAndSkill(IndieDamR, skillID));
                    tsm.removeIndieStat(IndieBooster, tsm.getOptByCTSAndSkill(IndieBooster, skillID));
                }
                break;
        }
    }

    public void handleCharInside(Char chr) {
        if (getOwner() == null) {
            return;
        }
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
        if (!tsm.hasAffectedArea(this)) {
            tsm.addAffectedArea(this);
        }
        int skillID = getSkillID();
        int slv = getSlv();
        SkillInfo si = SkillData.getSkillInfoById(skillID);
        Option o1 = new Option();
        Option o2 = new Option();
        Option o3 = new Option();
        Option o4 = new Option();
        switch (skillID) {
            case Zero.TIME_DISTORTION:
                if (tsm.getOptByCTSAndSkill(IndieBooster, skillID) == null) {
                    tsm.removeAllDebuffs();
                    o1.nReason = skillID;
                    o1.nValue = si.getValue(indieBooster, slv);
                    o1.tTerm = si.getValue(time, slv);
                    newStats.put(IndieBooster, o1); // Indie
                }
                break;
            case BlazeWizard.BURNING_CONDUIT:
                if (tsm.getOptByCTSAndSkill(IndieDamR, BlazeWizard.BURNING_CONDUIT_BUFF) == null) {
                    o1.nReason = BlazeWizard.BURNING_CONDUIT_BUFF;
                    o1.nValue = si.getValue(indieDamR, slv);
                    o1.tTerm = si.getValue(time, slv);
                    newStats.put(IndieDamR, o1); // Indie
                    o2.nReason = BlazeWizard.BURNING_CONDUIT_BUFF;
                    o2.nValue = si.getValue(indieBooster, slv);
                    o2.tTerm = si.getValue(time, slv);
                    newStats.put(IndieBooster, o2); // Indie
                }
                break;
            case Kanna.BELLFLOWER_BARRIER:
                if (tsm.getOptByCTSAndSkill(BossDamageRate, skillID) == null) {
                    o1.nOption = si.getValue(bdR, slv) + (getOwner().hasSkill(Kanna.BELLFLOWER_BARRIER_BOSS_RUSH_H) ? 20 : 0);
                    o1.rOption = skillID;
                    o1.tOption = si.getValue(time, slv);
                    newStats.put(BossDamageRate, o1);
                    o2.nReason = skillID;
                    o2.nValue = si.getValue(indieDamR, slv);
                    o2.tTerm = si.getValue(time, slv);
                    newStats.put(IndieDamR, o2);
                }
                break;
            case Kanna.BLOSSOM_BARRIER:
                if (tsm.getOptByCTSAndSkill(DamageReduce, skillID) == null) {
                    o1.nOption = si.getValue(SkillStat.x, slv);
                    o1.rOption = skillID;
                    newStats.put(DamageReduce, o1);
                    o2.nOption = si.getValue(SkillStat.y, slv);
                    o2.rOption = skillID;
                    newStats.put(AsrR, o2);
                    newStats.put(TerR, o2.deepCopy());
                }
                break;
            case Aran.MAHAS_DOMAIN:
                chr.heal((int) (chr.getMaxHP() / ((double) 100 / si.getValue(w, slv))));
                chr.healMP((int) (chr.getMaxHP() / ((double) 100 / si.getValue(w, slv))));
                tsm.removeAllDebuffs();
                break;
            case Shadower.SMOKE_SCREEN:
                if (tsm.getOptByCTSAndSkill(PVPDamage, skillID) == null) {
                    o1.nOption = 1;
                    o1.rOption = skillID;
                    o1.tOption = si.getValue(time, slv);
                    newStats.put(PVPDamage, o1);
                }
                break;
            case Xenon.HYPOGRAM_FIELD_FUSION:
                if (tsm.getOptByCTSAndSkill(IndieDamR, skillID) == null) {
                    o1.nReason = skillID;
                    o1.nValue = si.getValue(indieDamR, slv);
                    o1.wOption = getCharID();
                    o1.tTerm = si.getValue(time, slv);
                    newStats.put(IndieDamR, o1);
                }
                break;
            case Xenon.HYPOGRAM_FIELD_SUPPORT:
            case Xenon.HEXA_HYPOGRAM_FIELD_SUPPORT:
                if (tsm.getOptByCTSAndSkill(IndieMHPR, skillID) == null) {
                    o1.nReason = skillID;
                    o1.nValue = si.getValue(indieMhpR, slv);
                    o1.wOption = getCharID();
                    o1.tTerm = si.getValue(time, slv);
                    newStats.put(IndieMHPR, o1);
                }
                break;
            case Pirate.PIRATES_BANNER:
                if (tsm.getOptByCTSAndSkill(IndieStatR, skillID) == null) {
                    o1.nReason = skillID;
                    o1.nValue = si.getValue(indieStatRBasic, slv);
                    o1.tTerm = si.getValue(time, slv);
                    newStats.put(IndieStatR, o1);
                }
                break;
            case Kanna.SPIRITS_DOMAIN:
                if (tsm.getOptByCTSAndSkill(IndiePMdR, skillID) == null) {
                    int stateMultiplier = getOption() + 1;
                    o1.nValue = si.getValue(SkillStat.x, slv) * stateMultiplier;
                    o1.nReason = skillID;
                    o1.tTerm = si.getValue(time, slv);
                    newStats.put(IndiePMdR, o1);
                    o2.nValue = si.getValue(w, slv) * stateMultiplier;
                    o2.nReason = skillID;
                    o2.tTerm = si.getValue(time, slv);
                    newStats.put(IndieAsrR, o2);
                    if (stateMultiplier == 3) {
                        o3.nValue = -2;
                        o3.nReason = skillID;
                        o3.tTerm = si.getValue(time, slv);
                        newStats.put(IndieBooster, o3);
                    }
                }
                break;
            case Pathfinder.OBSIDIAN_BARRIER_BURST:
            case Pathfinder.OBSIDIAN_BARRIER_TORRENT:
                if (tsm.getOptByCTSAndSkill(DamageReduce, skillID) == null) {
                    o1.nOption = si.getValue(SkillStat.y, slv);
                    o1.rOption = skillID;
                    o1.tOption = si.getValue(time, slv);
                    newStats.put(DamageReduce, o1);
                }
                break;
            // Auras
            case BattleMage.HASTY_AURA:
                if (tsm.getOptByCTSAndSkill(BMageAuraYellow, skillID) == null) {
                    o1.nReason = skillID;
                    o1.nValue = si.getValue(indieSpeed, slv);
                    o1.tTerm = si.getValue(time, slv);
                    newStats.put(IndieSpeed, o1);
                    o2.nReason = skillID;
                    o2.nValue = -1;
                    o2.tTerm = si.getValue(time, slv);
                    newStats.put(IndieBooster, o2);
                    o3.nOption = 1;
                    o3.rOption = skillID;
                    o3.tOption = si.getValue(time, slv);
                    newStats.put(BMageAuraYellow, o3);
                }
                break;
            case BattleMage.DRAINING_AURA:
                if (tsm.getOptByCTSAndSkill(BMageAuraDrain, skillID) == null) {
                    o1.nReason = skillID;
                    o1.nValue = si.getValue(killRecoveryR, slv);
                    o1.tTerm = si.getValue(time, slv);
                    newStats.put(IndieDrainHP, o1);
                    o2.nOption = 1;
                    o2.rOption = skillID;
                    o2.xOption = chr.getId();
                    o2.tOption = si.getValue(time, slv);
                    newStats.put(BMageAuraDrain, o2);
                }
                break;
            case BattleMage.BLUE_AURA:
                if (tsm.getOptByCTSAndSkill(BMageAuraBlue, skillID) == null) {
                    o2.nReason = skillID;
                    o2.nValue = si.getValue(asrR, slv);
                    o2.tOption = si.getValue(time, slv);
                    newStats.put(IndieAsrR, o2);
                    o3.nReason = skillID;
                    o3.nValue = si.getValue(terR, slv);
                    o3.tTerm = si.getValue(time, slv);
                    newStats.put(IndieTerR, o3);
                    o4.nOption = si.getValue(SkillStat.y, slv);
                    o4.rOption = skillID;
                    o4.tOption = si.getValue(time, slv);
                    newStats.put(DamageReduce, o4);
                    o1.nOption = 1;
                    o1.rOption = skillID;
                    o1.tOption = si.getValue(time, slv);
                    newStats.put(BMageAuraBlue, o1);
                }
                break;
            case BattleMage.DARK_AURA:
                if (tsm.getOptByCTSAndSkill(BMageAuraDark, skillID) == null) {
                    o1.nReason = skillID;
                    o1.nValue = si.getValue(indieDamR, slv);
                    o1.tTerm = si.getValue(time, slv);
                    newStats.put(IndieDamR, o1);
                    o3.nOption = 1;
                    o3.rOption = skillID;
                    o3.tOption = si.getValue(time, slv);
                    newStats.put(BMageAuraDark, o3);
                }
                break;
            case Bishop.HOLY_WATER:
                int heal = (int) (chr.getMaxHP() / ((double) 100 / si.getValue(SkillStat.w, slv)));
                if (owner != null) {
                    int inte = owner.getStat(Stat.inte);
                    int stack = inte >= 2500 ? inte / 2500 : 0;
                    heal += heal * stack;
                }
                chr.heal(heal);
                break;
            case Bishop.BENEDICTION:
            case Bishop.HEXA_BENEDICTION:
                if (tsm.getOptByCTSAndSkill(IndiePMdR, skillID) == null) {
                    // Final Damage (all party members in range): +q%:
                    o1.nReason = skillID;
                    o1.nValue = si.getValue(q, slv);
                    o1.tTerm = si.getValue(time, slv);
                    newStats.put(IndiePMdR, o1);
                    // Periodically recovers x% Max HP/MP:
                    chr.heal((int) (chr.getMaxHP() / ((double) 100 / si.getValue(SkillStat.x, slv))));
                    chr.healMP((int) (chr.getMaxMP() / ((double) 100 / si.getValue(SkillStat.x, slv))));
                    // Clears certain statuses:
                    tsm.removeAllDebuffs();
                    // Final Damage: +1% per q2 INT (up to +75% max):
                    o2.nReason = skillID;
                    o2.nValue = Math.min((int) Math.floor(chr.getTotalStat(BaseStat.inte) / si.getValue(q2, slv)),
                            chr.hasSkill(Bishop.BENEDICTION) ? 75 : 40) + si.getValue(dot, slv);
                    o2.tTerm = si.getValue(time, slv);
                    newStats.put(IndieDamR, o2);
                    // Attack Speed: +1 per u INT (up to +3 max):
                    o3.nReason = skillID;
                    o3.nValue = Math.min((int) Math.floor(chr.getTotalStat(BaseStat.inte) / si.getValue(u, slv)), 3);
                    o3.tTerm = si.getValue(time, slv);
                    newStats.put(IndieBooster, o3);
                }
                break;
        }
        if (!newStats.isEmpty()) {
            tsm.sendStat(newStats);
        }
    }

    public void activateTimer(int initialDelayMS, int delayMS) {
        ScheduledFuture<?> sf = getTimer().addFixedRateEvent(this::doScheduledFuture, initialDelayMS, delayMS, false);
        this.aaTimer = sf;
        GlobalTimerManager.addCharTimer(getCharID(), sf);
    }

    private void doScheduledFuture() {
        int skillID = getSkillID();
        SkillInfo si = SkillData.getSkillInfoById(getSkillID());
        int slv = getSlv();
        Field field = getField();
        List<Char> chrList = field.getCharsInRect(getRect());
        switch (skillID) {
            case Xenon.TEMPORAL_POD:
                for (Char chr : chrList) {
                    if (!chr.hasSkillOnCooldown(SkillConstants.XENON_POD_FOR_COOLDOWN)) {
                        for (int skillId : chr.getSkillCoolTimes().keySet()) {
                            si = SkillData.getSkillInfoById(skillId);
                            if (si != null) {
                                chr.reduceSkillCoolTime(skillId, 1000);
                            }
                        }
                        chr.addSkillCooldown(SkillConstants.XENON_POD_FOR_COOLDOWN, 950);
                    }
                }
                break;
            case Aran.MAHAS_DOMAIN:
                for (Char chr : chrList) {
                    TemporaryStatManager tsm = chr.getTemporaryStatManager();
                    tsm.removeAllDebuffs();
                    chr.heal((int) ((chr.getMaxHP() * si.getValue(w, slv)) / 100D));
                    chr.healMP((int) ((chr.getMaxMP() * si.getValue(w, slv)) / 100D));
                }
                break;
            case Kanna.SPIRITS_DOMAIN:
                int stateMultiplier = getOption() + 1;
                int healed = si.getValue(SkillStat.y, slv) * stateMultiplier;
                for (Char chr : chrList) {
                    chr.heal(chr.getHPPerc(healed));
                    chr.healMP((int) ((chr.getMaxMP() * healed) / 100D));
                }
                break;
            case Kanna.BLOSSOM_BARRIER: // Only if Mana Vein is nearby
            case Kanna.BELLFLOWER_BARRIER:
            case Kanna.MANA_VEIN:
                si = SkillData.getSkillInfoById(Kanna.MANA_VEIN);
                int healedMana = si.getValue(w, slv);
                if (skillID != Kanna.MANA_VEIN) {
                    healedMana -= getOwner().getSkillStatValue(w, Kanna.WARDING_BARRIER);
                }
                int finalHealedMana = healedMana;
                chrList.stream().filter(c -> c == getOwner()).findFirst().ifPresent(chrInsideAA -> chrInsideAA.healMP(finalHealedMana));
                break;
            default:
                aaTimer.cancel(false);
                break;
        }
    }

    public void handleAARemoved() {
        Field field = getField();
        if (getMobOrigin() > 0) {
            // Mob Affected Areas
            if (getSkillID() == 131 && getSlv() == 28) {
                // Demian Flying Sword Affected Area.
                Life life = field.getLifeByObjectID(getLinkingSkillID());
                if (life == null) {
                    return;
                }
                if (life instanceof DemianFlyingSword sword) {
                    List<Position> path;
                    if (new Random().nextBoolean()) {
                        path = DemianFlyingSwordPath.flyingSwordPathBouncing1;
                    } else {
                        path = DemianFlyingSwordPath.flyingSwordPathBouncing2;
                    }
                    sword.setDemianFlyingSwordPath(DemianFlyingSwordPath.flyingSwordBouncingPath(path));
                    sword.startPath();
                    sword.target();
                }
            }
        } else {
            // Char Affected Areas
        }
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    @Override
    public void broadcastSpawnPacket(Char onlyChar) {
        Field field = getField();
        field.broadcast(FieldPacket.affectedAreaCreated(this));
        field.checkCharInAffectedAreas(onlyChar);
    }

    @Override
    public void broadcastLeavePacket() {
        if (aaTimer != null) {
            aaTimer.cancel(false);
        }
        Field field = getField();

        net.swordie.ms.client.jobs.flora.Tms282AdeleShardbreaker.removedArea(this);
        handleAARemoved(); // Used for special cases, where something has to happen upon removal of AA.

        field.broadcast(FieldPacket.affectedAreaRemoved(this));
        for (Char chr : field.getChars()) {
            TemporaryStatManager tsm = chr.getTemporaryStatManager();
            if (tsm.hasAffectedArea(this)) {
                tsm.removeStatsBySkill(getSkillID());
            }
        }
    }

    public int getCharLevel() {
        return charLevel;
    }

    public void setCharLevel(int charLevel) {
        this.charLevel = charLevel;
    }

    public int getMobLvl() {
        return mobLvl;
    }

    public void setMobLvl(int mobLvl) {
        this.mobLvl = mobLvl;
    }

    public Set<AffectedAreaSpeacial> getAffectedAreaSpeacials() {
        return affectedAreaSpeacials;
    }

    public void setAffectedAreaSpeacials(Set<AffectedAreaSpeacial> affectedAreaSpeacials) {
        this.affectedAreaSpeacials = affectedAreaSpeacials;
    }
}
