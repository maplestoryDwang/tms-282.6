package net.swordie.ms.client.character.quest;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.AccountQuest;
import net.swordie.ms.constants.QuestConstants;
import net.swordie.ms.enums.QuestStatus;
import net.swordie.ms.loaders.QuestData;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TMS 282.3 quest state on top of the 265 model ({@link Quest}, table {@code quests}). Wire formats live in
 * Tms282QuestRequest / Tms282QuestPackets; this class only decides what the server holds and writes it with the 265
 * persistence method {@link Quest#saveToSQL}.
 * <p>
 * Rule for CP01A6 act 1 (accept) and act 4 (opening script, answered without running the script): a quest the
 * character does not have yet is created with its modeled progress requirements. Started and Completed records
 * remain unchanged; NotStarted transitions preserve the owned row ID, QR/date data and timestamps. Account quests
 * belong exclusively to the Account quest map and quests_acc table; packet callers use a Quest view of that record.
 */
public final class Tms282QuestService {
    private Tms282QuestService() {
    }

    /** Result of {@link #markStarted}: the quest the character now holds and whether it was created by this call. */
    public record Started(Quest quest, boolean created, boolean accountQuest) {
        public Started {
            Objects.requireNonNull(quest, "quest");
        }

        public QuestStatus status() {
            return quest.getStatus();
        }
    }

    /** In memory: adds a Started quest when the character has none with this ID. */
    public static Started markStarted(Char chr, int questId) {
        Objects.requireNonNull(chr, "chr");
        if (questId <= 0) {
            throw new IllegalArgumentException("Quest ID must be positive");
        }
        if (QuestConstants.isAccountQuest(questId)) {
            if (chr.getAccount() == null) throw new IllegalStateException("Account quest requires an account owner");
            synchronized (chr.getAccount()) {
                AccountQuest existing = chr.getAccount().getQuestById(questId);
                if (existing != null && existing.getStatus() != QuestStatus.NotStarted) return new Started(view(existing), false, true);
                AccountQuest quest = QuestData.createAccQuestFromId(questId, chr.getAccount().getId());
                if (existing != null) {
                    quest.setId(existing.getId()); quest.setQrValue(existing.getQRValue());
                    quest.setCompletedTime(existing.getCompletedTime()); quest.setExpireTerm(existing.getExpireTerm());
                    quest.getProperties().putAll(existing.getProperties());
                }
                chr.getAccount().getQuests().put(questId, quest);
                return new Started(view(quest), true, true);
            }
        }
        Map<Integer, Quest> quests = chr.getQuests();
        if (quests == null) {
            quests = new ConcurrentHashMap<>();
            chr.setQuests(quests);
        }
        boolean account = QuestConstants.isAccountQuest(questId);
        Quest existing = quests.get(questId);
        if (existing != null && existing.getStatus() != QuestStatus.NotStarted) {
            return new Started(existing, false, account);
        }
        Quest quest = QuestData.createQuestFromId(questId, chr.getId());
        if (existing != null) {
            quest.setId(existing.getId()); quest.setQrValue(existing.getQRValue());
            quest.setCompletedTime(existing.getCompletedTime()); quest.setExpireTerm(existing.getExpireTerm());
            quest.getProperties().putAll(existing.getProperties());
        } else quest.setQrValue("");
        Quest raced = existing == null ? quests.putIfAbsent(questId, quest) : quests.replace(questId, existing, quest) ? null : quests.get(questId);
        return raced != null ? new Started(raced, false, account) : new Started(quest, true, account);
    }

    /** Packet/store view of the real owner's record; account quests never occupy the character quest map. */
    public static Quest held(Char chr, int questId) {
        if (QuestConstants.isAccountQuest(questId)) {
            AccountQuest q = chr.getAccount() == null ? null : chr.getAccount().getQuestById(questId);
            return q == null ? null : view(q);
        }
        return chr.getQuests() == null ? null : chr.getQuests().get(questId);
    }

    private static Quest view(AccountQuest account) {
        Quest q = new Quest(account.getAccId(), account.getQRKey(), account.getStatus());
        q.setId(account.getId()); q.setQrValue(account.getQRValue()); q.setCompletedTime(account.getCompletedTime());
        q.setProgressRequirements(account.getProgressRequirements());
        return q;
    }

    /**
     * Writes a quest created by {@link #markStarted} with 265 {@link Quest#saveToSQL} (one INSERT; the generated row
     * ID is stored after commit). Character and account records use their own tables. Unchanged records do not
     * write; SQL failure or a missing generated ID raises IllegalStateException before start rewards are granted.
     */
    public static boolean save(Char chr, Started started) {
        Objects.requireNonNull(chr, "chr");
        Objects.requireNonNull(started, "started");
        if (!started.created()) {
            return false;
        }
        Quest quest = started.quest();
        if (started.accountQuest()) {
            AccountQuest shared = chr.getAccount().getQuestById(quest.getQRKey());
            AccountQuest.saveToSQL(Map.of(shared.getQRKey(), shared), chr.getAccount().getId());
            quest.setId(shared.getId());
        } else Quest.saveToSQL(Map.of(quest.getQRKey(), quest), chr.getId());
        if (quest.getId() == 0) {
            throw new IllegalStateException("Quest " + quest.getQRKey() + " was not written (see the HIKARICP log)");
        }
        return true;
    }
}
