package net.swordie.ms.loaders;


import net.swordie.ms.ServerConfig;
import net.swordie.ms.ServerConstants;
import net.swordie.ms.life.Reactor;
import net.swordie.ms.life.drop.DropInfo;
import net.swordie.ms.loaders.containerclasses.ReactorInfo;
import net.swordie.ms.scripts.ScriptInfo;
import net.swordie.ms.scripts.ScriptType;
import net.swordie.ms.util.DataPrinter;
import net.swordie.ms.util.Rect;
import net.swordie.ms.util.Util;
import net.swordie.ms.util.XMLApi;
import net.swordie.ms.world.field.Portal;
import org.w3c.dom.Node;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created on 4/21/2018.
 */
public class ReactorData {

    private static final boolean LOG_UNKS = false;
    private static HashMap<Integer, ReactorInfo> reactorInfo = new HashMap<>();

    private static void loadReactorsFromWZ() {
        String wzDir = ServerConstants.WZ_DIR + "/Reactor.wz";
        File dir = new File(wzDir);
        File[] files = dir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) continue;
            int id = Integer.parseInt(file.getName().replace(".img.xml", ""));
            ReactorInfo ri = new ReactorInfo();
            ri.setId(id);
            Node node = XMLApi.getAllChildren(XMLApi.getRoot(file)).get(0);
            Node questNode = XMLApi.getFirstChildByNameBF(node, "quest");
            if (questNode != null) {
                ri.setQuest(Integer.parseInt(XMLApi.getNamedAttribute(questNode, "value")));
            }
            Node actionNode = XMLApi.getFirstChildByNameBF(node, "action");
            if (actionNode != null) {
                ri.setAction(XMLApi.getNamedAttribute(actionNode, "value"));
            }
            Node mainInfoNode = XMLApi.getFirstChildByNameBF(node, "info");
            for (Node infoNode : XMLApi.getAllChildren(mainInfoNode)) {
                String name = XMLApi.getNamedAttribute(infoNode, "name");
                String value = XMLApi.getNamedAttribute(infoNode, "value");
                switch (name) {
                    case "info":
                        ri.setInfo(value);
                        break;
                    case "lt":
                        int x1 = Integer.parseInt(XMLApi.getNamedAttribute(infoNode, "x"));
                        int y1 = Integer.parseInt(XMLApi.getNamedAttribute(infoNode, "y"));
                        Node rbNode = XMLApi.getFirstChildByNameBF(mainInfoNode, "rb");
                        int x2 = Integer.parseInt(XMLApi.getNamedAttribute(rbNode, "x"));
                        int y2 = Integer.parseInt(XMLApi.getNamedAttribute(rbNode, "y"));
                        ri.setRect(new Rect(x1, y1, x2, y2));
                        break;
                    case "name":
                        ri.setName(value);
                        break;
                    case "viewName":
                        ri.setViewName(value);
                        break;
                    case "link":
                        ri.setLink(Integer.parseInt(value));
                        break;
                    case "level":
                        ri.setLevel(Integer.parseInt(value));
                        break;
                    case "resetTime":
                        ri.setResetTime(Integer.parseInt(value));
                        break;
                    case "hitCount":
                        ri.setHitCount(Integer.parseInt(value));
                        break;
                    case "overlapHit":
                        ri.setOverlapHit(Integer.parseInt(value));
                        break;
                    case "overlapHitTime":
                        ri.setOverlapHitTime(Integer.parseInt(value));
                        break;
                    case "actMark":
                        ri.setActMark(Integer.parseInt(value));
                        break;
                    case "dcMark":
                        ri.setDcMark("1".equalsIgnoreCase(value));
                        break;
                    case "activateByTouch":
                        ri.setActivateByTouch("1".equalsIgnoreCase(value));
                        break;
                    case "removeInFieldSet":
                        ri.setRemoveInFieldSet("1".equalsIgnoreCase(value));
                        break;
                    case "notHitable":
                        ri.setNotHittable("1".equalsIgnoreCase(value));
                        break;
                    case "notFatigue":
                        ri.setNotFatigue("1".equalsIgnoreCase(value));
                        break;
                    case "backTile":
                    case "frontTile":
                    case "forcedViewName":
                    case "rb":
                        break;
                    default:
                        if (LOG_UNKS) {
                            System.out.printf("[Reactor] Unhandled info node id %d, %s, value %s", ri.getId(), name, value);
                        }
                }
            }
            int maxHitCount = 0;
            for (byte state = 0; state < 100; state++) {
                Node name = XMLApi.getFirstChildByNameBF(node, String.valueOf(state));
                if (name != null) {
                    for (Node eventMainNode : XMLApi.getAllChildren(name)) {
                        if (XMLApi.getNamedAttribute(eventMainNode, "name").equals("event")) {
                            maxHitCount++;
                        }
                    }
                }
            }
            ri.setMaxHitCount(maxHitCount);
            addReactorInfo(ri);
        }
    }

    private static void saveReactors(String dir) {
        Util.makeDirIfAbsent(dir);
        for (ReactorInfo ri : getReactorInfo().values()) {
            File file = new File(String.format("%s/%d.dat", dir, ri.getId()));
            try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(file))) {
                dos.writeInt(ri.getId());
                dos.writeInt(ri.getLink());
                dos.writeInt(ri.getLevel());
                dos.writeInt(ri.getResetTime());
                dos.writeInt(ri.getOverlapHitTime());
                dos.writeInt(ri.getActMark());
                dos.writeInt(ri.getHitCount());
                dos.writeInt(ri.getOverlapHit());
                dos.writeInt(ri.getMaxHitCount());
                dos.writeInt(ri.getQuest());
                dos.writeUTF(ri.getInfo());
                dos.writeUTF(ri.getName());
                dos.writeUTF(ri.getViewName());
                dos.writeUTF(ri.getAction());
                dos.writeBoolean(ri.isNotFatigue());
                dos.writeBoolean(ri.isDcMark());
                dos.writeBoolean(ri.isRemoveInFieldSet());
                dos.writeBoolean(ri.isActivateByTouch());
                dos.writeBoolean(ri.isNotHittable());
                dos.writeBoolean(ri.getRect() != null);
                if (ri.getRect() != null) {
                    Rect r = ri.getRect();
                    dos.writeShort(r.getLeft());
                    dos.writeShort(r.getTop());
                    dos.writeShort(r.getRight());
                    dos.writeShort(r.getBottom());
                }
            } catch (IOException e) {
                DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
            }
        }
    }

    private static ReactorInfo loadReactorFromFile(File file) {
        ReactorInfo ri = null;
        try (DataInputStream dis = new DataInputStream(new FileInputStream(file))) {
            ri = new ReactorInfo();
            ri.setId(dis.readInt());
            ri.setLink(dis.readInt());
            ri.setLevel(dis.readInt());
            ri.setResetTime(dis.readInt());
            ri.setOverlapHitTime(dis.readInt());
            ri.setActMark(dis.readInt());
            ri.setHitCount(dis.readInt());
            ri.setOverlapHit(dis.readInt());
            ri.setMaxHitCount(dis.readInt());
            ri.setQuest(dis.readInt());
            ri.setInfo(dis.readUTF());
            ri.setName(dis.readUTF());
            ri.setViewName(dis.readUTF());
            ri.setAction(dis.readUTF());
            ri.setNotFatigue(dis.readBoolean());
            ri.setDcMark(dis.readBoolean());
            ri.setRemoveInFieldSet(dis.readBoolean());
            ri.setActivateByTouch(dis.readBoolean());
            ri.setNotHittable(dis.readBoolean());
            boolean hasRect = dis.readBoolean();
            if (hasRect) {
                Rect r = new Rect();
                r.setLeft(dis.readShort());
                r.setTop(dis.readShort());
                r.setRight(dis.readShort());
                r.setBottom(dis.readShort());
                ri.setRect(r);
            }
            ri.setDrops(DropData.getDropInfoByID(ri.getId()).stream().filter(DropInfo::getReactorDrop).collect(Collectors.toSet()));
            addReactorInfo(ri);
        } catch (IOException e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
        }
        return ri;
    }

    public static ReactorInfo getReactorInfoByID(int id) {
        ReactorInfo ri = getReactorInfo().get(id);
        return ri == null ? loadReactorByID(id) : ri;
    }

    public static Reactor getReactorByID(int id) {
        Reactor r = new Reactor(id);
        r.init();
        return r;
    }

    private static ReactorInfo loadReactorByID(int id) {
        // 282 profile: data/dat282 overlay first; 265 profile: exactly DAT_DIR/reactors as before.
        File file = net.swordie.ms.loaders.overlay.DataOverlays.current().datFile("reactors/" + id + ".dat");
        if (file.exists()) {
            return loadReactorFromFile(file);
        } else {
            return null;
        }
    }

    private static HashMap<Integer, ReactorInfo> getReactorInfo() {
        return reactorInfo;
    }

    private static void addReactorInfo(ReactorInfo ri) {
        getReactorInfo().put(ri.getId(), ri);
    }

    public static void generateDatFiles() {
        System.out.println("Started generating reactor data.");
        long start = System.currentTimeMillis();
        loadReactorsFromWZ();
        saveReactors(String.format("%s/reactors", ServerConstants.DAT_DIR));
        System.out.printf("Completed generating reactor data in %dms%n", System.currentTimeMillis() - start);
    }

    public static void main(String[] args) {
        generateDatFiles();
    }

    public static void clear() {
        reactorInfo.clear();
    }

    public static void load() {
        loadReactorsFromWZ();
    }
}
