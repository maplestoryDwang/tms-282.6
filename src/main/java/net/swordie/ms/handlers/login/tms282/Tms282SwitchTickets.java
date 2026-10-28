package net.swordie.ms.handlers.login.tms282;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.function.LongSupplier;

/** Single-use handoff from an authenticated in-field character to an authenticated login session.
 * This credential is not a PIC and never changes the user's stored PIC.
 */
public final class Tms282SwitchTickets {
    private static final long TTL = 120_000;
    private final SecureRandom random = new SecureRandom();
    private final Map<Integer, Ticket> pending = new HashMap<>();
    private final LongSupplier clock;
    public record Target(int accountId, int characterId, int worldId, int channelId,
                         boolean fileReportVerified, String peerAddress) { }
    private record Ticket(Target target, String token, String authCookie, long created, boolean authUsed) {
        @Override public String toString() {
            return "Ticket[target=" + target + ", created=" + created + ", authUsed=" + authUsed + "]";
        }
    }

    public Tms282SwitchTickets() { this(System::currentTimeMillis); }
    public Tms282SwitchTickets(LongSupplier clock) { this.clock = java.util.Objects.requireNonNull(clock); }

    public synchronized String issue(int userId, int accountId, int characterId) {
        return issue(userId, accountId, characterId, 0, 0);
    }

    public synchronized String issue(int userId, int accountId, int characterId, int worldId, int channelId) {
        return issue(userId, accountId, characterId, worldId, channelId, false, "");
    }

    public synchronized String issue(int userId, int accountId, int characterId, int worldId, int channelId,
                                     boolean fileReportVerified, String peerAddress) {
        java.util.Objects.requireNonNull(peerAddress, "peerAddress");
        if (userId <= 0 || accountId <= 0 || characterId <= 0) throw new IllegalArgumentException("Invalid switch identity");
        if (worldId < 0 || worldId > 255 || channelId < 0 || channelId > 255) throw new IllegalArgumentException("Invalid switch world/channel");
        long now = clock.getAsLong();
        pending.values().removeIf(t -> now < t.created || now - t.created >= TTL);
        byte[] bytes = new byte[12];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        byte[] authBytes = new byte[24];
        random.nextBytes(authBytes);
        String authCookie = Base64.getUrlEncoder().withoutPadding().encodeToString(authBytes);
        pending.put(userId, new Ticket(new Target(accountId, characterId, worldId, channelId,
                fileReportVerified && !peerAddress.isBlank(), peerAddress), token, authCookie, now, false));
        return token;
    }

    /** Separate login-authentication credential; empty when absent, expired or already authenticated. */
    public synchronized String authCookieFor(int userId) {
        Ticket ticket = activeTicket(userId);
        return ticket == null || ticket.authUsed ? "" : ticket.authCookie;
    }

    /** Authenticate once from the issuing peer, retaining the independent CP0079 selection credential. */
    public synchronized java.util.Optional<Integer> consumeAuthentication(String cookie, String peer) {
        if (cookie == null || cookie.length() != 32 || peer == null || peer.isBlank())
            return java.util.Optional.empty();
        byte[] supplied = cookie.getBytes(StandardCharsets.UTF_8);
        long now = clock.getAsLong();
        var iterator = pending.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            Ticket ticket = entry.getValue();
            if (now < ticket.created || now - ticket.created >= TTL) {
                iterator.remove();
                continue;
            }
            if (ticket.authUsed || ticket.target.peerAddress.isBlank()
                    || !ticket.target.peerAddress.equals(peer)
                    || !MessageDigest.isEqual(ticket.authCookie.getBytes(StandardCharsets.US_ASCII), supplied)) continue;
            entry.setValue(new Ticket(ticket.target, ticket.token, ticket.authCookie, ticket.created, true));
            return java.util.Optional.of(entry.getKey());
        }
        return java.util.Optional.empty();
    }

    /** True only between this ticket's successful native authentication and final CP0079 consumption. */
    public synchronized boolean hasAuthenticatedRelogin(int userId) {
        Ticket ticket = activeTicket(userId);
        return ticket != null && ticket.authUsed;
    }

    private Ticket activeTicket(int userId) {
        Ticket ticket = pending.get(userId);
        if (ticket == null) return null;
        long now = clock.getAsLong();
        if (now < ticket.created || now - ticket.created >= TTL) {
            pending.remove(userId);
            return null;
        }
        return ticket;
    }

    /** Non-secret routing information; reading it never consumes or renews the credential. */
    public synchronized java.util.Optional<Target> pendingTarget(int userId) {
        Ticket ticket = activeTicket(userId);
        return ticket == null ? java.util.Optional.empty() : java.util.Optional.of(ticket.target);
    }

    public synchronized void cancel(int userId) { pending.remove(userId); }

    public synchronized boolean consume(int userId, int accountId, int characterId, String token) {
        Ticket ticket = activeTicket(userId);
        if (ticket == null) return false;
        if (ticket.target.accountId != accountId || ticket.target.characterId != characterId || token == null
                || !MessageDigest.isEqual(ticket.token.getBytes(StandardCharsets.US_ASCII), token.getBytes(StandardCharsets.UTF_8))) return false;
        pending.remove(userId);
        return true;
    }
}
