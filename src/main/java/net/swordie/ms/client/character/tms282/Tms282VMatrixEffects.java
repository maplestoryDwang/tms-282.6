package net.swordie.ms.client.character.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.SkillStat;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;
import net.swordie.ms.connection.packet.Tms282SkillPackets;
import net.swordie.ms.loaders.SkillData;
import java.util.*;

/** Applies only the installed282 special-core effects; runtime owns activation and proc cooldowns. */
public final class Tms282VMatrixEffects {
    private record Buff(SkillStat field,CharacterTemporaryStat stat) { }
    private static final Map<Integer,Buff> BUFFS=Map.ofEntries(
            Map.entry(400007002,new Buff(SkillStat.indieDamR,CharacterTemporaryStat.IndieDamR)),
            Map.entry(400007003,new Buff(SkillStat.indieCr,CharacterTemporaryStat.IndieCrR)),
            Map.entry(400007005,new Buff(SkillStat.indieIgnoreMobpdpR,CharacterTemporaryStat.IndieIgnoreMobpdpR)),
            Map.entry(400007006,new Buff(SkillStat.indieNotDamaged,CharacterTemporaryStat.IndieNotDamaged)),
            Map.entry(400007007,new Buff(SkillStat.indieExp,CharacterTemporaryStat.IndieEXP)),
            Map.entry(400007008,new Buff(SkillStat.indieBDR,CharacterTemporaryStat.IndieBDR)),
            Map.entry(400007010,new Buff(SkillStat.indieDamR,CharacterTemporaryStat.IndieDamR)),
            Map.entry(400007011,new Buff(SkillStat.indieExp,CharacterTemporaryStat.IndieEXP)),
            Map.entry(400007013,new Buff(SkillStat.indieDamR,CharacterTemporaryStat.IndieDamR)),
            Map.entry(400007014,new Buff(SkillStat.indieBDR,CharacterTemporaryStat.IndieBDR)),
            Map.entry(400007015,new Buff(SkillStat.indieBDR,CharacterTemporaryStat.IndieBDR)),
            Map.entry(400007016,new Buff(SkillStat.indieIgnoreMobpdpR,CharacterTemporaryStat.IndieIgnoreMobpdpR)),
            Map.entry(400007017,new Buff(SkillStat.indieCr,CharacterTemporaryStat.IndieCrR)));
    private static final Map<Integer,Integer> REDUCTIONS=Map.of(400007102,30,400007103,20,400007106,25,400007107,20);
    private Tms282VMatrixEffects() { }
    public static boolean source(int id){return BUFFS.containsKey(id)||REDUCTIONS.containsKey(id)||id==400007104;}
    /** Exact source+CTS admission to the existing traced local indie codec; no global CTS whitelist. */
    public static boolean supports(CharacterTemporaryStat stat,Option option){
        Buff buff=BUFFS.get(option.nReason);return buff!=null && buff.stat()==stat;
    }
    public static EnumMap<CharacterTemporaryStat,Option> buffs(int skill,int level){
        Buff buff=BUFFS.get(skill);var info=SkillData.getSkillInfoById(skill);
        if(buff==null || info==null || level!=1 || info.getMaxLevel()<level)throw new IllegalArgumentException("Unsupported282 special buff");
        int value=info.getValue(buff.field(),level),seconds=info.getValue(SkillStat.time,level);
        if(value<=0 || seconds<=0)throw new IllegalArgumentException("Missing282 special buff values");
        Option option=new Option();option.nReason=skill;option.nValue=value;option.tTerm=seconds;
        var result=new EnumMap<CharacterTemporaryStat,Option>(CharacterTemporaryStat.class);result.put(buff.stat(),option);return result;
    }
    public static void apply(Char chr,Tms282VMatrixSpecialEngine.Effect effect){
        Objects.requireNonNull(chr);Objects.requireNonNull(effect);
        if(Protocol.active().version()!=ProtocolVersion.TMS_282_3)throw new IllegalStateException("282 effect in another profile");
        switch(effect.type()){
            case SELF_BUFF -> chr.getTemporaryStatManager().sendStat(buffs(effect.skillId(),effect.skillLevel()));
            case HEAL -> {
                if(effect.skillId()!=400007104 || effect.percent()!=100)throw new IllegalArgumentException("Unknown282 heal effect");
                long amount=(long)chr.getMaxHP()*effect.percent()/100;
                chr.heal((int)Math.min(Integer.MAX_VALUE,amount));
            }
            case REDUCE_COOLDOWNS -> {
                if(!Objects.equals(REDUCTIONS.get(effect.skillId()),effect.percent()))throw new IllegalArgumentException("Unknown282 cooldown effect");
                long now=System.currentTimeMillis();
                Map<Integer,Integer> updated=new LinkedHashMap<>();
                for(var entry:new HashMap<>(chr.getSkillCoolTimes()).entrySet()){
                    int skill=entry.getKey();long remaining=Math.max(0,entry.getValue()-now);
                    var info=SkillData.getSkillInfoById(skill);
                    // Special-core proc timers are persisted by runtime and must not shorten themselves/each other.
                    if(source(skill)||remaining==0||info==null||info.isNotCooltimeReset())continue;
                    long reduction=remaining/100*effect.percent()+remaining%100*effect.percent()/100;
                    if(reduction>0){
                        chr.reduceSkillCoolTime(skill,reduction);
                        long left=Math.max(0,chr.getSkillCoolTimes().get(skill)-System.currentTimeMillis());
                        updated.put(skill,(int)Math.min(Integer.MAX_VALUE,(left+999)/1000));
                    }
                }
                // Legacy reduceSkillCoolTime emits SKILL_COOLTIME_SET_M, which282 intentionally drops.
                if(!updated.isEmpty())chr.write(Tms282SkillPackets.skillCooltimeSet(updated));
            }
        }
    }
    /** Runtime calls this when a core expires or is deselected; normal TSM timers handle buff duration. */
    public static void remove(Char chr,int source){
        if(Protocol.active().version()==ProtocolVersion.TMS_282_3 && BUFFS.containsKey(source))
            chr.getTemporaryStatManager().removeStatsBySkill(source);
    }
}
