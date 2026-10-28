package net.swordie.ms.client.character.skills;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.swordie.ms.connection.OutPacket;

public class SecondAtomCollision {

    public int secondAtomObjId;
    public int mobObjId;
    public Int2IntMap map = new Int2IntOpenHashMap();

    public SecondAtomCollision() {}

    public void encode(OutPacket outPacket) {
        outPacket.encodeInt(secondAtomObjId);
        outPacket.encodeInt(mobObjId);
        outPacket.encodeInt(map.size());
        for (var entry : map.int2IntEntrySet()) {
            outPacket.encodeInt(entry.getIntKey());
            outPacket.encodeInt(entry.getIntValue());
        }
    }
}
