package net.swordie.ms.connection.api.launcher.account;

import com.fasterxml.jackson.databind.JsonNode;
import net.swordie.ms.connection.api.launcher.LauncherContext;

import java.sql.Connection;

/**
 * What the {@link DashboardSection}s of one dashboard request share: the request, its one connection, the session's
 * user, and what earlier sections found (the character list, read by later sections such as worlds).
 */
public final class DashboardContext {
    private final LauncherContext launcher;
    private final JsonNode request;
    private final Connection connection;
    private final int userId;
    private CharacterList characters;

    DashboardContext(LauncherContext launcher, JsonNode request, Connection connection, int userId) {
        this.launcher = launcher;
        this.request = request;
        this.connection = connection;
        this.userId = userId;
    }

    public LauncherContext launcher() {
        return launcher;
    }

    public JsonNode request() {
        return request;
    }

    public Connection connection() {
        return connection;
    }

    /** The session's user (never a request field). */
    public int userId() {
        return userId;
    }

    /** Set by the characters section; null before it ran (or when it is not part of this dashboard). */
    public CharacterList characters() {
        return characters;
    }

    void characters(CharacterList characters) {
        this.characters = characters;
    }
}
