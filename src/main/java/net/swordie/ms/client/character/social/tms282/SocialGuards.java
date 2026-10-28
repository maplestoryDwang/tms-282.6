package net.swordie.ms.client.character.social.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.enums.ChatType;

/**
 * Block-list checks for the social handlers (one call per hook, see the hook list in the report). Each method returns
 * true when the action must be refused and has already told the sender why (Traditional Chinese). The receiver is
 * never told, and the sender is not told that it is a block (same wording as "cannot receive").
 */
public final class SocialGuards {
    private SocialGuards() {
    }

    /** Whisper (ChatHandler.handleWhisper type 6). */
    public static boolean refuseWhisper(Char from, Char to) {
        if (!BlockListService.get().blocks(to, from)) {
            return false;
        }
        from.chatMessage(ChatType.Tip, String.format("%s 目前無法接收你的悄悄話。", to.getName()));
        return true;
    }

    /** Party invitation (PartyHandler Request_Invite). The hook sends the party message itself. */
    public static boolean refusePartyInvite(Char from, Char to) {
        return BlockListService.get().blocks(to, from);
    }

    /** Trade / RPS invitation (RoomHandler.handleMiniRoom TradeInviteRequest / RPSInviteRequest). */
    public static boolean refuseTradeInvite(Char from, Char to) {
        if (!BlockListService.get().blocks(to, from)) {
            return false;
        }
        from.chatMessage(ChatType.Tip, String.format("%s 拒絕了你的交易邀請。", to.getName()));
        return true;
    }

    /** Buddy request, guild invitation and other invitations: plain check. */
    public static boolean blocked(Char receiver, Char sender) {
        return BlockListService.get().blocks(receiver, sender);
    }
}
