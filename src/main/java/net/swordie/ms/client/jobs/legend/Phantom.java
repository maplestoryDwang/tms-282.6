package net.swordie.ms.client.jobs.legend;

import it.unimi.dsi.fastutil.ints.Int2LongMap;
import net.swordie.ms.ServerConfig;
import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.CharacterStat;
import net.swordie.ms.client.character.info.HitInfo;
import net.swordie.ms.client.character.items.BodyPart;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.client.character.skills.ForceAtom;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.Skill;
import net.swordie.ms.client.character.skills.info.*;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.client.jobs.Job;
import net.swordie.ms.client.jobs.adventurer.archer.Archer;
import net.swordie.ms.client.jobs.adventurer.archer.BowMaster;
import net.swordie.ms.client.jobs.adventurer.archer.Marksman;
import net.swordie.ms.client.jobs.adventurer.archer.Pathfinder;
import net.swordie.ms.client.jobs.adventurer.magician.Bishop;
import net.swordie.ms.client.jobs.adventurer.magician.FirePoison;
import net.swordie.ms.client.jobs.adventurer.magician.IceLightning;
import net.swordie.ms.client.jobs.adventurer.magician.Magician;
import net.swordie.ms.client.jobs.adventurer.pirate.Buccaneer;
import net.swordie.ms.client.jobs.adventurer.pirate.Cannoneer;
import net.swordie.ms.client.jobs.adventurer.pirate.Corsair;
import net.swordie.ms.client.jobs.adventurer.pirate.Pirate;
import net.swordie.ms.client.jobs.adventurer.thief.DualBlade;
import net.swordie.ms.client.jobs.adventurer.thief.NightLord;
import net.swordie.ms.client.jobs.adventurer.thief.Shadower;
import net.swordie.ms.client.jobs.adventurer.thief.Thief;
import net.swordie.ms.client.jobs.adventurer.warrior.DarkKnight;
import net.swordie.ms.client.jobs.adventurer.warrior.Hero;
import net.swordie.ms.client.jobs.adventurer.warrior.Paladin;
import net.swordie.ms.client.jobs.adventurer.warrior.Warrior;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.*;
import net.swordie.ms.constants.FieldConstants;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.enums.ForceAtomEnum;
import net.swordie.ms.life.AffectedArea;
import net.swordie.ms.life.Life;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.life.mob.MobStat;
import net.swordie.ms.life.mob.MobTemporaryStat;
import net.swordie.ms.loaders.ItemData;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.scripts.ScriptManagerImpl;
import net.swordie.ms.util.Position;
import net.swordie.ms.util.Rect;
import net.swordie.ms.util.Util;
import net.swordie.ms.world.field.Field;
import net.swordie.ms.world.field.fieldeffect.FieldEffect;
import org.python.modules.math;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static net.swordie.ms.client.character.skills.SkillStat.*;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.EVA;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.PAD;
import static net.swordie.ms.enums.InvType.EQUIPPED;
import static net.swordie.ms.life.mob.MobStat.*;

public class Phantom extends Job {

    public static final int TO_THE_SKIES = 20031203;
    public static final int SHROUD_WALK = 20031205;
    public static final int DEXTEROUS_TRAINING = 20030206;
    public static final int SKILL_SWIPE = 20031207;
    public static final int LOADOUT = 20031208;
    public static final int JUDGMENT_DRAW_1 = 20031209;
    public static final int JUDGMENT_DRAW_2 = 20031210;
    public static final int JUDGMENT_DRAW_AUT_MANUAL = 20031260;
    public static final int GHOSTWALK = 20031211;

    public static final int IMPECCABLE_MEMORY_I = 24001001;

    public static final int IMPECCABLE_MEMORY_II = 24101001;
    public static final int CANE_BOOSTER = 24101005; //Buff
    public static final int CARTE_BLANCHE = 24100003;
    public static final int MILLE_CARTES = 24101002;

    public static final int IMPECCABLE_MEMORY_III = 24111001;
    public static final int FINAL_FEINT = 24111002; //Buff (Unlimited Duration) Gone upon Death
    public static final int BAD_LUCK_WARD = 24111003; //Buff
    public static final int CLAIR_DE_LUNE = 24111005; //Buff

    public static final int IMPECCABLE_MEMORY_IV = 24121001;
    public static final int PRIERE_DARIA = 24121004; //Buff
    public static final int VOL_DAME = 24121007; // Special Buff
    public static final int MAPLE_WARRIOR_PH = 24121008; //Buff
    public static final int CARTE_NOIR = 24120002;              //80001890
    public static final int HEROS_WILL_PH = 24121009;
    public static final int PENOMBRE = 24121003;
    public static final int MILLE_AIGUILLES = 24121000;
    public static final int TEMPEST = 24121005;

    public static final int HEROIC_MEMORIES_PH = 24121053;
    public static final int CARTE_ROSE_FINALE = 24121052;
    public static final int CARTE_ATOM = 80001890;

    // V skill
    public static final int LUCK_OF_THE_DRAW = 400041009;
    public static final int LUCK_OF_THE_DRAW_ATOM = 400041010;
    public static final int LUCK_OF_THE_DRAW_RED = 400041011;
    public static final int LUCK_OF_THE_DRAW_TREE = 400041012;
    public static final int LUCK_OF_THE_DRAW_HOUR = 400041013;
    public static final int LUCK_OF_THE_DRAW_SHARP = 400041014;
    public static final int LUCK_OF_THE_DRAW_FLURRY = 400041015;
    public static final int ACE_IN_THE_HOLE = 400041022;
    public static final int ACE_IN_THE_HOLE_ATOM = 400041023;
    public static final int ACE_IN_THE_HOLE_FINISHER = 400041024;
    public static final int PHANTOMS_MARK = 400041040;
    public static final int PHANTOMS_MARK_2 = 400041045;
    public static final int PHANTOMS_MARK_3 = 400041046;
    public static final int RIFT_BREAK_TELEPORT = 400041055; // hold key arrow
    public static final int RIFT_BREAK_ALL_ATTACKS_IN_ONE = 400041056;

