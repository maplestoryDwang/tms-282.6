package net.swordie.ms.client.character.tms282;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.enums.InvType;
import net.swordie.ms.loaders.ItemData;
import net.swordie.ms.util.FileTime;
import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Durable redo for VMatrix's Aria item/money rows. Caller holds the character economic/save lock.
 * Pending characters cannot trade, mutate or save stale memory. Recover before loading inventory.
 * This handles application-process crashes; durability still depends on the database engine/storage.
 */
public final class Tms282VMatrixJournal {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Set<Integer> BLOCKED = ConcurrentHashMap.newKeySet();
    static final List<String> COLUMNS = List.of("inventoryid","charid","trunkid","auctionHouseStatus",
            "itemid","bagindex","cashitemserialnumber","dateexpire","invtype","iscash","quantity",
            "expireonlogout","owner","obtainedonce","zeroShareItemID","attribute","bossrewardid","mobtemplateid","partysize","price");
    public record Change(long rowId, Map<String,String> before, Map<String,String> after) { }
    public record Intent(int characterId,long expectedRevision,Tms282VMatrixState next,List<Change> changes,
                         int statId,String moneyBefore,String moneyAfter) { }
    private final DataSource source;
    public Tms282VMatrixJournal(){this(null);}
    public Tms282VMatrixJournal(DataSource source){this.source=source;}
    private Connection connection() throws SQLException{return source==null?DatabaseManager.getConnection():source.getConnection();}
    public static boolean isBlocked(int id){return BLOCKED.contains(id);}
    public boolean hasPending(int id)throws SQLException{
        try(var c=connection()){return pending(c,id,false)!=null;}
    }

    /** No packets, saves, or callbacks during redo. Publish economic memory only after final commit. */
    public Tms282VMatrixState execute(Char chr,Tms282VMatrixState old,Tms282VMatrixService.Plan plan)throws SQLException{
        int id=chr.getId();
        if(id<=0 || !BLOCKED.add(id))throw new SQLException("VMatrix recovery pending", "40001");
        boolean prepared=false;
        try {
            Intent intent;
            try(var c=connection()){
                c.setAutoCommit(false);
                try {
                    var current=Tms282VMatrixStore.read(c,id,true);
                    if(current==null || !current.equals(old) || pending(c,id,true)!=null)throw conflict();
                    intent=prepare(c,chr,old,plan);
                    validate(intent);
                    try(var s=c.prepareStatement("INSERT INTO char_vmatrix282_pending (character_id,expected_revision,payload) VALUES (?,?,?)")){
                        s.setInt(1,id);s.setLong(2,old.revision());s.setString(3,encode(intent));s.executeUpdate();
                    }
                    // Once commit is attempted its outcome can be unknown: leave the gate shut on failure.
                    prepared=true;c.commit();
                }catch(SQLException|RuntimeException e){c.rollback();throw e;}
            }
            Map<Long,Long> generated=redo(id);
            publish(chr,intent,generated);
            BLOCKED.remove(id);
            return intent.next();
        }catch(SQLException|RuntimeException e){
            if(!prepared){
                try{if(!hasPending(id))BLOCKED.remove(id);}catch(SQLException check){e.addSuppressed(check);}
            }
            throw e;
        }
    }

    /** Login recovery only, before the character's legacy items/money are loaded into memory. */
    public Optional<Tms282VMatrixState> recover(int id)throws SQLException{
        if(id<=0)throw new IllegalArgumentException("Invalid character");
        BLOCKED.add(id);
        redo(id);
        var state=new Tms282VMatrixStore(source).load(id);
        BLOCKED.remove(id);
        return state;
    }

