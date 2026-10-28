package net.swordie.ms.world.tms282.contents.changeburning.dream;

import net.swordie.ms.connection.hikariCP.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 夢幻的裝備 persistence: one bag row (dust, next serial) and one row per equip, changed together in one transaction
 * ({@link #commit}) so dust and items can never drift apart. Tables are created with CREATE TABLE IF NOT EXISTS (the same
 * statements open deploy/content-1005/sql/20261005-dream-equipment.sql, compared by tools/Tms282DreamEquipmentCheck).
 */
public interface DreamStore {
    /** One equip. {@code pos} &gt; 0 = bag slot, &lt; 0 = equipped slot -bodyPart (推定 encoding). */
    record Equip(long sn, int infoIndex, int grade, int bodyPart, int enhance, int pos, int flag) {
        public Equip at(int newPos) {
            return new Equip(sn, infoIndex, grade, bodyPart, enhance, newPos, flag);
        }

        public Equip enhanced(int level) {
            return new Equip(sn, infoIndex, grade, bodyPart, level, pos, flag);
        }

        public boolean equipped() {
            return pos < 0;
        }
    }

    record Bag(long dust, long nextSn, List<Equip> equips) {
        public Bag {
            equips = List.copyOf(equips);
        }

        public static Bag empty() {
            return new Bag(0, 1, List.of());
        }

        public Equip at(int pos) {
            for (Equip e : equips) {
                if (e.pos() == pos) {
                    return e;
                }
            }
            return null;
        }
    }

    /** The changes of one command: rows to write, serials to delete, the new dust and next serial. */
    record Change(List<Equip> upserts, List<Long> deletes, long dust, long nextSn) {
        public Change {
            upserts = List.copyOf(upserts);
            deletes = List.copyOf(deletes);
        }
    }

    Bag load(int characterId) throws SQLException;

    void commit(int characterId, Change change) throws SQLException;

    String[] DDL = {
            "CREATE TABLE IF NOT EXISTS tms282_dream_bag (character_id INT NOT NULL,dust BIGINT NOT NULL,next_sn BIGINT NOT NULL,"
                    + "updated_at BIGINT NOT NULL,PRIMARY KEY (character_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci",
            "CREATE TABLE IF NOT EXISTS tms282_dream_items (character_id INT NOT NULL,sn BIGINT NOT NULL,info_index INT NOT NULL,"
                    + "grade INT NOT NULL,body_part INT NOT NULL,enhance INT NOT NULL,pos INT NOT NULL,flag INT NOT NULL,"
                    + "PRIMARY KEY (character_id, sn)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci",
    };

    /** In-memory store (tests). {@link #failNext} makes the next commit throw before anything changes. */
    final class Memory implements DreamStore {
        private final Map<Integer, Bag> bags = new HashMap<>();
        public boolean failNext;
        public int commits;

        @Override
        public synchronized Bag load(int characterId) {
            return bags.getOrDefault(characterId, Bag.empty());
        }

        @Override
        public synchronized void commit(int characterId, Change change) throws SQLException {
            if (failNext) {
                failNext = false;
                throw new SQLException("simulated failure");
            }
            Map<Long, Equip> rows = new java.util.LinkedHashMap<>();
            for (Equip e : load(characterId).equips()) {
                rows.put(e.sn(), e);
            }
            change.deletes().forEach(rows::remove);
            change.upserts().forEach(e -> rows.put(e.sn(), e));
            bags.put(characterId, new Bag(change.dust(), change.nextSn(), new ArrayList<>(rows.values())));
            commits++;
        }
    }

    /** The server tables. */
    final class Sql implements DreamStore {
        private volatile boolean ready;

        private Connection open() throws SQLException {
            Connection c = DatabaseManager.getConnection();
            if (!ready) {
                try (Statement s = c.createStatement()) {
                    for (String ddl : DDL) {
                        s.executeUpdate(ddl);
                    }
                }
                ready = true;
            }
            return c;
        }

        @Override
        public Bag load(int characterId) throws SQLException {
            try (Connection c = open()) {
                long dust = 0, next = 1;
                try (PreparedStatement p = c.prepareStatement("SELECT dust,next_sn FROM tms282_dream_bag WHERE character_id=?")) {
                    p.setInt(1, characterId);
                    try (ResultSet r = p.executeQuery()) {
                        if (r.next()) {
                            dust = r.getLong(1);
                            next = r.getLong(2);
                        }
                    }
                }
                List<Equip> equips = new ArrayList<>();
                try (PreparedStatement p = c.prepareStatement(
                        "SELECT sn,info_index,grade,body_part,enhance,pos,flag FROM tms282_dream_items WHERE character_id=? ORDER BY sn")) {
                    p.setInt(1, characterId);
                    try (ResultSet r = p.executeQuery()) {
                        while (r.next()) {
                            equips.add(new Equip(r.getLong(1), r.getInt(2), r.getInt(3), r.getInt(4), r.getInt(5), r.getInt(6), r.getInt(7)));
                        }
                    }
                }
                return new Bag(dust, next, equips);
            }
        }

        @Override
        public void commit(int characterId, Change change) throws SQLException {
            try (Connection c = open()) {
                boolean auto = c.getAutoCommit();
                c.setAutoCommit(false);
                try {
                    try (PreparedStatement p = c.prepareStatement("DELETE FROM tms282_dream_items WHERE character_id=? AND sn=?")) {
                        for (long sn : change.deletes()) {
                            p.setInt(1, characterId);
                            p.setLong(2, sn);
                            p.addBatch();
                        }
                        p.executeBatch();
                    }
                    try (PreparedStatement p = c.prepareStatement("INSERT INTO tms282_dream_items(character_id,sn,info_index,grade,"
                            + "body_part,enhance,pos,flag) VALUES(?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE info_index=VALUES(info_index),"
                            + "grade=VALUES(grade),body_part=VALUES(body_part),enhance=VALUES(enhance),pos=VALUES(pos),flag=VALUES(flag)")) {
                        for (Equip e : change.upserts()) {
                            p.setInt(1, characterId);
                            p.setLong(2, e.sn());
                            p.setInt(3, e.infoIndex());
                            p.setInt(4, e.grade());
                            p.setInt(5, e.bodyPart());
                            p.setInt(6, e.enhance());
                            p.setInt(7, e.pos());
                            p.setInt(8, e.flag());
                            p.addBatch();
                        }
                        p.executeBatch();
                    }
                    try (PreparedStatement p = c.prepareStatement("INSERT INTO tms282_dream_bag(character_id,dust,next_sn,updated_at)"
                            + " VALUES(?,?,?,?) ON DUPLICATE KEY UPDATE dust=VALUES(dust),next_sn=VALUES(next_sn),updated_at=VALUES(updated_at)")) {
                        p.setInt(1, characterId);
                        p.setLong(2, change.dust());
                        p.setLong(3, change.nextSn());
                        p.setLong(4, System.currentTimeMillis());
                        p.executeUpdate();
                    }
                    c.commit();
                } catch (SQLException | RuntimeException e) {
                    c.rollback();
                    throw e;
                } finally {
                    c.setAutoCommit(auto);
                }
            }
        }
    }
}
