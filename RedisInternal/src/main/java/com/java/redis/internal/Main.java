package com.java.redis.internal;

import com.java.redis.internal.command.CommandExecutor;
import com.java.redis.internal.config.Config;
import com.java.redis.internal.config.ConfigLoader;
import com.java.redis.internal.config.RedisConfig;
import com.java.redis.internal.datastore.DataStore;
import com.java.redis.internal.network.BasicSocketServer;
import com.java.redis.internal.network.NetworkServer;
import com.java.redis.internal.persistence.NoOpPersistence;
import com.java.redis.internal.persistence.PersistenceHandler;
import com.java.redis.internal.persistence.RDBPersistenceManager;
import com.java.redis.internal.server.NettyServer;

import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) {
        System.out.println("[DEBUG] Loading Redis configuration...");
        RedisConfig config = RedisConfig.load();
        System.out.println("[DEBUG] Configuration loaded: " + config);

        System.out.println("[DEBUG] Initializing DataStore...");
        DataStore store = new DataStore();

        // PersistenceManager selection
        PersistenceHandler persistence;
        if ("rdb".equalsIgnoreCase(config.getPersistenceMode())) {
            Path p = config.getRdbFilePath();
            System.out.println("[DEBUG] Using RDBPersistenceManager with file: " + p);
            persistence = new RDBPersistenceManager(p);
        } else {
            System.out.println("[DEBUG] Using NoOpPersistence (no persistence)");
            persistence = new NoOpPersistence();
        }

        // Load existing snapshot if any
        System.out.println("[DEBUG] Loading existing snapshot (if any)...");
        persistence.load(store);
        System.out.println("[DEBUG] Snapshot load complete.");

        // Schedule periodic snapshot if RDB mode
        ScheduledExecutorService scheduler;
        if (persistence instanceof RDBPersistenceManager) {
            scheduler = Executors.newSingleThreadScheduledExecutor();
            int interval = config.getRdbSnapshotIntervalSeconds();
            System.out.println("[DEBUG] Scheduling periodic snapshot every " + interval + " seconds.");
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    System.out.println("[DEBUG] Saving scheduled snapshot...");
                    persistence.saveSnapshot(store);
                    System.out.println("[DEBUG] Scheduled snapshot saved.");
                } catch (Exception e) {
                    System.err.println("[ERROR] Error during scheduled snapshot: " + e.getMessage());
                    e.printStackTrace();
                }
            }, interval, interval, TimeUnit.SECONDS);
        } else {
            scheduler = null;
        }

        // Register shutdown hook for final snapshot
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[DEBUG] Shutdown initiated: saving final snapshot...");
            persistence.saveSnapshot(store);
            if (scheduler != null) {
                scheduler.shutdown();
                System.out.println("[DEBUG] Scheduler shutdown.");
            }
            System.out.println("[DEBUG] Shutdown hook complete.");
        }));

        // Create CommandExecutor with store (handlers may call persistence.appendCommand if desired)
        System.out.println("[DEBUG] Creating CommandExecutor...");
        CommandExecutor executor = new CommandExecutor(store);

        // Start Netty server
        NettyServer server = new NettyServer(
                config.getPort(),
                executor,
                config.getNettyBossThreads(),
                config.getNettyWorkerThreads()
        );
        try {
            System.out.println("[DEBUG] Starting Netty server on port " + config.getPort() + "...");
            server.start();
            System.out.println("[DEBUG] Netty server started.");
        } catch (InterruptedException e) {
            System.err.println("[ERROR] Server interrupted: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                System.out.println("[DEBUG] Stopping Netty server...");
                server.stop();
                System.out.println("[DEBUG] Netty server stopped.");
            } catch (InterruptedException e) {
                System.err.println("[ERROR] Error during server stop: " + e.getMessage());
                throw new RuntimeException(e);
            }
            if (scheduler != null) {
                scheduler.shutdown();
                System.out.println("[DEBUG] Scheduler shutdown in finally block.");
            }
        }
    }
}