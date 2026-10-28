package net.swordie.ms.connection.protocol.codec;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.life.Life;
import net.swordie.ms.life.movement.MovementBase;
import net.swordie.ms.util.Position;

import java.util.List;

/**
 * One element of a TMS 282.3 CMovePath. The layout comes from {@link Tms282MoveShape#of(int)}; the fields are the
 * 265 {@link MovementBase} ones plus the fifth tail byte and the u16 sub type of types 79/81. Game logic uses it
 * through {@link net.swordie.ms.life.movement.Movement} like any 265 element.
 */
public final class Tms282MoveElement extends MovementBase {
    private final Tms282MoveShape shape;
    private byte tailExtra;
    private short subType;

    private Tms282MoveElement(int type) {
        this.command = (byte) type;
        this.shape = Tms282MoveShape.of(type);
    }

    /** Reads the element body after its type byte. */
    public static Tms282MoveElement decode(int type, InPacket in) {
        Tms282MoveElement e = new Tms282MoveElement(type);
        for (Tms282MovePart part : e.parts()) {
            part.read(in, e);
        }
        return e;
    }

    /** Writes the type byte and the body. */
    @Override
    public void encode(OutPacket out) {
        out.encodeByte(command);
        for (Tms282MovePart part : parts()) {
            part.write(out, this);
        }
    }

    /** Wire type, unsigned. */
    public int type() {
        return command & 0xFF;
    }

    public Tms282MoveShape shape() {
        return shape;
    }

    public List<Tms282MovePart> parts() {
        return shape.parts(type());
    }

    public boolean has(Tms282MovePart part) {
        return parts().contains(part);
    }

    /** Fifth tail byte ([element+74]); 265 elements have no such byte. */
    public byte getTailExtra() {
        return tailExtra;
    }

    /** u16 sub type of types 79/81. */
    public short getSubType() {
        return subType;
    }

    @Override
    public void applyTo(Char chr) {
        if (has(Tms282MovePart.POSITION)) {
            chr.setPosition(getPosition());
        }
        if (has(Tms282MovePart.FOOTHOLD)) {
            chr.setFoothold(getFh());
        }
        if (has(Tms282MovePart.TAIL)) {
            chr.setMoveAction(getMoveAction());
        }
    }

    @Override
    public void applyTo(Life life) {
        if (has(Tms282MovePart.POSITION)) {
            life.setPosition(getPosition());
        }
        if (has(Tms282MovePart.VELOCITY)) {
            life.setvPosition(getVPosition());
        }
        if (has(Tms282MovePart.FOOTHOLD)) {
            life.setFh(getFh());
        }
        if (has(Tms282MovePart.TAIL)) {
            life.setMoveAction(getMoveAction());
        }
    }

    @Override
    public String toString() {
        return "Tms282MoveElement{type=" + type() + ", shape=" + shape + ", pos=" + position + ", v=" + vPosition
                + ", fh=" + fh + ", action=" + moveAction + ", elapse=" + elapse + "}";
    }

    // --- package-private setters for Tms282MovePart ---

    void setPosition(Position position) {
        this.position = position;
    }

    void setVPosition(Position vPosition) {
        this.vPosition = vPosition;
    }

    void setOffset(Position offset) {
        this.offset = offset;
    }

    void setFh(short fh) {
        this.fh = fh;
    }

    void setFootStart(short footStart) {
        this.footStart = footStart;
    }

    void setStat(byte stat) {
        this.stat = stat;
    }

    void setSubType(short subType) {
        this.subType = subType;
    }

    void setTail(byte moveAction, short elapse, byte forcedStop, byte tailExtra) {
        this.moveAction = moveAction;
        this.elapse = elapse;
        this.forcedStop = forcedStop;
        this.tailExtra = tailExtra;
    }
}
