package net.swordie.ms.loaders;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.swordie.ms.ServerConstants;
import net.swordie.ms.life.npc.Npc;
import net.swordie.ms.util.DataPrinter;
import net.swordie.ms.util.Util;
import net.swordie.ms.util.XMLApi;
import net.swordie.ms.world.shop.NpcShopDlg;
import net.swordie.ms.world.shop.NpcShopItem;
import org.w3c.dom.Node;

import java.io.*;
import java.util.*;

public class NpcData {

    private static final Int2ObjectMap<Npc> npcs = new Int2ObjectOpenHashMap<>();
    private static final Map<Integer, NpcShopDlg> shops = new HashMap<>();

    private static void loadNpcsFromWz() {
        String wzDir = String.format("%s/Npc.wz", ServerConstants.WZ_DIR);
        for (File file : new File(wzDir).listFiles()) {
            if (file.isDirectory()) continue;
            Npc npc = new Npc(0);
            Node node = XMLApi.getRoot(file);
            Node mainNode = XMLApi.getAllChildren(node).get(0);
            int id = Integer.parseInt(XMLApi.getNamedAttribute(mainNode, "name")
                    .replace(".xml", "").replace(".img", ""));
            npc.setTemplateId(id);
            npc.setMove(XMLApi.getFirstChildByNameBF(mainNode, "move") != null);
            Node scriptNode = XMLApi.getFirstChildByNameBF(mainNode, "script");
            if (scriptNode != null) {
                for (Node idNode : XMLApi.getAllChildren(scriptNode)) {
                    String scriptIDString = XMLApi.getNamedAttribute(idNode, "name");
                    if (!Util.isNumber(scriptIDString)) {
                        continue;
                    }
                    int scriptID = Integer.parseInt(XMLApi.getNamedAttribute(idNode, "name"));
                    Node scriptValueNode = XMLApi.getFirstChildByNameDF(idNode, "script");
                    if (scriptValueNode != null) {
                        String scriptName = XMLApi.getNamedAttribute(scriptValueNode, "value");
                        npc.getScripts().put(scriptID, scriptName);
                    }
                }
            }
            Node infoNode = XMLApi.getFirstChildByNameBF(mainNode, "info");
            for (Node infoChildNode : XMLApi.getAllChildren(infoNode)) {
                String name = XMLApi.getNamedAttribute(infoChildNode, "name");
                String value = XMLApi.getNamedAttribute(infoChildNode, "value");
                switch (name) {
                    case "trunkGet":
                        npc.setTrunkGet(Integer.parseInt(value));
                        break;
                    case "trunkPut":
                        npc.setTrunkPut(Integer.parseInt(value));
                        break;
                    case "shop":
                        npc.setShop(Integer.parseInt(value) != 0);
                        break;
                }
            }
            npcs.put(npc.getTemplateId(), npc);
        }
    }

    public static void saveNpcsToDat(String dir) {
        Util.makeDirIfAbsent(dir);
        for (Npc npc : npcs.values()) {
            File file = new File(String.format("%s/%d.dat", dir, npc.getTemplateId()));
            try (DataOutputStream das = new DataOutputStream(new FileOutputStream(file))) {
                das.writeInt(npc.getTemplateId());
                das.writeBoolean(npc.isMove());
                das.writeInt(npc.getTrunkGet());
                das.writeInt(npc.getTrunkPut());
                das.writeBoolean(npc.isShop());
                das.writeShort(npc.getScripts().size());
                npc.getScripts().forEach((key, val) -> {
                    try {
                        das.writeInt(key);
                        das.writeUTF(val);
                    } catch (IOException e) {
                        DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
                    }
                });
            } catch (IOException e) {
                DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
            }
        }
    }

    private static Npc getNpc(int templateID) {
        return npcs.get(templateID);
    }

    public static Npc getNpcDeepCopyById(int id) {
        Npc npc = getNpc(id);
        if (npc != null) {
            npc = npc.deepCopy();
        } else {
            // 282 profile: data/dat282 overlay first; 265 profile: exactly DAT_DIR/npc as before.
            File file = net.swordie.ms.loaders.overlay.DataOverlays.current().datFile("npc/" + id + ".dat");
            if (file.exists()) {
                npc = loadNpcFromDat(file).deepCopy();
                npcs.put(npc.getTemplateId(), npc);
            }
        }
        return npc;
    }

    private static Npc loadNpcFromDat(File file) {
        try (DataInputStream dis = new DataInputStream(new FileInputStream(file))) {
            Npc npc = new Npc(dis.readInt());
            npc.setMove(dis.readBoolean());
            npc.setTrunkGet(dis.readInt());
            npc.setTrunkPut(dis.readInt());
            npc.setShop(dis.readBoolean());
            short size = dis.readShort();
            for (int i = 0; i < size; i++) {
                int id = dis.readInt();
                String val = dis.readUTF();
                npc.getScripts().put(id, val);
            }
            npcs.put(npc.getTemplateId(), npc);
            return npc;
        } catch (IOException e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
        }
        return null;
    }

    private static NpcShopDlg loadNpcShopDlgFromDB(int id) {
        List<NpcShopItem> items = NpcShopItem.getNPCShopItemsFromSQLByShopID(id);
        if (items.isEmpty()) {
            return null;
        }
        NpcShopDlg nsd = new NpcShopDlg();
        nsd.setNpcTemplateID(id);
        nsd.setShopID(id);
        items.sort(Comparator.comparingInt(NpcShopItem::getItemID));
        nsd.setItems(items);
        nsd.generateProjectiles();
        shops.put(id, nsd);
        return nsd;
    }

    public static NpcShopDlg getShopById(int shopID) {
        NpcShopDlg npcShopDlg = shops.get(shopID);
        if (npcShopDlg == null) {
            npcShopDlg = loadNpcShopDlgFromDB(shopID);
        }
        if (npcShopDlg != null) {
            leaveOutUnavailableItems(npcShopDlg, shopID, net.swordie.ms.connection.protocol.codec.ItemAvailability.active());
        }
        return npcShopDlg;
    }

    /**
     * Removes the items the active client has no data for (TMS 282.3: its shop dialog throws assertion 0x21000003 on
     * one). The shared dialog is filtered, so the server's list and the client's list (buy indexes) stay identical.
     * GMS 265 ({@code ItemAvailability.ALL}) removes nothing. Returns how many were removed.
     */
    public static int leaveOutUnavailableItems(NpcShopDlg npcShopDlg, int shopID,
                                               net.swordie.ms.connection.protocol.codec.ItemAvailability availability) {
        var items = npcShopDlg.getItems();
        if (items == null || items.stream().allMatch(i -> availability.isAvailable(i.getItemID()))) {
            return 0;
        }
        var kept = new java.util.ArrayList<>(items);
        kept.removeIf(i -> !availability.isAvailable(i.getItemID()));
        int removed = items.size() - kept.size();
        System.out.printf("[ItemAvailability] shop %d: %d of %d item(s) not in the client left out%n", shopID, removed,
                items.size());
        npcShopDlg.setItems(kept);
        return removed;
    }

    public static void generateDatFiles() {
        System.out.println("Started generating npc data.");
        long start = System.currentTimeMillis();
        loadNpcsFromWz();
        saveNpcsToDat(ServerConstants.DAT_DIR + "/npc");
        System.out.printf("Completed generating npc data in %dms%n", System.currentTimeMillis() - start);
    }

    public static void main(String[] args) {
        generateDatFiles();
    }

    public static void clear() {
        npcs.clear();
    }

    public static void load() {
        loadNpcsFromWz();
    }
}
