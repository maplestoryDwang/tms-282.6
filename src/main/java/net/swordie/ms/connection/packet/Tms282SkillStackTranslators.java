package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.handlers.header.OutHeader;

import java.util.Optional;

/**
 * Stack skills on TMS 282.3 (research/tms2823/force-atom/README.md section 6). All three handlers work on the local
 * user's stack map (5F345B0(-2) + 0x2B0, keyed by 82FF20(skill): -1 cannoneer roulette, -2, -3, -5, -6, -7 Ren, else the
 * skill), which is why the server's SkillConstants.STACK_SKILLS starts with -7..-1.
 * <ul>
 *     <li>265 STACK_SKILL_REQUEST_RESULT (570: i32 skill, u8 stack, i32 -1) -> <b>LP0266</b> (6001F40):
 *     i32 skill, u8 stack. 282 does not read the trailing -1.</li>
 *     <li>265 ALL_STACK_SKILLS_RESULT (571: i32 n, n x (i32 skill, i32 value)) -> <b>LP0267</b> (6002170):
 *     <b>u8 flag</b> (0), i32 n, n x (i32 skill, i32 value) (node +0x38 = now + value).</li>
 *     <li>LP0268 (60023B0, 282 only): i32 skill, i32 delta (node +0x38 -= delta); {@link #reduce}.</li>
 * </ul>
 * Opcodes: delta +44 as HEXA_MESSAGE 581 -> 0x271, HYPER_STATS_PRESET 572 -> 0x269 (0x268 inserted); layouts traced CLEAN
 * with the emulated client (fake local user). {@code tms282.skillstack.visible=false} drops them again.
 */
public final class Tms282SkillStackTranslators implements Tms282LegacyPackets.Contributor {
    public static final Tms282SkillStackTranslators INSTANCE = new Tms282SkillStackTranslators();

    public static final int STACK_RESULT = 0x0266;
    public static final int ALL_STACKS = 0x0267;
    public static final int REDUCE = 0x0268;
    public static final int MAX_ENTRIES = 1024;

    public static final boolean VISIBLE = !"false".equalsIgnoreCase(property("tms282.skillstack.visible"));

    private Tms282SkillStackTranslators() {
    }

    private static String property(String key) {
        String v = System.getProperty(key);
        if (v != null) {
            return v.trim();
        }
        try {
            String c = net.swordie.ms.console.config.ServerRuntimeConfig.stringValue(key);
            return c == null ? "" : c.trim();
        } catch (RuntimeException | LinkageError e) {
            return "";
        }
    }

    @Override
    public void contribute(Tms282LegacyPackets.Builder builder) {
        builder.on(OutHeader.STACK_SKILL_REQUEST_RESULT, p -> VISIBLE ? stackResult(p) : Optional.empty())
                .on(OutHeader.ALL_STACK_SKILLS_RESULT, p -> VISIBLE ? allStacks(p) : Optional.empty());
    }

    static Optional<OutPacket> stackResult(OutPacket legacy) {
        Tms282LegacyPackets.Body b = Tms282LegacyPackets.Body.of(legacy);
        int skill = b.i32();
        int stack = b.u8();
        if (b.remaining() == 4) {
            b.i32(); // 265 trailing -1, not read by 6001F40
        }
        b.end();
        return Optional.of(stackResult(skill, stack));
    }

    public static OutPacket stackResult(int skill, int stack) {
        OutPacket out = Tms282LoginPacketSupport.packet(STACK_RESULT);
        out.encodeInt(skill);
        out.encodeByte(stack);
        return out;
    }

    static Optional<OutPacket> allStacks(OutPacket legacy) {
        Tms282LegacyPackets.Body b = Tms282LegacyPackets.Body.of(legacy);
        int n = b.i32();
        if (n < 0 || n > MAX_ENTRIES) {
            throw new IllegalArgumentException("stack entries " + n);
        }
        OutPacket out = Tms282LoginPacketSupport.packet(ALL_STACKS);
        out.encodeByte(0);
        out.encodeInt(n);
        for (int i = 0; i < n; i++) {
            out.encodeInt(b.i32());
            out.encodeInt(b.i32());
        }
        b.end();
        return Optional.of(out);
    }

    /** LP0268: shorten a stack skill's timer by {@code delta} ms (282 only; nothing in 265 builds it). */
    public static OutPacket reduce(int skill, int delta) {
        OutPacket out = Tms282LoginPacketSupport.packet(REDUCE);
        out.encodeInt(skill);
        out.encodeInt(delta);
        return out;
    }
}
