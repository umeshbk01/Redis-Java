package com.java.redis.internal.command.handlers;

import com.java.redis.internal.command.CommandHandler;
import com.java.redis.internal.datastore.DataStore;
import com.java.redis.internal.protocol.Command;
import com.java.redis.internal.protocol.RedisReply;

public class ExpireCommand implements CommandHandler {
    private final DataStore dataStore;
    public ExpireCommand(DataStore dataStore) {
        this.dataStore = dataStore;
    }
    @Override
    public RedisReply handle(Command cmd) {
        if (cmd.getArgs().size() != 2) {
            return RedisReply.error("ERR wrong number of arguments for 'expire' command");
        }
        String key = cmd.getArgs().get(0);
        long seconds;
        try{
            seconds = Long.parseLong(cmd.getArgs().get(1));
            if (seconds < 0) {
                return RedisReply.error("ERR value is not an integer or out of range");
            }
        } catch (NumberFormatException e) {
            return RedisReply.error("ERR value is not an integer or out of range");
        }
        int res = dataStore.expire(key, seconds);
        return RedisReply.integer(res);
    }
}
