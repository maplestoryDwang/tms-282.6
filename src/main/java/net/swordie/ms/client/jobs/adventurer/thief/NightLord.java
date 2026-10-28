package net.swordie.ms.client.jobs.adventurer.thief;

import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.*;
import net.swordie.ms.client.character.skills.info.*;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.*;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.constants.SkillConstants;
import net.swordie.ms.enums.AssistType;
import net.swordie.ms.enums.ForceAtomEnum;
import net.swordie.ms.enums.MoveAbility;
import net.swordie.ms.life.AffectedArea;
import net.swordie.ms.life.Life;
import net.swordie.ms.life.Summon;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.life.mob.MobStat;
import net.swordie.ms.life.mob.MobTemporaryStat;
import net.swordie.ms.life.mob.skill.BurnedInfo;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.util.Position;
import net.swordie.ms.util.Rect;
import net.swordie.ms.util.Util;
import net.swordie.ms.world.field.Field;

import java.util.*;

import static net.swordie.ms.client.character.skills.SkillStat.*;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.DarkSight;

public class NightLord extends Thief {

    public static final int SHURIKEN_BURST = 4101008;
    public static final int GUST_CHARM = 4101010;

    public static final int ASSASSINS_MARK = 4101011; //Buff (ON/OFF)
    public static final int ASSASSINS_MARK_ATOM = 4100012;
    public static final int SHADOW_SURGE = 4101015;
    public static final int NIGHTLORD_MARK_ATOM = 4120019;

    public static final int TRIPLE_THROW = 4111010;
    public static final int SHADE_SPLITTER = 4111015;
    public static final int SHADOW_PARTNER = 4111002; //Buff
    public static final int EXPERT_THROWING_STAR_HANDLING = 4110012;
    public static final int DARK_FLARE = 4111007; //Summon
    public static final int SHADOW_WEB = 4111003; //Special Attack (Dot + Bind)
    public static final int VENOM = 4110011; //Passive DoT
    public static final int SPIRIT_OF_THE_STAR = 4110016; // Passive NoBulletConsume

    public static final int QUAD_STAR = 4121013;
    public static final int SHOWDOWN = 4121017; //Special Attack
    public static final int SHOWDOWN_ATOM = 4121020; //Special Attack
    public static final int SUDDEN_RAID = 4121016; //Special Attack
    public static final int FRAILTY_CURSE = 4121015; //AoE
    public static final int FRAILTY_CURSE_SLOW = 4120047;
    public static final int FRAILTY_CURSE_ENHANCE = 4120046;
    public static final int FRAILTY_CURSE_BOSS_RUSH = 4120048;
    public static final int NIGHT_LORD_MARK = 4120018;
    public static final int TOXIC_VENOM = 4120011; //Passive DoT
    public static final int HEROS_WILL = 4121009;
    public static final int NIGHTFALL_SIGNET = 4121022;

    //Hyper skills
    public static final int DEATH_STAR = 4121052;
    public static final int EPIC_ADVENTURE = 4121053;
    public static final int BLEED_DART = 4121054;

    // V Skills
    public static final int DARK_LORDS_OMEN = 400041038;
    public static final int THROWING_STAR_BARRAGE = 400041001;
    public static final int THROWING_STAR_BARRAGE_DOUBLE = 400041016; // Lucky Seven
    public static final int THROWING_STAR_BARRAGE_TRIPLE = 400041017; // Triple Throw
    public static final int THROWING_STAR_BARRAGE_QUAD = 400041018; // Quad Throw
    public static final int SHURRIKANE = 400041020;
    public static final int THROW_BLASTING_BUFF = 400041061; // buff
    public static final int THROW_BLASTING_ATTACK = 400041062; //  attack
    public static final int THROW_BLASTING_CD = 400041079; // cooldown

