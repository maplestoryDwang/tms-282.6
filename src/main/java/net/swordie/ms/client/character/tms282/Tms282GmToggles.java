package net.swordie.ms.client.character.tms282;

import net.swordie.ms.client.character.Char;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-GM switches of the admin panel (管理員測試/GM開關): each applies only to the GM who turned it on, lives in memory
 * (cleared by a server restart) and is ignored for a character that is not a GM.
 * <ul>
 *     <li>one-hit kill: {@code Tms282AttackGates.gmOneHitKill} (AttackHandler only asks for GM hits);</li>
 *     <li>free shopping: NPC shops (NpcHandler meso / token / point prices), custom shops
 *     (ScriptManagerImpl.deductCustomShopCurrency) and the panel's script shops cost nothing.</li>
 * </ul>
 * Invincibility is the existing {@code Char.setInvincible} flag (265 handleUserHit, used by the 282 hit route).
 */
public final class Tms282GmToggles {
    private static final Set<Integer> ONE_HIT_KILL = ConcurrentHashMap.newKeySet();
    private static final Set<Integer> FREE_SHOP = ConcurrentHashMap.newKeySet();

    private Tms282GmToggles() {
    }

    public static boolean oneHitKill(Char chr) {
        return chr != null && ONE_HIT_KILL.contains(chr.getId()) && gm(chr);
    }

    public static boolean freeShop(Char chr) {
        return chr != null && FREE_SHOP.contains(chr.getId()) && gm(chr);
    }

    /** Flips one-hit kill for {@code chr}; returns the new state (always false for a non-GM). */
    public static boolean toggleOneHitKill(Char chr) {
        return toggle(ONE_HIT_KILL, chr);
    }

    /** Flips free shopping for {@code chr}; returns the new state (always false for a non-GM). */
    public static boolean toggleFreeShop(Char chr) {
        return toggle(FREE_SHOP, chr);
    }

    /** Flips invincibility for {@code chr}; returns the new state (always false for a non-GM). */
    public static boolean toggleInvincible(Char chr) {
        if (!gm(chr)) {
            return false;
        }
        chr.setInvincible(!chr.isInvincible());
        return chr.isInvincible();
    }

    /** A GM; a user without an account type (e.g. not loaded) is not one. */
    private static boolean gm(Char chr) {
        return chr != null && chr.getUser() != null && chr.getUser().getAccountType() != null && chr.isGM();
    }

    private static boolean toggle(Set<Integer> set, Char chr) {
        if (!gm(chr)) {
            return false;
        }
        if (!set.remove(chr.getId())) {
            set.add(chr.getId());
            return true;
        }
        return false;
    }
}
