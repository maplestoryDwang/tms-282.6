package net.swordie.ms.connection.protocol;

import net.swordie.ms.connection.OutPacket;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Diagnostic switch for isolating a client crash: packets matching {@code debug.suppressOutbound} are not sent
 * (the caller reports them as DROPPED, so they stay visible in the packet trace). Empty by default; nothing is
 * suppressed and every profile behaves as before.
 * <p>
 * Syntax (JVM property or config/server.properties): comma-separated wire opcodes in hex, optionally with the first
 * body byte, e.g. {@code debug.suppressOutbound=074D,00AE:03} suppresses every LP074D and the LP00AE whose type
 * byte is 0x03. Read once at startup.
 */
public final class DebugOutboundFilter {
    public static final String KEY = "debug.suppressOutbound";
    private static final List<int[]> RULES = load();

    private DebugOutboundFilter() {
    }

    /** Whether {@code packet} (wire bytes: u16 opcode + body) is to be held back. */
    public static boolean suppress(OutPacket packet) {
        if (RULES.isEmpty() || packet == null) {
            return false;
        }
        byte[] d = packet.getData();
        if (d == null || d.length < 2) {
            return false;
        }
        int op = (d[0] & 0xFF) | (d[1] & 0xFF) << 8;
        for (int[] r : RULES) {
            if (r[0] == op && (r[1] < 0 || (d.length > 2 && (d[2] & 0xFF) == r[1]))) {
                return true;
            }
        }
        return false;
    }

    public static List<int[]> rules() {
        return RULES;
    }

    static List<int[]> load() {
        String v = System.getProperty(KEY);
        if (v == null && Files.exists(ProtocolConfig.CONFIG_FILE)) {
            Properties p = new Properties();
            try (InputStream in = Files.newInputStream(ProtocolConfig.CONFIG_FILE)) {
                p.load(in);
                v = p.getProperty(KEY);
            } catch (IOException e) {
                System.out.println("[DebugOutboundFilter] cannot read " + ProtocolConfig.CONFIG_FILE + ": " + e);
            }
        }
        return parse(v);
    }

    static List<int[]> parse(String v) {
        List<int[]> rules = new ArrayList<>();
        if (v == null || v.isBlank()) {
            return rules;
        }
        for (String part : v.split(",")) {
            String s = part.trim();
            if (s.isEmpty()) {
                continue;
            }
            String[] f = s.split(":");
            rules.add(new int[]{Integer.parseInt(f[0].trim(), 16), f.length > 1 ? Integer.parseInt(f[1].trim(), 16) : -1});
        }
        System.out.println("[DebugOutboundFilter] suppressing outbound " + v.trim());
        return List.copyOf(rules);
    }
}
