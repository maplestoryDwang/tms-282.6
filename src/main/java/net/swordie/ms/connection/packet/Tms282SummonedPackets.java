package net.swordie.ms.connection.packet;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.avatar.AvatarLook;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.protocol.codec.SummonedPacketEncoder;
import net.swordie.ms.constants.SkillConstants;
import net.swordie.ms.life.Summon;
import net.swordie.ms.util.Position;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * TMS 282.3 CSummonedPool packets (skill2-trace/README.md). The pool's dispatcher CSummonedPool::OnPacket 45AB780
 * covers LP0664..LP068C (265 SUMMONED_CREATED 1588 .. + 48; jump table 45AC1B4), reads {@code i32 ownerId,
 * i32 summonedId} for every op, then:
 * <p>
 * LP0664 SUMMONED_CREATED -> 45AD1E0 ({@code i32 skillId, i32 charLevel, i32 slv}) -> CSummoned::Init 44D6530:
 * <pre>
 * i16 x, i16 y, u8 moveAction, i16 foothold, u8 moveAbility, u8 assistType, u8 enterType, i32 mobId,
 * u8 flyMob, u8 beforeFirstAttack, i32 templateId, i32 bulletId,
 * u8 hasAvatar [AvatarLook 901AF0]
 * [Rock 'n Shock (11A2990: 35111002, 35141007): u8 state; state 1: 3 x (i16 x, i16 y)]
 * [118E770 set (DELAY_SKILLS): i32 actionDelay, i32 movementDelay]
 * i32 summonTerm, u8 attackActive, i32 left, i32 0, i32 (162101012 ? 400 : 0)
 * u8 stateUsing [i32 count, i32 state]
 * i32 n, n x i32 linked summon skill
 * i32 0, i32 0, i32 -1, u8 0, u8 0, i32 0
 * [skill tail 44D6DA0: ONE_INT_TAIL i32; THREE_INT_TAIL 3 x i32; INT_BYTE_TAIL i32 + u8]
 * </pre>
 * Differences from the 265 body (Summoned.created): 282 has no {@code jaguarActive} byte before the term, reads the
 * tesla positions only in state 1, reads Kanna's Kishin shorts / Suborbital byte / 400001065 int nowhere, and its
 * skill-specific tails are the client's lists above (not 265's). The subclass hook {@code [vtbl+0x48](pkt)} at
 * 44D6E0B is not modelled: summon classes that read more there are unknown (not a live-client test).
 * All fixtures are traced CLEAN by skill2-trace/skill2_trace.py (entry 45AB780 + 44D6530).
 */
public final class Tms282SummonedPackets implements SummonedPacketEncoder {
    /** 265 CreateShootObj (16) is a no-op in the 282 update switch (45115F6 on +0x448 -> 4512550); 282 fires shoot
     * objects (CP02EB, e.g. Altar of Annihilation 4512308) for assist type 18. Other values are unchanged. */
    static int assistType282(net.swordie.ms.life.Summon summon) {
        if (summon.getAssistType() == null) return 0;
        return summon.getAssistType() == net.swordie.ms.enums.AssistType.CreateShootObj ? 18 : summon.getAssistType().getVal();
    }

    public static final int SUMMONED_CREATED = 0x0664;
    public static final int SUMMONED_REMOVED = 0x066F;
    /** 265 SUMMONED_CREATED..SUMMONED_* + 48 = 282 (anchors: CREATED 0x664, REMOVED 0x66F, jump table 45AC1B4). */
    public static final int DELTA_FROM_265 = 48;

    /** 11A2990(35111002, skill): Rock 'n Shock and its HEXA form. */
    static final Set<Integer> TESLA_SKILLS = Set.of(35111002, 35141007);
    /** 118E770: summons that carry an action delay and a movement delay. */
    static final Set<Integer> DELAY_SKILLS = Set.of(14111024, 14121054, 14121055, 14121056, 131001017, 131002017,
            131003017, 400001071, 400031007, 400031008, 400041095, 500001085, 500061004, 500061046, 500061047,
            500061048);
    /** 44D6DA0 tail: one i32 (44D7000 / 44D7013 / 44D703C / 44D704F / 44D708F / 44D70C8). */
    static final Set<Integer> ONE_INT_TAIL = Set.of(400011065, 2341006, 151111001, 400021047, 152141501, 152141505,
            164141056, 500001005, 500001024, 500001026, 500001028, 500001063, 500001064, 500001065, 500001066,
            500001067);
    /** 44D6F6A: three i32. */
    static final Set<Integer> THREE_INT_TAIL = Set.of(14141017, 14121003, 14121012, 14141020);
    /** 44D70A2: one i32 and one u8. */
    static final Set<Integer> INT_BYTE_TAIL = Set.of(500001068, 500001070);
    /** Illium's Deploy Crystal: the only summon class with an extra create read (summon-hook-audit/SUMMON-HOOK.md). */
    static final int ILLIUM_DEPLOY_CRYSTAL = 152101000;

