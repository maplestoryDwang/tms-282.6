package net.swordie.ms.world.tms282.boxes;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.client.character.items.Tms282Beauty;
import net.swordie.ms.enums.ChatType;

import java.util.HashMap;
import java.util.Map;

/**
 * Random hair/face/skin coupons (515xxxx), data from {@code beauty/beauty-coupon.tsv}
 * (TMS279 beauty_coupon.csv merged with MetaMS 273 BeautyCard), plus the complete official pools of
 * {@code beauty/beauty-coupon-pools-web.tsv} (TW probability pages; the current look is excluded and its weight
 * redistributed proportionally, as the pages state). Styles are re-checked at use time against the
 * 282 beauty catalogue ({@link Tms282Beauty}) and the character's gender; the look change goes through
 * {@link Tms282Beauty#apply}, the single mutation point that sends the stat update and avatar-modified packet.
 */
public final class BeautyCouponHandler implements BoxUseHandler {
    /** kind: 0 hair, 1 face, 2 skin (Tms282Beauty categories); gender 0 male, 1 female, 2 any. */
    public record Style(int kind, int styleId, int gender) {
    }

    private final Map<Integer, WeightedPool<Style>> pools;

    private BeautyCouponHandler(Map<Integer, WeightedPool<Style>> pools) {
        this.pools = Map.copyOf(pools);
    }

    public static BeautyCouponHandler load() {
        Map<Integer, WeightedPool.Builder<Style>> builders = new HashMap<>();
        for (String[] r : BoxSupport.readTsv(BoxSupport.dataDir().resolve("beauty").resolve("beauty-coupon.tsv"), 5)) {
            int kind = switch (r[1]) {
                case "hair" -> 0;
                case "face" -> 1;
                case "skin" -> 2;
                default -> throw new IllegalArgumentException("Invalid beauty coupon kind " + r[1]);
            };
            builders.computeIfAbsent(Integer.parseInt(r[0]), k -> WeightedPool.builder())
                    .add(new Style(kind, Integer.parseInt(r[2]), BoxSupport.intOr(r[3], 2)), BoxSupport.intOr(r[4], 1));
        }
        // Official TW probability pages (beauty-coupon-pools-web.tsv): only complete pools, weight = percent x 100;
        // coupons already in beauty-coupon.tsv keep their pool unless the web rows say "replaces-tsv" (user decision
        // 2026-09-28: cash-shop royal coupons 5150040/5152053 follow the current 8372 period; their tsv rows stay in
        // beauty-coupon.tsv untouched and come back if the web rows are removed or incomplete).
        java.nio.file.Path web = net.swordie.ms.world.tms282.items.Tms282BeautyCoupons.webFile();
        if (java.nio.file.Files.isRegularFile(web)) {
            net.swordie.ms.world.tms282.items.Tms282BeautyCoupons.completeRandomWebPools(web).forEach((coupon, rows) -> {
                boolean replaces = rows.stream().allMatch(r -> r.confidence().contains("replaces-tsv"));
                if (!builders.containsKey(coupon) || replaces) {
                    WeightedPool.Builder<Style> b = WeightedPool.builder();
                    builders.put(coupon, b);
                    rows.forEach(r -> b.add(new Style(r.kind(), r.styleId(), r.gender()), (int) Math.round(r.prob() * 100)));
                }
            });
        }
        Map<Integer, WeightedPool<Style>> pools = new HashMap<>();
        builders.forEach((k, b) -> pools.put(k, b.build()));
        return new BeautyCouponHandler(pools);
    }

    @Override
    public String name() {
        return "beauty(" + pools.size() + " coupons)";
    }

    /** Pool entries of a coupon (offline checks); empty when the coupon has no pool. */
    public java.util.List<WeightedPool.Entry<Style>> entries(int coupon) {
        WeightedPool<Style> p = pools.get(coupon);
        return p == null ? java.util.List.of() : p.entries();
    }

    @Override
    public boolean handles(int itemId) {
        return pools.containsKey(itemId);
    }

    private static boolean renderable(Style s) {
        try {
            Tms282Beauty.name(s.kind(), s.styleId());
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    @Override
    public void use(Char chr, Item box) {
        int gender = chr.getAvatarData().getAvatarLook().getGender();
        Style pick = pools.get(box.getItemId()).pick(s -> (s.gender() == 2 || s.gender() == gender)
                && Tms282Beauty.current(chr, s.kind()) != s.styleId()
                && renderable(s));
        if (pick == null) {
            chr.chatMessage(ChatType.SystemNotice, "此美容券目前沒有適合你的造型，未消耗此道具。");
            return;
        }
        String name = Tms282Beauty.name(pick.kind(), pick.styleId());
        Tms282Beauty.apply(chr, pick.kind(), pick.styleId());
        chr.consumeItem(box, 1);
        String what = switch (pick.kind()) {
            case 0 -> "髮型";
            case 1 -> "臉型";
            default -> "膚色";
        };
        chr.chatMessage(ChatType.GameDesc, String.format("%s已變更為「%s」。", what, name));
    }
}
