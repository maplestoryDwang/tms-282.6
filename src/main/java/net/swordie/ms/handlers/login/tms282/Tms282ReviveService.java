package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.tms282.Tms282InventoryWatch;
import net.swordie.ms.client.character.tms282.Tms282StatDiff;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.packet.Tms282InventoryPackets;
import net.swordie.ms.connection.packet.Tms282ReviveRequest;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The death window's revive, 282 side. 265 has no in-place revive (it never reads the premium flag): {@code
 * Char.revive} always sends the character to the field's returnMap (or through the instance / death-count rules).
 * <p>
 * In-place revive (282 overlay only): the user picked the premium option AND holds a revive item
 * ({@link #REVIVE_ITEMS}, first found is used). One item is consumed, HP/MP are refilled, the character stays where it
 * died and no return-map penalty is applied. Replies: LP008F (the consumed item) and LP009D (HP/MP, exclRequest).
 * Otherwise the normal 265 revive runs (returnMap, HP/MP full; LP009D from the caller).
 */
final class Tms282ReviveService {
    /** 5510000 Wheel of Destiny (the item 265 {@code Char.openUIOnDead} offers the option for). */
    static final int[] REVIVE_ITEMS = {5510000};

    private final Charset charset;

    Tms282ReviveService(Charset charset) {
        this.charset = Objects.requireNonNull(charset, "charset");
    }

    /** The packets of an in-place revive, or an empty list when it does not apply (then the caller revives normally). */
    List<OutPacket> reviveInPlace(Char chr, Tms282ReviveRequest request) {
        int item = request.premium() ? reviveItem(chr) : 0;
        if (item == 0) {
            return List.of();
        }
        Tms282InventoryWatch inventory = Tms282InventoryWatch.of(chr);
        Tms282StatDiff stats = Tms282StatDiff.of(chr);
        chr.consumeItem(item, 1);
        chr.healHPMP();
        List<OutPacket> packets = new ArrayList<>(2);
        List<Tms282InventoryPackets.Operation> ops = inventory.changes();
        if (!ops.isEmpty()) {
            packets.add(Tms282InventoryPackets.inventoryOperation(false, ops, charset));
        }
        packets.add(stats.packet(true));
        System.out.printf("[TMS282] %s character=%d -> in-place revive with item %d (%d inventory op(s))%n", request,
                chr.getId(), item, ops.size());
        return List.copyOf(packets);
    }

    private static int reviveItem(Char chr) {
        for (int id : REVIVE_ITEMS) {
            if (chr.hasItem(id)) {
                return id;
            }
        }
        return 0;
    }
}
