package net.swordie.ms.life.mob.skill;

public class FairyDust {

    private int scale;
    private int createDelay;
    private int moveSpeed;
    private int angle;

    public FairyDust(int scale, int createDelay, int moveSpeed, int angle) {
        this.scale = scale;
        this.createDelay = createDelay;
        this.moveSpeed = moveSpeed;
        this.angle = angle;
    }

    public int getScale() {
        return scale;
    }

    public void setScale(int scale) {
        this.scale = scale;
    }

    public int getCreateDelay() {
        return createDelay;
    }

    public void setCreateDelay(int createDelay) {
        this.createDelay = createDelay;
    }

    public int getMoveSpeed() {
        return moveSpeed;
    }

    public void setMoveSpeed(int moveSpeed) {
        this.moveSpeed = moveSpeed;
    }

    public int getAngle() {
        return angle;
    }

    public void setAngle(int angle) {
        this.angle = angle;
    }
}
