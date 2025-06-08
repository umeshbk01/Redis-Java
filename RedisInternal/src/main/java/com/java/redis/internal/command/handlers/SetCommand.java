package com.java.redis.internal.command.handlers;

import com.java.redis.internal.command.CommandHandler;
import com.java.redis.internal.datastore.DataStore;
import com.java.redis.internal.protocol.Command;
import com.java.redis.internal.protocol.RedisReply;

public class SetCommand implements CommandHandler {
    private final DataStore dataStore;
    public SetCommand(DataStore dataStore) {
        this.dataStore = dataStore;
    }
    @Override
    public RedisReply handle(Command cmd) {
       if(cmd.getArgs().size() != 2) {
            return RedisReply.error("ERR wrong number of arguments for 'set' command");
        }
        String key = cmd.getArgs().get(0);
        String value = cmd.getArgs().get(1);
        dataStore.setString(key, value);
        return RedisReply.ok();
    }
}
