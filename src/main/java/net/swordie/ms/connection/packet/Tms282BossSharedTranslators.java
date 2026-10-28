package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.handlers.header.OutHeader;

import java.nio.charset.Charset;
import java.util.Optional;

/**
 * Boss-shared packets (research/tms2823/lucid-re-0930/shared, 2026-09-30, static + emulated, not live-tested):
 * 265 packets the fail-closed guard dropped for Lucid / Will / Suu / Magnus and most modern bosses, sent under their
 * 282 opcodes. Every output layout below was traced CLEAN (all bytes read, no exception) against the 282.3 client
 * handler (282.4 layouts identical) with the unicorn emulators; fixtures in shared/out, traces in shared/expected.
 * <ul>
 *     <li>MOB_NEXT_TARGET_FROM_SVR (265 1757) -> LP070B (CMobPool 3FAB7F0 reads the mob id, 3F52660 reads
 *     {@code i32 charId} into CMob+0x12FC, clears the cached target at +0x468; 3F18783 resolves +0x12FC through
 *     CUserPool). Body = 265 {@code i32 mobId, i32 charId}.</li>
 *     <li>DEATH_COUNT_INFO (1239) -> LP050E (CUserLocal inline 57890BA, {@code i32 count} -> DeathCountInfo UI
 *     48E3F50) and INDIVIDUAL_DEATH_COUNT_INFO (1240) -> LP050F (58AE4A0, {@code i32 charId, i32 count}; the
 *     client compares charId with the local character). 282 LP050F can also read one AUX byte (optional, not sent).</li>
 *     <li>CREATE_OBSTACLE (791) -> LP0332 (CField 38A70A0, "Effect/BasicEff.img/ObtacleAtomCreate/%d") and
 *     CLEAR_OBSTACLE (793) -> LP0334 (38A9880, no body). The 282 read order equals 265 FieldPacket.createObtacle:
 *     {@code i32 0, i32 n, u8 type, [type 1: i32,u8,i32 x4], [type 4: i32 x5], n x (u8 1, i32 x7, u8, rect16,
 *     i32 x8, [type 5: i32 x5])}; the loop is count-driven (u8 0 = skip slot), no terminator.</li>
 *     <li>MOB_SKILL_DELAY (1679) -> <b>LP06BD</b> (not 0x06BE: 3EA9A70 -> 36230A0 on CMob+0xAC8). 282 reads
 *     {@code i32 mobId, i32 skillAfter, i32 skillId, i32 slv, i32 interval, i32 extra, i32 n, n x rect16} = the
 *     265 byte layout (265 field 4 "option 900 for 230" is the 282 per-rect interval, 265 field 5 is only used
 *     by the 282 client for skill 213 levels 11/12). When the MobSkill level has a {@code selectSkill} list
 *     (level+0x1D0) 282 reads {@code i32, i32 levelIdx} after every rect; no skill in data/wz282 Skill.wz/MobSkill
 *     has one, so the 265 body is sent unchanged. skillId 0 (the 265 "no skill" form without a count) is declined:
 *     the client stops after the MobSkill lookup fails.</li>
 *     <li>WEATHER_EFFECT_NOTICE (267) -> LP010C (CWvsContext 5E2B470: {@code str msg, i32 type, i32 durationMs,
 *     u8 forced}; weatherEffectMessage/Type/Duration/Forced). The UTF-8 265 string is re-encoded in the 282
 *     charset.</li>
 *     <li>MOB_SPEAKING (1678) -> LP06BB (3EA99D0 -> 3EE0240, speakInfo indexes the template's speak list) and
 *     MOB_MESSAGING (1678) -> LP06BC (3EA9A20 -> 3EE1390); both {@code i32 mobId, i32, i32}.</li>
 *     <li>MOB_LASER_CONTROL (1716) -> LP06E1 (3EAE940: {@code i32 angle, i32 autoOffset, u8 up}).</li>
 *     <li>MOB_FORCED_ACTION (265 UnkValue) -> LP06CE (3EAF0C0: {@code i32 action} -> 3EAF0F0(mob, action), no
 *     further packet access).</li>
 * </ul>
 * Not translated (see the README): MOB_FORCED_SKILL_ACTION (candidate LP06CD reads three i32, semantics of the
 * last two unproven), TOSSED_BY_MOB_SKILL (the map's 0x0402 = 60E7E00 is a list removal, not the toss handler).
 * Malformed 265 bodies are declined (dropped by the guard as before). {@code -Dtms282.bossShared.enabled=false}
 * turns the whole contributor off.
 */
public final class Tms282BossSharedTranslators implements Tms282LegacyPackets.Contributor {
    public static final Tms282BossSharedTranslators INSTANCE = new Tms282BossSharedTranslators();

    public static final int MOB_NEXT_TARGET_FROM_SVR = 0x070B;
    public static final int DEATH_COUNT_INFO = 0x050E;
    public static final int INDIVIDUAL_DEATH_COUNT_INFO = 0x050F;
    public static final int CREATE_OBSTACLE = 0x0332;
    public static final int CLEAR_OBSTACLE = 0x0334;
    public static final int MOB_SKILL_DELAY = 0x06BD;
    public static final int WEATHER_EFFECT_NOTICE = 0x010C;
    public static final int MOB_SPEAKING = 0x06BB;
    public static final int MOB_MESSAGING = 0x06BC;
    public static final int MOB_LASER_CONTROL = 0x06E1;
    public static final int MOB_FORCED_ACTION = 0x06CE;

