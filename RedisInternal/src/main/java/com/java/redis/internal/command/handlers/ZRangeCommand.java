package com.java.redis.internal.command.handlers;

import com.java.redis.internal.command.CommandHandler;
import com.java.redis.internal.datastore.DataStore;
import com.java.redis.internal.protocol.Command;
import com.java.redis.internal.protocol.RedisReply;

import java.util.List;
import java.util.stream.Collectors;

public class ZRangeCommand implements CommandHandler {
    private final DataStore store;
    public ZRangeCommand(DataStore store) { this.store = store; }

    @Override
    public RedisReply handle(Command cmd) {
        if (cmd.getArgs().size() != 3) {
            return RedisReply.error("ERR wrong number of arguments for 'zrange' command");
        }
        String key = cmd.getArgs().get(0);
        int start, stop;
        try {
            start = Integer.parseInt(cmd.getArgs().get(1));
            stop  = Integer.parseInt(cmd.getArgs().get(2));
        } catch (NumberFormatException e) {
            return RedisReply.error("ERR value is not an integer or out of range");
        }
        List<String> members = store.zrange(key, start, stop);
        List<RedisReply> replies = members.stream()
                .map(RedisReply::bulkString)
                .collect(Collectors.toList());
        return RedisReply.array(replies);
    }
}
