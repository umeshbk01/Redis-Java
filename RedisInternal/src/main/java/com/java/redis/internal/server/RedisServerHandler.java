package com.java.redis.internal.server;

import java.io.InputStream;
import java.util.concurrent.Executors;

import com.java.redis.internal.command.CommandExecutor;
import com.java.redis.internal.protocol.Command;
import com.java.redis.internal.protocol.RedisReply;
import com.java.redis.internal.protocol.RespParser;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class RedisServerHandler extends ChannelInboundHandlerAdapter{
    private final CommandExecutor commandExecutor;
    public RedisServerHandler(CommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
    }
    @Override
    public void channelRead(io.netty.channel.ChannelHandlerContext ctx, Object msg) throws Exception {
        var buffer = (io.netty.buffer.ByteBuf) msg;
        InputStream in = new io.netty.buffer.ByteBufInputStream(buffer);
        RespParser parser = new RespParser(in);
        try{
            Command cmd = parser.parse();
            Executors.newThreadPerTaskExecutor(Thread.ofVirtual().factory())
                .execute(() -> {
                    RedisReply reply = commandExecutor.execute(cmd);
                    ctx.writeAndFlush(Unpooled.copiedBuffer(reply.toBytes()));
                });
        }catch(Exception e) {
            ctx.writeAndFlush(Unpooled.copiedBuffer(RedisReply.error(e.getMessage()).toBytes()));
        }finally{
            buffer.release();
        }
    }
    @Override public void exceptionCaught(ChannelHandlerContext ctx,Throwable t){ctx.close();}
}
