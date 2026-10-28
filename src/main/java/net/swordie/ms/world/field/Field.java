package net.swordie.ms.world.field;

import net.swordie.ms.connection.protocol.codec.MobPacketEncoder;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.swordie.ms.Server;
import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.Core;
import net.swordie.ms.client.character.items.Equip;
import net.swordie.ms.client.character.items.EquipAttribute;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.client.character.runestones.RuneStone;
import net.swordie.ms.client.character.skills.TownPortal;
import net.swordie.ms.client.character.skills.info.MobAttackInfo;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.client.jobs.resistance.OpenGate;
import net.swordie.ms.client.social.Guild.GuildSkill;
import net.swordie.ms.client.social.Party.Party;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.packet.*;
import net.swordie.ms.constants.*;
import net.swordie.ms.enums.*;
import net.swordie.ms.handlers.Timer;
import net.swordie.ms.life.*;
import net.swordie.ms.life.drop.Drop;
import net.swordie.ms.life.drop.DropExpire;
import net.swordie.ms.life.drop.DropInfo;
import net.swordie.ms.life.mob.ForcedMobStat;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.life.mob.skill.*;
import net.swordie.ms.life.npc.Npc;
import net.swordie.ms.loaders.*;
import net.swordie.ms.loaders.containerclasses.ItemInfo;
import net.swordie.ms.loaders.containerclasses.MobSkillInfo;
import net.swordie.ms.scripts.ScriptManager;
import net.swordie.ms.scripts.ScriptManagerImpl;
import net.swordie.ms.scripts.ScriptType;
import net.swordie.ms.util.*;
import net.swordie.ms.util.container.Tuple;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static net.swordie.ms.client.character.skills.SkillStat.*;

public class Field {

    private int SN;

    private long creationTime;
    private long deprecationStartTime;
    private Rect rect;
    private int vrTop, vrLeft, vrBottom, vrRight;
    private double mobRate;
    private int id;
    private FieldType fieldType;
    private long fieldLimit;
    private int returnMap, forcedReturn, createMobInterval, timeOut, timeLimit, lvLimit, lvForceMove;
    private int consumeItemCoolTime, link;
    private boolean town, swim, fly, reactorShuffle, expeditionOnly, partyOnly, needSkillForFly;
    private Set<Portal> portals;
    private Set<Foothold> footholds;
    private final Set<Npc> npcs;
    private String onFirstUserEnter = "", onUserEnter = "";
    private int fixedMobCapacity;
    private final Map<Integer, Life> lifes;
    private final Map<Integer, Mob> mobs;
    private final Map<Integer, ScheduledFuture<?>> lifeSchedules;
    private int objectIDCounter = 1_000_000;
    private boolean userFirstEnter = false;
    private String fieldScript = "";
    private final ScriptManagerImpl scriptManagerImpl = new ScriptManagerImpl(this);
    private RuneStone runeStone;
    private ScheduledFuture<?> runeStoneHordesTimer;
    private long nextEliteSpawnTime = System.currentTimeMillis();
    private int killedElites;
    private EliteState eliteState;
    private int bossMobID;
    private boolean kishin;
    private boolean defaultClock;

    //Is Active, Vip Grade
    private Tuple<Boolean, Integer> totem;
    private volatile int tms282MonolithTier;

    private List<OpenGate> openGateList = new ArrayList<>();
    private List<TownPortal> townPortalList = new ArrayList<>();
    private boolean isChannelField;
    private Int2ObjectOpenHashMap<List<String>> directionInfo;
    private Clock clock;
    private int channel;
    private Map<String, Object> properties;
    private int barrier;
    private int barrierArc;
    private boolean dropsDisabled;
    private ScheduledFuture<?> fieldEffectTimer;
    private BlowWeather bw;
    private int monsterGauge = 0;
    private Position reviveCurFieldOfNoTransferPoint;
    private boolean individualMobPool, reviveCurField, reviveCurFieldOfNoTransfer;

    private boolean isBossSpawned = false;

    // Gollux
    private boolean gollux_head = false;
    private boolean gollux_LArm = false;
    private boolean gollux_RArm = false;
    private boolean gollux_Hip = false;
    private boolean rewardDropped = false;

    // Verus Hilla
    private boolean chaosPinkBeanSpawned = false;

    // Lucid
    private boolean lucidRewardSpawned = false;

    // Zakum
    private int zakumStand = 0;

    // Lucid
    private LucidState lucidState;
    private int lucidStatueGauge;
    private int butterFlyCount = 0;
    private ScheduledFuture<?> spawnButterflyTimer;
    private ScheduledFuture<?> destroyButterflyTimer;

    // Verus Hilla
    private int candles = 0;
    private int lightCandles = 0;
    private int reqTouched = 0;
    private long sandGlassTime = 0L;

    // Boosters
    private int boosterMobID = 0;
    private int boosterEXPMulti = 0;

    protected ConcurrentLinkedQueue<Runnable> tasks;
    protected PriorityQueue<DropExpire> dropExpiry;

    public Field(int fieldID) {
        this.id = fieldID;
        this.SN = Server.get().getFieldIdAndIncrement();
        this.rect = new Rect();
        this.portals = new HashSet<>();
        this.footholds = new HashSet<>();
        this.npcs = new HashSet<>();
        this.lifes = new ConcurrentHashMap<>();
        this.mobs = new ConcurrentHashMap<>();
        this.lifeSchedules = new ConcurrentHashMap<>();
        this.directionInfo = new Int2ObjectOpenHashMap<>();
        this.fixedMobCapacity = FieldConstants.DEFAULT_FIELD_MOB_CAPACITY; // default
        this.properties = new ConcurrentHashMap<>();
        this.dropsDisabled = false;
        this.tasks = new ConcurrentLinkedQueue<>();
        this.dropExpiry = new PriorityQueue<>(Comparator.comparingLong(a -> a.expireAt));
    }

    public void addTask(Runnable r) {
        tasks.offer(r);
    }

    public long update(long now) {
        long count = 0;

        final long deadline = now + 300; // ms
        Runnable r;

        while (now <= deadline && (r = tasks.poll()) != null) {
            try {
                r.run();
            } catch (Exception e) {
                DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
            }
        }

        try {
            count += checkDrops(now);
        } catch (Exception e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
        }

        try {
            for (Mob mob : mobs.values()) {
                try {
                    mob.update(now);
                } catch (Exception e) {
                    DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
                }
            }
        } catch (Exception e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
        }
        return count;
    }

    public long checkDrops(long now) {
        long count = 0;
        while (true) {
            DropExpire top = dropExpiry.peek();
            if (top == null || top.expireAt > now) {
                break;
            }
            dropExpiry.poll();
            Life life = lifes.get(top.dropId);
            if (life instanceof Drop) { // still exists
                removeDrop(top.dropId, 0, false, -1);
                count++;
            }
        }
        return count;
    }

    public void startFieldScript() {
        if (!ScriptManagerImpl.GAME_SCRIPTS_ENABLED) return;
        String script = getFieldScript();
        if (!"".equalsIgnoreCase(script)) {
            System.out.printf("Starting Field script %s.%n", script);
            scriptManagerImpl.startScript(null, getId(), script, ScriptType.Field);
        }
    }

    public boolean getDropsDisabled() {
        return dropsDisabled;
    }

    public void setDropsDisabled(boolean val) {
        dropsDisabled = val;
    }

    public Rect getRect() {
        return rect;
    }

    public void setRect(Rect rect) {
        this.rect = rect;
    }

    public int getVrTop() {
        return vrTop;
    }

    public void setVrTop(int vrTop) {
        this.vrTop = vrTop;
    }

    public int getVrLeft() {
        return vrLeft;
    }

    public void setVrLeft(int vrLeft) {
        this.vrLeft = vrLeft;
    }

    public int getVrBottom() {
        return vrBottom;
    }

    public void setVrBottom(int vrBottom) {
        this.vrBottom = vrBottom;
    }

    public int getVrRight() {
        return vrRight;
    }

    public void setVrRight(int vrRight) {
        this.vrRight = vrRight;
    }

    public int getHeight() {
        return Math.abs(getVrTop() - getVrBottom());
    }

