package com.java.redis.internal.network;

import com.java.redis.internal.command.CommandExecutor;

public interface NetworkServer {
    /**
     * Start the server (blocks the current thread).
     * @param port TCP port to bind.
     * @throws Exception if binding fails.
     */
    void start(int port, CommandExecutor executor) throws Exception;

    /**
     * Stop the server and release resources (optional for v1).
     */
    void stop() throws Exception;
}
