package net.swordie.ms.life.mob.boss.demian;

import net.swordie.ms.util.Position;

public class DemainDelayedAttack {

    private int objectID;
    private Position pos;
    private int angle;

    public DemainDelayedAttack() {
    }

    public DemainDelayedAttack(int objectID, Position pos, int angle) {
        this.objectID = objectID;
        this.pos = pos;
        this.angle = angle;
    }

    public int getObjectID() {
        return objectID;
    }

    public void setObjectID(int objectID) {
        this.objectID = objectID;
    }

    public Position getPos() {
        return pos;
    }

    public void setPos(Position pos) {
        this.pos = pos;
    }

    public int getAngle() {
        return angle;
    }

    public void setAngle(int angle) {
        this.angle = angle;
    }
}
