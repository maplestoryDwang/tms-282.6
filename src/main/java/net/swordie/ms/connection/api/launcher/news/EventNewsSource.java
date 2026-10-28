package net.swordie.ms.connection.api.launcher.news;

import net.swordie.ms.world.event.EventListData;
import net.swordie.ms.world.event.EventListLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The running events of the in-game event list: {@code data/resources/tms282/events/events.tw.json}, read with
 * {@link EventListLoader#loadFromFile} (not through {@code EventConstants}, whose static initialiser loads game data).
 * Re-read when the file's modification time or size changed (checked at most every {@value #CHECK_MS} ms; a file that
 * fails to load keeps the previous list). An event is shown while {@code fromDate <= now <= toDate}
 * ({@code yyyyMMddHHmm}, server time zone; a start before 2000 is sent as null = always): {@code kind:"ev"},
 * {@code id:"event-<n>"} (n = position in the file, from 1), title = name (line breaks as spaces), body = desc.
 * <p>
 * event-fixes-1005: the 265-generated file carried {@code toDate 202612312359} on every entry, so the launcher kept
 * advertising events whose 282 client quests had long ended. Now
 * <ul>
 *     <li>{@code "launcherNews": false} hides an entry (facilities that are not events);</li>
 *     <li>the end is capped by the event's 282 client quest ({@code questId}, else a numeric {@code scriptName} of a
 *     quest-type entry, else {@code q<id>s} in {@code scriptName} / {@code uiName}): Quest.wz Check/0 {@code end},
 *     through {@link QuestEnd} (the server's QuestData); an unknown quest or no end leaves {@code toDate}.</li>
 * </ul>
 */
public final class EventNewsSource implements NewsSource {
    public static final Path DEFAULT_FILE = Path.of("data", "resources", "tms282", "events", "events.tw.json");
    static final long CHECK_MS = 5_000;
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("uuuuMMddHHmm");
    private static final long YEAR_2000 = 946_684_800_000L;

    /** An event with its parsed period. */
    private record Event(NewsItem item, long from, long to) {
    }

    /** End of a 282 client quest as {@code yyyyMMddHHmm} (Quest.wz Check/0 end; padded like Tms282QuestFlow), or 0. */
    @FunctionalInterface
    public interface QuestEnd {
        long of(int questId);

        /** No cap (tests, or when the quest data is unavailable). */
        QuestEnd NONE = questId -> 0;

        /** The server's quest data (QuestData.getQuestInfoById, lazily loaded per quest); failures mean no cap. */
        static QuestEnd questData() {
            return questId -> {
                try {
                    net.swordie.ms.loaders.containerclasses.QuestInfo qi = net.swordie.ms.loaders.QuestData.getQuestInfoById(questId);
                    return qi == null ? 0 : qi.getEnd();
                } catch (RuntimeException | LinkageError e) {
                    return 0;
                }
            };
        }
    }

    private static final java.util.regex.Pattern QUEST_SCRIPT = java.util.regex.Pattern.compile("q(\\d{1,9})s");
    private static final java.util.regex.Pattern QUEST_NUMBER = java.util.regex.Pattern.compile("\\d{1,9}");

    private final Path file;
    private final ZoneId zone;
    private final QuestEnd questEnd;
    private long lastCheck = Long.MIN_VALUE;
    private String lastKey;
    private List<Event> events = List.of();

    public EventNewsSource(Path file) {
        this(file, ZoneId.systemDefault(), QuestEnd.questData());
    }

    public EventNewsSource(Path file, ZoneId zone) {
        this(file, zone, QuestEnd.NONE);
    }

    public EventNewsSource(Path file, ZoneId zone, QuestEnd questEnd) {
        this.file = Objects.requireNonNull(file, "file");
        this.zone = Objects.requireNonNull(zone, "zone");
        this.questEnd = Objects.requireNonNull(questEnd, "questEnd");
    }

    @Override
    public String name() {
        return "events";
    }

    @Override
    public synchronized List<NewsItem> items(long now) {
        refresh(now);
        List<NewsItem> out = new ArrayList<>();
        for (Event e : events) {
            if (now >= e.from() && now <= e.to()) {
                out.add(e.item());
            }
        }
        return out;
    }

    private void refresh(long now) {
        if (lastCheck != Long.MIN_VALUE && now - lastCheck < CHECK_MS && now >= lastCheck) {
            return;
        }
        lastCheck = now;
        String key;
        try {
            BasicFileAttributes a = Files.readAttributes(file, BasicFileAttributes.class);
            key = a.lastModifiedTime().toMillis() + ":" + a.size();
        } catch (IOException e) {
            key = "missing";
        }
        if (key.equals(lastKey)) {
            return;
        }
        lastKey = key;
        if (key.equals("missing")) {
            events = List.of();
            return;
        }
        try {
            events = convert(EventListLoader.loadFromFile(file));
        } catch (RuntimeException e) {
            System.out.println("[LauncherNews] " + file + " could not be read (" + e.getClass().getSimpleName()
                    + "): keeping the previous " + events.size() + " events");
        }
    }

    private List<Event> convert(List<EventListData> list) {
        List<Event> out = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            EventListData d = list.get(i);
            if (d == null || d.name == null || !d.showInLauncherNews()) {
                continue;
            }
            Long from = stamp(d.fromDate, false);
            Long to = stamp(d.toDate, true);
            if (from == null || to == null) {
                continue;
            }
            int quest = questOf(d);
            long questStamp = quest > 0 ? pad(questEnd.of(quest)) : 0;
            Long questTo = questStamp > 0 ? stamp(String.valueOf(questStamp), true) : null;
            if (questTo != null && questTo < to) {
                to = questTo; // the 282 client quest ended earlier than the file says
            }
            String title = d.name.replace("\r\n", " ").replace('\n', ' ').replace('\r', ' ').strip();
            if (title.isEmpty()) {
                continue;
            }
            NewsItem item = new NewsItem("event-" + (i + 1), "ev", title, d.desc, from < YEAR_2000 ? null : from, to, false);
            out.add(new Event(item, from, to));
        }
        return List.copyOf(out);
    }

    /** The 282 client quest of an entry, 0 when none. */
    static int questOf(EventListData d) {
        if (d.questId > 0) {
            return d.questId;
        }
        if (d.scriptType == 1 && d.scriptName != null && QUEST_NUMBER.matcher(d.scriptName.strip()).matches()) {
            return Integer.parseInt(d.scriptName.strip());
        }
        for (String s : new String[]{d.scriptName, d.uiName}) {
            if (s != null) {
                java.util.regex.Matcher m = QUEST_SCRIPT.matcher(s.strip());
                if (m.matches()) {
                    return Integer.parseInt(m.group(1));
                }
            }
        }
        return 0;
    }

    /** yyyyMMdd / yyyyMMddHH -> yyyyMMddHHmm (Tms282QuestFlow.stamp); 0 stays 0. */
    static long pad(long v) {
        if (v <= 0) {
            return 0;
        }
        long s = v;
        while (s < 100_000_000_000L) {
            s *= 100;
        }
        return s;
    }

    /** {@code yyyyMMddHHmm} in the zone; an end covers its whole minute. Null when malformed. */
    private Long stamp(String s, boolean end) {
        if (s == null) {
            return null;
        }
        try {
            long ms = LocalDateTime.parse(s.strip(), STAMP).atZone(zone).toInstant().toEpochMilli();
            return end ? ms + 59_999 : ms;
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
