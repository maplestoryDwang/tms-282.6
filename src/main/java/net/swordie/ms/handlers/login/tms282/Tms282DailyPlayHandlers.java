package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.Client;
import net.swordie.ms.client.User;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.client.character.tms282.Tms282ChangeWatch;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.packet.Tms282CashShopTranslators;
import net.swordie.ms.connection.packet.Tms282ChannelTransferRequest;
import net.swordie.ms.connection.packet.Tms282ReturnItemRequest;
import net.swordie.ms.connection.packet.Tms282ShopRequest;
import net.swordie.ms.connection.packet.Tms282StatPackets;
import net.swordie.ms.connection.packet.Tms282TrunkRequest;
import net.swordie.ms.enums.FieldOption;
import net.swordie.ms.enums.InvType;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.handlers.item.ItemHandler;
import net.swordie.ms.handlers.life.NpcHandler;
import net.swordie.ms.handlers.user.UserTrunkHandler;
import net.swordie.ms.loaders.ItemData;
import net.swordie.ms.loaders.containerclasses.ItemInfo;
import net.swordie.ms.world.Channel;
import net.swordie.ms.world.World;
import net.swordie.ms.world.field.Field;
import net.swordie.ms.world.migration.MigrationRegistry;
import net.swordie.ms.world.migration.MigrationTicket;
import net.swordie.ms.Server;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.function.Supplier;

/**
 * TMS 282.3 daily-play requests (channel socket after SetField, {@link Tms282Guards#IN_FIELD}):
 * <ul>
 *     <li>CP012E USER_SHOP_REQUEST ({@link Tms282ShopRequest}, moved here from the NPC phase) -> 265
 *     {@code NpcHandler.handleUserShopRequest}; the shop's own replies are LP0963 through the profile's
 *     {@code ShopPacketEncoder}, the bag and meso changes go out as LP008F / LP009D ({@link Tms282ChangeWatch});</li>
 *     <li>CP0131 USER_TRUNK_REQUEST ({@link Tms282TrunkRequest}) -> 265 {@code UserTrunkHandler}; LP097B through
 *     {@code TrunkPacketEncoder}, bags and mesos as above;</li>
 *     <li>CP00FC USER_TRANSFER_CHANNEL_REQUEST ({@link Tms282ChannelTransferRequest}): the 265 checks, then the
 *     cash-shop exit's way out: 265 migrate-out state, a TMS 282 migration ticket for the target channel and LP001F
 *     to this socket's own address with the target channel's port; the client reconnects and enters with CP007E /
 *     LP02EF. A refusal releases the client's lock (LP009D) and says why in the chat;</li>
 *     <li>CP015F USER_PORTAL_SCROLL_USE_REQUEST -> 265 {@code ItemHandler.handleUserPortalScrollUseRequest}
 *     (SetField, then LP008F for the used scroll);</li>
 *     <li>CP015E USER_MAP_TRANSFER_ITEM_USE_REQUEST: 265 has no handler; an item with an {@code ItemInfo.moveTo}
 *     field is used like a scroll to that field, anything else is refused.</li>
 * </ul>
 * Every path ends with a packet carrying exclRequest. The 265 calls go through {@link Gms265} (strategy) so the
 * offline check can run without the database.
 */
final class Tms282DailyPlayHandlers {

    /** The 265 server-side calls (strategy): {@link #standard()} in the server, stubs in the offline check. */
    interface Gms265 {
        void shop(Char chr, byte[] body);

        void trunk(Char chr, byte[] body);

        void portalScroll(Char chr, byte[] body);

        /** Uses {@code item} (in the consume bag) to move to {@code fieldId}: warp, then one item less. */
        void moveWithItem(Char chr, Item item, int fieldId);

        /** Port of server channel {@code channelId}, empty when the world has no such channel. */
        OptionalInt channelPort(int channelId);

