package net.swordie.ms.connection.api.launcher.account;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Paper-doll images for the launcher (1.2.8): one PNG per look, drawn from the host's own 282 client files by
 * {@code tools/avatar/AvatarRender} in a separate java process (its WZ reader stays off the server's classpath; a
 * crash or a slow file read there cannot touch the game server) and cached under {@code logs/avatar-cache} by the
 * SHA-1 of the look. One render at a time; a request never waits longer than {@link #TIMEOUT_SEC}.
 * <p>
 * Locations (system properties, defaults relative to the server folder):
 * {@code avatar.render.dir} = {@code bin/avatar-render} (holding {@code classes} and {@code orz}),
 * {@code avatar.render.lib} = {@code D:\GMS\OrzRepacker\lib}, {@code avatar.render.data} = {@code ../../MapleStory/Data}.
 * Unavailable (any of them missing) is an answer, not an error: the launcher then shows no picture.
 */
public final class AvatarRenderService {
    public static final int TIMEOUT_SEC = 20;
    public static final int MAX_ITEMS = 40;
    public static final int MAX_BYTES = 200 * 1024;
    private static final AvatarRenderService STANDARD = new AvatarRenderService(
            Path.of(System.getProperty("avatar.render.dir", "bin/avatar-render")),
            Path.of(System.getProperty("avatar.render.lib", "D:\\GMS\\OrzRepacker\\lib")),
            Path.of(System.getProperty("avatar.render.data", "../../MapleStory/Data")),
            Path.of("logs", "avatar-cache"));

    /** @param items visible equip ids (order does not matter) */
    public record Look(int skin, int face, int hair, boolean elfEar, List<Integer> items) {
        public Look {
            items = items.stream().filter(i -> i != null && i >= 1_000_000 && i < 2_000_000).distinct().sorted()
                    .limit(MAX_ITEMS).toList();
        }

        String key() {
            String text = "v2|" + skin + "|" + face + "|" + hair + "|" + (elfEar ? 1 : 0) + "|"
                    + items.stream().map(String::valueOf).collect(Collectors.joining(","));
            try {
                return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(text.getBytes(StandardCharsets.UTF_8)));
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private final Path dir;
    private final Path lib;
    private final Path data;
    private final Path cache;
    private final ReentrantLock rendering = new ReentrantLock();

    public AvatarRenderService(Path dir, Path lib, Path data, Path cache) {
        this.dir = dir;
        this.lib = lib;
        this.data = data;
        this.cache = cache;
    }

    public static AvatarRenderService standard() {
        return STANDARD;
    }

    /** The renderer, its library and the client data are all there. */
    public boolean available() {
        return Files.isRegularFile(dir.resolve("classes").resolve("AvatarRender.class")) && Files.isDirectory(dir.resolve("orz"))
                && Files.isDirectory(lib) && Files.isDirectory(data.resolve("Character")) && Files.isDirectory(data.resolve("Base"));
    }

    /** The PNG of {@code look}, or null when it cannot be drawn (unavailable, busy for too long, unknown ids). */
    public byte[] png(Look look) {
        if (look.face() <= 0 || look.hair() <= 0 || look.skin() < 0) {
            return null;
        }
        Path file = cache.resolve(look.key() + ".png");
        try {
            if (Files.isRegularFile(file)) {
                return read(file);
            }
            if (!available()) {
                return null;
            }
            if (!rendering.tryLock(TIMEOUT_SEC, TimeUnit.SECONDS)) {
                return null;
            }
            try {
                if (!Files.isRegularFile(file)) {
                    render(look, file);
                }
            } finally {
                rendering.unlock();
            }
            return Files.isRegularFile(file) ? read(file) : null;
        } catch (IOException e) {
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    private static byte[] read(Path file) throws IOException {
        return Files.size(file) > MAX_BYTES ? null : Files.readAllBytes(file);
    }

    private void render(Look look, Path file) throws IOException, InterruptedException {
        Files.createDirectories(cache);
        Path tmp = cache.resolve(look.key() + ".tmp.png");
        String javaExe = ProcessHandle.current().info().command().orElse("java");
        String cp = dir.resolve("orz") + ";" + lib + "\\*;" + dir.resolve("classes");
        List<String> cmd = new ArrayList<>(List.of(javaExe, "-Xmx256m", "-cp", cp, "AvatarRender", data.toString(), tmp.toString(),
                String.valueOf(look.skin()), String.valueOf(look.face()), String.valueOf(look.hair()), look.elfEar() ? "1" : "0",
                look.items().stream().map(String::valueOf).collect(Collectors.joining(","))));
        Process p = new ProcessBuilder(cmd).redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
        try {
            if (!p.waitFor(TIMEOUT_SEC, TimeUnit.SECONDS)) {
                return;
            }
            if (p.exitValue() == 0 && Files.isRegularFile(tmp) && Files.size(tmp) > 100 && Files.size(tmp) <= MAX_BYTES) {
                Files.move(tmp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            if (p.isAlive()) {
                p.destroyForcibly();
            }
            Files.deleteIfExists(tmp);
        }
    }
}
