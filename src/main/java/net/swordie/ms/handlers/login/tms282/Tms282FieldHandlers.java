package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.CharacterStat;
import net.swordie.ms.client.character.Tms282StatRecoveryService;
import net.swordie.ms.client.character.quest.Quest;
import net.swordie.ms.client.character.tms282.Tms282ChatCommand;
import net.swordie.ms.client.character.tms282.Tms282ChatCommands;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.packet.Tms282ChatPackets;
import net.swordie.ms.connection.packet.Tms282ChatRequest;
import net.swordie.ms.connection.packet.Tms282FuncKeyModifiedRequest;
import net.swordie.ms.connection.packet.Tms282InstanceTablePackets;
import net.swordie.ms.connection.packet.Tms282InstanceTableRequest;
import net.swordie.ms.connection.packet.Tms282QuestPackets;
import net.swordie.ms.connection.packet.Tms282QuestRequest;
import net.swordie.ms.connection.packet.Tms282StatPackets;
import net.swordie.ms.connection.packet.Tms282StatRecoveryRequest;
import net.swordie.ms.handlers.header.InHeader;

import java.nio.charset.Charset;
import java.util.Objects;
import java.util.Optional;

/**
 * In-field packets of TMS 282.3 on the channel socket, after LP02EF (the research probe's channelHandlers() entries
 * guarded by {@code when(IN_FIELD)}; infield-capture-1790158495223.md 2.1/2.2/2.4, ingame-layouts-282.md 4.3):
 * <ul>
 *     <li>CP020F FUNC_KEY_MAPPED_MODIFIED: key-config changes, saved to funckeymap/keymaps ({@link Tms282KeyConfig});</li>
 *     <li>CP0117 USER_CHAT -> LP03D5 to the whole field, the speaker included (the probe echoes to the speaker only,
 *     having no other user); '@' lines go to the {@link Tms282ChatCommandHook} first;</li>
 *     <li>CP01A6 USER_QUEST_REQUEST -> the quest flow {@link Tms282QuestFlow}: accept / complete (LP00AE QuestRecord,
 *     rewards as LP008F/LP009D, LP04D0 act 12 with tms282.questResult), quest scripts through the 265 QuestHandler;</li>
 *     <li>CP0193 USER_CHANGE_STAT_REQUEST (natural recovery) -> LP009D hp|mp with exclRequest; hp/mp written by
 *     {@link Tms282HpMpSaveTimer};</li>
 *     <li>CP01C0 USER_REQUEST_INSTANCE_TABLE -> LP0121.</li>
 * </ul>
 * Malformed requests are logged and dropped, as in the probe; any other exception ends the session
 * ({@link Tms282Routes#adapt}). Chat text, key values and macro names are never logged.
 */
final class Tms282FieldHandlers {
    private final Tms282FieldServices services;
    private final Charset nameCharset;
    private final boolean questResult;
    private final Tms282KeyConfig keys;

    Tms282FieldHandlers(Tms282LoginContext context) {
        Objects.requireNonNull(context, "context");
        this.services = context.fieldServices();
        this.nameCharset = context.settings().nameCharset();
        this.questResult = context.settings().questResult();
        this.keys = new Tms282KeyConfig(nameCharset);
        this.quests = new Tms282QuestFlow(services.store(), nameCharset, questResult, java.time.LocalDateTime::now);
    }

    private final Tms282QuestFlow quests;
    /** Characters whose stuck autoComplete quests were already swept in this server run. */
    private final java.util.Set<Integer> swept = java.util.concurrent.ConcurrentHashMap.newKeySet();

