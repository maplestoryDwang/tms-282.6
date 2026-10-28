package net.swordie.ms.world.shop.result;

import net.swordie.ms.connection.OutPacket;

/**
 * Created on 3/29/2018.
 */
public class BuyShopResult implements ShopResult {

    private boolean isUpdateRepurchaseItem;
    private int repurchaseItemIndex;
    private int itemID;
    private int amountLeft;
    private int starCoinUpdate;

    public BuyShopResult(int repurchaseItemIndex) {
        this.isUpdateRepurchaseItem = true;
        this.repurchaseItemIndex = repurchaseItemIndex;
    }

    public BuyShopResult(int itemID, int amountLeft) {
        this.isUpdateRepurchaseItem = false;
        this.itemID = itemID;
        this.amountLeft = amountLeft;
        this.starCoinUpdate = 0;
    }

    @Override
    public ShopResultType getType() {
        return ShopResultType.Buy;
    }

    @Override
    public void encode(OutPacket outPacket) {
        if (isUpdateRepurchaseItem) {
            outPacket.encodeByte(true);
            outPacket.encodeInt(repurchaseItemIndex);
        } else {
            outPacket.encodeByte(false);
            outPacket.encodeInt(itemID);
            outPacket.encodeInt(amountLeft);
            outPacket.encodeInt(starCoinUpdate);
        }
    }
}
