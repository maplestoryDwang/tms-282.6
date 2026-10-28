package net.swordie.ms.connection.protocol.capture;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Capture of one socket (facade over the pipeline taps): numbers each packet on the socket thread, then on the
 * {@link Executor} (normally {@link CaptureWriter}) applies the redaction rules and hands the result to every
 * {@link CaptureObserver}, so the socket thread only copies bytes and never touches the disk.
 * <ul>
 *     <li>RECV ({@link CaptureInboundHandler}, after decryption): redacted when {@link CaptureRedaction} says so
 *     (SensitiveClientOpcodes plus the 265 credential headers), the probe rule;</li>
 *     <li>SEND ({@link CaptureOutboundHandler}, before encryption): redacted when {@link SendSecretScan} finds a
 *     secret; SensitiveClientOpcodes does not apply to LP opcodes (probe rule);</li>
 *     <li>DROPPED ({@code NettyClient.write}, refused by the OutboundGuard): never a send file, traced as DROPPED
 *     with its own sequence;</li>
 *     <li>HELLO: the plaintext handshake, {@code hello.bin} only.</li>
 * </ul>
 * An observer that throws is switched off for this socket and logged once; the socket keeps running.
 */
public final class PacketCapture {
    /** Set on channels whose outbound packets are captured, so NettyClient can report guard drops. */
    public static final AttributeKey<PacketCapture> KEY = AttributeKey.valueOf("packetCapture");

    private final String session;
    private final CaptureRedaction receiveRedaction;
    private final SendSecretScan sendScan;
    private final List<CaptureObserver> observers;
    private final boolean[] failed;
    private final Executor executor;
    private final AtomicInteger recvIndex = new AtomicInteger();
    private final AtomicInteger sendIndex = new AtomicInteger();
    private final AtomicInteger dropIndex = new AtomicInteger();

    public PacketCapture(String session, CaptureRedaction receiveRedaction, SendSecretScan sendScan,
                         List<CaptureObserver> observers, Executor executor) {
        this.session = Objects.requireNonNull(session, "session");
        this.receiveRedaction = Objects.requireNonNull(receiveRedaction, "receiveRedaction");
        this.sendScan = Objects.requireNonNull(sendScan, "sendScan");
        this.observers = List.copyOf(observers);
        this.failed = new boolean[this.observers.size()];
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    /** Files only, written on the calling thread (the former CaptureInboundHandler behavior). */
    public static PacketCapture filesOnly(CaptureSession files) {
        return new PacketCapture(files.name(), files.redaction(), SendSecretScan.HASH_MARKERS_ONLY, List.of(files),
                CaptureWriter.direct());
    }

    /** The capture of {@code channel}, or null when its outbound packets are not captured. */
    public static PacketCapture of(Channel channel) {
        return channel == null ? null : channel.attr(KEY).get();
    }

    public String session() {
        return session;
    }

    /** A decrypted client packet; {@code plain} and {@code auxiliary} are copied, not kept. */
    public void received(byte[] plain, byte[] auxiliary) {
        int index = recvIndex.getAndIncrement();
        byte[] data = plain.clone();
        byte[] aux = auxiliary == null ? null : auxiliary.clone();
        submit(() -> {
            CapturedPacket packet = new CapturedPacket(CapturedPacket.Kind.RECV, index, data, aux, false);
            return receiveRedaction.mustRedact(packet.opcode())
                    ? new CapturedPacket(CapturedPacket.Kind.RECV, index, CapturedPacket.redact(data), null, true)
                    : packet;
        });
    }

    /**
     * A packet on its way to the encoder: {@code frame} is what NettyClient hands to the channel
     * ({@code OutPacket.getFrameData()}: main, then for an auxiliary packet AUX and uint32LE(AUX.length)).
     * Copied before returning, because the encoder encrypts a {@code byte[]} in place.
     */
    public void sent(byte[] frame, boolean auxiliary) {
        int index = sendIndex.getAndIncrement();
        byte[] copy = frame.clone();
        submit(() -> {
            byte[] main = copy;
            byte[] aux = null;
            if (auxiliary && copy.length >= 6) {
                int length = (copy[copy.length - 4] & 0xFF) | (copy[copy.length - 3] & 0xFF) << 8
                        | (copy[copy.length - 2] & 0xFF) << 16 | (copy[copy.length - 1] & 0xFF) << 24;
                int mainEnd = copy.length - 4 - length;
                if (length >= 0 && mainEnd >= 2) {
                    main = Arrays.copyOf(copy, mainEnd);
                    aux = Arrays.copyOfRange(copy, mainEnd, copy.length - 4);
                }
            }
            return sendScan.leaks(main, aux)
                    ? new CapturedPacket(CapturedPacket.Kind.SEND, index, CapturedPacket.redact(main), null, true)
                    : new CapturedPacket(CapturedPacket.Kind.SEND, index, main, aux, false);
        });
    }

    /** The plaintext hello (written before the channel has a client). */
    public void hello(byte[] blob) {
        byte[] copy = blob.clone();
        submit(() -> new CapturedPacket(CapturedPacket.Kind.HELLO, 0, copy, null, false));
    }

    /** A packet the OutboundGuard refused ({@code OutPacket.getData()}); traced, never written as a send file. */
    public void dropped(byte[] data) {
        int index = dropIndex.getAndIncrement();
        byte[] copy = data.clone();
        submit(() -> sendScan.leaks(copy, null)
                ? new CapturedPacket(CapturedPacket.Kind.DROPPED, index, CapturedPacket.redact(copy), null, true)
                : new CapturedPacket(CapturedPacket.Kind.DROPPED, index, copy, null, false));
    }

    private void submit(Supplier<CapturedPacket> builder) {
        try {
            executor.execute(() -> dispatch(builder));
        } catch (RuntimeException e) {
            System.out.println("[Capture] " + session + ": packet not captured: " + e);
        }
    }

    /** Writer thread: builds the (redacted) packet once and gives it to every live observer. */
    private void dispatch(Supplier<CapturedPacket> builder) {
        CapturedPacket packet;
        try {
            packet = builder.get();
        } catch (RuntimeException e) {
            System.out.println("[Capture] " + session + ": packet not captured: " + e);
            return;
        }
        for (int i = 0; i < observers.size(); i++) {
            if (failed[i]) {
                continue;
            }
            CaptureObserver observer = observers.get(i);
            try {
                observer.accept(packet);
            } catch (IOException | RuntimeException | LinkageError e) {
                failed[i] = true;
                System.out.println("[Capture] disabled " + observer.describe() + ": " + e);
            }
        }
    }
}
