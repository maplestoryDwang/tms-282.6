package net.swordie.ms.client.social.Guild;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Durable before/after redo. Existing character/money tables may be Aria: JDBC rollback is not assumed.
 * Callers must hold the shared economy monitor. Pending records block stale character saves until reload.
 */
public final class Tms282GuildJournal {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Set<Integer> BLOCKED = ConcurrentHashMap.newKeySet();
    // A preloaded Char can be absent from World during login/migration. Keep its save gate closed
    // after the durable intent retires, until that instance reloads membership at the entry hook.
    private static final Set<Integer> OFFLINE_DIRTY = ConcurrentHashMap.newKeySet();
    private static final Map<String,List<String>> KEYS=Map.of("guilds",List.of("id"),
            "guildgrades",List.of("guildid","graderole"), "guildmembers",List.of("charid"),
            "characters",List.of("id"), "characterstats",List.of("id","characterid"),
            "char_guild282_applications",List.of("charid","guildid"), "guild282_skill_levels",List.of("guild_id","skill_id"),
            "char_guild282_skill_member",List.of("charid"),"char_guild282_skill_uses",List.of("charid","skill_id"));
    private static final Map<String,List<String>> COLUMNS=Map.of(
            "guilds",List.of("id","name","leaderid","worldid","markbg","markbgcolor","mark","markcolor",
                    "maxmembers","notice","points","seasonpoints","allianceid","level","guildrank","ggp",
                    "appliable","joinsetting","reqlevel","bbsNotice","battleSp"),
            "guildgrades",List.of("guildid","graderole","gradename","gradepermission"),
            "guildmembers",List.of("charid","guildid","grade","alliancegrade","commitment","daycommitment",
                    "igp","name","job","level"),
            "characters",List.of("id","guild"),
            "characterstats",List.of("id","characterid","money"),
            "char_guild282_applications",List.of("charid","guildid","name","job","level","message","created_at","cancel_until","active"),
            "guild282_skill_levels",List.of("guild_id","skill_id","level","buyer","editor"),
            "char_guild282_skill_member",List.of("charid","guild_id","joined_at","date_source"),
            "char_guild282_skill_uses",List.of("charid","skill_id","guild_id","level","cooldown_until","buff_until"));
    public record Change(String table, Map<String,String> key, Map<String,String> before, Map<String,String> after) { }
    public record Intent(int characterId, String operation, int guildId, long moneyAfter, List<Change> changes) { }
    private final DataSource source;
    public Tms282GuildJournal() { this(null); }
    public Tms282GuildJournal(DataSource source) { this.source=source; }
    private Connection connection() throws SQLException { return source==null?DatabaseManager.getConnection():source.getConnection(); }
    public static boolean isBlocked(int id) { return BLOCKED.contains(id) || OFFLINE_DIRTY.contains(id); }

    /** Read-only deletion preflight. Caller keeps ECONOMY_MONITOR until character deletion finishes. */
    public boolean canDeleteCharacter(int id) throws SQLException {
        if(id<=0||isBlocked(id))return false;
        try(var c=connection()) {
            var character=read(c,"characters",map("id",id));
            if(character==null||number(character.get("guild"))!=0||read(c,"guildmembers",map("charid",id))!=null)return false;
            try(var s=c.prepareStatement("SELECT payload FROM char_guild282_pending WHERE character_id=?")) {
                s.setInt(1,id);try(var r=s.executeQuery()) { if(r.next())return false; }
            }
            return !Tms282GuildApplications.enabled()||applications(c,id,false).stream().noneMatch(Application::active);
        }
    }

    public Guild create(Char chr,String name,long cost) throws SQLException {
        if(cost<0 || chr.getMoney()<cost || chr.getGuild()!=null || chr.getGuildID()!=0) throw conflict();
        Intent intent=execute(chr,"create",0,name,cost);
        return refresh(chr,intent);
    }
    public Guild join(Char chr,Guild guild) throws SQLException {
        if(chr.getGuild()!=null || chr.getGuildID()!=0 || guild.getMembers().size()>=guild.getMaxMembers()) throw conflict();
        return refresh(chr,execute(chr,"join",guild.getId(),null,0));
    }
    public void leave(Char chr,Guild guild) throws SQLException {
        if(guild.getLeaderID()==chr.getId() || guild.getMemberByCharID(chr.getId())==null) throw conflict();
        refresh(chr,execute(chr,"leave",guild.getId(),null,0));
    }
    public void rank(Char chr,Guild guild,int rank) throws SQLException {
        if(rank<2||rank>10||guild.getLeaderID()==chr.getId()||guild.getMemberByCharID(chr.getId())==null)throw conflict();
        refresh(chr,execute(chr,"rank",guild.getId(),Integer.toString(rank),0));
    }

