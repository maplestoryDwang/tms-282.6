package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.info.AttackInfo;
import net.swordie.ms.client.character.skills.info.MobAttackInfo;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.packet.Tms282SummonAttackRequest;
import net.swordie.ms.connection.packet.Tms282SummonRequests;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.codec.MovePathCodec;
import net.swordie.ms.connection.protocol.codec.Tms282AttackDecoder;
import net.swordie.ms.connection.protocol.codec.Tms282MovePathCodec;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.handlers.life.SummonedHandler;
import net.swordie.ms.handlers.user.AttackHandler;
import net.swordie.ms.life.Life;
import net.swordie.ms.life.Summon;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.world.field.Field;

import java.nio.charset.Charset;
import java.util.Objects;

/**
 * TMS 282.3 summon requests on the channel socket after SetField; the 265 summon logic does the work:
 * <ul>
 *     <li>CP02E8 SUMMONED_ATTACK ({@link Tms282SummonAttackRequest}) -> the 265 {@link AttackInfo} of
 *     AttackHandler.handleSummonedAttack -> {@link Tms282CombatGuard} -> 265 {@code AttackHandler.handleAttack}
 *     (mob damage, death, drops, EXP; LP0671 SUMMONED_ATTACK to the field through the combat translator).</li>
 *     <li>CP02E7 SUMMONED_MOVE: the path moves the summon (265 {@code MovementInfo.applyTo}) and LP0670 goes to the
 *     field ({@link Tms282SummonRequests#summonedMove}).</li>
 *     <li>CP02E9 SUMMONED_HIT, CP02EA SUMMONED_SKILL, CP02EC SUMMONED_REMOVE: 265 bodies to the 265
 *     SummonedHandler.</li>
 * </ul>
 * A request that does not parse, or a summon that is not the character's, is logged and dropped.
 */
final class Tms282SummonHandlers {
    private final Charset charset;
    private final Tms282CombatGuard guard;

    Tms282SummonHandlers(Tms282LoginContext context) {
        this(context, Tms282CombatSettings.load());
    }

    Tms282SummonHandlers(Tms282LoginContext context, Tms282CombatSettings settings) {
        this.charset = Objects.requireNonNull(context, "context").settings().nameCharset();
        this.guard = new Tms282CombatGuard(settings);
    }

    void register(Tms282Routes routes) {
        routes.channel(Tms282SummonAttackRequest.OPCODE, InHeader.SUMMONED_ATTACK).when(Tms282Guards.IN_FIELD)
                .handle("summon attack -> 265 damage/death/drops/EXP, LP0671 to the field", this::onAttack);
        routes.channel(Tms282SummonRequests.MOVE, InHeader.SUMMONED_MOVE).when(Tms282Guards.IN_FIELD)
                .handle("summon move -> LP0670 to the field", this::onMove);
        routes.channel(Tms282SummonRequests.HIT, InHeader.SUMMONED_HIT).when(Tms282Guards.IN_FIELD)
                .handle("summon hit -> 265 SummonedHandler", this::onHit);
        routes.channel(Tms282SummonRequests.SKILL, InHeader.SUMMONED_SKILL).when(Tms282Guards.IN_FIELD)
                .handle("summon skill -> 265 SummonedHandler", this::onSkill);
        routes.channel(Tms282SummonRequests.REMOVE, InHeader.SUMMONED_REMOVE).when(Tms282Guards.IN_FIELD)
                .handle("summon remove -> 265 SummonedHandler (LP066F)", this::onRemove);
    }

    /** The character's summon with {@code objectId}, or null. */
    static Summon ownSummon(Char chr, int objectId) {
        Field field = chr.getField();
        Life life = field == null ? null : field.getLifeByObjectID(objectId);
        return life instanceof Summon summon && summon.getOwnerId() == chr.getId() ? summon : null;
    }

