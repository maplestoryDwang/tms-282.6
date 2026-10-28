package net.swordie.ms.client.character.tms282;

import java.sql.*;
import java.util.*;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.hexa.HexaSkill;
import net.swordie.ms.client.character.skills.Skill;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.connection.packet.WvsContext;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.constants.SixthJobSkillConstants;
import net.swordie.ms.loaders.Etc.HexaCore.HexaCore;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.loaders.Tms282SkillRequirements;

/** GM advancement policy: one native Origin core at least Lv1; no mastery/boost/stat cores or inferred H2. */
public final class Tms282GmOrigin {
    private Tms282GmOrigin() { }
    private record Plan(HexaSkill core,List<Skill> skills,boolean changed) { }

    /** Strict core/skill transaction. The caller must still save and verify the surrounding quest/character flow. */
    public static boolean ensure(Char chr) {
        if(!Tms282SkillLearning.enabled())return false;
        if(chr==null||chr.getId()<=0||!chr.isGM())throw new IllegalStateException("Origin initialization requires a saved GM character");
        boolean[] changed={false};
        EconomyLocks.packet(chr,()->{changed[0]=ensureLocked(chr);return null;});
        return changed[0];
    }

    private static boolean ensureLocked(Char chr) {
        if(chr.getLevel()<260||!JobConstants.isReadyForFifthJob(chr.getJob(),chr.getSubJob())
                ||!chr.hasQuestCompleted(1465)||!chr.hasQuestCompleted(1488)||!chr.hasQuestCompleted(38625))
            throw new IllegalStateException("Origin initialization requires completed fifth/sixth advancement");
        var entry=SixthJobSkillConstants.getOriginEntryForJob(chr.getJob());
        var definition=entry==null?null:HexaCore.getSkillCoreData(entry.originCoreId());
        if(definition==null||definition.getMaxLevel()<=0||HexaCore.getSkillCoreType(entry.originCoreId())!=1
                ||!HexaCore.hasSkillCoreByJob(1,entry.originCoreId(),chr.getJob()))
            throw new IllegalStateException("No native Origin core for job "+chr.getJob());
        var ids=new LinkedHashSet<>(definition.getConnectSkills());ids.add(entry.originSkillId());
        for(int id:ids){
            Skill skill=SkillData.getSkillDeepCopyById(id);
            if(!Tms282SkillRequirements.hasOriginalSkill(id)||skill==null||skill.getMaxLevel()<=0)
                throw new IllegalStateException("Missing native Origin skill definition "+id);
            Skill old=chr.getSkill(id);
            if(old!=null&&old.getCharId()!=0&&old.getCharId()!=chr.getId())throw new IllegalStateException("Origin skill owner mismatch");
        }
        HexaSkill owned=null;
        for(HexaSkill core:chr.getHexaSkills())if(core.getSkillId()==entry.originCoreId()){
            if(owned!=null)throw new IllegalStateException("Duplicate in-memory Origin core");owned=core;
        }
        if(owned!=null&&(owned.getCharId()!=0&&owned.getCharId()!=chr.getId()
                ||owned.getSkillLevel()<0||owned.getSkillLevel()>definition.getMaxLevel()))
            throw new IllegalStateException("Invalid in-memory Origin core");
        Plan plan=null;Connection con=null;SQLException failure=null;boolean committed=false;
        try{
            con=DatabaseManager.getConnection();con.setAutoCommit(false);
            // Serialize this character's initialization even when another process retries it.
            try(PreparedStatement p=con.prepareStatement("SELECT id FROM characters WHERE id=? FOR UPDATE")){
                p.setInt(1,chr.getId());try(ResultSet r=p.executeQuery()){if(!r.next())throw new SQLException("Character row is absent");}
            }
            plan=write(con,chr,entry.originCoreId(),definition.getMaxLevel(),owned,ids);
            con.commit();committed=true;
        }catch(SQLException e){failure=e;}
        finally{
            if(con!=null){
                if(!committed)try{con.rollback();}catch(SQLException e){if(failure==null)failure=e;else failure.addSuppressed(e);}
                try{con.close();}catch(SQLException e){if(failure==null)failure=e;else failure.addSuppressed(e);}
            }
        }
        if(committed){
            HexaSkill core=plan.core();
            if(owned!=null)chr.getHexaSkills().remove(owned);
            chr.getHexaSkills().add(core);
            List<Skill> notify=new ArrayList<>();
            for(Skill skill:plan.skills()){
                Skill old=chr.getSkill(skill.getSkillId());boolean differs=!same(old,skill);
                int savedId=skill.getId();Tms282SkillLearning.putWithStats(chr,skill);skill.setId(savedId);
                if(differs)notify.add(skill);
            }
            if(!notify.isEmpty())chr.write(WvsContext.changeSkillRecordResult(notify,true,false,false));
            if(plan.changed())chr.write(WvsContext.hexaSkillsUpdate(chr));
        }
        // A close failure after a successful commit must not revert memory to the old, uncommitted state.
        if(failure!=null)throw new IllegalStateException(committed?"Origin committed; connection cleanup failed, retry safely":"Origin initialization was not saved",failure);
        return plan.changed();
    }

