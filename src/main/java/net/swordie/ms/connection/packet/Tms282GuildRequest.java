package net.swordie.ms.connection.packet;

import java.util.HexFormat;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;

/**
 * Native CP01F6/01F7 i32 selectors, evidenced by direct senders in research/tms2823/guild-trace.
 * The core operations below have strict field/tail validation. Unmapped selectors retain their raw payload
 * for investigation and are never passed to the incompatible 265 u8 GuildHandler.
 */
public record Tms282GuildRequest(int opcode, int type, byte[] rest) {
    public static final int GUILD_REQUEST = 0x01F6;
    public static final int GUILD_RESULT = 0x01F7;

    public Tms282GuildRequest {
        rest = rest.clone();
    }

    @Override
    public byte[] rest() {
        return rest.clone();
    }

    public record Action(int type,int targetId,String text) { }
    public String[] decline(Charset charset) {
        if(opcode!=GUILD_RESULT || type<48 || type>50)throw new IllegalArgumentException("Not an invitation decline");
        try {
            ByteBuffer in=ByteBuffer.wrap(rest).order(ByteOrder.LITTLE_ENDIAN);
            String[] names={string(in,charset,24),string(in,charset,24)};
            if(in.hasRemaining())throw new IllegalArgumentException("Trailing invite result");return names;
        } catch(java.nio.BufferUnderflowException e) { throw new IllegalArgumentException("Truncated invite result",e); }
    }
    public Action action(Charset charset) {
        if(opcode!=GUILD_REQUEST)throw new IllegalArgumentException("Not a guild action");
        ByteBuffer in=ByteBuffer.wrap(rest).order(ByteOrder.LITTLE_ENDIAN);
        int target=0;String text="";
        try {
            switch(type) {
                case 1,35 -> text=string(in,charset,24);
                case 2,34,42 -> { }
                case 3 -> { target=in.getInt();text=string(in,charset,1000);in.getInt();in.getInt();
                    if(Byte.toUnsignedInt(in.get())>1 || Byte.toUnsignedInt(in.get())>1)throw new IllegalArgumentException("Invalid application flags"); }
                case 8 -> { target=in.getInt();text=string(in,charset,24); }
                case 4,9,32,33 -> target=in.getInt();
                case 12 -> { target=in.getInt();int rank=Byte.toUnsignedInt(in.get());if(rank<2||rank>10)throw new IllegalArgumentException("Invalid guild rank");text=Integer.toString(rank); }
                case 40 -> { target=in.getInt();int amount=Byte.toUnsignedInt(in.get());if(amount<1||amount>15)throw new IllegalArgumentException("Invalid Noble SP quantity");text=Integer.toString(amount); }
                case 19 -> { return new Action(type,0,notice(charset)); }
                default -> throw new IllegalArgumentException("Guild action not ported: "+type);
            }
            if(in.hasRemaining() || isIdAction(type)&&target<=0)throw new IllegalArgumentException("Invalid guild action fields");
            return new Action(type,target,text);
        } catch(java.nio.BufferUnderflowException e) { throw new IllegalArgumentException("Truncated guild action",e); }
    }
    /** Native3356020: a byte count then that many IDs; CP7 explicitly caps a rejection list at100. */
    public java.util.List<Integer> applicationTargets() {
        if(opcode!=GUILD_REQUEST || type!=6&&type!=7)throw new IllegalArgumentException("Not an application review");
        ByteBuffer in=ByteBuffer.wrap(rest).order(ByteOrder.LITTLE_ENDIAN);
        try {
            int count=Byte.toUnsignedInt(in.get());
            if(count==0 || type==7&&count>100 || in.remaining()!=count*4)throw new IllegalArgumentException("Invalid application review length");
            var ids=new java.util.ArrayList<Integer>();
            for(int i=0;i<count;i++) { int id=in.getInt();if(id<=0||ids.contains(id))throw new IllegalArgumentException("Invalid applicant identity");ids.add(id); }
            return java.util.List.copyOf(ids);
        } catch(java.nio.BufferUnderflowException e) { throw new IllegalArgumentException("Truncated application review",e); }
    }
    private static boolean isIdAction(int type) { return type==3 || type==4 || type==8 || type==9 || type==12 || type==32 || type==33 || type==40; }
    private static String string(ByteBuffer in,Charset charset,int maximum) {
        int n=Short.toUnsignedInt(in.getShort());
        if(n>maximum || n>in.remaining())throw new IllegalArgumentException("Invalid guild string length");
        ByteBuffer bytes=in.slice(in.position(),n);in.position(in.position()+n);
        try {
            String value=charset.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(bytes).toString();
            if(value.indexOf('\0')>=0)throw new IllegalArgumentException("Invalid guild string");return value;
        } catch(CharacterCodingException e) { throw new IllegalArgumentException("Invalid guild string encoding",e); }
    }

    /** CP01F6/type19, sender37326A0: a single EncodeStr after the i32 selector. */
    public String notice(Charset charset) {
        if (opcode != GUILD_REQUEST || type != 19 || rest.length < 2) {
            throw new IllegalArgumentException("Not a guild notice request");
        }
        ByteBuffer buffer = ByteBuffer.wrap(rest).order(ByteOrder.LITTLE_ENDIAN);
        int length = Short.toUnsignedInt(buffer.getShort());
        if (length != buffer.remaining() || length > 1000) throw new IllegalArgumentException("Invalid guild notice length");
        try {
            String notice = charset.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(buffer).toString();
            if (notice.length() > 255 || notice.indexOf('\0') >= 0) throw new IllegalArgumentException("Invalid guild notice");
            return notice;
        } catch (CharacterCodingException badText) {
            throw new IllegalArgumentException("Invalid guild notice encoding", badText);
        }
    }

    /** {@code i32 type}, the rest raw. */
    public static Tms282GuildRequest parse(byte[] packet, int opcode) {
        Tms282FieldRequestReader in = Tms282FieldRequestReader.open(packet, opcode, String.format("CP%04X", opcode));
        return new Tms282GuildRequest(opcode, in.i32(), in.rest());
    }

    @Override
    public String toString() {
        return String.format("CP%04X %s type=%d rest=%s", opcode, opcode == GUILD_REQUEST ? "guild request" : "guild result",
                type, HexFormat.of().formatHex(rest));
    }
}
