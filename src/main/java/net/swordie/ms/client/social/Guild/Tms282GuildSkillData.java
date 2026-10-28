package net.swordie.ms.client.social.Guild;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.swordie.ms.client.character.skills.SkillStat;
import net.swordie.ms.enums.BaseStat;
import java.nio.file.*;
import java.util.*;

/** Manifest-backed original282 Skill/9100.img. No265 skill IDs, caps or pricing are inferred. */
public final class Tms282GuildSkillData {
    public static final Set<Integer> NOBLE=Set.of(91001022,91001023,91001024,91001025);
    public record Definition(int id,int maxLevel,int guildLevel,boolean disabled,boolean passive,
                             Map<Integer,Integer> prerequisites,Map<String,String> values) {
        public int value(String name,int level) {
            if(level<=0||level>maxLevel)return 0;
            String expression=values.get(name);if(expression==null)return 0;
            if(expression.matches("-?[0-9]+"))return Integer.parseInt(expression);
            if(expression.matches("[0-9]+\\*x"))return Math.multiplyExact(Integer.parseInt(expression.substring(0,expression.length()-2)),level);
            throw new IllegalArgumentException("Unsupported original guild skill expression: "+expression);
        }
    }
    private final Map<Integer,Definition> definitions;
    public Tms282GuildSkillData(Path path) throws java.io.IOException {
        var rows=new LinkedHashMap<Integer,Definition>();JsonNode root=new ObjectMapper().readTree(path.toFile());
        for(JsonNode node:root.path("skills")) {
            int id=node.path("id").asInt();var values=new LinkedHashMap<String,String>();
            node.path("common").fields().forEachRemaining(e->values.put(e.getKey(),e.getValue().asText()));
            var req=new LinkedHashMap<Integer,Integer>();node.path("req").fields().forEachRemaining(e->{if(e.getKey().matches("[0-9]+"))req.put(Integer.parseInt(e.getKey()),Integer.parseInt(e.getValue().asText()));});
            var def=new Definition(id,Integer.parseInt(values.getOrDefault("maxLevel","0")),node.path("req").path("reqGuildLv").asInt(0),
                    node.path("flags").path("disable").asInt(0)!=0,node.path("flags").path("psd").asInt(0)!=0,Map.copyOf(req),Map.copyOf(values));
            if(id/10000!=9100||def.maxLevel()<1||def.maxLevel()>255||NOBLE.contains(id)&&def.maxLevel()!=15||rows.put(id,def)!=null)throw new java.io.IOException("Invalid original guild skill catalogue");
        }
        if(!rows.keySet().containsAll(NOBLE))throw new java.io.IOException("Missing original Noble skills");definitions=Map.copyOf(rows);
    }
    public Map<Integer,Definition> definitions(){return definitions;}
    public Definition get(int id){return definitions.get(id);}
    public int level(int guildLevel,int skillId,Map<Integer,Integer> learned) {
        return level(guildLevel,skillId,learned,new HashSet<>());
    }
    private int level(int guildLevel,int skillId,Map<Integer,Integer> learned,Set<Integer> seen) {
        Definition def=get(skillId);
        if(guildLevel<1||def==null||def.disabled()||def.guildLevel()>guildLevel||!seen.add(skillId))return 0;
        for(var req:def.prerequisites().entrySet())if(level(guildLevel,req.getKey(),learned,new HashSet<>(seen))<req.getValue())return 0;
        if(NOBLE.contains(skillId))return Math.min(def.maxLevel(),Math.max(0,learned.getOrDefault(skillId,0)));
        return def.guildLevel()>0&&def.maxLevel()==1?1:0;
    }
    /** Flat stat contributions only. Damage reduction/EXP/meso/shop/ARC/stars have dedicated call sites. */
    public Map<BaseStat,Integer> baseStats(int guildLevel) {
        var stats=new EnumMap<BaseStat,Integer>(BaseStat.class);
        for(var def:definitions.values()) {
            int level=level(guildLevel,def.id(),Map.of());if(level==0||!def.passive())continue;
            for(String key:List.of("padX","madX","strX","dexX","intX","lukX","mhpX","nbdR")) {
                int value=def.value(key,level);if(value!=0)stats.merge(SkillStat.valueOf(key).getBaseStat(),value,Integer::sum);
            }
        }
        return Map.copyOf(stats);
    }
    public int value(int guildLevel,int skillId,String stat) {var def=get(skillId);return def==null?0:def.value(stat,level(guildLevel,skillId,Map.of()));}
    public int experiencePercent(int guildLevel,int mobLevel,boolean boss) {
        Definition def=get(91000027);int level=level(guildLevel,def.id(),Map.of());
        return boss||level==0||mobLevel<def.value("y",level)||mobLevel>def.value("z",level)?0:def.value("expGuild",level);
    }
    public int mesoBonus(int guildLevel,boolean huntingDrop){return huntingDrop?value(guildLevel,91000011,"mesoG"):0;}
    public int shopDiscount(int guildLevel,double buyPrice,double sellPrice){return buyPrice>0&&sellPrice>=0&&sellPrice<buyPrice*0.7?value(guildLevel,91000008,"disCountR"):0;}
}
