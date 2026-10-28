package net.swordie.ms.connection.api.launcher.news;

import net.swordie.ms.GameSettings;

import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * {@code login.maintenance} (config/server.properties, hot reloaded by {@link GameSettings}): while on, one pinned
 * notice {@code {id:"maintenance", kind:"mt"}} whose body is {@code login.maintenance.message}.
 */
public final class MaintenanceNewsSource implements NewsSource {
    public static final String ID = "maintenance";
    public static final String TITLE = "伺服器維護中";

    private final BooleanSupplier on;
    private final Supplier<String> message;

    /** The live settings. */
    public MaintenanceNewsSource() {
        this(GameSettings::maintenance, GameSettings::maintenanceMessage);
    }

    public MaintenanceNewsSource(BooleanSupplier on, Supplier<String> message) {
        this.on = Objects.requireNonNull(on, "on");
        this.message = Objects.requireNonNull(message, "message");
    }

    @Override
    public String name() {
        return "maintenance";
    }

    @Override
    public List<NewsItem> items(long now) {
        if (!on.getAsBoolean()) {
            return List.of();
        }
        return List.of(new NewsItem(ID, "mt", TITLE, message.get(), null, null, true));
    }
}
