package net.swordie.ms.util;

import net.swordie.ms.ServerConstants;
import net.swordie.ms.loaders.StringData;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.io.*;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemScriptPrinter {

    public static Map<Integer, String> consumeItems = new HashMap<>();

    public static void main(String[] args) {
        StringData.loadItemStringsFromWz();
        loadDamageSkinFromWZ();
        try {
            saveConsumeItemScript(ServerConstants.SCRIPT_DIR + "/test");
        } catch (FileNotFoundException e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
        }
    }

    public static void saveConsumeItemScript(String dir) throws FileNotFoundException {
        Util.makeDirIfAbsent(dir);
        for (Map.Entry<Integer, String> damageSkin : consumeItems.entrySet()) {
            String fileName = "consume_" + damageSkin.getKey();
            PrintWriter printWriter = new PrintWriter(new FileOutputStream(new File(dir + "/" + fileName + ".py")));
            printWriter.write("# " + damageSkin.getValue());
            printWriter.print("\n");
            printWriter.print("sm.setSpeakerID(9010000)");
            printWriter.print("\n");
            printWriter.print("sm.flipDialogue()");
            printWriter.print("\n");
            printWriter.write("response = sm.sendAskYesNo(\"Would you like to replace the #v\"+str(sm.getActivatedDamageSkin())+\"# #b#t\"+str(sm.getActivatedDamageSkin())+\"##k\\r\\nthat is currently active with the new #v" + damageSkin.getKey() + "# #b#t" + damageSkin.getKey() + "##k?\")");
            printWriter.print("\n");
            printWriter.write("if response:");
            printWriter.print("\n");
            printWriter.print("    if sm.hasDamageSkin(" + damageSkin.getKey() + "):");
            printWriter.print("\n");
            printWriter.print("        sm.sendSayOkay(\"You already have this damage skin. Please try another.\")");
            printWriter.print("\n");
            printWriter.print("    else:");
            printWriter.print("\n");
            printWriter.print("        sm.addDamageSkin(" + damageSkin.getKey() + ")");
            printWriter.print("\n");
            printWriter.write("        sm.chat(\"The " + damageSkin.getValue() + " has been added to your account's damage skin collection.\")");
            printWriter.flush();
            printWriter.close();

        }
    }

    public static void loadDamageSkinFromWZ() {
        String wzDir = ServerConstants.WZ_DIR + "/Item.wz";
        String[] subMaps = new String[]{"Consume"};
        for (String subMap : subMaps) {
            File subDir = new File(String.format("%s/%s", wzDir, subMap));
            File[] files = subDir.listFiles();
            for (File file : files) {
                Document doc = XMLApi.getRoot(file);
                Node node = doc;
                List<Node> nodes = XMLApi.getAllChildren(node);
                for (Node mainNode : XMLApi.getAllChildren(nodes.get(0))) {
                    String nodeName = XMLApi.getNamedAttribute(mainNode, "name");
                    if (!Util.isNumber(nodeName)) {
                        continue;
                    }
                    int itemID = Integer.parseInt(nodeName);
                    if (StringData.getItemStringById(itemID) != null) {
                        if (itemID >= 2431965 && itemID <= 2631943) {
                            if (StringData.getItemStringById(itemID).endsWith("Damage Skin")) {
                                consumeItems.put(itemID, StringData.getItemStringById(itemID));
                            }
                        }
                    }
                }
            }
        }
    }
}
