package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.tms282.Tms282SkillService;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.packet.Tms282SkillStateRequests;
import net.swordie.ms.connection.packet.Tms282CooldownSyncRequest;
import net.swordie.ms.connection.packet.Tms282SkillPackets;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.handlers.user.SkillHandler;

import java.util.Objects;

/**
 * TMS 282.3 skill state requests on the channel socket after SetField ({@link Tms282SkillStateRequests}):
 * <ul>
 *     <li>CP04B7 USER_SKILL_CANCEL_REQUEST: the buffs of the skill leave the socket's {@link Tms282SessionBuffs}
 *     book at once ({@link Tms282SkillService#cancel}: LP009F to the owner, LP0490 / LP048F to the field, as an
 *     expiry), then the 265 {@code SkillHandler.handleTemporaryStatResetRequest} runs for its job-specific part
 *     (key-down cool time, LP047E REMOTE_SKILL_CANCEL through the combat translator).</li>
 *     <li>CP04B8 USER_SKILL_PREPARE_STOP / CP04B9 USER_SKILL_PREPARE_START (key-down skills): the 265 handlers
 *     (LP047C REMOTE_SKILL_PREPARE / LP047E to the field through the combat translator).</li>
 * </ul>
 * A request that does not parse is logged and dropped; a failing 265 handler is logged. The 265 EXCL_REQUEST of a
 * refusal becomes the LP009D unlock through {@code Tms282UnlockScope}.
 */
final class Tms282SkillStateHandlers {
    private final Tms282FieldServices services;

    Tms282SkillStateHandlers(Tms282LoginContext context) {
        this.services = Objects.requireNonNull(context, "context").fieldServices();
    }

    void register(Tms282Routes routes) {
        routes.channel(net.swordie.ms.connection.packet.Tms282SkillEventRequests.CHECK_DURATION,
                        InHeader.USER_CHECK_TEMPORARY_STAT_DURATION_REQUEST).when(Tms282Guards.IN_FIELD)
                .handle("buff duration check -> LP009F for stats the server no longer runs", this::onCheckDuration);
        routes.channel(net.swordie.ms.connection.packet.Tms282SkillEventRequests.MOB_LIFTING_END,
                        InHeader.MOB_LIFTING_END).when(Tms282Guards.IN_FIELD)
                .handle("mob lifting end (i32 mob id, no reply; the 265 handler reads another body)", (s, op, p) -> {
                    try {
                        net.swordie.ms.connection.packet.Tms282SkillEventRequests.MobLiftingEnd.parse(p);
                    } catch (IllegalArgumentException e) {
                        System.out.printf("[TMS282] REJECTED CP0527 len=%d: %s%n", p.length, e.getMessage());
                    }
                });
        routes.channel(Tms282CooldownSyncRequest.OPCODE, InHeader.CLIENT_SYNC_COOLTIME_REQUEST).when(Tms282Guards.IN_FIELD)
                .handle("authoritative cooldown sync -> LP0662", this::onCooldownSync);
        routes.channel(Tms282SkillStateRequests.CANCEL, InHeader.USER_SKILL_CANCEL_REQUEST).when(Tms282Guards.IN_FIELD)
                .handle("skill cancel -> LP009F / LP0490 + 265 key-down cleanup", this::onCancel);
        routes.channel(Tms282SkillStateRequests.PREPARE_STOP, InHeader.USER_SKILL_PREPARE_STOP).when(Tms282Guards.IN_FIELD)
                .handle("key-down stop -> 265 handleUserSkillPrepareStop", this::onPrepareStop);
        routes.channel(Tms282SkillStateRequests.PREPARE_START, InHeader.USER_SKILL_PREPARE_START).when(Tms282Guards.IN_FIELD)
                .handle("key-down start -> 265 handleUserSkillPrepareStart", this::onPrepareStart);
    }

    private void onCooldownSync(Tms282LoginState session,int op,byte[] packet) {
        Tms282CooldownSyncRequest request;
        try { request=Tms282CooldownSyncRequest.parse(packet); }
        catch(IllegalArgumentException e) {
            System.out.printf("[TMS282] REJECTED CP0420 len=%d: %s%n",packet.length,e.getMessage());
            return;
        }
        Char chr=session.fieldCharacter();
        long now=services.skills().clock().getAsLong();
        var secondsBySkill=new java.util.LinkedHashMap<Integer,Integer>();
        for(int skill:request.remaining().keySet()) {
            if(SkillData.getSkillInfoById(skill)==null)continue;
            // A client may report expired/negative time; it never changes either server authority.
            long remaining=Math.max(Math.max(0,chr.getRemainingCoolTime(skill)),session.buffs().book().cooldownRemainingMillis(skill,now));
            int seconds=(int)Math.min(Integer.MAX_VALUE,remaining/1000+(remaining%1000==0?0:1));
            secondsBySkill.put(skill,seconds);
        }
        if(!secondsBySkill.isEmpty())session.send(Tms282SkillPackets.skillCooltimeSet(secondsBySkill));
    }