        static Gms265 standard() {
            return new Gms265() {
                @Override
                public void shop(Char chr, byte[] body) {
                    NpcHandler.handleUserShopRequest(chr, new InPacket(body));
                }

                @Override
                public void trunk(Char chr, byte[] body) {
                    UserTrunkHandler.handleUserTrunkRequest(chr, new InPacket(body));
                }

                @Override
                public void portalScroll(Char chr, byte[] body) {
                    ItemHandler.handleUserPortalScrollUseRequest(chr, new InPacket(body));
                }

                @Override
                public void moveWithItem(Char chr, Item item, int fieldId) {
                    Field toField = chr.getOrCreateFieldByCurrentInstanceType(fieldId);
                    if (toField == null) {
                        throw new IllegalStateException("no field " + fieldId);
                    }
                    chr.warp(toField, toField.getDefaultPortal());
                    chr.consumeItem(item.getItemId(), 1);
                }

                @Override
                public OptionalInt channelPort(int channelId) {
                    World world = Server.get().getWorld();
                    Channel channel = world == null ? null : world.getChannelById((byte) channelId);
                    return channel == null ? OptionalInt.empty() : OptionalInt.of(channel.getPort());
                }
            };
        }
    }

    private final Charset charset;
    private final Supplier<MigrationRegistry> migrations;
    private final Tms282CashShopHandlers.States states;
    private final Gms265 gms265;

    Tms282DailyPlayHandlers(Tms282LoginContext context) {
        this(context.settings().nameCharset(), context::migrations, Tms282CashShopHandlers.States.gms265(), Gms265.standard());
    }

    Tms282DailyPlayHandlers(Charset charset, Supplier<MigrationRegistry> migrations, Tms282CashShopHandlers.States states,
                            Gms265 gms265) {
        this.charset = Objects.requireNonNull(charset, "charset");
        this.migrations = Objects.requireNonNull(migrations, "migrations");
        this.states = Objects.requireNonNull(states, "states");
        this.gms265 = Objects.requireNonNull(gms265, "gms265");
    }

    void register(Tms282Routes routes) {
        routes.channel(Tms282ShopRequest.OPCODE, InHeader.USER_SHOP_REQUEST).when(Tms282Guards.IN_FIELD)
                .handle("NPC shop -> 265 shop, LP0963 + LP008F / LP009D", this::onShop);
        routes.channel(Tms282TrunkRequest.OPCODE, InHeader.USER_TRUNK_REQUEST).when(Tms282Guards.IN_FIELD)
                .handle("storage -> 265 trunk, LP097B + LP008F / LP009D", this::onTrunk);
        routes.channel(Tms282ChannelTransferRequest.OPCODE, InHeader.USER_TRANSFER_CHANNEL_REQUEST).when(Tms282Guards.IN_FIELD)
                .handle("change channel -> ticket + LP001F (refused: LP009D)", this::onChangeChannel);
        routes.channel(Tms282ReturnItemRequest.PORTAL_SCROLL, InHeader.USER_PORTAL_SCROLL_USE_REQUEST).when(Tms282Guards.IN_FIELD)
                .handle("return scroll -> 265, LP02EF + LP008F", this::onPortalScroll);
        routes.channel(Tms282ReturnItemRequest.MAP_TRANSFER_ITEM, InHeader.USER_MAP_TRANSFER_ITEM_USE_REQUEST)
                .when(Tms282Guards.IN_FIELD).handle("map transfer item -> ItemInfo.moveTo, LP02EF + LP008F", this::onMapTransferItem);
        routes.channel(DAILY_GIFT_CLAIM, InHeader.DAILY_GIFT_REQUEST).when(Tms282Guards.IN_FIELD)
                .handle("daily gift claim -> 265 DailyGift.claim, LP0AA6", this::onDailyGiftClaim);
        routes.channel(net.swordie.ms.world.tms282.contents.changeburning.ChangeBurningPackets.CONTENTS_REQUEST, InHeader.CP04E1)
                .when(Tms282Guards.IN_FIELD).handle("contents request (5 = 變身燃燒 露希妲) -> RPEventStat + LP02C7", this::onContentsRequest);
        registerSecondAtoms(routes);
        registerForceAtoms(routes);
    }

    private void registerForceAtoms(Tms282Routes routes) {
        // ---- force atom collision: CP01C4 (research/tms2823/force-atom) ----
        routes.channel(net.swordie.ms.connection.packet.Tms282ForceAtomCollision.OPCODE, InHeader.USER_FORCE_ATOM_COLLISION).when(Tms282Guards.IN_FIELD)
                .handle("force atom collision -> 265 net.swordie.ms.handlers.user.SkillHandler.handleForceAtomCollision", (s, op, p) -> {
                    Char chr = s.fieldCharacter();
                    net.swordie.ms.connection.packet.Tms282ForceAtomCollision r;
                    try {
                        r = net.swordie.ms.connection.packet.Tms282ForceAtomCollision.parse(p, key -> {
                            net.swordie.ms.client.character.skills.ForceAtom fa = chr.getForceAtomByKey(key);
                            return fa == null ? -1 : fa.getForceAtomEnum().getForceAtomType();
                        });
                    } catch (IllegalArgumentException e) {
                        Tms282SocialHandlers.rejected(op, p, e);
                        return;
                    }
                    Tms282SocialHandlers.guarded(chr, op, () -> net.swordie.ms.handlers.user.SkillHandler.handleForceAtomCollision(chr, new InPacket(r.toGms265Body())));
                });
    }

