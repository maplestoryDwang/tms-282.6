package net.swordie.ms.client.social.Friend;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.enums.social.Friend.FriendFlag;
import net.swordie.ms.util.DataPrinter;

import java.sql.*;
import java.util.HashSet;
import java.util.Set;

public class Friend {
    private int id;
    private int ownerID;
    private int ownerAccID;
    private int friendID;
    private String name;
    private byte flag; // 5 through 8 = account friend
    private int channelID;
    private String group;
    private byte mobile;
    private int friendAccountID;
    private String nickname;
    private String memo;
    private boolean inShop;

    public static Set<Friend> getAccountFriendsFromSQLByOwnerAccID(int ownerAccID) {
        Set<Friend> friends = new HashSet<>();
        String query = "SELECT * FROM friends WHERE owneraccid = ?";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, ownerAccID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Friend friend = new Friend();
                    friend.setId(rs.getInt("id"));
                    friend.setOwnerID(rs.getInt("ownerid"));
                    friend.setOwnerAccID(rs.getInt("owneraccid"));
                    friend.setFriendID(rs.getInt("friendid"));
                    friend.setFriendAccountID(rs.getInt("friendaccountid"));
                    friend.setName(rs.getString("name"));
                    friend.setFlag(rs.getByte("flag"));
                    friend.setGroup(rs.getString("groupname"));
                    friend.setMobile(rs.getByte("mobile"));
                    friend.setNickname(rs.getString("nickname"));
                    friend.setMemo(rs.getString("memo"));
                    friends.add(friend);
                }
            }
        } catch (Exception exception) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, exception);
        }
        return friends;
    }

    public static Set<Friend> getFriendsFromSQLByOwnerID(int ownerID) {
        Set<Friend> friends = new HashSet<>();
        String query = "SELECT * FROM friends WHERE ownerid = ?";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, ownerID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Friend friend = new Friend();
                    friend.setId(rs.getInt("id"));
                    friend.setOwnerID(rs.getInt("ownerid"));
                    friend.setOwnerAccID(rs.getInt("owneraccid"));
                    friend.setFriendID(rs.getInt("friendid"));
                    friend.setFriendAccountID(rs.getInt("friendaccountid"));
                    friend.setName(rs.getString("name"));
                    friend.setFlag(rs.getByte("flag"));
                    friend.setGroup(rs.getString("groupname"));
                    friend.setMobile(rs.getByte("mobile"));
                    friend.setNickname(rs.getString("nickname"));
                    friend.setMemo(rs.getString("memo"));
                    friends.add(friend);
                }
            }
        } catch (Exception exception) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, exception);
        }
        return friends;
    }

    public void saveToSQL() {
        String sql;
        boolean isInsert = (getId() == 0);
        if (isInsert) {
            sql = "INSERT INTO `friends` (" +
                    "`ownerid`, `owneraccid`, `friendid`, `friendaccountid`, `name`, `flag`, " +
                    "`groupname`, `mobile`, `nickname`, `memo`) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
        } else {
            sql = "UPDATE `friends` SET " +
                    "`ownerid` = ?, `owneraccid` = ?, `friendid` = ?, `friendaccountid` = ?, " +
                    "`name` = ?, `flag` = ?, `groupname` = ?, `mobile` = ?, " +
                    "`nickname` = ?, `memo` = ? " +
                    "WHERE `id` = ?;";
        }
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, isInsert ? Statement.RETURN_GENERATED_KEYS : Statement.NO_GENERATED_KEYS)) {
            ps.setInt(1, getOwnerID());
            ps.setInt(2, getOwnerAccID());
            ps.setInt(3, getFriendID());
            ps.setInt(4, getFriendAccountID());
            ps.setString(5, getName());
            ps.setInt(6, getFlag());
            ps.setString(7, getGroup());
            ps.setInt(8, getMobile());
            ps.setString(9, getNickname());
            ps.setString(10, getMemo());
            if (!isInsert) {
                ps.setInt(11, getId());
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

    public void deleteFriendFromSQL() {
        String query = "DELETE FROM `friends` WHERE " +
                String.format("`id` = %d", getId());
        DatabaseManager.executeStatement(query);
        setId(0);
    }

    public void encode(OutPacket outPacket) {
        outPacket.encodeInt(getFriendID());
        outPacket.encodeString(getName(), 13);
        outPacket.encodeByte(getFlag()); // 7
        outPacket.encodeInt(getChannelID()); //Channel ID = 0 is Same Character
        outPacket.encodeString(getGroup(), 17);
        outPacket.encodeByte(getMobile());
        outPacket.encodeInt(getFriendAccountID());
        outPacket.encodeString(getNickname(), 13);
        outPacket.encodeString(getMemo(), 256);
        outPacket.encodeInt(isInShop() ? 1 : 0);
    }

    public int getFriendID() {
        return friendID;
    }

    public void setFriendID(int friendID) {
        this.friendID = friendID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public byte getFlag() {
        return flag;
    }

    public void setFlag(byte flag) {
        this.flag = flag;
    }

    public void setFlag(FriendFlag flag) {
        this.flag = (byte) flag.getVal();
    }

    public int getChannelID() {
        return channelID;
    }

    public void setChannelID(int channelID) {
        this.channelID = channelID;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public byte getMobile() {
        return mobile;
    }

    public void setMobile(byte mobile) {
        this.mobile = mobile;
    }

    public int getFriendAccountID() {
        return friendAccountID;
    }

    public void setFriendAccountID(int friendAccountID) {
        this.friendAccountID = friendAccountID;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public boolean isInShop() {
        return inShop;
    }

    public void setInShop(boolean inShop) {
        this.inShop = inShop;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getOwnerID() {
        return ownerID;
    }

    public void setOwnerID(int ownerID) {
        this.ownerID = ownerID;
    }

    public int getOwnerAccID() {
        return ownerAccID;
    }

    public void setOwnerAccID(int ownerAccID) {
        this.ownerAccID = ownerAccID;
    }

    public boolean isAccount() {
        return getFlag() > 4;
    }

}
