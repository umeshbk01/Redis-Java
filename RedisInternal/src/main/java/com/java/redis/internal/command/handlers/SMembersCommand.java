package com.java.redis.internal.command.handlers;

import com.java.redis.internal.command.CommandHandler;
import com.java.redis.internal.datastore.DataStore;
import com.java.redis.internal.protocol.Command;
import com.java.redis.internal.protocol.RedisReply;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SMembersCommand implements CommandHandler {
    private final DataStore store;
    public SMembersCommand(DataStore store) { this.store = store; }

    @Override
    public RedisReply handle(Command cmd) {
        if (cmd.getArgs().size() != 1) {
            return RedisReply.error("ERR wrong number of arguments for 'smembers' command");
        }
        Set<String> members = store.smembers(cmd.getArgs().get(0));
        List<RedisReply> replies = members.stream()
                .map(RedisReply::bulkString)
                .collect(Collectors.toList());
        return RedisReply.array(replies);
    }
}
