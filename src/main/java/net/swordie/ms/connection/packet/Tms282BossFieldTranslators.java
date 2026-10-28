package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.handlers.header.OutHeader;

import java.util.Optional;

/**
 * Boss-mechanics phase (2026-09-30, research/tms2823/boss-mech-0930): the boss-specific 265 packets of Will, Demian,
 * Verus Hilla and Pierre that the fail-closed guard dropped, in their 282 form. Lucid and the shared boss packets
 * (MOB_NEXT_TARGET, DEATH_COUNT, OBSTACLE, MOB_SKILL_DELAY, weather, MOB_SPEAKING, MOB_LASER_CONTROL,
 * MOB_FORCED_ACTION) are Tms282BossSharedTranslators / the Lucid contributor (research/tms2823/lucid-re-0930) and
 * are NOT registered here.
 * <p>
 * 282 dispatches these through the field's own OnPacket override (CField subclass chosen by Map.wz info/fieldType in
 * the factory 3712650), so the same opcode means different things in different boss fields (0x881 is Will's spider
 * attack and Dunkel's own packet). The server only sends them from the matching boss field.
 * <ul>
 *     <li>Will (fieldType 182/183/184 -> 3A42220 / 3A49940 / 3A4D040, shared base 3A346F0): 265 op + 0x35 for
 *     2103..2114, then SET_HP 0x879, SET_HP2 0x87A, STUN 0x87B, USE_SPECIAL 0x87C (order-based), SPIDER_ATTACK 0x881.
 *     Bodies = 265, except WILL_SPIDER remove (type 4): the client reads only {@code i32 4, i32 objectId}.</li>
 *     <li>Demian (fieldType 144 -> 387D240): 265 op + 0x35 (2009 -> 0x80E ... 2019 -> 0x818); bodies = 265.</li>
 *     <li>Verus Hilla (fieldType 199/212 -> 3412650 -> 34159C0): VERUS_HILLA -> 0x372; types 0/1/3/4/6/7/8 = 265,
 *     type 10 is a list in 282: {@code i32 10, i32 n, n x (i32 charId, i32 deathCount)} (265 sends one pair).</li>
 *     <li>CMobPool: MOB_DEMIAN_DELAYED_ATTACK_CREATE -> 0x6EF (3F40080), VERUS_HILLA_BLACK_HAND -> 0x6FC (3F20740);
 *     bodies = 265.</li>
 *     <li>CField: CHASE_EFFECT_SET (Pierre) -> 0x340 (inline 3726643); body = 265.</li>
 * <li>MOB_FORCED_SKILL_ACTION (Will phase-1 stun, Zakum arms) -> 0x6CA (3EAA0B0: {@code i32 action, u8} -> 3EAA590(mob,
 * action + 0x1E, flag) -> 3E6FCE0, the same "skill action = index + 0x1E" transform as 0x6CD); body = 265. Found by
 * sweeping every CMobPool op with the 265 body (research/tms2823/boss-mech-0930 section 5); lucid-re-0930/shared left it
 * unresolved (its 0x6CD reads three i32).</li>
 * </ul>
 * Every rule was traced with the emulated 282.3 client (boss-mech-0930/out -> expected, tools/run_traces.py);
 * 282.4 layouts are identical. Malformed 265 bodies are declined (dropped as before).
 * {@code -Dtms282.bossMech.enabled=false} turns the whole contributor off (same pattern as Tms282BossSharedTranslators).
 * <p>
 * Registration (bossmech2-0930): one entry in {@link Tms282BossTranslators#PARTS}, not a separate line in
 * Tms282FieldEncoders. TOSSED_BY_MOB_SKILL (candidate 0x040A) and MOB_FORCE_CHASE stay untranslated (unconfirmed).
 */
public final class Tms282BossFieldTranslators implements Tms282LegacyPackets.Contributor {
    public static final Tms282BossFieldTranslators INSTANCE = new Tms282BossFieldTranslators();


