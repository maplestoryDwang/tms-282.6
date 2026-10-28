package net.swordie.ms.client.jobs.resistance;

import net.swordie.ms.client.Client;
import net.swordie.ms.client.User;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.SkillStat;
import net.swordie.ms.client.character.skills.info.SkillUseInfo;
import net.swordie.ms.connection.packet.UserLocal;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.handlers.login.tms282.Tms282LoginState;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.world.field.Field;

import java.util.List;
import java.util.Map;

/** The single server-owned33001005 mark. CP0466 cannot remove an unexpired live mark. */
public final class Tms282PredatorsLeap {
    private final Char owner;
    private final WildHunter job;
    private Mark mark;
    private long generation;

    private record Mark(Client client,User user,int userId,int ownerId,Field field,Mob mob,
                        int level,long created,long until,long generation) { }

    Tms282PredatorsLeap(Char owner,WildHunter job) { this.owner=owner;this.job=job; }

    /** Shared skill validation calls this after typed process parsing, before any cost/cooldown. */
    public static boolean validate(Char chr,SkillUseInfo request) {
        if (!Tms282WildHunter.active() || request==null || !handles(request.skillID)) return true;
        return chr!=null && chr.getJobHandler() instanceof WildHunter hunter
                && hunter.predatorsLeap().canUse(request,System.currentTimeMillis());
    }

    public static boolean handles(int skill) {
        return skill==Tms282WildHunter.PREDATORS_LEAP || skill==Tms282WildHunter.PREDATORS_LEAP_JUMP;
    }

    private boolean currentOwner() {
        Client c=owner.getClient();Field f=owner.getField();
        return Tms282WildHunter.active() && owner.getJobHandler()==job && JobConstants.isWildHunter(owner.getJob())
                && owner.getId()>0 && owner.getHP()>0 && c!=null && c.getChr()==owner
                && c.getCh()!=null && c.getCh().isActive() && c.getCurrentState()==Client.IN_FIELD
                && c.getNextState()==Client.NONE && c.getUser()!=null && c.getUser()==owner.getUser()
                && c.getUser().getId()>0 && Tms282LoginState.of(c).isChannel()
                && Tms282LoginState.of(c).fieldCharacter()==owner && f!=null && f.getCharByID(owner.getId())==owner;
    }

    private int level() { return owner.getSkillLevel(Tms282WildHunter.PREDATORS_LEAP); }

    private Mob target(int id) {
        var life=owner.getField().getLifeByObjectID(id);
        return id>0 && life instanceof Mob mob && mob.getObjectId()==id && mob.getField()==owner.getField()
                && mob.getHp()>0 ? mob : null;
    }

    private boolean sameBinding(Mark m) {
        return m!=null && m.generation==generation && m.client==owner.getClient() && m.user==owner.getUser()
                && m.userId==owner.getUser().getId() && m.ownerId==owner.getId() && m.field==owner.getField();
    }

    private boolean usable(Mark m,long now) {
        return currentOwner() && sameBinding(m) && level()==m.level && now>=m.created && now<=m.until
                && target(m.mob.getObjectId())==m.mob;
    }

    public synchronized boolean canUse(SkillUseInfo request,long now) {
        var data=SkillData.getSkillInfoById(Tms282WildHunter.PREDATORS_LEAP);
        if (request==null || !currentOwner() || now<0 || level()<1 || level()>5 || data==null
                || data.getValue(SkillStat.time,level())!=600) { clear();return false; }
        if (request.skillID==Tms282WildHunter.PREDATORS_LEAP) return target(request.objectId)!=null;
        if (request.skillID!=Tms282WildHunter.PREDATORS_LEAP_JUMP) return false;
        if (!usable(mark,now)) { clear();return false; }
        return request.objectId==0 || request.objectId==mark.mob.getObjectId();
    }

    /** The normal accepted skill route has already paid once. No independent payment or teleport is added here. */
    public synchronized boolean afterAccepted(SkillUseInfo request,long now) {
        if (!canUse(request,now)) return false;
        if (request.skillID==Tms282WildHunter.PREDATORS_LEAP_JUMP) {
            clear(); // Revoke permission before writing the existing clear packet.
            owner.write(UserLocal.setMobAdvMark(Tms282WildHunter.PREDATORS_LEAP,Map.of(),0));
            return true;
        }
        var data=SkillData.getSkillInfoById(Tms282WildHunter.PREDATORS_LEAP);
        int duration=data==null ? 0 : data.getValue(SkillStat.time,level())*1000;
        Mob mob=target(request.objectId);
        if (duration!=600000 || mob==null) return false; // Exact current282 WZ and native marker duration.
        clear();
        Mark fresh=new Mark(owner.getClient(),owner.getUser(),owner.getUser().getId(),owner.getId(),owner.getField(),
                mob,level(),now,Math.addExact(now,duration),generation);
        owner.write(UserLocal.setMobAdvMark(Tms282WildHunter.PREDATORS_LEAP,Map.of(mob.getObjectId(),1),duration));
        // A synchronous failed write or a receipt/identity change cannot retain a usable permission.
        if (usable(fresh,now)) mark=fresh;
        return mark==fresh;
    }

    public synchronized boolean cleanup(List<Integer> targets,long now) {
        if (!currentOwner() || level()<1) { clear();throw new IllegalArgumentException("Unowned Predator cleanup"); }
        if (mark==null) return false;
        if (!sameBinding(mark)) { clear();return false; }
        if (!targets.contains(mark.mob.getObjectId())) return false;
        if (now>mark.until || target(mark.mob.getObjectId())!=mark.mob) { clear();return true; }
        return false; // Native equality retains a live target; the client supplies no authoritative clock.
    }

    /** Called for every field receipt (including same-field and cached-field return) and job cancellation. */
    public synchronized void clear() { mark=null;generation++; }
}
