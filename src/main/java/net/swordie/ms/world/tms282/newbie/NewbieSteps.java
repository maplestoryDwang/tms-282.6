package net.swordie.ms.world.tms282.newbie;

import net.swordie.ms.world.tms282.newbie.steps.FirstJobStep;
import net.swordie.ms.world.tms282.newbie.steps.SpawnStep;
import net.swordie.ms.world.tms282.newbie.steps.StarterGiftStep;
import net.swordie.ms.world.tms282.newbie.steps.StoryDialogStep;
import net.swordie.ms.world.tms282.newbie.steps.WarpMenuStep;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * The step registry (command pattern, like the admin actions): {@code type} of a step entry -> the factory that
 * reads its settings and builds the immutable step. A new kind of step is one class plus one {@link #register} line;
 * order, content and per-job texts live only in the configuration.
 */
public final class NewbieSteps {

    /** Reads one {@code {"id", "type", ...}} entry. */
    @FunctionalInterface
    public interface Factory {
        NewbieStep build(String id, NewbieJson settings);
    }

    private static final NewbieSteps STANDARD = new NewbieSteps()
            .register(SpawnStep.TYPE, SpawnStep::read)
            .register(StoryDialogStep.TYPE, StoryDialogStep::read)
            .register(StarterGiftStep.TYPE, StarterGiftStep::read)
            .register(FirstJobStep.TYPE, FirstJobStep::read)
            .register(WarpMenuStep.TYPE, WarpMenuStep::read);

    private final Map<String, Factory> factories = new LinkedHashMap<>();

    public static NewbieSteps standard() {
        return STANDARD;
    }

    NewbieSteps register(String type, Factory factory) {
        if (factories.putIfAbsent(type, factory) != null) throw new IllegalArgumentException("duplicate step type " + type);
        return this;
    }

    public Set<String> types() {
        return Collections.unmodifiableSet(factories.keySet());
    }

    NewbieStep build(NewbieJson entry) {
        if (!entry.node.isObject()) throw NewbieJson.invalid(entry.path, "must be an object");
        String id = entry.word("id", 32);
        String type = entry.word("type", 32);
        Factory f = factories.get(type);
        if (f == null) throw NewbieJson.invalid(entry.path + ".type", "unknown step type " + type + " (known: " + factories.keySet() + ")");
        return f.build(id, entry);
    }
}
