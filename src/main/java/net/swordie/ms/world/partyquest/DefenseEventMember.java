package net.swordie.ms.world.partyquest;


import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.packet.DefensePacket;

public class DefenseEventMember {
    private Char chr;
    private int point = 0, exp = 0;

    public DefenseEventMember(Char chr) {
        this.chr = chr;
    }

    public Char getChr() {
        return chr;
    }

    public int getCharID() {
        return chr.getId();
    }

    public void plusPoint(int point) {
        this.point += point;
        changePoint();
    }

    public void minusPoint(int point) {
        if (getPoint() > 0) {
            this.point -= point;
            changePoint();
        }
    }

    public int getPoint() {
        return this.point;
    }

    public void changePoint() {
        getChr().write(DefensePacket.point(getPoint()));
    }

    public void plusExp(int exp) {
        this.exp += exp;
    }

    public int getExp() {
        return exp;
    }
}
