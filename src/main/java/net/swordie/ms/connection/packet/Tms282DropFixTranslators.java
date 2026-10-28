package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.handlers.header.OutHeader;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Optional;

/**
 * dropfix-0930 (research/tms2823/dropfix-0930/README.md): 265 packets the fail-closed OutboundGuard still dropped
 * although the 282.4 client (client-2823) has the same packet. Each rule was found by running the real 265 body
 * (OutboundGuard drop samples) through the client dispatcher for the candidate opcodes (dropfix-0930/sweep.py over
 * daily_trace.py) and confirmed statically; the translated fixtures read CLEAN (dropfix-0930/PART/expected).
 * MESSAGE / EFFECT / CLOCK additions live in their existing rules (Tms282SystemMessageTranslators,
 * Tms282DirectionTranslators) because a header can have only one rule.
 * <p>
 * Registered once in {@link Tms282FieldEncoders}; one part per client dispatcher range in {@link #PARTS}. Kill
 * switches: {@code -Dtms282.dropfix.enabled=false} (every part and the MESSAGE / EFFECT additions),
 * {@code -Dtms282.dropfix.PART.enabled=false}, and per risky rule as noted.
 */
public final class Tms282DropFixTranslators implements Tms282LegacyPackets.Contributor {
    public static final Tms282DropFixTranslators INSTANCE = new Tms282DropFixTranslators();

    /** A named part with its own kill switch. */
    record Part(String name, Tms282LegacyPackets.Contributor rules) {
    }

    /** In registration order. */
    static final List<Part> PARTS = List.of(
            new Part("local", Tms282DropFixTranslators::local), // CUserLocal 5788060 (dropfix-0930/local)
            new Part("ctxb", Tms282DropFixTranslators::ctxb),   // CWvsContext 5DF5B20 login-time / misc (ctx-b)
            new Part("ctxa", Tms282DropFixTranslators::ctxa),   // CWvsContext 5DF5B20 skill / system (ctx-a)
            new Part("field", Tms282DropFixTranslators::field)  // CField / pools (field)
    );

    private Tms282DropFixTranslators() {
    }

    public static List<String> partNames() {
        return PARTS.stream().map(Part::name).toList();
    }

    static boolean enabled(String key) {
        return !"false".equalsIgnoreCase(System.getProperty("tms282.dropfix.enabled", "true"))
                && !"false".equalsIgnoreCase(System.getProperty("tms282.dropfix." + key + ".enabled", "true"));
    }

    @Override
    public void contribute(Tms282LegacyPackets.Builder builder) {
        for (Part part : PARTS) {
            if (enabled(part.name())) {
                builder.with(part.rules());
            }
        }
    }

    /** The 265 body unchanged under the 282 opcode, exact length only. */
    static Optional<OutPacket> same(OutPacket legacy, int op, int width) {
        byte[] body = Tms282LegacyPackets.Body.of(legacy).rest();
        if (body.length != width) {
            return Optional.empty();
        }
        OutPacket out = Tms282LoginPacketSupport.packet(op);
        out.encodeArr(body);
        return Optional.of(out);
    }

    // ---- part "local": CUserLocal::OnPacket 5788060 (op-0x4BE, table 578DC3C), research dropfix-0930/local ----
    static final int OPEN_UI = 0x04D9, CLOSE_UI = 0x04DA, OPEN_UI_WITH_OPTION = 0x04DB, MOD_COMBO_RESPONSE = 0x04EC,
            SET_UTIL_DLG = 0x04F3, DODGE_SKILL_READY = 0x0504, AGGRO_RANK_INFO_NAME = 0x050D, IS_UNIVERSE = 0x053E,
            SEND_CLIENT_RESOLUTION = 0x057D, NOVILITY_SHILED = 0x05BB, SET_MONSTER_DEBUFF_MARK = 0x0604,
            HOLY_ADVENT_RESULT = 0x061B;
    /** skills-1006 (research/tms2823/skill-coverage-1006): 265 body unchanged, traced CLEAN with daily_trace. */
    /** skills-1006 batch 3 (agent-local): CUserLocal +77 block and neighbours, see research/tms2823/WORK-STATE.md.
     * SET_MONSTER_DEBUFF_MARK moved 05F2 -> 0604: 592EB70 (05F2) is the Battle Mage mark, 5921AA0 (0604) the Paladin one. */
    static final int ATTACK_MONSTER_DEBUFF_MARK = 0x0606,     // 5922370: 265 body unchanged (needs the 0604 mark object)
            ATTACK_ADVENTURER_MARK = 0x0608,                  // 59240C0: 265 body unchanged
            SET_MOB_ADVENTURER_MARK = 0x0607,                 // 5922F40 -> 8293F0: entry tail is u8, i32 (265: i32, u8)
            GUIDE_FORCE_ATOM = 0x032D,                        // CField 3AD2590: i32 type 1..4, see guideForceAtom
            REGISTER_EXTRA_SKILL = 0x059D,                    // 58EB950 -> 134A2B0 per entry = 265 ExtraSkill.encode
            SIT_TIME_CAPSULE = 0x0522;                        // inline 5789620, empty body
    static final int POOL_MAKER_REQUEST = 0x05E3,            // 591F99E: u8 on; on: i32 skill, i32 remain, i32 cooltime (400051074 at 591F9A8)
            BONUS_ATTACK_DELAY_REQUEST = 0x058A;              // 58D2920: i32 n, n x (i32 skill, i32 delay) std::map (Pathfinder, Shade)

