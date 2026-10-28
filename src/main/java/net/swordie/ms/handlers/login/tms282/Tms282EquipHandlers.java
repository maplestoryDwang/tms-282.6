package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.client.character.tms282.Tms282InventoryWatch;
import net.swordie.ms.client.character.tms282.Tms282StatDiff;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.packet.Tms282EnhanceRequests;
import net.swordie.ms.connection.packet.Tms282EnhanceRequests.Layout;
import net.swordie.ms.connection.packet.Tms282InventoryPackets;
import net.swordie.ms.connection.packet.Tms282StatPackets;
import net.swordie.ms.enums.InvType;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.handlers.item.CubeHandler;
import net.swordie.ms.handlers.item.ItemHandler;
import net.swordie.ms.handlers.item.ItemUpgradeHandler;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * TMS 282.3 equipment enhancement on the channel socket after LP02EF: scrolls, star force / spell trace, potential
 * and bonus potential scrolls, cubes, flames, arcane / authentic symbols, the magnifying glass and Toad's hammer.
 * Each request is decoded by its {@link Layout} into the 265 body and handed to the unchanged 265 handler (a
 * {@link Gms265Call} per layout: one command table, no per-request code here). The 265 handler changes the equip in
 * place and answers with 265 INVENTORY_OPERATION / STAT_CHANGED / EXCL_REQUEST, which the fail-closed guard drops;
 * the 282 reply is computed from what it changed ({@link Tms282InventoryWatch}, {@link Tms282StatDiff}):
 * LP008F with exclRequest (the changed equip re-sent over its slot, the used scroll / cube stack), LP009D when
 * stats changed, LP009D unlock when nothing did. The star force window's own packets (EQUIPMENT_ENCHANT) reach the
 * client through the LP01A1 legacy translator (Tms282EquipmentEnchantTranslators).
 * <p>
 * The 265 handlers keep their database writes ({@code Equip.saveToSQL}) in production.
 */
final class Tms282EquipHandlers {
    /** The 265 handler of one layout. */
    @FunctionalInterface
    interface Gms265Call extends BiConsumer<Char, InPacket> {
    }

    /** One route per layout: its 265 header and handler. */
    record Command(InHeader header, Gms265Call call) {
    }

    static final Map<Layout, Command> COMMANDS = commands();

    private final Charset charset;

    Tms282EquipHandlers(Tms282LoginContext context) {
        this.charset = Objects.requireNonNull(context, "context").settings().nameCharset();
    }

    private static Map<Layout, Command> commands() {
        Map<Layout, Command> m = new EnumMap<>(Layout.class);
        m.put(Layout.UPGRADE_ITEM, new Command(InHeader.USER_UPGRADE_ITEM_USE_REQUEST,
                ItemUpgradeHandler::handleUserUpgradeItemUseRequest));
        m.put(Layout.HYPER_UPGRADE_ITEM, new Command(InHeader.USER_HYPER_UPGRADE_ITEM_USE_REQUEST,
                ItemUpgradeHandler::handleUserHyperUpgradeItemUseRequest));
        m.put(Layout.MEMORIAL_EX_UPGRADE_ITEM, new Command(InHeader.USER_MEMORIAL_EX_UPGRADE_ITEM_USE_REQUEST,
                ItemUpgradeHandler::handleMemorialExUpgradeItemUseRequest));
        m.put(Layout.ITEM_OPTION_UPGRADE_ITEM, new Command(InHeader.USER_ITEM_OPTION_UPGRADE_ITEM_USE_REQUEST,
                ItemUpgradeHandler::handleUserItemOptionUpgradeItemUseRequest));
        m.put(Layout.ADDITIONAL_OPT_UPGRADE_ITEM, new Command(InHeader.USER_ADDITIONAL_OPT_UPGRADE_ITEM_USE_REQUEST,
                ItemUpgradeHandler::handleUserAdditionalOptUpgradeItemUseRequest));
        m.put(Layout.CONSUME_CUBE_ITEM, new Command(InHeader.USER_CONSUME_CUBE_ITEM_USE_REQUEST,
                CubeHandler::handleUserConsumeCubeItemUserRequest));
        m.put(Layout.BONUS_MYSTICAL_CUBE_ITEM, new Command(InHeader.USER_BONUS_MYSTICAL_ITEM_USE_REQUEST,
                CubeHandler::handleUserBonusMysticalCubeItemUseRequest));
        m.put(Layout.BONUS_BRIGHT_CUBE_ITEM, new Command(InHeader.USER_BONUS_BRIGHT_CUBE_ITEM_USE_REQUEST,
                CubeHandler::handleUserBonusCubeItemUseRequest));
        m.put(Layout.EQUIPMENT_ENCHANT, new Command(InHeader.USER_EQUIPMENT_ENCHANT_WITH_SINGLE_UI_REQUEST,
                ItemHandler::handleUserEquipmentEnchantWithSingleUIRequest));
        m.put(Layout.ARCANE_SYMBOL_ENCHANCE, new Command(InHeader.USER_UI_ARCANE_SYMBOL_ENCHANCE_REQUEST,
                ItemHandler::handleUserArcaneSymbolEnchanceRequest));
        m.put(Layout.SACRED_SYMBOL_ENCHANCE, new Command(InHeader.USER_UI_SACRED_SYMBOL_ENCHANCE_REQUEST,
                ItemHandler::handleUserSacredSymbolEnchanceRequest));
        m.put(Layout.ITEM_RELEASE, new Command(InHeader.USER_ITEM_RELEASE_REQUEST,
                ItemUpgradeHandler::handleUserItemReleaseRequest));
        m.put(Layout.TOADS_HAMMER, new Command(InHeader.USER_TOADS_HAMMER_REQUEST,
                ItemUpgradeHandler::handleUserToadsHammerRequest));
        if (m.size() != Layout.values().length) {
            throw new IllegalStateException("every enhancement layout needs a 265 command");
        }
        return m;
    }

