package net.swordie.ms.life.mob.skill;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.packet.WillPacket;
import net.swordie.ms.life.Life;
import net.swordie.ms.util.Position;
import net.swordie.ms.util.container.Tuple;
import net.swordie.ms.world.field.Field;

import java.util.ArrayList;
import java.util.List;

public class SpiderWeb extends Life {

    private int number;
    private Position pos;
    private int pattern;
    private static List<Tuple<Integer, Position>> spiderPositions;

    public static void load() {
        spiderPositions = new ArrayList<>();
        spiderPositions.add(new Tuple(0, new Position(-711, -254)));
        spiderPositions.add(new Tuple(0, new Position(712, 310)));
        spiderPositions.add(new Tuple(0, new Position(552, 459)));
        spiderPositions.add(new Tuple(0, new Position(531, -268)));
        spiderPositions.add(new Tuple(0, new Position(-594, 251)));
        spiderPositions.add(new Tuple(0, new Position(-506, 432)));
        spiderPositions.add(new Tuple(0, new Position(-626, -179)));
        spiderPositions.add(new Tuple(0, new Position(604, -153)));
        spiderPositions.add(new Tuple(0, new Position(736, 56)));
        spiderPositions.add(new Tuple(0, new Position(-749, 17)));
        spiderPositions.add(new Tuple(0, new Position(-197, -300)));
        spiderPositions.add(new Tuple(0, new Position(-282, 488)));
        spiderPositions.add(new Tuple(0, new Position(-606, -75)));
        spiderPositions.add(new Tuple(0, new Position(558, -58)));
        spiderPositions.add(new Tuple(0, new Position(-555, 151)));
        spiderPositions.add(new Tuple(0, new Position(-520, 331)));
        spiderPositions.add(new Tuple(0, new Position(-420, 373)));
        spiderPositions.add(new Tuple(0, new Position(-270, 65)));
        spiderPositions.add(new Tuple(0, new Position(-344, -140)));
        spiderPositions.add(new Tuple(0, new Position(-230, -65)));
        spiderPositions.add(new Tuple(0, new Position(-90, -153)));
        spiderPositions.add(new Tuple(0, new Position(200, -120)));
        spiderPositions.add(new Tuple(0, new Position(395, -45)));
        spiderPositions.add(new Tuple(0, new Position(651, 228)));
        spiderPositions.add(new Tuple(0, new Position(563, 188)));
        spiderPositions.add(new Tuple(0, new Position(470, 165)));
        spiderPositions.add(new Tuple(0, new Position(382, 80)));
        spiderPositions.add(new Tuple(0, new Position(-5, 370)));
        spiderPositions.add(new Tuple(0, new Position(85, 150)));
        spiderPositions.add(new Tuple(0, new Position(110, 345)));

        spiderPositions.add(new Tuple(1, new Position(-701, 182)));
        spiderPositions.add(new Tuple(1, new Position(718, 432)));
        spiderPositions.add(new Tuple(1, new Position(-577, -298)));
        spiderPositions.add(new Tuple(1, new Position(699, -82)));
        spiderPositions.add(new Tuple(1, new Position(577, 345)));
        spiderPositions.add(new Tuple(1, new Position(-733, -122)));
        spiderPositions.add(new Tuple(1, new Position(-405, 484)));
        spiderPositions.add(new Tuple(1, new Position(391, -307)));
        spiderPositions.add(new Tuple(1, new Position(458, -163)));
        spiderPositions.add(new Tuple(1, new Position(80, 482)));
        spiderPositions.add(new Tuple(1, new Position(-485, -148)));
        spiderPositions.add(new Tuple(1, new Position(772, 169)));
        spiderPositions.add(new Tuple(1, new Position(-650, 45)));
        spiderPositions.add(new Tuple(1, new Position(-61, -275)));
        spiderPositions.add(new Tuple(1, new Position(210, 450)));
        spiderPositions.add(new Tuple(1, new Position(-280, 365)));
        spiderPositions.add(new Tuple(1, new Position(-500, 15)));
        spiderPositions.add(new Tuple(1, new Position(-355, -45)));
        spiderPositions.add(new Tuple(1, new Position(-220, -200)));
        spiderPositions.add(new Tuple(1, new Position(50, -155)));
        spiderPositions.add(new Tuple(1, new Position(320, -170)));
        spiderPositions.add(new Tuple(1, new Position(275, -20)));
        spiderPositions.add(new Tuple(1, new Position(130, 0)));
        spiderPositions.add(new Tuple(1, new Position(622, 64)));
        spiderPositions.add(new Tuple(1, new Position(497, 44)));
        spiderPositions.add(new Tuple(1, new Position(460, 290)));
        spiderPositions.add(new Tuple(1, new Position(-168, 245)));
        spiderPositions.add(new Tuple(1, new Position(10, 240)));

        spiderPositions.add(new Tuple(2, new Position(164, -308)));
        spiderPositions.add(new Tuple(2, new Position(-683, 395)));
        spiderPositions.add(new Tuple(2, new Position(702, -280)));
        spiderPositions.add(new Tuple(2, new Position(378, 480)));
        spiderPositions.add(new Tuple(2, new Position(-366, -325)));
        spiderPositions.add(new Tuple(2, new Position(-84, 481)));
        spiderPositions.add(new Tuple(2, new Position(-388, 200)));
        spiderPositions.add(new Tuple(2, new Position(-84, 48)));
        spiderPositions.add(new Tuple(2, new Position(250, 230)));
    }

    public SpiderWeb(int number) {
        super(0);
        this.number = number;
        this.pattern = (spiderPositions.get(number)).getLeft();
        this.pos = (spiderPositions.get(number)).getRight();
    }

    @Override
    public SpiderWeb deepCopy() {
        SpiderWeb copy = new SpiderWeb(getNumber());
        copy.setObjectId(getObjectId());
        copy.setTemplateId(getTemplateId());
        copy.setX(getX());
        copy.setY(getY());
        copy.setPattern(getPattern());
        copy.setPos(getPos());
        return copy;
    }

    @Override
    public void broadcastSpawnPacket(Char onlyChar) {
        Field field = getField();
        if (onlyChar == null) {
            field.broadcast(WillPacket.spider(true, this));
        } else {
            onlyChar.write(WillPacket.spider(true, this));
        }
    }

    @Override
    public void broadcastLeavePacket() {
        getField().broadcast(WillPacket.spider(false, this));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SpiderWeb spiderWeb) {
            return spiderWeb.getTemplateId() == getTemplateId() && spiderWeb.getObjectId() == getObjectId() && spiderWeb.getField().equals(getField());
        }
        return false;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public Position getPos() {
        return pos;
    }

    public void setPos(Position pos) {
        this.pos = pos;
    }

    public int getPattern() {
        return pattern;
    }

    public void setPattern(int pattern) {
        this.pattern = pattern;
    }
}
