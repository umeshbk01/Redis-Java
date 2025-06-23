// src/main/java/com/example/redis/config/RedisConfig.java
package com.java.redis.internal.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class RedisConfig {
    private final int port;
    private final String persistenceMode; // "rdb" or "none"
    private final Path rdbFilePath;
    private final int rdbSnapshotIntervalSeconds;
    private final int nettyBossThreads;
    private final int nettyWorkerThreads;

    // add getters...

    public static RedisConfig load() {
        // Port from existing ConfigLoader or here:
        int port = Optional.ofNullable(System.getenv("REDIS_PORT"))
                .map(Integer::parseInt)
                .orElse(6379);
        String pm = Optional.ofNullable(System.getenv("PERSISTENCE_MODE"))
                .orElse("none");
        String path = Optional.ofNullable(System.getenv("RDB_FILE_PATH"))
                .orElse("dump.rdb");
        int interval = Optional.ofNullable(System.getenv("RDB_SNAPSHOT_INTERVAL"))
                .map(Integer::parseInt)
                .orElse(60);
        int boss = Optional.ofNullable(System.getenv("NETTY_BOSS_THREADS"))
                .map(Integer::parseInt)
                .orElse(1);
        int worker = Optional.ofNullable(System.getenv("NETTY_WORKER_THREADS"))
                .map(Integer::parseInt)
                .orElse(Runtime.getRuntime().availableProcessors() * 2);
        return new RedisConfig(port, pm, Paths.get(path), interval, boss, worker);
    }

    private RedisConfig(int port, String pm, Path rdbPath, int interval, int boss, int worker) {
        this.port = port;
        this.persistenceMode = pm;
        this.rdbFilePath = rdbPath;
        this.rdbSnapshotIntervalSeconds = interval;
        this.nettyBossThreads = boss;
        this.nettyWorkerThreads = worker;
    }

    // getters...

    public int getPort() {
        return port;
    }

    public String getPersistenceMode() {
        return persistenceMode;
    }

    public Path getRdbFilePath() {
        return rdbFilePath;
    }

    public int getRdbSnapshotIntervalSeconds() {
        return rdbSnapshotIntervalSeconds;
    }

    public int getNettyBossThreads() {
        return nettyBossThreads;
    }

    public int getNettyWorkerThreads() {
        return nettyWorkerThreads;
    }

}
