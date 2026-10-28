package net.swordie.ms.handlers;

import net.swordie.ms.client.Client;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.console.config.ServerRuntimeConfig;
import net.swordie.ms.handlers.header.OutHeader;
import net.swordie.ms.handlers.login.tms282.ChannelAddressPolicy;

public class ClientSocket {

    public static OutPacket migrateCommand(boolean succeed, short port) {
        return migrateCommand(succeed, port, ServerRuntimeConfig.serverHostBytes());
    }

    /** As {@link #migrateCommand(boolean, short)} to {@code client}: 282 gets the ChannelAddressPolicy address (launcherapi7e). */
    public static OutPacket migrateCommand(Client client, boolean succeed, short port) {
        return migrateCommand(succeed, port, ChannelAddressPolicy.server().legacyAddress(client));
    }

    private static OutPacket migrateCommand(boolean succeed, short port, byte[] host) {
        OutPacket outPacket = new OutPacket(OutHeader.MIGRATE_COMMAND);

        outPacket.encodeByte(succeed); // will disconnect if false
        if (succeed) {
            outPacket.encodeArr(host); // 遊戲伺服器 IP（server.host；282 見 ChannelAddressPolicy）
            outPacket.encodeShort(port);
            outPacket.encodeInt(0); // ??
        }

        return outPacket;
    }

    public static OutPacket auctionHouseOut(short port) {
        return auctionHouseOut(port, ServerRuntimeConfig.serverHostBytes());
    }

    /** As {@link #auctionHouseOut(short)} to {@code client}: 282 gets the ChannelAddressPolicy address (launcherapi7e). */
    public static OutPacket auctionHouseOut(Client client, short port) {
        return auctionHouseOut(port, ChannelAddressPolicy.server().legacyAddress(client));
    }

    private static OutPacket auctionHouseOut(short port, byte[] host) {
        OutPacket outPacket = new OutPacket(OutHeader.MIGRATE_COMMAND);

        outPacket.encodeByte(true);
        outPacket.encodeArr(host); // 拍賣行伺服器 IP（server.host；282 見 ChannelAddressPolicy）
        outPacket.encodeShort(port);
        outPacket.encodeInt(0); // ??

        return outPacket;
    }

    public static OutPacket opcodeEncryption(byte[] buf) {
        OutPacket outPacket = new OutPacket(OutHeader.OPCODE_ENCRYPTION);

        outPacket.encodeInt(buf.length);
        outPacket.encodeArr(buf);

        return outPacket;
    }
}
