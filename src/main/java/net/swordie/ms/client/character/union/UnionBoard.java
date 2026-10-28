package net.swordie.ms.client.character.union;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.constants.UnionConstants;
import net.swordie.ms.util.Util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UnionBoard {
    private int id;
    private int unionId;
    private int unionPower;
    private long unionDamage;
    private Set<UnionMember> activeMembers = new HashSet<>();
    private List<Integer> synergyGrid = new ArrayList<>();

    public UnionBoard(int unionId) {
        this.unionId = unionId;
    }

    public void init() {
        for (int allocation = 0; allocation < Union.MAX_STATS; allocation++) {
            synergyGrid.add(allocation);
        }
    }

    public String getSynergyGridsSQL() {
        return String.format("%d,%d,%d,%d,%d,%d,%d,%d",
                getSynergyGrid().get(0),
                getSynergyGrid().get(1),
                getSynergyGrid().get(2),
                getSynergyGrid().get(3),
                getSynergyGrid().get(4),
                getSynergyGrid().get(5),
                getSynergyGrid().get(6),
                getSynergyGrid().get(7));
    }

    public void updateUnionBoardToSQL() {
        if (getId() == 0) {
            for (UnionMember unionMember : getActiveMembers()) {
                unionMember.updateUnionMemberToSQL();
            }
            String query = "INSERT INTO `unionboard` (" +
                    "`unionid`, " +
                    "`unionpower`, " +
                    "`uniondamage`, " +
                    "`synergygrids` " +
                    ") VALUES (" +
                    String.format("%d, ", getUnionId()) +
                    String.format("%d, ", getUnionPower()) +
                    String.format("%d, ", getUnionDamage()) +
                    String.format("'%s' ", getSynergyGridsSQL()) +
                    ");";
            int id = (int) DatabaseManager.executeStatementReturnID(query);
            setId(id);
        } else {
            for (UnionMember unionMember : getActiveMembers()) {
                unionMember.updateUnionMemberToSQL();
            }
            String query = "UPDATE unionboard SET " +
                    String.format("unionid = %d, ", getUnionId()) +
                    String.format("unionpower = %d, ", getUnionPower()) +
                    String.format("uniondamage = %d, ", getUnionDamage()) +
                    String.format("synergygrids = '%s' ", getSynergyGridsSQL()) +
                    String.format("WHERE id = %d;", getId());
            DatabaseManager.executeStatement(query);
        }
    }

    public void deleteUnionBoardsFromSQL() {
        String query = "DELETE FROM `unionboard` WHERE " +
                String.format("`unionid` = %d", getUnionId());
        DatabaseManager.executeStatement(query);
        setId(0);
    }

    public Set<UnionMember> getActiveMembers() {
        return activeMembers;
    }

    public void setActiveMembers(Set<UnionMember> activeMembers) {
        this.activeMembers = activeMembers;
    }

    public int getUnionPower() {
        return unionPower;
    }

    public void setUnionPower(int unionPower) {
        this.unionPower = unionPower;
    }

    public long getUnionDamage() {
        return unionDamage;
    }

    public void setUnionDamage(long unionDamage) {
        this.unionDamage = unionDamage;
    }

    public List<Integer> getSynergyGrid() {
        return synergyGrid;
    }

    public void setSynergyGrid(List<Integer> synergyGrid) {
        this.synergyGrid = synergyGrid;
    }

    public void recalcUnionPower() {
        setUnionPower(calculateTotalUnionPower());
    }

    public int calculateTotalUnionPower() {
        return calculateUnionAttackPower() + calculateUnionStarForcePower();
    }

    public int calculateUnionAttackPower() {
        double attackPower = 0.0;
        for (UnionMember um : getActiveMembers()) {
            double multiplier = UnionConstants.getUnionMultiplier((short) Math.min(Short.MAX_VALUE, um.getLevel()));
            attackPower += multiplier * Math.pow(um.getLevel(), 3) + 12500D;
        }
        return (int) attackPower;
    }

    public int calculateUnionStarForcePower() {
        int chuc = 0;
        for (UnionMember um : getActiveMembers()) {
            chuc += um.getChuc();
        }
        UnionConstants.UnionChucMultiplier mult = UnionConstants.getUnionChucMultiplier(chuc);
        double starforcePower = mult.firstMulti * Math.pow(chuc, 3) + mult.secondMulti * Math.pow(chuc, 2)
                + mult.thirdMulti * chuc + mult.fourthMulti;
        return (int) starforcePower;
    }

    public void encode(OutPacket outPacket) {
        for (int allocation : getSynergyGrid()) { // size = 8
            outPacket.encodeInt(allocation);
        }
        outPacket.encodeInt(getActiveMembers().size());
        for (UnionMember um : getActiveMembers()) {
            um.encode(outPacket, false);
        }
    }

    /**
     * Sets a position to the given Char in this board. Will create a new member if no matching one exists for the Char.
     * Deletes the member if grid is -1.
     *
     * @param chr      the Char to assign
     * @param rotation the rotation of the assignment
     * @param grid     the position of the assignment
     */
    public void setCharGridPos(Char chr, int rotation, int grid) {
        UnionMember member = getMemberById(chr.getId());
        if (grid == -1) {
            if (member != null) {
                member.deleteFromSQL();
                getActiveMembers().remove(member);
            }
            return;
        }
        if (member == null) {
            member = chr.createUnionMember();
            member.setUnionBoardID(getId());
            getActiveMembers().add(member);
        }
        member.setGridPos(grid);
        member.setGridRotation(rotation % 360);
        member.updateUnionMemberToSQL();
    }

    public UnionMember getMemberById(int charId) {
        return Util.findWithPred(getActiveMembers(), am -> am.getCharId() == charId);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void removeMemberByCharId(int id) {
        UnionMember um = getMemberById(id);
        if (um == null) {
            return;
        }
        um.deleteFromSQL();
        getActiveMembers().removeIf(x -> x.getCharId() == id);
    }

    public int getUnionId() {
        return unionId;
    }

    public void setUnionId(int unionId) {
        this.unionId = unionId;
    }
}
