package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;

/** CP0780 basic-cube RPC callbacks on LP0245. Outer decoder7028210 correlates a pending token,
 * reads transport status, then calls36C48E0/36C4820/36C4760 (consume/bonus-consume/bonus-cash).
 * Each callback reads mask0F plus i32,i32,i64,i32. It is NOT the old GMS CUBE_SYSTEM_RELEASE body.
 */
public final class Tms282CubeRpcPackets {
    public static final int OPCODE=0x0245;
    private Tms282CubeRpcPackets() { }

    /** LP0158 ->5FD27B0 clears CharacterData+105C MemorialData and raises its UI event; no payload. */
    public static OutPacket clearChoice() { return Tms282LoginPacketSupport.packet(0x0158); }

    /** Envelope for a native callback-specific typed body. */
    public static OutPacket callback(int token, byte[] typedPayload) {
        if ((token >>> 24) == 0 || typedPayload == null || typedPayload.length == 0)
            throw new IllegalArgumentException("Invalid cube RPC callback");
        OutPacket out = Tms282LoginPacketSupport.packet(OPCODE);
        out.encodeInt(token);
        out.encodeInt(0);
        out.encodeArr(typedPayload);
        return out;
    }

    public static OutPacket result(int token,int resultCode,int resultFlag,long equipmentSerial,int remaining) {
        // Native token lookup requires a nonzero generation byte; signed tokens remain valid.
        if((token>>>24)==0 || remaining<0 || equipmentSerial<0)
            throw new IllegalArgumentException("Invalid cube RPC reply");
        OutPacket out=Tms282LoginPacketSupport.packet(OPCODE);
        out.encodeInt(token);
        out.encodeInt(0); // RPC transport success; application failure is in resultCode.
        out.encodeByte(0x0F); // All result fields are initialized, including error replies.
        out.encodeInt(resultCode);
        out.encodeInt(resultFlag);
        out.encodeLong(equipmentSerial);
        out.encodeInt(remaining);
        return out;
    }

    /** Native resultCode!=0 path releases the request lock; nothing has been charged. */
    public static OutPacket refused(int token) { return result(token,1,0,0,0); }
}
