package net.swordie.ms.world.shop.auctionhouse;

import net.swordie.ms.enums.auctionhouse.AuctionState;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntFunction;

/**
 * The TMS 282.3 auction search (CP013B type 32). 265 has no reachable search case (Req_Show / Req_MarketPrice are
 * -1), so the 282 overlay answers the searched name itself: selling entries whose item name contains the text,
 * spaces ignored; an empty text lists every selling entry. The level / price / potential filters of the 282 request
 * are not applied.
 */
public final class Tms282AuctionSearch {
    private final IntFunction<String> itemName;
    private final int max;

    public Tms282AuctionSearch(IntFunction<String> itemName, int max) {
        this.itemName = Objects.requireNonNull(itemName, "itemName");
        this.max = max;
    }

    public Set<AuctionItem> find(Collection<AuctionItem> entries, String text) {
        String needle = text == null ? "" : text.replace(" ", "");
        Set<AuctionItem> found = new LinkedHashSet<>();
        for (AuctionItem a : entries) {
            if (found.size() >= max) {
                break;
            }
            if (a.getItem() == null || a.getState() != AuctionState.Selling) {
                continue;
            }
            String name = itemName.apply(a.getItem().getItemId());
            if (needle.isEmpty() || (name != null && name.replace(" ", "").contains(needle))) {
                found.add(a);
            }
        }
        return found;
    }
}