    private void onCancel(Tms282LoginState session, int op, byte[] packet) {
        Tms282SkillStateRequests.Cancel request;
        try {
            request = Tms282SkillStateRequests.Cancel.parse(packet);
        } catch (IllegalArgumentException e) {
            System.out.printf("[TMS282] REJECTED CP04B7 len=%d: %s%n", packet.length, e.getMessage());
            return;
        }
        Char chr = session.fieldCharacter();
        if (net.swordie.ms.client.jobs.Jianghu.Tms282HowlingStorm.stop(chr, request.skillId(), System.currentTimeMillis())) return;
        var protection = net.swordie.ms.client.jobs.special.SaitamaActionProtection.of(chr);
        if (net.swordie.ms.client.jobs.flora.Tms282AetherGuard.stop(chr, request.skillId(), System.currentTimeMillis())) return;
        if (net.swordie.ms.client.jobs.adventurer.thief.Tms282BladeTempest.stop(chr, request.skillId())) return;
        if (request.skillId() == net.swordie.ms.client.jobs.cygnus.BlazeWizard.INFERNO_SPHERE) {
            net.swordie.ms.client.jobs.cygnus.BlazeWizard.stopInferno(chr); return;
        }
        if (net.swordie.ms.client.character.tms282.Tms282DragonMaster.stop(chr, request.skillId(), System.currentTimeMillis())) return;
        if (net.swordie.ms.client.jobs.resistance.Tms282OmegaBlaster.stop(chr, request.skillId(), System.currentTimeMillis())) return;
        if (protection != null) protection.stop(chr, request.skillId());
        Tms282SessionBuffs buffs = session.buffs();
        String log;
        try {
            Tms282SkillRuntime skills = services.skills();
            Tms282SkillService.Outcome outcome = skills.service().cancel(buffs.book(), chr.getId(), request.skillId());
            for (OutPacket p : outcome.packets()) {
                session.send(p);
            }
            for (OutPacket p : outcome.fieldPackets()) {
                Tms282RemoteAudience.FIELD_OTHERS.broadcast(chr, p);
            }
            if (outcome.accepted()) {
                buffs.rearm();
            }
            log = outcome.log();
        } catch (RuntimeException | LinkageError e) {
            log = "282 buff cancel failed: " + e;
        }
        String legacy = run265(() -> SkillHandler.handleTemporaryStatResetRequest(chr,
                new InPacket(request.toGms265Body(CharacterTemporaryStat.length))));
        System.out.println("[TMS282] CP04B7 cancel skill " + request.skillId() + " character=" + chr.getId() + ": " + log
                + legacy);
    }

    private void onPrepareStop(Tms282LoginState session, int op, byte[] packet) {
        Tms282SkillStateRequests.PrepareStop request;
        try {
            request = Tms282SkillStateRequests.PrepareStop.parse(packet);
        } catch (IllegalArgumentException e) {
            System.out.printf("[TMS282] REJECTED CP04B8 len=%d: %s%n", packet.length, e.getMessage());
            return;
        }
        Char chr = session.fieldCharacter();
        if (net.swordie.ms.client.jobs.flora.Tms282AetherGuard.stop(chr, request.skillId(), System.currentTimeMillis())) return;
        if (net.swordie.ms.client.jobs.Jianghu.Tms282HowlingStorm.stop(chr, request.skillId(), System.currentTimeMillis())) return;
        if (net.swordie.ms.client.jobs.adventurer.thief.Tms282BladeTempest.stop(chr, request.skillId())) return;
        if (net.swordie.ms.client.jobs.resistance.Tms282OmegaBlaster.stop(chr, request.skillId(), System.currentTimeMillis())) return;
        if (request.skillId() == net.swordie.ms.client.jobs.cygnus.BlazeWizard.INFERNO_SPHERE) {
            net.swordie.ms.client.jobs.cygnus.BlazeWizard.stopInferno(chr); return;
        }
        if (net.swordie.ms.client.character.tms282.Tms282DragonMaster.stop(chr, request.skillId(), System.currentTimeMillis())) return;
        var protection = net.swordie.ms.client.jobs.special.SaitamaActionProtection.of(chr);
        if (protection != null) protection.stop(chr, request.skillId());
        String legacy = run265(() -> SkillHandler.handleUserSkillPrepareStop(chr, new InPacket(request.toGms265Body())));
        System.out.println("[TMS282] CP04B8 key-down stop skill " + request.skillId() + " character=" + chr.getId() + legacy);
    }

