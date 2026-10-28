package net.swordie.ms.connection.protocol.codec;

/**
 * Codec family: whether the active profile's client has data for an item. The GMS 265 server data holds items a
 * newer client no longer ships; a TMS 282.3 shop dialog listing one throws assertion 0x21000003 and the client
 * disconnects. GMS 265 registers none and falls back to {@link #ALL}; TMS 282.3 registers
 * {@code Tms282ItemAvailability}.
 */
public interface ItemAvailability {
    ItemAvailability ALL = itemId -> true;

    boolean isAvailable(int itemId);

    /** The active profile's availability, else {@link #ALL}. */
    static ItemAvailability active() {
        return ProfileEncoders.find(ItemAvailability.class).orElse(ALL);
    }
}
