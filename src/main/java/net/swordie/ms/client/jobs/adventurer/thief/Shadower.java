package net.swordie.ms.client.jobs.adventurer.thief;

import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.client.character.skills.ForceAtom;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.Skill;
import net.swordie.ms.client.character.skills.info.*;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.*;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.enums.ForceAtomEnum;
import net.swordie.ms.enums.MoveAbility;
import net.swordie.ms.life.AffectedArea;
import net.swordie.ms.life.Life;
import net.swordie.ms.life.Summon;
import net.swordie.ms.life.drop.Drop;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.life.mob.MobStat;
import net.swordie.ms.life.mob.MobTemporaryStat;
import net.swordie.ms.life.mob.skill.BurnedInfo;
import net.swordie.ms.loaders.ItemData;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.util.Position;
import net.swordie.ms.util.Rect;
import net.swordie.ms.util.Util;
import net.swordie.ms.world.field.Field;
import net.swordie.ms.world.field.Foothold;

import java.util.*;

import static net.swordie.ms.client.character.skills.SkillStat.*;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;

public class Shadower extends Thief {

    public static final int STEAL = 4201017; //Special Attack (Steal Debuff)?

    public static final int SHADOW_PARTNER = 4211008; //Buff
    public static final int DARK_FLARE = 4211007; //Summon
    public static final int PICK_POCKET = 4211003; //Buff
    public static final int MESO_EXPLOSION = 4211006; //CreateForceAtom Attack "mesoExplosion"
    public static final int MESO_EXPLOSION_ATOM = 4210014; // ?
    public static final int VENOM = 4210010; //Passive DoT
    public static final int INTO_DARKNESS = 4211016; //Buff //Stacks (Body Count)

    public static final int BOOMERANG = 4221007; //Special Attack (Stun Debuff)
    public static final int MAPLE_WARRIOR = 4221000; //Buff
    public static final int SHADOWER_INSTINCT = 4220013; //Passive
    public static final int SUDDEN_RAID = 4221010; //Special Attack
    public static final int MESO_EXPLOSION_ENHANCE = 4220045;
    public static final int SMOKE_SCREEN = 4221006; //Affected Area
    public static final int TOXIC_VENOM = 4220011; //Passive DoT
    public static final int HEROS_WILL = 4221008;
    public static final int ASSASSINATE = 4221014;
    public static final int ASSASSINATE_FINISHER = 4221016;
    public static final int CRUEL_STAB = 4221017;
    public static final int BLOOD_MONEY = 4221018;
    public static final int ENHANCED_MESO_EXPLOSION = 4221019; //CreateForceAtom Attack "mesoExplosion"
    public static final int ENHANCED_MESO_EXPLOSION_ATOM = 4220021;

    //Hyper skills
    public static final int SHADOW_VEIL = 4221052;
    public static final int EPIC_ADVENTURE = 4221053;
    public static final int FLIP_THE_COIN = 4221054;

    // V Skills
    public static final int SHADOW_ASSAULT_4 = 400041005;
    public static final int SHADOW_ASSAULT_3 = 400041004;
    public static final int SHADOW_ASSAULT_2 = 400041003;
    public static final int SHADOW_ASSAULT = 400041002;
    public static final int TRICKBLADE = 400041025;
    public static final int TRICKBLADE_FINISHER = 400041026;
    public static final int TRICKBLADE_MOB_ATTACK = 400041027;
    public static final int SONIC_BLOW = 400041039;
    public static final int SLASH_SHADOW_FORMATION = 400041069;
    public static final int SLASH_SHADOW_FORMATION_1 = 400041070;
    public static final int SLASH_SHADOW_FORMATION_2 = 400041071;
    public static final int SLASH_SHADOW_FORMATION_3 = 400041072;
    public static final int SLASH_SHADOW_FORMATION_4 = 400041073;

