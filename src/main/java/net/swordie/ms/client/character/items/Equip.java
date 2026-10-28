package net.swordie.ms.client.character.items;

import net.swordie.ms.client.Account;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.CharacterStat;
import net.swordie.ms.client.character.union.Union;
import net.swordie.ms.client.character.union.UnionBoard;
import net.swordie.ms.client.character.union.UnionMember;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.connection.packet.*;
import net.swordie.ms.constants.GameConstants;
import net.swordie.ms.constants.ItemConstants;
import net.swordie.ms.constants.PotentialConstants;
import net.swordie.ms.enums.*;
import net.swordie.ms.life.Android;
import net.swordie.ms.loaders.ItemData;
import net.swordie.ms.loaders.StringData;
import net.swordie.ms.loaders.containerclasses.ItemInfo;
import net.swordie.ms.util.FileTime;
import net.swordie.ms.util.Randomizer;
import net.swordie.ms.util.Util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

import static net.swordie.ms.enums.ChatType.SystemNotice;

public class Equip extends Item {

    private long serialNumber;
    private long primaryKeyItemID;
    private String title = "";
    private FileTime equippedDate = FileTime.fromType(FileTime.Type.PLAIN_ZERO);
    private int prevBonusExpRate;
    private short tuc;
    private short cuc;
    private short iStr;
    private short iDex;
    private short iInt;
    private short iLuk;
    private short iMaxHp;
    private short iMaxHpr;
    private short iMaxMp;
    private short iMaxMpr;
    private short iPad;
    private short iMad;
    private short iPDD;
    private short iMDD;
    private short iAcc;
    private short iEva;
    private short iCraft;
    private short iSpeed;
    private short iJump;
    private short bdr;
    private short imdr;
    private short damR;
    private short statR;
    private int attribute;
    private short levelUpType;
    private short level;
    private short exp;
    private short durability;
    private short iuc;
    private short iPvpDamage;
    private byte iReduceReq;
    private int specialAttribute;
    private short durabilityMax;
    private short iIncReq;
    private short growthEnchant;
    private short psEnchant;
    private boolean bossReward;
    private boolean superiorEqp;
    private short cuttable;
    private long exGradeOption;
    private int hyperUpgrade;
    private short itemState;
    private short chuc;
    private short soulOptionId;
    private short soulSocketId;
    private short soulOption;
    private int soulItemId;
    private short rStr;
    private short rDex;
    private short rInt;
    private short rLuk;
    private short rLevel;
    private short rJob;
    private short rPop;
    private List<Integer> options = new ArrayList<>(); // base + add pot + anvil
    private int specialGrade;
    private boolean fixedPotential;
    private boolean noPotential;
    private boolean tradeBlock;
    private boolean only;
    private boolean notSale;
    private int attackSpeed;
    private long price;
    private int charmEXP;
    private int setItemID;
    private boolean exItem;
    private boolean equipTradeBlock;
    private String iSlot = "";
    private String vSlot = "";
    private int fixedGrade;

    private Map<EnchantStat, Integer> enchantStats = new HashMap<>();
    private List<Short> sockets = new LinkedList<>();
    private int dropStreak = 0;
    private List<ItemSkill> itemSkills = new ArrayList<>();
    private short iucMax = ItemConstants.MAX_HAMMER_SLOTS;
    private boolean hasIUCMax = false;
    private int android;
    private Android androidLife;
    private int androidGrade;
    private EquipFlame flameStat;

    private byte ExceptionalSlot = 0;
    private EquipExceptional exceptionalStat;

    private EquipSymbol symbol;

    private int tradeAvailable = 0; //1 = Untrade, 2 = Trade disable when equipped
    private boolean accountSharable = false;
    private boolean sharableOnce = false;
    private boolean isLastScrollFail = false;
    private boolean isTalented = false;
    private int talentProgress = 0;

    private int preset = 0;

    public void insertEquipToSQL(long id) {
        try { insertEquipToSQL(id, null); }
        catch (java.sql.SQLException e) { throw new IllegalStateException("Equip insert failed", e); }
    }

    /** A caller-owned transaction; the legacy null-connection path keeps its existing behavior. */
    public void insertEquipToSQL(long id, java.sql.Connection connection) throws java.sql.SQLException {
        String query = "INSERT INTO `equips` (" +
                "`charid`, " +
                "`preset`, " +
                "`itemid`, " +
                "`title`, " +
                "`equippeddate`, " +
                "`options`, " +
                "`sockets`, " +
                "`tuc`, " +
                "`cuc`, " +
                "`istr`, " +
                "`idex`, " +
                "`iint`, " +
                "`iluk`, " +
                "`imaxhp`, " +
                "`imaxhpr`, " +
                "`imaxmp`, " +
                "`imaxmpr`, " +
                "`ipad`, " +
                "`imad`, " +
                "`ipdd`, " +
                "`imdd`, " +
                "`iacc`, " +
                "`ieva`, " +
                "`icraft`, " +
                "`equipattribute`, " +
                "`ispeed`, " +
                "`ijump`, " +
                "`leveluptype`, " +
                "`level`, " +
                "`exp`, " +
                "`durability`, " +
                "`iuc`, " +
                "`ireducereq`, " +
                "`specialattribute`, " +
                "`durabilitymax`, " +
                "`iincreq`, " +
                "`growthenchant`, " +
                "`psenchant`, " +
                "`hyperupgrade`, " +
                "`bdr`, " +
                "`imdr`, " +
                "`damr`, " +
                "`statr`, " +
                "`cuttable`, " +
                "`exgradeoption`, " +
                "`itemstate`, " +
                "`grade`, " +
                "`chuc`, " +
                "`souloptionid`, " +
                "`soulsocketid`, " +
                "`souloption`, " +
                "`soulitemid`, " +
                "`rstr`, " +
                "`rdex`, " +
                "`rint`, " +
                "`rluk`, " +
                "`rlevel`, " +
                "`rjob`, " +
                "`rpop`, " +
                "`specialgrade`, " +
                "`fixedpotential`, " +
                "`tradeblock`, " +
                "`isonly`, " +
                "`notsale`, " +
                "`attackspeed`, " +
                "`price`, " +
                "`charmexp`, " +
                "`setitemid`, " +
                "`exitem`, " +
                "`equiptradeblock`, " +
                "`islot`, " +
                "`vslot`, " +
                "`fixedgrade`, " +
                "`nopotential`, " +
                "`bossreward`, " +
                "`superioreqp`, " +
                "`android`, " +
                "`androidgrade`, " +

                "`arcane_stat`, " +
                "`arcane_exp`, " +
                "`arcane_level`, " +

                "`flame_str`, " +
                "`flame_dex`, " +
                "`flame_int`, " +
                "`flame_luk`, " +
                "`flame_pad`, " +
                "`flame_mad`, " +
                "`flame_pdd`, " +
                "`flame_hp`, " +
                "`flame_mp`, " +
                "`flame_speed`, " +
                "`flame_jump`, " +
                "`flame_allStatR`, " +
                "`flame_bossDamageR`, " +
                "`flame_damageR`, " +
                "`flame_reduceReqLevel`, " +

                "`except_slot`, " +
                "`except_str`, " +
                "`except_dex`, " +
                "`except_int`, " +
                "`except_luk`, " +
                "`except_pad`, " +
                "`except_mad`, " +
                "`except_pdd`, " +
                "`except_hp`, " +
                "`except_mp`, " +
                "`except_speed`, " +
                "`except_jump`, " +
                "`except_allStatR`, " +
                "`except_bossDamageR`, " +
                "`except_damageR`, " +
                "`except_reduceReqLevel` " +

                ") VALUES (" +
                (getCharID() != 0 ? String.format("%d, ", getCharID()) : "NULL, ") +
                String.format("%d, ", getPreset()) +
                String.format("%d, ", id) +
                String.format("'%s', ", getTitle()) +
                "'1970-01-01 00:00:00.000', " +
                String.format("'%s', ", getOptionsSQL()) +
                String.format("'%s', ", getSocketsSQL()) +
                String.format("%d, ", getTuc()) +
                String.format("%d, ", getCuc()) +
                String.format("%d, ", getiStr()) +
                String.format("%d, ", getiDex()) +
                String.format("%d, ", getiInt()) +
                String.format("%d, ", getiLuk()) +
                String.format("%d, ", getiMaxHp()) +
                String.format("%d, ", getiMaxHpr()) +
                String.format("%d, ", getiMaxMp()) +
                String.format("%d, ", getiMaxMpr()) +
                String.format("%d, ", getiPad()) +
                String.format("%d, ", getiMad()) +
                String.format("%d, ", getiPDD()) +
                String.format("%d, ", getiMDD()) +
                String.format("%d, ", getiAcc()) +
                String.format("%d, ", getiEva()) +
                String.format("%d, ", getiCraft()) +
                String.format("%d, ", getAttribute()) +
                String.format("%d, ", getiSpeed()) +
                String.format("%d, ", getiJump()) +
                String.format("%d, ", getLevelUpType()) +
                String.format("%d, ", getItemLevel()) +
                String.format("%d, ", getItemEXP()) +
                String.format("%d, ", getDurability()) +
                String.format("%d, ", getIuc()) +
                String.format("%d, ", getiReduceReq()) +
                String.format("%d, ", getSpecialAttribute()) +
                String.format("%d, ", getDurabilityMax()) +
                String.format("%d, ", getiIncReq()) +
                String.format("%d, ", getGrowthEnchant()) +
                String.format("%d, ", getPsEnchant()) +
                String.format("%d, ", getHyperUprade()) +
                String.format("%d, ", getBdr()) +
                String.format("%d, ", getImdr()) +
                String.format("%d, ", getDamR()) +
                String.format("%d, ", getStatR()) +
                String.format("%d, ", getCuttable()) +
                String.format("%d, ", getExGradeOption()) +
                String.format("%d, ", getItemState()) +
                String.format("%d, ", getGrade()) +
                String.format("%d, ", getChuc()) +
                String.format("%d, ", getSoulOptionId()) +
                String.format("%d, ", getSoulSocketId()) +
                String.format("%d, ", getSoulOption()) +
                String.format("%d, ", getSoulItemId()) +
                String.format("%d, ", getrStr()) +
                String.format("%d, ", getrDex()) +
                String.format("%d, ", getrInt()) +
                String.format("%d, ", getrLuk()) +
                String.format("%d, ", getrLevel()) +
                String.format("%d, ", getrJob()) +
                String.format("%d, ", getrPop()) +
                String.format("%d, ", getSpecialGrade()) +
                String.format("%d, ", isFixedPotential() ? 1 : 0) +
                String.format("%d, ", isTradeBlock() ? 1 : 0) +
                String.format("%d, ", isOnly() ? 1 : 0) +
                String.format("%d, ", isNotSale() ? 1 : 0) +
                String.format("%d, ", getAttackSpeed()) +
                String.format("%d, ", getPrice()) +
                String.format("%d, ", getCharmEXP()) +
                String.format("%d, ", getSetItemID()) +
                String.format("%d, ", isExItem() ? 1 : 0) +
                String.format("%d, ", isEquipTradeBlock() ? 1 : 0) +
                String.format("'%s', ", getiSlot()) +
                String.format("'%s', ", getvSlot()) +
                String.format("%d, ", getFixedGrade()) +
                String.format("%d, ", isNoPotential() ? 1 : 0) +
                String.format("%d, ", isBossReward() ? 1 : 0) +
                String.format("%d, ", isSuperiorEqp() ? 1 : 0) +
                String.format("%d, ", getAndroid()) +
                String.format("%d, ", getAndroidGrade()) +
                String.format("%d, ", getSymbol().getInc()) +
                String.format("%d, ", getSymbol().getExp()) +
                String.format("%d, ", getSymbol().getLevel()) +
                String.format("%d, ", getFlameStat().getSTR()) +
                String.format("%d, ", getFlameStat().getDEX()) +
                String.format("%d, ", getFlameStat().getINT()) +
                String.format("%d, ", getFlameStat().getLUK()) +
                String.format("%d, ", getFlameStat().getPAD()) +
                String.format("%d, ", getFlameStat().getMAD()) +
                String.format("%d, ", getFlameStat().getPDD()) +
                String.format("%d, ", getFlameStat().getHP()) +
                String.format("%d, ", getFlameStat().getMP()) +
                String.format("%d, ", getFlameStat().getSpeed()) +
                String.format("%d, ", getFlameStat().getJump()) +
                String.format("%d, ", getFlameStat().getAllStatR()) +
                String.format("%d, ", getFlameStat().getBossDamageR()) +
                String.format("%d, ", getFlameStat().getDamage()) +
                String.format("%d, ", getFlameStat().getReduceReqLevel()) +
                String.format("%d, ", getExceptionalSlot()) +
                String.format("%d, ", getExceptionalStat().getSTR()) +
                String.format("%d, ", getExceptionalStat().getDEX()) +
                String.format("%d, ", getExceptionalStat().getINT()) +
                String.format("%d, ", getExceptionalStat().getLUK()) +
                String.format("%d, ", getExceptionalStat().getPAD()) +
                String.format("%d, ", getExceptionalStat().getMAD()) +
                String.format("%d, ", getExceptionalStat().getPDD()) +
                String.format("%d, ", getExceptionalStat().getHP()) +
                String.format("%d, ", getExceptionalStat().getMP()) +
                String.format("%d, ", getExceptionalStat().getSpeed()) +
                String.format("%d, ", getExceptionalStat().getJump()) +
                String.format("%d, ", getExceptionalStat().getAllStatR()) +
                String.format("%d, ", getExceptionalStat().getBossDamageR()) +
                String.format("%d, ", getExceptionalStat().getDamage()) +
                String.format("%d ", getExceptionalStat().getReduceReqLevel()) +
                ");";
        if (connection == null) DatabaseManager.executeStatement(query);
        else try (java.sql.PreparedStatement statement = connection.prepareStatement(query)) {
            if (statement.executeUpdate() != 1) throw new java.sql.SQLException("Equip insert was not saved");
        }
    }

