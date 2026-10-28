package net.swordie.ms.client.character.skills.matrix;

import net.swordie.ms.connection.Encodable;
import net.swordie.ms.connection.OutPacket;

public class NodeEnhance implements Encodable {

    public int pos;
    public int expGained;
    public int oldSlv;
    public int newSlv;


    @Override
    public void encode(OutPacket outPacket) {
        outPacket.encodeInt(pos);
        outPacket.encodeInt(expGained);
        outPacket.encodeInt(oldSlv);
        outPacket.encodeInt(newSlv);
    }
}
