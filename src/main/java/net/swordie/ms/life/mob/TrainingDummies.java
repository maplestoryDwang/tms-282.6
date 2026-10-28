package net.swordie.ms.life.mob;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.life.Life;
import net.swordie.ms.world.field.Field;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * The player-placed training dummies (稻草人召喚袋子): which consume item summons which dummy mob, where, and the
 * summon itself. The items are script items ({@code spec/script consume_&lt;id&gt;}, {@code notConsume=1}: the bag is
 * reusable), so every item script calls {@link #summon}. Item -> mob from the 282 String/Item descriptions and the mob
 * data (PDRate 300 / 100 / none = 100% / 75% / 25% defence, elemAttr = elements halved):
 * <pre>
 *   2434325 橡樹 100% -> 9305650   2434326 橡樹 75% -> 9305651   2434327 橡樹 25% -> 9305652
 *   2434328 甘草 100% -> 9305653   2434329 甘草 75% -> 9305654   2434330 甘草 25% -> 9305655
 *   2436812 / 2437511 肯迪 -> 9305678   2436813 / 2630285 蕭公 -> 9305679   2630284 溫莉 -> 9305681
 *   2630283 稻草 25% -> 9305655
 * </pre>
 * The dummies are level-105 boss mobs with {@code removeAfter=900} (the field removes them after 15 minutes). They are
 * spawned with {@link #HP} so they cannot be killed (no EXP, no drops) and are exempt from the GM one-hit kill of the
 * 282 attack gates.
 */
public final class TrainingDummies {
    /** 武陵道場 練習用後院 - the only non-town field the item text allows. */
    public static final int DOJO_PRACTICE_FIELD = 925041002;
    /** Effectively infinite, far from overflow when damage is summed. */
    public static final long HP = 999_999_999_999_999L;

    private static final Map<Integer, Integer> MOB_BY_ITEM = Map.ofEntries(
            Map.entry(2434325, 9305650), Map.entry(2434326, 9305651), Map.entry(2434327, 9305652),
            Map.entry(2434328, 9305653), Map.entry(2434329, 9305654), Map.entry(2434330, 9305655),
            Map.entry(2436812, 9305678), Map.entry(2437511, 9305678), Map.entry(2436813, 9305679),
            Map.entry(2630285, 9305679), Map.entry(2630284, 9305681), Map.entry(2630283, 9305655));

    /** Why a summon was refused. */
    public enum Refusal {
        NOT_A_DUMMY_ITEM("這不是稻草人召喚道具。"),
        WRONG_FIELD("只能在村莊與武陵道場練習用後院內召喚稻草人。"),
        ALREADY_SUMMONED("這張地圖已經有稻草人了。"),
        SPAWN_FAILED("稻草人召喚失敗。");

        private final String message;

        Refusal(String message) {
            this.message = message;
        }

        public String message() {
            return message;
        }
    }

    private TrainingDummies() {
    }

    public static OptionalInt mobFor(int itemId) {
        Integer mob = MOB_BY_ITEM.get(itemId);
        return mob == null ? OptionalInt.empty() : OptionalInt.of(mob);
    }

    public static boolean isDummy(int mobTemplateId) {
        return MOB_BY_ITEM.containsValue(mobTemplateId);
    }

    public static boolean allowedIn(Field field) {
        return field != null && (field.isTown() || field.getId() == DOJO_PRACTICE_FIELD);
    }

    /** Whether {@code field} already holds a training dummy. */
    public static boolean hasDummy(Field field) {
        for (Life life : field.getLifes().values()) {
            if (life instanceof Mob mob && isDummy(mob.getTemplateId()) && mob.getHp() > 0) {
                return true;
            }
        }
        return false;
    }

    /** Spawns the dummy of {@code itemId} at {@code chr}'s position; empty on success, else why not. */
    public static Optional<Refusal> summon(Char chr, int itemId) {
        Objects.requireNonNull(chr, "chr");
        OptionalInt mobId = mobFor(itemId);
        if (mobId.isEmpty()) {
            return Optional.of(Refusal.NOT_A_DUMMY_ITEM);
        }
        Field field = chr.getField();
        if (!allowedIn(field)) {
            return Optional.of(Refusal.WRONG_FIELD);
        }
        if (hasDummy(field)) {
            return Optional.of(Refusal.ALREADY_SUMMONED);
        }
        Mob mob = field.spawnMob(mobId.getAsInt(), chr.getPosition().getX(), chr.getPosition().getY(), false, HP, 0);
        return mob == null ? Optional.of(Refusal.SPAWN_FAILED) : Optional.empty();
    }
}
