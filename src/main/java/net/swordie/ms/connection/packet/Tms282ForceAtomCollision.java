package net.swordie.ms.connection.packet;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.IntUnaryOperator;

/**
 * TMS 282.3 CP01C4 USER_FORCE_ATOM_COLLISION (265 InHeader 442 = 0x1BA, delta +0xA; sender 3ACF640, opcode at
 * 3ACFEBE, 346A070 tail list: a u32 follows the body). Static layout (research/tms2823/force-atom/README.md §3):
 * <pre>
 * i32 atomCount, i32 skillId (A: the skill of the first hit atom, +0x10C), i32 guidedCount
 * guidedCount x { i32 skillId, i32 id (+0x34), i32 x, i32 y,
 *                 [A in 152120001, 152141000/1/2/5/6: i32 target], [A in 152120001, 152141000/2/5: i32 (+0x38)] }
 * atomCount   x { i32 key (absolute), u8 keyWasNegative, i32 mobId, i32 (+0x188), i32 (+0xFC), i32 x, i32 y,
 *                 u8 (+0x1FC), i32 skillId (+0x10C), extras }
 * extras (A = header skill, t = the atom's force atom type, neg = keyWasNegative):
 *   A == 400021069: i32 ; A == 400041023: u8            (then the checks below continue)
 *   A in 31221014, 31241001, 400041010: i32              (done)
 *   (A in {14000029,14141007} and !neg) or t in {33,74} or (A in {14110035,14141010} and !neg):
 *        i32 target, i32 x, i32 y, [A in {14110035,14141010}: i32]
 *   else t in {37,38,91,92,44,45,81,82}: i32 (a vector size)
 *   else t == 32: 4 x i32
 * </pre>
 * The atom type is not in the packet: the caller resolves it from the key (Char.getForceAtomByKey); for unknown keys
 * the type-dependent extra (0 / 12 / 4 / 16 bytes) is resolved by the total length.
 */
public record Tms282ForceAtomCollision(int skillId, List<Guided> guided, List<Atom> atoms) {
    public static final int OPCODE = 0x01C4;
    public static final int MAX_ATOMS = 512;

    static final Set<Integer> ILLIUM_TARGET = Set.of(152120001, 152141000, 152141001, 152141002, 152141005, 152141006);
    static final Set<Integer> ILLIUM_EXTRA = Set.of(152120001, 152141000, 152141002, 152141005);
    static final Set<Integer> BAT = Set.of(14000029, 14141007);        // 11A2990(14000029, A)
    static final Set<Integer> BAT_EXTRA = Set.of(14110035, 14141010);  // 11A2990(14110035, A)
    static final Set<Integer> DONE_ONE = Set.of(31221014, 31241001, 400041010);
    static final Set<Integer> COUNT_TYPES = Set.of(37, 38, 91, 92, 44, 45, 81, 82); // 1194A40 | 1194A20

    public record Guided(int skillId, int id, int x, int y, byte[] extra) {
    }

    public record Atom(int key, boolean negative, int mobId, int v188, int vFC, int x, int y, int b1FC, int skillId,
                       byte[] extra) {
    }

    public static Tms282ForceAtomCollision parse(byte[] packet, IntUnaryOperator typeOfKey) {
        if (packet == null || packet.length < 2 + 12 + 4) {
            throw new IllegalArgumentException("CP01C4 too short");
        }
        int op = (packet[0] & 0xFF) | (packet[1] & 0xFF) << 8;
        if (op != OPCODE) {
            throw new IllegalArgumentException(String.format("CP%04X expected, got CP%04X", OPCODE, op));
        }
        ByteBuffer b = ByteBuffer.wrap(packet, 2, packet.length - 6).slice().order(ByteOrder.LITTLE_ENDIAN);
        int count = i32(b);
        int skill = i32(b);
        int guidedCount = i32(b);
        if (count < 0 || count > MAX_ATOMS || guidedCount < 0 || guidedCount > MAX_ATOMS) {
            throw new IllegalArgumentException("CP01C4 counts " + count + "/" + guidedCount);
        }
        List<Guided> guided = new ArrayList<>();
        for (int i = 0; i < guidedCount; i++) {
            int s = i32(b), id = i32(b), x = i32(b), y = i32(b);
            int extra = (ILLIUM_TARGET.contains(skill) ? 4 : 0) + (ILLIUM_EXTRA.contains(skill) ? 4 : 0);
            guided.add(new Guided(s, id, x, y, bytes(b, extra)));
        }
        List<Atom> atoms = new ArrayList<>();
        if (!parseAtoms(b, count, skill, typeOfKey, atoms)) {
            throw new IllegalArgumentException("CP01C4 atoms do not fit the body");
        }
        return new Tms282ForceAtomCollision(skill, List.copyOf(guided), List.copyOf(atoms));
    }