    /** Caller holds ECONOMY_MONITOR. Does not load/save an offline Char or access its money. */
    public Guild manageOffline(Char actor,int targetId,int guildId,int rank,boolean kick) throws SQLException {
        if(targetId<=0 || targetId==actor.getId() || actor.getWorld().getCharById(targetId)!=null
                || !BLOCKED.add(targetId))throw conflict();
        boolean committed=false;
        try {
            try(var c=connection()) {
                c.setAutoCommit(false);
                try {
                    if(pending(c,targetId)!=null) { committed=true;throw conflict(); }
                    try(var s=c.prepareStatement("SELECT character_id FROM char_guild282_pending WHERE guild_id=?")) {
                        s.setInt(1,guildId);try(var r=s.executeQuery()) { if(r.next())throw conflict(); }
                    }
                    var guild=read(c,"guilds",map("id",guildId));
                    var manager=read(c,"guildmembers",map("charid",actor.getId()));
                    var member=read(c,"guildmembers",map("charid",targetId));
                    var character=read(c,"characters",map("id",targetId));
                    if(guild==null || number(guild.get("worldid"))!=actor.getWorld().getWorldId()
                            || manager==null || number(manager.get("guildid"))!=guildId
                            || member==null || number(member.get("guildid"))!=guildId
                            || character==null || number(character.get("guild"))!=guildId)throw conflict();
                    int leaderId=number(guild.get("leaderid")),managerRank=number(manager.get("grade"));
                    var leader=read(c,"guildmembers",map("charid",leaderId));
                    var grade=read(c,"guildgrades",map("guildid",guildId,"graderole",managerRank));
                    if(leader==null || number(leader.get("guildid"))!=guildId || number(leader.get("grade"))!=1
                            || managerRank<1 || managerRank>10 || targetId==leaderId
                            || number(member.get("grade"))<=managerRank || number(member.get("grade"))>10
                            || actor.getId()!=leaderId && (grade==null || (number(grade.get("gradepermission"))&(kick?16:4))==0)
                            || !kick && (rank<=managerRank || rank>10)
                            || actor.getWorld().getCharById(targetId)!=null)throw conflict();
                    var afterCharacter=new LinkedHashMap<>(character);
                    if(kick)afterCharacter.put("guild",null);
                    Map<String,String> afterMember=null;
                    if(!kick) { afterMember=new LinkedHashMap<>(member);afterMember.put("grade",Integer.toString(rank)); }
                    // moneyAfter is not used by redo; load recovery always reads the actual persisted money.
                    var changes=new ArrayList<Change>(List.of(
                            new Change("guildmembers",map("charid",targetId),member,afterMember),
                            new Change("characters",map("id",targetId),character,afterCharacter)));
                    if(kick)membershipClock(c,changes,targetId,guildId,false,System.currentTimeMillis());
                    var intent=new Intent(targetId,kick?"leave":"rank",guildId,0,List.copyOf(changes));
                    validate(intent);
                    try(var s=c.prepareStatement("INSERT INTO char_guild282_pending (character_id,guild_id,world_id,guild_name,payload) VALUES (?,?,?,?,?)")) {
                        s.setInt(1,targetId);s.setInt(2,guildId);s.setInt(3,actor.getWorld().getWorldId());s.setString(4,null);
                        s.setString(5,JSON.writeValueAsString(intent));if(s.executeUpdate()!=1)throw conflict();
                    }
                    committed=true;OFFLINE_DIRTY.add(targetId);c.commit();
                } catch(Exception e) { c.rollback();throw sql(e); }
            }
            redo(targetId);
            Guild guild;
            try(var c=connection()) { guild=loadGuild(c,guildId); }
            if(guild==null)throw conflict();
            guild=publishGuild(actor.getWorld(),guild);
            // Only this successfully completed intent is unblocked; DIRTY still prevents stale saves.
            BLOCKED.remove(targetId);return guild;
        } catch(SQLException|RuntimeException e) { if(!committed)BLOCKED.remove(targetId);throw e; }
    }

    /** Entry-time repair for an instance loaded before an offline change. No monetary side effects. */
    public void refreshOfflineMembership(Char chr) throws SQLException {
        if(!OFFLINE_DIRTY.contains(chr.getId()))return;
        if(BLOCKED.contains(chr.getId()))throw conflict(); // unresolved redo must use beforeLoad recovery
        try(var c=connection()) {
            var row=read(c,"characters",map("id",chr.getId()));if(row==null)throw conflict();
            int guildId=number(row.get("guild"));
            Guild guild=guildId==0?null:loadGuild(c,guildId);
            if(guildId!=0 && (guild==null || guild.getWorldID()!=chr.getWorld().getWorldId()
                    || guild.getMemberByCharID(chr.getId())==null))throw conflict();
            if(guild!=null)guild=publishGuild(chr.getWorld(),guild);
            chr.setGuildID(guildId);chr.setGuild(guild);
            OFFLINE_DIRTY.remove(chr.getId());
        }
    }

