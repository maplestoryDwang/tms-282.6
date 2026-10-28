package net.swordie.ms.client.character.reward;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.enums.reward.RewardSystemType;

public class RewardResult {

    private final RewardSystemType type;
    private RewardSystem rewardSystem;
    private int arg1;
    private int arg2;
    private byte arg3;

    public RewardResult(RewardSystemType type) {
        this.type = type;
    }

    public static RewardResult response_Rewards(RewardSystem rewardSystem) {
        RewardResult rewardResult = new RewardResult(RewardSystemType.Response_Rewards);
        rewardResult.rewardSystem = rewardSystem;
        return rewardResult;
    }

    public static RewardResult response_Received_MaplePoint_Success(int arg1, int arg2) {
        RewardResult rewardResult = new RewardResult(RewardSystemType.Response_Received_MaplePoint_Success);
        rewardResult.arg1 = arg1;
        rewardResult.arg2 = arg2;
        return rewardResult;
    }

    public static RewardResult response_Received_GameItem_Success(int arg1) {
        RewardResult rewardResult = new RewardResult(RewardSystemType.Response_Received_GameItem_Success);
        rewardResult.arg1 = arg1;
        return rewardResult;
    }

    public static RewardResult response_Received_CashItem_Success(int arg1) {
        RewardResult rewardResult = new RewardResult(RewardSystemType.Response_Received_CashItem_Success);
        rewardResult.arg1 = arg1;
        return rewardResult;
    }

    public static RewardResult response_Received_Mesos_Success(int arg1, int arg2) {
        RewardResult rewardResult = new RewardResult(RewardSystemType.Response_Received_Mesos_Success);
        rewardResult.arg1 = arg1;
        rewardResult.arg2 = arg2;
        return rewardResult;
    }

    public static RewardResult response_Received_Exp_Success(int arg1, int arg2) {
        RewardResult rewardResult = new RewardResult(RewardSystemType.Response_Received_Exp_Success);
        rewardResult.arg1 = arg1;
        rewardResult.arg2 = arg2;
        return rewardResult;
    }

    public static RewardResult response_Received_GameItem_Fail(byte arg3) {
        RewardResult rewardResult = new RewardResult(RewardSystemType.Response_Received_GameItem_Fail);
        rewardResult.arg3 = arg3;
        return rewardResult;
    }

    public static RewardResult response_Received_CashItem_Fail(byte arg3) {
        RewardResult rewardResult = new RewardResult(RewardSystemType.Response_Received_CashItem_Fail);
        rewardResult.arg3 = arg3;
        return rewardResult;
    }

    public void encode(OutPacket outPacket) {
        outPacket.encodeByte(type.getVal());
        switch (type) {
            case Response_Rewards:
                rewardSystem.encode(outPacket);
                break;
            case Response_Received_MaplePoint_Success:
            case Response_Received_GameItem_Success:
            case Response_Received_CashItem_Success:
                outPacket.encodeInt(arg1);
                outPacket.encodeInt(arg2);
                break;
            case Response_Received_Mesos_Success:
            case Response_Received_Exp_Success:
                outPacket.encodeInt(arg1); //isDelete
                outPacket.encodeLong(arg2); //value
                outPacket.encodeInt(arg3);
                break;
            case Response_Received_GameItem_Fail:
            case Response_Received_CashItem_Fail:
                outPacket.encodeByte(arg3);
                break;
        }
    }
}
