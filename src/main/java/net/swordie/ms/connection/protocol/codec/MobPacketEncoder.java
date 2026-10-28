package net.swordie.ms.connection.protocol.codec;

import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.enums.DeathType;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.life.mob.MobSkillAttackInfo;
import net.swordie.ms.life.mob.MobStat;
import net.swordie.ms.life.mob.skill.BurnedInfo;
import net.swordie.ms.life.movement.MovementInfo;

import java.util.Set;
import java.util.TreeMap;

/**
 * CMobPool / CMob server packets whose body differs between profiles. Signatures follow the 265
 * {@link net.swordie.ms.connection.packet.MobPool} builders so call sites can switch to
 * {@code Protocol.active().codecs().require(MobPacketEncoder.class)} one by one.
 * <p>
 * Codec family of {@link net.swordie.ms.connection.protocol.ProfileCodecs}.
 */
public interface MobPacketEncoder {

    /**
     * The active profile's encoder: the registered codec, or {@link Gms265MobPackets} (the {@code MobPool} builders
     * unchanged) when the profile registers none (GMS 265). The mob call sites of the 265 game logic go through this.
     */
    static MobPacketEncoder active() {
        return Protocol.active().codecs().find(MobPacketEncoder.class).orElse(Gms265MobPackets.INSTANCE);
    }

    /** MOB_ENTER_FIELD with an empty MobTemporaryStat (as 265 {@code MobPool.enterField}). */
    default OutPacket enterField(Mob mob, String linkTeam) {
        return enterField(mob, new TreeMap<>(), Set.of(), linkTeam);
    }

    /** MOB_ENTER_FIELD with the given MobTemporaryStat. */
    OutPacket enterField(Mob mob, TreeMap<MobStat, Option> stats, Set<BurnedInfo> burnedInfos, String linkTeam);

    OutPacket leaveField(int objectId, DeathType deathType);

    /** MOB_CHANGE_CONTROLLER level 0: stop controlling. */
    OutPacket changeController(int objectId);

    /** MOB_CHANGE_CONTROLLER level 2 (auto-aggro) with the full mob. */
    OutPacket changeController(Mob mob, String linkTeam);

    OutPacket controlAck(Mob mob, boolean nextAttackPossible, short mobCtrlSN, int skillId, int slv, int forcedAttack);

    /** MOB_MOVE broadcast to the other players. */
    OutPacket move(Mob mob, MobSkillAttackInfo msai, MovementInfo path, byte action);

    OutPacket damaged(int objectId, long damage, int templateId, byte type, int hp, long maxHp);

    OutPacket hpIndicator(int objectId, byte percent);

    OutPacket statSet(int objectId, byte calcDamageIndex, TreeMap<MobStat, Option> addList,
                      Set<BurnedInfo> burnedInfos, String linkTeam);

    OutPacket statReset(int objectId, byte calcDamageIndex, TreeMap<MobStat, Option> removeList,
                        Set<BurnedInfo> burnedInfos);
}
