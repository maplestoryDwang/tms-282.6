package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.handlers.header.OutHeader;

import java.util.Optional;

/**
 * 265 CREATE_FORCE_ATOM (784) -> TMS 282.3 <b>LP032B</b> (CField 377C380). Before this contributor the 282 profile
 * dropped every force atom (OutboundGuard PROFILE_CODEC): Demon Slayer fury, Phantom cards, Kaiser swords, Night Walker
 * bats, Xenon rockets, Kinesis orbs, Wind Archer arrows, Evan debris, Illium javelins, ... (25 job handlers).
 * <p>
 * The 265 body is parsed with the 265 encoder's own rules and re-encoded with the 282 reader's rules
 * ({@link Tms282ForceAtomPackets}); the main change is that 282 no longer reads the 265 default post "i32 -1".
 * Fields the 265 body cannot carry get neutral values: type 90's i32 x 3 + rect (4, 550, 0, empty rect), type 35's
 * trailing i32 pair (0, 0). 265 type 62 (no target count) is declined. Callers that want the exact values can send
 * {@link Tms282ForceAtomPackets#create} directly.
 * <p>
 * {@code tms282.forceatom.visible=false} (server.properties / -D) drops them again.
 */
public final class Tms282ForceAtomTranslators implements Tms282LegacyPackets.Contributor {
    public static final Tms282ForceAtomTranslators INSTANCE = new Tms282ForceAtomTranslators();

    public static final boolean VISIBLE = !"false".equalsIgnoreCase(property("tms282.forceatom.visible"));

    private Tms282ForceAtomTranslators() {
    }

    private static String property(String key) {
        String v = System.getProperty(key);
        if (v != null) {
            return v.trim();
        }
        try {
            String c = net.swordie.ms.console.config.ServerRuntimeConfig.stringValue(key);
            return c == null ? "" : c.trim();
        } catch (RuntimeException | LinkageError e) {
            return "";
        }
    }

    @Override
    public void contribute(Tms282LegacyPackets.Builder builder) {
        builder.on(OutHeader.CREATE_FORCE_ATOM, p -> VISIBLE ? create(p) : Optional.empty());
    }

    static Optional<OutPacket> create(OutPacket legacy) {
        return Optional.of(Tms282ForceAtomPackets.fromLegacy(Tms282LegacyPackets.Body.of(legacy)).encode());
    }
}
