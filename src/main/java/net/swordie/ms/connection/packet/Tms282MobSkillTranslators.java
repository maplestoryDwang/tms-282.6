package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.handlers.header.OutHeader;

import java.util.Optional;

/**
 * Mob skill packets the 265 MobHandler / MobSkill send (live 2026-09-27: about 10 000 of each dropped by the guard),
 * sent under their 282 CMobPool opcodes with the unchanged 265 body. Both were traced CLEAN through CMobPool::OnPacket
 * 3FA78E0 with research/tms2823/mob-trace/mob_trace.py --fake-mob (all bytes read, no exception):
 * <ul>
 *     <li>MOB_SET_AFTER_ATTACK -> LP069A (3FA7AE0): {@code i32 mobId, i16 afterAttack, i32 attackCount,
 *     i32 serverAction, u8 left} (in-game map 證實, +45).</li>
 *     <li>MOB_ATTACK_BLOCK -> LP06D3 (3FAB7F0 reads the mob id, 3EAC320): {@code i32 mobId, i32 n, n x i32 skillId},
 *     n = 0 and n = 2 traced.</li>
 * </ul>
 * MOB_NEXT_TARGET_FROM_SVR ({@code i32 mobId, i32 charId}) stays dropped: the map's guess 0x70F reads only the mob id
 * and 0x710 reads three i32 (both NOT CLEAN).
 */
public final class Tms282MobSkillTranslators implements Tms282LegacyPackets.Contributor {
    public static final Tms282MobSkillTranslators INSTANCE = new Tms282MobSkillTranslators();
    public static final int MOB_SET_AFTER_ATTACK = 0x069A;
    public static final int MOB_ATTACK_BLOCK = 0x06D3;
    public static final int MOB_SPECIAL_EFFECT_BY_SKILL = 0x06B4;

    private Tms282MobSkillTranslators() {
    }

    @Override
    public void contribute(Tms282LegacyPackets.Builder builder) {
        builder.on(OutHeader.MOB_SET_AFTER_ATTACK, p -> copy(p, MOB_SET_AFTER_ATTACK, 15))
                .on(OutHeader.MOB_ATTACK_BLOCK, Tms282MobSkillTranslators::attackBlock)
                // Pool reads mobId;3EA83C0 reads skillId,casterId,u16. The last value is skill-specific (Cryo counter).
                .on(OutHeader.MOB_SPECIAL_EFFECT_BY_SKILL, Tms282MobSkillTranslators::specialEffect);
    }

    static Optional<OutPacket> copy(OutPacket legacy, int op, int length) {
        byte[] body = Tms282LegacyPackets.Body.of(legacy).rest();
        if (body.length != length) {
            return Optional.empty();
        }
        OutPacket out = Tms282LoginPacketSupport.packet(op);
        out.encodeArr(body);
        return Optional.of(out);
    }

    static Optional<OutPacket> attackBlock(OutPacket legacy) {
        Tms282LegacyPackets.Body body = Tms282LegacyPackets.Body.of(legacy);
        int mobId = body.i32();
        int n = body.i32();
        if (n < 0 || n > 64 || body.remaining() != n * 4) {
            return Optional.empty();
        }
        OutPacket out = Tms282LoginPacketSupport.packet(MOB_ATTACK_BLOCK);
        out.encodeInt(mobId);
        out.encodeInt(n);
        for (int i = 0; i < n; i++) {
            out.encodeInt(body.i32());
        }
        body.end();
        return Optional.of(out);
    }

    static Optional<OutPacket> specialEffect(OutPacket legacy) {
        var body = Tms282LegacyPackets.Body.of(legacy);
        int mobId = body.i32(), skillId = body.i32(), casterId = body.i32(), value = body.u16();
        body.end();
        // Bound this header to current, verified producer variants; other original skills are not sufficient evidence.
        int expected = switch (skillId) {
            case 1241005, 2341001, 36141008, 2240006 -> 0;
            case 3110001 -> 9;
            case 3210001 -> 1;
            default -> -1;
        };
        if (mobId <= 0 || casterId <= 0 || value != expected
                || !net.swordie.ms.loaders.Tms282SkillRequirements.hasOriginalSkill(skillId))
            return Optional.empty();
        OutPacket out = Tms282LoginPacketSupport.packet(MOB_SPECIAL_EFFECT_BY_SKILL);
        out.encodeInt(mobId); out.encodeInt(skillId); out.encodeInt(casterId); out.encodeShort(value);
        return Optional.of(out);
    }
}
