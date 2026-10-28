package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.Account;
import net.swordie.ms.client.Client;
import net.swordie.ms.client.User;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.packet.AuctionHousePacket;
import net.swordie.ms.connection.packet.Stage;
import net.swordie.ms.connection.packet.Tms282AuctionRequest;
import net.swordie.ms.connection.packet.Tms282CashShopTranslators;
import net.swordie.ms.connection.packet.Tms282StatPackets;
import net.swordie.ms.enums.FieldOption;
import net.swordie.ms.handlers.AuctionHouseHandler;
import net.swordie.ms.handlers.dispatch.ClientGuard;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.loaders.StringData;
import net.swordie.ms.world.field.Field;
import net.swordie.ms.world.migration.MigrationRegistry;
import net.swordie.ms.world.migration.MigrationTicket;
import net.swordie.ms.world.shop.auctionhouse.AuctionItem;
import net.swordie.ms.world.shop.auctionhouse.Tms282AuctionSearch;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * TMS 282.3 auction house on the channel socket, entered in place as 265 (no socket change on the way in):
 * <ul>
 *     <li>CP0100 USER_MIGRATE_TO_AUCTION_HOUSE_REQUEST [5DEF680: i32; 265 reads nothing] -> {@link States#enter}
 *     (265 {@code Client.migrateIn(true)}: out of the field, state IN_AUCTION_HOUSE) and LP02F0 SET_AUCTION_FIELD
 *     ({@code Stage.setAuctionField}, 282 body from {@code Tms282AuctionPackets}). Refused (field limit, dead, already
 *     inside): LP009D unlock and a chat line;</li>
 *     <li>CP013B AUCTION_HOUSE_REQUEST ({@link Tms282AuctionRequest}): the types whose 282 body begins with the 265
 *     read go to 265 {@code AuctionHouseHandler} as sent; its replies (LP0A96 show / market price / selling / sold /
 *     wish list / sell / buy results) are built with the 282 header and item slots. Type 32 (search) is answered
 *     here with the selling entries whose item name contains the searched text (265 has no reachable search case);
 *     other types are logged;</li>
 *     <li>CP013C AUCTION_LEAVE_REQUEST [2CBFCF0, no body] -> {@link States#leave} (265 {@code auctionHouseOut}
 *     state), a TMS 282 migration ticket and LP001F to this socket's own endpoint, as the cash shop exit.</li>
 * </ul>
 * Nothing here ends the session.
 */
final class Tms282AuctionHandlers {
    /** At most this many search results, as the 265 home list (100). */
    static final int MAX_RESULTS = 100;

    static final ClientGuard IN_AUCTION_HOUSE = Tms282Guards.state("IN_AUCTION_HOUSE",
            s -> s.isChannel() && s.fieldCharacter() != null && s.client().getCurrentState() == Client.IN_AUCTION_HOUSE);

    /** The 265 session-state side of entering and leaving (strategy); offline checks use one without database. */
    interface States {
        void enter(Client c, Char chr);

        void leave(Client c, Char chr);

        static States gms265() {
            return new States() {
                @Override
                public void enter(Client c, Char chr) {
                    c.migrateIn(true);
                }

                @Override
                public void leave(Client c, Char chr) {
                    c.getUser().setClientState(Client.CHANGING_CHANNEL);
                    c.setCurrentState(Client.CHANGING_CHANNEL);
                    c.setNextState(Client.IN_FIELD);
                    c.getUser().saveToSQL(false);
                }
            };
        }
    }

    private final Charset charset;
    private final Supplier<MigrationRegistry> migrations;
    private final States states;
    private final Function<Char, Collection<AuctionItem>> worldItems;
    private final Tms282AuctionSearch search;

    Tms282AuctionHandlers(Tms282LoginContext context) {
        this(context.settings().nameCharset(), context::migrations, States.gms265(),
                chr -> chr.getWorld().getAuctionItems(), new Tms282AuctionSearch(StringData::getItemStringById, MAX_RESULTS));
    }

    Tms282AuctionHandlers(Charset charset, Supplier<MigrationRegistry> migrations, States states,
                          Function<Char, Collection<AuctionItem>> worldItems, Tms282AuctionSearch search) {
        this.charset = Objects.requireNonNull(charset, "charset");
        this.migrations = Objects.requireNonNull(migrations, "migrations");
        this.states = Objects.requireNonNull(states, "states");
        this.worldItems = Objects.requireNonNull(worldItems, "worldItems");
        this.search = Objects.requireNonNull(search, "search");
    }

    void register(Tms282Routes routes) {
        routes.channel(Tms282AuctionRequest.ENTER, InHeader.USER_MIGRATE_TO_AUCTION_HOUSE_REQUEST).when(Tms282Guards.IN_FIELD)
                .handle("enter auction house -> LP02F0", this::onEnter);
        routes.channel(Tms282AuctionRequest.OPCODE, InHeader.AUCTION_HOUSE_REQUEST).when(IN_AUCTION_HOUSE)
                .handle("auction request -> 265 AuctionHouseHandler / search, LP0A96", this::onRequest);
        routes.channel(Tms282AuctionRequest.LEAVE, InHeader.AUCTION_LEAVE_REQUEST).when(IN_AUCTION_HOUSE)
                .handle("leave auction house -> LP001F", this::onLeave);
    }

    // ---- CP0100 ------------------------------------------------------------------------------------------------

    void onEnter(Tms282LoginState session, int op, byte[] packet) {
        Char chr = session.fieldCharacter();
        String refusal = refusal(chr);
        if (refusal != null) {
            System.out.printf("[TMS282] CP0100 auction house refused for character %d: %s%n", chr.getId(), refusal);
            chr.chatMessage("現在無法移動到楓之谷拍賣場。");
            session.send(Tms282StatPackets.unlock());
            return;
        }
        states.enter(session.client(), chr);
        session.send(Stage.setAuctionField(chr));
        System.out.printf("[TMS282] CP0100 character %d entered the auction house; SENT LP02F0%n", chr.getId());
    }

    /** The 265 entry checks ({@code handleUserMigrateToAuctionHouseRequest}). Null when the auction house may open. */
    static String refusal(Char chr) {
        if (!net.swordie.ms.GameSettings.auctionEnabled()) {
            return "feature.auction=false";
        }
        if (chr.getClient() != null && chr.getClient().getCurrentState() == Client.IN_AUCTION_HOUSE) {
            return "already in the auction house";
        }
        Field field = chr.getField();
        if (field == null) {
            return "no field";
        }
        if ((field.getFieldLimit() & FieldOption.MigrateLimit.getVal()) != 0) {
            return "field " + field.getId() + " limits migration";
        }
        if (chr.getHP() <= 0) {
            return "dead";
        }
        if (chr.getUser() == null) {
            return "no user";
        }
        return null;
    }

    // ---- CP013B ------------------------------------------------------------------------------------------------

    void onRequest(Tms282LoginState session, int op, byte[] packet) {
        Char chr = session.fieldCharacter();
        Tms282AuctionRequest request;
        try {
            request = Tms282AuctionRequest.parse(packet, charset);
        } catch (IllegalArgumentException e) {
            System.out.printf("[TMS282] REJECTED CP013B len=%d: %s%n", packet.length, e.getMessage());
            return;
        }
        if (request.passThrough()) {
            System.out.println("[TMS282] " + request.describe() + " -> 265 AuctionHouseHandler");
            try {
                AuctionHouseHandler.handleActionHouseRequest(session.client(), new InPacket(request.body()));
            } catch (RuntimeException e) {
                System.out.printf("[TMS282] FAILED auction type %d character=%d: %s%n", request.type(), chr.getId(), e);
            }
            return;
        }
        if (request.type() == Tms282AuctionRequest.SEARCH) {
            Set<AuctionItem> found = search.find(worldItems.apply(chr), request.searchName());
            session.send(AuctionHousePacket.show(chr, found));
            System.out.printf("[TMS282] %s -> LP0A96 show %d entries%n", request.describe(), found.size());
            return;
        }
        System.out.println("[TMS282] " + request.describe() + " not ported (no 265 case)");
    }

    // ---- CP013C ------------------------------------------------------------------------------------------------

    void onLeave(Tms282LoginState session, int op, byte[] packet) {
        SocketAddress local = session.client().getCh().localAddress();
        if (!(local instanceof InetSocketAddress inet) || inet.getAddress().getAddress().length != 4) {
            System.out.printf("[TMS282] CP013C leave auction house: channel endpoint %s is not IPv4; kept inside%n", local);
            return;
        }
        leave(session, inet);
    }

    /** Back to the field through {@code inet} (this socket's own endpoint; LP001F carries the ChannelAddressPolicy address). */
    void leave(Tms282LoginState session, InetSocketAddress inet) {
        Char chr = session.fieldCharacter();
        Client c = session.client();
        User user = c.getUser();
        Account account = chr.getAccount();
        states.leave(c, chr);
        MigrationRegistry registry = migrations.get();
        registry.issue(MigrationTicket.tms282(chr.getId(), user, account, c.getChannel(), Math.max(0, c.getChannel() - 1),
                0, c, registry.now()));
        byte[] ip = ChannelAddressPolicy.server().channelAddress(inet);
        OutPacket migrate = Tms282CashShopTranslators.migrateCommand(ip, inet.getPort());
        session.send(migrate);
        System.out.printf("[TMS282] CP013C character %d leaves the auction house; SENT LP001F %s:%d (local %s, ticket issued)%n",
                chr.getId(), ChannelAddressPolicy.text(ip), inet.getPort(), inet);
    }
}
