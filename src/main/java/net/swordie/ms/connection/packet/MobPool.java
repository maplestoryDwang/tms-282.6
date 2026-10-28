package net.swordie.ms.connection.packet;

import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.jobs.adventurer.warrior.Paladin;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.constants.BossConstants;
import net.swordie.ms.enums.DeathType;
import net.swordie.ms.handlers.header.OutHeader;
import net.swordie.ms.life.mob.*;
import net.swordie.ms.life.mob.boss.demian.DemainDelayedAttack;
import net.swordie.ms.life.mob.skill.BurnedInfo;
import net.swordie.ms.life.mob.skill.EnergySphere;
import net.swordie.ms.life.mob.skill.MobSkillID;
import net.swordie.ms.life.mob.skill.MobSkillStat;
import net.swordie.ms.life.movement.MovementInfo;
import net.swordie.ms.loaders.containerclasses.MobSkillInfo;
import net.swordie.ms.util.Position;
import net.swordie.ms.util.Rect;
import net.swordie.ms.util.Util;
import net.swordie.ms.util.container.Triple;

import java.util.*;

import static net.swordie.ms.life.mob.MobStat.PAD;

public class MobPool {
    public static OutPacket enterField(Mob mob, String linkteam) {
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
        MobTemporaryStat.encode(outPacket, new TreeMap<>(), new HashSet<>(), linkteam);

        mob.encodeInit(outPacket);

        return outPacket;
    }

    public static OutPacket changeController(int objectID) {
        OutPacket outPacket = new OutPacket(OutHeader.MOB_CHANGE_CONTROLLER);

        outPacket.encodeByte(0); // 2 = auto-aggro, 1 = normal
        outPacket.encodeInt(objectID);

        return outPacket;
    }

    public static OutPacket changeController(Mob mob, String linkteam) {
        OutPacket outPacket = new OutPacket(OutHeader.MOB_CHANGE_CONTROLLER);

        outPacket.encodeByte(2); // 2 = auto-aggro, 1 = normal
        outPacket.encodeInt(mob.getObjectId());
        outPacket.encodeByte(mob.getCalcDamageIndex());
        outPacket.encodeInt(mob.getTemplateId());
        ForcedMobStat fms = mob.getForcedMobStat();
        outPacket.encodeByte(fms != null);
        if (fms != null) {
            fms.encode(outPacket);
        }
        MobTemporaryStat.encode(outPacket, new TreeMap<>(), new HashSet<>(), linkteam);

        mob.encodeInit(outPacket);

        return outPacket;
    }

    public static OutPacket leaveField(int id, DeathType deadType) {
        OutPacket outPacket = new OutPacket(OutHeader.MOB_LEAVE_FIELD);

        outPacket.encodeInt(id);
        byte type = deadType.getVal();
        outPacket.encodeByte(type);
        outPacket.encodeByte(0);
        if (type == 0
                || type == 1
                || type == 5
                || type == 6
                || type == 7
                || type == 8
                || type == 9
                || type == 10
                || type == 11) {
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);
        }
        if (type == 12) {
            outPacket.encodeInt(0);
        }
        if (type == 9) {
            outPacket.encodeInt(0);
        }

