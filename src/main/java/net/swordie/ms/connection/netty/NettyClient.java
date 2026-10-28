package net.swordie.ms.connection.netty;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import net.swordie.ms.ServerConfig;
import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.SharedPacket;
import net.swordie.ms.connection.crypto.MapleCrypto;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.capture.PacketCapture;
import net.swordie.ms.handlers.ClientSocket;
import net.swordie.ms.handlers.header.OutHeader;
import net.swordie.ms.console.PacketDebugLog;
import net.swordie.ms.util.DataPrinter;

import java.util.Arrays;

public class NettyClient {

    public static final int MAX_PACKET_SIZE = 16777215;
    public static final AttributeKey<MapleCrypto> CRYPTO_KEY = AttributeKey.valueOf("A");
    public static final AttributeKey<NettyClient> CLIENT_KEY = AttributeKey.valueOf("C");

    protected final Channel ch;
    private int storedLength = -1;
    /** Packets written while {@link #holdOutbound()} is active; sent by {@link #releaseOutboundAfter}. */
    private java.util.List<OutPacket> held;

    /**
     * Holds every packet written from now on until {@link #releaseOutboundAfter} (TMS 282.3 first field entry: the
     * 265 character load writes quest records etc. before LP02EF, which the 282 client cannot take yet).
     */
    public synchronized void holdOutbound() {
        if (held == null) {
            held = new java.util.ArrayList<>();
        }
    }

    /** Writes {@code first}, then every held packet in order, and stops holding. */
    public void releaseOutboundAfter(OutPacket first) {
        releaseOutboundAfter(() -> {
            if (first != null) {
                write(first);
            }
        });
    }

    /** Stops holding, runs {@code first} (its writes go out directly), then writes every held packet in order. */
    public void releaseOutboundAfter(Runnable first) {
        java.util.List<OutPacket> queued;
        synchronized (this) {
            queued = held;
            held = null;
        }
        first.run();
        if (queued != null) {
            if (!queued.isEmpty()) {
                System.out.println("[Outbound] " + queued.size() + " packet(s) written before the field entry sent after it");
            }
            queued.forEach(this::write);
        }
    }

    public NettyClient(Channel channel) {
        this.ch = channel;
    }

    public final int getStoredLength() {
        return storedLength;
    }

    public final void setStoredLength(int val) {
        storedLength = val;
    }

    public void write(OutPacket packet) {
        synchronized (this) {
            if (held != null) {
                held.add(packet);
                return;
            }
        }
        packet = net.swordie.ms.connection.protocol.codec.LegacyPacketTranslator.apply(packet); // 265: unchanged
        if (!Protocol.active().outboundGuard().permit(packet, this::getIP)
                || net.swordie.ms.connection.protocol.DebugOutboundFilter.suppress(packet)) { // debug.suppressOutbound, empty by default
            reportDropped(packet);
            return;
        }
        if (packet.getLength() > MAX_PACKET_SIZE) {
            DataPrinter.send(DataPrinter.PACKET_LOG_ERR, String.format("Packet is too big! ([Out] %d size = %d)",
                    packet.getHeader(), packet.getLength()), false);
            return;
        }
        final byte[] data = packet.getFrameData();
        this.ch.writeAndFlush(packet.hasAuxiliaryData() ? new SharedPacket(data, true) : data);
        debug(this, packet);
        try {
            net.swordie.ms.handlers.login.tms282.Tms282FamiliarWindow.afterSent(this, packet);
        } catch (RuntimeException | LinkageError failure) {
            System.out.println("[TMS282] Optional familiar window bootstrap failed: " + failure.getClass().getSimpleName());
        }
        try {
            net.swordie.ms.handlers.login.tms282.Tms282LynnController.afterSent(this, packet);
        } catch (RuntimeException | LinkageError failure) {
            System.out.println("[TMS282] Optional Lynn controller bootstrap failed: " + failure.getClass().getSimpleName());
        }
        try {
            net.swordie.ms.client.jobs.sengoku.Tms282KannaOniController.afterSent(this, packet);
        } catch (RuntimeException | LinkageError failure) {
            System.out.println("[TMS282] Kanna Oni bootstrap failed: " + failure);
        }
        try {
            net.swordie.ms.handlers.login.tms282.Tms282MonolithManager.afterSent(this, packet);
        } catch (RuntimeException | LinkageError failure) {
            System.out.println("[TMS282] Optional monolith manager failed: " + failure);
        }
    }

    /** Hands a guard-dropped packet to the socket's capture (capture.send), so the packet trace lists it. */
    private void reportDropped(OutPacket packet) {
        try {
            PacketCapture capture = PacketCapture.of(ch);
            if (capture != null) {
                capture.dropped(packet.getData());
            }
        } catch (RuntimeException ignored) {
        }
    }

    public static void debug(String name, OutPacket outPacket) {
        try {
            OutHeader header = OutHeader.getOutHeaderByOp(outPacket.getHeader());
            if (!OutHeader.isSpamHeader(header)) {
                if (ServerConfig.PACKET_LOG) {
                    byte[] payload = copyPayload(outPacket);
                    PacketDebugLog.logSend("", header, outPacket.getHeader(), payload, name);
                }
            }
        } catch (Exception ignored) {
        }
    }

    public static void debug(NettyClient c, OutPacket outPacket) {
        try {
            if (c == null) {
                return;
            }
            Client client = (Client) c;
            Char chr = client.getChr();
            OutHeader header = OutHeader.getOutHeaderByOp(outPacket.getHeader());
            if (OutHeader.isSpamHeader(header)) {
                return;
            }
            if (!ServerConfig.PACKET_LOG) {
                return;
            }
            byte[] payload = copyPayload(outPacket);
            String ip = client.getIP();
            if (chr != null) {
                PacketDebugLog.logSend(ip, header, outPacket.getHeader(), payload, chr.getName());
            } else {
                PacketDebugLog.logSend(ip, header, outPacket.getHeader(), payload, null);
            }
        } catch (Exception ignored) {
        }
    }

    private static byte[] copyPayload(OutPacket outPacket) {
        byte[] data = outPacket.getData();
        if (data == null || data.length <= 2) {
            return new byte[0];
        }
        return Arrays.copyOfRange(data, 2, data.length);
    }

    public Channel getCh() {
        return ch;
    }

    public void close() {
        System.out.println("[NettyClient] 已向 " + getIP() + " 發送關閉客戶端封包");
        write(ClientSocket.migrateCommand(false, (short) 0));
    }

    public String getIP() {
        String ip = ch.remoteAddress().toString().split(":")[0];
        return ip.startsWith("/") ? ip.substring(1) : ip;
    }

    public String getPort() {
        return ch.remoteAddress().toString().split(":")[1];
    }
}
