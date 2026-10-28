package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.Tms282SummonShootRequest;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.handlers.life.SummonedHandler;

/** CP02EB summon shoot objects (Altar of Annihilation curses, bam-1006): the 265 handler with the identical body. */
final class Tms282SummonShootHandlers {
    void register(Tms282Routes routes) {
        routes.channel(Tms282SummonShootRequest.OPCODE, InHeader.SUMMONED_CREATE_SHOOT_OBJ).when(Tms282Guards.IN_FIELD)
                .handle("summon shoot objects -> 265 SummonedHandler.handleSummonedCreateShootOBj", (s, op, p) -> {
                    Char chr = s.fieldCharacter();
                    Tms282SummonShootRequest r;
                    try {
                        r = Tms282SummonShootRequest.parse(p);
                    } catch (IllegalArgumentException e) {
                        Tms282SocialHandlers.rejected(op, p, e);
                        return;
                    }
                    System.out.printf("[TMS282] CP02EB summon %d shoot skill=%d objects=%d character=%d%n",
                            r.summonId(), r.skillId(), r.count(), chr.getId());
                    Tms282SocialHandlers.guarded(chr, op,
                            () -> SummonedHandler.handleSummonedCreateShootOBj(chr, new InPacket(r.toGms265Body())));
                });
    }
}
