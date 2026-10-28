package net.swordie.ms.client.character.tms282;

import net.swordie.ms.connection.packet.Tms282MyHomePackets.Snapshot;
import java.util.*;

/** Account/world authority, independent of a character's session and of client supplied owner IDs. */
public record Tms282MyHomeState(int accountId, int worldId, long revision, Snapshot snapshot, Map<Long,Placement> placements,Map<Integer,Integer> returnMaps) {
    public record Placement(int gridKey,int x,int y,boolean flipped) { }
    public Tms282MyHomeState(int accountId,int worldId,long revision,Snapshot snapshot) {
        this(accountId,worldId,revision,snapshot,Map.of(),Map.of());
    }
    public Tms282MyHomeState(int accountId,int worldId,long revision,Snapshot snapshot,Map<Long,Placement> placements) {
        this(accountId,worldId,revision,snapshot,placements,Map.of());
    }
    public Tms282MyHomeState {
        requireOwner(accountId,worldId);
        if (revision < 0 || snapshot == null || snapshot.accountId() != accountId)
            throw new IllegalArgumentException("Invalid home ownership/revision");
        placements=Collections.unmodifiableMap(new TreeMap<>(placements));
        returnMaps=Collections.unmodifiableMap(new TreeMap<>(returnMaps==null?Map.of():returnMaps));
        returnMaps.forEach((character,map)->{if(character<=0 || map<=0 || map>=999999999 || map>=871100000 && map<=871299999)throw new IllegalArgumentException("Invalid home return field");});
        if(!snapshot.furniture().keySet().containsAll(placements.keySet()) || placements.values().stream().anyMatch(Objects::isNull))
            throw new IllegalArgumentException("Placed furniture is not owned");
    }
    public static void requireOwner(int accountId,int worldId) {
        if(accountId<=0 || worldId<0 || worldId>255)throw new IllegalArgumentException("Invalid home owner");
    }
    public void requireOwnerMatches(int requesterAccount,int requesterWorld) {
        if(accountId!=requesterAccount || worldId!=requesterWorld)throw new SecurityException("Home belongs to another account/world");
    }
    /** Caller may offer naming only after the introductory quest has durably created the home. */
    public Tms282MyHomeState rename(int requesterAccount,int requesterWorld,String name) {
        requireOwnerMatches(requesterAccount,requesterWorld);
        if(name==null || name.isBlank())throw new IllegalArgumentException("Empty home name");
        return changed(new Snapshot(accountId,snapshot.templateId(),name,snapshot.comment(),snapshot.record(),
                snapshot.instanceSerial(),snapshot.furniture(),snapshot.unlockedInventory(),snapshot.furnitureSlots(),
                snapshot.visitors(),snapshot.likes(),snapshot.presetSlots(),snapshot.authority()));
    }
    /** Stable server serials make notification retries idempotent. Never accepts client-selected grants. */
    public Tms282MyHomeState registerFurniture(int requesterAccount,int requesterWorld,long serial,int itemId,Tms282MyHomeRules rules) {
        requireOwnerMatches(requesterAccount,requesterWorld);
        rules.requireFurniture(itemId);
        Integer existing=snapshot.furniture().get(serial);
        if(existing!=null){if(existing!=itemId)throw new IllegalArgumentException("Furniture serial reused");return this;}
        var furniture=new TreeMap<>(snapshot.furniture());furniture.put(serial,itemId);
        return changed(copyInventory(furniture,snapshot.unlockedInventory()));
    }
    public Tms282MyHomeState registerInventory(int requesterAccount,int requesterWorld,int itemId,Tms282MyHomeRules rules) {
        requireOwnerMatches(requesterAccount,requesterWorld);
        rules.requireExterior(itemId);
        if(snapshot.unlockedInventory().contains(itemId))return this;
        var inventory=new TreeSet<>(snapshot.unlockedInventory());inventory.add(itemId);
        return changed(copyInventory(snapshot.furniture(),inventory));
    }
    private Snapshot copyInventory(Map<Long,Integer> furniture,Set<Integer> inventory) {
        return new Snapshot(accountId,snapshot.templateId(),snapshot.name(),snapshot.comment(),snapshot.record(),
                snapshot.instanceSerial(),furniture,inventory,snapshot.furnitureSlots(),snapshot.visitors(),
                snapshot.likes(),snapshot.presetSlots(),snapshot.authority());
    }
    private Tms282MyHomeState changed(Snapshot next) {
        return new Tms282MyHomeState(accountId,worldId,Math.addExact(revision,1),next,placements,returnMaps);
    }
    public Tms282MyHomeState place(int account,int world,long serial,Placement position,Tms282MyHomeRules rules) {
        requireOwnerMatches(account,world);Integer item=snapshot.furniture().get(serial);
        if(item==null)throw new SecurityException("Furniture is not owned");
        rules.requirePlacement(snapshot.templateId(),item,position);
        if(position.equals(placements.get(serial)))return this;
        var next=new TreeMap<>(placements);next.put(serial,position);
        return new Tms282MyHomeState(accountId,worldId,Math.addExact(revision,1),snapshot,next,returnMaps);
    }
    public Tms282MyHomeState storeAll(int account,int world) {
        requireOwnerMatches(account,world);
        return placements.isEmpty()?this:new Tms282MyHomeState(accountId,worldId,Math.addExact(revision,1),snapshot,Map.of(),returnMaps);
    }
    /** Furniture actor MoveToInventory: preserves ownership and only removes this placement. */
    public Tms282MyHomeState storeFurniture(int account,int world,long serial){
        requireOwnerMatches(account,world);
        if(!snapshot.furniture().containsKey(serial))throw new SecurityException("Furniture is not owned");
        if(!placements.containsKey(serial))return this;
        var next=new TreeMap<>(placements);next.remove(serial);
        return new Tms282MyHomeState(accountId,worldId,Math.addExact(revision,1),snapshot,next,returnMaps);
    }
    public Tms282MyHomeState rememberReturn(int account,int world,int characterId,int mapId) {
        requireOwnerMatches(account,world);
        if(Objects.equals(returnMaps.get(characterId),mapId))return this;
        var maps=new TreeMap<>(returnMaps);maps.put(characterId,mapId);
        return new Tms282MyHomeState(accountId,worldId,Math.addExact(revision,1),snapshot,placements,maps);
    }
}
