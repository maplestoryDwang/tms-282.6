package net.swordie.ms.connection.packet;

import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.protocol.codec.InventoryOperationEncoder;
import net.swordie.ms.enums.InvType;
import net.swordie.ms.enums.InventoryOperation;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * TMS 282.3 {@link InventoryOperationEncoder}: the 265 WvsContext inventory builders as LP008F
 * ({@link Tms282InventoryPackets}), for the flows that run outside a 282 route - item/NPC/content scripts
 * (sm.giveItem, chr.addItemToInventory, sm.consumeItem), timers - whose 265 INVENTORY_OPERATION the guard dropped, so
 * the client never saw items a script gave or took (control-panel audit 2026-09-24).
 * <p>
 * Inside a 282 route ({@link Tms282UnlockScope#inRoute()}) it declines: those routes diff the inventories and send
 * their own LP008F (Tms282ItemHandlers, Tms282EquipHandlers, Tms282DailyPlayHandlers ...), so a translation there
 * would apply every change twice. Operations without a 282 layout here (bag, lock, item exp, preset) are declined too.
 */
public final class Tms282InventoryOperationEncoder implements InventoryOperationEncoder {
    private final Charset charset;

    public Tms282InventoryOperationEncoder(Charset charset) {
        this.charset = Objects.requireNonNull(charset, "charset");
    }

    @Override
    public Optional<OutPacket> operation(boolean exclRequest, boolean notRemoveAddInfo, InventoryOperation type,
                                         short oldPos, short newPos, Item item) {
        if (Tms282UnlockScope.inRoute() || item == null || type == null) {
            return Optional.empty();
        }
        InvType invType = invType(item, oldPos, newPos);
        Tms282InventoryPackets.Operation op = switch (type) {
            case Add -> new Tms282InventoryPackets.Add(invType, oldPos, item);
            case UpdateQuantity -> new Tms282InventoryPackets.UpdateQuantity(invType, oldPos, item.getQuantity());
            case Move -> new Tms282InventoryPackets.Move(invType, oldPos, newPos);
            case Remove -> new Tms282InventoryPackets.Remove(invType, oldPos);
            default -> null;
        };
        return op == null ? Optional.empty()
                : Optional.of(Tms282InventoryPackets.inventoryOperation(exclRequest, List.of(op), charset));
    }

    @Override
    public Optional<OutPacket> add(Item item) {
        if (Tms282UnlockScope.inRoute() || item == null) {
            return Optional.empty();
        }
        return Optional.of(Tms282InventoryPackets.inventoryOperation(true,
                List.of(new Tms282InventoryPackets.Add(item.getInvType(), (short) item.getBagIndex(), item)), charset));
    }

    @Override
    public Optional<OutPacket> removeAndAdd(Item item) {
        if (Tms282UnlockScope.inRoute() || item == null) {
            return Optional.empty();
        }
        short pos = (short) item.getBagIndex();
        return Optional.of(Tms282InventoryPackets.inventoryOperation(false, List.of(
                new Tms282InventoryPackets.Remove(item.getInvType(), pos),
                new Tms282InventoryPackets.Add(item.getInvType(), pos, item)), charset));
    }

    /** As WvsContext.inventoryOperation: a worn item moving from/to the bag is named by its bag inventory. */
    private static InvType invType(Item item, short oldPos, short newPos) {
        InvType invType = item.getInvType();
        if ((oldPos > 0 && newPos < 0 && invType == InvType.EQUIPPED) || (invType == InvType.EQUIPPED && oldPos < 0)) {
            invType = item.isCash() ? InvType.DECORATION : InvType.EQUIP;
        }
        return invType;
    }
}
