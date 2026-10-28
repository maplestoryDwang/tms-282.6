package net.swordie.ms.handlers.login.tms282;

import it.unimi.dsi.fastutil.ints.Int2LongMap;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.tms282.Tms282BuffBook;
import net.swordie.ms.client.character.tms282.Tms282SkillService;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.packet.Tms282SkillUpRequest;
import net.swordie.ms.connection.packet.Tms282SkillUseRequest;
import net.swordie.ms.connection.packet.Tms282StatPackets;
import net.swordie.ms.handlers.header.InHeader;

import java.util.Objects;

/**
 * TMS 282.3 skill packets on the channel socket after LP02EF (the research probe's onSkillUp / onSkillUse /
 * armBuffExpiry), on the skill agent's {@link Tms282SkillService} ({@link Tms282SkillRuntime}):
 * <ul>
 *     <li>CP0196 USER_SKILL_UP_REQUEST: 265 UserStatHandler rules; skill row + characterstats written; LP00A2 +
 *     LP009D(sp), or LP009D unlock. The skill goes into the 265 {@code Char} skill map ({@code putSkill}).</li>
 *     <li>CP04B4 USER_SKILL_USE_REQUEST: MP/HP cost, LP0662 cooldown, LP009E buff, LP009D hp/mp unlock last; buff ends
 *     are sent as LP009F by the socket's {@link Tms282SessionBuffs} timer (cancelled on disconnect).</li>
 * </ul>
 * A malformed request or a failing service is answered with LP009D unlock, as in the probe.
 * <p>
 * Formal-server additions: the 265 {@code Char.skillCoolTimes} and the session's buff book agree. Before a use,
 * a 265 cooldown the book does not know (e.g. loaded from the database) is copied into it, so the service rejects
 * the use; after an accepted use, the cooldown it started is written into {@code Char.addSkillCoolTime}, so 265 code
 * and the 265 disconnect save see it. Buffs stay in the session's book (not in the 265 TemporaryStatManager, whose
 * packets have no 282 codec). The other users of the field get the outcome's field packets (LP048E SkillUse,
 * LP048F; LP0490 when the buff ends) through {@link Tms282RemoteAudience#FIELD_OTHERS}.
 * The hp/mp a use costs are written by {@link Tms282HpMpSaveTimer} (probe hpMpDirty).
 */
final class Tms282SkillHandlers {
    private final Tms282FieldServices services;
    /** skills phase: the 265 job handler's server-side effects after an accepted use (summons, areas, heals...). */
    private final Tms282SkillEffects effects;

    Tms282SkillHandlers(Tms282LoginContext context) {
        this(context, Tms282SkillSettings.load());
    }

    Tms282SkillHandlers(Tms282LoginContext context, Tms282SkillSettings settings) {
        this.services = Objects.requireNonNull(context, "context").fieldServices();
        this.effects = new Tms282SkillEffects(settings);
    }

    void register(Tms282Routes routes) {
        routes.channel(Tms282SkillUpRequest.OPCODE, InHeader.USER_SKILL_UP_REQUEST).when(Tms282Guards.IN_FIELD)
                .handle("skill up -> LP00A2 + LP009D(sp)", this::onSkillUp);
        routes.channel(Tms282SkillUseRequest.OPCODE, InHeader.USER_SKILL_USE_REQUEST).when(Tms282Guards.IN_FIELD)
                .handle("skill use -> LP0662 / LP009E / LP009D", this::onSkillUse);
        routes.channel(net.swordie.ms.connection.packet.Tms282AdeleShardPackets.REPLY,InHeader.USER_AREA_EXPLOSION_REQUEST)
                .when(Tms282Guards.IN_FIELD).handle("owned Adele shard geometry -> affected areas",this::onAdeleShards);
    }

    private void onAdeleShards(Tms282LoginState session,int op,byte[] packet) {
        Char chr=session.fieldCharacter();
        if(!(chr.getJobHandler() instanceof net.swordie.ms.client.jobs.flora.Adele adele))return;
        try {
            var reply=net.swordie.ms.connection.packet.Tms282AdeleShardPackets.Reply.parse(packet);
            if(!adele.shardbreaker().complete(reply,services.skills().clock().getAsLong()))
                System.out.println("[TMS282] REJECTED CP0137: no matching owned Adele shard cast");
        } catch(IllegalArgumentException e) {
            System.out.println("[TMS282] REJECTED CP0137: "+e.getMessage());
        }
    }

