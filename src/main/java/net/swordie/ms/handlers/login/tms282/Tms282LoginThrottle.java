package net.swordie.ms.handlers.login.tms282;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import net.swordie.ms.connection.api.launcher.LoginBackoff;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Failed CP0074 logins (health audit 2026-10-01, step 2), on top of P5a's {@link LoginBackoff}:
 * <ul>
 *     <li>per (account, source IP): the {@value LoginBackoff#LOCK_AFTER_FAILURES}th failure in a row locks the pair
 *     for 30 s, doubling up to 15 minutes (LoginBackoff; the count is only cleared by a correct password, a launcher
 *     password reset or 30 idle minutes, so each further wrong password after a lock expires locks again, longer).
 *     A CP0074 for a locked pair is refused without a password check (no BCrypt, nothing read from the database),
 *     with {@link #lockoutMessage(long)} instead of the bare wrong-password reply, and the socket is closed. The
 *     failure that starts a lock is answered with the same message;</li>
 *     <li>per socket: after {@value #MAX_FAILURES_PER_SOCKET} failures the socket is closed (a client has to
 *     reconnect, which net.maxPerIp and net.allow also see).</li>
 * </ul>
 * A correct password clears the pair. Separate from the launcher API's back-off (8600), so the two never lock each
 * other. State lives in memory only.
 */
public final class Tms282LoginThrottle {
    public static final int MAX_FAILURES_PER_SOCKET = 5;
    private static final AttributeKey<AtomicInteger> FAILURES = AttributeKey.valueOf("tms282.loginFailures");
    private static final Tms282LoginThrottle STANDARD = new Tms282LoginThrottle(new LoginBackoff(System::currentTimeMillis));

    /** What to do after a failed password. */
    public enum Outcome {
        /** Answer the failure; the client may try again on this socket. */
        RETRY,
        /** Answer the failure and close the socket. */
        DISCONNECT
    }

    private final LoginBackoff backoff;

    public Tms282LoginThrottle(LoginBackoff backoff) {
        this.backoff = Objects.requireNonNull(backoff, "backoff");
    }

    public static Tms282LoginThrottle standard() {
        return STANDARD;
    }

    /** Milliseconds the (account, IP) pair is still locked; 0 = may try. */
    public long lockedForMs(String account, String ip) {
        return backoff.retryAfterMs(account, ip);
    }

    /** Records a failed password on {@code socket}. */
    public Outcome failure(Channel socket, String account, String ip) {
        backoff.failure(account, ip);
        AtomicInteger n = socket.attr(FAILURES).setIfAbsent(new AtomicInteger());
        if (n == null) {
            n = socket.attr(FAILURES).get();
        }
        int onSocket = n.incrementAndGet();
        if (onSocket >= MAX_FAILURES_PER_SOCKET || backoff.retryAfterMs(account, ip) > 0) {
            return Outcome.DISCONNECT;
        }
        return Outcome.RETRY;
    }

    public void success(String account, String ip) {
        backoff.success(account, ip);
    }

    /** Forgets the account's pairs from every IP (a successful launcher password reset); returns how many. */
    public int clearAccount(String account) {
        return backoff.clearAccount(account);
    }

    /**
     * The LP0000 text for a locked pair. Says that the account is locked from this computer for a while and that
     * even the correct password is refused until then, so a friend does not keep guessing (which locks it longer).
     */
    public static String lockoutMessage(long lockedMs) {
        long sec = Math.max(1, (lockedMs + 999) / 1000);
        String wait = sec >= 60 ? (sec + 59) / 60 + " 分鐘" : sec + " 秒";
        return "密碼錯誤次數過多，暫時鎖定，請 " + wait + "後再試（鎖定期間正確密碼也無法登入）。忘記密碼請用啟動器的「忘記密碼」重設。";
    }

    /** The account's remembered (account, IP) pairs of the CP0074 back-off (admin console, read-only). */
    public java.util.List<LoginBackoff.Pair> pairsOf(String account) {
        return backoff.pairsOf(account);
    }

    /** (account, IP) pairs locked right now (admin console). */
    public int lockedCount() {
        return backoff.lockedCount();
    }

    /** Failures counted on this socket (checks). */
    public static int failuresOn(Channel socket) {
        AtomicInteger n = socket.attr(FAILURES).get();
        return n == null ? 0 : n.get();
    }
}
