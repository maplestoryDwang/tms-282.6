package net.swordie.ms.client.character.skills;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.util.DataPrinter;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SequenceSkill {

    public static final int MAX_ROW = 10;
    public int id;
    public int charid;
    public int index;
    public String name = "";
    public List<Integer> skillIds = new ArrayList<>();

    public static List<SequenceSkill> getSkillSequenceSkillsByCharID(int charID) {
        List<SequenceSkill> out = new ArrayList<>();
        String sql = "SELECT id,charid,`index`,name,"
                + "skill1,skill2,skill3,skill4,skill5,skill6,skill7,skill8,skill9,skill10,"
                + "skill11,skill12,skill13,skill14,skill15 "
                + "FROM skillsequences_skills WHERE charid=? ORDER BY `index`";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, charID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    SequenceSkill seq = new SequenceSkill();
                    seq.setId(rs.getInt("id"));
                    seq.setCharId(rs.getInt("charid"));
                    seq.setIndex(rs.getInt("index"));
                    seq.setName(rs.getString("name"));

                    List<Integer> skills = new ArrayList<>(15);
                    for (int k = 1; k <= 15; k++) {
                        Integer v = rs.getObject("skill" + k, Integer.class);
                        skills.add(v != null ? v : 0);
                    }
                    seq.setSkillIds(skills);
                    out.add(seq);
                }
            }
        } catch (SQLException e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
        }
        return out;
    }

    public void saveToSQL() {
        final String INS = "INSERT INTO skillsequences_skills "
                + "(charid,`index`,name,skill1,skill2,skill3,skill4,skill5,skill6,skill7,skill8,skill9,skill10,"
                + "skill11,skill12,skill13,skill14,skill15) VALUES "
                + "(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        final String UPD = "UPDATE skillsequences_skills SET "
                + "charid=?,`index`=?,name=?,"
                + "skill1=?,skill2=?,skill3=?,skill4=?,skill5=?,skill6=?,skill7=?,skill8=?,skill9=?,skill10=?,"
                + "skill11=?,skill12=?,skill13=?,skill14=?,skill15=? "
                + "WHERE id=?";

        List<Integer> skills = getSkillIds();
        if (skills == null || skills.size() != 15) throw new IllegalStateException("skillIds must have 15 items");

        try (Connection con = DatabaseManager.getConnection()) {
            if (getId() == 0) {
                try (PreparedStatement ps = con.prepareStatement(INS, Statement.RETURN_GENERATED_KEYS)) {
                    int p = 1;
                    ps.setInt(p++, getCharId());
                    ps.setInt(p++, getIndex());
                    ps.setString(p++, getName());
                    for (int k = 0; k < 15; k++) ps.setInt(p++, skills.get(k));
                    ps.executeUpdate();
                    try (ResultSet gk = ps.getGeneratedKeys()) {
                        if (gk.next()) setId(gk.getInt(1));
                    }
                }
            } else {
                try (PreparedStatement ps = con.prepareStatement(UPD)) {
                    int p = 1;
                    ps.setInt(p++, getCharId());
                    ps.setInt(p++, getIndex());
                    ps.setString(p++, getName());
                    for (int k = 0; k < 15; k++) ps.setInt(p++, skills.get(k));
                    ps.setInt(p++, getId());
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
        }
    }

    public void deleteFromSQL() {
        if (getId() == 0) return;
        String sql = "DELETE FROM skillsequences_skills WHERE id=?";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, getId());
            ps.executeUpdate();
            setId(0);
        } catch (SQLException e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
        }
    }

    public SequenceSkill() {
    }

    public SequenceSkill(int charid, int index, String name, List<Integer> skillIds) {
        this.charid = charid;
        this.index = index;
        this.name = name;
        this.skillIds = skillIds;
    }

    public void encode(OutPacket outPacket) {
        outPacket.encodeString(getName());
        for (Integer skillId : getSkillIds()) {
            outPacket.encodeInt(skillId);
        }
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public int getCharId() {
            return charid;
        }

        public void setCharId(int charid) {
            this.charid = charid;
        }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Integer> getSkillIds() {
        return skillIds;
    }

    public void setSkillIds(List<Integer> skillIds) {
        this.skillIds = skillIds;
    }
}
