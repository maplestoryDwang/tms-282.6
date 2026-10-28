package net.swordie.ms.connection.netty;

import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.InPacket;

@FunctionalInterface
public interface PacketHandler {
    void handle(Client c, Char chr, InPacket in);
}