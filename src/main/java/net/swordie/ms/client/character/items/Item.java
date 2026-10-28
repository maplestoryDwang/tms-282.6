package net.swordie.ms.client.character.items;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.Encodable;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.connection.packet.WvsContext;
import net.swordie.ms.constants.GameConstants;
import net.swordie.ms.constants.ItemConstants;
import net.swordie.ms.enums.InvType;
import net.swordie.ms.loaders.ItemData;
import net.swordie.ms.util.DataPrinter;
import net.swordie.ms.util.FileTime;

import java.io.Serializable;
import java.sql.*;
import java.text.ParseException;
import java.util.*;

import static net.swordie.ms.enums.InvType.*;
import static net.swordie.ms.enums.InventoryOperation.Add;

public class Item implements Serializable, Encodable {

    protected int itemId;
    protected int bagIndex;
    protected long cashItemSerialNumber;
    protected FileTime dateExpire = FileTime.MAX_TIME();
    protected InvType invType;
    protected Type type;
    protected boolean isCash;
    protected int quantity;
    protected boolean expireOnLogout;
    protected boolean obtainedOnce;
    protected long id;
    protected int inventoryID;
    protected int charID;
    protected int trunkID;
    protected AuctionHouseStatus auctionHouseStatus = AuctionHouseStatus.NORMAL;
    protected String owner = "";
    protected long zeroShareItemID;
    protected short attribute = 0; //Không được sài lung tung chỉ sài cho item untrade có tag: "Can be Traded once within an account." vì chưa tìm hết các attribute.

    // Boss Reward | Intense Power Crystal
    protected int bossRewardID;
    protected int mobTemplateID;
    protected int partySize;
    protected long price;

    // Title
    protected boolean titleOn;

    // Equipped Preset
    protected int preset;

    private static Item createItemFromResultSet(ResultSet rs) throws SQLException, ParseException {
        Item item = null;
        int itemID = rs.getInt("itemid");
        if (ItemData.getItemDeepCopy(itemID) == null) {
            System.out.println("Unable to find item ID " + itemID + " for createItemFromResultSet");
            return null;
        }
        if (ItemConstants.isEquip(itemID)) {
            Equip equip = new Equip();
            equip.setItemId(itemID);
            loadEquipData(equip, rs);
            item = equip;
            item.setType(Type.EQUIP);
        } else if (ItemConstants.isPet(itemID)) {
            PetItem petItem = new PetItem();
            loadPetItemData(petItem, rs);
            item = petItem;
            item.setType(Type.PET);
        } else {
            item = new Item();
            item.setType(Type.ITEM);
        }
        loadCommonItemData(item, rs);
        return item;
    }

