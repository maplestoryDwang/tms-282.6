package net.swordie.ms.world.tms282.activity;

import net.swordie.ms.client.character.Char;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Point-paid raffles (樂豆抽獎, e.g. 樂豆輪迴). The script rolls the prizes; {@link PointOrder} charges the wallet and
 * writes a receipt in one InnoDB transaction, then grants exactly the prepared items and saves the character.
 * Nothing is charged when the bag cannot hold every prize or a prize is not available in 282.
 * A crash after the commit but before the inventory save needs manual receipt review (same as {@link ActivityRewards}).
 */
public final class PointRaffle {
    public static final int RECEIPT_QUEST = 99_999_997;
    public static final int MAX_LINES = 128;
    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    /** LIMITED: a purchase limit of the order ({@link PointOrder#limit}) is already used up. */
    public enum Status {GRANTED, INSUFFICIENT, FULL, UNAVAILABLE, LIMITED, OFFLINE, FAILED}

    /** {@code saved} is false only for GRANTED when the follow-up character save did not report SAVED. */
    public record Result(Status status, int charged, int balance, String receipt, boolean saved) {
        public String name() { return status.name(); }
        public boolean granted() { return status == Status.GRANTED; }
        static Result of(Status status) { return new Result(status, 0, 0, null, false); }
    }

    private PointRaffle() {}

    static void validate(String raffleKey, int cost, int[] itemIds, int[] quantities) {
        if (raffleKey == null || !raffleKey.matches("[a-z][a-z0-9_]{0,23}") || cost <= 0 || cost > 1_000_000
                || itemIds == null || quantities == null || itemIds.length == 0 || itemIds.length > MAX_LINES
                || itemIds.length != quantities.length)
            throw new IllegalArgumentException("Invalid raffle request");
        for (int count : quantities) if (count <= 0 || count > 30_000) throw new IllegalArgumentException("Invalid raffle quantity");
    }

    static String receiptKey(String raffleKey, int characterId) {
        return "raffle:" + raffleKey + ":" + Long.toString(System.currentTimeMillis(), 36) + ":"
                + Integer.toString(characterId, 36) + ":" + Integer.toString(SEQUENCE.incrementAndGet() & 0xFFFFF, 36);
    }

    /** 樂豆 (users.maplePoints) raffle with aggregated prize lines; same as a {@code maple} {@link PointOrder}. */
    public static Result purchase(Char chr, String raffleKey, int cost, int[] itemIds, int[] quantities) {
        return purchase(chr, "maple", raffleKey, cost, itemIds, quantities);
    }

    public static Result purchase(Char chr, String wallet, String raffleKey, int cost, int[] itemIds, int[] quantities) {
        validate(raffleKey, cost, itemIds, quantities);
        PointOrder order = new PointOrder(chr, wallet, raffleKey, cost);
        for (int i = 0; i < itemIds.length; i++) order.item(itemIds[i], quantities[i]);
        return order.purchase();
    }
}
