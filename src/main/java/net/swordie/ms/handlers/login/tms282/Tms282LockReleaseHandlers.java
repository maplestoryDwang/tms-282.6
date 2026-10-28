package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.connection.packet.Tms282StatPackets;

import java.util.HexFormat;

/**
 * 282 requests whose sender locks the client (5E04DA0(ctx, 1)) but whose meaning is not proven yet: the packet is
 * logged and answered with the LP009D unlock only, so the client never stays frozen. Nothing is changed server side.
 * <p>
 * No request is routed here now: CP0239 was the hyper skill up after all (the live value 0x08789851 = 142121041,
 * Kinesis's hyper skill, had been misread as 142120017) and is {@link Tms282HyperStatHandlers}'s.
 */
public final class Tms282LockReleaseHandlers {
    void register(Tms282Routes routes) {
        // kept for the next unproven locking request: routes.channel(op, InHeader.CPxxxx).handle(..., this::onLockingRequest)
    }

    public void onLockingRequest(Tms282LoginState session, int op, byte[] packet) {
        session.send(Tms282StatPackets.unlock());
        var chr = session.fieldCharacter();
        System.out.printf("[TMS282] CP%04X locking request (meaning unproven) character=%s body=%s; SENT LP009D unlock%n",
                op, chr == null ? "-" : chr.getId(), HexFormat.of().formatHex(packet, 2, packet.length));
    }
}