    static void local(Tms282LegacyPackets.Builder b) {
        Charset cs = b.charset();
        b.on(OutHeader.CLOSE_UI, p -> same(p, CLOSE_UI, 4))                        // inline 57885C7 -> UI_Close 5DE9470
                .on(OutHeader.OPEN_UI_WITH_OPTION, Tms282DropFixTranslators::openUiWithOption) // 5794990
                .on(OutHeader.DODGE_SKILL_READY, p -> same(p, DODGE_SKILL_READY, 0))     // 5A26A00 (client answers CP0227)
                .on(OutHeader.AGGRO_RANK_INFO_NAME, p -> aggroRank(p, cs))                // inline 5788FAE
                .on(OutHeader.MOD_COMBO_RESPONSE, Tms282DropFixTranslators::modCombo)    // 588E450 reads i32 only
                .on(OutHeader.SET_UTIL_DLG, p -> utilDlg(p, cs))                          // 5894A40
                .on(OutHeader.IS_UNIVERSE, p -> same(p, IS_UNIVERSE, 1))                  // 58B6B40
                .on(OutHeader.SEND_CLIENT_RESOLUTION, p -> same(p, SEND_CLIENT_RESOLUTION, 0)) // 58CD510 (-> CP01BC)
                .on(OutHeader.HOLY_ADVENT_RESULT, p -> same(p, HOLY_ADVENT_RESULT, 0))   // 591C700
                .on(OutHeader.SET_MONSTER_DEBUFF_MARK, Tms282DropFixTranslators::debuffMark) // 5921AA0
                .on(OutHeader.SET_MOB_ADVENTURER_MARK, Tms282DropFixTranslators::mobAdventurerMark) // 5922F40
                .on(OutHeader.GUIDE_FORCE_ATOM, Tms282DropFixTranslators::guideForceAtom) // 3AD2590
                .on(OutHeader.LYNN_RESULT, Tms282DropFixTranslators::lynnResult) // LP0425 ALynnController leaves
                .on(OutHeader.POOL_MAKER_REQUEST, Tms282DropFixTranslators::poolMaker)
                .on(OutHeader.BONUS_ATTACK_DELAY_REQUEST, p -> intList(p, BONUS_ATTACK_DELAY_REQUEST, 2))
                .on(OutHeader.ATTACK_MONSTER_DEBUFF_MARK, p -> counted(p, ATTACK_MONSTER_DEBUFF_MARK, 12, 8, 4))
                .on(OutHeader.ATTACK_ADVENTURER_MARK, p -> counted(p, ATTACK_ADVENTURER_MARK, 12, 8, 0))
                .on(OutHeader.REGISTER_EXTRA_SKILL, Tms282DropFixTranslators::registerExtraSkill)
                .on(OutHeader.SIT_TIME_CAPSULE, p -> same(p, SIT_TIME_CAPSULE, 0))
                .on(OutHeader.EXTRA_SYSTEM_RESULT, Tms282DropFixTranslators::extraSystem);
        if (enabled("openUi")) {
            b.on(OutHeader.OPEN_UI, p -> same(p, OPEN_UI, 4));                          // 5794350, UI ids 推定 = 265
        }
        if (enabled("novility")) {
            b.on(OutHeader.NOVILITY_SHILED, Tms282DropFixTranslators::novility);         // 578B61E i32,i32,i32 (推定)
        }
    }

    /** 265 UserLocal.poolMakerRequest: u8 on, then (on only) skill, remain, cooltime. */
    static Optional<OutPacket> poolMaker(OutPacket legacy) {
        Tms282LegacyPackets.Body body = Tms282LegacyPackets.Body.of(legacy);
        int on = body.u8();
        OutPacket out = Tms282LoginPacketSupport.packet(POOL_MAKER_REQUEST);
        out.encodeByte(on);
        if (on != 0) {
            out.encodeInt(body.i32());
            out.encodeInt(body.i32());
            out.encodeInt(body.i32());
        }
        body.end();
        return Optional.of(out);
    }

    static Optional<OutPacket> openUiWithOption(OutPacket legacy) {
        Tms282LegacyPackets.Body body = Tms282LegacyPackets.Body.of(legacy);
        int ui = body.i32();
        int option = body.i32();
        int n = body.i32();
        if (n < 0 || n > 64) {
            return Optional.empty();
        }
        OutPacket out = Tms282LoginPacketSupport.packet(OPEN_UI_WITH_OPTION);
        out.encodeInt(ui);
        out.encodeInt(option);
        out.encodeInt(n);
        for (int i = 0; i < n; i++) {
            out.encodeInt(body.i32());
        }
        body.end();
        return Optional.of(out);
    }

    static Optional<OutPacket> aggroRank(OutPacket legacy, Charset cs) {
        Tms282LegacyPackets.Body body = Tms282LegacyPackets.Body.of(legacy);
        int n = body.i32();
        if (n < 0 || n > 64) {
            return Optional.empty();
        }
        OutPacket out = Tms282LoginPacketSupport.packet(AGGRO_RANK_INFO_NAME);
        out.encodeInt(n);
        for (int i = 0; i < n; i++) {
            Tms282LoginPacketSupport.str(out, body.str(), cs);
        }
        body.end();
        return Optional.of(out);
    }

    static Optional<OutPacket> modCombo(OutPacket legacy) {
        Tms282LegacyPackets.Body body = Tms282LegacyPackets.Body.of(legacy);
        int combo = body.i32();
        body.i32();
        body.end();
        OutPacket out = Tms282LoginPacketSupport.packet(MOD_COMBO_RESPONSE);
        out.encodeInt(combo);
        return Optional.of(out);
    }

    static Optional<OutPacket> utilDlg(OutPacket legacy, Charset cs) {
        Tms282LegacyPackets.Body body = Tms282LegacyPackets.Body.of(legacy);
        String msg = body.str();
        int npc = body.i32();
        body.end();
        OutPacket out = Tms282LoginPacketSupport.packet(SET_UTIL_DLG);
        Tms282LoginPacketSupport.str(out, msg, cs);
        out.encodeInt(npc);
        return Optional.of(out);
    }

    private static int le32(byte[] b, int at) {
        return (b[at] & 0xFF) | (b[at + 1] & 0xFF) << 8 | (b[at + 2] & 0xFF) << 16 | (b[at + 3] & 0xFF) << 24;
    }

    private static Optional<OutPacket> raw(int op, byte[] body, byte... tail) {
        OutPacket out = Tms282LoginPacketSupport.packet(op);
        out.encodeArr(body);
        if (tail.length > 0) out.encodeArr(tail);
        return Optional.of(out);
    }

    /** A 265 body kept as is when it is exactly: countAt bytes, i32 n, n entries of perEntry bytes, tail bytes. */
    static Optional<OutPacket> counted(OutPacket legacy, int op, int countAt, int perEntry, int tail) {
        byte[] body = Tms282LegacyPackets.Body.of(legacy).rest();
        if (body.length < countAt + 4) return Optional.empty();
        int n = le32(body, countAt);
        if (n < 0 || n > 4096 || body.length != countAt + 4 + (long) n * perEntry + tail) return Optional.empty();
        return raw(op, body);
    }

    /** 265 UserLocal.setMonsterDebuffMark: u8 0 clears; the set form gains the trailing empty map (i32 0) 828200 reads. */
    static Optional<OutPacket> debuffMark(OutPacket legacy) {
        byte[] body = Tms282LegacyPackets.Body.of(legacy).rest();
        if (body.length == 1 && body[0] == 0) return raw(SET_MONSTER_DEBUFF_MARK, body);
        if (body.length < 37 || body[0] != 1) return Optional.empty();
        int n = le32(body, 1);
        if (n < 0 || n > 4096 || body.length < 5 + (long) n * 4 + 28) return Optional.empty();
        int at = 5 + n * 4 + 24;
        int cnt = le32(body, at);
        if (cnt < 0 || cnt > 4096 || body.length != at + 4 + (long) cnt * 16) return Optional.empty();
        return raw(SET_MONSTER_DEBUFF_MARK, body, (byte) 0, (byte) 0, (byte) 0, (byte) 0);
    }

