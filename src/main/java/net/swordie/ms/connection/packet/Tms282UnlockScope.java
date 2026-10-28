package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.handlers.header.OutHeader;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * The 282 answer to the 265 {@code EXCL_REQUEST} ({@code Char.dispose()}, the "enable actions" of every 265 flow),
 * which has no 282 opcode: the 282 client clears its exclusive-request flag only from a packet that carries it
 * (LP009D stat change, LP008F inventory operation, byte 2 = 1). Without a translation every 265 flow that ends with
 * {@code chr.dispose()} (NPC without a script, script end, portal/reactor/item scripts, refusals) leaves the client
 * ignoring clicks; translating it blindly doubles the unlock of the 282 routes that already reply with the flag set.
 * <p>
 * So the translation is scoped to the request being handled (one thread, see {@link #run}):
 * <ul>
 *     <li>inside a 282 route: EXCL_REQUEST only marks an unlock as pending; when the route ends without having sent
 *     an unlock-carrying packet itself ({@link #observe}), one LP009D unlock ({@link Tms282StatPackets#unlock()}) is
 *     sent. The 265 packet itself is declined and dropped by the guard as before;</li>
 *     <li>outside a route (script threads, timers): EXCL_REQUEST becomes the LP009D unlock at once.</li>
 * </ul>
 * Registered as a {@link Tms282LegacyPackets.Contributor} (EXCL_REQUEST rule) in {@code Tms282FieldEncoders}.
 */
public final class Tms282UnlockScope implements Tms282LegacyPackets.Contributor {
    public static final Tms282UnlockScope INSTANCE = new Tms282UnlockScope();

    private static final int LP_INVENTORY_OPERATION = 0x008F;
    private static final int LP_STAT_CHANGED = 0x009D;
    private static final ThreadLocal<Scope> CURRENT = new ThreadLocal<>();

    /** The body of one request; may throw like {@code Tms282Command.execute}. */
    @FunctionalInterface
    public interface Body {
        void run() throws Exception;
    }

    private static final class Scope {
        boolean unlockSent;
        boolean unlockPending;
    }

    private Tms282UnlockScope() {
    }

    @Override
    public void contribute(Tms282LegacyPackets.Builder builder) {
        builder.on(OutHeader.EXCL_REQUEST, p -> exclRequest());
    }

    /**
     * Runs {@code body} as one request scope; afterwards sends one LP009D unlock through {@code sink} when a 265 flow
     * asked for it and nothing sent by the request carried the unlock flag. Nested calls join the outer scope.
     */
    public static void run(Consumer<OutPacket> sink, Body body) throws Exception {
        Objects.requireNonNull(sink, "sink");
        if (CURRENT.get() != null) {
            body.run();
            return;
        }
        Scope scope = new Scope();
        CURRENT.set(scope);
        try {
            body.run();
        } finally {
            CURRENT.remove();
            if (scope.unlockPending && !scope.unlockSent) {
                sink.accept(Tms282StatPackets.unlock());
            }
        }
    }

    /**
     * Whether the current thread is handling a 282 route. Codecs whose 265 packets the 282 routes replace with their
     * own replies (inventory diffs) translate only outside a route, i.e. on script threads and timers.
     */
    public static boolean inRoute() {
        return CURRENT.get() != null;
    }

    /** Notes a packet about to be written (after translation); an unlock-carrying one settles the scope. */
    public static void observe(OutPacket sent) {
        Scope scope = CURRENT.get();
        if (scope != null && sent != null && carriesUnlock(sent.getData())) {
            scope.unlockSent = true;
        }
    }

    /** LP009D / LP008F with the exclRequest byte set. */
    static boolean carriesUnlock(byte[] data) {
        if (data == null || data.length < 3) {
            return false;
        }
        int op = (data[0] & 0xFF) | (data[1] & 0xFF) << 8;
        return (op == LP_STAT_CHANGED || op == LP_INVENTORY_OPERATION) && data[2] == 1;
    }

    static Optional<OutPacket> exclRequest() {
        Scope scope = CURRENT.get();
        if (scope != null) {
            scope.unlockPending = true;
            return Optional.empty();
        }
        return Optional.of(Tms282StatPackets.unlock());
    }
}
