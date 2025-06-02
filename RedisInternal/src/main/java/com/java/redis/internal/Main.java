package com.java.redis.internal;

import com.java.redis.internal.config.Config;
import com.java.redis.internal.config.ConfigLoader;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
// Load configuration
        Config config = ConfigLoader.loadConfig();
        int port = config.getPort();
        System.out.println("Redis server is running on port: " + port);
    }
}