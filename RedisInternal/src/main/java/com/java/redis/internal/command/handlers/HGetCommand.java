package com.java.redis.internal.command.handlers;

import com.java.redis.internal.command.CommandHandler;
import com.java.redis.internal.datastore.DataStore;
import com.java.redis.internal.protocol.Command;
import com.java.redis.internal.protocol.RedisReply;
/**
 * This class handles the logic for the Redis HGET command, 
 * retrieving a field from a hash and returning the appropriate
 *  Redis protocol reply.
 */
public class HGetCommand implements CommandHandler {
    private final DataStore store;
    public HGetCommand(DataStore store) { this.store = store; }

    @Override
    public RedisReply handle(Command cmd) {
        if (cmd.getArgs().size() != 2) {
            return RedisReply.error("ERR wrong number of arguments for 'hget' command");
        }
        String val = store.hget(cmd.getArgs().get(0), cmd.getArgs().get(1));
        return (val == null)
                ? RedisReply.nullBulk()
                : RedisReply.bulkString(val);
    }
}
