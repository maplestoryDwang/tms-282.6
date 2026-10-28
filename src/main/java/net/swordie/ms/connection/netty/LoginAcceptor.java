package net.swordie.ms.connection.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import net.swordie.ms.ServerConstants;
import net.swordie.ms.connection.protocol.SessionKind;
import net.swordie.ms.util.DataPrinter;

import java.util.concurrent.CountDownLatch;

public class LoginAcceptor implements Runnable {

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
            b.childHandler(new ProtocolChannelInitializer(SessionKind.LOGIN));
            b.childOption(ChannelOption.TCP_NODELAY, true);
            b.childOption(ChannelOption.SO_KEEPALIVE, true);
            ChannelFuture f = b.bind("127.0.0.1", ServerConstants.LOGIN_PORT).sync();
            System.out.printf("Login listening on 127.0.0.1:%d.%n", ServerConstants.LOGIN_PORT);
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