    // ---- CP069C / 069D / 069E second atoms (research/tms2823/second-atom, 2026-09-29) ------------------------------

    private void registerSecondAtoms(Tms282Routes routes) {
        routes.channel(net.swordie.ms.connection.packet.Tms282SecondAtomRequest.COMMAND, InHeader.USER_SECOND_ATOM_COMMAND_REQUEST).when(Tms282Guards.IN_FIELD)
                .handle("second atom command -> Lara 265 SecondAtomHandler / LP0463 to the others", (s, op, p) -> {
                    net.swordie.ms.connection.packet.Tms282SecondAtomRequest.Command r;
                    try {
                        r = net.swordie.ms.connection.packet.Tms282SecondAtomRequest.Command.parse(p);
                    } catch (IllegalArgumentException e) {
                        Tms282SocialHandlers.rejected(op, p, e);
                        return;
                    }
                    Char chr = s.fieldCharacter();
                    Tms282SocialHandlers.guarded(chr, op, () -> {
                        if (net.swordie.ms.constants.JobConstants.isLara(chr.getJob())) {
                            if (r.count() > 0) {
                                net.swordie.ms.handlers.user.SecondAtomHandler.handleUserSecondAtomCommandRequest(chr, new InPacket(r.toGms265Body()));
                            }
                        } else if (net.swordie.ms.connection.packet.Tms282SecondAtomTranslators.VISIBLE) {
                            chr.getField().broadcast(r.result(chr.getId()), chr);
                        }
                    });
                });
        routes.channel(net.swordie.ms.connection.packet.Tms282SecondAtomRequest.REMOVE, InHeader.USER_SECOND_ATOM_REMOVE_REQUEST).when(Tms282Guards.IN_FIELD)
                .handle("second atom remove -> 265 SecondAtomHandler per entry (LP0467)", (s, op, p) -> {
                    net.swordie.ms.connection.packet.Tms282SecondAtomRequest.Remove r;
                    try {
                        r = net.swordie.ms.connection.packet.Tms282SecondAtomRequest.Remove.parse(p);
                    } catch (IllegalArgumentException e) {
                        Tms282SocialHandlers.rejected(op, p, e);
                        return;
                    }
                    Char chr = s.fieldCharacter();
                    for (byte[] body : r.toGms265Bodies()) {
                        Tms282SocialHandlers.guarded(chr, op, () -> net.swordie.ms.handlers.user.SecondAtomHandler.handleUserSecondAtomRemoveRequest(chr, new InPacket(body)));
                    }
                });
        routes.channel(net.swordie.ms.connection.packet.Tms282SecondAtomRequest.COLLISION, InHeader.USER_SECOND_ATOM_COLLISION_REQUEST).when(Tms282Guards.IN_FIELD)
                .handle("second atom collision -> 265 SecondAtomHandler (LP0466 to the field)", (s, op, p) -> {
                    net.swordie.ms.connection.packet.Tms282SecondAtomRequest.Collision r;
                    try {
                        r = net.swordie.ms.connection.packet.Tms282SecondAtomRequest.Collision.parse(p);
                    } catch (IllegalArgumentException e) {
                        Tms282SocialHandlers.rejected(op, p, e);
                        return;
                    }
                    Char chr = s.fieldCharacter();
                    Tms282SocialHandlers.guarded(chr, op, () -> net.swordie.ms.handlers.user.SecondAtomHandler.handleUserSecondAtomCollisionRequest(chr, new InPacket(r.toGms265Body())));
                });
        // CP069F / CP06A0 (282 only, no 265 handler): left unrouted (logged as unknown); no reply is expected.
    }

    // ---- CP04E1 ------------------------------------------------------------------------------------------------

