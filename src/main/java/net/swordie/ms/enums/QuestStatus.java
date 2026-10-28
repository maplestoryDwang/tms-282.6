package net.swordie.ms.enums;

/**
 * Created on 3/2/2018.
 */
public enum QuestStatus {
    NotStarted(0),
    Started(1),
    Completed(2);

    private final byte val;

    QuestStatus(int val) {
        this.val = (byte) val;
    }

    public static QuestStatus getValByNum(int num) {
        for (QuestStatus at : QuestStatus.values()) {
            if (at.getVal() == num) {
                return at;
            }
        }
        return null;
    }

    public byte getVal() {
        return val;
    }
}
