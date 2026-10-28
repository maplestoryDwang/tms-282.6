package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.handlers.header.OutHeader;

import java.nio.charset.Charset;
import java.util.Optional;
import java.util.Set;

/**
 * {@link Tms282LegacyPackets.Contributor} for the cutscene and boss packets that 265 scripts build directly
 * (ScriptManagerImpl camera/delay/effect helpers, field effects, clocks, the boss entry UI). Each 282 reader was
 * traced with the fixtures of {@code tools/Tms282DirectionCheck} (research/tms2823/boss-trace, packet-audit
 * boss-cutscene-2823.md).
 * <ul>
 *     <li>IN_GAME_DIRECTION_EVENT -> LP0502: CUserLocal case 5788C81 -> 3B2FC80 (type byte, jump table 3B33370,
 *     types 0..0x2A). 282 inserted a type 22 ({@code u8, i32, i32}), so the 265 types 22..39 are one higher; the
 *     265 nIdx byte of Unk24/Unk40 is an i32 in 282; Monologue_2 (265 39) is 282 42. Types 6/10/12/18 are
 *     untraced and declined.</li>
 *     <li>FIELD_EFFECT -> LP0308: CField case 3725D31 -> 3758FB0 (type byte, table 376364C, types 0..0x5F). 282
 *     inserted a type 19 ({@code str, i32}), so the 265 types 19..43 are one higher. OffSpineScreen changed its
 *     tail: 282 reads {@code str, i32 mode} then nothing (0), {@code i32} (1) or {@code str} (2).</li>
 *     <li>CLOCK -> LP030F: CField case 3725E6D -> virtual +0x2A8 = 3747D70; 265 types 0..7 and 9 read alike.</li>
 *     <li>DESTROY_CLOCK -> LP0317 (3744CC0, no body).</li>
 *     <li>EFFECT (local) -> LP04CA (CUser::OnEffect 545EC40): the cutscene types in {@link #EFFECT_TYPES}, without the
 *     265 trailing i32 -1.</li>
 *     <li>NPC_SET_FORCE_MOVE -> LP0735 (40CE4C0 {@code i32, i32, i32}), NPC_SET_FORCE_FLIP -> LP0736 (40CE540
 *     {@code i32}), NPC_RESET_SPECIAL_ACTION -> LP073D (40CD820), NPC_SET_SPECIAL_ACTION -> LP0745 (40CD510
 *     {@code str, i32, u8, u8}, lp-map 證實): CNpcPool::OnNpcPacket reads the i32 objId first like 265; the shift is
 *     +50 up to 0x736 and +51 from 0x73D.</li>
 *     <li>BOSS_UI -> LP00D7 (5F9C950): the 265 body without its trailing quest-condition list, which 282 reads with
 *     three strings and more per entry (52FF7A..5300E6); sent as an empty list.</li>
 * </ul>
 * Opcodes: 0x502 and 0x308 by the read shapes of the whole type switch (推定 -> traced), 0x30F/0x317 by the +27
 * CField shift between SET_QUEST_TIME 0x315 and the clock reader, 0xD7 static 證實.
 */
