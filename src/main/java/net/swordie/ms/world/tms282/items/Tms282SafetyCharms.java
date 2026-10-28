package net.swordie.ms.world.tms282.items;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;
import net.swordie.ms.enums.ChatType;

import java.util.Set;

/**
 * 282 safety charms from item data, not ids: a CASH item whose client cash type is 0x07 and whose Item.wz
 * {@code info/recoveryRate} is set ({@link Tms282CashItemTypes.Entry#safetyCharm()}): 5130000 (100), 5130002 (30),
 * 5130003 (30), 5131000 (100); 5130003 is use-only ({@link #USE_ONLY}), not passive. Hook for Char's death path (Char is owned elsewhere; see the Fix D README snippet):
 * {@code if (Tms282SafetyCharms.protectOnDeath(this)) { no EXP loss, no death debuff }}. Only active under the 282
 * profile; 265 keeps its 5130000 script. Counted charms are consumed, period charms (45-day VIP) are kept.
 */
public final class Tms282SafetyCharms {
    private Tms282SafetyCharms() {
    }

    /**
     * Charms that only work when double-clicked (CP0150 0x07 removes the running death debuff), never passively on
     * death: 5130003's description says it removes the debuff when used.
     */
    static final Set<Integer> USE_ONLY = Set.of(5130003);

    /** True when owning {@code itemId} protects on death (a data safety charm that is not use-only). */
    public static boolean passive(int itemId) {
        if (USE_ONLY.contains(itemId)) {
            return false;
        }
        Tms282CashItemTypes.Entry e = Tms282CashItemTypes.get().entry(itemId);
        return e != null && e.safetyCharm();
    }

    public static boolean active() {
        return Protocol.active().version() == ProtocolVersion.TMS_282_3;
    }

    /** First owned passive charm by data (use-only charms excluded), or null. */
    public static Item find(Char chr) {
        for (Item item : chr.getCashInventory().getItems()) {
            if (passive(item.getItemId()) && item.getQuantity() > 0
                    && (item.getDateExpire() == null || !item.getDateExpire().isExpired())) {
                return item;
            }
        }
        return null;
    }

    /** HP/MP percent restored on revive by the charm (info/recoveryRate), 0 when none. */
    public static int recoveryRate(Item charm) {
        Tms282CashItemTypes.Entry e = charm == null ? null : Tms282CashItemTypes.get().entry(charm.getItemId());
        return e == null ? 0 : e.recoveryRate();
    }

    /**
     * On death under the 282 profile: uses the first charm (consumed unless it is a period item) and returns true, so
     * the caller skips the EXP loss and the death debuff; false when not 282 or no charm is owned.
     */
    public static boolean protectOnDeath(Char chr) {
        if (!active()) {
            return false;
        }
        Item charm = find(chr);
        if (charm == null) {
            return false;
        }
        Tms282CashItemUse.consumeUnlessPeriod(chr, charm);
        chr.chatMessage(ChatType.GameDesc, "護身符發揮效果，經驗值不會減少。");
        return true;
    }
}
