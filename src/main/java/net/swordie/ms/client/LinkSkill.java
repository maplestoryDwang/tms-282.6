package net.swordie.ms.client;

import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.util.DataPrinter;
import net.swordie.ms.util.FileTime;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class LinkSkill {

    private int id;
    private int accID;
    private int ownerID;
    private int linkCount;
    private int linkSkillID;
    private int level;
    private FileTime addedDate;

    public static Set<LinkSkill> getLinkSkillsFromSQLByAccountID(int accountID) {
        Set<LinkSkill> linkSkills = new HashSet<>();
        String query = "SELECT * FROM linkskills WHERE accid = ?";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, accountID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LinkSkill linkSkill = new LinkSkill();
                    linkSkill.setId(rs.getInt("id"));
                    linkSkill.setAccID(rs.getInt("accid"));
                    linkSkill.setOwnerID(rs.getInt("ownerid"));
                    linkSkill.setLinkCount(rs.getInt("linkedcharid"));
                    linkSkill.setLinkSkillID(rs.getInt("linkskillid"));
                    linkSkill.setLevel(rs.getInt("level"));
                    linkSkill.setAddedDate(DatabaseManager.getFileTimeFromString(rs.getString("addeddate")));
                    linkSkills.add(linkSkill);
                }
            }
        } catch (Exception exception) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, exception);
        }
        return linkSkills;
    }

    public void updateLinkSkillToSQL() {
        if (getId() == 0) {
            String query = "INSERT INTO `linkskills` (" +
                    "`accid`, " +
                    "`ownerid`, " +
                    "`linkedcharid`, " +
                    "`linkskillid`, " +
                    "`level`, " +
                    "`addeddate` " +
                    ") VALUES (" +
                    String.format("%d, ", getAccID()) +
                    String.format("%d, ", getOwnerID()) +
                    String.format("%d, ", getLinkCount()) +
                    String.format("%d, ", getLinkSkillID()) +
                    String.format("%d, ", getLevel()) +
                    DatabaseManager.getSQLStringSyntax(false, "", getAddedDate(), true) +
                    ");";
            int id = (int) DatabaseManager.executeStatementReturnID(query);
            setId(id);
        } else {
            String query = "UPDATE linkskills SET " +
                    String.format("accid = %d, ", getAccID()) +
                    String.format("ownerid = %d, ", getOwnerID()) +
                    String.format("linkedcharid = %d, ", getLinkCount()) +
                    String.format("linkskillid = %d, ", getLinkSkillID()) +
                    String.format("level = %d, ", getLevel()) +
                    DatabaseManager.getSQLStringSyntax(true, "addeddate", getAddedDate(), true) +
                    String.format("WHERE id = %d;", getId());
            DatabaseManager.executeStatement(query);
        }
    }

    public void deleteLinkSkillFromSQL() {
        String query = "DELETE FROM `linkskills` WHERE " +
                String.format("`id` = %d", getId());
        net.swordie.ms.connection.hikariCP.DatabaseManager.executeStatement(query);
        setId(0);
    }

    public LinkSkill() {
    }

    public LinkSkill(int accID, int ownerID, int linkSkillID, int linkCount, int level, FileTime addedDate) {
        this.accID = accID;
        this.ownerID = ownerID;
        this.linkSkillID = linkSkillID;
        this.linkCount = linkCount;
        this.level = level;
        this.addedDate = addedDate;
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

    public int getOwnerID() {
        return ownerID;
    }

    public void setOwnerID(int ownerID) {
        this.ownerID = ownerID;
    }

    public int getLinkCount() {
        return linkCount;
    }

    public void setLinkCount(int linkCount) {
        this.linkCount = linkCount;
    }

    public int getLinkSkillID() {
        return linkSkillID;
    }

    public void setLinkSkillID(int linkSkillID) {
        this.linkSkillID = linkSkillID;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public FileTime getAddedDate() {
        return addedDate;
    }

    public void setAddedDate(FileTime addedDate) {
        this.addedDate = addedDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LinkSkill linkSkill = (LinkSkill) o;
        return ownerID == linkSkill.ownerID &&
                linkSkillID == linkSkill.linkSkillID;
    }

    @Override
    public int hashCode() {

        return Objects.hash(ownerID, linkSkillID);
    }
}
