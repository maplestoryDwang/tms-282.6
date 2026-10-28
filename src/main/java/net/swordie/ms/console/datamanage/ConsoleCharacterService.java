package net.swordie.ms.console.datamanage;

import net.swordie.ms.Server;
import net.swordie.ms.client.User;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.CharacterStat;
import net.swordie.ms.client.character.avatar.AvatarData;
import net.swordie.ms.client.character.items.Equip;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.connection.packet.Effect;
import net.swordie.ms.connection.packet.UserRemote;
import net.swordie.ms.connection.packet.WvsContext;
import net.swordie.ms.constants.GameConstants;
import net.swordie.ms.constants.ItemConstants;
import net.swordie.ms.enums.AccountType;
import net.swordie.ms.enums.Stat;
import net.swordie.ms.loaders.ItemData;
import net.swordie.ms.loaders.StringData;
import net.swordie.ms.world.field.Field;
import net.swordie.ms.world.field.Portal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 控制檯對在線/離線角色的統一操作。
 */
public final class ConsoleCharacterService {
    private static final int LIST_LIMIT = 500;

    private ConsoleCharacterService() {
    }

    public static final class PlayerTarget {
        public final String name;
        public final int characterId;
        public final Char online;

        PlayerTarget(String name, int characterId, Char online) {
            this.name = name;
            this.characterId = characterId;
            this.online = online;
        }

        public boolean isOnline() {
            return online != null && online.getClient() != null;
        }
    }

    public static final class OpResult {
        public final boolean success;
        public final String message;

        private OpResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public static OpResult ok(String message) {
            return new OpResult(true, message);
        }

        public static OpResult fail(String message) {
            return new OpResult(false, message);
        }
    }

    public static int listLimit() {
        return LIST_LIMIT;
    }

    public static PlayerTarget resolve(int characterId, String name) {
        if (characterId <= 0) {
            return null;
        }
        String resolvedName = name;
        if (resolvedName == null || resolvedName.isBlank()) {
            resolvedName = lookupName(characterId);
        }
        if (resolvedName == null || resolvedName.isBlank()) {
            return null;
        }
        return new PlayerTarget(resolvedName, characterId, findOnline(resolvedName));
    }

    public static PlayerTarget resolve(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        int id = lookupCharacterId(name.trim());
        if (id <= 0) {
            return null;
        }
        return new PlayerTarget(name.trim(), id, findOnline(name.trim()));
    }

    public static Char findOnline(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        Char byId = Server.get().getChars().values().stream()
                .filter(c -> c.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
        if (byId != null) {
            return byId;
        }
        return Server.get().getWorld() == null ? null : Server.get().getWorld().getCharByName(name);
    }

    public static Set<String> onlineNames() {
        Set<String> names = new HashSet<>();
        for (Char chr : Server.get().getChars().values()) {
            names.add(chr.getName());
        }
        return names;
    }

    public static boolean itemExists(int itemId) {
        if (itemId <= 0) {
            return false;
        }
        if (ItemData.getItemInfoByID(itemId) != null) {
            return true;
        }
        return ItemData.getItemDeepCopy(itemId) != null;
    }

    public static String itemName(int itemId) {
        String name = StringData.getItemStringById(itemId);
        return name == null || name.isBlank() ? "?" : name;
    }

    public static List<GiveItemDialog.ItemRow> searchItems(String keyword, int limit) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        String key = keyword.trim();
        String lower = key.toLowerCase(Locale.ROOT);
        String digitsOnly = key.replaceAll("[^0-9]", "");
        List<GiveItemDialog.ItemRow> rows = new ArrayList<>();

        if (!digitsOnly.isEmpty()) {
            try {
                int exactId = Integer.parseInt(digitsOnly);
                if (itemExists(exactId)) {
                    rows.add(new GiveItemDialog.ItemRow(exactId, itemName(exactId)));
                }
            } catch (NumberFormatException ignored) {
            }
        }

        Map<Integer, String> byName = StringData.getItemStringByName(lower);
        for (Map.Entry<Integer, String> entry : byName.entrySet()) {
            if (rows.size() >= limit) {
                break;
            }
            int id = entry.getKey();
            String name = entry.getValue();
            boolean matchName = name != null && name.toLowerCase(Locale.ROOT).contains(lower);
            boolean matchId = !digitsOnly.isEmpty() && String.valueOf(id).contains(digitsOnly);
            if (!matchName && !matchId) {
                continue;
            }
            if (!itemExists(id)) {
                continue;
            }
            boolean dup = false;
            for (GiveItemDialog.ItemRow row : rows) {
                if (row.itemId == id) {
                    dup = true;
                    break;
                }
            }
            if (!dup) {
                rows.add(new GiveItemDialog.ItemRow(id, name == null ? "?" : name));
            }
        }
        return rows.size() > limit ? rows.subList(0, limit) : rows;
    }

