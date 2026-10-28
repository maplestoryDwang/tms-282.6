package net.swordie.ms.connection.protocol.codec;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.world.shop.NpcShopDlg;
import net.swordie.ms.world.shop.result.ShopResult;

/**
 * Codec family of the NPC shop packets (265 OutHeader.SHOP_OPEN / SHOP_RESULT; TMS 282.3 LP0962 / LP0963). The 265
 * {@code ShopDlg.openShop} / {@code ShopDlg.shopResult} build through {@link #active()}, so a profile only registers
 * its own encoder: GMS 265 registers none and falls back to {@link Gms265ShopPackets} (the unchanged 265 bytes);
 * TMS 282.3 registers {@code Tms282ShopPackets}.
 */
public interface ShopPacketEncoder {

    /** Opens {@code nsd} (its items, then the repurchase items of its character). */
    OutPacket openShop(int petTemplateId, NpcShopDlg nsd);

    /**
     * One shop result.
     *
     * @throws UnsupportedOperationException when this profile has no layout for the result's type
     */
    OutPacket shopResult(ShopResult shopResult);

    /** The active profile's encoder (or its version's fallback), else the GMS 265 one. */
    static ShopPacketEncoder active() {
        return ProfileEncoders.find(ShopPacketEncoder.class).orElse(Gms265ShopPackets.INSTANCE);
    }
}
