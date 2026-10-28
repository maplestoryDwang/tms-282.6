package net.swordie.ms.connection.netty;

import io.netty.channel.Channel;
import net.swordie.ms.connection.api.launcher.SourceAllowlist;
import net.swordie.ms.connection.protocol.SessionKind;
import net.swordie.ms.lifecycle.ServerShutdown;
import net.swordie.ms.util.HotProperties;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Source allowlist of the game ports (login 8484, channels 8585..), checked in
 * {@link ProtocolChannelInitializer#initChannel} before any handler is added: a refused socket is closed before the
 * hello, nothing is read from it (health audit 2026-10-01, step 2; the firewall is not touched).
 * <p>
 * The packaged source policy is exactly {@value #DEFAULT}; neither configuration nor JVM arguments can broaden it.
 * <p>
 * A refused connection is closed and logged in one line; the same address is logged at most once per
 * {@value #LOG_EVERY_MS} ms (with the number of refusals since), so a scan cannot flood the log.
 * <p>
 * Also limits the sockets one address may hold open on the game ports together: {@value #MAX_PER_IP_KEY}
 * (default {@value #DEFAULT_MAX_PER_IP}, 0 = no limit, hot reloaded). A game client holds one login and one channel
 * socket, so the default leaves room for several clients on one machine.
 */
public final class GameSourceGate {
    public static final String KEY = "net.allow";
    public static final String DEFAULT = "127.0.0.1";
    public static final long LOG_EVERY_MS = 60_000;
    public static final String MAX_PER_IP_KEY = "net.maxPerIp";
    public static final int DEFAULT_MAX_PER_IP = 16;
    private static final int LOG_MEMORY = 1024;

    private static final GameSourceGate STANDARD = new GameSourceGate(HotProperties.server(), System::currentTimeMillis);
    private static final SourceAllowlist LOCAL_ONLY = SourceAllowlist.parse(DEFAULT);

    /** Decision for one new socket. */
    public enum Verdict {
        ADMIT, NOT_ALLOWED, SHUTTING_DOWN, NOT_INET, TOO_MANY
    }

    private final HotProperties settings;
    private final LongSupplier clock;
    private final ConcurrentHashMap<String, long[]> lastLog = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> open = new ConcurrentHashMap<>();

    public GameSourceGate(HotProperties settings, LongSupplier clock) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public static GameSourceGate standard() {
        return STANDARD;
    }

    /** The source restriction is compiled into this local-only package. */
    public SourceAllowlist allowlist() {
        return LOCAL_ONLY;
    }

    /** The verdict for a socket from {@code remote}; does not log. */
    public Verdict verdict(SocketAddress remote) {
        if (ServerShutdown.inProgress()) {
            return Verdict.SHUTTING_DOWN;
        }
        if (!(remote instanceof InetSocketAddress inet) || inet.getAddress() == null) {
            return Verdict.NOT_INET;
        }
        return allowlist().allows(inet.getAddress()) ? Verdict.ADMIT : Verdict.NOT_ALLOWED;
    }

    /** True when the new socket may continue; otherwise logs (throttled) and the caller closes it. */
    public boolean admit(Channel ch, SessionKind kind) {
        SocketAddress remote = ch.remoteAddress();
        Verdict v = verdict(remote);
        if (v == Verdict.ADMIT) {
            String ip = ((InetSocketAddress) remote).getAddress().getHostAddress();
            if (acquire(ip)) {
                ch.closeFuture().addListener(f -> release(ip));
                return true;
            }
            v = Verdict.TOO_MANY;
        }
        logRefusal(remote, kind, ch.localAddress(), v);
        return false;
    }

    public int maxPerIp() {
        return settings.integer(MAX_PER_IP_KEY, DEFAULT_MAX_PER_IP, 0, 100_000);
    }

    /** Counts one more open socket of {@code ip}; false (nothing counted) when it already holds the maximum. */
    boolean acquire(String ip) {
        int max = maxPerIp();
        boolean[] ok = {false};
        open.compute(ip, (k, n) -> {
            int now = n == null ? 0 : n;
            if (max > 0 && now >= max) {
                return n;
            }
            ok[0] = true;
            return now + 1;
        });
        return ok[0];
    }

    void release(String ip) {
        open.computeIfPresent(ip, (k, n) -> n <= 1 ? null : n - 1);
    }

    /** Open game sockets of {@code ip} right now (checks, diagnostics). */
    public int openFrom(String ip) {
        return open.getOrDefault(ip, 0);
    }

    void logRefusal(SocketAddress remote, SessionKind kind, SocketAddress local, Verdict v) {
        String ip = remote instanceof InetSocketAddress inet && inet.getAddress() != null
                ? inet.getAddress().getHostAddress() : String.valueOf(remote);
        String key = ip + "|" + v;
        long now = clock.getAsLong();
        try {
            // admin console: recent refused sources (counted before the log throttle)
            RefusedSources.standard().record(ip, v.name(), String.valueOf(kind),
                    local instanceof InetSocketAddress l0 ? l0.getPort() : -1, now);
        } catch (RuntimeException ignored) {
            // monitoring must never change the refusal
        }
        long[] state = lastLog.get(key);
        if (state == null && lastLog.size() >= LOG_MEMORY) {
            lastLog.clear();
        }
        state = lastLog.computeIfAbsent(key, k -> new long[]{Long.MIN_VALUE, 0});
        long suppressed;
        synchronized (state) {
            if (state[0] != Long.MIN_VALUE && now - state[0] < LOG_EVERY_MS && now >= state[0]) {
                state[1]++;
                return;
            }
            suppressed = state[1];
            state[0] = now;
            state[1] = 0;
        }
        int port = local instanceof InetSocketAddress l ? l.getPort() : -1;
        String why = switch (v) {
            case NOT_ALLOWED -> "not in " + KEY;
            case SHUTTING_DOWN -> "server is shutting down";
            case NOT_INET -> "no IP address";
            case TOO_MANY -> "already " + openFrom(ip) + " open game sockets from this address (" + MAX_PER_IP_KEY + "=" + maxPerIp() + ")";
            case ADMIT -> "admitted";
        };
        System.out.printf("[NetAllow] refused %s on %s port %d: %s%s%n", ip, kind, port, why,
                suppressed > 0 ? " (+" + suppressed + " more in the last " + LOG_EVERY_MS / 1000 + " s)" : "");
    }

    /** For checks: whether {@code address} is allowed right now. */
    public boolean allows(InetAddress address) {
        return allowlist().allows(address);
    }
}
