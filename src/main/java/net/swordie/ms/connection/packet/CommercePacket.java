package net.swordie.ms.connection.packet;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.CommerceItemRecord;
import net.swordie.ms.client.character.CommerceRegionRecord;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.enums.CommerceType;
import net.swordie.ms.handlers.header.OutHeader;

import java.util.Set;

public class CommercePacket {

    public static OutPacket update(Char chr) {
        OutPacket outPacket = new OutPacket(OutHeader.COMMERCE_RESULT);

        outPacket.encodeByte(CommerceType.Res_MainUpdate.getVal());
        chr.getCommerceRecord().encode(outPacket);

        return outPacket;
    }

    public static OutPacket enter(boolean hasParty, Set<CommerceItemRecord> commerceItemRecords) {
        OutPacket outPacket = new OutPacket(OutHeader.COMMERCE_RESULT);

        outPacket.encodeByte(CommerceType.Res_Enter.getVal());
        outPacket.encodeByte(hasParty);
        outPacket.encodeByte(!commerceItemRecords.isEmpty());
        if (!commerceItemRecords.isEmpty()) {
            for (CommerceItemRecord cir : commerceItemRecords) {
                cir.encode(outPacket);
            }
        }

        return outPacket;
    }

    public static OutPacket regionReveal(boolean success, int level) {
        OutPacket outPacket = new OutPacket(OutHeader.COMMERCE_RESULT);

        outPacket.encodeByte(CommerceType.Res_MapReveal.getVal());
        outPacket.encodeByte(success);
        outPacket.encodeByte(level);

        return outPacket;
    }

    public static OutPacket regionUpdate(CommerceRegionRecord crc) {
        OutPacket outPacket = new OutPacket(OutHeader.COMMERCE_RESULT);

        outPacket.encodeByte(CommerceType.Res_RegionUpdate.getVal());
        outPacket.encodeByte(crc.getRegionType());
        outPacket.encodeInt(crc.getVoyagesCompleted());

        return outPacket;
    }
}
