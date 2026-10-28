package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import java.nio.charset.Charset;

/** Original282 GuildData skill updates and owned Noble-SP summary; no invented rankings. */
public final class Tms282GuildSkillPackets {
    public static final long EMPTY_TIME=94354848000000000L; // original constructor820C208/87F7EA0
    private Tms282GuildSkillPackets() { }
    /** LP00DD116: guild ID, skill ID, editor ID, SkillEntry(9B29F0). */
    public static OutPacket changed(int guildId,int skillId,int editorId,int level,String editor,Charset charset) {
        return changed(guildId,skillId,editorId,level,editor,editor,charset);
    }
    public static OutPacket changed(int guildId,int skillId,int editorId,int level,String buyer,String editor,Charset charset) {
        if(guildId<=0||editorId<=0||skillId<91001022||skillId>91001025||level<0||level>15)throw new IllegalArgumentException("Invalid native guild skill update");
        OutPacket out=Tms282LoginPacketSupport.packet(Tms282GuildPackets.OPCODE);out.encodeInt(116);out.encodeInt(guildId);out.encodeInt(skillId);out.encodeInt(editorId);
        out.encodeShort(level);out.encodeLong(EMPTY_TIME);Tms282LoginPacketSupport.str(out,buyer,charset);Tms282LoginPacketSupport.str(out,editor,charset);return out;
    }
    /** LP0162/type5: no ranking period/records, explicit SP total, no unimplemented source breakdown. */
    public static OutPacket points(int total) {
        if(total<0)throw new IllegalArgumentException("Invalid stored guild SP");
        OutPacket out=Tms282LoginPacketSupport.packet(0x0162);out.encodeByte(5);out.encodeLong(EMPTY_TIME);out.encodeLong(EMPTY_TIME);
        out.encodeInt(0);for(int i=0;i<4;i++)out.encodeByte(false);
        out.encodeByte(true);out.encodeInt(total);out.encodeInt(0);return out;
    }
}
