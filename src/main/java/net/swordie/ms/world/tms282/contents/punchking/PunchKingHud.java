package net.swordie.ms.world.tms282.contents.punchking;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolConfig;
import net.swordie.ms.connection.protocol.ProtocolVersion;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Properties;

/**
 * Sends the punch-king HUD (LP0833, {@link PunchKingHudPackets}) to a character standing in a fieldType-236 map.
 * Event scripts call {@link #clock}, {@link #score}, {@link #state} (e.g. the revival punch-king event after the
 * merge: {@code PunchKingHud.clock(chr, 125)} on start, {@code PunchKingHud.score(chr, total)} on hit).
 * <p>
 * Off by default: {@code tms282.punchking.hud=true} in config/server.properties (or the same system property)
 * turns it on, after a GM confirmed the sub meanings in game with {@code @punchhud}. Off, outside a HUD map, on a
 * non-282 profile or with a null character, nothing is sent and the methods return false.
 */
public final class PunchKingHud {
    public static final String PROPERTY = "tms282.punchking.hud";
    private static volatile Boolean enabled;

    private PunchKingHud() {
    }

    public static boolean enabled() {
        Boolean e = enabled;
        if (e == null) {
            e = load();
            enabled = e;
        }
        return e;
    }

    /** Tests and the GM toggle; null re-reads the configuration. */
    public static void setEnabled(Boolean value) {
        enabled = value;
    }

    private static boolean load() {
        String sys = System.getProperty(PROPERTY);
        if (sys != null) {
            return Boolean.parseBoolean(sys.trim());
        }
        Properties p = new Properties();
        if (Files.isRegularFile(ProtocolConfig.CONFIG_FILE)) {
            try (InputStream in = Files.newInputStream(ProtocolConfig.CONFIG_FILE)) {
                p.load(in);
            } catch (IOException e) {
                return false;
            }
        }
        return Boolean.parseBoolean(p.getProperty(PROPERTY, "false").trim());
    }

    public static boolean clock(Char chr, int seconds) {
        return send(chr, PunchKingHudPackets.clock(seconds));
    }

    public static boolean score(Char chr, long score) {
        return send(chr, PunchKingHudPackets.score((int) Math.max(0, Math.min(Integer.MAX_VALUE, score))));
    }

    public static boolean state(Char chr, int state) {
        return send(chr, PunchKingHudPackets.state(state));
    }

    public static boolean mobValue(Char chr, int mobId, int value) {
        return send(chr, PunchKingHudPackets.mobValue(mobId, value));
    }

    /** True when {@code chr} can receive the HUD now (flag aside). */
    public static boolean inHudField(Char chr) {
        return chr != null && chr.getField() != null && PunchKingHudPackets.isHudField(chr.getField().getId())
                && Protocol.active().version() == ProtocolVersion.TMS_282_3;
    }

    private static boolean send(Char chr, OutPacket packet) {
        if (!enabled() || !inHudField(chr)) {
            return false;
        }
        chr.write(packet);
        return true;
    }
}
