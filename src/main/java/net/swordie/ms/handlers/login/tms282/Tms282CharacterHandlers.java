package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.Server;
import net.swordie.ms.client.Account;
import net.swordie.ms.client.Client;
import net.swordie.ms.client.User;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.social.Party.Party;
import net.swordie.ms.client.social.Guild.Guild;
import net.swordie.ms.connection.packet.Tms282BurningCharacterRequest;
import net.swordie.ms.connection.packet.Tms282CharacterNameRequest;
import net.swordie.ms.connection.packet.Tms282CharacterPackets;
import net.swordie.ms.connection.packet.Tms282CreateCharacterRequest;
import net.swordie.ms.connection.packet.Tms282CreateCharacterValidator;
import net.swordie.ms.connection.packet.Tms282DeleteCharacterRequest;
import net.swordie.ms.connection.packet.Tms282DisplayedCharactersRequest;
import net.swordie.ms.connection.packet.Tms282LoginUiPackets;
import net.swordie.ms.connection.packet.Tms282SelectCharacterRequest;
import net.swordie.ms.enums.LoginType;
import net.swordie.ms.handlers.LoginHandler;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.util.FileTime;
import net.swordie.ms.world.World;
import net.swordie.ms.world.migration.MigrationRegistry;
import net.swordie.ms.world.migration.MigrationTicket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Character name check, creation, selection (SPW, LP0014 + {@link MigrationTicket}), deletion, burning and
 * displayed-character packets of the TMS 282.3 login socket (probe onCharacterNameCheck, createCharacter,
 * selectCharacter, enterAfterCreation, deleteCharacter, onBurningCharacter, onDisplayedCharacters and the
 * character-dialog declines).
 */
final class Tms282CharacterHandlers {
    /** A reserved character can be deleted this long after its reservation (LP001C deletableAt). */
    static final long DELETE_RESERVATION_MILLIS = TimeUnit.HOURS.toMillis(24);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Tms282LoginContext context;

