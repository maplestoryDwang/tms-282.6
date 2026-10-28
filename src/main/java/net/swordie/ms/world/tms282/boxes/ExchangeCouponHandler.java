package net.swordie.ms.world.tms282.boxes;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.Item;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * W2: exchange coupons ("X交換券" -> the one 282 item named X, research boxes/out/exchange-coupon-derived.tsv, only
 * the coupons with a unique same-name id), table {@code tms282/boxes/exchange-coupon.tsv} built by
 * {@code build_box_tables.py}. Coupons that the official-rewards table also lists are left to that table (most of the
 * 1,353 unique ones: their desc names the same item), so this table only holds the rest. Consumes the coupon and
 * gives the item; a coupon with a written item script is left to the script.
 * <p>
 * {@link #exchange} is the static entry for callers outside the registry (e.g. an item-script "no script file"
 * fallback): it returns false when the coupon is not in either table.
 */
public final class ExchangeCouponHandler implements BoxUseHandler {
    private final Map<Integer, OfficialRewardBoxHandler.Row> coupons;

    ExchangeCouponHandler(Map<Integer, OfficialRewardBoxHandler.Row> coupons) {
        this.coupons = Map.copyOf(coupons);
    }

    public static ExchangeCouponHandler load() {
        Path file = BoxSupport.dataDir().resolve("boxes").resolve("exchange-coupon.tsv");
        Map<Integer, OfficialRewardBoxHandler.Row> coupons = new HashMap<>();
        if (Files.exists(file)) {
            for (String[] r : BoxSupport.readTsv(file, 3)) {
                int coupon = Integer.parseInt(r[0]);
                if (coupons.put(coupon, new OfficialRewardBoxHandler.Row(Integer.parseInt(r[1]),
                        Math.max(1, BoxSupport.intOr(r[2], 1)), 0, 0)) != null) {
                    throw new IllegalArgumentException("coupon " + coupon + " listed twice");
                }
            }
        }
        return new ExchangeCouponHandler(coupons);
    }

    @Override
    public String name() {
        return "exchange-coupon(" + coupons.size() + " coupons)";
    }

    public Map<Integer, OfficialRewardBoxHandler.Row> coupons() {
        return coupons;
    }

    @Override
    public boolean handles(int itemId) {
        return coupons.containsKey(itemId) && !BoxSupport.hasItemScript(itemId);
    }

    @Override
    public void use(Char chr, Item coupon) {
        OfficialRewardBoxHandler.giveAll(chr, coupon, List.of(coupons.get(coupon.getItemId())));
    }

    /**
     * Uses {@code coupon} through the box registry (this table, the official-rewards table and the other box
     * handlers) and, on a 282 request thread, sends the 282 bag / stat replies.
     *
     * @return false when no box handler owns the item (nothing sent, nothing consumed)
     */
    public static boolean exchange(Char chr, Item coupon) {
        return Tms282BoxUse.tryUse(chr, coupon, Tms282BoxUse.Replies.ALL_BAGS);
    }
}
