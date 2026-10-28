package net.swordie.ms.client.character.tms282;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

/** Only original282 templates/items extracted from the installed client;273 shopping is not enabled. */
public final class Tms282MyHomeRules {
    private final Set<Integer> templates=new HashSet<>(),furniture=new HashSet<>(),exteriors=new HashSet<>();
    public record Grid(int key,int rows,int columns) { }
    public record Furniture(int width,int height,boolean chair,int seatCount) { }
    private final Map<Integer,Integer> maps=new HashMap<>();
    private final Map<Integer,Map<Integer,Grid>> grids=new HashMap<>();
    private final Map<Integer,Furniture> sizes=new HashMap<>();
    private final Set<Integer> registrable=new HashSet<>();
    private final Set<Integer> startWorlds=new HashSet<>();
    private final int minimumLevel;
    private final int instanceFieldType;
    private final JsonNode defaults,starterConstruction;
    public Tms282MyHomeRules(Path homeData,Path placementData) throws IOException {
        var mapper=new ObjectMapper();var home=mapper.readTree(homeData.toFile());
        if(!home.path("source").asText().equals("MapleStory/Data/Etc/Etc_002.wz/myHome.img"))throw new IOException("Not original282 housing data");
        defaults=home.path("data").path("Construction").path("DefaultFurnitureInitialization").path("001");
        starterConstruction=home.path("data").path("Construction").path("001");
        var rows=home.path("data").path("Construction").fields();
        while(rows.hasNext()) {
            var row=rows.next();if(!row.getKey().matches("[0-9]{3}"))continue;
            int id=Integer.parseInt(row.getKey());templates.add(id);maps.put(id,row.getValue().path("templateMapID").asInt());collectExterior(row.getValue());
        }
        var placement=mapper.readTree(placementData.toFile());var items=placement.path("furniture").fields();
        instanceFieldType=placement.path("instanceField").path("info").path("fieldType").asInt(-1);
        if(placement.path("instanceField").path("id").asInt()!=871200000 || instanceFieldType!=500)throw new IOException("Missing original282 housing instance field");
        while(items.hasNext()){
            var item=items.next();var v=item.getValue();int id=Integer.parseInt(item.getKey());
            if(v.path("gridX").asInt()>0 && v.path("gridY").asInt()>0){
                furniture.add(id);boolean chair=v.path("interact").asText().equals("chair");
                int seatCount=0;while(v.path("sitSlots").has(Integer.toString(seatCount)))seatCount++;
                sizes.put(id,new Furniture(v.path("gridX").asInt(),v.path("gridY").asInt(),chair,seatCount));
            }
            if((v.path("type").asText().equals("deco") && v.path("interact").asText().isEmpty())
                    || (v.path("type").asText().equals("rest") && v.path("interact").asText().equals("chair")))registrable.add(id);
        }
        for(var template:maps.entrySet()) {
            var byKey=new HashMap<Integer,Grid>();var list=placement.path("grids").path(String.valueOf(template.getValue())).fields();
            while(list.hasNext()){var e=list.next();int key=gridKey(e.getKey());var v=e.getValue();if(byKey.put(key,new Grid(key,v.path("row").asInt(),v.path("col").asInt()))!=null)throw new IOException("Duplicate home grid key");}
            grids.put(template.getKey(),Map.copyOf(byKey));
        }
        var quest=mapper.readTree(homeData.resolveSibling("quest-candidates.json").toFile()).path("500763").path("Check").path("0");
        minimumLevel=quest.path("lvmin").asInt(-1);quest.path("worldList").elements().forEachRemaining(v->startWorlds.add(v.asInt(-1)));
        if(minimumLevel!=200 || startWorlds.isEmpty() || defaults.size()!=2)throw new IOException("Incomplete original282 intro rules");
        if(templates.size()!=55 || furniture.isEmpty() || exteriors.isEmpty())throw new IOException("Incomplete282 housing data");
    }
    private void collectExterior(JsonNode node) {
        if(node.has("itemID")){int id=node.path("itemID").asInt();if(id>0)exteriors.add(id);}
        node.elements().forEachRemaining(value->{if(value.isContainerNode())collectExterior(value);});
    }
    public void requireFurniture(int itemId) { if(!furniture.contains(itemId))throw new IllegalArgumentException("Unsupported282 furniture"); }
    public void requireExterior(int itemId) { if(!exteriors.contains(itemId))throw new IllegalArgumentException("Unsupported282 exterior"); }
    /** Other original furniture types need their own native actors/interaction before economic use. */
    public void requireRegistrableFurniture(int itemId){requireFurniture(itemId);if(!registrable.contains(itemId))throw new IllegalArgumentException("This furniture interaction is not available yet");}
    public void validate(Tms282MyHomeState state) {
        if(!templates.contains(state.snapshot().templateId()))throw new IllegalArgumentException("Unsupported282 home template");
        state.snapshot().furniture().values().forEach(this::requireFurniture);
        state.snapshot().unlockedInventory().forEach(this::requireExterior);
        state.placements().forEach((serial,p)->requirePlacement(state.snapshot().templateId(),state.snapshot().furniture().get(serial),p));
    }
    public int mapId(int templateId) {Integer map=maps.get(templateId);if(map==null)throw new IllegalArgumentException("Unknown home template");return map;}
    public int instanceFieldType(){return instanceFieldType;}
    public Furniture furniture(int itemId){requireFurniture(itemId);return sizes.get(itemId);}
    public static int gridKey(String name) {
        try{return ByteBuffer.wrap(MessageDigest.getInstance("SHA-1").digest(name.getBytes(StandardCharsets.UTF_16LE))).order(ByteOrder.LITTLE_ENDIAN).getInt();}
        catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }
    public void requirePlacement(int templateId,int itemId,Tms282MyHomeState.Placement p) {
        requireFurniture(itemId);var grid=grids.getOrDefault(templateId,Map.of()).get(p.gridKey());var item=sizes.get(itemId);
        // Grid anchors use the bottom-left cell, as in original282 starter furniture coordinates.
        if(grid==null || p.x()<0 || p.y()<item.height()-1 || (long)p.x()+item.width()>grid.columns() || p.y()>=grid.rows())
            throw new IllegalArgumentException("Furniture lies outside the original282 grid");
    }
    /** One-time entitlement only; does not complete/reset any quest or issue the later intro rewards. */
    public Tms282MyHomeState starter(int account,int world,int level,String playerName) {
        Tms282MyHomeState.requireOwner(account,world);
        if(level<minimumLevel || !startWorlds.contains(world))throw new IllegalArgumentException("Original282 home introduction requires an eligible level/world");
        long instance=((world+1L)<<32)|account;var owned=new TreeMap<Long,Integer>();var placed=new TreeMap<Long,Tms282MyHomeState.Placement>();
        int index=1;for(var row:defaults){long serial=(instance<<16)|index++;int item=row.path("itemID").asInt();owned.put(serial,item);
            String[] pos=row.path("Pos").asText().split(",");placed.put(serial,new Tms282MyHomeState.Placement(gridKey(row.path("GridName").asText()),Integer.parseInt(pos[0].trim()),Integer.parseInt(pos[1].trim()),row.path("Flip").asInt()!=0));}
        var inventory=new TreeSet<Integer>();starterConstruction.elements().forEachRemaining(slot->{if(slot.isObject())slot.elements().forEachRemaining(choice->{if(choice.has("itemID"))inventory.add(choice.path("itemID").asInt());});});
        String name=playerName==null||playerName.isBlank()?"我的小屋":playerName;
        if(name.length()>12){name=name.substring(0,12);if(Character.isHighSurrogate(name.charAt(name.length()-1)))name=name.substring(0,name.length()-1);}
        name+="的小屋";
        // Original status constructor77E4680 sets capacity72 and zero AuthorityFlag.
        // Blueprint defaultSlot3 comes from the original WZ; no unproven permission bits.
        var snapshot=new net.swordie.ms.connection.packet.Tms282MyHomePackets.Snapshot(account,1,name,"","",instance,owned,inventory,72,0,0,3,0);
        var state=new Tms282MyHomeState(account,world,0,snapshot,placed);validate(state);return state;
    }
}
