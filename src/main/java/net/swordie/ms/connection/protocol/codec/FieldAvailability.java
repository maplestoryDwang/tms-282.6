package net.swordie.ms.connection.protocol.codec;

/**
 * Codec family: whether the active profile's client can load a field. The GMS 265 server data holds maps a newer
 * client no longer ships; sending such a client there makes it crash while loading the map. GMS 265 registers
 * none and falls back to {@link #ALL} (every field available, behaviour unchanged); TMS 282.3 registers
 * {@code Tms282FieldAvailability}.
 */
public interface FieldAvailability {
    FieldAvailability ALL = fieldId -> true;

    boolean isAvailable(int fieldId);

    /** The active profile's availability, else {@link #ALL}. */
    static FieldAvailability active() {
        return ProfileEncoders.find(FieldAvailability.class).orElse(ALL);
    }
}
