package net.swordie.ms.client.character.tms282;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.connection.packet.Tms282RemoteBuffs;
import net.swordie.ms.connection.packet.Tms282UserHitRequest;
import net.swordie.ms.life.mob.MobStat;
import net.swordie.ms.loaders.overlay.DataOverlays;

/** CP0115's ordinary mob sender reports damage before indie52/67; fixed/percentage senders report final damage. */
public final class Tms282UserHitDamage {
    private record Rules(boolean body, Map<Integer,Boolean> ordinary) { }
    private static final Rules UNKNOWN=new Rules(false,Map.of());
    private static final Map<Integer,Rules> RULES=new ConcurrentHashMap<>();
    private static volatile int generation=-1;
    private Tms282UserHitDamage() { }

    public static int damage(Char chr,Tms282UserHitRequest hit,long now) {
        int raw=hit.hpDamage();
        if(!Tms282RemoteBuffs.active() || chr==null || chr.getField()==null || raw<=0 || hit.attackIdx() < -1
                || hit.zeroInt()<=0 || Byte.toUnsignedInt(hit.type())>4 || Byte.toUnsignedInt(hit.elemAttr())>1
                || hit.critical()!=0) return raw;
        // Existing TSM snapshot holds both groups consistently; aura refresh can otherwise mutate a live list.
        var stats=chr.getTemporaryStatManager().getCurrentStats();
        var reductions=stats.getOrDefault(CharacterTemporaryStat.IndieDamReduceR,List.of());
        var normalReductions=stats.getOrDefault(CharacterTemporaryStat.IndieNormalDamReduceR,List.of());
        if(reductions.isEmpty() && normalReductions.isEmpty()) return raw;
        byte[] rest=hit.rest();
        // Exact simple mob tail: template/OID/direction/guard/stance. Reflect variants and other senders stay unchanged.
        if(rest.length!=80 || rest[55]<0 || rest[55]>1 || rest[64]!=0 || rest[65]!=0
                || rest[75]!=0) return raw;
        var in=ByteBuffer.wrap(rest).order(ByteOrder.LITTLE_ENDIAN);
        if(in.getInt(56)!=0 || in.getInt(60)!=0 || in.getInt(76)!=0 || in.getInt(51)!=hit.zeroInt()) return raw;
        var mob=chr.getField().getMobByObjectId(hit.zeroInt());
        if(mob==null || mob.getObjectId()!=hit.zeroInt() || mob.getTemplateId()!=in.getInt(47) || mob.getHp()<=0) return raw;
        var fixed=mob.getTemporaryStat().getCurrentOptionsByMobStat(MobStat.FixDamRBuff);
        // MTS stores tOption in milliseconds even when its legacy isInMillis flag is false.
        if(fixed!=null && fixed.nOption>0 && now>=fixed.startTime
                && (fixed.tOption==0 || now-fixed.startTime<fixed.tOption)) return raw;
        Rules rules=rules(mob.getTemplateId());
        if(hit.attackIdx()==-1 ? !rules.body() : !rules.ordinary().getOrDefault((int)hit.attackIdx(),false)) return raw;
        // Main raw sender57A763F supplies0/0/-1:7C46D5..472B saves normal=1,4CC8 reloads it for53F2.
        // Native12888F0 aggregates each stat separately;53D5 then53FA multiply group52 then group67.
        double factor=indieFactor(reductions,now)*indieFactor(normalReductions,now);
        // Native7C6231..6261 truncates once after composing independent sources, then keeps positive damage >=1.
        double reduced=raw*factor;
        return factor==1?raw:Math.max(1,reduced>=0x1p31 || reduced < -0x1p31 ? Integer.MIN_VALUE : (int)reduced);
    }

    private static double indieFactor(List<Option> options,long now) {
        double factor=1;
        for(var option:options) {
            long duration=option.tOption>0?option.tOption:option.tTerm;
            if(!option.isInMillis()) duration*=1000L;
            if(now<option.startTime
                    || (duration>0 && now-option.startTime>=duration)) continue;
            factor*=(100D-option.nValue)/100D;
        }
        return factor;
    }

    private static Rules rules(int template) {
        int current=DataOverlays.generation();
        if(generation!=current) synchronized(RULES) {
            if(generation!=current){RULES.clear();generation=current;}
        }
        return RULES.computeIfAbsent(template,Tms282UserHitDamage::read);
    }

    private static Rules read(int template) {
        if(!"tms282.3".equals(DataOverlays.current().id())) return UNKNOWN;
        // The282 XML overlay is a changed/added delta; unchanged mobs retain the selected base .xml.
        var file=DataOverlays.current().wzLayers(String.format("Mob.wz/%07d.img.xml",template)).getLast();
        if(!file.isFile()) file=DataOverlays.current().wzLayers(String.format("Mob.wz/%07d.xml",template)).getLast();
        if(!file.isFile()) return UNKNOWN;
        try {
            var factory=DocumentBuilderFactory.newInstance();factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
            factory.setExpandEntityReferences(false);
            Element info=child(factory.newDocumentBuilder().parse(file).getDocumentElement(),"info");
            if(info==null || child(info,"link")!=null) return UNKNOWN;
            var attacks=child(info,"attack");Map<Integer,Boolean> ordinary=new HashMap<>();
            if(attacks!=null) for(Node n=attacks.getFirstChild();n!=null;n=n.getNextSibling()) {
                if(!(n instanceof Element e) || !e.getAttribute("name").matches("[0-9]+")) continue;
                ordinary.put(Integer.parseInt(e.getAttribute("name")),value(child(e,"fixDamR"))<=0 && value(child(e,"fixDamage"))==0);
            }
            return new Rules(value(child(info,"bodyAttack"))>0 && child(info,"bodyAttackInfo")==null,Map.copyOf(ordinary));
        } catch(Exception e) {
            System.err.println("[TMS282] Cannot classify mob hit metadata "+file+": "+e.getClass().getSimpleName());
            return UNKNOWN;
        }
    }
    private static Element child(Element parent,String name) {
        for(Node n=parent.getFirstChild();n!=null;n=n.getNextSibling()) if(n instanceof Element e && name.equals(e.getAttribute("name"))) return e;
        return null;
    }
    private static int value(Element e){return e==null?0:Integer.parseInt(e.getAttribute("value"));}
}