    public void updateEquipToSQL() {
        String query = "UPDATE equips SET " +
                (getCharID() != 0 ? String.format("charid = %d, ", getCharID()) : "charid = NULL, ") +
                String.format("preset = %d, ", getPreset()) +
                String.format("title = '%s', ", getTitle()) +
                String.format("equippeddate = '%s', ", DatabaseManager.convertToDateTimeSQL(getEquippedDate())) +
                String.format("prevbonusexprate = %d, ", getPrevBonusExpRate()) +
                String.format("options = '%s', ", getOptionsSQL()) +
                String.format("sockets = '%s', ", getSocketsSQL()) +
                String.format("tuc = %d, ", getTuc()) +
                String.format("cuc = %d, ", getCuc()) +
                String.format("istr = %d, ", getiStr()) +
                String.format("idex = %d, ", getiDex()) +
                String.format("iint = %d, ", getiInt()) +
                String.format("iluk = %d, ", getiLuk()) +
                String.format("imaxhp = %d, ", getiMaxHp()) +
                String.format("imaxhpr = %d, ", getiMaxHpr()) +
                String.format("imaxmp = %d, ", getiMaxMp()) +
                String.format("imaxmpr = %d, ", getiMaxMpr()) +
                String.format("ipad = %d, ", getiPad()) +
                String.format("imad = %d, ", getiMad()) +
                String.format("ipdd = %d, ", getiPDD()) +
                String.format("imdd = %d, ", getiMDD()) +
                String.format("iacc = %d, ", getiAcc()) +
                String.format("ieva = %d, ", getiEva()) +
                String.format("icraft = %d, ", getiCraft()) +
                String.format("equipattribute = %d,", getAttribute()) +
                String.format("ispeed = %d, ", getiSpeed()) +
                String.format("leveluptype = %d, ", getLevelUpType()) +
                String.format("level = %d, ", getItemLevel()) +
                String.format("exp = %d, ", getItemEXP()) +
                String.format("durability = %d, ", getDurability()) +
                String.format("iuc = %d, ", getIuc()) +
                String.format("ipvpdamage = %d, ", getiPvpDamage()) +
                String.format("ireducereq = %d, ", getiReduceReq()) +
                String.format("specialattribute = %d, ", getSpecialAttribute()) +
                String.format("durabilitymax = %d, ", getDurabilityMax()) +
                String.format("iincreq = %d, ", getiIncReq()) +
                String.format("growthenchant = %d, ", getGrowthEnchant()) +
                String.format("psenchant = %d, ", getPsEnchant()) +
                String.format("hyperupgrade = %d, ", getHyperUprade()) +
                String.format("bdr = %d, ", getBdr()) +
                String.format("imdr = %d, ", getImdr()) +
                String.format("damr = %d, ", getDamR()) +
                String.format("statr = %d, ", getStatR()) +
                String.format("cuttable = %d, ", getCuttable()) +
                String.format("exgradeoption = %d, ", getExGradeOption()) +
                String.format("itemstate = %d, ", getItemState()) +
                String.format("grade = %d, ", getGrade()) +
                String.format("chuc = %d, ", getChuc()) +
                String.format("souloptionid = %d, ", getSoulOptionId()) +
                String.format("soulsocketid = %d, ", getSoulSocketId()) +
                String.format("souloption = %d, ", getSoulOption()) +
                String.format("soulitemid = %d, ", getSoulItemId()) +
                String.format("rstr = %d, ", getrStr()) +
                String.format("rdex = %d, ", getrDex()) +
                String.format("rint = %d, ", getrInt()) +
                String.format("rluk = %d, ", getrLuk()) +
                String.format("rlevel = %d, ", getrLevel()) +
                String.format("rjob = %d, ", getrJob()) +
                String.format("rpop = %d, ", getrPop()) +
                String.format("specialgrade = %d, ", getSpecialGrade()) +
                String.format("fixedpotential = %d, ", isFixedPotential() ? 1 : 0) +
                String.format("tradeblock = %d, ", isTradeBlock() ? 1 : 0) +
                String.format("isonly = %d, ", isOnly() ? 1 : 0) +
                String.format("notsale = %d, ", isNotSale() ? 1 : 0) +
                String.format("attackspeed = %d, ", getAttackSpeed()) +
                String.format("price = %d, ", getPrice()) +
                String.format("charmexp = %d, ", getCharmEXP()) +
                String.format("setitemid = %d, ", getSetItemID()) +
                String.format("exitem = %d, ", isExItem() ? 1 : 0) +
                String.format("equiptradeblock = %d, ", isEquipTradeBlock() ? 1 : 0) +
                String.format("islot = '%s', ", getiSlot()) +
                String.format("vslot = '%s', ", getvSlot()) +
                String.format("fixedgrade = %d, ", getFixedGrade()) +
                String.format("nopotential = %d, ", isNoPotential() ? 1 : 0) +
                String.format("bossreward = %d, ", isBossReward() ? 1 : 0) +
                String.format("superioreqp = %d, ", isSuperiorEqp() ? 1 : 0) +
                String.format("android = %d, ", getAndroid()) +
                String.format("androidgrade = %d, ", getAndroidGrade()) +
                String.format("arcane_stat = %d, ", getSymbol().getInc()) +
                String.format("arcane_exp = %d, ", getSymbol().getExp()) +
                String.format("arcane_level = %d, ", getSymbol().getLevel()) +
                String.format("flame_str = %d, ", getFlameStat().getSTR()) +
                String.format("flame_dex = %d, ", getFlameStat().getDEX()) +
                String.format("flame_int = %d, ", getFlameStat().getINT()) +
                String.format("flame_luk = %d, ", getFlameStat().getLUK()) +
                String.format("flame_pad = %d, ", getFlameStat().getPAD()) +
                String.format("flame_mad = %d, ", getFlameStat().getMAD()) +
                String.format("flame_pdd = %d, ", getFlameStat().getPDD()) +
                String.format("flame_hp = %d, ", getFlameStat().getHP()) +
                String.format("flame_mp = %d, ", getFlameStat().getMP()) +
                String.format("flame_speed = %d, ", getFlameStat().getSpeed()) +
                String.format("flame_jump = %d, ", getFlameStat().getJump()) +
                String.format("flame_allStatR = %d, ", getFlameStat().getAllStatR()) +
                String.format("flame_bossDamageR = %d, ", getFlameStat().getBossDamageR()) +
                String.format("flame_damageR = %d, ", getFlameStat().getDamage()) +
                String.format("flame_reduceReqLevel = %d, ", getFlameStat().getReduceReqLevel()) +
                String.format("except_slot = %d, ", getExceptionalSlot()) +
                String.format("except_str = %d, ", getExceptionalStat().getSTR()) +
                String.format("except_dex = %d, ", getExceptionalStat().getDEX()) +
                String.format("except_int = %d, ", getExceptionalStat().getINT()) +
                String.format("except_luk = %d, ", getExceptionalStat().getLUK()) +
                String.format("except_pad = %d, ", getExceptionalStat().getPAD()) +
                String.format("except_mad = %d, ", getExceptionalStat().getMAD()) +
                String.format("except_pdd = %d, ", getExceptionalStat().getPDD()) +
                String.format("except_hp = %d, ", getExceptionalStat().getHP()) +
                String.format("except_mp = %d, ", getExceptionalStat().getMP()) +
                String.format("except_speed = %d, ", getExceptionalStat().getSpeed()) +
                String.format("except_jump = %d, ", getExceptionalStat().getJump()) +
                String.format("except_allStatR = %d, ", getExceptionalStat().getAllStatR()) +
                String.format("except_bossDamageR = %d, ", getExceptionalStat().getBossDamageR()) +
                String.format("except_damageR = %d, ", getExceptionalStat().getDamage()) +
                String.format("except_reduceReqLevel = %d ", getExceptionalStat().getReduceReqLevel()) +
                String.format("WHERE itemid = %d;", getId());
        DatabaseManager.executeStatement(query);
    }

    public Equip() {
        super();
        this.exp = 0;
        this.level = 1;
        this.flameStat = new EquipFlame();
        this.exceptionalStat = new EquipExceptional();
        this.symbol = new EquipSymbol();
    }

    public Equip deepCopy() {
        Equip ret = new Equip();
        ret.quantity = quantity;
        ret.bagIndex = bagIndex;
        ret.serialNumber = serialNumber;
        ret.title = title;
        ret.equippedDate = equippedDate.deepCopy();
        ret.prevBonusExpRate = prevBonusExpRate;
        ret.tuc = tuc;
        ret.iucMax = iucMax;
        ret.hasIUCMax = hasIUCMax;
        ret.cuc = cuc;
        ret.iStr = iStr;
        ret.iDex = iDex;
        ret.iInt = iInt;
        ret.iLuk = iLuk;
        ret.iMaxHp = iMaxHp;
        ret.iMaxMp = iMaxMp;
        ret.iPad = iPad;
        ret.iMad = iMad;
        ret.iPDD = iPDD;
        ret.iMDD = iMDD;
        ret.iAcc = iAcc;
        ret.iEva = iEva;
        ret.iCraft = iCraft;
        ret.iSpeed = iSpeed;
        ret.iJump = iJump;
        ret.attribute = attribute;
        ret.levelUpType = levelUpType;
        ret.level = level;
        ret.exp = exp;
        ret.durability = durability;
        ret.iuc = iuc;
        ret.iPvpDamage = iPvpDamage;
        ret.iReduceReq = iReduceReq;
        ret.specialAttribute = specialAttribute;
        ret.durabilityMax = durabilityMax;
        ret.iIncReq = iIncReq;
        ret.growthEnchant = growthEnchant;
        ret.psEnchant = psEnchant;
        ret.bdr = bdr;
        ret.imdr = imdr;
        ret.bossReward = bossReward;
        ret.superiorEqp = superiorEqp;
        ret.damR = damR;
        ret.statR = statR;
        ret.cuttable = cuttable;
        ret.exGradeOption = exGradeOption;
        ret.hyperUpgrade = hyperUpgrade;
        ret.itemState = itemState;
        ret.chuc = chuc;
        ret.soulOptionId = soulOptionId;
        ret.soulSocketId = soulSocketId;
        ret.soulOption = soulOption;
        ret.soulItemId = soulItemId;
        ret.rStr = rStr;
        ret.rDex = rDex;
        ret.rInt = rInt;
        ret.rLuk = rLuk;
        ret.rLevel = rLevel;
        ret.rJob = rJob;
        ret.rPop = rPop;
        ret.iSlot = iSlot;
        ret.vSlot = vSlot;
        ret.fixedGrade = fixedGrade;
        ret.options = new ArrayList<>();
        ret.options.addAll(options);
        ret.specialGrade = specialGrade;
        ret.fixedPotential = fixedPotential;
        ret.noPotential = noPotential;
        ret.tradeBlock = tradeBlock;
        ret.only = only;
        ret.notSale = notSale;
        ret.attackSpeed = attackSpeed;
        ret.price = price;
        ret.charmEXP = charmEXP;
        ret.setItemID = setItemID;
        ret.exItem = exItem;
        ret.equipTradeBlock = equipTradeBlock;
        ret.setOwner(getOwner());
        ret.itemId = itemId;
        ret.cashItemSerialNumber = cashItemSerialNumber;
        ret.dateExpire = dateExpire.deepCopy();
        ret.invType = invType;
        ret.type = type;
        ret.isCash = isCash;
        ret.sockets = new ArrayList<>();
        ret.sockets.addAll(sockets);
        ret.dropStreak = dropStreak;
        ret.itemSkills = new ArrayList<>();
        for (ItemSkill skill : itemSkills) {
            ret.itemSkills.add(new ItemSkill(skill.getSkill(), skill.getSlv()));
        }
        ret.symbol = symbol.deepCopy();
        ret.flameStat = flameStat.deepCopy();
        ret.ExceptionalSlot = ExceptionalSlot;
        ret.exceptionalStat = exceptionalStat.deepCopy();
        ret.tradeAvailable = tradeAvailable;
        ret.android = android;
        ret.androidLife = androidLife;
        ret.androidGrade = androidGrade;
        ret.accountSharable = accountSharable;
        ret.sharableOnce = sharableOnce;
        if (net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) {
            ret.enchantStats.putAll(enchantStats);
        }
        return ret;
    }

    public long getSerialNumber() {
        return getId();
    }

    public void setSerialNumber(long serialNumber) {
        this.serialNumber = serialNumber;
    }

    public long getPrimaryKeyItemID() {
        return primaryKeyItemID;
    }

    public void setPrimaryKeyItemID(long primaryKeyItemID) {
        this.primaryKeyItemID = primaryKeyItemID;
    }

    public int getPreset() {
        return preset;
    }

