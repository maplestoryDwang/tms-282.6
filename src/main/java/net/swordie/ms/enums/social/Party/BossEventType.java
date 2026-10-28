package net.swordie.ms.enums.social.Party;

import java.util.Arrays;

public enum BossEventType {

    BALROG(0),
    ZAKUM(1),
    URSUS(2),
    MAGNUS(3),
    HILLA(4),
    ROOT_ABYSS(5),
    VON_LEON(6),
    HORNTAIL(7),
    ARKARIUM(8),
    PINK_BEAN(9),
    CYGNUS(10),
    LOTUS(11),
    DAMIEN(12),
    GOLLUX(13),
    RANMARU(14),
    PRINCESS_NO(15),
    LUCID(16),
    PAPULATUS(17),
    GLOOM(18),
    VERUS_HILLA(19),
    DARKNELL(20),
    WILL(21),
    BLACK_MAGE(22),
    JULIETA(23),
    OMNI_CLN(24),
    ;

    private final byte val;

    BossEventType(int val) {
        this.val = (byte) val;
    }

    public static BossEventType getByVal(byte type) {
        return Arrays.stream(values()).filter(i -> i.getVal() == type).findFirst().orElse(null);
    }

    public byte getVal() {
        return val;
    }
}
