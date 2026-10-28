package net.swordie.ms.client;

import net.swordie.ms.client.character.achievement.AchievementHandler;
import net.swordie.ms.client.character.info.MedalAchievementInfo;
import net.swordie.ms.client.character.quest.Quest;
import net.swordie.ms.client.character.quest.progress.*;
import net.swordie.ms.client.character.quest.requirement.QuestStartMinStatRequirement;
import net.swordie.ms.client.character.quest.requirement.QuestStartRequirement;
import net.swordie.ms.client.character.quest.reward.QuestBuffItemReward;
import net.swordie.ms.client.character.quest.reward.QuestExpReward;
import net.swordie.ms.client.character.quest.reward.QuestItemReward;
import net.swordie.ms.client.character.quest.reward.QuestReward;
import net.swordie.ms.client.character.skills.Skill;
import net.swordie.ms.client.character.union.UnionArtifact;
import net.swordie.ms.client.daily.DailyGift;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.MonsterCollection;
import net.swordie.ms.client.character.MonsterCollectionStore;
import net.swordie.ms.client.character.achievement.AchievementData;
import net.swordie.ms.client.character.achievement.AchievementRank;
import net.swordie.ms.client.character.union.Union;
import net.swordie.ms.client.social.Friend.Friend;
import net.swordie.ms.client.trunk.Trunk;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.connection.packet.WvsContext;
import net.swordie.ms.constants.GameConstants;
import net.swordie.ms.constants.GuildConstants;
import net.swordie.ms.constants.QuestConstants;
import net.swordie.ms.constants.SkillConstants;
import net.swordie.ms.enums.AccountType;
import net.swordie.ms.enums.QuestStatus;
import net.swordie.ms.enums.Stat;
import net.swordie.ms.life.Merchant.EmployeeTrunk;
import net.swordie.ms.loaders.QuestData;
import net.swordie.ms.loaders.StringData;
import net.swordie.ms.loaders.containerclasses.QuestInfo;
import net.swordie.ms.util.DataPrinter;
import net.swordie.ms.util.FileTime;
import net.swordie.ms.util.Util;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static net.swordie.ms.enums.ChatType.SystemNotice;
import static net.swordie.ms.enums.QuestStatus.*;

public class Account {

    private int id;
    private int ownerUserId;
    private int worldId;
    private Trunk trunk;
    private EmployeeTrunk employeeTrunk;
    private MonsterCollection monsterCollection;
    private Set<Friend> friends;
    private Set<Char> characters = new HashSet<>();
    private int rewardPoint;
    private Set<LinkSkill> linkSkills = new HashSet<>();
    private User user;
    private Char currentChr;
    private int achievementMasterRank = 0;
    private int achievementPoint;
    private int loginTheme;
    private Set<AchievementRank> achievementRanks = new HashSet<>();
    private Set<AchievementData> achievementDatas = new HashSet<>();
    private Union union;
    private Map<Integer, UnionArtifact> unionArtifacts = new HashMap<>();
    private DailyGift dailyGift;
    private Map<Integer, AccountQuest> quests = new ConcurrentHashMap<>(); //QuestID, Quest

