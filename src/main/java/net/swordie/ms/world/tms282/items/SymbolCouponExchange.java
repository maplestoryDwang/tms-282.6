package net.swordie.ms.world.tms282.items;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.*;
import net.swordie.ms.client.character.tms282.EconomyLocks;
import net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.connection.packet.WvsContext;
import net.swordie.ms.enums.InvType;
import net.swordie.ms.enums.InventoryOperation;
import net.swordie.ms.scripts.ScriptManagerImpl;
import net.swordie.ms.world.tms282.activity.ActivityRewards;
import net.swordie.ms.world.tms282.audit.AuditLog;
import java.sql.*;
import java.util.*;

/** Six Arcane coupons: debit and prepared equipment persist together, then the live bags are updated. */
public final class SymbolCouponExchange {
    private SymbolCouponExchange() {}
    private static final Map<Integer, java.util.concurrent.CopyOnWriteArrayList<java.lang.ref.WeakReference<Char>>> UNCERTAIN = new java.util.concurrent.ConcurrentHashMap<>();
    private static final String INSERT_ITEM = "INSERT INTO items (inventoryid,charid,trunkid,auctionHouseStatus,itemid,bagindex,cashitemserialnumber,dateexpire,invtype,iscash,quantity,expireonlogout,owner,obtainedonce,zeroShareItemID,attribute,bossrewardid,mobtemplateid,partysize,price) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

    public static boolean isUncertain(Char chr) {
        var refs = chr == null ? null : UNCERTAIN.get(chr.getId());
        if (refs == null) return false;
        refs.removeIf(ref -> ref.get() == null);
        return refs.stream().anyMatch(ref -> ref.get() == chr);
    }
    public static boolean hasUncertainInstances() {
        return UNCERTAIN.values().stream().flatMap(Collection::stream).anyMatch(ref -> ref.get() != null);
    }
    public static void requireCertain(Char chr) {
        if (isUncertain(chr)) throw new IllegalStateException("符文券兌換結果待確認，請重新登入核對背包；舊角色副本不能繼續交易或存檔。");
    }
    private static void uncertain(Char chr) {
        // Char.equals compares IDs: keep weak references and compare identity, including multiple old copies.
        if (!isUncertain(chr)) UNCERTAIN.computeIfAbsent(chr.getId(), id -> new java.util.concurrent.CopyOnWriteArrayList<>()).add(new java.lang.ref.WeakReference<>(chr));
        System.out.println("[SymbolCouponExchange] uncertain transaction; reconnect required char=" + chr.getId());
        try {chr.chatScriptMessage("符文券兌換結果待確認，請重新登入後核對背包。");} catch (RuntimeException ignored) {}
    }

    public static boolean exchange(Char chr, int couponId, int symbolId, int quantity) {
        if (couponId < 2635509 || couponId > 2635514 || symbolId != 1712001 + couponId - 2635509 || quantity < 1 || quantity > 100) return false;
        if (chr == null || net.swordie.ms.connection.protocol.Protocol.active().version() != net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) return false;
        boolean[] result = {false};
        EconomyLocks.packet(chr, () -> {result[0] = exchangeLocked(chr, couponId, symbolId, quantity); return null;});
        return result[0];
    }

    private static boolean exchangeLocked(Char chr, int couponId, int symbolId, int quantity) {
            boolean committed = false;
            if (!ActivityRewards.isOnline(chr)) return false;
            try {
                Tms282VMatrixRuntime.requireEconomicAccess(chr);
                if (!Tms282ItemAvailability.isGrantable(symbolId) || !Tms282ItemAvailability.isGrantable(couponId)) return false;
                Inventory bag = chr.getEquipInventory(), consume = chr.getConsumeInventory();
                if (bag.getId() <= 0 || consume.getId() <= 0 || bag.getEmptySlots() < quantity) return false;
                Map<Item, Integer> remaining = new LinkedHashMap<>();
                int needed = quantity;
                for (Item coupon : consume.getItems()) {
                    if (coupon.getItemId() != couponId || coupon.getQuantity() <= 0 || coupon.getDateExpire() != null && coupon.getDateExpire().isExpired()) continue;
                    int taken = Math.min(needed, coupon.getQuantity()); remaining.put(coupon, coupon.getQuantity() - taken); needed -= taken;
                    if (needed == 0) break;
                }
                if (needed != 0) return false;
                List<Equip> materials = new ArrayList<>();
                Set<Integer> occupied = new HashSet<>(); bag.getItems().forEach(i -> occupied.add(i.getBagIndex()));
                int slot = 1;
                for (int i = 0; i < quantity; i++) {
                    while (occupied.contains(slot)) slot++;
                    Equip item = ScriptManagerImpl.createSymbolEquipment(chr, symbolId, 1, 0);
                    item.setInventoryID(bag.getId()); item.setCharID(chr.getId()); item.setBagIndex(slot++);
                    materials.add(item);
                }
                if (!chr.canHold(new ArrayList<Item>(materials))) return false;
                Map<Item, Long> ids = persist(chr, remaining, materials);
                committed = true;
                List<net.swordie.ms.connection.OutPacket> replies = new ArrayList<>();
                for (var entry : remaining.entrySet()) {
                    Item coupon = entry.getKey(); int taken = coupon.getQuantity() - entry.getValue();
                    coupon.setQuantity(entry.getValue());
                    if (entry.getValue() == 0) {consume.removeItem(coupon); coupon.setCharID(0); coupon.setId(0);}
                    else if (ids.containsKey(coupon)) coupon.setId(ids.get(coupon));
                    replies.add(WvsContext.inventoryOperation(true, false, entry.getValue() == 0 ? InventoryOperation.Remove : InventoryOperation.UpdateQuantity, (short) coupon.getBagIndex(), (byte) -1, 0, coupon));
                    AuditLog.itemLose(chr, coupon, taken);
                }
                for (Equip item : materials) {
                    item.setId(ids.get(item)); bag.addSwapItem(item);
                    replies.add(WvsContext.inventoryOperation(true, false, InventoryOperation.Add, (short) item.getBagIndex(), (byte) -1, 0, item));
                    AuditLog.itemGain(chr, item, 1);
                }
                // All memory changes precede replies, so a closing channel cannot leave half the bag synchronized.
                try {replies.forEach(chr::write); chr.write(WvsContext.dropPickupMessage(symbolId, (short) quantity));}
                catch (RuntimeException e) {System.out.println("[SymbolCouponExchange] committed reply failed char=" + chr.getId());}
                return true;
            } catch (SQLException | RuntimeException e) {
                if (committed) uncertain(chr);
                System.out.println("[SymbolCouponExchange] refused char=" + chr.getId() + " error=" + e.getClass().getSimpleName());
                return false;
            }
    }

