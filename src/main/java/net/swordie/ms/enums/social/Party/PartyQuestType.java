package net.swordie.ms.enums.social.Party;

import java.util.Arrays;

public enum PartyQuestType {

    MOON_BUNNY(0),
    FIRST_TIME_TOGETHER(1),
    DIMENSION_INVASION(3),
    //EVOLUTION_SYSTEM(4),
    NETT_PYRAMID(5),
    ESCAPE(6),
    TANGYOON_COOKING(7),
    HUNGRY_MUTO(8),
    XERXES_CHRYSE(9),
    LORD_PIRATE(9),
    DRAGON_RIDER(10),
    KENTA_IN_DANGER(11),
    ROMEO(12),
    JULIET(13),
    ALIEN_VISITOR(14),
    ;

    private final byte val;

    PartyQuestType(int val) {
        this.val = (byte) val;
    }

    public static PartyQuestType getByVal(byte type) {
        return Arrays.stream(values()).filter(i -> i.getVal() == type).findFirst().orElse(null);
    }

    public byte getVal() {
        return val;
    }
}
