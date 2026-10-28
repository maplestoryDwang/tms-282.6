package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.protocol.ProtocolConfig;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Properties;

/**
 * The TMS 282.x client build the login server speaks to: {@code tms282.clientBuild} in {@code config/server.properties}
 * (a JVM {@code -D} property with the same key wins), 28203 / 28204 / 28205, default 28205. Read once at start-up.
 * <p>
 * One build at a time: the client compares the hello build ints for equality (282.4 346CBBE / 282.5 346CF1E
 * {@code cmp [rsp+0x8c], build; jne}), so a 282.4 client cannot log in to a 282.5 server or the other way round.
 * Between the minor versions only these values differ (research/tms2824/PORT.md, research/tms2825/PORT.md):
 * <ul>
 *     <li>hello patch string {@code "<minor>:1"} and the two hello build ints;</li>
 *     <li>CP0071 build at offset 7;</li>
 *     <li>CP0078 magic2 {@code 0x68860C00 + minor - 3} (and the MapleStory.exe CRC in the file reference).</li>
 * </ul>
 */
public final class Tms282ClientBuild {
    public static final String KEY = "tms282.clientBuild";
    public static final int DEFAULT_BUILD = 28205;
    public static final int MIN_BUILD = 28203;
    public static final int MAX_BUILD = 28205;
    private static final int BUILD = load();

    private Tms282ClientBuild() {
    }

    /** 28203 / 28204 / 28205. */
    public static int build() {
        return BUILD;
    }

    /** 3 / 4 / 5. */
    public static int minor() {
        return BUILD - 28200;
    }

    /** Login hello patch string, e.g. {@code "5:1"}. */
    public static String patch() {
        return minor() + ":1";
    }

    /** "282.5" - what the launcher shows and compares with the player's MapleStory.exe. */
    public static String protocol() {
        return "282." + minor();
    }

    /** CP0078 magic2 of this build: 0x68860C00 (282.3), 0x68860C01 (282.4), 0x68860C02 (282.5). */
    public static int fileReportMagic() {
        return 0x68860C00 + minor() - 3;
    }

    static int parse(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_BUILD;
        }
        int b = Integer.parseInt(value.trim());
        if (b < MIN_BUILD || b > MAX_BUILD) {
            throw new IllegalStateException(KEY + "=" + b + ": known builds are " + MIN_BUILD + ".." + MAX_BUILD);
        }
        return b;
    }

    private static int load() {
        String v = System.getProperty(KEY);
        if (v == null && Files.isRegularFile(ProtocolConfig.CONFIG_FILE)) {
            Properties p = new Properties();
            try (InputStream in = Files.newInputStream(ProtocolConfig.CONFIG_FILE)) {
                p.load(in);
            } catch (IOException e) {
                throw new IllegalStateException("Cannot read " + ProtocolConfig.CONFIG_FILE, e);
            }
            v = p.getProperty(KEY);
        }
        int b = parse(v);
        System.out.println("[TMS282] client build " + b + " (" + "282." + (b - 28200) + ")");
        return b;
    }
}
