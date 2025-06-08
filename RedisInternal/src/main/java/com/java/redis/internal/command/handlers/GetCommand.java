package com.java.redis.internal.command.handlers;

import com.java.redis.internal.command.CommandHandler;
import com.java.redis.internal.constants.ReplyType;
import com.java.redis.internal.datastore.DataStore;
import com.java.redis.internal.protocol.Command;
import com.java.redis.internal.protocol.RedisReply;

public class GetCommand implements CommandHandler{
    private final DataStore dataStore;
    public GetCommand(DataStore dataStore) {
        this.dataStore = dataStore;
    }
    @Override
    public RedisReply handle(Command cmd) {
        if(cmd.getArgs().size() != 1) {
            return RedisReply.error("ERR wrong number of arguments for 'set' command");
        }
        String key = cmd.getArgs().get(0);
        String value = dataStore.getString(key);
        return (value == null)
                ? RedisReply.nullBulk()
                : RedisReply.bulkString(value);
    }
}
