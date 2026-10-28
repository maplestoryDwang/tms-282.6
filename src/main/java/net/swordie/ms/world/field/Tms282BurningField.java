package net.swordie.ms.world.field;

import net.swordie.ms.constants.FieldConstants;

/** Field-scoped Firestarter state. The existing server burning interval is retained. */
public final class Tms282BurningField {
    private int level = -1;
    private long changedAt;
    private boolean occupied;

    public synchronized boolean active() { return level >= 0; }

    public synchronized void ignite(long now) {
        level = FieldConstants.BURNING_FIELD_MAX_LEVEL;
        changedAt = now;
        occupied = true;
    }

    public synchronized int level(long now, boolean currentlyOccupied) {
        if (level < 0) return -1;
        long interval = FieldConstants.BURNING_FIELD_TIMER * 60_000L;
        long steps = now < changedAt ? 0 : (now - changedAt) / interval;
        if (steps > 0) {
            int amount = (int) Math.min(FieldConstants.BURNING_FIELD_MAX_LEVEL, steps);
            level = occupied ? Math.max(0, level - amount)
                    : Math.min(FieldConstants.BURNING_FIELD_MAX_LEVEL, level + amount);
            changedAt += steps * interval;
        }
        if (occupied != currentlyOccupied) {
            occupied = currentlyOccupied;
            changedAt = Math.max(changedAt, now);
        }
        return level;
    }
}
