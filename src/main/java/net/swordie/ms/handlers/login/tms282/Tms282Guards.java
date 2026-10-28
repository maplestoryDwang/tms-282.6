package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.Client;
import net.swordie.ms.connection.protocol.SessionKind;
import net.swordie.ms.handlers.dispatch.ClientGuard;
import net.swordie.ms.handlers.dispatch.HandlerDecorators;

import java.util.function.Predicate;

/**
 * Session preconditions of the TMS 282.3 overlay, as {@link ClientGuard}s over {@link Tms282LoginState} (the
 * research probe's Guard constants, same names and composition).
 * <p>
 * Probe semantics are kept: a {@code require} guard that fails ends the session ({@link #disconnect}), a
 * {@code when} guard drops the packet with one log line ({@link #logRejected}).
 */
public final class Tms282Guards {
    private Tms282Guards() {
    }

    public static final ClientGuard LOGIN_SOCKET = state("loginSocket", s -> s.kind() == SessionKind.LOGIN);
    public static final ClientGuard CHANNEL_SOCKET = state("channelSocket", s -> s.kind() == SessionKind.CHANNEL);

    public static final ClientGuard AUTHENTICATED = state("AUTHENTICATED", Tms282LoginState::authenticated);
    public static final ClientGuard WORLD_SELECTED = AUTHENTICATED.and(state("worldSelected", Tms282LoginState::worldSelected));
    public static final ClientGuard CHARACTER_LIST_SENT = WORLD_SELECTED.and(state("characterListSent", Tms282LoginState::characterListSent));
    public static final ClientGuard CHARACTER_LIST_VERIFIED = CHARACTER_LIST_SENT.and(state("fileReportVerified", Tms282LoginState::fileReportVerified));
    /**
     * CP007C's original check omits the selected account; kept exactly (characterListSent is only set with it). The
     * client sends CP0078 only in some sessions, so the enter-game steps must not wait for it.
     */
    public static final ClientGuard LIST_SENT = AUTHENTICATED.and(state("characterListSent", Tms282LoginState::characterListSent));
    /** Channel socket: CP007E is accepted once. */
    public static final ClientGuard AWAITING_MIGRATE_IN = state("AWAITING_MIGRATE_IN", s -> s.isChannel() && !s.migrateInSeen());
    /** Channel socket: in-field CPs only after the SetField was sent. */
    public static final ClientGuard IN_FIELD = state("IN_FIELD", s -> s.isChannel() && s.fieldCharacter() != null
            && s.client().getChr() == s.fieldCharacter() && s.fieldCharacter().getClient() == s.client()
            && !net.swordie.ms.client.character.tms282.Tms282VMatrixJournal.isBlocked(s.fieldCharacter().getId()));

    /** Guard over the socket's {@link Tms282LoginState}; a missing client never passes. */
    static ClientGuard state(String name, Predicate<Tms282LoginState> test) {
        return ClientGuard.named(name, (c, chr) -> c != null && test.test(Tms282LoginState.of(c)));
    }

    /** Fatal rejection: logs {@code failure} and closes the socket (probe {@code require}: IOException). */
    public static HandlerDecorators.RejectionListener disconnect(String failure) {
        return (guard, c, op, length) -> {
            HandlerDecorators.RejectionListener.LOG.rejected(guard, c, op, length);
            if (c != null) {
                Tms282LoginState.of(c).terminate(String.format("CP%04X rejected by %s: %s", op, guard.description(), failure));
            }
        };
    }

    /** Non-fatal rejection: one line, the packet is dropped (probe {@code when}: "REJECTED %04X precondition"). */
    public static HandlerDecorators.RejectionListener logRejected() {
        return (guard, c, op, length) -> System.out.printf("[TMS282] REJECTED %04X precondition %s%n", op, guard.description());
    }

    static boolean isChannel(Client c) {
        return c != null && Tms282LoginState.of(c).isChannel();
    }
}
