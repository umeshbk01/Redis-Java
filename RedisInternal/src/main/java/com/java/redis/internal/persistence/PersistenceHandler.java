package com.java.redis.internal.persistence;

import com.java.redis.internal.command.CommandExecutor;
import com.java.redis.internal.protocol.Command;

public interface PersistenceHandler {

    /**
     * Saves the current state of the Redis database to a persistent storage.
     */
    void save(CommandExecutor executor);

    /**
     * Loads the state of the Redis database from persistent storage.
     */
    void load(CommandExecutor executor);

    void append(Command command);

    /**
     * Deletes the persistent storage file.
     */
    void delete();

    /**
     * Checks if the persistent storage file exists.
     *
     * @return true if the file exists, false otherwise
     */
    boolean exists();
}
