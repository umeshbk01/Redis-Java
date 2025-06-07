package com.java.redis.internal.command;

import com.java.redis.internal.protocol.Command;
import com.java.redis.internal.protocol.RedisReply;

import java.util.HashMap;
import java.util.Map;

public class CommandExecutor {
    private final Map<String, CommandHandler> registry = new HashMap<>();

    public CommandExecutor(){
        // In the future: registry.put("GET", new GetCommandHandler(...));
        //           registry.put("SET", new SetCommandHandler(...));
    }

    public RedisReply execute(Command cmd) {
        CommandHandler handler = registry.get(cmd.getName());
        if (handler != null) {
            return handler.handle(cmd);
        }
        // Stub: unknown commands return OK for now
        return RedisReply.ok();
    }
}
