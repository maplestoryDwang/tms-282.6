package net.swordie.ms.connection.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import net.swordie.ms.client.Client;
import net.swordie.ms.connection.SharedPacket;
import net.swordie.ms.connection.crypto.MapleCrypto;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.SessionCipher;
import net.swordie.ms.connection.protocol.SessionKind;

public final class PacketEncoder extends MessageToByteEncoder<Object> {

    private byte[] iv;
    private final SessionCipher cipher;

    /**
     * Encoder with the active profile's login-socket cipher (GMS 265: the former rule, identical on both
     * sockets). Acceptors pass the socket's own cipher through the other constructor.
     */
    public PacketEncoder(byte[] initialIv) {
        this(initialIv, Protocol.active().cipherPolicy().forSession(SessionKind.LOGIN));
    }

    public PacketEncoder(byte[] initialIv, SessionCipher cipher) {
        this.iv = initialIv.clone();
        this.cipher = java.util.Objects.requireNonNull(cipher, "cipher");
    }

    @Override
    protected void encode(ChannelHandlerContext chc, Object msg, ByteBuf bb) throws Exception {
        if (this.iv == null) {
            throw new IllegalStateException("IV has not been initialized.");
        }
        final byte[] src;
        final boolean shared;
        final boolean auxiliary;
        if (msg instanceof byte[]) {
            src = (byte[]) msg;
            shared = false;
            auxiliary = false;
        } else if (msg instanceof SharedPacket) {
            src = ((SharedPacket) msg).data;
            shared = true;
            auxiliary = ((SharedPacket) msg).auxiliary;
        } else {
            throw new IllegalArgumentException("Unsupported msg type: " + msg.getClass());
        }
        final byte[] data;
        if (shared) {
            data = new byte[src.length];
            System.arraycopy(src, 0, data, 0, src.length);
        } else {
            data = src;
        }
        NettyClient c = chc.channel().attr(NettyClient.CLIENT_KEY).get();
        MapleCrypto mCr = chc.channel().attr(NettyClient.CRYPTO_KEY).get();
        if (c != null) {
            if (data.length < 2 || data.length > NettyClient.MAX_PACKET_SIZE) {
                throw new IllegalArgumentException("Invalid packet length: " + data.length);
            }
            byte[] currentIv = this.iv;
            byte[] head = MapleCrypto.getFrameHeader(data.length, currentIv, auxiliary);
            cipher.encrypt(mCr, (Client) c, currentIv, data, auxiliary);
            this.iv = MapleCrypto.getNewIv(currentIv);
            bb.writeBytes(head);
            bb.writeBytes(data);
        } else {
            bb.writeBytes(data);
        }
    }

}
