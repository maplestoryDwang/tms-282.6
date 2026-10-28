package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.protocol.codec.MovePathCodec;
import net.swordie.ms.connection.protocol.codec.Tms282MovePathCodec;
import net.swordie.ms.life.movement.MovementInfo;

/**
 * CP0545 NPC_MOVE [verified: static] (265 0x52A; senders 40D5BA0, 5C72EF0; ingame-cp-map-2823.tsv):
 * {@code u32 objectId, u8 oneTimeAction, u8 chatIdx, u32 duration} and, for a moving NPC, the CMovePath through
 * 3FDB6B0 (the UserMove encoder: 282 element table, key pad included). Sent by the NPC's controller only.
 *
 * @param path null when the packet ends after the duration (265 NpcHandler: a path only if bytes remain)
 */
public record Tms282NpcMoveRequest(int objectId, int oneTimeAction, int chatIdx, int duration, MovementInfo path) {
    public static final int OPCODE = 0x0545;

    public static Tms282NpcMoveRequest parse(byte[] packet) {
        return parse(packet, Tms282MovePathCodec.INSTANCE);
    }

    public static Tms282NpcMoveRequest parse(byte[] packet, MovePathCodec paths) {
        Tms282FieldRequestReader r = Tms282FieldRequestReader.open(packet, OPCODE, "CP0545");
        int objectId = r.i32();
        int oneTimeAction = r.u8();
        int chatIdx = r.u8();
        int duration = r.i32();
        MovementInfo path = r.remaining() > 0 ? r.path(paths) : null;
        r.end();
        return new Tms282NpcMoveRequest(objectId, oneTimeAction, chatIdx, duration, path);
    }
}
