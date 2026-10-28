package net.swordie.ms.world.shop.auctionhouse;

import net.swordie.ms.ServerConstants;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.enums.auctionhouse.AuctionState;
import net.swordie.ms.util.DataPrinter;
import net.swordie.ms.util.FileTime;

import java.sql.*;
import java.text.ParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

public class AuctionItem {

    private long id;
    private int type;
    private long itemID;
    private Item item;
    private int ownerUserId;
    private int ownerId;
    private AuctionState state;
    private String ownerName;
    private long price = 0;
    private long listingPrice = 2000;
    private long directPrice;
    private int bidUserID;
    private int bidCharID;
    private String bidUsername = "";
    private long bid;
    private FileTime endDate;
    private FileTime regDate;
    private FileTime bidDate;

    public static Set<AuctionItem> getAuctionItemsFromSQL() {
        Set<AuctionItem> auctionItems = new HashSet<>();
        Map<Long, Item> itemsMap = new HashMap<>();
        Set<Long> itemIDs = new HashSet<>();
        String query = "SELECT * FROM auction_items;";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement ps = connection.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                AuctionItem item = createAuctionItemWithoutItem(rs);
                auctionItems.add(item);
                itemIDs.add(item.getItemID());
            }
        } catch (Exception exception) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, exception);
        }
        for (Long itemID : itemIDs) {
            Item item = Item.getItemFromSQLByID(itemID);
            if (item != null) {
                itemsMap.put(itemID, item);
            }
        }
        for (AuctionItem auctionItem : auctionItems) {
            auctionItem.setItem(itemsMap.get(auctionItem.getItemID()));
        }
        return auctionItems;
    }

    private static AuctionItem createAuctionItemWithoutItem(ResultSet rs) throws SQLException, ParseException {
        AuctionItem item = new AuctionItem();
        item.setId(rs.getLong("id"));
        item.setItemID(rs.getLong("itemid"));
        item.setState(AuctionState.getByVal(rs.getByte("state")));
        item.setOwnerUserId(rs.getInt("owneraccid"));
        item.ownerId = rs.getInt("ownerid");
        item.ownerName = rs.getString("ownername");
        item.directPrice = rs.getLong("mesos");
        item.bidUserID = rs.getInt("biduserid");
        item.bidCharID = rs.getInt("bidcharid");
        item.bidDate = DatabaseManager.getFileTimeFromString(rs.getString("bidtime"));
        item.endDate = DatabaseManager.getFileTimeFromString(rs.getString("expiredtime"));
        item.regDate = DatabaseManager.getFileTimeFromString(rs.getString("starttime"));
        return item;
    }

    public void saveToSQL() {
        getItem().saveToSQL();
        try (Connection conn = DatabaseManager.getConnection()) {
            if (getId() == 0) {
                String insertQuery = "INSERT INTO `auction_items` (" +
                        "`itemid`, `state`, `owneraccid`, `ownerid`, `ownername`, `mesos`, " +
                        "`biduserid`, `bidcharid`, `bidtime`, `expiredtime`, `starttime` " +
                        ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setLong(1, getItemID());
                    ps.setInt(2, getState().getVal());
                    ps.setInt(3, getOwnerUserId());
                    ps.setInt(4, getOwnerId());
                    ps.setString(5, getOwnerName());
                    ps.setLong(6, getDirectPrice());
                    ps.setInt(7, getBidUserID());
                    ps.setInt(8, getBidCharID());
                    ps.setTimestamp(9, getBidDate() != null ? new java.sql.Timestamp(getBidDate().toMillis()) : null);
                    ps.setTimestamp(10, getEndDate() != null ? new java.sql.Timestamp(getEndDate().toMillis()) : null);
                    ps.setTimestamp(11, getRegDate() != null ? new java.sql.Timestamp(getRegDate().toMillis()) : null);
                    int affectedRows = ps.executeUpdate();
                    if (affectedRows > 0) {
                        try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                            if (generatedKeys.next()) {
                                setId(generatedKeys.getInt(1));
                            }
                        }
                    }
                }
            } else {
                String updateQuery = "UPDATE auction_items SET " +
                        "itemid = ?, state = ?, owneraccid = ?, ownerid = ?, ownername = ?, " +
                        "mesos = ?, biduserid = ?, bidcharid = ?, bidtime = ?, expiredtime = ?, starttime = ? " +
                        "WHERE id = ?";
                try (PreparedStatement ps = conn.prepareStatement(updateQuery)) {
                    ps.setLong(1, getItemID());
                    ps.setInt(2, getState().getVal());
                    ps.setInt(3, getOwnerUserId());
                    ps.setInt(4, getOwnerId());
                    ps.setString(5, getOwnerName());
                    ps.setLong(6, getDirectPrice());
                    ps.setInt(7, getBidUserID());
                    ps.setInt(8, getBidCharID());
                    ps.setTimestamp(9, getBidDate() != null ? new java.sql.Timestamp(getBidDate().toMillis()) : null);
                    ps.setTimestamp(10, getEndDate() != null ? new java.sql.Timestamp(getEndDate().toMillis()) : null);
                    ps.setTimestamp(11, getRegDate() != null ? new java.sql.Timestamp(getRegDate().toMillis()) : null);
                    ps.setLong(12, getId());
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            DataPrinter.send(DataPrinter.HIKARICP_ERROR, e);
        }
    }

    public void deleteFromSQL() {
        String query = "DELETE FROM `auction_items` WHERE " +
                String.format("`id` = %d", getId());
        DatabaseManager.executeStatement(query);
        setId(0);
    }

    public AuctionItem() {
        state = AuctionState.Init;
    }

    public AuctionItem(Item item, int ownerUserId, int ownerId, String ownerName, long mesos, long bid, int buyer,
                       FileTime expiredTime, FileTime startTime) {
        super();
        this.itemID = item.getId();
        this.item = item;
        this.ownerUserId = ownerUserId;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.directPrice = mesos;
        this.bid = bid;
        this.bidUserID = buyer;
        this.endDate = expiredTime;
        this.regDate = startTime;
        this.state = AuctionState.Done;
    }

    public AuctionItem deepCopy() {
        var copy = new AuctionItem();
        copy.type = type;
        copy.itemID = itemID;
        copy.item = item.deepCopy();
        copy.ownerUserId = ownerUserId;
        copy.ownerId = ownerId;
        copy.state = state;
        copy.ownerName = ownerName;
        copy.price = price;
        copy.listingPrice = listingPrice;
        copy.directPrice = directPrice;
        copy.bidUserID = bidUserID;
        copy.bidCharID = bidCharID;
        copy.bidUsername = bidUsername;
        copy.bid = bid;
        copy.endDate = endDate;
        copy.regDate = regDate;
        copy.bidDate = bidDate;
        return copy;
    }

    public void encode(OutPacket outPacket, Char chr, long now) {
        encode(outPacket, chr, now, (out, it) -> it.encode(out));
    }

    /** {@link #encode(OutPacket, Char, long)} with the item slot written by {@code itemWriter} (profile item layout). */
    public void encode(OutPacket outPacket, Char chr, long now, BiConsumer<OutPacket, Item> itemWriter) {
        if (price == 0) {
            price = directPrice * item.getQuantity();
        }
        byte hoursLeft = (byte) ((endDate.toMillis() - now) / 3_600_000L);
        if (hoursLeft <= 0) {
            hoursLeft = 0;
        }
        outPacket.encodeLong(id); // auction Id
        outPacket.encodeInt(0); // worldID
        outPacket.encodeByte(true); // market price (true)
        outPacket.encodeInt(item.getItemId());
        outPacket.encodeInt(item.getQuantity());
        outPacket.encodeByte(state.getVal());
        outPacket.encodeLong(price);
        outPacket.encodeLong(listingPrice); // Nếu mua thì cái này = 0
        outPacket.encodeFT(regDate);
        outPacket.encodeFT(endDate);
        outPacket.encodeFT(bidDate != null ? bidDate : FileTime.MIN_TIME());
        outPacket.encodeFT(bidDate != null ? FileTime.fromDate(bidDate.toLocalDateTime().plusYears(1)) : FileTime.MAX_TIME());
        outPacket.encodeLong(directPrice);
        outPacket.encodeLong(Double.doubleToRawLongBits(directPrice));
        outPacket.encodeInt(item.getItemId());
        outPacket.encodeInt(-100);
        outPacket.encodeByte(state.getVal() == 1 && chr.getId() == ownerId);
        if (state.getVal() == 1 && chr.getId() == ownerId) {
            outPacket.encodeLong((int) ownerUserId);
            outPacket.encodeByte(hoursLeft); // timeleft
            outPacket.encodeInt(ownerUserId);
            outPacket.encodeInt(ownerId);
            outPacket.encodeInt(0);
            outPacket.encodeFT(FileTime.MIN_TIME());
            outPacket.encodeByte(0);
        } else {
            outPacket.encodeByte(hoursLeft);
        }
        outPacket.encodeByte(state.getVal() == 3 && chr.getId() == bidCharID);
        if (state.getVal() == 3 && chr.getId() == bidCharID) {
            outPacket.encodeByte(hoursLeft);
            outPacket.encodeInt(bidUserID);
            outPacket.encodeInt(bidCharID);
            outPacket.encodeInt(bidCharID);
            outPacket.encodeFT(bidDate);
            outPacket.encodeByte(11);
        } else {
            outPacket.encodeByte(hoursLeft);
        }
        itemWriter.accept(outPacket, item);
    }

    public void encodeHistory(OutPacket outPacket, Char chr) {
        encodeHistory(outPacket, chr, (out, it) -> it.encode(out));
    }

    /** {@link #encodeHistory(OutPacket, Char)} with the item slot written by {@code itemWriter}. */
    public void encodeHistory(OutPacket outPacket, Char chr, BiConsumer<OutPacket, Item> itemWriter) {
        if (price == 0) {
            price = directPrice * item.getQuantity();
        }
        boolean boughtItem = chr.getId() == getBidCharID();
        outPacket.encodeLong(id); // auction Id
        outPacket.encodeInt(0);
        outPacket.encodeInt(boughtItem ? getBidUserID() : getOwnerUserId());
        outPacket.encodeInt(boughtItem ? getBidCharID() : getOwnerId());
        outPacket.encodeInt(getItemID());
        outPacket.encodeInt(getState().getVal());
        outPacket.encodeLong(price);
        outPacket.encodeFT(getBidDate());
        outPacket.encodeLong(listingPrice); // Nếu mua thì cái này = 0
        outPacket.encodeInt(item.getQuantity());
        outPacket.encodeInt(0);
        outPacket.encodeByte(getState().getVal() != 9);
        if (getState().getVal() != 9) {
            outPacket.encodeInt(id);
            outPacket.encodeInt(0);
            outPacket.encodeInt(getState().getVal());
            outPacket.encodeInt(item.getQuantity());
            outPacket.encodeLong(price);
            outPacket.encodeLong(-1); // Second Price
            outPacket.encodeLong(price);
            outPacket.encodeLong(directPrice);
            outPacket.encodeLong(Double.doubleToRawLongBits(directPrice));
            outPacket.encodeFT(endDate);
            outPacket.encodeFT(regDate);
            outPacket.encodeLong(0);
            outPacket.encodeInt(bidUserID);
            outPacket.encodeInt(bidCharID);
            outPacket.encodeFT(bidDate != null ? bidDate : FileTime.MIN_TIME());
            outPacket.encodeFT(bidDate != null ? FileTime.fromDate(bidDate.toLocalDateTime().plusYears(1)) : FileTime.MAX_TIME());
            outPacket.encodeByte(true);
            outPacket.encodeLong(id);
            outPacket.encodeInt(bidUserID);
            outPacket.encodeInt(bidCharID);
            outPacket.encodeString(bidUsername);
            itemWriter.accept(outPacket, item);
        } else {
            outPacket.encodeByte(true);
            itemWriter.accept(outPacket, item);
        }
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(int ownerId) {
        this.ownerId = ownerId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public void setEndDate(FileTime endDate) {
        this.endDate = endDate;
    }

    public FileTime getEndDate() {
        return endDate;
    }

    public void setRegDate(FileTime regDate) {
        this.regDate = regDate;
    }

    public FileTime getRegDate() {
        return regDate;
    }

    public long getBid() {
        return bid;
    }

    public void setBid(long set) {
        bid = set;
    }

    public long getPrice() {
        return price;
    }

    public void setPrice(long price) {
        this.price = price;
    }

    public long getListingPrice() {
        return listingPrice;
    }

    public void setListingPrice(long listingPrice) {
        this.listingPrice = listingPrice;
    }

    public long getDirectPrice() {
        return directPrice;
    }

    public void setDirectPrice(long set) {
        directPrice = set;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public int getBidUserID() {
        return bidUserID;
    }

    public void setBidUserID(int bidUserID) {
        this.bidUserID = bidUserID;
    }

    public String getBidUsername() {
        return bidUsername;
    }

    public void setBidUsername(String bidUsername) {
        this.bidUsername = bidUsername;
    }

    public AuctionState getState() {
        return state;
    }

    public void setState(AuctionState state) {
        this.state = state;
    }

    public int getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(int ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public long getItemID() {
        return itemID;
    }

    public void setItemID(long itemID) {
        this.itemID = itemID;
    }

    public int getBidCharID() {
        return bidCharID;
    }

    public void setBidCharID(int bidCharID) {
        this.bidCharID = bidCharID;
    }

    public FileTime getBidDate() {
        return bidDate;
    }

    public void setBidDate(FileTime bidDate) {
        this.bidDate = bidDate;
    }
}
