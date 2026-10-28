package net.swordie.ms.connection.protocol.codec;

import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.packet.MobPool;
import net.swordie.ms.enums.DeathType;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.life.mob.MobSkillAttackInfo;
import net.swordie.ms.life.mob.MobStat;
import net.swordie.ms.life.mob.MobTemporaryStat;
import net.swordie.ms.life.mob.ForcedMobStat;
import net.swordie.ms.life.mob.skill.BurnedInfo;
import net.swordie.ms.life.movement.MovementInfo;
import net.swordie.ms.handlers.header.OutHeader;

import java.util.Set;
import java.util.TreeMap;

/** GMS 265: the {@link MobPool} builders unchanged. */
public final class Gms265MobPackets implements MobPacketEncoder {
    public static final Gms265MobPackets INSTANCE = new Gms265MobPackets();

    private Gms265MobPackets() {
    }

    @Override
    public OutPacket enterField(Mob mob, String linkTeam) {
        return MobPool.enterField(mob, linkTeam);
    }

    /** {@link MobPool#enterField} with a stat set (265 itself always sends an empty one). */
    @Override
    public OutPacket enterField(Mob mob, TreeMap<MobStat, Option> stats, Set<BurnedInfo> burnedInfos, String linkTeam) {
        if (stats.isEmpty()) {
            return MobPool.enterField(mob, linkTeam);
        }
        OutPacket outPacket = new OutPacket(OutHeader.MOB_ENTER_FIELD);
        ForcedMobStat fms = mob.getForcedMobStat();
        outPacket.encodeByte(mob.isSealedInsteadDead());
        outPacket.encodeInt(mob.getObjectId());
        outPacket.encodeByte(mob.getCalcDamageIndex());
        outPacket.encodeInt(mob.getTemplateId());
        outPacket.encodeByte(fms != null);
        if (fms != null) {
            fms.encode(outPacket);
        }
        MobTemporaryStat.encode(outPacket, stats, burnedInfos, linkTeam);
        mob.encodeInit(outPacket);
        return outPacket;
    }

    @Override
    public OutPacket leaveField(int objectId, DeathType deathType) {
        return MobPool.leaveField(objectId, deathType);
    }

    @Override
    public OutPacket changeController(int objectId) {
        return MobPool.changeController(objectId);
    }

    @Override
    public OutPacket changeController(Mob mob, String linkTeam) {
        return MobPool.changeController(mob, linkTeam);
    }

    @Override
    public OutPacket controlAck(Mob mob, boolean nextAttackPossible, short mobCtrlSN, int skillId, int slv, int forcedAttack) {
        return MobPool.ctrlAck(mob, nextAttackPossible, mobCtrlSN, skillId, slv, forcedAttack);
    }

    @Override
    public OutPacket move(Mob mob, MobSkillAttackInfo msai, MovementInfo path, byte action) {
        return MobPool.move(mob, msai, path, action);
    }

    @Override
    public OutPacket damaged(int objectId, long damage, int templateId, byte type, int hp, long maxHp) {
        return MobPool.damaged(objectId, damage, templateId, type, hp, maxHp);
    }

    @Override
    public OutPacket hpIndicator(int objectId, byte percent) {
        return MobPool.hpIndicator(objectId, percent);
    }

    @Override
    public OutPacket statSet(int objectId, byte calcDamageIndex, TreeMap<MobStat, Option> addList,
                             Set<BurnedInfo> burnedInfos, String linkTeam) {
        return MobPool.statSet(objectId, calcDamageIndex, addList, burnedInfos, linkTeam);
    }

    @Override
    public OutPacket statReset(int objectId, byte calcDamageIndex, TreeMap<MobStat, Option> removeList,
                               Set<BurnedInfo> burnedInfos) {
        return MobPool.statReset(objectId, calcDamageIndex, removeList, burnedInfos);
    }
}
