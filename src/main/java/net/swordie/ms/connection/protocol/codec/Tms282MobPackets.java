package net.swordie.ms.connection.protocol.codec;

import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.protocol.ProtocolVersion;
import net.swordie.ms.enums.DeathType;
import net.swordie.ms.life.mob.ForcedMobStat;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.life.mob.MobSkillAttackInfo;
import net.swordie.ms.life.mob.MobStat;
import net.swordie.ms.life.mob.skill.BurnedInfo;
import net.swordie.ms.life.movement.MovementInfo;
import net.swordie.ms.util.Position;

import java.util.Set;
import java.util.TreeMap;

/**
 * TMS 282.3 CMobPool / CMob packets (CMobPool::OnPacket 0x3FA78E0; per-mob packets 0x6A0.. go through
 * OnMobPacket 0x3FAB7F0, which reads the object id first). Layouts: mob-enter-field-282.md, mobstat-bits-282.md,
 * ingame-lp-map-2823.md section 2; every packet here was read back by the emulated client
 * (research/tms2823/mob-trace, Tms282MobPacketsCheck).
 */
public final class Tms282MobPackets implements MobPacketEncoder {
    public static final int MOB_ENTER_FIELD = 0x068D;
    public static final int MOB_LEAVE_FIELD = 0x0698;
    public static final int MOB_CHANGE_CONTROLLER = 0x0699;
    public static final int MOB_MOVE = 0x06A0;
    public static final int MOB_CONTROL_ACK = 0x06AB;
    public static final int MOB_STAT_SET = 0x06AD;
    public static final int MOB_STAT_RESET = 0x06AE;
    public static final int MOB_DAMAGED = 0x06B1;
    public static final int MOB_HP_INDICATOR = 0x06B7;

    public static final Tms282MobPackets INSTANCE =
            new Tms282MobPackets(Tms282MobStatCodec.INSTANCE, Tms282MovePathCodec.INSTANCE);

    private final MobStatCodec stats;
    private final MovePathCodec paths;

    public Tms282MobPackets(MobStatCodec stats, MovePathCodec paths) {
        this.stats = stats;
        this.paths = paths;
    }

    private static OutPacket packet(int op) {
        return OutPacket.forProfile(ProtocolVersion.TMS_282_3, op);
    }

    /**
     * {@code B sealed, Str "" (282: split at '_' into a field-wide string when not empty), I objId, B calcIdx,
     * I templateId, B hasFms [FMS], MobTemporaryStat, CMob::Init}.
     */
    @Override
    public OutPacket enterField(Mob mob, TreeMap<MobStat, Option> stats, Set<BurnedInfo> burnedInfos, String linkTeam) {
        OutPacket out = packet(MOB_ENTER_FIELD);
        out.encodeByte(mob.isSealedInsteadDead());
        out.encodeString("");
        out.encodeInt(mob.getObjectId());
        encodeMob(out, mob, stats, burnedInfos, linkTeam);
        return out;
    }

    /**
     * Inline in CMobPool::OnPacket (0x3FA7936): {@code I objId, B type, B}; when 0x861F60(type) (types 0, 1, 5..11,
     * the 265 set) three ints (265 wrote two); type 12: I; type 9: I.
     */
    @Override
    public OutPacket leaveField(int objectId, DeathType deathType) {
        OutPacket out = packet(MOB_LEAVE_FIELD);
        out.encodeInt(objectId);
        byte type = deathType.getVal();
        out.encodeByte(type);
        out.encodeByte(0);
        if (type == 0 || type == 1 || (type >= 5 && type <= 11)) {
            out.encodeInt(0);
            out.encodeInt(0);
            out.encodeInt(0);
        }
        if (type == 12) {
            out.encodeInt(0);
        }
        if (type == 9) {
            out.encodeInt(0);
        }
        return out;
    }

    /** Level 0: {@code B 0, I objId} and nothing else (0x3FA79FE). */
    @Override
    public OutPacket changeController(int objectId) {
        OutPacket out = packet(MOB_CHANGE_CONTROLLER);
        out.encodeByte(0);
        out.encodeInt(objectId);
        return out;
    }

