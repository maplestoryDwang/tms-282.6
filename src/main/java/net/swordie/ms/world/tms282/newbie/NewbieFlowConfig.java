package net.swordie.ms.world.tms282.newbie;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.swordie.ms.world.tms282.newbie.steps.SpawnStep;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * One valid version of {@code data/newbie/newbie-flow.json} (immutable). Top level: {@code version},
 * {@code enabled}, {@code lobbyMap} / {@code lobbyPortal} (the 新手大廳), {@code speakerNpc} (the NPC shown in the
 * dialog), {@code popupDelayMs} (after the field entry), {@code resumeOnLogin} (an unfinished flow also opens at login
 * outside the lobby) and {@code steps}: the ordered step list, each {@code {"id", "type", ...}} built by the
 * {@link NewbieSteps} registry. A file that does not parse or validate is rejected as a whole
 * ({@link InvalidConfig} names the first offending field).
 */
public record NewbieFlowConfig(String version, boolean enabled, int lobbyMap, int lobbyPortal, int speakerNpc,
                               int popupDelayMs, boolean resumeOnLogin, List<Integer> recoveryMaps, List<NewbieStep> steps) {
    public static final int MAX_FILE_BYTES = 256 * 1024;
    private static final ObjectMapper JSON = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);

    public NewbieFlowConfig {
        recoveryMaps = List.copyOf(recoveryMaps);
        steps = List.copyOf(steps);
    }

    /** A configuration file that cannot be used; the message names the first problem (never the whole content). */
    public static final class InvalidConfig extends RuntimeException {
        public InvalidConfig(String message) {
            super(message);
        }
    }

    public static NewbieFlowConfig parse(byte[] bytes) {
        if (bytes == null) throw new InvalidConfig("file: missing");
        if (bytes.length > MAX_FILE_BYTES) throw new InvalidConfig("file: larger than " + MAX_FILE_BYTES + " bytes");
        int start = bytes.length >= 3 && (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB && (bytes[2] & 0xFF) == 0xBF ? 3 : 0;
        JsonNode root;
        try {
            root = JSON.readTree(new String(bytes, start, bytes.length - start, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new InvalidConfig("file: not valid JSON (line " + lineOf(e) + ")");
        }
        if (root == null || !root.isObject()) throw new InvalidConfig("file: must be a JSON object");
        NewbieJson r = new NewbieJson(root, "");
        String version = r.word("version", 40);
        boolean enabled = r.bool("enabled", true);
        int lobby = r.integer("lobbyMap", 1, 999_999_998);
        int portal = r.integer("lobbyPortal", 0, 255, 0);
        int npc = r.integer("speakerNpc", 1, 99_999_999);
        int delay = r.integer("popupDelayMs", 0, 30_000, 2500);
        boolean resume = r.bool("resumeOnLogin", true);
        List<NewbieJson> stepNodes = r.array("steps", 1, 32);
        Set<String> ids = new HashSet<>();
        java.util.ArrayList<NewbieStep> steps = new java.util.ArrayList<>();
        int spawns = 0;
        for (NewbieJson s : stepNodes) {
            NewbieStep step = NewbieSteps.standard().build(s);
            if (!ids.add(step.id())) throw NewbieJson.invalid(s.path + ".id", "duplicate step id " + step.id());
            if (step instanceof SpawnStep) spawns++;
            steps.add(step);
        }
        if (spawns > 1) throw new InvalidConfig("steps: at most one Spawn step");
        boolean giftSeen = false;
        for (int i = 0; i < steps.size(); i++) {
            NewbieStep step = steps.get(i);
            if (step instanceof net.swordie.ms.world.tms282.newbie.steps.WarpMenuStep && i != steps.size() - 1)
                throw new InvalidConfig("steps: WarpMenu must be last (completion waits for arrival)");
            if (step instanceof net.swordie.ms.world.tms282.newbie.steps.StarterGiftStep gift
                    && gift.equipment().keySet().stream().anyMatch(key -> !key.equals("*"))) giftSeen = true;
            if (giftSeen && step instanceof net.swordie.ms.world.tms282.newbie.steps.FirstJobStep)
                throw new InvalidConfig("steps: FirstJob must precede StarterGift (equipment uses the job branch)");
        }
        List<Integer> recovery = r.integers("recoveryMaps", 1, 999_999_998, 32);
        if (recovery.contains(lobby)) throw new InvalidConfig("recoveryMaps: must not contain the lobby");
        return new NewbieFlowConfig(version, enabled, lobby, portal, npc, delay, resume, recovery, steps);
    }

    public NewbieFlowConfig withEnabled(boolean value) {
        return new NewbieFlowConfig(version, value, lobbyMap, lobbyPortal, speakerNpc, popupDelayMs, resumeOnLogin, recoveryMaps, steps);
    }

    private static String lineOf(IOException e) {
        if (e instanceof com.fasterxml.jackson.core.JsonProcessingException j && j.getLocation() != null) {
            return String.valueOf(j.getLocation().getLineNr());
        }
        return "?";
    }

    /** The built-in configuration ({@link NewbieFlowDefaults}). */
    public static NewbieFlowConfig defaults() {
        return parse(NewbieFlowDefaults.JSON.getBytes(StandardCharsets.UTF_8));
    }

    public Optional<SpawnStep> spawn() {
        for (NewbieStep s : steps) {
            if (s instanceof SpawnStep sp) return Optional.of(sp);
        }
        return Optional.empty();
    }

    public String firstStepId() {
        return steps.get(0).id();
    }

    /** Index of the step with {@code id}; 0 for "" or an id this version does not have (steps are idempotent). */
    public int indexOf(String id) {
        if (id == null || id.isEmpty()) return 0;
        for (int i = 0; i < steps.size(); i++) {
            if (steps.get(i).id().equals(id)) return i;
        }
        return 0;
    }

    /** One line for logs and the admin console. */
    public String summary() {
        StringBuilder b = new StringBuilder();
        for (NewbieStep s : steps) {
            if (b.length() > 0) b.append(" -> ");
            b.append(s.id()).append('(').append(s.type()).append(')');
        }
        return "version " + version + (enabled ? "" : " (disabled)") + ", lobby " + lobbyMap + ", steps " + b;
    }
}
