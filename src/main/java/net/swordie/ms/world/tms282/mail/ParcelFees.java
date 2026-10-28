package net.swordie.ms.world.tms282.mail;

/**
 * Duey fee rules (classic MapleStory Duey): a flat delivery fee per parcel plus a tax on the meso sent.
 * Quick delivery additionally consumes one {@link #QUICK_DELIVERY_TICKET}.
 */
public final class ParcelFees {
    private ParcelFees() { }

    /** 快速宅配券 (Quick Delivery Ticket, cash item). */
    public static final int QUICK_DELIVERY_TICKET = 5330000;
    public static final long BASE_FEE = 5_000;

    /** Meso tax on the amount sent: >=10M 4%, >=5M 3%, >=1M 2%, >=100K 1%, below that none. */
    public static long mesoTax(long money) {
        if (money >= 10_000_000L) return money * 4 / 100;
        if (money >= 5_000_000L) return money * 3 / 100;
        if (money >= 1_000_000L) return money * 2 / 100;
        if (money >= 100_000L) return money / 100;
        return 0;
    }

    /** Total the sender pays on top of the meso sent. */
    public static long fee(long money) {
        return BASE_FEE + mesoTax(Math.max(0, money));
    }
}
