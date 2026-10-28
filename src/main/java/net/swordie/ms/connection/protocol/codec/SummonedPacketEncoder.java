package net.swordie.ms.connection.protocol.codec;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.life.Summon;

/**
 * CSummonedPool packets whose body differs between profiles. The 265 builder
 * {@link net.swordie.ms.connection.packet.Summoned#created} asks the active profile for this family first and keeps
 * its own 265 body as the fallback, so every 265 flow that shows a summon (job handlers, {@code Field.spawnSummon},
 * {@code Summon.broadcastSpawnPacket}) sends the profile's packet without changing its call site.
 * <p>
 * Codec family of {@link net.swordie.ms.connection.protocol.ProfileCodecs}; the GMS 265 profile registers none.
 * Packets whose body is the same in both profiles (SUMMONED_REMOVED) are translated after the fact instead
 * ({@link LegacyPacketTranslator}).
 */
public interface SummonedPacketEncoder {

    /** SUMMONED_CREATED: shows {@code summon} (owned by {@code owner}) to the users of the field. */
    OutPacket created(Summon summon, Char owner);
}
