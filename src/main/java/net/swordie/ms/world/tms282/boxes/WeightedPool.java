package net.swordie.ms.world.tms282.boxes;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

/** Immutable weighted pool; {@link #pick(Predicate)} draws among the entries accepted by the filter. */
public final class WeightedPool<T> {
    public record Entry<T>(T value, int weight) {
    }

    private final List<Entry<T>> entries;

    private WeightedPool(List<Entry<T>> entries) {
        this.entries = List.copyOf(entries);
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public List<Entry<T>> entries() {
        return entries;
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /** @return a weighted random value accepted by {@code filter}, or null when none qualifies. */
    public T pick(Predicate<T> filter) {
        long total = 0;
        List<Entry<T>> eligible = new ArrayList<>();
        for (Entry<T> e : entries) {
            if (e.weight() > 0 && filter.test(e.value())) {
                eligible.add(e);
                total += e.weight();
            }
        }
        if (total <= 0) {
            return null;
        }
        long roll = ThreadLocalRandom.current().nextLong(total);
        for (Entry<T> e : eligible) {
            roll -= e.weight();
            if (roll < 0) {
                return e.value();
            }
        }
        return eligible.get(eligible.size() - 1).value();
    }

    public static final class Builder<T> {
        private final List<Entry<T>> entries = new ArrayList<>();

        public Builder<T> add(T value, int weight) {
            if (weight > 0) {
                entries.add(new Entry<>(value, weight));
            }
            return this;
        }

        public WeightedPool<T> build() {
            return new WeightedPool<>(entries);
        }
    }
}