    private void onSkillUp(Tms282LoginState session, int op, byte[] packet) {
        Tms282SkillUpRequest request;
        try {
            request = Tms282SkillUpRequest.parse(packet);
        } catch (IllegalArgumentException e) {
            System.out.printf("[TMS282] REJECTED CP0196 len=%d: %s; SENT LP009D unlock%n", packet.length, e.getMessage());
            session.send(Tms282StatPackets.unlock());
            return;
        }
        Char chr = session.fieldCharacter();
        Tms282SkillService.Outcome outcome;
        try {
            outcome = services.skills().service().skillUp(chr, request);
        } catch (RuntimeException | LinkageError e) {
            System.out.println("[TMS282] FAILED CP0196 skill=" + request.skillId() + ": " + e + "; SENT LP009D unlock");
            session.send(Tms282StatPackets.unlock());
            return;
        }
        for (OutPacket p : outcome.packets()) {
            session.send(p);
        }
        System.out.println("[TMS282] " + outcome.log() + " character=" + chr.getId());
    }

    private void onSkillUse(Tms282LoginState session, int op, byte[] packet) {
        Tms282SkillUseRequest request;
        net.swordie.ms.client.character.skills.info.SkillUseInfo use;
        try {
            request = Tms282SkillUseRequest.parse(packet);
            use=Tms282SkillEffects.validate(session.fieldCharacter(),request);
        } catch (IllegalArgumentException e) {
            System.out.printf("[TMS282] REJECTED CP04B4 len=%d: %s; SENT LP009D unlock%n", packet.length, e.getMessage());
            session.send(Tms282StatPackets.unlock());
            return;
        }
        Char chr = session.fieldCharacter();
        if(use!=null&&request.skillId()==net.swordie.ms.client.jobs.flora.Adele.AETHER_CRYSTAL
                &&net.swordie.ms.connection.packet.Tms282AdeleShardPackets.handles(use.skillId)){
            boolean accepted=chr.getJobHandler() instanceof net.swordie.ms.client.jobs.flora.Adele adele
                    &&adele.shardbreaker().crystal(use,services.skills().clock().getAsLong());
            session.send(Tms282StatPackets.unlock());
            System.out.println("[TMS282] "+(accepted?"ACCEPTED":"REJECTED")+" CP04B4 owned Adele crystal continuation");
            return;
        }
        Tms282SessionBuffs buffs = session.buffs();
        Tms282SkillRuntime skills = services.skills();
        Tms282SkillService.Outcome outcome;
        try {
            Tms282SkillService service = skills.service();
            long now = skills.clock().getAsLong();
            cooldownFrom265(chr, buffs.book(), request.skillId(), now);
            outcome = service.useSkill(chr, buffs.book(), request);
            if (outcome.accepted()) {
                cooldownTo265(chr, buffs.book(), request.skillId(), now);
            }
        } catch (RuntimeException | LinkageError e) {
            System.out.println("[TMS282] FAILED " + request + ": " + e + "; SENT LP009D unlock");
            session.send(Tms282StatPackets.unlock());
            return;
        }
        for (OutPacket p : outcome.packets()) {
            session.send(p);
        }
        for (OutPacket p : outcome.fieldPackets()) {
            Tms282RemoteAudience.FIELD_OTHERS.broadcast(chr, p);
        }
        String effectLog = "";
        if (outcome.accepted()) {
            session.hpMpSaveTimer(services).markDirty();
            buffs.arm(session, chr.getId(), skills.service(), skills.clock());
            effectLog = "; " + effects.afterAccepted(chr, request).log();
        }
        System.out.println("[TMS282] " + outcome.log() + " character=" + chr.getId() + " " + request + effectLog);
    }

    /** A 265 cooldown still running that the book lacks is copied into the book. */
    static void cooldownFrom265(Char chr, Tms282BuffBook book, int skillId, long now) {
        Int2LongMap times = chr.getSkillCoolTimes();
        long until = times == null ? 0 : times.get(skillId);
        if (until > now && !book.coolingDown(skillId, now)) {
            book.startCooldown(skillId, until);
        }
    }

    /** The cooldown the service started goes into the 265 Char (next usable time in epoch milliseconds). */
    static void cooldownTo265(Char chr, Tms282BuffBook book, int skillId, long now) {
        long remaining = book.cooldownRemainingMillis(skillId, now);
        if (remaining > 0 && chr.getSkillCoolTimes() != null) {
            chr.addSkillCoolTime(skillId, now + remaining);
        }
    }
}
