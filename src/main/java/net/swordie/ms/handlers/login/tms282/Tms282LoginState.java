package net.swordie.ms.handlers.login.tms282;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import net.swordie.ms.client.Account;
import net.swordie.ms.client.Client;
import net.swordie.ms.client.User;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.packet.Tms282DisplayedCharacters;
import net.swordie.ms.connection.packet.Tms282LoginSystemPackets;
import net.swordie.ms.connection.packet.Tms282StagePackets;
import net.swordie.ms.connection.protocol.SessionKind;
import org.mindrot.jbcrypt.BCrypt;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Per-socket TMS 282.3 session state (the research probe's {@code Session} fields), attached to the Netty channel of
 * a {@link Client} ({@link #of}). Login sockets track authentication, world and character-list progress; channel
 * sockets track the one-time CP007E and the entered field. Also owns the socket's timers: the LP0020 keep-alive
 * (first ping 6 s after connect, the next 8 s after each CP00AB; one unanswered ping at most) and the experimental
 * SPW-dialog cancel fallback, and on channel sockets the CP0193 HP/MP write timer ({@link Tms282HpMpSaveTimer}) and
 * the CP04B4 buff-expiry timer ({@link Tms282SessionBuffs}).
 * Handlers run on the channel's event loop; the timer fields are thread-safe.
 */
public final class Tms282LoginState {
    private static final AttributeKey<Tms282LoginState> KEY = AttributeKey.valueOf("tms282.loginState");
    static final long FIRST_PING_SECONDS = 6;
    static final long PING_AFTER_PONG_SECONDS = 8;
    private static final ScheduledExecutorService TIMER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "Tms282SessionTimer");
        t.setDaemon(true);
        return t;
    });

    private final Client client;
    private final SessionKind kind;

    // Login socket.
    private User user;
    private Account selectedAccount;
    private boolean characterListSent;
    private boolean fileReportVerified;
    private boolean nativeRelogin;

    boolean nativeRelogin() { return nativeRelogin; }
    void markNativeRelogin() { nativeRelogin = true; }
    private int selectedChannel;
    /** CP00EC choices, world ID -> displayed character IDs. Session only (no database column). */
    private final Map<Integer, List<Integer>> displayedCharacters = new HashMap<>();
    private final Set<Integer> createdThisSession = new HashSet<>();

    // Channel socket.
    private boolean migrateInSeen;
    private int approvedSwitchCharacter;

    void approveSwitch(int characterId) { approvedSwitchCharacter=characterId; }
    boolean consumeSwitch(int characterId) {
        if(characterId<=0 || approvedSwitchCharacter!=characterId) return false;
        approvedSwitchCharacter=0;
        return true;
    }
    void leaveFieldForRelogin() {
        stopTimers();
        fieldCharacter = null;
    }
    private Char fieldCharacter;
    private Tms282StagePackets.DamageSeeds damageSeeds;
    private final AtomicReference<Tms282HpMpSaveTimer> hpMpSaveTimer = new AtomicReference<>();
    private final Tms282SessionBuffs buffs = new Tms282SessionBuffs();

    // Timers.
    private final AtomicReference<Object> spwCancelCheck = new AtomicReference<>();
    private final AtomicReference<ScheduledFuture<?>> nextPing = new AtomicReference<>();
    private volatile boolean awaitingPong;
    private volatile boolean closed;

    private Tms282LoginState(Client client, SessionKind kind) {
        this.client = Objects.requireNonNull(client, "client");
        this.kind = Objects.requireNonNull(kind, "kind");
    }

    /**
     * Creates the state of a new socket (called once by the 282 handshake). Starts the keep-alive on login sockets,
     * and on channel sockets when {@code channelPing}; timers stop when the socket closes.
     */
    public static Tms282LoginState attach(Client client, SessionKind kind, boolean channelPing) {
        Tms282LoginState state = new Tms282LoginState(client, kind);
        Channel ch = client.getCh();
        if (!ch.attr(KEY).compareAndSet(null, state)) {
            throw new IllegalStateException("282 session state already attached to " + ch);
        }
        Tms282ClientDiagnostics.registerClient(client);
        ch.closeFuture().addListener(f -> {
            Tms282ClientDiagnostics.removeClient(client);
            state.stopTimers();
        });
        if (kind == SessionKind.LOGIN || channelPing) {
            state.schedulePing(FIRST_PING_SECONDS);
        }
        return state;
    }

    /** The socket's state; a socket the 282 handshake did not set up gets a login-socket state without timers. */
    public static Tms282LoginState of(Client client) {
        Channel ch = client.getCh();
        Tms282LoginState state = ch.attr(KEY).get();
        if (state == null) {
            Tms282LoginState created = new Tms282LoginState(client, SessionKind.LOGIN);
            state = ch.attr(KEY).setIfAbsent(created);
            if (state == null) {
                state = created;
            }
        }
        return state;
    }

    public Client client() {
        return client;
    }

    public SessionKind kind() {
        return kind;
    }

    public boolean isChannel() {
        return kind == SessionKind.CHANNEL;
    }

    /** Sends through the client (OutboundGuard, profile cipher). */
    public void send(OutPacket packet) {
        client.write(packet);
    }

    /** Ends the session like the probe's IOException: logs and closes the socket. */
    public void terminate(String reason) {
        System.out.printf("[TMS282] %s %s: %s; disconnecting%n", kind, safeAddress(), reason);
        stopTimers();
        client.getCh().close();
    }

    String safeAddress() {
        try {
            return String.valueOf(client.getCh().remoteAddress());
        } catch (RuntimeException e) {
            return "?";
        }
    }

    // ---- login progress --------------------------------------------------------------------------------------

    public User user() {
        return user;
    }

    public boolean authenticated() {
        return user != null;
    }

    /** CP0074 success: the client also carries the user (LOGIN_TRANSITION, as the 265 login does). */
    void authenticate(User authenticated) {
        nativeRelogin = false;
        this.user = Objects.requireNonNull(authenticated, "user");
        client.setUser(authenticated);
        client.setCurrentState(Client.LOGIN_TRANSITION);
    }

    public Account selectedAccount() {
        return selectedAccount;
    }

    public boolean worldSelected() {
        return selectedAccount != null;
    }

    public boolean characterListSent() {
        return characterListSent;
    }

    public boolean fileReportVerified() {
        return fileReportVerified;
    }

    void markFileReportVerified() {
        fileReportVerified = true;
    }

    /** Only the server-issued migration's origin may carry an already validated installation report. */
    void inheritFileReportVerification(Client origin) {
        if (origin == null) return;
        Tms282LoginState source = origin.getCh().attr(KEY).get();
        if (source != null && source.fileReportVerified()) markFileReportVerified();
    }

    public int selectedChannel() {
        return selectedChannel;
    }

    /** LP0013 sent for {@code account}'s world with zero-based wire channel {@code channel}. */
    void characterListSent(Account account, int channel) {
        cancelSpwCancelCheck();
        this.selectedAccount = Objects.requireNonNull(account, "account");
        this.selectedChannel = channel;
        this.characterListSent = true;
        client.setAccount(account);
        client.setWorldId((byte) account.getWorldId());
    }

    /** CP0086: back to the world list. */
    void returnToWorldSelect() {
        nativeRelogin = false;
        cancelSpwCancelCheck();
        characterListSent = false;
        selectedAccount = null;
    }

    /**
     * CP0081 logout: forget everything tied to the account. fileReportVerified stays: the CP0078 report describes
     * this client installation, not the account, and is sent only once per connection.
     */
    void resetLogin() {
        nativeRelogin = false;
        cancelSpwCancelCheck();
        user = null;
        selectedAccount = null;
        characterListSent = false;
        displayedCharacters.clear();
        createdThisSession.clear();
        client.setUser(null);
        client.setAccount(null);
        client.setCurrentState(Client.NONE);
    }

    Map<Integer, List<Integer>> displayedCharacters() {
        return displayedCharacters;
    }

    Set<Integer> createdThisSession() {
        return createdThisSession;
    }

    /** The selected world's character with this ID, or null. */
    Char characterInSelectedWorld(int characterId) {
        if (selectedAccount == null || selectedAccount.getCharacters() == null) {
            return null;
        }
        return selectedAccount.getCharacters().stream().filter(c -> c.getId() == characterId).findFirst().orElse(null);
    }

    /** LP0013 displayed-character fields: the stored CP00EC list (IDs still present), else show all. */
    Tms282DisplayedCharacters displayedFor(Account account) {
        List<Integer> stored = displayedCharacters.getOrDefault(account.getWorldId(), List.of());
        List<Integer> present = stored.stream()
                .filter(id -> account.getCharacters().stream().anyMatch(c -> c.getId() == id)).toList();
        return present.isEmpty() ? Tms282DisplayedCharacters.showAll(user.getCharacterSlots())
                : Tms282DisplayedCharacters.only(user.getCharacterSlots(), present);
    }

    /** BCrypt check against the user's PIC; an empty or missing PIC never matches. */
    boolean secondaryPasswordMatches(String plain) {
        String pic = user == null ? null : user.getPic();
        return pic != null && !pic.isBlank() && !plain.isEmpty() && BCrypt.checkpw(plain, pic);
    }

    // ---- channel progress ------------------------------------------------------------------------------------

    public boolean migrateInSeen() {
        return migrateInSeen;
    }

    void markMigrateInSeen() {
        migrateInSeen = true;
    }

    public Char fieldCharacter() {
        return fieldCharacter;
    }

    public Tms282StagePackets.DamageSeeds damageSeeds() {
        return damageSeeds;
    }

    /** LP02EF was sent: later in-field CPs are accepted. */
    void enteredField(Char chr, Tms282StagePackets.DamageSeeds seeds) {
        this.fieldCharacter = Objects.requireNonNull(chr, "chr");
        Tms282ClientDiagnostics.snapshotClient(client, chr);
        this.damageSeeds = Objects.requireNonNull(seeds, "seeds");
    }

    /**
     * The field character's HP/MP write timer, created on first use (the first interval counts from then, as the
     * probe's counts from the session start); stopped with the socket.
     */
    Tms282HpMpSaveTimer hpMpSaveTimer(Tms282FieldServices services) {
        Tms282HpMpSaveTimer timer = hpMpSaveTimer.get();
        if (timer == null) {
            Tms282HpMpSaveTimer created = new Tms282HpMpSaveTimer(this, fieldCharacter, services.store(),
                    services.hpMpSaveInterval().toNanos(), System::nanoTime);
            timer = hpMpSaveTimer.compareAndExchange(null, created);
            if (timer == null) {
                timer = created;
                if (closed) {
                    timer.stop();
                }
            }
        }
        return timer;
    }

    // ---- timers ----------------------------------------------------------------------------------------------

    /** CP00AB: the next ping follows {@link #PING_AFTER_PONG_SECONDS} later. */
    void onPong() {
        awaitingPong = false;
        schedulePing(PING_AFTER_PONG_SECONDS);
    }

    public boolean awaitingPong() {
        return awaitingPong;
    }

    private void schedulePing(long delaySeconds) {
        if (closed) {
            return;
        }
        ScheduledFuture<?> future = TIMER.schedule(this::ping, delaySeconds, TimeUnit.SECONDS);
        ScheduledFuture<?> previous = nextPing.getAndSet(future);
        if (previous != null) {
            previous.cancel(false);
        }
    }

    /** One unanswered ping at most: loading can delay CP00AB for over 20 seconds. */
    private void ping() {
        if (closed || awaitingPong || !client.getCh().isActive()) {
            return;
        }
        awaitingPong = true;
        send(Tms282LoginSystemPackets.ping());
    }

    /**
     * EXPERIMENTAL (spw-state-2823.md): cancelling the SPW dialog after LP0031 [01 01] sends nothing and leaves the
     * UI locked. If no subsequent login action arrives within {@code seconds}, LP0043 clears CLogin+0x220.
     * Token check and unlock run on the same event loop as the next request, so a stale timer cannot race it.
     */
    void scheduleSpwCancelCheck(long seconds, Runnable unlock) {
        Object token = new Object();
        spwCancelCheck.set(token);
        client.getCh().eventLoop().schedule(() -> {
            if (!closed && spwCancelCheck.compareAndSet(token, null)) {
                unlock.run();
            }
        }, seconds, TimeUnit.SECONDS);
    }

    /** CP04B4 buffs and cooldowns of this socket; their expiry timer stops with the socket. */
    Tms282SessionBuffs buffs() {
        return buffs;
    }

    /** Runs {@code task} once on the session timer unless the socket closed first; null when already closed. */
    ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit) {
        Objects.requireNonNull(task, "task");
        if (closed) {
            return null;
        }
        return TIMER.schedule(() -> {
            if (!closed) {
                task.run();
            }
        }, delay, unit);
    }

    /** A subsequent login operation ends the pending SPW cancel check. */
    void cancelSpwCancelCheck() {
        spwCancelCheck.set(null);
    }

    boolean spwCancelCheckPending() {
        return spwCancelCheck.get() != null;
    }

    private void stopTimers() {
        closed = true;
        net.swordie.ms.client.character.tms282.Tms282MyHomeRuntime.onDisconnect(client, fieldCharacter);
        spwCancelCheck.set(null);
        Tms282HpMpSaveTimer hpMp = hpMpSaveTimer.get();
        if (hpMp != null) {
            hpMp.stop();
        }
        buffs.cancel();
        ScheduledFuture<?> future = nextPing.getAndSet(null);
        if (future != null) {
            future.cancel(false);
        }
    }
}
