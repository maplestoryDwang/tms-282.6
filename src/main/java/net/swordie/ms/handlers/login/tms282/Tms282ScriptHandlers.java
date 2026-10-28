package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.Tms282ScriptAnswerRequest;
import net.swordie.ms.connection.packet.Tms282ScriptDialog;
import net.swordie.ms.connection.packet.Tms282ScriptItemUseRequest;
import net.swordie.ms.connection.packet.Tms282SelectNpcRequest;
import net.swordie.ms.connection.packet.Tms282ShopRequest;
import net.swordie.ms.connection.packet.Tms282StatPackets;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.handlers.item.ItemHandler;
import net.swordie.ms.handlers.life.NpcHandler;
import net.swordie.ms.life.npc.NpcMessageType;
import net.swordie.ms.scripts.ScriptManagerImpl;
import net.swordie.ms.util.DataPrinter;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Objects;

/**
 * NPC and script requests of TMS 282.3 on the channel socket, after LP02EF ({@code when(IN_FIELD)}). Each request is
 * parsed with its 282 layout and handed to the unchanged 265 flow; the dialogs the 265 ScriptManagerImpl sends go out
 * as LP0960 through the profile's {@link net.swordie.ms.connection.protocol.codec.ScriptMessageEncoder}
 * ({@link net.swordie.ms.connection.packet.Tms282ScriptPackets}).
 * <ul>
 *     <li>CP012B USER_SELECT_NPC [verified] ({@link Tms282SelectNpcRequest}, +i32 -1) -> 265
 *     {@link NpcHandler#handleUserSelectNpc}: the NPC's script (ScriptType.Npc), a shop or a trunk;</li>
 *     <li>CP012C USER_SCRIPT_MESSAGE_ANSWER [verified] ({@link Tms282ScriptAnswerRequest}) -> 265
 *     {@link ScriptManagerImpl#handleAction} (the steps of 265 ScriptHandler.handleUserScriptMessageAnswer); an
 *     answer that cannot reach the waiting script ends it and unlocks the UI ({@link #releaseStuckScript});</li>
 *     <li>CP014E USER_SCRIPT_ITEM_USE_REQUEST [verified, 265 layout] ({@link Tms282ScriptItemUseRequest}) -> 265
 *     {@link ItemHandler#handleUserScriptItemUseRequest} (2432022 opens content script expand/menu), then LP009D
 *     unlock: the client sets its exclusive-request flag when it sends CP014E (5E11692) and the 265 end-of-script
 *     EXCL_REQUEST has no 282 opcode;</li>
 *     <li>CP012E USER_SHOP_REQUEST ({@link Tms282ShopRequest}) is routed by {@link Tms282DailyPlayHandlers} (shop
 *     window LP0962, results LP0963, bag and meso replies).</li>
 * </ul>
 * A request that does not parse is logged and dropped; a failure of the 265 logic is logged; neither ends the session.
 */
final class Tms282ScriptHandlers {
    private final Charset charset;

    Tms282ScriptHandlers(Charset charset) {
        this.charset = Objects.requireNonNull(charset, "charset");
    }

    void register(Tms282Routes routes) {
        routes.channel(Tms282SelectNpcRequest.OPCODE, InHeader.USER_SELECT_NPC).when(Tms282Guards.IN_FIELD)
                .handle("select NPC -> 265 NPC script (LP0960 dialogs)", this::selectNpc);
        routes.channel(Tms282ScriptAnswerRequest.OPCODE, InHeader.USER_SCRIPT_MESSAGE_ANSWER).when(Tms282Guards.IN_FIELD)
                .handle("script dialog answer -> 265 ScriptManager", this::answer);
        routes.channel(Tms282ScriptItemUseRequest.OPCODE, InHeader.USER_SCRIPT_ITEM_USE_REQUEST).when(Tms282Guards.IN_FIELD)
                .handle("script item use -> 265 item script + LP009D unlock", this::scriptItem);
        // CP012E USER_SHOP_REQUEST: moved to Tms282DailyPlayHandlers (daily-play phase, LP0962/LP0963 ported)
    }

    // ---- CP012B ------------------------------------------------------------------------------------------------

    private void selectNpc(Tms282LoginState session, int op, byte[] packet) {
        Tms282SelectNpcRequest request;
        try {
            request = Tms282SelectNpcRequest.parse(packet);
        } catch (IllegalArgumentException e) {
            rejected(op, packet, e);
            return;
        }
        Char chr = session.fieldCharacter();
        System.out.printf("[TMS282] CP012B select NPC object %d at (%d,%d) extra %d%n", request.objectId(), request.x(),
                request.y(), request.extra());
        guarded(chr, op, () -> NpcHandler.handleUserSelectNpc(chr, new InPacket(request.toGms265Body())));
    }

    // ---- CP012C ------------------------------------------------------------------------------------------------

    private void answer(Tms282LoginState session, int op, byte[] packet) {
        Char chr = session.fieldCharacter();
        Tms282ScriptAnswerRequest request;
        try {
            request = Tms282ScriptAnswerRequest.parse(packet, charset);
        } catch (IllegalArgumentException e) {
            rejected(op, packet, e);
            releaseStuckScript(chr, "CP012C rejected: " + e.getMessage());
            return;
        }
        try {
            answer(chr, request);
        } catch (RuntimeException e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
            System.out.printf("[TMS282] FAILED CP%04X character=%d: %s%n", op, chr.getId(), e);
            releaseStuckScript(chr, "CP012C failed: " + e);
        }
    }

