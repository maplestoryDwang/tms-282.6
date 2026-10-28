package net.swordie.ms.connection.api.launcher.news;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * The launcher's notices (composite of {@link NewsSource}s): the host's news.json ({@link NewsFileSource}) plus the
 * generated ones - running events, maintenance, game.slide. A failing source is skipped (one log line) without hiding
 * the others. Order: pinned first, then maintenance, then newest {@code startAt} (none = oldest), then id. The
 * {@link NewsSnapshot#version()} hashes exactly the returned list, so the dashboard can tell the launcher whether to
 * reload the news tab.
 */
public final class LauncherNews {
    static final Comparator<NewsItem> ORDER = Comparator.comparing((NewsItem i) -> !i.pinned())
            .thenComparing(i -> !"mt".equals(i.kind()))
            .thenComparing((NewsItem i) -> i.startAt() == null ? Long.MIN_VALUE : i.startAt(), Comparator.reverseOrder())
            .thenComparing(NewsItem::id);
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final LauncherNews STANDARD = new LauncherNews(new NewsFileSource(NewsFileSource.DEFAULT_FILE),
            List.of(new EventNewsSource(EventNewsSource.DEFAULT_FILE), new MaintenanceNewsSource(), new SlideNewsSource()),
            System::currentTimeMillis);

    private final NewsFileSource file;
    private final List<NewsSource> generated;
    private final LongSupplier clock;
    private final Set<String> reportedFailures = ConcurrentHashMap.newKeySet();

    public LauncherNews(NewsFileSource file, List<NewsSource> generated, LongSupplier clock) {
        this.file = Objects.requireNonNull(file, "file");
        this.generated = List.copyOf(generated);
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /** The working directory's data/launcher/news.json and events, the live settings; shared by the process. */
    public static LauncherNews standard() {
        return STANDARD;
    }

    public NewsSnapshot snapshot() {
        long now = clock.getAsLong();
        List<NewsItem> items = new ArrayList<>(file.items(now));
        for (NewsSource source : generated) {
            try {
                items.addAll(source.items(now));
                reportedFailures.remove(source.name());
            } catch (RuntimeException e) {
                if (reportedFailures.add(source.name())) {
                    System.out.println("[LauncherNews] source " + source.name() + " failed: " + e.getClass().getSimpleName());
                }
            }
        }
        items.sort(ORDER);
        NewsSnapshot unversioned = new NewsSnapshot(items, "", file.status());
        return new NewsSnapshot(items, version(unversioned), file.status());
    }

    /** First 16 hex digits of the SHA-256 of the items as JSON (wire order). */
    static String version(NewsSnapshot s) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(JSON.writeValueAsBytes(s.itemMaps()));
            return HexFormat.of().formatHex(h, 0, 8);
        } catch (NoSuchAlgorithmException | JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    public NewsFileSource file() {
        return file;
    }
}
