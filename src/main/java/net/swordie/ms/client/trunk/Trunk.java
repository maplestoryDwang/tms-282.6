package net.swordie.ms.client.trunk;

import net.swordie.ms.client.character.items.Equip;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.client.character.items.PetItem;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.constants.GameConstants;
import net.swordie.ms.enums.DBChar;
import net.swordie.ms.enums.InvType;
import net.swordie.ms.util.DataPrinter;
import net.swordie.ms.util.Util;
import net.swordie.ms.world.shop.cashshop.CashItemInfo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Trunk {

    private int id;
    private List<Item> items;
    private long money;
    private int slotCount;
    private List<CashItemInfo> locker;

    public static Trunk getTrunkFromSQLByTrunkID(int trunkID) {
        Trunk trunk = null;
        String query = "SELECT * FROM trunks WHERE id = ?";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, trunkID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    trunk = new Trunk();
                    trunk.setId(trunkID);
                    trunk.setSlotCount(rs.getInt("slotcount"));
                    trunk.setMoney(rs.getLong("money"));
                    trunk.setItems(Item.getItemsFromSQLByTrunkID(trunkID, false));
                    trunk.setLocker(CashItemInfo.getCashItemInfosFromSQLByTrunkID(trunkID));
                }
            }
        } catch (Exception exception) {
            DataPrinter.send(DataPrinter.HIKARICP_ERROR, exception);
        }
        return trunk;
    }

    public void saveToSQL() {
        String sql;
        boolean isInsert = (getId() == 0);
        if (isInsert) {
            sql = "INSERT INTO `trunks` (`slotcount`, `money`) VALUES (?, ?)";
        } else {
            sql = "UPDATE `trunks` SET `slotcount` = ?, `money` = ? WHERE `id` = ?";
        }
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, isInsert ? Statement.RETURN_GENERATED_KEYS : Statement.NO_GENERATED_KEYS)) {
            ps.setInt(1, getSlotCount());
            ps.setLong(2, getMoney());
            if (!isInsert) {
                ps.setInt(3, getId());
            }
            int affectedRows = ps.executeUpdate();
            if (isInsert && affectedRows > 0) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        setId(generatedKeys.getInt(1));
                    }
                }
            }
        } catch (Exception e) {
            DataPrinter.send(DataPrinter.HIKARICP_ERROR, e);
        }
    }

    public void updateTrunkSlotToSQL() {
        String sql = "UPDATE `trunks` SET `slotcount` = ? WHERE `id` = ?";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, getSlotCount());
            ps.setInt(2, getId());
            ps.executeUpdate();
        } catch (Exception e) {
            DataPrinter.send(DataPrinter.HIKARICP_ERROR, e);
        }
    }

    public Trunk() {
    }

    public Trunk(int slotCount) {
        this.items = new CopyOnWriteArrayList<>();
        this.locker = new CopyOnWriteArrayList<>();
        this.slotCount = slotCount;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public int getSlotCount() {
        return slotCount;
    }

    public void setSlotCount(int slotCount) {
        this.slotCount = Math.min(slotCount, GameConstants.MAX_INVENTORY_SLOTS);
    }

    public void encodeItems(OutPacket outPacket) {
        outPacket.encodeByte(getSlotCount());
        for (int i = 0; i < 100; i++) {
            outPacket.encodeByte(1);
        }
        outPacket.encodeLong(getMoney());
        for (int i = 1; i <= 6; i++) {
            InvType curInvType = InvType.getInvTypeByVal(i);
            List<Item> items = getItems().stream().filter(it -> it.getInvType() == curInvType).toList();
            outPacket.encodeInt(items.size());
            for (Item item : items) {
                if (item instanceof Equip equip) {
                    equip.encode(outPacket);
                } else if (item instanceof PetItem petItem) {
                    petItem.encode(outPacket);
                } else {
                    item.encode(outPacket);
                }
            }
        }

    }

    public long getMoney() {
        return money;
    }

    public void setMoney(long money) {
        this.money = money;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean canAddMoney(long amount) {
        return (0 <= (getMoney() + amount)) && ((getMoney() + amount) <= GameConstants.MAX_MONEY);
    }

    public void addMoney(long reqMoney) {
        if (canAddMoney(reqMoney)) {
            setMoney(getMoney() + reqMoney);
        }
    }

    public void deductMoney(long reqMoney) {
        addMoney(-reqMoney);
    }

    public void addItem(Item item, short quantity) {
        item.setQuantity(quantity);
        getItems().add(item);
    }

    public Item getItemByItemID(int itemID) {
        return getItems().stream().filter(i -> i.getItemId() == itemID).findAny().orElse(null);
    }

    public void removeItem(Item item) {
        getItems().removeIf(x -> x.equals(item));
    }

    public void sortItemsByIndex() {
        // workaround for sort not being available for CopyOnWriteArrayList
        List<Item> temp = new ArrayList<>(getItems());
        temp.sort(Comparator.comparingInt(Item::getItemId));
        //temp.sort(Comparator.comparingInt(Item::getQuantity));
        getItems().clear();
        getItems().addAll(temp);
    }

    public Item getItemByIndexAndType(InvType invType, byte index) {
        List<Item> filterItem = new ArrayList<>();
        for (Item item : getItems()) {
            if (item.getInvType() == invType) {
                filterItem.add(item);
            }
        }
        return filterItem.get(index);
    }

    public List<CashItemInfo> getLocker() {
        return locker;
    }

    public void setLocker(List<CashItemInfo> locker) {
        this.locker = locker;
    }

    public void addCashItem(CashItemInfo cii) {
        getLocker().add(cii);
    }

    public CashItemInfo getLockerItemBySlot(int slot) {
        if (slot < getLocker().size()) {
            return getLocker().get(slot);
        }
        return null;
    }

    public void removeCashItemBySlot(int slot) {
        if (slot < getLocker().size()) {
            CashItemInfo cii = getLocker().get(slot);
            getLocker().removeIf(x -> x.equals(cii));
        }
    }

    public CashItemInfo getLockerItemBySn(long sn) {
        return Util.findWithPred(getLocker(), cii -> cii.getItem() != null && cii.getItem().getId() == sn);
    }

    public boolean isFull() {
        return getLocker().size() >= GameConstants.MAX_LOCKER_SIZE;
    }

    public void addSlots(int amount) {
        setSlotCount(getSlotCount() + amount);
    }
}
