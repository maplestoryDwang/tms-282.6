package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.character.tms282.Tms282RuneRuntime;
import net.swordie.ms.connection.packet.Tms282RuneRequests;
import net.swordie.ms.handlers.header.InHeader;

final class Tms282RuneHandlers {
    void register(Tms282Routes routes) {
        routes.channel(0x580, InHeader.RUNE_STONE_USE_REQ).when(Tms282Guards.IN_FIELD).handle("rune challenge", this::handle);
        routes.channel(0x581, InHeader.RUNE_STONE_SKILL_REQ).when(Tms282Guards.IN_FIELD).handle("validated rune completion", this::handle);
        routes.channel(0x582, InHeader.RUNE_STONE_USE_CHK).when(Tms282Guards.IN_FIELD).handle("rune direction", this::handle);
    }
    private void handle(Tms282LoginState session, int op, byte[] packet) {
        var chr = session.fieldCharacter();
        try {
            switch (op) {
                case 0x580 -> Tms282RuneRuntime.use(chr, Tms282RuneRequests.use(packet));
                case 0x581 -> Tms282RuneRuntime.finish(chr, Tms282RuneRequests.finish(packet));
                case 0x582 -> Tms282RuneRuntime.direction(chr, Tms282RuneRequests.direction(packet));
                default -> throw new IllegalArgumentException("Unexpected rune opcode");
            }
        } catch (IllegalArgumentException ex) {
            Tms282RuneRuntime.cancel(chr);
            System.out.printf("[TMS282] CP%04X rune rejected: %s%n", op, ex.getMessage());
        } finally { chr.dispose(); }
    }
}
