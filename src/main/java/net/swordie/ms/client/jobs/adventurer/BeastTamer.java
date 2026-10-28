package net.swordie.ms.client.jobs.adventurer;

import net.swordie.ms.ServerConfig;
import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.CharacterStat;
import net.swordie.ms.client.character.info.HitInfo;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.Skill;
import net.swordie.ms.client.character.skills.SkillStat;
import net.swordie.ms.client.character.skills.TownPortal;
import net.swordie.ms.client.character.skills.info.AttackInfo;
import net.swordie.ms.client.character.skills.info.MobAttackInfo;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.client.character.skills.info.SkillUseInfo;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.client.jobs.Job;
import net.swordie.ms.client.jobs.adventurer.magician.Bishop;
import net.swordie.ms.client.social.Party.Party;
import net.swordie.ms.client.social.Party.PartyMember;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.*;
import net.swordie.ms.constants.FieldConstants;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.enums.ChatType;
import net.swordie.ms.enums.MoveAbility;
import net.swordie.ms.life.AffectedArea;
import net.swordie.ms.life.Life;
import net.swordie.ms.life.Summon;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.life.mob.MobStat;
import net.swordie.ms.life.mob.MobTemporaryStat;
import net.swordie.ms.life.mob.skill.BurnedInfo;
import net.swordie.ms.loaders.FieldData;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.scripts.ScriptManagerImpl;
import net.swordie.ms.util.Position;
import net.swordie.ms.util.Rect;
import net.swordie.ms.util.Util;
import net.swordie.ms.world.field.Field;

import java.util.*;

import static net.swordie.ms.client.character.skills.SkillStat.*;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;

public class BeastTamer extends Job {

    //Common
    public static final int GUARDIAN_LEAP = 110001506;
    public static final int CRITTER_SELECT = 110001510;
    public static final int HOMEWARD_BOUND = 110001514;
    public static final int MAPLE_GUARDIAN = 110001511;
    public static final int BEAR_MODE = 110001501;
    public static final int SNOW_LEOPARD_MODE = 110001502;
    public static final int HAWK_MODE = 110001503;
    public static final int CAT_MODE = 110001504;

    public static final int BEAST_SCEPTER_MASTERY = 110000515;
    public static final int GROWTH_SPURT = 110000513;
    public static final int BEASTLY_RESOLVE = 110001512;

    //Bear Mode
    public static final int LIL_FORT = 112001007;
    public static final int FORT_FOLLOW_UP = 112000015;
    public static final int MAJESTIC_TRUMPET = 112001006;
    public static final int BEAR_REBORN = 112000016;
    public static final int BEAR_ASSAULT = 112001009;
    public static final int FISHY_SLAP = 112001008;

    //Snow Leopard Mode
    public static final int BRO_ATTACK = 112101016;
    public static final int THUNDER_DASH = 112101007;
    public static final int ADV_THUNDER_DASH = 112100012;
    public static final int THUNDER_TRAIL = 112100008; //tile

    //Hawk Mode
    public static final int EKA_EXPRESS = 112111010;    //Door skill
    public static final int FLY = 112111000;
    public static final int HAWK_FLOCK = 112111007;
    public static final int RAPTOR_TALONS = 112111006;
    public static final int BIRDS_EYE_VIEW = 112111009;
    public static final int RAZOR_BEAK = 112111008;
    public static final int REGROUP = 112111011;    //Warp Party to player
    public static final int DEFENSIVE_FORMATION = 112110005;
    public static final int TORNADO_FLIGHT = 112001016;

    //Cat Mode
    public static final int MEOW_HEAL = 112121013;
    public static final int PURR_ZONE = 112121005; // Special Skill
    public static final int MEOW_CARD = 112121006; // Meow Card
    public static final int MEOW_CARD_RED = 112121007; //Red
    public static final int MEOW_CARD_BLUE = 112121008; //Blue
    public static final int MEOW_CARD_GREEN = 112121009; //Green
    public static final int MEOW_CARD_GOLD = 112121020; //112120009;    //Gold
    public static final int MEOW_CARD_GOLD_SKILL = 112120019; // If chr has the Gold Card Skill
    public static final int FIRE_KITTY = 112121004;
    public static final int CATS_CRADLE_BLITZKRIEG = 112121057; // Special Skill (like PURR_ZONE)
    public static final int KITTY_BATTLE_SQUAD = 112120021;
    public static final int KITTY_TREATS = 112120023;
    public static final int STICKY_PAWS = 112120017;
    public static final int CAT_CLAWS = 112120018;
    public static final int MOUSERS_INSIGHT = 112120022;
    public static final int FRIENDS_OF_ARBY = 112120016;
    public static final int MEOW_CURE = 112121010;
    public static final int MEOW_REVIVE = 112121011;

