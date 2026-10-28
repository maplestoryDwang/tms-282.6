package net.swordie.ms.client.jobs.sengoku;

import net.swordie.ms.client.Client;
import net.swordie.ms.client.User;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.info.AttackInfo;
import net.swordie.ms.client.character.skills.info.SkillUseInfo;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.netty.NettyClient;
import net.swordie.ms.connection.packet.Tms282KannaOniPackets;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.client.character.tms282.Tms282BuffBook;
import net.swordie.ms.client.character.tms282.Tms282BuffDirectory;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import net.swordie.ms.handlers.login.tms282.Tms282LoginState;
import net.swordie.ms.world.field.Field;

import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.IndieBuffIcon;

/** One owner Kanna job instance. Native actor existence and the active summon skill are separate states. */
public final class Tms282KannaOniController {
    public static final int GHOST = 42001003;
    public static final int GHOST_VI = 42141003;
    public static final int BASIC = 42001004;
    public static final int DESCENT = 42001005;
    public static final int BASIC_VI = 42141004;
    public static final int DESCENT_VI = 42141005;
    private static final long CONTROLLER_BASE = 0x52000000L;
    private static final long ONI_BASE = 0x53000000L;

    private final Char owner;
    private final Kanna job;
    private Client client;
    private User user;
    private Field field;
    private int userId;
    private int characterId;
    private long controllerId;
    private long oniId;
    private int source;
    private long activeUntil;
    private long lastReserveAt;
    private record PaidCast(Client client, User user, Tms282BuffBook book, int userId, int characterId,
                            int job, int source, int level, Option icon, long expires) { }
    private PaidCast paidCast, reviveCast;
    private long castGeneration, reviveGeneration, lastCheckedAt;

    public Tms282KannaOniController(Char owner, Kanna job) {
        this.owner = owner;
        this.job = job;
    }

    public static boolean isGhostAttack(int skill) {
        return skill == BASIC || skill == DESCENT || skill == BASIC_VI || skill == DESCENT_VI;
    }

    private boolean currentOwner() {
        Client current = owner.getClient();
        return Protocol.active().version() == ProtocolVersion.TMS_282_3 && owner.getJobHandler() == job
                && JobConstants.isKanna(owner.getJob()) && owner.getId() > 0 && owner.getId() <= 0x00FFFFFF
                && owner.getField() != null && current != null && current.getChr() == owner
                && current.getCh() != null && current.getCh().isActive() && !owner.isBot()
                && current.getCurrentState() == Client.IN_FIELD && current.getUser() != null
                && current.getUser().getId() > 0 && owner.getUser() == current.getUser()
                && owner.getField().getCharByID(owner.getId()) == owner
                && Tms282LoginState.of(current).isChannel();
    }

    private boolean bound() {
        return currentOwner() && client == owner.getClient() && field == owner.getField()
                && user == client.getUser() && userId == user.getId() && characterId == owner.getId()
                && controllerId == CONTROLLER_BASE + characterId && oniId == ONI_BASE + characterId;
    }

    /** Netty hook only after the native SetField passed the outbound guard and entered the socket queue. */
    public static void afterSent(NettyClient netty, OutPacket packet) {
        if (Protocol.active().version() != ProtocolVersion.TMS_282_3 || packet.getHeader() != 0x02EF
                || packet.getBuiltFor() != ProtocolVersion.TMS_282_3 || !(netty instanceof Client client)
                || !client.getCh().isActive() || client.getChr() == null
                || client.getChr().getClient() != client
                || !(client.getChr().getJobHandler() instanceof Kanna kanna)) return;
        kanna.getOniController().enterField();
    }

