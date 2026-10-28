package net.swordie.ms.enums.reward;

import java.util.Arrays;

public enum RewardSystemType {
    Response_Rewards(9),

    /**
     * Encode Int, Int
     * You have received the Maple Points.\r\n( %d maple point )
     **/
    Response_Received_MaplePoint_Success(11),

    /**
     * Encode Int (1 = Visual Remove in Reward List)
     * You have received the Game item.
     **/
    Response_Received_GameItem_Success(12),

    /**
     * Encode Int
     * You have received the Cash Item.
     **/
    Response_Received_CashItem_Success(13),

    /**
     * Encode Int, Int
     * You have received the Mesos.\r\n( %d meso )
     **/
    Response_Received_Mesos_Success(14),

    /**
     * Encode Int, Int
     * You have received the EXP.\r\n( %d exp )
     **/
    Response_Received_Exp_Success(15),

    /**
     * Failed to receive the Maple Point.
     **/
    Response_Received_MaplePoint_Fail(20),

    /**
     * Encode Byte
     * v33 = 102: Your inventory is full.
     * v33 = 103: You already have the item
     * other: Failed to receive the Game item.
     **/
    Response_Received_GameItem_Fail(21),

    /**
     * Encode Byte
     * v40 = 32: Your inventory is full.
     * v40 = 34: You already have the same item.
     * other: Failed to receive the Cash item.
     **/
    Response_Received_CashItem_Fail(22),

    /**
     * Failed to receive the Mesos.
     **/
    Response_Received_Mesos_Fail(23),

    /**
     * Failed to receive the EXP.
     **/
    Response_Received_Exp_Fail(24),

    /**
     * Failed to receive reward. Please try again later.
     **/
    Response_UnexpectedError(-1),

    ;

    private final byte val;

    RewardSystemType(int val) {
        this.val = (byte) val;
    }

    public static RewardSystemType getByVal(byte val) {
        return Arrays.stream(values()).filter(rt -> rt.getVal() == val).findAny().orElse(null);
    }

    public byte getVal() {
        return val;
    }
}
