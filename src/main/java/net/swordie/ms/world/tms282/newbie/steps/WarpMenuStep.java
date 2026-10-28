package net.swordie.ms.world.tms282.newbie.steps;

import net.swordie.ms.world.tms282.newbie.NewbieJson;
import net.swordie.ms.world.tms282.newbie.NewbieRun;
import net.swordie.ms.world.tms282.newbie.NewbieStep;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code WarpMenu}: where to go at the end. Settings: {@code text}, {@code destinations} ({@code label}, {@code map},
 * {@code portal}; only maps tested with the 282 client belong here) and an optional {@code stayLabel} /
 * {@code stayText}: staying keeps this step, so the menu opens again at the next login or lobby entry. Choosing a
 * destination ends the flow; the warp happens when the dialog closes (ScriptManager field transfer).
 */
public final class WarpMenuStep implements NewbieStep {
    public static final String TYPE = "WarpMenu";

    public record Destination(String label, int map, int portal) {
    }

    private final String id;
    private final String text;
    private final List<Destination> destinations;
    private final String stayLabel;
    private final String stayText;

    WarpMenuStep(String id, String text, List<Destination> destinations, String stayLabel, String stayText) {
        this.id = id;
        this.text = text;
        this.destinations = List.copyOf(destinations);
        this.stayLabel = stayLabel;
        this.stayText = stayText;
    }

    public static WarpMenuStep read(String id, NewbieJson s) {
        String text = s.text("text");
        List<Destination> d = new ArrayList<>();
        for (NewbieJson n : s.array("destinations", 1, 10)) {
            String label = n.text("label");
            if (label.contains("#")) throw NewbieJson.invalid(n.path + ".label", "must not contain #");
            d.add(new Destination(label, n.integer("map", 1, 999_999_998), n.integer("portal", 0, 255, 0)));
        }
        String stayLabel = s.text("stayLabel", "");
        if (stayLabel.contains("#")) throw NewbieJson.invalid(s.path + ".stayLabel", "must not contain #");
        String stayText = stayLabel.isEmpty() ? "" : s.text("stayText");
        return new WarpMenuStep(id, text, d, stayLabel, stayText);
    }

    public List<Destination> destinations() {
        return destinations;
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
        List<String> labels = new ArrayList<>();
        destinations.forEach(x -> labels.add(x.label()));
        if (!stayLabel.isEmpty()) labels.add(stayLabel);
        int i = run.dialog().menu(text, labels);
        if (i < 0) return Outcome.STAY;
        if (i >= destinations.size()) {
            if (i != destinations.size() || stayLabel.isEmpty()) return Outcome.STAY;
            run.dialog().ok(stayText);
            return Outcome.STAY;
        }
        Destination d = destinations.get(i);
        run.dialog().warpAfter(d.map(), d.portal());
        return Outcome.NEXT;
    }
}