    private final Charset charset;

    public Tms282SummonedPackets(Charset charset) {
        this.charset = Objects.requireNonNull(charset, "charset");
    }

    @Override
    public OutPacket created(Summon summon, Char owner) {
        Objects.requireNonNull(summon, "summon");
        int skill = summon.getSkillID();
        OutPacket out = Tms282LoginPacketSupport.packet(SUMMONED_CREATED);
        out.encodeInt(summon.getOwnerId());
        out.encodeInt(summon.getObjectId());
        out.encodeInt(skill);
        out.encodeInt(summon.getCharLevel());
        out.encodeInt(summon.getSlv());
        Position pos = summon.getPosition() != null ? summon.getPosition() : new Position();
        out.encodePosition(pos);
        out.encodeByte(summon.getMoveAction());
        out.encodeShort(summon.getCurFoothold());
        out.encodeByte(summon.getMoveAbility() == null ? 0 : summon.getMoveAbility().getVal());
        out.encodeByte(assistType282(summon));
        out.encodeByte(summon.getEnterType() == null ? 0 : summon.getEnterType().getVal());
        out.encodeInt(summon.getMobID());
        out.encodeByte(summon.isFlyMob());
        out.encodeByte(summon.isBeforeFirstAttack());
        out.encodeInt(summon.getTemplateId());
        out.encodeInt(summon.getBulletID());
        AvatarLook look = summon.getAvatarLook();
        out.encodeByte(look != null);
        if (look != null) {
            String name = owner != null ? owner.getName() : "";
            Tms282AvatarEncoder.encodeLook(out, look, name == null ? "" : name, charset);
        }
        if (TESLA_SKILLS.contains(skill)) {
            int state = summon.getTeslaCoilState();
            out.encodeByte(state);
            if (state == 1) {
                List<Position> coils = summon.getTeslaCoilPositions();
                for (int i = 0; i < 3; i++) {
                    out.encodePosition(i < coils.size() ? coils.get(i) : pos);
                }
            }
        }
        if (DELAY_SKILLS.contains(skill)) {
            out.encodeInt(summon.getActionDelay());
            out.encodeInt(summon.getMovementDelay());
        }
        out.encodeInt(summon.getSummonTerm());
        out.encodeByte(summon.isAttackActive());
        out.encodeInt(owner != null && owner.isLeft() ? 1 : 0);
        out.encodeInt(0);
        out.encodeInt(skill == 162101012 ? 400 : 0);
        boolean stateUsing = SkillConstants.isStateUsingSummon(skill);
        out.encodeByte(stateUsing);
        if (stateUsing) {
            out.encodeInt(summon.getCount());
            out.encodeInt(summon.getState());
        }
        List<Integer> linked = summon.getLinkedSummonSkillIds();
        out.encodeInt(linked.size());
        for (int id : linked) {
            out.encodeInt(id);
        }
        out.encodeInt(0);
        out.encodeInt(0);
        out.encodeInt(-1);
        out.encodeByte(0);
        out.encodeByte(0);
        out.encodeInt(0);
        if (THREE_INT_TAIL.contains(skill)) {
            out.encodeInt(0);
            out.encodeInt(0);
            out.encodeInt(0);
        } else if (ONE_INT_TAIL.contains(skill)) {
            out.encodeInt(skill == 400011065 ? summon.getSummonTerm() : 0);
        } else if (INT_BYTE_TAIL.contains(skill)) {
            out.encodeInt(0);
            out.encodeByte(0);
        }
        if (skill == ILLIUM_DEPLOY_CRYSTAL) {
            // the crystal summon class (45821A0) overrides the create hook vtbl+0x48 (4582400): i32 n, n x (i32 crystal
            // skill index, i32 usable). Without it the client threw (Decode4, 0 remain) - skill2-trace/summon-hook-audit.
            java.util.Map<Integer, Boolean> crystal = owner != null
                    && owner.getJobHandler() instanceof net.swordie.ms.client.jobs.flora.Illium illium
                    && illium.crystalSkillMap != null ? illium.crystalSkillMap : java.util.Map.of();
            out.encodeInt(crystal.size());
            for (var e : new java.util.TreeMap<>(crystal).entrySet()) {
                out.encodeInt(e.getKey());
                out.encodeInt(Boolean.TRUE.equals(e.getValue()) ? 1 : 0);
            }
        }
        return out;
    }
}
