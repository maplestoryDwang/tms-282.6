package net.swordie.ms.world.field;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.util.Position;

public class HomingBullet {

    private Position start;
    private Position end;
    private int speed;

    public HomingBullet(Position start, Position end, int speed) {
        this.start = start;
        this.end = end;
        this.speed = speed;
    }

    public void encode(OutPacket outPacket) {
        outPacket.encodePositionInt(getStart());
        outPacket.encodePositionInt(getEnd());
        outPacket.encodeInt(getSpeed());
    }

    public Position getStart() {
        return start;
    }

    public void setStart(Position start) {
        this.start = start;
    }

    public Position getEnd() {
        return end;
    }

    public void setEnd(Position end) {
        this.end = end;
    }

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }
}
