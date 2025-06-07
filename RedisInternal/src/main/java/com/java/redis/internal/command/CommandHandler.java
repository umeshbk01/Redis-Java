package com.java.redis.internal.command;

import com.java.redis.internal.protocol.Command;
import com.java.redis.internal.protocol.RedisReply;

public interface CommandHandler {
    RedisReply handle(Command cmd);
}