    void onContentsRequest(Tms282LoginState session, int op, byte[] packet) {
        net.swordie.ms.world.tms282.contents.changeburning.ChangeBurningPackets.Request request;
        try {
            request = net.swordie.ms.world.tms282.contents.changeburning.ChangeBurningPackets.Request.parse(packet);
        } catch (IllegalArgumentException e) {
            rejected(session, op, packet, e);
            return;
        }
        Char chr = session.fieldCharacter();
        boolean handled = net.swordie.ms.world.tms282.contents.changeburning.ChangeBurning.get().onRequest(chr, request);
        System.out.printf("[TMS282] CP04E1 contents type=%d action=%d%s%n", request.type(), request.action(),
                handled ? "" : " (ignored)");
    }

    // ---- CP03B9 ------------------------------------------------------------------------------------------------

    /** 每日禮物 claim button: sender 48D82B0 writes CP03B9 + i32 (the day); 265 claims today's reward itself. */
    static final int DAILY_GIFT_CLAIM = 0x03B9;

    void onDailyGiftClaim(Tms282LoginState session, int op, byte[] packet) {
        Char chr = session.fieldCharacter();
        delegate(session, chr, "daily gift claim", () -> net.swordie.ms.handlers.user.UserHandler.handleUserReceiveDailyGiftRequest(chr, null));
    }

    // ---- CP012E ------------------------------------------------------------------------------------------------

    void onShop(Tms282LoginState session, int op, byte[] packet) {
        Tms282ShopRequest request;
        byte[] body;
        try {
            request = Tms282ShopRequest.parse(packet, charset);
            body = request.toGms265Body();
        } catch (IllegalArgumentException e) {
            rejected(session, op, packet, e);
            return;
        }
        Char chr = session.fieldCharacter();
        delegate(session, chr, request.toString(), () -> gms265.shop(chr, body));
    }

    // ---- CP0131 ------------------------------------------------------------------------------------------------

    void onTrunk(Tms282LoginState session, int op, byte[] packet) {
        Tms282TrunkRequest request;
        try {
            request = Tms282TrunkRequest.parse(packet);
        } catch (IllegalArgumentException e) {
            rejected(session, op, packet, e);
            return;
        }
        Char chr = session.fieldCharacter();
        if (chr.getAccount() == null || chr.getAccount().getTrunk() == null) {
            refuse(session, chr, request.toString(), "no account trunk", "倉庫目前無法使用。");
            return;
        }
        delegate(session, chr, request.toString(), () -> gms265.trunk(chr, request.gms265Body()));
    }

    // ---- CP00FC ------------------------------------------------------------------------------------------------

    void onChangeChannel(Tms282LoginState session, int op, byte[] packet) {
        Tms282ChannelTransferRequest request;
        try {
            request = Tms282ChannelTransferRequest.parse(packet);
        } catch (IllegalArgumentException e) {
            rejected(session, op, packet, e);
            return;
        }
        Char chr = session.fieldCharacter();
        Client c = session.client();
        int target = request.serverChannel();
        OptionalInt port = gms265.channelPort(target);
        String refusal = channelRefusal(chr, c, target, port);
        if (refusal != null) {
            refuse(session, chr, request.toString(), refusal, "目前無法切換頻道。");
            return;
        }
        changeChannel(session, request, port.getAsInt(), c.getCh().localAddress());
    }

    /** The move itself: {@code local} is this socket's own endpoint (IPv4); LP001F carries its ChannelAddressPolicy address. */
    void changeChannel(Tms282LoginState session, Tms282ChannelTransferRequest request, int port, SocketAddress local) {
        Char chr = session.fieldCharacter();
        Client c = session.client();
        int target = request.serverChannel();
        if (!(local instanceof InetSocketAddress inet) || inet.getAddress().getAddress().length != 4) {
            refuse(session, chr, request.toString(), "channel endpoint " + local + " is not IPv4", "目前無法切換頻道。");
            return;
        }
        User user = c.getUser();
        states.leave(c, chr);
        MigrationRegistry registry = migrations.get();
        registry.issue(MigrationTicket.tms282(chr.getId(), user, chr.getAccount(), (byte) target, request.wireChannel(), 0, c,
                registry.now()));
        byte[] ip = ChannelAddressPolicy.server().channelAddress(inet);
        OutPacket migrate = Tms282CashShopTranslators.migrateCommand(ip, port);
        session.send(migrate);
        System.out.printf("[TMS282] %s character=%d: ticket issued, SENT LP001F %s:%d (local %s)%n", request, chr.getId(),
                ChannelAddressPolicy.text(ip), port, inet);
    }

