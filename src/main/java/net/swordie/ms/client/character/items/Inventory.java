package net.swordie.ms.client.character.items;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntAVLTreeSet;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.constants.GameConstants;
import net.swordie.ms.constants.ItemConstants;
import net.swordie.ms.enums.InvType;
import net.swordie.ms.loaders.ItemData;
import net.swordie.ms.loaders.containerclasses.ItemInfo;
import net.swordie.ms.util.DataPrinter;

import java.sql.*;
import java.util.*;
import java.util.concurrent.locks.StampedLock;
import java.util.stream.Collectors;

public class Inventory {
    private final StampedLock lock = new StampedLock();
    private int id;
    private List<Item> items;
    private InvType type;
    private int slots;
    private ArrayList<Item> equippedItems;
    private Int2ObjectOpenHashMap<Item> bySlot;
    private IntAVLTreeSet freeSlots;

    public static Map<Integer, Inventory> getAllInventoriesFromSQLByCharID(int charID) {
        Map<Integer, Inventory> inventoryMap = new HashMap<>();
        String query = "SELECT c.id AS charID, i.id AS inventoryid, i.type AS inventoryType, i.slots, " +
                "it.*, e.*, p.* " +
                "FROM characters c " +
                "LEFT JOIN inventories i ON c.equippedinventory = i.id OR " +
                "c.equipinventory = i.id OR c.consumeinventory = i.id OR " +
                "c.etcinventory = i.id OR c.installinventory = i.id OR " +
                "c.cashinventory = i.id OR c.decorationinventory = i.id " +
                "LEFT JOIN items it ON i.id = it.inventoryid " +
                "LEFT JOIN equips e ON it.id = e.itemid " +
                "LEFT JOIN petitems p ON it.id = p.itemid " +
                "WHERE c.id = ?";

        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, charID);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int inventoryId = rs.getInt("inventoryid");

                    // Khởi tạo đối tượng Inventory chỉ khi nó chưa tồn tại trong Map
                    if (inventoryId > 0 && !inventoryMap.containsKey(inventoryId)) {
                        Inventory inventory = new Inventory();
                        inventory.setId(inventoryId);
                        inventory.setType(InvType.getInvTypeByVal(rs.getInt("inventoryType")));
                        inventory.setSlots(rs.getInt("slots"));
                        inventory.setItems(new ArrayList<>());
                        inventoryMap.put(inventoryId, inventory);
                    }

