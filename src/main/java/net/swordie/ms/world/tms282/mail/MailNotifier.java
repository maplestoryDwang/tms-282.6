package net.swordie.ms.world.tms282.mail;

import net.swordie.ms.client.character.Char;

import java.util.List;

/**
 * How mail events reach the client (Strategy). The default {@link #CHAT} uses the in-field system chat, which
 * works on 282 today. A packet-based view (LP MEMO_RESULT 0x00AF / PARCEL_DLG) can replace it once those
 * packets are traced and verified; see {@link MailPacketsTodo}.
 */
public interface MailNotifier {

    /** Unread memos waiting at login / when one arrives while online. */
    void memosWaiting(Char chr, List<Memo> unread);

    /** Parcels ready to collect (login, quick delivery, a return). */
    void parcelsWaiting(Char chr, int count, boolean quickDelivery);

    /** Result text of a request. */
    void result(Char chr, String message);

    MailNotifier CHAT = new MailNotifier() {
        @Override
        public void memosWaiting(Char chr, List<Memo> unread) {
            if (unread.isEmpty()) return;
            chr.chatMessage("你有 " + unread.size() + " 則未讀的留言。輸入 @memo 查看。");
        }

        @Override
        public void parcelsWaiting(Char chr, int count, boolean quickDelivery) {
            if (count <= 0) return;
            chr.chatMessage((quickDelivery ? "【快速宅配】" : "【宅配】") + "你有 " + count
                    + " 件包裹可以領取，請找宅配人員篤伊或輸入 @parcel 查看。");
        }

        @Override
        public void result(Char chr, String message) {
            chr.chatMessage(message);
        }
    };
}
