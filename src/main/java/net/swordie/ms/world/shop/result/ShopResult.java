package net.swordie.ms.world.shop.result;

import net.swordie.ms.connection.OutPacket;

public interface ShopResult {

    ShopResultType getType();

    void encode(OutPacket outPacket);
}
