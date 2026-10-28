package net.swordie.ms.client.jobs.flora;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.SkillStat;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.tms282.Tms282BuffBook;
import net.swordie.ms.connection.packet.Tms282RemoteBuffs;
import net.swordie.ms.connection.packet.Tms282SkillPackets;
import net.swordie.ms.connection.packet.UserRemote;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.enums.FieldOption;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.world.field.Field;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;

/** One accepted282 prepare owns the guard, initial immunity, enhancement and release refund. */
public final class Tms282AetherGuard {
    public static final int SKILL=Adele.AETHER_GUARD;
    private Cast cast;
    private static final class Cast {
        final Field field; final Tms282BuffBook book; final int level; final long start,until,invulnerableUntil,perfectUntil;
        boolean enhanced; ScheduledFuture<?> expiry;
        Cast(Field f,Tms282BuffBook b,int level,long now,int duration,int invulnerable,int perfect){
            field=f;book=b;this.level=level;start=now;until=now+duration;invulnerableUntil=now+invulnerable;perfectUntil=now+perfect;
        }
    }
    private static Tms282AetherGuard of(Char chr){
        return Tms282RemoteBuffs.active() && chr!=null && chr.getJobHandler() instanceof Adele adele
                ? adele.getAetherGuardRuntime() : null;
    }
    public static boolean prepare(Char chr,Tms282BuffBook book,long now){
        var runtime=of(chr);return runtime!=null && runtime.begin(chr,book,now);
    }
    public static boolean active(Char chr,long now){
        var runtime=of(chr);if(runtime==null)return false;
        synchronized(runtime){return runtime.cast!=null && runtime.valid(chr,runtime.cast,now);}
    }
    private synchronized boolean begin(Char chr,Tms282BuffBook book,long now){
        if(cast!=null){if(valid(chr,cast,now))return false;finish(chr,cast,now);}
        var info=SkillData.getSkillInfoById(SKILL);int level=chr.getSkillLevel(SKILL);
        if(book==null || info==null || level<=0 || !JobConstants.isAdele(chr.getJob()) || chr.getHP()<=0 || chr.getField()==null
                || (chr.getField().getFieldLimit() & (FieldOption.SkillLimit.getVal()|FieldOption.MoveSkillOnly.getVal()))!=0
                || chr.hasSkillOnCooldown(SKILL) || book.coolingDown(SKILL,now))return false;
        int duration=info.getValue(SkillStat.updatableTime,level);
        if(chr.getSkillLevel(Adele.AETHER_GUARD_PERSIST)>0)
            duration+=SkillData.getSkillInfoById(Adele.AETHER_GUARD_PERSIST).getValue(SkillStat.subTime,chr.getSkillLevel(Adele.AETHER_GUARD_PERSIST));
        int invulnerable=info.getValue(SkillStat.s,level),perfect=info.getValue(SkillStat.y,level);
        if(duration<=0 || invulnerable<=0 || perfect<=0 || !chr.applyMpCon(info,level,true))return false;
        Cast current=new Cast(chr.getField(),book,level,now,duration,invulnerable,perfect);cast=current;
        // Preserve the existing server start-cooldown policy (SkillConstants.isCooltimeOnStartSkill).
        chr.setSkillCooldown(SKILL,level);book.startCooldown(SKILL,chr.getSkillCoolTimes().getOrDefault(SKILL,0L));
        var stats=new EnumMap<CharacterTemporaryStat,Option>(CharacterTemporaryStat.class);
        stats.put(IndieDamReduceR,indie(info.getValue(SkillStat.x,level),duration));
        stats.put(IndieApplySuperStance,indie(1,duration));
        Option enabled=new Option();enabled.nOption=1;enabled.rOption=SKILL;enabled.tOption=duration;enabled.setInMillis(true);stats.put(KeyDownEnable,enabled);
        Option immune=new Option();immune.nOption=1;immune.rOption=SKILL;immune.tOption=invulnerable;immune.setInMillis(true);stats.put(LWDike,immune);
        chr.getTemporaryStatManager().sendStat(stats);
        current.expiry=chr.getTimer().addEvent(()->expire(chr,current,System.currentTimeMillis()),duration);
        chr.getField().broadcast(UserRemote.skillPrepare(chr,SKILL,level),chr);
        return true;
    }
    private static Option indie(int value,int millis){
        Option o=new Option();o.nReason=SKILL;o.nValue=value;o.tTerm=millis;o.setInMillis(true);return o;
    }
    private boolean valid(Char chr,Cast current,long now){
        return current==cast && chr.getHP()>0 && chr.getField()==current.field && of(chr)==this
                && JobConstants.isAdele(chr.getJob()) && chr.getSkillLevel(SKILL)>0 && now>=current.start && now<current.until;
    }
    /** Native guard senders may report0 damage plus the guard boolean; ordinary senders retain positive raw damage. */
    public static int damage(Char chr,int damage,boolean guarded,long now){
        var runtime=of(chr);return runtime==null?damage:runtime.absorb(chr,damage,guarded,now);
    }
    private synchronized int absorb(Char chr,int damage,boolean guarded,long now){
        Cast current=cast;
        if(current==null)return damage;
        if(!valid(chr,current,now)){finish(chr,current,now);return damage;}
        if(now>current.invulnerableUntil || damage<0)return damage;
        if(!current.enhanced && now<=current.perfectUntil && (damage>0 || guarded)){
            current.enhanced=true;
            int value=SkillData.getSkillInfoById(SKILL).getValue(SkillStat.z,current.level);
            chr.getTemporaryStatManager().sendStat(IndieDamReduceR,indie(value,(int)(current.until-now)));
        }
        return 0;
    }
    public static boolean stop(Char chr,int skill,long now){
        if(skill!=SKILL || !Tms282RemoteBuffs.active())return false;
        reset(chr,now);return true;
    }
    public static void reset(Char chr,long now){
        var runtime=of(chr);if(runtime!=null)runtime.clear(chr,now);
    }
    private synchronized void clear(Char chr,long now){if(cast!=null)finish(chr,cast,now);}
    private synchronized void expire(Char chr,Cast current,long now){if(now>=current.until)finish(chr,current,now);}
    private void finish(Char chr,Cast current,long now){
        if(cast!=current)return;
        cast=null;if(current.expiry!=null)current.expiry.cancel(false);
        chr.getTemporaryStatManager().removeStatsBySkill(SKILL);
        long seconds=now<current.start?0:Math.max(0,current.until-now)/1000;
        long refund=seconds*SkillData.getSkillInfoById(SKILL).getValue(SkillStat.w,current.level);
        if(refund>0){
            long until=Math.max(chr.getSkillCoolTimes().getOrDefault(SKILL,0L),now+current.book.cooldownRemainingMillis(SKILL,now));
            long adjusted=Math.max(now,until-refund);
            chr.addSkillCoolTime(SKILL,adjusted);current.book.startCooldown(SKILL,adjusted);
            chr.write(Tms282SkillPackets.skillCooltimeSet(Map.of(SKILL,(int)((adjusted-now+999)/1000))));
        }
        if(chr.getField()!=null)chr.getField().broadcast(UserRemote.skillCancel(chr.getId(),SKILL),chr);
    }
}
