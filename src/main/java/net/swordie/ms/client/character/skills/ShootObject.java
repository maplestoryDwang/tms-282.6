package net.swordie.ms.client.character.skills;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.util.Position;

public class ShootObject {
    private Char owner;
    private int skillId, slv, id;
    private short direction;
    private Position position;
    private Position position2;
    private int delay;
    private int unknownInt1;
    private int unknownInt2;
    private int unknownInt3;
    private boolean flip;
    private boolean isArrow;
    private boolean isCrystal;
    private Position crystalPosition;
    private Position arrowPosition;

    public ShootObject(Char owner, InPacket inPacket) {
        this.owner = owner;
        this.skillId = inPacket.decodeInt();
        this.slv = inPacket.decodeInt();
        this.id = inPacket.decodeInt();
        this.unknownInt1 = inPacket.decodeInt();
        this.direction = inPacket.decodeShort();
        this.position = inPacket.decodePosition();
        this.position2 = inPacket.decodePosition();
        this.delay = inPacket.decodeInt();
        this.flip = inPacket.decodeByte() != 0;
        this.unknownInt2 = inPacket.decodeInt();
        this.isCrystal = inPacket.decodeByte() != 0;
        if (isCrystal()) {
            this.crystalPosition = inPacket.decodePositionInt();
        }
        this.isArrow = inPacket.decodeByte() != 0;
        if (isArrow()) {
            this.arrowPosition = inPacket.decodePositionInt();
        }
        this.unknownInt3 = inPacket.decodeInt();
    }

    public void encodeShootObjectRemote(OutPacket outPacket) {
        outPacket.encodeInt(skillId); // skill id
        outPacket.encodeInt(slv); // slv
        outPacket.encodeInt(id); // object Id
        outPacket.encodeInt(unknownInt1); // unk
        outPacket.encodeShort(direction); // direction
        outPacket.encodePosition(position); // start position
        outPacket.encodePosition(position2); // end position
        outPacket.encodeInt(delay); // delay
        outPacket.encodeByte(flip); // flip
        outPacket.encodeInt(unknownInt2); // unk2
        outPacket.encodeByte(isCrystal); // crystal
        if (isCrystal) {
            outPacket.encodePositionInt(crystalPosition);
        }
        outPacket.encodeByte(isArrow);
        if (isArrow) {
            outPacket.encodePositionInt(arrowPosition);
        }
        outPacket.encodeInt(unknownInt3);
    }

    public Char getOwner() {
        return owner;
    }

    public void setOwner(Char owner) {
        this.owner = owner;
    }

    public int getSkillId() {
        return skillId;
    }

    public void setSkillId(int skillId) {
        this.skillId = skillId;
    }

    public int getSlv() {
        return slv;
    }

    public void setSlv(int slv) {
        this.slv = slv;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public short getDirection() {
        return direction;
    }

    public void setDirection(short direction) {
        this.direction = direction;
    }

    public boolean isArrow() {
        return isArrow;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public boolean isFlip() {
        return flip;
    }

    public void setFlip(boolean flip) {
        this.flip = flip;
    }

    public boolean isCrystal() {
        return isCrystal;
    }

    public void setCrystal(boolean crystal) {
        isCrystal = crystal;
    }

    public Position getCrystalPosition() {
        return crystalPosition;
    }

    public Position getArrowPosition() {
        return arrowPosition;
    }

    public void setCrystalPosition(Position crystalPosition) {
        this.crystalPosition = crystalPosition;
    }
}
