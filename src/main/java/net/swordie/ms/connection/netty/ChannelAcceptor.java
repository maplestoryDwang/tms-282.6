package net.swordie.ms.connection.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import net.swordie.ms.connection.protocol.SessionKind;
import net.swordie.ms.util.DataPrinter;

import java.util.concurrent.CountDownLatch;


public class ChannelAcceptor implements Runnable {

    public net.swordie.ms.world.Channel channel;

    @Override
    public void run() {
        run(null);
    }

    public void run(CountDownLatch bindLatch) {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup);
            b.channel(NioServerSocketChannel.class);
            b.childHandler(new ProtocolChannelInitializer(SessionKind.CHANNEL));
            b.childOption(ChannelOption.TCP_NODELAY, true);
            b.childOption(ChannelOption.SO_KEEPALIVE, true);
            ChannelFuture f = b.bind("127.0.0.1", channel.getPort()).sync();
            System.out.printf("[World %d] Channel %d listening on 127.0.0.1:%d.%n", channel.getWorldId(), channel.getChannelId(), channel.getPort());
            if (bindLatch != null) {
                bindLatch.countDown();
            }
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
}
