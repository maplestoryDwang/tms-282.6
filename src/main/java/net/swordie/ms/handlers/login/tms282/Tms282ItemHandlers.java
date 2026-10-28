package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.client.character.tms282.Tms282InventoryDiff;
import net.swordie.ms.client.character.tms282.Tms282SlotChange;
import net.swordie.ms.client.character.tms282.Tms282StatDiff;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.packet.Tms282AbilityMassUpRequest;
import net.swordie.ms.connection.packet.Tms282AbilityUpRequest;
import net.swordie.ms.connection.packet.Tms282ChangeSlotRequest;
import net.swordie.ms.connection.packet.Tms282InventoryPackets;
import net.swordie.ms.connection.packet.Tms282ItemUseRequest;
import net.swordie.ms.connection.packet.Tms282StatPackets;
import net.swordie.ms.enums.InvType;
import net.swordie.ms.enums.Stat;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.handlers.item.InventoryHandler;
import net.swordie.ms.handlers.item.ItemHandler;
import net.swordie.ms.handlers.user.UserStatHandler;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/**
 * TMS 282.3 items and AP on the channel socket after LP02EF: each 282 request is decoded here and handed, as its
 * 265 body, to the 265 handler (consumables, slot moves / equip / drop and AP stay 265 code). 265 answers with its
 * own INVENTORY_OPERATION / STAT_CHANGED, which the fail-closed guard drops; the 282 replies are computed from what
 * the 265 call changed (mementos taken before it):
 * <ul>
 *     <li>CP0148 potion / stat-change item ({@link Tms282ItemUseRequest}) -> 265
 *     {@code ItemHandler.handleUserStatChangeItemUseRequest} -> LP008F from a {@link Tms282InventoryDiff} of the
 *     CONSUME bag and LP009D from a {@link Tms282StatDiff} (hp/mp);</li>
 *     <li>CP0141 move / swap / equip / unequip / drop ({@link Tms282ChangeSlotRequest}) -> 265
 *     {@code InventoryHandler.handleUserChangeSlotPositionRequest} -> LP008F Move, Remove or UpdateQuantity
 *     ({@link Tms282SlotChange}), LP009D when HP/MP were capped; the drop reaches the field as LP074D (265
 *     {@code Field.drop} through the profile's DropPacketEncoder) and a new look as LP048D (265
 *     {@code Char.equip}/{@code unequip} through the AvatarModifiedEncoder);</li>
 *     <li>CP0190 one AP ({@link Tms282AbilityUpRequest}) -> 265 {@code UserStatHandler.handleUserAbilityUpRequest}
 *     -> LP009D (AP and the stat);</li>
 *     <li>CP0191 auto-assign ({@link Tms282AbilityMassUpRequest}) -> the same 265 single-AP handler once per point
 *     (265's own MASS_UP adds amount x 20 to every stat, STR included; the single-AP rule is +1, MaxHP/MaxMP +20)
 *     -> one LP009D.</li>
 * </ul>
 * Every request locks the client (5E04DA0(ctx, 1)), so every path ends in a packet with exclRequest set; a refused
 * or failed request gets LP009D unlock. Nothing here ends the session.
 */
final class Tms282ItemHandlers {
    /** The AP stats a client may name (the LP009D bits of STR/DEX/INT/LUK/MaxHP/MaxMP, also the 265 flags). */
    static final Set<Long> AP_STATS = Set.of(Tms282StatPackets.Stat.STR.mask(), Tms282StatPackets.Stat.DEX.mask(),
            Tms282StatPackets.Stat.INT.mask(), Tms282StatPackets.Stat.LUK.mask(), Tms282StatPackets.Stat.MAX_HP.mask(),
            Tms282StatPackets.Stat.MAX_MP.mask());

    private final Charset charset;

    Tms282ItemHandlers(Tms282LoginContext context) {
        this.charset = Objects.requireNonNull(context, "context").settings().nameCharset();
    }

