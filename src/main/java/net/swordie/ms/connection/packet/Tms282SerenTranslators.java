package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.handlers.header.OutHeader;

import java.util.Optional;

/**
 * 受選的賽蓮 (newboss-0930, research/tms2823/newboss-0930/README.md): {@link SerenPacket}'s 265 packets in their 282
 * form. 282 dispatches them through the Seren field classes (Map.wz info/fieldType 270 -> 2F92AB0, 271 -> 2F94A80),
 * so the server only sends them from Seren fields ({@code net.swordie.ms.world.boss.SerenFight}).
 * <ul>
 *     <li>SEREN_STUN_GAUGE -> 0x8AA, {@code i32 max, i32 current} (8 bytes).</li>
 *     <li>SEREN_TIMER -> 0x8AB, {@code buf4 type} + type 0/1: 5 x i32, type 2: u8, type 3: i32.</li>
 *     <li>SEREN_BACKGROUND_CHANGE -> 0x8A9, {@code i32 timeOfDay} (4 bytes).</li>
 * </ul>
 * Traced CLEAN with the emulated 282.3 client (daily_trace.py: 508E230 / 508E270 directly and through 2F92AB0 /
 * 2F94A80 with the UI global patched in, 0x8A9 through 2F94A80). Not sent / not translated (layouts known, server
 * behaviour not implemented): 0x8AC (phase-1 MobSkill 264 area list), 0x8AD (clear that list), 0x8AE (MobSkill 263
 * object), 0x8AF. Malformed bodies are declined. {@code -Dtms282.seren.enabled=false} turns the contributor off.
 * Registered as one entry of {@link Tms282BossTranslators#PARTS} (bossmech3-0930).
 */
public final class Tms282SerenTranslators implements Tms282LegacyPackets.Contributor {
    public static final Tms282SerenTranslators INSTANCE = new Tms282SerenTranslators();

    public static final int SEREN_BACKGROUND_CHANGE = 0x08A9;
    public static final int SEREN_STUN_GAUGE = 0x08AA;
    public static final int SEREN_TIMER = 0x08AB;

    private Tms282SerenTranslators() {
    }

    public static boolean enabled() {
        return !"false".equalsIgnoreCase(System.getProperty("tms282.seren.enabled", "true"));
    }

    @Override
    public void contribute(Tms282LegacyPackets.Builder builder) {
        if (!enabled()) {
            return;
        }
        builder.on(OutHeader.SEREN_STUN_GAUGE, p -> Tms282BossFieldTranslators.copy(p, SEREN_STUN_GAUGE, 8));
        builder.on(OutHeader.SEREN_BACKGROUND_CHANGE, p -> Tms282BossFieldTranslators.copy(p, SEREN_BACKGROUND_CHANGE, 4));
        builder.on(OutHeader.SEREN_TIMER, Tms282SerenTranslators::timer);
    }

    /** SEREN_TIMER: the type decides the body length (508E270); unknown types and wrong lengths are declined. */
    static Optional<OutPacket> timer(OutPacket legacy) {
        Tms282LegacyPackets.Body body = Tms282LegacyPackets.Body.of(legacy);
        if (body.remaining() < 4) {
            return Optional.empty();
        }
        int type = body.i32();
        int expected = switch (type) {
            case SerenPacket.TIMER_START, SerenPacket.TIMER_REPLAN -> 20;
            case SerenPacket.TIMER_PAUSE -> 1;
            case SerenPacket.TIMER_HAND -> 4;
            default -> -1;
        };
        byte[] rest = body.rest();
        if (expected < 0 || rest.length != expected) {
            return Optional.empty();
        }
        OutPacket out = Tms282LoginPacketSupport.packet(SEREN_TIMER);
        out.encodeInt(type);
        out.encodeArr(rest);
        return Optional.of(out);
    }
}
