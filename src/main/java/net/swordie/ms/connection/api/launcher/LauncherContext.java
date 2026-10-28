package net.swordie.ms.connection.api.launcher;

import java.util.Objects;

/**
 * Who is asking and through which channel. Built by the CLI or an HTTP endpoint; {@link LauncherPolicies#SESSION}
 * attaches the verified {@link LauncherSession}. {@link #toString()} never includes the bearer token.
 */
public final class LauncherContext {
    public static final String LOCAL_SOURCE = "127.0.0.1";

    private final LauncherChannel channel;
    private final String sourceIp;
    private final String launcherVersion;
    private final String bearerToken;
    private final LauncherServices services;
    private volatile LauncherSession session;

    private LauncherContext(LauncherChannel channel, String sourceIp, String launcherVersion, String bearerToken,
                            LauncherServices services) {
        this.channel = Objects.requireNonNull(channel, "channel");
        this.sourceIp = sourceIp == null ? "" : sourceIp;
        this.launcherVersion = launcherVersion == null ? "" : launcherVersion;
        this.bearerToken = bearerToken;
        this.services = Objects.requireNonNull(services, "services");
    }

    /** A LocalAccountCli request (source 127.0.0.1: the game client of this machine logs in from there too). */
    public static LauncherContext cli() {
        return cli(LauncherServices.forCli());
    }

    public static LauncherContext cli(LauncherServices services) {
        return new LauncherContext(LauncherChannel.CLI, LOCAL_SOURCE, "", null, services);
    }

    /** An HttpAuthApi request; {@code bearerToken} may be null. */
    public static LauncherContext http(LauncherChannel channel, String sourceIp, String launcherVersion,
                                       String bearerToken, LauncherServices services) {
        if (!channel.http()) {
            throw new IllegalArgumentException("not an HTTP channel: " + channel);
        }
        return new LauncherContext(channel, sourceIp, launcherVersion, bearerToken, services);
    }

    public LauncherChannel channel() {
        return channel;
    }

    public String sourceIp() {
        return sourceIp;
    }

    /** The X-Cat-Launcher header ("" when absent, e.g. cat-launcher 1.0.0 or the CLI). */
    public String launcherVersion() {
        return launcherVersion;
    }

    String bearerToken() {
        return bearerToken;
    }

    public LauncherServices services() {
        return services;
    }

    /** The verified session of a {@code requiresSession} action; null otherwise. */
    public LauncherSession session() {
        return session;
    }

    void attach(LauncherSession session) {
        this.session = session;
    }

    /** 127.0.0.0/8 or ::1 (the host itself). */
    public boolean loopbackSource() {
        return sourceIp.startsWith("127.") || sourceIp.equals("::1") || sourceIp.equals("0:0:0:0:0:0:0:1");
    }

    @Override
    public String toString() {
        return "LauncherContext[" + channel + ", source=" + sourceIp + ", launcher=" + launcherVersion
                + ", bearer=" + (bearerToken != null) + "]";
    }
}
