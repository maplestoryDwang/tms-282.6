package net.swordie.ms.world.shop;

import net.swordie.ms.connection.hikariCP.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CustomShopData {

    private static final Map<String, Integer> shopKeyToId = new LinkedHashMap<>();
    private static final Map<String, String> shopWelcomeNames = new LinkedHashMap<>();
    private static final Map<Integer, List<CustomShopItem>> shopItems = new LinkedHashMap<>();
    /** getItems() view: shopItems minus ids the 282 client lacks / the server has no data for (plan B1). */
    private static final Map<Integer, List<CustomShopItem>> availableItems =
            new java.util.concurrent.ConcurrentHashMap<>();

    public static void load() {
        shopKeyToId.clear();
        shopWelcomeNames.clear();
        shopItems.clear();
        availableItems.clear();
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT id, shop_key, welcome_name FROM custom_shops WHERE enabled = 1 ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("id");
                String key = rs.getString("shop_key");
                shopKeyToId.put(key, id);
                String welcome = rs.getString("welcome_name");
                shopWelcomeNames.put(key, welcome != null ? welcome : key);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }

        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT shop_id, item_id, price, currency_type, period_ms, item_flag "
                             + "FROM custom_shop_items ORDER BY shop_id, sort_order, id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int shopId = rs.getInt("shop_id");
                CustomShopItem item = new CustomShopItem(
                        rs.getInt("item_id"),
                        rs.getLong("price"),
                        rs.getInt("currency_type"),
                        rs.getLong("period_ms"),
                        rs.getInt("item_flag")
                );
                shopItems.computeIfAbsent(shopId, k -> new ArrayList<>()).add(item);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        System.out.printf("[CustomShopData] Loaded %d shops, %d total items%n",
                shopKeyToId.size(), shopItems.values().stream().mapToInt(List::size).sum());
    }

    public static List<CustomShopItem> getItems(String shopKey) {
        Integer shopId = shopKeyToId.get(shopKey);
        if (shopId == null) {
            return Collections.emptyList();
        }
        // Filtered lazily (item data / 282 profile are ready by the first shop visit). Script indices and
        // ScriptManagerImpl.buyCustomShopItem both use this same list, so "sel" stays consistent.
        return availableItems.computeIfAbsent(shopId, id -> List.copyOf(
                net.swordie.ms.world.tms282.items.Tms282ItemAvailability.filter(
                        shopItems.getOrDefault(id, Collections.emptyList()), CustomShopItem::getItemId,
                        "custom shop " + shopKey)));
    }

    public static String getWelcomeName(String shopKey) {
        return shopWelcomeNames.getOrDefault(shopKey, shopKey);
    }

    public static boolean hasShop(String shopKey) {
        return shopKeyToId.containsKey(shopKey);
    }
}
