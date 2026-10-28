package net.swordie.ms.world.tms282.mail;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.Inventory;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.client.character.items.Tms282ParcelItemRows;
import net.swordie.ms.client.character.tms282.Tms282CubeRuntime;
import net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime;
import net.swordie.ms.connection.packet.WvsContext;
import net.swordie.ms.constants.GameConstants;
import net.swordie.ms.constants.ItemConstants;
import net.swordie.ms.enums.InvType;
import net.swordie.ms.enums.InventoryOperation;
import net.swordie.ms.loaders.ItemData;
import net.swordie.ms.loaders.StringData;
import net.swordie.ms.loaders.containerclasses.ItemInfo;
import net.swordie.ms.util.DataPrinter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Duey parcels: send meso and/or one item (normal or quick delivery, with fee), list, receive, refuse (return),
 * expiry (return to sender once; a returned parcel that expires again is destroyed).
 * <p>
 * Consistency: every change is one InnoDB transaction (parcel row + item row custody + characterstats.money)
 * committed before the live Char is changed, under the same ECONOMY_MONITOR -> Char lock order that
 * Char.addMoney / addItemToInventory use on 282, so a concurrent save cannot write a stale inventory back.
 */
public final class ParcelService {
    public static final int MAX_PARCELS_PER_RECEIVER = 50;
    public static final int MAX_MEMO = 100;
    public static final Duration KEEP = Duration.ofDays(30);
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
    private static final ParcelService INSTANCE = new ParcelService(MailNotifier.CHAT);

    private final MailNotifier notifier;

    public ParcelService(MailNotifier notifier) {
        this.notifier = notifier;
    }

    public static ParcelService get() {
        return INSTANCE;
    }

    /** Minutes before a normal (non-quick) parcel can be collected; system property tms282.parcel.normalDelayMinutes. */
    static Duration normalDelay() {
        return Duration.ofMinutes(Math.max(0, Long.getLong("tms282.parcel.normalDelayMinutes", 0L)));
    }

    // ------------------------------------------------------------------ send

    /**
     * @param invType  inventory of the item, or null for a meso-only parcel
     * @param slot     bag slot of the item
     * @param quantity how many of the stack (equips / pets: 1)
     */
    public MailResult send(Char sender, String receiverName, long money, InvType invType, int slot, int quantity,
                           String memo, boolean quick) {
        ParcelExpiry.ensureStarted();
        if (sender == null) return MailResult.fail("無法寄送包裹。");
        String name = receiverName == null ? "" : receiverName.strip();
        String note = memo == null ? "" : memo.strip();
        if (name.isEmpty()) return MailResult.fail("請輸入收件角色名稱。");
        if (name.equalsIgnoreCase(sender.getName())) return MailResult.fail("不能寄包裹給自己。");
        if (money < 0) return MailResult.fail("楓幣數量不正確。");
        if (note.length() > MAX_MEMO) return MailResult.fail("包裹附言最多 " + MAX_MEMO + " 個字。");
        if (money == 0 && invType == null) return MailResult.fail("請放入道具或楓幣。");
        try {
            return locked(sender, () -> sendLocked(sender, name, money, invType, slot, quantity, note, quick));
        } catch (IllegalStateException | IllegalArgumentException e) {
            return MailResult.fail(e.getMessage() != null ? e.getMessage() : "目前無法寄送包裹。");
        }
    }

