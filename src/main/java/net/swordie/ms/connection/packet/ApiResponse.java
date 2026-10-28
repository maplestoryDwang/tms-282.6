package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.api.ApiOutHeader;
import net.swordie.ms.enums.AccountCreateResult;
import net.swordie.ms.enums.ApiTokenResultType;

public class ApiResponse {

    public static OutPacket tokenRequestResult(ApiTokenResultType atrt, String token) {
        OutPacket outPacket = new OutPacket(ApiOutHeader.REQUEST_TOKEN_RESULT);

        outPacket.encodeByte(atrt.getVal());
        if (atrt == ApiTokenResultType.Success) {
            outPacket.encodeString(token);
        }

        return outPacket;
    }

    public static OutPacket tokenRequestResult(ApiTokenResultType atrt, int token) {
        OutPacket outPacket = new OutPacket(ApiOutHeader.REQUEST_TOKEN_RESULT_X64);

        outPacket.encodeByte(atrt.getVal());
        if (atrt == ApiTokenResultType.Success) {
            outPacket.encodeInt(token);
        }

        return outPacket;
    }

    public static OutPacket createAccountResult(AccountCreateResult acr) {
        OutPacket outPacket = new OutPacket(ApiOutHeader.CREATE_ACCOUNT_RESULT);

        outPacket.encodeByte(acr.ordinal());

        return outPacket;
    }
}
