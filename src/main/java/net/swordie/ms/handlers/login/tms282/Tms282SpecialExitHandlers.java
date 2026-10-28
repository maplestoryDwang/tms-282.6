package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.Tms282SpecialExitRequest;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.handlers.user.UserHandler;

/** CP035D special exit (the 離開 button of event maps such as 帽子裡的世界): the 265 handler with the identical body. */
final class Tms282SpecialExitHandlers {
    void register(Tms282Routes routes) {
        routes.channel(Tms282SpecialExitRequest.OPCODE, InHeader.USER_SPECIAL_EXIT).when(Tms282Guards.IN_FIELD)
                .handle("special exit -> 265 UserHandler.handleUserSpecialExit", (s, op, p) -> {
                    Char chr = s.fieldCharacter();
                    Tms282SpecialExitRequest r;
                    try {
                        r = Tms282SpecialExitRequest.parse(p);
                    } catch (IllegalArgumentException e) {
                        Tms282SocialHandlers.rejected(op, p, e);
                        chr.dispose();
                        return;
                    }
                    System.out.printf("[TMS282] CP035D special exit type=%d value=%d field=%d character=%d%n",
                            r.type(), r.value(), chr.getFieldID(), chr.getId());
                    Tms282SocialHandlers.guarded(chr, op,
                            () -> UserHandler.handleUserSpecialExit(chr, new InPacket(r.toGms265Body())));
                });
    }
}