    // HEXA skills
    public static final int HEXA_TEMPEST = 24141000;
    public static final int HEXA_MILLE_AIGUILLES = 24141001;
    public static final int HEXA_MILLE_AIGUILLES_FORTUNE = 24141002;
    public static final int FATE_SHUFFLE = 24141003;
    public static final int HEXA_ROSE_CARTE_FINALE = 24141006;
    public static final int HEXA_ROSE_CARTE_FINALE_TILE = 24141006;
    public static final int LA_MORT_CARTE = 24141008;

    private final int[] addedSkills = new int[]{
            SKILL_SWIPE,
            LOADOUT,
            TO_THE_SKIES,
            SHROUD_WALK,
            JUDGMENT_DRAW_1,
            JUDGMENT_DRAW_AUT_MANUAL,
            DEXTEROUS_TRAINING,};

    private boolean isCaneSkill(int skillId) {
        return skillId == 24001000 || skillId == 24101000 || skillId == 24101002 || skillId == 24111000
                || skillId == 24111006 || skillId == 24121010 || skillId == MILLE_AIGUILLES || skillId == HEXA_MILLE_AIGUILLES;
    }

    private byte cardAmount;
    private byte phantomMarkMille = 0;
    private Set<Job> stealJobHandlers = new HashSet<>();

    public Phantom(Char chr) {
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
        // Warrior
        stealJobHandlers.add(new Hero(chr));
        stealJobHandlers.add(new Paladin(chr));
        stealJobHandlers.add(new DarkKnight(chr));

        // Mage
        stealJobHandlers.add(new FirePoison(chr));
        stealJobHandlers.add(new IceLightning(chr));
        stealJobHandlers.add(new Bishop(chr));

        // Bowman
        stealJobHandlers.add(new BowMaster(chr));
        stealJobHandlers.add(new Marksman(chr));
        stealJobHandlers.add(new Pathfinder(chr));

        // Thief
        stealJobHandlers.add(new NightLord(chr));
        stealJobHandlers.add(new Shadower(chr));
        stealJobHandlers.add(new DualBlade(chr));

        // Pirate
        stealJobHandlers.add(new Buccaneer(chr));
        stealJobHandlers.add(new Corsair(chr));
        stealJobHandlers.add(new Cannoneer(chr));
    }

