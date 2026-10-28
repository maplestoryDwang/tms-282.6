package net.swordie.ms.world.tms282.boxes;

import net.swordie.ms.Server;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.connection.packet.Tms282ItemAvailability;
import net.swordie.ms.connection.packet.UserLocal;
import net.swordie.ms.constants.ItemConstants;
import net.swordie.ms.enums.ChatType;
import net.swordie.ms.loaders.ItemData;
import net.swordie.ms.loaders.StringData;
import net.swordie.ms.util.FileTime;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Shared helpers for the box handlers: TSV loading, 282 item availability, reward creation and notices. */
final class BoxSupport {
    private static volatile Tms282ItemAvailability availability;

    private BoxSupport() {
    }

    static Path dataDir() {
        return Path.of(System.getProperty("tms282.boxesDir", "data/resources/tms282"));
    }

    /**
     * Reads a UTF-8 TSV: skips blank lines, '#' comments and the header row (first column not numeric).
     * Every returned row has at least {@code minCols} columns (missing trailing columns are empty strings).
     */
    static List<String[]> readTsv(Path file, int minCols) {
        List<String[]> rows = new ArrayList<>();
        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        for (String line : lines) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            String[] raw = line.split("\t", -1);
            if (raw[0].isEmpty() || !Character.isDigit(raw[0].charAt(0))) {
                continue; // header
            }
            String[] row = new String[Math.max(minCols, raw.length)];
            for (int i = 0; i < row.length; i++) {
                row[i] = i < raw.length ? raw[i].trim() : "";
            }
            rows.add(row);
        }
        return rows;
    }

    static int intOr(String s, int def) {
        return s == null || s.isEmpty() ? def : Integer.parseInt(s);
    }

    /** Item exists in the server data and was not removed from the 282 client. */
    static boolean available(int itemId) {
        Tms282ItemAvailability a = availability;
        if (a == null) {
            synchronized (BoxSupport.class) {
                if (availability == null) {
                    availability = Tms282ItemAvailability.load();
                }
                a = availability;
            }
        }
        if (!a.isAvailable(itemId)) {
            return false;
        }
        if (ItemConstants.isEquip(itemId)) {
            return ItemData.getEquipById(itemId) != null;
        }
        if (ItemConstants.isPet(itemId)) {
            return ItemData.getPetInfoByID(itemId) != null;
        }
        return ItemData.getItemInfoByID(itemId) != null;
    }

    /** Cash-shop outfits of the other gender are skipped (gender 2 = unisex). */
    static boolean genderAllows(Char chr, int itemId) {
        if (!ItemConstants.isEquip(itemId)) {
            return true;
        }
        int g = ItemConstants.getGenderFromId(itemId);
        return g == 2 || g == chr.getAvatarData().getAvatarLook().getGender();
    }

    /** @return a fresh reward item, or null when the server has no data for it. */
    static Item createItem(int itemId, int amount, int periodDays) {
        Item item = ItemData.getItemDeepCopy(itemId);
        if (item == null) {
            return null;
        }
        if (!ItemConstants.isEquip(itemId) && !ItemConstants.isPet(itemId)) {
            item.setQuantity(Math.max(1, amount));
        }
        if (periodDays > 0) {
            item.setDateExpire(FileTime.fromDate(LocalDateTime.now().plusDays(periodDays)));
        }
        return item;
    }

    /** As {@link #createItem} with a WZ {@code reward/period} in minutes (265 ItemInfo.getRandomReward). */
    static Item createItemMinutes(int itemId, int amount, int periodMinutes) {
        Item item = createItem(itemId, amount, 0);
        if (item != null && periodMinutes > 0) {
            item.setDateExpire(FileTime.fromDate(LocalDateTime.now().plusMinutes(periodMinutes)));
        }
        return item;
    }

    /**
     * Whether the item has an item script file (info/spec script, consume_&lt;id&gt; or &lt;id&gt;, as
     * ItemHandler.resolveScriptItemName): a written script always wins over the derived box tables.
     */
    static boolean hasItemScript(int itemId) {
        net.swordie.ms.loaders.containerclasses.ItemInfo ii = ItemData.getItemInfoByID(itemId);
        if (ii != null && ii.getScript() != null && !ii.getScript().isEmpty()
                && net.swordie.ms.scripts.ScriptManagerImpl.hasScriptFile(net.swordie.ms.scripts.ScriptType.Item, ii.getScript())) {
            return true;
        }
        return net.swordie.ms.scripts.ScriptManagerImpl.hasScriptFile(net.swordie.ms.scripts.ScriptType.Item, "consume_" + itemId)
                || net.swordie.ms.scripts.ScriptManagerImpl.hasScriptFile(net.swordie.ms.scripts.ScriptType.Item, String.valueOf(itemId));
    }

    /** The box is still the item in its bag slot with at least one left (a dialog may have run in between). */
    static boolean stillOwned(Char chr, Item box) {
        if (box == null || box.getQuantity() < 1 || box.getInvType() == null) {
            return false;
        }
        var inv = chr.getInventoryByType(box.getInvType());
        return inv != null && inv.getItemBySlot(box.getBagIndex()) == box;
    }

    /**
     * Gives {@code gains} and {@code meso} for one {@code box}: checks bag space and the meso cap first, then consumes
     * the box, then adds the rewards. Nothing is consumed when a check fails.
     *
     * @return whether the box was opened
     */
    static boolean giveAndConsume(Char chr, Item box, List<Item> gains, long meso) {
        int boxId = box.getItemId();
        if (!stillOwned(chr, box)) {
            chr.chatMessage(ChatType.SystemNotice, "找不到要使用的道具，未消耗任何道具。");
            return false;
        }
        if (!chr.canHold(gains)) {
            chr.chatMessage(ChatType.SystemNotice, "請先保留足夠的背包空間，未消耗此道具。");
            return false;
        }
        if (meso > 0 && !chr.canAddMoney(meso)) {
            chr.chatMessage(ChatType.SystemNotice, "楓幣已達上限，未消耗此道具。");
            return false;
        }
        chr.consumeItem(box, 1);
        for (Item gain : gains) {
            String what = describe(gain);
            chr.addItemToInventory(gain);
            chr.chatMessage(ChatType.GameDesc, String.format("從 %s 獲得了 %s。", itemName(boxId), what));
        }
        if (meso > 0) {
            chr.addMoney(meso);
            chr.chatMessage(ChatType.GameDesc, String.format("從 %s 獲得了 %,d 楓幣。", itemName(boxId), meso));
        }
        return true;
    }

    static void refuseNoData(Char chr) {
        chr.chatMessage(ChatType.SystemNotice, "獎勵資料尚未就緒，未消耗此道具。");
    }

    static String itemName(int itemId) {
        String name = StringData.getItemStringById(itemId);
        return name == null ? String.valueOf(itemId) : name;
    }

    static String describe(Item item) {
        String name = itemName(item.getItemId());
        return item.getQuantity() > 1 ? name + " x" + item.getQuantity() : name;
    }

    /** World-wide notice in Traditional Chinese (CHAT_MSG is mapped for 282; BROADCAST_MSG is not). */
    static void announce(Char chr, int boxId, Item reward) {
        String msg = String.format("[%s] 恭喜 %s 獲得了 %s！", itemName(boxId), chr.getName(), describe(reward));
        Server.get().broadcastForWorld(UserLocal.chatMsg(ChatType.Notice2, msg));
    }
}
