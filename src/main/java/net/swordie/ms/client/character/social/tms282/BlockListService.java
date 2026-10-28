package net.swordie.ms.client.character.social.tms282;

import net.swordie.ms.Server;
import net.swordie.ms.client.character.Char;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Block list (黑名單) of TMS 282.3, persisted in {@link SocialTables#BLACKLIST_USER} (owner = world account, so every
 * character of the account blocks) and {@link SocialTables#BLACKLIST_GUILD} (owner = character). A blocked entry
 * stores the character id and its account id: a block also covers the blocked player's other characters.
 * <p>
 * The client has no block-list opcode in 282 yet (InHeader.BLACK_LIST / OutHeader.BLACK_LIST are unknown), so the
 * list is managed with the chat commands of {@link SocialChatCommands} and enforced through {@link SocialGuards}.
 * Lists are cached per account (loaded on first use, updated on write).
 */
public final class BlockListService {
    public static final int MAX_ENTRIES = 100;
    private static final BlockListService INSTANCE = new BlockListService();

    public record Entry(int blockCharacterId, int blockAccountId, String name, String memo) {
    }

    public record GuildEntry(int guildId, String name) {
    }

    public enum AddResult { ADDED, ALREADY, SELF, NOT_FOUND, FULL, FAILED }

    private final Map<Integer, Map<Integer, Entry>> byAccount = new ConcurrentHashMap<>();
    private final Map<Integer, Map<Integer, GuildEntry>> guildsByCharacter = new ConcurrentHashMap<>();

    private BlockListService() {
    }

    public static BlockListService get() {
        return INSTANCE;
    }

    // ---- queries ---------------------------------------------------------------------------------------------

    public List<Entry> list(Char owner) {
        return List.copyOf(users(owner.getAccId()).values());
    }

    public List<GuildEntry> guilds(Char owner) {
        return List.copyOf(guilds(owner.getId()).values());
    }

    /** True when {@code receiver}'s account blocked {@code sender} (its character or its account). */
    public boolean blocks(Char receiver, Char sender) {
        if (receiver == null || sender == null || receiver.getAccId() == sender.getAccId()) {
            return false;
        }
        Map<Integer, Entry> list = users(receiver.getAccId());
        if (list.isEmpty()) {
            return false;
        }
        if (list.containsKey(sender.getId())) {
            return true;
        }
        int acc = sender.getAccId();
        return acc > 0 && list.values().stream().anyMatch(e -> e.blockAccountId() == acc);
    }

    /** True when {@code receiver} blocked the guild {@code guildId}. */
    public boolean blocksGuild(Char receiver, int guildId) {
        return receiver != null && guildId > 0 && guilds(receiver.getId()).containsKey(guildId);
    }

    // ---- changes ---------------------------------------------------------------------------------------------

    public AddResult add(Char owner, String targetName, String memo) {
        Optional<int[]> target = lookup(targetName);
        if (target.isEmpty()) {
            return AddResult.NOT_FOUND;
        }
        int charId = target.get()[0];
        int accId = target.get()[1];
        if (charId == owner.getId() || accId == owner.getAccId()) {
            return AddResult.SELF;
        }
        Map<Integer, Entry> list = users(owner.getAccId());
        if (list.containsKey(charId)) {
            return AddResult.ALREADY;
        }
        if (list.size() >= MAX_ENTRIES) {
            return AddResult.FULL;
        }
        String name = nameOf(charId, targetName);
        try (Connection c = SocialTables.connection(); PreparedStatement p = c.prepareStatement(
                "REPLACE INTO " + SocialTables.BLACKLIST_USER
                        + " (account_id, block_character_id, name, nick_name, block_account_id, memo) VALUES (?,?,?,?,?,?)")) {
            p.setInt(1, owner.getAccId());
            p.setInt(2, charId);
            p.setString(3, name);
            p.setString(4, null);
            p.setInt(5, accId);
            p.setString(6, memo);
            p.executeUpdate();
        } catch (SQLException e) {
            SocialTables.log("block " + targetName + " for account " + owner.getAccId(), e);
            return AddResult.FAILED;
        }
        list.put(charId, new Entry(charId, accId, name, memo));
        return AddResult.ADDED;
    }

    /** @return the removed entry, or empty when {@code targetName} is not on the list */
    public Optional<Entry> remove(Char owner, String targetName) {
        Map<Integer, Entry> list = users(owner.getAccId());
        Entry e = list.values().stream().filter(x -> x.name().equalsIgnoreCase(targetName)).findFirst().orElse(null);
        if (e == null) {
            return Optional.empty();
        }
        try (Connection c = SocialTables.connection(); PreparedStatement p = c.prepareStatement(
                "DELETE FROM " + SocialTables.BLACKLIST_USER + " WHERE account_id=? AND block_character_id=?")) {
            p.setInt(1, owner.getAccId());
            p.setInt(2, e.blockCharacterId());
            p.executeUpdate();
        } catch (SQLException ex) {
            SocialTables.log("unblock " + targetName + " for account " + owner.getAccId(), ex);
            return Optional.empty();
        }
        list.remove(e.blockCharacterId());
        return Optional.of(e);
    }

    public boolean addGuild(Char owner, int guildId, String guildName) {
        try (Connection c = SocialTables.connection(); PreparedStatement p = c.prepareStatement(
                "REPLACE INTO " + SocialTables.BLACKLIST_GUILD + " (character_id, guild_id, name) VALUES (?,?,?)")) {
            p.setInt(1, owner.getId());
            p.setInt(2, guildId);
            p.setString(3, guildName == null ? "" : guildName);
            p.executeUpdate();
        } catch (SQLException e) {
            SocialTables.log("block guild " + guildId + " for " + owner.getId(), e);
            return false;
        }
        guilds(owner.getId()).put(guildId, new GuildEntry(guildId, guildName == null ? "" : guildName));
        return true;
    }

    public boolean removeGuild(Char owner, int guildId) {
        try (Connection c = SocialTables.connection(); PreparedStatement p = c.prepareStatement(
                "DELETE FROM " + SocialTables.BLACKLIST_GUILD + " WHERE character_id=? AND guild_id=?")) {
            p.setInt(1, owner.getId());
            p.setInt(2, guildId);
            p.executeUpdate();
        } catch (SQLException e) {
            SocialTables.log("unblock guild " + guildId + " for " + owner.getId(), e);
            return false;
        }
        return guilds(owner.getId()).remove(guildId) != null;
    }

    /** Drops the cache of an account whose characters are all offline (optional; the lists are small). */
    public void evict(int accountId) {
        byAccount.remove(accountId);
    }

    // ---- loading ---------------------------------------------------------------------------------------------

    private Map<Integer, Entry> users(int accountId) {
        return byAccount.computeIfAbsent(accountId, this::loadUsers);
    }

    private Map<Integer, GuildEntry> guilds(int characterId) {
        return guildsByCharacter.computeIfAbsent(characterId, this::loadGuilds);
    }

    private Map<Integer, Entry> loadUsers(int accountId) {
        Map<Integer, Entry> out = Collections.synchronizedMap(new LinkedHashMap<>());
        try (Connection c = SocialTables.connection(); PreparedStatement p = c.prepareStatement(
                "SELECT block_character_id, block_account_id, name, memo FROM " + SocialTables.BLACKLIST_USER
                        + " WHERE account_id=? ORDER BY created_at")) {
            p.setInt(1, accountId);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    out.put(r.getInt(1), new Entry(r.getInt(1), r.getInt(2), r.getString(3), r.getString(4)));
                }
            }
        } catch (SQLException e) {
            SocialTables.log("load block list of account " + accountId, e);
        }
        return out;
    }

    private Map<Integer, GuildEntry> loadGuilds(int characterId) {
        Map<Integer, GuildEntry> out = Collections.synchronizedMap(new LinkedHashMap<>());
        try (Connection c = SocialTables.connection(); PreparedStatement p = c.prepareStatement(
                "SELECT guild_id, name FROM " + SocialTables.BLACKLIST_GUILD + " WHERE character_id=? ORDER BY created_at")) {
            p.setInt(1, characterId);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    out.put(r.getInt(1), new GuildEntry(r.getInt(1), r.getString(2)));
                }
            }
        } catch (SQLException e) {
            SocialTables.log("load guild block list of " + characterId, e);
        }
        return out;
    }

    /** [characterId, accountId] of the character called {@code name} (online first, then the database). */
    static Optional<int[]> lookup(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        for (Char c : new ArrayList<>(Server.get().getChars().values())) {
            if (c.getName() != null && c.getName().equalsIgnoreCase(name)) {
                return Optional.of(new int[]{c.getId(), c.getAccId()});
            }
        }
        try (Connection c = SocialTables.connection(); PreparedStatement p = c.prepareStatement(
                "SELECT cs.characterid, ch.accid FROM characterstats cs JOIN characters ch ON ch.id = cs.characterid "
                        + "WHERE cs.name = ? LIMIT 1")) {
            p.setString(1, name);
            try (ResultSet r = p.executeQuery()) {
                return r.next() ? Optional.of(new int[]{r.getInt(1), r.getInt(2)}) : Optional.empty();
            }
        } catch (SQLException e) {
            SocialTables.log("look up character " + name, e);
            return Optional.empty();
        }
    }

    private static String nameOf(int characterId, String fallback) {
        Char online = Server.get().getChars().get(characterId);
        return online != null && online.getName() != null ? online.getName() : fallback;
    }
}
