package net.swordie.ms.client.jobs.adventurer.thief;

import java.util.EnumMap;
import java.util.concurrent.ScheduledFuture;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.info.AttackInfo;
import net.swordie.ms.client.character.tms282.Tms282BuffBook;
import net.swordie.ms.connection.packet.Tms282RemoteBuffs;
import net.swordie.ms.connection.packet.UserRemote;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.enums.FieldOption;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.world.field.Field;
import static net.swordie.ms.client.character.skills.SkillStat.*;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;

/** The native opening CP010D precedes CP04B9; each opening/loop attack costs the parent's MP. */
public final class Tms282BladeTempest {
    public static final int HELD=400041006, OPENING=400040006;
    private Cast cast;
    public static final class Cast {
        final Field field; final Tms282BuffBook book; final int level; final long start;
        long until; boolean prepared,credit; int pulses; ScheduledFuture<?> expiry;
        Cast(Char chr,Tms282BuffBook book,int level,long now,int duration){
            field=chr.getField();this.book=book;this.level=level;start=now;until=now+duration;
        }
    }
    public static boolean handles(int skill){return Tms282RemoteBuffs.active()&&(skill==HELD||skill==OPENING);}
    private static Tms282BladeTempest of(Char chr){
        return Tms282RemoteBuffs.active()&&chr!=null&&chr.getJobHandler() instanceof DualBlade job?job.bladeTempest:null;
    }
    private static boolean eligible(Char chr){
        return JobConstants.isDualBlade(chr.getJob())&&chr.getSkillLevel(HELD)>0&&chr.getHP()>0&&chr.getField()!=null
                &&(chr.getField().getFieldLimit()&(FieldOption.SkillLimit.getVal()|FieldOption.MoveSkillOnly.getVal()))==0;
    }
    private boolean valid(Char chr,Cast current,long now){
        return current!=null&&cast==current&&of(chr)==this&&eligible(chr)&&chr.getField()==current.field
                &&chr.getSkillLevel(HELD)==current.level&&now>=current.start&&now<current.until;
    }
    private Cast start(Char chr,Tms282BuffBook book,long now){
        if(cast!=null&&!valid(chr,cast,now))finish(chr,cast);
        if(cast!=null||book==null||!eligible(chr)||chr.hasSkillOnCooldown(HELD)||book.coolingDown(HELD,now))return null;
        int level=chr.getSkillLevel(HELD);var info=SkillData.getSkillInfoById(HELD);
        int duration=info.getValue(time,level)*1000;
        if(duration<=0||info.getValue(subTime,level)<=0||!chr.applyMpCon(info,level,true))return null;
        cast=new Cast(chr,book,level,now,duration);cast.credit=true;
        arm(chr,cast,now);return cast;
    }
    private void arm(Char chr,Cast current,long now){
        if(current.expiry!=null)current.expiry.cancel(false);
        current.expiry=chr.getTimer().addEvent(()->expire(chr,current,System.currentTimeMillis()),current.until-now);
    }
    public static boolean prepare(Char chr,Tms282BuffBook book,long now){
        var runtime=of(chr);return runtime!=null&&runtime.begin(chr,book,now);
    }
    private synchronized boolean begin(Char chr,Tms282BuffBook book,long now){
        if(cast!=null&&!valid(chr,cast,now))finish(chr,cast);
        Cast current=cast==null?start(chr,book,now):cast;
        if(current==null||current.prepared||current.book!=book)return false;
        current.prepared=true;int duration=SkillData.getSkillInfoById(HELD).getValue(time,current.level)*1000;
        current.until=now+duration;arm(chr,current,now);
        var stats=new EnumMap<net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat,Option>(net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.class);
        Option reduction=new Option();reduction.nReason=HELD;reduction.nValue=50;reduction.tTerm=duration;reduction.setInMillis(true);
        stats.put(IndieDamReduceR,reduction); // Current WZ common/s5=50, existing native CTS52 contract.
        Option shell=new Option();shell.nReason=HELD;shell.nValue=1;shell.tTerm=duration;shell.setInMillis(true);stats.put(IndieAntiMagicShell,shell);
        Option moving=new Option();moving.rOption=HELD;moving.nOption=SkillData.getSkillInfoById(HELD).getValue(x,current.level);
        moving.tOption=duration;moving.setInMillis(true);stats.put(KeyDownMoving,moving);
        chr.getTemporaryStatManager().sendStat(stats);
        current.field.broadcast(UserRemote.skillPrepare(chr,HELD,current.level),chr);return true;
    }
    public static Cast claim(Char chr,Tms282BuffBook book,AttackInfo attack,long now){
        var runtime=of(chr);return runtime==null?null:runtime.claim0(chr,book,attack,now);
    }
    private synchronized Cast claim0(Char chr,Tms282BuffBook book,AttackInfo attack,long now){
        if(!handles(attack.skillId)||!eligible(chr)||attack.inHeader!=InHeader.USER_MELEE_ATTACK)return null;
        var info=SkillData.getSkillInfoById(attack.skillId);int level=chr.getSkillLevel(HELD);
        int hits=info.getValue(attackCount,level),mobs=info.getValue(mobCount,level);
        Option mirror=chr.getTemporaryStatManager().getOptByCTSAndSkill(ShadowPartner,DualBlade.MIRROR_IMAGE);
        if(mirror!=null&&mirror.rOption==DualBlade.MIRROR_IMAGE&&mirror.nOption>0&&now>=mirror.startTime
                &&(mirror.tOption==0||now-mirror.startTime<(mirror.isInMillis()?mirror.tOption:mirror.tOption*1000L)))hits*=2;
        final int cap=hits;
        if(Byte.toUnsignedInt(attack.hits)>cap||attack.mobCount>mobs||attack.mobAttackInfo.size()>mobs
                ||attack.mobAttackInfo.stream().anyMatch(m->m.damages==null||m.damages.length>cap))return null;
        if(cast!=null&&!valid(chr,cast,now))finish(chr,cast);
        if(attack.skillId==OPENING){
            if(cast!=null)return null; // Native opening is exactly once, before the prepare.
            if(start(chr,book,now)==null)return null;
        }else if(cast==null||!cast.prepared||cast.pulses>=
                SkillData.getSkillInfoById(HELD).getValue(time,level)*1000/SkillData.getSkillInfoById(HELD).getValue(subTime,level))return null;
        if(!cast.credit&&!chr.applyMpCon(SkillData.getSkillInfoById(HELD),level,true)){finish(chr,cast);return null;}
        cast.credit=false;if(attack.skillId==HELD)cast.pulses++;
        attack.slv=level;return cast;
    }
    public static boolean validGrant(Char chr,Cast grant,long now){
        var runtime=of(chr);if(runtime==null)return false;
        synchronized(runtime){return runtime.valid(chr,grant,now);}
    }
    public static boolean stop(Char chr,int skill){
        if(!handles(skill))return false;reset(chr);return true;
    }
    public static void validate(Char chr,long now){
        var runtime=of(chr);if(runtime!=null)synchronized(runtime){if(runtime.cast!=null&&!runtime.valid(chr,runtime.cast,now))runtime.finish(chr,runtime.cast);}
    }
    public static void reset(Char chr){var runtime=of(chr);if(runtime!=null)runtime.clear(chr);}
    private synchronized void clear(Char chr){if(cast!=null)finish(chr,cast);}
    private synchronized void expire(Char chr,Cast current,long now){if(now>=current.until)finish(chr,current);}
    private void finish(Char chr,Cast current){
        if(current==null||cast!=current)return;
        cast=null;if(current.expiry!=null)current.expiry.cancel(false);
        chr.getTemporaryStatManager().removeStatsBySkill(HELD);
        // Preserve existing Job.handleCancelKeyDownSkill release-time CD, including forced cleanup.
        chr.setSkillCooldown(HELD,current.level);current.book.startCooldown(HELD,chr.getSkillCoolTimes().getOrDefault(HELD,0L));
        if(current.prepared&&chr.getField()!=null)chr.getField().broadcast(UserRemote.skillCancel(chr.getId(),HELD),chr);
    }
}
