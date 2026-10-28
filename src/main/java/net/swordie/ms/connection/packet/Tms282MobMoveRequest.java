package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.protocol.codec.MovePathCodec;
import net.swordie.ms.connection.protocol.codec.Tms282MovePathCodec;
import net.swordie.ms.life.mob.MobMoveRequest;
import net.swordie.ms.life.mob.MobSkillAttackInfo;
import net.swordie.ms.life.movement.MovementInfo;
import net.swordie.ms.util.Position;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * CP0516 MOB_MOVE [verified opcode] (265 0x04FF, delta +0x17; sender 3EF9EF0, packet built from 3EFB9EB, site
 * 3EFB9F7; ingame-cp-map-2823.md 4.1). No trailing u32 (not in the 346A070 list). Body, from the static sender:
 * <pre>
 * u32 objId            [mob+558]                                          3EFBA0C
 * u16 moveId           ++[mob+464]                                        3EFBA4B
 * u8  actionAndDirMask (bit 4 = take control, as 265)                     3EFBA6E
 * u8  action           (action &lt;&lt; 1 | flip)                               3EFBA7F
 * u64 targetInfo       (skill id, level, target: 265 split)               3EFBA92
 * u8, u8                                                                  3EFBAA3, 3EFBAB4
 * u8 n + n x (i16 x, i16 y)    multi-target balls [mob+AB8]               3EFBACF..3EFBB8A
 * u8 n + n x i16               area attack times  [mob+AC0]               3EFBBB5..3EFBC22
 * u32 index            (the byte [mob+12B8], widened)                     3EFBC41
 *   index != 0: 11 x u32 ([12D4], [12CC], [12D0], 8 floats as ints)       3EFBC64..3EFBD53
 * u8 n (the client writes 10) + n x u32 (i16 values)      NEW in 282      3EFBD61, 3EFBD7C
 * u8  mask             (bit 0, bit 4: as 265)                             3EFBDB0
 * u32                  (265 hackedCode position)                          3EFBE2F
 * [str]                only when the value at [a5+BC8] is 0x12 (3EFBED2)  NEW in 282          3EFBEF7
 * u32, u32             ([a5+E38] values, or 0x00FFDDCC twice)             3EFBF2F|3EFBF52, 3EFBF60
 * u32                  ([a5+E58])                                         3EFBF7A
 * u32                  ([mob+1038] value or 0)                            3EFBFA2
 * u8                   (vtbl+0x30)                                        3EFBFBA
 * CMovePath::Encode 3FDB6B0 (282 elements, 5-byte tail, key pad)          3EFC3CE
 * tail, 30 bytes, kept raw: u8 flags, u32 x5, u8 flags, u32 [mob+B08], u8 [mob+AF0], u8, u8, u8   3EFC448..3EFC5FA
 * </pre>
 * (a5 = the fifth argument of 3EF9EF0, not identified.) The client sends the packet only when the path encoder succeeded ([rbp-74] == 0 after 3EFC3CE; otherwise the
 * packet is destroyed at 3EFC7CD), and then always with the whole tail, so every CP0516 ends with exactly
 * {@link #TAIL_LENGTH} bytes after the path (the sequence tool marked the tail conditional because of that branch).
 * 265 has the same order without the u32 list and the conditional string (265: mask, 5 x u32, u8, path).
 * <p>
 * The server cannot know the string's condition, so {@link #parse} tries the layout without it first and falls back
 * to the one with it. A layout is accepted when its path decodes, has at most {@link #MAX_ELEMENTS} elements and
 * leaves exactly {@link #TAIL_LENGTH} bytes. The layout was read statically; no live CP0516 has been captured yet.
 */
public record Tms282MobMoveRequest(int objectId, short moveId, byte actionAndDirMask, byte action, long targetInfo,
                                   int unk1, int unk2, List<Position> multiTargetForBalls,
                                   List<Short> randTimeForAreaAttacks, int index, int[] rect, int[] values282,
                                   byte mask, String conditional, int[] beforePath, byte lastByte, MovementInfo path,
                                   byte[] tail) {
    public static final int OPCODE = 0x0516;
    public static final int MAX_ELEMENTS = 128;
    public static final int TAIL_LENGTH = 30;
    /** Value of both u32 fields after the conditional string when the client's mode (3EFBF10) is not 3. */
    public static final int NO_TARGET = 0x00FFDDCC;

    public Tms282MobMoveRequest {
        Objects.requireNonNull(multiTargetForBalls, "multiTargetForBalls");
        Objects.requireNonNull(randTimeForAreaAttacks, "randTimeForAreaAttacks");
        Objects.requireNonNull(values282, "values282");
        Objects.requireNonNull(beforePath, "beforePath");
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(tail, "tail");
        multiTargetForBalls = List.copyOf(multiTargetForBalls);
        randTimeForAreaAttacks = List.copyOf(randTimeForAreaAttacks);
        if (index != 0 && (rect == null || rect.length != 11)) {
            throw new IllegalArgumentException("index != 0 needs 11 ints");
        }
        if (beforePath.length != 5) {
            throw new IllegalArgumentException("5 ints before the path");
        }
    }

    public static Tms282MobMoveRequest parse(byte[] packet) {
        return parse(packet, Tms282MovePathCodec.INSTANCE);
    }

    /**
     * Whole plaintext packet, opcode included.
     *
     * @throws IllegalArgumentException when neither layout (without / with the conditional string) reads
     */
    public static Tms282MobMoveRequest parse(byte[] packet, MovePathCodec paths) {
        Objects.requireNonNull(paths, "paths");
        try {
            return parse(packet, paths, false);
        } catch (IllegalArgumentException withoutString) {
            try {
                return parse(packet, paths, true);
            } catch (IllegalArgumentException withString) {
                IllegalArgumentException e = new IllegalArgumentException("CP0516 fits neither layout: "
                        + withoutString.getMessage() + " / with the string: " + withString.getMessage());
                e.addSuppressed(withoutString);
                e.addSuppressed(withString);
                throw e;
            }
        }
    }

    private static Tms282MobMoveRequest parse(byte[] packet, MovePathCodec paths, boolean withString) {
        Tms282FieldRequestReader r = Tms282FieldRequestReader.open(packet, OPCODE, "CP0516");
        int objectId = r.i32();
        short moveId = r.i16();
        byte actionAndDirMask = (byte) r.u8();
        byte action = (byte) r.u8();
        long targetInfo = Integer.toUnsignedLong(r.i32()) | (long) r.i32() << 32;
        int unk1 = r.u8();
        int unk2 = r.u8();
        int balls = r.u8();
        List<Position> ballList = new ArrayList<>(balls);
        for (int i = 0; i < balls; i++) {
            ballList.add(new Position(r.i16(), r.i16()));
        }
        int times = r.u8();
        List<Short> timeList = new ArrayList<>(times);
        for (int i = 0; i < times; i++) {
            timeList.add(r.i16());
        }
        int index = r.i32();
        int[] rect = null;
        if (index != 0) {
            rect = new int[11];
            for (int i = 0; i < rect.length; i++) {
                rect[i] = r.i32();
            }
        }
        int[] values = new int[r.u8()];
        for (int i = 0; i < values.length; i++) {
            values[i] = r.i32();
        }
        byte mask = (byte) r.u8();
        int[] beforePath = new int[5];
        beforePath[0] = r.i32();
        String conditional = withString ? r.str(StandardCharsets.UTF_8) : null;
        for (int i = 1; i < beforePath.length; i++) {
            beforePath[i] = r.i32();
        }
        byte lastByte = (byte) r.u8();
        MovementInfo path = r.path(paths);
        if (path.getMovements().size() > MAX_ELEMENTS) {
            throw new IllegalArgumentException("CP0516 path has " + path.getMovements().size() + " elements");
        }
        if (r.remaining() != TAIL_LENGTH) {
            throw new IllegalArgumentException("CP0516 has " + r.remaining() + " bytes after the path, not " + TAIL_LENGTH);
        }
        byte[] tail = r.rest();
        return new Tms282MobMoveRequest(objectId, moveId, actionAndDirMask, action, targetInfo, unk1, unk2, ballList,
                timeList, index, rect, values, mask, conditional, beforePath, lastByte, path, tail);
    }

    /** True when the conditional string was present (the fallback layout). */
    public boolean hasConditional() {
        return conditional != null;
    }

    /** The protocol-independent request, with the 265 split of the action and target fields. */
    public MobMoveRequest toRequest() {
        MobSkillAttackInfo msai = new MobSkillAttackInfo();
        msai.actionAndDirMask = actionAndDirMask;
        msai.action = (byte) (action >> 1);
        msai.flip = action % 2 != 0;
        msai.skillID = (short) (targetInfo & 0xFFFFL);
        msai.slv = (short) (targetInfo >> 16L & 0xFFFFL);
        msai.targetInfo = (int) (targetInfo >> 32L & 0xFFFFL);
        msai.multiTargetForBalls.addAll(multiTargetForBalls);
        msai.randTimeForAreaAttacks.addAll(randTimeForAreaAttacks);
        if (index != 0) {
            msai.unkMobMovement = new MobSkillAttackInfo.UnkMobMovement(index, rect[0], rect[1], rect[2], rect[3],
                    rect[4], rect[5], rect[6], rect[7], rect[8], rect[9], rect[10]);
        }
        return new MobMoveRequest(objectId, moveId, msai, action, path);
    }
}
