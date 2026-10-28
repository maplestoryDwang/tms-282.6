package net.swordie.ms.connection.protocol.codec;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.packet.ScriptMan;
import net.swordie.ms.life.npc.NpcMessageType;
import net.swordie.ms.life.npc.NpcScriptInfo;

/** The GMS 265 dialog packet: exactly {@link ScriptMan#scriptMessage} (the 265 builder is not changed). */
public enum Gms265ScriptMessages implements ScriptMessageEncoder {
    INSTANCE;

    @Override
    public OutPacket scriptMessage(NpcScriptInfo nsi, NpcMessageType type) {
        return ScriptMan.scriptMessage(nsi, type);
    }
}
