package net.swordie.ms.connection.api.launcher.daily;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Item icons for the launcher's sign-in pages (2026-10-08): the {@code info/icon} of a reward item as a small PNG, read
 * from the host's own 282 client files by {@code tools/avatar/ItemIconRender} in a separate java process (the WZ reader
 * stays off the server's classpath, like {@code AvatarRenderService}) and kept under {@code logs/item-icon-cache} as
 * {@code <itemId>.png}. An icon is extracted once: afterwards a request is one small file read.
 * <p>
 * {@link #ensure} extracts every missing icon of a list in one run (about two seconds for thirty items); it is called
 * when the reward table or the streak bonuses change and, as a fallback, by the first request that misses. One run at
 * a time. An item without an icon is remembered for {@link #RETRY_MISSING_MS} so it does not start a process again.
 * <p>
 * Locations are the renderer's (system properties, defaults relative to the server folder): {@code avatar.render.dir}
 * = {@code bin/avatar-render} (holding {@code classes} with ItemIconRender.class, and {@code orz}),
 * {@code avatar.render.lib}, {@code avatar.render.data} = {@code ../../MapleStory/Data}. Unavailable (any of them
 * missing) is an answer, not an error: the launcher then keeps its generic icon.
 */
public final class ItemIconService {
    public static final int TIMEOUT_SEC = 40;
    public static final int LOCK_WAIT_SEC = 15;
    /** Icons are 0.3 - 2 KB; anything larger is not an icon. */
    public static final int MAX_BYTES = 24 * 1024;
    public static final int MAX_BATCH = 64;
    public static final long RETRY_MISSING_MS = 6 * 3_600_000L;
    private static final byte[] PNG_MAGIC = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
    private static final ItemIconService STANDARD = new ItemIconService(
            Path.of(System.getProperty("avatar.render.dir", "bin/avatar-render")),
            Path.of(System.getProperty("avatar.render.lib", "D:\\GMS\\OrzRepacker\\lib")),
            Path.of(System.getProperty("avatar.render.data", "../../MapleStory/Data")),
            Path.of("logs", "item-icon-cache"));

    private final Path dir;
    private final Path lib;
    private final Path data;
    private final Path cache;
    private final ReentrantLock rendering = new ReentrantLock();
    private final Map<Integer, Long> missing = new ConcurrentHashMap<>();

    public ItemIconService(Path dir, Path lib, Path data, Path cache) {
        this.dir = dir;
        this.lib = lib;
        this.data = data;
        this.cache = cache;
    }

    public static ItemIconService standard() {
        return STANDARD;
    }

    public static boolean validId(int itemId) {
        return itemId >= 1_000_000 && itemId <= 9_999_999;
    }

    /** The extractor, its library and the client data are all there. */
    public boolean available() {
        return Files.isRegularFile(dir.resolve("classes").resolve("ItemIconRender.class")) && Files.isDirectory(dir.resolve("orz"))
                && Files.isDirectory(lib) && Files.isDirectory(data.resolve("Item"));
    }

    /** The stored icon, or null when there is none (yet). Never starts a process. */
    public byte[] cached(int itemId) {
        if (!validId(itemId)) {
            return null;
        }
        Path file = cache.resolve(itemId + ".png");
        try {
            if (!Files.isRegularFile(file)) {
                return null;
            }
            long size = Files.size(file);
            if (size < PNG_MAGIC.length + 20 || size > MAX_BYTES) {
                return null;
            }
            byte[] b = Files.readAllBytes(file);
            for (int i = 0; i < PNG_MAGIC.length; i++) {
                if (b[i] != PNG_MAGIC[i]) {
                    return null;
                }
            }
            return b;
        } catch (IOException e) {
            return null;
        }
    }

    /** True when {@code itemId} was looked for recently and has no icon (no process is started for it for a while). */
    public boolean knownMissing(int itemId) {
        Long at = missing.get(itemId);
        if (at == null) {
            return false;
        }
        if (System.currentTimeMillis() - at > RETRY_MISSING_MS) {
            missing.remove(itemId);
            return false;
        }
        return true;
    }

    /** The icon of {@code itemId}; when it is not stored yet and {@code extract} is set, every missing one of {@code together} is extracted first. */
    public byte[] png(int itemId, boolean extract, Collection<Integer> together) {
        byte[] b = cached(itemId);
        if (b != null || !extract || knownMissing(itemId)) {
            return b;
        }
        List<Integer> ids = new ArrayList<>();
        ids.add(itemId);
        if (together != null) {
            ids.addAll(together);
        }
        ensure(ids);
        return cached(itemId);
    }

    /** Extracts the icons of {@code ids} that are not stored yet, off the caller's thread. */
    public void ensureAsync(Collection<Integer> ids) {
        List<Integer> copy = ids == null ? List.of() : List.copyOf(ids);
        if (todo(copy).isEmpty()) {
            return;
        }
        Thread.ofVirtual().name("item-icons").start(() -> {
            try {
                ensure(copy);
            } catch (RuntimeException e) {
                System.out.println("[ItemIcon] extraction failed: " + e);
            }
        });
    }

    /**
     * Extracts the icons of {@code ids} that are not stored yet, in one process run (at most {@link #MAX_BATCH}); waits
     * up to {@link #LOCK_WAIT_SEC} for a run in progress. Returns how many were written.
     */
    public int ensure(Collection<Integer> ids) {
        if (ids == null || todo(ids).isEmpty() || !available()) {
            return 0;
        }
        try {
            if (!rendering.tryLock(LOCK_WAIT_SEC, TimeUnit.SECONDS)) {
                return 0;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return 0;
        }
        try {
            List<Integer> todo = todo(ids);   // another run may have written some of them meanwhile
            if (todo.isEmpty()) {
                return 0;
            }
            run(todo);
            int written = 0;
            List<Integer> none = new ArrayList<>();
            long now = System.currentTimeMillis();
            for (int id : todo) {
                if (cached(id) != null) {
                    written++;
                } else {
                    missing.put(id, now);
                    none.add(id);
                }
            }
            System.out.println("[ItemIcon] extracted " + written + " of " + todo.size() + (none.isEmpty() ? "" : " (no icon: " + none + ")"));
            return written;
        } catch (IOException e) {
            System.out.println("[ItemIcon] extraction failed: " + e.getClass().getSimpleName());
            return 0;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return 0;
        } finally {
            rendering.unlock();
        }
    }

    private List<Integer> todo(Collection<Integer> ids) {
        return new TreeSet<>(ids).stream().filter(i -> i != null && validId(i) && cached(i) == null && !knownMissing(i))
                .limit(MAX_BATCH).collect(Collectors.toList());
    }

    private void run(List<Integer> ids) throws IOException, InterruptedException {
        Files.createDirectories(cache);
        String javaExe = ProcessHandle.current().info().command().orElse("java");
        String cp = dir.resolve("orz") + ";" + lib + "\\*;" + dir.resolve("classes");
        List<String> cmd = List.of(javaExe, "-Xmx256m", "-cp", cp, "ItemIconRender", data.toString(), cache.toString(),
                ids.stream().map(String::valueOf).collect(Collectors.joining(",")));
        Process p = new ProcessBuilder(cmd).redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
        try {
            p.waitFor(TIMEOUT_SEC, TimeUnit.SECONDS);
        } finally {
            if (p.isAlive()) {
                p.destroyForcibly();
            }
        }
    }
}