    /**
     * 265 ExtraTMSSystem.peck / purify (LYNN_RESULT kind 7): i32 chr, u8 7, i32 actor 112, i32 class 0x243FF351, u8 1,
     * u16 21 u16 flags, u16 22 i32 skill, u16 23 i32 mob, u16 24 i32 level, 3 x u8 0. 282: the controller made by
     * {@link Tms282LynnPackets} (actor 0x51000000 + chr), leaves + 1 (22 GuardianFlags, 23..25 ActivatingPecking*).
     * Every other LYNN_RESULT shape (FocusBuffList arrays, the 0x825AA912 actor) stays declined.
     */
    static Optional<OutPacket> lynnResult(OutPacket legacy) {
        byte[] body = Tms282LegacyPackets.Body.of(legacy).rest();
        if (body.length != 14 + 4 + 6 + 6 + 6 + 3 || body[4] != 7 || le32(body, 9) != Tms282LynnPackets.CLASS_SIGNATURE
                || body[13] != 1 || le16(body, 14) != 21 || le16(body, 18) != 22 || le16(body, 24) != 23
                || le16(body, 30) != 24) return Optional.empty();
        int chr = le32(body, 0);
        if (chr <= 0 || chr > 0x00FFFFFF) return Optional.empty();
        OutPacket out = Tms282LoginPacketSupport.packet(Tms282LynnPackets.OPCODE);
        out.encodeInt(chr);
        out.encodeByte(0);
        out.encodeInt(0x51000000 + chr);
        out.encodeInt(Tms282LynnPackets.CLASS_SIGNATURE);
        out.encodeByte(3);
        out.encodeByte(0);
        out.encodeByte(0);
        out.encodeShort(22);
        out.encodeShort(le16(body, 16));
        out.encodeShort(23);
        out.encodeInt(le32(body, 20));
        out.encodeShort(24);
        out.encodeInt(le32(body, 26));
        out.encodeShort(25);
        out.encodeInt(le32(body, 32));
        out.encodeShort(0);
        out.encodeShort(0);
        out.encodeByte(0);
        out.encodeInt(0);
        return Optional.of(out);
    }

    private static int le16(byte[] b, int at) {
        return (b[at] & 0xFF) | (b[at + 1] & 0xFF) << 8;
    }

    /**
     * 265 FieldPacket.guideForceAtom (only caller: 追蹤箭 400031000 in Job): i32 key, i32 owner, 3 x i32 1, i32 mob.
     * 282 3AD2590 reads i32 type: 1 nothing; 2 i32 key ([atom+0x34]), i32 owner ([atom+0x174]); 3 3AD29E0; 4 3AD2E60 i32 a
     * ([atom+0x10C]), i32 owner, i32 mob -> 3A9FEA0(atom, mob). Type 4 with a = the skill id is sent (a is not the key
     * field of type 2; 推定 skill). A mismatch is a no-op in the client loop. 未實測.
     */
    static Optional<OutPacket> guideForceAtom(OutPacket legacy) {
        byte[] body = Tms282LegacyPackets.Body.of(legacy).rest();
        if (body.length != 24 || le32(body, 8) != 1 || le32(body, 12) != 1 || le32(body, 16) != 1) return Optional.empty();
        OutPacket out = Tms282LoginPacketSupport.packet(GUIDE_FORCE_ATOM);
        out.encodeInt(4);
        out.encodeInt(400031000);
        out.encodeInt(le32(body, 4));
        out.encodeInt(le32(body, 20));
        return Optional.of(out);
    }

    /**
     * 265 UserLocal.setMobAdvMark: i32 skill, u8 set [, i32 n, n x (i32 mob, 4 x i32, i32 e, u8 f)]. 282 5922F40 reads the
     * same head and 8293F0 per entry i32 mob, 4 x i32, u8 f, i32 e (the mark object is built by 829660 from the skill id:
     * unknown ids make no object). Only the Wild Hunter marks 33001005 / 33110010 are let through (others untested).
     */
    static Optional<OutPacket> mobAdventurerMark(OutPacket legacy) {
        byte[] body = Tms282LegacyPackets.Body.of(legacy).rest();
        if (body.length < 5) return Optional.empty();
        int skill = le32(body, 0);
        if (skill != 33001005 && skill != 33110010) return Optional.empty();
        if (body[4] == 0) return body.length == 5 ? raw(SET_MOB_ADVENTURER_MARK, body) : Optional.empty();
        if (body[4] != 1 || body.length < 9) return Optional.empty();
        int n = le32(body, 5);
        if (n <= 0 || n > 256 || body.length != 9 + n * 25) return Optional.empty();
        byte[] out = body.clone();
        for (int i = 0; i < n; i++) {
            int at = 9 + i * 25 + 20; // 265 i32 e, u8 f -> 282 u8 f, i32 e
            out[at] = body[at + 4];
            System.arraycopy(body, at, out, at + 1, 4);
        }
        return raw(SET_MOB_ADVENTURER_MARK, out);
    }

    /** 265 UserLocal.registerExtraSkill: i32 skill, u16 n, n x ExtraSkill.encode (same fields as 134A2B0). */
    static Optional<OutPacket> registerExtraSkill(OutPacket legacy) {
        byte[] body = Tms282LegacyPackets.Body.of(legacy).rest();
        if (body.length < 6) return Optional.empty();
        int n = (body[4] & 0xFF) | (body[5] & 0xFF) << 8;
        if (n == 0 ? body.length != 6 : body.length < 6 + n * 60) return Optional.empty();
        return raw(REGISTER_EXTRA_SKILL, body);
    }

    /** 265 WvsContext.sendExtraSystemInit / *Stack / *Power: method 33 -> 41, 34 -> 42; the reply request is dropped
     * (bool 0) because 282 has no known inbound opcode for it. */
    static Optional<OutPacket> extraSystem(OutPacket legacy) {
        byte[] body = Tms282LegacyPackets.Body.of(legacy).rest();
        if (body.length < 6 || le32(body, 0) != EXTRA_SYSTEM_LEGACY) return Optional.empty();
        int method = (body[4] & 0xFF) | (body[5] & 0xFF) << 8;
        OutPacket out = Tms282LoginPacketSupport.packet(EXTRA_SYSTEM_RPC);
        out.encodeInt(EXTRA_SYSTEM_INTERFACE);
        if (method == 33 && body.length == 6) {
            out.encodeShort(41);
            return Optional.of(out);
        }
        if (method != 34 || body.length != 26 || body[19] != 1) return Optional.empty();
        out.encodeShort(42);
        out.encodeArr(java.util.Arrays.copyOfRange(body, 6, 19)); // u8 type, i32 value, i32 time, i32 flag
        out.encodeByte(0);
        return Optional.of(out);
    }

