package net.swordie.ms.connection.packet;

import net.swordie.ms.client.character.tms282.*;
import net.swordie.ms.connection.OutPacket;
import java.io.ByteArrayOutputStream;
import java.nio.charset.*;
import java.util.*;

/** Original282 Housing actor layouts. IDs must come from the current private world's lease ledger. */
public final class Tms282MyHomeActors {
    public static final int OPCODE=0x0392, CONTROLLER=0x146936DE, FIELD_STATE=0xB732FF66;
    private static final int FURNITURE=0x16278DAB, CHAIR=0xFC0ACD9D;
    private Tms282MyHomeActors() { }
    private static void le(ByteArrayOutputStream out,long value,int bytes){for(int i=0;i<bytes;i++)out.write((int)(value >>>(8*i))&255);}
    private static void count(ByteArrayOutputStream out,int count){long n=(long)count*2;while(n>=128){out.write((int)(n&127)|128);n>>>=7;}out.write((int)n);}
    private static void scalar(ByteArrayOutputStream out,int leaf,long value,int width){le(out,leaf,2);le(out,value,width);}
    private static void text(ByteArrayOutputStream out,int leaf,String value){le(out,leaf,2);count(out,value.length());out.writeBytes(value.getBytes(StandardCharsets.UTF_16LE));}
    private static void start(ByteArrayOutputStream out,int id,int signature){if(id<=0)throw new IllegalArgumentException("Invalid home actor ID");le(out,id,4);le(out,signature,4);out.write(3);out.write(0);out.write(0);}
    private static void end(ByteArrayOutputStream out){le(out,0,2);le(out,0,2);out.write(0);}
    private static void owner(ByteArrayOutputStream out,int user,int character){
        if(user<=0 || character<=0)throw new IllegalArgumentException("Missing home actor owner");
        scalar(out,16,((long)character<<32)|Integer.toUnsignedLong(user),8);scalar(out,17,3,1);
    }
    /** 77CEF90: instance rows are full inventory records, NOT references to network actors.
     * The icon's item definition is looked up by the second item-ID field (77CF060 ->6654D30). */
    private static void furnitureList(ByteArrayOutputStream out,Tms282MyHomeState state){
        le(out,27,2);count(out,state.snapshot().furniture().size());int i=1;
        for(var row:new TreeMap<>(state.snapshot().furniture()).entrySet()){
            var p=state.placements().get(row.getKey());le(out,i++,2);le(out,row.getKey(),8);
            le(out,p==null?0:p.gridKey(),4);le(out,row.getValue(),4);
            le(out,p==null?0:p.x(),4);le(out,p==null?0:p.y(),4);le(out,0,4);
            out.write(p!=null && p.flipped()?1:0);count(out,0);le(out,row.getValue(),4);out.write(0);
        }
        le(out,0,2);
    }
    private static void inventory(ByteArrayOutputStream out,Set<Integer> items){le(out,29,2);count(out,items.size());int i=1;for(int item:new TreeSet<>(items))scalar(out,i++,item,4);le(out,0,2);}
    private static void validate(int controller,int fieldState,Map<Long,Integer> ids){
        var unique=new HashSet<Integer>();unique.add(controller);
        if(controller<=0 || fieldState<=0 || !unique.add(fieldState))throw new IllegalArgumentException("Duplicate home root actor IDs");
        for(var row:ids.entrySet())if(row.getKey()<=0 || row.getValue()<=0 || !unique.add(row.getValue()))throw new IllegalArgumentException("Duplicate furniture actor ID");
    }
    private static OutPacket finish(ByteArrayOutputStream body){le(body,0,4);if(body.size()>60000)throw new IllegalArgumentException("Home actor packet too large");var out=Tms282LoginPacketSupport.packet(OPCODE);out.encodeByte(0);out.encodeArr(body.toByteArray());return out;}
    private static void furniture(ByteArrayOutputStream body,Tms282MyHomeState state,int character,long serial,int actor,int controller,Tms282MyHomeRules rules){
        var placed=state.placements().get(serial);int item=state.snapshot().furniture().get(serial);
        start(body,actor,rules.furniture(item).chair()?CHAIR:FURNITURE);owner(body,state.accountId(),character);
        scalar(body,20,serial,8);scalar(body,21,0,4); // Native constructor's default animation state, not an ownership flag.
        scalar(body,22,placed.gridKey(),4);le(body,23,2);le(body,placed.x(),4);le(body,placed.y(),4);
        scalar(body,24,item,4);scalar(body,25,placed.flipped()?1:0,1);scalar(body,29,Integer.toUnsignedLong(controller),8);end(body);
    }
    public static OutPacket bootstrap(Tms282MyHomeState state,int character,int controller,int fieldState,Map<Long,Integer> ids,Tms282MyHomeRules rules){
        rules.validate(state);validate(controller,fieldState,ids);
        if(!ids.keySet().equals(state.placements().keySet()))throw new IllegalArgumentException("Home actor ledger does not match placements");
        var body=new ByteArrayOutputStream();
        start(body,fieldState,FIELD_STATE);owner(body,state.accountId(),character);text(body,27,state.snapshot().name());end(body);
        start(body,controller,CONTROLLER);owner(body,state.accountId(),character);scalar(body,20,state.snapshot().templateId(),4);text(body,21,state.snapshot().name());
        scalar(body,22,Integer.toUnsignedLong(fieldState),8);scalar(body,23,state.snapshot().presetSlots(),4);inventory(body,state.snapshot().unlockedInventory());end(body);
        for(var row:new TreeMap<>(ids).entrySet())furniture(body,state,character,row.getKey(),row.getValue(),controller,rules);
        start(body,controller,CONTROLLER);furnitureList(body,state);end(body);
        return finish(body);
    }
    public static OutPacket placements(Tms282MyHomeState before,Tms282MyHomeState after,int character,int controller,int fieldState,Map<Long,Integer> beforeIds,Map<Long,Integer> afterIds,Tms282MyHomeRules rules){
        after.requireOwnerMatches(before.accountId(),before.worldId());rules.validate(after);validate(controller,fieldState,afterIds);validate(controller,fieldState,beforeIds);
        beforeIds.forEach((serial,id)->{if(afterIds.containsKey(serial) && !afterIds.get(serial).equals(id))throw new IllegalArgumentException("Home actor ID changed within one world");});
        if(!afterIds.keySet().equals(after.placements().keySet()) || !beforeIds.keySet().equals(before.placements().keySet()))throw new IllegalArgumentException("Home actor ledger mismatch");
        var body=new ByteArrayOutputStream();
        for(var row:new TreeMap<>(afterIds).entrySet())if(!Objects.equals(before.placements().get(row.getKey()),after.placements().get(row.getKey())))furniture(body,after,character,row.getKey(),row.getValue(),controller,rules);
        start(body,controller,CONTROLLER);furnitureList(body,after);end(body);
        for(var row:beforeIds.entrySet())if(!afterIds.containsKey(row.getKey())){
            int item=before.snapshot().furniture().get(row.getKey());le(body,row.getValue(),4);le(body,rules.furniture(item).chair()?CHAIR:FURNITURE,4);body.write(4);body.write(0);
        }
        return finish(body);
    }
    public static OutPacket name(Tms282MyHomeState state,int controller,int fieldState){
        validate(controller,fieldState,Map.of());var body=new ByteArrayOutputStream();
        start(body,fieldState,FIELD_STATE);text(body,27,state.snapshot().name());end(body);
        start(body,controller,CONTROLLER);text(body,21,state.snapshot().name());end(body);return finish(body);
    }
    /** Original Chair SitSlots entries are(characterId, avatar override);-1 preserves default avatar state. */
    public static OutPacket chairOccupancy(int actor,int slotCount,int occupiedSlot,int characterId){
        if(slotCount<1 || slotCount>64 || occupiedSlot< -1 || occupiedSlot>=slotCount || characterId<0
                || (occupiedSlot==-1)!=(characterId==0))throw new IllegalArgumentException("Invalid chair occupancy");
        var body=new ByteArrayOutputStream();start(body,actor,CHAIR);le(body,30,2);count(body,slotCount);
        for(int i=0;i<slotCount;i++){le(body,i+1,2);le(body,i==occupiedSlot?characterId:0,4);le(body,-1,4);}
        le(body,0,2);end(body);return finish(body);
    }
    /** Send in the OLD scene, before SetField. Child actors leave before their controller/field-state parents. */
    public static OutPacket destroy(Tms282MyHomeState state,int controller,int fieldState,Map<Long,Integer> ids,Tms282MyHomeRules rules){
        validate(controller,fieldState,ids);var body=new ByteArrayOutputStream();
        for(var row:ids.entrySet()){
            Integer item=state.snapshot().furniture().get(row.getKey());if(item==null)throw new IllegalArgumentException("Cannot destroy an unowned furniture actor");
            destroy(body,row.getValue(),rules.furniture(item).chair()?CHAIR:FURNITURE);
        }
        destroy(body,controller,CONTROLLER);destroy(body,fieldState,FIELD_STATE);return finish(body);
    }
    private static void destroy(ByteArrayOutputStream body,int actor,int signature){le(body,actor,4);le(body,signature,4);body.write(4);body.write(0);}
}
