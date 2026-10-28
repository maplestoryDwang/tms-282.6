package net.swordie.ms.connection.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.swordie.ms.Server;
import net.swordie.ms.ServerConfig;
import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.packet.WvsContext;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.handlers.*;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.handlers.item.*;
import net.swordie.ms.handlers.life.*;
import net.swordie.ms.handlers.social.*;
import net.swordie.ms.handlers.ui.*;
import net.swordie.ms.handlers.user.*;
import net.swordie.ms.console.PacketDebugLog;
import net.swordie.ms.console.PacketLogFormat;
import net.swordie.ms.console.ConsoleDebugMode;
import net.swordie.ms.util.DataPrinter;

import java.io.IOException;

import static net.swordie.ms.connection.netty.NettyClient.CLIENT_KEY;

public class ChannelHandler extends SimpleChannelInboundHandler<InPacket> {

    public ChannelHandler(boolean autoRelease) {
        super(autoRelease);
    }

    @Override
    public void channelActive(ChannelHandlerContext chc) throws Exception {
        super.channelActive(chc);
        Server.get().getChannels().add(chc.channel());
        Client c = (Client) chc.channel().attr(CLIENT_KEY).get();
        if (c != null) {
            System.out.println("[連接] 客戶端 " + c.getIP() + " 已連接");
        }
        System.out.println(STR."Online(s): \{Server.get().getChannels().size()}");
    }

    @Override
    public void channelInactive(ChannelHandlerContext chc) {
        Client c = (Client) chc.channel().attr(CLIENT_KEY).get();
        if (c != null) {
            System.out.println("[斷線] 客戶端 " + c.getIP() + " 已斷開");
            c.handleChannelInactive();
        }
        Server.get().getChannels().remove(chc.channel());
        System.out.println(STR."Online(s): \{Server.get().getChannels().size()}");
    }

    @Override
    public void channelRead0(ChannelHandlerContext chc, InPacket inPacket) {
        final var c = (Client) chc.channel().attr(CLIENT_KEY).get();
        final var chr = c != null ? c.getChr() : null;
        final var op = inPacket.decodeShort();
        final var header = Protocol.active().opcodes().inHeader(op);
        if (header == null) {
            if (ServerConfig.PACKET_LOG) {
                handleUnknown(c, chr, inPacket, op);
            }
            return;
        }
        if (ServerConfig.PACKET_LOG && !InHeader.isSpamHeader(header)) {
            try {
                debug(c, chr, inPacket, header, op);
            } catch (Exception ignored) {}
        }
        final PacketHandler handler = PacketProcessor.getHandler(header);
        if (handler == null) {
            if (c != null && c.isExiting()) {
                PacketProcessor.acknowledgeExitingUnknownPacket(c, chr, inPacket);
            }
            return;
        }
        try {
            handler.handle(c, chr, inPacket);
        } catch (Exception e) {
            final var inPacketEx = new InPacket(inPacket.getData().clone());
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, String.format("Packet %s [%d] got Exception: %s", header, inPacketEx.decodeShort(), inPacketEx));
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
            if (c != null) c.write(WvsContext.exclRequest());
        }
    }

    /** Opcode without a header in the active profile (so never a spam header). */
    private void handleUnknown(Client c, Char chr, InPacket inPacket, short op) {
        String who = chr != null ? chr.getName() : (c != null ? c.getIP() : "?");
        String ip = c != null ? c.getIP() : "";
        byte[] remain = copyUnread(inPacket);
        PacketDebugLog.logUnknownRecv(ip, op, remain, who);
        if (chr != null) {
            DataPrinter.send(DataPrinter.PACKET_LOG + chr.getName() + ".txt",
                    PacketLogFormat.formatUnknownRecv(ip, op, remain, who), false);
        }
    }

    private void debug(Client c, Char chr, InPacket inPacket, InHeader header, short op) {
        if (c == null) {
            return;
        }
        String ip = c.getIP();
        byte[] remain = copyUnread(inPacket);
        String charName = chr != null ? chr.getName() : null;
        PacketDebugLog.logRecv(ip, header, op, remain, charName);
    }

    private static byte[] copyUnread(InPacket inPacket) {
        int unread = inPacket.getUnreadAmount();
        if (unread <= 0) {
            return new byte[0];
        }
        byte[] all = inPacket.getData();
        int start = all.length - unread;
        if (start < 0 || start >= all.length) {
            return new byte[0];
        }
        return java.util.Arrays.copyOfRange(all, start, all.length);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        Client c = (Client) ctx.channel().attr(CLIENT_KEY).get();
        if (c == null) {
            return;
        }
        if (cause instanceof IOException) {
            ConsoleDebugMode.mirrorLine("斷線", c.getIP() + " 強制關閉連接");
            System.out.printf("%s 強制關閉了遊戲連接。%n", c.getIP());
        } else {
            ConsoleDebugMode.mirrorLine("異常", c.getIP() + " " + cause.getClass().getSimpleName() + ": " + cause.getMessage());
            System.out.printf("%s 發生異常：%n", c.getIP());
            cause.printStackTrace();
        }
    }
}
