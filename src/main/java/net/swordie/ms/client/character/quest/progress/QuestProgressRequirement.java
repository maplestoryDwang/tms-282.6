package net.swordie.ms.client.character.quest.progress;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.loaders.DatSerializable;

public abstract class QuestProgressRequirement implements DatSerializable {

    private long id; // order of encoding for quest record messages
    private int order = 999;

    public abstract boolean isComplete(Char chr);

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public abstract QuestProgressRequirement deepCopy();

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    /** TMS parent saves use their own connection and publish this id only after commit. */
    public long insertToSQL(java.sql.Connection connection, int ownerId, long questId, boolean account) throws java.sql.SQLException {
        String type; Integer unit = null; int required, current = 0;
        if (this instanceof QuestProgressMobRequirement r) {type = "mob"; unit = r.getMobID(); required = r.getRequiredCount(); current = r.getCurrentCount();}
        else if (this instanceof QuestProgressItemRequirement r) {type = "item"; unit = r.getItemID(); required = r.getRequiredCount(); current = r.getCurrentCount();}
        else if (this instanceof QuestProgressMoneyRequirement r) {type = "money"; required = r.getMoney();}
        else if (this instanceof QuestProgressLevelRequirement r) {type = "level"; required = r.getLevel();}
        else return 0;
        String table = account ? "questprogressrequirements_acc" : "questprogressrequirements";
        String owner = account ? "accid" : "charid";
        if (getId() != 0) {
            if (this instanceof QuestProgressMobRequirement || this instanceof QuestProgressItemRequirement) {
                try (java.sql.PreparedStatement p = connection.prepareStatement("UPDATE " + table + " SET currentcount=? WHERE id=? AND " + owner + "=? AND questid=?")) {
                    p.setInt(1, current); p.setLong(2, getId()); p.setInt(3, ownerId); p.setLong(4, questId);
                    if (p.executeUpdate() != 1) throw new java.sql.SQLException("Quest progress update did not match its owner record");
                }
            }
            return getId();
        }
        try (java.sql.PreparedStatement p = connection.prepareStatement("INSERT INTO " + table
                + " (orderNum," + owner + ",progresstype,questid,unitid,requiredcount,currentcount) VALUES(?,?,?,?,?,?,?)", java.sql.Statement.RETURN_GENERATED_KEYS)) {
            p.setInt(1, getOrder()); p.setInt(2, ownerId); p.setString(3, type); p.setLong(4, questId);
            if (unit == null) p.setNull(5, java.sql.Types.INTEGER); else p.setInt(5, unit);
            p.setInt(6, required);
            p.setInt(7, current);
            if (p.executeUpdate() != 1) throw new java.sql.SQLException("Quest progress was not saved");
            try (java.sql.ResultSet r = p.getGeneratedKeys()) {
                if (!r.next() || r.getLong(1) <= 0) throw new java.sql.SQLException("Quest progress generated id is missing");
                return r.getLong(1);
            }
        }
    }
}
