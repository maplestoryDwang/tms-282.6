package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.client.character.items.PetItem;
import net.swordie.ms.client.character.tms282.Tms282ChangeWatch;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.packet.Tms282InventoryPackets;
import net.swordie.ms.connection.packet.Tms282PetPackets;
import net.swordie.ms.connection.packet.Tms282PetRequests;
import net.swordie.ms.enums.FieldOption;
import net.swordie.ms.life.pet.Pet;
import net.swordie.ms.enums.InvType;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.handlers.life.PetHandler;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * Pet requests of TMS 282.3 on the channel socket after LP02EF: each request is rewritten as its 265 body
 * ({@link Tms282PetRequests}) and handed to the unchanged 265 {@link PetHandler}. The 265 pet packets reach the
 * client through {@link net.swordie.ms.connection.packet.Tms282PetPackets} (LP0441..LP0446); the 265 inventory
 * updates have no 282 form, so the requests that change bags reply with the 282 diff ({@link Tms282ChangeWatch}),
 * and activation re-sends the pet item (its active state is not a bag change the diff can see). A 265 EXCL_REQUEST
 * becomes the request-scoped LP009D unlock (Tms282UnlockScope).
 */
final class Tms282PetHandlers {
    /** One pet request: the opcode, its 265 header and handler, and whether its bag changes are answered. */
    private record PetRoute(int opcode, InHeader header, BiConsumer<Char, InPacket> handler, boolean bagReply,
                            String description) {
    }

    private static final List<PetRoute> ROUTES = List.of(
            new PetRoute(Tms282PetRequests.ACTIVATE, InHeader.USER_ACTIVATE_PET_REQUEST,
                    PetHandler::handleUserActivatePetRequest, true, "activate pet -> 265 PetHandler (LP0441, LP008F)"),
            new PetRoute(Tms282PetRequests.FOOD, InHeader.USER_PET_FOOD_ITEM_USE_REQUEST,
                    PetHandler::handleUserPetFoodItemUseRequest, true, "pet food -> 265 PetHandler (LP008F)"),
            new PetRoute(Tms282PetRequests.ACTION, InHeader.PET_ACTION, PetHandler::handlePetAction, false,
                    "pet action -> 265 PetHandler (LP0443)"),
            new PetRoute(Tms282PetRequests.INTERACTION, InHeader.PET_INTERACTION_REQUEST,
                    PetHandler::handlePetInteractionRequest, true, "pet interaction -> 265 PetHandler (LP044A)"),
            new PetRoute(Tms282PetRequests.PICK_UP, InHeader.PET_DROP_PICK_UP_REQUEST,
                    PetHandler::handlePetDropPickUpRequest, true, "pet pick-up -> 265 PetHandler (LP008F, LP074E)"),
            new PetRoute(Tms282PetRequests.STAT_CHANGE_ITEM, InHeader.PET_STAT_CHANGE_ITEM_USE_REQUEST,
                    PetHandler::handlePetStatChangeItemUseRequest, true, "pet potion -> 265 PetHandler (LP008F)"),
            new PetRoute(Tms282PetRequests.OPEN_SHOP, InHeader.PET_OPEN_SHOP, PetHandler::handlePetOpenShop, false,
                    "pet shop -> 265 PetHandler"));

    private final Charset charset;

    Tms282PetHandlers(Charset charset) {
        this.charset = Objects.requireNonNull(charset, "charset");
    }

    void register(Tms282Routes routes) {
        for (PetRoute r : ROUTES) {
            routes.channel(r.opcode(), r.header()).when(Tms282Guards.IN_FIELD)
                    .handle(r.description(), (session, op, packet) -> handle(r, session, op, packet));
        }
        routes.channel(Tms282PetRequests.MOVE, InHeader.PET_MOVE).when(Tms282Guards.IN_FIELD)
                .handle("pet move [推定 opcode] -> pet position + LP0442 to the field", this::move);
    }

    /**
     * CP02D6: the 265 PetHandler.handlePetMove steps (NoPet field, pet by index, path applied, broadcast to the others)
     * with the 282 path kept as parsed, so LP0442 carries the client's own 282 elements.
     */
    private void move(Tms282LoginState session, int op, byte[] packet) {
        Tms282PetRequests.Move request;
        try {
            request = Tms282PetRequests.Move.parse(packet);
        } catch (IllegalArgumentException e) {
            Tms282SocialHandlers.rejected(op, packet, e);
            return;
        }
        Char chr = session.fieldCharacter();
        Tms282SocialHandlers.guarded(chr, op, () -> {
            if ((chr.getField().getFieldLimit() & FieldOption.NoPet.getVal()) > 0) {
                return;
            }
            Pet pet = chr.getPetByIdx(request.idx());
            if (pet != null) {
                request.path().applyTo(pet);
                chr.getField().broadcast(Tms282PetPackets.move(chr.getId(), request.idx(), request.path()), chr);
            }
        });
    }

    private void handle(PetRoute route, Tms282LoginState session, int op, byte[] packet) {
        byte[] body;
        try {
            body = Tms282PetRequests.toGms265Body(op, packet, charset);
        } catch (IllegalArgumentException e) {
            Tms282SocialHandlers.rejected(op, packet, e);
            return;
        }
        Char chr = session.fieldCharacter();
        Tms282ChangeWatch watch = route.bagReply() ? Tms282ChangeWatch.of(chr) : null;
        Tms282SocialHandlers.guarded(chr, op, () -> route.handler().accept(chr, new InPacket(body)));
        if (watch == null) {
            return;
        }
        List<Tms282InventoryPackets.Operation> ops = new java.util.ArrayList<>(watch.operations());
        if (op == Tms282PetRequests.ACTIVATE) {
            petItemAt(chr, (short) ((body[4] & 0xFF) | (body[5] & 0xFF) << 8))
                    .ifPresent(ops::add);
        }
        if (op == Tms282PetRequests.FOOD || op == Tms282PetRequests.INTERACTION) {
            for (Pet pet : chr.getPets()) {
                PetItem item = pet.getItem();
                ops.add(new Tms282InventoryPackets.Add(item.getInvType(), (short) item.getBagIndex(), item));
            }
        }
        if (!ops.isEmpty()) {
            session.send(Tms282InventoryPackets.inventoryOperation(true, ops, charset));
        }
        for (OutPacket p : watch.replies(charset)) {
            if (p.getData().length >= 2 && ((p.getData()[0] & 0xFF) | (p.getData()[1] & 0xFF) << 8)
                    != Tms282InventoryPackets.INVENTORY_OPERATION) {
                session.send(p);
            }
        }
        System.out.printf("[TMS282] CP%04X pet request: %d bag operation(s)%n", op, ops.size());
    }

    /** The pet item at {@code slot} (cash bag, else consume, as 265 looks it up), re-sent in place. */
    private static java.util.Optional<Tms282InventoryPackets.Operation> petItemAt(Char chr, short slot) {
        for (InvType type : new InvType[]{InvType.CASH, InvType.CONSUME}) {
            Item item = chr.getInventoryByType(type).getItemBySlot(slot);
            if (item instanceof PetItem) {
                return java.util.Optional.of(new Tms282InventoryPackets.Add(type, slot, item));
            }
        }
        return java.util.Optional.empty();
    }
}
