package net.swordie.ms.client.character.keys;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.Macro;
import net.swordie.ms.client.character.tms282.Tms282HiddenSkillParents;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.loaders.Tms282SkillRequirements;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/** Macro storage grants no skills. The client's full special-skill/cooldown blacklist remains authoritative for
 * its UI; this boundary accepts only learned visible action IDs, never derived children or passive IDs. */
public final class Tms282MacroService {
    private Tms282MacroService() { }

    public static void validate(Char chr, List<Macro> macros) {
        for (Macro macro : macros) for (int id : macro.getSkills()) {
            if (id == 0) continue;
            var info = SkillData.getSkillInfoById(id);
            int digit = id / 1000 % 10; // Native 10A47F0 -> 11E5860 rejects 0 and 9.
            if (info == null || !Tms282SkillRequirements.hasOriginalSkill(id) || info.isInvisible()
                    || digit == 0 || digit == 9 || chr.getSkillLevel(id) <= 0
                    || Tms282HiddenSkillParents.useLevel(chr, id) <= 0) {
                throw new IllegalArgumentException("macro contains an unavailable action skill " + id);
            }
        }
    }

    /** Replace only this character's macros in one transaction. Caller publishes the candidate list after commit.
     * The channel route and ordinary character saves share the existing character/save-gate locks. */
    public static void save(Char chr, List<Macro> macros) {
        if (chr.getId() <= 0) throw new IllegalArgumentException("unsaved character");
        boolean committed = false;
        try (Connection con = DatabaseManager.getConnection()) {
            con.setAutoCommit(false);
            try {
                try (var ps = con.prepareStatement("DELETE ms FROM macroskills ms JOIN macros m ON m.id=ms.macroid WHERE m.charid=?")) {
                    ps.setInt(1, chr.getId()); ps.executeUpdate();
                }
                try (var ps = con.prepareStatement("DELETE FROM macros WHERE charid=?")) {
                    ps.setInt(1, chr.getId()); ps.executeUpdate();
                }
                for (Macro macro : macros) {
                    try (var ps = con.prepareStatement("INSERT INTO macros (charid,muted,name) VALUES (?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
                        ps.setInt(1, chr.getId()); ps.setBoolean(2, macro.isMuted()); ps.setString(3, macro.getName());
                        if (ps.executeUpdate() != 1) throw new SQLException("macro insert failed");
                        try (var keys = ps.getGeneratedKeys()) {
                            if (!keys.next()) throw new SQLException("macro ID missing");
                            macro.setId(keys.getLong(1)); macro.setCharID(chr.getId());
                        }
                    }
                    try (var ps = con.prepareStatement("INSERT INTO macroskills (macroid,ordercol,skillid) VALUES (?,?,?)")) {
                        for (int slot = 0; slot < 3; slot++) {
                            ps.setLong(1, macro.getId()); ps.setInt(2, slot); ps.setInt(3, macro.getSkills()[slot]);
                            if (ps.executeUpdate() != 1) throw new SQLException("macro skill insert failed");
                        }
                    }
                }
                con.commit();
                committed = true;
            } catch (SQLException | RuntimeException e) {
                try { con.rollback(); } catch (SQLException rollback) { e.addSuppressed(rollback); }
                throw e;
            }
        } catch (SQLException e) {
            if (!committed) throw new IllegalStateException("macro transaction failed", e);
            // The rows are durable. A pool cleanup failure must not restore stale in-memory row IDs.
            System.out.println("[TMS282] macro connection cleanup failed after commit; character=" + chr.getId());
        }
    }
}
