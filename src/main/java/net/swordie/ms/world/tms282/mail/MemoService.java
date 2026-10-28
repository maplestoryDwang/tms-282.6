package net.swordie.ms.world.tms282.mail;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.util.DataPrinter;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Offline notes: send to any character of the same world (online or not), list, read, delete, login notice.
 * Transport-agnostic: callers are the chat commands, the Duey NPC script and (later) the CP memo request.
 */
public final class MemoService {
    public static final int MAX_CONTENT = 200;
    public static final int MAX_STORED_PER_RECEIVER = 100;
    static final long SEND_COOLDOWN_MS = 3_000;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("MM/dd HH:mm");
    private static final MemoService INSTANCE = new MemoService(MailNotifier.CHAT);

    private final MailNotifier notifier;
    private final Map<Integer, Long> lastSend = new ConcurrentHashMap<>();

    public MemoService(MailNotifier notifier) {
        this.notifier = notifier;
    }

    public static MemoService get() {
        return INSTANCE;
    }

    public MailNotifier notifier() {
        return notifier;
    }

    public MailResult send(Char sender, String receiverName, String content) {
        if (sender == null) return MailResult.fail("無法傳送留言。");
        String text = content == null ? "" : content.strip();
        String name = receiverName == null ? "" : receiverName.strip();
        if (name.isEmpty()) return MailResult.fail("請輸入收件角色名稱。");
        if (text.isEmpty()) return MailResult.fail("留言內容不可空白。");
        if (text.length() > MAX_CONTENT) return MailResult.fail("留言內容最多 " + MAX_CONTENT + " 個字。");
        if (name.equalsIgnoreCase(sender.getName())) return MailResult.fail("不能留言給自己。");
        long now = System.currentTimeMillis();
        Long last = lastSend.get(sender.getId());
        if (last != null && now - last < SEND_COOLDOWN_MS) return MailResult.fail("留言傳送太頻繁，請稍後再試。");
        try (Connection c = MailSchema.connection()) {
            Optional<MailRecipient> found = MailRecipient.byName(c, name);
            if (found.isEmpty()) return MailResult.fail("找不到角色「" + name + "」。");
            MailRecipient r = found.get();
            if (r.worldId() != MailRecipient.worldIdOf(sender)) return MailResult.fail("只能留言給同一個世界的角色。");
            if (MemoStore.countFor(c, r.characterId()) >= MAX_STORED_PER_RECEIVER) {
                return MailResult.fail("「" + r.name() + "」的留言箱已滿。");
            }
            Memo memo = new Memo(0, 0, Memo.UNREAD, sender.getId(), sender.getAccId(), sender.getName(),
                    LocalDateTime.now(), r.characterId(), r.name(), false, text);
            int id = MemoStore.insert(c, memo);
            lastSend.put(sender.getId(), now);
            r.online().ifPresent(online -> notifier.memosWaiting(online, unreadSafe(online)));
            DataPrinter.send(DataPrinter.ITEM, String.format("memo %d %s(%d) -> %s(%d)", id, sender.getName(),
                    sender.getId(), r.name(), r.characterId()), true);
            return MailResult.ok("已留言給「" + r.name() + "」。");
        } catch (SQLException e) {
            DataPrinter.send(DataPrinter.HIKARICP_ERROR, e);
            return MailResult.fail("留言傳送失敗，請稍後再試。");
        }
    }

    public List<Memo> list(Char chr, boolean unreadOnly) throws SQLException {
        try (Connection c = MailSchema.connection()) {
            return MemoStore.listFor(c, chr.getId(), unreadOnly);
        }
    }

    /** Text listing for chat / NPC. */
    public MailResult listText(Char chr) {
        try {
            List<Memo> all = list(chr, false);
            if (all.isEmpty()) return MailResult.ok("沒有任何留言。");
            StringBuilder sb = new StringBuilder("留言列表（" + all.size() + "）：");
            for (Memo m : all) {
                sb.append("\r\n").append(m.unread() ? "[新] " : "").append('#').append(m.id()).append(' ')
                        .append(m.senderName()).append(' ').append(TIME.format(m.sentTime()));
            }
            return MailResult.ok(sb.toString());
        } catch (SQLException e) {
            DataPrinter.send(DataPrinter.HIKARICP_ERROR, e);
            return MailResult.fail("讀取留言失敗。");
        }
    }

    public MailResult read(Char chr, int memoId) {
        try (Connection c = MailSchema.connection()) {
            Optional<Memo> m = MemoStore.get(c, memoId, chr.getId());
            if (m.isEmpty()) return MailResult.fail("找不到這則留言。");
            if (m.get().unread() && MemoStore.markRead(c, memoId, chr.getId())) {
                MemoStore.record(c, memoId, Memo.READ, m.get());
            }
            Memo memo = m.get();
            return MailResult.ok("來自「" + memo.senderName() + "」（" + TIME.format(memo.sentTime()) + "）：" + memo.content());
        } catch (SQLException e) {
            DataPrinter.send(DataPrinter.HIKARICP_ERROR, e);
            return MailResult.fail("讀取留言失敗。");
        }
    }

    public MailResult delete(Char chr, int memoId) {
        try (Connection c = MailSchema.connection()) {
            Optional<Memo> m = MemoStore.get(c, memoId, chr.getId());
            if (m.isEmpty() || !MemoStore.delete(c, memoId, chr.getId())) return MailResult.fail("找不到這則留言。");
            MemoStore.record(c, memoId, Memo.DELETED, m.get());
            return MailResult.ok("已刪除留言 #" + memoId + "。");
        } catch (SQLException e) {
            DataPrinter.send(DataPrinter.HIKARICP_ERROR, e);
            return MailResult.fail("刪除留言失敗。");
        }
    }

    /** Field-entry notice of unread memos. */
    public void onEnterField(Char chr) {
        notifier.memosWaiting(chr, unreadSafe(chr));
    }

    private List<Memo> unreadSafe(Char chr) {
        try {
            return list(chr, true);
        } catch (SQLException e) {
            DataPrinter.send(DataPrinter.HIKARICP_ERROR, e);
            return List.of();
        }
    }
}
