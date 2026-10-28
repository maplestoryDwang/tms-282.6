package net.swordie.ms.client;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.constants.GameConstants;
import net.swordie.ms.enums.AccountType;
import net.swordie.ms.enums.PicStatus;
import net.swordie.ms.util.DataPrinter;
import net.swordie.ms.util.FileTime;
import net.swordie.ms.util.Util;

import java.sql.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class User {
    private int id;
    private String name;
    private String password;
    private String email;
    private String pic;
    private byte clientState;
    private AccountType accountType;
    private int votePoints;
    private int donationPoints;
    private int age;
    private int vipGrade;
    private int vipPoints;
    private FileTime freeVipPointDate;
    private FileTime vipExpiredDate;
    private int nBlockReason;
    private byte gender;
    private byte msg2;
    private byte purchaseExp;
    private byte pBlockReason;
    private byte gradeCode;
    private long chatUnblockDate;
    private boolean hasCensoredNxLoginID;
    private String censoredNxLoginID;
    private int characterSlots;
    private FileTime creationDate;
    private int maplePoints;
    private int nxPrepaid;
    private Set<Account> accounts = new HashSet<>();
    private byte[] machineID;
    private FileTime banExpireDate = FileTime.MIN_TIME();
    private String banReason;
    private Char currentChr;
    private Account currentAcc;
    private Client client;
    private int lastCharID;

    public static User getUserFromSQLForAPI(String username) {
        User user = null;
        String query = "SELECT id, name, password, email, banExpireDate FROM users WHERE name = ?";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    user = new User();
                    user.setId(rs.getInt("id"));
                    user.setName(rs.getString("name"));
                    user.setPassword(rs.getString("password"));
                    user.setEmail(rs.getString("email"));
                    FileTime banExpireDate = null;
                    if (rs.getTimestamp("banExpireDate") != null) {
                        banExpireDate = FileTime.fromLong(rs.getDate("banExpireDate").getTime());
                    }
                    user.setBanExpireDate(banExpireDate);
                }
            }
        } catch (Exception e) {
            DataPrinter.send(DataPrinter.HIKARICP_ERROR, e);
        }
        return user;
    }

    public static User getUserFromSQLByName(String username) {
        User user = null;
        String query = "SELECT * FROM users WHERE name = ?";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    user = new User();
                    user.setId(rs.getInt("id"));
                    user.setName(rs.getString("name"));
                    user.setPassword(rs.getString("password"));
                    user.setPic(rs.getString("pic"));
                    user.setEmail(rs.getString("email"));
                    user.setAge(rs.getInt("age"));
                    user.setAccountType(AccountType.getByVal(rs.getInt("accounttype")));
                    user.setVotePoints(rs.getInt("votepoints"));
                    user.setDonationPoints(rs.getInt("donationpoints"));
                    user.setMaplePoints(rs.getInt("maplePoints"));
                    user.setNxPrepaid(rs.getInt("nxPrepaid"));
                    user.setClientState(rs.getByte("clientstate"));
                    user.setVipGrade(rs.getInt("vipgrade"));
                    user.setVipPoints(rs.getInt("vippoints"));
                    user.setFreeVipPointDate(DatabaseManager.getFileTimeFromString(rs.getString("freevippointdate")));
                    user.setVipExpiredDate(DatabaseManager.getFileTimeFromString(rs.getString("vipexpireddate")));
                    user.setnBlockReason(rs.getInt("nblockreason"));
                    user.setGender(rs.getByte("gender"));
                    user.setMsg2(rs.getByte("msg2"));
                    user.setPurchaseExp(rs.getByte("purchaseexp"));
                    user.setpBlockReason(rs.getByte("pblockreason"));
                    FileTime banExpireDate = null;
                    if (rs.getDate("banExpireDate") != null) {
                        banExpireDate = FileTime.fromLong(rs.getDate("banExpireDate").getTime());
                    }
                    user.setBanExpireDate(banExpireDate);
                    user.setBanReason(rs.getString("banReason"));
                    //user.setOffenseManager(rs.getInt("offensemanager"));
                    user.setChatUnblockDate(rs.getLong("chatunblockdate"));
                    user.setHasCensoredNxLoginID(rs.getByte("hascensorednxloginid") != 0);
                    user.setGradeCode(rs.getByte("gradecode"));
                    user.setCensoredNxLoginID(rs.getString("censorednxloginid"));
                    user.setCharacterSlots(rs.getInt("characterslots"));
                    FileTime creationDate = DatabaseManager.getFileTimeFromString(rs.getString("creationdate"));
                    user.setCreationDate(creationDate);
                    user.setLastCharID(rs.getInt("lastcharid"));
                }
            }
        } catch (Exception e) {
            DataPrinter.send(DataPrinter.HIKARICP_ERROR, e);
        }
        return user;
    }

    public static User getUserFromSQLByID(int userID) {
        User user = null;
        String query = "SELECT * FROM users WHERE id = ?";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, userID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    user = new User();
                    user.setId(rs.getInt("id"));
                    user.setName(rs.getString("name"));
                    user.setPassword(rs.getString("password"));
                    user.setPic(rs.getString("pic"));
                    user.setEmail(rs.getString("email"));
                    user.setAge(rs.getInt("age"));
                    user.setAccountType(AccountType.getByVal(rs.getInt("accounttype")));
                    user.setVotePoints(rs.getInt("votepoints"));
                    user.setDonationPoints(rs.getInt("donationpoints"));
                    user.setMaplePoints(rs.getInt("maplePoints"));
                    user.setNxPrepaid(rs.getInt("nxPrepaid"));
                    user.setClientState(rs.getByte("clientstate"));
                    user.setVipGrade(rs.getInt("vipgrade"));
                    user.setVipPoints(rs.getInt("vippoints"));
                    user.setFreeVipPointDate(DatabaseManager.getFileTimeFromString(rs.getString("freevippointdate")));
                    user.setVipExpiredDate(DatabaseManager.getFileTimeFromString(rs.getString("vipexpireddate")));
                    user.setnBlockReason(rs.getInt("nblockreason"));
                    user.setGender(rs.getByte("gender"));
                    user.setMsg2(rs.getByte("msg2"));
                    user.setPurchaseExp(rs.getByte("purchaseexp"));
                    user.setpBlockReason(rs.getByte("pblockreason"));
                    FileTime banExpireDate = null;
                    if (rs.getDate("banExpireDate") != null) {
                        banExpireDate = FileTime.fromLong(rs.getDate("banExpireDate").getTime());
                    }
                    user.setBanExpireDate(banExpireDate);
                    user.setBanReason(rs.getString("banReason"));
                    //user.setOffenseManager(rs.getInt("offensemanager"));
                    user.setChatUnblockDate(rs.getLong("chatunblockdate"));
                    user.setHasCensoredNxLoginID(rs.getByte("hascensorednxloginid") != 0);
                    user.setGradeCode(rs.getByte("gradecode"));
                    user.setCensoredNxLoginID(rs.getString("censorednxloginid"));
                    user.setCharacterSlots(rs.getInt("characterslots"));
                    FileTime creationDate = DatabaseManager.getFileTimeFromString(rs.getString("creationdate"));
                    user.setCreationDate(creationDate);
                    String machineIdStr = rs.getString("machineid");
                    byte[] machineId = (machineIdStr != null) ? Util.getByteArrayByString(machineIdStr) : new byte[0];
                    user.setMachineID(machineId);
                    user.setLastCharID(rs.getInt("lastcharid"));
                }
            }
        } catch (Exception e) {
            DataPrinter.send(DataPrinter.HIKARICP_ERROR, e);
        }
        return user;
    }

    public void loadAccountsData() {
        try { setAccounts(Account.getAccountsFromSQLByUserID(this)); }
        catch (SQLException e) { throw new IllegalStateException("Unable to load user world accounts", e); }
    }

    public void insertUserToSQL() {
        try (Connection con = DatabaseManager.getConnection()) {
            insertUserToSQL(con);
        } catch (Exception e) {
            DataPrinter.send(DataPrinter.HIKARICP_ERROR, e);
        }
    }

    /** Allows account creation and recovery-code storage to commit together. */
    public void insertUserToSQL(Connection con) throws SQLException {
        String query = "INSERT INTO `users` (" +
                "`name`, `password`, `pic`, `email`, `age`, `accounttype`, `votepoints`, `donationpoints`, " +
                "`maplePoints`, `nxPrepaid`, `clientstate`, `vipgrade`, `vippoints`, " +
                "`freevippointdate`, `vipexpireddate`, `nblockreason`, `machineid`, `banExpireDate`, " +
                "`banReason`, `gender`, `msg2`, `purchaseexp`, `pblockreason`, " +
                "`chatunblockdate`, `hascensorednxloginid`, `gradecode`, `censorednxloginid`, " +
                "`characterslots`, `lastcharid`, `creationdate` " +
                ") VALUES (" +
                "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            int i = 1;
            ps.setString(i++, getName());
            ps.setString(i++, getPassword());
            ps.setString(i++, getPic());
            ps.setString(i++, getEmail());
            ps.setInt(i++, getAge());
            ps.setInt(i++, getAccountType().getVal());
            ps.setInt(i++, getVotePoints());
            ps.setInt(i++, getDonationPoints());
            ps.setInt(i++, getMaplePoints());
            ps.setInt(i++, getNxPrepaid());
            ps.setInt(i++, getClientState());
            ps.setInt(i++, getVipGrade());
            ps.setInt(i++, getVipPoints());
            ps.setTimestamp(i++, getFreeVipPointDate() != null ? new java.sql.Timestamp(getFreeVipPointDate().toMillis()) : null);
            ps.setTimestamp(i++, getVipExpiredDate() != null ? new java.sql.Timestamp(getVipExpiredDate().toMillis()) : null);
            ps.setInt(i++, getnBlockReason());
            ps.setString(i++, Util.readableByteArray(getMachineID() != null ? getMachineID() : new byte[]{}));
            ps.setTimestamp(i++, getBanExpireDate() != null ? new java.sql.Timestamp(getBanExpireDate().toMillis()) : null);
            ps.setString(i++, getBanReason());
            ps.setInt(i++, getGender());
            ps.setInt(i++, getMsg2());
            ps.setInt(i++, getPurchaseExp());
            ps.setInt(i++, getpBlockReason());
            ps.setLong(i++, getChatUnblockDate());
            ps.setBoolean(i++, hasCensoredNxLoginID());
            ps.setInt(i++, getGradeCode());
            ps.setString(i++, getCensoredNxLoginID());
            ps.setInt(i++, getCharacterSlots());
            ps.setInt(i++, getLastCharID());
            ps.setTimestamp(i++, getCreationDate() != null ? new java.sql.Timestamp(getCreationDate().toMillis()) : null);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    setId(rs.getInt(1));
                }
            }
        }
    }

    /**
     * Synchronized (P7): reading password / pic and writing the row is one step under this copy's monitor, so the
     * launcher's changePassword / changePin (which update the row and every in-memory copy under the same monitors)
     * can never be undone by a save that read the old value. Nothing inside takes another lock.
     */
    public synchronized void updateUserToSQL() {
        if(net.swordie.ms.client.character.tms282.Tms282CashExpansionService.isBalanceUncertain(this))return;
        boolean walletInDatabase = net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3;
        String query = "UPDATE users SET " +
                "password = ?, pic = ?, email = ?, age = ?, accounttype = ?, votepoints = ?, " +
                "donationpoints = ?, " + (walletInDatabase ? "" : "maplePoints = ?, nxPrepaid = ?, ") + "clientstate = ?, " +
                "vipgrade = ?, vippoints = ?, freevippointdate = ?, vipexpireddate = ?, " +
                "nblockreason = ?, machineid = ?, banExpireDate = ?, banReason = ?, gender = ?, " +
                "msg2 = ?, purchaseexp = ?, pblockreason = ?, chatunblockdate = ?, " +
                "hascensorednxloginid = ?, gradecode = ?, censorednxloginid = ?, characterslots = ?, lastcharid = ?, " +
                "creationdate = ? " +
                "WHERE name = ?";

        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {

            int i = 1;
            ps.setString(i++, getPassword());
            ps.setString(i++, getPic());
            ps.setString(i++, getEmail());
            ps.setInt(i++, getAge());
            ps.setInt(i++, getAccountType().getVal());
            ps.setInt(i++, getVotePoints());
            ps.setInt(i++, getDonationPoints());
            // 282 wallet writers commit atomically/CAS. A late User copy or unknown commit ACK must never
            // restore an old wallet value while saving unrelated account fields.
            if (!walletInDatabase) {
                ps.setInt(i++, getMaplePoints());
                ps.setInt(i++, getNxPrepaid());
            }
            ps.setInt(i++, getClientState());
            ps.setInt(i++, getVipGrade());
            ps.setInt(i++, getVipPoints());
            ps.setTimestamp(i++, getFreeVipPointDate() != null ? new java.sql.Timestamp(getFreeVipPointDate().toMillis()) : null);
            ps.setTimestamp(i++, getVipExpiredDate() != null ? new java.sql.Timestamp(getVipExpiredDate().toMillis()) : null);
            ps.setInt(i++, getnBlockReason());
            ps.setString(i++, Util.readableByteArray(getMachineID() != null ? getMachineID() : new byte[]{}));
            ps.setTimestamp(i++, getBanExpireDate() != null ? new java.sql.Timestamp(getBanExpireDate().toMillis()) : null);
            ps.setString(i++, getBanReason());
            ps.setInt(i++, getGender());
            ps.setInt(i++, getMsg2());
            ps.setInt(i++, getPurchaseExp());
            ps.setInt(i++, getpBlockReason());
            ps.setLong(i++, getChatUnblockDate());
            ps.setBoolean(i++, hasCensoredNxLoginID());
            ps.setInt(i++, getGradeCode());
            ps.setString(i++, getCensoredNxLoginID());
            ps.setInt(i++, getCharacterSlots());
            ps.setInt(i++, getLastCharID());
            ps.setTimestamp(i++, getCreationDate() != null ? new java.sql.Timestamp(getCreationDate().toMillis()) : null);
            ps.setString(i++, getName());

            ps.executeUpdate();
        } catch (Exception e) {
            DataPrinter.send(DataPrinter.HIKARICP_ERROR, e);
        }
    }

    public void saveToSQL(boolean logout) {
        if(net.swordie.ms.client.character.tms282.Tms282CashExpansionService.isBalanceUncertain(this))return;
        if (getId() == 0) {
            insertUserToSQL(); // User creation
        } else {
            if (getCurrentChr() != null) {
                getCurrentChr().saveToSQL();
            }
            if (getCurrentAcc() != null) {
                getCurrentAcc().saveToSQL();
            }
            updateUserToSQL();
        }
    }

    public void updateUserClientStateToSQL() {
        updateUserFieldToSQL("clientstate", getClientState());
    }

    public void updateUserMachineIDToSQL() {
        updateUserFieldToSQL("machineid", Util.readableByteArray(getMachineID() != null ? getMachineID() : new byte[]{}));
    }

    public void updateUserCharacterSlotToSQL() {
        updateUserFieldToSQL("characterslots", getCharacterSlots());
    }

    public void updateUserVotePointToSQL() {
        updateUserFieldToSQL("votepoints", getVotePoints());
    }

    public void updateUserDonationPointToSQL() {
        updateUserFieldToSQL("donationpoints", getDonationPoints());
    }

    public void updateUserMaplePointToSQL() {
        updateUserFieldToSQL("maplepoints", getMaplePoints());
    }

    /** Existing 1:1 donation-point to MVP-point exchange, committed as one conditional update. */
    public synchronized boolean exchangeDonationForVip(int amount) {
        if (amount <= 0 || donationPoints < amount || vipPoints < 0 || amount > Integer.MAX_VALUE - vipPoints) return false;
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement p = c.prepareStatement("UPDATE users SET donationpoints=?,vippoints=? WHERE id=? AND donationpoints=? AND vippoints=?")) {
            p.setInt(1, donationPoints - amount); p.setInt(2, vipPoints + amount); p.setInt(3, getId());
            p.setInt(4, donationPoints); p.setInt(5, vipPoints);
            if (p.executeUpdate() != 1) return false;
            donationPoints -= amount; vipPoints += amount;
            return true;
        } catch (SQLException e) {
            DataPrinter.send(DataPrinter.HIKARICP_ERROR, e);
            return false;
        }
    }

    public synchronized boolean purchaseMvpMembership(int cost, int grade, FileTime expiry) {
        if (cost <= 0 || vipPoints < cost || grade < 1 || grade > 4 || expiry == null) return false;
        if (grade != Math.min(vipGrade+1,4) || cost != MvpMembership.nextCost(vipGrade)
                || !MvpMembership.mayRenew(vipGrade, vipExpiredDate == null ? null : vipExpiredDate.toLocalDateTime(), java.time.LocalDateTime.now())) return false;
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement p = c.prepareStatement("UPDATE users SET vippoints=?,vipgrade=?,vipexpireddate=? WHERE id=? AND vippoints=? AND vipgrade=?")) {
            p.setInt(1, vipPoints-cost); p.setInt(2, grade); p.setTimestamp(3, new Timestamp(expiry.toMillis()));
            p.setInt(4, getId()); p.setInt(5, vipPoints); p.setInt(6, vipGrade);
            if (p.executeUpdate() != 1) return false;
            vipPoints -= cost; vipGrade = grade; vipExpiredDate = expiry;
            return true;
        } catch (SQLException e) { DataPrinter.send(DataPrinter.HIKARICP_ERROR,e); return false; }
    }

    public void updateUserPICToSQL() {
        updateUserFieldToSQL("pic", getPic());
    }

    public void updateUserVipPointToSQL() {
        updateUserFieldToSQL("vippoints", getVipPoints());
    }

    public void updateUserLastCharIdToSQL() {
        updateUserFieldToSQL("lastcharid", getLastCharID());
    }

    private void updateUserFieldToSQL(String columnName, Object value) {
        String query = String.format("UPDATE users SET %s = ? WHERE id = ?", columnName);
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {

            if (value instanceof String) {
                ps.setString(1, (String) value);
            } else if (value instanceof Integer) {
                ps.setInt(1, (Integer) value);
            } else if (value instanceof Long) {
                ps.setLong(1, (Long) value);
            } else {
                // Handle other data types if needed
                ps.setObject(1, value);
            }

            ps.setInt(2, getId());
            ps.executeUpdate();

        } catch (Exception e) {
            DataPrinter.send(DataPrinter.HIKARICP_ERROR, e);
        }
    }

    public User() {}

    public User(String name, String password) {
        this.name = name;
        this.password = password;
        this.accountType = AccountType.Player;
        this.creationDate = FileTime.currentTime();
        this.accounts = new HashSet<>();
        this.banExpireDate = null;
        this.characterSlots = GameConstants.MAX_CHARACTER_SLOTS;
        this.pBlockReason = 3;
        this.lastCharID = 0;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public int getVipGrade() {
        return vipGrade;
    }

    public void setVipGrade(int vipGrade) {
        this.vipGrade = vipGrade;
    }

    public int getVipPoints() {
        return vipPoints;
    }

    public void setVipPoints(int vipPoints) {
        this.vipPoints = vipPoints;
    }

    public void addVipPoints(int points) {
        int newPoints = getVipPoints() + points;
        setVipPoints(newPoints);
    }

    public void deductVipPoints(int points) {
        addVipPoints(-points);
    }

    public FileTime getFreeVipPointDate() {
        return freeVipPointDate;
    }

    public void setFreeVipPointDate(FileTime freeVipPointDate) {
        this.freeVipPointDate = freeVipPointDate;
    }

    public FileTime getVipExpiredDate() {
        return vipExpiredDate;
    }

    public void setVipExpiredDate(FileTime vipExpiredDate) {
        this.vipExpiredDate = vipExpiredDate;
    }

    public int getnBlockReason() {
        return nBlockReason;
    }

    public void setnBlockReason(int nBlockReason) {
        this.nBlockReason = nBlockReason;
    }

    public FileTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(FileTime creationDate) {
        this.creationDate = creationDate;
    }

    public Char getCurrentChr() {
        return currentChr;
    }

    public void setCurrentChr(Char currentChr) {
        this.currentChr = currentChr;
    }

    public int getMaplePoints() {
        return maplePoints;
    }

    public void setMaplePoints(int maplePoints) {
        this.maplePoints = maplePoints;
    }

    public int getNxPrepaid() {
        return nxPrepaid;
    }

    public void setNxPrepaid(int nxPrepaid) {
        this.nxPrepaid = nxPrepaid;
    }

    public synchronized void addMaplePoints(int points) {
        long next=(long)getMaplePoints()+points;
        if(next<0 || next>Integer.MAX_VALUE)throw new IllegalArgumentException("Maple-point balance out of range");
        if(points==0)return;
        try(Connection c=DatabaseManager.getConnection();PreparedStatement p=c.prepareStatement(
                "UPDATE users SET maplePoints=? WHERE id=? AND maplePoints=?")) {
            p.setInt(1,(int)next);p.setInt(2,getId());p.setInt(3,getMaplePoints());
            if(p.executeUpdate()!=1)throw new IllegalStateException("Maple-point balance changed; refresh first");
            setMaplePoints((int)next);
        }catch(SQLException e){throw new IllegalStateException("Maple-point balance could not be saved",e);}
    }

    public void deductMaplePoints(int points) {
        addMaplePoints(-points);
    }

    public void addDonationPoint(int points) {
        int newPoints = getDonationPoints() + points;
        setDonationPoints(newPoints);
        updateUserDonationPointToSQL();
    }

    public void deductDonationPoints(int points) {
        addDonationPoint(-points);
    }

    public synchronized void addNXPrepaid(int prepaid) {
        long newPrepaid = (long) getNxPrepaid() + prepaid;
        if (newPrepaid < 0 || newPrepaid > Integer.MAX_VALUE) throw new IllegalArgumentException("NX balance out of range");
        if (prepaid == 0) return;
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement p = c.prepareStatement("UPDATE users SET nxPrepaid=? WHERE id=? AND nxPrepaid=?")) {
            p.setInt(1, (int) newPrepaid); p.setInt(2, getId()); p.setInt(3, getNxPrepaid());
            if (p.executeUpdate() != 1) throw new IllegalStateException("NX balance changed; refresh before purchasing");
            setNxPrepaid((int) newPrepaid);
        } catch (SQLException e) {
            throw new IllegalStateException("NX balance could not be saved", e);
        }
    }

    public void deductNXPrepaid(int prepaid) {
        addNXPrepaid(-prepaid);
    }

    public void addVotePoint(int points) {
        int newPoints = getVotePoints() + points;
        setVotePoints(newPoints);
        updateUserVotePointToSQL();
    }

    public void deductVotePoints(int points) {
        addVotePoint(-points);
    }

    public Set<Account> getAccounts() {
        return accounts;
    }

    public void setAccounts(Set<Account> accounts) {
        Set<Integer> worlds = new HashSet<>();
        for (Account account : accounts) {
            if (!worlds.add(account.getWorldId())) throw new IllegalArgumentException("Duplicate world account");
            if (account.getOwnerUserId() > 0 && account.getOwnerUserId() != getId()) throw new IllegalArgumentException("World account owner mismatch");
        }
        for (Account account : accounts) account.setUser(this);
        this.accounts = new HashSet<>(accounts);
    }

    public void addAccount(Account account) {
        if (account.getOwnerUserId() > 0 && account.getOwnerUserId() != getId()) throw new IllegalArgumentException("World account owner mismatch");
        for (Account existing : getAccounts()) {
            if (existing.getWorldId() == account.getWorldId() && existing.getId() != account.getId())
                throw new IllegalStateException("Duplicate world account");
        }
        account.setUser(this);
        getAccounts().removeIf(existing -> existing.getId() == account.getId());
        getAccounts().add(account);
    }

    public FileTime getBanExpireDate() {
        return banExpireDate;
    }

    public void setBanExpireDate(FileTime banExpireDate) {
        this.banExpireDate = banExpireDate;
    }

    public String getBanReason() {
        return banReason;
    }

    public void setBanReason(String banReason) {
        this.banReason = banReason;
    }

    public Account getCurrentAcc() {
        return currentAcc;
    }

    public void setCurrentAcc(Account currentAcc) {
        this.currentAcc = currentAcc;
    }

    public String getPic() {
        return pic;
    }

    public void setPic(String pic) {
        this.pic = pic;
    }

    public PicStatus getPicStatus() {
        return PicStatus.ENTER_PIC;
    }

    public byte getGender() {
        return gender;
    }

    public void setGender(byte gender) {
        this.gender = gender;
    }

    public byte getMsg2() {
        return msg2;
    }

    public void setMsg2(byte msg2) {
        this.msg2 = msg2;
    }

    public byte getPurchaseExp() {
        return purchaseExp;
    }

    public void setPurchaseExp(byte purchaseExp) {
        this.purchaseExp = purchaseExp;
    }

    public byte getpBlockReason() {
        return pBlockReason;
    }

    public void setpBlockReason(byte pBlockReason) {
        this.pBlockReason = pBlockReason;
    }

    public byte getGradeCode() {
        return gradeCode;
    }

    public void setGradeCode(byte gradeCode) {
        this.gradeCode = gradeCode;
    }

    public long getChatUnblockDate() {
        return chatUnblockDate;
    }

    public void setChatUnblockDate(long chatUnblockDate) {
        this.chatUnblockDate = chatUnblockDate;
    }

    public boolean hasCensoredNxLoginID() {
        return hasCensoredNxLoginID;
    }

    public void setHasCensoredNxLoginID(boolean hasCensoredNxLoginID) {
        this.hasCensoredNxLoginID = hasCensoredNxLoginID;
    }

    public String getCensoredNxLoginID() {
        return censoredNxLoginID;
    }

    public void setCensoredNxLoginID(String censoredNxLoginID) {
        this.censoredNxLoginID = censoredNxLoginID;
    }

    public int getCharacterSlots() {
        return characterSlots;
    }

    public void setCharacterSlots(int characterSlots) {
        this.characterSlots = Math.min(characterSlots, GameConstants.MAX_CHARACTER_SLOTS);
    }

    public void addCharacterSlots(int amount) {
        setCharacterSlots(getCharacterSlots() + amount);
    }

    public Account getAccountByWorldId(int worldId) {
        Account found = null;
        for (Account account : getAccounts()) {
            if (account.getWorldId() == worldId) {
                if (found != null) throw new IllegalStateException("Duplicate world account");
                found = account;
            }
        }
        return found;
    }

    public int getVotePoints() {
        return votePoints;
    }

    public void setVotePoints(int votePoints) {
        this.votePoints = votePoints;
    }

    public int getDonationPoints() {
        return donationPoints;
    }

    public void setDonationPoints(int donationPoints) {
        this.donationPoints = donationPoints;
    }

    public byte getClientState() {
        return clientState;
    }

    public void setClientState(byte clientState) {
        this.clientState = clientState;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public boolean hasCorrectMachineID(byte[] machineID) {
        return Arrays.equals(machineID, getMachineID());
    }

    public byte[] getMachineID() {
        return machineID;
    }

    public void setMachineID(byte[] machineID) {
        this.machineID = machineID;
    }

    public int getLastCharID() {
        return lastCharID;
    }

    public void setLastCharID(int lastCharID) {
        this.lastCharID = lastCharID;
    }
}
