package net.swordie.ms.client.jobs.flora;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.SkillStat;
import net.swordie.ms.client.character.skills.info.AttackInfo;
import net.swordie.ms.client.character.skills.info.SkillUseInfo;
import net.swordie.ms.connection.packet.Tms282AdeleShardPackets;
import net.swordie.ms.connection.packet.Tms282AdeleShardPackets.Area;
import net.swordie.ms.connection.packet.Tms282AdeleShardPackets.Reply;
import net.swordie.ms.connection.packet.Tms282RemoteBuffs;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.life.AffectedArea;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.util.Position;
import net.swordie.ms.util.Rect;
import net.swordie.ms.world.field.Field;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.SecondAtomLockOn;

/** One paid burst owns one native geometry reply and at most two attacks per created shard. */
public final class Tms282AdeleShardbreaker {
    private record Shard(Position position, Area area) { }
    private record Pending(Field field, int skill, int level, int request, boolean marked,
                           long until, int duration, int maxHits, int maxMobs, List<Shard> shards) { }
    public static final class Grant {
        private final AffectedArea area;
        private final long until, crystalUntil;
        private boolean crystalReady, crystalUsed;
        private final int maxHits, maxMobs;
        private int remaining = 2;
        private Grant(AffectedArea area, long until, long crystalUntil, int maxHits, int maxMobs) {
            this.area=area;this.until=until;this.crystalUntil=crystalUntil;this.maxHits=maxHits;this.maxMobs=maxMobs;
        }
    }
    private final Char chr;
    private Pending pending;
    private int sequence;
    private final Map<Integer,Grant> areas = new HashMap<>();

    public Tms282AdeleShardbreaker(Char chr) { this.chr=chr; }

    public static void validate(Char chr, SkillUseInfo use) {
        if(!Tms282AdeleShardPackets.handles(use.skillID))return;
        var shards=use.adeleShardPositions;
        int maximum=use.skillInfo.getValue(SkillStat.mobCount,use.slv);
        if(shards==null||shards.isEmpty()||shards.size()>maximum||shards.size()>Tms282AdeleShardPackets.MAX_AREAS
                ||(use.adeleShardMarked&&shards.size()>(maximum+1)/2))
            throw new IllegalArgumentException("Invalid Adele shard list");
        if(chr==null)return; // The reusable shape validator also runs without a character.
        if(!JobConstants.isAdele(chr.getJob())||!(chr.getJobHandler() instanceof Adele)||chr.getField()==null||chr.getHP()<=0)
            throw new IllegalArgumentException("Adele shard owner is not active");
        Rect search=chr.getRectAround(use.skillInfo.getRects().getFirst());
        int marked=chr.getTemporaryStatManager().hasStat(SecondAtomLockOn)
                ?chr.getTemporaryStatManager().getOption(SecondAtomLockOn).xOption:0;
        var ids=new HashSet<Integer>();
        int minX=Integer.MAX_VALUE,maxX=Integer.MIN_VALUE;
        Integer markedY=null;
        for(var shard:shards){
            int id=shard.getLeft();Position p=shard.getRight();
            // Native mark-mode positions spread100px either side of the target centre.
            int margin=use.adeleShardMarked?100:0;
            if(p==null||p.getX()<Short.MIN_VALUE||p.getX()>Short.MAX_VALUE||p.getY()<Short.MIN_VALUE||p.getY()>Short.MAX_VALUE
                    ||p.getX()<(long)search.getLeft()-margin||p.getX()>(long)search.getRight()+margin
                    ||p.getY()<search.getTop()||p.getY()>search.getBottom()||id<0)
                throw new IllegalArgumentException("Adele shard position outside cast range");
            if(id!=0&&(!(chr.getField().getLifeByObjectID(id) instanceof Mob mob)||mob.getHp()<=0))
                throw new IllegalArgumentException("Adele shard target is absent");
            if(use.adeleShardMarked){
                if(id==0||id!=marked)throw new IllegalArgumentException("Adele shard mark ownership");
                // 5A6AA5A..AB3F: one captured target bottom, X spread200 then field-boundary clamping.
                minX=Math.min(minX,p.getX());maxX=Math.max(maxX,p.getX());
                if((markedY!=null&&markedY!=p.getY())||(long)maxX-minX>200)
                    throw new IllegalArgumentException("Adele marked shards do not form one target group");
                markedY=p.getY();
            }
            else if(id!=0&&!ids.add(id))throw new IllegalArgumentException("Duplicate Adele shard target");
        }
    }

