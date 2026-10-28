package net.swordie.ms.connection.api.launcher.release;

import com.fasterxml.jackson.databind.JsonNode;
import net.swordie.ms.connection.api.launcher.LauncherCodes;
import net.swordie.ms.connection.api.launcher.LauncherContext;
import net.swordie.ms.connection.api.launcher.LauncherResult;
import net.swordie.ms.connection.api.launcher.ReadAction;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * {@code manifestMeta} (public, P8): {@code referenceSha256}, {@code exeSha256} and {@code count} of the host's client
 * files ({@link ClientManifestMeta}), so a launcher can tell its built-in client-manifest.json is outdated. Either
 * file unreadable -> {@code unavailable}. No paths in the answer.
 */
public final class ManifestMetaAction extends ReadAction {
    private final Supplier<ClientManifestMeta> meta;

    public ManifestMetaAction() {
        this(ClientManifestMeta::standard);
    }

    public ManifestMetaAction(Supplier<ClientManifestMeta> meta) {
        this.meta = Objects.requireNonNull(meta, "meta");
    }

    @Override
    public String name() {
        return "manifestMeta";
    }

    @Override
    public LauncherResult execute(LauncherContext ctx, JsonNode request) {
        Optional<ClientManifestMeta.Meta> m = meta.get().read(null);
        if (m.isEmpty()) {
            return LauncherResult.fail(LauncherCodes.UNAVAILABLE);
        }
        return LauncherResult.ok(LauncherCodes.OK).with("referenceSha256", m.get().referenceSha256())
                .with("exeSha256", m.get().exeSha256()).with("count", m.get().count());
    }
}
