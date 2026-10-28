package net.swordie.ms.client.character.tms282;

import java.util.List;
import java.util.Map;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.SkillStat;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.jobs.legend.Evan;
import net.swordie.ms.connection.packet.Tms282RemoteBuffs;
import net.swordie.ms.connection.packet.Tms282SkillPackets;
import net.swordie.ms.connection.packet.UserRemote;
import net.swordie.ms.enums.TSIndex;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.world.field.Field;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;

/** Accepted Dragon Master casts only: server time owns riding, protection and the one release refund. */
public final class Tms282DragonMaster {
    public static final int SKILL=Evan.DRAGON_MASTER;
    private Cast cast;
    private Option pendingMarker;
    private Field pendingField;
    private static final class Cast {
        final Field field; final long started,until; final int level;
        boolean ended; long protectedUntil; Option mount,marker;
        Cast(Field field,long now,int seconds,int level){this.field=field;started=now;until=now+seconds*1000L;this.level=level;}
    }
    private static Tms282DragonMaster of(Char chr) {
        return chr!=null && Tms282RemoteBuffs.active() && chr.getJobHandler() instanceof Evan evan
                ? evan.getDragonMasterRuntime() : null;
    }
    public static boolean begin(Char chr,int level,long now) {
        var runtime=of(chr);if(runtime==null)return false;
        runtime.start(chr,level,now);return true;
    }
    private synchronized void start(Char chr,int level,long now) {
        if(chr.getHP()<=0 || chr.getField()==null || level<=0)return;
        if(cast!=null && !cast.ended && now<cast.until && validMount(chr))return;
        clearCast(chr);
        var info=SkillData.getSkillInfoById(SKILL);int seconds=info.getValue(SkillStat.time,level);
        Cast current=new Cast(chr.getField(),now,seconds,level);cast=current;
        if(pendingField==chr.getField())current.marker=pendingMarker;
        pendingMarker=null;pendingField=null;
        var tsm=chr.getTemporaryStatManager();var mount=tsm.getTSBByTSIndex(TSIndex.RideVehicleExpire);
        mount.setNOption(1939007);mount.setROption(SKILL);mount.setExpireTerm(seconds);
        current.mount=mount.getOption().deepCopy();
        tsm.sendStat(RideVehicleExpire,current.mount);current.mount.startTime=now;mount.getOption().startTime=now;
        put(chr,NewFlying,seconds*1000,now);
        // Continuous immunity covers riding plus natural-end grace. Early release replaces it with exactly u2.
        // Its later expiry also prevents the TSM's mount-expiry batch from deleting a newly added grace option.
        put(chr,NotDamaged,(seconds+info.getValue(SkillStat.u2,level))*1000,now);
        chr.getTimer().addEvent(()->expire(chr,current,System.currentTimeMillis()),seconds*1000L);
    }
    private synchronized void expire(Char chr,Cast current,long now) {
        if(now>=current.until)finish(chr,current,now,false,false);
    }
    private static void put(Char chr,CharacterTemporaryStat stat,int millis,long now) {
        if(millis<=0)return;
        Option option=new Option();option.nOption=1;option.rOption=SKILL;option.tOption=millis;option.setInMillis(true);
        chr.getTemporaryStatManager().sendStat(stat,option);option.startTime=now;
    }
    private boolean validMount(Char chr) {
        var tsm=chr.getTemporaryStatManager();if(!tsm.hasStat(RideVehicleExpire))return false;
        var o=tsm.getOption(RideVehicleExpire);return cast!=null && o==cast.mount && o.rOption==SKILL && o.nOption==1939007;
    }
    public static boolean blocksDamage(Char chr,long now) {
        var runtime=of(chr);return runtime!=null && runtime.protectedNow(chr,now);
    }
    private synchronized boolean protectedNow(Char chr,long now) {
        Cast current=cast;
        if(current==null || chr.getHP()<=0 || chr.getField()!=current.field || now<current.started)return false;
        if(!current.ended) {
            if(now<current.until)return validMount(chr);
            // Damage can arrive before the scheduled expiry callback; keep the natural-end grace continuous.
            finish(chr,current,now,false,false);
        }
        if(now>=current.protectedUntil)return false;
        var tsm=chr.getTemporaryStatManager();if(!tsm.hasStat(NotDamaged))return false;
        Option o=tsm.getOption(NotDamaged);
        long duration=o.isInMillis()?o.tOption:o.tOption*1000L;
        return o.rOption==SKILL && o.nOption>0 && duration>0 && now>=o.startTime && now-o.startTime<duration;
    }
    /** The client may release either the mount skill or its WZ vehicleAttackSkill. Replays are handled but inert. */
    public static boolean stop(Char chr,int requestSkill,long now) {
        var runtime=of(chr);
        if(runtime==null || (requestSkill!=SKILL && requestSkill!=Evan.DRAGON_MASTER_ATTACK))return false;
        runtime.release(chr,now);return true;
    }
    private synchronized void release(Char chr,long now) {
        clearPending(chr);
        Cast current=cast;
        if(current==null || current.ended)return;
        finish(chr,current,now,true,false);
        if(chr.getField()!=null)chr.getField().broadcast(UserRemote.skillCancel(chr.getId(),SKILL),chr);
    }
    /** Called after RESET bytes are sent; never sets the grace buff before a queued RESET of the same source. */
    public static void afterReset(Char chr,Map<CharacterTemporaryStat,List<Option>> removed,long now) {
        if(!removed.containsKey(RideVehicleExpire))return;
        var runtime=of(chr);if(runtime!=null)runtime.removed(chr,removed.get(RideVehicleExpire),now);
    }
    private synchronized void removed(Char chr,List<Option> options,long now) {
        if(cast!=null && !cast.ended && options.stream().anyMatch(o->o==cast.mount))finish(chr,cast,now,false,true);
    }
    private synchronized void finish(Char chr,Cast current,long now,boolean manual,boolean removed) {
        if(cast!=current || current.ended || of(chr)!=this)return;
        boolean valid=chr.getHP()>0 && chr.getField()==current.field && now>=current.started && (removed || validMount(chr));
        boolean refundable=manual && valid && validMount(chr) && now<current.until;
        current.ended=true;
        clearMarker(chr,current.marker);current.marker=null;
        var info=SkillData.getSkillInfoById(SKILL);
        long endedAt=Math.min(now,current.until);
        current.protectedUntil=valid?endedAt+info.getValue(SkillStat.u2,current.level)*1000L:0;
        chr.getTemporaryStatManager().removeStatsBySkill(SKILL);
        if(refundable){
            long seconds=(current.until-now)/1000;
            long refund=Math.min(info.getValue(SkillStat.s2,current.level),seconds*info.getValue(SkillStat.s,current.level))*1000L;
            if(refund>0){
                var book=Tms282BuffDirectory.INSTANCE.book(chr.getId());
                long until=chr.getSkillCoolTimes().getOrDefault(SKILL,0);
                if(book!=null)until=Math.max(until,now+book.cooldownRemainingMillis(SKILL,now));
                long adjusted=Math.max(now,until-refund);chr.addSkillCoolTime(SKILL,adjusted);
                if(book!=null)book.startCooldown(SKILL,adjusted);
                chr.write(Tms282SkillPackets.skillCooltimeSet(Map.of(SKILL,(int)((adjusted-now+999)/1000))));
            }
        }
        if(valid && now<current.protectedUntil)put(chr,NotDamaged,(int)(current.protectedUntil-now),now);
    }
    public static void onFieldChange(Char chr) {
        var runtime=of(chr);if(runtime!=null)runtime.clear(chr);
    }
    /** Capture only the exact marker created by an accepted prepare for the vehicle attack, before or after its cast. */
    public static void prepared(Char chr,int skill,Option marker,long now) {
        var runtime=of(chr);
        if(runtime!=null && skill==Evan.DRAGON_MASTER_ATTACK)runtime.captureMarker(chr,marker,now);
    }
    private synchronized void captureMarker(Char chr,Option marker,long now) {
        if(cast!=null && !cast.ended && cast.field==chr.getField() && now<cast.until && validMount(chr))cast.marker=marker;
        else {pendingMarker=marker;pendingField=chr.getField();}
    }
    private static void clearMarker(Char chr,Option marker) {
        // removeIndieStat compares identity, so a later key-down skill with the same job reason remains intact.
        if(marker!=null)chr.getTemporaryStatManager().removeIndieStat(IndieKeyDownTime,marker);
    }
    private void clearPending(Char chr) {
        Option marker=pendingMarker;pendingMarker=null;pendingField=null;clearMarker(chr,marker);
    }
    private synchronized void clear(Char chr) {
        clearPending(chr);clearCast(chr);
    }
    private void clearCast(Char chr) {
        if(cast==null)return;
        Cast old=cast;cast=null;clearMarker(chr,old.marker);chr.getTemporaryStatManager().removeStatsBySkill(SKILL);
    }
}
