package net.swordie.ms.client.character.emoticon;

import net.swordie.ms.client.character.Char;

/**
 * Script entry points (ScriptManagerImpl delegates here). Item scripts use the boolean result:
 * {@code if (stage.gainChatEmoticon(10030001)) { ...; ctx.consume(); } else { stage.dropAlertNotice("無法使用。"); }}
 * so true is returned only when the collection actually changed and the coupon may be consumed.
 */
public final class EmoticonScripts {
    private EmoticonScripts() {
    }

    public static boolean gainChatEmoticon(Char chr, int emoticonId) {
        return gainChatEmoticon(chr, emoticonId, 0);
    }

    /** {@code days} &lt;= 0 = permanent. */
    public static boolean gainChatEmoticon(Char chr, int emoticonId, int days) {
        EmoticonService.GrantResult r = EmoticonService.get().grant(chr, emoticonId,
                days > 0 ? days * 86_400_000L : 0);
        if (!r.changed() && chr != null) {
            chr.chatMessage(switch (r) {
                case ALREADY_OWNED -> "你已經擁有這個聊天表情。";
                case UNKNOWN -> "找不到這個聊天表情（" + emoticonId + "）。";
                default -> "聊天表情儲存失敗，請稍後再試。";
            });
        }
        return r.changed();
    }

    public static boolean gainChatEmoticonGroup(Char chr, int groupId) {
        int n = EmoticonService.get().grantGroup(chr, groupId);
        if (n <= 0 && chr != null) {
            chr.chatMessage(n == 0 ? "你已經擁有這組聊天表情的全部表情。"
                    : "這組聊天表情（" + groupId + "）尚未收錄或儲存失敗。");
        }
        return n > 0;
    }
}
