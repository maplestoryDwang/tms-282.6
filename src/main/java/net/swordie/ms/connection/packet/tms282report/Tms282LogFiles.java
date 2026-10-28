package net.swordie.ms.connection.packet.tms282report;

import java.net.URISyntaxException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Path;
import java.security.CodeSource;

/**
 * Path resolution shared by every file the 282 login probe writes and the server GUI reads
 * ({@link JsonlClientReportSink#defaultFile()}, {@link JsonlPacketTraceSink#defaultFile()},
 * {@link Tms282OpcodeNames#defaultDir()}).
 * <p>
 * The server root is the parent of the {@code bin} directory these classes are loaded from
 * ({@code bin/classes} or the {@code bin/*.jar}), i.e. {@code D:\TMS\server\265}; both processes load them
 * from there, so they agree without depending on their working directories. Only when the classes come
 * from elsewhere is the working directory used as the server root.
 */
public final class Tms282LogFiles {
    private Tms282LogFiles() {
    }

    /**
     * Always absolute: the system property {@code property} when set, otherwise
     * {@code <server root>/logs/<defaultName>}.
     */
    public static Path logFile(String property, String defaultName) {
        String configured = System.getProperty(property);
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured.trim()).toAbsolutePath().normalize();
        }
        return serverRoot().resolve("logs").resolve(defaultName).toAbsolutePath().normalize();
    }

    /** Absolute server root (see the class comment). */
    public static Path serverRoot() {
        try {
            CodeSource code = Tms282LogFiles.class.getProtectionDomain().getCodeSource();
            if (code != null && code.getLocation() != null) {
                Path bin = Path.of(code.getLocation().toURI()).toAbsolutePath().getParent();
                if (bin != null && bin.getFileName() != null && bin.getFileName().toString().equalsIgnoreCase("bin")
                        && bin.getParent() != null) {
                    return bin.getParent();
                }
            }
        } catch (URISyntaxException | IllegalArgumentException | FileSystemNotFoundException | SecurityException ignored) {
            // Fall through to the working directory.
        }
        return Path.of("").toAbsolutePath();
    }
}