public final class Tms282DirectionTranslators implements Tms282LegacyPackets.Contributor {
    public static final int IN_GAME_DIRECTION_EVENT = 0x0502;
    public static final int FIELD_EFFECT = 0x0308;
    public static final int CLOCK = 0x030F;
    public static final int DESTROY_CLOCK = 0x0317;
    public static final int BOSS_UI = 0x00D7;
    public static final int EFFECT = 0x04CA;
    public static final int NPC_SET_FORCE_MOVE = 0x0735;
    public static final int NPC_SET_FORCE_FLIP = 0x0736;
    public static final int NPC_RESET_SPECIAL_ACTION = 0x073D;
    public static final int NPC_SET_SPECIAL_ACTION = 0x0745;
    /**
     * 265 EFFECT (local) types used by cutscene scripts -> 282 CUser::OnEffect type: the only 282 types that read
     * each 265 body exactly (boss-trace/effscan.py over 0..0x70); 40..67 are +7 in 282. Other types are declined
     * (0 LevelUp and 1 SkillUse have their own 282 builders, Tms282EffectPackets).
     */
    static final java.util.Map<Integer, Integer> EFFECT_TYPES = java.util.Map.ofEntries(
            // dropfix-0930 (research/tms2823/dropfix-0930/effect, remote_trace eff- CLEAN): 265 -> 282 is +1 for
            // 13..26, -1 for 27..39 (265 unk27/unk30 gone), +7 from 40 (0x27..0x2E new in 282).
            java.util.Map.entry(1, 0x01),   // SkillUse: REMOTE only, traced skills only (local has Tms282EffectPackets)
            java.util.Map.entry(4, 0x04),   // SkillAffected 546D089: i32 skill, u8 slv [, i32 for 31111003/25121006]
            java.util.Map.entry(6, 0x06),   // SkillAffected_Select: verified Cannoneer dice results, 546DC2B..546DC56
            java.util.Map.entry(13, 0x0E),  // PlayPortalSE 5471D81 (no body)
            java.util.Map.entry(15, 0x10),  // QuestComplete 5472C43 Effect/BasicEff.img/QuestClear (no body)
            java.util.Map.entry(16, 0x11),  // IncDecHPEffect u8 -> 543B430 HP number
            java.util.Map.entry(17, 0x12),  // BuffItemEffect i32 (Effect/ItemEff.img/%d)
            java.util.Map.entry(25, 0x1A),  // FieldExpItemConsumed: 5474214 reads i64, 1D9EA10 draws the "exp" number
            java.util.Map.entry(26, 0x1B),  // ReservedEffect
            java.util.Map.entry(31, 0x1E),  // AvatarOriented str UOL (first switch 545F3AA), e.g. LevelUpHyper
            java.util.Map.entry(38, 0x25),  // IncDecHPEffect_EX i32, u8, i32 -> 543B430
            java.util.Map.entry(39, 0x26),  // IncDecHPRegenEffect i32 (5473AF0 -> 543B430: the green heal number)
            // 40 EffectUOL -> 0x2F (54748CF str,u8,i32,i32,u8; dropfix-0930 traced CLEAN, all logged UOLs exist in 282
            // WZ) is still OFF by default: it was suspected in the 2026-09-24 live crash after a mob kill
            // (formal-channel-1790190163739-3 send-1272, monsterCollectionGet UOL). -Dtms282.effectUol.enabled=true.
            java.util.Map.entry(40, 0x2F),
            java.util.Map.entry(46, 0x35),  // FadeInOut
            java.util.Map.entry(49, 0x38),  // BlindEffect
            java.util.Map.entry(57, 0x40),  // ReservedEffectRepeat
            java.util.Map.entry(62, 0x45),  // SpeechBalloon
            java.util.Map.entry(67, 0x4A)); // TextEffect
    /** dropfix-0930 types (off with -Dtms282.effect.dropfix.enabled=false / -Dtms282.dropfix.enabled=false). */
    static final Set<Integer> EFFECT_DROPFIX = Set.of(1, 4, 13, 15, 16, 17, 31, 38, 39, 40);
    /**
     * HP number effects: inside a 282 route (CP0148 item use) the route's LP009D already carries the recovery
     * number (Tms282StatPackets.statChanged hpRecovery), so the 265 copy would draw it twice: declined there.
     */
    static final Set<Integer> HP_NUMBER_TYPES = Set.of(16, 38, 39);
    /** 282 SkillUse reads a fourth i32 for these (546C590). */
    static final Set<Integer> SKILL_USE_FOUR_INT = Set.of(80002393, 80002394, 80002395, 80002421);
    /** 282 SkillAffected reads two more i32 after slv for these (546D0B9..546D0EF), 265 sends none: declined. */
    static final Set<Integer> SKILL_AFFECTED_EXTRA2 = Set.of(400021031, 400021040, 400051076, 500061031);