    /** Backtracking over the type-dependent extras of atoms whose type is unknown; the body must end exactly. */
    private static boolean parseAtoms(ByteBuffer b, int left, int skill, IntUnaryOperator typeOfKey, List<Atom> out) {
        if (left == 0) {
            return !b.hasRemaining();
        }
        if (b.remaining() < 4 + 1 + 5 * 4 + 1 + 4) {
            return false;
        }
        int mark = b.position();
        int key = b.getInt();
        boolean neg = b.get() != 0;
        int mob = b.getInt(), v188 = b.getInt(), vFC = b.getInt(), x = b.getInt(), y = b.getInt();
        int b1FC = b.get() & 0xFF;
        int s = b.getInt();
        int fixed = 0;
        boolean done = false;
        if (skill == Tms282ForceAtomPackets.GRIM_REAPER) {
            fixed += 4;
        } else if (skill == Tms282ForceAtomPackets.TAIL_POS) {
            fixed += 1;
        } else if (DONE_ONE.contains(skill)) {
            fixed += 4;
            done = true;
        }
        int type = typeOfKey.applyAsInt(key);
        int[] candidates;
        if (done) {
            candidates = new int[]{0};
        } else if ((BAT.contains(skill) && !neg) || (BAT_EXTRA.contains(skill) && !neg)) {
            candidates = new int[]{BAT_EXTRA.contains(skill) ? 16 : 12};
        } else if (type >= 0) {
            candidates = new int[]{typeExtra(type, skill)};
        } else {
            int bat = BAT_EXTRA.contains(skill) ? 16 : 12;
            candidates = new int[]{0, bat, 4, 16};
        }
        for (int extra : candidates) {
            int total = fixed + extra;
            if (b.remaining() < total) {
                continue;
            }
            int before = b.position();
            byte[] e = new byte[total];
            b.get(e);
            out.add(new Atom(key, neg, mob, v188, vFC, x, y, b1FC, s, e));
            if (parseAtoms(b, left - 1, skill, typeOfKey, out)) {
                return true;
            }
            out.remove(out.size() - 1);
            b.position(before);
        }
        b.position(mark);
        return false;
    }

    static int typeExtra(int type, int skill) {
        if (type == 33 || type == 74) {
            return BAT_EXTRA.contains(skill) ? 16 : 12;
        }
        if (COUNT_TYPES.contains(type)) {
            return 4;
        }
        return type == 32 ? 16 : 0;
    }

    /**
     * The body the 265 SkillHandler.handleForceAtomCollision reads: i32 size, i32 skillId, i32 size2,
     * size2 x (skillId, key, unk 0, x, y), size x (key, u8, mobId, createTime, owner, unk 0, x, y, [skill 0: u8, i32]).
     */
    public byte[] toGms265Body() {
        ByteBuffer o = ByteBuffer.allocate(12 + guided.size() * 20 + atoms.size() * (33 + 5)).order(ByteOrder.LITTLE_ENDIAN);
        o.putInt(atoms.size()).putInt(skillId).putInt(guided.size());
        for (Guided g : guided) {
            o.putInt(g.skillId()).putInt(g.id()).putInt(0).putInt(g.x()).putInt(g.y());
        }
        for (Atom a : atoms) {
            o.putInt(a.key()).put((byte) (a.negative() ? 1 : 0)).putInt(a.mobId()).putInt(a.v188()).putInt(a.vFC())
                    .putInt(0).putInt(a.x()).putInt(a.y());
            if (skillId == 0) {
                o.put((byte) a.b1FC()).putInt(a.skillId());
            }
        }
        return Arrays.copyOf(o.array(), o.position());
    }

    private static int i32(ByteBuffer b) {
        if (b.remaining() < 4) {
            throw new IllegalArgumentException("CP01C4 truncated");
        }
        return b.getInt();
    }

    private static byte[] bytes(ByteBuffer b, int n) {
        if (b.remaining() < n) {
            throw new IllegalArgumentException("CP01C4 truncated");
        }
        byte[] r = new byte[n];
        b.get(r);
        return r;
    }
}
