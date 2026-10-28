package net.swordie.ms.connection.hotfix;

import net.swordie.ms.ServerConstants;
import net.swordie.ms.client.Client;
import net.swordie.ms.connection.packet.Login;
import net.swordie.ms.console.config.ServerRuntimeConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Login WZ hotfix: answers the client's APPLIED_HOT_FIX request with SET_HOT_FIX carrying a WZ override image.
 * <p>
 * Config (config/server.properties, or -D system properties which win):
 * <ul>
 *     <li>{@code hotfix.enabled} (default false) - master switch; off = nothing is read or sent (previous behaviour);</li>
 *     <li>{@code hotfix.source} {@code xml} (default) builds the image from {@code hotfix.xml}
 *     (default data/hotfix/Data.xml, see {@link HotfixDocument}); {@code raw} sends the bytes of
 *     {@code hotfix.raw} (default data/resources/Data2.wz, the 265 sample) unchanged;</li>
 *     <li>{@code hotfix.tms282.allowUnverified} (default false) - required as well while the protocol is 282.</li>
 * </ul>
 * 282 status (2026-09-28): the hotfix packets are NOT verified for the 282.3 client.
 * research/tms2823/packet-audit/OutHeader.tsv lists SET_HOT_FIX 0x002A as UNVERIFIED_282 (shared number with
 * SECURITY_PACKET_CODE_ATTACH), and CP 0x00AE - the 265 APPLIED_HOT_FIX number - is CAPTURED in 282 as
 * CLIENT_PACKET_DECODE_ERROR (opcode-status-282.tsv; handled by Tms282ReportHandlers), so a 282 client never sends
 * the request this handler answers, and the LP002A body layout / string key of the 282 hotfix reader are unknown.
 * Sending anything on the 282 login socket is therefore double-gated.
 */
public final class HotfixService {
    public static final String ENABLED = "hotfix.enabled";
    public static final String SOURCE = "hotfix.source";
    public static final String XML = "hotfix.xml";
    public static final String RAW = "hotfix.raw";
    public static final String ALLOW_UNVERIFIED_282 = "hotfix.tms282.allowUnverified";

    private static final HotfixService INSTANCE = new HotfixService();

    private record Cached(Path file, long modified, byte[] payload) {
    }

    private volatile Cached cached;
    private volatile boolean warned282;

    private HotfixService() {
    }

    public static HotfixService get() {
        return INSTANCE;
    }

    static void log(String msg) {
        System.out.println("[hotfix] " + msg);
    }

    private static String value(String key, String fallback) {
        String raw = System.getProperty(key);
        if (raw == null || raw.isBlank()) {
            raw = ServerRuntimeConfig.stringValue(key);
        }
        return raw == null || raw.isBlank() ? fallback : raw.trim();
    }

    public static boolean enabled() {
        return Boolean.parseBoolean(value(ENABLED, "false"));
    }

    private static Path resolve(String p) {
        Path path = Path.of(p);
        return path.isAbsolute() ? path : Path.of(ServerConstants.DIR).resolve(path);
    }

    /** Called from LoginHandler.handleAppliedHotFix. Sends nothing unless enabled (and allowed for 282). */
    public void onAppliedHotFix(Client c) {
        if (c == null || !enabled()) {
            return;
        }
        if (ServerConstants.version >= 282 && !Boolean.parseBoolean(value(ALLOW_UNVERIFIED_282, "false"))) {
            if (!warned282) {
                warned282 = true;
                log("hotfix.enabled=true ignored: SET_HOT_FIX / APPLIED_HOT_FIX are unverified for 282 "
                        + "(set " + ALLOW_UNVERIFIED_282 + "=true to send anyway)");
            }
            return;
        }
        byte[] payload = payload();
        if (payload == null) {
            return;
        }
        c.write(Login.setHotFix(payload));
    }

    /** Current payload (cached while the source file is unchanged), or null when it cannot be built. */
    public byte[] payload() {
        boolean raw = "raw".equalsIgnoreCase(value(SOURCE, "xml"));
        Path file = resolve(raw ? value(RAW, "data/resources/Data2.wz") : value(XML, "data/hotfix/Data.xml"));
        long modified;
        try {
            modified = Files.getLastModifiedTime(file).toMillis();
        } catch (IOException e) {
            log("source missing: " + file);
            return null;
        }
        Cached cur = cached;
        if (cur != null && cur.file().equals(file) && cur.modified() == modified) {
            return cur.payload();
        }
        byte[] payload;
        try {
            payload = raw ? Files.readAllBytes(file) : build(file);
        } catch (IOException e) {
            log("read " + file + " failed: " + e);
            return null;
        }
        cached = new Cached(file, modified, payload);
        return payload;
    }

    /** Drops the cached payload and path lookups (after editing an included file or the WZ dump). */
    public void reload() {
        cached = null;
        HotfixPaths.clearCaches();
    }

    private static byte[] build(Path xml) {
        HotfixDocument doc = HotfixDocument.parse(xml);
        List<HotfixEntry> entries = doc.entries();
        for (String w : doc.warnings()) {
            log("warning: " + w);
        }
        byte[] out = WzImageWriter.write(entries);
        log("built " + entries.size() + " entries, " + out.length + " bytes from " + xml);
        return out;
    }

    /**
     * Offline check: {@code java -cp maplestory.jar net.swordie.ms.connection.hotfix.HotfixService [Data.xml] [out.img]}
     * (run from the server root so WZ_DIR resolves). Prints every entry and writes the image.
     */
    public static void main(String[] args) throws IOException {
        Path xml = resolve(args.length > 0 ? args[0] : "data/hotfix/Data.xml");
        HotfixDocument doc = HotfixDocument.parse(xml);
        for (HotfixEntry e : doc.entries()) {
            System.out.println(e.path() + " = " + e.value());
        }
        for (String w : doc.warnings()) {
            System.out.println("WARN " + w);
        }
        byte[] img = WzImageWriter.write(doc.entries());
        if (args.length > 1) {
            Files.write(Path.of(args[1]), img);
        }
        System.out.println(doc.entries().size() + " entries, " + img.length + " bytes");
    }
}
