package net.swordie.ms.connection.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.Packet;
import net.swordie.ms.connection.crypto.MapleCrypto;

import java.util.List;
import java.util.Arrays;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class PacketDecoder extends ByteToMessageDecoder {

    /**
     * Health audit step 2: a socket that has not authenticated yet (no user: the login socket before CP0074 / CP0073
     * succeeded, a channel socket before its MIGRATE_IN) may send frames of at most 64 KB (the largest real one is
     * the 14 KB CP0078 file report); afterwards {@link NettyClient#MAX_PACKET_SIZE} as before.
     */
    public static final int PREAUTH_MAX_PACKET_SIZE = 64 * 1024;

    private byte[] iv;
    private boolean paddedFrame;

    public PacketDecoder(byte[] initialIv) {
        this.iv = initialIv.clone();
    }

    @Override
    protected void decode(ChannelHandlerContext chc, ByteBuf in, List<Object> out) {
        if (this.iv == null) {
            throw new IllegalStateException("IV has not been initialized.");
        }
        NettyClient c = chc.channel().attr(NettyClient.CLIENT_KEY).get();
        MapleCrypto mCr = chc.channel().attr(NettyClient.CRYPTO_KEY).get();
        if (c != null) {
            byte[] currentIv = this.iv;
            if (c.getStoredLength() == -1) {
                if (in.readableBytes() >= 4) {
                    in.markReaderIndex();
                    int h = in.readInt();
                    if (!MapleCrypto.checkPacket(h, currentIv)) {
                        in.skipBytes(in.readableBytes());
                        chc.close();
                        return;
                    }
                    int length = MapleCrypto.getLength(h);
                    paddedFrame = false;
                    if (length >= Packet.MAX_SHORT_PACKET_SIZE) {
                        if (in.readableBytes() < 4) {
                            in.resetReaderIndex();
                            return;
                        }
                        int sequence = Integer.reverseBytes(h) & 0xFFFF;
                        length = in.readIntLE() ^ sequence;
                        paddedFrame = length < 0;
                        length &= Integer.MAX_VALUE;
                    }
                    if (length < (paddedFrame ? 6 : 2) || length > NettyClient.MAX_PACKET_SIZE) {
                        in.skipBytes(in.readableBytes());
                        chc.close();
                        return;
                    }
                    if (length > PREAUTH_MAX_PACKET_SIZE && !authenticated(c)) {
                        in.skipBytes(in.readableBytes());
                        System.out.printf("[NetLimit] %s: %d-byte frame before authentication (max %d), closing%n",
                                chc.channel().remoteAddress(), length, PREAUTH_MAX_PACKET_SIZE);
                        chc.close();
                        return;
                    }
                    c.setStoredLength(length);
                } else {
                    return;
                }
            }
            if (in.readableBytes() >= c.getStoredLength()) {
                byte[] dec = new byte[c.getStoredLength()];
                in.readBytes(dec);
                c.setStoredLength(-1);
                dec = mCr.crypt(dec, currentIv, paddedFrame);
                byte[] auxiliary = new byte[0];
                if (paddedFrame) {
                    // High bit: MAIN || AUX || uint32LE(AUX.length). Native F8E244
                    // splits two independent cursors; AUX is not disposable padding.
                    int padding = ByteBuffer.wrap(dec, dec.length - 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
                    if (padding < 0 || padding > dec.length - 6) {
                        in.skipBytes(in.readableBytes());
                        chc.close();
                        return;
                    }
                    int mainEnd = dec.length - 4 - padding;
                    auxiliary = Arrays.copyOfRange(dec, mainEnd, dec.length - 4);
                    dec = Arrays.copyOf(dec, mainEnd);
                }
                this.iv = MapleCrypto.getNewIv(currentIv);
                InPacket inPacket = new InPacket(dec, auxiliary);
                out.add(inPacket);
            }
        }
    }

    /** A user is attached: CP0074 / CP0073 succeeded on the login socket, MIGRATE_IN on a channel socket. */
    static boolean authenticated(NettyClient c) {
        return c instanceof net.swordie.ms.client.Client client && client.getUser() != null;
    }
}
