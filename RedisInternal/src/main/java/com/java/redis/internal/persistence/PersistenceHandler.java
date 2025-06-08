package com.java.redis.internal.persistence;

import com.java.redis.internal.command.CommandExecutor;
import com.java.redis.internal.datastore.DataStore;
import com.java.redis.internal.protocol.Command;

public interface PersistenceHandler {
    void load(DataStore store);
    void appendCommand(String cmd);
    void saveSnapshot(DataStore store);
}
