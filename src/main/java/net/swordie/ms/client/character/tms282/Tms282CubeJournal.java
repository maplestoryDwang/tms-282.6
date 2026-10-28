package net.swordie.ms.client.character.tms282;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.Equip;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.enums.InvType;
import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import static net.swordie.ms.client.character.tms282.Tms282VMatrixJournal.*;

/** Durable cube payment/result redo for Aria player tables. Caller holds ECONOMY_MONITOR then chr.
 * A choice remains in InnoDB across logout; choosing writes a second intent and never charges again.
 * No JDBC rollback claim is made for Aria: each external row is compared against its before/after image.
 */
public final class Tms282CubeJournal {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Set<Integer> BLOCKED = ConcurrentHashMap.newKeySet();
    public record Intent(String token, int characterId, int statId, long equipId, Map<String,String> equipBefore,
                         String optionsBefore, String optionsAfter, long cubeId, Map<String,String> cubeBefore,
                         long moneyBefore, long moneyAfter, boolean choice, boolean selecting,
                         boolean bonus, int index) { }
    private final DataSource source;
    public Tms282CubeJournal() { this(null); }
    public Tms282CubeJournal(DataSource source) { this.source=source; }
    private Connection connection() throws SQLException { return source==null?DatabaseManager.getConnection():source.getConnection(); }
    public static boolean isBlocked(int id) { return BLOCKED.contains(id); }

    public Intent execute(Char chr, Item cube, Equip equip, Equip rolled, long cost, boolean choice,
                          boolean bonus, int index) throws SQLException {
        if(cost<0 || chr.getMoney()<cost || cube.getQuantity()<1 || equip==rolled) throw conflict();
        return commit(chr, () -> {
            try(var c=connection()) {
                var equipRow=readItem(c,equip.getId()); var cubeRow=readItem(c,cube.getId());
                var stats=chr.getAvatarData().getCharacterStat();
                if(equipRow==null || cubeRow==null
                        || !sameMemory(equip,equipRow,chr.getId(),chr.getInventoryByType(equip.getInvType()).getId())
                        || !sameMemory(cube,cubeRow,chr.getId(),chr.getInventoryByType(cube.getInvType()).getId())
                        || !equip.getOptionsSQL().equals(readOptions(c,equip.getId()))
                        || !Long.toString(chr.getMoney()).equals(readMoney(c,stats.getId(),chr.getId()))
                        || read(c,"char_cube282_choice",chr.getId(),false)!=null) throw conflict();
                Intent intent=new Intent(UUID.randomUUID().toString(),chr.getId(),stats.getId(),equip.getId(),equipRow,
                        equip.getOptionsSQL(),rolled.getOptionsSQL(),cube.getId(),cubeRow,chr.getMoney(),chr.getMoney()-cost,
                        choice,false,bonus,index);
                validate(intent); return intent;
            }
        }, intent -> {
            int remaining=Integer.parseInt(intent.cubeBefore().get("quantity"))-1;
            cube.setQuantity(remaining);
            if(remaining==0) chr.getInventoryByType(cube.getInvType()).removeItem(cube);
            chr.getAvatarData().getCharacterStat().setMoney(intent.moneyAfter());
            if(!intent.choice()) equip.setOptions(options(intent.optionsAfter()));
        });
    }

    /** keepNew=false discards the proposal without overwriting any equipment changes made since opening. */
    public Intent choose(Char chr, Equip equip, boolean keepNew) throws SQLException {
        return commit(chr, () -> {
            try(var c=connection()) {
                Intent old=read(c,"char_cube282_choice",chr.getId(),false);
                if(old==null || old.equipId()!=equip.getId()) throw conflict();
                var row=readItem(c,equip.getId());
                if(row==null || !sameMemory(equip,row,chr.getId(),chr.getInventoryByType(equip.getInvType()).getId())
                        || !equip.getOptionsSQL().equals(readOptions(c,equip.getId()))) throw conflict();
                if(keepNew && !old.optionsBefore().equals(equip.getOptionsSQL())) throw conflict();
                return new Intent(old.token(),old.characterId(),old.statId(),old.equipId(),row,
                        equip.getOptionsSQL(),keepNew?old.optionsAfter():equip.getOptionsSQL(),old.cubeId(),old.cubeBefore(),
                        old.moneyBefore(),old.moneyAfter(),true,true,old.bonus(),old.index());
            }
        }, intent -> equip.setOptions(options(intent.optionsAfter())));
    }

