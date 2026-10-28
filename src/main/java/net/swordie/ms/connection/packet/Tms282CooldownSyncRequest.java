package net.swordie.ms.connection.packet;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedHashMap;
import java.util.Map;

/** Native5FF6DCE: count, count*(skillId, signed remaining milliseconds). Unlike265, no leading tick. */
public record Tms282CooldownSyncRequest(Map<Integer,Integer> remaining) {
    public static final int OPCODE=0x0420;
    public Tms282CooldownSyncRequest { remaining=Map.copyOf(remaining); }
    public static Tms282CooldownSyncRequest parse(byte[] packet) {
        if(packet==null || packet.length<6)throw new IllegalArgumentException("CP0420 truncated");
        var in=ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
        if(Short.toUnsignedInt(in.getShort())!=OPCODE)throw new IllegalArgumentException("Not CP0420");
        int count=in.getInt();
        if(count<0 || count>512 || (long)count*8!=in.remaining())throw new IllegalArgumentException("CP0420 invalid count/length");
        Map<Integer,Integer> values=new LinkedHashMap<>();
        for(int i=0;i<count;i++) {
            int skill=in.getInt(),remaining=in.getInt();
            if(skill<=0 || values.putIfAbsent(skill,remaining)!=null)throw new IllegalArgumentException("CP0420 invalid/duplicate skill");
        }
        return new Tms282CooldownSyncRequest(values);
    }
}
