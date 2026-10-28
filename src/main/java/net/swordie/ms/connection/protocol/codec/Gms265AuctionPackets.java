package net.swordie.ms.connection.protocol.codec;

import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.handlers.header.OutHeader;

/** The GMS 265 auction house headers and item slot: what {@code AuctionHousePacket} wrote before, unchanged. */
public enum Gms265AuctionPackets implements AuctionPacketEncoder {
    INSTANCE;

    @Override
    public OutPacket packet(Kind kind) {
        return new OutPacket(switch (kind) {
            case MSG -> OutHeader.AUCTION_HOUSE_MSG;
            case ACTION -> OutHeader.AUCTION_HOUSE_ACTION;
            case EXIT -> OutHeader.AUCTION_HOUSE_EXIT;
        });
    }

    @Override
    public void encodeItem(OutPacket out, Item item) {
        item.encode(out);
    }
}
