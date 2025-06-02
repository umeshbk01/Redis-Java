package com.java.redis.internal.config;

import java.util.Optional;

public class ConfigLoader {
    private static final int DEFAULT_PORT = 6379;
    private static final String ENV_PORT = "REDIS_PORT";

    public static Config loadConfig() {
        int port = Optional.ofNullable(System.getenv(ENV_PORT))
                .map(Integer::parseInt)
                .orElse(DEFAULT_PORT);
        return new Config(port);
    }
}