    static Optional<OutPacket> rwMultiChargeCancel(OutPacket legacy) {
        byte[] body = Tms282LegacyPackets.Body.of(legacy).rest();
        return body.length >= 5 && body.length == 5 + (body[4] & 0xFF) * 4 ? raw(RW_MULTI_CHARGE_CANCEL, body) : Optional.empty();
    }

    /** 265 FieldPacket.delWreckage: owner, n, bool, n x id; 282 reads a second u8 after the bool (0 for Evan). */
    static Optional<OutPacket> delWreckage(OutPacket legacy) {
        byte[] body = Tms282LegacyPackets.Body.of(legacy).rest();
        if (body.length < 9) return Optional.empty();
        int n = le32(body, 4);
        if (n < 0 || n > 4096 || body.length != 9 + (long) n * 4) return Optional.empty();
        OutPacket out = Tms282LoginPacketSupport.packet(DEL_WRECKAGE);
        out.encodeArr(java.util.Arrays.copyOfRange(body, 0, 9));
        out.encodeByte(0);
        out.encodeArr(java.util.Arrays.copyOfRange(body, 9, body.length));
        return Optional.of(out);
    }

    /** 265 MobPool: mob, skill, chr; 3F42070 stores two more ints (never read in CMob: 0). */
    static Optional<OutPacket> mobSelectedEffect(OutPacket legacy) {
        byte[] body = Tms282LegacyPackets.Body.of(legacy).rest();
        return body.length == 12 ? raw(MOB_SELECTED_EFFECT_BY_SKILL, body, new byte[8]) : Optional.empty();
    }

    /** 265 FieldPacket.createArkForceAtom: chr, skill, n groups; 377D7B0 CLEAN on the unchanged body. */
    static Optional<OutPacket> arkForceAtom(OutPacket legacy) {
        byte[] body = Tms282LegacyPackets.Body.of(legacy).rest();
        if (body.length < 12) return Optional.empty();
        int n = le32(body, 8);
        return n >= 0 && n <= 64 ? raw(CREATE_ARK_FORCE_ATOM, body) : Optional.empty();
    }

    /** owner, object, u8 action, i32 skill; Salamander Mischief adds i32 hide (4527460 reads it for 400021092). */
    static Optional<OutPacket> summonedSkill(OutPacket legacy) {
        byte[] body = Tms282LegacyPackets.Body.of(legacy).rest();
        return body.length == 13 || body.length == 17 ? raw(SUMMONED_SKILL, body) : Optional.empty();
    }

    /** 265 sends owner, object, mob; 45289E0 reads a count first. */
    static Optional<OutPacket> beholderRevenge(OutPacket legacy) {
        byte[] body = Tms282LegacyPackets.Body.of(legacy).rest();
        if (body.length != 12) return Optional.empty();
        OutPacket out = Tms282LoginPacketSupport.packet(SUMMONED_BEHOLDER_REVENGE);
        out.encodeArr(java.util.Arrays.copyOfRange(body, 0, 8));
        out.encodeInt(1);
        out.encodeArr(java.util.Arrays.copyOfRange(body, 8, 12));
        return Optional.of(out);
    }

    static Optional<OutPacket> summonedStateChanged(OutPacket legacy) {
        byte[] body = Tms282LegacyPackets.Body.of(legacy).rest();
        if (body.length < 12) return Optional.empty();
        int type = le32(body, 8);
        if (type == 1) return body.length == 12 ? raw(SUMMONED_STATE_CHANGED, body) : Optional.empty();
        if (type != 2 || body.length < 16) return Optional.empty();
        int n = le32(body, 12);
        return n >= 0 && n <= 4096 && body.length == 16 + (long) n * 8 ? raw(SUMMONED_STATE_CHANGED, body) : Optional.empty();
    }

    static Optional<OutPacket> summonedEffect(OutPacket legacy) {
        byte[] body = Tms282LegacyPackets.Body.of(legacy).rest();
        if (body.length < 12) return Optional.empty();
        int type = le32(body, 8);
        if (type >= 0 && type <= 2) return body.length == 12 ? raw(SUMMONED_EFFECT, body) : Optional.empty();
        if (type == 3) return body.length == 16 ? raw(SUMMONED_EFFECT, body) : Optional.empty();
        if (type == 4) return body.length == 16 ? raw(SUMMONED_EFFECT, body, (byte) 0) : Optional.empty();
        return Optional.empty();
    }

    static Optional<OutPacket> summonedUseSkill(OutPacket legacy) {
        byte[] body = Tms282LegacyPackets.Body.of(legacy).rest();
        return body.length == 12 ? raw(SUMMONED_USE_SPECIFIED_SKILL, body, new byte[5]) : Optional.empty();
    }

    /** Only the two forms 4529310 reads like 265: type 2 (crystal count, state) and type 3 (reset). */
    static Optional<OutPacket> summonedUpgradeStage(OutPacket legacy) {
        byte[] body = Tms282LegacyPackets.Body.of(legacy).rest();
        if (body.length < 12) return Optional.empty();
        int type = le32(body, 8);
        return type == 2 && body.length == 20 || type == 3 && body.length == 12
                ? raw(SUMMONED_UPGRADE_STAGE, body) : Optional.empty();
    }

    static Optional<OutPacket> novility(OutPacket legacy) {
        Tms282LegacyPackets.Body body = Tms282LegacyPackets.Body.of(legacy);
        int shield = body.i32();
        int second = body.i32();
        body.end();
        OutPacket out = Tms282LoginPacketSupport.packet(NOVILITY_SHILED);
        out.encodeInt(shield);
        out.encodeInt(second);
        out.encodeInt(0);
        return Optional.of(out);
    }

    // ---- part "ctxb": CWvsContext::OnPacket 5DF5B20 (op-0x8F, table 5DFB140), research dropfix-0930/ctx-b ----
    static final int MOB_DROP_MESO_PICKUP = 0x00A5, CLAIM_SVR_STATUS_CHANGED = 0x00BA,
            CASHPET_PICK_UP_ON_OFF_RESULT = 0x00EF, SET_POTION_DISCOUNT_RATE = 0x00F5, HOUR_CHANGE = 0x00FB,
            SET_BUY_EQUIP_EXT = 0x0105, CLEAR_ANNOUNCED_QUEST = 0x011C, UNK_372 = 0x0177, EVENT_NAME_TAG = 0x017B,
            UPDATE_TIME_UNK = 0x0227, USER_CHARACTER_LIST_RESULT = 0x022E;

