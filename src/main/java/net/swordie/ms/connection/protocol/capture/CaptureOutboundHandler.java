package net.swordie.ms.connection.protocol.capture;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.util.AttributeKey;
import net.swordie.ms.connection.SharedPacket;

import java.util.Objects;

/**
 * Pipeline tap on the tail side of PacketEncoder (so it sees plaintext, after NettyClient's OutboundGuard),
 * installed only with {@code capture.send=true}. Every message the encoder will write is handed to
 * {@link PacketCapture}: a {@code byte[]} or {@link SharedPacket} while the channel has a client is a SEND
 * (frame data: main, then the auxiliary trailer when flagged); before that, the {@code byte[]} is the plaintext
 * hello. The message itself is passed on untouched (PacketCapture copies it first, because the encoder
 * encrypts a {@code byte[]} in place).
 */
public final class CaptureOutboundHandler extends ChannelOutboundHandlerAdapter {
    private final PacketCapture capture;
    private final AttributeKey<?> clientKey;

    /**
     * @param clientKey the channel attribute that is set once the hello has been written (NettyClient.CLIENT_KEY)
     */
    public CaptureOutboundHandler(PacketCapture capture, AttributeKey<?> clientKey) {
        this.capture = Objects.requireNonNull(capture, "capture");
        this.clientKey = Objects.requireNonNull(clientKey, "clientKey");
    }

    public PacketCapture capture() {
        return capture;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        try {
            boolean hasClient = ctx.channel().attr(clientKey).get() != null;
            if (msg instanceof byte[] data) {
                if (hasClient) {
                    capture.sent(data, false);
                } else {
                    capture.hello(data);
                }
            } else if (msg instanceof SharedPacket shared && hasClient) {
                capture.sent(shared.data, shared.auxiliary);
            }
        } catch (RuntimeException e) {
            System.out.println("[Capture] " + capture.session() + ": SEND not captured: " + e);
        }
        ctx.write(msg, promise);
    }
}