    private MailResult sendLocked(Char sender, String name, long money, InvType invType, int slot, int quantity,
                                  String note, boolean quick) {
        Item item = null;
        Inventory inventory = null;
        if (invType != null) {
            if (invType != InvType.EQUIP && invType != InvType.CONSUME && invType != InvType.INSTALL
                    && invType != InvType.ETC) {
                return MailResult.fail("這個欄位的道具無法寄送。");
            }
            inventory = sender.getInventoryByType(invType);
            item = inventory == null ? null : inventory.getItemBySlot(slot);
            MailResult bad = checkSendable(sender, item, quantity);
            if (bad != null) return bad;
        }
        long fee = ParcelFees.fee(money);
        long total = money + fee;
        if (money > GameConstants.MAX_MONEY || sender.getMoney() < total) {
            return MailResult.fail("楓幣不足。需要 " + String.format("%,d", total) + " 楓幣（含手續費 "
                    + String.format("%,d", fee) + "）。");
        }
        if (quick && !sender.hasItem(ParcelFees.QUICK_DELIVERY_TICKET)) {
            return MailResult.fail("快速宅配需要快速宅配券。");
        }
        boolean whole = item != null && quantity == item.getQuantity();
        // Stack / slot changes of the live inventory reach SQL only on save; flush this row so the guarded
        // custody UPDATEs below compare against the live state.
        if (item != null) item.saveToSQL();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime available = quick ? now : now.plus(normalDelay());
        MailRecipient r;
        int parcelId;
        try (Connection c = MailSchema.connection()) {
            c.setAutoCommit(false);
            try {
                Optional<MailRecipient> found = MailRecipient.byName(c, name);
                if (found.isEmpty()) { c.rollback(); return MailResult.fail("找不到角色「" + name + "」。"); }
                r = found.get();
                if (r.worldId() != MailRecipient.worldIdOf(sender)) {
                    c.rollback(); return MailResult.fail("只能寄包裹給同一個世界的角色。");
                }
                if (r.userId() == MailRecipient.userIdOf(sender) || r.characterId() == sender.getId()) {
                    c.rollback(); return MailResult.fail("不能寄包裹給同一個帳號的角色。");
                }
                if (ParcelStore.countFor(c, r.characterId()) >= MAX_PARCELS_PER_RECEIVER) {
                    c.rollback(); return MailResult.fail("「" + r.name() + "」的包裹箱已滿。");
                }
                // InnoDB parcel row first (uncommitted), then the legacy (Aria, non-transactional) items and
                // characterstats rows, each with an explicit undo, then commit.
                Parcel p = new Parcel(0, now, available, r.characterId(), r.name(), sender.getName(), sender.getId(),
                        sender.getId(), money, available.plus(KEEP), quick ? Parcel.QUICK : Parcel.NORMAL, note,
                        0, item != null ? item.getItemId() : 0, item != null ? quantity : 0);
                parcelId = ParcelStore.insert(c, p);
                LegacyUndo undo = new LegacyUndo(c, sender, item);
                try {
                    if (item != null) {
                        long itemRow;
                        if (whole) {
                            if (!Tms282ParcelItemRows.detach(c, item, sender.getId())) {
                                c.rollback(); return MailResult.fail("道具資料已變動，請稍後再試。");
                            }
                            undo.detached = true;
                            itemRow = item.getId();
                        } else {
                            if (!Tms282ParcelItemRows.reduceStack(c, item, quantity)) {
                                c.rollback(); return MailResult.fail("道具資料已變動，請稍後再試。");
                            }
                            undo.reduced = quantity;
                            itemRow = Tms282ParcelItemRows.insertDetachedStackPart(c, item, quantity);
                            undo.partRow = itemRow;
                        }
                        ParcelStore.setItemRow(c, parcelId, itemRow);
                    }
                    setMoney(c, sender.getId(), sender.getMoney() - total);
                    undo.moneyWritten = true;
                    c.commit();
                } catch (SQLException | RuntimeException e) {
                    undo.run();
                    throw e;
                }
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            DataPrinter.send(DataPrinter.HIKARICP_ERROR, e);
            return MailResult.fail("包裹寄送失敗，請稍後再試。");
        }
        // Committed: mirror it on the live character.
        if (item != null) {
            if (whole) {
                inventory.removeItem(item);
                item.setCharID(0);
                sender.write(WvsContext.inventoryOperation(true, false, InventoryOperation.Remove,
                        (short) item.getBagIndex(), (byte) 0, 0, item));
            } else {
                item.setQuantity(item.getQuantity() - quantity);
                sender.write(WvsContext.inventoryOperation(true, false, InventoryOperation.UpdateQuantity,
                        (short) item.getBagIndex(), (byte) -1, 0, item));
            }
        }
        sender.deductMoney(total);
        if (quick) sender.consumeItem(ParcelFees.QUICK_DELIVERY_TICKET, 1);
        DataPrinter.send(DataPrinter.ITEM, String.format("parcel %d %s(%d) -> %s(%d) meso=%d fee=%d item=%d x%d quick=%b",
                parcelId, sender.getName(), sender.getId(), r.name(), r.characterId(), money, fee,
                item != null ? item.getItemId() : 0, item != null ? quantity : 0, quick), true);
        if (!available.isAfter(LocalDateTime.now())) {
            r.online().ifPresent(online -> notifier.parcelsWaiting(online, availableCount(online), quick));
        }
        return MailResult.ok((quick ? "已使用快速宅配寄給「" : "已寄給「") + r.name() + "」。手續費 "
                + String.format("%,d", fee) + " 楓幣。");
    }

    /** Null when the item may go into a parcel, otherwise the refusal. */
    static MailResult checkSendable(Char sender, Item item, int quantity) {
        if (item == null) return MailResult.fail("找不到要寄送的道具。");
        if (quantity <= 0 || quantity > item.getQuantity()) return MailResult.fail("道具數量不正確。");
        boolean unique = ItemConstants.isEquip(item.getItemId()) || ItemConstants.isPet(item.getItemId());
        if (unique && quantity != item.getQuantity()) return MailResult.fail("道具數量不正確。");
        if (item.getId() <= 0) return MailResult.fail("道具資料尚未存檔，請稍後再試。");
        if (item.isCash()) return MailResult.fail("點數商品無法寄送。");
        if (ItemConstants.isPet(item.getItemId())) return MailResult.fail("寵物無法寄送。");
        ItemInfo info = ItemData.getItemInfoByID(item.getItemId());
        if (!unique && info == null) return MailResult.fail("這個道具無法寄送。");
        if (info != null && info.isQuest()) return MailResult.fail("任務道具無法寄送。");
        if (!item.isTradable()) return MailResult.fail("無法交易的道具不能寄送。");
        if (item.getDateExpire() != null && item.getDateExpire().isExpired()) return MailResult.fail("已到期的道具無法寄送。");
        if (item.getAuctionHouseStatus() != Item.AuctionHouseStatus.NORMAL) return MailResult.fail("拍賣中的道具無法寄送。");
        Tms282CubeRuntime.requireMovable(sender, item); // throws IllegalArgumentException with its own text
        return null;
    }

    // ------------------------------------------------------------------ receive / list / return

    public List<Parcel> list(Char chr) throws SQLException {
        try (Connection c = MailSchema.connection()) {
            return ParcelStore.listFor(c, chr.getId());
        }
    }

    public MailResult listText(Char chr) {
        try {
            List<Parcel> all = list(chr);
            if (all.isEmpty()) return MailResult.ok("沒有任何包裹。");
            LocalDateTime now = LocalDateTime.now();
            StringBuilder sb = new StringBuilder("包裹列表（" + all.size() + "）：");
            for (Parcel p : all) sb.append("\r\n").append(describe(p, now));
            return MailResult.ok(sb.toString());
        } catch (SQLException e) {
            DataPrinter.send(DataPrinter.HIKARICP_ERROR, e);
            return MailResult.fail("讀取包裹失敗。");
        }
    }

    static String describe(Parcel p, LocalDateTime now) {
        StringBuilder sb = new StringBuilder("#").append(p.id()).append(' ');
        if (p.deliveryType() == Parcel.RETURNED) sb.append("[退件] ");
        else if (p.deliveryType() == Parcel.QUICK) sb.append("[快速] ");
        sb.append("寄件人 ").append(p.sender());
        if (p.hasItem()) {
            String itemName = StringData.getItemStringById(p.itemId());
            sb.append("，").append(itemName != null ? itemName : String.valueOf(p.itemId())).append(" x").append(p.quantity());
        }
        if (p.money() > 0) sb.append("，").append(String.format("%,d", p.money())).append(" 楓幣");
        if (!p.available(now)) sb.append("（").append(DAY.format(p.availableAt())).append(" 送達）");
        else sb.append("（保管至 ").append(DAY.format(p.expire())).append("）");
        if (p.memo() != null && !p.memo().isEmpty()) sb.append(" 附言：").append(p.memo());
        return sb.toString();
    }

    public MailResult receive(Char chr, int parcelId) {
        ParcelExpiry.ensureStarted();
        try {
            return locked(chr, () -> receiveLocked(chr, parcelId));
        } catch (IllegalStateException | IllegalArgumentException e) {
            return MailResult.fail(e.getMessage() != null ? e.getMessage() : "目前無法領取包裹。");
        }
    }

    private MailResult receiveLocked(Char chr, int parcelId) {
        Parcel parcel;
        Item item = null;
        try (Connection c = MailSchema.connection()) {
            c.setAutoCommit(false);
            try {
                Optional<Parcel> found = ParcelStore.lockForReceiver(c, parcelId, chr.getId());
                if (found.isEmpty()) { c.rollback(); return MailResult.fail("找不到這個包裹。"); }
                parcel = found.get();
                LocalDateTime now = LocalDateTime.now();
                if (!parcel.available(now)) { c.rollback(); return MailResult.fail("包裹還在運送中。"); }
                if (!parcel.expire().isAfter(now)) { c.rollback(); return MailResult.fail("包裹已過保管期限。"); }
                if (parcel.money() > 0 && chr.getMoney() + parcel.money() > GameConstants.MAX_MONEY) {
                    c.rollback(); return MailResult.fail("持有楓幣已達上限，無法領取。");
                }
                if (parcel.hasItem()) {
                    item = Tms282ParcelItemRows.load(c, parcel.itemRow());
                    if (item == null) { c.rollback(); return MailResult.fail("包裹內的道具無法讀取，請聯絡管理員。"); }
                    if (!chr.canHold(List.of(item))) { c.rollback(); return MailResult.fail("背包空間不足。"); }
                }
                if (!ParcelStore.delete(c, parcel.id(), chr.getId())) { c.rollback(); return MailResult.fail("找不到這個包裹。"); }
                c.commit(); // the InnoDB parcel row is the claim; legacy (Aria) rows follow it
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
            c.setAutoCommit(true);
            try {
                if (parcel.hasItem()) Tms282ParcelItemRows.delete(c, parcel.itemRow());
                if (parcel.money() > 0) setMoney(c, chr.getId(), chr.getMoney() + parcel.money());
            } catch (SQLException e) {
                // The parcel is already claimed; the live grant below still happens and the next save persists it.
                DataPrinter.send(DataPrinter.HIKARICP_ERROR, e);
            }
        } catch (SQLException e) {
            DataPrinter.send(DataPrinter.HIKARICP_ERROR, e);
            return MailResult.fail("領取包裹失敗，請稍後再試。");
        }
        if (item != null) {
            item.setId(0);
            item.setInventoryID(0);
            item.setCharID(0);
            item.setBagIndex(0);
            chr.addItemToInventory(item);
        }
        if (parcel.money() > 0) chr.addMoney(parcel.money());
        DataPrinter.send(DataPrinter.ITEM, String.format("parcel %d received by %s(%d) meso=%d item=%d x%d",
                parcel.id(), chr.getName(), chr.getId(), parcel.money(), parcel.itemId(), parcel.quantity()), true);
        return MailResult.ok("已領取包裹 #" + parcel.id() + "。");
    }

    /** The receiver refuses a parcel: it goes back to the sender (a returned parcel cannot be refused again). */
    public MailResult refuse(Char chr, int parcelId) {
        try (Connection c = MailSchema.connection()) {
            c.setAutoCommit(false);
            try {
                Optional<Parcel> found = ParcelStore.lockForReceiver(c, parcelId, chr.getId());
                if (found.isEmpty()) { c.rollback(); return MailResult.fail("找不到這個包裹。"); }
                Parcel p = found.get();
                if (p.deliveryType() == Parcel.RETURNED) { c.rollback(); return MailResult.fail("退件包裹無法再退回。"); }
                LocalDateTime now = LocalDateTime.now();
                if (!ParcelStore.returnToSender(c, p, now, now.plus(KEEP))) {
                    c.rollback(); return MailResult.fail("找不到這個包裹。");
                }
                c.commit();
                notifyReturned(c, p);
                return MailResult.ok("已將包裹 #" + p.id() + " 退回給「" + p.sender() + "」。");
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            DataPrinter.send(DataPrinter.HIKARICP_ERROR, e);
            return MailResult.fail("退回包裹失敗。");
        }
    }

    void notifyReturned(Connection c, Parcel p) {
        Char online = net.swordie.ms.Server.get().getChars().get(p.senderId());
        if (online != null) notifier.parcelsWaiting(online, availableCount(online), false);
    }

    /** Field-entry: expire what is due, then announce what can be collected. */
    public void onEnterField(Char chr) {
        ParcelExpiry.ensureStarted();
        notifier.parcelsWaiting(chr, availableCount(chr), false);
    }

    int availableCount(Char chr) {
        try {
            LocalDateTime now = LocalDateTime.now();
            return (int) list(chr).stream().filter(p -> p.available(now) && p.expire().isAfter(now)).count();
        } catch (SQLException e) {
            DataPrinter.send(DataPrinter.HIKARICP_ERROR, e);
            return 0;
        }
    }

    /** Undo of the non-transactional legacy rows touched by a send whose InnoDB commit did not happen. */
    private static final class LegacyUndo {
        final Connection c;
        final Char sender;
        final Item item;
        boolean detached;
        int reduced;
        long partRow;
        boolean moneyWritten;

        LegacyUndo(Connection c, Char sender, Item item) {
            this.c = c;
            this.sender = sender;
            this.item = item;
        }

        void run() {
            try {
                if (detached) Tms282ParcelItemRows.reattach(c, item, sender.getId());
                if (partRow > 0) Tms282ParcelItemRows.delete(c, partRow);
                if (reduced > 0) Tms282ParcelItemRows.restoreStack(c, item, reduced);
                if (moneyWritten) setMoney(c, sender.getId(), sender.getMoney());
            } catch (SQLException | RuntimeException e) {
                DataPrinter.send(DataPrinter.ITEM, "parcel send undo failed for " + sender.getName() + " item row "
                        + (item != null ? item.getId() : 0) + ": " + e, true);
            }
        }
    }

    // ------------------------------------------------------------------ helpers

    static void setMoney(Connection c, int characterId, long money) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE characterstats SET money = ? WHERE characterid = ?")) {
            ps.setLong(1, Math.max(0, Math.min(GameConstants.MAX_MONEY, money)));
            ps.setInt(2, characterId);
            ps.executeUpdate();
        }
    }

    /** ECONOMY_MONITOR -> Char, the order Char.addMoney / addItemToInventory take on 282. */
    static <T> T locked(Char chr, Supplier<T> body) {
        if (Tms282VMatrixRuntime.economyEnabled()) {
            synchronized (Tms282VMatrixRuntime.ECONOMY_MONITOR) {
                synchronized (chr) {
                    Tms282VMatrixRuntime.requireEconomicAccess(chr);
                    return body.get();
                }
            }
        }
        synchronized (chr) {
            return body.get();
        }
    }
}
