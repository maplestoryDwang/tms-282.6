package net.swordie.ms.life.mob.skill;


import net.swordie.ms.connection.protocol.codec.MobPacketEncoder;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TemporaryStatBase;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.client.jobs.adventurer.magician.Bishop;
import net.swordie.ms.client.jobs.adventurer.magician.Magician;
import net.swordie.ms.client.jobs.adventurer.thief.DualBlade;
import net.swordie.ms.client.jobs.adventurer.thief.Thief;
import net.swordie.ms.client.jobs.cygnus.WindArcher;
import net.swordie.ms.connection.packet.*;
import net.swordie.ms.constants.BossConstants;
import net.swordie.ms.enums.*;
import net.swordie.ms.life.AffectedArea;
import net.swordie.ms.life.Reactor;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.life.mob.MobStat;
import net.swordie.ms.life.mob.MobTemporaryStat;
import net.swordie.ms.life.mob.boss.demian.DemainDelayedAttack;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.loaders.StringData;
import net.swordie.ms.loaders.containerclasses.MobSkillInfo;
import net.swordie.ms.util.Position;
import net.swordie.ms.util.Randomizer;
import net.swordie.ms.util.Rect;
import net.swordie.ms.util.Util;
import net.swordie.ms.util.container.Triple;
import net.swordie.ms.util.container.Tuple;
import net.swordie.ms.world.field.Field;
import net.swordie.ms.world.field.Portal;
import net.swordie.ms.world.field.fieldeffect.FieldEffect;
import net.swordie.ms.world.field.obtacleatom.ObtacleAtomInfo;
import net.swordie.ms.world.field.obtacleatom.ObtacleRadianInfo;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.Attract;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.FireBomb;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.ReturnTeleport;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;
import static net.swordie.ms.life.mob.skill.MobSkillID.*;
import static net.swordie.ms.life.mob.skill.MobSkillStat.*;


/**
 * Created on 2/28/2018.
 */
public class MobSkill {

    private int skillSN;
    private byte action;
    private int level;
    private int effectAfter;
    private int skillAfter;
    private byte priority;
    private boolean firstAttack;
    private boolean onlyFsm;
    private boolean onlyOtherSkill;
    private int skillForbid;
    private int afterDelay;
    private int fixDamR;
    private boolean doFirst;
    private int preSkillIndex;
    private int preSkillCount;
    private String info;
    private String text;
    private boolean afterDead;
    private boolean flip;
    private int afterAttack = -1;
    private int afterAttackCount;
    private int castTime;
    private int coolTime;
    private int delay;
    private int useLimit;
    private String speak;
    private int skillID;
    private int disease;
    private int sequenceDelay;
    private boolean spawnTwice = false; //Use for pierre.

    public int getSkillSN() {
        return skillSN;
    }

    public void setSkillSN(int skillSN) {
        this.skillSN = skillSN;
    }

    public byte getAction() {
        return action;
    }

