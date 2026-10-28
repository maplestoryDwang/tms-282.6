package net.swordie.ms.loaders.Etc.SetItemInfo;

import net.swordie.ms.ServerConstants;
import net.swordie.ms.util.DataPrinter;
import net.swordie.ms.util.Loader;
import net.swordie.ms.util.Util;
import net.swordie.ms.util.XMLApi;
import net.swordie.ms.util.container.Tuple;
import org.w3c.dom.Node;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SetItemInfoData {

    private static List<SetItemInfo> setItemInfos = new ArrayList<>();

    public static List<SetItemInfo> getSetItemInfos() {
        return setItemInfos;
    }

    public static void addSetItemInfo(SetItemInfo setItemInfo) {
        SetItemInfoData.setItemInfos.add(setItemInfo);
    }

    public static void saveSetItemInfo(String dir) {
        Util.makeDirIfAbsent(dir);
        try {
            DataOutputStream dataOutputStream = new DataOutputStream(new FileOutputStream(new File(dir + "/setItemInfo.dat")));
            dataOutputStream.writeInt(setItemInfos.size());
            for (SetItemInfo setItemInfo : setItemInfos) {
                dataOutputStream.writeInt(setItemInfo.getSetItemID());
                dataOutputStream.writeInt(setItemInfo.getCompleteCount());

                dataOutputStream.writeInt(setItemInfo.getActiveSkills().size());
                for (SetItemInfo.ActiveSkill activeSkill : setItemInfo.getActiveSkills()) {
                    dataOutputStream.writeInt(activeSkill.getEffectIndex());
                    dataOutputStream.writeInt(activeSkill.getLevel());
                    dataOutputStream.writeInt(activeSkill.getSkillID());
                }
                //dataOutputStream.writeChars(setItemInfo.getEffectLink());

                dataOutputStream.writeInt(setItemInfo.getItemIDs().size());
                for (SetItemInfo.ItemID itemID : setItemInfo.getItemIDs()) {
                    dataOutputStream.writeInt(itemID.getEffectIndex());
                    dataOutputStream.writeInt(itemID.getItemID());
                }

                dataOutputStream.writeInt(setItemInfo.getParts());
                //dataOutputStream.writeChars(setItemInfo.getSetItemName());
            }
        } catch (IOException e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
        }
    }

    public static Tuple<Integer, Integer> getSkill(int setItemID, int petID) {
        for (SetItemInfo setItemInfo : getSetItemInfos()) {
            if (setItemInfo.getSetItemID() == setItemID) {
                for (SetItemInfo.ItemID itemID : setItemInfo.getItemIDs()) {
                    if (itemID.getItemID() == petID) {
                        for (SetItemInfo.ActiveSkill activeSkill : setItemInfo.getActiveSkills()) {
                            if (activeSkill.getEffectIndex() == itemID.getEffectIndex()) {
                                return new Tuple<>(activeSkill.getSkillID(), activeSkill.getEffectIndex());
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    @Loader(varName = "/etc/setItemInfo")
    public static void loadSetItemInfo(File file, boolean exists) {
        // 282 profile: data/dat282/etc/setItemInfo.dat (the whole 282 SetItemInfo.img) replaces the 265 table.
        File overlay = net.swordie.ms.loaders.overlay.DataOverlays.current().overlayDat("etc/setItemInfo.dat").orElse(null);
        if (overlay != null) {
            file = overlay;
            exists = true;
        }
        if (!exists) {
            loadSetItemInfoFromWz();
            saveSetItemInfo(ServerConstants.DAT_DIR + "/etc");
        } else {
            try (DataInputStream dataInputStream = new DataInputStream(new FileInputStream(file))) {
                int setItemInfoSize = dataInputStream.readInt();
                for (int i = 0; i < setItemInfoSize; i++) {
                    SetItemInfo setItemInfo = new SetItemInfo();
                    setItemInfo.setSetItemID(dataInputStream.readInt());
                    setItemInfo.setCompleteCount(dataInputStream.readInt());

                    int activeSkillSize = dataInputStream.readInt();
                    for (int j = 0; j < activeSkillSize; j++) {
                        SetItemInfo.ActiveSkill activeSkill = new SetItemInfo.ActiveSkill();
                        activeSkill.setEffectIndex(dataInputStream.readInt());
                        activeSkill.setLevel(dataInputStream.readInt());
                        activeSkill.setSkillID(dataInputStream.readInt());
                        setItemInfo.addActiveSkill(activeSkill);
                    }

                    //setItemInfo.setEffectLink(dataInputStream.readUTF());

                    int itemIDSize = dataInputStream.readInt();
                    for (int j = 0; j < itemIDSize; j++) {
                        SetItemInfo.ItemID itemID = new SetItemInfo.ItemID();
                        itemID.setEffectIndex(dataInputStream.readInt());
                        itemID.setItemID(dataInputStream.readInt());
                        setItemInfo.addItemID(itemID);
                    }

                    setItemInfo.setParts(dataInputStream.readInt());
                    //setItemInfo.setSetItemName(dataInputStream.readUTF());

                    //System.out.println(setItemInfo);
                    SetItemInfoData.setItemInfos.add(setItemInfo);
                }
            } catch (IOException e) {
                DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
            }
        }
    }

    public static void loadSetItemInfoFromWz() {
        File file = new File(String.format("%s/Etc.wz/SetItemInfo.img.xml", ServerConstants.WZ_DIR));
        Node root = XMLApi.getRoot(file);
        Node firstNode = XMLApi.getAllChildren(root).get(0);
        List<Node> setItemInfoNodes = XMLApi.getAllChildren(firstNode);
        for (Node setItemInfoNode : setItemInfoNodes) {
            //Mỗi setItemInfoNode là một set Item Info
            SetItemInfo setItemInfo = new SetItemInfo();

            int setItemID = Integer.parseInt(XMLApi.getNamedAttribute(setItemInfoNode, "name"));

            //System.out.println(setItemID);
            setItemInfo.setSetItemID(setItemID);

            List<Node> setItemInfoDetails = XMLApi.getAllChildren(setItemInfoNode);
            for (Node setItemInfoDetail : setItemInfoDetails) {
                switch (XMLApi.getNamedAttribute(setItemInfoDetail, "name")) {
                    case "completeCount":
                        int completeCount = Integer.parseInt(XMLApi.getNamedAttribute(setItemInfoDetail, "value"));
                        setItemInfo.setCompleteCount(completeCount);
                        break;
                    case "Effect":
                        List<Node> effectNodes = XMLApi.getAllChildren(setItemInfoDetail);
                        for (Node effectNode : effectNodes) {
                            int effectSet = Integer.parseInt(XMLApi.getNamedAttribute(effectNode, "name"));
                            List<Node> effectDetails = XMLApi.getAllChildren(effectNode);
                            for (Node effectDetail : effectDetails) {
                                String effectName = XMLApi.getNamedAttribute(effectDetail, "name");
                                switch (effectName) {
                                    case "activeSkill":
                                        List<Node> activeSkillNodes = XMLApi.getAllChildren(effectDetail);
                                        for (Node activeSkillNode : activeSkillNodes) {
                                            List<Node> activeSkillDetailNodes = XMLApi.getAllChildren(activeSkillNode);

                                            SetItemInfo.ActiveSkill activeSkill = new SetItemInfo.ActiveSkill();
                                            activeSkill.setEffectIndex(effectSet);

                                            for (Node activeSkillDetailNode : activeSkillDetailNodes) {
                                                //System.out.println(XMLApi.getNamedAttribute(activeSkillDetailNode, "name"));
                                                switch (XMLApi.getNamedAttribute(activeSkillDetailNode, "name")) {
                                                    case "autoRunOnlyTown": //TODO: LAZY NOW
                                                        continue;
                                                    case "id":
                                                        int skillID = Integer.parseInt(XMLApi.getNamedAttribute(activeSkillDetailNode, "value"));
                                                        activeSkill.setSkillID(skillID);
                                                        break;
                                                    case "level":
                                                        int level = Integer.parseInt(XMLApi.getNamedAttribute(activeSkillDetailNode, "value"));
                                                        activeSkill.setLevel(level);
                                                        break;
                                                }
                                            }
                                            setItemInfo.addActiveSkill(activeSkill);
                                            //System.out.println(activeSkill);
                                        }
                                        break;
                                    case "Option": //Potential Info of set.
                                        //TODO: Lazy btw
                                        break;
                                    default:
                                        //TODO
                                        break;

                                }
                                //System.out.println("Effect Detail: " + XMLApi.getNamedAttribute(effectDetail, "name"));
                            }

                            //System.out.println("Effect: " + XMLApi.getNamedAttribute(effectNode, "name"));
                        }
                        break;
                    case "effectLink":
                        String effectLink = XMLApi.getNamedAttribute(setItemInfoDetail, "value");
                        setItemInfo.setEffectLink(effectLink);
                        break;
                    case "ItemID":
                        List<Node> itemIDNodes = XMLApi.getAllChildren(setItemInfoDetail);

                        for (Node itemIDNode : itemIDNodes) {
                            SetItemInfo.ItemID itemID = new SetItemInfo.ItemID();
                            int id = Integer.parseInt(XMLApi.getNamedAttribute(itemIDNode, "name"));
                            itemID.setEffectIndex(id);

                            List<Node> itemDetailNodes = XMLApi.getAllChildren(itemIDNode);
                            for (Node itemDetailNode : itemDetailNodes) {
                                switch (XMLApi.getNamedAttribute(itemDetailNode, "name")) {
                                    case "typeName":
                                    case "representName":
                                        continue;
                                    default:
                                        //System.out.println(XMLApi.getNamedAttribute(itemDetailNode, "value"));
                                        int item = Integer.parseInt(XMLApi.getNamedAttribute(itemDetailNode, "value"));
                                        itemID.setItemID(item);
                                        //System.out.println(itemID);
                                        setItemInfo.addItemID(itemID);
                                        break;
                                }
                            }
                        }
                        break;
                    case "parts":
                        int parts = Integer.parseInt(XMLApi.getNamedAttribute(setItemInfoDetail, "value"));
                        setItemInfo.setParts(parts);
                        break;
                    case "setItemName":
                        String setItemName = XMLApi.getNamedAttribute(setItemInfoDetail, "value");
                        setItemInfo.setSetItemName(setItemName);
                        break;
                }
                //System.out.println(XMLApi.getNamedAttribute(setItemInfoDetail, "name"));
            }
            addSetItemInfo(setItemInfo);
            //System.out.println("");
        }
    }

    public static void generateDatFiles() {
        System.out.println("Started generating Etc: SetItemInfo data.");
        Util.makeDirIfAbsent(ServerConstants.DAT_DIR + "/etc");
        long start = System.currentTimeMillis();

        loadSetItemInfoFromWz();
        saveSetItemInfo(ServerConstants.DAT_DIR + "/etc");

        System.out.printf("Completed generating Etc: SetItemInfo data in %dms%n", System.currentTimeMillis() - start);
    }

    public static void main(String[] args) {
        generateDatFiles();
    }
}
