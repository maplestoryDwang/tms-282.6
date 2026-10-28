package net.swordie.ms.connection.packet;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/** Native kind9 AFamiliarController RPC framing. No actor lookup, authorization or execution.
 * Includes the outer two-byte opcode. Normal verified mode0 only; unverified modes fail closed.
 * Method indexes include WZ2Object5 + AActor79. String bytes have no assumed charset here.
 */
public record Tms282FamiliarRpcRequest(int opcode, int mode, List<Frame> frames) {
    public Tms282FamiliarRpcRequest { frames = List.copyOf(frames); }
    public record Frame(long actorId, int methodIndex, Arguments arguments) { }
    public sealed interface Arguments permits Empty, Serial, Register, Rename, ReleaseMany, Lock, Favorite, Cube, MemorialCube, MemorialSelect, Upgrade, Link, SlotIndex, Extend { }
    public record Empty() implements Arguments { }
    /** Raw64-bit serial; signedness/ownership is not inferred from transport. */
    public record Serial(long bits) implements Arguments { }
    /** Native UI362EC38 passes GW_ItemSlotBase+48 through7693780: consume inventory slot, not item ID. */
    public record Register(int consumeSlot) implements Arguments {
        public Register { if (consumeSlot <= 0) throw invalid("Invalid consume inventory slot"); }
    }
    public record Rename(long serialBits, byte[] nameBytes) implements Arguments {
        public Rename { nameBytes = nameBytes.clone(); }
        @Override public byte[] nameBytes() { return nameBytes.clone(); }
    }
    public record ReleaseMany(List<Long> serialBits) implements Arguments {
        public ReleaseMany { serialBits = List.copyOf(serialBits); }
    }
    /** Native string/i32/u8; contents and numeric meanings remain unverified. */
    public record Lock(byte[] textBytes, int value, int flag) implements Arguments {
        public Lock { textBytes = textBytes.clone(); }
        @Override public byte[] textBytes() { return textBytes.clone(); }
    }
    public record Favorite(long serialBits, int flag) implements Arguments { }
    /** Wire 103 UseFamiliarCube (76A28D0): i32 cash slot, i64 serial. */
    public record Cube(int cashSlot, long serialBits) implements Arguments { }
    /** Wire 104 UseFamiliarMemorialCube (76A2B70): i64 serial, vector of i32 cash slots, i32 count, u8 flag. */
    public record MemorialCube(long serialBits, List<Integer> cashSlots, int count, int flag) implements Arguments {
        public MemorialCube { cashSlots = List.copyOf(cashSlots); }
    }
    /** Wire 105 SelectFamiliarMemorialCubeOption (76A2D60): i64 serial, i32 choice. */
    public record MemorialSelect(long serialBits, int choice) implements Arguments { }
    /** Wire 91 UpgradeFamiliar (76A12F0): i64 target, i64 material (0 = none), i32 essence. */
    public record Upgrade(long targetBits, long materialBits, int essence) implements Arguments { }
    /** Wire 92 LinkFamiliarCard (76A14C0): i32 slot, i64 serial. */
    public record Link(int slot, long serialBits) implements Arguments { }
    /** Wires 119 UnlockLinkSlot / 120 ActivateLinkSlot: u8 index / slot. */
    public record SlotIndex(int index) implements Arguments { }
    /** Wire 121 ExtendLinkSlot (76A4990): u8 slot, i64 client FILETIME now. */
    public record Extend(int slot, long clientTime) implements Arguments { }

