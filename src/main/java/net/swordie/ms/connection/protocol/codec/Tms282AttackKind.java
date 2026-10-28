package net.swordie.ms.connection.protocol.codec;

import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.handlers.header.OutHeader;

import java.util.Optional;

/**
 * The five TMS 282.3 user attack requests (ingame-cp-map-2823.md 3.2 / 4.4) and how their static prefix differs.
 * Evidence: CP010D melee and CP010E shoot [verified]; CP010F magic, CP0110 body, CP0111 area dot [presumed: +2 from
 * 265, same header form as magic]. Every send site jumps into the client's Themida VM after the mob loop, so the
 * part after the last mob is not known statically (see {@link Tms282AttackTail}).
 *
 * @param op                282 wire opcode
 * @param request           the 265 header the request stands for ({@code AttackInfo.inHeader})
 * @param remote            the 265 remote attack header ({@code AttackInfo.attackHeader}); none for area dot
 * @param boxAttackByte     shoot: one byte right after the leading two u32 (265 boxAttack)
 * @param addAttackProc     melee and shoot: a byte after slv
 * @param magicMobExtra     magic: each mob block has u8 hpPerc + u16 magicInfo where the others have one u16
 */
public enum Tms282AttackKind {
    MELEE(0x010D, InHeader.USER_MELEE_ATTACK, OutHeader.REMOTE_MELEE_ATTACK, false, true, false),
    SHOOT(0x010E, InHeader.USER_SHOOT_ATTACK, OutHeader.REMOTE_SHOOT_ATTACK, true, true, false),
    MAGIC(0x010F, InHeader.USER_MAGIC_ATTACK, OutHeader.REMOTE_MAGIC_ATTACK, false, false, true),
    BODY(0x0110, InHeader.USER_BODY_ATTACK, OutHeader.REMOTE_BODY_ATTACK, false, false, false),
    AREA_DOT(0x0111, InHeader.USER_AREA_DOT_ATTACK, null, false, false, false);

    private final int op;
    private final InHeader request;
    private final OutHeader remote;
    private final boolean boxAttackByte;
    private final boolean addAttackProc;
    private final boolean magicMobExtra;

    Tms282AttackKind(int op, InHeader request, OutHeader remote, boolean boxAttackByte, boolean addAttackProc,
                     boolean magicMobExtra) {
        this.op = op;
        this.request = request;
        this.remote = remote;
        this.boxAttackByte = boxAttackByte;
        this.addAttackProc = addAttackProc;
        this.magicMobExtra = magicMobExtra;
    }

    public int op() {
        return op;
    }

    public InHeader request() {
        return request;
    }

    /** The 265 remote attack header, or null (area dot has none, as in 265). */
    public OutHeader remote() {
        return remote;
    }

    public boolean boxAttackByte() {
        return boxAttackByte;
    }

    public boolean addAttackProc() {
        return addAttackProc;
    }

    public boolean magicMobExtra() {
        return magicMobExtra;
    }

    /** Bytes of the per-mob block before the damages: I id, 5B, I template, B, 4S, S (magic: B S), I, I, B, I (282). */
    public int mobDamageOffset() {
        return 4 + 5 + 4 + 1 + 8 + (magicMobExtra ? 3 : 2) + 4 + 4 + 1 + 4;
    }

    public static Optional<Tms282AttackKind> ofOpcode(int op) {
        for (Tms282AttackKind k : values()) {
            if (k.op == op) {
                return Optional.of(k);
            }
        }
        return Optional.empty();
    }

    public static Optional<Tms282AttackKind> ofHeader(InHeader header) {
        for (Tms282AttackKind k : values()) {
            if (k.request == header) {
                return Optional.of(k);
            }
        }
        return Optional.empty();
    }
}
