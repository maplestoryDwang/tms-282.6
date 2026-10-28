package net.swordie.ms.client.character.tms282;

import com.fasterxml.jackson.databind.ObjectMapper;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.*;
import java.util.*;

/** Dedicated InnoDB authority. No schema auto-creation and no writes to legacy Aria inventories.
 * An economic grant must be coordinated by a separate durable item journal before using this store. */
public final class Tms282MyHomeStore {
    private static final ObjectMapper JSON=new ObjectMapper();
    private final DataSource source;
    private final Tms282MyHomeRules rules;
    public Tms282MyHomeStore(Tms282MyHomeRules rules){this.source=null;this.rules=Objects.requireNonNull(rules);}
    public Tms282MyHomeStore(DataSource source,Tms282MyHomeRules rules) { this.source=Objects.requireNonNull(source);this.rules=Objects.requireNonNull(rules); }
    Connection connection()throws SQLException{return source==null?net.swordie.ms.connection.hikariCP.DatabaseManager.getConnection():source.getConnection();}
    public Optional<Tms282MyHomeState> load(int accountId,int worldId) throws SQLException {
        try(var c=connection()){return load(c,accountId,worldId,false);}
    }
    Optional<Tms282MyHomeState> load(Connection c,int accountId,int worldId,boolean lock)throws SQLException {
        Tms282MyHomeState.requireOwner(accountId,worldId);
        try(var p=c.prepareStatement(
                "SELECT revision,state_json FROM account_myhome282 WHERE account_id=? AND world_id=?"+(lock?" FOR UPDATE":""))) {
            p.setInt(1,accountId);p.setInt(2,worldId);
            try(var r=p.executeQuery()) {
                if(!r.next())return Optional.empty();
                try {
                    var state=JSON.readValue(r.getString(2),Tms282MyHomeState.class);
                    state.requireOwnerMatches(accountId,worldId);
                    rules.validate(state);
                    if(state.revision()!=r.getLong(1))throw new SQLException("Home revision mismatch");
                    return Optional.of(state);
                } catch(IOException|IllegalArgumentException|SecurityException e){throw new SQLException("Invalid stored home",e);}
            }
        }
    }
    /** Only the quest/reward owner may call this. A duplicate owner key does not overwrite existing state. */
    public boolean create(Tms282MyHomeState initial) throws SQLException {
        if(initial.revision()!=0)throw new IllegalArgumentException("Initial revision must be zero");
        return write(initial,null);
    }
    /** Optimistic revision check rejects stale sessions and retry races, scoped by both account and world. */
    public boolean save(Tms282MyHomeState before,Tms282MyHomeState after) throws SQLException {
        after.requireOwnerMatches(before.accountId(),before.worldId());
        if(after.revision()!=Math.addExact(before.revision(),1))throw new IllegalArgumentException("Invalid home revision transition");
        return write(after,before.revision());
    }
    private boolean write(Tms282MyHomeState state,Long previous) throws SQLException {
        rules.validate(state);
        final String body;
        try{body=JSON.writeValueAsString(state);}catch(IOException e){throw new SQLException("Home encode failed",e);}
        if(body.length()>200000)throw new SQLException("Home state exceeds storage bound");
        String sql=previous==null
                ? "INSERT INTO account_myhome282(account_id,world_id,revision,state_json) VALUES(?,?,?,?)"
                : "UPDATE account_myhome282 SET revision=?,state_json=? WHERE account_id=? AND world_id=? AND revision=?";
        try(var c=connection()) {
            c.setAutoCommit(false);
            try(var p=c.prepareStatement(sql)) {
                if(previous!=null){
                    load(c,state.accountId(),state.worldId(),true);
                    if(Tms282MyHomeJournal.pending(c,state.accountId(),state.worldId())!=null)
                        throw new SQLException("Home inventory registration is awaiting recovery","40001");
                }
                if(previous==null){p.setInt(1,state.accountId());p.setInt(2,state.worldId());p.setLong(3,state.revision());p.setString(4,body);}
                else {p.setLong(1,state.revision());p.setString(2,body);p.setInt(3,state.accountId());p.setInt(4,state.worldId());p.setLong(5,previous);}
                int affected=p.executeUpdate();
                if(affected!=1){c.rollback();return false;}c.commit();return true;
            } catch(SQLException e) {
                c.rollback();
                if(previous==null && e.getErrorCode()==1062)return false;
                throw e;
            } catch(RuntimeException e){c.rollback();throw e;}
        }
    }
    /** Called only while the journal holds this home's row and pending intent locks. */
    void grant(Connection c,Tms282MyHomeState before,Tms282MyHomeState after)throws SQLException {
        rules.validate(after);after.requireOwnerMatches(before.accountId(),before.worldId());
        if(after.revision()!=Math.addExact(before.revision(),1))throw new SQLException("Invalid grant revision");
        try(var p=c.prepareStatement("UPDATE account_myhome282 SET revision=?,state_json=? WHERE account_id=? AND world_id=? AND revision=?")) {
            p.setLong(1,after.revision());p.setString(2,JSON.writeValueAsString(after));p.setInt(3,before.accountId());p.setInt(4,before.worldId());p.setLong(5,before.revision());
            if(p.executeUpdate()!=1)throw new SQLException("Home grant revision conflict","40001");
        }catch(IOException e){throw new SQLException(e);}
    }
}
