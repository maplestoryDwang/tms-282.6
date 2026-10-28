package net.swordie.ms.connection.protocol.codec;

import net.swordie.ms.connection.protocol.ProfileCodecs;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolProfile;
import net.swordie.ms.connection.protocol.ProtocolVersion;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lookup of a codec family for the 265 packet builders that defer to the active profile
 * ({@link UserPoolPacketEncoder}, {@link NpcPacketEncoder}, {@link StagePacketEncoder}). The active profile's own
 * {@link ProfileCodecs} win; a profile that does not register a family yet falls back to the encoders installed for
 * its version with {@link #installFallback} (the TMS 282.3 handler overlay installs its field encoders there, so they
 * work before and after the profile factory registers them). No profile and no fallback: empty, and the 265 builder
 * keeps its own body.
 */
public final class ProfileEncoders {
    private static final Map<ProtocolVersion, ProfileCodecs> FALLBACK = new ConcurrentHashMap<>();

    private ProfileEncoders() {
    }

    /** The active profile's codec of {@code family}, else the fallback installed for its version. */
    public static <T> Optional<T> find(Class<T> family) {
        return find(Protocol.active(), family);
    }

    public static <T> Optional<T> find(ProtocolProfile profile, Class<T> family) {
        Objects.requireNonNull(family, "family");
        Optional<T> own = profile.codecs().find(family);
        if (own.isPresent()) {
            return own;
        }
        ProfileCodecs fallback = FALLBACK.get(profile.version());
        return fallback == null ? Optional.empty() : fallback.find(family);
    }

    /** Replaces the fallback encoders of {@code version} (profile start-up; checks). */
    public static void installFallback(ProtocolVersion version, ProfileCodecs codecs) {
        FALLBACK.put(Objects.requireNonNull(version, "version"), Objects.requireNonNull(codecs, "codecs"));
    }
}
