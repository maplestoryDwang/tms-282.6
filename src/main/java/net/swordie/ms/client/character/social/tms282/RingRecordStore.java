package net.swordie.ms.client.character.social.tms282;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC access to the ring tables ({@link SocialTables#COUPLE}, {@link SocialTables#FRIEND},
 * {@link SocialTables#MARRIAGE}). A pair ring is stored once per side (character_id = owner), as in the TMS 279
 * schema, so each side loads its own record with one indexed query.
 */
public final class RingRecordStore {

    /** One side of a couple / friendship ring pair. */
    public record PairRing(RingKind kind, int characterId, int pairCharacterId, String pairName, int ringId,
                           long ringSn, long pairRingSn) {
        public PairRing {
            if (kind == RingKind.MARRIAGE) {
                throw new IllegalArgumentException("marriage rings are stored as a Marriage");
            }
            pairName = pairName == null ? "" : pairName;
        }

        public PairRing mirror(String ownerName) {
            return new PairRing(kind, pairCharacterId, characterId, ownerName, ringId, pairRingSn, ringSn);
        }
    }

    /** A marriage (stage 0 = engaged, 1 = married, 2 = divorced; divorced rows are not loaded). */
    public record Marriage(int marriageId, int groomId, int brideId, String groomName, String brideName, int stage,
                           int groomRingId, int brideRingId, long groomRingSn, long brideRingSn) {
        public static final int ENGAGED = 0;
        public static final int MARRIED = 1;
        public static final int DIVORCED = 2;

        public int partnerOf(int characterId) {
            return characterId == groomId ? brideId : groomId;
        }

        public String partnerNameOf(int characterId) {
            return characterId == groomId ? brideName : groomName;
        }

        public long ringSnOf(int characterId) {
            return characterId == groomId ? groomRingSn : brideRingSn;
        }

        public int ringIdOf(int characterId) {
            return characterId == groomId ? groomRingId : brideRingId;
        }
    }

    private RingRecordStore() {
    }

    private static String table(RingKind kind) {
        return kind == RingKind.COUPLE ? SocialTables.COUPLE : SocialTables.FRIEND;
    }

    public static List<PairRing> loadPairs(int characterId) throws SQLException {
        List<PairRing> out = new ArrayList<>();
        try (Connection c = SocialTables.connection()) {
            for (RingKind kind : new RingKind[]{RingKind.COUPLE, RingKind.FRIENDSHIP}) {
                try (PreparedStatement p = c.prepareStatement("SELECT pair_character_id, pair_name, ring_id, ring_sn, "
                        + "pair_ring_sn FROM " + table(kind) + " WHERE character_id=? ORDER BY created_at")) {
                    p.setInt(1, characterId);
                    try (ResultSet r = p.executeQuery()) {
                        while (r.next()) {
                            out.add(new PairRing(kind, characterId, r.getInt(1), r.getString(2), r.getInt(3),
                                    r.getLong(4), r.getLong(5)));
                        }
                    }
                }
            }
        }
        return out;
    }

    /** Inserts both sides in one transaction (replacing an earlier record with the same owner and ring serial). */
    public static void insertPair(PairRing side, String ownerName) throws SQLException {
        PairRing other = side.mirror(ownerName);
        String sql = "REPLACE INTO " + table(side.kind())
                + " (character_id, pair_character_id, pair_name, ring_id, ring_sn, pair_ring_sn) VALUES (?,?,?,?,?,?)";
        try (Connection c = SocialTables.connection()) {
            c.setAutoCommit(false);
            try (PreparedStatement p = c.prepareStatement(sql)) {
                for (PairRing r : List.of(side, other)) {
                    p.setInt(1, r.characterId());
                    p.setInt(2, r.pairCharacterId());
                    p.setString(3, r.pairName());
                    p.setInt(4, r.ringId());
                    p.setLong(5, r.ringSn());
                    p.setLong(6, r.pairRingSn());
                    p.addBatch();
                }
                p.executeBatch();
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }

    /** Deletes the pair that {@code ringSn} of {@code characterId} belongs to (both sides). Returns the rows removed. */
    public static int deletePair(RingKind kind, int characterId, long ringSn) throws SQLException {
        try (Connection c = SocialTables.connection(); PreparedStatement p = c.prepareStatement(
                "DELETE FROM " + table(kind) + " WHERE (character_id=? AND ring_sn=?) OR (pair_character_id=? AND pair_ring_sn=?)")) {
            p.setInt(1, characterId);
            p.setLong(2, ringSn);
            p.setInt(3, characterId);
            p.setLong(4, ringSn);
            return p.executeUpdate();
        }
    }

    /** The current (not divorced) marriage of {@code characterId}. */
    public static Optional<Marriage> loadMarriage(int characterId) throws SQLException {
        try (Connection c = SocialTables.connection(); PreparedStatement p = c.prepareStatement(
                "SELECT marriage_id, groom_id, bride_id, groom_name, bride_name, stage, groom_ring_id, bride_ring_id, "
                        + "groom_ring_sn, bride_ring_sn FROM " + SocialTables.MARRIAGE
                        + " WHERE (groom_id=? OR bride_id=?) AND stage<>? ORDER BY marriage_id DESC LIMIT 1")) {
            p.setInt(1, characterId);
            p.setInt(2, characterId);
            p.setInt(3, Marriage.DIVORCED);
            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) {
                    return Optional.empty();
                }
                return Optional.of(new Marriage(r.getInt(1), r.getInt(2), r.getInt(3), r.getString(4), r.getString(5),
                        r.getInt(6), r.getInt(7), r.getInt(8), r.getLong(9), r.getLong(10)));
            }
        }
    }

    /** Inserts a marriage and returns it with its id. */
    public static Marriage insertMarriage(Marriage m) throws SQLException {
        try (Connection c = SocialTables.connection(); PreparedStatement p = c.prepareStatement(
                "INSERT INTO " + SocialTables.MARRIAGE + " (groom_id, bride_id, groom_name, bride_name, stage, groom_ring_id, "
                        + "bride_ring_id, groom_ring_sn, bride_ring_sn) VALUES (?,?,?,?,?,?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            p.setInt(1, m.groomId());
            p.setInt(2, m.brideId());
            p.setString(3, m.groomName());
            p.setString(4, m.brideName());
            p.setInt(5, m.stage());
            p.setInt(6, m.groomRingId());
            p.setInt(7, m.brideRingId());
            p.setLong(8, m.groomRingSn());
            p.setLong(9, m.brideRingSn());
            p.executeUpdate();
            try (ResultSet k = p.getGeneratedKeys()) {
                int id = k.next() ? k.getInt(1) : 0;
                return new Marriage(id, m.groomId(), m.brideId(), m.groomName(), m.brideName(), m.stage(),
                        m.groomRingId(), m.brideRingId(), m.groomRingSn(), m.brideRingSn());
            }
        }
    }

    public static void updateMarriageStage(int marriageId, int stage) throws SQLException {
        try (Connection c = SocialTables.connection(); PreparedStatement p = c.prepareStatement(
                "UPDATE " + SocialTables.MARRIAGE + " SET stage=? WHERE marriage_id=?")) {
            p.setInt(1, stage);
            p.setInt(2, marriageId);
            p.executeUpdate();
        }
    }
}
