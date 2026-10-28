package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import java.nio.charset.Charset;
import java.util.List;

/** Original 282 LP00DD application/list bodies; 132D410 is the shared guild-summary decoder. */
public final class Tms282GuildApplicationPackets {
    private Tms282GuildApplicationPackets() { }
    public record Summary(int guildId,int level,String name,String leader,int members,int averageLevel,
                          boolean applied,long time50,boolean flag41,String notice,int value58,int value5c,
                          int value60,boolean flag64) {
        public Summary { if(guildId<=0||level<1||level>255||members<1||members>65535||averageLevel<1||averageLevel>65535)
            throw new IllegalArgumentException("Invalid guild application summary"); }
    }
    private static OutPacket result(int type) {
        OutPacket out=Tms282LoginPacketSupport.packet(Tms282GuildPackets.OPCODE);out.encodeInt(type);return out;
    }
    /** Result144 clears and replaces ctx+198, then marks the list initialized (+1A8). */
    public static OutPacket applications(List<Summary> values,Charset charset) {
        if(values.size()>5||values.stream().map(Summary::guildId).distinct().count()!=values.size())
            throw new IllegalArgumentException("Invalid pending guild list");
        OutPacket out=result(144);out.encodeInt(values.size());for(Summary value:values)encode(out,value,charset);return out;
    }
    /** Result26, StringPool34AC: guild application submitted. */
    public static OutPacket submitted(Summary value,Charset charset) {
        OutPacket out=result(26);encode(out,value,charset);return out;
    }
    /** Result24 inserts the applicant in the receiving guild's map. The map key precedes the value ID. */
    public static OutPacket applicant(int guildId,Tms282GuildPackets.Applicant value,Charset charset) {
        if(guildId<=0||value.characterId()<=0)throw new IllegalArgumentException("Invalid guild applicant");
        OutPacket out=result(24);out.encodeInt(guildId);out.encodeInt(value.characterId());out.encodeInt(value.characterId());
        Tms282LoginPacketSupport.str(out,value.name(),charset);out.encodeInt(value.job());out.encodeInt(value.level());
        out.encodeByte(value.flag());Tms282LoginPacketSupport.str(out,value.message(),charset);return out;
    }
    /** Results23/25 remove a request without the voluntary-cancel popup; 32 includes the 60-second text. */
    public static OutPacket removed(int result,int characterId,int guildId) {
        if((result!=23&&result!=25&&result!=32)||characterId<=0||guildId<=0)throw new IllegalArgumentException("Invalid guild application removal");
        OutPacket out=result(result);out.encodeInt(characterId);out.encodeInt(guildId);return out;
    }
    public static void encode(OutPacket out,Summary value,Charset charset) {
        out.encodeInt(value.guildId());out.encodeByte(value.level());Tms282LoginPacketSupport.str(out,value.name(),charset);
        Tms282LoginPacketSupport.str(out,value.leader(),charset);out.encodeShort(value.members());out.encodeShort(value.averageLevel());
        out.encodeByte(value.applied());out.encodeLong(value.time50());out.encodeByte(value.flag41());
        Tms282LoginPacketSupport.str(out,value.notice(),charset);out.encodeInt(value.value58());out.encodeInt(value.value5c());
        out.encodeInt(value.value60());out.encodeByte(value.flag64());
    }
}