    public record Application(int characterId,int guildId,String name,int job,int level,String message,
                              long createdAt,long cancelUntil,boolean active) { }
    public List<Application> applications(int id,boolean byGuild) throws SQLException {
        try(var c=connection()) { return applications(c,id,byGuild); }
    }
    private static List<Application> applications(Connection c,int id,boolean byGuild) throws SQLException {
        var result=new ArrayList<Application>();
        try(var s=c.prepareStatement("SELECT "+String.join(",",COLUMNS.get("char_guild282_applications"))
                +" FROM char_guild282_applications WHERE "+(byGuild?"guildid":"charid")+"=?")) {
            s.setInt(1,id);try(var r=s.executeQuery()) { while(r.next())result.add(new Application(r.getInt("charid"),r.getInt("guildid"),
                    r.getString("name"),r.getInt("job"),r.getInt("level"),r.getString("message"),r.getLong("created_at"),r.getLong("cancel_until"),r.getInt("active")!=0)); }
        }
        return List.copyOf(result);
    }
    public Guild applicationGuild(int worldId,int guildId) throws SQLException {
        Guild guild=findApplicationGuild(worldId,guildId);if(guild==null)throw conflict();return guild;
    }
    public Guild findApplicationGuild(int worldId,int guildId) throws SQLException {
        try(var c=connection()) {
            Guild guild=loadGuild(c,guildId);
            if(guild!=null&&guild.getWorldID()!=worldId)throw conflict();return guild;
        }
    }
    /** One applicant per redo intent. Approval clears every active application, not just this guild's row. */
    public void application(Char actor,int targetId,int guildId,String operation,String message,long now) throws SQLException {
        if(!Set.of("apply","withdraw","approve","reject","prune").contains(operation) || targetId<=0 || now<0
                || (operation.equals("apply")||operation.equals("withdraw")||operation.equals("prune")) && actor.getId()!=targetId
                || !BLOCKED.add(targetId))throw conflict();
        boolean committed=false;
        try {
            try(var c=connection()) {
                c.setAutoCommit(false);
                try {
                    if(pending(c,targetId)!=null) { committed=true;throw conflict(); }
                    try(var s=c.prepareStatement("SELECT character_id FROM char_guild282_pending WHERE guild_id=?")) {
                        s.setInt(1,guildId);try(var r=s.executeQuery()) { if(r.next())throw conflict(); }
                    }
                    var guild=read(c,"guilds",map("id",guildId));
                    var character=read(c,"characters",map("id",targetId));
                    var targetMember=read(c,"guildmembers",map("charid",targetId));
                    boolean prune=operation.equals("prune");
                    if(character==null || prune&&guild!=null
                            || !prune&&(guild==null||number(guild.get("worldid"))!=actor.getWorld().getWorldId())
                            || (operation.equals("apply")||operation.equals("approve"))&&(number(character.get("guild"))!=0||targetMember!=null))throw conflict();
                    int leaderId=prune?0:number(guild.get("leaderid"));
                    if(!prune) {
                        var leader=read(c,"guildmembers",map("charid",leaderId));
                        if(leader==null||number(leader.get("guildid"))!=guildId||number(leader.get("grade"))!=1)throw conflict();
                    }
                    if(operation.equals("approve")||operation.equals("reject")) {
                        var manager=read(c,"guildmembers",map("charid",actor.getId()));
                        if(manager==null||number(manager.get("guildid"))!=guildId)throw conflict();
                        int rank=number(manager.get("grade"));
                        var grade=read(c,"guildgrades",map("guildid",guildId,"graderole",rank));
                        if(rank<1||rank>10||actor.getId()!=leaderId&&(grade==null||(number(grade.get("gradepermission"))&64)==0))throw conflict();
                    }
                    var rows=applications(c,targetId,false);
                    var active=rows.stream().filter(Application::active).toList();
                    if(active.size()>5)throw conflict();
                    var key=map("charid",targetId,"guildid",guildId);
                    var before=read(c,"char_guild282_applications",key);
                    var changes=new ArrayList<Change>();
                    if(operation.equals("apply")) {
                        if(number(guild.get("appliable"))==0||actor.getGuildID()!=0||actor.getGuild()!=null
                                || actor.getLevel()<Math.max(101,number(guild.get("reqlevel")))
                                || message==null||message.length()>1000||message.indexOf('\0')>=0
                                || active.size()>=5 || active.stream().anyMatch(a->a.guildId()==guildId)
                                || rows.stream().anyMatch(a->a.cancelUntil()>now))throw conflict();
                        var after=map("charid",targetId,"guildid",guildId,"name",actor.getName(),"job",actor.getJob(),"level",actor.getLevel(),
                                "message",message,"created_at",now,"cancel_until",0,"active",1);
                        changes.add(new Change("char_guild282_applications",key,before,after));
                    } else {
                        if(before==null||number(before.get("active"))!=1)throw conflict();
                        if(operation.equals("approve")) {
                            // Native metadata is refreshed from the target's persisted profile, never a whole Char or money.
                            String name;int job,level;
                            try(var s=c.prepareStatement("SELECT name,job,level FROM characterstats WHERE characterid=?")) {
                                s.setInt(1,targetId);try(var r=s.executeQuery()) { if(!r.next())throw conflict();
                                    name=r.getString("name");job=r.getInt("job");level=r.getInt("level");if(r.next())throw conflict(); }
                            }
                            Char online=actor.getWorld().getCharById(targetId);
                            if(online!=null) {
                                if(online.getGuildID()!=0||online.getGuild()!=null)throw conflict();
                                name=online.getName();job=online.getJob();level=online.getLevel();
                            }
                            if(name==null||level<Math.max(101,number(guild.get("reqlevel"))))throw conflict();
                            try(var s=c.prepareStatement("SELECT COUNT(*) FROM guildmembers WHERE guildid=?")) {
                                s.setInt(1,guildId);try(var r=s.executeQuery()) { if(!r.next()||r.getInt(1)>=number(guild.get("maxmembers")))throw conflict(); }
                            }
                            for(Application a:active) {
                                var otherKey=map("charid",targetId,"guildid",a.guildId());
                                var old=read(c,"char_guild282_applications",otherKey);var after=new LinkedHashMap<>(old);after.put("active","0");
                                changes.add(new Change("char_guild282_applications",otherKey,old,after));
                            }
                            changes.add(new Change("guildmembers",map("charid",targetId),null,map("charid",targetId,"guildid",guildId,
                                    "grade",5,"alliancegrade",5,"commitment",0,"daycommitment",0,"igp",0,"name",name,"job",job,"level",level)));
                            var after=new LinkedHashMap<>(character);after.put("guild",Integer.toString(guildId));
                            changes.add(new Change("characters",map("id",targetId),character,after));
                            membershipClock(c,changes,targetId,guildId,true,now);
                        } else {
                            var after=new LinkedHashMap<>(before);after.put("active","0");
                            if(operation.equals("withdraw"))after.put("cancel_until",Long.toString(Math.addExact(now,60000)));
                            changes.add(new Change("char_guild282_applications",key,before,after));
                        }
                    }
                    var intent=new Intent(targetId,operation,guildId,0,List.copyOf(changes));validate(intent);
                    try(var s=c.prepareStatement("INSERT INTO char_guild282_pending (character_id,guild_id,world_id,guild_name,payload) VALUES (?,?,?,?,?)")) {
                        s.setInt(1,targetId);s.setInt(2,guildId);s.setInt(3,actor.getWorld().getWorldId());s.setString(4,null);
                        s.setString(5,JSON.writeValueAsString(intent));if(s.executeUpdate()!=1)throw conflict();
                    }
                    committed=true;
                    if(operation.equals("approve"))OFFLINE_DIRTY.add(targetId);
                    c.commit();
                } catch(Exception e) { c.rollback();throw sql(e); }
            }
            redo(targetId);
            if(operation.equals("approve")) {
                Guild guild=applicationGuild(actor.getWorld().getWorldId(),guildId);publishGuild(actor.getWorld(),guild);
            }
            BLOCKED.remove(targetId);
        } catch(SQLException|RuntimeException e) { if(!committed)BLOCKED.remove(targetId);throw e; }
    }

