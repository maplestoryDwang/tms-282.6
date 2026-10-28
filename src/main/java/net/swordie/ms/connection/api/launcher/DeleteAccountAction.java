package net.swordie.ms.connection.api.launcher;

import com.fasterxml.jackson.databind.JsonNode;
import net.swordie.ms.world.tms282.audit.BanType;
import net.swordie.ms.world.tms282.audit.BanService;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * {@code deleteAccount} (launcher 1.4, session + current password) {currentPassword, confirm}: the player closes
 * their own account. {@code confirm} must be the account name (typed again in the launcher).
 * <p>
 * It is a closure, not an erasure: the account gets a permanent login block ({@link BanType#GENERAL}, reason
 * {@link #REASON}) and every launcher session of it is revoked, so nobody can log in to the launcher or the game
 * with it any more; the rows stay, which lets the administrator undo a deletion made by mistake or with a stolen
 * password (admin console: unban). The name is not freed. Refused while the account is in the game
 * ({@code accountOnline}). Everything {@link AccountWriteAction} enforces applies: a live session, the current
 * password again, the login back-off, the {@link LauncherRateLimits#AUTH} bucket, only while the server runs.
 */
public final class DeleteAccountAction extends AccountWriteAction {
    public static final String REASON = "帳號已由玩家本人在登入器刪除";
    static final long HOURS = 24L * 3650;

    @Override
    public String name() {
        return "deleteAccount";
    }

    @Override
    protected String validate(JsonNode request, String currentPassword) {
        String confirm = LauncherJson.text(request, "confirm");
        return confirm.isEmpty() || confirm.length() > 64 ? "confirm" : null;
    }

    @Override
    protected LauncherResult write(LauncherContext ctx, JsonNode request, String currentPassword, AccountSecretWriter.Row row)
            throws Exception {
        if (!LauncherJson.text(request, "confirm").equalsIgnoreCase(row.name())) {
            return LauncherResult.fail(LauncherCodes.INVALID_INPUT).with("problem", "confirm");
        }
        if (ctx.services().onlineAccounts().online(row.id())) {
            return LauncherResult.fail(DeleteCharacterAction.ACCOUNT_ONLINE);
        }
        Boolean written;
        try {
            written = BanService.tempBan(row.id(), BanType.GENERAL, HOURS, REASON).get(10, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            written = null;
        }
        if (!Boolean.TRUE.equals(written)) {
            return LauncherResult.fail(LauncherCodes.UNAVAILABLE);
        }
        int revoked = ctx.services().sessions().revokeAll(row.id());
        System.out.printf("[HttpAuthApi] deleteAccount: account %d closed by its owner (from %s; %d session(s) revoked)%n", row.id(), ctx.sourceIp(), revoked);
        return LauncherResult.ok(LauncherCodes.OK).with("closed", true);
    }
}