    void register(Tms282Routes routes) {
        routes.channel(Tms282ItemUseRequest.OPCODE, InHeader.USER_STAT_CHANGE_ITEM_USE_REQUEST).when(Tms282Guards.IN_FIELD)
                .handle("potion -> 265 stat-change item, LP008F + LP009D", this::onItemUse);
        routes.channel(Tms282ChangeSlotRequest.OPCODE, InHeader.USER_CHANGE_SLOT_POSITION_REQUEST).when(Tms282Guards.IN_FIELD)
                .handle("move/equip/drop -> 265 InventoryHandler, LP008F (+ LP074D, LP048D)", this::onChangeSlot);
        routes.channel(Tms282AbilityUpRequest.OPCODE, InHeader.USER_ABILITY_UP_REQUEST).when(Tms282Guards.IN_FIELD)
                .handle("AP -> 265 ability up, LP009D", this::onAbilityUp);
        routes.channel(Tms282AbilityMassUpRequest.OPCODE, InHeader.USER_ABILITY_MASS_UP_REQUEST).when(Tms282Guards.IN_FIELD)
                .handle("AP auto-assign -> 265 ability up per point, LP009D", this::onAbilityMassUp);
    }

    /** The replies of one delegated request, in send order, and a log summary. */
    record Reply(List<OutPacket> packets, String log) {
    }

    // ---- CP0148 ----------------------------------------------------------------------------------------------

    private void onItemUse(Tms282LoginState session, int op, byte[] packet) {
        Tms282ItemUseRequest request;
        try {
            request = Tms282ItemUseRequest.parse(packet);
        } catch (IllegalArgumentException e) {
            reject(session, op, packet, e);
            return;
        }
        Char chr = session.fieldCharacter();
        Tms282InventoryDiff consume = Tms282InventoryDiff.of(InvType.CONSUME, chr.getInventoryByType(InvType.CONSUME));
        send(session, request.toString(), chr, () -> consume.changes(),
                () -> ItemHandler.handleUserStatChangeItemUseRequest(chr, new InPacket(request.toGms265Body())), true);
    }

    // ---- CP0141 ----------------------------------------------------------------------------------------------

    private void onChangeSlot(Tms282LoginState session, int op, byte[] packet) {
        Tms282ChangeSlotRequest request;
        try {
            request = Tms282ChangeSlotRequest.parse(packet);
        } catch (IllegalArgumentException e) {
            reject(session, op, packet, e);
            return;
        }
        Char chr = session.fieldCharacter();
        Tms282SlotChange change = Tms282SlotChange.before(chr, request.invType(), request.serverFrom(), request.serverTo());
        Item item = change.item();
        String summary = request + (item == null ? " (empty slot)" : " item=" + item.getItemId());
        send(session, summary, chr, change::operations,
                () -> InventoryHandler.handleUserChangeSlotPositionRequest(chr, new InPacket(request.toGms265Body())));
    }

    // ---- CP0190 / CP0191 -------------------------------------------------------------------------------------

    private void onAbilityUp(Tms282LoginState session, int op, byte[] packet) {
        Tms282AbilityUpRequest request;
        try {
            request = Tms282AbilityUpRequest.parse(packet);
        } catch (IllegalArgumentException e) {
            reject(session, op, packet, e);
            return;
        }
        Char chr = session.fieldCharacter();
        if (!AP_STATS.contains(request.stat())) {
            refuse(session, chr, request.toString(), "not an AP stat");
            return;
        }
        send(session, request.toString(), chr, List::of, () -> abilityUp(chr, request.tick(), request.stat()));
    }

    private void onAbilityMassUp(Tms282LoginState session, int op, byte[] packet) {
        Tms282AbilityMassUpRequest request;
        try {
            request = Tms282AbilityMassUpRequest.parse(packet);
        } catch (IllegalArgumentException e) {
            reject(session, op, packet, e);
            return;
        }
        Char chr = session.fieldCharacter();
        for (Tms282AbilityMassUpRequest.Entry e : request.entries()) {
            if (!AP_STATS.contains(e.stat()) || e.amount() < 0) {
                refuse(session, chr, request.toString(), "entry " + e + " is not an AP stat / amount");
                return;
            }
        }
        long ap = chr.getAvatarData().getCharacterStat().getAp();
        if (request.totalAmount() > ap) {
            refuse(session, chr, request.toString(), "asks " + request.totalAmount() + " AP, has " + ap);
            return;
        }
        send(session, request.toString(), chr, List::of, () -> {
            for (Tms282AbilityMassUpRequest.Entry e : request.entries()) {
                for (int i = 0; i < e.amount(); i++) {
                    abilityUp(chr, request.tick(), e.stat());
                }
            }
        });
    }