    /** Production first-login recovery. Avatar-list money was loaded BEFORE redo; refresh it before unblocking.
     * Inventories are loaded by the caller afterwards. Never call this on a published online character.
     */
    public Optional<Tms282VMatrixState> recoverForLoad(Char chr)throws SQLException{
        Objects.requireNonNull(chr);
        int id=chr.getId();if(id<=0)throw new IllegalArgumentException("Invalid character");
        BLOCKED.add(id);
        redo(id);
        var stats=chr.getAvatarData().getCharacterStat();
        if(stats.getId()<=0)throw new SQLException("Missing persisted characterstats identity");
        String value;
        try(var c=connection()){value=readMoney(c,stats.getId(),id);}
        if(value==null)throw new SQLException("Missing persisted character money after VMatrix recovery");
        long money;
        try{money=Long.parseLong(value);}catch(NumberFormatException e){throw new SQLException("Invalid persisted character money",e);}
        if(money<0)throw new SQLException("Negative persisted character money");
        stats.setMoney(money);
        var state=new Tms282VMatrixStore(source).load(id);
        BLOCKED.remove(id);
        return state;
    }

    private Intent prepare(Connection c,Char chr,Tms282VMatrixState old,Tms282VMatrixService.Plan plan)throws SQLException{
        if(plan.state().revision()!=Math.addExact(old.revision(),1))throw conflict();
        var inventory=chr.getConsumeInventory();
        if(inventory.getId()<=0)throw new SQLException("Save inventory before using VMatrix");
        List<Change> changes=new ArrayList<>();Set<Long> touched=new HashSet<>();
        Set<Integer> slots=new HashSet<>();inventory.getItems().forEach(item->slots.add(item.getBagIndex()));
        int ordinal=0;
        for(var delta:plan.items()){
            if(delta.quantity()==0 || delta.itemId()/1_000_000!=2)throw new SQLException("VMatrix supports consume-item deltas only");
            if(delta.quantity()<0){
                long remaining=-(long)delta.quantity();
                for(Item item:inventory.getItems().stream().filter(i->i.getItemId()==delta.itemId()
                        && i.getQuantity()>0 && i.getDateExpire()!=null && !i.getDateExpire().isExpired())
                        .sorted(Comparator.comparingInt(Item::getBagIndex)).toList()){
                    if(remaining==0)break;
                    if(item.getId()<=0 || !touched.add(item.getId()))throw new SQLException("Save item before using VMatrix");
                    var before=readItem(c,item.getId());
                    if(before==null || !sameMemory(item,before,chr.getId(),inventory.getId()))throw conflict();
                    int taken=(int)Math.min(remaining,item.getQuantity());
                    Map<String,String> after=null;
                    if(item.getQuantity()>taken){after=new LinkedHashMap<>(before);after.put("quantity",Integer.toString(item.getQuantity()-taken));}
                    changes.add(new Change(item.getId(),before,after));remaining-=taken;
                }
                if(remaining!=0)throw conflict();
            }else{
                var info=ItemData.getItemInfoByID(delta.itemId());
                if(info==null || info.getSlotMax()<=0)throw new SQLException("Missing gemstone stack data");
                int remaining=delta.quantity();
                while(remaining>0){
                    int slot=1;while(slots.contains(slot))slot++;
                    if(slot>inventory.getSlots())throw new SQLException("Not enough consume inventory space");
                    // Do not merge new expiring stones into existing stacks with different expiry/identity.
                    slots.add(slot);int amount=Math.min(remaining,info.getSlotMax());
                    Item item=ItemData.getItemDeepCopy(delta.itemId());
                    if(item==null || item.getInvType()!=InvType.CONSUME || item.isCash())throw new SQLException("Invalid VMatrix gemstone template");
                    item.setInventoryID(inventory.getId());item.setCharID(chr.getId());item.setBagIndex(slot);item.setQuantity(amount);
                    item.setDateExpire(FileTime.fromEpochMillis(delta.expiresEpochMillis()));
                    var after=itemValues(item);
                    after.put("cashitemserialnumber",Long.toString(token(plan.state().revision(),++ordinal)));
                    if(findToken(c,chr.getId(),after.get("cashitemserialnumber"))!=0 || occupied(c,inventory.getId(),slot))throw conflict();
                    changes.add(new Change(0,null,after));remaining-=amount;
                }
            }
        }
        int statId=chr.getAvatarData().getCharacterStat().getId();
        String before=null,after=null;
        if(plan.mesoDelta()!=0){
            before=readMoney(c,statId,chr.getId());
            if(before==null || Long.parseLong(before)!=chr.getMoney())throw conflict();
            long money=Math.addExact(chr.getMoney(),plan.mesoDelta());if(money<0)throw conflict();after=Long.toString(money);
        }
        return new Intent(chr.getId(),old.revision(),plan.state(),List.copyOf(changes),statId,before,after);
    }

