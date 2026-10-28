package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.ServerConstants;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.quest.Quest;
import net.swordie.ms.client.character.quest.Tms282QuestService;
import net.swordie.ms.client.character.quest.reward.QuestBuffItemReward;
import net.swordie.ms.client.character.quest.reward.QuestItemReward;
import net.swordie.ms.client.character.quest.reward.QuestReward;
import net.swordie.ms.client.character.tms282.Tms282ChangeWatch;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.packet.Tms282QuestPackets;
import net.swordie.ms.connection.packet.Tms282QuestRequest;
import net.swordie.ms.constants.QuestConstants;
import net.swordie.ms.enums.QuestStatus;
import net.swordie.ms.handlers.script.QuestHandler;
import net.swordie.ms.loaders.QuestData;
import net.swordie.ms.loaders.containerclasses.QuestInfo;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.scripts.ScriptManagerImpl;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The TMS 282.3 quest flow of CP01A6 (NPC-driven and automatic quests) on top of the 265 quest model and the 282
 * quest data ({@code data/dat282/quests}, the whole 282 Quest.wz):
 * <ul>
 * <li><b>accept</b> (act 1, and act 4 when the quest has no script): the Check-0 date window is enforced, the quest is
 * created with its 265 progress requirements ({@link QuestData#createQuestFromId}, so mob/item counts work) and
 * written; LP00AE Started (+ LP04D0 act 12 with tms282.questResult). A quest whose QuestInfo has
 * {@code autoComplete}/{@code autoPreComplete} and whose end requirements are already met is completed at once.</li>
 * <li><b>complete</b> (act 2, and act 5 without a script): only a held, Started quest whose 265 progress requirements
 * are met; 265 {@code Char.completeQuest} gives the rewards (its QuestRecord message reaches the client as LP00AE
 * through the legacy translator), the row is written, and the bag/stat changes go out as LP008F/LP009D
 * ({@link Tms282ChangeWatch}). A repeated request for a completed quest re-sends the completed record only.</li>
 * <li><b>scripts</b> (acts 4/5 with a quest script, 6 later step): the 265 QuestHandler with the 265 body; the
 * scripts' dialogs are LP0960 through the script codec.</li>
 * <li><b>resign</b> (act 3): only a Started character quest; LP00AE NotStarted, row deleted through the store.</li>
 * <li><b>field entry sweep</b>: Started quests with {@code autoComplete} and no end requirement at all (the 282 event
 * trackers 504755..504759 that stayed Started before act 2 was handled) are completed without rewards, once.</li>
 * </ul>
 * The client evaluates the 282 Check conditions the 265 model does not know (runeAct, dayByDay, ...); the server
 * never auto-completes a quest on such a condition, it only answers the client's act 2.
 */
final class Tms282QuestFlow {
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    /** What a request did (for the log and the checks). */
    enum Outcome {
        STARTED, ALREADY_HELD, OUT_OF_DATE, COMPLETED, ALREADY_COMPLETED, NOT_READY, NOT_HELD, RESIGNED, SCRIPT, PASSED_TO_265
    }

    record Result(Outcome outcome, int questId, String detail) {
        @Override
        public String toString() {
            return outcome + " quest=" + questId + (detail.isEmpty() ? "" : " (" + detail + ")");
        }
    }

    private final Tms282FieldStore store;
    private final Charset charset;
    private final boolean questResult;
    private final Supplier<LocalDateTime> clock;

    Tms282QuestFlow(Tms282FieldStore store, Charset charset, boolean questResult, Supplier<LocalDateTime> clock) {
        this.store = Objects.requireNonNull(store, "store");
        this.charset = Objects.requireNonNull(charset, "charset");
        this.questResult = questResult;
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /** One CP01A6 request; replies go to {@code send}, 265 side effects write through the character. */
    List<Result> handle(Char chr, Tms282QuestRequest request, Consumer<OutPacket> send) {
        synchronized (net.swordie.ms.client.character.QuestShortcut.monitor(chr)) {
            return handleLocked(chr, request, send);
        }
    }

    private List<Result> handleLocked(Char chr, Tms282QuestRequest request, Consumer<OutPacket> send) {
        List<Result> out = new ArrayList<>(2);
        switch (request.payload()) {
            case Tms282QuestRequest.Accept a -> accept(chr, a.questId(), a.npcId(), send, out);
            case Tms282QuestRequest.Complete c -> out.add(complete(chr, c.questId(), c.npcId(), send, "act 2"));
            case Tms282QuestRequest.OpeningScript o -> {
                Quest held = Tms282QuestService.held(chr, o.questId());
                QuestInfo info = QuestData.getQuestInfoById(o.questId());
                if (info == null || ((held == null || held.getStatus() == QuestStatus.NotStarted) && (!inDateWindow(info) || !chr.canStartQuest(o.questId())))) {
                    out.add(new Result(Outcome.NOT_READY, o.questId(), "opening script requires known eligible quest data"));
                    break;
                }
                // These one-time ability scripts do nothing once completed. Re-send the missing client
                // record instead of queueing that no-op and its delayed, unrelated request unlock.
                if (o.questId() >= 12394 && o.questId() <= 12396
                        && held != null && held.getStatus() == QuestStatus.Completed) {
                    send.accept(Tms282FieldHandlers.questRecord(o.questId(), held, charset));
                    chr.dispose(); // existing request scope coalesces this into one synchronous unlock
                    out.add(new Result(Outcome.ALREADY_COMPLETED, o.questId(), "ability initialization record re-sent"));
                    break;
                }
                String script = scriptName(o.questId(), true);
                if (script != null) {
                    passTo265(chr, Tms282QuestRequest.ACT_OPENING_SCRIPT, o.questId(), o.npcId(), o.x(), o.y());
                    out.add(new Result(Outcome.SCRIPT, o.questId(), "quest/" + script));
                } else {
                    accept(chr, o.questId(), o.npcId(), send, out);
                }
            }
            case Tms282QuestRequest.CompleteScript c -> {
                if (chr.hasQuestCompleted(c.questId())) {
                    chr.dispose();
                    out.add(new Result(Outcome.ALREADY_COMPLETED, c.questId(), "completed quest cannot replay reward script"));
                    break;
                }
                Quest held = Tms282QuestService.held(chr, c.questId());
                if (held == null || held.getStatus() != QuestStatus.Started || !held.isComplete(chr)) {
                    out.add(new Result(Outcome.NOT_READY, c.questId(), "completion script requires owned Started ready quest"));
                    break;
                }
                String script = scriptName(c.questId(), false);
                if (script != null) {
                    passTo265(chr, Tms282QuestRequest.ACT_COMPLETE_SCRIPT, c.questId(), c.npcId(), c.x(), c.y());
                    out.add(new Result(Outcome.SCRIPT, c.questId(), "quest/" + script));
                } else {
                    out.add(complete(chr, c.questId(), c.npcId(), send, "act 5 without script"));
                }
            }
            case Tms282QuestRequest.Resign r -> out.add(resign(chr, r.questId(), send));
            case Tms282QuestRequest.LaterStep l -> {
                if (!chr.hasQuestInProgress(l.questId()) && !chr.hasQuestCompleted(l.questId())) {
                    out.add(new Result(Outcome.NOT_HELD, l.questId(), "later step requires an owned active/completed quest"));
                    break;
                }
                // A quest that waits for the client between its scripts (quest/q<id>s_<n>): run the next one.
                java.util.Optional<String> step = chr.hasQuestInProgress(l.questId())
                        ? Tms282QuestStepScripts.next(l.questId(), chr.getQRValue(l.questId())) : java.util.Optional.empty();
                if (step.isPresent()) {
                    chr.getScriptManager().startScript(chr, l.questId(), step.get(), net.swordie.ms.scripts.ScriptType.Quest);
                    out.add(new Result(Outcome.SCRIPT, l.questId(), "later step quest/" + step.get()));
                    break;
                }
                passTo265Quest(chr, Tms282QuestRequest.ACT_LATER_STEP, l.questId());
                out.add(new Result(Outcome.PASSED_TO_265, l.questId(), "later step"));
            }
            case Tms282QuestRequest.ResultAck r -> out.add(new Result(Outcome.ALREADY_HELD, r.questId(), "act 7 ack"));
            case Tms282QuestRequest.Other x -> {
            }
        }
        return out;
    }

    // ---- accept ----------------------------------------------------------------------------------------------

    /** Which accept condition failed, for the log only (never changes the outcome). */
    private static String acceptRefusalDetail(Char chr, int questId, QuestInfo qi) {
        try {
            if (qi == null) return " [no quest data]";
            StringBuilder b = new StringBuilder(" [job=" + chr.getJob() + " lv=" + chr.getLevel());
            String script = scriptName(questId, true);
            if (script != null) b.append(" startScript=").append(script);
            if (chr.hasQuestCompleted(questId)) b.append(" alreadyCompleted");
            if (chr.hasQuestInProgress(questId)) b.append(" inProgress");
            if (QuestData.hasUnsupportedStartGrouping(questId)) b.append(" unsupportedStartGrouping");
            for (var r : qi.getQuestStartRequirements()) {
                if (!r.hasRequirements(chr)) {
                    b.append(" unmet=").append(r.getClass().getSimpleName().replace("QuestStart", "").replace("Requirement", ""));
                    if (r instanceof net.swordie.ms.client.character.quest.requirement.QuestStartJobRequirement j) {
                        b.append(j.getJobReq().size() > 12 ? "(" + j.getJobReq().size() + " jobs)" : j.getJobReq().toString());
                    }
                }
            }
            return b.append(']').toString();
        } catch (RuntimeException e) {
            return " [detail failed: " + e + "]";
        }
    }

    private void accept(Char chr, int questId, int npcId, Consumer<OutPacket> send, List<Result> out) {
        QuestInfo qi = QuestData.getQuestInfoById(questId);
        Quest held = Tms282QuestService.held(chr, questId);
        net.swordie.ms.client.AccountQuest previousShared = QuestConstants.isAccountQuest(questId) && chr.getAccount() != null ? chr.getAccount().getQuestById(questId) : null;
        if ((held == null || held.getStatus() == QuestStatus.NotStarted) && (qi == null || !chr.canStartQuest(questId) || scriptName(questId, true) != null)) {
            out.add(new Result(Outcome.NOT_READY, questId, "accept: unknown data, unmet start requirements or start script required"
                    + acceptRefusalDetail(chr, questId, qi)));
            return;
        }
        if ((held == null || held.getStatus() == QuestStatus.NotStarted) && qi != null && !inDateWindow(qi)) {
            out.add(new Result(Outcome.OUT_OF_DATE, questId, "Check 0 start=" + qi.getStart() + " end=" + qi.getEnd()));
            return;
        }
        Tms282QuestService.Started started;
        if (held != null || qi == null || QuestConstants.isAccountQuest(questId)) {
            started = Tms282QuestService.markStarted(chr, questId); // existing rule: never reset a held quest
        } else {
            Map<Integer, Quest> quests = chr.getQuests();
            if (quests == null) {
                quests = new ConcurrentHashMap<>();
                chr.setQuests(quests);
            }
            Quest quest = QuestData.createQuestFromId(questId, chr.getId()); // with its progress requirements
            quest.setQrValue("");
            Quest raced = quests.putIfAbsent(questId, quest);
            started = raced != null ? new Tms282QuestService.Started(raced, false, false)
                    : new Tms282QuestService.Started(quest, true, false);
        }
        String saved;
        try {
            saved = store.saveQuest(chr, started) ? "inserted id=" + started.quest().getId()
                    : "existing, unchanged";
        } catch (RuntimeException e) {
            if (started.created()) {
                if (started.accountQuest()) {
                    if (previousShared == null) chr.getAccount().getQuests().remove(questId);
                    else chr.getAccount().getQuests().put(questId, previousShared);
                } else if (held == null) chr.getQuests().remove(questId, started.quest());
                else chr.getQuests().put(questId, held);
            }
            out.add(new Result(Outcome.NOT_READY, questId, "start persistence failed: " + e.getClass().getSimpleName()));
            return;
        }
        Quest quest = started.quest();
        send.accept(Tms282FieldHandlers.questRecord(questId, quest, charset));
        if (questResult && quest.getStatus() == QuestStatus.Started) {
            send.accept(Tms282QuestPackets.questResultSuccess(questId, npcId, 0, false));
        }
        String startRewards = started.created() && qi != null ? giveStartRewards(chr, qi, send) : "";
        out.add(new Result(started.created() ? Outcome.STARTED : Outcome.ALREADY_HELD, questId,
                "state=" + quest.getStatus() + " db=" + saved + startRewards));
        if (started.created() && qi != null && QuestData.canAutoComplete(qi) && quest.isComplete(chr)) {
            out.add(complete(chr, questId, npcId, send, "autoComplete on accept"));
        }
    }

    /**
     * The Act-0 (start) item and buff-item rewards, as 265 {@code Char.addQuest} gives them; the bag/stat changes go
     * out as LP008F/LP009D. Nothing is sent for a quest without start rewards.
     */
    private String giveStartRewards(Char chr, QuestInfo qi, Consumer<OutPacket> send) {
        List<QuestReward> rewards = new ArrayList<>();
        for (QuestReward qr : qi.getQuestRewards()) {
            if ((qr instanceof QuestItemReward qir && qir.getStatus() == 0)
                    || (qr instanceof QuestBuffItemReward qbr && qbr.getStatus() == 0)) {
                rewards.add(qr);
            }
        }
        if (rewards.isEmpty()) {
            return "";
        }
        Tms282ChangeWatch watch = Tms282ChangeWatch.of(chr);
        String failure = "";
        try {
            for (QuestReward qr : rewards) {
                if (qr instanceof QuestItemReward qir) {
                    qir.giveReward(chr, qi.getQuestName());
                } else {
                    qr.giveReward(chr);
                }
            }
        } catch (RuntimeException e) {
            failure = " FAILED: " + e;
        }
        String changes = watch.describe();
        for (OutPacket p : watch.replies(charset)) {
            send.accept(p);
        }
        return "; start rewards " + rewards.size() + failure + " -> " + changes;
    }

    /** Check 0 {@code start}/{@code end} (yyyyMMddHHmm, or yyyyMMddHH); 0 means unbounded. */
    boolean inDateWindow(QuestInfo qi) {
        long now = Long.parseLong(clock.get().format(STAMP));
        long start = stamp(qi.getStart()), end = stamp(qi.getEnd());
        return (start == 0 || now >= start) && (end == 0 || now <= end);
    }

    static long stamp(long v) {
        if (v <= 0) {
            return 0;
        }
        long s = v;
        while (s < 100_000_000_000L) { // pad yyyyMMdd / yyyyMMddHH to minutes
            s *= 100;
        }
        return s;
    }

    // ---- complete --------------------------------------------------------------------------------------------

    private Result complete(Char chr, int questId, int npcId, Consumer<OutPacket> send, String why) {
        Quest quest = Tms282QuestService.held(chr, questId);
        if (quest == null) {
            return new Result(Outcome.NOT_HELD, questId, why);
        }
        if (quest.getStatus() == QuestStatus.Completed) {
            send.accept(Tms282FieldHandlers.questRecord(questId, quest, charset));
            return new Result(Outcome.ALREADY_COMPLETED, questId, why + "; completed record re-sent");
        }
        QuestInfo info = QuestData.getQuestInfoById(questId);
        if (quest.getStatus() != QuestStatus.Started || info == null || !QuestData.hasSupportedEndChecks(info)
                || info.getEndScript() != null && !info.getEndScript().isBlank()
                || scriptName(questId, false) != null || !quest.isComplete(chr)) {
            return new Result(Outcome.NOT_READY, questId, why + "; 265 progress requirements not met");
        }
        Tms282ChangeWatch watch = Tms282ChangeWatch.of(chr);
        String failure = "";
        try {
            chr.completeQuest(questId, false, () -> store.saveQuestState(chr, Tms282QuestService.held(chr, questId)));
            quest = Tms282QuestService.held(chr, questId);
        } catch (RuntimeException e) {
            // A 265 reward/effect path failed after (or before) the status change: keep the session, report it.
            failure = "; 265 completeQuest FAILED: " + e;
            quest = Tms282QuestService.held(chr, questId);
            if (quest.getStatus() != QuestStatus.Completed) {
                return new Result(Outcome.NOT_READY, questId, why + failure);
            }
            send.accept(Tms282FieldHandlers.questRecord(questId, quest, charset));
        }
        String saved = "written before rewards" + failure;
        String changes = watch.describe();
        for (OutPacket p : watch.replies(charset)) {
            send.accept(p);
        }
        if (questResult) {
            QuestInfo qi = QuestData.getQuestInfoById(questId);
            send.accept(Tms282QuestPackets.questResultSuccess(questId, npcId, qi == null ? 0 : qi.getNextQuest(), false));
        }
        return new Result(Outcome.COMPLETED, questId, why + "; db=" + saved + "; " + changes);
    }

    private String persist(Char chr, Quest quest) {
        try {
            store.saveQuestState(chr, quest);
            return "written";
        } catch (RuntimeException e) {
            return "FAILED (" + e.getClass().getSimpleName() + "; in memory, saved with the character)";
        }
    }

    // ---- resign ----------------------------------------------------------------------------------------------

    /**
     * Act 3: only a held, Started character quest is given up (265 Char.removeQuest drops any status and deletes the
     * row itself); the record becomes NotStarted (LP00AE state 0) and the row is removed through the store.
     */
    private Result resign(Char chr, int questId, Consumer<OutPacket> send) {
        Quest quest = chr.getQuests() == null ? null : chr.getQuests().get(questId);
        if (quest == null || quest.getStatus() != QuestStatus.Started || QuestConstants.isAccountQuest(questId)) {
            return new Result(Outcome.NOT_HELD, questId, "resign: no Started character quest");
        }
        String saved;
        try {
            store.deleteQuest(chr, quest);
            saved = "deleted";
        } catch (RuntimeException e) {
            return new Result(Outcome.NOT_READY, questId, "resign persistence failed: " + e.getClass().getSimpleName());
        }
        chr.getQuests().remove(questId, quest);
        quest.setStatus(QuestStatus.NotStarted);
        send.accept(Tms282QuestPackets.questRecordNotStarted(questId));
        return new Result(Outcome.RESIGNED, questId, "db=" + saved);
    }

    // ---- field entry -----------------------------------------------------------------------------------------

    /**
     * Completes (without rewards) the Started quests that have {@code autoComplete} and no 265 end requirement:
     * nothing is left for the client or the server to wait for. Other Started quests are untouched.
     */
    List<Result> completeStuckAutoQuests(Char chr, Consumer<OutPacket> send) {
        List<Result> out = new ArrayList<>();
        if (chr.getQuests() == null) {
            return out;
        }
        for (Quest quest : List.copyOf(chr.getQuests().values())) {
            if (quest.getStatus() != QuestStatus.Started || QuestConstants.isAccountQuest(quest.getQRKey())) {
                continue;
            }
            QuestInfo qi = QuestData.getQuestInfoById(quest.getQRKey());
            if (qi == null || !QuestData.canAutoComplete(qi) || !qi.getQuestProgressRequirements().isEmpty()
                    || qi.getEndScript() != null && !qi.getEndScript().isBlank()
                    || scriptName(quest.getQRKey(), false) != null) {
                continue;
            }
            try {
                chr.completeQuest(quest.getQRKey(), true);
                // 265 wrote the QuestRecord message; the legacy translator sends it as LP00AE state 2
            } catch (RuntimeException e) {
                quest.completeQuest(); // the 265 extras (record message, effect) failed; the state change is the fix
                send.accept(Tms282FieldHandlers.questRecord(quest.getQRKey(), quest, charset));
            }
            String saved = persist(chr, quest);
            out.add(new Result(Outcome.COMPLETED, quest.getQRKey(), "field-entry sweep (no rewards); db=" + saved));
        }
        return out;
    }

    // ---- 265 --------------------------------------------------------------------------------------------------

    /** The quest script the 265 QuestHandler would run for act 4 ({@code start}) or 5, when its file exists. */
    static String scriptName(int questId, boolean start) {
        QuestInfo qi = QuestData.getQuestInfoById(questId);
        String name = qi == null ? null : start ? qi.getStartScript() : qi.getEndScript();
        if (name == null || name.isBlank()) {
            name = "q" + questId + (start ? ScriptManagerImpl.QUEST_START_SCRIPT_END_TAG
                    : ScriptManagerImpl.QUEST_COMPLETE_SCRIPT_END_TAG);
        }
        for (String ext : new String[]{".py", ".js"}) {
            if (new File(ServerConstants.SCRIPT_DIR + "/quest/" + name + ext).isFile()) {
                return name;
            }
        }
        return null;
    }

    private static void passTo265(Char chr, int act, int questId, int npcId, short x, short y) {
        ByteBuffer b = ByteBuffer.allocate(13).order(ByteOrder.LITTLE_ENDIAN);
        b.put((byte) act).putInt(questId).putInt(npcId).putShort(x).putShort(y);
        QuestHandler.handleUserQuestRequest(chr, new InPacket(b.array()));
    }

    private static void passTo265Quest(Char chr, int act, int questId) {
        ByteBuffer b = ByteBuffer.allocate(5).order(ByteOrder.LITTLE_ENDIAN);
        b.put((byte) act).putInt(questId);
        QuestHandler.handleUserQuestRequest(chr, new InPacket(b.array()));
    }
}
