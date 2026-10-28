package net.swordie.ms.connection.packet;

import java.nio.*;

/** CP0780 UserMemorialFreeCube/UserMemorialFreeAdditionalCube. No live mutation or guessed selection mapping. */
public record Tms282CubeChoiceRequest(Kind kind,boolean selection,int cubeSlot,int equipPosition,int mode,
                                     int selectedIndex,int callbackToken) {
    public enum Kind {
        CONSUME(0x58FF0B43,false,14),BONUS_CONSUME(0x0357ADA4,true,15);
        private final int key,restoreMethod;private final boolean bonus;
        Kind(int key,boolean bonus,int restoreMethod){this.key=key;this.bonus=bonus;this.restoreMethod=restoreMethod;}
        public int key(){return key;}public boolean bonus(){return bonus;}public int restoreMethod(){return restoreMethod;}
        public boolean supportedItem(int id){return this==CONSUME?id==2711024||id==2711025:id==2730011||id==2730012;}
        static Kind from(int key){for(Kind k:values())if(k.key==key)return k;throw new IllegalArgumentException("Unsupported choice cube interface");}
    }
    public static Tms282CubeChoiceRequest parse(byte[] packet) {
        if(packet==null || packet.length<20 || packet.length>29)throw new IllegalArgumentException("Choice cube RPC length");
        var in=ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
        try {
            if(Short.toUnsignedInt(in.getShort())!=0x780)throw new IllegalArgumentException("Choice cube opcode");
            Kind kind=Kind.from(in.getInt());int method=Short.toUnsignedInt(in.getShort()),mask=Byte.toUnsignedInt(in.get());
            int slot=0,equip=0,mode=0,index=0;
            if(method==0) {
                if(packet.length!=29 || mask!=7 || in.get()!=2)throw new IllegalArgumentException("Single-cube choice request required");
                slot=in.getInt();equip=in.getInt();mode=in.getInt();
                if(slot<=0 || slot>Short.MAX_VALUE || equip==0 || equip<=Short.MIN_VALUE || equip>Short.MAX_VALUE || mode<0 || mode>255)
                    throw new IllegalArgumentException("Choice cube positions/mode");
            } else if(method==1) {
                if(packet.length!=20 || mask!=1)throw new IllegalArgumentException("Choice selection shape");
                index=in.getInt(); // Native3000 button -> -1;4000 button -> candidate0 (1AB6290/1ADC3E0).
                if(index!=-1 && index!=0)throw new IllegalArgumentException("Unsolicited choice candidate index");
            } else throw new IllegalArgumentException("Unsupported choice cube method");
            if(in.get()!=1 || Short.toUnsignedInt(in.getShort())!=0x245 || in.remaining()!=4)
                throw new IllegalArgumentException("Choice cube callback destination");
            return new Tms282CubeChoiceRequest(kind,method==1,slot,equip,mode,index,in.getInt());
        }catch(BufferUnderflowException e){throw new IllegalArgumentException("Truncated choice cube RPC",e);}
    }
}
