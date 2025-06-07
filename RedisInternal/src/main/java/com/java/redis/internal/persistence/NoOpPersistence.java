package com.java.redis.internal.persistence;

import com.java.redis.internal.command.CommandExecutor;
import com.java.redis.internal.protocol.Command;

public class NoOpPersistence implements PersistenceHandler{
    @Override
    public void save(CommandExecutor executor) {

    }

    @Override
    public void load(CommandExecutor executor) {

    }

    @Override
    public void append(Command command) {

    }

    @Override
    public void delete() {

    }

    @Override
    public boolean exists() {
        return false;
    }
}
