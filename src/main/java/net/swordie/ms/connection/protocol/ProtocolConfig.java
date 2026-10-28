package net.swordie.ms.connection.protocol;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;

/**
 * Protocol settings from {@code config/server.properties}; a JVM system property with the same key wins.
 * <ul>
 *     <li>{@code protocol.version}: {@code gms265} (default, the current behavior) or {@code tms282.3};</li>
 *     <li>{@code protocol.dataDir}: opcode tables root, default {@code data/protocol};</li>
 *     <li>{@code capture.recv}: {@code true} writes every decoded client packet to disk (default false);</li>
 *     <li>{@code capture.send}: {@code true} writes every packet that reaches the encoder (plaintext, after the
 *     OutboundGuard) to disk and reports guard drops (default false);</li>
 *     <li>{@code capture.trace}: {@code true} also appends the captured directions to the GUI's packet trace
 *     ({@code logs/tms282-packets.jsonl}, 282 封包 tab; default false);</li>
 *     <li>{@code capture.dir}: capture root, default {@code logs/capture}; folders go to {@code <dir>/sessions};</li>
 *     <li>{@code capture.sessionPrefix}: prepended to every session folder name (default empty; the formal
 *     launcher uses {@code formal-} next to the probe's folders).</li>
 * </ul>
 * With every capture key at its default (the 265 setup) no capture handler is installed.
 *
 * @param version              selected protocol
 * @param dataRoot             directory holding {@code <version id>/inbound.tsv} and {@code outbound.tsv}
 * @param captureRecv          whether CaptureInboundHandler is installed
 * @param captureRoot          where capture sessions go ({@code <captureRoot>/sessions/...})
 * @param captureSend          whether CaptureOutboundHandler is installed
 * @param captureTrace         whether captured packets also go to the packet trace JSONL
 * @param captureSessionPrefix session folder name prefix
 */
public record ProtocolConfig(ProtocolVersion version, Path dataRoot, boolean captureRecv, Path captureRoot,
                             boolean captureSend, boolean captureTrace, String captureSessionPrefix) {
    public static final Path CONFIG_FILE = Path.of("config", "server.properties");
    public static final String KEY_VERSION = "protocol.version";
    public static final String KEY_DATA_DIR = "protocol.dataDir";
    public static final String KEY_CAPTURE_RECV = "capture.recv";
    public static final String KEY_CAPTURE_DIR = "capture.dir";
    public static final String KEY_CAPTURE_SEND = "capture.send";
    public static final String KEY_CAPTURE_TRACE = "capture.trace";
    public static final String KEY_CAPTURE_PREFIX = "capture.sessionPrefix";

    public ProtocolConfig {
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(dataRoot, "dataRoot");
        Objects.requireNonNull(captureRoot, "captureRoot");
        captureSessionPrefix = captureSessionPrefix == null ? "" : captureSessionPrefix.trim();
        if (!captureSessionPrefix.matches("[A-Za-z0-9_.-]*")) {
            throw new IllegalArgumentException("capture.sessionPrefix may only hold letters, digits, '_', '.', '-'");
        }
    }

    /** Receive capture only (send and trace off, no prefix): the settings before capture.send existed. */
    public ProtocolConfig(ProtocolVersion version, Path dataRoot, boolean captureRecv, Path captureRoot) {
        this(version, dataRoot, captureRecv, captureRoot, false, false, "");
    }

    /** True when any capture handler is installed. */
    public boolean captureAny() {
        return captureRecv || captureSend;
    }

    /** Defaults only: GMS 265, capture off. */
    public static ProtocolConfig defaults() {
        return fromProperties(new Properties());
    }

    /** Reads {@link #CONFIG_FILE} (if present) plus system property overrides. */
    public static ProtocolConfig load() {
        Properties p = new Properties();
        if (Files.isRegularFile(CONFIG_FILE)) {
            try (InputStream in = Files.newInputStream(CONFIG_FILE)) {
                p.load(in);
            } catch (IOException e) {
                throw new IllegalStateException("Cannot read " + CONFIG_FILE, e);
            }
        }
        for (String key : new String[]{KEY_VERSION, KEY_DATA_DIR, KEY_CAPTURE_RECV, KEY_CAPTURE_DIR,
                KEY_CAPTURE_SEND, KEY_CAPTURE_TRACE, KEY_CAPTURE_PREFIX}) {
            String override = System.getProperty(key);
            if (override != null) {
                p.setProperty(key, override);
            }
        }
        return fromProperties(p);
    }

    public static ProtocolConfig fromProperties(Properties p) {
        return new ProtocolConfig(
                ProtocolVersion.parse(p.getProperty(KEY_VERSION, ProtocolVersion.GMS_265.id())),
                Path.of(p.getProperty(KEY_DATA_DIR, "data/protocol").trim()),
                Boolean.parseBoolean(p.getProperty(KEY_CAPTURE_RECV, "false").trim()),
                Path.of(p.getProperty(KEY_CAPTURE_DIR, "logs/capture").trim()),
                Boolean.parseBoolean(p.getProperty(KEY_CAPTURE_SEND, "false").trim()),
                Boolean.parseBoolean(p.getProperty(KEY_CAPTURE_TRACE, "false").trim()),
                p.getProperty(KEY_CAPTURE_PREFIX, ""));
    }

    /** Opcode table directory of the selected version. */
    public Path versionDataDir() {
        return dataRoot.resolve(version.id());
    }

    /**
     * Builds the profile this config selects. TMS 282.3 is the phase-1 profile: skeleton opcode table, login and
     * channel-entry overlay (settings: {@code tms282.*} keys, see Tms282LoginSettings).
     */
    public ProtocolProfile createProfile() {
        return switch (version) {
            case GMS_265 -> Gms265Profile.INSTANCE;
            case TMS_282_3 -> net.swordie.ms.handlers.login.tms282.Tms282Profiles.standard(versionDataDir());
        };
    }
}