    static boolean effectDropfixEnabled() {
        return !"false".equalsIgnoreCase(System.getProperty("tms282.effect.dropfix.enabled", "true"))
                && !"false".equalsIgnoreCase(System.getProperty("tms282.dropfix.enabled", "true"));
    }

    static boolean effectUolEnabled() {
        return "true".equalsIgnoreCase(System.getProperty("tms282.effectUol.enabled", "false"));
    }

    static final int EFFECT_SPEECH_BALLOON = 62;
    static final int EFFECT_TEXT = 67;

    /** 265 direction types whose body 282 reads unchanged under the same type number. */
    static final Set<Integer> DIRECTION_SAME = Set.of(0, 1, 2, 3, 4, 5, 7, 8, 9, 11, 13, 14, 15, 16, 17, 19, 20, 21);
    /** 265 direction types that start with shown text (Monologue, MonologueScroll, Monologue_2). */
    static final Set<Integer> DIRECTION_TEXT = Set.of(15, 16, 39);
    /** 265 field-effect types below the 282 insertion (19) that read alike; 5/6/8/13..17 have no 265 builder. */
    static final Set<Integer> FIELD_EFFECT_SAME = Set.of(0, 1, 2, 3, 4, 7, 9, 10, 11, 12, 18);
    /** 265 field-effect types from 19 on that are one higher in 282 (22 and 26..32 untraced). */
    static final Set<Integer> FIELD_EFFECT_SHIFTED = Set.of(19, 20, 21, 23, 24, 25, 33, 34, 35, 36, 37, 38, 40, 41,
            42, 43);
    static final int FIELD_EFFECT_OFF_SPINE_265 = 43;
    /** 265 clock types that 282 reads alike. */
    static final Set<Integer> CLOCK_SAME = Set.of(0, 1, 2, 3, 4, 5, 6, 7, 9);
    /**
     * The fixed quest-condition list that ends every 265 BOSS_UI (WvsContext.startBossUI): count 2, then two
     * {@code i32, i32, i32, str, str} entries.
     */
    static final byte[] BOSS_UI_265_CONDITIONS = hex("02000000"
            + "9D1CE1FF" + "C8000000" + "79AE0700" + "0600" + "715374617465" + "0100" + "31"
            + "D31CE1FF" + "C8000000" + "79AE0700" + "0600" + "715374617465" + "0100" + "31");

    public static final Tms282DirectionTranslators INSTANCE = new Tms282DirectionTranslators();

    private Tms282DirectionTranslators() {
    }

    @Override
    public void contribute(Tms282LegacyPackets.Builder builder) {
        Charset charset = builder.charset();
        builder.on(OutHeader.IN_GAME_DIRECTION_EVENT, p -> inGameDirectionEvent(p, charset))
                .on(OutHeader.FIELD_EFFECT, p -> fieldEffect(p, charset))
                .on(OutHeader.CLOCK, Tms282DirectionTranslators::clock)
                // DESTROY_CLOCK declined 2026-09-24: 265 broadcasts it on field events without a clock shown; LP0317 was the
                // last packet before the live crash after a mob kill (formal-channel-1790190163739-3 send-1278).
                .on(OutHeader.DESTROY_CLOCK, p -> Optional.empty())
                .on(OutHeader.BOSS_UI, Tms282DirectionTranslators::bossUi)
                .on(OutHeader.EFFECT, p -> effect(p, charset))
                .on(OutHeader.REMOTE_EFFECT, p -> remoteEffect(p, charset)) // multiplayer-0930
                .on(OutHeader.NPC_SET_SPECIAL_ACTION, p -> npc(p, NPC_SET_SPECIAL_ACTION))
                .on(OutHeader.NPC_RESET_SPECIAL_ACTION, p -> npc(p, NPC_RESET_SPECIAL_ACTION))
                .on(OutHeader.NPC_SET_FORCE_MOVE, p -> npc(p, NPC_SET_FORCE_MOVE))
                .on(OutHeader.NPC_SET_FORCE_FLIP, p -> npc(p, NPC_SET_FORCE_FLIP));
    }

