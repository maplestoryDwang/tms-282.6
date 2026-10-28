package net.swordie.ms.client.jobs.resistance;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.info.AttackInfo;
import net.swordie.ms.client.character.tms282.Tms282BuffBook;
import net.swordie.ms.client.character.tms282.Tms282HiddenSkillParents;
import net.swordie.ms.connection.packet.Tms282RemoteBuffs;
import net.swordie.ms.connection.packet.Tms282StatPackets;
import net.swordie.ms.connection.packet.UserRemote;
import net.swordie.ms.enums.FieldOption;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.world.field.Field;
import static net.swordie.ms.client.character.skills.SkillStat.*;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;

/** Native007 prepare/release and092 continuation share one paid, field-owned activation. */
public final class Tms282OmegaBlaster {
    public static final int HELD=400041007, INSTALLED=400041092;
    private Cast cast;
    private static final class Cast {
        final Field field; final Tms282BuffBook book; final int level; final long start,holdUntil;
        long releasedAt,installedUntil; int heldAttacks,installedAttacks,installedCap;
        boolean released,installed,cooldownStarted; Option reduction;
        Cast(Char chr,Tms282BuffBook book,int level,long now) {
            field=chr.getField();this.book=book;this.level=level;start=now;
            holdUntil=now+SkillData.getSkillInfoById(HELD).getValue(updatableTime,level);
        }
    }
    public static final class Grant {
        private final Cast cast; private final long until;
        private Grant(Cast cast,long until){this.cast=cast;this.until=until;}
    }
    public static boolean handles(int skill){return Tms282RemoteBuffs.active()&&(skill==HELD||skill==INSTALLED);}
    private static Tms282OmegaBlaster of(Char chr) {
        return chr!=null&&Tms282RemoteBuffs.active()&&chr.getJobHandler() instanceof Xenon xenon?xenon.omegaBlaster:null;
    }
    private static boolean eligible(Char chr) {
        return net.swordie.ms.constants.JobConstants.isXenon(chr.getJob())&&chr.getHP()>0&&chr.getField()!=null
                &&(chr.getField().getFieldLimit()&(FieldOption.SkillLimit.getVal()|FieldOption.MoveSkillOnly.getVal()))==0
                &&Tms282HiddenSkillParents.useLevel(chr,HELD)>0;
    }
    private boolean valid(Char chr,Cast current,long now) {
        return current!=null&&cast==current&&of(chr)==this&&eligible(chr)&&chr.getField()==current.field
                &&now>=current.start&&Tms282HiddenSkillParents.useLevel(chr,HELD)==current.level;
    }
    private static boolean evolution(Char chr,long now) {
        var tsm=chr.getTemporaryStatManager();if(!tsm.hasStat(ArtificialEvolution))return false;
        var option=tsm.getOption(ArtificialEvolution);
        long term=option.isInMillis()?option.tOption:option.tOption*1000L;
        return option.nOption>0&&option.rOption==Xenon.ARTIFICIAL_EVOLUTION_BUFF
                &&now>=option.startTime&&(term==0||now-option.startTime<term);
    }
    private boolean pay(Char chr,Tms282BuffBook book,long now) {
        if(cast!=null&&!cast.released)return false;
        if(!eligible(chr)||book.coolingDown(HELD,now)||chr.getSkillCoolTimes().getOrDefault(HELD,0L)>now)return false;
        int level=Tms282HiddenSkillParents.useLevel(chr,HELD);var info=SkillData.getSkillInfoById(HELD);
        int cost=info.getValue(mpCon,level),cooldown=info.getValue(cooltime,level);
        if(cost<0||cooldown<=0||chr.getMP()<cost)return false;
        clear0(chr);cast=new Cast(chr,book,level,now);
        chr.getAvatarData().getCharacterStat().setMp(chr.getMP()-cost);
        chr.write(Tms282StatPackets.hpMpChanged(true,chr.getHP(),chr.getMP()));
        return true;
    }
    private void startCooldown(Char chr,Cast current) {
        if(current.cooldownStarted)return;
        current.cooldownStarted=true;
        // Preserve Job.handleCancelKeyDownSkill policy:007 is not isCooltimeOnStartSkill.
        // Native SetClientCooltime is only1000ms and does not prove the official120s anchor.
        chr.setSkillCooldown(HELD,current.level);
        current.book.startCooldown(HELD,chr.getSkillCoolTimes().getOrDefault(HELD,0L));
    }
    public static boolean prepare(Char chr,Tms282BuffBook book,long now) {
        var runtime=of(chr);return runtime!=null&&runtime.begin(chr,book,now);
    }
    private synchronized boolean begin(Char chr,Tms282BuffBook book,long now) {
        if(evolution(chr,now)||!pay(chr,book,now))return false;
        Cast current=cast;
        // Original282 common/s5=50 at every level; legacy DAT did not retain this stat name.
        Option reduction=new Option();reduction.nReason=HELD;reduction.nValue=50;
        reduction.tTerm=(int)(current.holdUntil-now);reduction.setInMillis(true);current.reduction=reduction;
        chr.getTemporaryStatManager().sendStat(IndieDamReduceR,reduction);reduction.startTime=now;
        current.field.broadcast(UserRemote.skillPrepare(chr,HELD,current.level),chr);
        chr.getTimer().addEvent(()->expireHeld(chr,current,System.currentTimeMillis()),current.holdUntil-now);
        return true;
    }
    private synchronized void expireHeld(Char chr,Cast current,long now) {
        if(cast==current&&of(chr)==this&&!current.released&&now>=current.holdUntil)release(chr,current,current.holdUntil);
    }
    private void release(Char chr,Cast current,long now) {
        if(current.released)return;
        current.released=true;current.releasedAt=Math.min(now,current.holdUntil);
        startCooldown(chr,current);
        if(current.reduction!=null)chr.getTemporaryStatManager().removeIndieStat(IndieDamReduceR,current.reduction);
        current.reduction=null;
        if(chr.getField()==current.field)current.field.broadcast(UserRemote.skillCancel(chr.getId(),HELD),chr);
    }
    /** Both native cancel then stop are expected; neither pays or restarts cooldown. */
    public static boolean stop(Char chr,int skill,long now) {
        if(!handles(skill))return false;
        var runtime=of(chr);if(runtime!=null)runtime.endHeld(chr,skill,now);return true;
    }
    private synchronized void endHeld(Char chr,int skill,long now) {
        if(skill==HELD&&valid(chr,cast,now))release(chr,cast,now);
        else if(skill==INSTALLED)clear0(chr);
    }
    public static boolean install(Char chr,Tms282BuffBook book,int option,long now) {
        var runtime=of(chr);return runtime!=null&&runtime.install0(chr,book,option,now);
    }
    private synchronized boolean install0(Char chr,Tms282BuffBook book,int option,long now) {
        boolean continuation=(option&0x20)!=0;
        if(continuation) {
            if(!valid(chr,cast,now)||cast.installed)return false;
            if(!cast.released&&now>=cast.holdUntil)release(chr,cast,cast.holdUntil);
            if(!cast.released)return false;
        } else {
            if(!evolution(chr,now)||!pay(chr,book,now))return false;
            cast.released=true;cast.releasedAt=now;
            startCooldown(chr,cast);
        }
        var info=SkillData.getSkillInfoById(INSTALLED);
        int duration=info.getValue(continuation?q:q2,cast.level),interval=info.getValue(subTime,cast.level);
        if(duration<=0||interval<=0||now>=cast.releasedAt+duration)return false;
        cast.installed=true;cast.installedUntil=cast.releasedAt+duration;cast.installedCap=duration/interval;
        return true;
    }
    public static Grant claim(Char chr,AttackInfo attack,long now) {
        var runtime=of(chr);return runtime==null?null:runtime.claim0(chr,attack,now);
    }
    private synchronized Grant claim0(Char chr,AttackInfo attack,long now) {
        Cast current=cast;if(!valid(chr,current,now)||attack.inHeader!=InHeader.USER_MELEE_ATTACK)return null;
        var info=SkillData.getSkillInfoById(attack.skillId);
        int hits=info.getValue(attackCount,current.level),mobs=info.getValue(mobCount,current.level);
        if(Byte.toUnsignedInt(attack.hits)>hits||attack.mobCount>mobs||attack.mobAttackInfo.size()>mobs
                ||attack.mobAttackInfo.stream().anyMatch(m->m.damages==null||m.damages.length>hits))return null;
        if(attack.skillId==HELD) {
            // Native prepare/time=600 is the first update-list delay, within updatableTime=3500.
            if(current.released||now<current.start+600||now>=current.holdUntil||current.heldAttacks>=info.getValue(u,current.level))return null;
            current.heldAttacks++;return new Grant(current,current.holdUntil);
        }
        if(attack.skillId!=INSTALLED||!current.installed||now>=current.installedUntil||current.installedAttacks>=current.installedCap)return null;
        current.installedAttacks++;return new Grant(current,current.installedUntil);
    }
    /** Accepted in-flight attacks retain their grant across release, but cannot cross field/death/recast/expiry. */
    public static boolean validGrant(Char chr,Grant grant,long now) {
        var runtime=of(chr);return runtime!=null&&grant!=null&&runtime.validGrant0(chr,grant,now);
    }
    private synchronized boolean validGrant0(Char chr,Grant grant,long now){return now<grant.until&&valid(chr,grant.cast,now);}
    public static void reset(Char chr){var runtime=of(chr);if(runtime!=null)runtime.clear0(chr);}
    private synchronized void clear0(Char chr) {
        Cast prior=cast;cast=null;
        if(prior!=null)startCooldown(chr,prior);
        if(prior!=null&&prior.reduction!=null)chr.getTemporaryStatManager().removeIndieStat(IndieDamReduceR,prior.reduction);
    }
}