    public synchronized boolean enterField() {
        if (!currentOwner()) return false;
        long now = System.currentTimeMillis();
        PaidCast resume = restorableCast(now);
        if (resume == null) cancel(); else revokeActors();
        long generation = castGeneration;
        Client nextClient = owner.getClient();
        Field nextField = owner.getField();
        int nextUser = nextClient.getUser().getId();
        int nextCharacter = owner.getId();
        long nextController = CONTROLLER_BASE + nextCharacter;
        long nextOni = ONI_BASE + nextCharacter;
        OutPacket bootstrap = Tms282KannaOniPackets.bootstrap(nextCharacter, nextUser, nextController, nextOni, owner.getPosition());
        if (!Protocol.active().outboundGuard().permit(bootstrap, nextClient::getIP)
                || net.swordie.ms.connection.protocol.DebugOutboundFilter.suppress(bootstrap)) return false;
        owner.write(bootstrap);
        if (!currentOwner() || owner.getClient() != nextClient || owner.getField() != nextField
                || nextClient.getUser().getId() != nextUser || owner.getId() != nextCharacter
                || castGeneration != generation) return false;
        client = nextClient;
        user = nextClient.getUser();
        field = nextField;
        userId = nextUser;
        characterId = nextCharacter;
        controllerId = nextController;
        oniId = nextOni;
        if (resume != null) restoreCast(resume, System.currentTimeMillis());
        return true;
    }

    private boolean validCast(PaidCast cast, boolean requireIcon, long now) {
        if (cast == null || now < lastCheckedAt || cast.expires() <= now
                || cast.icon().startTime + cast.icon().tTerm <= now || cast.icon().nReason != cast.source()
                || cast.icon().nValue != 1 || !currentOwner()
                || cast.client() != owner.getClient() || cast.user() != owner.getUser()
                || cast.userId() != owner.getUser().getId() || cast.characterId() != owner.getId()
                || cast.job() != owner.getJob() || cast.level() != learnedLevel(cast.source())
                || cast.book() == null || cast.book() != Tms282BuffDirectory.INSTANCE.book(owner.getId())) return false;
        lastCheckedAt = now;
        return !requireIcon || owner.getTemporaryStatManager().getOptByCTSAndSkill(IndieBuffIcon, cast.source()) == cast.icon();
    }

    private PaidCast restorableCast(long now) {
        if (validCast(paidCast, true, now)) return paidCast;
        return reviveGeneration == castGeneration && validCast(reviveCast, false, now) ? reviveCast : null;
    }

    /** A server field assignment revokes actor/queue scope without discarding an owned notRemoved cast. */
    public synchronized void leaveField() {
        if (Protocol.active().version() != ProtocolVersion.TMS_282_3) return;
        if (restorableCast(System.currentTimeMillis()) == null) cancel(); else revokeActors();
    }

    /** Only the real dead owner's still-present paid icon can survive the dedicated revive clearing boundary. */
    public synchronized void beforeRevive() {
        if (owner.getHP() > 0 || !validCast(paidCast, true, System.currentTimeMillis())) return;
        reviveCast = paidCast;
        reviveGeneration = castGeneration;
        source = 0;
        activeUntil = 0;
        lastReserveAt = 0;
    }

    public synchronized boolean afterRevive() {
        PaidCast resume = reviveCast;
        try {
            if (owner.getHP() <= 0 || reviveGeneration != castGeneration
                    || !validCast(resume, false, System.currentTimeMillis())) return false;
            if (!bound()) return enterField();
            return restoreCast(resume, System.currentTimeMillis());
        } finally { reviveCast = null; }
    }

    private boolean restoreCast(PaidCast cast, long now) {
        if (!bound() || !validCast(cast, false, now)) return false;
        long expires = Math.min(cast.expires(), cast.icon().startTime + cast.icon().tTerm);
        if (expires <= now) return false;
        Option icon = new Option();
        icon.nReason = cast.source();
        icon.nValue = 1;
        icon.tTerm = (int)(expires - now);
        icon.setInMillis(true);
        owner.getTemporaryStatManager().sendStat(IndieBuffIcon, icon);
        // TSM normally stamps the send time. Keep the original server deadline exactly, including send overhead.
        icon.startTime = expires - icon.tTerm;
        icon.tStart = (int)icon.startTime;
        if (!bound() || !validCast(cast, false, System.currentTimeMillis())
                || owner.getTemporaryStatManager().getOptByCTSAndSkill(IndieBuffIcon, cast.source()) != icon) return false;
        paidCast = new PaidCast(cast.client(), cast.user(), cast.book(), cast.userId(), cast.characterId(),
                cast.job(), cast.source(), cast.level(), icon, expires);
        source = cast.source();
        activeUntil = expires;
        return true;
    }