    //Hyper
    public static final int TEAM_ROAR = 112121056;

    //V skills
    public static final int CHAMP_CHARGE = 400021019;
    public static final int CHAMP_CHARGE_LEOPARD = 400021024;
    public static final int CHAMP_CHARGE_BEAR = 400021035;
    public static final int CHAMP_CHARGE_BIRD = 400021037;
    public static final int CHAMP_CHARGE_MEOW = 400021039;

    private static final HashMap<Integer, int[]> buffsByMode;
    private static final int[] bearBuffs = new int[]{
            BEAR_ASSAULT,};
    private static final int[] leopardBuffs = new int[]{
            BRO_ATTACK,};
    private static final int[] hawkBuffs = new int[]{
            HAWK_FLOCK,
            RAPTOR_TALONS,
            BIRDS_EYE_VIEW,
            RAZOR_BEAK,};
    private static final int[] catBuffs = new int[]{
            MEOW_CARD,
            MEOW_CARD_RED,
            MEOW_CARD_BLUE,
            MEOW_CARD_GREEN,
            MEOW_CARD_GOLD,
            MEOW_CARD_GOLD_SKILL,
            KITTY_BATTLE_SQUAD,
            KITTY_TREATS,
            STICKY_PAWS,
            CAT_CLAWS,
            MOUSERS_INSIGHT,
            FRIENDS_OF_ARBY,};

    static {
        buffsByMode = new HashMap<>();
        buffsByMode.put(BEAR_MODE, bearBuffs);
        buffsByMode.put(SNOW_LEOPARD_MODE, leopardBuffs);
        buffsByMode.put(HAWK_MODE, hawkBuffs);
        buffsByMode.put(CAT_MODE, catBuffs);
    }

    private final int[] buffs = new int[]{
            MAPLE_GUARDIAN,
            BEAR_MODE,
            SNOW_LEOPARD_MODE,
            HAWK_MODE,
            CAT_MODE,
            LIL_FORT,
            BEAR_ASSAULT,
            BRO_ATTACK,
            FLY,
            HAWK_FLOCK,
            RAPTOR_TALONS,
            BIRDS_EYE_VIEW,
            RAZOR_BEAK,
            DEFENSIVE_FORMATION,
            MEOW_CARD,
            MEOW_CARD_RED,
            MEOW_CARD_BLUE,
            MEOW_CARD_GREEN,
            MEOW_CARD_GOLD,
            MEOW_CARD_GOLD_SKILL,
            KITTY_BATTLE_SQUAD,
            KITTY_TREATS,
            STICKY_PAWS,
            CAT_CLAWS,
            MOUSERS_INSIGHT,
            FRIENDS_OF_ARBY,
            TEAM_ROAR,};
    private final int[] addedSkills = new int[]{
            GUARDIAN_LEAP,
            CRITTER_SELECT,
            BEAR_MODE,
            SNOW_LEOPARD_MODE,
            HAWK_MODE,
            CAT_MODE,
            HOMEWARD_BOUND,};
    private final int[] cards = new int[]{
            MEOW_CARD_RED,
            MEOW_CARD_GREEN,
            MEOW_CARD_BLUE,
            MEOW_CARD_GOLD
    };
    private int fortFollowUpAddAttack = 0;
    private Summon defensiveFormation;

