package net.swordie.ms.world.tms282.contents.dimensiontower;

import com.fasterxml.jackson.databind.JsonNode;
import net.swordie.ms.world.tms282.contents.common.AccountLedger;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * One 次元之塔 season from {@code data/tms282-dimensiontower/tower.json} (Taipei time, {@code yyyyMMddHHmm}):
 * the race runs [start, end], ranking rewards can be claimed (end, claimEnd]. Rank tiers are "rank &lt;= upTo".
 */
public record TowerSeason(String id, LocalDateTime start, LocalDateTime end, LocalDateTime claimEnd, int minLevel,
                          int stage1Medal, int stage2Medal, List<Tier> tiers) {
    public static final ZoneId ZONE = ZoneId.of("Asia/Taipei");
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    public record Tier(int upTo, List<AccountLedger.Reward> rewards) {
        public Tier {
            if (upTo < 1) {
                throw new IllegalArgumentException("tier upTo " + upTo);
            }
            rewards = List.copyOf(rewards);
        }
    }

    public TowerSeason {
        Objects.requireNonNull(id, "id");
        if (!id.matches("[a-z0-9_]{1,16}")) {
            throw new IllegalArgumentException("season id " + id);
        }
        if (!end.isAfter(start) || claimEnd.isBefore(end)) {
            throw new IllegalArgumentException("season " + id + " period");
        }
        tiers = List.copyOf(tiers);
        int last = 0;
        for (Tier t : tiers) {
            if (t.upTo() <= last) {
                throw new IllegalArgumentException("tiers must be ascending");
            }
            last = t.upTo();
        }
    }

    public boolean running(LocalDateTime now) {
        return !now.isBefore(start) && !now.isAfter(end);
    }

    public boolean claimable(LocalDateTime now) {
        return now.isAfter(end) && !now.isAfter(claimEnd);
    }

    /** Rewards of a 1-based rank, or null outside every tier. */
    public Tier tierOf(int rank) {
        if (rank < 1) {
            return null;
        }
        for (Tier t : tiers) {
            if (rank <= t.upTo()) {
                return t;
            }
        }
        return null;
    }

    public static LocalDateTime time(String s) {
        return LocalDateTime.parse(s, STAMP);
    }

    public record Config(boolean enabled, List<TowerSeason> seasons) {
        public Config {
            seasons = List.copyOf(seasons);
        }

        public static Config disabled() {
            return new Config(false, List.of());
        }

        /** The season running now, or else the one whose claim window is open, or null. */
        public TowerSeason current(LocalDateTime now) {
            if (!enabled) {
                return null;
            }
            for (TowerSeason s : seasons) {
                if (s.running(now)) {
                    return s;
                }
            }
            for (TowerSeason s : seasons) {
                if (s.claimable(now)) {
                    return s;
                }
            }
            return null;
        }

        public static Config parse(JsonNode root) {
            List<TowerSeason> seasons = new ArrayList<>();
            for (JsonNode s : root.path("seasons")) {
                List<Tier> tiers = new ArrayList<>();
                for (JsonNode t : s.path("rankRewards")) {
                    List<AccountLedger.Reward> rewards = new ArrayList<>();
                    for (JsonNode r : t.path("rewards")) {
                        rewards.add(new AccountLedger.Reward(r.path("item").asInt(), r.path("count").asInt(1)));
                    }
                    tiers.add(new Tier(t.path("upTo").asInt(), rewards));
                }
                seasons.add(new TowerSeason(s.path("id").asText(), time(s.path("start").asText()), time(s.path("end").asText()),
                        time(s.path("claimEnd").asText()), s.path("minLevel").asInt(101), s.path("stage1Medal").asInt(),
                        s.path("stage2Medal").asInt(), tiers));
            }
            return new Config(root.path("enabled").asBoolean(false), seasons);
        }
    }
}