    public static Tms282FamiliarRpcRequest parse(byte[] packet) {
        if (packet == null || packet.length < 12) throw invalid("Missing RPC frame");
        ByteBuffer in = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
        int opcode = Short.toUnsignedInt(in.getShort());
        if (opcode != 0x027D && opcode != 0x06D2) throw invalid("Unsupported RPC transport opcode");
        if (Byte.toUnsignedInt(in.get()) != 9) throw invalid("Expected RPC kind9");
        int mode = Byte.toUnsignedInt(in.get());
        // flag byte (6E7AB8D writes a client state bool): 0 in the traced fixtures, 1 for the live card register
        // CP027D of 09-29 (09 01 0a00 ...); the frame layout is the same for both.
        if (mode > 1) throw invalid("Unverified RPC mode");
        List<Frame> frames = new ArrayList<>();
        while (in.hasRemaining()) {
            need(in, 2);
            int length = Short.toUnsignedInt(in.getShort()); // excludes this length field; includes actor+method
            if (length < 6 || length > in.remaining()) throw invalid("Invalid RPC frame length");
            ByteBuffer body = in.slice().order(ByteOrder.LITTLE_ENDIAN); body.limit(length);
            in.position(in.position() + length);
            long actor = Integer.toUnsignedLong(body.getInt());
            if (actor == 0) throw invalid("Missing network actor ID");
            int method = Short.toUnsignedInt(body.getShort());
            Arguments arguments = switch (method) {
                case 84, 100 -> new Serial(i64(body));
                case 85 -> new Empty();
                case 86 -> new Register(i32(body));
                case 99 -> new Rename(i64(body), string(body));
                case 101 -> {
                    int count = length(body);
                    if (count > body.remaining() / 8) throw invalid("Truncated serial vector");
                    List<Long> serials = new ArrayList<>(count);
                    for (int i = 0; i < count; i++) serials.add(i64(body));
                    yield new ReleaseMany(serials);
                }
                case 116 -> new Lock(string(body), i32(body), u8(body));
                case 117 -> new Favorite(i64(body), u8(body));
                case 103 -> new Cube(i32(body), i64(body));
                case 104 -> {
                    long serial = i64(body);
                    int count = length(body);
                    if (count > body.remaining() / 4) throw invalid("Truncated slot vector");
                    List<Integer> slots = new ArrayList<>(count);
                    for (int i = 0; i < count; i++) slots.add(i32(body));
                    yield new MemorialCube(serial, slots, i32(body), u8(body));
                }
                case 105 -> new MemorialSelect(i64(body), i32(body));
                case 91 -> new Upgrade(i64(body), i64(body), i32(body));
                case 92 -> new Link(i32(body), i64(body));
                case 119, 120 -> new SlotIndex(u8(body));
                case 121 -> new Extend(u8(body), i64(body));
                default -> throw invalid("Unsupported or callback Familiar RPC method");
            };
            if (body.hasRemaining()) throw invalid("Unexpected RPC argument tail");
            frames.add(new Frame(actor, method, arguments));
        }
        return new Tms282FamiliarRpcRequest(opcode, mode, frames);
    }
    private static byte[] string(ByteBuffer in) {
        int length = length(in); need(in, length);
        byte[] bytes = new byte[length]; in.get(bytes); return bytes;
    }
    /** Native90F080 writes nonnegative N as unsigned LEB128(2*N). Native string/vector limit is <2^20. */
    private static int length(ByteBuffer in) {
        int encoded = 0;
        for (int index = 0; index < 3; index++) {
            int next = u8(in); encoded |= (next & 127) << (index * 7);
            if ((next & 128) == 0) {
                if (index > 0 && next == 0 || (encoded & 1) != 0) throw invalid("Noncanonical or negative length");
                int length = encoded >>> 1;
                if (length >= 0x100000) throw invalid("Length outside native limit");
                return length;
            }
        }
        throw invalid("Oversized length varint");
    }
    private static int u8(ByteBuffer in) { need(in, 1); return Byte.toUnsignedInt(in.get()); }
    private static int i32(ByteBuffer in) { need(in, 4); return in.getInt(); }
    private static long i64(ByteBuffer in) { need(in, 8); return in.getLong(); }
    private static void need(ByteBuffer in, int size) { if (size < 0 || in.remaining() < size) throw invalid("Truncated RPC arguments"); }
    private static IllegalArgumentException invalid(String message) { return new IllegalArgumentException(message); }
}
