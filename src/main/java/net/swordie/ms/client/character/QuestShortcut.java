package net.swordie.ms.client.character;

import net.swordie.ms.client.AccountQuest;
import net.swordie.ms.client.character.quest.Quest;
import net.swordie.ms.client.character.quest.reward.*;
import net.swordie.ms.client.character.tms282.Tms282ChangeWatch;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;
import net.swordie.ms.constants.QuestConstants;
import net.swordie.ms.enums.QuestStatus;
import net.swordie.ms.handlers.login.tms282.Tms282LoginSettings;
import net.swordie.ms.loaders.QuestData;
import net.swordie.ms.loaders.containerclasses.QuestInfo;
import net.swordie.ms.scripts.ScriptManagerImpl;
import net.swordie.ms.scripts.ScriptType;
import net.swordie.ms.util.FileTime;
import java.sql.Statement;
import java.util.*;
import java.util.function.ToLongBiFunction;
import java.util.function.BiConsumer;

/** Explicit player-selected quest shortcuts. Normal NPC play remains available. */
public final class QuestShortcut {
    private static final String RECEIPT = "quest-shortcut:v1";
    public enum Result { COMPLETED, NOT_STARTED, ALREADY_CLAIMED, NPC_REQUIRED, BUSY, INVENTORY_FULL, STORAGE_FAILED, REWARD_FAILED }
    private QuestShortcut() { }
    public static boolean enabled() { return Protocol.active().version() == ProtocolVersion.TMS_282_3; }
    public static Object monitor(Char chr) { return chr.getAccount() == null ? chr : chr.getAccount(); }

    /** Negative QR keys are internal durable receipts, excluded from all client quest lists. */
    public static boolean wasClaimed(Char chr, int id) {
        if (id <= 0) return false;
        if (QuestConstants.isAccountQuest(id)) {
            AccountQuest q = chr.getAccount().getQuestById(-id);
            return q != null && RECEIPT.equals(q.getQRValue());
        }
        Quest q = chr.getQuestById(-id);
        return q != null && RECEIPT.equals(q.getQRValue());
    }

    public static List<QuestInfo> available(Char chr) {
        if (!enabled()) return List.of();
        synchronized (monitor(chr)) {
            Set<Integer> ids = new TreeSet<>();
            for (Quest q : chr.getQuestsInProgress()) ids.add(q.getQRKey());
            if (chr.getAccount() != null) for (AccountQuest q : chr.getAccount().getQuests().values()) {
                if (q.getQRKey() > 0 && q.getStatus() == QuestStatus.Started) ids.add(q.getQRKey());
            }
            List<QuestInfo> result = new ArrayList<>();
            for (int id : ids) {
                QuestInfo qi = QuestData.getQuestInfoById(id);
                if (qi != null && !qi.isSecret() && qi.getQuestName() != null && !qi.getQuestName().isBlank()
                        && chr.hasQuestInProgress(id) && !wasClaimed(chr, id)) result.add(qi);
            }
            return result;
        }
    }

    /**
     * Exact header token of an end script that only plays Say dialog and calls completeQuest(parentID) once: such a
     * script is treated as absent by {@link #canSkip}. Must be one of the first {@link #MARKER_LINES} lines.
     */
    public static final String DIALOG_ONLY_MARKER = "# tms282: dialog-only-complete";
    static final int MARKER_LINES = 5;

    public static boolean canSkip(int id) {
        return canSkip(id, script -> ScriptManagerImpl.scriptFilePath(ScriptType.Quest, script));
    }

    /** @param endScriptFile resolves an end-script name to its file, null when there is none */
    static boolean canSkip(int id, java.util.function.Function<String, java.nio.file.Path> endScriptFile) {
        // Completion includes guild-castle side effects; its compatibility binding is not implemented.
        if (id == 66669) return false;
        QuestInfo qi = id > 0 ? QuestData.getQuestInfoById(id) : null;
        if (!enabled() || qi == null || qi.isSecret() || qi.getQuestName() == null || qi.getQuestName().isBlank()) return false;
        String script = qi.getEndScript();
        boolean declared = script != null && !script.isBlank();
        if (script == null || script.isBlank()) script = "q" + id + "e";
        // Job changes and script-only/choice rewards must not be silently lost; a dialog-only-complete end
        // script counts as no script.
        java.nio.file.Path file = endScriptFile.apply(script);
        return file == null ? !declared : isDialogOnlyComplete(file);
    }

