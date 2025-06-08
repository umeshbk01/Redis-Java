package com.java.redis.internal.persistence;

import com.java.redis.internal.command.CommandExecutor;
import com.java.redis.internal.datastore.DataStore;
import com.java.redis.internal.protocol.Command;

public class NoOpPersistence implements PersistenceHandler{
    public void load(DataStore s) {}
    public void appendCommand(String c) {}
    public void saveSnapshot(DataStore s) {}

}
