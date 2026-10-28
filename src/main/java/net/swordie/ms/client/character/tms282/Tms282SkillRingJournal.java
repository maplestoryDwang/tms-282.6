package net.swordie.ms.client.character.tms282;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.Equip;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.connection.packet.Tms282SkillRingRequest.Rule;
import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import static net.swordie.ms.client.character.tms282.Tms282VMatrixJournal.*;

/** Durable redo for the existing nontransactional Aria equipment/items/money tables.
 * Callers hold the shared economic gate. Recovery runs before loading a character's inventory.
 * Only the new pending table requires InnoDB; no engine conversion of existing player data.
 */
public final class Tms282SkillRingJournal {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Set<Integer> BLOCKED = ConcurrentHashMap.newKeySet();
    public record Stone(long id, Map<String,String> before, int remaining) { }
    public record Intent(int characterId, int statId, long ringId, Map<String,String> ringBefore,
                         int fromLevel, int toLevel, long moneyBefore, long moneyAfter,
                         Rule rule, boolean success, List<Stone> stones) { }
    private final DataSource source;
    public Tms282SkillRingJournal() { this(null); }
    public Tms282SkillRingJournal(DataSource source) { this.source=source; }
    private Connection connection() throws SQLException { return source==null ? DatabaseManager.getConnection() : source.getConnection(); }
    public static boolean isBlocked(int characterId) { return BLOCKED.contains(characterId); }

    public boolean execute(Char chr, Equip ring, Rule rule, boolean success) throws SQLException {
        int id=chr.getId();
        if(id<=0 || !BLOCKED.add(id)) throw conflict();
        boolean commitAttempted=false;
        try {
            Intent intent;
            try(var c=connection()) {
                c.setAutoCommit(false);
                try {
                    if(pending(c,id,true)!=null) { commitAttempted=true; throw conflict(); }
                    intent=prepare(c,chr,ring,rule,success);
                    try(var s=c.prepareStatement("INSERT INTO char_skillring282_pending (character_id,payload) VALUES (?,?)")) {
                        s.setInt(1,id); s.setString(2,JSON.writeValueAsString(intent));
                        if(s.executeUpdate()!=1) throw conflict();
                    }
                    commitAttempted=true;
                    c.commit();
                } catch(Exception e) { c.rollback(); throw e instanceof SQLException sql ? sql : new SQLException(e); }
            }
            redo(id);
            for(Stone change:intent.stones()) {
                Item item=chr.getConsumeInventory().getItems().stream().filter(i->i.getId()==change.id()).findFirst().orElseThrow();
                if(change.remaining()==0) chr.getConsumeInventory().removeItem(item);
                else item.setQuantity(change.remaining());
            }
            ring.setItemLevel((short)intent.toLevel());
            chr.getAvatarData().getCharacterStat().setMoney(intent.moneyAfter());
            BLOCKED.remove(id);
            return success;
        } catch(SQLException|RuntimeException e) {
            if(!commitAttempted) BLOCKED.remove(id);
            throw e;
        }
    }

    private static Intent prepare(Connection c,Char chr,Equip ring,Rule rule,boolean success) throws SQLException {
        var ringRow=readItem(c,ring.getId());
        if(ringRow==null || !sameMemory(ring,ringRow,chr.getId(),chr.getEquipInventory().getId())
                || ring.getItemId()!=rule.ringItemId() || ring.getItemLevel()!=rule.ringFromLevel()
                || readLevel(c,ring.getId())!=rule.ringFromLevel()) throw conflict();
        var stats=chr.getAvatarData().getCharacterStat();
        String money=readMoney(c,stats.getId(),chr.getId());
        if(money==null || Long.parseLong(money)!=chr.getMoney() || chr.getMoney()<rule.meso()) throw conflict();
        List<Stone> changes=new ArrayList<>(); int needed=rule.useCount();
        for(Item item:chr.getConsumeInventory().getItems().stream()
                .filter(i->rule.acceptsStone(i.getItemId()) && i.getQuantity()>0 && i.getDateExpire()!=null && !i.getDateExpire().isExpired())
                .sorted(Comparator.comparingInt(Item::getBagIndex)).toList()) {
            if(needed==0) break;
            var row=readItem(c,item.getId());
            if(row==null || !sameMemory(item,row,chr.getId(),chr.getConsumeInventory().getId())) throw conflict();
            int used=Math.min(needed,item.getQuantity());
            changes.add(new Stone(item.getId(),row,item.getQuantity()-used)); needed-=used;
        }
        if(needed!=0) throw conflict();
        return new Intent(chr.getId(),stats.getId(),ring.getId(),ringRow,rule.ringFromLevel(),
                success?rule.ringToLevel():rule.ringFromLevel(),chr.getMoney(),chr.getMoney()-rule.meso(),rule,success,List.copyOf(changes));
    }

    public void recoverForLoad(Char chr) throws SQLException {
        BLOCKED.add(chr.getId());
        redo(chr.getId());
        try(var c=connection()) {
            var stats=chr.getAvatarData().getCharacterStat();
            String money=readMoney(c,stats.getId(),chr.getId());
            if(money==null || Long.parseLong(money)<0) throw conflict();
            stats.setMoney(Long.parseLong(money));
        }
        BLOCKED.remove(chr.getId());
    }

