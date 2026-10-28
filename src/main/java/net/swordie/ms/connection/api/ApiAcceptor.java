package net.swordie.ms.connection.api;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import net.swordie.ms.ServerConstants;
import net.swordie.ms.client.Client;

import java.util.concurrent.CountDownLatch;

import static net.swordie.ms.connection.netty.NettyClient.CLIENT_KEY;

/**
 * The old 8483 token / register API (no current launcher uses it; cat-launcher talks to 8600). Health audit step 2:
 * off unless {@value #ENABLED_KEY}=true in config/server.properties, and then bound to 127.0.0.1 only.
 */
public class ApiAcceptor implements Runnable {
    public static final String ENABLED_KEY = "api.legacy.enabled";
    public static final String BIND_ADDRESS = "127.0.0.1";

    public static boolean enabled() {
        return net.swordie.ms.util.HotProperties.server().bool(ENABLED_KEY, false);
    }

    @Override
    public void run() {
        run(null);
    }

    public void run(CountDownLatch bindLatch) {
        if (!enabled()) {
            System.out.printf("API %d disabled (%s=false; the launchers use 8600).%n", ServerConstants.API_PORT, ENABLED_KEY);
            if (bindLatch != null) {
                bindLatch.countDown();
            }
            return;
        }
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup);
            b.channel(NioServerSocketChannel.class);
            b.childHandler(new ChannelInitializer<SocketChannel>() {

                @Override
                protected void initChannel(SocketChannel ch) {
                    ch.pipeline().addLast(new ApiDecoder(), new ApiHandler(), new ApiEncoder());
                    Client c = new Client(ch);
                    ch.attr(CLIENT_KEY).set(c);
                }
            });

            b.childOption(ChannelOption.TCP_NODELAY, true);
            b.childOption(ChannelOption.SO_KEEPALIVE, true);
            ChannelFuture f = b.bind(BIND_ADDRESS, ServerConstants.API_PORT).sync();
            System.out.printf("API listening on %s:%d (%s=true).%n", BIND_ADDRESS, ServerConstants.API_PORT, ENABLED_KEY);
            if (bindLatch != null) {
                bindLatch.countDown();
            }
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
}
