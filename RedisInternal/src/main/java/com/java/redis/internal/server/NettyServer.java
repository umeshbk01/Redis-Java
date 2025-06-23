package com.java.redis.internal.server;

import com.java.redis.internal.command.CommandExecutor;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class NettyServer {
    private final int port;
    private final CommandExecutor commandExecutor;
    private final int bossThreads;
    private final int workerThreads;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    public NettyServer(int port, CommandExecutor commandExecutor, int bossThreads, int workerThreads) {
        this.port = port;
        this.commandExecutor = commandExecutor;
        this.bossThreads = bossThreads;
        this.workerThreads = workerThreads;
    }

    public void start() throws InterruptedException {
        System.out.println("[DEBUG] Initializing Netty server...");
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        System.out.println("[DEBUG] Initializing channel pipeline for new connection...");
                        ch.pipeline().addLast(new RedisServerHandler(commandExecutor));
                    }
                })
                .childOption(ChannelOption.SO_KEEPALIVE, true);
        System.out.println("[DEBUG] Binding server to port " + port + "...");
        ChannelFuture future = bootstrap.bind(port).sync();
        serverChannel = future.channel();
        System.out.println("[DEBUG] Server started on port " + port);
        serverChannel.closeFuture().sync();
        System.out.println("[DEBUG] Server channel closed.");
    }

    public void stop() throws InterruptedException {
        System.out.println("[DEBUG] Stopping Netty server...");
        if (serverChannel != null) {
            serverChannel.close();
            System.out.println("[DEBUG] Server channel closed.");
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
            System.out.println("[DEBUG] Worker group shutdown initiated.");
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
            System.out.println("[DEBUG] Boss group shutdown initiated.");
        }
        System.out.println("[DEBUG] Netty server stopped.");
    }
}