    /** 265 ScriptHandler.handleUserScriptMessageAnswer after its decode. */
    static void answer(Char chr, Tms282ScriptAnswerRequest request) {
        ScriptManagerImpl sm = chr.getScriptManager();
        NpcMessageType shown = sm.getNpcScriptInfo().getMessageType();
        if (!request.dialog().answers(shown)) {
            // e.g. LP0502 type 11 camera release answers ask 3 unasked; handed to a Say it would skip the page
            System.out.printf("[TMS282] CP012C %s answer ignored: the script waits on %s%n", request.dialog(), shown);
            return;
        }
        NpcMessageType type = shown != null ? shown : fallbackType(request.dialog());
        if (shown != null && Tms282ScriptDialog.awaitedBy(shown).orElse(null) != request.dialog()) {
            System.out.printf("[TMS282] CP012C answers dialog type %d but the server showed %s%n", request.wireType(), shown);
        }
        Tms282ScriptDialog.Answer a = request.answer();
        if (sm.isActive(sm.getLastActiveScriptType())) {
            sm.handleAction(sm.getLastActiveScriptType(), type, (byte) a.action(), (int) a.answer(), a.text());
            return;
        }
        if (a.action() == -1) {
            sm.dispose();
        }
    }

    /**
     * Safety net for a CP012C that cannot reach the waiting script (unported answer type, malformed tail, a failure
     * in the 265 logic): without it the script thread stays parked in ScriptInfo.await* for EXPIRED_SCRIPT_TIME (1 h)
     * with the UI locked (enter_450003000 09-29). Ends the script like a closed dialog (dispose releases the waiter;
     * its finally runs stop / chr.dispose) and sends the LP04E2 unlock: scripts that lock with
     * sm.setInGameDirectionMode(True, ...) are not tracked by ScriptManagerImpl.unlockUIIfNeeded.
     */
    static void releaseStuckScript(Char chr, String reason) {
        if (chr == null) {
            return;
        }
        ScriptManagerImpl sm = chr.getScriptManager();
        if (sm == null || !sm.isActive(sm.getLastActiveScriptType())) {
            return;
        }
        System.out.printf("[TMS282] %s: ending script %s (%s) and unlocking the UI%n", reason, sm.getLastActiveScriptName(),
                sm.getLastActiveScriptType());
        try {
            sm.dispose();
            sm.lockInGameUI(false, true); // 265 SET_IN_GAME_DIRECTION_MODE off -> LP04E2 {0, 1}
            chr.dispose();
        } catch (RuntimeException e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
        }
    }

    private static NpcMessageType fallbackType(Tms282ScriptDialog dialog) {
        return switch (dialog) {
            case SAY -> NpcMessageType.Say;
            case SAY_IMAGE -> NpcMessageType.SayImage;
            case ASK_YES_NO -> NpcMessageType.AskYesNo;
            case ASK_TEXT -> NpcMessageType.AskText;
            case ASK_NUMBER -> NpcMessageType.AskNumber;
            case ASK_MENU -> NpcMessageType.AskMenu;
            case ASK_AVATAR -> NpcMessageType.AskAvatar;
            case ASK_ACCEPT -> NpcMessageType.AskAccept;
            case IN_GAME_DIRECTION -> NpcMessageType.AskIngameDirection;
            case PLAY_MOVIE -> NpcMessageType.PlayMovieClip;
            case MONOLOGUE -> NpcMessageType.Monologue;
        };
    }

    // ---- CP014E ------------------------------------------------------------------------------------------------

    private void scriptItem(Tms282LoginState session, int op, byte[] packet) {
        Tms282ScriptItemUseRequest request;
        try {
            request = Tms282ScriptItemUseRequest.parse(packet);
        } catch (IllegalArgumentException e) {
            rejected(op, packet, e);
            session.send(Tms282StatPackets.unlock());
            return;
        }
        Char chr = session.fieldCharacter();
        System.out.printf("[TMS282] CP014E script item %d slot %d quantity %d%n", request.itemId(), request.slot(),
                request.quantity());
        if(net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime.isGemstone(request.itemId())) {
            var item=chr.getConsumeInventory().getItemBySlot(request.slot());
            if(item!=null && item.getItemId()==request.itemId() && request.quantity()>0
                    && item.getQuantity()>=request.quantity() && (item.getDateExpire()==null || !item.getDateExpire().isExpired()))
                net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime.consumeGemstone(chr,request.itemId(),request.quantity());
            else session.send(Tms282StatPackets.unlock());
            return;
        }
        if (Tms282CharacterInfoBackgroundHandlers.isCoupon(request.itemId())) {
            Tms282CharacterInfoBackgroundHandlers.coupon(session, request, charset);
            return;
        }
        guarded(chr, op, () -> ItemHandler.handleUserScriptItemUseRequest(chr, new InPacket(request.toGms265Body())));
        session.send(Tms282StatPackets.unlock());
    }

    // ---- shared ------------------------------------------------------------------------------------------------

    private static void rejected(int op, byte[] packet, IllegalArgumentException e) {
        System.out.printf("[TMS282] REJECTED CP%04X len=%d: %s (%s)%n", op, packet.length, e.getMessage(),
                packet.length <= 48 ? Arrays.toString(packet) : "");
    }

    private static void guarded(Char chr, int op, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
            System.out.printf("[TMS282] FAILED CP%04X character=%d: %s%n", op, chr.getId(), e);
        }
    }
}