    // ---- IN_GAME_DIRECTION_EVENT -----------------------------------------------------------------------------------

    static Optional<OutPacket> inGameDirectionEvent(OutPacket legacy, Charset charset) {
        Tms282LegacyPackets.Body body = Tms282LegacyPackets.Body.of(legacy);
        int type = body.u8();
        Integer type282 = directionType(type);
        if (type282 == null) {
            return Optional.empty();
        }
        OutPacket out = Tms282LoginPacketSupport.packet(IN_GAME_DIRECTION_EVENT);
        out.encodeByte(type282);
        if (DIRECTION_TEXT.contains(type)) { // shown text first: 265 UTF-8 -> 282 charset
            Tms282LoginPacketSupport.str(out, body.str(), charset);
        }
        byte[] rest = body.rest();
        if (type == 24 || type == 40) { // Unk24 / Unk40: 265 u8 nIdx, 282 i32
            if (rest.length != 1) {
                return Optional.empty();
            }
            out.encodeInt(rest[0] & 0xFF);
        } else {
            out.encodeArr(rest);
        }
        return Optional.of(out);
    }

    /** The 282 type of a 265 direction type, or null when it is not translated. */
    static Integer directionType(int type265) {
        if (DIRECTION_SAME.contains(type265)) {
            return type265;
        }
        if (type265 >= 22 && type265 <= 38) {
            return type265 + 1;
        }
        if (type265 == 39) {
            return 42; // Monologue_2
        }
        if (type265 == 40) {
            return 41;
        }
        return null;
    }

    // ---- FIELD_EFFECT ----------------------------------------------------------------------------------------------

    static Optional<OutPacket> fieldEffect(OutPacket legacy, Charset charset) {
        Tms282LegacyPackets.Body body = Tms282LegacyPackets.Body.of(legacy);
        int type = body.u8();
        OutPacket out = Tms282LoginPacketSupport.packet(FIELD_EFFECT);
        if (type == FIELD_EFFECT_OFF_SPINE_265) {
            // 265: str layer, i32 type, then str (type 1) or i32 alpha time; 282: str, i32 mode {0: -, 1: i32, 2: str}
            String layer = body.str();
            int mode = body.i32();
            out.encodeByte(type + 1);
            Tms282LoginPacketSupport.str(out, layer, charset);
            if (mode == 1) {
                String name = body.str();
                body.end();
                out.encodeInt(2);
                Tms282LoginPacketSupport.str(out, name, charset);
            } else {
                int alphaTime = body.i32();
                body.end();
                if (alphaTime > 0) {
                    out.encodeInt(1);
                    out.encodeInt(alphaTime);
                } else {
                    out.encodeInt(0);
                }
            }
            return Optional.of(out);
        }
        byte[] rest = body.rest();
        if (FIELD_EFFECT_SAME.contains(type)) {
            out.encodeByte(type);
        } else if (FIELD_EFFECT_SHIFTED.contains(type)) {
            out.encodeByte(type + 1);
        } else {
            return Optional.empty();
        }
        out.encodeArr(rest);
        return Optional.of(out);
    }

    // ---- CLOCK / BOSS_UI -------------------------------------------------------------------------------------------

    static Optional<OutPacket> clock(OutPacket legacy) {
        Tms282LegacyPackets.Body body = Tms282LegacyPackets.Body.of(legacy);
        int type = body.u8();
        if (type == 8 && effectDropfixEnabled()) {
            // dropfix-0930 (research/tms2823/dropfix-0930/field): the 282 clock reader 3747D70 has no type 8
            // (TimerInfoEx i32, i32): sent as type 2 SecondsClock i32 (traced CLEAN, clock-type8-as-seconds).
            int a = body.i32();
            int b = body.i32();
            body.end();
            int seconds = a == b && a >= 1000 && a % 1000 == 0 ? a / 1000 : Math.max(0, a - b);
            OutPacket out = Tms282LoginPacketSupport.packet(CLOCK);
            out.encodeByte(2);
            out.encodeInt(seconds);
            return Optional.of(out);
        }
        if (!CLOCK_SAME.contains(type)) {
            return Optional.empty();
        }
        OutPacket out = Tms282LoginPacketSupport.packet(CLOCK);
        out.encodeByte(type);
        out.encodeArr(body.rest());
        return Optional.of(out);
    }

