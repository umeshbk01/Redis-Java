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

/**
 * NettyServer: A server implementation using Netty framework
 * This class is responsible for starting and stopping the server,
 * handling incoming connections, and delegating command execution.
 * It uses Netty's event-driven architecture to handle network operations efficiently.
 * 
 * Note: This is a simplified version and does not include all the necessary error handling
 * and configuration options that a production server would require.
 * It is intended to demonstrate the basic structure and flow of a Netty-based server.
 */
public class NettyServer {
    private final int port;
    private final CommandExecutor commandExecutor;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    public NettyServer(int port, CommandExecutor commandExecutor) {
        this.port = port;
        this.commandExecutor = commandExecutor;
    }
    
    public void start() throws InterruptedException {
        // Initialize and start the Netty server components here
        // This is where you would set up the bootstrap, channel options, etc.
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel ch) throws Exception {
                            // Initialize the channel pipeline with handlers
                            ch.pipeline().addLast(new RedisServerHandler(commandExecutor));
                        }
                    })
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
        ChannelFuture future = bootstrap.bind(port).sync();
        serverChannel = future.channel();
        System.out.println("Server started on port " + port);
        serverChannel.closeFuture().sync();
        
    }
    public void stop() throws InterruptedException {
        if (serverChannel != null) {
            serverChannel.close();
        }
        if(workerGroup != null){
            workerGroup.shutdownGracefully();
        }
        if(bossGroup != null){
            bossGroup.shutdownGracefully();
        }
    }
}