    /** Returns token->generated rowId for publishing a just-executed operation. */
    private Map<Long,Long> redo(int id)throws SQLException{
        try(var c=connection()){
            c.setAutoCommit(false);
            try{
                // Same lock order as prepare: matrix authority then pending row.
                var current=Tms282VMatrixStore.read(c,id,true);
                Intent intent=pending(c,id,true);
                if(intent==null){c.commit();return Map.of();}
                validate(intent);
                if(current==null || current.revision()!=intent.expectedRevision())throw conflict();
                Map<Long,Long> generated=new HashMap<>();
                for(var change:intent.changes()){
                    if(change.before()==null){
                        String serial=change.after().get("cashitemserialnumber");
                        long row=findToken(c,id,serial);
                        if(row==0){
                            if(occupied(c,integer(change.after(),"inventoryid"),integer(change.after(),"bagindex")))throw conflict();
                            insertItem(c,change.after());
                            row=findToken(c,id,serial);
                        }
                        if(row==0 || !change.after().equals(readItem(c,row)))throw conflict();
                        generated.put(Long.parseLong(serial),row);
                    }else{
                        var actual=readItem(c,change.rowId());
                        if(Objects.equals(actual,change.after()))continue;
                        if(!change.before().equals(actual))throw conflict();
                        String sql=change.after()==null?"DELETE FROM items WHERE id=?":"UPDATE items SET quantity=? WHERE id=?";
                        sql+=" AND "+String.join(" AND ",COLUMNS.stream().map(column->column+" <=> ?").toList());
                        try(var s=c.prepareStatement(sql)){
                            int p=1;if(change.after()!=null)s.setString(p++,change.after().get("quantity"));s.setLong(p++,change.rowId());
                            for(String column:COLUMNS)s.setString(p++,change.before().get(column));
                            if(s.executeUpdate()!=1)throw conflict();
                        }
                    }
                }
                if(intent.moneyBefore()!=null){
                    String actual=readMoney(c,intent.statId(),id);
                    if(!intent.moneyAfter().equals(actual)){
                        if(!intent.moneyBefore().equals(actual))throw conflict();
                        try(var s=c.prepareStatement("UPDATE characterstats SET money=? WHERE id=? AND characterid=? AND money <=> ?")){
                            s.setString(1,intent.moneyAfter());s.setInt(2,intent.statId());s.setInt(3,id);s.setString(4,intent.moneyBefore());
                            if(s.executeUpdate()!=1)throw conflict();
                        }
                    }
                }
                var next=intent.next();
                try(var s=c.prepareStatement("UPDATE character_vmatrix282 SET points=?,revision=?,migration_pending=?,retained_ids=? WHERE character_id=? AND revision=?")){
                    s.setLong(1,next.points());s.setLong(2,next.revision());s.setBoolean(3,next.migrationPending());
                    s.setString(4,Tms282VMatrixStore.encodeSet(next.retainedSet()));s.setInt(5,id);s.setLong(6,intent.expectedRevision());
                    if(s.executeUpdate()!=1)throw conflict();
                }
                try(var s=c.prepareStatement("DELETE FROM character_vmatrix282_core WHERE character_id=?")){s.setInt(1,id);s.executeUpdate();}
                Tms282VMatrixStore.writeCores(c,id,next);
                try(var s=c.prepareStatement("DELETE FROM char_vmatrix282_pending WHERE character_id=? AND expected_revision=?")){
                    s.setInt(1,id);s.setLong(2,intent.expectedRevision());if(s.executeUpdate()!=1)throw conflict();
                }
                c.commit();return Map.copyOf(generated);
            }catch(SQLException|RuntimeException e){c.rollback();throw e;}
        }
    }

