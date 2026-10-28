package net.swordie.ms.connection.api.launcher.news;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The notices of one moment, merged and ordered ({@link LauncherNews#snapshot()}).
 *
 * @param version    first 16 hex digits of the SHA-256 of exactly the returned items (changes when an item is
 *                   edited, added, expires or starts)
 * @param fileStatus news.json: {@code ok}, {@code missing} or {@code invalid} (then the previous valid version is
 *                   in {@code items})
 */
public record NewsSnapshot(List<NewsItem> items, String version, String fileStatus) {

    public NewsSnapshot {
        items = List.copyOf(items);
    }

    public List<Map<String, Object>> itemMaps() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (NewsItem i : items) {
            out.add(i.toMap());
        }
        return out;
    }
}