    // HEXA Skills
    public static final int HEXA_QUAD_STAR = 4141000;
    public static final int HEXA_ENHANCED_QUAD_STAR = 4141001;
    public static final int HEXA_ASSASSINS_MARK = 4141002;
    public static final int HEXA_ASSASSINS_MARK_DOT = 4141003;
    public static final int HEXA_ASSASSINS_MARK_ATOM = 4141004;
    public static final int HEXA_DARK_FLARE = 4141007;
    public static final int HEXA_SHOWDOWN = 4141005;
    public static final int HEXA_SHOWDOWN_ATOM = 4141006;
    public static final int DARKNESS_SHURIKEN = 4140011;
    public static final int HEXA_SUDDEN_RAID = 4141008;
    public static final int HEXA_DEATH_STAR = 4141009;
    public static final int HEXA_DEATH_STAR_RAMPANT = 4141010;

    ShootObjectSkillInfo sosi = null;
    private long darkShurikenNextGainAt = 0; // gate 5s trong thời gian cooldown
    private int  darkShurikenExtraSec = 0;   // extra cộng cho lần proc kế tiếp (0..cap-base)

    public NightLord(Char chr) {
        super(chr);
    }

    @Override
    public boolean isHandlerOfJob(short id) {
        return JobConstants.isNightLord(id);
    }

    @Override
    public void handleCancelTimer(Char chr) {
        super.handleCancelTimer(chr);
    }

