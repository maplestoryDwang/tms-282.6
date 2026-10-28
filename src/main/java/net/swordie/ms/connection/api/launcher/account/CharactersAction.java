package net.swordie.ms.connection.api.launcher.account;

import com.fasterxml.jackson.databind.JsonNode;
import net.swordie.ms.connection.api.launcher.LauncherCodes;
import net.swordie.ms.connection.api.launcher.LauncherContext;
import net.swordie.ms.connection.api.launcher.LauncherResult;
import net.swordie.ms.connection.api.launcher.SessionReadAction;

import java.sql.Connection;

/**
 * {@code characters} (session): the caller's characters over every world ({@link CharacterView}: job line name,
 * branch, collaboration flag, world, slot, last logout, deletion scheduled, online) and {@code lastPlayed}
 * ({@link CharacterList}). Only the session's user; a userId in the request is ignored.
 */
public final class CharactersAction extends SessionReadAction {
    private final CharacterReader reader;

    public CharactersAction() {
        this(new CharacterReader());
    }

    /** With another schema source (checks: a database without characters.deletionStartTime). */
    public CharactersAction(CharacterReader reader) {
        this.reader = reader;
    }

    @Override
    public String name() {
        return "characters";
    }

    @Override
    protected LauncherResult read(LauncherContext ctx, JsonNode request, Connection con, int userId) throws Exception {
        CharacterList list = reader.read(con, userId, ctx.services().memory());
        return LauncherResult.ok(LauncherCodes.OK).with("characters", list.characterMaps()).with("lastPlayed", list.lastPlayedMap());
    }
}
