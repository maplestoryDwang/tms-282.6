package net.swordie.ms.world.tms282.contents.changeburning.trial;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 夢幻的試煉 definitions ({@code data/tms282-changeburning/dreamtrial.json}, hot reloaded by {@link DreamTrialService}).
 * Every map / mob id of the trial lives here, not in code: a trial is a list of stages (one fight field each), every
 * stage spawns its mobs with an HP rule and is cleared when all its {@code clearMobs} died.
 * <p>
 * HP rule ({@link HpSpec}): {@code clamp(min, max, (base + perLucidLevel * (lucidLevel - 1)) * hpMultiplier)}. The
 * dream bosses are fought with the forced transformation r = 80004228 (ATK x1, only the Lucid's own ability), so the
 * WZ maxHP (e.g. 夢幻的殘暴炎魔 2,100,000,000) is far beyond what a Lucid can deal; the default values are 自訂
 * estimates to be tuned in game (docs/dreamtrial-1005).
 */
public record DreamTrialConfig(int timeLimitSeconds, int revives, int dailyLimit, int minLucidLevel, double hpMultiplier,
                               int spawnDelayMs, int stageDelayMs, int exitDelaySeconds, boolean hpTag, int townField,
                               List<Trial> trials) {

    public record HpSpec(long base, long perLucidLevel, long min, long max) {
        public long hp(int lucidLevel, double multiplier) {
            int lv = Math.max(1, lucidLevel);
            double raw = (base + (double) perLucidLevel * (lv - 1)) * multiplier;
            long v = raw >= Long.MAX_VALUE ? Long.MAX_VALUE : (long) Math.floor(raw);
            return Math.max(Math.max(1, min), Math.min(max, v));
        }
    }

    public record MobSpawn(int id, int x, int y, HpSpec hp) {
    }

    /** One fight field. {@code lucidPhase} 1 / 2 = run the Lucid butterflies of that phase ({@code lucidMode} as the
     *  normal fight: 0 easy .. 2 hard); {@code vellumMechanics} = the mobs use the Vellum burrow / breath code. */
    public record Stage(int field, List<MobSpawn> mobs, Set<Integer> clearMobs, int lucidPhase, int lucidMode,
                       boolean vellumMechanics) {
    }

    public record Item(int id, int quantity) {
    }

    /** Per clear (within the daily limit): dust (夢幻的裝備 store) and items; the first clear also completes {@code quest}. */
    public record Reward(long dust, List<Item> items) {
    }

    /**
     * {@code quest} = the LucidBoss mission (102931..102934, 102961) completed on the first clear; {@code logKey} = the
     * daily counter key (tms282_bossrun_pqlog); {@code exitField} = where a finished / failed run returns (the trial
     * lobby); {@code closedText} is shown instead of an entry when {@code enabled} is false.
     */
    public record Trial(String key, String name, boolean enabled, String closedText, int quest, int logKey, int dailyLimit,
                        int lobbyField, int exitField, List<Stage> stages, Reward reward) {
        public Set<Integer> stageFields() {
            Set<Integer> s = new LinkedHashSet<>();
            for (Stage st : stages) {
                s.add(st.field());
            }
            return s;
        }

        public boolean hasStageField(int fieldId) {
            for (Stage st : stages) {
                if (st.field() == fieldId) {
                    return true;
                }
            }
            return false;
        }
    }

    public DreamTrialConfig {
        trials = List.copyOf(trials);
    }

    public static DreamTrialConfig empty() {
        return new DreamTrialConfig(1800, 5, 1, 1, 1.0, 3000, 3000, 20, true, 100000000, List.of());
    }

    public Trial trial(String key) {
        for (Trial t : trials) {
            if (t.key().equalsIgnoreCase(key)) {
                return t;
            }
        }
        return null;
    }

    /** The trial whose stages use {@code fieldId}, or null. */
    public Trial trialOfField(int fieldId) {
        for (Trial t : trials) {
            if (t.hasStageField(fieldId)) {
                return t;
            }
        }
        return null;
    }

    /** The trial whose lobby is {@code fieldId}, or null. */
    public Trial trialOfLobby(int fieldId) {
        for (Trial t : trials) {
            if (t.lobbyField() == fieldId) {
                return t;
            }
        }
        return null;
    }

    public int dailyLimitOf(Trial t) {
        return t.dailyLimit() > 0 ? t.dailyLimit() : dailyLimit;
    }

    /** Stage fields by Lucid phase (1 / 2) of the enabled trials (for {@code Lucid.setExtraFields}). */
    public Set<Integer> lucidFields(int phase) {
        Set<Integer> s = new LinkedHashSet<>();
        for (Trial t : trials) {
            if (!t.enabled()) continue;
            for (Stage st : t.stages()) {
                if (st.lucidPhase() == phase) s.add(st.field());
            }
        }
        return s;
    }

    /** Mobs of stages with {@code vellumMechanics} of the enabled trials (for {@code BossConstants.setExtraVellum}). */
    public Set<Integer> vellumMobs() {
        Set<Integer> s = new LinkedHashSet<>();
        for (Trial t : trials) {
            if (!t.enabled()) continue;
            for (Stage st : t.stages()) {
                if (st.vellumMechanics()) st.mobs().forEach(m -> s.add(m.id()));
            }
        }
        return s;
    }

    // ---- parsing -------------------------------------------------------------------------------------------------

    public static DreamTrialConfig parse(JsonNode root) {
        if (root == null || !root.isObject()) {
            throw new IllegalArgumentException("root must be an object");
        }
        double mult = root.path("hpMultiplier").asDouble(1.0);
        if (!(mult > 0) || mult > 1_000_000) {
            throw new IllegalArgumentException("hpMultiplier must be > 0");
        }
        List<Trial> trials = new ArrayList<>();
        Map<String, Boolean> keys = new LinkedHashMap<>();
        Map<Integer, String> fields = new LinkedHashMap<>();
        for (JsonNode t : root.path("trials")) {
            Trial trial = trial(t);
            if (keys.put(trial.key().toLowerCase(java.util.Locale.ROOT), true) != null) {
                throw new IllegalArgumentException("duplicate trial key " + trial.key());
            }
            for (int f : trial.stageFields()) {
                String other = fields.put(f, trial.key());
                if (other != null) {
                    throw new IllegalArgumentException("field " + f + " used by " + other + " and " + trial.key());
                }
            }
            trials.add(trial);
        }
        return new DreamTrialConfig(
                positive(root, "timeLimitSeconds", 1800),
                Math.max(0, root.path("revives").asInt(5)),
                positive(root, "dailyLimit", 1),
                Math.max(1, root.path("minLucidLevel").asInt(1)),
                mult,
                Math.max(0, root.path("spawnDelayMs").asInt(3000)),
                Math.max(0, root.path("stageDelayMs").asInt(3000)),
                Math.max(0, root.path("exitDelaySeconds").asInt(20)),
                root.path("hpTag").asBoolean(true),
                root.path("townField").asInt(100000000),
                trials);
    }

    private static int positive(JsonNode n, String key, int def) {
        int v = n.path(key).asInt(def);
        if (v <= 0) {
            throw new IllegalArgumentException(key + " must be > 0");
        }
        return v;
    }

    private static int required(JsonNode n, String key, String where) {
        if (!n.path(key).canConvertToInt() || n.path(key).asInt() <= 0) {
            throw new IllegalArgumentException(where + ": " + key + " missing");
        }
        return n.path(key).asInt();
    }

    private static Trial trial(JsonNode t) {
        String key = t.path("key").asText("").trim();
        if (key.isEmpty() || !key.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("trial key missing or not [A-Za-z0-9_]");
        }
        String where = "trial " + key;
        List<Stage> stages = new ArrayList<>();
        for (JsonNode s : t.path("stages")) {
            stages.add(stage(s, where));
        }
        boolean enabled = t.path("enabled").asBoolean(true);
        if (enabled && stages.isEmpty()) {
            throw new IllegalArgumentException(where + ": an enabled trial needs stages");
        }
        List<Item> items = new ArrayList<>();
        for (JsonNode i : t.path("reward").path("items")) {
            int id = required(i, "id", where + " reward item");
            int q = i.path("quantity").asInt(1);
            if (q <= 0) throw new IllegalArgumentException(where + ": reward quantity must be > 0");
            items.add(new Item(id, q));
        }
        long dust = t.path("reward").path("dust").asLong(0);
        if (dust < 0) throw new IllegalArgumentException(where + ": reward dust < 0");
        int quest = t.path("quest").asInt(0);
        return new Trial(key, t.path("name").asText(key), enabled, t.path("closedText").asText("尚未開放。"),
                quest, t.path("logKey").asInt(quest > 0 ? quest : 0), Math.max(0, t.path("dailyLimit").asInt(0)),
                enabled ? required(t, "lobbyField", where) : t.path("lobbyField").asInt(0),
                enabled ? required(t, "exitField", where) : t.path("exitField").asInt(0),
                Collections.unmodifiableList(stages), new Reward(dust, List.copyOf(items)));
    }

    private static Stage stage(JsonNode s, String where) {
        int field = required(s, "field", where + " stage");
        String w = where + " stage " + field;
        List<MobSpawn> mobs = new ArrayList<>();
        for (JsonNode m : s.path("mobs")) {
            mobs.add(new MobSpawn(required(m, "id", w + " mob"), m.path("x").asInt(0), m.path("y").asInt(0), hp(m.path("hp"), w)));
        }
        Set<Integer> clear = new LinkedHashSet<>();
        for (JsonNode c : s.path("clearMobs")) clear.add(c.asInt());
        if (mobs.isEmpty() || clear.isEmpty()) {
            throw new IllegalArgumentException(w + ": mobs and clearMobs are required");
        }
        for (int c : clear) {
            if (mobs.stream().noneMatch(m -> m.id() == c)) {
                throw new IllegalArgumentException(w + ": clear mob " + c + " is not spawned");
            }
        }
        int phase = s.path("lucidPhase").asInt(0);
        if (phase < 0 || phase > 2) throw new IllegalArgumentException(w + ": lucidPhase 0..2");
        return new Stage(field, List.copyOf(mobs), Collections.unmodifiableSet(clear), phase,
                Math.max(0, Math.min(2, s.path("lucidMode").asInt(2))), s.path("vellumMechanics").asBoolean(false));
    }

    private static HpSpec hp(JsonNode h, String where) {
        if (h.isMissingNode() || h.isNull()) {
            throw new IllegalArgumentException(where + ": every mob needs an hp rule");
        }
        long base = h.path("base").asLong(-1);
        long per = h.path("perLucidLevel").asLong(0);
        long min = h.path("min").asLong(1);
        long max = h.path("max").asLong(Long.MAX_VALUE);
        if (base <= 0 || per < 0 || min < 1 || max < min) {
            throw new IllegalArgumentException(where + ": bad hp rule (base > 0, perLucidLevel >= 0, 1 <= min <= max)");
        }
        return new HpSpec(base, per, min, max);
    }
}