    public static OpResult giveItem(PlayerTarget target, int itemId, int quantity) {
        GiveItemSpec spec = new GiveItemSpec();
        spec.quantity = quantity;
        return giveItem(target, itemId, spec);
    }

    public static OpResult giveItem(PlayerTarget target, int itemId, GiveItemSpec spec) {
        if (target == null) {
            return OpResult.fail("未找到角色。");
        }
        if (spec == null) {
            return OpResult.fail("發物參數無效。");
        }
        int quantity = spec.quantity;
        if (quantity <= 0) {
            return OpResult.fail("數量必須大於 0。");
        }
        if (!itemExists(itemId)) {
            return OpResult.fail("物品 ID " + itemId + " 不存在。");
        }
        try {
            if (target.isOnline()) {
                return giveItemOnline(target.online, itemId, spec);
            }
            return giveItemOffline(target.characterId, itemId, spec);
        } catch (Exception ex) {
            return OpResult.fail("發放失敗: " + safeMessage(ex));
        }
    }

    public static OpResult giveMeso(PlayerTarget target, long amount) {
        if (target == null) {
            return OpResult.fail("未找到角色。");
        }
        if (amount == 0) {
            return OpResult.fail("數量不能爲 0。");
        }
        try {
            if (target.isOnline()) {
                target.online.addMoney(amount);
                target.online.chatMessage("[系統] 管理員已調整金幣 " + (amount > 0 ? "+" : "") + amount);
                return OpResult.ok("已發放金幣 " + amount + "（在線即時到賬）");
            }
            try (Connection con = DatabaseManager.getConnection();
                 PreparedStatement ps = con.prepareStatement(
                         "UPDATE characterstats SET money = money + ? WHERE characterid = ? AND money + ? >= 0 AND money + ? <= ?")) {
                ps.setLong(1, amount);
                ps.setInt(2, target.characterId);
                ps.setLong(3, amount);
                ps.setLong(4, amount);
                ps.setLong(5, GameConstants.MAX_MONEY);
                if (ps.executeUpdate() <= 0) {
                    return OpResult.fail("金幣更新失敗（可能超出上限或爲負）。");
                }
            }
            return OpResult.ok("已寫入金幣 " + amount + "（離線，下次登錄生效）");
        } catch (Exception ex) {
            return OpResult.fail("發放金幣失敗: " + safeMessage(ex));
        }
    }

    public static OpResult giveMaplePoints(PlayerTarget target, int amount) {
        return giveUserPoints(target, "maplePoints", amount, "楓點", true);
    }

    public static OpResult giveNxPrepaid(PlayerTarget target, int amount) {
        return giveUserPoints(target, "nxPrepaid", amount, "樂豆點", false);
    }

    public static OpResult giveDonationPoints(PlayerTarget target, int amount) {
        return giveUserPoints(target, "donationpoints", amount, "贊助積分", false);
    }

    public static OpResult giveVotePoints(PlayerTarget target, int amount) {
        return giveUserPoints(target, "votepoints", amount, "投票積分", false);
    }

