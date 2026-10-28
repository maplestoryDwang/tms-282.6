package net.swordie.ms.connection.api.launcher.account;

import net.swordie.ms.connection.api.launcher.ServerMemory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The caller's characters ({@link CharacterSchema#query()}, SELECT only, filtered by the session's user id). Rows whose
 * stats are missing (broken avatardata link) are skipped. An online character takes its level / job / sub job from
 * server memory, which runs ahead of the last save.
 */
public final class CharacterReader {
    private final CharacterSchema.Source schemaSource;

    /** The connected database's schema ({@link CharacterSchema#detect}). */
    public CharacterReader() {
        this(CharacterSchema::detect);
    }

    public CharacterReader(CharacterSchema.Source schemaSource) {
        this.schemaSource = schemaSource;
    }

    public CharacterList read(Connection con, int userId, ServerMemory memory) throws SQLException {
        CharacterSchema schema = schemaSource.schema(con);
        List<CharacterView> out = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(schema.query())) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rs.getInt("statid");
                    if (rs.wasNull()) {
                        continue;
                    }
                    int id = rs.getInt("id");
                    String name = rs.getString("name");
                    int level = rs.getInt("level");
                    int job = rs.getInt("job");
                    int subJob = rs.getInt("subjob");
                    boolean deleting = schema.hasDeletionStartTime() && rs.getLong("deletionStartTime") != 0;
                    Optional<ServerMemory.CharacterLive> live = memory.onlineCharacter(id);
                    if (live.isPresent()) {
                        level = live.get().level();
                        job = live.get().job();
                        subJob = live.get().subJob();
                    }
                    out.add(new CharacterView(id, name == null ? "" : name, level, job, subJob, rs.getInt("worldid"),
                            rs.getInt("orderid"), AccountReader.epochMillis(rs.getTimestamp("lastlogout")), deleting,
                            live.isPresent()));
                }
            }
        }
        return CharacterList.of(CharacterExtras.apply(con, userId, out));
    }
}
