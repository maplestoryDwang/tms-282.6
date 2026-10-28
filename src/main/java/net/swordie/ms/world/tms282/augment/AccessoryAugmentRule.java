package net.swordie.ms.world.tms282.augment;

/**
 * One MetaMS 273 {@code AccessoryAugment.json} row: spending {@code reqCount} of {@code reqItemId} plus
 * {@code mesos} on ring {@code targetItemId} at item level {@code baseLevel} succeeds with {@code prob}% and raises it
 * to {@code nextLevel}.
 */
public record AccessoryAugmentRule(int reqItemId, int reqCount, int prob, long mesos, int targetItemId,
                                   int baseLevel, int nextLevel) {
}
