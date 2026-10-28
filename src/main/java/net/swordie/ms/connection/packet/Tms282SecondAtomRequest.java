package net.swordie.ms.connection.packet;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * TMS 282.3 second atom client requests (research/tms2823/second-atom/README.md). All five opcodes are in the
 * 346A070 tail list (0x69C-0x6A0): the socket layer appends a u32 after the body, which every parser here requires
 * and drops. Each {@code parse} takes the whole packet (2-byte opcode first) and rejects any other length with
 * {@link IllegalArgumentException}.
 * <pre>
 * CP069C COMMAND   (105ECC0)  u8, u8, i32 count, count x (i32 atomId, i32 n, n x (i32 type, type body)), [u32]
 *                            = 265 USER_SECOND_ATOM_COMMAND_REQUEST (1647; Lara reads u8, u8, i32, i32 atomId)
 * CP069D REMOVE    (303E79D)  i32 count (&lt;= 50), count x (i32 objectId, 4 bytes, i32 0, i32), [u32]
 *                            265 USER_SECOND_ATOM_REMOVE_REQUEST (1648) reads i32 (skipped) + i32 objectId: one
 *                            265 body {1, objectId} per entry
 * CP069E COLLISION (303E69E)  i32 characterId, i32 count, count x (i32 atomId, i32 mobId, i32 n, n x (i32, i32)), [u32]
 *                            = 265 USER_SECOND_ATOM_COLLISION_REQUEST (1649)
 * CP069F           (304D401)  i32 objectId, [u32]            (282 only, no 265 handler)
 * CP06A0           (3028DE6)  i32 objectId, i32 state, [u32] (282 only, no 265 handler)
 * </pre>
 */
public final class Tms282SecondAtomRequest {
    public static final int COMMAND = 0x069C;
    public static final int REMOVE = 0x069D;
    public static final int COLLISION = 0x069E;
    public static final int AREA = 0x069F;
    public static final int STATE = 0x06A0;

    /** 303E787: at most 50 entries per CP069D. */
    public static final int MAX_REMOVE = 50;
    /** Sanity limits for the collision request (the client sends the atoms that hit in one frame). */
    public static final int MAX_COLLISIONS = 512;
    public static final int MAX_COLLISION_MAP = 256;

    private Tms282SecondAtomRequest() {
    }

    /** The body between the opcode and the tail u32. */
    static ByteBuffer payload(byte[] packet, int opcode, int minBody) {
        if (packet == null || packet.length < 2 + minBody + 4) {
            throw new IllegalArgumentException(String.format("CP%04X too short", opcode));
        }
        int op = (packet[0] & 0xFF) | (packet[1] & 0xFF) << 8;
        if (op != opcode) {
            throw new IllegalArgumentException(String.format("CP%04X expected, got CP%04X", opcode, op));
        }
        return ByteBuffer.wrap(packet, 2, packet.length - 6).slice().order(ByteOrder.LITTLE_ENDIAN);
    }

    static void end(ByteBuffer b, int opcode) {
        if (b.hasRemaining()) {
            throw new IllegalArgumentException(String.format("CP%04X has %d unread bytes before the tail", opcode, b.remaining()));
        }
    }

    static int i32(ByteBuffer b, int opcode) {
        if (b.remaining() < 4) {
            throw new IllegalArgumentException(String.format("CP%04X truncated", opcode));
        }
        return b.getInt();
    }

    /** CP069C: the command entries are opaque per type (client vcall encoders); kept raw. */
    public record Command(int first, int second, int count, int firstAtomId, byte[] entries) {
        public static Command parse(byte[] packet) {
            ByteBuffer b = payload(packet, COMMAND, 2 + 4);
            int first = b.get() & 0xFF;
            int second = b.get() & 0xFF;
            byte[] entries = new byte[b.remaining()];
            b.get(entries);
            ByteBuffer e = ByteBuffer.wrap(entries).order(ByteOrder.LITTLE_ENDIAN);
            int count = i32(e, COMMAND);
            if (count < 0 || count > Tms282SecondAtomTranslators.MAX_COMMAND_ENTRIES) {
                throw new IllegalArgumentException("CP069C entry count " + count);
            }
            int firstAtom = count > 0 ? i32(e, COMMAND) : 0;
            return new Command(first, second, count, firstAtom, entries);
        }

