package net.swordie.ms.connection.api;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import net.swordie.ms.connection.netty.NettyClient;

public class ApiEncoder extends MessageToByteEncoder<byte[]> {

    @Override
    protected void encode(ChannelHandlerContext chc, byte[] data, ByteBuf bb) throws Exception {
        NettyClient c = chc.channel().attr(NettyClient.CLIENT_KEY).get();
        if (c != null) {
            bb.writeInt(data.length);
            bb.writeBytes(data);
        } else {
            bb.writeBytes(data);
        }
    }
}
