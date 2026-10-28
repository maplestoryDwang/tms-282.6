package net.swordie.ms.life.mob;

import net.swordie.ms.connection.InPacket;
import net.swordie.ms.life.movement.MovementInfo;
import net.swordie.ms.util.Position;

/**
 * Decodes a MOB_MOVE body into a {@link MobMoveRequest} (strategy: one layout per protocol, as
 * {@link net.swordie.ms.world.migration.MigrateInDecoder}). The opcode has already been read. The TMS 282.3 layout is
 * {@link net.swordie.ms.connection.packet.Tms282MobMoveRequest}.
 */
@FunctionalInterface
public interface MobMoveRequestDecoder {
    MobMoveRequest decode(InPacket in);

    /**
     * GMS 265 (the reads of the former {@code MobHandler.handleMobMove}, in the same order): {@code I objId, S moveId,
     * B actionAndDirMask, B action, L targetInfo, B, B, B n + n x (S x, S y), B n + n x S, I index [!= 0: 11 x I],
     * B mask, 5 x I, B, MovementInfo (265 path), [B 5 x I B I B B B when bytes remain]}.
     */
    MobMoveRequestDecoder GMS_265 = in -> {
        int objectId = in.decodeInt();
        MobSkillAttackInfo msai = new MobSkillAttackInfo();
        short moveId = in.decodeShort();
        msai.actionAndDirMask = in.decodeByte();
        byte action = in.decodeByte();
        msai.action = (byte) (action >> 1);
        msai.flip = action % 2 != 0;
        long targetInfo = in.decodeLong();
        msai.skillID = (short) (targetInfo & 0xFFFFL);
        msai.slv = (short) (targetInfo >> 16L & 0xFFFFL);
        msai.targetInfo = (int) (targetInfo >> 32L & 0xFFFFL);
        in.decodeByte(); // v263 | 0
        in.decodeByte(); // v263 | 0
        byte multiTargetForBallSize = in.decodeByte();
        for (int i = 0; i < multiTargetForBallSize; i++) {
            Position pos = in.decodePosition(); // list of ball positions
            msai.multiTargetForBalls.add(pos);
        }
        byte randTimeForAreaAttackSize = in.decodeByte();
        for (int i = 0; i < randTimeForAreaAttackSize; i++) {
            msai.randTimeForAreaAttacks.add(in.decodeShort()); // could be used for cheat detection
        }
        int index = in.decodeInt();
        if (index != 0) {
            msai.unkMobMovement = new MobSkillAttackInfo.UnkMobMovement(index, in.decodeInt(), in.decodeInt(),
                    in.decodeInt(), in.decodeInt(), in.decodeInt(), in.decodeInt(), in.decodeInt(), in.decodeInt(),
                    in.decodeInt(), in.decodeInt(), in.decodeInt());
        }
        in.decodeByte(); // mask: bit 0 targetUserIDFromSvr, bit 4 isCheatMobMoveRand
        in.decodeInt(); // hackedCode
        in.decodeInt(); // oneTimeActionCS
        in.decodeInt(); // moveActionCS
        in.decodeInt(); // hitExpire
        in.decodeInt();
        in.decodeByte();
        MovementInfo path = new MovementInfo(in);
        if (in.getUnreadAmount() > 0) {
            in.decodeByte();
            for (int i = 0; i < 5; i++) {
                in.decodeInt();
            }
            in.decodeByte();
            in.decodeInt();
            in.decodeByte();
            in.decodeByte();
            in.decodeByte();
        }
        return new MobMoveRequest(objectId, moveId, msai, action, path);
    };
}