    public int getWidth() {
        return Math.abs(getVrRight() - getVrLeft());
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getSN() {
        return SN;
    }

    public void setSN(int SN) {
        this.SN = SN;
    }

    public FieldType getFieldType() {
        return fieldType;
    }

    public void setFieldType(FieldType fieldType) {
        this.fieldType = fieldType;
    }

    public long getFieldLimit() {
        return fieldLimit;
    }

    public void setFieldLimit(long fieldLimit) {
        this.fieldLimit = fieldLimit;
    }

    public Set<Portal> getPortals() {
        return portals;
    }

    public void setPortals(Set<Portal> portals) {
        this.portals = portals;
    }

    public void addPortal(Portal portal) {
        getPortals().add(portal);
    }

    public int getReturnMap() {
        return returnMap;
    }

    public void setReturnMap(int returnMap) {
        this.returnMap = returnMap;
    }

    public int getForcedReturn() {
        return forcedReturn;
    }

    public void setForcedReturn(int forcedReturn) {
        this.forcedReturn = forcedReturn;
    }

    public double getMobRate() {
        return mobRate;
    }

    public void setMobRate(double mobRate) {
        this.mobRate = mobRate;
    }

    public int getCreateMobInterval() {
        return createMobInterval;
    }

    public void setCreateMobInterval(int createMobInterval) {
        this.createMobInterval = createMobInterval;
    }

    public int getTimeOut() {
        return timeOut;
    }

    public void setTimeOut(int timeOut) {
        this.timeOut = timeOut;
    }

    public int getTimeLimit() {
        return timeLimit;
    }

    public void setTimeLimit(int timeLimit) {
        this.timeLimit = timeLimit;
    }

    public int getLvLimit() {
        return lvLimit;
    }

    public void setLvLimit(int lvLimit) {
        this.lvLimit = lvLimit;
    }

    public int getLvForceMove() {
        return lvForceMove;
    }

    public void setLvForceMove(int lvForceMove) {
        this.lvForceMove = lvForceMove;
    }

    public int getConsumeItemCoolTime() {
        return consumeItemCoolTime;
    }

    public void setConsumeItemCoolTime(int consumeItemCoolTime) {
        this.consumeItemCoolTime = consumeItemCoolTime;
    }

    public int getLink() {
        return link;
    }

    public void setLink(int link) {
        this.link = link;
    }

    public boolean isTown() {
        return town;
    }

    public void setTown(boolean town) {
        this.town = town;
    }

    public boolean isSwim() {
        return swim;
    }

    public void setSwim(boolean swim) {
        this.swim = swim;
    }

    public boolean isFly() {
        return fly;
    }

    public void setFly(boolean fly) {
        this.fly = fly;
    }

    public boolean isReactorShuffle() {
        return reactorShuffle;
    }

    public void setReactorShuffle(boolean reactorShuffle) {
        this.reactorShuffle = reactorShuffle;
    }

    public boolean isExpeditionOnly() {
        return expeditionOnly;
    }

    public void setExpeditionOnly(boolean expeditionONly) {
        this.expeditionOnly = expeditionONly;
    }

    public boolean isPartyOnly() {
        return partyOnly;
    }

    public void setPartyOnly(boolean partyOnly) {
        this.partyOnly = partyOnly;
    }

    public boolean isNeedSkillForFly() {
        return needSkillForFly;
    }

    public void setNeedSkillForFly(boolean needSkillForFly) {
        this.needSkillForFly = needSkillForFly;
    }

    public String getOnFirstUserEnter() {
        return onFirstUserEnter;
    }

    public void setOnFirstUserEnter(String onFirstUserEnter) {
        this.onFirstUserEnter = onFirstUserEnter;
    }

    public String getOnUserEnter() {
        return onUserEnter;
    }

    public void setOnUserEnter(String onUserEnter) {
        this.onUserEnter = onUserEnter;
    }

    public Portal getPortalByName(String name) {
        return Util.findWithPred(getPortals(), portal -> portal.getName().equals(name));
    }

    public Portal getPortalByID(int id) {
        return Util.findWithPred(getPortals(), portal -> portal.getId() == id);
    }

    public RuneStone getRuneStone() {
        return runeStone;
    }

    public void setRuneStone(RuneStone runeStone) {
        this.runeStone = runeStone;
    }

    public Foothold findFootHoldBelow(Position pos) {
        Set<Foothold> footholds = getFootholds().stream().filter(fh -> (pos.getX() >= fh.getX1() && pos.getX() <= fh.getX2()) || (pos.getX() >= fh.getX2() && pos.getX() <= fh.getX1())).collect(Collectors.toSet());
        Foothold res = null;
        int lastY = Integer.MAX_VALUE;
        for (Foothold fh : footholds) {
            int y = fh.getYFromX(pos.getX());
            if (res == null && y >= pos.getY()) {
                res = fh;
                lastY = y;
            } else {
                if (y < lastY && y >= pos.getY()) {
                    res = fh;
                    lastY = y;
                }
            }
        }
        return res;
    }

    public Tuple<Foothold, Foothold> getMinMaxNonWallFH() {
        Set<Foothold> footholds = getFootholds().stream().filter(fh -> !fh.isWall()).collect(Collectors.toSet());
        Foothold left = footholds.iterator().next();
        Foothold right = footholds.iterator().next(); // retun vals

        for (Foothold fh : footholds) {
            if (fh.getX1() < left.getX1()) {
                left = fh;
            } else if (fh.getX1() > right.getX1()) {
                right = fh;
            }
        }
        return new Tuple<>(left, right);
    }

    public Set<Foothold> getFootholds() {
        return footholds;
    }

    public void setFootholds(Set<Foothold> footholds) {
        this.footholds = footholds;
    }

    public void addFoothold(Foothold foothold) {
        getFootholds().add(foothold);
    }

    public Set<Npc> getOriginalNpcs() {
        return npcs;
    }

    public int getFixedMobCapacity() {
        return fixedMobCapacity;
    }

    public void setFixedMobCapacity(int fixedMobCapacity) {
        this.fixedMobCapacity = fixedMobCapacity;
    }

    public Map<Integer, Life> getLifes() {
        return lifes;
    }

    public void addLife(Life life) {
        if (life == null) return;
        if (life.getObjectId() < 0) {
            life.setObjectId(getNewObjectID());
        }
        lifes.put(life.getObjectId(), life);
        if (life instanceof Mob mob) {
            mobs.put(life.getObjectId(), mob);
        }
        life.setField(this);
        if (life instanceof Mob) {
            var sm = getScriptManager();
            if (sm != null) {
                life.addObserver(sm);
            }
            for (Char chr : getChars()) {
                life.addObserver(chr.getScriptManager());
            }
        }
    }

    public void removeLife(int id, boolean fromSchedule) {
        removeLife(id, fromSchedule, true);
    }

    public void removeLife(int id, boolean fromSchedule, boolean isBroadcastLeavePacket) {
        Life life = lifes.remove(id);
        if (life instanceof Mob) {
            mobs.remove(id);
            net.swordie.ms.client.character.skills.temp.SecondAtomLockOnStat.targetRemoved(this, id);
            net.swordie.ms.client.character.skills.GuidedBullet.targetRemoved(this, id);
        }
        ScheduledFuture<?> sf = lifeSchedules.remove(id);
        if (life != null && isBroadcastLeavePacket) {
            life.broadcastLeavePacket();
        }
        if (sf != null && !fromSchedule) {
            sf.cancel(false);
        }
    }

    public Life getLifeByObjectID(int id) {
        return lifes.get(id);
    }

    public void removeMob(int id) {
        removeMob(id, DeathType.ANIMATION_DEATH);
    }

    public void removeMobsByTemplateID(int templateID) {
        for (Mob m : getMobs()) {
            if (m.getTemplateId() == templateID) {
                removeMob(m.getObjectId(), DeathType.ANIMATION_DEATH);
            }
        }
    }

    public void removeMobBySvr(int objectID, Char chr) {
        broadcast(MobPool.leaveFieldTargetFromSvr(objectID, chr.getId(), chr.getJob()));
        removeLife(objectID, true, false);
    }

    public void removeMob(int objectID, DeathType deathType) {
        broadcast(MobPacketEncoder.active().leaveField(objectID, deathType));
        removeLife(objectID, true, false);
    }

    public void removeMob(Char chr, int objectID, DeathType deathType) {
        chr.write(MobPacketEncoder.active().leaveField(objectID, deathType));
        removeLife(objectID, true, false);
    }

    public void spawnSummon(Summon summon) {
        Summon oldSummon = (Summon) getLifes().values().stream()
                .filter(s -> s instanceof Summon exist &&
                        exist.getOwnerId() == summon.getOwnerId() &&
                        exist.getSkillID() == summon.getSkillID())
                .findFirst().orElse(null);
        if (oldSummon != null) {
            removeLife(oldSummon.getObjectId(), false);
        }
        spawnLife(summon, null);
        if (summon.getSkillID() == net.swordie.ms.client.jobs.Job.MONOLITH
                && net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) {
            setTms282MonolithTier(Tms282Monolith.activeTier(this, null));
        }
    }

    public void spawnAddSummon(Summon summon) { //Test
        spawnLife(summon, null);
    }

    public void removeSummon(int skillID, int chrID) {
        Summon summon = (Summon) getLifes().values().stream()
                .filter(s -> s instanceof Summon exist &&
                        exist.getOwnerId() == chrID &&
                        exist.getSkillID() == skillID)
                .findFirst().orElse(null);
        if (summon != null) {
            removeLife(summon.getObjectId(), false);
        }
    }

    public void spawnWreckage(Char chr, Wreckage wreckage) {
        addLife(wreckage);
        broadcast(FieldPacket.addWreckage(wreckage, getWreckageByChrId(chr.getId()).size()));
        ScheduledFuture<?> sf = getTimer().addEvent(() -> removeWreckage(chr, wreckage), wreckage.getDuration(), TimeUnit.MILLISECONDS);
        addLifeSchedule(wreckage.getObjectId(), sf);
    }

    public void removeWreckage(Char chr, Wreckage wreckage) {
        removeWreckage(chr, Collections.singletonList(wreckage));
    }

    public void removeWreckage(Char chr, List<Wreckage> wreckageList) {
        broadcast(FieldPacket.delWreckage(chr, wreckageList));
        for (Wreckage wreckage : wreckageList) {
            removeLife(wreckage);
        }
    }

    public void spawnLifeForTime(Life life, int timeMS) {
        spawnLife(life, null);
        ScheduledFuture<?> sf = getTimer().addEvent(() -> removeLife(life.getObjectId(), true), timeMS, TimeUnit.MILLISECONDS);
        addLifeSchedule(life.getObjectId(), sf);
    }

    public void spawnLife(Life life, Char onlyChar) {
        addLife(life);
        if (!getChars().isEmpty()) {
            if (life instanceof Npc || life instanceof Mob) {
                if (life.getControllerID() == -1) {
                    setController(life);
                }
            }
            if (life instanceof Mob mob) {
                handleCustomField(mob, Util.getRandomFromCollection(getChars()));
                boolean buffed = isChannelField() && getChannel() > GameConstants.BUFFED_CHANNELS && !mob.isBoss();
                if (buffed) {
                    if (!mob.isBoss()) {
                        if (mob.getHp() == mob.getMaxHp()) {
                            mob.setHp(mob.getMaxHp() * GameConstants.BUFFED_MOB_HP_MULTIPLIER);
                        }
                        mob.setScale(GameConstants.BUFFED_MOB_SCALE);
                        mob.setMaxHp(mob.getMaxHp() * GameConstants.BUFFED_MOB_HP_MULTIPLIER);
                        mob.setPad(mob.getPad() * GameConstants.BUFFED_MOB_DAMAGE_MULTIPLIER);
                        mob.setMad(mob.getMad() * GameConstants.BUFFED_MOB_DAMAGE_MULTIPLIER);
                        ForcedMobStat fms = mob.getForcedMobStat();
                        fms.setPdr(mob.getPdr() * GameConstants.BUFFED_MOB_DEFENSE_MULTIPLIER);
                        fms.setMdr(mob.getMdr() * GameConstants.BUFFED_MOB_DEFENSE_MULTIPLIER);
                        fms.setExp((long) (mob.getExp() * GameConstants.BUFFED_MOB_EXP_MULTIPLIER / 100.0D));
                    }
                }
            }
            life.broadcastSpawnPacket(onlyChar);
            if (life instanceof Mob mob) {
                if (mob.getRemoveAfter() > 0) { // removeafter == 1 means its supposed to die immediately
                    if (mob.getRemoveAfter() == 1) {
                        mob.remove(false);
                    } else if (!lifeSchedules.containsKey(mob.getObjectId())) {
                        addLifeSchedule(mob.getObjectId(), getTimer().addEvent(() -> mob.remove(false), mob.getRemoveAfter(), TimeUnit.SECONDS));
                    }
                }
            }
        }
    }

    public void setController(Life life) {
        Position pos = life.getPosition().deepCopy();
        Char closestChar = findClosestChar(pos, 10000);
        if (closestChar != null) {
            life.setControllerID(closestChar.getId());
        } else {
            Char random = Util.getRandomFromCollection(getChars());
            life.setControllerID(random != null ? random.getId() : -1);
        }
    }

    public void removeLife(Life life) {
        removeLife(life.getObjectId(), false);
    }

    public Foothold getFootholdById(int fh) {
        return getFootholds().stream().filter(f -> f.getId() == fh).findFirst().orElse(null);
    }

    public Set<Char> getChars() {
        return Server.get().getCharsByFieldId(getSN());
    }

    public Set<Char> getCharsExcept(Char exceptChar) {
        if (exceptChar == null) {
            return getChars();
        }
        return getChars().stream()
                .filter(chr -> chr.getId() != exceptChar.getId())
                .collect(Collectors.toSet());
    }

    public Set<Char> getBots() {
        return Server.get().getBotsByFieldId(getSN());
    }

    public Char getCharByID(int id) {
        // Lấy danh sách một lần và lọc
        return getChars().stream()
                .filter(chr -> chr != null && chr.getId() == id)
                .findFirst().orElse(null);
    }

    public Char getCharByName(String name) {
        // Lấy danh sách một lần và lọc
        return getChars().stream()
                .filter(chr -> chr != null && chr.getName().equalsIgnoreCase(name))
                .findFirst().orElse(null);
    }

    public void addChar(Char chr) {
        if (net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3)
            tms282Burning.level(System.currentTimeMillis(), true);
        // 282 first-enter scripts must run after SetField and after chr.field has changed.
        if (net.swordie.ms.connection.protocol.Protocol.active().version()
                != net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) {
            execFirstUserEnterScript(chr);
        }
        final OutPacket packet = UserPool.userEnterField(chr);
        broadcast(packet, chr); // multiplayer-0930: never to the entering user itself (same-map re-entry got its own LP03D0)
        //broadcast(UserRemote.setTemporaryStat(chr, chr.getTemporaryStatManager().getRemoteStats()), chr);
        chr.getClient().getChannelInstance().trySpawnAreaBoss(chr, getId(), getChannel());
        // 282: the owner's android on login / map change. Char.warp only spawns it for a non-GM with an active
        // familiar (its familiar `if` body is commented out, so the android line became that body): the android
        // vanished after every relogin / warp (09-29). A fresh object id for this field; spawnLife is idempotent
        // per id, so the Char.warp spawn (when it does run) only repeats the create.
        var android = chr.getAndroid();
        if (android != null && net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3 && getLifes().get(android.getObjectId()) != android) {
            android.setObjectId(-1);
            spawnLife(android, null);
        }
        // 282 變身燃燒: manager 5 + user component after SetField (both creates are idempotent in the client)
        if (ScriptManagerImpl.GAME_SCRIPTS_ENABLED)
            net.swordie.ms.world.tms282.contents.changeburning.ChangeBurning.get().onFieldEnter(chr);
    }

    public synchronized void execFirstUserEnterScript(Char chr) {
        if (!ScriptManagerImpl.GAME_SCRIPTS_ENABLED) return;
        boolean compatible = net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3;
        if (!isUserFirstEnter()) {
            if (compatible) {
                setUserFirstEnter(true); // claim before synchronous Java dispatch or an async Python script
                if (CustomFieldScripts.getScriptForField(getId()) != null
                        && BossConstants.getBossEntranceMapForFightField(getId()) != 0) {
                    return; // the boss's custom initializer owns its summon and intro
                }
            }
            if (hasUserFirstEnterScript()) {
                chr.getScriptManager().startScript(chr, getId(), getOnFirstUserEnter(), ScriptType.FirstEnterField);
                setUserFirstEnter(true);
            } else if (CustomFUEFieldScripts.getByVal(getId()) != null) {
                String feFieldScript = CustomFUEFieldScripts.getByVal(getId()).toString();
                chr.getScriptManager().startScript(chr, getId(), feFieldScript, ScriptType.FirstEnterField);
                setUserFirstEnter(true);
            }
        }
    }

    private boolean hasUserFirstEnterScript() {
        return getOnFirstUserEnter() != null && !getOnFirstUserEnter().equalsIgnoreCase("");
    }

    public void removeChar(int charID) {
        Char chr = getCharByID(charID);
        if (chr == null) {
            return;
        }
        if (net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3)
            tms282Burning.level(System.currentTimeMillis(), !getCharsExcept(chr).isEmpty());
        broadcast(UserPool.userLeaveField(chr), chr);
        // change controllers for which the chr was the controller of
        Char random = Util.getRandomFromCollection(getCharsExcept(chr));
        if (random != null) {
            for (Life life : getLifes().values()) {
                if (life instanceof Npc npc) {
                    npc.setControllerID(random.getId());
                    npc.notifyControllerChange();
                } else if (life instanceof Mob mob && mob.getOwner() == null) {
                    mob.setControllerID(random.getId());
                    mob.notifyControllerChange();
                }
            }
        } else {
            for (Life life : getLifes().values()) {
                if (life instanceof Npc npc) {
                    npc.setControllerID(-1);
                } else if (life instanceof Mob mob && mob.getOwner() == null) {
                    mob.setControllerID(-1);
                }
            }
        }
        // remove summons of that char & remove field attacks of that char
        List<Integer> removedList = new ArrayList<>();
        for (Life life : getLifes().values()) {
            if (life instanceof Summon summon) {
                if (summon.getOwnerId() == charID) {
                    removedList.add(life.getObjectId());
                }
            } else if (life instanceof FieldAttackObj fieldAttackObj) {
                if (fieldAttackObj.getOwnerId() == charID) {
                    removedList.add(life.getObjectId());
                }
            } else if (life instanceof Wreckage wreckage) {
                if (wreckage.getOwnerId() == charID) {
                    removedList.add(life.getObjectId());
                }
            } else if (life instanceof Dragon dragon) {
                if (dragon.getOwnerId() == charID) {
                    removedList.add(life.getObjectId());
                }
            } else if (life instanceof Android android) {
                if (android.getOwnerId() == charID) {
                    removedList.add(life.getObjectId());
                }
            } else if (life instanceof FoxMan foxman) {
                if (foxman.getOwnerId() == charID) {
                    removedList.add(life.getObjectId());
                }
            } else if (life instanceof SkillPet skillpet) {
                if (skillpet.getOwnerId() == charID) {
                    removedList.add(life.getObjectId());
                }
            }
        }
        for (int id : removedList) {
            removeLife(id, true);
        }
    }

    public Life getLifeByTemplateId(int templateId) {
        return getLifes().values().stream().filter(l -> l.getTemplateId() == templateId).findFirst().orElse(null);
    }

    public void spawnLifesForChar(Char chr) {
        for (Life life : getLifes().values()) {
            if (life instanceof net.swordie.ms.life.Android own && own.getOwnerChar() == chr) {
                continue; // the owner's android: addChar's deferred field-wide create (not right after SetField)
            }
            if (life instanceof Drop drop) {
                if (drop.getOwnerID() == chr.getId() || drop.getOwnerID() == 0) {
                    spawnLife(life, chr);
                }
            } else if (life instanceof Reactor reactor) {
                if (reactor.getOwnerID() == chr.getId() || reactor.getOwnerID() == 0) {
                    spawnLife(life, chr);
                }
            } else if (life instanceof AffectedArea aa) {
                if (aa.getMobOrigin() != 1 && getCharByID(aa.getCharID()) == null) {
                    continue;
                }
                spawnLife(aa, chr);
            } else {
                spawnLife(life, chr);
            }
        }
        if (chr.getInstance() == null) {
            for (Npc n : getOriginalNpcs()) {
                int templateID = n.getTemplateId();
                Life life = getNpcs().stream().filter(l -> l.getTemplateId() == templateID).findFirst().orElse(null);
                if (life == null) {
                    spawnLife(n, null);
                }
            }
        }
        if (getRuneStone() != null
                && getMobs() != null
                && !getMobs().isEmpty()
                && getBossMobID() == 0
                && isChannelField()
                && !isTown()
                && Arrays.stream(FieldConstants.BLOCKED_RUNE_MAPS).noneMatch(m -> m == getId())) {
            chr.write(FieldPacket.runeStoneAppear(getRuneStone()));
        }
        if (getOpenGates() != null && !getOpenGates().isEmpty()) {
            for (OpenGate openGate : getOpenGates()) {
                openGate.showOpenGate(this);
            }
        }
        if (getTownPortalList() != null && !getTownPortalList().isEmpty()) {
            for (TownPortal townPortal : getTownPortalList()) {
                townPortal.showTownPortal(this);
            }
        }
        if (getClock() != null) {
            getClock().showClock(chr);
        }
        if (chr.getInstance() != null && chr.getInstance().getWarpOutTimer() != null) {
            chr.write(FieldPacket.clock(ClockPacket.secondsClock(chr.getInstance().getRemainingTime())));
        }
        if (getZakumStand() == 1) {
            createZakumStand();
        }
        for (Char other : getChars()) {
            if (other.getId() == chr.getId()) {
                continue;
            }
            final OutPacket packet = UserPool.userEnterField(other);
            chr.write(packet);
            if (other.getJobHandler() instanceof net.swordie.ms.client.jobs.adventurer.pirate.Cannoneer cannon
                    && cannon.isMonkeyVisible()) {
                chr.write(net.swordie.ms.connection.packet.Tms282CannoneerPackets.monkey(other.getId(), true));
            }
            if (other.getDragon() != null) {
                chr.write(DragonPool.createDragon(other.getDragon()));
            }
            if (other.getActiveFamiliar() != null) {
                //chr.write(CFamiliar.familiarEnterField(c.getId(), true, c.getActiveFamiliar(), true, false));
            }
            if (other.getAndroid() != null) {
                chr.write(AndroidPacket.created(other.getAndroid()));
            }
            other.initPets(chr);
        }
        if (FieldConstants.HENESYS_ID == getId() && Server.get().bot != null) {
            final OutPacket packet = UserPool.userEnterField(Server.get().bot);
            chr.write(packet);
        }
        for (Char bot : getBots()) {
            final OutPacket packet = UserPool.userEnterField(bot);
            chr.write(packet);
        }
    }

    @Override
    public String toString() {
        return "" + getId();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Field field) {
            return field.getSN() == getSN();
        }
        return false;
    }

