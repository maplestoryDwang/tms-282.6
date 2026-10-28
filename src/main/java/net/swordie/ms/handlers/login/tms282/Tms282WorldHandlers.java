package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.Account;
import net.swordie.ms.client.User;
import net.swordie.ms.connection.packet.Tms282CharacterPackets;
import net.swordie.ms.connection.packet.Tms282DisplayedCharacters;
import net.swordie.ms.connection.packet.Tms282LoginUiPackets;
import net.swordie.ms.connection.packet.Tms282WorldPackets;
import net.swordie.ms.handlers.header.InHeader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.SecureRandom;
import java.util.List;
import java.util.Objects;

/**
 * World list, channel view and world selection of the TMS 282.3 login socket (probe worlds, quickLogin,
 * onViewChannels, selectWorld, onReturnToWorldSelect, onWorldRefresh, onWorldChannelExtraRequest,
 * onLoginListRequest), over {@link Tms282WorldCatalog}.
 */
final class Tms282WorldHandlers {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final Tms282LoginContext context;

    Tms282WorldHandlers(Tms282LoginContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    void register(Tms282Routes routes) {
        routes.login(0x76, InHeader.VIEW_CHANNEL_REQUEST).when(Tms282Guards.AUTHENTICATED).handle("view channels -> LP0012", this::onViewChannels);
        routes.login(0x7c0, InHeader.CP07C0).when(Tms282Guards.AUTHENTICATED).handle("quick login -> LP0B7F", (s, op, p) -> quickLogin(s));
        routes.login(0x77, InHeader.SELECT_WORLD).when(Tms282Guards.AUTHENTICATED).handle("select world -> LP0010/0011/0013", this::selectWorld);
        routes.login(0x86, InHeader.LOGOUT_WORLD).when(Tms282Guards.AUTHENTICATED).handle("return to world select", this::onReturnToWorldSelect);
        routes.login(0x84, InHeader.WORLD_INFO_REQUEST).when(Tms282Guards.AUTHENTICATED).handle("world refresh -> worlds", this::onWorldRefresh);
        routes.login(0x83, InHeader.CP0083).when(Tms282Guards.AUTHENTICATED).handle("world list -> worlds", (s, op, p) -> sendWorlds(s));
        routes.login(0xb6, InHeader.CP00B6).require(Tms282Guards.AUTHENTICATED, "CP00B6 before authentication")
                .handle("world channel extra -> LP005F + LP0043", this::onWorldChannelExtraRequest);
        routes.login(0xde, InHeader.LOGIN_NOTICE_LIST_REQUEST).handle("login list -> empty LP0060", this::onLoginListRequest);
    }

    /** LP000B per world, LP006B VIP map, LP000B end, [LP000C last world], [LP0B7F when no last character]. */
    void sendWorlds(Tms282LoginState s) throws IOException {
        Tms282WorldCatalog catalog = context.worlds();
        User user = s.user();
        if (user.getLastCharID() != 0 && user.getAccounts().isEmpty()) user.loadAccountsData();
        var favorite = context.favorite(user);
        var previous = previousSelection(user);
        for (Tms282WorldCatalog.World world : catalog.worlds()) {
            s.send(Tms282WorldPackets.worldInformation(world.id(), world.name(), catalog.channelNames(world), catalog.worldMessage(world.id()),
                    context.settings().nameCharset()));
        }
        // CLogin::OnPacket case 0x6B, RVA 0x3c9359b: count + (world, channel, minimum grade).
        s.send(Tms282WorldPackets.vipChannels(catalog.vipGrades()));
        s.send(Tms282WorldPackets.worldEnd(0, catalog.worldIds(), user.getCharacterSlots(), favorite, previous));
        Integer lastWorld = context.lastWorld().get(user.getId());
        if (lastWorld != null && catalog.contains(lastWorld)) {
            s.send(Tms282WorldPackets.latestConnectedWorld(lastWorld));
            System.out.println("[TMS282] SENT latest connected world " + lastWorld);
        }
        if (!s.nativeRelogin()) quickLogin(s);
        System.out.println("[TMS282] SENT worlds=" + catalog.worldIds() + " normal=" + Tms282WorldCatalog.NORMAL_CHANNELS
                + " VIP=" + catalog.vipChannelCount());
        if (!s.nativeRelogin()) resumeApprovedSwitch(s);
    }

    /** An empty LP015F cookie preserves ordinary account auth but disables native automatic CP0077.
     * After that auth succeeds, send the approved world's list to trigger native CP0079 auto-reselection.
     */
    void resumeApprovedSwitch(Tms282LoginState s) throws IOException {
        if (!s.authenticated() || s.characterListSent()) return;
        var target = context.switchTickets().pendingTarget(s.user().getId()).orElse(null);
        if (target == null) return;
        if (!target.peerAddress().isEmpty() && !target.peerAddress().equals(s.client().getIP())) {
            System.out.println("[TMS282] Pending switch belongs to another connection source");
            return;
        }
        if (s.user().getAccounts().isEmpty()) s.user().loadAccountsData();
        Account account = s.user().getAccountByWorldId(target.worldId());
        var choice = new net.swordie.ms.connection.packet.Tms282QuickSelection(
                target.worldId(), target.channelId(), target.characterId());
        if (account == null || account.getId() != target.accountId() || context.quickCharacter(s.user(), choice) == null) {
            System.out.println("[TMS282] Approved switch no longer has an available owned target");
            return;
        }
        // Reconnect resets the client's report-completion flag. Restore only a report validated on the
        // source session, after fresh account/target checks, before LP0013 can enter its SPW wait path.
        if (target.fileReportVerified() && !s.fileReportVerified()) {
            s.markFileReportVerified();
            s.send(net.swordie.ms.connection.packet.Tms282LoginSystemPackets.fileCheckComplete());
            System.out.println("[TMS282] Restored validated source file report for approved character switch");
        }
        sendCharacterList(s, account, target.channelId());
        System.out.printf("[TMS282] Resumed approved switch world=%d channel=%d target=%d after account authentication%n",
                target.worldId(), target.channelId(), target.characterId());
    }

    /** CP07C0 / after the world list: only an owned, available last character may be shown. */
    void quickLogin(Tms282LoginState s) throws IOException {
        User user = s.user();
        if (user.getLastCharID() != 0 && user.getAccounts().isEmpty()) user.loadAccountsData();
        var favorite = context.favorite(user);
        var character = favorite == null ? lastCharacter(user) : context.quickCharacter(user, favorite);
        s.send(character == null ? Tms282CharacterPackets.noQuickLoginCharacter()
                : Tms282CharacterPackets.quickLoginCharacter(character, context.settings().nameCharset(),
                        context.settings().avatarAuxFlag()));
        System.out.println("[TMS282] SENT quick-login character=" + (character == null ? 0 : character.getId()));
    }

    net.swordie.ms.client.character.Char lastCharacter(User user) {
        if (user.getLastCharID() <= 0) return null;
        for (Account account : user.getAccounts()) {
            if (!context.worlds().contains(account.getWorldId()) || account.getCharacters() == null) continue;
            var chr = account.getCharById(user.getLastCharID());
            if (chr != null && chr.getAccId() == account.getId() && chr.getDeletionStartTime() == 0) return chr;
        }
        return null;
    }

    private net.swordie.ms.connection.packet.Tms282QuickSelection previousSelection(User user) {
        var chr = lastCharacter(user);
        if (chr == null) return null;
        // The owning account is authoritative; a log-only avatar world field may still be zero.
        for (Account account : user.getAccounts()) {
            if (account.getId() == chr.getAccId()) return new net.swordie.ms.connection.packet.Tms282QuickSelection(
                    account.getWorldId(), 0, chr.getId());
        }
        return null;
    }

    /** Native3C9C5B1: opcode, opaque byte, int world, opaque byte. */
    void onViewChannels(Tms282LoginState s, int op, byte[] plain) throws IOException {
        if (plain.length != 8) {
            throw new IOException("Unexpected view-channels request size");
        }
        int world = ByteBuffer.wrap(plain, 3, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
        if (!context.worlds().contains(world)) {
            throw new IOException("Unknown requested world");
        }
        s.send(Tms282WorldPackets.viewChannels(world));
    }

    /** CP0077 u8 world, u8 zero-based channel: LP0010 client key, LP0011 physical world, LP0013 character list. */
    void selectWorld(Tms282LoginState s, int op, byte[] plain) throws Exception {
        if (plain.length < 5) {
            throw new IOException("Truncated select-world request");
        }
        int channel = Byte.toUnsignedInt(plain[4]);
        int worldId = Byte.toUnsignedInt(plain[3]);
        Tms282WorldCatalog catalog = context.worlds();
        User user = s.user();
        if (!catalog.contains(worldId) || channel >= catalog.channelCount()) {
            rejectWorld(s, "This world or channel is unavailable.");
            return;
        }
        if (channel >= Tms282WorldCatalog.NORMAL_CHANNELS && user.getVipGrade() < catalog.minimumVipGrade(channel)) {
            rejectWorld(s, "Your VIP grade does not meet this channel's requirement.");
            return;
        }
        if (s.nativeRelogin()) {
            var target = context.switchTickets().pendingTarget(user.getId()).orElse(null);
            if (target == null || !context.switchTickets().hasAuthenticatedRelogin(user.getId())
                    || target.worldId() != worldId || target.channelId() != channel
                    || !target.peerAddress().equals(s.client().getIP())) {
                rejectWorld(s, "The character switch has expired or does not match this world/channel.");
                return;
            }
            if (user.getAccounts().isEmpty()) user.loadAccountsData();
            Account owned = user.getAccountByWorldId(worldId);
            var choice = new net.swordie.ms.connection.packet.Tms282QuickSelection(worldId, channel, target.characterId());
            if (owned == null || owned.getId() != target.accountId() || context.quickCharacter(user, choice) == null) {
                rejectWorld(s, "The requested character is unavailable.");
                return;
            }
            if (target.fileReportVerified() && !s.fileReportVerified()) {
                s.markFileReportVerified();
                s.send(net.swordie.ms.connection.packet.Tms282LoginSystemPackets.fileCheckComplete());
            }
            sendCharacterList(s, owned, channel);
            System.out.printf("[TMS282] Native CP0077 resumed switch target=%d world=%d channel=%d%n",
                    target.characterId(), worldId, channel);
            return;
        }
        Account account = Account.getOrCreateWorldAccount(user, worldId);
        if (account.getId() <= 0 || account.getWorldId() != worldId) {
            throw new IOException("Invalid account world");
        }
        sendCharacterList(s, account, channel);
    }

    private void sendCharacterList(Tms282LoginState s, Account account, int channel) throws IOException {
        User user = s.user();
        int worldId = account.getWorldId();
        Tms282WorldCatalog catalog = context.worlds();
        if (account.getCharacters() == null) {
            throw new IOException("Account characters were not loaded");
        }
        s.send(Tms282WorldPackets.clientKey(RANDOM.nextLong()));
        s.send(Tms282WorldPackets.physicalWorld(worldId));
        int worldType = catalog.worldType(worldId);
        Tms282DisplayedCharacters displayed = account.getCharacters().isEmpty() ? null : s.displayedFor(account);
        var settings = context.settings();
        var previous = previousSelection(user);
        s.send(Tms282CharacterPackets.characterList(user, account, channel, settings.nameCharset(), settings.avatarAuxFlag(), worldType,
                displayed == null ? Tms282DisplayedCharacters.showAll(user.getCharacterSlots()) : displayed,
                context.favorite(user), previous));
        s.characterListSent(account, channel);
        context.lastWorld().put(user.getId(), worldId);
        System.out.println("[TMS282] SENT character list accountId=" + account.getId() + " world=" + worldId + " worldType=" + worldType
                + " characters=" + account.getCharacters().size()
                + (displayed != null && displayed.limitFlag() != 0 ? " displayed=" + displayed.characterIds() : ""));
    }

    void rejectWorld(Tms282LoginState s, String message) {
        s.send(Tms282CharacterPackets.characterListFailure(message));
        System.out.println("[TMS282] REJECTED select-world: " + message);
    }

    void onReturnToWorldSelect(Tms282LoginState s, int op, byte[] plain) throws IOException {
        if (plain.length != 31) {
            throw new IOException("Unexpected return-to-world request size");
        }
        s.returnToWorldSelect();
        context.switchTickets().cancel(s.user().getId());
        System.out.println("[TMS282] CLIENT returning to world selection");
    }

    void onWorldRefresh(Tms282LoginState s, int op, byte[] plain) throws IOException {
        if (plain.length != 2 && plain.length != 3) {
            throw new IOException("Unexpected world refresh size");
        }
        sendWorlds(s);
    }

    /**
     * CP00B6 u16 = first word of a CLogin+260 world record (presumed world ID); busy=1. Presumed reply LP005F is not
     * documented to clear CLogin+220, so LP0043 follows it.
     */
    void onWorldChannelExtraRequest(Tms282LoginState s, int op, byte[] plain) throws IOException {
        if (plain.length != 4) {
            throw new IOException("Unexpected CP00B6 size");
        }
        int worldId = Short.toUnsignedInt(ByteBuffer.wrap(plain, 2, 2).order(ByteOrder.LITTLE_ENDIAN).getShort());
        boolean known = context.worlds().contains(worldId);
        if (known) {
            s.send(Tms282WorldPackets.worldChannelExtra(worldId, List.of()));
        }
        s.send(Tms282LoginUiPackets.unlockRequest());
        System.out.println("[TMS282] CP00B6 world=" + worldId + (known ? ": SENT empty LP005F + LP0043" : ": unknown world, SENT LP0043 unlock only"));
    }

    /** CP00DE int nonce -> LP0060 must echo it (3C9A330); no list content exists here. */
    void onLoginListRequest(Tms282LoginState s, int op, byte[] plain) throws IOException {
        if (plain.length != 6) {
            throw new IOException("Unexpected CP00DE size");
        }
        int nonce = ByteBuffer.wrap(plain, 2, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
        s.send(Tms282LoginUiPackets.emptyLoginList(nonce));
        System.out.println("[TMS282] SENT empty LP0060 list for nonce " + nonce);
    }
}
