package net.swordie.ms.world.migration;

import net.swordie.ms.client.Account;
import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;

/**
 * The protocol-specific part of entering a channel (strategy used by {@link MigrationService}): which world the
 * session belongs to, extra ticket checks, how a refused client is sent away and how the first field is shown.
 * {@link Gms265FieldEntry} is the former MigrationHandler behaviour; TMS 282.3 has its own (LP02EF SetField).
 */
public interface FieldEntryPolicy {
    /** World ID the entering client is bound to. */
    int worldIdOf(MigrationTicket ticket);

    /** Extra checks of the resolved character; null when it may enter, else the reason. */
    String validate(Char chr, Account account, MigrationTicket ticket);

    /** Whether entering stores the character as the user's last character (quick login). */
    boolean recordsLastCharacter();

    /** Logs {@code reason} and sends the client away. */
    void reject(Client c, String reason);

    /** Loads the character and shows the first field. The session objects are already bound. */
    void enter(MigrationContext context);
}