    void register(Tms282Routes routes) {
        routes.channel(Tms282FuncKeyModifiedRequest.OPCODE, InHeader.FUNC_KEY_MAPPED_MODIFIED).when(Tms282Guards.IN_FIELD)
                .handle("key config change -> funckeymap/keymaps (no reply)", (s, op, p) -> keys.onModified(s, p, services.store()));
        routes.channel(net.swordie.ms.connection.packet.Tms282MacroRequest.OPCODE, InHeader.USER_MACRO_SYS_DATA_MODIFIED)
                .when(Tms282Guards.IN_FIELD)
                .handle("macro change -> atomic macro storage", (s, op, p) -> keys.onMacrosModified(s, p, services.store()));
        routes.channel(Tms282ChatRequest.OPCODE, InHeader.USER_CHAT).when(Tms282Guards.IN_FIELD)
                .handle("chat -> LP03D5 to the field", this::onChat);
        routes.channel(Tms282QuestRequest.OPCODE, InHeader.USER_QUEST_REQUEST).when(Tms282Guards.IN_FIELD)
                .handle("quest request -> LP00AE QuestRecord", this::onQuestRequest);
        routes.channel(Tms282StatRecoveryRequest.OPCODE, InHeader.USER_CHANGE_STAT_REQUEST).when(Tms282Guards.IN_FIELD)
                .handle("hp/mp recovery -> LP009D", this::onStatRecovery);
        routes.channel(Tms282InstanceTableRequest.OPCODE, InHeader.USER_REQUEST_INSTANCE_TABLE).when(Tms282Guards.IN_FIELD)
                .handle("instance table -> LP0121", this::onInstanceTable);
    }

    // ---- CP0117 ----------------------------------------------------------------------------------------------

    private void onChat(Tms282LoginState session, int op, byte[] packet) {
        Tms282ChatRequest request;
        try {
            request = Tms282ChatRequest.parse(packet, nameCharset);
        } catch (IllegalArgumentException e) {
            System.out.printf("[TMS282] REJECTED CP0117 len=%d: %s%n", packet.length, e.getMessage());
            return;
        }
        Char chr = session.fieldCharacter();
        CharacterStat stat = chr.getAvatarData().getCharacterStat();
        var sender = new Tms282ChatPackets.ChatSender(chr.getId(), chr.getAccId(), chr.getName(), stat.getWorldIdForLog());
        if (Tms282ChatCommands.isCommand(request.text()) && runChatCommand(session, chr, sender, request)) {
            return;
        }
        OutPacket line = Tms282ChatPackets.userChat(sender, Tms282ChatPackets.CHAT_TYPE_USER, request.text(),
                request.displayMode(), nameCharset);
        int sockets = services.audience().broadcast(session, chr, line);
        System.out.println("[TMS282] CP0117 chat character=" + chr.getId() + " chars=" + request.text().length()
                + " mode=" + request.displayMode() + "; SENT LP03D5 to " + sockets + " socket(s) in the field");
    }

    /**
     * '@' line through the command hook: a reply is sent to the speaker only (its packets, then "[cmd] message" as
     * LP03D5); no reply, or a failing hook, falls back to ordinary chat (probe runChatCommand).
     */
    private boolean runChatCommand(Tms282LoginState session, Char chr, Tms282ChatPackets.ChatSender sender,
                                   Tms282ChatRequest request) {
        int accountType = accountTypeOf(chr);
        Optional<Tms282ChatCommand.Reply> reply;
        try {
            reply = services.chatCommands().dispatch(chr, accountType, request.text());
        } catch (RuntimeException | LinkageError e) {
            System.out.println("[TMS282] FAILED chat command character=" + chr.getId() + ": " + e);
            return false;
        }
        if (reply.isEmpty()) {
            return false;
        }
        for (OutPacket p : reply.get().packets()) {
            session.send(p);
        }
        session.send(Tms282ChatPackets.userChat(sender, Tms282ChatPackets.CHAT_TYPE_USER, "[cmd] " + reply.get().message(),
                request.displayMode(), nameCharset));
        System.out.println("[TMS282] CHAT COMMAND character=" + chr.getId() + " accounttype=" + accountType + " "
                + reply.get().log() + "; SENT " + reply.get().packets().size() + " packet(s) + LP03D5");
        return true;
    }

    /** users.accounttype of the bound user (265 AccountType value); 0 when unknown. */
    static int accountTypeOf(Char chr) {
        return chr.getUser() == null || chr.getUser().getAccountType() == null ? 0 : chr.getUser().getAccountType().getVal();
    }

    // ---- CP01A6 ----------------------------------------------------------------------------------------------

