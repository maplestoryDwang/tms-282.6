package net.swordie.ms.world.tms282.contents.academy;

import com.fasterxml.jackson.databind.JsonNode;
import net.swordie.ms.world.tms282.contents.common.AccountLedger;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 楓之谷學院 rules and rewards from {@code data/tms282-academy/academy.json} (Taipei time {@code yyyyMMddHHmm}).
 * Official values (beanfun eventAdId=19029): Lv101, account-wide; sign-in once a day, 5 a week (week resets
 * Thursday 00:00), 30 in total, each sign-in = 學分 + 約會券 plus the day's reward (days 1..20); basic course =
 * 1000 level-range kills, once a day, 5 a week; advanced course = 10000 level-range kills each, 5 a week; credit
 * milestones every 5 credits up to 100. Items the server cannot use are replaced in the JSON (see its notes).
 */
public record AcademyConfig(boolean enabled, LocalDateTime start, LocalDateTime end, int minLevel, int levelRange,
                            int creditItem, List<AccountLedger.Reward> perSignIn, int signInMax, int signInPerWeek,
                            List<List<AccountLedger.Reward>> signInRewards, int basicKills, int basicPerWeek,
                            int advancedKills, int advancedPerWeek, List<Milestone> milestones) {
    public static final ZoneId ZONE = ZoneId.of("Asia/Taipei");
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    public record Milestone(int credits, List<AccountLedger.Reward> rewards) {
        public Milestone {
            if (credits < 1) {
                throw new IllegalArgumentException("milestone " + credits);
            }
            rewards = List.copyOf(rewards);
        }
    }

    public AcademyConfig {
        perSignIn = List.copyOf(perSignIn);
        signInRewards = signInRewards.stream().map(List::copyOf).toList();
        milestones = List.copyOf(milestones);
        if (enabled && (start == null || end == null || !end.isAfter(start))) {
            throw new IllegalArgumentException("academy period");
        }
        if (signInMax < 0 || signInPerWeek < 0 || basicKills < 1 || advancedKills < 1 || basicPerWeek < 0
                || advancedPerWeek < 0 || levelRange < 0) {
            throw new IllegalArgumentException("academy limits");
        }
        int last = 0;
        for (Milestone m : milestones) {
            if (m.credits() <= last) {
                throw new IllegalArgumentException("milestones must be ascending");
            }
            last = m.credits();
        }
    }

    public static AcademyConfig disabled() {
        return new AcademyConfig(false, null, null, 101, 20, 4310398, List.of(), 0, 0, List.of(), 1000, 0, 10000, 0, List.of());
    }

    public boolean running(LocalDateTime now) {
        return enabled && !now.isBefore(start) && !now.isAfter(end);
    }

    /** Rewards of the n-th sign-in (1-based): the per-sign-in items plus the day's reward. */
    public List<AccountLedger.Reward> signInRewardsFor(int n) {
        List<AccountLedger.Reward> out = new ArrayList<>(perSignIn);
        if (n >= 1 && n <= signInRewards.size()) {
            out.addAll(signInRewards.get(n - 1));
        }
        return out;
    }

    public Milestone milestone(int credits) {
        for (Milestone m : milestones) {
            if (m.credits() == credits) {
                return m;
            }
        }
        return null;
    }

    static LocalDateTime time(String s) {
        return LocalDateTime.parse(s, STAMP);
    }

    static List<AccountLedger.Reward> rewards(JsonNode array) {
        List<AccountLedger.Reward> out = new ArrayList<>();
        for (JsonNode r : array) {
            out.add(new AccountLedger.Reward(r.path("item").asInt(), r.path("count").asInt(1)));
        }
        return out;
    }

    public static AcademyConfig parse(JsonNode root) {
        List<List<AccountLedger.Reward>> days = new ArrayList<>();
        for (JsonNode d : root.path("signInRewards")) {
            days.add(rewards(d.path("rewards")));
        }
        List<Milestone> ms = new ArrayList<>();
        for (JsonNode m : root.path("milestones")) {
            ms.add(new Milestone(m.path("credits").asInt(), rewards(m.path("rewards"))));
        }
        return new AcademyConfig(root.path("enabled").asBoolean(false), time(root.path("start").asText()),
                time(root.path("end").asText()), root.path("minLevel").asInt(101), root.path("levelRange").asInt(20),
                root.path("creditItem").asInt(4310398), rewards(root.path("perSignIn")), root.path("signInMax").asInt(30),
                root.path("signInPerWeek").asInt(5), days, root.path("basicKills").asInt(1000),
                root.path("basicPerWeek").asInt(5), root.path("advancedKills").asInt(10000),
                root.path("advancedPerWeek").asInt(5), ms);
    }
}
