package net.swordie.ms.connection.packet;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.CharacterStat;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.protocol.codec.AuctionPacketEncoder;
import net.swordie.ms.util.FileTime;
import net.swordie.ms.world.shop.auctionhouse.AuctionItem;

import java.nio.charset.Charset;
import java.util.Objects;
import java.util.Optional;

/**
 * TMS 282.3 auction house encoder ({@link AuctionPacketEncoder}); the 265 {@code AuctionHousePacket} bodies are kept.
 * <ul>
 *     <li>LP02F0 SET_AUCTION_FIELD: CStage handler 44BC460 reads {@code FT} (44BC498 DecodeBuf 8) and then the
 *     CharacterData of the first SetField entry (44BC4F4 -> 92AD20), nothing more: traced CLEAN with the starter and
 *     coverage characters (research/tms2823/auction-trace) [證實];</li>
 *     <li>CAuctionHouse::OnPacket 2922090 (sub edx, 0xA95): LP0A95 -> 2CA9630 (265 AUCTION_HOUSE_MSG, u8 u8),
 *     LP0A96 -> 2CAC210 (265 AUCTION_HOUSE_ACTION, u8 type then a 0x3D-case switch), LP0A97 -> 2CB6E20 (265
 *     AUCTION_HOUSE_SEARCH), LP0A98 -> 2CA9750 (265 AUCTION_HOUSE_EXIT, no body) [dispatch 證實, same order as 265
 *     2659..2662];</li>
 *     <li>the item slot of an entry is the 282 GW_ItemSlotBase ({@link Tms282ItemEncoder}).</li>
 * </ul>
 */
public final class Tms282AuctionPackets implements AuctionPacketEncoder {
    public static final int SET_AUCTION_FIELD = 0x02F0;
    public static final int AUCTION_MSG = 0x0A95;
    public static final int AUCTION_ACTION = 0x0A96;
    public static final int AUCTION_SEARCH = 0x0A97;
    public static final int AUCTION_EXIT = 0x0A98;

    private final Charset charset;

    public Tms282AuctionPackets(Charset charset) {
        this.charset = Objects.requireNonNull(charset, "charset");
    }

    @Override
    public OutPacket packet(Kind kind) {
        return Tms282LoginPacketSupport.packet(switch (kind) {
            case MSG -> AUCTION_MSG;
            case ACTION -> AUCTION_ACTION;
            case EXIT -> AUCTION_EXIT;
        });
    }

    @Override
    public void encodeItem(OutPacket out, Item item) {
        Tms282ItemEncoder.encode(out, item, charset);
    }

    /** Case 26 (2CAC691): {@code i64 id, i32 world} and the entry is removed from the list; nothing else is read. */
    @Override
    public void encodeWishListRemoval(OutPacket out, Char chr, AuctionItem item) {
        out.encodeLong(item.getId());
        out.encodeInt(0);                                    // world id, as the first i32 of the 265 entry
    }

    @Override
    public Optional<OutPacket> setAuctionField(Char chr) {
        Objects.requireNonNull(chr, "chr");
        CharacterStat stat = Objects.requireNonNull(chr.getAvatarData(), "avatar data").getCharacterStat();
        Tms282StagePackets.InventorySlots slots = Tms282StagePackets.InventorySlots.of(chr);
        return Optional.of(setAuctionField(stat, slots, Tms282StagePackets.Inventories.of(chr), FileTime.currentTime()));
    }

    /** LP02F0 from a stat and its items (offline fixtures use this one). */
    public OutPacket setAuctionField(CharacterStat stat, Tms282StagePackets.InventorySlots slots,
                                     Tms282StagePackets.Inventories items, FileTime now) {
        OutPacket out = Tms282LoginPacketSupport.packet(SET_AUCTION_FIELD);
        out.encodeFT(now);                                   // 44BC498 DecodeBuf(8)
        Tms282StagePackets.encodeCharacterData(out, stat, slots,
                new Tms282StagePackets.ItemSlotBlocks(items, slots), charset); // 44BC4F4 -> 92AD20, as LP02F1
        return out;
    }
}
