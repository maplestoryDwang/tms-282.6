package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.protocol.codec.MovePathCodec;
import net.swordie.ms.connection.protocol.codec.Tms282MovePathCodec;
import net.swordie.ms.life.movement.MovementInfo;

import java.util.Objects;

/**
 * CP0106 USER_MOVE [verified] (265 0x104; sender 13EF190, site 13EF41A; ingame-cp-map-2823.md 3.1):
 * <pre>
 * u8  fieldKey         13EF435 (3734E10(field))
 * u32 fieldCrc         13EF44E (37725B0(field), 265 "something with field")
 * u32 tick             13EF45F
 * u8  flag             13EF471 ([rsp+590], 265 "doesn't get set at all")
 * CMovePath::Encode 3FDB6B0: header, count x element (282 types, 5-byte tail), key pad   13EF492
 * </pre>
 * No trailing u32 (0x106 is not in the 346A070 list). The live samples of sessions channel-1790158495223 and
 * channel-1790159554177 are read to their last byte (Tms282FieldMovementCheck).
 */
public record Tms282UserMoveRequest(int fieldKey, int fieldCrc, int tick, int flag, MovementInfo path) {
    public static final int OPCODE = 0x0106;

    public Tms282UserMoveRequest {
        Objects.requireNonNull(path, "path");
    }

    public static Tms282UserMoveRequest parse(byte[] packet) {
        return parse(packet, Tms282MovePathCodec.INSTANCE);
    }

    /** Whole plaintext packet, opcode included; must be read exactly. */
    public static Tms282UserMoveRequest parse(byte[] packet, MovePathCodec paths) {
        Tms282FieldRequestReader r = Tms282FieldRequestReader.open(packet, OPCODE, "CP0106");
        int fieldKey = r.u8();
        int crc = r.i32();
        int tick = r.i32();
        int flag = r.u8();
        MovementInfo path = r.path(paths);
        r.end();
        return new Tms282UserMoveRequest(fieldKey, crc, tick, flag, path);
    }
}
