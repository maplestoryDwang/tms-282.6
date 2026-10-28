package net.swordie.ms.enums.social.Friend;

import java.util.Arrays;

/**
 * Created on 4/1/2018.
 */
public enum FriendFlag {
    Friend(0),
    FriendRequest(1),
    FriendOffline(2),
    FriendOnline(3),
    MobileOnline(4),
    MobileOffline(5),
    AccountFriendRequest(6),
    AccountFriendOnline(7),
    AccountFriendOffline(8),
    ;

    private final int val;

    FriendFlag(int val) {
        this.val = val;
    }

    public static FriendFlag getTypeByVal(byte val) {
        return Arrays.stream(values()).filter(grt -> grt.getVal() == val).findAny().orElse(null);
    }

    public int getVal() {
        return val;
    }
}
