package com.java.redis.internal.command.handlers;

import com.java.redis.internal.command.CommandHandler;
import com.java.redis.internal.datastore.DataStore;
import com.java.redis.internal.protocol.Command;
import com.java.redis.internal.protocol.RedisReply;

public class HSetCommand implements CommandHandler {
    private final DataStore store;
    public HSetCommand(DataStore store) { this.store = store; }

    @Override
    public RedisReply handle(Command cmd) {
        if (cmd.getArgs().size() != 3) {
            return RedisReply.error("ERR wrong number of arguments for 'hset' command");
        }
        int added = store.hset(
                cmd.getArgs().get(0),
                cmd.getArgs().get(1),
                cmd.getArgs().get(2)
        );
        return RedisReply.integer(added);
    }
}
