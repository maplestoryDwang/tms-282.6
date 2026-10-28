package net.swordie.ms.connection.packet;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.ArrayList;
import net.swordie.ms.client.character.skills.info.SkillUseInfo;
import net.swordie.ms.util.Position;
import net.swordie.ms.util.Rect;
import net.swordie.ms.util.container.Tuple;

/** 282 main-stream ProcessType, native1207F20/1208F30. Distinct from the265 payload tags. */
public final class Tms282SkillProcess {
    private Tms282SkillProcess() { }
    /** CP04B4's common process, special-move record and option, before the skill-specific body. */
    public static int decodeUsePrefix(ByteBuffer in, SkillUseInfo skill) {
        decode(in,skill);
        if(in.remaining()<21)throw new IllegalArgumentException("truncated special-move fields");
        in.position(in.position()+19);
        int size=Short.toUnsignedInt(in.getShort());
        if(size>in.remaining()-4)throw new IllegalArgumentException("truncated special-move string");
        in.position(in.position()+size);
        return in.getInt();
    }
    public static void decode(ByteBuffer in, SkillUseInfo skill) {
        in.getInt(); // CRC
        if (!present(in)) return;
        in.getInt();
        while (true) {
            int type=in.getInt();
            if(type==-1)break;
            if(type<1 || type>52)throw new IllegalArgumentException("Unsupported 282 ProcessType " + type);
            // Native1207F98 emits every tag before the switch. Its18 default-table entries emit no presence byte.
            if(!hasPayload(type))continue;
            if (!present(in)) continue;
            switch (type) {
                case 1 -> { skip(in,12);in.get();skip(in,Byte.toUnsignedInt(in.get())*4); }
                case 2 -> { // virtual encoder1204EE0: its optional final bool/int is new in282.
                    skip(in,2);skill.skillId=in.getInt();in.getInt();in.get();
                    skill.endingPosition=position(in);
                    if(skill.skillID==151100002){
                        if(skill.adeleCrystalOrigin!=null)throw new IllegalArgumentException("Duplicate crystal source");
                        skill.adeleCrystalOrigin=skill.endingPosition;
                    }
                    skip(in,16);
                    if(present(in))in.getInt();
                }
                case 3 -> skip(in,9);
                case 4 -> { skill.rect=rect(in);skill.endingPosition=position(in);in.getInt(); }
                case 5,6,10,11,12,13,14,22,23 -> { }
                case 7 -> { skill.endingPosition=position(in);skip(in,4);skill.isLeft=present(in); }
                case 8 -> skill.rect=rect(in);
                case 9 -> { skip(in,8);skill.endingPosition=position(in);skill.isLeft=in.getInt()==-1; }
                case 15 -> { skip(in,count(in,16)*16);skip(in,16); }
                case 19 -> { // live 33001005 捕食者之躍 (recv-388-04b4): the first i32 is the target mob
                    if(skill.skillID==33001005){skill.objectId=in.getInt();skip(in,16);}else skip(in,20);
                }
                case 20,41 -> skip(in,count(in,4)*4);
                case 24 -> skip(in,32);
                case 25 -> { // Native records are28 bytes, not the265 id/position-only12 bytes.
                    int n=count(in,28);skill.shardsPositions=new HashSet<>();
                    boolean adele=Tms282AdeleShardPackets.handles(skill.skillID);
                    if(adele)skill.adeleShardPositions=new ArrayList<>();
                    for(int i=0;i<n;i++){
                        int id=in.getInt();Position p=position(in);var shard=new Tuple<>(id,p);skill.shardsPositions.add(shard);
                        if(adele){skill.adeleShardPositions.add(shard);for(int j=0;j<4;j++)if(in.getInt()!=0)throw new IllegalArgumentException("Unknown Adele shard record");}
                        else skip(in,16);
                    }
                    boolean flag=present(in);
                    if(adele)skill.adeleShardMarked=flag;else skill.isLeft=flag;
                }
                case 26 -> {
                    skill.endingPosition=position(in);
                    if(skill.skillID==151100002){
                        if(skill.adeleCrystalPosition!=null)throw new IllegalArgumentException("Duplicate crystal position");
                        skill.adeleCrystalPosition=skill.endingPosition;
                    }
                }
                case 29,42 -> in.getInt();
                case 34 -> skip(in,count(in,8)*8);
                case 37 -> skip(in,43);
                case 39 -> { skill.count=in.getInt();in.getInt();skip(in,count(in,4)*4); }
                case 44 -> skip(in,17);
                case 46 -> skip(in,count(in,8)*8);
                case 47 -> { skip(in,count(in,8)*8);skip(in,count(in,4)*4); }
                case 49 -> skip(in,42);
                case 50 -> skip(in,8);
                case 52 -> { skip(in,20);skip(in,count(in,4)*4); }
                default -> throw new AssertionError(type);
            }
        }
        // 1208DA6/F91250: ZigZag varint byte-vector length, followed by that exact main-stream vector.
        int encoded=0;
        for(int shift=0;shift<35;shift+=7) {
            int b=Byte.toUnsignedInt(in.get());
            if(shift==28 && (b&0xF0)!=0)throw new IllegalArgumentException("ProcessType vector length overflow");
            encoded|=(b&0x7F)<<shift;
            if((b&0x80)==0){int n=(encoded>>>1)^-(encoded&1);skip(in,n);return;}
        }
        throw new IllegalArgumentException("ProcessType vector length overflow");
    }
    private static boolean hasPayload(int type) {
        return switch(type) {
            case 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,19,20,22,23,24,25,26,29,34,37,39,41,42,44,46,47,49,50,52 -> true;
            default -> false;
        };
    }
    private static boolean present(ByteBuffer in) { return in.get()!=0; }
    private static int count(ByteBuffer in,int width) {
        int n=in.getInt();if(n<0 || n>in.remaining()/width)throw new IllegalArgumentException("Truncated ProcessType list");return n;
    }
    private static void skip(ByteBuffer in,int bytes) {
        if(bytes<0 || bytes>in.remaining())throw new IllegalArgumentException("Truncated ProcessType payload");
        in.position(in.position()+bytes);
    }
    private static Position position(ByteBuffer in) { return new Position(in.getInt(),in.getInt()); }
    private static Rect rect(ByteBuffer in) { return new Rect(in.getInt(),in.getInt(),in.getInt(),in.getInt()); }
}