    // Attack related methods ------------------------------------------------------------------------------------------
    @Override
    public void handleDebuffOnMob(Client c, Mob mob, SkillInfo si, int skillID, int slv, long damage) {
        Char chr = c.getChr();
        if (skillID == 400041018 || skillID == 4120019 || skillID == 4100012) {
            //This skill not active type like create atom, dot damage,...
            return;
        }
        Option o1 = new Option();
        Option o2 = new Option();
        MobTemporaryStat mts = mob.getTemporaryStat();
        EnumMap<MobStat, Option> map = new EnumMap<>(MobStat.class);
        BurnedInfo bi;

        setMarkonMob(mob, damage);

        applyPassiveDoTSkillsOnMob(mob, damage);

        applyBleedDartOnMob(mob, damage);

        switch (skillID) {
            case SHADOW_WEB:
                bi = BurnedInfo.createBurnInfo(chr, skillID, slv, damage);
                if (Util.succeedProp(si.getValue(prop, slv)) && !mts.hasCurrentMobStatBySkillId(skillID) && !mob.isBoss()) {
                    o1.nOption = 1;
                    o1.rOption = skillID;
                    o1.tOption = si.getValue(time, slv);
                    mts.addStatOptions(mob, MobStat.Stun, o1);
                } else {
                    mts.createAndAddBurnedInfo(mob, bi, skillID);
                }
                break;
            case SHOWDOWN:
            case HEXA_SHOWDOWN:
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    if (!mob.isBoss()) {
                        o1.nOption = 1;
                        o1.rOption = skillID;
                        o1.tOption = si.getValue(time, slv);
                        map.put(MobStat.Showdown, o1);
                    }
                    o2.nOption = 1;
                    o2.rOption = skillID;
                    o2.tOption = si.getValue(time, slv);
                    o2.xOption = (mob.isBoss() ? si.getValue(x, slv) / 2 : si.getValue(x, slv)); // Exp
                    o2.yOption = (mob.isBoss() ? si.getValue(x, slv) / 2 : si.getValue(x, slv)); // Item Drop
                    map.put(MobStat.AddEffect, o2);
                    mts.addStatOptions(mob, map);
                }
                break;
            case SUDDEN_RAID:
            case HEXA_SUDDEN_RAID:
                bi = BurnedInfo.createBurnInfo(chr, skillID, slv, damage);
                mts.createAndAddBurnedInfo(mob, bi, skillID);
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
        int skillID = attackInfo.skillId;
        boolean hasHitMobs = !attackInfo.mobAttackInfo.isEmpty();
        int slv = attackInfo.slv;
        EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (hasHitMobs) {
            if (tsm.hasStat(DarkSight)) {
                tsm.removeStat(DarkSight);
            }
            if ((chr.hasSkill(ASSASSINS_MARK) || chr.hasSkill(HEXA_ASSASSINS_MARK))
                    && skillID != THROWING_STAR_BARRAGE_DOUBLE
                    && skillID != THROWING_STAR_BARRAGE_TRIPLE
                    && skillID != THROWING_STAR_BARRAGE_QUAD) {
                if (!SkillConstants.isForceAtomSkill(skillID)) {
                    handleMark(skillID);
                }
            }
            // Expert Throwing Star Handling
            if (skillID != NIGHTLORD_MARK_ATOM && skillID != ASSASSINS_MARK_ATOM && skillID != HEXA_ASSASSINS_MARK_ATOM) {
                procExpertThrowingStar(skillID);
            }


            if (isShurikenAttack(attackInfo.skillId)) {

                // Throw Blasting
                if (chr.hasSkill(THROW_BLASTING_BUFF)) {
                    if (tsm.hasStat(ThrowBlasting)) {
                        doActiveThrowBlasting(attackInfo);
                    } else {
                        doPassiveThrowBlasting(attackInfo);
                    }
                }

                // Darkness Shuriken
                if (chr.hasSkill(DARKNESS_SHURIKEN)) {
                    doDarknessShuriken(now);
                }
            }
        }
        switch (skillID) {
            case THROWING_STAR_BARRAGE_DOUBLE:
            case THROWING_STAR_BARRAGE_TRIPLE:
            case THROWING_STAR_BARRAGE_QUAD:
                final int shootObjId = attackInfo.shootObjId;
                ShootObject matched = null;
                final List<ShootObject> list = sosi.getShootObjects();
                for (int i = list.size() - 1; i >= 0; i--) { // tương đương reverse + put đè key
                    ShootObject so = list.get(i);
                    if (so.getId() == shootObjId) {
                        matched = so;
                        break;
                    }
                }
                if (matched == null || !chr.hasSkill(ASSASSINS_MARK)) {
                    return;
                }
                for (MobAttackInfo mai : attackInfo.mobAttackInfo) {
                    Life life = chr.getField().getLifeByObjectID(mai.mobId);
                    if (!(life instanceof Mob mob) || mob.getHp() <= 0) {
                        continue;
                    }
                    long minDamage = Long.MAX_VALUE;
                    long[] damages = mai.damages;
                    for (int i = 0; i < damages.length; i++) {
                        long d = damages[i];
                        if (d < minDamage) minDamage = d;
                    }
                    if (damages.length == 0) {
                        continue; // hoặc minDamage = 0 tùy bạn muốn xử lý
                    }
                    applyPassiveDoTSkillsOnMob(mob, minDamage);
                }
                handleMark(skillID);
                break;
            case DARK_LORDS_OMEN:
                if (attackInfo.attackActionType == 25 && attackInfo.summon != null) {
                    tsm.removeStatsBySkill(attackInfo.summon.getSkillID());
                    chr.getField().removeSummon(attackInfo.summon.getSkillID(), chr.getId());
                }
                break;
            case SHURRIKANE:
                if (!chr.hasSkillOnCooldown(skillID)) {
                    chr.addSkillCooldown(skillID, 25000);
                }
                break;
            case HEXA_QUAD_STAR:
            case HEXA_ENHANCED_QUAD_STAR:
                Option o1 = tsm.getOption(EnhanceQuadrupleThrow);
                var val = o1.nOption + 1;
                if (val > 3) {
                    val = 0;
                }
                o1.nOption = val;
                o1.rOption = skillID;
                tsm.sendStat(EnhanceQuadrupleThrow, o1);
                break;
            case HEXA_DEATH_STAR:
            case HEXA_DEATH_STAR_RAMPANT:
                o1 = tsm.getOption(EnhanceNightLordFourSeasons);
                val = o1.nOption + 1;
                if (val > 1) {
                    val = 0;
                }
                o1.nOption = val;
                o1.rOption = skillID;
                tsm.sendStat(EnhanceNightLordFourSeasons, o1);
                break;
        }
    }

