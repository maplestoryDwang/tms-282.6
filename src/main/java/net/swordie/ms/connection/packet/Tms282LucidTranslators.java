package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.handlers.header.OutHeader;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Lucid boss field packets (2026-09-30, research/tms2823/lucid-re-0930): the ten 265 LUCID_* packets
 * ({@link LucidPacket}, OutHeader 2023..2033) that the fail-closed guard dropped, sent under their 282 opcodes.
 * <p>
 * The 282.3 client (282.4 identical) has two Lucid field classes whose OnPacket overrides (secondary vtable slot)
 * dispatch 0x081C..0x082A (265 + 53):
 * <ul>
 *     <li>38F13D0 (vtable 86417B8, ctor 38F0662/38F07A3/38F7D39): 0x081C, 0x081D, 0x081F, 0x0820, 0x0823, rest to
 *     CField::OnPacket 3725AF0 (phase 1: no stained glass / flying / barrage);</li>
 *     <li>38FAA00 (vtable 8642BF8, ctor 38F9A57/38F9BD7/3901A3E): 0x081C..0x0826, 0x082A (phase 2 / last phase).</li>
 * </ul>
 * Both call the same leaf readers. Every output below was traced CLEAN through 38FAA00 with
 * research/tms2823/daily-trace/daily_trace.py (fixtures lucid-re-0930/out, layouts expected/*.tsv):
 * <ul>
 *     <li>BUTTERFLY_INIT -> 0x081C (38E8180): {@code i32 unk, i32 n, n x (i32 type, i32 x, i32 y)} = 265 body.</li>
 *     <li>BUTTERFLY_ACTION -> 0x081D (38E7F20): {@code i32 mode}; 0 add (38E83A0) {@code i32, i32 type, i32 x,
 *     i32 y}; 1 move {@code i32 x, i32 y}; 2 attack (38E8700) {@code i32 count, i32 n, n x i32 charId}
 *     (= {@link LucidPacket#butterflyAttack}; the {@code butterflyAction(Attack, ..)} form count+delay is NOT the
 *     282 body and is declined unless it happens to parse as a list); 3 erase: nothing.</li>
 *     <li>DRAGON_INIT -> 0x081F (38EC660): {@code i32 phase, i32 x, i32 y, i32 createX, i32 createY, u8 left}.</li>
 *     <li>DO_SKILL -> 0x0820 (inline 38FAB16): {@code i32 skillId (only 238 is handled), i32 slv}, then slv 1-3
 *     flower trap (38EEA70) {@code i32 pattern, i32 x, i32 y, u8}; 4/10 fairy dust (38ECD00) {@code i32 n,
 *     n x 4 i32}; 5 laser rain (38ED7A0) {@code i32 delay, i32 n, n x i32}; 6/11 forced teleport {@code i32 charId};
 *     8 rush (38EA5A0) {@code i32 path}; 7 and 9 read nothing (the 265 server only sends those headers);
 *     12 (38FEA40) is 282-only and never built by the 265 server (declined).</li>
 *     <li>STAINED_GLASS_ON_OFF -> 0x0821 (38FAE60): {@code u8 on, i32 n, n x str}; STAINED_GLASS_BREAK -> 0x0822
 *     (38FB2B0): {@code i32 n, n x str}. Strings are re-encoded in the 282 charset.</li>
 *     <li>STATUE_STATE_CHANGE -> 0x0823 (3D843D0): {@code i32 kind}; 1 {@code u8}; 0 {@code i32 gauge, u8};
 *     other kinds read nothing.</li>
 *     <li>SET_FLYING_MODE -> 0x0824 (inline 38FACBA): {@code u8}.</li>
 *     <li>WELCOME_BARRAGE -> 0x0825 (3904F40): {@code i32 type (0..6)}; 0 {@code i32 angle, i32 speed}; 1, 2
 *     nothing; 3 {@code 4 x i32}; 4, 5 {@code 8 x i32}; 6 nothing (the client answers CP0618).</li>
 *     <li>LAST_PHASE_HARD_MODE -> 0x0826 (3902AF0, "Fury"): {@code i32 type}; 1 background, 2 fail, 3 fog;
 *     0 is accepted and does nothing.</li>
 * </ul>
 * Malformed or unknown 265 bodies are declined (dropped as before). Disable with
 * {@code -Dtms282.lucid.enabled=false}. Registered through {@link Tms282BossTranslators} (bossmech1-0930).
 * The hard-mode last phase start is sent as type 1 by BossHelper on 282 (type 0 is a client no-op).
 */
public final class Tms282LucidTranslators implements Tms282LegacyPackets.Contributor {
    public static final Tms282LucidTranslators INSTANCE = new Tms282LucidTranslators();

    public static final int BUTTERFLY_INIT = 0x081C;
    public static final int BUTTERFLY_ACTION = 0x081D;
    public static final int DRAGON_INIT = 0x081F;
    public static final int DO_SKILL = 0x0820;
    public static final int STAINED_GLASS_ON_OFF = 0x0821;
    public static final int STAINED_GLASS_BREAK = 0x0822;
    public static final int STATUE_STATE_CHANGE = 0x0823;
    public static final int SET_FLYING_MODE = 0x0824;
    public static final int WELCOME_BARRAGE = 0x0825;
    public static final int LAST_PHASE_HARD_MODE = 0x0826;
    /** MobSkillID.Lucid; the 282 DO_SKILL handler ignores every other skill id. */
    public static final int LUCID_MOB_SKILL = 238;
    static final int MAX_LIST = 256;

    private Tms282LucidTranslators() {
    }

    static boolean enabled() {
        return !"false".equalsIgnoreCase(System.getProperty("tms282.lucid.enabled", "true"));
    }

    @Override
    public void contribute(Tms282LegacyPackets.Builder builder) {
        Charset cs = builder.charset();
        builder.on(OutHeader.LUCID_BUTTERFLY_INIT, Tms282LucidTranslators::butterflyInit)
                .on(OutHeader.LUCID_BUTTERFLY_ACTION, Tms282LucidTranslators::butterflyAction)
                .on(OutHeader.LUCID_DRAGON_INIT, p -> fixed(p, DRAGON_INIT, 21))
                .on(OutHeader.LUCID_DO_SKILL, Tms282LucidTranslators::doSkill)
                .on(OutHeader.LUCID_STAINED_GLASS_ON_OFF, p -> stainedGlass(p, STAINED_GLASS_ON_OFF, true, cs))
                .on(OutHeader.LUCID_STAINED_GLASS_BREAK, p -> stainedGlass(p, STAINED_GLASS_BREAK, false, cs))
                .on(OutHeader.LUCID_STATUE_STATE_CHANGE, Tms282LucidTranslators::statue)
                .on(OutHeader.LUCID_SET_FLYING_MODE, p -> fixed(p, SET_FLYING_MODE, 1))
                .on(OutHeader.LUCID_WELCOME_BARRAGE, Tms282LucidTranslators::welcomeBarrage)
                .on(OutHeader.LUCID_LAST_PHASE_HARD_MODE, Tms282LucidTranslators::lastPhase);
    }

    static Optional<OutPacket> fixed(OutPacket legacy, int op, int length) {
        if (!enabled()) {
            return Optional.empty();
        }
        byte[] body = Tms282LegacyPackets.Body.of(legacy).rest();
        if (body.length != length) {
            return Optional.empty();
        }
        OutPacket out = Tms282LoginPacketSupport.packet(op);
        out.encodeArr(body);
        return Optional.of(out);
    }

    /** Copies the 265 body when {@code check} consumed it exactly. */
    private static Optional<OutPacket> copyChecked(OutPacket legacy, int op, java.util.function.Consumer<Tms282LegacyPackets.Body> check) {
        if (!enabled()) {
            return Optional.empty();
        }
        byte[] all = Tms282LegacyPackets.Body.of(legacy).rest();
        Tms282LegacyPackets.Body body = Tms282LegacyPackets.Body.of(legacy);
        check.accept(body);
        body.end();
        OutPacket out = Tms282LoginPacketSupport.packet(op);
        out.encodeArr(all);
        return Optional.of(out);
    }

    static int count(Tms282LegacyPackets.Body body, int elementBytes) {
        int n = body.i32();
        if (n < 0 || n > MAX_LIST || (long) n * elementBytes > body.remaining()) {
            throw new IllegalArgumentException("bad list count " + n);
        }
        return n;
    }

    static Optional<OutPacket> butterflyInit(OutPacket legacy) {
        return copyChecked(legacy, BUTTERFLY_INIT, b -> {
            b.i32();
            b.bytes(count(b, 12) * 12);
        });
    }

    static Optional<OutPacket> butterflyAction(OutPacket legacy) {
        return copyChecked(legacy, BUTTERFLY_ACTION, b -> {
            switch (b.i32()) {
                case 0 -> b.bytes(16);
                case 1 -> b.bytes(8);
                case 2 -> {
                    b.i32();
                    b.bytes(count(b, 4) * 4);
                }
                case 3 -> { }
                default -> throw new IllegalArgumentException("butterfly mode");
            }
        });
    }

    static Optional<OutPacket> doSkill(OutPacket legacy) {
        return copyChecked(legacy, DO_SKILL, b -> {
            if (b.i32() != LUCID_MOB_SKILL) {
                throw new IllegalArgumentException("not the Lucid mob skill");
            }
            switch (b.i32()) {
                case 1, 2, 3 -> b.bytes(13);
                case 4, 10 -> b.bytes(count(b, 16) * 16);
                case 5 -> {
                    b.i32();
                    b.bytes(count(b, 4) * 4);
                }
                case 6, 8, 11 -> b.i32();
                case 7, 9 -> { }
                default -> throw new IllegalArgumentException("slv");
            }
        });
    }

    static Optional<OutPacket> stainedGlass(OutPacket legacy, int op, boolean onOff, Charset cs) {
        if (!enabled()) {
            return Optional.empty();
        }
        Tms282LegacyPackets.Body b = Tms282LegacyPackets.Body.of(legacy);
        OutPacket out = Tms282LoginPacketSupport.packet(op);
        if (onOff) {
            int on = b.u8();
            if (on > 1) {
                return Optional.empty();
            }
            out.encodeByte(on);
        }
        int n = count(b, 2);
        out.encodeInt(n);
        for (int i = 0; i < n; i++) {
            byte[] s = b.str().getBytes(cs == null ? StandardCharsets.US_ASCII : cs);
            out.encodeShort(s.length);
            out.encodeArr(s);
        }
        b.end();
        return Optional.of(out);
    }

    static Optional<OutPacket> statue(OutPacket legacy) {
        return copyChecked(legacy, STATUE_STATE_CHANGE, b -> {
            switch (b.i32()) {
                case 1 -> b.u8();
                case 0 -> {
                    b.i32();
                    b.u8();
                }
                default -> throw new IllegalArgumentException("statue kind");
            }
        });
    }

    static Optional<OutPacket> welcomeBarrage(OutPacket legacy) {
        return copyChecked(legacy, WELCOME_BARRAGE, b -> {
            switch (b.i32()) {
                case 0 -> b.bytes(8);
                case 1, 2, 6 -> { }
                case 3 -> b.bytes(16);
                case 4, 5 -> b.bytes(32);
                default -> throw new IllegalArgumentException("barrage type");
            }
        });
    }

    static Optional<OutPacket> lastPhase(OutPacket legacy) {
        return copyChecked(legacy, LAST_PHASE_HARD_MODE, b -> {
            int type = b.i32();
            if (type < 0 || type > 3) {
                throw new IllegalArgumentException("fury type");
            }
        });
    }
}
