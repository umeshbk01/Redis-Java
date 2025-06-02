package com.java.redis.internal.config;


public class Config {
    private final int port;
    public Config(int port) {
        this.port = port;
    }
    public int getPort() {
        return port;
    }
}