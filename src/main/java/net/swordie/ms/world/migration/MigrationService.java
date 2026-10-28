package net.swordie.ms.world.migration;

import net.swordie.ms.Server;
import net.swordie.ms.client.Account;
import net.swordie.ms.client.Client;
import net.swordie.ms.client.User;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.world.World;

import java.util.Objects;
import java.util.Optional;

/**
 * Enters a channel for a decoded MIGRATE_IN (the protocol-independent part of the former
 * {@code MigrationHandler.handleMigrateIn}): claims the {@link MigrationTicket}, resolves user, account and
 * character, binds them to the entering client and hands over to the profile's {@link FieldEntryPolicy}.
 * Decoding is a {@link MigrateInDecoder}'s job, so the same service serves every protocol.
 */
public final class MigrationService {
    private final MigrationRegistry registry;

    public MigrationService(MigrationRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    /** The service over the server's registry. */
    public static MigrationService standard() {
        return new MigrationService(Server.get().getMigrations());
    }

    public MigrationRegistry registry() {
        return registry;
    }

    /**
     * @return the entered character, or empty when the request was refused ({@code policy} already sent the
     * client away)
     */
    public Optional<Char> enter(Client c, MigrateInRequest request, FieldEntryPolicy policy) {
        Objects.requireNonNull(c, "client");
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(policy, "policy");
        int charId = request.characterId();
        Optional<MigrationTicket> claimed = registry.claim(request, c.getIP()); // 282 tickets: same source IP only
        if (claimed.isEmpty()) {
            policy.reject(c, String.format("Client %s 在遷移過程中出錯：系統沒有對應數據（%s）。", c.getIP(), request));
            return Optional.empty();
        }
        MigrationTicket ticket = claimed.get();
        // step 6: the character's save gate for the whole load (a logout save of its previous session may still be
        // writing its rows; saves no longer hold ECONOMY_MONITOR). Re-entrant: Tms282FieldEntry takes it again.
        return net.swordie.ms.client.character.tms282.EconomyLocks.withGate(charId,
                () -> enterClaimed(c, request, policy, charId, ticket));
    }

    private Optional<Char> enterClaimed(Client c, MigrateInRequest request, FieldEntryPolicy policy, int charId,
                                        MigrationTicket ticket) {
        Client oldClient = ticket.origin();
        if (oldClient != null) {
            c.setOldChannel(oldClient.getOldChannel());
        }
        User user = ticket.resolveUser();
        if (user == null) {
            policy.reject(c, String.format("Client %s 在遷移過程中出錯：user 爲空。", c.getIP()));
            return Optional.empty();
        }
        int worldId = policy.worldIdOf(ticket);
        World world = Server.get().getWorld();
        Account acc = ticket.account() != null ? ticket.account() : user.getAccountByWorldId(worldId);
        if (acc == null) {
            policy.reject(c, String.format("Client %s 的賬號 %s 在遷移過程中出錯：account 爲空。", c.getIP(), user.getName()));
            return Optional.empty();
        }
        c.setAccount(acc);
        byte channel = ticket.channelId();
        c.setChannel(channel);
        c.setWorldId((byte) worldId);
        c.setChannelInstance(world == null ? null : world.getChannelById(channel));
        boolean isFirstLogin = false;
        Char chr = oldClient == null ? null : oldClient.getChr();
        if (chr == null || chr.getId() != charId) {
            isFirstLogin = true;
            chr = acc.getCharById(charId);
            if (chr == null) {
                chr = Char.getCharDataByID(charId);
                if (chr == null || chr.getAccId() != acc.getId()) {
                    policy.reject(c, String.format("Client %s 的賬號 %s 嘗試進入不存在或不屬於該賬號的角色，角色ID: %d。",
                            c.getIP(), user.getName(), charId));
                    return Optional.empty();
                }
            }
        }
        String problem = policy.validate(chr, acc, ticket);
        if (problem != null) {
            policy.reject(c, String.format("Client %s 的賬號 %s 角色 %d 不能進入：%s", c.getIP(), user.getName(), charId, problem));
            return Optional.empty();
        }
        bind(c, chr, user, acc, policy.recordsLastCharacter());
        policy.enter(new MigrationContext(c, chr, user, acc, ticket, request, isFirstLogin));
        return Optional.of(chr);
    }

    /** User, character, account and client refer to each other; the client is IN_FIELD. */
    private static void bind(Client c, Char chr, User user, Account acc, boolean recordLastCharacter) {
        // User:
        user.setCurrentChr(chr);
        user.setCurrentAcc(acc);
        user.setClientState(Client.IN_FIELD);
        user.updateUserClientStateToSQL();
        if (recordLastCharacter) {
            user.setLastCharID(chr.getId());
            user.updateUserLastCharIdToSQL();
        }

        // Char:
        chr.setUser(user);
        chr.setClient(c);
        chr.setAccount(acc);

        // Account:
        acc.setCurrentChr(chr);
        acc.setUser(user);

        // Client:
        c.setCurrentState(Client.IN_FIELD);
        c.setNextState(Client.NONE);
        c.setChr(chr);
        c.setUser(user);
    }
}
