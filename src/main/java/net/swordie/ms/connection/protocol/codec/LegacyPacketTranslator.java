package net.swordie.ms.connection.protocol.codec;

import net.swordie.ms.connection.OutPacket;

import java.util.Optional;

/**
 * Last-chance translation of a finished 265 packet (built by a 265 builder from an OutHeader, not by a profile
 * codec) into the active profile's wire form, for the small 265 packets that many call sites build directly
 * (chat-log lines, the exclusive-request unlock, in-game direction mode, MESSAGE records). {@code NettyClient.write}
 * asks the active profile for this family right before its OutboundGuard; a translated packet is
 * {@link OutPacket#forProfile built for} the profile, anything the translator declines keeps going to the guard
 * unchanged (and is dropped there as before).
 * <p>
 * Codec family of {@link net.swordie.ms.connection.protocol.ProfileCodecs}; the GMS 265 profile registers none, so
 * its packets are never touched.
 */
public interface LegacyPacketTranslator {

    /**
     * The profile packet for {@code legacy}, or empty when this translator does not know its 265 header or body.
     * Must not throw for a body it declines; a malformed body of a known header is declined too.
     */
    Optional<OutPacket> translate(OutPacket legacy);

    /** {@code packet} translated by the active profile's translator, else {@code packet} itself. */
    static OutPacket apply(OutPacket packet) {
        if (packet == null) {
            return null;
        }
        Optional<LegacyPacketTranslator> translator = ProfileEncoders.find(LegacyPacketTranslator.class);
        if (translator.isEmpty()) {
            return packet;
        }
        OutPacket out = packet;
        if (packet.getBuiltFor() == null && packet.getOrigin() != null) {
            try {
                out = translator.get().translate(packet).orElse(packet);
            } catch (RuntimeException e) {
                System.out.printf("[LegacyPacketTranslator] %s not translated: %s%n", packet.getOrigin(), e);
            }
        }
        try {
            translator.get().observe(out);
        } catch (RuntimeException e) {
            System.out.printf("[LegacyPacketTranslator] observe failed: %s%n", e);
        }
        return out;
    }

    /** Sees every packet about to be written (translated or not); the default ignores it. */
    default void observe(OutPacket sent) {
    }
}
