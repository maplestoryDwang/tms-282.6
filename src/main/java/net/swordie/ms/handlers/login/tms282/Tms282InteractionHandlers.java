package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.Tms282InteractionRequests.Emotion;
import net.swordie.ms.connection.packet.Tms282InteractionRequests.PortableChairSit;
import net.swordie.ms.connection.packet.Tms282InteractionRequests.Sit;
import net.swordie.ms.connection.packet.Tms282InteractionRequests.QuickMove;
import net.swordie.ms.connection.packet.Tms282InteractionRequests.KaiserAppearance;
import net.swordie.ms.connection.packet.Tms282QuickMove;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.connection.packet.Tms282StatPackets;
import net.swordie.ms.connection.packet.Tms282BattleRecordRequest;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.handlers.item.ItemHandler;
import net.swordie.ms.handlers.user.UserHandler;

/**
 * Character-interaction routes (2026-09-24), channel socket, in field only:
 * <ul>
 *     <li>CP011A USER_EMOTION ({@link Emotion}) -> 265 {@link UserHandler#handleUserEmotion}: others get REMOTE_EMOTION
 *     (LP0480, 265 body);</li>
 *     <li>CP0108 USER_PORTABLE_CHAIR_SIT_REQUEST ({@link PortableChairSit}, 282 adds one i32) -> 265
 *     {@link UserHandler#handleUserPortableChairSitRequest}. The client locks after sending it (5E04DA0(ctx, 1)); the
 *     265 handler ends with {@code chr.dispose()}, which {@code Tms282UnlockScope} turns into the LP009D unlock; a
 *     request that does not parse is answered with the unlock directly.</li>
 * </ul>
 * CP07A9 / CP025C quick move (2026-10-05): open request -> 265 list (LP0296), button click (282 EasyMove code) -> 265
 * {@link UserHandler#handleUserQuickMoveScript}; codes and strings are converted by {@code Tms282QuickMove}.
 * <p>
 * CP0107 USER_SIT_REQUEST ({@link Sit}, the 265 layout) -> 265 {@link UserHandler#handleUserSitRequest}: LP0401
 * SIT_RESULT to the user, LP048C stand-up to others, unlock via {@code chr.dispose()}.
 * The replies are translated by {@code Tms282InteractionTranslators}.
 */
final class Tms282InteractionHandlers {

    void register(Tms282Routes routes) {
        routes.channel(KaiserAppearance.WING_OPCODE, InHeader.USER_KAISER_TRANSFORM_WING).when(Tms282Guards.IN_FIELD)
                .handle("Kaiser wing -> LP0488", this::kaiserAppearance);
        routes.channel(KaiserAppearance.TAIL_OPCODE, InHeader.USER_KAISER_TRANSFORM_TAIL).when(Tms282Guards.IN_FIELD)
                .handle("Kaiser tail -> LP0488", this::kaiserAppearance);
        routes.channel(Tms282BattleRecordRequest.OPCODE, InHeader.BATTLE_RECORD_ON_OFF_REQUEST).when(Tms282Guards.IN_FIELD)
                .handle("battle analysis on/off -> LP0B48", this::battleRecord);
        routes.channel(QuickMove.OPCODE, InHeader.USER_OPEN_QUICK_MOVE_REQUEST).when(Tms282Guards.IN_FIELD)
                .handle("quick move -> 265 UserHandler (LP0296)", this::quickMove);
        routes.channel(Tms282QuickMove.Script.OPCODE, InHeader.USER_QUICK_MOVE_SCRIPT).when(Tms282Guards.IN_FIELD)
                .handle("quick move button -> 265 UserHandler quick move script", this::quickMoveScript);
        routes.channel(Emotion.OPCODE, InHeader.USER_EMOTION).when(Tms282Guards.IN_FIELD)
                .handle("emotion -> 265 UserHandler (LP0480 to others)", this::emotion);
        routes.channel(Sit.OPCODE, InHeader.USER_SIT_REQUEST).when(Tms282Guards.IN_FIELD)
                .handle("field seat / stand up -> 265 UserHandler (LP0401, LP048C)", this::sit);
        routes.channel(PortableChairSit.OPCODE, InHeader.USER_PORTABLE_CHAIR_SIT_REQUEST).when(Tms282Guards.IN_FIELD)
                .handle("portable chair -> 265 UserHandler (+ unlock)", this::chair);
        routes.channel(CHAIR_STAT_CHANGE, InHeader.USER_STAT_CHANGE_BY_PORTABLE_CHAIR_REQUEST).when(Tms282Guards.IN_FIELD)
                .handle("portable chair recovery tick -> 265 ItemHandler (HP/MP, unlock)", this::chairRecovery);
    }

    /**
     * CP014A USER_STAT_CHANGE_BY_PORTABLE_CHAIR_REQUEST: the chair's periodic recovery. Opcode 推定 (Δ+5 interval
     * 12C..14B, 10 anchors; sender 5E3CED0) with an empty body on both sides; a trailing u32 (socket-layer tail) is
     * tolerated. Only while a chair is set: 265 dereferences {@code chr.getChair()} without a check.
     */
    static final int CHAIR_STAT_CHANGE = 0x014A;

