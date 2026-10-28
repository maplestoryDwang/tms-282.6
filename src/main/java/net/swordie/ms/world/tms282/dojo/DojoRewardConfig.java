package net.swordie.ms.world.tms282.dojo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.swordie.ms.ServerConstants;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Dojo weekly reward tiers loaded from {@code data/resources/tms282/dojang/rewards.json}.
 * Rank codes stored in {@code zsf_rank_reward.rank}: overall rank (&lt;100), per-job-line tier code (100..999),
 * participation {@code codeBase + stage} (&gt;1000), matching the MetaMS 273 claim script.
 */
public final class DojoRewardConfig {
    public record RewardItem(int id, int count) {
    }

    public record OverallTier(int from, int to, String label, List<RewardItem> items) {
    }

    public record JobTier(int code, int rank, int minStage, String label, List<RewardItem> items) {
    }

    public record Participation(int codeBase, int minStage, int itemId, int perStage, String label) {
    }

    /** Resolved reward of a stored rank code. */
    public record Reward(String label, String menuText, List<RewardItem> items) {
    }

    private final List<OverallTier> overall;
    private final List<JobTier> job;
    private final Participation participation;

    private DojoRewardConfig(List<OverallTier> overall, List<JobTier> job, Participation participation) {
        this.overall = List.copyOf(overall);
        List<JobTier> sorted = new ArrayList<>(job);
        sorted.sort(Comparator.comparingInt(JobTier::minStage).reversed());
        this.job = List.copyOf(sorted);
        this.participation = participation;
    }

    public static Path path() {
        return Path.of(ServerConstants.RESOURCES_DIR, "tms282", "dojang", "rewards.json");
    }

    public static DojoRewardConfig load() {
        try {
            JsonNode root = new ObjectMapper().readTree(path().toFile());
            List<OverallTier> overall = new ArrayList<>();
            for (JsonNode n : root.path("overall")) {
                overall.add(new OverallTier(n.path("from").asInt(), n.path("to").asInt(), n.path("label").asText(), items(n)));
            }
            List<JobTier> job = new ArrayList<>();
            for (JsonNode n : root.path("job")) {
                int code = n.path("code").asInt();
                if (code < 100 || code >= 1000) throw new IllegalArgumentException("Job tier code must be 100..999: " + code);
                job.add(new JobTier(code, n.path("rank").asInt(1), n.path("minStage").asInt(0), n.path("label").asText(), items(n)));
            }
            JsonNode p = root.path("participation");
            Participation participation = p.isMissingNode() ? null : new Participation(p.path("codeBase").asInt(1000),
                    p.path("minStage").asInt(1), p.path("itemId").asInt(), p.path("perStage").asInt(1), p.path("label").asText());
            return new DojoRewardConfig(overall, job, participation);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load " + path(), e);
        }
    }

    private static List<RewardItem> items(JsonNode tier) {
        List<RewardItem> items = new ArrayList<>();
        for (JsonNode i : tier.path("items")) {
            items.add(new RewardItem(i.path("id").asInt(), i.path("count").asInt(1)));
        }
        return List.copyOf(items);
    }

    /** Rank codes to insert for one ranked record. */
    public List<Integer> codesFor(DojoRankEntry e) {
        List<Integer> codes = new ArrayList<>();
        for (OverallTier t : overall) {
            if (e.rank() >= t.from() && e.rank() <= t.to()) {
                codes.add(e.rank());
                break;
            }
        }
        for (JobTier t : job) {
            if (e.jobRank() == t.rank() && e.record().stage() >= t.minStage()) {
                codes.add(t.code());
                break;
            }
        }
        if (participation != null && e.record().stage() >= participation.minStage()) {
            codes.add(participation.codeBase() + e.record().stage());
        }
        return codes;
    }

    /** Reward of a stored rank code, or null if the code no longer maps to a tier. */
    public Reward reward(int code) {
        if (participation != null && code > participation.codeBase()) {
            int stage = code - participation.codeBase();
            return new Reward(participation.label(), "參與獎: " + stage + "層",
                    List.of(new RewardItem(participation.itemId(), stage * participation.perStage())));
        }
        if (code >= 100) {
            for (JobTier t : job) {
                if (t.code() == code) return new Reward(t.label(), "排名: 本職第" + t.rank(), t.items());
            }
            return null;
        }
        for (OverallTier t : overall) {
            if (code >= t.from() && code <= t.to()) return new Reward(t.label(), "排名: 全職第" + code, t.items());
        }
        return null;
    }
}
