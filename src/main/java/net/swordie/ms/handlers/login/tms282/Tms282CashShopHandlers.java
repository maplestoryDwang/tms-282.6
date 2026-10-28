package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.Account;
import net.swordie.ms.client.Client;
import net.swordie.ms.client.User;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.packet.Tms282CashItemRequest;
import net.swordie.ms.connection.packet.Tms282CashShopPackets;
import net.swordie.ms.connection.packet.Tms282CashShopTranslators;
import net.swordie.ms.connection.packet.Tms282StatPackets;
import net.swordie.ms.enums.CashErrorType;
import net.swordie.ms.enums.FieldOption;
import net.swordie.ms.handlers.CashShopHandler;
import net.swordie.ms.handlers.dispatch.ClientGuard;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.loaders.CashShopData;
import net.swordie.ms.world.field.Field;
import net.swordie.ms.world.migration.MigrationRegistry;
import net.swordie.ms.world.migration.MigrationTicket;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.Charset;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.function.IntPredicate;
import java.util.function.Supplier;

/**
 * TMS 282.3 cash shop on the channel socket, entered in place (as 265: no socket change on the way in):
 * <ul>
 *     <li>CP00FF USER_MIGRATE_TO_CASH_SHOP_REQUEST [static sender 5DD3390: i32 5B4FC50(), u8, u8; 265 reads nothing]
 *     -> {@link States#enter} (265 {@code Client.migrateIn(false)}: out of the field, state IN_CASH_SHOP), then
 *     LP02F1 SET_CASH_SHOP, LP0A75 balances, LP0A76 kind 6 locker. Refused (field limit, dead, no account/trunk):
 *     LP009D unlock and a chat line;</li>
 *     <li>CP0740 CASH_SHOP_QUERY_CASH_REQUEST [2130AF0/2132640, no body] -> 265
 *     {@code CashShopHandler.handleCashShopQueryCashRequest} -> LP0A75;</li>
 *     <li>CP0741 CASH_SHOP_CASH_ITEM_REQUEST ({@link Tms282CashItemRequest}): buy (0x02), inventory -> locker (0x0C),
 *     locker -> inventory (0x0B) become their 265 body and go to 265 {@code CashShopHandler.handleCashShopCashItemRequest},
 *     whose CCashShop replies the profile encodes (LP0A76 15 / 34 / 32 or a failure kind). Any other kind, or a
 *     body that does not parse, is answered with a failure so the shop's pending flag (CCashShop+0x88) clears.
 *     Kind 3 (gift, {@link net.swordie.ms.connection.packet.Tms282CashShopRequests}) -> cs_gift, LP0A76 0x18 / 0x19;
 *     kind 4 (wish list) -> cs_wishlist, LP0A76 0x0D. Entry also delivers pending gifts into the locker and pushes the
 *     wish list (LP0A76 0x0B); LP02F3 carries the blocked SNs and client-visible commodity overrides;</li>
 *     <li>leaving: the shop's exit (216B6F0, log "TransferField Shop -> Channel") sends CP00FB with an empty body
 *     (216B9AE). In the shop that opcode means "back to the field": {@link States#leave} (265 {@code migrateOut}
 *     state), a TMS 282 migration ticket (character + user + world, as LP0014) and LP001F to the address and port of
 *     this very socket, so the client reconnects and enters with CP007E / LP02EF as after a login. The exit is taken
 *     through {@link Tms282TransferHandlers#intercept} when USER_TRANSFER_FIELD_REQUEST is configured as 0x00FB, and
 *     routed here when it is not configured.</li>
 * </ul>
 * Nothing here ends the session.
 */
final class Tms282CashShopHandlers {
    static final int MIGRATE_TO_CASH_SHOP = 0x00FF;
    static final int QUERY_CASH = 0x0740;
    /** The shop's exit request: the transfer-field opcode without a body. */
    static final int LEAVE = 0x00FB;

    /** Channel socket whose field character is in the cash shop (265 Client state IN_CASH_SHOP). */
    static final ClientGuard IN_CASH_SHOP = Tms282Guards.state("IN_CASH_SHOP",
            s -> s.isChannel() && s.fieldCharacter() != null && s.client().getCurrentState() == Client.IN_CASH_SHOP);

    /**
     * The 265 session-state side of entering and leaving (strategy): the formal server uses {@link #gms265()}; offline
     * checks use one that does not touch the database.
     */
    interface States {
        /** Takes {@code chr} out of its field into the cash shop (265 {@code Client.migrateIn(false)}). */
        void enter(Client c, Char chr);