    /** True when one of the first {@link #MARKER_LINES} lines of {@code file} is exactly {@link #DIALOG_ONLY_MARKER}. */
    static boolean isDialogOnlyComplete(java.nio.file.Path file) {
        if (file == null || !file.getFileName().toString().endsWith(".py")) return false;
        try (var reader = java.nio.file.Files.newBufferedReader(file, java.nio.charset.StandardCharsets.UTF_8)) {
            for (int i = 0; i < MARKER_LINES; i++) {
                String line = reader.readLine();
                if (line == null) return false;
                if (i == 0 && line.startsWith("\uFEFF")) line = line.substring(1);
                if (line.strip().equals(DIALOG_ONLY_MARKER)) return true;
            }
            return false;
        } catch (java.io.IOException | RuntimeException e) {
            return false; // unreadable: keep the NPC requirement
        }
    }

    public static Result complete(Char chr, int id) { return complete(chr, id, QuestShortcut::reserve, QuestShortcut::persist); }

    static Result complete(Char chr, int id, ToLongBiFunction<Char, Integer> receipts, BiConsumer<Char, Integer> persist) {
        if (!enabled() || id <= 0) return Result.NOT_STARTED;
        synchronized (monitor(chr)) {
            if (wasClaimed(chr, id)) return Result.ALREADY_CLAIMED;
            if (!chr.hasQuestInProgress(id)) return Result.NOT_STARTED;
            if (!canSkip(id)) return Result.NPC_REQUIRED;
            if (chr.getScriptManager().isActive(ScriptType.Quest)) return Result.BUSY;
            if (!canReceiveItems(chr, QuestData.getQuestInfoById(id))) return Result.INVENTORY_FULL;
            Tms282ChangeWatch changes = Tms282ChangeWatch.of(chr);
            try {
                long receiptId = receipts.applyAsLong(chr, id);
                if (receiptId == 0) return Result.ALREADY_CLAIMED;
                cacheReceipt(chr, id, Math.abs(receiptId));
                if (receiptId < 0) return Result.ALREADY_CLAIMED;
            } catch (RuntimeException e) {
                System.out.printf("[QuestShortcut] reservation failed char=%d quest=%d: %s%n", chr.getId(), id, e);
                return Result.STORAGE_FAILED; // no reward is applied before the durable receipt
            }
            try {
                chr.completeQuestInternal(id, false, true);
                persist.accept(chr, id);
                // The legacy account-quest message has no 282 codec; send the verified completion record.
                chr.write(net.swordie.ms.connection.packet.Tms282QuestPackets.questRecordCompleted(id,
                        net.swordie.ms.connection.packet.Tms282QuestPackets.fileTimeOf(System.currentTimeMillis())));
                for (var packet : changes.replies(Tms282LoginSettings.load().nameCharset())) chr.write(packet);
                return Result.COMPLETED;
            } catch (RuntimeException e) {
                System.out.printf("[QuestShortcut] reward needs review char=%d quest=%d: %s%n", chr.getId(), id, e);
                // Keep the receipt if a partial reward was applied. Retrying must never duplicate it.
                return Result.REWARD_FAILED;
            }
        }
    }

    private static void persist(Char chr, int id) {
        chr.saveToSQL(); // existing character/inventory/quest persistence, no schema change
        if (QuestConstants.isAccountQuest(id)) AccountQuest.saveToSQL(chr.getAccount().getQuests(), chr.getAccount().getId());
    }

    public static boolean isSkippedCost(QuestReward reward) {
        return reward instanceof QuestItemReward item && (item.getStatus() == 0 || item.getQuantity() < 0)
                || reward instanceof QuestBuffItemReward buff && buff.getStatus() == 0
                || reward instanceof QuestMoneyReward money && money.getMoney() < 0;
    }

