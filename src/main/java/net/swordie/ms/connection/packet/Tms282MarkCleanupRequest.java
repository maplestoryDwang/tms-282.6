package net.swordie.ms.connection.packet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/** CP0466/5923B10: client removal of missing/expired marked targets. Only captured Wild Hunter families. */
public record Tms282MarkCleanupRequest(int skillId, List<Integer> targets) {
    public Tms282MarkCleanupRequest { targets = List.copyOf(targets); }
    public static Tms282MarkCleanupRequest parse(byte[] packet) {
        var in=Tms282FieldRequestReader.open(packet,0x0466,"CP0466");
        int skill=in.i32(),count=in.i32();
        int max=skill==33110010 ? 15 : skill==33001005 ? 1 : 0;
        if(max==0||count<1||count>max)
            throw new IllegalArgumentException("Unsupported mark cleanup skill/count");
        var ids=new ArrayList<Integer>();var unique=new HashSet<Integer>();
        for(int i=0;i<count;i++) {
            int id=in.i32();
            if(id<=0||!unique.add(id))throw new IllegalArgumentException("Invalid mark target");
            ids.add(id);
        }
        in.i32();in.end(); // 346A070 transport trailer
        return new Tms282MarkCleanupRequest(skill,ids);
    }
}