    public static OpResult setAccountType(PlayerTarget target, AccountType type) {
        if (target == null) {
            return OpResult.fail("未找到角色。");
        }
        if (type == null) {
            return OpResult.fail("無效的賬號類型。");
        }
        try {
            if (target.isOnline() && target.online.getUser() != null) {
                target.online.getUser().setAccountType(type);
                target.online.getUser().updateUserToSQL();
                target.online.chatMessage("[系統] 您的賬號權限已設置爲 " + accountTypeLabel(type));
                return OpResult.ok("已將「" + target.name + "」設爲 " + accountTypeLabel(type) + "（在線即時生效）");
            }
            int userId = resolveUserId(target.characterId);
            if (userId <= 0) {
                return OpResult.fail("無法解析角色所屬賬號。");
            }
            try (Connection con = DatabaseManager.getConnection();
                 PreparedStatement ps = con.prepareStatement("UPDATE users SET accounttype = ? WHERE id = ?")) {
                ps.setInt(1, type.getVal());
                ps.setInt(2, userId);
                if (ps.executeUpdate() <= 0) {
                    return OpResult.fail("數據庫更新失敗。");
                }
            }
            return OpResult.ok("已將「" + target.name + "」設爲 " + accountTypeLabel(type) + "（下次登錄生效）");
        } catch (Exception ex) {
            return OpResult.fail("設置賬號權限失敗: " + safeMessage(ex));
        }
    }

    public static CharacterStatsSnapshot loadCharacterStats(PlayerTarget target) {
        if (target == null) {
            return null;
        }
        try {
            if (target.isOnline()) {
                CharacterStat cs = target.online.getAvatarData().getCharacterStat();
                return new CharacterStatsSnapshot(
                        target.online.getLevel(),
                        cs.getStr(),
                        cs.getDex(),
                        cs.getInt(),
                        cs.getLuk(),
                        cs.getMaxHp(),
                        cs.getMaxMp());
            }
            return snapshotFromDatabase(target.characterId);
        } catch (Exception ex) {
            return null;
        }
    }

    public static OpResult updateCharacterStats(PlayerTarget target, CharacterStatsSnapshot requested) {
        if (target == null) {
            return OpResult.fail("未找到角色。");
        }
        if (requested == null) {
            return OpResult.fail("屬性數據無效。");
        }
        CharacterStatsSnapshot after = CharacterStatsSnapshot.clamp(requested);
        CharacterStatsSnapshot before = loadCharacterStats(target);
        if (before != null && snapshotsEqual(before, after)) {
            return OpResult.fail("屬性與當前一致，無需保存。");
        }
        try {
            OpResult applied = target.isOnline()
                    ? applyStatsOnline(target.online, after)
                    : applyStatsOffline(target.characterId, after);
            if (!applied.success) {
                return applied;
            }
            String summary = after.diffFrom(before);
            return OpResult.ok("已保存屬性修改。\n" + summary
                    + (target.isOnline() ? "\n（在線玩家已即時生效）" : "\n（離線，下次登錄生效）"));
        } catch (Exception ex) {
            return OpResult.fail("保存屬性失敗: " + safeMessage(ex));
        }
    }

    public static OpResult kick(PlayerTarget target) {
        if (target == null || !target.isOnline()) {
            return OpResult.fail("該玩家不在線。");
        }
        try {
            if (target.online.getClient() != null) {
                target.online.getClient().getCh().close();
            }
            return OpResult.ok("已斷開「" + target.name + "」的連接。");
        } catch (Exception ex) {
            return OpResult.fail("踢下線失敗: " + safeMessage(ex));
        }
    }

    public static OpResult warpToFreeMarket(PlayerTarget target) {
        return warpToMap(target, 910000000);
    }

    public static OpResult warpToMap(PlayerTarget target, int mapId) {
        if (target == null || !target.isOnline()) {
            return OpResult.fail("該玩家不在線，無法傳送。");
        }
        if (mapId <= 0) {
            return OpResult.fail("地圖 ID 無效。");
        }
        try {
            Char chr = target.online;
            if (chr.getClient() == null || chr.getClient().getChannelInstance() == null) {
                return OpResult.fail("玩家頻道信息不可用。");
            }
            Field toField = chr.getClient().getChannelInstance().getField(mapId);
            if (toField == null) {
                return OpResult.fail("地圖 " + mapId + " 未加載。");
            }
            Portal portal = toField.getDefaultPortal();
            chr.warp(toField, portal);
            return OpResult.ok("已將「" + target.name + "」傳送到地圖 " + mapId + "。");
        } catch (Exception ex) {
            return OpResult.fail("傳送失敗: " + safeMessage(ex));
        }
    }