    /** Only the four original Noble skills spend SP. Existing owned legacy levels are adopted, never granted. */
    public void learnGuildSkill(Char actor,Guild guild,int skillId,int amount) throws SQLException {
        int owner=actor.getId(),guildId=guild.getId();
        if(!Tms282GuildSkills.enabled()||!Tms282GuildSkills.noble(skillId)||amount<1||amount>15||isBlocked(owner))throw conflict();
        BLOCKED.add(owner);boolean committed=false;
        try {
            try(var c=connection()) {
                c.setAutoCommit(false);
                try {
                    if(pending(c,owner)!=null){committed=true;throw conflict();}
                    try(var ps=c.prepareStatement("SELECT character_id FROM char_guild282_pending WHERE guild_id=?")) {
                        ps.setInt(1,guildId);try(var rs=ps.executeQuery()){if(rs.next())throw conflict();}
                    }
                    var persisted=read(c,"guilds",map("id",guildId));var character=read(c,"characters",map("id",owner));
                    var member=read(c,"guildmembers",map("charid",owner));
                    if(persisted==null||number(persisted.get("worldid"))!=actor.getWorld().getWorldId()||character==null
                            ||number(character.get("guild"))!=guildId||member==null||number(member.get("guildid"))!=guildId)throw conflict();
                    int rank=number(member.get("grade"));var grade=read(c,"guildgrades",map("guildid",guildId,"graderole",rank));
                    if(rank<1||rank>10||(rank==1)!=(number(persisted.get("leaderid"))==owner)
                            ||rank!=1&&(grade==null||(number(grade.get("gradepermission"))&256)==0))throw conflict();
                    var levels=Tms282GuildSkills.readRows(c,guildId);int spent=levels.values().stream().mapToInt(Tms282GuildSkills.Row::level).sum();
                    int budget=number(persisted.get("battleSp"));var current=levels.get(skillId);int cap=Tms282GuildSkills.data().get(skillId).maxLevel();
                    if(budget<0||spent>budget||amount>budget-spent||current.level()+amount>cap)throw conflict();
                    // Baseline adoption and pending intent are one InnoDB commit. A rollback cannot lose legacy ownership.
                    for(var row:levels.values()) {
                        var key=map("guild_id",guildId,"skill_id",row.skillId());
                        if(read(c,"guild282_skill_levels",key)==null)apply(c,new Change("guild282_skill_levels",key,null,
                                map("guild_id",guildId,"skill_id",row.skillId(),"level",row.level(),"buyer",row.buyer(),"editor",row.editor())));
                    }
                    var key=map("guild_id",guildId,"skill_id",skillId);var before=read(c,"guild282_skill_levels",key);
                    var after=new LinkedHashMap<>(before);after.put("level",Integer.toString(current.level()+amount));
                    if(current.level()==0)after.put("buyer",actor.getName());after.put("editor",actor.getName());
                    var intent=new Intent(owner,"guild-skill",guildId,0,List.of(
                            new Change("guilds",map("id",guildId),persisted,persisted),
                            new Change("guildmembers",map("charid",owner),member,member),
                            new Change("guild282_skill_levels",key,before,after)));
                    validate(intent);
                    try(var ps=c.prepareStatement("INSERT INTO char_guild282_pending (character_id,guild_id,world_id,guild_name,payload) VALUES (?,?,?,?,?)")) {
                        ps.setInt(1,owner);ps.setInt(2,guildId);ps.setInt(3,actor.getWorld().getWorldId());ps.setString(4,null);
                        ps.setString(5,JSON.writeValueAsString(intent));if(ps.executeUpdate()!=1)throw conflict();
                    }
                    committed=true;c.commit();
                }catch(Exception e){c.rollback();throw sql(e);}
            }
            redo(owner);Tms282GuildSkills.reload(guild);BLOCKED.remove(owner);
        }catch(SQLException|RuntimeException e){if(!committed)BLOCKED.remove(owner);throw e;}
    }

    /** CP32: transfer a standalone guild. Native130 changes the former master to rank3, successor to rank1. */
    public Guild transferLeadership(Char actor,Guild guild,int targetId) throws SQLException {
        int owner=actor.getId(),guildId=guild.getId();
        if(owner<=0||targetId<=0||owner==targetId||isBlocked(owner)||isBlocked(targetId))throw conflict();
        BLOCKED.add(owner);BLOCKED.add(targetId);boolean committed=false;
        try {
            Intent intent;
            try(var c=connection()) {
                c.setAutoCommit(false);
                try {
                    if(pending(c,owner)!=null||pending(c,targetId)!=null) { committed=true;throw conflict(); }
                    try(var ps=c.prepareStatement("SELECT character_id FROM char_guild282_pending WHERE guild_id=?")) {
                        ps.setInt(1,guildId);try(var rs=ps.executeQuery()) { if(rs.next())throw conflict(); }
                    }
                    var persisted=read(c,"guilds",map("id",guildId));
                    var beforeOwner=read(c,"guildmembers",map("charid",owner));
                    var beforeTarget=read(c,"guildmembers",map("charid",targetId));
                    var ownerChar=read(c,"characters",map("id",owner));var targetChar=read(c,"characters",map("id",targetId));
                    if(persisted==null||number(persisted.get("worldid"))!=actor.getWorld().getWorldId()
                            || number(persisted.get("leaderid"))!=owner||number(persisted.get("allianceid"))!=0
                            || ownerChar==null||targetChar==null||number(ownerChar.get("guild"))!=guildId||number(targetChar.get("guild"))!=guildId
                            || beforeOwner==null||beforeTarget==null||number(beforeOwner.get("guildid"))!=guildId||number(beforeTarget.get("guildid"))!=guildId
                            || number(beforeOwner.get("grade"))!=1||number(beforeTarget.get("grade"))<2||number(beforeTarget.get("grade"))>10)throw conflict();
                    int job,level;
                    try(var ps=c.prepareStatement("SELECT job,level FROM characterstats WHERE characterid=?")) {
                        ps.setInt(1,targetId);try(var rs=ps.executeQuery()) { if(!rs.next())throw conflict();job=rs.getInt("job");level=rs.getInt("level");if(rs.next())throw conflict(); }
                    }
                    Char online=actor.getWorld().getCharById(targetId);
                    if(online!=null) { job=online.getJob();level=online.getLevel(); }
                    if(!Tms282GuildRuntime.eligibleLeader(job,level))throw conflict();
                    var afterGuild=new LinkedHashMap<>(persisted);afterGuild.put("leaderid",Integer.toString(targetId));
                    var afterOwner=new LinkedHashMap<>(beforeOwner);afterOwner.put("grade","3");
                    var afterTarget=new LinkedHashMap<>(beforeTarget);afterTarget.put("grade","1");
                    intent=new Intent(owner,"transfer",guildId,0,List.of(
                            new Change("guilds",map("id",guildId),persisted,afterGuild),
                            new Change("guildmembers",map("charid",owner),beforeOwner,afterOwner),
                            new Change("guildmembers",map("charid",targetId),beforeTarget,afterTarget)));
                    validate(intent);
                    try(var ps=c.prepareStatement("INSERT INTO char_guild282_pending (character_id,guild_id,world_id,guild_name,payload) VALUES (?,?,?,?,?)")) {
                        ps.setInt(1,owner);ps.setInt(2,guildId);ps.setInt(3,actor.getWorld().getWorldId());ps.setString(4,null);
                        ps.setString(5,JSON.writeValueAsString(intent));if(ps.executeUpdate()!=1)throw conflict();
                    }
                    committed=true;OFFLINE_DIRTY.add(owner);OFFLINE_DIRTY.add(targetId);c.commit();
                }catch(Exception e){c.rollback();throw sql(e);}
            }
            redo(owner);return publishTransfer(actor,intent);
        }catch(SQLException|RuntimeException e){if(!committed){BLOCKED.remove(owner);BLOCKED.remove(targetId);}throw e;}
    }
    private Guild publishTransfer(Char context,Intent intent) throws SQLException {
        Guild guild=applicationGuild(context.getWorld().getWorldId(),intent.guildId());
        guild=publishGuild(context.getWorld(),guild);
        for(int id:transferParticipants(intent)) { OFFLINE_DIRTY.add(id);BLOCKED.remove(id); }
        for(int id:transferParticipants(intent)) {
            Char online=context.getWorld().getCharById(id);if(online!=null)refreshOfflineMembership(online);
        }
        return guild;
    }
    /** A successor or another guild member may log in before the initiating old master does. */
    private void recoverTransferForLoad(Char chr) throws SQLException {
        var transfers=new ArrayList<Intent>();
        try(var c=connection()) {
            var row=read(c,"characters",map("id",chr.getId()));int guildId=row==null?0:number(row.get("guild"));
            if(guildId==0)return;
            try(var ps=c.prepareStatement("SELECT character_id FROM char_guild282_pending WHERE guild_id=?")) {
                ps.setInt(1,guildId);try(var rs=ps.executeQuery()) { while(rs.next()) {
                    Intent intent=pending(c,rs.getInt(1));
                    if(intent!=null&&intent.operation().equals("transfer")) {
                        validate(intent);if(intent.guildId()!=guildId)throw conflict();transfers.add(intent);
                    }
                } }
            }
        }
        if(transfers.size()>1)throw conflict();
        for(Intent intent:transfers) {
            for(int id:transferParticipants(intent)){BLOCKED.add(id);OFFLINE_DIRTY.add(id);}
            redo(intent.characterId());publishTransfer(chr,intent);
        }
    }
    private static Set<Integer> transferParticipants(Intent intent) throws SQLException {
        var change=intent.changes().stream().filter(c->c.table().equals("guilds")).findFirst().orElseThrow(Tms282GuildJournal::conflict);
        return Set.of(intent.characterId(),number(change.after().get("leaderid")));
    }
    /** Only leaderid is owned by transfer. Concurrent notice/progress metadata must neither block redo nor be overwritten. */
    private static void redoLeadership(Connection c,Change change) throws SQLException {
        var actual=read(c,"guilds",change.key());if(actual==null)throw conflict();
        for(String column:List.of("id","worldid","allianceid"))if(!Objects.equals(actual.get(column),change.before().get(column)))throw conflict();
        if(Objects.equals(actual.get("leaderid"),change.after().get("leaderid")))return;
        if(!Objects.equals(actual.get("leaderid"),change.before().get("leaderid")))throw conflict();
        try(var ps=c.prepareStatement("UPDATE guilds SET leaderid=? WHERE id <=> ? AND worldid <=> ? AND allianceid <=> ? AND leaderid <=> ?")) {
            ps.setString(1,change.after().get("leaderid"));ps.setString(2,change.before().get("id"));ps.setString(3,change.before().get("worldid"));
            ps.setString(4,change.before().get("allianceid"));ps.setString(5,change.before().get("leaderid"));if(ps.executeUpdate()!=1)throw conflict();
        }
    }

