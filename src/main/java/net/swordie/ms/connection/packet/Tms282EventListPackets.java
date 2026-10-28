package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.protocol.ProtocolVersion;
import net.swordie.ms.world.event.EventListData;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;

/**
 * TMS 282.3 in-game event list (UIEventList), LP {@value #EVENT_LIST} (research/tms2823/eventlist-1005, static RE of
 * the 282.4 client: CWvsContext case 5DF7E2A -> singleton [9EF7318] -> 50CEAB0; fixtures traced CLEAN; not live
 * tested). The 265 EVENT_LIST(370) body throws in the 282 reader (DecodeStr at 1041D39), so 282 needs this codec.
 * <pre>
 * i32 nGroups, nGroups x group, trailer
 * group   (1044400): Str, Str, Str, u8, i32, i32 (meaning unknown; sent empty), i32 nEvents, nEvents x event
 * event   (1041D10): Str name, desc, requirements, period, fromDate, toDate, fromDate2;
 *                    i32 scriptType, Str scriptName; i32 uiType, Str uiName; u8 unkBool1..3;
 *                    i32 unkVal (std::map key, must be unique), i32 unkVal2, i32 unkVal3, i32 unkVal282 (282);
 *                    Str unkStr, u8 unkVal4, Str unkStr282 (282; 265 had u8 unkVal5); Str icon, Str backgrnd;
 *                    i32 levelReq; 3 x vec&lt;i32&gt; (empty); condition; i32 0 (no {i32,Str,Str,i32,condition});
 *                    condition; vec&lt;i32&gt; rewards; u8 unkBool, u8 unkBool282, i32 unkInt282 (282)
 * condition (103E5A0): tag 0 = i32 0, u8 0 (the only one sent)
 * trailer: Str, i32, Str, Str, vec&lt;i32&gt; (meaning unknown; sent empty)
 * </pre>
 * Strings are in the client code page ({@code tms282.nameCharset}, MS950). All events go into one group with an empty
 * header, as in the traced fixtures.
 */
public final class Tms282EventListPackets {
    public static final int EVENT_LIST = 0x0175;

    private Tms282EventListPackets() {
    }

    /** LP 0x0175 with {@code events} in one group (null entries skipped). */
    public static OutPacket eventList(List<EventListData> events, Charset charset) {
        Objects.requireNonNull(events, "events");
        Objects.requireNonNull(charset, "charset");
        List<EventListData> list = events.stream().filter(Objects::nonNull).toList();
        OutPacket out = OutPacket.forProfile(ProtocolVersion.TMS_282_3, EVENT_LIST);
        if (list.isEmpty()) {
            out.encodeInt(0);
        } else {
            out.encodeInt(1);
            group(out, list, charset);
        }
        // trailer
        str(out, "", charset);
        out.encodeInt(0);
        str(out, "", charset);
        str(out, "", charset);
        out.encodeInt(0);
        return out;
    }

    private static void group(OutPacket out, List<EventListData> events, Charset cs) {
        str(out, "", cs);
        str(out, "", cs);
        str(out, "", cs);
        out.encodeByte(0);
        out.encodeInt(0);
        out.encodeInt(0);
        out.encodeInt(events.size());
        for (EventListData e : events) {
            event(out, e, cs);
        }
    }

    static void event(OutPacket out, EventListData e, Charset cs) {
        str(out, e.name, cs);
        str(out, e.desc, cs);
        str(out, e.requirements, cs);
        str(out, e.period, cs);
        str(out, e.fromDate, cs);
        str(out, e.toDate, cs);
        str(out, e.fromDate2, cs);
        out.encodeInt(e.scriptType);
        str(out, e.scriptName, cs);
        out.encodeInt(e.uiType);
        str(out, e.uiName, cs);
        out.encodeByte(e.unkBool1);
        out.encodeByte(e.unkBool2);
        out.encodeByte(e.unkBool3);
        out.encodeInt(e.unkVal);
        out.encodeInt(e.unkVal2);
        out.encodeInt(e.unkVal3);
        out.encodeInt(e.unkVal282);
        str(out, e.unkStr, cs);
        out.encodeByte(e.unkVal4);
        str(out, e.unkStr282, cs);
        str(out, e.icon, cs);
        str(out, e.backgrnd, cs);
        out.encodeInt(e.levelReq);
        out.encodeInt(0); // vec<i32>
        out.encodeInt(0); // vec<i32>
        out.encodeInt(0); // vec<i32>
        condition(out);
        out.encodeInt(0); // no {i32, Str, Str, i32, condition}
        condition(out);
        int[] rewards = e.rewards == null ? new int[0] : e.rewards;
        out.encodeInt(rewards.length);
        for (int id : rewards) {
            out.encodeInt(id);
        }
        out.encodeByte(e.unkBool);
        out.encodeByte(e.unkBool282);
        out.encodeInt(e.unkInt282);
    }

    /** 103E5A0 tag 0: no condition. */
    private static void condition(OutPacket out) {
        out.encodeInt(0);
        out.encodeByte(0);
    }

    /** Client DecodeStr (F8D3D0): u16 byte length + bytes in {@code cs}; null = "". */
    private static void str(OutPacket out, String s, Charset cs) {
        byte[] b = (s == null ? "" : s).getBytes(cs);
        if (b.length > 0xFFFF) {
            throw new IllegalArgumentException("event list string too long");
        }
        out.encodeShort(b.length);
        out.encodeArr(b);
    }
}
