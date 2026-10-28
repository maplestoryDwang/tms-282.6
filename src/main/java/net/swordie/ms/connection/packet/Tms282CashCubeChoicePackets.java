package net.swordie.ms.connection.packet;

import java.util.List;
import net.swordie.ms.client.character.tms282.Tms282CubeRuntime;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.packet.Tms282CashCubeChoiceRequest.Kind;
import net.swordie.ms.connection.packet.Tms282CubeChoicePackets.Candidate;
import net.swordie.ms.connection.packet.Tms282CubeChoicePackets.Memorial;
import net.swordie.ms.enums.InvType;

/** CASH restoration results1AE3410/36C52D0 and reconnect methods11/13.
 * The service supplies committed identities and options; this codec does not authorize or charge a use.
 */
public final class Tms282CashCubeChoicePackets {
    private Tms282CashCubeChoicePackets() { }

    public static Kind kind(Tms282CubeRuntime.Result result) {
        if(result==null || !result.choice())return null;
        for(var kind:Kind.values())if(kind.supportedItem(result.cubeId()) && kind.bonus()==result.bonus())return kind;
        return null;
    }

    public static byte[] presentation(Tms282CubeRuntime.Result result) {
        Kind kind=kind(result);
        if(kind==null || result.equip()==null || result.rolled()==null)
            throw new IllegalArgumentException("Unverified CASH choice presentation");
        var equip=result.equip();var rolled=result.rolled();
        int position=equip.getInvType()==InvType.EQUIPPED?-equip.getBagIndex():equip.getBagIndex();
        var candidate=new Candidate(rolled.getGrade()&255,rolled.getOptions().subList(0,6),0,rolled.getBonusGrade()&255);
        var memorial=new Memorial(0,equip.getId(),result.cubeId(),result.cubeSlot(),equip.getItemId(),position,List.of(candidate));
        // Actual cash consumers36A6BD0/36A6A80 read only resultCode and MemorialData.
        return typedUseReply(kind,0,memorial);
    }

    public static byte[] presentationFailure(Kind kind) {return typedUseReply(kind,1,null);}

    /** Base method1 reuses1AE3410's richer result; bonus method1 uses1AE3880's single-code record. */
    public static byte[] typedSelectReply(Kind kind,int code) {
        if(kind==null)throw new IllegalArgumentException("Missing CASH selection kind");
        return kind.bonus()?Tms282CubeChoicePackets.typedSelectReply(code):typedCashUseReply(code,0,null,0);
    }

    public static OutPacket restore(Tms282CubeRuntime.Result result) {
        Kind kind=kind(result);
        if(kind==null)throw new IllegalArgumentException("Unverified CASH choice reconnect family");
        return restore(kind,presentation(result));
    }

    public static byte[] typedUseReply(Kind kind,int code,Memorial memorial) {
        if(kind==null)throw new IllegalArgumentException("Missing CASH choice kind");
        return kind.bonus()?Tms282CubeChoicePackets.typedBonusUseReply(code,0,0,0,memorial)
                           :typedCashUseReply(code,0,memorial,0);
    }

    /** Native1AE3410 reads MemorialData before the final scalar, unlike consume36C4E00. */
    public static byte[] typedCashUseReply(int code,int field4,Memorial memorial,int field64) {
        OutPacket out=new OutPacket();out.encodeByte(0x0f);out.encodeInt(code);out.encodeInt(field4);
        out.encodeArr(Tms282CubeChoicePackets.memorial(memorial));out.encodeInt(field64);return out.getData();
    }

    private static OutPacket restore(Kind kind,byte[] typedPayload) {
        OutPacket out=Tms282LoginPacketSupport.packet(0x0244);
        out.encodeInt(0x3f7d24b3);out.encodeShort(kind.restoreMethod());out.encodeArr(typedPayload);return out;
    }
}
