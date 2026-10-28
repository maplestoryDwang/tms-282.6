package net.swordie.ms.client.character.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.connection.packet.Tms282InventoryArrangeTranslators;
import net.swordie.ms.enums.InvType;
import javax.sql.DataSource;
import java.sql.*;
import java.util.Objects;
import java.util.logging.Logger;

/** Free, retryable capacity benefit. Never consumes a coupon; callers may consume only after success.
 * Aria supports this single conditional UPDATE, not transaction rollback. Publish only after its
 * affected-row result confirms persistence. No inventory contents or unrelated account data are saved.
 */
public final class Tms282InventoryExpansion {
    private static final int MAX = 128;
    private static final Tms282InventoryExpansion DEFAULT = new Tms282InventoryExpansion();
    private static final Logger LOG = Logger.getLogger(Tms282InventoryExpansion.class.getName());
    private final DataSource source;
    private Tms282InventoryExpansion() { source = null; }
    public Tms282InventoryExpansion(DataSource source) { this.source = Objects.requireNonNull(source); }

    public static int growInventory(Char chr, int amount, int type) { return DEFAULT.growInventoryWith(chr, amount, type); }
    public static int growStorage(Char chr, int amount) { return DEFAULT.growStorageWith(chr, amount); }
    public static int growInventoryForCash(Char chr, int amount, int type) { return DEFAULT.growInventoryForCashWith(chr,amount,type); }
    public static int remaining(Char chr, int type) {
        return Math.max(0, MAX - checkedCurrent(chr.getInventoryByType(inventoryType(type)).getSlots()));
    }
    public static int remainingForCash(Char chr,int type) {
        var inv=cashInventoryType(type);
        return Math.max(0,(type==6?256:MAX)-checkedCurrent(chr.getInventoryByType(inv).getSlots()));
    }
    public static int storageRemaining(Char chr) {
        return Math.max(0, MAX - checkedCurrent(chr.getAccount().getTrunk().getSlotCount()));
    }

    public int growInventoryWith(Char chr, int amount, int type) {
        return growInventoryWith(chr,amount,type,true);
    }
    public int growInventoryForCashWith(Char chr,int amount,int type) {
        return growInventoryWith(chr,amount,type,false);
    }
    private int growInventoryWith(Char chr,int amount,int type,boolean notify) {
        var invType = notify?inventoryType(type):cashInventoryType(type); requirePositive(amount);
        synchronized (Tms282VMatrixRuntime.ECONOMY_MONITOR) { synchronized (chr) {
            Tms282VMatrixRuntime.requireEconomicAccess(chr);
            var inventory = chr.getInventoryByType(invType);
            int before = checkedCurrent(inventory.getSlots()), after = target(before, amount, !notify&&type==6?256:MAX);
            if (after == before) return 0;
            var packet = notify?Tms282InventoryArrangeTranslators.grow(type, after):null;
            save("UPDATE inventories SET slots=? WHERE id=? AND slots=?", inventory.getId(), before, after);
            inventory.setSlots(after);
            if(notify)chr.write(packet);
            return after - before;
        } }
    }

    public int growStorageWith(Char chr, int amount) {
        requirePositive(amount);
        synchronized (Tms282VMatrixRuntime.ECONOMY_MONITOR) { synchronized (chr) {
            Tms282VMatrixRuntime.requireEconomicAccess(chr);
            var trunk = chr.getAccount().getTrunk();
            int before = checkedCurrent(trunk.getSlotCount()), after = target(before, amount);
            if (after == before) return 0;
            save("UPDATE trunks SET slotcount=? WHERE id=? AND slotcount=?", trunk.getId(), before, after);
            trunk.setSlotCount(after); // storage capacity is included when its UI is next opened
            return after - before;
        } }
    }

    private void save(String sql, int id, int before, int after) {
        if (id <= 0) throw new IllegalStateException("Capacity owner has no persisted row");
        boolean saved = false;
        try (Connection connection = source == null ? DatabaseManager.getConnection() : source.getConnection()) {
            if (!connection.getAutoCommit()) throw new SQLException("Capacity update requires an autocommit connection");
            try (var statement = connection.prepareStatement(sql)) {
                statement.setInt(1, after); statement.setInt(2, id); statement.setInt(3, before);
                int changed = statement.executeUpdate();
                if (changed != 1) throw new IllegalStateException("Capacity changed concurrently or row is missing; reload before retrying");
                saved = true;
            }
        } catch (SQLException failure) {
            if (!saved) throw new IllegalStateException("Capacity was not confirmed saved; no local benefit published", failure);
            // An acknowledged autocommit update cannot be rolled back by a later close failure.
            LOG.warning("Capacity CAS was saved; JDBC close failed (SQLState=" + failure.getSQLState() + ")");
        }
    }
    private static InvType inventoryType(int type) {
        if (type < 1 || type > 5) throw new IllegalArgumentException("Only inventory types1..5 support expansion");
        return InvType.getInvTypeByVal(type);
    }
    private static InvType cashInventoryType(int type) {
        if(type<1||type>6)throw new IllegalArgumentException("Cash expansion requires inventory types1..6");
        return InvType.getInvTypeByVal(type);
    }
    private static void requirePositive(int amount) { if (amount <= 0) throw new IllegalArgumentException("Expansion amount must be positive"); }
    private static int checkedCurrent(int current) {
        if (current < 0) throw new IllegalStateException("Invalid existing capacity");
        return current;
    }
    private static int target(int current, int amount) { return target(current,amount,MAX); }
    private static int target(int current,int amount,int maximum) { return current>=maximum?current:(int)Math.min(maximum,(long)current+amount); }
}
