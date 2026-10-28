package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.Account;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.connection.packet.Tms282CharacterOrderRequest;
import net.swordie.ms.handlers.header.InHeader;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Native character-management Apply: persist the existing characters.orderid field, without a new schema. */
final class Tms282CharacterOrderHandlers {
    void register(Tms282Routes routes) {
        routes.login(Tms282CharacterOrderRequest.OPCODE, InHeader.CHANGE_CHARACTER_SLOT)
                .when(Tms282Guards.WORLD_SELECTED).handle("save character order (no reply)", this::save);
        routes.channel(Tms282CharacterOrderRequest.OPCODE, InHeader.CHANGE_CHARACTER_SLOT)
                .when(Tms282Guards.IN_FIELD).handle("save character order (no reply)", this::save);
    }

    private void save(Tms282LoginState session, int op, byte[] packet) throws SQLException {
        Account account = session.isChannel() ? session.client().getAccount() : session.selectedAccount();
        if (account == null) {
            System.out.println("[TMS282] REJECTED CP00C7: missing world account");
            return;
        }
        // Login and channel can briefly share this Account while migrating.
        synchronized (account) {
            saveOrder(session, account, packet);
        }
    }

    private void saveOrder(Tms282LoginState session, Account account, byte[] packet) throws SQLException {
        var user = session.isChannel() ? session.client().getUser() : session.user();
        List<Char> ordered;
        Tms282CharacterOrderRequest request;
        try {
            request = Tms282CharacterOrderRequest.parse(packet);
            ordered = order(request, user == null ? -1 : user.getId(), account);
        } catch (IllegalArgumentException e) {
            System.out.println("[TMS282] REJECTED CP00C7: " + e.getMessage());
            return;
        }
        if (request.characterIds().length == 0 || ordered.isEmpty()) return;
        // One statement also supports installations where characters still uses MyISAM.
        String sql = "UPDATE characters SET orderid = CASE id" + " WHEN ? THEN ?".repeat(ordered.size())
                + " ELSE orderid END WHERE accid = ?";
        try (var connection = DatabaseManager.getConnection(); var statement = connection.prepareStatement(sql)) {
            int parameter = 1;
            for (int i = 0; i < ordered.size(); i++) {
                statement.setInt(parameter++, ordered.get(i).getId());
                statement.setInt(parameter++, i);
            }
            statement.setInt(parameter, account.getId());
            statement.executeUpdate();
        }
        for (int i = 0; i < ordered.size(); i++) ordered.get(i).setOrderId(i);
        System.out.printf("[TMS282] CP00C7 saved order user=%d world=%d characters=%d%n",
                user.getId(), account.getWorldId(), ordered.size());
    }

    /** Validate every requested id before saving; hidden/unlisted characters retain their relative order at the end. */
    static List<Char> order(Tms282CharacterOrderRequest request, int userId, Account account) {
        if (request.userId() != userId || userId <= 0 || account == null || account.getUser() == null
                || account.getUser().getId() != userId) throw new IllegalArgumentException("User/world account mismatch");
        List<Char> remaining = new ArrayList<>(account.getCharacters());
        remaining.sort(Comparator.comparingInt(Char::getOrderId).thenComparingInt(Char::getId));
        List<Char> ordered = new ArrayList<>();
        for (int id : request.characterIds()) {
            Char character = remaining.stream().filter(c -> c.getId() == id && c.getAccId() == account.getId())
                    .findFirst().orElseThrow(() -> new IllegalArgumentException("Character outside selected world account"));
            remaining.remove(character);
            ordered.add(character);
        }
        ordered.addAll(remaining);
        return ordered;
    }
}
