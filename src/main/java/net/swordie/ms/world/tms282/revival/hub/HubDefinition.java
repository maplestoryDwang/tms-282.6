package net.swordie.ms.world.tms282.revival.hub;

import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.*;

/**
 * Data of one event hub: the missions (kill counters / level milestones whose rewards are account receipts) and the
 * coin exchange. Immutable, validated on construction; {@link HubCatalog} holds one per revival event key.
 */
public record HubDefinition(String eventKey, String title, int npcId, List<Mission> missions, List<Offer> offers) {

    /** How often a mission can be claimed again (the period is part of the receipt key). */
    public enum Period {
        DAILY, WEEKLY, ONCE;

        public String key(LocalDate day) {
            return switch (this) {
                case DAILY -> day.toString();
                case WEEKLY -> day.get(IsoFields.WEEK_BASED_YEAR) + "-W" + String.format("%02d", day.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
                case ONCE -> "once";
            };
        }

        public String label() {
            return switch (this) {
                case DAILY -> "每日";
                case WEEKLY -> "每週";
                case ONCE -> "一次";
            };
        }
    }

    public enum Kind {
        /** Any mob within {@code range} levels of the character (the client's 9101025 rule). */
        KILL_RANGE,
        /** One of {@code mobs}. */
        KILL_MOBS,
        /** Character level at least {@code target}; nothing to count, no acceptance. */
        LEVEL
    }

    public record Reward(int itemId, int quantity) {
        public Reward {
            if (itemId <= 0 || quantity <= 0 || quantity > 30_000) throw new IllegalArgumentException("reward " + itemId + "x" + quantity);
        }
    }

    /** {@code solo}: kills only count while the character is not in a party. */
    public record Mission(String id, String title, Kind kind, int target, int range, Set<Integer> mobs, int minLevel,
                          Period period, List<Reward> rewards, boolean solo) {
        public Mission {
            if (id == null || !id.matches("[a-z0-9_]{1,12}")) throw new IllegalArgumentException("mission id " + id);
            if (title == null || title.isBlank()) throw new IllegalArgumentException("mission title " + id);
            if (target <= 0) throw new IllegalArgumentException("target " + id);
            mobs = Set.copyOf(mobs == null ? Set.of() : mobs);
            rewards = List.copyOf(rewards);
            if (rewards.isEmpty() || rewards.size() > 8) throw new IllegalArgumentException("rewards " + id);
            if (kind == Kind.KILL_RANGE && range <= 0) throw new IllegalArgumentException("range " + id);
            if (kind == Kind.KILL_MOBS && mobs.isEmpty()) throw new IllegalArgumentException("mobs " + id);
            Objects.requireNonNull(period, "period");
        }

        public static Mission killRange(String id, String title, int count, int minLevel, Period period, Reward... rewards) {
            return new Mission(id, title, Kind.KILL_RANGE, count, 20, Set.of(), minLevel, period, List.of(rewards), false);
        }

        public static Mission killMobs(String id, String title, int count, Set<Integer> mobs, int minLevel, Period period, Reward... rewards) {
            return new Mission(id, title, Kind.KILL_MOBS, count, 0, mobs, minLevel, period, List.of(rewards), false);
        }

        public static Mission soloBoss(String id, String title, Set<Integer> mobs, int minLevel, Period period, Reward... rewards) {
            return new Mission(id, title, Kind.KILL_MOBS, 1, 0, mobs, minLevel, period, List.of(rewards), true);
        }

        public static Mission level(String id, String title, int level, Reward... rewards) {
            return new Mission(id, title, Kind.LEVEL, level, 0, Set.of(), 0, Period.ONCE, List.of(rewards), false);
        }

        public boolean counts(int charLevel, int mobId, int mobLevel) {
            if (charLevel < minLevel) return false;
            return switch (kind) {
                case KILL_RANGE -> mobLevel > 0 && Math.abs(mobLevel - charLevel) <= range;
                case KILL_MOBS -> mobs.contains(mobId);
                case LEVEL -> false;
            };
        }

        public boolean counted() {
            return kind != Kind.LEVEL;
        }
    }

    /** Pay {@code costQuantity} of {@code costItem}, get {@code quantity} of {@code itemId}. */
    public record Offer(int itemId, int quantity, int costItem, int costQuantity) {
        public Offer {
            if (itemId <= 0 || quantity <= 0 || quantity > 30_000 || costItem <= 0 || costQuantity <= 0 || costQuantity > 30_000)
                throw new IllegalArgumentException("offer " + itemId);
            if (itemId == costItem) throw new IllegalArgumentException("offer pays with itself " + itemId);
        }
    }

    public HubDefinition {
        if (eventKey == null || !eventKey.matches("[a-z][a-z0-9_]{0,20}")) throw new IllegalArgumentException("event " + eventKey);
        missions = List.copyOf(missions);
        offers = List.copyOf(offers);
        Set<String> ids = new HashSet<>();
        for (Mission m : missions) if (!ids.add(m.id())) throw new IllegalArgumentException("duplicate mission " + m.id());
        if (missions.size() > 40 || offers.size() > 60) throw new IllegalArgumentException("too many entries");
    }

    public Optional<Mission> mission(String id) {
        return missions.stream().filter(m -> m.id().equals(id)).findFirst();
    }

    /** Receipt activity: {@code rv_<event>} (ActivityRewards.validReceiptKey). */
    public String receiptKey(Mission m, LocalDate day) {
        return "rv_" + eventKey + ":" + m.period().key(day) + ":" + m.id();
    }
}
