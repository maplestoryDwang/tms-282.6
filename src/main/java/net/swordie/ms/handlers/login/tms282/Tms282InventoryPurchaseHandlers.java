package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.character.tms282.Tms282CashExpansionService;
import net.swordie.ms.connection.packet.Tms282InventoryPurchasePackets;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.loaders.CashShopData;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Inventory-window "+" slot purchase (Tms282InventoryPurchasePackets): CP00AF balance query -> LP01C9,
 * CP00B0 purchase -> capacity then debit (Tms282CashExpansionService) -> LP01CA. Every refusal still answers, so the
 * modal dialog never stays pending; nothing here ends the session.
 */
public final class Tms282InventoryPurchaseHandlers {
    private final Tms282CashExpansionService service;

    Tms282InventoryPurchaseHandlers() {
        this(new Tms282CashExpansionService());
    }

    public Tms282InventoryPurchaseHandlers(Tms282CashExpansionService service) {
        this.service = service;
    }

    /**
     * The on-sale 282 commodity (Etc/Commodity 1102000xx) of each dialog item. CP00B0's fourth i32 ([+0x291C]) is not a
     * commodity SN (live 2026-09-28: 50300358 for 9112001, no such SN), so the price comes from the item id.
     */
    static final java.util.Map<Integer, Integer> DIALOG_SN = java.util.Map.of(9110002, 110200010, 9111001, 110200006,
            9112001, 110200007, 9113002, 110200008, 9114001, 110200009);

    static net.swordie.ms.loaders.Etc.Commodity.CommodityInfo commodity(int itemId, int sn) {
        var bySn = CashShopData.getCommodityBySN(sn);
        if (bySn != null && bySn.getItemId() == itemId) {
            return bySn;
        }
        Integer own = DIALOG_SN.get(itemId);
        return own == null ? null : CashShopData.getCommodityBySN(own);
    }

    void register(Tms282Routes routes) {
        routes.channel(0x00AF, InHeader.CP00AF).when(Tms282Guards.IN_FIELD)
                .handle("inventory slot purchase: balance query -> LP01C9", this::onBalance);
        routes.channel(0x00B0, InHeader.CP00B0).when(Tms282Guards.IN_FIELD)
                .handle("inventory slot purchase -> LP01CA", this::onPurchase);
    }

    public void onBalance(Tms282LoginState session, int op, byte[] packet) {
        var chr = session.fieldCharacter();
        if (chr == null || chr.getUser() == null) {
            return;
        }
        var user = chr.getUser();
        session.send(Tms282InventoryPurchasePackets.balance(user.getNxPrepaid(), user.getMaplePoints()));
        System.out.printf("[TMS282] CP00AF slot-purchase balance character=%d cash=%d mileage=%d -> LP01C9%n",
                chr.getId(), user.getNxPrepaid(), user.getMaplePoints());
    }

    public void onPurchase(Tms282LoginState session, int op, byte[] packet) {
        var chr = session.fieldCharacter();
        if (chr == null || chr.getUser() == null) {
            return;
        }
        var user = chr.getUser();
        int itemId = 0;
        int result = Tms282InventoryPurchasePackets.FAILED;
        String outcome;
        try {
            if (packet.length != 19) {
                throw new IllegalArgumentException("CP00B0 length " + packet.length);
            }
            ByteBuffer in = ByteBuffer.wrap(packet, 2, 17).order(ByteOrder.LITTLE_ENDIAN);
            int accountId = in.getInt(), characterId = in.getInt();
            itemId = in.getInt();
            int sn = in.getInt(), currency = Byte.toUnsignedInt(in.get());
            if (characterId != chr.getId() || accountId != user.getId()) { // the client's account id is the user id (CP00AF 09000000)
                throw new IllegalArgumentException("CP00B0 owner " + accountId + "/" + characterId);
            }
            var bought = service.buyFromInventoryDialog(chr, itemId, commodity(itemId, sn), currency);
            result = Tms282InventoryPurchasePackets.OK; // the capacity is saved: the client adds the same 8 slots
            outcome = "item=" + itemId + " sn=" + sn + " currency=" + currency + " slots=" + bought.slots()
                    + (bought.paid() ? " paid" : " NOT PAID (debit failed; capacity kept)");
        } catch (RuntimeException e) {
            outcome = "refused " + e.getClass().getSimpleName() + ": " + e.getMessage();
        }
        session.send(Tms282InventoryPurchasePackets.result(itemId, user.getNxPrepaid(), user.getMaplePoints(), result));
        System.out.printf("[TMS282] CP00B0 slot purchase character=%d %s -> LP01CA result=%d%n", chr.getId(), outcome, result);
        if (Tms282CashExpansionService.isBalanceUncertain(user)) {
            session.client().close();
        }
    }
}
