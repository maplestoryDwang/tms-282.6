package net.swordie.ms.world.tms282.boxes;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.Item;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Boxes without client {@code reward/} data whose contents come from the 2026-09-28 research
 * (research/tms2823/item-usability-20260928/boxes/official/rewards.tsv, high/med confidence only), filtered by
 * {@code boxes/build_box_tables.py} into {@code tms282/boxes/official-rewards.tsv}. Three modes, never an invented
 * probability:
 * <ul>
 *     <li>{@code weighted}: official probability page, rows add up to 100% -> one draw;</li>
 *     <li>{@code choice}: "擇一" box -> selection menu ({@link BoxChoiceDialog}, NPC 9010000);</li>
 *     <li>{@code fixed}: exchange coupon / package -> every listed item.</li>
 * </ul>
 * A box whose item has a written item script is left to the script.
 */
public final class OfficialRewardBoxHandler implements BoxUseHandler {
    public enum Mode { WEIGHTED, CHOICE, FIXED }

    public record Row(int itemId, int count, int weight, int periodDays) {
    }

    public record Box(Mode mode, List<Row> rows, WeightedPool<Row> pool) {
    }

    private final Map<Integer, Box> boxes;
    private final String label;

    OfficialRewardBoxHandler(Map<Integer, Box> boxes) {
        this(boxes, "official-rewards");
    }

    OfficialRewardBoxHandler(Map<Integer, Box> boxes, String label) {
        this.boxes = Map.copyOf(boxes);
        this.label = label;
    }

    public static OfficialRewardBoxHandler load() {
        return load("official-rewards.tsv", "official-rewards");
    }

    /**
     * Another table of the same format, e.g. {@code collab-frieren.tsv} (the hand-verified 葬送的芙莉蓮 collab
     * tickets, registered before the generic tables).
     */
    public static OfficialRewardBoxHandler load(String file, String label) {
        return new OfficialRewardBoxHandler(read(BoxSupport.dataDir().resolve("boxes").resolve(file)), label);
    }

    public String label() {
        return label;
    }

    /** box_id, mode, reward_item_id, count, weight, period_days, source. */
    static Map<Integer, Box> read(Path file) {
        Map<Integer, Mode> modes = new HashMap<>();
        Map<Integer, List<Row>> rows = new HashMap<>();
        if (!Files.exists(file)) {
            return Map.of();
        }
        for (String[] r : BoxSupport.readTsv(file, 7)) {
            int box = Integer.parseInt(r[0]);
            Mode mode = Mode.valueOf(r[1].toUpperCase());
            Mode before = modes.putIfAbsent(box, mode);
            if (before != null && before != mode) {
                throw new IllegalArgumentException("box " + box + " has rows of modes " + before + " and " + mode);
            }
            Row row = new Row(Integer.parseInt(r[2]), Math.max(1, BoxSupport.intOr(r[3], 1)), BoxSupport.intOr(r[4], 0),
                    BoxSupport.intOr(r[5], 0));
            if (mode == Mode.WEIGHTED && row.weight() <= 0) {
                throw new IllegalArgumentException("weighted box " + box + " row without a weight: " + row);
            }
            rows.computeIfAbsent(box, k -> new ArrayList<>()).add(row);
        }
        Map<Integer, Box> out = new HashMap<>();
        rows.forEach((box, rs) -> {
            WeightedPool.Builder<Row> b = WeightedPool.builder();
            if (modes.get(box) == Mode.WEIGHTED) {
                rs.forEach(r -> b.add(r, r.weight()));
            }
            out.put(box, new Box(modes.get(box), List.copyOf(rs), b.build()));
        });
        return out;
    }

    @Override
    public String name() {
        return label + "(" + boxes.size() + " boxes)";
    }

    public Map<Integer, Box> boxes() {
        return boxes;
    }

    @Override
    public boolean handles(int itemId) {
        return boxes.containsKey(itemId) && !BoxSupport.hasItemScript(itemId);
    }

    @Override
    public void use(Char chr, Item box) {
        Box b = boxes.get(box.getItemId());
        switch (b.mode()) {
            case WEIGHTED -> {
                Row pick = b.pool().pick(r -> BoxSupport.available(r.itemId()) && BoxSupport.genderAllows(chr, r.itemId()));
                Item reward = pick == null ? null : BoxSupport.createItem(pick.itemId(), pick.count(), pick.periodDays());
                if (reward == null) {
                    BoxSupport.refuseNoData(chr);
                    return;
                }
                BoxSupport.giveAndConsume(chr, box, List.of(reward), 0);
            }
            case FIXED -> giveAll(chr, box, b.rows());
            case CHOICE -> {
                List<BoxChoiceDialog.Option> options = new ArrayList<>();
                for (Row r : b.rows()) {
                    if (BoxSupport.available(r.itemId()) && BoxSupport.genderAllows(chr, r.itemId())) {
                        options.add(new BoxChoiceDialog.Option(r.itemId(), r.count(), r.periodDays()));
                    }
                }
                if (options.isEmpty()) {
                    BoxSupport.refuseNoData(chr);
                    return;
                }
                BoxChoiceDialog.open(chr, box, options);
            }
        }
    }

    /** Every row, or nothing when one of them no longer exists in 282. */
    static void giveAll(Char chr, Item box, List<Row> rows) {
        List<Item> gains = new ArrayList<>();
        for (Row r : rows) {
            Item it = BoxSupport.available(r.itemId()) ? BoxSupport.createItem(r.itemId(), r.count(), r.periodDays()) : null;
            if (it == null) {
                BoxSupport.refuseNoData(chr);
                return;
            }
            gains.add(it);
        }
        BoxSupport.giveAndConsume(chr, box, gains, 0);
    }
}
