package com.java.redis.internal.command.handlers;

import com.java.redis.internal.command.CommandHandler;
import com.java.redis.internal.datastore.DataStore;
import com.java.redis.internal.protocol.Command;
import com.java.redis.internal.protocol.RedisReply;

public class ZAddCommand implements CommandHandler {
    private final DataStore store;
    public ZAddCommand(DataStore store) { this.store = store; }

    @Override
    public RedisReply handle(Command cmd) {
        if (cmd.getArgs().size() != 3) {
            return RedisReply.error("ERR wrong number of arguments for 'zadd' command");
        }
        String key = cmd.getArgs().get(0);
        double score;
        try {
            score = Double.parseDouble(cmd.getArgs().get(1));
        } catch (NumberFormatException e) {
            return RedisReply.error("ERR value is not a valid float");
        }
        String member = cmd.getArgs().get(2);
        int added = store.zadd(key, score, member);
        return RedisReply.integer(added);
    }
}