    void register(Tms282Routes routes) {
        for (Map.Entry<Layout, Command> e : COMMANDS.entrySet()) {
            Layout layout = e.getKey();
            boolean obsoleteCube=layout==Layout.CONSUME_CUBE_ITEM || layout==Layout.BONUS_MYSTICAL_CUBE_ITEM
                    || layout==Layout.BONUS_BRIGHT_CUBE_ITEM;
            routes.channel(layout.opcode(), e.getValue().header()).when(Tms282Guards.IN_FIELD)
                    .handle(obsoleteCube?"obsolete cube fallback refusal/unlock; no payment, active UI uses CP0780"
                                    :layout.label() + " -> 265 " + e.getValue().header() + ", LP008F/LP009D",
                            (session, op, packet) -> onRequest(session, layout, packet));
        }
    }

    /** The 282 replies of one delegated request, in send order, and a log summary. */
    record Reply(List<OutPacket> packets, String log) {
    }

    private void onRequest(Tms282LoginState session, Layout layout, byte[] packet) {
        Char chr = session.fieldCharacter();
        // Original282 AF65E0 emits types13..20;369C9F6's <=12 fallbacks are not its active cube UI.
        // Their old result/choice packets are unsupported. Never charge for an unreachable result window.
        if(layout==Layout.CONSUME_CUBE_ITEM || layout==Layout.BONUS_MYSTICAL_CUBE_ITEM
                || layout==Layout.BONUS_BRIGHT_CUBE_ITEM) {
            chr.chatMessage("此舊版方塊入口不適用目前版本，未扣除道具或楓幣。請使用原生方塊介面。");
            session.send(Tms282StatPackets.unlock());
            System.out.printf("[TMS282] REJECTED obsolete cube CP%04X character=%d; no payment, use native CP0780%n",
                    layout.opcode(),chr.getId());
            return;
        }
        Tms282EnhanceRequests.Parsed request;
        try {
            request = layout.parse(packet, slots(chr));
        } catch (IllegalArgumentException e) {
            session.send(Tms282StatPackets.unlock());
            System.out.printf("[TMS282] REJECTED CP%04X %s len=%d: %s; SENT LP009D unlock%n", layout.opcode(),
                    layout.label(), packet.length, e.getMessage());
            return;
        }
        String summary = String.format("CP%04X %s", layout.opcode(), request.describe());
        Tms282InventoryWatch inventory = Tms282InventoryWatch.of(chr);
        Tms282StatDiff stats = Tms282StatDiff.of(chr);
        try {
            COMMANDS.get(layout).call().accept(chr, new InPacket(request.body()));
        } catch (RuntimeException | LinkageError e) {
            // 265 may have changed the equip before failing: still send what changed, then unlock
            System.out.println("[TMS282] FAILED " + summary + " character=" + chr.getId() + ": " + e);
        }
        Reply reply = reply(inventory.changes(), stats.changes());
        for (OutPacket p : reply.packets()) {
            session.send(p);
        }
        System.out.println("[TMS282] " + summary + " character=" + chr.getId() + " -> 265; " + reply.log());
    }

    /** LP008F (operations) and LP009D (stats) with exclRequest; LP009D unlock when both are empty. */
    Reply reply(List<Tms282InventoryPackets.Operation> operations, Map<Tms282StatPackets.Stat, Long> stats) {
        List<OutPacket> packets = new ArrayList<>(2);
        StringBuilder log = new StringBuilder();
        if (!operations.isEmpty()) {
            packets.add(Tms282InventoryPackets.inventoryOperation(true, operations, charset));
            log.append("SENT LP008F ").append(operations.size()).append(" op(s) ").append(describe(operations));
        }
        if (!stats.isEmpty() || packets.isEmpty()) {
            packets.add(Tms282StatPackets.statChanged(true, stats));
            log.append(log.isEmpty() ? "" : " + ").append(stats.isEmpty() ? "SENT LP009D unlock (nothing changed)" : "SENT LP009D " + stats);
        }
        return new Reply(List.copyOf(packets), log.toString());
    }

    private static String describe(List<Tms282InventoryPackets.Operation> ops) {
        StringBuilder sb = new StringBuilder("[");
        for (Tms282InventoryPackets.Operation op : ops) {
            if (sb.length() > 1) {
                sb.append(", ");
            }
            sb.append(op.getClass().getSimpleName()).append(' ').append(op.invType()).append(' ').append(op.pos());
            if (op instanceof Tms282InventoryPackets.Add add) {
                sb.append(" item=").append(add.item().getItemId());
            }
        }
        return sb.append(']').toString();
    }

    /** The character's slots for the layouts that choose between two positions. */
    static Tms282EnhanceRequests.SlotLookup slots(Char chr) {
        return (type, pos) -> {
            Item item = chr.getInventoryByType(type).getItemBySlot(type == InvType.EQUIPPED ? Math.abs(pos) : pos);
            return item == null ? 0 : item.getItemId();
        };
    }
}
