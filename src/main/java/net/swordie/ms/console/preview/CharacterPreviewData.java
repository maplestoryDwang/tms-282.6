package net.swordie.ms.console.preview;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.avatar.AvatarData;
import net.swordie.ms.client.character.avatar.AvatarLook;
import net.swordie.ms.client.character.items.Equip;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.console.datamanage.ConsoleCharacterService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

/** Read-only snapshot of a character's look and worn equipment: live objects when online, database otherwise. */
public final class CharacterPreviewData {
    /** Cash slots are the normal body part + 100 (101 cap .. 115 ring4). */
    public static final int CASH_OFFSET = 100;

    public record Snapshot(String name, int level, int job, boolean online, AvatarRenderer.Look look,
                           Map<Integer, Equip> normal, Map<Integer, Equip> cash) { }

    private CharacterPreviewData() { }

    public static Snapshot load(String name) throws Exception {
        ConsoleCharacterService.PlayerTarget target = ConsoleCharacterService.resolve(name);
        if (target == null) throw new IllegalArgumentException("找不到角色：" + name);
        Char chr = target.online;
        if (chr != null && chr.getAvatarData() != null) {
            List<Item> worn = new ArrayList<>(chr.getEquippedInventory().getItems());
            return build(chr.getName(), chr.getLevel(), chr.getJob(), true, chr.getAvatarData().getAvatarLook(), worn);
        }
        int avatarDataId, equippedInventoryId;
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT avatardata, equippedinventory FROM characters WHERE id = ?")) {
            ps.setInt(1, target.characterId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException("角色資料不存在：" + name);
                avatarDataId = rs.getInt("avatardata");
                equippedInventoryId = rs.getInt("equippedinventory");
            }
        }
        AvatarData avatar = AvatarData.getAvatarDataFromSQLByID(avatarDataId);
        if (avatar == null || avatar.getAvatarLook() == null) throw new IllegalStateException("角色外觀資料讀取失敗：" + name);
        List<Item> worn = equippedInventoryId > 0 ? Item.getItemsFromSQLByInventoryID(equippedInventoryId) : List.of();
        var stat = avatar.getCharacterStat();
        return build(target.name, stat == null ? 0 : stat.getLevel(), stat == null ? 0 : stat.getJob(), false, avatar.getAvatarLook(), worn);
    }

    static Snapshot build(String name, int level, int job, boolean online, AvatarLook look, List<Item> worn) {
        Map<Integer, Equip> normal = new TreeMap<>(), cash = new TreeMap<>();
        for (Item item : worn) {
            if (!(item instanceof Equip equip)) continue;
            int slot = Math.abs(equip.getBagIndex());
            if (slot > CASH_OFFSET && slot < CASH_OFFSET + 20) cash.put(slot - CASH_OFFSET, equip);
            else normal.put(slot, equip);
        }
        // What other players see: a cash item overrides the normal one in the same body part.
        Map<Integer, Integer> visible = new TreeMap<>();
        normal.forEach((slot, e) -> visible.put(slot, e.getItemId()));
        cash.forEach((slot, e) -> visible.put(slot, e.getItemId()));
        if (look.getWeaponStickerId() > 0) visible.put(11, look.getWeaponStickerId());
        AvatarRenderer.Look renderLook = new AvatarRenderer.Look(look.getSkin(), look.getFace(), look.getHair(),
                look.isDrawElfEar(), visible);
        return new Snapshot(name, level, job, online, renderLook, normal, cash);
    }
}