    private static void publish(Char chr,Intent intent,Map<Long,Long> generated)throws SQLException{
        var items=chr.getConsumeInventory().getItems();
        for(var change:intent.changes()){
            if(change.before()==null){
                var a=change.after();Item item=ItemData.getItemDeepCopy(integer(a,"itemid"));
                item.setId(generated.get(Long.parseLong(a.get("cashitemserialnumber"))));
                item.setInventoryID(integer(a,"inventoryid"));item.setCharID(chr.getId());item.setBagIndex(integer(a,"bagindex"));
                item.setQuantity(integer(a,"quantity"));item.setDateExpire(FileTime.fromEpochMillis(Timestamp.valueOf(a.get("dateexpire")).getTime()));
                item.setCashItemSerialNumber(Long.parseLong(a.get("cashitemserialnumber")));
                chr.getConsumeInventory().addSwapItem(item);
            }else{
                Item item=items.stream().filter(i->i.getId()==change.rowId()).findFirst().orElseThrow(()->new IllegalStateException("Inventory changed during VMatrix commit"));
                if(change.after()==null)chr.getConsumeInventory().removeItem(item);else item.setQuantity(integer(change.after(),"quantity"));
            }
        }
        if(intent.moneyAfter()!=null)chr.getAvatarData().getCharacterStat().setMoney(Long.parseLong(intent.moneyAfter()));
    }
    private static Intent pending(Connection c,int id,boolean lock)throws SQLException{
        try(var s=c.prepareStatement("SELECT payload FROM char_vmatrix282_pending WHERE character_id=?"+(lock?" FOR UPDATE":""))){
            s.setInt(1,id);try(var r=s.executeQuery()){if(!r.next())return null;
                try{Intent intent=JSON.readValue(r.getString(1),Intent.class);if(intent.characterId()!=id)throw conflict();return intent;}
                catch(java.io.IOException e){throw new SQLException("Invalid VMatrix redo record",e);}
            }
        }
    }
    private static String encode(Intent intent)throws SQLException{try{return JSON.writeValueAsString(intent);}catch(java.io.IOException e){throw new SQLException(e);}}
    private static int integer(Map<String,String> row,String key){return Integer.parseInt(row.get(key));}
    private static long token(long revision,int ordinal){
        if(revision<=0 || revision>0x7fffffffffffL || ordinal<=0 || ordinal>65535)throw new IllegalArgumentException("VMatrix token range");
        return -((revision<<16)|ordinal);
    }
    private static void validate(Intent intent)throws SQLException{
        if(intent.characterId()<=0 || intent.next().revision()!=intent.expectedRevision()+1)throw conflict();
        Set<Long> ids=new HashSet<>();Set<String> tokens=new HashSet<>();
        for(var change:intent.changes()){
            if(change.before()==null && change.after()==null)throw conflict();
            var row=change.before()==null?change.after():change.before();
            if(!row.keySet().equals(new HashSet<>(COLUMNS)) || integer(row,"charid")!=intent.characterId()
                    || integer(row,"invtype")!=InvType.CONSUME.getVal() || integer(row,"quantity")<=0)throw conflict();
            if(change.before()==null){
                if(change.rowId()!=0 || Long.parseLong(row.get("cashitemserialnumber"))>=0 || !tokens.add(row.get("cashitemserialnumber")))throw conflict();
            }else{
                if(change.rowId()<=0 || !ids.add(change.rowId()))throw conflict();
                if(change.after()!=null){
                    var expected=new LinkedHashMap<>(change.before());expected.put("quantity",change.after().get("quantity"));
                    if(!expected.equals(change.after()) || integer(change.after(),"quantity")<=0 || integer(change.after(),"quantity")>=integer(change.before(),"quantity"))throw conflict();
                }
            }
        }
        if((intent.moneyBefore()==null)!=(intent.moneyAfter()==null))throw conflict();
        if(intent.moneyBefore()!=null && (intent.statId()<=0 || Long.parseLong(intent.moneyAfter())<0 || Long.parseLong(intent.moneyAfter())>=Long.parseLong(intent.moneyBefore())))throw conflict();
    }
    static Map<String,String> readItem(Connection c,long id)throws SQLException{
        try(var s=c.prepareStatement("SELECT "+String.join(",",COLUMNS)+" FROM items WHERE id=?")){
            s.setLong(1,id);try(var r=s.executeQuery()){if(!r.next())return null;var values=new LinkedHashMap<String,String>();
                for(String column:COLUMNS)values.put(column,column.equals("dateexpire")?(r.getTimestamp(column)==null?null:r.getTimestamp(column).toString()):r.getString(column));return values;}
        }
    }
    private static long findToken(Connection c,int id,String token)throws SQLException{
        try(var s=c.prepareStatement("SELECT id FROM items WHERE charid=? AND cashitemserialnumber=?")){
            s.setInt(1,id);s.setString(2,token);try(var r=s.executeQuery()){if(!r.next())return 0;long row=r.getLong(1);if(r.next())throw conflict();return row;}
        }
    }
    private static boolean occupied(Connection c,int inventory,int slot)throws SQLException{
        try(var s=c.prepareStatement("SELECT id FROM items WHERE inventoryid=? AND bagindex=?")){
            s.setInt(1,inventory);s.setInt(2,slot);try(var r=s.executeQuery()){return r.next();}
        }
    }
    static String readMoney(Connection c,int stat,int chr)throws SQLException{
        try(var s=c.prepareStatement("SELECT money FROM characterstats WHERE id=? AND characterid=?")){
            s.setInt(1,stat);s.setInt(2,chr);try(var r=s.executeQuery()){return r.next()?r.getString(1):null;}
        }
    }
    private static void insertItem(Connection c,Map<String,String> row)throws SQLException{
        try(var s=c.prepareStatement("INSERT INTO items ("+String.join(",",COLUMNS)+") VALUES ("+String.join(",",Collections.nCopies(COLUMNS.size(),"?"))+")")){
            int p=1;for(String column:COLUMNS)s.setString(p++,row.get(column));if(s.executeUpdate()!=1)throw conflict();
        }
    }
    static boolean sameMemory(Item item,Map<String,String> row,int chr,int inventory){
        return integer(row,"charid")==chr && integer(row,"inventoryid")==inventory && integer(row,"itemid")==item.getItemId()
                && integer(row,"bagindex")==item.getBagIndex() && integer(row,"quantity")==item.getQuantity()
                && Objects.equals(row.get("dateexpire"),DatabaseManager.convertToDateTimeSQL(item.getDateExpire()).toString());
    }
    private static Map<String,String> itemValues(Item item){
        Map<String,String> row=new LinkedHashMap<>();
        Object[] values={item.getInventoryID(),item.getCharID(),null,item.getAuctionHouseStatus().getVal(),item.getItemId(),item.getBagIndex(),0,
                DatabaseManager.convertToDateTimeSQL(item.getDateExpire()).toString(),item.getInvType().getVal(),item.isCash()?1:0,item.getQuantity(),
                item.isExpireOnLogout()?1:0,item.getOwner(),item.hasObtainedOnce()?1:0,item.getZeroShareItemID(),item.getItemAttribute(),item.getBossRewardID(),item.getMobTemplateID(),item.getPartySize(),item.getPrice()};
        for(int i=0;i<COLUMNS.size();i++)row.put(COLUMNS.get(i),values[i]==null?null:values[i].toString());return row;
    }
    private static SQLException conflict(){return new SQLException("VMatrix persisted economy changed; keep pending and reload safely","40001");}
}