    public static Account getAccountFromSQLByAccountID(int accountID) {
        Account account = null;
        String query = "SELECT * FROM accounts WHERE id = ?";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, accountID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    account = new Account();
                    int id = rs.getInt("id");
                    account.setId(id);
                    account.ownerUserId = rs.getInt("userid");
                    account.setWorldId(rs.getInt("worldid"));
                    account.setRewardPoint(rs.getInt("nxCredit"));
                    account.setAchievementPoint(rs.getInt("achievementPoint"));
                    account.setCharacters(Char.loadAvatarData(account.getId()));
                    account.setLinkSkills(LinkSkill.getLinkSkillsFromSQLByAccountID(id));
                    account.setFriends(Friend.getAccountFriendsFromSQLByOwnerAccID(id));
                    account.setTrunk(Trunk.getTrunkFromSQLByTrunkID(rs.getInt("trunkid")));
                    account.setAchievementDatas(AchievementData.getAchievementDatasFromSQLByAccountID(id));
                    //account.setAchievementRanks(AchievementRank.getAchievementRanksFromSQLByAccountID(id));
                    account.setUnion(Union.getUnionFromSQLByAccountID(id));
                    account.setUnionArtifacts(UnionArtifact.getUnionArtifactsFromSQLByAccountID(id));
                    account.setDailyGift(DailyGift.getDailyGiftFromSQLByAccountID(accountID));
                    account.setQuests(getAllQuestsFromSQLByAccID(accountID));
                    account.setMonsterCollection(MonsterCollectionStore.load(id));
                    account.setLoginTheme(rs.getInt("theme"));
                }
            }
        } catch (Exception exception) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, exception);
        }
        return account;
    }

    public static Account getAccountFromSQLForFriendsByAccountID(int accountID) {
        Account account = null;
        String query = "SELECT * FROM accounts WHERE id = ?";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, accountID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    account = new Account();
                    int id = rs.getInt("id");
                    account.setId(id);
                    account.setFriends(Friend.getAccountFriendsFromSQLByOwnerAccID(id));
                }
            }
        } catch (Exception exception) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, exception);
        }
        return account;
    }

    public static Account getAccountFromSQLByUserID(int userID) {
        // Compatibility callers explicitly mean the normal world, never an arbitrary user's row.
        try { return getAccountFromSQLByUserID(userID, 0); }
        catch (SQLException e) { throw new IllegalStateException("Unable to load the default world account", e); }
    }

    /** Returns null only for an absent (user, world). Query failures and duplicate identities throw. */
    public static Account getAccountFromSQLByUserID(int userID, int worldID) throws SQLException {
        validateWorldIdentity(userID, worldID);
        try (Connection con = DatabaseManager.getConnection()) {
            return getAccountFromSQLByUserID(con, userID, worldID);
        }
    }

    private record WorldAccountRow(int id, int userId, int worldId, int trunkId, int credit, int achievementPoint, int theme) { }

    private static List<WorldAccountRow> readWorldAccounts(Connection con, int userID, Integer worldID) throws SQLException {
        String query = "SELECT id, userid, worldid, trunkid, nxCredit, achievementPoint, theme FROM accounts WHERE userid = ?"
                + (worldID == null ? " ORDER BY worldid, id" : " AND worldid = ?");
        List<WorldAccountRow> rows = new ArrayList<>();
        Set<Integer> worlds = new HashSet<>();
        try (PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, userID);
            if (worldID != null) ps.setInt(2, worldID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int owner = rs.getInt("userid");
                    if (rs.wasNull() || owner != userID) throw new SQLException("World account owner mismatch");
                    int world = rs.getInt("worldid");
                    if (rs.wasNull() || world < 0 || world > 255 || (worldID != null && world != worldID))
                        throw new SQLException("Invalid world account identity");
                    if (!worlds.add(world)) throw new SQLException("Duplicate world account identity");
                    rows.add(new WorldAccountRow(rs.getInt("id"), owner, world, rs.getInt("trunkid"),
                            rs.getInt("nxCredit"), rs.getInt("achievementPoint"), rs.getInt("theme")));
                }
            }
        }
        return rows;
    }

    private static Account getAccountFromSQLByUserID(Connection con, int userID, int worldID) throws SQLException {
        List<WorldAccountRow> rows = readWorldAccounts(con, userID, worldID);
        return rows.isEmpty() ? null : loadWorldAccount(rows.getFirst(), null);
    }

    public static Set<Account> getAccountsFromSQLByUserID(User user) throws SQLException {
        Objects.requireNonNull(user, "user");
        if (user.getId() <= 0) throw new IllegalArgumentException("A persisted user is required");
        List<WorldAccountRow> rows;
        try (Connection con = DatabaseManager.getConnection()) { rows = readWorldAccounts(con, user.getId(), null); }
        Set<Account> accounts = new LinkedHashSet<>();
        for (WorldAccountRow row : rows) accounts.add(loadWorldAccount(row, user));
        return accounts;
    }

    private static Account loadWorldAccount(WorldAccountRow row, User owner) throws SQLException {
        if (owner != null && owner.getId() != row.userId()) throw new SQLException("World account owner mismatch");
        Account account = new Account();
        int id = row.id();
        account.setId(id);
        account.ownerUserId = row.userId();
        account.setWorldId(row.worldId());
        account.setUser(owner);
        account.setRewardPoint(row.credit());
        account.setAchievementPoint(row.achievementPoint());
        account.setLoginTheme(row.theme());
        account.setCharacters(Char.loadAvatarData(id));
        account.setLinkSkills(LinkSkill.getLinkSkillsFromSQLByAccountID(id));
        account.setFriends(Friend.getAccountFriendsFromSQLByOwnerAccID(id));
        account.setTrunk(Trunk.getTrunkFromSQLByTrunkID(row.trunkId()));
        if (row.trunkId() <= 0 || account.getTrunk() == null) throw new SQLException("World account trunk could not be loaded");
        account.setAchievementDatas(AchievementData.getAchievementDatasFromSQLByAccountID(id));
        account.setUnion(Union.getUnionFromSQLByAccountID(id));
        account.setUnionArtifacts(UnionArtifact.getUnionArtifactsFromSQLByAccountID(id));
        account.setDailyGift(DailyGift.getDailyGiftFromSQLByAccountID(id));
        account.setQuests(getAllQuestsFromSQLByAccID(id));
        account.setMonsterCollection(MonsterCollectionStore.load(id));
        return account;
    }

    private static void validateWorldIdentity(int userID, int worldID) {
        if (userID <= 0 || worldID < 0 || worldID > 255) throw new IllegalArgumentException("Invalid user/world identity");
    }

    /** Named locks serialize separate JVMs; the unique key also protects against older writers. */
    public static Account getOrCreateWorldAccount(User user, int worldID) throws SQLException {
        Objects.requireNonNull(user, "user");
        validateWorldIdentity(user.getId(), worldID);
        String lock = "tms:world-account:" + user.getId() + ":" + worldID;
        try (Connection con = DatabaseManager.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT GET_LOCK(?, 10)")) {
                ps.setString(1, lock);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next() || rs.getInt(1) != 1) throw new SQLException("World account creation is busy");
                }
            }
            try {
                Account account = getAccountFromSQLByUserID(con, user.getId(), worldID);
                if (account == null) {
                    requireWorldIdentityConstraint(con);
                    try (PreparedStatement ps = con.prepareStatement("SELECT id FROM users WHERE id = ?")) {
                        ps.setInt(1, user.getId());
                        try (ResultSet rs = ps.executeQuery()) { if (!rs.next()) throw new SQLException("User does not exist"); }
                    }
                    int trunkID = insertGenerated(con, "INSERT INTO trunks(slotcount, money) VALUES (?, 0)", GameConstants.DEFAULT_TRUNK_SIZE);
                    boolean inserted = false;
                    try {
                        insertGenerated(con, "INSERT INTO accounts(worldid, userid, trunkid, nxCredit, achievementPoint, monstercollectionid, employeetrunkid) VALUES (?, ?, ?, 0, 0, NULL, NULL)", worldID, user.getId(), trunkID);
                        inserted = true;
                    } catch (SQLException e) {
                        if (e.getErrorCode() != 1062) throw e;
                        // A writer not using the named lock won the unique-key race. Reload strictly below.
                    } finally {
                        if (!inserted) {
                            try (PreparedStatement ps = con.prepareStatement("DELETE FROM trunks WHERE id = ? AND NOT EXISTS (SELECT 1 FROM accounts WHERE trunkid = ?)")) {
                                ps.setInt(1, trunkID); ps.setInt(2, trunkID); ps.executeUpdate();
                            }
                        }
                    }
                    account = getAccountFromSQLByUserID(con, user.getId(), worldID);
                    if (account == null) throw new SQLException("World account creation did not produce an account");
                }
                account.setUser(user);
                user.addAccount(account);
                return account;
            } finally {
                try (PreparedStatement ps = con.prepareStatement("SELECT RELEASE_LOCK(?)")) {
                    ps.setString(1, lock); ps.executeQuery().close();
                }
            }
        }
    }

    private static int insertGenerated(Connection con, String sql, int... values) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < values.length; i++) ps.setInt(i + 1, values[i]);
            if (ps.executeUpdate() != 1) throw new SQLException("World account insert failed");
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (!rs.next() || rs.getInt(1) <= 0) throw new SQLException("World account insert did not return an ID");
                return rs.getInt(1);
            }
        }
    }

    private static void requireWorldIdentityConstraint(Connection con) throws SQLException {
        String sql = "SELECT COUNT(*) FROM (SELECT INDEX_NAME FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'accounts' AND NON_UNIQUE = 0 GROUP BY INDEX_NAME HAVING COUNT(*) = 2 AND SUM(COLUMN_NAME = 'userid') = 1 AND SUM(COLUMN_NAME = 'worldid') = 1 AND MAX(SUB_PART) IS NULL) world_keys";
        try (PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            if (!rs.next() || rs.getInt(1) == 0) throw new SQLException("The accounts(userId,worldId) unique-key migration is required");
        }
    }

    public void saveToSQL() {
        // Legacy multi-connection child/parent saves are NOT atomic (accounts/trunks may be Aria).
        // Creation callers must use getOrCreateWorldAccount rather than racing this method.
        // Step 1: Save all child objects first to get their IDs.
        if (getTrunk() != null) {
            getTrunk().saveToSQL();
        }
        if (getUnion() != null) {
            getUnion().saveToSQL();
        }
        if (getUnionArtifacts() != null && !getUnionArtifacts().isEmpty()) {
            UnionArtifact.saveToSQL(getUnionArtifacts(), getId());
        }
        if (getDailyGift() != null) {
            getDailyGift().saveToSQL();
        }
        if (getAchievementDatas() != null && !getAchievementDatas().isEmpty()) {
            updateAchievementDatas(getAchievementDatas());
        }
        if (getQuests() != null && !getQuests().isEmpty()) {
            AccountQuest.saveToSQL(getQuests(), getId());
        }
        if (monsterCollection != null && getId() > 0) {
            MonsterCollectionStore.save(getId(), monsterCollection); // 265 never saved it (tables: sql/tms282)
        }
        // Step 2: Now save the parent object (accounts).
        String sql;
        boolean isInsert = (getId() == 0);
        boolean walletInDatabase = net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3;
        if (isInsert) {
            sql = "INSERT INTO `accounts` (`worldid`, `userid`, `trunkid`, `nxCredit`, `achievementPoint`, `monstercollectionid`, `employeetrunkid`) VALUES (?, ?, ?, ?, ?, ?, ?)";
        } else {
            sql = "UPDATE `accounts` SET `worldid` = ?, `trunkid` = ?, "
                    + (walletInDatabase ? "" : "`nxCredit` = ?, ")
                    + "`achievementPoint` = ?, `theme` = ? WHERE `id` = ?";
        }
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, isInsert ? Statement.RETURN_GENERATED_KEYS : Statement.NO_GENERATED_KEYS)) {
            if (isInsert) {
                ps.setInt(1, getWorldId());
                ps.setInt(2, getUser().getId());
                ps.setInt(3, getTrunk().getId()); // Now the ID is available!
                ps.setInt(4, 0);
                ps.setInt(5, 0);
                ps.setNull(6, Types.INTEGER);
                ps.setNull(7, Types.INTEGER);
            } else {
                int i = 1;
                ps.setInt(i++, getWorldId());
                ps.setInt(i++, getTrunk().getId());
                // 282 mileage writers use CAS; a late Account snapshot must not restore spent points.
                if (!walletInDatabase) ps.setInt(i++, getRewardPoint());
                ps.setInt(i++, getAchievementPoint());
                ps.setInt(i++, getLoginTheme());
                ps.setInt(i, getId());
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

    public void updateAchievementDatas(Set<AchievementData> achievementDatas) {
        String insertSql = "INSERT INTO `achievement_datas` (`accid`, `infoid`, `missionid`, `status`, `msg`, `unlocktime`) VALUES (?, ?, ?, ?, ?, ?)";
        String updateSql = "UPDATE `achievement_datas` SET `accid` = ?, `infoid` = ?, `missionid` = ?, `status` = ?, `msg` = ?, `unlocktime` = ? WHERE `id` = ?";
        List<AchievementData> newAchievements = new ArrayList<>();
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement insertPS = con.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement updatePS = con.prepareStatement(updateSql)) {
            for (AchievementData data : achievementDatas) {
                Timestamp unlockTimestamp = data.getUnlockTime() != null ? new Timestamp(data.getUnlockTime().toMillis()) : null;
                if (data.getId() == 0) {
                    newAchievements.add(data);
                    insertPS.setInt(1, data.getAccID());
                    insertPS.setInt(2, data.getInfoID());
                    insertPS.setInt(3, data.getMissionID());
                    insertPS.setInt(4, data.getStatus());
                    insertPS.setString(5, data.getMsg());
                    insertPS.setTimestamp(6, unlockTimestamp);
                    insertPS.addBatch();
                } else {
                    updatePS.setInt(1, data.getAccID());
                    updatePS.setInt(2, data.getInfoID());
                    updatePS.setInt(3, data.getMissionID());
                    updatePS.setInt(4, data.getStatus());
                    updatePS.setString(5, data.getMsg());
                    updatePS.setTimestamp(6, unlockTimestamp);
                    updatePS.setInt(7, data.getId());
                    updatePS.addBatch();
                }
            }
            insertPS.executeBatch();
            updatePS.executeBatch();
            try (ResultSet rs = insertPS.getGeneratedKeys()) {
                for (AchievementData data : newAchievements) {
                    if (rs.next()) {
                        data.setId(rs.getInt(1));
                    }
                }
            }
        } catch (Exception e) {
            DataPrinter.send(DataPrinter.HIKARICP_ERROR, e);
        }
    }

    public Account(User user, int worldId) {
        setUser(user);
        this.worldId = worldId;
        this.trunk = new Trunk(GameConstants.DEFAULT_TRUNK_SIZE); // Free first 4 storage slot
        this.monsterCollection = new MonsterCollection();
        this.friends = new HashSet<>(40);
        this.characters = new HashSet<>();
        this.achievementMasterRank = 0;
        this.achievementPoint = 0;
        this.achievementDatas = new HashSet<>();
        AchievementData ad = new AchievementData(getId(), 1, (byte) -1, (byte) 2, FileTime.currentTime(), "script=1");
        this.achievementDatas.add(ad);
        this.achievementRanks = new HashSet<>();
        AchievementRank ar = new AchievementRank(getId(), 1, (byte) 1, FileTime.currentTime());
        this.achievementRanks.add(ar);
        this.union = new Union(getId(), 2, 101);
    }

    public Account() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Set<Char> getCharacters() {
        return characters;
    }

    public List<Char> getDeletionCharacters(List<Char> chars) {
        List<Char> deletions = new LinkedList<>();
        for (Char chr : chars) {
            if (chr != null && chr.getDeletionStartTime() != 0) {
                deletions.add(chr);
            }
        }
        return deletions;
    }

    public void setCharacters(Set<Char> charSet) {
        this.characters = charSet;
    }

    public void addCharacter(Char character) {
        getCharacters().add(character);
    }

    public void removeCharacter(Char remove) {
        Iterator<Char> iterator = getCharacters().iterator();
        while (iterator.hasNext()) {
            Char chr = iterator.next();
            if (chr.getId() == remove.getId()) {
                iterator.remove();
                break;
            }
        }
    }

    public Set<Friend> getFriends() {
        return friends;
    }

    public void setFriends(Set<Friend> friends) {
        this.friends = friends;
    }

    public void addFriend(Friend friend) {
        if (getFriendByAccID(friend.getFriendAccountID()) == null) {
            getFriends().add(friend);
        }
    }

    public Friend getFriendByAccID(int accID) {
        return getFriends().stream().filter(f -> f.getFriendAccountID() == accID).findAny().orElse(null);
    }

    public void removeFriend(int accID) {
        removeFriend(getFriendByAccID(accID));
    }

    public void removeFriend(Friend f) {
        if (f != null) {
            getFriends().removeIf(x -> x.getId() == f.getId()
                    && x.getFriendAccountID() == f.getFriendAccountID()
                    && x.getOwnerID() == f.getOwnerID()
                    && x.getFriendID() == f.getFriendID()
            );
        }
    }

    public Set<Friend> getAllFriends() {
        Set<Friend> res = new HashSet<>(getFriends());
        for (Char chr : getCharacters()) {
            res.addAll(chr.getFriends());
        }
        return res;
    }

    public Trunk getTrunk() {
        if (trunk == null) {
            trunk = new Trunk(GameConstants.DEFAULT_TRUNK_SIZE);
        }
        return trunk;
    }

    public void setTrunk(Trunk trunk) {
        this.trunk = trunk;
    }

    public int getRewardPoint() {
        return rewardPoint;
    }

    public void setRewardPoint(int rewardPoint) {
        this.rewardPoint = rewardPoint;
    }

    public void addLinkSkill(LinkSkill linkSkill) {
        try {
            getLinkSkills().removeIf(ls -> ls.getOwnerID() == linkSkill.getOwnerID());
        } catch (Exception e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
        } finally {
            getLinkSkills().add(linkSkill);
        }
    }

    public void addLinkSkill(Char originChar, int linkedSkill, FileTime ft) {
        int originCharID = originChar.getId();
        switch (linkedSkill) {
            case 80000110:
            {
                LinkSkill ls = new LinkSkill(getId(), originCharID, linkedSkill, 0, 1, ft);
                addLinkSkill(ls);
                break;
            }
            default:
            {
                int level = SkillConstants.getLinkSkillLevelByCharLevel(originChar.getLevel(), originChar.getJob());
                LinkSkill ls = new LinkSkill(getId(), originCharID, linkedSkill, 0, level, ft);
                addLinkSkill(ls);
                break;
            }
        }
    }

    /**
     * A link skill row is only made by the level 70 (Zero 110) level-up: a character whose level was set directly (GM
     * level, 輪迴, level rewards) never got one, so the link skill window had nothing to hand out for it. Creates the
     * missing rows (the TMS 282 login never runs {@link #init()}, so the field entry calls this too).
     */
    public void ensureLinkSkillRows() {
        if (getLinkSkills() == null || getCharacters() == null) {
            return;
        }
        for (Char owner : getCharacters()) {
            int linkSkillID = SkillConstants.getLinkSkillByJob(owner.getJob());
            int needLevel = net.swordie.ms.constants.JobConstants.isZero(owner.getJob()) ? 110 : SkillConstants.LINK_SKILL_1_LEVEL;
            if (linkSkillID == 0 || owner.getLevel() < needLevel
                    || getLinkSkills().stream().anyMatch(ls -> ls.getOwnerID() == owner.getId())) {
                continue;
            }
            try {
                addLinkSkill(owner, linkSkillID, FileTime.MIN_TIME());
                getLinkSkills().stream().filter(ls -> ls.getOwnerID() == owner.getId())
                        .forEach(LinkSkill::updateLinkSkillToSQL);
            } catch (RuntimeException e) {
                DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
            }
        }
    }

    public void removeLinkSkillByChar(int charID) {
        for (LinkSkill ls : getLinkSkills()) {
            if (ls.getLinkCount() == charID) {
                ls.setLinkCount(0);
                ls.updateLinkSkillToSQL();
            }
        }
        LinkSkill linkSkill = getLinkSkills().stream().filter(l -> l.getOwnerID() == charID).findFirst().orElse(null);
        if (linkSkill != null) {
            int linkSkillID = linkSkill.getLinkSkillID();
            if (linkSkill.getLinkCount() != 0) {
                Char chr = Char.getCharSkillsByID(linkSkill.getLinkCount());
                int skillLevel = linkSkill.getLevel();
                int ordinarySkill = 0;
                if (linkSkillID >= 80000066 && linkSkillID <= 80000070) {
                    ordinarySkill = 80000055;
                } else if ((linkSkillID >= 80000333 && linkSkillID <= 80000335) || linkSkillID == 80000378) {
                    ordinarySkill = 80000329;
                } else if (linkSkillID >= 80002759 && linkSkillID <= 80002761) {
                    ordinarySkill = 80002758;
                } else if (linkSkillID >= 80002763 && linkSkillID <= 80002765) {
                    ordinarySkill = 80002762;
                } else if (linkSkillID >= 80002767 && linkSkillID <= 80002769) {
                    ordinarySkill = 80002766;
                } else if (linkSkillID >= 80002771 && linkSkillID <= 80002773) {
                    ordinarySkill = 80002770;
                } else if ((linkSkillID >= 80002775 && linkSkillID <= 80002776) || linkSkillID == 80000000) {
                    ordinarySkill = 80002774;
                }
                if (ordinarySkill > 0) {
                    Skill skill = chr.getSkill(ordinarySkill);
                    if (skill != null) {
                        skill.setCurrentLevel(skill.getCurrentLevel() - skillLevel);
                        skill.saveToSQL();
                    }
                }
                chr.removeSkill(linkSkillID);
            }
            linkSkill.deleteLinkSkillFromSQL();
        }
        getLinkSkills().clear();
        setLinkSkills(LinkSkill.getLinkSkillsFromSQLByAccountID(id));
    }

    public Set<LinkSkill> getLinkSkills() {
        return linkSkills;
    }

    public void setLinkSkills(Set<LinkSkill> linkSkills) {
        this.linkSkills = linkSkills;
    }

    public LinkSkill getLinkSkillBySkillID(int skillID) {
        for (LinkSkill linkSkill : getLinkSkills()) {
            if (linkSkill.getLinkSkillID() == skillID) {
                return linkSkill;
            }
        }
        return null;
    }

    public LinkSkill getLinkSkill(int charID, int skillID) {
        for (LinkSkill linkSkill : getLinkSkills()) {
            if (linkSkill.getLinkSkillID() == skillID && linkSkill.getOwnerID() == charID) {
                return linkSkill;
            }
        }
        return null;
    }

    public synchronized void addRewardPoint(int credit) {
        long newRP = (long)getRewardPoint() + credit;
        if(newRP<0 || newRP>Integer.MAX_VALUE)throw new IllegalArgumentException("Cash shop mileage balance out of range");
        if(credit==0)return;
        try(Connection c=DatabaseManager.getConnection();PreparedStatement p=c.prepareStatement(
                "UPDATE accounts SET nxCredit=? WHERE id=? AND nxCredit=?")) {
            p.setInt(1,(int)newRP);p.setInt(2,getId());p.setInt(3,getRewardPoint());
            if(p.executeUpdate()!=1)throw new IllegalStateException("Cash shop mileage balance changed; refresh first");
            setRewardPoint((int)newRP);
        } catch(java.sql.SQLException e) {
            throw new IllegalStateException("Cash shop mileage balance could not be saved",e);
        }
    }

    public void deductRewardPoint(int rp) {
        addRewardPoint(-rp);
    }

    public MonsterCollection getMonsterCollection() {
        if (monsterCollection == null) {
            monsterCollection = new MonsterCollection();
        }
        return monsterCollection;
    }

    public void setMonsterCollection(MonsterCollection monsterCollection) {
        this.monsterCollection = monsterCollection;
    }

    public boolean hasCharacter(int charID) {
        // doing a .contains on getCharacters() does not work, even if the hashcode is just a hash of the id
        return getCharById(charID) != null;
    }

    public Char getCharById(int id) {
        return Util.findWithPred(getCharacters(), chr -> chr.getId() == id);
    }

    public User getUser() {
        return user;
    }

    public int getOwnerUserId() {
        return ownerUserId;
    }

    public void setUser(User user) {
        if (user != null && ownerUserId > 0 && user.getId() != ownerUserId)
            throw new IllegalArgumentException("World account owner mismatch");
        this.user = user;
        if (user != null) ownerUserId = user.getId();
    }

    public int getWorldId() {
        return worldId;
    }

    public void setWorldId(int worldId) {
        this.worldId = worldId;
    }

    public Char getCurrentChr() {
        return currentChr;
    }

    public void setCurrentChr(Char currentChr) {
        this.currentChr = currentChr;
    }

    public EmployeeTrunk getEmployeeTrunk() {
        if (employeeTrunk == null) {
            employeeTrunk = new EmployeeTrunk();
        }

        return employeeTrunk;
    }

    public int generateNewOrderId() {
        int max = 0;
        List<Integer> chars = new ArrayList<>();
        for (Char ch : getCharacters()) {
            chars.add(ch.getOrderId());
        }
        if (!chars.isEmpty()) {
            Integer currentMaxOrderId = Collections.max(chars);
            max = currentMaxOrderId + 1;
        }
        return max;
    }

    public Set<AchievementRank> getAchievementRanks() {
        return achievementRanks;
    }

    public void setAchievementRanks(Set<AchievementRank> achievementRanks) {
        this.achievementRanks = achievementRanks;
    }

    public Set<AchievementData> getAchievementDatas() {
        return achievementDatas;
    }

    public void setAchievementDatas(Set<AchievementData> achievementDatas) {
        this.achievementDatas = achievementDatas;
    }

    public int getAchievementMasterRank() {
        return achievementMasterRank;
    }

    public void setAchievementMasterRank(int achievementMasterRank) {
        this.achievementMasterRank = achievementMasterRank;
    }

    public int getAchievementPoint() {
        return achievementPoint;
    }

    public void setAchievementPoint(int achievementPoint) {
        this.achievementPoint = achievementPoint;
    }

    public void incAPoint(int inc) {
        this.achievementPoint += inc;
    }

    public Union getUnion() {
        return union;
    }

    public void setUnion(Union union) {
        this.union = union;
    }

    public Set<Char> getEligibleUnionChars() {
        // Only take 3rd job+ characters that are at least level 60.
        return getCharacters().stream()
                .filter(chr -> chr.getLevel() >= 60 && chr.getJob() / 10 >= 1)
                .collect(Collectors.toSet());
    }

    public void init() {
        boolean initialised = false;
        for (AchievementData data : getAchievementDatas()) {
            if (data.getInfoID() == 1) {
                initialised = true;
                break;
            }
        }
        if (!initialised) {
            getAchievementDatas().add(new AchievementData(getId(), 1, (byte) -1, (byte) 2, FileTime.currentTime(), "script=1"));
        }
        if (getAchievementRanks().isEmpty()) {
            getAchievementRanks().add(new AchievementRank(getId(), 1, 1, FileTime.currentTime()));
            if (getAchievementPoint() >= 5000 && getAchievementPoint() <= 19999) {
                // Silver
                getAchievementRanks().add(new AchievementRank(getId(), 2, 1, FileTime.currentTime()));
            }
            if (getAchievementPoint() >= 20000 && getAchievementPoint() <= 29999) {
                // gold
                getAchievementRanks().add(new AchievementRank(getId(), 3, 1, FileTime.currentTime()));
            }
            if (getAchievementPoint() >= 30000 && getAchievementPoint() <= 35999) {
                // Platinum
                getAchievementRanks().add(new AchievementRank(getId(), 4, 1, FileTime.currentTime()));
            }
            if (getAchievementPoint() >= 36000 && getAchievementPoint() <= 38730) {
                // Diamond
                getAchievementRanks().add(new AchievementRank(getId(), 5, 1, FileTime.currentTime()));
            }
        }
        if (getUnion() == null) {
            setUnion(new Union(getId(), 2, 101));
        }
        ensureLinkSkillRows();
        for (LinkSkill linkSkill : getLinkSkills()) {
            Char owner = getCharById(linkSkill.getOwnerID());
            if (owner != null) {
                int level = owner.getLevel();
                int reqLevel = SkillConstants.getLinkSkillLevelByCharLevel((short) level, owner.getJob());
                if (linkSkill.getLevel() != reqLevel) {
                    linkSkill.setLevel(reqLevel);
                    linkSkill.updateLinkSkillToSQL();
                }
            }
        }
    }

    public DailyGift getDailyGift() {
        return dailyGift;
    }

    public void setDailyGift(DailyGift dailyGift) {
        this.dailyGift = dailyGift;
    }

    public Map<Integer, AccountQuest> getQuests() {
        return quests;
    }

    public void setQuests(Map<Integer, AccountQuest> quests) {
        this.quests = quests;
    }

    public static Map<Integer, AccountQuest> getAllQuestsFromSQLByAccID(int accId) {
        Map<Integer, AccountQuest> allQuests = new HashMap<>();
        String query = "SELECT " +
                "    q.id AS q_id, " +
                "    q.accid, " +
                "    q.qrkey, " +
                "    q.qrvalue, " +
                "    q.status, " +
                "    q.completedtime, " +
                "    q.expireterm, " +
                "    qpr.id AS qpr_id, " +
                "    qpr.orderNum, " +
                "    qpr.progresstype, " +
                "    qpr.unitid, " +
                "    qpr.requiredcount, " +
                "    qpr.currentcount " +
                "FROM quests_acc q " +
                "LEFT JOIN questprogressrequirements_acc qpr ON q.id = qpr.questid " +
                "WHERE q.accid = ?"; // Thêm điều kiện WHERE để lọc theo accid
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, accId); // Gán giá trị accid vào dấu hỏi
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int questID = rs.getInt("qrkey");
                    if (!allQuests.containsKey(questID)) {
                        AccountQuest quest = new AccountQuest();
                        quest.setId(rs.getLong("q_id"));
                        quest.setAccId(rs.getInt("accid"));
                        quest.setQRKey(questID);
                        quest.setQrValue(rs.getString("qrvalue"));
                        quest.setStatus(QuestStatus.getValByNum(rs.getInt("status")));
                        FileTime completedTime = DatabaseManager.getFileTimeFromString(rs.getString("completedtime"));
                        if (completedTime != null) {
                            quest.setCompletedTime(completedTime);
                        }
                        FileTime expireTerm = DatabaseManager.getFileTimeFromString(rs.getString("expireterm"));
                        if (expireTerm != null) {
                            quest.setExpireTerm(expireTerm);
                        }
                        allQuests.put(questID, quest);
                    }

                    long qprID = rs.getLong("qpr_id");
                    if (qprID != 0) {
                        AccountQuest currentQuest = allQuests.get(questID);
                        if (currentQuest != null) {
                            String progressType = rs.getString("progresstype");
                            int unitID = rs.getInt("unitid");
                            int requiredCount = rs.getInt("requiredcount");
                            int currentCount = rs.getInt("currentcount");
                            int orderNum = rs.getInt("orderNum");
                            QuestProgressRequirement qpr = null;
                            switch (progressType) {
                                case "mob": {
                                    QuestProgressMobRequirement questProgressMobRequirement = new QuestProgressMobRequirement();
                                    questProgressMobRequirement.setId(qprID);
                                    questProgressMobRequirement.setOrder(orderNum);
                                    questProgressMobRequirement.setMobID(unitID);
                                    questProgressMobRequirement.setRequiredCount(requiredCount);
                                    questProgressMobRequirement.setCurrentCount(currentCount);
                                    qpr = questProgressMobRequirement;
                                    break;
                                }
                                case "item": {
                                    QuestProgressItemRequirement questProgressItemRequirement = new QuestProgressItemRequirement();
                                    questProgressItemRequirement.setId(qprID);
                                    questProgressItemRequirement.setOrder(orderNum);
                                    questProgressItemRequirement.setItemID(unitID);
                                    questProgressItemRequirement.setRequiredCount(requiredCount);
                                    questProgressItemRequirement.setCurrentCount(currentCount);
                                    qpr = questProgressItemRequirement;
                                    break;
                                }
                                case "money": {
                                    QuestProgressMoneyRequirement questProgressMoneyRequirement = new QuestProgressMoneyRequirement();
                                    questProgressMoneyRequirement.setId(qprID);
                                    questProgressMoneyRequirement.setOrder(orderNum);
                                    questProgressMoneyRequirement.setMoney(net.swordie.ms.client.character.QuestShortcut.enabled() ? requiredCount : unitID);
                                    qpr = questProgressMoneyRequirement;
                                    break;
                                }
                                case "level": {
                                    QuestProgressLevelRequirement questProgressLevelRequirement = new QuestProgressLevelRequirement();
                                    questProgressLevelRequirement.setId(qprID);
                                    questProgressLevelRequirement.setOrder(orderNum);
                                    questProgressLevelRequirement.setLevel(net.swordie.ms.client.character.QuestShortcut.enabled() ? requiredCount : unitID);
                                    qpr = questProgressLevelRequirement;
                                    break;
                                }
                            }
                            if (qpr != null) {
                                currentQuest.getProgressRequirements().add(qpr);
                            }
                        }
                    }
                }
            }
        } catch (Exception exception) {
            System.err.println("[HikariCP Database] Exception: " + exception);
            if (net.swordie.ms.client.character.QuestShortcut.enabled()) throw new IllegalStateException("Account quest load failed", exception);
        }
        return allQuests;
    }

    public Set<AccountQuest> getCompletedQuests() {
        return getQuests().values().stream()
                .filter(quest -> quest.getStatus() == Completed)
                .filter(quest -> quest.getQRKey() >= 1000)
                .collect(Collectors.toSet());
    }

    public Set<AccountQuest> getQuestsInProgress() {
        return getQuests().values().stream()
                .filter(quest -> quest.getStatus() == Started)
                .filter(quest -> quest.getQRKey() >= 1000)
                .collect(Collectors.toSet());
    }

    public void addQuest(AccountQuest quest) {
        addQuest(quest, true);
    }

    public void addCustomQuest(AccountQuest quest) {
        addQuest(quest, false);
    }

    private void addQuest(AccountQuest quest, boolean addRewardsFromWz) {
        if (!getQuests().containsKey(quest.getQRKey())) {
            getQuests().put(quest.getQRKey(), quest);
            if (currentChr != null) {
                currentChr.write(WvsContext.questRecordMessage(quest));
                currentChr.write(WvsContext.questWorldShareMessage(quest));
                if (quest.getStatus() == QuestStatus.Completed) {
                    if (getUser().getAccountType().getVal() != AccountType.Player.getVal()) {
                        currentChr.chatMessage(SystemNotice, "[資訊] 已完成帳號任務 " + quest.getQRKey());
                    }
                    if (currentChr.getGuild() != null) {
                        currentChr.getGuild().addContributionToChar(currentChr, GuildConstants.CONTRIBUTION_PER_QUEST);
                    }
                } else {
                    if (getUser().getAccountType().getVal() != AccountType.Player.getVal()) {
                        currentChr.chatMessage(SystemNotice, "[資訊] 已接受帳號任務 " + quest.getQRKey());
                    }
                    if (addRewardsFromWz) {
                        QuestInfo qi = QuestData.getQuestInfoById(quest.getQRKey());
                        if (qi != null) {
                            for (QuestReward qr : qi.getQuestRewards()) {
                                if (qr instanceof QuestItemReward qir && qir.getStatus() == 0) {
                                    qir.giveReward(this, qi.getQuestName());
                                } else if (qr instanceof QuestBuffItemReward && ((QuestBuffItemReward) qr).getStatus() == 0) {
                                    qr.giveReward(this, qi.getQuestName());
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void handleCompleteQuest(QuestInfo qi, int questID) {
        handleCompleteQuest(qi, questID, false);
    }

    public void handleCompleteQuest(QuestInfo qi, int questID, boolean skipObjectives) {
        if (qi != null) {
            for (QuestProgressRequirement qsr : qi.getQuestProgressRequirements()) {
                if (!skipObjectives && qsr instanceof QuestProgressItemRequirement qpir) {
                    currentChr.consumeItem(qpir.getItemID(), qpir.getRequiredCount());
                }
            }
            for (QuestReward qr : qi.getQuestRewards()) {
                if (!net.swordie.ms.client.character.QuestShortcut.enabled()
                        && (!(qr instanceof QuestItemReward) || ((QuestItemReward) qr).getStatus() != 0)) {
                    qr.giveReward(this, qi.getQuestName());
                }
            }
            List<QuestItemReward> tempRewardItems = new ArrayList<>();
            boolean hasEXPQuest = false;
            for (QuestReward qr : qi.getQuestRewards()) {
                if (net.swordie.ms.client.character.QuestShortcut.enabled() && (qr instanceof QuestBuffItemReward buff && buff.getStatus() == 0
                        || qr instanceof QuestItemReward item && item.getStatus() == 0)) continue;
                if (skipObjectives && net.swordie.ms.client.character.QuestShortcut.isSkippedCost(qr)) continue;
                if (qr instanceof QuestItemReward qir && qir.getStatus() != 0) {
                    if (qir.getProp() > 0) {
                        tempRewardItems.add(qir);
                    } else {
                        qir.giveReward(this, qi.getQuestName());
                    }
                } else if (qr instanceof QuestExpReward) {
                    qr.giveReward(this, qi.getQuestName());
                    hasEXPQuest = true;
                } else {
                    qr.giveReward(this, qi.getQuestName());
                }
            }
            if (!tempRewardItems.isEmpty()) {
                Util.getRandomFromCollection(tempRewardItems).giveReward(this, qi.getQuestName());
            }
            if (!hasEXPQuest) {
                int lvlMin = 0;
                for (QuestStartRequirement questReq : qi.getQuestStartRequirements()) {
                    if (questReq instanceof QuestStartMinStatRequirement min && min.getStat() == Stat.level) {
                        lvlMin = min.getReqAmount();
                        break;
                    }
                }
                if (lvlMin != 0) {
                    long baseEXP = GameConstants.charExp[lvlMin] / 100;
                    QuestExpReward qer = new QuestExpReward(baseEXP, "");
                    qer.giveReward(this, qi.getQuestName());
                    qi.addReward(qer);
                }
            }
            if (qi.getMedalItemId() != 0 && currentChr.findMedalByID(qi.getMedalItemId(), questID) == null) {
                MedalAchievementInfo medal = new MedalAchievementInfo(getId(), questID, qi.getMedalItemId(), FileTime.currentTime());
                currentChr.addMedalAchievementInfo(medal);
                medal.saveToSQL();
                if (currentChr.canHold(qi.getMedalItemId(), 1)) {
                    currentChr.addItemToInventory(qi.getMedalItemId(), 1);
                } else {
                    currentChr.sendRewardToChar(qi.getMedalItemId(), 1, 0, "背包已滿，因此完成任務 " + qi.getQuestName() + " 的獎勵將通過郵件發送。", 30);
                }
                currentChr.chatScriptMessage("你獲得了 " + StringData.getItemStringById(qi.getMedalItemId()));
            }
            if (currentChr.getGuild() != null) {
                currentChr.getGuild().addContributionToChar(currentChr, GuildConstants.CONTRIBUTION_PER_QUEST);
            }
            AchievementHandler.handleQuestCompleted(currentChr, questID);
        }
    }

    public AccountQuest getQuestById(int questID) {
        return getQuests().getOrDefault(questID, null);
    }

    public AccountQuest getOrCreateQuestById(int questId) {
        AccountQuest quest = getQuestById(questId);
        if (quest == null) {
            AccountQuest q = QuestData.createAccQuestFromId(questId, getId());
            addQuest(q);
            return q;
        }
        return quest;
    }

    public void setMVPMileage(int mvp) {
        String now = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String nowMonth = new SimpleDateFormat("yyyyMM").format(new Date());
        AccountQuest q1 = getQuestById(5);
        String value = "todayAmount_" + now + "=" + mvp + ";amount=" + mvp;
        if (q1 == null) {
            q1 = QuestData.createAccQuestFromId(5, getId());
            q1.setQrValue(value);
            addQuest(q1);
        } else {
            if (!q1.getQRValue().equalsIgnoreCase(value)) {
                q1.setQrValue(value);
            }
        }
        currentChr.write(WvsContext.questWorldShareMessage(q1));
        AccountQuest q2 = getQuestById(6);
        value = "sp_1" + "=" + nowMonth;
        if (q2 == null) {
            q2 = QuestData.createAccQuestFromId(6, getId());
            q2.setProperty("sp_1", nowMonth);
            addQuest(q2);
        } else {
            if (!q2.getQRValue().equalsIgnoreCase(value)) {
                q2.setQrValue(value);
            }
        }
        currentChr.write(WvsContext.questWorldShareMessage(q2));
        AccountQuest q3 = getQuestById(90);
        if (q3 == null) {
            q3 = QuestData.createAccQuestFromId(90, getId());
            q3.setQrValue("1_1");
            addQuest(q3);
        } else {
            q3.setQrValue("1_1");
        }
        currentChr.write(WvsContext.questWorldShareMessage(q3));
    }

    public int getMVPMileage() {
        AccountQuest quest = getQuestById(5);
        if (quest == null) {
            String now = new SimpleDateFormat("yyyyMMdd").format(new Date());
            String value = "todayAmount_" + now + "=0;amount=0";
            quest = QuestData.createAccQuestFromId(5, getId());
            quest.setQrValue(value);
            addQuest(quest);
        }
        return Integer.parseInt(quest.getProperty("amount"));
    }

    public int getLoginTheme() {
        return loginTheme;
    }

    public void setLoginTheme(int loginTheme) {
        this.loginTheme = loginTheme;
    }

    public void saveLoginTheme() {
        String sql = "UPDATE `accounts` SET `theme` = ? WHERE `id` = ?";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.NO_GENERATED_KEYS)) {
            ps.setInt(1, loginTheme);
            ps.setInt(2, getId());
            ps.executeUpdate();
        } catch (Exception e) {
            DataPrinter.send(DataPrinter.HIKARICP_ERROR, e);
        }
    }

    public Map<Integer, UnionArtifact> getUnionArtifacts() {
        return unionArtifacts;
    }

    public void setUnionArtifacts(Map<Integer, UnionArtifact> unionArtifacts) {
        this.unionArtifacts = unionArtifacts;
    }
}