    /** Level 2: {@code B 2, I objId}, then as MOB_ENTER_FIELD from calcIdx on (no sealed byte, no string). */
    @Override
    public OutPacket changeController(Mob mob, String linkTeam) {
        OutPacket out = packet(MOB_CHANGE_CONTROLLER);
        out.encodeByte(2);
        out.encodeInt(mob.getObjectId());
        encodeMob(out, mob, new TreeMap<>(), Set.of(), linkTeam);
        return out;
    }

    private void encodeMob(OutPacket out, Mob mob, TreeMap<MobStat, Option> stats, Set<BurnedInfo> burnedInfos,
                           String linkTeam) {
        out.encodeByte(mob.getCalcDamageIndex());
        out.encodeInt(mob.getTemplateId());
        ForcedMobStat fms = mob.getForcedMobStat();
        out.encodeByte(fms != null);
        if (fms != null) {
            fms.encode(out); // L I L 10xI B, same as 265
        }
        this.stats.encode(out, stats, burnedInfos, linkTeam);
        Tms282MobInit.encode(out, mob);
    }

    /** Same as 265: {@code I objId, S ctrlSN, B nextAttackPossible, I mp, I skillId, S slv, I forcedAttack, I 0, B 0}. */
    @Override
    public OutPacket controlAck(Mob mob, boolean nextAttackPossible, short mobCtrlSN, int skillId, int slv, int forcedAttack) {
        OutPacket out = packet(MOB_CONTROL_ACK);
        out.encodeInt(mob.getObjectId());
        out.encodeShort(mobCtrlSN);
        out.encodeByte(nextAttackPossible);
        out.encodeInt((int) mob.getMp());
        out.encodeInt(skillId);
        out.encodeShort(slv);
        out.encodeInt(forcedAttack);
        out.encodeInt(0);
        out.encodeByte(0);
        return out;
    }

    /**
     * Remote MOB_MOVE (CMob::OnMove 0x3EA5840): {@code I objId, B actionAndDirMask, B action, L targetInfo,
     * B n + n x (S x, S y), B n + n x S, I index [!= 0: 3 x I + 8 x I (floats as ints)], I, B n + n x I (n <= 10,
     * new in 282), path without key pad (3FDD1D0), B, B (two flags; 265 wrote the key pad and one flag)}.
     */
    @Override
    public OutPacket move(Mob mob, MobSkillAttackInfo msai, MovementInfo path, byte action) {
        OutPacket out = packet(MOB_MOVE);
        out.encodeInt(mob.getObjectId());
        out.encodeByte(msai.actionAndDirMask);
        out.encodeByte(action);
        out.encodeLong(msai.targetInfo);
        out.encodeByte(msai.multiTargetForBalls.size());
        for (Position pos : msai.multiTargetForBalls) {
            out.encodePosition(pos);
        }
        out.encodeByte(msai.randTimeForAreaAttacks.size());
        for (short s : msai.randTimeForAreaAttacks) {
            out.encodeShort(s);
        }
        MobSkillAttackInfo.UnkMobMovement u = msai.unkMobMovement;
        out.encodeInt(u != null ? u.index : 0);
        if (u != null && u.index != 0) {
            for (int v : new int[]{u.unk1, u.unk2, u.unk3, u.unk4, u.unk5, u.unk6, u.unk7, u.unk8, u.unk9, u.unk10, u.unk11}) {
                out.encodeInt(v);
            }
        }
        out.encodeInt(0);   // > 0: passed to [mob+0x1038] (0x795EE0)
        out.encodeByte(0);  // list of up to 10 ints
        paths.encodeBroadcast(out, path);
        out.encodeByte(0);  // flag 0x3EA6786
        out.encodeByte(0);  // flag 0x3EA6827
        return out;
    }

