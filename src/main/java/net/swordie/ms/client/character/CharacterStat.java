package net.swordie.ms.client.character;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.constants.EventConstants;
import net.swordie.ms.constants.GameConstants;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.util.FileTime;
import net.swordie.ms.util.SystemTime;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class CharacterStat {

    private int id;
    private int characterId;
    private int characterIdForLog;
    private int worldIdForLog;
    private String name;
    private int gender;
    private int skin;
    private int face;
    private int hair;
    private int mixBaseHairColor = -1;
    private int mixAddHairColor = 0;
    private int mixHairBaseProb = 0;
    private int level;
    private int job;
    private int str;
    private int dex;
    private int inte;
    private int luk;
    private int hp;
    private int maxHp;
    private int mp;
    private int maxMp;
    private int ap;
    private int sp;
    private long exp;
    private int pop; // fame
    private long money;
    private int wp;
    private int node_shards;
    private int maxFriends;
    private ExtendSP extendSP;
    private long posMap;
    private int portal;
    private int subJob;
    private int defFaceAcc;
    private int fatigue;
    private int lastFatigueUpdateTime;
    private int charismaExp;
    private int insightExp;
    private int willExp;
    private int craftExp;
    private int senseExp;
    private int charmExp;
    private NonCombatStatDayLimit nonCombatStatDayLimit;
    private int mcpoint;
    private int pvpExp;
    private int pvpGrade;
    private int pvpPoint;
    private int pvpModeLevel;
    private int pvpModeType;
    private int eventPoint;
    private int albaActivityID;
    private FileTime albaStartTime;
    private int albaDuration;
    private int albaSpecialReward;
    private boolean burning;
    private SystemTime accountLastLogout;
    private FileTime lastLogout;
    private int gachExp;
    private int honorExp;
    private FileTime nextAvailableFameTime;
    private long combatPower;
    private int chuc;
    private int arc;
    private int aut;

    public void updateCharacterStatToSQL() {
        if (getExtendSP() != null) {
            getExtendSP().updateExtendSPToSQL();
        }
        if (getNonCombatStatDayLimit() != null) {
            getNonCombatStatDayLimit().updateNonCombatStatDayLimitToSQL();
        }
        if (getAccountLastLogout() != null) {
            getAccountLastLogout().updateSystemTimeToSQL();
        }
        if (getId() == 0) {
            String query = "INSERT INTO `characterstats` (" +
                    "`characterid`, " +
                    "`characteridforlog`, " +
                    "`worldidforlog`, " +
                    "`name`, " +
                    "`gender`, " +
                    "`skin`, " +
                    "`face`, " +
                    "`hair`, " +
                    "`mixbasehaircolor`, " +
                    "`mixaddhaircolor`, " +
                    "`mixhairbaseprob`, " +
                    "`level`, " +
                    "`job`, " +
                    "`str`, " +
                    "`dex`, " +
                    "`inte`, " +
                    "`luk`, " +
                    "`hp`, " +
                    "`maxhp`, " +
                    "`mp`, " +
                    "`maxmp`, " +
                    "`ap`, " +
                    "`sp`, " +
                    "`exp`, " +
                    "`pop`, " +
                    "`money`, " +
                    "`wp`, " +
                    "`extendsp`, " + //ID
                    "`posmap`, " +
                    "`portal`, " +
                    "`subjob`, " +
                    "`deffaceacc`, " +
                    "`fatigue`, " +
                    "`lastfatigueupdatetime`, " +
                    "`charismaexp`, " +
                    "`insightexp`, " +
                    "`willexp`, " +
                    "`craftexp`, " +
                    "`senseexp`, " +
                    "`charmexp`, " +
                    "`noncombatstatdaylimit`, " + //ID
                    "`mcpoint`, " +
                    "`pvpexp`, " +
                    "`pvpgrade`, " +
                    "`pvppoint`, " +
                    "`pvpmodelevel`, " +
                    "`pvpmodetype`, " +
                    "`eventpoint`, " +
                    "`albaactivityid`, " +
                    "`albastarttime`, " +
                    "`albaduration`, " +
                    "`albaspecialreward`, " +
                    "`charactercard`, " + //ID
                    "`accountlastlogout`, " + //ID
                    "`lastlogout`, " +
                    "`gachexp`, " +
                    "`honorexp`, " +
                    "`nextavailablefametime`, " +
                    "`node_shards`, " +
                    "`maxfriends`, " +
                    "`combatpower`, " +
                    "`chuc`, " +
                    "`arc`, " +
                    "`aut` " +
                    ") VALUES (" +
                    String.format("%d, ", getCharacterId()) +
                    String.format("%d, ", getCharacterIdForLog()) +
                    String.format("%d, ", getWorldIdForLog()) +
                    String.format("'%s', ", DatabaseManager.getStringFilter(getName())) +
                    String.format("%d, ", getGender()) +
                    String.format("%d, ", getSkin()) +
                    String.format("%d, ", getFace()) +
                    String.format("%d, ", getHair()) +
                    String.format("%d, ", getMixBaseHairColor()) +
                    String.format("%d, ", getMixAddHairColor()) +
                    String.format("%d, ", getMixHairBaseProb()) +
                    String.format("%d, ", getLevel()) +
                    String.format("%d, ", getJob()) +
                    String.format("%d, ", getStr()) +
                    String.format("%d, ", getDex()) +
                    String.format("%d, ", getInt()) +
                    String.format("%d, ", getLuk()) +
                    String.format("%d, ", getHp()) +
                    String.format("%d, ", getMaxHp()) +
                    String.format("%d, ", getMp()) +
                    String.format("%d, ", getMaxMp()) +
                    String.format("%d, ", getAp()) +
                    String.format("%d, ", getSp()) +
                    String.format("%d, ", getExp()) +
                    String.format("%d, ", getPop()) +
                    String.format("%d, ", getMoney()) +
                    String.format("%d, ", getWp()) +
                    (getExtendSP() != null ? String.format("%d, ", getExtendSP().getId()) : "NULL, ") +
                    String.format("%d, ", getPosMap()) +
                    String.format("%d, ", getPortal()) +
                    String.format("%d, ", getSubJob()) +
                    String.format("%d, ", getDefFaceAcc()) +
                    String.format("%d, ", getFatigue()) +
                    String.format("%d, ", getLastFatigueUpdateTime()) +
                    String.format("%d, ", getCharismaExp()) +
                    String.format("%d, ", getInsightExp()) +
                    String.format("%d, ", getWillExp()) +
                    String.format("%d, ", getCraftExp()) +
                    String.format("%d, ", getSenseExp()) +
                    String.format("%d, ", getCharmExp()) +
                    (getNonCombatStatDayLimit() != null ? String.format("%d, ", getNonCombatStatDayLimit().getId()) : "NULL, ") +
                    String.format("%d, ", getMcpoint()) +
                    String.format("%d, ", getPvpExp()) +
                    String.format("%d, ", getPvpGrade()) +
                    String.format("%d, ", getPvpPoint()) +
                    String.format("%d, ", getPvpModeLevel()) +
                    String.format("%d, ", getPvpModeType()) +
                    String.format("%d, ", getEventPoint()) +
                    String.format("%d, ", getAlbaActivityID()) +
                    DatabaseManager.getSQLStringSyntax(false, "", getAlbaStartTime(), false) +
                    String.format("%d, ", getAlbaDuration()) +
                    String.format("%d, ", getAlbaSpecialReward()) +
                    "NULL, " +
                    (getAccountLastLogout() != null ? String.format("%d, ", getAccountLastLogout().getId()) : "NULL, ") +
                    DatabaseManager.getSQLStringSyntax(false, "", getLastLogout(), false) +
                    String.format("%d, ", getGachExp()) +
                    String.format("%d, ", getHonorExp()) +
                    DatabaseManager.getSQLStringSyntax(false, "", getNextAvailableFameTime(), false) +
                    String.format("%d, ", getNodeShards()) +
                    String.format("%d, ", getMaxFriends()) +
                    String.format("%d, ", getCombatPower()) +
                    String.format("%d, ", getChuc()) +
                    String.format("%d, ", getArc()) +
                    String.format("%d ", getAut()) +
                    ");";
            int id = (int) DatabaseManager.executeStatementReturnID(query);
            setId(id);
        } else {
            String query = "UPDATE characterstats SET " +
                    String.format("characterid = %d, ", getCharacterId()) +
                    String.format("worldidforlog = %d, ", getWorldIdForLog()) +
                    String.format("name = '%s', ", DatabaseManager.getStringFilter(getName())) +
                    String.format("gender = %d, ", getGender()) +
                    String.format("skin = %d, ", getSkin()) +
                    String.format("face = %d, ", getFace()) +
                    String.format("hair = %d, ", getHair()) +
                    String.format("mixbasehaircolor = %d, ", getMixBaseHairColor()) +
                    String.format("mixaddhaircolor = %d, ", getMixAddHairColor()) +
                    String.format("mixhairbaseprob = %d, ", getMixHairBaseProb()) +
                    String.format("level = %d, ", getLevel()) +
                    String.format("job = %d, ", getJob()) +
                    String.format("str = %d, ", getStr()) +
                    String.format("dex = %d, ", getDex()) +
                    String.format("inte = %d, ", getInt()) +
                    String.format("luk = %d, ", getLuk()) +
                    String.format("hp = %d, ", getHp()) +
                    String.format("maxhp = %d, ", getMaxHp()) +
                    String.format("mp = %d, ", getMp()) +
                    String.format("maxmp = %d, ", getMaxMp()) +
                    String.format("ap = %d, ", getAp()) +
                    String.format("sp = %d, ", getSp()) +
                    String.format("exp = %d, ", getExp()) +
                    String.format("pop = %d, ", getPop()) +
                    String.format("money = %d, ", getMoney()) +
                    String.format("wp = %d, ", getWp()) +
                    String.format("posmap = %d, ", getPosMap()) +
                    String.format("portal = %d, ", getPortal()) +
                    String.format("subjob = %d, ", getSubJob()) +
                    String.format("deffaceacc = %d, ", getDefFaceAcc()) +
                    String.format("fatigue = %d, ", getFatigue()) +
                    String.format("lastfatigueupdatetime = %d, ", getLastFatigueUpdateTime()) +
                    String.format("charismaexp = %d, ", getCharismaExp()) +
                    String.format("insightexp = %d, ", getInsightExp()) +
                    String.format("willexp = %d, ", getWillExp()) +
                    String.format("craftexp = %d, ", getCraftExp()) +
                    String.format("senseexp = %d, ", getSenseExp()) +
                    String.format("charmexp = %d, ", getCharmExp()) +
                    String.format("mcpoint = %d, ", getMcpoint()) +
                    //PVP, Alba, Burning not use
                    String.format("eventpoint = %d, ", getEventPoint()) +
                    "lastlogout = " + sqlDateTime(getLastLogout()) + ", " +
                    String.format("gachexp = %d, ", getGachExp()) +
                    String.format("honorexp = %d, ", getHonorExp()) +
                    "nextavailablefametime = " + sqlDateTime(getNextAvailableFameTime()) + ", " +
                    String.format("node_shards = %d, ", getNodeShards()) +
                    String.format("maxfriends = %d, ", getMaxFriends()) +
                    String.format("combatpower = %d, ", getCombatPower()) +
                    String.format("chuc = %d, ", getChuc()) +
                    String.format("arc = %d, ", getArc()) +
                    String.format("aut = %d ", getAut()) +
                    String.format("WHERE id = %d;", getId());
            DatabaseManager.executeStatement(query);
        }
    }

    public void deleteFromSQL(Connection con) throws SQLException {
        if (getExtendSP() != null) {
            getExtendSP().deleteFromSQL(con);
        }
        if (getNonCombatStatDayLimit() != null) {
            getNonCombatStatDayLimit().deleteFromSQL(con);
        }
        if (getAccountLastLogout() != null) {
            getAccountLastLogout().deleteFromSQL(con);
        }
        String query = "DELETE FROM `characterstats` WHERE `id` = ?";
        try (PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, getId());
            ps.executeUpdate();
        }
    }

    public void updateCharacterMaxFriendsToSQL() {
        String query = "UPDATE characterstats SET " +
                String.format("maxfriends = %d ", getMaxFriends()) +
                String.format("WHERE id = %d;", getId());
        DatabaseManager.executeStatement(query);
    }

    public CharacterStat() {
        extendSP = new ExtendSP(7);
        nonCombatStatDayLimit = new NonCombatStatDayLimit();
        albaStartTime = FileTime.fromType(FileTime.Type.PLAIN_ZERO);
        lastLogout = FileTime.fromType(FileTime.Type.PLAIN_ZERO);
        accountLastLogout = new SystemTime(LocalDateTime.now().getYear(), LocalDateTime.now().getMonth().getValue());
        nextAvailableFameTime = FileTime.currentTime();
        // TODO fill in default vals
    }

    public CharacterStat(String name, int job) {
        this();
        this.name = name;
        this.job = job;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public short getAp() {
        return (short) ap;
    }

    public void setAp(int ap) {
        this.ap = ap;
    }

    public short getDex() {
        return (short) dex;
    }

    public void setDex(int dex) {
        this.dex = dex;
    }

    public int getHp() {
        return hp;
    }

    public void setHp(int hp) {
        this.hp = Math.min(hp, GameConstants.MAX_HP_MP);
    }

    public short getInt() {
        return (short) inte;
    }

    public void setInt(int inte) {
        this.inte = inte;
    }

    public short getJob() {
        return (short) job;
    }

    public void setJob(int job) {
        this.job = job;
    }

    public short getLevel() {
        return (short) level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getCharismaExp() {
        return (short) charismaExp;
    }

    public void setCharismaExp(int charismaExp) {
        this.charismaExp = charismaExp;
    }

    public short getLuk() {
        return (short) luk;
    }

    public void setLuk(int luk) {
        this.luk = luk;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public void setMaxHp(int maxHp) {
        this.maxHp = Math.min(maxHp, GameConstants.MAX_HP_MP);
    }

    public int getMaxMp() {
        return maxMp;
    }

    public void setMaxMp(int maxMp) {
        this.maxMp = Math.min(maxMp, GameConstants.MAX_HP_MP);
    }

    public int getMp() {
        return mp;
    }

    public void setMp(int mp) {
        this.mp = Math.min(mp, GameConstants.MAX_HP_MP);
    }

    public short getPop() { //Fame
        return (short) pop;
    }

    public void setPop(int pop) {
        this.pop = pop;
    }

    public short getSp() {
        return (short) sp;
    }

    public void setSp(int sp) {
        this.sp = sp;
    }

    public short getStr() {
        return (short) str;
    }

    public void setStr(int str) {
        this.str = str;
    }

    public short getWp() {
        return (short) wp;
    }

    public void setWp(int wp) {
        this.wp = wp;
    }

    public long getExp() {
        return exp;
    }

    public void setExp(long exp) {
        this.exp = exp;
    }

    public long getMoney() {
        return money;
    }

    public void setMoney(long money) {
        this.money = money;
    }

    public int getNodeShards() {
        return node_shards;
    }

    public void setNodeShards(int nodeShards) {
        this.node_shards = nodeShards;
    }

    public ExtendSP getExtendSP() {
        return extendSP;
    }

    public void setExtendSP(ExtendSP extendSP) {
        this.extendSP = extendSP;
    }

    public int getCharacterId() {
        return characterId;
    }

    public void setCharacterId(int characterId) {
        this.characterId = characterId;
    }

    public int getCharacterIdForLog() {
        return characterId;
    }

    public void setCharacterIdForLog(int characterIdForLog) {
        this.characterIdForLog = characterIdForLog;
    }

    public int getFace() {
        return face;
    }

    public void setFace(int face) {
        this.face = face;
    }

    public int getGender() {
        return gender;
    }

    public void setGender(int gender) {
        this.gender = gender;
    }

    public int getHair() {
        return hair;
    }

    public void setHair(int hair) {
        this.hair = hair;
    }

    public int getMixAddHairColor() {
        return mixAddHairColor;
    }

    public void setMixAddHairColor(int mixAddHairColor) {
        this.mixAddHairColor = mixAddHairColor;
    }

    public int getMixBaseHairColor() {
        return mixBaseHairColor;
    }

    public void setMixBaseHairColor(int mixBaseHairColor) {
        this.mixBaseHairColor = mixBaseHairColor;
    }

    public int getMixHairBaseProb() {
        return mixHairBaseProb;
    }

    public void setMixHairBaseProb(int mixHairBaseProb) {
        this.mixHairBaseProb = mixHairBaseProb;
    }

    public int getSkin() {
        return skin;
    }

    public void setSkin(int skin) {
        this.skin = skin;
    }

    public int getWorldIdForLog() {
        return worldIdForLog;
    }

    public void setWorldIdForLog(int worldIdForLog) {
        this.worldIdForLog = worldIdForLog;
    }

    public int getCharmExp() {
        return charmExp;
    }

    public void setCharmExp(int charmExp) {
        this.charmExp = charmExp;
    }

    public int getCraftExp() {
        return craftExp;
    }

    public void setCraftExp(int craftExp) {
        this.craftExp = craftExp;
    }

    public int getAlbaActivityID() {
        return albaActivityID;
    }

    public void setAlbaActivityID(int albaActivityID) {
        this.albaActivityID = albaActivityID;
    }

    public int getEventPoint() {
        return eventPoint;
    }

    public void setEventPoint(int eventPoint) {
        this.eventPoint = eventPoint;
    }

    public int getPortal() {
        return portal;
    }

    public void setPortal(int portal) {
        this.portal = portal;
    }

    public int getAlbaDuration() {
        return albaDuration;
    }

    public void setAlbaDuration(int albaDuration) {
        this.albaDuration = albaDuration;
    }

    public int getInsightExp() {
        return insightExp;
    }

    public void setInsightExp(int insightExp) {
        this.insightExp = insightExp;
    }

    public int getAlbaSpecialReward() {
        return albaSpecialReward;
    }

    public void setAlbaSpecialReward(int albaSpecialReward) {
        this.albaSpecialReward = albaSpecialReward;
    }

    public int getPvpExp() {
        return pvpExp;
    }

    public void setPvpExp(int pvpExp) {
        this.pvpExp = pvpExp;
    }

    public int getPvpGrade() {
        return pvpGrade;
    }

    public void setPvpGrade(int pvpGrade) {
        this.pvpGrade = pvpGrade;
    }

    public int getPvpModeLevel() {
        return pvpModeLevel;
    }

    public void setPvpModeLevel(int pvpModeLevel) {
        this.pvpModeLevel = pvpModeLevel;
    }

    public int getPvpModeType() {
        return pvpModeType;
    }

    public void setPvpModeType(int pvpModeType) {
        this.pvpModeType = pvpModeType;
    }

    public int getPvpPoint() {
        return pvpPoint;
    }

    public void setPvpPoint(int pvpPoint) {
        this.pvpPoint = pvpPoint;
    }

    public int getSenseExp() {
        return senseExp;
    }

    public void setSenseExp(int senseExp) {
        this.senseExp = senseExp;
    }

    public int getWillExp() {
        return willExp;
    }

    public void setWillExp(int willExp) {
        this.willExp = willExp;
    }

    public long getPosMap() {
        return posMap == 0 ? 931000000 : posMap;
    }

    public void setPosMap(long posMap) {
        this.posMap = posMap;
    }

    public NonCombatStatDayLimit getNonCombatStatDayLimit() {
        return nonCombatStatDayLimit;
    }

    public void setNonCombatStatDayLimit(NonCombatStatDayLimit nonCombatStatDayLimit) {
        this.nonCombatStatDayLimit = nonCombatStatDayLimit;
    }

    public FileTime getAlbaStartTime() {
        return albaStartTime;
    }

    public void setAlbaStartTime(FileTime albaStartTime) {
        this.albaStartTime = albaStartTime;
    }

    public int getDefFaceAcc() {
        return defFaceAcc;
    }

    public void setDefFaceAcc(int defFaceAcc) {
        this.defFaceAcc = defFaceAcc;
    }

    public int getFatigue() {
        return fatigue;
    }

    public void setFatigue(int fatigue) {
        this.fatigue = fatigue;
    }

    public int getLastFatigueUpdateTime() {
        return lastFatigueUpdateTime;
    }

    public void setLastFatigueUpdateTime(int lastFatigueUpdateTime) {
        this.lastFatigueUpdateTime = lastFatigueUpdateTime;
    }

    public int getSubJob() {
        return subJob;
    }

    public void setSubJob(int subJob) {
        this.subJob = subJob;
    }

    public SystemTime getAccountLastLogout() {
        return accountLastLogout;
    }

    public void setAccountLastLogout(SystemTime accountLastLogout) {
        this.accountLastLogout = accountLastLogout;
    }

    public void encode(OutPacket outPacket) {
        outPacket.encodeInt(getCharacterId());
        outPacket.encodeInt(getCharacterIdForLog());
        outPacket.encodeInt(getWorldIdForLog());
        outPacket.encodeString(getName(), 13);
        outPacket.encodeByte(getGender());
        outPacket.encodeByte(getSkin());
        outPacket.encodeInt(0);
        outPacket.encodeInt(getFace());
        if (getMixBaseHairColor() != -1) {
            outPacket.encodeInt(getHair() / 10 * 10 + getMixAddHairColor());
        } else {
            outPacket.encodeInt(getHair());
        }
        outPacket.encodeInt(getLevel());
        outPacket.encodeShort(getJob());
        outPacket.encodeShort(getStr());
        outPacket.encodeShort(getDex());
        outPacket.encodeShort(getInt());
        outPacket.encodeShort(getLuk());
        outPacket.encodeInt(getHp());
        outPacket.encodeInt(getMaxHp());
        outPacket.encodeInt(getMp());
        outPacket.encodeInt(getMaxMp());
        outPacket.encodeShort(getAp());
        if (JobConstants.isExtendSpJob(getJob())) {
            getExtendSP().encode(outPacket);
        } else {
            outPacket.encodeShort(getSp());
        }
        outPacket.encodeLong(getExp());
        outPacket.encodeInt(getPop());
        outPacket.encodeInt(getWp()); // Zero Weapon Point
        outPacket.encodeInt(getGachExp());
        outPacket.encodeInt((int) getPosMap());
        outPacket.encodeByte(getPortal());
        outPacket.encodeInt(0); // 3171
        outPacket.encodeShort(getSubJob());
        if (JobConstants.isDemon(getJob())
                || JobConstants.isXenon(getJob())
                || JobConstants.isBeastTamer(getJob())
                || JobConstants.isArk(getJob())
                || JobConstants.isHoYoung(getJob())) {
            outPacket.encodeInt(getDefFaceAcc());
        }
        outPacket.encodeByte(0);
        outPacket.encodeFT(FileTime.MIN_TIME());
        outPacket.encodeInt(getCharismaExp());
        outPacket.encodeInt(getInsightExp());
        outPacket.encodeInt(getWillExp()); //willPower
        outPacket.encodeInt(getCraftExp());
        outPacket.encodeInt(getSenseExp()); //empathy
        outPacket.encodeInt(getCharmExp()); //charm
        getNonCombatStatDayLimit().encode(outPacket);
        outPacket.encodeInt(getPvpExp());
        outPacket.encodeByte(getPvpGrade());
        outPacket.encodeInt(getPvpPoint());
        outPacket.encodeByte(0);
        outPacket.encodeByte(getPvpModeType());
        outPacket.encodeInt(getEventPoint());
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
        encodeBurning(outPacket);
        outPacket.encodeInt(0);
    }

    public void encodeBurning(OutPacket outPacket) {
        // Start burning info, names are guesses
        if (EventConstants.HYPER_BURNING_MAX && getLevel() < 260) {
            outPacket.encodeFT(FileTime.fromDate(EventConstants.HYPER_BURNING_MAX_START_DATE));
            outPacket.encodeFT(FileTime.fromDate(EventConstants.HYPER_BURNING_MAX_END_DATE));
            outPacket.encodeInt(EventConstants.HYPER_BURNING_MAX_MIN_LEVEL);
            outPacket.encodeInt(EventConstants.HYPER_BURNING_MAX_MAX_LEVEL);
            outPacket.encodeInt(EventConstants.HYPER_BURNING_MAX_TYPE);
            outPacket.encodeByte(EventConstants.HYPER_BURNING_MAX_BURNING_TYPE);
        } else if (EventConstants.BEYOND_BURNING) {
            outPacket.encodeFT(FileTime.fromDate(EventConstants.BEYOND_BURNING_START_DATE));
            outPacket.encodeFT(FileTime.fromDate(EventConstants.BEYOND_BURNING_END_DATE));
            outPacket.encodeInt(EventConstants.BEYOND_BURNING_MIN_LEVEL);
            outPacket.encodeInt(EventConstants.BEYOND_BURNING_MAX_LEVEL);
            outPacket.encodeInt(EventConstants.BEYOND_BURNING_TYPE);
            outPacket.encodeByte(EventConstants.BEYOND_BURNING_BURNING_TYPE);
        } else {
            outPacket.encodeLong(150842304000000000L);
            outPacket.encodeLong(94354848000000000L);
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);
            outPacket.encodeByte(0);
        }
        outPacket.encodeInt(0);
        outPacket.encodeString("");
        outPacket.encodeInt(0);
        outPacket.encodeString("");
        outPacket.encodeString("");
        outPacket.encodeString("");
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
        // End burning info
    }

    public void encodeUnk(OutPacket outPacket) {
        outPacket.encodeInt(getChuc());
        outPacket.encodeInt(getArc());
        outPacket.encodeLong(0);
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
        outPacket.encodeByte(0);
        outPacket.encodeByte(0);
        outPacket.encodeString("");
        outPacket.encodeByte(0);
        outPacket.encodeString("");
        outPacket.encodeLong(getCombatPower()); // Combat Power
    }

    /** 時間欄位寫進 UPDATE：值是 null 時寫 SQL 的 NULL（原本會變成字串 'null'，整條 UPDATE 失敗，角色能力值、楓幣、經驗都存不進去）。 */
    private static String sqlDateTime(FileTime fileTime) {
        java.sql.Timestamp ts = fileTime == null ? null : DatabaseManager.convertToDateTimeSQL(fileTime);
        return ts == null ? "NULL" : "'" + ts + "'";
    }

    public FileTime getLastLogout() {
        return lastLogout;
    }

    public void setLastLogout(FileTime lastLogout) {
        this.lastLogout = lastLogout;
    }

    public boolean isBurning() {
        return burning;
    }

    public void setBurning(boolean burning) {
        this.burning = burning;
    }

    public int getGachExp() {
        return gachExp;
    }

    public void setGachExp(int gachExp) {
        this.gachExp = gachExp;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getHonorExp() {
        return honorExp;
    }

    public void setHonorExp(int honorExp) {
        this.honorExp = honorExp;
    }

    public FileTime getNextAvailableFameTime() {
        return nextAvailableFameTime;
    }

    public void setNextAvailableFameTime(FileTime nextAvailableFameTime) {
        this.nextAvailableFameTime = nextAvailableFameTime;
    }

    public int getMaxFriends() {
        return maxFriends;
    }

    public void setMaxFriends(int maxFriends) {
        this.maxFriends = maxFriends;
    }

    public void addMaxFriends(int slot) {
        this.maxFriends = getMaxFriends() + slot;
    }

    public int getMcpoint() {
        return mcpoint;
    }

    public void setMcpoint(int mcpoint) {
        this.mcpoint = mcpoint;
    }

    public void incMcpoint(int mcpoint) {
        this.mcpoint += mcpoint;
    }

    public void decMcpoint(int dec) {
        int point = getMcpoint();
        this.mcpoint = Math.max(point - dec, 0);
    }

    public long getCombatPower() {
        return combatPower;
    }

    public void setCombatPower(long combatPower) {
        this.combatPower = combatPower;
    }

    public int getChuc() {
        return chuc;
    }

    public void setChuc(int chuc) {
        this.chuc = chuc;
    }

    public int getArc() {
        return arc;
    }

    public void setArc(int arc) {
        this.arc = arc;
    }

    public int getAut() {
        return aut;
    }

    public void setAut(int aut) {
        this.aut = aut;
    }
}

