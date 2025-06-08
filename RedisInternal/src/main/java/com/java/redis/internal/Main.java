package com.java.redis.internal;

import com.java.redis.internal.command.CommandExecutor;
import com.java.redis.internal.config.Config;
import com.java.redis.internal.config.ConfigLoader;
import com.java.redis.internal.datastore.DataStore;
import com.java.redis.internal.network.BasicSocketServer;
import com.java.redis.internal.network.NetworkServer;
import com.java.redis.internal.persistence.NoOpPersistence;
import com.java.redis.internal.persistence.PersistenceHandler;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
// Load configuration
        Config config = ConfigLoader.loadConfig();
        int port = config.getPort();
        System.out.println("Redis server is running on port: " + port);

        PersistenceHandler persistence = new NoOpPersistence();
        persistence.load(null);

        // 2) Initialize in-memory DataStore
        DataStore store = new DataStore();
        CommandExecutor executor = new CommandExecutor(store);

        NetworkServer server = new BasicSocketServer();
        try {
            server.start(port, executor);
        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
        }

    }
}