package net.swordie.ms.world.tms282.mail;

import java.time.LocalDateTime;

/**
 * One offline note (tms282_memo row).
 *
 * @param flag  memo flag (0 = normal note; kept for the client's MEMO_RESULT flag byte)
 * @param state {@link #UNREAD} or {@link #READ}
 */
public record Memo(int id, int flag, int state, int senderCharacterId, int senderAccountId, String senderName,
                   LocalDateTime sentTime, int receiverId, String receiverName, boolean accountMemo, String content) {
    public static final int UNREAD = 0;
    public static final int READ = 1;
    /** memo_record.state for a deleted memo. */
    public static final int DELETED = 2;

    public boolean unread() {
        return state == UNREAD;
    }
}