    // HEXA Skills
    public static final int HALVE_CUT = 4241500;
    public static final int COVETOUS_DARKNESS = 4241503; // AuthenticDarkness
    public static final int COVETOUS_DARKNESS_FA = 4241504; // AuthenticDarkness
    public static final int COVETOUS_DARKNESS_AA = 4241505; // AuthenticDarkness
    public static final int HEXA_ASSASSINATE = 4241000;
    public static final int HEXA_ASSASSINATE_FINISHER = 4241001;
    public static final int HEXA_PULVERIZE = 4241002;
    public static final int HEXA_PULVERIZ_FINISHER = 4241003;
    public static final int HEXA_MESO_EXPLOSION = 4241006;
    public static final int HEXA_ENHANCED_MESO_EXPLOSION = 4241007;
    public static final int HEXA_MESO_EXPLOSION_ATOM = 4241008;
    public static final int HEXA_ENHANCED_MESO_EXPLOSION_ATOM = 4241009;
    public static final int HEXA_DARK_FLARE = 4241012;
    public static final int HEXA_CRUEL_STAB = 4241011;
    public static final int HEXA_SHADOW_VEIL = 4241013;
    public static final int HEXA_SHADOW_VEIL_ATT = 4240014;
    public static final int HEXA_SUDDEN_RAID = 4241015;
    public static final int COVERT_EDGE = 4240016;
    public static final int COVERT_EDGE_ATT = 4241018;

    // HEXA Boosts
    public static final int HEXA_SHADOW_ASSAULT_BOOST = 500004040;
    public static final int HEXA_SHADOW_ASSAULT_6 = 500061026;
    public static final int HEXA_SHADOW_ASSAULT_5 = 500061025;

    public List<Integer> drops = new ArrayList<>();

    public Shadower(Char chr) {
        super(chr);
    }