    @FunctionalInterface private interface Prepare { Intent get() throws SQLException; }
    private Intent commit(Char chr, Prepare prepare, java.util.function.Consumer<Intent> publish) throws SQLException {
        int id=chr.getId(); if(id<=0 || !BLOCKED.add(id)) throw conflict();
        boolean attempted=false;
        try {
            Intent intent;
            try(var c=connection()) {
                c.setAutoCommit(false);
                try {
                    if(read(c,"char_cube282_pending",id,true)!=null) { attempted=true; throw conflict(); }
                    intent=prepare.get(); validate(intent);
                    insert(c,"char_cube282_pending",intent);
                    attempted=true; c.commit();
                } catch(Exception e) { c.rollback(); throw sql(e); }
            }
            redo(id);
            publish.accept(intent);
            BLOCKED.remove(id);
            return intent;
        } catch(SQLException|RuntimeException e) { if(!attempted) BLOCKED.remove(id); throw e; }
    }

    public Intent choice(int id) throws SQLException {
        try(var c=connection()) { return read(c,"char_cube282_choice",id,false); }
    }

    /** A GM removal/expiry/ownership transfer must not permanently block character login.
     * Only the InnoDB proposal is discarded; never touch an unavailable/foreign equipment or refund a guessed cost.
     * If DB still owns the item in an equipment inventory, retain the proposal for a later complete reload.
     */
    public boolean cancelUnavailableTarget(Char chr) throws SQLException {
        int id=chr.getId();if(id<=0 || !BLOCKED.add(id))throw conflict();
        boolean attempted=false;
        try(var c=connection()) {
            c.setAutoCommit(false);
            try {
                if(read(c,"char_cube282_pending",id,true)!=null){attempted=true;throw conflict();}
                Intent intent=read(c,"char_cube282_choice",id,true);
                if(intent==null){c.commit();BLOCKED.remove(id);return true;}
                var row=readItem(c,intent.equipId());
                boolean stillOwned=false;
                if(row!=null && Integer.toString(id).equals(row.get("charid"))) {
                    int inv=Integer.parseInt(row.get("inventoryid")),type=Integer.parseInt(row.get("invtype"));
                    boolean equipmentInventory=type==InvType.EQUIP.getVal()&&inv==chr.getEquipInventory().getId()
                            || type==InvType.EQUIPPED.getVal()&&inv==chr.getEquippedInventory().getId();
                    String expiry=row.get("dateexpire");
                    boolean expired=expiry!=null&&Timestamp.valueOf(expiry).getTime()<System.currentTimeMillis();
                    stillOwned=equipmentInventory&&!expired;
                }
                if(stillOwned){c.rollback();BLOCKED.remove(id);return false;}
                delete(c,"char_cube282_choice",id);attempted=true;c.commit();BLOCKED.remove(id);return true;
            }catch(Exception e){c.rollback();throw sql(e);}
        }catch(SQLException|RuntimeException e){if(!attempted)BLOCKED.remove(id);throw e;}
    }

    /** Must run before loading inventory; never replay over an online character's stale memory. */
    public void recoverForLoad(Char chr) throws SQLException {
        BLOCKED.add(chr.getId()); redo(chr.getId());
        try(var c=connection()) {
            var stats=chr.getAvatarData().getCharacterStat();
            String money=readMoney(c,stats.getId(),chr.getId());
            if(money==null || Long.parseLong(money)<0) throw conflict();
            stats.setMoney(Long.parseLong(money));
        }
        BLOCKED.remove(chr.getId());
    }

    public void redo(int id) throws SQLException {
        try(var c=connection()) {
            c.setAutoCommit(false);
            try {
                Intent i=read(c,"char_cube282_pending",id,true);
                if(i==null) { c.commit(); return; }
                validate(i);
                if(!i.equipBefore().equals(readItem(c,i.equipId()))) throw conflict();
                if(i.selecting()) {
                    Intent old=read(c,"char_cube282_choice",id,true);
                    if(old==null || !old.token().equals(i.token()) || old.equipId()!=i.equipId()) throw conflict();
                } else {
                    var after=new LinkedHashMap<>(i.cubeBefore());
                    int remaining=Integer.parseInt(i.cubeBefore().get("quantity"))-1;
                    after.put("quantity",Integer.toString(remaining));
                    Map<String,String> actual=readItem(c,i.cubeId());
                    if(!Objects.equals(actual,remaining==0?null:after)) {
                        if(!i.cubeBefore().equals(actual)) throw conflict();
                        String query=(remaining==0?"DELETE FROM items WHERE id=?":"UPDATE items SET quantity=? WHERE id=?")
                                +" AND "+String.join(" AND ",COLUMNS.stream().map(s->s+" <=> ?").toList());
                        try(var s=c.prepareStatement(query)) {
                            int n=1;if(remaining>0)s.setInt(n++,remaining);s.setLong(n++,i.cubeId());
                            for(String key:COLUMNS)s.setString(n++,i.cubeBefore().get(key));
                            if(s.executeUpdate()!=1)throw conflict();
                        }
                    }
                    String money=readMoney(c,i.statId(),id);
                    if(!Long.toString(i.moneyAfter()).equals(money)) {
                        if(!Long.toString(i.moneyBefore()).equals(money))throw conflict();
                        try(var s=c.prepareStatement("UPDATE characterstats SET money=? WHERE id=? AND characterid=? AND money=?")) {
                            s.setLong(1,i.moneyAfter());s.setInt(2,i.statId());s.setInt(3,id);s.setLong(4,i.moneyBefore());
                            if(s.executeUpdate()!=1)throw conflict();
                        }
                    }
                }
                if(!i.choice() || i.selecting()) {
                    String actual=readOptions(c,i.equipId());
                    if(!i.optionsAfter().equals(actual)) {
                        if(!i.optionsBefore().equals(actual))throw conflict();
                        try(var s=c.prepareStatement("UPDATE equips SET options=? WHERE itemid=? AND options=?")) {
                            s.setString(1,i.optionsAfter());s.setLong(2,i.equipId());s.setString(3,i.optionsBefore());
                            if(s.executeUpdate()!=1)throw conflict();
                        }
                    }
                } else {
                    if(!i.optionsBefore().equals(readOptions(c,i.equipId())))throw conflict();
                    Intent old=read(c,"char_cube282_choice",id,true);
                    if(old==null) insert(c,"char_cube282_choice",i);
                    else if(!old.equals(i))throw conflict();
                }
                if(i.selecting())delete(c,"char_cube282_choice",id);
                delete(c,"char_cube282_pending",id); c.commit();
            } catch(Exception e) { c.rollback(); throw sql(e); }
        }
    }

