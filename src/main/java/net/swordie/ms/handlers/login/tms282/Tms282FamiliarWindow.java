package net.swordie.ms.handlers.login.tms282;

import io.netty.util.AttributeKey;
import net.swordie.ms.client.Client;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.netty.NettyClient;
import net.swordie.ms.connection.packet.Tms282FamiliarPackets;
import net.swordie.ms.connection.packet.Tms282FamiliarRpcRequest;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;
import net.swordie.ms.console.config.ServerRuntimeConfig;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** Opt-in native window bootstrap. No familiar/economy backend is enabled by this class.
 * Server policy reserves 50000001..5FFFFFFF, keyed by character ID; this is not a client-reserved range.
 * A socket/character ledger records the binding after each actual SetField. UserCommon controllers
 * survive field transitions: reuse the same character ID across maps/reconnections, never destroy
 * a previous field's actor in a replacement field. Other actor features must use a different range.
 */
public final class Tms282FamiliarWindow {
    private static final boolean ENABLED=Boolean.parseBoolean(System.getProperty("tms282.familiar.window.enabled",
            ServerRuntimeConfig.stringValue("tms282.familiar.window.enabled")));
    private static final AttributeKey<Reservation> KEY=AttributeKey.valueOf("tms282-familiar-window");
    private record Reservation(int characterId,int fieldId,long generation,int actorId) { }
    /** The first move frames of each character are logged (live capture of FlushMovePath). */
    private static final java.util.Map<Integer,Integer> PAWN_MOVE_LOGS=new java.util.concurrent.ConcurrentHashMap<>();
    private Tms282FamiliarWindow() { }
    public static boolean enabled(){return ENABLED && Protocol.active().version()==ProtocolVersion.TMS_282_3;}

    /** Called only after a packet passed the outbound guard and was written to the socket queue. */
    public static void afterSent(NettyClient client,OutPacket packet) {
        if(!enabled() || packet.getBuiltFor()!=ProtocolVersion.TMS_282_3 || packet.getHeader()!=0x02EF
                || !(client instanceof Client c) || !c.getCh().isActive() || c.getCurrentState()!=Client.IN_FIELD
                || c.getChr()==null || c.getChr().getId()<=0 || c.getUser()==null || c.getUser().getId()<=0
                || !Tms282LoginState.of(c).isChannel())return;
        synchronized(c) {
            // Only the verified constructor-empty preview is available. Do not erase/misrepresent
            // legacy collections, and do not enter fee paths requiring existing link activation QR.
            var chr=c.getChr();
            if(chr.getFamiliars()!=null && !chr.getFamiliars().isEmpty())return;
            // QR68469 (link expiry / unlock / extend) is now handled by Tms282FamiliarGrowth; fee leaves are sent below
            var old=c.getCh().attr(KEY).get();
            int character=c.getChr().getId();
            if(character>0x0FFFFFFF){System.out.println("[TMS282] Character ID exceeds familiar actor reservation range");return;}
            int actor=0x50000000+character;
            var state=new Reservation(character,c.getChr().getFieldID(),old==null?1:old.generation()+1,actor);
            c.getCh().attr(KEY).set(state);
            boolean allowed=net.swordie.ms.client.character.tms282.Tms282FamiliarCombat.allowedIn(c.getChr().getFieldID());
            c.write(Tms282FamiliarPackets.bootstrap(character,Integer.toUnsignedLong(actor),c.getUser().getId(),allowed));
            int registered=0;
            try {
                // phase 1: the registered familiars (character_familiar282) into leaf21 of the new controller
                var familiars=net.swordie.ms.client.character.tms282.Tms282FamiliarRuntime.reload(chr);
                registered=familiars.registered().size();
                if(registered>0)c.write(Tms282FamiliarPackets.snapshot(character,Integer.toUnsignedLong(actor),familiars));
                // phase 2: the summoned familiar again in the new field's world
                if(allowed) {
                    for(var p:net.swordie.ms.client.character.tms282.Tms282FamiliarPawn.create(chr,familiars))c.write(p);
                    if(familiars.summonedSerial()!=0)net.swordie.ms.client.character.tms282.Tms282FamiliarCombat.showToOthers(chr,familiars);
                }
                net.swordie.ms.client.character.tms282.Tms282FamiliarCombat.showOthersTo(chr);
                long[] links=net.swordie.ms.client.character.tms282.Tms282FamiliarRuntime.links(chr);
                if(java.util.Arrays.stream(links).anyMatch(v->v!=0))c.write(Tms282FamiliarPackets.linkSlots(character,Integer.toUnsignedLong(actor),links));
            }catch(java.sql.SQLException | RuntimeException e){
                System.out.println("[TMS282] Familiar records not loaded char="+character+": "+e);
            }
            System.out.printf("[TMS282] Familiar window bootstrap char=%d actor=%08X field=%d generation=%d registered=%d%n",
                    character,actor,state.fieldId(),state.generation(),registered);
        }
    }

