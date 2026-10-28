package net.swordie.ms.client.social.Guild;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.connection.packet.*;
import net.swordie.ms.enums.BaseStat;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Original282 guild skills. Independent gate until schema, stat integration and cast authority are installed. */
public final class Tms282GuildSkills {
    private static final boolean ENABLED=Boolean.parseBoolean(System.getProperty("tms282.guild.skills.enabled","false"));
    private static final Tms282GuildJournal JOURNAL=new Tms282GuildJournal();
    private static final Map<Integer,State> STATES=new ConcurrentHashMap<>();
    private static final Map<Char,Stamp> RECENT=new WeakHashMap<>();
    private record Stamp(int skill,int amount,long time) { }
    public record Row(int skillId,int level,String buyer,String editor) { }
    public record Cast(int characterId,int guildId,int skillId,int level,long cooldownUntil,long buffUntil) { }
    private record State(int budget,Map<Integer,Row> rows,Map<Integer,Long> joined,Map<Integer,List<Cast>> casts) { }
    public static final long JOIN_WAIT_MILLIS=1440L*60_000L; // original Etc_002.wz/guildCommon.img
    private static final class DataHolder {
        static final Tms282GuildSkillData DATA=load();
        static final Set<Integer> KNOWN_MAPS=new HashSet<>(),LEARNING_BLOCKED=new HashSet<>();
        static {try {
            var maps=new com.fasterxml.jackson.databind.ObjectMapper().readTree(Path.of("data/protocol/tms282.3/guild-skill-maps.json").toFile());
            maps.path("known").forEach(n->KNOWN_MAPS.add(n.asInt()));maps.path("forbidden").forEach(n->LEARNING_BLOCKED.add(n.asInt()));
            if(KNOWN_MAPS.isEmpty()||!KNOWN_MAPS.containsAll(LEARNING_BLOCKED))throw new java.io.IOException("Invalid native map catalogue");
        }catch(java.io.IOException e){throw new IllegalStateException("Original guild learning restrictions unavailable",e);}}
        private static Tms282GuildSkillData load(){try{return new Tms282GuildSkillData(Path.of("data/protocol/tms282.3/guild-skills.json"));}
            catch(java.io.IOException e){throw new IllegalStateException("Original guild skill data unavailable",e);}}
    }
    private Tms282GuildSkills() { }
    public static boolean enabled(){return ENABLED&&Tms282GuildRuntime.enabled();}
    public static Tms282GuildSkillData data(){return DataHolder.DATA;}
    public static boolean noble(int id){return Tms282GuildSkillData.NOBLE.contains(id);}
    /** Native9695B0 is the learning predicate, not the Noble-buff use restriction. */
    public static boolean canLearnInMap(int mapId){return DataHolder.KNOWN_MAPS.contains(mapId)&&!DataHolder.LEARNING_BLOCKED.contains(mapId);}
    /** Strictly read existing owned legacy levels, then overlay the authoritative282 rows. No automatic grants. */
    static Map<Integer,Row> readRows(Connection c,int guildId) throws SQLException {
        Map<Integer,Row> values=new HashMap<>();Set<Integer> authoritative=new HashSet<>();
        try(var ps=c.prepareStatement("SELECT skill_id,level,buyer,editor FROM guild282_skill_levels WHERE guild_id=?")) {
            ps.setInt(1,guildId);try(var rs=ps.executeQuery()){while(rs.next()){
                int id=rs.getInt("skill_id");if(!noble(id)||!authoritative.add(id))throw new SQLException("Invalid guild skill identity");
                values.put(id,checked(id,rs.getInt("level"),rs.getString("buyer"),rs.getString("editor")));
            }}
        }
        if(authoritative.size()<4)try(var links=c.prepareStatement("SELECT skillid FROM guildskills WHERE guild_id=?")) {
            links.setInt(1,guildId);try(var rs=links.executeQuery()){while(rs.next()){
                try(var skill=c.prepareStatement("SELECT skillid,level,buycharactername,extendcharactername FROM guildskill WHERE id=?")) {
                    skill.setInt(1,rs.getInt("skillid"));try(var row=skill.executeQuery()){
                        if(!row.next())throw new SQLException("Dangling legacy guild skill link");int id=row.getInt("skillid");
                        if(noble(id)&&!authoritative.contains(id)) {
                            Row value=checked(id,row.getInt("level"),row.getString("buycharactername"),row.getString("extendcharactername"));
                            if(values.putIfAbsent(id,value)!=null)throw new SQLException("Duplicate legacy guild skill");
                        }
                        if(row.next())throw new SQLException("Nonunique legacy guild skill");
                    }
                }
            }}
        }
        for(int id:Tms282GuildSkillData.NOBLE)values.putIfAbsent(id,new Row(id,0,"",""));return Map.copyOf(values);
    }
    private static Row checked(int id,int level,String buyer,String editor) throws SQLException {
        if(level<0||level>data().get(id).maxLevel())throw new SQLException("Stored guild skill exceeds original282 data cap");
        return new Row(id,level,buyer==null?"":buyer,editor==null?"":editor);
    }
    /** Called under the economy monitor after durable change/recovery; hot stat getters never query SQL. */
    static void reload(Guild guild) throws SQLException {
        if(!enabled()||guild==null)return;
        try(var c=DatabaseManager.getConnection()) {
            int budget;
            try(var ps=c.prepareStatement("SELECT battleSp FROM guilds WHERE id=?")) {
                ps.setInt(1,guild.getId());try(var rs=ps.executeQuery()){if(!rs.next())throw new SQLException("Guild missing");budget=rs.getInt("battleSp");}
            }
            if(budget<0)throw new SQLException("Invalid stored guild SP budget");
            var rows=readRows(c,guild.getId());observeLegacyMemberships(c,guild.getId());
            var joined=new HashMap<Integer,Long>();var casts=new HashMap<Integer,List<Cast>>();
            try(var ps=c.prepareStatement("SELECT charid,joined_at FROM char_guild282_skill_member WHERE guild_id=?")) {
                ps.setInt(1,guild.getId());try(var rs=ps.executeQuery()){while(rs.next()) {
                    long since=rs.getLong("joined_at");if(since<=0||joined.put(rs.getInt("charid"),since)!=null)throw new SQLException("Invalid guild join date");
                }}
            }
            try(var ps=c.prepareStatement("SELECT charid,skill_id,level,cooldown_until,buff_until FROM char_guild282_skill_uses WHERE guild_id=?")) {
                ps.setInt(1,guild.getId());try(var rs=ps.executeQuery()){while(rs.next()) {
                    int id=rs.getInt("skill_id"),level=rs.getInt("level"),charId=rs.getInt("charid");
                    if(!noble(id)||level<1||level>data().get(id).maxLevel())throw new SQLException("Invalid saved guild cast");
                    casts.computeIfAbsent(charId,k->new ArrayList<>()).add(new Cast(charId,guild.getId(),id,level,rs.getLong("cooldown_until"),rs.getLong("buff_until")));
                }}
            }
            STATES.put(guild.getId(),new State(budget,rows,Map.copyOf(joined),Map.copyOf(casts)));guild.setBattleSp(budget);
        }
    }
    /** Single InnoDB insert per unknown legacy membership, not a fictional historical join date.
     * Caller holds ECONOMY_MONITOR; no Aria row changes and no Char/money save. Retry never advances the anchor.
     */
    private static void observeLegacyMemberships(Connection c,int guildId) throws SQLException {
        var ids=new ArrayList<Integer>();
        try(var ps=c.prepareStatement("SELECT charid FROM guildmembers WHERE guildid=?")) {
            ps.setInt(1,guildId);try(var rs=ps.executeQuery()){while(rs.next())ids.add(rs.getInt("charid"));}
        }
        for(int id:ids) {
            try(var ps=c.prepareStatement("SELECT guild_id FROM char_guild282_skill_member WHERE charid=?")) {
                ps.setInt(1,id);try(var rs=ps.executeQuery()){if(rs.next()){if(rs.getInt("guild_id")!=guildId)throw new SQLException("Conflicting guild membership clock");continue;}}
            }
            try(var ps=c.prepareStatement("INSERT INTO char_guild282_skill_member (charid,guild_id,joined_at,date_source) VALUES (?,?,?,?)")) {
                ps.setInt(1,id);ps.setInt(2,guildId);ps.setLong(3,System.currentTimeMillis());ps.setInt(4,1);
                if(ps.executeUpdate()!=1)throw new SQLException("Could not observe legacy membership");
            }
        }
    }
    public static int level(Guild guild,int skillId) {
        if(!enabled()||guild==null)return 0;var state=STATES.get(guild.getId());var levels=new HashMap<Integer,Integer>();
        if(state!=null)state.rows.forEach((id,row)->levels.put(id,row.level()));
        return data().level(guild.getLevel(),skillId,levels);
    }
    private static Guild owned(Char chr){Guild guild=chr==null?null:chr.getGuild();return enabled()&&guild!=null&&guild.getId()==chr.getGuildID()&&guild.getMemberByCharID(chr.getId())!=null?guild:null;}
    public static long joinedAt(Guild guild,int charId) {if(!enabled()||guild==null)return 0;State s=STATES.get(guild.getId());return s==null?0:s.joined.getOrDefault(charId,0L);}
    /** Restores only this membership's durable effects; a leave/rejoin cannot revive an older proposal. */
    public static List<Cast> restorable(Char chr,long now) {
        Guild g=owned(chr);if(g==null)return List.of();State s=STATES.get(g.getId());long joined=joinedAt(g,chr.getId());
        if(s==null||joined<=0)return List.of();
        return s.casts.getOrDefault(chr.getId(),List.of()).stream().filter(c->c.cooldownUntil()-300_000L>=joined
                &&(c.cooldownUntil()>now||c.buffUntil()>now)).toList();
    }
    public static Map<BaseStat,Integer> baseStats(Char chr){Guild g=owned(chr);return g==null?Map.of():data().baseStats(g.getLevel());}
    public static int value(Char chr,int skillId,String stat){Guild g=owned(chr);return g==null?0:data().value(g.getLevel(),skillId,stat);}
    public static int huntingMesoBonus(Char chr,boolean huntingDrop){Guild g=owned(chr);return g==null?0:data().mesoBonus(g.getLevel(),huntingDrop);}
    public static int experiencePercent(Char chr,int mobLevel,boolean boss){Guild g=owned(chr);return g==null?0:data().experiencePercent(g.getLevel(),mobLevel,boss);}
    public static int shopDiscountPercent(Char chr,double buyPrice,double sellPrice){Guild g=owned(chr);return g==null?0:data().shopDiscount(g.getLevel(),buyPrice,sellPrice);}
    public static List<Tms282GuildPackets.Skill> presentation(Guild guild) {
        if(!enabled())return guild.getSkills().values().stream().map(s->new Tms282GuildPackets.Skill(s.getSkillID(),s.getLevel(),s.getExpireDate()==null?0:s.getExpireDate().toLong(),text(s.getBuyCharacterName()),text(s.getExtendCharacterName()))).toList();
        var result=new ArrayList<Tms282GuildPackets.Skill>();var state=STATES.get(guild.getId());
        for(int id:data().definitions().keySet().stream().sorted().toList()) {
            int level=level(guild,id);if(level==0)continue;Row row=state==null?null:state.rows.get(id);
            result.add(new Tms282GuildPackets.Skill(id,level,Tms282GuildSkillPackets.EMPTY_TIME,row==null?"":row.buyer(),row==null?"":row.editor()));
        }
        return List.copyOf(result);
    }
    public static void sendPoints(Char chr){if(enabled()){Guild g=owned(chr);State s=g==null?null:STATES.get(g.getId());chr.write(Tms282GuildSkillPackets.points(s==null?0:s.budget));}}
    public static void learn(Char actor,int skillId,int amount,Charset charset) throws SQLException {
        learn(actor,skillId,amount,charset,System.nanoTime()/1_000_000L);
    }
    static void learn(Char actor,int skillId,int amount,Charset charset,long now) throws SQLException {
        if(!enabled())throw new IllegalArgumentException("Native guild skills are not enabled");
        synchronized(Tms282VMatrixRuntime.ECONOMY_MONITOR) {
            Tms282VMatrixRuntime.requireEconomicAccess(actor);Guild guild=owned(actor);
            if(guild==null||!guild.hasPermission(actor.getId(),256)||!noble(skillId)||amount<1||amount>data().get(skillId).maxLevel()
                    ||actor.getHP()<=0||actor.getField()==null||!canLearnInMap(actor.getField().getId())
                    ||actor.getTradeRoom()!=null||actor.getMiniRoom()!=null)throw new IllegalArgumentException("Guild skill learning is unavailable");
            Stamp recent=RECENT.get(actor);
            if(recent!=null&&now-recent.time()<500){
                if(recent.skill()!=skillId||recent.amount()!=amount)throw new IllegalArgumentException("Wait for the previous guild skill request");
                Tms282GuildRuntime.sendMembership(actor,1,charset);return;
            }
            JOURNAL.learnGuildSkill(actor,guild,skillId,amount);RECENT.put(actor,new Stamp(skillId,amount,now));
            for(var member:List.copyOf(guild.getMembers())){Char chr=actor.getWorld().getCharById(member.getCharID());if(chr!=null)sendCurrent(chr,guild,skillId,actor.getId(),charset);}
        }
    }
    private static void sendCurrent(Char chr,Guild guild,int id,int editorId,Charset charset) {
        Row row=STATES.get(guild.getId()).rows.get(id);
        chr.write(Tms282GuildSkillPackets.changed(guild.getId(),id,editorId,row.level(),row.buyer(),row.editor(),charset));sendPoints(chr);
    }
    private static String text(String s){return s==null?"":s;}
}