        /** 265 body: the 282 body without the tail (Lara's 265 handler reads u8, u8, i32 count, i32 atomId). */
        public byte[] toGms265Body() {
            byte[] r = new byte[2 + entries.length];
            r[0] = (byte) first;
            r[1] = (byte) second;
            System.arraycopy(entries, 0, r, 2, entries.length);
            return r;
        }

        /** LP0463 for the other users of the field. */
        public net.swordie.ms.connection.OutPacket result(int owner) {
            return Tms282SecondAtomTranslators.commandResult(owner, entries);
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Command c && c.first == first && c.second == second && Arrays.equals(c.entries, entries);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(entries) * 31 + first * 7 + second;
        }
    }

    /** One CP069D entry (303E0xx / 3046020: objectId, 3021D0(), 0, 3028C20()). */
    public record RemoveEntry(int objectId, int reason, int zero, int extra) {
    }

    public record Remove(List<RemoveEntry> entries) {
        public static Remove parse(byte[] packet) {
            ByteBuffer b = payload(packet, REMOVE, 4);
            int count = i32(b, REMOVE);
            if (count < 0 || count > MAX_REMOVE || b.remaining() != 16L * count) {
                throw new IllegalArgumentException("CP069D count " + count + " for " + b.remaining() + " bytes");
            }
            List<RemoveEntry> list = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                list.add(new RemoveEntry(b.getInt(), b.getInt(), b.getInt(), b.getInt()));
            }
            end(b, REMOVE);
            return new Remove(List.copyOf(list));
        }

        /** One 265 USER_SECOND_ATOM_REMOVE_REQUEST body {i32 1, i32 objectId} per entry. */
        public List<byte[]> toGms265Bodies() {
            List<byte[]> r = new ArrayList<>(entries.size());
            for (RemoveEntry e : entries) {
                r.add(ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putInt(1).putInt(e.objectId()).array());
            }
            return r;
        }
    }

    public record Collision(int characterId, int count, byte[] body) {
        public static Collision parse(byte[] packet) {
            ByteBuffer b = payload(packet, COLLISION, 8);
            int chr = i32(b, COLLISION);
            int count = i32(b, COLLISION);
            if (count < 0 || count > MAX_COLLISIONS) {
                throw new IllegalArgumentException("CP069E count " + count);
            }
            for (int i = 0; i < count; i++) {
                i32(b, COLLISION);
                i32(b, COLLISION);
                int n = i32(b, COLLISION);
                if (n < 0 || n > MAX_COLLISION_MAP || b.remaining() < 8L * n) {
                    throw new IllegalArgumentException("CP069E map size " + n);
                }
                b.position(b.position() + 8 * n);
            }
            end(b, COLLISION);
            return new Collision(chr, count, Arrays.copyOfRange(packet, 2, packet.length - 4));
        }

        /** = the 265 body (same layout, no tail). */
        public byte[] toGms265Body() {
            return body.clone();
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Collision c && Arrays.equals(c.body, body);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(body);
        }
    }

    /** CP069F (282 only): an atom reached its area test (304D260). */
    public record Area(int objectId) {
        public static Area parse(byte[] packet) {
            ByteBuffer b = payload(packet, AREA, 4);
            Area a = new Area(b.getInt());
            end(b, AREA);
            return a;
        }
    }

    /** CP06A0 (282 only): the atom's state changed (3028D60 sets atom +0xF4). */
    public record State(int objectId, int state) {
        public static State parse(byte[] packet) {
            ByteBuffer b = payload(packet, STATE, 8);
            State s = new State(b.getInt(), b.getInt());
            end(b, STATE);
            return s;
        }
    }
}
