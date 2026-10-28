package net.swordie.ms.connection.protocol.codec;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.OutPacket;

/**
 * REMOTE_AVATAR_MODIFIED: tells the other users of a field that {@code chr} looks different (equip / unequip,
 * hair or face change, speed, carried item). The 265 builder
 * {@link net.swordie.ms.connection.packet.UserRemote#avatarModified} asks the active profile for this family first
 * and keeps its own 265 body as the fallback, so every 265 flow that changes the look ({@code Char.equip},
 * {@code Char.unequip}, scripts) sends the profile's packet without changing its call site.
 * <p>
 * Codec family of {@link net.swordie.ms.connection.protocol.ProfileCodecs}; the GMS 265 profile registers none.
 * The mask bits are those of {@link net.swordie.ms.enums.AvatarModifiedMask}.
 */
public interface AvatarModifiedEncoder {

    OutPacket avatarModified(Char chr, byte mask, byte carryItemEffect);
}
