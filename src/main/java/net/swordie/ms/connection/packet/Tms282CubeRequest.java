package net.swordie.ms.connection.packet;

import net.swordie.ms.enums.InvType;
import java.nio.*;
import java.util.HexFormat;

/** Active CP0780 cube RPCs, native36B3AC0/36B3DC0/36B40C0.
 * The native request builders call36A74A0 with count1. F91250 encodes that count using signed ZigZag (02).
 * The callback token is opaque correlation, never authority over a cube or an equipment serial number.
 */
public record Tms282CubeRequest(Kind kind,int cubeSlot,int equipPosition,int extra,int flag1,int flag2,
                               String opaqueStringHex,int callbackToken) {
    public enum Kind {
        CONSUME(0xC516F80F,InvType.CONSUME,false),
        BONUS_CONSUME(0x46E382FC,InvType.CONSUME,true),
        BONUS_CASH(0xC062AF03,InvType.CASH,true);
        private final int key;private final InvType inventory;private final boolean bonus;
        Kind(int key,InvType inventory,boolean bonus){this.key=key;this.inventory=inventory;this.bonus=bonus;}
        public int key(){return key;}public InvType inventory(){return inventory;}public boolean bonus(){return bonus;}
        /** Exact native branch membership, separate from server rule support. Evidence: native-registry.json. */
        public boolean acceptsItem(int id) {
            return switch(this) {
                case CONSUME -> switch(id) {
                    case 2710000,2711000,2711001,2711003,2711004,2711005,2711006,2711007,2711008,2711009,
                         2711010,2711011,2711012,2711013,2711014,2711015,2711016,2711017,2711018,2711019,
                         2711020,2711021,2711022,2711023,2711026,2711027,2711028 -> true;
                    default -> false;
                };
                case BONUS_CONSUME -> switch(id) {
                    case 2730000,2730001,2730002,2730004,2730005,2730006,2730013,2730020,
                         2730039,2730042,2730043,2730044,2730055 -> true;
                    default -> false;
                };
                case BONUS_CASH -> id==5062507 || id==5062508 || id==5062520;
            };
        }
        static Kind from(int key){for(Kind k:values())if(k.key==key)return k;throw new IllegalArgumentException("Unsupported cube RPC interface");}
    }
    public static Tms282CubeRequest parse(byte[] packet) {
        if(packet==null || packet.length<33 || packet.length>2081)throw new IllegalArgumentException("Cube RPC length");
        var in=ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
        try {
            if(Short.toUnsignedInt(in.getShort())!=0x780)throw new IllegalArgumentException("Cube RPC opcode");
            Kind kind=Kind.from(in.getInt());
            if(in.getShort()!=0 || Byte.toUnsignedInt(in.get())!=0x3f)
                throw new IllegalArgumentException("Cube RPC method/required fields");
            if(in.get()!=2)throw new IllegalArgumentException("Single cube request required");
            int slot=in.getInt(),equip=in.getInt(),extra=in.getInt();
            int f1=Byte.toUnsignedInt(in.get()),f2=Byte.toUnsignedInt(in.get());
            int length=Short.toUnsignedInt(in.getShort());
            if(length>2048 || in.remaining()!=length+7)throw new IllegalArgumentException("Cube RPC string/trailer length");
            byte[] text=new byte[length];in.get(text);
            if(in.get()!=1 || Short.toUnsignedInt(in.getShort())!=0x245)
                throw new IllegalArgumentException("Cube RPC callback destination");
            int token=in.getInt();
            if(slot<=0 || slot>Short.MAX_VALUE || equip==0 || equip<=Short.MIN_VALUE || equip>Short.MAX_VALUE
                    || extra<0 || extra>Short.MAX_VALUE || f1>1 || f2>1)throw new IllegalArgumentException("Cube RPC positions/flags");
            return new Tms282CubeRequest(kind,slot,equip,extra,f1,f2,HexFormat.of().formatHex(text),token);
        } catch(BufferUnderflowException e){throw new IllegalArgumentException("Truncated cube RPC",e);}
    }
}
