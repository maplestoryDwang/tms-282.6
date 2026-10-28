package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.client.character.items.Tms282Beauty;
import net.swordie.ms.enums.SpecStat;
import net.swordie.ms.loaders.ItemData;
import net.swordie.ms.loaders.containerclasses.ItemInfo;

/**
 * Consume-bag hair / face / skin coupons (254xxxx hair, 289xxxx face, 2893xxx skin; e.g. the Frieren collab
 * 2543137..2543142 and 2897007..2897012) sent as CP01C2. Each coupon names one style in its 282 item data ({@code spec/cosmetic}); the
 * style must be in the 282 beauty catalogue ({@link Tms282Beauty}) and fit the character's gender (ids whose
 * thousands digit is 0 are male, 1 female, anything else unisex, as 265 {@code ItemConstants.getGenderFromId}).
 * The look changes through {@link Tms282Beauty#apply} (the single mutation point: stat update + avatar-modified),
 * then one coupon is consumed. Only the character's own look (target 0) is supported; the android (0x64 + SN) and
 * the other target codes of 4E16E0 are refused without consuming until a capture shows the official reply.
 */
final class Tms282ConsumeBeautyCoupons {
    private Tms282ConsumeBeautyCoupons() {
    }

    /** What {@link #use} did: the kind (0 hair, 1 face) and style, or why it refused (nothing changed then). */
    record Result(int kind, int styleId, String refused) {
        static Result refuse(String why) {
            return new Result(-1, 0, why);
        }

        boolean ok() {
            return refused == null;
        }
    }

    static boolean isCoupon(int itemId) {
        int prefix = itemId / 10000;
        return prefix == 254 || prefix == 289;
    }

    /** The coupon's style ({@code spec/cosmetic}), or 0. */
    static int cosmetic(int itemId) {
        ItemInfo info = ItemData.getItemInfoByID(itemId);
        return info == null ? 0 : info.getSpecStats().getOrDefault(SpecStat.cosmetic, 0);
    }

    /** 0 hair, 1 face, 2 skin (2893xxx skin coupons: cosmetic = skin id), -1 other. */
    static int kind(int style) {
        if (style > 0 && style <= 255) return 2;
        if (style >= 30000 && style < 50000 || style >= 60000 && style < 70000) return 0;
        if (style >= 20000 && style < 30000 || style >= 50000 && style < 60000) return 1;
        return -1;
    }

    /** 0 male, 1 female, 2 any. */
    static int gender(int style) {
        if (kind(style) == 2) return 2;
        int digit = style / 1000 % 10;
        return digit == 0 ? 0 : digit == 1 ? 1 : 2;
    }

    /** Whether the coupon names a style this server can apply (catalogue + kind), regardless of the character. */
    static boolean usable(int itemId) {
        int style = cosmetic(itemId);
        int kind = kind(style);
        if (!isCoupon(itemId) || kind < 0) return false;
        try {
            Tms282Beauty.name(kind, style);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    static Result use(Char chr, Item coupon, int target) {
        int itemId = coupon.getItemId();
        if (!isCoupon(itemId)) return Result.refuse("not a 254/289 beauty coupon");
        if (target != 0) return Result.refuse("target " + target + " (android / second look) not supported yet");
        int style = cosmetic(itemId);
        int kind = kind(style);
        if (kind < 0) return Result.refuse("no spec/cosmetic hair or face (" + style + ")");
        try {
            Tms282Beauty.name(kind, style);
        } catch (RuntimeException e) {
            return Result.refuse("style " + style + " not in the 282 beauty catalogue");
        }
        int g = gender(style);
        if (g != 2 && g != chr.getAvatarData().getAvatarLook().getGender()) {
            return Result.refuse("style " + style + " is for gender " + g);
        }
        if (Tms282Beauty.current(chr, kind) == style) return Result.refuse("already wearing " + style);
        Tms282Beauty.apply(chr, kind, style);
        chr.consumeItem(coupon, 1);
        return new Result(kind, style, null);
    }
}
