package net.swordie.ms.world.migration;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.LongSupplier;
import java.util.function.Predicate;

/**
 * Pending channel migrations, one {@link MigrationTicket} per character (replaces {@code Channel.transfers}).
 * <ul>
 *     <li>TTL: a ticket the client never uses (closed, crashed, wrong channel) expires after {@link #ttl()};</li>
 *     <li>one-time: {@link #claim} removes the ticket atomically, so a replayed MIGRATE_IN finds nothing;</li>
 *     <li>matching: the ticket's {@link MigrationTicket.MatchRule} (265: character; 282: character + user + world,
 *     from the IP address the ticket was issued to). A request that does not match leaves the ticket in place for
 *     the right client.</li>
 *     <li>a client whose socket closes outside a migration hand-off withdraws its tickets ({@link #discardIssuedBy}).</li>
 * </ul>
 * Thread-safe. The clock is injectable for checks.
 */
public final class MigrationRegistry {
    /** Health audit step 2: 60 s (was 5 minutes); the client connects to the channel within a few seconds. */
    public static final Duration DEFAULT_TTL = Duration.ofSeconds(60);

    private final ConcurrentMap<Integer, MigrationTicket> byCharacter = new ConcurrentHashMap<>();
    private final LongSupplier clock;
    private final long ttlMillis;

    public MigrationRegistry() {
        this(System::currentTimeMillis, DEFAULT_TTL);
    }

    public MigrationRegistry(LongSupplier clock, Duration ttl) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.ttlMillis = Objects.requireNonNull(ttl, "ttl").toMillis();
        if (ttlMillis <= 0) {
            throw new IllegalArgumentException("TTL must be positive: " + ttl);
        }
    }

    /** Current time of this registry's clock (ticket issue times use it). */
    public long now() {
        return clock.getAsLong();
    }

    public Duration ttl() {
        return Duration.ofMillis(ttlMillis);
    }

    /** Adds a ticket, replacing the character's previous one, and drops expired tickets. */
    public void issue(MigrationTicket ticket) {
        Objects.requireNonNull(ticket, "ticket");
        purgeExpired();
        byCharacter.put(ticket.characterId(), ticket);
    }

    /**
     * One-time use: the character's unexpired ticket, removed, when it matches {@code request}. Empty when there
     * is none, it expired (then it is removed too) or the request does not match (then it stays).
     */
    public Optional<MigrationTicket> claim(MigrateInRequest request) {
        return claim(request, null);
    }

    /**
     * As {@link #claim(MigrateInRequest)}, for a MIGRATE_IN that arrived from {@code sourceIp}: a source-bound ticket
     * (282) is only claimed from the address it was issued to; from another address it stays in place and one line is
     * logged.
     */
    public Optional<MigrationTicket> claim(MigrateInRequest request, String sourceIp) {
        Objects.requireNonNull(request, "request");
        MigrationTicket ticket = byCharacter.get(request.characterId());
        if (ticket == null) {
            return Optional.empty();
        }
        if (ticket.expiredAt(now(), ttlMillis)) {
            byCharacter.remove(ticket.characterId(), ticket);
            return Optional.empty();
        }
        if (!ticket.matches(request)) {
            return Optional.empty();
        }
        if (!ticket.acceptsSource(sourceIp)) {
            System.out.printf("[Migration] refused MIGRATE_IN of character %d from %s: the ticket was issued to %s%n",
                    request.characterId(), sourceIp, ticket.originIp());
            return Optional.empty();
        }
        return byCharacter.remove(ticket.characterId(), ticket) ? Optional.of(ticket) : Optional.empty();
    }

    /** The character's unexpired ticket without claiming it. */
    public Optional<MigrationTicket> peek(int characterId) {
        MigrationTicket ticket = byCharacter.get(characterId);
        return ticket == null || ticket.expiredAt(now(), ttlMillis) ? Optional.empty() : Optional.of(ticket);
    }

    public void discard(int characterId) {
        byCharacter.remove(characterId);
    }

    /** Removes the tickets {@code origin} asked for (its socket closed outside a migration); returns how many. */
    public int discardIssuedBy(Object origin) {
        if (origin == null) {
            return 0;
        }
        int[] n = {0};
        byCharacter.values().removeIf(t -> {
            boolean mine = t.origin() == origin;
            if (mine) {
                n[0]++;
            }
            return mine;
        });
        return n[0];
    }

    /** Removes every ticket {@code filter} accepts. */
    public void discardIf(Predicate<MigrationTicket> filter) {
        Objects.requireNonNull(filter, "filter");
        byCharacter.values().removeIf(filter);
    }

    /** Drops expired tickets; returns how many remain. */
    public int purgeExpired() {
        long now = now();
        byCharacter.values().removeIf(t -> t.expiredAt(now, ttlMillis));
        return byCharacter.size();
    }

    /** Tickets held, expired ones included until the next purge. */
    public int size() {
        return byCharacter.size();
    }
}
