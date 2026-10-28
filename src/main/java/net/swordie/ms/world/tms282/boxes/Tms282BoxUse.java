package net.swordie.ms.world.tms282.boxes;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.client.character.tms282.Tms282ChangeWatch;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.packet.Tms282InventoryPackets;
import net.swordie.ms.connection.packet.Tms282UnlockScope;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;
import net.swordie.ms.enums.ChatType;
import net.swordie.ms.enums.InvType;
import net.swordie.ms.handlers.login.tms282.Tms282LoginSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Registry of data-driven box/coupon handlers for the TMS 282 client. Hooked from
 * {@code ItemHandler.handleUserConsumeCashItemUseRequest} (CP0150, all 5xxxxxx cash items; the 282 route diffs every
 * bag itself), {@code handleUserScriptItemUseRequest} (CP014E script items, exchange coupons, 568xxxx) and
 * {@code handleUserStatChangeItemUseRequest} (CP0148, 2028xxx boxes with {@code reward/}). Items without data
 * return {@code false} and keep their legacy behaviour.
 * <p>
 * Order matters: Luna Crystal and beauty coupon tables are checked before the generic raffle table; the hand-verified
 * 葬送的芙莉蓮 collab table next; then the client's own WZ {@code reward/} data before the research tables
 * (official-rewards, exchange-coupon) and the 265 hardcoded pools.
 */
public final class Tms282BoxUse {
    /**
     * Which 282 bag replies {@link #tryUse(Char, Item, Replies)} sends itself. Inside a 282 route the 265
     * INVENTORY_OPERATION is declined (Tms282InventoryOperationEncoder), so a route that does not diff the bags
     * must get them from here.
     */
    public enum Replies {
        /** The caller's route diffs every bag (CP0150, CP016F) or it is not a 282 request thread. */
        NONE,
        /** CP014E: the route only sends an unlock -> LP008F of every bag + LP009D (Tms282ChangeWatch). */
        ALL_BAGS,
        /** CP0148: the route diffs CONSUME and the stats -> LP008F (no exclRequest) of the other bags only. */
        OTHER_THAN_CONSUME
    }

    private static volatile List<BoxUseHandler> handlers;

    private Tms282BoxUse() {
    }

    private static List<BoxUseHandler> handlers() {
        List<BoxUseHandler> hs = handlers;
        if (hs != null) {
            return hs;
        }
        synchronized (Tms282BoxUse.class) {
            if (handlers == null) {
                List<BoxUseHandler> loaded = new ArrayList<>();
                register(loaded, "luna", LunaCrystalHandler::load);
                register(loaded, "beauty", BeautyCouponHandler::load);
                register(loaded, "raffle", RaffleBoxHandler::load);
                register(loaded, "collab-frieren", () -> OfficialRewardBoxHandler.load("collab-frieren.tsv", "collab-frieren"));
                register(loaded, "wz-reward", WzRewardBoxHandler::load);
                register(loaded, "official-rewards", OfficialRewardBoxHandler::load);
                register(loaded, "exchange-coupon", ExchangeCouponHandler::load);
                register(loaded, "legacy-265-pools", LegacyPoolBoxHandler::load);
                handlers = List.copyOf(loaded);
            }
            return handlers;
        }
    }

    private static void register(List<BoxUseHandler> into, String what, Supplier<BoxUseHandler> loader) {
        try {
            BoxUseHandler h = loader.get();
            into.add(h);
            System.out.println("[TMS282][Boxes] loaded " + h.name());
        } catch (RuntimeException e) {
            System.out.println("[TMS282][Boxes] failed to load " + what + " data: " + e);
        }
    }

    /** Drops the cached tables; the next use reloads them from disk. */
    public static void reload() {
        synchronized (Tms282BoxUse.class) {
            handlers = null;
        }
    }

    /** The loaded handlers in lookup order (checks, reports). */
    public static List<BoxUseHandler> loadedHandlers() {
        return handlers();
    }

    /** @return whether a data-driven handler exists for {@code itemId}. */
    public static boolean handles(int itemId) {
        return owner(itemId) != null;
    }

    /** The first handler that owns {@code itemId}, or null. */
    public static BoxUseHandler owner(int itemId) {
        for (BoxUseHandler h : handlers()) {
            if (h.handles(itemId)) {
                return h;
            }
        }
        return null;
    }

    /**
     * Uses {@code item} through the matching handler. Always disposes the client when handled.
     *
     * @return false when no handler owns the item (caller continues with its legacy path).
     */
    public static boolean tryUse(Char chr, Item item) {
        return tryUse(chr, item, Replies.NONE);
    }

    /** As {@link #tryUse(Char, Item)}, then the 282 bag replies {@code replies} names (only on a 282 route thread). */
    public static boolean tryUse(Char chr, Item item, Replies replies) {
        if (chr == null || item == null || item.getQuantity() < 1) {
            return false;
        }
        BoxUseHandler h = owner(item.getItemId());
        if (h == null) {
            return false;
        }
        Tms282ChangeWatch watch = replies != Replies.NONE && onTms282Route() ? Tms282ChangeWatch.of(chr) : null;
        try {
            h.use(chr, item);
        } catch (RuntimeException e) {
            System.out.printf("[TMS282][Boxes] %s failed for %s item %d: %s%n", h.name(), chr.getName(), item.getItemId(), e);
            chr.chatMessage(ChatType.SystemNotice, "道具使用失敗，請稍後再試。");
        } finally {
            if (watch != null) {
                sendReplies(chr, watch, replies);
            }
            chr.dispose();
        }
        return true;
    }

    /**
     * For an item-script path that only knows the id (e.g. the "no script file" fallback): the first CONSUME, then
     * CASH stack of {@code itemId}, through {@link #tryUse(Char, Item, Replies)} with {@link Replies#ALL_BAGS}.
     */
    public static boolean tryUseById(Char chr, int itemId) {
        if (chr == null || owner(itemId) == null) {
            return false;
        }
        for (InvType t : new InvType[]{InvType.CONSUME, InvType.CASH, InvType.ETC}) {
            var inv = chr.getInventoryByType(t);
            Item it = inv == null ? null : inv.getItemByItemID(itemId);
            if (it != null && it.getQuantity() > 0) {
                return tryUse(chr, it, Replies.ALL_BAGS);
            }
        }
        return false;
    }

    static boolean onTms282Route() {
        return Protocol.active().version() == ProtocolVersion.TMS_282_3 && Tms282UnlockScope.inRoute();
    }

    private static void sendReplies(Char chr, Tms282ChangeWatch watch, Replies replies) {
        var charset = Tms282LoginSettings.load().nameCharset();
        if (replies == Replies.ALL_BAGS) {
            for (OutPacket p : watch.replies(charset)) {
                chr.write(p);
            }
            return;
        }
        List<Tms282InventoryPackets.Operation> ops = new ArrayList<>();
        for (Tms282InventoryPackets.Operation op : watch.operations()) {
            if (op.invType() != InvType.CONSUME) {
                ops.add(op);
            }
        }
        if (!ops.isEmpty()) {
            chr.write(Tms282InventoryPackets.inventoryOperation(false, ops, charset));
        }
    }
}
