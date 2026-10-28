package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.tms282.Tms282HiddenSkillParents;
import net.swordie.ms.client.character.tms282.Tms282ProjectileState;
import net.swordie.ms.client.jobs.Job;
import net.swordie.ms.client.jobs.flora.Illium;
import net.swordie.ms.connection.packet.Tms282ProjectileMotionRequest;
import net.swordie.ms.connection.packet.Tms282SkillAuxRequests;
import net.swordie.ms.connection.packet.Tms282SummonSequenceRequest;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;
import net.swordie.ms.client.character.tms282.Tms282SkillLearning;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.handlers.header.InHeader;

/** Native 1007 motion/sequence reports update owned state and synchronize other players. Damage remains CP02E8. */
public final class Tms282SummonLifecycleHandlers {
    void register(Tms282Routes routes, InHeader projectileMotion) {
        routes.channel(Tms282ProjectileMotionRequest.OPCODE, projectileMotion).when(Tms282Guards.IN_FIELD)
                .handle("owned physical-object motion -> LP041E", (s, op, p) -> guarded(s, op, p,
                        () -> motion(s.fieldCharacter(), Tms282ProjectileMotionRequest.parse(p))));
        routes.channel(Tms282SummonSequenceRequest.OPCODE, InHeader.SUMMONED_SEQUENCE_ATTACK).when(Tms282Guards.IN_FIELD)
                .handle("owned mirror-spider sequence -> LP0688", (s, op, p) -> guarded(s, op, p,
                        () -> sequence(s.fieldCharacter(), Tms282SummonSequenceRequest.parse(p))));
    }

    public static void motion(Char chr, Tms282ProjectileMotionRequest request) {
        Tms282ProjectileState.motion(chr, request);
        chr.getField().broadcast(request.remote(chr.getId()), chr);
    }

    public static void sequence(Char chr, Tms282SummonSequenceRequest request) {
        if (Protocol.active().version() != ProtocolVersion.TMS_282_3)
            throw new IllegalArgumentException("Summon sequence requires TMS282");
        var summon = Tms282SummonHandlers.ownSummon(chr, request.objectId());
        if (summon == null || summon.getSkillID() != request.skillId() || summon.getSlv() != request.level()
                || chr.getSkillLevel(Job.TRUE_ARACHNID_REFLECTION) != request.level())
            throw new IllegalArgumentException("Unowned mirror-spider sequence");
        if (summon.getTms282SequencePhase() == request.phase()) return; // repeat reports do not restart the sequence
        if (request.phase() == 2 && summon.getTms282SequencePhase() == 0)
            throw new IllegalArgumentException("Mirror-spider end without start");
        summon.setTms282SequencePhase(request.phase());
        chr.getField().broadcast(request.remote(chr.getId()), chr);
    }

    /** CP02F2 VI reaction reports previously fell through Summon.onAttack's unhandled default.
     * Returns false for other skills so their existing handler remains in use.
     */
    public static boolean reaction(Char chr, Tms282SkillAuxRequests.SummonReaction request) {
        if (Protocol.active().version() != ProtocolVersion.TMS_282_3) return false;
        if (request.attackSkill() != 152141007 && request.attackSkill() != 152141008) return false;
        var summon = Tms282SummonHandlers.ownSummon(chr, request.objectId());
        boolean destruction = request.attackSkill() == 152141007;
        boolean trigger = destruction
                ? request.triggerSkill() == Illium.RADIANT_JAVELIN || request.triggerSkill() == Illium.RADIANT_JAVELIN_II
                    || request.triggerSkill() == Illium.LONGINUS_SPEAR
                : request.triggerSkill() == Illium.RADIANT_ORB || request.triggerSkill() == Illium.RADIANT_ORB_II
                    || request.triggerSkill() == 152141014;
        int level = Tms282HiddenSkillParents.authorisedLevel(chr, request.attackSkill());
        if (!(chr.getJobHandler() instanceof Illium) || summon == null || summon.getSkillID() != request.summonSkill()
                || (request.summonSkill() != Illium.DEPLOY_CRYSTAL && request.summonSkill() != Illium.CRYSTALLINE_SPIRIT)
                || !trigger || level <= 0 || Tms282HiddenSkillParents.authorisedLevel(chr, request.triggerSkill()) <= 0
                || !Tms282SkillLearning.requirementsMet(chr, SkillData.getSkillInfoById(request.attackSkill())))
            throw new IllegalArgumentException("Unowned Illium VI reaction");
        if (!chr.hasSkillOnCooldown(request.attackSkill())) chr.setSkillCooldown(request.attackSkill(), level);
        return true;
    }

    private static void guarded(Tms282LoginState state, int opcode, byte[] packet, Runnable action) {
        try { action.run(); }
        catch (IllegalArgumentException e) { Tms282SocialHandlers.rejected(opcode, packet, e); state.fieldCharacter().dispose(); }
        catch (RuntimeException | LinkageError e) {
            System.out.printf("[TMS282] FAILED CP%04X character=%d: %s%n", opcode, state.fieldCharacter().getId(), e);
            state.fieldCharacter().dispose();
        }
    }
}
