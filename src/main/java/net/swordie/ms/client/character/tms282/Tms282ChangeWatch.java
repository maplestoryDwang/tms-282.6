package net.swordie.ms.client.character.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.Inventory;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.packet.Tms282InventoryPackets;
import net.swordie.ms.connection.packet.Tms282StatPackets;
import net.swordie.ms.enums.InvType;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Memento of everything a 265 request handler may change in a character's bags and stats: one
 * {@link Tms282InventoryDiff} per bag (equip, consume, install, etc, cash, decoration) and one {@link Tms282StatDiff}.
 * {@link #replies} turns what changed into the TMS 282.3 replies, both with exclRequest (the client locked on the
 * request): LP008F with every bag operation, then LP009D with the changed stats; when nothing changed, LP009D alone
 * releases the lock. Used by the daily-play handlers (shop, storage, return scrolls) the way the items phase uses
 * the two diffs directly.
 */
public final class Tms282ChangeWatch {
    private static final InvType[] BAGS = {InvType.EQUIP, InvType.CONSUME, InvType.INSTALL, InvType.ETC, InvType.CASH,
            InvType.DECORATION};

    private final List<Tms282InventoryDiff> bags = new ArrayList<>(BAGS.length);
    private final Tms282StatDiff stats;

    private Tms282ChangeWatch(Char chr) {
        for (InvType type : BAGS) {
            Inventory inventory = chr.getInventoryByType(type);
            if (inventory != null) {
                bags.add(Tms282InventoryDiff.of(type, inventory));
            }
        }
        stats = Tms282StatDiff.of(chr);
    }

    /** Remembers {@code chr}'s bags and stats as they are now. */
    public static Tms282ChangeWatch of(Char chr) {
        return new Tms282ChangeWatch(Objects.requireNonNull(chr, "chr"));
    }

    /** The bag operations since {@link #of}, bag by bag. */
    public List<Tms282InventoryPackets.Operation> operations() {
        List<Tms282InventoryPackets.Operation> ops = new ArrayList<>();
        for (Tms282InventoryDiff bag : bags) {
            ops.addAll(bag.changes());
        }
        return ops;
    }

    /** LP008F (if a bag changed) and LP009D (if a stat changed, or alone as the unlock), exclRequest on both. */
    public List<OutPacket> replies(Charset charset) {
        List<Tms282InventoryPackets.Operation> ops = operations();
        Map<Tms282StatPackets.Stat, Long> changed = stats.changes();
        List<OutPacket> out = new ArrayList<>(2);
        if (!ops.isEmpty()) {
            out.add(Tms282InventoryPackets.inventoryOperation(true, ops, charset));
        }
        if (!changed.isEmpty() || out.isEmpty()) {
            out.add(Tms282StatPackets.statChanged(true, changed));
        }
        return out;
    }

    /** One log fragment for {@link #replies}' content (computed again; call before the character changes further). */
    public String describe() {
        List<Tms282InventoryPackets.Operation> ops = operations();
        Map<Tms282StatPackets.Stat, Long> changed = stats.changes();
        if (ops.isEmpty() && changed.isEmpty()) {
            return "LP009D unlock (nothing changed)";
        }
        return (ops.isEmpty() ? "" : "LP008F " + ops + (changed.isEmpty() ? "" : " + "))
                + (changed.isEmpty() ? "" : "LP009D " + changed);
    }
}