    public void respawn(Mob mob) {
        mob.setHp(mob.getMaxHp());
        mob.setMp(mob.getMaxMp());
        mob.setPosition(mob.getHomePosition().deepCopy());
        spawnLife(mob, null);
    }

    public void broadcast(OutPacket outPacket) {
        broadcast(outPacket, null);
    }

    public void broadcast(OutPacket outPacket, Char exceptChar) {
        Server.get().broadcastForField(outPacket, this, exceptChar);
    }

    public void spawnAffectedArea(AffectedArea aa) {
        addLife(aa);
        SkillInfo si = SkillData.getSkillInfoById(aa.getSkillID());
        MobSkillInfo msi = SkillData.getMobSkillInfoByIdAndLevel(aa.getSkillID(), aa.getSlv());
        if (si != null || (aa.getMobOrigin() > 0 && msi != null)) {
            int duration = 0;
            if (aa.getMobOrigin() > 0 && msi != null) {
                duration = aa.getDuration() == 0 ? msi.getSkillStatIntValue(MobSkillStat.time) * 1000 : aa.getDuration();
            }
            if (si != null) {
                duration = aa.getDuration() == 0 ? si.getValue(time, aa.getSlv()) * 1000 : aa.getDuration();
            }
            if (duration > 0) {
                addLifeSchedule(aa.getObjectId(), getTimer().addEvent(() -> removeLife(aa.getObjectId(), true), duration));
            }
        }
        broadcast(FieldPacket.affectedAreaCreated(aa));
        getChars().forEach(chr -> aa.getField().checkCharInAffectedAreas(chr));
        getMobs().forEach(mob -> aa.getField().checkMobInAffectedAreas(mob));
    }

    public void spawnAffectedAreaAndRemoveOld(AffectedArea aa) {
        AffectedArea oldAA = (AffectedArea) getLifes().values().stream()
                .filter(l -> l instanceof AffectedArea &&
                        ((AffectedArea) l).getCharID() == aa.getCharID() &&
                        ((AffectedArea) l).getSkillID() == aa.getSkillID())
                .findFirst().orElse(null);
        if (oldAA != null) {
            removeLife(oldAA.getObjectId(), false);
        }
        spawnAffectedArea(aa);
    }

    public void removeAffectedArea(int skillID, int charID) {
        AffectedArea oldAA = (AffectedArea) getLifes().values().stream()
                .filter(l -> l instanceof AffectedArea &&
                        ((AffectedArea) l).getCharID() == charID &&
                        ((AffectedArea) l).getSkillID() == skillID)
                .findFirst().orElse(null);
        if (oldAA != null) {
            removeLife(oldAA.getObjectId(), false);
        }
    }

    private <T> Set<T> getLifesByClass(Class clazz) {
        return (Set<T>) getLifes().values().stream()
                .filter(l -> l.getClass().equals(clazz))
                .collect(Collectors.toSet());
    }

    public Collection<Mob> getMobs() {
        return mobs.values();
    }

    public void killMobs() {
        for (Mob mob : new ArrayList<>(getMobs())) {
            if (mob != null) {
                mob.removeWithAnimation();
            }
        }
    }

    public Set<Summon> getSummons() {
        return getLifesByClass(Summon.class);
    }

    public List<Summon> getSummonsByChar(Char chr) {
        if (chr == null) {
            return null;
        }
        return getSummons().stream().filter(s -> s.getOwnerId() == chr.getId()).collect(Collectors.toList());
    }

    public Summon getSummonBySkillId(Char chr, int skillID) {
        if (chr == null) {
            return null;
        }
        return getSummons().stream().filter(s -> s.getSkillID() == skillID && s.getOwnerId() == chr.getId()).findFirst().orElse(null);
    }

    public List<Summon> getSummonsBySkillId(Char chr, int skillID) {
        if (chr == null) {
            return null;
        }
        return getSummons().stream().filter(s -> s.getSkillID() == skillID && s.getOwnerId() == chr.getId()).toList();
    }

    public Summon getSummonByChrAndSkillIdInRect(Char chr, int skillId, Rect rect) {
        if (chr == null) {
            return null;
        }
        return getSummonsInRect(rect).stream().filter(s -> s.getSkillID() == skillId && s.getOwnerId() == chr.getId()).findFirst().orElse(null);
    }

    public Set<Drop> getDrops() {
        return getLifesByClass(Drop.class);
    }

    public Set<MobGen> getMobGens() {
        return getLifesByClass(MobGen.class);
    }

    public Set<AffectedArea> getAffectedAreas() {
        return getLifesByClass(AffectedArea.class);
    }

    public AffectedArea getAffectedAreaBySkillID(int charID, int skillID) {
        for (AffectedArea aa : getAffectedAreas()) {
            if (aa.getSkillID() == skillID && aa.getCharID() == charID) {
                return aa;
            }
        }
        return null;
    }

    public List<AffectedArea> getAffectedAreasBySkillID(int charID, int skillID) {
        List<AffectedArea> result = new ArrayList<>();
        for (AffectedArea aa : getAffectedAreas()) {
            if (aa.getSkillID() == skillID && aa.getCharID() == charID) {
                result.add(aa);
            }
        }
        return result;
    }

    public Set<Reactor> getReactors() {
        return getLifesByClass(Reactor.class);
    }

    public Set<Npc> getNpcs() {
        return getLifesByClass(Npc.class);
    }

    public Set<FieldAttackObj> getFieldAttackObjects() {
        return getLifesByClass(FieldAttackObj.class);
    }

    public Set<Wreckage> getWreckage() {
        return getLifesByClass(Wreckage.class);
    }

    public List<Wreckage> getWreckageByChrId(int chrId) {
        return getWreckage().stream().filter(w -> w.getOwnerId() == chrId).collect(Collectors.toList());
    }

    public Set<SpiderWeb> getSpiderWebs() {
        return getLifesByClass(SpiderWeb.class);
    }

    public void setObjectIDCounter(int idCounter) {
        objectIDCounter = idCounter;
    }

    public int getNewObjectID() {
        if (objectIDCounter == Integer.MAX_VALUE) {
            if (!lifes.isEmpty()) {
                throw new IllegalStateException("ObjectID reached MAX_VALUE while lifes not empty. Restart required.");
            }
            objectIDCounter = 1_000_000;
        }
        return objectIDCounter++;
    }

    public List<Life> getLifesInRect(Rect rect) {
        List<Life> lifes = new ArrayList<>();
        for (Life life : getLifes().values()) {
            if (life == null || life.getPosition() == null) {
                continue;
            }
            Position position = life.getPosition();
            int x = position.getX();
            int y = position.getY();
            if (x >= rect.getLeft() && y >= rect.getTop()
                    && x <= rect.getRight() && y <= rect.getBottom()) {
                lifes.add(life);
            }
        }
        return lifes;
    }

    public List<Char> getCharsInRect(Rect rect) {
        return getChars().stream()
                .filter(chr -> chr != null && chr.getPosition() != null)
                .filter(chr -> {
                    Position position = chr.getPosition();
                    return position.getX() >= rect.getLeft() && position.getX() <= rect.getRight() &&
                            position.getY() >= rect.getTop() && position.getY() <= rect.getBottom();
                })
                .collect(Collectors.toList());
    }

    public List<Char> getPartyCharSameFieldInRect(Char chr, Rect rect) {
        Party party = chr.getParty();
        List<Char> partyChars = new ArrayList<>();
        for (Char pmChr : party.getPartyMembersInSameFieldWithChr(chr)) {
            if (pmChr == null || pmChr.getPosition() == null) {
                continue;
            }
            Position position = pmChr.getPosition();
            int x = position.getX();
            int y = position.getY();
            if (x >= rect.getLeft() && y >= rect.getTop()
                    && x <= rect.getRight() && y <= rect.getBottom()) {
                partyChars.add(pmChr);
            }
        }
        return partyChars;
    }

    public List<Mob> getMobsInRect(Rect rect) {
        if (net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) {
            List<Mob> targets = new ArrayList<>();
            for (Mob mob : mobs.values()) {
                if (mob != null && mob.getHp() > 0 && (rect == null || rect.hasPositionInside(mob.getPosition())))
                    targets.add(mob);
            }
            return targets;
        }
        Mob boss = null;
        List<Mob> normals = new ArrayList<>();
        for (Mob mob : mobs.values()) {
            if (mob == null || mob.getHp() <= 0) {
                continue;
            }
            if (rect != null && !rect.hasPositionInside(mob.getPosition())) {
                continue;
            }
            if (mob.isBoss()) {
                if (boss == null || mob.getHp() > boss.getHp()) {
                    boss = mob;
                }
            } else {
                normals.add(mob);
            }
        }
        if (boss != null) {
            final var size = normals.size();
            normals.clear();
            for (int i = 0; i < size; i++) {
                normals.add(boss);
            }
            return normals;
        }
        return normals;
    }

    public List<Mob> getMobs(List<MobAttackInfo> mais) {
        boolean exactTargets = net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3;
        if (exactTargets && mais == null) return new ArrayList<>();
        Mob boss = null;
        Set<Integer> seen = new HashSet<>();
        List<Mob> normals = new ArrayList<>();
        for (MobAttackInfo mai : mais) {
            if (exactTargets && mai == null) continue;
            int mobOID = mai.mobId;
            if (!seen.add(mobOID)) {
                continue;
            }
            Life life = getLifeByObjectID(mobOID);
            if (!(life instanceof Mob mob)) {
                continue;
            }
            if (mob.getHp() <= 0) continue;

            if (exactTargets) {
                normals.add(mob);
                continue;
            }

            if (mob.isBoss()) {
                if (boss == null || mob.getHp() > boss.getHp()) {
                    boss = mob;
                }
            } else {
                normals.add(mob);
            }
        }
        if (boss != null) {
            final var size = normals.size();
            normals.clear();
            for (int i = 0; i < size; i++) {
                normals.add(boss);
            }
            return normals;
        }
        return normals;
    }

    public List<Mob> getMobsFiltered() {
        if (net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) return getMobsInRect(null);
        Mob boss = null;
        List<Mob> normals = new ArrayList<>();
        for (Mob mob : mobs.values()) {
            if (mob == null || mob.getHp() <= 0) continue;
            if (mob.isBoss()) {
                if (boss == null || mob.getHp() > boss.getHp()) {
                    boss = mob;
                }
            } else {
                normals.add(mob);
            }
        }
        if (boss != null) {
            final var size = normals.size();
            normals.clear();
            for (int i = 0; i < size; i++) {
                normals.add(boss);
            }
            return normals;
        }
        return normals;
    }

    public Mob getClosestMobInFront(Char chr) {
        Field field = chr.getField();
        if (field == null) return null;

        Position cp = chr.getPosition();
        int cx = cp.getX();
        int cy = cp.getY();
        boolean left = chr.isLeft();

        Mob best = null;
        long bestScore = Long.MAX_VALUE;

        for (Mob mob : field.getMobs()) { // use your existing mob collection getter
            if (mob == null || mob.getHp() <= 0) continue;

            Position mp = mob.getPosition();
            int mx = mp.getX();
            int my = mp.getY();

            // must be in front
            if (left) {
                if (mx >= cx) continue;
            } else {
                if (mx <= cx) continue;
            }

            // score: prioritize closeness in X, penalize Y difference
            long dx = Math.abs((long) mx - cx);
            long dy = Math.abs((long) my - cy);
            long score = dx + (dy << 2); // Y weighted x4 to prefer same platform

            if (score < bestScore) {
                bestScore = score;
                best = mob;
            }
        }
        return best;
    }

    public List<Drop> getDropsInRect(Rect rect) {
        List<Drop> drops = new ArrayList<>();
        for (Drop drop : getDrops()) {
            if (drop == null || drop.getPosition() == null) {
                continue;
            }
            Position position = drop.getPosition();
            int x = position.getX();
            int y = position.getY();
            if (x >= rect.getLeft() && y >= rect.getTop()
                    && x <= rect.getRight() && y <= rect.getBottom()) {
                drops.add(drop);
            }
        }
        return drops;
    }

    public List<Summon> getSummonsInRect(Rect rect) {
        List<Summon> summons = new ArrayList<>();
        for (Summon summon : getSummons()) {
            if (summon == null || summon.getPosition() == null) {
                continue;
            }
            Position position = summon.getPosition();
            int x = position.getX();
            int y = position.getY();
            if (x >= rect.getLeft() && y >= rect.getTop()
                    && x <= rect.getRight() && y <= rect.getBottom()) {
                summons.add(summon);
            }
        }
        return summons;
    }

    public List<AffectedArea> getAffectAreasInRect(Rect rect) {
        List<AffectedArea> aas = new ArrayList<>();
        for (AffectedArea aa : getAffectedAreas()) {
            if (aa == null || aa.getPosition() == null) {
                continue;
            }
            Position position = aa.getPosition();
            int x = position.getX();
            int y = position.getY();
            if (x >= rect.getLeft() && y >= rect.getTop()
                    && x <= rect.getRight() && y <= rect.getBottom()) {
                aas.add(aa);
            }
        }
        return aas;
    }

    public void removeDrop(int dropID, int pickupUserID, boolean fromSchedule, int petID) {
        Life life = getLifeByObjectID(dropID);
        if (!(life instanceof Drop drop)) {
            return;
        }
        Item item = drop.getItem();
        removeLife(dropID, fromSchedule);
        if (petID >= 0) {
            broadcast(DropPool.dropLeaveField(DropLeaveType.PetPickup, pickupUserID, dropID, (short) 0, petID, 0));
        } else if (pickupUserID != 0) {
            broadcast(DropPool.dropLeaveField(dropID, pickupUserID));
        } else {
            // Check if item is not null before trying to delete it from SQL
            if (item != null) {
                item.deleteFromSQL();
            }
            broadcast(DropPool.dropLeaveField(DropLeaveType.Fade, 0, dropID, (short) 0, 0, 0));
        }
    }

    public void addLifeSchedule(int objectID, ScheduledFuture<?> scheduledFuture) {
        lifeSchedules.putIfAbsent(objectID, scheduledFuture);
    }

    public void checkMobInAffectedAreas(Mob mob) {
        final var pos = mob.getPosition();
        for (AffectedArea aa : getAffectedAreas()) {
            Rect r = aa.getRect();
            if (r == null) {
                continue;
            }
            boolean isInside = r.hasPositionInside(pos);
            if (isInside) aa.handleMobInside(mob);
            else aa.handleMobOutside(mob);
        }
    }

    public void checkCharInAffectedAreas(Char chr) {
        final var pos = chr.getPosition();
        for (AffectedArea aa : getAffectedAreas()) {
            Rect r = aa.getRect();
            if (r == null) {
                continue;
            }
            boolean isInside = r.hasPositionInside(pos);
            if (isInside) aa.handleCharInside(chr);
            else aa.handleCharOutside(chr);
        }
    }

    public void drop(Drop drop, Position posFrom, Position posTo) {
        drop(drop, posFrom, posTo, false);
    }

