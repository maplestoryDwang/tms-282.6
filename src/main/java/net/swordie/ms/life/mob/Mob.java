package net.swordie.ms.life.mob;

import net.swordie.ms.connection.protocol.codec.MobPacketEncoder;
import net.swordie.ms.Server;
import net.swordie.ms.ServerConfig;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.Core;
import net.swordie.ms.client.character.info.ExpIncreaseInfo;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.SkillStat;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.client.jobs.adventurer.magician.FirePoison;
import net.swordie.ms.client.jobs.legend.Shade;
import net.swordie.ms.client.social.Guild.GuildSkill;
import net.swordie.ms.client.social.Party.Party;
import net.swordie.ms.client.social.Party.PartyDamageInfo;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.packet.*;
import net.swordie.ms.constants.*;
import net.swordie.ms.enums.*;
import net.swordie.ms.handlers.header.OutHeader;
import net.swordie.ms.handlers.user.SpecialHPBossHandler;
import net.swordie.ms.life.Life;
import net.swordie.ms.life.drop.Drop;
import net.swordie.ms.life.drop.DropInfo;
import net.swordie.ms.life.mob.boss.demian.stigma.DemianStigma;
import net.swordie.ms.life.mob.skill.BurnedInfo;
import net.swordie.ms.life.mob.skill.MobSkill;
import net.swordie.ms.life.mob.skill.MobSkillID;
import net.swordie.ms.life.mob.skill.ShootingMoveStat;
import net.swordie.ms.loaders.MobData;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.util.*;
import net.swordie.ms.util.container.Triple;
import net.swordie.ms.util.container.Tuple;
import net.swordie.ms.world.boss.BossHelper;
import net.swordie.ms.world.event.InGameEvent;
import net.swordie.ms.world.event.InGameEventManager;
import net.swordie.ms.world.event.PinkZakumEvent;
import net.swordie.ms.world.event.hottime.HotTime;
import net.swordie.ms.world.field.Clock;
import net.swordie.ms.world.field.Field;
import net.swordie.ms.world.field.Foothold;
import net.swordie.ms.world.field.fieldeffect.FieldEffect;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Mob extends Life {

    private int owner = 0;
    private boolean sealedInsteadDead, patrolMob;
    private int option, effectItemID, range, detectX, senseX, phase, curZoneDataType;
    private int refImgMobID, lifeReleaseOwnerAID, afterAttack, currentAction, scale, eliteGrade, eliteType, targetUserIdFromServer;
    private long hp;
    private long mp;
    private byte calcDamageIndex = 1, moveAction, appearType, teamForMCarnival;
    private Position prevPos;
    private Foothold curFoodhold;
    private Foothold homeFoothold;
    private String lifeReleaseOwnerName = "", lifeReleaseMobName = "";
    private ShootingMoveStat shootingMoveStat;
    private ForcedMobStat forcedMobStat;
    private MobTemporaryStat temporaryStat;
    private int firstAttack;
    private int summonType;
    private int category;
    private String mobType = "";
    private int link;
    private double fs;
    private String elemAttr = "";
    private int hpTagColor;
    private int hpTagBgcolor;
    private boolean HPgaugeHide;
    private int rareItemDropLevel;
    private boolean boss;
    private int hpRecovery;
    private int mpRecovery;
    private boolean undead;
    private int mbookID;
    private boolean noRegen;
    private int chaseSpeed;
    private int explosiveReward;
    private int flySpeed;
    private int dropItemPeriod;
    private boolean invincible;
    private boolean hideName;
    private boolean hideHP;
    private String changeableMobType = "";
    private boolean changeable;
    private boolean noFlip;
    private boolean tower;
    private boolean partyBonusMob;
    private int wp;
    private boolean useReaction;
    private boolean publicReward;
    private boolean minion;
    private boolean forward;
    private boolean isRemoteRange;
    private boolean ignoreFieldOut;
    private boolean ignoreMoveImpact;
    private int summonEffect;
    private boolean skeleton;
    private boolean hideUserDamage;
    private int fixedDamage;
    private boolean individualReward;
    private int removeAfter;
    private boolean notConsideredFieldSet;
    private String fixedMoveDir = "";
    private boolean noDoom;
    private boolean useCreateScript;
    private boolean knockback;
    private boolean blockUserMove;
    private int bodyDisease;
    private int bodyDiseaseLevel;
    private int point;
    private int partyBonusR;
    private boolean removeQuest;
    private int passiveDisease;
    private int coolDamageProb;
    private int coolDamage;
    private int damageRecordQuest;
    private int sealedCooltime;
    private int willEXP;
    private boolean onFieldSetSummon;
    private boolean userControll;
    private boolean noDebuff;
    private boolean targetFromSvr;
    private int charismaEXP;
    private boolean isSplit;
    private boolean bossKillHandled;

    /** Survives clear(): quest credit and the final-hit callback both visit this same dead mob. */
    public synchronized boolean claimBossKillHandling() {
        if (bossKillHandled) return false;
        bossKillHandled = true;
        return true;
    }
    private int splitLink;
    private final Map<Char, Long> damageDone = new HashMap<>();
    private Set<DropInfo> drops = new HashSet<>();
    private final List<MobSkill> skills = new ArrayList<>();
    private final List<MobSkill> attacks = new ArrayList<>();
    private Set<Integer> quests = new HashSet<>();
    private Set<Integer> revives = new HashSet<>();
    private final Map<Integer, Long> skillCooldowns = new HashMap<>();
    private long nextPossibleSkillTime = 0;
    private final List<Tuple<Integer, Integer>> eliteSkills = new ArrayList<>();
    private boolean selfDestruction;
    private final List<MobSkill> skillDelays = new CopyOnWriteArrayList<>();
    private boolean inAttack;
    private boolean isBanMap;
    private int banType = 1;// default
    private int banMsgType = 1;// default
    private String banMsg = "";
    private List<Tuple<Integer, String>> banMap = new ArrayList<>();// field, portal name
    private boolean isEscortMob = false;
    private List<EscortDest> escortDest = new ArrayList<>();
    private List<MobBindInfo> mobBindInfos = new ArrayList<>();
    private int currentDestIndex = 0;
    private int escortStopDuration = 0;
    private Set<Integer> mobSet = new HashSet<>(); // for quests: a list of mobs
    private Set<Integer> parentMobSet = new HashSet<>(); // for quests: which mobs this mob corresponds to
    private int mobSpawnerId;
    private Map<String, Object> properties = new HashMap<>();
    private long respawnDelay;
    private ScheduledFuture<?> eliteBossTimer;
    private MobZoneInfo mobZone;
    private long dotDamage;
    private EnumMap<MobStat, Long> lastDebuffTimes = new EnumMap<>(MobStat.class);

    private boolean isAdvMarked = false;

    private long lastTimeChangeState = 0; //Use for queen

    private long lifeTime = 0;
    private boolean isPierreSpawnTwice = false;
    private long firstPierreDieTime = 0;
    private long capEffectTime = 0;

    // Will (Boss):
    private boolean isUseSpecialSkill = false;
    private boolean isSpecialPattern = false;
    private List<Integer> willHPlist = new ArrayList<>();

    private long nextAggroSendTime; // ms
    private transient ArrayList<Map.Entry<Char, Long>> cachedTop10;
    private transient long cachedTop10At;
    private static final int AGGRO_TOP_N = 10;
    private static final long AGGRO_SEND_INTERVAL_MS = 200;

    public Mob(int templateId) {
        super(templateId);
        this.controllerID = -1;
        this.forcedMobStat = new ForcedMobStat();
        this.temporaryStat = new MobTemporaryStat();
        this.scale = 100;
        this.calcDamageIndex = 1;
        this.respawnDelay = 2000L;
    }

    public void update(long now) {
        getTemporaryStat().update(this, now);
    }

    public Mob deepCopy() {
        Mob copy = new Mob(getTemplateId());
        // start life
        copy.setObjectId(getObjectId());
        copy.setControllerID(getControllerID());
        copy.setLifeType(getLifeType());
        copy.setTemplateId(getTemplateId());
        copy.setX(getX());
        copy.setY(getY());
        copy.setMobTime(getMobTime());
        copy.setFlip(isFlip());
        copy.setHide(isHide());
        copy.setFh(getFh());
        copy.setCy(getCy());
        copy.setRx0(getRx0());
        copy.setRx1(getRx1());
        copy.setLimitedName(getLimitedName());
        copy.setUseDay(isUseDay());
        copy.setUseNight(isUseNight());
        copy.setHold(isHold());
        copy.setNoFoothold(isNoFoothold());
        copy.setDummy(isDummy());
        copy.setSpine(isSpine());
        copy.setMobTimeOnDie(isMobTimeOnDie());
        copy.setRegenStart(getRegenStart());
        copy.setMobAliveReq(getMobAliveReq());
        // end life
        copy.setSealedInsteadDead(isSealedInsteadDead());
        copy.setOption(getOption());
        copy.setEffectItemID(getEffectItemID());
        copy.setPatrolMob(isPatrolMob());
        copy.setRange(getRange());
        copy.setDetectX(getDetectX());
        copy.setSenseX(getSenseX());
        copy.setPhase(getPhase());
        copy.setCurZoneDataType(getCurZoneDataType());
        copy.setRefImgMobID(getRefImgMobID());
        copy.setLifeReleaseOwnerAID(getLifeReleaseOwnerAID());
        copy.setAfterAttack(getAfterAttack());
        copy.setCurrentAction(getCurrentAction());
        copy.setScale(getScale());
        copy.setEliteGrade(getEliteGrade());
        copy.setEliteType(getEliteType());
        copy.setTargetUserIdFromServer(getTargetUserIdFromServer());
        copy.setCalcDamageIndex(getCalcDamageIndex());
        copy.setMoveAction(getMoveAction());
        copy.setAppearType(getAppearType());
        copy.setTeamForMCarnival(getTeamForMCarnival());
        if (getPrevPos() != null) {
            copy.setPrevPos(getPrevPos().deepCopy());
        }
        if (getCurFoodhold() != null) {
            copy.setCurFoodhold(getCurFoodhold().deepCopy());
        }
        if (getHomeFoothold() != null) {
            copy.setHomeFoothold(getHomeFoothold().deepCopy());
        }
        copy.setLifeReleaseOwnerName(getLifeReleaseOwnerName());
        copy.setLifeReleaseMobName(getLifeReleaseMobName());
        copy.setShootingMoveStat(null);
        if (getForcedMobStat() != null) {
            copy.setForcedMobStat(getForcedMobStat().deepCopy());
        }
        if (getTemporaryStat() != null) {
            copy.setTemporaryStat(getTemporaryStat().deepCopy());
        }
        copy.setFirstAttack(getFirstAttack());
        copy.setSummonType(getSummonType());
        copy.setCategory(getCategory());
        copy.setMobType(getMobType());
        copy.setLink(getLink());
        copy.setFs(getFs());
        copy.setElemAttr(getElemAttr());
        copy.setHpTagColor(getHpTagColor());
        copy.setHpTagBgcolor(getHpTagBgcolor());
        copy.setHPgaugeHide(isHPgaugeHide());
        copy.setRareItemDropLevel(getRareItemDropLevel());
        copy.setBoss(isBoss());
        copy.setHpRecovery(getHpRecovery());
        copy.setMpRecovery(getMpRecovery());
        copy.setUndead(isUndead());
        copy.setMbookID(getMbookID());
        copy.setNoRegen(isNoRegen());
        copy.setChaseSpeed(getChaseSpeed());
        copy.setExplosiveReward(getExplosiveReward());
        copy.setFlySpeed(getFlySpeed());
        copy.setInvincible(isInvincible());
        copy.setHideName(isHideName());
        copy.setHideHP(isHideHP());
        copy.setChangeableMobType(getChangeableMobType());
        copy.setChangeable(isChangeable());
        copy.setNoFlip(isNoFlip());
        copy.setTower(isTower());
        copy.setPartyBonusMob(isPartyBonusMob());
        copy.setWp(getWp());
        copy.setUseReaction(isUseReaction());
        copy.setPublicReward(isPublicReward());
        copy.setMinion(isMinion());
        copy.setForward(isForward());
        copy.setIsRemoteRange(isRemoteRange());
        copy.setIgnoreFieldOut(isIgnoreFieldOut());
        copy.setIgnoreMoveImpact(isIgnoreMoveImpact());
        copy.setSummonEffect(getSummonEffect());
        copy.setSkeleton(isSkeleton());
        copy.setHideUserDamage(isHideUserDamage());
        copy.setFixedDamage(getFixedDamage());
        copy.setIndividualReward(isIndividualReward());
        copy.setRemoveAfter(getRemoveAfter());
        copy.setNotConsideredFieldSet(isNotConsideredFieldSet());
        copy.setFixedMoveDir(getFixedMoveDir());
        copy.setNoDoom(isNoDoom());
        copy.setUseCreateScript(isUseCreateScript());
        copy.setKnockback(isKnockback());
        copy.setBlockUserMove(isBlockUserMove());
        copy.setBodyDisease(getBodyDisease());
        copy.setBodyDiseaseLevel(getBodyDiseaseLevel());
        copy.setPoint(getPoint());
        copy.setPartyBonusR(getPartyBonusR());
        copy.setRemoveQuest(isRemoveQuest());
        copy.setPassiveDisease(getPassiveDisease());
        copy.setCoolDamageProb(getCoolDamageProb());
        copy.setCoolDamage(getCoolDamage());
        copy.setDamageRecordQuest(getDamageRecordQuest());
        copy.setSealedCooltime(getSealedCooltime());
        copy.setWillEXP(getWillEXP());
        copy.setOnFieldSetSummon(isOnFieldSetSummon());
        copy.setUserControll(isUserControll());
        copy.setNoDebuff(isNoDebuff());
        copy.setTargetFromSvr(isTargetFromSvr());
        copy.setCharismaEXP(getCharismaEXP());
        copy.setMp(getMp());
        copy.setMaxMp(getMaxMp());
        copy.setDrops(getDrops()); // doesn't get mutated, so should be fine
        copy.setBanMap(isBanMap());
        copy.setBanType(getBanType());
        copy.setBanMsgType(getBanMsgType());
        copy.setBanMsg(getBanMsg());
        copy.setBanMapFields(getBanMapFields());
        copy.setRespawnDelay(getRespawnDelay());
        for (MobSkill ms : getSkills()) {
            copy.addSkill(ms);
        }
        for (MobSkill ms : getAttacks()) {
            copy.addAttack(ms);
        }
        for (int rev : getRevives()) {
            copy.addRevive(rev);
        }
        for (int i : getQuests()) {
            copy.addQuest(i);
        }
        for (int i : getMobSet()) {
            copy.addMob(i);
        }
        for (int i : getParentMobSet()) {
            copy.addParentMob(i);
        }
        copy.setEscortMob(isEscortMob());
        copy.setMobZone(getMobZone());
        copy.setHp(getHp());
        copy.setMaxHp(getMaxHp());
        copy.setLevel(getLevel());
        return copy;
    }

    public long getRespawnDelay() {
        return respawnDelay;
    }

    public void setRespawnDelay(long delay) {
        respawnDelay = delay;
    }

    public Set<DropInfo> getDrops() {
        return drops;
    }

    public void setDrops(Set<DropInfo> drops) {
        this.drops = drops;
    }

    public boolean isSealedInsteadDead() {
        return sealedInsteadDead;
    }

    public void setSealedInsteadDead(boolean sealedInsteadDead) {
        this.sealedInsteadDead = sealedInsteadDead;
    }

    public ForcedMobStat getForcedMobStat() {
        return forcedMobStat;
    }

    public void setForcedMobStat(ForcedMobStat forcedMobStat) {
        this.forcedMobStat = forcedMobStat;
    }

    public boolean isPatrolMob() {
        return patrolMob;
    }

    public void setPatrolMob(boolean patrolMob) {
        this.patrolMob = patrolMob;
    }

    public int getOption() {
        return option;
    }

    public void setOption(int option) {
        this.option = option;
    }

    public int getEffectItemID() {
        return effectItemID;
    }

    public void setEffectItemID(int effectItemID) {
        this.effectItemID = effectItemID;
    }

    public int getDetectX() {
        return detectX;
    }

    public void setDetectX(int detectX) {
        this.detectX = detectX;
    }

    public int getSenseX() {
        return senseX;
    }

    public void setSenseX(int senseX) {
        this.senseX = senseX;
    }

    public int getRange() {
        return range;
    }

    public void setRange(int range) {
        this.range = range;
    }

    public int getPhase() {
        return phase;
    }

    public void setPhase(int phase) {
        this.phase = phase;
    }

    public int getCurZoneDataType() {
        return curZoneDataType;
    }

    public void setCurZoneDataType(int curZoneDataType) {
        this.curZoneDataType = curZoneDataType;
    }

    public int getRefImgMobID() {
        return refImgMobID;
    }

    public void setRefImgMobID(int refImgMobID) {
        this.refImgMobID = refImgMobID;
    }

    public int getLifeReleaseOwnerAID() {
        return lifeReleaseOwnerAID;
    }

    public void setLifeReleaseOwnerAID(int lifeReleaseOwnerAID) {
        this.lifeReleaseOwnerAID = lifeReleaseOwnerAID;
    }

    public int getAfterAttack() {
        return afterAttack;
    }

    public void setAfterAttack(int afterAttack) {
        this.afterAttack = afterAttack;
    }

    public int getCurrentAction() {
        return currentAction;
    }

    public void setCurrentAction(int currentAction) {
        this.currentAction = currentAction;
    }

    public int getScale() {
        return scale;
    }

    public void setScale(int scale) {
        this.scale = scale;
    }

    public int getEliteGrade() {
        return eliteGrade;
    }

    public void setEliteGrade(int eliteGrade) {
        this.eliteGrade = eliteGrade;
    }

    public int getEliteType() {
        return eliteType;
    }

    public void setEliteType(int eliteType) {
        this.eliteType = eliteType;
    }

    public int getTargetUserIdFromServer() {
        return targetUserIdFromServer;
    }

    public void setTargetUserIdFromServer(int targetUserIdFromServer) {
        this.targetUserIdFromServer = targetUserIdFromServer;
    }

    public long getHp() {
        return hp;
    }

    public void setHp(long hp) {
        this.hp = hp;
    }

    public int getHpComparedToMaxHP() {
        if (getMaxHp() <= Integer.MAX_VALUE) {
            return (int) getHp();
        } else {
            return (int) (getHp() * (((double) Integer.MAX_VALUE) / getMaxHp()));
        }
    }

    public int getHPPercent() {
        return (int) Math.ceil(getHp() * 100.0D / getMaxHp());
    }

    public byte getCalcDamageIndex() {
        return calcDamageIndex;
    }

    public void setCalcDamageIndex(byte calcDamageIndex) {
        this.calcDamageIndex = calcDamageIndex;
    }

    public byte getMoveAction() {
        return moveAction;
    }

    public void setMoveAction(byte moveAction) {
        this.moveAction = moveAction;
    }

    public byte getAppearType() {
        return appearType;
    }

    public void setAppearType(byte appearType) {
        this.appearType = appearType;
    }

    public byte getTeamForMCarnival() {
        return teamForMCarnival;
    }

    public void setTeamForMCarnival(byte teamForMCarnival) {
        this.teamForMCarnival = teamForMCarnival;
    }

    public Position getPrevPos() {
        return prevPos;
    }

    public void setPrevPos(Position prevPos) {
        this.prevPos = prevPos;
    }

    public Foothold getCurFoodhold() {
        return curFoodhold;
    }

    public void setCurFoodhold(Foothold curFoodhold) {
        this.curFoodhold = curFoodhold;
    }

    public String getLifeReleaseOwnerName() {
        return lifeReleaseOwnerName;
    }

    public void setLifeReleaseOwnerName(String lifeReleaseOwnerName) {
        this.lifeReleaseOwnerName = lifeReleaseOwnerName;
    }

    public String getLifeReleaseMobName() {
        return lifeReleaseMobName;
    }

    public void setLifeReleaseMobName(String lifeReleaseMobName) {
        this.lifeReleaseMobName = lifeReleaseMobName;
    }

    public ShootingMoveStat getShootingMoveStat() {
        return shootingMoveStat;
    }

    public void setShootingMoveStat(ShootingMoveStat shootingMoveStat) {
        this.shootingMoveStat = shootingMoveStat;
    }

    public Foothold getHomeFoothold() {
        return homeFoothold;
    }

    public void setHomeFoothold(Foothold homeFoothold) {
        this.homeFoothold = homeFoothold;
    }

    public long getMaxHp() {
        return getForcedMobStat().getMaxHP();
    }

    public void setMaxHp(long maxHp) {
        getForcedMobStat().setMaxHP(maxHp);
    }

    public long getExp() {
        return getForcedMobStat().getExp();
    }

    public void setExp(long exp) {
        getForcedMobStat().setExp(exp);
    }

    public long getMp() {
        return mp;
    }

    public void setMp(long mp) {
        this.mp = mp;
    }

    public long getMaxMp() {
        return getForcedMobStat().getMaxMP();
    }

    public void setMaxMp(long maxMp) {
        getForcedMobStat().setMaxMP(maxMp);
    }

    public int getLevel() {
        return getForcedMobStat().getLevel();
    }

    public void setLevel(int level) {
        getForcedMobStat().setLevel(level);
    }

    public int getPad() {
        return getForcedMobStat().getPad();
    }

    public void setPad(int pad) {
        getForcedMobStat().setPad(pad);
    }

    public int getMad() {
        return getForcedMobStat().getMad();
    }

    public void setMad(int mad) {
        getForcedMobStat().setMad(mad);
    }

    public int getPdr() {
        return getForcedMobStat().getPdr();
    }

    public int getMdr() {
        return getForcedMobStat().getMdr();
    }

    public MobTemporaryStat getTemporaryStat() {
        return temporaryStat;
    }

    public void setTemporaryStat(MobTemporaryStat temporaryStat) {
        this.temporaryStat = temporaryStat;
    }

    public int getFirstAttack() {
        return firstAttack;
    }

    public void setFirstAttack(int firstAttack) {
        this.firstAttack = firstAttack;
    }

    public int getSummonType() {
        return summonType;
    }

    public void setSummonType(int summonType) {
        this.summonType = summonType;
    }

    public int getCategory() {
        return category;
    }

    public void setCategory(int category) {
        this.category = category;
    }

    public String getMobType() {
        return mobType;
    }

    public void setMobType(String mobType) {
        this.mobType = mobType;
    }

    public int getLink() {
        return link;
    }

    public void setLink(int link) {
        this.link = link;
    }

    public double getFs() {
        return fs;
    }

    public void setFs(double fs) {
        this.fs = fs;
    }

    public String getElemAttr() {
        return elemAttr;
    }

    public void setElemAttr(String elemAttr) {
        this.elemAttr = elemAttr;
    }

    public int getHpTagColor() {
        return hpTagColor;
    }

    public void setHpTagColor(int hpTagColor) {
        this.hpTagColor = hpTagColor;
    }

    public int getHpTagBgcolor() {
        return hpTagBgcolor;
    }

    public void setHpTagBgcolor(int hpTagBgcolor) {
        this.hpTagBgcolor = hpTagBgcolor;
    }

    public boolean isHPgaugeHide() {
        return HPgaugeHide;
    }

    public void setHPgaugeHide(boolean HPgaugeHide) {
        this.HPgaugeHide = HPgaugeHide;
    }

    public int getRareItemDropLevel() {
        return rareItemDropLevel;
    }

    public void setRareItemDropLevel(int rareItemDropLevel) {
        this.rareItemDropLevel = rareItemDropLevel;
    }

    public boolean isBoss() {
        return boss;
    }

    public void setBoss(boolean boss) {
        this.boss = boss;
    }

    public int getHpRecovery() {
        return hpRecovery;
    }

    public void setHpRecovery(int hpRecovery) {
        this.hpRecovery = hpRecovery;
    }

    public int getMpRecovery() {
        return mpRecovery;
    }

    public void setMpRecovery(int mpRecovery) {
        this.mpRecovery = mpRecovery;
    }

    public boolean isUndead() {
        return undead;
    }

    public void setUndead(boolean undead) {
        this.undead = undead;
    }

    public int getMbookID() {
        return mbookID;
    }

    public void setMbookID(int mbookID) {
        this.mbookID = mbookID;
    }

    public boolean isNoRegen() {
        return noRegen;
    }

    public void setNoRegen(boolean noRegen) {
        this.noRegen = noRegen;
    }

    public int getChaseSpeed() {
        return chaseSpeed;
    }

    public void setChaseSpeed(int chaseSpeed) {
        this.chaseSpeed = chaseSpeed;
    }

    public int getExplosiveReward() {
        return explosiveReward;
    }

    public void setExplosiveReward(int explosiveReward) {
        this.explosiveReward = explosiveReward;
    }

    public int getFlySpeed() {
        return flySpeed;
    }

    public void setFlySpeed(int flySpeed) {
        this.flySpeed = flySpeed;
    }

    public int getDropItemPeriod() {
        return dropItemPeriod;
    }

    public void setDropItemPeriod(int d) {
        this.dropItemPeriod = d;
    }

    public boolean isInvincible() {
        return invincible;
    }

    public void setInvincible(boolean invincible) {
        this.invincible = invincible;
    }

    public boolean isHideName() {
        return hideName;
    }

    public void setHideName(boolean hideName) {
        this.hideName = hideName;
    }

    public boolean isHideHP() {
        return hideHP;
    }

    public void setHideHP(boolean hideHP) {
        this.hideHP = hideHP;
    }

    public String getChangeableMobType() {
        return changeableMobType;
    }

    public void setChangeableMobType(String changeableMobType) {
        this.changeableMobType = changeableMobType;
    }

    public boolean isChangeable() {
        return changeable;
    }

    public void setChangeable(boolean changeable) {
        this.changeable = changeable;
    }

    public boolean isNoFlip() {
        return noFlip;
    }

    public void setNoFlip(boolean noFlip) {
        this.noFlip = noFlip;
    }

    public boolean isTower() {
        return tower;
    }

    public void setTower(boolean tower) {
        this.tower = tower;
    }

    public boolean isPartyBonusMob() {
        return partyBonusMob;
    }

    public void setPartyBonusMob(boolean partyBonusMob) {
        this.partyBonusMob = partyBonusMob;
    }

    public int getWp() {
        return wp;
    }

    public void setWp(int wp) {
        this.wp = wp;
    }

    public boolean isUseReaction() {
        return useReaction;
    }

    public void setUseReaction(boolean useReaction) {
        this.useReaction = useReaction;
    }

    public boolean isPublicReward() {
        return publicReward;
    }

    public void setPublicReward(boolean publicReward) {
        this.publicReward = publicReward;
    }

    public boolean isMinion() {
        return minion;
    }

    public void setMinion(boolean minion) {
        this.minion = minion;
    }

    public boolean isForward() {
        return forward;
    }

    public void setForward(boolean forward) {
        this.forward = forward;
    }

    public void setIsRemoteRange(boolean isRemoteRange) {
        this.isRemoteRange = isRemoteRange;
    }

    public boolean isRemoteRange() {
        return isRemoteRange;
    }

    public void setRemoteRange(boolean isRemoteRange) {
        this.isRemoteRange = isRemoteRange;
    }

    public boolean isIgnoreFieldOut() {
        return ignoreFieldOut;
    }

    public void setIgnoreFieldOut(boolean ignoreFieldOut) {
        this.ignoreFieldOut = ignoreFieldOut;
    }

    public boolean isIgnoreMoveImpact() {
        return ignoreMoveImpact;
    }

    public void setIgnoreMoveImpact(boolean ignoreMoveImpact) {
        this.ignoreMoveImpact = ignoreMoveImpact;
    }

    public int getSummonEffect() {
        return summonEffect;
    }

    public void setSummonEffect(int summonEffect) {
        this.summonEffect = summonEffect;
    }

    public boolean isSkeleton() {
        return skeleton;
    }

    public void setSkeleton(boolean skeleton) {
        this.skeleton = skeleton;
    }

    public boolean isHideUserDamage() {
        return hideUserDamage;
    }

    public void setHideUserDamage(boolean hideUserDamage) {
        this.hideUserDamage = hideUserDamage;
    }

    public int getFixedDamage() {
        return fixedDamage;
    }

    public void setFixedDamage(int fixedDamage) {
        this.fixedDamage = fixedDamage;
    }

    public boolean isIndividualReward() {
        return individualReward;
    }

    public void setIndividualReward(boolean individualReward) {
        this.individualReward = individualReward;
    }

    public int getRemoveAfter() {
        return removeAfter;
    }

    public void setRemoveAfter(int removeAfter) {
        this.removeAfter = removeAfter;
    }

    public boolean isNotConsideredFieldSet() {
        return notConsideredFieldSet;
    }

    public void setNotConsideredFieldSet(boolean notConsideredFieldSet) {
        this.notConsideredFieldSet = notConsideredFieldSet;
    }

    public String getFixedMoveDir() {
        return fixedMoveDir;
    }

    public void setFixedMoveDir(String fixedMoveDir) {
        this.fixedMoveDir = fixedMoveDir;
    }

    public boolean isNoDoom() {
        return noDoom;
    }

    public void setNoDoom(boolean noDoom) {
        this.noDoom = noDoom;
    }

    public boolean isUseCreateScript() {
        return useCreateScript;
    }

    public void setUseCreateScript(boolean useCreateScript) {
        this.useCreateScript = useCreateScript;
    }

    public boolean isKnockback() {
        return knockback;
    }

    public void setKnockback(boolean knockback) {
        this.knockback = knockback;
    }

    public boolean isBlockUserMove() {
        return blockUserMove;
    }

    public void setBlockUserMove(boolean blockUserMove) {
        this.blockUserMove = blockUserMove;
    }

    public int getBodyDisease() {
        return bodyDisease;
    }

    public void setBodyDisease(int bodyDisease) {
        this.bodyDisease = bodyDisease;
    }

    public int getBodyDiseaseLevel() {
        return bodyDiseaseLevel;
    }

    public void setBodyDiseaseLevel(int bodyDiseaseLevel) {
        this.bodyDiseaseLevel = bodyDiseaseLevel;
    }

    public int getPoint() {
        return point;
    }

    public void setPoint(int point) {
        this.point = point;
    }

    public int getPartyBonusR() {
        return partyBonusR;
    }

    public void setPartyBonusR(int partyBonusR) {
        this.partyBonusR = partyBonusR;
    }

    public boolean isRemoveQuest() {
        return removeQuest;
    }

    public void setRemoveQuest(boolean removeQuest) {
        this.removeQuest = removeQuest;
    }

    public int getPassiveDisease() {
        return passiveDisease;
    }

    public void setPassiveDisease(int passiveDisease) {
        this.passiveDisease = passiveDisease;
    }

    public int getCoolDamageProb() {
        return coolDamageProb;
    }

    public void setCoolDamageProb(int coolDamageProb) {
        this.coolDamageProb = coolDamageProb;
    }

    public int getCoolDamage() {
        return coolDamage;
    }

    public void setCoolDamage(int coolDamage) {
        this.coolDamage = coolDamage;
    }

    public int getDamageRecordQuest() {
        return damageRecordQuest;
    }

    public void setDamageRecordQuest(int damageRecordQuest) {
        this.damageRecordQuest = damageRecordQuest;
    }

    public int getSealedCooltime() {
        return sealedCooltime;
    }

    public void setSealedCooltime(int sealedCooltime) {
        this.sealedCooltime = sealedCooltime;
    }

    public int getWillEXP() {
        return willEXP;
    }

    public void setWillEXP(int willEXP) {
        this.willEXP = willEXP;
    }

    public boolean isOnFieldSetSummon() {
        return onFieldSetSummon;
    }

    public void setOnFieldSetSummon(boolean onFieldSetSummon) {
        this.onFieldSetSummon = onFieldSetSummon;
    }

    public boolean isUserControll() {
        return userControll;
    }

    public void setUserControll(boolean userControll) {
        this.userControll = userControll;
    }

    public boolean isNoDebuff() {
        return noDebuff;
    }

    public void setNoDebuff(boolean noDebuff) {
        this.noDebuff = noDebuff;
    }

    public boolean isTargetFromSvr() {
        return targetFromSvr;
    }

    public void setTargetFromSvr(boolean targetFromSvr) {
        this.targetFromSvr = targetFromSvr;
    }

    public int getCharismaEXP() {
        return charismaEXP;
    }

    public void setCharismaEXP(int charismaEXP) {
        this.charismaEXP = charismaEXP;
    }

    public boolean isSplit() {
        return isSplit;
    }

    public void setSplit(boolean isSplit) {
        this.isSplit = isSplit;
    }

    public int getSplitLink() {
        return splitLink;
    }

    public void setSplitLink(int splitLinkID) {
        this.splitLink = splitLinkID;
    }

    public Set<Integer> getRevives() {
        return revives;
    }

    public void setRevives(Set<Integer> revives) {
        this.revives = revives;
    }

    public void addRevive(int revive) {
        revives.add(revive);
    }

    public boolean isBanMap() {
        return isBanMap;
    }

    public void setBanMap(boolean isBanMap) {
        this.isBanMap = isBanMap;
    }

    public int getBanType() {
        return banType;
    }

    public void setBanType(int banType) {
        this.banType = banType;
    }

    public int getBanMsgType() {
        return banMsgType;
    }

    public void setBanMsgType(int banMsgType) {
        this.banMsgType = banMsgType;
    }

    public String getBanMsg() {
        return banMsg;
    }

    public void setBanMsg(String banMsg) {
        this.banMsg = banMsg;
    }

    public List<Tuple<Integer, String>> getBanMapFields() {
        return banMap;
    }

    public void setBanMapFields(List<Tuple<Integer, String>> banMap) {
        this.banMap = banMap;
    }

    public void addBanMap(int fieldID, String portal) {
        this.banMap.add(new Tuple<>(fieldID, portal));
    }

    /**
     * Damages a mob.
     *
     * @param totalDamage the total damage that should be applied to the mob
     */
    public boolean damage(Char chr, long totalDamage, int skillID) {
        if (this.hp <= 0) {
            chr.write(MobPacketEncoder.active().leaveField(getObjectId(), DeathType.NO_ANIMATION_DEATH));
            return false;
        }
        final int templateID = getTemplateId();
        final Field field = getField();
        final Position pos = getPosition();
        int balrogSinkId = BossHpConstants.getBalrogDamageSinkId(templateID);
        if (balrogSinkId > 0) {
            Mob sink = field.getMobByTemplateId(balrogSinkId);
            if (sink != null && sink.getObjectId() != getObjectId()) {
                return sink.damage(chr, totalDamage, skillID);
            }
        }
        long calculatedDamage = totalDamage;
        if (templateID >= 9833101 && templateID <= 9833105 && (field.getId() == 921172000 || field.getId() == 921172100)) {
            Mob mob = field.getMobByTemplateId(templateID + 100);
            if (mob == null) {
                return false;
            }
            if (mob.getHp() <= 0) {
                return false;
            }
            return mob.damage(chr, totalDamage, skillID);
        }
        if (templateID == BossHelper.CHAOS_PIERRE_2 || templateID == BossHelper.CHAOS_PIERRE_3 || templateID == BossHelper.PIERRE_2 || templateID == BossHelper.PIERRE_3) {
            TemporaryStatManager tsm = chr.getTemporaryStatManager();
            if (tsm.hasStat(CharacterTemporaryStat.CapDebuff)) {
                int nOption = tsm.getOption(CharacterTemporaryStat.CapDebuff).nOption;
                if (nOption == 100 && (templateID == 8900001 || templateID == 8900101) || nOption == 200 && (templateID == 8900002 || templateID == 8900102)) {
                    heal(totalDamage);
                    field.broadcast(FieldPacket.fieldEffect(FieldEffect.mobHPTagFieldEffect(this)));
                    return false;
                }
            }
        } else if (templateID == 9400902) { // pink zakum third body
            InGameEvent event = InGameEventManager.getInstance().getActiveEvent();
            if (event instanceof PinkZakumEvent) {
                ((PinkZakumEvent) event).win();
            }
        } else if (templateID == BossConstants.INFERNO_WOLF) {
            if (chr.hasQuest(QuestConstants.INFERNO_WOLF_DAMAGE_RECORD)) {
                long currentDamageRecord = Long.parseLong(chr.getQRValueByKey(QuestConstants.INFERNO_WOLF_DAMAGE_RECORD, "damage"));
                if (currentDamageRecord < 900000000000L) {
                    long newDamageRecord = currentDamageRecord + totalDamage;
                    chr.setQRValue(QuestConstants.INFERNO_WOLF_DAMAGE_RECORD, "damage=" + newDamageRecord);
                }
            } else {
                chr.createQuestWithQRValue(QuestConstants.INFERNO_WOLF_DAMAGE_RECORD, "damage=" + totalDamage);
            }
        } else if (templateID == 8880303 || templateID == 8880304 || templateID == 8880343 || templateID == 8880344) {
            // TMS 282: phase-1 HP gates / dimension sync / phase end (research/tms2823/will-flow-0930).
            if (net.swordie.ms.world.boss.Tms282WillPhase1.onBodyDamage(chr, this, totalDamage)) {
                chr.getJobHandler().handleMobDamaged(this, totalDamage);
                return false;
            }
            if (this.hp - totalDamage > 0) {
                Mob will = field.getMobByTemplateId(8880300);
                if (will == null) {
                    will = field.getMobByTemplateId(8880340);
                }
                long nowhp = this.hp - totalDamage;
                double HPPercent = nowhp * 100.0D / this.hp;
                if (HPPercent <= 66.6D && will.getWillHPlist().contains(666)) {
                    setHp((long) (getMaxHp() * 66.6D / 100.0D));
                    field.broadcast(FieldPacket.fieldEffect(FieldEffect.mobHPTagFieldEffect(this)));
                    return false;
                }
                if (HPPercent <= 33.3D && will.getWillHPlist().contains(333)) {
                    setHp((long) (getMaxHp() * getBonusHp() * 33.3D / 100.0D));
                    field.broadcast(FieldPacket.fieldEffect(FieldEffect.mobHPTagFieldEffect(this)));
                    return false;
                }
                if (HPPercent <= 0.3D && will.getWillHPlist().contains(3)) {
                    setHp((long) (getMaxHp() * 0.3D / 100.0D));
                    field.broadcast(FieldPacket.fieldEffect(FieldEffect.mobHPTagFieldEffect(this)));
                    return false;
                }
            }
        } else if (templateID == 8880301 || templateID == 8880341) {
            if (this.hp - totalDamage > 0) {
                long nowhp = this.hp - totalDamage;
                double HPPercent = nowhp * 100.0D / this.hp;
                if (HPPercent <= 50.0D && getWillHPlist().contains(500)) {
                    setHp(getMaxHp() * 50L / 100L);
                    field.broadcast(FieldPacket.fieldEffect(FieldEffect.mobHPTagFieldEffect(this)));
                    if (!isUseSpecialSkill()) {
                        MobSkill mobSkill = new MobSkill();
                        mobSkill.setSkillID(MobSkillID.Will.getVal());
                        mobSkill.setLevel(7);
                        mobSkill.setFlip(isFlip());
                        getSkillDelays().add(mobSkill);
                        setSkillDelay(mobSkill.getSkillDelay());
                        getField().broadcast(MobPool.setSkillDelay(this, mobSkill.getSkillDelay(), MobSkillID.Will.getVal(), 7));
                        mobSkill.applyEffect(this);
                        setUseSpecialSkill(true);
                        getTimer().addEvent(() -> setUseSpecialSkill(false), 120000);
                    }
                    return false;
                }
                if (HPPercent <= 0.3D && getWillHPlist().contains(3)) {
                    setHp((long) (getMaxHp() * getBonusHp() * 0.3D / 100.0D));
                    field.broadcast(FieldPacket.fieldEffect(FieldEffect.mobHPTagFieldEffect(this)));
                    if (!isUseSpecialSkill()) {
                        MobSkill mobSkill = new MobSkill();
                        mobSkill.setSkillID(MobSkillID.Will.getVal());
                        mobSkill.setLevel(7);
                        mobSkill.setFlip(isFlip());
                        getSkillDelays().add(mobSkill);
                        setSkillDelay(mobSkill.getSkillDelay());
                        getField().broadcast(MobPool.setSkillDelay(this, mobSkill.getSkillDelay(), MobSkillID.Will.getVal(), 7));
                        mobSkill.applyEffect(this);
                        setUseSpecialSkill(true);
                        getTimer().addEvent(() -> setUseSpecialSkill(false), 120000);
                    }
                    return false;
                }
            }
        }
        if (chr.getGuild() != null) {
            GuildSkill guildSkill = chr.getGuild().getSkillById(GuildConstants.TEAM_PLAYERS);
            SkillInfo skillInfo = SkillData.getSkillInfoById(GuildConstants.TEAM_PLAYERS);
            if (guildSkill != null && skillInfo != null) {
                totalDamage += (long) (totalDamage * skillInfo.getValue(SkillStat.nbdR, guildSkill.getLevel()) / 100.0D);
            }
        }
        net.swordie.ms.scripts.event.EventManager.get().onMobHit(chr, this, totalDamage);
        addDamage(chr, totalDamage);
        if (JobConstants.isShade(chr.getJob())) {
            if (chr.getTemporaryStatManager().hasStatBySkillId(Shade.SPIRIT_BOND_MAX_2) && Util.succeedProp(10)) {
                field.drop(Util.makeSet(new DropInfo(4001847, 10000)), field.findFootHoldBelow(pos), pos, chr, 0, 0, false, new ArrayList<>());
            }
        }
        long maxHP = getMaxHp();
        long oldHp = this.hp;
        long newHp = oldHp - (templateID == 9834610 ? 0 : totalDamage);
        if (templateID == BossConstants.DEMIAN_NORMAL_PHASE_1_TEMPLATE_ID || templateID == BossConstants.DEMIAN_HARD_PHASE_1_TEMPLATE_ID) {
            long decHp = (long) (getMaxHp() * 30 / 100.0D);
            if (newHp < decHp) {
                newHp = decHp;
                DemianStigma.startNextPhase(chr, false);
            }
        }
        this.hp = newHp;
        double percDamage = ((double) newHp / maxHP);
        newHp = newHp > Integer.MAX_VALUE ? Integer.MAX_VALUE : newHp;
        if (!hasProperty("triggeredEvent") && templateID / 10000 == 939 && getField().getId() != 863010600) {
            if (oldHp > 0 && newHp <= maxHP * 0.5) {
                int chance = templateID == 9390610 || templateID == 9390611 ? 100 : BossConstants.GOLLUX_DROP_STONE_CHANCE;
                if (new Random().nextInt(101) <= chance) {
                    setProperty("triggeredEvent", true);
                    field.broadcast(FieldPacket.createFallingCatcherGollux(templateID, chr.getPosition()));
                }
            }
        }
        if (isBoss()) {
            SpecialHPBossHandler.onDamage(chr, this, totalDamage, skillID, false);
        }
        if (oldHp > 0 && newHp <= 0) {
            if (BossHpConstants.isBalrogPart(templateID)) {
                this.hp = 1;
                return false;
            }
            if (templateID >= 9833201 && templateID <= 9833205 && (field.getId() == 921172000 || field.getId() == 921172100)) {
                setHp(0);
                return true;
            }
            remove(true);
            chr.getJobHandler().handleMobKilled(this, skillID);
            return true;
        }
        if (isBoss()) {
            handleDamageBoss(chr, field);
        } else {
            final var hpIndicator = MobPacketEncoder.active().hpIndicator(getObjectId(), (byte) (percDamage * 100));
            if (isOwnedBySameField()) {
                getOwner().write(hpIndicator);
            } else {
                field.broadcast(hpIndicator);
            }
        }
        //handleWillHPList(field);
        chr.getJobHandler().handleMobDamaged(this, totalDamage);
        return false;
    }

    private void handleDamageBoss(Char chr, Field field) {
        final int templateID = getTemplateId();
        if (templateID >= 9833201 && templateID <= 9833205 && (field.getId() == 921172000 || field.getId() == 921172100)) {
            int mobsize = 0;
            int flyingmob = 0;
            for (Mob mob1 : field.getMobs()) {
                if (mob1.getTemplateId() >= 9833106 && mob1.getTemplateId() <= 9833111) {
                    if (mob1.getTemplateId() == 9833110 || mob1.getTemplateId() == 9833111) {
                        ++flyingmob;
                    } else {
                        ++mobsize;
                    }
                }
            }
            Position pos44 = null;
            if (mobsize < 8) {
                for (int a2 = mobsize; a2 < 8; ++a2) {
                    final int type = Randomizer.rand(0, 10);
                    if (type == 0) {
                        pos44 = new Position(Randomizer.rand(1452, 1921), -596);
                    } else if (type == 1) {
                        pos44 = new Position(Randomizer.rand(1452, 1921), -440);
                    } else if (type == 2) {
                        pos44 = new Position(Randomizer.rand(1452, 1921), -284);
                    } else if (type == 3) {
                        pos44 = new Position(Randomizer.rand(1452, 1921), -128);
                    } else if (type == 4) {
                        pos44 = new Position(Randomizer.rand(2737, 3208), -596);
                    } else if (type == 5) {
                        pos44 = new Position(Randomizer.rand(2737, 3208), -440);
                    } else if (type == 6) {
                        pos44 = new Position(Randomizer.rand(2737, 3208), -284);
                    } else if (type == 7) {
                        pos44 = new Position(Randomizer.rand(2737, 3208), -128);
                    } else {
                        pos44 = new Position(Randomizer.rand(1597, 3199), 17);
                    }
                    field.spawnMob(Randomizer.rand(9833106, 9833109), pos44.getX(), pos44.getY(), false, 10000000L);
                }
            }
            if (flyingmob < 3) {
                for (int a2 = flyingmob; a2 < 4; ++a2) {
                    final int type = Randomizer.rand(0, 10);
                    if (type == 0) {
                        pos44 = new Position(Randomizer.rand(1452, 1921), -596);
                    } else if (type == 1) {
                        pos44 = new Position(Randomizer.rand(1452, 1921), -440);
                    } else if (type == 2) {
                        pos44 = new Position(Randomizer.rand(1452, 1921), -284);
                    } else if (type == 3) {
                        pos44 = new Position(Randomizer.rand(1452, 1921), -128);
                    } else if (type == 4) {
                        pos44 = new Position(Randomizer.rand(2737, 3208), -596);
                    } else if (type == 5) {
                        pos44 = new Position(Randomizer.rand(2737, 3208), -440);
                    } else if (type == 6) {
                        pos44 = new Position(Randomizer.rand(2737, 3208), -284);
                    } else if (type == 7) {
                        pos44 = new Position(Randomizer.rand(2737, 3208), -128);
                    } else {
                        pos44 = new Position(Randomizer.rand(1597, 3199), 17);
                    }
                    field.spawnMob(Randomizer.isSuccess(40) ? 9833111 : 9833110, pos44.getX(), pos44.getY(), false, 20000000L);
                }
            }
        } else {
            long now = System.currentTimeMillis();
            if (now >= nextAggroSendTime) {
                nextAggroSendTime = now + AGGRO_SEND_INTERVAL_MS;
                final Map<Char, Long> src = getDamageDone();
                if (src != null && !src.isEmpty()) {
                    ArrayList<Map.Entry<Char, Long>> top = computeTopN(src, AGGRO_TOP_N);
                    chr.write(UserLocal.aggroRankInfoName(top, getMaxHp()));
                }
            }

            if (getHpTagColor() != 0) {
                field.broadcast(FieldPacket.fieldEffect(FieldEffect.mobHPTagFieldEffect(this)));
            } else if (net.swordie.ms.connection.protocol.Protocol.active().version()
                    == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) {
                tms282BossHp(field);
            }
        }
    }

    /**
     * TMS 282 (research/tms2823/familiar-mobhp-0930): a boss-flagged mob without its own hpTagColor (e.g. 9305600 /
     * 9305602 with info/link 2220000, Papulatus' 8500003 / 8500004) got no HP packet at all, so its HP never moved on
     * the client until it died. Use the linked template's HP tag when it has one (MobHPTag field effect, colours of
     * the link), otherwise the HP indicator like a normal mob (the 282 client records it when the template allows).
     */
    private void tms282BossHp(Field field) {
        Mob linked = getLink() > 0 && getLink() != getTemplateId() ? MobData.getMobById(getLink()) : null;
        if (linked != null && linked.getHpTagColor() != 0) {
            FieldEffect tag = FieldEffect.mobHPTagFieldEffect(this);
            tag.setArg4(linked.getHpTagColor());
            tag.setArg5(linked.getHpTagBgcolor());
            field.broadcast(FieldPacket.fieldEffect(tag));
            return;
        }
        long maxHp = Math.max(1, getMaxHp());
        field.broadcast(MobPacketEncoder.active().hpIndicator(getObjectId(), (byte) ((double) getHp() / maxHp * 100)));
    }

    private static ArrayList<Map.Entry<Char, Long>> computeTopN(Map<Char, Long> src, int n) {
        PriorityQueue<Map.Entry<Char, Long>> pq =
                new PriorityQueue<>(n, Comparator.comparingLong(Map.Entry::getValue));

        for (Map.Entry<Char, Long> e : src.entrySet()) {
            if (pq.size() < n) pq.offer(e);
            else if (pq.peek() != null && e.getValue() > pq.peek().getValue()) {
                pq.poll();
                pq.offer(e);
            }
        }
        ArrayList<Map.Entry<Char, Long>> top = new ArrayList<>(pq);
        top.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        return top;
    }

    private void handleWillHPList(Field field) {
        switch (getTemplateId()) {
            case 8880300:
            case 8880303:
            case 8880304:
                Mob will = field.getMobByTemplateId(8880300);
                if (will != null) {
                    List<Integer> hps = will.getWillHPlist();
                    field.broadcast(WillPacket.setHp(hps, field, 8880300, 8880303, 8880304));
                }
                break;
            case 8880340:
            case 8880343:
            case 8880344:
                will = field.getMobByTemplateId(8880340);
                if (will != null) {
                    List<Integer> hps = will.getWillHPlist();
                    field.broadcast(WillPacket.setHp(hps, field, 8880340, 8880343, 8880344));
                }
                break;
            case 8880301:
            case 8880341:
                field.broadcast(WillPacket.setHp(getWillHPlist()));
                break;
        }
    }

    public void handleDebuffOnMob(Char chr, SkillInfo si, int skillID, int slv, long randomDamage) {
        if (si != null) {
            chr.getJobHandler().handleDebuffOnMob(chr.getClient(), this, si, skillID, slv, randomDamage);
        }
    }

    /**
     * Damages a mob by a @mob.
     *
     * @param totalDamage the total damage that should be applied to the mob
     */
    public void damageByMob(long totalDamage) {
        long maxHP = getMaxHp();
        long oldHp = getHp();
        long newHp = oldHp - totalDamage;
        setHp(newHp);
        double percDamage = ((double) newHp / maxHP);
        newHp = newHp > Integer.MAX_VALUE ? Integer.MAX_VALUE : newHp;
        if (oldHp > 0 && newHp <= 0) {
            // Boss sponges
            removeWithAnimation();
            if (isBoss() && getHpTagColor() != 0) {
                getField().broadcast(FieldPacket.fieldEffect(FieldEffect.mobHPTagFieldEffect(this)));
            }
        } else if (isBoss() && getHpTagColor() != 0) {
            getField().broadcast(FieldPacket.fieldEffect(FieldEffect.mobHPTagFieldEffect(this)));
        } else {
            getField().broadcast(MobPacketEncoder.active().hpIndicator(getObjectId(), (byte) (percDamage * 100)));
        }
    }

    private void clear() {
        getDamageDone().clear(); // Temporary
        getSkillCooldowns().clear();
        getSkillDelays().clear();
        getProperties().clear();
        getEscortDest().clear();
    }

    // Remove Mob instantly from the Server with no animation Death
    public void remove() {
        getField().removeMob(getObjectId(), DeathType.INSTA_DEATH);
        if (!getRevives().isEmpty()) {
            getTimer().addEvent(() -> doRevive(getRevives()), getRespawnDelay());
        }
        clear();
        setChanged();
        notifyObservers();
    }

    // Remove Mob instantly from the Server with its animation Death
    public void removeWithAnimation() {
        getField().removeMob(getObjectId(), DeathType.ANIMATION_DEATH);
        if (!getRevives().isEmpty()) {
            getTimer().addEvent(() -> doRevive(getRevives()), getRespawnDelay());
        }
        clear();
        setChanged();
        notifyObservers();
    }

    // Remove Mob instantly from the Server with drop, animation Death and by Character
    public void remove(boolean drops) {
        Field field = getField();
        if (isOwnedBySameField()) {
            field.removeMob(getOwner(), getObjectId(), DeathType.ANIMATION_DEATH);
        } else {
            field.removeMob(getObjectId(), DeathType.ANIMATION_DEATH);
        }
        if (GameConstants.isEliteBossTemplate(getTemplateId())) {
            field.drop(new Drop(-1, GameConstants.RARE_TREASURE_CHEST), getPosition());
            if (field.getClock() != null) {
                field.getClock().removeClock();
            }
            if (eliteBossTimer != null) {
                eliteBossTimer.cancel(false);
            }
            field.setEliteState(EliteState.None);
        }
        if (!isSplit()) {
            try {
                distributeExp();
            } catch (Exception e) {
                DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
            }
            if (field.getId() != FieldConstants.SUB_ZERO_HUNT && field.getId() != EventConstants.TERA_BLINK_FIELD) {
                try {
                    initElite();
                } catch (Exception e) {
                    DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
                }
            }
            if (drops) {
                try {
                    dropItems();
                } catch (Exception e) {
                    DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
                }
            }
            var revives = getRevives();
            if (!revives.isEmpty()) {
                getTimer().addEvent(() -> doRevive(revives), getRespawnDelay());
            }
            clear();
            setChanged();
            notifyObservers();
        }
    }

    private void initElite() {
        if (isBoss()) {
            return;
        }
        Field field = getField();
        if (field.getFieldType() == FieldType.DEFAULT && field.getId() != EventConstants.TERA_BLINK_FIELD) {
            if (field.canSpawnElite() && getEliteType() == EliteState.None.getVal() && Util.succeedProp(GameConstants.ELITE_MOB_SPAWN_CHANCE, 1000)) {
                spawnEliteVersion();
            } else if (getEliteType() == EliteState.EliteMob.getVal()) {
                field.incrementEliteKillCount();
                if (field.getKilledElites() >= GameConstants.ELITE_BOSS_REQUIRED_KILLS) {
                    spawnEliteBoss(true);
                } else if (field.getKilledElites() >= GameConstants.ELITE_MOB_DARK_NOTIFICATION) {
                    field.broadcast(WvsContext.weatherEffectNotice(WeatherEffNoticeType.EliteBoss, "你在黑暗能量中感覺到了某種存在……", 8000)); // 8 seconds
                } else {
                    field.broadcast(WvsContext.weatherEffectNotice(WeatherEffNoticeType.EliteBoss, "黑暗能量仍然停留在這裏，讓周圍變得格外陰森。", 8000)); // 8 seconds
                }
            }
        }
    }

    public boolean isAroundLevel(int charLevel) {
        return charLevel >= getLevel() - 20 && charLevel <= getLevel() + 20;
    }

    public boolean isOwnedBySameField() {
        Char owner = getOwner();
        return owner != null && owner.getField() != null && owner.getField().getSN() == getField().getSN();
    }

    private void dropItems() {
        Char chr = isOwnedBySameField() ? getOwner() : getMostDamageChar();
        if (chr == null) {
            chr = Util.getRandomFromCollection(getDamageDone().keySet());
            if (chr == null) {
                return;
            }
        }
        Field field = getField();
        Set<DropInfo> dropInfoSet = getDrops();
        // Add consumable/equip drops based on min(charLv, mobLv)
        int level = Math.min(chr.getLevel(), getForcedMobStat().getLevel());
        dropInfoSet.addAll(ItemConstants.getConsumableMobDrops(level));
        dropInfoSet.addAll(ItemConstants.getGlobalCustomMobDrops(getLevel()));

        List<Integer> filterItems = chr.getFilterItems();
        if (chr.hasQuest(QuestConstants.ITEM_IGNORED_QUEST) && filterItems.isEmpty()) {
            String qrValue = chr.getQRValueByKey(QuestConstants.ITEM_IGNORED_QUEST, "items");
            if (qrValue != null && !qrValue.isEmpty()) {
                filterItems.addAll(Arrays.stream(qrValue.split(",")).map(Integer::parseInt).toList());
            }
        }

        int mesoRate = getTotalMesoRate(chr);
        int dropRate = getTotalDropRate(chr);
        if (!field.getDropsDisabled()) {
            final boolean hasPetVac = Char.hasPetVac(chr);
            final Position dropPos = getPosition().deepCopy();
            final Foothold fh = field.findFootHoldBelow(dropPos);
            if (hasPetVac) {
                getField().drop(dropInfoSet, dropPos, chr, mesoRate, dropRate, filterItems);
            } else {
                if (isBoss()) {
                    field.dropForPartyMember(dropInfoSet, fh, dropPos, chr, this, true, filterItems);
                } else if (isOwnedBySameField()) {
                    field.dropForOwner(dropInfoSet, fh, dropPos, getOwner(), this, isBoss(), filterItems);
                } else {
                    field.drop(dropInfoSet, fh, dropPos, chr, mesoRate, dropRate, false, filterItems);
                }
            }
            initCustomizedDrops(field, chr, filterItems, mesoRate, dropRate);
        }

        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (tsm.hasStat(CharacterTemporaryStat.SoulMP)) tsm.addSoulMPFromMobDeath(this);
    }

    public void initCustomizedDrops(Field field, Char owner, List<Integer> filterItems, int mesoRate, int dropRate) {
        final boolean hasPetVac = Char.hasPetVac(owner);
        final Position dropPos = getPosition().deepCopy();
        final Foothold fh = field.findFootHoldBelow(dropPos);
        final Set<DropInfo> nodeStone = Util.makeSet(new DropInfo(2435902, 5, 1, 1));
        // Elite Monsters Rewards:
        if (getEliteType() == EliteState.EliteMob.getVal()) {
            if (hasPetVac) {
                field.drop(ItemConstants.getEliteMobDrops(), dropPos, owner, mesoRate, dropRate, filterItems);
            } else {
                field.drop(ItemConstants.getEliteMobDrops(), fh, dropPos, owner, mesoRate, dropRate, isBoss(), filterItems);
            }
            field.drop(ItemConstants.getEliteMobDrops(), fh, dropPos, owner, 0, 0, false, filterItems);
        }
        // Arcane Symbol Drop:
        if (GameConstants.isVanishingJourney(getTemplateId())) {
            if (hasPetVac) {
                field.drop(nodeStone, dropPos, owner, mesoRate, dropRate, filterItems);
                field.drop(ItemConstants.arcaneSymbolDrop.getOrDefault(0, new HashSet<>()), dropPos, owner, mesoRate, dropRate, filterItems);
            } else {
                field.drop(nodeStone, fh, dropPos, owner, mesoRate, dropRate, isBoss(), filterItems);
                field.drop(ItemConstants.arcaneSymbolDrop.getOrDefault(0, new HashSet<>()), fh, dropPos, owner, mesoRate, dropRate, isBoss(), filterItems);
            }
        } else if (GameConstants.isChuChuIsLand(getTemplateId())) {
            if (hasPetVac) {
                field.drop(nodeStone, dropPos, owner, mesoRate, dropRate, filterItems);
                field.drop(ItemConstants.arcaneSymbolDrop.getOrDefault(1, new HashSet<>()), dropPos, owner, mesoRate, dropRate, filterItems);
            } else {
                field.drop(nodeStone, fh, dropPos, owner, mesoRate, dropRate, isBoss(), filterItems);
                field.drop(ItemConstants.arcaneSymbolDrop.getOrDefault(1, new HashSet<>()), fh, dropPos, owner, mesoRate, dropRate, isBoss(), filterItems);
            }
        } else if (GameConstants.isLachelein(getTemplateId())) {
            if (hasPetVac) {
                field.drop(nodeStone, dropPos, owner, mesoRate, dropRate, filterItems);
                field.drop(ItemConstants.arcaneSymbolDrop.getOrDefault(2, new HashSet<>()), dropPos, owner, mesoRate, dropRate, filterItems);
            } else {
                field.drop(nodeStone, fh, dropPos, owner, mesoRate, dropRate, isBoss(), filterItems);
                field.drop(ItemConstants.arcaneSymbolDrop.getOrDefault(2, new HashSet<>()), fh, dropPos, owner, mesoRate, dropRate, isBoss(), filterItems);
            }
        } else if (GameConstants.isArcana(getTemplateId())) {
            if (hasPetVac) {
                field.drop(nodeStone, dropPos, owner, mesoRate, dropRate, filterItems);
                field.drop(ItemConstants.arcaneSymbolDrop.getOrDefault(3, new HashSet<>()), dropPos, owner, mesoRate, dropRate, filterItems);
            } else {
                field.drop(nodeStone, fh, dropPos, owner, mesoRate, dropRate, isBoss(), filterItems);
                field.drop(ItemConstants.arcaneSymbolDrop.getOrDefault(3, new HashSet<>()), fh, dropPos, owner, mesoRate, dropRate, isBoss(), filterItems);
            }
        } else if (GameConstants.isMorass(getTemplateId())) {
            if (hasPetVac) {
                field.drop(nodeStone, dropPos, owner, mesoRate, dropRate, filterItems);
                field.drop(ItemConstants.arcaneSymbolDrop.getOrDefault(4, new HashSet<>()), dropPos, owner, mesoRate, dropRate, filterItems);
            } else {
                field.drop(nodeStone, fh, dropPos, owner, mesoRate, dropRate, isBoss(), filterItems);
                field.drop(ItemConstants.arcaneSymbolDrop.getOrDefault(4, new HashSet<>()), fh, dropPos, owner, mesoRate, dropRate, isBoss(), filterItems);
            }
        } else if (GameConstants.isEsfera(getTemplateId())) {
            if (hasPetVac) {
                field.drop(nodeStone, dropPos, owner, mesoRate, dropRate, filterItems);
                field.drop(ItemConstants.arcaneSymbolDrop.getOrDefault(5, new HashSet<>()), dropPos, owner, mesoRate, dropRate, filterItems);
            } else {
                field.drop(nodeStone, fh, dropPos, owner, mesoRate, dropRate, isBoss(), filterItems);
                field.drop(ItemConstants.arcaneSymbolDrop.getOrDefault(5, new HashSet<>()), fh, dropPos, owner, mesoRate, dropRate, isBoss(), filterItems);
            }
        }
    }

    public int getTotalMesoRate(Char chr) {
        if (chr == null) {
            return 0;
        }
        int vipMesoRate = 0;
        int vipGrade = chr.getUser().getVipGrade();
        if (vipGrade > 0) {
            vipMesoRate += 25 * vipGrade;
        }
        int mostDamageCharMesoRate = net.swordie.ms.GameSettings.bonusMeso(chr.getTotalStat(BaseStat.mesoR) + vipMesoRate); // rate.bonus.meso %
        int mesoRateMob = (getTemporaryStat().hasCurrentMobStat(MobStat.AddEffect) ? getTemporaryStat().getCurrentOptionsByMobStat(MobStat.AddEffect).zOption : 0); // Meso Drop Rate
        int mesoRateFromCashDropR = 0;
        for (int i : ItemConstants.CASH_DROP_2X_COUPON) {
            if (chr.getCashInventory().getItemByItemID(i) != null && !chr.getCashInventory().getItemByItemID(i).getDateExpire().isExpired()) {
                mesoRateFromCashDropR = 1;
                break;
            }
        }
        // Elite Channel Advantages:
        boolean buffed = getField() != null && getField().isChannelField() && getField().getChannel() > GameConstants.BUFFED_CHANNELS && !isBoss();
        if (buffed) {
            mesoRateMob *= 2;
        }
        int mesoResult = (mesoRateMob + mostDamageCharMesoRate) * (ServerConfig.MESO_RATE + mesoRateFromCashDropR);
        return (int) (mesoResult * HotTime.mesoMultiplier()); // HotTime (1.0 when inactive)
    }

    public int getTotalDropRate(Char chr) {
        if (chr == null) {
            return 0;
        }
        int mostDamageCharDropRate = net.swordie.ms.GameSettings.bonusDrop(chr.getTotalStat(BaseStat.dropR)); // rate.bonus.drop %
        int dropRateMob = (getTemporaryStat().hasCurrentMobStat(MobStat.AddEffect) ? getTemporaryStat().getCurrentOptionsByMobStat(MobStat.AddEffect).yOption : 0); // Item Drop Rate
        int totalDropRate = dropRateMob + mostDamageCharDropRate;
        int dropRateFromCashDropR = 0;
        for (int i : ItemConstants.CASH_DROP_2X_COUPON) {
            if (chr.getCashInventory().getItemByItemID(i) != null && !chr.getCashInventory().getItemByItemID(i).getDateExpire().isExpired()) {
                dropRateFromCashDropR = 1;
                break;
            }
        }
        // Elite Channel Advantages:
        boolean buffed = getField() != null && getField().isChannelField() && getField().getChannel() > GameConstants.BUFFED_CHANNELS && !isBoss();
        if (buffed) {
            totalDropRate = (int) (totalDropRate * 1.5);
        }
        int result = totalDropRate * (net.swordie.ms.GameSettings.dropRate(isBoss()) + dropRateFromCashDropR); // rate.drop.boss / rate.drop.normal, 0 = rate.drop
        if (chr.hasQuest(100716)) {
            int decDropR = Integer.parseInt(chr.getQRValueByKey(100716, "decDropR"));
            int decDrop = (int) result * decDropR / 100;
            result -= decDrop;
        }
        return (int) (result * HotTime.dropMultiplier()); // HotTime (1.0 when inactive)
    }

    public Map<Char, Long> getDamageDone() {
        return damageDone;
    }

    /**
     * Adds a damage amount to the given Char's current damage. Purely used for keeping track of total damage done by
     * a Char.
     *
     * @param chr    the Char the damage originates from
     * @param damage the damage done
     */
    public void addDamage(Char chr, long damage) {
        long cur = 0;
        if (getDamageDone().containsKey(chr)) {
            cur = getDamageDone().get(chr);
        }
        cur += Math.min(damage, getHp());
        getDamageDone().put(chr, cur);
    }

    public void distributeExp() {
        MobTemporaryStat mts = getTemporaryStat();
        long mobExp = getForcedMobStat().getExp();
        if (getDamageDone().size() == 1 && !isBoss() && getEliteType() == EliteState.None.getVal()) { // SOLO && NORMAL MOBS
            Char chr = getDamageDone().keySet().iterator().next();
            if (chr != null) {
                int level = chr.getLevel();
                long appliedExpPre;
                appliedExpPre = mobExp * net.swordie.ms.GameSettings.expRate(level); // rate.exp.levels, else the 3 band keys
                appliedExpPre = (long) (appliedExpPre * HotTime.currentExpMultiplier()); // HotTime (1.0 when inactive)
                long appliedExpPost = appliedExpPre;
                Tuple<ExpIncreaseInfo, Long> cachedExp = chr.getExpPerMob().get(getTemplateId());
                if (cachedExp != null) {
                    if (cachedExp.getRight() == 0 ||  cachedExp.getRight() + 10 * 60 * 1000L <= System.currentTimeMillis()) { // 10 mins
                        ExpIncreaseInfo eei = calculateExp(chr, mobExp, appliedExpPre, appliedExpPost);
                        // + Exp% MobStats | Cái này phải tách ra vì kỹ năng phải tính lại exp liên tục
                        if (mts.hasCurrentMobStat(MobStat.AddEffect) && mts.getCurrentOptionsByMobStat(MobStat.AddEffect).xOption > 0) { // xOption for Exp%
                            int expIncrease = mts.getCurrentOptionsByMobStat(MobStat.AddEffect).xOption;
                            long mobStatBonusExp = ((appliedExpPre * expIncrease) / 100);
                            eei.setBaseAddExp((int) mobStatBonusExp);
                            eei.setIncEXP(Util.maxInt(eei.getIncEXP() + mobStatBonusExp));
                            appliedExpPost += mobStatBonusExp;
                        }
                        cachedExp.setLeft(eei);
                        cachedExp.setRight(System.currentTimeMillis());
                        chr.addExp(appliedExpPost, eei, true);
                    } else {
                        ExpIncreaseInfo eei = cachedExp.getLeft().deepCopy();
                        long tempAppliedExpPost = appliedExpPost;
                        // + Exp% MobStats | Cái này phải tách ra vì kỹ năng phải tính lại exp liên tục
                        if (mts.hasCurrentMobStat(MobStat.AddEffect) && mts.getCurrentOptionsByMobStat(MobStat.AddEffect).xOption > 0) { // xOption for Exp%
                            int expIncrease = mts.getCurrentOptionsByMobStat(MobStat.AddEffect).xOption;
                            long mobStatBonusExp = ((appliedExpPre * expIncrease) / 100);
                            eei.setBaseAddExp((int) mobStatBonusExp);
                            eei.setIncEXP(Util.maxInt(eei.getIncEXP() + mobStatBonusExp));
                            tempAppliedExpPost += mobStatBonusExp;
                        }
                        chr.addExp(tempAppliedExpPost, eei, true);
                    }
                } else {
                    ExpIncreaseInfo eei = calculateExp(chr, mobExp, appliedExpPre, appliedExpPost);
                    // + Exp% MobStats | Cái này phải tách ra vì kỹ năng phải tính lại exp liên tục
                    if (mts.hasCurrentMobStat(MobStat.AddEffect) && mts.getCurrentOptionsByMobStat(MobStat.AddEffect).xOption > 0) { // xOption for Exp%
                        int expIncrease = mts.getCurrentOptionsByMobStat(MobStat.AddEffect).xOption;
                        long mobStatBonusExp = ((appliedExpPre * expIncrease) / 100);
                        eei.setBaseAddExp((int) mobStatBonusExp);
                        eei.setIncEXP(Util.maxInt(eei.getIncEXP() + mobStatBonusExp));
                        appliedExpPost += mobStatBonusExp;
                    }
                    chr.addExp(appliedExpPost, eei, true);
                    chr.getExpPerMob().put(getTemplateId(), new Tuple<>(eei, System.currentTimeMillis()));
                }
                if (Util.succeedProp(GameConstants.NX_DROP_CHANCE)) {
                    int nx = Math.max(getLevel() * 6, Util.getRandom(1, 5));
                    chr.addMaplePoint(nx);
                }
                Party party = chr.getParty();
                if (party != null) {
                    PartyDamageInfo pdi = new PartyDamageInfo(party, this);
                    pdi.addDamageInfo(chr, 1);
                    pdi.distributeExp();
                }
                chr.handleMobKillForQuest(this);
                chr.getAccount().getMonsterCollection().addMobAndUpdateClient(getTemplateId(), chr);
            }
        } else {
            long totalDamage = getDamageDone().values().stream().mapToLong(l -> l).sum();
            Map<Party, PartyDamageInfo> damagePercPerParty = new HashMap<>();
            for (Char chr : getDamageDone().keySet()) {
                if (chr == null || chr.getHP() <= 0) {
                    continue;
                }
                int level = chr.getLevel();
                double damagePerc = getDamageDone().getOrDefault(chr, 0L) / (double) totalDamage;
                long appliedExpPre = 0;
                appliedExpPre = (long) (mobExp * net.swordie.ms.GameSettings.expRate(level) * damagePerc); // rate.exp.levels, else the 3 band keys
                appliedExpPre = (long) (appliedExpPre * HotTime.currentExpMultiplier()); // HotTime (1.0 when inactive)
                long appliedExpPost = appliedExpPre;
                chr.addExp(appliedExpPost, calculateExp(chr, mobExp, appliedExpPre, appliedExpPost), true);
                if (Util.succeedProp(GameConstants.NX_DROP_CHANCE)) {
                    int nx = Math.max(getLevel() * 6, Util.getRandom(1, 5));
                    chr.addMaplePoint(nx);
                }
                Party party = chr.getParty();
                if (party != null) {
                    if (!damagePercPerParty.containsKey(party)) {
                        damagePercPerParty.put(party, new PartyDamageInfo(party, this));
                    }
                    damagePercPerParty.get(party).addDamageInfo(chr, damagePerc);
                }
                chr.handleMobKillForQuest(this);
                chr.getAccount().getMonsterCollection().addMobAndUpdateClient(getTemplateId(), chr);
            }
            for (PartyDamageInfo pdi : damagePercPerParty.values()) {
                pdi.distributeExp();
            }
        }
    }

    private ExpIncreaseInfo calculateExp(Char chr, Long mobExp, Long appliedExpPre, Long appliedExpPost) {
        ExpIncreaseInfo eei = new ExpIncreaseInfo();

        // Burning Field
        if (chr.getBurningFieldLevel() > 0) {
            int burningFieldBonusExp = (int) (appliedExpPre * chr.getBonusExpByBurningFieldLevel() / 100);
            eei.setRestFieldBonusExp(burningFieldBonusExp);
            eei.setRestFieldExpRate(chr.getBonusExpByBurningFieldLevel());
            appliedExpPost += burningFieldBonusExp;
        }

        // + Exp% by Item
        for (Item item : chr.getEquippedInventory().getItems()) {
            double incBonusExpByItem = ItemConstants.getBonusExpByItem(item.getItemId());
            if (incBonusExpByItem != 0) {
                int incExpR = (int) (appliedExpPre * incBonusExpByItem);
                eei.setItemBonusExp(eei.getItemBonusExp() + incExpR);
                appliedExpPost += incExpR;
            }
        }

        // + Exp% by Core
        if (chr.getField().getId() >= GameConstants.EVOLVING_LINK_MAP_1 && chr.getField().getId() <= GameConstants.EVOLVING_LINK_MAP_9) {
            Core mobExpCore = chr.getEquippedCores().stream()
                    .filter(c -> c.getCoreID() >= 3602000 && c.getCoreID() <= 3602002).findAny().orElse(null);
            if (mobExpCore != null) {
                int coreExpR = (int) (appliedExpPre * (GameConstants.getCoreValue(mobExpCore.getCoreID()) / 100.0D));
                eei.setInstallItemBonusExp(coreExpR);
                appliedExpPost += coreExpR;
            }
        }

        // + Exp% by Guild Skills
        if (chr.getGuild() != null && getLevel() <= 200 && getLevel() >= 101) {
            GuildSkill guildSkill = chr.getGuild().getSkillById(GuildConstants.GUILD_EXPERTISE);
            SkillInfo skillInfo = SkillData.getSkillInfoById(GuildConstants.GUILD_EXPERTISE);
            if (guildSkill != null && skillInfo != null) {
                int incExpR = (int) (appliedExpPre * skillInfo.getValue(SkillStat.expGuild, guildSkill.getLevel()));
                eei.setPsdBonusExpRate(eei.getPsdBonusExpRate() + incExpR);
                appliedExpPost += incExpR;
            }
        }

        // + Exp% by Use Coupon
        if (chr.getTotalStat(BaseStat.expR) > 0) {
            int useExpR = (int) (appliedExpPre * (chr.getTotalStat(BaseStat.expR) / 100D));
            eei.setIndieBonusExp(eei.getIndieBonusExp() + useExpR);
            appliedExpPost += useExpR;
        }

        // + Exp% by Cash Coupon
        for (int id : ItemConstants.EXP_2X_COUPON) {
            if (chr.hasItem(id)) {
                appliedExpPost += appliedExpPre;
                eei.setIndieBonusExp((int) (eei.getIndieBonusExp() + appliedExpPre));
                break; //so coupons won't stack
            }
        }

        eei.setLastHit(true);
        eei.setIncEXP(Util.maxInt(appliedExpPost));
        return eei;
    }

    public Char getMostDamageChar() {
        Tuple<Char, Long> max = new Tuple<>(null, (long) -1);
        for (Map.Entry<Char, Long> entry : getDamageDone().entrySet()) {
            Char chr = entry.getKey();
            long damage = entry.getValue();
            if (max == null || damage > max.getRight()) {
                max.setLeft(chr);
                max.setRight(damage);
            }
        }
        return max.getLeft();
    }

    public List<MobSkill> getSkills() {
        return skills;
    }

    public void addSkill(MobSkill skill) {
        getSkills().add(skill);
    }

    public List<MobSkill> getAttacks() {
        return attacks;
    }

    public void addAttack(MobSkill mobSkill) {
        getAttacks().add(mobSkill);
    }

    public MobSkill getAttackById(int attackID) {
        return Util.findWithPred(getAttacks(), att -> att.getSkillSN() == attackID);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Mob mob) {
            return mob.getTemplateId() == getTemplateId() && mob.getObjectId() == getObjectId() && mob.getField().equals(getField());
        }
        return false;
    }

    public Set<Integer> getQuests() {
        return quests;
    }

    public void setQuests(Set<Integer> quests) {
        this.quests = quests;
    }

    public void addQuest(int questID) {
        getQuests().add(questID);
    }

    public void soulSplitMob(Char chr, Mob origin, int duration, int skillID) {
        Field field = chr.getField();
        Position position = origin.getPosition();
        EnumMap<MobStat, Option> map = new EnumMap<>(MobStat.class);

        Mob copy = MobData.getMobDeepCopyById(origin.getTemplateId());
        copy.setSplit(true);
        copy.setPosition(position);
        copy.setHp(origin.getHp());
        copy.setMaxHp(origin.getMaxHp());
        copy.setMp(origin.getMp());
        copy.setMaxMp(origin.getMaxMp());
        copy.setNotRespawnable(true);
        copy.setField(field);

        copy.setSplitLink(origin.getObjectId());
        //copy.setDrops(null);

        field.spawnLife(copy, null);

        MobTemporaryStat mtsCopy = copy.getTemporaryStat();
        Option o1 = new Option();
        Option o2 = new Option();
        o1.nOption = 50;
        o1.rOption = skillID;
        o1.tOption = duration;
        //map.put(MobStat.SeperateSoulC, o1);
        o2.nOption = 70;
        o2.rOption = skillID;
        o2.tOption = duration;
        //map.put(MobStat.SeperateSoulP, o2);
        //mtsCopy.addStatOptions(this, map);
        getTimer().addEvent(() -> removeSoulSplitLife(chr, origin, copy), duration, TimeUnit.SECONDS);
    }

    public void removeSoulSplitLife(Char chr, Mob origin, Mob copy) {
        chr.getField().removeLife(copy.getObjectId(), true);
        chr.getField().broadcast(MobPacketEncoder.active().leaveField(copy.getObjectId(), DeathType.ANIMATION_DEATH));
        origin.setSplit(false);
    }

    public boolean isAlive() {
        return getHp() > 0;
    }

    private void doRevive(Set<Integer> revives) {
        List<Integer> chaosPinkBeanMob = List.of(
                8820213, 8820214, //Chaos Solomon the Wise
                8820215, 8820216, //Chaos Rex the Wise
                8820225, 8820226, 8820227, //Chaos Ariel
                8820217, 8820218, 8820219, 8820220, //Chaos Hugin
                8820221, 8820222, 8820223, 8820224  //Chaos Munin
        );
        for (int reviveTemplateID : revives) {
            //Chaos Zakum Custom Fix
            if (reviveTemplateID >= 8800130 && reviveTemplateID <= 8800137) {
                continue;
            }
            //Chaos Horntail Custom Fix - Fuck Nexon
            if (reviveTemplateID == 8810118) {
                reviveTemplateID = 8810122;
            }
            Mob mob = MobData.getMobDeepCopyById(reviveTemplateID);
            if (mob == null) {
                continue;
            }
            if (reviveTemplateID == 8810122) {
                mob.setHp(10000000000L);
                mob.setMaxHp(10000000000L);
            }//Chaos PinkBean Fix - Fuck Nexon
            else if (chaosPinkBeanMob.contains(reviveTemplateID)) {
                continue;
            } else if (reviveTemplateID == 8820300) {
                mob.setRespawnDelay(getRespawnDelay());
                mob.setHp(2100000000L);
                mob.setMaxHp(2100000000L);
                mob.setRevives(MobData.getMobDeepCopyById(8820110).getRevives());
            } else if (reviveTemplateID == 8820301) {
                mob.setRespawnDelay(getRespawnDelay());
                mob.setHp(4200000000L);
                mob.setMaxHp(4200000000L);
                mob.setRevives(MobData.getMobDeepCopyById(8820111).getRevives());
            } else if (reviveTemplateID == 8820302) {
                mob.setRespawnDelay(getRespawnDelay());
                mob.setHp(6300000000L);
                mob.setMaxHp(6300000000L);
                mob.setRevives(MobData.getMobDeepCopyById(8820112).getRevives());
            } else if (reviveTemplateID == 8820303) {
                mob.setRespawnDelay(getRespawnDelay());
                mob.setHp(8400000000L);
                mob.setMaxHp(8400000000L);
                mob.setRevives(MobData.getMobDeepCopyById(8820113).getRevives());
            } else if (reviveTemplateID == 8820304) {
                mob.setRespawnDelay(getRespawnDelay());
                mob.setHp(14700000000L);
                mob.setMaxHp(14700000000L);
                mob.setRevives(MobData.getMobDeepCopyById(8820114).getRevives());
            }
            mob.setNotRespawnable(true);
            mob.setPosition(getPosition());
            getField().spawnMob(mob.getTemplateId(), mob.getHomePosition().getX(), mob.getHomePosition().getY(), false);
        }
    }

    private Map<Integer, Long> getSkillCooldowns() {
        return skillCooldowns;
    }

    public boolean hasSkillOffCooldown(int skillID, int slv) {
        return System.currentTimeMillis() >= getSkillCooldowns().getOrDefault(skillID | (slv << 16), Long.MIN_VALUE);
    }

    public boolean hasAttackOffCooldown(int attackID) {
        return System.currentTimeMillis() >= getSkillCooldowns().getOrDefault(-attackID, Long.MIN_VALUE);
    }

    public void putSkillCooldown(int skillID, int slv, long nextUseableTime) {
        getSkillCooldowns().put(skillID | (slv << 16), nextUseableTime);
    }

    public void putAttackOnCooldown(int skillID, int delayForNextAttack) {
        getSkillCooldowns().put(-skillID, System.currentTimeMillis() + delayForNextAttack);
    }

    public boolean hasSkillDelayExpired() {
        return System.currentTimeMillis() > getNextPossibleSkillTime();
    }

    /**
     * Sets when a next skill can be used (in ms from current time).
     *
     * @param delay The delay until the next skill can be used
     */
    public void setSkillDelay(long delay) {
        setNextPossibleSkillTime(System.currentTimeMillis() + delay);
    }

    private long getNextPossibleSkillTime() {
        return nextPossibleSkillTime;
    }

    private void setNextPossibleSkillTime(long nextPossibleSkillTime) {
        this.nextPossibleSkillTime = nextPossibleSkillTime;
    }

    @Override
    public void broadcastSpawnPacket(Char onlyChar) {
        MobTemporaryStat mts = getTemporaryStat();
        if (mts == null) {
            mts = new MobTemporaryStat();
        }
        Field field = getField();
        Position pos = getPosition();
        Foothold fh = field.getFootholdById(getFh());
        if (fh == null) {
            fh = field.findFootHoldBelow(pos);
        }
        if (fh == null) {
            // Edge case where the mob is spawned on some weird foothold
            fh = Util.getRandomFromCollection(field.getFootholds());
            if (fh == null) {
                return;
            }
            pos = fh.getRandomPosition();
            setPosition(pos);
            setHomeFoothold(fh.deepCopy());
            setCurFoodhold(fh.deepCopy());
        } else {
            setHomeFoothold(fh.deepCopy());
            setCurFoodhold(fh.deepCopy());
        }
        Char owner = getOwner();
        final boolean isOwnerBySameField = isOwnedBySameField();
        final TreeMap<MobStat, Option> map = mts.getCurrentStatVals();
        final Set<BurnedInfo> burnedInfos = mts.getAllBurns();
        final String linkteam = mts.getLinkTeam();
        final int objectID = getObjectId();
        final int controllerID = getControllerID();
        final Set<Char> chars = field.getChars();
        final byte calcDamageIndex = getCalcDamageIndex();
        final MobPacketEncoder mobPackets = MobPacketEncoder.active();
        if (isOwnerBySameField) {
            owner.write(mobPackets.enterField(this, linkteam));
            owner.write(mobPackets.changeController(this, linkteam));
            owner.write(MobPool.nextTargetFromSvr(this, controllerID));
            return;
        }
        if (onlyChar == null) {
            for (Char chr : chars) {
                if (chr != null) {
                    chr.write(mobPackets.enterField(this, linkteam));
                    chr.write(controllerID == chr.getId() ? mobPackets.changeController(this, linkteam) : mobPackets.changeController(objectID));
                    chr.write(MobPool.nextTargetFromSvr(this, controllerID));
                }
            }
        } else {
            onlyChar.write(mobPackets.enterField(this, linkteam));
            onlyChar.write(controllerID == onlyChar.getId() ? mobPackets.changeController(this, linkteam) : mobPackets.changeController(objectID));
            onlyChar.write(MobPool.nextTargetFromSvr(this, controllerID));
        }
    }

    @Override
    public void notifyControllerChange() {
        Field field = getField();
        if (field == null) {
            return;
        }
        final TreeMap<MobStat, Option> map = getTemporaryStat().getCurrentStatVals();
        final Set<BurnedInfo > burnedInfos = getTemporaryStat().getAllBurns();
        final String linkteam = getTemporaryStat().getLinkTeam();
        final int objectID = getObjectId();
        final int controllerID = getControllerID();
        final Set<Char> chars = field.getChars();
        final byte calcDamageIndex = getCalcDamageIndex();
        final MobPacketEncoder mobPackets = MobPacketEncoder.active();
        for (Char chr : chars) {
            if (chr != null) {
                chr.write(controllerID == chr.getId() ? mobPackets.changeController(this, linkteam) : mobPackets.changeController(objectID));
                chr.write(MobPool.nextTargetFromSvr(this, controllerID));
            }
        }
    }

    public void spawnEliteVersion() {
        Mob elite = MobData.getMobDeepCopyById(getTemplateId());
        elite.setHomePosition(getPosition().deepCopy());
        elite.setPosition(getPosition().deepCopy());
        elite.setCurFoodhold(getCurFoodhold().deepCopy());
        elite.setHomeFoothold(getCurFoodhold().deepCopy());
        elite.setNotRespawnable(true);
        List<Triple<Integer, Double, Double>> eliteInfos = GameConstants.getEliteInfoByMobLevel(elite.getForcedMobStat().getLevel());
        Triple<Integer, Double, Double> eliteInfo = Util.getRandomFromCollection(eliteInfos);
        int eliteGrade = eliteInfo.getLeft();
        long newHp = (long) (eliteInfo.getMiddle() * elite.getMaxHp());
        long newExp = (long) (eliteInfo.getRight() * elite.getForcedMobStat().getExp());
        elite.setEliteType(EliteState.EliteMob.getVal());
        elite.setEliteGrade(eliteGrade);
        Map<Integer, Integer> possibleSkillsMap = SkillData.getEliteMobSkillsByGrade(eliteGrade);
        if (possibleSkillsMap != null) {
            List<Tuple<Integer, Integer>> possibleSkills = new ArrayList<>();
            possibleSkillsMap.forEach((k, v) -> possibleSkills.add(new Tuple(k, v)));
            for (int i = 0; i < GameConstants.ELITE_MOB_SKILL_COUNT; i++) {
                Tuple<Integer, Integer> randomSkill = Util.getRandomFromCollection(possibleSkills);
                elite.addEliteSkill(randomSkill.getLeft(), randomSkill.getRight());
                possibleSkills.removeIf(x -> x.equals(randomSkill));
            }
        }
        elite.setMaxHp(newHp);
        elite.setHp(newHp);
        elite.getForcedMobStat().setExp(newExp);
        getField().setNextEliteSpawnTime(System.currentTimeMillis() + GameConstants.ELITE_MOB_RESPAWN_TIME * 1000);
        getField().spawnLife(elite, null);
    }

    public void spawnEliteMobRuneOfDarkness() {
        Mob elite = MobData.getMobDeepCopyById(getTemplateId());
        elite.setHomePosition(getPosition().deepCopy());
        elite.setPosition(getPosition().deepCopy());
        elite.setCurFoodhold(getCurFoodhold().deepCopy());
        elite.setHomeFoothold(getCurFoodhold().deepCopy());
        elite.setNotRespawnable(true);
        List<Triple<Integer, Double, Double>> eliteInfos = GameConstants.getEliteInfoByMobLevel(elite.getForcedMobStat().getLevel());
        Triple<Integer, Double, Double> eliteInfo = Util.getRandomFromCollection(eliteInfos);
        int eliteGrade = eliteInfo.getLeft();
        long newHp = (long) (eliteInfo.getMiddle() * elite.getMaxHp());
        long newExp = (long) (eliteInfo.getRight() * elite.getForcedMobStat().getExp());
        elite.setEliteType(EliteState.EliteMob.getVal());
        elite.setEliteGrade(eliteGrade);
        Map<Integer, Integer> possibleSkillsMap = SkillData.getEliteMobSkillsByGrade(eliteGrade);
        if (possibleSkillsMap != null) {
            List<Tuple<Integer, Integer>> possibleSkills = new ArrayList<>();
            possibleSkillsMap.forEach((k, v) -> possibleSkills.add(new Tuple(k, v)));
            for (int i = 0; i < GameConstants.ELITE_MOB_SKILL_COUNT; i++) {
                Tuple<Integer, Integer> randomSkill = Util.getRandomFromCollection(possibleSkills);
                elite.addEliteSkill(randomSkill.getLeft(), randomSkill.getRight());
                possibleSkills.removeIf(x -> x.equals(randomSkill));
            }
        }
        elite.setMaxHp(newHp);
        elite.setHp(newHp);
        elite.getForcedMobStat().setExp(newExp);
        getField().spawnLife(elite, null);
    }

    public void spawnEliteBoss(boolean hasEliteMobs) {
        int eliteBossGrade = 0;
        if (hasEliteMobs) {
            // Triệu hồi 2 con elite monster nhỏ
            for (int i = 0; i < 2; i++) {
                Mob eliteMonster = MobData.getMobDeepCopyById(getTemplateId());
                eliteMonster.setHomePosition(getPosition().deepCopy());
                eliteMonster.setPosition(getPosition().deepCopy());
                eliteMonster.setCurFoodhold(getCurFoodhold().deepCopy());
                eliteMonster.setHomeFoothold(getCurFoodhold().deepCopy());
                eliteMonster.setNotRespawnable(true);
                List<Triple<Integer, Double, Double>> eliteInfos = GameConstants.getEliteInfoByMobLevel(eliteMonster.getForcedMobStat().getLevel());
                Triple<Integer, Double, Double> eliteInfo = Util.getRandomFromCollection(eliteInfos);
                int eliteGrade = eliteInfo.getLeft();
                long newHp = (long) (eliteInfo.getMiddle() * eliteMonster.getMaxHp());
                long newExp = (long) (eliteInfo.getRight() * eliteMonster.getForcedMobStat().getExp());
                eliteMonster.setEliteType(EliteState.EliteMob.getVal());
                eliteMonster.setEliteGrade(eliteGrade);
                eliteBossGrade = eliteGrade;
                Map<Integer, Integer> possibleSkillsMap = SkillData.getEliteMobSkillsByGrade(eliteGrade);
                if (possibleSkillsMap != null) {
                    List<Tuple<Integer, Integer>> possibleSkills = new ArrayList<>();
                    possibleSkillsMap.forEach((k, v) -> possibleSkills.add(new Tuple<>(k, v)));
                    for (int j = 0; j < GameConstants.ELITE_MOB_SKILL_COUNT; j++) {
                        Tuple<Integer, Integer> randomSkill = Util.getRandomFromCollection(possibleSkills);
                        eliteMonster.addEliteSkill(randomSkill.getLeft(), randomSkill.getRight());
                        possibleSkills.removeIf(x -> x.equals(randomSkill));
                    }
                }
                eliteMonster.setMaxHp(newHp);
                eliteMonster.setHp(newHp);
                eliteMonster.getForcedMobStat().setExp(newExp);
                getField().spawnLife(eliteMonster, null);
            }
        }
        // Triệu hồi Elite Boss
        getField().setKilledElites(getField().getKilledElites() % GameConstants.ELITE_BOSS_REQUIRED_KILLS);
        int bossTemplate = Util.getRandom(9303130, 9303139);
        Mob eliteBoss = MobData.getMobDeepCopyById(bossTemplate);
        eliteBoss.setEliteType(EliteState.EliteBoss.getVal());
        eliteBoss.setNotRespawnable(true);
        long hp = Math.min(MobData.getMobDeepCopyById(getTemplateId()).getMaxHp() * GameConstants.ELITE_BOSS_HP_RATE, Long.MAX_VALUE);
        eliteBoss.setMaxHp(hp);
        eliteBoss.setHp(eliteBoss.getMaxHp());
        eliteBoss.setHomeFoothold(getCurFoodhold().deepCopy());
        eliteBoss.setCurFoodhold(getCurFoodhold().deepCopy());
        eliteBoss.setPosition(getPosition().deepCopy());
        eliteBoss.setHomePosition(getPosition().deepCopy());
        eliteBoss.setScale(GameConstants.ELITE_BOSS_SCALE);
        eliteBoss.setPad(getPad() * GameConstants.ELITE_BOSS_DAMAGE_MULTIPLIER);
        eliteBoss.setMad(getMad() * GameConstants.ELITE_BOSS_DAMAGE_MULTIPLIER);
        eliteBoss.getForcedMobStat().setPdr(getPdr() * GameConstants.ELITE_BOSS_DEFENSE_MULTIPLIER);
        eliteBoss.getForcedMobStat().setMdr(getMdr() * GameConstants.ELITE_BOSS_DEFENSE_MULTIPLIER);
        Map<Integer, Integer> possibleSkillsMap = SkillData.getEliteMobSkillsByGrade(eliteBossGrade);
        if (possibleSkillsMap != null) {
            List<Tuple<Integer, Integer>> possibleSkills = new ArrayList<>();
            possibleSkillsMap.forEach((k, v) -> possibleSkills.add(new Tuple<>(k, v)));
            for (int i = 0; i < GameConstants.ELITE_MOB_SKILL_COUNT; i++) {
                Tuple<Integer, Integer> randomSkill = Util.getRandomFromCollection(possibleSkills);
                eliteBoss.addEliteSkill(randomSkill.getLeft(), randomSkill.getRight());
                possibleSkills.removeIf(x -> x.equals(randomSkill));
            }
        }
        getField().spawnLife(eliteBoss, null);
        getField().setEliteState(EliteState.EliteBoss);
        Clock clock = new Clock(ClockType.SecondsClock, getField(), 1800);
        getField().setClock(clock);
        this.eliteBossTimer = getTimer().addFixedRateEvent(this::removeEliteBossTimeExpire, 1800, 10, TimeUnit.SECONDS, false);
        getField().broadcast(FieldPacket.eliteState(EliteState.EliteBoss, false, GameConstants.ELITE_BOSS_BGM,
                null, null));
        // Thông báo khi Elite Boss được triệu hồi
        WeatherEffNoticeType went = null;
        String msg = null;
        switch (bossTemplate) {
            case 9303130, 9303135 -> {
                went = WeatherEffNoticeType.EliteBoss_Warrior;
                msg = "黑騎士魔凱丁：為了榮耀！";
            }
            case 9303131, 9303136 -> {
                went = WeatherEffNoticeType.EliteBoss_Mage;
                msg = "瘋魔法師卡力阿因：在我面前低頭吧！";
            }
            case 9303132, 9303137 -> {
                went = WeatherEffNoticeType.EliteBoss_Thief;
                msg = "突擊型 CQ57：發現目標。開始執行排除程序。";
            }
            case 9303133, 9303138 -> {
                went = WeatherEffNoticeType.EliteBoss_Bowman;
                msg = "獵頭者朱萊：狩獵開始了！";
            }
            case 9303134, 9303139 -> {
                went = WeatherEffNoticeType.EliteBoss_Pirate;
                msg = "打手普列德：來吧，派對開始囉！";
            }
        }
        if (went != null) {
            getField().broadcast(WvsContext.weatherEffectNotice(went, msg, 8000)); // 8 seconds
        }
    }

    public void removeEliteBossTimeExpire() {
        if (getField().getClock() != null) {
            getField().getClock().removeClock();
        }
        for (Mob mob : getField().getMobs()) {
            getField().removeMob(mob.getObjectId());
        }
        getField().setEliteState(EliteState.None);
        getField().broadcast(FieldPacket.eliteState(EliteState.None, false, null, null, null));
        if (this.eliteBossTimer != null) {
            this.eliteBossTimer.cancel(false);
        }
    }

    public List<Tuple<Integer, Integer>> getEliteSkills() {
        return eliteSkills;
    }

    public void addEliteSkill(int skillID, int skillLevel) {
        MobSkill ms = new MobSkill();
        ms.setSkillSN(-1);
        ms.setSkillID(skillID);
        ms.setLevel(skillLevel);
        addSkill(ms);
        getEliteSkills().add(new Tuple<>(skillID, skillID));
    }

    public boolean isSelfDestruction() {
        return selfDestruction;
    }

    public void setSelfDestruction(boolean selfDestruction) {
        this.selfDestruction = selfDestruction;
    }

    public List<MobSkill> getSkillDelays() {
        return skillDelays;
    }

    public boolean isInAttack() {
        return inAttack;
    }

    public void setInAttack(boolean inAttack) {
        this.inAttack = inAttack;
    }

    public boolean isEscortMob() {
        return isEscortMob;
    }

    public void setEscortMob(boolean isEscortMob) {
        this.isEscortMob = isEscortMob;
    }

    public List<EscortDest> getEscortDest() {
        return escortDest;
    }

    public void addEscortDest(int destPosX, int destPosY, int attr) {
        addEscortDest(destPosX, destPosY, attr, 0, 0);
    }

    public void addEscortDest(int destPosX, int destPosY, int attr, int mass, int stopDuration) {
        this.escortDest.add(new EscortDest(destPosX, destPosY, attr, mass, stopDuration));
    }

    public int getCurrentDestIndex() {
        return currentDestIndex;
    }

    public void setCurrentDestIndex(int currentDestIndex) {
        this.currentDestIndex = currentDestIndex;
    }

    public int getEscortStopDuration() {
        return escortStopDuration;
    }

    public void setEscortStopDuration(int escortStopDuration) {
        this.escortStopDuration = escortStopDuration;
    }

    public void clearEscortDest() {
        this.escortDest = new ArrayList<>();
    }

    public void escortFullPath(int oldAttr) {
        getField().broadcast(MobPool.escortFullPath(this, oldAttr, false));
    }

    public boolean isFinishedEscort() {
        return this.escortDest.isEmpty();
    }

    public void encodeInit(OutPacket outPacket) {
        // CMob::Init
        outPacket.encodePosition(getPosition());
        outPacket.encodeByte(getMoveAction());
        if (templateId == 8910000 || templateId == 8910100 || templateId == 9990033) { // is_banban_boss
            outPacket.encodeByte(0); // fake?
        }
        outPacket.encodeShort(getCurFoodhold().getId());
        outPacket.encodeShort(getHomeFoothold().getId());
        outPacket.encodeByte(0); // v263
        byte appearType = getAppearType();
        outPacket.encodeByte(appearType);
        if (appearType == -3 || appearType == -6 || appearType >= 0) {
            // init -> -2, -1 else
            outPacket.encodeInt(getOption());
        }
        outPacket.encodeByte(getTeamForMCarnival()); // false = blue; true = red
        outPacket.encodeInt(getScale()); // 100?
        outPacket.encodeLong(getHp());
        outPacket.encodeInt(getEffectItemID());
        if (isPatrolMob()) {
            outPacket.encodeInt(getPosition().getX() - getRange());
            outPacket.encodeInt(getPosition().getX() + getRange());
            outPacket.encodeInt(getDetectX());
            outPacket.encodeInt(getSenseX());
        }
        outPacket.encodeInt(0); // ?
        outPacket.encodeInt(getPhase());
        outPacket.encodeInt(getCurZoneDataType());
        outPacket.encodeInt(getRefImgMobID());
        outPacket.encodeInt(getAfterAttack());
        outPacket.encodeInt(getCurrentAction());
        outPacket.encodeInt(0);
        outPacket.encodeByte(Util.getRandom(0, Byte.MAX_VALUE));
        outPacket.encodeByte(0); // bool => int
        outPacket.encodeByte(0);
        int size = 0;
        outPacket.encodeInt(size);
        for (int i = 0; i < size; i++) {
            outPacket.encodeInt(0); // ?
            outPacket.encodeInt(0); // extra time?
        }
        outPacket.encodeInt(getScale()); // 100?
        outPacket.encodeInt(getEliteGrade());
        if (getEliteGrade() >= 0) {
            outPacket.encodeInt(getEliteSkills().size());
            for (Tuple<Integer, Integer> eliteSkill : getEliteSkills()) {
                outPacket.encodeInt(eliteSkill.getLeft()); // first skillID?
                outPacket.encodeInt(eliteSkill.getRight()); // second skillID?
            }
            outPacket.encodeInt(getEliteType()); // 1 normal, 3 elite boss probably
        }
        ShootingMoveStat sms = getShootingMoveStat();
        outPacket.encodeByte(sms != null);
        if (sms != null) {
            sms.encode(outPacket);
        }
        outPacket.encodeInt(0); // ?
        outPacket.encodeInt(0); // 2 ints
        List<Integer> mobs = new ArrayList<>();
        if (templateId == 8880101 || templateId == 8880111) {
            for (Mob m : getField().getMobs()) {
                if (m.templateId == 8880102) {
                    mobs.add(m.getObjectId());
                }
            }
            outPacket.encodeInt(mobs.size());
            for (Integer objectId : mobs) {
                outPacket.encodeInt(5); // nType
                outPacket.encodeInt(objectId); // key?
            }
        } else {
            outPacket.encodeInt(0);
        }
        outPacket.encodeByte(0);// bool encode 120 bytes
        outPacket.encodeString("");
        if (templateId == 8880102 || templateId == 8880605) {
            outPacket.encodeInt(getTargetFromSvr());
        }
        // CMob::OnAttackBlock
        outPacket.encodeInt(0);

        outPacket.encodeByte(0); // true => sub_140524C30

        outPacket.encodeByte(0);

        outPacket.encodeInt(0);

        boolean isSpecial = templateId == 8880181 || templateId == 8880187 // Golem
                || templateId == 8880010 || templateId == 8880002 || templateId == 8880000 // Magnus
                || templateId == 8881100 || templateId == 8881200 || templateId == 8881300 // Lotus
                ;
        outPacket.encodeInt(isSpecial ? 1 : 0);
        if (isSpecial) {
            switch (templateId) {
                case 8880000: // Hard Magnus
                case 8880002: // Normal magnus
                case 8880010: // Easy Magnus
                    outPacket.encodeInt(60);
                    break;
                case 8881100: // Easy Lotus
                case 8881200: // Normal Lotus
                case 8881300: // Extreme Lotus
                    outPacket.encodeInt(225);
                    break;
                default:
                    outPacket.encodeInt(200);
                    break;
            }
        }

        outPacket.encodeInt(0); // true => sub_14059D7F0

        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);

        outPacket.encodeInt(0); // true => vòng lặp integer

        outPacket.encodeInt(getControllerID());
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);

        outPacket.encodeShort(0);
        if (templateId == 8881100
                || templateId == 8881111
                || templateId == 8881104
                || templateId == 8881110
        ) {
            outPacket.encodeArr(new byte[14]);
        }
        if (hasHitParts(templateId)) {
            outPacket.encodeByte(1);
            switch (templateId) {
                case 8800022:
                case 8800002:
                case 8800102:
                    outPacket.encodeString("box_body");
                    break;
                case 8800023:
                case 8800027:
                case 8800003:
                case 8800007:
                case 8800103:
                case 8800107:
                case 8800141:
                case 8800145:
                    outPacket.encodeString("R_Arm_001_bound");
                    break;
                case 8800024:
                case 8800028:
                case 8800004:
                case 8800008:
                case 8800104:
                case 8800108:
                case 8800142:
                case 8800146:
                    outPacket.encodeString("R_Arm_002_bound");
                    break;
                case 8800005:
                case 8800009:
                case 8800025:
                case 8800029:
                case 8800105:
                case 8800109:
                case 8800143:
                case 8800147:
                    outPacket.encodeString("R_Arm_003_bound");
                    break;
                case 8800026:
                case 8800030:
                case 8800006:
                case 8800010:
                case 8800106:
                case 8800110:
                case 8800144:
                case 8800148:
                    outPacket.encodeString("R_Arm_004_bound");
                    break;
            }
            outPacket.encodeInt(0);
            outPacket.encodeByte(0);
        }
    }

    private boolean hasHitParts(int templateID) {
        switch (templateID) {
            case 8800022:
            case 8800002:
            case 8800102:
            case 8800023:
            case 8800027:
            case 8800003:
            case 8800007:
            case 8800103:
            case 8800107:
            case 8800141:
            case 8800145:
            case 8800024:
            case 8800028:
            case 8800004:
            case 8800008:
            case 8800104:
            case 8800108:
            case 8800142:
            case 8800146:
            case 8800005:
            case 8800009:
            case 8800025:
            case 8800029:
            case 8800105:
            case 8800109:
            case 8800143:
            case 8800147:
            case 8800026:
            case 8800030:
            case 8800006:
            case 8800010:
            case 8800106:
            case 8800110:
            case 8800144:
            case 8800148:
                return true;
        }
        return false;
    }
    public long handleDamageReflect(Char chr, OutHeader outHeader, int skillID, long totalDamage) {
        MobTemporaryStat mts = getTemporaryStat();
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        SkillInfo si = SkillData.getSkillInfoById(skillID);
        boolean hasIgnoreCounterCts = tsm.hasStat(CharacterTemporaryStat.IgnoreAllCounter) || tsm.hasStat(CharacterTemporaryStat.NotDamaged) || tsm.hasStat(CharacterTemporaryStat.AranBoostEndHunt);
        boolean ignoreCounter = hasIgnoreCounterCts || (si != null && si.isIgnoreCounter());
        if (tsm.hasStatBySkillId(Shade.SPIRIT_FLOW)) {
            ignoreCounter = true;
        }
        if (skillID == 14121003 || skillID == FirePoison.INFERNO_AURA || skillID == FirePoison.HEXA_INFERNO_AURA) {
            ignoreCounter = true;
        }
        if ((mts.hasCurrentMobStat(MobStat.PCounter) || mts.hasCurrentMobStat(MobStat.MCounter)) && !ignoreCounter) {
            Option o1 = mts.getCurrentOptionsByMobStat(MobStat.PCounter);
            Option o2 = mts.getCurrentOptionsByMobStat(MobStat.MCounter);
            int damagePerc = 0, prop = 0;
            if (o1 != null && o2 == null && outHeader != OutHeader.REMOTE_MAGIC_ATTACK) {
                damagePerc = o1.nOption;
                prop = o1.mOption;
                if (prop >= 100 || Util.succeedProp(prop)) {
                    int hpDamage = Math.max((chr.getMaxHP() * damagePerc) / 100, chr.getHP());
                    chr.damage(hpDamage);
                }
                return 1;
            } else if (o1 == null && o2 != null && outHeader.equals(OutHeader.REMOTE_MAGIC_ATTACK)) {
                damagePerc = o2.nOption;
                prop = o2.mOption;
                if (prop >= 100 || Util.succeedProp(prop)) {
                    int hpDamage = Math.max((chr.getMaxHP() * damagePerc) / 100, chr.getHP());
                    chr.damage(hpDamage);
                }
                return 1;
            } else if (o1 != null && o2 != null) {
                damagePerc = o1.nOption;
                prop = o1.mOption;
                if (prop >= 100 || Util.succeedProp(prop)) {
                    int hpDamage = Math.max((chr.getMaxHP() * damagePerc) / 100, chr.getHP());
                    chr.damage(hpDamage);
                }
                return 1;
            }
        }
        return totalDamage;
    }

    public long handleDamageImmune(Char chr, OutHeader outHeader, long totalDamage, long[] damageInfo) {
        MobTemporaryStat mts = getTemporaryStat();
        TemporaryStatManager tsm = chr.getTemporaryStatManager();

        boolean hasPImmune = mts.hasCurrentMobStat(MobStat.PImmune);
        //chr.chatMessage("has Physical Immune: " + hasPImmune);

        boolean hasMImmune = mts.hasCurrentMobStat(MobStat.MImmune);
        //chr.chatMessage("has Magic Immune: " + hasMImmune);

        boolean hasPowerImmune = mts.hasCurrentMobStat(MobStat.PowerImmune);
        //chr.chatMessage("has Power Immune: " + hasPowerImmune);

        if (tsm.hasStat(CharacterTemporaryStat.IgnoreAllImmune) && (hasPImmune || hasMImmune || hasPowerImmune)) {
            return totalDamage;
        } else if (tsm.hasStat(CharacterTemporaryStat.IgnorePImmune) && (hasPImmune || hasPowerImmune)) {
            return totalDamage;
            //Có thể còn nhiều dạng có thể thêm sau
        } else if (outHeader == OutHeader.REMOTE_MELEE_ATTACK || outHeader == OutHeader.REMOTE_SHOOT_ATTACK) {
            if (hasPImmune || hasPowerImmune) {
                return totalDamage * damageInfo.length;
            }
        } else if (outHeader == OutHeader.REMOTE_MAGIC_ATTACK) {
            if (hasMImmune || hasPowerImmune) {
                return totalDamage * damageInfo.length;
            }
        }
        return totalDamage;
    }

    public void heal(long amount) {
        long oldHp = getHp();
        long newHp = oldHp + amount;
        if (newHp > getMaxHp()) {
            newHp = getMaxHp();
        } else if (newHp < 0) {
            newHp = 0;
        }
        setHp(newHp);
        long diff = newHp - oldHp;
        if (getField() != null & diff != 0) {
            if (isOwnedBySameField()) {
                getOwner().write(MobPacketEncoder.active().damaged(getObjectId(), diff, getTemplateId(), (byte) 0, Util.maxInt(getHp()), Util.maxInt(getMaxHp())));
            } else {
                getField().broadcast(MobPacketEncoder.active().damaged(getObjectId(), diff, getTemplateId(), (byte) 0, Util.maxInt(getHp()), Util.maxInt(getMaxHp())));
            }
        }
        if (oldHp > 0 && newHp <= 0) {
            remove(true);
        }
    }

    public void healMP(int amount) {
        long oldMp = getMp();
        long newMp = oldMp + amount;
        if (newMp > getMaxMp()) {
            newMp = getMaxMp();
        } else if (newMp < 0) {
            newMp = 0;
        }
        setMp(newMp);
    }

    public void teleport(int skillAfter, int xPos, int yPos) {
        Rect possibleRect = getPosition().getRectAround(new Rect(-xPos, -yPos, xPos, yPos));
        setPosition(new Position(Util.getRandom(possibleRect.getLeft(), possibleRect.getRight()),
                Util.getRandom(possibleRect.getTop(), possibleRect.getBottom())));
        getField().broadcast(MobPool.teleportRequest(getObjectId(), skillAfter, getPosition()));
    }

    public int getMobSpawnerId() {
        return mobSpawnerId;
    }

    public void setMobSpawnerId(int mobSpawnerId) {
        this.mobSpawnerId = mobSpawnerId;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public boolean hasProperty(String key) {
        return getProperties().containsKey(key);
    }

    public Object getProperty(String key) {
        return getProperties().get(key);
    }

    public void setProperty(String key, Object value) {
        getProperties().put(key, value);
    }

    public void addMob(int mobId) {
        mobSet.add(mobId);
    }

    public void addParentMob(int mobId) {
        parentMobSet.add(mobId);
    }

    public Set<Integer> getMobSet() {
        return mobSet;
    }

    public Set<Integer> getParentMobSet() {
        return parentMobSet;
    }

    public MobZoneInfo getMobZone() {
        return mobZone;
    }

    public void setMobZone(MobZoneInfo mobZone) {
        this.mobZone = mobZone;
    }

    public long getDotDamage() {
        return dotDamage;
    }

    public void setDotDamage(long damage) {
        this.dotDamage = damage;
    }

    public List<MobBindInfo> getMobBindInfos() {
        return mobBindInfos;
    }

    public void setMobBindInfos(List<MobBindInfo> mobBindInfos) {
        this.mobBindInfos = mobBindInfos;
    }

    public boolean isIgnoredItem(int itemID, List<Integer> ignoredItems) {
        return ignoredItems.contains(itemID);
    }

    public long getLastTimeChangeState() {
        return lastTimeChangeState;
    }

    public void setLastTimeChangeState(long lastTimeChangeState) {
        this.lastTimeChangeState = lastTimeChangeState;
    }

    public long getLifeTime() {
        return lifeTime;
    }

    public void setLifeTime(long lifeTime) {
        this.lifeTime = lifeTime;
    }

    public boolean isPierreSpawnTwice() {
        return isPierreSpawnTwice;
    }

    public void setPierreSpawnTwice(boolean pierreSpawnTwice) {
        isPierreSpawnTwice = pierreSpawnTwice;
    }

    public void setFirstPierreDieTime(long firstPierreDieTime) {
        this.firstPierreDieTime = firstPierreDieTime;
    }

    public long getFirstPierreDieTime() {
        return firstPierreDieTime;
    }

    public long getCapEffectTime() {
        return capEffectTime;
    }

    public void setCapEffectTime(long capEffectTime) {
        this.capEffectTime = capEffectTime;
    }

    public double getBonusHp() {
        int level = getLevel();
        double bonus = 1.0D;
        if (level >= 200 && level <= 210) {
            bonus = 1.5D;
        } else if (level >= 211 && level <= 220) {
            bonus = 2.0D;
        } else if (level >= 221 && level <= 230) {
            bonus = 2.5D;
        } else if (level >= 231 && level <= 240) {
            bonus = 3.0D;
        } else if (level >= 241) {
            bonus = 3.5D;
        }
        if (getTemplateId() >= 9833070 && getTemplateId() <= 9833099) {
            bonus = 1.0D;
        }
        if (isBoss()) {
            switch (getTemplateId()) {
                case 8644650:
                case 8645009:
                    bonus *= 15.0D;
                    break;
                case 8880405:
                case 8880408:
                case 8880409:
                    bonus *= 10.0D;
                    break;
                default:
                    bonus *= 2.0D;
                    break;
            }
        }
        return 1.0D;
    }

    public boolean isUseSpecialSkill() {
        return isUseSpecialSkill;
    }

    public void setUseSpecialSkill(boolean useSpecialSkill) {
        this.isUseSpecialSkill = useSpecialSkill;
    }

    public boolean isSpecialPattern() {
        return isSpecialPattern;
    }

    public void setSpecialPattern(boolean specialPattern) {
        isSpecialPattern = specialPattern;
    }

    public List<Integer> getWillHPlist() {
        return willHPlist;
    }

    public void setWillHPlist(List<Integer> willHPlist) {
        this.willHPlist = willHPlist;
    }

    public Char getOwner() {
        return owner != 0 ? Server.get().getWorld().getCharById(owner) : null;
    }

    public void setOwner(int owner) {
        this.owner = owner;
    }

    public EnumMap<MobStat, Long> getLastDebuffTimes() {
        return lastDebuffTimes;
    }

    public void setLastDebuffTimes(EnumMap<MobStat, Long> lastDebuffTimes) {
        this.lastDebuffTimes = lastDebuffTimes;
    }

    public boolean isAdvMarked() {
        return isAdvMarked;
    }

    public void setAdvMarked(boolean isAdvMarked) {
        this.isAdvMarked = isAdvMarked;
    }
}
