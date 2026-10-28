package net.swordie.ms.connection.api.launcher;

import com.fasterxml.jackson.databind.JsonNode;
import net.swordie.ms.Server;
import net.swordie.ms.client.Account;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.tms282.EconomyLocks;
import net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime;
import net.swordie.ms.client.social.Party.Party;
import net.swordie.ms.client.social.Guild.Guild;
import net.swordie.ms.client.social.Guild.Tms282GuildRuntime;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.util.Util;
import net.swordie.ms.world.World;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.TimeUnit;

/**
 * {@code deleteCharacter} (launcher, session + current password): the player removes one of their own characters.
 * <ul>
 *     <li>{@code {currentPassword, pin, confirm, characterId}} (launcher 1.4.1): deletes it now. {@code pin} is the
 *     second password - the same two secrets the select screen asks for - and {@code confirm} the character's name.
 *     It runs the game's own deletion (LoginHandler.handleReservedDeleteCharacterConfirm /
 *     Tms282CharacterHandlers.completeDeletion): refused while the character is in a guild or has guild business
 *     pending ({@code inGuild}), {@code Char.deleteFromSQL} in one transaction, then party, union boards and link
 *     skills. Not reversible.</li>
 *     <li>{@code {currentPassword, characterId}} without a pin (launcher 1.4.0): only writes a deletion reservation
 *     that is already due, so the select screen's delete (second password there) completes at once.</li>
 * </ul>
 * Refused while the account is in the game ({@code accountOnline}: the select screen holds the characters in memory)
 * and for a character that is not the caller's ({@code notFound}). A wrong second password counts as a failed login
 * for the back-off ({@code wrongPin}). Everything {@link AccountWriteAction} enforces applies: a live session, the
 * current password again, the login back-off, the {@link LauncherRateLimits#AUTH} bucket, only while the server runs.
 */
public final class DeleteCharacterAction extends AccountWriteAction {
    public static final String ACCOUNT_ONLINE = "accountOnline";
    public static final String NOT_FOUND = "notFound";
    public static final String WRONG_PIN = "wrongPin";
    public static final String IN_GUILD = "inGuild";
    /** The select screen's wait ({@code Tms282CharacterHandlers.DELETE_RESERVATION_MILLIS}) plus a minute of slack. */
    static final long BACKDATE_MS = TimeUnit.HOURS.toMillis(24) + TimeUnit.MINUTES.toMillis(1);

    @Override
    public String name() {
        return "deleteCharacter";
    }

    @Override
    protected String validate(JsonNode request, String currentPassword) {
        JsonNode id = request == null ? null : request.get("characterId");
        if (id == null || !id.isIntegralNumber() || !id.canConvertToInt() || id.intValue() <= 0) {
            return "characterId";
        }
        if (request.has("pin") && !request.get("pin").isNull()) {
            if (!request.get("pin").isTextual() || !PIN.matcher(request.get("pin").textValue()).matches()) {
                return "pin";
            }
            String confirm = LauncherJson.text(request, "confirm");
            if (confirm.isEmpty() || confirm.length() > 64) {
                return "confirm";
            }
        }
        return null;
    }

