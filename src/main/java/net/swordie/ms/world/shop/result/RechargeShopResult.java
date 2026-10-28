package net.swordie.ms.world.shop.result;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.world.shop.NpcShopDlg;

/**
 * Created on 3/29/2018.
 */
public class RechargeShopResult implements ShopResult {

    private NpcShopDlg shop;

    public RechargeShopResult(NpcShopDlg shop) {
        this.shop = shop;
    }

    /** The shop whose list this result carries (the 282 encoder re-encodes it). */
    public NpcShopDlg getShop() {
        return shop;
    }

    @Override
    public ShopResultType getType() {
        return ShopResultType.RechargeSuccess;
    }

    @Override
    public void encode(OutPacket outPacket) {
        shop.encode(outPacket);
    }
}