    static Optional<OutPacket> bossUi(OutPacket legacy) {
        byte[] body = Tms282LegacyPackets.Body.of(legacy).rest();
        int keep = body.length - BOSS_UI_265_CONDITIONS.length;
        if (keep < 9 || !java.util.Arrays.equals(body, keep, body.length, BOSS_UI_265_CONDITIONS, 0,
                BOSS_UI_265_CONDITIONS.length)) {
            return Optional.empty();
        }
        OutPacket out = Tms282LoginPacketSupport.packet(BOSS_UI);
        byte[] entries = java.util.Arrays.copyOf(body, keep);
        var wire = java.nio.ByteBuffer.wrap(entries).order(java.nio.ByteOrder.LITTLE_ENDIAN);
        int order = wire.getInt(0), count = wire.getInt(5);
        if (count < 0 || count > (keep - 9) / 18) return Optional.empty();
        for (int i = 0; i < count; i++) {
            int offset = 9 + i * 18;
            wire.putInt(offset, Tms282BossUiRequest.nativeDifficulty(order, wire.getInt(offset)));
        }
        out.encodeArr(entries);
        out.encodeInt(0); // quest-condition list: none
        return Optional.of(out);
    }

    // ---- EFFECT (local) / NPC actions ----------------------------------------------------------------------------

    /**
     * 265 UserPacket.effect: u8 type, body, i32 -1 (v265.1 trailer); 282 LP04CA: u8 type, body (no trailer). Shown
     * text (SpeechBalloon, TextEffect) is re-encoded in the 282 charset.
     */
    static Optional<OutPacket> effect(OutPacket legacy, Charset charset) {
        return effect(legacy, charset, false);
    }

    static Optional<OutPacket> effect(OutPacket legacy, Charset charset, boolean remote) {
        Tms282LegacyPackets.Body body = Tms282LegacyPackets.Body.of(legacy);
        int type = body.u8();
        Integer type282 = EFFECT_TYPES.get(type);
        if (type282 == null || EFFECT_DROPFIX.contains(type) && !effectDropfixEnabled()
                || type == 1 && !remote || type == 40 && !effectUolEnabled()
                || !remote && HP_NUMBER_TYPES.contains(type) && Tms282UnlockScope.inRoute()) {
            return Optional.empty();
        }
        OutPacket out = Tms282LoginPacketSupport.packet(EFFECT);
        out.encodeByte(type282);
        if (type == 1) { // SkillUse (remote): skill, charLevel, slv [, i32]; only skills whose 282 reads were traced
            int skill = body.i32();
            boolean four = SKILL_USE_FOUR_INT.contains(skill);
            if (!four && !Tms282EffectPackets.SKILL_USE_TRACED.contains(skill)
                    && !Tms282EffectPackets.SKILL_USE_BATCH.contains(skill)) {
                return Optional.empty();
            }
            out.encodeInt(skill);
            out.encodeInt(body.i32());
            out.encodeInt(body.i32());
            if (four) {
                out.encodeInt(body.i32());
            }
        } else if (type == 6) { // Dice: select, rootSelect, skill, slv, special; no 265 trailer in 282.
            int select = body.i32();
            int rootSelect = body.i32();
            int skill = body.i32();
            if (skill != 5311005 && skill != 5320007) return Optional.empty();
            out.encodeInt(select);
            out.encodeInt(rootSelect);
            out.encodeInt(skill);
            out.encodeByte(body.u8());
            out.encodeByte(body.u8());
        } else if (type == 4) { // SkillAffected
            int skill = body.i32();
            if (SKILL_AFFECTED_EXTRA2.contains(skill)) {
                return Optional.empty();
            }
            out.encodeInt(skill);
            out.encodeByte(body.u8());
            if (skill == 31111003 || skill == 25121006) {
                out.encodeInt(body.i32());
            }
        } else if (type == 31) { // AvatarOriented
            Tms282LoginPacketSupport.str(out, body.str(), charset);
        } else if (type == 40) { // EffectUOL: 282 has no read for the 265 nItemID of kind 2
            Tms282LoginPacketSupport.str(out, body.str(), charset);
            out.encodeByte(body.u8());
            out.encodeInt(body.i32());
            int kind = body.i32();
            if (kind == 2) {
                return Optional.empty();
            }
            out.encodeInt(kind);
            out.encodeByte(body.u8());
        } else if (type == 25) {
            out.encodeLong(body.i32()); // 282 widened the EXP effect amount from i32 to i64.
        } else if (type == EFFECT_SPEECH_BALLOON) { // u8, i32, i32, str, 9 x i32
            out.encodeByte(body.u8());
            out.encodeInt(body.i32());
            out.encodeInt(body.i32());
            Tms282LoginPacketSupport.str(out, body.str(), charset);
            for (int i = 0; i < 9; i++) {
                out.encodeInt(body.i32());
            }
        } else if (type == EFFECT_TEXT) { // str, 10 x i32, str, i32, u8
            Tms282LoginPacketSupport.str(out, body.str(), charset);
            for (int i = 0; i < 10; i++) {
                out.encodeInt(body.i32());
            }
            Tms282LoginPacketSupport.str(out, body.str(), charset);
            out.encodeInt(body.i32());
            out.encodeByte(body.u8());
        } else {
            byte[] rest = body.rest();
            if (rest.length < 4 || !trailer(rest)) {
                return Optional.empty();
            }
            out.encodeArr(java.util.Arrays.copyOf(rest, rest.length - 4));
            return Optional.of(out);
        }
        if (body.i32() != -1) {
            return Optional.empty();
        }
        body.end();
        return Optional.of(out);
    }