    /** One AP through the 265 handler ({@code tick, i16 stat}). */
    private static void abilityUp(Char chr, int tick, long stat) {
        byte[] body = ByteBuffer.allocate(6).order(ByteOrder.LITTLE_ENDIAN).putInt(tick).putShort((short) stat).array();
        if (Stat.getByVal((short) stat) == null) {
            throw new IllegalStateException(String.format("no 265 Stat for 0x%X", stat));
        }
        UserStatHandler.handleUserAbilityUpRequest(chr, new InPacket(body));
    }

    // ---- shared ----------------------------------------------------------------------------------------------

    /**
     * Runs {@code gms265} and sends the 282 replies of what it changed: LP008F with {@code inventory}'s operations
     * (if any), then LP009D with the changed stats; exclRequest on both. Nothing changed: LP009D unlock only.
     */
    private void send(Tms282LoginState session, String summary, Char chr,
                      Supplier<List<Tms282InventoryPackets.Operation>> inventory, Runnable gms265) {
        send(session, summary, chr, inventory, gms265, false);
    }

    /** {@code recovery}: the HP / MP gained are shown over the character (potions, placeholder-audit-0929 #4). */
    private void send(Tms282LoginState session, String summary, Char chr,
                      Supplier<List<Tms282InventoryPackets.Operation>> inventory, Runnable gms265, boolean recovery) {
        Tms282StatDiff stats = Tms282StatDiff.of(chr);
        long hpBefore = chr.getAvatarData().getCharacterStat().getHp();
        long mpBefore = chr.getAvatarData().getCharacterStat().getMp();
        try {
            gms265.run();
        } catch (RuntimeException | LinkageError e) {
            session.send(Tms282StatPackets.unlock());
            System.out.println("[TMS282] FAILED " + summary + " character=" + chr.getId() + ": " + e + "; SENT LP009D unlock");
            return;
        }
        int hpGain = recovery ? (int) Math.max(0, chr.getAvatarData().getCharacterStat().getHp() - hpBefore) : 0;
        int mpGain = recovery ? (int) Math.max(0, chr.getAvatarData().getCharacterStat().getMp() - mpBefore) : 0;
        Reply reply = reply(inventory.get(), stats.changes(), hpGain, mpGain);
        for (OutPacket p : reply.packets()) {
            session.send(p);
        }
        System.out.println("[TMS282] " + summary + " character=" + chr.getId() + " -> 265; " + reply.log());
    }

    /** LP008F (operations) and LP009D (stats) with exclRequest; LP009D unlock when both are empty. */
    Reply reply(List<Tms282InventoryPackets.Operation> operations, Map<Tms282StatPackets.Stat, Long> stats) {
        return reply(operations, stats, 0, 0);
    }

    Reply reply(List<Tms282InventoryPackets.Operation> operations, Map<Tms282StatPackets.Stat, Long> stats,
                int hpRecovery, int mpRecovery) {
        List<OutPacket> packets = new ArrayList<>(2);
        StringBuilder log = new StringBuilder();
        if (!operations.isEmpty()) {
            packets.add(Tms282InventoryPackets.inventoryOperation(true, operations, charset));
            log.append("SENT LP008F ").append(operations);
        }
        if (!stats.isEmpty() || packets.isEmpty()) {
            packets.add(Tms282StatPackets.statChanged(true, stats, hpRecovery, mpRecovery));
            log.append(log.isEmpty() ? "" : " + ").append(stats.isEmpty() ? "SENT LP009D unlock (nothing changed)" : "SENT LP009D " + stats)
                    .append(hpRecovery > 0 || mpRecovery > 0 ? " recovery +" + hpRecovery + "/+" + mpRecovery : "");
        }
        return new Reply(List.copyOf(packets), log.toString());
    }

    private static void refuse(Tms282LoginState session, Char chr, String summary, String why) {
        session.send(Tms282StatPackets.unlock());
        System.out.println("[TMS282] REFUSED " + summary + " character=" + chr.getId() + ": " + why + "; SENT LP009D unlock");
    }

    private static void reject(Tms282LoginState session, int op, byte[] packet, IllegalArgumentException e) {
        session.send(Tms282StatPackets.unlock());
        System.out.printf("[TMS282] REJECTED CP%04X len=%d: %s; SENT LP009D unlock%n", op, packet.length, e.getMessage());
    }
}