    static void ctxb(Tms282LegacyPackets.Builder b) {
        Charset cs = b.charset();
        b.on(OutHeader.MOB_DROP_MESO_PICKUP, p -> same(p, MOB_DROP_MESO_PICKUP, 4))            // 5EFB230 i32 (BattleRecord)
                .on(OutHeader.CLAIM_SVR_STATUS_CHANGED, p -> same(p, CLAIM_SVR_STATUS_CHANGED, 1))   // 5F27840 u8
                .on(OutHeader.CASHPET_PICK_UP_ON_OFF_RESULT, p -> same(p, CASHPET_PICK_UP_ON_OFF_RESULT, 2)) // 5DF6178 SP 0x324E/F
                .on(OutHeader.SET_POTION_DISCOUNT_RATE, p -> same(p, SET_POTION_DISCOUNT_RATE, 1))   // 5F29C00 u8
                .on(OutHeader.HOUR_CHANGE, p -> same(p, HOUR_CHANGE, 4))                             // 5DF6703 u16, u16
                .on(OutHeader.CLEAR_ANNOUNCED_QUEST, p -> same(p, CLEAR_ANNOUNCED_QUEST, 0))         // 5F31530
                .on(OutHeader.UNK_372, p -> same(p, UNK_372, 0))                                     // 5DF7E4F event-list flag
                .on(OutHeader.UPDATE_TIME_UNK, p -> same(p, UPDATE_TIME_UNK, 8))                     // 5F4D3F0 FILETIME (推定)
                .on(OutHeader.EVENT_NAME_TAG, p -> eventNameTag(p, cs))                              // 5E4E270 5 x (str, u8)
                .on(OutHeader.USER_CHARACTER_LIST_RESULT, p -> characterList(p, cs));                // 5F7CAE0 -> 83AF00
        if ("true".equalsIgnoreCase(System.getProperty("tms282.buyEquipExt.enabled", "false"))) {  // semantics 推定: off
            b.on(OutHeader.SET_BUY_EQUIP_EXT, p -> same(p, SET_BUY_EQUIP_EXT, 0));               // 5DF6851
        }
    }

    static Optional<OutPacket> eventNameTag(OutPacket legacy, Charset cs) {
        Tms282LegacyPackets.Body body = Tms282LegacyPackets.Body.of(legacy);
        OutPacket out = Tms282LoginPacketSupport.packet(EVENT_NAME_TAG);
        for (int i = 0; i < 5; i++) {
            Tms282LoginPacketSupport.str(out, body.str(), cs);
            out.encodeByte(body.u8());
        }
        body.end();
        return Optional.of(out);
    }

    /** 265 entries end their burning block with two i32 (0) that 282 8D93D0 does not read: dropped. */
    static Optional<OutPacket> characterList(OutPacket legacy, Charset cs) {
        Tms282LegacyPackets.Body b = Tms282LegacyPackets.Body.of(legacy);
        int n = b.i32();
        if (n < 0 || n > 64) {
            return Optional.empty();
        }
        OutPacket out = Tms282LoginPacketSupport.packet(USER_CHARACTER_LIST_RESULT);
        out.encodeInt(n);
        for (int i = 0; i < n; i++) {
            out.encodeInt(b.i32());
            out.encodeShort(b.u16());
            out.encodeInt(b.i32());
            out.encodeLong(b.i64());
            Tms282LoginPacketSupport.str(out, b.str(), cs);
            out.encodeArr(b.bytes(16));
            out.encodeInt(b.i32());
            out.encodeInt(b.i32());
            out.encodeInt(b.i32());
            out.encodeByte(b.u8());
            out.encodeInt(b.i32());
            Tms282LoginPacketSupport.str(out, b.str(), cs);
            out.encodeInt(b.i32());
            Tms282LoginPacketSupport.str(out, b.str(), cs);
            Tms282LoginPacketSupport.str(out, b.str(), cs);
            Tms282LoginPacketSupport.str(out, b.str(), cs);
            b.i32();
            b.i32();
            out.encodeByte(b.u8());
        }
        b.end();
        return Optional.of(out);
    }

    // ---- part "ctxa": CWvsContext::OnPacket 5DF5B20 skill / system headers, research dropfix-0930/ctx-a ----
    // shift anchors: BOSS_MATCHING 553 -> 0x24D (+36), EXP_DROP_PENALTY 561 -> 0x256 (+37), STACK_SKILL 570 -> 0x266,
    // HEXA_SKILL_ERDA 586 -> 0x277 (+45), DAMAGE_SKIN_SAVE 610 -> 0x28E, USER_OPEN_QUICK_MOVE 619 -> 0x296 (+43).
    static final int EXP_DROP_PENALTY = 0x0256;              // 5E7CB20: u16, i32 x4 (string 0x42B1 MVP death-penalty) CLEAN
    static final int ACHIEVEMENT_UPDATE = 0x0257;            // 5E7CD30: u8 (0 -> nothing more) CLEAN
    static final int PASSIVE_DEBUFF_SKILL_REQUEST = 0x0258;  // 6002570: i32 n, n x i32 (std::set<int>) CLEAN
    static final int SET_ASCENT_SKILL_STACK_REQUEST = 0x0259;// 60026F0 -> 504EF0: i32 n, n x (i32 key, i32 value) (std::map<int,int>) CLEAN
    static final int UNION_ARTIFACT_INFORMATION = 0x0279;    // 53FDAC0: i32 n, n x (i32,i32,i32); read-complete (handler never returns in emulation)
    static final int SEND_MIGRATE_SECURITY_RESULT = 0x0289;  // 5ED26A0: str, i32 CLEAN
    static final int DAMAGE_SKIN_SAVE_RESULT = 0x028E;       // 5FB2C50: u8 req, u8 res (damage-skin strings 0x3A5C..0x3A66) CLEAN
    static final int SKILL_SEQUENCE_SKILLS = 0x0298;         // 903E45..: u8 n, n x (str, 15 x i32), u8, u8, u8 CLEAN
    static final int SKILL_SEQUENCE_BUFFS = 0x0299;          // 904411..: u8, u8, i32 n, n x (buf9, 30 x i32, 30 x i32) CLEAN
    static final int SKILL_SEQUENCE_BUFFS_INFORMATION = 0x029A; // 7656E0 x2 (int vectors) + 1B31CA0 (i32 n1 records, i32 n2 (i32,str)) CLEAN with +i32 0,+i32 0
    static final int REN_IMUGI_SPIRIT_SWORD_REQUEST = 0x02AC;// 60043D0: i32 mode; mode 0: u8, i32 skillId; mode 1: i32 CLEAN
    static final int LINKED_SKILL_INFO = 0x0251;             // 5FD2CE0: i32 type; 0: u8, i32 n, n x i32; 1: u8; 2/3: u8, i32 (link-skill-1006)
    /** skills-1006: Blaze Wizard, adjacent handlers 5FF0729 / 5FF079D, 265 bodies unchanged (sweep.py ctx CLEAN). */
    static final int FLAME_WALK_EFFECT = 0x01AF,             // i32 chr
            FLARE_BLINK = 0x01B0;                            // i32 chr, u8 used; unused: u8, u16 n, n x (i16 x, i16 y), i16, i16


