package net.swordie.ms.loaders;

import net.swordie.ms.ServerConstants;
import net.swordie.ms.loaders.Etc.Commodity.CommodityInfo;
import net.swordie.ms.util.*;
import org.w3c.dom.Node;

import java.io.*;
import java.text.NumberFormat;
import java.util.*;

public class CashShopData {

    public static List<CommodityInfo> commodities = new ArrayList<>();
    private static final Map<String, Map<Integer, CommodityInfo>> commodityIndexes = new java.util.concurrent.ConcurrentHashMap<>();

    /** Catalog overrides mutate cached commodities; reload must re-read raw sale/price values as well. */
    public static void clearCommodityIndexes() { commodityIndexes.clear(); }

    public static List<CommodityInfo> getCommodities() {
        return commodities;
    }

    public static CommodityInfo getCommodityBySN(int SN) {
        if (net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) return loadCommodityBySN(SN);
        for (CommodityInfo ci : getCommodities()) {
            if (ci.getSN() == SN) {
                return ci;
            }
        }
        return loadCommodityBySN(SN);
    }

    /**
     * Scans the commodity layers of the active data source for {@code SN}, the version overlay first: TMS 282.3 reads
     * {@code data/wz282/Etc.wz/Commodity.img.xml} (the 282 client's SNs; exported by data282-import CommodityCommand)
     * before data/wz265; GMS 265 has the one base file, as before.
     */
    public static CommodityInfo loadCommodityBySN(int SN) {
        List<File> layers = net.swordie.ms.loaders.overlay.DataOverlays.current().wzLayers("Etc.wz/Commodity.img.xml");
        for (int i = layers.size() - 1; i >= 0; i--) {
            CommodityInfo found = loadCommodityBySN(layers.get(i), SN);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static CommodityInfo loadCommodityBySN(File file, int SN) {
        return commodityIndexes.computeIfAbsent(file.getAbsolutePath(), ignored -> readCommodityIndex(file)).get(SN);
    }

    private static Map<Integer, CommodityInfo> readCommodityIndex(File file) {
        Map<Integer, CommodityInfo> index = new HashMap<>();
        Node root = XMLApi.getRoot(file);
        Node firstNode = XMLApi.getAllChildren(root).getFirst();
        List<Node> nodes = XMLApi.getAllChildren(firstNode);
        for (Node node : nodes) {
            int id = Integer.parseInt(XMLApi.getNamedAttribute(node, "name"));
            CommodityInfo ci = new CommodityInfo();
            ci.setId(id);
            List<Node> nodes1 = XMLApi.getAllChildren(node);
            for (Node node1 : nodes1) {
                String name = XMLApi.getNamedAttribute(node1, "name");
                String value = XMLApi.getNamedAttribute(node1, "value");
                switch (name) {
                    case "LimitMax":
                        ci.setLimitMax(Integer.parseInt(value));
                        break;
                    case "MonthlyLimited":
                        ci.setMonthlyLimited(Integer.parseInt(value) != 0);
                        break;
                    case "onlyMileage":
                        ci.setOnlyMileage(Integer.parseInt(value) != 0);
                        break;
                    case "SN":
                        ci.setSN(Integer.parseInt(value));
                        break;
                    case "ItemId":
                        ci.setItemId(Integer.parseInt(value));
                        break;
                    case "Count":
                        ci.setCount(Integer.parseInt(value));
                        break;
                    case "Price":
                        ci.setPrice(Integer.parseInt(value));
                        break;
                    case "Period":
                        ci.setPeriod(Integer.parseInt(value));
                        break;
                    case "gameWorld":
                        ci.setGameWorld(value);
                        break;
                    case "originalPrice":
                        ci.setOriginalPrice(Integer.parseInt(value));
                        break;
                    case "Gender":
                        ci.setGender(Integer.parseInt(value));
                        break;
                    case "OnSale":
                        ci.setOnSale(Integer.parseInt(value));
                        break;
                    case "discount":
                        ci.setDiscount(Integer.parseInt(value));
                        break;
                    case "termStart":
                        ci.setTermStart(value);
                        break;
                    case "termEnd":
                        ci.setTermEnd(value);
                        break;
                }
            }
            index.put(ci.getSN(), net.swordie.ms.world.shop.cashshop.CashShopCatalogRules.apply(ci));
        }
        return Map.copyOf(index);
    }

    public static void load() {
        File file = new File(String.format("%s/Etc.wz/Commodity.img.xml", ServerConstants.WZ_DIR));
        Node root = XMLApi.getRoot(file);
        Node firstNode = XMLApi.getAllChildren(root).getFirst();
        List<Node> nodes = XMLApi.getAllChildren(firstNode);
        for (Node node : nodes) {
            int id = Integer.parseInt(XMLApi.getNamedAttribute(node, "name"));
            CommodityInfo ci = new CommodityInfo();
            ci.setId(id);
            List<Node> nodes1 = XMLApi.getAllChildren(node);
            for (Node node1 : nodes1) {
                String name = XMLApi.getNamedAttribute(node1, "name");
                String value = XMLApi.getNamedAttribute(node1, "value");
                switch (name) {
                    case "SN":
                        ci.setSN(Integer.parseInt(value));
                        break;
                    case "ItemId":
                        ci.setItemId(Integer.parseInt(value));
                        break;
                    case "Count":
                        ci.setCount(Integer.parseInt(value));
                        break;
                    case "Price":
                        ci.setPrice(Integer.parseInt(value));
                        break;
                    case "Period":
                        ci.setPeriod(Integer.parseInt(value));
                        break;
                    case "gameWorld":
                        ci.setGameWorld(value);
                        break;
                    case "originalPrice":
                        ci.setOriginalPrice(Integer.parseInt(value));
                        break;
                    case "Gender":
                        ci.setGender(Integer.parseInt(value));
                        break;
                    case "OnSale":
                        ci.setOnSale(Integer.parseInt(value));
                        break;
                    case "discount":
                        ci.setDiscount(Integer.parseInt(value));
                        break;
                    case "termStart":
                    case "termEnd":
                        ci.setTerm(1);
                        break;
                }
            }
            // Blocked SNs / commodity overrides (data/resources/tms282/cashshop) after the WZ values.
            getCommodities().add(net.swordie.ms.world.shop.cashshop.CashShopCatalogRules.apply(ci));
        }
    }

    public static void main(String[] args) {
        StringData.loadItemStringsFromWz();
        load();
        commodities.sort(Comparator.comparingInt(CommodityInfo::getId));
        for (CommodityInfo ci : commodities) {
            if (ci.getGameWorld().equalsIgnoreCase("") || !ci.getGameWorld().contains("0") || ci.getTerm() == 1 || ci.getOnSale() == 0) {
                continue;
            }
            if (StringData.getItemStringById(ci.getItemId()) == null) {
                continue;
            }
            System.out.printf("[Id: %d] SN: %d | " +
                            "Item Id: %d | " +
                            "Item Name: %s | " +
                            "Count: %d | " +
                            "Discount: %d | " +
                            "Price: %s | " +
                            "originalPrice: %s | " +
                            "Period: %s.%n",
                    ci.getId(),
                    ci.getSN(),
                    ci.getItemId(),
                    StringData.getItemStringById(ci.getItemId()),
                    ci.getCount(),
                    ci.getDiscount(),
                    Util.getNumberFormat(ci.getPrice()),
                    Util.getNumberFormat(ci.getOriginalPrice()),
                    ci.getPeriod() == 0 ? "vĩnh viễn" : ci.getPeriod() + " ngày");
        }
    }
}