    public synchronized void begin(SkillUseInfo use,long now) {
        validate(chr,use);
        var list=new ArrayList<Shard>();
        int key=0;
        for(var entry:use.adeleShardPositions){
            Position p=new Position(entry.getRight().getX(),entry.getRight().getY());
            // The legacy DAT loader duplicated lt/rb for lt2. Pin these two current WZ rect2 values.
            Rect rect=p.getRectAround(use.skillID==Tms282AdeleShardPackets.BASE
                    ?new Rect(-175,-385,175,30):new Rect(-200,-385,200,65));
            list.add(new Shard(p,new Area(++key,rect.getLeft(),rect.getTop(),rect.getRight(),rect.getBottom(),true)));
        }
        // Native16900D8: delay units100ms;169C801: end=start+z+120ms.
        int duration=300+use.skillInfo.getValue(SkillStat.z,use.slv)+120;
        pending=new Pending(chr.getField(),use.skillID,use.slv,++sequence,use.adeleShardMarked,
                now+use.skillInfo.getValue(SkillStat.cooltime,use.slv)*1000L,duration,
                use.skillInfo.getValue(use.adeleShardMarked?SkillStat.s2:SkillStat.attackCount,use.slv),
                use.skillInfo.getValue(use.adeleShardMarked?SkillStat.u2:SkillStat.s,use.slv),List.copyOf(list));
        areas.clear();
        chr.write(Tms282AdeleShardPackets.request(use.skillID,pending.request(),list.stream().map(Shard::area).toList()));
    }

    public synchronized boolean complete(Reply reply,long now) {
        Pending cast=pending;
        if(cast==null||!JobConstants.isAdele(chr.getJob())||cast.field()!=chr.getField()||chr.getHP()<=0||now>=cast.until()
                ||chr.getSkillLevel(cast.skill())!=cast.level()||reply.skillId()!=cast.skill()
                ||reply.requestId()!=cast.request()||!reply.areas().equals(cast.shards().stream().map(Shard::area).toList()))return false;
        pending=null; // Consume the entire owned response before creating any field object.
        for(var shard:cast.shards()){
            AffectedArea area=AffectedArea.getAffectedArea(chr,cast.skill(),cast.level());
            area.setPosition(shard.position());area.setRect(shard.area().rect());area.setDelay((short)3);
            area.setDuration(cast.duration());area.setHitMob(true);area.setOption(cast.marked()?1:0);
            cast.field().spawnAffectedArea(area);
            areas.put(area.getObjectId(),new Grant(area,now+cast.duration(),cast.until(),cast.maxHits(),cast.maxMobs()));
        }
        return true;
    }

    public synchronized Grant claim(AttackInfo attack,long now) {
        Grant grant=areas.get(attack.affectedAreaObjId);
        if(!valid(grant,now)||grant.remaining<=0||attack.inHeader!=InHeader.USER_AREA_DOT_ATTACK
                ||attack.skillId!=grant.area.getSkillID()
                ||Byte.toUnsignedInt(attack.hits)>grant.maxHits||attack.mobCount>grant.maxMobs||attack.mobAttackInfo.size()>grant.maxMobs
                ||attack.mobAttackInfo.stream().anyMatch(m->m.damages.length>grant.maxHits))return null;
        grant.remaining--;
        attack.option=grant.area.getOption();
        return grant;
    }

    public synchronized boolean valid(Grant grant,long now) {
        return owns(grant)&&now<grant.until&&chr.getField().getLifeByObjectID(grant.area.getObjectId())==grant.area;
    }

    private boolean owns(Grant grant) {
        return grant!=null&&JobConstants.isAdele(chr.getJob())&&areas.get(grant.area.getObjectId())==grant&&chr.getHP()>0
                &&chr.getField()==grant.area.getField()&&chr.getSkillLevel(grant.area.getSkillID())==grant.area.getSlv();
    }

    /** One actual broadcast removal authorizes one crystal; an arbitrary missing OID never does. */
    public synchronized void removed(AffectedArea area) {
        Grant grant=areas.get(area.getObjectId());
        if(owns(grant)&&grant.area==area&&chr.getField().getLifeByObjectID(area.getObjectId())==null)grant.crystalReady=true;
    }

    public synchronized boolean crystal(SkillUseInfo use,long now) {
        if(use.skillID!=Adele.AETHER_CRYSTAL||use.adeleCrystalOption!=32||chr.getSkillLevel(Adele.AETHER_CRYSTAL)<=0
                ||!(chr.getJobHandler() instanceof Adele adele))return false;
        for(Grant grant:areas.values()){
            if(owns(grant)&&grant.crystalReady&&!grant.crystalUsed&&now<grant.crystalUntil
                    &&grant.area.getSkillID()==use.skillId&&grant.area.getPosition().equals(use.adeleCrystalOrigin)){
                grant.crystalUsed=true;
                // The client clamps its final point to screen-dependent bounds. Authority stays at our owned AA origin.
                adele.spawnAetherShard(grant.area.getPosition().deepCopy());
                return true;
            }
        }
        return false;
    }

    public static void removedArea(AffectedArea area) {
        Char owner=area.getOwner();
        if(Tms282RemoteBuffs.active()&&Tms282AdeleShardPackets.handles(area.getSkillID())&&owner!=null
                &&owner.getJobHandler() instanceof Adele adele)adele.shardbreaker().removed(area);
    }

    public synchronized void clear() { pending=null;areas.clear(); }
    public static void clear(Char chr) {
        if(chr!=null&&Tms282RemoteBuffs.active()&&chr.getJobHandler() instanceof Adele adele)adele.clearShardbreaker();
    }
}
