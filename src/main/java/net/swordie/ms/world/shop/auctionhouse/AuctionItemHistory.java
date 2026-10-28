package net.swordie.ms.world.shop.auctionhouse;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.util.DataPrinter;
import net.swordie.ms.util.FileTime;

import java.sql.*;
import java.text.ParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AuctionItemHistory {

    private long id;
    private int auctionId;
    private long itemID;
    private int quantity;
    private Item item;
    private int ownerAccId;
    private int ownerId;
    private String ownerName;
    private int historyType;
    private long directPrice;
    private int bidUserID;
    private String bidUsername = "";
    private long bid;
    private FileTime endDate;
    private FileTime buyTime;
    private FileTime startTime;

    public static Set<AuctionItemHistory> getAuctionItemHistoriesFromSQL() {
        Set<AuctionItemHistory> auctionItemHistories = new HashSet<>();
        Map<Long, Item> itemsMap = new HashMap<>();
        Set<Long> itemIDs = new HashSet<>();
        String historyQuery = "SELECT * FROM auction_histories;";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement psHistory = connection.prepareStatement(historyQuery);
             ResultSet rsHistory = psHistory.executeQuery()) {
            while (rsHistory.next()) {
                AuctionItemHistory history = createAuctionHistoryWithoutItem(rsHistory);
                auctionItemHistories.add(history);
                itemIDs.add(history.getItemID());
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
        for (AuctionItemHistory history : auctionItemHistories) {
            history.setItem(itemsMap.get(history.getItemID()));
        }
        return auctionItemHistories;
    }

    private static AuctionItemHistory createAuctionHistoryWithoutItem(ResultSet rs) throws SQLException, ParseException {
        AuctionItemHistory history = new AuctionItemHistory();
        history.id = rs.getInt("id");
        history.auctionId = rs.getInt("auctionid");
        history.itemID = rs.getInt("itemid");
        history.quantity = rs.getInt("quantity");
        history.ownerAccId = rs.getInt("owneraccid");
        history.ownerId = rs.getInt("ownerid");
        history.ownerName = rs.getString("ownername");
        history.historyType = rs.getInt("historytype");
        history.directPrice = rs.getLong("mesos");
        history.bidUserID = rs.getInt("buyer");
        history.bidUsername = rs.getString("bidname");
        history.bid = rs.getLong("bid");
        history.endDate = DatabaseManager.getFileTimeFromString(rs.getString("expiredtime"));
        history.buyTime = DatabaseManager.getFileTimeFromString(rs.getString("buytime"));
        history.startTime = DatabaseManager.getFileTimeFromString(rs.getString("starttime"));
        return history;
    }

    public void saveToSQL() {
        getItem().saveToSQL();
        try (Connection conn = DatabaseManager.getConnection()) {
            if (getId() == 0) {
                String insertQuery = "INSERT INTO `auction_histories` (" +
                        "`auctionid`, `itemid`, `quantity`, `owneraccid`, `ownerid`, `ownername`, " +
                        "`historytype`, `mesos`, `buyer`, `bid`, `bidname`, " +
                        "`expiredtime`, `buytime`, `starttime`" +
                        ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS)) {
                    pstmt.setInt(1, getAuctionId());
                    pstmt.setLong(2, getItemID());
                    pstmt.setInt(3, getQuantity());
                    pstmt.setInt(4, getOwnerAccId());
                    pstmt.setInt(5, getOwnerId());
                    pstmt.setString(6, getOwnerName());
                    pstmt.setInt(7, getHistoryType());
                    pstmt.setLong(8, getDirectPrice());
                    pstmt.setInt(9, getBidUserID());
                    pstmt.setLong(10, getBid());
                    pstmt.setString(11, getBidUsername());
                    pstmt.setTimestamp(12, getEndDate() != null ? new java.sql.Timestamp(getEndDate().toMillis()) : null);
                    pstmt.setTimestamp(13, getBuyTime() != null ? new java.sql.Timestamp(getBuyTime().toMillis()) : null);
                    pstmt.setTimestamp(14, getStartTime() != null ? new java.sql.Timestamp(getStartTime().toMillis()) : null);
                    int affectedRows = pstmt.executeUpdate();
                    if (affectedRows > 0) {
                        try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                            if (generatedKeys.next()) {
                                setId(generatedKeys.getInt(1));
                            }
                        }
                    }
                }
            } else {
                String updateQuery = "UPDATE auction_histories SET " +
                        "auctionid = ?, itemid = ?, quantity = ?, owneraccid = ?, ownerid = ?, ownername = ?, " +
                        "historytype = ?, mesos = ?, buyer = ?, bid = ?, bidname = ?, " +
                        "expiredtime = ?, buytime = ?, starttime = ? " +
                        "WHERE id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(updateQuery)) {
                    pstmt.setInt(1, getAuctionId());
                    pstmt.setLong(2, getItemID());
                    pstmt.setLong(3, getQuantity());
                    pstmt.setInt(4, getOwnerAccId());
                    pstmt.setInt(5, getOwnerId());
                    pstmt.setString(6, getOwnerName());
                    pstmt.setInt(7, getHistoryType());
                    pstmt.setLong(8, getDirectPrice());
                    pstmt.setInt(9, getBidUserID());
                    pstmt.setLong(10, getBid());
                    pstmt.setString(11, getBidUsername());
                    pstmt.setTimestamp(12, getEndDate() != null ? new java.sql.Timestamp(getEndDate().toMillis()) : null);
                    pstmt.setTimestamp(13, getBuyTime() != null ? new java.sql.Timestamp(getBuyTime().toMillis()) : null);
                    pstmt.setTimestamp(14, getStartTime() != null ? new java.sql.Timestamp(getStartTime().toMillis()) : null);
                    pstmt.setLong(15, getId());
                }
            }
        } catch (SQLException e) {
            DataPrinter.send(DataPrinter.HIKARICP_ERROR, e);
        }
    }

    public AuctionItemHistory() {

    }

    public AuctionItemHistory(Item item, int auctionId, boolean refund,
                              int ownerAccId, int ownerId, String ownerName, int historyType,
                              long mesos, long bid, int buyer, String bidUsername,
                              FileTime expiredTime, FileTime buyTime, FileTime startTime) {
        super();
        this.itemID = item.getItemId();
        this.item = item;
        this.auctionId = auctionId;
        this.ownerAccId = ownerAccId;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.historyType = historyType;
        this.directPrice = mesos;
        this.bid = bid;
        this.bidUserID = buyer;
        this.bidUsername = bidUsername;
        this.endDate = expiredTime;
        this.buyTime = buyTime;
        this.startTime = startTime;
    }

    public void encode(OutPacket outPacket, Char chr) {
        Item item = getItem();
        boolean boughtItem = chr.getId() == getBidUserID();
        outPacket.encodeLong(getId());
        outPacket.encodeInt(getAuctionId());
        outPacket.encodeInt(boughtItem ? chr.getAccId() : getOwnerId());
        outPacket.encodeInt(boughtItem ? chr.getId() : getOwnerId());
        outPacket.encodeInt(getItemID());
        outPacket.encodeInt(getHistoryType());
        outPacket.encodeLong(getBid());
        outPacket.encodeFT(getBuyTime());
        outPacket.encodeLong(boughtItem ? 0 : 2000);
        outPacket.encodeInt(getQuantity());
        outPacket.encodeInt(0);

        outPacket.encodeByte(item != null);
        if (item != null) {
            long price = directPrice * getQuantity();
            outPacket.encodeInt(getAuctionId()); // auction Id
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);
            outPacket.encodeInt(getHistoryType() == 2 ? 3 : 0);
            outPacket.encodeLong(getHistoryType() == 2 ? price : 0); // Price
            outPacket.encodeLong(getHistoryType() == 0 ? -1 : 0); // Second Price
            outPacket.encodeLong(price);
            outPacket.encodeLong(directPrice);
            outPacket.encodeLong(Double.doubleToRawLongBits(directPrice));
            outPacket.encodeFT(getEndDate());
            outPacket.encodeFT(getStartTime());
            outPacket.encodeLong(0);
            outPacket.encodeInt(getHistoryType() == 2 ? 1 : 0);
            outPacket.encodeInt(0);
            outPacket.encodeLong(2000); // Deposit Price
            outPacket.encodeFT(getHistoryType() == 2 ? getEndDate() : FileTime.MAX_TIME());
            outPacket.encodeByte(true);
            outPacket.encodeLong(getAuctionId());
            outPacket.encodeInt(getAuctionId());
            outPacket.encodeInt(getBidUserID());
            outPacket.encodeString(getBidUsername());
            item.encode(outPacket);
        } else {
            outPacket.encodeByte(false);
        }
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
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

    public void setBuyTime(FileTime buyTime) {
        this.buyTime = buyTime;
    }

    public FileTime getBuyTime() {
        return buyTime;
    }

    public void setStartTime(FileTime startTime) {
        this.startTime = startTime;
    }

    public FileTime getStartTime() {
        return startTime;
    }

    public long getBid() {
        return bid;
    }

    public void setBid(long set) {
        bid = set;
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

    public int getHistoryType() {
        return historyType;
    }

    public void setHistoryType(int historyType) {
        this.historyType = historyType;
    }

    public int getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(int auctionId) {
        this.auctionId = auctionId;
    }

    public int getOwnerAccId() {
        return ownerAccId;
    }

    public void setOwnerAccId(int ownerAccId) {
        this.ownerAccId = ownerAccId;
    }

    public String getBidUsername() {
        return bidUsername;
    }

    public void setBidUsername(String bidUsername) {
        this.bidUsername = bidUsername;
    }

    public long getItemID() {
        return itemID;
    }

    public void setItemID(long itemID) {
        this.itemID = itemID;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
