package net.swordie.ms.connection.packet;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.handlers.header.OutHeader;

import static net.swordie.ms.client.character.skills.BuffFavorite.MAX_ROW;

public class BuffFavoritePacket {

    public static OutPacket encode(Int2ObjectMap<Int2IntMap> favorites) {
        OutPacket outPacket = new OutPacket(OutHeader.BUFF_FAVORITES_RESULT);

        outPacket.encodeInt(MAX_ROW * 10); // 100
        for (int preset = 0; preset < MAX_ROW; preset++) {
            Int2IntMap m = favorites.get(preset);
            for (int index = 0; index < MAX_ROW; index++) {
                int skillId = (m == null) ? 0 : m.getOrDefault(index, 0);
                if (skillId == 0) {
                    outPacket.encodeByte(0);
                    outPacket.encodeByte(0);
                    outPacket.encodeInt(0);
                } else {
                    outPacket.encodeByte(preset);
                    outPacket.encodeByte(index);
                    outPacket.encodeInt(skillId);
                }
            }
        }

        return outPacket;
    }
}