    /**
     * Drops an item to this map, given a {@link Drop}, a starting Position and an ending Position.
     * Immediately broadcasts the drop packet.
     *
     * @param drop              The Drop to drop.
     * @param posFrom           The Position that the drop starts off from.
     * @param posTo             The Position where the drop lands.
     * @param ignoreTradability if the drop should ignore tradability (i.e., untradable items won't disappear)
     */
    public void drop(Drop drop, Position posFrom, Position posTo, boolean ignoreTradability) {
        boolean isTradable = true;
        Item item = drop.getItem();
        if (item != null) {
            int itemID = item.getItemId();
            ItemInfo itemInfo = ItemData.getItemInfoByID(itemID);
            if (ItemConstants.isEquip(itemID)) {
                Equip equip = (Equip) item;
                if (equip.hasAttribute(EquipAttribute.UnTradable) ||
                        equip.hasAttribute(EquipAttribute.TradedOnceWithinAccount) ||
//                        !equip.isTradable() ||
                        equip.isAccountSharable() ||
                        equip.isTradeBlock()) {
                    isTradable = false;
                }
                if (!equip.hasAttribute(EquipAttribute.UnTradable) && !equip.hasAttribute(EquipAttribute.TradedOnceWithinAccount) && equip.isTradeDisableWhenEquip() && !equip.isTradeBlock()) {
                    isTradable = true;
                }
            } else {
                isTradable = ignoreTradability || (item.isTradable() && itemInfo != null && !itemInfo.isQuest() && !itemInfo.isAccountSharable()) || GameConstants.isIntensePowerCrystal(itemID);
            }
            // Elite Channel Advantages:
            if (itemID == ItemConstants.SPECIAL_BEAUTY_COUPON || itemID == ItemConstants.REWARD_COIN) {
                isTradable = false;
            }
        }
        drop.setPosition(posTo);
        if (isTradable) {
            addLife(drop);
            this.dropExpiry.add(new DropExpire(drop.getObjectId(), System.currentTimeMillis() + GameConstants.DROP_REMAIN_ON_GROUND_TIME));
        } else {
            drop.setObjectId(getNewObjectID()); // just so the client sees the drop
        }
        // Check for collision items such as exp orbs from combo kills
        if (!isTradable) {
            drop.getItem().deleteFromSQL();
            broadcast(DropPool.dropEnterField(drop, posFrom, 0, DropEnterType.FadeAway));
        } else if (drop.getItem() != null && ItemConstants.isCollisionLootItem(drop.getItem().getItemId())) {
            broadcast(DropPool.dropEnterFieldCollisionPickUp(drop, posFrom, 0));
        } else {
            for (Char chr : getChars()) {
                if (drop.canBePickedUpBy(chr)) {
                    chr.write(DropPool.dropEnterField(drop, posFrom, posTo, 0, drop.canBePickedUpBy(chr)));
                }
            }
        }
    }

    /**
     * Drops an item to this map, given a {@link Drop}, a starting Position and an ending Position.
     * Broadcasts the drop packet a specified amount of Milliseconds delay.
     *
     * @param drop              The Drop to drop.
     * @param posFrom           The Position that the drop starts off from.
     * @param posTo             The Position where the drop lands.
     * @param ignoreTradability If the drop should ignore tradability (i.e., untradable items won't disappear)
     * @param delay             Millisecond delay to drop the item at.
     */
    public void drop(Drop drop, Position posFrom, Position posTo, boolean ignoreTradability, int delay) {
        boolean isTradable = true;
        Item item = drop.getItem();
        if (item != null) {
            ItemInfo itemInfo = ItemData.getItemInfoByID(item.getItemId());
            // must be tradable, and if not an equip, not a quest item
            isTradable = ignoreTradability || (item.isTradable() && (ItemConstants.isEquip(item.getItemId()) || itemInfo != null && !itemInfo.isQuest()));
        }
        drop.setPosition(posTo);
        if (isTradable) {
            addLife(drop);
            this.dropExpiry.add(new DropExpire(drop.getObjectId(), System.currentTimeMillis() + GameConstants.DROP_REMAIN_ON_GROUND_TIME));
        } else {
            drop.setObjectId(getNewObjectID()); // just so the client sees the drop
        }
        // Check for collision items such as exp orbs from combo kills
        if (!isTradable) {
            broadcast(DropPool.dropEnterField(drop, posFrom, 0, DropEnterType.FadeAway));
        } else if (drop.getItem() != null && ItemConstants.isCollisionLootItem(drop.getItem().getItemId())) {
            broadcast(DropPool.dropEnterFieldCollisionPickUp(drop, posFrom, 0));
        } else {
            for (Char chr : getChars()) {
                if (drop.canBePickedUpBy(chr)) {
                    chr.write(DropPool.dropEnterField(drop, posFrom, posTo, 0, drop.canBePickedUpBy(chr), delay));
                }
            }
        }
    }

    public void drop(Drop drop, Position posFrom, Position posTo, boolean ignoreTradability, int delay, Char owner) {
        boolean isTradable = true;
        Item item = drop.getItem();
        if (item != null) {
            ItemInfo itemInfo = ItemData.getItemInfoByID(item.getItemId());
            // must be tradable, and if not an equip, not a quest item
            isTradable = ignoreTradability || (item.isTradable() && (ItemConstants.isEquip(item.getItemId()) || itemInfo != null && !itemInfo.isQuest()));
        }
        drop.setPosition(posTo);
        if (isTradable) {
            addLife(drop);
            this.dropExpiry.add(new DropExpire(drop.getObjectId(), System.currentTimeMillis() + GameConstants.DROP_REMAIN_ON_GROUND_TIME));
        } else {
            drop.setObjectId(getNewObjectID()); // just so the client sees the drop
        }
        // Check for collision items such as exp orbs from combo kills
        if (!isTradable) {
            owner.write(DropPool.dropEnterField(drop, posFrom, 0, DropEnterType.FadeAway));
        } else if (drop.getItem() != null && ItemConstants.isCollisionLootItem(drop.getItem().getItemId())) {
            owner.write(DropPool.dropEnterFieldCollisionPickUp(drop, posFrom, 0));
        } else {
            for (Char chr : getChars()) {
                if (drop.canBePickedUpBy(chr)) {
                    chr.write(DropPool.dropEnterField(drop, posFrom, posTo, 0, drop.canBePickedUpBy(chr), delay));
                }
            }
        }
    }

    /**
     * Drops a {@link Drop} according to a given {@link DropInfo DropInfo}'s specification.
     *
     * @param dropInfo The
     * @param posFrom  The Position that hte drop starts off from.
     * @param posTo    The Position where the drop lands.
     */
    public void drop(DropInfo dropInfo, Position posFrom, Position posTo, Char owner, boolean isExclusive, boolean onlyChar) {
        int total = getDrops().size();
        int ownerID = owner != null ? owner.getId() : 0;
        if (total >= 500 && !FieldConstants.BOSS_BATTLE_MAP.contains(getId())) {
            for (Char chr : getChars()) {
                if (chr.getId() == ownerID && chr.getInstance() == null) {
                    for (Drop drop : getDrops()) {
                        broadcast(DropPool.dropLeaveField(DropLeaveType.Fade, ownerID, drop.getObjectId(), (short) 0, 0, 0));
                        removeLife(drop.getObjectId(), true);
                    }
                }
            }
        }
        int itemID = dropInfo.getItemID();
        Item item;
        Drop drop = new Drop(-1);
        drop.setPosition(posTo);
        drop.setOwnerID(ownerID);
        drop.setExplosiveDrop(isExclusive);
        drop.setSpecialDrop(BossConstants.isBossSet(itemID));
        Set<Integer> quests = new HashSet<>();
        if (itemID != 0) {
            item = ItemData.getItemDeepCopy(itemID, true);
            if (item != null) {
                item.setQuantity(dropInfo.getQuantity());
                drop.setItem(item);
                ItemInfo ii = ItemData.getItemInfoByID(itemID);
                if (ii != null && ii.isQuest()) {
                    quests = ii.getQuestIDs();
                }
            } else {
                System.out.println("Was not able to find the item to drop! id = " + itemID);
                return;
            }
        } else {
            drop.setMoney(net.swordie.ms.GameSettings.capMesoDrop(dropInfo.getMoney())); // meso.pickup.max
        }
        addLife(drop);

        final long now = System.currentTimeMillis();
        drop.setExpireTime(FileTime.fromLong(now + GameConstants.DROP_REMOVE_OWNERSHIP_TIME));
        this.dropExpiry.add(new DropExpire(drop.getObjectId(), now + GameConstants.DROP_REMAIN_ON_GROUND_TIME));

        if (onlyChar && owner != null) {
            if (owner.hasAnyQuestsInProgress(quests)) {
                owner.write(DropPool.dropEnterField(drop, posFrom, posTo, ownerID, drop.canBePickedUpBy(owner)));
            }
            if (itemID == GameConstants.MOB_SOUL && ownerID != 0) {
                owner.write(DropPool.dropEnterFieldSoul(drop, posFrom, ownerID));
                getTimer().addEvent(() -> removeDrop(drop.getObjectId(), ownerID, true, -1), 200, TimeUnit.MILLISECONDS);
            }
        } else {
            getTimer().addEvent(() -> drop.setOwnerID(0), GameConstants.DROP_REMOVE_OWNERSHIP_TIME, TimeUnit.MILLISECONDS);
            for (Char chr : getChars()) {
                if (chr.getId() == ownerID && chr.getInstance() == null) {
                    if (chr.hasAnyQuestsInProgress(quests)) {
                        chr.write(DropPool.dropEnterField(drop, posFrom, posTo, ownerID, drop.canBePickedUpBy(chr)));
                    }
                    if (itemID == GameConstants.MOB_SOUL && chr.getId() == ownerID) {
                        chr.write(DropPool.dropEnterFieldSoul(drop, posFrom, ownerID));
                        getTimer().addEvent(() -> removeDrop(drop.getObjectId(), ownerID, true, -1), 200, TimeUnit.MILLISECONDS);
                    }
                }
            }
        }
    }

    public void drop(DropInfo dropInfo, Char owner, Position posFrom) {
        int itemID = dropInfo.getItemID();
        Drop drop = new Drop(-1);
        drop.setPosition(posFrom);
        drop.setOwnerID(owner.getId());
        drop.setExplosiveDrop(false);
        drop.setSpecialDrop(false);
        Set<Integer> quests = new HashSet<>();
        if (itemID != 0) {
            Item item = ItemData.getItemDeepCopy(itemID, true);
            ItemInfo ii = ItemData.getItemInfoByID(itemID);
            if (item != null) {
                item.setQuantity(dropInfo.getQuantity());
                drop.setItem(item);
                if (ii != null) {
                    if (ii.isQuest()) {
                        quests = ii.getQuestIDs();
                    }
                    if (ii.getReqQuestOnProgress() != 0) {
                        quests.add(ii.getReqQuestOnProgress());
                    }
                }
            }
        } else {
            drop.setMoney(net.swordie.ms.GameSettings.capMesoDrop(dropInfo.getMoney())); // meso.pickup.max
        }
        if (itemID == 0 && owner.canAddMoney(drop.getMoney())) {
            int bonusMesos = 0;
            if (owner.getGuild() != null) {
                GuildSkill gs = owner.getGuild().getSkillById(GuildConstants.SPOTTING_SMALL_CHANGE);
                SkillInfo si = SkillData.getSkillInfoById(GuildConstants.SPOTTING_SMALL_CHANGE);
                if (gs != null && si != null) {
                    bonusMesos = si.getValue(mesoG, gs.getLevel());
                }
            }
            owner.addMoney(drop.getMoney() + bonusMesos);
            owner.handleMoneyGain(drop.getMoney());
            owner.write(WvsContext.dropPickupMessage(drop.getMoney(), (short) 0, (short) bonusMesos));
        } else if (itemID != 0) {
            owner.addDrop(drop);
        } else {
            addLife(drop);

            final long now = System.currentTimeMillis();
            drop.setExpireTime(FileTime.fromLong(now + GameConstants.DROP_REMOVE_OWNERSHIP_TIME));
            this.dropExpiry.add(new DropExpire(drop.getObjectId(), now + GameConstants.DROP_REMAIN_ON_GROUND_TIME));

            getTimer().addEvent(() -> drop.setOwnerID(0), GameConstants.DROP_REMOVE_OWNERSHIP_TIME, TimeUnit.MILLISECONDS);
            for (Char chr : getChars()) {
                if (chr.hasAnyQuestsInProgress(quests)) {
                    chr.write(DropPool.dropEnterField(drop, posFrom, posFrom, owner.getId(), drop.canBePickedUpBy(chr)));
                }
            }
        }
    }

    /**
     * Drops a Set of {@link DropInfo}s from a base Position.
     *
     * @param dropInfos The Set of DropInfos.
     * @param position  The Position the initial Drop comes from.
     */
    public void drop(Set<DropInfo> dropInfos, Position position, Char owner) {
        drop(dropInfos, findFootHoldBelow(position), position, owner, 0, 0, false, new ArrayList<>());
    }

    public void drop(Drop drop, Position position) {
        drop(drop, position, false);
    }

    /**
     * Drops a {@link Drop} at a given Position. Calculates the Position that the Drop should land at.
     *
     * @param drop        The Drop that should be dropped.
     * @param position    The Position it is dropped from.
     * @param fromReactor if it quest item the item will disapear
     */
    public void drop(Drop drop, Position position, boolean fromReactor) {
        int x = position.getX();
        Foothold fh = findFootHoldBelow(position);
        if (fh != null) {
            Position posTo = new Position(x, fh.getYFromX(x));
            drop(drop, position, posTo, fromReactor);
        }
    }

