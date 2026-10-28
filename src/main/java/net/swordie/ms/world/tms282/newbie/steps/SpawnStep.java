package net.swordie.ms.world.tms282.newbie.steps;

import net.swordie.ms.world.tms282.newbie.NewbieJson;
import net.swordie.ms.world.tms282.newbie.NewbieRun;
import net.swordie.ms.world.tms282.newbie.NewbieStep;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@code Spawn}: where a new character is created. Applied once, at character creation
 * ({@link net.swordie.ms.world.tms282.newbie.NewbieFlowService#beforeCreateSave}); inside the dialog it has nothing
 * left to do. Settings: {@code default} and {@code byLoginJob} ({@code LoginJob} name, e.g. {@code KINESIS}) with
 * the value {@code "lobby"} (the flow's lobbyMap), {@code "original"} (the job's own creation map, as before) or a map
 * id.
 */
public final class SpawnStep implements NewbieStep {
    public static final String TYPE = "Spawn";
    /** A target: the lobby. */
    public static final int LOBBY = -1;
    /** A target: the job's own creation map. */
    public static final int ORIGINAL = -2;

    private final String id;
    private final int defaultTarget;
    private final Map<String, Integer> byLoginJob;

    SpawnStep(String id, int defaultTarget, Map<String, Integer> byLoginJob) {
        this.id = id;
        this.defaultTarget = defaultTarget;
        this.byLoginJob = Map.copyOf(byLoginJob);
    }

    public static SpawnStep read(String id, NewbieJson s) {
        int def = s.has("default") ? target(new NewbieJson(s.node.get("default"), s.path + ".default")) : LOBBY;
        Map<String, Integer> by = new LinkedHashMap<>();
        for (Map.Entry<String, NewbieJson> e : s.entries("byLoginJob").entrySet()) {
            try { net.swordie.ms.constants.JobConstants.LoginJob.valueOf(e.getKey()); }
            catch (IllegalArgumentException ex) { throw NewbieJson.invalid(e.getValue().path, "unknown LoginJob"); }
            if (!e.getKey().matches("[A-Z_0-9]{1,32}")) throw NewbieJson.invalid(e.getValue().path, "LoginJob name expected (e.g. KINESIS)");
            by.put(e.getKey(), target(e.getValue()));
        }
        return new SpawnStep(id, def, by);
    }

    private static int target(NewbieJson v) {
        if (v.node.isTextual()) {
            return switch (v.node.asText()) {
                case "lobby" -> LOBBY;
                case "original" -> ORIGINAL;
                default -> throw NewbieJson.invalid(v.path, "must be \"lobby\", \"original\" or a map id");
            };
        }
        if (v.node.isIntegralNumber() && v.node.asLong() > 0 && v.node.asLong() < 999_999_999L) {
            return v.node.asInt();
        }
        throw NewbieJson.invalid(v.path, "must be \"lobby\", \"original\" or a map id");
    }

    /** The creation map for {@code loginJob}: the lobby, {@code originalMap}, or a configured map. */
    public int mapFor(String loginJob, int originalMap, int lobbyMap) {
        int t = byLoginJob.getOrDefault(loginJob, defaultTarget);
        return t == LOBBY ? lobbyMap : t == ORIGINAL ? originalMap : t;
    }

    public Map<String, Integer> byLoginJob() {
        return byLoginJob;
    }

    public int defaultTarget() {
        return defaultTarget;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Outcome run(NewbieRun run) {
        return Outcome.NEXT;
    }
}