    public static String accountTypeLabel(AccountType type) {
        if (type == null) {
            return "未知";
        }
        return switch (type) {
            case Player -> "普通玩家";
            case Tester -> "測試員";
            case Intern -> "實習生";
            case GameMaster -> "遊戲管理員";
            case Admin -> "超級管理員";
        };
    }

    public static AccountType readAccountType(int characterId) {
        try {
            int userId = resolveUserId(characterId);
            if (userId <= 0) {
                return AccountType.Player;
            }
            try (Connection con = DatabaseManager.getConnection();
                 PreparedStatement ps = con.prepareStatement("SELECT accounttype FROM users WHERE id = ?")) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        AccountType type = AccountType.getByVal(rs.getInt("accounttype"));
                        return type == null ? AccountType.Player : type;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return AccountType.Player;
    }

    private static OpResult giveItemOnline(Char chr, int itemId, GiveItemSpec spec) {
        OpResult[] result = {OpResult.fail("角色已離線，請重新選取。")};
        net.swordie.ms.client.character.tms282.EconomyLocks.packet(chr, () -> {
            if (Server.get().getChars().get(chr.getId()) != chr || !chr.isOnline() || chr.getClient() == null
                    || chr.getClient().getChr() != chr || chr.getClient().getCh() == null || !chr.getClient().getCh().isActive()) return null;
            List<Item> prepared = prepareItems(itemId, spec);
            if (prepared == null) {result[0] = OpResult.fail("無法創建物品 " + itemId); return null;}
            if (!chr.canHold(prepared)) {result[0] = OpResult.fail("背包空間不足。"); return null;}
            if (!ItemConstants.isEquip(itemId) && ItemConstants.isSymbol(itemId)) {
                chr.getScriptManager().giveSymbol(itemId, (short) spec.quantity);
            } else {
                if (!ItemConstants.isEquip(itemId) && spec.periodDays <= 0
                        && !net.swordie.ms.world.tms282.items.Tms282ItemAvailability.allowGrant(chr, itemId, "ConsoleCharacterService.giveItem")) {
                    result[0] = OpResult.fail("目前版本無法發放該物品。"); return null;
                }
                for (Item item : prepared) chr.addItemToInventory(item.getInvType(), item, false, false);
            }
            chr.chatMessage("[系統] 管理員已發放: " + itemName(itemId) + (spec.quantity > 1 ? " x" + spec.quantity : ""));
            result[0] = OpResult.ok("已發送 " + itemId + " " + itemName(itemId) + " x" + spec.quantity + "（在線即時到賬）");
            return null;
        });
        return result[0];
    }

    private static OpResult giveItemOffline(int characterId, int itemId, GiveItemSpec spec) throws SQLException {
        if (Thread.holdsLock(net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime.ECONOMY_MONITOR)
                && !net.swordie.ms.client.character.tms282.EconomyLocks.gateHeld(characterId))
            return OpResult.fail("無法在此鎖定狀態發放離線物品。");
        return net.swordie.ms.client.character.tms282.EconomyLocks.withGate(characterId, () -> {
            if (Server.get().getChars().containsKey(characterId)) return OpResult.fail("角色已上線，請重新選取。");
            Char chr = loadOfflineChar(characterId);
            if (chr == null) return OpResult.fail("無法加載離線角色數據。");
            if (Server.get().getChars().containsKey(characterId) || findOnline(chr.getName()) != null)
                return OpResult.fail("角色已上線，請重新選取。");
            OpResult[] result = {OpResult.fail("背包空間不足。")};
            net.swordie.ms.client.character.tms282.EconomyLocks.packet(chr, () -> {
                List<Item> prepared = prepareItems(itemId, spec);
                if (prepared == null) {result[0] = OpResult.fail("無法創建物品 " + itemId); return null;}
                if (!chr.canHold(prepared)) return null;
                for (Item item : prepared) chr.addItemToInventory(item.getInvType(), item, false, false);
                result[0] = OpResult.ok("已寫入 " + itemId + " " + itemName(itemId) + " x" + spec.quantity + "（離線，下次登錄生效）");
                return null;
            });
            if (result[0].success) chr.saveToSQL(); // Existing save gate -> Char sequence, outside ECONOMY_MONITOR.
            return result[0];
        });
    }

    private static List<Item> prepareItems(int itemId, GiveItemSpec spec) {
        List<Item> items = new ArrayList<>();
        if (ItemConstants.isEquip(itemId)) {
            if (spec.quantity > Math.max(GameConstants.MAX_INVENTORY_SLOTS, GameConstants.MAX_CASH_INVENTORY_SLOTS)) return null;
            for (int i = 0; i < spec.quantity; i++) {
                Equip equip = EquipGrantHelper.createEquip(itemId, spec);
                equip.setQuantity(1);
                items.add(equip);
            }
        } else {
            Item item = ItemData.getItemDeepCopy(itemId);
            if (item == null) return null;
            if (item instanceof net.swordie.ms.client.character.items.PetItem pet && pet.getName() == null)
                pet.setName(itemName(itemId));
            item.setQuantity(spec.quantity);
            items.add(item);
        }
        if (spec.periodDays > 0) {
            EquipGrantHelper.applyPeriod(items.getFirst(), spec.periodDays);
            var expiry = items.getFirst().getDateExpire();
            for (int i = 1; i < items.size(); i++) items.get(i).setDateExpire(expiry.deepCopy());
        }
        return items;
    }

    private static Char loadOfflineChar(int characterId) throws SQLException {
        Char chr = new Char();
        chr.setId(characterId);
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT avatardata FROM characters WHERE id = ?")) {
            ps.setInt(1, characterId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                int avatarDataId = rs.getInt("avatardata");
                if (avatarDataId != 0) {
                    chr.setAvatarData(AvatarData.getAvatarDataFromSQLByID(avatarDataId));
                }
            }
        }
        if (chr.getAvatarData() == null) {
            return null;
        }
        chr.loadCharacterData();
        return chr;
    }

