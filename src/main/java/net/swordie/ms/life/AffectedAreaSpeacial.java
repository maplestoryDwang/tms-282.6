package net.swordie.ms.life;

import net.swordie.ms.util.Position;
import net.swordie.ms.util.Rect;

public class AffectedAreaSpeacial {

    private int originalSkillID;
    private int value;
    private Position position;
    private Rect rect;
    private int y; // 350
    private int u; // 290

    public AffectedAreaSpeacial() {}

    public int getOriginalSkillID() {
        return originalSkillID;
    }

    public void setOriginalSkillID(int originalSkillID) {
        this.originalSkillID = originalSkillID;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public Rect getRect() {
        return rect;
    }

    public void setRect(Rect rect) {
        this.rect = rect;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getU() {
        return u;
    }

    public void setU(int u) {
        this.u = u;
    }

    public Rect getRectAround(Rect rect) {
        int x = getPosition().getX();
        int y = getPosition().getY();
        return new Rect(x + rect.getLeft(), y + rect.getTop(), x + rect.getRight(), y + rect.getBottom());
    }
}