    // ---- Will (CField 3A346F0 base; phase classes 3A42220 / 3A49940 / 3A4D040) ----
    public static final int WILL_SET_MOONGAUGE = 0x086C;
    public static final int WILL_MOONGAUGE = 0x086D;
    public static final int WILL_COOLTIME_MOONGAUGE = 0x086E;
    public static final int WILL_CREATE_BULLETEYE = 0x086F;
    public static final int WILL_SPIDER = 0x0870;
    public static final int WILL_THIRD_ONE = 0x0871;
    public static final int WILL_POISON = 0x0872;
    public static final int WILL_POISON_2 = 0x0873;
    public static final int WILL_POISON_REMOVE = 0x0874;
    public static final int WILL_POISON_ATTACK = 0x0875;
    public static final int WILL_TELEPORT = 0x0877;
    public static final int WILL_SET_HP = 0x0879;
    public static final int WILL_SET_HP2 = 0x087A;
    public static final int WILL_STUN = 0x087B;
    public static final int WILL_USE_SPECIAL = 0x087C;
    public static final int WILL_SPIDER_ATTACK = 0x0881;
    // ---- Demian (CField 387D240) ----
    public static final int DEMIAN_STIGMA_INCINERATE_OBJECT = 0x080E;
    public static final int DEMIAN_STIGMA_OBJECT_EFFECT = 0x080F;
    public static final int DEMIAN_FLYING_SWORD_CREATE = 0x0810;
    public static final int DEMIAN_FLYING_SWORD_NODE = 0x0813;
    public static final int DEMIAN_FLYING_SWORD_TARGET = 0x0814;
    public static final int DEMIAN_CORRUPTION_CHANGE = 0x0818;
    // ---- Verus Hilla (CField 3412650) ----
    public static final int VERUS_HILLA = 0x0372;
    // ---- CMobPool ----
    public static final int MOB_DEMIAN_DELAYED_ATTACK_CREATE = 0x06EF;
    public static final int VERUS_HILLA_BLACK_HAND = 0x06FC;
    public static final int MOB_FORCED_SKILL_ACTION = 0x06CA;
    // ---- CField ----
    public static final int CHASE_EFFECT_SET = 0x0340;

    /** 265 WillPacket.spider: 3 = create, 4 = remove. */
    static final int WEB_CREATE = 3;
    static final int WEB_REMOVE = 4;
    /** 265 VerusHillaPacket type 10: one (charId, deathCount) pair; 282 reads a counted list. */
    static final int VH_DEATH_COUNT = 10;

    private Tms282BossFieldTranslators() {
    }

    public static boolean enabled() {
        return !"false".equalsIgnoreCase(System.getProperty("tms282.bossMech.enabled", "true"));
    }

    @Override
    public void contribute(Tms282LegacyPackets.Builder builder) {
        if (!enabled()) {
            return;
        }
        // Will
        same(builder, OutHeader.WILL_SET_MOONGAUGE, WILL_SET_MOONGAUGE, 8);
        same(builder, OutHeader.WILL_MOONGAUGE, WILL_MOONGAUGE, 4);
        same(builder, OutHeader.WILL_COOLTIME_MOONGAUGE, WILL_COOLTIME_MOONGAUGE, 4);
        same(builder, OutHeader.WILL_CREATE_BULLETEYE, WILL_CREATE_BULLETEYE, -1);
        builder.on(OutHeader.WILL_SPIDER, Tms282BossFieldTranslators::spider);
        same(builder, OutHeader.WILL_THIRD_ONE, WILL_THIRD_ONE, 5);
        same(builder, OutHeader.WILL_POISON, WILL_POISON, -1);
        same(builder, OutHeader.WILL_POISON_2, WILL_POISON_2, 17);
        same(builder, OutHeader.WILL_POISON_REMOVE, WILL_POISON_REMOVE, -1);
        same(builder, OutHeader.WILL_POISON_ATTACK, WILL_POISON_ATTACK, 12);
        same(builder, OutHeader.WILL_TELEPORT, WILL_TELEPORT, 4);
        same(builder, OutHeader.WILL_SET_HP, WILL_SET_HP, -1);
        same(builder, OutHeader.WILL_SET_HP2, WILL_SET_HP2, -1);
        same(builder, OutHeader.WILL_STUN, WILL_STUN, 0);
        same(builder, OutHeader.WILL_USE_SPECIAL, WILL_USE_SPECIAL, 0);
        same(builder, OutHeader.WILL_SPIDER_ATTACK, WILL_SPIDER_ATTACK, -1);
        // Demian
        same(builder, OutHeader.DEMIAN_STIGMA_INCINERATE_OBJECT_PACKET, DEMIAN_STIGMA_INCINERATE_OBJECT, -1);
        same(builder, OutHeader.DEMIAN_STIGMA_OBJECT_EFFECT, DEMIAN_STIGMA_OBJECT_EFFECT, 0);
        same(builder, OutHeader.DEMIAN_FLYING_SWORD_CREATE, DEMIAN_FLYING_SWORD_CREATE, -1);
        same(builder, OutHeader.DEMIAN_FLYING_SWORD_NODE, DEMIAN_FLYING_SWORD_NODE, -1);
        same(builder, OutHeader.DEMIAN_FLYING_SWORD_TARGET, DEMIAN_FLYING_SWORD_TARGET, 8);
        same(builder, OutHeader.DEMIAN_CORRUPTION_CHANGE, DEMIAN_CORRUPTION_CHANGE, 5);
        // Verus Hilla
        builder.on(OutHeader.VERUS_HILLA, Tms282BossFieldTranslators::verusHilla);
        same(builder, OutHeader.VERUS_HILLA_BLACK_HAND, VERUS_HILLA_BLACK_HAND, -1);
        // Demian mob, Pierre
        same(builder, OutHeader.MOB_DEMIAN_DELAYED_ATTACK_CREATE, MOB_DEMIAN_DELAYED_ATTACK_CREATE, -1);
        same(builder, OutHeader.CHASE_EFFECT_SET, CHASE_EFFECT_SET, 10);
        same(builder, OutHeader.MOB_FORCED_SKILL_ACTION, MOB_FORCED_SKILL_ACTION, 9);
    }

