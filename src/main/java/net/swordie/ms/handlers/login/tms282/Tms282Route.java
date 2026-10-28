package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.connection.netty.PacketHandler;
import net.swordie.ms.connection.protocol.SessionKind;
import net.swordie.ms.handlers.header.InHeader;

import java.util.Objects;

/**
 * One registered TMS 282.3 client opcode on one socket kind: the composed handler plus the metadata checks
 * compare with the research probe (guard mode and guard name).
 *
 * @param op          wire opcode
 * @param header      header used when the opcode table has no constant for {@code op}
 * @param socket      socket kind the handler serves (the probe had separate login and channel maps)
 * @param mode        how the guard applies
 * @param guard       guard description ({@code ClientGuard#description}), "-" when none
 * @param action      short description for logs and checks
 * @param handler     guard, decorators and command, composed
 */
public record Tms282Route(int op, InHeader header, SessionKind socket, GuardMode mode, String guard, String action,
                          PacketHandler handler) {
    /** Probe guard semantics. */
    public enum GuardMode {
        /** No precondition. */
        NONE,
        /** Unmet guard ends the session (probe {@code require}). */
        REQUIRE,
        /** Unmet guard drops the packet with a log line (probe {@code when}). */
        WHEN
    }

    public Tms282Route {
        Objects.requireNonNull(header, "header");
        Objects.requireNonNull(socket, "socket");
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(guard, "guard");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(handler, "handler");
        if (op < 0 || op > 0xFFFF) {
            throw new IllegalArgumentException("opcode out of range: " + op);
        }
    }

    @Override
    public String toString() {
        return String.format("CP%04X[%s %s %s: %s]", op, socket, mode, guard, action);
    }
}