    private Intent execute(Char chr,String operation,int guildId,String name,long cost) throws SQLException {
        int id=chr.getId(); if(id<=0 || !BLOCKED.add(id)) throw conflict();
        boolean committed=false;
        try {
            Intent intent;
            try(var c=connection()) {
                c.setAutoCommit(false);
                try {
                    if(pending(c,id)!=null) { committed=true; throw conflict(); }
                    if(operation.equals("create")) {
                        try(var s=c.prepareStatement("SELECT id FROM guilds WHERE worldid=? AND name=?")) {
                            s.setInt(1,chr.getWorld().getWorldId());s.setString(2,name);
                            try(var r=s.executeQuery()) { if(r.next())throw new SQLException("Guild name already exists"); }
                        }
                        guildId=reserveId(c);
                    }
                    var changes=new ArrayList<Change>();
                    var charKey=map("id",id); var character=read(c,"characters",charKey);
                    if(character==null)throw conflict();
                    int beforeGuild=number(character.get("guild"));
                    if(operation.equals("leave")||operation.equals("rank") ? beforeGuild!=guildId : beforeGuild!=0)throw conflict();
                    var statKey=map("id",chr.getAvatarData().getCharacterStat().getId(),"characterid",id);
                    var stats=read(c,"characterstats",statKey);
                    if(stats==null || Long.parseLong(stats.get("money"))!=chr.getMoney())throw conflict();
                    long moneyAfter=Math.subtractExact(chr.getMoney(),cost);
                    if(moneyAfter<0)throw conflict();
                    if(cost!=0) { var after=new LinkedHashMap<>(stats);after.put("money",Long.toString(moneyAfter));changes.add(new Change("characterstats",statKey,stats,after)); }
                    if(operation.equals("create")) {
                        var guild=map("id",guildId,"name",name,"leaderid",id,"worldid",chr.getWorld().getWorldId(),
                                "markbg",0,"markbgcolor",0,"mark",0,"markcolor",0,"maxmembers",20,"notice","",
                                "points",0,"seasonpoints",0,"allianceid",0,"level",1,"guildrank",0,"ggp",0,
                                "appliable",1,"joinsetting",0,"reqlevel",101,"bbsNotice",0,"battleSp",0);
                        changes.add(new Change("guilds",map("id",guildId),null,guild));
                        for(int rank=1;rank<=10;rank++) {
                            var key=map("guildid",guildId,"graderole",rank);
                            changes.add(new Change("guildgrades",key,null,map("guildid",guildId,"graderole",rank,
                                    "gradename",rank==1?"會長":rank==2?"副會長":"會員"+rank,"gradepermission",rank==1?-1:0)));
                        }
                    } else {
                        try(var s=c.prepareStatement("SELECT character_id FROM char_guild282_pending WHERE guild_id=?")) {
                            s.setInt(1,guildId);try(var r=s.executeQuery()) { if(r.next())throw conflict(); }
                        }
                        var persistedGuild=read(c,"guilds",map("id",guildId));
                        if(persistedGuild==null || number(persistedGuild.get("worldid"))!=chr.getWorld().getWorldId())throw conflict();
                        var leader=read(c,"guildmembers",map("charid",number(persistedGuild.get("leaderid"))));
                        if(leader==null || number(leader.get("guildid"))!=guildId || number(leader.get("grade"))!=1)throw conflict();
                        if(operation.equals("leave") && number(persistedGuild.get("leaderid"))==id)throw conflict();
                        if(operation.equals("join")) {
                            try(var s=c.prepareStatement("SELECT COUNT(*) FROM guildmembers WHERE guildid=?")) {
                                s.setInt(1,guildId);try(var r=s.executeQuery()) {
                                    if(!r.next() || r.getInt(1)>=number(persistedGuild.get("maxmembers")))throw conflict();
                                }
                            }
                        }
                    }
                    var memberKey=map("charid",id); var member=read(c,"guildmembers",memberKey);
                    if(operation.equals("leave")) {
                        if(member==null || number(member.get("guildid"))!=guildId)throw conflict();
                        changes.add(new Change("guildmembers",memberKey,member,null));
                    } else if(operation.equals("rank")) {
                        if(member==null || number(member.get("guildid"))!=guildId || number(member.get("grade"))==1)throw conflict();
                        int rank=Integer.parseInt(name);if(rank<2||rank>10)throw conflict();
                        var ranked=new LinkedHashMap<>(member);ranked.put("grade",name);
                        changes.add(new Change("guildmembers",memberKey,member,ranked));
                    } else {
                        if(member!=null)throw conflict();
                        changes.add(new Change("guildmembers",memberKey,null,map("charid",id,"guildid",guildId,
                                "grade",operation.equals("create")?1:5,"alliancegrade",5,"commitment",0,
                                "daycommitment",0,"igp",0,"name",chr.getName(),"job",chr.getJob(),"level",chr.getLevel())));
                    }
                    var after=new LinkedHashMap<>(character); after.put("guild",operation.equals("leave")?null:Integer.toString(guildId));
                    changes.add(new Change("characters",charKey,character,after));
                    if(Tms282GuildApplications.enabled() && (operation.equals("create")||operation.equals("join"))) {
                        for(Application a:applications(c,id,false))if(a.active()) {
                            var appKey=map("charid",id,"guildid",a.guildId());var old=read(c,"char_guild282_applications",appKey);
                            var closed=new LinkedHashMap<>(old);closed.put("active","0");
                            changes.add(new Change("char_guild282_applications",appKey,old,closed));
                        }
                    }
                    if(!operation.equals("rank"))membershipClock(c,changes,id,guildId,!operation.equals("leave"),System.currentTimeMillis());
                    intent=new Intent(id,operation,guildId,moneyAfter,List.copyOf(changes));
                    validate(intent);
                    try(var s=c.prepareStatement("INSERT INTO char_guild282_pending (character_id,guild_id,world_id,guild_name,payload) VALUES (?,?,?,?,?)")) {
                        s.setInt(1,id);s.setInt(2,guildId);s.setInt(3,chr.getWorld().getWorldId());s.setString(4,operation.equals("create")?name:null);
                        s.setString(5,JSON.writeValueAsString(intent));if(s.executeUpdate()!=1)throw conflict();
                    }
                    committed=true; c.commit();
                } catch(Exception e) { c.rollback();throw sql(e); }
            }
            redo(id);return intent;
        } catch(SQLException|RuntimeException e) { if(!committed)BLOCKED.remove(id);throw e; }
    }

