package net.swordie.ms.console.admin;

import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import java.awt.Font;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * The stand-alone admin console (2026-10-03): a Swing window that talks to the running headless server only through
 * its admin API (127.0.0.1:8600/api/v2/admin with the token of {@code <server folder>\admin.token}). Opening or
 * closing it never affects the game; it never starts, restarts or kills the server and never writes the database.
 * <pre>
 * javaw --enable-preview -cp maplestory-282.3-jar-with-dependencies.jar net.swordie.ms.console.admin.AdminConsoleMain
 *       [--home D:\TMS\server\265] [--port 8600]
 *       [--screenshots &lt;dir&gt; [--live]]   (render every tab to PNG offscreen and exit; fake data unless --live)
 * </pre>
 * Without {@code --port} the port is {@code api.http.port} of {@code <home>\config\server.properties} (read only),
 * default 8600. This class must never load net.swordie.ms.Server (its static initialiser builds the whole server).
 */
public final class AdminConsoleMain {
    private AdminConsoleMain() {
    }

    public static void main(String[] args) throws Exception {
        Path home = Path.of("").toAbsolutePath();
        Integer port = null;
        Path shots = null;
        boolean live = false;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--home" -> home = Path.of(args[++i]).toAbsolutePath();
                case "--port" -> port = Integer.parseInt(args[++i]);
                case "--screenshots" -> shots = Path.of(args[++i]).toAbsolutePath();
                case "--live" -> live = true;
                default -> throw new IllegalArgumentException("unknown option " + args[i]);
            }
        }
        int p = port != null ? port : configuredPort(home);
        System.setProperty(DeployTab.HOME_KEY, home.toString());
        lookAndFeel();
        if (shots != null) {
            AdminGateway gateway = live ? new HttpAdminGateway(p, home.resolve("admin.token")) : new FakeAdminGateway(false);
            int problems = AdminScreenshots.render(gateway, live, shots);
            System.exit(problems == 0 ? 0 : 2);
            return;
        }
        HttpAdminGateway gateway = new HttpAdminGateway(p, home.resolve("admin.token"));
        SwingUtilities.invokeLater(() -> new AdminConsoleFrame(gateway, false, SaveShutdownTab::processAlive).showWindow());
    }

    static void lookAndFeel() {
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception | LinkageError e) {
            // the default look and feel still works
        }
        UIManager.put("defaultFont", new Font(AdminUi.FONT, Font.PLAIN, 13));
    }

    /** api.http.port of the server's config (the formal server leaves it unset: 8600). */
    static int configuredPort(Path home) {
        Path cfg = home.resolve("config").resolve("server.properties");
        Properties props = new Properties();
        if (Files.isRegularFile(cfg)) {
            try (InputStream in = Files.newInputStream(cfg)) {
                props.load(in);
            } catch (IOException | IllegalArgumentException e) {
                // fall back to 8600
            }
        }
        try {
            int n = Integer.parseInt(props.getProperty("api.http.port", "8600").trim());
            return n >= 1 && n <= 65535 ? n : 8600;
        } catch (NumberFormatException e) {
            return 8600;
        }
    }
}
