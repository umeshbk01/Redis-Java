package com.java.redis.internal.command.handlers;

import com.java.redis.internal.command.CommandHandler;
import com.java.redis.internal.datastore.DataStore;
import com.java.redis.internal.protocol.Command;
import com.java.redis.internal.protocol.RedisReply;

public class TTLCommand implements CommandHandler {
    private final DataStore dataStore;
    public TTLCommand(DataStore dataStore) {
        this.dataStore = dataStore;
    }
    @Override
    public RedisReply handle(Command cmd) {
        if( cmd.getArgs().size() != 1) {
            return RedisReply.error("ERR wrong number of arguments for 'ttl' command");
        }
        long res = dataStore.ttl(cmd.getArgs().get(0));
        return RedisReply.integer(res);
    }
}
