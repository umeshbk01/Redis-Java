package com.java.redis.internal.command;

import com.java.redis.internal.command.handlers.*;
import com.java.redis.internal.datastore.DataStore;
import com.java.redis.internal.protocol.Command;
import com.java.redis.internal.protocol.RedisReply;

import java.util.HashMap;
import java.util.Map;

public class CommandExecutor {
    private final Map<String, CommandHandler> registry = new HashMap<>();

    public CommandExecutor(DataStore store) {
        // Strings
        registry.put("GET",    new GetCommand(store));
        registry.put("SET",    new SetCommand(store));
        registry.put("INCR",   new IncrCommand(store));
        // Hashes
        registry.put("HSET",   new HSetCommand(store));
        registry.put("HGET",   new HGetCommand(store));
        // Lists
        registry.put("LPUSH",  new LPushCommand(store));
        // Sets
        registry.put("SADD",   new SAddCommand(store));
        registry.put("SREM",   new SRemCommand(store));
        registry.put("SMEMBERS", new SMembersCommand(store));
        // Sorted Sets
        registry.put("ZADD",   new ZAddCommand(store));
        registry.put("ZRANGE", new ZRangeCommand(store));
    }

    public RedisReply execute(Command cmd) {
        CommandHandler handler = registry.get(cmd.getName());
        if (handler == null) {
            return RedisReply.error("ERR unknown command '" + cmd.getName() + "'");
        }
        try {
            return handler.handle(cmd);
        } catch (IllegalStateException e) {
            return RedisReply.error(e.getMessage());
        }
    }
}
