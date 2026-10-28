package net.swordie.ms.enums.social.Guild;

import java.util.Arrays;

/**
 * @author Sjonnie
 * Created on 8/12/2018.
 */
public enum GuildBBSType {
    Request_RecordCreate(1),
    Request_RecordDelete(2),
    Request_PagesLoad(3),
    Request_RecordLoad(4),
    Request_ReplyCreate(5),
    Request_ReplyDelete(6),
    Response_PagesLoad(7),
    Response_RecordLoad(8),
    Response_SomethingElse(9),
    ;

    private final int val;

    GuildBBSType(int val) {
        this.val = val;
    }

    public static GuildBBSType getByValue(byte val) {
        return Arrays.stream(values()).filter(gbt -> gbt.getVal() == val).findAny().orElse(null);
    }

    public int getVal() {
        return val;
    }
}
