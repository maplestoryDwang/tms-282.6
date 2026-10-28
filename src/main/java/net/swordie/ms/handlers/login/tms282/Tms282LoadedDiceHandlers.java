package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.character.tms282.Tms282LoadedDice;
import net.swordie.ms.connection.packet.Tms282LoadedDiceRequest;
import net.swordie.ms.handlers.header.InHeader;

/** Native58843D5 selection i32 plus346A070 transport; no extra cost or guessed ACK. */
public final class Tms282LoadedDiceHandlers {
    public void register(Tms282Routes routes) {
        routes.channel(Tms282LoadedDiceRequest.OPCODE, InHeader.CHOOSE_LOAD_DICE_NUMBER_REQUEST).when(Tms282Guards.IN_FIELD)
                .handle("owned native Loaded Dice choice -> actual dice roll", (s, op, p) -> {
                    try {
                        var chr = s.fieldCharacter();
                        if (chr.getClient() != s.client()) throw new IllegalArgumentException("Stale Loaded Dice session");
                        Tms282LoadedDice.select(chr, Tms282LoadedDiceRequest.parse(p).choice());
                    }
                    catch (IllegalArgumentException rejected) {
                        System.out.println("[TMS282] REJECTED CP03D5: " + rejected.getMessage());
                    }
                });
    }

}