    /** Membership date is part of the same redo as membership, never contribution time. */
    private static void membershipClock(Connection c,List<Change> changes,int charId,int guildId,boolean joining,long now) throws SQLException {
        if(!Tms282GuildSkills.enabled())return;
        var key=map("charid",charId);var before=read(c,"char_guild282_skill_member",key);
        var after=joining?map("charid",charId,"guild_id",guildId,"joined_at",now,"date_source",0):null;
        if(before!=null||after!=null)changes.add(new Change("char_guild282_skill_member",key,before,after));
    }

    /** Sequence reservation and pending intent commit together; a crashed allocation is never reused. */
    private static int reserveId(Connection c) throws SQLException {
        try(var s=c.prepareStatement("INSERT IGNORE INTO guild282_sequence (id,next_id) VALUES (1,1)")) { s.executeUpdate(); }
        long next;
        try(var s=c.prepareStatement("SELECT next_id FROM guild282_sequence WHERE id=1 FOR UPDATE");var r=s.executeQuery()) {
            if(!r.next())throw conflict();next=r.getLong(1);
        }
        try(var s=c.prepareStatement("SELECT MAX(id) FROM guilds");var r=s.executeQuery()) {
            if(!r.next())throw conflict();next=Math.max(next,r.getLong(1)+1);
        }
        try(var s=c.prepareStatement("SELECT AUTO_INCREMENT FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='guilds'");var r=s.executeQuery()) {
            if(!r.next())throw conflict();next=Math.max(next,r.getLong(1));
        }
        if(next<=0 || next>=Integer.MAX_VALUE)throw conflict();
        try(var s=c.prepareStatement("UPDATE guild282_sequence SET next_id=? WHERE id=1")) { s.setLong(1,next+1);if(s.executeUpdate()!=1)throw conflict(); }
        return (int)next;
    }

    public void redo(int characterId) throws SQLException {
        try(var c=connection()) {
            c.setAutoCommit(false);
            try {
                Intent intent=pending(c,characterId);
                if(intent==null) { c.commit();return; }
                validate(intent);
                for(Change change:intent.changes()) {
                    if(intent.operation().equals("transfer")&&change.table().equals("guilds")) { redoLeadership(c,change);continue; }
                    if(intent.operation().equals("guild-skill")&&change.table().equals("guilds")) {
                        var actual=read(c,"guilds",change.key());if(actual==null)throw conflict();
                        for(String field:List.of("id","worldid","battleSp"))if(!Objects.equals(actual.get(field),change.before().get(field)))throw conflict();
                        continue; // budget dependency only; unrelated notice/progress is not overwritten
                    }
                    var actual=read(c,change.table(),change.key());
                    if(Objects.equals(actual,change.after()))continue;
                    if(!Objects.equals(actual,change.before()))throw conflict();
                    apply(c,change);
                }
                try(var s=c.prepareStatement("DELETE FROM char_guild282_pending WHERE character_id=?")) {
                    s.setInt(1,characterId);if(s.executeUpdate()!=1)throw conflict();
                }
                c.commit();
            } catch(Exception e) { c.rollback();throw sql(e); }
        }
    }

    public void recoverForLoad(Char chr) throws SQLException {
        BLOCKED.add(chr.getId());recoverTransferForLoad(chr);redo(chr.getId());
        try(var c=connection()) {
            var row=read(c,"characters",map("id",chr.getId()));
            var money=read(c,"characterstats",map("id",chr.getAvatarData().getCharacterStat().getId(),"characterid",chr.getId()));
            if(row==null || money==null)throw conflict();
            int id=number(row.get("guild"));
            refresh(chr,new Intent(chr.getId(),id==0?"leave":"join",id,Long.parseLong(money.get("money")),List.of()));
        }
    }