    private static Intent read(Connection c,String table,int id,boolean lock) throws SQLException {
        try(var s=c.prepareStatement("SELECT payload FROM "+table+" WHERE character_id=?"+(lock?" FOR UPDATE":""))) {
            s.setInt(1,id);try(var r=s.executeQuery()) {
                if(!r.next())return null;
                try { Intent i=JSON.readValue(r.getString(1),Intent.class);validate(i);if(i.characterId()!=id || r.next())throw conflict();return i; }
                catch(java.io.IOException e) {throw new SQLException("Invalid cube journal",e);}
            }
        }
    }
    private static void insert(Connection c,String table,Intent i) throws SQLException {
        try(var s=c.prepareStatement("INSERT INTO "+table+" (character_id,payload) VALUES (?,?)")) {
            s.setInt(1,i.characterId());s.setString(2,JSON.writeValueAsString(i));if(s.executeUpdate()!=1)throw conflict();
        } catch(java.io.IOException e) {throw new SQLException(e);}
    }
    private static void delete(Connection c,String table,int id) throws SQLException {
        try(var s=c.prepareStatement("DELETE FROM "+table+" WHERE character_id=?")) {
            s.setInt(1,id);if(s.executeUpdate()!=1)throw conflict();
        }
    }
    private static String readOptions(Connection c,long id) throws SQLException {
        try(var s=c.prepareStatement("SELECT options FROM equips WHERE itemid=?")) {
            s.setLong(1,id);try(var r=s.executeQuery()) {if(!r.next())throw conflict();String v=r.getString(1);if(r.next())throw conflict();return v;}
        }
    }
    public static ArrayList<Integer> options(String value) {
        var result=new ArrayList<Integer>();for(String s:value.split(",",-1))result.add(Integer.parseInt(s));
        if(result.size()!=7)throw new IllegalArgumentException("Seven equipment option fields required");return result;
    }
    private static void validate(Intent i) throws SQLException {
        if(i.token()==null || i.token().length()!=36 || i.characterId()<=0 || i.statId()<=0 || i.equipId()<=0 || i.cubeId()<=0
                || i.equipId()==i.cubeId() || i.moneyAfter()<0 || i.moneyAfter()>i.moneyBefore() || i.index()<0 || i.index()>255
                || i.selecting() && !i.choice() || !i.equipBefore().keySet().equals(new HashSet<>(COLUMNS))
                || !i.cubeBefore().keySet().equals(new HashSet<>(COLUMNS))
                || !Integer.toString(i.characterId()).equals(i.equipBefore().get("charid"))
                || !Integer.toString(i.characterId()).equals(i.cubeBefore().get("charid"))
                || Integer.parseInt(i.cubeBefore().get("quantity"))<1)throw conflict();
        var before=options(i.optionsBefore());var after=options(i.optionsAfter());
        for(int p=0;p<7;p++) {
            if(p< (i.bonus()?3:0) || p>= (i.bonus()?6:3)) {if(!before.get(p).equals(after.get(p)))throw conflict();}
            else if(after.get(p)<0 || before.get(p)!=0 && after.get(p)==0)throw conflict();
        }
    }
    private static SQLException sql(Exception e) {return e instanceof SQLException s?s:new SQLException(e);}
    private static SQLException conflict() {return new SQLException("Cube economy conflict: preserve journal and reload", "40001");}
}