    /**
     * multiplayer-0930: 265 UserRemote.effect = i32 characterId + the local EFFECT body; 282 LP048E REMOTE_EFFECT is
     * the character id (read by CUserPool::OnUserRemotePacket) and then the same CUser::OnEffect 545EC40 body as
     * LP04CA (ingame-lp-map 0x48E -> 545EC40 證實). So the same verified {@link #EFFECT_TYPES} are translated; the
     * rest stays declined (LevelUp / SkillUse have their own 282 senders: Tms282ExpGain, Tms282SkillService).
     */
    static Optional<OutPacket> remoteEffect(OutPacket legacy, Charset charset) {
        Tms282LegacyPackets.Body body = Tms282LegacyPackets.Body.of(legacy);
        int characterId = body.i32();
        byte[] rest = body.rest();
        OutPacket local = new OutPacket();
        local.encodeShort(0); // stands for the opcode Body.of skips
        local.encodeArr(rest);
        Optional<OutPacket> translated = effect(local, charset, true);
        if (translated.isEmpty()) {
            return Optional.empty();
        }
        byte[] data = translated.get().getData();
        OutPacket out = Tms282LoginPacketSupport.packet(Tms282UserRemotePackets.REMOTE_EFFECT);
        out.encodeInt(characterId);
        out.encodeArr(java.util.Arrays.copyOfRange(data, 2, data.length));
        return Optional.of(out);
    }

    private static boolean trailer(byte[] rest) {
        int n = rest.length;
        return rest[n - 1] == -1 && rest[n - 2] == -1 && rest[n - 3] == -1 && rest[n - 4] == -1;
    }

    /** CNpcPool::OnNpcPacket ops (i32 objId first, as in 265): the 265 body under the 282 opcode. */
    static Optional<OutPacket> npc(OutPacket legacy, int op) {
        OutPacket out = Tms282LoginPacketSupport.packet(op);
        out.encodeArr(Tms282LegacyPackets.Body.of(legacy).rest());
        return Optional.of(out);
    }

    private static byte[] hex(String s) {
        return java.util.HexFormat.of().parseHex(s);
    }
}
