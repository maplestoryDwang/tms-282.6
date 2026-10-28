package net.swordie.ms.connection.protocol.codec;

import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.util.Position;

/**
 * One field group of a TMS 282.3 CMovePath element (strategy per group). {@link Tms282MoveShape} orders them.
 * Widths from CMovePath::Encode F189B0 (ingame-cp-map-2823.md section 3.1).
 */
public enum Tms282MovePart {
    /** x, y (i16 each). */
    POSITION(4) {
        @Override
        void read(InPacket in, Tms282MoveElement e) {
            e.setPosition(new Position(in.decodeShort(), in.decodeShort()));
        }

        @Override
        void write(OutPacket out, Tms282MoveElement e) {
            out.encodePosition(e.getPosition());
        }
    },
    /** vx, vy (i16 each). */
    VELOCITY(4) {
        @Override
        void read(InPacket in, Tms282MoveElement e) {
            e.setVPosition(new Position(in.decodeShort(), in.decodeShort()));
        }

        @Override
        void write(OutPacket out, Tms282MoveElement e) {
            out.encodePosition(e.getVPosition());
        }
    },
    /** Foothold (i16). */
    FOOTHOLD(2) {
        @Override
        void read(InPacket in, Tms282MoveElement e) {
            e.setFh(in.decodeShort());
        }

        @Override
        void write(OutPacket out, Tms282MoveElement e) {
            out.encodeShort(e.getFh());
        }
    },
    /** Foothold the fall started from (i16). */
    FALL_START(2) {
        @Override
        void read(InPacket in, Tms282MoveElement e) {
            e.setFootStart(in.decodeShort());
        }

        @Override
        void write(OutPacket out, Tms282MoveElement e) {
            out.encodeShort(e.getFootStart());
        }
    },
    /** x offset, y offset (i16 each). */
    OFFSET(4) {
        @Override
        void read(InPacket in, Tms282MoveElement e) {
            e.setOffset(new Position(in.decodeShort(), in.decodeShort()));
        }

        @Override
        void write(OutPacket out, Tms282MoveElement e) {
            out.encodePosition(e.getOffset());
        }
    },
    /** x offset only (i16), type 51; kept as offset (x, 0). */
    OFFSET_X(2) {
        @Override
        void read(InPacket in, Tms282MoveElement e) {
            e.setOffset(new Position(in.decodeShort(), 0));
        }

        @Override
        void write(OutPacket out, Tms282MoveElement e) {
            out.encodeShort(e.getOffset().getX());
        }
    },
    /** u16 [element+7C] (265 MovementNormal "unk", new in 202). */
    WORD(2) {
        @Override
        void read(InPacket in, Tms282MoveElement e) {
            e.setUnk(in.decodeShort());
        }

        @Override
        void write(OutPacket out, Tms282MoveElement e) {
            out.encodeShort(e.getUnk());
        }
    },
    /** u32 (teleport / value types). */
    VALUE(4) {
        @Override
        void read(InPacket in, Tms282MoveElement e) {
            e.setIdk(in.decodeInt());
        }

        @Override
        void write(OutPacket out, Tms282MoveElement e) {
            out.encodeInt(e.getIdk());
        }
    },
    /** u8 stat (type 12, the whole element). */
    STAT(1) {
        @Override
        void read(InPacket in, Tms282MoveElement e) {
            e.setStat(in.decodeByte());
        }

        @Override
        void write(OutPacket out, Tms282MoveElement e) {
            out.encodeByte(e.getStat());
        }
    },
    /** u16 sub type of types 79/81 (265 MovementDefault "MPA"). */
    SUB_TYPE(2) {
        @Override
        void read(InPacket in, Tms282MoveElement e) {
            e.setSubType(in.decodeShort());
        }

        @Override
        void write(OutPacket out, Tms282MoveElement e) {
            out.encodeShort(e.getSubType());
        }
    },
    /**
     * Element tail T: u8 moveAction [+58], u16 elapse [+48], u8 forcedStop [+60], u8 [+74]. 265 has no last byte.
     */
    TAIL(5) {
        @Override
        void read(InPacket in, Tms282MoveElement e) {
            e.setTail(in.decodeByte(), in.decodeShort(), in.decodeByte(), in.decodeByte());
        }

        @Override
        void write(OutPacket out, Tms282MoveElement e) {
            out.encodeByte(e.getMoveAction());
            out.encodeShort(e.getDuration());
            out.encodeByte(e.getForcedStop());
            out.encodeByte(e.getTailExtra());
        }
    };

    private final int size;

    Tms282MovePart(int size) {
        this.size = size;
    }

    /** Bytes on the wire. */
    public int size() {
        return size;
    }

    abstract void read(InPacket in, Tms282MoveElement e);

    abstract void write(OutPacket out, Tms282MoveElement e);
}
