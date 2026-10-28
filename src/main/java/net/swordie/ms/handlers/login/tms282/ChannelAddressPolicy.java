package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.Client;
import net.swordie.ms.console.config.ServerRuntimeConfig;
import net.swordie.ms.util.HotProperties;

import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Objects;

/**
 * Login and migration packets in this package always point to the loopback alias {@link ServerRuntimeConfig#PACKET_HOST}
 * (on this computer's loopback interface, relayed by the launcher to the server on 127.0.0.1).
 */
public final class ChannelAddressPolicy {
    public static final String KEY = "tms282.channelAdvertisedHost";
    public static final String DEFAULT_HOST = "127.0.0.1";
    public static final String AUTO = "auto";

    private static final ChannelAddressPolicy SERVER = new ChannelAddressPolicy(HotProperties.server());

    public ChannelAddressPolicy(HotProperties settings) {
        Objects.requireNonNull(settings, "settings");
    }

    public static ChannelAddressPolicy server() {
        return SERVER;
    }

    /** A new array so callers cannot mutate the packaged address. Config and JVM overrides are ignored. */
    public byte[] advertisedHost() {
        return ServerRuntimeConfig.serverHostBytes();
    }

    public String describe() {
        return ServerRuntimeConfig.PACKET_HOST;
    }

    /** Invalid socket families still refuse migration, as before. */
    public byte[] channelAddress(SocketAddress local) {
        return ipv4(local) == null ? null : advertisedHost();
    }

    public byte[] legacyAddress(Client client) {
        return advertisedHost();
    }

    public static String text(byte[] ip) {
        return (ip[0] & 0xFF) + "." + (ip[1] & 0xFF) + "." + (ip[2] & 0xFF) + "." + (ip[3] & 0xFF);
    }

    static byte[] ipv4(SocketAddress address) {
        if (!(address instanceof InetSocketAddress inet) || !(inet.getAddress() instanceof Inet4Address a)) {
            return null;
        }
        return a.getAddress();
    }
}
