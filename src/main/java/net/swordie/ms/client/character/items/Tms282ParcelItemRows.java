package net.swordie.ms.client.character.items;

import net.swordie.ms.constants.ItemConstants;
import net.swordie.ms.loaders.ItemData;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Rows of the 265 {@code items} table that are held by a Duey parcel (net.swordie.ms.world.tms282.mail).
 * A parcel item row has {@code inventoryid}, {@code charid} and {@code trunkid} NULL and bagindex 0; the parcel
 * row points at it through {@code tms282_parcel.sn}. Lives in this package only to reuse the package-private
 * 265 column loaders of {@link Item}, so an equip keeps every stat, potential and flame.
 * All methods take the caller's connection so they join the caller's transaction.
 */
public final class Tms282ParcelItemRows {
    private Tms282ParcelItemRows() { }

    /** Loads a parcel item row (including equips / petitems columns); null when missing or its item id is unknown. */
    public static Item load(Connection c, long rowId) throws SQLException {
        String query = "SELECT i.*, e.*, p.* FROM items i "
                + "LEFT JOIN equips e ON i.id = e.itemid "
                + "LEFT JOIN petitems p ON i.id = p.itemid "
                + "WHERE i.id = ? AND i.inventoryid IS NULL AND i.trunkid IS NULL";
        try (PreparedStatement ps = c.prepareStatement(query)) {
            ps.setLong(1, rowId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                int itemId = rs.getInt("itemid");
                if (ItemData.getItemDeepCopy(itemId) == null) {
                    return null;
                }
                Item item;
                if (ItemConstants.isEquip(itemId)) {
                    Equip equip = new Equip();
                    equip.setItemId(itemId);
                    Item.loadEquipData(equip, rs);
                    item = equip;
                    item.setType(Item.Type.EQUIP);
                } else if (ItemConstants.isPet(itemId)) {
                    PetItem pet = new PetItem();
                    Item.loadPetItemData(pet, rs);
                    item = pet;
                    item.setType(Item.Type.PET);
                } else {
                    item = new Item();
                    item.setType(Item.Type.ITEM);
                }
                Item.loadCommonItemData(item, rs);
                return item;
            } catch (java.text.ParseException e) {
                throw new SQLException("parcel item row " + rowId + " unreadable", e);
            }
        }
    }

    /**
     * Moves a whole inventory row into parcel custody. Guarded by the row's current owner, slot and quantity so a
     * row that changed since the in-memory check is refused (returns false; the caller rolls back).
     */
    public static boolean detach(Connection c, Item item, int charId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE items SET inventoryid = NULL, charid = NULL, trunkid = NULL, "
                + "bagindex = 0 WHERE id = ? AND inventoryid = ? AND bagindex = ? AND quantity = ? AND trunkid IS NULL "
                + "AND auctionHouseStatus = 0")) {
            ps.setLong(1, item.getId());
            ps.setInt(2, item.getInventoryID());
            ps.setInt(3, item.getBagIndex());
            ps.setInt(4, item.getQuantity());
            return ps.executeUpdate() == 1;
        }
    }

    /**
     * Inserts a detached copy of part of a stack (non-equip, non-pet only: those are never split).
     * Returns the new row id.
     */
    public static long insertDetachedStackPart(Connection c, Item source, int quantity) throws SQLException {
        if (ItemConstants.isEquip(source.getItemId()) || ItemConstants.isPet(source.getItemId())) {
            throw new SQLException("equips and pets are never split into parcels");
        }
        Item copy = source.deepCopy();
        copy.setId(0);
        copy.setInventoryID(0);
        copy.setCharID(0);
        copy.setTrunkID(0);
        copy.setBagIndex(0);
        copy.setQuantity(quantity);
        copy.setItemAttribute(source.getItemAttribute());
        copy.setAuctionHouseStatus(Item.AuctionHouseStatus.NORMAL);
        String sql = "INSERT INTO items (inventoryid, charid, trunkid, auctionHouseStatus, itemid, bagindex, "
                + "cashitemserialnumber, dateexpire, invtype, iscash, quantity, expireonlogout, owner, obtainedonce, "
                + "zeroShareItemID, attribute, bossrewardid, mobtemplateid, partysize, price) "
                + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            copy.fillInsertStatement(ps);
            if (ps.executeUpdate() != 1) {
                throw new SQLException("parcel item insert failed");
            }
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("parcel item insert returned no id");
                }
                return keys.getLong(1);
            }
        }
    }

    /** Lowers a stack in place (the sender keeps the rest). Guarded by the current quantity. */
    public static boolean reduceStack(Connection c, Item item, int take) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE items SET quantity = quantity - ? WHERE id = ? AND inventoryid = ? AND quantity = ?")) {
            ps.setInt(1, take);
            ps.setLong(2, item.getId());
            ps.setInt(3, item.getInventoryID());
            ps.setInt(4, item.getQuantity());
            return ps.executeUpdate() == 1;
        }
    }

    /** Undo of {@link #detach}: puts the row back into the slot it came from. */
    public static boolean reattach(Connection c, Item item, int charId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE items SET inventoryid = ?, charid = ?, bagindex = ? "
                + "WHERE id = ? AND inventoryid IS NULL AND trunkid IS NULL")) {
            ps.setInt(1, item.getInventoryID());
            ps.setInt(2, charId);
            ps.setInt(3, item.getBagIndex());
            ps.setLong(4, item.getId());
            return ps.executeUpdate() == 1;
        }
    }

    /** Undo of {@link #reduceStack}. */
    public static boolean restoreStack(Connection c, Item item, int take) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE items SET quantity = quantity + ? WHERE id = ? AND inventoryid = ? AND quantity = ?")) {
            ps.setInt(1, take);
            ps.setLong(2, item.getId());
            ps.setInt(3, item.getInventoryID());
            ps.setInt(4, item.getQuantity() - take);
            return ps.executeUpdate() == 1;
        }
    }

    /** Deletes a parcel item row (equips / petitems rows go with it through their ON DELETE CASCADE keys). */
    public static void delete(Connection c, long rowId) throws SQLException {
        for (String table : new String[]{"equips", "petitems"}) {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM " + table + " WHERE itemid = ?")) {
                ps.setLong(1, rowId);
                ps.executeUpdate();
            } catch (SQLException ignored) {
                // Table layout differs on some 265 databases; the items row delete below is authoritative.
            }
        }
        try (PreparedStatement ps = c.prepareStatement(
                "DELETE FROM items WHERE id = ? AND inventoryid IS NULL AND trunkid IS NULL")) {
            ps.setLong(1, rowId);
            ps.executeUpdate();
        }
    }
}