    public BeastTamer(Char chr) {
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
            if (chr.getJob() != 11212) {
                chr.setJob(11212);
            }
            List<Skill> updateSkills = new ArrayList<>();
            for (int i = 11200; i <= 11212; i++) {
                if (i == 11200 || i == 11210 || i == 11211 || i == 11212) {
                    for (Skill skill : SkillData.getSkillsByJob((short) i)) {
                        if (chr.getSkillLevel(skill.getSkillId()) == 0) {
                            skill.setCurrentLevel(skill.getMaxLevel());
                            skill.setMasterLevel(skill.getMaxLevel());
                            chr.addSkill(skill);
                            updateSkills.add(skill);
                        }
                    }
                }
            }
            if (chr.getSkillLevel(BEAST_SCEPTER_MASTERY) == 0) {
                chr.addSkill(BEAST_SCEPTER_MASTERY, 10, 10);
            }
            if (chr.getSkillLevel(GROWTH_SPURT) == 0) {
                chr.addSkill(GROWTH_SPURT, 30, 30);
            }
            if (chr.getSkillLevel(MAPLE_GUARDIAN) == 0) {
                chr.addSkill(MAPLE_GUARDIAN, 30, 30);
            }
            if (chr.getSkillLevel(BEASTLY_RESOLVE) == 0) {
                chr.addSkill(BEASTLY_RESOLVE, 5, 5);
            }
            chr.write(WvsContext.changeSkillRecordResult(updateSkills, true, false, false));
        }
    }

    public static void beastTamerRegroup(Char chr) { //Handled in WorldHandler
        Party party = chr.getParty();
        if (party != null) {
            for (PartyMember pm : party.getOnlineMembers()) {
                Char pmChr = pm.getChr();
                if (pmChr != null && pmChr.getId() != chr.getId() && pmChr.getClient().getChannel() == chr.getClient().getChannel() && pmChr.getLevel() > 9) {
                    pmChr.warp(chr.getField());
                    pmChr.write(FieldPacket.teleport(chr.getPosition(), pmChr));
                }
                pmChr.dispose();
            }
        }
    }

    public static void reviveByBearReborn(Char chr) { // TODO
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        chr.heal(chr.getMaxHP(), true);
        tsm.removeStatsBySkill(BEAR_REBORN);
        chr.chatMessage(ChatType.Tip, "透過波波的重生復活了。");
        chr.write(UserPacket.effect(Effect.skillAffected(BEAR_REBORN, (byte) 1, 0)));
        chr.getField().broadcast(UserRemote.effect(chr.getId(), Effect.skillAffected(BEAR_REBORN, (byte) 1, 0)), chr);
    }

    @Override
    public boolean isHandlerOfJob(short id) {
        return JobConstants.isBeastTamer(id);
    }

    private boolean isBearMode() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        return tsm.getOption(ShamanMode).nOption == 1;
    }

    private boolean isLeopardMode() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        return tsm.getOption(ShamanMode).nOption == 2;
    }

    private boolean isHawkMode() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        return tsm.getOption(ShamanMode).nOption == 3;
    }

    private boolean isCatMode() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        return tsm.getOption(ShamanMode).nOption == 4;
    }

    private void giveMeowCard(int slv) {
        if (!chr.hasSkill(MEOW_CARD) && !chr.hasSkill(MEOW_CARD_GOLD_SKILL)) {
            return;
        }
        EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        SkillInfo mc = SkillData.getSkillInfoById(MEOW_CARD);
        Option o1 = new Option();
        Option o2 = new Option();
        Option o3 = new Option();
        Option o4 = new Option();

        int randomMeowCard = getRandomMeowCard();

        resetPrevMeowCards();
        switch (randomMeowCard) {
            case MEOW_CARD_RED:
                o1.nReason = randomMeowCard;
                o1.nValue = mc.getValue(indieDamR, slv);
                o1.tTerm = mc.getValue(time, slv);
                tsm.sendStat(IndieDamR, o1);
                break;
            case MEOW_CARD_GREEN:
                o1.nReason = randomMeowCard;
                o1.nValue = mc.getValue(indieBooster, slv);
                o1.tTerm = mc.getValue(time, slv);
                newStats.put(IndieBooster, o1);
                o2.nReason = randomMeowCard;
                o2.nValue = mc.getValue(indieSpeed, slv);
                o2.tTerm = mc.getValue(time, slv);
                newStats.put(IndieSpeed, o2);
                tsm.sendStat(newStats);
                break;
            case MEOW_CARD_BLUE:
                o1.nReason = randomMeowCard;
                o1.nValue = mc.getValue(pdd, slv);
                o1.tTerm = mc.getValue(time, slv);
                tsm.sendStat(IndiePDD, o1);
                break;
            case MEOW_CARD_GOLD:
                o1.nReason = randomMeowCard;
                o1.nValue = mc.getValue(indieDamR, slv);
                o1.tTerm = mc.getValue(time, slv);
                newStats.put(IndieDamR, o1);
                o2.nReason = randomMeowCard;
                o2.nValue = mc.getValue(indieBooster, slv);
                o2.tTerm = mc.getValue(time, slv);
                newStats.put(IndieBooster, o2);
                o3.nReason = randomMeowCard;
                o3.nValue = mc.getValue(indieSpeed, slv);
                o3.tTerm = mc.getValue(time, slv);
                newStats.put(IndieSpeed, o3);
                o4.nReason = randomMeowCard;
                o4.nValue = mc.getValue(pdd, slv);
                o4.tTerm = mc.getValue(time, slv);
                newStats.put(IndiePDD, o4);
                tsm.sendStat(newStats);
                break;
        }
    }

    private int getRandomMeowCard() {
        int rng = new Random().nextInt((chr.hasSkill(MEOW_CARD_GOLD_SKILL) ? cards.length : cards.length - 1));
        return cards[rng];
    }

    private void resetPrevMeowCards() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        for (int cardBuffId : cards) {
            if (tsm.hasStatBySkillId(cardBuffId)) {
                tsm.removeStatsBySkill(cardBuffId);
            }
        }
    }

    private void giveKittyBattleSquadBuff() {
        EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o = new Option();
        SkillInfo si = SkillData.getSkillInfoById(KITTY_BATTLE_SQUAD);
        int slv = si.getCurrentLevel();
        o.nReason = KITTY_BATTLE_SQUAD;
        o.nValue = si.getValue(indiePad, slv);
        newStats.put(IndiePAD, o);
        newStats.put(IndieMAD, o.deepCopy());
        tsm.sendStat(newStats);
    }

    private void giveKittyTreatsBuff() {
        EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o1 = new Option();
        Option o2 = new Option();
        SkillInfo si = SkillData.getSkillInfoById(KITTY_TREATS);
        int slv = si.getCurrentLevel();
        o1.nReason = KITTY_TREATS;
        o1.nValue = si.getValue(indieMhp, slv);
        newStats.put(IndieMHP, o1);
        o2.nReason = KITTY_TREATS;
        o2.nValue = si.getValue(indieMmp, slv);
        newStats.put(IndieMMP, o2);
        tsm.sendStat(newStats);
    }

    private void giveStickyPawsBuff() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o1 = new Option();
        SkillInfo si = SkillData.getSkillInfoById(STICKY_PAWS);
        int slv = si.getCurrentLevel();
        o1.nOption = si.getValue(v, slv);
        o1.rOption = STICKY_PAWS;
        tsm.sendStat(DropRate, o1);
    }

    private void giveCatClawsBuff() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o1 = new Option();
        SkillInfo si = SkillData.getSkillInfoById(CAT_CLAWS);
        int slv = si.getCurrentLevel();
        o1.nOption = si.getValue(x, slv);
        o1.rOption = CAT_CLAWS;
        tsm.sendStat(CriticalBuff, o1);
    }

    private void giveMouserInsightBuff() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o1 = new Option();
        SkillInfo si = SkillData.getSkillInfoById(MOUSERS_INSIGHT);
        int slv = si.getCurrentLevel();
        o1.nOption = si.getValue(x, slv);
        o1.rOption = MOUSERS_INSIGHT;
        tsm.sendStat(IgnoreMobpdpR, o1);
    }

    private void giveFriendsOfArbyBuff() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o1 = new Option();
        SkillInfo si = SkillData.getSkillInfoById(FRIENDS_OF_ARBY);
        int slv = si.getCurrentLevel();
        if (tsm.getOptByCTSAndSkill(HolySymbol, Bishop.HOLY_SYMBOL) == null) { // Only apply if player doesn't have Holy Symbol
            o1.nOption = si.getValue(x, slv);
            o1.rOption = FRIENDS_OF_ARBY;
            tsm.sendStat(HolySymbol, o1);
        }
    }

    private Summon defensiveFormationSummon() {
        Skill skill = chr.getSkill(DEFENSIVE_FORMATION);
        SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
        int slv = skill.getCurrentLevel();
        defensiveFormation = Summon.getSummonByAndSetStat(c.getChr(), DEFENSIVE_FORMATION, slv);
        defensiveFormation.setFlyMob(true);
        defensiveFormation.setSummonTerm(si.getValue(time, slv));
        defensiveFormation.setMoveAbility(MoveAbility.Fly); // Different MoveAbility?
        return defensiveFormation;
    }

    // Attack related methods ------------------------------------------------------------------------------------------
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

        if (isLeopardMode()) { // Leopard
            if (hasHitMobs) {
                if (skillID != BRO_ATTACK) {
                    procBroAttack(attackInfo);
                }
            }
        }

        if (isHawkMode()) { // Hawk
            if (hasHitMobs) {
                applyRaptorTalonsOnMob(attackInfo);
            }
        }

        if (isCatMode()) { // Cat
            giveKittyBattleSquadBuff();
            giveKittyTreatsBuff();
            giveStickyPawsBuff();
            giveCatClawsBuff();
            giveMouserInsightBuff();
            giveFriendsOfArbyBuff();
        }
        Option o = new Option();
        switch (attackInfo.skillId) {
            case MAJESTIC_TRUMPET:
                if (!chr.hasSkillOnCooldown(skillID)) {
                    SkillInfo rca = SkillData.getSkillInfoById(skillID);
                    AffectedArea aa = AffectedArea.getAffectedArea(chr, attackInfo);
                    aa.setMobOrigin((byte) 0);
                    aa.setSkillID(skillID);
                    int x = chr.getPosition().getX();
                    int y = chr.getPosition().getY() + 41;
                    aa.setPosition(new Position(x, y));
                    aa.setRect(aa.getPosition().getRectAround(rca.getRects().get(0)));
                    aa.setDelay((short) 4);
                    chr.getField().spawnAffectedArea(aa);
                    chr.setSkillCooldown(skillID, slv);
                }
                break;
            case THUNDER_DASH:
            case ADV_THUNDER_DASH:
                SkillInfo tdi = SkillData.getSkillInfoById(THUNDER_TRAIL);
                AffectedArea aa2 = AffectedArea.getAffectedArea(chr, attackInfo);
                aa2.setMobOrigin((byte) 0);
                aa2.setSkillID(THUNDER_TRAIL);
                //int x = chr.getPosition().getX();
                //int y = chr.getPosition().getY() + 41;
                //aa.setPosition(new Position(x, y));
                aa2.setPosition(chr.getPosition());
                Rect rect = tdi.getRects().get(0);
                if (!chr.isLeft()) {
                    rect = rect.moveRight();
                }
                aa2.setRect(aa2.getPosition().getRectAround(rect));
                aa2.setDelay((short) 4);
                chr.getField().spawnAffectedArea(aa2);
                break;
            case PURR_ZONE: //TODO  isn't a AffectedArea, but a 'Special'
                SkillInfo pz = SkillData.getSkillInfoById(PURR_ZONE);
                AffectedArea aa3 = AffectedArea.getAffectedArea(chr, attackInfo);
                aa3.setMobOrigin((byte) 0);
                aa3.setSkillID(skillID);
                aa3.setPosition(chr.getPosition());
                aa3.setRect(aa3.getPosition().getRectAround(pz.getRects().get(0)));
                aa3.setSlv(slv);
                chr.getField().spawnAffectedArea(aa3);
                break;
            case FIRE_KITTY:
                o.nOption = si.getValue(SkillStat.x, slv);
                o.rOption = skillID;
                o.tOption = si.getValue(time, slv);
                for (MobAttackInfo mai : attackInfo.mobAttackInfo) {
                    Life life = chr.getField().getLifeByObjectID(mai.mobId);
                    if (life instanceof Mob mob) {
                        if (mob == null || mob.getHp() <= 0) {
                            continue;
                        }
                        MobTemporaryStat mts = mob.getTemporaryStat();
                        mts.addStatOptions(mob, MobStat.PDR, o.deepCopy());
                    }
                }
                break;
        }
    }

    private void procBroAttack(AttackInfo attackInfo) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o1 = new Option();
        if (tsm.getOptByCTSAndSkill(ACC, BRO_ATTACK) != null) {
            Summon summon;
            Field field;
            Skill skill = chr.getSkill(BRO_ATTACK);
            if (!chr.hasSkill(BRO_ATTACK)) {
                return;
            }
            SkillInfo si = SkillData.getSkillInfoById(BRO_ATTACK);
            int slv = skill.getCurrentLevel();
            int summonProp = si.getValue(prop, slv);
            for (MobAttackInfo mai : attackInfo.mobAttackInfo) {
                Life life = chr.getField().getLifeByObjectID(mai.mobId);
                if (life instanceof Mob mob) {
                    if (mob == null || mob.getHp() <= 0) {
                        continue;
                    }
                    if (Util.succeedProp(summonProp)) {
                        summon = Summon.getSummonByAndSetStat(c.getChr(), BRO_ATTACK, slv);
                        field = c.getChr().getField();
                        summon.setFlyMob(false);
                        summon.setPosition(mob.getPosition());
                        summon.setSummonTerm(si.getValue(x, slv));
                        summon.setMoveAbility(MoveAbility.WalkRandom);
                        field.spawnAddSummon(summon);
                    }
                }
            }

        }
    }

    private void applyRaptorTalonsOnMob(AttackInfo attackInfo) {
        if (!chr.hasSkill(RAPTOR_TALONS)) {
            return;
        }
        Skill skill = chr.getSkill(RAPTOR_TALONS);
        SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
        int slv = skill.getCurrentLevel();
        int dotTime = si.getValue(SkillStat.dotTime, slv);
        BurnedInfo bi = BurnedInfo.createBurnInfo(chr, RAPTOR_TALONS, slv, Arrays.stream(attackInfo.mobAttackInfo.get(0).damages).sum());
        for (MobAttackInfo mai : attackInfo.mobAttackInfo) {
            Life life = chr.getField().getLifeByObjectID(mai.mobId);
            if (life instanceof Mob mob) {
                if (mob == null || mob.getHp() <= 0) {
                    return;
                }
                MobTemporaryStat mts = mob.getTemporaryStat();
                if (Util.succeedProp(si.getValue(prop, slv))) {
                    mts.createAndAddBurnedInfo(mob, bi, RAPTOR_TALONS);
                }
            }
        }
    }

    @Override
    public int getFinalAttackSkill(int faSkill) {
        fortFollowUpAddAttack++;
        if (isBearMode() && chr.hasSkill(FORT_FOLLOW_UP) && fortFollowUpAddAttack >= 4) {
            fortFollowUpAddAttack = 0;
            return FORT_FOLLOW_UP;
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
        Option o3 = new Option();
        Option o4 = new Option();
        Summon summon;
        Field field;
        switch (skillID) {//Common
            case BEAR_MODE:
            case SNOW_LEOPARD_MODE:
            case HAWK_MODE:
            case CAT_MODE:
                o1.nOption = (skillID - 110001500);
                o1.rOption = skillID;
                tsm.sendStat(ShamanMode, o1);

                for (int modeId : buffsByMode.keySet()) {
                    if (skillID == modeId) {
                        continue;
                    }
                    for (int buffId : buffsByMode.get(modeId)) {
                        tsm.removeStatsBySkill(buffId);
                    }
                }
                break;
            //Bear Mode
            case LIL_FORT:
                summon = Summon.getSummonByAndSetStat(c.getChr(), skillID, slv);
                field = c.getChr().getField();
                summon.setFlyMob(false);
                summon.setSummonTerm(si.getValue(time, slv));
                summon.setMoveAbility(MoveAbility.Stop);
                field.spawnSummon(summon);
                break;
            case BEAR_ASSAULT:
                o1.nOption = si.getValue(x, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(q, slv);
                newStats.put(DamR, o1);
                o2.nOption = si.getValue(z, slv);
                o2.rOption = skillID;
                o2.tOption = si.getValue(q, slv);
                newStats.put(CriticalBuff, o2);
                o3.nOption = si.getValue(mobCount, slv);
                o3.rOption = skillID;
                o3.tOption = si.getValue(q, slv);
                newStats.put(Enrage, o3);
                tsm.sendStat(newStats);
                break;
            //Leopard Mode
            case BRO_ATTACK:
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(ACC, o1);
                break;
            //Hawk Mode
            case FLY:
                o1.nOption = 1;
                o1.rOption = skillID;
                tsm.sendStat(NewFlying, o1);
                break;
            case HAWK_FLOCK:
                o1.nOption = si.getValue(speed, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                newStats.put(Speed, o1);
                o2.nOption = si.getValue(jump, slv);
                o2.rOption = skillID;
                o2.tOption = si.getValue(time, slv);
                newStats.put(Jump, o2);
                tsm.sendStat(newStats);
                break;
            case RAPTOR_TALONS:
                o1.nReason = skillID;
                o1.nValue = si.getValue(indieMad, slv);
                o1.tTerm = si.getValue(time, slv);
                tsm.sendStat(IndieMAD, o1);
                break;
            case BIRDS_EYE_VIEW:
                o1.nReason = skillID;
                o1.nValue = si.getValue(indieCr, slv);
                o1.tTerm = si.getValue(time, slv);
                newStats.put(IndieCrR, o1);
                o2.nOption = si.getValue(epdd, slv);
                o2.rOption = skillID;
                o2.tOption = si.getValue(time, slv);
                newStats.put(EPDD, o2);
                o3.nOption = si.getValue(acc, slv);
                o3.rOption = skillID;
                o3.tOption = si.getValue(time, slv);
                newStats.put(ACC, o3);
                o4.nOption = si.getValue(eva, slv);
                o4.rOption = skillID;
                o4.tOption = si.getValue(time, slv);
                newStats.put(EVA, o4);
                tsm.sendStat(newStats);
                break;
            case RAZOR_BEAK:
                o1.nReason = skillID;
                o1.nValue = si.getValue(indieMad, slv);
                o1.tTerm = si.getValue(time, slv);
                newStats.put(IndieMAD, o1);
                o2.nReason = skillID;
                o2.nValue = si.getValue(indiePad, slv);
                o2.tTerm = si.getValue(time, slv);
                newStats.put(IndiePAD, o2);
                tsm.sendStat(newStats);
                break;
            //Cat Mode
            case MEOW_CARD:
            case MEOW_CARD_GOLD_SKILL:
                giveMeowCard(slv);
                break;
            //Hyper
            case TEAM_ROAR:
                o1.nReason = skillID;
                o1.nValue = si.getValue(indieDamR, slv);
                o1.tTerm = si.getValue(time, slv);
                newStats.put(IndieDamR, o1);
                o2.nOption = 1;
                o2.rOption = skillID;
                o2.tOption = si.getValue(time, slv);
                newStats.put(NotDamaged, o2);
                o2.nOption = 1;
                o2.rOption = skillID;
                o2.tOption = si.getValue(time, slv);
                newStats.put(Chachacha, o2);
                tsm.sendStat(newStats);
                break;
            case CHAMP_CHARGE_LEOPARD:
            case CHAMP_CHARGE_BEAR:
            case CHAMP_CHARGE_BIRD:
                chr.addSkillCooldown(CHAMP_CHARGE, 60 * 1000);
                break;
            case CHAMP_CHARGE_MEOW:
                AffectedArea aa = AffectedArea.getPassiveAA(chr, skillID, slv);
                aa.setMobOrigin((byte) 0);
                aa.setPosition(chr.getPosition());
                aa.setDuration(si.getValue(s, slv) * 1000);
                aa.setCurFoothold(chr.getFoothold());
                aa.setRect(aa.getPosition().getRectAround(si.getRects().get(0)));
                chr.getField().spawnAffectedArea(aa);
                chr.addSkillCooldown(CHAMP_CHARGE, 60 * 1000);
                break;
            case HOMEWARD_BOUND:
                Field toField = chr.getOrCreateFieldByCurrentInstanceType(si.getValue(x, slv));
                chr.warp(toField);
                break;
            case EKA_EXPRESS: //TODO Eka Express Skill
                Field townField = FieldData.getFieldById(chr.getField().getReturnMap());
                int x = townField.getPortalByName("tp").getX();
                int y = townField.getPortalByName("tp").getY();
                Position townPosition = new Position(x, y); // Grabs the Portal Co-ordinates for the TownPortalPoint
                int duration = si.getValue(time, slv);
                if (chr.getTownPortal() != null) {
                    TownPortal townPortal = chr.getTownPortal();
                    townPortal.despawnTownPortal();
                }
                TownPortal townPortal = new TownPortal(chr, townPosition, chr.getPosition(), chr.getField().getReturnMap(), chr.getFieldID(), skillID, duration);
                townPortal.spawnTownPortal();
                chr.dispose();
                break;
            case MEOW_CURE:
            case BEASTLY_RESOLVE:
                tsm.removeAllDebuffs();
                break;
            case MEOW_HEAL:
                chr.heal((int) (chr.getMaxHP() / ((double) 100 / si.getValue(hp, slv))));
                break;
            case MEOW_REVIVE:
                Party party = chr.getParty();
                if (party != null) {
                    field = chr.getField();
                    Rect rect = chr.getPosition().getRectAround(si.getRects().get(0));
                    if (!chr.isLeft()) {
                        rect = rect.moveRight();
                    }
                    List<Char> eligblePartyCharList = field.getPartyCharSameFieldInRect(chr, rect).stream().
                            filter(pmchr -> pmchr.getId() != chr.getId() && pmchr.getHP() <= 0).toList();
                    if (eligblePartyCharList.size() > 0) {
                        Char randomPartyChr = Util.getRandomFromCollection(eligblePartyCharList);
                        if (randomPartyChr != null) {
                            TemporaryStatManager partyTSM = randomPartyChr.getTemporaryStatManager();
                            randomPartyChr.heal(randomPartyChr.getMaxHP());
                            partyTSM.sendStat(NotDamaged, o1);
                            randomPartyChr.write(UserPacket.effect(Effect.skillAffected(skillID, (byte) 1, 0)));
                            randomPartyChr.getField().broadcast(UserRemote.effect(randomPartyChr.getId(), Effect.skillAffected(skillID, (byte) 1, 0)));
                        }
                    }
                }
                break;
        }
    }

    // Hit related methods ---------------------------------------------------------------------------------------------
    @Override
    public void handleHit(Client c, InPacket inPacket, HitInfo hitInfo) {

        super.handleHit(c, inPacket, hitInfo);
    }

    @Override
    public void setCharCreationStats(Char chr) {
        super.setCharCreationStats(chr);
        CharacterStat cs = chr.getAvatarData().getCharacterStat();
        cs.setPosMap(FieldConstants.HOME_MAP);//cs.setPosMap(FieldConstants.HENESYS_ID);
        cs.setJob(JobConstants.JobEnum.BEAST_TAMER_1.getJobId());
        cs.setLevel(10);
        cs.setStr(4);
        cs.setDex(4);
        cs.setInt(45);
        cs.setLuk(4);
        cs.setHp(1000);
        cs.setMaxHp(1000);
        cs.setMp(500);
        cs.setMaxMp(500);
        cs.getExtendSP().addSpToJobLevel(1, 5);
    }

    @Override
    public void handleJobAdvance() {
        ScriptManagerImpl sm = chr.getScriptManager();
        if (chr.getJob() == JobConstants.JobEnum.BEAST_TAMER_1.getJobId()) {
            if (chr.getLevel() < 30) {
                sm.sendSayOkay("#e這個職業需要達到#r30#k等以上才能轉職");
                return;
            }
            if (sm.sendAskYesNo("#e要跳過轉職任務嗎？")) {
                if (sm.getEmptyInventorySlots(1) < 1) {
                    sm.sendSayOkay("#e請在裝備欄空出更多空間。");
                    return;
                }
                sm.jobAdvance(JobConstants.JobEnum.BEAST_TAMER_2.getJobId());
            }
        } else if (chr.getJob() == JobConstants.JobEnum.BEAST_TAMER_2.getJobId()) {
            if (chr.getLevel() < 60) {
                sm.sendSayOkay("#e這個職業需要達到#r60#k等以上才能轉職");
                return;
            }
            if (sm.sendAskYesNo("#e要跳過轉職任務嗎？")) {
                if (sm.getEmptyInventorySlots(1) < 1) {
                    sm.sendSayOkay("#e請在裝備欄空出更多空間。");
                    return;
                }
                sm.jobAdvance(JobConstants.JobEnum.BEAST_TAMER_3.getJobId());
            }
        } else if (chr.getJob() == JobConstants.JobEnum.BEAST_TAMER_3.getJobId()) {
            if (chr.getLevel() < 100) {
                sm.sendSayOkay("#e這個職業需要達到#r100#k等以上才能轉職");
                return;
            }
            if (sm.sendAskYesNo("#e要跳過轉職任務嗎？")) {
                if (sm.getEmptyInventorySlots(1) < 1) {
                    sm.sendSayOkay("#e請在裝備欄空出更多空間。");
                    return;
                }
                sm.jobAdvance(JobConstants.JobEnum.BEAST_TAMER_4.getJobId());
            }
        } else {
            sm.sendSayOkay("#e目前的狀態無法轉職。");
        }
    }
}
