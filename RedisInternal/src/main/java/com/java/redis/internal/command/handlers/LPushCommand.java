package com.java.redis.internal.command.handlers;

import com.java.redis.internal.command.CommandHandler;
import com.java.redis.internal.datastore.DataStore;
import com.java.redis.internal.protocol.Command;
import com.java.redis.internal.protocol.RedisReply;

import java.util.List;

public class LPushCommand implements CommandHandler {
    private final DataStore store;
    public LPushCommand(DataStore store) { this.store = store; }

    @Override
    public RedisReply handle(Command cmd) {
        if (cmd.getArgs().size() < 2) {
            return RedisReply.error("ERR wrong number of arguments for 'lpush' command");
        }
        String key = cmd.getArgs().get(0);
        List<String> vals = cmd.getArgs().subList(1, cmd.getArgs().size());
        int len = store.lpush(key, vals);
        return RedisReply.integer(len);
    }
}
