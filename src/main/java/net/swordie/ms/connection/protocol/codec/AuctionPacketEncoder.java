package net.swordie.ms.connection.protocol.codec;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.world.shop.auctionhouse.AuctionItem;

import java.util.Optional;

/**
 * Codec family of the auction house packets (265 OutHeader.SET_AUCTION_FIELD / AUCTION_HOUSE_MSG / _ACTION / _EXIT;
 * TMS 282.3 LP02F0 / LP0A95 / LP0A96 / LP0A98). The 265 {@code AuctionHousePacket} builders keep their bodies and ask
 * the active encoder for the packet header and the item slot inside an auction entry, so a profile only registers its
 * own encoder: GMS 265 registers none and falls back to {@link Gms265AuctionPackets} (the unchanged 265 bytes);
 * TMS 282.3 registers {@code Tms282AuctionPackets}.
 */
public interface AuctionPacketEncoder {

    /** The auction house packets that share a header. */
    enum Kind {
        MSG, ACTION, EXIT
    }

    /** An empty packet of {@code kind}'s header. */
    OutPacket packet(Kind kind);

    /** The item slot of an auction entry (265 {@code Item.encode}). */
    void encodeItem(OutPacket out, Item item);

    /** The body of a wish-list removal after its type byte (265: the whole entry). */
    default void encodeWishListRemoval(OutPacket out, Char chr, AuctionItem item) {
        item.encode(out, chr, System.currentTimeMillis(), this::encodeItem);
    }

    /** SET_AUCTION_FIELD for {@code chr}; empty keeps the 265 {@code Stage.setAuctionField} body. */
    default Optional<OutPacket> setAuctionField(Char chr) {
        return Optional.empty();
    }

    /** The active profile's encoder (or its version's fallback), else the GMS 265 one. */
    static AuctionPacketEncoder active() {
        return ProfileEncoders.find(AuctionPacketEncoder.class).orElse(Gms265AuctionPackets.INSTANCE);
    }
}