    private void kaiserAppearance(Tms282LoginState session, int op, byte[] packet) {
        try {
            var request = KaiserAppearance.parse(packet, op);
            Char chr = session.fieldCharacter();
            if (!JobConstants.isKaiser(chr.getJob())) {
                throw new IllegalArgumentException("Kaiser appearance requires Kaiser job");
            }
            if (request.itemId() != 0) {
                var item = chr.getCashInventory().getItemByItemID(request.itemId());
                if (item == null || item.getQuantity() <= 0 || item.getDateExpire() != null && item.getDateExpire().isExpired()) {
                    throw new IllegalArgumentException("Kaiser appearance item not owned");
                }
            }
            Tms282SocialHandlers.guarded(chr, op, () -> {
                var body = new InPacket(request.toGms265Body());
                if (request.tail()) {
                    ItemHandler.handleUserKaierTransformTail(chr, body);
                } else {
                    ItemHandler.handleUserKaierTransformWing(chr, body);
                }
            });
        } catch (IllegalArgumentException e) {
            Tms282SocialHandlers.rejected(op, packet, e);
        }
    }

    private void battleRecord(Tms282LoginState session, int op, byte[] packet) {
        try {
            var request = Tms282BattleRecordRequest.parse(packet);
            UserHandler.handleBattleRecordOnOffRequest(session.client(), new InPacket(request.toGms265Body()));
        } catch (IllegalArgumentException e) {
            Tms282SocialHandlers.rejected(op, packet, e);
        }
    }

    private void quickMove(Tms282LoginState session, int op, byte[] packet) {
        QuickMove request;
        try {
            request = QuickMove.parse(packet);
        } catch (IllegalArgumentException e) {
            Tms282SocialHandlers.rejected(op, packet, e);
            return; // The sender takes no exclusive-request lock.
        }
        Char chr = session.fieldCharacter();
        Tms282SocialHandlers.guarded(chr, op,
                () -> UserHandler.handleUserOpenQuickMoveRequest(chr, new InPacket(request.toGms265Body())));
    }

    /**
     * CP025C: a quick-move button (282 EasyMove code) -> its 265 QuickMoveType. The client checks level / quest /
     * period itself before sending (967310) and takes no exclusive-request lock; buttons without 265 content are
     * logged and ignored.
     */
    private void quickMoveScript(Tms282LoginState session, int op, byte[] packet) {
        Tms282QuickMove.Script request;
        try {
            request = Tms282QuickMove.Script.parse(packet);
        } catch (IllegalArgumentException e) {
            Tms282SocialHandlers.rejected(op, packet, e);
            return;
        }
        Char chr = session.fieldCharacter();
        Tms282SocialHandlers.guarded(chr, op,
                () -> UserHandler.handleUserQuickMoveScript(chr, new InPacket(request.toGms265Body())));
    }

    private void chairRecovery(Tms282LoginState session, int op, byte[] packet) {
        if (packet.length != 2 && packet.length != 6) {
            Tms282SocialHandlers.rejected(op, packet, new IllegalArgumentException("CP014A body of " + (packet.length - 2) + " bytes"));
            session.send(Tms282StatPackets.unlock());
            return;
        }
        Char chr = session.fieldCharacter();
        if (chr == null || chr.getChair() == null) {
            session.send(Tms282StatPackets.unlock());
            return;
        }
        Tms282SocialHandlers.guarded(chr, op,
                () -> ItemHandler.handleUserStatChangePortalChairRequest(chr, new InPacket(new byte[0])));
    }

    private void emotion(Tms282LoginState session, int op, byte[] packet) {
        Emotion request;
        try {
            request = Emotion.parse(packet);
        } catch (IllegalArgumentException e) {
            Tms282SocialHandlers.rejected(op, packet, e);
            return;
        }
        Char chr = session.fieldCharacter();
        Tms282SocialHandlers.guarded(chr, op, () -> UserHandler.handleUserEmotion(chr, new InPacket(request.toGms265Body())));
    }

    private void sit(Tms282LoginState session, int op, byte[] packet) {
        Sit request;
        try {
            request = Sit.parse(packet);
        } catch (IllegalArgumentException e) {
            Tms282SocialHandlers.rejected(op, packet, e);
            session.send(Tms282StatPackets.unlock());
            return;
        }
        Char chr = session.fieldCharacter();
        Tms282SocialHandlers.guarded(chr, op, () -> UserHandler.handleUserSitRequest(chr, new InPacket(request.toGms265Body())));
    }

    private void chair(Tms282LoginState session, int op, byte[] packet) {
        PortableChairSit request;
        try {
            request = PortableChairSit.parse(packet);
        } catch (IllegalArgumentException e) {
            Tms282SocialHandlers.rejected(op, packet, e);
            session.send(Tms282StatPackets.unlock());
            return;
        }
        Char chr = session.fieldCharacter();
        System.out.println("[TMS282] " + request);
        Tms282SocialHandlers.guarded(chr, op,
                () -> UserHandler.handleUserPortableChairSitRequest(chr, new InPacket(request.toGms265Body())));
    }
}
