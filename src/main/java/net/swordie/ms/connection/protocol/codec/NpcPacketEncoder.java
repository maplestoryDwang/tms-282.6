package net.swordie.ms.connection.protocol.codec;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.life.npc.Npc;

/**
 * CNpcPool packets whose body differs between profiles. The 265 builders
 * {@link net.swordie.ms.connection.packet.NpcPool#npcEnterField}, {@code npcLeaveField} and
 * {@code npcChangeController} ask the active profile for this family first and keep their 265 body as the
 * fallback, so the 265 spawn and controller logic ({@code Npc.broadcastSpawnPacket}, {@code Field.removeChar}...)
 * sends the profile's packets unchanged.
 * <p>
 * Codec family of {@link net.swordie.ms.connection.protocol.ProfileCodecs}; the GMS 265 profile registers none.
 */
public interface NpcPacketEncoder {

    OutPacket enterField(Npc npc);

    OutPacket leaveField(Npc npc);

    /**
     * @param controller whether the receiving user controls the NPC (sends its moves)
     * @param remove     true when the NPC leaves: only the object id follows
     */
    OutPacket changeController(Npc npc, boolean controller, boolean remove);
}