    private static Plan write(Connection con,Char chr,int coreId,int maximum,HexaSkill owned,Set<Integer> ids)throws SQLException {
        HexaSkill stored=null;
        try(PreparedStatement p=con.prepareStatement("SELECT id,slv,enabled,disabled FROM hexaskills WHERE charid=? AND skillid=? FOR UPDATE")){
            p.setInt(1,chr.getId());p.setInt(2,coreId);
            try(ResultSet r=p.executeQuery()){while(r.next()){
                if(stored!=null)throw new SQLException("Duplicate persisted Origin core");
                stored=new HexaSkill(coreId,r.getInt("slv"));stored.setId(r.getInt("id"));
                stored.setEnabled(r.getBoolean("enabled"));stored.setDiabled(r.getBoolean("disabled"));
            }}
        }
        if(stored!=null&&(stored.getId()<=0||stored.getSkillLevel()<0||stored.getSkillLevel()>maximum))throw new SQLException("Invalid persisted Origin core");
        if(owned!=null&&owned.getId()>0&&(stored==null||owned.getId()!=stored.getId()))throw new SQLException("Origin row identity changed");
        int level=Math.max(1,Math.max(owned==null?0:owned.getSkillLevel(),stored==null?0:stored.getSkillLevel()));
        HexaSkill target=new HexaSkill(coreId,level);target.setCharId(chr.getId());
        boolean changed=owned==null||owned.getSkillLevel()!=level||!owned.isEnabled()||owned.isDiabled();
        if(stored==null){
            target.setId(insert(con,"INSERT INTO hexaskills (charid,skillid,slv,enabled,disabled) VALUES (?,?,?,1,0)",chr.getId(),coreId,level));changed=true;
        }else{
            target.setId(stored.getId());
            if(stored.getSkillLevel()!=level||!stored.isEnabled()||stored.isDiabled()){
                update(con,"UPDATE hexaskills SET slv=?,enabled=1,disabled=0 WHERE id=? AND charid=? AND skillid=?",level,stored.getId(),chr.getId(),coreId);changed=true;
            }
            if(owned!=null&&owned.getId()!=stored.getId())changed=true;
        }
        List<Skill> skills=new ArrayList<>();
        for(int id:ids){
            Skill old=chr.getSkill(id),db=null;
            try(PreparedStatement p=con.prepareStatement("SELECT id,rootid,maxlevel,currentlevel,masterlevel FROM skills WHERE charid=? AND skillid=? FOR UPDATE")){
                p.setInt(1,chr.getId());p.setInt(2,id);
                try(ResultSet r=p.executeQuery()){while(r.next()){
                    if(db!=null)throw new SQLException("Duplicate persisted Origin skill "+id);
                    db=new Skill();db.setId(r.getInt("id"));db.setSkillId(id);db.setRootId(r.getInt("rootid"));db.setMaxLevel(r.getInt("maxlevel"));db.setCurrentLevel(r.getInt("currentlevel"));db.setMasterLevel(r.getInt("masterlevel"));
                }}
            }
            if(db!=null&&db.getId()<=0)throw new SQLException("Invalid persisted Origin skill ID");
            if(old!=null&&old.getId()>0&&(db==null||old.getId()!=db.getId()))throw new SQLException("Origin skill row identity changed "+id);
            Skill skill=SkillData.getSkillDeepCopyById(id);skill.setCharId(chr.getId());
            skill.setCurrentLevel(Math.max(Math.min(level,skill.getMaxLevel()),Math.max(old==null?0:old.getCurrentLevel(),db==null?0:db.getCurrentLevel())));
            skill.setMaxLevel(Math.max(skill.getMaxLevel(),Math.max(old==null?0:old.getMaxLevel(),db==null?0:db.getMaxLevel())));
            skill.setMasterLevel(Math.max(skill.getCurrentLevel(),Math.max(skill.getMasterLevel(),Math.max(old==null?0:old.getMasterLevel(),db==null?0:db.getMasterLevel()))));
            if(db==null){
                skill.setId(insert(con,"INSERT INTO skills (charid,skillid,rootid,maxlevel,currentlevel,masterlevel) VALUES (?,?,?,?,?,?)",chr.getId(),id,skill.getRootId(),skill.getMaxLevel(),skill.getCurrentLevel(),skill.getMasterLevel()));changed=true;
            }else{
                skill.setId(db.getId());
                if(!same(db,skill)){
                    update(con,"UPDATE skills SET rootid=?,maxlevel=?,currentlevel=?,masterlevel=? WHERE id=? AND charid=? AND skillid=?",skill.getRootId(),skill.getMaxLevel(),skill.getCurrentLevel(),skill.getMasterLevel(),db.getId(),chr.getId(),id);changed=true;
                }
            }
            if(!same(old,skill))changed=true;
            skills.add(skill);
        }
        return new Plan(target,List.copyOf(skills),changed);
    }
    private static boolean same(Skill a,Skill b){return a!=null&&a.getId()==b.getId()&&a.getRootId()==b.getRootId()&&a.getMaxLevel()==b.getMaxLevel()&&a.getCurrentLevel()==b.getCurrentLevel()&&a.getMasterLevel()==b.getMasterLevel();}
    private static void bind(PreparedStatement p,int... values)throws SQLException{for(int i=0;i<values.length;i++)p.setInt(i+1,values[i]);}
    private static int insert(Connection con,String sql,int... values)throws SQLException{
        try(PreparedStatement p=con.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)){
            bind(p,values);if(p.executeUpdate()!=1)throw new SQLException("Origin insert did not write one row");
            try(ResultSet r=p.getGeneratedKeys()){if(!r.next()||r.getInt(1)<=0)throw new SQLException("Origin insert has no row ID");return r.getInt(1);}
        }
    }
    private static void update(Connection con,String sql,int... values)throws SQLException{
        try(PreparedStatement p=con.prepareStatement(sql)){bind(p,values);if(p.executeUpdate()!=1)throw new SQLException("Origin update did not write one row");}
    }
}
