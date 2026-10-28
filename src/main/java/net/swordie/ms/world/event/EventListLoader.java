// EventListLoader.java
package net.swordie.ms.world.event;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public final class EventListLoader {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static List<EventListData> loadFromFile(Path path) {
        try (InputStream in = Files.newInputStream(path)) {
            List<EventListData> list = MAPPER.readValue(in, new TypeReference<>() {});
            return list == null ? Collections.emptyList() : list;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load events.json: " + path, e);
        }
    }

    /**
     * 優先讀取繁中版 {@code twPath}（存在時），失敗或不存在則退回 {@code fallback}。
     */
    public static List<EventListData> loadPreferTw(Path twPath, Path fallback) {
        if (twPath != null && Files.isRegularFile(twPath)) {
            try {
                List<EventListData> list = loadFromFile(twPath);
                System.out.printf("[EventList] 已載入繁中活動清單 %s (%d 筆)%n", twPath, list.size());
                return list;
            } catch (RuntimeException e) {
                System.err.println("[EventList] 繁中活動清單讀取失敗，改用預設檔: " + e.getMessage());
            }
        }
        return loadFromFile(fallback);
    }
}