    /**
     * Drops a Set of {@link DropInfo}s, locked to a specific {@link Foothold}.
     * Not all drops are guaranteed to be dropped, as this method calculates whether or not a Drop should drop, according
     * to the DropInfo's prop chance.
     *
     * @param dropInfos The Set of DropInfos that should be dropped.
     * @param fh        The Foothold this Set of DropInfos is bound to.
     * @param position  The Position the Drops originate from.
     * @param mesoRate  The added meso rate of the character.
     * @param dropRate  The added drop rate of the character.
     */
    public void drop(Set<DropInfo> dropInfos, Foothold fh, Position position, Char owner, int mesoRate, int dropRate, boolean isExclusive, List<Integer> filterList, boolean onlyChar) {
        //x is position of Character.
        int x = position.getX();
        if (!FieldConstants.BOSS_BATTLE_MAP.contains(getId())) {
            //minX = min Value of Platform
            //maxX = max Value of Platform
            int minX = 0;
            int maxX = 0;
            List<Foothold> currentPlatform = new ArrayList<>();
            if (fh != null) {
                Foothold footholdMax = fh;
                Foothold footholdMin = fh;
                for (Foothold foothold : getNonWallFootholds()) {
                    if (fh.getX1() == foothold.getX2()) {
                        footholdMin = foothold;
                        currentPlatform.add(foothold);
                    } else if (fh.getX2() == foothold.getX1()) {
                        footholdMax = foothold;
                        currentPlatform.add(foothold);
                    }
                }
                //Nếu footMax == footMin thì platform đó chỉ có 2 điểm.
                if (footholdMax == footholdMin) {
                    minX = footholdMax.getX1();
                    maxX = footholdMax.getX2();
                } else {
                    minX = footholdMin.getX1();
                    maxX = footholdMax.getX2();
                }
            } else if (fh == null) {
                minX = maxX = x;
            }

            int diff = 0;
            Set<DropInfo> newDropInfos = new HashSet<>();
            //Modify Custom Drop ETC Style.
            for (DropInfo dropInfo : dropInfos) {
                //Chỉ cho trường hợp min là 1.
                //Ex: Coin có min value drop là 1 và max là 5. -> Tách thành 5 cái.
                //for(int i = 0; i < 4; i++)
                if ((dropInfo.getItemID() / 1000000 == 4 || dropInfo.getItemID() / 1000000 == 2) && dropInfo.getMaxQuant() > 1 && isExclusive) {
                    DropInfo modifyDropInfo = null;
                    if (dropInfo.getMinQuant() == dropInfo.getMaxQuant() && dropInfo.getMinQuant() > 1) {
                        for (int i = 0; i < dropInfo.getMaxQuant(); i++) {
                            modifyDropInfo = new DropInfo(dropInfo.getItemID(), dropInfo.getChance(), 1, 1);
                            newDropInfos.add(modifyDropInfo);
                        }
                    } else if (dropInfo.getMinQuant() == 1) {
                        for (int i = 0; i < dropInfo.getMaxQuant(); i++) {
                            modifyDropInfo = new DropInfo(dropInfo.getItemID(), dropInfo.getChance() / dropInfo.getMaxQuant(), dropInfo.getMinQuant(), 1);
                            newDropInfos.add(modifyDropInfo);
                        }
                    }
                } else {
                    switch (getId()) {
                        case GameConstants.MOON_BUNNY_STAGE:
                            if (dropInfo.getItemID() == GameConstants.PRIMROSE_SEED) {
                                newDropInfos.add(dropInfo);
                            }
                            break;
                        case GameConstants.FIRST_TIME_TOGETHER_STAGE_1:
                            if (dropInfo.getItemID() == 4001007) {
                                newDropInfos.add(dropInfo);
                            }
                            break;
                        case GameConstants.FIRST_TIME_TOGETHER_STAGE_4:
                            if (dropInfo.getItemID() >= 4034329 && dropInfo.getItemID() <= 4034338) {
                                newDropInfos.add(dropInfo);
                            }
                            break;
                        case GameConstants.HUNGRY_MUTO_NORMAL_STAGE:
                        case GameConstants.HUNGRY_MUTO_HARD_STAGE:
                            if (dropInfo.getItemID() >= 2435856 && dropInfo.getItemID() <= 2435872) {
                                newDropInfos.add(dropInfo);
                            }
                            break;
                        default:
                            newDropInfos.add(dropInfo);
                            break;
                    }
                }
            }
            for (DropInfo dropInfo : newDropInfos) {
                //dropInfo.setChance(10000); //This is for test only LOL.
                if (filterList.size() > 0 && filterList.contains(dropInfo.getItemID())) {
                    continue;
                }
                if (dropInfo.willDrop(dropRate)) {
                    //System.out.println("Item Drop: " + dropInfo.getItemID());
                    //Lấy vị trí hiện tại + khoảng cách.
                    x = x + diff;
                    //Nếu x lớn hơn điểm x lớn nhất của platform thì lấy điểm lớn nhất - 5
                    if (x > maxX) {
                        x = maxX - 10;
                    }
                    //Nếu x bé hơn điểm x bé nhất của platform thì lấy điểm nhỏ nhất + 5
                    else if (x < minX) {
                        x = minX + 10;
                    }
                    Position posTo;
                    if (fh == null) {
                        posTo = position.deepCopy();
                    } else {
                        Foothold currentFoothold = fh;
                        for (Foothold foothold : currentPlatform) {
                            if (foothold.getX1() <= x && foothold.getX2() >= x) {
                                currentFoothold = foothold;
                            }
                        }
                        posTo = new Position(x, currentFoothold.getYFromX(x));
                    }
                    // Copy the drop info for money, as we chance the amount that's in there.
                    // Not copying -> original dropinfo will keep increasing in mesos
                    DropInfo copy = null;
                    if (dropInfo.isMoney()) {
                        copy = dropInfo.deepCopy();
                        int money = dropInfo.getMoney();
                        copy.setMoney((int) (money * ((100 + mesoRate) / 100D)));
                    }
                    drop(copy != null ? copy : dropInfo, position, posTo, owner, isExclusive, onlyChar);
                    diff = isExclusive ? (diff < 0 ? Math.abs(diff - GameConstants.DROP_DIFF) : -(diff + GameConstants.DROP_DIFF)) : diff < 0 ? Math.abs(diff - 30) : -(diff + 30);
                    dropInfo.generateNextDrop();
                }
            }
        } else {
            int minX = getMinMaxNonWallFH().getLeft().getX1();
            int maxX = getMinMaxNonWallFH().getRight().getX1();
            int diff = 0;
            Set<DropInfo> newDropInfos = new HashSet<>();
            //Modify Custom Drop ETC Style.
            for (DropInfo dropInfo : dropInfos) {
                if ((dropInfo.getItemID() / 1000000 == 4 || dropInfo.getItemID() == 2433103) && dropInfo.getMaxQuant() > 1 && dropInfo.getMaxQuant() <= 10 && isExclusive) {
                    DropInfo modifyDropInfo = null;
                    if (dropInfo.getMinQuant() == dropInfo.getMaxQuant() && dropInfo.getMinQuant() > 1) {
                        for (int i = 0; i < dropInfo.getMaxQuant(); i++) {
                            modifyDropInfo = new DropInfo(dropInfo.getItemID(), dropInfo.getChance(), 1, 1);
                            newDropInfos.add(modifyDropInfo);
                        }
                    } else if (dropInfo.getMinQuant() == 1) {
                        for (int i = 0; i < dropInfo.getMaxQuant(); i++) {
                            modifyDropInfo = new DropInfo(dropInfo.getItemID(), dropInfo.getChance() / dropInfo.getMaxQuant(), dropInfo.getMinQuant(), 1);
                            newDropInfos.add(modifyDropInfo);
                        }
                    }
                } else {
                    newDropInfos.add(dropInfo);
                }
            }
            for (DropInfo dropInfo : newDropInfos) {
                if (filterList.size() > 0 && filterList.contains(dropInfo.getItemID())) {
                    continue;
                }
                if (dropInfo.willDrop(dropRate)) {
                    x = x + diff; //Lấy vị trí hiện tại + khoảng cách.
                    if (x > maxX) { //Nếu x lớn hơn điểm x lớn nhất của platform thì lấy điểm lớn nhất - 15
                        x = maxX - 15;
                    } else if (x < minX) { //Nếu x bé hơn điểm x bé nhất của platform thì lấy điểm nhỏ nhất + 15
                        x = minX + 15;
                    }
                    if (fh == null) {
                        fh = findFootHoldBelow(position);
                        if (fh == null) {
                            continue;
                        }
                    }
                    Position posTo = new Position(x, fh.getYFromX(x));
                    DropInfo copy = null;
                    if (dropInfo.isMoney()) {
                        copy = dropInfo.deepCopy();
                        copy.setMoney((int) (dropInfo.getMoney() * ((100 + mesoRate) / 100D)));
                    }
                    drop(copy != null ? copy : dropInfo, position, posTo, owner, isExclusive, onlyChar);
                    if (isExclusive) {
                        if (diff < 0) {
                            diff = Math.abs(diff - (GameConstants.DROP_DIFF));
                        } else {
                            diff = -(diff + (GameConstants.DROP_DIFF));
                        }
                    } else {
                        if (diff < 0) {
                            diff = Math.abs(diff - 10);
                        } else {
                            diff = -(diff + 10);
                        }
                    }
                    //diff = isExclusive ? (diff < 0 ? Math.abs(diff - GameConstants.DROP_DIFF) : -(diff + GameConstants.DROP_DIFF)) : diff < 0 ? Math.abs(diff - 10) : -(diff + 10);
                    dropInfo.generateNextDrop();
                }
            }
        }
    }

    public void drop(Set<DropInfo> dropInfos, Foothold fh, Position position, Char owner, int mesoRate, int dropRate, boolean isExclusive, List<Integer> filterList) {
        drop(dropInfos, fh, position, owner, mesoRate, dropRate, isExclusive, filterList, false);
    }

    public void dropForPartyMember(Set<DropInfo> dropInfos, Foothold fh, Position position, Char owner, Mob mob, boolean isExclusive, List<Integer> filterList) {
        if (owner.getParty() != null) {
            for (Char chr : owner.getParty().getPartyMembersInSameFieldWithChr(owner)) {
                drop(dropInfos, fh, position, chr, mob.getTotalMesoRate(chr), mob.getTotalDropRate(chr), isExclusive, filterList, true);
            }
        } else {
            drop(dropInfos, fh, position, owner, mob.getTotalMesoRate(owner), mob.getTotalDropRate(owner), isExclusive, filterList);
        }
    }

    public void dropForOwner(Set<DropInfo> dropInfos, Foothold fh, Position position, Char owner, Mob mob, boolean isExclusive, List<Integer> filterList) {
        drop(dropInfos, fh, position, owner, mob.getTotalMesoRate(owner), mob.getTotalDropRate(owner), isExclusive, filterList, true);
    }

    public void drop(Set<DropInfo> dropInfos, Position posFrom, Char owner, int mesoRate, int dropRate, List<Integer> filterList) {
        for (DropInfo dropInfo : dropInfos) {
            if (filterList.size() > 0 && filterList.contains(dropInfo.getItemID())) {
                continue;
            }
            if (dropInfo.willDrop(dropRate)) {
                DropInfo copy = null;
                if (dropInfo.isMoney()) {
                    copy = dropInfo.deepCopy();
                    copy.setMoney((int) (dropInfo.getMoney() * ((100 + mesoRate) / 100D)));
                }
                drop(copy != null ? copy : dropInfo, owner, posFrom);
                dropInfo.generateNextDrop();
            }
        }
    }

    /**
     * Drops a list of items evenly spaced along a line of the specified parameters.
     *
     * @param items     List of item ids.
     * @param quantitys List of item quantitys.
     * @param randomize If the items should be randomized drop in order.
     * @param range     Range overall that the items should be dropped along, centered on the starting position.
     * @param startPosX The X Position in which the Drops originate from.
     * @param startPosY The Y Position in which the Drops originate from.
     * @param delay     Delay between every drop.
     */

    public void dropItemsAlongLine(int[] items, int[] quantitys, boolean randomize, int range, int startPosX, int startPosY, int delay) {
        if (items.length == 0 || items.length != quantitys.length) {
            return; // avoid divide by zero error
        }

        List<HashMap<Integer, Integer>> itemList = new ArrayList<>();
        int i1 = 0;
        for (int index : items) {
            if (items[i1] != 0) {
                HashMap<Integer, Integer> quantityMap = new HashMap<>();
                quantityMap.put(items[i1], quantitys[i1]);
                itemList.add(i1, quantityMap);
                i1++;
            }
        }
        if (randomize) {
            Collections.shuffle(itemList);
        }

        Tuple<Foothold, Foothold> lrFh = getMinMaxNonWallFH();
        range = Math.max(range, items.length);
        int offset = Math.max((range / items.length) * 2, 3); // we want offset >= 3 || multiply by 2 so that the drops go past the start point
        int i2 = 0;
        for (var map : itemList) {
            int endPosX = startPosX - range + (offset * i2);
            endPosX = Math.max(endPosX, lrFh.getLeft().getX1()); // left is lowest x val
            endPosX = Math.min(endPosX, lrFh.getRight().getX1()); // right is highest x val

            int itemID = (Integer) map.keySet().toArray()[0];
            int quantity = (Integer) map.get(itemID);

            i2++;

            if (itemID > 999999) { // item
                Drop drop = new Drop(getNewObjectID());
                drop.setItem(ItemData.getItemDeepCopy(itemID));
                drop.getItem().setQuantity(quantity);
                Position startPos = new Position(startPosX, startPosY);
                Foothold fh = findFootHoldBelow(new Position(endPosX, startPosY - 25));
                Position endPos = new Position(endPosX, fh != null ? fh.getY1() : 0);
                drop(drop, startPos, endPos, true, delay * i2);
            }
        }
    }

    public void dropItemsAlongLine(int[] items, int[] quantitys, boolean randomize, int range, int startPosX, int startPosY, int delay, Char owner) {
        if (owner.getParty() == null) {
            dropItemsAlongLine(items, quantitys, randomize, range, startPosX, startPosY, delay);
            return;
        }
        if (items.length == 0 || items.length != quantitys.length) {
            return; // avoid divide by zero error
        }
        List<HashMap<Integer, Integer>> itemList = new ArrayList<>();
        int i1 = 0;
        for (int index : items) {
            if (items[i1] != 0) {
                HashMap<Integer, Integer> quantityMap = new HashMap<>();
                quantityMap.put(items[i1], quantitys[i1]);
                itemList.add(i1, quantityMap);
                i1++;
            }
        }
        if (randomize) {
            Collections.shuffle(itemList);
        }

        Tuple<Foothold, Foothold> lrFh = getMinMaxNonWallFH();
        range = Math.max(range, items.length);
        int offset = Math.max((range / items.length) * 2, 3); // we want offset >= 3 || multiply by 2 so that the drops go past the start point
        int i2 = 0;
        for (HashMap map : itemList) {
            int endPosX = startPosX - range + (offset * i2);
            endPosX = Math.max(endPosX, lrFh.getLeft().getX1()); // left is lowest x val
            endPosX = Math.min(endPosX, lrFh.getRight().getX1()); // right is highest x val

            int itemID = (Integer) map.keySet().toArray()[0];
            int quantity = (Integer) map.get(itemID);

            i2++;

            if (itemID > 999999) { // item
                for (Char chr : owner.getParty().getPartyMembersInSameFieldWithChr(owner)) {
                    Drop drop = new Drop(getNewObjectID());
                    drop.setItem(ItemData.getItemDeepCopy(itemID));
                    drop.getItem().setQuantity(quantity);
                    Position startPos = new Position(startPosX, startPosY);
                    Foothold fh = findFootHoldBelow(new Position(endPosX, startPosY - 25));
                    Position endPos = new Position(endPosX, fh != null ? fh.getY1() : 0);
                    drop(drop, startPos, endPos, true, delay * i2, chr);
                }
            }
        }
    }

    public List<Portal> getClosestPortal(Rect rect) {
        List<Portal> portals = new ArrayList<>();
        for (Portal portals2 : getPortals()) {
            int x = portals2.getX();
            int y = portals2.getY();
            if (x >= rect.getLeft() && y >= rect.getTop()
                    && x <= rect.getRight() && y <= rect.getBottom()) {
                portals.add(portals2);
            }
        }
        return portals;
    }

    public Char getClosestChars(Rect rect) {
        for (Char chr : getChars()) {
            if (chr != null && chr.getPosition() != null) {
                int x = chr.getPosition().getX();
                int y = chr.getPosition().getY();
                if (x >= rect.getLeft() && y >= rect.getTop()
                        && x <= rect.getRight() && y <= rect.getBottom()) {
                    return chr;
                }
            }
        }
        return null;
    }

    public Char findClosestChar(Position pos, double maxDistance) {
        Char closestChar = null;
        double minDistance = Double.MAX_VALUE;
        for (Char chr : getChars()) {
            if (chr != null && chr.getPosition() != null) {
                double distance = calculateDistance(pos, chr.getPosition());
                if (distance <= maxDistance && distance < minDistance) {
                    minDistance = distance;
                    closestChar = chr;
                }
            }
        }
        return closestChar;
    }

