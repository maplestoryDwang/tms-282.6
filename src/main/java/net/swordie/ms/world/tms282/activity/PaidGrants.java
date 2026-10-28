package net.swordie.ms.world.tms282.activity;

import net.swordie.ms.client.character.Char;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * {@link PaidService.Grant}s that scripts can buy (fixes10): one-time rights written in the charge's transaction.
 * <p>
 * Atomicity: PaidService locks the buyer's {@code users} row FOR UPDATE before the grant runs, so every purchase of the
 * same login account (any character, any channel) waits for the previous one to commit or roll back; the grant then
 * reads the rights with a locking read (latest committed rows) and inserts only when there is none. The check, the
 * insert, the charge and the receipt commit together or not at all - no unique key (a schema change on live data) is
 * needed.
 */
public final class PaidGrants {
    private PaidGrants() {
    }

    /**
     * 高級戰令 of {@code season} for the buying character: one {@code zz_pass_vip} row (characters_id, season); a second
     * purchase of the same season answers LIMITED and charges nothing. Script:
     * {@code PaidService.charge(stage.getChr(), "rmb", "battle_pass", 68, "高級戰令 " + season, PaidGrants.battlePass(season))}.
     */
    public static PaidService.Grant battlePass(String season) {
        if (season == null || !season.matches("[0-9]{1,32}")) {
            throw new IllegalArgumentException("Invalid battle pass season");
        }
        return new BattlePass(season);
    }

    record BattlePass(String season) implements PaidService.Grant {
        @Override
        public List<String> tables() {
            return List.of("zz_pass_vip");
        }

        @Override
        public boolean grant(Connection c, Char chr) throws SQLException {
            try (PreparedStatement p = c.prepareStatement("SELECT id FROM zz_pass_vip WHERE characters_id=? AND season=? LIMIT 1 FOR UPDATE")) {
                p.setInt(1, chr.getId());
                p.setString(2, season);
                try (ResultSet r = p.executeQuery()) {
                    if (r.next()) {
                        return false;
                    }
                }
            }
            try (PreparedStatement p = c.prepareStatement("INSERT INTO zz_pass_vip(characters_id, season) VALUES (?, ?)")) {
                p.setInt(1, chr.getId());
                p.setString(2, season);
                if (p.executeUpdate() != 1) {
                    throw new SQLException("Battle pass was not saved");
                }
            }
            return true;
        }

        @Override
        public String describe() {
            return "zz_pass_vip season " + season;
        }
    }

    /** Whether the character already holds 高級戰令 of {@code season} (read-only, for the menu). */
    public static boolean hasBattlePass(Char chr, String season) {
        if (chr == null || season == null || !season.matches("[0-9]{1,32}")) {
            return false;
        }
        try (Connection c = net.swordie.ms.connection.hikariCP.DatabaseManager.getConnection();
             PreparedStatement p = c.prepareStatement("SELECT 1 FROM zz_pass_vip WHERE characters_id=? AND season=? LIMIT 1")) {
            p.setInt(1, chr.getId());
            p.setString(2, season);
            try (ResultSet r = p.executeQuery()) {
                return r.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }
}
