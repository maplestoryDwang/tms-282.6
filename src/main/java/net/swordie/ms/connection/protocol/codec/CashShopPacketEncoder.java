package net.swordie.ms.connection.protocol.codec;

import net.swordie.ms.client.Account;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.enums.CashErrorType;
import net.swordie.ms.enums.CashItemType;
import net.swordie.ms.util.FileTime;
import net.swordie.ms.world.shop.cashshop.CashItemInfo;

import java.util.Optional;

/**
 * Codec family of the cash shop packets: SET_CASH_SHOP (the stage), CASH_SHOP_QUERY_CASH_RESULT (the balances) and
 * the CASH_SHOP_CASH_ITEM_RESULT kinds the 265 cash shop flow sends (locker, buy, locker moves, failures). The 265
 * builders ({@link net.swordie.ms.connection.packet.CCashShop}, {@code Stage.setCashShop}) ask {@link #active()} first
 * and keep their own 265 body as the fallback, so {@code MigrationHandler} / {@code CashShopHandler} send the
 * profile's packets without changing a call site. GMS 265 registers none.
 * <p>
 * Codec family of {@link net.swordie.ms.connection.protocol.ProfileCodecs}.
 */
public interface CashShopPacketEncoder {

    /** The cash shop stage for {@code chr} (its CharacterData, items loaded). */
    OutPacket setCashShop(Char chr);

    /** Prepaid NX, maple points, reward points of {@code chr}'s user / account. */
    OutPacket queryCashResult(Char chr);

    /** The whole locker (cash item storage) of {@code account}. */
    OutPacket loadLockerDone(Account account);

    /** A bought item now in the locker; {@code registerDate} / {@code receiveBonus} may be null. */
    OutPacket buyDone(CashItemType resultType, CashItemInfo item, FileTime registerDate, CashItemInfo receiveBonus);

    /** Locker -> inventory done: {@code item} is in the inventory at its bag index. */
    OutPacket moveLtoSDone(Item item);

    /** Inventory -> locker done: {@code item} is the new locker entry. */
    OutPacket moveStoLDone(CashItemInfo item);

    /** A failed request of the 265 result kind {@code type} (a *_Failed kind) with its reason. */
    OutPacket failed(CashItemType type, CashErrorType reason);

    /** 265 {@code CCashShop.error()}: the generic failure answer to an unknown request. */
    OutPacket error();

    /** The active profile's encoder (or its version's fallback); empty for GMS 265. */
    static Optional<CashShopPacketEncoder> active() {
        return ProfileEncoders.find(CashShopPacketEncoder.class);
    }
}
