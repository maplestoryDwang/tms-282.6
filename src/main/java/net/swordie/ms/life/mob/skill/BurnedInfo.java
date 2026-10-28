package net.swordie.ms.life.mob.skill;

import net.swordie.ms.Server;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.SkillStat;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.util.Util;

import java.util.Objects;
import java.util.concurrent.ScheduledFuture;

public class BurnedInfo {
    private int characterId, skillId, interval, end, dotAnimation, dotCount, superPos, attackDelay, dotTickIdx, dotTickDamR;
    private int start;
    private int lastUpdate;
    private long damage;
    private int addedTickDamR;
    private ScheduledFuture<?> damageScheduledFuture;

    public static BurnedInfo createBurnInfo(Char chr, int skillId, int slv, long damage) {
        SkillInfo si = SkillData.getSkillInfoById(skillId);
        int dotDmg = si.getValue(SkillStat.dot, slv);
        int dotInterval = si.getValue(SkillStat.dotInterval, slv);
        if (dotInterval <= 0) {
            dotInterval = 1;
        }
        int damageMulti = si.getValue(SkillStat.damage, slv) > 0 ? si.getValue(SkillStat.damage, slv) : 100;
        int dotDamage = si.getValue(SkillStat.dot, slv) > 0 ? (100 + si.getValue(SkillStat.dot, slv)) : 100;
        int dotTime = si.getValue(SkillStat.dotTime, slv);
        long now = System.currentTimeMillis();
        int time = dotTime * 1000;
        BurnedInfo bi = new BurnedInfo();
        bi.setCharacterId(chr.getId());
        bi.setSkillId(skillId);
        damage /= (long) ((double) damageMulti / 100);
        damage *= (long) ((double) dotDamage / 100);
        long minDmg = (long) (damage * 50.0D / 100.0D);
        long maxDmg = (long) (damage * 80.0D / 100.0D);
        try {
            damage = Util.getRandom(minDmg, maxDmg);
        } catch (Exception e) {
            damage = minDmg;
        } finally {
            bi.setDamage(damage);
        }
        bi.setInterval(dotInterval * 1000);
        bi.setDotCount(time / bi.getInterval());
        bi.setAttackDelay(0);
        bi.setDotTickIdx(0);
        bi.setDotTickDamR(si.getValue(SkillStat.DotTickDamR, slv)); //damage added for every tick
        bi.setDotAnimation(bi.getAttackDelay() + bi.getInterval() + time);
        bi.setStart((int) now);
        bi.setEnd((int) (now + time));
        bi.setLastUpdate((int) now);
        bi.setDamageScheduledFuture(null);
        return bi;
    }

    public int getCharacterId() {
        return characterId;
    }

    public void setCharacterId(int characterId) {
        this.characterId = characterId;
    }

    public int getSkillId() {
        return skillId;
    }

    public void setSkillId(int skillId) {
        this.skillId = skillId;
    }

    public long getDamage() {
        return damage;
    }

    public void setDamage(long damage) {
        this.damage = damage;
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public int getDotAnimation() {
        return dotAnimation;
    }

    public void setDotAnimation(int dotAnimation) {
        this.dotAnimation = dotAnimation;
    }

    public int getDotCount() {
        return dotCount;
    }

    public void setDotCount(int dotCount) {
        this.dotCount = dotCount;
    }

    public int getSuperPos() {
        return superPos;
    }

    public void setSuperPos(int superPos) {
        this.superPos = superPos;
    }

    public int getAttackDelay() {
        return attackDelay;
    }

    public void setAttackDelay(int attackDelay) {
        this.attackDelay = attackDelay;
    }

    public int getDotTickIdx() {
        return dotTickIdx;
    }

    public void setDotTickIdx(int dotTickIdx) {
        this.dotTickIdx = dotTickIdx;
    }

    public int getDotTickDamR() {
        return dotTickDamR;
    }

    public void setDotTickDamR(int dotTickDamR) {
        this.dotTickDamR = dotTickDamR;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(int lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public int getAddedTickDamR() {
        return addedTickDamR;
    }

    public void setAddedTickDamR(int addedTickDamR) {
        this.addedTickDamR = addedTickDamR;
    }

    public ScheduledFuture<?> getDamageScheduledFuture() {
        return damageScheduledFuture;
    }

    public void setDamageScheduledFuture(ScheduledFuture<?> damageScheduledFuture) {
        this.damageScheduledFuture = damageScheduledFuture;
    }

    public void encode(OutPacket outPacket) {
        outPacket.encodeLong(0); // 123_967_739 | 123_967_739 | 123_976_637
        outPacket.encodeInt(getCharacterId());
        outPacket.encodeInt(getSkillId());
        outPacket.encodeLong(getDamage()); // 61, 62
        outPacket.encodeInt(getInterval()); // 1000
        outPacket.encodeInt(getEnd()); // 720718045
        outPacket.encodeInt(getDotAnimation()); // 6393
        outPacket.encodeInt(getDotCount()); // 5
        outPacket.encodeInt(0); // 0
        outPacket.encodeInt(getAttackDelay()); // 0
        outPacket.encodeInt(getSuperPos()); // 0
        outPacket.encodeInt(0); // 0
        outPacket.encodeInt(getDotTickDamR()); // 0
        outPacket.encodeInt(getAddedTickDamR()); // 0
        outPacket.encodeInt(getEnd() - getStart()); // 5393
        outPacket.encodeInt(getInterval() / 1000); // 1
        outPacket.encodeLong(getDamage()); // 61
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BurnedInfo that = (BurnedInfo) o;
        return characterId == that.characterId &&
                skillId == that.skillId &&
                start == that.start;
    }

    @Override
    public int hashCode() {
        return Objects.hash(characterId, skillId, start);
    }

    @Override
    public String toString() {
        return "BurnedInfo{" +
                "skillId=" + skillId +
                ", end=" + end +
                ", superPos=" + superPos +
                ", startTime=" + start +
                '}';
    }
}

