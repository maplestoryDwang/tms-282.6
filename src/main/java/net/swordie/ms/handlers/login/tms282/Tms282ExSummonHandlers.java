package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.jobs.flora.Illium;
import net.swordie.ms.connection.packet.Tms282ExSummonRequests;
import net.swordie.ms.connection.packet.Tms282SummonAttackRequest;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.life.Summon;
import net.swordie.ms.world.field.Field;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

/** Native EX attack lifecycle. CP02F0 already broadcasts animation; these reports authorize actual CP02E8. */
public final class Tms282ExSummonHandlers {
    // ponytail: one short global lock; split by summon if packet throughput requires it.
    private static final Map<Summon, Phase> PHASES = new WeakHashMap<>();
    private static final class Phase {
        final WeakReference<Client> client;
        final WeakReference<Field> field;
        long deadline, lastServerAttack;
        Integer lastTick;
        boolean active, attacked, cancelling;
        Phase(Char chr) { client = new WeakReference<>(chr.getClient()); field = new WeakReference<>(chr.getField()); }
    }

    void register(Tms282Routes routes) {
        routes.channel(Tms282ExSummonRequests.BEGIN, InHeader.SUMMONED_APOCALYPSE_CANNON_REQUEST).when(Tms282Guards.IN_FIELD)
                .handle("owned EX action begin -> authorize summon attacks", (s, op, p) -> guarded(s, op, p,
                        () -> begin(s.fieldCharacter(), Tms282ExSummonRequests.Begin.parse(p))));
        routes.channel(Tms282ExSummonRequests.END, InHeader.SUMMONED_ELEMENTAL_FURY_REQUEST).when(Tms282Guards.IN_FIELD)
                .handle("owned EX action end -> stop accepting summon attacks", (s, op, p) -> guarded(s, op, p,
                        () -> end(s.fieldCharacter(), Tms282ExSummonRequests.End.parse(p))));
        routes.channel(Tms282ExSummonRequests.CANCEL, InHeader.SUMMONED_ATTACK_PVP).when(Tms282Guards.IN_FIELD)
                .handle("owned EX action cancel -> revoke summon attack permission", (s, op, p) -> guarded(s, op, p,
                        () -> cancel(s.fieldCharacter(), Tms282ExSummonRequests.Cancel.parse(p))));
    }

    public static synchronized void begin(Char chr, Tms282ExSummonRequests.Begin request) {
        var summon = ownedEx(chr, request.objectId());
        if (request.sourceSkill() != summon.getSkillID()) throw new IllegalArgumentException("Foreign EX action skill");
        var phase = phase(chr, summon);
        long now = System.currentTimeMillis();
        if (phase != null && phase.active && phase.deadline > now) return; // repeats cannot extend the existing deadline
        if (phase != null && phase.cancelling && phase.deadline > now)
            throw new IllegalArgumentException("EX begin while waiting for cancelled-action end");
        if (phase == null) phase = new Phase(chr);
        phase.active = true;
        phase.attacked = false;
        phase.cancelling = false;
        phase.deadline = now + request.remainingMs();
        PHASES.put(summon, phase);
    }

    public static synchronized void end(Char chr, Tms282ExSummonRequests.End request) {
        var summon = ownedEx(chr, request.objectId());
        var phase = phase(chr, summon);
        if (phase == null || (!(phase.active && phase.attacked) && !phase.cancelling)
                || phase.deadline <= System.currentTimeMillis())
            throw new IllegalArgumentException("EX end without a current action");
        phase.active = false;
        phase.cancelling = false;
    }

    public static synchronized void cancel(Char chr, Tms282ExSummonRequests.Cancel request) {
        var summon = ownedEx(chr, request.objectId());
        var phase = phase(chr, summon);
        if (phase == null || !phase.active || phase.deadline <= System.currentTimeMillis())
            throw new IllegalArgumentException("EX cancel without a current action");
        phase.active = false;
        phase.cancelling = true; // Native sends one CP02F5 after cancellation; accept that final cleanup once.
    }

    /** Call in CP02E8 after ownership is established and before any damage is applied.
     * Unrelated summons remain on their existing path. Tick subtraction is unsigned modulo 2^32.
     */
    public static synchronized void beforeAttack(Char chr, Tms282SummonAttackRequest request) {
        if (Protocol.active().version() != ProtocolVersion.TMS_282_3) return;
        var summon = Tms282SummonHandlers.ownSummon(chr, request.summonObjectId());
        if (summon == null) throw new IllegalArgumentException("Unowned summon attack");
        if (summon.getSkillID() != Illium.EX && request.skillId() != Illium.EX) return;
        ownedEx(chr, request.summonObjectId());
        var phase = phase(chr, summon);
        long now = System.currentTimeMillis();
        if (request.skillId() != Illium.EX || phase == null || !phase.active || phase.deadline <= now)
            throw new IllegalArgumentException("EX attack outside its current action");
        if (phase.lastTick != null) {
            long delta = Integer.toUnsignedLong(request.updateTime() - phase.lastTick);
            long allowed = Math.min(Integer.MAX_VALUE, Tms282ExSummonRequests.MAX_REMAINING_MS
                    + Math.max(0, now - phase.lastServerAttack));
            if (delta == 0 || delta > allowed) throw new IllegalArgumentException("Replayed or invalid EX attack tick");
        }
        phase.lastTick = request.updateTime();
        phase.lastServerAttack = now;
        phase.attacked = true;
    }

    public static synchronized long deadline(Char chr, int objectId) {
        var summon = Tms282SummonHandlers.ownSummon(chr, objectId);
        var phase = summon == null ? null : phase(chr, summon);
        return phase == null || !phase.active ? 0 : phase.deadline;
    }

    private static Summon ownedEx(Char chr, int objectId) {
        var summon = Tms282SummonHandlers.ownSummon(chr, objectId);
        if (Protocol.active().version() != ProtocolVersion.TMS_282_3 || !JobConstants.isIllium(chr.getJob())
                || chr.getClient() == null || summon == null || summon.getSkillID() != Illium.EX
                || chr.getSkillLevel(Illium.EX) <= 0 || chr.getSkillLevel(Illium.EX) != summon.getSlv())
            throw new IllegalArgumentException("Unowned EX action");
        return summon;
    }

    private static Phase phase(Char chr, Summon summon) {
        var phase = PHASES.get(summon);
        if (phase != null && (phase.client.get() != chr.getClient() || phase.field.get() != chr.getField())) {
            PHASES.remove(summon);
            phase = null;
        }
        return phase;
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
