package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;

/** CP03EE kind 2 (3AAFEB0): Illium javelin target state, mirrored by LP032E/3AD2460 -> vtable+48/3AAFFF0.
 * +258 is preserved as an opaque native state integer; it is not the server's recreation counter.
 */
public record Tms282ForceAtomUpdateRequest(int key, int targetId, int state, int flip,
                                         short left, short top, short right, short bottom) {
    public static final int OPCODE = 0x03EE;
    public static final int REMOTE = 0x032E;

    public static Tms282ForceAtomUpdateRequest parse(byte[] packet) {
        var r = Tms282FieldRequestReader.open(packet, OPCODE, "CP03EE");
        if (r.i32() != 2) throw new IllegalArgumentException("Unsupported CP03EE native kind");
        int key = r.i32(), target = r.i32(), state = r.i32(), flip = r.u8();
        short left = r.i16(), top = r.i16(), right = r.i16(), bottom = r.i16();
        r.i32(); // 346A070 transport trailer
        r.end();
        if (key <= 0 || target < 0 || flip > 1 || left > right || top > bottom)
            throw new IllegalArgumentException("Invalid CP03EE javelin state");
        return new Tms282ForceAtomUpdateRequest(key, target, state, flip, left, top, right, bottom);
    }

    /** LP032E has no kind field: the existing atom's vtable selects its payload decoder. */
    public OutPacket remote(int ownerId) {
        OutPacket out = Tms282LoginPacketSupport.packet(REMOTE);
        out.encodeInt(key); out.encodeInt(ownerId);
        out.encodeInt(targetId); out.encodeInt(state); out.encodeByte(flip);
        out.encodeShort(left); out.encodeShort(top); out.encodeShort(right); out.encodeShort(bottom);
        return out;
    }
}