    private double calculateDistance(Position pos1, Position pos2) {
        double deltaX = pos1.getX() - pos2.getX();
        double deltaY = pos1.getY() - pos2.getY();
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY);
    }

    public void execUserEnterScript(Char chr) {
        if (!ScriptManagerImpl.GAME_SCRIPTS_ENABLED) return;
        chr.clearCurrentDirectionNode();
        String customFieldScriptName = CustomFieldScripts.getScriptForField(getId());
        if (customFieldScriptName != null) {
            if (net.swordie.ms.connection.protocol.Protocol.active().version()
                    == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3
                    && chr.getInstance() != null && BossConstants.getBossEntranceMapForFightField(getId()) != 0) {
                var boss = net.swordie.ms.constants.BossHpConstants.getCurrentBossParty(chr);
                int order = boss == null ? -1 : boss.getOrderId();
                // Dedicated encounters own their per-character original cutscene/phase initialization.
                if (!java.util.Set.of(13, 15, 19, 23, 28, 30, 31, 33, 34, 35, 36, 37, 38, 110).contains(order)) {
                    net.swordie.ms.world.boss.BossHelper.playBossEntry(chr, () -> {
                        synchronized (Field.this) {
                            if (hasProperty("Tms282EntryInitialization")) return;
                            setProperty("Tms282EntryInitialization", true);
                        }
                        chr.getScriptManager().startScript(chr, getId(), customFieldScriptName, ScriptType.Field);
                    });
                    return;
                }
            }
            chr.getScriptManager().startScript(chr, getId(), customFieldScriptName, ScriptType.Field);
        } else if (getOnUserEnter() != null && !getOnUserEnter().equalsIgnoreCase("")) {
            String script = getOnUserEnter();
            chr.getScriptManager().startScript(chr, getId(), script, ScriptType.Field);
        }
    }

    public boolean isUserFirstEnter() {
        return userFirstEnter;
    }

    public void setUserFirstEnter(boolean userFirstEnter) {
        this.userFirstEnter = userFirstEnter;
    }

    public String getFieldScript() {
        return fieldScript;
    }

    public void setFieldScript(String fieldScript) {
        this.fieldScript = fieldScript;
    }

    public Mob spawnMobWithAppearType(int id, int x, int y, int appearType, int option) {
        Mob mob = MobData.getMobDeepCopyById(id);
        Position pos = new Position(x, y);
        mob.setPosition(pos.deepCopy());
        mob.setPrevPos(pos.deepCopy());
        mob.setPosition(pos.deepCopy());
        mob.setNotRespawnable(true);
        mob.setAppearType((byte) appearType);
        mob.setOption(option);
        mob.setField(this);
        spawnLife(mob, null);
        return mob;
    }

    /**
     * Spawns an NPC at given coordinates.
     */
    public void spawnNpc(int npcId, int pX, int pY) {
        Npc npc = NpcData.getNpcDeepCopyById(npcId);
        Position position = new Position(pX, pY);
        npc.setPosition(position);
        npc.setCy(pY);
        npc.setRx0(pX + 50);
        npc.setRx1(pX - 50);
        npc.setFh(findFootHoldBelow(new Position(pX, pY - 2)).getId());
        npc.setNotRespawnable(true);
        npc.setField(this);

        spawnLife(npc, null);
    }

    /**
     * 按地圖內玩家 Boss 難度上下文解析標準血量，供 Java 側刷怪（如四門 reactor）使用。
     */
    private long resolveBossSpawnHp(int mobTemplateId) {
        for (Char chr : getChars()) {
            long hp = BossHpConstants.resolveMaxHp(mobTemplateId, chr);
            if (hp > 0) {
                return hp;
            }
        }
        return BossHpConstants.getMaxHp(mobTemplateId);
    }

    /**
     * Spawns a mob at given point.
     *
     * @param id          ID of the mob
     * @param p           point to spawn mob at
     * @param respawnable whether mob will respawn automatically
     * @param hp          hp of mob. Set to 0 for default hp.
     * @return
     */
    public Mob spawnMob(int id, Point p, boolean respawnable, long hp) {
        return spawnMob(id, p.getLocation().x, p.getLocation().y, respawnable, hp);
    }

    public Mob spawnMob(int id, int x, int y, boolean respawnable) {
        return spawnMob(id, x, y, respawnable, 0);
    }

    public Mob spawnMob(int id, int x, int y, boolean respawnable, long hp) {
        return spawnMob(id, x, y, respawnable, hp, 0);
    }

    public Mob spawnMob(int id, int x, int y, boolean respawnable, long hp, long exp) {
        Mob mob = MobData.getMobDeepCopyById(id);
        if (mob == null) {
            System.err.printf("spawnMob failed: mob %d not found (field %d)%n", id, getId());
            return null;
        }
        Position pos = new Position(x, y);
        mob.setPosition(pos.deepCopy());
        mob.setPrevPos(pos.deepCopy());
        mob.setPosition(pos.deepCopy());
        mob.setNotRespawnable(!respawnable);
        mob.setField(this);
        if (hp > 0) {
            mob.setHp(hp);
            mob.setMaxHp(hp);
        } else {
            long bossHp = resolveBossSpawnHp(id);
            if (bossHp > 0) {
                mob.setHp(bossHp);
                mob.setMaxHp(bossHp);
            } else if (mob.isBoss()) {
                long fallbackHp = BossHpConstants.getMaxHp(id);
                if (fallbackHp > 0 && mob.getMaxHp() < 10_000) {
                    mob.setHp(fallbackHp);
                    mob.setMaxHp(fallbackHp);
                }
            }
        }
        if (exp > 0) {
            mob.setExp(exp);
        }
        if (id == 8880300 || id == 8880303 || id == 8880304 || id == 8880340) {
            mob.getWillHPlist().add(666);
            mob.getWillHPlist().add(333);
            mob.getWillHPlist().add(3);
            broadcast(WillPacket.setHp(mob.getWillHPlist(), this, id, id + 3, id + 4));
        }
        if (id == 8880301 || id == 8880341) {
            mob.getWillHPlist().add(500);
            mob.getWillHPlist().add(3);
            broadcast(WillPacket.setHp(mob.getWillHPlist()));
        }
        if (id == 8880400 || id == 8880405 || id == 8880415) {
            MobSkill mobSkill = new MobSkill();
            mobSkill.setSkillID(MobSkillID.VHillaSlash.getVal());
            mobSkill.setLevel(1);
            mobSkill.setFlip(mob.isFlip());
            mob.setSkillDelay(mobSkill.getSkillDelay());
            mobSkill.applyEffect(mob);
            MobSkill.JinHillaGlassTime(mob, 150);
        }

        // Chaos Pink Bean
        if (id == 8820114) { // Ariel
            mob.getRevives().add(8820101);
        }
        spawnLife(mob, null);
        return mob;
    }

    public Mob spawnMobForTime(int id, int x, int y, int timeMS) {
        Mob mob = MobData.getMobDeepCopyById(id);
        Position pos = new Position(x, y);
        mob.setPosition(pos.deepCopy());
        mob.setPrevPos(pos.deepCopy());
        mob.setPosition(pos.deepCopy());
        mob.setNotRespawnable(true);
        mob.setField(this);
        mob.setHp(Math.min(mob.getHp() * 20L, Long.MAX_VALUE));
        mob.setMaxHp(Math.min(mob.getMaxHp() * 20L, Long.MAX_VALUE));
        spawnLife(mob, null);
        ScheduledFuture<?> sf = getTimer().addEvent(() -> removeMob(mob.getObjectId()), timeMS, TimeUnit.MILLISECONDS);
        addLifeSchedule(mob.getObjectId(), sf);
        return mob;
    }

    public Mob spawnMob(int id, int x, int y, boolean respawnable, long hp, int pad, int mad, int pdr, int mdr, long exp) {
        Mob mob = MobData.getMobDeepCopyById(id);
        Position pos = new Position(x, y);
        mob.setPosition(pos.deepCopy());
        mob.setPrevPos(pos.deepCopy());
        mob.setPosition(pos.deepCopy());
        mob.setNotRespawnable(!respawnable);
        mob.setField(this);
        if (hp > 0) {
            mob.setHp(hp);
            mob.setMaxHp(hp);
        }
        if (pad > 0) {
            mob.setPad(pad);
        }
        if (mad > 0) {
            mob.setMad(mad);
        }
        if (pdr > 0) {
            mob.getForcedMobStat().setPdr(pdr);
        }
        if (mdr > 0) {
            mob.getForcedMobStat().setMdr(mdr);
        }
        if (exp > 0) {
            mob.getForcedMobStat().setExp(exp);
        }
        spawnLife(mob, null);
        return mob;
    }

    public Mob spawnPierre(int id, Position position, long hp, boolean isFlip, long lifeTime) {
        Mob mob = MobData.getMobDeepCopyById(id);
        Position pos = new Position(position.getX(), position.getY());
        mob.setPosition(pos.deepCopy());
        mob.setPrevPos(pos.deepCopy());
        mob.setPosition(pos.deepCopy());
        mob.setNotRespawnable(true);
        mob.setField(this);
        if (hp > 0) {
            mob.setHp(hp);
        }
        mob.setFlip(isFlip);
        mob.setLifeTime(lifeTime);
        spawnLife(mob, null);
        return mob;
    }

    public Mob spawnMobForOnlyChar(Char chr, int id, int x, int y, boolean respawnable) {
        Mob mob = MobData.getMobDeepCopyById(id);
        Position pos = new Position(x, y);
        mob.setControllerID(chr.getId());
        mob.setOwner(chr.getId());
        mob.setHomePosition(pos.deepCopy());
        mob.setPosition(pos.deepCopy());
        mob.setPrevPos(pos.deepCopy());
        mob.setPosition(pos.deepCopy());
        mob.setNotRespawnable(!respawnable);
        mob.setField(this);
        spawnLife(mob, null);
        return mob;
    }

    public void removeMobsByOnlyChar(Char chr) {
        for (Mob mob : getMobs()) {
            if (chr.getId() == mob.getOwner().getId()) {
                mob.remove();
            }
        }
    }

    public boolean hasMobById(int mobID) {
        return getLifeByTemplateId(mobID) != null && getLifeByTemplateId(mobID) instanceof Mob;
    }

    public Mob getMobByObjectId(int id) {
        for (Life mob : getLifes().values()) {
            if (mob instanceof Mob) {
                if (mob.getObjectId() == id) {
                    return (Mob) mob;
                }
            }
        }
        return null;
    }

    public Mob getMob(int id, int x, int y) {
        for (Life mob : getLifes().values()) {
            if (mob instanceof Mob) {
                if (mob.getPosition().equals(new Position(x, y)) && mob.getTemplateId() == id) {
                    return (Mob) mob;
                }
            }
        }
        return null;
    }

    public Mob getMobByTemplateId(int id) {
        for (Life mob : getLifes().values()) {
            if (mob instanceof Mob) {
                if (mob.getTemplateId() == id) {
                    return (Mob) mob;
                }
            }
        }
        return null;
    }

    public void spawnRuneStone() {
        if (!net.swordie.ms.GameSettings.runeEnabled()) return; // feature.rune
        if (getMobs().size() <= 0 || getBossMobID() != 0 || !isChannelField()) {
            return;
        }

        for (int i = 0; i < FieldConstants.BLOCKED_RUNE_MAPS.length; i++) {
            if (getId() == FieldConstants.BLOCKED_RUNE_MAPS[i]) {
                return;
            }
        }

        if (getRuneStone() == null) {
            RuneStone runeStone = new RuneStone().getRandomRuneStone(this);
            setRuneStone(runeStone);
            broadcast(FieldPacket.runeStoneAppear(runeStone));
        }
    }

    public void useRuneStone(Client c, RuneStone runeStone) {
        Char chr = c.getChr();
        switch (runeStone.getRuneType()) {
            case Destruction:
                chr.chatMessage("破滅之輪：提升攻擊力，並獲得額外 100% 經驗值。");
                break;
            case Thunder:
                chr.chatMessage("雷之輪：降下強力的落雷，並獲得解放的輪之力。");
                break;
            case Giants:
                chr.chatMessage("巨人之輪：讓角色變得巨大，並獲得解放的輪之力。");
                break;
            case Darkness:
                chr.chatMessage("黑暗之輪：召喚強大的敵人，並獲得解放的輪之力。");
                break;
            case Blessing:
                chr.chatMessage("祝福之輪：在畫面中央召喚古代之輪，定期攻擊周圍的怪物，並獲得 200% 經驗值增益。");
                break;
            case Skill:
                chr.chatMessage("技能之輪：可重置冷卻的技能冷卻時間縮短為 5 秒，效果期間獲得額外 100% 經驗值。");
                break;
            case Purification:
                chr.chatMessage("淨化之輪：吸收被擊敗敵人的能量並釋放出淨化稜鏡，並獲得解放的輪之力。");
                break;
            case Contact:
                chr.chatMessage("光束之輪：以神聖光束連結輪與角色來攻擊敵人，並獲得解放的輪之力。");
                break;
            case Ignition:
                chr.chatMessage("燃燒之輪：攻擊時擴散強烈的火焰攻擊周圍敵人，並獲得解放的輪之力。");
                break;
        }
        RuneType runeType = runeStone.getRuneType();
        setRuneStone(null);
        chr.write(FieldPacket.runeActSuccess(runeType));
        chr.write(FieldPacket.runeStoneSkillAck(runeType));
        broadcast(FieldPacket.runeStoneDisappear(chr.getId()));
        getTimer().addEvent(this::spawnRuneStone, FieldConstants.RUNE_RESPAWN_TIME, TimeUnit.MINUTES);
    }

    public void runeStoneHordeEffect(int mobRateMultiplier, int duration) {
        double prevMobRate = getMobRate();
        setMobRate(getMobRate() * mobRateMultiplier); //Temporary increase in mob Spawn
        if (this.runeStoneHordesTimer != null && !this.runeStoneHordesTimer.isDone()) {
            this.runeStoneHordesTimer.cancel(true);
        }
        this.runeStoneHordesTimer = getTimer().addEvent(() -> setMobRate(prevMobRate), duration, TimeUnit.SECONDS);
    }

    public long getNextEliteSpawnTime() {
        return nextEliteSpawnTime;
    }

    public void setNextEliteSpawnTime(long nextEliteSpawnTime) {
        this.nextEliteSpawnTime = nextEliteSpawnTime;
    }

    public boolean canSpawnElite() {
        return isChannelField()
                && (getEliteState() == null || getEliteState() == EliteState.None)
                && getNextEliteSpawnTime() < System.currentTimeMillis();
    }

    public int getKilledElites() {
        return killedElites;
    }

    public void setKilledElites(int killedElites) {
        this.killedElites = killedElites;
    }

    public void incrementEliteKillCount() {
        setKilledElites(getKilledElites() + 1);
    }

    public EliteState getEliteState() {
        return eliteState;
    }

    public void setEliteState(EliteState eliteState) {
        this.eliteState = eliteState;
    }

    public List<Foothold> getNonWallFootholds() {
        return getFootholds().stream().filter(fh -> !fh.isWall()).collect(Collectors.toList());
    }



    public Foothold getRandomWalkableFoothold() {
        var fhs = getNonWallFootholds();
        return Util.getRandomFromCollection(fhs);
    }

    public List<Foothold> getNonWallFootholdsWithinRect(Rect rect) {
        List<Foothold> ret = new ArrayList<>();

        for (var fh : getNonWallFootholds()) {
            var midX = (fh.getX1() + fh.getX2()) / 2; // grab middle of foothold
            var midY = (fh.getY1() + fh.getY2()) / 2; // grab middle of foothold

            var midPos = new Position(midX, midY);

            if (rect.hasPositionInside(midPos)) {
                ret.add(fh);
            }
        }

        return ret;
    }

    public Position getRandomPosOnWalkableFoothold(int edgeDistance, boolean canSpawnOnPortal) {
        boolean found = false;
        Position pos = null;
        int iterations = 0;
        int maxAttempts = 500;
        while (!found && iterations < maxAttempts) {
            iterations++;
            Foothold fh = getRandomWalkableFoothold();
            Position tempPos = fh.getRandomPositionFromEdges(edgeDistance);

            if (canSpawnOnPortal) {
                found = true;
                pos = tempPos;
            } else {
                Rect rect = new Rect(
                        new Position(
                                tempPos.getX() - 75,
                                tempPos.getY() - 75),
                        new Position(
                                tempPos.getX() + 75,
                                tempPos.getY() + 75)
                );
                List<Portal> portals = getClosestPortal(rect);
                if (portals.isEmpty()) {
                    found = true;
                    pos = tempPos;
                }
            }
        }
        return pos;
    }

    public Position getRandomPosOnWalkableFoothold(int edgeDistance, boolean canSpawnOnLife, boolean canSpawnOnPortals) {
        boolean found = false;
        Position pos = null;
        int iterations = 0;
        int maxAttempts = 500;
        while (!found && iterations < maxAttempts) {
            iterations++;
            Foothold fh = getRandomWalkableFoothold();
            Position tempPos = fh.getRandomPositionFromEdges(edgeDistance);
            if (canSpawnOnLife) {
                found = true;
                pos = tempPos;
            } else {
                Rect rect = new Rect(
                        new Position(
                                tempPos.getX() - 75,
                                tempPos.getY() - 75),
                        new Position(
                                tempPos.getX() + 75,
                                tempPos.getY() + 75)
                );
                List<Life> lifes = getLifesInRect(rect);
                boolean hasBlockingLife = false;
                for (Life life : lifes) {
                    if ((life instanceof Reactor)) {
                        hasBlockingLife = true;
                        break;
                    }
                }
                if (!hasBlockingLife) {
                    found = true;
                    pos = tempPos;
                }
                if (!canSpawnOnPortals) {
                    List<Portal> portals = getClosestPortal(rect);
                    if (portals.isEmpty()) {
                        found = true;
                        pos = tempPos;
                    } else {
                        found = false;
                        pos = null;
                    }
                }
            }
        }
        return pos;
    }

    public int getBossMobID() {
        return bossMobID;
    }

    public void setBossMobID(int bossMobID) {
        this.bossMobID = bossMobID;
    }

    public Portal getDefaultPortal() {
        Portal p = getPortalByName("sp");
        return p == null ? getPortalByID(0) : p;
    }

    private ScriptManager getScriptManager() {
        return scriptManagerImpl;
    }

    public void handleCustomField(Mob mob, Char chr) {
        if (mob == null) {
            return;
        }
        if (chr == null) {
            chr = getCharByID(mob.getControllerID());
        }
        if (getId() == 993162500
                || getId() == 993162600
                || getId() == 993162700
                || getId() == 993162900
                || getId() == 993163000
                || getId() == 993163100) {
            int level = chr == null ? 140 : chr.getLevel();
            final long varifiedHP = 100000L * level;
            mob.setLevel(Math.min(275, level + 10));
            mob.setHp(varifiedHP);
            mob.setMaxHp(varifiedHP);
            mob.setMp(Integer.MAX_VALUE);
            mob.setMaxMp(Integer.MAX_VALUE);
            ForcedMobStat fms = mob.getForcedMobStat();
            fms.setPad(5000);
            fms.setMad(5000);
            fms.setPdr(50);
            fms.setMdr(50);
            return;
        }
        if (chr != null && (chr.getLevel() >= 200 || chr.getInstance() == null)) {
            return;
        }
        if (getId() == GameConstants.FIRST_TIME_TOGETHER_STAGE_1 || getId() == GameConstants.FIRST_TIME_TOGETHER_STAGE_4) {
            initKerningPQ(mob, chr);
        } else if (getId() >= GameConstants.EVOLVING_LINK_MAP_1 && getId() <= GameConstants.EVOLVING_LINK_MAP_9) {
            initEvolPQ(mob, chr);
        } else if (getId() == 933001000 || getId() == 910010000 || getId() == 910010001) {
            initHenesysPQ(mob, chr);
        }
    }

    public void initHenesysPQ(Mob mob, Char chr) {
        int avgLevel = 100;
        if (chr != null) {
            avgLevel = chr.getParty().getAvgPartyLevel();
        }
        final long mobExp = (long) ((GameConstants.charExp[avgLevel] * 0.001D) / 100.0D);
        final long varifiedHP = 50L * avgLevel;
        modifyMobs(mob, avgLevel, varifiedHP, avgLevel * 2, avgLevel * 2, mobExp);
    }

    public void initKerningPQ(Mob mob, Char chr) {
        int avgLevel = 100;
        if (chr != null) {
            avgLevel = chr.getParty().getAvgPartyLevel();
        }
        final long mobExp = (long) ((GameConstants.charExp[avgLevel] * 0.002D) / 100.0D);
        final long varifiedHP = 100L * avgLevel;
        modifyMobs(mob, avgLevel, varifiedHP, avgLevel * 2, avgLevel * 2, mobExp);
    }

    public void initEvolPQ(Mob mob, Char chr) {
        if (chr == null) {
            return;
        }
        int mobLv = 0;
        int mobHPRate = 0;
        int mobRate;
        Core mobLvCore = chr.getEquippedCores().stream().filter(c -> c.getCoreID() >= 3600000 && c.getCoreID() <= 3600005)
                .findAny().orElse(null);
        if (mobLvCore != null) {
            mobLv = GameConstants.getCoreValue(mobLvCore.getCoreID());
        }
        Core mobHpRateCore = chr.getEquippedCores().stream().filter(c -> c.getCoreID() >= 3600100 && c.getCoreID() <= 3600104)
                .findAny().orElse(null);
        if (mobHpRateCore != null) {
            mobHPRate = GameConstants.getCoreValue(mobHpRateCore.getCoreID());
        }
        Core mobRateCore = chr.getEquippedCores().stream().filter(c -> c.getCoreID() >= 3600200 && c.getCoreID() <= 3600204)
                .findAny().orElse(null);
        if (mobRateCore != null) {
            mobRate = GameConstants.getCoreValue(mobRateCore.getCoreID());
            setFixedMobCapacity((int) (getFixedMobCapacity() * (100 + mobRate) / 100.0D));
        }
        final long mobExp = (long) ((GameConstants.charExp[chr.getLevel()] * 0.015D) / 100.0D);
        final long varifiedHP = (long) ((100000L * chr.getLevel()) * (100 + mobHPRate) / 100.0D);
        modifyMobs(mob, chr.getLevel() + mobLv, varifiedHP, chr.getLevel() * 10, chr.getLevel() * 10, mobExp);
    }

    public void initRomeoJulietPQ() {
        for (Mob m : getMobs()) {
            if (m.getTemplateId() == 9300150) {
                removeLife(m);
            }
        }
        for (MobGen mg : getMobGens()) {
            if (mg.getMob().getTemplateId() == 9300150) {
                removeLife(mg);
            }
        }
        Npc npc = NpcData.getNpcDeepCopyById(getId() == 926100401 ? 2112000 : 2112010);
        Position position = new Position(250, 150);
        npc.setPosition(position);
        npc.setCy(150);
        npc.setRx0(300);
        npc.setRx1(200);
        npc.setFh(findFootHoldBelow(new Position(250, 148)).getId());
        npc.setNotRespawnable(true);
        addLife(npc);
    }

    /**
     * Goes through all MobGens, and spawns a Mob from it if allowed to do so. Only generates when there are Chars
     * on this Field, or if the field is being initialized.
     *
     * @param init if this is the first time that this method is called.
     */
    public void generateMobs(boolean init) {
        if (init || !getChars().isEmpty()) {
            List<MobGen> mobGenList;
            Set<MobGen> allMobGens = getMobGens();
            List<MobGen> nonBossMobGens = allMobGens.stream()
                    .filter(mobGen -> mobGen != null && MobData.getMobById(mobGen.getTemplateId()) != null && !MobData.getMobById(mobGen.getTemplateId()).isBoss())
                    .collect(Collectors.toList());
            // 4. Handle bonus mob spawns
            if (getBonusMobWaves() > 0) {
                mobGenList = new CopyOnWriteArrayList<>(allMobGens);
                for (int i = 0; i < getBonusMobWaves(); i++) {
                    for (int j = 0; j < 5; j++) {
                        MobGen bonusGen = Util.getRandomFromCollection(nonBossMobGens);
                        if (bonusGen != null) {
                            mobGenList.add(bonusGen);
                        }
                    }
                }
            } else {
                mobGenList = new CopyOnWriteArrayList<>(allMobGens);
            }
            int currentMobs = getMobs().size();
            int mobCapacity = getMobCapacity();
            boolean hasMobCapacityLimit = (getFieldLimit() & FieldOption.NoMobCapacityLimit.getVal()) == 0;

            // 5. Spawn mobs from the prepared list
            for (MobGen mobGen : mobGenList) {
                if (mobGen != null && mobGen.canSpawnOnField(this)) {
                    mobGen.spawnMob(this);
                    currentMobs++;

                    if (hasMobCapacityLimit && currentMobs > mobCapacity) {
                        break;
                    }
                }
            }
        }
        getTimer().addEvent(() -> generateMobs(false), getRespawnRate(), TimeUnit.MILLISECONDS);
    }

    public int getBonusMobWaves() {
        if (hasKishin() || (getTotem() != null && getTotem().getLeft())) {
            return 5;
        }
        return 0;
    }

    public long getRespawnRate() {
        //Giảm một nữa thời gian triệu hồi lần kết tiếp.
        double cap = 1;
        if (hasKishin()) {
            cap = 2;
        }
        if (getTotem() != null) {
            if (getTotem().getLeft()) {
                cap = 2;
                Tms282Monolith.Rates monolith = Tms282Monolith.rates(tms282MonolithTier);
                if (monolith != null) {
                    cap = hasKishin() ? Math.max(2, monolith.respawnDivisor()) : monolith.respawnDivisor();
                }
            }
        }
        return (long) (FieldConstants.BASE_MOB_RESPAWN_RATE / (getMobRate() * cap));
    }

    public int getMobCapacity() {
        double cap = 1;
        if (hasKishin()) {
            cap = FieldConstants.KISHIN_MOB_MULTIPLIER;
        }
        if (getTotem() != null) {
            //Is Active
            if (getTotem().getLeft()) {
                cap = getTotem().getRight() == 0 ? FieldConstants.TOTEM_MOB_MULTIPLIER_NORMAL : FieldConstants.TOTEM_MOB_MULTIPLIER_VIP;
                Tms282Monolith.Rates monolith = Tms282Monolith.rates(tms282MonolithTier);
                if (monolith != null) {
                    cap = hasKishin() ? Math.max(FieldConstants.KISHIN_MOB_MULTIPLIER, monolith.capacityMultiplier())
                            : monolith.capacityMultiplier();
                }
            }
        }
        return (int) (getFixedMobCapacity() * cap);
    }

    public boolean hasKishin() {
        return kishin;
    }

    public void setKishin(boolean kishin) {
        this.kishin = kishin;
    }

    public Tuple<Boolean, Integer> getTotem() {
        return totem;
    }

    private final Tms282BurningField tms282Burning = new Tms282BurningField();

    public Tms282BurningField getTms282Burning() { return tms282Burning; }

    public void setTotem(Tuple<Boolean, Integer> totem) {
        this.totem = totem;
    }

    /** 0 = no 輪迴碑石 on this field; otherwise the highest active tier (TMS 282 only). */
    public int getTms282MonolithTier() {
        return tms282MonolithTier;
    }

    public void setTms282MonolithTier(int tier) {
        this.tms282MonolithTier = Math.max(0, tier);
    }

    public List<OpenGate> getOpenGates() {
        return openGateList;
    }

    public void setOpenGates(List<OpenGate> openGateList) {
        this.openGateList = openGateList;
    }

    public void addOpenGate(OpenGate openGate) {
        getOpenGates().add(openGate);
    }

    public void removeOpenGate(OpenGate openGate) {
        getOpenGates().removeIf(x -> x.equals(openGate));
    }

    public boolean isChannelField() {
        return isChannelField;
    }

    public void setChannelField(boolean channelField) {
        this.isChannelField = channelField;
    }

    public List<TownPortal> getTownPortalList() {
        return townPortalList;
    }

    public void setTownPortalList(List<TownPortal> townPortalList) {
        this.townPortalList = townPortalList;
    }

    public void addTownPortal(TownPortal townPortal) {
        getTownPortalList().add(townPortal);
    }

    public void removeTownPortal(TownPortal townPortal) {
        getTownPortalList().removeIf(x -> x.equals(townPortal));
    }

    public TownPortal getTownPortalByChrId(int chrId) {
        return getTownPortalList().stream().filter(tp -> tp.getChr().getId() == chrId).findAny().orElse(null);
    }

    public void increaseReactorState(int templateId, int stateLength) {
        Life life = getLifeByTemplateId(templateId);
        if (life instanceof Reactor reactor) {
            reactor.increaseState();
            broadcast(ReactorPool.reactorChangeState(reactor, (short) 0, (byte) stateLength));
        }
    }

    public void removeReactorByID(int objectId) {
        removeReactor(objectId);
    }

    public void removeReactorByTemplateId(int templateId) {
        Life life = getLifeByTemplateId(templateId);
        if (life instanceof Reactor reactor) {
            removeReactor(reactor.getObjectId());
        }
    }

    public void removeReactor(int objectId) {
        Life life = getLifeByObjectID(objectId);
        if (!(life instanceof Reactor)) {
            return;
        }
        broadcast(ReactorPool.reactorRemove(objectId));
        removeLife(objectId, false, false);
    }

    public Int2ObjectOpenHashMap<List<String>> getDirectionInfo() {
        return directionInfo;
    }

    public void setDirectionInfo(Int2ObjectOpenHashMap<List<String>> directionInfo) {
        this.directionInfo = directionInfo;
    }

    public List<String> getDirectionNode(int node) {
        return directionInfo.getOrDefault(node, null);
    }

    public void addDirectionInfo(int node, List<String> scripts) {
        directionInfo.put(node, scripts);
    }

    public Clock getClock() {
        return clock;
    }

    public void setClock(Clock clock) {
        this.clock = clock;
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
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

    public int calcHighestFhYValue() {
        int max = Integer.MIN_VALUE;
        for (Foothold fh : getFootholds()) {
            if (fh.getY1() > max) {
                max = fh.getY1();
            }
            if (fh.getY2() > max) {
                max = fh.getY2();
            }
        }
        return max;
    }

    public List<Foothold> getFootholdsByGroupId(int groupId) {
        return getFootholds().stream().filter(fh -> fh.getGroupId() == groupId).collect(Collectors.toList());
    }

    public Set<Foothold> getFootholdsInRect(Rect rect) {
        Set<Foothold> fields = new HashSet<>();
        for (Foothold fh : getFootholds()) {
            if (rect.hasPositionInside(new Position(fh.getX1(), fh.getY2()))
                    || rect.hasPositionInside(new Position(fh.getX2(), fh.getY2()))) {
                fields.add(fh);
            }
        }
        return fields;
    }

    public Mob spawnMobRespawnable(int id, int x, int y, boolean respawnable, long hp, int respawnTime) {
        Mob mob = spawnMob(id, x, y, respawnable, hp);
        getTimer().addEvent(() -> spawnMobRespawnable(id, x, y, respawnable, hp, respawnTime), respawnTime * 1000L); //milliseconds to seconds.
        return mob;
    }

    public void startFieldEffect(BlowWeather bw, int duration) {
        if (fieldEffectTimer != null && !fieldEffectTimer.isDone()) {
            return;
        }
        setBw(bw);
        broadcast(FieldPacket.blowWeather(bw.getItemID(), bw.getMsg(), 7000, null));
        fieldEffectTimer = getTimer().addEvent(this::stopFieldEffect, duration, TimeUnit.SECONDS);
    }

    public void stopFieldEffect() {
        fieldEffectTimer.cancel(true);
        fieldEffectTimer = null;
        setBw(null);
        broadcast(FieldPacket.removeBlowWeather());
    }

    public ScheduledFuture<?> getFieldEffectTimer() {
        return fieldEffectTimer;
    }

    public void setFieldEffectTimer(ScheduledFuture<?> fieldEffectTimer) {
        this.fieldEffectTimer = fieldEffectTimer;
    }

    public int getMonsterGauge() {
        return monsterGauge;
    }

    public void setMonsterGauge(int monsterGauge) {
        this.monsterGauge = monsterGauge;
    }

    public void incMonsterGauge(int amount) {
        this.monsterGauge += amount;
    }

    public void decMonsterGauge(int amount) {
        this.monsterGauge -= amount;
        if (monsterGauge < 0) {
            this.monsterGauge = 0;
        }
    }

    public int getZakumStand() {
        return zakumStand;
    }

    public void setZakumStand(int zakumStand) {
        this.zakumStand = zakumStand;
    }

    public void createZakumStand() {
        List<DynamicFootHood> dynamicFootHoods = new LinkedList<>();
        dynamicFootHoods.add(new DynamicFootHood("zdc1", (byte) 0, getZakumStand(), new Position(-464, -186)));
        dynamicFootHoods.add(new DynamicFootHood("zdc10", (byte) 0, getZakumStand(), new Position(350, -189)));
        dynamicFootHoods.add(new DynamicFootHood("zdc12", (byte) 0, getZakumStand(), new Position(504, -187)));
        dynamicFootHoods.add(new DynamicFootHood("zdc13", (byte) 0, getZakumStand(), new Position(384, -99)));
        dynamicFootHoods.add(new DynamicFootHood("zdc14", (byte) 0, getZakumStand(), new Position(464, -102)));
        dynamicFootHoods.add(new DynamicFootHood("zdc15", (byte) 0, getZakumStand(), new Position(546, -101)));
        dynamicFootHoods.add(new DynamicFootHood("zdc16", (byte) 0, getZakumStand(), new Position(363, -7)));
        dynamicFootHoods.add(new DynamicFootHood("zdc17", (byte) 0, getZakumStand(), new Position(439, -5)));
        dynamicFootHoods.add(new DynamicFootHood("zdc18", (byte) 0, getZakumStand(), new Position(517, -8)));
        dynamicFootHoods.add(new DynamicFootHood("zdc2", (byte) 0, getZakumStand(), new Position(-388, -187)));
        dynamicFootHoods.add(new DynamicFootHood("zdc3", (byte) 0, getZakumStand(), new Position(-310, -184)));
        dynamicFootHoods.add(new DynamicFootHood("zdc4", (byte) 0, getZakumStand(), new Position(-514, -102)));
        dynamicFootHoods.add(new DynamicFootHood("zdc5", (byte) 0, getZakumStand(), new Position(-439, -101)));
        dynamicFootHoods.add(new DynamicFootHood("zdc6", (byte) 0, getZakumStand(), new Position(-362, -99)));
        dynamicFootHoods.add(new DynamicFootHood("zdc7", (byte) 0, getZakumStand(), new Position(-512, -7)));
        dynamicFootHoods.add(new DynamicFootHood("zdc8", (byte) 0, getZakumStand(), new Position(-436, -5)));
        dynamicFootHoods.add(new DynamicFootHood("zdc9", (byte) 0, getZakumStand(), new Position(-358, -8)));
        broadcast(FieldPacket.syncDynamicFootHold(dynamicFootHoods));
    }

    public boolean isDefaultClock() {
        return defaultClock;
    }

    public void setDefaultClock(boolean defaultClock) {
        this.defaultClock = defaultClock;
    }

    public class DynamicFootHood {
        public String name;
        public byte unk;
        public int visible;
        public Position pos;

        public DynamicFootHood(String name, byte unk, int visible, Position pos) {
            this.name = name;
            this.unk = unk;
            this.visible = visible;
            this.pos = pos;
        }
    }

    public LucidState getLucidState() {
        return lucidState;
    }

    public void setLucidState(LucidState lucidState) {
        this.lucidState = lucidState;
        if (lucidState == LucidState.None) {
            broadcast(LucidPacket.welcomeBarrage(1));
            broadcast(LucidPacket.stainedGlassOnOff(true, BossConstants.STAINED_GLASS));
            broadcast(LucidPacket.setFlyingMode(false));
        }
    }

    public int getLucidStatueGauge() {
        return lucidStatueGauge;
    }

    public void setLucidStatueGauge(int lucidStatueGauge) {
        this.lucidStatueGauge = lucidStatueGauge;
    }

    public int getButterFlyCount() {
        return butterFlyCount;
    }

    public void setButterFlyCount(int butterFlyCount) {
        this.butterFlyCount = butterFlyCount;
    }

    public ScheduledFuture<?> getSpawnButterflyTimer() {
        return spawnButterflyTimer;
    }

    public void setSpawnButterflyTimer(ScheduledFuture<?> spawnButterflyTimer) {
        this.spawnButterflyTimer = spawnButterflyTimer;
    }

    public ScheduledFuture<?> getDestroyButterflyTimer() {
        return destroyButterflyTimer;
    }

    public void setDestroyButterflyTimer(ScheduledFuture<?> destroyButterflyTimer) {
        this.destroyButterflyTimer = destroyButterflyTimer;
    }

    public BlowWeather getBw() {
        return bw;
    }

    public void setBw(BlowWeather bw) {
        this.bw = bw;
    }

    public boolean isGollux_head() {
        return gollux_head;
    }

    public void setGollux_head(boolean gollux_head) {
        this.gollux_head = gollux_head;
    }

    public boolean isGollux_LArm() {
        return gollux_LArm;
    }

    public void setGollux_LArm(boolean gollux_LArm) {
        this.gollux_LArm = gollux_LArm;
    }

    public boolean isGollux_RArm() {
        return gollux_RArm;
    }

    public void setGollux_RArm(boolean gollux_RArm) {
        this.gollux_RArm = gollux_RArm;
    }

    public boolean isGollux_Hip() {
        return gollux_Hip;
    }

    public void setGollux_Hip(boolean gollux_Hip) {
        this.gollux_Hip = gollux_Hip;
    }

    public boolean isRewardDropped() {
        return rewardDropped;
    }

    public void setRewardDropped(boolean rewardDropped) {
        this.rewardDropped = rewardDropped;
    }

    public boolean isChaosPinkBeanSpawned() {
        return chaosPinkBeanSpawned;
    }

    public void setChaosPinkBeanSpawned(boolean chaosPinkBeanSpawned) {
        this.chaosPinkBeanSpawned = chaosPinkBeanSpawned;
    }

    public boolean isBossSpawned() {
        return isBossSpawned;
    }

    public void setBossSpawned(boolean bossSpawned) {
        this.isBossSpawned = bossSpawned;
    }

    public int getBarrier() {
        return barrier;
    }

    public void setBarrier(int barrier) {
        this.barrier = barrier;
    }

    public int getBarrierArc() {
        return barrierArc;
    }

    public void setBarrierArc(int barrierArc) {
        this.barrierArc = barrierArc;
    }

    public void setReviveCurFieldOfNoTransferPoint(Position reviveCurFieldOfNoTransferPoint) {
        this.reviveCurFieldOfNoTransferPoint = reviveCurFieldOfNoTransferPoint;
    }

    public void setReviveCurField(boolean reviveCurField) {
        this.reviveCurField = reviveCurField;
    }

    public boolean isReviveCurField() {
        return reviveCurField;
    }

    public void setReviveCurFieldOfNoTransfer(boolean reviveCurFieldOfNoTransfer) {
        this.reviveCurFieldOfNoTransfer = reviveCurFieldOfNoTransfer;
    }

    public boolean isReviveCurFieldOfNoTransfer() {
        return reviveCurFieldOfNoTransfer;
    }

    public Position getReviveCurFieldOfNoTransferPoint() {
        return reviveCurFieldOfNoTransferPoint;
    }

    public boolean isLucidRewardSpawned() {
        return lucidRewardSpawned;
    }

    public void setLucidRewardSpawned(boolean lucidRewardSpawned) {
        this.lucidRewardSpawned = lucidRewardSpawned;
    }

    public void shutdownField() {
        boolean hasBoss = !getChars().isEmpty();
        for (Mob m : getMobs()) {
            if (m.isBoss()) {
                hasBoss = true;
                break;
            }
        }
        if (hasBoss) {
            return;
        }
        this.lifes.clear();
        for (ScheduledFuture<?> sf : lifeSchedules.values()) {
            if (sf != null) {
                sf.cancel(true);
            }
        }
        this.lifeSchedules.clear();
        this.objectIDCounter = 1000000;
        this.runeStone = null;
        if (this.runeStoneHordesTimer != null && !this.runeStoneHordesTimer.isDone()) {
            this.runeStoneHordesTimer.cancel(true);
        }
        this.runeStoneHordesTimer = null;
        this.nextEliteSpawnTime = System.currentTimeMillis();
        this.kishin = false;
        this.totem = new Tuple<>(false, 0);
        this.openGateList = new ArrayList<>();
        this.townPortalList = new ArrayList<>();
        this.properties.clear();
        if (this.fieldEffectTimer != null) {
            this.fieldEffectTimer.cancel(true);
        }
        this.fieldEffectTimer = null;
        this.bw = null;
        this.monsterGauge = 0;
        if (this.spawnButterflyTimer != null) {
            this.spawnButterflyTimer.cancel(true);
        }
        this.spawnButterflyTimer = null;
        if (this.destroyButterflyTimer != null) {
            this.destroyButterflyTimer.cancel(true);
        }
        this.destroyButterflyTimer = null;
    }

    public long getDeprecationStartTime() {
        return deprecationStartTime;
    }

    public void setDeprecationStartTime(long deprecationStartTime) {
        this.deprecationStartTime = deprecationStartTime;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
    }

    public static class BlowWeather {
        private boolean running;
        private int itemID;
        private String msg;

        public BlowWeather() {
        }

        public BlowWeather(boolean running, int itemID, String msg) {
            this.running = running;
            this.itemID = itemID;
            this.msg = msg;
        }

        public boolean isRunning() {
            return running;
        }

        public void setRunning(boolean running) {
            this.running = running;
        }

        public int getItemID() {
            return itemID;
        }

        public void setItemID(int itemID) {
            this.itemID = itemID;
        }

        public String getMsg() {
            return msg;
        }

        public void setMsg(String msg) {
            this.msg = msg;
        }

    }

    public void modifyMobs(Mob mob, int level, long hp, int pad, int mad, long exp) {
        mob.setLevel(level);
        mob.setHp(hp);
        mob.setMaxHp(hp);
        mob.setMp(Integer.MAX_VALUE);
        mob.setMaxMp(Integer.MAX_VALUE);
        mob.setPad(pad);
        mob.setMad(mad);
        mob.getForcedMobStat().setPdr(20);
        mob.getForcedMobStat().setMdr(20);
        mob.getForcedMobStat().setExp(exp);
    }

    public void messageByMob(Mob mob, int action) {
        if (mob.getTemplateId() == 8880000 || mob.getTemplateId() == 8880002 || mob.getTemplateId() == 8880010) {
            if (action == 30 || action == 31) {
                broadcast(FieldPacket.smartMobNotice(mob.getTemplateId(), 0, 5, 1, "梅格耐斯在狹小的區域感到威脅，試圖逃脫。"));
            } else if (action == 27 || action == 26) {
                broadcast(FieldPacket.smartMobNotice(mob.getTemplateId(), 0, 5, 1, "梅格耐斯試圖甩開附近的敵人。"));
            } else if (action == 34 || action == 35) {
                broadcast(FieldPacket.smartMobNotice(mob.getTemplateId(), 0, 5, 1, "梅格耐斯發動連續攻擊，對付剩下的敵人。"));
            } else if (action == 60) {
                broadcast(FieldPacket.smartMobNotice(mob.getTemplateId(), 0, 5, 1, "梅格耐斯準備對行動變慢的敵人施展強力一擊。"));
            } else if (action == 62) {
                broadcast(FieldPacket.smartMobNotice(mob.getTemplateId(), 0, 5, 1, "梅格耐斯看到敵人恢復／強化後，開始準備反制手段。"));
            }
        } else if (mob.getTemplateId() == 8800022) {
            if (action == 30 || action == 31) {
                //broadcast(FieldPacket.smartMobNotice(mob.getTemplateId(), 0, 1, 4, "Zakum is gathering his strength to destroy everything!"));
            }
        } else if (mob.getTemplateId() == 8870000 || mob.getTemplateId() == 8870100) {
            if (action != -1) {
                if (mob.getTemplateId() == 8870100) {
                    if (action == 76 || action == 77) {
                        broadcast(MobPool.speaking(mob, 3, 0));
                    } else if (action == 36 || action == 37) {
                        broadcast(MobPool.speaking(mob, 1, 0));
                    }
                } else if (action == 30 || action == 31) {
                    broadcast(MobPool.speaking(mob, 0, 0));
                } else if (action == 34 || action == 35) {
                    broadcast(MobPool.speaking(mob, 1, 0));
                }
            }
        } else if (mob.getTemplateId() == 8500001 || mob.getTemplateId() == 8500002 || mob.getTemplateId() == 8500011
                || mob.getTemplateId() == 8500012 || mob.getTemplateId() == 8500021 || mob.getTemplateId() == 8500022) {
            boolean phase1 = !(mob.getTemplateId() == 8500002 || mob.getTemplateId() == 8500012 || mob.getTemplateId() == 8500022);
            if (phase1) {
                if (action == 68 || action == 69) {
                    broadcast(MobPool.speaking(mob, 2, 0));
                }
            } else if (action == 66 || action == 67) {
                broadcast(MobPool.speaking(mob, 1, 0));
            }
        }
    }

    public void clear() {
        removeDrops();
        removeMobs();
    }

    public void removeDrops() {
        List<Drop> drops = new ArrayList<>(getDrops());
        for (Drop drop : drops) {
            if (drop.getOwnerID() == 0) {
                removeDrop(drop.getObjectId(), 0, false, -1);
            }
        }
    }

    public void removeMobs() {
        for (Mob mob : getMobs()) {
            if (!mob.isBoss() || mob.getHp() == mob.getMaxHp()) {
                if (mob.getEliteType() == EliteState.None.getVal() && mob.getTemplateId() != 9600063) {
                    removeMob(mob.getObjectId(), DeathType.NO_ANIMATION_DEATH);
                }
            }
        }
    }

    public void removeMobsBySvr(Char chr) {
        final var charID = chr.getId();
        final var jobID = chr.getJob();
        for (Mob mob : getMobs()) {
            broadcast(MobPool.leaveFieldTargetFromSvr(mob.getObjectId(), charID, jobID));
            removeLife(mob.getObjectId(), true, false);
        }
    }

    public int getCandles() {
        return candles;
    }

    public void setCandles(int candles) {
        this.candles = candles;
    }

    public int getLightCandles() {
        return lightCandles;
    }

    public void setLightCandles(int lightCandles) {
        this.lightCandles = lightCandles;
    }

    public int getReqTouched() {
        return reqTouched;
    }

    public void setReqTouched(int reqTouched) {
        this.reqTouched = reqTouched;
    }

    public long getSandGlassTime() {
        return sandGlassTime;
    }

    public void setSandGlassTime(long sandGlassTime) {
        this.sandGlassTime = sandGlassTime;
    }

    // Timer && Thread Zone
    public Timer getTimer() {
        return Server.get().getFieldTimer();
    }

    public int getBoosterMobID() {
        return boosterMobID;
    }

    public void setBoosterMobID(int boosterMobID) {
        this.boosterMobID = boosterMobID;
    }

    public int getBoosterEXPMulti() {
        return boosterEXPMulti;
    }

    public void setBoosterEXPMulti(int boosterEXPMulti) {
        this.boosterEXPMulti = boosterEXPMulti;
    }
}
