package net.swordie.ms.client.jobs.cygnus;

import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.ExtendSP;
import net.swordie.ms.client.character.info.HitInfo;
import net.swordie.ms.client.character.skills.ForceAtom;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.SecondAtom;
import net.swordie.ms.client.character.skills.Skill;
import net.swordie.ms.client.character.skills.info.*;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.Summoned;
import net.swordie.ms.connection.packet.UserLocal;
import net.swordie.ms.connection.packet.WvsContext;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.enums.*;
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

import java.util.*;
import java.util.stream.Collectors;

import static net.swordie.ms.client.character.skills.SkillStat.*;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;

public class WindArcher extends Noblesse {

    public static final int ELEMENTAL_HARMONY_DEX = 10000247;

    public static final int STORM_ELEMENTAL = 13001022; //Buff

    public static final int TRIFLING_WIND_I = 13101022; //Special Buff (Proc) (ON/OFF)
    public static final int TRIFLING_WIND_ATOM = 13100027;
    public static final int TRIFLING_WIND_ATOM_ENHANCED = 13100027;                     // Enhanced Arrow - Atom
    public static final int BOW_BOOSTER = 13101023; //Buff
    public static final int SYLVAN_AID = 13100028; //Passive

    public static final int TRIFLING_WIND_II = 13110022; //Special Buff Upgrade
    public static final int TRIFLING_WIND_II_ATOM = 13110027;
    public static final int EMERALD_FLOWER = 13111024; //Summon (Stationary, No Attack, Aggros)
    public static final int SECOND_WIND = 13110026; //
    public static final int PINPOINT_PIERCE = 13111021;

    public static final int TRIFLING_WIND_III_ENHANCED = 13120010;                      // Enhanced Arrow - Atom
    public static final int ALBATROSS_MAX = 13120008; // Passive
    public static final int TRIFLING_WIND_III = 13120003; //Special Buff Upgrade
    public static final int SHARP_EYES = 13121005; //Buff
    public static final int CALL_OF_CYGNUS_WA = 13121000; //Buff
    public static final int EMERALD_DUST = 13120007;
    public static final int SPIRALING_VORTEX = 13121002;
    public static final int SPIRALING_VORTEX_EXPLOSION = 13121009;
    public static final int TRIFLING_WIND_ENHANCE = 13120044;
    public static final int TRIFLING_WIND_DOUBLE_CHANCE = 13120045;
    public static final int SONG_OF_HEAVEN = 13121001;
    public static final int STORM_BRINGER = 13121017;

    public static final int MONSOON = 13121052;
    public static final int GLORY_OF_THE_GUARDIANS = 13121053;
    public static final int STORM_WHIM = 13121055;

    // V Skills
    public static final int HOWLING_GALE = 400031003; // takes 1 Wind Energy
    public static final int HOWLING_GALE_BIG = 400031004; // takes 2 Wind Energy
    public static final int MERCILESS_WINDS = 400031022;
    public static final int GALE_BARRIER = 400031030;
    public static final int GALE_BARRIER_ATOM = 400031031;
    public static final int VORTEX_SPHERE = 400031058;
    public static final int VORTEX_SPHERE_2 = 400031059;

    // HEXA Skills
    public static final int MISTRAL_SPRING = 13141500;
    public static final int MISTRAL_SPRING_SUMMON = 13141501;
    public static final int MISTRAL_SPRING_SA_1 = 13141502;
    public static final int MISTRAL_SPRING_SA_2 = 13141503;
    public static final int MISTRAL_SPRING_SA_3 = 13141504;
    public static final int MISTRAL_SPRING_SUMMON_MOVE = 13141505;
    public static final int HEXA_TRIFLING_WIND = 13141001; //Special Buff Upgrade
    public static final int HEXA_TRIFLING_WIND_ENHANCED = 13141002; //Special Buff Upgrade
    public static final int HEXA_STORM_BRINGER = 13141003;
    public static final int HEXA_MONSOON = 13141005;
    public static final int HEXA_STORM_WHIM = 13141006;
    public static final int ANEMOI = 13141007;
    public static final int ANEMOI_SUMMON = 13141008;