    static void ctxa(Tms282LegacyPackets.Builder builder) {
        Charset cs = builder.charset();
        builder.on(OutHeader.EXP_DROP_PENALTY, p -> same(p, EXP_DROP_PENALTY, 18))
                .on(OutHeader.ACHIEVEMENT_UPDATE, Tms282DropFixTranslators::achievementUpdate)
                .on(OutHeader.PASSIVE_DEBUFF_SKILL_REQUEST, p -> intList(p, PASSIVE_DEBUFF_SKILL_REQUEST, 1))
                .on(OutHeader.SET_ASCENT_SKILL_STACK_REQUEST, p -> intList(p, SET_ASCENT_SKILL_STACK_REQUEST, 2))
                .on(OutHeader.SEND_MIGRATE_SECURITY_RESULT, p -> migrateSecurity(p, cs))
                .on(OutHeader.DAMAGE_SKIN_SAVE_RESULT, p -> same(p, DAMAGE_SKIN_SAVE_RESULT, 2)) // u8, u8 forms only
                .on(OutHeader.SKILL_SEQUENCE_SKILLS, p -> sequenceSkills(p, cs))
                .on(OutHeader.SKILL_SEQUENCE_BUFFS, p -> sequenceBuffs(p, cs))
                .on(OutHeader.SKILL_SEQUENCE_BUFFS_INFORMATION, Tms282DropFixTranslators::sequenceBuffsInformation)
                .on(OutHeader.REN_IMUGI_SPIRIT_SWORD_REQUEST, Tms282DropFixTranslators::renImugi)
                .on(OutHeader.LINKED_SKILL_INFO, Tms282DropFixTranslators::linkedSkillInfo)
                .on(OutHeader.FLAME_WIZARD_FLAME_WALK_EFFECT, p -> same(p, FLAME_WALK_EFFECT, 4))
                .on(OutHeader.FLAME_WIZARD_FLARE_BLINK, Tms282DropFixTranslators::flareBlink);
        // opcode by position only and the handler never returned in emulation (read-complete): off by default
        if ("true".equalsIgnoreCase(System.getProperty("tms282.dropfix.unionArtifact.enabled", "false"))) {
            builder.on(OutHeader.UNION_ARTIFACT_INFORMATION, p -> intList(p, UNION_ARTIFACT_INFORMATION, 3));
        }
    }

    static Optional<OutPacket> achievementUpdate(OutPacket legacy) {
        Tms282LegacyPackets.Body body = Tms282LegacyPackets.Body.of(legacy);
        int type = body.u8();
        body.end();
        if (type != 0) return Optional.empty(); // only type 0 traced (non-zero reads a large record, 5E7CD60..)
        OutPacket out = Tms282LoginPacketSupport.packet(ACHIEVEMENT_UPDATE);
        out.encodeByte(0);
        // 282.4 live dump 48116: type 0 still reads a throw-mode AUX byte at 5E7DD35.
        // A zero flag ends the optional section; a plain frame throws ZException 0x26 during field entry.
        out.setAuxiliaryData(new byte[]{0});
        return Optional.of(out);
    }

    /** i32 n, n x (perEntry x i32): the same bytes, count checked against the body length. */
    static Optional<OutPacket> intList(OutPacket legacy, int op, int perEntry) {
        Tms282LegacyPackets.Body body = Tms282LegacyPackets.Body.of(legacy);
        int n = body.i32();
        if (n < 0 || (long) n * perEntry * 4 != body.remaining()) return Optional.empty();
        OutPacket out = Tms282LoginPacketSupport.packet(op);
        out.encodeInt(n);
        out.encodeArr(body.rest());
        return Optional.of(out);
    }

    static Optional<OutPacket> migrateSecurity(OutPacket legacy, Charset cs) {
        Tms282LegacyPackets.Body body = Tms282LegacyPackets.Body.of(legacy);
        String s = body.str();
        int v = body.i32();
        body.end();
        OutPacket out = Tms282LoginPacketSupport.packet(SEND_MIGRATE_SECURITY_RESULT);
        Tms282LoginPacketSupport.str(out, s, cs);
        out.encodeInt(v);
        return Optional.of(out);
    }

    static Optional<OutPacket> sequenceSkills(OutPacket legacy, Charset cs) {
        Tms282LegacyPackets.Body body = Tms282LegacyPackets.Body.of(legacy);
        int n = body.u8();
        OutPacket out = Tms282LoginPacketSupport.packet(SKILL_SEQUENCE_SKILLS);
        out.encodeByte(n);
        for (int i = 0; i < n; i++) {
            Tms282LoginPacketSupport.str(out, body.str(), cs);   // name re-encoded (265 UTF-8)
            out.encodeArr(body.bytes(15 * 4));
        }
        out.encodeArr(body.bytes(3));
        body.end();
        return Optional.of(out);
    }

    static Optional<OutPacket> sequenceBuffs(OutPacket legacy, Charset cs) {
        Tms282LegacyPackets.Body body = Tms282LegacyPackets.Body.of(legacy);
        OutPacket out = Tms282LoginPacketSupport.packet(SKILL_SEQUENCE_BUFFS);
        out.encodeArr(body.bytes(2));
        int n = body.i32();
        if (n < 0 || (long) n * (9 + 240) != body.remaining()) return Optional.empty();
        out.encodeInt(n);
        for (int i = 0; i < n; i++) {
            byte[] name = body.bytes(9);                            // 265: UTF-8 padded to 9; 282 DecodeBuffer(9)
            int len = 0; while (len < 9 && name[len] != 0) len++;
            byte[] enc = new String(name, 0, len, java.nio.charset.StandardCharsets.UTF_8).getBytes(cs);
            out.encodeArr(java.util.Arrays.copyOf(enc, 9));          // truncated / zero-padded to 9 (推定: client null-terminates)
            out.encodeArr(body.bytes(240));
        }
        body.end();
        return Optional.of(out);
    }

    /** 265 body (two int vectors) + the two 282-only lists as empty (1B31CA0: i32 0 records, i32 0 (i32,str)). */
    static Optional<OutPacket> sequenceBuffsInformation(OutPacket legacy) {
        Tms282LegacyPackets.Body body = Tms282LegacyPackets.Body.of(legacy);
        OutPacket out = Tms282LoginPacketSupport.packet(SKILL_SEQUENCE_BUFFS_INFORMATION);
        for (int k = 0; k < 2; k++) {
            int n = body.i32();
            if (n < 0 || n * 4L > body.remaining()) return Optional.empty();
            out.encodeInt(n);
            out.encodeArr(body.bytes(n * 4));
        }
        body.end();
        out.encodeInt(0);
        out.encodeInt(0);
        return Optional.of(out);
    }

