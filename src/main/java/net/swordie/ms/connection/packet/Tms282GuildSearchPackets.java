package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import java.nio.*;
import java.nio.charset.*;
import java.util.List;

/** CP01F6/47 serializer13495A0 and LP0163 decoder5E4F960. Cursor contents are server-owned opaque data. */
public final class Tms282GuildSearchPackets {
    public static final int OPCODE=0x0163;
    private Tms282GuildSearchPackets() { }
    public record Cursor(int value20,int value24,long value28,boolean flag30,boolean flag31,int value34) { }
    public record Criteria(int mode,int destination,String text,boolean exact,Cursor cursor,boolean replace,Cursor next) { }
    public static Criteria parse(Tms282GuildRequest request,Charset charset) {
        if(request.opcode()!=Tms282GuildRequest.GUILD_REQUEST||request.type()!=47)throw new IllegalArgumentException("Not a native guild search");
        ByteBuffer in=ByteBuffer.wrap(request.rest()).order(ByteOrder.LITTLE_ENDIAN);
        try {
            int mode=Byte.toUnsignedInt(in.get()),destination=Byte.toUnsignedInt(in.get());
            int length=Short.toUnsignedInt(in.getShort());
            if(length>24||length>in.remaining())throw new IllegalArgumentException("Invalid guild search text length");
            ByteBuffer raw=in.slice(in.position(),length);in.position(in.position()+length);
            String text=charset.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(raw).toString();
            Criteria criteria=new Criteria(mode,destination,text,bool(in),cursor(in),bool(in),cursor(in));
            if(in.hasRemaining()||mode<1||mode>4||destination>2||text.indexOf('\0')>=0||text.codePointCount(0,text.length())>12
                    || criteria.next()!=null||mode==4&&(destination!=0||!text.isEmpty())||destination!=0&&(mode!=1||!criteria.exact())
                    || mode!=4&&text.codePointCount(0,text.length())<(destination==0?2:1))throw new IllegalArgumentException("Unsupported guild search criteria");
            return criteria;
        }catch(BufferUnderflowException|CharacterCodingException e){throw new IllegalArgumentException("Invalid native guild search",e);}
    }
    private static boolean bool(ByteBuffer in){int value=Byte.toUnsignedInt(in.get());if(value>1)throw new IllegalArgumentException("Invalid guild search boolean");return value!=0;}
    private static Cursor cursor(ByteBuffer in){return !bool(in)?null:new Cursor(Byte.toUnsignedInt(in.get()),in.getInt(),in.getLong(),bool(in),bool(in),in.getInt());}
    public static OutPacket result(Criteria criteria,List<Tms282GuildApplicationPackets.Summary> invited,
                                    List<Tms282GuildApplicationPackets.Summary> matches,Charset charset) {
        OutPacket out=Tms282LoginPacketSupport.packet(OPCODE);encodeCriteria(out,criteria,charset);
        out.encodeInt(invited.size());for(var guild:invited)Tms282GuildApplicationPackets.encode(out,guild,charset);
        out.encodeInt(matches.size());for(var guild:matches)Tms282GuildApplicationPackets.encode(out,guild,charset);
        return out;
    }
    public static void encodeCriteria(OutPacket out,Criteria criteria,Charset charset) {
        out.encodeByte(criteria.mode());out.encodeByte(criteria.destination());Tms282LoginPacketSupport.str(out,criteria.text(),charset);
        out.encodeByte(criteria.exact());encodeCursor(out,criteria.cursor());out.encodeByte(criteria.replace());encodeCursor(out,criteria.next());
    }
    private static void encodeCursor(OutPacket out,Cursor cursor) {
        out.encodeByte(cursor!=null);if(cursor==null)return;
        out.encodeByte(cursor.value20());out.encodeInt(cursor.value24());out.encodeLong(cursor.value28());
        out.encodeByte(cursor.flag30());out.encodeByte(cursor.flag31());out.encodeInt(cursor.value34());
    }
}
