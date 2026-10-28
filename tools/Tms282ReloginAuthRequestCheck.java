import net.swordie.ms.connection.packet.Tms282ReloginAuthRequest;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Pure parser regression; native writer fixture uses a synthetic cookie, never live credentials or DB. */
public class Tms282ReloginAuthRequestCheck {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static void reject(byte[] bytes,String why){
        try { Tms282ReloginAuthRequest.parse(bytes);throw new AssertionError(why); }
        catch(IllegalArgumentException expected){check(!expected.getMessage().contains("synthetic"),"errors omit credential");}
    }
    static byte[] fixture(int mode) {
        // Native F91180 writes one length-prefixed cookie, followed by F91320 exactly 16 raw bytes (not a string).
        ByteBuffer b=ByteBuffer.allocate(71).order(ByteOrder.LITTLE_ENDIAN);
        b.putShort((short)0x73).putInt(mode).put((byte)1).put((byte)0).putInt(0);
        b.putShort((short)32).put("synthetic_cookie_0123456789ABCDE".getBytes(StandardCharsets.US_ASCII));
        for(int value:new int[]{2026,9,5,25,2,30,40,123})b.putShort((short)value);
        b.putInt(0x12345678).put((byte)1).putInt(0x0100007f);
        check(b.position()==71,"native field widths total71 bytes");
        return b.array();
    }
    public static void main(String[] args)throws Exception {
        for(int mode:new int[]{3,4,9,10}) {
            byte[] raw=fixture(mode);var request=Tms282ReloginAuthRequest.parse(raw);
            check(request.mode()==mode && request.environment()==1,"native cookie modes");
            check(request.cookie().length()==32 && request.retry()==0 && request.retryCount()==0,"cookie/retry fields");
            check(request.tick()==0x12345678 && request.launcherMode()==1 && request.localAddress()==0x0100007f,"tail offsets");
            check(ByteBuffer.wrap(request.systemTime()).order(ByteOrder.LITTLE_ENDIAN).getShort()==2026,"SYSTEMTIME raw16");
            check(!request.toString().contains(request.cookie()) && request.toString().contains("redacted"),"no credential in logs");
            byte[] copy=request.systemTime();copy[0]=0;
            check(request.systemTime()[0]!=(byte)0,"timestamp defensive copy");
            if(args.length==1){Path dir=Path.of(args[0]);Files.createDirectories(dir);Files.write(dir.resolve("cp0073-mode"+mode+".bin"),raw);}
        }
        byte[] valid=fixture(3);
        for(int length=0;length<valid.length;length++)reject(Arrays.copyOf(valid,length),"truncation");
        reject(Arrays.copyOf(valid,valid.length+1),"trailing data");reject(null,"null");
        for(int mode:new int[]{-1,0,1,2,5,6,7,8,11,Integer.MAX_VALUE}) {
            byte[] b=valid.clone();ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).putInt(2,mode);reject(b,"unsupported native mode");
        }
        for(int len:new int[]{0,16,31,33,65535}) {
            byte[] b=valid.clone();ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).putShort(12,(short)len);reject(b,"wrong string length");
        }
        for(int value:new int[]{0,32,43,47,61,127,128,255}) {
            byte[] b=valid.clone();b[14]=(byte)value;reject(b,"non-base64url cookie");
        }
        for(int count:new int[]{-1,11,Integer.MAX_VALUE}) {
            byte[] b=valid.clone();ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).putInt(8,count);reject(b,"retry count");
        }
        byte[] b=valid.clone();b[7]=2;reject(b,"retry flag");
        b=valid.clone();b[0]=0x74;reject(b,"CP0074 is not cookie auth");
        b=valid.clone();b[7]=1;ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).putInt(8,10);
        check(Tms282ReloginAuthRequest.parse(b).retryCount()==10,"maximum documented native retries parse");
        if(args.length==1) accountInfoFixture(Path.of(args[0]));
        System.out.println("PASS "+checks+" CP0073 parser checks (synthetic data only; no DB/network)");
    }

    /** Optional production LP0015 builder fixture; reflection keeps the pure parser test dependency-free. */
    static void accountInfoFixture(Path dir)throws Exception {
        Class<?> userType=Class.forName("net.swordie.ms.client.User");
        Object user=userType.getConstructor().newInstance();
        userType.getMethod("setId",int.class).invoke(user,9);
        userType.getMethod("setName",String.class).invoke(user,"offline");
        Class<?> accountType=Class.forName("net.swordie.ms.enums.AccountType");
        Object player=Arrays.stream(accountType.getEnumConstants()).filter(v->v.toString().equals("Player")).findFirst().orElseThrow();
        userType.getMethod("setAccountType",accountType).invoke(user,player);
        Object packet=Class.forName("net.swordie.ms.connection.packet.Tms282AuthPackets")
                .getMethod("accountInfoSuccess",userType,java.nio.charset.Charset.class,java.util.function.Predicate.class)
                .invoke(null,user,StandardCharsets.UTF_8,(java.util.function.Predicate<Object>)job->false);
        byte[] bytes=(byte[])packet.getClass().getMethod("getData").invoke(packet);
        Files.write(dir.resolve("lp0015-success.bin"),bytes);
        check(bytes[0]==0x15 && bytes[1]==0 && bytes[2]==0 && bytes[bytes.length-1]==0,"production account info success/unlock fixture");
        Object handoff=Class.forName("net.swordie.ms.connection.packet.Tms282CharacterSwitchPackets")
                .getMethod("reloginHandoff",int.class,String.class,String.class)
                .invoke(null,35,"0123456789ABCDEF","synthetic_cookie_0123456789ABCDE");
        byte[] wire=(byte[])handoff.getClass().getMethod("getData").invoke(handoff);
        Files.write(dir.resolve("lp015f-auth-handoff.bin"),wire);
        check(wire.length==58 && ByteBuffer.wrap(wire).order(ByteOrder.LITTLE_ENDIAN).getShort(2)==32,
                "production relogin first string is32-byte auth cookie, separate from16-byte reselect token");
    }
}
