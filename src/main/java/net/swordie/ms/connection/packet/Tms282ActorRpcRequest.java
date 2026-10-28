package net.swordie.ms.connection.packet;

import java.nio.*;
import java.util.*;

/** Shared kind9 envelope only. Method indexes are class-local and must NEVER select the actor's feature. */
public record Tms282ActorRpcRequest(int opcode,int mode,List<Frame> frames) {
    public Tms282ActorRpcRequest(int opcode,List<Frame> frames){this(opcode,0,frames);}
    public Tms282ActorRpcRequest {
        if(opcode!=0x027D && opcode!=0x06D2)throw new IllegalArgumentException("Unknown actor transport");
        if(mode!=0 && mode!=1)throw new IllegalArgumentException("Unsupported actor RPC mode");
        frames=List.copyOf(frames);
        if(frames.isEmpty() || frames.size()>128)throw new IllegalArgumentException("Invalid actor frame count");
    }
    public record Frame(long actorId,int methodIndex,byte[] arguments) {
        public Frame {
            if(actorId<=0 || actorId>0xffffffffL || methodIndex<0 || methodIndex>0xffff || arguments==null || arguments.length>65529)
                throw new IllegalArgumentException("Invalid actor frame");
            arguments=arguments.clone();
        }
        @Override public byte[] arguments(){return arguments.clone();}
    }
    /** Input includes opcode. Parses all boundaries before the caller authorizes or mutates anything. */
    public static Tms282ActorRpcRequest parse(byte[] packet) {
        if(packet==null || packet.length<12 || packet.length>65535)throw new IllegalArgumentException("Invalid actor RPC packet size");
        var in=ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);int opcode=Short.toUnsignedInt(in.getShort());
        if(in.get()!=9)throw new IllegalArgumentException("Unsupported actor RPC kind");
        int mode=Byte.toUnsignedInt(in.get());if(mode>1)throw new IllegalArgumentException("Unsupported actor RPC mode");
        var frames=new ArrayList<Frame>();
        while(in.hasRemaining()){
            if(in.remaining()<2 || frames.size()==128)throw new IllegalArgumentException("Invalid actor RPC boundary/count");
            int length=Short.toUnsignedInt(in.getShort());
            if(length<6 || length>in.remaining())throw new IllegalArgumentException("Invalid actor RPC frame length");
            long actor=Integer.toUnsignedLong(in.getInt());int method=Short.toUnsignedInt(in.getShort());
            byte[] args=new byte[length-6];in.get(args);frames.add(new Frame(actor,method,args));
        }
        return new Tms282ActorRpcRequest(opcode,mode,frames);
    }
    /** Lossless single-frame adapter for an existing feature decoder after actor-ownership classification. */
    public byte[] packetFor(Frame frame) {
        byte[] args=frame.arguments();
        return ByteBuffer.allocate(12+args.length).order(ByteOrder.LITTLE_ENDIAN).putShort((short)opcode)
                .put((byte)9).put((byte)mode).putShort((short)(6+args.length)).putInt((int)frame.actorId())
                .putShort((short)frame.methodIndex()).put(args).array();
    }
}