    private static Map<Item, Long> persist(Char chr, Map<Item, Integer> remaining, List<Equip> materials) throws SQLException {
        Map<Item, Long> ids = new IdentityHashMap<>(); boolean committed = false, commitAttempted = false;
        try (Connection c = DatabaseManager.getConnection()) {
            for (String table : List.of("items", "equips")) {
                try (PreparedStatement p = c.prepareStatement("SELECT ENGINE FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=?")) {
                    p.setString(1, table);
                    try (ResultSet r = p.executeQuery()) {if (!r.next() || !"InnoDB".equalsIgnoreCase(r.getString(1))) throw new SQLException("Symbol exchange requires InnoDB " + table);}
                }
            }
            c.setAutoCommit(false);
            try {
                for (var entry : remaining.entrySet()) {
                    Item coupon = entry.getKey(); int left = entry.getValue();
                    if (coupon.getId() == 0) {
                        if (left > 0) {
                            Item copy = coupon.deepCopy(); copy.setQuantity(left);
                            copy.setInventoryID(coupon.getInventoryID()); copy.setCharID(chr.getId()); copy.setItemAttribute(coupon.getItemAttribute());
                            ids.put(coupon, insert(c, copy));
                        }
                        continue;
                    }
                    try (PreparedStatement p = c.prepareStatement("SELECT itemid,quantity,bagindex,invtype,inventoryid FROM items WHERE id=? AND charid=? FOR UPDATE")) {
                        p.setLong(1, coupon.getId()); p.setInt(2, chr.getId());
                        try (ResultSet r = p.executeQuery()) {
                            if (!r.next() || r.getInt(1) != coupon.getItemId() || r.getInt(2) != coupon.getQuantity() || r.getInt(3) != coupon.getBagIndex() || r.getInt(4) != InvType.CONSUME.getVal() || r.getInt(5) != coupon.getInventoryID()) throw new SQLException("Coupon snapshot mismatch");
                        }
                    }
                    String sql = left == 0 ? "DELETE FROM items WHERE id=? AND charid=? AND quantity=?" : "UPDATE items SET quantity=? WHERE id=? AND charid=? AND quantity=?";
                    try (PreparedStatement p = c.prepareStatement(sql)) {
                        int i = 1; if (left != 0) p.setInt(i++, left);
                        p.setLong(i++, coupon.getId()); p.setInt(i++, chr.getId()); p.setInt(i, coupon.getQuantity());
                        if (p.executeUpdate() != 1) throw new SQLException("Coupon debit failed");
                    }
                }
                for (Equip material : materials) {long id = insert(c, material); material.insertEquipToSQL(id, c); ids.put(material, id);}
                commitAttempted = true; c.commit(); committed = true;
            } catch (SQLException | RuntimeException e) {
                if (commitAttempted) uncertain(chr);
                try {c.rollback();} catch (SQLException rollbackFailure) {uncertain(chr); e.addSuppressed(rollbackFailure);}
                throw e;
            }
        } catch (SQLException e) {if (!committed) throw e; System.out.println("[SymbolCouponExchange] connection close failed after acknowledged commit");}
        return ids;
    }

    private static long insert(Connection c, Item item) throws SQLException {
        try (PreparedStatement p = c.prepareStatement(INSERT_ITEM, Statement.RETURN_GENERATED_KEYS)) {
            item.fillInsertStatement(p);
            if (p.executeUpdate() != 1) throw new SQLException("Symbol item insert failed");
            try (ResultSet r = p.getGeneratedKeys()) {if (!r.next() || r.getLong(1) <= 0) throw new SQLException("Symbol item id missing"); return r.getLong(1);}
        }
    }
}
