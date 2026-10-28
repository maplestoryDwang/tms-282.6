package net.swordie.ms.client.social.Guild;

import net.swordie.ms.connection.Encodable;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.enums.social.Guild.GuildBBSType;

import java.util.List;

/**
 * @author Sjonnie
 * Created on 8/12/2018.
 */
public class GuildBBSPacket implements Encodable {

    private GuildBBSType type;
    private BBSRecord record;
    private int totalSize;
    private List<BBSRecord> records;

    public static GuildBBSPacket response_PagesLoad(BBSRecord notice, int totalSize, List<BBSRecord> pageRecords) {
        GuildBBSPacket gbp = new GuildBBSPacket();

        gbp.type = GuildBBSType.Response_PagesLoad;
        gbp.record = notice;
        gbp.totalSize = totalSize;
        gbp.records = pageRecords;

        return gbp;
    }

    public static GuildBBSPacket response_RecordLoad(BBSRecord record) {
        GuildBBSPacket gbp = new GuildBBSPacket();

        gbp.type = GuildBBSType.Response_RecordLoad;
        gbp.record = record;

        return gbp;
    }

    @Override
    public void encode(OutPacket outPacket) {
        outPacket.encodeByte(type.getVal());
        switch (type) {
            case Response_PagesLoad:
                outPacket.encodeByte(record != null);
                if (record != null) {
                    record.encodeForPagesLoad(outPacket);
                }
                outPacket.encodeInt(totalSize);
                if (totalSize > 0) {
                    outPacket.encodeInt(records.size());
                    for (BBSRecord record : records) {
                        record.encodeForPagesLoad(outPacket);
                    }
                }
                break;
            case Response_RecordLoad:
                record.encodeForRecordLoad(outPacket);
                break;
        }
    }
}
