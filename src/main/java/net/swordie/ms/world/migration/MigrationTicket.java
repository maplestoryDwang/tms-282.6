package net.swordie.ms.world.migration;

import net.swordie.ms.client.Account;
import net.swordie.ms.client.Client;
import net.swordie.ms.client.User;

import java.util.Objects;

/**
 * Permission for one character to enter a channel once (value object). Issued where the client is told to
 * connect to a channel (character select, channel change, leaving the cash shop / auction house) and claimed by
 * the channel's MIGRATE_IN through {@link MigrationRegistry#claim}.
 *
 * @param characterId     character that may enter
 * @param userId          user (login account) ID; 0 when the issuer does not know it (265 channel change)
 * @param accountId       per-world account ID; 0 when unknown
 * @param worldId         world ID the ticket was issued for (282: the selected 282 world)
 * @param channelId       server channel ID (World.getChannelById) the client was sent to
 * @param clientChannelId channel ID as the client numbers it (282: zero-based wire channel; 265: channelId)
 * @param token           random value sent to the client with the channel address (282 LP0014); 0 when none.
 *                        Diagnostics only: the 282 CP007E does not echo it.
 * @param issuedAt        issue time in {@link MigrationRegistry} clock milliseconds
 * @param rule            how a MIGRATE_IN request is matched against this ticket
 * @param origin          client that asked for the migration (265 reuses its user and character); may be null
 * @param user            user object to continue with; null means {@code origin.getUser()}
 * @param account         world account to continue with; null means the user's account for the entry world
 * @param originIp        IP address of {@code origin} when the ticket was issued (null when unknown); a
 *                        {@link MatchRule#bindsSource() source-bound} rule only lets a MIGRATE_IN from this address claim it
 */
public record MigrationTicket(int characterId, int userId, int accountId, int worldId, byte channelId,
                              int clientChannelId, int token, long issuedAt, MatchRule rule,
                              Client origin, User user, Account account, String originIp) {

    /** How much of a MIGRATE_IN request must agree with the ticket. */
    public enum MatchRule {
        /** GMS 265 behaviour: the character ID alone (the old Channel.transfers lookup). */
        CHARACTER {
            @Override
            boolean matches(MigrationTicket t, MigrateInRequest r) {
                return t.characterId() == r.characterId();
            }

            @Override
            public boolean bindsSource() {
                return false;
            }
        },
        /**
         * TMS 282.3: character, user and world must all agree (the LP0014 token is not in CP007E), and the entering
         * socket must come from the IP address the ticket was issued to (health audit step 2).
         */
        CHARACTER_USER_WORLD {
            @Override
            boolean matches(MigrationTicket t, MigrateInRequest r) {
                return t.characterId() == r.characterId() && t.userId() == r.userId() && t.worldId() == r.worldId();
            }

            @Override
            public boolean bindsSource() {
                return true;
            }
        };

        abstract boolean matches(MigrationTicket ticket, MigrateInRequest request);

        /** True when only a MIGRATE_IN from the ticket's {@link MigrationTicket#originIp()} may claim it. */
        public abstract boolean bindsSource();
    }

    public MigrationTicket {
        Objects.requireNonNull(rule, "rule");
        if (characterId <= 0) {
            throw new IllegalArgumentException("characterId must be positive: " + characterId);
        }
    }

    /**
     * GMS 265 ticket (the former {@code Channel.addClientInTransfer}): matched by character only; the entering
     * socket continues with {@code origin}'s user, account and (on a channel change) character object.
     */
    public static MigrationTicket gms265(int characterId, byte channelId, Client origin, long issuedAt) {
        Objects.requireNonNull(origin, "origin");
        return new MigrationTicket(characterId, 0, 0, 0, channelId, channelId, 0, issuedAt, MatchRule.CHARACTER,
                origin, null, null, ipOf(origin));
    }

    /** TMS 282.3 ticket issued with LP0014: matched by character, user and world. */
    public static MigrationTicket tms282(int characterId, User user, Account account, byte channelId,
                                         int clientChannelId, int token, Client origin, long issuedAt) {
        Objects.requireNonNull(user, "user");
        Objects.requireNonNull(account, "account");
        return new MigrationTicket(characterId, user.getId(), account.getId(), account.getWorldId(), channelId,
                clientChannelId, token, issuedAt, MatchRule.CHARACTER_USER_WORLD, origin, user, account, ipOf(origin));
    }

    /** {@code client.getIP()}, or null when there is no client / address. */
    static String ipOf(Client client) {
        if (client == null) {
            return null;
        }
        try {
            return client.getIP();
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** True when {@code sourceIp} may claim this ticket under its rule. */
    public boolean acceptsSource(String sourceIp) {
        return !rule.bindsSource() || (originIp != null && originIp.equals(sourceIp));
    }

    public boolean matches(MigrateInRequest request) {
        return rule.matches(this, request);
    }

    /** Expired when strictly more than {@code ttlMillis} passed since issue. */
    public boolean expiredAt(long now, long ttlMillis) {
        return now - issuedAt > ttlMillis;
    }

    /** User to continue with: the ticket's own, else the origin client's. */
    public User resolveUser() {
        if (user != null) {
            return user;
        }
        return origin == null ? null : origin.getUser();
    }

    @Override
    public String toString() {
        return "MigrationTicket[character=" + characterId + ", user=" + userId + ", account=" + accountId
                + ", world=" + worldId + ", channel=" + channelId + "/" + clientChannelId + ", rule=" + rule
                + ", from=" + originIp + "]";
    }
}