    private int learnedLevel(int skill) {
        int level = owner.getSkillLevel(skill);
        return skill == GHOST_VI && owner.getSkillLevel(GHOST) < 10 ? 0 : level;
    }

    /** Called by Kanna only after the CP04B4 cost/cooldown service accepted the learned summon. */
    public synchronized boolean afterAccepted(SkillUseInfo use) {
        if (!canActivate(use)) return false;
        int level = learnedLevel(use.skillID);
        int seconds = use.skillInfo.getValue(net.swordie.ms.client.character.skills.SkillStat.time, level);
        long deadline = owner.getSkillCoolTimes().get(use.skillID);
        // The service owns payment and reduction. Both WZ sources share its exact accepted deadline.
        owner.addSkillCoolTime(GHOST, deadline);
        owner.addSkillCoolTime(GHOST_VI, deadline);
        cancelActive();
        Option icon = new Option();
        icon.nReason = use.skillID;
        icon.nValue = 1;
        icon.tTerm = seconds;
        owner.getTemporaryStatManager().sendStat(IndieBuffIcon, icon);
        source = use.skillID;
        activeUntil = icon.startTime + seconds * 1000L;
        castGeneration++;
        paidCast = new PaidCast(client, user, Tms282BuffDirectory.INSTANCE.book(owner.getId()), userId, characterId,
                owner.getJob(), source, level, icon, activeUntil);
        lastCheckedAt = icon.startTime;
        return true;
    }

    /** Effects validation hook before the service can charge MP or cooldown. Other skills retain existing validation. */
    public static boolean validate(Char character, SkillUseInfo use) {
        if (use == null) return false;
        if (Protocol.active().version() != ProtocolVersion.TMS_282_3 || (use.skillID != GHOST && use.skillID != GHOST_VI)) return true;
        return character != null && character.getJobHandler() instanceof Kanna kanna
                && kanna.getOniController().canActivate(use)
                && !character.hasSkillOnCooldown(GHOST) && !character.hasSkillOnCooldown(GHOST_VI);
    }

    private synchronized boolean canActivate(SkillUseInfo use) {
        if (use == null || (use.skillID != GHOST && use.skillID != GHOST_VI)) return false;
        int level = learnedLevel(use.skillID);
        if (!bound() || owner.getHP() <= 0 || level <= 0 || use.slv != level || use.skillInfo == null
                || use.skillInfo.getSkillId() != use.skillID) return false;
        int seconds = use.skillInfo.getValue(net.swordie.ms.client.character.skills.SkillStat.time, level);
        return seconds == 55;
    }

    public synchronized boolean active() {
        if (!bound() || owner.getHP() <= 0 || activeUntil <= System.currentTimeMillis() || source == 0
                || !validCast(paidCast, true, System.currentTimeMillis())) return false;
        Option icon = owner.getTemporaryStatManager().getOptByCTSAndSkill(IndieBuffIcon, source);
        return icon != null && icon.nReason == source && icon.nValue > 0;
    }

    /** Shared Combat hook, after its ordinary packet guard and before AttackHandler is queued. */
    public static boolean beforeAttack(Char character, AttackInfo attack) {
        if (attack == null) return false;
        if (!isGhostAttack(attack.skillId) || Protocol.active().version() != ProtocolVersion.TMS_282_3) return true;
        if (character == null || !(character.getJobHandler() instanceof Kanna kanna)) return false;
        return kanna.getOniController().authorizeAttack(attack);
    }

    private synchronized boolean authorizeAttack(AttackInfo attack) {
        int expected = attack.skillId == BASIC_VI || attack.skillId == DESCENT_VI ? GHOST_VI : GHOST;
        if (!active() || source != expected) return false;
        attack.slv = learnedLevel(expected);
        return attack.slv > 0;
    }

