package net.swordie.ms.connection.api.launcher.news;

import java.util.List;

/**
 * One origin of launcher notices ({@link LauncherNews} merges them; each fails on its own without hiding the others):
 * the host's news.json ({@link NewsFileSource}), the running events ({@link EventNewsSource}), login.maintenance
 * ({@link MaintenanceNewsSource}), game.slide ({@link SlideNewsSource}).
 */
public interface NewsSource {

    /** Short name for log lines. */
    String name();

    /** The notices shown at {@code now} (epoch milliseconds). */
    List<NewsItem> items(long now);
}