    // HEXA Boosts
    public static final int HEXA_GALE_BARRIER = 500061033;
    public static final int HEXA_GALE_BARRIER_ATOM = 500061034; // todo

    private long lastGaleBarrierFA = Long.MIN_VALUE;
    private long mistalSpring = Long.MIN_VALUE;

    private final int[] addedSkills = new int[]{
            ELEMENTAL_HARMONY_DEX
    };

    public WindArcher(Char chr) {
        super(chr);
        if (isHandlerOfJob(chr.getJob())) {
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
    public void update(long now) {
        if (chr.hasSkill(MISTRAL_SPRING) && (mistalSpring <= 0 || now - mistalSpring >= 2000L)) {
            var summon = chr.getField().getSummonBySkillId(chr, MISTRAL_SPRING_SUMMON);
            if (summon != null) {
                handleMistralSpring(now);
            }
        }
        super.update(now);
    }

    private void handleMistralSpring(long now) {
        var dataIndex = 47;
        final var mobs = chr.getField().getMobsFiltered();
        if (mobs.isEmpty()) {
            return;
        }
        for (int skillID = MISTRAL_SPRING_SA_1; skillID <= MISTRAL_SPRING_SA_3; skillID++) {
            var si = SkillData.getSkillInfoById(skillID);
            int key = 0;
            List<SecondAtom> secondAtoms = new LinkedList<>();
            for (var sai : si.getSecondAtomInfos().values()) {
                var mob = Util.getRandomFromCollection(mobs);
                var pos = chr.getPosition().add(sai.getPos());
                var sa = new SecondAtom(sai, chr.getNewSecondAtomKey(), chr.getId(), mob.getObjectId(), key,
                        skillID, pos, now);
                sa.setDataIndex(dataIndex);
                sa.setFirstAngleStart(1);
                secondAtoms.add(sa);
                key++;
            }
            chr.createSecondAtom(secondAtoms);
            dataIndex++;
        }
        mistalSpring = now;
    }

    public static Skill getEmeraldFlowerSkill(Char chr) {
        Skill skill = null;
        if (chr.hasSkill(EMERALD_FLOWER)) {
            skill = chr.getSkill(EMERALD_FLOWER);
        }
        if (chr.hasSkill(EMERALD_DUST)) {
            skill = chr.getSkill(EMERALD_DUST);
        }
        return skill;
    }

    @Override
    public boolean isHandlerOfJob(short id) {
        return JobConstants.isWindArcher(id);
    }

    // Attack related methods ------------------------------------------------------------------------------------------
    @Override
    public void handleDebuffOnMob(Client c, Mob mob, SkillInfo si, int skillID, int slv, long damage) {
        Char chr = c.getChr();
        Option o1 = new Option();
        MobTemporaryStat mts = mob.getTemporaryStat();
        switch (skillID) {
            case MONSOON:
            case HEXA_MONSOON:
                BurnedInfo bi = BurnedInfo.createBurnInfo(chr, skillID, slv, damage);
                mts.createAndAddBurnedInfo(mob, bi, skillID);
                break;
            case PINPOINT_PIERCE:
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    o1.nOption = si.getValue(x, slv);
                    o1.rOption = skillID;
                    o1.xOption = si.getValue(y, slv);
                    mts.addStatOptions(mob, MobStat.WindBreakerPinpointPierce, o1);
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
            if (skillID != TRIFLING_WIND_ATOM
                    && skillID != TRIFLING_WIND_II_ATOM
                    && skillID != TRIFLING_WIND_III
                    && skillID != TRIFLING_WIND_III_ENHANCED
                    && skillID != HEXA_TRIFLING_WIND
                    && skillID != HEXA_TRIFLING_WIND_ENHANCED
                    && skillID != ANEMOI_SUMMON
                    && skillID != HOWLING_GALE
                    && skillID != HOWLING_GALE_BIG
                    && skillID != 0
                    && skillID != 13111020) {
                int maxtrif = getMaxTriffling();
                for (int i = 0; i < maxtrif; i++) {
                    createTriflingWindForceAtom(attackInfo, now);
                }
                createStormBringerForceAtom(now);
                createStormWhim(attackInfo);
                createGaleBarrierForceAtom(now);
            }
        }
        switch (skillID) {
            case SPIRALING_VORTEX:
                List<MobAttackInfo> mai = attackInfo.mobAttackInfo;
                if (attackInfo.mobAttackInfo.size() <= 0) {
                    return;
                }
                Life life = chr.getField().getLifeByObjectID(Util.getRandomFromCollection(mai).mobId);
                if (life instanceof Mob mob) {
                    if (mob.getHp() > 0) {
                        chr.getField().broadcast(UserLocal.explosionAttack(SPIRALING_VORTEX_EXPLOSION, mob.getPosition(), mob.getObjectId(), 1));
                    }
                }
                break;
        }
    }

    private void createStormWhim(AttackInfo attackInfo) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (tsm.hasStat(StormWhim) && Util.succeedProp(30)) {
            int skillID = chr.hasSkill(HEXA_STORM_WHIM) ? HEXA_STORM_WHIM : STORM_WHIM;
            SkillInfo si = SkillData.getSkillInfoById(skillID);
            final long now = System.currentTimeMillis();
            final var mobs = chr.getField().getMobs(attackInfo.mobAttackInfo);
            if (mobs.isEmpty()) {
                return;
            }
            int key = 0;
            List<SecondAtom> secondAtoms = new LinkedList<>();
            for (var sai : si.getSecondAtomInfos().values()) {
                var mob = Util.getRandomFromCollection(mobs);
                var pos = chr.getPosition().add(sai.getPos());
                var sa = new SecondAtom(sai, chr.getNewSecondAtomKey(), chr.getId(), mob.getObjectId(), key,
                        skillID, pos, now);
                sa.setDataIndex(skillID == HEXA_STORM_WHIM ? 79 : 41);
                sa.setSpecialAtom(true);
                secondAtoms.add(sa);
                key++;
            }
            chr.createSecondAtom(secondAtoms);
        }
    }

    public void increaseWindEnergy() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        int count = 1;
        if (tsm.hasStat(HowlingGaleStack)) {
            count = tsm.getOption(HowlingGaleStack).nOption;
            if (count < 3) {
                count++;
            }
        }
        updateWindEnergy(count);
    }

