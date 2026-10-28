package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.Tms282ChatGroupRequest;
import net.swordie.ms.connection.packet.Tms282FriendRequest;
import net.swordie.ms.handlers.social.FriendHandler;
import net.swordie.ms.connection.packet.Tms282PartyAnswerRequest;
import net.swordie.ms.connection.packet.Tms282PartyRequest;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.handlers.social.ChatHandler;
import net.swordie.ms.handlers.social.PartyHandler;
import net.swordie.ms.util.DataPrinter;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Objects;

/**
 * Social requests of TMS 282.3 on the channel socket after LP02EF ({@code when(IN_FIELD)}): each request is parsed
 * with its 282 layout and handed to the unchanged 265 handler; the 265 replies reach the client through the
 * {@link net.swordie.ms.connection.protocol.codec.LegacyPacketTranslator} contributors
 * ({@link net.swordie.ms.connection.packet.Tms282PartyTranslators} for PARTY_RESULT).
 * <ul>
 *     <li>CP01F2 PARTY_REQUEST ({@link Tms282PartyRequest}) -> 265 {@link PartyHandler#handlePartyRequest}:
 *     create, leave, invite, kick, change leader, apply, settings; other 282 types are logged;</li>
 *     <li>CP01F3 PARTY_RESULT ({@link Tms282PartyAnswerRequest}) -> 265 {@link PartyHandler#handlePartyResult}: the
 *     answer to an invitation (282 type 47/48 = 265 type 46);</li>
 *     <li>CP01ED WHISPER / CP01EA GROUP_MESSAGE ({@link Tms282ChatGroupRequest}) -> 265
 *     {@link ChatHandler#handleWhisper} / {@link ChatHandler#handleGroupMessage}; the replies go out as LP0305 /
 *     LP0302 ({@link net.swordie.ms.connection.packet.Tms282ChatGroupTranslators});</li>
 * </ul>
 * A request that does not parse is logged and dropped; a failure of the 265 logic is logged; neither ends the session.
 */
final class Tms282SocialHandlers {
    private final Charset charset;

    Tms282SocialHandlers(Charset charset) {
        this.charset = Objects.requireNonNull(charset, "charset");
    }

    void register(Tms282Routes routes) {
        routes.channel(Tms282PartyRequest.OPCODE, InHeader.PARTY_REQUEST).when(Tms282Guards.IN_FIELD)
                .handle("party request -> 265 PartyHandler (LP00CF replies)", this::partyRequest);
        routes.channel(Tms282PartyAnswerRequest.OPCODE, InHeader.PARTY_RESULT).when(Tms282Guards.IN_FIELD)
                .handle("party invitation answer -> 265 PartyHandler", this::partyAnswer);
        routes.channel(Tms282ChatGroupRequest.WHISPER_OPCODE, InHeader.WHISPER).when(Tms282Guards.IN_FIELD)
                .handle("whisper / find -> 265 ChatHandler (LP0305 replies)", this::whisper);
        routes.channel(Tms282ChatGroupRequest.GROUP_MESSAGE_OPCODE, InHeader.GROUP_MESSAGE).when(Tms282Guards.IN_FIELD)
                .handle("buddy / party / guild chat -> 265 ChatHandler (LP0302 lines)", this::groupMessage);
        // multiplayer-0930
        routes.channel(PARTY_INVITABLE_SET_OPCODE, InHeader.PARTY_INVITABLE_SET).when(Tms282Guards.IN_FIELD)
                .handle("party invitation setting u8 -> Char.partyInvitable", this::partyInvitableSet);
        routes.channel(Tms282FriendRequest.OPCODE, InHeader.FRIEND_REQUEST).when(Tms282Guards.IN_FIELD)
                .handle("buddy list request -> 265 FriendHandler (LP00DB replies)", this::friendRequest);
    }

    /** CP01F4 PARTY_INVITABLE_SET [證實 ingame-cp-map: 600F1E0 u8 [obj+180], captured u8 1 at field entry]. */
    static final int PARTY_INVITABLE_SET_OPCODE = 0x01F4;

    // ---- CP01F4 ------------------------------------------------------------------------------------------------

    private void partyInvitableSet(Tms282LoginState session, int op, byte[] packet) {
        if (packet.length != 3) {
            rejected(op, packet, new IllegalArgumentException("CP01F4 must be 1 byte, got " + (packet.length - 2)));
            return;
        }
        Char chr = session.fieldCharacter();
        boolean invitable = packet[2] != 0;
        System.out.printf("[TMS282] CP01F4 party invitable=%s character=%d%n", invitable, chr.getId());
        guarded(chr, op, () -> PartyHandler.handlePartyInvitableSet(chr, new InPacket(new byte[]{packet[2]})));
    }

    // ---- CP0205 ------------------------------------------------------------------------------------------------

