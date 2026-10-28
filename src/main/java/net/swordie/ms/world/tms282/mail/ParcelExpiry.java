package net.swordie.ms.world.tms282.mail;

import net.swordie.ms.client.character.items.Tms282ParcelItemRows;
import net.swordie.ms.util.DataPrinter;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Parcel expiry: a parcel past its keep time goes back to its sender (type RETURNED, a fresh 30-day keep);
 * a RETURNED parcel that expires again is destroyed together with its item row. Runs every
 * {@link #PERIOD_MINUTES} minutes on a daemon thread, started lazily by the first mail use (no hook needed).
 */
public final class ParcelExpiry {
    public static final long PERIOD_MINUTES = 10;
    static final int BATCH = 200;
    private static final AtomicBoolean STARTED = new AtomicBoolean();

    private ParcelExpiry() { }

    public static void ensureStarted() {
        if (!STARTED.compareAndSet(false, true)) return;
        ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "tms282-parcel-expiry");
            t.setDaemon(true);
            return t;
        });
        ses.scheduleWithFixedDelay(ParcelExpiry::sweepQuietly, 1, PERIOD_MINUTES, TimeUnit.MINUTES);
    }

    static void sweepQuietly() {
        try {
            sweep(LocalDateTime.now());
        } catch (Exception e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
        }
    }

    /** Returns how many parcels were returned or destroyed. */
    public static int sweep(LocalDateTime now) throws SQLException {
        int done = 0;
        List<Parcel> due;
        try (Connection c = MailSchema.connection()) {
            due = ParcelStore.expired(c, now, BATCH);
        }
        for (Parcel p : due) {
            try (Connection c = MailSchema.connection()) {
                c.setAutoCommit(false);
                try {
                    if (ParcelStore.lockForReceiver(c, p.id(), p.receiverId()).isEmpty()) { c.rollback(); continue; }
                    boolean destroy = p.deliveryType() == Parcel.RETURNED || p.senderId() <= 0
                            || MailRecipient.byId(c, p.senderId()).isEmpty();
                    if (destroy) {
                        ParcelStore.delete(c, p.id(), p.receiverId());
                        c.commit();
                        c.setAutoCommit(true);
                        if (p.hasItem()) Tms282ParcelItemRows.delete(c, p.itemRow());
                        DataPrinter.send(DataPrinter.ITEM, String.format("parcel %d expired and destroyed (item=%d x%d meso=%d)",
                                p.id(), p.itemId(), p.quantity(), p.money()), true);
                    } else {
                        if (!ParcelStore.returnToSender(c, p, now, now.plus(ParcelService.KEEP))) { c.rollback(); continue; }
                        c.commit();
                        ParcelService.get().notifyReturned(c, p);
                    }
                    done++;
                } catch (SQLException | RuntimeException e) {
                    c.rollback();
                    DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
                }
            }
        }
        return done;
    }
}