    private void onAttack(Tms282LoginState session, int op, byte[] packet) {
        Char chr = session.fieldCharacter();
        Tms282SummonAttackRequest request;
        try {
            request = Tms282SummonAttackRequest.parse(packet, charset, Tms282AttackDecoder.mobsOf(chr));
        } catch (IllegalArgumentException e) {
            System.out.printf("[TMS282] REJECTED CP02E8 len=%d: %s%n", packet.length, e.getMessage());
            return;
        }
        Summon summon = ownSummon(chr, request.summonObjectId());
        if (summon == null) {
            System.out.println("[TMS282] DROPPED " + request + " character=" + chr.getId() + ": not the character's summon");
            return;
        }
        AttackInfo ai = request.toAttackInfo();
        try { Tms282ExSummonHandlers.beforeAttack(chr,request); }
        catch(IllegalArgumentException e) {
            System.out.printf("[TMS282] REJECTED CP02E8 EX lifecycle character=%d: %s%n",chr.getId(),e.getMessage());
            return;
        }
        ai.summon = summon;
        ai.skillInfo = SkillData.getSkillInfoById(request.skillId());
        if (ai.skillInfo == null && request.skillId() != 0) {
            System.out.println("[TMS282] DROPPED " + request + ": skill without 265 data");
            return;
        }
        int totalCr = critRate(chr);
        for (MobAttackInfo mai : ai.mobAttackInfo) {
            mai.crits = AttackHandler.calculateCriticalsByAverage(mai.damages, totalCr);
        }
        Tms282CombatGuard.Verdict verdict = guard.inspect(chr, ai);
        String summary = request + " character=" + chr.getId() + " total=" + ai.totalDamageDealt;
        if (!verdict.clean()) {
            System.out.println("[TMS282] COMBAT CHECK " + summary + " " + verdict.findings()
                    + (verdict.apply() ? " (logged only)" : " (attack not applied)"));
        }
        if (!verdict.apply()) {
            return;
        }
        try {
            AttackHandler.handleAttack(chr, ai);
        } catch (RuntimeException | LinkageError e) {
            System.out.println("[TMS282] FAILED " + summary + ": " + e);
            return;
        }
        System.out.println("[TMS282] " + summary + " -> 265 attack queued; " + String.join("; ", request.notes()));
    }

    private void onMove(Tms282LoginState session, int op, byte[] packet) {
        Char chr = session.fieldCharacter();
        MovePathCodec paths = Protocol.active().codecs().find(MovePathCodec.class).orElse(Tms282MovePathCodec.INSTANCE);
        Tms282SummonRequests.Move request;
        try {
            request = Tms282SummonRequests.Move.parse(packet, paths);
        } catch (IllegalArgumentException e) {
            System.out.printf("[TMS282] REJECTED CP02E7 len=%d: %s%n", packet.length, e.getMessage());
            return;
        }
        Summon summon = ownSummon(chr, request.summonObjectId());
        if (summon == null) {
            return;
        }
        try {
            request.path().applyTo(summon);
            OutPacket move = Tms282SummonRequests.summonedMove(chr.getId(), summon.getObjectId(), request.path(), paths);
            Tms282RemoteAudience.FIELD_OTHERS.broadcast(chr, move);
        } catch (RuntimeException | LinkageError e) {
            System.out.println("[TMS282] FAILED CP02E7 summon " + request.summonObjectId() + ": " + e);
        }
    }

    private void onHit(Tms282LoginState session, int op, byte[] packet) {
        run(session, "CP02E9", () -> {
            Tms282SummonRequests.Hit r = Tms282SummonRequests.Hit.parse(packet);
            SummonedHandler.handleSummonedHit(session.fieldCharacter(), new InPacket(r.toGms265Body()));
            return r.toString();
        });
    }

    private void onSkill(Tms282LoginState session, int op, byte[] packet) {
        run(session, "CP02EA", () -> {
            Tms282SummonRequests.Skill r = Tms282SummonRequests.Skill.parse(packet);
            SummonedHandler.handleSummonedSkill(session.fieldCharacter(), new InPacket(r.toGms265Body()));
            return r.toString();
        });
    }

    private void onRemove(Tms282LoginState session, int op, byte[] packet) {
        run(session, "CP02EC", () -> {
            Tms282SummonRequests.Remove r = Tms282SummonRequests.Remove.parse(packet);
            SummonedHandler.handleSummonedRemove(session.fieldCharacter(), new InPacket(r.toGms265Body()));
            return r.toString();
        });
    }

    @FunctionalInterface
    interface Step {
        String run();
    }

    private static void run(Tms282LoginState session, String name, Step step) {
        try {
            System.out.println("[TMS282] " + name + " " + step.run());
        } catch (IllegalArgumentException e) {
            System.out.println("[TMS282] REJECTED " + name + ": " + e.getMessage());
        } catch (RuntimeException | LinkageError e) {
            System.out.println("[TMS282] FAILED " + name + ": " + e);
        }
    }

    private static int critRate(Char chr) {
        try {
            return chr.getTotalStat(net.swordie.ms.enums.BaseStat.cr);
        } catch (RuntimeException e) {
            return 0;
        }
    }
}