    /** Body unchanged, 282 opcode; {@code length >= 0} declines any other body length. */
    private static void same(Tms282LegacyPackets.Builder builder, OutHeader header, int op, int length) {
        builder.on(header, p -> copy(p, op, length));
    }

    static Optional<OutPacket> copy(OutPacket legacy, int op, int length) {
        byte[] body = Tms282LegacyPackets.Body.of(legacy).rest();
        if (length >= 0 && body.length != length) {
            return Optional.empty();
        }
        OutPacket out = Tms282LoginPacketSupport.packet(op);
        out.encodeArr(body);
        return Optional.of(out);
    }

    /** WILL_SPIDER: create = 265 body (7 x i32); remove = {@code i32 4, i32 objectId} only (client 3A4C5E2). */
    static Optional<OutPacket> spider(OutPacket legacy) {
        Tms282LegacyPackets.Body body = Tms282LegacyPackets.Body.of(legacy);
        int type = body.i32();
        int objectId = body.i32();
        if (type == WEB_CREATE) {
            byte[] rest = body.rest(); // pattern, x, y, w, h
            if (rest.length != 20) {
                return Optional.empty();
            }
            OutPacket out = Tms282LoginPacketSupport.packet(WILL_SPIDER);
            out.encodeInt(type);
            out.encodeInt(objectId);
            out.encodeArr(rest);
            return Optional.of(out);
        }
        if (type == WEB_REMOVE) {
            OutPacket out = Tms282LoginPacketSupport.packet(WILL_SPIDER);
            out.encodeInt(type);
            out.encodeInt(objectId);
            return Optional.of(out);
        }
        return Optional.empty();
    }

    /** VERUS_HILLA: type 10 becomes a one-element list; the other 265 types are sent as they are. */
    static Optional<OutPacket> verusHilla(OutPacket legacy) {
        Tms282LegacyPackets.Body body = Tms282LegacyPackets.Body.of(legacy);
        int type = body.i32();
        OutPacket out = Tms282LoginPacketSupport.packet(VERUS_HILLA);
        out.encodeInt(type);
        switch (type) {
            case 0, 1, 3, 4, 6, 7, 8 -> out.encodeArr(body.rest());
            case VH_DEATH_COUNT -> {
                int charId = body.i32();
                int deathCount = body.i32();
                body.end();
                out.encodeInt(1);
                out.encodeInt(charId);
                out.encodeInt(deathCount);
            }
            default -> {
                return Optional.empty(); // 2, 5, 9: never built by 265 VerusHillaPacket, not traced
            }
        }
        return Optional.of(out);
    }
}
