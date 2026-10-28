package net.swordie.ms.connection.protocol.codec;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.life.npc.NpcMessageType;
import net.swordie.ms.life.npc.NpcScriptInfo;

/**
 * Codec family of the NPC / script dialog packet (265 OutHeader.SCRIPT_MESSAGE; TMS 282.3 LP0960). The 265
 * ScriptManagerImpl builds every dialog through {@link #active()}, so a profile only has to register its own
 * encoder: GMS 265 registers none and falls back to {@link Gms265ScriptMessages} (the unchanged
 * {@code ScriptMan.scriptMessage}); TMS 282.3 registers {@code Tms282ScriptPackets}.
 */
public interface ScriptMessageEncoder {

    /**
     * The dialog {@code type} of {@code nsi} (text, speaker, parameters) as one packet.
     *
     * @throws UnsupportedOperationException when this profile has no layout for {@code type}
     */
    OutPacket scriptMessage(NpcScriptInfo nsi, NpcMessageType type);

    /** The active profile's encoder (or its version's fallback), else the GMS 265 one. */
    static ScriptMessageEncoder active() {
        return ProfileEncoders.find(ScriptMessageEncoder.class).orElse(Gms265ScriptMessages.INSTANCE);
    }
}
