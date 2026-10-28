package net.swordie.ms.connection.api.launcher.release;

import net.swordie.ms.connection.packet.Tms282FileReport;
import net.swordie.ms.console.config.ServerRuntimeConfig;
import net.swordie.ms.handlers.login.tms282.Tms282LoginSettings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * The values of {@code manifestMeta} (P8), computed exactly like cat-launcher's scripts/build-remote-helper.mjs:
 * <ul>
 *     <li>{@code referenceSha256}: SHA-256 (upper-case hex) of the whole {@code tms282.fileReference} file
 *     ({@link Tms282LoginSettings#fileReference()}, the trusted CP0078 report);</li>
 *     <li>{@code exeSha256}: SHA-256 of the host's client {@code MapleStory.exe} - setting {@value #CLIENT_EXE_KEY}
 *     (-D first, then server.properties), default {@value #DEFAULT_CLIENT_EXE};</li>
 *     <li>{@code count}: the number of files of that report ({@link Tms282FileReport}).</li>
 * </ul>
 * Both hashes are cached by path + size + modification time ({@link FileHashCache}); a missing or unreadable file
 * gives empty (the action answers unavailable - never an empty or made-up hash).
 */
public final class ClientManifestMeta {
    public static final String CLIENT_EXE_KEY = "launcher.clientExe";
    public static final String DEFAULT_CLIENT_EXE = "D:\\TMS\\MapleStory\\MapleStory.exe";
    private static final ClientManifestMeta STANDARD = new ClientManifestMeta(() -> Tms282LoginSettings.load().fileReference(),
            ClientManifestMeta::configuredClientExe, FileHashCache.standard());

    public record Meta(String referenceSha256, String exeSha256, int count, Path reference, Path clientExe) {
    }

    private final Supplier<Path> reference;
    private final Supplier<Path> clientExe;
    private final FileHashCache hashes;
    private String countKey;
    private int count;

    public ClientManifestMeta(Supplier<Path> reference, Supplier<Path> clientExe, FileHashCache hashes) {
        this.reference = Objects.requireNonNull(reference, "reference");
        this.clientExe = Objects.requireNonNull(clientExe, "clientExe");
        this.hashes = Objects.requireNonNull(hashes, "hashes");
    }

    public static ClientManifestMeta standard() {
        return STANDARD;
    }

    public static Path configuredClientExe() {
        String raw = System.getProperty(CLIENT_EXE_KEY);
        if (raw == null || raw.isBlank()) {
            raw = ServerRuntimeConfig.stringValue(CLIENT_EXE_KEY);
        }
        return Path.of(raw == null || raw.isBlank() ? DEFAULT_CLIENT_EXE : raw.trim()).toAbsolutePath().normalize();
    }

    /** The three values, or empty when the report or the exe cannot be read (one reason in {@code why[0]} if given). */
    public Optional<Meta> read(String[] why) {
        Path ref;
        Path exe;
        try {
            ref = reference.get();
            exe = clientExe.get();
        } catch (RuntimeException e) {
            return fail(why, "settings");
        }
        String refSha;
        String exeSha;
        int n;
        try {
            refSha = hashes.sha256(ref);
            n = count(ref);
        } catch (IOException | RuntimeException e) {
            return fail(why, "reference");
        }
        try {
            exeSha = hashes.sha256(exe);
        } catch (IOException | RuntimeException e) {
            return fail(why, "clientExe");
        }
        return Optional.of(new Meta(refSha, exeSha, n, ref, exe));
    }

    private synchronized int count(Path ref) throws IOException {
        String key = ref.toAbsolutePath().normalize() + "|" + FileHashCache.key(ref);
        if (!key.equals(countKey)) {
            if (Files.isSymbolicLink(ref) || !Files.isRegularFile(ref, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException("not a regular file");
            }
            byte[] bytes = Files.readAllBytes(ref);
            count = Tms282FileReport.parse(bytes).fileCrcs().size();
            countKey = key;
        }
        return count;
    }

    private static Optional<Meta> fail(String[] why, String reason) {
        if (why != null && why.length > 0) {
            why[0] = reason;
        }
        return Optional.empty();
    }
}
