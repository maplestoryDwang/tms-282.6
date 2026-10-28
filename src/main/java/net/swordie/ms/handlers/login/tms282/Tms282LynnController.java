package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.Client;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.netty.NettyClient;
import net.swordie.ms.connection.packet.Tms282LynnPackets;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;
import net.swordie.ms.constants.JobConstants;

/**
 * Sends the Lynn controller actor ({@link Tms282LynnPackets}) after every 282 SetField of a Lynn character, the way
 * {@link Tms282FamiliarWindow} sends the familiar controller. Switch: -Dtms282.lynn.controller=false.
 */
public final class Tms282LynnController {
    private static final boolean ENABLED = Boolean.parseBoolean(System.getProperty("tms282.lynn.controller", "true"));
    /** Actor id range of this feature in the user's world (familiar: 0x50000000 + character). */
    private static final int ACTOR_BASE = 0x51000000;

    private Tms282LynnController() {
    }

    /** Called only after a packet passed the outbound guard and was written to the socket queue. */
    public static void afterSent(NettyClient client, OutPacket packet) {
        if (!ENABLED || Protocol.active().version() != ProtocolVersion.TMS_282_3
                || packet.getBuiltFor() != ProtocolVersion.TMS_282_3 || packet.getHeader() != 0x02EF
                || !(client instanceof Client c) || !c.getCh().isActive() || c.getCurrentState() != Client.IN_FIELD
                || c.getChr() == null || c.getChr().getId() <= 0 || c.getUser() == null || c.getUser().getId() <= 0
                || !JobConstants.isLynn(c.getChr().getJob()) || !Tms282LoginState.of(c).isChannel()) {
            return;
        }
        int character = c.getChr().getId();
        if (character > 0x00FFFFFF) {
            return;
        }
        int actor = ACTOR_BASE + character;
        c.write(Tms282LynnPackets.bootstrap(character, Integer.toUnsignedLong(actor), c.getUser().getId()));
        System.out.printf("[TMS282] Lynn controller bootstrap char=%d actor=%08X field=%d%n", character, actor,
                c.getChr().getFieldID());
    }
}
