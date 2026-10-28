package net.swordie.ms.connection.protocol.codec;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.util.Position;
import net.swordie.ms.world.field.Field;

/**
 * CStage SET_FIELD of a field change (characterData = false: portal, warp, script warp). The 265 builder
 * {@link net.swordie.ms.connection.packet.Stage#setField} asks the active profile for this family when
 * {@code characterData} is false and keeps its 265 body as the fallback, so {@code Char.warp} (the only caller)
 * sends the profile's packet at the 265 point of the warp. A profile whose first entry is sent by its
 * {@link net.swordie.ms.world.migration.FieldEntryPolicy} (TMS 282.3: {@code Tms282FieldEntry}) leaves the
 * characterData = true call to the 265 body, which its fail-closed OutboundGuard drops.
 * <p>
 * Codec family of {@link net.swordie.ms.connection.protocol.ProfileCodecs}; the GMS 265 profile registers none.
 */
public interface StagePacketEncoder {

    /**
     * @param channelId the server channel as {@code Char.warp} passes it (1-based; 265 writes {@code channelId - 1})
     * @param portal    target portal index in {@code field}
     * @param position  explicit spawn position, or null to spawn at the portal
     */
    OutPacket changeField(Char chr, Field field, int channelId, int portal, Position position, boolean usingBuffProtector);
}
