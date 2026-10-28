package net.swordie.ms.connection.protocol;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Codec family implementations of a profile, keyed by their interface (MovePath, AttackRequest, ... in
 * {@code connection/protocol/codec/}, phase 1+). Immutable; built with {@link #builder()}.
 */
public final class ProfileCodecs {
    private static final ProfileCodecs NONE = new ProfileCodecs(Map.of());

    private final Map<Class<?>, Object> codecs;

    private ProfileCodecs(Map<Class<?>, Object> codecs) {
        this.codecs = codecs;
    }

    public static ProfileCodecs none() {
        return NONE;
    }

    public static Builder builder() {
        return new Builder();
    }

    public <T> Optional<T> find(Class<T> family) {
        return Optional.ofNullable(family.cast(codecs.get(family)));
    }

    /** @throws IllegalStateException when the profile has no codec of that family */
    public <T> T require(Class<T> family) {
        return find(family).orElseThrow(() -> new IllegalStateException("No codec for " + family.getSimpleName()));
    }

    public int size() {
        return codecs.size();
    }

    public static final class Builder {
        private final Map<Class<?>, Object> codecs = new LinkedHashMap<>();

        private Builder() {
        }

        /** @throws IllegalStateException when the family is registered twice */
        public <T> Builder add(Class<T> family, T codec) {
            Objects.requireNonNull(codec, "codec");
            if (codecs.putIfAbsent(Objects.requireNonNull(family, "family"), family.cast(codec)) != null) {
                throw new IllegalStateException("Codec family registered twice: " + family.getSimpleName());
            }
            return this;
        }

        public ProfileCodecs build() {
            return codecs.isEmpty() ? NONE : new ProfileCodecs(Map.copyOf(codecs));
        }
    }
}
