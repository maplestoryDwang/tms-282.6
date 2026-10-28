package net.swordie.ms.client.character.tms282;

import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.packet.*;
import net.swordie.ms.connection.protocol.*;
import net.swordie.ms.loaders.FieldData;
import net.swordie.ms.world.field.Field;
import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Opt-in native starter home, private scenes and journaled furniture registration. */
public final class Tms282MyHomeRuntime {
    public static final int INSTANCE_MAP=871200000;
    private static final AtomicInteger ACTORS=new AtomicInteger(0x60000000), FIELD_SERIALS=new AtomicInteger(-1);
    private static volatile Tms282MyHomeRuntime active;
    private final Tms282MyHomeStore store;
    private final Tms282MyHomeRules rules;
    private final Tms282MyHomeJournal journal;
    // ponytail: a small identity map; each private scene belongs to one authenticated session.
    private final Map<Char,Lease> leases=Collections.synchronizedMap(new IdentityHashMap<>());
    private record RegistrationStamp(Client client,long nanos) { }
    private final Map<Char,RegistrationStamp> registrations=Collections.synchronizedMap(new IdentityHashMap<>());
    private static final class Lease {
        final Client client;final int account,world,channel,controller,fieldState;final Field field,returnField;
        final int templateMap;Tms282MyHomeState state;Map<Long,Integer> actors;
        Lease(Char chr,Tms282MyHomeState state,Field field,Field returnField,int templateMap){
            client=chr.getClient();account=state.accountId();world=state.worldId();channel=client.getChannel();
            this.state=state;this.field=field;this.returnField=returnField;this.templateMap=templateMap;
            controller=nextActor();fieldState=nextActor();actors=actorIds(state,Map.of());
        }
    }
    private Tms282MyHomeRuntime(Tms282MyHomeStore store,Tms282MyHomeRules rules){this.store=store;this.rules=rules;this.journal=new Tms282MyHomeJournal(store,rules);}
    public static boolean enabled(){return flag("tms282.myhome.enabled") && Protocol.active().version()==ProtocolVersion.TMS_282_3;}
    public static boolean registrationEnabled(){return enabled() && flag("tms282.myhome.registration.enabled");}
    /** A -D JVM property wins; otherwise config/server.properties (as tms282.cube.enabled). */
    private static boolean flag(String key){
        String jvm=System.getProperty(key);
        return Boolean.parseBoolean(jvm!=null?jvm:net.swordie.ms.console.config.ServerRuntimeConfig.stringValue(key));
    }
    /** Startup/check injection only. Never installs SQL, starts a server, or reads user rows. */
    public static synchronized void initialize(DataSource source,Path dataDirectory)throws IOException {
        if(active!=null && !active.leases.isEmpty())throw new IllegalStateException("Cannot replace an active home runtime");
        var rules=new Tms282MyHomeRules(dataDirectory.resolve("myhome-data.json"),dataDirectory.resolve("placement-data.json"));
        active=new Tms282MyHomeRuntime(source==null?new Tms282MyHomeStore(rules):new Tms282MyHomeStore(source,rules),rules);
    }
    private static synchronized Tms282MyHomeRuntime runtime()throws IOException {
        if(!enabled())throw new IllegalStateException("MyHome is not enabled");
        if(active==null)initialize(null,Path.of(System.getProperty("tms282.myhome.dataDir","data/protocol/tms282.3/myhome")));
        return active;
    }
    private static int[] identity(Char chr,boolean inField){
        if(chr==null || chr.getClient()==null || chr.getClient().getChr()!=chr || chr.getClient().getUser()==null || chr.getAccount()==null)
            throw new SecurityException("Missing authenticated home owner");
        var c=chr.getClient();int user=c.getUser().getId(),world=Byte.toUnsignedInt(c.getWorldId());
        if(user<=0 || chr.getAccount().getOwnerUserId()!=user || chr.getAccount().getWorldId()!=world || inField && c.getCurrentState()!=Client.IN_FIELD)
            throw new SecurityException("Mismatched home owner/session/world");
        return new int[]{user,world};
    }
    private static int nextActor(){return ACTORS.getAndUpdate(v->{if(v>=0x6fffffff)throw new IllegalStateException("Home actor ID range exhausted");return v+1;});}
    private static Map<Long,Integer> actorIds(Tms282MyHomeState state,Map<Long,Integer> existing){
        var result=new TreeMap<Long,Integer>();for(long serial:state.placements().keySet())result.put(serial,existing.containsKey(serial)?existing.get(serial):nextActor());return Map.copyOf(result);
    }
    private static boolean valid(Char chr,Lease lease){
        if(lease==null || chr.getClient()!=lease.client || chr.getField()!=lease.field || lease.client.getChannel()!=lease.channel)return false;
        try{var id=identity(chr,true);return id[0]==lease.account && id[1]==lease.world;}catch(SecurityException e){return false;}
    }
    /** Exact StageEncoder hook. A shared/public field, a stale session or a different Field object gets no override. */
    public static int templateMapId(Char chr,Field field){
        var engine=active;if(engine==null || chr==null)return 0;
        synchronized(chr){var lease=engine.leases.get(chr);
            if(valid(chr,lease) && field==lease.field)return lease.templateMap;
            if(lease!=null && chr.getField()!=lease.field)engine.leases.remove(chr,lease);
            return 0;}
    }
    public static boolean acceptsActor(Char chr,long actorId){
        var engine=active;if(engine==null || chr==null || !enabled())return false;
        synchronized(chr){var lease=engine.leases.get(chr);
            return valid(chr,lease) && (actorId==lease.controller || actorId==lease.fieldState || lease.actors.values().stream().anyMatch(id->actorId==id));}
    }
    public static boolean openOrCreate(Char chr){
        if(!enabled()){if(chr!=null)chr.chatMessage("我的小屋尚未開放。");return false;}
        try{return runtime().open(chr);}catch(IOException|SQLException|RuntimeException e){failure(chr,"目前無法進入我的小屋，請稍後再試。",e);return false;}
    }
    private boolean open(Char chr)throws SQLException {
        if(chr==null)throw new SecurityException("Missing home character");
        synchronized(Tms282VMatrixRuntime.ECONOMY_MONITOR){synchronized(chr){
            var id=identity(chr,true);var sourceClient=chr.getClient();
            var previous=leases.get(chr);
            if(valid(chr,previous)){synchronizeScene(chr,previous,store.load(id[0],id[1]).orElseThrow());return true;}
            if(previous!=null)leases.remove(chr,previous);
            if(chr.getInstance()!=null || chr.getField()==null || !chr.getField().isChannelField())throw new IllegalArgumentException("Cannot leave a private/event instance for a home");
            if(chr.getField().getId()>=871100000 && chr.getField().getId()<871300000)throw new IllegalArgumentException("A saved private field must return to a public field first");
            var existing=store.load(id[0],id[1]);
            var state=existing.orElseGet(()->rules.starter(id[0],id[1],chr.getLevel(),chr.getName()));
            int template=rules.mapId(state.snapshot().templateId());Field original=FieldData.getFieldById(template);
            if(original==null || !original.getMobGens().isEmpty())throw new IllegalStateException("Home template is not a peaceful housing scene");
            Field room=FieldData.getFieldCopyById(template,false,false);
            if(original==null || room==null || room==original || room.getPortals().isEmpty())throw new IllegalStateException("Home template cannot be privately copied");
            var housingType=net.swordie.ms.enums.FieldType.getByVal(rules.instanceFieldType());
            if(housingType==null)throw new IllegalStateException("Native housing field type is not installed");
            room.setFieldType(housingType);
            room.setId(INSTANCE_MAP);room.setSN(FIELD_SERIALS.getAndUpdate(v->{if(v==Integer.MIN_VALUE)throw new IllegalStateException("Home field serial range exhausted");return v-1;}));
            room.setChannelField(false);room.setChannel(chr.getClient().getChannel());room.setRuneStone(null);
            if(original.getId()!=template)throw new IllegalStateException("Home template cache was mutated");
            if(existing.isEmpty()){
                try{store.create(state);}catch(SQLException uncertain){if(store.load(id[0],id[1]).isEmpty())throw uncertain;}
                state=store.load(id[0],id[1]).orElseThrow();if(state.snapshot().templateId()!=1)throw new SQLException("Concurrent home construction changed the template");
            }
            int returnMap=chr.getField().getId();
            var remembered=state.rememberReturn(id[0],id[1],chr.getId(),returnMap);
            if(remembered!=state)state=persist(state,remembered);
            var lease=new Lease(chr,state,room,chr.getField(),template);
            // Build all feature packets before touching the player's current scene.
            var status=Tms282MyHomePackets.status(state.snapshot());var bootstrap=Tms282MyHomeActors.bootstrap(state,chr.getId(),lease.controller,lease.fieldState,lease.actors,rules);
            if(chr.getClient()!=sourceClient || !Arrays.equals(identity(chr,true),id))throw new SecurityException("Home session changed during entry");
            leases.put(chr,lease);
            try{lease.client.write(status);chr.warp(room);if(chr.getField()!=room)throw new IllegalStateException("Private home warp was refused");lease.client.write(bootstrap);return true;}
            catch(RuntimeException e){leases.remove(chr,lease);if(chr.getField()==room)chr.warp(lease.returnField);throw e;}
        }}
    }
    public static boolean handleGameUser(Char chr,byte[] payload){
        if(payload==null || payload.length<6)return false;
        if((payload[0]&255)!=0x3a || (payload[1]&255)!=0x38 || (payload[2]&255)!=0x1a || (payload[3]&255)!=0x0a)return false;
        int method=(payload[4]&255)|((payload[5]&255)<<8);if(method!=8 && method!=12)return false;
        try{
            var request=Tms282MyHomeRequest.gameUser(payload);var id=identity(chr,true);
            if(request instanceof Tms282MyHomeRequest.Enter enter){
                if(enter.accountId()!=id[0]){chr.chatMessage("目前只開放進入自己的小屋。");return true;}
                openOrCreate(chr);
            }else if(request instanceof Tms282MyHomeRequest.Consume consume)registerFurniture(chr,consume.itemId());
        }catch(IllegalArgumentException|SecurityException e){failure(chr,"小屋操作無效，請重新操作。",e);}
        return true;
    }
    /** Hook before inventory loading, including a session returning after an ambiguous Aria write. */
    public static void beforeLoad(Char chr)throws SQLException {
        if(!enabled())return;
        synchronized(Tms282VMatrixRuntime.ECONOMY_MONITOR){
            try{runtime().journal.recoverForLoad(chr.getId());}catch(IOException e){throw new SQLException(e);}
        }
    }
    public static boolean registerFurniture(Char chr,int itemId) {
        if(chr==null)return false;
        if(!registrationEnabled()){chr.chatMessage("家具道具登錄尚未開放。");return false;}boolean committed=false;
        synchronized(Tms282VMatrixRuntime.ECONOMY_MONITOR){synchronized(chr){
            try {
                var id=identity(chr,true);var client=chr.getClient();var engine=runtime();
                long now=System.nanoTime();var last=engine.registrations.get(chr);
                // Native item-use caller362EC42 invokes5E04C30 with the same500ms interval.
                if(last!=null && last.client()==client && now-last.nanos()<500_000_000L){chr.chatMessage("請稍候再登錄下一件家具。");return false;}
                Tms282VMatrixRuntime.requireEconomicAccess(chr);
                if(Tms282MyHomeJournal.isBlocked(chr.getId()) || chr.getTradeRoom()!=null || chr.getMiniRoom()!=null)
                    throw new IllegalStateException("Inventory is not available for housing registration");
                engine.rules.requireRegistrableFurniture(itemId);
                if(net.swordie.ms.loaders.ItemData.getItemInfoByID(itemId)==null)throw new IllegalArgumentException("Furniture item template is missing");
                var before=engine.store.load(id[0],id[1]).orElseThrow(()->new IllegalArgumentException("Enter your own home first"));
                var item=chr.getConsumeInventory().getItems().stream()
                        .filter(i->i.getItemId()==itemId && i.getQuantity()>0 && i.getBagIndex()>0 && i.getDateExpire()!=null && !i.getDateExpire().isExpired())
                        .min(Comparator.comparingInt(net.swordie.ms.client.character.items.Item::getBagIndex))
                        .orElseThrow(()->new IllegalArgumentException("Owned furniture item was not found"));
                chr.saveToSQL(); // Journal independently verifies the exact persisted slot/owner/quantity.
                if(client!=chr.getClient() || !Arrays.equals(id,identity(chr,true)))throw new SecurityException("Home session changed");
                engine.registrations.put(chr,new RegistrationStamp(client,now));
                var intent=engine.journal.execute(chr,item,before);committed=true;
                int remaining=Integer.parseInt(intent.itemBefore().get("quantity"))-1;
                Tms282InventoryPackets.Operation update=remaining==0
                        ?new Tms282InventoryPackets.Remove(net.swordie.ms.enums.InvType.CONSUME,(short)item.getBagIndex())
                        :new Tms282InventoryPackets.UpdateQuantity(net.swordie.ms.enums.InvType.CONSUME,(short)item.getBagIndex(),remaining);
                client.write(Tms282InventoryPackets.inventoryOperation(true,List.of(update),net.swordie.ms.handlers.login.tms282.Tms282LoginSettings.load().nameCharset()));
                // Native LP22 appends to private inventory, so initialize its before-image first.
                client.write(Tms282MyHomePackets.status(before.snapshot()));
                client.write(Tms282MyHomePackets.furnitureRegistered(intent.serial(),itemId));
                var lease=engine.leases.get(chr);
                if(valid(chr,lease))engine.synchronizeScene(chr,lease,intent.homeAfter());
                else client.write(Tms282MyHomePackets.status(intent.homeAfter().snapshot()));
                return true;
            }catch(SQLException|IOException|RuntimeException e){
                failure(chr,committed?"家具已登錄，但畫面尚未同步，請重新登入。":"家具登錄未完成或正在等待復原，請重新登入確認；不支援的家具不會扣除。",e);
                return false;
            }
        }}
    }
    /** Ownership classification precedes typed decoding. A furniture actor cannot impersonate the controller. */
    public static boolean handleFrame(Char chr,int opcode,Tms282ActorRpcRequest.Frame frame){
        if(chr==null || opcode!=0x06D2)return false;
        synchronized(chr){
            if(!acceptsActor(chr,frame.actorId()))return false;
            var engine=active;var lease=engine.leases.get(chr);boolean persisted=false;
            if(!valid(chr,lease))return true;
            try{
                var before=engine.store.load(lease.account,lease.world).orElseThrow();Tms282MyHomeState after;
                if(frame.actorId()!=lease.controller){
                    var serial=lease.actors.entrySet().stream().filter(e->Integer.toUnsignedLong(e.getValue())==frame.actorId()).map(Map.Entry::getKey).findFirst();
                    if(serial.isEmpty())return true; // FieldState has no enabled inbound method.
                    if(frame.methodIndex()!=84 || frame.arguments().length!=0)throw new IllegalArgumentException("Unsupported furniture actor method");
                    after=before.storeFurniture(lease.account,lease.world,serial.get());
                }else{
                    var action=Tms282MyHomeRequest.controller(frame.methodIndex(),frame.arguments());
                    if(action instanceof Tms282MyHomeRequest.Install install)after=before.place(lease.account,lease.world,install.serial(),new Tms282MyHomeState.Placement(install.gridKey(),install.x(),install.y(),install.flipped()),engine.rules);
                    else if(action instanceof Tms282MyHomeRequest.StoreAll)after=before.storeAll(lease.account,lease.world);
                    else {chr.chatMessage("家具回收尚未開放；可以使用收回全部家具。");return true;}
                }
                if(after!=before)after=engine.persist(before,after);
                persisted=true;
                engine.synchronizeScene(chr,lease,after);
            }catch(SQLException|RuntimeException e){failure(chr,persisted?"家具配置已儲存，但畫面尚未同步，請重新進入小屋。":"目前無法確認配置的完成狀態，請重新進入小屋。",e);}
            return true;
        }
    }
    private Tms282MyHomeState persist(Tms282MyHomeState before,Tms282MyHomeState after)throws SQLException {
        try{if(!store.save(before,after))throw new SQLException("Stale home revision");return after;}
        catch(SQLException uncertain){
            try{var actual=store.load(after.accountId(),after.worldId());if(actual.isPresent() && actual.get().equals(after))return actual.get();}
            catch(SQLException readFailure){uncertain.addSuppressed(readFailure);}
            throw uncertain;
        }
    }
    private void synchronizeScene(Char chr,Lease lease,Tms282MyHomeState state){
        if(!valid(chr,lease))throw new SecurityException("Home scene/session changed before display synchronization");
        state.requireOwnerMatches(lease.account,lease.world);
        if(state.snapshot().templateId()!=lease.state.snapshot().templateId())throw new IllegalStateException("Reenter home after a template change");
        var ids=actorIds(state,lease.actors);var update=Tms282MyHomeActors.placements(lease.state,state,chr.getId(),lease.controller,lease.fieldState,lease.actors,ids,rules);
        lease.client.write(Tms282MyHomePackets.status(state.snapshot()));lease.client.write(update);
        if(!state.snapshot().name().equals(lease.state.snapshot().name()))lease.client.write(Tms282MyHomeActors.name(state,lease.controller,lease.fieldState));
        lease.state=state;lease.actors=ids;
    }
    public static boolean rename(Char chr,String name){
        var engine=active;if(engine==null || chr==null || !enabled())return false;
        synchronized(chr){var lease=engine.leases.get(chr);if(!valid(chr,lease))return false;boolean persisted=false;try{
            var before=engine.store.load(lease.account,lease.world).orElseThrow();var after=before.rename(lease.account,lease.world,name);
            after=engine.persist(before,after);persisted=true;engine.synchronizeScene(chr,lease,after);return true;
        }catch(SQLException|RuntimeException e){failure(chr,persisted?"小屋名稱已儲存，但畫面尚未同步，請重新進入小屋。":"目前無法確認名稱變更的完成狀態，請重新進入小屋；名稱限1到15字。",e);return false;}}
    }
    public static boolean leave(Char chr){
        var engine=active;if(engine==null || chr==null)return false;
        synchronized(Tms282VMatrixRuntime.ECONOMY_MONITOR){synchronized(chr){var lease=engine.leases.get(chr);if(!valid(chr,lease))return false;
            beforeWarp(chr,lease.returnField);chr.warp(lease.returnField);return true;}}
    }
    /** Shared Char.warp hook after destination validation, before old Field.removeChar or any SetField. */
    public static void beforeWarp(Char chr,Field destination){
        var engine=active;if(engine==null || chr==null || destination==null)return;
        synchronized(chr){var lease=engine.leases.get(chr);if(lease==null || destination==lease.field)return;
            if(valid(chr,lease))lease.client.write(Tms282MyHomeActors.destroy(lease.state,lease.controller,lease.fieldState,lease.actors,engine.rules));
            engine.leases.remove(chr,lease);
        }
    }
    /** Root calls after character data load, before choosing the login field; private scenes are never shared on relog. */
    public static void beforeLoginField(Char chr){
        if(chr==null || chr.getFieldID()!=INSTANCE_MAP)return;int target=100000000;
        try{var id=identity(chr,false);if(enabled())target=runtime().store.load(id[0],id[1]).map(s->s.returnMaps().getOrDefault(chr.getId(),100000000)).orElse(100000000);}
        catch(IOException|SQLException|RuntimeException e){System.out.println("[MyHome] Saved private scene returns to public fallback: "+e.getClass().getSimpleName());}
        if(FieldData.getFieldById(target)==null || !net.swordie.ms.connection.protocol.codec.FieldAvailability.active().isAvailable(target))target=100000000;
        chr.getAvatarData().getCharacterStat().setPosMap(target);chr.getAvatarData().getCharacterStat().setPortal(0);
    }
    /** Client identity prevents a late disconnect from removing a newer channel/session's lease. */
    public static void onDisconnect(Client client,Char chr){var engine=active;if(engine==null || chr==null)return;synchronized(chr){var lease=engine.leases.get(chr);if(lease!=null && lease.client==client)engine.leases.remove(chr,lease);var stamp=engine.registrations.get(chr);if(stamp!=null && stamp.client()==client)engine.registrations.remove(chr,stamp);}}
    private static void failure(Char chr,String message,Exception failure){if(chr!=null)chr.chatMessage(message);System.out.println("[MyHome] "+failure.getClass().getSimpleName()+": "+failure.getMessage());}
}