    /**
     * Phase 1 operations on this feature's owned actor: register (wire 86), rename (99), favorite (117); every
     * other method is refused with OnRequestFailed 14 (clears the UI's pending flag). Other actors are untouched.
     */
    static void handle(Tms282LoginState session,byte[] packet,java.nio.charset.Charset charset) {
        if(!enabled() || packet.length<12 || packet[0]!=(byte)0x7D || packet[1]!=2 || packet[2]!=9 || (packet[3]&0xFF)>1)return; // flag 0/1 (6E7AB8D: a client state bool; same layout)
        Client c=session.client();
        var owner=c.getCh().attr(KEY).get();
        var chr=session.fieldCharacter();
        if(owner==null || chr==null || c.getCurrentState()!=Client.IN_FIELD || owner.characterId()!=chr.getId())return;
        long actor=Integer.toUnsignedLong(owner.actorId());
        long pawnActor=net.swordie.ms.client.character.tms282.Tms282FamiliarPawn.pawnActorId(chr);
        // Frames are routed by their own actor: the controller's methods below, the summoned field familiar's (move /
        // attack / magic atom, PAWN-RPC-RESEARCH.md) to Tms282FamiliarCombat; other actors are left alone.
        java.util.List<Tms282FamiliarRpcRequest.Frame> frames=new java.util.ArrayList<>();
        ByteBuffer raw=ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);raw.position(4);
        while(raw.remaining()>=2) {
            int length=Short.toUnsignedInt(raw.getShort());
            if(length<6 || length>raw.remaining()){session.send(Tms282FamiliarPackets.failure(chr.getId(),actor,13));return;}
            byte[] frame=new byte[length];raw.get(frame);
            long target=Integer.toUnsignedLong(ByteBuffer.wrap(frame).order(ByteOrder.LITTLE_ENDIAN).getInt(0));
            int wire=Short.toUnsignedInt(ByteBuffer.wrap(frame).order(ByteOrder.LITTLE_ENDIAN).getShort(4));
            if(target==pawnActor) {
                String log=net.swordie.ms.client.character.tms282.Tms282FamiliarCombat.onPawnFrame(chr,wire,java.util.Arrays.copyOfRange(frame,6,length));
                if(wire!=84 || PAWN_MOVE_LOGS.merge(chr.getId(),1,Integer::sum)<=20)
                    System.out.printf("[TMS282] Familiar pawn wire %d char=%d len=%d: %s; body=%s%n",wire,chr.getId(),length-6,log,
                            java.util.HexFormat.of().formatHex(frame,6,Math.min(length,6+96)));
                continue;
            }
            if(target!=actor)continue;
            ByteBuffer single=ByteBuffer.allocate(6+length).order(ByteOrder.LITTLE_ENDIAN);
            single.put(packet,0,4).putShort((short)length).put(frame);
            try {
                frames.addAll(Tms282FamiliarRpcRequest.parse(single.array()).frames());
            }catch(IllegalArgumentException malformed){
                System.out.printf("[TMS282] Familiar method %d malformed char=%d: %s%n",wire,chr.getId(),malformed.getMessage());
                session.send(Tms282FamiliarPackets.failure(chr.getId(),actor,13));
                return;
            }
        }
        for(var frame:frames) {
            net.swordie.ms.client.character.tms282.Tms282FamiliarRuntime.Outcome outcome;
            try {
                int wire=frame.methodIndex();
                outcome=wire==101 && frame.arguments() instanceof Tms282FamiliarRpcRequest.ReleaseMany rm ? net.swordie.ms.client.character.tms282.Tms282FamiliarGrowth.disassemble(chr,rm.serialBits())
                        : wire==119 && frame.arguments() instanceof Tms282FamiliarRpcRequest.SlotIndex si ? net.swordie.ms.client.character.tms282.Tms282FamiliarGrowth.unlock(chr,si.index())
                        : wire==120 && frame.arguments() instanceof Tms282FamiliarRpcRequest.SlotIndex si ? net.swordie.ms.client.character.tms282.Tms282FamiliarGrowth.activate(chr,si.index())
                        : frame.arguments() instanceof Tms282FamiliarRpcRequest.Extend ex ? net.swordie.ms.client.character.tms282.Tms282FamiliarGrowth.extend(chr,ex.slot())
                        : frame.arguments() instanceof Tms282FamiliarRpcRequest.Link ln ? net.swordie.ms.client.character.tms282.Tms282FamiliarGrowth.link(chr,ln.slot(),ln.serialBits())
                        : frame.arguments() instanceof Tms282FamiliarRpcRequest.Upgrade up ? net.swordie.ms.client.character.tms282.Tms282FamiliarGrowth.upgrade(chr,up.targetBits(),up.materialBits(),up.essence())
                        : frame.arguments() instanceof Tms282FamiliarRpcRequest.Cube cu ? net.swordie.ms.client.character.tms282.Tms282FamiliarGrowth.cube(chr,cu.cashSlot(),cu.serialBits(),charset)
                        : frame.arguments() instanceof Tms282FamiliarRpcRequest.MemorialCube mc ? net.swordie.ms.client.character.tms282.Tms282FamiliarGrowth.memorialCube(chr,mc.serialBits(),mc.cashSlots(),charset)
                        : frame.arguments() instanceof Tms282FamiliarRpcRequest.MemorialSelect ms ? net.swordie.ms.client.character.tms282.Tms282FamiliarGrowth.memorialSelect(chr,ms.serialBits(),ms.choice())
                        : frame.methodIndex()==84 && frame.arguments() instanceof Tms282FamiliarRpcRequest.Serial sn
                        ? net.swordie.ms.client.character.tms282.Tms282FamiliarRuntime.summon(chr,sn.bits())
                        : frame.methodIndex()==85 ? net.swordie.ms.client.character.tms282.Tms282FamiliarRuntime.unsummon(chr)
                        : switch(frame.arguments()) {
                    case Tms282FamiliarRpcRequest.Register r -> net.swordie.ms.client.character.tms282.Tms282FamiliarRuntime.register(chr,r.consumeSlot(),charset);
                    case Tms282FamiliarRpcRequest.Rename r -> net.swordie.ms.client.character.tms282.Tms282FamiliarRuntime.rename(chr,r.serialBits(),r.nameBytes());
                    case Tms282FamiliarRpcRequest.Favorite f -> net.swordie.ms.client.character.tms282.Tms282FamiliarRuntime.favorite(chr,f.serialBits(),f.flag()!=0);
                    default -> null;
                };
            }catch(java.sql.SQLException | RuntimeException e){
                System.out.println("[TMS282] Familiar method "+frame.methodIndex()+" FAILED char="+chr.getId()+": "+e);
                session.send(Tms282FamiliarPackets.failure(chr.getId(),actor,13));
                return;
            }
            if(outcome==null){
                System.out.printf("[TMS282] Familiar method %d not enabled yet char=%d%n",frame.methodIndex(),chr.getId());
                session.send(Tms282FamiliarPackets.failure(chr.getId(),actor,14));
                chr.chatMessage("此萌獸功能尚未開放，本次沒有扣除道具或楓幣。");
                return;
            }
            if(!outcome.accepted()){
                System.out.printf("[TMS282] Familiar method %d REFUSED char=%d: %s%n",frame.methodIndex(),chr.getId(),outcome.log());
                if(outcome.packets().isEmpty())session.send(Tms282FamiliarPackets.failure(chr.getId(),actor,outcome.reason()));
                else for(var p:outcome.packets())session.send(p);
                return;
            }
            for(var p:outcome.packets())session.send(p);
            System.out.printf("[TMS282] Familiar method %d char=%d: %s%n",frame.methodIndex(),chr.getId(),outcome.log());
        }
    }

    /** Refuse only this feature's owned actor; other replicated actors/transports remain untouched. */
    static void refuseUnavailable(Tms282LoginState session,byte[] packet) {
        if(!enabled() || packet.length<12 || packet[0]!=(byte)0x7D || packet[1]!=2 || packet[2]!=9 || (packet[3]&0xFF)>1)return; // flag 0/1 (6E7AB8D: a client state bool; same layout)
        Client c=session.client();
        var owner=c.getCh().attr(KEY).get();
        var chr=session.fieldCharacter();
        if(owner==null || chr==null || c.getCurrentState()!=Client.IN_FIELD || owner.characterId()!=chr.getId()
                || owner.fieldId()!=chr.getFieldID())return;
        long firstActor=Integer.toUnsignedLong(ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).getInt(6));
        if(firstActor!=Integer.toUnsignedLong(owner.actorId()))return;
        int reason=14;
        try {
            var request=Tms282FamiliarRpcRequest.parse(packet);
            if(request.frames().stream().anyMatch(f->f.actorId()!=firstActor))return;
            System.out.printf("[TMS282] Familiar operation not enabled char=%d methods=%s%n",chr.getId(),
                    request.frames().stream().map(Tms282FamiliarRpcRequest.Frame::methodIndex).toList());
        }catch(IllegalArgumentException malformed){reason=13;}
        session.send(Tms282FamiliarPackets.failure(chr.getId(),firstActor,reason));
        chr.chatMessage("怪怪操作尚未開放，本次沒有扣除道具或楓幣。");
    }
}
