package net.swordie.ms.connection.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import net.swordie.ms.util.HotProperties;

import java.util.concurrent.TimeUnit;

/**
 * Read-idle timeout of the login socket (health audit 2026-10-01, step 2): a login socket that sends nothing for
 * {@value #KEY} seconds (default {@value #DEFAULT_SECONDS}) is closed with one log line. A live 282 client answers
 * the server's ping every ~8 s (Tms282LoginState: first ping 6 s after connect, the next 8 s after each CP00AB), so
 * only dead or idle sockets reach the limit. 0 turns it off. Read when the socket is accepted (hot reloaded for new
 * sockets). First handler of the pipeline, so every received byte counts as activity.
 */
public final class LoginIdleTimeout extends IdleStateHandler {
    public static final String KEY = "net.login.idleSec";
    public static final int DEFAULT_SECONDS = 60;

    private final int seconds;

    public LoginIdleTimeout(int seconds) {
        super(seconds, 0, 0, TimeUnit.SECONDS);
        this.seconds = seconds;
    }

    /** The configured seconds (0 = off), clamped to 0..3600. */
    public static int configuredSeconds(HotProperties settings) {
        return settings.integer(KEY, DEFAULT_SECONDS, 0, 3600);
    }

    @Override
    protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) {
        if (evt.state() == IdleState.READER_IDLE) {
            System.out.printf("[NetLimit] %s: login socket idle for %d s, closing%n", ctx.channel().remoteAddress(), seconds);
            ctx.close();
        }
    }
}
