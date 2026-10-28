package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.character.tms282.Tms282HiddenSkillParents;
import net.swordie.ms.client.jobs.adventurer.Kinesis;
import net.swordie.ms.client.jobs.anima.Ren;
import net.swordie.ms.client.jobs.resistance.Blaster;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.packet.*;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.handlers.life.SummonedHandler;
import net.swordie.ms.handlers.user.SkillHandler;
import net.swordie.ms.loaders.SkillData;
import static net.swordie.ms.client.character.skills.SkillStat.ppCon;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.RenPlumSwordForm3Shoot;

/** Multi-job capture batch 1007. Native requests are validated before the existing job logic runs. */
final class Tms282SkillAuxHandlers {
    void register(Tms282Routes routes) {
        routes.channel(0x03CB, InHeader.SHOOT_OBJECT_CREATE_REQUEST).when(Tms282Guards.IN_FIELD)
                .handle("native shoot objects -> job state, LP04B2 to other players", (s, op, p) -> guarded(s, op, p, () -> {
                    var r = Tms282ShootObjectRequest.parse(p);
                    var chr = s.fieldCharacter();
                    int level = Tms282HiddenSkillParents.authorisedLevel(chr, r.skillId());
                    if (level <= 0 || level != r.slv()) throw new IllegalArgumentException("Unowned shoot skill/level");
                    if ((r.skillId() == Ren.RIOTOUS_HEART || r.skillId() == Ren.HEXA_RIOTOUS_HEART)
                            && !chr.getTemporaryStatManager().hasStat(RenPlumSwordForm3Shoot)) return;
                    var info = r.info(chr);
                    if (r.skillId() == Kinesis.MIND_OVER_MATTER && chr.getJobHandler() instanceof Kinesis kinesis)
                        kinesis.substractPP(SkillData.getSkillInfoById(r.skillId()).getValue(ppCon, level));
                    chr.getJobHandler().handleShootObject(chr, info);
                    net.swordie.ms.client.character.tms282.Tms282ProjectileState.created(chr, info);
                    chr.getField().broadcast(r.remote(info), chr);
                    chr.dispose();
                }));
        routes.channel(0x04BA, InHeader.USER_SKILL_MOVING_SHOOT_ATTACK_PREPARE_REQUEST).when(Tms282Guards.IN_FIELD)
                .handle("moving-shoot prepare (without transport trailer) -> 265 SkillHandler", (s, op, p) -> guarded(s, op, p, () -> {
                    var r = Tms282SkillAuxRequests.MovingPrepare.parse(p);
                    var chr = s.fieldCharacter();
                    if (chr.getSkillLevel(r.skillId()) <= 0) throw new IllegalArgumentException("Unowned moving-shoot skill");
                    OutPacket body = new OutPacket(); body.encodeInt(r.skillId());
                    SkillHandler.handleUsermovingShootAttackPrepareRequest(chr, new InPacket(body.getData()));
                }));
        routes.channel(0x040B, InHeader.BULLET_BLAST_RELOAD_REQUEST).when(Tms282Guards.IN_FIELD)
                .handle("Blaster Bullet Blast reload -> 265 SkillHandler", (s, op, p) -> guarded(s, op, p, () -> {
                    var r = Tms282SkillAuxRequests.BulletReload.parse(p);
                    var chr = s.fieldCharacter();
                    if (!(chr.getJobHandler() instanceof Blaster) || r.skillId() != Blaster.BULLET_BLAST
                            || chr.getSkillLevel(r.skillId()) <= 0) throw new IllegalArgumentException("Unowned Blaster reload");
                    OutPacket body = new OutPacket(); body.encodeInt(r.skillId()); body.encodeByte(r.flag());
                    SkillHandler.handleBulletBlastReloadRequest(chr, new InPacket(body.getData()));
                }));
        routes.channel(0x02F0, InHeader.SUMMONED_ACTION).when(Tms282Guards.IN_FIELD)
                .handle("owned summon action -> LP067A", (s, op, p) -> guarded(s, op, p, () -> {
                    var r = Tms282SkillAuxRequests.SummonAction.parse(p);
                    var chr = s.fieldCharacter();
                    if (Tms282SummonHandlers.ownSummon(chr, r.objectId()) == null)
                        throw new IllegalArgumentException("Unowned summon");
                    OutPacket body = new OutPacket(); body.encodeInt(r.objectId()); body.encodeByte(r.action());
                    SummonedHandler.handleSummonedAction(chr, new InPacket(body.getData()));
                }));
        routes.channel(0x02F2, InHeader.SUMMONED_SKILL_ATTACK).when(Tms282Guards.IN_FIELD)
                .handle("owned summon reaction -> 265 SummonedHandler", (s, op, p) -> guarded(s, op, p, () -> {
                    var r = Tms282SkillAuxRequests.SummonReaction.parse(p);
                    var chr = s.fieldCharacter();
                    var summon = Tms282SummonHandlers.ownSummon(chr, r.objectId());
                    if (summon == null || summon.getSkillID() != r.summonSkill()
                            || Tms282HiddenSkillParents.authorisedLevel(chr, r.attackSkill()) <= 0
                            || Tms282HiddenSkillParents.authorisedLevel(chr, r.triggerSkill()) <= 0)
                        throw new IllegalArgumentException("Unowned summon reaction");
                    OutPacket body = new OutPacket(); body.encodeInt(r.objectId()); body.encodeInt(r.tick());
                    if (Tms282SummonLifecycleHandlers.reaction(chr, r)) return;
                    body.encodeInt(r.summonSkill()); body.encodeInt(r.attackSkill());
                    SummonedHandler.handleSummonedSkillAttack(chr, new InPacket(body.getData()));
                }));
    }

    private static void guarded(Tms282LoginState s, int op, byte[] p, Runnable action) {
        try { action.run(); }
        catch (IllegalArgumentException e) { Tms282SocialHandlers.rejected(op, p, e); s.fieldCharacter().dispose(); }
        catch (RuntimeException | LinkageError e) {
            System.out.printf("[TMS282] FAILED CP%04X character=%d: %s%n", op, s.fieldCharacter().getId(), e);
            s.fieldCharacter().dispose();
        }
    }
}