                    // Kiểm tra nếu có vật phẩm và thêm vào Inventory tương ứng
                    if (rs.getObject("it.id") != null) {
                        Item item;
                        int itemID = rs.getInt("it.itemid");
                        if (ItemConstants.isEquip(itemID)) {
                            Equip equip = new Equip();
                            equip.setItemId(itemID);
                            Item.loadEquipData(equip, rs);
                            item = equip;
                            item.setType(Item.Type.EQUIP);
                        } else if (ItemConstants.isPet(itemID)) {
                            PetItem petItem = new PetItem();
                            Item.loadPetItemData(petItem, rs);
                            item = petItem;
                            item.setType(Item.Type.PET);
                        } else {
                            item = new Item();
                            item.setType(Item.Type.ITEM);
                        }
                        Item.loadCommonItemData(item, rs);
                        Inventory parent = inventoryMap.get(inventoryId);
                        if (net.swordie.ms.connection.protocol.Protocol.active().version()
                                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) {
                            // Older swaps could persist an EQUIP label in the EQUIPPED container.
                            // The owning inventory is authoritative; normal saves persist this correction.
                            item.setInvType(parent.getType());
                            item.setInventoryID(parent.getId());
                        }
                        parent.getItems().add(item);
                    }
                }
            }
        } catch (Exception e) {
            DataPrinter.send(DataPrinter.HIKARICP_ERROR, e);
        }
        return inventoryMap;
    }

    public void insertToSQL() {
        String query = "INSERT INTO `inventories` (" +
                "`type`, " +
                "`slots` " +
                ") VALUES (" +
                String.format("%d, ", getType().getVal()) +
                String.format("%d ", getSlots()) +
                ");";
        int id = (int) DatabaseManager.executeStatementReturnID(query);
        setId(id);
    }

    public void updateToSQL() {
        String query = "UPDATE inventories SET " +
                String.format("type = %d, ", getType().getVal()) +
                String.format("slots = %d ", getSlots()) +
                String.format("WHERE id = %d;", getId());
        DatabaseManager.executeStatement(query);
        saveItems();
    }

    public void saveItems() {
        List<Item> items = new ArrayList<>(getItems());
        if (items.isEmpty()) {
            return;
        }
        String insertSql =
                "INSERT INTO items (" +
                        "inventoryid, " +
                        "charid, " +
                        "trunkid, " +
                        "auctionHouseStatus, " +
                        "itemid, " +
                        "bagindex, " +
                        "cashitemserialnumber, " +
                        "dateexpire, " +
                        "invtype, " +
                        "iscash, " +
                        "quantity, " +
                        "expireonlogout, " +
                        "owner, " +
                        "obtainedonce, " +
                        "zeroShareItemID, " +
                        "attribute, " +
                        "bossrewardid, " +
                        "mobtemplateid, " +
                        "partysize, " +
                        "price" +
                        ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        String updateSql =
                "UPDATE items SET " +
                        "inventoryid = ?, " +
                        "charid = ?, " +
                        "trunkid = ?, " +
                        "auctionHouseStatus = ?, " +
                        // itemid cố tình KHÔNG update để giữ nguyên như code cũ
                        "bagindex = ?, " +
                        "cashitemserialnumber = ?, " +
                        "dateexpire = ?, " +
                        "invtype = ?, " +
                        "iscash = ?, " +
                        "quantity = ?, " +
                        "expireonlogout = ?, " +
                        "owner = ?, " +
                        "obtainedonce = ?, " +
                        "zeroShareItemID = ?, " +
                        "attribute = ?, " +
                        "bossrewardid = ?, " +
                        "mobtemplateid = ?, " +
                        "partysize = ?, " +
                        "price = ? " +
                        "WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement psInsert = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement psUpdate = conn.prepareStatement(updateSql)) {
            conn.setAutoCommit(false);
            List<Item> newItems = new ArrayList<>();
            List<Item> existingItems = new ArrayList<>();
            for (Item item : items) {
                if (item.getId() == 0) {
                    item.fillInsertStatement(psInsert);
                    psInsert.addBatch();
                    newItems.add(item);
                } else {
                    item.fillUpdateStatement(psUpdate);
                    psUpdate.addBatch();
                    existingItems.add(item);
                }
            }
            if (!newItems.isEmpty()) {
                psInsert.executeBatch();
                try (ResultSet rs = psInsert.getGeneratedKeys()) {
                    int idx = 0;
                    while (rs.next() && idx < newItems.size()) {
                        long generatedId = rs.getLong(1);
                        Item it = newItems.get(idx++);
                        it.setId(generatedId);
                        if (ItemConstants.isEquip(it.getItemId())) {
                            ((Equip) it).insertEquipToSQL(generatedId);
                        } else if (ItemConstants.isPet(it.getItemId())) {
                            ((PetItem) it).insertPetItemToSQL(generatedId);
                        }
                    }
                }
            }
            if (!existingItems.isEmpty()) {
                psUpdate.executeBatch();
                for (Item it : existingItems) {
                    if (ItemConstants.isEquip(it.getItemId())) {
                        ((Equip) it).updateEquipToSQL();
                    } else if (ItemConstants.isPet(it.getItemId())) {
                        ((PetItem) it).updatePetItemToSQL();
                    }
                }
            }
            conn.commit();
        } catch (SQLException e) {
            DataPrinter.send(DataPrinter.HIKARICP_ERROR, e);
        }
    }

    public void updateSlotsToSQL() {
        String query = "UPDATE inventories SET " +
                String.format("slots = %d ", getSlots()) +
                String.format("WHERE id = %d;", getId());
        DatabaseManager.executeStatement(query);
    }

    public Inventory() {
        this.items = new ArrayList<>();
    }

    public Inventory(InvType t, int slots) {
        this();
        this.type = t;
        this.slots = Math.min(slots, t.getVal() == InvType.DECORATION.getVal() ?
                GameConstants.MAX_CASH_INVENTORY_SLOTS : GameConstants.MAX_INVENTORY_SLOTS);
        long s = lock.writeLock();
        try {
            ensureInitLocked();
        } finally {
            lock.unlockWrite(s);
        }
    }

    private void ensureInitLocked() {
        if (type == null) return;

        if (type == InvType.EQUIPPED) {
            if (equippedItems == null) {
                if (items == null) {
                    equippedItems = new ArrayList<>();
                } else if (items instanceof ArrayList<Item> al) {
                    equippedItems = al;
                } else {
                    equippedItems = new ArrayList<>(items);
                }
            }
            items = equippedItems;
            return;
        }

        if (bySlot == null) bySlot = new Int2ObjectOpenHashMap<>();
        if (freeSlots == null) freeSlots = new IntAVLTreeSet();

        // rebuild index from current items list (DB load case)
        bySlot.clear();
        freeSlots.clear();

        int max = Math.min(slots, getType().getVal() == InvType.DECORATION.getVal() ?
                GameConstants.MAX_CASH_INVENTORY_SLOTS : GameConstants.MAX_INVENTORY_SLOTS);
        for (int i = 1; i <= max; i++) freeSlots.add(i);

        if (items == null) items = new ArrayList<>();
        for (Item it : items) {
            int si = it.getBagIndex();
            if (si > 0 && si <= max) {
                bySlot.put(si, it);
                freeSlots.remove(si);
            }
        }
    }

    public void rebuildIndex() {
        long s = lock.writeLock();
        try {
            ensureInitLocked();
        } finally {
            lock.unlockWrite(s);
        }
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getSlots() {
        return slots;
    }

    public void setSlots(int slots) {
        slots = Math.min(slots, getType() == InvType.DECORATION ?
                GameConstants.MAX_CASH_INVENTORY_SLOTS : GameConstants.MAX_INVENTORY_SLOTS);
        long s = lock.writeLock();
        try {
            this.slots = slots;
            ensureInitLocked();
        } finally {
            lock.unlockWrite(s);
        }
    }

    public void addSlots(int amount) {
        setSlots(getSlots() + amount);
    }

    private int firstFreeSlotLocked() {
        if (freeSlots == null || freeSlots.isEmpty()) return -1;
        return freeSlots.firstInt();
    }

    public boolean addItem(Item item, int charId) {
        long s = lock.writeLock();
        try {
            ensureInitLocked();

            item.setInventoryID(id);
            item.setInvType(type);
            item.setCharID(charId);

            if (type == InvType.EQUIPPED) {
                items.add(item);
                return true;
            }

            int slot = firstFreeSlotLocked();
            if (slot <= 0) return false;

            item.setBagIndex(slot);

            // keep both list + index in sync
            items.add(item);
            bySlot.put(slot, item);
            freeSlots.remove(slot);
            return true;
        } finally {
            lock.unlockWrite(s);
        }
    }

    public void addSwapItem(Item item) {
        long s = lock.writeLock();
        try {
            ensureInitLocked();
            if (net.swordie.ms.connection.protocol.Protocol.active().version()
                    == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) {
                item.setInvType(type);
                item.setInventoryID(id);
            }
            items.add(item);

            if (type != InvType.EQUIPPED) {
                int si = item.getBagIndex();
                if (si > 0 && si <= slots) {
                    bySlot.put(si, item);
                    freeSlots.remove(si);
                }
            }
        } finally {
            lock.unlockWrite(s);
        }
    }

    public void removeItem(Item item) {
        long s = lock.writeLock();
        try {
            ensureInitLocked();

            items.remove(item);

            if (type != InvType.EQUIPPED) {
                int si = item.getBagIndex();
                if (si > 0 && si <= slots) {
                    Item cur = bySlot.get(si);
                    if (cur == item) {
                        bySlot.remove(si);
                        freeSlots.add(si);
                    } else {
                        // fallback: remove by identity scan (rare)
                        int found = -1;
                        for (var e : bySlot.int2ObjectEntrySet()) {
                            if (e.getValue() == item) { found = e.getIntKey(); break; }
                        }
                        if (found != -1) {
                            bySlot.remove(found);
                            freeSlots.add(found);
                        }
                    }
                }
            }
        } finally {
            lock.unlockWrite(s);
        }
    }

    public int getEmptySlots() {
        if (type == InvType.EQUIPPED) {
            return 0;
        }
        long s = lock.writeLock();
        try {
            ensureInitLocked();
            return freeSlots.size();
        } finally {
            lock.unlockWrite(s);
        }
    }

    public int getFirstOpenSlot() {
        if (type == InvType.EQUIPPED) {
            return -1;
        }
        long s = lock.writeLock();
        try {
            ensureInitLocked();
            return firstFreeSlotLocked();
        } finally {
            lock.unlockWrite(s);
        }
    }

    public Item getFirstItemByBodyPart(BodyPart bodyPart) {
        List<Item> items = getItemsByBodyPart(bodyPart);
        return items != null && !items.isEmpty() ? items.getFirst() : null;
    }

    public List<Item> getItemsByBodyPart(BodyPart bodyPart) {
        if (type != InvType.EQUIPPED) {
            return Collections.emptyList();
        }
        long s = lock.writeLock();
        try {
            ensureInitLocked();
            return items.stream()
                    .filter(item -> item.getBagIndex() == bodyPart.getVal())
                    .collect(Collectors.toList());
        } finally {
            lock.unlockWrite(s);
        }
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        long s = lock.writeLock();
        try {
            this.items = items;
            this.equippedItems = null;
            ensureInitLocked();
        } finally {
            lock.unlockWrite(s);
        }
    }

    public InvType getType() {
        return type;
    }

    public void setType(InvType type) {
        long s = lock.writeLock();
        try {
            this.type = type;
            ensureInitLocked();
        } finally {
            lock.unlockWrite(s);
        }
    }

    public Item getItemBySlot(int bagIndex) {
        int idx = bagIndex < 0 ? -bagIndex : bagIndex;
        long s = lock.writeLock();
        try {
            ensureInitLocked();
            if (type == InvType.EQUIPPED) {
                for (Item it : items) if (it.getBagIndex() == idx) return it;
                return null;
            }
            return bySlot.get(idx);
        } finally {
            lock.unlockWrite(s);
        }
    }

    public Item getItemByItemID(int itemId) {
        long s = lock.readLock();
        try {
            for (Item it : items) {
                if (it.getItemId() == itemId && it.getQuantity() != 0) return it;
            }
            return null;
        } finally {
            lock.unlockRead(s);
        }
    }

    public Item getItemByItemIDAndStackable(int itemId) {
        ItemInfo ii = ItemData.getItemInfoByID(itemId);
        if (ii == null) {
            return getItemByItemID(itemId);
        }
        long s = lock.readLock();
        try {
            if (type == InvType.EQUIPPED) {
                for (Item it : equippedItems) {
                    if (it.getItemId() == itemId && it.getQuantity() < ii.getSlotMax()) return it;
                }
                return null;
            }
            for (Item it : bySlot.values()) {
                if (it.getItemId() == itemId && it.getQuantity() < ii.getSlotMax()) return it;
            }
            return null;
        } finally {
            lock.unlockRead(s);
        }
    }

    public Item getItemBySN(long sn) {
        long s = lock.readLock();
        try {
            if (type == InvType.EQUIPPED) {
                for (Item it : equippedItems) if (it.getId() == sn) return it;
                return null;
            }
            for (Item it : bySlot.values()) if (it.getId() == sn) return it;
            return null;
        } finally {
            lock.unlockRead(s);
        }
    }

    /** 282: timed items must retain their own expiry rather than merge into a permanent/shorter stack. */
    public Item getItemByItemIDAndStackable(Item incoming) {
        if (net.swordie.ms.connection.protocol.Protocol.active().version() != net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3)
            return getItemByItemIDAndStackable(incoming.getItemId());
        ItemInfo info = ItemData.getItemInfoByID(incoming.getItemId());
        if (info == null) return null;
        return getItemByItemIDAndStackable(incoming, info.getSlotMax());
    }

    /** A few cash-item grant paths use a caller-supplied stack limit. */
    public Item getItemByItemIDAndStackable(Item incoming, int slotMax) {
        if (net.swordie.ms.connection.protocol.Protocol.active().version() != net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3)
            return getItemByItemID(incoming.getItemId());
        long stamp = expiry(incoming);
        long s = lock.readLock();
        try {
            for (Item it : type == InvType.EQUIPPED ? equippedItems : bySlot.values())
                if (it.getItemId() == incoming.getItemId() && it.getQuantity() < slotMax && expiry(it) == stamp)
                    return it;
            return null;
        } finally { lock.unlockRead(s); }
    }

    public static long expiry(Item item) {
        return item.getDateExpire() == null ? net.swordie.ms.util.FileTime.MAX_TIME().toLong() : item.getDateExpire().toLong();
    }

    public boolean containsItem(int itemID) {
        return getItemByItemID(itemID) != null;
    }

    public boolean canPickUp(Item item) {
        if (net.swordie.ms.connection.protocol.Protocol.active().version() == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) {
            if (item == null || item.getInvType() != type) return false;
            if (ItemConstants.isEquip(item.getItemId())) return getEmptySlots() > 0;
            if (item.getQuantity() <= 0) return false;
            ItemInfo info = ItemData.getItemInfoByID(item.getItemId());
            int max = info == null ? 1 : Math.max(1, info.getSlotMax());
            long required = item.getQuantity();
            long s = lock.writeLock();
            try {
                ensureInitLocked();
                if (info != null && type.isStackable() && !ItemConstants.isThrowingItem(item.getItemId()) && !ItemConstants.isBullet(item.getItemId())) {
                    for (Item existing : bySlot.values())
                        if (existing.getItemId() == item.getItemId() && expiry(existing) == expiry(item)
                                && (existing.getDateExpire() == null || !existing.getDateExpire().isExpired()))
                            required -= Math.max(0, max - existing.getQuantity());
                }
                return required <= (long)freeSlots.size() * max;
            } finally { lock.unlockWrite(s); }
        }
        if (!isFull()) {
            return true;
        }
        if (!item.getInvType().isStackable()) {
            return false;
        }
        return getItemByItemIDAndStackable(item) != null;
    }

    public boolean isFull() {
        if (type == InvType.EQUIPPED) return false;
        long s = lock.writeLock();
        try {
            ensureInitLocked();
            return freeSlots.isEmpty();
        } finally {
            lock.unlockWrite(s);
        }
    }

    public void moveSlot(Item item, int newSlot) {
        long s = lock.writeLock();
        try {
            ensureInitLocked();
            if (type == InvType.EQUIPPED) {
                item.setBagIndex(newSlot);
                return;
            }
            int old = item.getBagIndex();
            if (old == newSlot) return;
            if (old > 0 && old <= slots) {
                Item cur = bySlot.get(old);
                if (cur == item) {
                    bySlot.remove(old);
                    freeSlots.add(old);
                }
            }
            if (newSlot > 0 && newSlot <= slots) {
                bySlot.put(newSlot, item);
                freeSlots.remove(newSlot);
            }
            item.setBagIndex(newSlot);
        } finally {
            lock.unlockWrite(s);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Items: ");
        for (Item item : getItems()) {
            sb.append(String.format("%d id=%d slot=%d | ", item.getItemId(), item.getId(), item.getBagIndex()));
        }
        return sb.toString();
    }
}
