package net.swordie.ms.life.mob.skill;

import net.swordie.ms.life.Life;

import java.util.List;
import java.util.concurrent.ScheduledFuture;

public class EnergySphere {

    private int objectID;
    private int x;
    private int y;
    private int size;
    private int startDelay;
    private int destroyDelay;
    private int density;
    private int friction;
    private int num;
    private int customx;
    private boolean ok;
    private boolean isDelayed;
    private List<Integer> pointx;
    private ScheduledFuture<?> schedule;

    public EnergySphere(boolean isDelayed, int size, int y, int density, int friction, int destroyDelay, int startDelay, List<Integer> pointx, int num) {
        setOk(true);
        setDelayed(isDelayed);
        setY(y);
        setDensity(density);
        setFriction(friction);
        this.destroyDelay = destroyDelay;
        this.startDelay = startDelay;
        this.size = size;
        this.pointx = pointx;
        this.num = num;
    }

    public int getX() {
        return this.x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getDestroyDelay() {
        return this.destroyDelay;
    }

    public int getStartDelay() {
        return this.startDelay;
    }

    public void setStartDelay(int startDelay) {
        this.startDelay = startDelay;
    }

    public boolean isOk() {
        return this.ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }

    public int getDensity() {
        return this.density;
    }

    public void setDensity(int density) {
        this.density = density;
    }

    public boolean isDelayed() {
        return this.isDelayed;
    }

    public void setDelayed(boolean isDelayed) {
        this.isDelayed = isDelayed;
    }

    public int getY() {
        return this.y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getFriction() {
        return this.friction;
    }

    public void setFriction(int friction) {
        this.friction = friction;
    }

    public ScheduledFuture<?> getSchedule() {
        return this.schedule;
    }

    public void setSchedule(ScheduledFuture<?> schedule) {
        this.schedule = schedule;
    }

    public int getSize() {
        return this.size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getNum() {
        return this.num;
    }

    public void setNum(int num) {
        this.num = num;
    }

    public void setCustomx(int customx) {
        this.customx = customx;
    }

    public List<Integer> getPointx() {
        return this.pointx;
    }

    public void setPointx(List<Integer> pointx) {
        this.pointx = pointx;
    }

    public int getObjectID() {
        return objectID;
    }

    public void setObjectID(int objectID) {
        this.objectID = objectID;
    }
}
