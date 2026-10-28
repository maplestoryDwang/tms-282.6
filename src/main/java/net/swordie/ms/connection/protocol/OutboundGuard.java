package net.swordie.ms.connection.protocol;

import net.swordie.ms.connection.OutPacket;

import java.util.function.Supplier;

/**
 * Last check before a packet is framed. Implementations must be fail-closed: a packet the profile cannot
 * prove it understands is dropped, never "sent and hoped for".
 */
public interface OutboundGuard {
    OutboundDecision inspect(OutPacket packet);

    /**
     * Inspects and logs a drop.
     *
     * @param target who the packet was for (IP, character or broadcast scope); evaluated only for the log line
     * @return whether the caller may send the packet
     */
    default boolean permit(OutPacket packet, Supplier<String> target) {
        OutboundDecision decision = inspect(packet);
        if (!decision.allowed()) {
            String who;
            try {
                who = target.get();
            } catch (RuntimeException e) {
                who = "?";
            }
            OutboundDropLog.record(packet, who, decision.reason());
        }
        return decision.allowed();
    }
}
