package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.Tms282UnlockScope;
import net.swordie.ms.connection.netty.PacketHandler;
import net.swordie.ms.connection.protocol.SessionKind;
import net.swordie.ms.handlers.dispatch.ClientGuard;
import net.swordie.ms.handlers.dispatch.HandlerDecorators;
import net.swordie.ms.handlers.header.InHeader;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Collects {@link Tms282Route}s (builder pattern). Each route is composed outermost first, in the probe's order:
 * <pre>
 * reportFirst -> endsSpwCancelCheck -> guard (require | when) -> command
 * </pre>
 * A (socket kind, opcode) pair may be registered once.
 */
final class Tms282Routes {
    private final List<Tms282Route> routes = new ArrayList<>();
    private final Tms282ReportHandlers reports;

    Tms282Routes(Tms282ReportHandlers reports) {
        this.reports = Objects.requireNonNull(reports, "reports");
    }

    /** A route of the login socket (the probe's clientHandlers()). */
    RouteBuilder login(int op, InHeader header) {
        return new RouteBuilder(op, header, SessionKind.LOGIN);
    }

    /** A route of the channel socket (the probe's channelHandlers()). */
    RouteBuilder channel(int op, InHeader header) {
        return new RouteBuilder(op, header, SessionKind.CHANNEL);
    }

    List<Tms282Route> build() {
        return List.copyOf(routes);
    }

    private void add(Tms282Route route) {
        for (Tms282Route r : routes) {
            if (r.op() == route.op() && r.socket() == route.socket()) {
                throw new IllegalStateException(String.format("Duplicate %s handler for CP%04X", route.socket(), route.op()));
            }
        }
        routes.add(route);
    }

    /** Adapts a command: whole packet in, body consumed, any exception ends the session. */
    static PacketHandler adapt(Tms282Command command) {
        Objects.requireNonNull(command, "command");
        return (c, chr, in) -> {
            byte[] packet = in.getData();
            consume(in);
            int op = packet.length < 2 ? -1 : (packet[0] & 0xFF) | (packet[1] & 0xFF) << 8;
            Tms282LoginState session = Tms282LoginState.of(c);
            try {
                Tms282UnlockScope.run(session::send, () -> command.execute(session, op, packet));
            } catch (Exception | LinkageError e) {
                session.terminate(String.format("CP%04X %s", op, e));
            }
        };
    }

    static void consume(InPacket in) {
        int unread = in.getUnreadAmount();
        if (unread > 0) {
            in.decodeArr(unread);
        }
    }

    final class RouteBuilder {
        private final int op;
        private final InHeader header;
        private final SessionKind socket;
        private Tms282Route.GuardMode mode = Tms282Route.GuardMode.NONE;
        private ClientGuard guard;
        private String failure;
        private boolean reportFirst;
        private boolean endsSpwCancelCheck;

        private RouteBuilder(int op, InHeader header, SessionKind socket) {
            this.op = op;
            this.header = Objects.requireNonNull(header, "header");
            this.socket = socket;
        }

        /** Unmet guard ends the session with {@code failure} (probe require). */
        RouteBuilder require(ClientGuard guard, String failure) {
            this.mode = Tms282Route.GuardMode.REQUIRE;
            this.guard = Objects.requireNonNull(guard, "guard");
            this.failure = Objects.requireNonNull(failure, "failure");
            return this;
        }

        /** Unmet guard drops the packet with a log line (probe when). */
        RouteBuilder when(ClientGuard guard) {
            this.mode = Tms282Route.GuardMode.WHEN;
            this.guard = Objects.requireNonNull(guard, "guard");
            return this;
        }

        /** Records the packet as a client report before anything else (probe reportThen). */
        RouteBuilder reportFirst() {
            this.reportFirst = true;
            return this;
        }

        /** CP0079 / CP007C: end the pending SPW-cancel check before the guard (probe endsSpwCancelCheck). */
        RouteBuilder endsSpwCancelCheck() {
            this.endsSpwCancelCheck = true;
            return this;
        }

        /** Composes the route around {@code command} and adds it. */
        void handle(String action, Tms282Command command) {
            handleWith(action, adapt(command));
        }

        /** As {@link #handle}, around a ready handler (shared report handlers). */
        void handleWith(String action, PacketHandler base) {
            PacketHandler h = base;
            switch (mode) {
                case REQUIRE -> h = HandlerDecorators.require(guard, h, Tms282Guards.disconnect(failure));
                case WHEN -> h = HandlerDecorators.when(guard, h, Tms282Guards.logRejected());
                case NONE -> {
                }
            }
            if (endsSpwCancelCheck) {
                PacketHandler inner = h;
                h = (c, chr, in) -> {
                    Tms282LoginState.of(c).cancelSpwCancelCheck();
                    inner.handle(c, chr, in);
                };
            }
            if (reportFirst) {
                h = HandlerDecorators.reportThen((c, chr, packet) -> reports.record(Tms282LoginState.of(c), packet), h);
            }
            PacketHandler guardedHandler=h;
            h=(c,chr,in)->{
                var actor=chr!=null?chr:Tms282LoginState.of(c).fieldCharacter();
                if(socket==net.swordie.ms.connection.protocol.SessionKind.CHANNEL && actor!=null
                        && net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime.economyEnabled()) {
                    // step 6: save gate(actor) -> ECONOMY_MONITOR -> actor (EconomyLocks.packet): while this character
                    // is being saved its packets wait on the gate, not inside the global monitor
                    net.swordie.ms.client.character.tms282.EconomyLocks.packet(actor, () -> {
                        if(!net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime.economicBlocked(actor.getId()))
                            guardedHandler.handle(c,chr,in);
                        return null;
                    });
                } else guardedHandler.handle(c,chr,in);
            };
            add(new Tms282Route(op, header, socket, mode, guard == null ? "-" : guard.description(), action, h));
        }
    }
}
