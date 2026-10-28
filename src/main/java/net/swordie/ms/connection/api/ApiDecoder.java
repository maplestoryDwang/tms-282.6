package net.swordie.ms.connection.api;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.crypto.MapleCrypto;
import net.swordie.ms.connection.netty.NettyClient;

import java.util.List;

/**
 * @author Sjonnie
 * Created on 10/5/2018.
 */
public class ApiDecoder extends ByteToMessageDecoder {
    /** A request is an opcode (2 bytes) plus at most a few strings: anything outside [MIN, MAX] closes the socket. */
    public static final int MIN_LENGTH = 2;
    public static final int MAX_LENGTH = 4096;

    @Override
    protected void decode(ChannelHandlerContext chc, ByteBuf in, List<Object> out) throws Exception {
        NettyClient c = chc.channel().attr(NettyClient.CLIENT_KEY).get();
        if (c != null) {
            if (c.getStoredLength() == -1) {
                if (in.readableBytes() < 4) {
                    return;
                }
                int length = in.readInt();
                if (length < MIN_LENGTH || length > MAX_LENGTH) {
                    in.skipBytes(in.readableBytes());
                    System.out.printf("[API] %s: length %d outside %d..%d, closing%n", chc.channel().remoteAddress(), length, MIN_LENGTH, MAX_LENGTH);
                    chc.close();
                    return;
                }
                c.setStoredLength(length);
            }
            if (in.readableBytes() >= c.getStoredLength()) {
                byte[] dec = new byte[c.getStoredLength()];
                in.readBytes(dec);
                c.setStoredLength(-1);

                InPacket inPacket = new InPacket(dec);
                out.add(inPacket);
            }
        }
    }
}