    /** The 265 handleUserTransferChannelRequest checks, plus the ones this route needs; null when allowed. */
    static String channelRefusal(Char chr, Client c, int target, OptionalInt port) {
        if (port.isEmpty()) {
            return "no server channel " + target;
        }
        if (c.getChannel() == target) {
            return "already on channel " + target;
        }
        if (chr.getHP() <= 0) {
            return "dead";
        }
        if (chr.getTradeRoom() != null) {
            return "trade window open"; // multiplayer-0930: the escrow must not follow a migration
        }
        Field field = chr.getField();
        if (field == null || (field.getFieldLimit() & FieldOption.MigrateLimit.getVal()) != 0) {
            return "field limits migration";
        }
        if (c.getUser() == null || chr.getAccount() == null) {
            return "no user / account";
        }
        return null;
    }

    // ---- CP015F / CP015E -----------------------------------------------------------------------------------------

    void onPortalScroll(Tms282LoginState session, int op, byte[] packet) {
        Tms282ReturnItemRequest request;
        try {
            request = Tms282ReturnItemRequest.parsePortalScroll(packet);
        } catch (IllegalArgumentException e) {
            rejected(session, op, packet, e);
            return;
        }
        Char chr = session.fieldCharacter();
        delegate(session, chr, request.toString(), () -> gms265.portalScroll(chr, request.toGms265PortalScrollBody()));
    }

    void onMapTransferItem(Tms282LoginState session, int op, byte[] packet) {
        Tms282ReturnItemRequest request;
        try {
            request = Tms282ReturnItemRequest.parseMapTransferItem(packet);
        } catch (IllegalArgumentException e) {
            rejected(session, op, packet, e);
            return;
        }
        Char chr = session.fieldCharacter();
        Item item = chr.getInventoryByType(InvType.CONSUME) == null ? null
                : chr.getInventoryByType(InvType.CONSUME).getItemBySlot(request.slot());
        if (item == null || item.getItemId() != request.itemId() || item.getQuantity() < 1) {
            refuse(session, chr, request.toString(), "no such item in the consume bag", "無法使用該道具。");
            return;
        }
        ItemInfo info = ItemData.getItemInfoByID(request.itemId());
        int moveTo = info == null ? 0 : info.getMoveTo();
        Field field = chr.getField();
        if (moveTo <= 0 || moveTo == 999999999 || field == null
                || (field.getFieldLimit() & FieldOption.PortalScrollLimit.getVal()) != 0) {
            refuse(session, chr, request.toString(), "item has no destination (moveTo=" + moveTo + ") or the field forbids it",
                    "無法在此使用該道具。");
            return;
        }
        delegate(session, chr, request + " -> field " + moveTo, () -> gms265.moveWithItem(chr, item, moveTo));
    }

    // ---- shared ------------------------------------------------------------------------------------------------

    /** Runs {@code gms265} under a {@link Tms282ChangeWatch} and sends its LP008F / LP009D (exclRequest). */
    private void delegate(Tms282LoginState session, Char chr, String summary, Runnable gms265Call) {
        Tms282ChangeWatch watch = Tms282ChangeWatch.of(chr);
        try {
            gms265Call.run();
        } catch (RuntimeException | LinkageError e) {
            session.send(Tms282StatPackets.unlock());
            System.out.println("[TMS282] FAILED " + summary + " character=" + chr.getId() + ": " + e + "; SENT LP009D unlock");
            return;
        }
        String log = watch.describe();
        for (OutPacket p : watch.replies(charset)) {
            session.send(p);
        }
        System.out.println("[TMS282] " + summary + " character=" + chr.getId() + " -> 265; SENT " + log);
    }

    private static void refuse(Tms282LoginState session, Char chr, String summary, String why, String message) {
        session.send(Tms282StatPackets.unlock());
        if (message != null) {
            chr.chatMessage(message);
        }
        System.out.println("[TMS282] REFUSED " + summary + " character=" + chr.getId() + ": " + why + "; SENT LP009D unlock");
    }

    private static void rejected(Tms282LoginState session, int op, byte[] packet, IllegalArgumentException e) {
        session.send(Tms282StatPackets.unlock());
        System.out.printf("[TMS282] REJECTED CP%04X len=%d: %s; SENT LP009D unlock%n", op, packet.length, e.getMessage());
    }
}
