package net.swordie.ms.connection.protocol.codec;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.handlers.header.OutHeader;
import net.swordie.ms.world.shop.NpcShopDlg;
import net.swordie.ms.world.shop.result.ShopResult;

/** The GMS 265 shop packets: the former bodies of {@code ShopDlg.openShop} / {@code ShopDlg.shopResult}, unchanged. */
public enum Gms265ShopPackets implements ShopPacketEncoder {
    INSTANCE;

    @Override
    public OutPacket openShop(int petTemplateID, NpcShopDlg nsd) {
        OutPacket outPacket = new OutPacket(OutHeader.SHOP_OPEN);

        outPacket.encodeInt(nsd.getNpcTemplateID());
        outPacket.encodeByte(petTemplateID != 0);
        if (petTemplateID != 0) {
            outPacket.encodeInt(petTemplateID);
        }
        nsd.encode(outPacket);

        return outPacket;
    }

    @Override
    public OutPacket shopResult(ShopResult shopResult) {
        OutPacket outPacket = new OutPacket(OutHeader.SHOP_RESULT);

        outPacket.encodeByte(shopResult.getType().getVal());
        shopResult.encode(outPacket);

        return outPacket;
    }
}