    public void setAction(byte action) {
        this.action = action;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getEffectAfter() {
        return effectAfter;
    }

    public void setEffectAfter(int effectAfter) {
        this.effectAfter = effectAfter;
    }

    public int getSkillDelay() {
        return skillAfter;
    }

    public void setSkillAfter(int skillAfter) {
        this.skillAfter = skillAfter;
    }

    public byte getPriority() {
        return priority;
    }

    public void setPriority(byte priority) {
        this.priority = priority;
    }

    public boolean getFirstAttack() {
        return firstAttack;
    }

    public void setFirstAttack(boolean firstAttack) {
        this.firstAttack = firstAttack;
    }

    public boolean isOnlyFsm() {
        return onlyFsm;
    }

    public void setOnlyFsm(boolean onlyFsm) {
        this.onlyFsm = onlyFsm;
    }

    public boolean isOnlyOtherSkill() {
        return onlyOtherSkill;
    }

    public void setOnlyOtherSkill(boolean onlyOtherSkill) {
        this.onlyOtherSkill = onlyOtherSkill;
    }

    public int getSkillForbid() {
        return skillForbid;
    }

    public void setSkillForbid(int skillForbid) {
        this.skillForbid = skillForbid;
    }

    public int getAfterDelay() {
        return afterDelay;
    }

    public void setAfterDelay(int afterDelay) {
        this.afterDelay = afterDelay;
    }

    public int getFixDamR() {
        return fixDamR;
    }

    public void setFixDamR(int fixDamR) {
        this.fixDamR = fixDamR;
    }

    public boolean isDoFirst() {
        return doFirst;
    }

    public void setDoFirst(boolean doFirst) {
        this.doFirst = doFirst;
    }

    public int getPreSkillIndex() {
        return preSkillIndex;
    }

    public void setPreSkillIndex(int preSkillIndex) {
        this.preSkillIndex = preSkillIndex;
    }

    public int getPreSkillCount() {
        return preSkillCount;
    }

    public void setPreSkillCount(int preSkillCount) {
        this.preSkillCount = preSkillCount;
    }

    public String getInfo() {
        return info == null ? "" : info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getText() {
        return text == null ? "" : text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isAfterDead() {
        return afterDead;
    }

    public void setAfterDead(boolean afterDead) {
        this.afterDead = afterDead;
    }

    public int getAfterAttack() {
        return afterAttack;
    }

    public void setAfterAttack(int afterAttack) {
        this.afterAttack = afterAttack;
    }

    public int getAfterAttackCount() {
        return afterAttackCount;
    }

    public void setAfterAttackCount(int afterAttackCount) {
        this.afterAttackCount = afterAttackCount;
    }

    public int getCastTime() {
        return castTime;
    }

    public void setCastTime(int castTime) {
        this.castTime = castTime;
    }

    public int getCoolTime() {
        return coolTime;
    }

    public void setCoolTime(int coolTime) {
        this.coolTime = coolTime;
    }

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public int getUseLimit() {
        return useLimit;
    }

    public void setUseLimit(int useLimit) {
        this.useLimit = useLimit;
    }

    public String getSpeak() {
        return speak == null ? "" : speak;
    }

    public void setSpeak(String speak) {
        this.speak = speak;
    }

    public int getSkillID() {
        return skillID;
    }

    public void setSkillID(int skillID) {
        this.skillID = skillID;
    }

    public int getDisease() {
        return disease;
    }

    public void setDisease(int disease) {
        this.disease = disease;
    }

    public int getSequenceDelay() {
        return sequenceDelay;
    }

    public void setSequenceDelay(int sequenceDelay) {
        this.sequenceDelay = sequenceDelay;
    }

    public boolean isSpawnTwice() {
        return spawnTwice;
    }

    public void setSpawnTwice(boolean spawnTwice) {
        this.spawnTwice = spawnTwice;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MobSkill mobSkill = (MobSkill) o;
        return skillSN == mobSkill.skillSN &&
                skillID == mobSkill.skillID &&
                level == mobSkill.level;
    }

    @Override
    public int hashCode() {
        return Objects.hash(skillSN, skillID, level);
    }

    public void applyEffect(Mob mob) {
        MobTemporaryStat mts = mob.getTemporaryStat();
        int skill = getSkillID();
        short slv = (short) getLevel();
        MobSkillInfo msi = SkillData.getMobSkillInfoByIdAndLevel(skill, slv);
        MobSkillID msID = MobSkillID.getMobSkillIDByVal(skill);
        Field field = mob.getField();

        Option o = new Option(skill);
        o.slv = slv;
        o.tOption = Util.getRandom(3, 10);//msi.getSkillStatIntValue(time);

        Option o2 = new Option(skill);
        o2.slv = slv;
        o2.tOption = Util.getRandom(3, 10);//msi.getSkillStatIntValue(time);

        Rect rect = null;
        Set<Mob> mobs = new HashSet<>();
        Set<Char> chars = new HashSet<>();
        if (msi.getLt() != null && msID != MobSkillID.Damage && slv != 27) {
            rect = new Rect(msi.getLt(), msi.getRb());
            if (mob.isFlip()) {
                rect.horizontalFlipAround(mob.getPosition().getX());
            }
            mobs.addAll(field.getMobsInRect(rect));
            chars.addAll(field.getCharsInRect(rect));
        } else if (msi.getLt2() != null && msID != MobSkillID.Damage && slv != 27) {
            rect = new Rect(msi.getLt2(), msi.getRb2());
            if (mob.isFlip()) {
                rect.horizontalFlipAround(mob.getPosition().getX());
            }
            mobs.addAll(field.getMobsInRect(rect));
            chars.addAll(field.getCharsInRect(rect));
        }
        if (chars.isEmpty()) { //Get Special Rect Skill.
            rect = BossConstants.getMobSkillRect(mob, slv);
            chars.addAll(field.getCharsInRect(rect));
        }
        switch (msID) {
            case PowerUp:
            case PowerUpM:
                o.nOption = msi.getSkillStatIntValue(x);
                mts.addMobSkillOptions(mob, MobStat.PowerUp, o);
                for (Mob m : mobs) {
                    m.getTemporaryStat().addMobSkillOptions(mob, MobStat.PowerUp, o);
                }
                break;
            case MagicUp:
            case MagicUpM:
                o.nOption = msi.getSkillStatIntValue(x);
                mts.addMobSkillOptions(mob, MobStat.MagicUp, o);
                for (Mob m : mobs) {
                    m.getTemporaryStat().addMobSkillOptions(mob, MobStat.MagicUp, o);
                }
                break;
            case PGuardUp:
            case PGuardUpM:
                o.nOption = msi.getSkillStatIntValue(x);
                mts.addMobSkillOptions(mob, MobStat.PGuardUp, o);
                for (Mob m : mobs) {
                    m.getTemporaryStat().addMobSkillOptions(mob, MobStat.PGuardUp, o);
                }
                break;
            case MGuardUp:
            case MGuardUpM:
                o.nOption = msi.getSkillStatIntValue(x);
                mts.addMobSkillOptions(mob, MobStat.MGuardUp, o);
                for (Mob m : mobs) {
                    m.getTemporaryStat().addMobSkillOptions(mob, MobStat.MGuardUp, o);
                }
                break;
            case HealM:
                for (Mob m : mobs) {
                    m.heal(msi.getSkillStatIntValue(x));
                }
                break;
            case GiveMeHeal:
                for (Mob m : mobs) {
                    m.heal((int) m.getMaxHp());
                }
                break;
            case Haste:
            case HasteM:
                o.nOption = msi.getSkillStatIntValue(x);
                mts.addMobSkillOptions(mob, MobStat.Speed, o);
                for (Mob m : mobs) {
                    m.getTemporaryStat().addMobSkillOptions(mob, MobStat.Speed, o);
                }
                break;
            case Hardskin:
                o.nOption = msi.getSkillStatIntValue(x);
                mts.addMobSkillOptions(mob, MobStat.HardSkin, o);
                for (Mob m : mobs) {
                    m.getTemporaryStat().addMobSkillOptions(mob, MobStat.HardSkin, o);
                }
                break;
            case Dazzle: {
                for (Char chr : field.getChars()) {
                    Option attract = new Option();
                    attract.nOption = mob.getPosition().getX() < chr.getPosition().getX() ? 1 : 2;
                    attract.rOption = msID.getVal();
                    attract.slv = slv;
                    attract.tOption = 5;
                    chr.getTemporaryStatManager().sendStat(Attract, attract);
                }
                break;
            }
            case Invincible:
                o.nOption = msi.getSkillStatIntValue(x);
                mob.getTimer().addEvent(() -> {
                    mts.addMobSkillOptions(mob, MobStat.Invincible, o);
                    for (Mob m : mobs) {
                        m.getTemporaryStat().addMobSkillOptions(mob, MobStat.Invincible, o);
                    }
                }, 3, TimeUnit.SECONDS);
                break;
            case Seal:
            case Darkness:
            case Weakness:
            case Stun:
            case Curse:
            case Poison:
            case Slow:
            case Dispel:
            case Attract:
            case Undead:
            case Fear:
            case Frozen:
            case DispelItemOption:
            case PainMark:
            case Magnet:
            case StopPortion:
            case StopMotion:
            case UserBomb:
            case DarkTornado:
            case Lapidification:
            case Deathmark:
            case Slowattack:
            case ReverseInput:
            case ReturnTeleport:
            case DebuffHalf:
                for (Char chr : chars) {
                    applyEffect(chr);
                }
                break;
            case UserMorph:
                for (Char chr : chars) {
                    applyEffect(chr);
                }
                if (mob.getTemplateId() == 8850011 || mob.getTemplateId() == 8850111) {
                    field.broadcast(FieldPacket.smartMobNotice(mob.getTemplateId(), 0, 5, 1, "西格諾斯嘲笑增益技能，並試圖把敵人變成緞帶肥肥。"));
                }
                break;
            case BanMap: {
                Char chr = chars.size() == 0 ? null : Util.getRandomFromCollection(chars);
                if (chr != null) {
                    if (mob.isBanMap()) {
                        if (mob.getBanType() == 1) {
                            if (mob.getBanMsgType() == 1) { // found 2 types (1(most of ban types), 2).
                                String banMsg = mob.getBanMsg();
                                if (banMsg != null && !banMsg.equals("")) {
                                    chr.write(WvsContext.message(MessageType.SYSTEM_MESSAGE, 0, banMsg, (byte) 0));
                                }
                            }
                            Tuple<Integer, String> banMapField = mob.getBanMapFields().get(0);
                            if (banMapField != null) {
                                Field toField = chr.getOrCreateFieldByCurrentInstanceType(banMapField.getLeft());
                                if (toField == null) {
                                    break;
                                }
                                Portal toPortal = toField.getPortalByName(banMapField.getRight());
                                if (toPortal == null) {
                                    toPortal = toField.getPortalByName("sp");
                                }
                                chr.warp(toField, toPortal);
                            }
                        }
                    }
                }
                break;
            }
            case AllKill:
            case Damage: {
                int rId = 0;
                int type = 0;
                switch (slv) {
                    case 6:
                        rId = 2708001;
                        break;
                    case 7:
                        rId = 2708002;
                        break;
                    case 8:
                        rId = 2708003;
                        break;
                    case 9:
                        rId = 2708004;
                        break;
                    case 25:
                    case 26:
                    case 33:
                    case 34:
                        type = mob.getTemplateId() % 10;
                        int minx = (type == 3) ? -85 : ((type == 4) ? -215 : ((type == 5) ? -340 : ((type == 6) ? -465 : ((type == 7) ? 60 : ((type == 8) ? 200 : ((type == 9) ? 350 : 500))))));
                        for (Char chr : field.getChars()) {
                            if (minx - 70 < chr.getPosition().getX() && minx + 70 > chr.getPosition().getX() && chr.getPosition().getY() > -15 && chr.getHP() > 0) {
                                int percent = (mob.getTemplateId() >= 8800023 && mob.getTemplateId() <= 8800030) ? 50 : ((mob.getTemplateId() >= 8800003 && mob.getTemplateId() <= 8800010) ? 90 : ((mob.getTemplateId() >= 8800103 && mob.getTemplateId() <= 8800110) ? 100 : 0));
                                int reduce = 0;
                                TemporaryStatManager tsm = chr.getTemporaryStatManager();
                                if (tsm.hasStat(IndieDamReduceR)) {
                                    reduce = (int) tsm.getTotalNOptionOfStat(IndieDamReduceR);
                                } else if (tsm.hasStat(IndieNotDamaged)) {
                                    continue;
                                }
                                final int minushp = -(int) (chr.getMaxHP() * (percent - reduce) / 100L);
                                chr.damage(minushp);
                            }
                        }
                        break;
                    case 27:
                        type = mob.getTemplateId() % 10;
                        for (Char chr : field.getChars()) {
                            if (chr.getHP() > 0) {
                                boolean damage = false;
                                if (type == 3 || type == 7) {
                                    if (chr.getFoothold() == 20 || chr.getFoothold() == 19 || chr.getFoothold() == 18 || chr.getFoothold() == 15 || chr.getFoothold() == 16 || chr.getFoothold() == 17) {
                                        damage = true;
                                    }
                                } else if (type == 4 || type == 8) {
                                    if (chr.getFoothold() == 14 || chr.getFoothold() == 13 || chr.getFoothold() == 12 || chr.getFoothold() == 11 || chr.getFoothold() == 10 || chr.getFoothold() == 9) {
                                        damage = true;
                                    }
                                } else if ((type == 5 || type == 9) && (chr.getFoothold() == 5 || chr.getFoothold() == 4 || chr.getFoothold() == 3 || chr.getFoothold() == 6 || chr.getFoothold() == 7 || chr.getFoothold() == 8)) {
                                    damage = true;
                                }
                                if (damage) {
                                    chr.damage(chr.getMaxHP());
                                }
                            }
                        }
                        break;
                }
                if (rId != 0 && mob != null) {
                    for (Reactor reactor : field.getReactors()) {
                        if (reactor.getTemplateId() == rId) {
                            reactor.setState((byte) 1);
                            field.broadcast(ReactorPool.reactorChangeState(reactor, (short) delay, 0));
                            break;
                        }
                    }
                }
                for (Char chr : chars) {
                    if (chr != null && chr.getHP() > 0) {
                        final long damage = msi.getSkillStatIntValue(x) > 0 ? msi.getSkillStatIntValue(x) : chr.getMaxHP();
                        chr.write(UserLocal.screenAttack(mob.getObjectId(), getSkillID(), getLevel(), damage));
                        field.broadcast(UserRemote.effect(chr.getId(), Effect.mobSkillHit(getSkillID(), getLevel())), chr);
                    }
                }
                break;
            }
            case Pad:
                o.nOption = msi.getSkillStatIntValue(x);
                mts.addMobSkillOptions(mob, MobStat.PAD, o);
                for (Mob m : mobs) {
                    m.getTemporaryStat().addMobSkillOptions(mob, MobStat.PAD, o);
                }
                break;
            case Mad:
                o.nOption = msi.getSkillStatIntValue(x);
                mts.addMobSkillOptions(mob, MobStat.MAD, o);
                for (Mob m : mobs) {
                    m.getTemporaryStat().addMobSkillOptions(mob, MobStat.MAD, o);
                }
                break;
            case Pdr:
                o.nOption = msi.getSkillStatIntValue(x);
                mts.addMobSkillOptions(mob, MobStat.PDR, o);
                for (Mob m : mobs) {
                    m.getTemporaryStat().addMobSkillOptions(mob, MobStat.PDR, o);
                }
                break;
            case Mdr:
                o.nOption = msi.getSkillStatIntValue(x);
                mts.addMobSkillOptions(mob, MobStat.MDR, o);
                for (Mob m : mobs) {
                    m.getTemporaryStat().addMobSkillOptions(mob, MobStat.MDR, o);
                }
                break;
            case Acc:
                o.nOption = msi.getSkillStatIntValue(x);
                mts.addMobSkillOptions(mob, MobStat.ACC, o);
                for (Mob m : mobs) {
                    m.getTemporaryStat().addMobSkillOptions(mob, MobStat.ACC, o);
                }
                break;
            case Eva:
                o.nOption = msi.getSkillStatIntValue(x);
                mts.addMobSkillOptions(mob, MobStat.EVA, o);
                for (Mob m : mobs) {
                    m.getTemporaryStat().addMobSkillOptions(mob, MobStat.EVA, o);
                }
                break;
            case Speed:
                o.nOption = msi.getSkillStatIntValue(x);
                o.mOption = 1;
                mts.addMobSkillOptions(mob, MobStat.Speed, o);
                for (Mob m : mobs) {
                    m.getTemporaryStat().addMobSkillOptions(mob, MobStat.Speed, o);
                }
                break;
            case SealSkill:
                o.nOption = msi.getSkillStatIntValue(x);
                o.mOption = 1;
                mts.addMobSkillOptions(mob, MobStat.SealSkill, o);
                for (Mob m : mobs) {
                    m.getTemporaryStat().addMobSkillOptions(mob, MobStat.SealSkill, o);
                }
                break;
            case AreaForce:
            case AreaTosp:
            case AreaAbnormal:
            case AreaTimezone:
            case AreaMobBuff:
            case AreaWarning:
            case AreaForceFromUser:
            case AreaFire:
            case AreaPoison: {
                AffectedArea aa = AffectedArea.getMobAA(mob, skill, slv, msi);
                if (msID == AreaPoison && slv == 23) {
                    aa.setSlv((byte) 22);
                }
                if (mob.getTemplateId() == 8920001) {
                    aa.setPosition(mob.getPosition());
                }
                field.spawnAffectedArea(aa);
                break;
            }
            case PhysicalImmune:
                if (!mts.hasCurrentMobStat(MobStat.PImmune)) {
                    o.nOption = msi.getSkillStatIntValue(x);
                    mob.getTimer().addEvent(() -> {
                        mts.addMobSkillOptions(mob, MobStat.PImmune, o);
                    }, 3, TimeUnit.SECONDS);
                }
                break;
            case MagicImmune:
                if (!mts.hasCurrentMobStat(MobStat.MImmune)) {
                    o.nOption = msi.getSkillStatIntValue(x);
                    mob.getTimer().addEvent(() -> {
                        mts.addMobSkillOptions(mob, MobStat.MImmune, o);
                    }, 3, TimeUnit.SECONDS);
                }
                break;
            case PowerImmune:
                if (!mts.hasCurrentMobStat(MobStat.PowerImmune)) {
                    o.nOption = msi.getSkillStatIntValue(x);
                    mob.getTimer().addEvent(() -> {
                        mts.addMobSkillOptions(mob, MobStat.PowerImmune, o);
                    }, 3, TimeUnit.SECONDS);
                }
                break;
            case Teleport:
                int xPos = msi.getSkillStatIntValue(x);
                int yPos = msi.getSkillStatIntValue(y);
                switch (slv) {
                    case 1:
                    case 3:
                        if (mob.getTemplateId() == 8840000 || mob.getTemplateId() == 8840007 || mob.getTemplateId() == 8840014) {
                            field.broadcast(FieldPacket.smartMobNotice(mob.getTemplateId(), 1, 5, 0,
                                    StringData.getMobStringById(mob.getTemplateId()) + " 瞬間移動到對自己威脅最大的敵人身邊。"));
                            break;
                        }
                        break;
                    case 42: {
                        int xFromPos = mob.getPosition().getX();
                        int yFromPos = mob.getPosition().getY();
                        Set<DemainDelayedAttack> ddaSet = new HashSet<>();
                        ddaSet.add(new DemainDelayedAttack(field.getNewObjectID(), new Position(isFlip() ? xFromPos - 900 : xFromPos + 900, yFromPos - Util.getRandom(25, 100)), Util.getRandom(0, 100)));
                        ddaSet.add(new DemainDelayedAttack(field.getNewObjectID(), new Position(isFlip() ? xFromPos - 600 : xFromPos + 600, yFromPos - Util.getRandom(25, 100)), Util.getRandom(0, 100)));
                        ddaSet.add(new DemainDelayedAttack(field.getNewObjectID(), new Position(isFlip() ? xFromPos - 300 : xFromPos + 300, yFromPos - Util.getRandom(25, 100)), Util.getRandom(0, 100)));
                        field.broadcast(MobPool.demainDelayedAttackCreate(mob, isFlip(), getAction(), ddaSet));
                        break;
                    }
                    case 44: {
                        if (mob.getTemplateId() == 8880409) {
                            mob.teleport(12, Util.getRandom(-700, 700), 16);
                            break;
                        }
                        mob.teleport(12, Util.getRandom(-40, 1400), 16);
                        break;
                    }
                    case 45: {
                        int x = mob.isLeft() ? -680 : 680;
                        int xFromPos = mob.getPosition().getX();
                        int yFromPos = mob.getPosition().getY();
                        DemainDelayedAttack dda = new DemainDelayedAttack(field.getNewObjectID(), new Position(xFromPos, yFromPos), 0);
                        field.broadcast(MobPool.demainDelayedAttackCreate(mob, slv, isFlip(), getAction(), dda));
                        break;
                    }
                    case 46:
                    case 47: {
                        mob.getTimer().addEvent(() -> {
                            Char chr = Util.getRandomFromCollection(field.getChars());
                            if (chr != null && field.findFootHoldBelow(chr.getPosition()) != null) {
                                int x = chr.getPosition().getX();
                                int y = field.findFootHoldBelow(chr.getPosition()).getYFromX(x);
                                int angle = isFlip() ? -45 : 45;
                                DemainDelayedAttack dda = new DemainDelayedAttack(field.getNewObjectID(), new Position(x, y), angle);
                                field.broadcast(MobPool.demainDelayedAttackCreate(mob, slv, isFlip(), getAction(), dda));
                            }
                        }, 2000);
                        break;
                    }
                }
                mob.teleport(0, mob.getPosition().getX(), mob.getPosition().getY());
                break;
            case PCounter:
                if (!mts.hasCurrentMobStat(MobStat.PCounter)) {
                    mob.getTimer().addEvent(() -> {
                        o.nOption = msi.getSkillStatIntValue(x);
                        o.mOption = 100;
                        o.bOption = msi.getSkillStatIntValue(MobSkillStat.delay);
                        mts.addMobSkillOptions(mob, MobStat.PCounter, o);
                    }, 3, TimeUnit.SECONDS);
                }
                break;
            case MCounter:
                if (!mts.hasCurrentMobStat(MobStat.MCounter)) {
                    mob.getTimer().addEvent(() -> {
                        o.nOption = msi.getSkillStatIntValue(x);
                        o.mOption = 100;
                        o.bOption = msi.getSkillStatIntValue(MobSkillStat.delay);
                        o.wOption = msi.getSkillStatIntValue(y);
                        mts.addMobSkillOptions(mob, MobStat.MCounter, o);
                    }, 3, TimeUnit.SECONDS);
                }
                break;
            case PMCounter:
                if (!mts.hasCurrentMobStat(MobStat.PCounter) && !mts.hasCurrentMobStat(MobStat.MCounter)) {
                    mob.getTimer().addEvent(() -> {
                        o.nOption = msi.getSkillStatIntValue(x);
                        o.mOption = 100;
                        o.bOption = msi.getSkillStatIntValue(MobSkillStat.delay);
                        o.wOption = msi.getSkillStatIntValue(y);
                        mts.addMobSkillOptions(mob, MobStat.PCounter, o);
                        o2.nOption = msi.getSkillStatIntValue(x);
                        o2.mOption = 100;
                        o2.bOption = msi.getSkillStatIntValue(MobSkillStat.delay);
                        o2.wOption = msi.getSkillStatIntValue(y);
                        mts.addMobSkillOptions(mob, MobStat.MCounter, o2);
                    }, 3, TimeUnit.SECONDS);
                    if (mob.getTemplateId() == 8840000 || mob.getTemplateId() == 8840007 || mob.getTemplateId() == 8840014) {
                        field.broadcast(FieldPacket.smartMobNotice(mob.getTemplateId(), 1, 5, 0, "陷入困境的凡雷恩會對近距離的敵人進行反擊。"));
                    }
                }
                break;
            case Summon:
            case Summon2:
                if (getSkillID() == Summon.getVal()) {
                    if (slv == 228) {
                        field.broadcast(FieldPacket.smartMobNotice(mob.getTemplateId(), 0, 2, 8, "西格諾斯對對方的行為感到憤怒。"));
                    } else if (slv == 212) {
                        field.broadcast(FieldPacket.smartMobNotice(mob.getTemplateId(), 0, 5, 1,
                                StringData.getMobStringById(mob.getTemplateId()) + " 周圍沒有可以吸取體力的怪物，因此試圖召喚新的怪物。"));
                    } else if (slv == 223) {
                        field.broadcast(FieldPacket.smartMobNotice(mob.getTemplateId(), 0, 2, 2, "西格諾斯為騎士們召喚了神獸。"));
                    }
                } else if (getSkillID() == Summon2.getVal()) {
                    if (slv == 160) {
                        field.broadcast(FieldPacket.smartMobNotice(mob.getTemplateId(), 0, 2, 8, "西格諾斯對對方的行為感到憤怒。"));
                    } else if (slv == 159) {
                        field.broadcast(FieldPacket.smartMobNotice(mob.getTemplateId(), 0, 2, 2, "西格諾斯為騎士們召喚了神獸。"));
                    } else if (slv == 210) {
                        field.broadcast(FieldPacket.smartMobNotice(mob.getTemplateId(), 0, 5, 1,
                                StringData.getMobStringById(mob.getTemplateId()) + " 周圍沒有可以吸取體力的怪物，因此試圖召喚新的怪物。"));
                    } else if (slv == 211) {
                        field.broadcast(FieldPacket.smartMobNotice(mob.getTemplateId(), 1, 5, 0,
                                StringData.getMobStringById(mob.getTemplateId()) + " 感受到這些敵人潛力的威脅，趁潛在能力被無效化時召喚了手下的怪物。"));
                    }
                }
                if (afterDead && mob.getHp() > 0) {
                    if (mob.getTemplateId() >= 8920000 && mob.getTemplateId() <= 8920003) {
                        for (Mob curMob : field.getMobs()) {
                            if (curMob.getTemplateId() >= 8920000 && curMob.getTemplateId() <= 8920003) {
                                field.removeMob(curMob.getObjectId());
                            }
                        }
                        //Crimson Queen
                        Position curPos = mob.getPosition();
                        for (int mobID : msi.getInts()) {
                            Mob m = field.spawnMob(mobID, curPos.getX(), curPos.getY(), false, mob.getHp());
                            m.setHp(mob.getHp());
                            m.setMaxHp(mob.getMaxHp());
                            m.setFlip(mob.isFlip());
                            m.setMobSpawnerId(mob.getObjectId());
                        }
                    }
                    break;
                }
                Position spawnPos = mob.getPosition();
                if (msi.getLt() != null) {
                    rect = new Rect(msi.getLt(), msi.getRb());
                    spawnPos = rect.getRandomPositionInside();
                }
                Set<Mob> spawnedMobs = field.getMobs().stream().filter(m -> m.getMobSpawnerId() == mob.getObjectId()).collect(Collectors.toSet());
                for (int mobId : msi.getInts()) {
                    long spawnedSize = spawnedMobs.stream().filter(m -> m.getTemplateId() == mobId).count();
                    int maxSpawned = Math.min(msi.getSkillStatIntValue(limit), 15);
                    if (spawnedSize < maxSpawned) {
                        try {
                            Mob m = field.spawnMobForTime(mobId, spawnPos.getX(), spawnPos.getY(), 20 * 1000);
                            m.setMobSpawnerId(mob.getObjectId());
                        } catch (NullPointerException e) {
                            for (Char chr : field.getChars()) {
                                if (chr != null) {
                                    spawnPos = chr.getPosition();
                                    break;
                                }
                            }
                            Mob m = field.spawnMobForTime(mobId, spawnPos.getX(), spawnPos.getY(), 20 * 1000);
                            m.setMobSpawnerId(mob.getObjectId());
                        }
                    }
                }
                break;
            case StunmadeBody:
                o.nOption = 1;
                mts.addMobSkillOptions(mob, MobStat.DodgeBodyAttack, o);
                break;
            case ResetMobstat:
                mts.removeBuffs(mob);
                break;
            case CastingBar:
                field.broadcast(MobPool.castingBarSkillStart(mob.getObjectId(), slv, msi.getSkillStatIntValue(castingTime), false, false));
                break;
            case BounceAttack:
                switch (slv) {
                    case 6:
                    case 13:
                    case 14:
                    case 16: {
                        ArrayList<Integer> list = new ArrayList<>();
                        for (int i = 0; i < 7; ++i) {
                            if (!Util.succeedProp(50)) {
                                list.add(-650 + 180 * i);
                            }
                        }
                        int rand = Util.getRandom(0, 7);
                        list.add(-650 + 180 * rand);
                        int i2 = 0;
                        ArrayList<EnergySphere> energySpheres = new ArrayList<>();
                        for (Integer x : list) {
                            EnergySphere energySphere = new EnergySphere(true, 1, -500, 100000, 15, 10000, 0, list, i2);
                            energySphere.setCustomx(x);
                            energySphere.setObjectID(field.getNewObjectID());
                            ++i2;
                        }
                        if (energySpheres.size() > 0) {
                            mob.getTimer().addEvent(() -> {
                                field.broadcast(MobPool.createEnergySpheres(mob.getObjectId(), slv, energySpheres));
                            }, 1700);
                        }
                        mob.getTimer().addEvent(() -> {
                            if (!mts.hasCurrentMobStat(MobStat.Freeze)) {
                                AffectedArea aa = AffectedArea.getMobAA(mob, 131, Util.getRandom(24, 27), msi);
                                field.spawnAffectedArea(aa);
                            }
                        }, Util.getRandom(4000, 10000));
                        break;
                    }
                    default: {
                        for (int i = 0; i < slv; i++) {
                            field.broadcast(MobPool.createBounceAttackSkill(mob, msi));
                        }
                    }
                }
                break;
            case AreaInstallBuff:
            case AreaInstalledFire: {
                Char chr = chars.size() == 0 ? null : Util.getRandomFromCollection(chars);
                if (chr != null) {
                    AffectedArea aa = AffectedArea.getMobAA(mob, skill, slv, msi);
                    field.spawnAffectedArea(aa);
                    field.broadcast(FieldPacket.affectedAreaInstallAreaFire(aa));
                }
                break;
            }
            case LaserAttack:
                if (!mts.hasCurrentMobStat(MobStat.Laser)) {
                    o.nOption = 1;
                    o.wOption = msi.getSkillStatIntValue(w);
                    o.uOption = msi.getSkillStatIntValue(z);
                    mts.addMobSkillOptions(mob, MobStat.Laser, o);
                    field.broadcast(MobPool.laserControl(mob, 120, 1, Util.succeedProp(50)));
                }
                break;
            case LaserControl:
                // Not needed? Automatically handled well by the controller
                break;
            case FireBomb:
                for (Char chr : field.getChars()) {
                    TemporaryStatManager tsm = chr.getTemporaryStatManager();
                    o.nOption = tsm.getOption(FireBomb) != null ? tsm.getOption(FireBomb).nOption + 1 : 1;
                    o.rOption = MobSkillID.FireBomb.getVal();
                    o.slv = 2;
                    o.tOption = 10; //3 sec
                    tsm.sendSetStatFromMobSkillPacket(FireBomb, o);
                    chr.write(UserPacket.effect(Effect.mobSkillHit(o.rOption, o.slv)));
                    chr.getField().broadcast(UserRemote.effect(chr.getId(), Effect.mobSkillHit(o.rOption, o.slv)), chr);
                }
                break;
            case LtRbDamage:
                field.broadcast(MobPool.ltrbDamageSkill(mob, getSkillID(), getLevel()));
                break;
            case Knockback:
            case Toos:
                for (Char c : chars) {
                    c.write(UserPacket.tossedByMobSkill(c.getId(), mob, msi, -1400));
                }
                break;
            case BreakdownTimezone:
                field.setProperty("enablevonbonportal", true);
                field.broadcast(FieldPacket.setObjectState(Util.getRandomFromCollection(BossConstants.VON_BON_PORTAL_NAMES), 0));
                mob.getTimer().addEvent(() -> {
                    field.setProperty("enablevonbonportal", false);
                    field.broadcast(MobPool.breakDownTimeZoneTimeOut(mob));
                }, 5 * 1000);
                break;
            case ObstacleAttack: {
                if (slv == 2 || slv == 4) {
                    int effect = 4;
                    int startY = mob.getY();
                    int minRad = 890;
                    int maxRad = msi.getSkillStatIntValue(y2) + startY;
                    int startX = 790;
                    int height = 0;
                    ObtacleRadianInfo obtacleRadianInfo = new ObtacleRadianInfo(effect, minRad, maxRad, startX, height);
                    ObtacleAtomEnum oae = slv == 4 ? ObtacleAtomEnum.RedOrb : ObtacleAtomEnum.DemianYellowOrb;
                    final int key = 1;
                    mob.getTimer().addFixedRateEvent(() -> {
                        Set<ObtacleAtomInfo> obtacleAtomInfosSet = new HashSet<>();
                        for (int i = 0; i < 4; i++) {
                            int rand = Util.getRandom(300, 500);
                            int xMin = mob.getPosition().getX() - rand;
                            int xMax = mob.getPosition().getX() + rand;
                            int x = mob.getPosition().getX();
                            int trueDamR = 90; // 90% damage
                            int mobDamR = 0;
                            int createDelay = 790;
                            int vPerSec = 71;
                            int maxP = 2;
                            int length = 514;
                            obtacleAtomInfosSet.add(new ObtacleAtomInfo(oae.getType(), key, new Position(x, maxRad), new Position(xMax, startY + 50), oae.getHitBox(), trueDamR, mobDamR, createDelay, height, vPerSec, maxP, length, 323));
                            obtacleAtomInfosSet.add(new ObtacleAtomInfo(oae.getType(), key, new Position(x, maxRad), new Position(x, startY + 50), oae.getHitBox(), trueDamR, mobDamR, createDelay, height, vPerSec, maxP, length, 0));
                            obtacleAtomInfosSet.add(new ObtacleAtomInfo(oae.getType(), key, new Position(x, maxRad), new Position(x + 100, startY + 50), oae.getHitBox(), trueDamR, mobDamR, createDelay, height, vPerSec, maxP, length, 0));
                            obtacleAtomInfosSet.add(new ObtacleAtomInfo(oae.getType(), key, new Position(x, maxRad), new Position(xMin, startY + 50), oae.getHitBox(), trueDamR, mobDamR, createDelay, height, vPerSec, maxP, length, 36));
                        }
                        field.broadcast(FieldPacket.createObtacle(ObtacleAtomCreateType.RADIAL, null, obtacleRadianInfo, obtacleAtomInfosSet));
                    }, 3000, 2000, 3);
                }
                break;
            }
            case FireAtRandomAttack:
                List<Rect> rects = new ArrayList<>();
                rects.add(rect);
                rects.add(rect.moveRight());
                rects.add(rect.moveRight().moveRight());
                rects.add(rect.moveRight().moveRight().moveRight());
                rects.add(rect.moveRight().moveRight().moveRight().moveRight());
                rects.add(rect.moveRight().moveRight().moveRight().moveRight().moveRight());
                rects.add(rect.moveLeft());
                rects.add(rect.moveLeft().moveLeft());
                rects.add(rect.moveLeft().moveLeft().moveLeft());
                rects.add(rect.moveLeft().moveLeft().moveLeft().moveLeft());
                rects.add(rect.moveLeft().moveLeft().moveLeft().moveLeft().moveLeft());
                Rect rect_ = Util.getRandomFromCollection(rects);
                mob.getSkillDelays().add(this);
                mob.setSkillDelay(getSkillDelay());
                field.broadcast(MobPool.setSkillDelay(mob, getSkillDelay(), msi, 4, rect_));
                mob.getTimer().addEvent(() -> {
                            for (Char player : field.getChars()) {
                                if (player != null) {
                                    if (rect_.hasPositionInside(player.getPosition())) {
                                        if (!player.getTemporaryStatManager().hasStat(HolyMagicShell)) {
                                            player.damage((int) (player.getMaxHP() * 0.5D));
                                        }
                                    }
                                }
                            }
                        }
                        , 2000);
                break;
            case FixDamrBuff:
                if (!mts.hasCurrentMobStat(MobStat.FixDamRBuff)) {
                    o.nOption = msi.getSkillStatIntValue(x);
                    mts.addMobSkillOptions(mob, MobStat.FixDamRBuff, o);
                }
                break;
            case Hangover:
                o.nOption = 1;
                mts.addMobSkillOptions(mob, MobStat.HangOver, o);
                break;
            case Lucid:
                if (net.swordie.ms.world.boss.Lucid.isSecondPhaseField(field.getId())) { // dreamtrial-1005: + trial copy
                    field.broadcast(MobPool.forceChase(mob.getObjectId(), true));
                }
                switch (slv) {
                    case 1, 2, 3 ->
                            field.broadcast(LucidPacket.doSkill(slv, Randomizer.nextInt(3), new Position(1000, 48), null, 0, null, 0));
                    case 4, 10 -> {
                        List<FairyDust> fairyDust = new ArrayList<>() {
                            {
                                int x, s, s2, v, v2, w, w2, u = 2640;
                                if (slv == 4) {
                                    x = 40;
                                    s = 180;
                                    s2 = 240;
                                    v = 100;
                                    v2 = 5;
                                    w = 3;
                                    w2 = 1;
                                } else {
                                    x = 5;
                                    s = 30;
                                    s2 = 330;
                                    v = 250;
                                    v2 = 100;
                                    w = 6;
                                    w2 = 3;
                                }
                                for (int i = 0, max = Randomizer.rand(w, w + w2); i < max; i++) {
                                    x += Randomizer.nextInt(x);
                                    add(new FairyDust(Randomizer.nextInt(3), u, v + Randomizer.nextInt(v2), x + Randomizer.rand(s, s2)));
                                }
                            }
                        };
                        field.broadcast(LucidPacket.doSkill(slv, 0, null, fairyDust, 0, null, 0));
                        field.broadcast(WvsContext.weatherEffectNotice(WeatherEffNoticeType.BossLucid, "露希妲即將釋放力量！", 3000));
                    }
                    case 5 -> {
                        List<Integer> laserIntervals = new ArrayList<>() {
                            {
                                for (int i = 0; i < 15; i++) {
                                    add(500);
                                }
                            }
                        };
                        field.broadcast(LucidPacket.doSkill(slv, 0, null, null, 4500, laserIntervals, 0));
                        field.broadcast(WvsContext.weatherEffectNotice(WeatherEffNoticeType.BossLucid, "露希妲即將使用強力攻擊！", 3000));
                    }
                    case 6, 11 ->
                            field.broadcast(LucidPacket.doSkill(slv, 0, null, null, 0, null, Randomizer.nextInt(8)));
                    case 7 -> {
                        boolean isLeft = Util.succeedProp(70);
                        if (net.swordie.ms.world.boss.Lucid.isFirstPhaseField(field.getId())) { // dreamtrial-1005: + trial copy
                            field.broadcast(LucidPacket.createDragon(1, 0, 0, 0, 0, isLeft));
                        } else {
                            int createPosX = isLeft ? -138 : 1498;
                            int createPosY = Randomizer.nextBoolean() ? -1312 : 238;
                            int posX = createPosX;
                            int posY = mob.getPosition().getY();
                            field.broadcast(LucidPacket.createDragon(2, posX, posY, createPosX, createPosY, isLeft));
                        }
                        field.broadcast(WvsContext.weatherEffectNotice(WeatherEffNoticeType.BossLucid, "強大的召喚獸正在接近！", 3000));
                    }
                    case 8 -> {
                        field.broadcast(LucidPacket.doSkill(slv));
                        field.broadcast(WvsContext.weatherEffectNotice(WeatherEffNoticeType.BossLucid, "攻擊那股風會讓夢境變得更強！", 3000));
                    }
                    case 9 -> {
                        field.setLucidState(LucidState.DoSkill);
                        field.broadcast(LucidPacket.welcomeBarrage(2));
                        field.broadcast(LucidPacket.stainedGlassOnOff(false, BossConstants.STAINED_GLASS));
                        field.broadcast(LucidPacket.setFlyingMode(true));
                        field.broadcast(LucidPacket.butterflyAction(ButterFlyType.Move, 0, new Position(600, -500), 0, 0));

                        field.broadcast(LucidPacket.welcomeBarrage(0, 180, 30));
                        field.broadcast(LucidPacket.welcomeBarrage(3, 50, 120, 500, 3));
                        field.broadcast(LucidPacket.welcomeBarrage(4, 180, 30, 0, 700, 12, 30, 5, 1));
                        field.broadcast(LucidPacket.welcomeBarrage(5, 180, 30, 0, 700, 12, 30, 5, 1));

                        field.broadcast(LucidPacket.welcomeBarrage(0, 180, 30));
                        field.broadcast(LucidPacket.welcomeBarrage(3, 50, 100, 500, 4));
                        field.broadcast(LucidPacket.welcomeBarrage(4, 180, 70, 0, 1000, 12, 30, 10, 0));
                        field.broadcast(LucidPacket.welcomeBarrage(5, 180, 70, 0, 1000, 12, 30, 10, 0));

                        field.broadcast(LucidPacket.welcomeBarrage(0, 180, 30));
                        field.broadcast(LucidPacket.welcomeBarrage(3, 50, 90, 700, 8));
                        field.broadcast(LucidPacket.welcomeBarrage(4, 180, 100, 0, 700, 12, 30, 0, 0));
                        field.broadcast(LucidPacket.welcomeBarrage(5, 180, 100, 0, 700, 12, 30, 0, 0));
                        mob.getTimer().addEvent(() -> field.setLucidState(LucidState.None), 12000);
                        field.broadcast(WvsContext.weatherEffectNotice(WeatherEffNoticeType.BossLucid, "露希妲正在呼喚真正的力量！", 3000));
                    }
                }
                break;
            case Will:
                switch (slv) {
                    case 1:
                    case 2:
                    case 3:
                        int id;
                        if (mob.getTemplateId() == 8880321 || mob.getTemplateId() == 8880322) {
                            id = mob.getTemplateId() - 18;
                        } else if (mob.getTemplateId() == 8880323 || mob.getTemplateId() == 8880324) {
                            id = mob.getTemplateId() - 22;
                        } else if (mob.getTemplateId() == 8880353 || mob.getTemplateId() == 8880354) {
                            id = mob.getTemplateId() - 12;
                        } else {
                            id = mob.getTemplateId() - 8;
                        }
                        int size = Randomizer.rand(17, 23);

                        ArrayList<Triple<Integer, Integer, Integer>> idx = new ArrayList<>() {
                            {
                                for (int i = 1; i <= size; i++) {
                                    add(new Triple<>(i, 1800 * (1 + (i / 6)), -650 + (130 * Randomizer.rand(1, 9))));
                                }
                            }
                        };
                        field.broadcast(WillPacket.spiderAttack(id, slv, 0, idx));
                        break;
                    case 4:
                        field.broadcast(WvsContext.weatherEffectNotice(WeatherEffNoticeType.BossWill,
                                "攻擊眼球以改變月光方向，並利用它生成月光屏障！", 30000));
                        field.broadcast(WillPacket.useSpecial());
                        ArrayList<Integer> arrayList = new ArrayList<>() {
                            {
                                for (int i = 0; i < 9; ++i) {
                                    this.add(i);
                                }
                            }
                        };
                        for (Mob m : field.getMobs()) {
                            m.getField().broadcast(MobPool.mobAttackBlock(mob, arrayList));
                            //mob.setSkillForbid(true);
                            mob.setUseSpecialSkill(true);
                            if (mob.getTemplateId() != 8880305) {
                                continue;
                            }
                            m.getField().removeMob(mob.getObjectId());
                        }
                        int bluecount = 0;
                        int purplecount = 0;
                        int reverge = 1;
                        Char chr = field.getCharByID(mob.getControllerID());
                        if (chr != null && chr.getParty() != null) {
                            reverge = chr.getParty().getMembers().size() / 2;
                        }
                        for (Char player : field.getChars()) {
                            int typee = Randomizer.rand(1, 2);
                            if (typee == 1) {
                                if (++bluecount > reverge) {
                                    typee = 2;
                                    --bluecount;
                                }
                            } else if (++purplecount > reverge) {
                                typee = 1;
                                --purplecount;
                            }
                            player.write(WillPacket.spiderAttack(mob.getTemplateId(), slv, typee, null));
                        }
                        for (AffectedArea aa : field.getAffectedAreas()) {
                            if (aa.getSkillID() != msID.getVal()) {
                                continue;
                            }
                            field.removeLife(aa.getObjectId(), false);
                        }
                        field.spawnMob(8880305, 0, -2020, false);
                        field.spawnMob(8880305, 0, 159, false);
                        mob.getTimer().addEvent(() -> {
                            field.removeMobsByTemplateID(8880305);
                            ArrayList<AffectedArea> affectedAreas = new ArrayList<>();
                            ArrayList<Char> targets = new ArrayList<>();
                            for (AffectedArea aa : field.getAffectedAreas()) {
                                if (aa.getMobOrigin() == (byte) 1 && aa.getSkillID() == msID.getVal() && aa.getSlv() == slv) {
                                    affectedAreas.add(aa);
                                    continue;
                                }
                                if (aa.getSkillID() != 400031039 && aa.getSkillID() != 400031040) {
                                    continue;
                                }
                                affectedAreas.add(aa);
                            }
                            for (Char target : field.getChars()) {
                                target.write(WillPacket.spiderAttack(mob.getTemplateId(), slv, 0, null));
                                for (AffectedArea aa : affectedAreas) {
                                    boolean isInsideAA = aa.getRect().hasPositionInside(target.getPosition());
                                    if (isInsideAA) {
                                        targets.add(target);
                                    }
                                }
                            }
                            for (Char target : targets) {
                                TemporaryStatManager tsm = target.getTemporaryStatManager();
                                if (target.hasSkill(Bishop.HOLY_MAGIC_SHELL) && tsm.hasStat(CharacterTemporaryStat.HolyMagicShell)) {
                                    Bishop bishop = (Bishop) target.getJobHandler();
                                    if (bishop.hmshits < Bishop.getHolyMagicShellMaxGuards(target)) {
                                        bishop.hmshits++;
                                        continue;
                                    } else {
                                        bishop.hmshits = 0;
                                        tsm.removeStatsBySkill(Bishop.HOLY_MAGIC_SHELL);
                                        continue;
                                    }
                                }
                                if (tsm.hasStat(Asura)) {
                                    tsm.removeStatsBySkill(tsm.getOption(Asura).rOption);
                                    continue;
                                }
                                if (target.hasSkill(WindArcher.GALE_BARRIER) && tsm.hasStat(WindBreakerStormGuard)
                                        && target.getJobHandler() instanceof WindArcher windArcher) {
                                    windArcher.diminishGaleBarrier(10);
                                    continue;
                                }
                                if (tsm.hasStat(CursorSniping) || tsm.hasStat(EtherealForm) || tsm.hasStat(IndieNotDamaged) || tsm.hasStat(NotDamaged)) {
                                    continue;
                                }
                                target.die();
                            }
                            if (mob.isUseSpecialSkill()) {
                                ArrayList<Integer> newHpList = new ArrayList<>();
                                if (mob.getHPPercent() <= 67 && mob.getWillHPlist().contains(666)) {
                                    newHpList.add(333);
                                    newHpList.add(3);
                                } else if (mob.getHPPercent() <= 34 && mob.getWillHPlist().contains(333)) {
                                    newHpList.add(3);
                                } else if (mob.getHPPercent() > 1 || !mob.getWillHPlist().contains(3)) {
                                    newHpList.add(666);
                                    newHpList.add(333);
                                    newHpList.add(3);
                                }
                                mob.setWillHPlist(newHpList);
                                field.broadcast(WillPacket.setHp(mob.getWillHPlist(), field,
                                        mob.getTemplateId(),
                                        mob.getTemplateId() + 3,
                                        mob.getTemplateId() + 4)
                                );

                                MobSkill mobSkill = new MobSkill();
                                mobSkill.setSkillID(MobSkillID.Will.getVal());
                                mobSkill.setLevel(8);
                                mobSkill.setFlip(isFlip());
                                mob.getSkillDelays().add(mobSkill);
                                mob.setSkillDelay(mobSkill.getSkillDelay());
                                mob.getField().broadcast(MobPool.setSkillDelay(mob, mobSkill.getSkillDelay(), MobSkillID.Will.getVal(), 8));
                                mobSkill.applyEffect(mob);
                            } else {
                                for (Mob m : field.getMobs()) {
                                    m.getField().broadcast(MobPool.mobAttackBlock(m, new ArrayList<>()));
                                    m.setUseSpecialSkill(false);
                                }
                                mob.getField().broadcast(MobPacketEncoder.active().controlAck(mob, true, (short) 0, 0, 0, 0));
                            }
                            mob.getTimer().addEvent(() -> {
                                MobSkill mobSkill = new MobSkill();
                                mobSkill.setSkillID(MobSkillID.Will.getVal());
                                mobSkill.setLevel(15);
                                mobSkill.setFlip(isFlip());
                                mob.getSkillDelays().add(mobSkill);
                                mob.setSkillDelay(mobSkill.getSkillDelay());
                                mob.getField().broadcast(MobPool.setSkillDelay(mob, mobSkill.getSkillDelay(), MobSkillID.Will.getVal(), 15));
                                mobSkill.applyEffect(mob);
                            }, 11000L);
                        }, 30000L);
                        break;
                    case 5:
                        int type = Randomizer.nextInt(2);
                        field.broadcast(FieldPacket.fieldEffect(FieldEffect.mobHPTagFieldEffect(mob)));
                        field.broadcast(WillPacket.spiderAttack(mob.getTemplateId(), slv, type, null));
                        mob.getTimer().addEvent(() -> {
                            if (mob.getTemplateId() == 8880300 || mob.getTemplateId() == 8880340) {
                                Mob will1 = field.getMobByTemplateId(mob.getTemplateId() + 3);
                                Mob will2 = field.getMobByTemplateId(mob.getTemplateId() + 4);
                                long hp1 = 0L;
                                long hp2 = 0L;
                                if (will1 != null) {
                                    hp1 = will1.getHp();
                                }
                                if (will2 != null) {
                                    hp2 = will2.getHp();
                                }
                                long newhp = Math.max(hp1, hp2);
                                mob.setHp(newhp);
                                if (will1 != null) {
                                    will1.setHp(newhp);
                                }
                                if (will2 != null) {
                                    will2.setHp(newhp);
                                }
                                field.broadcast(WillPacket.setHp(mob.getWillHPlist(), field,
                                        mob.getTemplateId(),
                                        mob.getTemplateId() + 3,
                                        mob.getTemplateId() + 4)
                                );
                            }
                            ArrayList<AffectedArea> affectedAreas = new ArrayList<>();
                            ArrayList<Char> targets = new ArrayList<>();
                            for (AffectedArea aa : field.getAffectedAreas()) {
                                if (aa.getMobOrigin() == (byte) 1 && aa.getSkillID() == msID.getVal() && aa.getSlv() == slv) {
                                    affectedAreas.add(aa);
                                    continue;
                                }
                                if (aa.getSkillID() != 400031039 && aa.getSkillID() != 400031040) {
                                    continue;
                                }
                                affectedAreas.add(aa);
                            }
                            for (Char target : field.getChars()) {
                                for (AffectedArea aa : affectedAreas) {
                                    boolean isInsideAA = aa.getRect().hasPositionInside(target.getPosition());
                                    if (isInsideAA) {
                                        targets.add(target);
                                    }
                                }
                            }
                            for (Char target : targets) {
                                TemporaryStatManager tsm = target.getTemporaryStatManager();
                                if (target.hasSkill(Bishop.HOLY_MAGIC_SHELL) && tsm.hasStat(CharacterTemporaryStat.HolyMagicShell)) {
                                    Bishop bishop = (Bishop) target.getJobHandler();
                                    if (bishop.hmshits < Bishop.getHolyMagicShellMaxGuards(target)) {
                                        bishop.hmshits++;
                                        continue;
                                    } else {
                                        bishop.hmshits = 0;
                                        tsm.removeStatsBySkill(Bishop.HOLY_MAGIC_SHELL);
                                        continue;
                                    }
                                }
                                if (tsm.hasStat(Asura)) {
                                    tsm.removeStatsBySkill(tsm.getOption(Asura).rOption);
                                    continue;
                                }
                                if (target.hasSkill(WindArcher.GALE_BARRIER) && tsm.hasStat(WindBreakerStormGuard)
                                        && target.getJobHandler() instanceof WindArcher windArcher) {
                                    windArcher.diminishGaleBarrier(10);
                                    continue;
                                }
                                if (tsm.hasStat(CursorSniping) || tsm.hasStat(EtherealForm) || tsm.hasStat(IndieNotDamaged) || tsm.hasStat(NotDamaged)) {
                                    continue;
                                }
                                int reduce = 0;
                                if (tsm.hasStat(IndieDamReduceR)) {
                                    reduce -= (int) tsm.getTotalNOptionOfStat(IndieDamReduceR);
                                }
                                if (type == 0 && target.getPosition().getY() > -455 && target.getPosition().getY() < 300) {
                                    target.heal((int) (-target.getMaxHP() * (100 - reduce) / 100L));
                                    continue;
                                }
                                if (type != 1 || target.getPosition().getY() <= -2500 || target.getPosition().getY() >= -1800)
                                    continue;
                                target.heal((int) (-target.getMaxHP() * (100 - reduce) / 100L));
                            }
                            mob.getTimer().addEvent(() -> {
                                MobSkill mobSkill = new MobSkill();
                                mobSkill.setSkillID(MobSkillID.Will.getVal());
                                mobSkill.setLevel(4);
                                mobSkill.setFlip(isFlip());
                                mob.getSkillDelays().add(mobSkill);
                                mob.setSkillDelay(mobSkill.getSkillDelay());
                                mob.getField().broadcast(MobPool.setSkillDelay(mob, mobSkill.getSkillDelay(), MobSkillID.Will.getVal(), 4));
                                double HPPercent = (double) mob.getHp() * 100.0 / (double) mob.getMaxHp();
                                if (HPPercent <= 66.6 && mob.getWillHPlist().contains(666)) {
                                    mobSkill.applyEffect(mob);
                                } else if (HPPercent <= 33.3 && mob.getWillHPlist().contains(333)) {
                                    mobSkill.applyEffect(mob);
                                } else if (HPPercent <= 0.3 && mob.getWillHPlist().contains(3)) {
                                    mobSkill.applyEffect(mob);
                                }
                            }, 1000L);
                        }, 3000L);
                        break;
                    case 7:
                        field.broadcast(WvsContext.weatherEffectNotice(WeatherEffNoticeType.BossWill,
                                "謊言之鏡會反轉攻擊。裂縫出現時，請正面迎擊！", 26000));
                        field.broadcast(WillPacket.useSpecial());
                        ArrayList<Integer> idss = new ArrayList<>() {
                            {
                                for (int i = 0; i < 9; ++i) {
                                    this.add(i);
                                }
                            }
                        };
                        for (Mob m : field.getMobs()) {
                            m.getField().broadcast(MobPool.mobAttackBlock(m, idss));
                            //m.setSkillForbid(false);
                            m.setUseSpecialSkill(false);
                        }
                        mob.getTimer().addEvent(() -> {
                            MobSkill mobSkill = new MobSkill();
                            mobSkill.setSkillID(242);
                            mobSkill.setLevel(14);
                            mobSkill.setFlip(isFlip());
                            mobSkill.applyEffect(mob);
                        }, 5000);
                        break;
                    case 8:
                        field.broadcast(WvsContext.weatherEffectNotice(WeatherEffNoticeType.BossWill,
                                "機會來了！趁威爾露出破綻時攻擊他！", 12000));
                        field.broadcast(WillPacket.stun());
                        for (AffectedArea aa : field.getAffectedAreas()) {
                            if (aa.getSkillID() == msID.getVal()) {
                                field.removeLife(aa.getObjectId(), false);
                            }
                        }
                        if (mob.getTemplateId() == 8880300 || mob.getTemplateId() == 8880340) {
                            Mob will2;
                            Mob will1 = field.getMobByTemplateId(mob.getTemplateId() + 3);
                            if (will1 != null) {
                                field.broadcast(MobPool.forcedSkillAction(will1.getObjectId(), 3));
                            }
                            if ((will2 = field.getMobByTemplateId(mob.getTemplateId() + 4)) != null) {
                                field.broadcast(MobPool.forcedSkillAction(will2.getObjectId(), 3));
                            }
                        } else {
                            field.broadcast(MobPool.forcedSkillAction(mob.getObjectId(), 2));
                        }
                        mob.getTimer().addEvent(() -> {
                            for (Mob m : field.getMobs()) {
                                m.getField().broadcast(MobPool.mobAttackBlock(mob, new ArrayList<>()));
                                //mob.setSkillForbid(false);
                                mob.setUseSpecialSkill(false);
                            }
                        }, 10000);
                        break;
                    case 9:
                        chr = Util.getRandomFromCollection(field.getChars());
                        if (chr != null) {
                            Option o1 = new Option();
                            int charID = chr.getId();
                            o1.nOption = 1;
                            o1.rOption = MobSkillID.Will.getVal();
                            o1.slv = slv;
                            o1.tOption = 7;
                            chr.getTemporaryStatManager().sendSetStatFromMobSkillPacket(BossWill_Infection, o1);
                            chr.getField().broadcast(WillPacket.poison(chr, mob.getObjectId()));
                            mob.getTimer().addEvent(() -> chr.getField().broadcast(WillPacket.removePoison(charID)), 7000L);
                        }
                        break;
                    case 10:
                    case 11:
                        field.broadcast(WillPacket.spiderAttack(mob.getTemplateId(), slv, 1, null));
                        break;
                    case 12:
                        id = 0;
                        if (mob.getTemplateId() == 8880325 || mob.getTemplateId() == 8880326) {
                            if (field.getId() == 450008150) {
                                if (mob.getPosition().getY() < 0) {
                                    id = 8880304;
                                } else {
                                    id = 8880303;
                                }
                            } else if (mob.getPosition().getY() < 0) {
                                id = 8880344;
                            } else {
                                id = 8880343;
                            }
                        } else if (mob.getTemplateId() == 8880327 || mob.getTemplateId() == 8880328) {
                            id = mob.getTemplateId() - 26;
                            if (field.getId() == 450008250 || field.getId() == 450008850) {
                                id = (field.getId() == 450008250) ? 8880301 : 8880341;
                                if (Randomizer.nextBoolean()) {
                                    field.broadcast(WillPacket.createBulletEyes(0, id, -492, -370));
                                    field.broadcast(WillPacket.createBulletEyes(0, id, -8, -370));
                                    field.broadcast(WillPacket.createBulletEyes(0, id, 501, -370));
                                } else {
                                    field.broadcast(WillPacket.createBulletEyes(0, id, -300, -370));
                                    field.broadcast(WillPacket.createBulletEyes(0, id, 300, -370));
                                }
                            } else if (field.getId() == 450008350 || field.getId() == 450008950) {
                                id = (field.getId() == (net.swordie.ms.world.boss.Will.isTms282()
                                        ? 450008350 : 450008250)) ? 8880302 : 8880342;
                                if (Randomizer.nextBoolean()) {
                                    field.broadcast(WillPacket.createBulletEyes(0, id, -300, -400));
                                } else {
                                    field.broadcast(WillPacket.createBulletEyes(0, id, -415, -400));
                                    field.broadcast(WillPacket.createBulletEyes(0, id, 300, -400));
                                }
                            }
                        } else if (mob.getTemplateId() == 8880355 || mob.getTemplateId() == 8880356) {
                            id = mob.getTemplateId() - 12;
                        }
                        //mob.setLastSkillUsed(this, System.currentTimeMillis(), getInterval());
                        if (id != 8880341 && id != 8880342 && id != 8880302 && id != 8880301) {
                            if (Randomizer.nextBoolean()) {
                                field.broadcast(WillPacket.createBulletEyes(0, id, -250, -370));
                                field.broadcast(WillPacket.createBulletEyes(0, id, 250, -370));
                            } else {
                                field.broadcast(WillPacket.createBulletEyes(0, id, -470, -440));
                                field.broadcast(WillPacket.createBulletEyes(0, id, 470, -440));
                            }
                        }
                        if (mob.getPosition().getY() < 0) {
                            field.broadcast(WillPacket.createBulletEyes(1, id, 300, 100, -690, -2634, 695, -2019));
                            break;
                        }
                        field.broadcast(WillPacket.createBulletEyes(1, id, 300, 100, -690, -455, 695, 500));
                        break;
                    case 13:
                        int webSize = field.getSpiderWebs().size();
                        ArrayList<Integer> a2 = new ArrayList<>();
                        if (webSize >= 67) break;
                        int i5 = 0;
                        boolean respawn = false;
                        for (SpiderWeb web : field.getSpiderWebs()) {
                            a2.add(web.getNumber());
                        }
                        Collections.sort(a2);
                        for (Integer liw : a2) {
                            if (liw != i5) {
                                field.spawnLife(new SpiderWeb(i5), null);
                                respawn = true;
                                break;
                            }
                            ++i5;
                        }
                        if (i5 >= 67 || respawn) break;
                        field.spawnLife(new SpiderWeb(i5), null);
                        break;
                    case 14:
                        if (mob.getTemplateId() != 8880301 && mob.getTemplateId() != 8880341) {
                            break;
                        }
                        mob.setSpecialPattern(true);
                        int a = Randomizer.rand(0, 1);
                        Char x = mob.getMostDamageChar();
                        if (x == null) {
                            break;
                        }
                        doSpider(x, mob, a, false);
                        mob.getTimer().addEvent(() -> doSpider(x, mob, a == 0 ? 1 : 0, true), 11000L);
                        mob.getTimer().addEvent(() -> {
                            if (mob.isSpecialPattern()) {
                                MobSkill mobSkill = new MobSkill();
                                mobSkill.setSkillID(242);
                                mobSkill.setLevel(8);
                                mobSkill.setFlip(isFlip());
                                mobSkill.applyEffect(mob);
                                if (mob.getHPPercent() <= 50 && mob.getWillHPlist().contains(500)) {
                                    mob.setWillHPlist(new ArrayList<Integer>());
                                    mob.getWillHPlist().add(3);
                                } else if (mob.getHPPercent() <= 3 && mob.getWillHPlist().contains(3)) {
                                    mob.setWillHPlist(new ArrayList<Integer>());
                                } else {
                                    mob.setWillHPlist(new ArrayList<Integer>());
                                    mob.getWillHPlist().add(500);
                                    mob.getWillHPlist().add(3);
                                }
                            } else {
                                for (Mob m : field.getMobs()) {
                                    m.getField().broadcast(MobPool.mobAttackBlock(m, new ArrayList<>()));
                                    m.setUseSpecialSkill(false);
                                    //m.setSkillForbid(false);
                                }
                            }
                            field.broadcast(WillPacket.setHp(mob.getWillHPlist()));
                            mob.setSpecialPattern(false);
                        }, 30000L);
                        break;
                    case 15:
                        mob.getTimer().addEvent(() -> {
                            for (Char player : field.getChars()) {
                                field.broadcast(UserLocal.portalTeleport(Util.succeedProp(50) ? "ptup" : "ptdown"));
                                player.write(WillPacket.spiderAttack(mob.getTemplateId(), slv, Randomizer.nextInt(2), null));
                            }
                        }, 3000L);
                        break;
                }
                break;
            case VHillaEvilHand: {
                List<Triple<Position, Integer, List<Rect>>> datas = new ArrayList<>();
                for (int i = 0; i < 7; i++) {
                    rects = new ArrayList<>();
                    int[] randXs = {0, 280, -560, 560, -280, -840, 840};
                    int t = Util.getRandom(randXs.length - 1);
                    int randX = randXs[t];
                    int delay = (t + 1) * 250;
                    int[][][] rectXs = {
                            {{-75, 50}, {13, -50}, {83, 72}, {83, 72}},
                            {{-81, 90}, {-59, -20}, {-25, 13}, {123, 31}, {138, -54}},
                            {{-78, 28}, {-13, -50}, {42, 81}, {75, -18}, {133, 4}},
                            {{-75, 50}, {13, -60}, {83, 72}, {83, 72}},
                            {{-81, 90}, {-59, -20}, {-25, 13}, {123, 31}, {138, -54}},
                            {{-78, 28}, {-13, -50}, {42, 81}, {75, -18}, {133, 4}},
                            {{-78, 28}, {-13, -50}, {42, 81}, {75, -18}, {133, 4}}
                    };
                    int[][] rectX = rectXs[t];
                    for (int[] ints : rectX) rects.add(new Rect(ints[0], -80, ints[1], 640));
                    datas.add(new Triple<>(new Position(randX, -260), delay, rects));
                }
                field.broadcast(MobPool.blackHand(mob, slv, datas));
                break;
            }
            case VHillaSlash: {
                Char player = Util.getRandomFromCollection(field.getChars());
                long glassTime = (mob.getHPPercent() >= 60) ? 150 : ((mob.getHPPercent() >= 30) ? 120 : 100);
                field.setSandGlassTime(glassTime);
                field.broadcast(VerusHillaPacket.encode(4, (mob.getMostDamageChar() == null) ? player : mob.getMostDamageChar(), mob.getField()));
                for (Char chr : field.getChars()) {
                    chr.write(VerusHillaPacket.encode(3, chr, field));
                    chr.getField().broadcast(VerusHillaPacket.encode(10, chr, field));
                }
                int sandCount = 0;
                for (Char chr : field.getChars()) {
                    sandCount += chr.getDeathCount();
                }
                field.setCandles((int) Math.round(sandCount * 0.5D));
                field.broadcast(VerusHillaPacket.encode(0, (mob.getMostDamageChar() == null) ? player : mob.getMostDamageChar(), field));
                field.setLightCandles(0);
                field.broadcast(VerusHillaPacket.encode(1, (mob.getMostDamageChar() == null) ? player : mob.getMostDamageChar(), field));
                field.broadcast(VerusHillaPacket.encode(8, (mob.getMostDamageChar() == null) ? player : mob.getMostDamageChar(), field));
                field.broadcast(UserLocal.screenAttack(mob.getObjectId(), getSkillID(), getLevel(), 100000));
                break;
            }
            case VHillaSinisterEnergy: {
                AffectedArea aa = new AffectedArea(0);
                aa.setMobOrigin((byte) 1);
                aa.setMob(mob);
                aa.setSkillID(getSkillID());
                aa.setSlv((byte) slv);
                aa.setDuration(3000);
                aa.setRect(new Rect(Util.getRandom(-400, 300), Util.getRandom(-400, 100), 400, 400));
                field.spawnAffectedArea(aa);
                break;
            }
            default:
                System.out.printf("[MobSkill::applyEffectToMob] Unhandled mob skillID %s, slv = %d%n", msID, getLevel());
                break;
        }

    }

    public void applyEffect(Char chr) {
        short skill = (short) getSkillID();
        if (skill == 0) {
            skill = (short) getDisease();
        }
        short level = (short) getLevel();
        MobSkillInfo msi = SkillData.getMobSkillInfoByIdAndLevel(skill, level);
        MobSkillID msID = MobSkillID.getMobSkillIDByVal(skill);
        Option o = new Option(skill);
        o.rOption = getSkillID();
        o.slv = level;
        o.tOption = Util.getRandom(3, 10);//msi.getSkillStatIntValue(time);
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        switch (msID) {
            case Seal, Darkness, Weakness, Stun, Curse, Slow, Fear, Frozen, DispelItemOption -> {
                o.nOption = 1;
                tsm.sendSetStatFromMobSkillPacket(msID.getAffectedCTS(), o);
            }
            case StopPortion, StopMotion, Slowattack, Attract, ReverseInput, Poison, Magnet -> {
                o.nOption = msi.getSkillStatIntValue(x);
                tsm.sendSetStatFromMobSkillPacket(msID.getAffectedCTS(), o);
            }
            case Lapidification, Deathmark, PainMark -> {
                o.nOption = msi.getSkillStatIntValue(x);
                tsm.sendSetStatFromMobSkillPacket(msID.getAffectedCTS(), o);
                chr.write(UserPacket.effect(Effect.mobSkillHit(getSkillID(), getLevel())));
                chr.getField().broadcast(UserRemote.effect(chr.getId(), Effect.mobSkillHit(getSkillID(), getLevel())), chr);
            }
            case Undead -> { //Todo: Fix this
                o.nOption = 1;
                TemporaryStatBase tsb = tsm.getTSBByTSIndex(TSIndex.Undead);
                tsb.setNOption(o.nOption);
                tsb.setROption(skill << level | 16);
                tsb.setExpireTerm(o.tOption);
                tsm.sendSetStatFromMobSkillPacket(CharacterTemporaryStat.Undead, o);
            }
            case Dispel -> tsm.removeAllDebuffs();
            case UserBomb -> {
                o.nOption = msi.getSkillStatIntValue(x);
                tsm.sendSetStatFromMobSkillPacket(CharacterTemporaryStat.TimeBomb, o);
                chr.write(UserPacket.effect(Effect.mobSkillHit(getSkillID(), getLevel())));
                chr.getField().broadcast(UserRemote.effect(chr.getId(), Effect.mobSkillHit(getSkillID(), getLevel())), chr);
            }
            case UserMorph -> {
                o.nOption = msi.getSkillStatIntValue(x);
                tsm.sendSetStatFromMobSkillPacket(CharacterTemporaryStat.Morph, o);
            }
            case DarkTornado -> {
                o.nOption = msi.getSkillStatIntValue(x);
                tsm.sendSetStatFromMobSkillPacket(CharacterTemporaryStat.DarkTornado, o);
            }
            case ReturnTeleport -> {
                if (!tsm.hasStat(ReturnTeleport)) {
                    o.nOption = 1;
                    o.tOption = 3;
                    tsm.sendSetStatFromMobSkillPacket(ReturnTeleport, o);
                }
            }
            case DebuffHalf -> {
                o.nOption = 1;
                tsm.sendSetStatFromMobSkillPacket(CharacterTemporaryStat.HalfstatByDebuff, o);
            }
            case Contagion -> {
                o.nOption = msi.getSkillStatIntValue(x);
                tsm.sendSetStatFromMobSkillPacket(CharacterTemporaryStat.Contagion, o);
            }
            default ->
                    System.out.printf("[MobSkill::applyEffect(Char)] Unhandled mob skillID %s, slv = %d%n", msID, getLevel());
        }
    }

    public boolean isFlip() {
        return flip;
    }

    public void setFlip(boolean flip) {
        this.flip = flip;
    }

    public static MobSkill handleMobSkillLogic(Mob mob, List<MobSkill> mobSkillList) {
        MobSkill mobSkill = null;
        if (mob.getTemplateId() >= 8800020 && mob.getTemplateId() <= 8800022 || mob.getTemplateId() >= 8800000 && mob.getTemplateId() <= 8800002 || mob.getTemplateId() >= 8800100 && mob.getTemplateId() <= 8800102) {
            for (Mob mobInField : mob.getField().getMobs()) {
                if (BossConstants.isZakumArm(mobInField.getTemplateId())) {
                    return null;
                }
            }
        }
        mobSkill = mobSkillList.get(Randomizer.nextInt(mobSkillList.size()));
        return mobSkill;
    }

    public void zakumApplyEffect(Mob mob) {
        MobTemporaryStat mts = mob.getTemporaryStat();
        short skill = (short) getSkillID();
        short slv = (short) getLevel();
        MobSkillInfo msi = SkillData.getMobSkillInfoByIdAndLevel(skill, slv);
        MobSkillID msID = MobSkillID.getMobSkillIDByVal(skill);
        Field field = mob.getField();
        Option o = new Option(skill);
        o.slv = slv;
        o.tOption = Util.getRandom(3, 10);//msi.getSkillStatIntValue(time);
        Option o2 = new Option(skill);
        o2.slv = slv;
        o2.tOption = Util.getRandom(3, 10);//msi.getSkillStatIntValue(time);
        Rect rect = null;
        Set<Mob> mobs = new HashSet<>();
        if (msi.getLt() != null && msID != MobSkillID.Damage && slv != 27) {
            rect = new Rect(msi.getLt(), msi.getRb());
            if (mob.isFlip()) {
                rect.horizontalFlipAround(mob.getPosition().getX());
            }
            mobs.addAll(field.getMobsInRect(rect));
        } else if (msi.getLt2() != null && msID != MobSkillID.Damage && slv != 27) {
            rect = new Rect(msi.getLt2(), msi.getRb2());
            if (mob.isFlip()) {
                rect.horizontalFlipAround(mob.getPosition().getX());
            }
            mobs.addAll(field.getMobsInRect(rect));
        }
        //chr.chatMessage(String.format("[Mob Skill] Controller: %s | Mob ID: %d | %s (%d) | level = %d", chr.getName(), mob.getTemplateId(), msID, getSkillID(), getLevel()));
        switch (msID) {
            case Damage:
                switch (slv) {
                    case 27 -> //Easy, Normal Handclap
                    {
                        List<Rect> rectList = new ArrayList<>();
                        int hpDamage = 0;
                        switch (mob.getTemplateId()) {
                            case 8800023, 8800027, 8800003, 8800007, 8800103, 8800107 -> {
                                rectList.add(new Rect(new Position(-514, -198), new Position(-260, -198)));
                                rectList.add(new Rect(new Position(300, -201), new Position(554, -201)));
                            }
                            case 8800024, 8800028, 8800004, 8800008, 8800104, 8800108 -> {
                                rectList.add(new Rect(new Position(-549, -113), new Position(-312, -113)));
                                rectList.add(new Rect(new Position(334, -113), new Position(589, -113)));
                            }
                            case 8800025, 8800029, 8800005, 8800009, 8800105, 8800109 -> {
                                rectList.add(new Rect(new Position(-549, -19), new Position(-308, -19)));
                                rectList.add(new Rect(new Position(313, -19), new Position(567, -19)));
                            }
                        }
                        mob.getTimer().addEvent(() -> {
                            for (Rect hitRect : rectList) {
                                if (hitRect != null) {
                                    for (Char member : field.getChars()) {
                                        if (member.getHP() <= 0) {
                                            continue;
                                        }
                                        if (hitRect.hasPositionInside(member.getPosition())) {
                                            member.damage((int) (member.getHP() * 0.7));
                                        }
                                    }
                                }
                            }
                            rectList.clear();
                        }, 1300, TimeUnit.MILLISECONDS);
                    }
                    case 25, 26, 33, 34, 35, 36 -> //Arm Smash Down
                    {
                        mob.getTimer().addEvent(() -> {
                            Rect hitRect = null;
                            int hpDamage = 0;
                            switch (mob.getTemplateId()) {
                                case 8800023, 8800003, 8800103 ->
                                        hitRect = new Rect(new Position(mob.getPosition().getX() - 125, mob.getPosition().getY()), new Position(mob.getPosition().getX() - 5, mob.getPosition().getY()));
                                case 8800024, 8800004, 8800104 ->
                                        hitRect = new Rect(new Position(mob.getPosition().getX() - 125 - 5 - 120, mob.getPosition().getY()), new Position(mob.getPosition().getX() - 5 - 5 - 120, mob.getPosition().getY()));
                                case 8800025, 8800005, 8800105 ->
                                        hitRect = new Rect(new Position(mob.getPosition().getX() - 125 - 129 - 120, mob.getPosition().getY()), new Position(mob.getPosition().getX() - 5 - 129 - 120, mob.getPosition().getY()));
                                case 8800026, 8800006, 8800106 ->
                                        hitRect = new Rect(new Position(mob.getPosition().getX() - 125 - 254 - 120, mob.getPosition().getY()), new Position(mob.getPosition().getX() - 5 - 254 - 120, mob.getPosition().getY()));
                                case 8800027, 8800007, 8800107 ->
                                        hitRect = new Rect(mob.getPosition(), new Position(mob.getPosition().getX() + 120, mob.getPosition().getY()));
                                case 8800028, 8800008, 8800108 ->
                                        hitRect = new Rect(new Position(mob.getPosition().getX() + 23 + 120, mob.getPosition().getY()), new Position(mob.getPosition().getX() + 23 + 120 + 120, mob.getPosition().getY()));
                                case 8800029, 8800009, 8800109 ->
                                        hitRect = new Rect(new Position(mob.getPosition().getX() + 170 + 120, mob.getPosition().getY()), new Position(mob.getPosition().getX() + 170 + 120 + 120, mob.getPosition().getY()));
                                case 8800030, 8800010, 8800110 ->
                                        hitRect = new Rect(new Position(mob.getPosition().getX() + 317 + 120, mob.getPosition().getY()), new Position(mob.getPosition().getX() + 317 + 120 + 120, mob.getPosition().getY()));
                            }
                            if (hitRect != null) {
                                for (Char member : field.getChars()) {
                                    if (hitRect.hasPositionInside(member.getPosition())) {
                                        if (mob.getTemplateId() >= 8800023 && mob.getTemplateId() <= 8800030) {
                                            hpDamage = (int) (member.getMaxHP() * 30.0D / 100.0D);
                                        } else if (mob.getTemplateId() >= 8800003 && mob.getTemplateId() <= 8800010) {
                                            hpDamage = (int) (member.getMaxHP() * 50.0D / 100.0D);
                                        } else if (mob.getTemplateId() >= 8800103 && mob.getTemplateId() <= 8800110) {
                                            hpDamage = (int) (member.getMaxHP() * 90.0D / 100.0D);
                                        }
                                        if (hpDamage >= 0) {
                                            if (member.getHP() <= 0) {
                                                continue;
                                            }
                                            member.damage(hpDamage);
                                            member.chatMessage("受到攻擊：" + mob.getTemplateId());
                                        }
                                    }
                                }
                            }
                        }, 1300, TimeUnit.MILLISECONDS);
                    }
                    default -> {
                    }
                }
                break;
            case Summon:
            case Summon2: {
                Position spawnPos = mob.getPosition();
                Set<Mob> spawnedMobs = field.getMobs().stream().filter(m -> m.getMobSpawnerId() == mob.getObjectId()).collect(Collectors.toSet());
                for (int mobId : msi.getInts()) {
                    long spawnedSize = spawnedMobs.stream().filter(m -> m.getTemplateId() == mobId).count();
                    int maxSpawned = Math.min(msi.getSkillStatIntValue(limit), 15);
                    if (spawnedSize < maxSpawned) {
                        try {
                            Mob m = field.spawnMobForTime(mobId, spawnPos.getX(), spawnPos.getY(), 20 * 1000);
                            m.setMobSpawnerId(mob.getObjectId());
                        } catch (NullPointerException e) {
                            for (Char member : field.getChars()) {
                                if (member != null) {
                                    spawnPos = member.getPosition();
                                    break;
                                }
                            }
                            Mob m = field.spawnMobForTime(mobId, spawnPos.getX(), spawnPos.getY(), 20 * 1000);
                            m.setMobSpawnerId(mob.getObjectId());
                        }
                    }
                }
                break;
            }
            case Stun: {
                Rect hitRect = new Rect(new Position(mob.getPosition().getX() - 200, mob.getPosition().getY()), new Position(mob.getPosition().getX() + 200, mob.getPosition().getY()));
                for (Char member : field.getCharsInRect(hitRect)) {
                    applyEffect(member);
                }
                break;
            }
            case PMCounter: {
                //Removed
                break;
            }
            case PhysicalImmune: {
                if (!mts.hasCurrentMobStat(MobStat.PImmune)) {
                    mob.getTimer().addEvent(() -> {
                        o.nOption = msi.getSkillStatIntValue(x);
                        mts.addMobSkillOptions(mob, MobStat.PImmune, o);
                    }, 3, TimeUnit.SECONDS);
                }
                break;
            }
            case MagicImmune: {
                if (!mts.hasCurrentMobStat(MobStat.MImmune)) {
                    mob.getTimer().addEvent(() -> {
                        o.nOption = msi.getSkillStatIntValue(x);
                        mts.addMobSkillOptions(mob, MobStat.MImmune, o);
                    }, 3, TimeUnit.SECONDS);
                }
                break;
            }
            case HealM: {
                for (Mob mobHeal : mobs) {
                    mobHeal.heal(msi.getSkillStatIntValue(x));
                }
                break;
            }
            default:
                break;
        }
    }

    public void vellumApplyEffect(Mob mob) {
        short skill = (short) getSkillID();
        short slv = (short) getLevel();
        MobSkillInfo msi = SkillData.getMobSkillInfoByIdAndLevel(skill, slv);
        MobSkillID msID = MobSkillID.getMobSkillIDByVal(skill);
        Field field = mob.getField();
        Rect rect = null;
        Set<Mob> mobs = new HashSet<>();
        if (msi.getLt() != null && msID != MobSkillID.Damage && slv != 27) {
            rect = new Rect(msi.getLt(), msi.getRb());
            if (mob.isFlip()) {
                rect.horizontalFlipAround(mob.getPosition().getX());
            }
            mobs.addAll(field.getMobsInRect(rect));
        } else if (msi.getLt2() != null && msID != MobSkillID.Damage && slv != 27) {
            rect = new Rect(msi.getLt2(), msi.getRb2());
            if (mob.isFlip()) {
                rect.horizontalFlipAround(mob.getPosition().getX());
            }
            mobs.addAll(field.getMobsInRect(rect));
        }
        switch (msID) {
            case Teleport: {
                boolean isLeft = mob.getMoveAction() % 2 != 0;
                field.broadcast(WvsContext.weatherEffectNotice(WeatherEffNoticeType.BossVellum, "貝倫正在深吸一口氣。", 10000));
                Position pos;
                if (Util.succeedProp(50)) {
                    pos = new Position(1200, 443);
                } else {
                    pos = new Position(-1700, 443);
                }
                mob.setPosition(pos);
                field.broadcast(MobPool.teleportRequest(mob, false, 3, pos));
                break;
            }
            case Summon2: {
                Char chr = mob.getField().getCharByID(mob.getControllerID());
                if (chr == null) {
                    break;
                }
                Position spawnPos = chr.getPosition();
                Set<Mob> spawnedMobs = field.getMobs().stream().filter(m -> m.getMobSpawnerId() == mob.getObjectId()).collect(Collectors.toSet());
                for (int mobId : msi.getInts()) {
                    long spawnedSize = spawnedMobs.stream().filter(m -> m.getTemplateId() == mobId).count();
                    int maxSpawned = Math.min(msi.getSkillStatIntValue(limit), 15);
                    if (spawnedSize < maxSpawned) {
                        try {
                            Mob m = field.spawnMobForTime(mobId, spawnPos.getX(), spawnPos.getY(), 20 * 1000);
                            m.setMobSpawnerId(mob.getObjectId());
                        } catch (NullPointerException e) {
                            for (Char member : field.getChars()) {
                                if (member != null) {
                                    spawnPos = member.getPosition();
                                    break;
                                }
                            }
                            Mob m = field.spawnMobForTime(mobId, spawnPos.getX(), spawnPos.getY(), 20 * 1000);
                            m.setMobSpawnerId(mob.getObjectId());
                        }
                    }
                }
                //Rock Fall
                List<Position> positions = new ArrayList<>();
                for (int i = 0; i < Util.getRandom(6, 9); i++) {
                    positions.add(Util.getRandomFromCollection(BossConstants.vellumStonePositionList));
                }
                chr.getField().broadcast(FieldPacket.createFallingCatcher("DropStone", 0, positions.size(), positions));
                positions.clear();
                break;
            }
            case AreaForce: {
                boolean isLeft = mob.getMoveAction() % 2 != 0;
                isLeft = true;
                AffectedArea aa = AffectedArea.getAreaForce(mob, skill, slv, 10000);
                aa.setPosition(isLeft ? new Position(965, 443) : new Position(-1750, 443));
                aa.setForce(isLeft ? 9275169 : 9175169);
                aa.setRect(new Rect(-1870, 133, 1150, 463));
                field.spawnAffectedArea(aa);
                break;
            }
        }
        //chr.chatMessage(String.format("[Mob Skill] Controller: %s | Mob ID: %d | %s (%d) | level = %d", chr.getName(), mob.getTemplateId(), msID, getSkillID(), getLevel()));
    }


    public static int getForceAttack(Mob mob, int attackIndex, boolean vellumBreath) {
        if (BossConstants.isVellum(mob.getTemplateId())) {
            if (vellumBreath) {
                return 9;
            }
            switch (attackIndex) {
                //Throws fireballs
                case 2:
                    return 3;
                case 3:
                    return 4;
                case 4:
                    return 5;
                case 5:
                    return 6;
                //Breath
                case 9:
                    return 10;
                case 10:
                    return 11;
                case 11:
                    return 12;
                //Poison with Chaos
                case 13:
                    return 14;
                case 14:
                    return 15;
                case 15:
                    return 16;
            }
        }
        return 0;
    }

    public static void handleMobSkillByAttackIndex(Char chr, Mob mob, int attackIndex) {
        if (BossConstants.isVellum(mob.getTemplateId())) {
            if (mob.getTemplateId() == 8930000) {
                switch (attackIndex) {
                    case 2, 3, 4, 6, 8, 11, 12, 13, 14, 15 -> {
                        MobSkill mobSkill = new MobSkill();
                        mobSkill.setSkillID(201);
                        mobSkill.setLevel(49);
                        mobSkill.vellumApplyEffect(mob);
                    }
                    case 10 -> {
                        MobSkill mobSkill = new MobSkill();
                        mobSkill.setSkillID(186);
                        mobSkill.setLevel(6);
                        mobSkill.vellumApplyEffect(mob);
                    }
                }
            }
        }
    }

    public static void handleDarkSight(Mob mob) {
        Field field = mob.getField();
        for (Char chr : field.getChars()) {
            TemporaryStatManager tsm = chr.getTemporaryStatManager();
            if (tsm.hasStat(DarkSight)) {
                tsm.removeStat(DarkSight);
            }
        }
    }

    public void doSpider(Char player, Mob monster, int type, boolean solo) {
        List<Triple<Integer, Integer, Integer>> spider = new ArrayList<>();
        if (type == 0) {
            spider.add(new Triple<>(0, 1200, -480));
            spider.add(new Triple<>(1, 1200, -80));
            spider.add(new Triple<>(2, 1200, 320));
            spider.add(new Triple<>(3, 2800, -320));
            spider.add(new Triple<>(4, 2800, 80));
            spider.add(new Triple<>(5, 2800, 480));
            spider.add(new Triple<>(6, 4400, -550));
            spider.add(new Triple<>(7, 4400, -150));
            spider.add(new Triple<>(8, 4400, 250));
            spider.add(new Triple<>(9, 7000, -470));
            spider.add(new Triple<>(10, 7000, -70));
            spider.add(new Triple<>(11, 7000, 330));
            spider.add(new Triple<>(12, 8600, -320));
            spider.add(new Triple<>(13, 8600, 80));
            spider.add(new Triple<>(14, 8600, 480));
            spider.add(new Triple<>(15, 10200, -150));
            spider.add(new Triple<>(16, 10200, 250));
            spider.add(new Triple<>(17, 10200, 650));
        } else {
            spider.add(new Triple<>(0, 1200, -480));
            spider.add(new Triple<>(1, 1200, -80));
            spider.add(new Triple<>(2, 1200, 320));
            spider.add(new Triple<>(3, 2800, -320));
            spider.add(new Triple<>(4, 2800, 80));
            spider.add(new Triple<>(5, 2800, 480));
            spider.add(new Triple<>(6, 4400, -550));
            spider.add(new Triple<>(7, 4400, -150));
            spider.add(new Triple<>(8, 4400, 250));
            spider.add(new Triple<>(9, 7000, -480));
            spider.add(new Triple<>(10, 7000, -80));
            spider.add(new Triple<>(11, 7000, 320));
            spider.add(new Triple<>(12, 8600, -320));
            spider.add(new Triple<>(13, 8600, 80));
            spider.add(new Triple<>(14, 8600, 480));
            spider.add(new Triple<>(15, 10200, -550));
            spider.add(new Triple<>(16, 10200, -150));
            spider.add(new Triple<>(17, 10200, 250));
        }
        if (solo) {
            spider.add(new Triple<>(0, 12800, -480));
            spider.add(new Triple<>(1, 12800, -80));
            spider.add(new Triple<>(2, 12800, 320));
            spider.add(new Triple<>(3, 14400, -320));
            spider.add(new Triple<>(4, 14400, 80));
            spider.add(new Triple<>(5, 14400, 480));
            spider.add(new Triple<>(6, 16000, -550));
            spider.add(new Triple<>(7, 16000, -150));
            spider.add(new Triple<>(8, 16000, 250));
        }
        player.getField().broadcast(WillPacket.spiderAttack(monster.getTemplateId(), 14, type, spider));
    }

    public static void JinHillaGlassTime(Mob mob, int time) {
        if (mob == null || mob.getField().getChars().size() <= 0) {
            return;
        }
        MobSkill mobSkill = new MobSkill();
        mobSkill.setSkillID(VHillaSlash.getVal());
        mobSkill.setLevel(1);
        mobSkill.setFlip(mob.isFlip());
        mob.getTimer().addEvent(() -> {
            if (mob.isAlive()) {
                mob.getField().broadcast(MobPool.forcedAction(mob.getObjectId(), 2));
                mob.setSkillDelay(mobSkill.getSkillDelay());
                mob.getField().broadcast(MobPool.setSkillDelay(mob, mobSkill.getSkillDelay(), VHillaSlash.getVal(), 1));
                mobSkill.applyEffect(mob);
                JinHillaGlassTime(mob, (mob.getHPPercent() >= 60) ? 150 : ((mob.getHPPercent() >= 30) ? 120 : 100));
            }
        }, (time * 1000L));
    }
}
