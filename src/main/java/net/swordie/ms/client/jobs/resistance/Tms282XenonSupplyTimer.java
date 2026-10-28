package net.swordie.ms.client.jobs.resistance;

import net.swordie.ms.client.Client;
import net.swordie.ms.client.User;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.packet.Tms282SkillStackTranslators;
import net.swordie.ms.connection.packet.Tms282XenonSupplyTimerRequest;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.handlers.login.tms282.Tms282LoginState;
import net.swordie.ms.world.field.Field;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.SurplusSupply;

/** Exact native82F58D period, owned409 receipt, and existing server+1 quantity/cap. */
public final class Tms282XenonSupplyTimer {
    public static final long INTERVAL_MILLIS = 2000;
    private final Xenon job;
    private final Char owner;
    private Client client;
    private User user;
    private int userId;
    private Field field;
    private long lastEarned;
    private int lastTick;

    Tms282XenonSupplyTimer(Xenon job, Char owner) { this.job = job; this.owner = owner; }

    public synchronized void clear() {
        client = null; user = null; userId = 0; field = null; lastEarned = 0; lastTick = 0;
    }

    public synchronized int pulse(Tms282XenonSupplyTimerRequest request, long now) {
        Client current = owner.getClient();
        if (Protocol.active().version() != ProtocolVersion.TMS_282_3 || now <= 0
                || owner.getJobHandler() != job || !JobConstants.isXenon(owner.getJob()) || owner.getJob() < 3600
                || owner.getId() <= 0 || owner.getHP() <= 0 || owner.getField() == null
                || owner.getField().getCharByID(owner.getId()) != owner || current == null || current.getChr() != owner
                || current.getCurrentState() != Client.IN_FIELD || current.getNextState() != Client.NONE
                || !current.getCh().isActive() || current.getUser() == null || current.getUser().getId() <= 0
                || current.getUser() != owner.getUser() || !Tms282LoginState.of(current).isChannel()
                || Tms282LoginState.of(current).fieldCharacter() != owner
                || !owner.hasSkill(Xenon.SUPPLY_SURPLUS) || owner.getSkillLevel(Xenon.SUPPLY_SURPLUS) <= 0)
            throw new IllegalArgumentException("Unowned Xenon supply timer");
        if (client != current || field != owner.getField() || user != current.getUser() || userId != current.getUser().getId()) clear();
        if (request.start()) {
            if (client == null) {
                client = current; field = owner.getField(); user = current.getUser(); userId = user.getId();
                lastEarned = now; lastTick = request.clientTick();
            }
            return 0;
        }
        if (client == null) throw new IllegalArgumentException("Missing Xenon supply timer start");
        int distance = request.clientTick() - lastTick;
        if (distance < 0) throw new IllegalArgumentException("Stale Xenon supply timer tick");
        int before = owner.getTemporaryStatManager().getOption(SurplusSupply).nOption;
        if (distance > 0) {
            lastTick = request.clientTick();
            if (now >= lastEarned && now - lastEarned >= INTERVAL_MILLIS) {
                lastEarned = now; // One native pending request; no credit for a disconnected/backlogged interval.
                job.incrementSupply(1);
            }
        }
        // 6001F40 reads a bool, not the supply value. Zero supply must still rearm this passive unit.
        owner.write(Tms282SkillStackTranslators.stackResult(Xenon.SUPPLY_SURPLUS, 1));
        return owner.getTemporaryStatManager().getOption(SurplusSupply).nOption - before;
    }
}
