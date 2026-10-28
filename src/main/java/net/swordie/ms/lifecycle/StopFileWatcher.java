package net.swordie.ms.lifecycle;

import net.swordie.ms.util.HotProperties;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.IntConsumer;

/**
 * Local graceful-stop entry: a file named by {@code server.stopFile} (default {@value #DEFAULT_FILE}, relative to the
 * server's working directory, i.e. {@code D:\TMS\server\265\shutdown.request}). Only something that can write into
 * the server folder on this machine can create it - nothing is listening on the network for it.
 * <p>
 * Checked every {@value #POLL_MS} ms. When it appears it is deleted, its first line (at most 200 characters, for the
 * log) is printed, {@link ServerShutdown} saves and disconnects everyone, and the JVM exits with 0. A file that is
 * already there when the server starts is a leftover: it is deleted and ignored (logged), so a stale request never
 * stops a freshly started server. Deploy.ps1 writes the file and waits for the process to exit by itself.
 * <p>
 * 維修倒數 (2026-10-05): a first line {@code countdown=<seconds>} (1..3600) starts {@link MaintenanceCountdown} and the
 * file stays while it runs; deleting it cancels the countdown (announced); at zero the file is removed and the usual
 * graceful stop runs. An invalid countdown line ({@code countdown=abc}, 0, more than 3600) is deleted and ignored -
 * it never stops the server at once. Any other content still stops at once (Deploy.ps1 / the admin API without a
 * countdown).
 */
public final class StopFileWatcher {
    public static final String KEY = "server.stopFile";
    public static final String DEFAULT_FILE = "shutdown.request";
    public static final long POLL_MS = 1_000;

    private final HotProperties settings;
    private final ServerShutdown shutdown;
    private final IntConsumer exit;
    private final MaintenanceCountdown countdown;
    private volatile boolean stopped;
    private static volatile StopFileWatcher running;

    StopFileWatcher(HotProperties settings, ServerShutdown shutdown, IntConsumer exit) {
        this(settings, shutdown, exit, MaintenanceCountdown.standard());
    }

    StopFileWatcher(HotProperties settings, ServerShutdown shutdown, IntConsumer exit, MaintenanceCountdown countdown) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.shutdown = Objects.requireNonNull(shutdown, "shutdown");
        this.exit = Objects.requireNonNull(exit, "exit");
        this.countdown = Objects.requireNonNull(countdown, "countdown");
    }

    /** The server's watcher: config/server.properties, the standard shutdown, System.exit. */
    public static synchronized StopFileWatcher start() {
        if (running != null) {
            return running;
        }
        StopFileWatcher w = new StopFileWatcher(HotProperties.server(), ServerShutdown.get(), System::exit);
        w.discardLeftover();
        Thread t = new Thread(w::loop, "StopFileWatcher");
        t.setDaemon(true);
        t.start();
        running = w;
        System.out.println("[Shutdown] graceful stop: create " + w.file().toAbsolutePath().normalize()
                + " (" + KEY + ") to save everyone and exit");
        return w;
    }

    public Path file() {
        return Path.of(settings.string(KEY, DEFAULT_FILE));
    }

    /** Deletes a request left over from before this start. */
    void discardLeftover() {
        Path f = file();
        if (Files.exists(f, LinkOption.NOFOLLOW_LINKS)) {
            String note = firstLine(f);
            try {
                Files.deleteIfExists(f);
            } catch (IOException e) {
                System.out.println("[Shutdown] cannot delete the leftover stop request " + f + ": " + e.getMessage());
                return;
            }
            System.out.println("[Shutdown] ignored a stop request left from before this start (" + f + ": " + note + ")");
        }
    }

    /**
     * One check; true when a request was found and handled (the JVM normally exits inside, through {@code exit}).
     */
    boolean poll() {
        Path f = file();
        if (!Files.exists(f, LinkOption.NOFOLLOW_LINKS)) {
            if (countdown.active()) {
                countdown.cancel("stop file " + f.getFileName() + " removed");
            }
            return false;
        }
        String note = firstLine(f);
        Integer seconds = MaintenanceCountdown.parse(note);
        if (seconds != null && seconds < 0) {
            try {
                Files.deleteIfExists(f);
            } catch (IOException e) {
                System.out.println("[Shutdown] cannot delete the invalid countdown request " + f + ": " + e.getMessage());
            }
            System.out.println("[Shutdown] ignored an invalid countdown request (" + note + "): expected countdown=<1.."
                    + MaintenanceCountdown.MAX_SEC + "> seconds; the server keeps running");
            return false;
        }
        if (seconds != null) {
            if (!countdown.active()) {
                countdown.start(seconds, "stop file " + f.getFileName() + ": " + note);
                return false;
            }
            if (countdown.tick() != MaintenanceCountdown.Tick.DUE) {
                return false;
            }
            note = note + " (countdown finished)";
        }
        try {
            Files.deleteIfExists(f);
        } catch (IOException e) {
            System.out.println("[Shutdown] cannot delete the stop request " + f + ": " + e.getMessage() + " (stopping anyway)");
        }
        System.out.println("[Shutdown] stop request " + f.toAbsolutePath().normalize() + " found: " + note);
        stopped = true;
        boolean saved = shutdown.run("stop file " + f.getFileName());
        System.out.println("[Shutdown] exiting (saves finished: " + saved + ")");
        System.out.flush();
        exit.accept(0);
        return true;
    }

    private void loop() {
        while (!stopped) {
            try {
                Thread.sleep(POLL_MS);
                poll();
            } catch (InterruptedException e) {
                return;
            } catch (Throwable t) {
                System.out.println("[Shutdown] stop-file check failed: " + t);
            }
        }
    }

    private static String firstLine(Path f) {
        try (InputStream in = Files.newInputStream(f)) {
            byte[] b = in.readNBytes(200);
            String s = new String(b, StandardCharsets.UTF_8).strip();
            int nl = s.indexOf('\n');
            if (nl >= 0) {
                s = s.substring(0, nl).strip();
            }
            s = s.replaceAll("\\p{Cntrl}", "?");
            return s.isEmpty() ? "(empty)" : s;
        } catch (IOException e) {
            return "(unreadable: " + e.getMessage() + ")";
        }
    }
}
