package net.swordie.ms.handlers.dispatch;

import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.netty.PacketHandler;
import net.swordie.ms.util.DataPrinter;

import java.util.Objects;
import java.util.function.BiFunction;

/**
 * Decorators for {@link PacketHandler} (decorator pattern), so overlays state policy declaratively:
 * <pre>
 * overlay.register(InHeader.X, require(ClientGuard.hasCharacter(), reportThen(sink, handler)));
 * </pre>
 * A rejected packet's unread body is always consumed, so no handler sees a half-read packet.
 */
public final class HandlerDecorators {
    private static final String REJECT_LOG = "Packets/guard-rejects.txt";

    private HandlerDecorators() {
    }

    /** Receives the whole packet (opcode included) before the handler parses it. */
    @FunctionalInterface
    public interface PacketReporter {
        void report(Client c, Char chr, byte[] packet);
    }

    /** Told about a packet a {@link #require} guard refused (after its body was consumed). */
    @FunctionalInterface
    public interface RejectionListener {
        void rejected(ClientGuard guard, Client c, int op, int length);

        /** Default: one line in logs/<date>/Packets/guard-rejects.txt. */
        RejectionListener LOG = (guard, c, op, length) -> DataPrinter.send(REJECT_LOG,
                String.format("[ClientGuard] %s rejected op=0x%04X len=%d from %s",
                        guard.description(), op, length, c == null ? "?" : safeIp(c)), false);
    }

    /** Runs {@code handler} only when {@code guard} permits; otherwise consumes the packet and logs the rejection. */
    public static PacketHandler require(ClientGuard guard, PacketHandler handler) {
        return require(guard, handler, RejectionListener.LOG);
    }

    /** As {@link #require(ClientGuard, PacketHandler)}, reporting rejections to {@code listener}. */
    public static PacketHandler require(ClientGuard guard, PacketHandler handler, RejectionListener listener) {
        Objects.requireNonNull(guard, "guard");
        Objects.requireNonNull(handler, "handler");
        Objects.requireNonNull(listener, "listener");
        return (c, chr, in) -> {
            if (guard.permits(c, chr)) {
                handler.handle(c, chr, in);
                return;
            }
            consume(in);
            listener.rejected(guard, c, opcodeOf(in), in.getLength());
        };
    }

    /** Runs {@code handler} only when {@code guard} permits; otherwise consumes the packet silently. */
    public static PacketHandler when(ClientGuard guard, PacketHandler handler) {
        Objects.requireNonNull(guard, "guard");
        Objects.requireNonNull(handler, "handler");
        return (c, chr, in) -> {
            if (guard.permits(c, chr)) {
                handler.handle(c, chr, in);
            } else {
                consume(in);
            }
        };
    }

    /**
     * Runs {@code handler} only when {@code guard} permits; otherwise consumes the packet and tells
     * {@code listener} (which must not treat the rejection as fatal: that is {@link #require}'s job).
     */
    public static PacketHandler when(ClientGuard guard, PacketHandler handler, RejectionListener listener) {
        Objects.requireNonNull(guard, "guard");
        Objects.requireNonNull(handler, "handler");
        Objects.requireNonNull(listener, "listener");
        return (c, chr, in) -> {
            if (guard.permits(c, chr)) {
                handler.handle(c, chr, in);
                return;
            }
            consume(in);
            listener.rejected(guard, c, opcodeOf(in), in.getLength());
        };
    }

    /** Hands a copy of the packet to {@code reporter}, then runs {@code handler}. */
    public static PacketHandler reportThen(PacketReporter reporter, PacketHandler handler) {
        Objects.requireNonNull(reporter, "reporter");
        Objects.requireNonNull(handler, "handler");
        return (c, chr, in) -> {
            reporter.report(c, chr, in.getData().clone());
            handler.handle(c, chr, in);
        };
    }

    /** Consumes the packet and answers with {@code reply}'s packet (null = no answer). */
    public static PacketHandler declineWith(BiFunction<Client, Char, OutPacket> reply) {
        Objects.requireNonNull(reply, "reply");
        return (c, chr, in) -> {
            consume(in);
            OutPacket out = reply.apply(c, chr);
            if (out != null && c != null) {
                c.write(out);
            }
        };
    }

    /** Reads and discards the packet (known opcode, deliberately ignored). */
    public static PacketHandler consumeSilently() {
        return (c, chr, in) -> consume(in);
    }

    static void consume(InPacket in) {
        int unread = in.getUnreadAmount();
        if (unread > 0) {
            in.decodeArr(unread);
        }
    }

    private static int opcodeOf(InPacket in) {
        byte[] data = in.getData();
        return data.length < 2 ? -1 : (data[0] & 0xFF) | (data[1] & 0xFF) << 8;
    }

    static String safeIp(Client c) {
        try {
            return c.getIP();
        } catch (RuntimeException e) {
            return "?";
        }
    }
}
