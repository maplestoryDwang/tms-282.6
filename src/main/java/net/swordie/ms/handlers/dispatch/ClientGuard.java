package net.swordie.ms.handlers.dispatch;

import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;

import java.util.Objects;
import java.util.function.BiPredicate;

/**
 * Precondition of a packet handler (specification pattern). Combine with {@link #and}, {@link #or},
 * {@link #negate}; apply with {@link HandlerDecorators#require} or {@link HandlerDecorators#when}.
 */
@FunctionalInterface
public interface ClientGuard {
    boolean permits(Client c, Char chr);

    /** Short text for logs. */
    default String description() {
        return "guard";
    }

    static ClientGuard named(String description, BiPredicate<Client, Char> test) {
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(test, "test");
        return new ClientGuard() {
            @Override
            public boolean permits(Client c, Char chr) {
                return test.test(c, chr);
            }

            @Override
            public String description() {
                return description;
            }
        };
    }

    static ClientGuard always() {
        return named("always", (c, chr) -> true);
    }

    /** A client object exists (every socket has one once the handshake ran). */
    static ClientGuard hasClient() {
        return named("hasClient", (c, chr) -> c != null);
    }

    static ClientGuard hasCharacter() {
        return named("hasCharacter", (c, chr) -> chr != null);
    }

    static ClientGuard noCharacter() {
        return named("noCharacter", (c, chr) -> chr == null);
    }

    static ClientGuard authorized() {
        return named("authorized", (c, chr) -> c != null && c.isAuthorized());
    }

    static ClientGuard notExiting() {
        return named("notExiting", (c, chr) -> c != null && !c.isExiting());
    }

    default ClientGuard and(ClientGuard other) {
        ClientGuard self = this;
        return named(description() + "&" + other.description(),
                (c, chr) -> self.permits(c, chr) && other.permits(c, chr));
    }

    default ClientGuard or(ClientGuard other) {
        ClientGuard self = this;
        return named(description() + "|" + other.description(),
                (c, chr) -> self.permits(c, chr) || other.permits(c, chr));
    }

    default ClientGuard negate() {
        ClientGuard self = this;
        return named("!" + description(), (c, chr) -> !self.permits(c, chr));
    }
}
