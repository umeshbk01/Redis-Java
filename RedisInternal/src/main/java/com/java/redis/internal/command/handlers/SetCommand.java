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
        int argc = cmd.getArgs().size();
        if(argc != 2 && argc != 4) {
            return RedisReply.error("ERR wrong number of arguments for 'set' command");
        }
        String key = cmd.getArgs().get(0);
        String value = cmd.getArgs().get(1);
        Long exSeconds = null;
        if(argc==4){
            String opt = cmd.getArgs().get(2).toUpperCase();
            if(!"EX".equals(opt)) {
                return RedisReply.error("ERR syntax error");
            }
            try{
                exSeconds = Long.parseLong(cmd.getArgs().get(3));
                if(exSeconds <0){
                    return RedisReply.error("ERR value is not an integer or out of range");
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
                return RedisReply.error("ERR value is not an integer or out of range");
            }
        }
        dataStore.setString(key, value, exSeconds);
        return RedisReply.ok();
    }
}
