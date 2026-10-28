package net.swordie.ms.connection.api.launcher;

import com.fasterxml.jackson.databind.JsonNode;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.constants.DevAccounts;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Locale;

/**
 * {@code checkName} (P7, public) {username}: whether register would accept the name - {@code available:false} with
 * {@code reason} {@code reserved} (DevAccounts.isForbiddenID) or {@code taken} (users.name, compared the way the
 * unique index compares). A malformed name answers {@code invalidInput} (the launcher checks the format itself).
 * SELECT only; own bucket {@link LauncherRateLimits#NAME} (10 per IP per minute) against enumeration.
 */
public final class CheckNameAction implements LauncherAction {

    @Override
    public String name() {
        return "checkName";
    }

    @Override
    public boolean requiresSession() {
        return false;
    }

    @Override
    public boolean mutating() {
        return false;
    }

    @Override
    public String rateBucket() {
        return LauncherRateLimits.NAME;
    }

    @Override
    public boolean remoteAllowed() {
        return true;
    }

    @Override
    public LauncherTransport transport() {
        return LauncherTransport.ANY;
    }

    @Override
    public LauncherResult execute(LauncherContext ctx, JsonNode request) throws Exception {
        String name = LauncherJson.text(request, "username");
        if (!AccountCredentials.validUsername(name)) {
            return LauncherResult.fail(LauncherCodes.INVALID_INPUT).with("problem", "username");
        }
        if (DevAccounts.isForbiddenID(name.toLowerCase(Locale.ROOT))) {
            return LauncherResult.ok(LauncherCodes.OK).with("available", false).with("reason", "reserved");
        }
        if (!LocalDatabaseGuard.accepted()) {
            return LauncherResult.fail(LauncherCodes.LOCAL_ONLY);
        }
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT 1 FROM users WHERE name = ? LIMIT 1")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return LauncherResult.ok(LauncherCodes.OK).with("available", false).with("reason", "taken");
                }
            }
        }
        return LauncherResult.ok(LauncherCodes.OK).with("available", true);
    }
}
