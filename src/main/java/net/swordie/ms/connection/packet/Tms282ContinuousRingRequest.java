package net.swordie.ms.connection.packet;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** Native282 RVA58FA1E0: CP03F8 + triggering skill + passive source skill + passive source level.
 * The client fields are observations only; the server still needs an accepted matching skill use,
 * the actual equipped ring and server-owned preparation state before granting any effect. */
public record Tms282ContinuousRingRequest(int triggeringSkill, int sourceSkill, int sourceLevel) {
    public static final int OPCODE=0x03F8;
    public static Tms282ContinuousRingRequest parse(byte[] packet) {
        if(packet==null || packet.length!=14)throw new IllegalArgumentException("CP03F8 requires exactly 14 bytes");
        ByteBuffer in=ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
        if(Short.toUnsignedInt(in.getShort())!=OPCODE)throw new IllegalArgumentException("Wrong CP03F8 opcode");
        int trigger=in.getInt(),source=in.getInt(),level=in.getInt();
        if(trigger<=0 || trigger==80003341 || trigger==80003342 || source!=80003341 || level<1 || level>6)
            throw new IllegalArgumentException("Invalid continuous ring source");
        return new Tms282ContinuousRingRequest(trigger,source,level);
    }
}
