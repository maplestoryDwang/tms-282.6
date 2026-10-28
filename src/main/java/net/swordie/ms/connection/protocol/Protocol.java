package net.swordie.ms.connection.protocol;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Static access point to the process-wide {@link ProtocolProfile}. The first call loads it from
 * {@link ProtocolConfig#load()}; startup may {@link #install} one explicitly. Tests switch profiles only
 * through {@link #withProfile}, which restores the previous profile.
 */
public final class Protocol {
    private static final Object LOCK = new Object();
    private static volatile ProtocolProfile active;
    private static volatile ProtocolConfig config;

    private Protocol() {
    }

    /** The active profile; hot path (every OutPacket(OutHeader), every inbound packet). */
    public static ProtocolProfile active() {
        ProtocolProfile p = active;
        return p != null ? p : initialize();
    }

    /** Settings the active profile was created from (defaults when a profile was installed directly). */
    public static ProtocolConfig config() {
        active();
        ProtocolConfig c = config;
        return c != null ? c : ProtocolConfig.defaults();
    }

    private static ProtocolProfile initialize() {
        synchronized (LOCK) {
            if (active == null) {
                ProtocolConfig loaded = ProtocolConfig.load();
                config = loaded;
                active = loaded.createProfile();
            }
            return active;
        }
    }

    /** Installs the profile of {@code cfg} for the whole process (startup). */
    public static ProtocolProfile install(ProtocolConfig cfg) {
        synchronized (LOCK) {
            ProtocolProfile profile = cfg.createProfile();
            config = cfg;
            active = profile;
            return profile;
        }
    }

    /**
     * Runs {@code body} with {@code profile} active and restores the previous one. Test use only: the
     * switch is process-wide, so never call it while sockets are live.
     */
    public static <T> T withProfile(ProtocolProfile profile, Supplier<T> body) {
        Objects.requireNonNull(profile, "profile");
        synchronized (LOCK) {
            ProtocolProfile previous = active;
            active = profile;
            try {
                return body.get();
            } finally {
                active = previous;
            }
        }
    }

    public static void withProfile(ProtocolProfile profile, Runnable body) {
        withProfile(profile, () -> {
            body.run();
            return null;
        });
    }
}
