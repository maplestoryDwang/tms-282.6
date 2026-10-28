package net.swordie.ms.world.tms282.audit;

import net.swordie.ms.client.User;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.world.field.Field;

import java.sql.Timestamp;

/**
 * Facade for all audit logging. Every method is non-blocking (enqueue only) and never throws into the caller.
 * <p>
 * Money: {@code log_money}. Inventory: {@code zlog_inventory}. GM commands: {@code zlog_command}.
 * Lie detector: {@code zlog_antimacro}.
 * <p>
 * System properties: {@code tms282.audit.money.minDiff} (default 0 = log every change),
 * {@code tms282.audit.inventory.enabled} (default true).
 */
public final class AuditLog {

    /** Operation / type labels (free text in the DB, constants here to keep them consistent). */
    public static final String OTHER = "OTHER";
    public static final String PICKUP = "PICKUP";
    public static final String DROP = "DROP";
    public static final String NPC_SHOP = "NPC_SHOP";
    public static final String TRADE = "TRADE";
    public static final String TRADE_OFFER = "TRADE_OFFER";
    public static final String TRADE_CANCEL = "TRADE_CANCEL";
    public static final String STORAGE = "STORAGE";
    public static final String STORAGE_PUT = "STORAGE_PUT";
    public static final String STORAGE_TAKE = "STORAGE_TAKE";
    public static final String QUEST = "QUEST";
    public static final String SCRIPT = "SCRIPT";
    public static final String GM = "GM";

    private static final long MONEY_MIN_DIFF = Math.max(0L, Long.getLong("tms282.audit.money.minDiff", 0L));
    private static final boolean INVENTORY_ENABLED =
            !"false".equalsIgnoreCase(System.getProperty("tms282.audit.inventory.enabled", "true"));

    private AuditLog() {
    }

    // ------------------------------------------------------------------ money

    /** Core hook (Char.addMoneyInternal): label comes from {@link AuditContext}, else {@link #OTHER}. */
    public static void money(Char chr, long before, long after) {
        if (chr == null || before == after || !AuditWriter.enabled()) {
            return;
        }
        try {
            AuditContext.Mark m = AuditContext.current(chr.getId());
            String type = m != null ? m.reason : OTHER;
            String detail = m != null && m.parentId != 0 ? "parent=" + m.parentId : null;
            money(chr, before, after, type, detail);
        } catch (Throwable ignored) {
        }
    }

    /** Explicit money log (use only where the core hook is not on the path, e.g. offline edits). */
    public static void money(Char chr, long before, long after, String type, String detail) {
        if (chr == null || before == after || !AuditWriter.enabled()) {
            return;
        }
        long diff = after - before;
        if (MONEY_MIN_DIFF > 0 && Math.abs(diff) < MONEY_MIN_DIFF) {
            return;
        }
        try {
            AuditWriter.submit(AuditSchema.LOG_MONEY,
                    now(), accountId(chr), chr.getId(), before, after, diff, mapId(chr), type, detail);
        } catch (Throwable ignored) {
        }
    }

    // -------------------------------------------------------------- inventory

    /** Core hook (Char.addInventoryItem): {@code quantity} actually added to the inventory. */
    public static void itemGain(Char chr, Item item, int quantity) {
        itemCore(chr, item, quantity, "GAIN");
    }

    /** Core hook (Char.consumeInventoryItem): {@code quantity} actually removed. */
    public static void itemLose(Char chr, Item item, int quantity) {
        itemCore(chr, item, -quantity, "LOSE");
    }

    /** Explicit loss for paths that call Inventory.removeItem directly (drop, storage put, trade offer). */
    public static void itemLose(Char chr, Item item, int quantity, String operation, long parentId) {
        item(chr, item, -Math.abs(quantity), operation, parentId, null);
    }

    /** Explicit gain for paths that bypass Char.addItemToInventory. */
    public static void itemGain(Char chr, Item item, int quantity, String operation, long parentId) {
        item(chr, item, Math.abs(quantity), operation, parentId, null);
    }

    private static void itemCore(Char chr, Item item, int signedQty, String direction) {
        if (chr == null || item == null || signedQty == 0 || !INVENTORY_ENABLED || !AuditWriter.enabled()) {
            return;
        }
        try {
            AuditContext.Mark m = AuditContext.current(chr.getId());
            String op = (m != null ? m.reason : OTHER) + "_" + direction;
            item(chr, item, signedQty, op, m != null ? m.parentId : 0L, null);
        } catch (Throwable ignored) {
        }
    }

    /**
     * @param signedQty positive = gained, negative = lost
     * @param parentId  counterparty id (other character, npc/shop, trunk id ...), 0 if none
     */
    public static void item(Char chr, Item item, int signedQty, String operation, long parentId, String detail) {
        if (chr == null || item == null || !INVENTORY_ENABLED || !AuditWriter.enabled()) {
            return;
        }
        try {
            int invType = item.getInvType() != null ? item.getInvType().getVal() : -1;
            String log = "acc=" + accountId(chr) + " map=" + mapId(chr) + " slot=" + item.getBagIndex()
                    + (detail != null ? " " + detail : "");
            AuditWriter.submit(AuditSchema.ZLOG_INVENTORY,
                    now(), item.getId(), parentId, chr.getId(), item.getItemId(), signedQty, invType, operation, log);
        } catch (Throwable ignored) {
        }
    }

    // --------------------------------------------------------------- commands

    /** GM command log, called by the command dispatcher (ChatHandler). */
    public static void command(Char chr, String rawCommand, String result) {
        if (chr == null || rawCommand == null || !AuditWriter.enabled()) {
            return;
        }
        try {
            String text = "[" + result + "] acc=" + accountId(chr) + " map=" + mapId(chr) + " " + rawCommand;
            AuditWriter.submit(AuditSchema.ZLOG_COMMAND, now(), chr.getId(), text);
        } catch (Throwable ignored) {
        }
    }

    // ------------------------------------------------------------- anti-macro

    /**
     * Lie detector event.
     *
     * @param requesterId character id of the GM / player who requested it (0 = system/auto)
     * @param expected    expected answer (text on the image), may be null
     * @param reply       what the target typed, may be null
     * @param result      true = passed, false = failed, null = request only (not answered yet)
     * @param reason      REQUEST / REFRESH / WRONG_ANSWER / EMPTY_ANSWER / EVASION ...
     */
    public static void antiMacro(Char target, int requesterId, int attempt, String expected, String reply,
                                 Boolean result, String reason) {
        if (target == null || !AuditWriter.enabled()) {
            return;
        }
        try {
            AuditWriter.submit(AuditSchema.ZLOG_ANTIMACRO,
                    now(), accountId(target), target.getId(), requesterId, mapId(target), attempt,
                    expected, reply, result, reason);
        } catch (Throwable ignored) {
        }
    }

    // ------------------------------------------------------------------ misc

    public static String stats() {
        return String.format("audit enabled=%s pending=%d/%d written=%d dropped=%d failed=%d",
                AuditWriter.enabled(), AuditWriter.pending(), AuditWriter.capacity(),
                AuditWriter.written(), AuditWriter.dropped(), AuditWriter.failed());
    }

    static Timestamp now() {
        return new Timestamp(System.currentTimeMillis());
    }

    static int accountId(Char chr) {
        User user = chr.getUser();
        return user != null ? user.getId() : 0;
    }

    static int mapId(Char chr) {
        Field field = chr.getField();
        return field != null ? field.getId() : 0;
    }
}
