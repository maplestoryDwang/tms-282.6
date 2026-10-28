package net.swordie.ms.handlers.login.tms282;

import io.netty.util.AttributeKey;
import java.nio.charset.Charset;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedHashMap;
import net.swordie.ms.client.character.tms282.*;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.packet.*;
import net.swordie.ms.handlers.header.InHeader;

/** Active CP0780 cube interfaces. Server ownership, generation and durable payment stay in CubeRuntime. */
final class Tms282CubeHandlers {
    private static final AttributeKey<LinkedHashMap<Integer,Completed>> CACHE=AttributeKey.valueOf("tms282.cube.completed");
    private record Completed(Object request,OutPacket reply) { }
    private final Charset charset;
    Tms282CubeHandlers(Tms282LoginContext context) { charset=context.settings().nameCharset(); }
    void register(Tms282Routes routes) {
        routes.channel(0x0780,InHeader.EXTRA_SYSTEM_REQUEST).when(Tms282Guards.IN_FIELD)
                .handle("native cube RPC -> durable payment/choice + inventory/stat + correlated LP0245",this::roll);
    }
    private void roll(Tms282LoginState session,int opcode,byte[] packet)throws Exception {
        Object request;
        int token;
        try {
            if(packet==null || packet.length<8)throw new IllegalArgumentException("RPC header truncated");
            int key=ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).getInt(2);
            if(Tms282CashCubeChoiceRequest.handles(key)) {
                var choice=Tms282CashCubeChoiceRequest.parse(packet);request=choice;token=choice.callbackToken();
            } else if(key==Tms282CubeChoiceRequest.Kind.CONSUME.key() || key==Tms282CubeChoiceRequest.Kind.BONUS_CONSUME.key()) {
                var choice=Tms282CubeChoiceRequest.parse(packet);request=choice;token=choice.callbackToken();
            } else {
                var basic=Tms282CubeRequest.parse(packet);request=basic;token=basic.callbackToken();
            }
            if((token>>>24)==0)throw new IllegalArgumentException("RPC token has no generation");
        }catch(IllegalArgumentException malformed) {
            // CP0780 is also a transport for other interfaces. Unknown bodies are never passed to 265 handlers.
            System.out.println("[TMS282] CP0780 not executed: "+malformed.getMessage());
            session.send(Tms282StatPackets.unlock());return;
        }
        var attribute=session.client().getCh().attr(CACHE);
        var cache=attribute.get();
        if(cache==null){cache=new LinkedHashMap<>();attribute.set(cache);}
        var done=cache.get(token);
        if(done!=null) {
            session.send(done.request().equals(request)?done.reply():refused(request,token));
            return;
        }
        var chr=session.fieldCharacter();
        var inventory=Tms282InventoryWatch.of(chr);
        var stats=Tms282StatDiff.of(chr);
        Tms282CubeRuntime.Result result;
        try {
            if(request instanceof Tms282CashCubeChoiceRequest choice) {
                result=choice.selection()?Tms282CubeRuntime.choose(chr,choice):Tms282CubeRuntime.roll(chr,choice);
            } else if(request instanceof Tms282CubeChoiceRequest choice) {
                result=choice.selection()?Tms282CubeRuntime.choose(chr,choice):Tms282CubeRuntime.roll(chr,choice);
            } else result=Tms282CubeRuntime.roll(chr,(Tms282CubeRequest)request);
        }catch(IllegalArgumentException refusal) {
            var reply=refused(request,token);
            // Cancellation/expiry can remove the proposal while the native window is still open.
            // A recovery read failure must propagate; never disguise it as an ordinary refusal.
            if(selection(request) && Tms282CubeRuntime.pending(chr)==null)
                session.send(Tms282CubeRpcPackets.clearChoice());
            remember(cache,token,request,reply);
            chr.chatMessage(refusal.getMessage());session.send(reply);return;
        }
        // Persisted mutation is complete. Any serialization/network error now disconnects through the common
        // route wrapper; it must not be mislabeled as a pre-payment refusal.
        OutPacket reply=request instanceof Tms282CashCubeChoiceRequest cash
                ?Tms282CubeRpcPackets.callback(token,cash.selection()?Tms282CashCubeChoicePackets.typedSelectReply(cash.kind(),0)
                    :Tms282CashCubeChoicePackets.presentation(result))
                :request instanceof Tms282CubeChoiceRequest choice
                ?Tms282CubeRpcPackets.callback(token,choice.selection()?Tms282CubeChoicePackets.typedSelectReply(0)
                    :Tms282CubeChoicePackets.presentation(result))
                :Tms282CubeRpcPackets.result(token,0,0,result.equip().getId(),result.remaining());
        remember(cache,token,request,reply);
        var changes=inventory.changes();
        if(!changes.isEmpty())session.send(Tms282InventoryPackets.inventoryOperation(false,changes,charset));
        var statChanges=stats.changes();
        if(!statChanges.isEmpty())session.send(Tms282StatPackets.statChanged(false,statChanges));
        if(selection(request))
            session.send(Tms282CubeRpcPackets.clearChoice());
        session.send(reply);
    }
    private static OutPacket refused(Object request,int token) {
        if(request instanceof Tms282CashCubeChoiceRequest cash) {
            byte[] payload=cash.selection()?Tms282CashCubeChoicePackets.typedSelectReply(cash.kind(),1)
                    :Tms282CashCubeChoicePackets.presentationFailure(cash.kind());
            return Tms282CubeRpcPackets.callback(token,payload);
        }
        if(request instanceof Tms282CubeChoiceRequest choice) {
            byte[] payload=choice.selection()?Tms282CubeChoicePackets.typedSelectReply(1)
                    :Tms282CubeChoicePackets.presentationFailure(choice.kind());
            return Tms282CubeRpcPackets.callback(token,payload);
        }
        return Tms282CubeRpcPackets.refused(token);
    }
    static void restorePending(net.swordie.ms.client.character.Char chr) throws java.sql.SQLException {
        var pending=Tms282CubeRuntime.pending(chr);
        if(pending!=null && Tms282CashCubeChoicePackets.kind(pending)!=null)
            chr.write(Tms282CashCubeChoicePackets.restore(pending));
        else if(pending!=null && Tms282CubeChoicePackets.kind(pending)!=null)
            chr.write(Tms282CubeChoicePackets.restore(pending));
    }
    private static boolean selection(Object request) {
        return request instanceof Tms282CashCubeChoiceRequest cash && cash.selection()
                || request instanceof Tms282CubeChoiceRequest choice && choice.selection();
    }
    private static void remember(LinkedHashMap<Integer,Completed> cache,int token,Object request,OutPacket reply) {
        // Native tokens have an 8-bit generation. Keep a bounded recent window so genuine token reuse after
        // generation wrap is possible. Durable item/payment recovery is separately enforced by CubeJournal.
        if(cache.size()>=64)cache.remove(cache.keySet().iterator().next());
        cache.put(token,new Completed(request,reply));
        // Replays send only the correlated result, never stale inventory/meso deltas over newer client state.
    }
}