    /**
     * CMob::OnDamaged 0x3EA7700: {@code I objId, B type, L damage}, then {@code L hp, L maxHp} when the template
     * shows an HP gauge ([template+0x168] > 0; 265 decides by template id 250xxxx/251xxxx).
     */
    @Override
    public OutPacket damaged(int objectId, long damage, int templateId, byte type, int hp, long maxHp) {
        OutPacket out = packet(MOB_DAMAGED);
        out.encodeInt(objectId);
        out.encodeByte(type);
        out.encodeLong(damage);
        if (templateId / 10000 == 250 || templateId / 10000 == 251) {
            out.encodeLong(hp);
            out.encodeLong(maxHp);
        }
        return out;
    }

    /** 0x3EA9290: {@code I objId, I percent, B, B} (282 reads one byte more than 265). */
    @Override
    public OutPacket hpIndicator(int objectId, byte percent) {
        OutPacket out = packet(MOB_HP_INDICATOR);
        out.encodeInt(objectId);
        out.encodeInt(percent);
        out.encodeByte(0);
        out.encodeByte(0);
        return out;
    }

    /** 0x3EA6D80: {@code I objId, MobTemporaryStat, S delay, B calcDamageStatIndex, [B if a movement stat]}. */
    @Override
    public OutPacket statSet(int objectId, byte calcDamageIndex, TreeMap<MobStat, Option> addList,
                             Set<BurnedInfo> burnedInfos, String linkTeam) {
        OutPacket out = packet(MOB_STAT_SET);
        out.encodeInt(objectId);
        stats.encode(out, addList, burnedInfos, linkTeam);
        out.encodeShort(0);
        out.encodeByte(calcDamageIndex);
        if (stats.hasMovementAffectingStat(addList.keySet())) {
            out.encodeByte(1);
        }
        return out;
    }

    /**
     * 0x3EA6E20 -> 0x3F05E40 (same order as 265, 24-byte mask): {@code I objId, mask}, [BurnedInfo (ECFF20):
     * {@code I 0, I n + n x (L 0, I charId, I skillId)}], [NewBurnedInfo (ED00E0 -> ECCC20): {@code I n} = 0],
     * {@code B calcDamageStatIndex}, per indie bit of the mask in bit order (ED0C80 -> 0x9DDEB0) {@code I 0},
     * and {@code B} when the mask has a movement stat. That last byte is read only by a client whose CMob has a
     * move object ([mob+0x430], vtbl+0x58 = 0x3E6D2A0); a client that does not read it ignores the trailing byte.
     */
    @Override
    public OutPacket statReset(int objectId, byte calcDamageIndex, TreeMap<MobStat, Option> removeList,
                               Set<BurnedInfo> burnedInfos) {
        OutPacket out = packet(MOB_STAT_RESET);
        out.encodeInt(objectId);
        if (stats instanceof Tms282MobStatCodec t) {
            t.reportUnsupported(removeList.keySet());
        }
        stats.encodeMask(out, removeList.keySet());
        if (removeList.containsKey(MobStat.BurnedInfo)) {
            Set<BurnedInfo> burns = burnedInfos == null ? Set.of() : burnedInfos; // 265 callers pass null
            out.encodeInt(0);
            out.encodeInt(burns.size());
            for (BurnedInfo bi : burns) {
                out.encodeLong(0);
                out.encodeInt(bi.getCharacterId());
                out.encodeInt(bi.getSkillId());
            }
        }
        if (removeList.containsKey(MobStat.NewBurnedInfo)) {
            out.encodeInt(0);
        }
        out.encodeByte(calcDamageIndex);
        TreeMap<Integer, MobStat> indie = new TreeMap<>();
        for (MobStat s : removeList.keySet()) {
            Tms282MobStatBits.bit(s).filter(Tms282MobStatBits::isIndie).ifPresent(bit -> indie.put(bit, s));
        }
        for (int ignored : indie.keySet()) {
            out.encodeInt(0);
        }
        if (stats.hasMovementAffectingStat(removeList.keySet())) {
            out.encodeByte(0);
        }
        return out;
    }
}
