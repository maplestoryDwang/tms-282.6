package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.handlers.header.OutHeader;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Skill-event phase (2026-09-29, research/tms2823/skill-events): 265 skill packets that the fail-closed guard
 * dropped, in their 282 form. Each output was traced against the 282.3 client handler (282.4 layouts identical)
 * with the emulators (skill-events/out/*.bin -> expected/*.tsv, all CLEAN; tools/Tms282SkillEventCheck):
 * <ul>
 *     <li>SKILL_COOLTIME_SET_M (265, milliseconds) -> LP0662 SKILL_COOLTIME_SET: {@code i32 n, n x (i32 skillId,
 *     i32 seconds)}; the client (inline 578831A) sets {@code seconds x 1000} ms (5FE8E30) or clears on 0
 *     (5FE91C0). Milliseconds are rounded up (1..1000 ms -> 1 s, so the client is never ready before the
 *     server); 0 or a negative value clears. 282 has no millisecond cooldown packet.</li>
 *     <li>FINAL_ATTACK_REQUEST -> LP0524 (CUserLocal 57A3630): {@code i32 time, i32 success, i32 skillId,
 *     i32 finalSkillId, i32 weaponType, i32 n, n x i32 mobId} = the 265 body.</li>
 *     <li>MOB_BIND_REGISTER -> LP06D0 (CMobPool::OnMobPacket 3FA78E0 -> 3EABE90 -> EB2E40): {@code i32 mobObjId,
 *     u8 0, i32 n, n x (i32 charId, i32 value, i32 skillId, i32 unk, i32 remainingMs)} = the 265 body. A body
 *     with the u8 set is declined (265 never writes it; the other 282 branch is untraced).</li>
 *     <li>TOWN_PORTAL -> LP00DF (CWvsContext 5EFB780): {@code i32 townField, i32 field, i32 skillId, i16 x,
 *     i16 y}. When either field id is 999999999 (the 265 removal form) the client reads only the two ids
 *     (5EFB7B7 / 5EFB7BF), so the removal is sent as those 8 bytes.</li>
 * </ul>
 * The static SAME_AS_265 rows of LP0524 / LP06D0 / LP00DF do not give the profile an opcode, so these are
 * translated like {@link Tms282InteractionTranslators}. Malformed 265 bodies are declined (dropped as before).
 */
public final class Tms282SkillEventTranslators implements Tms282LegacyPackets.Contributor {
    public static final Tms282SkillEventTranslators INSTANCE = new Tms282SkillEventTranslators();

    public static final int SKILL_COOLTIME_SET = Tms282SkillPackets.SKILL_COOLTIME_SET;
    public static final int FINAL_ATTACK_REQUEST = 0x0524;
    public static final int MOB_BIND_REGISTER = 0x06D0;
    public static final int TOWN_PORTAL = 0x00DF;
    /** 265 {@code TownPortal.removeTownPortalInTownField} and the client's "no portal" field id. */
    public static final int NO_FIELD = 999_999_999;

    private Tms282SkillEventTranslators() {
    }

    @Override
    public void contribute(Tms282LegacyPackets.Builder builder) {
        builder.on(OutHeader.SKILL_COOLTIME_SET_M, Tms282SkillEventTranslators::cooltimeSetMillis)
                .on(OutHeader.FINAL_ATTACK_REQUEST, Tms282SkillEventTranslators::finalAttack)
                .on(OutHeader.MOB_BIND_REGISTER, Tms282SkillEventTranslators::mobBindRegister)
                .on(OutHeader.TOWN_PORTAL, Tms282SkillEventTranslators::townPortal)
                .on(OutHeader.USER_BONUS_ATTACK_REQUEST, Tms282SkillEventTranslators::bonusAttack);
    }

    /** Native5A2F650: proven Hero/Ren producers only;265 has one extra common I before target pairs. */
    static Optional<OutPacket> bonusAttack(OutPacket legacy) {
        var body = Tms282LegacyPackets.Body.of(legacy);
        int skill = body.i32();
        boolean renTargets = skill == 400011148;
        // 33100002 Wild Hunter 支援 (5A2FCAD isAssist branch): plain target records, no tail; live CP03CB/attack untested
        boolean targets = renTargets || skill == 33100002;
        if (!targets && skill != 400011151 && skill != 400011152
                && (skill < 400011074 || skill > 400011076)) return Optional.empty();
        int count = body.i32(), onUser = body.u8();
        if (count < 0 || (!targets && count != 0) || onUser != (count == 0 ? 1 : 0)
                || body.i32() != 0 || body.i32() != 0 || body.i32() != 0) {
            return Optional.empty();
        }
        int tail = renTargets ? 4 : 0;
        if (body.remaining() < tail || (body.remaining() - tail) % 8 != 0
                || count != (body.remaining() - tail) / 8) return Optional.empty();
        OutPacket out = Tms282LoginPacketSupport.packet(0x057E);
        out.encodeInt(skill);
        out.encodeInt(count);
        out.encodeByte(onUser);
        out.encodeInt(0);
        out.encodeInt(0);
        for (int i = 0; i < count; i++) {
            int objectId = body.i32();
            if (body.i32() != 0) return Optional.empty(); // Every real Ren target record's second I is zero.
            out.encodeInt(objectId); out.encodeInt(0);
        }
        if (renTargets) out.encodeInt(body.i32()); //5A2FC59..65: actual triggering attack skill, including basic0.
        body.end();
        return Optional.of(out);
    }

    /** Milliseconds -> whole seconds, rounded up; {@code <= 0} -> 0 (clears). */
    public static int seconds(long millis) {
        if (millis <= 0) {
            return 0;
        }
        return (int) Math.min(Integer.MAX_VALUE, millis / 1000 + (millis % 1000 == 0 ? 0 : 1));
    }

    static Optional<OutPacket> cooltimeSetMillis(OutPacket legacy) {
        Tms282LegacyPackets.Body body = Tms282LegacyPackets.Body.of(legacy);
        int n = body.i32();
        if (n < 0 || n > body.remaining() / 8) {
            return Optional.empty();
        }
        Map<Integer, Integer> secondsBySkill = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            int skillId = body.i32();
            secondsBySkill.merge(skillId, seconds(body.i32()), Math::max);
        }
        body.end();
        return Optional.of(Tms282SkillPackets.skillCooltimeSet(secondsBySkill));
    }

    static Optional<OutPacket> finalAttack(OutPacket legacy) {
        Tms282LegacyPackets.Body body = Tms282LegacyPackets.Body.of(legacy);
        byte[] head = body.bytes(20); // time, success, skillId, finalSkillId, weaponType
        int n = body.i32();
        if (n < 0 || n != body.remaining() / 4 || body.remaining() % 4 != 0) {
            return Optional.empty();
        }
        OutPacket out = Tms282LoginPacketSupport.packet(FINAL_ATTACK_REQUEST);
        out.encodeArr(head);
        out.encodeInt(n);
        out.encodeArr(body.rest());
        return Optional.of(out);
    }

    static Optional<OutPacket> mobBindRegister(OutPacket legacy) {
        Tms282LegacyPackets.Body body = Tms282LegacyPackets.Body.of(legacy);
        int mobObjId = body.i32();
        int flag = body.u8();
        int n = body.i32();
        if (flag != 0 || n < 0 || n != body.remaining() / 20 || body.remaining() % 20 != 0) {
            return Optional.empty();
        }
        OutPacket out = Tms282LoginPacketSupport.packet(MOB_BIND_REGISTER);
        out.encodeInt(mobObjId);
        out.encodeByte(0);
        out.encodeInt(n);
        out.encodeArr(body.rest());
        return Optional.of(out);
    }

    static Optional<OutPacket> townPortal(OutPacket legacy) {
        Tms282LegacyPackets.Body body = Tms282LegacyPackets.Body.of(legacy);
        int townField = body.i32();
        int field = body.i32();
        int skillId = body.i32();
        short x = (short) body.u16();
        short y = (short) body.u16();
        body.end();
        OutPacket out = Tms282LoginPacketSupport.packet(TOWN_PORTAL);
        out.encodeInt(townField);
        out.encodeInt(field);
        if (townField != NO_FIELD && field != NO_FIELD) {
            out.encodeInt(skillId);
            out.encodeShort(x);
            out.encodeShort(y);
        }
        return Optional.of(out);
    }
}
