package net.swordie.ms.enums;

import net.swordie.ms.enums.reward.RewardItemType;

import java.util.Arrays;

public enum AccountType {
    Player(0),
    Tester(1), //1 << 5
    Intern(2), //1 << 3
    GameMaster(3), //1 << 4
    Admin(4); //1 << 4

    private final int val;

    AccountType(int val) {
        this.val = val;
    }

    public static AccountType getByVal(int val) {
        return Arrays.stream(values()).filter(t -> t.getVal() == val).findAny().orElse(null);
    }

    public int getVal() {
        return val;
    }


}
