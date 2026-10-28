package net.swordie.ms.world.shop.cashshop;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.swordie.ms.ServerConstants;
import net.swordie.ms.loaders.Etc.Commodity.CommodityInfo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Server-side cash shop catalog rules, loaded once from {@code data/resources/tms282/cashshop}:
 * <ul>
 *     <li>{@code blocked-sn.txt}: commodity SNs that can never be bought (TMS279 {@code cs.block-commodity} + MetaMS 273
 *     {@code BlockedSN.json}); the 282 client hides them when they are sent in LP02F3 (5ED943E: i32 n, n x i32 SN, each
 *     found SN erased from the commodity array at 5ED9860..5ED997D);</li>
 *     <li>{@code commodity-modifies.json}: per-SN overrides (MetaMS 273 {@code CommodityModifies.json} and TMS279
 *     {@code cs.modified-commodity}; 279 wins on conflict). Blocked SNs win over overrides.</li>
 * </ul>
 * The rules are applied to every {@link CommodityInfo} the loaders build ({@link #apply}), so every purchase path
 * charges the overridden price and honours the overridden OnSale flag.
 */
public final class CashShopCatalogRules {
    /** One override; null fields are unchanged. */
    public record Modify(int sn, Integer itemId, Integer count, Integer price, Integer period, Boolean onSale,
                         Integer originalPrice, Integer discount, String source) {
        /** CS_COMMODITY::DecodeModify 779470 mask bits verified against the Commodity.img loader 77B7A0. */
        public static final long ITEM_ID = 0x1L, COUNT = 0x2L, PRICE = 0x4L, PERIOD = 0x20L, ON_SALE = 0x400L;

        public long clientMask() {
            long mask = 0;
            if (itemId != null) mask |= ITEM_ID;
            if (count != null) mask |= COUNT;
            if (price != null) mask |= PRICE;
            if (period != null) mask |= PERIOD;
            if (onSale != null) mask |= ON_SALE;
            return mask;
        }
    }

    private static volatile CashShopCatalogRules current;

    private final Set<Integer> blocked;
    private final Map<Integer, Modify> modifies;

    CashShopCatalogRules(Set<Integer> blocked, Map<Integer, Modify> modifies) {
        this.blocked = Collections.unmodifiableSet(new LinkedHashSet<>(blocked));
        Map<Integer, Modify> m = new LinkedHashMap<>(modifies);
        m.keySet().removeAll(this.blocked);
        this.modifies = Collections.unmodifiableMap(m);
    }

    private static final CashShopCatalogRules NONE = new CashShopCatalogRules(Set.of(), Map.of());

    /** The rules of the TMS 282.3 profile; GMS 265 has none (its catalog and packets stay unchanged). */
    public static CashShopCatalogRules get() {
        try {
            if (net.swordie.ms.connection.protocol.Protocol.active().version()
                    != net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) {
                return NONE;
            }
        } catch (RuntimeException e) {
            return NONE;
        }
        CashShopCatalogRules rules = current;
        if (rules == null) {
            synchronized (CashShopCatalogRules.class) {
                if (current == null) {
                    current = load(directory());
                }
                rules = current;
            }
        }
        return rules;
    }

    /** Re-reads the files (GM tools / tests). */
    public static synchronized CashShopCatalogRules reload() {
        current = load(directory());
        net.swordie.ms.loaders.CashShopData.clearCommodityIndexes();
        return get();
    }

    static Path directory() {
        return Path.of(System.getProperty("tms282.cashshopDir",
                ServerConstants.RESOURCES_DIR + "/tms282/cashshop"));
    }

    static CashShopCatalogRules load(Path dir) {
        Set<Integer> blocked = new LinkedHashSet<>();
        Map<Integer, Modify> modifies = new LinkedHashMap<>();
        Path blockFile = dir.resolve("blocked-sn.txt");
        try {
            if (Files.isRegularFile(blockFile)) {
                for (String line : Files.readAllLines(blockFile, StandardCharsets.UTF_8)) {
                    int hash = line.indexOf('#');
                    String value = (hash >= 0 ? line.substring(0, hash) : line).trim();
                    if (value.isEmpty()) continue;
                    try {
                        blocked.add(Integer.parseInt(value));
                    } catch (NumberFormatException e) {
                        System.out.printf("[CashShop] blocked-sn.txt: ignored '%s'%n", value);
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("[CashShop] blocked-sn.txt unreadable: " + e.getMessage());
        }
        Path modifyFile = dir.resolve("commodity-modifies.json");
        try {
            if (Files.isRegularFile(modifyFile)) {
                JsonNode root = new ObjectMapper().readTree(modifyFile.toFile());
                JsonNode list = root.isArray() ? root : root.path("modifies");
                for (JsonNode n : list) {
                    if (!n.hasNonNull("sn")) continue;
                    int sn = n.get("sn").asInt();
                    modifies.put(sn, new Modify(sn, integer(n, "itemId"), integer(n, "count"), integer(n, "price"),
                            integer(n, "period"), bool(n, "onSale"), integer(n, "originalPrice"),
                            n.hasNonNull("discount") ? (n.get("discount").isBoolean()
                                    ? (n.get("discount").asBoolean() ? 1 : 0) : n.get("discount").asInt()) : null,
                            n.path("source").asText("")));
                }
            }
        } catch (IOException | RuntimeException e) {
            System.out.println("[CashShop] commodity-modifies.json unreadable: " + e.getMessage());
        }
        CashShopCatalogRules rules = new CashShopCatalogRules(blocked, modifies);
        System.out.printf("[CashShop] catalog rules: %d blocked SNs, %d commodity overrides (%s)%n",
                rules.blocked.size(), rules.modifies.size(), dir);
        return rules;
    }

    private static Integer integer(JsonNode n, String field) {
        return n.hasNonNull(field) ? n.get(field).asInt() : null;
    }

    private static Boolean bool(JsonNode n, String field) {
        if (!n.hasNonNull(field)) return null;
        JsonNode v = n.get(field);
        return v.isBoolean() ? v.asBoolean() : v.asInt() != 0;
    }

    public static boolean isBlocked(int sn) {
        return get().blocked.contains(sn);
    }

    public Set<Integer> blocked() {
        return blocked;
    }

    public Map<Integer, Modify> modifies() {
        return modifies;
    }

    /**
     * Applies this SN's override (and the block list: a blocked commodity is not on sale) to a freshly loaded
     * commodity. Called by the loaders; the object is the cached instance.
     */
    public static CommodityInfo apply(CommodityInfo ci) {
        if (ci == null) return null;
        CashShopCatalogRules rules = get();
        if (rules.blocked.contains(ci.getSN())) {
            ci.setOnSale(0);
            return ci;
        }
        Modify m = rules.modifies.get(ci.getSN());
        if (m == null) return ci;
        if (m.itemId() != null) ci.setItemId(m.itemId());
        if (m.count() != null) ci.setCount(m.count());
        if (m.price() != null) ci.setPrice(m.price());
        if (m.period() != null) ci.setPeriod(m.period());
        if (m.onSale() != null) ci.setOnSale(m.onSale() ? 1 : 0);
        if (m.originalPrice() != null) ci.setOriginalPrice(m.originalPrice());
        if (m.discount() != null) ci.setDiscount(m.discount());
        return ci;
    }
}
