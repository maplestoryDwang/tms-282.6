package net.swordie.ms.connection.packet;

import net.swordie.ms.client.character.skills.ForceAtom;
import net.swordie.ms.client.character.skills.info.ForceAtomInfo;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.util.Position;
import net.swordie.ms.util.Rect;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * TMS 282.3 CREATE_FORCE_ATOM = <b>LP032B</b> (265 784, delta +27), client reader CField 377C380
 * (research/tms2823/force-atom/README.md). Static read of 377C380 (jump tables 377D6FC/377D704, 377D730/377D738,
 * position predicate 3823260 table 3823290/3823298) and every type fixture traced CLEAN with the emulated 282.3 client.
 * <pre>
 * u8 byMob, [byMob: i32 userOwner], i32 charId, i32 type
 * type 36/37/89/91          : i32 skillId                                  (377CAAE)
 * type 0/9/14/35            : nothing (skill = 0)                            (377C477)
 * type 29/42                : i32 skillId, [skillId 400021069: i32]          (377C489)
 * other                     : u8 toMob, targets, i32 skillId                 (377C4AF)
 *     LIST types (377C4DF)  : i32 n, n x (i32 target, [type 114: 16-byte rect])
 *     type 62               : i32 n, n x i32 target                          (377C7D0)
 *     else                  : i32 target
 * atoms (377CAC0)           : (u8 1, 13 x i32, u8)* u8 0                     (= 265 ForceAtomInfo.encode)
 * post by type (377CC97)    : see {@link #post282Length}
 * skill tail (377D326)      : 400011059 i32 | 400011131 i32,u8 | 400041023 i32,i32,i32   (skill = the one read above)
 * type 35 (377D3BB)         : i32, i32
 * </pre>
 * Differences from the 265 encoder (FieldPacket.createForceAtom):
 * <ul>
 *     <li><b>the 265 default post "i32 -1" is gone</b> (most types: Demon Slayer fury 0, Phantom cards 1, Xenon 5/6,
 *     Kinesis 22, ...); 282 reads nothing there.</li>
 *     <li>LIST types: 265 list minus 66, 110, 112 (single target in 282), plus 115 (Saitama psychic power).</li>
 *     <li>type 62: i32 count before the targets (265 wrote first target + all targets without a count).</li>
 *     <li>type 18: position only (265 position + rect + i32); types 110-113: no position.</li>
 *     <li>type 90 (Illium 152141001, 282): i32, i32, i32, rect (265 wrote -1).</li>
 *     <li>skill tail: fox spirits 25100010 / 25120115 no longer read; 400041023 reads i32 + 2 x i32 (= 265).</li>
 *     <li>type 35 (Kanna spirit domain): two i32 after the skill tail (265 wrote -1).</li>
 * </ul>
 */
public final class Tms282ForceAtomPackets {
    public static final int OPCODE = 0x032B;
    public static final int MAX_TARGETS = 256;
    public static final int MAX_ATOMS = 512;
    /** 13 x i32 + u8 (377CAF3..377CBA9). */
    public static final int ATOM_BYTES = 13 * 4 + 1;

    /** 377C4DF: list-target types (jump table 377D730 / 377D738, type - 2). */
    public static final Set<Integer> LIST_282 = Set.of(2, 3, 7, 11, 12, 13, 17, 19, 20, 23, 24, 25, 27, 28, 30, 32, 34,
            38, 39, 40, 41, 47, 48, 49, 52, 53, 54, 55, 56, 57, 58, 60, 64, 65, 67, 68, 72, 73, 75, 76, 77, 84, 86, 87, 90,
            92, 93, 95, 97, 101, 114, 115);
    /** FieldPacket.createForceAtom list-target types. */
    public static final Set<Integer> LIST_265 = Set.of(2, 3, 7, 11, 12, 13, 17, 19, 20, 23, 24, 25, 27, 28, 30, 32, 34,
            38, 39, 40, 41, 47, 48, 49, 52, 53, 54, 55, 56, 57, 58, 60, 64, 65, 66, 67, 68, 72, 73, 75, 76, 77, 84, 86, 87,
            90, 92, 93, 95, 97, 101, 110, 112, 114);
    /** 3823260 (type 16 is tested separately, 377CE4E): the post is a position. */
    public static final Set<Integer> POSITION_282 = Set.of(2, 3, 4, 16, 20, 25, 26, 30, 33, 61, 64, 67, 69, 74, 76, 85,
            93, 94, 95, 97, 101);
    public static final Set<Integer> POSITION_265 = Set.of(2, 3, 4, 16, 20, 25, 26, 30, 33, 61, 64, 67, 69, 74, 76, 85,
            93, 94, 95, 97, 101, 110, 111, 112, 113);
    /** Types without the u8 toMob / targets / skill block. */
    public static final Set<Integer> NO_TARGET = Set.of(0, 9, 14, 29, 35, 42);
    public static final Set<Integer> SKILL_ONLY = Set.of(36, 37, 89, 91);

    public static final int GRIM_REAPER = 400021069;   // 0x17D7D64D (377C494)
    public static final int TAIL_ONE = 400011059;      // 0x17D7AF33
    public static final int MJOLNIR = 400011131;       // +0x48
    public static final int TAIL_POS = 400041023;      // +0x74C4

    private Tms282ForceAtomPackets() {
    }

    /** One LP032B body, independent of how it was built. */
    public record Wire(boolean byMob, int userOwner, int charId, int type, boolean toMob, List<Integer> targets,
                       List<int[]> targetRects, int skillId, int grimReaperGauge, List<byte[]> atoms, byte[] post,
                       byte[] skillTail, byte[] typeTail) {

        public OutPacket encode() {
            OutPacket out = Tms282LoginPacketSupport.packet(OPCODE);
            out.encodeByte(byMob);
            if (byMob) {
                out.encodeInt(userOwner);
            }
            out.encodeInt(charId);
            out.encodeInt(type);
            if (SKILL_ONLY.contains(type)) {
                out.encodeInt(skillId);
            } else if (NO_TARGET.contains(type)) {
                if (type == 29 || type == 42) {
                    out.encodeInt(skillId);
                    if (skillId == GRIM_REAPER) {
                        out.encodeInt(grimReaperGauge);
                    }
                }
            } else {
                out.encodeByte(toMob);
                if (LIST_282.contains(type)) {
                    out.encodeInt(targets.size());
                    for (int i = 0; i < targets.size(); i++) {
                        out.encodeInt(targets.get(i));
                        if (type == 114) {
                            int[] r = i < targetRects.size() ? targetRects.get(i) : new int[4];
                            for (int v : r) {
                                out.encodeInt(v);
                            }
                        }
                    }
                } else if (type == 62) {
                    out.encodeInt(targets.size());
                    targets.forEach(out::encodeInt);
                } else {
                    out.encodeInt(targets.isEmpty() ? 0 : targets.get(0));
                }
                out.encodeInt(skillId);
            }
            for (byte[] a : atoms) {
                out.encodeByte(1);
                out.encodeArr(a);
            }
            out.encodeByte(0);
            out.encodeArr(post);
            out.encodeArr(skillTail);
            out.encodeArr(typeTail);
            return out;
        }

        /** The skill the 282 reader keeps in r12 (0 for types 0/9/14/35). */
        public int readSkill() {
            return NO_TARGET.contains(type) && type != 29 && type != 42 ? 0 : skillId;
        }
    }

    // ------------------------------------------------------------------ 282 lengths

    /** Bytes 377CC97.. reads after the atom list for {@code type}. */
    public static int post282Length(int type) {
        switch (type) {
            case 11: return 20;
            case 9: return 16;
            case 15: return 16 + 1 + 4;
            case 29: return 24;
            case 17: case 77: case 18: return 8;
            case 27: case 28: case 34: return 20;
            case 57: case 58: case 86: case 87: return 28;
            case 36: case 89: return 12 + 16 + 16 + 4;
            case 39: case 90: return 12 + 16;
            case 37: case 91: return 4 + 16 + 8;
            case 42: case 7: return 16;
            case 49: return 8 + 16;
            case 50: return 8 + 4;
            default: return POSITION_282.contains(type) ? 8 : 0;
        }
    }

    /** 377D326: the skill tail length for the skill the reader kept. */
    public static int skillTail282Length(int readSkill) {
        switch (readSkill) {
            case TAIL_ONE: return 4;
            case MJOLNIR: return 5;
            case TAIL_POS: return 12;
            default: return 0;
        }
    }

    // ------------------------------------------------------------------ direct encoder from the server object

    /** LP032B from a {@link ForceAtom} (the object FieldPacket.createForceAtom encodes for 265). */
    public static OutPacket create(ForceAtom fa) {
        return wire(fa).encode();
    }

    public static Wire wire(ForceAtom fa) {
        int type = fa.getForceAtomEnum().getForceAtomType();
        List<Integer> targets = fa.getTargetIdList() == null ? List.of() : List.copyOf(fa.getTargetIdList());
        if (targets.size() > MAX_TARGETS) {
            throw new IllegalArgumentException("force atom targets " + targets.size());
        }
        List<int[]> rects = new ArrayList<>();
        for (int i = 0; i < targets.size(); i++) {
            rects.add(rect(fa.getRect()));
        }
        List<byte[]> atoms = new ArrayList<>();
        for (ForceAtomInfo fai : fa.getFaiList()) {
            OutPacket o = new OutPacket();
            fai.encode(o);
            byte[] d = o.getData();
            byte[] a = d.length == ATOM_BYTES + 2 ? java.util.Arrays.copyOfRange(d, 2, d.length) : d;
            if (a.length != ATOM_BYTES) {
                throw new IllegalStateException("ForceAtomInfo.encode wrote " + a.length + " bytes");
            }
            atoms.add(a);
        }
        if (atoms.size() > MAX_ATOMS) {
            throw new IllegalArgumentException("force atoms " + atoms.size());
        }
        Bytes post = new Bytes();
        Rect r = fa.getRect();
        switch (type) {
            case 11 -> post.rect(r).i32(fa.getBulletId());
            case 9, 42, 7 -> post.rect(r);
            case 15 -> post.rect(r).u8(0).i32(fa.getUserOwner());
            case 29 -> post.rect(r).pos(fa.getForcedTargetPosition());
            case 17, 77 -> post.i32(fa.getArriveDir()).i32(fa.getArriveRange());
            case 18 -> post.pos(fa.getForcedTargetPosition());
            case 27 -> post.rect(r).i32(0);
            case 28, 34 -> post.rect(r).i32(fa.getTime());
            case 57, 58, 86, 87 -> post.rect(r).i32(fa.getTime()).pos(fa.getPosition());
            case 36, 39, 89, 90 -> {
                post.i32(4).i32(550).i32(0).rect(r);
                if (type == 36 || type == 89) {
                    post.rect(fa.getRect2()).i32(0);
                }
            }
            case 37, 91 -> post.i32(0).rect(r).pos(fa.getForcedTargetPosition());
            case 49 -> post.i32(fa.getBulletId()).i32(fa.getUserOwner()).rect(r);
            case 50 -> post.pos(fa.getForcedTargetPosition()).i32(0);
            default -> {
                if (POSITION_282.contains(type)) {
                    post.pos(fa.getForcedTargetPosition());
                }
            }
        }
        int skill = fa.getSkillId();
        int readSkill = NO_TARGET.contains(type) && type != 29 && type != 42 ? 0 : skill;
        Bytes tail = new Bytes();
        switch (readSkill) {
            case TAIL_ONE -> tail.i32(0);
            case MJOLNIR -> tail.i32(32768).u8(3);
            case TAIL_POS -> tail.i32(0).pos(fa.getPosition());
            default -> {
            }
        }
        Bytes typeTail = new Bytes();
        if (type == 35) {
            typeTail.pos(fa.getPosition() != null ? fa.getPosition() : fa.getForcedTargetPosition());
        }
        return new Wire(fa.isByMob(), fa.getUserOwner(), fa.getCharId(), type, fa.isToMob(), targets, rects,
                NO_TARGET.contains(type) && type != 29 && type != 42 ? 0 : skill, 0, atoms, post.get(), tail.get(),
                typeTail.get());
    }

    // ------------------------------------------------------------------ 265 body -> Wire (translator)

    /** Parses a finished 265 CREATE_FORCE_ATOM body (FieldPacket.createForceAtom) into the 282 wire. */
    public static Wire fromLegacy(Tms282LegacyPackets.Body b) {
        boolean byMob = b.u8() != 0;
        int userOwner = byMob ? b.i32() : 0;
        int charId = b.i32();
        int type = b.i32();
        if (type < 0 || type > 200) {
            throw new IllegalArgumentException("force atom type " + type);
        }
        boolean toMob = false;
        List<Integer> targets = new ArrayList<>();
        List<int[]> rects = new ArrayList<>();
        int skill = 0;
        int gauge = 0;
        if (SKILL_ONLY.contains(type)) {
            skill = b.i32();
        } else if (!NO_TARGET.contains(type)) {
            toMob = b.u8() != 0;
            if (type == 62) {
                throw new IllegalArgumentException("265 type 62 target list has no count");
            }
            if (LIST_265.contains(type)) {
                int n = b.i32();
                if (n < 0 || n > MAX_TARGETS) {
                    throw new IllegalArgumentException("force atom targets " + n);
                }
                for (int i = 0; i < n; i++) {
                    targets.add(b.i32());
                    if (type == 114) {
                        rects.add(new int[]{b.i32(), b.i32(), b.i32(), b.i32()});
                    }
                }
            } else {
                targets.add(b.i32());
            }
            skill = b.i32();
        }
        if (type == 29 || type == 42) {
            skill = b.i32();
            if (skill == GRIM_REAPER) {
                gauge = b.i32();
            }
        }
        if (!LIST_282.contains(type) && targets.size() > 1) {
            targets = List.of(targets.get(0)); // 66 / 110 / 112: single target in 282
        }
        List<byte[]> atoms = new ArrayList<>();
        while (b.u8() != 0) {
            if (atoms.size() >= MAX_ATOMS) {
                throw new IllegalArgumentException("force atoms > " + MAX_ATOMS);
            }
            atoms.add(b.bytes(ATOM_BYTES));
        }
        byte[] post265 = b.bytes(post265Length(type));
        byte[] post = post282(type, post265);
        // 265 skill tail (fa.getSkillId(); only the skill in the body is known)
        int readSkill = NO_TARGET.contains(type) && type != 29 && type != 42 ? 0 : skill;
        byte[] tail;
        switch (readSkill) {
            case 25100010, 25120115 -> {
                b.i32();
                tail = new byte[0]; // fox spirit id: 282 no longer reads it
            }
            case TAIL_ONE -> tail = b.bytes(4);
            case MJOLNIR -> tail = b.bytes(5);
            case TAIL_POS -> tail = b.bytes(12);
            default -> tail = new byte[0];
        }
        b.end();
        byte[] typeTail = type == 35 ? new byte[8] : new byte[0]; // 265 has no value for it: (0, 0)
        return new Wire(byMob, userOwner, charId, type, toMob, targets, rects, skill, gauge, atoms, post, tail, typeTail);
    }

    /** FieldPacket.createForceAtom post switch length. */
    public static int post265Length(int type) {
        switch (type) {
            case 11: return 20;
            case 9: return 16;
            case 15: return 21;
            case 29: return 24;
            case 17: case 77: return 8;
            case 18: return 8 + 16 + 4;
            case 27: case 28: case 34: return 20;
            case 57: case 58: case 86: case 87: return 28;
            case 36: case 89: return 48;
            case 39: return 28;
            case 37: case 91: return 28;
            case 42: case 7: return 16;
            case 49: return 24;
            case 50: return 12;
            default: return POSITION_265.contains(type) ? 8 : 4; // default: i32 -1
        }
    }

    /** 265 post bytes -> 282 post bytes. */
    static byte[] post282(int type, byte[] p265) {
        if (type == 18) {
            return java.util.Arrays.copyOf(p265, 8); // position; 265 rect + i32 dropped
        }
        if (type == 90) {
            return new Bytes().i32(4).i32(550).i32(0).i32(0).i32(0).i32(0).i32(0).get(); // 265 wrote only -1
        }
        int len282 = post282Length(type);
        if (len282 == p265.length) {
            return p265;
        }
        if (len282 == 0) {
            return new byte[0]; // 265 default -1, or the 110-113 position
        }
        throw new IllegalArgumentException("force atom type " + type + ": 265 post " + p265.length + " vs 282 " + len282);
    }

    static int[] rect(Rect r) {
        return r == null ? new int[4] : new int[]{r.getLeft(), r.getTop(), r.getRight(), r.getBottom()};
    }

    /** Small little-endian writer. */
    static final class Bytes {
        private final ByteBuffer buf = ByteBuffer.allocate(128).order(ByteOrder.LITTLE_ENDIAN);

        Bytes i32(int v) {
            buf.putInt(v);
            return this;
        }

        Bytes u8(int v) {
            buf.put((byte) v);
            return this;
        }

        Bytes rect(Rect r) {
            for (int v : Tms282ForceAtomPackets.rect(r)) {
                buf.putInt(v);
            }
            return this;
        }

        Bytes pos(Position p) {
            buf.putInt(p == null ? 0 : p.getX());
            buf.putInt(p == null ? 0 : p.getY());
            return this;
        }

        byte[] get() {
            byte[] r = new byte[buf.position()];
            buf.duplicate().flip().get(r);
            return r;
        }
    }
}
