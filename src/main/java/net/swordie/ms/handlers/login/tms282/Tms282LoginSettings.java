package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.connection.protocol.ProtocolConfig;
import net.swordie.ms.connection.packet.Tms282CharacterPackets;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;

/**
 * Settings of the TMS 282.3 login overlay, from {@code config/server.properties}; a JVM {@code -D} property with
 * the same key wins, and the research probe's own {@code -Dtms.*} names are accepted as fallbacks.
 * <ul>
 *     <li>{@code tms282.researchDir}: research folder, default {@code ../../research/tms2823} (relative to server/265);</li>
 *     <li>{@code tms282.preauthDir}: the six official pre-login packets answered to CP0071 (send-*.bin), default
 *     {@code <researchDir>/official-client-preauth} (probe: {@code -Dtms.preauth});</li>
 *     <li>{@code tms282.fileReference}: the verified CP0078 client file report, default
 *     {@code <researchDir>/client-file-reference/report.bin};</li>
 *     <li>{@code tms282.world0Type}: LP0013 world type of world 0 (1, 5 or 6; default 6), probe {@code -Dtms.world0Type};</li>
 *     <li>{@code tms282.nameCharset}: character-name charset, default UTF-8 (probe {@code -Dtms.nameCharset});</li>
 *     <li>{@code tms282.avatarAux}: per-entry avatar AUX byte, default 0 (probe {@code -Dtms.avatarAux});</li>
 *     <li>{@code tms282.channelPing}: LP0020 keep-alive on the channel socket too, default false (probe
 *     {@code -Dtms.channelPing}).</li>
 *     <li>{@code tms282.questResult}: CP01A6 accept/opening script also answered with LP04D0 act 12 after the
 *     QuestRecord, default false (probe {@code -Dtms.questResult}; the 282 success handler answers it with one more
 *     CP01A6 act 7, and the QuestRecord alone updates the state).</li>
 * </ul>
 */
public record Tms282LoginSettings(Path preauthDir, Path fileReference, int world0Type, Charset nameCharset,
                                  int avatarAuxFlag, boolean channelPing, boolean questResult) {
    public static final String KEY_RESEARCH_DIR = "tms282.researchDir";
    public static final String KEY_PREAUTH_DIR = "tms282.preauthDir";
    public static final String KEY_FILE_REFERENCE = "tms282.fileReference";
    public static final String KEY_WORLD0_TYPE = "tms282.world0Type";
    public static final String KEY_NAME_CHARSET = "tms282.nameCharset";
    public static final String KEY_AVATAR_AUX = "tms282.avatarAux";
    public static final String KEY_CHANNEL_PING = "tms282.channelPing";
    public static final String KEY_QUEST_RESULT = "tms282.questResult";
    public static final Path DEFAULT_RESEARCH_DIR = Path.of("..", "..", "research", "tms2823");

    public Tms282LoginSettings {
        Objects.requireNonNull(preauthDir, "preauthDir");
        Objects.requireNonNull(fileReference, "fileReference");
        Objects.requireNonNull(nameCharset, "nameCharset");
        Tms282WorldCatalog.checkWorldType(world0Type);
    }

    /** Reads {@link ProtocolConfig#CONFIG_FILE} (when present) plus system properties. */
    public static Tms282LoginSettings load() {
        Properties p = new Properties();
        if (Files.isRegularFile(ProtocolConfig.CONFIG_FILE)) {
            try (InputStream in = Files.newInputStream(ProtocolConfig.CONFIG_FILE)) {
                p.load(in);
            } catch (IOException e) {
                throw new IllegalStateException("Cannot read " + ProtocolConfig.CONFIG_FILE, e);
            }
        }
        return fromProperties(p);
    }

    /** {@code file} settings with system-property overrides (tms282.* first, then the probe's tms.* names). */
    public static Tms282LoginSettings fromProperties(Properties file) {
        Path research = Path.of(value(file, KEY_RESEARCH_DIR, null, DEFAULT_RESEARCH_DIR.toString()));
        String preauth = value(file, KEY_PREAUTH_DIR, "tms.preauth", null);
        Path preauthDir = preauth == null ? research.resolve("official-client-preauth") : research.resolve(preauth);
        String reference = value(file, KEY_FILE_REFERENCE, null, null);
        Path fileReference = reference == null ? research.resolve("client-file-reference").resolve("report.bin")
                : Path.of(reference);
        String world0 = value(file, KEY_WORLD0_TYPE, "tms.world0Type",
                String.valueOf(Tms282CharacterPackets.WORLD_TYPE_COLLABORATION));
        return new Tms282LoginSettings(preauthDir.toAbsolutePath().normalize(), fileReference.toAbsolutePath().normalize(),
                Tms282WorldCatalog.parseWorldType(world0),
                Charset.forName(value(file, KEY_NAME_CHARSET, "tms.nameCharset", "UTF-8")),
                Integer.parseInt(value(file, KEY_AVATAR_AUX, "tms.avatarAux", "0").trim()),
                Boolean.parseBoolean(value(file, KEY_CHANNEL_PING, "tms.channelPing", "false").trim()),
                Boolean.parseBoolean(value(file, KEY_QUEST_RESULT, "tms.questResult", "false").trim()));
    }

    /** Defaults only (no config file); checks use it. */
    public static Tms282LoginSettings defaults() {
        return fromProperties(new Properties());
    }

    /** This configuration with another pre-login packet folder (checks). */
    public Tms282LoginSettings withPreauthDir(Path dir) {
        return new Tms282LoginSettings(dir, fileReference, world0Type, nameCharset, avatarAuxFlag, channelPing, questResult);
    }

    /** This configuration with LP04D0 after the QuestRecord on or off (checks). */
    public Tms282LoginSettings withQuestResult(boolean enabled) {
        return new Tms282LoginSettings(preauthDir, fileReference, world0Type, nameCharset, avatarAuxFlag, channelPing, enabled);
    }

    private static String value(Properties file, String key, String probeKey, String fallback) {
        String v = System.getProperty(key);
        if (v == null && probeKey != null) {
            v = System.getProperty(probeKey);
        }
        if (v == null) {
            v = file.getProperty(key);
        }
        return v == null || v.isBlank() ? fallback : v.trim();
    }
}