    /** Public for isolated recovery tests. Never recover over an online character's stale memory. */
    public void redo(int characterId) throws SQLException {
        try(var c=connection()) {
            c.setAutoCommit(false);
            try {
                Intent intent=pending(c,characterId,true);
                if(intent==null) { c.commit(); return; }
                validate(intent);
                if(!intent.ringBefore().equals(readItem(c,intent.ringId()))) throw conflict();
                for(Stone change:intent.stones()) {
                    var actual=readItem(c,change.id());
                    Map<String,String> after=null;
                    if(change.remaining()>0) { after=new LinkedHashMap<>(change.before()); after.put("quantity",Integer.toString(change.remaining())); }
                    if(Objects.equals(actual,after)) continue;
                    if(!change.before().equals(actual)) throw conflict();
                    String sql=change.remaining()==0 ? "DELETE FROM items WHERE id=?" : "UPDATE items SET quantity=? WHERE id=?";
                    sql+=" AND "+String.join(" AND ",COLUMNS.stream().map(column->column+" <=> ?").toList());
                    try(var s=c.prepareStatement(sql)) {
                        int n=1; if(change.remaining()>0) s.setInt(n++,change.remaining()); s.setLong(n++,change.id());
                        for(String column:COLUMNS) s.setString(n++,change.before().get(column));
                        if(s.executeUpdate()!=1) throw conflict();
                    }
                }
                long money=Long.parseLong(Objects.requireNonNull(readMoney(c,intent.statId(),characterId)));
                if(money!=intent.moneyAfter()) {
                    if(money!=intent.moneyBefore()) throw conflict();
                    try(var s=c.prepareStatement("UPDATE characterstats SET money=? WHERE id=? AND characterid=? AND money=?")) {
                        s.setLong(1,intent.moneyAfter());s.setInt(2,intent.statId());s.setInt(3,characterId);s.setLong(4,intent.moneyBefore());
                        if(s.executeUpdate()!=1) throw conflict();
                    }
                }
                int level=readLevel(c,intent.ringId());
                if(level!=intent.toLevel()) {
                    if(level!=intent.fromLevel()) throw conflict();
                    try(var s=c.prepareStatement("UPDATE equips SET level=? WHERE itemid=? AND level=?")) {
                        s.setInt(1,intent.toLevel());s.setLong(2,intent.ringId());s.setInt(3,intent.fromLevel());
                        if(s.executeUpdate()!=1) throw conflict();
                    }
                }
                try(var s=c.prepareStatement("DELETE FROM char_skillring282_pending WHERE character_id=?")) {
                    s.setInt(1,characterId);if(s.executeUpdate()!=1) throw conflict();
                }
                c.commit();
            } catch(Exception e) { c.rollback(); throw e instanceof SQLException sql ? sql : new SQLException(e); }
        }
    }

    private static int readLevel(Connection c,long ringId) throws SQLException {
        try(var s=c.prepareStatement("SELECT level FROM equips WHERE itemid=?")) {
            s.setLong(1,ringId);try(var r=s.executeQuery()) {
                if(!r.next()) throw conflict(); int level=r.getInt(1);if(r.next())throw conflict();return level;
            }
        }
    }
    private static Intent pending(Connection c,int id,boolean lock) throws SQLException {
        try(var s=c.prepareStatement("SELECT payload FROM char_skillring282_pending WHERE character_id=?"+(lock?" FOR UPDATE":""))) {
            s.setInt(1,id);try(var r=s.executeQuery()) {
                if(!r.next())return null;
                try { Intent intent=JSON.readValue(r.getString(1),Intent.class);if(intent.characterId()!=id)throw conflict();return intent; }
                catch(java.io.IOException e){throw new SQLException("Invalid ring recovery record",e);}
            }
        }
    }
    private static void validate(Intent i) throws SQLException {
        if(i.characterId()<=0 || i.statId()<=0 || i.ringId()<=0 || i.fromLevel()!=i.rule().ringFromLevel()
                || i.toLevel()!=(i.success()?i.rule().ringToLevel():i.fromLevel())
                || i.moneyBefore()<i.rule().meso() || i.moneyAfter()!=i.moneyBefore()-i.rule().meso()) throw conflict();
        long used=0;Set<Long> ids=new HashSet<>();
        for(Stone s:i.stones()) {
            if(s.id()<=0 || !ids.add(s.id()) || s.id()==i.ringId() || s.remaining()<0
                    || Integer.parseInt(s.before().get("charid"))!=i.characterId()
                    || !i.rule().acceptsStone(Integer.parseInt(s.before().get("itemid")))) throw conflict();
            int before=Integer.parseInt(s.before().get("quantity"));if(before<=s.remaining())throw conflict();used+=before-s.remaining();
        }
        if(used!=i.rule().useCount() || Integer.parseInt(i.ringBefore().get("charid"))!=i.characterId()
                || Integer.parseInt(i.ringBefore().get("itemid"))!=i.rule().ringItemId())throw conflict();
    }
    private static SQLException conflict() { return new SQLException("Ring economy conflict: preserve recovery record and reload", "40001"); }
}
