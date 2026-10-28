package net.swordie.ms.connection.api.launcher;

import net.swordie.ms.connection.api.launcher.daily.DailyRewardTable;
import net.swordie.ms.connection.api.launcher.news.LauncherNews;

import java.util.Objects;

/**
 * The collaborators a launcher request may use. The server process shares one instance (sessions and limits live for
 * the whole run); every CLI request gets its own inert one. Tests build their own with an injected clock. The news and
 * the reward table are process-wide by default ({@link LauncherNews#standard()}, {@link DailyRewardTable#standard()}:
 * the working directory's files).
 */
public record LauncherServices(LauncherSessions sessions, LauncherRateLimits rateLimits, LoginBackoff loginBackoff,
                               LoginGate loginGate, GameServerProbe serverProbe, OnlineAccounts onlineAccounts,
                               ServerMemory memory, LauncherNews news, DailyRewardTable rewards) {

    public LauncherServices {
        Objects.requireNonNull(sessions, "sessions");
        Objects.requireNonNull(rateLimits, "rateLimits");
        Objects.requireNonNull(loginBackoff, "loginBackoff");
        Objects.requireNonNull(loginGate, "loginGate");
        Objects.requireNonNull(serverProbe, "serverProbe");
        Objects.requireNonNull(onlineAccounts, "onlineAccounts");
        Objects.requireNonNull(memory, "memory");
        Objects.requireNonNull(news, "news");
        Objects.requireNonNull(rewards, "rewards");
    }

    /** The working directory's news and reward table (P5b signature). */
    public LauncherServices(LauncherSessions sessions, LauncherRateLimits rateLimits, LoginBackoff loginBackoff,
                            LoginGate loginGate, GameServerProbe serverProbe, OnlineAccounts onlineAccounts,
                            ServerMemory memory) {
        this(sessions, rateLimits, loginBackoff, loginGate, serverProbe, onlineAccounts, memory, LauncherNews.standard(),
                DailyRewardTable.standard());
    }

    /** Without the read views' server memory ({@link ServerMemory#none()}). */
    public LauncherServices(LauncherSessions sessions, LauncherRateLimits rateLimits, LoginBackoff loginBackoff,
                            LoginGate loginGate, GameServerProbe serverProbe, OnlineAccounts onlineAccounts) {
        this(sessions, rateLimits, loginBackoff, loginGate, serverProbe, onlineAccounts, ServerMemory.none());
    }

    /** Without server memory ({@link OnlineAccounts#none()}, {@link ServerMemory#none()}): tests. */
    public LauncherServices(LauncherSessions sessions, LauncherRateLimits rateLimits, LoginBackoff loginBackoff,
                            LoginGate loginGate, GameServerProbe serverProbe) {
        this(sessions, rateLimits, loginBackoff, loginGate, serverProbe, OnlineAccounts.none(), ServerMemory.none());
    }

    /** The same services reading {@code memory} instead (tests: a fake running server). */
    public LauncherServices withMemory(ServerMemory memory) {
        return new LauncherServices(sessions, rateLimits, loginBackoff, loginGate, serverProbe, onlineAccounts, memory, news, rewards);
    }

    /** The same services seeing {@code onlineAccounts} instead (tests). */
    public LauncherServices withOnlineAccounts(OnlineAccounts onlineAccounts) {
        return new LauncherServices(sessions, rateLimits, loginBackoff, loginGate, serverProbe, onlineAccounts, memory, news, rewards);
    }

    /** The same services with other news (tests: a temporary news.json and clock). */
    public LauncherServices withNews(LauncherNews news) {
        return new LauncherServices(sessions, rateLimits, loginBackoff, loginGate, serverProbe, onlineAccounts, memory, news, rewards);
    }

    /** The same services with another reward table (tests). */
    public LauncherServices withRewards(DailyRewardTable rewards) {
        return new LauncherServices(sessions, rateLimits, loginBackoff, loginGate, serverProbe, onlineAccounts, memory, news, rewards);
    }

    /** HttpAuthApi: real clock, standard limits and back-off, BanService / GameSettings, the server's memory. */
    public static LauncherServices forServer() {
        return new LauncherServices(new LauncherSessions(System::currentTimeMillis),
                LauncherRateLimits.standard(System::currentTimeMillis), new LoginBackoff(System::currentTimeMillis),
                LoginGate.live(), GameServerProbe.loginPort(), OnlineAccounts.live(), ServerMemory.live());
    }

    /** One LocalAccountCli request: nothing is shared across JVMs; transport checks probe 127.0.0.1:login.port. */
    public static LauncherServices forCli() {
        return forCli(GameServerProbe.loginPort());
    }

    /** {@link #forCli()} with another probe (tests: "server not running" without depending on this machine's 8484). */
    public static LauncherServices forCli(GameServerProbe serverProbe) {
        return new LauncherServices(new LauncherSessions(System::currentTimeMillis), LauncherRateLimits.unlimited(),
                LoginBackoff.disabled(), LoginGate.live(), serverProbe, OnlineAccounts.none(), ServerMemory.none());
    }
}