    private void friendRequest(Tms282LoginState session, int op, byte[] packet) {
        Tms282FriendRequest request;
        try {
            request = Tms282FriendRequest.parse(packet, charset);
        } catch (IllegalArgumentException e) {
            rejected(op, packet, e);
            return;
        }
        Char chr = session.fieldCharacter();
        switch (request) {
            case Tms282FriendRequest.Add add -> {
                System.out.printf("[TMS282] CP0205 friend add %s group=%s character=%d%n", add.name(), add.group(), chr.getId());
                guarded(chr, op, () -> FriendHandler.handleFriendRequest(chr, new InPacket(add.toGms265Body())));
            }
            case Tms282FriendRequest.ByAccount a -> {
                System.out.printf("[TMS282] CP0205 friend type %d account=%d character=%d%n", a.type(), a.accountId(), chr.getId());
                guarded(chr, op, () -> FriendHandler.handleFriendRequest(chr, new InPacket(a.toGms265Body())));
            }
            case Tms282FriendRequest.RejectAll r -> {
                java.util.List<Integer> pending = new java.util.ArrayList<>();
                try {
                    for (net.swordie.ms.client.social.Friend.Friend f : chr.getAllFriends()) {
                        if (f != null && f.getFlag() == net.swordie.ms.enums.social.Friend.FriendFlag.AccountFriendRequest.getVal()) {
                            pending.add(f.getFriendAccountID());
                        }
                    }
                } catch (RuntimeException e) {
                    System.out.printf("[TMS282] CP0205 reject all: friend list unavailable: %s%n", e);
                }
                System.out.printf("[TMS282] CP0205 friend reject all (%d) character=%d%n", pending.size(), chr.getId());
                for (int account : pending) {
                    byte[] body = new Tms282FriendRequest.ByAccount(4, account, 0, "").toGms265Body();
                    guarded(chr, op, () -> FriendHandler.handleFriendRequest(chr, new InPacket(body)));
                }
            }
            case Tms282FriendRequest.MoveGroups m -> {
                System.out.printf("[TMS282] CP0205 friend group move x%d character=%d%n", m.moves().size(), chr.getId());
                for (Tms282FriendRequest.GroupMove move : m.moves()) {
                    guarded(chr, op, () -> FriendHandler.handleFriendRequest(chr, new InPacket(move.toGms265Body())));
                }
            }
            case Tms282FriendRequest.Other o -> {
                if (o.type() != 0x0C) { // the group-name list is sent unasked after every LP00DB
                    System.out.printf("[TMS282] CP0205 friend type %d len=%d not handled (no 265 counterpart)%n", o.type(), o.rest().length);
                }
            }
        }
    }

    // ---- CP01F2 ------------------------------------------------------------------------------------------------

    private void partyRequest(Tms282LoginState session, int op, byte[] packet) {
        Tms282PartyRequest request;
        try {
            request = Tms282PartyRequest.parse(packet, charset);
        } catch (IllegalArgumentException e) {
            rejected(op, packet, e);
            return;
        }
        Char chr = session.fieldCharacter();
        byte[] body = request.toGms265Body();
        System.out.printf("[TMS282] CP01F2 party request %s%s%n", request, body == null ? " (no 265 counterpart)" : "");
        if (body != null) {
            guarded(chr, op, () -> PartyHandler.handlePartyRequest(chr, new InPacket(body)));
        }
    }

    // ---- CP01F3 ------------------------------------------------------------------------------------------------

    private void partyAnswer(Tms282LoginState session, int op, byte[] packet) {
        Tms282PartyAnswerRequest request;
        try {
            request = Tms282PartyAnswerRequest.parse(packet);
        } catch (IllegalArgumentException e) {
            rejected(op, packet, e);
            return;
        }
        Char chr = session.fieldCharacter();
        System.out.printf("[TMS282] CP01F3 party answer %s%n", request);
        if (request.isInvitationAnswer()) {
            guarded(chr, op, () -> PartyHandler.handlePartyResult(chr, new InPacket(request.toGms265Body())));
        }
    }

    // ---- CP01ED / CP01EA -----------------------------------------------------------------------------------------

    private void whisper(Tms282LoginState session, int op, byte[] packet) {
        Tms282ChatGroupRequest.Whisper request;
        try {
            request = Tms282ChatGroupRequest.parseWhisper(packet, charset);
        } catch (IllegalArgumentException e) {
            rejected(op, packet, e);
            return;
        }
        Char chr = session.fieldCharacter();
        System.out.printf("[TMS282] CP01ED whisper type %d to %s%n", request.type(), request.target());
        guarded(chr, op, () -> ChatHandler.handleWhisper(chr, new InPacket(request.toGms265Body()), InHeader.WHISPER));
    }

    private void groupMessage(Tms282LoginState session, int op, byte[] packet) {
        Tms282ChatGroupRequest.Group request;
        try {
            request = Tms282ChatGroupRequest.parseGroup(packet, charset);
        } catch (IllegalArgumentException e) {
            rejected(op, packet, e);
            return;
        }
        Char chr = session.fieldCharacter();
        System.out.printf("[TMS282] CP01EA group chat type %d to %d character(s)%n", request.type(), request.recipients().size());
        guarded(chr, op, () -> ChatHandler.handleGroupMessage(chr, new InPacket(request.toGms265Body())));
    }

    // ---- shared ------------------------------------------------------------------------------------------------

    static void rejected(int op, byte[] packet, IllegalArgumentException e) {
        System.out.printf("[TMS282] REJECTED CP%04X len=%d: %s (%s)%n", op, packet.length, e.getMessage(),
                packet.length <= 48 ? Arrays.toString(packet) : "");
    }

    static void guarded(Char chr, int op, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
            System.out.printf("[TMS282] FAILED CP%04X character=%d: %s%n", op, chr.getId(), e);
        }
    }
}
