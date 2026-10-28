package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.handlers.life.MobHandler;

import java.util.Arrays;
import java.util.function.BiConsumer;

/**
 * Boss-mechanics phase (2026-09-30, research/tms2823/boss-mech-0930 section 3): the boss CPs whose 265 handlers exist
 * (MobHandler) but had no TMS 282.3 route. Senders found in the 282.3 client (282.4 identical); none of these opcodes
 * is in the 346A070 tail-u32 list, so the body is exactly what the sender encodes:
 * <ul>
 *     <li>CP0648 WILL_USE_MOON_GAUGE, sender 3A34A50 (Will base field; after the moon-gauge UI 4BAEB60): empty.</li>
 *     <li>CP0649 WILL_BEHOLDER_EYES_HIT, sender 3A318F0 (Will eye manager): empty.</li>
 *     <li>CP064A WILL_SPIDER_WEB_HIT, sender 3A4C45E (Will web code): {@code i32 webObjectId, u8 seal, i32}. The 265
 *     handler reads i32 and (only when it damages) u8; the trailing i32 is ignored.</li>
 *     <li>CP0608 DEMIAN_OBJECT_NODE_END, sender 206DD00 (Demian sword path): {@code i32 swordObjectId, i16 pathIdx,
 *     i16 nodeIdx, 8 bytes sword position, 8 bytes target position} = the 265 handler's reads.</li>
 * </ul>
 * Registration (bossmech2-0930): one line in {@link Tms282BossMechHandlers#register} (the boss CP collection of
 * bossmech1-0930), not in Tms282LoginOverlay. Research original: boss-mech-0930/patches/.../Tms282BossMechHandlers.java
 * (renamed here because bossmech1 already owns that class name as the collection).
 */
final class Tms282BossFieldHandlers {
    static final int WILL_USE_MOON_GAUGE = 0x0648;
    static final int WILL_BEHOLDER_EYES_HIT = 0x0649;
    static final int WILL_SPIDER_WEB_HIT = 0x064A;
    static final int DEMIAN_OBJECT_NODE_END = 0x0608;

    void register(Tms282Routes routes) {
        route(routes, WILL_USE_MOON_GAUGE, InHeader.WILL_USE_MOON_GAUGE, 0, 0,
                "Will moon gauge use -> 265 MobHandler.handleWillUseMoonGauge", MobHandler::handleWillUseMoonGauge);
        route(routes, WILL_BEHOLDER_EYES_HIT, InHeader.WILL_BEHOLDER_EYES_HIT, 0, 0,
                "Will eye hit -> 265 MobHandler.handleWillBeholderEyesHit", MobHandler::handleWillBeholderEyesHit);
        route(routes, WILL_SPIDER_WEB_HIT, InHeader.WILL_SPIDER_WEB_HIT, 9, 9,
                "Will web hit -> 265 MobHandler.handleWillSpiderWebHit", MobHandler::handleWillSpiderWebHit);
        route(routes, DEMIAN_OBJECT_NODE_END, InHeader.DEMIAN_OBJECT_NODE_END, 24, 24,
                "Demian sword node end -> 265 MobHandler.handleDemianObjectNodeEnd", MobHandler::handleDemianObjectNodeEnd);
    }

    /** Body (after the 2-byte opcode) of {@code min..max} bytes, handed unchanged to the 265 handler. */
    private static void route(Tms282Routes routes, int opcode, InHeader header, int min, int max, String action,
                              BiConsumer<Char, InPacket> handler) {
        routes.channel(opcode, header).when(Tms282Guards.IN_FIELD).handle(action, (s, op, p) -> {
            int len = p.length - 2;
            if (len < min || len > max) {
                Tms282SocialHandlers.rejected(op, p, new IllegalArgumentException("body " + len + " bytes, want "
                        + min + (max == min ? "" : ".." + max)));
                return;
            }
            Char chr = s.fieldCharacter();
            byte[] body = Arrays.copyOfRange(p, 2, p.length);
            Tms282SocialHandlers.guarded(chr, op, () -> handler.accept(chr, new InPacket(body)));
        });
    }
}
