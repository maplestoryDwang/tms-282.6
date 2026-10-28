package net.swordie.ms.client.character.tms282;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.enums.InvType;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import static net.swordie.ms.client.character.tms282.Tms282VMatrixJournal.*;

/** Durable single-item registration across legacy Aria items and InnoDB home state.
 * ECONOMY_MONITOR -> Char is held by the caller; blocked characters cannot save stale inventory.
 * Lock order is home row, pending row. Recovery is only before loading inventory, never over live memory.
 */
public final class Tms282MyHomeJournal {
    private static final ObjectMapper JSON=new ObjectMapper();
    private static final Set<Integer> BLOCKED=ConcurrentHashMap.newKeySet();
    private final Tms282MyHomeStore store;
    private final Tms282MyHomeRules rules;
    public record Intent(int characterId,long itemRow,Map<String,String> itemBefore,
                         Tms282MyHomeState homeBefore,Tms282MyHomeState homeAfter,long serial) { }
    public Tms282MyHomeJournal(Tms282MyHomeStore store,Tms282MyHomeRules rules){this.store=store;this.rules=rules;}
    public static boolean isBlocked(int characterId){return BLOCKED.contains(characterId);}

    public Intent execute(Char chr,Item item,Tms282MyHomeState before)throws SQLException {
        int character=chr.getId();
        if(character<=0 || !BLOCKED.add(character))throw conflict();
        boolean commitAttempted=false;
        try {
            Intent intent;
            try(var c=store.connection()) {
                c.setAutoCommit(false);
                try {
                    var current=store.load(c,before.accountId(),before.worldId(),true).orElseThrow(Tms282MyHomeJournal::conflict);
                    var outstanding=pending(c,before.accountId(),before.worldId());
                    if(outstanding!=null){commitAttempted=outstanding.characterId()==character;throw conflict();}
                    if(!current.equals(before) || chr.getAccount()==null || chr.getAccount().getOwnerUserId()!=before.accountId()
                            || chr.getAccount().getWorldId()!=before.worldId())throw conflict();
                    var inventory=chr.getConsumeInventory();
                    rules.requireRegistrableFurniture(item.getItemId());
                    if(item.getId()<=0 || item.getQuantity()<=0 || item.getBagIndex()<=0
                            || inventory.getItemBySlot(item.getBagIndex())!=item || item.getInvType()!=InvType.CONSUME
                            || item.isCash() || item.getDateExpire()==null || item.getDateExpire().isExpired())throw conflict();
                    var row=readItem(c,item.getId());
                    if(row==null || !sameMemory(item,row,character,inventory.getId())
                            || !Integer.toString(InvType.CONSUME.getVal()).equals(row.get("invtype"))
                            || row.get("trunkid")!=null || !"0".equals(row.get("auctionHouseStatus")))throw conflict();
                    try(var p=c.prepareStatement("SELECT id FROM items WHERE inventoryid=? AND bagindex=?")) {
                        p.setInt(1,inventory.getId());p.setInt(2,item.getBagIndex());
                        try(var r=p.executeQuery()){if(!r.next() || r.getLong(1)!=item.getId() || r.next())throw conflict();}
                    }
                    long serial=Math.addExact(before.snapshot().furniture().keySet().stream().mapToLong(Long::longValue).max().orElse(0),1);
                    var after=before.registerFurniture(before.accountId(),before.worldId(),serial,item.getItemId(),rules);
                    // The SQL snapshot contains nullable fields; preserve those exactly in the persisted intent.
                    intent=new Intent(character,item.getId(),Collections.unmodifiableMap(new LinkedHashMap<>(row)),before,after,serial);
                    validate(intent);
                    try(var p=c.prepareStatement("INSERT INTO account_myhome282_pending(account_id,world_id,character_id,payload) VALUES(?,?,?,?)")) {
                        p.setInt(1,before.accountId());p.setInt(2,before.worldId());p.setInt(3,character);p.setString(4,JSON.writeValueAsString(intent));
                        if(p.executeUpdate()!=1)throw conflict();
                    }
                    commitAttempted=true;c.commit();
                }catch(Exception e){if(e instanceof SQLException s && s.getErrorCode()==1062)commitAttempted=true;c.rollback();throw asSql(e);}
            }
            redo(intent.homeBefore().accountId(),intent.homeBefore().worldId(),character);
            // No mutations were permitted between the persisted baseline and this memory update.
            int remaining=Integer.parseInt(intent.itemBefore().get("quantity"))-1;
            if(remaining==0)chr.getConsumeInventory().removeItem(item);else item.setQuantity(remaining);
            BLOCKED.remove(character);return intent;
        }catch(SQLException|RuntimeException e){if(!commitAttempted)BLOCKED.remove(character);throw e;}
    }
    /** Query the owner key from the durable record, not from client-provided account/world IDs. */
    public void recoverForLoad(int characterId)throws SQLException {
        if(characterId<=0)throw conflict();BLOCKED.add(characterId);
        int account=0,world=0;
        try(var c=store.connection();var p=c.prepareStatement("SELECT account_id,world_id FROM account_myhome282_pending WHERE character_id=?")) {
            p.setInt(1,characterId);try(var r=p.executeQuery()){
                if(r.next()){account=r.getInt(1);world=r.getInt(2);if(r.next())throw conflict();}
            }
        }
        if(account!=0)redo(account,world,characterId);
        BLOCKED.remove(characterId);
    }
    /** Tests may replay this against fakeJDBC; callers must not use it over online inventory memory. */
    public void redo(int account,int world,int characterId)throws SQLException {
        try(var c=store.connection()) {
            c.setAutoCommit(false);
            try {
                var home=store.load(c,account,world,true).orElseThrow(Tms282MyHomeJournal::conflict);
                var intent=pending(c,account,world);
                if(intent==null){c.commit();return;}
                if(intent.characterId()!=characterId)throw conflict();validate(intent);
                if(!home.equals(intent.homeBefore()))throw conflict();
                var before=intent.itemBefore();int remaining=Integer.parseInt(before.get("quantity"))-1;
                Map<String,String> after=null;
                if(remaining>0){after=new LinkedHashMap<>(before);after.put("quantity",Integer.toString(remaining));}
                var actual=readItem(c,intent.itemRow());
                if(!Objects.equals(actual,after)) {
                    if(!before.equals(actual))throw conflict();
                    String sql=(remaining==0?"DELETE FROM items WHERE id=?":"UPDATE items SET quantity=? WHERE id=?")
                            +" AND "+String.join(" AND ",COLUMNS.stream().map(column->column+" <=> ?").toList());
                    try(var p=c.prepareStatement(sql)) {
                        int i=1;if(remaining>0)p.setInt(i++,remaining);p.setLong(i++,intent.itemRow());
                        for(String column:COLUMNS)p.setString(i++,before.get(column));
                        if(p.executeUpdate()!=1)throw conflict();
                    }
                }
                // Both statements below are InnoDB. Their rollback leaves the durable intent for redo;
                // the preceding Aria deduction remains and is recognized by its exact after-image.
                store.grant(c,intent.homeBefore(),intent.homeAfter());
                try(var p=c.prepareStatement("DELETE FROM account_myhome282_pending WHERE account_id=? AND world_id=? AND character_id=?")) {
                    p.setInt(1,account);p.setInt(2,world);p.setInt(3,characterId);if(p.executeUpdate()!=1)throw conflict();
                }
                c.commit();
            }catch(Exception e){c.rollback();throw asSql(e);}
        }
    }
    static Intent pending(Connection c,int account,int world)throws SQLException {
        try(var p=c.prepareStatement("SELECT payload FROM account_myhome282_pending WHERE account_id=? AND world_id=? FOR UPDATE")) {
            p.setInt(1,account);p.setInt(2,world);try(var r=p.executeQuery()) {
                if(!r.next())return null;
                try{var i=JSON.readValue(r.getString(1),Intent.class);i.homeBefore().requireOwnerMatches(account,world);return i;}
                catch(Exception e){throw new SQLException("Invalid home inventory recovery record",e);}
            }
        }
    }
    private void validate(Intent intent)throws SQLException {
        try {
            var before=intent.itemBefore();var home=intent.homeBefore();
            if(intent.characterId()<=0 || intent.itemRow()<=0 || !before.keySet().equals(new HashSet<>(COLUMNS))
                    || Integer.parseInt(before.get("charid"))!=intent.characterId() || Integer.parseInt(before.get("inventoryid"))<=0
                    || Integer.parseInt(before.get("bagindex"))<=0 || Integer.parseInt(before.get("quantity"))<=0
                    || !Integer.toString(InvType.CONSUME.getVal()).equals(before.get("invtype"))
                    || before.get("trunkid")!=null || !"0".equals(before.get("auctionHouseStatus"))
                    || !"0".equals(before.get("iscash")) || before.get("dateexpire")==null)throw conflict();
            int item=Integer.parseInt(before.get("itemid"));rules.requireRegistrableFurniture(item);rules.validate(home);
            if(home.snapshot().furniture().containsKey(intent.serial())
                    || !home.registerFurniture(home.accountId(),home.worldId(),intent.serial(),item,rules).equals(intent.homeAfter()))throw conflict();
        }catch(RuntimeException e){throw new SQLException("Invalid home grant intent",e);}
    }
    private static SQLException asSql(Exception e){return e instanceof SQLException s?s:new SQLException(e);}
    private static SQLException conflict(){return new SQLException("Home inventory registration conflicts; preserve intent and reload","40001");}
}