    public static void reviveByFinalFeint(Char chr) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Skill skill = chr.getSkill(FINAL_FEINT);
        SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
        chr.heal(si.getValue(x, (byte) skill.getCurrentLevel()) * chr.getMaxHP(), true);
        tsm.removeStatsBySkill(skill.getSkillId());
        chr.chatMessage("你被幸運幻影技能復活。");
        chr.write(UserPacket.effect(Effect.skillSpecial(skill.getSkillId())));
        chr.getField().broadcast(UserRemote.effect(chr.getId(), Effect.skillSpecial(skill.getSkillId())), chr);
    }

    @Override
    public boolean isHandlerOfJob(short id) {
        return JobConstants.isPhantom(id);
    }

    private void giveJudgmentDrawBuff(int skillId) {

        Skill skill = chr.getSkill(skillId);
        SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
        int slv = skill.getCurrentLevel();

        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o = new Option();
        int randomInt = new Random().nextInt((skillId == JUDGMENT_DRAW_1 ? 2 : 5)) + 1;
        int xOpt = 0;
        switch (randomInt) {
            case 1: // Crit Rate
                xOpt = si.getValue(v, slv);
                break;
            case 2: // Item Drop Rate
                xOpt = si.getValue(w, slv);
                break;
            case 3: // AsrR & TerR
                xOpt = si.getValue(x, slv);
                break;
            case 4: // Defense %
                xOpt = 10;
                break;
            case 5: // Life Drain
                xOpt = 1;
                break;
        }
        chr.write(UserPacket.effect(Effect.avatarOriented("Skill/2003.img/skill/20031210/affected/" + (randomInt - 1))));
        chr.getField().broadcast(UserRemote.effect(chr.getId(), Effect.avatarOriented("Skill/2003.img/skill/20031210/affected/" + (randomInt - 1))), chr);

        o.nOption = randomInt;
        o.rOption = skill.getSkillId();
        o.tOption = si.getValue(time, slv);
        o.xOption = xOpt;
        tsm.sendStat(Judgement, o);
    }

    // Attack related methods ------------------------------------------------------------------------------------------
    @Override
    public void handleDebuffOnMob(Client c, Mob mob, SkillInfo si, int skillID, int slv, long damage) {
        for (Job jobHandler : stealJobHandlers) {
            jobHandler.handleDebuffOnMob(c, mob, si, skillID, slv, damage);
        }
        super.handleDebuffOnMob(c, mob, si, skillID, slv, damage);
    }

    @Override
    public void handleAttack(Client c, AttackInfo attackInfo, SkillInfo si, long now) {
        for (Job jobHandler : stealJobHandlers) {
            jobHandler.handleAttack(c, attackInfo, si, now);
        }
        Char chr = c.getChr();
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        int skillID = attackInfo.skillId;
        boolean hasHitMobs = !attackInfo.mobAttackInfo.isEmpty();
        int slv = attackInfo.slv;
        if (hasHitMobs
                && skillID != CARTE_NOIR
                && skillID != CARTE_BLANCHE
                && skillID != LUCK_OF_THE_DRAW_ATOM) {
            for (int i = 0; i < attackInfo.mobCount; i++) {
                cartDeck();
                createCarteForceAtom(attackInfo);
            }
            drainLifeByJudgmentDraw();
            if (chr.hasSkill(PHANTOMS_MARK) && isCaneSkill(skillID)) {
                setPhantomMarkOnMob(attackInfo);
            }
        }
        Option o1 = new Option();
        switch (skillID) {
            case CARTE_ROSE_FINALE:
            case HEXA_ROSE_CARTE_FINALE:
                for (MobAttackInfo mai : attackInfo.mobAttackInfo) {
                    Mob mob = (Mob) chr.getField().getLifeByObjectID(mai.mobId);
                    if (mob == null || mob.getHp() <= 0) {
                        continue;
                    }
                    AffectedArea aa = AffectedArea.getAffectedArea(chr, attackInfo);
                    aa.setMobOrigin((byte) 1);
                    aa.setMob(mob);
                    aa.setPosition(mob.getPosition());
                    aa.setDelay((short) 13);
                    aa.setRect(aa.getPosition().getRectAround(si.getRects().getFirst()));
                    chr.getField().spawnAffectedArea(aa);
                }
                break;
            case PHANTOMS_MARK:
            case PHANTOMS_MARK_2:
            case PHANTOMS_MARK_3:
                EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
                o1.nOption = 1;
                o1.rOption = PHANTOMS_MARK;
                o1.tOption = 3;
                newStats.put(IndieNotDamaged, o1);
                newStats.put(DarkSight, o1.deepCopy());
                tsm.sendStat(newStats);
                break;
        }
        super.handleAttack(c, attackInfo, si, now);
    }

    private void setPhantomMarkOnMob(AttackInfo attackInfo) {
        Option o1 = new Option();
        Option o2 = new Option();
        SkillInfo si = SkillData.getSkillInfoById(PHANTOMS_MARK);
        int slv = chr.getSkillLevel(PHANTOMS_MARK);

        int count = 1;
        int prevMobId = 0;
        if (attackInfo.skillId == MILLE_AIGUILLES || attackInfo.skillId == HEXA_MILLE_AIGUILLES) {
            if (phantomMarkMille < si.getValue(y, slv)) {
                phantomMarkMille++;
                return;
            }
            phantomMarkMille = 0;
        }
        EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (tsm.hasStat(PhantomMarkOfPhantomTarget)) {
            count = tsm.getOption(PhantomMarkOfPhantomTarget).nOption;
            prevMobId = tsm.getOption(PhantomMarkOfPhantomTarget).xOption;
        }

        int finalPrevMobId = prevMobId;
        boolean hitsPrevMob = attackInfo.mobAttackInfo.stream().anyMatch(mai -> mai.mobId == finalPrevMobId);
        Map<Integer, Life> lifes = chr.getField().getLifes();
        if (hitsPrevMob) {
            Mob mob = (Mob) lifes.get(finalPrevMobId);
            if (mob != null) {
                count++;
                o1.nOption = Math.min(count, si.getValue(s2, slv));
                o1.xOption = finalPrevMobId;
            }
        } else {
            o1.nOption = 1;
            for (MobAttackInfo mai : attackInfo.mobAttackInfo) {
                Mob mob = (Mob) chr.getField().getLifeByObjectID(mai.mobId);
                if (mob == null || mob.getHp() <= 0) {
                    continue;
                }
                if (mob.isBoss()) { //  QoL | If attacking boss, boss will get hit.
                    o1.xOption = mob.getObjectId();
                } else {
                    o1.xOption = mob.getObjectId();
                }
                break;
            }
        }
        o1.rOption = si.getSkillId();
        o1.tOption = si.getValue(time, slv);
        newStats.put(PhantomMarkOfPhantomTarget, o1);
        o2.nOption = 1;
        o2.rOption = si.getSkillId();
        o2.xOption = (tsm.hasStat(PhantomMarkOfPhantomOwner) ? (Math.min(tsm.getOption(PhantomMarkOfPhantomOwner).xOption + 1, si.getValue(q, slv))) : 1);
        newStats.put(PhantomMarkOfPhantomOwner, o2);
        tsm.sendStat(newStats);
    }

    private void createCarteForceAtom(AttackInfo attackInfo) {
        if (chr.hasSkill(CARTE_BLANCHE)) {
            final var now = Util.getCurrentTime();
            for (Mob mob : chr.getField().getMobs(attackInfo.mobAttackInfo)) {
                if (mob == null) {
                    continue;
                }
                var angle = new Random().nextInt(30) + 295;
                if (chr.hasSkill(CARTE_NOIR)) {
                    int mobID = mob.getObjectId();
                    ForceAtomEnum fae = ForceAtomEnum.PHANTOM_CARD_2;
                    ForceAtomInfo forceAtomInfo = new ForceAtomInfo(chr.getNewForceAtomKey(), fae.getInc(), 20, 35,
                            angle, 0, now, 0, 0, new Position()); //Slightly behind the player
                    chr.createForceAtom(new ForceAtom(false, 0, chr.getId(), fae,
                            true, mobID, CARTE_NOIR, forceAtomInfo, new Rect(), 0, 300,
                            mob.getPosition(), CARTE_NOIR, mob.getPosition(), 0));
                } else {
                    int mobID = mob.getObjectId();
                    ForceAtomEnum fae = ForceAtomEnum.PHANTOM_CARD_1;
                    ForceAtomInfo forceAtomInfo = new ForceAtomInfo(chr.getNewForceAtomKey(), fae.getInc(), 20, 40,
                            angle, 0, now, 0, 0, new Position()); //Slightly behind the player
                    chr.createForceAtom(new ForceAtom(false, 0, chr.getId(), fae,
                            true, mobID, CARTE_BLANCHE, forceAtomInfo, new Rect(), 0, 300,
                            mob.getPosition(), CARTE_BLANCHE, mob.getPosition(), 0));
                }
            }
        }
    }

    private void createCarteForceAtomByJudgmentDraw() {
        if (chr.hasSkill(CARTE_BLANCHE)) {
            SkillInfo si = SkillData.getSkillInfoById(CARTE_BLANCHE);
            Rect rect = new Rect(
                    new Position(
                            chr.getPosition().getX() - 450,
                            chr.getPosition().getY() - 450),
                    new Position(
                            chr.getPosition().getX() + 450,
                            chr.getPosition().getY() + 450)
            );
            List<Mob> mobs = chr.getField().getMobsInRect(rect);
            if (mobs.size() <= 0) {
                chr.dispose();
                return;
            }
            Mob mob = Util.getRandomFromCollection(mobs);

            for (int i = 0; i < 10; i++) {
                if (chr.hasSkill(CARTE_NOIR)) {
                    int mobID = mob.getObjectId();
                    ForceAtomEnum fae = ForceAtomEnum.PHANTOM_CARD_2;
                    ForceAtomInfo forceAtomInfo = new ForceAtomInfo(chr.getNewForceAtomKey(), fae.getInc(), 20, 35,
                            350 - (2 * i), i * 5, Util.getCurrentTime(), 1, 0,
                            new Position()); //Slightly behind the player
                    chr.createForceAtom(new ForceAtom(false, 0, chr.getId(), fae,
                            true, mobID, CARTE_NOIR, forceAtomInfo, new Rect(), 0, 300,
                            mob.getPosition(), CARTE_NOIR, mob.getPosition(), 0));
                } else if (chr.hasSkill(CARTE_BLANCHE)) {
                    int mobID = mob.getObjectId();
                    ForceAtomEnum fae = ForceAtomEnum.PHANTOM_CARD_1;
                    ForceAtomInfo forceAtomInfo = new ForceAtomInfo(chr.getNewForceAtomKey(), fae.getInc(), 20, 40,
                            350 - (2 * i), i * 5, Util.getCurrentTime(), 1, 0,
                            new Position()); //Slightly behind the player
                    chr.createForceAtom(new ForceAtom(false, 0, chr.getId(), fae,
                            true, mobID, CARTE_BLANCHE, forceAtomInfo, new Rect(), 0, 300,
                            mob.getPosition(), CARTE_BLANCHE, mob.getPosition(), 0));
                }
            }
        }
    }

    private void drainLifeByJudgmentDraw() {
        if (!chr.hasSkill(JUDGMENT_DRAW_2)) {
            return;
        }
        Skill skill = chr.getSkill(JUDGMENT_DRAW_2);
        SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
        int slv = skill.getCurrentLevel();
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (tsm.hasStat(Judgement) && tsm.getOption(Judgement).nOption == 5) {
            int healRate = si.getValue(z, slv);
            chr.heal((int) (chr.getMaxHP() * ((double) healRate / 100)));
        }
    }

    private int getMaxCards() {
        int num = 0;
        if (chr.hasSkill(JUDGMENT_DRAW_1)) {
            num = 20;
        }
        if (chr.hasSkill(JUDGMENT_DRAW_2)) {
            num = 40;
        }
        return num;
    }

    private void resetCardStack() {
        setCardAmount((byte) 0);
    }

    public byte getCardAmount() {
        return cardAmount;
    }

    public void setCardAmount(byte cardAmount) {
        this.cardAmount = cardAmount;
        c.write(UserLocal.incJudgementStack(getCardAmount()));
    }

    private void cartDeck() {
        if (getCardAmount() < getMaxCards()) {
            setCardAmount((byte) (getCardAmount() + 1));
        }
    }

    public void createLuckOfTheDrawForceAtom() {
        Skill skill = chr.getSkill(LUCK_OF_THE_DRAW);
        SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
        int slv = skill.getCurrentLevel();

        //Rect rect = chr.getRectAround(si.getFirstRect());
        Rect rect = chr.getRectAround(new Rect(-700, -400, 0, 40));
        if (!chr.isLeft()) {
            rect = rect.horizontalFlipAround(chr.getPosition().getX());
        }
        List<Integer> targetList = new ArrayList<>();
        List<ForceAtomInfo> faiList = new ArrayList<>();
        ForceAtomEnum fae = ForceAtomEnum.PHANTOM_CARD_2;
        for (int i = 0; i < 14; i++) {
            Mob mob = Util.getRandomFromCollection(chr.getField().getMobsInRect(rect));
            int fImpact = new Random().nextInt(15) + 15;
            int sImpact = new Random().nextInt(5) + 8;
            int angle = new Random().nextInt(60) + 295;
            int fullR = new Random().nextInt(360);
            int radius = new Random().nextInt(20) + 70;
            ForceAtomInfo forceAtomInfo = new ForceAtomInfo(chr.getNewForceAtomKey(), fae.getInc(), fImpact, sImpact,
                    angle, 30 * i, Util.getCurrentTime(), 0, 0,
                    new Position((int) (radius * math.cos(fullR)), (int) (radius * math.sin(fullR))));
            faiList.add(forceAtomInfo);
            targetList.add(mob != null ? mob.getObjectId() : 0);
        }
        chr.createForceAtom(new ForceAtom(false, 0, chr.getId(), fae,
                true, targetList, LUCK_OF_THE_DRAW_ATOM, faiList, rect, 0, 0,
                new Position(), LUCK_OF_THE_DRAW_ATOM, new Position(), 0));
        chr.setSkillCooldown(LUCK_OF_THE_DRAW, slv);
    }

    private void drawCardByLuckOfTheDraw() {
        List<Integer> randomCardSkillId = Arrays.asList(
                LUCK_OF_THE_DRAW_RED,
                LUCK_OF_THE_DRAW_TREE,
                LUCK_OF_THE_DRAW_HOUR,
                LUCK_OF_THE_DRAW_SHARP,
                LUCK_OF_THE_DRAW_FLURRY
        );

        int drawnCard = Util.getRandomFromCollection(randomCardSkillId);
        Option o1 = new Option();
        Option o2 = new Option();
        Option o3 = new Option();
        Option o4 = new Option();
        Option o5 = new Option();
        SkillInfo si = SkillData.getSkillInfoById(drawnCard);
        int slv = chr.getSkillLevel(LUCK_OF_THE_DRAW);
        int duration = SkillData.getSkillInfoById(LUCK_OF_THE_DRAW).getValue(dotInterval, slv);
        List<Char> partyChrList = new ArrayList<>();
        if (chr.getParty() != null) {
            partyChrList.addAll(chr.getParty().getPartyMembersInSameFieldWithChr(chr));
        } else {
            partyChrList.add(chr);
        }
        for (Char partyChr : partyChrList) {
            EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
            TemporaryStatManager ptyTsm = partyChr.getTemporaryStatManager();
            switch (drawnCard) {
                case LUCK_OF_THE_DRAW_RED:
                    o1.nValue = si.getValue(indieMhpR, slv);
                    o1.nReason = drawnCard;
                    o1.tTerm = duration;
                    newStats.put(IndieMHPR, o1);
                    healOTByLuckOfTheDraw(ptyTsm, si, slv);
                    break;
                case LUCK_OF_THE_DRAW_TREE:
                    o1.nOption = si.getValue(z, slv);
                    o1.rOption = drawnCard;
                    o1.tOption = duration;
                    newStats.put(DamageReduce, o1);
                    o2.nValue = si.getValue(indieAsrR, slv);
                    o2.nReason = drawnCard;
                    o2.tTerm = duration;
                    newStats.put(IndieAsrR, o2);
                    break;
                case LUCK_OF_THE_DRAW_HOUR:
                    for (Int2LongMap.Entry e : partyChr.getSkillCoolTimes().int2LongEntrySet()) {
                        int cdSkillId = e.getIntKey();
                        si = SkillData.getSkillInfoById(cdSkillId);
                        if (si != null) {
                            partyChr.reduceSkillCoolTime(cdSkillId, (long) (chr.getRemainingCoolTime(cdSkillId) * ((double) si.getValue(x, slv) / 100)));
                        }
                    }
                    break;
                case LUCK_OF_THE_DRAW_SHARP:
                    o1.nValue = si.getValue(indiePMdR, slv);
                    o1.nReason = drawnCard;
                    o1.tTerm = duration;
                    newStats.put(IndiePMdR, o1);
                    break;
                case LUCK_OF_THE_DRAW_FLURRY:
                    o1.nValue = si.getValue(indieMhpR, slv);
                    o1.nReason = drawnCard;
                    o1.tTerm = duration;
                    newStats.put(IndieMHPR, o1);
                    o2.nOption = si.getValue(z, slv);
                    o2.rOption = drawnCard;
                    o2.tOption = duration;
                    newStats.put(DamageReduce, o2);
                    o3.nValue = si.getValue(indieAsrR, slv);
                    o3.nReason = drawnCard;
                    o3.tTerm = duration;
                    newStats.put(IndieAsrR, o3);
                    o4.nValue = si.getValue(indiePMdR, slv);
                    o4.nReason = drawnCard;
                    o4.tTerm = duration;
                    newStats.put(IndiePMdR, o4);
                    for (Int2LongMap.Entry e : partyChr.getSkillCoolTimes().int2LongEntrySet()) {
                        int cdSkillId = e.getIntKey();
                        si = SkillData.getSkillInfoById(cdSkillId);
                        if (si != null) {
                            partyChr.reduceSkillCoolTime(cdSkillId, (long) (chr.getRemainingCoolTime(cdSkillId) * ((double) si.getValue(x, slv) / 100)));
                        }
                    }
                    break;
            }
            if (chr == partyChr) {
                o5.nValue = 1;
                o5.nReason = LUCK_OF_THE_DRAW;
                o5.tTerm = 2;
                newStats.put(IndieNotDamaged, o5);
            }
            ptyTsm.sendStat(newStats);
        }
        for (int skill : Arrays.asList(LUCK_OF_THE_DRAW, drawnCard)) {
            chr.write(FieldPacket.fieldEffect(FieldEffect.getOffFieldEffectFromWz(String.format("Skill/40004.img/skill/%d/screen", skill), 0)));
            chr.getField().broadcast(UserRemote.effect(chr.getId(), Effect.avatarOriented(String.format("Skill/40004.img/skill/%d/screen", skill))), chr);
        }
    }

    private void healOTByLuckOfTheDraw(TemporaryStatManager tsm, SkillInfo si, int slv) {
        if (tsm.getOptByCTSAndSkill(IndieMHPR, LUCK_OF_THE_DRAW_RED) != null) {
            int healR = (int) ((double) (chr.getMaxHP() * si.getValue(y, slv)) / 100F);
            tsm.getChr().heal(healR);
            tsm.getChr().healMP(healR);

            chr.getTimer().addEvent(() -> healOTByLuckOfTheDraw(tsm, si, slv), 2, TimeUnit.SECONDS);
        }
    }

    private void createAceInTheHoleForceAtom() {
        Field field = chr.getField();
        if (!chr.hasSkill(ACE_IN_THE_HOLE)) {
            return;
        }
        Skill skill = chr.getSkill(ACE_IN_THE_HOLE);
        SkillInfo si = SkillData.getSkillInfoById(ACE_IN_THE_HOLE);
        int slv = skill.getCurrentLevel();
        ForceAtomEnum fae = ForceAtomEnum.ACE_IN_THE_HOLE;

        Mob mob = Util.getRandomFromCollection(field.getMobs());
        ForceAtomInfo forceAtomInfo = new ForceAtomInfo(chr.getNewForceAtomKey(), fae.getInc(), 4, 4,
                270, 500, Util.getCurrentTime(), 1, 0,
                new Position());
        ForceAtom fa = new ForceAtom(false, 0, chr.getId(), fae,
                true, mob == null || mob.getHp() <= 0 ? 0 : mob.getObjectId(), ACE_IN_THE_HOLE_ATOM, forceAtomInfo, chr.getPosition().getRectAround(si.getFirstRect()), 0, 300,
                chr.getPosition(), 0, mob == null || mob.getHp() <= 0 ? new Position() : mob.getPosition(), 0);
        fa.setMaxRecreationCount(si.getValue(z, slv));
        chr.createForceAtom(fa);
    }

    @Override
    public void handleForceAtomCollision(int faKey, int skillId, int mobObjId, Position position, InPacket inPacket) {
        ForceAtom fa = chr.getForceAtomByKey(faKey);
        if (fa != null && fa.getCurRecreationCount(faKey) >= fa.getMaxRecreationCount(faKey) && fa.getSkillId() == ACE_IN_THE_HOLE_ATOM) {
            chr.write(UserLocal.aceInTheHoleFinisher(ACE_IN_THE_HOLE_FINISHER, chr.getSkillLevel(ACE_IN_THE_HOLE), position));
        }
        super.handleForceAtomCollision(faKey, skillId, mobObjId, position, inPacket);
    }

    // Skill related methods -------------------------------------------------------------------------------------------
    @Override
    public void handleSkill(Client c, InPacket inPacket, SkillUseInfo skillUseInfo) {
        super.handleSkill(c, inPacket, skillUseInfo);
        for (Job jobHandler : stealJobHandlers) {
            jobHandler.handleSkill(c, inPacket, skillUseInfo);
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
        Option o5 = new Option();
        switch (skillID) {
            case GHOSTWALK:
                o1.nOption = si.getValue(x, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(DarkSight, o1);
                break;
            case FINAL_FEINT:
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(CharacterTemporaryStat.ReviveOnce, o1);
                break;
            case BAD_LUCK_WARD:
                o1.nValue = si.getValue(indieMhpR, slv);
                o1.nReason = skillID;
                o1.tTerm = si.getValue(time, slv);
                newStats.put(IndieMHPR, o1);
                o2.nValue = si.getValue(indieMmpR, slv);
                o2.nReason = skillID;
                o2.tTerm = si.getValue(time, slv);
                newStats.put(IndieMMPR, o2);
                o3.nOption = si.getValue(x, slv);
                o3.rOption = skillID;
                o3.tOption = si.getValue(time, slv);
                newStats.put(AsrR, o3);
                o4.nOption = si.getValue(y, slv);
                o4.rOption = skillID;
                o4.tOption = si.getValue(time, slv);
                newStats.put(TerR, o4);
                tsm.sendStat(newStats);
                break;
            case PRIERE_DARIA:
                o1.nReason = skillID;
                o1.nValue = si.getValue(indieDamR, slv);
                o1.tTerm = si.getValue(time, slv);
                newStats.put(IndieDamR, o1);
                o2.nReason = skillID;
                o2.nValue = si.getValue(indieIgnoreMobpdpR, slv);
                o2.tTerm = si.getValue(time, slv);
                newStats.put(IndieIgnoreMobpdpR, o2);
                tsm.sendStat(newStats);
                break;
            case HEROIC_MEMORIES_PH:
                o1.nReason = skillID;
                o1.nValue = si.getValue(indieDamR, slv);
                o1.tTerm = si.getValue(time, slv);
                tsm.sendStat(IndieDamR, o1);
                break;
            case LUCK_OF_THE_DRAW_RED:
                o1.nReason = skillID;
                o1.nValue = si.getValue(indieMhpR, slv);
                o1.tTerm = si.getValue(time, slv);
                tsm.sendStat(IndieMHPR, o1);
                break;
            case LUCK_OF_THE_DRAW_TREE:
                o1.nReason = skillID;
                o1.nValue = si.getValue(indieAsrR, slv);
                o1.tTerm = si.getValue(time, slv);
                newStats.put(IndieAsrR, o1);
                o2.rOption = skillID;
                o2.nOption = si.getValue(z, slv);
                o2.tOption = si.getValue(time, slv);
                newStats.put(IgnoreMobDamR, o2);
                tsm.sendStat(newStats);
                break;
            case LUCK_OF_THE_DRAW_HOUR:
                o1.nReason = skillID;
                o1.nValue = 200 + slv * 4;
                o1.tTerm = si.getValue(time, slv);
                tsm.sendStat(IndieCooltimeReduce, o1); //Indie
                break;
            case LUCK_OF_THE_DRAW_SHARP:
                o1.nReason = skillID;
                o1.nValue = si.getValue(indiePMdR, slv);
                o1.tTerm = si.getValue(time, slv);
                tsm.sendStat(IndiePMdR, o1);
                break;
            case LUCK_OF_THE_DRAW_FLURRY:
                o1.nReason = skillID;
                o1.nValue = si.getValue(indieMhpR, slv);
                o1.tTerm = si.getValue(time, slv);
                newStats.put(IndieMHPR, o1);
                o2.nReason = skillID;
                o2.nValue = si.getValue(indieAsrR, slv);
                o2.tTerm = si.getValue(time, slv);
                newStats.put(IndieAsrR, o2);
                o3.nReason = skillID;
                o3.nValue = si.getValue(indiePMdR, slv);
                o3.tTerm = si.getValue(time, slv);
                newStats.put(IndiePMdR, o3);
                o4.nOption = si.getValue(z, slv);
                o4.rOption = skillID;
                o4.tOption = si.getValue(time, slv);
                newStats.put(IgnoreMobDamR, o4);
                o5.nReason = skillID;
                o5.nValue = 200 + slv * 4;
                o5.tTerm = si.getValue(time, slv);
                newStats.put(IndieCooltimeReduce, o5); //Indie
                tsm.sendStat(newStats);
                break;
            case ACE_IN_THE_HOLE:
                createAceInTheHoleForceAtom();
                break;
            case SHROUD_WALK:
                o1.nOption = si.getValue(z, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(Invisible, o1);
                break;
            case VOL_DAME:
                stealBuffVolDame();
                break;
            case TO_THE_SKIES:
                Field toField = chr.getOrCreateFieldByCurrentInstanceType(si.getValue(x, slv));
                chr.warp(toField);
                break;
            case JUDGMENT_DRAW_1:
            case JUDGMENT_DRAW_2:
                createCarteForceAtomByJudgmentDraw();
                giveJudgmentDrawBuff(skillID);
                resetCardStack();
                break;
            case HEROS_WILL_PH:
                tsm.removeAllDebuffs();
                break;
            case PENOMBRE:
                o1.nValue = si.getValue(y, slv);
                o1.nReason = skillID;
                o1.tTerm = si.getValue(time, slv);
                tsm.sendStat(IndieIgnoreMobpdpR, o1);
                break;
            case CLAIR_DE_LUNE:
                o1.nValue = si.getValue(indiePad, slv);
                o1.nReason = skillID;
                o1.tTerm = si.getValue(time, slv);
                newStats.put(IndiePAD, o1);
                o2.nValue = si.getValue(indieAcc, slv);
                o2.nReason = skillID;
                o2.tTerm = si.getValue(time, slv);
                newStats.put(IndieACC, o2);
                tsm.sendStat(newStats);
                break;
        }
    }

    private void stealBuffVolDame() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o1 = new Option();

        if (!chr.hasSkill(VOL_DAME)) {
            return;
        }

        Skill skill = chr.getSkill(VOL_DAME);
        SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
        int slv = skill.getCurrentLevel();

        Rect rect = new Rect( //NPE when using the skill's rect
                new Position(
                        chr.getPosition().getX() - 250,
                        chr.getPosition().getY() - 250),
                new Position(
                        chr.getPosition().getX() + 250,
                        chr.getPosition().getY() + 250)
        );
        List<Mob> mobs = chr.getField().getMobsInRect(rect);
        if (mobs.size() <= 0) {
            return;
        }
        MobStat buffFromMobStat = MobStat.TotalDamParty; //Needs to be initialised
        MobStat[] mobStats = new MobStat[]{ //Ordered from Weakest to Strongest, since  the for loop will save the last MobsStat
                PCounter, //Dmg Reflect 600%
                MCounter, //Dmg Reflect 600%
                PImmune, //Dmg Recv -40%
                MImmune, //Dmg Recv -40%
                PowerUp, //Attack +40
                MagicUp, //Attack +40
                MobStat.Invincible, //Invincible for short time
        };
        List<MobStat> removeList = new ArrayList<>();
        for (Mob mob : mobs) {
            MobTemporaryStat mts = mob.getTemporaryStat();
            for (MobStat mobStat : Arrays.stream(mobStats).toList()) {
                if (mts.hasCurrentMobStat(mobStat)) {
                    removeList.add(mobStat);
                    buffFromMobStat = mobStat;
                    break;
                }
            }
            mts.removeMobStat(mob, buffFromMobStat);
        }
        for (MobStat mobStat : removeList) {
            switch (mobStat) {
                case PCounter:
                case MCounter:
                    o1.nOption = si.getValue(y, slv);
                    o1.rOption = skill.getSkillId();
                    o1.tOption = 30;
                    tsm.sendStat(PowerGuard, o1);
                    break;
                case PImmune:
                case MImmune:
                    o1.nOption = si.getValue(x, slv);
                    o1.rOption = skill.getSkillId();
                    o1.tOption = 30;
                    tsm.sendStat(EVA, o1); //as a check to allow for DmgReduction in the Hit Handler
                    break;
                case PowerUp:
                case MagicUp:
                    o1.nOption = si.getValue(epad, slv);
                    o1.rOption = skill.getSkillId();
                    o1.tOption = 30;
                    tsm.sendStat(PAD, o1);
                    break;
                case Invincible:
                    o1.nOption = 1;
                    o1.rOption = skill.getSkillId();
                    o1.tOption = 5;
                    tsm.sendStat(NotDamaged, o1);
                    break;
            }
        }
    }

    // Hit related methods ---------------------------------------------------------------------------------------------
    @Override
    public void handleHit(Client c, InPacket inPacket, HitInfo hitInfo) {
        boolean tms282 = net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3;
        for (Job jobHandler : stealJobHandlers) {
            jobHandler.handleHit(c, inPacket, hitInfo);
        }
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (!chr.hasSkill(VOL_DAME) && !tms282) {
            return;
        }
        if (chr.hasSkill(VOL_DAME) && tsm.getOptByCTSAndSkill(EVA, VOL_DAME) != null) {
            Skill skill = chr.getSkill(VOL_DAME);
            SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
            int dmgPerc = si.getValue(x, skill.getCurrentLevel());
            int dmg = hitInfo.hpDamage;
            hitInfo.hpDamage = tms282 ? dmg - (int) ((long) dmg * dmgPerc / 100)
                    : dmg - (dmg * (dmgPerc / 100));
        }

        super.handleHit(c, inPacket, hitInfo);
    }

    @Override
    public void handleSkillRemove(Char chr, int skillID) {
        switch (skillID) {
            case TEMPEST:
            case HEXA_TEMPEST:
                Skill skill = chr.getSkill(skillID);
                if (skill != null) {
                    SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
                    int slv = skill.getCurrentLevel();
                    chr.addSkillCooldown(skillID, si.getValue(cooltime, slv) * 1000);
                }
                break;
        }
    }

    @Override
    public void setCharCreationStats(Char chr) {
        CharacterStat cs = chr.getAvatarData().getCharacterStat();
        cs.setPosMap(FieldConstants.HOME_MAP);
        cs.setLevel(10);
        cs.setStr(4);
        cs.setDex(4);
        cs.setInt(4);
        cs.setLuk(45);
        cs.setHp(1000);
        cs.setMaxHp(1000);
        if (!JobConstants.isNoManaJob(chr.getJob())) {
            cs.setMp(500);
            cs.setMaxMp(500);
        }
        chr.setStolenSkills(new HashSet<>());
        chr.setChosenSkills(new HashSet<>());
    }

    @Override
    public void handleInitAfterMigrate(Char chr) {
        if (chr.getLevel() < 30) {
            ScriptManagerImpl sm = chr.getScriptManager();
            sm.setJob(JobConstants.JobEnum.PHANTOM2.getJobId());
            sm.levelUntil(30);
            for (int qid = 25400; qid <= 25425; qid++) {
                sm.completeQuestNoRewards(qid);
            }
            sm.completeQuestNoRewards(25100);
            sm.completeQuestNoRewards(25101);
            sm.addSPJobAdv(JobConstants.JobEnum.PHANTOM1.getJobId(), 5);
            sm.addSPJobAdv(JobConstants.JobEnum.PHANTOM2.getJobId(), 3);
            sm.giveAndEquip(1362005);
            sm.giveAndEquip(1352101);
            sm.warp(FieldConstants.HOME_MAP);
        }
        super.handleInitAfterMigrate(chr);
    }

    @Override
    public void handleLevelUp(short level) {
        super.handleLevelUp(level);
        // hacks to bypass the quest glitch (accept but no packet)
        if (level >= 100 && !chr.hasSkill(JUDGMENT_DRAW_2)) {
            chr.addSkill(JUDGMENT_DRAW_2, 1, 1);
        }
        var sm = chr.getScriptManager();
        if (level == 60 || level == 100) {
            final short jobID = chr.getJob();
            if (!JobConstants.canJobAdvance(jobID)) {
                return;
            }
            final short next = JobConstants.nextJob(jobID);
            sm.setJob(next);
            if (level == 60) {
                sm.completeQuestNoRewards(25111);
                for (int qid = 25426; qid <= 25433; qid++) {
                    sm.completeQuestNoRewards(qid);
                }
                for (int qid = 25444; qid <= 25447; qid++) {
                    sm.completeQuestNoRewards(qid);
                }
            } else {
                for (int qid = 25120; qid <= 25122; qid++) {
                    sm.completeQuestNoRewards(qid);
                }
                for (int qid = 25434; qid <= 25438; qid++) {
                    sm.completeQuestNoRewards(qid);
                }
                for (int qid = 25439; qid <= 25441; qid++) {
                    sm.completeQuestNoRewards(qid);
                }
                for (int qid = 25300; qid <= 25302; qid++) {
                    sm.completeQuestNoRewards(qid);
                }
                sm.completeQuestNoRewards(25446);
                sm.completeQuestNoRewards(25448);
            }
            sm.addSPJobAdv(jobID, 5);
            sm.addSPJobAdv(next, 3);
        }
    }

    public void handleCancelKeyDownSkill(Char chr, int skillID) {
        if (skillID == LUCK_OF_THE_DRAW) {
            drawCardByLuckOfTheDraw();
        } else {
            super.handleCancelKeyDownSkill(chr, skillID);
        }
    }

    @Override
    public int handleSetCoolDownSkill(int skillId) {
        switch (skillId) {
            case HEXA_ROSE_CARTE_FINALE -> {
                chr.setSkillCooldown(CARTE_ROSE_FINALE, chr.getSkillLevel(HEXA_ROSE_CARTE_FINALE));
                return 1;
            }
            case HEXA_TEMPEST -> {
                chr.setSkillCooldown(TEMPEST, chr.getSkillLevel(HEXA_TEMPEST));
                return 1;
            }
        }
        return super.handleSetCoolDownSkill(skillId);
    }
}
