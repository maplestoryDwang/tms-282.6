package net.swordie.ms.world.tms282.contents.mushroompass;

import com.fasterxml.jackson.databind.JsonNode;
import net.swordie.ms.world.tms282.contents.common.AccountLedger;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * One season of the 282 pass window {@code CUIHyperMushPass} (LP065D entry), read from
 * {@code data/tms282-mushroompass/season.json}. Times are Taipei time ({@code yyyyMMddHHmm}).
 * <ul>
 *     <li>{@code passQuest}: QR-ex "pass" / "hyper" = 1 unlocks the premium / hyper track (this server sells
 *     neither, so the server never writes it);</li>
 *     <li>{@code claimQuest}: QR-ex "b&lt;idx&gt;" / "p&lt;idx&gt;" = 1 marks a claimed basic / premium reward;</li>
 *     <li>{@code unknown}: the int the client stores at season+0x10 (meaning not found; 0);</li>
 *     <li>{@code levels}: index = page × 4 + slot; each has the required level, the basic rewards, the premium track
 *     selector (0 premium, 1 hyper) and the premium rewards (0..4 items each: the window has 1EA..4EA layouts and
 *     shows nothing for 0).</li>
 * </ul>
 */
public record MushroomPassSeason(String id, LocalDateTime start, LocalDateTime end, int passQuest, int unknown,
                                 int claimQuest, List<Level> levels) {
    public static final ZoneId ZONE = ZoneId.of("Asia/Taipei");
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
    public static final int MAX_REWARDS = 4;

    public record Level(int level, List<AccountLedger.Reward> basic, int tier, List<AccountLedger.Reward> premium) {
        public Level {
            if (level < 1 || level > 300) {
                throw new IllegalArgumentException("level " + level);
            }
            if (tier != 0 && tier != 1) {
                throw new IllegalArgumentException("tier " + tier);
            }
            basic = List.copyOf(basic);
            premium = List.copyOf(premium);
            if (basic.size() > MAX_REWARDS || premium.size() > MAX_REWARDS) {
                throw new IllegalArgumentException("at most " + MAX_REWARDS + " rewards per track");
            }
        }
    }

    public MushroomPassSeason {
        Objects.requireNonNull(id, "id");
        if (!id.matches("[a-z0-9_]{1,24}")) {
            throw new IllegalArgumentException("season id " + id);
        }
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("season " + id + " ends before it starts");
        }
        if (passQuest <= 0 || claimQuest <= 0 || passQuest == claimQuest) {
            throw new IllegalArgumentException("season " + id + " quests");
        }
        levels = List.copyOf(levels);
        if (levels.isEmpty() || levels.size() > 64) {
            throw new IllegalArgumentException("season " + id + " has " + levels.size() + " levels");
        }
    }

    public boolean activeAt(LocalDateTime now) {
        return !now.isBefore(start) && !now.isAfter(end);
    }

    /** FILETIME (100 ns since 1601) of a Taipei wall-clock time, as the client compares it with "now". */
    public static long fileTime(LocalDateTime t) {
        return t.atZone(ZONE).toInstant().toEpochMilli() * 10_000L + 116_444_736_000_000_000L;
    }

    public static LocalDateTime time(String s) {
        return LocalDateTime.parse(s, STAMP);
    }

    // --------------------------------------------------------------------------------------------- JSON

    /** {@code {"enabled":true,"seasons":[...]}} -> config. */
    public record Config(boolean enabled, List<MushroomPassSeason> seasons) {
        public Config {
            seasons = List.copyOf(seasons);
        }

        public static Config disabled() {
            return new Config(false, List.of());
        }

        /** The season running at {@code now}, or null. */
        public MushroomPassSeason active(LocalDateTime now) {
            if (!enabled) {
                return null;
            }
            for (MushroomPassSeason s : seasons) {
                if (s.activeAt(now)) {
                    return s;
                }
            }
            return null;
        }

        public static Config parse(JsonNode root) {
            boolean enabled = root.path("enabled").asBoolean(false);
            List<MushroomPassSeason> seasons = new ArrayList<>();
            for (JsonNode s : root.path("seasons")) {
                List<Level> levels = new ArrayList<>();
                for (JsonNode l : s.path("levels")) {
                    levels.add(new Level(l.path("level").asInt(), rewards(l.path("basic")), l.path("tier").asInt(0),
                            rewards(l.path("premium"))));
                }
                seasons.add(new MushroomPassSeason(s.path("id").asText(), time(s.path("start").asText()),
                        time(s.path("end").asText()), s.path("passQuest").asInt(), s.path("unknown").asInt(0),
                        s.path("claimQuest").asInt(), levels));
            }
            return new Config(enabled, seasons);
        }

        private static List<AccountLedger.Reward> rewards(JsonNode array) {
            List<AccountLedger.Reward> out = new ArrayList<>();
            for (JsonNode r : array) {
                out.add(new AccountLedger.Reward(r.path("item").asInt(), r.path("count").asInt(1)));
            }
            return out;
        }
    }
}