    private static List<Item> getItemsFromSQLByQuery(PreparedStatement ps) {
        List<Item> items = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int itemID = rs.getInt("itemid");
                Item item;
                if (ItemData.getItemDeepCopy(itemID) == null) {
                    continue;
                }
                if (ItemConstants.isEquip(itemID)) {
                    Equip equip = new Equip();
                    equip.setItemId(itemID);
                    loadEquipData(equip, rs);
                    item = equip;
                    item.setType(Type.EQUIP);
                } else if (ItemConstants.isPet(itemID)) {
                    PetItem petItem = new PetItem();
                    loadPetItemData(petItem, rs);
                    item = petItem;
                    item.setType(Type.PET);
                } else {
                    item = new Item();
                    item.setType(Type.ITEM);
                }
                loadCommonItemData(item, rs);
                items.add(item);
            }
        } catch (Exception e) {
            DataPrinter.send(DataPrinter.HIKARICP_ERROR, e);
        }
        return items;
    }

    static void loadEquipData(Equip equip, ResultSet rs) throws SQLException, ParseException {
        // --- Bắt đầu phần code từ Equip ---
        //equip.setPreset(rs.getInt("preset"));

        equip.setSerialNumber(rs.getLong("serialnumber"));
        equip.setEquippedDate(DatabaseManager.getFileTimeFromString(rs.getString("equippeddate")));
        equip.setPrevBonusExpRate(rs.getInt("prevbonusexprate"));

        // Tải Potential
        String potentialString = rs.getString("options");
        List<Integer> options = new LinkedList<>();
        if (potentialString != null && !potentialString.isEmpty() && !potentialString.equals(" ")) {
            String[] optionsString = potentialString.split(",");
            for (String s : optionsString) {
                options.add(Integer.parseInt(s));
            }
        } else {
            options.add(0);
            options.add(0);
            options.add(0);
            options.add(0);
            options.add(0);
            options.add(0);
            options.add(0);
        }
        equip.setOptions(options);

        // Tải Socket
        String socketsString = rs.getString("sockets");
        List<Short> sockets = new LinkedList<>();
        if (socketsString != null && !socketsString.isEmpty() && !socketsString.equals(" ")) {
            String[] socketString = socketsString.split(",");
            for (String s : socketString) {
                sockets.add(Short.parseShort(s));
            }
        } else {
            sockets.add((short) 0);
            sockets.add((short) 0);
            sockets.add((short) 0);
        }
        equip.setSockets(sockets);

        // Tải các chỉ số cơ bản
        equip.setTuc(rs.getShort("tuc"));
        equip.setCuc(rs.getShort("cuc"));
        equip.setiStr(rs.getShort("istr"));
        equip.setiDex(rs.getShort("idex"));
        equip.setiInt(rs.getShort("iint"));
        equip.setiLuk(rs.getShort("iluk"));
        equip.setiMaxHp(rs.getShort("imaxhp"));
        equip.setiMaxHpr(rs.getShort("imaxhpr"));
        equip.setiMaxMp(rs.getShort("imaxmp"));
        equip.setiMaxMpr(rs.getShort("imaxmpr"));
        equip.setiPad(rs.getShort("ipad"));
        equip.setiMad(rs.getShort("imad"));
        equip.setiPDD(rs.getShort("ipdd"));
        equip.setiMDD(rs.getShort("imdd"));
        equip.setiAcc(rs.getShort("iacc"));
        equip.setiEva(rs.getShort("ieva"));
        equip.setiCraft(rs.getShort("icraft"));
        equip.setiSpeed(rs.getShort("ispeed"));
        equip.setiJump(rs.getShort("ijump"));
        equip.setAttribute(rs.getInt("equipattribute"));
        equip.setLevelUpType(rs.getShort("leveluptype"));
        equip.setItemLevel(rs.getShort("level"));
        equip.setItemEXP(rs.getShort("exp"));
        equip.setDurability(rs.getShort("durability"));
        equip.setIuc(rs.getShort("iuc"));
        equip.setiPvpDamage(rs.getShort("ipvpdamage"));
        equip.setiReduceReq(rs.getShort("ireducereq"));
        equip.setSpecialAttribute(rs.getShort("specialattribute"));
        equip.setDurabilityMax(rs.getShort("durabilitymax"));
        equip.setiIncReq(rs.getShort("iincreq"));
        equip.setGrowthEnchant(rs.getShort("growthenchant"));
        equip.setPsEnchant(rs.getShort("psenchant"));
        equip.setBossReward(rs.getByte("bossreward") != 0);
        equip.setSuperiorEqp(rs.getByte("superioreqp") != 0);
        equip.setCuttable(rs.getShort("cuttable"));
        equip.setExGradeOption(rs.getLong("exgradeoption"));
        equip.setHyperUpgrade(rs.getInt("hyperupgrade"));
        equip.setItemState(rs.getShort("itemstate"));
        equip.setChuc(rs.getShort("chuc"), false);
        equip.setSoulOption(rs.getShort("souloption"));
        equip.setSoulSocketId(rs.getShort("soulsocketid"));
        equip.setSoulOptionId(rs.getShort("souloptionid"));
        equip.setSoulItemId(rs.getInt("soulitemid"));
        equip.setrStr(rs.getShort("rstr"));
        equip.setrDex(rs.getShort("rdex"));
        equip.setrInt(rs.getShort("rint"));
        equip.setrLuk(rs.getShort("rluk"));
        equip.setrLevel(rs.getShort("rlevel"));
        equip.setrJob(rs.getShort("rjob"));
        equip.setrPop(rs.getShort("rpop"));
        equip.setSpecialGrade(rs.getInt("specialgrade"));
        equip.setFixedPotential(rs.getByte("fixedpotential") != 0);
        equip.setNoPotential(rs.getByte("nopotential") != 0);
        equip.setTradeBlock(rs.getByte("tradeblock") != 0);
        equip.setOnly(rs.getByte("isonly") != 0);
        equip.setNotSale(rs.getByte("notsale") != 0);
        equip.setAttackSpeed(rs.getInt("attackspeed"));
        equip.setPrice(rs.getLong("price"));
        equip.setCharmEXP(rs.getInt("charmexp"));
        equip.setSetItemID(rs.getInt("setitemid"));
        equip.setExItem(rs.getByte("exitem") != 0);
        equip.setEquipTradeBlock(rs.getByte("equiptradeblock") != 0);
        equip.setiSlot(rs.getString("islot"));
        equip.setvSlot(rs.getString("vslot"));
        equip.setFixedGrade(rs.getInt("fixedgrade"));
        equip.setAndroid(rs.getInt("android"));
        equip.setAndroidGrade(rs.getInt("androidgrade"));

        // Tải Flame Stats
        EquipFlame equipFlame = new EquipFlame(
                rs.getShort("flame_str"),
                rs.getShort("flame_dex"),
                rs.getShort("flame_int"),
                rs.getShort("flame_luk"),
                rs.getShort("flame_pad"),
                rs.getShort("flame_mad"),
                rs.getShort("flame_pdd"),
                rs.getShort("flame_hp"),
                rs.getShort("flame_mp"),
                rs.getShort("flame_speed"),
                rs.getShort("flame_jump"),
                rs.getShort("flame_allStatR"),
                rs.getShort("flame_bossDamageR"),
                rs.getShort("flame_damageR"),
                rs.getShort("flame_reduceReqLevel"));
        equip.setFlameStat(equipFlame);

        EquipSymbol symbol = new EquipSymbol(
                rs.getShort("arcane_stat"),
                rs.getInt("arcane_exp"),
                rs.getInt("arcane_level"),
                ItemConstants.isSacredSymbol(equip.getItemId()));
        equip.setSymbol(symbol);

        equip.setExceptionalSlot(rs.getByte("except_slot"));
        EquipExceptional equipExceptional = new EquipExceptional(
                rs.getShort("except_str"),
                rs.getShort("except_dex"),
                rs.getShort("except_int"),
                rs.getShort("except_luk"),
                rs.getShort("except_pad"),
                rs.getShort("except_mad"),
                rs.getShort("except_pdd"),
                rs.getShort("except_hp"),
                rs.getShort("except_mp"),
                rs.getShort("except_speed"),
                rs.getShort("except_jump"),
                rs.getShort("except_allStatR"),
                rs.getShort("except_bossDamageR"),
                rs.getShort("except_damageR"),
                rs.getShort("except_reduceReqLevel"));
        equip.setExceptionalStat(equipExceptional);
        // --- Kết thúc phần code từ Equip ---
    }

    static void loadPetItemData(PetItem petItem, ResultSet rs) throws SQLException, ParseException {
        // --- Bắt đầu phần code từ PetItem ---
        petItem.setName(rs.getString("name"));
        petItem.setLevel(rs.getByte("level"));
        petItem.setTameness(rs.getShort("tameness"));
        petItem.setRepleteness(rs.getByte("repleteness"));
        petItem.setPetAttribute(rs.getShort("petattribute"));
        petItem.setPetSkill(rs.getInt("petskill"));
        petItem.setDateDead(DatabaseManager.getFileTimeFromString(rs.getString("datedead")));
        petItem.setRemainLife(rs.getInt("remainlife"));
        petItem.setAttribute(rs.getShort("attribute"));
        petItem.setActiveState(rs.getByte("activestate"));
        petItem.setAutoBuffSkill(rs.getInt("autobuffskill"));
        petItem.setPetHue(rs.getInt("pethue"));
        petItem.setGiantRate(rs.getShort("giantrate"));

        String rawExceptionList = rs.getString("exceptionList");
        petItem.setExceptionList(new ArrayList<>());
        if (rawExceptionList != null && !rawExceptionList.isEmpty()) {
            List<Integer> exceptionList = Arrays.stream(rawExceptionList.split(","))
                    .map(Integer::parseInt)
                    .toList();
            petItem.setExceptionList(exceptionList);
        }
        // --- Kết thúc phần code từ PetItem ---
    }

    static void loadCommonItemData(Item item, ResultSet rs) throws SQLException, ParseException {
        item.setId(rs.getLong("id"));
        item.setInventoryID(rs.getInt("inventoryid"));
        item.setCharID(rs.getInt("charid"));
        item.setTrunkID(rs.getInt("trunkid"));
        item.setAuctionHouseStatus(AuctionHouseStatus.getTypeById(rs.getByte("auctionHouseStatus")));
        item.setItemId(rs.getInt("itemid"));
        item.setBagIndex(rs.getInt("bagindex"));
        item.setCashItemSerialNumber(rs.getLong("cashitemserialnumber"));
        item.setDateExpire(DatabaseManager.getFileTimeFromString(rs.getString("dateexpire")));
        item.setInvType(InvType.getInvTypeByVal(rs.getInt("invtype")));
        item.setCash(rs.getByte("iscash") != 0);
        item.setQuantity(rs.getInt("quantity"));
        item.setExpireOnLogout(rs.getByte("expireonlogout") != 0);
        item.setOwner(rs.getString("owner"));
        item.setObtainedOnce(rs.getByte("obtainedonce") != 0);
        item.setZeroShareItemID(rs.getLong("zeroShareItemID"));
        item.setItemAttribute(rs.getShort("attribute"));
        item.setBossRewardID(rs.getInt("bossrewardid"));
        item.setMobTemplateID(rs.getInt("mobtemplateid"));
        item.setPartySize(rs.getInt("partysize"));
        item.setPrice(rs.getLong("price"));
        item.setTitleOn(rs.getByte("titleon") != 0);
    }

    public static List<Item> getItemsFromSQLByInventoryID(int inventoryID) {
        String query = "SELECT i.*, e.*, p.* FROM items i " +
                "LEFT JOIN equips e ON i.id = e.itemid " +
                "LEFT JOIN petitems p ON i.id = p.itemid " +
                "WHERE i.inventoryid = ?";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, inventoryID);
            return getItemsFromSQLByQuery(ps);
        } catch (Exception e) {
            DataPrinter.send(DataPrinter.HIKARICP_ERROR, e);
            return new ArrayList<>();
        }
    }

    public static List<Item> getItemsFromSQLByTrunkID(int trunkID, boolean isCash) {
        String query = "SELECT i.*, e.*, p.* FROM items i " +
                "LEFT JOIN equips e ON i.id = e.itemid " +
                "LEFT JOIN petitems p ON i.id = p.itemid " +
                "WHERE i.trunkid = ? AND i.iscash = ?";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, trunkID);
            ps.setBoolean(2, isCash);
            return getItemsFromSQLByQuery(ps);
        } catch (Exception e) {
            DataPrinter.send(DataPrinter.HIKARICP_ERROR, e);
            return new ArrayList<>();
        }
    }

    public static Item getItemFromSQLByID(int id, boolean isCash) {
        Item item = null;
        String query =
                "SELECT it.*, e.*, p.* " +
                        "FROM items it " +
                        "LEFT JOIN equips e ON it.id = e.itemid " +
                        "LEFT JOIN petitems p ON it.id = p.itemid " +
                        "WHERE it.id = ? AND it.iscash != 0";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, id);
            ps.setBoolean(2, isCash); // Use setBoolean for clarity and correctness
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    item = createItemFromResultSet(rs);
                }
            }
        }  catch (Exception e) {
            DataPrinter.send(DataPrinter.HIKARICP_ERROR, e);
        }
        return item;
    }

    public static Item getItemFromSQLByID(long id) {
        Item item = null;
        String query =
                "SELECT it.*, e.*, p.* " +
                        "FROM items it " +
                        "LEFT JOIN equips e ON it.id = e.itemid " +
                        "LEFT JOIN petitems p ON it.id = p.itemid " +
                        "WHERE it.id = ? AND it.auctionHouseStatus != 0";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    item = createItemFromResultSet(rs);
                }
            }
        } catch (Exception e) {
            DataPrinter.send(DataPrinter.HIKARICP_ERROR, e);
        }
        return item;
    }

    public void saveToSQL() {
        StringBuilder stringBuilder = new StringBuilder();
        if (getId() == 0) { //Tức là chưa có trong SQL
            stringBuilder.append("INSERT INTO `items` (");
            stringBuilder.append("`inventoryid`, ");
            stringBuilder.append("`charid`, ");
            stringBuilder.append("`trunkid`, ");
            stringBuilder.append("`auctionHouseStatus`, ");
            stringBuilder.append("`itemid`, ");
            stringBuilder.append("`bagindex`, ");
            stringBuilder.append("`cashitemserialnumber`, ");
            stringBuilder.append("`dateexpire`, ");
            stringBuilder.append("`invtype`, ");
            stringBuilder.append("`iscash`, ");
            stringBuilder.append("`quantity`, ");
            stringBuilder.append("`expireonlogout`, ");
            stringBuilder.append("`owner`, ");
            stringBuilder.append("`obtainedonce`, ");
            stringBuilder.append("`zeroShareItemID`, ");
            stringBuilder.append("`attribute`, ");
            stringBuilder.append("`bossrewardid`, ");
            stringBuilder.append("`mobtemplateid`, ");
            stringBuilder.append("`partysize`, ");
            stringBuilder.append("`price`, ");
            stringBuilder.append("`titleon` ");
            stringBuilder.append(") VALUES (");
            if (getInventoryID() != 0) {
                stringBuilder.append(String.format("%d, ", getInventoryID()));
            } else {
                stringBuilder.append("NULL, ");
            }
            if (getCharID() != 0) {
                stringBuilder.append(String.format("%d, ", getCharID()));
            } else {
                stringBuilder.append("NULL, ");
            }
            if (getTrunkID() != 0) {
                stringBuilder.append(String.format("%d, ", getTrunkID()));
            } else {
                stringBuilder.append("NULL, ");
            }
            stringBuilder.append(String.format("%d, ", getAuctionHouseStatus().getVal()));
            stringBuilder.append(String.format("%d, ", getItemId()));
            stringBuilder.append(String.format("%d, ", getBagIndex()));
            stringBuilder.append(String.format("%d, ", getCashItemSerialNumber()));
            stringBuilder.append(String.format("'%s', ", DatabaseManager.convertToDateTimeSQL(getDateExpire())));
            stringBuilder.append(String.format("%d, ", getInvType().getVal()));
            stringBuilder.append(String.format("%d, ", isCash() ? 1 : 0));
            stringBuilder.append(String.format("%d, ", getQuantity()));
            stringBuilder.append(String.format("%d, ", isExpireOnLogout() ? 1 : 0));
            stringBuilder.append(String.format("'%s', ", DatabaseManager.getStringFilter(getOwner())));
            stringBuilder.append(String.format("%d, ", hasObtainedOnce() ? 1 : 0));
            stringBuilder.append(String.format("%d, ", getZeroShareItemID()));
            stringBuilder.append(String.format("%d, ", getItemAttribute()));
            stringBuilder.append(String.format("%d, ", getBossRewardID()));
            stringBuilder.append(String.format("%d, ", getMobTemplateID()));
            stringBuilder.append(String.format("%d, ", getPartySize()));
            stringBuilder.append(String.format("%d, ", getPrice()));
            stringBuilder.append(String.format("%d ", isTitleOn() ? 1 : 0));
            stringBuilder.append(");");
            String query = stringBuilder.toString();
            long id = DatabaseManager.executeStatementReturnID(query);
            setId(id);
            if (ItemConstants.isEquip(getItemId())) {
                Equip equip = (Equip) this;
                equip.insertEquipToSQL(id);
            } else if (ItemConstants.isPet(getItemId())) {
                PetItem petItem = (PetItem) this;
                petItem.insertPetItemToSQL(id);
            }
        } else {
            stringBuilder.append("UPDATE items SET ");
            if (getInventoryID() != 0) {
                stringBuilder.append(String.format("inventoryid = %d, ", getInventoryID()));
            } else {
                stringBuilder.append("inventoryid = NULL, ");
            }
            if (getCharID() != 0) {
                stringBuilder.append(String.format("charid = %d, ", getCharID()));
            } else {
                stringBuilder.append("charid = NULL, ");
            }
            if (getTrunkID() != 0) {
                stringBuilder.append(String.format("trunkid = %d, ", getTrunkID()));
            } else {
                stringBuilder.append("trunkid = NULL, ");
            }
            stringBuilder.append(String.format("auctionHouseStatus = %d, ", getAuctionHouseStatus().getVal()));
            //Never Change ID stringBuilder.append(String.format("itemid = %d, ", getItemId()));
            stringBuilder.append(String.format("bagindex = %d, ", getBagIndex()));
            stringBuilder.append(String.format("cashitemserialnumber = %d, ", getCashItemSerialNumber()));
            stringBuilder.append(String.format("dateexpire = '%s', ", DatabaseManager.convertToDateTimeSQL(getDateExpire())));
            stringBuilder.append(String.format("invtype = %d, ", getInvType().getVal()));
            stringBuilder.append(String.format("iscash = %d, ", isCash() ? 1 : 0));
            stringBuilder.append(String.format("quantity = %d, ", getQuantity()));
            stringBuilder.append(String.format("expireonlogout = %d, ", isExpireOnLogout() ? 1 : 0));
            stringBuilder.append(String.format("owner = '%s', ", DatabaseManager.getStringFilter(getOwner())));
            stringBuilder.append(String.format("obtainedonce = %d, ", hasObtainedOnce() ? 1 : 0));
            stringBuilder.append(String.format("zeroShareItemID = %d, ", getZeroShareItemID()));
            stringBuilder.append(String.format("attribute = %d, ", getItemAttribute()));
            stringBuilder.append(String.format("bossrewardid = %d, ", getBossRewardID()));
            stringBuilder.append(String.format("mobtemplateid = %d, ", getMobTemplateID()));
            stringBuilder.append(String.format("partysize = %d, ", getPartySize()));
            stringBuilder.append(String.format("price = %d, ", getPrice()));
            stringBuilder.append(String.format("titleon = %d ", isTitleOn() ? 1 : 0));
            stringBuilder.append(String.format("WHERE id = %d;", getId()));
            String query = stringBuilder.toString();
            DatabaseManager.executeStatement(query);
            if (ItemConstants.isEquip(getItemId())) {
                Equip equip = (Equip) this;
                equip.updateEquipToSQL();
            } else if (ItemConstants.isPet(getItemId())) {
                PetItem petItem = (PetItem) this;
                petItem.updatePetItemToSQL();
            }
        }
    }

    public void updatePositionInSQL() {
        if (getId() == 0) {
            return;
        }
        String sql = "UPDATE items SET " +
                "inventoryid = ?, " +
                "trunkid = ?, " +
                "bagindex = ?, " +
                "invtype = ?, " +
                "charid = ? " +
                "WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (getInventoryID() != 0) {
                ps.setInt(1, getInventoryID());
            } else {
                ps.setNull(1, Types.INTEGER);
            }
            if (getTrunkID() != 0) {
                ps.setInt(2, getTrunkID());
            } else {
                ps.setNull(2, Types.INTEGER);
            }
            ps.setInt(3, getBagIndex());
            ps.setInt(4, getInvType().getVal());
            if (getCharID() != 0) {
                ps.setInt(5, getCharID());
            } else {
                ps.setNull(5, Types.INTEGER);
            }
            ps.setLong(6, getId());
            ps.executeUpdate();
            DataPrinter.send(DataPrinter.HIKARICP, ps.toString(), true);
        } catch (SQLException e) {
            DataPrinter.send(DataPrinter.HIKARICP_ERROR, e);
        }
    }

    public void updateQuantityInSQL() {
        if (getId() == 0) {
            return;
        }
        String sql = "UPDATE items SET quantity = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, getQuantity());
            ps.setLong(2, getId());
            ps.executeUpdate();
            DataPrinter.send(DataPrinter.HIKARICP, ps.toString(), true);
        } catch (SQLException e) {
            DataPrinter.send(DataPrinter.HIKARICP_ERROR, e);
        }
    }

    public void updateBagIndexInSQL() {
        if (getId() == 0) {
            return;
        }
        String sql = "UPDATE items SET bagindex = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, getBagIndex());
            ps.setLong(2, getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            DataPrinter.send(DataPrinter.HIKARICP_ERROR, e);
        }
    }

    public void fillInsertStatement(PreparedStatement ps) throws SQLException {
        if (getInventoryID() != 0) {
            ps.setInt(1, getInventoryID());
        } else {
            ps.setNull(1, Types.INTEGER);
        }
        if (getCharID() != 0) {
            ps.setInt(2, getCharID());
        } else {
            ps.setNull(2, Types.INTEGER);
        }
        if (getTrunkID() != 0) {
            ps.setInt(3, getTrunkID());
        } else {
            ps.setNull(3, Types.INTEGER);
        }
        ps.setInt(4, getAuctionHouseStatus().getVal());
        ps.setInt(5, getItemId());
        ps.setInt(6, getBagIndex());
        ps.setLong(7, getCashItemSerialNumber());
        ps.setTimestamp(8, DatabaseManager.convertToDateTimeSQL(getDateExpire()));
        ps.setInt(9, getInvType().getVal());
        ps.setInt(10, isCash() ? 1 : 0);
        ps.setInt(11, getQuantity());
        ps.setInt(12, isExpireOnLogout() ? 1 : 0);
        ps.setString(13, DatabaseManager.getStringFilter(getOwner()));
        ps.setInt(14, hasObtainedOnce() ? 1 : 0);
        ps.setLong(15, getZeroShareItemID());
        ps.setInt(16, getItemAttribute());
        ps.setInt(17, getBossRewardID());
        ps.setInt(18, getMobTemplateID());
        ps.setInt(19, getPartySize());
        ps.setLong(20, getPrice());
    }

    public void fillUpdateStatement(PreparedStatement ps) throws SQLException {
        if (getInventoryID() != 0) {
            ps.setInt(1, getInventoryID());
        } else {
            ps.setNull(1, Types.INTEGER);
        }
        if (getCharID() != 0) {
            ps.setInt(2, getCharID());
        } else {
            ps.setNull(2, Types.INTEGER);
        }
        if (getTrunkID() != 0) {
            ps.setInt(3, getTrunkID());
        } else {
            ps.setNull(3, Types.INTEGER);
        }
        ps.setInt(4, getAuctionHouseStatus().getVal());
        ps.setInt(5, getBagIndex());
        ps.setLong(6, getCashItemSerialNumber());
        ps.setTimestamp(7, DatabaseManager.convertToDateTimeSQL(getDateExpire()));
        ps.setInt(8, getInvType().getVal());
        ps.setInt(9, isCash() ? 1 : 0);
        ps.setInt(10, getQuantity());
        ps.setInt(11, isExpireOnLogout() ? 1 : 0);
        ps.setString(12, DatabaseManager.getStringFilter(getOwner()));
        ps.setInt(13, hasObtainedOnce() ? 1 : 0);
        ps.setLong(14, getZeroShareItemID());
        ps.setInt(15, getItemAttribute());
        ps.setInt(16, getBossRewardID());
        ps.setInt(17, getMobTemplateID());
        ps.setInt(18, getPartySize());
        ps.setLong(19, getPrice());
        ps.setLong(20, getId());
    }

    public void deleteFromSQL() {
        Connection con = null;
        try {
            con = DatabaseManager.getConnection();
            con.setAutoCommit(false);
            deleteFromSQL(con);
            con.commit();
            setId(0);
        } catch (SQLException e) {
            if (con != null) {
                try {
                    con.rollback();
                } catch (Exception rollBack) {
                    DataPrinter.send(DataPrinter.HIKARICP_ERROR, rollBack);
                }
            }
            DataPrinter.send(DataPrinter.HIKARICP_ERROR, e);
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (Exception e) {
                    DataPrinter.send(DataPrinter.HIKARICP_ERROR, e);
                }
            }
        }
    }

    public void deleteFromSQL(Connection con) throws SQLException {
        if (ItemConstants.isEquip(getItemId())) {
            Equip equip = (Equip) this;
            deleteSubItemFromSQL(con, "equips", equip.getId());
        } else if (ItemConstants.isPet(getItemId())) {
            PetItem petItem = (PetItem) this;
            deleteSubItemFromSQL(con, "petitems", petItem.getId());
        }
        String query = "DELETE FROM `items` WHERE `id` = ?";
        try (PreparedStatement ps = con.prepareStatement(query)) {
            ps.setLong(1, getId());
            ps.executeUpdate();
            DataPrinter.send(DataPrinter.HIKARICP, ps.toString(), true);
        }
    }

    private void deleteSubItemFromSQL(Connection con, String tableName, long itemId) throws SQLException {
        String query = "DELETE FROM `" + tableName + "` WHERE `itemid` = ?";
        try (PreparedStatement ps = con.prepareStatement(query)) {
            ps.setLong(1, itemId);
            ps.executeUpdate();
            DataPrinter.send(DataPrinter.HIKARICP, ps.toString(), true);
        }
    }

    public Item() {
    }

    public Item(int itemId, int bagIndex, long cashItemSerialNumber, FileTime dateExpire, InvType invType, boolean isCash, boolean obtainedOnce, Type type) {
        this.itemId = itemId;
        this.bagIndex = bagIndex;
        this.cashItemSerialNumber = cashItemSerialNumber;
        this.dateExpire = dateExpire;
        this.invType = invType;
        this.isCash = isCash;
        this.obtainedOnce = obtainedOnce;
        this.type = type;
    }

    public Item deepCopy() {
        Item ret = new Item();
        ret.itemId = itemId;
        ret.bagIndex = bagIndex;
        ret.cashItemSerialNumber = cashItemSerialNumber;
        ret.dateExpire = dateExpire;
        ret.invType = invType;
        ret.isCash = isCash;
        ret.type = type;
        ret.owner = owner;
        ret.quantity = quantity;
        ret.expireOnLogout = expireOnLogout;
        ret.obtainedOnce = obtainedOnce;
        ret.auctionHouseStatus = auctionHouseStatus;
        return ret;
    }

    public boolean isTradable() {
        return !ItemData.getItemInfoByID(getItemId()).isTradeBlock();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getInventoryID() {
        return inventoryID;
    }

    public void setInventoryID(int inventoryID) {
        this.inventoryID = inventoryID;
    }

    public int getCharID() {
        return charID;
    }

    public void setCharID(int charID) {
        this.charID = charID;
    }

    public int getTrunkID() {
        return trunkID;
    }

    public void setTrunkID(int trunkID) {
        this.trunkID = trunkID;
    }

    public AuctionHouseStatus getAuctionHouseStatus() {
        return auctionHouseStatus;
    }

    public void setAuctionHouseStatus(AuctionHouseStatus auctionHouseStatus) {
        this.auctionHouseStatus = auctionHouseStatus;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public void drop() {
        setInventoryID(0);
        setTrunkID(0);
        setBagIndex(0);
        setCharID(0);
        updatePositionInSQL();
    }

    public void addQuantity(int amount) {
        if (amount > 0 && amount + getQuantity() > 0) {
            setQuantity(getQuantity() + amount);
            updateQuantityInSQL();
        }
    }

    public void removeQuantity(int amount) {
        if (amount > 0) {
            setQuantity(Math.max(0, getQuantity() - amount));
            updateQuantityInSQL();
        }
    }

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    public int getBagIndex() {
        return bagIndex;
    }

    public void setBagIndex(int bagIndex) {
        this.bagIndex = Math.abs(bagIndex);
    }

    public long getCashItemSerialNumber() {
        return getId();
    }

    public void setCashItemSerialNumber(long cashItemSerialNumber) {
        this.cashItemSerialNumber = cashItemSerialNumber;
    }

    public FileTime getDateExpire() {
        return dateExpire;
    }

    public void setDateExpire(FileTime dateExpire) {
        this.dateExpire = dateExpire;
    }

    // script-pack compatibility alias
    public void setExpireDate(FileTime dateExpire) {
        setDateExpire(dateExpire);
    }

    public InvType getInvType() {
        return invType;
    }

    public void setInvType(InvType invType) {
        this.invType = invType;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public boolean isCash() {
        return isCash || getInvType() == CASH || getInvType() == DECORATION;
    }

    public void setCash(boolean cash) {
        isCash = cash;
    }

    public boolean hasObtainedOnce() {
        return obtainedOnce;
    }

    public void setObtainedOnce(boolean obtainedOnce) {
        this.obtainedOnce = obtainedOnce;
    }

    public long getZeroShareItemID() {
        return zeroShareItemID;
    }

    public void setZeroShareItemID(long id) {
        this.zeroShareItemID = id;
    }

    public short getItemAttribute() {
        return attribute;
    }

    public void setItemAttribute(short attribute) {
        this.attribute = attribute;
    }

    public boolean hasAttribute(short attribute) {
        return attribute != 0 && (getItemAttribute() & attribute) == attribute;
    }

    public void addAttribute(short attribute) {
        setItemAttribute((short) (getItemAttribute() | attribute));
    }

    public void removeAttribute(short attribute) {
        setItemAttribute((short) (getItemAttribute() & ~attribute));
    }

    public int getBossRewardID() {
        return bossRewardID;
    }

    public void setBossRewardID(int bossRewardID) {
        this.bossRewardID = bossRewardID;
    }

    public int getMobTemplateID() {
        return mobTemplateID;
    }

    public void setMobTemplateID(int mobTemplateID) {
        this.mobTemplateID = mobTemplateID;
    }

    public int getPartySize() {
        return partySize;
    }

    public void setPartySize(int partySize) {
        this.partySize = partySize;
    }

    public long getPrice() {
        return price;
    }

    public void setPrice(long price) {
        this.price = price;
    }

    public boolean isTitleOn() {
        return titleOn;
    }

    public void setTitleOn(boolean titleOn) {
        this.titleOn = titleOn;
    }

    public int getPreset() {
        return preset;
    }

    public void setPreset(int preset) {
        this.preset = preset;
    }

    public static void equipZeroShareItemByBodyPart(Char chr, Item originItem, int bodyPart) {
        Item zeroShareItem = originItem.deepCopy();
        zeroShareItem.setInventoryID(chr.getEquippedInventory().getId());
        zeroShareItem.setInvType(EQUIPPED);
        chr.equip(zeroShareItem, bodyPart);
        zeroShareItem.updateToChar(chr);
        originItem.setZeroShareItemID(zeroShareItem.getId());
        zeroShareItem.setZeroShareItemID(originItem.getId());
    }

    public void encode(OutPacket outPacket) {
        int itemId = getItemId();
        outPacket.encodeByte(getType().getVal());
        // GW_ItemSlotBase
        outPacket.encodeInt(itemId);
        outPacket.encodeByte(isCash());
        if (isCash()) {
            outPacket.encodeLong(getId());
        }
        outPacket.encodeFT(FileTime.fromDate(getDateExpire().toLocalDateTime()));
        outPacket.encodeInt(getBagIndex()); // bagIndex if it's in a bag
        outPacket.encodeByte(getType() == Type.EQUIP
                || GameConstants.isIntensePowerCrystal(itemId)
                || ItemConstants.isTitleItem(itemId));
        if (getType() == Type.ITEM) {
            outPacket.encodeShort(getQuantity()); // nQuantity
            outPacket.encodeString(getOwner(), 13);
            outPacket.encodeShort(getItemAttribute()); // flag
            outPacket.encodeByte(isTitleOn()); // 1 => nickItemExpired
            if (ItemConstants.isThrowingStar(itemId) || ItemConstants.isBullet(itemId) || ItemConstants.isFamiliar(itemId) ||
                    itemId / 10000 == 288 || GameConstants.isIntensePowerCrystal(itemId)) { // idk what the last one for is, there's no items that begin with this
                outPacket.encodeLong(getId()); // not id, basically an option
            }
            outPacket.encodeInt(0); // new 196
        }
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    @Override
    public String toString() {
        return "Id: " + getId() + ", ItemId: " + getItemId() + ", Qty: " + getQuantity() + ", InvType: " + getInvType()
                + ", BagIndex: " + getBagIndex();
    }

    /**
     * Sends a packet to the given Char to show that this Item has updated.
     *
     * @param chr The Char to give the update to
     */
    public void updateToChar(Char chr) {
        short bagIndex = (short) (getInvType() == EQUIPPED ? -getBagIndex() : getBagIndex());
        chr.write(WvsContext.inventoryOperation(true, false, Add, bagIndex, (short) 0, 0, this));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Item item = (Item) o;
        return id == item.id && item.id == item.itemId && quantity == item.quantity;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, itemId);
    }

    public boolean isExpireOnLogout() {
        return expireOnLogout;
    }

    public void setExpireOnLogout(boolean expireOnLogout) {
        this.expireOnLogout = expireOnLogout;
    }

    public enum Type {
        EQUIP(1),
        ITEM(2),
        PET(3);

        private final byte val;

        Type(byte val) {
            this.val = val;
        }

        Type(int val) {
            this((byte) val);
        }

        public static Type getTypeById(int id) {
            return Arrays.stream(Type.values()).filter(type -> type.getVal() == id).findFirst().orElse(null);
        }

        public byte getVal() {
            return val;
        }
    }

    public enum AuctionHouseStatus {
        NORMAL(0),
        SELL(1),
        TRANSACTION(2),
        CLAIMED(3);

        private final byte val;

        AuctionHouseStatus(byte val) {
            this.val = val;
        }

        AuctionHouseStatus(int val) {
            this((byte) val);
        }

        public static AuctionHouseStatus getTypeById(int id) {
            return Arrays.stream(AuctionHouseStatus.values()).filter(type -> type.getVal() == id).findFirst().orElse(null);
        }

        public byte getVal() {
            return val;
        }
    }
}