    /** 265 WvsContext.flameWizardFlareBlink: 5 bytes (used) or 16 bytes (set position, one point). */
    static Optional<OutPacket> flareBlink(OutPacket legacy) {
        Tms282LegacyPackets.Body body = Tms282LegacyPackets.Body.of(legacy);
        int len = body.remaining();
        if (len != 5 && len != 16) return Optional.empty();
        OutPacket out = Tms282LoginPacketSupport.packet(FLARE_BLINK);
        out.encodeArr(body.rest());
        return Optional.of(out);
    }

    /** 265 WvsContext.linkedSkillInfo, unchanged: 282 5FD2CE0 reads the same fields for types 0..3. */
    static Optional<OutPacket> linkedSkillInfo(OutPacket legacy) {
        Tms282LegacyPackets.Body b = Tms282LegacyPackets.Body.of(legacy);
        int type = b.i32();
        OutPacket out = Tms282LoginPacketSupport.packet(LINKED_SKILL_INFO);
        out.encodeInt(type);
        switch (type) {
            case 0 -> {
                out.encodeByte(b.u8());
                int n = b.i32();
                if (n < 0 || n > 64) {
                    return Optional.empty();
                }
                out.encodeInt(n);
                for (int i = 0; i < n; i++) {
                    out.encodeInt(b.i32());
                }
            }
            case 1 -> out.encodeByte(b.u8());
            case 2, 3 -> {
                out.encodeByte(b.u8());
                out.encodeInt(b.i32());
            }
            default -> {
                return Optional.empty();
            }
        }
        b.end();
        return Optional.of(out);
    }

    static Optional<OutPacket> renImugi(OutPacket legacy) {
        Tms282LegacyPackets.Body body = Tms282LegacyPackets.Body.of(legacy);
        int mode = body.i32();
        if (mode != 0) return Optional.empty();
        int flag = body.u8();
        int skillId = body.i32();
        body.end();
        OutPacket out = Tms282LoginPacketSupport.packet(REN_IMUGI_SPIRIT_SWORD_REQUEST);
        out.encodeInt(0);
        out.encodeByte(flag);
        out.encodeInt(skillId);
        return Optional.of(out);
    }


    // ---- part "field": CField 3725AF0 and the pools, research dropfix-0930/field ----
    /** skills-1006: CSummoned methods behind 45AB780 (owner, object id read by the pool), static decode order = 265. */
    /** skills-1006 batch 3 (agent-summon): 45AB780 reads owner + object; 0664..0683 = 265 + 48, 0685..0689 = 265 + 49. */
    static final int SUMMONED_ACTION_CHANGE = 0x067A,         // 4528280: u8
            SUMMONED_BEHOLDER_REVENGE = 0x067E,               // 45289E0: i32 n, n x i32 mob (265 sends one mob, no count)
            SUMMONED_SPECIAL_ASSIST = 0x067F,                 // 4528D60: i32 skill, u8
            SUMMONED_STATE_CHANGED = 0x0680,                  // vtbl+0x40: i32 type; type 2: i32 n, n x (i32, i32)
            SUMMONED_EFFECT = 0x0685,                         // 4529230: i32 type; 3: i32; 4: i32, u8
            SUMMONED_USE_SPECIFIED_SKILL = 0x0686,            // 45292A0: i32 skill, i32 x, u8 flag
            SUMMONED_UPGRADE_STAGE = 0x0687,                  // 4529310: i32 type; 2: i32 count, i32 state; 3: nothing
            SUMMONED_LOTUS_POWER = 0x0689;                    // 45879D0: nothing
    /** skills-1006 batch 3 (agent-userpool): user common 5AA7B10 (+56 / +57), remote 5AA6E40, CField, CMobPool. */
    static final int INHUMAN_SPEED_RESULT = 0x042E,           // 5613E50: u8, i32 (400031020 gate at user+0x7E8C)
            SHOOT_OBJECT_EXPLODE_RESULT = 0x041D,             // 56A1C60: i32 n, n x i32 (0420 has the same shape: 推定)
            RW_MULTI_CHARGE_CANCEL = 0x04AE,                  // 5AFA110: u8 n, n x i32 skill
            DEL_WRECKAGE = 0x0369,                            // 37A28F0: i32 owner, i32 n, u8, u8 (new), n x i32
            MOB_SELECTED_EFFECT_BY_SKILL = 0x06F4,            // 3F42070: i32 skill, i32 chr, i32, i32 (two new, 0)
            CREATE_ARK_FORCE_ATOM = 0x032C;                   // 377D7B0 reads the 265 body unchanged
    /** skills-1006 batch 3 (agent-moxuan-cts): LP0244 typed RPC, 265 methods 33 / 34 = 282 methods 41 / 42 (UIMukHyun). */
    static final int EXTRA_SYSTEM_RPC = 0x0244, EXTRA_SYSTEM_INTERFACE = 0x3F7D24B3, EXTRA_SYSTEM_LEGACY = -1289454273;
    static final int SUMMONED_SKILL = 0x0674,              // 4527460: u8 type, i32 skill (plain summons; shoot-object form not proven)
            SUMMONED_ASSIST_ATTACK = 0x067B,               // 4528690: i32 identifier, i32
            SUMMONED_REPOSITION = 0x0682;                  // 45290C0: i32 skill, i32 slv, i32 x, i32 y
    static final int MOB_AFFECTED = 0x06B0, MOB_FORCE_CHASE = 0x06E3, MOB_FREEZE_EFFECT = 0x0715,
            SUMMON_ATTACK_ACTIVE = 0x067C, TOWN_PORTAL_CREATED = 0x0755, TOWN_PORTAL_REMOVED = 0x0756,
            REACTOR_ENTER_FIELD = 0x0767, RUNE_STONE_APPEAR = 0x07F9, SET_SOUL_EFFECT = 0x0400,
            CUR_NODE_EVENT_END = 0x032A, SET_QUEST_CLEAR = 0x0314, SHOP_CRYSTAL = 0x0964;
    /** 265 town portal packets carry no skill id; Mystic Door is the only 265 creator (推定). */
    static final int DEFAULT_DOOR_SKILL = 2311002;

