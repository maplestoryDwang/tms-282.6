package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.netty.NettyClient;
import net.swordie.ms.connection.packet.Tms282MonolithPackets;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;
import net.swordie.ms.console.config.ServerRuntimeConfig;
import net.swordie.ms.world.field.Field;
import net.swordie.ms.world.field.Tms282Monolith;

/**
 * Sends the client its ASkillMonolithManager after each SetField so the 輪迴 skill key (80011261) passes the client's
 * own MonolithBlockField check ({@link Tms282MonolithPackets}). Only for characters wearing the totem 1202193, so an
 * unverified actor never reaches anyone else. Actor IDs 60000001..6FFFFFFF keyed by character ID (the familiar
 * window owns 5xxxxxxx). Off with {@code tms282.monolith.manager.enabled=false}.
 */
public final class Tms282MonolithManager {
    private static final String KEY = "tms282.monolith.manager.enabled";
    private static final boolean ENABLED = !"false".equalsIgnoreCase(System.getProperty(KEY, ServerRuntimeConfig.stringValue(KEY)));

    private Tms282MonolithManager() { }

    /** Mob maps outside instances; towns and mobless maps keep the client's refusal message. */
    static boolean summonable(Char chr, Field field) {
        return field != null && !field.isTown() && !field.getMobGens().isEmpty() && chr.getInstance() == null;
    }

    /** Called only after a packet passed the outbound guard and was written to the socket queue. */
    public static void afterSent(NettyClient client, OutPacket packet) {
        if (!ENABLED || Protocol.active().version() != ProtocolVersion.TMS_282_3
                || packet.getBuiltFor() != ProtocolVersion.TMS_282_3 || packet.getHeader() != 0x02EF
                || !(client instanceof Client c) || !c.getCh().isActive() || c.getCurrentState() != Client.IN_FIELD
                || c.getChr() == null || c.getChr().getId() <= 0 || c.getUser() == null || c.getUser().getId() <= 0
                || !Tms282LoginState.of(c).isChannel()) return;
        Char chr = c.getChr();
        if (chr.getId() > 0x0FFFFFFF || Tms282Monolith.tierOf(chr) <= 0) return;
        boolean summonable = summonable(chr, chr.getField());
        int actor = 0x60000000 + chr.getId();
        c.write(Tms282MonolithPackets.manager(actor, c.getUser().getId(), chr.getId(), summonable));
        System.out.printf("[TMS282] Monolith manager char=%d actor=%08X field=%d bSummonableField=%b (LP0392, presumed leaf 20)%n",
                chr.getId(), actor, chr.getFieldID(), summonable);
    }
}