    Tms282CharacterHandlers(Tms282LoginContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    private Charset charset() {
        return context.settings().nameCharset();
    }

    void register(Tms282Routes routes) {
        routes.login(0x85, InHeader.CHECK_DUPLICATE_ID).endsSpwCancelCheck()
                .require(Tms282Guards.CHARACTER_LIST_VERIFIED, "Character name requested before verified character selection")
                .handle("name check -> LP0017", this::onCharacterNameCheck);
        routes.login(0x8e, InHeader.CREATE_NEW_CHARACTER).endsSpwCancelCheck()
                .require(Tms282Guards.CHARACTER_LIST_VERIFIED, "Character creation requested before verified character selection")
                .handle("create character -> LP0019 / LP0017", this::createCharacter);
        routes.login(0x79, InHeader.SELECT_CHARACTER).endsSpwCancelCheck()
                .require(Tms282Guards.CHARACTER_LIST_SENT, "Character selection requested before character selection")
                .handle("select character (SPW) -> LP0014", this::selectCharacter);
        // CP0090 (str spw, int id) has no UI caller in 282.3 but shares the CP0091 handling.
        routes.login(0x90, InHeader.DELETE_CHARACTER_LEGACY).endsSpwCancelCheck()
                .require(Tms282Guards.CHARACTER_LIST_SENT, "Character deletion requested before character selection")
                .handle("delete character (legacy) -> LP001C / LP001A", this::deleteCharacter);
        routes.login(0x91, InHeader.DELETE_CHARACTER).endsSpwCancelCheck()
                .require(Tms282Guards.CHARACTER_LIST_SENT, "Character deletion requested before character selection")
                .handle("delete character -> LP001C / LP001A", this::deleteCharacter);
        routes.login(0x92, InHeader.RESERVED_DELETE_CHARACTER_CANCEL).endsSpwCancelCheck()
                .require(Tms282Guards.CHARACTER_LIST_SENT, "Character deletion requested before character selection")
                .handle("cancel delete reservation -> LP001D", this::deleteCharacter);
        routes.login(0xc6, InHeader.LOGIN_AFTER_CHAR_CREATION)
                .require(Tms282Guards.WORLD_SELECTED, "CP00C6 before world selection")
                .handle("enter after creation -> LP0014", this::enterAfterCreation);
        // Busy CPs: native replies found statically, not live-tested; the features are not implemented.
        routes.login(0xe8, InHeader.CP00E8).when(Tms282Guards.AUTHENTICATED).handle("rename application -> LP0046 ok=0",
                Tms282AuthHandlers.decline(() -> Tms282CharacterPackets.characterRenameResult(false, 0, null, null, charset()),
                        "character rename application; LP0046 ok=0 shows notice 6"));
        routes.login(0xb1, InHeader.CP00B1).when(Tms282Guards.AUTHENTICATED).handle("CSChangeName -> LP0042 kind 3",
                Tms282AuthHandlers.decline(() -> Tms282LoginUiPackets.requestNotice(3), "CSChangeName; LP0042 kind 3 shows notice 0x0C"));
        routes.login(0xed, InHeader.CP00ED).when(Tms282Guards.AUTHENTICATED).handle("displayed-character reset -> LP006D 1",
                Tms282AuthHandlers.decline(() -> Tms282CharacterPackets.initializeResult(1),
                        "displayed-character reset; LP006D 1 only shows the notice, nothing is reset or reloaded"));
        // CP00C5 burning-character request: recorded as a report, then LP01E9 r=0 (burning not implemented).
        routes.login(0xc5, InHeader.CP00C5).reportFirst().when(Tms282Guards.AUTHENTICATED)
                .handle("burning request -> LP01E9 0", this::onBurningCharacter);
        // CP00EC displayed-character choice: stored in the session only, no reply; the next LP0013 carries it.
        routes.login(0xec, InHeader.CP00EC).when(Tms282Guards.WORLD_SELECTED)
                .handle("displayed characters (stored, no reply)", this::onDisplayedCharacters);
    }

    void onCharacterNameCheck(Tms282LoginState s, int op, byte[] plain) throws Exception {
        var request = Tms282CharacterNameRequest.parse(plain, charset());
        // 0x7A / 0x7B show "name in use" / "name not allowed"; the old 1 / 2 both fell to "unknown failure".
        int result = !Tms282CharacterNameRequest.isValidName(request.name(), charset())
                ? Tms282CreateCharacterValidator.NAME_NOT_ALLOWED
                : Char.isAvailableCharacter(request.name(), s.selectedAccount().getWorldId())
                ? Tms282CreateCharacterValidator.NAME_OK : Tms282CreateCharacterValidator.NAME_IN_USE;
        s.send(Tms282CharacterPackets.nameCheckResult(request, result));
        System.out.println("[TMS282] SENT name check result=" + result + " bytes=" + request.nameBytes().length
                + " context=" + request.contextInt() + "/" + request.contextShort() + "/" + request.contextByte());
    }

    void createCharacter(Tms282LoginState s, int op, byte[] plain) throws Exception {
        var request = Tms282CreateCharacterRequest.parse(plain, charset());
        int[] items = request.items();
        Account account = s.selectedAccount();
        User user = s.user();
        int worldId = account.getWorldId();
        var check = Tms282CreateCharacterValidator.validate(request, context.makeCharInfo(),
                name -> Char.isAvailableCharacter(name, worldId), account.getCharacters().size(), user.getCharacterSlots(), charset());
        if (!check.ok()) {
            rejectCreation(s, request, check.nameResult(), check.problem());
            return;
        }
        Char chr;
        try {
            chr = LoginHandler.createNewCharacter(account, request.name(), request.keySettingType(),
                    request.eventNewCharSaleJob(), check.loginJob(), request.subJob(), request.gender(), request.skin(),
                    check.face(), check.hair(), items, worldId);
        } catch (Exception e) {
            // createNewCharacter already deleted a partial character and left the account unchanged.
            rejectCreation(s, request, 2, e.toString());
            return;
        }
        if (chr == null || chr.getId() <= 0) {
            rejectCreation(s, request, 2, "character was not saved");
            return;
        }
        // LP0019 compares this int with CLogin+62C; the world ID is still a live-test hypothesis.
        s.send(Tms282CharacterPackets.createCharacterSuccess(chr, worldId, charset(), context.settings().avatarAuxFlag()));
        s.createdThisSession().add(chr.getId());
        // A stored CP00EC list would otherwise hide the new character until the session ends.
        s.displayedCharacters().computeIfPresent(worldId, (world, ids) -> {
            List<Integer> updated = new ArrayList<>(ids);
            updated.add(chr.getId());
            return List.copyOf(updated);
        });
        LoginHandler.grantStarterItems(chr, request.race());
        System.out.println("[TMS282] CREATED characterId=" + chr.getId() + " race=" + request.race() + " job=" + chr.getJob()
                + " world=" + worldId + " items=" + Arrays.toString(items));
    }

    /** A rejected creation is answered through the LP0017 name-check result (validator NAME_IN_USE / NAME_NOT_ALLOWED / REJECTED). */
    void rejectCreation(Tms282LoginState s, Tms282CreateCharacterRequest request, int result, String reason) {
        byte[] nameBytes = request.name().getBytes(charset());
        s.send(Tms282CharacterPackets.nameCheckResult(new Tms282CharacterNameRequest(request.name(), 0, 0, 0, nameBytes), result));
        System.out.println("[TMS282] REJECTED create race=" + request.race() + ": " + reason);
    }

    static byte[] rotateRight(byte[] raw, int n) {
        byte[] out = new byte[raw.length];
        for (int i = 0; i < raw.length; i++) {
            int b = raw[i] & 0xFF;
            out[i] = (byte) (((b >>> n) | (b << (8 - n))) & 0xFF);
        }
        return out;
    }

    /** Label of the byte rotation (rotr0..7) that turns the received SPW into the PIC, or null (diagnostics). */
    String matchingSpwRotation(Tms282LoginState s, Tms282SelectCharacterRequest request) {
        for (int n = 0; n < 8; n++) {
            if (s.secondaryPasswordMatches(new String(rotateRight(request.spwRaw(), n), charset()))) {
                return "rotr" + n;
            }
        }
        return null;
    }

    /**
     * CP0079: manual entry is plaintext or rotr(key%7) (not yet told apart, both accepted); the auto-reselect
     * rotates left by key%7+1 (enter-game-2823.md 1.3, proven). The log says which one matched.
     */
    void selectCharacter(Tms282LoginState s, int op, byte[] plain) throws Exception {
        var request = Tms282SelectCharacterRequest.parse(plain);
        Char chr = s.characterInSelectedWorld(request.characterId());
        int k = Math.floorMod(request.spwKey(), 7);
        String typed = new String(request.spwRaw(), charset());
        String typedRotated = new String(rotateRight(request.spwRaw(), k), charset());
        String reselect = new String(request.spwPlainBytes(), charset());
        if (chr == null || chr.getDeletionStartTime() > 0) {
            // The client silently ignores LP0014 for an unknown ID, so answer with a visible error.
            s.send(Tms282CharacterPackets.selectCharacterFailure(LoginType.NotRegistered.getValue()));
            System.out.println("[TMS282] REJECTED select: character " + request.characterId() + " not in world "
                    + s.selectedAccount().getWorldId());
            return;
        }
        String accepted = context.switchTickets().consume(s.user().getId(), s.selectedAccount().getId(), chr.getId(), reselect)
                ? "single-use authenticated switch"
                : s.secondaryPasswordMatches(typed) ? "typed plaintext"
                : k != 0 && s.secondaryPasswordMatches(typedRotated) ? "typed rotr(key%7)"
                : s.secondaryPasswordMatches(reselect) ? "auto-reselect rotr(key%7+1)" : null;
        if (accepted == null) {
            String rule = matchingSpwRotation(s, request); // diagnostic only: an unknown encoding is still rejected
            if (rule != null) {
                System.out.println("[TMS282] SPW would match unaccepted rule " + rule + " (key%7=" + k + ")");
            }
            s.send(Tms282CharacterPackets.selectCharacterFailure(LoginType.IncorrectPassword.getValue()));
            System.out.println("[TMS282] REJECTED select: secondary password mismatch for character " + chr.getId()
                    + " (spwBytes=" + request.spwRaw().length + ")");
            return;
        }
        System.out.println("[TMS282] SPW accepted as " + accepted + " (key%7=" + k + ")");
        saveFavorite(s.user(), request);
        sendSelectSuccess(s, chr, "select");
        System.out.println("[TMS282] CP0079 triples=(" + request.worldA() + "," + request.channelA() + "," + request.charIdA() + ")/("
                + request.worldB() + "," + request.channelB() + "," + request.charIdB() + ") ui=" + request.uiByte()
                + " trailer=" + request.trailer());
    }

    /** CP0079 tuple A is the native favorite selection, not the selected character's identity. */
    void saveFavorite(User user, Tms282SelectCharacterRequest request) {
        if (request.flagA() != 0 && request.flagA() != 1) return;
        try {
            net.swordie.ms.connection.packet.Tms282QuickSelection favorite = null;
            if (request.flagA() == 1) {
                favorite = new net.swordie.ms.connection.packet.Tms282QuickSelection(request.worldA(), request.channelA(), request.charIdA());
                if (user.getAccountByWorldId(favorite.worldId()) == null) user.loadAccountsData();
                if (context.quickCharacter(user, favorite) == null) {
                    System.out.println("[TMS282] Ignored favorite outside owned available characters");
                    return;
                }
            }
            context.characterPreferences().write(user.getId(), favorite);
        } catch (IOException | IllegalArgumentException e) {
            System.out.println("[TMS282] Character favorite not saved: " + e.getMessage());
        }
    }

    /** CP00C6 (int newCharacterId, u8): enter right after LP0019; only for this session's new characters. */
    void enterAfterCreation(Tms282LoginState s, int op, byte[] plain) throws Exception {
        if (plain.length != 7) {
            throw new IOException("Unexpected CP00C6 size");
        }
        int characterId = ByteBuffer.wrap(plain, 2, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
        Char chr = s.characterInSelectedWorld(characterId);
        if (chr == null || !s.createdThisSession().contains(characterId)) {
            s.send(Tms282CharacterPackets.selectCharacterFailure(LoginType.NotRegistered.getValue()));
            System.out.println("[TMS282] REJECTED enter-after-creation for character " + characterId);
            return;
        }
        sendSelectSuccess(s, chr, "enter-after-creation");
    }

    /**
     * LP0014 with the {@link ChannelAddressPolicy} address (default 202.80.104.26; {@code auto} = the login socket's own
     * IPv4 address) and the port of server channel (wire channel + 1), after a one-time {@link MigrationTicket}
     * (character + user + world, TTL) was issued for the channel's CP007E.
     */
    void sendSelectSuccess(Tms282LoginState s, Char chr, String reason) throws Exception {
        Client client = s.client();
        SocketAddress local = client.getCh().localAddress();
        byte[] ip = ChannelAddressPolicy.server().channelAddress(local);
        if (ip == null) {
            throw new IOException("Channel endpoint must be IPv4");
        }
        Account account = s.selectedAccount();
        int token = RANDOM.nextInt();
        int key = RANDOM.nextInt();
        int worldType = context.worlds().worldType(account.getWorldId());
        byte serverChannel = Tms282LoginContext.serverChannel(s.selectedChannel());
        int port = Tms282LoginContext.channelPort(serverChannel);
        if (Server.get().getWorld() != null) {
            // As the 265 character login: finish stale sessions of this account, keep migrations in progress.
            Server.get().prepareAccountForCharacterLogin(account);
        }
        MigrationRegistry migrations = context.migrations();
        migrations.issue(MigrationTicket.tms282(chr.getId(), s.user(), account, serverChannel, s.selectedChannel(), token,
                client, migrations.now()));
        client.setNextState(Client.IN_FIELD);
        s.send(Tms282CharacterPackets.selectCharacterSuccess(ip, port, chr.getId(), worldType, token, key));
        System.out.println("[TMS282] SENT select-character success (" + reason + ") character=" + chr.getId() + " name=" + chr.getName()
                + " channel=" + ChannelAddressPolicy.text(ip) + ":" + port + " (local " + local + ", server channel " + serverChannel
                + ", wire " + s.selectedChannel() + ")");
    }

    /**
     * CP0090/CP0091 reserve, then (after deletableAt, SPW again) delete; CP0092 cancels a reservation. Times are UTC
     * FILETIMEs, the clock frame LP0013 uses (A3).
     */
    void deleteCharacter(Tms282LoginState s, int op, byte[] plain) throws Exception {
        var request = Tms282DeleteCharacterRequest.parse(plain, charset());
        Char chr = s.characterInSelectedWorld(request.characterId());
        if (request.opcode() == 0x92) {
            if (chr != null) {
                chr.setDeletionStartTime(0);
                chr.updateCharFieldToSQL("deletionStartTime", 0L);
            }
            s.send(Tms282CharacterPackets.cancelDeleteReservation(request.characterId()));
            System.out.println("[TMS282] CANCELLED delete reservation character=" + request.characterId() + " known=" + (chr != null));
            return;
        }
        long now = System.currentTimeMillis();
        long reservedAt = chr == null ? 0 : chr.getDeletionStartTime();
        if (reservedAt != 0) {
            long deletableAt = reservedAt + DELETE_RESERVATION_MILLIS;
            if (!s.secondaryPasswordMatches(request.spw())) {
                // Only a wrong SPW uses LP001A 0x14: a non-zero LP001C result would reset the client's reservation map.
                s.send(Tms282CharacterPackets.deleteCharacterResult(chr.getId(), 0x14));
                System.out.println("[TMS282] REJECTED delete character=" + chr.getId() + ": secondary password mismatch");
            } else if (now < deletableAt) {
                // Not due yet: repeat the stored reservation so the client rewrites its map and clears busy.
                s.send(Tms282CharacterPackets.reserveDeleteSuccess(chr.getId(), FileTime.fromLong(reservedAt), FileTime.fromLong(deletableAt)));
                System.out.println("[TMS282] KEPT delete reservation character=" + chr.getId() + ": not due until " + Instant.ofEpochMilli(deletableAt));
            } else {
                completeDeletion(s, chr);
            }
            return;
        }
        if (chr == null || !s.secondaryPasswordMatches(request.spw())) {
            s.send(Tms282CharacterPackets.reserveDeleteFailure(request.characterId()));
            System.out.println("[TMS282] REJECTED delete character=" + request.characterId() + " known=" + (chr != null));
            return;
        }
        // First request: reserve only; a CP0091/CP0090 after deletableAt completes the deletion.
        chr.setDeletionStartTime(now);
        chr.updateCharFieldToSQL("deletionStartTime", now);
        s.send(Tms282CharacterPackets.reserveDeleteSuccess(chr.getId(), FileTime.fromLong(now), FileTime.fromLong(now + DELETE_RESERVATION_MILLIS)));
        System.out.println("[TMS282] RESERVED delete character=" + chr.getId() + " name=" + chr.getName());
    }

    /** Due reservation: LoginHandler.handleReservedDeleteCharacterConfirm's cleanup, then LP001A. */
    void completeDeletion(Tms282LoginState s, Char chr) {
        // step 6: the save gate of this id first (a logout save of it may still run without ECONOMY_MONITOR)
        net.swordie.ms.client.character.tms282.EconomyLocks.withGate(chr.getId(), () -> {
            completeDeletionGated(s, chr);
            return null;
        });
    }

    private void completeDeletionGated(Tms282LoginState s, Char chr) {
        synchronized (net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime.ECONOMY_MONITOR) {
            try {
                if (!net.swordie.ms.client.social.Guild.Tms282GuildRuntime.canDeleteCharacter(chr.getId())) {
                    s.send(Tms282CharacterPackets.deleteCharacterResult(chr.getId(), 0x14));
                    System.out.println("[TMS282] REJECTED delete character=" + chr.getId()
                            + ": leave guild, withdraw applications and finish membership recovery first");
                    return;
                }
            } catch (java.sql.SQLException failure) {
                s.send(Tms282CharacterPackets.deleteCharacterResult(chr.getId(), 0x14));
                System.out.println("[TMS282] REJECTED delete: guild state could not be verified, SQLState=" + failure.getSQLState());
                return;
            }
            completeDeletionAfterGuildCheck(s, chr);
        }
    }

    /** Caller retains the economy monitor so an offline approval cannot race the irreversible delete. */
    private void completeDeletionAfterGuildCheck(Tms282LoginState s, Char chr) {
        int characterId = chr.getId();
        String name = chr.getName();
        int guildId = chr.getGuildID();
        int partyId = chr.getPartyID();
        Account account = s.selectedAccount();
        // deleteFromSQL rolls back and swallows SQL errors; it sets the ID to 0 only after commit.
        chr.deleteFromSQL();
        if (chr.getId() != 0) {
            s.send(Tms282CharacterPackets.deleteCharacterResult(characterId, 0x14));
            System.out.println("[TMS282] FAILED delete character=" + characterId + ": SQL deletion rolled back");
            return;
        }
        try {
            World world = Server.get().getWorld();
            if (world != null) {
                Guild guild = guildId == 0 ? null : world.getGuildByID(guildId);
                if (guild != null && !net.swordie.ms.client.social.Guild.Tms282GuildRuntime.enabled()) {
                    guild.removeMember(characterId);
                }
                Party party = partyId == 0 ? null : world.getPartyByPartyID(partyId);
                if (party != null) {
                    if (party.getMembers().size() == 1) {
                        party.disband();
                    } else {
                        party.leave(characterId);
                    }
                }
                Server.get().removeChar(characterId);
            }
            if (account.getUnion() != null && account.getUnion().getUnionBoards() != null) {
                for (var board : account.getUnion().getUnionBoards()) {
                    board.removeMemberByCharId(characterId);
                }
            }
            account.removeLinkSkillByChar(characterId);
        } catch (Exception e) {
            // The character rows are already gone; report success so the client list matches the database.
            System.out.println("[TMS282] WARNING delete character=" + characterId + " guild/party/union/link-skill cleanup failed: " + e);
        }
        // removeCharacter matches by getId(), which is now 0 for this deleted object only.
        account.removeCharacter(chr);
        s.createdThisSession().remove(characterId);
        s.send(Tms282CharacterPackets.deleteCharacterResult(characterId, 0));
        System.out.println("[TMS282] DELETED character=" + characterId + " name=" + name + " (reservation due; SENT LP001A 0)");
    }

    /** CP00C5 (already recorded as a report): LP01E9 r=0 clears the client's pending flag, even when malformed. */
    void onBurningCharacter(Tms282LoginState s, int op, byte[] plain) {
        String detail;
        try {
            var request = Tms282BurningCharacterRequest.parse(plain);
            boolean ownAccount = request.accountId() == s.user().getId();
            boolean known = s.selectedAccount() != null && s.characterInSelectedWorld(request.characterId()) != null;
            detail = "character=" + request.characterId() + " burnType=" + request.burnType() + " job=" + request.job()
                    + " flag=" + request.flag() + " trailer=" + request.trailer() + " ownAccount=" + ownAccount + " knownCharacter=" + known;
        } catch (IllegalArgumentException e) {
            detail = "malformed (" + e.getMessage() + ")";
        }
        s.send(Tms282CharacterPackets.burningResultNone());
        System.out.println("[TMS282] CP00C5 burning request " + detail + ": burning not implemented, SENT LP01E9 r=0 (pending cleared)");
    }

    /** CP00EC: validated, then kept per world for the next LP0013 (limitFlag 1). No reply packet exists. */
    void onDisplayedCharacters(Tms282LoginState s, int op, byte[] plain) {
        Tms282DisplayedCharactersRequest request;
        try {
            request = Tms282DisplayedCharactersRequest.parse(plain);
        } catch (IllegalArgumentException e) {
            System.out.println("[TMS282] REJECTED CP00EC: " + e.getMessage());
            return;
        }
        // app+23AC is the ID LP0000 sends (user ID), not the per-world account row.
        if (request.accountId() != s.user().getId()) {
            System.out.println("[TMS282] REJECTED CP00EC: account " + request.accountId() + " is not user " + s.user().getId());
            return;
        }
        List<Integer> ids = request.characterIds();
        if (new HashSet<>(ids).size() != ids.size()) {
            System.out.println("[TMS282] REJECTED CP00EC: duplicate character ID");
            return;
        }
        int worldId = s.selectedAccount().getWorldId();
        for (int id : ids) {
            if (s.characterInSelectedWorld(id) == null) {
                System.out.println("[TMS282] REJECTED CP00EC: character " + id + " is not in world " + worldId);
                return;
            }
        }
        s.displayedCharacters().put(worldId, ids);
        System.out.println("[TMS282] STORED displayed characters (session only) world=" + worldId + " ids=" + ids
                + "; no reply, next LP0013 sends limitFlag 1");
    }
}
