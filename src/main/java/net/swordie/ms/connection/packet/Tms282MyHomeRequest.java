package net.swordie.ms.connection.packet;

import java.nio.*;

/** Decoding only. Runtime must authorize account/world AND the current world's owned controller ID. */
public final class Tms282MyHomeRequest {
    private Tms282MyHomeRequest() { }
    public sealed interface Action permits Enter,Consume,Activate,Install,Delete,StoreAll { }
    public record Enter(int accountId) implements Action { }
    public record Consume(int itemId) implements Action { }
    public record Activate(long serial,Integer option,int token) implements Action { }
    public record Install(long serial,int gridKey,int x,int y,boolean flipped) implements Action { }
    public record Delete(long serial) implements Action { }
    public record StoreAll() implements Action { }
    private static ByteBuffer exact(byte[] bytes,int length) {
        if(bytes==null || bytes.length!=length)throw new IllegalArgumentException("Invalid housing request length");
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
    }
    /** CP0782 payload without opcode. All other GameUser methods belong to other route owners. */
    public static Action gameUser(byte[] bytes) {
        if(bytes!=null && bytes.length>=6 && ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getInt()==0x0A1A383A
                && Short.toUnsignedInt(ByteBuffer.wrap(bytes,4,2).order(ByteOrder.LITTLE_ENDIAN).getShort())==13){
            if(bytes.length!=22 && bytes.length!=26)throw new IllegalArgumentException("Invalid activation length");
            var in=ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);in.position(6);long serial=in.getLong();int present=Byte.toUnsignedInt(in.get());
            if(serial<=0 || present>1 || bytes.length!=(present==0?22:26))throw new IllegalArgumentException("Invalid activation fields");
            Integer option=present==0?null:in.getInt();
            if(in.get()!=1 || Short.toUnsignedInt(in.getShort())!=0x245)throw new IllegalArgumentException("Invalid activation response envelope");
            return new Activate(serial,option,in.getInt());
        }
        var in=exact(bytes,10);
        if(in.getInt()!=0x0A1A383A)throw new IllegalArgumentException("Not GameUser RPC");
        int method=Short.toUnsignedInt(in.getShort()),value=in.getInt();
        if(value<=0)throw new IllegalArgumentException("Invalid home request target");
        return switch(method){case 8->new Enter(value);case 12->new Consume(value);default->throw new IllegalArgumentException("Not an implemented housing request");};
    }
    /** Controller local methods0/2/4 follow84 inherited methods, verified from original runtime metadata.
     * Args exclude actor-frame kind/length/objectId/method. This is not a generic CP027D frame parser. */
    public static Action controller(int method,byte[] arguments) {
        if(method==88){exact(arguments,0);return new StoreAll();}
        var in=exact(arguments,method==84?21:method==86?8:-1);
        long serial=in.getLong();if(serial<=0)throw new IllegalArgumentException("Invalid furniture serial");
        if(method==86)return new Delete(serial);
        int grid=in.getInt(),x=in.getInt(),y=in.getInt();int flip=Byte.toUnsignedInt(in.get());
        if(flip>1)throw new IllegalArgumentException("Invalid furniture flip flag");
        return new Install(serial,grid,x,y,flip==1);
    }
}
