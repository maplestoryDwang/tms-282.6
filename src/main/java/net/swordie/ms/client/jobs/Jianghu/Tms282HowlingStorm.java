package net.swordie.ms.client.jobs.Jianghu;

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

/** Native003/006 prepare, held melee, cancel/stop, then exactly one paired004/007 finisher. */
public final class Tms282HowlingStorm {
    public static final int HELD=175121003, FINISH=175121004, HEXA_HELD=175141006, HEXA_FINISH=175141007;
    // Server delivery tolerance for the finisher queued immediately by native68210xx/6827Fxx.
    // This is not WZ cancelableTime, nor a claim about an official server timeout.
    private static final int FINISH_DELIVERY_MS=1000, PREPARE_MS=240;
    private Cast cast;
    private static final class Cast {
        final Field field; final Tms282BuffBook book; final int skill, finish, level, attackLimit;
        final long start, until;
        int attacks; long finishUntil; boolean released, finished;
        Option reduction, shell;
        Cast(Char chr,Tms282BuffBook book,int skill,int level,long now) {
            field=chr.getField();this.book=book;this.skill=skill;finish=skill==HELD?FINISH:HEXA_FINISH;this.level=level;start=now;
            var info=SkillData.getSkillInfoById(skill);
            int duration=info.getValue(updatableTime,level), interval=info.getValue(attackDelay,level);
            until=now+duration;
            attackLimit=1+(duration-PREPARE_MS-1)/interval;
        }
    }
    public static final class Grant {
        private final Cast cast; private final long until;
        private Grant(Cast cast,long until){this.cast=cast;this.until=until;}
    }
    public static boolean handles(int id){return Tms282RemoteBuffs.active()&&(id==HELD||id==FINISH||id==HEXA_HELD||id==HEXA_FINISH);}
    public static int parent(int id){return id==HEXA_HELD||id==HEXA_FINISH?HEXA_HELD:HELD;}
    public static boolean isHeld(int id){return id==HELD||id==HEXA_HELD;}
    private static int selected(Char chr){return chr.getSkillLevel(HEXA_HELD)>0&&chr.getSkillLevel(HELD)>=30?HEXA_HELD:HELD;}
    private static Tms282HowlingStorm of(Char chr){return Tms282RemoteBuffs.active()&&chr!=null
            &&chr.getJobHandler() instanceof MoXuan mo?mo.howlingStorm:null;}
    private static boolean eligible(Char chr,int skill){return JobConstants.isMoXuan(chr.getJob())&&chr.getHP()>0
            &&chr.getField()!=null&&chr.getSkillLevel(skill)>0
            &&(skill!=HEXA_HELD||chr.getSkillLevel(HELD)>=30)
            &&(chr.getField().getFieldLimit()&(FieldOption.SkillLimit.getVal()|FieldOption.MoveSkillOnly.getVal()))==0;}
    private boolean valid(Char chr,Cast current,long now){return current!=null&&cast==current&&of(chr)==this
            &&eligible(chr,current.skill)&&selected(chr)==current.skill&&chr.getField()==current.field
            &&chr.getSkillLevel(current.skill)==current.level&&now>=current.start;}
    public static boolean prepare(Char chr,Tms282BuffBook book,long now){return prepare(chr,book,HELD,now);}
    public static boolean prepare(Char chr,Tms282BuffBook book,int skill,long now){var runtime=of(chr);return runtime!=null&&isHeld(skill)&&runtime.begin(chr,book,skill,now);}
    private synchronized boolean begin(Char chr,Tms282BuffBook book,int skill,long now){
        if(cast!=null&&!valid(chr,cast,now))clear0(chr);
        if(cast!=null&&!cast.released)return false;
        if(!eligible(chr,skill)||selected(chr)!=skill)return false;
        // WZ changeSkill is one upgraded ability: neither sibling may bypass the other's retained cooldown.
        for(int sibling:new int[]{HELD,HEXA_HELD})
            if(book.coolingDown(sibling,now)||chr.getSkillCoolTimes().getOrDefault(sibling,0L)>now)return false;
        int level=chr.getSkillLevel(skill);var info=SkillData.getSkillInfoById(skill);
        if(info==null||info.getValue(updatableTime,level)<=PREPARE_MS||info.getValue(attackDelay,level)<=0)return false;
        // Existing server key-down policy: prepare uses applyMpCon(true); owned attacks do not repay.
        if(!chr.applyMpCon(info,level,true))return false;
        Cast current=new Cast(chr,book,skill,level,now);cast=current;
        Option reduction=new Option();reduction.nReason=skill;reduction.nValue=50;
        // Current282 common/s5 is fixed50; absent from the legacy SkillStat enum.
        reduction.tTerm=(int)(current.until-now);reduction.setInMillis(true);current.reduction=reduction;
        Option shell=new Option();shell.nReason=skill;shell.nValue=1;
        shell.tTerm=reduction.tTerm;shell.setInMillis(true);current.shell=shell;
        var stats=new java.util.EnumMap<net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat,Option>(net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.class);
        stats.put(IndieDamReduceR,reduction);stats.put(IndieAntiMagicShell,shell);
        chr.getTemporaryStatManager().sendStat(stats);reduction.startTime=now;shell.startTime=now;
        current.field.broadcast(UserRemote.skillPrepare(chr,skill,level),chr);
        chr.getTimer().addEvent(()->expire(chr,current,System.currentTimeMillis()),current.until-now);
        return true;
    }
    private synchronized void expire(Char chr,Cast current,long now){
        if(cast==current&&of(chr)==this&&!current.released&&now>=current.until)release(chr,current,current.until);
    }
    private void release(Char chr,Cast current,long now){
        if(current.released)return;
        current.released=true;current.finishUntil=Math.min(now,current.until)+FINISH_DELIVERY_MS;
        // Existing Job.handleCancelKeyDownSkill policy;003/006 are not isCooltimeOnStartSkill.
        chr.setSkillCooldown(current.skill,current.level);
        current.book.startCooldown(current.skill,chr.getSkillCoolTimes().getOrDefault(current.skill,0L));
        var tsm=chr.getTemporaryStatManager();
        if(current.reduction!=null)tsm.removeIndieStat(IndieDamReduceR,current.reduction);
        if(current.shell!=null)tsm.removeIndieStat(IndieAntiMagicShell,current.shell);
        current.reduction=null;current.shell=null;
        if(chr.getField()==current.field)current.field.broadcast(UserRemote.skillCancel(chr.getId(),current.skill),chr);
    }
    public static boolean stop(Char chr,int id,long now){
        if(!handles(id))return false;
        var runtime=of(chr);if(runtime!=null)runtime.stop0(chr,id,now);return true;
    }
    private synchronized void stop0(Char chr,int id,long now){
        if(cast==null||id!=cast.skill&&id!=cast.finish)return;
        if(id==cast.finish||!valid(chr,cast,now)){clear0(chr);return;}
        release(chr,cast,now);
    }
    public static Grant claim(Char chr,AttackInfo attack,long now){var runtime=of(chr);return runtime==null?null:runtime.claim0(chr,attack,now);}
    private synchronized Grant claim0(Char chr,AttackInfo attack,long now){
        Cast current=cast;if(!valid(chr,current,now)){clear0(chr);return null;}
        if(attack.inHeader!=InHeader.USER_MELEE_ATTACK)return null;
        var info=SkillData.getSkillInfoById(attack.skillId);if(info==null)return null;
        int hits=info.getValue(attackCount,current.level),mobs=info.getValue(mobCount,current.level);
        if(Byte.toUnsignedInt(attack.hits)>hits||attack.mobCount>mobs||attack.mobAttackInfo.size()>mobs
                ||attack.mobAttackInfo.stream().anyMatch(m->m.damages==null||m.damages.length>hits))return null;
        if(attack.skillId==current.skill){
            if(current.released||now<current.start+PREPARE_MS||now>=current.until||current.attacks>=current.attackLimit)return null;
            current.attacks++;return new Grant(current,current.until);
        }
        if(attack.skillId!=current.finish)return null;
        if(!current.released&&now>=current.until)release(chr,current,current.until);
        if(!current.released||current.finished||now>=current.finishUntil)return null;
        current.finished=true;return new Grant(current,current.finishUntil);
    }
    /** Accepted queued damage may cross normal release, never cast replacement or a lifecycle cleanup. */
    public static boolean validGrant(Char chr,Grant grant,long now){var runtime=of(chr);return runtime!=null&&grant!=null&&runtime.validGrant0(chr,grant,now);}
    private synchronized boolean validGrant0(Char chr,Grant grant,long now){return now<grant.until&&valid(chr,grant.cast,now);}
    public static void reset(Char chr){var runtime=of(chr);if(runtime!=null)runtime.clear0(chr);}
    private synchronized void clear0(Char chr){Cast prior=cast;cast=null;if(prior!=null)release(chr,prior,System.currentTimeMillis());}
}
