package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.Tms282NickItemRequest;
import net.swordie.ms.enums.InvType;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.handlers.item.ItemHandler;
import net.swordie.ms.loaders.ItemData;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.loaders.containerclasses.ItemInfo;

/**
 * CP0120 title activation ({@link Tms282NickItemRequest}) on the channel socket, in field -> 265
 * {@link ItemHandler#handleUserActivateNickItem}: nickSkill stats (IndiePAD/MAD/PDD/AllStat/MHP/MMP/BDR/
 * IgnoreMobpdpR/EXP/Arc/Aut, all in the 282 CTS table) as LP009E, the item's title flag (inventory update), and
 * REMOTE_SET_ACTIVE_NICK_ITEM LP0485 to the others (282 handler 5456F80 reads i32 item, u8, [str] after the
 * pool's character id = 265 layout). 265 NICK_SKILL_EXPIRED has no 282 opcode and is dropped by the guard.
 * Before this route CP0120 was logged as an unknown report ({@code Tms282FieldReport}).
 * <p>Checks before the 265 logic (which dereferences the item and the skill data without checks): the item must
 * be in the install slot the client names, not expired, with item info, and a nickSkill with server skill data.
 * {@code [0, x]} while no title is worn (the client sends {@code [0, 0]} after field entry) is ignored.
 * Opcode status: CP0120 IMPLEMENTED_UNTESTED.
 */
final class Tms282TitleHandlers {

    void register(Tms282Routes routes) {
        routes.channel(Tms282NickItemRequest.OPCODE, InHeader.CP0120).when(Tms282Guards.IN_FIELD)
                .handle("title on/off -> 265 ItemHandler nick item (LP009E stats, LP0485 to others)", this::nickItem);
    }

    private void nickItem(Tms282LoginState session, int op, byte[] packet) {
        Tms282NickItemRequest request;
        try {
            request = Tms282NickItemRequest.parse(packet);
        } catch (IllegalArgumentException e) {
            Tms282SocialHandlers.rejected(op, packet, e);
            return;
        }
        Char chr = session.fieldCharacter();
        String refusal = refusal(chr, request);
        if (refusal != null) {
            System.out.printf("[TMS282] CP0120 title %d slot %d ignored: %s%n", request.itemId(), request.slot(), refusal);
            return;
        }
        System.out.printf("[TMS282] CP0120 title %d slot %d%n", request.itemId(), request.slot());
        Tms282SocialHandlers.guarded(chr, op,
                () -> ItemHandler.handleUserActivateNickItem(chr, new InPacket(request.toGms265Body())));
    }

    /** Why the request is not passed to the 265 logic, or null. */
    static String refusal(Char chr, Tms282NickItemRequest request) {
        if (request.itemId() == 0) {
            return chr.getActiveNickItemID() == 0 ? "no title worn (field-entry sync)" : null;
        }
        Item item = chr.getInventoryByType(InvType.INSTALL).getItemBySlot(request.slot());
        if (item == null || item.getItemId() != request.itemId()) {
            return "not in install slot";
        }
        if (item.getDateExpire() != null && item.getDateExpire().isExpired()) {
            return "expired";
        }
        ItemInfo ii = ItemData.getItemInfoByID(request.itemId());
        if (ii == null) {
            return "no item info";
        }
        if (ii.getNickSkill() != 0 && SkillData.getSkillInfoById(ii.getNickSkill()) == null) {
            return "no skill data for nickSkill " + ii.getNickSkill();
        }
        return null;
    }
}
