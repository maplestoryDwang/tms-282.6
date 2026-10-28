package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.Account;
import net.swordie.ms.client.User;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.packet.Tms282CharacterSwitchPackets;
import net.swordie.ms.connection.packet.Tms282CharacterSwitchRequest;
import net.swordie.ms.connection.packet.Tms282ReloginRequest;
import net.swordie.ms.handlers.header.InHeader;
import org.mindrot.jbcrypt.BCrypt;

import java.nio.charset.Charset;
import java.util.HexFormat;
import java.util.Objects;

/**
 * In-game character switch (TMS 282.3 only):
 * <ul>
 *     <li>CP04A7 ({@link Tms282CharacterSwitchRequest}) -> {@link Rules#decide} -> LP028D
 *     ({@link Tms282CharacterSwitchPackets}). Every path answers, so the client's pending flag (ctx+0x4138) is cleared.
 *     On acceptance it sends CP024B; that second phase verifies the account and approved target, saves the old
 *     character, and replies LP015F to start relogin. The empty cookie preserves the client's existing credentials.</li>
 *     <li>CP077C is a Zero-job-only report from 5E647C0, not the general switch continuation.</li>
 * </ul>
 * The second password: empty means the client skipped it (flag 0 shows SP[0x4345]); a typed one is checked against
 * the account's BCrypt PIC like the login server does; a wrong one answers 1 (「請確認第2道密碼後再試一次。」).
 */
final class Tms282CharacterSwitchHandlers {
    static final int CP077C = 0x077C;

    /** The decision, pure and testable offline. */
    static final class Rules {
        private Rules() {
        }

        /** LP028D result code for switching {@code chr} to {@code request.characterId()}. */
        static int decide(Char chr, Tms282CharacterSwitchRequest request) {
            if (chr == null || request.characterId() == chr.getId()) {
                return Tms282CharacterSwitchPackets.UNAVAILABLE;
            }
            Account account = chr.getAccount();
            if (account == null || !account.hasCharacter(request.characterId())) {
                return Tms282CharacterSwitchPackets.UNAVAILABLE;
            }
            Char target = account.getCharById(request.characterId());
            if (target == null || target.getDeletionStartTime() > 0) {
                return Tms282CharacterSwitchPackets.UNAVAILABLE;
            }
            if (!request.secondaryPassword().isEmpty() && !picMatches(chr.getUser(), request.secondaryPassword())) {
                return Tms282CharacterSwitchPackets.WRONG_SECONDARY_PASSWORD;
            }
            return Tms282CharacterSwitchPackets.ACCEPTED;
        }

