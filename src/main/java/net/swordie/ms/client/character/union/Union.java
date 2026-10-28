package net.swordie.ms.client.character.union;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.util.DataPrinter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

public class Union {

    private int id;
    private int accID;
    private List<UnionBoard> unionBoards = new ArrayList<>();
    private int unionCoin;
    private int unionRank;
    private int presets;

    public static final int MAX_PRESETS = 5;
    public static final int MAX_STATS = 8;
    private static final int DEFAULT_PRESETS = 2;

    public Union() {
    }

    public Union(int accID, int presets, int unionRank) {
        this.accID = accID;
        this.presets = presets;
        this.unionRank = unionRank;
        for (int i = 0; i < DEFAULT_PRESETS; i++) {
            addPreset(i);
        }
    }

    public static Union getUnionFromSQLByAccountID(int accountID) {
        Union union = null;
        Map<Integer, UnionBoard> unionBoardMap = new HashMap<>();
        String query = "SELECT " +
                "u.id AS u_id, u.accid, u.unioncoin, u.unionrank, u.presets, " +
                "ub.id AS ub_id, ub.unionpower, ub.uniondamage, ub.synergygrids, " +
                "um.id AS um_id, um.unionboardid, um.type, um.gridPos, um.gridRotation, um.charID, " +
                "cs.name, cs.level, cs.job, cs.subjob, cs.chuc, cs.combatpower " +
                "FROM `union` u " +
                "LEFT JOIN unionboard ub ON u.id = ub.unionid " +
                "LEFT JOIN unionmember um ON ub.id = um.unionboardid " +
                "LEFT JOIN characters c ON um.charID = c.id " +
                "LEFT JOIN characterstats cs ON c.id = cs.characterid " +
                "WHERE u.accid = ?";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, accountID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (union == null) {
                        union = new Union();
                        union.setId(rs.getInt("u_id"));
                        union.setAccID(rs.getInt("accid"));
                        union.setUnionCoin(rs.getInt("unioncoin"));
                        union.setUnionRank(rs.getInt("unionrank"));
                        union.setPresets(rs.getInt("presets"));
                        union.setUnionBoards(new ArrayList<>());
                    }
                    int boardId = rs.getInt("ub_id");
                    if (boardId > 0 && !unionBoardMap.containsKey(boardId)) {
                        UnionBoard unionBoard = new UnionBoard(union.getId());
                        unionBoard.setId(boardId);
                        unionBoard.setUnionPower(rs.getInt("unionpower"));
                        unionBoard.setUnionDamage(rs.getLong("uniondamage"));
                        String synergygridsString = rs.getString("synergygrids");
                        if (synergygridsString != null && !synergygridsString.isEmpty()) {
                            List<Integer> synergyGrids = new ArrayList<>();
                            for (String s : synergygridsString.split(",")) {
                                synergyGrids.add(Integer.parseInt(s.trim()));
                            }
                            unionBoard.setSynergyGrid(synergyGrids);
                        }
                        unionBoard.setActiveMembers(new HashSet<>());
                        unionBoardMap.put(boardId, unionBoard);
                        union.getUnionBoards().add(unionBoard);
                    }
                    int memberId = rs.getInt("um_id");
                    if (memberId > 0) {
                        if (rs.getObject("name") != null) {
                            UnionMember unionMember = new UnionMember();
                            unionMember.setId(memberId);
                            unionMember.setUnionBoardID(boardId);
                            unionMember.setType(rs.getInt("type"));
                            unionMember.setGridPos(rs.getInt("gridPos"));
                            unionMember.setGridRotation(rs.getInt("gridRotation"));
                            unionMember.setCharID(rs.getInt("charID"));
                            unionMember.setCharName(rs.getString("name"));
                            unionMember.setCharLevel(rs.getInt("level"));
                            unionMember.setCharJob(rs.getInt("job"));
                            unionMember.setCharSubJob(rs.getInt("subjob"));
                            unionMember.setCharTotalChuc(rs.getInt("chuc"));
                            unionMember.setCharCombatPower(rs.getInt("combatpower"));
                            unionBoardMap.get(boardId).getActiveMembers().add(unionMember);
                        }
                    }
                }
            }
        } catch (Exception exception) {
            DataPrinter.send(DataPrinter.HIKARICP_ERROR, exception);
        }
        return union;
    }

    public void saveToSQL() {
        if (getId() == 0) {
            for (UnionBoard unionBoard : getUnionBoards()) {
                unionBoard.updateUnionBoardToSQL();
            }
            String query = "INSERT INTO `union` (" +
                    "`accid`, " +
                    "`unioncoin`, " +
                    "`unionrank`, " +
                    "`presets` " +
                    ") VALUES (" +
                    String.format("%d, ", getAccID()) +
                    String.format("%d, ", getUnionCoin()) +
                    String.format("%d, ", getUnionRank()) +
                    String.format("%d ", getPresets()) +
                    ");";
            int id = (int) DatabaseManager.executeStatementReturnID(query);
            setId(id);
        } else {
            for (UnionBoard unionBoard : getUnionBoards()) {
                unionBoard.updateUnionBoardToSQL();
            }
            String query = "UPDATE `union` SET " +
                    String.format("accid = %d, ", getAccID()) +
                    String.format("unioncoin = %d, ", getUnionCoin()) +
                    String.format("unionrank = %d, ", getUnionRank()) +
                    String.format("presets = %d ", getPresets()) +
                    String.format("WHERE id = %d;", getId());
            DatabaseManager.executeStatement(query);
        }
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getAccID() {
        return accID;
    }

    public void setAccID(int accID) {
        this.accID = accID;
    }

    public void addPreset(int preset) {
        setPresets(preset + 1);
        UnionBoard unionBoard = new UnionBoard(getId());
        unionBoard.init();
        this.unionBoards.add(unionBoard);
    }

    public UnionBoard getBoardByPreset(int preset) {
        if (hasPresetUnlocked(preset)) {
            if (getUnionBoards().isEmpty()) {
                for (int i = 0; i < DEFAULT_PRESETS; i++) {
                    addPreset(i);
                }
            }
            return getUnionBoards().get(preset);
        }
        return null;
    }

    public List<UnionBoard> getUnionBoards() {
        return unionBoards;
    }

    public void setUnionBoards(List<UnionBoard> unionBoards) {
        this.unionBoards = unionBoards;
    }

    public int getUnionCoin() {
        return unionCoin;
    }

    public void setUnionCoin(int unionCoin) {
        this.unionCoin = unionCoin;
    }

    public void addUnionCoin(int amount) {
        setUnionCoin(Math.max(0, getUnionCoin() + amount));
    }

    public void decUnionCoin(int amount) {
        setUnionCoin(getUnionCoin() - amount);
    }

    public int getUnionRank() {
        return unionRank;
    }

    public void setUnionRank(int unionRank) {
        this.unionRank = unionRank;
    }

    public int getPresets() {
        return presets;
    }

    public void setPresets(int presets) {
        this.presets = presets;
    }

    public boolean hasPresetUnlocked(int preset) {
        return preset >= 0 && preset < getPresets();
    }

    public Set<UnionMember> getActiveUnionChars(int preset) {
        // Get eligible characters that have a grid position of >= 0.
        if (hasPresetUnlocked(preset)) {
            UnionBoard unionBoard = getBoardByPreset(preset);
            if (unionBoard != null) {
                return unionBoard.getActiveMembers();
            }
        }
        return new HashSet<>();
    }

    public void setCharPosForPreset(int preset, Char chr, int rotation, int grid) {
        UnionBoard unionBoard = getBoardByPreset(preset);
        if (unionBoard != null) {
            unionBoard.setCharGridPos(chr, rotation, grid);
        }
    }
}