        /** Marks {@code c} as changing channel back into the field (265 {@code Client.migrateOut} without its send). */
        void leave(Client c, Char chr);

        static States gms265() {
            return new States() {
                @Override
                public void enter(Client c, Char chr) {
                    c.migrateIn(false);
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

    /**
     * Persistence-backed shop extras (strategy): gifts (cs_gift), the wish list (cs_wishlist) and the LP02F3 catalog
     * rules. The formal server uses {@link #database()}; offline checks get {@link #none()} (no SQL, the plain LP02F3,
     * no wish-list packet, gifts refused).
     */
    interface Services {
        /** Delivers pending gifts into the locker before it is sent; the number delivered. */
        int deliverGifts(Char chr);

        /** The stored wish list to push on entry (LP0A76 0x0B), or null to send none. */
        int[] wishList(Char chr);

        /** LP02F3 for this entry. */
        OutPacket openCashShop();

        net.swordie.ms.world.shop.cashshop.CashShopGifts.Outcome gift(Char chr, int sn, String receiver, String message);

        void saveWishList(Char chr, int[] list) throws java.sql.SQLException;

        static Services database() {
            return new Services() {
                @Override
                public int deliverGifts(Char chr) {
                    return net.swordie.ms.world.shop.cashshop.CashShopGifts.deliverPending(chr);
                }

                @Override
                public int[] wishList(Char chr) {
                    return net.swordie.ms.world.shop.cashshop.CashShopWishlist.load(chr.getId());
                }

                @Override
                public OutPacket openCashShop() {
                    return openCashShopPacket();
                }

                @Override
                public net.swordie.ms.world.shop.cashshop.CashShopGifts.Outcome gift(Char chr, int sn, String receiver,
                                                                                      String message) {
                    return net.swordie.ms.world.shop.cashshop.CashShopGifts.gift(chr, sn, receiver, message);
                }

                @Override
                public void saveWishList(Char chr, int[] list) throws java.sql.SQLException {
                    net.swordie.ms.world.shop.cashshop.CashShopWishlist.save(chr.getId(), list);
                }
            };
        }

        static Services none() {
            return new Services() {
                @Override
                public int deliverGifts(Char chr) {
                    return 0;
                }

                @Override
                public int[] wishList(Char chr) {
                    return null;
                }

                @Override
                public OutPacket openCashShop() {
                    return Tms282CashShopPackets.openCashShop(net.swordie.ms.util.FileTime.currentTime());
                }

                @Override
                public net.swordie.ms.world.shop.cashshop.CashShopGifts.Outcome gift(Char chr, int sn, String receiver,
                                                                                      String message) {
                    return new net.swordie.ms.world.shop.cashshop.CashShopGifts.Outcome(false,
                            CashErrorType.PleaseTryAgain, null, 0, 0);
                }

                @Override
                public void saveWishList(Char chr, int[] list) throws java.sql.SQLException {
                    throw new java.sql.SQLException("no wish-list store");
                }
            };
        }
    }

    private final Tms282CashShopPackets packets;
    private final Charset charset;
    private final Tms282TransferSettings transfer;
    private final Supplier<MigrationRegistry> migrations;
    private final States states;
    private final IntPredicate isCommodity;
    private final Services services;

    Tms282CashShopHandlers(Tms282LoginContext context, Tms282TransferSettings transfer) {
        this(context.settings().nameCharset(), transfer, context::migrations, States.gms265(),
                sn -> CashShopData.getCommodityBySN(sn) != null
                        && !net.swordie.ms.world.shop.cashshop.CashShopCatalogRules.isBlocked(sn),
                Services.database());
    }

    Tms282CashShopHandlers(Charset charset, Tms282TransferSettings transfer, Supplier<MigrationRegistry> migrations,
                           States states, IntPredicate isCommodity) {
        this(charset, transfer, migrations, states, isCommodity, Services.none());
    }

    Tms282CashShopHandlers(Charset charset, Tms282TransferSettings transfer, Supplier<MigrationRegistry> migrations,
                           States states, IntPredicate isCommodity, Services services) {
        this.packets = new Tms282CashShopPackets(Objects.requireNonNull(charset, "charset"));
        this.charset = charset;
        this.transfer = Objects.requireNonNull(transfer, "transfer");
        this.migrations = Objects.requireNonNull(migrations, "migrations");
        this.states = Objects.requireNonNull(states, "states");
        this.isCommodity = Objects.requireNonNull(isCommodity, "isCommodity");
        this.services = Objects.requireNonNull(services, "services");
    }

    void register(Tms282Routes routes) {
        routes.channel(0x0150, InHeader.USER_CONSUME_CASH_ITEM_USE_REQUEST).when(Tms282Guards.IN_FIELD)
                .handle("owned cash reward box / pet skill -> item handler + inventory sync", this::onUseCashReward);
        // IMPLEMENTED-UNTESTED (2026-09-28 integration): comet 5222214/5222215 sender 5F54BD0, no capture yet.
        routes.channel(COMET_USE, InHeader.CP016F).when(Tms282Guards.IN_FIELD)
                .handle("comet 5222214/5222215 (untested) -> box registry + inventory sync", this::onUseComet);
        routes.channel(net.swordie.ms.connection.packet.Tms282CashUseVariant.CHOICE_OPEN_OPCODE, InHeader.CP02BC).when(Tms282Guards.IN_FIELD).handle("choice beauty coupon -> LP023E style list (untested)", this::onBeautyChoiceOpen);
        routes.channel(MIGRATE_TO_CASH_SHOP, InHeader.USER_MIGRATE_TO_CASH_SHOP_REQUEST).when(Tms282Guards.IN_FIELD)
                .handle("enter cash shop -> LP02F1 + LP0A75 + LP0A76 locker", this::onEnter);
        routes.channel(QUERY_CASH, InHeader.CASH_SHOP_QUERY_CASH_REQUEST).when(IN_CASH_SHOP)
                .handle("query cash -> LP0A75", this::onQueryCash);
        routes.channel(Tms282CashItemRequest.OPCODE, InHeader.CASH_SHOP_CASH_ITEM_REQUEST).when(IN_CASH_SHOP)
                .handle("cash item (buy / locker moves) -> 265 CashShopHandler, LP0A76", this::onCashItem);
        // One route per 265 header: with tms282.transferFieldOp=0x00FB the transfer route takes the exit through
        // exitInterception(); unset, the exit gets its own route; set to another opcode, the exit is unreachable.
        if (transfer.transferFieldOp().isEmpty()) {
            routes.channel(LEAVE, InHeader.USER_TRANSFER_FIELD_REQUEST).when(IN_CASH_SHOP)
                    .handle("leave cash shop -> LP001F", this::onLeave);
        } else if (transfer.transferFieldOp().getAsInt() != LEAVE) {
            System.out.printf("[TMS282] tms282.transferFieldOp=0x%04X is not 0x00FB: the cash shop exit (CP00FB) has no route%n",
                    transfer.transferFieldOp().getAsInt());
        }
    }

    /** The exit taken first by USER_TRANSFER_FIELD_REQUEST on 0x00FB while the socket is in the shop. */
    Tms282TransferHandlers.Interception exitInterception() {
        return new Tms282TransferHandlers.Interception(
                s -> s.isChannel() && s.fieldCharacter() != null && s.client().getCurrentState() == Client.IN_CASH_SHOP,
                this::onLeave);
    }

    // ---- CP00FF ------------------------------------------------------------------------------------------------

    /** CP016F (282 only): comet use. */
    static final int COMET_USE = 0x016F;
    /** CP0150 incubator / raffle body sent by 5F51BA0 (0506xxxx, no key): i32 0, i32 tick, u16 slot, i32 id, i32 0, i32 0. */
    static final int INCUBATOR_LENGTH = 24;

    void onUseCashReward(Tms282LoginState session, int op, byte[] packet) {
        Char chr = session.fieldCharacter();
        // Luna Crystal fusion (44B, research/tms2823/luna-crystal/README.md): inventory ops first, then LP0193,
        // which clears the client's exclusive state itself - no unlock after it.
        if (net.swordie.ms.world.tms282.boxes.LunaCrystalHandler.isFusionPacket(packet)) {
            var lunaWatch = net.swordie.ms.client.character.tms282.Tms282ChangeWatch.of(chr);
            OutPacket result = net.swordie.ms.world.tms282.boxes.LunaCrystalHandler.handleFusion(chr, packet, charset);
            for (OutPacket reply : lunaWatch.replies(charset)) session.send(reply);
            session.send(result);
            return;
        }
        if (isIncubatorPacket(packet)) {
            onUseIncubator(session, chr, packet);
            return;
        }
        if (onUseCashItemFamily(session, chr, packet)) {
            return;
        }
        var watch = net.swordie.ms.client.character.tms282.Tms282ChangeWatch.of(chr);
        net.swordie.ms.client.character.items.PetItem pet = null;
        try {
            var request = net.swordie.ms.connection.packet.Tms282CashUseRequest.parse(packet);
            var item = chr.getCashInventory().getItemBySlot(request.slot());
            if (item == null || item.getItemId() != request.itemId() || item.getQuantity() < 1)
                throw new IllegalArgumentException("Cash item slot changed");
            if (item.getDateExpire() != null && item.getDateExpire().isExpired())
                throw new IllegalArgumentException("Expired cash item");
            if (request.petSn() != 0) {
                var target = chr.getCashInventory().getItemBySN(request.petSn());
                if (!(target instanceof net.swordie.ms.client.character.items.PetItem ownedPet))
                    throw new IllegalArgumentException("Pet not owned");
                pet = ownedPet;
            }
            net.swordie.ms.handlers.item.ItemHandler.handleUserConsumeCashItemUseRequest(chr, new InPacket(request.body()));
        } catch (RuntimeException e) {
            System.out.println("[TMS282] cash use refused: " + e.getClass().getSimpleName());
        } finally {
            for (OutPacket reply : watch.replies(charset)) session.send(reply);
            if (pet != null) session.send(net.swordie.ms.connection.packet.Tms282InventoryPackets.inventoryOperation(true,
                    java.util.List.of(new net.swordie.ms.connection.packet.Tms282InventoryPackets.Add(
                            net.swordie.ms.enums.InvType.CASH, (short) pet.getBagIndex(), pet)), charset));
            session.send(Tms282StatPackets.unlock());
        }
    }

    /**
     * CP0150 families with an emulation-verified 282 layout (teleport rocks, megaphones, pet food, meso bags, beauty
     * coupons, scissors, anvils, weather, ...; research/tms2823/item-usability-20260928/cash-use/README.md) ->
     * {@link net.swordie.ms.world.tms282.items.Tms282CashItemUse}. False when the item's client cash type is not one of
     * them (553 reward boxes and 519 pet skills continue below). Always unlocks when it takes the packet.
     */
    boolean onUseCashItemFamily(Tms282LoginState session, Char chr, byte[] packet) {
        var types = net.swordie.ms.world.tms282.items.Tms282CashItemTypes.get();
        java.util.Optional<net.swordie.ms.connection.packet.Tms282CashUseVariant> variant;
        try {
            variant = net.swordie.ms.world.tms282.items.Tms282CashItemUse.recognize(packet, types, charset);
        } catch (IllegalArgumentException e) {
            System.out.println("[TMS282] CP0150 family body refused: " + e.getMessage() + " "
                    + HexFormat.of().formatHex(packet, 0, Math.min(packet.length, 48)));
            session.send(Tms282StatPackets.unlock());
            return true;
        }
        if (variant.isEmpty()) {
            return false;
        }
        var watch = net.swordie.ms.client.character.tms282.Tms282ChangeWatch.of(chr);
        try {
            net.swordie.ms.world.tms282.items.Tms282CashItemUse.use(chr, variant.get(), packet);
        } catch (RuntimeException e) {
            System.out.println("[TMS282] CP0150 " + variant.get().family() + " failed: " + e);
        } finally {
            for (OutPacket reply : watch.replies(charset)) session.send(reply);
            session.send(Tms282StatPackets.unlock());
        }
        return true;
    }

    /**
     * CP02BC (282 only): a beauty coupon with Item.wz info/choice was double-clicked (5F83D2A). Answer LP023E with the
     * coupon's pool (or its failure result); LP023E's handler 5F86DE0 clears the exclusive flag itself. The pick
     * returns as CP0150 ({@link #onUseCashItemFamily}).
     */
    void onBeautyChoiceOpen(Tms282LoginState session, int op, byte[] packet) {
        Char chr = session.fieldCharacter();
        net.swordie.ms.connection.packet.Tms282CashUseVariant.ChoiceOpen req;
        try {
            req = net.swordie.ms.connection.packet.Tms282CashUseVariant.ChoiceOpen.parse(packet);
        } catch (IllegalArgumentException e) {
            System.out.println("[TMS282] CP02BC refused: " + e.getMessage());
            session.send(Tms282StatPackets.unlock());
            return;
        }
        OutPacket out = OutPacket.forProfile(net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3,
                net.swordie.ms.connection.packet.Tms282CashUseVariant.CHOICE_LIST_OPCODE);
        out.encodeArr(net.swordie.ms.world.tms282.items.Tms282CashItemUse.openChoice(chr, req));
        session.send(out);
    }

    /** 24B CP0150 of an incubator / raffle cash item (id / 10000 == 506) that the box registry owns. */
    static boolean isIncubatorPacket(byte[] packet) {
        if (packet == null || packet.length != INCUBATOR_LENGTH) return false;
        var in = java.nio.ByteBuffer.wrap(packet).order(java.nio.ByteOrder.LITTLE_ENDIAN);
        if (Short.toUnsignedInt(in.getShort(0)) != 0x0150 || in.getInt(2) != 0) return false;
        int id = in.getInt(12);
        return id / 10000 == 506 && in.getInt(16) == 0 && in.getInt(20) == 0
                && net.swordie.ms.world.tms282.boxes.Tms282BoxUse.handles(id);
    }

    /** Incubator / raffle (box-rules README, sender 5F51BA0): the server finds the key itself. */
    void onUseIncubator(Tms282LoginState session, Char chr, byte[] packet) {
        var watch = net.swordie.ms.client.character.tms282.Tms282ChangeWatch.of(chr);
        try {
            var in = java.nio.ByteBuffer.wrap(packet).order(java.nio.ByteOrder.LITTLE_ENDIAN);
            useOwnedBox(chr, Short.toUnsignedInt(in.getShort(10)), in.getInt(12));
        } catch (RuntimeException e) {
            System.out.println("[TMS282] incubator use refused: " + e.getClass().getSimpleName() + " " + e.getMessage());
        } finally {
            for (OutPacket reply : watch.replies(charset)) session.send(reply);
            session.send(Tms282StatPackets.unlock());
        }
    }

    /**
     * CP016F comet 5222214 / 5222215: u16 op, i32 tick, u16 slot, i32 id (12 bytes). IMPLEMENTED-UNTESTED: the
     * layout comes from the static sender 5F54BD0 (box-rules README); the official reply is not captured.
     */
    void onUseComet(Tms282LoginState session, int op, byte[] packet) {
        Char chr = session.fieldCharacter();
        var watch = net.swordie.ms.client.character.tms282.Tms282ChangeWatch.of(chr);
        try {
            if (packet == null || packet.length != 12) throw new IllegalArgumentException("Comet length");
            var in = java.nio.ByteBuffer.wrap(packet).order(java.nio.ByteOrder.LITTLE_ENDIAN);
            int slot = Short.toUnsignedInt(in.getShort(6));
            int id = in.getInt(8);
            if (id != 5222214 && id != 5222215) throw new IllegalArgumentException("Not a comet: " + id);
            useOwnedBox(chr, slot, id);
        } catch (RuntimeException e) {
            System.out.println("[TMS282] comet use refused: " + e.getClass().getSimpleName() + " " + e.getMessage());
        } finally {
            for (OutPacket reply : watch.replies(charset)) session.send(reply);
            session.send(Tms282StatPackets.unlock());
        }
    }

    private static void useOwnedBox(Char chr, int slot, int itemId) {
        if (slot <= 0 || slot > Short.MAX_VALUE) throw new IllegalArgumentException("Box slot");
        var item = chr.getCashInventory().getItemBySlot(slot);
        if (item == null || item.getItemId() != itemId || item.getQuantity() < 1)
            throw new IllegalArgumentException("Cash item slot changed");
        if (item.getDateExpire() != null && item.getDateExpire().isExpired())
            throw new IllegalArgumentException("Expired cash item");
        if (!net.swordie.ms.world.tms282.boxes.Tms282BoxUse.tryUse(chr, item))
            throw new IllegalArgumentException("No box handler for " + itemId);
    }

    void onEnter(Tms282LoginState session, int op, byte[] packet) {
        Char chr = session.fieldCharacter();
        Client c = session.client();
        String refusal = refusal(chr);
        if (refusal != null) {
            System.out.printf("[TMS282] CP00FF cash shop refused for character %d: %s (body %s)%n", chr.getId(),
                    refusal, HexFormat.of().formatHex(packet));
            chr.chatMessage("現在無法進入購物商城。");
            session.send(Tms282StatPackets.unlock());
            return;
        }
        states.enter(c, chr);
        int gifts = 0;
        try {
            gifts = services.deliverGifts(chr);
        } catch (RuntimeException e) {
            System.out.printf("[TMS282] gift delivery failed for character %d: %s%n", chr.getId(), e);
        }
        session.send(packets.setCashShop(chr));
        session.send(services.openCashShop());
        session.send(packets.queryCashResult(chr));
        session.send(packets.loadLockerDone(chr.getAccount()));
        int[] wish = null;
        try {
            wish = services.wishList(chr);
        } catch (RuntimeException e) {
            System.out.printf("[TMS282] wish list load failed for character %d: %s%n", chr.getId(), e);
        }
        if (wish != null) session.send(Tms282CashShopPackets.loadWishDone(wish));
        System.out.printf("[TMS282] CP00FF character %d entered the cash shop; SENT LP02F1 + LP02F3 + LP0A75 + LP0A76 locker (%d, %d gift(s) delivered) + LP0A76 wish %s%n",
                chr.getId(), chr.getAccount().getTrunk().getLocker().size(), gifts,
                net.swordie.ms.world.shop.cashshop.CashShopWishlist.toString(wish));
    }

    /**
     * LP02F3 with the blocked SNs and client-visible commodity overrides of data/resources/tms282/cashshop.
     * {@code -Dtms282.cashshop.hideBlocked=false} / {@code -Dtms282.cashshop.sendModifies=false} fall back to empty
     * lists (the server-side refusal and pricing stay in force either way).
     */
    static OutPacket openCashShopPacket() {
        var now = net.swordie.ms.util.FileTime.currentTime();
        try {
            var rules = net.swordie.ms.world.shop.cashshop.CashShopCatalogRules.get();
            java.util.List<Integer> blocked = Boolean.parseBoolean(System.getProperty("tms282.cashshop.hideBlocked", "true"))
                    ? java.util.List.copyOf(rules.blocked()) : java.util.List.of();
            java.util.List<net.swordie.ms.world.shop.cashshop.CashShopCatalogRules.Modify> modifies =
                    Boolean.parseBoolean(System.getProperty("tms282.cashshop.sendModifies", "true"))
                            ? rules.modifies().values().stream()
                            .filter(m -> m.clientMask() != 0 && CashShopData.getCommodityBySN(m.sn()) != null).toList()
                            : java.util.List.of();
            return Tms282CashShopPackets.openCashShop(now, blocked, modifies);
        } catch (RuntimeException e) {
            System.out.println("[TMS282] cash shop catalog rules not sent: " + e);
            return Tms282CashShopPackets.openCashShop(now);
        }
    }

    /** The 265 entry checks, and the objects the shop packets need. Null when the shop may open. */
    static String refusal(Char chr) {
        if (!net.swordie.ms.GameSettings.cashShopEnabled()) return "feature.cashshop=false";
        if (chr.getClient() != null && chr.getClient().getCurrentState() == Client.IN_CASH_SHOP) {
            return "already in the cash shop";
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
        Account account = chr.getAccount();
        if (chr.getUser() == null || account == null || account.getTrunk() == null) {
            return "no user / account / trunk";
        }
        return null;
    }

    // ---- CP0740 ------------------------------------------------------------------------------------------------

    void onQueryCash(Tms282LoginState session, int op, byte[] packet) {
        Char chr = session.fieldCharacter();
        guarded(session, chr, "query cash", () -> CashShopHandler.handleCashShopQueryCashRequest(session.client(),
                new InPacket(new byte[0])));
    }

    // ---- CP0741 ------------------------------------------------------------------------------------------------

    void onCashItem(Tms282LoginState session, int op, byte[] packet) {
        Char chr = session.fieldCharacter();
        Tms282CashItemRequest request;
        try {
            request = Tms282CashItemRequest.parse(packet);
        } catch (IllegalArgumentException e) {
            System.out.printf("[TMS282] REJECTED CP0741 len=%d: %s%n", packet.length, e.getMessage());
            session.send(Tms282CashShopPackets.failed(Tms282CashShopPackets.Kind.GENERIC_FAILED,
                    Tms282CashShopPackets.clientReason(CashErrorType.PleaseTryAgain)));
            return;
        }
        if (request.kind() == 0x2F) {
            try {
                int[] query=request.purchaseCountQuery();
                var item=CashShopData.getCommodityBySN(query[0]);
                int count=item==null?Integer.MAX_VALUE:net.swordie.ms.world.shop.cashshop.CashShopPurchaseCounts.count(chr.getUser().getId(),item);
                session.send(Tms282CashShopPackets.purchaseCount(query[0],query[1],count,net.swordie.ms.util.FileTime.currentTime()));
                System.out.printf("[TMS282] cash purchase-count sn=%d scope=%d count=%d -> LP0165%n",query[0],query[1],count);
            } catch (IllegalArgumentException | java.sql.SQLException e) {
                // A read-only availability query must never be reported as a failed purchase.
                System.out.printf("[TMS282] purchase-count query unavailable: %s%n",e.getClass().getSimpleName());
            }
            return;
        }
        if (request.kind() == net.swordie.ms.connection.packet.Tms282CashShopRequests.GIFT) {
            onGift(session, chr, request);
            return;
        }
        if (request.kind() == net.swordie.ms.connection.packet.Tms282CashShopRequests.SET_WISH) {
            onSetWish(session, chr, request);
            return;
        }
        if(request.kind()==5 || request.kind()==6) {
            boolean storage=request.kind()==6;
            try {
                var expansion=request.expansion();
                var result=new net.swordie.ms.client.character.tms282.Tms282CashExpansionService().buy(chr,expansion,
                        net.swordie.ms.world.shop.cashshop.CashShopCatalogRules.isBlocked(expansion.commoditySn()) ? null
                                : CashShopData.getCommodityBySN(expansion.commoditySn()));
                if(result.paid())session.send(Tms282CashShopPackets.expansionDone(storage,result.inventoryType(),result.slots()));
                else {
                    if(!storage)session.send(net.swordie.ms.connection.packet.Tms282InventoryArrangeTranslators.grow(result.inventoryType(),result.slots()));
                    session.send(Tms282CashShopPackets.expansionFailed(storage,CashErrorType.PleaseTryAgain));
                }
                session.send(packets.queryCashResult(chr));
                if(net.swordie.ms.client.character.tms282.Tms282CashExpansionService.isBalanceUncertain(chr.getUser()))session.client().close();
            }catch(RuntimeException e) {
                System.out.printf("[TMS282] cash capacity refused character=%d: %s%n",chr.getId(),e.getClass().getSimpleName());
                session.send(Tms282CashShopPackets.expansionFailed(storage,CashErrorType.PleaseTryAgain));
                if(net.swordie.ms.client.character.tms282.Tms282CashExpansionService.isBalanceUncertain(chr.getUser()))session.client().close();
            }
            return;
        }
        Optional<byte[]> body;
        try {
            body = request.toGms265Body(isCommodity);
        } catch (IllegalArgumentException e) {
            System.out.printf("[TMS282] REJECTED %s: %s%n", request, e.getMessage());
            session.send(packets.failed(request.failureType(), CashErrorType.PleaseTryAgain));
            return;
        }
        if (body.isEmpty()) {
            System.out.printf("[TMS282] %s not ported; answered with a failure%n", request);
            session.send(packets.failed(request.failureType(), CashErrorType.PleaseTryAgain));
            return;
        }
        System.out.println("[TMS282] " + request + " -> 265 CashShopHandler");
        guarded(session, chr, request.toString(), () -> CashShopHandler.handleCashShopCashItemRequest(session.client(),
                new InPacket(body.get())));
    }

    /** CP0741 kind 3: buy a commodity for another character (cs_gift; delivered on the receiver's next shop entry). */
    void onGift(Tms282LoginState session, Char chr, Tms282CashItemRequest request) {
        net.swordie.ms.connection.packet.Tms282CashShopRequests.Gift gift;
        try {
            gift = net.swordie.ms.connection.packet.Tms282CashShopRequests.gift(request.body(), charset);
        } catch (IllegalArgumentException e) {
            System.out.printf("[TMS282] REJECTED %s: %s%n", request, e.getMessage());
            session.send(Tms282CashShopPackets.giftFailed(CashErrorType.PleaseTryAgain));
            return;
        }
        net.swordie.ms.world.shop.cashshop.CashShopGifts.Outcome outcome;
        try {
            outcome = services.gift(chr, gift.sn(), gift.receiver(), gift.message());
        } catch (RuntimeException e) {
            System.out.printf("[TMS282] FAILED cash gift character=%d: %s%n", chr.getId(), e);
            outcome = new net.swordie.ms.world.shop.cashshop.CashShopGifts.Outcome(false, CashErrorType.PleaseTryAgain,
                    null, 0, 0);
        }
        System.out.printf("[TMS282] CP0741 gift sn=%d option=%d offer=%d to '%s' -> %s%n", gift.sn(), gift.option(),
                gift.selectedOffer(), gift.receiver(), outcome.done() ? "done" : outcome.reason());
        session.send(outcome.done() ? packets.giftDone(outcome.receiverName(), outcome.itemId(), outcome.count())
                : Tms282CashShopPackets.giftFailed(outcome.reason()));
        session.send(packets.queryCashResult(chr));
    }

    /** CP0741 kind 4: add (mode 0) / remove (mode 1) a wish-list SN; answered with LP0A76 0x0D and the stored list. */
    void onSetWish(Tms282LoginState session, Char chr, Tms282CashItemRequest request) {
        int[] stored = null;
        try {
            stored = services.wishList(chr);
        } catch (RuntimeException e) {
            System.out.printf("[TMS282] wish list load failed for character %d: %s%n", chr.getId(), e);
        }
        if (stored == null) stored = new int[net.swordie.ms.world.shop.cashshop.CashShopWishlist.SIZE];
        try {
            var wish = net.swordie.ms.connection.packet.Tms282CashShopRequests.setWish(request.body());
            int[] next = net.swordie.ms.world.shop.cashshop.CashShopWishlist.apply(wish.list(), wish.add(), wish.sn(),
                    isCommodity);
            services.saveWishList(chr, next);
            stored = next;
            System.out.printf("[TMS282] CP0741 %s -> saved %s%n", wish,
                    net.swordie.ms.world.shop.cashshop.CashShopWishlist.toString(next));
        } catch (IllegalArgumentException | java.sql.SQLException e) {
            System.out.printf("[TMS282] wish list change refused character=%d: %s%n", chr.getId(), e.getMessage());
        }
        // Always answered (the shop's pending flag); on refusal with the unchanged stored list.
        session.send(Tms282CashShopPackets.setWishDone(stored));
    }

    // ---- leave (CP00FB in the shop) ----------------------------------------------------------------------------

    void onLeave(Tms282LoginState session, int op, byte[] packet) {
        SocketAddress local = session.client().getCh().localAddress();
        if (!(local instanceof InetSocketAddress inet) || inet.getAddress().getAddress().length != 4) {
            System.out.printf("[TMS282] CP%04X leave cash shop: channel endpoint %s is not IPv4; kept in the shop%n", op, local);
            return;
        }
        leave(session, op, inet);
    }

    /** Back to the field through {@code inet} (this socket's own endpoint; LP001F carries the ChannelAddressPolicy address). */
    void leave(Tms282LoginState session, int op, InetSocketAddress inet) {
        Char chr = session.fieldCharacter();
        Client c = session.client();
        User user = c.getUser();
        Account account = chr.getAccount();
        states.leave(c, chr);
        MigrationRegistry registry = migrations.get();
        // As LP0014 (Tms282CharacterHandlers.sendSelectSuccess): character + user + world; wire channel = server - 1.
        registry.issue(MigrationTicket.tms282(chr.getId(), user, account, c.getChannel(), Math.max(0, c.getChannel() - 1),
                0, c, registry.now()));
        byte[] ip = ChannelAddressPolicy.server().channelAddress(inet);
        OutPacket migrate = Tms282CashShopTranslators.migrateCommand(ip, inet.getPort());
        session.send(migrate);
        System.out.printf("[TMS282] CP%04X character %d leaves the cash shop; SENT LP001F %s:%d (local %s, ticket issued)%n", op,
                chr.getId(), ChannelAddressPolicy.text(ip), inet.getPort(), inet);
    }

    // ---- shared ------------------------------------------------------------------------------------------------

    /** Runs a 265 call; a failure is logged and answered so the shop does not stay pending. */
    private void guarded(Tms282LoginState session, Char chr, String what, Runnable call) {
        try {
            call.run();
        } catch (RuntimeException e) {
            System.out.printf("[TMS282] FAILED cash shop %s character=%d: %s%n", what, chr == null ? -1 : chr.getId(), e);
            session.send(Tms282CashShopPackets.failed(Tms282CashShopPackets.Kind.GENERIC_FAILED,
                    Tms282CashShopPackets.clientReason(CashErrorType.PleaseTryAgain)));
        }
    }
}