    @Override
    public void update(long now) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (chr.hasSkill(SHADOW_ASSAULT)) {
            if ((tsm.hasStat(Shadower_ShadowAssault) && !chr.hasSkillOnCooldown(SHADOW_ASSAULT))
                    || !tsm.hasStat(Shadower_ShadowAssault)) {
                int max = 4;
                if (chr.hasSkill(HEXA_SHADOW_ASSAULT_BOOST)) {
                    max = 5;
                }
                Option o = tsm.getOption(Shadower_ShadowAssault);
                if (o.nOption < max) {
                    o.nOption = max;
                }
                o.rOption = HEXA_SHADOW_ASSAULT_5;
                tsm.sendStat(Shadower_ShadowAssault, o);
            }
        }
        super.update(now);
    }

    @Override
    public boolean isHandlerOfJob(short id) {
        return JobConstants.isShadower(id);
    }

    private void incrementFlipTheCoinStack(TemporaryStatManager tsm) {
        EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
        Option o1 = new Option();
        Option o2 = new Option();
        Option o3 = new Option();
        int skillID = FLIP_THE_COIN;
        SkillInfo si = SkillData.getSkillInfoById(skillID);
        int amount = 1;
        if (tsm.hasStat(FlipTheCoin)) {
            amount = tsm.getOption(FlipTheCoin).nOption;
            if (amount < si.getValue(y, 1)) {
                amount++;
            }
        }
        o1.nOption = amount;
        o1.rOption = skillID;
        o1.tOption = si.getValue(time, 1);
        newStats.put(FlipTheCoin, o1);
        o2.nOption = (amount * si.getValue(x, 1));
        o2.rOption = skillID;
        o2.tOption = si.getValue(time, 1);
        newStats.put(CriticalBuff, o2);
        o3.nReason = skillID;
        o3.nValue = (amount * si.getValue(indieDamR, 1));
        o3.tTerm = si.getValue(time, 1);
        newStats.put(IndieDamR, o3);
        tsm.sendStat(newStats);
    }

    private void activateFlipTheCoin(TemporaryStatManager tsm) {    //TODO  Change to proc on Critical Hits
        if (tsm.getOption(FlipTheCoin).nOption < 5) {
            if (Util.succeedProp(50)) { //Proc on Crit<<<
                c.write(WvsContext.flipTheCoinEnabled((byte) 1));
            }
        }
    }

    @Override
    public void handleCancelTimer(Char chr) {
        super.handleCancelTimer(chr);
    }

    // Attack related methods ------------------------------------------------------------------------------------------
    @Override
    public void handleDebuffOnMob(Client c, Mob mob, SkillInfo si, int skillID, int slv, long damage) {
        Char chr = c.getChr();
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o1 = new Option();
        MobTemporaryStat mts = mob.getTemporaryStat();
        BurnedInfo bi;
        if (tsm.hasStat(PickPocket) && skillID != MESO_EXPLOSION_ATOM
                && skillID != ENHANCED_MESO_EXPLOSION_ATOM
                && skillID != HEXA_MESO_EXPLOSION_ATOM
                && skillID != HEXA_ENHANCED_MESO_EXPLOSION_ATOM) {
            dropFromPickPocket(mob.getPosition());
        }
        applyPassiveDoTSkillsOnMob(mob, damage);
        if (tsm.hasStat(ThiefSteal)) {
            handleSteal(mob);
        }
        switch (skillID) {
            case SUDDEN_RAID:
            case HEXA_SUDDEN_RAID:
                bi = BurnedInfo.createBurnInfo(chr, skillID, slv, damage);
                mts.createAndAddBurnedInfo(mob, bi, skillID);
                break;
            case BOOMERANG:
                if (Util.succeedProp(si.getValue(prop, slv)) && !mts.hasCurrentMobStatBySkillId(skillID) && !mob.isBoss()) {
                    o1.nOption = 1;
                    o1.rOption = skillID;
                    o1.tOption = si.getValue(time, slv);
                    mts.addStatOptions(mob, MobStat.Stun, o1);
                }
                break;
        }
        if (!JobConstants.isPhantom(chr.getJob())) {
            super.handleDebuffOnMob(c, mob, si, skillID, slv, damage);
        }
    }

    private void handleSteal(Mob mob) {
        SkillInfo si = SkillData.getSkillInfoById(STEAL);
        int slv = chr.getSkillLevel(STEAL);
        if (Util.succeedProp(si.getValue(z, slv))) {
            int itemId = si.getValue(x, slv);
            if (mob.isBoss()) {
                itemId = si.getValue(y, slv);
            }
            Item item = ItemData.getItemDeepCopy(itemId);
            Drop drop = new Drop(item.getItemId(), item);
            chr.getField().drop(drop, mob.getPosition());
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
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (hasHitMobs) {
            if (chr.hasSkill(FLIP_THE_COIN)) {
                activateFlipTheCoin(tsm);
            }
            if (chr.hasSkill(COVERT_EDGE)) {
                handleCovertEdge();
            }
        }
        Option o1 = new Option();
        Option o2 = new Option();
        switch (skillID) {
            case ASSASSINATE_FINISHER:
                if (!hasHitMobs) {
                    return;
                }
                if (chr.hasSkill(TRICKBLADE)) {
                    int count = 1;
                    int prevMobId = 0;
                    if (tsm.hasStat(Shadower_Assassination)) {
                        count = tsm.getOption(Shadower_Assassination).nOption;
                        prevMobId = tsm.getOption(Shadower_Assassination).xOption;
                    }

                    int finalPrevMobId = prevMobId;
                    boolean hitsPrevMob = attackInfo.mobAttackInfo.stream().anyMatch(mai -> mai.mobId == finalPrevMobId);
                    if (hitsPrevMob) {
                        Mob mob = (Mob) chr.getField().getLifeByObjectID(finalPrevMobId);
                        if (mob != null) {
                            count++;
                            o1.nOption = Math.min(count, 3);
                            o1.xOption = finalPrevMobId;
                        }
                    } else {
                        o1.nOption = 1;
                        for (MobAttackInfo mai : attackInfo.mobAttackInfo) {
                            Mob mob = (Mob) chr.getField().getLifeByObjectID(mai.mobId);
                            if (mob == null || mob.getHp() <= 0) {
                                continue;
                            }
                            o1.xOption = mob.getObjectId();

                            if (mob.isBoss()) { //  QoL | If attacking boss, boss will get hit.
                                break;
                            }
                        }
                    }
                }
                o1.rOption = skillID;
                o1.tOption = 10;
                tsm.sendStat(Shadower_Assassination, o1);
                break;
            case HEXA_ASSASSINATE_FINISHER:
                if (!hasHitMobs) {
                    return;
                }
                if (chr.hasSkill(TRICKBLADE)) {
                    int count = 1;
                    int prevMobId = 0;
                    if (tsm.hasStat(SixthAssassination)) {
                        count = tsm.getOption(SixthAssassination).nOption;
                        prevMobId = tsm.getOption(SixthAssassination).xOption;
                    }

                    int finalPrevMobId = prevMobId;
                    boolean hitsPrevMob = attackInfo.mobAttackInfo.stream().anyMatch(mai -> mai.mobId == finalPrevMobId);
                    if (hitsPrevMob) {
                        Mob mob = (Mob) chr.getField().getLifeByObjectID(finalPrevMobId);
                        if (mob != null) {
                            count++;
                            o1.nOption = Math.min(count, 3);
                            o1.xOption = finalPrevMobId;
                        }
                    } else {
                        o1.nOption = 1;
                        for (MobAttackInfo mai : attackInfo.mobAttackInfo) {
                            Mob mob = (Mob) chr.getField().getLifeByObjectID(mai.mobId);
                            if (mob == null || mob.getHp() <= 0) {
                                continue;
                            }
                            o1.xOption = mob.getObjectId();

                            if (mob.isBoss()) { //  QoL | If attacking boss, boss will get hit.
                                break;
                            }
                        }
                    }
                }
                o1.rOption = skillID;
                o1.tOption = 10;
                tsm.sendStat(SixthAssassination, o1);
                break;
            case SONIC_BLOW:
                if (tsm.hasStatBySkillId(skillID)) {
                    break;
                }
                EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
                o1.nValue = si.getValue(z, slv);
                o1.nReason = skillID;
                o1.tTerm = 3;
                newStats.put(IndieDamReduceR, o1);
                o2.nValue = 1;
                o2.nReason = skillID;
                o2.tTerm = 3;
                newStats.put(IndieAntiMagicShell, o2);
                tsm.sendStat(newStats);
                break;
            case COVETOUS_DARKNESS:
                if (tsm.hasStat(AuthenticDarkness)) {
                    break;
                }
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(updatableTime, slv);
                o1.setInMillis(true);
                tsm.sendStat(AuthenticDarkness, o1);
                createCovetousDarkness();
                break;
            case TRICKBLADE_FINISHER:
                tsm.removeStatsBySkill(ASSASSINATE_FINISHER);
                si = SkillData.getSkillInfoById(TRICKBLADE);
                slv = chr.getSkillLevel(TRICKBLADE);
                o1.nValue = 1;
                o1.nReason = TRICKBLADE;
                o1.tTerm = si.getValue(s, slv);
                tsm.sendStat(IndieNotDamaged, o1);
            case TRICKBLADE_MOB_ATTACK:
                si = SkillData.getSkillInfoById(TRICKBLADE);
                slv = chr.getSkillLevel(TRICKBLADE);
                chr.addSkillCooldown(TRICKBLADE, (int) (si.getValue(cooltime, slv) * 1000L));
                break;
        }
    }

    private void handleCovertEdge() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        int skillID = COVERT_EDGE;
        int slv = chr.getSkillLevel(skillID);
        SkillInfo si = SkillData.getSkillInfoById(skillID);
        if (si != null) {
            int prop = si.getValue(y, slv);
            if (Util.succeedProp(prop)) {
                if (!tsm.hasStat(SixthCovertShadowBuff)){
                    Option o = new Option();
                    o.nOption = 1;
                    o.rOption = skillID;
                    o.tOption = 10;
                    tsm.sendStat(SixthCovertShadowBuff, o);
                }
            }
        }
    }

    private void createCovetousDarkness() {
        Field field = chr.getField();
        Mob mob = Util.getRandomFromCollection(field.getMobsFiltered());
        if (mob == null) return;
        Position pMob = mob.getPosition();
        int cx = pMob.getX();
        int cy = pMob.getY() - 20;
        int halfW = 20, halfH = 12;
        Rect rect = new Rect(cx - halfW, cy - halfH, cx + halfW, cy + halfH);
        List<Position> starts = new ArrayList<>(512);
        for (Foothold fh : field.getFootholds()) {
            if (fh.isWall()) continue;
            int x1 = fh.getX1(), y1 = fh.getY1();
            int x2 = fh.getX2(), y2 = fh.getY2();
            int minX = Math.min(x1, x2);
            int maxX = Math.max(x1, x2);
            int len = maxX - minX;
            if (len < 60) continue;
            int step = 35;
            int n = Math.max(1, len / step);
            for (int i = 0; i <= n; i++) {
                int x = minX + (i * len) / n;
                int y;
                if (x1 == x2) {
                    y = Math.max(y1, y2);
                } else {
                    y = y1 + (int) (((long) (y2 - y1) * (x - x1)) / (x2 - x1));
                }
                starts.add(new Position(x, y - 15));
            }
        }
        if (starts.isEmpty()) return;
        int count = Math.min(250, starts.size());
        Collections.shuffle(starts);
        var fae = ForceAtomEnum.COVETOUS_DARKNESS;
        int mobOid = mob.getObjectId();
        for (int i = 0; i < count; i++) {
            Position startPos = starts.get(i);
            ForceAtomInfo fai = new ForceAtomInfo(
                    chr.getNewForceAtomKey(), fae.getInc(),
                    Util.getRandom(3, 5), Util.getRandom(3, 5),
                    0, 2900, Util.getCurrentTime(),
                    1, 0,
                    startPos);
            List<Integer> targetList = new ArrayList<>(1);
            targetList.add(mobOid);
            ForceAtom fa = new ForceAtom(false, chr.getId(), chr.getId(), fae,
                    true, targetList, COVETOUS_DARKNESS, List.of(fai),
                    new Rect(), 0, 0,
                    new Position(), COVETOUS_DARKNESS, new Position(), 0);
            fa.setRect(rect);
            chr.createForceAtom(fa);
        }
    }

    private void createMesoExplosionForceAtom() {
        if (!chr.hasSkill(MESO_EXPLOSION)) {
            return;
        }
        Field field = chr.getField();
        List<Integer> removedDrops = new ArrayList<>();
        List<ForceAtom> fas = new ArrayList<>();
        for (var dropOID : drops) {
            Life life = field.getLifeByObjectID(dropOID);
            if (life == null) {
                removedDrops.add(dropOID);
                continue;
            }
            if (life instanceof Drop drop) {
                if (drop.getMoneyType() == 0) {
                    continue;
                }
                List<Integer> targetList = new ArrayList<>();
                List<ForceAtomInfo> faiList = new ArrayList<>();
                Position pos = drop.getPosition();
                Rect rect = new Rect(pos.getX() - 500, pos.getY() - 500, pos.getX() + 500, pos.getY() + 500);
                var mob = Util.getRandomFromCollection(field.getMobsInRect(rect));
                if (mob == null) {
                    continue;
                }
                var fae = chr.hasSkill(HEXA_MESO_EXPLOSION) ? ForceAtomEnum.HEXA_FLYING_MESO :
                        drop.getMoneyType() == 1 ? ForceAtomEnum.FLYING_MESO : ForceAtomEnum.ENHANCE_FLYING_MESO;
                var atomType = drop.getMoneyType() == 7 ? HEXA_ENHANCED_MESO_EXPLOSION_ATOM :
                        drop.getMoneyType() == 6 ? HEXA_MESO_EXPLOSION_ATOM :
                        drop.getMoneyType() == 4 ? ENHANCED_MESO_EXPLOSION_ATOM : MESO_EXPLOSION_ATOM;
                ForceAtomInfo forceAtomInfo = new ForceAtomInfo(chr.getNewForceAtomKey(), fae.getInc(), 2, 3,
                        0, 0, Util.getCurrentTime(), 1, 0,
                        drop.getPosition());
                targetList.add(mob.getObjectId());
                faiList.add(forceAtomInfo);
                removedDrops.add(dropOID);
                var fa = new ForceAtom(false, chr.getId(), chr.getId(), fae,
                        true, targetList, atomType, faiList, new Rect(), 0, 300,
                        mob.getPosition(), atomType, mob.getPosition(), 0);
                fas.add(fa);
            } else {
                removedDrops.add(dropOID);
            }
        }
        for (var fa : fas) {
            chr.createForceAtom(fa);
        }
        for (var dropOID : removedDrops) {
            field.removeDrop(dropOID, 0, false, 0);
        }
        drops.removeAll(removedDrops);
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o = tsm.getOption(PickPocket);
        o.nOption = 1;
        o.xOption = drops.size();
        tsm.sendStat(PickPocket, o);
    }

    private void applyPassiveDoTSkillsOnMob(Mob mob, long damage) {
        MobTemporaryStat mts = mob.getTemporaryStat();
        //Shadower
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

    public void dropFromPickPocket(Position postion) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Field field = chr.getField();
        Skill skill = chr.getSkill(PICK_POCKET);
        SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
        int slv = skill.getCurrentLevel();
        int proc = si.getValue(prop, slv) + (chr.hasSkill(MESO_EXPLOSION_ENHANCE) ? 10 : 0);
        if (Util.succeedProp(proc)) {
            int maxMesoCount = 15 + (chr.hasSkill(MESO_EXPLOSION_ENHANCE) ? 5 : 0);
            if (drops.size() >= maxMesoCount) {
                return;
            }
            Option o = tsm.getOption(PickPocket);
            o.nOption = 1;
            o.xOption = drops.size() + 1;
            tsm.sendStat(PickPocket, o);

            Drop drop = new Drop(-1, 1);
            drop.setByPickPocket(true);
            if (o.rOption == HEXA_ENHANCED_MESO_EXPLOSION) {
                drop.setMoneyType((byte) 6);
            } else if (o.rOption == HEXA_MESO_EXPLOSION) {
                drop.setMoneyType((byte) 7);
            } else if (o.rOption == BLOOD_MONEY) {
                drop.setMoneyType((byte) 4);
            } else {
                drop.setMoneyType((byte) 1);
            }
            drop.setOwnerID(chr.getId());
            postion = new Position(postion.getX() + Util.getRandom(-50, 50), postion.getY());
            field.drop(drop, postion);
            drops.add(drop.getObjectId());
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
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o1 = new Option();
        switch (skillID) {
            case STEAL:
                if (tsm.hasStat(ThiefSteal)) {
                    tsm.removeStatsBySkill(skillID);
                } else {
                    o1.nOption = 1;
                    o1.rOption = skillID;
                    tsm.sendStat(ThiefSteal, o1);
                }
                break;
            case SHADOW_PARTNER:
                o1.nOption = si.getValue(x, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(ShadowPartner, o1);
                break;
            case PICK_POCKET:
                if (tsm.hasStat(PickPocket)) {
                    tsm.removeStat(PickPocket);
                } else {
                    o1.nOption = si.getValue(x, slv);
                    o1.rOption = chr.hasSkill(HEXA_MESO_EXPLOSION) ? HEXA_MESO_EXPLOSION : skillID;
                    tsm.sendStat(PickPocket, o1);
                }
                drops.clear();
                break;
            case BLOOD_MONEY:
                if (tsm.hasStat(PickPocket)) {
                    tsm.removeStat(PickPocket);
                } else {
                    o1.nOption = si.getValue(x, slv);
                    o1.rOption = chr.hasSkill(HEXA_MESO_EXPLOSION) ? HEXA_ENHANCED_MESO_EXPLOSION : skillID;
                    tsm.sendStat(PickPocket, o1);
                }
                drops.clear();
                break;
            case MESO_EXPLOSION:
            case ENHANCED_MESO_EXPLOSION:
            case HEXA_MESO_EXPLOSION:
            case HEXA_ENHANCED_MESO_EXPLOSION:
                createMesoExplosionForceAtom();
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
            case FLIP_THE_COIN:
                incrementFlipTheCoinStack(tsm);
                c.write(WvsContext.flipTheCoinEnabled((byte) 0));
                break;
            case SHADOW_ASSAULT:
            case SHADOW_ASSAULT_2:
            case SHADOW_ASSAULT_3:
            case SHADOW_ASSAULT_4:
            case HEXA_SHADOW_ASSAULT_5:
            case HEXA_SHADOW_ASSAULT_6:
                o1 = tsm.getOption(Shadower_ShadowAssault);
                o1.nOption = Math.max(o1.nOption - 1, 0);
                o1.rOption = skillID;
                tsm.sendStat(Shadower_ShadowAssault, o1);
                break;
            case SMOKE_SCREEN:
                AffectedArea aa = AffectedArea.getPassiveAA(chr, skillID, slv);
                aa.setMobOrigin((byte) 0);
                aa.setPosition(chr.getPosition());
                aa.setRect(aa.getPosition().getRectAround(si.getRects().getFirst()));
                aa.setDelay((short) 4);
                chr.getField().spawnAffectedArea(aa);
                break;
            case HEROS_WILL:
                tsm.removeAllDebuffs();
                break;
            case COVETOUS_DARKNESS_AA:
                tsm.removeStat(AuthenticDarkness);
                break;
        }
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
            case HEXA_SHADOW_ASSAULT_5 -> {
                int skillID = SHADOW_ASSAULT;
                int slv = chr.getSkillLevel(skillID);
                chr.setSkillCooldown(skillID, slv);
                return 1;
            }
            case HEXA_PULVERIZ_FINISHER -> {
                chr.addSkillCooldown(HEXA_PULVERIZE, 10000);
                return 1;
            }
            case HEXA_SHADOW_VEIL -> {
                int skillID = SHADOW_VEIL;
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
        }
        return super.handleSetCoolDownSkill(skillId);
    }
}
