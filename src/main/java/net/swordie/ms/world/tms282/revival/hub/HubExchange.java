package net.swordie.ms.world.tms282.revival.hub;

/**
 * Coin exchange of a hub: pay coins, get the offer, as one step under the character's economy lock (the live
 * {@link Wallet} is {@link LiveWallet}). Nothing is taken when the result does not fit; nothing is given when the
 * coins are short. Counted across every stack of the coin.
 */
public final class HubExchange {
    public enum Result {BOUGHT, NOT_ENOUGH, FULL, UNAVAILABLE, BAD_AMOUNT, CLOSED, OFFLINE, FAILED}

    /** The inventory operations of one exchange; {@link #run} executes the body atomically for the character. */
    public interface Wallet {
        Result run(java.util.function.Supplier<Result> body);

        int count(int itemId);

        boolean grantable(int itemId);

        boolean canHold(int itemId, int quantity);

        /** Removes exactly {@code quantity} (several stacks if needed); false = nothing removed. */
        boolean take(int itemId, int quantity);

        /** Adds the items; only called after {@link #canHold}. */
        void give(int itemId, int quantity);

        /** Makes the change durable (character save); false is reported as FAILED but the change stays. */
        boolean commit();
    }

    public static final int MAX_TIMES = 100;

    private HubExchange() {
    }

    public static Result buy(Wallet w, HubDefinition.Offer offer, int times) {
        if (offer == null) return Result.CLOSED;
        if (times <= 0 || times > MAX_TIMES) return Result.BAD_AMOUNT;
        long cost = (long) offer.costQuantity() * times, amount = (long) offer.quantity() * times;
        if (cost > 30_000 * 10L || amount > 30_000) return Result.BAD_AMOUNT;
        return w.run(() -> {
            if (!w.grantable(offer.itemId())) return Result.UNAVAILABLE;
            if (w.count(offer.costItem()) < cost) return Result.NOT_ENOUGH;
            if (!w.canHold(offer.itemId(), (int) amount)) return Result.FULL;
            if (!w.take(offer.costItem(), (int) cost)) return Result.NOT_ENOUGH;
            w.give(offer.itemId(), (int) amount);
            return w.commit() ? Result.BOUGHT : Result.FAILED;
        });
    }
}
