package net.swordie.ms.connection.protocol.codec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.IntPredicate;

/**
 * Field layout of one TMS 282.3 CMovePath element, by element type (ingame-cp-map-2823.md section 3.1: switch of
 * CMovePath::Encode 3FDB6B0 -> F189B0 at F18B1A, types 0..0x6D, anything else takes the default = tail only).
 * Each shape is an ordered list of {@link Tms282MovePart}s; a few parts only exist for some types of the shape.
 * The 265 class that holds the same data is noted per shape; the 265 numbering differs from 71 on.
 */
public enum Tms282MoveShape {
    /** 265 MovementNormal. */
    NORMAL(types(0, 8, 15, 17, 19, 71, 72, 73, 74, 75, 76, 92, 106),
            part(Tms282MovePart.POSITION), part(Tms282MovePart.VELOCITY), part(Tms282MovePart.FOOTHOLD),
            part(Tms282MovePart.FALL_START, t -> t == 15 || t == 17), part(Tms282MovePart.OFFSET),
            part(Tms282MovePart.WORD), part(Tms282MovePart.TAIL)),
    /** 265 MovementJump. */
    JUMP(types(1, 2, 18, 21, 22, 24, 62, 64, 65, 66, 67, 68, 69, 97),
            part(Tms282MovePart.VELOCITY), part(Tms282MovePart.FALL_START, t -> t == 21 || t == 22),
            part(Tms282MovePart.OFFSET, t -> t == 62), part(Tms282MovePart.TAIL)),
    /** 265 MovementTeleport (the u32 is the de-obfuscated value of F1BD30). */
    TELEPORT(types(3, 4, 5, 6, 7, 9, 10, 11, 13, 26, 27, 54, 55, 56, 82, 83, 84, 87, 109),
            part(Tms282MovePart.POSITION), part(Tms282MovePart.FOOTHOLD), part(Tms282MovePart.VALUE),
            part(Tms282MovePart.TAIL)),
    /** 265 MovementStatChange: one byte, no tail. */
    STAT_CHANGE(types(12), part(Tms282MovePart.STAT)),
    /** 265 MovementStartFallDown. */
    START_FALL_DOWN(types(14, 16),
            part(Tms282MovePart.VELOCITY), part(Tms282MovePart.FALL_START), part(Tms282MovePart.TAIL)),
    /** 265 MovementFlyingBlock. */
    FLYING_BLOCK(types(23, 29, 100, 101),
            part(Tms282MovePart.POSITION), part(Tms282MovePart.VELOCITY), part(Tms282MovePart.TAIL)),
    /** Type 28 (265 read it as a StartFallDown; 282 writes x, y, fh). */
    POSITION_FOOTHOLD(types(28),
            part(Tms282MovePart.POSITION), part(Tms282MovePart.FOOTHOLD), part(Tms282MovePart.TAIL)),
    /** 265 MovementNew1: one u32 (30: path-level [path+50]; 31/43/91: F1BD30 value). */
    VALUE(types(30, 31, 43, 91), part(Tms282MovePart.VALUE), part(Tms282MovePart.TAIL)),
    /** 265 MovementOffsetX (265 read x, y, vx, vy, fh; 282 writes the x offset instead of fh). */
    OFFSET_X(types(51),
            part(Tms282MovePart.POSITION), part(Tms282MovePart.VELOCITY), part(Tms282MovePart.OFFSET_X),
            part(Tms282MovePart.TAIL)),
    /** 265 MovementAngle. */
    ANGLE(types(58, 70, 108),
            part(Tms282MovePart.POSITION), part(Tms282MovePart.VELOCITY), part(Tms282MovePart.FOOTHOLD),
            part(Tms282MovePart.TAIL)),
    /** Type 107 (no 265 class): every field of a normal element behind a u32. */
    FULL(types(107),
            part(Tms282MovePart.VALUE), part(Tms282MovePart.POSITION), part(Tms282MovePart.VELOCITY),
            part(Tms282MovePart.FOOTHOLD), part(Tms282MovePart.FALL_START), part(Tms282MovePart.OFFSET),
            part(Tms282MovePart.WORD), part(Tms282MovePart.TAIL)),
    /** 265 MovementDefault 81/94 (seven shorts): u16 sub type, x, y, vx, vy, x/y offset, no tail. */
    PARAMS(types(79, 81),
            part(Tms282MovePart.SUB_TYPE), part(Tms282MovePart.POSITION), part(Tms282MovePart.VELOCITY),
            part(Tms282MovePart.OFFSET)),
    /** 265 MovementAction: the tail only. Every type not listed above, including all types above 109. */
    ACTION(types(), part(Tms282MovePart.TAIL));

    /** Highest type with its own switch case in F18B1A; higher types take the default (tail only). */
    public static final int LAST_SWITCH_TYPE = 0x6D;

    private static final Tms282MoveShape[] BY_TYPE = new Tms282MoveShape[256];

    static {
        for (Tms282MoveShape shape : values()) {
            for (int type : shape.types) {
                if (BY_TYPE[type] != null) {
                    throw new ExceptionInInitializerError("Move type " + type + " in " + BY_TYPE[type] + " and " + shape);
                }
                BY_TYPE[type] = shape;
            }
        }
        for (int type = 0; type < BY_TYPE.length; type++) {
            if (BY_TYPE[type] == null) {
                BY_TYPE[type] = ACTION;
            }
        }
    }

    private final int[] types;
    private final List<ConditionalPart> parts;

    Tms282MoveShape(int[] types, ConditionalPart... parts) {
        this.types = types;
        this.parts = List.of(parts);
    }

    /** Shape of a wire type (unsigned byte). */
    public static Tms282MoveShape of(int type) {
        return BY_TYPE[type & 0xFF];
    }

    /** Types listed for this shape (empty for {@link #ACTION}, the default). */
    public List<Integer> listedTypes() {
        List<Integer> list = new ArrayList<>(types.length);
        for (int type : types) {
            list.add(type);
        }
        return Collections.unmodifiableList(list);
    }

    /** Parts on the wire, in order, for this type of the shape. */
    public List<Tms282MovePart> parts(int type) {
        List<Tms282MovePart> list = new ArrayList<>(parts.size());
        for (ConditionalPart p : parts) {
            if (p.when().test(type & 0xFF)) {
                list.add(p.part());
            }
        }
        return list;
    }

    /** Encoded size of an element of this type, type byte included. */
    public int size(int type) {
        int n = 1;
        for (Tms282MovePart p : parts(type)) {
            n += p.size();
        }
        return n;
    }

    private static int[] types(int... types) {
        return types;
    }

    private static ConditionalPart part(Tms282MovePart part) {
        return new ConditionalPart(part, t -> true);
    }

    private static ConditionalPart part(Tms282MovePart part, IntPredicate when) {
        return new ConditionalPart(part, when);
    }

    private record ConditionalPart(Tms282MovePart part, IntPredicate when) {
    }
}
