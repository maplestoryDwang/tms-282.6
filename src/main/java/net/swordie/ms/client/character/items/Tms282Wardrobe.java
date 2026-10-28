package net.swordie.ms.client.character.items;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;
import net.swordie.ms.constants.ItemConstants;
import net.swordie.ms.enums.InvType;
import net.swordie.ms.loaders.ItemData;
import java.util.Set;

/** Free cosmetic selection; fresh template copies, no replacement or permanent skill grants. */
public final class Tms282Wardrobe {
    private static final Set<Integer> CATEGORIES = Set.of(100,101,102,103,104,105,106,107,108,109,110,170);
    private Tms282Wardrobe() { }

    public static boolean eligible(int itemId, int gender) {
        if (Protocol.active().version() != ProtocolVersion.TMS_282_3 || !CATEGORIES.contains(itemId / 10000)
                || !Tms282WardrobeCatalog.IDS.contains(itemId)) return false;
        Equip template = ItemData.getEquipById(itemId);
        int requiredGender = ItemConstants.getGenderFromId(itemId);
        return template != null && template.isCash()
                && (requiredGender == 2 || gender == 2 || gender == requiredGender);
    }

    public static boolean owned(Char chr, int id) {
        return java.util.stream.Stream.of(chr.getEquippedInventory(),chr.getEquipInventory(),chr.getDecorationInventory())
                .flatMap(inv -> inv.getItems().stream()).anyMatch(item -> item.getItemId() == id);
    }

    public static Equip prepare(Char chr, int id) {
        if (chr == null || !eligible(id,chr.getAvatarData().getAvatarLook().getGender())) throw new IllegalArgumentException("此時裝資料不存在或不符合角色性別。");
        if (owned(chr,id)) throw new IllegalArgumentException("你已持有這件時裝，不會重複領取。");
        if (!chr.canHold(id,1)) throw new IllegalArgumentException("請先在裝飾欄空出一格。");
        Equip equip = ItemData.getEquipDeepCopyFromID(id,false);
        if (equip == null || equip.getId() != 0 || !equip.isCash() || equip.getInvType() != InvType.DECORATION)
            throw new IllegalArgumentException("無法建立全新時裝，已取消領取。");
        return equip;
    }

    public static void claim(Char chr, int id) {
        if (chr == null) throw new IllegalArgumentException("角色不存在。");
        synchronized (net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime.ECONOMY_MONITOR) { synchronized (chr) { synchronized (chr.getDecorationInventory()) { // multiplayer-0930 lock order
            Equip equip = prepare(chr,id); // Recheck source, gender, duplicates and the correct inventory at commit.
            chr.addItemToInventory(equip);
        } } }
    }
}
