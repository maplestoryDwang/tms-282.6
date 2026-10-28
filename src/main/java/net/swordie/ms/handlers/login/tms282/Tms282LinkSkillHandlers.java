package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.Tms282LinkSkillRequest;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.handlers.user.SkillHandler;

/** CP0211 link skill window (research/tms2823/link-skill-1006): the 265 handler with the identical body. */
final class Tms282LinkSkillHandlers {
    void register(Tms282Routes routes) {
        routes.channel(Tms282LinkSkillRequest.OPCODE, InHeader.LINK_SKILL_REQUEST).when(Tms282Guards.IN_FIELD)
                .handle("link skill -> 265 SkillHandler.handleLinkSkillRequest (LP0251 replies)", (s, op, p) -> {
                    Char chr = s.fieldCharacter();
                    Tms282LinkSkillRequest r;
                    try {
                        r = Tms282LinkSkillRequest.parse(p);
                    } catch (IllegalArgumentException e) {
                        Tms282SocialHandlers.rejected(op, p, e);
                        chr.dispose();
                        return;
                    }
                    System.out.printf("[TMS282] CP0211 link skill type=%d preset=%d skills=%s character=%d%n",
                            r.type(), r.preset(), r.skills(), chr.getId());
                    Tms282SocialHandlers.guarded(chr, op, () -> SkillHandler.handleLinkSkillRequest(chr, new InPacket(r.toGms265Body())));
                });
    }
}