    /** ObtacleAtomCreateType values with a header block / per-atom block in the 282 reader. */
    static final int OBSTACLE_IN_ROW = 1;
    static final int OBSTACLE_RADIAL = 4;
    static final int OBSTACLE_DIAGONAL = 5;
    /** One ObtacleAtomInfo: i32 x7, u8, rect (4 x i32), i32 x8. */
    static final int OBSTACLE_ATOM_BYTES = 7 * 4 + 1 + 16 + 8 * 4;
    static final int OBSTACLE_MAX = 512;
    static final int SKILL_DELAY_MAX_RECTS = 64;

    private Tms282BossSharedTranslators() {
    }

    public static boolean enabled() {
        return !"false".equalsIgnoreCase(System.getProperty("tms282.bossShared.enabled", "true"));
    }

    @Override
    public void contribute(Tms282LegacyPackets.Builder builder) {
        if (!enabled()) {
            return;
        }
        Charset charset = builder.charset();
        builder.on(OutHeader.MOB_NEXT_TARGET_FROM_SVR, p -> copy(p, MOB_NEXT_TARGET_FROM_SVR, 8))
                .on(OutHeader.DEATH_COUNT_INFO, p -> copy(p, DEATH_COUNT_INFO, 4))
                .on(OutHeader.INDIVIDUAL_DEATH_COUNT_INFO, p -> copy(p, INDIVIDUAL_DEATH_COUNT_INFO, 8))
                .on(OutHeader.CREATE_OBSTACLE, Tms282BossSharedTranslators::createObstacle)
                .on(OutHeader.CLEAR_OBSTACLE, p -> copy(p, CLEAR_OBSTACLE, 0))
                .on(OutHeader.MOB_SKILL_DELAY, Tms282BossSharedTranslators::skillDelay)
                .on(OutHeader.WEATHER_EFFECT_NOTICE, p -> weatherNotice(p, charset))
                .on(OutHeader.MOB_SPEAKING, p -> copy(p, MOB_SPEAKING, 12))
                .on(OutHeader.MOB_MESSAGING, p -> copy(p, MOB_MESSAGING, 12))
                .on(OutHeader.MOB_LASER_CONTROL, p -> copy(p, MOB_LASER_CONTROL, 13))
                .on(OutHeader.MOB_FORCED_ACTION, p -> copy(p, MOB_FORCED_ACTION, 8));
    }

    /** The 265 body unchanged under {@code op}, when it has exactly {@code length} bytes. */
    static Optional<OutPacket> copy(OutPacket legacy, int op, int length) {
        byte[] body = Tms282LegacyPackets.Body.of(legacy).rest();
        if (body.length != length) {
            return Optional.empty();
        }
        OutPacket out = Tms282LoginPacketSupport.packet(op);
        if (length > 0) {
            out.encodeArr(body);
        }
        return Optional.of(out);
    }

    /** Validates the 265 FieldPacket.createObtacle layout against the 282 reader, then sends it unchanged. */
    static Optional<OutPacket> createObstacle(OutPacket legacy) {
        Tms282LegacyPackets.Body body = Tms282LegacyPackets.Body.of(legacy);
        byte[] all = Tms282LegacyPackets.Body.of(legacy).rest();
        body.i32(); // unused key
        int n = body.i32();
        int type = body.u8();
        if (n < 0 || n > OBSTACLE_MAX) {
            return Optional.empty();
        }
        if (type == OBSTACLE_IN_ROW) {
            body.bytes(4 + 1 + 4 * 4);
        } else if (type == OBSTACLE_RADIAL) {
            body.bytes(5 * 4);
        }
        for (int i = 0; i < n; i++) {
            if (body.u8() == 0) {
                continue; // the client skips the slot (38A84CA)
            }
            body.bytes(OBSTACLE_ATOM_BYTES);
            if (type == OBSTACLE_DIAGONAL) {
                body.bytes(5 * 4);
            }
        }
        body.end();
        OutPacket out = Tms282LoginPacketSupport.packet(CREATE_OBSTACLE);
        out.encodeArr(all);
        return Optional.of(out);
    }

    /** 265 MobPool.setSkillDelay (all three forms) -> LP06BD; the byte layout is the 282 one (see class doc). */
    static Optional<OutPacket> skillDelay(OutPacket legacy) {
        Tms282LegacyPackets.Body body = Tms282LegacyPackets.Body.of(legacy);
        byte[] all = Tms282LegacyPackets.Body.of(legacy).rest();
        body.i32(); // mob id
        body.i32(); // skillAfter
        int skillId = body.i32();
        body.i32(); // slv
        body.i32(); // 265 option (900 for 230) = 282 per-rect interval
        body.i32(); // 265 sequenceDelay = 282 extra (skill 213 lv 11/12 only)
        if (skillId <= 0) {
            return Optional.empty();
        }
        int n = body.i32();
        if (n < 0 || n > SKILL_DELAY_MAX_RECTS || body.remaining() != n * 16) {
            return Optional.empty();
        }
        OutPacket out = Tms282LoginPacketSupport.packet(MOB_SKILL_DELAY);
        out.encodeArr(all);
        return Optional.of(out);
    }

    static Optional<OutPacket> weatherNotice(OutPacket legacy, Charset charset) {
        Tms282LegacyPackets.Body body = Tms282LegacyPackets.Body.of(legacy);
        String msg = body.str();
        int type = body.i32();
        int duration = body.i32();
        int forced = body.u8();
        body.end();
        OutPacket out = Tms282LoginPacketSupport.packet(WEATHER_EFFECT_NOTICE);
        Tms282LoginPacketSupport.str(out, msg, charset);
        out.encodeInt(type);
        out.encodeInt(duration);
        out.encodeByte(forced);
        return Optional.of(out);
    }
}
