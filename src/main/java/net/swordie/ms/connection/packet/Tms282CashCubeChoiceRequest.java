package net.swordie.ms.connection.packet;

import java.nio.*;
import java.util.HexFormat;
import net.swordie.ms.enums.InvType;

/** Original cash restoration RPCs36B31E0/36B5750; their fields differ from consume-choice requests. */
public record Tms282CashCubeChoiceRequest(Kind kind,boolean selection,int cubeSlot,int equipPosition,
                                        int extra,int mode,int flag1,int flag2,String opaqueStringHex,
                                        int selectedIndex,int callbackToken) {
    public enum Kind {
        CASH(0xF0A4E6D7,false,11),BONUS_CASH(0xCEACEA1B,true,13);
        private final int key;private final boolean bonus;private final int restoreMethod;
        Kind(int key,boolean bonus,int restoreMethod){this.key=key;this.bonus=bonus;this.restoreMethod=restoreMethod;}
        public int key(){return key;}public boolean bonus(){return bonus;}public InvType inventory(){return InvType.CASH;}
        /** Original5E9B430 jump table, leading to36A6BD0/36A6A80; distinct from consume14/15. */
        public int restoreMethod(){return restoreMethod;}
        /** Only these requested, independently evidenced rule families; specialized/karma base IDs stay separate. */
        public boolean supportedItem(int id){return this==CASH?id==5062030:id==5062031||id==5062521;}
        static Kind from(int key){for(var k:values())if(k.key==key)return k;throw new IllegalArgumentException("Unsupported cash choice interface");}
    }
    public static boolean handles(int key){for(var k:Kind.values())if(k.key()==key)return true;return false;}
    public static Tms282CashCubeChoiceRequest parse(byte[] packet) {
        if(packet==null||packet.length<20||packet.length>2085)throw new IllegalArgumentException("Cash choice request length");
        var in=ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
        try {
            if(Short.toUnsignedInt(in.getShort())!=0x780)throw new IllegalArgumentException("Cash choice opcode");
            Kind kind=Kind.from(in.getInt());int method=Short.toUnsignedInt(in.getShort()),mask=Byte.toUnsignedInt(in.get());
            int slot=0,equip=0,extra=0,mode=0,f1=0,f2=0,index=0;String text="";
            if(method==0){
                if(mask!=(kind.bonus()?0x7f:0x3b)||in.get()!=2)throw new IllegalArgumentException("Single cash choice required fields");
                slot=in.getInt();
                if(kind.bonus()){equip=in.getInt();extra=in.getInt();mode=in.getInt();}
                else equip=in.getShort(); //36B33C8 movzx word: wire is signed equipment position16, neveri32.
                f1=Byte.toUnsignedInt(in.get());f2=Byte.toUnsignedInt(in.get());int length=Short.toUnsignedInt(in.getShort());
                if(length>2048||in.remaining()!=length+7)throw new IllegalArgumentException("Cash choice string/callback length");
                byte[] opaque=new byte[length];in.get(opaque);text=HexFormat.of().formatHex(opaque);
                if(slot<=0||slot>Short.MAX_VALUE||equip==0||equip<=Short.MIN_VALUE||equip>Short.MAX_VALUE
                        ||extra<0||extra>Short.MAX_VALUE||mode<0||mode>255||f1>1||f2>1)
                    throw new IllegalArgumentException("Cash choice positions/flags");
            }else if(method==1){
                if(packet.length!=20||mask!=1)throw new IllegalArgumentException("Cash choice selection fields");
                index=in.getInt();if(index!=-1&&index!=0)throw new IllegalArgumentException("Unowned cash choice candidate index");
            }else throw new IllegalArgumentException("Unsupported cash choice method");
            if(in.get()!=1||Short.toUnsignedInt(in.getShort())!=0x245||in.remaining()!=4)throw new IllegalArgumentException("Cash choice callback destination");
            return new Tms282CashCubeChoiceRequest(kind,method==1,slot,equip,extra,mode,f1,f2,text,index,in.getInt());
        }catch(BufferUnderflowException e){throw new IllegalArgumentException("Truncated cash choice",e);}
    }
}
