package com.java.redis.internal.command.handlers;

import com.java.redis.internal.command.CommandHandler;
import com.java.redis.internal.datastore.DataStore;
import com.java.redis.internal.protocol.Command;
import com.java.redis.internal.protocol.RedisReply;

public class IncrCommand implements CommandHandler {
    private final DataStore store;

    public IncrCommand(DataStore store) {
        this.store = store;
    }

    @Override
    public RedisReply handle(Command cmd) {
        if (cmd.getArgs().size() != 1) {
            return RedisReply.error("ERR wrong number of arguments for 'incr' command");
        }
        try {
            long v = store.incr(cmd.getArgs().get(0));
            return RedisReply.integer(v);
        } catch (NumberFormatException e) {
            return RedisReply.error("ERR value is not an integer or out of range");
        } catch (IllegalStateException e) {
            return RedisReply.error(e.getMessage());
        }
    }
}
