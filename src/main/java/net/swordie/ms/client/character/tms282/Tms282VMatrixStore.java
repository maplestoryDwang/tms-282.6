package net.swordie.ms.client.character.tms282;

import net.swordie.ms.connection.hikariCP.DatabaseManager;
import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

/** Separate282 tables; schema creation is deliberately external and never performed by this DAO. */
public final class Tms282VMatrixStore {
    @FunctionalInterface public interface CommitWork { void run(Connection transaction) throws SQLException; }
    private final DataSource source;
    public Tms282VMatrixStore() { this(null); }
    public Tms282VMatrixStore(DataSource source) { this.source = source; }
    private Connection connection() throws SQLException { return source == null ? DatabaseManager.getConnection() : source.getConnection(); }

    public Optional<Tms282VMatrixState> load(int characterId) throws SQLException {
        requireCharacter(characterId);
        try (var connection = connection()) {
            connection.setAutoCommit(false);
            try {
                var state = read(connection, characterId, true);
                connection.commit();
                return Optional.ofNullable(state);
            } catch (SQLException | RuntimeException e) { connection.rollback(); throw e; }
        }
    }

    /** Race-safe initialization: only the first insert seeds cores; existing authority is never overwritten. */
    public Tms282VMatrixState initialize(int characterId, Tms282VMatrixState seed) throws SQLException {
        requireCharacter(characterId);
        if (seed.revision() != 0) throw new IllegalArgumentException("Initial matrix revision must be zero");
        try (var connection = connection()) {
            connection.setAutoCommit(false);
            try {
                int inserted;
                try (var statement = connection.prepareStatement("INSERT IGNORE INTO character_vmatrix282 "
                        + "(character_id,points,revision,migration_pending,retained_ids) VALUES (?,?,0,?,?)")) {
                    statement.setInt(1, characterId); statement.setLong(2, seed.points());
                    statement.setBoolean(3, seed.migrationPending()); statement.setString(4, encodeSet(seed.retainedSet()));
                    inserted = statement.executeUpdate();
                }
                if (inserted == 1) writeCores(connection, characterId, seed);
                var current = read(connection, characterId, true);
                if (current == null) throw new SQLException("282 matrix initialization did not create state");
                connection.commit();
                return current;
            } catch (SQLException | RuntimeException e) { connection.rollback(); throw e; }
        }
    }

    /** InnoDB-only callback. Legacy Aria inventory/money MUST use Tms282VMatrixJournal instead. */
    public void commit(int characterId, long expectedRevision, Tms282VMatrixState next, CommitWork economicChanges) throws SQLException {
        requireCharacter(characterId); Objects.requireNonNull(economicChanges);
        if (next.revision() != Math.addExact(expectedRevision, 1)) throw new IllegalArgumentException("Invalid matrix revision transition");
        try (var connection = connection()) {
            connection.setAutoCommit(false);
            try {
                try (var statement = connection.prepareStatement("UPDATE character_vmatrix282 SET points=?,revision=?,"
                        + "migration_pending=?,retained_ids=? WHERE character_id=? AND revision=?")) {
                    statement.setLong(1, next.points()); statement.setLong(2, next.revision());
                    statement.setBoolean(3, next.migrationPending()); statement.setString(4, encodeSet(next.retainedSet()));
                    statement.setInt(5, characterId); statement.setLong(6, expectedRevision);
                    if (statement.executeUpdate() != 1) throw new SQLException("VMatrix state changed; reload before retry", "40001");
                }
                economicChanges.run(connection);
                try (var statement = connection.prepareStatement("DELETE FROM character_vmatrix282_core WHERE character_id=?")) {
                    statement.setInt(1, characterId); statement.executeUpdate();
                }
                writeCores(connection, characterId, next);
                connection.commit();
            } catch (SQLException | RuntimeException e) { connection.rollback(); throw e; }
        }
    }

    static Tms282VMatrixState read(Connection connection, int id, boolean lock) throws SQLException {
        long points, revision; boolean pending; Set<Integer> retained;
        try (var statement = connection.prepareStatement("SELECT points,revision,migration_pending,retained_ids "
                + "FROM character_vmatrix282 WHERE character_id=?" + (lock ? " FOR UPDATE" : ""))) {
            statement.setInt(1, id);
            try (var rows = statement.executeQuery()) {
                if (!rows.next()) return null;
                points = rows.getLong(1); revision = rows.getLong(2); pending = rows.getBoolean(3); retained = decodeSet(rows.getString(4));
            }
        }
        Map<Integer, Tms282VMatrixState.Core> cores = new TreeMap<>();
        try (var statement = connection.prepareStatement("SELECT core_id,level,selected,expires_at,protected_level "
                + "FROM character_vmatrix282_core WHERE character_id=?")) {
            statement.setInt(1, id);
            try (var rows = statement.executeQuery()) {
                while (rows.next()) {
                    int coreId = rows.getInt(1);
                    cores.put(coreId, new Tms282VMatrixState.Core(coreId, rows.getInt(2), rows.getBoolean(3), rows.getLong(4), rows.getInt(5)));
                }
            }
        }
        return new Tms282VMatrixState(points, revision, pending, cores, retained);
    }

    static void writeCores(Connection connection, int id, Tms282VMatrixState state) throws SQLException {
        if (state.cores().isEmpty()) return;
        try (var statement = connection.prepareStatement("INSERT INTO character_vmatrix282_core "
                + "(character_id,core_id,level,selected,expires_at,protected_level) VALUES (?,?,?,?,?,?)")) {
            for (var core : new TreeMap<>(state.cores()).values()) {
                statement.setInt(1, id); statement.setInt(2, core.id()); statement.setInt(3, core.level());
                statement.setBoolean(4, core.selected()); statement.setLong(5, core.expiresAt()); statement.setInt(6, core.protectedLevel());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }
    static String encodeSet(Set<Integer> values) {
        return new TreeSet<>(values).stream().map(String::valueOf).collect(Collectors.joining(","));
    }
    private static Set<Integer> decodeSet(String value) throws SQLException {
        if (value == null || value.isBlank()) return Set.of();
        try {
            Set<Integer> result = new TreeSet<>();
            for (String part : value.split(",", -1)) { int id = Integer.parseInt(part); if (id <= 0 || !result.add(id)) throw new NumberFormatException(); }
            return result;
        } catch (NumberFormatException e) { throw new SQLException("Invalid persisted282 matrix identifiers", e); }
    }
    private static void requireCharacter(int id) { if (id <= 0) throw new IllegalArgumentException("Invalid character ID"); }
}