        static boolean picMatches(User user, String typed) {
            String pic = user == null ? null : user.getPic();
            if (pic == null || pic.isBlank()) {
                return false;
            }
            try {
                return BCrypt.checkpw(typed, pic);
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
    }

    private final Charset charset;
    private final java.util.function.Consumer<net.swordie.ms.client.Client> finishSession;
    private final Tms282SwitchTickets tickets;

    Tms282CharacterSwitchHandlers(Tms282LoginContext context) {
        this(context.settings().nameCharset(), c -> c.finalizeSession(false), context.switchTickets());
    }

    Tms282CharacterSwitchHandlers(Charset charset) {
        this(charset,c -> c.finalizeSession(false));
    }

    Tms282CharacterSwitchHandlers(Charset charset, java.util.function.Consumer<net.swordie.ms.client.Client> finishSession) {
        this(charset, finishSession, new Tms282SwitchTickets());
    }

    Tms282CharacterSwitchHandlers(Charset charset, java.util.function.Consumer<net.swordie.ms.client.Client> finishSession,
                                 Tms282SwitchTickets tickets) {
        this.charset = Objects.requireNonNull(charset, "charset");
        this.finishSession=Objects.requireNonNull(finishSession,"finishSession");
        this.tickets=Objects.requireNonNull(tickets,"tickets");
    }

    void register(Tms282Routes routes) {
        routes.channel(Tms282ReloginRequest.OPCODE, InHeader.REQUEST_RELOGIN_COOKIE).when(Tms282Guards.IN_FIELD)
                .handle("finish approved character switch -> LP015F", this::relogin);
        routes.channel(Tms282CharacterSwitchRequest.OPCODE, InHeader.CP04A7).when(Tms282Guards.IN_FIELD)
                .handle("change character -> LP028D", this::changeCharacter);
        routes.channel(CP077C, InHeader.CP077C)
                .handle("character switch report (after LP028D 0), recorded only", this::report);
    }

    private void changeCharacter(Tms282LoginState session, int op, byte[] packet) {
        Tms282CharacterSwitchRequest request;
        try {
            request = Tms282CharacterSwitchRequest.parse(packet, charset);
        } catch (IllegalArgumentException e) {
            System.out.printf("[TMS282] REJECTED CP04A7 len=%d: %s%n", packet.length, e.getMessage());
            session.send(Tms282CharacterSwitchPackets.refused(Tms282CharacterSwitchPackets.UNAVAILABLE));
            return;
        }
        Char chr = session.fieldCharacter();
        int result;
        try {
            result = Rules.decide(chr, request);
        } catch (RuntimeException e) {
            System.out.printf("[TMS282] %s: check failed: %s%n", request, e);
            result = Tms282CharacterSwitchPackets.UNAVAILABLE;
        }
        OutPacket reply = result == Tms282CharacterSwitchPackets.ACCEPTED
                ? Tms282CharacterSwitchPackets.accepted(request.characterId(), request.flag(), request.secondaryPassword(),
                charset)
                : Tms282CharacterSwitchPackets.refused(result);
        System.out.printf("[TMS282] %s by character %d -> LP028D result %d%n", request,
                chr == null ? -1 : chr.getId(), result);
        if(result==Tms282CharacterSwitchPackets.ACCEPTED) session.approveSwitch(request.characterId());
        session.send(reply);
    }

    private void relogin(Tms282LoginState session,int op,byte[] packet) {
        try {
            var request=Tms282ReloginRequest.parse(packet,charset);
            Char chr=session.fieldCharacter();
            boolean accountMatches=chr.getUser()!=null && chr.getUser().getName().equals(request.accountName());
            if(request.characterId()==0) {
                // 遊戲結束 (main menu exit) sends CP024B with target 0 and no password (live 09-29: 4b02 0600 "tms282"
                // 00000000). It is not a character switch: save/log out and answer LP015F with an empty cookie and
                // target 0 (5E277F0 reads no password for 0, stores the target via 5FAB5C0, then 5E81400(ctx, 2)
                // leaves the field). Refusing it showed 「目前無法進行」.
                if(!accountMatches) {
                    session.send(Tms282CharacterSwitchPackets.refused(Tms282CharacterSwitchPackets.UNAVAILABLE));
                    return;
                }
                finishSession.accept(session.client());
                session.leaveFieldForRelogin();
                session.send(Tms282CharacterSwitchPackets.relogin(0,"",charset));
                System.out.printf("[TMS282] CP024B exit (target 0) -> LP015F; character %d saved/logged out%n",chr.getId());
                return;
            }
            int result=Rules.decide(chr,new Tms282CharacterSwitchRequest(request.characterId(),request.secondaryPassword(),0));
            if(!accountMatches || result!=Tms282CharacterSwitchPackets.ACCEPTED || !session.consumeSwitch(request.characterId())) {
                session.send(Tms282CharacterSwitchPackets.refused(Tms282CharacterSwitchPackets.UNAVAILABLE));
                return;
            }
            int userId=chr.getUser().getId(), accountId=chr.getAccount().getId();
            int worldId=chr.getAccount().getWorldId();
            int channelId=Math.max(0, Byte.toUnsignedInt(session.client().getChannel())-1);
            boolean filesVerified=session.fileReportVerified();
            String peerAddress=session.client().getIP();
            finishSession.accept(session.client()); // Save/log out the old character before the new login starts.
            session.leaveFieldForRelogin();
            // Separate one-time cookies authenticate native CP0073 and select this target via CP0079.
            // A nonempty login cookie also preserves the client's automatic world-selection path.
            String token=tickets.issue(userId,accountId,request.characterId(),worldId,channelId,filesVerified,peerAddress);
            String authCookie=tickets.authCookieFor(userId);
            if (authCookie.isEmpty()) throw new IllegalArgumentException("Missing relogin credential");
            OutPacket reply=Tms282CharacterSwitchPackets.reloginHandoff(request.characterId(),token,
                    authCookie);
            session.send(reply);
            System.out.printf("[TMS282] CP024B approved target=%d -> LP015F single-use reselect; previous character saved/logged out%n",request.characterId());
        } catch(IllegalArgumentException e) {
            System.out.printf("[TMS282] REJECTED CP024B len=%d (invalid relogin data)%n",packet.length);
            session.send(Tms282CharacterSwitchPackets.refused(Tms282CharacterSwitchPackets.UNAVAILABLE));
        }
    }

    private void report(Tms282LoginState session, int op, byte[] packet) {
        System.out.printf("[TMS282] REPORT CP077C character switch %s%n", HexFormat.of().formatHex(packet));
    }
}
