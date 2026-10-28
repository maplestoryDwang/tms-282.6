package net.swordie.ms.connection.packet;

import java.nio.charset.Charset;

/** CP024B / 5E56560: account name, target character, optional secondary password. */
public record Tms282ReloginRequest(String accountName, int characterId, String secondaryPassword) {
    public static final int OPCODE=0x024B;
    public static Tms282ReloginRequest parse(byte[] packet, Charset charset) {
        var in=Tms282FieldRequestReader.open(packet,OPCODE,"CP024B");
        String name=in.str(charset);
        int id=in.i32();
        String password=id==0 ? "" : in.str(charset);
        in.end();
        if(name.isEmpty() || name.length()>64 || id<0 || password.length()>64)
            throw new IllegalArgumentException("Invalid relogin request");
        return new Tms282ReloginRequest(name,id,password);
    }
    @Override public String toString() { return "CP024B relogin target="+characterId+" (credentials redacted)"; }
}