    static boolean canReceiveItems(Char chr, QuestInfo qi) {
        var items = new ArrayList<net.swordie.ms.client.character.items.Item>();
        var slots = new EnumMap<net.swordie.ms.enums.InvType, Integer>(net.swordie.ms.enums.InvType.class);
        // Reserve room for every possible item, including random alternatives, before consuming the receipt.
        for (QuestReward reward : qi.getQuestRewards()) {
            if (!(reward instanceof QuestItemReward item) || isSkippedCost(item) || item.getQuantity() == 0) continue;
            var copy = net.swordie.ms.loaders.ItemData.getItemDeepCopy(item.getId());
            if (copy == null) return false;
            copy.setQuantity(item.getQuantity());
            items.add(copy);
            slots.merge(copy.getInvType(), 1, Integer::sum);
        }
        if (qi.getMedalItemId() != 0) {
            var medal = net.swordie.ms.loaders.ItemData.getItemDeepCopy(qi.getMedalItemId());
            if (medal == null) return false;
            medal.setQuantity(1); items.add(medal);
            slots.merge(medal.getInvType(), 1, Integer::sum);
        }
        // The existing reward giver needs an empty slot even when a stack could be merged.
        for (var slot : slots.entrySet()) {
            var bag = chr.getInventoryByType(slot.getKey());
            if (bag == null || bag.getEmptySlots() < slot.getValue()) return false;
        }
        return chr.canHold(items);
    }

    private static void cacheReceipt(Char chr, int id, long rowId) {
        if (QuestConstants.isAccountQuest(id)) {
            AccountQuest q = new AccountQuest(chr.getAccount().getId(), -id, QuestStatus.Completed);
            q.setId(rowId); q.setQrValue(RECEIPT); q.setCompletedTime(FileTime.currentTime());
            chr.getAccount().getQuests().put(-id, q);
        } else {
            Quest q = new Quest(chr.getId(), -id, QuestStatus.Completed);
            q.setId(rowId); q.setQrValue(RECEIPT); q.setCompletedTime(FileTime.currentTime());
            chr.getQuests().put(-id, q);
        }
    }

    /** Positive ID = reserved now, negative ID = existing receipt. Account/character lock serializes claims. */
    private static long reserve(Char chr, Integer questId) {
        boolean account = QuestConstants.isAccountQuest(questId);
        String table = account ? "quests_acc" : "quests", ownerColumn = account ? "accid" : "charid";
        int owner = account ? chr.getAccount().getId() : chr.getId();
        try (var con = DatabaseManager.getConnection()) {
            try (var find = con.prepareStatement("SELECT id FROM " + table + " WHERE " + ownerColumn + "=? AND qrkey=? AND qrvalue=? LIMIT 1")) {
                find.setInt(1, owner); find.setInt(2, -questId); find.setString(3, RECEIPT);
                try (var rows = find.executeQuery()) { if (rows.next()) return -rows.getLong(1); }
            }
            // Pool connections use autocommit; explicitly require it before issuing an irreversible reward.
            con.setAutoCommit(true);
            try (var insert = con.prepareStatement("INSERT INTO " + table + " (" + ownerColumn
                    + ",qrkey,qrvalue,status,completedtime,expireterm) VALUES (?,?,?,2,CURRENT_TIMESTAMP(3),NULL)", Statement.RETURN_GENERATED_KEYS)) {
                insert.setInt(1, owner); insert.setInt(2, -questId); insert.setString(3, RECEIPT);
                if (insert.executeUpdate() != 1) throw new IllegalStateException("Receipt not written");
                try (var key = insert.getGeneratedKeys()) {
                    if (!key.next() || key.getLong(1) <= 0) throw new IllegalStateException("Receipt ID missing");
                    return key.getLong(1);
                }
            }
        } catch (java.sql.SQLException e) { throw new IllegalStateException("Quest shortcut receipt unavailable", e); }
    }
}
