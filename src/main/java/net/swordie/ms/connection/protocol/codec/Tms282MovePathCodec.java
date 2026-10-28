package net.swordie.ms.connection.protocol.codec;

import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.life.movement.Movement;
import net.swordie.ms.life.movement.MovementInfo;
import net.swordie.ms.util.Position;

import java.util.ArrayList;
import java.util.List;

/**
 * TMS 282.3 CMovePath (CMovePath::Encode 3FDB6B0 / client decode 13DC8B0; ingame-cp-map-2823.md section 3.1):
 * <pre>
 * I [path+58]  I [path+40]  S x S y  S vx S vy  S count
 * count x ( B type, fields of {@link Tms282MoveShape#of(int) the type's shape} )
 * B keyPad n, ceil(n/2) x B (two 4-bit keys per byte)      -- client packets only, see encodeBroadcast
 * </pre>
 * Shared by CP USER_MOVE 0106, CP MOB_MOVE 0516, CP NPC_MOVE 0545 and the LP broadcasts REMOTE_MOVE 046D and
 * MOB_MOVE 06A0. Elements decode to {@link Tms282MoveElement}.
 */
public final class Tms282MovePathCodec implements MovePathCodec {
    public static final Tms282MovePathCodec INSTANCE = new Tms282MovePathCodec();

    private Tms282MovePathCodec() {
    }

    @Override
    public MovementInfo decode(InPacket in) {
        int gatherDuration = in.decodeInt();
        int unkInt = in.decodeInt();
        Position oldPos = in.decodePosition();
        Position oldVPos = in.decodePosition();
        int count = in.decodeShort() & 0xFFFF;
        List<Movement> elements = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int type = in.decodeByte() & 0xFF;
            elements.add(Tms282MoveElement.decode(type, in));
        }
        byte keyPadState = in.decodeByte();
        int keyBytes = ((keyPadState & 0xFF) + 1) / 2;
        List<Byte> keys = new ArrayList<>(keyBytes);
        for (int i = 0; i < keyBytes; i++) {
            keys.add(in.decodeByte());
        }
        return new MovementInfo(gatherDuration, unkInt, oldPos, oldVPos, elements, keyPadState, keys);
    }

    /** The path as the client sends it (CMovePath::Encode 3FDB6B0): header, elements, key pad. */
    @Override
    public void encode(OutPacket out, MovementInfo path) {
        int keyBytes = ((path.getKeyPadState() & 0xFF) + 1) / 2;
        if (path.getKeyPadList().size() != keyBytes) {
            throw new IllegalArgumentException("Key pad: " + (path.getKeyPadState() & 0xFF) + " keys need "
                    + keyBytes + " bytes, path has " + path.getKeyPadList().size());
        }
        encodeBroadcast(out, path);
        out.encodeByte(path.getKeyPadState());
        for (byte b : path.getKeyPadList()) {
            out.encodeByte(b);
        }
    }

    /**
     * The path in LP REMOTE_MOVE 046D / MOB_MOVE 06A0: both handlers call 3FDD1D0 with keyPad = 0 (5ACD8B4,
     * 3EA6775), which reads the header and the elements only.
     */
    @Override
    public void encodeBroadcast(OutPacket out, MovementInfo path) {
        List<Movement> elements = path.getMovements();
        for (Movement m : elements) {
            if (!(m instanceof Tms282MoveElement)) {
                throw new IllegalArgumentException("Not a 282 path element: " + m.getClass().getSimpleName()
                        + " type " + (m.getCommand() & 0xFF) + " (265 types are numbered differently)");
            }
        }
        out.encodeInt(path.getEncodedGatherDuration());
        out.encodeInt(path.getUnkInt());
        out.encodePosition(path.getOldPos());
        out.encodePosition(path.getOldVPos());
        out.encodeShort(elements.size());
        for (Movement m : elements) {
            m.encode(out);
        }
    }
}
