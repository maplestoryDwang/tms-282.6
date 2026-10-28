package net.swordie.ms.client.character.skills.info;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.SkillStat;
import net.swordie.ms.client.jobs.adventurer.magician.Bishop;
import net.swordie.ms.client.jobs.adventurer.warrior.Paladin;
import net.swordie.ms.client.jobs.cygnus.Mihile;
import net.swordie.ms.client.jobs.legend.Luminous;
import net.swordie.ms.client.jobs.resistance.demon.Demon;
import net.swordie.ms.client.jobs.resistance.demon.DemonSlayer;
import net.swordie.ms.client.jobs.sengoku.Kanna;
import net.swordie.ms.client.social.Guild.GuildSkill;
import net.swordie.ms.constants.GuildConstants;
import net.swordie.ms.enums.BaseStat;
import net.swordie.ms.enums.Stat;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.util.Position;
import net.swordie.ms.util.Rect;
import net.swordie.ms.util.container.Tuple;

import java.util.*;

public class SkillInfo {

    private int skillId;
    private int rootId;
    private int maxLevel;
    private int currentLevel;
    private boolean invisible;
    private int masterLevel;
    private int fixLevel;
    // WZ metadata only; deliberately not appended to the existing DAT format.
    private int reqLevel;
    private List<Rect> rects = new ArrayList<>();
    private boolean massSpell;
    private int type;
    private Set<Integer> psdSkills = new HashSet<>();
    private String elemAttr = "";
    private int hyper;
    private int vSkill;
    private int hyperstat;
    private int vehicleId;
    private int reqTierPoint;
    private final Map<Integer, Integer> reqSkills = new HashMap<>();
    private boolean notCooltimeReset;
    private boolean notIncBuffDuration;
    private boolean isOriginSkill;
    private boolean isAscentSkill;
    private boolean psd;
    private Set<Integer> addAttackSkills = new HashSet<>();
    private Map<Integer, ExtraSkillInfo> extraSkillInfo = new HashMap<>();
    private Map<Map<Integer, Integer>, Integer> randomSkills = new HashMap<>();
    private boolean ignoreCounter;
    private boolean petPassive;
    private boolean isSequenceOn;
    private int weapon;
    private int setItemPartsCount;
    private int setItemReason;
    private int boostDamR;
    private boolean isFinalAttack;
    private List<Integer> finalAttacks = new ArrayList<>();
    private List<Integer> skillList1 = new ArrayList<>();
    private List<Integer> skillList2 = new ArrayList<>();
    private final EnumMap<SkillStat, String> skillStatInfo = new EnumMap<>(SkillStat.class);
    private final Int2ObjectMap<EnumMap<SkillStat, Integer>> cachedData = new Int2ObjectOpenHashMap<>(); // <Level, <SkillStat, Value>>
    private int processType = 0;
    private List<Integer> additionalProcess = new ArrayList<>();
    private Int2ObjectMap<SecondAtomInfo> secondAtomInfos = new Int2ObjectOpenHashMap<>();
    private boolean shootObject;

    public int getSkillId() {
        return skillId;
    }

    public int getReqLevel() {
        return reqLevel;
    }

    public void setReqLevel(int reqLevel) {
        this.reqLevel = reqLevel;
    }

    public void setSkillId(int skillId) {
        this.skillId = skillId;
    }

    public int getRootId() {
        return rootId;
    }

