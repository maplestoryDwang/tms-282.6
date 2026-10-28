package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.handlers.header.OutHeader;

import java.util.Optional;

/**
 * FIELD_SYNC_DYNAMIC_FOOTHOLD (265 0x0306) -> LP 0x0321 (zakum-0930, research/tms2823/zakum-crash-0930/README.md).
 * <p>
 * The 282 opcode row is STATIC_CONFIRMED only, so the profile has no opcode for the 265 header and the guard dropped
 * every sync (Zakum altar platforms {@code Field.createZakumStand}, Gollux / BossHelper phases, script
 * {@code syncDynamicFootHold}). Layout {@code i32 n, n x (Str name, u8 flag, [flag == 0: i32 state, i32 x, i32 y])}
 * in both the base CField handler 37A7860 and the Zakum field override 3A68CD0 -> 3A68E70 (field type 12); the 265
 * builders always write flag 0 and the three ints, so the body is copied unchanged. A body with a non-zero flag (282
 * would not read its ints) or any other shape is declined (dropped by the guard as before). Traced CLEAN: the
 * 17-platform Zakum body (335 bytes) through 3A68CD0. {@code -Dtms282.dynamicFoothold.enabled=false} turns it off.
 * Registered as one entry of {@link Tms282BossTranslators#PARTS}.
 */
public final class Tms282DynamicFootholdTranslators implements Tms282LegacyPackets.Contributor {
    public static final Tms282DynamicFootholdTranslators INSTANCE = new Tms282DynamicFootholdTranslators();

    public static final int FIELD_SYNC_DYNAMIC_FOOTHOLD = 0x0321;
    static final int MAX_ENTRIES = 4096;

    private Tms282DynamicFootholdTranslators() {
    }

    public static boolean enabled() {
        return !"false".equalsIgnoreCase(System.getProperty("tms282.dynamicFoothold.enabled", "true"));
    }

    @Override
    public void contribute(Tms282LegacyPackets.Builder builder) {
        if (!enabled()) {
            return;
        }
        builder.on(OutHeader.FIELD_SYNC_DYNAMIC_FOOTHOLD, Tms282DynamicFootholdTranslators::sync);
    }

    static Optional<OutPacket> sync(OutPacket legacy) {
        Tms282LegacyPackets.Body body = Tms282LegacyPackets.Body.of(legacy);
        int n = body.i32();
        if (n < 0 || n > MAX_ENTRIES) {
            return Optional.empty();
        }
        for (int i = 0; i < n; i++) {
            body.bytes(body.u16()); // name
            if (body.u8() != 0) {
                return Optional.empty();
            }
            body.i32(); // state (visible)
            body.i32(); // x
            body.i32(); // y
        }
        body.end();
        byte[] data = legacy.getData();
        OutPacket out = Tms282LoginPacketSupport.packet(FIELD_SYNC_DYNAMIC_FOOTHOLD);
        out.encodeArr(java.util.Arrays.copyOfRange(data, 2, data.length));
        return Optional.of(out);
    }
}
