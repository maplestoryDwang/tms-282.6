package net.swordie.ms.connection;

import net.swordie.ms.connection.api.ApiAcceptor;
import net.swordie.ms.connection.netty.ChannelAcceptor;
import net.swordie.ms.connection.netty.LoginAcceptor;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolConfig;
import net.swordie.ms.connection.protocol.ProtocolProfile;
import net.swordie.ms.handlers.PacketProcessor;
import net.swordie.ms.world.Channel;
import net.swordie.ms.world.World;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** 啓服時綁定 API / 登錄 / 頻道端口，全部就緒後再返回。 */
public final class ServerNetworkBootstrap {
    private static final long BIND_TIMEOUT_SECONDS = 120L;

    private ServerNetworkBootstrap() {
    }

    public static void startAllAndAwait(World world) throws InterruptedException {
        if (world == null || world.getChannels() == null) {
            throw new IllegalStateException("World 未初始化，無法啓動網絡服務");
        }
        // 協定 profile（config protocol.version）；PROFILE_CODEC 的入站 op 仍對到 265 handler 時拒絕啓動。
        ProtocolProfile profile = Protocol.active();
        PacketProcessor.validate(profile);
        ProtocolConfig config = Protocol.config();
        System.out.printf("[Protocol] %s, capture.recv=%s, capture.send=%s, capture.trace=%s, unproven 265 routes=%d%n",
                profile, config.captureRecv(), config.captureSend(), config.captureTrace(),
                PacketProcessor.registryFor(profile).unprovenRoutes(profile).size());
        if (config.captureAny()) {
            System.out.printf("[Capture] sessions: %s (prefix \"%s\")%n",
                    config.captureRoot().resolve("sessions").toAbsolutePath().normalize(), config.captureSessionPrefix());
        }
        int channelCount = world.getChannels().size();
        CountDownLatch latch = new CountDownLatch(2 + channelCount);

        Thread api = new Thread(() -> new ApiAcceptor().run(latch), "ApiAcceptor");
        Thread login = new Thread(() -> new LoginAcceptor().run(latch), "LoginAcceptor");
        api.setDaemon(false);
        login.setDaemon(false);
        api.start();
        login.start();

        for (Channel channel : world.getChannels()) {
            ChannelAcceptor acceptor = new ChannelAcceptor();
            acceptor.channel = channel;
            Thread channelThread = new Thread(() -> acceptor.run(latch), "ChannelAcceptor-" + channel.getChannelId());
            channelThread.setDaemon(false);
            channelThread.start();
        }

        if (!latch.await(BIND_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            throw new IllegalStateException("網絡服務綁定超時，請檢查端口是否被佔用");
        }
    }
}