    static void field(Tms282LegacyPackets.Builder b) {
        Charset cs = b.charset();
        b.on(OutHeader.MOB_AFFECTED, p -> same(p, MOB_AFFECTED, 15))                  // 3EA75D0 i32,i32,u16,u8,i32
                .on(OutHeader.MOB_FORCE_CHASE, p -> same(p, MOB_FORCE_CHASE, 5))            // 3F3E7B0 i32,u8 (op 推定)
                .on(OutHeader.SUMMONED_SUMMON_ATTACK_ACTIVE, p -> same(p, SUMMON_ATTACK_ACTIVE, 9)) // 45AB780 -> 45288C0
                .on(OutHeader.SUMMONED_SKILL, Tms282DropFixTranslators::summonedSkill)
                .on(OutHeader.SUMMONED_ACTION_CHANGE, p -> same(p, SUMMONED_ACTION_CHANGE, 9))
                .on(OutHeader.SUMMONED_BEHOLDER_REVENGE_ATTACK, Tms282DropFixTranslators::beholderRevenge)
                .on(OutHeader.SUMMONED_SPECIAL_ASSIST_SKILL, p -> same(p, SUMMONED_SPECIAL_ASSIST, 13))
                .on(OutHeader.SUMMONED_STATE_CHANGED, Tms282DropFixTranslators::summonedStateChanged)
                .on(OutHeader.SUMMONED_EFFECT, Tms282DropFixTranslators::summonedEffect)
                .on(OutHeader.SUMMONED_SUMMON_USE_SPECIFIED_SKILL, Tms282DropFixTranslators::summonedUseSkill)
                .on(OutHeader.SUMMONED_UPGRADE_STAGE, Tms282DropFixTranslators::summonedUpgradeStage)
                .on(OutHeader.SUMMONED_LOTUS_POWER, p -> same(p, SUMMONED_LOTUS_POWER, 8))
                .on(OutHeader.INHUMAN_SPEED_RESULT, p -> same(p, INHUMAN_SPEED_RESULT, 9))
                .on(OutHeader.SHOOT_OBJECT_EXPLODE_RESULT, p -> counted(p, SHOOT_OBJECT_EXPLODE_RESULT, 4, 4, 0))
                .on(OutHeader.REMOTE_RW_MULTI_CHARGE_CANCEL_REQUEST, Tms282DropFixTranslators::rwMultiChargeCancel)
                .on(OutHeader.DEL_WRECKAGE, Tms282DropFixTranslators::delWreckage)
                .on(OutHeader.MOB_SPECIAL_SELECTED_EFFECT_BY_SKILL, Tms282DropFixTranslators::mobSelectedEffect)
                .on(OutHeader.CREATE_ARK_FORCE_ATOM, Tms282DropFixTranslators::arkForceAtom)
                .on(OutHeader.SUMMONED_ASSIST_ATTACK_REQUEST, p -> same(p, SUMMONED_ASSIST_ATTACK, 16))
                .on(OutHeader.SUMMONED_REPOSITION_SUMMON, p -> same(p, SUMMONED_REPOSITION, 24))
                .on(OutHeader.TOWN_PORTAL_REMOVED, p -> same(p, TOWN_PORTAL_REMOVED, 5))    // 461EBA0 u8,i32
                .on(OutHeader.REACTOR_ENTER_FIELD, p -> reactorEnter(p, cs))               // 4242430 -> 4243AF0
                .on(OutHeader.RUNE_STONE_APPEAR, p -> same(p, RUNE_STONE_APPEAR, 25))      // 42A3420 -> 4286630
                .on(OutHeader.SET_SOUL_EFFECT, p -> same(p, SET_SOUL_EFFECT, 6))           // 5AA8318 -> 5662A60
                .on(OutHeader.CUR_NODE_EVENT_END, p -> same(p, CUR_NODE_EVENT_END, 1))     // 3B38A10 u8
                .on(OutHeader.SET_QUEST_CLEAR, p -> same(p, SET_QUEST_CLEAR, 0))           // 3C895A0 (no body)
                .on(OutHeader.SHOP_CRYSTAL, Tms282DropFixTranslators::shopCrystal);         // DB9FF0
        if (enabled("townPortal")) {
            b.on(OutHeader.TOWN_PORTAL_CREATED, Tms282DropFixTranslators::townPortalCreated); // 461D8E0 (skill 推定)
        }
        // MOB_FREEZE_EFFECT -> LP0715 reads CLEAN but the client answers CP053B (no 282 route): off by default.
        if ("true".equalsIgnoreCase(System.getProperty("tms282.dropfix.mobFreeze.enabled", "false"))) {
            b.on(OutHeader.MOB_FREEZE_EFFECT, p -> same(p, MOB_FREEZE_EFFECT, 8));
        }
    }

    /** 265 u8, i32 char, pos town, pos field -> 282 u8, i32 char, i32 skill, pos (461D8E0). */
    static Optional<OutPacket> townPortalCreated(OutPacket legacy) {
        Tms282LegacyPackets.Body body = Tms282LegacyPackets.Body.of(legacy);
        int noAnim = body.u8();
        int chr = body.i32();
        body.u16();
        body.u16();
        int x = body.u16();
        int y = body.u16();
        body.end();
        OutPacket out = Tms282LoginPacketSupport.packet(TOWN_PORTAL_CREATED);
        out.encodeByte(noAnim);
        out.encodeInt(chr);
        out.encodeInt(DEFAULT_DOOR_SKILL);
        out.encodeShort(x);
        out.encodeShort(y);
        return Optional.of(out);
    }

    /** u8, i32 obj, i32 template, u8 state, i16 x, i16 y, u8 flip, str name: the name re-encoded. */
    static Optional<OutPacket> reactorEnter(OutPacket legacy, Charset cs) {
        Tms282LegacyPackets.Body body = Tms282LegacyPackets.Body.of(legacy);
        OutPacket out = Tms282LoginPacketSupport.packet(REACTOR_ENTER_FIELD);
        out.encodeByte(body.u8());
        out.encodeInt(body.i32());
        out.encodeInt(body.i32());
        out.encodeByte(body.u8());
        out.encodeShort(body.u16());
        out.encodeShort(body.u16());
        out.encodeByte(body.u8());
        Tms282LoginPacketSupport.str(out, body.str(), cs);
        body.end();
        return Optional.of(out);
    }

    /**
     * 265 u8 on; on: 7 x i32, then two periods (FT, FT, i32 n, n x (i32, i64, i32)). 282 DB9FF0: the same u8 and
     * 29-byte head, then i32 nPeriods, the periods, then one more period: sent as nPeriods = 1 (traced CLEAN).
     */
    static Optional<OutPacket> shopCrystal(OutPacket legacy) {
        byte[] b = Tms282LegacyPackets.Body.of(legacy).rest();
        OutPacket out = Tms282LoginPacketSupport.packet(SHOP_CRYSTAL);
        if (b.length == 1 && b[0] == 0) {
            out.encodeByte(0);
            return Optional.of(out);
        }
        if (b.length < 29 || b[0] != 1) {
            return Optional.empty();
        }
        Tms282LegacyPackets.Body body = Tms282LegacyPackets.Body.of(legacy);
        body.bytes(29);
        for (int period = 0; period < 2; period++) {
            body.i64();
            body.i64();
            int n = body.i32();
            if (n < 0 || n > 1000) {
                return Optional.empty();
            }
            body.bytes(n * 16);
        }
        body.end();
        out.encodeArr(java.util.Arrays.copyOf(b, 29));
        out.encodeInt(1);
        out.encodeArr(java.util.Arrays.copyOfRange(b, 29, b.length));
        return Optional.of(out);
    }
}
