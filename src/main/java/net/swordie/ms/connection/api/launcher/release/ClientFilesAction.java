package net.swordie.ms.connection.api.launcher.release;

import com.fasterxml.jackson.databind.JsonNode;
import net.swordie.ms.connection.api.launcher.LauncherCodes;
import net.swordie.ms.connection.api.launcher.LauncherContext;
import net.swordie.ms.connection.api.launcher.LauncherResult;
import net.swordie.ms.connection.api.launcher.ReadAction;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * {@code clientFiles} (public, cat-launcher 1.2.3): the signed client file list the host published with the console
 * ({@link LauncherClientFiles}): {@code version}, {@code files} [{path, size, sha256}] sorted by path, {@code signature}
 * (unchanged; the launcher verifies it with the key in its app.asar before downloading anything). Nothing published or
 * the published set not complete -> {@code unavailable}. No paths of the host, no account data. Separate from
 * launcherVersion because launchers up to 1.2.2 cap that answer at 8 KB (396 files are about 45 KB); launcherVersion
 * only carries the small summary.
 * <p>
 * Path rules 2 (fixes10): a list with a {@code _}-first segment is answered only when the request declares
 * {@code "pathRules":2}; without it (launchers up to 1.2.5, which would refuse the whole signed list) the answer is
 * {@code unavailable} with {@code reason:"pathRules"} - the old launcher shows "nothing published" instead of
 * "invalid list".
 */
public final class ClientFilesAction extends ReadAction {
    private final Supplier<LauncherClientFiles> files;

    public ClientFilesAction() {
        this(LauncherClientFiles::standard);
    }

    public ClientFilesAction(Supplier<LauncherClientFiles> files) {
        this.files = Objects.requireNonNull(files, "files");
    }

    @Override
    public String name() {
        return "clientFiles";
    }

    @Override
    public LauncherResult execute(LauncherContext ctx, JsonNode request) {
        ClientFileSet s = files.get().snapshot().set();
        if (s == null) {
            return LauncherResult.fail(LauncherCodes.UNAVAILABLE);
        }
        if (!ClientFileRules.offerable(s, ClientFileRules.declaredPathRules(request))) {
            return LauncherResult.fail(LauncherCodes.UNAVAILABLE).with("reason", "pathRules");
        }
        return LauncherResult.ok(LauncherCodes.OK).with("version", s.version()).with("files", s.fileMaps()).with("signature", s.signature());
    }
}
