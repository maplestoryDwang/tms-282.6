package net.swordie.ms.connection.protocol.capture;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import net.swordie.ms.connection.InPacket;

import java.util.Objects;

/**
 * Pipeline tap between PacketDecoder and ChannelHandler, installed only with {@code capture.recv=true}: hands
 * every decoded client packet to a {@link PacketCapture}, then passes it on unchanged (the packet's read position
 * is not touched). Capture failures are logged by PacketCapture and never reach the socket.
 */
public final class CaptureInboundHandler extends ChannelInboundHandlerAdapter {
    private final PacketCapture capture;

    public CaptureInboundHandler(PacketCapture capture) {
        this.capture = Objects.requireNonNull(capture, "capture");
    }

    /** Files only, written on the socket thread (research checks). */
    public CaptureInboundHandler(CaptureSession session) {
        this(PacketCapture.filesOnly(session));
    }

    public PacketCapture capture() {
        return capture;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof InPacket packet) {
            try {
                capture.received(packet.getData(), packet.getAuxiliaryData());
            } catch (RuntimeException e) {
                System.out.println("[Capture] " + capture.session() + ": RECV not captured: " + e);
            }
        }
        ctx.fireChannelRead(msg);
    }
}
