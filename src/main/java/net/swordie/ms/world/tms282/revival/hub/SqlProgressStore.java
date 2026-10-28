package net.swordie.ms.world.tms282.revival.hub;

import net.swordie.ms.connection.hikariCP.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

/** {@code revival_progress} (created on first use; sql/20261005-revival-progress.sql is the same DDL). */
public final class SqlProgressStore implements ProgressStore {
    public static final String DDL = "CREATE TABLE IF NOT EXISTS revival_progress (charid INT NOT NULL, event VARCHAR(24) NOT NULL, "
            + "mission VARCHAR(24) NOT NULL, period VARCHAR(16) NOT NULL, count INT NOT NULL DEFAULT 0, "
            + "claimed TINYINT NOT NULL DEFAULT 0, updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, "
            + "PRIMARY KEY (charid, event, mission, period)) ENGINE=InnoDB";
    private volatile boolean ready;

    private void schema(Connection c) throws Exception {
        if (ready) return;
        try (Statement s = c.createStatement()) {
            s.executeUpdate(DDL);
        }
        ready = true;
    }

    @Override
    public Map<String, Row> load(int charId, String event) throws Exception {
        Map<String, Row> out = new HashMap<>();
        try (Connection c = DatabaseManager.getConnection()) {
            schema(c);
            try (PreparedStatement p = c.prepareStatement("SELECT mission, period, count, claimed FROM revival_progress WHERE charid=? AND event=?")) {
                p.setInt(1, charId);
                p.setString(2, event);
                try (ResultSet r = p.executeQuery()) {
                    while (r.next()) out.put(ProgressStore.key(r.getString(1), r.getString(2)), new Row(r.getInt(3), r.getInt(4) != 0));
                }
            }
        }
        return out;
    }

    @Override
    public void save(int charId, String event, String mission, String period, int count, boolean claimed) throws Exception {
        try (Connection c = DatabaseManager.getConnection()) {
            schema(c);
            try (PreparedStatement p = c.prepareStatement("INSERT INTO revival_progress (charid, event, mission, period, count, claimed) "
                    + "VALUES (?,?,?,?,?,?) ON DUPLICATE KEY UPDATE count=GREATEST(count, VALUES(count)), claimed=GREATEST(claimed, VALUES(claimed))")) {
                p.setInt(1, charId);
                p.setString(2, event);
                p.setString(3, mission);
                p.setString(4, period);
                p.setInt(5, count);
                p.setInt(6, claimed ? 1 : 0);
                p.executeUpdate();
            }
        }
    }
}
