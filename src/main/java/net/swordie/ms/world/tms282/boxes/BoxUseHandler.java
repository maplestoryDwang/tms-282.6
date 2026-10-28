package net.swordie.ms.world.tms282.boxes;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.Item;

/**
 * One data-driven family of "open a box / use a coupon" cash items (raffle boxes, incubators, Luna Crystals,
 * random beauty coupons). Registered in {@link Tms282BoxUse}; the first handler whose {@link #handles(int)} is true
 * owns the request, so a handler must never fall back to the legacy hardcoded paths.
 */
public interface BoxUseHandler {
    /** Short name for logs. */
    String name();

    /** Whether this handler has data for {@code itemId}. */
    boolean handles(int itemId);

    /**
     * Uses {@code box} (already verified to be in {@code chr}'s inventory with quantity >= 1).
     * Must consume the box only after every check passed and the reward was granted.
     */
    void use(Char chr, Item box);
}