    /**
     * The quest flow ({@link Tms282QuestFlow}): accept (date window, 265 progress requirements, autoComplete),
     * complete (act 2, rewards through 265, LP008F/LP009D), quest scripts (acts 4/5 when the script exists, 3, 6
     * through the 265 QuestHandler). The first request of a character in this server run also completes its stuck
     * autoComplete quests that have no end requirement ({@link Tms282QuestFlow#completeStuckAutoQuests}). Act 7 and
     * uncaptured shapes are logged only.
     */
    private void onQuestRequest(Tms282LoginState session, int op, byte[] packet) {
        Tms282QuestRequest request;
        try {
            request = Tms282QuestRequest.parse(packet);
        } catch (IllegalArgumentException e) {
            System.out.printf("[TMS282] REJECTED CP01A6 len=%d: %s%n", packet.length, e.getMessage());
            return;
        }
        Char chr = session.fieldCharacter();
        if (swept.add(chr.getId())) {
            for (Tms282QuestFlow.Result r : quests.completeStuckAutoQuests(chr, session::send)) {
                System.out.println("[TMS282] CP01A6 " + r + "; SENT LP00AE completed");
            }
        }
        if (request.payload() instanceof Tms282QuestRequest.Other x) {
            System.out.println("[TMS282] CP01A6 act " + request.act() + " len=" + x.body().length
                    + " (layout not captured; logged only)");
            return;
        }
        for (Tms282QuestFlow.Result r : quests.handle(chr, request, session::send)) {
            System.out.println("[TMS282] CP01A6 act " + request.act() + " npc-side " + r);
        }
    }

    /** LP00AE QuestRecord for the quest's current state (probe startQuest). */
    static OutPacket questRecord(int questId, Quest quest, Charset charset) {
        return switch (quest.getStatus()) {
            case Started -> Tms282QuestPackets.questRecordStarted(questId, quest.getQRValue() == null ? "" : quest.getQRValue(), charset);
            case Completed -> Tms282QuestPackets.questRecordCompleted(questId, Tms282QuestPackets.fileTimeOf(
                    quest.getCompletedTime() != null ? quest.getCompletedTime().toMillis() : System.currentTimeMillis()));
            case NotStarted -> Tms282QuestPackets.questRecordNotStarted(questId);
        };
    }

    // ---- CP0193 ----------------------------------------------------------------------------------------------

    /**
     * Natural recovery through {@link Tms282StatRecoveryService}: capped at the 265 total max (Char.getMaxHP/MP), then
     * LP009D with exclRequest 1. A change marks hp/mp for the next periodic write.
     */
    private void onStatRecovery(Tms282LoginState session, int op, byte[] packet) {
        Tms282StatRecoveryRequest request;
        try {
            request = Tms282StatRecoveryRequest.parse(packet);
        } catch (IllegalArgumentException e) {
            System.out.printf("[TMS282] REJECTED CP0193 len=%d: %s%n", packet.length, e.getMessage());
            return;
        }
        var applied = Tms282StatRecoveryService.apply(session.fieldCharacter(), request);
        if (applied.changed()) {
            session.hpMpSaveTimer(services).markDirty();
        }
        session.send(Tms282StatPackets.hpMpChanged(true, applied.newHp(), applied.newMp()));
        var cap = applied.cap();
        System.out.println("[TMS282] CP0193 recovery request +" + request.hp() + "hp/+" + request.mp() + "mp option="
                + request.option() + " hp " + applied.oldHp() + "->" + applied.newHp() + " mp " + applied.oldMp() + "->"
                + applied.newMp() + " cap=" + cap.maxHp() + "/" + cap.maxMp()
                + (cap.fromTotal() ? " (265 total" : " (BASE fallback: 265 total failed") + ", base " + cap.baseMaxHp()
                + "/" + cap.baseMaxMp() + "); SENT LP009D");
    }

    // ---- CP01C0 ----------------------------------------------------------------------------------------------

    /** LP0121 with the 265 hyper SP rules ({@link Tms282InstanceTablePackets#valueOf}). */
    private void onInstanceTable(Tms282LoginState session, int op, byte[] packet) {
        Tms282InstanceTableRequest request;
        try {
            request = Tms282InstanceTableRequest.parse(packet);
        } catch (IllegalArgumentException e) {
            System.out.printf("[TMS282] REJECTED CP01C0 len=%d: %s%n", packet.length, e.getMessage());
            return;
        }
        var value = Tms282InstanceTablePackets.valueOf(request);
        session.send(Tms282InstanceTablePackets.answer(request));
        System.out.println("[TMS282] CP01C0 " + request.table() + " " + request.type() + "/" + request.subType()
                + "; SENT LP0121 " + (value.isPresent() ? "ok value=" + value.getAsInt() : "ok=0 (no data)"));
    }
}
