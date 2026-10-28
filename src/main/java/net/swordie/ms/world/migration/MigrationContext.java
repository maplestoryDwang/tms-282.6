package net.swordie.ms.world.migration;

import net.swordie.ms.client.Account;
import net.swordie.ms.client.Client;
import net.swordie.ms.client.User;
import net.swordie.ms.client.character.Char;

/**
 * Everything {@link MigrationService} resolved for one MIGRATE_IN, handed to the profile's
 * {@link FieldEntryPolicy#enter}.
 *
 * @param client     the entering channel socket's client (already bound to user, account and character)
 * @param chr        the entering character
 * @param firstLogin true when the character object was loaded for this entry (not reused from a channel change)
 */
public record MigrationContext(Client client, Char chr, User user, Account account, MigrationTicket ticket,
                               MigrateInRequest request, boolean firstLogin) {
}