    public void setRootId(int rootId) {
        this.rootId = rootId;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public void setMaxLevel(int maxLevel) {
        this.maxLevel = maxLevel;
    }

    public int getCurrentLevel() {
        return currentLevel;
    }

    public void setCurrentLevel(int currentLevel) {
        this.currentLevel = currentLevel;
    }

    public EnumMap<SkillStat, String> getSkillStatInfo() {
        return skillStatInfo;
    }

    public void addSkillStatInfo(SkillStat sc, String value) {
        skillStatInfo.put(sc, value);
    }

    public void load() {
        for (SkillStat skillStat : skillStatInfo.keySet()) {
            for (int i = 1; i <= getMaxLevel(); i++) {
                getValue(skillStat, i);
            }
        }
    }

    public void clearCalculatedCache() {
        cachedData.clear();
    }

    public int getValue(SkillStat skillStat, int slv) {
        if (slv <= 0) {
            return 0;
        }

        // cache theo level
        EnumMap<SkillStat, Integer> lvCache = cachedData.get(slv);
        if (lvCache != null) {
            Integer cached = lvCache.get(skillStat);
            if (cached != null) {
                return cached;
            }
        }

        String expr = skillStatInfo.get(skillStat);
        if (expr == null || expr.isEmpty()) {
            return 0;
        }

        int result = SkillFormulaEvaluator.eval(expr, slv);

        if (lvCache == null) {
            lvCache = new EnumMap<>(SkillStat.class);
            cachedData.put(slv, lvCache);
        }
        lvCache.put(skillStat, result);
        return result;
    }

    public boolean isInvisible() {
        return invisible;
    }

    public void setInvisible(boolean invisible) {
        this.invisible = invisible;
    }

    public int getMasterLevel() {
        return masterLevel;
    }

    public void setMasterLevel(int masterLevel) {
        this.masterLevel = masterLevel;
    }

    public boolean isFixDamageSkill() {
        String value = skillStatInfo.get(SkillStat.fixdamage);
        return value != null;
    }

    public int getFixLevel() {
        return fixLevel;
    }

    public void setFixLevel(int fixLevel) {
        this.fixLevel = fixLevel;
    }

    public void addRect(Rect rect) {
        getRects().add(rect);
    }

    public List<Rect> getRects() {
        return rects;
    }

    public void setRects(List<Rect> rects) {
        this.rects = rects;
    }

    public Rect getLastRect() {
        return rects != null && rects.size() > 0 ? rects.get(rects.size() - 1) : null;
    }

    public Rect getFirstRect() {
        return rects != null && rects.size() > 0 ? rects.get(0) : null;
    }

    public boolean isMassSpell() {
        switch (getSkillId()) {
            case Bishop.BLESSED_ENSEMBLE:
            case Bishop.HOLY_SYMBOL:
            case Bishop.ANGEL_RAY:
            case Bishop.HEXA_ANGEL_RAY:
            case Luminous.RAY_OF_REDEMPTION: // Recover 100% HP
            case DemonSlayer.LEECH_AURA:
            case Kanna.FALLING_SAKURA: // Recover 100% HP
            case Paladin.DIVINE_ECHO:
            case Paladin.HEXA_DIVINE_ECHO:
            case Mihile.SHIELD_OF_LIGHT:
            case Mihile.HEXA_SHIELD_OF_LIGHT:
                return true;
            default:
                return massSpell;
        }
    }

    public void setMassSpell(boolean massSpell) {
        this.massSpell = massSpell;
    }

    public boolean hasCooltime() {
        return getValue(SkillStat.cooltime, 1) > 0 || getValue(SkillStat.cooltimeMS, 1) > 0;
    }

    public Map<BaseStat, Integer> getBaseStatValues(Char chr, int slv) {
        Map<BaseStat, Integer> stats = new HashMap<>();
        for (SkillStat ss : getSkillStatInfo().keySet()) {
            Tuple<BaseStat, Integer> bs = getBaseStatValue(ss, slv, chr);
            stats.put(bs.getLeft(), bs.getRight());
        }
        if (net.swordie.ms.connection.protocol.Protocol.active().version() != net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) {
            Map<BaseStat, Integer> guildSkillStats = getBaseStatValuesFromGuildSkills(chr);
            if (!guildSkillStats.isEmpty()) stats.putAll(guildSkillStats);
        }
        return stats;
    }

    public Map<BaseStat, Integer> getBaseStatValuesFromGuildSkills(Char chr) {
        return getGuildBaseStatValues(chr);
    }

    public static Map<BaseStat, Integer> getGuildBaseStatValues(Char chr) {
        Map<BaseStat, Integer> stats = new HashMap<>();
        if (chr.getGuild() != null) {
            for (GuildSkill gs : chr.getGuild().getSkills().values()) {
                int skillID = gs.getSkillID();
                if (skillID == GuildConstants.GUILD_ON_FIRE_I
                        || skillID == GuildConstants.GUILD_ON_FIRE_II
                        || skillID == GuildConstants.GUILD_ON_FIRE_III) {
                    SkillInfo si = SkillData.getSkillInfoById(skillID);
                    int slv = gs.getLevel();
                    stats.put(BaseStat.pad, si.getValue(SkillStat.padX, slv));
                    stats.put(BaseStat.mad, si.getValue(SkillStat.madX, slv));
                } else if (skillID == GuildConstants.WELL_ROUNDED) {
                    SkillInfo si = SkillData.getSkillInfoById(skillID);
                    int slv = gs.getLevel();
                    stats.put(BaseStat.str, si.getValue(SkillStat.strX, slv));
                    stats.put(BaseStat.dex, si.getValue(SkillStat.dexX, slv));
                    stats.put(BaseStat.inte, si.getValue(SkillStat.intX, slv));
                    stats.put(BaseStat.luk, si.getValue(SkillStat.lukX, slv));
                    stats.put(BaseStat.mhp, si.getValue(SkillStat.mhpX, slv));
                }
            }
        }
        return stats;
    }

    private Tuple<BaseStat, Integer> getBaseStatValue(SkillStat ss, int slv, Char chr) {
        BaseStat bs = ss.getBaseStat();
        int value = getValue(ss, slv);
        switch (ss) {
            case lv2damX:
            case lv2pad:
            case lv2mad:
            case lv2str:
            case lv2dex:
            case lv2int:
            case lv2luk:
                value *= chr.getLevel();
                break;
            case str2dex:
                value *= chr.getStat(Stat.str);
                break;
            case dex2luk:
            case dex2str:
                value *= chr.getStat(Stat.dex);
                break;
            case int2luk:
                value *= chr.getStat(Stat.inte);
                break;
            case luk2dex:
            case luk2int:
                value *= chr.getStat(Stat.luk);
                break;
            case mhp2damX:
                value *= chr.getStat(Stat.mhp);
                break;
            case mmp2damX:
                value *= chr.getStat(Stat.mmp);
                break;
        }
        return new Tuple<>(bs, value);
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public void addPsdSkill(int skillID) {
        getPsdSkills().add(skillID);
    }

    public Set<Integer> getPsdSkills() {
        return psdSkills;
    }

    public void setPsdSkills(Set<Integer> psdSkills) {
        this.psdSkills = psdSkills;
    }

    public String getElemAttr() {
        return elemAttr;
    }

    public void setElemAttr(String elemAttr) {
        this.elemAttr = elemAttr;
    }

    public int getHyper() {
        return hyper;
    }

    public void setHyper(int hyper) {
        this.hyper = hyper;
    }

    public int getHyperStat() {
        return hyperstat;
    }

    public void setHyperStat(int hyperstat) {
        this.hyperstat = hyperstat;
    }

    public int getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(int vehicleId) {
        this.vehicleId = vehicleId;
    }

    public int getReqTierPoint() {
        return reqTierPoint;
    }

    public void setReqTierPoint(int reqTierPoint) {
        this.reqTierPoint = reqTierPoint;
    }

    public void addReqSkill(int skillID, int slv) {
        getReqSkills().put(skillID, slv);
    }

    public Map<Integer, Integer> getReqSkills() {
        return reqSkills;
    }

    public boolean isNotCooltimeReset() {
        return notCooltimeReset;
    }

    public void setNotCooltimeReset(boolean notCooltimeReset) {
        this.notCooltimeReset = notCooltimeReset;
    }

    public boolean isNotIncBuffDuration() {
        return notIncBuffDuration;
    }

    public void setNotIncBuffDuration(boolean notIncBuffDuration) {
        this.notIncBuffDuration = notIncBuffDuration;
    }

    public boolean isPsd() {
        return psd;
    }

    public void setPsd(boolean psd) {
        this.psd = psd;
    }

    public Set<Integer> getAddAttackSkills() {
        return addAttackSkills;
    }

    public void setAddAttackSkills(Set<Integer> addAttackSkills) {
        this.addAttackSkills = addAttackSkills;
    }

    public void addAddAttackSkills(int skillId) {
        getAddAttackSkills().add(skillId);
    }

    public Map<Integer, ExtraSkillInfo> getExtraSkillInfo() {
        return extraSkillInfo;
    }

    public boolean isIgnoreCounter() {
        return getValue(SkillStat.ignoreCounter, 1) != 0;
    }

    public boolean isPetPassive() {
        return petPassive;
    }

    public void setPetPassive(boolean petPassive) {
        this.petPassive = petPassive;
    }

    public boolean isSequenceOn() {
        return isSequenceOn;
    }

    public void setSequenceOn(boolean isSequenceOn) {
        this.isSequenceOn = isSequenceOn;
    }

    public int getWeapon() {
        return weapon;
    }

    public void setWeapon(int weapon) {
        this.weapon = weapon;
    }

    public int getSetItemPartsCount() {
        return setItemPartsCount;
    }

    public void setSetItemPartsCount(int setItemPartsCount) {
        this.setItemPartsCount = setItemPartsCount;
    }

    public int getSetItemReason() {
        return setItemReason;
    }

    public void setSetItemReason(int setItemReason) {
        this.setItemReason = setItemReason;
    }

    public Map<Map<Integer, Integer>, Integer> getRandomSkills() {
        return randomSkills;
    }

    public void setRandomSkills(Map<Map<Integer, Integer>, Integer> randomSkills) {
        this.randomSkills = randomSkills;
    }

    public void addRandomSkill(Map<Integer, Integer> skillList, int prob) {
        this.randomSkills.put(skillList, prob);
    }

    public int getBoostDamR() {
        return boostDamR;
    }

    public void setBoostDamR(int boostDamR) {
        this.boostDamR = boostDamR;
    }

    public int getVSkill() {
        return vSkill;
    }

    public void setVSkill(int vSkill) {
        this.vSkill = vSkill;
    }

    public boolean isFinalAttack() {
        return isFinalAttack;
    }

    public void setFinalAttack(boolean finalAttack) {
        this.isFinalAttack = finalAttack;
    }

    public List<Integer> getFinalAttacks() {
        return finalAttacks;
    }

    public void setFinalAttack(List<Integer> finalAttacks) {
        this.finalAttacks = finalAttacks;
    }

    public List<Integer> getSkillList1() {
        return skillList1;
    }

    public void setSkillList1(List<Integer> skillList1) {
        this.skillList1 = skillList1;
    }

    public List<Integer> getSkillList2() {
        return skillList2;
    }

    public void setSkillList2(List<Integer> skillList2) {
        this.skillList2 = skillList2;
    }

    public int getProcessType() {
        return processType;
    }

    public void setProcessType(int processType) {
        this.processType = processType;
    }

    public List<Integer> getAdditionalProcess() {
        return additionalProcess;
    }

    public void setAdditionalProcess(List<Integer> additionalProcess) {
        this.additionalProcess = additionalProcess;
    }

    public boolean findProcessType(int type) {
        return this.processType == type || this.additionalProcess.contains(type);
    }

    public boolean isAscentSkill() {
        return isAscentSkill;
    }

    public void setAscentSkill(boolean ascentSkill) {
        isAscentSkill = ascentSkill;
    }

    public boolean isOriginSkill() {
        return isOriginSkill;
    }

    public void setOriginSkill(boolean originSkill) {
        isOriginSkill = originSkill;
    }

    public Int2ObjectMap<SecondAtomInfo> getSecondAtomInfos() {
        return secondAtomInfos;
    }

    public void setSecondAtomInfos(Int2ObjectMap<SecondAtomInfo> secondAtomInfos) {
        this.secondAtomInfos = secondAtomInfos;
    }

    public boolean isShootObject() {
        return shootObject;
    }

    public void setShootObject(boolean shootObject) {
        this.shootObject = shootObject;
    }

    public static class SecondAtomInfo {
        private int createDelay;
        private int enableDelay;
        private int rotate;
        private int expire;
        private int attackableCount = 1;
        private int dataIndex = -1;
        private int firstAngleStart;
        private int firstAngleRange;
        private int notRotateEndEffect;
        private int posRandomOffset;
        private int followAngle;
        private int localOnly;
        private Position pos = new Position(0, 0);
        private Int2IntMap customs = new Int2IntOpenHashMap();
        private Int2ObjectMap<Position> extraPos = new Int2ObjectOpenHashMap<>();

        public SecondAtomInfo() {}

        public int getCreateDelay() {
            return createDelay;
        }

        public void setCreateDelay(int createDelay) {
            this.createDelay = createDelay;
        }

        public int getEnableDelay() {
            return enableDelay;
        }

        public void setEnableDelay(int enableDelay) {
            this.enableDelay = enableDelay;
        }

        public int getRotate() {
            return rotate;
        }

        public void setRotate(int rotate) {
            this.rotate = rotate;
        }

        public int getExpire() {
            return expire;
        }

        public void setExpire(int expire) {
            this.expire = expire;
        }

        public int getAttackableCount() {
            return attackableCount;
        }

        public void setAttackableCount(int attackableCount) {
            this.attackableCount = attackableCount;
        }

        public int getDataIndex() {
            return dataIndex;
        }

        public void setDataIndex(int dataIndex) {
            this.dataIndex = dataIndex;
        }

        public int getFirstAngleStart() {
            return firstAngleStart;
        }

        public void setFirstAngleStart(int firstAngleStart) {
            this.firstAngleStart = firstAngleStart;
        }

        public int getFirstAngleRange() {
            return firstAngleRange;
        }

        public void setFirstAngleRange(int firstAngleRange) {
            this.firstAngleRange = firstAngleRange;
        }

        public int getNotRotateEndEffect() {
            return notRotateEndEffect;
        }

        public void setNotRotateEndEffect(int notRotateEndEffect) {
            this.notRotateEndEffect = notRotateEndEffect;
        }

        public int getPosRandomOffset() {
            return posRandomOffset;
        }

        public void setPosRandomOffset(int posRandomOffset) {
            this.posRandomOffset = posRandomOffset;
        }

        public Position getPos() {
            return pos;
        }

        public void setPos(Position pos) {
            this.pos = pos;
        }

        public int getFollowAngle() {
            return followAngle;
        }

        public void setFollowAngle(int followAngle) {
            this.followAngle = followAngle;
        }

        public int getLocalOnly() {
            return localOnly;
        }

        public void setLocalOnly(int localOnly) {
            this.localOnly = localOnly;
        }

        public Int2IntMap getCustoms() {
            return customs;
        }

        public void setCustoms(Int2IntMap customs) {
            this.customs = customs;
        }

        public Int2ObjectMap<Position> getExtraPos() {
            return extraPos;
        }

        public void setExtraPos(Int2ObjectMap<Position> extraPos) {
            this.extraPos = extraPos;
        }
    }
}
