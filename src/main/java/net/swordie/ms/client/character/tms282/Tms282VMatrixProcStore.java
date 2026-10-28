package net.swordie.ms.client.character.tms282;

import net.swordie.ms.connection.hikariCP.DatabaseManager;
import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

/** Special-core cooldown authority, separate from legacy delete-on-login skillcooltimes. */
public final class Tms282VMatrixProcStore {
    private final DataSource source;
    public Tms282VMatrixProcStore(){this(null);}
    public Tms282VMatrixProcStore(DataSource source){this.source=source;}
    private Connection connection()throws SQLException{return source==null?DatabaseManager.getConnection():source.getConnection();}
    public Map<Integer,Long> load(int character)throws SQLException{
        if(character<=0)throw new IllegalArgumentException("Invalid character");
        Map<Integer,Long> result=new HashMap<>();
        try(var c=connection();var s=c.prepareStatement("SELECT core_id,ready_at FROM character_vmatrix282_special_cd WHERE char_id=?")){
            s.setInt(1,character);try(var rows=s.executeQuery()){
                while(rows.next()){
                    int core=rows.getInt(1);long ready=rows.getLong(2);
                    if(core<=0 || ready<0 || result.put(core,ready)!=null)throw new SQLException("Invalid special-core cooldown authority");
                }
            }
        }
        return Map.copyOf(result);
    }
    /** Commit the proc reservation BEFORE applying its effect. Unknown commit outcome must not apply an effect. */
    public boolean reserve(int character,int core,long now,long cooldown)throws SQLException{
        if(character<=0 || core<=0 || now<0 || cooldown<=0)throw new IllegalArgumentException("Invalid proc reservation");
        long next=Math.addExact(now,cooldown);
        try(var c=connection()){
            c.setAutoCommit(false);
            try{
                try(var s=c.prepareStatement("INSERT IGNORE INTO character_vmatrix282_special_cd (char_id,core_id,ready_at) VALUES (?,?,0)")){
                    s.setInt(1,character);s.setInt(2,core);s.executeUpdate();
                }
                long ready;
                try(var s=c.prepareStatement("SELECT ready_at FROM character_vmatrix282_special_cd WHERE char_id=? AND core_id=? FOR UPDATE")){
                    s.setInt(1,character);s.setInt(2,core);try(var rows=s.executeQuery()){
                        if(!rows.next())throw new SQLException("Missing special cooldown lock row");ready=rows.getLong(1);
                        if(ready<0 || rows.next())throw new SQLException("Invalid special cooldown lock row");
                    }
                }
                if(ready>now){c.commit();return false;}
                try(var s=c.prepareStatement("UPDATE character_vmatrix282_special_cd SET ready_at=? WHERE char_id=? AND core_id=?")){
                    s.setLong(1,next);s.setInt(2,character);s.setInt(3,core);
                    if(s.executeUpdate()!=1)throw new SQLException("Special cooldown reservation lost");
                }
                c.commit();return true;
            }catch(SQLException|RuntimeException e){c.rollback();throw e;}
        }
    }
}
