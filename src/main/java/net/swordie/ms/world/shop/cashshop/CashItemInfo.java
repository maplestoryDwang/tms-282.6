package net.swordie.ms.world.shop.cashshop;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.Equip;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.util.DataPrinter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

public class CashItemInfo {

    private long id;
    private int trunkID;
    private int accountID;
    private int characterID;
    private int commodityID;
    private String buyCharacterID;
    private int paybackRate;
    private double discount;
    private int orderNo;
    private int productNo;
    private boolean refundable;
    private byte sourceFlag;
    private boolean storeBank;
    private int position;
    private Item item;

    public static List<CashItemInfo> getCashItemInfosFromSQLByTrunkID(int trunkID) {
        List<CashItemInfo> cashItemInfos = new ArrayList<>();
        String query = "SELECT * FROM cashiteminfos WHERE trunkid = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, trunkID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long id = rs.getLong("id");
                    int accountID = rs.getInt("accountid");
                    int characterID = rs.getInt("characterid");
                    int commodityID = rs.getInt("commodityid");
                    String buyCharacterID = rs.getString("buycharacterid");
                    int payBackRate = rs.getInt("paybackrate");
                    double discount = rs.getDouble("discount");
                    int orderNo = rs.getInt("orderno");
                    int productNo = rs.getInt("productno");
                    boolean refundable = rs.getByte("refundable") == 1;
                    byte sourceFlag = rs.getByte("sourceflag");
                    boolean storeBank = rs.getByte("storebank") == 1;
                    int itemID = rs.getInt("itemid");
                    int position = rs.getInt("position");

                    CashItemInfo cashItemInfo = new CashItemInfo();
                    cashItemInfo.setId(id);
                    cashItemInfo.setTrunkID(trunkID);
                    cashItemInfo.setAccountID(accountID);
                    cashItemInfo.setCharacterID(characterID);
                    cashItemInfo.setCommodityID(commodityID);
                    cashItemInfo.setBuyCharacterID(buyCharacterID);
                    cashItemInfo.setPaybackRate(payBackRate);
                    cashItemInfo.setDiscount(discount);
                    cashItemInfo.setOrderNo(orderNo);
                    cashItemInfo.setProductNo(productNo);
                    cashItemInfo.setRefundable(refundable);
                    cashItemInfo.setSourceFlag(sourceFlag);
                    cashItemInfo.setStoreBank(storeBank);
                    cashItemInfo.setPosition(position);
                    cashItemInfo.setItem(Item.getItemFromSQLByID(itemID, true));

                    if (cashItemInfo.getItem() == null) {
                        cashItemInfo.deleteCashItemInfoFromSQL();
                        continue;
                    }
                    cashItemInfos.add(cashItemInfo);
                }
            }
        } catch (Exception exception) {
            DataPrinter.send(DataPrinter.HIKARICP_ERROR, exception);
        }
        return cashItemInfos;
    }

    public void updateCashItemInfoToSQL() {
        getItem().saveToSQL();
        if (getId() == 0) {
            String query = "INSERT INTO `cashiteminfos` (" +
                    "`accountid`, " +
                    "`characterid`, " +
                    "`commodityid`, " +
                    "`buycharacterid`, " +
                    "`paybackrate`, " +
                    "`discount`, " +
                    "`orderno`, " +
                    "`productno`, " +
                    "`refundable`, " +
                    "`sourceflag`, " +
                    "`storebank`, " +
                    "`itemid`, " +
                    "`trunkid`, " +
                    "`position` " +
                    ") VALUES (" +
                    String.format("%d, ", getAccountID()) +
                    String.format("%d, ", getCharacterID()) +
                    String.format("%d, ", getCommodityID()) +
                    DatabaseManager.getSQLStringSyntax(false, "", getBuyCharacterID(), false) +
                    String.format("%d, ", getPaybackRate()) +
                    String.format("%f, ", getDiscount()) +
                    String.format("%d, ", getOrderNo()) +
                    String.format("%d, ", getProductNo()) +
                    String.format("%d, ", isRefundable() ? 1 : 0) +
                    String.format("%d, ", getSourceFlag()) +
                    String.format("%d, ", isStoreBank() ? 1 : 0) +
                    String.format("%d, ", getItem().getId()) +
                    String.format("%d, ", getTrunkID()) +
                    String.format("%d ", getPosition()) +
                    ");";
            long id = DatabaseManager.executeStatementReturnID(query);
            setId(id);
        } else {
            String query = "UPDATE cashiteminfos SET " +
                    String.format("accountid = %d, ", getAccountID()) +
                    String.format("characterid = %d, ", getCharacterID()) +
                    String.format("commodityid = %d, ", getCommodityID()) +
                    DatabaseManager.getSQLStringSyntax(true, "buycharacterid", getBuyCharacterID(), false) +
                    String.format("paybackrate = %d, ", getPaybackRate()) +
                    String.format("discount = %f, ", getDiscount()) +
                    String.format("orderno = %d, ", getOrderNo()) +
                    String.format("productno = %d, ", getProductNo()) +
                    String.format("refundable = %d, ", isRefundable() ? 1 : 0) +
                    String.format("sourceflag = %d, ", getSourceFlag()) +
                    String.format("storebank = %d, ", isStoreBank() ? 1 : 0) +
                    String.format("itemid = %d, ", getItem().getId()) +
                    String.format("trunkid = %d, ", getTrunkID()) +
                    String.format("position = %d ", getPosition()) +
                    String.format("WHERE id = %d;", getId());
            DatabaseManager.executeStatement(query);
        }
    }

    public void deleteCashItemInfoFromSQL() {
        if (getItem() != null) {
            getItem().deleteFromSQL();
        }
        String query = "DELETE FROM `cashiteminfos` WHERE " +
                String.format("`id` = %d", getId());
        DatabaseManager.executeStatement(query);
    }

    /**
     * Creates a CashItemInfo from a given cash Item. If the Item is not a cash Item, returns null.
     *
     * @param chr  the chr to which the items belongs to
     * @param item the item from which the CashItemInfo should be created from
     * @return corresponding CashItemInfo
     */
    public static CashItemInfo fromItem(Char chr, Item item) {
        if (!item.isCash()) {
            return null;
        }
        CashItemInfo cii = new CashItemInfo();
        cii.setAccountID(chr.getAccId());
        cii.setCommodityID(1); // could grab this from cashshop sql
        cii.setItem(item);
        cii.setTrunkID(chr.getAccount().getTrunk().getId());
        return cii;
    }

    public void encode(OutPacket outPacket) {
        outPacket.encodeLong(item.getId());
        outPacket.encodeInt(getAccountID());
        outPacket.encodeInt(0); // v265.1
        outPacket.encodeInt(item.getItemId());
        outPacket.encodeInt(getCommodityID());
        outPacket.encodeShort(item.getQuantity());
        outPacket.encodeString(getBuyCharacterID(), 13); // gifter
        outPacket.encodeFT(item.getDateExpire().toLocalDateTime());
        outPacket.encodeInt(getPaybackRate()); // 0
        outPacket.encodeLong((long) getDiscount()); // 0
        outPacket.encodeInt(getOrderNo()); // 124804229
        outPacket.encodeInt(getProductNo()); // 150373
        outPacket.encodeByte(isRefundable()); // 0
        outPacket.encodeByte(getSourceFlag()); // 1
        outPacket.encodeByte(isStoreBank()); // 0
        // GW_CashItemOption::Decode
        outPacket.encodeByte(true); // v214
        item.encode(outPacket);
    }

    public int getTrunkID() {
        return trunkID;
    }

    public void setTrunkID(int trunkID) {
        this.trunkID = trunkID;
    }

    public int getAccountID() {
        return accountID;
    }

    public void setAccountID(int accountID) {
        this.accountID = accountID;
    }

    public int getCharacterID() {
        return characterID;
    }

    public void setCharacterID(int characterID) {
        this.characterID = characterID;
    }

    public int getCommodityID() {
        return commodityID;
    }

    public void setCommodityID(int commodityID) {
        this.commodityID = commodityID;
    }

    public String getBuyCharacterID() {
        return buyCharacterID;
    }

    public void setBuyCharacterID(String buyCharacterID) {
        this.buyCharacterID = buyCharacterID;
    }

    public int getPaybackRate() {
        return paybackRate;
    }

    public void setPaybackRate(int paybackRate) {
        this.paybackRate = paybackRate;
    }

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
    }

    public int getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(int orderNo) {
        this.orderNo = orderNo;
    }

    public int getProductNo() {
        return productNo;
    }

    public void setProductNo(int productNo) {
        this.productNo = productNo;
    }

    public boolean isRefundable() {
        return refundable;
    }

    public void setRefundable(boolean refundable) {
        this.refundable = refundable;
    }

    public byte getSourceFlag() {
        return sourceFlag;
    }

    public void setSourceFlag(byte sourceFlag) {
        this.sourceFlag = sourceFlag;
    }

    public boolean isStoreBank() {
        return storeBank;
    }

    public void setStoreBank(boolean storeBank) {
        this.storeBank = storeBank;
    }

    public List<Integer> getOptions() {
        return !(item instanceof Equip)
                ? new ArrayList<>(Arrays.asList(0, 0, 0))
                : ((Equip) item).getOptions().subList(0, 3); // take the first 3 options
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, item);
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }
}
