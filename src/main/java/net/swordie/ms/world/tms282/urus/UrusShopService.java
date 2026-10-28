package net.swordie.ms.world.tms282.urus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.swordie.ms.ServerConstants;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.constants.ItemConstants;
import net.swordie.ms.util.DataPrinter;
import net.swordie.ms.world.tms282.dojo.WeekKeys;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;

/**
 * Urus (烏勒斯) coin shop of NPC 艾霞 (9070101), data from MetaMS 273 {@code UrusShop.json}
 * ({@code data/resources/tms282/urus/urus-shop.json}). Prices are in Urus coins: the JSON keys are the 273 coin
 * quest ids (502105..502116) which {@code questItem} maps to the coin items (4009345..4009356) spent here.
 * {@code maxLimit} is a per-character weekly limit (week = Monday 00:00, {@link WeekKeys}).
 */
public final class UrusShopService {
    public static final int NPC_ID = 9070101;

    public record ShopItem(int index, int itemId, int periodMinutes, int weeklyLimit, Map<Integer, Integer> cost) {
    }

    public enum Status { SUCCESS, NOT_REQUIRE, NOT_SLOT, LIMIT, ERROR }

    public record Result(Status status, String message) {
    }

    private static volatile UrusShopService instance;

    private final List<ShopItem> items;

    private UrusShopService(List<ShopItem> items) {
        this.items = List.copyOf(items);
    }

    public static UrusShopService get() {
        UrusShopService s = instance;
        if (s == null) {
            synchronized (UrusShopService.class) {
                if (instance == null) instance = load();
                s = instance;
            }
        }
        return s;
    }

    public static synchronized void reload() {
        instance = load();
    }

    private static UrusShopService load() {
        Path path = Path.of(ServerConstants.RESOURCES_DIR, "tms282", "urus", "urus-shop.json");
        try {
            JsonNode root = new ObjectMapper().readTree(path.toFile());
            Map<String, Integer> coinItems = new HashMap<>();
            root.path("questItem").fields().forEachRemaining(e -> coinItems.put(e.getKey(), e.getValue().asInt()));
            List<ShopItem> items = new ArrayList<>();
            for (JsonNode n : root.path("shopItem")) {
                Map<Integer, Integer> cost = new LinkedHashMap<>();
                n.path("require").fields().forEachRemaining(e -> {
                    Integer coin = coinItems.get(e.getKey());
                    if (coin == null) throw new IllegalArgumentException("Unknown Urus coin " + e.getKey());
                    cost.merge(coin, e.getValue().asInt(), Integer::sum);
                });
                items.add(new ShopItem(items.size(), n.path("itemId").asInt(), n.path("period").asInt(0),
                        n.path("maxLimit").asInt(0), Collections.unmodifiableMap(cost)));
            }
            return new UrusShopService(items);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load " + path, e);
        }
    }

    public List<ShopItem> items() {
        return items;
    }

    public ShopItem item(int index) {
        return index >= 0 && index < items.size() ? items.get(index) : null;
    }

    /** Purchases left this week, or Integer.MAX_VALUE when unlimited; -1 on database error. */
    public int remaining(Char chr, ShopItem item) {
        if (item.weeklyLimit() <= 0) return Integer.MAX_VALUE;
        try {
            return Math.max(0, item.weeklyLimit() - UrusPurchaseLedger.count(chr.getId(), item.itemId(), WeekKeys.current()));
        } catch (SQLException e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
            return -1;
        }
    }

    /** How many the character could afford by coins alone. */
    public int affordable(Char chr, ShopItem item) {
        int max = Integer.MAX_VALUE;
        for (Map.Entry<Integer, Integer> e : item.cost().entrySet()) {
            if (e.getValue() <= 0) continue;
            max = Math.min(max, InventoryCost.count(chr, e.getKey()) / e.getValue());
        }
        return max == Integer.MAX_VALUE ? 0 : max;
    }

    public String costText(ShopItem item, int quantity) {
        StringBuilder sb = new StringBuilder();
        item.cost().forEach((coin, n) -> sb.append("#v").append(coin).append("# #t").append(coin).append("# x ")
                .append((long) n * quantity).append("\r\n"));
        return sb.toString();
    }

    public String menuText(Char chr) {
        StringBuilder sb = new StringBuilder("請選擇要交換的道具（每週一 00:00 重置購買次數）：\r\n#b");
        for (ShopItem item : items) {
            int left = remaining(chr, item);
            sb.append("#L").append(item.index()).append("##v").append(item.itemId()).append("# #t").append(item.itemId()).append("#");
            if (item.periodMinutes() > 0) sb.append(" (").append(periodText(item.periodMinutes())).append(")");
            if (left != Integer.MAX_VALUE) sb.append(" 本週剩餘 ").append(Math.max(left, 0)).append("/").append(item.weeklyLimit());
            sb.append("#l\r\n");
        }
        return sb.toString();
    }

    public static String periodText(int minutes) {
        return minutes % 1440 == 0 ? (minutes / 1440) + "天" : minutes % 60 == 0 ? (minutes / 60) + "小時" : minutes + "分鐘";
    }

    public Result buy(Char chr, int index, int quantity) {
        ShopItem item = item(index);
        if (item == null || quantity <= 0) return new Result(Status.ERROR, "發生未知錯誤，請重試。");
        int week = WeekKeys.current();
        synchronized (net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime.ECONOMY_MONITOR) { synchronized (chr) { // multiplayer-0930 lock order
            if (!InventoryCost.has(chr, item.cost(), quantity)) {
                return new Result(Status.NOT_REQUIRE, "用於交換的材料不夠呢, 能再確認一下所需的材料再來交易嗎? ");
            }
            if (!chr.canHold(item.itemId(), quantity)) {
                return new Result(Status.NOT_SLOT, "背包欄位空間不足，請檢查。");
            }
            boolean limited = item.weeklyLimit() > 0;
            try {
                if (limited && !UrusPurchaseLedger.reserve(chr.getId(), item.itemId(), week, quantity, item.weeklyLimit())) {
                    return new Result(Status.LIMIT, "這個道具本週的交換次數已達上限，請下週再來。");
                }
            } catch (SQLException e) {
                DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
                return new Result(Status.ERROR, "發生未知錯誤，請重試。");
            }
            try {
                item.cost().forEach((coin, n) -> InventoryCost.consume(chr, coin, n * quantity));
            } catch (RuntimeException e) {
                DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
                if (limited) {
                    try {
                        UrusPurchaseLedger.cancel(chr.getId(), item.itemId(), week, quantity);
                    } catch (SQLException ignored) {
                    }
                }
                return new Result(Status.ERROR, "發生未知錯誤，請重試。");
            }
            give(chr, item, quantity);
            DataPrinter.send(DataPrinter.ITEM, String.format("烏勒斯商店：%s(%d) 交換 %d x%d，週 %d。",
                    chr.getName(), chr.getId(), item.itemId(), quantity, week), true);
            return new Result(Status.SUCCESS, "交易成功了, 希望日後也要多多使用哦. ");
        } }
    }

    private static void give(Char chr, ShopItem item, int quantity) {
        boolean equip = ItemConstants.isEquip(item.itemId());
        int times = equip ? quantity : 1;
        int each = equip ? 1 : quantity;
        for (int i = 0; i < times; i++) {
            if (item.periodMinutes() > 0) {
                chr.addItemToInventory(item.itemId(), each, "minute", item.periodMinutes());
            } else {
                chr.addItemToInventory(item.itemId(), each);
            }
        }
    }
}
