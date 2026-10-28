package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.client.character.tms282.Tms282InventoryDiff;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.packet.FieldPacket;
import net.swordie.ms.connection.packet.Tms282InventoryPackets;
import net.swordie.ms.connection.packet.Tms282StatPackets;
import net.swordie.ms.connection.packet.Tms282TamingMobFoodRequest;
import net.swordie.ms.enums.InvType;
import net.swordie.ms.handlers.header.InHeader;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Taming mob (mount) food on the 282 overlay (social phase 3). 265 has no handler for
 * USER_TAMING_MOB_FOOD_ITEM_USE_REQUEST, so the rule is implemented here:
 * <ul>
 *     <li>CP014D ({@link Tms282TamingMobFoodRequest}): the item at the CONSUME slot must be the requested 226xxxx
 *     food; one is consumed and the mount fatigue drops by {@value #FATIGUE_PER_FOOD} (not below 0) [推定: the 282
 *     item data has no incFatigue value in the overlay; 30 is the classic value]. Replies: LP008F (the CONSUME change,
 *     exclRequest) and SET_TAMING_MOB_INFO (265 body, passes as LP00BD) with the new fatigue.</li>
 *     <li>any refusal (wrong slot/item, not a food) and any failure: LP009D unlock, since the sender locked the
 *     client (5E10725).</li>
 * </ul>
 */
public final class Tms282TamingMobHandlers {
    static final int FATIGUE_PER_FOOD = 30;

    private final Charset charset;

    Tms282TamingMobHandlers(Charset charset) {
        this.charset = Objects.requireNonNull(charset, "charset");
    }

    void register(Tms282Routes routes) {
        routes.channel(Tms282TamingMobFoodRequest.OPCODE, InHeader.USER_TAMING_MOB_FOOD_ITEM_USE_REQUEST)
                .when(Tms282Guards.IN_FIELD)
                .handle("taming mob food -> fatigue, LP008F + LP00BD (265 has no handler)", this::onFood);
    }

    private void onFood(Tms282LoginState session, int op, byte[] packet) {
        Tms282TamingMobFoodRequest request;
        try {
            request = Tms282TamingMobFoodRequest.parse(packet);
        } catch (IllegalArgumentException e) {
            session.send(Tms282StatPackets.unlock());
            System.out.printf("[TMS282] REJECTED CP%04X len=%d: %s; SENT LP009D unlock%n", op, packet.length, e.getMessage());
            return;
        }
        Char chr = session.fieldCharacter();
        List<OutPacket> replies;
        try {
            replies = feed(chr, request, charset);
        } catch (RuntimeException | LinkageError e) {
            session.send(Tms282StatPackets.unlock());
            System.out.println("[TMS282] FAILED " + request + ": " + e + "; SENT LP009D unlock");
            return;
        }
        for (OutPacket p : replies) {
            session.send(p);
        }
        System.out.println("[TMS282] " + request + " character=" + chr.getId() + " fatigue=" + chr.getTamingMobFatigue()
                + " -> " + (replies.size() == 1 ? "refused, LP009D unlock" : "LP008F + LP00BD"));
    }

    /** The replies of one CP014D: the LP008F CONSUME change and the taming mob info, or only an unlock. */
    public static List<OutPacket> feed(Char chr, Tms282TamingMobFoodRequest request, Charset charset) {
        Item item = chr.getInventoryByType(InvType.CONSUME).getItemBySlot(request.slot());
        if (item == null || item.getItemId() != request.itemId() || request.itemId() / 10000 != 226) {
            return List.of(Tms282StatPackets.unlock());
        }
        Tms282InventoryDiff consume = Tms282InventoryDiff.of(InvType.CONSUME, chr.getInventoryByType(InvType.CONSUME));
        chr.consumeItem(item, 1);
        chr.setTamingMobFatigue(Math.max(0, chr.getTamingMobFatigue() - FATIGUE_PER_FOOD));
        List<OutPacket> out = new ArrayList<>(2);
        List<Tms282InventoryPackets.Operation> changes = consume.changes();
        out.add(changes.isEmpty() ? Tms282StatPackets.unlock()
                : Tms282InventoryPackets.inventoryOperation(true, changes, charset));
        out.add(FieldPacket.setTamingMobInfo(chr.getId(), chr.getTamingMobLevel(), chr.getTamingMobExp(),
                chr.getTamingMobFatigue(), true));
        return List.copyOf(out);
    }
}
