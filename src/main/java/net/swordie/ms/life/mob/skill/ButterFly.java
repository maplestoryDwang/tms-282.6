package net.swordie.ms.life.mob.skill;

import net.swordie.ms.ServerConstants;
import net.swordie.ms.util.DataPrinter;
import net.swordie.ms.util.Position;
import net.swordie.ms.util.XMLApi;

import org.w3c.dom.Node;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ButterFly {

    public static List<Position> BUTTERFLY_POS1 = new ArrayList<>();
    public static List<Position> BUTTERFLY_POS2 = new ArrayList<>();
    private int type;// templateId 0~8
    private Position pos;

    public ButterFly(int type, boolean isFirstPhase, int index) {
        this(type, getPosition(isFirstPhase, index));
    }

    public ButterFly(int type, Position pos) {
        this.type = type;
        this.pos = pos;
    }

    public static void load() {
        BUTTERFLY_POS1 = new ArrayList<>();
        BUTTERFLY_POS2 = new ArrayList<>();
        try {
            long start = System.currentTimeMillis();
            File file = new File(String.format("%s/Etc.wz/BossLucid.img.xml", ServerConstants.WZ_DIR));
            Node root = XMLApi.getRoot(file);
            Node mainNode = XMLApi.getAllChildren(root).get(0);
            List<Node> nodes = XMLApi.getAllChildren(mainNode);
            for (Node node : nodes) {
                String name = XMLApi.getNamedAttribute(node, "name");
                if (name.equals("Butterfly")) {
                    List<Node> node1 = XMLApi.getAllChildren(node);
                    for (Node node2 : node1) {
                        String name1 = XMLApi.getNamedAttribute(node2, "name");
                        List<Node> node3 = XMLApi.getAllChildren(node2);
                        if (name1.equals("phase1_pos")) {
                            for (Node node4 : node3) {
                                List<Node> node5 = XMLApi.getAllChildren(node4);
                                for (Node node6 : node5) {
                                    BUTTERFLY_POS1.add(new Position(
                                            Integer.parseInt(XMLApi.getNamedAttribute(node6, "x")),
                                            Integer.parseInt(XMLApi.getNamedAttribute(node6, "y"))));
                                }
                            }
                        } else if (name1.equals("phase2_pos")) {
                            for (Node node4 : node3) {
                                List<Node> node5 = XMLApi.getAllChildren(node4);
                                for (Node node6 : node5) {
                                    BUTTERFLY_POS2.add(new Position(
                                            Integer.parseInt(XMLApi.getNamedAttribute(node6, "x")),
                                            Integer.parseInt(XMLApi.getNamedAttribute(node6, "y"))));
                                }
                            }
                        }
                    }
                }
            }
            System.out.printf("[WZ Data] Loaded Butter Fly Data in %dms%n", System.currentTimeMillis() - start);
        } catch (NullPointerException e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
        }
    }

    public static Position getPosition(boolean isFirstPhase, int index) {
        if (isFirstPhase && index < BUTTERFLY_POS1.size()) {
            return BUTTERFLY_POS1.get(index);
        } else if (index < BUTTERFLY_POS2.size()) {
            return BUTTERFLY_POS2.get(index);
        } else {
            return new Position(0, 0);
        }
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public Position getPos() {
        return pos;
    }

    public void setPos(Position pos) {
        this.pos = pos;
    }
}
