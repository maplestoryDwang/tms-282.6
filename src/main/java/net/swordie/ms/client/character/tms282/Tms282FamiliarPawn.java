package net.swordie.ms.client.character.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.packet.Tms282FamiliarPackets;

import java.util.ArrayList;
import java.util.List;

/**
 * The summoned 萌獸 in the field (phase 2, research/tms2823/familiar-20260927/SUMMON-RESEARCH.md). The client does
 * not create it from SummonedSN: the server creates an AFamiliar actor (class 0x9B1C9E5B) in the owner's LP0425 world
 * and points the controller's leaf20 at it; the owner's client then moves it and runs its attack AI (its own RPCs,
 * wires 84..90 on the pawn actor, are not handled yet: the familiar is visible but deals no damage).
 * Actor ids: controller 0x50000000 + character id, pawn 0x60000000 + character id.
 */
public final class Tms282FamiliarPawn {
    private Tms282FamiliarPawn() {
    }

    public static long pawnActorId(Char chr) {
        return 0x60000000L + chr.getId();
    }

    /** Packets after the summoned serial changed from {@code previous} to {@code state.summonedSerial()}. */
    static List<OutPacket> onSummonChanged(Char chr, long previous, Tms282FamiliarState state) {
        List<OutPacket> packets = new ArrayList<>(4);
        long controller = Tms282FamiliarRuntime.actorId(chr);
        if (previous != 0) {
            packets.add(Tms282FamiliarPackets.summoned(chr.getId(), controller, 0, 0));
            packets.add(Tms282FamiliarPackets.pawnDestroy(chr.getId(), pawnActorId(chr)));
        }
        packets.addAll(create(chr, state));
        return packets;
    }

    /** What the other players of the field get: the same AFamiliar create, prefixed with the owner's id. */
    public static List<OutPacket> observerCreate(Char owner, Tms282FamiliarState state) {
        var record = state.registered().get(state.summonedSerial());
        if (state.summonedSerial() == 0 || record == null || owner.getUser() == null) {
            return List.of();
        }
        int x = owner.getPosition() == null ? 0 : owner.getPosition().getX();
        int y = owner.getPosition() == null ? 0 : owner.getPosition().getY();
        return List.of(Tms282FamiliarPackets.pawnCreate(owner.getId(), pawnActorId(owner), owner.getUser().getId(), record, x, y));
    }

    /** The summoned familiar's create + controller link (after a summon, and after every window bootstrap). */
    public static List<OutPacket> create(Char chr, Tms282FamiliarState state) {
        var record = state.registered().get(state.summonedSerial());
        if (state.summonedSerial() == 0 || record == null || chr.getUser() == null) {
            return List.of();
        }
        long pawn = pawnActorId(chr);
        int x = chr.getPosition() == null ? 0 : chr.getPosition().getX();
        int y = chr.getPosition() == null ? 0 : chr.getPosition().getY();
        return List.of(Tms282FamiliarPackets.pawnCreate(chr.getId(), pawn, chr.getUser().getId(), record, x, y),
                Tms282FamiliarPackets.summoned(chr.getId(), Tms282FamiliarRuntime.actorId(chr), pawn, state.summonedSerial()));
    }
}
