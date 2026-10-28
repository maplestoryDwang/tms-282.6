package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.client.character.tms282.Tms282CubeRuntime;
import net.swordie.ms.enums.InvType;
import java.util.List;

/** Native MemorialData (8F22E0/8F24B0) and LP0244 reconnect envelopes.
 * Widths are proved; neutral header names must not be confused with an authorization contract.
 * These builders do not enable a paid operation until the service supplies verified identities/selection semantics.
 */
public final class Tms282CubeChoicePackets {
    public record Candidate(int grade,List<Integer> options,int auxiliaryByte,int bonusGrade) {
        public Candidate {
            options=List.copyOf(options);
            if(grade<0 || grade>255 || options.size()!=6 || options.stream().anyMatch(i->i<0 || i>65535)
                    || auxiliaryByte<0 || auxiliaryByte>255 || bonusGrade<0 || bonusGrade>255)
                throw new IllegalArgumentException("Invalid native cube candidate");
        }
    }
    public record Memorial(long header0,long header8,int cubeId,int cubeSlot,int equipmentId,int equipmentSlot,
                           List<Candidate> candidates) {
        public Memorial {
            candidates=List.copyOf(candidates);
            if(header0<0 || header8<=0 || cubeId<=0 || cubeSlot<=0 || cubeSlot>Short.MAX_VALUE
                    || equipmentId/1_000_000!=1 || equipmentSlot==0 || equipmentSlot<=Short.MIN_VALUE || equipmentSlot>Short.MAX_VALUE
                    || candidates.isEmpty() || candidates.size()>255)
                throw new IllegalArgumentException("Invalid MemorialData identities or count");
        }
    }
    private Tms282CubeChoicePackets() { }
    public static Tms282CubeChoiceRequest.Kind kind(Tms282CubeRuntime.Result result) {
        if(result==null || !result.choice())return null;
        for(var kind:Tms282CubeChoiceRequest.Kind.values())if(kind.supportedItem(result.cubeId())&&kind.bonus()==result.bonus())return kind;
        return null;
    }
    /** Remaining/cube identity comes from the committed result; only the proposal's six potential lines are overlaid. */
    public static byte[] presentation(Tms282CubeRuntime.Result result) {
        var kind=kind(result);if(kind==null)throw new IllegalArgumentException("Unverified native choice presentation");
        var equip=result.equip();var rolled=result.rolled();
        int position=equip.getInvType()==InvType.EQUIPPED?-equip.getBagIndex():equip.getBagIndex();
        var candidate=new Candidate(rolled.getGrade()&255,rolled.getOptions().subList(0,6),0,0);
        var memorial=new Memorial(0,equip.getId(),result.cubeId(),result.cubeSlot(),equip.getItemId(),position,List.of(candidate));
        //36A6AF0/36A6B60 read only resultCode and MemorialData; initialize their unused scalar fields explicitly.
        return kind.bonus()?typedBonusUseReply(0,0,0,0,memorial):typedConsumeUseReply(0,0,0,memorial);
    }
    public static byte[] presentationFailure(Tms282CubeChoiceRequest.Kind kind) {
        return kind.bonus()?typedBonusUseReply(1,0,0,0,null):typedConsumeUseReply(1,0,0,null);
    }
    public static OutPacket restore(Tms282CubeRuntime.Result result) {
        var kind=kind(result);if(kind==null)throw new IllegalArgumentException("Unverified choice reconnect family");
        return restore(kind,presentation(result));
    }

    /** Native UserMemorialFreeCube::OnUse result / WvsContext method14, 36C4E00 and5E9BB71. */
    public static byte[] typedConsumeUseReply(int code,int field4,int field8,Memorial memorial) {
        OutPacket out=new OutPacket();out.encodeByte(0x0f);out.encodeInt(code);out.encodeInt(field4);out.encodeInt(field8);
        encodeMemorial(out,memorial);return out.getData();
    }
    /** Native bonus result / WvsContext method15, 5E9BCAC. */
    public static byte[] typedBonusUseReply(int code,int field4,long field8,int field16,Memorial memorial) {
        OutPacket out=new OutPacket();out.encodeByte(0x1f);out.encodeInt(code);out.encodeInt(field4);out.encodeLong(field8);out.encodeInt(field16);
        encodeMemorial(out,memorial);return out.getData();
    }
    /** OnSelectOption readers1AE3800/1AE3780: presence1 plus one i32 application result. */
    public static byte[] typedSelectReply(int code) {
        OutPacket out=new OutPacket();out.encodeByte(1);out.encodeInt(code);return out.getData();
    }
    /** Only the exact typed payload shared with the corresponding LP0245 callback is appended. */
    public static OutPacket restore(Tms282CubeChoiceRequest.Kind kind,byte[] typedPayload) {
        if(typedPayload==null || typedPayload.length==0 || (typedPayload[0]&255)!=(kind.bonus()?0x1f:0x0f))
            throw new IllegalArgumentException("Wrong choice reconnect payload");
        OutPacket out=Tms282LoginPacketSupport.packet(0x0244);
        out.encodeInt(0x3f7d24b3);out.encodeShort(kind.restoreMethod());out.encodeArr(typedPayload);return out;
    }
    public static byte[] memorial(Memorial value) {OutPacket out=new OutPacket();encodeMemorial(out,value);return out.getData();}
    private static void encodeMemorial(OutPacket out,Memorial value) {
        out.encodeByte(value!=null);
        if(value==null)return;
        out.encodeLong(value.header0());out.encodeInt(value.cubeId());out.encodeShort(value.cubeSlot());
        out.encodeLong(value.header8());out.encodeInt(value.equipmentId());out.encodeInt(value.equipmentSlot());
        out.encodeByte(value.candidates().size());
        for(Candidate c:value.candidates()) {
            out.encodeByte(c.grade());for(int p:c.options())out.encodeShort(p);
            out.encodeByte(c.auxiliaryByte());out.encodeInt(c.bonusGrade());
        }
    }
}
