package net.swordie.ms.world.tms282.contents.common;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.constants.ItemConstants;
import net.swordie.ms.util.FileTime;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Opens a box / exchange coupon written for content-1005 (楓之谷學院 boxes and coupons): inside the economy lock it
 * checks the coupon is still there, prepares every item (282-grantable; {@code days &gt; 0} = that many days from now,
 * else permanent), checks the bag, consumes one coupon, adds the items and saves at once (the coupon is the receipt:
 * a double click sees the coupon gone). Item scripts call {@link #get()}.{@link #open}.
 */
public final class CouponOpener {
    public enum Result {OK, NO_COUPON, FULL, UNAVAILABLE, OFFLINE, FAILED}

    public record Grant(int itemId, int quantity, int days) {
        public Grant {
            if (itemId <= 0 || quantity <= 0 || quantity > 30_000 || days < 0) {
                throw new IllegalArgumentException("bad grant " + itemId);
            }
        }

        public static Grant of(int itemId, int quantity) {
            return new Grant(itemId, quantity, 0);
        }
    }

    private static volatile CouponOpener instance;
    private final AccountLedger.Runtime runtime;

    public CouponOpener(AccountLedger.Runtime runtime) {
        this.runtime = Objects.requireNonNull(runtime);
    }

    public static CouponOpener get() {
        CouponOpener o = instance;
        if (o == null) {
            o = new CouponOpener(new AccountLedger.ServerRuntime());
            instance = o;
        }
        return o;
    }

    public static void install(CouponOpener opener) {
        instance = opener;
    }

    public Result open(Char chr, int couponId, List<Grant> grants) {
        if (chr == null) {
            return Result.OFFLINE;
        }
        List<Grant> list = List.copyOf(grants);
        Result[] out = {Result.FAILED};
        runtime.locked(chr, () -> {
            boolean online;
            try {
                online = runtime.online(chr);
            } catch (RuntimeException e) {
                online = false;
            }
            if (!online) {
                out[0] = Result.OFFLINE;
                return;
            }
            try {
                if (!chr.hasItemCount(couponId, 1)) {
                    out[0] = Result.NO_COUPON;
                    return;
                }
                List<Item> items = new ArrayList<>();
                for (Grant g : list) {
                    Item item = runtime.prepare(g.itemId(), g.quantity());
                    if (item == null) {
                        out[0] = Result.UNAVAILABLE;
                        return;
                    }
                    if (g.days() > 0) {
                        item.setDateExpire(FileTime.fromDate(LocalDateTime.now().plusDays(g.days())));
                    }
                    if (ItemConstants.isEquip(g.itemId())) {
                        for (int i = 0; i < g.quantity(); i++) {
                            Item copy = i == 0 ? item : item.deepCopy();
                            copy.setQuantity(1);
                            items.add(copy);
                        }
                    } else {
                        items.add(item);
                    }
                }
                if (!items.isEmpty() && !runtime.canHold(chr, items)) {
                    out[0] = Result.FULL;
                    return;
                }
                chr.consumeItem(couponId, 1);
                for (Item item : items) {
                    runtime.add(chr, item);
                }
                out[0] = runtime.save(chr) ? Result.OK : Result.FAILED;
                System.out.println("[CouponOpener] character=" + chr.getId() + " coupon=" + couponId + " result=" + out[0]);
            } catch (RuntimeException e) {
                System.out.println("[CouponOpener] coupon=" + couponId + " character=" + chr.getId() + " failed: " + e);
                out[0] = Result.FAILED;
            }
        });
        return out[0];
    }
}
