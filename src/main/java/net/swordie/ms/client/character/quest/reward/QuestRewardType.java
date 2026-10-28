package net.swordie.ms.client.character.quest.reward;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * Created on 3/8/2018.
 */
public enum QuestRewardType {
    EXP(0),
    ITEM(1),
    MONEY(2),
    POP(3),
    BUFFITEM(4),
    TRAIT(5);

    private final byte val;

    QuestRewardType(int val) {
        this.val = (byte) val;
    }

    public static QuestRewardType getQPRTByObj(Object o) {
        return o instanceof QuestExpReward ? EXP :
                o instanceof QuestItemReward ? ITEM :
                        o instanceof QuestMoneyReward ? MONEY :
                                o instanceof QuestPopReward ? POP :
                                        o instanceof QuestBuffItemReward ? BUFFITEM :
                                                o instanceof QuestTraitReward ? TRAIT : null;
    }

    public static QuestRewardType getQPRTByVal(byte val) {
        return Arrays.stream(QuestRewardType.values())
                .filter(qprt -> qprt.getVal() == val).findFirst().orElse(null);
    }

    public byte getVal() {
        return val;
    }

    public QuestReward load(DataInputStream dis) throws IOException {
        switch (this) {
            case EXP:
                return (QuestReward) new QuestExpReward().load(dis);
            case ITEM:
                return (QuestReward) new QuestItemReward().load(dis);
            case MONEY:
                return (QuestReward) new QuestMoneyReward().load(dis);
            case POP:
                return (QuestReward) new QuestPopReward().load(dis);
            case BUFFITEM:
                return (QuestReward) new QuestBuffItemReward().load(dis);
            case TRAIT:
                return (QuestReward) new QuestTraitReward().load(dis);
            default:
                return null;
        }
    }
}
