package net.swordie.ms.world.tms282.mail;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.Inventory;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.enums.InvType;
import net.swordie.ms.util.DataPrinter;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Facade for NPC scripts (data/scripts/npc/9010009.py 篤伊): plain ints / strings in, player text out.
 * Menu strings use the NPC dialog #L..#l syntax.
 */
public final class DueyScriptApi {
    private DueyScriptApi() { }

    public static MailResult send(Char chr, String receiver, long money, int invType, int slot, int quantity,
                                  String memo, boolean quick) {
        InvType inv = invType <= 0 ? null : InvType.getInvTypeByVal(invType);
        if (invType > 0 && inv == null) return MailResult.fail("這個欄位的道具無法寄送。");
        return ParcelService.get().send(chr, receiver, money, inv, slot, quantity, memo, quick);
    }

    public static MailResult receive(Char chr, int parcelId) {
        return ParcelService.get().receive(chr, parcelId);
    }

    public static MailResult refuse(Char chr, int parcelId) {
        return ParcelService.get().refuse(chr, parcelId);
    }

    public static long fee(long money) {
        return ParcelFees.fee(money);
    }

    /** "#L<id>#..." menu of this character's parcels, or "" when none. */
    public static String parcelMenu(Char chr) {
        try {
            List<Parcel> all = ParcelService.get().list(chr);
            LocalDateTime now = LocalDateTime.now();
            StringBuilder sb = new StringBuilder();
            for (Parcel p : all) {
                sb.append("\r\n#L").append(p.id()).append('#');
                if (p.hasItem()) sb.append("#v").append(p.itemId()).append("# ");
                sb.append(ParcelService.describe(p, now)).append("#l");
            }
            return sb.toString();
        } catch (SQLException e) {
            DataPrinter.send(DataPrinter.HIKARICP_ERROR, e);
            return "";
        }
    }

    /** "#L<slot>#..." menu of the items in one inventory (1 裝備, 2 消耗, 3 裝飾, 4 其他), or "" when empty. */
    public static String itemMenu(Char chr, int invType) {
        InvType type = InvType.getInvTypeByVal(invType);
        Inventory inv = type == null ? null : chr.getInventoryByType(type);
        if (inv == null) return "";
        StringBuilder sb = new StringBuilder();
        inv.getItems().stream().sorted(Comparator.comparingInt(Item::getBagIndex)).forEach(item ->
                sb.append("\r\n#L").append(item.getBagIndex()).append("##v").append(item.getItemId()).append("# #t")
                        .append(item.getItemId()).append("# x").append(item.getQuantity()).append("#l"));
        return sb.toString();
    }

    public static int quantityAt(Char chr, int invType, int slot) {
        InvType type = InvType.getInvTypeByVal(invType);
        Inventory inv = type == null ? null : chr.getInventoryByType(type);
        Item item = inv == null ? null : inv.getItemBySlot(slot);
        return item == null ? 0 : item.getQuantity();
    }
}