    private void doDarknessShuriken(long now) {
        final int skillId = DARKNESS_SHURIKEN;
        int slv = chr.getSkillLevel(skillId);
        if (slv <= 0) return;

        SkillInfo si = SkillData.getSkillInfoById(skillId);

        final int cdSec       = si.getValue(cooltime, slv); // 60
        final int baseAtomSec = si.getValue(x, slv);        // 10
        final long gateMs     = si.getValue(w, slv) * 1000L;// 5s
        final int addSec      = si.getValue(y, slv);        // 7
        final int capTotalSec = si.getValue(z, slv);        // 60 (TOTAL duration cap)

        // Trong cooldown 60s -> chỉ tích duration cho lần proc kế tiếp (không sendStat, không spawn)
        if (chr.hasSkillOnCooldown(skillId)) {
            if (now >= darkShurikenNextGainAt) {
                darkShurikenNextGainAt = now + gateMs;

                int curTotal = baseAtomSec + darkShurikenExtraSec;
                int newTotal = Math.min(capTotalSec, curTotal + addSec);
                darkShurikenExtraSec = newTotal - baseAtomSec; // lưu dạng extra
            }
            return;
        }

        // Cooldown hết -> proc mới: duration = base + extra (cap), và tOption PHẢI = expire
        int durationSec = Math.min(capTotalSec, baseAtomSec + darkShurikenExtraSec);

        // set cooldown 60s cho lần tiếp theo
        chr.addSkillCooldown(skillId, cdSec * 1000);

        // sendStat 1 lần, tOption = durationSec (đồng nhất với expire)
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o = new Option();
        o.nOption = 1;
        o.rOption = skillId;
        o.tOption = durationSec;
        tsm.sendStat(DarknessShuriken, o);

        // spawn SecondAtom 1 lần, expire = durationSec
        var sai = si.getSecondAtomInfos().get(0);
        final var pos = chr.getPosition().add(sai.getPos());

        SecondAtom sa = new SecondAtom(sai, chr.getNewSecondAtomKey(), chr.getId(), 0, 0, si.getSkillId(), pos, now);
        sa.setExpire(durationSec * 10000);
        chr.createSecondAtom(sa);

        // reset vòng đời cộng dồn cho lần sau
        darkShurikenExtraSec = 0;
        darkShurikenNextGainAt = now + gateMs;
    }

    private boolean isShurikenAttack(int skillId) {
        return skillId == LUCKY_SEVEN
                || skillId == SHURIKEN_BURST
                || skillId == GUST_CHARM
                || skillId == TRIPLE_THROW
                || skillId == SHADE_SPLITTER
                || skillId == QUAD_STAR || skillId == HEXA_QUAD_STAR
                || skillId == SHOWDOWN || skillId == HEXA_SHOWDOWN;
    }

    public void doActiveThrowBlasting(AttackInfo attackInfo) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (!tsm.hasStat(ThrowBlasting) || attackInfo.mobAttackInfo.size() <= 0) {
            return;
        }

        Option o = tsm.getOption(ThrowBlasting);
        var charmCount = o.nOption;
        var minCharms = o.sOption;
        var maxCharms = o.wOption;

        minCharms = Math.min(charmCount, minCharms);
        maxCharms = Math.min(charmCount, maxCharms);

        var charmsThrown = Util.getRandom(minCharms, maxCharms + 1);