    private void onPrepareStart(Tms282LoginState session, int op, byte[] packet) {
        Tms282SkillStateRequests.PrepareStart request;
        try {
            request = Tms282SkillStateRequests.PrepareStart.parse(packet);
        } catch (IllegalArgumentException e) {
            System.out.printf("[TMS282] REJECTED CP04B9 len=%d: %s%n", packet.length, e.getMessage());
            return;
        }
        Char chr = session.fieldCharacter();
        if (net.swordie.ms.client.jobs.adventurer.thief.Tms282BladeTempest.handles(request.skillId())) {
            if (request.skillId()!=net.swordie.ms.client.jobs.adventurer.thief.Tms282BladeTempest.HELD
                    || !net.swordie.ms.client.jobs.adventurer.thief.Tms282BladeTempest.prepare(chr,session.buffs().book(),System.currentTimeMillis())) chr.dispose();
            else session.hpMpSaveTimer(services).markDirty();
            return;
        }
        if (request.skillId()==net.swordie.ms.client.jobs.flora.Adele.AETHER_GUARD) {
            if (!net.swordie.ms.client.jobs.flora.Tms282AetherGuard.prepare(chr,session.buffs().book(),System.currentTimeMillis())) chr.dispose();
            else session.hpMpSaveTimer(services).markDirty();
            return;
        }
        if (net.swordie.ms.client.jobs.Jianghu.Tms282HowlingStorm.handles(request.skillId())) {
            if (!net.swordie.ms.client.jobs.Jianghu.Tms282HowlingStorm.isHeld(request.skillId())
                    || !net.swordie.ms.client.jobs.Jianghu.Tms282HowlingStorm.prepare(chr, session.buffs().book(), request.skillId(), System.currentTimeMillis())) chr.dispose();
            else session.hpMpSaveTimer(services).markDirty();
            return;
        }
        if (net.swordie.ms.client.jobs.resistance.Tms282OmegaBlaster.handles(request.skillId())) {
            if(request.skillId()!=net.swordie.ms.client.jobs.resistance.Tms282OmegaBlaster.HELD
                    || !net.swordie.ms.client.jobs.resistance.Tms282OmegaBlaster.prepare(chr,session.buffs().book(),System.currentTimeMillis()))
                chr.dispose();
            else session.hpMpSaveTimer(services).markDirty();
            return;
        }
        if (request.skillId() == net.swordie.ms.client.jobs.cygnus.BlazeWizard.INFERNO_SPHERE) {
            if (!(chr.getJobHandler() instanceof net.swordie.ms.client.jobs.cygnus.BlazeWizard blaze)
                    || !blaze.prepareInferno(session.buffs().book(), System.currentTimeMillis())) chr.dispose();
            else session.hpMpSaveTimer(services).markDirty();
            return;
        }
        var protection = net.swordie.ms.client.jobs.special.SaitamaActionProtection.of(chr);
        if (protection != null && net.swordie.ms.client.jobs.special.SaitamaActionProtection.isKeyDown(request.skillId())
                && !protection.startKeyDown(chr, request.skillId(), System.currentTimeMillis())) {
            chr.dispose();
            return;
        }
        String legacy = run265(() -> SkillHandler.handleUserSkillPrepareStart(chr, new InPacket(request.toGms265Body())));
        System.out.println("[TMS282] CP04B9 key-down start " + request + " character=" + chr.getId() + legacy);
    }

    /** Runs a 265 handler; "" when it returned, else the failure for the log line. */
    static String run265(Runnable handler) {
        try {
            handler.run();
            return "";
        } catch (RuntimeException | LinkageError e) {
            return "; 265 handler failed: " + e;
        }
    }

    private void onCheckDuration(Tms282LoginState session, int op, byte[] packet) {
        net.swordie.ms.connection.packet.Tms282SkillEventRequests.CheckDuration request;
        try {
            request = net.swordie.ms.connection.packet.Tms282SkillEventRequests.CheckDuration.parse(packet);
        } catch (IllegalArgumentException e) {
            System.out.printf("[TMS282] REJECTED CP019D len=%d: %s%n", packet.length, e.getMessage());
            return;
        }
        Char chr = session.fieldCharacter();
        var tsm = chr.getTemporaryStatManager();
        long now = System.currentTimeMillis();
        tsm.update(now);                 // 265 expiry first (its own resets)
        session.buffs().rearm();         // CP04B4 book expiry
        long bookNow = services.skills().clock().getAsLong();
        net.swordie.ms.connection.packet.Tms282SkillEventDurationReply.reply(
                net.swordie.ms.connection.protocol.codec.ProfileEncoders
                        .find(net.swordie.ms.connection.packet.TemporaryStatEncoder.class).orElse(null),
                request, tsm.getCurrentStats(), now, session.buffs().book().snapshot(), bookNow)
                .ifPresent(session::send);
    }
}