    private Guild refresh(Char chr,Intent intent) throws SQLException {
        Guild guild;
        try(var c=connection()) { guild=intent.guildId()==0?null:loadGuild(c,intent.guildId()); }
        if(intent.guildId()!=0 && guild==null)throw conflict();
        if(!intent.operation().equals("leave") && guild.getMemberByCharID(chr.getId())==null)throw conflict();
        if(guild!=null)guild=publishGuild(chr.getWorld(),guild);
        if(intent.operation().equals("leave")) { chr.setGuildID(0);chr.setGuild(null); }
        else { chr.setGuildID(guild.getId());chr.setGuild(guild); }
        Tms282GuildSkills.reload(guild);
        chr.getAvatarData().getCharacterStat().setMoney(intent.moneyAfter());
        OFFLINE_DIRTY.remove(chr.getId());BLOCKED.remove(chr.getId());return guild;
    }

    private static Guild publishGuild(net.swordie.ms.world.World world,Guild guild) {
        if(guild!=null) {
            Guild existing=world.getGuilds().get(guild.getId());
            if(existing!=null) {
                existing.setMembers(guild.getMembers());existing.setLeaderID(guild.getLeaderID());
                for(int rank=1;rank<=10;rank++)existing.setGrade(rank,guild.gradeName(rank),guild.gradePermission(rank));
                guild=existing;
            }
            world.addGuild(guild);
            for(GuildMember member:guild.getMembers()) {
                Char online=world.getCharById(member.getCharID());
                member.setOnline(online!=null);
                if(online!=null) { online.setGuildID(guild.getId());online.setGuild(guild); }
            }
        }
        return guild;
    }

    /** Legacy loaders catch SQL failures and return partial objects. Recovery must fail closed instead. */
    private static Guild loadGuild(Connection c,int id) throws SQLException {
        var row=read(c,"guilds",map("id",id));if(row==null)return null;
        Guild guild=new Guild();guild.setId(id);guild.setName(row.get("name"));guild.setLeaderID(number(row.get("leaderid")));
        guild.setWorldID(number(row.get("worldid")));guild.setMaxMembers(number(row.get("maxmembers")));guild.setNotice(row.get("notice"));
        guild.setMarkBg(number(row.get("markbg")));guild.setMarkBgColor(number(row.get("markbgcolor")));
        guild.setMark(number(row.get("mark")));guild.setMarkColor(number(row.get("markcolor")));
        guild.setLevel(number(row.get("level")));guild.setRank(number(row.get("guildrank")));guild.setHonorEXP(number(row.get("points")));
        guild.setSeasonPoints(number(row.get("seasonpoints")));guild.setAllianceID(number(row.get("allianceid")));
        guild.setGgp(number(row.get("ggp")));guild.setBattleSp(number(row.get("battleSp")));guild.setAppliable(number(row.get("appliable"))!=0);
        guild.setJoinSetting(number(row.get("joinsetting")));guild.setReqLevel(number(row.get("reqlevel")));
        Set<Integer> ranks=new HashSet<>();
        try(var s=c.prepareStatement("SELECT graderole,gradename,gradepermission FROM guildgrades WHERE guildid=?")) {
            s.setInt(1,id);try(var r=s.executeQuery()) {
                while(r.next()) { int rank=r.getInt("graderole");if(rank<1||rank>10||!ranks.add(rank))throw conflict();
                    guild.setGrade(rank,r.getString("gradename"),r.getInt("gradepermission")); }
            }
        }
        Set<Integer> members=new HashSet<>();
        try(var s=c.prepareStatement("SELECT id,charid,guildid,grade,alliancegrade,commitment,daycommitment,igp,name,job,level,commitmentinctime FROM guildmembers WHERE guildid=?")) {
            s.setInt(1,id);try(var r=s.executeQuery()) {
                while(r.next()) {
                    var member=new GuildMember();member.setId(r.getInt("id"));member.setCharID(r.getInt("charid"));member.setGuildID(id);
                    member.setRank(r.getInt("grade"));member.setAllianceRank(r.getInt("alliancegrade"));member.setContribution(r.getInt("commitment"));
                    member.setDayContribution(r.getInt("daycommitment"));member.setIGP(r.getInt("igp"));member.setName(r.getString("name"));
                    member.setJob(r.getInt("job"));member.setLevel(r.getInt("level"));
                    try { member.setContributionIncTime(DatabaseManager.getFileTimeFromString(r.getString("commitmentinctime"))); }
                    catch(java.text.ParseException invalid) { throw new SQLException("Invalid guild member timestamp",invalid); }
                    if(!members.add(member.getCharID()))throw conflict();guild.getMembers().add(member);
                }
            }
        }
        if(guild.getGuildLeader()==null || guild.getGuildLeader().getRank()!=1)throw conflict();
        return guild;
    }