        Mob mob = (Mob) chr.getField().getLifeByObjectID(attackInfo.mobAttackInfo.getFirst().mobId);
        if (mob == null) {
            mob = Util.getRandomFromCollection(chr.getField().getMobs(attackInfo.mobAttackInfo));
        }
        if (mob == null) {
            return;
        }
        Rect rect = mob.getRectAround(new Rect(-130, -130, 130, 130));
        List<ExtraSkill> extraSkills = new ArrayList<>();
        for (int i = 0; i < charmsThrown; i++) {
            ExtraSkill extraSkill = new ExtraSkill(THROW_BLASTING_ATTACK, rect.getRandomPositionInside());
            extraSkill.FaceLeft = chr.isLeft() ? 1 : 0;
            extraSkill.Value = 1;
            extraSkills.add(extraSkill);
        }
        if (!extraSkills.isEmpty()) {
            chr.write(UserLocal.registerExtraSkill(THROW_BLASTING_BUFF, extraSkills));
        }

        o.nOption -= charmsThrown;
        if (o.nOption <= 0) {
            tsm.removeStatsBySkill(THROW_BLASTING_BUFF);
            return;
        }
        tsm.updateStat(ThrowBlasting, o);
    }

    public void doPassiveThrowBlasting(AttackInfo attackInfo) {
        if (chr.hasSkillOnCooldown(THROW_BLASTING_CD)) {
            return;
        }
        Mob mob = (Mob) chr.getField().getLifeByObjectID(attackInfo.mobAttackInfo.getFirst().mobId);
        if (mob == null) {
            return;
        }
        Rect rect = mob.getRectAround(new Rect(-80, -80, 80, 80));
        ExtraSkill extraSkill = new ExtraSkill(THROW_BLASTING_CD, rect.getRandomPositionInside());
        extraSkill.FaceLeft = chr.isLeft() ? 1 : 0;
        extraSkill.Value = 2;
        chr.write(UserLocal.registerExtraSkill(THROW_BLASTING_BUFF, List.of(extraSkill)));
        chr.addSkillCooldown(THROW_BLASTING_CD, 120_000);
    }

    private void procExpertThrowingStar(int skillId) {
        if (!chr.hasSkill(EXPERT_THROWING_STAR_HANDLING)) {
            return;
        }
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o = new Option();

        Skill skill = chr.getSkill(EXPERT_THROWING_STAR_HANDLING);
        SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
        int slv = skill.getCurrentLevel();
        int hideIconSkillId = skill.getSkillId() + 100; // there's no Buff Icon

        if (tsm.getOptByCTSAndSkill(IndieDamR, hideIconSkillId) == null) {
            if (tsm.hasStatBySkillId(hideIconSkillId)) {
                tsm.removeStatsBySkill(hideIconSkillId);
            }
            if (Util.succeedProp(si.getValue(prop, slv))) {
                o.nReason = hideIconSkillId;
                o.nValue = si.getValue(pdR, slv);
                o.tTerm = 5;
                tsm.sendStat(IndieDamR, o);

                chr.write(UserLocal.setNextShootExJablin());
            }
        } else {
            tsm.removeStatsBySkill(hideIconSkillId);
            o.nOption = 100;
            o.rOption = hideIconSkillId;
            o.tOption = 5;
            tsm.sendStat(CriticalGrowing, o);

            chr.write(UserLocal.setNextShootExJablin());

            if (SkillData.getSkillInfoById(skillId) != null) {
                chr.healMP(SkillData.getSkillInfoById(skillId).getValue(mpCon, slv));
            }

            chr.write(UserPacket.effect(Effect.skillAffected(skill.getSkillId(), slv, 0)));
            chr.getField().broadcast(UserRemote.effect(chr.getId(), Effect.skillAffected(skill.getSkillId(), slv, 0)), chr);
        }
    }

    public void createDarkLordOmenForceAtoms(Summon summon) {
        int skillID = DARK_LORDS_OMEN;
        Skill skill = chr.getSkill(skillID);
        if (skill == null) {
            return;
        }
        SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
        int slv = chr.getSkillLevel(skill.getSkillId());

        Position position = summon.getPosition();
        Rect rect = position.getRectAround(si.getFirstRect());
        ForceAtomEnum fae = chr.hasSkill(NIGHT_LORD_MARK) ? ForceAtomEnum.DARK_LORD_OMEN_2 : ForceAtomEnum.DARK_LORD_OMEN;
        int mobCountInRect = chr.getField().getMobsInRect(rect).size();
        int forceAtomCount = ((mobCountInRect * si.getValue(bulletCount, slv)) + si.getValue(x, slv));
        List<Integer> targetList = new ArrayList<>();
        List<ForceAtomInfo> faiList = new ArrayList<>();
        for (int i = 0; i < forceAtomCount; i++) {
            int angle = (360 / forceAtomCount) * i;
            Mob mob = Util.getRandomFromCollection(chr.getField().getMobs());
            ForceAtomInfo forceAtomInfo = new ForceAtomInfo(chr.getNewForceAtomKey(), fae.getInc(), 44, 3,
                    angle, 0, Util.getCurrentTime(), 1, 0,
                    new Position());
            targetList.add(mob != null ? mob.getObjectId() : 0);
            faiList.add(forceAtomInfo);
        }
        chr.createForceAtom(new ForceAtom(false, summon.getObjectId(), chr.getId(), fae,
                true, targetList, skillID, faiList, rect, 0, 0,
                position, chr.getBulletIDForAttack(), position, 0));
        var effect = Effect.skillAffected(skillID, slv, 0);
        chr.write(UserPacket.effect(effect));
        chr.getField().broadcast(UserRemote.effect(chr.getId(), effect), chr);
    }

    private void setMarkonMob(Mob mob, long damage) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (tsm.hasStat(NightLordMark)) {
            int skillID = chr.hasSkill(HEXA_ASSASSINS_MARK) ? HEXA_ASSASSINS_MARK_DOT : getMarkSkill().getSkillId();
            int slv = getMarkSkill().getCurrentLevel();
            var bi = BurnedInfo.createBurnInfo(chr, skillID, slv, damage);
            MobTemporaryStat mts = mob.getTemporaryStat();
            mts.createAndAddBurnedInfo(mob, bi, skillID);
        }
    }

    private void handleMark(int skillID) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (!tsm.hasStat(NightLordMark)) {
            return;
        }
        if (skillID != NIGHTLORD_MARK_ATOM && skillID != ASSASSINS_MARK_ATOM && skillID != HEXA_ASSASSINS_MARK_ATOM) {
            Field field = chr.getField();
            Skill skill = getMarkSkill();
            int starCount = chr.getSkillStatValue(bulletCount, skill.getSkillId());

            ForceAtomEnum fae = ForceAtomEnum.ASSASSIN_MARK;
            int atom = ASSASSINS_MARK_ATOM;
            if (chr.hasSkill(HEXA_ASSASSINS_MARK)) {
                fae = ForceAtomEnum.HEXA_ASSASSIN_MARK;
                atom = HEXA_ASSASSINS_MARK_ATOM;
            } else if (chr.hasSkill(NIGHT_LORD_MARK)) {
                fae = ForceAtomEnum.NIGHTLORD_MARK;
                atom = NIGHTLORD_MARK_ATOM;
            }
            int randomInt = new Random().nextInt((360 / starCount) - 1);
            Rect rect = new Rect(new Position(chr.getPosition().getX() - 300, chr.getPosition().getY() - 300), new Position(chr.getPosition().getX() + 300, chr.getPosition().getY() + 300));
            List<Mob> lifes = chr.getField().getMobsInRect(rect);
            if (lifes.size() <= 0) {
                return;
            }
            List<Integer> targetList = new ArrayList<>();
            List<ForceAtomInfo> faiList = new ArrayList<>();
            Mob mob = Util.getRandomFromCollection(lifes);
            var mobs = field.getMobsInRect(mob.getRectAround(rect));
            for (int i = 0; i < getAssassinsMarkStarCount(); i++) {
                Mob m;
                if (!mobs.isEmpty()) {
                    m = Util.getRandomFromCollection(mobs);
                } else {
                    m = null;
                }
                targetList.add(m == null || m.getHp() <= 0 ? 0 : m.getObjectId());

                int angle = (360 / starCount) * i;
                ForceAtomInfo fai = new ForceAtomInfo(chr.getNewForceAtomKey(), fae.getInc(), 40, 4,
                        randomInt + angle, 170, Util.getCurrentTime(), 0, 0,
                        new Position());
                faiList.add(fai);
            }
            chr.createForceAtom(new ForceAtom(true, chr.getId(), mob.getObjectId(), fae,
                    true, targetList, atom, faiList, rect, 0, 300,
                    new Position(), chr.getBulletIDForAttack(), new Position(), 0));
        }
    }

    private int getAssassinsMarkStarCount() {
        if (getMarkSkill() != null) {
            Skill skill = getMarkSkill();
            SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
            int slv = skill.getCurrentLevel();

            return si.getValue(bulletCount, slv) * 2;
        }
        return 0;
    }

    private Skill getMarkSkill() {
        Skill skill = null;
        if (chr.hasSkill(ASSASSINS_MARK)) {
            skill = chr.getSkill(ASSASSINS_MARK);
        }
        if (chr.hasSkill(NIGHT_LORD_MARK)) {
            skill = chr.getSkill(NIGHT_LORD_MARK);
        }
        if (chr.hasSkill(HEXA_ASSASSINS_MARK)) {
            skill = chr.getSkill(HEXA_ASSASSINS_MARK);
        }
        return skill;
    }

    private int getCurMarkLv() {
        int supgrade = 0;
        if (chr.hasSkill(ASSASSINS_MARK)) {
            supgrade = ASSASSINS_MARK;
        }
        if (chr.hasSkill(NIGHT_LORD_MARK)) {
            supgrade = NIGHT_LORD_MARK;
        }
        if (chr.hasSkill(HEXA_ASSASSINS_MARK)) {
            supgrade = HEXA_ASSASSINS_MARK;
        }
        return supgrade;
    }

    private void applyPassiveDoTSkillsOnMob(Mob mob, long damage) {
        MobTemporaryStat mts = mob.getTemporaryStat();
        if (chr.hasSkill(TOXIC_VENOM)) {
            Skill skill = chr.getSkill(TOXIC_VENOM);
            int slv = skill.getCurrentLevel();
            SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
            int proc = si.getValue(prop, slv);
            if (Util.succeedProp(proc)) {
                BurnedInfo bi = BurnedInfo.createBurnInfo(chr, TOXIC_VENOM, slv, damage);
                mts.createAndAddBurnedInfo(mob, bi, TOXIC_VENOM);
            }
        } else if (chr.hasSkill(VENOM)) {
            Skill skill = chr.getSkill(VENOM);
            int slv = skill.getCurrentLevel();
            SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
            int proc = si.getValue(prop, slv);
            if (Util.succeedProp(proc)) {
                BurnedInfo bi = BurnedInfo.createBurnInfo(chr, VENOM, slv, damage);
                mts.createAndAddBurnedInfo(mob, bi, VENOM);
            }
        }
    }

    private void applyBleedDartOnMob(Mob mob, long damage) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (tsm.hasStat(BleedingToxin)) {
            Skill skill = chr.getSkill(BLEED_DART);
            BurnedInfo bi = BurnedInfo.createBurnInfo(chr, BLEED_DART, skill.getCurrentLevel(), damage);
            MobTemporaryStat mts = mob.getTemporaryStat();
            mts.createAndAddBurnedInfo(mob, bi, BLEED_DART);
        }
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
            case ASSASSINS_MARK:
            case HEXA_ASSASSINS_MARK:
                if (tsm.hasStat(NightLordMark)) {
                    tsm.removeStat(NightLordMark);
                } else {
                    o1.nOption = 1;
                    o1.rOption = skillID;
                    tsm.sendStat(NightLordMark, o1);
                }
                break;
            case SHADOW_PARTNER:
                o1.nOption = si.getValue(x, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(ShadowPartner, o1);
                break;
            case DARK_FLARE:
            case HEXA_DARK_FLARE: {
                Summon summon = Summon.getSummonByAndSetStat(c.getChr(), skillID, slv);
                Field field = c.getChr().getField();
                summon.setFlyMob(false);
                summon.setMoveAction((byte) 0);
                summon.setMoveAbility(MoveAbility.Stop);
                field.spawnSummon(summon);
                break;
            }
            case EPIC_ADVENTURE:
                o1.nReason = skillID;
                o1.nValue = si.getValue(indieDamR, slv);
                o1.tTerm = si.getValue(time, slv);
                tsm.sendStat(IndieDamR, o1);
                break;
            case BLEED_DART:
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                newStats.put(BleedingToxin, o1);
                o2.nReason = skillID;
                o2.nValue = si.getValue(indiePad, slv);
                o2.tTerm = si.getValue(time, slv);
                newStats.put(IndiePAD, o2);
                tsm.sendStat(newStats);
                break;
            case DARK_LORDS_OMEN: {
                Summon summon = Summon.getSummonByAndSetStat(chr, skillID, slv);
                summon.setFlyMob(false);
                summon.setAttackActive(true);
                summon.setMoveAbility(MoveAbility.Stop);
                summon.setAssistType(AssistType.CreateForceAtom);
                chr.getField().spawnSummon(summon);
                break;
            }
            case THROWING_STAR_BARRAGE:
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(NightLord_SpreadThrow, o1);
                break;
            case FRAILTY_CURSE:
                AffectedArea aa = AffectedArea.getPassiveAA(chr, skillID, slv);
                aa.setPosition(chr.getPosition());
                aa.setRect(aa.getPosition().getRectAround(si.getRects().getFirst()));
                aa.setFlip(!chr.isLeft());
                aa.setDelay((short) 3);
                chr.getField().spawnAffectedArea(aa);
                break;
            case HEROS_WILL:
                tsm.removeAllDebuffs();
                break;
            case THROW_BLASTING_BUFF:
                o1.nOption = si.getValue(x, slv); // count
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                o1.sOption = si.getValue(s, slv); // min blasts
                o1.wOption = si.getValue(w, slv); // max blasts
                tsm.sendStat(ThrowBlasting, o1);
                break;
        }
    }

    @Override
    public void handleShootObject(Char chr, ShootObjectSkillInfo sosi) {
        var skillId = sosi.getSkillId();
        if (skillId == THROWING_STAR_BARRAGE_DOUBLE || skillId == THROWING_STAR_BARRAGE_TRIPLE || skillId == THROWING_STAR_BARRAGE_QUAD) {
            this.sosi = sosi;
        }
        super.handleShootObject(chr, sosi);
    }

    @Override
    public int handleSetCoolDownSkill(int skillId) {
        switch (skillId) {
            case HEXA_DARK_FLARE -> {
                int skillID = DARK_FLARE;
                int slv = chr.getSkillLevel(skillID);
                chr.setSkillCooldown(skillID, slv);
                return 1;
            }
            case HEXA_SUDDEN_RAID -> {
                int skillID = SUDDEN_RAID;
                int slv = chr.getSkillLevel(skillID);
                chr.setSkillCooldown(skillID, slv);
                return 1;
            }
            case HEXA_DEATH_STAR -> {
                int skillID = DEATH_STAR;
                int slv = chr.getSkillLevel(skillID);
                chr.setSkillCooldown(skillID, slv);
                return 1;
            }
        }
        return super.handleSetCoolDownSkill(skillId);
    }
}
