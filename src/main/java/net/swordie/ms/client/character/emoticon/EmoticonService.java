package net.swordie.ms.client.character.emoticon;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.console.config.ServerRuntimeConfig;
import net.swordie.ms.util.FileTime;

import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Chat emoticons owned by characters: grant (single / whole group, permanent or timed), list, favorite bar slots,
 * text commands. Write-through persistence in {@link EmoticonTables}; per-character cache loaded lazily on first use
 * and dropped by {@link #onLogout}. Expired emoticons are purged on load and before every read.
 * <p>
 * Packets: the 282 opcodes of the emoticon UI (265 CHAT_EMOTICON = UnkValue, USER_EMOTICON_EFFECT 1026 /
 * UNVERIFIED_282, inbound UserSetActiveEmoticonItem) are not known, so nothing is sent live. Owned emoticons reach
 * the client only through the CharacterData block ({@link #encodeCharacterData}), whose non-empty layout is
 * unverified for 282 and therefore behind {@code emoticon.encode.enabled} (default false: the exact bytes the
 * server always sent, int 0 / int 0 / short 0). Newly granted emoticons show after the next CharacterData
 * (re-login / channel change) once that flag is on.
 * <p>
 * Database failures are logged; they never break login, encode or the calling script (grant then returns FAILED).
 */
public final class EmoticonService {
    /** FavoriteEmoticonBar/key has 9 slot canvases in 265 UI.wz/ChatEmoticon.img. */
    public static final int FAVORITE_SLOTS = 9;
    public static final int MAX_COMMANDS = 20;
    public static final int MAX_COMMAND_LENGTH = 32;

    public static final String ENCODE_FLAG = "emoticon.encode.enabled";
    public static final String ALLOW_UNLISTED_FLAG = "emoticon.allowUnlisted";

    private static final EmoticonService INSTANCE = new EmoticonService();

    /** One owned emoticon. {@code expireAt} null = permanent; {@code favoritePos} -1 = not on the favorite bar. */
    public record Owned(int emoticonId, int groupId, Instant expireAt, int favoritePos) {
        public boolean permanent() {
            return expireAt == null;
        }

        public boolean expired(Instant now) {
            return expireAt != null && !expireAt.isAfter(now);
        }

        Owned withFavorite(int pos) {
            return new Owned(emoticonId, groupId, expireAt, pos);
        }

        Owned withExpire(Instant expire) {
            return new Owned(emoticonId, groupId, expire, favoritePos);
        }
    }

    /** A chat text command: typing {@code command} shows {@code emoticonId}. */
    public record Command(int index, int emoticonId, String command) {
    }

    public enum GrantResult {
        GRANTED, EXTENDED, ALREADY_OWNED, UNKNOWN, FAILED;

        /** True when the character's collection changed (the calling item may be consumed). */
        public boolean changed() {
            return this == GRANTED || this == EXTENDED;
        }
    }

    /** Cached state of one character. Guarded by its own monitor. */
    private static final class State {
        final Map<Integer, Owned> owned = new TreeMap<>();
        final Map<Integer, Command> commands = new TreeMap<>();
    }

    private final Map<Integer, State> states = new ConcurrentHashMap<>();

    private EmoticonService() {
    }

    public static EmoticonService get() {
        return INSTANCE;
    }

    static boolean flag(String key, boolean fallback) {
        String raw = System.getProperty(key);
        if (raw == null || raw.isBlank()) {
            raw = ServerRuntimeConfig.stringValue(key);
        }
        return raw == null || raw.isBlank() ? fallback : Boolean.parseBoolean(raw.trim());
    }

    // ---- lifecycle -------------------------------------------------------------------------------------------

    /** Drops the cache of a character leaving the server (the database keeps everything). */
    public void onLogout(Char chr) {
        if (chr != null) {
            states.remove(chr.getId());
        }
    }

    /** Forces a reload from the database on next use (e.g. after an offline edit through the console). */
    public void invalidate(int characterId) {
        states.remove(characterId);
    }

    private State state(int characterId) {
        return states.computeIfAbsent(characterId, this::load);
    }

    private State load(int characterId) {
        State s = new State();
        try {
            for (Owned o : EmoticonStore.loadOwned(characterId)) {
                s.owned.put(o.emoticonId(), o);
            }
            for (Command c : EmoticonStore.loadCommands(characterId)) {
                s.commands.put(c.index(), c);
            }
        } catch (SQLException e) {
            EmoticonTables.log("load emoticons of " + characterId, e);
        }
        purgeExpired(characterId, s);
        return s;
    }

    /** Removes expired emoticons, their favorite slots and the commands bound to them. Caller holds the lock or owns s. */
    private void purgeExpired(int characterId, State s) {
        Instant now = Instant.now();
        List<Integer> gone = new ArrayList<>();
        s.owned.values().removeIf(o -> {
            if (o.expired(now)) {
                gone.add(o.emoticonId());
                return true;
            }
            return false;
        });
        if (gone.isEmpty()) {
            return;
        }
        List<Integer> cmdGone = new ArrayList<>();
        s.commands.values().removeIf(c -> {
            if (gone.contains(c.emoticonId())) {
                cmdGone.add(c.index());
                return true;
            }
            return false;
        });
        try {
            EmoticonStore.deleteOwned(characterId, gone);
            EmoticonStore.deleteCommands(characterId, cmdGone);
        } catch (SQLException e) {
            EmoticonTables.log("purge expired emoticons of " + characterId, e);
        }
    }

    // ---- grant -----------------------------------------------------------------------------------------------

    /** Permanent grant. */
    public GrantResult grant(Char chr, int emoticonId) {
        return grant(chr, emoticonId, 0);
    }

    /**
     * Grants one emoticon. {@code durationMillis} &lt;= 0 = permanent. Owning it already: a permanent grant upgrades
     * a timed one, a timed grant extends a timed one (from its current expiry), otherwise ALREADY_OWNED.
     * Unknown ids: must be well formed ({@code 1000xxxx..1999xxxx}); ids of groups the catalog lists must be
     * listed; ids of unlisted groups are accepted while {@code emoticon.allowUnlisted} (default true), because the
     * 282 groups (1000..1007, ...) are missing from the 265 UI.wz dump.
     */
    public GrantResult grant(Char chr, int emoticonId, long durationMillis) {
        if (chr == null || !acceptable(emoticonId)) {
            return GrantResult.UNKNOWN;
        }
        State s = state(chr.getId());
        synchronized (s) {
            purgeExpired(chr.getId(), s);
            Instant now = Instant.now();
            Owned cur = s.owned.get(emoticonId);
            Owned next;
            GrantResult result;
            if (cur == null) {
                next = new Owned(emoticonId, EmoticonCatalog.groupOf(emoticonId),
                        durationMillis > 0 ? now.plusMillis(durationMillis) : null, -1);
                result = GrantResult.GRANTED;
            } else if (cur.permanent()) {
                return GrantResult.ALREADY_OWNED;
            } else {
                next = cur.withExpire(durationMillis > 0 ? cur.expireAt().plusMillis(durationMillis) : null);
                result = GrantResult.EXTENDED;
            }
            try {
                EmoticonStore.upsertOwned(chr.getId(), next);
            } catch (SQLException e) {
                EmoticonTables.log("grant emoticon " + emoticonId + " to " + chr.getId(), e);
                return GrantResult.FAILED;
            }
            s.owned.put(emoticonId, next);
            return result;
        }
    }

    /**
     * Grants every catalogued emoticon of a group (permanent).
     *
     * @return number of newly granted / upgraded emoticons; 0 when all were owned; -1 when the group is unknown or
     * the database failed before anything was granted
     */
    public int grantGroup(Char chr, int groupId) {
        List<Integer> ids = EmoticonCatalog.group(groupId);
        if (chr == null || ids.isEmpty()) {
            return -1;
        }
        int changed = 0;
        for (int id : ids) {
            GrantResult r = grant(chr, id, 0);
            if (r == GrantResult.FAILED && changed == 0) {
                return -1;
            }
            if (r.changed()) {
                changed++;
            }
        }
        return changed;
    }

    private static boolean acceptable(int emoticonId) {
        if (!EmoticonCatalog.wellFormed(emoticonId)) {
            return false;
        }
        if (EmoticonCatalog.knowsGroup(EmoticonCatalog.groupOf(emoticonId))) {
            return EmoticonCatalog.exists(emoticonId);
        }
        return flag(ALLOW_UNLISTED_FLAG, true);
    }

    /** Removes an emoticon (GM / expiry tooling); also clears the commands bound to it. */
    public boolean revoke(Char chr, int emoticonId) {
        if (chr == null) {
            return false;
        }
        State s = state(chr.getId());
        synchronized (s) {
            if (s.owned.remove(emoticonId) == null) {
                return false;
            }
            List<Integer> cmdGone = new ArrayList<>();
            s.commands.values().removeIf(c -> {
                if (c.emoticonId() == emoticonId) {
                    cmdGone.add(c.index());
                    return true;
                }
                return false;
            });
            try {
                EmoticonStore.deleteOwned(chr.getId(), List.of(emoticonId));
                EmoticonStore.deleteCommands(chr.getId(), cmdGone);
            } catch (SQLException e) {
                EmoticonTables.log("revoke emoticon " + emoticonId + " of " + chr.getId(), e);
            }
            return true;
        }
    }

    // ---- queries ---------------------------------------------------------------------------------------------

    /** Owned, non-expired emoticons ordered by id. */
    public List<Owned> list(Char chr) {
        if (chr == null) {
            return List.of();
        }
        State s = state(chr.getId());
        synchronized (s) {
            purgeExpired(chr.getId(), s);
            return List.copyOf(s.owned.values());
        }
    }

    public boolean owns(Char chr, int emoticonId) {
        return list(chr).stream().anyMatch(o -> o.emoticonId() == emoticonId);
    }

    /** Owned group ids, ascending (the tabs of the emoticon window). */
    public List<Integer> groups(Char chr) {
        TreeSet<Integer> g = new TreeSet<>();
        for (Owned o : list(chr)) {
            g.add(o.groupId());
        }
        return List.copyOf(g);
    }

    /** Favorite bar: slot -&gt; emoticon id. */
    public Map<Integer, Integer> favorites(Char chr) {
        Map<Integer, Integer> out = new TreeMap<>();
        for (Owned o : list(chr)) {
            if (o.favoritePos() >= 0) {
                out.put(o.favoritePos(), o.emoticonId());
            }
        }
        return out;
    }

    public List<Command> commands(Char chr) {
        if (chr == null) {
            return List.of();
        }
        State s = state(chr.getId());
        synchronized (s) {
            purgeExpired(chr.getId(), s);
            return List.copyOf(s.commands.values());
        }
    }

    /** Emoticon bound to a chat command (exact match, case-insensitive), if any. */
    public Optional<Integer> resolveCommand(Char chr, String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        String t = text.strip();
        return commands(chr).stream().filter(c -> c.command().equalsIgnoreCase(t)).map(Command::emoticonId).findFirst();
    }

    // ---- favorites -------------------------------------------------------------------------------------------

    /**
     * Puts an owned emoticon on favorite slot {@code pos} (0..{@link #FAVORITE_SLOTS}-1); the previous occupant of
     * the slot is cleared, and the emoticon leaves its old slot. {@code pos} -1 clears the emoticon's slot.
     */
    public boolean setFavorite(Char chr, int emoticonId, int pos) {
        if (chr == null || pos < -1 || pos >= FAVORITE_SLOTS) {
            return false;
        }
        State s = state(chr.getId());
        synchronized (s) {
            purgeExpired(chr.getId(), s);
            Owned target = s.owned.get(emoticonId);
            if (target == null) {
                return false;
            }
            List<Owned> changed = new ArrayList<>();
            if (pos >= 0) {
                for (Owned o : s.owned.values()) {
                    if (o.favoritePos() == pos && o.emoticonId() != emoticonId) {
                        changed.add(o.withFavorite(-1));
                    }
                }
            }
            if (target.favoritePos() != pos) {
                changed.add(target.withFavorite(pos));
            }
            try {
                for (Owned o : changed) {
                    EmoticonStore.upsertOwned(chr.getId(), o);
                }
            } catch (SQLException e) {
                EmoticonTables.log("set favorite emoticon " + emoticonId + " of " + chr.getId(), e);
                return false;
            }
            for (Owned o : changed) {
                s.owned.put(o.emoticonId(), o);
            }
            return true;
        }
    }

    public boolean clearFavorite(Char chr, int emoticonId) {
        return setFavorite(chr, emoticonId, -1);
    }

    // ---- commands --------------------------------------------------------------------------------------------

    /**
     * Binds chat text {@code command} to an owned emoticon at {@code index} (0..{@link #MAX_COMMANDS}-1), replacing
     * the index. The same text may not be bound twice.
     */
    public boolean setCommand(Char chr, int index, int emoticonId, String command) {
        if (chr == null || index < 0 || index >= MAX_COMMANDS || command == null) {
            return false;
        }
        String text = command.strip();
        if (text.isEmpty() || text.length() > MAX_COMMAND_LENGTH) {
            return false;
        }
        State s = state(chr.getId());
        synchronized (s) {
            purgeExpired(chr.getId(), s);
            if (!s.owned.containsKey(emoticonId)) {
                return false;
            }
            for (Command c : s.commands.values()) {
                if (c.index() != index && c.command().equalsIgnoreCase(text)) {
                    return false;
                }
            }
            Command cmd = new Command(index, emoticonId, text);
            try {
                EmoticonStore.upsertCommand(chr.getId(), cmd);
            } catch (SQLException e) {
                EmoticonTables.log("set emoticon command " + index + " of " + chr.getId(), e);
                return false;
            }
            s.commands.put(index, cmd);
            return true;
        }
    }

    public boolean removeCommand(Char chr, int index) {
        if (chr == null) {
            return false;
        }
        State s = state(chr.getId());
        synchronized (s) {
            if (s.commands.remove(index) == null) {
                return false;
            }
            try {
                EmoticonStore.deleteCommands(chr.getId(), List.of(index));
            } catch (SQLException e) {
                EmoticonTables.log("remove emoticon command " + index + " of " + chr.getId(), e);
            }
            return true;
        }
    }

    // ---- CharacterData ---------------------------------------------------------------------------------------

    /**
     * Writes the emoticon block of CharacterData (Char.encode, inside {@code DBChar.Familiar}, replacing the
     * hard-coded zero counts).
     * <p>
     * Flag off (default): {@code i32 0, i32 0, u16 0} - byte-identical to what the server always sent, no database
     * access. Flag on: the 265 layout as far as Char.java documents it (comment "14 bytes" per emoticon, u16 per tab),
     * field order inferred and UNVERIFIED for 282:
     * <pre>
     *   i32 n,  n x { i32 emoticonId, FT expire (MAX_TIME = permanent), u16 favoritePos (0xFFFF = none) }
     *   i32 m,  m x { u16 groupId }            // owned tabs
     *   u16 0                                  // unknown trailing short (commands are not part of it - TODO)
     * </pre>
     */
    public void encodeCharacterData(Char chr, OutPacket out) {
        if (chr == null || !flag(ENCODE_FLAG, false)) {
            out.encodeInt(0);
            out.encodeInt(0);
            out.encodeShort(0);
            return;
        }
        List<Owned> owned = new ArrayList<>(list(chr));
        owned.sort(Comparator.comparingInt(Owned::emoticonId));
        out.encodeInt(owned.size());
        for (Owned o : owned) {
            out.encodeInt(o.emoticonId());
            out.encodeFT(o.permanent() ? FileTime.fromType(FileTime.Type.MAX_TIME)
                    : FileTime.fromInstant(o.expireAt()));
            out.encodeShort(o.favoritePos());
        }
        List<Integer> tabs = groups(chr);
        out.encodeInt(tabs.size());
        for (int g : tabs) {
            out.encodeShort(g);
        }
        out.encodeShort(0);
    }
}
