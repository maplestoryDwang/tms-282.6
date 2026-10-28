package net.swordie.ms.world.shop.result;

import net.swordie.ms.connection.OutPacket;

/**
 * Created on 3/29/2018.
 */
public class MsgShopResult implements ShopResult {

    private ShopResultType type;
    private int intArg;

    public MsgShopResult(ShopResultType type) {
        this.type = type;
    }

    public static MsgShopResult canOnlyPurchaseXMore(int value) {
        MsgShopResult msgShopResult = new MsgShopResult(ShopResultType.CanOnlyPurchaseXMoreMsgInt);
        msgShopResult.intArg = value;
        return msgShopResult;
    }

    @Override
    public ShopResultType getType() {
        return type;
    }

    @Override
    public void encode(OutPacket outPacket) {
        if (type == ShopResultType.CanOnlyPurchaseXMoreMsgInt) {
            outPacket.encodeInt(intArg);
        }
    }
}