        return outPacket;
    }

    public static OutPacket forceChase(int mobID, boolean chase) {
        OutPacket outPacket = new OutPacket(OutHeader.MOB_FORCE_CHASE);

        outPacket.encodeInt(mobID);
        outPacket.encodeByte(chase);

        return outPacket;
    }

    public static OutPacket deadFPSMode(int mobID, int point) {
        OutPacket outPacket = new OutPacket(OutHeader.MOB_DEAD_FPS_MODE);

        outPacket.encodeInt(mobID);
        outPacket.encodeInt(point);

        return outPacket;
    }

    public static OutPacket damaged(int mobID, long damage, int templateID, byte type, int hp, long maxHp) {
        OutPacket outPacket = new OutPacket(OutHeader.MOB_DAMAGED);

        outPacket.encodeInt(mobID);
        outPacket.encodeByte(type);
        outPacket.encodeLong(damage);
        if (templateID / 10000 == 250 || templateID / 10000 == 251) {
            outPacket.encodeLong(hp);
            outPacket.encodeLong(maxHp);
        }

        return outPacket;
    }

    public static OutPacket hpIndicator(int mobID, byte percDamage) {
        OutPacket outPacket = new OutPacket(OutHeader.MOB_HP_INDICATOR);

        outPacket.encodeInt(mobID);
        outPacket.encodeInt(percDamage);
        outPacket.encodeByte(false);

        return outPacket;
    }

    public static OutPacket ctrlAck(Mob mob, boolean nextAttackPossible, short mobCtrlSN, int skillID, int slv, int forcedAttack) {
        OutPacket outPacket = new OutPacket(OutHeader.MOB_CONTROL_ACK);

        outPacket.encodeInt(mob.getObjectId());
        outPacket.encodeShort(mobCtrlSN);
        outPacket.encodeByte(nextAttackPossible);
        outPacket.encodeInt((int) mob.getMp());
        outPacket.encodeInt(skillID);
        outPacket.encodeShort(slv);
        outPacket.encodeInt(forcedAttack);
        outPacket.encodeInt(0);
        outPacket.encodeByte(0);

        return outPacket;
    }

    public static OutPacket statSet(int objectID, byte calcDamageIndex, TreeMap<MobStat, Option> addList, Set<BurnedInfo> burnedInfos, String linkteam) {
        OutPacket outPacket = new OutPacket(OutHeader.MOB_STAT_SET);

        boolean hasMovementStat = addList.keySet().stream().anyMatch(MobStat::isMovementAffectingStat);
        outPacket.encodeInt(objectID);
        MobTemporaryStat.encode(outPacket, addList, burnedInfos, linkteam);
        outPacket.encodeShort(0); // delay
        outPacket.encodeByte(calcDamageIndex); // nCalcDamageStatIndex
        if (hasMovementStat) {
            outPacket.encodeByte(1); // ?
        }

        return outPacket;
    }

    public static OutPacket statReset(int objectID, byte calcDamageIndex, TreeMap<MobStat, Option> removeList, Set<BurnedInfo> bis) {
        OutPacket outPacket = new OutPacket(OutHeader.MOB_STAT_RESET);

        int[] mask = MobTemporaryStat.getMaskByCollection(removeList);
        outPacket.encodeInt(objectID);
        for (int i = 0; i < MobStat.LENGTH; i++) {
            outPacket.encodeInt(mask[i]);
        }
        if (removeList.containsKey(MobStat.BurnedInfo)) {
            if (bis.isEmpty()) {
                outPacket.encodeInt(0);
                outPacket.encodeInt(0);
            } else {
                outPacket.encodeInt(0);
                outPacket.encodeInt(bis.size());
                for (BurnedInfo bi : bis) {
                    outPacket.encodeLong(0);
                    outPacket.encodeInt(bi.getCharacterId());
                    outPacket.encodeInt(bi.getSkillId());
                }
            }
        }
        if (removeList.containsKey(MobStat.NewBurnedInfo)) {
            outPacket.encodeInt(0);
        }
        outPacket.encodeByte(calcDamageIndex); // calcDamageStatIndex
        for (Map.Entry<MobStat, Option> entry : removeList.entrySet()) {
            MobStat mobStat = entry.getKey();
            if (mobStat.ordinal() < PAD.ordinal()) {
                //MobTemporaryStat.encodeIndieTempStat(outPacket, removeList);
                outPacket.encodeInt(0);
            }
        }
        if (MobTemporaryStat.hasRemovedMovementAffectingStat(removeList)) {
            outPacket.encodeByte(0);
        }

        return outPacket;
    }

    public static OutPacket statReset(int objectID, TreeMap<MobStat, Option> removeList, byte calcDamageStatIndex, boolean sn) {
        OutPacket outPacket = new OutPacket(OutHeader.MOB_STAT_RESET);

        int[] mask = MobTemporaryStat.getMaskByCollection(removeList);
        outPacket.encodeInt(objectID);
        for (int i = 0; i < MobStat.LENGTH; i++) {
            outPacket.encodeInt(mask[i]);
        }
        if (removeList.containsKey(MobStat.BurnedInfo)) {
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);
        }
        outPacket.encodeByte(calcDamageStatIndex);
        for (Map.Entry<MobStat, Option> entry : removeList.entrySet()) {
            MobStat mobStat = entry.getKey();
            if (mobStat.ordinal() < PAD.ordinal()) {
                MobTemporaryStat.encodeIndieTempStat(outPacket, removeList);
            }
        }
        if (MobTemporaryStat.hasRemovedMovementAffectingStat(removeList)) {
            outPacket.encodeByte(sn);
        }

        return outPacket;
    }

    public static OutPacket specialEffectBySkill(Mob mob, int skillID, int charId, int delay) {
        OutPacket outPacket = new OutPacket(OutHeader.MOB_SPECIAL_EFFECT_BY_SKILL);

        outPacket.encodeInt(mob.getObjectId());
        outPacket.encodeInt(skillID);
        outPacket.encodeInt(charId);
        outPacket.encodeShort(delay);

        return outPacket;
    }

    public static OutPacket specialSelectedEffectBySkill(Mob mob, int skillID, int charId) {
        OutPacket outPacket = new OutPacket(OutHeader.MOB_SPECIAL_SELECTED_EFFECT_BY_SKILL);

        outPacket.encodeInt(mob.getObjectId());
        outPacket.encodeInt(skillID);
        outPacket.encodeInt(charId);

        return outPacket;
    }

    public static OutPacket suspendReset(Mob mob) {
        OutPacket outPacket = new OutPacket(OutHeader.MOB_SUSPEND_RESET);

        outPacket.encodeInt(mob.getObjectId());
        outPacket.encodeByte(true);

        return outPacket;
    }

    public static OutPacket affected(Mob mob, int skillID, int unk, boolean userSkill, short delay) {
        OutPacket outPacket = new OutPacket(OutHeader.MOB_AFFECTED);

        outPacket.encodeInt(mob.getObjectId());
        outPacket.encodeInt(skillID);
        outPacket.encodeShort(delay);
        outPacket.encodeByte(userSkill);
        outPacket.encodeInt(skillID == Paladin.DIVINE_SHIELD ? 1 : unk); // 5?

        return outPacket;
    }

    public static OutPacket move(Mob mob, MobSkillAttackInfo msai, MovementInfo movementInfo, byte action) {
        OutPacket outPacket = new OutPacket(OutHeader.MOB_MOVE);

        outPacket.encodeInt(mob.getObjectId());
        outPacket.encodeByte(msai.actionAndDirMask);
        outPacket.encodeByte(action);
        outPacket.encodeLong(msai.targetInfo);
        outPacket.encodeByte(msai.multiTargetForBalls.size());
        for (Position pos : msai.multiTargetForBalls) {
            outPacket.encodePosition(pos);
        }
        outPacket.encodeByte(msai.randTimeForAreaAttacks.size());
        for (short s : msai.randTimeForAreaAttacks) {
            outPacket.encodeShort(s);
        }
        outPacket.encodeInt(msai.unkMobMovement != null ? msai.unkMobMovement.index : 0);
        if (msai.unkMobMovement != null && msai.unkMobMovement.index != 0) {
            outPacket.encodeInt(msai.unkMobMovement.unk1);
            outPacket.encodeInt(msai.unkMobMovement.unk2);
            outPacket.encodeInt(msai.unkMobMovement.unk3);
            outPacket.encodeInt(msai.unkMobMovement.unk4);
            outPacket.encodeInt(msai.unkMobMovement.unk5);
            outPacket.encodeInt(msai.unkMobMovement.unk6);
            outPacket.encodeInt(msai.unkMobMovement.unk7);
            outPacket.encodeInt(msai.unkMobMovement.unk8);
            outPacket.encodeInt(msai.unkMobMovement.unk9);
            outPacket.encodeInt(msai.unkMobMovement.unk10);
            outPacket.encodeInt(msai.unkMobMovement.unk11);
        }
        outPacket.encodeInt(0);
        outPacket.encode(movementInfo);
        outPacket.encodeByte(0); // bool

        return outPacket;
    }

    public static OutPacket bindRegister(Mob mob, List<MobBindInfo> mobBindInfos) {
        OutPacket outPacket = new OutPacket(OutHeader.MOB_BIND_REGISTER);

        final long now = System.currentTimeMillis();
        List<MobBindInfo> list = new ArrayList<>(mobBindInfos.size());
        for (MobBindInfo mbi : mobBindInfos) {
            if (now < mbi.getEndTime()) {
                list.add(mbi);
            }
        }
        outPacket.encodeInt(mob.getObjectId());
        outPacket.encodeByte(false);
        outPacket.encodeInt(list.size());
        for (MobBindInfo mbi : list) {
            outPacket.encodeInt(mbi.getCharID());
            outPacket.encodeInt(mbi.getValue());
            outPacket.encodeInt(mbi.getSkillID());
            outPacket.encodeInt(mbi.getUnk());
            outPacket.encodeInt((mbi.getEndTime() - now));
        }

        mob.setMobBindInfos(list);

        return outPacket;
    }

    public static OutPacket oneKillDamage(Mob mob) {
        OutPacket outPacket = new OutPacket(OutHeader.MOB_ONEKILL_DAMAGE);

        outPacket.encodeInt(mob.getObjectId());

        return outPacket;
    }

    public static OutPacket castingBarSkillStart(int mobID, int gaugeType, int castingTime, boolean isReverseGauge, boolean isShowUI) {
        OutPacket outPacket = new OutPacket(OutHeader.MOB_CASTING_BAR_SKILL);

        outPacket.encodeInt(mobID);
        outPacket.encodeByte(0);
        outPacket.encodeInt(gaugeType);
        outPacket.encodeInt(castingTime);
        outPacket.encodeByte(isReverseGauge);
        outPacket.encodeByte(isShowUI);
        outPacket.encodeByte(1);

        return outPacket;
    }

    public static OutPacket castingBarSkillEnd(int mobID, int type, int x, int forcedAttackIdx, int skillID, int slv2) {
        OutPacket outPacket = new OutPacket(OutHeader.MOB_CASTING_BAR_SKILL);

        outPacket.encodeInt(mobID);
        outPacket.encodeByte(type);
        switch (type) {
            case 1: // end
                outPacket.encodeByte(true); // success
                outPacket.encodeInt(x);
                if (x - 30 > 16) {
                    if (x - 13 <= 16) {
                        outPacket.encodeInt(forcedAttackIdx); // forcedAttackIdx
                    }
                } else {
                    outPacket.encodeInt(skillID); // skillID
                    outPacket.encodeInt(slv2); // slv
                }
                break;
            case 2: // end 2
                outPacket.encodeByte(true); // end
                outPacket.encodeInt(x);
                if (x - 30 <= 16) {
                    outPacket.encodeInt(skillID); // skillID
                    outPacket.encodeInt(slv2); // slv
                }
                break;
        }

        return outPacket;
    }

    public static OutPacket castingBarSkillReduce(int mobID, int reduceTime) {
        OutPacket outPacket = new OutPacket(OutHeader.MOB_CASTING_BAR_SKILL);

        outPacket.encodeInt(mobID);
        outPacket.encodeByte(3);
        outPacket.encodeInt(reduceTime);

        return outPacket;
    }

    public static OutPacket createBounceAttackAfterConvexSkill(Mob mob, MobSkillInfo msi) {
        // 0s are the ones where the wz property to take is unknown
        OutPacket outPacket = new OutPacket(OutHeader.MOB_BOUNCE_ATTACK_SKILL);

        outPacket.encodeInt(mob.getObjectId());
        outPacket.encodeInt(msi.getId());
        outPacket.encodeInt(msi.getLevel());
        outPacket.encodeByte(true);
        int count = msi.getSkillStatIntValue(MobSkillStat.count);
        outPacket.encodeInt(count); // nCount
        outPacket.encodeByte(false); // bDelayedSkill
        outPacket.encodeInt(mob.getPosition().getY()); // nCreateY
        outPacket.encodeInt(mob.getPosition().getY()); // nDensity
        outPacket.encodeInt(msi.getSkillStatIntValue(MobSkillStat.z)); // nFriction
        outPacket.encodeInt(msi.getSkillStatIntValue(MobSkillStat.w)); // nRestitution
        outPacket.encodeInt(BossConstants.LOTUS_BOUNCING_BALL_DURATION); // tDestroyDelay
        for (int i = 0; i < count; i++) {
            outPacket.encodeInt(mob.getObjectId() + i + 1);
        }

        return outPacket;
    }

    public static OutPacket createBounceAttackSkill(Mob mob, MobSkillInfo msi) {
        // 0s are the ones where the wz property to take is unknown
        OutPacket outPacket = new OutPacket(OutHeader.MOB_BOUNCE_ATTACK_SKILL);

        outPacket.encodeInt(mob.getObjectId());
        outPacket.encodeInt(msi.getId());
        outPacket.encodeInt(msi.getLevel());
        outPacket.encodeByte(false);
        Position pos = mob.getPosition();
        outPacket.encodeInt(pos.getX() + msi.getSkillStatIntValue(MobSkillStat.x));
        outPacket.encodeInt(pos.getY() + msi.getSkillStatIntValue(MobSkillStat.y));
        int count = msi.getSkillStatIntValue(MobSkillStat.count);
        outPacket.encodeInt(count);
        for (int i = 0; i < count; i++) {
            outPacket.encodeInt(mob.getObjectId() + i + 1); // nObjectSN
            outPacket.encodePositionInt(msi.getLt());
        }
        outPacket.encodeInt(msi.getSkillStatIntValue(MobSkillStat.z)); // nFriction
        outPacket.encodeInt(msi.getSkillStatIntValue(MobSkillStat.w)); // nRestitution
        outPacket.encodeInt(BossConstants.LOTUS_BOUNCING_BALL_DURATION); // tDestroyDelay
        outPacket.encodeInt(1000); // tStartDelay
        outPacket.encodeInt(0);
        outPacket.encodeByte(msi.getSkillStatIntValue(MobSkillStat.noGravity)); // bNoGravity
        boolean notDestroyByCollide = msi.getSkillStatIntValue(MobSkillStat.notDestroyByCollide) != 0;
        outPacket.encodeByte(notDestroyByCollide); // bNotDestroyByCollide
        if (msi.getId() == MobSkillID.BounceAttack.getVal() && (msi.getLevel() == 3 || msi.getLevel() == 4)) {
            outPacket.encodePositionInt(msi.getRb2());
        }
        if (notDestroyByCollide) {
            outPacket.encodeInt(5); // nIncScale
            outPacket.encodeInt(200); // nMaxScale
            outPacket.encodeInt(40); // nDecRadius
            outPacket.encodeInt(60); // fAngle
        }

        return outPacket;
    }

    public static OutPacket createEnergySpheres(int objectID, int slv, List<EnergySphere> energySpheres) {
        // 0s are the ones where the wz property to take is unknown
        OutPacket outPacket = new OutPacket(OutHeader.MOB_BOUNCE_ATTACK_SKILL);

        EnergySphere energySphere = energySpheres.get(0);
        outPacket.encodeInt(objectID);
        outPacket.encodeInt(217);
        outPacket.encodeInt(slv);
        outPacket.encodeByte(true);
        outPacket.encodeInt(energySpheres.size());
        outPacket.encodeByte(energySphere.isDelayed());
        outPacket.encodeInt(energySphere.getY());
        outPacket.encodeInt(energySphere.getDensity());
        outPacket.encodeInt(energySphere.getFriction());
        outPacket.encodeInt(energySphere.getStartDelay());
        outPacket.encodeInt(energySphere.getDestroyDelay());
        for (EnergySphere es : energySpheres) {
            outPacket.encodeInt(es.getObjectID());
        }

        return outPacket;
    }

    public static OutPacket nextAttack(int mobID, int forcedAttackIdx) {
        OutPacket outPacket = new OutPacket(OutHeader.MOB_NEXT_ATTACK);

        outPacket.encodeInt(mobID);
        outPacket.encodeInt(forcedAttackIdx);

        return outPacket;
    }

    public static OutPacket teleportRequest(int mobID, int skillAfter, Position position) {
        OutPacket outPacket = new OutPacket(OutHeader.MOB_TELEPORT_REQUEST);

        outPacket.encodeInt(mobID);
        outPacket.encodeByte(skillAfter == 0);
        outPacket.encodeInt(skillAfter);
        switch (skillAfter) {
            case 3:
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
            case 10:
            case 11:
            case 12:
            case 14:
            case 100:
                outPacket.encodePositionInt(position); // possible position?
                break;
            case 4:
                outPacket.encodePosition(position); // ptMobTeleportDest
                break;
        }

        return outPacket;
    }

    public static OutPacket teleportRequest(Mob mob, boolean skillAfter, int type, Position position) {
        OutPacket outPacket = new OutPacket(OutHeader.MOB_TELEPORT_REQUEST);

        outPacket.encodeInt(mob.getObjectId());
        outPacket.encodeByte(skillAfter);
        if (!skillAfter) {
            outPacket.encodeInt(type);
            switch (type) {
                case 3, 5, 6, 7, 8, 9, 10, 11, 12, 14, 15, 16, 100 ->
                        outPacket.encodePositionInt(position); // possible position?
                case 4 -> outPacket.encodePosition(position); // ptMobTeleportDest
            }
        }
        return outPacket;
    }

    public static OutPacket forcedAction(int mobID, int action) {
        OutPacket outPacket = new OutPacket(OutHeader.MOB_FORCED_ACTION);

        outPacket.encodeInt(mobID);
        outPacket.encodeInt(action);

        return outPacket;
    }

    public static OutPacket forcedSkillAction(int mobID, int action) {
        OutPacket outPacket = new OutPacket(OutHeader.MOB_FORCED_SKILL_ACTION);

        outPacket.encodeInt(mobID);
        outPacket.encodeInt(action);
        outPacket.encodeByte(false);

        return outPacket;
    }

    public static OutPacket setAfterAttack(int mobID, short afterAttack, int serverAction, int attackCount, boolean left) {
        OutPacket outPacket = new OutPacket(OutHeader.MOB_SET_AFTER_ATTACK);

        outPacket.encodeInt(mobID);
        outPacket.encodeShort(afterAttack);
        outPacket.encodeInt(attackCount);
        outPacket.encodeInt(serverAction);
        outPacket.encodeByte(left);

        return outPacket;
    }

    public static OutPacket escortFullPath(Mob mob, int oldAttr, boolean stopEscort) {
        OutPacket outPacket = new OutPacket(OutHeader.ESCORT_FULL_PATH);

        outPacket.encodeInt(mob.getObjectId());
        outPacket.encodeInt(mob.getEscortDest().size());
        outPacket.encodeShort(mob.getPosition().getX());
        outPacket.encodeShort(oldAttr);
        outPacket.encodeInt(mob.getPosition().getY());
        for (EscortDest escortDest : mob.getEscortDest()) {
            outPacket.encodeShort(escortDest.getDestPos().getX());
            outPacket.encodeShort(escortDest.getAttr());
            outPacket.encodeInt(escortDest.getDestPos().getY());
            outPacket.encodeInt(escortDest.getMass());
            if (escortDest.getMass() == 2) {
                outPacket.encodeInt(escortDest.getStopDuration());
            }
        }
        outPacket.encodeInt(mob.getCurrentDestIndex());
        int stopDuration = mob.getEscortStopDuration();
        outPacket.encodeByte(stopDuration > 0);
        if (stopDuration > 0) {
            outPacket.encodeInt(stopDuration);
        }
        outPacket.encodeByte(stopEscort);

        return outPacket;
    }

    public static OutPacket mobAttackBlock(Mob mob, ArrayList<Integer> skillsIDS) {
        OutPacket outPacket = new OutPacket(OutHeader.MOB_ATTACK_BLOCK);

        outPacket.encodeInt(mob.getObjectId());
        outPacket.encodeInt(skillsIDS.size());
        for (int skillID : skillsIDS) {
            outPacket.encodeInt(skillID);
        }

        return outPacket;
    }

    public static OutPacket damageShareInfoToLocal(Mob mob, boolean isLocalUserInInServer, byte countInServer) {
        OutPacket outPacket = new OutPacket(OutHeader.MOB_DAMAGE_SHARE_INFO_TO_LOCAL);

        outPacket.encodeInt(mob.getObjectId());
        outPacket.encodeByte(isLocalUserInInServer);
        outPacket.encodeByte(countInServer);

        return outPacket;
    }

    public static OutPacket damageShareInfoToRemote(Mob mob, byte countInServer) {
        OutPacket outPacket = new OutPacket(OutHeader.MOB_DAMAGE_SHARE_INFO_TO_REMOTE);

        outPacket.encodeInt(mob.getObjectId());
        outPacket.encodeByte(countInServer);

        return outPacket;
    }

    public static OutPacket breakDownTimeZoneTimeOut(Mob mob) {
        OutPacket outPacket = new OutPacket(OutHeader.MOB_BREAK_DOWN_TIME_ZONE_TIME_OUT);

        outPacket.encodeInt(mob.getObjectId());

        return outPacket;
    }

    public static OutPacket catchEffect(Mob mob, boolean success, int delay) { // Monster Magnet (xóa rồi)
        OutPacket outPacket = new OutPacket(OutHeader.MOB_CATCH_EFFECT);

        outPacket.encodeInt(mob.getObjectId());
        outPacket.encodeByte(success);
        outPacket.encodeByte(delay);

        return outPacket;
    }

    public static OutPacket stealEffect(Mob mob, int shiftCanvas) {
        OutPacket outPacket = new OutPacket(OutHeader.MOB_STEAL_EFFECT);

        outPacket.encodeInt(mob.getObjectId());
        outPacket.encodeInt(shiftCanvas);

        return outPacket;
    }

    public static OutPacket effectByItem(Mob mob, int itemID, boolean success) {
        OutPacket outPacket = new OutPacket(OutHeader.MOB_EFFECT_BY_ITEM);

        outPacket.encodeInt(mob.getObjectId());
        outPacket.encodeInt(itemID);
        outPacket.encodeByte(success);

        return outPacket;
    }

    public static OutPacket speaking(Mob mob, int speakInfo, int speech) {
        OutPacket outPacket = new OutPacket(OutHeader.MOB_SPEAKING);

        outPacket.encodeInt(mob.getObjectId());
        outPacket.encodeInt(speakInfo);
        outPacket.encodeInt(speech);

        return outPacket;
    }

    public static OutPacket messaging(Mob mob, int messageInfo, int message) {
        OutPacket outPacket = new OutPacket(OutHeader.MOB_MESSAGING);

        outPacket.encodeInt(mob.getObjectId());
        outPacket.encodeInt(messageInfo);
        outPacket.encodeInt(message);

        return outPacket;
    }

    public static OutPacket setSkillDelay(Mob mob, int skillAfter, MobSkillInfo msi, int sequenceDelay, Rect rect) {
        OutPacket outPacket = new OutPacket(OutHeader.MOB_SKILL_DELAY);

        outPacket.encodeInt(mob.getObjectId());

        outPacket.encodeInt(skillAfter);
        outPacket.encodeInt(msi.getId());
        outPacket.encodeInt(msi.getLevel());
        outPacket.encodeInt(msi.getId() == 230 ? 900 : 0); // v214

        outPacket.encodeInt(sequenceDelay);
        if (msi.getId() != 0) {
            outPacket.encodeInt(1);
            outPacket.encodeRectInt(rect);
        }

        return outPacket;
    }

    public static OutPacket setSkillDelay(Mob mob, int skillAfter, MobSkillInfo msi, int sequenceDelay, List<Rect> rects) {
        OutPacket outPacket = new OutPacket(OutHeader.MOB_SKILL_DELAY);

        outPacket.encodeInt(mob.getObjectId());

        outPacket.encodeInt(skillAfter);
        outPacket.encodeInt(msi.getId());
        outPacket.encodeInt(msi.getLevel());
        outPacket.encodeInt(msi.getId() == 230 ? 900 : 0); // v214
        if (rects == null || rects.isEmpty()) {
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);
        } else {
            outPacket.encodeInt(sequenceDelay);
            outPacket.encodeInt(rects.size());
            for (Rect rect : rects) {
                outPacket.encodeRectInt(rect);
            }
        }

        return outPacket;
    }

    public static OutPacket setSkillDelay(Mob mob, int skillAfter, int skillID, int slv) {
        OutPacket outPacket = new OutPacket(OutHeader.MOB_SKILL_DELAY);

        outPacket.encodeInt(mob.getObjectId());

        outPacket.encodeInt(skillAfter);
        outPacket.encodeInt(skillID);
        outPacket.encodeInt(slv);
        outPacket.encodeInt(skillID == 230 ? 900 : 0); // v214
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);

        return outPacket;
    }

    public static OutPacket airHit(Mob mob, short AirHitVy, int AirHitElapse) {
        OutPacket outPacket = new OutPacket(OutHeader.MOB_AIR_HIT);

        outPacket.encodeInt(mob.getObjectId());
        outPacket.encodeShort(AirHitVy);
        outPacket.encodeShort(AirHitElapse);

        return outPacket;
    }


    public static OutPacket registerRelMobZone(Mob mob, int mobID, int dataType) {
        OutPacket outPacket = new OutPacket(OutHeader.MOB_REGISTER_REL_MOB_ZONE);

        outPacket.encodeInt(mob.getObjectId());
        outPacket.encodeInt(dataType);
        outPacket.encodeInt(mobID);

        return outPacket;
    }

    public static OutPacket unregisterRelMobZone(Mob mob, int mobID, int dataType) {
        OutPacket outPacket = new OutPacket(OutHeader.MOB_UNREGISTER_REL_MOB_ZONE);

        outPacket.encodeInt(mob.getObjectId());
        outPacket.encodeInt(dataType);
        outPacket.encodeInt(mobID);

        return outPacket;
    }

    public static OutPacket ltrbDamageSkill(Mob mob, int skillID, int slv) {
        OutPacket outPacket = new OutPacket(OutHeader.MOB_LTRB_DAMAGE_SKILL);

        outPacket.encodeInt(mob.getObjectId());
        outPacket.encodeInt(skillID);
        outPacket.encodeInt(slv);

        return outPacket;
    }

    public static OutPacket laserControl(Mob mob, int angle, int autoOffset, boolean directionToUp) {
        OutPacket outPacket = new OutPacket(OutHeader.MOB_LASER_CONTROL);

        outPacket.encodeInt(mob.getObjectId());
        outPacket.encodeInt(angle);
        outPacket.encodeInt(autoOffset);
        outPacket.encodeByte(directionToUp);

        return outPacket;
    }

    public static OutPacket demainDelayedAttackCreate(Mob mob, boolean isFlip, int mobAttackIdx, Set<DemainDelayedAttack> ddaSet) {
        OutPacket outPacket = new OutPacket(OutHeader.MOB_DEMIAN_DELAYED_ATTACK_CREATE);

        outPacket.encodeInt(mob.getObjectId());
        outPacket.encodeInt(170);
        outPacket.encodeInt(42);
        outPacket.encodeByte(isFlip);
        outPacket.encodeInt(mobAttackIdx);
        outPacket.encodeInt(ddaSet.size());
        for (DemainDelayedAttack dda : ddaSet) {
            outPacket.encodeInt(dda.getObjectID());
            outPacket.encodePositionInt(dda.getPos());
            outPacket.encodeInt(dda.getAngle());
        }

        return outPacket;
    }

    public static OutPacket demainDelayedAttackCreate(Mob mob, int slv, boolean isFlip, int mobAttackIdx, DemainDelayedAttack dda) {
        OutPacket outPacket = new OutPacket(OutHeader.MOB_DEMIAN_DELAYED_ATTACK_CREATE);

        outPacket.encodeInt(mob.getObjectId());
        outPacket.encodeInt(170);
        outPacket.encodeInt(slv);
        if (slv > 44 & slv <= 47) {
            outPacket.encodeByte(isFlip);
            outPacket.encodeInt(mobAttackIdx);
            outPacket.encodeInt(dda.getObjectID());
            outPacket.encodePositionInt(dda.getPos());
        }

        return outPacket;
    }

    public static OutPacket bossPaternResult(Mob mob, int unk, long x) {
        OutPacket outPacket = new OutPacket(OutHeader.MOB_BOSS_PATERN_RESULT);

        outPacket.encodeInt(mob.getObjectId());
        outPacket.encodeInt(unk);
        outPacket.encodeLong(x); // <int32 name="x" value="10500000" />
        // PatternSystem.img

        return outPacket;
    }

    public static OutPacket nextTargetFromSvr(Mob mob, int charID) {
        OutPacket outPacket = new OutPacket(OutHeader.MOB_NEXT_TARGET_FROM_SVR);

        outPacket.encodeInt(mob.getObjectId());
        outPacket.encodeInt(charID);

        return outPacket;
    }

    public static OutPacket freezeEffect(Mob mob) {
        OutPacket outPacket = new OutPacket(OutHeader.MOB_FREEZE_EFFECT);

        outPacket.encodeInt(mob.getObjectId());
        outPacket.encodeInt(0);

        return outPacket;
    }

    public static OutPacket leaveFieldTargetFromSvr(int mobObjectID, int charID, short jobID) {
        OutPacket outPacket = new OutPacket(OutHeader.MOB_LEAVE_FIELD_TARGET_FROM_SVR);

        outPacket.encodeInt(mobObjectID);
        outPacket.encodeInt(charID);
        outPacket.encodeInt(jobID);

        return outPacket;
    }

    public static OutPacket mobAttackedByMob(Mob mob, int attackIdx, long damage, int mobTemplateID, boolean isLeft) {
        OutPacket outPacket = new OutPacket(OutHeader.MOB_ATTACKED_BY_MOB);

        outPacket.encodeInt(mob.getObjectId());
        outPacket.encodeByte(attackIdx); // looks like the effect, must be > -2
        outPacket.encodeLong(damage);
        outPacket.encodeByte(0);
        outPacket.encodeInt(mobTemplateID);
        outPacket.encodeByte(isLeft);
        outPacket.encodeInt(0);

        return outPacket;
    }

    public static OutPacket blackHand(Mob mob, int slv, List<Triple<Position, Integer, List<Rect>>> datas) {
        OutPacket outPacket = new OutPacket(OutHeader.VERUS_HILLA_BLACK_HAND);

        outPacket.encodeInt(mob.getObjectId());
        outPacket.encodeInt(datas.size());
        int i = 0;
        for (Triple<Position, Integer, List<Rect>> point : datas) {
            i++;
            outPacket.encodeInt(1);
            outPacket.encodeInt(i);
            outPacket.encodeInt(MobSkillID.VHillaEvilHand.getVal());
            outPacket.encodeInt(slv);
            outPacket.encodeByte(true);
            outPacket.encodeInt(1);
            outPacket.encodePosition(point.getLeft());
            outPacket.encodeInt(point.getMiddle());
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);
            outPacket.encodeInt(point.getRight().size());
            for (Rect rect : point.getRight()) {
                outPacket.encodeRectInt(rect);
            }
        }

        return outPacket;
    }

    public static OutPacket spirit(Mob mob, int charID, Rect rectx, Position pos, int slv) {
        OutPacket outPacket = new OutPacket(OutHeader.VERUS_HILLA_SPIRIT);

        int rand = Util.getRandom(4, 6);
        List<Rect> rects = new ArrayList<>();
        for (int i = 0; i < rand; i++) {
            rects.add(new Rect(Util.getRandom(-100, 150), -80, Util.getRandom(10, 15) * 5, 640));
        }

        outPacket.encodeInt(mob.getObjectId());
        outPacket.encodeInt(charID);
        outPacket.encodeRectInt(rectx);
        outPacket.encodeInt(0);
        outPacket.encodeInt(Util.getRandom(0, 210000000));
        outPacket.encodeInt(MobSkillID.VHillaEvilHand.getVal());
        outPacket.encodeInt(slv);
        outPacket.encodeByte(true);
        outPacket.encodeInt(1);
        outPacket.encodePosition(new Position(pos.getX(), -260));
        outPacket.encodeInt(250 * rand);
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
        outPacket.encodeInt(rects.size());
        for (Rect rect : rects) {
            outPacket.encodeRectInt(rect);
        }

        return outPacket;
    }
}