    /** Capture the exact accepted source icon before queuing damage. A recast creates a different Option. */
    public static Option attackGrant(Char character, AttackInfo attack) {
        if (character == null || attack == null || !isGhostAttack(attack.skillId)
                || Protocol.active().version() != ProtocolVersion.TMS_282_3
                || !(character.getJobHandler() instanceof Kanna kanna)) return null;
        return kanna.getOniController().grant(attack);
    }

    private synchronized Option grant(AttackInfo attack) {
        return authorizeAttack(attack) ? owner.getTemporaryStatManager().getOptByCTSAndSkill(IndieBuffIcon, source) : null;
    }

    /** Queued attack permission cannot transfer to another accepted summon, even with the same source ID. */
    public static boolean validAttack(Char character, AttackInfo attack, Option grant) {
        return grant != null && attackGrant(character, attack) == grant;
    }

    /** Central CP027D router calls this per raw frame; other actors remain available to the familiar handler. */
    public static boolean handleFrame(Char character, long actor, int method, byte[] arguments) {
        if (character == null || Protocol.active().version() != ProtocolVersion.TMS_282_3) return false;
        long expectedController = CONTROLLER_BASE + character.getId();
        long expectedOni = ONI_BASE + character.getId();
        if (actor != expectedController && actor != expectedOni) return false;
        if (character.getJobHandler() instanceof Kanna kanna) {
            kanna.getOniController().reserve(actor, method, arguments);
        }
        return true;
    }

    private synchronized void reserve(long actor, int method, byte[] args) {
        if (actor != oniId || method != 140 || args == null || args.length != 20 || !active()) return;
        ByteBuffer in = ByteBuffer.wrap(args).order(ByteOrder.LITTLE_ENDIAN);
        int attack = in.getInt();
        if ((source == GHOST && attack != BASIC) || (source == GHOST_VI && attack != BASIC_VI)) return;
        // Native66FF340: flags0/facing/0/0;66EBBC0 derives facing0/1 from target/actor X.
        if (in.get() != 0 || Byte.toUnsignedInt(in.get()) > 1 || in.get() != 0 || in.get() != 0
                || in.getInt() != 0 || in.getInt() != 0 || in.getInt() != 0) return;
        long now = System.currentTimeMillis();
        if (lastReserveAt != 0 && now - lastReserveAt < 500) return;
        lastReserveAt = now;
        // Observers need this owner's two actors in their remote UserCommon world before the callback.
        // Same IDs use the native create-or-reuse branch. The owner has already queued the command locally.
        field.broadcast(Tms282KannaOniPackets.observerBootstrap(characterId, userId, controllerId, oniId, owner.getPosition()), owner);
        field.broadcast(Tms282KannaOniPackets.reserve(characterId, oniId, args), owner);
    }

    private void cancelActive() {
        PaidCast oldCast = paidCast;
        int oldSource = oldCast == null ? source : oldCast.source();
        source = 0;
        activeUntil = 0;
        lastReserveAt = 0;
        paidCast = null;
        reviveCast = null;
        castGeneration++;
        if (oldSource != 0) {
            var tsm = owner.getTemporaryStatManager();
            Option icon = tsm.getOptByCTSAndSkill(IndieBuffIcon, oldSource);
            if (icon != null && oldCast != null && icon == oldCast.icon()) tsm.removeIndieStat(IndieBuffIcon, icon);
        }
    }

    /** Job cancel/migration invalidates the old owner-world permission and removes only its source icon. */
    public synchronized void cancel() {
        revokeActors();
        cancelActive();
    }

    private void revokeActors() {
        client = null;
        user = null;
        field = null;
        userId = 0;
        characterId = 0;
        controllerId = 0;
        oniId = 0;
        source = 0;
        activeUntil = 0;
        lastReserveAt = 0;
    }

    public synchronized long getControllerId() { return bound() ? controllerId : 0; }
    public synchronized long getOniId() { return bound() ? oniId : 0; }
}