    public void setPreset(int preset) {
        this.preset = preset;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public FileTime getEquippedDate() {
        return equippedDate;
    }

    public void setEquippedDate(FileTime equippedDate) {
        this.equippedDate = equippedDate;
    }

    public int getPrevBonusExpRate() {
        return prevBonusExpRate;
    }

    public void setPrevBonusExpRate(int prevBonusExpRate) {
        this.prevBonusExpRate = prevBonusExpRate;
    }

    // %d Remaining (Scroll Enhancement)
    public short getTuc() {
        return tuc;
    }

    public void setTuc(short tuc) {
        this.tuc = tuc;
    }

    public short getIUCMax() {
        return hasIUCMax ? iucMax : ItemConstants.MAX_HAMMER_SLOTS;
    }

    public void setIUCMax(short iucMax) {
        this.iucMax = iucMax;
    }

    public boolean isHasIUCMax() {
        return hasIUCMax;
    }

    public void setHasIUCMax(boolean hasIUCMax) {
        this.hasIUCMax = hasIUCMax;
    }

    // Scroll Enhancement %d time(s)
    public short getCuc() {
        return cuc;
    }

    public void setCuc(short cuc) {
        this.cuc = cuc;
    }

    public short getiStr() {
        return iStr;
    }

    public void setiStr(short iStr) {
        this.iStr = iStr;
    }

    public short getiDex() {
        return iDex;
    }

    public void setiDex(short iDex) {
        this.iDex = iDex;
    }

    public short getiInt() {
        return iInt;
    }

    public void setiInt(short iInt) {
        this.iInt = iInt;
    }

    public short getiLuk() {
        return iLuk;
    }

    public void setiLuk(short iLuk) {
        this.iLuk = iLuk;
    }

    public short getiMaxHp() {
        return iMaxHp;
    }

    public void setiMaxHp(short iMaxHp) {
        this.iMaxHp = iMaxHp;
    }

    public short getiMaxHpr() {
        switch (getItemId()) {
            case 1012255:
                if (iMaxHpr == 0) {
                    this.iMaxHpr = 2;
                }
                break;
            case 1012256:
                if (iMaxHpr == 0) {
                    this.iMaxHpr = 4;
                }
                break;
            case 1004119:
            case 1122278:
            case 1122309:
            case 1142792:
                if (iMaxHpr == 0) {
                    this.iMaxHpr = 5;
                }
                break;
            case 1012257:
                if (iMaxHpr == 0) {
                    this.iMaxHpr = 6;
                }
                break;
            case 1122077:
            case 1122163:
            case 1122164:
            case 1122165:
            case 1122166:
            case 1122175:
            case 1122176:
            case 1122177:
            case 1122178:
                if (iMaxHpr == 0) {
                    this.iMaxHpr = 7;
                }
                break;
            case 1032077:
            case 1032078:
            case 1032079:
            case 1032102:
            case 1032103:
            case 1032104:
            case 1032196:
            case 1032197:
            case 1032198:
            case 1032199:
            case 1122076:
            case 1122114:
            case 1122150:
            case 1122151:
            case 1122310:
            case 1142788:
            case 1003112:
                if (iMaxHpr == 0) {
                    this.iMaxHpr = 10;
                }
                break;
            case 1122311:
                if (iMaxHpr == 0) {
                    this.iMaxHpr = 15;
                }
                break;
        }
        return iMaxHpr;
    }

    public void setiMaxHpr(short iMaxHpr) {
        this.iMaxHpr = iMaxHpr;
    }

    public short getiMaxMpr() {
        switch (getItemId()) {
            case 1012255:
                if (iMaxMpr == 0) {
                    this.iMaxMpr = 2;
                }
                break;
            case 1012256:
                if (iMaxMpr == 0) {
                    this.iMaxMpr = 4;
                }
                break;
            case 1004119:
            case 1122278:
            case 1122309:
            case 1142792:
                if (iMaxMpr == 0) {
                    this.iMaxMpr = 5;
                }
                break;
            case 1012257:
                if (iMaxMpr == 0) {
                    this.iMaxMpr = 6;
                }
                break;
            case 1122077:
            case 1122163:
            case 1122164:
            case 1122165:
            case 1122166:
            case 1122175:
            case 1122176:
            case 1122177:
            case 1122178:
                if (iMaxMpr == 0) {
                    this.iMaxMpr = 7;
                }
                break;
            case 1032077:
            case 1032078:
            case 1032079:
            case 1032102:
            case 1032103:
            case 1032104:
            case 1032196:
            case 1032197:
            case 1032198:
            case 1032199:
            case 1122076:
            case 1122114:
            case 1122150:
            case 1122151:
            case 1122310:
            case 1142788:
            case 1003112:
                if (iMaxMpr == 0) {
                    this.iMaxMpr = 10;
                }
                break;
            case 1122311:
                if (iMaxMpr == 0) {
                    this.iMaxMpr = 15;
                }
                break;
        }
        return iMaxMpr;
    }

    public void setiMaxMpr(short iMaxMpr) {
        this.iMaxMpr = iMaxMpr;
    }

    public short getiMaxMp() {
        return iMaxMp;
    }

    public void setiMaxMp(short iMaxMp) {
        this.iMaxMp = iMaxMp;
    }

    public short getiPad() {
        return iPad;
    }

    public void setiPad(short iPad) {
        this.iPad = iPad;
    }

    public short getiMad() {
        return iMad;
    }

    public void setiMad(short iMad) {
        this.iMad = iMad;
    }

    public short getiPDD() {
        return iPDD;
    }

    public void setiPDD(short iPDD) {
        this.iPDD = iPDD;
    }

    public short getiMDD() {
        return iMDD;
    }

    public void setiMDD(short iMDD) {
        this.iMDD = iMDD;
    }

    public short getiAcc() {
        return iAcc;
    }

    public void setiAcc(short iAcc) {
        this.iAcc = iAcc;
    }

    public short getiEva() {
        return iEva;
    }

    public void setiEva(short iEva) {
        this.iEva = iEva;
    }

    public short getiCraft() {
        return iCraft;
    }

    public void setiCraft(short iCraft) {
        this.iCraft = iCraft;
    }

    public short getiSpeed() {
        return iSpeed;
    }

    public void setiSpeed(short iSpeed) {
        this.iSpeed = iSpeed;
    }

    public short getiJump() {
        return iJump;
    }

    public void setiJump(short iJump) {
        this.iJump = iJump;
    }

    public int getAttribute() {
        return attribute;
    }

    public void setAttribute(int attribute) {
        this.attribute = attribute;
    }

    public void addAttribute(EquipAttribute ea) {
        int v = ea.getVal();
        if (v <= 0) return;
        attribute |= v;
    }

    public boolean hasAttribute(EquipAttribute ea) {
        int v = ea.getVal();
        return v > 0 && (attribute & v) == v;
    }

    public void removeAttribute(EquipAttribute ea) {
        int v = ea.getVal();
        if (v <= 0) return;
        attribute &= ~v;
    }

    public short getLevelUpType() {
        return levelUpType;
    }

    public void setLevelUpType(short levelUpType) {
        this.levelUpType = levelUpType;
    }

    public short getItemLevel() {
        return level;
    }

    public void setItemLevel(short level) {
        this.level = level;
    }

    public long getItemEXP() {
        if (level == 0) level = 1;
        long expNibble = GameConstants.charExp[rLevel] * exp / GameConstants.itemExp[level];
        if (expNibble > 0) {
            return expNibble;
        }
        return exp;
    }

    public void setItemEXP(short exp) {
        this.exp = exp;
    }

    public short getDurability() {
        return durability;
    }

    public void setDurability(short durability) {
        this.durability = durability;
    }

    public short getIuc() {
        return iuc;
    }

    public void setIuc(short iuc) {
        this.iuc = iuc;
    }

    public short getiPvpDamage() {
        return iPvpDamage;
    }

    public void setiPvpDamage(short iPvpDamage) {
        this.iPvpDamage = iPvpDamage;
    }

    public byte getiReduceReq() {
        return iReduceReq;
    }

    public void setiReduceReq(short iReduceReq) {
        this.iReduceReq = (byte) iReduceReq;
    }

    public int getSpecialAttribute() {
        return specialAttribute;
    }

    public void setSpecialAttribute(int specialAttribute) {
        this.specialAttribute = specialAttribute;
    }

    public boolean hasSpecialAttribute(EquipSpecialAttribute esa) {
        int v = esa.getVal();
        return v > 0 && (specialAttribute & v) == v;
    }

    public void addSpecialAttribute(EquipSpecialAttribute esa) {
        int v = esa.getVal();
        if (v <= 0) return;
        specialAttribute |= v;
    }

    public void removeSpecialAttribute(EquipSpecialAttribute esa) {
        int v = esa.getVal();
        if (v <= 0) return;
        specialAttribute &= ~v;
    }

    public long getExGradeOption() {
        return exGradeOption;
    }

    public void setExGradeOption(long exGradeOption) {
        this.exGradeOption = exGradeOption;
    }

    public short getCuttable() {
        return cuttable;
    }

    public void setCuttable(short cuttable) {
        this.cuttable = cuttable;
    }

    public short getStatR() {
        return statR;
    }

    public void setStatR(short statR) {
        this.statR = statR;
    }

    public short getDamR() {
        return damR;
    }

    public void setDamR(short damR) {
        this.damR = damR;
    }

    public short getImdr() {
        return imdr;
    }

    public void setImdr(short imdr) {
        this.imdr = imdr;
    }

    public boolean isBossReward() {
        return bossReward;
    }

    public void setBossReward(boolean bossReward) {
        this.bossReward = bossReward;
    }

    public boolean isSuperiorEqp() {
        return superiorEqp;
    }

    public void setSuperiorEqp(boolean superiorEqp) {
        this.superiorEqp = superiorEqp;
    }

    public short getBdr() {
        return bdr;
    }

    public void setBdr(short bdr) {
        this.bdr = bdr;
    }

    public short getPsEnchant() {
        return psEnchant;
    }

    public void setPsEnchant(short psEnchant) {
        this.psEnchant = psEnchant;
    }

    public short getGrowthEnchant() {
        return growthEnchant;
    }

    public void setGrowthEnchant(short growthEnchant) {
        this.growthEnchant = growthEnchant;
    }

    public short getiIncReq() {
        return iIncReq;
    }

    public void setiIncReq(short iIncReq) {
        this.iIncReq = iIncReq;
    }

    public short getDurabilityMax() {
        return durabilityMax;
    }

    public void setDurabilityMax(short durabilityMax) {
        this.durabilityMax = durabilityMax;
    }

    public short getItemState() {
        return itemState;
    }

    public void setItemState(short itemState) {
        this.itemState = itemState;
    }

    public int getHyperUprade() {
        return hyperUpgrade;
    }

    public void setHyperUpgrade(int hyperUpgrade) {
        this.hyperUpgrade = hyperUpgrade;
    }

    public void addHyperUpgrade(int hyperUpgrade) {
        if (hyperUpgrade == 0) return;
        hyperUpgrade |= getHyperUprade();
        setHyperUpgrade(hyperUpgrade);
    }

    public boolean hasHyperUpgrade(int hyperUpgrade) {
        return hyperUpgrade != 0 && (getHyperUprade() & hyperUpgrade) == hyperUpgrade;
    }

    public void removeHyperUpgrade(int hyperUpgrade) {
        if (hyperUpgrade == 0) return;
        setHyperUpgrade(getHyperUprade() & ~hyperUpgrade);
    }

    public short getGrade() {
        ItemGrade bonusGrade = ItemGrade.getGradeByVal(getBonusGrade());
        if (bonusGrade.isHidden()) {
            return ItemGrade.getHiddenBonusGradeByBaseGrade(ItemGrade.getGradeByVal(getBaseGrade())).getVal();
        }
        return getBaseGrade();
    }

    public short getBaseGrade() {
        return ItemGrade.getGradeByOption(getOptionBase(0)).getVal();
    }

    public short getBonusGrade() {
        return ItemGrade.getGradeByOption(getOptionBonus(0)).getVal();
    }

    public short getChuc() {
        return chuc;
    }

    public void setChuc(short chuc, boolean recalcEnchantmentStats) {
        this.chuc = chuc;
        if (recalcEnchantmentStats) {
            recalcEnchantmentStats();
        }
    }

    public short getSoulOptionId() {
        return soulOptionId;
    }

    public void setSoulOptionId(short soulOptionId) {
        this.soulOptionId = soulOptionId;
    }

    public short getSoulSocketId() {
        return soulSocketId;
    }

    public void setSoulSocketId(short soulSocketId) {
        this.soulSocketId = soulSocketId;
    }

    public short getSoulOption() {
        return soulOption;
    }

    public void setSoulOption(short soulOption) {
        this.soulOption = soulOption;
    }

    public int getSoulItemId() {
        return soulItemId;
    }

    public void setSoulItemId(int soulItemId) {
        this.soulItemId = soulItemId;
    }

    public short getrPop() {
        return rPop;
    }

    public void setrPop(short rPop) {
        this.rPop = rPop;
    }

    public short getrJob() {
        return rJob;
    }

    public void setrJob(short rJob) {
        this.rJob = rJob;
    }

    public short getrLevel() {
        return rLevel;
    }

    public void setrLevel(short rLevel) {
        this.rLevel = rLevel;
    }

    public short getrLuk() {
        return rLuk;
    }

    public void setrLuk(short rLuk) {
        this.rLuk = rLuk;
    }

    public short getrInt() {
        return rInt;
    }

    public void setrInt(short rInt) {
        this.rInt = rInt;
    }

    public short getrDex() {
        return rDex;
    }

    public void setrDex(short rDex) {
        this.rDex = rDex;
    }

    public short getrStr() {
        return rStr;
    }

    public void setrStr(short rStr) {
        this.rStr = rStr;
    }

    public List<Integer> getOptions() {
        return options;
    }

    public void setOptions(List<Integer> options) {
        this.options = options;
    }

    public String getiSlot() {
        return iSlot;
    }

    public void setiSlot(String iSlot) {
        this.iSlot = iSlot;
    }

    public String getvSlot() {
        return vSlot;
    }

    public void setvSlot(String vSlot) {
        this.vSlot = vSlot;
    }

    public int getSpecialGrade() {
        return specialGrade;
    }

    public void setSpecialGrade(int specialGrade) {
        this.specialGrade = specialGrade;
    }

    public boolean isFixedPotential() {
        return fixedPotential;
    }

    public void setFixedPotential(boolean fixedPotential) {
        this.fixedPotential = fixedPotential;
    }

    public boolean isNoPotential() {
        return noPotential;
    }

    public void setNoPotential(boolean noPotential) {
        this.noPotential = noPotential;
    }

    public boolean isTradeBlock() {
        return tradeBlock;
    }

    public void setTradeBlock(boolean tradeBlock) {
        this.tradeBlock = tradeBlock;
    }

    public boolean isOnly() {
        return only;
    }

    public void setOnly(boolean only) {
        this.only = only;
    }

    public boolean isNotSale() {
        return notSale;
    }

    public void setNotSale(boolean notSale) {
        this.notSale = notSale;
    }

    public int getAttackSpeed() {
        return attackSpeed;
    }

    public void setAttackSpeed(int attackSpeed) {
        this.attackSpeed = attackSpeed;
    }

    public long getPrice() {
        return price;
    }

    public void setPrice(long price) {
        this.price = price;
    }

    public int getCharmEXP() {
        return charmEXP;
    }

    public void setCharmEXP(int charmEXP) {
        this.charmEXP = charmEXP;
    }

    public int getSetItemID() {
        return setItemID;
    }

    public void setSetItemID(int setItemID) {
        this.setItemID = setItemID;
    }

    public int getFixedGrade() {
        return fixedGrade;
    }

    public void setFixedGrade(int fixedGrade) {
        this.fixedGrade = fixedGrade;
    }

    public boolean isExItem() {
        return exItem;
    }

    public void setExItem(boolean exItem) {
        this.exItem = exItem;
    }

    public boolean isEquipTradeBlock() {
        return equipTradeBlock;
    }

    public void setEquipTradeBlock(boolean equipTradeBlock) {
        this.equipTradeBlock = equipTradeBlock;
    }

    public int getDropStreak() {
        return dropStreak;
    }

    public void setDropStreak(int dropStreak) {
        this.dropStreak = dropStreak;
    }

    public List<ItemSkill> getItemSkills() {
        return itemSkills;
    }

    public void setItemSkills(List<ItemSkill> itemSkills) {
        this.itemSkills = itemSkills;
    }

    public void addItemSkill(ItemSkill itemSkill) {
        this.itemSkills.add(itemSkill);
    }

    public void removeItemSkill(ItemSkill is) {
        getItemSkills().removeIf(x -> x.getSkill() == is.getSkill() && x.getSlv() == is.getSlv());
    }

    public EquipFlame getFlameStat() {
        return flameStat;
    }

    public void setFlameStat(EquipFlame flameStat) {
        this.flameStat = flameStat;
    }

    public byte getExceptionalSlot() {
        return this.ExceptionalSlot;
    }

    public void addExceptionalSlot(byte a) {
        this.ExceptionalSlot += a;
    }

    public void setExceptionalSlot(byte value) {
        this.ExceptionalSlot = value;
    }

    public EquipExceptional getExceptionalStat() {
        return exceptionalStat;
    }

    public void setExceptionalStat(EquipExceptional exceptionalStat) {
        this.exceptionalStat = exceptionalStat;
    }

    public EquipSymbol getSymbol() {
        symbol.setSacred(ItemConstants.isSacredSymbol(getItemId()));
        return symbol;
    }

    public void setSymbol(EquipSymbol symbol) {
        this.symbol = symbol;
    }

    public int getTradeAvailable() {
        return tradeAvailable;
    }

    public void setTradeAvailable(int tradeAvailable) {
        this.tradeAvailable = tradeAvailable;
    }

    public boolean getAccountSharable() {
        return accountSharable;
    }

    public void setAccountSharable(boolean accountSharable) {
        this.accountSharable = accountSharable;
    }

    public boolean getSharableOnce() {
        return sharableOnce;
    }

    public void setSharableOnce(boolean sharableOnce) {
        this.sharableOnce = sharableOnce;
    }

    public boolean isLastScrollFail() {
        return isLastScrollFail;
    }

    public void setLastScrollFail(boolean fail) {
        this.isLastScrollFail = fail;
    }

    public boolean isTalented() {
        return isTalented;
    }

    public void setTalented(boolean talented) {
        isTalented = talented;
    }

    public int getTalentProgress() {
        return talentProgress;
    }

    public void setTalentProgress(int talentProgress) {
        this.talentProgress = talentProgress;
    }

    public boolean isTradeDisableWhenEquip() {
        return ItemData.getEquipById(getItemId()) != null && ItemData.getEquipById(getItemId()).getTradeAvailable() == 2;
    }

    public boolean isAccountSharable() {
        return ItemData.getEquipById(getItemId()) != null && ItemData.getEquipById(getItemId()).getAccountSharable();
    }

    public boolean isSharableOnce() {
        return ItemData.getEquipById(getItemId()) != null && ItemData.getEquipById(getItemId()).getSharableOnce();
    }

    public void encode(OutPacket outPacket) {
        // GW_ItemSlotBase
        super.encode(outPacket);
        // GW_ItemSlotEquipBase
        encodeEquipBase(outPacket);
        // GW_ItemSlotEquipOpt
        outPacket.encodeString(getOwner(), 13);
        outPacket.encodeByte(getGrade());
        outPacket.encodeByte(getChuc());
        for (int i = 0; i < 7; i++) {
            outPacket.encodeShort(getOptions().get(i)); // 7x, last is fusion anvil
        }
        outPacket.encodeShort(getSocketMask()); // socket state, 0 = nothing, 0xFF = see loop
        for (int i = 0; i < 3; i++) {
            outPacket.encodeShort(getSocket(i)); // sockets 0 through 2 (-1 = none, 0 = empty, >0 = filled
        }
        if (!isCash()) {
            outPacket.encodeLong(getId());
        }
        outPacket.encodeFT(FileTime.MAX_TIME()); // ftEquipped
        // @start GW_CashItemOption::Decode
        outPacket.encodeLong(0); // cash sn already encoded in the super's encode
        outPacket.encodeFT(FileTime.fromDate(getDateExpire().toLocalDateTime()));
        outPacket.encodeInt(getGrade());
        for (int i = 0; i < 3; i++) {
            outPacket.encodeInt(getOptionBase(i));
        }
        // @end GW_CashItemOption::Decode
        // sub_140657660
        outPacket.encodeLong(0);
        outPacket.encodeInt(0);

        outPacket.encodeInt(0);
        // sub_1406577D0
        int size = 0;
        outPacket.encodeInt(size); // size?
        for (int i = 0; i < size; i++) {
            outPacket.encodeLong(0);
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);
        }
        outPacket.encodeShort(getSoulOptionId()); // soul ID
        outPacket.encodeShort(getSoulSocketId()); // enchanter ID
        outPacket.encodeShort(getSoulOption()); // optionID (same as potentials)
        if (ItemConstants.isSymbol(getItemId())) {
            outPacket.encodeShort(getSymbol().getInc()); // inc
            outPacket.encodeInt(getSymbol().getExp()); // exp
            outPacket.encodeShort(getSymbol().getLevel()); // Arcane Level
        }
        outPacket.encodeShort(-1);
        outPacket.encodeFT(FileTime.MAX_TIME());
        outPacket.encodeFT(FileTime.MIN_TIME());
        outPacket.encodeFT(FileTime.MAX_TIME());
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
        if (ItemConstants.isAndroid(getItemId())) {
            if (getAndroidLife() != null) {
                getAndroidLife().encodeAndroidInfo(outPacket);
            } else {
                new Android(getItemId()).encodeAndroidInfo(outPacket);
            }
        }
        // sub_14069B190
        outPacket.encodeByte(getExceptionalSlot());
        outPacket.encodeByte(false);

        // Exceptional Enhancement
        int exceptionalMask = getStatMask(0, 2);
        encodeEquipCalcStat(outPacket, exceptionalMask, 2);

        int enhanceMask = getStatMask(0, 1);
        outPacket.encodeByte(enhanceMask != 0);
        if (enhanceMask != 0) {
            encodeEquipCalcStat(outPacket, enhanceMask, 1);
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Paste: <mask> <pos>. Example: 207 0   |   132866 1   |   exit");

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String line;
        while ((line = br.readLine()) != null) {
            String t = line.trim();
            if (t.equalsIgnoreCase("exit")) break;
            if (t.isEmpty()) continue;

            String[] p = t.split("\\s+");
            if (p.length < 2) {
                System.out.println("Need: <mask> <pos> (pos=0 or 1)");
                continue;
            }

            int mask = parseMask(p[0]);
            int pos = Integer.parseInt(p[1]);

            System.out.println("mask=" + mask + " (0x" + Integer.toHexString(mask).toUpperCase() + "), pos=" + pos);
            for (EquipBaseStat s : EquipBaseStat.values()) {
                if (s.getPos() != pos) continue;      // chỉ xét đúng pos (0 hoặc 1)
                int v = s.getVal();
                if (v <= 0) continue;                 // bỏ -1 (removed)
                if ((mask & v) != 0) System.out.println(" - " + s.name());
            }
        }
    }

    private static int parseMask(String s) {
        s = s.replace("_", "").trim();
        if (s.startsWith("0x") || s.startsWith("0X")) return (int) Long.parseLong(s.substring(2), 16);
        return (int) Long.parseLong(s, 10);
    }

    public void encodeEquipBase(OutPacket outPacket) {
        // GW_ItemSlotEquipBase__Encode
        int baseMask = getStatMask(0, 0);
        encodeEquipCalcStat(outPacket, baseMask, 0);
        int mask = getStatMask(1);
        outPacket.encodeInt(mask);
        if (hasStat(EquipBaseStat.tuc)) {
            outPacket.encodeByte(getTuc());
        }
        if (hasStat(EquipBaseStat.cuc)) {
            outPacket.encodeByte(getCuc());
        }
        if (hasStat(EquipBaseStat.attribute)) {
            outPacket.encodeShort(getAttribute());
        }
        if (hasStat(EquipBaseStat.levelUpType)) {
            outPacket.encodeByte(getLevelUpType());
        }
        if (hasStat(EquipBaseStat.level)) {
            outPacket.encodeByte(getItemLevel());
        }
        if (hasStat(EquipBaseStat.exp)) {
            outPacket.encodeLong(getItemEXP());
        }
        if (hasStat(EquipBaseStat.durability)) {
            outPacket.encodeInt(getDurability());
        }
        if (hasStat(EquipBaseStat.iuc)) {
            outPacket.encodeInt(getIuc()); // Gold Hammer
        }
        if (hasStat(EquipBaseStat.iReduceReq)) {
            outPacket.encodeByte(getTotalStat(EquipBaseStat.iReduceReq));
        }
        if (hasStat(EquipBaseStat.specialAttribute)) {
            outPacket.encodeShort(getSpecialAttribute());
        }
        if (hasStat(EquipBaseStat.durabilityMax)) {
            outPacket.encodeInt(getDurabilityMax());
        }
        if (hasStat(EquipBaseStat.iIncReq)) {
            outPacket.encodeByte(getiIncReq());
        }
        if (hasStat(EquipBaseStat.growthEnchant)) {
            outPacket.encodeByte(getGrowthEnchant()); // ygg
        }
        if (hasStat(EquipBaseStat.psEnchant)) {
            outPacket.encodeByte(getPsEnchant()); // final strike
        }
        if (hasStat(EquipBaseStat.bdr)) {
            outPacket.encodeByte(getTotalStat(EquipBaseStat.bdr)); // bd
        }
        if (hasStat(EquipBaseStat.imdr)) {
            outPacket.encodeByte(getTotalStat(EquipBaseStat.imdr)); // ied
        }
        if (hasStat(EquipBaseStat.damR)) {
            outPacket.encodeByte(getTotalStat(EquipBaseStat.damR)); // td
        }
        if (hasStat(EquipBaseStat.statR)) {
            outPacket.encodeByte(getTotalStat(EquipBaseStat.statR)); // as
        }
        if (hasStat(EquipBaseStat.cuttable)) {
            outPacket.encodeByte(getCuttable()); // sok
        }
        if (hasStat(EquipBaseStat.exGradeOption)) {
            outPacket.encodeLong(getExGradeOption());
        }
        if (hasStat(EquipBaseStat.hyperUpgrade)) {
            outPacket.encodeInt(getHyperUprade());
        }
    }

    public void encodeEquipCalcStat(OutPacket outPacket, int mask, int type) {
        outPacket.encodeInt(mask);
        boolean isTotalMask = type == 0;
        boolean isEnchantMask = type == 1;
        boolean isExceptionalMask = type == 2;
        if (hasStat(EquipBaseStat.iStr, type)) {
            outPacket.encodeShort(isTotalMask ? getTotalEnhanceStat(EquipBaseStat.iStr) : (isExceptionalMask ? getExceptionalStat().getSTR(): getEnchantStat(EnchantStat.STR)));
        }
        if (hasStat(EquipBaseStat.iDex, type)) {
            outPacket.encodeShort(isTotalMask ? getTotalEnhanceStat(EquipBaseStat.iDex) : (isExceptionalMask ? getExceptionalStat().getDEX() : getEnchantStat(EnchantStat.DEX)));
        }
        if (hasStat(EquipBaseStat.iInt, type)) {
            outPacket.encodeShort(isTotalMask ? getTotalEnhanceStat(EquipBaseStat.iInt) : (isExceptionalMask ? getExceptionalStat().getDEX() : getEnchantStat(EnchantStat.INT)));
        }
        if (hasStat(EquipBaseStat.iLuk, type)) {
            outPacket.encodeShort(isTotalMask ? getTotalEnhanceStat(EquipBaseStat.iLuk) : (isExceptionalMask ? getExceptionalStat().getLUK() : getEnchantStat(EnchantStat.LUK)));
        }
        if (hasStat(EquipBaseStat.iMaxHP, type)) {
            outPacket.encodeShort(isTotalMask ? getTotalEnhanceStat(EquipBaseStat.iMaxHP) : (isExceptionalMask ? getExceptionalStat().getHP() : getEnchantStat(EnchantStat.MHP)));
        }
        if (hasStat(EquipBaseStat.iMaxMP, type)) {
            outPacket.encodeShort(isTotalMask ? getTotalEnhanceStat(EquipBaseStat.iMaxMP) : (isExceptionalMask ? getExceptionalStat().getMP() : getEnchantStat(EnchantStat.MMP)));
        }
        if (hasStat(EquipBaseStat.iPAD, type)) {
            outPacket.encodeShort(isTotalMask ? getTotalEnhanceStat(EquipBaseStat.iPAD) : (isExceptionalMask ? getExceptionalStat().getPAD() : getEnchantStat(EnchantStat.PAD)));
        }
        if (hasStat(EquipBaseStat.iMAD, type)) {
            outPacket.encodeShort(isTotalMask ? getTotalEnhanceStat(EquipBaseStat.iMAD) : (isExceptionalMask ? getExceptionalStat().getMAD() : getEnchantStat(EnchantStat.MAD)));
        }
        if (hasStat(EquipBaseStat.iPDD, type)) {
            outPacket.encodeShort(isTotalMask ? getTotalEnhanceStat(EquipBaseStat.iPDD) : (isExceptionalMask ? getExceptionalStat().getPDD() : getEnchantStat(EnchantStat.PDD)));
        }
        if (hasStat(EquipBaseStat.iMDD, type)) {
            outPacket.encodeShort(isTotalMask ? getTotalEnhanceStat(EquipBaseStat.iMDD) : (isExceptionalMask ? getExceptionalStat().getPDD() : getEnchantStat(EnchantStat.MDD)));
        }
        if (hasStat(EquipBaseStat.iCraft, type)) {
            outPacket.encodeShort(isTotalMask ? getTotalEnhanceStat(EquipBaseStat.iCraft) : 0);
        }
        if (hasStat(EquipBaseStat.iSpeed, type)) {
            outPacket.encodeShort(isTotalMask ? getTotalEnhanceStat(EquipBaseStat.iSpeed) : (isExceptionalMask ? getExceptionalStat().getSpeed() : getEnchantStat(EnchantStat.SPEED)));
        }
        if (hasStat(EquipBaseStat.iJump, type)) {
            outPacket.encodeShort(isTotalMask ? getTotalEnhanceStat(EquipBaseStat.iJump): (isExceptionalMask ? getExceptionalStat().getJump() : getEnchantStat(EnchantStat.JUMP)));
        }
    }

    public int getTotalEnhanceStat(EquipBaseStat stat) {
        double isTalented = isTalented() ? 2 : 1;
        return switch (stat) {
            case iStr -> (int) ((getiStr()
                    + getFlameStat().getSTR()
                    + getEnchantStat(EnchantStat.STR)
                    + getSocketStat(ScrollStat.incSTR)) * isTalented);
            case iDex -> (int) ((getiDex()
                    + getFlameStat().getDEX()
                    + getEnchantStat(EnchantStat.DEX)
                    + getSocketStat(ScrollStat.incDEX)) * isTalented);
            case iInt -> (int) ((getiInt()
                    + getFlameStat().getINT()
                    + getEnchantStat(EnchantStat.INT)
                    + getSocketStat(ScrollStat.incINT)) * isTalented);
            case iLuk -> (int) ((getiLuk()
                    + getFlameStat().getLUK()
                    + getEnchantStat(EnchantStat.LUK)
                    + getSocketStat(ScrollStat.incLUK)) * isTalented);
            case iMaxHP -> (int) ((getBaseStat(EquipBaseStat.iMaxHP)
                    + getFlameStat().getHP()
                    + getEnchantStat(EnchantStat.MHP)
                    + getSocketStat(ScrollStat.incMHP)) * isTalented);
            case iMaxMP -> (int) ((getiMaxMp()
                    + getFlameStat().getMP()
                    + getEnchantStat(EnchantStat.MMP)
                    + getSocketStat(ScrollStat.incMMP)) * isTalented);
            case iPAD -> (int) ((getiPad()
                    + getFlameStat().getPAD()
                    + getEnchantStat(EnchantStat.PAD)
                    + getSocketStat(ScrollStat.incPAD)) * isTalented);
            case iMAD -> (int) ((getiMad()
                    + getFlameStat().getMAD()
                    + getEnchantStat(EnchantStat.MAD)
                    + getSocketStat(ScrollStat.incMAD)) * isTalented);
            case iPDD -> (int) ((getiPDD()
                    + getFlameStat().getPDD()
                    + getEnchantStat(EnchantStat.PDD)
                    + getSocketStat(ScrollStat.incPDD)) * isTalented);
            case iMDD -> (int) ((getiMDD()
                    + getFlameStat().getPDD()
                    + getEnchantStat(EnchantStat.MDD)
                    + getSocketStat(ScrollStat.incMDD)) * isTalented);
            case iACC -> getiAcc()
                    + getEnchantStat(EnchantStat.ACC)
                    + getSocketStat(ScrollStat.incACC);
            case iEVA -> getiEva()
                    + getEnchantStat(EnchantStat.EVA)
                    + getSocketStat(ScrollStat.incEVA);
            case iCraft -> getiCraft();
            case iSpeed -> (int) ((getiSpeed()
                    + getFlameStat().getSpeed()
                    + getEnchantStat(EnchantStat.SPEED)
                    + getSocketStat(ScrollStat.incSpeed)) * isTalented);
            case iJump -> (int) ((getiSpeed()
                    + getFlameStat().getJump()
                    + getEnchantStat(EnchantStat.JUMP)
                    + getSocketStat(ScrollStat.incJump)) * isTalented);
            default -> 0;
        };
    }

    public int getTotalStat(EquipBaseStat stat) {
        double isTalented = isTalented() ? 2 : 1;
        return switch (stat) {
            case tuc -> getTuc();
            case cuc -> getCuc();
            case iStr -> (int) ((getiStr()
                    + getFlameStat().getSTR()
                    + getExceptionalStat().getSTR()
                    + getEnchantStat(EnchantStat.STR)
                    + getSocketStat(ScrollStat.incSTR)) * isTalented);
            case iDex -> (int) ((getiDex()
                    + getFlameStat().getDEX()
                    + getExceptionalStat().getDEX()
                    + getEnchantStat(EnchantStat.DEX)
                    + getSocketStat(ScrollStat.incDEX)) * isTalented);
            case iInt -> (int) ((getiInt()
                    + getFlameStat().getINT()
                    + getExceptionalStat().getINT()
                    + getEnchantStat(EnchantStat.INT)
                    + getSocketStat(ScrollStat.incINT)) * isTalented);
            case iLuk -> (int) ((getiLuk()
                    + getFlameStat().getLUK()
                    + getExceptionalStat().getLUK()
                    + getEnchantStat(EnchantStat.LUK)
                    + getSocketStat(ScrollStat.incLUK)) * isTalented);
            case iMaxHP -> (int) ((getBaseStat(EquipBaseStat.iMaxHP)
                    + getFlameStat().getHP()
                    + getExceptionalStat().getHP()
                    + getEnchantStat(EnchantStat.MHP)
                    + getSocketStat(ScrollStat.incMHP)) * isTalented);
            case iMaxMP -> (int) ((getiMaxMp()
                    + getFlameStat().getMP()
                    + getExceptionalStat().getMP()
                    + getEnchantStat(EnchantStat.MMP)
                    + getSocketStat(ScrollStat.incMMP)) * isTalented);
            case iPAD -> (int) ((getiPad()
                    + getFlameStat().getPAD()
                    + getExceptionalStat().getPAD()
                    + getEnchantStat(EnchantStat.PAD)
                    + getSocketStat(ScrollStat.incPAD)) * isTalented);
            case iMAD -> (int) ((getiMad()
                    + getFlameStat().getMAD()
                    + getExceptionalStat().getMAD()
                    + getEnchantStat(EnchantStat.MAD)
                    + getSocketStat(ScrollStat.incMAD)) * isTalented);
            case iPDD -> (int) ((getiPDD()
                    + getFlameStat().getPDD()
                    + getExceptionalStat().getPDD()
                    + getEnchantStat(EnchantStat.PDD)
                    + getSocketStat(ScrollStat.incPDD)) * isTalented);
            case iMDD -> (int) ((getiMDD()
                    + getFlameStat().getPDD()
                    + getExceptionalStat().getPDD()
                    + getEnchantStat(EnchantStat.MDD)
                    + getSocketStat(ScrollStat.incMDD)) * isTalented);
            case iACC -> getiAcc()
                    + getEnchantStat(EnchantStat.ACC)
                    + getSocketStat(ScrollStat.incACC);
            case iEVA -> getiEva()
                    + getEnchantStat(EnchantStat.EVA)
                    + getSocketStat(ScrollStat.incEVA);
            case iCraft -> getiCraft();
            case iSpeed -> (int) ((getiSpeed()
                    + getFlameStat().getSpeed()
                    + getExceptionalStat().getSpeed()
                    + getEnchantStat(EnchantStat.SPEED)
                    + getSocketStat(ScrollStat.incSpeed)) * isTalented);
            case iJump -> (int) ((getiJump()
                    + getFlameStat().getJump()
                    + getExceptionalStat().getJump()
                    + getEnchantStat(EnchantStat.JUMP)
                    + getSocketStat(ScrollStat.incJump)) * isTalented);
            case attribute -> getAttribute();
            case levelUpType -> getLevelUpType();
            case level -> getItemLevel();
            case durability -> getDurability();
            case iuc -> getIuc(); // hammer
            case iPvpDamage -> getiPvpDamage();
            case iReduceReq -> (byte) (getiReduceReq() + getFlameStat().getReduceReqLevel() + getExceptionalStat().getReduceReqLevel());
            case specialAttribute -> getSpecialAttribute();
            case durabilityMax -> getDurabilityMax();
            case iIncReq -> getiIncReq();
            case growthEnchant -> getGrowthEnchant(); // ygg
            case psEnchant -> getPsEnchant(); // final strike
            case bdr -> (byte) ((getBdr()
                    + getFlameStat().getBossDamageR()
                    + getExceptionalStat().getBossDamageR()
                    + getSocketStat(ScrollStat.boss)) * isTalented); // bd
            case imdr -> getImdr()
                    + getSocketStat(ScrollStat.ignoreTargetDEF); // ied
            case damR -> (byte) ((getDamR()
                    + getFlameStat().getDamage()
                    + getExceptionalStat().getDamage()
                    + getSocketStat(ScrollStat.incDAMr)) * isTalented); // td
            case statR -> (byte) ((getStatR()
                    + getFlameStat().getAllStatR()
                    + getExceptionalStat().getAllStatR()) * isTalented); // as
            case cuttable -> getCuttable(); // sok
            case hyperUpgrade -> getHyperUprade();
            default -> 0;
        };
    }

    private boolean hasStat(EquipBaseStat ebs) {
        return getTotalStat(ebs) != 0
                || getBaseStatFlame(ebs) != 0
                || getBaseStatExceptional(ebs) != 0
                || getEnchantmentStat(ebs) != 0
                || (getExGradeOption() != 0 && ebs.getVal() == EquipBaseStat.exGradeOption.getVal());
    }


    private boolean hasStat(EquipBaseStat ebs, int type) {
        if (type == 0) {
            return getTotalEnhanceStat(ebs) != 0;
        } else if (type == 1) {
            return getEnchantmentStat(ebs) != 0;
        } else if (type == 2) {
            return getBaseStatExceptional(ebs) != 0;
        }
        return false;
    }

    private int getStatMask(int pos) {
        int mask = 0;
        for (EquipBaseStat ebs : EquipBaseStat.values()) {
            if (hasStat(ebs) && ebs.getPos() == pos) {
                if (ebs.getVal() != -1) {
                    mask |= ebs.getVal();
                }
            }
        }
        return mask;
    }

    private int getStatMask(int pos, int type) {
        int mask = 0;
        for (EquipBaseStat ebs : EquipBaseStat.values()) {
            if ((hasStat(ebs, type)) && ebs.getPos() == pos) {
                if (ebs.getVal() != -1) {
                    mask |= ebs.getVal();
                }
            }
        }
        return mask;
    }

    public void setBaseStat(EquipBaseStat equipBaseStat, long amount) {
        switch (equipBaseStat) {
            case tuc -> setTuc((short) amount);
            case cuc -> setCuc((short) amount);
            case iStr -> setiStr((short) amount);
            case iDex -> setiDex((short) amount);
            case iInt -> setiInt((short) amount);
            case iLuk -> setiLuk((short) amount);
            case iMaxHP -> setiMaxHp((short) amount);
            case iMaxMP -> setiMaxMp((short) amount);
            case iPAD -> setiPad((short) amount);
            case iMAD -> setiMad((short) amount);
            case iPDD -> setiPDD((short) amount);
            case iMDD -> setiMDD((short) amount);
            case iACC -> setiAcc((short) amount);
            case iEVA -> setiEva((short) amount);
            case iCraft -> setiCraft((short) amount);
            case iSpeed -> setiSpeed((short) amount);
            case iJump -> setiJump((short) amount);
            case attribute -> setAttribute((int) amount);
            case levelUpType -> setLevelUpType((short) amount);
            case level -> setItemLevel((byte) amount);
            case exp -> setItemEXP((short) amount);
            case durability -> setDurability((short) amount);
            case iuc -> setIuc((short) amount);
            case iPvpDamage -> setiPvpDamage((short) amount);
            case iReduceReq -> setiReduceReq((byte) amount);
            case specialAttribute -> setSpecialAttribute((int) amount);
            case durabilityMax -> setDurabilityMax((short) amount);
            case iIncReq -> setiIncReq((short) amount);
            case growthEnchant -> setGrowthEnchant((short) amount);
            case psEnchant -> setPsEnchant((short) amount);
            case bdr -> setBdr((short) amount);
            case imdr -> setImdr((short) amount);
            case damR -> setDamR((short) amount);
            case statR -> setStatR((short) amount);
            case cuttable -> setCuttable((short) amount);
            case exGradeOption -> setExGradeOption(amount);
            case hyperUpgrade -> setHyperUpgrade((int) amount);
        }
    }

    public long getBaseStat(EquipBaseStat equipBaseStat) {
        return switch (equipBaseStat) {
            case tuc -> getTuc();
            case cuc -> getCuc();
            case iStr -> getiStr();
            case iDex -> getiDex();
            case iInt -> getiInt();
            case iLuk -> getiLuk();
            case iMaxHP -> net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3
                ? Short.toUnsignedInt(getiMaxHp()) : getiMaxHp();
            case iMaxMP -> getiMaxMp();
            case iPAD -> getiPad();
            case iMAD -> getiMad();
            case iPDD -> getiPDD();
            case iMDD -> getiMDD();
            case iACC -> getiAcc();
            case iEVA -> getiEva();
            case iCraft -> getiCraft();
            case iSpeed -> getiSpeed();
            case iJump -> getiJump();
            case attribute -> getAttribute();
            case levelUpType -> getLevelUpType();
            case level -> getItemLevel();
            case exp -> getItemEXP();
            case durability -> getDurability();
            case iuc -> getIuc();
            case iPvpDamage -> getiPvpDamage();
            case iReduceReq -> getiReduceReq();
            case specialAttribute -> getSpecialAttribute();
            case durabilityMax -> getDurabilityMax();
            case iIncReq -> getiIncReq();
            case growthEnchant -> getGrowthEnchant();
            case psEnchant -> getPsEnchant();
            case bdr -> getBdr();
            case imdr -> getImdr();
            case damR -> getDamR();
            case statR -> getStatR();
            case cuttable -> getCuttable();
            case exGradeOption -> getExGradeOption();
            case hyperUpgrade -> getHyperUprade();
        };
    }

    public long getBaseStatFlame(EquipBaseStat equipBaseStat) {
        return switch (equipBaseStat) {
            case iStr -> getFlameStat().getSTR();
            case iDex -> getFlameStat().getDEX();
            case iInt -> getFlameStat().getINT();
            case iLuk -> getFlameStat().getLUK();
            case iMaxHP -> getFlameStat().getHP();
            case iMaxMP -> getFlameStat().getMP();
            case iPAD -> getFlameStat().getPAD();
            case iMAD -> getFlameStat().getMAD();
            case iPDD, iMDD -> getFlameStat().getPDD();
            case iSpeed -> getFlameStat().getSpeed();
            case iJump -> getFlameStat().getJump();
            case statR -> getFlameStat().getAllStatR();
            case bdr -> getFlameStat().getBossDamageR();
            case damR -> getFlameStat().getDamage();
            case iReduceReq -> getFlameStat().getReduceReqLevel();
            default -> 0;
        };
    }

    public void setBaseStatFlame(EquipBaseStat equipBaseStat, int amount) {
        switch (equipBaseStat) {
            case iStr -> getFlameStat().setSTR(amount);
            case iDex -> getFlameStat().setDEX(amount);
            case iInt -> getFlameStat().setINT(amount);
            case iLuk -> getFlameStat().setLUK(amount);
            case iMaxHP -> getFlameStat().setHP(amount);
            case iMaxMP -> getFlameStat().setMP(amount);
            case iPAD -> getFlameStat().setPAD(amount);
            case iMAD -> getFlameStat().setMAD(amount);
            case iPDD, iMDD -> getFlameStat().setPDD(amount);
            case iSpeed -> getFlameStat().setSpeed(amount);
            case iJump -> getFlameStat().setJump(amount);
            case statR -> getFlameStat().setAllStatR(amount);
            case bdr -> getFlameStat().setBossDamageR(amount);
            case damR -> getFlameStat().setDamage(amount);
            case iReduceReq -> getFlameStat().setReduceReqLevel(amount);
        }
    }

    public long getBaseStatExceptional(EquipBaseStat equipBaseStat) {
        return switch (equipBaseStat) {
            case iStr -> getExceptionalStat().getSTR();
            case iDex -> getExceptionalStat().getDEX();
            case iInt -> getExceptionalStat().getINT();
            case iLuk -> getExceptionalStat().getLUK();
            case iMaxHP -> getExceptionalStat().getHP();
            case iMaxMP -> getExceptionalStat().getMP();
            case iPAD -> getExceptionalStat().getPAD();
            case iMAD -> getExceptionalStat().getMAD();
            case iPDD, iMDD -> getExceptionalStat().getPDD();
            case iSpeed -> getExceptionalStat().getSpeed();
            case iJump -> getExceptionalStat().getJump();
            case statR -> getExceptionalStat().getAllStatR();
            case bdr -> getExceptionalStat().getBossDamageR();
            case damR -> getExceptionalStat().getDamage();
            case iReduceReq -> getExceptionalStat().getReduceReqLevel();
            default -> 0;
        };
    }

    public void setBaseStatExceptional(EquipBaseStat equipBaseStat, int amount) {
        switch (equipBaseStat) {
            case iStr -> getExceptionalStat().setSTR(amount);
            case iDex -> getExceptionalStat().setDEX(amount);
            case iInt -> getExceptionalStat().setINT(amount);
            case iLuk -> getExceptionalStat().setLUK(amount);
            case iMaxHP -> getExceptionalStat().setHP(amount);
            case iMaxMP -> getExceptionalStat().setMP(amount);
            case iPAD -> getExceptionalStat().setPAD(amount);
            case iMAD -> getExceptionalStat().setMAD(amount);
            case iPDD, iMDD -> getExceptionalStat().setPDD(amount);
            case iSpeed -> getExceptionalStat().setSpeed(amount);
            case iJump -> getExceptionalStat().setJump(amount);
            case statR -> getExceptionalStat().setAllStatR(amount);
            case bdr -> getExceptionalStat().setBossDamageR(amount);
            case damR -> getExceptionalStat().setDamage(amount);
            case iReduceReq -> getExceptionalStat().setReduceReqLevel(amount);
        }
    }

    public long getEnchantmentStat(EquipBaseStat equipBaseStat) {
        return switch (equipBaseStat) {
            case iStr -> getEnchantStat(EnchantStat.STR);
            case iDex -> getEnchantStat(EnchantStat.DEX);
            case iInt -> getEnchantStat(EnchantStat.INT);
            case iLuk -> getEnchantStat(EnchantStat.LUK);
            case iMaxHP -> getEnchantStat(EnchantStat.MHP);
            case iMaxMP -> getEnchantStat(EnchantStat.MMP);
            case iPAD -> getEnchantStat(EnchantStat.PAD);
            case iMAD -> getEnchantStat(EnchantStat.MAD);
            case iPDD -> getEnchantStat(EnchantStat.PDD);
            case iMDD -> getEnchantStat(EnchantStat.MDD);
            case iSpeed -> getEnchantStat(EnchantStat.SPEED);
            case iJump -> getEnchantStat(EnchantStat.JUMP);
            default -> 0;
        };
    }

    public void addStat(EquipBaseStat stat, int amount) {
        int cur = (int) getBaseStat(stat);
        int newStat = Math.max(cur + amount, 0); // stat cannot be negative
        setBaseStat(stat, newStat);
    }

    public TreeMap<EnchantStat, Integer> getHyperUpgradeStats() {
        Comparator<EnchantStat> comparator = Comparator.comparingInt(EnchantStat::getVal);
        TreeMap<EnchantStat, Integer> res = new TreeMap<>(comparator);
        for (EnchantStat es : EnchantStat.values()) {
            assert es.getEquipBaseStat() != null;
            int curAmount = (int) getBaseStat(es.getEquipBaseStat());
            if (curAmount > 0 || (es == EnchantStat.PAD || es == EnchantStat.MAD || es == EnchantStat.PDD || es == EnchantStat.MDD)) {
                res.put(es, GameConstants.getEnchantmentValByChuc(this, es, getChuc(), curAmount));
            }
        }
        return res;
    }

    public boolean hasPotential() {
        return getOptions().get(0) != 0 || getOptions().get(3) != 0;
    }

    public int[] getOptionBase() {
        return new int[]{getOptions().get(0), getOptions().get(1), getOptions().get(2)};
    }

    public int getOptionBase(int num) {
        return getOptions().get(num);
    }

    public void setOptionBase(int num, int val) {
        getOptions().set(num, val);
    }

    public int[] getOptionBonus() {
        return new int[]{getOptions().get(3), getOptions().get(4), getOptions().get(5)};
    }

    public int getOption(int num, boolean bonus) {
        return bonus ? getOptionBonus(num) : getOptionBase(num);
    }

    public int getOptionBonus(int num) {
        return getOptions().get(num + 3);
    }

    public void setOptionBonus(int num, int val) {
        getOptions().set(num + 3, val);
    }

    public void setOption(int num, int val, boolean bonus) {
        if (bonus) {
            setOptionBonus(num, val);
        } else {
            setOptionBase(num, val);
        }
    }

    public int getRandomOption(boolean bonus, int line, int cubeId, int additionalPrimes) {
        // Cubes with rows in data/resources/tms282/enhance (273 OptionLevel / option weights, 279 overrides) replace
        // this fork's prime-line chance and option weights; the legal option set still comes from ItemConstants.
        int tableCube = net.swordie.ms.loaders.enhance.CubeRateTable.effectiveCube(cubeId);
        ItemGrade itemGrade = ItemGrade.getGradeByVal(bonus ? getBonusGrade() : getBaseGrade());
        int gradeIndex = net.swordie.ms.loaders.enhance.CubeRateTable.gradeIndex(itemGrade);
        if (line > 0 && gradeIndex > 0) {
            int same = net.swordie.ms.loaders.enhance.CubeRateTable.sameGradeChance(tableCube, gradeIndex, line + 1);
            if (same >= 0) {
                additionalPrimes = java.util.concurrent.ThreadLocalRandom.current().nextInt(10_000) < same ? line : 0;
            }
        }
        List<Integer> data = ItemConstants.getWeightedOptionsByEquip(this, bonus, line, cubeId, additionalPrimes);
        if (gradeIndex > 0 && !data.isEmpty()) {
            var weights = net.swordie.ms.loaders.enhance.CubeRateTable.optionWeights(tableCube,
                    net.swordie.ms.loaders.enhance.CubeRateTable.gradeIndex(ItemConstants.getLineTier(line, itemGrade, additionalPrimes)),
                    new HashSet<>(data));
            if (weights != null) {
                data = expandWeights(weights);
            }
        }
        int result = data.get(Util.getRandom(data.size() - 1));
        if (cubeId != 1) {
            if (line != 0) {
                int option1 = getOptions().getFirst();
                if (PotentialConstants.isBossDamagePercent(option1)) {
                    if (Util.succeedProp(10)) {
                        while (PotentialConstants.isBossDamagePercent(result) || PotentialConstants.isATTPercent(result) || PotentialConstants.isMATTPercent(result)) {
                            result = data.get(Util.getRandom(data.size() - 1));
                        }
                    }
                } else if (PotentialConstants.isDamagePercent(option1)) {
                    if (Util.succeedProp(10)) {
                        while (PotentialConstants.isDamagePercent(result)) {
                            result = data.get(Util.getRandom(data.size() - 1));
                        }
                    }
                } else if (PotentialConstants.isATTPercent(option1)) {
                    if (Util.succeedProp(10)) {
                        while (PotentialConstants.isATTPercent(result) || PotentialConstants.isBossDamagePercent(result)) {
                            result = data.get(Util.getRandom(data.size() - 1));
                        }
                    }
                } else if (PotentialConstants.isMATTPercent(option1)) {
                    if (Util.succeedProp(10)) {
                        while (PotentialConstants.isMATTPercent(result) || PotentialConstants.isBossDamagePercent(result)) {
                            result = data.get(Util.getRandom(data.size() - 1));
                        }
                    }
                } else if (PotentialConstants.isIgnoreMonsterDEFPercent(option1)) {
                    if (Util.succeedProp(10)) {
                        while (PotentialConstants.isIgnoreMonsterDEFPercent(result) || PotentialConstants.isBossDamagePercent(result)) {
                            result = data.get(Util.getRandom(data.size() - 1));
                        }
                    }
                } else if (PotentialConstants.isAllStatsPercent(option1)) {
                    if (Util.succeedProp(10)) {
                        while (PotentialConstants.isSTRPercent(result) || PotentialConstants.isDEXPercent(result) || PotentialConstants.isINTPercent(result) || PotentialConstants.isLUKPercent(result)) {
                            result = data.get(Util.getRandom(data.size() - 1));
                        }
                    }
                } else if (PotentialConstants.isSTRPercent(option1)) {
                    if (Util.succeedProp(10)) {
                        while (PotentialConstants.isSTRPercent(result) || PotentialConstants.isAllStatsPercent(result)) {
                            result = data.get(Util.getRandom(data.size() - 1));
                        }
                    }
                } else if (PotentialConstants.isDEXPercent(option1)) {
                    if (Util.succeedProp(10)) {
                        while (PotentialConstants.isDEXPercent(result) || PotentialConstants.isAllStatsPercent(result)) {
                            result = data.get(Util.getRandom(data.size() - 1));
                        }
                    }
                } else if (PotentialConstants.isLUKPercent(option1)) {
                    if (Util.succeedProp(10)) {
                        while (PotentialConstants.isLUKPercent(result) || PotentialConstants.isAllStatsPercent(result)) {
                            result = data.get(Util.getRandom(data.size() - 1));
                        }
                    }
                } else if (PotentialConstants.isINTPercent(option1)) {
                    if (Util.succeedProp(10)) {
                        while (PotentialConstants.isINTPercent(result) || PotentialConstants.isAllStatsPercent(result)) {
                            result = data.get(Util.getRandom(data.size() - 1));
                        }
                    }
                } else if (PotentialConstants.isCriticalDamagePercent(option1)) {
                    if (Util.succeedProp(10)) {
                        while (PotentialConstants.isCriticalDamagePercent(result)) {
                            result = data.get(Util.getRandom(data.size() - 1));
                        }
                    }
                } else if (PotentialConstants.isMaxHPPercent(option1)) {
                    if (Util.succeedProp(10)) {
                        while (PotentialConstants.isMaxHPPercent(result)) {
                            result = data.get(Util.getRandom(data.size() - 1));
                        }
                    }
                } else if (PotentialConstants.isMesoObtainedPercent(option1)) {
                    if (Util.succeedProp(10)) {
                        while (PotentialConstants.isMesoObtainedPercent(result)) {
                            result = data.get(Util.getRandom(data.size() - 1));
                        }
                    }
                } else if (PotentialConstants.isItemDropRatePercent(option1)) {
                    if (Util.succeedProp(10)) {
                        while (PotentialConstants.isItemDropRatePercent(result)) {
                            result = data.get(Util.getRandom(data.size() - 1));
                        }
                    }
                }
            }
        }
        return result;
    }

    /** Weighted table as a flat list (weights divided by their gcd) so the reroll logic above can keep using it. */
    private static List<Integer> expandWeights(net.swordie.ms.loaders.enhance.EnhanceTables.Weighted weights) {
        int gcd = 0;
        for (int option : weights.keys()) gcd = java.math.BigInteger.valueOf(gcd).gcd(java.math.BigInteger.valueOf(weights.weightOf(option))).intValue();
        List<Integer> list = new ArrayList<>();
        for (int option : weights.keys()) {
            for (int i = weights.weightOf(option) / gcd; i > 0; i--) list.add(option);
        }
        return list;
    }

    public List<Integer> getVioletOptions(int cubeId, int additionalPrimes, int totalLine) {
        List<Integer> results = new ArrayList<>();
        List<Integer> data = ItemConstants.getWeightedOptionsByEquip(this, false, 1, cubeId, additionalPrimes);
        if (ItemConstants.isBottom(getItemId())) {
            data.removeAll(List.of(40376, 40377, 40356, 40357));
        }
        if (ItemConstants.isGlove(getItemId())) {
            data.removeAll(List.of(20351, 20352, 20353, 30356, 30357, 40356, 40357, 60044, 60045, 60046, 60047, 70020, 70032));
        }
        //Option Index, List<Integer>
        Map<Integer, List<Integer>> mapFilter = new HashMap<>();
        //Đưa ra X line không giống nhau
        for (int i = 0; i < totalLine; i++) {
            int option = data.get(Util.getRandom(data.size() - 1));
            results.add(option);
            mapFilter.put(i, PotentialConstants.potentialFilter(data, option));
        }
        //2nd line same
        if (Randomizer.isSuccess(70)) {
            int option = results.get(Util.getRandom(results.size() - 1));
            //Thứ tự của line.
            int idx = results.indexOf(option);
            int idx2 = PotentialConstants.getRandomIndexExceptValue(List.of(idx), 0, totalLine - 1);
            if (mapFilter.get(idx) == null) {
                results.set(idx2, option);
            } else {
                results.set(idx2, mapFilter.get(idx).get(Util.getRandom(mapFilter.get(idx).size() - 1)));
            }
            //3rd line same
            if (Randomizer.isSuccess(20)) {
                int idx3 = PotentialConstants.getRandomIndexExceptValue(List.of(idx, idx2), 0, totalLine - 1);
                if (mapFilter.get(idx) == null) {
                    results.set(idx3, option);
                } else {
                    results.set(idx3, mapFilter.get(idx).get(Util.getRandom(mapFilter.get(idx).size() - 1)));
                }
            }
        }
        return results;
    }

    public String getOptionsSQL() {
        return String.format("%d,%d,%d,%d,%d,%d,%d",
                getOptions().get(0),
                getOptions().get(1),
                getOptions().get(2),
                getOptions().get(3),
                getOptions().get(4),
                getOptions().get(5),
                getOptions().get(6));
    }


    public String getSocketsSQL() {
        if (getSockets().isEmpty()) {
            setSockets(0, 0, 0);
        } else if (getSockets().size() < 2) {
            setSockets(getSockets().get(0), 0, 0);
        } else if (getSockets().size() == 2) {
            setSockets(getSockets().get(0), getSockets().get(1), 0);
        }
        return String.format("%d,%d,%d", getSockets().get(0), getSockets().get(1), getSockets().get(2));
    }

    // required level for players to equip this
    public int getRequiredLevel() {
        // the highest of them as negative values won't work as intended
        return Math.max(0, getrLevel() + getiIncReq() - (getiReduceReq() + getFlameStat().getReduceReqLevel()));
    }

    public void setOptionBase(short val, int thirdLineChance) {
        if (!ItemConstants.canEquipHavePotential(this)) {
            return;
        }

        int max = 3;
        if (getOptionBase(2) == 0) {
            // If this equip did not have a 3rd line already, thirdLineChance to get it
            if (Util.succeedProp(100 - thirdLineChance)) {
                max = 2;
            }
        }
        for (int i = 0; i < max; i++) {
            setOptionBase(i, val);
        }
    }

    /**
     * Resets the potential of this equip's base options. Takes the value of an ItemGrade (1-4), and sets the appropriate values.
     * Also calculates if a third line should be added.
     *
     * @param val             The value of the item's grade (HiddenRare~HiddenLegendary).getVal().
     * @param thirdLineChance The chance of a third line being added.
     */
    public void setHiddenOptionBase(short val, int thirdLineChance) {
        if (!ItemConstants.canEquipHavePotential(this)) {
            return;
        }

        int max = 3;
        if (getOptionBase(2) == 0) {
            // If this equip did not have a 3rd line already, thirdLineChance to get it
            if (Util.succeedProp(100 - thirdLineChance)) {
                max = 2;
            }
        }
        for (int i = 0; i < max; i++) {
            setOptionBase(i, -val);
        }
    }

    public void addLineHiddenOptionBonus(int slot, short val) {
        if (!ItemConstants.canEquipHavePotential(this)) {
            return;
        }

        setOptionBonus(slot, -val);
    }

    public void setHiddenOptionBonus(short val) {
        if (!ItemConstants.canEquipHavePotential(this)) {
            return;
        }

        setOptionBonus(0, -val);
    }

    public void resetHiddenOptionBonus(short val) {
        if (!ItemConstants.canEquipHavePotential(this)) {
            return;
        }

        int max = getOptionBonus(2) != 0 ? 3 : getOptionBonus(1) != 0 ? 2 : 1;
        for (int i = 0; i < max; i++) {
            setOptionBonus(i, -val);
        }
    }

    public void releaseOptions(boolean bonus) {
        if (!ItemConstants.canEquipHavePotential(this)) {
            return;
        }
        //Have to generate the prime count here as we loop afterwards and would regenerate otherwise
        int n = ItemConstants.getAdditionalPrimeCountForCube(ItemConstants.SYSTEM_DEFAULT_CUBE_INDICATOR);
        for (int i = 0; i < 3; i++) {
            if (getOption(i, bonus) < 0) {
                setOption(i, getRandomOption(bonus, i, ItemConstants.SYSTEM_DEFAULT_CUBE_INDICATOR, n), bonus);
            }
        }
    }

    public void releaseOptions(boolean bonus, int cubeId) {
        if (!ItemConstants.canEquipHavePotential(this)) {
            return;
        }
        //Have to generate the prime count here as we loop afterwards and would regenerate otherwise
        int n = ItemConstants.getAdditionalPrimeCountForCube(cubeId);
        for (int i = 0; i < 3; i++) {
            if (getOption(i, bonus) < 0) {
                setOption(i, getRandomOption(bonus, i, cubeId, n), bonus);
            }
        }
    }

    public int getAnvilId() {
        return getOptions().get(6); // Anvil
    }

    public Map<EnchantStat, Integer> getEnchantStats() {
        return enchantStats;
    }

    public void setEnchantStats(Map<EnchantStat, Integer> enchantStats) {
        this.enchantStats = enchantStats;
    }

    public void putEnchantStat(EnchantStat es, int val) {
        getEnchantStats().put(es, val);
    }

    public void recalcEnchantmentStats() {
        getEnchantStats().clear();
        for (int i = 0; i < getChuc(); i++) {
            for (EnchantStat es : getHyperUpgradeStats().keySet()) {
                putEnchantStat(es, getEnchantStats().getOrDefault(es, 0) + GameConstants.getEnchantmentValByChuc(this, es, (short) i, (int) getBaseStat(es.getEquipBaseStat())));
            }
        }
    }

    /**
     * Returns the current value of an EnchantStat. Zero if absent.
     *
     * @param es The EnchantStat to get
     * @return the corresponding stat value
     */
    public int getEnchantStat(EnchantStat es) {
        return getEnchantStats().getOrDefault(es, 0);
    }

    public int getSocketStat(ScrollStat ss) {
        int amount = 0;
        for (int i = 0; i < getSockets().size(); i++) {
            int id = getSocket(i);
            if (id != 0 && id != ItemConstants.EMPTY_SOCKET_ID) {
                int nebuliteId = ItemConstants.NEBILITE_BASE_ID + id;
                ItemInfo ii = ItemData.getItemInfoByID(nebuliteId);
                if (ii == null) {
                    amount = 0;
                } else {
                    amount = ii.getScrollStats().getOrDefault(ss, 0);
                }
            }
        }
        return amount;
    }

    public double getBaseStat(BaseStat baseStat) {
        double res = 0;
        for (int i = 0; i < getOptions().size() - 1; i++) { // last one is anvil => skipped
            int id = getOptions().get(i);
            int rawLevel = getrLevel() + getiIncReq();
            int level = rawLevel / 10;
            ItemOption io = ItemData.getItemOptionById(id);
            if (io != null) {
                if (rawLevel == 0 && net.swordie.ms.connection.protocol.Protocol.active().version()
                        == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) {
                    level = io.getOptionLevel(rawLevel);
                }
                Map<BaseStat, Double> valMap = io.getStatValuesByLevel(level);
                res += valMap.getOrDefault(baseStat, 0D);
            }
        }
        switch (baseStat) {
            case str -> res += getTotalStat(EquipBaseStat.iStr);
            case dex -> res += getTotalStat(EquipBaseStat.iDex);
            case inte -> res += getTotalStat(EquipBaseStat.iInt);
            case luk -> res += getTotalStat(EquipBaseStat.iLuk);
            case pad -> res += getTotalStat(EquipBaseStat.iPAD);
            case mad -> res += getTotalStat(EquipBaseStat.iMAD);
            case pdd -> res += getTotalStat(EquipBaseStat.iPDD);
            case mdd -> res += getTotalStat(EquipBaseStat.iMDD);
            case mhp -> res += getTotalStat(EquipBaseStat.iMaxHP);
            case mmp -> res += getTotalStat(EquipBaseStat.iMaxMP);
            case damR -> res += getTotalStat(EquipBaseStat.damR);
            case bd -> res += getTotalStat(EquipBaseStat.bdr);
            case ied -> res += getTotalStat(EquipBaseStat.imdr);
            case eva -> res += getTotalStat(EquipBaseStat.iEVA);
            case acc -> res += getTotalStat(EquipBaseStat.iACC);
            case speed -> res += getTotalStat(EquipBaseStat.iSpeed);
            case jump -> res += getTotalStat(EquipBaseStat.iJump);
            case booster -> res += getAttackSpeed();
            case strR -> res += getSocketStat(ScrollStat.incSTRr) + getTotalStat(EquipBaseStat.statR);
            case dexR -> res += getSocketStat(ScrollStat.incDEXr) + getTotalStat(EquipBaseStat.statR);
            case intR -> res += getSocketStat(ScrollStat.incINTr) + getTotalStat(EquipBaseStat.statR);
            case lukR -> res += getSocketStat(ScrollStat.incLUKr) + getTotalStat(EquipBaseStat.statR);
            case reduceCooltime -> res += getSocketStat(ScrollStat.reduceCooltime);
            case mpconReduce -> res += getSocketStat(ScrollStat.mpconReduce);
            case crDmg -> {
                if (getSocketStat(ScrollStat.incCriticaldamage) != 0) {
                    res += getSocketStat(ScrollStat.incCriticaldamage);
                } else {
                    int min = Math.max(getSocketStat(ScrollStat.incCriticaldamageMin), 0);
                    int max = Math.max(getSocketStat(ScrollStat.incCriticaldamageMax), 0);
                    res += (max + min) / 2.0D;
                }
            }
            case evaR -> res += getSocketStat(ScrollStat.incEVAr);
            case accR -> res += getSocketStat(ScrollStat.incACCr);
            case dropR -> res += getSocketStat(ScrollStat.incRewardProp);
            case mesoR -> res += getSocketStat(ScrollStat.incMesoProp);
            // 282: only arcane symbols (1712xxx) give arcane force; authentic ones give AUT (equips README WI-5)
            case arc -> res += net.swordie.ms.connection.protocol.Protocol.active().version()
                    == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3
                    && !net.swordie.ms.constants.ItemConstants.isArcaneSymbol(getItemId()) ? 0 : getSymbol().getInc();
        }
        // 282: 豪華真實符文 1714xxx worn in 1750..1752 add EXP / meso / drop % per level (grand-authentic.tsv, WI-5)
        if (getItemId() / 1000 == 1714) {
            res += net.swordie.ms.world.tms282.symbols.Tms282GrandAuthenticSymbols.wornBonus(getItemId(), getBagIndex(),
                    symbol == null ? 0 : symbol.getLevel(), baseStat);
        }
        return res;
    }

    @Override
    public boolean isTradable() {
        return !isTradeBlock() && !isEquipTradeBlock();
    }

    public boolean hasUsedSlots() {
        Equip defaultEquip = ItemData.getEquipDeepCopyFromID(getItemId(), false);
        return defaultEquip.getTuc() != getTuc();
    }

    public short getSocketMask() {
        short socketMask = 0; // 0b0nnn_kkkb: from right to left: boolean active, k empty, n has socket
        for (int i = 0; i < getSockets().size(); i++) {
            int socket = getSocket(i);
            // Self made numbers for socket: 3 == empty (since 0 is already taken for STR+1, similar for 1/2)
            if (socket != 0) {
                socketMask |= 1;
                socketMask |= 1 << i + 1;
                if (socket != ItemConstants.EMPTY_SOCKET_ID) {
                    socketMask |= 1 << (i + 4); // 3 sockets, look at the comment at socketMask.
                }
            }
        }
        return socketMask;
    }

    public short getSocket(int num) {
        return num < getSockets().size() ? getSockets().get(num) : 0;
    }

    public void setSocket(int num, int value) {
        final List<Short> result = new LinkedList<>();
        if (num == 0) {
            result.add((short) value);
            result.add(getSocket(1));
            result.add(getSocket(2));
        } else if (num == 1) {
            result.add(getSocket(0));
            result.add((short) value);
            result.add(getSocket(2));
        } else if (num == 2) {
            result.add(getSocket(0));
            result.add(getSocket(1));
            result.add((short) value);
        }
        setSockets(result);
    }

    public void setSockets(int value1, int value2, int value3) {
        final List<Short> result = new LinkedList<>();
        result.add((short) value1);
        result.add((short) value2);
        result.add((short) value3);
        setSockets(result);
    }

    public List<Short> getSockets() {
        return sockets;
    }

    public void setSockets(List<Short> sockets) {
        this.sockets = sockets;
    }

    public int getAndroid() {
        return android;
    }

    public void setAndroid(int android) {
        this.android = android;
    }

    public Android getAndroidLife() {
        return androidLife;
    }

    public void setAndroidLife(Android androidLife) {
        this.androidLife = androidLife;
    }

    public int getAndroidGrade() {
        return androidGrade;
    }

    public void setAndroidGrade(int androidGrade) {
        this.androidGrade = androidGrade;
    }

    public boolean canSafeguardHyperUpgrade() {
        return !isSuperiorEqp();
    }

    public void reset(boolean isPerfect) {
        reset(isPerfect, false);
    }

    public void reset(boolean isPerfect, boolean isArkInno) {
        // Reset Equip to Base Stats:
        Equip normalEquip = ItemData.getEquipDeepCopyFromID(getItemId(), false);
        for (EquipBaseStat ebs : EquipBaseStat.values()) {
            if (ebs != EquipBaseStat.attribute && ebs != EquipBaseStat.growthEnchant && ebs != EquipBaseStat.psEnchant) {
                setBaseStat(ebs, normalEquip.getBaseStat(ebs));
            }
        }
        // Reset flames:
        getFlameStat().reset();

        // Reset Star Force Enchancements:
        if (!isArkInno) {
            setChuc((short) 0, true);
        }

        if (isPerfect) {
            setIuc((short) 0);
        }
    }

    /**
     * TMS 282 回真 as the 282 item texts describe it. PERFECT (完美回真): everything but the potential, flames and
     * scissors count included (= {@link #reset(boolean) reset(true)}). NORMAL (回真): everything but the potential
     * and the flames (追加屬性不會初始化), scissors count kept. ARC (亞克回真, arcReset): everything but the
     * potential, star force and flames.
     */
    public void resetTms282(Tms282EnhanceItems.ResetKind kind) {
        if (kind == Tms282EnhanceItems.ResetKind.PERFECT) {
            reset(true);
            return;
        }
        if (kind != Tms282EnhanceItems.ResetKind.NORMAL && kind != Tms282EnhanceItems.ResetKind.ARC) {
            return;
        }
        boolean arc = kind == Tms282EnhanceItems.ResetKind.ARC;
        Equip normalEquip = ItemData.getEquipDeepCopyFromID(getItemId(), false);
        for (EquipBaseStat ebs : EquipBaseStat.values()) {
            if (ebs == EquipBaseStat.attribute || ebs == EquipBaseStat.growthEnchant || ebs == EquipBaseStat.psEnchant
                    || ebs == EquipBaseStat.exGradeOption || ebs == EquipBaseStat.cuttable
                    || (arc && ebs == EquipBaseStat.hyperUpgrade)) {
                continue;
            }
            setBaseStat(ebs, normalEquip.getBaseStat(ebs));
        }
        // Flames (getFlameStat / exGradeOption) are kept; star force is recomputed on the reset base stats.
        setChuc(arc ? getChuc() : (short) 0, true);
    }

    public void applyScroll(Item scroll, Char chr, boolean success, boolean boom, boolean tucProtect) {
        if (scroll == null || hasSpecialAttribute(EquipSpecialAttribute.Vestige)) {
            chr.chatMessage(SystemNotice, "發生未知錯誤。");
            chr.dispose();
            return;
        }
        int scrollID = scroll.getItemId();
        ItemInfo itemInfo = ItemData.getItemInfoByID(scrollID);
        Map<ScrollStat, Integer> vals = itemInfo.getScrollStats();
        TreeMap<EnchantStat, Integer> chaos = new TreeMap<>();
        if (!vals.isEmpty()) {
            boolean recover = vals.containsKey(ScrollStat.recover);
            boolean reset = vals.containsKey(ScrollStat.reset);
            // TMS 282: the 回真 kind comes from the 282 data (arcReset is not in the item dat).
            Tms282EnhanceItems.ResetKind reset282 = Tms282EnhanceItems.active()
                    ? Tms282EnhanceItems.resetKind(scrollID) : Tms282EnhanceItems.ResetKind.NONE;
            if (reset282 != Tms282EnhanceItems.ResetKind.NONE) {
                reset = true;
                recover = false;
            }
            int chance = vals.getOrDefault(ScrollStat.success, 100);
            boolean useTuc = /*!recover &&*/ !reset;
            boolean isChaos = vals.containsKey(ScrollStat.randStat);
            if (ItemConstants.isCleanStateScroll(scrollID)) {
                useTuc = false;
            }
            Equip fullTucEquip = ItemData.getEquipDeepCopyFromID(getItemId(), false);
            int maxTuc = fullTucEquip.getTuc();
            if (getTuc() >= maxTuc && (maxTuc + getIuc() - getTuc() <= getCuc() && ItemConstants.isCleanStateScroll(scrollID) && !isLastScrollFail) /*&& recover*/) {
                chr.chatMessage(SystemNotice, "無法對這件裝備使用。");
                chr.dispose();
                return;
            }
            if (success) {
                if (!ItemConstants.isCleanStateScroll(scrollID) && !ItemConstants.isInnocenceScroll(scrollID)) {
                    setLastScrollFail(false);
                }
                if (!recover && !reset && hasAttribute(EquipAttribute.ReturnScroll)) {
                    Equip copy = deepCopy();
                    copy.setId(getId());
                    copy.setInventoryID(0);
                    copy.setCharID(0);
                    chr.returnEffectInfo.equip = copy;
                    chr.setQRValueByKey(ReturnEffectInfo.RETURN_QR, "scrollID", "" + scroll.getItemId());
                }
                if (isChaos) {
                    if (getBaseStat(EquipBaseStat.tuc) <= 0) {
                        chr.chatMessage(SystemNotice, "無法對這件裝備使用。");
                        chr.dispose();
                        return;
                    }
                    boolean incredibleChaos = vals.containsKey(ScrollStat.noNegative);
                    // 273 RandStatScroll.json weights (rand-stat-scroll.tsv) for this scroll/stat, else the old roll.
                    var weightedChaos = net.swordie.ms.loaders.enhance.RandStatScrollTable.forScroll(scrollID);
                    for (EquipBaseStat ebs : ScrollStat.equipBaseStat) {
                        int cur = (int) getBaseStat(ebs);
                        if (cur == 0) {
                            continue;
                        }
                        var statWeights = weightedChaos == null ? null : weightedChaos.get(ebs);
                        if (statWeights != null) {
                            int delta = statWeights.pick(java.util.concurrent.ThreadLocalRandom.current());
                            addStat(ebs, delta);
                            chaos.put(EnchantStat.getByEquipBaseStat(ebs), delta);
                            continue;
                        }
                        int randStat = Util.getRandom(ItemConstants.RAND_CHAOS_MAX);
                        if (ebs.getVal() == EquipBaseStat.iMaxHP.getVal() || ebs.getVal() == EquipBaseStat.iMaxMP.getVal()) {
                            randStat *= 10;
                        }
                        if (!incredibleChaos) {
                            if (Util.succeedProp(65)) {
                                addStat(ebs, -randStat);
                                chaos.put(EnchantStat.getByEquipBaseStat(ebs), -randStat);
                            } else {
                                addStat(ebs, randStat);
                                chaos.put(EnchantStat.getByEquipBaseStat(ebs), randStat);
                            }
                        } else {
                            randStat = Util.getRandom(ItemConstants.RAND_CHAOS_MAX);
                            addStat(ebs, randStat);
                            chaos.put(EnchantStat.getByEquipBaseStat(ebs), randStat);
                        }
                    }
                } else if (recover) {
                    if (isLastScrollFail) {
                        setLastScrollFail(false);
                        addStat(EquipBaseStat.tuc, 1);
                    } else {
                        chr.chatMessage(SystemNotice, "只能用於至少失敗過 01 次的裝備。");
                    }
                } else if (reset) {
                    if (reset282 != Tms282EnhanceItems.ResetKind.NONE) {
                        resetTms282(reset282);
                    } else {
                        reset(vals.containsKey(ScrollStat.perfectReset));
                    }
                } else {
                    if (getBaseStat(EquipBaseStat.tuc) <= 0) {
                        chr.chatMessage(SystemNotice, "無法對這件裝備使用。");
                        chr.dispose();
                        return;
                    }
                    for (Map.Entry<ScrollStat, Integer> entry : vals.entrySet()) {
                        ScrollStat ss = entry.getKey();
                        int val = entry.getValue();
                        if (ss.getEquipStat() != null) {
                            addStat(ss.getEquipStat(), val);
                        }
                    }
                }
                if (useTuc) {
                    addStat(EquipBaseStat.tuc, -1);
                    addStat(EquipBaseStat.cuc, 1);
                }
            }
            else {
                if (!ItemConstants.isCleanStateScroll(scrollID) && !ItemConstants.isInnocenceScroll(scrollID)) {
                    setLastScrollFail(true);
                }
                if (tucProtect) {
                    chr.sendPopupSay("[公會技能] 已保護 1 次可升級次數。");
                }
                if (useTuc && !hasAttribute(EquipAttribute.UpgradeCountProtection) && !tucProtect) {
                    addStat(EquipBaseStat.tuc, -1);
                }
                if (boom) {
                    if (!hasAttribute(EquipAttribute.ProtectionScroll) && !ItemConstants.isLongOrBigSword(getItemId())) {
                        chr.consumeItem(this);
                        chr.write(UserLocal.hyperEnchantScrollRegister(success, scrollID, getItemId()));
                        if (isChaos) {
                            chr.write(FieldPacket.showChaosScrollUpgradeResult(scroll.getBagIndex(), scrollID, chaos, success, boom, tucProtect));
                        }
                        if (!hasAttribute(EquipAttribute.ScrollProtection) && !ItemConstants.isBigSword(getItemId())) {
                            chr.consumeItem(scrollID, 1);
                        } else {
                            removeAttribute(EquipAttribute.ScrollProtection);
                        }
                        return;
                    }
                }
            }
            removeAttribute(EquipAttribute.ProtectionScroll);
            removeAttribute(EquipAttribute.LuckyDay);
            if (useTuc) {
                removeAttribute(EquipAttribute.UpgradeCountProtection);
            }
            chr.write(UserLocal.hyperEnchantScrollRegister(success, scrollID, getItemId()));
            if (isChaos) {
                chr.write(FieldPacket.showChaosScrollUpgradeResult(scroll.getBagIndex(), scrollID, chaos, success, boom, tucProtect));
            }
            if (success) {
                chr.write(UserLocal.hyperEnchantScrollResult(scrollID, this));
            }
            if (!boom) {
                recalcEnchantmentStats();
                updateToChar(chr);
            }
            if (!hasAttribute(EquipAttribute.ScrollProtection) && !ItemConstants.isBigSword(getItemId())) {
                chr.consumeItem(scrollID, 1);
            } else {
                removeAttribute(EquipAttribute.ScrollProtection);
            }
            updateToChar(chr);
            if (success && !recover && !reset && hasAttribute(EquipAttribute.ReturnScroll)) {
                chr.write(WvsContext.returnEffectConfirm(chr));
                chr.write(WvsContext.returnEffectModified(this, scroll.getItemId()));
                chr.write(FieldPacket.showScrollVestigeCompensationResult(true));
            }
        } else {
            chr.chatMessage(SystemNotice, "發生未知錯誤。");
        }
        chr.dispose();
    }

    // Gets ATT bonus by flame tier.
    public short getATTBonus(short tier) {
        if (ItemConstants.isWeapon(getItemId())) {
            final double[] multipliers = isBossReward() ? ItemConstants.WEAPON_FLAME_MULTIPLIER_BOSS_WEAPON : ItemConstants.WEAPON_FLAME_MULTIPLIER;
            Equip baseEquip = ItemData.getEquipById(getItemId());
            int att = 0;
            if (baseEquip != null) {
                att = Math.max(baseEquip.getiPad(), baseEquip.getiMad());
            }
            return (short) Math.ceil(att * (multipliers[tier - 1] * getFlameLevel()) / 100.0);
        } else {
            return tier;
        }
    }

    /**
     * Equips whose stats are fixed by their template: arcane / authentic / grand authentic symbols (1712 / 1713 /
     * 1714xxx, stats come from the symbol level), medals, badges, emblems, totems, androids and mechanical hearts.
     * None of them takes a flame (ItemConstants.canEquipHaveFlame) or a stat variation in GMS 265 / TMS 282, and
     * (nearly) all have no upgrade slots (tuc 0) in the data. {@link #random()} leaves them unchanged.
     */
    public static boolean hasFixedStats(int itemId) {
        return ItemConstants.isSymbol(itemId) || ItemConstants.isMedal(itemId) || ItemConstants.isBadge(itemId)
                || ItemConstants.isEmblem(itemId) || ItemConstants.isTotem(itemId) || ItemConstants.isAndroid(itemId)
                || ItemConstants.isMechanicalHeart(itemId);
    }

    /** Random bonus lines of a new non-cash equip (ItemData.getEquipDeepCopyFromID); none for {@link #hasFixedStats}. */
    public void random() {
        if (hasFixedStats(getItemId())) {
            return;
        }
        int lvl = Math.max(1, rLevel);
        int unit = Math.max(1, lvl / 10); // 150 -> 15, 200 -> 20, 250 -> 25

        int minTier, maxTier, minLines, maxLines, powerMul = 1;
        if (lvl < 100) {
            minTier = 1; maxTier = 1;
            minLines = 1; maxLines = 2;
        } else if (lvl <= 129) {
            minTier = 1; maxTier = 2;
            minLines = 2; maxLines = 3;
        } else if (lvl <= 150) {
            minTier = 2; maxTier = 3;
            minLines = 3; maxLines = 5;
        } else if (lvl <= 199) {
            minTier = 3; maxTier = 4;
            minLines = 5; maxLines = 7;
        } else if (lvl <= 249) {
            minTier = 4; maxTier = 6;
            minLines = 6; maxLines = 9;
        } else {
            minTier = 5; maxTier = 7;
            minLines = 8; maxLines = 10;
        }

        int lines = Util.getRandom(minLines, maxLines);

        final int[] pool = {
                0,0,0,0, 1,1,1,1, 2,2,2,2, 3,3,3,3,
                4,4,4, 6,6,6,
                5,7,
                8,8,8, 9,9,9,
                10,11,
                12,13,14,
                15,15
        };

        boolean[] used = new boolean[16];

        int applied = 0;
        while (applied < lines) {
            int stat = pool[Util.getRandom(pool.length - 1)];

            if (stat != 15 && used[stat]) continue;

            int tier = Util.getRandom(minTier, maxTier) / 2;

            int baseStat = (unit / 3 + 1) * powerMul;     // main stat
            int baseAtt  = (unit / 5 + 1) * powerMul;     // PAD/MAD
            int baseDef  = (unit * 2) * powerMul;         // PDD/MDD

            switch (stat) {
                case 0 -> iStr += (short) (tier * Util.getRandom(1, 2) * baseStat);
                case 1 -> iDex += (short) (tier * Util.getRandom(1, 2) * baseStat);
                case 2 -> iInt += (short) (tier * Util.getRandom(1, 2) * baseStat);
                case 3 -> iLuk += (short) (tier * Util.getRandom(1, 2) * baseStat);
                case 4 -> iMaxHp += (short) Math.min(Short.MAX_VALUE, unit * 30 * tier * Math.max(1, powerMul / 2));
                case 6 -> iMaxMp += (short) Math.min(Short.MAX_VALUE, unit * 30 * tier * Math.max(1, powerMul / 2));
                case 5 -> iMaxHpr += (short) Math.min(30, tier + (powerMul >= 6 ? 2 : powerMul >= 4 ? 1 : 0));
                case 7 -> iMaxMpr += (short) Math.min(30, tier + (powerMul >= 6 ? 2 : powerMul >= 4 ? 1 : 0));
                case 8 -> iPad += (short) Math.min(Short.MAX_VALUE, tier * baseAtt);
                case 9 -> iMad += (short) Math.min(Short.MAX_VALUE, tier * baseAtt);
                case 10 -> iPDD += (short) Math.min(Short.MAX_VALUE, tier * baseDef);
                case 11 -> iMDD += (short) Math.min(Short.MAX_VALUE, tier * baseDef);
                case 12 -> iAcc += (short) (tier * (unit / 6 + 1) * Math.max(1, powerMul / 2));
                case 13 -> iEva += (short) (tier * (unit / 6 + 1) * Math.max(1, powerMul / 2));
                case 14 -> iCraft += (short) (tier * (unit / 6 + 1) * Math.max(1, powerMul / 2));
                case 15 -> {
                    if ((Util.getRandom(1) & 1) == 0) iSpeed += (short) Math.min(20, tier + (powerMul >= 6 ? 2 : 0));
                    else iJump += (short) Math.min(20, tier + (powerMul >= 6 ? 2 : 0));
                }
            }
            if (stat != 15) used[stat] = true;
            applied++;
        }
    }

    public short flame(FlameType type) {
        getFlameStat().reset();
        if (!ItemConstants.canEquipHaveFlame(this)) {
            return 0;
        }
        final boolean adv = isBossReward(); // flame-advantaged
        final int bonusLines = adv ? 4 : (type == FlameType.Powerful ? 4 : Util.getRandom(1, 3));

        final int minTier, maxTier;
        switch (type) {
            case Powerful -> {            // up to T4, adv up to T6
                minTier = 1;
                maxTier = adv ? 6 : 4;
            }
            case Eternal, Black -> {      // min T2, rare T5; adv min T4, rare T7
                minTier = adv ? 4 : 2;
                maxTier = adv ? 7 : 5;
            }
            case Abyssal -> {             // min T3, rare T5; adv min T5, rare T7
                minTier = adv ? 5 : 3;
                maxTier = adv ? 7 : 5;
            }
            default -> throw new IllegalStateException();
        }
        return applyFlameLines(bonusLines, () -> Util.getRandom(minTier, maxTier), false);
    }

    /** TMS 282 weighted flame: the caller rolls line count and per-line tier; lines are distinct stats. */
    public short flameTms282(int bonusLines, java.util.function.IntSupplier tierRoll) {
        getFlameStat().reset();
        if (!ItemConstants.canEquipHaveFlame(this)) {
            return 0;
        }
        return applyFlameLines(bonusLines, tierRoll, true);
    }

    private short applyFlameLines(int bonusLines, java.util.function.IntSupplier tierRoll, boolean distinct) {
        short tier = 0;
        boolean[] picked = new boolean[FlameStat.values().length];
        long exGradeOption = 0, factor = 1;
        int applied = 0;
        while (applied < bonusLines) {
            int r = Util.getRandom(FlameStat.values().length - 1);
            FlameStat fs = FlameStat.getByVal(r);
            if (fs == null) continue;
            if (distinct && picked[r]) continue;
            if (fs == FlameStat.LevelReduction && (getrLevel() + getiIncReq() < 5)) continue;
            if ((fs == FlameStat.BossDamage || fs == FlameStat.Damage) && !ItemConstants.isWeapon(getItemId())) continue;

            tier = (short) tierRoll.getAsInt();
            int added = tier * getFlameLevel();
            int addedExt = tier * getFlameLevelExtended();
            switch (fs) {
                case STR -> getFlameStat().setSTR(getFlameStat().getSTR() + addedExt);
                case DEX -> getFlameStat().setDEX(getFlameStat().getDEX() + addedExt);
                case INT -> getFlameStat().setINT(getFlameStat().getINT() + addedExt);
                case LUK -> getFlameStat().setLUK(getFlameStat().getLUK() + addedExt);
                case STRDEX -> {
                    getFlameStat().setSTR(getFlameStat().getSTR() + added);
                    getFlameStat().setDEX(getFlameStat().getDEX() + added);
                }
                case STRINT -> {
                    getFlameStat().setSTR(getFlameStat().getSTR() + added);
                    getFlameStat().setINT(getFlameStat().getINT() + added);
                }
                case STRLUK -> {
                    getFlameStat().setSTR(getFlameStat().getSTR() + added);
                    getFlameStat().setLUK(getFlameStat().getLUK() + added);
                }
                case DEXINT -> {
                    getFlameStat().setDEX(getFlameStat().getDEX() + added);
                    getFlameStat().setINT(getFlameStat().getINT() + added);
                }
                case DEXLUK -> {
                    getFlameStat().setDEX(getFlameStat().getDEX() + added);
                    getFlameStat().setLUK(getFlameStat().getLUK() + added);
                }
                case INTLUK -> {
                    getFlameStat().setINT(getFlameStat().getINT() + added);
                    getFlameStat().setLUK(getFlameStat().getLUK() + added);
                }
                case Attack -> getFlameStat().setPAD(getFlameStat().getPAD() + getATTBonus(tier));
                case MagicAttack -> getFlameStat().setMAD(getFlameStat().getMAD() + getATTBonus(tier));
                case Defense -> getFlameStat().setPDD(getFlameStat().getPDD() + addedExt);
                case MaxHP ->
                        getFlameStat().setHP(getFlameStat().getHP() + ((getrLevel() + getiIncReq()) / 10) * 30 * tier);
                case MaxMP ->
                        getFlameStat().setMP(getFlameStat().getMP() + ((getrLevel() + getiIncReq()) / 10) * 30 * tier);
                case Speed -> getFlameStat().setSpeed(getFlameStat().getSpeed() + tier);
                case Jump -> getFlameStat().setJump(getFlameStat().getJump() + tier);
                case AllStats -> getFlameStat().setAllStatR(getFlameStat().getAllStatR() + tier);
                case BossDamage -> getFlameStat().setBossDamageR(getFlameStat().getBossDamageR() + tier * 2);
                case Damage -> getFlameStat().setDamage(getFlameStat().getDamage() + tier);
                case LevelReduction ->
                        getFlameStat().setReduceReqLevel(getFlameStat().getReduceReqLevel() + (5 * tier));
            }
            int line = fs.getExGrade() * 10 + tier;
            exGradeOption += factor * line;
            factor *= 1000L;

            picked[r] = true;
            applied++;
        }
        setExGradeOption(exGradeOption);
        return tier;
    }

    public void flame(long exGradeOption) {
        setExGradeOption(exGradeOption);
        getFlameStat().reset();
        if (!ItemConstants.canEquipHaveFlame(this)) return;

        FlameStat[] vals = FlameStat.values();

        while (exGradeOption > 0) {
            int line = (int) (exGradeOption % 1000); // aab
            exGradeOption /= 1000;

            int exType = line / 10;                  // aa
            short tier = (short) (line % 10);        // b
            if (tier <= 0) continue;

            FlameStat fs = null;
            for (FlameStat t : vals) {
                if (t.getExGrade() == exType) { fs = t; break; }
            }
            if (fs == null) continue;

            int added    = tier * getFlameLevel();
            int addedExt = tier * getFlameLevelExtended();

            switch (fs) {
                case STR -> getFlameStat().setSTR(getFlameStat().getSTR() + addedExt);
                case DEX -> getFlameStat().setDEX(getFlameStat().getDEX() + addedExt);
                case INT -> getFlameStat().setINT(getFlameStat().getINT() + addedExt);
                case LUK -> getFlameStat().setLUK(getFlameStat().getLUK() + addedExt);

                case STRDEX -> { getFlameStat().setSTR(getFlameStat().getSTR() + added);
                    getFlameStat().setDEX(getFlameStat().getDEX() + added); }
                case STRINT -> { getFlameStat().setSTR(getFlameStat().getSTR() + added);
                    getFlameStat().setINT(getFlameStat().getINT() + added); }
                case STRLUK -> { getFlameStat().setSTR(getFlameStat().getSTR() + added);
                    getFlameStat().setLUK(getFlameStat().getLUK() + added); }
                case DEXINT -> { getFlameStat().setDEX(getFlameStat().getDEX() + added);
                    getFlameStat().setINT(getFlameStat().getINT() + added); }
                case DEXLUK -> { getFlameStat().setDEX(getFlameStat().getDEX() + added);
                    getFlameStat().setLUK(getFlameStat().getLUK() + added); }
                case INTLUK -> { getFlameStat().setINT(getFlameStat().getINT() + added);
                    getFlameStat().setLUK(getFlameStat().getLUK() + added); }

                case Attack -> getFlameStat().setPAD(getFlameStat().getPAD() + getATTBonus(tier));
                case MagicAttack -> getFlameStat().setMAD(getFlameStat().getMAD() + getATTBonus(tier));
                case Defense -> getFlameStat().setPDD(getFlameStat().getPDD() + addedExt);
                case MaxHP -> getFlameStat().setHP(getFlameStat().getHP() + ((getrLevel() + getiIncReq()) / 10) * 30 * tier);
                case MaxMP -> getFlameStat().setMP(getFlameStat().getMP() + ((getrLevel() + getiIncReq()) / 10) * 30 * tier);
                case Speed -> getFlameStat().setSpeed(getFlameStat().getSpeed() + tier);
                case Jump -> getFlameStat().setJump(getFlameStat().getJump() + tier);
                case AllStats -> getFlameStat().setAllStatR(getFlameStat().getAllStatR() + tier);
                case BossDamage -> getFlameStat().setBossDamageR(getFlameStat().getBossDamageR() + tier * 2);
                case Damage -> getFlameStat().setDamage(getFlameStat().getDamage() + tier);
                case LevelReduction -> getFlameStat().setReduceReqLevel(getFlameStat().getReduceReqLevel() + (5 * tier));
            }
        }
    }

    // Used for STR/DEX/INT/LUK/DEF additions.
    public short getFlameLevelExtended() {
        return (short) Math.ceil((getrLevel() + getiIncReq() + 1.0) / ItemConstants.EQUIP_FLAME_LEVEL_DIVIDER_EXTENDED);
    }

    // Used for secondary stat increasing.
    public short getFlameLevel() {
        return (short) Math.ceil((getrLevel() + getiIncReq() + 1.0) / ItemConstants.EQUIP_FLAME_LEVEL_DIVIDER);
    }

    /**
     * Conversion factor between mob exp and equip exp gain. Through many calculations, the expected for equipment levelup
     * from level 1 to 2 is killing about 100~200 mobs of the same level range, on a 1x EXP rate scenario.
     */
    private static double normalizedMasteryExp(int reqLevel) {
        if (reqLevel < 5) {
            return 42;
        } else if (reqLevel >= 158) {
            return Math.max((20827.296 * Math.exp(reqLevel * 0.04543)), 15);
        } else if (reqLevel >= 78) {
            return Math.max((10413.648 * Math.exp(reqLevel * 0.03275)), 15);
        } else if (reqLevel >= 38) {
            return Math.max((4985.818 * Math.exp(reqLevel * 0.02007)), 15);
        } else if (reqLevel >= 18) {
            return Math.max((248.219 * Math.exp(reqLevel * 0.11093)), 15);
        } else {
            return Math.max(((1334.564 * Math.log(reqLevel)) - 1731.976), 15);
        }
    }

    public void gainItemExp(Char chr, long gain) {
        if (level >= ItemConstants.EQUIPMENT_MAXLEVEL) {
            return;
        }
        float masteryGain = (float) GameConstants.charExp[1] / (float) normalizedMasteryExp(getrLevel());
        float elementGain = 0.6f;
        long baseExpGain = (long) (gain * elementGain * masteryGain);
        exp += baseExpGain;
        long expNeeded = GameConstants.itemExp[level];
        if (exp >= expNeeded) {
            while (exp >= expNeeded) {
                exp -= expNeeded;
                gainLevel(chr);
                if (level >= ItemConstants.EQUIPMENT_MAXLEVEL) {
                    exp = 0;
                    break;
                }
                expNeeded = GameConstants.itemExp[level];
            }
        }
        updateToChar(chr);
        chr.dispose();
    }

    private void gainLevel(Char chr) {
        level++;
        int max = ItemConstants.INC_RAND_LEVELUP_MAX;
        for (EquipBaseStat ebs : ScrollStat.equipBaseStat) {
            int cur = (int) getBaseStat(ebs);
            if (cur != 0) {
                continue;
            }
            int randStat = Util.getRandom(1, max);
            addStat(ebs, randStat);
        }
        chr.write(UserPacket.effect(Effect.itemLevelUpEffect()));
        chr.getField().broadcast(UserRemote.effect(chr.getId(), Effect.itemLevelUpEffect()), chr);
        chr.chatMessage(ChatType.Notice, "通知：你的 " + StringData.getItemStringById(getItemId()) + " 已提升到 " + level + " 級！");
        updateToChar(chr);
    }

    public void setScrollStatForTransmission(Equip equip) {
        Equip baseWeapon = ItemData.getEquipById(equip.getItemId());
        if (baseWeapon == null) {
            return;
        }
        Map<EquipBaseStat, Integer> scrollStats = new HashMap<>() {{
            put(EquipBaseStat.tuc, (int) (equip.getTuc())); // Slot Available = lazuliWeapon.getTuc() - oldWeapon.getTuc() _ when newWeapon.getTuc() > oldWeapon.getTuc()
            put(EquipBaseStat.cuc, (int) equip.getCuc()); // Slot Applied
            put(EquipBaseStat.iuc, (int) equip.getIuc()); //Total Hammer Applied
            put(EquipBaseStat.iStr, (int) equip.getiStr() - baseWeapon.getiStr());
            put(EquipBaseStat.iDex, (int) equip.getiDex() - baseWeapon.getiDex());
            put(EquipBaseStat.iInt, (int) equip.getiInt() - baseWeapon.getiInt());
            put(EquipBaseStat.iLuk, (int) equip.getiLuk() - baseWeapon.getiLuk());
            put(EquipBaseStat.iMaxHP, (int) equip.getiMaxHp() - baseWeapon.getiMaxHp());
            put(EquipBaseStat.iPAD, (int) equip.getiPad() - baseWeapon.getiPad());
            put(EquipBaseStat.iMAD, (int) equip.getiMad() - baseWeapon.getiMad());
            put(EquipBaseStat.iPDD, (int) equip.getiPDD() - baseWeapon.getiPDD());
            put(EquipBaseStat.iMDD, (int) equip.getiMDD() - baseWeapon.getiMDD());
            put(EquipBaseStat.iSpeed, (int) equip.getiSpeed() - baseWeapon.getiSpeed());
            put(EquipBaseStat.iJump, (int) equip.getiJump() - baseWeapon.getiJump());
        }};
        short soulOptionID = equip.getSoulOptionId();
        short soulSocketID = equip.getSoulSocketId();
        short soulOption = equip.getSoulOption();
        int soulItemID = equip.getSoulItemId();
        int setItemID = equip.getSetItemID();
        short totalStar = equip.getChuc();
        EquipFlame equipFlame = equip.getFlameStat();
        EquipExceptional equipExceptional = equip.getExceptionalStat();
        List<Integer> potentials = equip.getOptions();

        for (Map.Entry<EquipBaseStat, Integer> scrollStat : scrollStats.entrySet()) {
            if (scrollStat.getKey() == EquipBaseStat.tuc) {
                this.setTuc(scrollStat.getValue().shortValue());
            } else if (scrollStat.getKey() == EquipBaseStat.cuc) {
                this.setCuc(scrollStat.getValue().shortValue());
            } else {
                this.addStat(scrollStat.getKey(), scrollStat.getValue());
            }
        }
        this.setSoulOptionId(soulOptionID);
        this.setSoulSocketId(soulSocketID);
        this.setSoulOption(soulOption);
        this.setSoulItemId(soulItemID);
        this.setSetItemID(setItemID);
        this.setChuc(totalStar, true);
        this.setFlameStat(equipFlame);
        this.setExceptionalStat(equipExceptional);
        this.setOptions(potentials);
    }

    public int getMesoR() {
        int mesoR = 0;
        for (var i : getOptions()) {
            if (i == 40650 || i == 70107) {
                ItemOption itemOption = ItemData.getItemOptionById(i);
                if (itemOption != null) {
                    int rawLevelPotential = getrLevel() + getiIncReq();
                    mesoR += itemOption.getValue(rawLevelPotential);
                }
            }
        }
        return mesoR;
    }

    public int getDropR() {
        int dropR = 0;
        for (var i : getOptions()) {
            if (i == 40656 || i == 70117) {
                ItemOption itemOption = ItemData.getItemOptionById(i);
                if (itemOption != null) {
                    int rawLevelPotential = getrLevel() + getiIncReq();
                    dropR += itemOption.getValue(rawLevelPotential);
                }
            }
        }
        return dropR;
    }

    public static void notifyUnionChuc(Char chr, int change) {
        if (chr == null || chr.getAvatarData() == null || chr.getAvatarData().getCharacterStat() == null) {
            return;
        }
        if (change != 0) {
            CharacterStat cs = chr.getAvatarData().getCharacterStat();
            cs.setChuc(cs.getChuc() + change);
            Account account = chr.getAccount();
            if (account != null) {
                Union union = account.getUnion();
                if (union != null) {
                    for (UnionBoard unionBoard : union.getUnionBoards()) {
                        if (unionBoard == null) {
                            continue;
                        }
                        for (UnionMember unionMember : unionBoard.getActiveMembers()) {
                            if (unionMember == null) {
                                continue;
                            }
                            if (unionMember.getCharId() == chr.getId()) {
                                int totalChuc = unionMember.getChuc();
                                unionMember.setCharTotalChuc(totalChuc + change);
                            }
                        }
                    }
                }
            }
        }
    }

    public static void notifyUnionChuc(Char chr, Equip equip, int oldChuc, int newChuc) {
        if (equip.getInvType() != InvType.EQUIPPED) {
            return;
        }
        int change = newChuc - oldChuc;
        notifyUnionChuc(chr, change);
    }

    // ==== script-pack compatibility aliases ====
    // The Chinese v265 script pack uses different accessor names than this fork.

    public void setChuc(int chuc) {
        setChuc((short) chuc, false);
    }

    public void setGrade(int grade) {
        // grade derives from potential options in this fork; scripts set options right after
    }

    public int getRuc() {
        return getTuc() - getCuc();
    }

    public void setRuc(int remaining) {
        setTuc((short) (getCuc() + remaining));
    }

    public void setCuc(int cuc) {
        setCuc((short) cuc);
    }

    public void setOption1(int val) {
        setOptionBase(0, val);
    }

    public void setOption2(int val) {
        setOptionBase(1, val);
    }

    public void setOption3(int val) {
        setOptionBase(2, val);
    }

    public int getPad() {
        return getiPad();
    }

    public void setPad(int pad) {
        setiPad((short) pad);
    }

    public int getMad() {
        return getiMad();
    }

    public void setMad(int mad) {
        setiMad((short) mad);
    }

    public int getBaseStr() {
        return getiStr();
    }

    public void setBaseStr(int v) {
        setiStr((short) v);
    }

    public int getBaseDex() {
        return getiDex();
    }

    public void setBaseDex(int v) {
        setiDex((short) v);
    }

    public int getBaseInt() {
        return getiInt();
    }

    public void setBaseInt(int v) {
        setiInt((short) v);
    }

    public int getBaseLuk() {
        return getiLuk();
    }

    public void setBaseLuk(int v) {
        setiLuk((short) v);
    }
}
