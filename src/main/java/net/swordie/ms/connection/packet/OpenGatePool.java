package net.swordie.ms.connection.packet;

import net.swordie.ms.client.jobs.resistance.OpenGate;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.handlers.header.OutHeader;

public class OpenGatePool {

    public static OutPacket openGateCreated(OpenGate openGate) {
        OutPacket outPacket = new OutPacket(OutHeader.OPEN_GATE_CREATED);

        outPacket.encodeByte(1); // Animation
        outPacket.encodeInt(openGate.getChr().getId()); // Character Id
        outPacket.encodePosition(openGate.getPosition()); // Position
        outPacket.encodeByte(openGate.getGateId()); // Gate Id
        outPacket.encodeInt(openGate.getParty() != null ? openGate.getParty().getId() : 0); // Party Id

        return outPacket;
    }

    public static OutPacket openGateClose(OpenGate openGate) {
        OutPacket outPacket = new OutPacket(OutHeader.OPEN_GATE_CLOSE);

        outPacket.encodeByte(openGate.getGateId()); // Gate Id

        return outPacket;
    }

    public static OutPacket openGateRemoved(OpenGate openGate) {
        OutPacket outPacket = new OutPacket(OutHeader.OPEN_GATE_REMOVED);

        outPacket.encodeByte(1); // Animation
        outPacket.encodeInt(openGate.getChr().getId()); // Character Id
        outPacket.encodeByte(openGate.getGateId()); // Gate Id

        return outPacket;
    }
}
