package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.Inventory;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.client.character.tms282.Tms282InventoryWatch;
import net.swordie.ms.client.character.tms282.Tms282StatDiff;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.packet.Tms282InventoryArrangeRequest;
import net.swordie.ms.connection.packet.Tms282InventoryPackets;
import net.swordie.ms.connection.packet.Tms282ItemMiscRequest;
import net.swordie.ms.connection.packet.Tms282ReviveRequest;
import net.swordie.ms.connection.packet.Tms282StatPackets;
import net.swordie.ms.enums.InvType;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.handlers.item.InventoryHandler;
import net.swordie.ms.handlers.item.ItemHandler;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Inventory arrange buttons, small item requests and the special-field revive (282 overlay):
 * <ul>
 *     <li>CP013F gather / CP0140 sort ({@link Tms282InventoryArrangeRequest}) -> 265 InventoryHandler. 265 answers
 *     with INVENTORY_OPERATION Move / Remove+Add packets (dropped: no 282 form) and GATHER/SORT_ITEM_RESULT
 *     (translated, {@code Tms282InventoryArrangeTranslators}); the 282 reply is one LP008F (exclRequest) that removes
 *     every moved item from its old slot and adds it at its new one - removes first, so no step lands on a slot that
 *     is still taken (the client applies the operations in order);</li>
 *     <li>CP0182 lock sort -> 265; LP008F re-adds the item over its own slot (its encoding carries the lock flag);</li>
 *     <li>CP015A character-slot item -> 265 (account slot +1); LP008F of the consumed item;</li>
 *     <li>CP0128 tomb effect -> 265 (field broadcast of REMOTE_SHOW_UPGRADE_TOMB_EFFECT, translated to LP048B);</li>
 *     <li>CP02A7 death-window answer on special fields ({@link Tms282ReviveRequest#parseSpecial}): in-place revive
 *     with a revive item, else 265 {@code Char.revive} on the current field (instance / death-count rules).</li>
 * </ul>
 * Every path ends with an unlock-carrying packet (LP008F or LP009D exclRequest).
 */
final class Tms282InventoryArrangeHandlers {
    private final Charset charset;
    private final Tms282ReviveService revive;

    Tms282InventoryArrangeHandlers(Tms282LoginContext context) {
        this.charset = Objects.requireNonNull(context, "context").settings().nameCharset();
        this.revive = new Tms282ReviveService(charset);
    }

    void register(Tms282Routes routes) {
        routes.channel(Tms282InventoryArrangeRequest.GATHER, InHeader.USER_GATHER_ITEM_REQUEST).when(Tms282Guards.IN_FIELD)
                .handle("gather items -> 265, LP008F moves", (s, op, p) -> arrange(s, p, false));
        routes.channel(Tms282InventoryArrangeRequest.SORT, InHeader.USER_SORT_ITEM_REQUEST).when(Tms282Guards.IN_FIELD)
                .handle("sort items -> 265, LP008F moves", (s, op, p) -> arrange(s, p, true));
        routes.channel(Tms282ItemMiscRequest.LOCK_SORT, InHeader.USER_LOCK_SORT_ITEM_REQUEST).when(Tms282Guards.IN_FIELD)
                .handle("lock item against sorting -> 265, LP008F refresh", this::lockSort);
        routes.channel(Tms282ItemMiscRequest.CHAR_SLOT_INC, InHeader.USER_CHAR_SLOT_INC_ITEM_USE_REQUEST)
                .when(Tms282Guards.IN_FIELD).handle("character slot item -> 265, LP008F", this::charSlotInc);
        routes.channel(Tms282ItemMiscRequest.TOMB_EFFECT, InHeader.USER_UPGRADE_TOMB_EFFECT).when(Tms282Guards.IN_FIELD)
                .handle("tomb effect -> 265 broadcast (LP048B)", this::tombEffect);
        routes.channel(Tms282ReviveRequest.SPECIAL_FIELD_REVIVE, InHeader.CP02A7).when(Tms282Guards.IN_FIELD)
                .handle("death window on a special field -> revive", this::specialRevive);
    }

    // ---- CP013F / CP0140 ---------------------------------------------------------------------------------------

    private void arrange(Tms282LoginState session, byte[] packet, boolean sort) {
        Char chr = session.fieldCharacter();
        Tms282InventoryArrangeRequest request;
        try {
            request = sort ? Tms282InventoryArrangeRequest.parseSort(packet) : Tms282InventoryArrangeRequest.parseGather(packet);
        } catch (IllegalArgumentException e) {
            refuse(session, packet, e);
            return;
        }
        InvType type = InvType.getInvTypeByVal(request.invType());
        if (type == null || type == InvType.EQUIPPED) {
            System.out.printf("[TMS282] %s character=%d: no such inventory; SENT LP009D unlock%n", request, chr.getId());
            session.send(Tms282StatPackets.unlock());
            return;
        }
        Map<Item, Short> before = positions(chr.getInventoryByType(type));
        try {
            if (sort) {
                InventoryHandler.handleUserSortItemRequest(chr, new InPacket(request.toGms265Body()));
            } else {
                InventoryHandler.handleUserGatherItemRequest(chr, new InPacket(request.toGms265Body()));
            }
        } catch (RuntimeException | LinkageError e) {
            System.out.printf("[TMS282] FAILED %s character=%d: %s%n", request, chr.getId(), e);
        }
        List<Tms282InventoryPackets.Operation> ops = relocations(type, before, chr.getInventoryByType(type));
        session.send(ops.isEmpty() ? Tms282StatPackets.unlock() : Tms282InventoryPackets.inventoryOperation(true, ops, charset, true));
        System.out.printf("[TMS282] %s character=%d -> 265; SENT %s%n", request, chr.getId(),
                ops.isEmpty() ? "LP009D unlock (nothing moved)" : "LP008F " + ops.size() + " op(s)");
    }

    private static Map<Item, Short> positions(Inventory inventory) {
        Map<Item, Short> m = new IdentityHashMap<>();
        for (Item item : new ArrayList<>(inventory.getItems())) {
            if (item != null) {
                m.put(item, (short) item.getBagIndex());
            }
        }
        return m;
    }

    /** Remove at the old slot for every item that moved, then Add at its new slot (same order as 265 sort). */
    static List<Tms282InventoryPackets.Operation> relocations(InvType type, Map<Item, Short> before, Inventory after) {
        List<Tms282InventoryPackets.Operation> removes = new ArrayList<>();
        List<Tms282InventoryPackets.Operation> adds = new ArrayList<>();
        List<Item> items = new ArrayList<>(after.getItems());
        items.sort(java.util.Comparator.comparingInt(Item::getBagIndex));
        for (Item item : items) {
            Short old = before.get(item);
            if (old != null && old != item.getBagIndex()) {
                removes.add(new Tms282InventoryPackets.Remove(type, old));
                adds.add(new Tms282InventoryPackets.Add(type, (short) item.getBagIndex(), item));
            }
        }
        removes.addAll(adds);
        return removes;
    }

    // ---- CP0182 ------------------------------------------------------------------------------------------------

    private void lockSort(Tms282LoginState session, int op, byte[] packet) {
        Char chr = session.fieldCharacter();
        Tms282ItemMiscRequest request;
        try {
            request = Tms282ItemMiscRequest.parseLockSort(packet);
        } catch (IllegalArgumentException e) {
            refuse(session, packet, e);
            return;
        }
        InvType type = InvType.getInvTypeByVal(request.i32(4));
        Item item = type == null ? null : chr.getInventoryByType(type).getItemBySlot(request.i32(8));
        guarded(chr, request.toString(), () -> InventoryHandler.handleLockSortItemRequest(chr, new InPacket(request.body())));
        if (item == null) {
            session.send(Tms282StatPackets.unlock());
            return;
        }
        session.send(Tms282InventoryPackets.inventoryOperation(true,
                List.of(new Tms282InventoryPackets.Add(type, (short) item.getBagIndex(), item)), charset));
        System.out.printf("[TMS282] %s character=%d -> 265; SENT LP008F refresh of slot %d%n", request, chr.getId(),
                item.getBagIndex());
    }

    // ---- CP015A ------------------------------------------------------------------------------------------------

    private void charSlotInc(Tms282LoginState session, int op, byte[] packet) {
        Char chr = session.fieldCharacter();
        Tms282ItemMiscRequest request;
        try {
            request = Tms282ItemMiscRequest.parseCharSlotInc(packet);
        } catch (IllegalArgumentException e) {
            refuse(session, packet, e);
            return;
        }
        Tms282InventoryWatch inventory = Tms282InventoryWatch.of(chr);
        guarded(chr, request.toString(), () -> ItemHandler.handleUserCharSlotIncItemUseRequest(chr, new InPacket(request.body())));
        List<Tms282InventoryPackets.Operation> ops = inventory.changes();
        session.send(ops.isEmpty() ? Tms282StatPackets.unlock() : Tms282InventoryPackets.inventoryOperation(true, ops, charset));
    }

    // ---- CP0128 ------------------------------------------------------------------------------------------------

    private void tombEffect(Tms282LoginState session, int op, byte[] packet) {
        Char chr = session.fieldCharacter();
        Tms282ItemMiscRequest request;
        try {
            request = Tms282ItemMiscRequest.parseTombEffect(packet);
        } catch (IllegalArgumentException e) {
            System.out.printf("[TMS282] IGNORED CP0128 len=%d: %s%n", packet.length, e.getMessage());
            return;
        }
        guarded(chr, request.toString(), () -> ItemHandler.handleUserUpgradeTombEffect(chr, new InPacket(request.body())));
    }

    // ---- CP02A7 ------------------------------------------------------------------------------------------------

    private void specialRevive(Tms282LoginState session, int op, byte[] packet) {
        Char chr = session.fieldCharacter();
        Tms282ReviveRequest request;
        try {
            request = Tms282ReviveRequest.parseSpecial(packet);
        } catch (IllegalArgumentException e) {
            refuse(session, packet, e);
            return;
        }
        if (chr.getHP() > 0) {
            System.out.printf("[TMS282] %s character=%d is alive; SENT LP009D unlock%n", request, chr.getId());
            session.send(Tms282StatPackets.unlock());
            return;
        }
        List<OutPacket> inPlace = revive.reviveInPlace(chr, request);
        if (!inPlace.isEmpty()) {
            inPlace.forEach(session::send);
            return;
        }
        Tms282StatDiff stats = Tms282StatDiff.of(chr);
        guarded(chr, request.toString(), () -> chr.revive(chr.getField().getId()));
        session.send(stats.packet(true));
        System.out.printf("[TMS282] %s character=%d -> 265 revive%n", request, chr.getId());
    }

    // ---- shared ------------------------------------------------------------------------------------------------

    private static void refuse(Tms282LoginState session, byte[] packet, IllegalArgumentException e) {
        int op = packet.length < 2 ? -1 : (packet[0] & 0xFF) | (packet[1] & 0xFF) << 8;
        System.out.printf("[TMS282] REJECTED CP%04X len=%d: %s; SENT LP009D unlock%n", op, packet.length, e.getMessage());
        session.send(Tms282StatPackets.unlock());
    }

    private static void guarded(Char chr, String what, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException | LinkageError e) {
            System.out.printf("[TMS282] FAILED %s character=%d: %s%n", what, chr.getId(), e);
        }
    }
}