    @Override
    protected LauncherResult write(LauncherContext ctx, JsonNode request, String currentPassword, AccountSecretWriter.Row row)
            throws Exception {
        int characterId = request.get("characterId").intValue();
        boolean direct = request.has("pin") && !request.get("pin").isNull();
        if (direct && !pinMatches(request.get("pin").textValue(), row.pin())) {
            ctx.services().loginBackoff().failure(row.name(), ctx.sourceIp());
            return LauncherResult.fail(WRONG_PIN);
        }
        if (ctx.services().onlineAccounts().online(row.id())) {
            return LauncherResult.fail(ACCOUNT_ONLINE);
        }
        String name;
        int accountId;
        try (Connection con = DatabaseManager.getConnection()) {
            // ownership is part of the statement: the id alone never selects another account's character
            try (PreparedStatement ps = con.prepareStatement("SELECT cs.name, a.id FROM accounts a JOIN characters c ON c.accid = a.id"
                    + " LEFT JOIN avatardata ad ON ad.id = c.avatardata LEFT JOIN characterstats cs ON cs.id = ad.characterstat"
                    + " WHERE a.userid = ? AND c.id = ?")) {
                ps.setInt(1, row.id());
                ps.setInt(2, characterId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return LauncherResult.fail(NOT_FOUND);
                    }
                    name = rs.getString(1) == null ? "" : rs.getString(1);
                    accountId = rs.getInt(2);
                }
            }
            if (!direct) {
                try (PreparedStatement ps = con.prepareStatement("UPDATE characters c JOIN accounts a ON a.id = c.accid"
                        + " SET c.deletionStartTime = ? WHERE a.userid = ? AND c.id = ?")) {
                    ps.setLong(1, System.currentTimeMillis() - BACKDATE_MS);
                    ps.setInt(2, row.id());
                    ps.setInt(3, characterId);
                    if (ps.executeUpdate() != 1) {
                        return LauncherResult.fail(NOT_FOUND);
                    }
                }
                System.out.printf("[HttpAuthApi] deleteCharacter: account %d marked character %d deletable now (from %s)%n", row.id(), characterId, ctx.sourceIp());
                return LauncherResult.ok(LauncherCodes.OK).with("characterId", characterId).with("name", name).with("next", "selectScreen");
            }
        }
        if (!LauncherJson.text(request, "confirm").equalsIgnoreCase(name) || name.isEmpty()) {
            return LauncherResult.fail(LauncherCodes.INVALID_INPUT).with("problem", "confirm");
        }
        if (!Tms282GuildRuntime.canDeleteCharacter(characterId)) {
            return LauncherResult.fail(IN_GUILD);
        }
        String outcome = EconomyLocks.withGate(characterId, () -> {
            synchronized (Tms282VMatrixRuntime.ECONOMY_MONITOR) {
                return deleteNow(characterId, accountId);
            }
        });
        if (!outcome.isEmpty()) {
            return LauncherResult.fail(outcome);
        }
        System.out.printf("[HttpAuthApi] deleteCharacter: account %d DELETED character %d (%s) from %s%n", row.id(), characterId, name, ctx.sourceIp());
        return LauncherResult.ok(LauncherCodes.OK).with("characterId", characterId).with("name", name).with("deleted", true);
    }

    /** A BCrypt hash is checked as one; an older plain value by equality. Null / empty never matches. */
    static boolean pinMatches(String pin, String stored) {
        if (stored == null || stored.isEmpty()) {
            AccountSecrets.spendVerifyTime(pin);
            return false;
        }
        return Util.isStringBCrypt(stored) ? AccountSecrets.matches(pin, stored) : stored.equals(pin);
    }

    /**
     * The game's deletion for a character whose account is not in the game: rows first (one transaction, the id is 0
     * only after the commit), then what refers to it. "" on success, else the failure code. A cleanup error after the
     * rows are gone is logged and does not turn the answer into a failure - the character no longer exists.
     */
    private static String deleteNow(int characterId, int accountId) {
        Char chr = Char.getCharDeleteByID(characterId);
        if (chr == null) {
            return NOT_FOUND;
        }
        int guildId = chr.getGuildID();
        int partyId = chr.getPartyID();
        chr.deleteFromSQL();
        if (chr.getId() != 0) {
            return LauncherCodes.UNAVAILABLE;
        }
        try {
            World world = Server.get().getWorld();
            if (world != null) {
                Guild guild = guildId == 0 ? null : world.getGuildByID(guildId);
                if (guild != null && !Tms282GuildRuntime.enabled()) {
                    guild.removeMember(characterId);
                }
                Party party = partyId == 0 ? null : world.getPartyByPartyID(partyId);
                if (party != null) {
                    if (party.getMembers().size() == 1) {
                        party.disband();
                    } else {
                        party.leave(characterId);
                    }
                }
                Server.get().removeChar(characterId);
            }
            Account account = Account.getAccountFromSQLByAccountID(accountId);
            if (account != null) {
                if (account.getUnion() != null && account.getUnion().getUnionBoards() != null) {
                    for (var board : account.getUnion().getUnionBoards()) {
                        board.removeMemberByCharId(characterId);
                    }
                }
                if (account.getLinkSkills() != null) {
                    account.removeLinkSkillByChar(characterId);
                }
            }
        } catch (Exception e) {
            System.out.println("[HttpAuthApi] WARNING deleteCharacter " + characterId + ": party / union / link-skill cleanup failed: " + e);
        }
        return "";
    }
}
