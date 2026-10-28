package net.swordie.ms.connection.api;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.swordie.ms.client.Client;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.handlers.ApiRequestHandler;

import java.io.IOException;

import static net.swordie.ms.connection.netty.NettyClient.CLIENT_KEY;

public class ApiHandler extends SimpleChannelInboundHandler<InPacket> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, InPacket inPacket) throws Exception {
        Client c = (Client) ctx.channel().attr(CLIENT_KEY).get();
        short op = inPacket.decodeShort();
        ApiInHeader opHeader = ApiInHeader.getInHeaderByOp(op);
        if (opHeader == null) {
            System.out.println("未知 API 請求 " + op);
            return;
        }
        switch (opHeader) {
            case REQUEST_TOKEN:
                break;
            case REQUEST_TOKEN_X64:
                ApiRequestHandler.handleTokenX64Request(c, inPacket);
                break;
            case CREATE_ACCOUNT_REQUEST:
                ApiRequestHandler.handleCreateAccountRequest(c, inPacket);
                break;
        }
        System.out.printf("[API 客戶端]\t| %s, %d/0x%s\t| %s%n", opHeader, op, Integer.toHexString(op).toUpperCase(), inPacket);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        Client c = (Client) ctx.channel().attr(CLIENT_KEY).get();
        if (!(cause instanceof IOException)) {
            System.out.printf("%s 發生異常：%n", c.getIP());
        }
    }
}
