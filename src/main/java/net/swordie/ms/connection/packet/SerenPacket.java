package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.handlers.header.OutHeader;

/**
 * 受選的賽蓮 field packets (265 builders; the 265 headers are UnkValue, so only the 282 profile sends them through
 * {@link Tms282SerenTranslators}). Layouts are the 282.3/282.4 client's (research/tms2823/newboss-0930/README.md):
 * <ul>
 *     <li>SEREN_STUN_GAUGE -> LP08AA, handled by the Seren UI (UI 0x509, global 9EE15D8, 508E230) in both the phase-1
 *     (fieldType 270, 2F92AB0) and phase-2 (271, 2F94A80 -> base 2F92AB0) fields: {@code i32 max, i32 current}.
 *     The UI draws current/max (5092790); its default max is 1000 (508D48F).</li>
 *     <li>SEREN_TIMER -> LP08AB, phase-2 field only (2F94A80 -> 508E270), {@code buf4 type} then:
 *     type 0 {@code i32 cycleMs, i32 noonDeg, i32 sunsetDeg, i32 midnightDeg, i32 dawnDeg} (start clock; hand at
 *     360 - noon/2, 1 degree per cycleMs/360, stops at the end of the current segment);
 *     type 1 {@code 4 x i32 degrees, i32 dialRotation} (re-plan); type 2 {@code u8 paused};
 *     type 3 {@code i32 handAngle} (forward only).</li>
 *     <li>SEREN_BACKGROUND_CHANGE -> LP08A9, phase-2 field only (2F94CD0): {@code i32 timeOfDay} 1 noon, 2 sunset,
 *     3 midnight, 4 dawn: background tint (3DC5940), BossSeren.img/2PhaseBGM track and the hand to that segment.</li>
 * </ul>
 */
public final class SerenPacket {
    public static final int TIMER_START = 0;
    public static final int TIMER_REPLAN = 1;
    public static final int TIMER_PAUSE = 2;
    public static final int TIMER_HAND = 3;

    private SerenPacket() {
    }

    public static OutPacket stunGauge(int max, int current) {
        OutPacket out = new OutPacket(OutHeader.SEREN_STUN_GAUGE);
        out.encodeInt(max);
        out.encodeInt(current);
        return out;
    }

    public static OutPacket timerStart(int cycleMs, int noon, int sunset, int midnight, int dawn) {
        OutPacket out = new OutPacket(OutHeader.SEREN_TIMER);
        out.encodeInt(TIMER_START);
        out.encodeInt(cycleMs);
        out.encodeInt(noon);
        out.encodeInt(sunset);
        out.encodeInt(midnight);
        out.encodeInt(dawn);
        return out;
    }

    public static OutPacket timerPause(boolean paused) {
        OutPacket out = new OutPacket(OutHeader.SEREN_TIMER);
        out.encodeInt(TIMER_PAUSE);
        out.encodeByte(paused ? 1 : 0);
        return out;
    }

    public static OutPacket timeOfDay(int timeOfDay) {
        OutPacket out = new OutPacket(OutHeader.SEREN_BACKGROUND_CHANGE);
        out.encodeInt(timeOfDay);
        return out;
    }
}