    private static OpResult giveUserPoints(PlayerTarget target, String column, int amount, String label, boolean notifyMaplePointPacket) {
        if (target == null) {
            return OpResult.fail("未找到角色。");
        }
        if (amount == 0) {
            return OpResult.fail("數量不能爲 0。");
        }
        try {
            if (target.isOnline() && target.online.getUser() != null) {
                User user = target.online.getUser();
                applyUserPointDelta(user, column, amount);
                user.updateUserToSQL();
                if (notifyMaplePointPacket) {
                    target.online.write(WvsContext.setMaplePoint(user.getMaplePoints()));
                }
                target.online.chatMessage("[系統] 管理員已調整" + label + " " + (amount > 0 ? "+" : "") + amount);
                return OpResult.ok("已發放" + label + " " + amount + "（在線即時到賬）");
            }
            int userId = resolveUserId(target.characterId);
            if (userId <= 0) {
                return OpResult.fail("無法解析角色所屬賬號。");
            }
            String sql = "UPDATE users SET `" + column + "` = `" + column + "` + ? WHERE id = ? AND `" + column + "` + ? >= 0";
            try (Connection con = DatabaseManager.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, amount);
                ps.setInt(2, userId);
                ps.setInt(3, amount);
                if (ps.executeUpdate() <= 0) {
                    return OpResult.fail("發放" + label + "失敗（餘額不足或已達上限）。");
                }
            }
            return OpResult.ok("已寫入" + label + " " + amount + "（離線，下次登錄生效）");
        } catch (Exception ex) {
            return OpResult.fail("發放" + label + "失敗: " + safeMessage(ex));
        }
    }

    private static void applyUserPointDelta(User user, String column, int amount) {
        switch (column) {
            case "maplePoints" -> user.addMaplePoints(amount);
            case "nxPrepaid" -> user.addNXPrepaid(amount);
            case "donationpoints" -> user.addDonationPoint(amount);
            case "votepoints" -> user.addVotePoint(amount);
            default -> throw new IllegalArgumentException("Unknown column: " + column);
        }
    }

    private static OpResult applyStatsOnline(Char chr, CharacterStatsSnapshot snap) {
        if (chr.getLevel() != snap.level) {
            chr.setStatAndSendPacket(Stat.level, snap.level);
            chr.setStatAndSendPacket(Stat.exp, 0);
            chr.getJobHandler().handleLevelUp((short) snap.level);
            if (chr.getField() != null) {
                chr.getField().broadcast(UserRemote.effect(chr.getId(), Effect.levelUpEffect()));
            }
        }
        chr.setStatAndSendPacket(Stat.str, snap.str);
        chr.setStatAndSendPacket(Stat.dex, snap.dex);
        chr.setStatAndSendPacket(Stat.inte, snap.inte);
        chr.setStatAndSendPacket(Stat.luk, snap.luk);
        chr.setStatAndSendPacket(Stat.mhp, snap.maxHp);
        chr.setStatAndSendPacket(Stat.mmp, snap.maxMp);
        chr.setStatAndSendPacket(Stat.hp, chr.getMaxHP());
        chr.setStatAndSendPacket(Stat.mp, chr.getMaxMP());
        chr.getAvatarData().getCharacterStat().updateCharacterStatToSQL();
        chr.chatMessage("[系統] 管理員已調整您的角色屬性。");
        return OpResult.ok("在線屬性已應用");
    }

    private static OpResult applyStatsOffline(int characterId, CharacterStatsSnapshot snap) throws SQLException {
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE characterstats SET level = ?, str = ?, dex = ?, inte = ?, luk = ?, maxhp = ?, maxmp = ?, hp = ?, mp = ? WHERE characterid = ?")) {
            ps.setInt(1, snap.level);
            ps.setInt(2, snap.str);
            ps.setInt(3, snap.dex);
            ps.setInt(4, snap.inte);
            ps.setInt(5, snap.luk);
            ps.setInt(6, snap.maxHp);
            ps.setInt(7, snap.maxMp);
            ps.setInt(8, snap.maxHp);
            ps.setInt(9, snap.maxMp);
            ps.setInt(10, characterId);
            if (ps.executeUpdate() <= 0) {
                return OpResult.fail("數據庫未更新角色屬性。");
            }
        }
        return OpResult.ok("離線屬性已寫入數據庫");
    }

    private static CharacterStatsSnapshot snapshotFromDatabase(int characterId) throws SQLException {
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT level, str, dex, inte, luk, maxhp, maxmp FROM characterstats WHERE characterid = ?")) {
            ps.setInt(1, characterId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new CharacterStatsSnapshot(
                        rs.getInt("level"),
                        rs.getInt("str"),
                        rs.getInt("dex"),
                        rs.getInt("inte"),
                        rs.getInt("luk"),
                        rs.getInt("maxhp"),
                        rs.getInt("maxmp"));
            }
        }
    }

    private static int lookupCharacterId(String name) {
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT characterid FROM characterstats WHERE name = ?")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("characterid");
                }
            }
        } catch (Exception ignored) {
        }
        return -1;
    }

    private static String lookupName(int characterId) {
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT name FROM characterstats WHERE characterid = ?")) {
            ps.setInt(1, characterId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("name");
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static int resolveUserId(int characterId) throws SQLException {
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT a.userid FROM characters c INNER JOIN accounts a ON c.accid = a.id WHERE c.id = ?")) {
            ps.setInt(1, characterId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("userid");
                }
            }
        }
        return -1;
    }

    private static boolean snapshotsEqual(CharacterStatsSnapshot a, CharacterStatsSnapshot b) {
        return a.level == b.level && a.str == b.str && a.dex == b.dex && a.inte == b.inte && a.luk == b.luk
                && a.maxHp == b.maxHp && a.maxMp == b.maxMp;
    }

    private static String safeMessage(Exception ex) {
        String msg = ex.getMessage();
        return msg == null || msg.isBlank() ? ex.getClass().getSimpleName() : msg;
    }
}