    private static Intent pending(Connection c,int id) throws SQLException {
        try(var s=c.prepareStatement("SELECT payload FROM char_guild282_pending WHERE character_id=? FOR UPDATE")) {
            s.setInt(1,id);try(var r=s.executeQuery()) {
                if(!r.next())return null;
                try { var intent=JSON.readValue(r.getString(1),Intent.class);if(intent.characterId()!=id)throw conflict();return intent; }
                catch(java.io.IOException e) { throw new SQLException("Invalid guild recovery intent",e); }
            }
        }
    }
    private static Map<String,String> read(Connection c,String table,Map<String,String> key) throws SQLException {
        List<String> columns=COLUMNS.get(table);if(columns==null || !key.keySet().equals(new HashSet<>(KEYS.get(table))))throw conflict();
        try(var s=c.prepareStatement("SELECT "+String.join(",",columns)+" FROM "+table+" WHERE "+where(KEYS.get(table)))) {
            bind(s,1,KEYS.get(table),key);try(var r=s.executeQuery()) {
                if(!r.next())return null;var row=new LinkedHashMap<String,String>();
                for(String column:columns)row.put(column,r.getString(column));
                if(r.next())throw conflict();return row;
            }
        }
    }
    private static void apply(Connection c,Change change) throws SQLException {
        List<String> columns=COLUMNS.get(change.table());String query;
        if(change.before()==null) query="INSERT INTO "+change.table()+" ("+String.join(",",columns)+") VALUES ("+String.join(",",Collections.nCopies(columns.size(),"?"))+")";
        else if(change.after()==null) query="DELETE FROM "+change.table()+" WHERE "+where(columns);
        else query="UPDATE "+change.table()+" SET "+String.join(",",columns.stream().map(x->x+"=?").toList())+" WHERE "+where(columns);
        try(var s=c.prepareStatement(query)) {
            int n=1;if(change.after()!=null)n=bind(s,n,columns,change.after());
            if(change.before()!=null)bind(s,n,columns,change.before());
            if(s.executeUpdate()!=1)throw conflict();
        }
    }
    private static int bind(PreparedStatement statement,int n,List<String> columns,Map<String,String> row) throws SQLException {
        for(String column:columns)statement.setString(n++,row.get(column));return n;
    }
    private static String where(List<String> columns) { return String.join(" AND ",columns.stream().map(x->x+" <=> ?").toList()); }
    private static Map<String,String> map(Object... pairs) {
        var map=new LinkedHashMap<String,String>();for(int i=0;i<pairs.length;i+=2)map.put((String)pairs[i],pairs[i+1]==null?null:pairs[i+1].toString());return map;
    }
    private static int number(String value) { return value==null?0:Integer.parseInt(value); }
    private static void validate(Intent intent) throws SQLException {
        if(intent.characterId()<=0 || intent.guildId()<=0 || intent.moneyAfter()<0 || !Set.of("create","join","leave","rank","apply","withdraw","approve","reject","prune","transfer","guild-skill").contains(intent.operation())
                || intent.changes()==null || intent.changes().size()<(Set.of("apply","withdraw","reject","prune").contains(intent.operation())?1:2) || intent.changes().size()>20)throw conflict();
        Set<String> unique=new HashSet<>();
        for(Change change:intent.changes()) {
            var columns=COLUMNS.get(change.table());var keys=KEYS.get(change.table());
            if(columns==null || !change.key().keySet().equals(new HashSet<>(keys)) || change.before()==null&&change.after()==null
                    || !unique.add(change.table()+change.key()))throw conflict();
            for(var row:Arrays.asList(change.before(),change.after())) {
                if(row==null)continue;
                if(!row.keySet().equals(new HashSet<>(columns)))throw conflict();
                for(String key:keys)if(!Objects.equals(row.get(key),change.key().get(key)))throw conflict();
            }
            if(Set.of("characters","guildmembers").contains(change.table()) && number(change.key().get(change.table().equals("characters")?"id":"charid"))!=intent.characterId()
                    && !(intent.operation().equals("transfer")&&transferParticipants(intent).contains(number(change.key().get("charid")))))throw conflict();
            if(change.table().equals("char_guild282_applications") && number(change.key().get("charid"))!=intent.characterId())throw conflict();
            if(Set.of("apply","withdraw","reject","prune").contains(intent.operation()) && !change.table().equals("char_guild282_applications"))throw conflict();
            if(intent.operation().equals("approve") && !Set.of("char_guild282_applications","characters","guildmembers","char_guild282_skill_member").contains(change.table()))throw conflict();
            if(change.table().equals("characterstats") && number(change.key().get("characterid"))!=intent.characterId())throw conflict();
            if(Set.of("char_guild282_skill_member","char_guild282_skill_uses").contains(change.table()) && number(change.key().get("charid"))!=intent.characterId())throw conflict();
            if(change.table().equals("guild282_skill_levels") && number(change.key().get("guild_id"))!=intent.guildId())throw conflict();
            if(change.table().equals("guilds") && number(change.key().get("id"))!=intent.guildId())throw conflict();
            if(change.table().equals("guildgrades") && number(change.key().get("guildid"))!=intent.guildId())throw conflict();
        }
        if(intent.operation().equals("transfer"))validateTransfer(intent);
        if(intent.operation().equals("guild-skill"))validateSkill(intent);
    }
    private static void validateSkill(Intent intent) throws SQLException {
        if(intent.moneyAfter()!=0||intent.changes().size()!=3)throw conflict();
        int skillRows=0,guildRows=0,memberRows=0;
        for(Change change:intent.changes()) {
            if(change.before()==null||change.after()==null)throw conflict();
            if(change.table().equals("guild282_skill_levels")) {
                skillRows++;int id=number(change.key().get("skill_id")),before=number(change.before().get("level")),after=number(change.after().get("level"));
                if(!Tms282GuildSkills.noble(id)||before<0||after<=before||after>Tms282GuildSkills.data().get(id).maxLevel())throw conflict();
                for(String key:change.before().keySet())if(!Set.of("level","buyer","editor").contains(key)
                        &&!Objects.equals(change.before().get(key),change.after().get(key)))throw conflict();
                if(before>0&&!Objects.equals(change.before().get("buyer"),change.after().get("buyer")))throw conflict();
            } else {
                if(!change.before().equals(change.after()))throw conflict();
                if(change.table().equals("guilds"))guildRows++;
                else if(change.table().equals("guildmembers")&&number(change.before().get("guildid"))==intent.guildId())memberRows++;
                else throw conflict();
            }
        }
        if(skillRows!=1||guildRows!=1||memberRows!=1)throw conflict();
    }
    private static void validateTransfer(Intent intent) throws SQLException {
        if(intent.moneyAfter()!=0||intent.changes().size()!=3)throw conflict();
        var guildChange=intent.changes().stream().filter(c->c.table().equals("guilds")).findFirst().orElseThrow(Tms282GuildJournal::conflict);
        if(guildChange.before()==null||guildChange.after()==null||number(guildChange.before().get("leaderid"))!=intent.characterId()
                || number(guildChange.before().get("allianceid"))!=0)throw conflict();
        Set<Integer> participants=transferParticipants(intent);
        if(participants.size()!=2||participants.stream().anyMatch(id->id<=0))throw conflict();
        for(Change change:intent.changes()) {
            if(change.before()==null||change.after()==null)throw conflict();
            String mutable=change.table().equals("guilds")?"leaderid":"grade";
            if(!change.table().equals("guilds")) {
                if(!change.table().equals("guildmembers")||!participants.contains(number(change.key().get("charid")))
                        || number(change.before().get("guildid"))!=intent.guildId())throw conflict();
                boolean old=number(change.key().get("charid"))==intent.characterId();
                if(old?number(change.before().get("grade"))!=1||number(change.after().get("grade"))!=3
                        :number(change.before().get("grade"))<2||number(change.before().get("grade"))>10||number(change.after().get("grade"))!=1)throw conflict();
            }
            for(String column:COLUMNS.get(change.table()))if(!column.equals(mutable)&&!Objects.equals(change.before().get(column),change.after().get(column)))throw conflict();
        }
    }
    private static SQLException sql(Exception e) { return e instanceof SQLException s?s:new SQLException(e); }
    private static SQLException conflict() { return new SQLException("Guild persistence conflict; preserve pending intent and reload", "40001"); }
}