    public void decreaseWindEnergy() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        int count = 0;
        if (tsm.hasStat(HowlingGaleStack)) {
            count = tsm.getOption(HowlingGaleStack).nOption;
            if (count > 0) {
                count--;
            }
        }
        updateWindEnergy(count);
    }

    public void updateWindEnergy(int energy) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o = new Option();
        o.nOption = energy;
        o.rOption = HOWLING_GALE;
        tsm.sendStat(HowlingGaleStack, o);
        chr.write(WvsContext.updateSkillStackRequestResult(HOWLING_GALE, (byte) 1));
    }

    public void diminishGaleBarrier(int elemental) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o = tsm.getOption(WindBreakerStormGuard);
        o.nOption -= elemental;
        if (o.nOption <= 0) {
            tsm.removeStat(WindBreakerStormGuard);
        } else {
            tsm.updateStat(WindBreakerStormGuard, o);
        }
    }

    private void createGaleBarrierForceAtom(long now) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (chr.hasSkill(GALE_BARRIER) && tsm.hasStat(WindBreakerStormGuard) && (lastGaleBarrierFA + 2000L < now)) {
            Skill skill = chr.getSkill(GALE_BARRIER);
            SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
            int slv = skill.getCurrentLevel();
            int atomID = chr.hasSkill(HEXA_GALE_BARRIER) ? HEXA_GALE_BARRIER_ATOM : GALE_BARRIER_ATOM;
            for (int i = 0; i < si.getValue(q2, slv); i++) {
                Rect rect = chr.getRectAround(new Rect(-350, -200, 100, 100));
                if (!chr.isLeft()) {
                    rect = rect.horizontalFlipAround(chr.getPosition().getX());
                }
                if (chr.getField().getMobsInRect(rect).size() <= 0) {
                    continue;
                }
                Mob mob = Util.getRandomFromCollection(chr.getField().getMobsInRect(rect));
                int randomfImpact = new Random().nextInt(6) + 30;
                int randomsImpact = new Random().nextInt(3) + 4;
                ForceAtomEnum fae = ForceAtomEnum.GREEN_TORNADO;
                ForceAtomInfo forceAtomInfo = new ForceAtomInfo(chr.getNewForceAtomKey(), fae.getInc(), randomfImpact, randomsImpact,
                        (int) Util.getAngleOfTwoPositions(chr.getPosition(), mob.getPosition()), 500, (int) now, 1, 0,
                        new Position());
                chr.createForceAtom(new ForceAtom(false, 0, chr.getId(), fae,
                        true, mob.getObjectId(), atomID, forceAtomInfo, new Rect(), 0, 300,
                        mob.getPosition(), atomID, mob.getPosition(), 0));
            }
            lastGaleBarrierFA = now;
        }
    }

    private void createMercilessWindForceAtom() {
        Field field = chr.getField();
        if (!chr.hasSkill(MERCILESS_WINDS)) {
            return;
        }
        Skill skill = chr.getSkill(MERCILESS_WINDS);
        SkillInfo si = SkillData.getSkillInfoById(MERCILESS_WINDS);
        int slv = skill.getCurrentLevel();
        int forceAtomCount = si.getValue(x, slv);
        ForceAtomEnum fae = ForceAtomEnum.MERCILESS_WINDS;
        for (int i = 0; i < forceAtomCount; i++) {
            int angle = (360 / forceAtomCount) * i;
            int circleRadii = 150;
            int vTranslation = (int) (Math.sin(angle) * circleRadii);
            int hTranslation = (int) (Math.cos(angle) * circleRadii);
            Mob mob = Util.getRandomFromCollection(field.getMobs());
            ForceAtomInfo forceAtomInfo = new ForceAtomInfo(chr.getNewForceAtomKey(), fae.getInc(), 35, 5,
                    angle, 500, Util.getCurrentTime(), 1, 0,
                    new Position(chr.getPosition().getX() + hTranslation, chr.getPosition().getY() + vTranslation));
            chr.createForceAtom(new ForceAtom(false, 0, chr.getId(), fae,
                    true, mob == null || mob.getHp() <= 0 ? 0 : mob.getObjectId(), MERCILESS_WINDS, forceAtomInfo, new Rect(), 0, 300,
                    chr.getPosition(), MERCILESS_WINDS, mob == null || mob.getHp() <= 0 ? new Position() : mob.getPosition(), 0));
        }
    }

    private void createTriflingWindForceAtom(AttackInfo attackInfo, long now) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Skill skill = getTriflingWindSkill();
        if (skill == null || !Util.succeedProp(getTriflingWindProp()) || !tsm.hasStat(TriflingWhimOnOff)) {
            return;
        }
        Field field = chr.getField();
        var mobs = field.getMobs(attackInfo.mobAttackInfo);
        if (mobs.isEmpty()) {
            return;
        }
        var mob = Util.getRandomFromCollection(mobs);
        int fImpact = new Random().nextInt(10) + 35; // 36
        int sImpact = 3;
        final var subProp = getTriflingWindSubProp();
        final var successSub = Util.succeedProp(subProp);
        final var isHexaTriflingWind = chr.hasSkill(HEXA_TRIFLING_WIND);
        ForceAtomEnum fae = successSub ?
                (isHexaTriflingWind ? ForceAtomEnum.WA_ARROW_HEXA_2 : ForceAtomEnum.WA_ARROW_2) :
                (isHexaTriflingWind ? ForceAtomEnum.WA_ARROW_HEXA_1 : ForceAtomEnum.WA_ARROW_1);
        int skillId;
        switch (skill.getSkillId()) {
            case HEXA_TRIFLING_WIND:
                skillId = successSub ? HEXA_TRIFLING_WIND_ENHANCED : skill.getSkillId();
                break;
            case TRIFLING_WIND_III:
                skillId = successSub ? TRIFLING_WIND_III_ENHANCED : skill.getSkillId();
                break;
            case TRIFLING_WIND_II:
                skillId = successSub ? skill.getSkillId() : TRIFLING_WIND_II_ATOM;
                break;
            default:
                skillId = successSub ? TRIFLING_WIND_ATOM_ENHANCED : skill.getSkillId();
                break;
        }
        for (int i = 0; i < (chr.hasSkill(TRIFLING_WIND_DOUBLE_CHANCE) ? 2 : 1); i++) {
            ForceAtomInfo fai = new ForceAtomInfo(chr.getNewForceAtomKey(), fae.getInc(), fImpact, sImpact,
                    new Random().nextBoolean() ? 180 : 0, 0, (int) now, 0, 0,
                    new Position(30, 0));
            ForceAtom fa = new ForceAtom(false, 0, chr.getId(), fae,
                    true, mob.getObjectId(), skillId, fai, new Rect(), 0, 0,
                    new Position(), 0, new Position(), 0);
            chr.createForceAtom(fa);
        }
    }

    private void createStormBringerForceAtom(long now) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (tsm.hasStat(StormBringer)) {
            int skillID = chr.hasSkill(HEXA_STORM_BRINGER) ? HEXA_STORM_BRINGER : STORM_BRINGER;
            SkillInfo si = SkillData.getSkillInfoById(skillID);
            int hyperprop = si.getValue(prop, 1);
            if (Util.succeedProp(hyperprop)) {
                Position pos = chr.getPosition();
                Rect rect = new Rect(pos.getX() - 300, pos.getY() - 300, pos.getX() + 300, pos.getY() + 300);
                Mob mob = Util.getRandomFromCollection(chr.getField().getMobsInRect(rect));
                if (mob != null && mob.getHp() > 0) {
                    int ranY = new Random().nextInt(150) - 100;
                    ForceAtomEnum fae = skillID == HEXA_STORM_BRINGER ?
                            ForceAtomEnum.WA_ARROW_HYPER_HEXA :
                            ForceAtomEnum.WA_ARROW_HYPER;
                    ForceAtomInfo forceAtomInfo = new ForceAtomInfo(chr.getNewForceAtomKey(), fae.getInc(), 5, 5,
                            270, 0, (int) now, 1, 0,
                            new Position(35, ranY)); //Slightly behind the player
                    chr.createForceAtom(new ForceAtom(false, 0, chr.getId(), fae,
                            true, mob.getObjectId(), skillID, forceAtomInfo, new Rect(), 0, 300,
                            mob.getPosition(), skillID, mob.getPosition(), 0));
                }
            }
        }
    }

    private Skill getTriflingWindSkill() {
        Skill skill = null;
        if (chr.hasSkill(TRIFLING_WIND_I)) {
            skill = chr.getSkill(TRIFLING_WIND_I);
        }
        if (chr.hasSkill(TRIFLING_WIND_II)) {
            skill = chr.getSkill(TRIFLING_WIND_II);
        }
        if (chr.hasSkill(TRIFLING_WIND_III)) {
            skill = chr.getSkill(TRIFLING_WIND_III);
        }
        if (chr.hasSkill(HEXA_TRIFLING_WIND)) {
            skill = chr.getSkill(HEXA_TRIFLING_WIND);
        }
        return skill;
    }

    private int getTriflingWindProp() {
        Skill skill = getTriflingWindSkill();
        if (skill != null) {
            SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
            int slv = skill.getCurrentLevel();

            return si.getValue(prop, slv) + (chr.hasSkill(TRIFLING_WIND_ENHANCE) ? 10 : 0);
        }
        return 0;
    }

    private int getTriflingWindSubProp() {
        Skill skill = getTriflingWindSkill();
        if (skill != null) {
            SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
            int slv = skill.getCurrentLevel();

            return si.getValue(subProp, slv);
        }
        return 0;
    }

    private int getMaxTriffling() {
        Skill skill = getTriflingWindSkill();
        if (skill != null) {
            SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
            int slv = skill.getCurrentLevel();

            return si.getValue(x, slv);
        }
        return 0;
    }

    public void applyEmeraldFlowerDebuffToMob(Summon summon, int mobTemplateId) {
        Skill skill = getEmeraldFlowerSkill(chr);
        if (skill == null) {
            return;
        }
        SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
        int slv = skill.getCurrentLevel();

        List<Mob> mobListWithTemplateId = chr.getField()
                .getMobsInRect(summon.getPosition().getRectAround(si.getRects().get(0)))
                .stream()
                .filter(mob -> mob.getTemplateId() == mobTemplateId)
                .toList();
        Option o = new Option();
        o.nOption = si.getValue(z, slv);
        o.rOption = skill.getSkillId();
        o.tOption = si.getValue(time, slv);
        for (Mob mob : mobListWithTemplateId) {
            MobTemporaryStat mts = mob.getTemporaryStat();
            if (!mts.hasCurrentMobStatBySkillId(skill.getSkillId())) {
                mts.addStatOptions(mob, MobStat.Speed, o.deepCopy());
            }
        }
    }

    public void applyEmeraldDustDebuffToMob(Summon summon, int mobTemplateId) {
        Skill skill = getEmeraldFlowerSkill(chr);
        if (skill == null) {
            return;
        }
        SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
        int slv = skill.getCurrentLevel();

        List<Mob> mobListWithTemplateId = chr.getField()
                .getMobsInRect(summon.getPosition().getRectAround(si.getRects().get(0)))
                .stream()
                .filter(mob -> mob.getTemplateId() == mobTemplateId)
                .collect(Collectors.toList());
        Option o = new Option();
        o.nOption = si.getValue(w, slv);
        o.rOption = skill.getSkillId();
        o.tOption = si.getValue(time, slv);
        for (Mob mob : mobListWithTemplateId) {
            MobTemporaryStat mts = mob.getTemporaryStat();
            if (!mts.hasCurrentMobStatBySkillId(skill.getSkillId())) {
                mts.addStatOptions(mob, MobStat.PDR, o.deepCopy());
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
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Field field;
        Summon summon;
        Option o1 = new Option();
        switch (skillID) {
            case STORM_ELEMENTAL:
                if (tsm.hasStatBySkillId(skillID)) {
                    tsm.removeStatsBySkill(skillID);
                } else {
                    o1.nReason = skillID;
                    o1.nValue = si.getValue(indieDamR, slv);
                    tsm.sendStat(IndieDamR, o1); //Indie
                }
                break;
            case SHARP_EYES: // x = crit rate    y = max crit dmg
                int cr = si.getValue(x, slv);
                int crDmg = si.getValue(y, slv);
                o1.nOption = (cr << 8) + crDmg;
                if (net.swordie.ms.connection.protocol.Protocol.active().version() == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) {
                    o1.xOption = cr;
                    o1.yOption = crDmg;
                }
                o1.nValue = si.getValue(y, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(SharpEyes, o1);
                break;
            case TRIFLING_WIND_I:
                if (tsm.hasStat(TriflingWhimOnOff)) {
                    tsm.removeStat(TriflingWhimOnOff);
                } else {
                    o1.nOption = 1;
                    o1.rOption = getTriflingWindSkill().getSkillId();
                    tsm.sendStat(TriflingWhimOnOff, o1);
                }
                break;
            case EMERALD_FLOWER:
            case EMERALD_DUST:
                Position position = new Position(chr.isLeft() ? chr.getPosition().getX() - 250 : chr.getPosition().getX() + 250, chr.getPosition().getY());
                if (chr.getField().findFootHoldBelow(position) != null) {
                    summon = Summon.getSummonByAndSetStat(c.getChr(), skillID, slv);
                    field = c.getChr().getField();
                    summon.setFlyMob(false);
                    summon.setMoveAction((byte) 0);
                    summon.setMoveAbility(MoveAbility.Stop);
                    summon.setCurFoothold((short) (int) java.util.Optional.ofNullable(chr.getField().findFootHoldBelow(position)).map(below -> below.getId()).orElse(0) /* in the air: no foothold (was an NPE) */);
                    summon.setPosition(position);
                    summon.setAttackActive(false);
                    summon.setAssistType(AssistType.None);
                    summon.setMaxHP(si.getValue(x, slv));
                    summon.setHp(summon.getMaxHP());
                    field.spawnSummon(summon);
                } else {
                    chr.chatMessage("請換個位置再使用這個技能。");
                }
                break;
            case GLORY_OF_THE_GUARDIANS:
                o1.nReason = skillID;
                o1.nValue = si.getValue(indieDamR, slv);
                o1.tTerm = si.getValue(time, slv);
                tsm.sendStat(IndieDamR, o1);
                break;
            case STORM_BRINGER:
            case HEXA_STORM_BRINGER:
                o1.nOption = slv;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(StormBringer, o1);
                break;
            case STORM_WHIM:
            case HEXA_STORM_WHIM:
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(StormWhim, o1);
                break;
            case MISTRAL_SPRING_SUMMON:
            case MISTRAL_SPRING_SUMMON_MOVE:
                summon = Summon.getSummonBy(chr, MISTRAL_SPRING_SUMMON, chr.getSkillLevel(MISTRAL_SPRING));
                summon.setFlyMob(false);
                summon.setMoveAction((byte) 4);
                summon.setAssistType(AssistType.ExplosionAttack);
                summon.setMoveAbility(MoveAbility.Stop);
                chr.getField().spawnSummon(summon);
                break;
            case ANEMOI_SUMMON:
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, chr.getSkillLevel(ANEMOI));
                tsm.sendStat(Anemoi, o1);
                summon = Summon.getSummonByAndSetStat(chr, skillID, chr.getSkillLevel(ANEMOI));
                summon.setFlyMob(false);
                summon.setMoveAction((byte) 4);
                summon.setAssistType(AssistType.Attack);
                summon.setMoveAbility(MoveAbility.Walk);
                chr.getField().spawnSummon(summon);
                break;
            case GALE_BARRIER:
            case HEXA_GALE_BARRIER:
                o1.nOption = si.getValue(w, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                o1.startTime = System.currentTimeMillis();
                tsm.sendStat(WindBreakerStormGuard, o1);
                break;
            case MERCILESS_WINDS:
                createMercilessWindForceAtom();
                break;
            case HOWLING_GALE:
                decreaseWindEnergy();
                break;
            case HOWLING_GALE_BIG:
                decreaseWindEnergy();
                decreaseWindEnergy();
                break;
        }
    }

    // Hit related methods ---------------------------------------------------------------------------------------------
    @Override
    public void handleHit(Client c, InPacket inPacket, HitInfo hitInfo) {
        int skillID = SECOND_WIND;
        if (chr.hasSkill(skillID) && !chr.hasSkillOnCooldown(skillID)) {
            TemporaryStatManager tsm = chr.getTemporaryStatManager();
            SkillInfo si = SkillData.getSkillInfoById(skillID);
            int slv = chr.getSkillLevel(skillID);
            Option o1 = new Option();
            o1.nValue = si.getValue(indiePad, slv);
            o1.nReason = skillID;
            o1.tTerm = si.getValue(time, slv);
            tsm.sendStat(IndiePAD, o1);
            chr.setSkillCooldown(skillID, slv);
        }

        super.handleHit(c, inPacket, hitInfo);
    }

    @Override
    public int handleSetCoolDownSkill(int skillId) {
        switch (skillId) {
            case HEXA_STORM_BRINGER -> {
                int skillID = STORM_BRINGER;
                int slv = chr.getSkillLevel(skillID);
                chr.setSkillCooldown(skillID, slv);
                return 1;
            }
            case HEXA_MONSOON -> {
                int skillID = MONSOON;
                int slv = chr.getSkillLevel(skillID);
                chr.setSkillCooldown(skillID, slv);
                return 1;
            }
            case HEXA_STORM_WHIM -> {
                int skillID = STORM_WHIM;
                int slv = chr.getSkillLevel(skillID);
                chr.setSkillCooldown(skillID, slv);
                return 1;
            }
        }
        return super.handleSetCoolDownSkill(skillId);
    }

    @Override
    public void handleJobAdvance() {
        ScriptManagerImpl sm = chr.getScriptManager();
        if (chr.getJob() == JobConstants.JobEnum.WINDARCHER1.getJobId()) {
            if (chr.getLevel() < 30) {
                sm.sendSayOkay("#e這個職業需要達到#r30#k等以上才能轉職");
                return;
            }
            if (sm.sendAskYesNo("#e要跳過轉職任務嗎？")) {
                if (sm.getEmptyInventorySlots(1) < 1) {
                    sm.sendSayOkay("#e請在裝備欄空出更多空間。");
                    return;
                }
                sm.jobAdvance(JobConstants.JobEnum.WINDARCHER2.getJobId());
                sm.giveItem(1142067);
                sm.completeQuestNoRewards(20870);
                sm.completeQuestNoRewards(20873);
            }
        } else if (chr.getJob() == JobConstants.JobEnum.WINDARCHER2.getJobId()) {
            if (chr.getLevel() < 60) {
                sm.sendSayOkay("#e這個職業需要達到#r60#k等以上才能轉職");
                return;
            }
            if (sm.sendAskYesNo("#e要跳過轉職任務嗎？")) {
                if (sm.getEmptyInventorySlots(1) < 1) {
                    sm.sendSayOkay("#e請在裝備欄空出更多空間。");
                    return;
                }
                sm.jobAdvance(JobConstants.JobEnum.WINDARCHER3.getJobId());
                sm.giveItem(1142068);
                sm.completeQuestNoRewards(20880);
                sm.completeQuestNoRewards(20881);
                sm.completeQuestNoRewards(20882);
                sm.completeQuestNoRewards(20883);
            }
        } else if (chr.getJob() == JobConstants.JobEnum.WINDARCHER3.getJobId()) {
            if (chr.getLevel() < 100) {
                sm.sendSayOkay("#e這個職業需要達到#r100#k等以上才能轉職");
                return;
            }
            if (sm.sendAskYesNo("#e要跳過轉職任務嗎？")) {
                if (sm.getEmptyInventorySlots(1) < 1) {
                    sm.sendSayOkay("#e請在裝備欄空出更多空間。");
                    return;
                }
                sm.jobAdvance(JobConstants.JobEnum.WINDARCHER4.getJobId());
                sm.giveItem(1142069);
                sm.completeQuestNoRewards(20890);
                sm.completeQuestNoRewards(20891);
                sm.completeQuestNoRewards(20892);
                sm.completeQuestNoRewards(20893);
                sm.completeQuestNoRewards(20894);
                sm.completeQuestNoRewards(20895);
            }
        } else {
            sm.sendSayOkay("#e目前的狀態無法轉職。");
        }
    }
}
